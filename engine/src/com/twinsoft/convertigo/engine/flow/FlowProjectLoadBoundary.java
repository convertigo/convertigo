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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ReferencedProjectManager;
import com.twinsoft.convertigo.engine.util.YamlConverter;

/** Filesystem/import boundary for the active Flow source layout. It only
 * validates existing project markers; migration is an explicit offline task. */
public final class FlowProjectLoadBoundary {
	public static final String PENDING_FILE = "_private/flow-migration.pending.json";
	private static final int MAX_REFERENCE_DEPTH = 128;
	private static final Pattern FLOW_YAML_KEY = Pattern.compile(".* \\[flow\\.(?:FlowEngine|Flow)(?:-[^\\]]+)?\\]");

	private FlowProjectLoadBoundary() { }

	/** Called before the import lock and reference loading, where failures cannot
	 * be swallowed by lockAndRun or referencedProjectManager.check. */
	public static void beforeImport(File descriptor, DatabaseObjectsManager manager) throws EngineException {
		try {
			if (!descriptor.exists() && descriptor.getName().endsWith(".xml")) {
				descriptor = new File(descriptor.getParentFile(), "c8oProject.yaml");
			}
			checkLocalAdmission(descriptor.getAbsoluteFile().getParentFile().toPath());
			if (descriptor.isFile()) checkReferences(descriptor, manager, new HashSet<>(), 0);
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("FLOW_PROJECT_IMPORT_PREFLIGHT_FAILED", e);
		}
	}

	private static void checkReferences(File descriptor, DatabaseObjectsManager manager, Set<Path> seen, int depth) throws Exception {
		var root = descriptor.getCanonicalFile().getParentFile().toPath();
		if (!seen.add(root)) return; // preserve existing cyclic project imports
		if (depth > MAX_REFERENCE_DEPTH) fail("MIGRATION_WORKSPACE_LIMIT", "references");
		for (var name : ReferencedProjectManager.references(descriptor)) {
			var reference = registeredDescriptor(name.getParser().getProjectName(), manager);
			if (reference != null) {
				checkLocalAdmission(reference.getAbsoluteFile().getParentFile().toPath());
				if (reference.isFile()) checkReferences(reference, manager, seen, depth + 1);
			}
		}
	}

	private static void checkLocalAdmission(Path directory) throws Exception {
		checkLocalAdmission(directory, FlowSourceLayout.current());
	}

	/** Package-visible layout seam for import-boundary tests. */
	static void checkLocalAdmission(Path directory, FlowSourceLayout layout) throws Exception {
		// Registered project-root links are legitimate, but no marker may be a link.
		var root = directory.toFile().getCanonicalFile().toPath();
		if (safeEntry(root, PENDING_FILE) != null) fail("FLOW_PROJECT_RECOVERY_REQUIRED", root.getFileName().toString());
		var canonical = safeEntry(root, "_flow");
		if (canonical != null && !canonical.isDirectory()) fail("FLOW_PROJECT_EXPECTED_DIRECTORY", "_flow");
		var legacy = safeEntry(root, "libs/flow") != null || safeEntry(root, "libs/flows") != null;
		if (layout == FlowSourceLayout.LEGACY) {
			if (canonical != null) fail("FLOW_PROJECT_CANONICAL_RUNTIME_REQUIRED", root.getFileName().toString());
			return;
		}
		if (legacy && canonical != null) fail("FLOW_PROJECT_MIXED_LAYOUT", root.getFileName().toString());
		if (legacy) fail("FLOW_PROJECT_LEGACY_RUNTIME_REQUIRED", root.getFileName().toString());
		if (canonical == null && isFlowDefinition(root.resolve("c8oProject.yaml").toFile())) {
			fail("FLOW_PROJECT_SOURCE_REQUIRED", root.getFileName().toString());
		}
	}

	/** Reads the same YAML bean structure used by normal project loading. */
	static boolean isFlowDefinition(File descriptor) throws Exception {
		if (!descriptor.isFile() || !descriptor.getName().equals("c8oProject.yaml")) return false;
		var document = YamlConverter.readYaml(descriptor);
		var beans = document.getElementsByTagName("bean");
		for (var index = 0; index < beans.getLength(); index++) {
			var bean = (org.w3c.dom.Element) beans.item(index);
			if (FLOW_YAML_KEY.matcher(bean.getAttribute("yaml_key")).matches()) return true;
		}
		return false;
	}

	private static File registeredDescriptor(String name, DatabaseObjectsManager manager) {
		if (name == null || name.isBlank()) return null;
		var registry = manager.getStudioProjects();
		var file = registry.getProjects(false).get(name);
		if (file == null) file = registry.getProject(name);
		if (file == null && Engine.PROJECTS_PATH != null) file = Engine.projectFile(name);
		if (file != null && !file.exists() && file.getName().endsWith(".xml")) {
			file = new File(file.getParentFile(), "c8oProject.yaml");
		}
		return file;
	}

	private static BasicFileAttributes safeEntry(Path root, String relative) throws Exception {
		var segments = relative.split("/");
		var path = root;
		for (var index = 0; index < segments.length; index++) {
			path = path.resolve(segments[index]);
			BasicFileAttributes info;
			try { info = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS); }
			catch (NoSuchFileException e) { return null; }
			if (info.isSymbolicLink() || (!info.isDirectory() && index < segments.length - 1)) {
				fail("FLOW_PROJECT_UNSAFE_PATH", relative);
			}
			if (index == segments.length - 1) return info;
		}
		return null;
	}

	private static void fail(String code, String identity) throws EngineException {
		throw new EngineException(code + ": " + identity);
	}
}
