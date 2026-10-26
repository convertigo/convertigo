/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class TimingAllocationSpanTest {
	private static volatile Object retained;
	@Before public void setup() throws Exception {
		Engine.logEngine = Logger.getLogger("timing-test");
		EnginePropertiesManager.initProperties();
		EnginePropertiesManager.setProperty(PropertyName.TIMING_TRACE_ALLOCATIONS, "true");
		EnginePropertiesManager.setProperty(PropertyName.XML_COUNT_DOCUMENTS, "true");
		XmlUseCounter.begin();
	}

	@org.junit.After public void cleanup() { XmlUseCounter.end(); EnginePropertiesManager.unload(); }

	@Test public void countsDocumentsAndChildThreadAllocationWithoutDoubleCounting() throws Exception {
		var child = new AtomicReference<TimingTrace.Span>();
		var root = TimingTrace.open("test", "root");
		try (root) {
			XMLUtils.createDom();
			var worker = new Thread(() -> {
				var span = TimingTrace.open("worker", "child");
				child.set(span);
				try (span) {
					try {
						XMLUtils.getDefaultDocumentBuilder().parse(new InputSource(new StringReader("<ok/>")));
						retained = new byte[1_000_000];
					} catch (Exception e) { throw new AssertionError(e); }
				}
			});
			worker.start(); worker.join();
		}
		assertEquals(2, root.documents());
		assertEquals(1, child.get().documents());
		assertTrue(child.get().allocatedBytes() >= 1_000_000);
		assertTrue(root.allocatedBytes() >= child.get().allocatedBytes());
		try (var next = TimingTrace.open("test", "next")) {
			assertEquals(0, next.documents());
		}
	}

	@Test public void preservesNoLogAndCountsSameThreadChildrenOnce() {
		MDC.put("nolog", "true");
		var root = TimingTrace.open("test", "root");
		TimingTrace.Span nested;
		try (root) {
			nested = TimingTrace.open("test", "nested");
			try (nested) { retained = new byte[4_000_000]; }
			assertEquals("true", MDC.get("nolog"));
		}
		assertEquals("true", MDC.get("nolog"));
		MDC.remove("nolog");
		assertTrue(root.allocatedBytes() >= nested.allocatedBytes());
		assertTrue(root.allocatedBytes() < nested.allocatedBytes() + 1_000_000);
	}

	@Test public void disabledDiagnosticsDoNotOpenSpans() {
		EnginePropertiesManager.setProperty(PropertyName.TIMING_TRACE_ALLOCATIONS, "false");
		assertNull(TimingTrace.open("test", "disabled"));
	}
}
