/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.beans.flow;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Publishes a validated source plan. No AST, kind or provider knowledge. */
final class FlowSourcePublisher {
	private FlowSourcePublisher() { }

	/**
	 * Writes use sibling staging files; removals follow successful writes. A failed
	 * publication restores changed files from their byte snapshots. Working copies
	 * are cleared by the owner only after success. This is in-process recovery,
	 * not a crash-atomic project transaction.
	 */
	static void publish(Map<String, String> writes, Collection<String> removals, File projectRoot) throws IOException {
		var original = new LinkedHashMap<Path, byte[]>();
		var permissions = new LinkedHashMap<Path, Set<PosixFilePermission>>();
		var paths = new LinkedHashSet<String>(writes.keySet());
		paths.addAll(removals);
		for (var key : paths) {
			var path = Path.of(key);
			original.put(path, Files.exists(path) ? Files.readAllBytes(path) : null);
			if (Files.exists(path)) {
				try { permissions.put(path, Files.getPosixFilePermissions(path)); }
				catch (UnsupportedOperationException e) { /* Non-POSIX filesystem. */ }
			}
		}
		var changed = new ArrayList<Path>();
		var createdDirectories = new LinkedHashSet<Path>();
		try {
			for (var entry : writes.entrySet()) {
				var path = Path.of(entry.getKey());
				for (var parent = path.getParent(); parent != null && !Files.exists(parent); parent = parent.getParent()) {
					createdDirectories.add(parent);
				}
				write(path, entry.getValue().getBytes(StandardCharsets.UTF_8), permissions.get(path));
				changed.add(path);
			}
			for (var key : removals) {
				var path = Path.of(key);
				if (Files.deleteIfExists(path)) changed.add(path);
			}
			// Empty source folders must not reappear during saved-source discovery.
			var root = projectRoot.getCanonicalFile().toPath();
			for (var key : removals) {
				for (var dir = Path.of(key).getParent(); dir != null && dir.startsWith(root) && !dir.equals(root); dir = dir.getParent()) {
					try { Files.deleteIfExists(dir); }
					catch (DirectoryNotEmptyException e) { break; }
				}
			}
		} catch (IOException | RuntimeException failure) {
			for (var index = changed.size() - 1; index >= 0; index--) {
				var path = changed.get(index);
				try {
					var bytes = original.get(path);
					if (bytes == null) Files.deleteIfExists(path);
					else write(path, bytes, permissions.get(path));
				} catch (IOException | RuntimeException rollbackFailure) {
					failure.addSuppressed(rollbackFailure);
				}
			}
			for (var dir : createdDirectories.stream().sorted(Comparator.comparingInt(Path::getNameCount).reversed()).toList()) {
				try { Files.deleteIfExists(dir); }
				catch (IOException | RuntimeException cleanupFailure) { failure.addSuppressed(cleanupFailure); }
			}
			throw failure;
		}
	}

	private static void write(Path path, byte[] bytes, Set<PosixFilePermission> permissions) throws IOException {
		Files.createDirectories(path.getParent());
		Path staged;
		try {
			// New sources use normal file permissions (subject to the process umask),
			// not the owner-only default of a temporary file.
			staged = Files.createTempFile(path.getParent(), ".flow-save-", ".tmp",
					PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-rw-rw-")));
		} catch (UnsupportedOperationException e) {
			staged = Files.createTempFile(path.getParent(), ".flow-save-", ".tmp");
		}
		try {
			Files.write(staged, bytes);
			if (permissions != null) Files.setPosixFilePermissions(staged, permissions);
			try { Files.move(staged, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
			catch (AtomicMoveNotSupportedException e) { Files.move(staged, path, StandardCopyOption.REPLACE_EXISTING); }
		} finally {
			Files.deleteIfExists(staged);
		}
	}
}
