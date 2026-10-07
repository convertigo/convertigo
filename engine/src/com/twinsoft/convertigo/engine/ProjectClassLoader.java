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

package com.twinsoft.convertigo.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A generation of the class path of a project: its libraries (libs/*.jar and libs/classes, with those of the projects it
 * references), searched before the engine. The protected packages of the engine (see
 * ProjectClassLoader-protected-packages.txt) are an exception: their classes are taken from the engine when it has them,
 * as a second copy breaks the JavaScript Packages object and casts. A generation never changes: new libraries make a new
 * generation (see {@link ProjectLibraries}).
 */
public final class ProjectClassLoader extends URLClassLoader {
	static {
		ClassLoader.registerAsParallelCapable();
	}

	private static final List<String> PROTECTED_PACKAGES = readProtectedPackages();
	private static final AtomicLong GENERATIONS = new AtomicLong();

	private final ClassLoader engine;
	private final List<String> protectedPackages;
	private final String project;
	private final String version;
	private final String fingerprint;
	private final File snapshot;
	private final String id;

	ProjectClassLoader(URL[] urls, ClassLoader engine, String project, String version, String fingerprint, File snapshot) {
		this(urls, engine, project, version, fingerprint, snapshot, PROTECTED_PACKAGES);
	}

	ProjectClassLoader(URL[] urls, ClassLoader engine, String project, String version, String fingerprint, File snapshot,
			List<String> protectedPackages) {
		super("project " + project, urls, null);
		this.engine = engine;
		this.protectedPackages = protectedPackages;
		this.project = project;
		this.version = version;
		this.fingerprint = fingerprint;
		this.snapshot = snapshot;
		this.id = project + "#" + GENERATIONS.incrementAndGet();
	}

	/** @return the identifier of this generation, unique in the JVM */
	public String getId() {
		return id;
	}

	public String getProject() {
		return project;
	}

	public String getVersion() {
		return version;
	}

	/** @return the fingerprint of the content of the libraries */
	public String getFingerprint() {
		return fingerprint;
	}

	/** @return the copy of the libraries this generation reads, null without libraries */
	File getSnapshot() {
		return snapshot;
	}

	boolean isProtected(String className) {
		for (var prefix : protectedPackages) {
			if (className.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			Class<?> cls = findLoadedClass(name);
			if (cls == null) {
				if (isProtected(name)) {
					try {
						cls = engine.loadClass(name);
					} catch (ClassNotFoundException e) {
						cls = super.loadClass(name, false);
					}
				} else {
					try {
						// the JDK, then the libraries of the project
						cls = super.loadClass(name, false);
					} catch (ClassNotFoundException e) {
						cls = engine.loadClass(name);
					}
				}
			}
			if (resolve) {
				resolveClass(cls);
			}
			return cls;
		}
	}

	/**
	 * Defines a copy of a class of the engine in this generation, to run code that the JDK sees as code of the
	 * generation (see {@link ProjectDriversCleanup}). The class must use only the JDK.
	 */
	Class<?> defineEngineClass(Class<?> engineClass) throws IOException {
		var name = engineClass.getName();
		synchronized (getClassLoadingLock(name)) {
			var defined = findLoadedClass(name);
			if (defined != null && defined.getClassLoader() == this) {
				return defined;
			}
			try (var input = engineClass.getResourceAsStream(engineClass.getSimpleName() + ".class")) {
				if (input == null) {
					throw new IOException("The class " + name + " is not available");
				}
				var bytes = input.readAllBytes();
				return defineClass(name, bytes, 0, bytes.length);
			}
		}
	}

	@Override
	public URL getResource(String name) {
		var url = super.getResource(name);
		return url != null ? url : engine.getResource(name);
	}

	/** @return the resources of the libraries of the project, then those of the engine */
	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		var urls = Collections.list(findResources(name));
		urls.addAll(Collections.list(engine.getResources(name)));
		return Collections.enumeration(urls);
	}

	@Override
	public String toString() {
		return "ProjectClassLoader[" + id + (version.isBlank() ? "" : ", version " + version) + ", libraries "
				+ (fingerprint.length() > 12 ? fingerprint.substring(0, 12) : fingerprint)
				+ (fingerprint.endsWith(ClasspathSnapshot.WITHOUT_SOURCES) ? " without the classes of its sources" : "") + "]";
	}

	private static List<String> readProtectedPackages() {
		var packages = new ArrayList<String>();
		try (var input = ProjectClassLoader.class.getResourceAsStream("ProjectClassLoader-protected-packages.txt")) {
			if (input != null) {
				var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
				for (String line; (line = reader.readLine()) != null;) {
					line = line.trim();
					if (!line.isEmpty() && !line.startsWith("#")) {
						packages.add(line);
					}
				}
			}
		} catch (IOException e) {
			// nothing protected
		}
		return List.copyOf(packages);
	}
}
