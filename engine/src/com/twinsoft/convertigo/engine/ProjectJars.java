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
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.http.client.methods.HttpGet;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.twinsoft.convertigo.engine.util.HttpUtils;

/**
 * The jars a libs folder declares in its jars.yaml, downloaded and checked instead of being committed:
 *
 * <pre>
 * repositories:                                # Maven repositories, tried in order (default: Maven Central)
 *   - https://nexus.example.com/repository/maven-releases
 * jars:
 *   - maven: com.querydsl:querydsl-sql:7.7      # group:artifact:version[:classifier], no transitive dependency
 *     sha256: 0e4f...
 *   - url: https://download.example.com/driver-1.2.jar
 *     sha256: 3f9a...
 * </pre>
 *
 * Each jar is checked against its sha256, whatever its location (http or https). A repository or url may reference
 * global symbols, for credentials: https://${nexus.user}:${nexus.password.secret}@nexus.example.com/... An engine
 * downloads only when it builds the libraries (server_build sources and above, always in the Studio and the CLI);
 * otherwise it takes the jars the archive of the project carries in libs/build/lib, when their sha256 match.
 */
final class ProjectJars {
	static final String DESCRIPTOR = "jars.yaml";
	/** where the archive of a project carries its declared jars, in its libs folder */
	static final String EMBEDDED = "build/lib";
	static final String DEFAULT_REPOSITORY = "https://repo.maven.apache.org/maven2";
	private static final long MAX_SIZE = 256L * 1024 * 1024;
	private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
	private static final Pattern COORDINATES = Pattern.compile("([\\w.-]+):([\\w.-]+):([\\w.+-]+)(?::([\\w.-]+))?");
	private static final Pattern FILE_NAME = Pattern.compile("[\\w.+-]+\\.jar");

	/** Whether the engine may download (overridden by the tests). */
	static java.util.function.BooleanSupplier downloadAllowed = ProjectSources::mayCompile;
	/** The cache of the downloaded jars (overridden by the tests). */
	static java.util.function.Supplier<File> cache = ProjectJars::defaultCache;

	/** How long a failure of a descriptor is kept before downloading again, while it is unchanged (tests). */
	static long retryDelay = 10 * 60_000;

	/** the files already checked against their sha256: path, size and date of the file -> sha256 */
	private static final Map<String, String> checked = new ConcurrentHashMap<>();
	/** the last failure of a descriptor: path, size and date of the descriptor -> failure */
	private static final Map<String, Failure> failures = new ConcurrentHashMap<>();

	private record Failure(long at, IOException error) {
	}

	/** A declared jar: its file name, the locations to try and its sha256 (null when the descriptor misses it). */
	record Declared(String fileName, List<String> locations, String sha256) {
	}

	private ProjectJars() {
	}

	/** @return the descriptor of a libs folder, null without it */
	static File descriptor(File libs) {
		var file = new File(libs, DESCRIPTOR);
		return file.isFile() ? file : null;
	}

	/** @return the jars a descriptor declares */
	static List<Declared> read(File descriptor) throws IOException {
		Object root;
		try (var in = Files.newInputStream(descriptor.toPath())) {
			root = new Yaml(new SafeConstructor(new LoaderOptions())).load(in);
		} catch (Exception e) {
			throw new IOException(DESCRIPTOR + " is not valid YAML: " + e.getMessage(), e);
		}
		if (root == null) {
			return List.of();
		}
		if (!(root instanceof Map<?, ?> map)) {
			throw new IOException(DESCRIPTOR + " must be a map with repositories and jars");
		}
		var repositories = new ArrayList<String>();
		if (map.get("repositories") instanceof List<?> list) {
			for (var repository : list) {
				repositories.add(String.valueOf(repository).replaceFirst("/+$", ""));
			}
		} else if (map.get("repositories") != null) {
			throw new IOException(DESCRIPTOR + ": repositories must be a list of URLs");
		}
		if (repositories.isEmpty()) {
			repositories.add(DEFAULT_REPOSITORY);
		}
		var declared = new ArrayList<Declared>();
		var names = new LinkedHashSet<String>();
		var jars = map.get("jars");
		if (jars != null && !(jars instanceof List)) {
			throw new IOException(DESCRIPTOR + ": jars must be a list");
		}
		for (var item : jars == null ? List.of() : (List<?>) jars) {
			if (!(item instanceof Map<?, ?> jar)) {
				throw new IOException(DESCRIPTOR + ": each jar is a map with maven or url, and sha256");
			}
			if (jar.get("sha256") instanceof Number) {
				throw new IOException(DESCRIPTOR + ": quote the sha256 of " + jar + ", YAML reads it as a number");
			}
			var sha256 = jar.get("sha256") == null ? null : String.valueOf(jar.get("sha256")).trim().toLowerCase();
			if (sha256 != null && !SHA256.matcher(sha256).matches()) {
				throw new IOException(DESCRIPTOR + ": the sha256 of " + jar + " must be 64 hexadecimal characters");
			}
			String fileName;
			var locations = new ArrayList<String>();
			if (jar.get("maven") != null) {
				var matcher = COORDINATES.matcher(String.valueOf(jar.get("maven")).trim());
				if (!matcher.matches()) {
					throw new IOException(DESCRIPTOR + ": " + jar.get("maven") + " is not group:artifact:version[:classifier]");
				}
				var artifact = matcher.group(2);
				var version = matcher.group(3);
				fileName = artifact + "-" + version + (matcher.group(4) == null ? "" : "-" + matcher.group(4)) + ".jar";
				var path = matcher.group(1).replace('.', '/') + "/" + artifact + "/" + version + "/" + fileName;
				for (var repository : repositories) {
					locations.add(repository + "/" + path);
				}
			} else if (jar.get("url") != null) {
				var url = String.valueOf(jar.get("url")).trim();
				locations.add(url);
				fileName = jar.get("name") != null ? String.valueOf(jar.get("name")).trim()
						: url.replaceFirst("[?#].*$", "").replaceFirst("^.*/", "");
			} else {
				throw new IOException(DESCRIPTOR + ": " + jar + " needs maven or url");
			}
			if (!FILE_NAME.matcher(fileName).matches()) {
				throw new IOException(DESCRIPTOR + ": " + fileName + " is not a jar file name (give one with name)");
			}
			if (!names.add(fileName)) {
				throw new IOException(DESCRIPTOR + ": " + fileName + " is declared twice");
			}
			declared.add(new Declared(fileName, List.copyOf(locations), sha256));
		}
		return declared;
	}

	/**
	 * @return the local files of the jars a libs folder declares, in their order: the cache (downloaded if allowed),
	 *         else the jars the archive of the project carries
	 * @throws IOException when a jar is not available or its sha256 does not match
	 */
	static List<File> resolve(File libs) throws IOException {
		var descriptor = descriptor(libs);
		if (descriptor == null) {
			return List.of();
		}
		// a failure is not tried again at each check of the libraries, only once the descriptor changes or later
		var key = descriptor.getAbsolutePath() + ":" + descriptor.length() + ":" + descriptor.lastModified();
		var failure = failures.get(key);
		if (failure != null && System.currentTimeMillis() - failure.at() < retryDelay) {
			throw failure.error();
		}
		try {
			var files = new ArrayList<File>();
			for (var jar : read(descriptor)) {
				files.add(resolve(libs, jar));
			}
			failures.remove(key);
			return files;
		} catch (IOException e) {
			failures.put(key, new Failure(System.currentTimeMillis(), e));
			throw e;
		}
	}

	/** Forgets the failures and the checked files (tests). */
	static void reset() {
		failures.clear();
		checked.clear();
	}

	private static File resolve(File libs, Declared jar) throws IOException {
		if (jar.sha256() != null) {
			var cached = new File(new File(cache.get(), jar.sha256()), jar.fileName());
			if (matches(cached, jar.sha256())) {
				return cached;
			}
			var embedded = new File(new File(libs, EMBEDDED), jar.fileName());
			if (!downloadAllowed.getAsBoolean()) {
				if (matches(embedded, jar.sha256())) {
					return embedded;
				}
				throw new IOException("The jar " + jar.fileName() + " is neither downloaded (this server does not build, "
						+ "server_build none) nor carried by the archive of the project in libs/" + EMBEDDED);
			}
		} else if (!downloadAllowed.getAsBoolean()) {
			throw new IOException(DESCRIPTOR + ": the jar " + jar.fileName() + " has no sha256");
		}
		return download(jar);
	}

	private static File download(Declared jar) throws IOException {
		var failures = new ArrayList<String>();
		for (var location : jar.locations()) {
			var staging = Files.createTempFile(createdCache().toPath(), "download-", ".jar").toFile();
			try {
				var sha256 = get(location, staging);
				if (jar.sha256() == null) {
					throw new IOException(DESCRIPTOR + ": the jar " + jar.fileName() + " has no sha256; the file downloaded from "
							+ redacted(location) + " has the sha256 " + sha256 + ", declare it once checked");
				}
				if (!sha256.equals(jar.sha256())) {
					failures.add(redacted(location) + ": sha256 " + sha256 + " instead of " + jar.sha256());
					continue;
				}
				var target = new File(new File(cache.get(), sha256), jar.fileName());
				Files.createDirectories(target.getParentFile().toPath());
				Files.move(staging.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.ATOMIC_MOVE);
				Engine.logEngine.info("(ProjectJars) " + jar.fileName() + " downloaded from " + redacted(location));
				return target;
			} catch (IOException e) {
				if (jar.sha256() == null) {
					throw e;
				}
				failures.add(redacted(location) + ": " + e.getMessage());
			} catch (RuntimeException e) {
				// an invalid location or a failure of the HTTP client: the next location, else the failure of the jar
				failures.add(redacted(location) + ": " + e);
			} finally {
				Files.deleteIfExists(staging.toPath());
			}
		}
		throw new IOException("Unable to download the jar " + jar.fileName() + ": " + String.join("; ", failures));
	}

	/** Downloads a location (global symbols resolved, credentials of the URL sent as basic authentication). */
	private static String get(String location, File target) throws IOException {
		var resolved = compiled(location);
		var uri = URI.create(resolved);
		if (!"https".equals(uri.getScheme()) && !"http".equals(uri.getScheme())) {
			throw new IOException("only http and https locations are downloaded");
		}
		var request = new HttpGet(resolved.replaceFirst("^([a-z]+://)[^/@]*@", "$1"));
		if (uri.getRawUserInfo() != null) {
			request.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(
					java.net.URLDecoder.decode(uri.getRawUserInfo(), StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8)));
		}
		try (var response = HttpUtils.makeHttpClient(false).execute(request)) {
			var status = response.getStatusLine().getStatusCode();
			if (status != 200 || response.getEntity() == null) {
				throw new IOException("HTTP " + status);
			}
			try (var in = response.getEntity().getContent()) {
				return copy(in, target);
			}
		}
	}

	private static String copy(InputStream in, File target) throws IOException {
		var digest = sha256();
		var size = 0L;
		try (var out = Files.newOutputStream(target.toPath())) {
			var buffer = new byte[65536];
			for (int read; (read = in.read(buffer)) != -1;) {
				size += read;
				if (size > MAX_SIZE) {
					throw new IOException("larger than " + MAX_SIZE / 1024 / 1024 + " MB");
				}
				digest.update(buffer, 0, read);
				out.write(buffer, 0, read);
			}
		}
		return HexFormat.of().formatHex(digest.digest());
	}

	/** @return whether a file exists with this sha256, checked once for a given size and date */
	private static boolean matches(File file, String sha256) throws IOException {
		if (!file.isFile()) {
			return false;
		}
		var key = file.getAbsolutePath() + ":" + file.length() + ":" + file.lastModified();
		var known = checked.get(key);
		if (known == null) {
			var digest = sha256();
			try (var in = Files.newInputStream(file.toPath())) {
				var buffer = new byte[65536];
				for (int read; (read = in.read(buffer)) != -1;) {
					digest.update(buffer, 0, read);
				}
			}
			known = HexFormat.of().formatHex(digest.digest());
			checked.put(key, known);
		}
		return known.equals(sha256);
	}

	private static String compiled(String location) {
		try {
			return Engine.theApp != null && Engine.theApp.databaseObjectsManager != null
					? Engine.theApp.databaseObjectsManager.getCompiledValue(location) : location;
		} catch (UndefinedSymbolsException e) {
			return String.valueOf(e.incompletValue());
		}
	}

	/** @return a location without its credentials, for the logs and the errors */
	static String redacted(String location) {
		return location.replaceFirst("^([a-z]+://)[^/@]*@", "$1***@");
	}

	private static File createdCache() throws IOException {
		var dir = cache.get();
		Files.createDirectories(dir.toPath());
		return dir;
	}

	private static File defaultCache() {
		var local = LocalWorkDirectory.getDirectory("jars");
		return local != null ? local : new File(Engine.USER_WORKSPACE_PATH, "libs/.jars");
	}

	private static MessageDigest sha256() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
