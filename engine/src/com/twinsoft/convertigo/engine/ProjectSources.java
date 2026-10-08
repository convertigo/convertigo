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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import com.twinsoft.convertigo.engine.ClasspathSnapshot.Entry;
import com.twinsoft.convertigo.engine.ClasspathSnapshot.Kind;
import com.twinsoft.convertigo.engine.util.DirClassLoader;
import com.twinsoft.convertigo.engine.util.FileUtils;

/**
 * The Java sources of the projects (libs/src), compiled in the copy of their libraries (see {@link ClasspathSnapshot}),
 * the projects they reference first, against the libraries, those of the workspace and the engine. The classes of a
 * build folder compiled from the same sources, by the CI or the export of the project, are taken instead: a server that
 * does not compile (server_build none) only takes them.
 */
final class ProjectSources {
	/** The most errors told for a compilation. */
	private static final int MAX_ERRORS = 20;

	/** Whether the engine compiles: always in the Studio, else when allowed (overridden by the tests). */
	static BooleanSupplier compileAllowed = () -> {
		try {
			return Engine.serverBuild().allows(EnginePropertiesManager.ServerBuild.sources);
		} catch (Exception e) {
			return false;
		}
	};

	private static volatile List<File> engineClasspath;

	/** Whether sources are compiled when a copy is prepared. */
	enum Compilation {
		/** compiled, unless the classes of their build folder are compiled from them */
		ALLOWED,
		/** only the classes of their build folder: the engine does not compile (server_build none) */
		NOT_ALLOWED,
		/** only the classes of their build folder: they do not compile */
		FAILED;

		static Compilation of(boolean allowed) {
			return allowed ? ALLOWED : NOT_ALLOWED;
		}
	}

	/** Sources that do not compile, with the errors of the compiler. */
	static final class CompilationException extends IOException {
		private static final long serialVersionUID = 1L;
		private final List<String> errors;

		CompilationException(String message, List<String> errors) {
			super(message + (errors.isEmpty() ? "" : ":\n" + String.join("\n", errors)));
			this.errors = List.copyOf(errors);
		}

		List<String> getErrors() {
			return errors;
		}
	}

	private ProjectSources() {
	}

	static boolean mayCompile() {
		return compileAllowed.getAsBoolean();
	}

	/**
	 * Gives their classes to the src folders of a copy being prepared, the last libs folders (those of the referenced
	 * projects) first.
	 *
	 * @param compilation whether sources are compiled; when not, they get only the classes of their build folder
	 * @return whether every src folder got its classes
	 * @throws CompilationException when sources do not compile
	 */
	static boolean build(String project, List<Entry> entries, File copy, Compilation compilation) throws IOException {
		var sources = entries.stream().filter(entry -> entry.kind() == Kind.SOURCES)
				.sorted((left, right) -> Integer.compare(right.dir(), left.dir())).toList();
		if (sources.isEmpty()) {
			return true;
		}
		var classpath = new ArrayList<File>(ClasspathSnapshot.classpath(entries, copy));
		boolean all = true;
		for (var entry : sources) {
			var src = new File(copy, entry.relativePath());
			var output = new File(copy, entry.compiledPath());
			var origin = origin(entry.source());
			if (takeBuild(entries, entry, copy, src, output)) {
				info("Libraries of the project " + project + ": " + origin + " uses its classes compiled before");
			} else if (compilation != Compilation.ALLOWED) {
				all = false;
				if (compilation == Compilation.FAILED) {
					continue;
				}
				warn("Libraries of the project " + project + ": " + origin + " is not compiled, as this server does not"
						+ " compile them (server_build none), and has no classes compiled from these sources: the project uses"
						+ " its libraries without them");
				continue;
			} else {
				var count = compile(src, output, classpath, origin);
				info("Libraries of the project " + project + ": " + count + " Java source(s) of " + origin + " compiled");
			}
			// the classes of a referenced project, for those of the projects that use it
			classpath.add(output);
		}
		return all;
	}

	/** @return how a src folder is told: project/libs/src */
	private static String origin(File src) {
		var libsDir = src.getParentFile();
		var projectDir = libsDir == null ? null : libsDir.getParentFile();
		return projectDir == null ? src.getPath() : projectDir.getName() + "/" + libsDir.getName() + "/" + src.getName();
	}

	/** @return whether the build folder next to the sources has their classes, now copied as theirs */
	private static boolean takeBuild(List<Entry> entries, Entry sources, File copy, File src, File output)
			throws IOException {
		for (var entry : entries) {
			if (entry.kind() == Kind.BUILD && entry.dir() == sources.dir()) {
				var build = new File(copy, entry.relativePath());
				var fingerprint = new File(build, ClasspathSnapshot.BUILD_FINGERPRINT);
				if (fingerprint.isFile() && Files.readString(fingerprint.toPath(), StandardCharsets.UTF_8).trim()
						.equalsIgnoreCase(ClasspathSnapshot.sourcesFingerprint(src))) {
					FileUtils.copyDirectory(new File(build, ClasspathSnapshot.BUILD_CLASSES), output);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Compiles the Java sources of a folder, and copies its other files as resources.
	 *
	 * @return the number of Java sources
	 */
	static int compile(File src, File output, List<File> classpath, String origin) throws IOException {
		Files.createDirectories(output.toPath());
		var javaFiles = new ArrayList<File>();
		var srcPath = src.toPath();
		try (var files = Files.walk(srcPath)) {
			for (var path : (Iterable<java.nio.file.Path>) files::iterator) {
				if (!Files.isRegularFile(path)) {
					continue;
				}
				if (path.getFileName().toString().endsWith(".java")) {
					javaFiles.add(path.toFile());
				} else {
					var resource = output.toPath().resolve(srcPath.relativize(path).toString());
					Files.createDirectories(resource.getParent());
					Files.copy(path, resource);
				}
			}
		}
		if (javaFiles.isEmpty()) {
			return 0;
		}
		var compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new CompilationException("Unable to compile " + origin + ": the Java runtime has no compiler (a JDK is"
					+ " required)", List.of());
		}
		var cp = new LinkedHashSet<File>(classpath);
		cp.addAll(engineClasspath());
		var options = List.of("-d", output.getPath(), "-classpath",
				cp.stream().map(File::getPath).collect(Collectors.joining(File.pathSeparator)), "--release",
				Integer.toString(Runtime.version().feature()), "-encoding", "UTF-8", "-proc:none", "-g", "-nowarn");
		var diagnostics = new DiagnosticCollector<JavaFileObject>();
		boolean success;
		try (var fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
			success = compiler.getTask(null, fileManager, diagnostics, options, null,
					fileManager.getJavaFileObjectsFromFiles(javaFiles)).call();
		}
		if (!success) {
			var errors = new ArrayList<String>();
			for (var diagnostic : diagnostics.getDiagnostics()) {
				if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
					if (errors.size() == MAX_ERRORS) {
						errors.add("…");
						break;
					}
					errors.add(describe(diagnostic, srcPath, origin));
				}
			}
			throw new CompilationException("The Java sources of " + origin + " do not compile", errors);
		}
		return javaFiles.size();
	}

	private static String describe(Diagnostic<? extends JavaFileObject> diagnostic, java.nio.file.Path src,
			String origin) {
		var where = origin;
		var source = diagnostic.getSource();
		if (source != null) {
			try {
				where = origin + "/" + src.relativize(new File(source.toUri()).toPath()).toString().replace(File.separatorChar, '/');
			} catch (Exception e) {
				where = source.getName();
			}
		}
		var line = diagnostic.getLineNumber();
		return where + (line > 0 ? ":" + line : "") + ": " + diagnostic.getMessage(Locale.ROOT);
	}

	/**
	 * @return the class path of the engine: the folders and jars of its class loaders, those of the engine classes, of
	 *         Rhino and of the servlet API (the Studio has no WEB-INF/lib), and the class path of the JVM
	 */
	static List<File> engineClasspath() {
		var classpath = engineClasspath;
		if (classpath == null) {
			var files = new LinkedHashSet<File>();
			var loader = Engine.getEngineClassLoader();
			for (var current = loader != null ? loader : ProjectSources.class.getClassLoader(); current != null;) {
				if (current instanceof URLClassLoader urlLoader) {
					for (var url : urlLoader.getURLs()) {
						addFile(files, url);
					}
				}
				current = current instanceof DirClassLoader dirLoader ? dirLoader.getParentLoader() : current.getParent();
			}
			addLocation(files, Engine.class, true);
			addLocation(files, org.mozilla.javascript.Context.class, false);
			addLocation(files, jakarta.servlet.Servlet.class, false);
			for (var path : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
				if (!path.isEmpty()) {
					files.add(new File(path));
				}
			}
			files.removeIf(file -> !file.exists());
			engineClasspath = classpath = List.copyOf(files);
		}
		return classpath;
	}

	private static void addFile(Set<File> files, java.net.URL url) {
		if ("file".equals(url.getProtocol())) {
			try {
				files.add(new File(url.toURI()));
			} catch (URISyntaxException | IllegalArgumentException e) {
				// not a file
			}
		}
	}

	/** @param siblings with the jars next to it: the libraries of the engine */
	private static void addLocation(Set<File> files, Class<?> cls, boolean siblings) {
		try {
			var codeSource = cls.getProtectionDomain().getCodeSource();
			if (codeSource == null || codeSource.getLocation() == null) {
				return;
			}
			var location = new File(codeSource.getLocation().toURI());
			if (addBundle(files, location)) {
				return;
			}
			files.add(location);
			if (siblings) {
				// a jar among the libraries, or a classes folder next to a lib folder (WEB-INF, a Studio plugin)
				var libDir = location.isFile() ? location.getParentFile() : new File(location.getParentFile(), "lib");
				var jars = libDir == null ? null : libDir.listFiles((dir, name) -> name.endsWith(".jar"));
				if (jars != null) {
					Arrays.sort(jars);
					files.addAll(Arrays.asList(jars));
				}
			}
		} catch (Exception e) {
			// not a file
		}
	}

	/**
	 * The Studio is an OSGi bundle: its classes are those of its Bundle-ClassPath, after the output folders an IDE
	 * compiles into when it launches the Studio (osgi.dev).
	 *
	 * @return whether the location is the folder of a bundle
	 */
	static boolean addBundle(Set<File> files, File location) throws IOException {
		var manifestFile = new File(location, "META-INF/MANIFEST.MF");
		if (!location.isDirectory() || !manifestFile.isFile()) {
			return false;
		}
		java.util.jar.Attributes attributes;
		try (var input = Files.newInputStream(manifestFile.toPath())) {
			attributes = new java.util.jar.Manifest(input).getMainAttributes();
		}
		var classpath = attributes.getValue("Bundle-ClassPath");
		if (classpath == null) {
			return false;
		}
		var name = attributes.getValue("Bundle-SymbolicName");
		name = name == null ? "" : name.split(";")[0].trim();
		var entries = new ArrayList<String>();
		var dev = System.getProperty("osgi.dev");
		if (dev != null && !dev.isBlank()) {
			if (dev.contains(":/")) {
				var properties = new java.util.Properties();
				try (var input = new java.net.URI(dev.trim()).toURL().openStream()) {
					properties.load(input);
				} catch (Exception e) {
					// not a readable file
				}
				dev = properties.getProperty(name, properties.getProperty("*", ""));
			}
			entries.addAll(Arrays.asList(dev.split(",")));
		}
		entries.addAll(Arrays.asList(classpath.split(",")));
		for (var entry : entries) {
			entry = entry.trim();
			if (!entry.isEmpty() && !entry.equals(".")) {
				var file = new File(location, entry);
				if (file.exists()) {
					files.add(file);
				}
			}
		}
		return true;
	}

	private static void info(String message) {
		if (Engine.logEngine != null) {
			Engine.logEngine.info(message);
		}
	}

	private static void warn(String message) {
		if (Engine.logEngine != null) {
			Engine.logEngine.warn(message);
		}
	}
}
