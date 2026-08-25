/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.admin.services.studio.treeview;

import java.beans.BeanInfo;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.beans.steps.LoopStep;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.flow.FlowStudioSupport;

@ServiceDefinition(
		name = "Get",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW },
		parameters = {},
		returnValue = ""
		)
public class Get extends JSonService {
	private static final Pattern FLOW_PROJECT_CHILD = Pattern.compile("^  \\u2193(.+?) \\[([^\\]]+)\\]:.*$");
	private static final String FLOW_ICON = "studio.dbo.GetIcon?iconPath=/com/twinsoft/convertigo/beans/flow/images/flow_color_32x32.png";
	private static final String FLOW_ENGINE_ICON = "studio.dbo.GetIcon?iconPath=/com/twinsoft/convertigo/beans/flow/images/flowengine_color_32x32.png";
	private static final String FLOW_VIRTUAL_ICON = "studio.dbo.GetIcon?iconPath=/com/twinsoft/convertigo/beans/flow/images/flowvirtualobject_color_16x16.png";
	private static final Set<String> FLOW_PROJECT_WARMUPS = ConcurrentHashMap.newKeySet();

	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var flow = "true".equals(request.getParameter("flow"));
		
		var ids = request.getParameter("ids");
		if (ids != null) {
			var jids = new JSONArray(ids);
			for (int i = 0; i < jids.length(); i++) {
				var id = jids.getString(i);
				response.put(id, getChildren(id, flow));
			}
			return;
		}
		
		var id = request.getParameter("id");
		response.put("children", getChildren(id, flow));
		response.put("id", id);
	}

	private JSONArray getChildren(String id, boolean flow) throws Exception {
		var children = new JSONArray();
		if (id == null) {
			for (String projectName: Engine.theApp.databaseObjectsManager.getAllProjectNamesList(true)) {
				if (flow) {
					children.put(getProjectNode(projectName, flow));
				} else {
					var dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(projectName);
					children.put(getNode(dbo, true, flow));
				}
			}
		} else if (id.contains("/")) {
			children = getFileChildren(id);
		} else {
			var reg = Utils.parseQName.matcher(id);
			reg.matches();
			var ft = FolderType.parse(reg.group(2));
			var qname = ft == null ? id : reg.group(1);
			if (flow && ft == null) {
				var projectName = projectNameOf(qname);
				if (isProjectQName(qname)) {
					var loadedProject = Engine.theApp.databaseObjectsManager.getLoadedProjectByName(projectName);
					if (loadedProject != null) {
						return getChildren(loadedProject, ft, true, flow);
					}
					var flowProjectChildren = getFlowProjectChildren(qname);
					if (flowProjectChildren != null) {
						warmFlowProject(qname);
						return flowProjectChildren;
					}
				} else {
					var flowVirtualChildren = getLazyFlowChildren(qname);
					if (flowVirtualChildren == null) {
						flowVirtualChildren = getLazyFlowVirtualChildren(qname);
					}
					if (flowVirtualChildren != null) {
						warmFlowProject(projectName);
						return flowVirtualChildren;
					}
				}
			}
			var dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
			if (dbo != null) {
				children = getChildren(dbo, ft, true, flow);
			}
		}
		return children;
	}

	private void warmFlowProject(String projectName) {
		if (!FLOW_PROJECT_WARMUPS.add(projectName)) {
			return;
		}
		Engine.execute(() -> {
			try {
				if (Engine.theApp.databaseObjectsManager.getLoadedProjectByName(projectName) == null) {
					Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
				}
			} catch (Exception e) {
				Engine.logDatabaseObjectManager.debug("Unable to warm Flow project \"" + projectName + "\".", e);
			} finally {
				FLOW_PROJECT_WARMUPS.remove(projectName);
			}
		});
	}

	private JSONObject getProjectNode(String projectName, boolean flow) throws Exception {
		var obj = new JSONObject();
		obj.put("label", projectName);
		obj.put("name", projectName);
		obj.put("icon", "studio.dbo.GetIcon?iconPath=/com/twinsoft/convertigo/beans/core/images/project_color_32x32.png");
		obj.put("id", projectName);
		obj.put("children", true);
		if (flow) {
			obj.put("classname", Project.class.getSimpleName());
			obj.put("isLoop", false);
			obj.put("isXml", false);
			obj.put("isSourceContainer", false);
		}
		return obj;
	}

	private JSONObject getNode(DatabaseObject dbo, boolean full, boolean flow) throws Exception {
		var qname = dbo.getFullQName();
		var obj = new JSONObject();
		obj.put("label", dbo.toString());
		obj.put("name", dbo.toString());
		var iconify = iconifyIcon(dbo);
		if (!iconify.isBlank()) {
			obj.put("iconify", iconify);
		}
		obj.put("icon", "studio.dbo.GetIcon?iconPath=" + iconPath(dbo));
		obj.put("id", qname);
		if (flow) {
			obj.put("classname", dbo.getClass().getSimpleName());
			obj.put("isLoop", dbo instanceof LoopStep);
			obj.put("isXml", dbo instanceof Step step && step.isXml());
			obj.put("isSourceContainer", dbo instanceof IStepSourceContainer);
		}
		if (!full) {
			obj.put("children", lazyChildrenState(dbo, flow));
		} else {
			var jChildren = getChildren(dbo, null, false, flow);
			if (dbo instanceof Project) {
				var o = new JSONObject();
				o.put("label", "Files");
				o.put("name", "Files");
				o.put("icon", "folder");
				o.put("id", qname + '/');
				o.put("children", true);
				jChildren.put(o);
			}
			obj.put("children", loadedChildrenState(dbo, jChildren, flow));
		}
		return obj;
	}

	private boolean isProjectQName(String qname) {
		return qname != null && qname.indexOf('.') == -1 && qname.indexOf(':') == -1;
	}

	private String projectNameOf(String qname) {
		if (qname == null) {
			return "";
		}
		var dot = qname.indexOf('.');
		var colon = qname.indexOf(':');
		var end = dot == -1 ? colon : colon == -1 ? dot : Math.min(dot, colon);
		return end == -1 ? qname : qname.substring(0, end);
	}

	private JSONArray getLazyFlowChildren(String qname) throws Exception {
		var projectName = projectNameOf(qname);
		var yaml = Engine.projectYamlFile(projectName);
		if (yaml == null || !yaml.isFile()) {
			return null;
		}

		for (var line : Files.readAllLines(yaml.toPath(), StandardCharsets.UTF_8)) {
			var matcher = FLOW_PROJECT_CHILD.matcher(line);
			if (!matcher.matches()) {
				continue;
			}

			var name = matcher.group(1).trim();
			var type = matcher.group(2).trim();
			if ("flow.Flow".equals(type)) {
				if (qname.equals(projectName + ".sq:" + name)) {
					var children = new JSONArray();
					children.put(getFlowRootNode(qname));
					return children;
				}
			} else if (!"flow.FlowEngine".equals(type)) {
				return null;
			}
		}
		return null;
	}

	private JSONArray getLazyFlowVirtualChildren(String qname) throws Exception {
		var parts = qname == null ? new String[0] : qname.split("\\.");
		if (parts.length < 2) {
			return null;
		}

		var projectName = parts[0];
		var flowEngineName = flowEngineName(projectName);
		if (flowEngineName.isBlank() || !flowEngineName.equals(parts[1])) {
			return null;
		}

		if (parts.length == 2) {
			return getLazyFlowEngineChildren(projectName, flowEngineName);
		}
		if (parts.length == 3 && "catalog".equals(parts[2])) {
			return getLazyFlowCatalogChildren(projectName, flowEngineName);
		}
		return null;
	}

	private String flowEngineName(String projectName) throws Exception {
		var yaml = Engine.projectYamlFile(projectName);
		if (yaml == null || !yaml.isFile()) {
			return "";
		}

		var flowEngineName = "";
		for (var line : Files.readAllLines(yaml.toPath(), StandardCharsets.UTF_8)) {
			var matcher = FLOW_PROJECT_CHILD.matcher(line);
			if (!matcher.matches()) {
				continue;
			}

			var type = matcher.group(2).trim();
			if ("flow.FlowEngine".equals(type)) {
				flowEngineName = matcher.group(1).trim();
			} else if (!"flow.Flow".equals(type)) {
				return "";
			}
		}
		return flowEngineName;
	}

	private JSONArray getFlowProjectChildren(String projectName) throws Exception {
		var yaml = Engine.projectYamlFile(projectName);
		if (yaml == null || !yaml.isFile()) {
			return null;
		}

		var flowNames = new ArrayList<String>();
		var flowEngineName = "";
		var hasProjectChild = false;
		var hasNonFlowChild = false;
		for (var line : Files.readAllLines(yaml.toPath(), StandardCharsets.UTF_8)) {
			var matcher = FLOW_PROJECT_CHILD.matcher(line);
			if (!matcher.matches()) {
				continue;
			}

			hasProjectChild = true;
			var name = matcher.group(1).trim();
			var type = matcher.group(2).trim();
			if ("flow.Flow".equals(type)) {
				flowNames.add(name);
			} else if ("flow.FlowEngine".equals(type)) {
				flowEngineName = name;
			} else {
				hasNonFlowChild = true;
				break;
			}
		}
		if (!hasProjectChild || hasNonFlowChild) {
			return null;
		}

		var children = new JSONArray();
		if (!flowNames.isEmpty()) {
			children.put(getFlowSequenceFolder(projectName, flowNames));
		}
		if (!flowEngineName.isBlank()) {
			children.put(getFlowEngineNode(projectName, flowEngineName));
		}
		return children;
	}

	private JSONArray getLazyFlowEngineChildren(String projectName, String flowEngineName) throws Exception {
		var children = new JSONArray();
		children.put(getFlowVirtualNode(projectName, flowEngineName, "engine", projectName + ".Engine", false));
		children.put(getFlowVirtualNode(projectName, flowEngineName, "catalog", "Catalog", true));
		return children;
	}

	private JSONArray getLazyFlowCatalogChildren(String projectName, String flowEngineName) throws Exception {
		var children = new JSONArray();
		children.put(getFlowVirtualNode(projectName, flowEngineName, "catalog.blocks", "Blocks", true));
		children.put(getFlowVirtualNode(projectName, flowEngineName, "catalog.libraries", "Libraries", false));
		children.put(getFlowVirtualNode(projectName, flowEngineName, "catalog.types", "Types", true));
		return children;
	}

	private JSONObject getFlowVirtualNode(String projectName, String flowEngineName, String path, String label, boolean children) throws Exception {
		var obj = new JSONObject();
		obj.put("label", label);
		obj.put("name", label);
		obj.put("icon", FLOW_VIRTUAL_ICON);
		obj.put("id", projectName + "." + flowEngineName + "." + path);
		obj.put("classname", FlowVirtualObject.class.getSimpleName());
		obj.put("isLoop", false);
		obj.put("isXml", false);
		obj.put("isSourceContainer", false);
		obj.put("children", children);
		return obj;
	}

	private JSONObject getFlowSequenceFolder(String projectName, List<String> flowNames) throws Exception {
		var folder = new JSONObject();
		folder.put("label", FolderType.SEQUENCE.displayName());
		folder.put("name", FolderType.SEQUENCE.displayName());
		folder.put("icon", "folder");
		folder.put("id", projectName + ':' + FolderType.SEQUENCE.shortName());

		var children = new JSONArray();
		folder.put("children", children);
		for (var flowName : flowNames) {
			children.put(getFlowNode(projectName, flowName));
		}
		return folder;
	}

	private JSONObject getFlowNode(String projectName, String flowName) throws Exception {
		var obj = new JSONObject();
		obj.put("label", flowName);
		obj.put("name", flowName);
		obj.put("icon", FLOW_ICON);
		obj.put("id", projectName + ".sq:" + flowName);
		obj.put("classname", Flow.class.getSimpleName());
		obj.put("isLoop", false);
		obj.put("isXml", false);
		obj.put("isSourceContainer", false);
		obj.put("children", true);
		return obj;
	}

	private JSONObject getFlowRootNode(String flowQName) throws Exception {
		var obj = new JSONObject();
		obj.put("label", "Flow");
		obj.put("name", "Flow");
		obj.put("icon", FLOW_VIRTUAL_ICON);
		obj.put("id", flowQName + ".flow");
		obj.put("classname", FlowVirtualObject.class.getSimpleName());
		obj.put("isLoop", false);
		obj.put("isXml", false);
		obj.put("isSourceContainer", false);
		obj.put("children", true);
		return obj;
	}

	private JSONObject getFlowEngineNode(String projectName, String flowEngineName) throws Exception {
		var obj = new JSONObject();
		obj.put("label", flowEngineName);
		obj.put("name", flowEngineName);
		obj.put("icon", FLOW_ENGINE_ICON);
		obj.put("id", projectName + "." + flowEngineName);
		obj.put("classname", FlowEngine.class.getSimpleName());
		obj.put("isLoop", false);
		obj.put("isXml", false);
		obj.put("isSourceContainer", false);
		obj.put("children", true);
		return obj;
	}

	static Object lazyChildrenState(DatabaseObject dbo, boolean flow) throws Exception {
		if (dbo.hasDatabaseObjectChildren()) {
			return true;
		}
		return acceptsChildren(dbo, flow) ? 0 : false;
	}

	static Object loadedChildrenState(DatabaseObject dbo, JSONArray children, boolean flow) throws Exception {
		if (children != null && children.length() > 0) {
			return children;
		}
		return acceptsChildren(dbo, flow) ? 0 : false;
	}

	private static boolean acceptsChildren(DatabaseObject dbo, boolean flow) throws Exception {
		if (flow && (dbo instanceof Flow || dbo instanceof FlowEngine)) {
			return true;
		}
		if (dbo instanceof FlowVirtualObject flowVirtualObject
				&& FlowStudioSupport.frontendBlockCanContainChildren(flowVirtualObject)) {
			return true;
		}
		return false;
	}

	private String iconPath(DatabaseObject dbo) {
		var iconPath = "";
		if (dbo instanceof FlowVirtualObject flowVirtualObject) {
			iconPath = firstNonBlank(flowVirtualObject.getVirtualInfoObject(), "iconFile16", "iconFile", "iconFile32", "iconSvg");
			if (iconPath.isBlank()) {
				iconPath = firstNonBlank(flowVirtualObject.getDefinitionObject(), "iconFile16", "iconFile", "iconFile32", "iconSvg", "icon");
			}
		}
		if (iconPath.isBlank() || isIconifyIcon(iconPath)) {
			iconPath = MySimpleBeanInfo.getIconName(dbo, BeanInfo.ICON_COLOR_32x32);
		}
		if (iconPath.startsWith(Engine.PROJECTS_PATH)) {
			return "projects:" + iconPath.substring(Engine.PROJECTS_PATH.length());
		}
		if (iconPath.startsWith(Engine.USER_WORKSPACE_PATH)) {
			return "workspace:" + iconPath.substring(Engine.USER_WORKSPACE_PATH.length());
		}
		return iconPath;
	}

	static String iconifyIcon(DatabaseObject dbo) {
		if (!(dbo instanceof FlowVirtualObject flowVirtualObject)) {
			return "";
		}
		var icon = firstNonBlank(flowVirtualObject.getVirtualInfoObject(), "iconify", "icon");
		if (!isIconifyIcon(icon)) {
			icon = firstNonBlank(flowVirtualObject.getDefinitionObject(), "iconify", "icon");
		}
		return isIconifyIcon(icon) ? icon : "";
	}

	private static String firstNonBlank(JSONObject object, String... keys) {
		if (object == null) {
			return "";
		}
		for (var key : keys) {
			var value = object.optString(key, "");
			if (!value.isBlank()) {
				return value;
			}
		}
		return "";
	}

	private static boolean isIconifyIcon(String icon) {
		return icon != null && icon.matches("[A-Za-z][A-Za-z0-9_-]*:[A-Za-z0-9_.-]+");
	}

	private JSONArray getChildren(DatabaseObject dbo, FolderType ft, boolean full, boolean flow) throws Exception {
		if (dbo == null) {
			return new JSONArray();
		}
		var qname = dbo.getFullQName();
		var children = dbo.getDatabaseObjectChildren();
		var jChildren = new JSONArray();
		var map = new HashMap<FolderType, JSONObject>();
		for (var child: children) {
			var cft = child.getFolderType();
			if (ft != null && cft != ft) {
				continue;
			}
			var jChild = jChildren;
			if (ft == null && cft != FolderType.NONE) {
				var o = map.get(cft);
				if (o == null) {
					map.put(cft, o = new JSONObject());
					jChildren.put(o);
					o.put("label", cft.displayName());
					o.put("name", cft.displayName());
					o.put("icon", "folder");
					o.put("id", qname + ':' + cft.shortName());
					if (full) {
						o.put("children", jChild = new JSONArray());
					} else {
						o.put("children", true);
						continue;
					}
				} else if (full) {
					jChild = o.getJSONArray("children");
				} else {
					continue;
				}
			}
			var node = getNode(child, ft != null, flow);
			jChild.put(node);
		}
		return jChildren;
	}
	
	private JSONArray getFileChildren(String id) throws Exception {
		var jChildren = new JSONArray();
		var split = id.split("/", 2);
		var project = (Project) Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(split[0]);
		var root = project.getDirFile();
		var file = new File(root, split[1]);
		if (file.isDirectory()) {
			var files = file.listFiles();
			Arrays.sort(files, (Comparator<File>) (f1, f2) -> {
				if (f1.isDirectory() == f2.isDirectory()) {
					return f1.getName().compareTo(f2.getName());
				}
				return f1.isDirectory() ? -1 : 1;
			});
			for (var f : files) {
				var o = new JSONObject();
				jChildren.put(o);
				o.put("label", f.getName());
				o.put("name", f.getName());
				o.put("icon", f.isDirectory() ? "folder" : "file");
				o.put("id", id + "/" + f.getName());
				o.put("children", f.isDirectory());
			}
		}
		return jChildren;
	}
}
