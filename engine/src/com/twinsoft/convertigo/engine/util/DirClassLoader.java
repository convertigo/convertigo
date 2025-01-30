/*
 * Copyright (c) 2001-2025 Convertigo SA.
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
import java.util.Enumeration;
import java.util.List;

public class DirClassLoader extends URLClassLoader {
	private ClassLoader parent;
	
	private List<File> dirs;
	private long nextCheck = 0;
	private String lastContent;
	
	public DirClassLoader(File dir, ClassLoader parent) {
		super(makeURLs(Arrays.asList(dir), null), null);
		this.dirs = Arrays.asList(dir);
		this.parent = parent;
		isContentChanged();
	}
	
	public DirClassLoader(List<File> dirs, ClassLoader parent, File copyTo) {
		super(makeURLs(dirs, copyTo), null);
		this.dirs = dirs;
		this.parent = parent;
		isContentChanged();
	}
	
	private static URL[] makeURLs(List<File> dirs, File copyTo) {
		ArrayList<URL> urls = new ArrayList<>();
		for (File dir: dirs) {
			if (dir.exists()) {
				String[] list = dir.list();
				for (String file: list) {
					if (file.endsWith(".jar") || file.equals("classes")) {
						File f = new File(dir, file);
						if (copyTo != null) {
							File d = new File(copyTo, file);
							try {
								if (f.isDirectory()) {
									FileUtils.copyDirectory(f, d);
								} else {
									FileUtils.copyFile(f, d);
								}
							} catch (Exception e) {
								// destination should exist and locked
							}
							f = d;
						}
						try {
							urls.add(f.toURI().toURL());
						} catch (Exception e) {
							// improbable
						}
					}
				}
			}
		}
		
		return urls.toArray(new URL[urls.size()]);
	}
	
	public synchronized boolean isContentChanged() {
		long now = System.currentTimeMillis();
		if (now < nextCheck) {
			return false;
		}
		nextCheck = now + 5000;
		StringBuilder sb = new StringBuilder();
		for (URL u : makeURLs(dirs, null)) {
			sb.append(u.toString().replaceFirst(".*/(.+?)$", "$1"));
		}
		String content = sb.toString();
		if (content.equals(lastContent)) {
			return false;
		}
		lastContent = content;
		return true;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		Class<?> cls = null;
		try {
			cls = super.loadClass(name);
		} catch (ClassNotFoundException e) {
			if (parent != null) {
				cls = parent.loadClass(name);
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

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		Enumeration<URL> eu = super.getResources(name);
		if (!eu.hasMoreElements() && parent != null) {
			eu = parent.getResources(name);
		}
		return eu;
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
