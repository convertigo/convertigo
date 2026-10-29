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

package com.twinsoft.convertigo.engine.flow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

/**
 * One walk of the source files of a root (a project, or the folder of the provider of the runtimes) for the dependency
 * fingerprint of the runtimes and of the method responses. Each entry costs one read of its attributes (its type, date
 * and size), a link two more (its target and the attributes of the file it points to); the canonical path of the root is
 * read once and the entries are named relative to it. The fingerprint changes when a file is added, removed, renamed or
 * modified, when a link points elsewhere, and when the root itself resolves to another folder.
 * <p>
 * A file is shown by its date and size, as the walk before this one showed it while its content is the same: of the same
 * date and size, it is unchanged; of another size, changed, without reading it; of the same size and another date only
 * (rewritten, copied again), it is read and unchanged when its content has the hash the walk before kept, its new date
 * then kept so that it is not read again. A file is read only then: the first rewrite of a file whose hash is not known
 * yet changes the fingerprint and keeps its hash. The dates of the directories are not shown: their entries tell what
 * was added, removed or renamed, and a directory created again with the same files (a deployment of the same archive)
 * leaves the fingerprint unchanged.
 */
final class FlowSourceFingerprint {
	/** The state of a file at a walk: its date and size, the hash of its content once read, and what the fingerprint shows. */
	record FileState(long modified, long size, String hash, String shown) {
	}

	/** Larger files are not read: another date is a change. */
	static final long HASHED_SIZE_LIMIT = 4L * 1024 * 1024;

	private final Path root;
	private final Map<String, FileState> previous;
	private final Map<String, FileState> files = new HashMap<>();
	private final StringBuilder text = new StringBuilder();
	/** The directories of the current path of the walk, against the cycles of links. */
	private final Set<Object> directories = new HashSet<>();
	private int reads;

	FlowSourceFingerprint(File root) {
		this(root, null);
	}

	/** @param previous the state of the files of the walk before this one, null for none */
	FlowSourceFingerprint(File root, Map<String, FileState> previous) {
		this.previous = previous == null ? Map.of() : previous;
		var path = root.toPath().toAbsolutePath().normalize();
		try {
			path = path.toRealPath();
		} catch (IOException e) {
			// a missing root: its absolute path
		}
		this.root = path;
	}

	String text() {
		return text.toString();
	}

	/** @return the state of the files of this walk, for the next one */
	Map<String, FileState> files() {
		return files;
	}

	/** @return the files this walk read */
	int reads() {
		return reads;
	}

	/** The line naming the root: another folder behind the same path (a link moved) changes the fingerprint. */
	FlowSourceFingerprint header(String label) {
		text.append(label).append(':').append(root).append('\n');
		return this;
	}

	/** A file or a directory of the root, by its path relative to it, its whole tree for a directory. */
	FlowSourceFingerprint entry(String relative) {
		visit(relative, root.resolve(relative));
		return this;
	}

	private void visit(String relative, Path path) {
		BasicFileAttributes attributes;
		try {
			attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
		} catch (IOException e) {
			text.append("m:").append(relative).append('\n');
			return;
		}
		if (attributes.isSymbolicLink()) {
			String target;
			try {
				target = Files.readSymbolicLink(path).toString();
			} catch (IOException e) {
				target = "";
			}
			text.append("l:").append(relative).append("->").append(target).append('\n');
			try {
				attributes = Files.readAttributes(path, BasicFileAttributes.class);
			} catch (IOException e) {
				// a link to nothing
				text.append("m:").append(relative).append('\n');
				return;
			}
		}
		if (attributes.isDirectory()) {
			directory(relative, path, attributes);
		} else if (attributes.isRegularFile()) {
			file(relative, path, attributes);
		} else {
			text.append("o:").append(relative).append('\n');
		}
	}

	private void directory(String relative, Path path, BasicFileAttributes attributes) {
		var key = attributes.fileKey();
		if (key != null && !directories.add(key)) {
			text.append("cycle:").append(relative).append('\n');
			return;
		}
		try {
			text.append("d:").append(relative).append('\n');
			var names = new ArrayList<String>();
			try (var children = Files.newDirectoryStream(path)) {
				for (var child : children) {
					names.add(child.getFileName().toString());
				}
			} catch (IOException e) {
				text.append("unreadable:").append(relative).append('\n');
			}
			Collections.sort(names);
			for (var name : names) {
				visit(relative.isEmpty() ? name : relative + "/" + name, path.resolve(name));
			}
		} finally {
			if (key != null) {
				directories.remove(key);
			}
		}
	}

	private void file(String relative, Path path, BasicFileAttributes attributes) {
		long modified = attributes.lastModifiedTime().toMillis(), size = attributes.size();
		var before = previous.get(relative);
		FileState state;
		if (before != null && before.size() == size && before.modified() == modified) {
			state = before;
		} else if (before != null && before.size() == size) {
			// another date only: its content tells
			var hash = size <= HASHED_SIZE_LIMIT ? hash(path) : null;
			state = new FileState(modified, size, hash, hash != null && hash.equals(before.hash()) ? before.shown() : modified + ":" + size);
		} else {
			// new, or another size: changed
			state = new FileState(modified, size, null, modified + ":" + size);
		}
		files.put(relative, state);
		text.append("f:").append(relative).append(':').append(state.shown()).append('\n');
	}

	/** @return the hash of the content of a file, null when it cannot be read */
	private String hash(Path path) {
		reads++;
		try (var input = Files.newInputStream(path)) {
			var digest = java.security.MessageDigest.getInstance("SHA-256");
			var buffer = new byte[64 * 1024];
			for (int read; (read = input.read(buffer)) > 0;) {
				digest.update(buffer, 0, read);
			}
			return HexFormat.of().formatHex(digest.digest());
		} catch (IOException | java.security.NoSuchAlgorithmException e) {
			return null;
		}
	}
}
