/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 */

package com.twinsoft.convertigo.engine.mobile;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import com.twinsoft.convertigo.engine.helpers.BatchOperationHelper;

/**
 * Tracks source generation independently from Studio and its live-build jobs.
 */
public final class MobileBuilderGeneration {

	public static final String STATUS_PENDING = "pending";
	public static final String STATUS_NO_CHANGE = "no_change";
	public static final String STATUS_CHANGED = "changed";
	public static final String STATUS_FAILED = "failed";

	public static final class State {
		private final long id;
		private final long startedAt;
		private final Set<String> changedFiles = ConcurrentHashMap.newKeySet();
		private volatile long completedAt;
		private volatile String status = STATUS_PENDING;
		private volatile String error = "";
		private int participants = 1;

		private State(long id, long startedAt) {
			this.id = id;
			this.startedAt = startedAt;
		}

		public long getId() {
			return id;
		}

		public long getStartedAt() {
			return startedAt;
		}

		public long getCompletedAt() {
			return completedAt;
		}

		public String getStatus() {
			return status;
		}

		public int getChangedFileCount() {
			return changedFiles.size();
		}

		public Set<String> getChangedFiles() {
			return Collections.unmodifiableSet(changedFiles);
		}

		public String getError() {
			return error;
		}

		private synchronized State retain() {
			if (STATUS_PENDING.equals(status)) {
				participants++;
			}
			return this;
		}

		private synchronized void recordFileChange(File file) {
			if (STATUS_PENDING.equals(status)) {
				changedFiles.add(file == null ? "" : file.getAbsolutePath());
			}
		}

		private synchronized void complete() {
			if (!STATUS_PENDING.equals(status)) {
				return;
			}
			if (--participants > 0) {
				return;
			}
			completedAt = System.currentTimeMillis();
			status = changedFiles.isEmpty() ? STATUS_NO_CHANGE : STATUS_CHANGED;
		}

		private synchronized void fail(String message) {
			if (!STATUS_PENDING.equals(status)) {
				return;
			}
			completedAt = System.currentTimeMillis();
			status = STATUS_FAILED;
			error = message == null ? "" : message;
		}
	}

	private static final AtomicLong sequence = new AtomicLong(System.currentTimeMillis());
	private static final ConcurrentMap<String, State> states = new ConcurrentHashMap<>();

	private MobileBuilderGeneration() {
	}

	public static State begin(String projectName) {
		String key = normalizeProjectName(projectName);
		if (key.isEmpty()) {
			throw new IllegalArgumentException("projectName is required");
		}
		long now = System.currentTimeMillis();
		State state = new State(sequence.updateAndGet(value -> Math.max(value + 1, now)), now);
		return states.compute(key, (ignored, current) ->
			current != null && STATUS_PENDING.equals(current.getStatus()) ? current.retain() : state
		);
	}

	public static void completeAfterBatch(String projectName, long id) {
		BatchOperationHelper.prepareEnd(() -> complete(projectName, id));
	}

	public static State complete(String projectName, long id) {
		State state = matchingState(projectName, id);
		if (state != null) {
			state.complete();
		}
		return state;
	}

	public static State fail(String projectName, long id, String message) {
		State state = matchingState(projectName, id);
		if (state != null) {
			state.fail(message);
		}
		return state;
	}

	public static State get(String projectName) {
		return states.get(normalizeProjectName(projectName));
	}

	public static boolean isPending(String projectName) {
		State state = get(projectName);
		return state != null && STATUS_PENDING.equals(state.getStatus());
	}

	public static void recordFileChange(String projectName, File file) {
		State state = get(projectName);
		if (state != null) {
			state.recordFileChange(file);
		}
	}

	public static boolean clear(String projectName, long id) {
		String key = normalizeProjectName(projectName);
		State state = states.get(key);
		return state != null && state.getId() == id && states.remove(key, state);
	}

	private static State matchingState(String projectName, long id) {
		State state = get(projectName);
		return state != null && state.getId() == id ? state : null;
	}

	private static String normalizeProjectName(String projectName) {
		return projectName == null ? "" : projectName.trim();
	}
}
