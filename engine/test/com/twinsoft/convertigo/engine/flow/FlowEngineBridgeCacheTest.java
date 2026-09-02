/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.EngineException;

public class FlowEngineBridgeCacheTest {
	@Test
	public void aggregatesNestedPerformanceDurationsWithoutMovingTheProfileCursor() throws Exception {
		var response = new JSONObject();
		var owner = FlowStudioSupport.startPerformanceProfile(true, "nested-proof");
		FlowStudioSupport.performanceProfileAdd("frontend.model.authoring.serialize", 1_500_000L);
		FlowStudioSupport.performanceProfileAdd("frontend.model.authoring.serialize", 2_500_000L);
		FlowStudioSupport.finishPerformanceProfile(owner, response);

		var profile = response.getJSONObject("_profile");
		assertEquals(4.0, profile.getJSONObject("phases")
				.getDouble("frontend.model.authoring.serialize"), 0.001);
		assertTrue(profile.getJSONObject("phases").has("service.response"));
	}

	@Test
	public void exposesAnUnambiguousRhinoRevealEntryPoint() throws Exception {
		assertEquals(void.class, FlowEngineBridge.class
				.getMethod("notifySourceMutationWithReveal", String.class, String.class, boolean.class)
				.getReturnType());
	}

	@Test
	public void preservesBrowserDescriptorsForAdminEvents() throws Exception {
		var payload = FlowEngineBridge.browserEventPayload(new JSONObject()
					.put("kind", "frontbuilder.svelte.dev")
					.put("project", "Demo")
					.put("url", "/convertigo/gw/ticket/")
					.toString());

		assertEquals("Demo", payload.getString("project"));
		assertEquals("frontbuilder.svelte.dev", payload.getString("kind"));
	}

	@Test
	public void distinguishesRuntimeSourcesFromAuthoringData() {
		assertTrue(FlowEngineBridge.requiresRuntimeCacheInvalidation("libs/flow/Engine.js"));
		assertTrue(FlowEngineBridge.requiresRuntimeCacheInvalidation("libs/flow/modules/runtime.js"));
		assertTrue(FlowEngineBridge.requiresRuntimeCacheInvalidation("libs\\flow\\lib\\helper.js"));

		assertFalse(FlowEngineBridge.requiresRuntimeCacheInvalidation("libs/flow/engine.yaml"));
		assertFalse(FlowEngineBridge.requiresRuntimeCacheInvalidation("libs/flow/blocks/list/map.block.js"));
		assertFalse(FlowEngineBridge.requiresRuntimeCacheInvalidation(
				"libs/flow/frontbuilder/svelte/model/app/src/routes/+page.flow.svelte"));
	}

	@Test
	public void recognizesFrontendModelsAndDrafts() {
		assertTrue(FlowEngineBridge.isFrontendAuthoringSourcePath(
				"libs/flow/frontbuilder/svelte/model/app/src/routes/+page.flow.svelte"));
		assertTrue(FlowEngineBridge.isFrontendAuthoringSourcePath(
				"/libs/flow/frontbuilder/svelte/.flow-drafts/draft/src/routes/+page.flow.svelte"));

		assertFalse(FlowEngineBridge.isFrontendAuthoringSourcePath(
				"libs/flow/frontbuilder/svelte/components/Text.flow.svelte"));
		assertFalse(FlowEngineBridge.isFrontendAuthoringSourcePath("libs/flows/MyFlow.flow.js"));
	}

	@Test
	public void serializesOnlyFrontendDocumentProviderCalls() throws Exception {
		assertTrue(FlowEngineBridge.usesFrontendDocumentProvider("authoringTree",
				new JSONObject().put("surface", "frontend")));
		assertTrue(FlowEngineBridge.usesFrontendDocumentProvider("authoringPalette",
				new JSONObject().put("surface", "frontend")));
		assertTrue(FlowEngineBridge.usesFrontendDocumentProvider("propertyEditor",
				new JSONObject().put("frontendSourceDrafts", new JSONObject())));
		assertTrue(FlowEngineBridge.usesFrontendDocumentProvider("describeTree",
				new JSONObject().put("target", "engine").put("frontendSourceDrafts", new JSONObject())));
		assertTrue(FlowEngineBridge.usesFrontendDocumentProvider("applySourceMutation",
				new JSONObject().put("target", "frontendSource").put("frontendSourceDrafts", new JSONObject())));

		assertFalse(FlowEngineBridge.usesFrontendDocumentProvider("authoringTree",
				new JSONObject().put("surface", "backend")));
		assertFalse(FlowEngineBridge.usesFrontendDocumentProvider("propertyEditor", new JSONObject()));
		assertFalse(FlowEngineBridge.usesFrontendDocumentProvider("applyMutation",
				new JSONObject().put("surface", "frontend").put("frontendSourceDrafts", new JSONObject())));
	}

	@Test
	public void exposesFrontendAuthoringLockMetrics() throws Exception {
		Class<?> statsClass = null;
		for (var candidate : FlowEngineBridge.class.getDeclaredClasses()) {
			if ("InvocationStats".equals(candidate.getSimpleName())) {
				statsClass = candidate;
				break;
			}
		}
		assertTrue("InvocationStats must remain available", statsClass != null);

		var constructor = statsClass.getDeclaredConstructor(String.class, String.class);
		constructor.setAccessible(true);
		var stats = constructor.newInstance("lib_flow_engine.Engine", "authoringPalette");
		var record = statsClass.getDeclaredMethods();
		java.lang.reflect.Method recordMethod = null;
		java.lang.reflect.Method toJsonMethod = null;
		for (var method : record) {
			if ("record".equals(method.getName())) {
				recordMethod = method;
			} else if ("toJson".equals(method.getName())) {
				toJsonMethod = method;
			}
		}
		assertTrue("InvocationStats.record must remain available", recordMethod != null);
		assertTrue("InvocationStats.toJson must remain available", toJsonMethod != null);
		recordMethod.setAccessible(true);
		toJsonMethod.setAccessible(true);
		recordMethod.invoke(stats, 12_000_000L, null, false, true, true, true,
				5_000_000L, 7_000_000L, 3_000_000L, 1_000_000L);

		var json = (JSONObject) toJsonMethod.invoke(stats);
		assertEquals(1L, json.getLong("authoringLockCalls"));
		assertEquals(1L, json.getLong("authoringLockContentions"));
		assertEquals(5.0, json.getDouble("authoringLockWaitTotalMs"), 0.001);
		assertEquals(7.0, json.getDouble("authoringLockHeldTotalMs"), 0.001);
		assertEquals(3.0, json.getDouble("methodFingerprintTotalMs"), 0.001);
		assertEquals(1.0, json.getDouble("methodCacheLookupTotalMs"), 0.001);

		var cacheInfo = FlowEngineBridge.class.getDeclaredMethod("bridgeCacheInfo", String.class);
		cacheInfo.setAccessible(true);
		var bridgeInfo = (JSONObject) cacheInfo.invoke(null, "lib_flow_engine.Engine");
		assertTrue(bridgeInfo.has("frontendAuthoringLocks"));
		assertEquals(32, bridgeInfo.getInt("frontendAuthoringSingleFlightLimit"));
		assertTrue(bridgeInfo.has("frontendAuthoringSingleFlightActive"));
		assertTrue(bridgeInfo.has("frontendAuthoringSingleFlightFollowers"));
	}

	@Test
	public void servesOptimisticReadsOnlyOutsideMutationWindows() {
		var gate = new FlowEngineBridge.FrontendAuthoringMutationGate();
		var initial = gate.readStamp();
		assertTrue(initial >= 0);
		assertTrue(gate.canServe(initial));

		gate.beginMutation();
		assertEquals(-1L, gate.readStamp());
		assertFalse(gate.canServe(initial));
		gate.endMutation();

		var after = gate.readStamp();
		assertTrue(after > initial);
		assertTrue(gate.canServe(after));
		assertFalse(gate.canServe(initial));
	}

	@Test
	public void sharesIdenticalFrontendAuthoringMisses() throws Exception {
		var leader = FlowEngineBridge.acquireFrontendAuthoringFlight("same-authoring-request");
		var follower = FlowEngineBridge.acquireFrontendAuthoringFlight("same-authoring-request");
		try {
			assertNotNull(leader);
			assertTrue(leader.leader());
			assertNotNull(follower);
			assertFalse(follower.leader());

			leader.complete(new JSONObject().put("ok", true).put("value", "shared"));
			assertEquals("shared", follower.await().getString("value"));
		} finally {
			FlowEngineBridge.releaseFrontendAuthoringFlight(leader);
			FlowEngineBridge.releaseFrontendAuthoringFlight(follower);
		}
	}

	@Test
	public void propagatesSingleFlightLeaderFailures() {
		var leader = FlowEngineBridge.acquireFrontendAuthoringFlight("failing-authoring-request");
		var follower = FlowEngineBridge.acquireFrontendAuthoringFlight("failing-authoring-request");
		try {
			leader.completeExceptionally(new EngineException("expected single-flight failure"));
			try {
				follower.await();
				throw new AssertionError("The follower must receive the leader failure.");
			} catch (EngineException e) {
				assertEquals("expected single-flight failure", e.getMessage());
			}
		} finally {
			FlowEngineBridge.releaseFrontendAuthoringFlight(leader);
			FlowEngineBridge.releaseFrontendAuthoringFlight(follower);
		}
	}

	@Test
	public void boundsIndependentFrontendAuthoringMisses() {
		var leaders = new java.util.ArrayList<FlowEngineBridge.FrontendAuthoringFlight>();
		try {
			for (var i = 0; i < 32; i++) {
				var flight = FlowEngineBridge.acquireFrontendAuthoringFlight("bounded-authoring-request-" + i);
				assertNotNull(flight);
				assertTrue(flight.leader());
				leaders.add(flight);
			}
			assertNull(FlowEngineBridge.acquireFrontendAuthoringFlight("bounded-authoring-overflow"));
		} finally {
			leaders.forEach(FlowEngineBridge::releaseFrontendAuthoringFlight);
		}
	}

	@Test
	public void cachesFrontendAuthoringTreesWithTheirStrictRequestFingerprint() throws Exception {
		var method = FlowEngineBridge.class.getDeclaredMethod("isCacheableMethod", String.class, JSONObject.class);
		method.setAccessible(true);
		var request = new JSONObject()
				.put("surface", "frontend")
				.put("focusPath", "frontends.svelte.routes.home.structure.title")
				.put("frontendSourceDrafts", new JSONObject().put("page.flow.svelte", "draft"));

		assertTrue((boolean) method.invoke(null, "authoringTree", request));
		assertFalse((boolean) method.invoke(null, "applySourceMutation", request));
	}
}
