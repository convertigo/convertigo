/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.tags;

import static org.junit.Assert.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.steps.XMLCopyStep;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.tags.TagManager.Scope;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/** Runs the real web clipboard entry point; Eclipse inherits these same failure scenarios. */
public class TagPasteAdapterTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	protected TagManager manager;
	protected Project source;
	protected FaultProject target;
	private GenericSequence sequence;
	private Object previousManager, previousOwner;
	private Engine previousEngine;
	private String tagId;
	protected static class FaultProject extends Project {
		Runnable afterAttachment;
		@Override public void add(DatabaseObject copy) throws EngineException {
			super.add(copy);
			if (afterAttachment != null) afterAttachment.run();
		}
	}
	private Path source(Project project) { return folder.getRoot().toPath().resolve(project.getName()).resolve("_c8oProject/tags.json"); }
	@Before public void setupAdapter() throws Exception {
		Engine.logStudio = Engine.logBeans = Engine.logEngine = Engine.logDatabaseObjectManager = Logger.getLogger("tag-paste-test");
		EnginePropertiesManager.initProperties();
		previousEngine = Engine.theApp; Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new com.twinsoft.convertigo.engine.DatabaseObjectsManager();
		source = new Project(); source.setName("Source"); source.isSubLoaded = true;
		target = new FaultProject(); target.setName("Target"); target.isSubLoaded = true;
		for (Project project : java.util.List.of(source, target)) {
			Files.createDirectories(source(project).getParent());
			TagDocument.write(source(project), new TagDocument(false).bytes(), "absent");
		}
		sequence = new GenericSequence(); sequence.setName("Copied"); sequence.isSubLoaded = true; source.add(sequence);
		var step = new XMLCopyStep(); step.setName("Child"); step.isSubLoaded = true; sequence.add(step);
		Map<String, Project> projects = Map.of("Source", source, "Target", target);
		manager = new TagManager(folder.getRoot().toPath(), projects::get, this::source, projects::keySet);
		manager.projectOpened(source); manager.projectOpened(target); source.hasChanged = target.hasChanged = false;
		manager.contributions().register("neutral", TagDocument.parseObject("{\"label\":\"Neutral\",\"fields\":{\"count\":{\"label\":\"Count\",\"type\":\"integer\"}}}"));
		var instance = TagManager.class.getDeclaredField("instance"); instance.setAccessible(true); previousManager = instance.get(null); instance.set(null, manager);
		var owner = TagManager.class.getDeclaredField("owner"); owner.setAccessible(true); previousOwner = owner.get(null); owner.set(null, Engine.theApp);
		tagId = UUID.randomUUID().toString();
	}
	@After public void restoreAdapter() throws Exception {
		var instance = TagManager.class.getDeclaredField("instance"); instance.setAccessible(true); instance.set(null, previousManager);
		var owner = TagManager.class.getDeclaredField("owner"); owner.setAccessible(true); owner.set(null, previousOwner);
		Engine.theApp = previousEngine;
	}
	protected void paste(String xml) throws Exception {
		var method = com.twinsoft.convertigo.engine.admin.services.studio.dbo.Paste.class.getDeclaredMethod("pasteInto", DatabaseObject.class, String.class, JSONObject.class);
		method.setAccessible(true);
		try { method.invoke(new com.twinsoft.convertigo.engine.admin.services.studio.dbo.Paste(), target, xml, new JSONObject()); }
		catch (InvocationTargetException e) { throw (Exception) e.getCause(); }
	}
	private String clipboard(String metadata, boolean malformedChild) throws Exception {
		var document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		var root = document.createElement("convertigo-clipboard"); root.setAttribute("beans", com.twinsoft.convertigo.engine.Version.fullProductVersion); document.appendChild(root);
		var bean = sequence.toXml(document); root.appendChild(bean);
		bean.setAttribute("c8o-tags", "{\"" + tagId + "\":{\"label\":\"Copied tag\",\"metadata\":" + metadata + "}}");
		var child = sequence.getAllSteps().get(0).toXml(document); bean.appendChild(child);
		if (malformedChild) child.setAttribute("c8o-tags", "{}");
		return XMLUtils.prettyPrintDOM(document);
	}
	private void assertNoCopy() throws Exception {
		assertTrue(target.getSequencesList().isEmpty()); assertFalse(target.hasChanged);
		var snapshot = manager.read(Scope.projectObjects, "Target");
		assertTrue(snapshot.path("tags").isEmpty()); assertTrue(snapshot.path("assignments").isEmpty()); assertFalse(snapshot.path("dirty").asBoolean());
		assertEquals(1, source.getSequencesList().size()); assertEquals(1, sequence.getAllSteps().size());
	}
	@Test public void invalidTypedMetadataLeavesNoAttachedCopy() throws Exception {
		assertThrows(IOException.class, () -> paste(clipboard("{\"neutral\":{\"count\":\"wrong\"}}", false))); assertNoCopy();
	}
	@Test public void childFailureLeavesNoAttachedSubtree() throws Exception {
		assertThrows(EngineException.class, () -> paste(clipboard("{}", true))); assertNoCopy();
	}
	@Test public void laterInvalidRootRollsBackTheWholeClipboard() throws Exception {
		var document = XMLUtils.getDefaultDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(clipboard("{}", false))));
		var invalid = XMLUtils.getDefaultDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(clipboard("{\"neutral\":{\"count\":\"wrong\"}}", false))));
		org.w3c.dom.Node invalidRoot = invalid.getDocumentElement().getFirstChild();
		while (invalidRoot.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) invalidRoot = invalidRoot.getNextSibling();
		document.getDocumentElement().appendChild(document.importNode(invalidRoot, true));
		assertThrows(IOException.class, () -> paste(XMLUtils.prettyPrintDOM(document))); assertNoCopy();
	}
	@Test public void externalConflictAfterAttachmentLeavesNoCopy() throws Exception {
		// Keep a draft so a concurrent valid edit is an actual fingerprint conflict.
		manager.mutate(Scope.projectObjects, "Target", manager.read(Scope.projectObjects, "Target").path("revision").asText(), "create", TagDocument.parseObject("{\"definition\":{\"label\":\"Existing draft\"}}"));
		target.hasChanged = false;
		var before = manager.read(Scope.projectObjects, "Target").path("tags").deepCopy();
		target.afterAttachment = () -> {
			try { Files.writeString(source(target), "{\"schemaVersion\":1,\"tags\":{},\"assignments\":{},\"projectTags\":{},\"external\":true}"); }
			catch (IOException e) { throw new RuntimeException(e); }
		};
		assertThrows(IOException.class, () -> paste(clipboard("{}", false)));
		assertTrue(target.getSequencesList().isEmpty()); assertFalse(target.hasChanged);
		var after = manager.read(Scope.projectObjects, "Target"); assertEquals(before, after.path("tags")); assertTrue(after.path("assignments").isEmpty());
		assertTrue(Files.readString(source(target)).contains("external"));
	}
	@Test public void readOnlySourceAfterAttachmentLeavesNoCopy() throws Exception {
		target.afterAttachment = () -> {
			try { Files.setPosixFilePermissions(source(target), PosixFilePermissions.fromString("r--r--r--")); }
			catch (IOException e) { throw new RuntimeException(e); }
		};
		try { assertThrows(IOException.class, () -> paste(clipboard("{}", false))); assertNoCopy(); }
		finally { Files.setPosixFilePermissions(source(target), PosixFilePermissions.fromString("rw-r--r--")); }
	}
	@Test public void successfulPasteKeepsSubtreeAndOpaqueMetadata() throws Exception {
		paste(clipboard("{\"neutral\":{\"count\":7},\"unavailable\":{\"keep\":[1,2]}}", false));
		assertEquals(1, target.getSequencesList().size()); assertEquals(1, target.getSequencesList().get(0).getAllSteps().size());
		var snapshot = manager.read(Scope.projectObjects, "Target"); assertEquals(1, snapshot.path("assignments").size());
		assertEquals("[1,2]", snapshot.path("tags").path(tagId).path("metadata").path("unavailable").path("keep").toString());
		assertTrue(target.hasChanged); assertTrue(snapshot.path("dirty").asBoolean());
	}
}
