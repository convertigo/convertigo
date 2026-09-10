/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details: <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.mobile;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.mobile.ComponentRefManager.Mode;

/** Standalone regression suite for issue #1156; no running Studio required. */
public final class ComponentRefManagerTest {
	private static final String CHART = "Charts.Application.App.Chart";
	private static final String DEMO_USE = "Charts.Application.App.Demo.UseChart";
	private static final String EMPTY_USE = "Target.Application.App.Page.EmptyUse";
	private static final String WRAPPER = "Library.Application.App.Wrapper";
	private static final String TARGET_USE = "Target.Application.App.Page.UseWrapper";
	private static int checks;

	public static void main(String[] args) throws Exception {
		Engine.logEngine = org.apache.log4j.Logger.getLogger("component-ref-test");
		try {
			emptyReferencesDoNotImportLibraryDemos();
			staleEmptyReferencesAreIgnoredAndRemoved();
			validTransitiveDependenciesSurviveTargetChanges();
			similarNamesAndCircularReferences();
			System.out.println("ComponentRefManager: " + checks + " checks passed");
		} finally {
			ComponentRefManager.get(Mode.stop);
		}
	}

	private static ComponentRefManager reset() {
		return ComponentRefManager.get(Mode.stop);
	}

	private static void emptyReferencesDoNotImportLibraryDemos() throws Exception {
		var refs = reset();
		refs.addConsumer(CHART, DEMO_USE);
		for (String target : new String[] { "", " \t", null }) {
			refs.addConsumer(target, EMPTY_USE);
			check(!ComponentRefManager.isCompUsedBy(CHART, "Target"), "empty target must not import library demo");
			check(!registry(refs).containsKey(target), "invalid target must not be registered");
			check(!ComponentRefManager.isCompUsedBy(target, "Target"), "invalid target has no dependencies");
			refs.copyKey(CHART, target);
			check(!registry(refs).containsKey(target), "renaming must not introduce an invalid target");
		}
		for (String use : new String[] { "", " \t", null }) {
			refs.addConsumer(CHART, use);
		}
		check(ComponentRefManager.getCompConsumers(CHART).equals(Set.of(DEMO_USE)), "invalid consumers must not be registered");
		refs.copyKey(CHART, WRAPPER);
		check(ComponentRefManager.getCompConsumers(WRAPPER).equals(Set.of(DEMO_USE)), "valid rename preserves consumers");
	}

	private static void staleEmptyReferencesAreIgnoredAndRemoved() throws Exception {
		var refs = reset();
		refs.addConsumer(CHART, DEMO_USE);
		// Simulate the registry left by importing an empty reference before the fix.
		for (String target : new String[] { "", " \t", null }) {
			registry(refs).put(target, new HashSet<>(Set.of(EMPTY_USE)));
		}
		check(!ComponentRefManager.isCompUsedBy(CHART, "Target"), "legacy invalid keys must not propagate dependencies");
		check(ComponentRefManager.getProjectsForUpdate("Target").isEmpty(), "legacy invalid targets must not become projects to update");
		for (String target : new String[] { "", " \t", null }) {
			refs.removeConsumer(target, EMPTY_USE);
			check(!registry(refs).containsKey(target), "deleting last stale consumer removes the key");
		}
	}

	private static void validTransitiveDependenciesSurviveTargetChanges() {
		var refs = reset();
		refs.addConsumer(CHART, DEMO_USE);
		refs.addConsumer(CHART, WRAPPER + ".UseChart");
		refs.addConsumer(WRAPPER, TARGET_USE);
		check(ComponentRefManager.isCompUsedBy(CHART, "Target"), "transitive shared component dependency");
		check(ComponentRefManager.getProjectsForUpdate("Target").equals(Set.of("Charts", "Library")), "only required external projects are updated");
		refs.removeConsumer(WRAPPER, TARGET_USE);
		refs.addConsumer("", TARGET_USE);
		check(!ComponentRefManager.isCompUsedBy(CHART, "Target"), "clearing target removes transitive dependency");
		refs.removeConsumer("", TARGET_USE);
		refs.addConsumer(WRAPPER, TARGET_USE);
		check(ComponentRefManager.isCompUsedBy(CHART, "Target"), "assigning target restores transitive dependency");
		refs.addConsumer(WRAPPER, TARGET_USE + "2");
		refs.removeConsumer(WRAPPER, TARGET_USE);
		check(ComponentRefManager.isCompUsedBy(CHART, "Target"), "deleting one consumer preserves the others");
		refs.removeConsumer(WRAPPER, TARGET_USE + "2");
		check(!ComponentRefManager.isCompUsedBy(CHART, "Target"), "deleting last consumer removes transitive dependency");

		String action = "Library.Application.App.SharedAction";
		refs.addConsumer(CHART, action + ".UseChart");
		refs.addConsumer(action, TARGET_USE);
		check(ComponentRefManager.isCompUsedBy(CHART, "Target"), "shared action dependency uses the same valid traversal");
		refs.removeConsumer(action, TARGET_USE);
		check(!ComponentRefManager.isCompUsedBy(CHART, "Target"), "removing shared action call removes its dependency");
	}

	private static void similarNamesAndCircularReferences() {
		var refs = reset();
		refs.addConsumer(CHART, WRAPPER + "Extra.UseChart");
		refs.addConsumer(WRAPPER, TARGET_USE);
		check(!ComponentRefManager.isCompUsedBy(CHART, "Target"), "similar component name is not a dependency");
		refs.addConsumer(CHART, WRAPPER + ".UseChart");
		refs.addConsumer(WRAPPER, CHART + ".UseWrapper");
		check(ComponentRefManager.areCircular(CHART, WRAPPER), "valid circular references are detected");
		check(ComponentRefManager.isCompUsedBy(CHART, "Target"), "circular traversal terminates and preserves consumers");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Set<String>> registry(ComponentRefManager refs) throws Exception {
		var field = ComponentRefManager.class.getDeclaredField("consumers");
		field.setAccessible(true);
		return (Map<String, Set<String>>) field.get(refs);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
		checks++;
	}
}
