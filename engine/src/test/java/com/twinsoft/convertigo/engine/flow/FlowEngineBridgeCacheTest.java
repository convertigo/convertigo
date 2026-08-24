/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FlowEngineBridgeCacheTest {
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
}
