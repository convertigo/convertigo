package com.twinsoft.convertigo.engine.tags;

import static org.junit.Assert.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.steps.IfStep;
import com.twinsoft.convertigo.beans.steps.XMLCopyStep;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.tags.TagManager.Scope;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.YamlConverter;

public class TagManagerTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private Path root;
	private final Map<String, Project> projects = new HashMap<>();
	private Project project;
	private GenericSequence sequence, neighbor;
	private GenericSequence first, second;
	private TagManager manager;

	@Before public void setup() throws Exception {
		root = folder.getRoot().toPath();
		Engine.logBeans = Engine.logEngine = Engine.logDatabaseObjectManager = Logger.getLogger("tags-test");
		EnginePropertiesManager.initProperties();
		project = project("Demo"); sequence = sequence(project, "Old"); neighbor = sequence(project, "Old2");
		first = sequence(project, "First"); second = sequence(project, "Second");
		manager = new TagManager(root, projects::get, this::source, projects::keySet);
		manager.projectOpened(project); project.hasChanged = false;
	}
	private Project project(String name) throws Exception {
		Project project = new Project(); project.setName(name); project.isSubLoaded = true;
		projects.put(name, project); Files.createDirectories(root.resolve(name)); return project;
	}
	private GenericSequence sequence(Project project, String name) throws Exception {
		var sequence = new GenericSequence(); sequence.setName(name); sequence.isSubLoaded = true; project.add(sequence); return sequence;
	}
	private XMLCopyStep step(GenericSequence parent, String name) throws Exception {
		var step = new XMLCopyStep(); step.setName(name); step.isSubLoaded = true; parent.add(step); return step;
	}
	private Path source(Project project) { return root.resolve(project.getName()).resolve("_c8oProject/tags.json"); }
	private ObjectNode json(String value) throws java.io.IOException { return TagDocument.parseObject(value); }
	private ObjectNode read(Scope scope) throws Exception { return manager.read(scope, project.getName()); }
	private ObjectNode command(Scope scope, String action, ObjectNode input) throws Exception {
		return manager.mutate(scope, project.getName(), read(scope).path("revision").asText(), action, input);
	}
	private String create(Scope scope, String label) throws Exception {
		return command(scope, "create", json("{\"definition\":{\"label\":\"" + label + "\",\"metadata\":{}}}")).path("id").asText();
	}
	private void assign(Scope scope, String id, String... targets) throws Exception {
		ObjectNode input = json("{\"tagIds\":[\"" + id + "\"]}"); var array = input.putArray("targets"); for (String target : targets) array.add(target);
		command(scope, "assign", input);
	}
	private void save(Project project) throws Exception { manager.save(project, source(project), () -> {}); project.hasChanged = false; }
	private void reference(Project owner, String name, String target) throws Exception {
		var reference = new com.twinsoft.convertigo.beans.references.ProjectSchemaReference();
		reference.setName(name); reference.setProjectName(target); reference.isSubLoaded = true; owner.add(reference);
	}

	@Test public void referenceTagPreviewIncludesRootTransitiveReferencesAndCyclesWithoutWriting() throws Exception {
		var lib = project("Lib"); var leaf = project("Leaf");
		reference(project, "Library", "Lib=https://github.com/example/lib.git");
		reference(project, "SameLibrary", "Lib"); reference(lib, "Leaf", "Leaf"); reference(leaf, "Cycle", "Demo");
		var schema = new com.twinsoft.convertigo.beans.references.ImportXsdSchemaReference();
		schema.setName("SchemaOnly"); schema.isSubLoaded = true; project.add(schema);
		ObjectNode before = read(Scope.workspaceProjects);
		var preview = manager.read(Scope.workspaceProjects, null, "Demo");
		assertEquals(List.of("Demo", "Lib", "Leaf"), TagDocument.JSON.convertValue(preview.path("referenceTargets"), List.class));
		assertEquals(before, read(Scope.workspaceProjects));
		assertFalse(Files.exists(root.resolve("studio/tags.json"))); assertFalse(Files.exists(source(project)));
	}

	@Test public void referenceTagCreationIsOneLocalRevisionCheckedCommandAndMembershipsStayEditable() throws Exception {
		var lib = project("Lib"); var leaf = project("Leaf");
		reference(project, "Library", "Lib"); reference(lib, "Leaf", "Leaf");
		ObjectNode input = json("{\"project\":\"Demo\",\"definition\":{\"label\":\"Application\",\"shared\":true,\"presentation\":{\"color\":\"#2563EB\"}}}");
		var result = command(Scope.workspaceProjects, "createFromReferences", input);
		String id = result.path("id").asText(); TagDocument.validateId(id);
		assertTrue(result.path("done").asBoolean()); assertEquals(3, result.path("affectedTargets").size());
		assertEquals("", result.path("affectedContainers").get(0).asText()); assertTrue(result.path("dirtyProjects").isEmpty());
		assertFalse(result.path("tags").path(id).path("shared").asBoolean());
		for (var owner : List.of(project, lib, leaf)) {
			assertEquals(id, result.path("assignments").path(owner.getName()).get(0).asText());
			assertFalse(Files.exists(source(owner))); owner.hasChanged = false;
		}
		var reopened = new TagManager(root, projects::get, this::source, projects::keySet);
		assertEquals(result.path("assignments"), reopened.read(Scope.workspaceProjects, null).path("assignments"));
		var later = project("Later"); reference(leaf, "Later", "Later");
		assertFalse(read(Scope.workspaceProjects).path("assignments").has(later.getName()));
		ObjectNode remove = json("{\"targets\":[\"Leaf\"],\"tagIds\":[\"" + id + "\"]}");
		command(Scope.workspaceProjects, "remove", remove);
		assertFalse(read(Scope.workspaceProjects).path("assignments").has("Leaf"));
	}

	@Test public void unavailableReferencesAreReportedWithoutPreventingAWorkspaceTag() throws Exception {
		var lib = project("Lib"); reference(project, "Library", "Lib"); reference(lib, "Cycle", "Demo");
		reference(lib, "Template", "__PROJECT_NAME__"); reference(project, "Missing", "NotInstalled");
		String existing = create(Scope.workspaceProjects, "Existing"); assign(Scope.workspaceProjects, existing, "Demo", "Lib");
		byte[] bytes = Files.readAllBytes(root.resolve("studio/tags.json"));
		var preview = manager.read(Scope.workspaceProjects, null, "Demo");
		assertEquals(List.of("Demo", "Lib"), TagDocument.JSON.convertValue(preview.path("referenceTargets"), List.class));
		assertEquals(2, preview.path("diagnostics").size());
		assertTrue(preview.path("diagnostics").toString().contains("NotInstalled"));
		assertTrue(preview.path("diagnostics").toString().contains("__PROJECT_NAME__"));
		assertFalse(preview.path("readOnly").asBoolean());
		assertArrayEquals(bytes, Files.readAllBytes(root.resolve("studio/tags.json")));
		var result = command(Scope.workspaceProjects, "createFromReferences", json("{\"project\":\"Demo\",\"definition\":{\"label\":\"Available stack\"}}"));
		assertTrue(result.path("done").asBoolean()); assertEquals(preview.path("diagnostics"), result.path("diagnostics"));
		for (String name : List.of("Demo", "Lib")) assertEquals(List.of(existing, result.path("id").asText()),
				TagDocument.JSON.convertValue(result.path("assignments").path(name), List.class));
		assertFalse(result.path("assignments").has("NotInstalled")); assertFalse(result.path("assignments").has("__PROJECT_NAME__"));
	}

	@Test public void invalidRootOrUnreadableReferencesCannotPublishAPartialTag() throws Exception {
		String existing = create(Scope.workspaceProjects, "Existing"); assign(Scope.workspaceProjects, existing, "Demo");
		ObjectNode before = read(Scope.workspaceProjects); byte[] bytes = Files.readAllBytes(root.resolve("studio/tags.json"));
		assertThrows(java.io.IOException.class, () -> command(Scope.workspaceProjects, "createFromReferences", json("{\"project\":\"Missing\",\"definition\":{\"label\":\"New\"}}")));
		assertEquals(before, read(Scope.workspaceProjects)); assertArrayEquals(bytes, Files.readAllBytes(root.resolve("studio/tags.json")));
		assertThrows(java.io.IOException.class, () -> command(Scope.workspaceProjects, "createFromReferences", json("{\"project\":42,\"definition\":{\"label\":\"New\"}}")));
		assertThrows(java.io.IOException.class, () -> command(Scope.projectObjects, "createFromReferences", json("{\"project\":\"Demo\",\"definition\":{\"label\":\"New\"}}")));
		assertThrows(java.io.IOException.class, () -> manager.read(Scope.projectObjects, "Demo", "Demo"));
		assertArrayEquals(bytes, Files.readAllBytes(root.resolve("studio/tags.json")));
		var unreadable = new TagManager(root, projects::get, this::source, projects::keySet, name -> { throw new java.io.IOException("Unreadable fixture"); });
		var failure = assertThrows(java.io.IOException.class, () -> unreadable.read(Scope.workspaceProjects, null, "Demo"));
		assertTrue(failure.getMessage().contains("Demo")); assertTrue(failure.getMessage().contains("Unreadable fixture"));
		assertThrows(java.io.IOException.class, () -> unreadable.mutate(Scope.workspaceProjects, null,
				unreadable.read(Scope.workspaceProjects, null).path("revision").asText(), "createFromReferences", json("{\"project\":\"Demo\",\"definition\":{\"label\":\"New\"}}")));
		assertEquals(before.path("assignments"), unreadable.read(Scope.workspaceProjects, null).path("assignments"));
		assertArrayEquals(bytes, Files.readAllBytes(root.resolve("studio/tags.json")));
	}

	@Test public void referenceCreationRejectsStaleRevisionsAndInvalidDefinitions() throws Exception {
		String revision = read(Scope.workspaceProjects).path("revision").asText();
		create(Scope.workspaceProjects, "Existing");
		ObjectNode before = read(Scope.workspaceProjects);
		assertThrows(java.io.IOException.class, () -> manager.mutate(Scope.workspaceProjects, null, revision, "createFromReferences", json("{\"project\":\"Demo\",\"definition\":{\"label\":\"New\"}}")));
		assertThrows(java.io.IOException.class, () -> command(Scope.workspaceProjects, "createFromReferences", json("{\"project\":\"Demo\",\"definition\":{\"label\":\"\"}}")));
		assertEquals(before, read(Scope.workspaceProjects));
	}

	@Test public void referenceResolverCanReadClosedProjectsWithoutLoadingUnderTheDomainMonitor() throws Exception {
		var names = List.of("Root", "Library", "Leaf"); var loads = new java.util.concurrent.atomic.AtomicInteger();
		final TagManager[] holder = new TagManager[1];
		var local = holder[0] = new TagManager(root.resolve("closed"), name -> { loads.incrementAndGet(); return null; },
				owner -> { fail("No project source may be opened"); return null; }, () -> names, name -> {
					assertFalse(Thread.holdsLock(holder[0]));
					return switch (name) { case "Root" -> List.of("Library"); case "Library" -> List.of("Leaf"); default -> List.of("Root"); };
				});
		var snapshot = local.read(Scope.workspaceProjects, null, "Root");
		var result = local.mutate(Scope.workspaceProjects, null, snapshot.path("revision").asText(), "createFromReferences", json("{\"project\":\"Root\",\"definition\":{\"label\":\"Closed stack\"}}"));
		assertEquals(3, result.path("assignments").size()); assertEquals(0, loads.get());
	}

	@Test public void referenceClosureIsBoundedBeforeAnyPublication() throws Exception {
		var names = new java.util.ArrayList<String>(); for (int i = 0; i <= 1000; i++) names.add("Project" + i);
		var local = new TagManager(root.resolve("bounded"), name -> null, this::source, () -> names,
				name -> name.equals("Project0") ? names : List.of());
		assertThrows(java.io.IOException.class, () -> local.projectReferenceTargets("Project0"));
		assertFalse(Files.exists(root.resolve("bounded/studio/tags.json")));
	}

	@Test public void extensionProvidersRunOutsideTheDomainMonitorForReadsAndEdits() throws Exception {
		manager.contributions().register("contextual", context -> {
			assertFalse(Thread.holdsLock(manager));
			return context.project() == null ? null : json("{\"label\":\"Contextual\",\"fields\":{\"resources\":{\"label\":\"Resources\",\"type\":\"array\",\"uniqueItems\":true,\"items\":{\"type\":\"string\",\"enum\":[\"B1\",\"B2\"]}}}}");
		});
		assertFalse(read(Scope.workspaceProjects).path("contributions").has("contextual"));
		String id = command(Scope.projectObjects, "create", json("{\"definition\":{\"label\":\"Ordered resources\",\"metadata\":{\"contextual\":{\"resources\":[\"B2\",\"B1\"]}}}}")).path("id").asText();
		assign(Scope.projectObjects, id, first.getFullQName());
		var context = manager.context(project);
		assertEquals(first.getFullQName(), context.path("aliases").path(first.getQName()).asText());
		assertEquals(id, context.path("assignments").path(first.getFullQName()).get(0).asText());
		assertEquals("B2", context.path("tags").path(id).path("metadata").path("contextual").path("resources").get(0).asText());
		context.removeAll(); assertTrue(manager.context(project).has("tags"));
		assertFalse(Files.exists(source(project)));
	}

	@Test public void missingSourcesStayMissingAfterRead() throws Exception {
		assertTrue(read(Scope.projectObjects).path("tags").isEmpty()); assertTrue(read(Scope.workspaceProjects).path("tags").isEmpty());
		assertFalse(Files.exists(source(project))); assertFalse(Files.exists(root.resolve("studio/tags.json"))); assertFalse(project.hasChanged);
	}
	@Test public void whitelistOnlyAllowsProjectsAndSequencesInTheirRespectiveScopes() throws Exception {
		var step = step(sequence, "Excluded");
		var connector = new com.twinsoft.convertigo.beans.connectors.HttpConnector(); connector.setName("Http"); project.add(connector);
		var transaction = new com.twinsoft.convertigo.beans.transactions.HttpTransaction(); transaction.setName("Call"); connector.add(transaction);
		var variable = new com.twinsoft.convertigo.beans.variables.RequestableVariable(); variable.setName("Input"); sequence.add(variable);
		var candidates = read(Scope.projectObjects).path("targets");
		assertTrue(candidates.toString().contains(sequence.getFullQName()));
		assertFalse(candidates.toString().contains(step.getFullQName()));
		assertTrue(TagPolicy.supports(Scope.workspaceProjects, project)); assertTrue(TagPolicy.supports(Scope.projectObjects, sequence));
		assertFalse(TagPolicy.supports(Scope.projectObjects, project)); assertFalse(TagPolicy.supports(Scope.workspaceProjects, sequence));
		String id = create(Scope.projectObjects, "eligible");
		for (var excluded : List.of(step, connector, transaction, variable, project)) {
			assertFalse(TagPolicy.supports(Scope.projectObjects, excluded));
			ObjectNode before = read(Scope.projectObjects); boolean dirty = project.hasChanged;
			assertThrows(java.io.IOException.class, () -> assign(Scope.projectObjects, id, first.getFullQName(), excluded.getFullQName()));
			assertEquals(before, read(Scope.projectObjects)); assertEquals(dirty, project.hasChanged);
		}
		assign(Scope.projectObjects, id, first.getFullQName());
		assertEquals(1, manager.badges(first).path("tags").size()); assertTrue(manager.badges(step).path("tags").isEmpty());
		assertTrue(manager.collection(Scope.projectObjects, "Demo", sequence.getFullQName() + ":st", List.of(step.getFullQName())).path("groups").isEmpty());
		String local = create(Scope.workspaceProjects, "local");
		assertThrows(java.io.IOException.class, () -> assign(Scope.workspaceProjects, local, sequence.getFullQName()));
		assign(Scope.workspaceProjects, local, "Demo"); assertEquals(1, manager.badges(project).path("tags").size());
	}
	@Test public void clipboardCannotBypassTheWhitelist() throws Exception {
		String id = create(Scope.projectObjects, "tag"); assign(Scope.projectObjects, id, first.getFullQName());
		var element = XMLUtils.getDefaultDocumentBuilder().newDocument().createElement("bean"); manager.copyToClipboard(element, first);
		var step = step(sequence, "Excluded"); ObjectNode before = read(Scope.projectObjects);
		try (var paste = manager.beginPaste()) { assertThrows(EngineException.class, () -> manager.pasteFromClipboard(element, step)); }
		assertEquals(before, read(Scope.projectObjects));
		var untagged = element.getOwnerDocument().createElement("bean"); manager.copyToClipboard(untagged, step); assertFalse(untagged.hasAttribute("c8o-tags"));
	}
	@Test public void unsupportedExistingMembershipIsDiagnosedWithoutErasingItsSource() throws Exception {
		var step = step(sequence, "Excluded"); String id = UUID.randomUUID().toString(); var document = new TagDocument(false);
		document.tags.put(id, json("{\"label\":\"legacy\"}")); document.assignments.put(step.getFullQName(), new java.util.LinkedHashSet<>(List.of(id)));
		byte[] original = document.bytes(); TagDocument.write(source(project), original, "absent"); manager.projectClosed(project); manager.projectOpened(project);
		var snapshot = read(Scope.projectObjects); assertTrue(snapshot.path("readOnly").asBoolean()); assertTrue(snapshot.path("diagnostics").toString().contains("only sequences"));
		assertTrue(manager.badges(step).path("tags").isEmpty()); assertArrayEquals(original, Files.readAllBytes(source(project)));
	}
	@Test public void closedWorkspaceProjectionAndPresentationMutationStayLazyAtScale() throws Exception {
		for (int size : List.of(10, 10000)) {
			Path workspace = root.resolve("performance-" + size); var names = new java.util.TreeSet<String>();
			String id = UUID.randomUUID().toString(); var document = new TagDocument(true);
			document.tags.put(id, json("{\"label\":\"Stack\",\"shared\":false}"));
			for (int index = 0; index < size; index++) { String name = "Closed" + index; names.add(name); document.assignments.put(name, new java.util.LinkedHashSet<>(List.of(id))); }
			TagDocument.write(workspace.resolve("studio/tags.json"), document.bytes(), "absent");
			var opened = new java.util.concurrent.atomic.AtomicInteger();
			var local = new TagManager(workspace, name -> { opened.incrementAndGet(); return null; }, p -> { fail("No closed-project source may be read"); return null; }, () -> names);
			long start = System.nanoTime(); var snapshot = local.read(Scope.workspaceProjects, null); long readTime = System.nanoTime() - start;
			var targets = new java.util.ArrayList<>(names);
			for (int warmup = 0; warmup < 3; warmup++) local.collection(Scope.workspaceProjects, null, "workspace", targets);
			start = System.nanoTime(); var projection = local.collection(Scope.workspaceProjects, null, "workspace", targets); long projectionTime = System.nanoTime() - start;
			start = System.nanoTime(); local.mutate(Scope.workspaceProjects, null, snapshot.path("revision").asText(), "update", json("{\"id\":\"" + id + "\",\"definition\":{\"label\":\"Changed\",\"shared\":false}}")); long mutationTime = System.nanoTime() - start;
			assertEquals(size, projection.path("groups").get(0).path("members").size()); assertEquals(0, opened.get()); assertTrue(snapshot.path("publication").isEmpty());
			System.out.printf(java.util.Locale.ROOT, "Tags fixture: %d closed projects, read %.1f ms, projection %.1f ms, mutation %.1f ms, project loads %d%n", size, readTime / 1e6, projectionTime / 1e6, mutationTime / 1e6, opened.get());
		}
	}
	@Test public void draftIdentityAndPresentationSurviveSaveReload() throws Exception {
		String id = create(Scope.projectObjects, "CRM"); assign(Scope.projectObjects, id, first.getFullQName(), second.getFullQName());
		assertFalse(Files.exists(source(project))); assertTrue(project.hasChanged);
		command(Scope.projectObjects, "update", json("{\"id\":\"" + id + "\",\"definition\":{\"label\":\"Équipe\",\"presentation\":{\"color\":\"#2563EB\"}}}"));
		save(project); byte[] saved = Files.readAllBytes(source(project));
		command(Scope.projectObjects, "update", json("{\"id\":\"" + id + "\",\"definition\":{\"label\":\"discard\"}}"));
		manager.projectClosed(project); manager.projectOpened(project);
		assertEquals("Équipe", read(Scope.projectObjects).path("tags").path(id).path("label").asText());
		assertEquals(2, read(Scope.projectObjects).path("assignments").size()); assertArrayEquals(saved, Files.readAllBytes(source(project)));
		assertFalse(read(Scope.projectObjects).path("dirty").asBoolean());
	}
	@Test public void renameUsesTypedIdentityAndExactDescendantBoundary() throws Exception {
		String id = create(Scope.projectObjects, "same label"); String other = create(Scope.projectObjects, "same label"); assertNotEquals(id, other);
		assign(Scope.projectObjects, id, sequence.getFullQName(), first.getFullQName(), second.getFullQName(), neighbor.getFullQName());
		sequence.setName("New"); first.setName("Renamed");
		var assignments = read(Scope.projectObjects).path("assignments");
		assertTrue(assignments.has("Demo.sq:Renamed")); assertTrue(assignments.has("Demo.sq:Old2"));
		assertFalse(assignments.has("Demo.sq:Old")); assertTrue(assignments.has("Demo.sq:New")); assertEquals(4, assignments.size());
	}
	@Test public void failedStructuralMutationDoesNotRemoveMembership() throws Exception {
		String id = create(Scope.projectObjects, "keep"); assign(Scope.projectObjects, id, first.getFullQName());
		project.remove(first); project.add(first); // existing move rollback restores the real object before reconciliation
		assertTrue(read(Scope.projectObjects).path("assignments").has(first.getFullQName()));
		first.delete(); assertTrue(read(Scope.projectObjects).path("assignments").isEmpty()); assertTrue(read(Scope.projectObjects).path("tags").has(id));
	}
	@Test public void deletingParentRemovesDescendantsOnly() throws Exception {
		String id = create(Scope.projectObjects, "tag"); assign(Scope.projectObjects, id, sequence.getFullQName(), neighbor.getFullQName());
		sequence.delete(); var assignments = read(Scope.projectObjects).path("assignments");
		assertEquals(1, assignments.size()); assertTrue(assignments.has(neighbor.getFullQName()));
	}
	@Test public void moveImportsOneLocalDefinitionForEntireSubtree() throws Exception {
		String id = create(Scope.projectObjects, "source"); assign(Scope.projectObjects, id, sequence.getFullQName());
		var child = step(sequence, "Child");
		Project target = project("Target");
		var targetDocument = new TagDocument(false); targetDocument.tags.put(id, json("{\"label\":\"different\"}"));
		TagDocument.write(source(target), targetDocument.bytes(), "absent"); manager.projectOpened(target);
		project.remove(sequence); target.add(sequence); manager.reconcile();
		var result = manager.read(Scope.projectObjects, "Target"); var firstIds = result.path("assignments").path(sequence.getFullQName());
		assertFalse(result.path("assignments").has(child.getFullQName())); assertSame(sequence, child.getParent()); assertNotEquals(id, firstIds.get(0).asText());
		assertEquals(2, result.path("tags").size()); assertTrue(read(Scope.projectObjects).path("assignments").isEmpty());
	}
	@Test public void clipboardCopiesActualNamesAndSharesRemappingWithinBatch() throws Exception {
		String id = create(Scope.projectObjects, "tag"); assign(Scope.projectObjects, id, first.getFullQName(), second.getFullQName());
		var doc = XMLUtils.getDefaultDocumentBuilder().newDocument(); var a = doc.createElement("bean"); var b = doc.createElement("bean");
		manager.copyToClipboard(a, first); manager.copyToClipboard(b, second);
		var copied = sequence(project, "First"); var copied2 = sequence(project, "Second");
		try (var paste = manager.beginPaste()) { manager.pasteFromClipboard(a, copied); manager.pasteFromClipboard(b, copied2); paste.commit(); }
		assertTrue(read(Scope.projectObjects).path("assignments").has(copied.getFullQName())); assertEquals(4, read(Scope.projectObjects).path("assignments").size());
		var cancelled = sequence(project, "Cancelled");
		try (var paste = manager.beginPaste()) { manager.pasteFromClipboard(a, cancelled); }
		assertFalse(read(Scope.projectObjects).path("assignments").has(cancelled.getFullQName()));
	}
	@Test public void revisionsAndExternalEditsPreventLostUpdates() throws Exception {
		String id = create(Scope.projectObjects, "tag"); String revision = read(Scope.projectObjects).path("revision").asText();
		assign(Scope.projectObjects, id, first.getFullQName());
		assertThrows(java.io.IOException.class, () -> manager.mutate(Scope.projectObjects, "Demo", revision, "create", json("{\"definition\":{\"label\":\"bad\"}}")));
		save(project); assign(Scope.projectObjects, id, second.getFullQName());
		Files.writeString(source(project), "{\"schemaVersion\":99}");
		assertTrue(read(Scope.projectObjects).path("readOnly").asBoolean());
		assertThrows(EngineException.class, () -> save(project)); assertTrue(project.hasChanged); assertEquals("{\"schemaVersion\":99}", Files.readString(source(project)));
	}
	@Test public void cleanExternalEditPreservesBoundIdentityDuringRename() throws Exception {
		String id = create(Scope.projectObjects, "before"); assign(Scope.projectObjects, id, first.getFullQName()); save(project);
		TagDocument external = TagDocument.read(source(project), false); external.tags.get(id).put("label", "external");
		TagDocument.write(source(project), external.bytes(), TagDocument.fingerprint(source(project)));
		manager.prepareStructuralChange(first, project); first.setName("After"); manager.reconcile();
		var result = read(Scope.projectObjects);
		assertTrue(result.path("assignments").has("Demo.sq:After"));
		assertFalse(result.path("assignments").has("Demo.sq:First"));
		assertEquals("external", result.path("tags").path(id).path("label").asText());
	}
	@Test public void unresolvedExternalTargetNeverBindsToFutureHomonym() throws Exception {
		String id = create(Scope.projectObjects, "keep diagnostic"); save(project);
		var external = TagDocument.read(source(project), false); external.assignments.put("Demo.sq:Absent", new java.util.LinkedHashSet<>(List.of(id)));
		TagDocument.write(source(project), external.bytes(), TagDocument.fingerprint(source(project))); read(Scope.projectObjects);
		var replacement = sequence(project, "Absent"); replacement.setName("Replacement");
		external = TagDocument.read(source(project), false); external.tags.get(id).put("description", "external edit");
		TagDocument.write(source(project), external.bytes(), TagDocument.fingerprint(source(project)));
		assertTrue(read(Scope.projectObjects).path("assignments").has("Demo.sq:Absent"));
		assertFalse(read(Scope.projectObjects).path("assignments").has(replacement.getFullQName()));
	}
	private AutoCloseable installCommonManager() throws Exception {
		var field = TagManager.class.getDeclaredField("instance"); field.setAccessible(true); Object previous = field.get(null);
		var owner = TagManager.class.getDeclaredField("owner"); owner.setAccessible(true); Object previousOwner = owner.get(null);
		field.set(null, manager); owner.set(null, Engine.theApp);
		return () -> { field.set(null, previous); owner.set(null, previousOwner); };
	}
	@Test public void commonRenameHookRemapsWithoutOptionalRefactorOrRead() throws Exception {
		String id = create(Scope.projectObjects, "tag"); assign(Scope.projectObjects, id, first.getFullQName());
		try (var installed = installCommonManager()) { first.setName("Renamed"); }
		var projection = manager.collection(Scope.projectObjects, "Demo", "Demo:sq", List.of(first.getFullQName()));
		assertEquals(id, projection.path("groups").get(0).path("tagId").asText());
	}
	@Test public void commonMutationPreflightRefusesDirtyExternalConflictBeforeRename() throws Exception {
		String id = create(Scope.projectObjects, "tag"); assign(Scope.projectObjects, id, first.getFullQName()); save(project);
		assign(Scope.projectObjects, id, second.getFullQName()); Files.writeString(source(project), "{\"schemaVersion\":9}");
		try (var installed = installCommonManager()) { assertThrows(EngineException.class, () -> first.setName("Refused")); }
		assertEquals("First", first.getName());
	}
	@Test public void failedRenameDoesNotApplyTheBeanNameCallback() throws Exception {
		String id = create(Scope.projectObjects, "tag"); assign(Scope.projectObjects, id, first.getFullQName());
		var step = new com.twinsoft.convertigo.beans.steps.SimpleStep(); step.setName("Before"); step.setExpression("Before"); sequence.add(step); step.hasChanged = false;
		// Another pending structural change makes reconciliation fail after the untagged step's preflight.
		project.remove(first); Files.createDirectories(source(project).getParent()); Files.writeString(source(project), "{\"schemaVersion\":9}");
		try (var installed = installCommonManager()) { assertThrows(EngineException.class, () -> step.setName("After")); }
		assertEquals("Before", step.getName()); assertEquals("Before", step.getExpression()); assertFalse(step.hasChanged);
	}
	@Test public void saveFailureKeepsDraftAndDirty() throws Exception {
		String id = create(Scope.projectObjects, "tag"); assign(Scope.projectObjects, id, first.getFullQName());
		assertThrows(EngineException.class, () -> manager.save(project, source(project), () -> { throw new java.io.IOException("writer failed"); }));
		assertFalse(Files.exists(source(project))); assertTrue(project.hasChanged); assertTrue(read(Scope.projectObjects).path("dirty").asBoolean());
	}
	@Test public void failedProjectRenameRestoresSavedSourceAndExistingDraft() throws Exception {
		String id = create(Scope.projectObjects, "original"); assign(Scope.projectObjects, id, first.getFullQName()); save(project);
		byte[] original = Files.readAllBytes(source(project));
		assign(Scope.projectObjects, id, second.getFullQName());
		String local = create(Scope.workspaceProjects, "local"); assign(Scope.workspaceProjects, local, "Demo");
		byte[] workspaceSource = Files.readAllBytes(root.resolve("studio/tags.json"));
		try (var rename = manager.beginProjectRename(project)) {
			Files.move(root.resolve("Demo"), root.resolve("Renamed")); project.setName("Renamed");
			manager.renameProject(project, "Demo"); save(project); // A later project rename step fails after export.
		}
		project.setName("Demo"); Files.move(root.resolve("Renamed"), root.resolve("Demo"));
		assertArrayEquals(original, Files.readAllBytes(source(project)));
		assertArrayEquals(workspaceSource, Files.readAllBytes(root.resolve("studio/tags.json")));
		var tags = read(Scope.projectObjects);
		assertTrue(tags.path("dirty").asBoolean()); assertTrue(tags.path("assignments").has(first.getFullQName()));
		assertTrue(tags.path("assignments").has(second.getFullQName())); assertTrue(project.hasChanged);
		assertTrue(read(Scope.workspaceProjects).path("assignments").has("Demo"));
	}
	@Test public void successfulProjectRenameCommitsWorkspaceMembershipAfterExport() throws Exception {
		String id = create(Scope.projectObjects, "keep"); assign(Scope.projectObjects, id, first.getFullQName()); save(project);
		String local = create(Scope.workspaceProjects, "local"); assign(Scope.workspaceProjects, local, "Demo");
		try (var rename = manager.beginProjectRename(project)) {
			Files.move(root.resolve("Demo"), root.resolve("Renamed")); project.setName("Renamed");
			manager.renameProject(project, "Demo"); save(project); rename.commit();
		}
		assertTrue(manager.read(Scope.projectObjects, "Demo").path("assignments").has("Renamed.sq:First"));
		assertTrue(read(Scope.workspaceProjects).path("assignments").has("Renamed"));
		assertFalse(read(Scope.workspaceProjects).path("assignments").has("Demo"));
	}
	@Test public void liveProjectRenamePreservesTagsWhenItsDirectoryMovesBeforeTheModel() throws Exception {
		String firstId = create(Scope.projectObjects, "first"), secondId = create(Scope.projectObjects, "second");
		assign(Scope.projectObjects, secondId, first.getFullQName()); assign(Scope.projectObjects, firstId, first.getFullQName()); save(project);
		var instance = TagManager.class.getDeclaredField("instance"); instance.setAccessible(true);
		var owner = TagManager.class.getDeclaredField("owner"); owner.setAccessible(true);
		Object previousInstance = instance.get(null), previousOwner = owner.get(null);
		try {
			instance.set(null, manager); owner.set(null, Engine.theApp);
			try (var rename = manager.beginProjectRename(project)) {
				Files.move(root.resolve("Demo"), root.resolve("Renamed"));
				manager.projectClosed(project); // The cache release keeps the transaction's sidecar draft.
				project.setName("Renamed"); // Runs the actual shared beforeModelChange callback.
				manager.renameProject(project, "Demo"); save(project); rename.commit();
			}
			var saved = TagDocument.read(source(project), false).object();
			assertFalse(saved.path("assignments").has("Demo.sq:First"));
			assertEquals(secondId, saved.path("assignments").path("Renamed.sq:First").get(0).asText());
			assertEquals(firstId, saved.path("assignments").path("Renamed.sq:First").get(1).asText());
			assertEquals(2, saved.path("tags").size());
			manager.projectClosed(project); manager.projectOpened(project);
			assertEquals(saved.path("assignments"), manager.context(project).path("assignments"));
		} finally { instance.set(null, previousInstance); owner.set(null, previousOwner); }
	}
	@Test public void malformedAndUnsupportedDocumentsAreDiagnosedAndPreserved() throws Exception {
		Files.createDirectories(source(project).getParent());
		for (String content : List.of("{", "{\"schemaVersion\":9}", "{\"schemaVersion\":1,\"schemaVersion\":1}", "{} {}")) {
			Files.writeString(source(project), content); manager.projectClosed(project); manager.projectOpened(project);
			assertTrue(read(Scope.projectObjects).path("readOnly").asBoolean());
			assertThrows(java.io.IOException.class, () -> create(Scope.projectObjects, "no")); assertEquals(content, Files.readString(source(project)));
		}
	}
	@Test public void workspaceLocalAssignmentsDoNotTouchProjectSources() throws Exception {
		String id = create(Scope.workspaceProjects, "local"); assign(Scope.workspaceProjects, id, "Demo");
		assertTrue(Files.exists(root.resolve("studio/tags.json"))); assertFalse(Files.exists(source(project))); assertFalse(project.hasChanged);
		assertFalse(read(Scope.workspaceProjects).path("tags").path(id).path("shared").asBoolean());
	}
	@Test public void sharedPublicationRequiresSaveAndDiscardRequiresExplicitRepublish() throws Exception {
		Project other = project("Other"); Project library = project("lib_Keep"); manager.projectOpened(other);
		String id = create(Scope.workspaceProjects, "stack"); assign(Scope.workspaceProjects, id, "Demo", "Other");
		command(Scope.workspaceProjects, "share", json("{\"id\":\"" + id + "\",\"shared\":true,\"confirmed\":true}"));
		assertTrue(project.hasChanged); assertTrue(other.hasChanged); assertFalse(library.hasChanged); assertFalse(Files.exists(source(project)));
		assertEquals("pendingSave", read(Scope.workspaceProjects).path("publication").get(0).path("status").asText());
		save(project); manager.projectClosed(other); manager.projectOpened(other);
		assertTrue(manager.read(Scope.projectObjects, "Other").path("projectTags").isEmpty());
		assertEquals("unpublished", read(Scope.workspaceProjects).path("publication").get(1).path("status").asText());
		command(Scope.workspaceProjects, "republish", json("{\"id\":\"" + id + "\",\"confirmed\":true}")); save(other);
		assertEquals("published", read(Scope.workspaceProjects).path("publication").get(1).path("status").asText());
		command(Scope.workspaceProjects, "share", json("{\"id\":\"" + id + "\",\"shared\":false,\"confirmed\":true}"));
		assertTrue(manager.read(Scope.projectObjects, "Other").path("projectTags").isEmpty()); assertTrue(Files.readString(source(other)).contains(id));
	}
	@Test public void closedSharedMemberRefusesWholeOperation() throws Exception {
		Project other = project("Closed"); String id = create(Scope.workspaceProjects, "stack"); assign(Scope.workspaceProjects, id, "Demo", "Closed"); projects.remove("Closed");
		byte[] workspace = Files.readAllBytes(root.resolve("studio/tags.json"));
		assertThrows(java.io.IOException.class, () -> command(Scope.workspaceProjects, "share", json("{\"id\":\"" + id + "\",\"shared\":true,\"confirmed\":true}")));
		assertArrayEquals(workspace, Files.readAllBytes(root.resolve("studio/tags.json"))); assertFalse(project.hasChanged); assertFalse(Files.exists(source(other)));
	}
	@Test public void discoveryKeepsIdsAndReportsDivergentPortableDefinition() throws Exception {
		String id = UUID.randomUUID().toString(); TagDocument document = new TagDocument(false); document.projectTags.put(id, json("{\"label\":\"stack\",\"metadata\":{\"absent\":{\"ordered\":[2,1]}}}"));
		TagDocument.write(source(project), document.bytes(), "absent"); manager.projectClosed(project); manager.projectOpened(project);
		assertTrue(read(Scope.workspaceProjects).path("tags").has(id)); byte[] source = Files.readAllBytes(source(project));
		Project other = project("Other"); document.projectTags.put(id, json("{\"label\":\"divergent\"}")); TagDocument.write(source(other), document.bytes(), "absent"); manager.projectOpened(other);
		assertEquals("conflict", read(Scope.workspaceProjects).path("publication").get(1).path("status").asText()); assertArrayEquals(source, Files.readAllBytes(source(project)));
		assertEquals("stack", read(Scope.workspaceProjects).path("tags").path(id).path("label").asText());
	}
	@Test public void unavailableMetadataIsPreservedAndTypedFieldsValidated() throws Exception {
		String id = UUID.randomUUID().toString(); TagDocument document = new TagDocument(false); document.tags.put(id, json("{\"label\":\"tag\",\"metadata\":{\"absent\":{\"items\":[2,1]}}}"));
		TagDocument.write(source(project), document.bytes(), "absent"); manager.projectClosed(project); manager.projectOpened(project);
		ObjectNode definition = (ObjectNode) read(Scope.projectObjects).path("tags").path(id).deepCopy(); definition.put("label", "updated");
		ObjectNode input = TagDocument.JSON.createObjectNode().put("id", id); input.set("definition", definition); command(Scope.projectObjects, "update", input);
		assertEquals(document.tags.get(id).path("metadata"), read(Scope.projectObjects).path("tags").path(id).path("metadata"));
		definition.remove("metadata"); assertThrows(java.io.IOException.class, () -> command(Scope.projectObjects, "update", input));
		manager.contributions().register("neutral.test", json("{\"label\":\"Neutral test\",\"fields\":{\"enabled\":{\"label\":\"Enabled\",\"type\":\"boolean\",\"required\":true},\"count\":{\"label\":\"Count\",\"type\":\"integer\"}}}"));
		ObjectNode metadata = definition.putObject("metadata"); metadata.set("absent", document.tags.get(id).path("metadata").path("absent").deepCopy());
		metadata.set("neutral.test", json("{\"enabled\":true,\"count\":3}")); command(Scope.projectObjects, "update", input);
		metadata.set("neutral.test", json("{\"enabled\":\"wrong\"}")); assertThrows(java.io.IOException.class, () -> command(Scope.projectObjects, "update", input));
	}
	@Test public void projectionHasIndependentOccurrencesAndRetainsCollectionBoundary() throws Exception {
		var targets = List.of(first.getFullQName(), second.getFullQName());
		assertTrue(manager.collection(Scope.projectObjects, "Demo", "Demo:sq", targets).path("groups").isEmpty());
		String a = create(Scope.projectObjects, "A"); String b = create(Scope.projectObjects, "B");
		assertTrue(manager.collection(Scope.projectObjects, "Demo", "Demo:sq", targets).path("groups").isEmpty());
		assign(Scope.projectObjects, a, first.getFullQName()); assign(Scope.projectObjects, b, first.getFullQName());
		var groups = manager.collection(Scope.projectObjects, "Demo", "Demo:sq", List.of(first.getFullQName(), second.getFullQName())).path("groups");
		assertEquals(3, groups.size()); assertEquals("Untagged", groups.get(2).path("label").asText());
		assertEquals(groups.get(0).path("members").get(0).path("targetId"), groups.get(1).path("members").get(0).path("targetId"));
		assertNotEquals(groups.get(0).path("members").get(0).path("rowId"), groups.get(1).path("members").get(0).path("rowId"));
		assertEquals("Demo:sq", groups.get(0).path("collectionId").asText());
		assertSame(project, first.getParent());
		command(Scope.projectObjects, "remove", json("{\"targets\":[\"" + first.getFullQName() + "\"],\"tagIds\":[\"" + a + "\",\"" + b + "\"]}"));
		assertTrue(manager.collection(Scope.projectObjects, "Demo", "Demo:sq", targets).path("groups").isEmpty());

		var nodes = new org.codehaus.jettison.json.JSONArray("[{\"id\":\"Other\"},{\"id\":\"Demo\"}]");
		try (var installed = installCommonManager()) {
			assertSame(nodes, com.twinsoft.convertigo.engine.admin.services.studio.tags.TreeProjection.apply(null, nodes));
			String stack = create(Scope.workspaceProjects, "Stack");
			assertSame(nodes, com.twinsoft.convertigo.engine.admin.services.studio.tags.TreeProjection.apply(null, nodes));
			assign(Scope.workspaceProjects, stack, "Demo");
			var workspace = com.twinsoft.convertigo.engine.admin.services.studio.tags.TreeProjection.apply(null, nodes);
			assertEquals(2, workspace.length());
			assertEquals("Stack", workspace.getJSONObject(0).getString("label"));
			assertEquals("Untagged", workspace.getJSONObject(1).getString("label"));
		}
	}
	@Test public void importedProjectRebasesOnlyItsOwnerAndPreservesPortableIds() throws Exception {
		String id = create(Scope.projectObjects, "tag"); assign(Scope.projectObjects, id, first.getFullQName()); save(project);
		TagDocument original = TagDocument.read(source(project), false); String shared = UUID.randomUUID().toString();
		original.projectTags.put(shared, json("{\"label\":\"stack\"}"));
		Path copy = root.resolve("Copied"); TagDocument.write(copy.resolve("_c8oProject/tags.json"), original.bytes(), "absent");
		TagManager.rebaseImportedSource(copy, "Demo", "Copied");
		TagDocument imported = TagDocument.read(copy.resolve("_c8oProject/tags.json"), false);
		assertTrue(imported.assignments.containsKey("Copied.sq:First")); assertTrue(imported.projectTags.containsKey(shared));
		assertTrue(TagDocument.read(source(project), false).assignments.containsKey(first.getFullQName()));
	}
	@Test public void unsharingStatusStaysPendingUntilSourceSave() throws Exception {
		String id = create(Scope.workspaceProjects, "stack"); assign(Scope.workspaceProjects, id, "Demo");
		command(Scope.workspaceProjects, "share", json("{\"id\":\"" + id + "\",\"shared\":true,\"confirmed\":true}")); save(project);
		command(Scope.workspaceProjects, "share", json("{\"id\":\"" + id + "\",\"shared\":false,\"confirmed\":true}"));
		assertEquals("pendingSave", read(Scope.workspaceProjects).path("publication").get(0).path("status").asText());
		save(project); assertTrue(read(Scope.workspaceProjects).path("publication").isEmpty()); // local-only organization has no portable publication
	}

	@Test public void yamlWriterKeepsSidecarsAndRemovesOnlyObsoleteYaml() throws Exception {
		Path yaml = root.resolve("c8oProject.yaml"); Path sub = root.resolve("_c8oProject"); Files.createDirectories(sub.resolve("sequences"));
		Files.writeString(yaml, "↑convertigo: 8.0.0.m006\n↓Demo [core.Project]: \n");
		Files.writeString(sub.resolve("tags.json"), "{\"sidecar\":true}\n"); Files.writeString(sub.resolve("notes.json"), "{}\n");
		Files.writeString(sub.resolve("sequences/obsolete.yaml"), "obsolete");
		YamlConverter.writeYaml(YamlConverter.readYaml(yaml.toFile(), false), yaml.toFile(), sub.toFile());
		assertEquals("{\"sidecar\":true}\n", Files.readString(sub.resolve("tags.json"))); assertTrue(Files.exists(sub.resolve("notes.json"))); assertFalse(Files.exists(sub.resolve("sequences/obsolete.yaml")));
	}
	@Test public void aFailedMultiProjectReconcileLeavesAllDraftAssociationsIntact() throws Exception {
		String id = create(Scope.projectObjects, "group"); assign(Scope.projectObjects, id, first.getFullQName(), second.getFullQName());
		Project target = project("Target"), invalid = project("Invalid");
		Files.createDirectories(source(invalid).getParent()); Files.writeString(source(invalid), "invalid");
		project.remove(first); target.add(first); project.remove(second); invalid.add(second);
		assertThrows(java.io.IOException.class, manager::reconcile);
		assertTrue(manager.read(Scope.projectObjects, "Target").path("assignments").isEmpty());
		var original = read(Scope.projectObjects); assertTrue(original.path("assignments").has("Demo.sq:First"));
		assertTrue(original.path("assignments").has("Demo.sq:Second"));
		assertTrue(original.path("readOnly").asBoolean());
	}
	@Test public void unavailableFieldsWithinKnownContributionsCannotBeDeleted() throws Exception {
		String id = UUID.randomUUID().toString(); TagDocument document = new TagDocument(false);
		document.tags.put(id, json("{\"label\":\"tag\",\"metadata\":{\"neutral.test\":{\"future\":{\"ordered\":[2,1]}}}}"));
		Files.createDirectories(source(project).getParent()); Files.write(source(project), document.bytes());
		manager.projectClosed(project); manager.projectOpened(project);
		manager.contributions().register("neutral.test", json("{\"label\":\"Neutral test\",\"fields\":{\"enabled\":{\"label\":\"Enabled\",\"type\":\"boolean\"}}}"));
		ObjectNode definition = document.tags.get(id).deepCopy(); definition.withObject("metadata").withObject("neutral.test").remove("future");
		ObjectNode input = TagDocument.JSON.createObjectNode().put("id", id); input.set("definition", definition);
		assertThrows(java.io.IOException.class, () -> command(Scope.projectObjects, "update", input));
		definition.set("metadata", document.tags.get(id).path("metadata").deepCopy()); definition.withObject("metadata").withObject("neutral.test").put("enabled", true);
		command(Scope.projectObjects, "update", input);
		assertEquals(document.tags.get(id).path("metadata").path("neutral.test").path("future"), read(Scope.projectObjects).path("tags").path(id).path("metadata").path("neutral.test").path("future"));
	}
	@Test public void discoveryConflictNeedsAnExplicitChoiceAndAlignmentNeedsSave() throws Exception {
		String id = UUID.randomUUID().toString(); Project other = project("Other");
		for (Project member : List.of(project, other)) {
			TagDocument document = new TagDocument(false); document.projectTags.put(id, json("{\"label\":\"" + member.getName() + "\"}"));
			Files.createDirectories(source(member).getParent()); Files.write(source(member), document.bytes()); manager.projectClosed(member); manager.projectOpened(member);
		}
		assertEquals(1, read(Scope.workspaceProjects).path("conflicts").size());
		ObjectNode choice = json("{\"id\":\"" + id + "\",\"fromProject\":\"Other\",\"confirmed\":true}");
		command(Scope.workspaceProjects, "resolve", choice);
		assertEquals("Other", read(Scope.workspaceProjects).path("tags").path(id).path("label").asText());
		assertFalse(read(Scope.projectObjects).path("dirty").asBoolean());
		choice.put("alignSources", true); command(Scope.workspaceProjects, "resolve", choice);
		assertTrue(read(Scope.projectObjects).path("dirty").asBoolean());
		assertEquals("Demo", TagDocument.read(source(project), false).projectTags.get(id).path("label").asText());
		save(project); assertEquals("Other", TagDocument.read(source(project), false).projectTags.get(id).path("label").asText());
	}
	@Test public void carExportAndImportPreserveTheProjectSidecar() throws Exception {
		String before = Engine.PROJECTS_PATH;
		try {
			Engine.PROJECTS_PATH = root.toString();
			String id = create(Scope.projectObjects, "portable"); assign(Scope.projectObjects, id, first.getFullQName()); save(project);
			Files.writeString(root.resolve("Demo/c8oProject.yaml"), "↑convertigo: 8.0.0.m006\n↓Demo [core.Project]: \n");
			byte[] expected = Files.readAllBytes(source(project));
			var archive = com.twinsoft.convertigo.engine.util.CarUtils.makeArchive(root.resolve("Demo.car").toFile(), project, com.twinsoft.convertigo.engine.enums.ArchiveExportOption.all);
			Path target = root.resolve("Imported");
			com.twinsoft.convertigo.engine.util.ZipUtils.expandZip(archive.toString(), target.toString(), "Demo");
			assertArrayEquals(expected, Files.readAllBytes(target.resolve("_c8oProject/tags.json")));
			TagManager.rebaseImportedSource(target, "Demo", "Imported");
			assertTrue(TagDocument.read(target.resolve("_c8oProject/tags.json"), false).assignments.containsKey("Imported.sq:First"));
		} finally { Engine.PROJECTS_PATH = before; }
	}
	@Test public void affectedCollectionsUseCanonicalTypedFolderIds() throws Exception {
		String id = create(Scope.projectObjects, "typed");
		ObjectNode input = json("{\"tagIds\":[\"" + id + "\"],\"targets\":[\"" + first.getFullQName() + "\"]}");
		var result = command(Scope.projectObjects, "assign", input);
		assertEquals("Demo:sq", result.path("affectedContainers").get(0).asText());
	}

	@Test public void saveFromAProjectCloneUsesTheCanonicalTagDraft() throws Exception {
		String id = create(Scope.projectObjects, "clone save"); assign(Scope.projectObjects, id, first.getFullQName());
		Project clone = project.clone(); manager.save(clone, source(project), () -> {});
		assertEquals(1, TagDocument.read(source(project), false).assignments.size());
		assertFalse(read(Scope.projectObjects).path("dirty").asBoolean());
	}

	private ObjectNode orderedCommand(String target, String... ids) {
		var input = TagDocument.JSON.createObjectNode(); input.putArray("targets").add(target);
		var array = input.putArray("tagIds"); for (String id : ids) array.add(id);
		return input;
	}
	private void assertOrder(Scope scope, String target, String... ids) throws Exception {
		assertEquals(TagDocument.JSON.valueToTree(List.of(ids)), read(scope).path("assignments").path(target));
	}
	@Test public void membershipOrderIsPortableWithoutChangingTheSourceShape() throws Exception {
		String firstId = "99999999-9999-4999-8999-999999999999", secondId = "11111111-1111-4111-8111-111111111111";
		var document = new TagDocument(false);
		document.tags.put(firstId, json("{\"label\":\"Z\"}")); document.tags.put(secondId, json("{\"label\":\"A\"}"));
		document.assignments.put(first.getFullQName(), new java.util.LinkedHashSet<>(List.of(firstId, secondId)));
		TagDocument.write(source(project), document.bytes(), "absent");
		assertEquals(List.of(firstId, secondId), List.copyOf(TagDocument.read(source(project), false).assignments.get(first.getFullQName())));
		manager.projectClosed(project); manager.projectOpened(project);
		assertOrder(Scope.projectObjects, first.getFullQName(), firstId, secondId);
		var groups = manager.collection(Scope.projectObjects, "Demo", "Demo:sq", List.of(first.getFullQName())).path("groups");
		assertEquals("A", groups.get(0).path("label").asText()); // Folder sorting is not composition order.
		assertEquals("Z", manager.badges(first).path("tags").get(0).path("label").asText());
	}
	@Test public void explicitReorderIsADraftAndReassigningDoesNotChangePriority() throws Exception {
		String a = create(Scope.projectObjects, "A"), b = create(Scope.projectObjects, "B"), c = create(Scope.projectObjects, "C");
		command(Scope.projectObjects, "assign", orderedCommand(first.getFullQName(), b, a)); save(project);
		byte[] saved = Files.readAllBytes(source(project));
		command(Scope.projectObjects, "reorder", orderedCommand(first.getFullQName(), a, b));
		assertTrue(read(Scope.projectObjects).path("dirty").asBoolean()); assertTrue(project.hasChanged);
		assertArrayEquals(saved, Files.readAllBytes(source(project)));
		assign(Scope.projectObjects, a, first.getFullQName()); assign(Scope.projectObjects, c, first.getFullQName());
		assertOrder(Scope.projectObjects, first.getFullQName(), a, b, c);
		manager.projectClosed(project); manager.projectOpened(project); assertOrder(Scope.projectObjects, first.getFullQName(), b, a);
		command(Scope.projectObjects, "reorder", orderedCommand(first.getFullQName(), a, b)); save(project);
		manager.projectClosed(project); manager.projectOpened(project); assertOrder(Scope.projectObjects, first.getFullQName(), a, b);
	}
	@Test public void invalidReorderCannotChangeMembershipOrPartiallyChangeTargets() throws Exception {
		String a = create(Scope.projectObjects, "A"), b = create(Scope.projectObjects, "B");
		command(Scope.projectObjects, "assign", orderedCommand(first.getFullQName(), a, b)); assign(Scope.projectObjects, a, second.getFullQName());
		var before = read(Scope.projectObjects);
		for (var ids : List.of(List.of(a), List.of(a, a), List.of(a, b, UUID.randomUUID().toString()))) {
			assertThrows(java.io.IOException.class, () -> command(Scope.projectObjects, "reorder", orderedCommand(first.getFullQName(), ids.toArray(String[]::new))));
			assertEquals(before, read(Scope.projectObjects));
		}
		var input = orderedCommand(first.getFullQName(), b, a); input.withArray("targets").add(second.getFullQName());
		assertThrows(java.io.IOException.class, () -> command(Scope.projectObjects, "reorder", input)); assertEquals(before, read(Scope.projectObjects));
	}
	@Test public void renameCopyMoveAndTransferKeepOrder() throws Exception {
		String a = create(Scope.projectObjects, "A"), b = create(Scope.projectObjects, "B"), c = create(Scope.projectObjects, "C");
		command(Scope.projectObjects, "assign", orderedCommand(first.getFullQName(), c, a, b));
		first.setName("Renamed"); assertOrder(Scope.projectObjects, first.getFullQName(), c, a, b);
		var clipboard = XMLUtils.getDefaultDocumentBuilder().newDocument().createElement("bean"); manager.copyToClipboard(clipboard, first);
		var copy = sequence(project, "Copy");
		try (var paste = manager.beginPaste()) { manager.pasteFromClipboard(clipboard, copy); paste.commit(); }
		assertOrder(Scope.projectObjects, copy.getFullQName(), c, a, b);
		var transfer = orderedCommand(copy.getFullQName(), b); transfer.put("fromTagId", a);
		command(Scope.projectObjects, "transfer", transfer); assertOrder(Scope.projectObjects, copy.getFullQName(), c, b);
		Project other = project("Other"); project.remove(first); other.add(first); manager.reconcile();
		assertEquals(TagDocument.JSON.valueToTree(List.of(c, a, b)), manager.read(Scope.projectObjects, "Other").path("assignments").path(first.getFullQName()));
	}
	@Test public void workspaceProjectTagsAlsoHaveAnExplicitPersistedOrder() throws Exception {
		String a = create(Scope.workspaceProjects, "A"), b = create(Scope.workspaceProjects, "B");
		command(Scope.workspaceProjects, "assign", orderedCommand("Demo", b, a));
		command(Scope.workspaceProjects, "reorder", orderedCommand("Demo", a, b));
		var reloaded = new TagManager(root, projects::get, this::source, projects::keySet);
		assertEquals(TagDocument.JSON.valueToTree(List.of(a, b)), reloaded.read(Scope.workspaceProjects, null).path("assignments").path("Demo"));
		assertFalse(project.hasChanged); assertFalse(Files.exists(source(project)));
	}

}
