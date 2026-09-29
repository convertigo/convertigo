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

package com.twinsoft.convertigo.engine.admin.services.studio.git;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.OrTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.BeansDefaultValues;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder.BuilderUtils;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.GitUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.YamlConverter;

/**
 * The objects of a project changed since a commit, as the tree of the web Studio shows them: added, removed,
 * modified with their properties, renamed or moved. The objects are matched by their priority, which a
 * rename or a move keeps, else by their path; the project compared is the one loaded, its changes not
 * saved included, both sides read from YAML as the project files hold them.
 * <ul>
 * <li>projectName: the project</li>
 * <li>ref: the commit compared to, a branch, a tag or an id, HEAD when empty</li>
 * <li>action: revert to give back to an object the value a property had at the commit, of the object id
 * of the tree and of its property, beanData.name for a property of Ionic of an NGX component</li>
 * </ul>
 */
@ServiceDefinition(name = "TreeDiff", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class TreeDiff extends JSonService {
	private static final int MAX_VALUE = 4000;

	/** an object of a side of the comparison */
	static class Bean {
		String key;
		String parentKey;
		String classname;
		String name;
		/** its element in the document of its side */
		Element element;
		Map<String, Element> properties = new LinkedHashMap<>();
		/** the keys of its children, in their order */
		java.util.List<String> children = new java.util.ArrayList<>();
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var projectName = request.getParameter("projectName");
		var project = SourceControl.project(projectName);
		if (project == null) {
			throw new ServiceException("The project " + projectName + " does not exist.");
		}
		var projectDir = project.getDirFile().getCanonicalFile();
		var workingDir = GitUtils.getWorkingDir(projectDir);
		if (workingDir == null) {
			response.put("repository", false);
			return;
		}
		workingDir = workingDir.getCanonicalFile();
		var prefix = projectDir.equals(workingDir) ? ""
				: workingDir.toPath().relativize(projectDir.toPath()).toString().replace(File.separatorChar, '/') + "/";
		var ref = request.getParameter("ref");
		ref = ref == null || ref.isBlank() ? "HEAD" : ref.trim();
		response.put("repository", true);
		response.put("ref", ref);

		var tmp = Files.createTempDirectory("c8o-treediff").toFile();
		try (var git = Git.open(workingDir)) {
			var repository = git.getRepository();
			var headDir = new File(tmp, "head");
			var commit = extract(repository, ref, prefix, headDir, response);
			var before = commit && new File(headDir, "c8oProject.yaml").exists()
					? beans(BeansDefaultValues.unshrinkProject(YamlConverter.readYaml(new File(headDir, "c8oProject.yaml"))))
					: new LinkedHashMap<String, Bean>();
			if ("revert".equals(request.getParameter("action"))) {
				revert(project, before, request.getParameter("id"), request.getParameter("property"));
				response.put("done", true);
				return;
			}
			var after = beans(current(project, new File(tmp, "current")));
			response.put("changes", compare(project, before, after));
		} finally {
			FileUtils.deleteQuietly(tmp);
		}
	}

	/**
	 * Gives back to an object the value a property had at the commit, as the Studio would set it.
	 */
	private static void revert(Project project, Map<String, Bean> before, String id, String name) throws Exception {
		if (id == null || name == null || name.isBlank()) {
			throw new ServiceException("missing id or property parameter");
		}
		var objects = new HashMap<String, DatabaseObject>();
		live(project, null, objects);
		var entry = objects.entrySet().stream().filter((e) -> id.equals(e.getValue().getFullQName())).findFirst()
				.orElseThrow(() -> new ServiceException("The object " + id + " does not exist."));
		var dbo = entry.getValue();
		var old = before.get(entry.getKey());
		if (old == null) {
			throw new ServiceException("The object " + dbo.getName() + " is not in the commit.");
		}
		if (name.startsWith("beanData.")) {
			revertIon(dbo, old, name.substring("beanData.".length()));
			return;
		}
		var property = old.properties.get(name);
		if (property == null) {
			throw new ServiceException("The property " + name + " is not in the commit.");
		}
		for (var pd : CachedIntrospector.getBeanInfo(dbo.getClass()).getPropertyDescriptors()) {
			if (!pd.getName().equals(name) || pd.getWriteMethod() == null || pd.getReadMethod() == null) {
				continue;
			}
			Element valueElement = null;
			for (var child = property.getFirstChild(); child != null && valueElement == null; child = child.getNextSibling()) {
				if (child instanceof Element element) {
					valueElement = element;
				}
			}
			if (valueElement == null) {
				throw new ServiceException("The property " + name + " has no value in the commit.");
			}
			Object value = XMLUtils.readObjectFromXml(valueElement);
			if (property.hasAttribute("ciphered")) {
				value = DatabaseObject.decryptPropertyValue(value);
			}
			var previous = pd.getReadMethod().invoke(dbo);
			pd.getWriteMethod().invoke(dbo, value);
			dbo.hasChanged = true;
			BuilderUtils.dboChanged(dbo, name, previous, pd.getReadMethod().invoke(dbo));
			return;
		}
		throw new ServiceException("The object " + dbo.getName() + " has no property " + name + ".");
	}

	/**
	 * Gives back to an NGX component of Ionic the mode and the value a property of Ionic had at the commit.
	 */
	private static void revertIon(DatabaseObject dbo, Bean old, String name) throws Exception {
		if (!(dbo instanceof UIDynamicElement element) || element.getIonBean() == null
				|| element.getIonBean().getProperty(name) == null) {
			throw new ServiceException("The object " + dbo.getName() + " has no property of Ionic " + name + ".");
		}
		var value = ionValues(value(old.properties.get("beanData")), new HashMap<>()).getOrDefault(name, "");
		var colon = value.indexOf(':');
		var mode = colon < 0 ? "plain" : value.substring(0, colon);
		var text = colon < 0 ? value : value.substring(colon + 1);
		var msst = new MobileSmartSourceType(text);
		if ("script".equals(mode)) {
			msst = new MobileSmartSourceType();
			msst.setMode(Mode.SCRIPT);
			msst.setSmartValue(text);
		} else if ("source".equals(mode)) {
			msst = new MobileSmartSourceType();
			msst.setMode(Mode.SOURCE);
			msst.setSmartValue(text);
		}
		var ionBean = element.getIonBean();
		var previous = ionBean.getPropertyValue(name);
		ionBean.setPropertyValue(name, msst);
		dbo.hasChanged = true;
		BuilderUtils.dboChanged(dbo, name, previous, ionBean.getPropertyValue(name));
	}

	/**
	 * Writes the files of the project at the commit in a folder.
	 * @return whether the commit is found
	 */
	static boolean extract(Repository repository, String ref, String prefix, File dir, JSONObject response) throws Exception {
		var id = repository.resolve(ref + "^{commit}");
		if (id == null) {
			if ("HEAD".equals(ref)) {
				// a repository without commit yet: the whole project is added
				return false;
			}
			throw new ServiceException("The commit " + ref + " does not exist.");
		}
		try (var walk = new RevWalk(repository); var tree = new TreeWalk(repository)) {
			var commit = walk.parseCommit(id);
			var info = new JSONObject();
			info.put("id", commit.getName());
			info.put("shortId", commit.getName().substring(0, 7));
			// not "message", which the Studio shows as a message
			info.put("subject", commit.getShortMessage());
			info.put("author", commit.getAuthorIdent().getName());
			info.put("time", commit.getCommitTime() * 1000L);
			response.put("commit", info);
			// the objects of the project are in its c8oProject.yaml and its _c8oProject folder
			tree.addTree(commit.getTree());
			tree.setRecursive(true);
			tree.setFilter(OrTreeFilter.create(new TreeFilter[] {
				PathFilter.create(prefix + "c8oProject.yaml"),
				PathFilter.create(prefix + "_c8oProject")
			}));
			while (tree.next()) {
				var path = tree.getPathString().substring(prefix.length());
				var file = new File(dir, path);
				file.getParentFile().mkdirs();
				Files.write(file.toPath(), repository.open(tree.getObjectId(0)).getBytes());
			}
		}
		return true;
	}

	/**
	 * @return the objects of the project at a commit, empty when the commit has not the project
	 */
	static Map<String, Bean> beansAt(Repository repository, org.eclipse.jgit.lib.ObjectId commitId, String prefix, File dir) throws Exception {
		return beans(documentAt(repository, commitId, prefix, dir));
	}

	/**
	 * @return the project at a commit, as its files hold it, or an empty project document
	 */
	static Document documentAt(Repository repository, org.eclipse.jgit.lib.ObjectId commitId, String prefix, File dir) throws Exception {
		writeFiles(repository, commitId, prefix, dir);
		return read(dir);
	}

	/**
	 * Writes the files of the objects of the project at a commit in a directory.
	 */
	static void writeFiles(Repository repository, org.eclipse.jgit.lib.ObjectId commitId, String prefix, File dir) throws Exception {
		try (var walk = new RevWalk(repository); var tree = new TreeWalk(repository)) {
			var commit = walk.parseCommit(commitId);
			tree.addTree(commit.getTree());
			tree.setRecursive(true);
			tree.setFilter(OrTreeFilter.create(new TreeFilter[] {
				PathFilter.create(prefix + "c8oProject.yaml"),
				PathFilter.create(prefix + "_c8oProject")
			}));
			while (tree.next()) {
				var file = new File(dir, tree.getPathString().substring(prefix.length()));
				file.getParentFile().mkdirs();
				Files.write(file.toPath(), repository.open(tree.getObjectId(0)).getBytes());
			}
		}
	}

	/**
	 * @return the project of a side of the conflicts of the index, as the conflicts of a stash applied, whose
	 *         commits are not known: the files merged, and the version of the side of the files in conflict,
	 *         1 for the base, 2 for mine, 3 for theirs
	 */
	static Document documentAtStage(Repository repository, int stage, String prefix, File dir) throws Exception {
		writeStageFiles(repository, stage, prefix, dir);
		return read(dir);
	}

	/**
	 * Writes the files of the objects of the project of a side of the conflicts of the index in a directory.
	 */
	static void writeStageFiles(Repository repository, int stage, String prefix, File dir) throws Exception {
		var index = repository.readDirCache();
		for (var i = 0; i < index.getEntryCount(); i++) {
			var entry = index.getEntry(i);
			var path = entry.getPathString();
			if (!path.startsWith(prefix) || (entry.getStage() != 0 && entry.getStage() != stage)) {
				continue;
			}
			var relative = path.substring(prefix.length());
			if (relative.equals("c8oProject.yaml") || relative.startsWith("_c8oProject/")) {
				var file = new File(dir, relative);
				file.getParentFile().mkdirs();
				Files.write(file.toPath(), repository.open(entry.getObjectId()).getBytes());
			}
		}
	}

	private static Document read(File dir) throws Exception {
		var yaml = new File(dir, "c8oProject.yaml");
		if (!yaml.exists()) {
			var empty = XMLUtils.getDefaultDocumentBuilder().newDocument();
			empty.appendChild(empty.createElement("convertigo"));
			return empty;
		}
		return BeansDefaultValues.unshrinkProject(YamlConverter.readYaml(yaml));
	}

	/**
	 * @return the project loaded, written in YAML and read again, as its files would hold it once saved
	 */
	static Document current(Project project, File dir) throws Exception {
		dir.mkdirs();
		var shrink = BeansDefaultValues.shrinkProject(CarUtils.exportProjectDocument(project));
		var yaml = new File(dir, "c8oProject.yaml");
		YamlConverter.writeYaml(shrink, yaml, new File(dir, "_c8oProject"));
		return BeansDefaultValues.unshrinkProject(YamlConverter.readYaml(yaml));
	}

	/**
	 * @return the objects of a project document, by their key
	 */
	static Map<String, Bean> beans(Document document) {
		var beans = new LinkedHashMap<String, Bean>();
		collect(document.getDocumentElement(), null, beans);
		return beans;
	}

	static void collect(Element element, String parentKey, Map<String, Bean> beans) {
		for (var node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (!(node instanceof Element child)) {
				continue;
			}
			if (child.hasAttribute("classname") && child.hasAttribute("priority")) {
				var bean = new Bean();
				bean.classname = child.getAttribute("classname");
				bean.parentKey = parentKey;
				for (var prop = child.getFirstChild(); prop != null; prop = prop.getNextSibling()) {
					if (prop instanceof Element property && "property".equals(property.getTagName())) {
						bean.properties.put(property.getAttribute("name"), property);
					}
				}
				bean.name = value(bean.properties.get("name"));
				bean.key = key(child.getAttribute("priority"), parentKey, bean.classname, bean.name);
				bean.element = child;
				// a copy keeps its first key
				if (beans.putIfAbsent(bean.key, bean) == null && parentKey != null && beans.containsKey(parentKey)) {
					beans.get(parentKey).children.add(bean.key);
				}
				collect(child, bean.key, beans);
			} else {
				collect(child, parentKey, beans);
			}
		}
	}

	/**
	 * An object is known by its priority, else by its path.
	 */
	static String key(String priority, String parentKey, String classname, String name) {
		return priority != null && !priority.isEmpty() && !"0".equals(priority) ? "p:" + priority
				: (parentKey == null ? "" : parentKey) + "/" + classname + ":" + name;
	}

	static void live(DatabaseObject dbo, String parentKey, Map<String, DatabaseObject> objects) throws Exception {
		var key = key(Long.toString(dbo.priority), parentKey, dbo.getClass().getName(), dbo.getName());
		objects.putIfAbsent(key, dbo);
		for (var child : dbo.getDatabaseObjectChildren()) {
			live(child, key, objects);
		}
	}

	private static JSONArray compare(Project project, Map<String, Bean> before, Map<String, Bean> after) throws Exception {
		var objects = new HashMap<String, DatabaseObject>();
		live(project, null, objects);
		var changes = new JSONArray();
		for (var bean : after.values()) {
			var dbo = objects.get(bean.key);
			if (dbo == null) {
				continue;
			}
			var old = before.get(bean.key);
			if (old == null) {
				var change = change(dbo, "added");
				changes.put(change);
				continue;
			}
			var properties = propertyChanges(old, bean);
			var renamed = !old.name.equals(bean.name);
			var moved = old.parentKey != null && !old.parentKey.equals(bean.parentKey);
			if (properties.length() > 0 || renamed || moved) {
				var change = change(dbo, "modified");
				change.put("properties", properties);
				if (renamed) {
					change.put("oldName", old.name);
				}
				if (moved) {
					change.put("moved", true);
				}
				changes.put(change);
			}
		}
		for (var bean : before.values()) {
			// the first object removed of a removed branch is shown, where it was, under its parent still there
			if (after.containsKey(bean.key) || bean.parentKey == null || !after.containsKey(bean.parentKey)) {
				continue;
			}
			var host = objects.get(bean.parentKey);
			if (host == null) {
				continue;
			}
			var change = new JSONObject();
			change.put("status", "removed");
			change.put("name", bean.name);
			change.put("classname", bean.classname);
			change.put("type", typeName(bean.classname));
			var parentId = host.getFullQName();
			var folder = folderType(bean.classname);
			change.put("parentId", folder == FolderType.NONE ? parentId : parentId + ':' + folder.shortName());
			change.put("objectParentId", parentId);
			change.put("id", parentId + "~removed~" + bean.key);
			changes.put(change);
		}
		return changes;
	}

	/**
	 * Adds the properties of Ionic that changed between two data of an NGX component, whose definition is
	 * held in a whole form or in a compact one, "property": "mode:value".
	 */
	/**
	 * @return the properties of an object that differ between two versions of it, but its name, as
	 *         {name, label, old, new}, the properties of Ionic of an NGX component one by one
	 */
	static JSONArray propertyChanges(Bean old, Bean bean) throws Exception {
		var properties = new JSONArray();
		var names = new TreeMap<String, Boolean>();
		old.properties.keySet().forEach((name) -> names.put(name, true));
		bean.properties.keySet().forEach((name) -> names.put(name, true));
		for (var name : names.keySet()) {
			var was = old.properties.get(name);
			var is = bean.properties.get(name);
			if ("name".equals(name) || canonical(was).equals(canonical(is))) {
				continue;
			}
			if ("beanData".equals(name)) {
				// the properties of an NGX component of Ionic, rather than its definition
				ionChanges(value(was), value(is), properties);
				continue;
			}
			var property = new JSONObject();
			property.put("name", name);
			property.put("label", label(bean.classname, name));
			var masked = was != null && was.hasAttribute("ciphered") || is != null && is.hasAttribute("ciphered");
			property.put("old", masked ? "••••••" : display(was));
			property.put("new", masked ? "••••••" : display(is));
			properties.put(property);
		}
		return properties;
	}

	private static void ionChanges(String before, String after, JSONArray properties) throws Exception {
		var labels = new HashMap<String, String>();
		var was = ionValues(before, labels);
		var is = ionValues(after, labels);
		var names = new TreeMap<String, Boolean>();
		was.keySet().forEach((name) -> names.put(name, true));
		is.keySet().forEach((name) -> names.put(name, true));
		for (var name : names.keySet()) {
			var from = was.getOrDefault(name, "");
			var to = is.getOrDefault(name, "");
			if (from.equals(to)) {
				continue;
			}
			var property = new JSONObject();
			property.put("name", "beanData." + name);
			property.put("label", labels.getOrDefault(name, name));
			property.put("old", readable(from));
			property.put("new", readable(to));
			properties.put(property);
		}
	}

	/**
	 * @return the mode and the value of each property of Ionic set, as "mode:value"
	 */
	static Map<String, String> ionValues(String json, Map<String, String> labels) {
		var values = new TreeMap<String, String>();
		if (json == null || json.isBlank()) {
			return values;
		}
		try {
			var data = new JSONObject(json);
			var whole = data.optJSONObject("properties");
			if (whole != null) {
				for (var it = whole.keys(); it.hasNext();) {
					var name = String.valueOf(it.next());
					var property = whole.optJSONObject(name);
					if (property == null) {
						continue;
					}
					if (property.has("label")) {
						labels.putIfAbsent(name, property.optString("label", name));
					}
					var value = property.opt("value");
					if (value == null || Boolean.FALSE.equals(value) || "".equals(value)) {
						continue;
					}
					values.put(name, property.optString("mode", "plain") + ":" + value);
				}
			} else {
				for (var it = data.keys(); it.hasNext();) {
					var name = String.valueOf(it.next());
					if ("ionBean".equals(name)) {
						continue;
					}
					var value = String.valueOf(data.opt(name));
					var colon = value.indexOf(':');
					if (colon < 0 || colon == value.length() - 1) {
						// a mode without value is not set
						continue;
					}
					values.put(name, value);
				}
			}
		} catch (Exception e) {
			values.put("", json);
		}
		return values;
	}

	/**
	 * @return a value of Ionic as the Properties view shows it, its mode before it when it is not a text
	 */
	static String readable(String value) {
		if (value.isEmpty()) {
			return "";
		}
		var colon = value.indexOf(':');
		var mode = colon < 0 ? "plain" : value.substring(0, colon);
		var text = colon < 0 ? value : value.substring(colon + 1);
		return "plain".equals(mode) ? text : mode + ": " + text;
	}

	private static JSONObject change(DatabaseObject dbo, String status) throws Exception {
		var change = new JSONObject();
		change.put("status", status);
		change.put("id", dbo.getFullQName());
		change.put("name", dbo.getName());
		change.put("classname", dbo.getClass().getName());
		change.put("type", typeName(dbo.getClass().getName()));
		var parent = dbo.getParent();
		if (parent != null) {
			change.put("objectParentId", parent.getFullQName());
		}
		return change;
	}

	static FolderType folderType(String classname) {
		try {
			return DatabaseObject.getFolderType(Class.forName(classname));
		} catch (Throwable e) {
			return FolderType.NONE;
		}
	}

	static String typeName(String classname) {
		try {
			return CachedIntrospector.getBeanInfo(Class.forName(classname).asSubclass(DatabaseObject.class)).getBeanDescriptor().getDisplayName();
		} catch (Throwable e) {
			return classname.substring(classname.lastIndexOf('.') + 1);
		}
	}

	static String label(String classname, String name) {
		try {
			for (var pd : CachedIntrospector.getBeanInfo(Class.forName(classname).asSubclass(DatabaseObject.class)).getPropertyDescriptors()) {
				if (pd.getName().equals(name)) {
					return pd.getDisplayName();
				}
			}
		} catch (Throwable e) {
			// the name of the property
		}
		return name;
	}

	/**
	 * @return the value of a property to compare, its format aside
	 */
	static String canonical(Node node) {
		if (node == null) {
			return "";
		}
		var sb = new StringBuilder();
		canonical(node, sb, true);
		return sb.toString();
	}

	private static void canonical(Node node, StringBuilder sb, boolean root) {
		if (node instanceof Element element) {
			if (!root) {
				sb.append('<').append(element.getTagName());
				var attributes = new TreeMap<String, String>();
				var map = element.getAttributes();
				for (int i = 0; i < map.getLength(); i++) {
					attributes.put(map.item(i).getNodeName(), map.item(i).getNodeValue());
				}
				attributes.forEach((name, value) -> sb.append(' ').append(name).append("=\"").append(value).append('"'));
				sb.append('>');
			}
			for (var child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
				canonical(child, sb, false);
			}
			if (!root) {
				sb.append("</").append(element.getTagName()).append('>');
			}
		} else if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
			var text = node.getNodeValue().strip();
			if (!text.isEmpty()) {
				sb.append(text);
			}
		}
	}

	/**
	 * @return the value of a property to read: the value of a simple type, else its text
	 */
	static String display(Element property) {
		var text = value(property);
		return text.length() > MAX_VALUE ? text.substring(0, MAX_VALUE) + "…" : text;
	}

	/**
	 * @return the whole value of a property: the value of a simple type, else its text
	 */
	static String value(Element property) {
		if (property == null) {
			return "";
		}
		for (var child = property.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element value) {
				return value.hasAttribute("value") ? value.getAttribute("value") : value.getTextContent().strip();
			}
		}
		return property.getTextContent().strip();
	}
}
