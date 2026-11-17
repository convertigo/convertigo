/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.Accessibility;

public class FlowGenericViewTest {
	private static Flow flow(String source) throws Exception {
		var flow = new Flow();
		flow.setName("Slow");
		flow.setFlowSource(source);
		return flow;
	}

	@Test public void longAndUnreadableSourcesAreReadWithoutRunningTheBridge() throws Exception {
		var longSource = "const _flow = { accessibility: 'Private', authenticatedContextRequired: true };\n"
				+ "x += 1;\n".repeat(100_000);
		var slow = flow(longSource);
		slow.setAccessibility(Accessibility.Hidden);
		var unreadable = flow("const _flow = { accessibility: compute() };\n");
		var plain = flow("function Flow() { return 1; }\n");
		var commented = flow("// const _flow = { accessibility: 'Private' };\nfunction Flow() { return 1; }\n");
		// stored values that differ from the Private default show that the view reads them
		plain.setAccessibility(Accessibility.Public);
		commented.setAccessibility(Accessibility.Public);
		long started = System.nanoTime();
		long calls = FlowEngineBridge.syncInputsAttempts();
		long runtimeCalls = FlowBridgeProbe.calls();
		try (var view = FlowGenericView.open()) {
			assertEquals(Accessibility.Private, slow.getAccessibility());
			assertTrue(slow.getAuthenticatedContextRequired());
			assertTrue(slow.getVariablesList().isEmpty());
			assertFalse(slow.hasVariables());
			assertEquals(0, slow.numberOfVariables());
			assertNull(slow.getVariable("missing"));
			assertTrue(slow.getFlowVirtualChildren().isEmpty());
			assertTrue(slow.getDatabaseObjectChildren().isEmpty());
			assertTrue(slow.getAllChildren().isEmpty());
			assertFalse(slow.hasDatabaseObjectChildren());
			assertEquals(Accessibility.Private, unreadable.getAccessibility());
			assertTrue(unreadable.getAuthenticatedContextRequired());
			assertEquals(Accessibility.Public, plain.getAccessibility());
			assertFalse(plain.getAuthenticatedContextRequired());
			// a mention in a comment is not a reference: the stored property decides, as the runtime finds no header
			assertEquals(Accessibility.Public, commented.getAccessibility());
		}
		assertEquals(calls, FlowEngineBridge.syncInputsAttempts());
		assertEquals(runtimeCalls, FlowBridgeProbe.calls());
		assertTrue("generic accessors took over one second", System.nanoTime() - started < TimeUnit.SECONDS.toNanos(1));
		assertFalse(FlowGenericView.active());
		var accessibility = RequestableObject.class.getDeclaredField("accessibility");
		accessibility.setAccessible(true);
		assertEquals(Accessibility.Hidden, accessibility.get(slow));
	}

	@Test public void scopesDoNotEscapeToAnotherThreadOrAfterAnException() throws Exception {
		try {
			try (var outer = FlowGenericView.open()) {
				try (var inner = FlowGenericView.open()) {
					assertTrue(FlowGenericView.active());
					var workerValue = new AtomicReference<Boolean>();
					var worker = new Thread(() -> workerValue.set(FlowGenericView.active()));
					worker.start();
					worker.join();
					assertEquals(Boolean.FALSE, workerValue.get());
				}
				assertTrue(FlowGenericView.active());
				throw new IllegalStateException("test");
			}
		} catch (IllegalStateException expected) { }
		assertFalse(FlowGenericView.active());
		var scope = FlowGenericView.open();
		var wrongThread = new AtomicReference<Throwable>();
		var closer = new Thread(() -> {
			try { scope.close(); } catch (Throwable failure) { wrongThread.set(failure); }
		});
		closer.start();
		closer.join();
		assertTrue(wrongThread.get() instanceof IllegalStateException);
		assertTrue(FlowGenericView.active());
		scope.close();
		scope.close();
		assertFalse(FlowGenericView.active());
	}

	@Test public void symbolsWalkerFinishesForALongFlowAndClosesTheScope() throws Exception {
		var previous = Engine.theApp;
		Engine.logBeans = Engine.logEngine = Engine.logDatabaseObjectManager = Logger.getLogger("generic-view-test");
		var slow = flow("const _flow = { accessibility: 'Private' };\n" + "x += 1;\n".repeat(100_000));
		var project = new Project() {
			@Override public List<Sequence> getSequencesList() { return List.of(slow); }
		};
		project.setName("GenericViewTest");
		var manager = new DatabaseObjectsManager();
		var projects = DatabaseObjectsManager.class.getDeclaredField("projects");
		projects.setAccessible(true);
		projects.set(manager, new HashMap<>(java.util.Map.of(project.getName(), project)));
		try {
			Engine.theApp = new Engine();
			Engine.theApp.databaseObjectsManager = manager;
			long started = System.nanoTime();
			long calls = FlowEngineBridge.syncInputsAttempts();
			long runtimeCalls = FlowBridgeProbe.calls();
			manager.symbolsCheck();
			assertEquals(calls, FlowEngineBridge.syncInputsAttempts());
			assertEquals(runtimeCalls, FlowBridgeProbe.calls());
			assertTrue("symbolsCheck took over one second", System.nanoTime() - started < TimeUnit.SECONDS.toNanos(1));
			assertFalse(FlowGenericView.active());
		} finally { Engine.theApp = previous; }
	}

	@Test public void uncertainFlowFormsFailClosed() throws Exception {
		var sources = List.of(
			"const a = 1; const _flow = {accessibility:'Private'};",
			"export const _flow = {accessibility:'Private'};",
			"_flow = {accessibility:'Private'};",
			"var a=1,\n _flow={accessibility:'Private'};",
			"const _flow = {accessibility:'Public'}; _flow.accessibility = 'Private';",
			"const _flow = {accessibility:'Public'}; Object.assign(_flow, {accessibility:'Private'});",
			"const _flow = {accessibility:'Public'}; `${_flow.accessibility = 'Private'}`;",
			"const _flow = {accessibility:'Public'}, other=1;",
			"const _flow = {accessibility:'Public'} + dynamic;",
			"const _flow = {authenticatedContextRequired:'false'}; _flow.accessibility='Private';");
		long calls = FlowEngineBridge.syncInputsAttempts();
		try (var view = FlowGenericView.open()) {
			for (String source : sources) {
				var candidate = flow(source);
				assertEquals(source, Accessibility.Private, candidate.getAccessibility());
				assertTrue(source, candidate.getAuthenticatedContextRequired());
			}
		}
		assertEquals(calls, FlowEngineBridge.syncInputsAttempts());
	}

	@Test public void headerValuesAreReadLikeTheRuntimeReadsThem() throws Exception {
		long calls = FlowEngineBridge.syncInputsAttempts();
		try (var view = FlowGenericView.open()) {
			// the runtime trims an accessibility name and ignores its case
			assertEquals(Accessibility.Hidden, flow("const _flow = {accessibility:'hidden'};\n").getAccessibility());
			assertEquals(Accessibility.Public, flow("const _flow = {accessibility:' PUBLIC '};\n").getAccessibility());
			assertEquals(Accessibility.Private, flow("const _flow = {accessibility:'publik'};\n").getAccessibility());
			assertEquals(Accessibility.Private, flow("const _flow = {accessibility:1};\n").getAccessibility());
			// it reads a true or false text as a boolean, and rejects anything else
			var text = flow("const _flow = {authenticatedContextRequired:'false'};\n");
			text.setAuthenticatedContextRequired(true);
			assertFalse(text.getAuthenticatedContextRequired());
			assertTrue(flow("const _flow = {authenticatedContextRequired:' TRUE '};\n").getAuthenticatedContextRequired());
			assertTrue(flow("const _flow = {authenticatedContextRequired:'yes'};\n").getAuthenticatedContextRequired());
			assertTrue(flow("const _flow = {authenticatedContextRequired:true};\n").getAuthenticatedContextRequired());
			// a comment after the header keeps it certain
			assertEquals(Accessibility.Public, flow("const _flow = {accessibility:'Public'} // shown to all\nfunction Flow() { return 1; }\n").getAccessibility());
			assertEquals(Accessibility.Public, flow("const _flow = {accessibility:'Public'}; /* note */\n").getAccessibility());
		}
		assertEquals(calls, FlowEngineBridge.syncInputsAttempts());
	}

	@Test public void generatedHeadersNamingTheirFilesStayCertain() throws Exception {
		// generated Flows name _flow paths in comments and strings, and carry headers far beyond 64 KB
		var rows = new StringBuilder();
		for (int i = 0; i < 3_000; i++) rows.append("      { \"column\": \"c").append(i).append("\", \"path\": \"_flow/resources/sql/t.json\" },\n");
		var source = "// @generated; owner: _flow/data/model.yaml\n"
				+ "import { rows } from \"lib_flow_sql/rows\";\n"
				+ "const _flow = {\n  accessibility: 'Hidden',\n  config: { url: \"jdbc:hsqldb:file:.//_flow/resources/db\", columns: [\n" + rows + "  ] }\n};\n"
				+ "function Flow({ input }) {\n  if({ condition: input.x, $$then: function () { return default(input.y, 'z'); } })\n  return 1;\n}\n";
		assertTrue(source.length() > 64 * 1024);
		long calls = FlowEngineBridge.syncInputsAttempts();
		try (var view = FlowGenericView.open()) {
			assertEquals(Accessibility.Hidden, flow(source).getAccessibility());
		}
		assertEquals(calls, FlowEngineBridge.syncInputsAttempts());
	}

	@Test public void declaredInputsAreShownWithoutChangingTheStoredVariables() throws Exception {
		var gateway = flow("const _flow = {\n  accessibility: 'Hidden',\n  inputs: {\n"
				+ "    entity: { type: 'string', required: true, description: 'Entity name' },\n"
				+ "    operation: { type: 'string', default: 'list' },\n"
				+ "    payload: { type: 'object' }\n  }\n};\nfunction Flow({ input }) { return input; }\n");
		var aliased = flow("const _flow = { inputs: { flag: { type: 'bool' } } };\nfunction Flow() { return 1; }\n");
		long calls = FlowEngineBridge.syncInputsAttempts();
		var previous = Engine.theApp;
		// a default text is compiled against the global symbols, as a synchronization does
		Engine.logBeans = Engine.logEngine = Engine.logDatabaseObjectManager = Logger.getLogger("generic-view-test");
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager();
		try (var view = FlowGenericView.open()) {
			var variables = gateway.getVariablesList();
			assertEquals(List.of("entity", "operation", "payload"), variables.stream().map(v -> v.getName()).toList());
			assertEquals("Entity name", variables.get(0).getDescription());
			assertTrue(variables.get(0).isRequired());
			assertEquals("list", variables.get(1).getValueOrNull());
			assertEquals(3, gateway.numberOfVariables());
			assertTrue(gateway.hasVariables());
			assertEquals("payload", gateway.getVariable("payload").getName());
			assertEquals("operation", gateway.getVariable(1).getName());
			// a spelling only the runtime normalizes keeps the stored variables
			assertTrue(aliased.getVariablesList().isEmpty());
		} finally {
			Engine.theApp = previous;
		}
		// stored variables keep their place, an undeclared one goes, a new one follows, as after a synchronization
		var ordered = flow(gateway.getFlowSource());
		for (var name : List.of("payload", "stale", "entity")) {
			var stored = new com.twinsoft.convertigo.beans.variables.RequestableVariable();
			stored.setName(name);
			ordered.addVariable(stored);
		}
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager();
		try (var view = FlowGenericView.open()) {
			assertEquals(List.of("payload", "entity", "operation"), ordered.getVariablesList().stream().map(v -> v.getName()).toList());
			assertEquals("Entity name", ordered.getVariable("entity").getDescription());
		} finally {
			Engine.theApp = previous;
		}
		assertEquals(calls, FlowEngineBridge.syncInputsAttempts());
		// nothing was written: the stored variables are untouched
		assertTrue(gateway.getAllVariables().isEmpty());
		assertFalse(FlowGenericView.active());
	}

	@Test public void aHeaderLineTheRuntimeReadsKeepsTheViewClosed() throws Exception {
		// the runtime reads its header line by line and knows no block comment
		var commented = flow("import { call } from \"requestable\";\n/*\nconst _flow = { accessibility: 'private' };\n*/\nfunction Flow() { return 1; }\n");
		// an import alias named _flow is not blanked: the source is not read as headerless
		var aliased = flow("import { a as _flow } from \"x\";\nfunction Flow() { return 1; }\n");
		long calls = FlowEngineBridge.syncInputsAttempts();
		try (var view = FlowGenericView.open()) {
			assertEquals(Accessibility.Private, commented.getAccessibility());
			assertTrue(commented.getAuthenticatedContextRequired());
			assertEquals(Accessibility.Private, aliased.getAccessibility());
		}
		assertEquals(calls, FlowEngineBridge.syncInputsAttempts());
	}

	@Test public void continuationsAndHiddenMentionsMakeTheHeaderUncertain() throws Exception {
		var sources = List.of(
			// an expression continued on the next line
			"const _flow = {accessibility:'Public'}\n.constructor;",
			"const _flow = {accessibility:'Public'}\n[0];",
			"const _flow = {accessibility:'Public'}\n(other);",
			"const _flow = {accessibility:'Public'}\ninstanceof Object;",
			// an apostrophe inside a nested template must not hide a later mutation
			"const _flow = {accessibility:'Public'};\nconst t = `${rows.map(r => `<p>l'objet</p>`)}`;\n_flow.accessibility = 'Private';\n",
			// a library name holds the word only as part of an identifier, a mutation names it
			"const _flow = {accessibility:'Public'};\nimport x from 'lib_flow_sql';\n_flow['accessibility'] = 'Private';\n");
		long calls = FlowEngineBridge.syncInputsAttempts();
		try (var view = FlowGenericView.open()) {
			for (String source : sources) {
				assertEquals(source, Accessibility.Private, flow(source).getAccessibility());
			}
			// part of an identifier is not a mention
			assertEquals(Accessibility.Public, flow("const _flow = {accessibility:'Public'};\nconst lib_flow_sql = 1, __flowHandle = 2;\n").getAccessibility());
		}
		assertEquals(calls, FlowEngineBridge.syncInputsAttempts());
	}
}
