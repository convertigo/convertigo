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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;

/**
 * The jars and classes folder of a folder, searched before a parent: the libraries of the workspace (workspace/libs),
 * loaded once. The libraries of the projects have their own class loaders (see ProjectClassLoader).
 */
public class DirClassLoader extends URLClassLoader {
	private ClassLoader parent;

	public DirClassLoader(File dir, ClassLoader parent) {
		super(makeURLs(dir), null);
		this.parent = parent;
	}

	private static URL[] makeURLs(File dir) {
		var urls = new ArrayList<URL>();
		var list = dir.list();
		if (list != null) {
			Arrays.sort(list);
			for (var name : list) {
				if (name.endsWith(".jar") || name.equals("classes")) {
					try {
						urls.add(new File(dir, name).toURI().toURL());
					} catch (Exception e) {
						// improbable
					}
				}
			}
		}
		return urls.toArray(new URL[urls.size()]);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		Class<?> cls = null;
		try {
			cls = super.loadClass(name);
		} catch (ClassNotFoundException e) {
			if (parent != null) {
				cls = parent.loadClass(name);
			} else {
				throw e;
			}
		}
		return cls;
	}

	@Override
	public URL getResource(String name) {
		URL url = super.getResource(name);
		if (url == null && parent != null) {
			url = parent.getResource(name);
		}
		return url;
	}

	/** @return the resources of this folder, then those of the parent */
	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		var urls = Collections.list(super.getResources(name));
		if (parent != null) {
			// URL.equals may resolve host names
			var known = new HashSet<String>();
			for (var url : urls) {
				known.add(url.toExternalForm());
			}
			for (var url : Collections.list(parent.getResources(name))) {
				if (known.add(url.toExternalForm())) {
					urls.add(url);
				}
			}
		}
		return Collections.enumeration(urls);
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		InputStream is = super.getResourceAsStream(name);
		if (is == null && parent != null) {
			is = parent.getResourceAsStream(name);
		}
		return is;
	}
}
