/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.flow;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Filesystem layout only; never interprets Flow source or block properties. */
public enum FlowSourceLayout {
	LEGACY("legacy", "libs/flow", "libs/flows"),
	FLOW("_flow", "_flow", "_flow/flows");

	private final String key;
	private final String root;
	private final String flows;
	private static final String HTTP_IGNORE_FILE = ".httpignore";
	private static final String HTTP_IGNORE_RULE = "/_flow/";

	FlowSourceLayout(String key, String root, String flows) {
		this.key = key;
		this.root = root;
		this.flows = flows;
	}

	/** Coordinated publication gate. No directory probing or per-request override. */
	public static FlowSourceLayout current() {
		return FLOW;
	}

	public String key() { return key; }
	public String root() { return root; }
	public String flows() { return flows; }

	/**
	 * Makes the canonical Flow source root non-public before a source writer can
	 * create files below it. Existing user rules are retained verbatim.
	 */
	public synchronized void ensureHttpIgnore(File projectDirectory) throws IOException {
		if (this != FLOW) {
			return;
		}
		if (projectDirectory == null) {
			throw new IOException("Missing project directory for Flow HTTP protection");
		}
		Path directory = projectDirectory.toPath();
		Files.createDirectories(directory);
		Path ignore = directory.resolve(HTTP_IGNORE_FILE);
		if (Files.exists(ignore, LinkOption.NOFOLLOW_LINKS)
				&& !Files.isRegularFile(ignore, LinkOption.NOFOLLOW_LINKS)) {
			throw new IOException("Expected a regular " + HTTP_IGNORE_FILE + " file: " + ignore);
		}
		String rules = Files.exists(ignore, LinkOption.NOFOLLOW_LINKS) ? Files.readString(ignore, StandardCharsets.UTF_8) : "";
		if (hasFinalHttpIgnoreRule(rules)) {
			return;
		}
		String eol = lineEnding(rules);
		String separator = rules.isEmpty() || rules.endsWith("\n") || rules.endsWith("\r") ? "" : eol;
		Path temporary = Files.createTempFile(directory, ".httpignore-", ".tmp");
		try {
			Files.writeString(temporary, rules + separator + HTTP_IGNORE_RULE + eol, StandardCharsets.UTF_8);
			Files.move(temporary, ignore, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private static boolean hasFinalHttpIgnoreRule(String rules) {
		String[] lines = rules.split("\\R", -1);
		for (int i = lines.length - 1; i >= 0; i--) {
			String line = lines[i];
			if (!line.isEmpty() && !line.startsWith("#")) {
				return HTTP_IGNORE_RULE.equals(line);
			}
		}
		return false;
	}

	private static String lineEnding(String value) {
		int lf = value.indexOf('\n');
		if (lf >= 0) {
			return lf > 0 && value.charAt(lf - 1) == '\r' ? "\r\n" : "\n";
		}
		return value.indexOf('\r') >= 0 ? "\r" : System.lineSeparator();
	}

	public enum ChangeKind { OUTSIDE, ICON, FLOW, RUNTIME, FRONTEND, CATALOG }

	/** Shared by filesystem watching and explicit editor refresh. Flow sources
	 * must be classified before the catalog because _flow/flows is nested.
	 */
	public ChangeKind changeKind(String relative) {
		var path = normalize(relative);
		if (path.startsWith(path("icons") + "/")) return ChangeKind.ICON;
		if (path.startsWith(flows + "/")) return ChangeKind.FLOW;
		if (!path.startsWith(root + "/")) return ChangeKind.OUTSIDE;
		if (requiresRuntimeInvalidation(path)) return ChangeKind.RUNTIME;
		if (isFrontendAuthoringSource(path)) return ChangeKind.FRONTEND;
		return ChangeKind.CATALOG;
	}

	public String path(String relative) {
		if (relative == null || relative.isEmpty()) return root;
		if (relative.startsWith("/") || relative.matches("^[A-Za-z]:.*") || relative.contains("\\")) {
			throw new IllegalArgumentException("Expected a Flow-root-relative path: " + relative);
		}
		for (var part : relative.split("/", -1)) {
			if (part.isEmpty() || part.equals(".") || part.equals("..")) {
				throw new IllegalArgumentException("Expected a Flow-root-relative path: " + relative);
			}
		}
		return root + "/" + relative;
	}

	public boolean requiresRuntimeInvalidation(String relative) {
		var path = normalize(relative).replaceFirst("^/+", "");
		return path.equals(path("Engine.js")) || path.startsWith(path("modules") + "/")
				|| path.startsWith(path("lib") + "/");
	}

	public boolean isFrontendAuthoringSource(String relative) {
		var path = normalize(relative).replaceFirst("^/+", "");
		return path.startsWith(path("frontbuilder") + "/")
				&& (path.contains("/model/") || path.contains("/.flow-drafts/"));
	}

	/** Classification, not path authorization; callers still enforce project containment. */
	public boolean isFrontendDocument(String source) {
		var path = normalize(source);
		var prefix = path("frontbuilder") + "/";
		return (path.startsWith(prefix) || path.contains("/" + prefix))
				&& (path.endsWith(".flow.svelte") || path.endsWith(".flow.css")
						|| path.endsWith(".front.json") || path.endsWith(".uiblock.json"));
	}

	private static String normalize(String path) {
		return path == null ? "" : path.replace('\\', '/');
	}
}
