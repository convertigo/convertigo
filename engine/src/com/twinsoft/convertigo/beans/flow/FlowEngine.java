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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.DatabaseObjectTypes;
import com.twinsoft.convertigo.engine.flow.FlowEngineBridge;

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

	private String engineQName = FlowEngineBridge.DEFAULT_ENGINE_QNAME;
	private String engineSource = DEFAULT_ENGINE_SOURCE;
	private transient boolean engineSourceDirty = false;
	private transient long engineSourceFileLastModified = -1;
	private transient Map<String, String> frontendSourceDrafts = new LinkedHashMap<>();
	private transient String flowVirtualChildrenCacheKey = "";
	private transient List<DatabaseObject> flowVirtualChildrenCache = null;

	public FlowEngine() {
		super();
		databaseType = DatabaseObjectTypes.FlowEngine.name();
	}

	@Override
	public FlowEngine clone() throws CloneNotSupportedException {
		var clone = (FlowEngine) super.clone();
		clone.frontendSourceDrafts = new LinkedHashMap<>();
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
	public boolean hasDatabaseObjectChildren() {
		return !getFlowVirtualChildren().isEmpty();
	}

	public List<DatabaseObject> getFlowVirtualChildren() {
		var source = getEngineSource();
		var key = FlowEngineBridge.cacheGeneration() + "\n" + getQName() + "\n" + engineQName + "\n"
				+ source + "\n" + frontendSourceDrafts().hashCode();
		if (flowVirtualChildrenCache != null && key.equals(flowVirtualChildrenCacheKey)) {
			return new ArrayList<>(flowVirtualChildrenCache);
		}
		var children = FlowVirtualProjector.childrenOf(this);
		flowVirtualChildrenCacheKey = key;
		flowVirtualChildrenCache = children;
		return new ArrayList<>(children);
	}

	@Override
	public void setParent(DatabaseObject databaseObject) {
		super.setParent(databaseObject);
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
		} catch (Exception e) {
			Engine.logBeans.warn("(FlowEngine) Unable to preload " + getQName(), e);
		}
	}

	@Override
	public Element toXml(Document document) throws EngineException {
		writeEngineSourceFile();
		writeSourceDraftFiles();
		var element = super.toXml(document);
		removeSerializedProperty(element, "engineSource");
		return element;
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

	public Map<String, String> getSourceDrafts() {
		return new LinkedHashMap<>(frontendSourceDrafts());
	}

	public String getFrontendSource(String sourcePath) throws EngineException {
		return getSource(sourcePath);
	}

	public String getSource(String sourcePath) throws EngineException {
		var key = canonicalSourcePath(sourcePath);
		if (frontendSourceDrafts().containsKey(key)) {
			return frontendSourceDrafts().get(key);
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
		var key = canonicalSourcePath(sourcePath);
		source = source == null ? "" : source;
		var saved = "";
		try {
			var file = new File(key);
			if (file.isFile()) {
				saved = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
			}
		} catch (Exception e) {
			throw new EngineException("Unable to read saved Flow source file \"" + key + "\".", e);
		}
		var drafts = frontendSourceDrafts();
		if (saved.equals(source)) {
			if (drafts.remove(key) != null) {
				clearFlowVirtualChildrenCache();
				changed();
			} else {
				clearFlowVirtualChildrenCache();
			}
			return;
		}
		if (!source.equals(drafts.get(key))) {
			drafts.put(key, source);
			clearFlowVirtualChildrenCache();
			changed();
		} else {
			clearFlowVirtualChildrenCache();
		}
	}

	public boolean isFrontendSourceDirty(String sourcePath) throws EngineException {
		return isSourceDirty(sourcePath);
	}

	public boolean isSourceDirty(String sourcePath) throws EngineException {
		return frontendSourceDrafts().containsKey(canonicalSourcePath(sourcePath));
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
		return new File(new File(project.getDirFile(), "libs/flow"), "engine.yaml");
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
		var drafts = frontendSourceDrafts();
		if (drafts.isEmpty()) {
			return;
		}
		try {
			for (var entry : new LinkedHashMap<>(drafts).entrySet()) {
				var file = new File(entry.getKey());
				file.getParentFile().mkdirs();
				FileUtils.writeStringToFile(file, entry.getValue(), StandardCharsets.UTF_8);
			}
			drafts.clear();
			clearFlowVirtualChildrenCache();
		} catch (Exception e) {
			throw new EngineException("Unable to write Flow source draft files.", e);
		}
	}

	private Map<String, String> frontendSourceDrafts() {
		if (frontendSourceDrafts == null) {
			frontendSourceDrafts = new LinkedHashMap<>();
		}
		return frontendSourceDrafts;
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
			var supported = name.endsWith(".front.json") || name.endsWith(".flow.svelte")
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
