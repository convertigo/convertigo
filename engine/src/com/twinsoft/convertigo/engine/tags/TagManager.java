/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.tags;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.w3c.dom.Element;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ReferencedProjectManager;

/** Shared editing domain. Project changes are drafts until the common project export succeeds. */
public final class TagManager {
	public enum Scope { projectObjects, workspaceProjects }
	// Every Flow request asks for the manager: once created it is read without a monitor (instance is written before owner).
	private static volatile Engine owner;
	private static volatile TagManager instance;

	public static TagManager get() {
		Engine current = owner; TagManager manager = instance;
		if (manager != null && current == Engine.theApp) return manager;
		synchronized (TagManager.class) {
			if (instance == null || owner != Engine.theApp) {
				// Eclipse stores engine configuration in plugin metadata; tag organization belongs to its real workspace.
				instance = new TagManager(Path.of(Engine.isStudioMode() ? Engine.PROJECTS_PATH : Engine.USER_WORKSPACE_PATH),
						name -> Engine.theApp.databaseObjectsManager.getLoadedProjectByName(name),
						project -> project.getDirFile().toPath().resolve("_c8oProject/tags.json"),
						() -> Engine.theApp.databaseObjectsManager.getAllProjectNamesList(false), name -> {
							Project project = Engine.theApp.databaseObjectsManager.getLoadedProjectByName(name);
							if (project != null) return referenceNames(project.getReferenceList());
							try { return referenceNames(ReferencedProjectManager.references(Engine.projectFile(name))); }
							catch (Exception e) { throw new IOException("Unable to read project references: " + name, e); }
						});
				owner = Engine.theApp;
			}
			return instance;
		}
	}

	/** Size and date of a tag source: a cheap hint that the file changed; its content fingerprint stays the authority. */
	private record FileStamp(long modified, long size) {
		static FileStamp of(Path path) {
			try {
				var attributes = Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class);
				return new FileStamp(attributes.lastModifiedTime().toMillis(), attributes.size());
			} catch (IOException e) { return new FileStamp(-1, -1); }
		}
	}

	/** Immutable tag context of a project for Flow requests, current while nothing it was built from has changed. */
	private record RunContext(Project project, State state, long generation, Path path, FileStamp stamp,
			int boundTargets, List<Observation> observed, String json) {
		boolean current(Project original) {
			if (project != original || generation != state.generation || boundTargets != state.bindings.size()
					|| !stamp.equals(FileStamp.of(path))) return false;
			// A member moved or renamed since this context needs the reconciliation done under the domain monitor.
			for (var observation : observed) if (observation.dbo().getProject() != observation.project()
					|| !observation.qname().equals(observation.dbo().getFullQName())) return false;
			return true;
		}
	}
	private record Observation(DatabaseObject dbo, Project project, String qname) { }

	private final class State {
		Path path;
		final Project project;
		TagDocument base, work;
		String fingerprint, diagnostic;
		// Read by Flow requests without the monitor; every change of the working state increments it.
		volatile long generation;
		long labelGeneration = -1, labelPortableVersion = -1;
		FileStamp stamp = new FileStamp(-1, -1);
		ObjectNode lastValidContext;
		String warned, conflict;
		// A pending remap of moved members that cannot be applied: this project only is read-only until it is resolved.
		String reconcileIssue;
		final Map<String, String> labels = new HashMap<>();
		final Map<String, DatabaseObject> bindings = new HashMap<>();
		State(Path path, Project project) { this.path = path; this.project = project; load(project == null); }
		void load(boolean workspace) {
			// Stamped before reading: a change during the read is seen as a change by the next Flow request.
			stamp = FileStamp.of(path);
			try { fingerprint = TagDocument.fingerprint(path); base = TagDocument.read(path, workspace); diagnostic = null; }
			catch (IOException e) { fingerprint = "invalid"; base = new TagDocument(workspace); diagnostic = e.getMessage(); }
			work = base.copy(); generation++;
			// Keep known object identities (including unresolved identities) across a clean external edit.
			// Only an explicit project reload may bind an old QName to a replacement object.
			bindings.keySet().retainAll(work.assignments.keySet());
			if (project != null) { bind(project, this); cachePortable(project.getName(), base.projectTags); refreshRunContext(project, this); }
		}
		boolean dirty() { return !work.object().equals(base.object()); }
		String revision() { return fingerprint + ":" + generation; }
		void checkExternal() throws IOException {
			String actual = TagDocument.fingerprint(path);
			if (!actual.equals(fingerprint)) {
				if (dirty()) throw new IOException("Tag source changed externally; discard/reload or resolve the conflict before saving");
				load(work.workspace);
			}
		}
		void editable(String expected) throws IOException {
			checkExternal();
			if (diagnostic != null) throw new IOException(diagnostic);
			if (reconcileIssue != null) throw new IOException(reconcileIssue);
			if (expected == null || !expected.equals(revision())) throw new IOException("Tag revision conflict; refresh the tag manager");
			checkWritable(path);
		}
	}

	private final Path workspacePath;
	private final Function<String, Project> loaded;
	private final Function<Project, Path> projectSource;
	private final Supplier<? extends Collection<String>> projectNames;
	@FunctionalInterface public interface ProjectReferences { Collection<String> names(String project) throws IOException; }
	private final ProjectReferences references;
	private final WeakHashMap<Project, State> projects = new WeakHashMap<>();
	private final java.util.concurrent.ConcurrentHashMap<String, RunContext> runContexts = new java.util.concurrent.ConcurrentHashMap<>();
	private final Map<String, Map<String, ObjectNode>> portable = new TreeMap<>();
	private State workspace;
	private long portableVersion;
	private final TagContributions contributions = new TagContributions();
	private final Set<Project> renaming = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
	private final Map<Project, ProjectRename> projectRenames = new java.util.IdentityHashMap<>();
	private final ThreadLocal<Paste> clipboardPaste = new ThreadLocal<>();

	/** Injection points keep the domain independently testable, with no Studio or runtime startup. */
	public TagManager(Path workspace, Function<String, Project> loaded, Function<Project, Path> projectSource,
			Supplier<? extends Collection<String>> projectNames) {
		this(workspace, loaded, projectSource, projectNames, name -> {
			Project project = loaded.apply(name);
			if (project == null) throw new IOException("Project must be open to read its references: " + name);
			return referenceNames(project.getReferenceList());
		});
	}
	public TagManager(Path workspace, Function<String, Project> loaded, Function<Project, Path> projectSource,
			Supplier<? extends Collection<String>> projectNames, ProjectReferences references) {
		this.workspacePath = workspace.resolve("studio/tags.json");
		this.loaded = loaded; this.projectSource = projectSource; this.projectNames = projectNames; this.references = references;
	}
	private static Collection<String> referenceNames(Collection<? extends com.twinsoft.convertigo.beans.core.Reference> references) {
		var names = new TreeSet<String>();
		for (var reference : references) if (reference instanceof ProjectSchemaReference project) {
			String name = project.getParser().getProjectName();
			if (!name.isBlank()) names.add(name);
		}
		return names;
	}

	/** A one-time membership seed, not a dynamic dependency group. Never imports or opens a project. */
	public List<String> projectReferenceTargets(String project) throws IOException {
		return projectReferenceSelection(project).targets();
	}
	private record ReferenceSelection(List<String> targets, List<String> warnings) {
		ObjectNode describe(ObjectNode result) {
			result.set("referenceTargets", TagDocument.JSON.valueToTree(targets));
			for (String warning : warnings) result.withArray("diagnostics").add(warning);
			return result;
		}
	}
	private ReferenceSelection projectReferenceSelection(String project) throws IOException {
		TagDocument.validateTarget(project);
		Set<String> known = Set.copyOf(projectNames.get());
		if (!known.contains(project)) throw new IOException("Project is not in this workspace: " + project);
		var members = new LinkedHashSet<String>();
		var warnings = new TreeSet<String>();
		var pending = new ArrayDeque<String>(); pending.add(project);
		while (!pending.isEmpty()) {
			String name = pending.removeFirst();
			if (members.contains(name)) continue;
			TagDocument.validateTarget(name);
			if (members.size() >= 1000) throw new IOException("Expected at most 1000 referenced projects");
			members.add(name);
			Collection<String> names;
			try { names = references.names(name); }
			catch (IOException e) { throw new IOException("Unable to read references of " + name + ": " + e.getMessage(), e); }
			for (String target : new TreeSet<>(names)) {
				TagDocument.validateTarget(target);
				if (known.contains(target)) pending.add(target);
				else warnings.add(name + " references " + target + ", which is not in this workspace and will not be included.");
			}
		}
		return new ReferenceSelection(List.copyOf(members), List.copyOf(warnings));
	}

	public ObjectNode read(Scope scope, String project, String referenceProject) throws IOException {
		ObjectNode result = read(scope, project);
		if (referenceProject != null && !referenceProject.isEmpty()) {
			if (scope != Scope.workspaceProjects) throw new IOException("Project references belong to workspace project tags");
			projectReferenceSelection(referenceProject).describe(result);
		}
		return result;
	}

	public TagContributions contributions() { return contributions; }

	/** Data-only working context for extensions; no descriptor/provider is invoked here. */
	public ObjectNode context(Project project) throws IOException { return TagDocument.parseObject(runContext(project)); }

	/**
	 * The working tag context of a project for a Flow request, as JSON. Its hot path takes no domain monitor, so a
	 * project save never holds Flow executions. A diagnosed tag source never fails the request: the last valid context
	 * keeps serving, with a "warning", or the context carries the "diagnostic" and its provider decides what depends on it.
	 */
	public String runContext(Project project) {
		Project original = (Project) project.getOriginal();
		RunContext cached = runContexts.get(original.getName());
		if (cached != null && cached.current(original)) return cached.json();
		synchronized (this) {
			reconcileAll();
			State state = state(original);
			FileStamp stamp = FileStamp.of(state.path);
			long generation = state.generation;
			try { state.checkExternal(); state.conflict = null; }
			catch (IOException e) {
				// An unsaved draft over an external change: the working copy stays authoritative until the conflict is resolved.
				if (!e.getMessage().equals(state.conflict)) { state.conflict = e.getMessage(); warn(original.getName() + ": " + e.getMessage()); }
			}
			if (generation == state.generation) state.stamp = stamp;
			cached = runContexts.get(original.getName());
			if (cached == null || !cached.current(original)) cached = refreshRunContext(original, state);
			return cached.json();
		}
	}

	/** Called under the monitor after every change of a project working state, so Flow requests keep their hot path. */
	private RunContext refreshRunContext(Project project, State state) {
		ObjectNode context;
		if (state.diagnostic == null) {
			context = TagDocument.JSON.createObjectNode().put("project", project.getName()).put("revision", state.revision());
			ObjectNode definitions = context.putObject("tags");
			state.work.tags.forEach((id, definition) -> definitions.set(id, definition.deepCopy()));
			ObjectNode assignments = context.putObject("assignments"), aliases = context.putObject("aliases");
			state.work.assignments.forEach((target, ids) -> {
				var values = assignments.putArray(target); ids.forEach(values::add);
				DatabaseObject dbo = state.bindings.get(target);
				if (dbo != null) aliases.put(dbo.getQName(), target);
			});
			state.lastValidContext = context; state.warned = null;
		} else if (state.lastValidContext != null) {
			context = state.lastValidContext.deepCopy().put("warning", state.diagnostic);
			warnOnce(state, state.diagnostic + " (the last valid tags stay in use)");
		} else {
			context = TagDocument.JSON.createObjectNode().put("project", project.getName()).put("revision", state.revision())
					.put("diagnostic", state.diagnostic);
			context.putObject("tags"); context.putObject("assignments"); context.putObject("aliases");
			warnOnce(state, state.diagnostic);
		}
		var observed = new ArrayList<Observation>();
		state.bindings.values().forEach(dbo -> { if (dbo != null) observed.add(new Observation(dbo, dbo.getProject(), dbo.getFullQName())); });
		var run = new RunContext(project, state, state.generation, state.path, state.stamp, state.bindings.size(),
				List.copyOf(observed), context.toString());
		runContexts.values().removeIf(previous -> previous.project() == project);
		runContexts.put(project.getName(), run);
		return run;
	}

	private void forgetRunContext(Project project) {
		Project original = (Project) project.getOriginal();
		runContexts.values().removeIf(previous -> previous.project() == original);
	}

	private static void warn(String message) { if (Engine.logEngine != null) Engine.logEngine.warn("[Tags] " + message); }
	private static void warnOnce(State state, String message) {
		if (message.equals(state.warned)) return;
		state.warned = message;
		warn((state.project == null ? "Workspace" : state.project.getName()) + ": " + message);
	}

	private State workspace() { if (workspace == null) workspace = new State(workspacePath, null); return workspace; }
	private State state(Project project) {
		project = (Project) project.getOriginal();
		State state = projects.get(project);
		if (state == null) {
			state = new State(projectSource.apply(project), project);
			projects.put(project, state);
		}
		return state;
	}

	private void cachePortable(String name, Map<String, ObjectNode> definitions) {
		Map<String, ObjectNode> previous = portable.get(name);
		if (!java.util.Objects.equals(previous, definitions)) { portable.put(name, new TreeMap<>(definitions)); portableVersion++; }
	}

	private Project requireProject(String name) throws IOException {
		TagDocument.validateTarget(name);
		Project project = loaded.apply(name);
		if (project == null) throw new IOException("Project must be open before editing its tag source: " + name);
		return project;
	}

	private static DatabaseObject resolve(Project project, String qname) throws IOException {
		if (qname.equals(project.getName())) return project;
		if (!qname.startsWith(project.getName() + ".")) throw new IOException("Tag target belongs to another project");
		DatabaseObject current = project;
		try {
			for (String part : qname.substring(project.getName().length() + 1).split("\\.")) current = current.getDatabaseObjectChild(part);
		} catch (Exception e) { throw new IOException("Unknown tag target: " + qname, e); }
		if (current == null || !current.getFullQName().equals(qname)) throw new IOException("Expected a canonical typed database-object QName: " + qname);
		return current;
	}

	private static void bind(Project project, State state) {
		for (String target : state.work.assignments.keySet()) if (!target.equals(project.getName()) && !target.startsWith(project.getName() + ".")) {
			state.diagnostic = "Tag target belongs to another project: " + target;
		}
		Map<String, DatabaseObject> indexed = new HashMap<>();
		long missing = state.work.assignments.keySet().stream().filter(name -> !state.bindings.containsKey(name)).count();
		if (missing > 64) {
			try { new com.twinsoft.convertigo.engine.helpers.WalkHelper() {
				@Override protected void walk(DatabaseObject dbo) throws Exception { indexed.put(dbo.getFullQName(), dbo); super.walk(dbo); }
			}.init(project); }
			catch (Exception e) { state.diagnostic = "Unable to bind tag targets: " + e.getMessage(); return; }
		}
		for (String qname : state.work.assignments.keySet()) {
			if (state.bindings.containsKey(qname)) continue;
			// Null remains a diagnostic, never a future match to a same-named replacement object.
			try {
				DatabaseObject dbo = missing > 64 ? indexed.get(qname) : resolve(project, qname);
				if (dbo != null && !TagPolicy.supports(Scope.projectObjects, dbo)) {
					state.diagnostic = "Unsupported tag target (only sequences are allowed): " + qname;
					dbo = null;
				}
				state.bindings.put(qname, dbo);
			}
			catch (IOException e) { state.bindings.put(qname, null); }
		}
	}

	private void changed(Project project, State state) { state.generation++; project.hasChanged = true; refreshRunContext(project, state); }

	/** Observe actual object identity only after a structural operation has finished (including rollback). */
	public synchronized void reconcile() throws IOException {
		String failure = reconcileAll();
		if (failure != null) throw new IOException(failure);
	}

	/**
	 * Remaps every pending identity change as one plan: a conflict in one destination must not partially remap another.
	 * A failed plan stays entirely pending and marks only the projects it involves (read-only, their save refused), so
	 * the other projects keep editing and saving. Returns the failure, or null.
	 */
	private String reconcileAll() {
		Map<State, TagDocument> drafts = new LinkedHashMap<>();
		Map<State, Map<String, DatabaseObject>> bindings = new LinkedHashMap<>();
		Map<State, Project> owners = new LinkedHashMap<>();
		Map<State, Map<String, String>> imports = new HashMap<>();
		Set<State> involved = new LinkedHashSet<>();
		String failure = null;
		for (var entry : new ArrayList<>(projects.entrySet())) {
			Project project = entry.getKey(); State source = entry.getValue();
			for (var binding : new ArrayList<>(source.bindings.entrySet())) {
				DatabaseObject dbo = binding.getValue();
				if (dbo == null) continue;
				Project destination = dbo.getProject(); String oldQName = binding.getKey();
				String nextQName = destination == null ? null : dbo.getFullQName();
				if (destination == project && oldQName.equals(nextQName)) continue;
				LinkedHashSet<String> ids = source.work.assignments.get(oldQName);
				if (ids == null) continue;
				State target = destination == null ? null : destination == project ? source : state(destination);
				involved.add(source); if (target != null) involved.add(target);
				if (failure != null) continue;
				try { preflight(source); if (target != null && target != source) preflight(target); }
				catch (IOException e) { failure = e.getMessage(); continue; }
				TagDocument sourceDraft = drafts.computeIfAbsent(source, ignored -> source.work.copy());
				Map<String, DatabaseObject> sourceBindings = bindings.computeIfAbsent(source, ignored -> new HashMap<>(source.bindings));
				owners.put(source, project);
				if (target != null) {
					TagDocument targetDraft = drafts.computeIfAbsent(target, ignored -> target.work.copy());
					Map<String, DatabaseObject> targetBindings = bindings.computeIfAbsent(target, ignored -> new HashMap<>(target.bindings));
					owners.put(target, destination);
					LinkedHashSet<String> transferred = new LinkedHashSet<>();
					for (String id : ids) {
						String key = project.getName() + ":" + id;
						transferred.add(destination == project ? id : imports.computeIfAbsent(target, ignored -> new HashMap<>())
								.computeIfAbsent(key, ignored -> importDefinition(targetDraft, id, source.work.tags.get(id))));
					}
					targetDraft.assignments.computeIfAbsent(nextQName, ignored -> new LinkedHashSet<>()).addAll(transferred);
					targetBindings.put(nextQName, dbo);
				}
				sourceDraft.assignments.remove(oldQName); sourceBindings.remove(oldQName);
			}
		}
		if (failure == null) {
			try { for (var draft : drafts.values()) draft.bytes(); }
			catch (IOException e) { failure = e.getMessage(); }
		}
		for (State state : projects.values()) state.reconcileIssue = failure != null && involved.contains(state) ? failure : null;
		if (failure != null) return failure;
		drafts.forEach((state, draft) -> {
			state.work = draft; state.bindings.clear(); state.bindings.putAll(bindings.get(state)); changed(owners.get(state), state);
		});
		return null;
	}

	private static void preflight(State state) throws IOException {
		state.checkExternal();
		if (state.diagnostic != null) throw new IOException(state.diagnostic);
		checkWritable(state.path);
	}

	/** Shared preflight for the existing model/clipboard/move entry points; no tags means no extra source work. */
	public synchronized void prepareStructuralChange(DatabaseObject object, Project destination) throws IOException {
		// The project-rename transaction already checked both sources before moving the directory.
		// Its subsequent setName must not reload the now-absent old path and discard that snapshot.
		if (object instanceof Project && projectRenames.containsKey(object)) return;
		Project project = object.getProject();
		State source = project == null ? null : projects.get(project.getOriginal());
		if (source == null) return;
		boolean tagged = source.bindings.values().stream().anyMatch(member -> {
			for (DatabaseObject current = member; current != null; current = current.getParent()) if (current == object) return true;
			return false;
		});
		if (!tagged) return;
		preflight(source);
		if (destination != null && destination.getOriginal() != project.getOriginal()) preflight(state(destination));
	}

	public static void beforeModelChange(DatabaseObject object, Project destination) throws EngineException {
		Engine current = owner; TagManager manager = current == Engine.theApp ? instance : null;
		if (manager == null || object.getOriginal() != object || object.isImporting) return;
		try { manager.prepareStructuralChange(object, destination); }
		catch (IOException e) { throw new EngineException("Tag source prevents this mutation: " + e.getMessage(), e); }
	}

	/** Name changes are complete here, including UPDATE_NONE. Moves reconcile after their final reattachment. */
	public static void afterModelRename(DatabaseObject object) throws EngineException {
		Engine current = owner; TagManager manager = current == Engine.theApp ? instance : null;
		if (manager == null || object.getOriginal() != object || object.isImporting || object instanceof Project) return;
		try { manager.reconcile(); }
		catch (IOException e) { throw new EngineException("Unable to maintain tag identities", e); }
	}

	private static String importDefinition(TagDocument target, String id, ObjectNode definition) {
		String imported = id;
		if (target.tags.containsKey(id) && !target.tags.get(id).equals(definition)) imported = UUID.randomUUID().toString();
		target.tags.putIfAbsent(imported, definition.deepCopy());
		return imported;
	}

	public ObjectNode read(Scope scope, String projectName) throws IOException {
		ObjectNode available = contributionDescriptors(scope, projectName);
		return read(scope, projectName, available);
	}

	private ObjectNode contributionDescriptors(Scope scope, String projectName) throws IOException {
		return contributions.descriptors(new TagContributions.Context(scope,
				scope == Scope.projectObjects ? requireProject(projectName) : null));
	}

	private synchronized ObjectNode read(Scope scope, String projectName, ObjectNode available) throws IOException {
		var diagnostics = new ArrayList<String>();
		reconcileAll();
		Project project = scope == Scope.projectObjects ? requireProject(projectName) : null;
		State state = project == null ? workspace() : state(project);
		if (state.reconcileIssue != null) diagnostics.add(state.reconcileIssue);
		try {
			long generation = state.generation; state.checkExternal();
			if (project != null && state.generation != generation) bind(project, state);
		} catch (IOException e) { diagnostics.add(e.getMessage()); }
		if (state.diagnostic != null) diagnostics.add(state.diagnostic);
		ObjectNode result = state.work.object();
		result.put("scope", scope.name()); result.put("revision", state.revision());
		result.put("dirty", state.dirty()); result.put("readOnly", !diagnostics.isEmpty() || !writable(state.path));
		var messages = result.putArray("diagnostics"); diagnostics.forEach(messages::add);
		if (project != null) {
			state.bindings.forEach((qname, dbo) -> { if (dbo == null) messages.add("Unresolved tag target: " + qname); });
			result.put("project", project.getName());
			var targets = result.putArray("targets");
			project.getSequencesList().stream().map(DatabaseObject::getFullQName).sorted().forEach(targets::add);
		} else {
			var conflicts = result.putArray("conflicts");
			for (var definition : state.work.tags.entrySet()) {
				String id = definition.getKey(); ObjectNode local = portableDefinition(definition.getValue());
				boolean differs = portable.values().stream().anyMatch(values -> values.containsKey(id) && !values.get(id).equals(local));
				if (differs) {
					var conflict = conflicts.addObject().put("tagId", id).put("presentationChosen", state.work.presentationChosen(id, local));
					conflict.set("local", local);
					var sources = conflict.putObject("sources"); portable.forEach((name, values) -> { if (values.containsKey(id)) sources.set(name, values.get(id)); });
				}
			}
			var statuses = result.putArray("publication");
			var namesList = result.putArray("projects"); new TreeSet<>(projectNames.get()).forEach(namesList::add);
			var origins = result.putObject("portableAssignments");
			portable.forEach((name, definitions) -> { var ids = origins.putArray(name); definitions.keySet().forEach(ids::add); });
			TreeSet<String> publicationIds = new TreeSet<>();
			state.work.tags.forEach((id, definition) -> { if (definition.path("shared").asBoolean()) publicationIds.add(id); });
			portable.values().forEach(definitions -> publicationIds.addAll(definitions.keySet()));
			projects.values().forEach(draft -> publicationIds.addAll(draft.work.projectTags.keySet()));
			publicationIds.forEach(id -> {
				ObjectNode definition = state.work.tags.get(id);
				TreeSet<String> names = new TreeSet<>();
				state.work.assignments.forEach((name, ids) -> { if (ids.contains(id)) names.add(name); });
				portable.forEach((name, definitions) -> { if (definitions.containsKey(id)) names.add(name); });
				for (String name : names) {
					boolean member = state.work.assignments.getOrDefault(name, new LinkedHashSet<>()).contains(id);
					ObjectNode desired = member && definition != null && definition.path("shared").asBoolean() ? portableDefinition(definition) : null;
					ObjectNode published = portable.getOrDefault(name, Map.of()).get(id);
					Project target = loaded.apply(name); State draft = target == null ? null : projects.get(target);
					ObjectNode working = draft == null ? published : draft.work.projectTags.get(id);
					String status = java.util.Objects.equals(desired, published) ? "published"
							: java.util.Objects.equals(desired, working) ? "pendingSave" : published != null ? "conflict" : "unpublished";
					statuses.addObject().put("tagId", id).put("project", name).put("status", status).put("open", target != null);
				}
			});
		}
		result.set("contributions", available);
		result.put("membershipOrder", "explicit");
		return result;
	}

	private static ObjectNode portableDefinition(ObjectNode definition) { ObjectNode portable = definition.deepCopy(); portable.remove("shared"); return portable; }
	private static ObjectNode metadata(ObjectNode definition) {
		return definition != null && definition.path("metadata") instanceof ObjectNode metadata ? metadata : TagDocument.JSON.createObjectNode();
	}

	/** One revision-checked domain command, with validated targets and complete impact information. */
	public ObjectNode mutate(Scope scope, String projectName, String revision, String action, ObjectNode input) throws IOException {
		ObjectNode available = contributionDescriptors(scope, projectName);
		ReferenceSelection selection = null;
		if (action.equals("createFromReferences")) {
			if (scope != Scope.workspaceProjects) throw new IOException("Project references belong to workspace project tags");
			if (!input.path("project").isTextual()) throw new IOException("Expected a project name");
			// File/model reads stay outside the tag-domain monitor, just like descriptor providers.
			selection = projectReferenceSelection(input.path("project").asText());
		}
		ObjectNode result = mutate(scope, projectName, revision, action, input, available, selection == null ? List.of() : selection.targets());
		return selection == null ? result : selection.describe(result);
	}

	private synchronized ObjectNode mutate(Scope scope, String projectName, String revision, String action, ObjectNode input,
			ObjectNode available, List<String> referenceTargets) throws IOException {
		reconcileAll();
		Project project = scope == Scope.projectObjects ? requireProject(projectName) : null;
		State state = project == null ? workspace() : state(project);
		state.editable(revision);
		TagDocument next = state.work.copy();
		String id = input.path("id").asText("");
		TreeSet<String> affected = new TreeSet<>();
		Map<Project, TagDocument> publications = new LinkedHashMap<>();
		Map<String, DatabaseObject> newBindings = new HashMap<>();
		switch (action) {
			case "create", "createFromReferences", "update" -> {
				ObjectNode definition = input.path("definition") instanceof ObjectNode d ? d.deepCopy() : null;
				if (definition == null) throw new IOException("Missing tag definition");
				if (!action.equals("update")) { id = UUID.randomUUID().toString(); if (project == null) definition.put("shared", false); }
				else { TagDocument.validateId(id); if (!next.tags.containsKey(id)) throw new IOException("Unknown tag"); }
				ObjectNode previous = next.tags.get(id);
				// Shared is changed only by the publication command, after preflighting every member.
				if (project == null && action.equals("update")) definition.put("shared", previous.path("shared").asBoolean());
				TagDocument.validateDefinition(definition, project == null);
				contributions.validateEdit(metadata(previous), metadata(definition), available);
				next.tags.put(id, definition);
				Set<String> knownProjects = referenceTargets.isEmpty() ? Set.of() : Set.copyOf(projectNames.get());
				for (String target : referenceTargets) {
					if (!knownProjects.contains(target)) throw new IOException("Unknown project: " + target);
					next.assignments.computeIfAbsent(target, ignored -> new LinkedHashSet<>()).add(id);
					affected.add(target);
				}
				final String updatedId = id;
				next.assignments.forEach((target, ids) -> { if (ids.contains(updatedId)) affected.add(target); });
			}
			case "delete" -> {
				TagDocument.validateId(id); if (!next.tags.containsKey(id)) throw new IOException("Unknown tag");
				int count = (int) next.assignments.values().stream().filter(ids -> ids.contains(input.path("id").asText())).count();
				if (!input.path("confirmed").asBoolean() || !input.path("memberCount").isIntegralNumber() || input.path("memberCount").asInt() != count)
					throw new IOException("Confirm deletion with the current membership count: " + count);
				next.tags.remove(id);
				for (var membership : next.assignments.entrySet()) if (membership.getValue().remove(id)) affected.add(membership.getKey());
			}
			case "assign", "remove", "clear", "transfer", "reorder" -> {
				Set<String> knownProjects = project == null ? Set.copyOf(projectNames.get()) : Set.of();
				LinkedHashSet<String> ids = ids(input.path("tagIds"));
				for (String tagId : ids) if (!next.tags.containsKey(tagId)) throw new IOException("Unknown tag: " + tagId);
				if (!input.path("targets").isArray() || input.path("targets").isEmpty() || input.path("targets").size() > 1000) throw new IOException("Expected 1–1000 targets");
				if (action.equals("clear") && !input.path("confirmed").asBoolean()) throw new IOException("Confirm removal of all target tags");
				String removed = input.path("fromTagId").asText("");
				if (action.equals("transfer")) { TagDocument.validateId(removed); if (!next.tags.containsKey(removed)) throw new IOException("Unknown source tag"); }
				for (var value : input.path("targets")) {
					if (!value.isTextual()) throw new IOException("Invalid tag target");
					String target = value.asText(); TagDocument.validateTarget(target);
					if (project != null) {
						DatabaseObject dbo = resolve(project, target);
						TagPolicy.require(scope, dbo);
						newBindings.put(target, dbo);
					}
					else if (!knownProjects.contains(target)) throw new IOException("Unknown project: " + target);
					LinkedHashSet<String> assigned = next.assignments.computeIfAbsent(target, key -> new LinkedHashSet<>());
					if (action.equals("clear")) assigned.clear();
					else if (action.equals("remove")) assigned.removeAll(ids);
					else if (action.equals("reorder")) {
						if (!assigned.equals(ids)) throw new IOException("Tag order must contain exactly the target's assigned tags");
						assigned.clear(); assigned.addAll(ids);
					} else if (action.equals("transfer") && assigned.contains(removed)) {
						// Replace the source at its position; never reorder unrelated memberships.
						LinkedHashSet<String> replacement = new LinkedHashSet<>();
						for (String current : assigned) { if (current.equals(removed)) replacement.addAll(ids); else replacement.add(current); }
						assigned.clear(); assigned.addAll(replacement);
					} else assigned.addAll(ids);
					affected.add(target);
				}
			}
			case "resolve" -> {
				if (project != null) throw new IOException("Only shared project definitions have discovery conflicts");
				TagDocument.validateId(id);
				if (!next.tags.containsKey(id) || !input.path("confirmed").asBoolean()) throw new IOException("Confirm the presentation choice");
				String from = input.path("fromProject").asText();
				ObjectNode choice = from.isEmpty() ? portableDefinition(next.tags.get(id)) : portable.getOrDefault(from, Map.of()).get(id);
				if (choice == null) throw new IOException("Unknown portable definition source");
				ObjectNode definition = choice.deepCopy(); definition.put("shared", true); next.tags.put(id, definition);
				next.choosePresentation(id, choice);
				for (var source : portable.entrySet()) if (source.getValue().containsKey(id)) next.assignments.computeIfAbsent(source.getKey(), ignored -> new LinkedHashSet<>()).add(id);
				if (input.path("alignSources").asBoolean()) {
					for (var member : next.assignments.entrySet()) if (member.getValue().contains(id)) {
						Project target = requireProject(member.getKey()); State draft = state(target); preflight(draft);
						TagDocument document = draft.work.copy(); document.projectTags.put(id, choice.deepCopy()); publications.put(target, document);
					}
				}
				next.assignments.forEach((target, assigned) -> { if (assigned.contains(input.path("id").asText())) affected.add(target); });
			}
			case "share", "republish" -> {
				if (project != null) throw new IOException("Only workspace project tags can be shared");
				TagDocument.validateId(id); if (!next.tags.containsKey(id)) throw new IOException("Unknown tag");
				if (action.equals("share")) next.tags.get(id).put("shared", input.path("shared").asBoolean());
				if (!input.path("confirmed").asBoolean()) throw new IOException("Confirm the affected project list before publication");
				final String selectedId = id;
				next.assignments.forEach((target, assigned) -> { if (assigned.contains(selectedId)) affected.add(target); });
			}
			default -> throw new IOException("Unknown tag action");
		}
		next.assignments.entrySet().removeIf(entry -> entry.getValue().isEmpty());
		if (project == null && !action.equals("resolve")) preparePublication(state.work, next, action.equals("republish") ? id : null, publications);
		// Serialization validates size before any in-memory or disk mutation is committed.
		next.bytes(); for (var publication : publications.values()) publication.bytes();
		if (project == null && !next.object().equals(state.work.object())) {
			TagDocument.write(state.path, next.bytes(), state.fingerprint);
			state.fingerprint = TagDocument.fingerprint(state.path); state.base = next.copy();
		}
		boolean changed = !next.object().equals(state.work.object()); state.work = next;
		// Bindings first: a change publishes the Flow run context, whose aliases come from them.
		state.bindings.putAll(newBindings);
		state.bindings.keySet().retainAll(next.assignments.keySet());
		if (changed) { if (project == null) state.generation++; else changed(project, state); }
		else if (project != null && !newBindings.isEmpty()) refreshRunContext(project, state);
		var dirtyProjects = new TreeSet<String>();
		if (project != null && state.dirty()) dirtyProjects.add(project.getName());
		publications.forEach((target, document) -> {
			State targetState = state(target);
			if (!document.object().equals(targetState.work.object())) { targetState.work = document; changed(target, targetState); }
			if (targetState.dirty()) dirtyProjects.add(target.getName());
		});
		ObjectNode result = read(scope, projectName, available);
		result.put("id", id); result.put("done", true);
		var dirty = result.putArray("dirtyProjects"); dirtyProjects.forEach(dirty::add);
		var targets = result.putArray("affectedTargets"); affected.forEach(targets::add);
		var containers = result.putArray("affectedContainers");
		TreeSet<String> parents = new TreeSet<>();
		for (String target : affected) {
			if (scope == Scope.workspaceProjects) { parents.add(""); continue; }
			int dot = target.lastIndexOf('.');
			if (dot < 0) { parents.add(target); continue; }
			String parent = target.substring(0, dot), segment = target.substring(dot + 1);
			int colon = segment.indexOf(':');
			parents.add(colon < 0 ? parent : parent + ":" + segment.substring(0, colon));
		}
		parents.forEach(containers::add);
		if (instance == this && Engine.theApp != null) {
			try {
				var payload = new org.codehaus.jettison.json.JSONObject().put("reason", "project.tagsChanged")
						.put("project", projectName == null ? "" : projectName).put("scope", scope.name());
				for (String key : List.of("dirtyProjects", "affectedContainers", "affectedTargets"))
					payload.put(key, new org.codehaus.jettison.json.JSONArray(result.path(key).toString()));
				com.twinsoft.convertigo.engine.admin.events.AdminEventBus.publish("projects.changed", payload);
			} catch (Exception e) { Engine.logStudio.warn("Unable to notify tag changes", e); }
		}
		return result;
	}

	private static LinkedHashSet<String> ids(JsonNode values) throws IOException {
		if (!values.isArray() || values.size() > 5000) throw new IOException("Expected a tag ID list");
		LinkedHashSet<String> ids = new LinkedHashSet<>();
		for (var value : values) {
			if (!value.isTextual()) throw new IOException("Tag ID must be a string");
			TagDocument.validateId(value.asText());
			if (!ids.add(value.asText())) throw new IOException("Duplicate tag ID");
		}
		return ids;
	}

	private void preparePublication(TagDocument before, TagDocument after, String republish, Map<Project, TagDocument> drafts) throws IOException {
		TreeSet<String> tags = new TreeSet<>(before.tags.keySet()); tags.addAll(after.tags.keySet());
		for (String id : tags) {
			ObjectNode oldDefinition = before.tags.get(id), newDefinition = after.tags.get(id);
			boolean oldShared = oldDefinition != null && oldDefinition.path("shared").asBoolean();
			boolean newShared = newDefinition != null && newDefinition.path("shared").asBoolean();
			if (!oldShared && !newShared && !id.equals(republish)) continue;
			TreeSet<String> names = new TreeSet<>(before.assignments.keySet()); names.addAll(after.assignments.keySet());
			for (String name : names) {
				boolean wasMember = oldShared && before.assignments.getOrDefault(name, new LinkedHashSet<>()).contains(id);
				boolean isMember = newShared && after.assignments.getOrDefault(name, new LinkedHashSet<>()).contains(id);
				if (!wasMember && !isMember) continue;
				if (wasMember == isMember && java.util.Objects.equals(oldDefinition, newDefinition) && !id.equals(republish)) continue;
				Project project = requireProject(name); State state = state(project);
				state.checkExternal(); if (state.diagnostic != null) throw new IOException(state.diagnostic); checkWritable(state.path);
				TagDocument draft = drafts.computeIfAbsent(project, key -> state.work.copy());
				ObjectNode published = state.work.projectTags.get(id);
				if (published != null && oldShared && !published.equals(portableDefinition(oldDefinition))) throw new IOException("Shared tag definition conflict in " + name);
				if (isMember) draft.projectTags.put(id, portableDefinition(newDefinition)); else draft.projectTags.remove(id);
			}
		}
	}

	/** Called only on project discovery/reload. Reading never republishes an abandoned draft. */
	public synchronized void projectOpened(Project project) {
		State state = state(project);
		cachePortable(project.getName(), state.base.projectTags);
		if (state.diagnostic != null || state.base.projectTags.isEmpty()) return;
		State workspace = workspace();
		try {
			workspace.checkExternal(); if (workspace.diagnostic != null) return;
			TagDocument next = workspace.work.copy();
			for (var definition : state.base.projectTags.entrySet()) {
				ObjectNode known = next.tags.get(definition.getKey());
				if (known == null) { known = definition.getValue().deepCopy(); known.put("shared", true); next.tags.put(definition.getKey(), known); }
				if (known.path("shared").asBoolean() && portableDefinition(known).equals(definition.getValue()))
					next.assignments.computeIfAbsent(project.getName(), key -> new LinkedHashSet<>()).add(definition.getKey());
			}
			if (!next.object().equals(workspace.work.object())) {
				TagDocument.write(workspace.path, next.bytes(), workspace.fingerprint);
				workspace.work = next; workspace.base = next.copy(); workspace.fingerprint = TagDocument.fingerprint(workspace.path); workspace.generation++;
			}
		} catch (IOException e) { workspace.diagnostic = "Portable tag discovery: " + e.getMessage(); }
	}

	public synchronized void projectClosed(Project project) { if (!renaming.contains(project)) { projects.remove(project); forgetRunContext(project); } }

	public interface Rename extends AutoCloseable {
		void commit() throws IOException;
		@Override void close() throws IOException;
	}
	private final class ProjectRename implements Rename {
		final Project project;
		final State state;
		final String oldName, fingerprint;
		final Path oldPath;
		final TagDocument base, work;
		final Map<String, DatabaseObject> bindings;
		final byte[] source;
		final boolean dirty;
		TagDocument localDraft;
		String localRevision;
		boolean committed, closed;
		ProjectRename(Project project, State state) throws IOException {
			this.project = project; this.state = state; oldName = project.getName(); oldPath = state.path;
			fingerprint = state.fingerprint; base = state.base.copy(); work = state.work.copy(); bindings = new HashMap<>(state.bindings);
			source = Files.exists(oldPath) ? Files.readAllBytes(oldPath) : null; dirty = project.hasChanged;
		}
		@Override public void commit() throws IOException { synchronized (TagManager.this) {
			if (localDraft != null) {
				State local = workspace(); local.checkExternal();
				if (!local.revision().equals(localRevision)) throw new IOException("Workspace tags changed during project rename");
				TagDocument.write(local.path, localDraft.bytes(), local.fingerprint);
				local.work = localDraft; local.base = localDraft.copy(); local.fingerprint = TagDocument.fingerprint(local.path); local.generation++;
			}
			portable.remove(oldName); cachePortable(project.getName(), state.base.projectTags); committed = true;
		} }
		@Override public void close() throws IOException { synchronized (TagManager.this) {
			if (closed) return; closed = true;
			try {
				if (!committed) {
					// The model owner restores its name/directory after this sidecar rollback.
					if (source != null) TagDocument.write(state.path, source, state.fingerprint);
					else if (Files.exists(state.path)) {
						if (!TagDocument.fingerprint(state.path).equals(state.fingerprint)) throw new IOException("Tag source changed during rename rollback");
						Files.delete(state.path);
					}
					state.path = oldPath; state.fingerprint = fingerprint; state.base = base; state.work = work;
					state.bindings.clear(); state.bindings.putAll(bindings); state.generation++; project.hasChanged = dirty;
					portable.remove(project.getName()); cachePortable(oldName, base.projectTags);
				}
			} finally { renaming.remove(project); projectRenames.remove(project); }
		} }
	}
	public synchronized Rename beginProjectRename(Project project) throws IOException {
		State state = state(project); preflight(state);
		State local = workspace(); local.checkExternal();
		if (local.diagnostic != null) throw new IOException(local.diagnostic);
		if (local.work.assignments.containsKey(project.getName())) checkWritable(local.path);
		if (projectRenames.containsKey(project)) throw new IOException("Project rename is already in progress");
		ProjectRename rename = new ProjectRename(project, state); renaming.add(project); projectRenames.put(project, rename); return rename;
	}

	public synchronized ObjectNode suggestions(String projectName) throws IOException {
		ObjectNode result = TagDocument.JSON.createObjectNode(); var suggestions = result.putArray("suggestions");
		for (var entry : projects.entrySet()) {
			if (entry.getKey().getName().equals(projectName) || loaded.apply(entry.getKey().getName()) != entry.getKey()) continue;
			for (var definition : entry.getValue().work.tags.entrySet()) {
				ObjectNode suggestion = suggestions.addObject(); suggestion.put("project", entry.getKey().getName()); suggestion.put("sourceId", definition.getKey());
				ObjectNode presentation = definition.getValue().deepCopy(); presentation.remove(Set.of("metadata", "shared")); suggestion.set("definition", presentation);
			}
		}
		return result;
	}

	@FunctionalInterface public interface ProjectWriter { void write() throws Exception; }

	/** Serialize the project and its sidecar under the same domain lock, clearing dirty only on success. */
	public synchronized void save(Project project, Path source, ProjectWriter writer) throws EngineException {
		try {
			reconcileAll(); State state = state(project);
			if (state.reconcileIssue != null) throw new IOException(state.reconcileIssue);
			state.checkExternal();
			if (state.dirty() && state.diagnostic != null) throw new IOException(state.diagnostic);
			byte[] bytes = state.dirty() ? state.work.bytes() : null;
			if (bytes != null) checkWritable(source);
			writer.write();
			if (bytes != null) {
				TagDocument.write(source, bytes, state.fingerprint);
				state.path = source; state.fingerprint = TagDocument.fingerprint(source); state.base = state.work.copy();
				state.stamp = FileStamp.of(source); state.generation++;
				refreshRunContext(project, state);
			}
			cachePortable(project.getName(), state.base.projectTags);
		} catch (Exception e) { project.hasChanged = true; throw new EngineException("Project save failed: " + e.getMessage(), e); }
	}

	/** Preserve drafts across the existing project-rename cache release, then rebase canonical memberships. */
	public synchronized void renameProject(Project project, String oldName) throws IOException {
		State state = state(project); state.path = projectSource.apply(project);
		TreeMap<String, LinkedHashSet<String>> remapped = new TreeMap<>();
		Map<String, DatabaseObject> remappedBindings = new HashMap<>();
		state.work.assignments.forEach((qname, ids) -> remapped.put(qname.equals(oldName) ? project.getName()
				: qname.startsWith(oldName + ".") ? project.getName() + qname.substring(oldName.length()) : qname, ids));
		state.bindings.forEach((qname, object) -> remappedBindings.put(qname.equals(oldName) ? project.getName()
				: qname.startsWith(oldName + ".") ? project.getName() + qname.substring(oldName.length()) : qname, object));
		state.work.assignments.clear(); state.work.assignments.putAll(remapped); state.bindings.clear(); state.bindings.putAll(remappedBindings);
		if (state.dirty()) changed(project, state);
		State workspace = workspace(); workspace.checkExternal();
		if (workspace.diagnostic != null) throw new IOException(workspace.diagnostic);
		LinkedHashSet<String> ids = workspace.work.assignments.get(oldName);
		if (ids != null) {
			TagDocument next = workspace.work.copy(); next.assignments.remove(oldName); next.assignments.put(project.getName(), new LinkedHashSet<>(ids));
			next.bytes();
			ProjectRename rename = projectRenames.get(project);
			if (rename != null) { rename.localDraft = next; rename.localRevision = workspace.revision(); }
			else {
				TagDocument.write(workspace.path, next.bytes(), workspace.fingerprint); workspace.work = next; workspace.base = next.copy();
				workspace.fingerprint = TagDocument.fingerprint(workspace.path); workspace.generation++;
			}
		}
		if (!projectRenames.containsKey(project)) { var declarations = portable.remove(oldName); if (declarations != null) portable.put(project.getName(), declarations); }
	}

	/** Project duplication/import already writes new sources; rebase identities without changing portable tag IDs. */
	public static void rebaseImportedSource(Path directory, String oldName, String newName) throws IOException {
		Path source = directory.resolve("_c8oProject/tags.json");
		if (!Files.exists(source) || oldName.equals(newName)) return;
		String revision = TagDocument.fingerprint(source); TagDocument document = TagDocument.read(source, false);
		TreeMap<String, LinkedHashSet<String>> assignments = new TreeMap<>();
		document.assignments.forEach((target, ids) -> assignments.put(target.equals(oldName) ? newName
				: target.startsWith(oldName + ".") ? newName + target.substring(oldName.length()) : target, ids));
		if (assignments.equals(document.assignments)) return;
		document.assignments.clear(); document.assignments.putAll(assignments);
		TagDocument.write(source, document.bytes(), revision);
	}

	public synchronized ObjectNode projectBadges(String name) {
		ObjectNode result = TagDocument.JSON.createObjectNode(); var badges = result.putArray("tags");
		State state = workspace();
		for (String id : state.work.assignments.getOrDefault(name, new LinkedHashSet<>())) {
			ObjectNode badge = state.work.tags.get(id).deepCopy(); badge.put("label", displayLabel(state, id)); badge.put("id", id); badges.add(badge);
		}
		return result;
	}

	/** Read-only property presentation; membership editing stays in the scoped manager. */
	public synchronized String labels(Scope scope, DatabaseObject dbo) {
		ObjectNode source = scope == Scope.workspaceProjects ? projectBadges(dbo.getProject().getName()) : objectBadges(dbo);
		List<String> values = new ArrayList<>(); source.path("tags").forEach(tag -> values.add(tag.path("label").asText()));
		return String.join(", ", values);
	}

	/** Clipboard transport is an attribute, not a serialized DBO property or a persistent UI identity. */
	public synchronized void copyToClipboard(Element element, DatabaseObject dbo) throws IOException {
		if (!TagPolicy.supports(Scope.projectObjects, dbo)) return;
		reconcileAll(); Project project = dbo.getProject(); if (project == null) return;
		State state = state(project); LinkedHashSet<String> ids = state.work.assignments.get(dbo.getFullQName());
		if (ids == null || ids.isEmpty()) return;
		ObjectNode definitions = TagDocument.JSON.createObjectNode(); ids.forEach(id -> definitions.set(id, state.work.tags.get(id)));
		element.setAttribute("c8o-tags", definitions.toString());
	}

	public void pasteFromClipboard(Element element, DatabaseObject dbo) throws EngineException {
		String text = element.getAttribute("c8o-tags"); if (text.isEmpty()) return;
		try { TagPolicy.require(Scope.projectObjects, dbo); }
		catch (IOException e) { throw new EngineException(e.getMessage(), e); }
		Paste plan = clipboardPaste.get();
		if (plan == null) throw new EngineException("Tag clipboard requires a completed paste transaction");
		try { plan.objects.put(dbo, TagDocument.parseObject(text)); }
		catch (IOException e) { throw new EngineException("Invalid clipboard tags", e); }
	}

	public Paste beginPaste() throws IOException {
		if (clipboardPaste.get() != null) throw new IOException("Nested clipboard transaction");
		Paste plan = new Paste(); clipboardPaste.set(plan); return plan;
	}

	/** The two clipboard adapters register copies before attaching them to the live model. */
	public void trackPasteAttachment(DatabaseObject parent, DatabaseObject copy) {
		Paste plan = clipboardPaste.get();
		if (plan == null || parent == null) return;
		for (DatabaseObject current = parent; current != null; current = current.getParent()) {
			if (plan.attachments.containsKey(current)) return; // Its newly copied root already owns this subtree.
			plan.changedBefore.putIfAbsent(current, current.hasChanged);
		}
		plan.attachments.put(copy, parent);
	}

	/** Copies use the actual names chosen by the paste, and commit tags only after the whole paste succeeds. */
	public final class Paste implements AutoCloseable {
		private final Map<DatabaseObject, ObjectNode> objects = new LinkedHashMap<>();
		private final Map<DatabaseObject, DatabaseObject> attachments = new LinkedHashMap<>();
		private final Map<DatabaseObject, Boolean> changedBefore = new java.util.IdentityHashMap<>();
		private boolean committed;
		public void commit() throws IOException {
			synchronized (TagManager.this) {
				if (committed) throw new IOException("Clipboard already committed");
				Map<Project, TagDocument> drafts = new LinkedHashMap<>();
				Map<Project, Map<String, String>> imports = new HashMap<>();
				for (var object : objects.entrySet()) {
					DatabaseObject dbo = object.getKey(); Project project = dbo.getProject();
					TagPolicy.require(Scope.projectObjects, dbo);
					if (project == null) throw new IOException("Pasted object has no project");
					State state = state(project); state.checkExternal();
					if (state.diagnostic != null) throw new IOException(state.diagnostic); checkWritable(state.path);
					TagDocument draft = drafts.computeIfAbsent(project, key -> state.work.copy());
					var fields = object.getValue().fields(); LinkedHashSet<String> assigned = new LinkedHashSet<>();
					while (fields.hasNext()) {
						var field = fields.next(); TagDocument.validateId(field.getKey());
						if (!(field.getValue() instanceof ObjectNode definition)) throw new IOException("Invalid clipboard tag");
						TagDocument.validateDefinition(definition, false);
						// Validate available typed fields while transporting unknown metadata unchanged.
						contributions.validateEdit(metadata(definition), metadata(definition));
						String key = field.getKey() + ":" + TagDocument.fingerprint(TagDocument.JSON.writeValueAsBytes(definition));
						assigned.add(imports.computeIfAbsent(project, ignored -> new HashMap<>()).computeIfAbsent(key,
								ignored -> importDefinition(draft, field.getKey(), definition)));
					}
					if (!assigned.isEmpty()) draft.assignments.computeIfAbsent(dbo.getFullQName(), key -> new LinkedHashSet<>()).addAll(assigned);
				}
				for (var draft : drafts.values()) draft.bytes();
				// Bindings first: each change publishes the Flow run context, whose aliases come from them.
				objects.keySet().forEach(dbo -> state(dbo.getProject()).bindings.put(dbo.getFullQName(), dbo));
				drafts.forEach((project, draft) -> { State state = state(project); state.work = draft; changed(project, state); });
				committed = true;
			}
		}
		@Override public void close() throws EngineException {
			try {
				if (committed) return;
				EngineException failure = null;
				for (var attachment : attachments.entrySet()) {
					DatabaseObject copy = attachment.getKey(), parent = attachment.getValue();
					if (copy.getParent() != parent) continue;
					try { parent.remove(copy); }
					catch (EngineException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
				}
				if (failure != null) throw failure;
				changedBefore.forEach((dbo, changed) -> dbo.hasChanged = changed);
			} finally { clipboardPaste.remove(); }
		}
	}

	public synchronized ObjectNode badges(DatabaseObject dbo) {
		return dbo instanceof Project ? projectBadges(dbo.getName()) : objectBadges(dbo);
	}
	private ObjectNode objectBadges(DatabaseObject dbo) {
		ObjectNode result = TagDocument.JSON.createObjectNode(); var badges = result.putArray("tags");
			if (!TagPolicy.supports(Scope.projectObjects, dbo)) return result;
			Project project = dbo.getProject(); if (project == null) return result;
			State state = state(project);
			for (String id : state.work.assignments.getOrDefault(dbo.getFullQName(), new LinkedHashSet<>())) {
				var badge = state.work.tags.get(id).deepCopy(); badge.put("label", displayLabel(state, id)); badge.put("id", id); badges.add(badge);
			}
		return result;
	}

	/** Presentation-only groups inside one real technical collection. Neither groups nor row IDs are DBOs. */
	public synchronized ObjectNode collection(Scope scope, String projectName, String collectionId, List<String> targets) throws IOException {
		// Presentation reads the lifecycle snapshot: no workspace serialization or filesystem scan per row.
		Project project = scope == Scope.workspaceProjects ? null : requireProject(projectName);
		State state = project == null ? workspace() : state(project);
		ObjectNode result = TagDocument.JSON.createObjectNode(); var groups = result.putArray("groups");
		Map<String, List<String>> buckets = new HashMap<>();
		for (String target : new java.util.LinkedHashSet<>(targets)) {
			if (project != null && !TagPolicy.supports(scope, resolve(project, target))) continue;
			Set<String> assigned = state.work.assignments.getOrDefault(target, new LinkedHashSet<>());
			if (assigned.isEmpty()) buckets.computeIfAbsent("", ignored -> new ArrayList<>()).add(target);
			else for (String id : assigned) buckets.computeIfAbsent(id, ignored -> new ArrayList<>()).add(target);
		}
		List<String> definitions = new ArrayList<>(buckets.keySet()); definitions.remove("");
		definitions.sort(java.util.Comparator.comparing((String id) -> displayLabel(state, id)).thenComparing(id -> id));
		// No assigned tag in this collection: adapters keep its normal children directly.
		if (!definitions.isEmpty()) definitions.add("");
		for (String id : definitions) {
			var members = buckets.getOrDefault(id, List.of());
			if (members.isEmpty()) continue;
			ObjectNode group = groups.addObject();
			String rowId = rowId(scope, collectionId, id, "");
			group.put("rowId", rowId); group.put("tagId", id); group.put("tagGroup", true); group.put("scope", scope.name());
			group.put("collectionId", collectionId); group.put("project", projectName == null ? "" : projectName);
			group.put("label", id.isEmpty() ? "Untagged" : displayLabel(state, id));
			group.put("count", members.size());
			if (!id.isEmpty()) group.put("tagHint", id.substring(0, 8));
			if (!id.isEmpty()) group.set("presentation", state.work.tags.get(id).path("presentation"));
			var occurrences = group.putArray("members");
			for (String target : members) occurrences.addObject().put("rowId", rowId(scope, collectionId, id, target)).put("targetId", target);
		}
		result.put("revision", state.revision()); return result;
	}

	private String displayLabel(State state, String id) {
		if (state.labelGeneration != state.generation || (state.work.workspace && state.labelPortableVersion != portableVersion)) {
			Map<String, Integer> counts = new HashMap<>();
			state.work.tags.values().forEach(tag -> counts.merge(tag.path("label").asText(), 1, Integer::sum));
			state.labels.clear();
			for (var entry : state.work.tags.entrySet()) {
				String tagId = entry.getKey(), label = entry.getValue().path("label").asText();
				if (state.work.workspace) {
					ObjectNode local = portableDefinition(entry.getValue());
					boolean conflict = portable.values().stream().anyMatch(values -> values.containsKey(tagId) && !values.get(tagId).equals(local));
					try { if (conflict && !state.work.presentationChosen(tagId, local)) label = "Conflicting tag"; }
					catch (IOException e) { label = "Conflicting tag"; }
				}
				state.labels.put(tagId, label.equals("Conflicting tag") || counts.getOrDefault(label, 0) > 1 ? label + " · " + tagId.substring(0, 8) : label);
			}
			state.labelGeneration = state.generation; state.labelPortableVersion = portableVersion;
		}
		return state.labels.get(id);
	}

	private static String rowId(Scope scope, String collection, String tag, String target) {
		return "tag-row:" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
				(scope.name() + "\u0000" + collection + "\u0000" + tag + "\u0000" + target).getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	private static boolean writable(Path path) { try { checkWritable(path); return true; } catch (IOException e) { return false; } }
	private static void checkWritable(Path path) throws IOException {
		Path existing = Files.exists(path) ? path : path.getParent();
		while (existing != null && !Files.exists(existing)) existing = existing.getParent();
		if (existing == null || !Files.isWritable(existing)) throw new IOException("Tag source is read-only");
		// Source paths come exclusively from project/workspace resolvers. Do not follow sidecar symlinks.
		if (Files.isSymbolicLink(path) || Files.isSymbolicLink(path.getParent())) throw new IOException("Tag source must not be a symbolic link");
	}
}
