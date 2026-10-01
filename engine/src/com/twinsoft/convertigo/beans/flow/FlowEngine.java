/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.flow;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.DatabaseObjectTypes;
import com.twinsoft.convertigo.engine.flow.FlowEngineBridge;
import com.twinsoft.convertigo.engine.flow.FlowSourceLayout;

@DboCategoryInfo(
		getCategoryId = "FlowEngine",
		getCategoryName = "Flow engine",
		getIconClassCSS = "convertigo-action-newFlowEngine"
	)
public class FlowEngine extends DatabaseObject {

	private static final long serialVersionUID = -304535780573293211L;

	private static final String DEFAULT_ENGINE_SOURCE = "version: 1\n"
			+ "engineQName: " + FlowEngineBridge.DEFAULT_ENGINE_QNAME + "\n"
			+ "bindings: {}\n"
			+ "config: {}\n";
	// A removal is an explicit working-copy state, never an empty source file.
	private record SourceDraft(String source, boolean removed) { }
	private static final SourceDraft REMOVED_SOURCE = new SourceDraft("", true);
	private static final Map<String, SourceDraft> sourceDrafts = new ConcurrentHashMap<>();
	// Only paths, never draft text: a reload must notify source consumers after
	// the replacement project is loaded, without retaining its discarded values.
	private static final Map<String, Set<String>> discardedSourcePaths = new ConcurrentHashMap<>();
	// Clones of one project share its lock; unrelated projects never wait for its Save I/O.
	private static final Map<String, Object> sourceLocks = new ConcurrentHashMap<>();
	public record SourceChanges(Map<String, String> writes, Set<String> removals) { }

	private String engineQName = FlowEngineBridge.DEFAULT_ENGINE_QNAME;
	private String engineSource = DEFAULT_ENGINE_SOURCE;
	private transient boolean engineSourceDirty = false;
	private transient long engineSourceFileLastModified = -1;
	private transient String flowVirtualChildrenCacheKey = "";
	private transient List<DatabaseObject> flowVirtualChildrenCache = null;

	public FlowEngine() {
		super();
		databaseType = DatabaseObjectTypes.FlowEngine.name();
	}

	@Override
	public FlowEngine clone() throws CloneNotSupportedException {
		var clone = (FlowEngine) super.clone();
		clone.clearFlowVirtualChildrenCache();
		return clone;
	}

	@Override
	public List<DatabaseObject> getDatabaseObjectChildren() {
		return new ArrayList<>(getFlowVirtualChildren());
	}

	@Override
	public List<DatabaseObject> getAllChildren() {
		return getDatabaseObjectChildren();
	}

	@Override
	public DatabaseObject getDatabaseObjectChild(String name) {
		return FlowVirtualObject.findChild(getDatabaseObjectChildren(), name);
	}

	@Override
	public boolean hasDatabaseObjectChildren() {
		return !getFlowVirtualChildren().isEmpty();
	}

	public List<DatabaseObject> getFlowVirtualChildren() {
		var key = flowVirtualChildrenCacheKey();
		if (flowVirtualChildrenCache != null && key.equals(flowVirtualChildrenCacheKey)) {
			return new ArrayList<>(flowVirtualChildrenCache);
		}
		var children = FlowVirtualProjector.childrenOf(this);
		flowVirtualChildrenCacheKey = key;
		flowVirtualChildrenCache = children;
		return new ArrayList<>(children);
	}

	private String flowVirtualChildrenCacheKey() {
		return FlowEngineBridge.cacheGeneration() + "\n" + getQName() + "\n" + engineQName + "\n"
				+ getEngineSource() + "\n" + sourceChanges().hashCode();
	}

	/**
	 * Captures the already projected virtual tree without causing a projection on a
	 * cache miss. The snapshot can be restored after a source mutation when the
	 * provider returned and applied a fresh replacement for the affected subtree.
	 */
	public List<DatabaseObject> snapshotFlowVirtualChildrenCache() {
		return flowVirtualChildrenCache == null ? null : new ArrayList<>(flowVirtualChildrenCache);
	}

	public void restoreFlowVirtualChildrenCache(List<DatabaseObject> snapshot) {
		if (snapshot == null) {
			return;
		}
		flowVirtualChildrenCacheKey = flowVirtualChildrenCacheKey();
		flowVirtualChildrenCache = new ArrayList<>(snapshot);
	}

	@Override
	public void setParent(DatabaseObject databaseObject) {
		super.setParent(databaseObject);
		if (databaseObject == null || !isOriginal()) {
			return;
		}
		ensureEngineProjectReference();
		if (!ownsRuntime()) {
			return;
		}
		if (isImporting) {
			DatabaseObjectsManager.getProjectLoadingData().addAfterLoaded(this::preloadRuntime);
		} else {
			preloadRuntime();
		}
	}

	private boolean ownsRuntime() {
		var project = getProject();
		var qname = getEngineQName();
		var separator = qname == null ? -1 : qname.indexOf('.');
		return project != null && separator > 0 && project.getName().equals(qname.substring(0, separator));
	}

	private void preloadRuntime() {
		try {
			var result = new FlowEngineBridge().preload(this);
			Engine.logBeans.info("(FlowEngine) Preloaded " + getQName() + " in "
					+ result.optLong("durationMs") + " ms (" + result.optInt("blockCount") + " blocks)");
			Flow.runtimePrepared(getEngineQName());
		} catch (Exception e) {
			Engine.logBeans.warn("(FlowEngine) Unable to preload " + getQName(), e);
		}
	}

	/**
	 * The Engine source and the working copies live in their files, which the save of the project writes
	 * (saveSources): a serialization that is not a save, as a state of the undo history, the Git view, a
	 * search or a copy, never writes them.
	 */
	@Override
	public Element toXml(Document document) throws EngineException {
		var element = super.toXml(document);
		removeSerializedProperty(element, "engineSource");
		return element;
	}

	/** Writes the Engine source, the working copies and the dependencies, as the save of the project. */
	public void saveSources() throws EngineException {
		writeEngineSourceFile();
		writeSourceDraftFiles();
		writeDependenciesFile();
	}

	public String getEngineQName() {
		return engineQName;
	}

	public void setEngineQName(String engineQName) {
		if (engineQName == null || engineQName.isBlank()) {
			engineQName = FlowEngineBridge.DEFAULT_ENGINE_QNAME;
		}
		if (!this.engineQName.equals(engineQName)) {
			this.engineQName = engineQName;
			clearFlowVirtualChildrenCache();
			changed();
			ensureEngineProjectReference();
		}
	}

	public String getEngineSource() {
		loadEngineSourceFile();
		return engineSource == null || engineSource.isBlank() ? DEFAULT_ENGINE_SOURCE : engineSource;
	}

	public void setEngineSource(String engineSource) {
		if (engineSource == null || engineSource.isBlank()) {
			engineSource = DEFAULT_ENGINE_SOURCE;
		}
		if (!this.engineSource.equals(engineSource)) {
			this.engineSource = engineSource;
			engineSourceDirty = true;
			clearFlowVirtualChildrenCache();
			changed();
		}
	}

	public Map<String, String> getFrontendSourceDrafts() {
		return getSourceDrafts();
	}

	/** Read-only lifecycle preflight. Never loads or saves the Engine source. */
	public boolean isEngineSourceDirty() {
		return engineSourceDirty;
	}

	/** Includes drafts surviving the owner instance; do not expose their contents. */
	public static boolean hasSourceDrafts(File projectDirectory) throws java.io.IOException {
		var root = projectDirectory.getCanonicalPath();
		var prefix = root + File.separator;
		synchronized (sourceLock(root)) {
			return sourceDrafts.keySet().stream().anyMatch(path -> path.startsWith(prefix));
		}
	}

	public Map<String, String> getSourceDrafts() {
		return new LinkedHashMap<>(getSourceChanges().writes());
	}

	/** Explicitly absent files in the effective source view, including saved files. */
	public Set<String> getSourceRemovals() {
		return new LinkedHashSet<>(getSourceChanges().removals());
	}

	/** One coherent snapshot for history, providers and source publication. */
	public SourceChanges getSourceChanges() {
		var writes = new LinkedHashMap<String, String>();
		var removals = new LinkedHashSet<String>();
		for (var entry : sourceChanges().entrySet()) {
			if (entry.getValue().removed()) removals.add(entry.getKey());
			else writes.put(entry.getKey(), entry.getValue().source());
		}
		return new SourceChanges(Map.copyOf(writes), Set.copyOf(removals));
	}

	private Map<String, SourceDraft> sourceChanges() {
		var drafts = new LinkedHashMap<String, SourceDraft>();
		var root = sourceRootPath();
		if (root == null) {
			return drafts;
		}
		var prefix = root + File.separator;
		synchronized (sourceLock(root)) {
			for (var entry : sourceDrafts.entrySet()) {
				if (entry.getKey().startsWith(prefix)) {
					drafts.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return drafts;
	}

	public String getFrontendSource(String sourcePath) throws EngineException {
		return getSource(sourcePath);
	}

	public String getSource(String sourcePath) throws EngineException {
		var key = canonicalSourcePath(sourcePath);
		SourceDraft draft;
		synchronized (sourceLock()) { draft = sourceDrafts.get(key); }
		if (draft != null) {
			if (draft.removed()) throw new EngineException("Flow source file is removed in the working copy: " + key);
			return draft.source();
		}
		try {
			return FileUtils.readFileToString(new File(key), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new EngineException("Unable to read Flow source file \"" + key + "\".", e);
		}
	}

	public void setFrontendSource(String sourcePath, String source) throws EngineException {
		setSource(sourcePath, source);
	}

	public void setSource(String sourcePath, String source) throws EngineException {
		var sources = new LinkedHashMap<String, String>();
		sources.put(sourcePath, source);
		setSources(sources);
	}

	/** Validate the whole source update before changing any working copy. No disk writes. */
	public void setSources(Map<String, String> sources) throws EngineException {
		applySourceChanges(sources, List.of());
	}

	/**
	 * Apply one source plan in memory. A move is writes at new paths plus explicit
	 * removals at old paths. Null in the existing text API still means empty text.
	 */
	public void applySourceChanges(Map<String, String> sources, Collection<String> removals) throws EngineException {
		synchronized (sourceLock()) {
			var updates = validatedSourceChanges(sources, removals);
			var modified = false;
			for (var entry : updates.entrySet()) {
				var key = entry.getKey();
				var draft = entry.getValue();
				if (draft == null) {
					modified |= sourceDrafts.remove(key) != null;
				} else if (!draft.equals(sourceDrafts.put(key, draft))) {
					modified = true;
				}
			}
			clearFlowVirtualChildrenCache();
			if (modified) changed();
		}
	}

	private Map<String, SourceDraft> validatedSourceChanges(Map<String, String> sources,
			Collection<String> removals) throws EngineException {
		var updates = new LinkedHashMap<String, SourceDraft>();
		var writePaths = new LinkedHashSet<String>();
		for (var entry : sources.entrySet()) {
			var key = canonicalSourcePath(entry.getKey());
			if (updates.containsKey(key)) {
				throw new EngineException("Duplicate Flow source destination: " + entry.getKey());
			}
			writePaths.add(key);
			var source = entry.getValue() == null ? "" : entry.getValue();
			try {
				var file = new File(key);
				if (file.exists() && !file.isFile()) {
					throw new EngineException("Flow source destination is not a file: " + key);
				}
				for (var parent = file.getParentFile(); parent != null; parent = parent.getParentFile()) {
					if (parent.exists() && !parent.isDirectory()) {
						throw new EngineException("Flow source parent is not a directory: " + parent);
					}
				}
				// An absent empty file is still a creation, not a discarded draft.
				var sameAsSaved = file.isFile() && source.equals(FileUtils.readFileToString(file, StandardCharsets.UTF_8));
				updates.put(key, sameAsSaved ? null : new SourceDraft(source, false));
			} catch (EngineException e) {
				throw e;
			} catch (Exception e) {
				throw new EngineException("Unable to read saved Flow source file \"" + key + "\".", e);
			}
		}
		for (var sourcePath : removals) {
			var key = canonicalSourcePath(sourcePath);
			if (updates.containsKey(key)) throw new EngineException("Duplicate Flow source destination: " + sourcePath);
			var file = new File(key);
			if (file.exists() && !file.isFile()) throw new EngineException("Flow source removal is not a file: " + key);
			// Removing a draft-only creation simply cancels it; there is no saved file to hide.
			updates.put(key, file.isFile() ? REMOVED_SOURCE : null);
		}
		for (var key : writePaths) {
			if (sourceDrafts.entrySet().stream().anyMatch(entry -> !entry.getValue().removed()
					&& entry.getKey().startsWith(key + File.separator))) {
				throw new EngineException("Flow source destination contains file working copies: " + key);
			}
			for (var parent = new File(key).getParentFile(); parent != null; parent = parent.getParentFile()) {
				var path = parent.getPath();
				var parentDraft = sourceDrafts.get(path);
				if (writePaths.contains(path)
						|| parentDraft != null && !parentDraft.removed()) {
					throw new EngineException("Flow source parent is a file working copy: " + path);
				}
			}
		}
		return updates;
	}

	/** Drop the working copy of a source (a created source not saved yet disappears). */
	public boolean discardSource(String sourcePath) throws EngineException {
		boolean removed;
		synchronized (sourceLock()) { removed = sourceDrafts.remove(canonicalSourcePath(sourcePath)) != null; }
		if (removed) {
			clearFlowVirtualChildrenCache();
		}
		return removed;
	}

	public boolean hasSource(String sourcePath) throws EngineException {
		var key = canonicalSourcePath(sourcePath);
		synchronized (sourceLock()) {
			var draft = sourceDrafts.get(key);
			return draft == null ? new File(key).isFile() : !draft.removed();
		}
	}

	public boolean isFrontendSourceDirty(String sourcePath) throws EngineException {
		return isSourceDirty(sourcePath);
	}

	public boolean isSourceDirty(String sourcePath) throws EngineException {
		synchronized (sourceLock()) { return sourceDrafts.containsKey(canonicalSourcePath(sourcePath)); }
	}

	@Override
	protected String defaultBeanName(String displayName) {
		return "FlowEngine";
	}

	private File getEngineSourceFile() {
		var project = getProject();
		if (project == null) {
			return null;
		}
		return new File(project.getDirFile(), sourceLayout().path("engine.yaml"));
	}

	protected FlowSourceLayout sourceLayout() {
		return FlowSourceLayout.current();
	}

	private void ensureEngineProjectReference() {
		var project = getProject();
		var engineProjectName = engineProjectName();
		if (project == null || engineProjectName.isBlank() || project.getName().equals(engineProjectName)
				|| Engine.theApp == null || Engine.theApp.referencedProjectManager == null) {
			return;
		}
		try {
			Engine.theApp.referencedProjectManager.getReferenceFromProject(project, engineProjectName);
		} catch (Exception e) {
			Engine.logBeans.warn("Unable to ensure FlowEngine project reference to \"" + engineProjectName + "\".", e);
		}
	}

	private String engineProjectName() {
		var qname = engineQName == null || engineQName.isBlank() ? FlowEngineBridge.DEFAULT_ENGINE_QNAME : engineQName.trim();
		var dot = qname.indexOf('.');
		return dot == -1 ? "" : qname.substring(0, dot);
	}

	private void loadEngineSourceFile() {
		if (engineSourceDirty) {
			return;
		}
		var file = getEngineSourceFile();
		if (file == null || !file.isFile()) {
			return;
		}
		var lastModified = file.lastModified();
		if (lastModified == engineSourceFileLastModified) {
			return;
		}
		try {
			engineSource = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
			engineSourceFileLastModified = lastModified;
			clearFlowVirtualChildrenCache();
		} catch (Exception e) {
			Engine.logBeans.warn("Unable to read FlowEngine source file \"" + file.getAbsolutePath() + "\".", e);
		}
	}

	private void writeEngineSourceFile() throws EngineException {
		var file = getEngineSourceFile();
		if (file == null) {
			return;
		}
		try {
			sourceLayout().ensureHttpIgnore(getProject().getDirFile());
			file.getParentFile().mkdirs();
			FileUtils.writeStringToFile(file, getEngineSource(), StandardCharsets.UTF_8);
			engineSourceDirty = false;
			engineSourceFileLastModified = file.lastModified();
			clearFlowVirtualChildrenCache();
		} catch (Exception e) {
			throw new EngineException("Unable to write FlowEngine source file \"" + file.getAbsolutePath() + "\".", e);
		}
	}

	private void writeSourceDraftFiles() throws EngineException {
		synchronized (sourceLock()) {
			var drafts = sourceChanges();
			if (drafts.isEmpty()) return;
			var changes = getSourceChanges();
			var writes = changes.writes();
			var removals = changes.removals();
			// Recheck confinement and filesystem shape at publication, not just at edit time.
			validatedSourceChanges(writes, removals);
			for (var key : drafts.keySet()) {
				if (!key.equals(canonicalSourcePath(key))) throw new EngineException("Flow source path changed before Save: " + key);
			}
			try {
				sourceLayout().ensureHttpIgnore(getProject().getDirFile());
				FlowSourcePublisher.publish(writes, removals, getProject().getDirFile());
				for (var entry : drafts.entrySet()) sourceDrafts.remove(entry.getKey(), entry.getValue());
				clearFlowVirtualChildrenCache();
			} catch (Exception e) {
				throw new EngineException("Unable to publish Flow source working copies; drafts retained.", e);
			}
		}
	}

	/**
	 * Records the version of each definer project whose definitions the project uses
	 * (_flow/dependencies.json), so its sources keep the defaults they were written against.
	 * Written only when its content changes; never blocks the save.
	 */
	private void writeDependenciesFile() {
		var project = getProject();
		// The Flow runtime answers only in a started engine (not in bare serialization tests).
		if (project == null || !Engine.isStarted || Engine.logBeans == null) {
			return;
		}
		try {
			var response = new FlowEngineBridge().dependencies(this);
			if (!response.optBoolean("ok", false)) {
				Engine.logBeans.warn("(FlowEngine) Unable to compute Flow dependencies of " + project.getName() + ": " + response.opt("error"));
				return;
			}
			var warnings = response.optJSONArray("warnings");
			for (int i = 0; warnings != null && i < warnings.length(); i++) {
				Engine.logBeans.warn("(FlowEngine) " + warnings.optJSONObject(i).optString("message"));
			}
			if (response.optBoolean("changed", false)) {
				var file = new File(project.getDirFile(), sourceLayout().path("dependencies.json"));
				file.getParentFile().mkdirs();
				FileUtils.writeStringToFile(file, response.optString("source"), StandardCharsets.UTF_8);
			}
		} catch (Exception e) {
			Engine.logBeans.warn("(FlowEngine) Unable to write Flow dependencies of " + project.getName(), e);
		}
	}

	static void projectUnloaded(Project project) {
		if (project == null) {
			return;
		}
		try {
			var root = project.getDirFile().getCanonicalPath();
			var prefix = root + File.separator;
			synchronized (sourceLock(root)) {
				var removed = new LinkedHashSet<String>();
				sourceDrafts.keySet().removeIf(path -> {
					if (!path.startsWith(prefix)) return false;
					removed.add(path);
					return true;
				});
				if (!removed.isEmpty()) {
					discardedSourcePaths.merge(root, removed, (before, after) -> {
						var all = new LinkedHashSet<>(before);
						all.addAll(after);
						return all;
					});
				}
			}
		} catch (Exception e) {
			Engine.logBeans.debug("Unable to discard Flow source drafts for project \"" + project.getName() + "\".", e);
		}
	}

	/** The paths whose effective content changed when the previous project was unloaded. */
	static Set<String> takeDiscardedSourcePaths(Project project) {
		if (project == null) return Set.of();
		try {
			var paths = discardedSourcePaths.remove(project.getDirFile().getCanonicalPath());
			return paths == null ? Set.of() : paths;
		} catch (Exception e) {
			Engine.logBeans.debug("Unable to resolve discarded Flow source paths.", e);
			return Set.of();
		}
	}

	private Object sourceLock() {
		var root = sourceRootPath();
		return sourceLock(root == null ? "" : root);
	}

	private static Object sourceLock(String root) {
		return sourceLocks.computeIfAbsent(root, key -> new Object());
	}

	private String sourceRootPath() {
		var project = getProject();
		if (project == null) {
			return null;
		}
		try {
			return project.getDirFile().getCanonicalPath();
		} catch (Exception e) {
			return project.getDirFile().getAbsolutePath();
		}
	}

	private String canonicalFrontendSourcePath(String sourcePath) throws EngineException {
		return canonicalSourcePath(sourcePath);
	}

	private String canonicalSourcePath(String sourcePath) throws EngineException {
		try {
			if (sourcePath == null || sourcePath.isBlank()) {
				throw new EngineException("Flow source path is empty.");
			}
			var file = new File(sourcePath).getCanonicalFile();
			var name = file.getName();
			var supported = name.endsWith(".flow.svelte")
					|| name.endsWith(".flow.css")
					|| name.endsWith(".block.js") || name.endsWith(".type.yaml")
					|| name.endsWith(".schema.json") || name.endsWith(".yaml")
					|| name.endsWith(".json") || name.endsWith(".js") || name.endsWith(".svelte");
			if (!supported) {
				throw new EngineException("Unsupported Flow source file: " + sourcePath);
			}
			var project = getProject();
			if (project != null) {
				var rootPath = project.getDirFile().getCanonicalPath();
				var filePath = file.getCanonicalPath();
				if (!filePath.equals(rootPath) && !filePath.startsWith(rootPath + File.separator)) {
					throw new EngineException("Flow source file is outside the project: " + sourcePath);
				}
			}
			return file.getAbsolutePath();
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Unable to resolve Flow source file \"" + sourcePath + "\".", e);
		}
	}

	public void clearFlowVirtualChildrenCache() {
		flowVirtualChildrenCacheKey = "";
		flowVirtualChildrenCache = null;
	}

	private static void removeSerializedProperty(Element element, String propertyName) {
		var properties = element.getChildNodes();
		for (var i = properties.getLength() - 1; i >= 0; i--) {
			var node = properties.item(i);
			if (node instanceof Element property
					&& "property".equals(property.getTagName())
					&& propertyName.equals(property.getAttribute("name"))) {
				element.removeChild(property);
			}
		}
	}
}
