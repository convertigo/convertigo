/* Copyright (c) 2001-2026 Convertigo SA. AGPL-3.0-or-later. */
package com.twinsoft.convertigo.engine.flow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class EngineTestFixture {
	private EngineTestFixture() {
	}

	static String readModule(String name) throws IOException {
		var root = System.getProperty("engine.test.fixture");
		if (root == null) {
			root = System.getenv("FLOW_ENGINE_RESOURCE_ROOT");
		}
		return Files.readString(Path.of(root == null ? "../../lib_flow_engine/_flow" : root).resolve("modules").resolve(name));
	}
}
