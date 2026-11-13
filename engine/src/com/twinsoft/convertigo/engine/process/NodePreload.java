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

package com.twinsoft.convertigo.engine.process;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

/**
 * The Node.js preload that stops a Node.js process when the engine that launched it is gone, even when
 * the engine was killed. It is loaded with "node --require &lt;file&gt;" for a node launched directly, or
 * through NODE_OPTIONS for a tree of Node.js processes (npm, ng, ionic), each node of the tree watching its
 * own parent.
 * <p>
 * Two independent signals, both valid on Windows, macOS and Linux:
 * <ul>
 * <li>stdin: the engine keeps a pipe open on it, the system closes the pipe when the engine dies; only a
 * pipe is watched, a file, /dev/null or a terminal tells nothing about the engine;</li>
 * <li>the engine PID (CONVERTIGO_PARENT_PID, else the parent PID), checked every second; on Unix an orphan
 * is also detected by the change of its parent PID.</li>
 * </ul>
 * CONVERTIGO_PARENT_WATCH=stdin or =pid keeps only one of them, =none disables the watch. The variables
 * are removed from the environment of the process: its children watch their own parent. It is written in
 * ES5 for the old Node.js versions some projects use.
 */
public final class NodePreload {
	public static final String FILE_NAME = "convertigo-parent-watch.js";
	static final String ENV_PARENT_PID = "CONVERTIGO_PARENT_PID";

	static final String SCRIPT = """
			'use strict';
			// Loaded with "node --require <this file> ..." or NODE_OPTIONS: stops this Node.js process when the
			// Convertigo engine that launched it is gone, even if the engine was killed.
			// Two independent signals, both valid on Windows, macOS and Linux:
			// - stdin: the engine keeps a pipe open on it, the system closes the pipe when the engine dies;
			// - the engine PID (CONVERTIGO_PARENT_PID, else the parent PID), checked every second.
			// CONVERTIGO_PARENT_WATCH=stdin or =pid keeps only one of them, =none disables the watch.
			(function () {
				var env = process.env;
				var mode = env.CONVERTIGO_PARENT_WATCH || 'all';
				if (mode === 'none') {
					return;
				}
				var initialPpid = process.ppid;
				var parentPid = parseInt(env.CONVERTIGO_PARENT_PID, 10) || initialPpid;
				var interval = parseInt(env.CONVERTIGO_PARENT_WATCH_INTERVAL, 10) || 1000;
				delete env.CONVERTIGO_PARENT_PID;
				delete env.CONVERTIGO_PARENT_WATCH;
				delete env.CONVERTIGO_PARENT_WATCH_INTERVAL;
				var stopping = false;

				function stop(reason) {
					if (stopping) {
						return;
					}
					stopping = true;
					try {
						process.stderr.on('error', function () {});
						process.stderr.write('[convertigo] ' + reason + ', exiting\\n');
					} catch (e) {
					}
					process.exit(0);
				}

				if (mode !== 'pid') {
					var stdin = null;
					try {
						// a pipe only: a file, /dev/null or a terminal does not tell anything about the engine
						stdin = process.stdin instanceof require('net').Socket && !process.stdin.isTTY ? process.stdin : null;
					} catch (e) {
					}
					if (stdin) {
						stdin.on('end', function () {
							stop('stdin closed by the engine');
						});
						stdin.on('error', function () {
							stop('stdin failed');
						});
						stdin.on('data', function () {});
						stdin.resume();
						if (typeof stdin.unref === 'function') {
							stdin.unref();
						}
					}
				}

				if (mode !== 'stdin' && parentPid > 1) {
					var timer = setInterval(function () {
						// Unix: an orphan is adopted by another process
						if (parentPid === initialPpid && process.ppid !== initialPpid) {
							return stop('engine ' + parentPid + ' is gone');
						}
						try {
							process.kill(parentPid, 0);
						} catch (e) {
							if (e.code === 'ESRCH') {
								stop('engine ' + parentPid + ' is gone');
							}
						}
					}, interval);
					if (typeof timer.unref === 'function') {
						timer.unref();
					}
				}
			})();
			""";

	private NodePreload() {
	}

	/**
	 * Writes the preload in dir, unless it is already there.
	 *
	 * @return the preload file
	 */
	public static File install(File dir) throws IOException {
		File file = new File(dir, FILE_NAME);
		byte[] content = SCRIPT.getBytes(StandardCharsets.UTF_8);
		Path path = file.toPath();
		if (!file.isFile() || !Arrays.equals(content, Files.readAllBytes(path))) {
			Files.createDirectories(dir.toPath());
			Path tmp = Files.createTempFile(dir.toPath(), ".preload-", ".tmp");
			try {
				Files.write(tmp, content);
				Files.move(tmp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			} finally {
				Files.deleteIfExists(tmp);
			}
		}
		return file;
	}

	/**
	 * @return whether the executable of command is node itself, which takes the preload as an argument
	 */
	static boolean isNode(String executable) {
		String name = new File(executable).getName().toLowerCase(java.util.Locale.ROOT);
		return name.equals("node") || name.equals("node.exe");
	}

	/**
	 * Adds the preload to the NODE_OPTIONS of env, after the options already there.
	 */
	static void addToNodeOptions(Map<String, String> env, File preload) {
		String key = env.containsKey("NODE_OPTIONS") ? "NODE_OPTIONS" : findKey(env, "NODE_OPTIONS");
		String previous = env.getOrDefault(key, "");
		String option = "--require " + quote(preload.getAbsolutePath());
		env.put(key, previous.isBlank() ? option : previous.trim() + " " + option);
	}

	private static String findKey(Map<String, String> env, String name) {
		for (String key : env.keySet()) {
			if (key.equalsIgnoreCase(name)) {
				return key;
			}
		}
		return name;
	}

	/**
	 * @return path as Node.js parses NODE_OPTIONS: as is without space nor quote, else between double quotes,
	 *         where a backslash escapes the next character
	 */
	static String quote(String path) {
		if (!path.contains(" ") && !path.contains("\"") && !path.contains("\t")) {
			return path;
		}
		return "\"" + path.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}
}
