/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import com.twinsoft.convertigo.engine.Engine;

public class XSLTUtils {

    private static final Map<String, Map<String, Templates>> projectCaches = new ConcurrentHashMap<>();

    /** Returns templates cached until the project is reloaded, deployed or removed. */
    public static Templates getTemplates(String projectName, String xslPath) throws Exception {
        Objects.requireNonNull(projectName, "projectName");
        File file = new File(xslPath).getCanonicalFile();
        String key = file.getAbsolutePath();
        Map<String, Templates> projectCache = projectCaches.computeIfAbsent(
                projectName, name -> new ConcurrentHashMap<>());

        Templates templates = projectCache.get(key);
        if (templates != null) {
            return templates;
        }

        synchronized (projectCache) {
            templates = projectCache.get(key);
            if (templates == null) {
                templates = compileXSL(file);
                projectCache.put(key, templates);
            }
        }
        return templates;
    }

    /**
     * Clears all compiled XSLT templates for a project when it is deployed, reloaded or removed.
     *
     * @param projectName the name of the project to clear
     */
    public static void clearProjectCache(String projectName) {
        if (projectName != null) {
            // Detach the cache so an in-flight compilation cannot repopulate the new cache.
            if (projectCaches.remove(projectName) != null) {
                Engine.logEngine.info("(XSLTUtils) XSLT cache cleared for project: " + projectName);
            }
        }
    }

    private static Templates compileXSL(File file) throws Exception {
        String uri = file.toURI().toString();

        StreamSource src = new StreamSource(uri);
        src.setSystemId(uri);

        TransformerFactory factory = new org.apache.xalan.processor.TransformerFactoryImpl();
        ErrorListener defaultListener = factory.getErrorListener();
        // Xalan can otherwise return partial templates after an unresolved include/import.
        factory.setErrorListener(new ErrorListener() {
            @Override
            public void warning(TransformerException exception) throws TransformerException {
                defaultListener.warning(exception);
            }

            @Override
            public void error(TransformerException exception) throws TransformerException {
                throw exception;
            }

            @Override
            public void fatalError(TransformerException exception) throws TransformerException {
                throw exception;
            }
        });
        return factory.newTemplates(src);
    }
}
