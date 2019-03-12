package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

public class DirClassLoader extends ClassLoader {
	ClassLoader parent;
	ClassLoader loader;
	List<File> dirs;
	long nextCheck = 0;
	String lastContent;
	
	public DirClassLoader(File dir, ClassLoader parent) {
		this.dirs = Arrays.asList(dir);
		this.parent = parent;
	}
	
	public DirClassLoader(List<File> dirs, ClassLoader parent) {
		this.dirs = dirs;
		this.parent = parent;
	}
	
	private synchronized void checkLoader() {
		long now = System.currentTimeMillis();
		if (now < nextCheck) {
			return;
		}
		nextCheck = now + 5000;
		ArrayList<URL> urls = new ArrayList<>();
		for (File dir: dirs) {
			if (dir.exists()) {
				String[] list = dir.list();
				for (String file: list) {
					if (file.endsWith(".jar") || file.equals("classes")) {
						try {
							urls.add(new File(dir, file).toURI().toURL());
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		
		if (urls.isEmpty()) {
			loader = parent;
			return;
		}
		
		String content = urls.toString();
		if (content.equals(lastContent)) {
			return;
		}
		lastContent = content;
		
		loader = new URLClassLoader(urls.toArray(new URL[urls.size()]), null);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		checkLoader();
		try {
			return loader.loadClass(name);
		} catch (ClassNotFoundException e) {
			return parent != null ? parent.loadClass(name) : null;
		}
	}

	@Override
	public URL getResource(String name) {
		checkLoader();
		URL url = loader.getResource(name);
		if (url != null || parent == null) {
			return url;
		} else {
			return parent.getResource(name);
		}
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		checkLoader();
		Enumeration<URL> eu = loader.getResources(name);
		if (eu.hasMoreElements() || parent == null) {
			return eu;
		} else {
			return parent.getResources(name);
		}
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		checkLoader();
		InputStream is = loader.getResourceAsStream(name);
		if (is != null || parent == null) {
			return is;
		} else {
			return parent.getResourceAsStream(name);
		}
	}

	@Override
	public void setDefaultAssertionStatus(boolean enabled) {
		checkLoader();
		loader.setDefaultAssertionStatus(enabled);
	}

	@Override
	public void setPackageAssertionStatus(String packageName, boolean enabled) {
		checkLoader();
		loader.setPackageAssertionStatus(packageName, enabled);
	}

	@Override
	public void setClassAssertionStatus(String className, boolean enabled) {
		checkLoader();
		loader.setClassAssertionStatus(className, enabled);
	}

	@Override
	public void clearAssertionStatus() {
		checkLoader();
		loader.clearAssertionStatus();
	}

}
