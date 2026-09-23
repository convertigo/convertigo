package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.engine.EngineException;

@RunWith(Parameterized.class)
public class FlowSourceLayoutTest {
	@Parameterized.Parameters(name = "{0}")
	public static Iterable<Object[]> layouts() {
		return Arrays.stream(FlowSourceLayout.values()).map(value -> new Object[] { value }).toList();
	}

	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private final FlowSourceLayout layout;
	public FlowSourceLayoutTest(FlowSourceLayout layout) { this.layout = layout; }

	private Project project(File directory) {
		return new Project() { @Override public File getDirFile() { return directory; } };
	}
	private Flow flow(Project project) throws Exception {
		var flow = new Flow() {
			@Override public Project getProject() { return project; }
			@Override protected FlowSourceLayout sourceLayout() { return layout; }
		};
		flow.setName("Proof");
		return flow;
	}
	private FlowEngine model(Project project) {
		return new FlowEngine() {
			@Override public Project getProject() { return project; }
			@Override protected FlowSourceLayout sourceLayout() { return layout; }
		};
	}
	private void write(Path path, String text) throws Exception {
		Files.createDirectories(path.getParent());
		Files.writeString(path, text);
	}

	@Test public void sourcePathsAreExplicitAndRejectTraversal() {
		assertEquals(FlowSourceLayout.FLOW, FlowSourceLayout.current());
		assertEquals(layout.root() + "/engine.yaml", layout.path("engine.yaml"));
		assertEquals(layout.root(), layout.path(""));
		for (var path : new String[] { "../escape", "a/../b", "/absolute", "C:/absolute", "a\\b", "a//b", "./a", "a/" }) {
			assertThrows(path, IllegalArgumentException.class, () -> layout.path(path));
		}
	}

	@Test public void canonicalHttpIgnoreKeepsUserRulesAndTheirLineEnding() throws Exception {
		var root = folder.newFolder().toPath();
		var ignore = root.resolve(".httpignore");
		Files.writeString(ignore, "# user rule\r\n!/_flow/resources/**\r\n");
		layout.ensureHttpIgnore(root.toFile());
		if (layout == FlowSourceLayout.LEGACY) {
			assertEquals("# user rule\r\n!/_flow/resources/**\r\n", Files.readString(ignore));
			return;
		}
		var expected = "# user rule\r\n!/_flow/resources/**\r\n/_flow/\r\n";
		assertEquals(expected, Files.readString(ignore));
		layout.ensureHttpIgnore(root.toFile());
		assertEquals(expected, Files.readString(ignore));
	}

	@Test public void canonicalHttpIgnoreCreatesTheMissingFileAndPreservesLf() throws Exception {
		var root = folder.newFolder().toPath();
		var ignore = root.resolve(".httpignore");
		layout.ensureHttpIgnore(root.toFile());
		if (layout == FlowSourceLayout.LEGACY) {
			assertFalse(Files.exists(ignore));
			return;
		}
		assertEquals("/_flow/" + System.lineSeparator(), Files.readString(ignore));
		Files.writeString(ignore, "# user rule\n");
		layout.ensureHttpIgnore(root.toFile());
		assertEquals("# user rule\n/_flow/\n", Files.readString(ignore));
	}

	@Test public void canonicalHttpIgnoreRejectsLinkedFiles() throws Exception {
		org.junit.Assume.assumeTrue(layout == FlowSourceLayout.FLOW);
		var root = folder.newFolder().toPath();
		var ignore = root.resolve(".httpignore");
		Files.writeString(root.resolve("target"), "# keep\n");
		Files.createSymbolicLink(ignore, Path.of("target"));
		assertThrows(IOException.class, () -> layout.ensureHttpIgnore(root.toFile()));
		Files.delete(ignore);
		Files.createSymbolicLink(ignore, Path.of("missing-target"));
		assertThrows(IOException.class, () -> layout.ensureHttpIgnore(root.toFile()));
	}

	@Test public void failedCanonicalHttpIgnoreWritePreventsFlowSourceCreation() throws Exception {
		org.junit.Assume.assumeTrue(layout == FlowSourceLayout.FLOW);
		var root = folder.newFolder().toPath();
		Files.createDirectory(root.resolve(".httpignore"));
		var flow = flow(project(root.toFile()));
		flow.setFlowSource("export default {};\n");
		assertThrows(EngineException.class, flow::saveFlowSourceFile);
		assertFalse(Files.exists(root.resolve("_flow/flows/Proof.flow.js")));
	}

	@Test public void classificationAndInvalidationUseTheSameRoot() {
		var other = layout == FlowSourceLayout.LEGACY ? FlowSourceLayout.FLOW : FlowSourceLayout.LEGACY;
		for (var path : new String[] { "Engine.js", "modules/source-layout.js", "lib/helper.js" }) {
			assertTrue(layout.requiresRuntimeInvalidation(layout.path(path)));
			assertTrue(layout.requiresRuntimeInvalidation("/" + layout.path(path).replace('/', '\\')));
			assertFalse(layout.requiresRuntimeInvalidation(other.path(path)));
		}
		var source = layout.path("frontbuilder/svelte/model/App/src/routes/+page.flow.svelte");
		assertTrue(layout.isFrontendAuthoringSource(source));
		assertTrue(layout.isFrontendDocument(source));
		assertTrue(layout.isFrontendDocument("/project/" + source));
		assertTrue(layout.isFrontendDocument(("C:/project/" + source).replace('/', '\\')));
		assertFalse(layout.isFrontendDocument(other.path("frontbuilder/svelte/model/a.flow.svelte")));
		assertFalse(layout.isFrontendDocument(layout.path("resources/a.flow.svelte")));
		assertFalse(layout.isFrontendAuthoringSource(layout.path("frontbuilder/svelte/components/widget.svelte")));
		assertFalse(layout.requiresRuntimeInvalidation(source));
		assertFalse(layout.isFrontendDocument(null));
	}

	@Test public void flowDraftSaveDiscardAndReloadStayInSelectedLayout() throws Exception {
		var root = folder.newFolder().toPath();
		var project = project(root.toFile());
		var path = root.resolve(layout.flows()).resolve("Proof.flow.js");
		var other = layout == FlowSourceLayout.LEGACY ? FlowSourceLayout.FLOW : FlowSourceLayout.LEGACY;
		var inactive = root.resolve(other.flows()).resolve("Proof.flow.js");
		write(path, "official");
		write(inactive, "inactive");
		var flow = flow(project);
		assertEquals(path.toFile(), flow.getFlowSourceFile());
		assertEquals("official", flow.getFlowSource());
		flow.setFlowSource("draft");
		assertTrue(flow.isFlowSourceDirty());
		assertEquals("official", Files.readString(path));
		flow.discardFlowSource();
		assertEquals("official", flow.getFlowSource());
		flow.setFlowSource("saved");
		flow.saveFlowSourceFile();
		assertFalse(flow.isFlowSourceDirty());
		assertEquals("saved", flow(project).getFlowSource());
		assertEquals("inactive", Files.readString(inactive));
	}

	@Test public void watcherAndExplicitRefreshShareExclusiveChangeKinds() {
		assertEquals(FlowSourceLayout.ChangeKind.FLOW, layout.changeKind(layout.flows() + "/Proof.flow.js"));
		assertEquals(FlowSourceLayout.ChangeKind.FLOW, layout.changeKind(layout.flows() + "/.flow-drafts/Proof.flow.js"));
		assertEquals(FlowSourceLayout.ChangeKind.ICON, layout.changeKind(layout.path("icons/widget.svg")));
		assertEquals(FlowSourceLayout.ChangeKind.RUNTIME, layout.changeKind(layout.path("modules/layout.js")));
		assertEquals(FlowSourceLayout.ChangeKind.FRONTEND, layout.changeKind(layout.path("frontbuilder/svelte/model/App/src/routes/+page.flow.svelte")));
		assertEquals(FlowSourceLayout.ChangeKind.FRONTEND, layout.changeKind(layout.path("frontbuilder/svelte/.flow-drafts/page.flow.svelte")));
		assertEquals(FlowSourceLayout.ChangeKind.CATALOG, layout.changeKind(layout.path("blocks/widget.block.js")));
		assertEquals(FlowSourceLayout.ChangeKind.CATALOG, layout.changeKind(layout.path("engine.yaml")));
		assertEquals(FlowSourceLayout.ChangeKind.CATALOG, layout.changeKind(layout.path("frontbuilder/svelte/components/Widget.flow.svelte")));
		var other = layout == FlowSourceLayout.LEGACY ? FlowSourceLayout.FLOW : FlowSourceLayout.LEGACY;
		assertEquals(FlowSourceLayout.ChangeKind.OUTSIDE, layout.changeKind(other.flows() + "/Proof.flow.js"));
		assertEquals(FlowSourceLayout.ChangeKind.OUTSIDE, layout.changeKind(other.path("engine.yaml")));
		assertEquals(FlowSourceLayout.ChangeKind.OUTSIDE, layout.changeKind("DisplayObjects/mobile/app.js"));
		assertEquals(FlowSourceLayout.ChangeKind.OUTSIDE, layout.changeKind(null));
		assertEquals(FlowSourceLayout.ChangeKind.FLOW, layout.changeKind((layout.flows() + "/Proof.flow.js").replace('/', '\\')));
	}

	@Test public void engineAndFrontendDraftsPersistOnlyOnExport() throws Exception {
		var root = folder.newFolder().toPath();
		var project = project(root.toFile());
		var config = root.resolve(layout.path("engine.yaml"));
		var frontend = root.resolve(layout.path("frontbuilder/svelte/model/App/src/routes/+page.flow.svelte"));
		write(config, "official config");
		write(frontend, "official page");
		var engine = model(project);
		assertEquals("official config", engine.getEngineSource());
		engine.setEngineSource("draft config");
		engine.setFrontendSource(frontend.toString(), "draft page");
		assertEquals("draft config", engine.getEngineSource());
		assertEquals("official config", Files.readString(config));
		assertEquals("official page", Files.readString(frontend));
		assertEquals("draft page", engine.getSourceDrafts().get(frontend.toFile().getCanonicalPath()));
		assertTrue(engine.isFrontendSourceDirty(frontend.toString()));
		engine.toXml(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
		assertEquals("draft config", model(project).getEngineSource());
		assertEquals("draft page", Files.readString(frontend));
		assertFalse(engine.isFrontendSourceDirty(frontend.toString()));
		engine.setFrontendSource(frontend.toString(), "discard me");
		Flow.projectUnloaded(project);
		assertEquals("draft page", model(project).getFrontendSource(frontend.toString()));
	}

	@Test public void rhinoPathFactoryAgreesWithJava() throws Exception {
		var resourceRoot = System.getenv("FLOW_ENGINE_RESOURCE_ROOT");
		org.junit.Assume.assumeTrue("Set FLOW_ENGINE_RESOURCE_ROOT to run the cross-repository contract", resourceRoot != null);
		var source = Files.readString(Path.of(resourceRoot, "modules/source-layout.js"));
		var cx = Context.enter();
		try {
			cx.setLanguageVersion(Context.VERSION_ES6);
			var scope = cx.initStandardObjects();
			var module = (Scriptable) cx.evaluateString(scope, source, "source-layout.js", 1, null);
			var create = (Function) ScriptableObject.getProperty(module, "create");
			var paths = (Scriptable) create.call(cx, scope, module, new Object[] { layout.key() });
			assertEquals(layout.root(), Context.toString(ScriptableObject.getProperty(paths, "root")));
			assertEquals(layout.flows(), Context.toString(ScriptableObject.getProperty(paths, "flows")));
			var path = (Function) ScriptableObject.getProperty(paths, "path");
			assertEquals(layout.path("frontbuilder/svelte"), Context.toString(path.call(cx, scope, paths, new Object[] { "frontbuilder/svelte" })));
		} finally {
			Context.exit();
		}
	}

	@Test public void bridgeSuppliesItsLayoutBeforeEngineEvaluation() throws Exception {
		var cx = Context.enter();
		try {
			var scope = cx.initStandardObjects();
			var engine = new File(folder.getRoot(), layout.path("Engine.js"));
			FlowEngineBridge.initializeSourceScope(scope, engine);
			assertEquals(FlowSourceLayout.current().key(), Context.toString(ScriptableObject.getProperty(scope, "__flowSourceLayout")));
			assertEquals(engine.getParentFile().getAbsolutePath(), Context.toString(ScriptableObject.getProperty(scope, "__flowEngineDir")));
		} finally {
			Context.exit();
		}
	}
}
