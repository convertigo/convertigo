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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
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

	public FlowEngine() {
		super();
		databaseType = DatabaseObjectTypes.FlowEngine.name();
	}

	@Override
	public FlowEngine clone() throws CloneNotSupportedException {
		return (FlowEngine) super.clone();
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
		return FlowVirtualProjector.childrenOf(this);
	}

	@Override
	public void setParent(DatabaseObject databaseObject) {
		super.setParent(databaseObject);
		ensureEngineProjectReference();
	}

	@Override
	public Element toXml(Document document) throws EngineException {
		writeEngineSourceFile();
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
			changed();
		}
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
		} catch (Exception e) {
			throw new EngineException("Unable to write FlowEngine source file \"" + file.getAbsolutePath() + "\".", e);
		}
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
