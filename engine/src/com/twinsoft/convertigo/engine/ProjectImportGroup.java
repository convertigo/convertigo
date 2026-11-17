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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.twinsoft.convertigo.beans.core.Project;

/** Request-thread scope for loading a group whose sources are already installed. */
public final class ProjectImportGroup implements AutoCloseable {
	private static final ThreadLocal<ProjectImportGroup> CURRENT = new ThreadLocal<>();
	private final DatabaseObjectsManager owner;
	private final Set<String> names;
	private final Map<String, Outcome> outcomes = new HashMap<>();
	private boolean closed;

	private static final class Outcome {
		Project project;
		Throwable error;
		boolean complete;
	}

	@FunctionalInterface
	interface Loader { Project load() throws EngineException; }

	private ProjectImportGroup(DatabaseObjectsManager owner, Set<String> names) {
		this.owner = owner;
		this.names = Set.copyOf(names);
	}

	static ProjectImportGroup open(DatabaseObjectsManager owner, Set<String> names) {
		if (CURRENT.get() != null) throw new IllegalStateException("A project import group is already active");
		var scope = new ProjectImportGroup(owner, names);
		CURRENT.set(scope);
		return scope;
	}

	static ProjectImportGroup current(DatabaseObjectsManager owner) {
		var scope = CURRENT.get();
		return scope != null && scope.owner == owner ? scope : null;
	}

	static ProjectImportGroup current(DatabaseObjectsManager owner, String name) {
		var scope = current(owner);
		return scope != null && name != null && scope.names.contains(name) ? scope : null;
	}

	static boolean skipsReference(DatabaseObjectsManager owner, String source, String target) {
		var scope = current(owner, source);
		return scope != null && scope.names.contains(target);
	}

	boolean completed(String name) {
		var result = outcomes.get(name);
		return result != null && result.complete;
	}

	Project completedProject(String name) throws EngineException {
		var result = outcomes.get(name);
		if (result == null || !result.complete) throw new IllegalStateException("The project import is not complete");
		if (result.error != null) throwFailure(result.error);
		return result.project;
	}

	Project load(String name, Loader loader) throws EngineException {
		var previous = outcomes.get(name);
		if (previous != null) {
			if (previous.complete) return completedProject(name);
			// Preserve the normal recursive access to a partially constructed bean;
			// only a completed outcome is reused as a successful import.
			return owner.getCachedProject(name);
		}
		var result = new Outcome();
		outcomes.put(name, result);
		try {
			result.project = loader.load();
			if (result.project == null) throw new EngineException("The project '" + name + "' could not be completely loaded");
			result.complete = true;
			return result.project;
		} catch (Throwable failure) {
			result.error = failure;
			result.complete = true;
			try { owner.clearCache(name); }
			catch (Throwable cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
			throwFailure(failure);
			throw new AssertionError("Unreachable");
		}
	}

	private static void throwFailure(Throwable failure) throws EngineException {
		if (failure instanceof EngineException engine) throw engine;
		if (failure instanceof RuntimeException runtime) throw runtime;
		if (failure instanceof Error error) throw error;
		throw new EngineException("Project import failed", failure);
	}

	@Override
	public void close() {
		if (closed) return;
		if (CURRENT.get() != this) throw new IllegalStateException("The project import group belongs to another operation");
		closed = true;
		CURRENT.remove();
		outcomes.clear();
	}
}
