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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;

/**
 * The persistent keys of the authoring responses ({@link FlowEngineBridge#persistentResponseKey}) without reading the
 * files at each call: a key shows the state of the files of the projects a response depends on, and that state is
 * kept in memory, by scope (the provider, the project, whether its engine names a frontend builder), between two
 * verifications.
 * <p>
 * A change made through an engine is seen at once. On this instance it forgets the walk of its project
 * ({@code FlowEngineBridge.forgetRootFingerprints}) or changes the revision of the data of the projects using it: the
 * next key walks it again. On the other instances of a shared workspace, the marker it writes reloads the project or
 * invalidates its data caches ({@code SharedWorkspaceSyncManager}), which does the same there.
 * <p>
 * A change made outside any engine is seen as the probes of the source revision of the runtimes see it, on demand: a
 * call finding a state verified more than {@link #recheckAge} ago (60 seconds on a server, as the snapshots of the
 * sources the providers read and those probes; 5 seconds in the Studio, whose providers read the files themselves) is
 * answered from it and starts one walk of its scope in the background, which the next calls use. A call finding a
 * state verified more than {@link #maxAge} ago waits for the walks instead, one walk of each project for all its
 * callers, as the first key after a start. Nothing reads the files while nobody asks for a key, and the thread of the
 * background walks ends once they are done.
 * <p>
 * The key computed from that state is the key computed from the files ({@link FlowEngineBridge#persistentResponseKey}):
 * the same text, its digest up to the files of the request kept for each prefix (format, versions, settings, provider,
 * method). No key while a walk shows a file too recent for its date to tell its next change: the walk is done again in
 * the background once it no longer is.
 */
final class FlowAuthoringKeys {
	/** On a server: the delay the source snapshots and the probes of the runtimes see a file changed outside the engines. */
	static final long SERVER_RECHECK_MS = 60_000;
	/**
	 * On a server, the oldest state a key is computed from: longer than the pauses of a user of the Studio, whose next
	 * call would else wait for walks of seconds on a network file system, and bounding what such a call can show.
	 */
	static final long SERVER_MAX_AGE_MS = 10 * 60_000;
	/** In the Studio, whose providers read the source files themselves, as the walks were kept before. */
	static final long STUDIO_RECHECK_MS = 5_000;
	/** In the Studio, on a local disk where a walk takes milliseconds. */
	static final long STUDIO_MAX_AGE_MS = 30_000;
	private static final int MAX_SCOPES = 256, MAX_FILES = 4096, MAX_PREFIXES = 32;

	/** The counts of the tests and of the diagnostics. */
	static final class Statistics {
		/** The walks of the projects of the keys, in the foreground or the background. */
		final LongAdder walks = new LongAdder();
		/** The keys computed from the state kept in memory, without reading any file. */
		final LongAdder reuses = new LongAdder();
		/** The scopes resolved for a key, its caller waiting. */
		final LongAdder resolutions = new LongAdder();
		/** The scopes and the files of requests verified in the background. */
		final LongAdder refreshes = new LongAdder();
	}

	static final Statistics statistics = new Statistics();

	/** What the projects of a key are resolved from. */
	record Scope(String engineFile, String projectDir, boolean frontbuilder) {
	}

	/** The projects of the keys of a scope, by their folder: each shown by its walk, or as missing. */
	private static final class Dependencies {
		/** The canonical folder of the project of the scope, empty for none, and the revision of its data when resolved. */
		final String projectRoot;
		final long dataRevision;
		/** When the projects were resolved (the clock). */
		long resolvedAt;
		/** By folder: the key of its walk (null for a missing project), and its text in the key (the text of its walk). */
		final String[] roots;
		final String[] lines;
		/** The digest of the text up to the files of the request, by its prefix: cloned for each key, never updated. */
		final Map<String, MessageDigest> digests = new ConcurrentHashMap<>();

		Dependencies(String projectRoot, long dataRevision, long resolvedAt, String[] roots, String[] lines) {
			this.projectRoot = projectRoot;
			this.dataRevision = dataRevision;
			this.resolvedAt = resolvedAt;
			this.roots = roots;
			this.lines = lines;
		}

		/**
		 * @return when what it shows was verified, and when the most recent file of its walks will no longer be recent; null
		 *         when it no longer is the state of the files known to the engine: a walk forgotten, or walked again with
		 *         another result, or another revision of the data of the project (a project it uses loaded again)
		 */
		Verified verified() {
			if (FlowEngineBridge.dataRevision(projectRoot) != dataRevision) {
				return null;
			}
			var at = resolvedAt;
			var settles = Long.MIN_VALUE;
			for (int i = 0; i < roots.length; i++) {
				if (roots[i] == null) {
					continue;
				}
				var current = FlowEngineBridge.knownRoot(roots[i]);
				// the same text is the same instance once walked again (FlowEngineBridge.walkRoot)
				if (current == null || current.value() != lines[i] && !current.value().equals(lines[i])) {
					return null;
				}
				at = Math.min(at, current.computedAt());
				if (FlowEngineBridge.unsettled(current)) {
					settles = Math.max(settles, current.newest() + FlowEngineBridge.RECENT_FILE_MS);
				}
			}
			return new Verified(at, settles);
		}

		/** @return a digest of the prefix followed by the text of its projects, to be completed by the request */
		MessageDigest digest(String prefix) throws Exception {
			var digest = digests.get(prefix);
			if (digest == null) {
				digest = MessageDigest.getInstance("SHA-256");
				update(digest, prefix);
				for (var line : lines) {
					update(digest, line);
				}
				if (digests.size() >= MAX_PREFIXES) {
					return digest;
				}
				var known = digests.putIfAbsent(prefix, digest);
				digest = known == null ? digest : known;
			}
			try {
				return (MessageDigest) digest.clone();
			} catch (CloneNotSupportedException e) {
				digests.clear();
				var copy = MessageDigest.getInstance("SHA-256");
				update(copy, prefix);
				for (var line : lines) {
					update(copy, line);
				}
				return copy;
			}
		}
	}

	/**
	 * @param at when the state was verified (the clock), the oldest of its walks and of its resolution
	 * @param settles when the most recent file of its walks will no longer be recent, Long.MIN_VALUE when none is
	 */
	private record Verified(long at, long settles) {
		boolean settled() {
			return settles == Long.MIN_VALUE;
		}
	}

	/** The line of a file named by a request, read at the date (the clock) it tells. */
	private static final class FileLine {
		final String line;
		long verifiedAt;

		FileLine(String line, long verifiedAt) {
			this.line = line;
			this.verifiedAt = verifiedAt;
		}
	}

	private static final Map<Scope, Dependencies> scopes = new ConcurrentHashMap<>();
	/** By the path the request gives. Its monitor orders the publications and the clears. */
	private static final Map<String, FileLine> files = new ConcurrentHashMap<>();
	/** Advanced by each clear of the files, under the monitor of the map: a line read before it is not kept. */
	private static long fileClears;
	/** The scopes and the files being verified in the background, each once at a time, and the end of each verification. */
	private static final Map<Object, CompletableFuture<Void>> refreshing = new ConcurrentHashMap<>();
	/** How long a caller waits for a verification that found a change before walking itself. */
	private static final long JOIN_SECONDS = 30;
	/** One thread of low priority while there is something to verify, none otherwise. */
	private static final ThreadPoolExecutor verifications = new ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(), runnable -> {
				var thread = new Thread(runnable, "AuthoringKeys");
				thread.setDaemon(true);
				thread.setPriority(Thread.MIN_PRIORITY);
				return thread;
			});

	static {
		verifications.allowCoreThreadTimeOut(true);
	}

	private FlowAuthoringKeys() {
	}

	/** @return the age from which what a key shows is verified again in the background, its callers going on */
	static long recheckAge() {
		return Engine.isStudioMode() ? STUDIO_RECHECK_MS : SERVER_RECHECK_MS;
	}

	/** @return the age from which what a key shows is no longer served, its callers waiting for it to be verified */
	static long maxAge() {
		return Engine.isStudioMode() ? STUDIO_MAX_AGE_MS : SERVER_MAX_AGE_MS;
	}

	/**
	 * @param prefix the text of the key before the projects: its format, the versions, the settings, the provider, the method
	 * @return the persistent key of a request without working copies, null while a file is too recent
	 */
	static String key(String prefix, File engineFile, JSONObject request) throws Exception {
		var scope = new Scope(engineFile == null ? "" : engineFile.getPath(), request.optString("projectDir", ""),
				request.optString("engineSource", "").contains("frontbuilder"));
		var dependencies = scopes.get(scope);
		var verified = dependencies == null ? null : dependencies.verified();
		var verifying = verified == null && dependencies != null ? refreshing.get(scope) : null;
		if (verifying != null) {
			// changed under a verification of the background, which is about to keep the new state: its result
			try {
				verifying.get(JOIN_SECONDS, TimeUnit.SECONDS);
			} catch (java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException e) {
				// walked by this caller
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
			dependencies = scopes.get(scope);
			verified = dependencies == null ? null : dependencies.verified();
		}
		var now = System.currentTimeMillis();
		if (verified == null || now - verified.at() >= maxAge()) {
			statistics.resolutions.increment();
			dependencies = resolve(scope, maxAge(), false);
			verified = dependencies.verified();
			if (verified == null) {
				// changed through the engine while resolved: this call may predate the change, its response is not kept
				return null;
			}
		} else {
			statistics.reuses.increment();
		}
		if (now - verified.at() >= recheckAge() || !verified.settled() && now >= verified.settles()) {
			verifyLater(scope);
		}
		if (!verified.settled()) {
			return null;
		}
		var digest = dependencies.digest(prefix);
		update(digest, fileLine("sourceFile", request.optString("sourceFile", "")));
		update(digest, fileLine("sourcePath", request.optString("sourcePath", "")));
		update(digest, FlowEngineBridge.canonicalJson(FlowEngineBridge.persistentRequest(request)));
		return HexFormat.of().formatHex(digest.digest());
	}

	/**
	 * Resolves the projects of a scope and shows each by its walk, the walks verified less than an age ago kept.
	 *
	 * @param settle whether a walk showing a recent file is done again once that file is no longer recent
	 */
	private static Dependencies resolve(Scope scope, long maxAge, boolean settle) throws JSONException {
		var removals = FlowEngineBridge.rootFingerprintRemovals();
		var projectRoot = FlowEngineBridge.projectRoot(scope.projectDir());
		// before anything is read: a change made meanwhile through the engine is another revision
		var revision = FlowEngineBridge.dataRevision(projectRoot);
		var resolvedAt = System.currentTimeMillis();
		var request = new JSONObject().put("projectDir", scope.projectDir()).put("engineSource", scope.frontbuilder() ? "frontbuilder" : "");
		var folders = FlowEngineBridge.persistentDependencyRoots(scope.engineFile().isEmpty() ? null : new File(scope.engineFile()), request);
		var roots = new String[folders.size()];
		var lines = new String[roots.length];
		for (int i = 0; i < roots.length; i++) {
			var folder = folders.get(i);
			if (!new File(folder, "c8oProject.yaml").isFile()) {
				lines[i] = "missing:" + FlowEngineBridge.canonicalPath(folder) + "\n";
				continue;
			}
			roots[i] = FlowEngineBridge.AUTHORING_ROOT + FlowEngineBridge.canonicalPath(folder);
			lines[i] = FlowEngineBridge.authoringRoot(roots[i], folder, maxAge, settle).value();
		}
		var resolved = new Dependencies(projectRoot, revision, resolvedAt, roots, lines);
		// forgotten since: what it shows may predate a change, the next call resolves it again
		if (FlowEngineBridge.rootFingerprintRemovals() == removals) {
			if (scopes.size() >= MAX_SCOPES) {
				scopes.clear();
			}
			scopes.put(scope, resolved);
		}
		return resolved;
	}

	/** @return the text of a file named by a request in its key, as {@link FlowEngineBridge#fileFingerprintLine} */
	private static String fileLine(String label, String path) {
		if (path == null || path.isBlank()) {
			return "";
		}
		var line = files.get(path);
		var age = line == null ? Long.MAX_VALUE : System.currentTimeMillis() - line.verifiedAt;
		if (age >= maxAge()) {
			line = readFileLine(path);
		} else if (age >= recheckAge()) {
			verifyLater(path);
		}
		return label + ":" + line.line;
	}

	private static FileLine readFileLine(String path) {
		long clears;
		synchronized (files) {
			clears = fileClears;
		}
		var line = new FileLine(FlowEngineBridge.fileFingerprintLine(new File(path)), System.currentTimeMillis());
		synchronized (files) {
			if (clears == fileClears) {
				if (files.size() >= MAX_FILES) {
					files.clear();
				}
				files.put(path, line);
			}
		}
		return line;
	}

	/**
	 * Walks were forgotten (a change made through the engine): the files of the requests are read again, and the
	 * scopes showing a forgotten walk resolved again at their next use, all of them when every walk is forgotten.
	 */
	static void forgotten(boolean all) {
		synchronized (files) {
			fileClears++;
			files.clear();
		}
		if (all) {
			scopes.clear();
		}
	}

	/** Verifies again in the background, once at a time, a scope ({@link Scope}) or the file of a request (its path). */
	private static void verifyLater(Object what) {
		var verification = new CompletableFuture<Void>();
		if (refreshing.putIfAbsent(what, verification) != null) {
			return;
		}
		try {
			verifications.execute(() -> {
				try {
					if (what instanceof Scope scope) {
						resolve(scope, recheckAge(), true);
					} else {
						readFileLine((String) what);
					}
					statistics.refreshes.increment();
				} catch (Exception | LinkageError e) {
					// verified again at the next use
					if (Engine.logEngine != null) {
						Engine.logEngine.debug("(FlowAuthoringKeys) Unable to verify the authoring key of " + what + ".", e);
					}
				} finally {
					refreshing.remove(what, verification);
					verification.complete(null);
				}
			});
		} catch (RejectedExecutionException e) {
			refreshing.remove(what, verification);
			verification.complete(null);
		}
	}

	private static void update(MessageDigest digest, String text) {
		if (!text.isEmpty()) {
			digest.update(text.getBytes(StandardCharsets.UTF_8));
		}
	}
}
