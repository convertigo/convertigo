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

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class XSLTUtils {

    private static final TransformerFactory factory = new org.apache.xalan.processor.TransformerFactoryImpl();

    private static final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private static class CacheEntry {
        final Templates templates;
        final long lastModified;

        CacheEntry(Templates templates, long lastModified) {
            this.templates = templates;
            this.lastModified = lastModified;
        }
    }

    public static Templates getTemplates(String xslPath) throws Exception {

        File file = new File(xslPath).getCanonicalFile();
        String key = file.getAbsolutePath();
        long currentLastModified = file.lastModified();

        CacheEntry entry = cache.get(key);

        if (entry == null || entry.lastModified != currentLastModified) {
            Templates templates = compileXSL(file);
            cache.put(key, new CacheEntry(templates, currentLastModified));
            return templates;
        }

        return entry.templates;
    }

    private static Templates compileXSL(File file) throws Exception {
        String uri = file.toURI().toString();

        StreamSource src = new StreamSource(uri);
        src.setSystemId(uri);

        return factory.newTemplates(src);
    }
}
