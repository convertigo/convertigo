/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import org.apache.commons.fileupload.ProgressListener;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.tools.tar.TarInputStream;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.LocalWorkDirectory;
import com.twinsoft.convertigo.engine.process.ProcessTrees;

/** Installs one official distribution; downloaded or manually supplied archives follow the same checks. */
final class NodeRuntimeInstaller {

	private final String version, rootName, archiveName, archiveUrl, checksumUrl;
	private final boolean windows;
	private final Path destination, archives;
	private final ProgressListener progress;
	private final Consumer<String> status;
	private String step = "preparing the runtime";

	NodeRuntimeInstaller(String version, String platform, File destination, File archives,
			ProgressListener progress, Consumer<String> status) {
		if (!version.matches("v\\d+\\.\\d+\\.\\d+(?:-[A-Za-z0-9.-]+)?")) {
			throw new IllegalArgumentException("Invalid Node.js version: " + version);
		}
		this.version = version;
		windows = platform.startsWith("win-");
		rootName = "node-" + version + "-" + platform;
		archiveName = rootName + (windows ? ".zip" : ".tar.gz");
		archiveUrl = "https://nodejs.org/dist/" + version + "/" + archiveName;
		checksumUrl = "https://nodejs.org/dist/" + version + "/SHASUMS256.txt";
		this.destination = destination.toPath().toAbsolutePath();
		this.archives = archives.toPath().toAbsolutePath();
		this.progress = progress;
		this.status = status;
	}

	File install() throws IOException, InterruptedException {
		Path partial = null, staging = null, supplied = null;
		try {
			Path source = destination.getParent().resolve(archiveName);
			if (!Files.isRegularFile(source)) source = archives.resolve(archiveName);
			if (Files.isRegularFile(source)) {
				supplied = source;
				message("Using local Node.js archive: " + source);
			} else {
				step = "downloading the archive";
				message("Downloading Node.js " + version + " from " + archiveUrl);
				Files.createDirectories(archives);
				partial = archives.resolve(archiveName + ".part-" + UUID.randomUUID());
				download(partial);
				source = partial;
			}
			step = "checking the archive";
			verifyChecksum(source);
			Files.createDirectories(destination.getParent());
			staging = Files.createTempDirectory(destination.getParent(), ".node-install-");
			LocalWorkDirectory.beginWork(staging.toFile());
			step = "extracting the archive";
			message("Extracting Node.js " + version + " from " + archiveName);
			Path runtime = extract(source, staging, rootName, windows);
			step = "checking the Node.js executable";
			Path executable = runtime.resolve(windows ? "node.exe" : "bin/node");
			verifyVersion(executable, version);
			message("Node.js archive and executable match " + rootName);
			step = "installing the runtime";
			// An existing incomplete directory is kept aside, never reused or overwritten piecemeal.
			Path previous = null;
			if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
				previous = destination.resolveSibling(destination.getFileName() + ".incomplete-" + UUID.randomUUID());
				Files.move(destination, previous);
				message("Kept the previous incomplete Node.js directory at " + previous);
			}
			try {
				try {
					Files.move(runtime, destination, StandardCopyOption.ATOMIC_MOVE);
				} catch (java.nio.file.AtomicMoveNotSupportedException e) {
					Files.move(runtime, destination);
				}
			} catch (IOException e) {
				if (previous != null) Files.move(previous, destination);
				throw e;
			}
			if (partial != null) {
				try {
					// Do not replace an archive supplied by another instance or by the user meanwhile.
					Files.move(partial, archives.resolve(archiveName));
				} catch (IOException e) {
					Engine.logEngine.debug("Node.js is installed but its download could not be retained", e);
				}
			}
			message("Node.js " + version + " is ready in " + destination);
			return (windows ? destination : destination.resolve("bin")).toFile();
		} catch (IOException | IllegalArgumentException e) {
			Path manualDirectory = supplied == null ? archives : supplied.getParent();
			String help = "Unable to prepare Node.js " + version + " while " + step + ": " + e.getMessage()
					+ "\nDownload this exact archive manually: " + archiveUrl
					+ "\nPlace " + archiveName + " unchanged in " + manualDirectory + " and retry. Do not unpack it."
					+ "\nIf available, also place SHASUMS256.txt from " + checksumUrl + " in that directory."
					+ "\nAn existing local archive is kept; replace it if it failed verification.";
			Engine.logEngine.warn(help);
			if (status != null) status.accept(help);
			throw new IOException(help, e);
		} finally {
			if (partial != null) {
				try { Files.deleteIfExists(partial); }
				catch (IOException e) { Engine.logEngine.debug("Cannot remove the incomplete Node.js download", e); }
			}
			if (staging != null) {
				FileUtils.deleteQuietly(staging.toFile());
				if (!Files.exists(staging)) LocalWorkDirectory.endWork(staging.toFile());
			}
		}
	}

	private void message(String text) {
		Engine.logEngine.info(text);
		if (status != null) status.accept(text);
	}

	private CloseableHttpResponse get(String url, int timeout) throws IOException {
		if (Engine.theApp == null || Engine.theApp.httpClient4 == null) {
			throw new IOException("The engine HTTP client is unavailable");
		}
		var request = new HttpGet(url);
		request.setConfig(RequestConfig.custom().setConnectTimeout(timeout).setSocketTimeout(timeout)
				.setConnectionRequestTimeout(timeout).setRedirectsEnabled(true).build());
		var response = Engine.theApp.httpClient4.execute(request);
		int code = response.getStatusLine().getStatusCode();
		if (code != 200 || response.getEntity() == null) {
			response.close();
			throw new IOException("HTTP " + code + " from " + url);
		}
		return response;
	}

	private void download(Path file) throws IOException {
		try (var response = get(archiveUrl, 30_000);
				var input = response.getEntity().getContent();
				var output = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW)) {
			long length = response.getEntity().getContentLength(), total = 0, nextProgress = 0;
			byte[] buffer = new byte[1024 * 1024];
			int size;
			while ((size = input.read(buffer)) != -1) {
				output.write(buffer, 0, size);
				total += size;
				if (progress != null && System.currentTimeMillis() >= nextProgress) {
					progress.update(total, length, 1);
					nextProgress = System.currentTimeMillis() + 2000;
				}
			}
			if (length >= 0 && total != length) throw new IOException("Incomplete download (" + total + "/" + length + " bytes)");
			if (progress != null) progress.update(total, length, 1);
		}
	}

	private void verifyChecksum(Path archive) throws IOException {
		message("Checking SHA-256 for " + archiveName);
		String expected = null;
		for (Path directory : new Path[] {archive.getParent(), archives}) {
			Path manifest = directory.resolve("SHASUMS256.txt");
			if (Files.isRegularFile(manifest)) {
				try (var input = Files.newInputStream(manifest)) {
					expected = checksum(input);
				}
				if (expected != null) break;
			}
		}
		if (expected == null) {
			String manifest;
			try (var response = get(checksumUrl, 5_000); var input = response.getEntity().getContent()) {
				manifest = readManifest(input);
			} catch (IOException unavailable) {
				checksumUnavailable();
				return;
			}
			expected = checksum(manifest);
			if (expected == null) {
				checksumUnavailable();
				return;
			}
		}
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			try (var input = Files.newInputStream(archive)) {
				byte[] buffer = new byte[1024 * 1024];
				int size;
				while ((size = input.read(buffer)) != -1) digest.update(buffer, 0, size);
			}
			if (!expected.equalsIgnoreCase(HexFormat.of().formatHex(digest.digest()))) {
				throw new IOException("SHA-256 mismatch for " + archiveName + "; the archive was not extracted");
			}
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IOException("SHA-256 is unavailable", e);
		}
		message("SHA-256 verified for " + archiveName);
	}

	private void checksumUnavailable() {
		String warning = "SHA-256 is unavailable for " + archiveName
				+ "; only the archive layout and executable version will be checked."
				+ " For offline SHA-256 verification, place SHASUMS256.txt from " + checksumUrl + " beside the archive.";
		Engine.logEngine.warn(warning);
		if (status != null) status.accept(warning);
	}

	private String checksum(InputStream input) throws IOException {
		return checksum(readManifest(input));
	}

	private static String readManifest(InputStream input) throws IOException {
		byte[] bytes = input.readNBytes(1024 * 1024 + 1);
		if (bytes.length > 1024 * 1024) throw new IOException("SHASUMS256.txt is too large");
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private String checksum(String manifest) throws IOException {
		String found = null;
		for (String line : manifest.split("\\R")) {
			String[] fields = line.trim().split("\\s+", 2);
			if (fields.length != 2 || !fields[1].replaceFirst("^\\*", "").equals(archiveName)) continue;
			if (!fields[0].matches("[0-9a-fA-F]{64}") || found != null) {
				throw new IOException("Invalid or duplicate SHA-256 entry for " + archiveName);
			}
			found = fields[0];
		}
		return found;
	}

	/** Zip extraction is also tested on Unix; symbolic links are created only after every regular file. */
	static Path extract(Path archive, Path staging, String rootName, boolean zip) throws IOException {
		Path root = staging.resolve(rootName);
		Map<Path, Path> links = new LinkedHashMap<>();
		if (zip) {
			// Check the central directory as well as the entry CRCs read by ZipInputStream.
			try (var index = new java.util.zip.ZipFile(archive.toFile())) { index.size(); }
			try (var input = new ZipInputStream(Files.newInputStream(archive))) {
				for (var entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
					Path target = entryPath(root, rootName, entry.getName());
					if (entry.isDirectory()) Files.createDirectories(target);
					else writeEntry(input, target);
				}
			}
		} else {
			try (var gzip = new GZIPInputStream(Files.newInputStream(archive)); var input = new TarInputStream(gzip, "UTF-8")) {
				for (var entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
					Path target = entryPath(root, rootName, entry.getName());
					if (entry.isDirectory()) Files.createDirectories(target);
					else if (entry.isSymbolicLink()) {
						Path link = Path.of(entry.getLinkName());
						if (link.isAbsolute() || !target.getParent().resolve(link).normalize().startsWith(root)) {
							throw new IOException("Archive link leaves its Node.js directory: " + entry.getName());
						}
						if (links.putIfAbsent(target, link) != null) throw new IOException("Duplicate archive link: " + entry.getName());
					} else if (entry.isFile() && input.canReadEntryData(entry)) {
						writeEntry(input, target);
						if ((entry.getMode() & 0111) != 0 && !target.toFile().setExecutable(true, false)) {
							throw new IOException("Cannot make the Node.js file executable: " + target);
						}
					} else throw new IOException("Unsupported Node.js archive entry: " + entry.getName());
				}
				// TAR end records precede the GZIP trailer: read it too to check CRC and truncation.
				gzip.transferTo(java.io.OutputStream.nullOutputStream());
			}
		}
		for (var link : links.entrySet()) {
			Files.createDirectories(link.getKey().getParent());
			if (!link.getKey().getParent().toRealPath().startsWith(root.toRealPath())
					|| !link.getKey().getParent().resolve(link.getValue()).toRealPath().startsWith(root.toRealPath())) {
				throw new IOException("Archive link does not resolve inside its Node.js directory: " + link.getKey());
			}
			Files.createSymbolicLink(link.getKey(), link.getValue());
		}
		Path binary = root.resolve(zip ? "node.exe" : "bin/node");
		if (!Files.isRegularFile(binary, LinkOption.NOFOLLOW_LINKS)) {
			throw new IOException("Archive does not contain the expected Node.js executable in " + rootName);
		}
		if (!zip && !Files.isExecutable(binary)) {
			throw new IOException("Node.js cannot be executed from " + binary + "; check filesystem permissions and execution policy");
		}
		if (!Files.isRegularFile(root.resolve(zip ? "npm.cmd" : "bin/npm"))
				|| !Files.isRegularFile(root.resolve("LICENSE"), LinkOption.NOFOLLOW_LINKS)) {
			throw new IOException("Incomplete Node.js archive: npm or LICENSE is missing from " + rootName);
		}
		return root;
	}

	private static Path entryPath(Path root, String rootName, String name) throws IOException {
		if (name.indexOf('\\') >= 0 || name.indexOf(':') >= 0
				|| (!name.equals(rootName) && !name.startsWith(rootName + "/"))) {
			throw new IOException("Archive does not match the expected Node.js directory " + rootName + ": " + name);
		}
		Path target = root.getParent().resolve(name).normalize();
		if (!target.startsWith(root)) throw new IOException("Archive entry leaves its Node.js directory: " + name);
		return target;
	}

	private static void writeEntry(InputStream input, Path target) throws IOException {
		Files.createDirectories(target.getParent());
		try (var output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
			input.transferTo(output);
		}
	}

	static void verifyVersion(Path executable, String version) throws IOException, InterruptedException {
		Process process = new ProcessBuilder(executable.toString(), "--version").redirectErrorStream(true).start();
		try {
			if (!process.waitFor(10, TimeUnit.SECONDS)) throw new IOException("Node.js --version did not finish within 10 seconds");
			String actual;
			try (var input = process.getInputStream()) {
				actual = new String(input.readNBytes(256), StandardCharsets.UTF_8).trim();
			}
			if (process.exitValue() != 0 || !actual.equals(version)) {
				throw new IOException("The archive's Node.js executable does not report " + version);
			}
		} finally {
			if (process.isAlive()) ProcessTrees.stop(process.toHandle(), Duration.ofSeconds(2));
		}
	}
}
