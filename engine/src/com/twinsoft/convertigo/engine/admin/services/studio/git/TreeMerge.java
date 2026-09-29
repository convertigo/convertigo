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
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.BeansDefaultValues;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.git.TreeDiff.Bean;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.util.GitUtils;
import com.twinsoft.convertigo.engine.util.YamlConverter;

/**
 * The conflicts of a project, of a merge, a rebase, a cherry-pick, a revert or a stash applied, merged object by
 * object rather than line by line: the project at the base, at HEAD, "mine", and at the commit taken,
 * "theirs", the changes of a single side taken, the others given to choose. The project files, which Git
 * filled with its markers, are written once the conflicts are resolved.
 * <ul>
 * <li>projectName: the project</li>
 * <li>action: state, the default; resolve, of a conflict id with a choice, mine, theirs, both or edit, and a
 * value to edit, clear to choose again; resolveAll with a choice; abort, as git merge --abort or git rebase
 * --abort; complete, the merged project written, added to the index and loaded again: a merge is then
 * committed, a cherry-pick or a revert commits, a rebase continues</li>
 * </ul>
 */
@ServiceDefinition(name = "TreeMerge", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class TreeMerge extends JSonService {

	private record Resolution(String choice, String value) {
	}

	/** the choices made for the conflicts of a project, until the commit they are against changes */
	private static class Session {
		String key;
		Map<String, Resolution> resolutions = new ConcurrentHashMap<>();
	}

	private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

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
			response.put("merging", false);
			return;
		}
		workingDir = workingDir.getCanonicalFile();
		var prefix = projectDir.equals(workingDir) ? ""
				: workingDir.toPath().relativize(projectDir.toPath()).toString().replace(File.separatorChar, '/') + "/";
		var action = request.getParameter("action") == null ? "state" : request.getParameter("action");
		try (var git = Git.open(workingDir)) {
			var repository = git.getRepository();
			var operation = GitOperation.of(git);
			if (!operation.conflicts(prefix)) {
				sessions.remove(projectName);
				response.put("merging", false);
				if (operation.stopped()) {
					response.put("operation", operation.toJson(git, prefix));
				}
				return;
			}
			// the choices made hold for a commit merged, replayed, picked or reverted, or for conflicts of the index
			var key = operation.kind + ":" + (operation.theirs != null ? operation.theirs.name() : stages(repository, prefix));
			var session = sessions.compute(projectName, (k, current) -> current != null && key.equals(current.key) ? current : new Session());
			session.key = key;

			if ("abort".equals(action)) {
				// the operation is left, as git merge --abort or git rebase --abort do, the projects load again
				var before = SourceControl.snapshot(git);
				var head = repository.resolve("HEAD^{commit}");
				operation.abort(git);
				sessions.remove(projectName);
				SourceControl.afterOperation(git, prefix, projectName, head, before, response);
				response.put("merging", false);
				response.put("aborted", true);
				response.put("kind", operation.kind);
				return;
			}
			if ("resolve".equals(action)) {
				var id = request.getParameter("id");
				var choice = request.getParameter("choice");
				if (id == null || choice == null) {
					throw new ServiceException("missing id or choice parameter");
				}
				if ("clear".equals(choice)) {
					session.resolutions.remove(id);
				} else {
					session.resolutions.put(id, new Resolution(choice, request.getParameter("value")));
				}
			}

			var tmp = Files.createTempDirectory("c8o-treemerge").toFile();
			try {
				Document base, ours, theirs;
				if (operation.theirs != null) {
					base = operation.base == null ? empty() : TreeDiff.documentAt(repository, operation.base, prefix, new File(tmp, "base"));
					ours = operation.ours == null ? empty() : TreeDiff.documentAt(repository, operation.ours, prefix, new File(tmp, "ours"));
					theirs = TreeDiff.documentAt(repository, operation.theirs, prefix, new File(tmp, "theirs"));
				} else {
					// conflicts whose commits are not known, as those of a stash applied: the sides the index keeps
					base = TreeDiff.documentAtStage(repository, 1, prefix, new File(tmp, "base"));
					ours = TreeDiff.documentAtStage(repository, 2, prefix, new File(tmp, "ours"));
					theirs = TreeDiff.documentAtStage(repository, 3, prefix, new File(tmp, "theirs"));
				}
				var live = new HashMap<String, DatabaseObject>();
				TreeDiff.live(project, null, live);
				var merge = new Merge(base, ours, theirs, session.resolutions, live);
				merge.run();
				var fileConflicts = fileConflicts(git, prefix, session);
				if ("resolveAll".equals(action)) {
					var choice = request.getParameter("choice");
					for (var id : merge.conflictIds()) {
						session.resolutions.putIfAbsent(id, new Resolution(choice, null));
					}
					for (var i = 0; i < fileConflicts.length(); i++) {
						session.resolutions.putIfAbsent(fileConflicts.getJSONObject(i).getString("id"), new Resolution(choice, null));
					}
					merge = new Merge(base, ours, theirs, session.resolutions, live);
					merge.run();
					fileConflicts = fileConflicts(git, prefix, session);
				}
				var unresolved = merge.unresolved() + unresolved(fileConflicts);
				if ("complete".equals(action)) {
					if (unresolved > 0) {
						throw new ServiceException(unresolved + " conflict" + (unresolved > 1 ? "s are" : " is") + " not resolved yet.");
					}
					var before = SourceControl.snapshot(git);
					var head = repository.resolve("HEAD^{commit}");
					var message = operation.message(repository);
					complete(git, prefix, projectDir, merge.merged, fileConflicts, session);
					sessions.remove(projectName);
					// a rebase goes on with the next commits, a cherry-pick or a revert commits, once the
					// repository has no other conflict; a merge is committed from the Source control view
					var resolved = GitOperation.of(git);
					GitOperation.Outcome outcome = null;
					if (resolved.conflicting.isEmpty() && (GitOperation.REBASE.equals(resolved.kind)
							|| GitOperation.CHERRY_PICK.equals(resolved.kind) || GitOperation.REVERT.equals(resolved.kind))) {
						outcome = resolved.proceed(git, null);
					}
					var next = SourceControl.afterOperation(git, prefix, projectName, head, before, response);
					response.put("completed", true);
					response.put("kind", operation.kind);
					response.put("commitMessage", message);
					if (outcome != null) {
						outcome.put(response);
					}
					if (next.stopped()) {
						response.put("operation", next.toJson(git, prefix));
					}
					return;
				}
				response.put("merging", true);
				response.put("kind", operation.kind);
				response.put("ours", operation.oursName);
				response.put("theirs", operation.theirsName);
				response.put("operation", operation.toJson(git, prefix));
				response.put("conflicts", merge.conflicts);
				for (var i = 0; i < fileConflicts.length(); i++) {
					merge.conflicts.put(fileConflicts.get(i));
				}
				response.put("changes", merge.changes);
				response.put("unresolved", unresolved);
			} finally {
				FileUtils.deleteQuietly(tmp);
			}
		}
	}

	/**
	 * Forgets the choices made for the conflicts of a project, as an operation ends.
	 */
	static void forget(String projectName) {
		sessions.remove(projectName);
	}

	/**
	 * @return a key of the conflicts of the index for a project, the versions of its files in conflict
	 */
	private static String stages(Repository repository, String prefix) throws Exception {
		var index = repository.readDirCache();
		var key = new StringBuilder();
		for (var i = 0; i < index.getEntryCount(); i++) {
			var entry = index.getEntry(i);
			if (entry.getStage() != 0 && entry.getPathString().startsWith(prefix)) {
				key.append(entry.getPathString()).append(entry.getStage()).append(entry.getObjectId().name());
			}
		}
		return Integer.toHexString(key.toString().hashCode());
	}

	private static Document empty() throws Exception {
		var document = com.twinsoft.convertigo.engine.util.XMLUtils.getDefaultDocumentBuilder().newDocument();
		document.appendChild(document.createElement("convertigo"));
		return document;
	}

	/**
	 * @return the conflicts of the files of the project that are not its objects, as its scripts
	 */
	private static JSONArray fileConflicts(Git git, String prefix, Session session) throws Exception {
		var conflicts = new JSONArray();
		for (var path : new TreeSet<>(git.status().call().getConflicting())) {
			if (!path.startsWith(prefix)) {
				continue;
			}
			var relative = path.substring(prefix.length());
			if (relative.equals("c8oProject.yaml") || relative.startsWith("_c8oProject/")) {
				continue;
			}
			var conflict = new JSONObject();
			conflict.put("id", "file:" + path);
			conflict.put("kind", "file");
			conflict.put("name", relative);
			conflict.put("path", path);
			conflict.put("choices", new JSONArray().put("mine").put("theirs"));
			var resolution = session.resolutions.get("file:" + path);
			if (resolution != null) {
				conflict.put("resolution", resolution.choice());
			}
			conflicts.put(conflict);
		}
		return conflicts;
	}

	private static int unresolved(JSONArray conflicts) throws Exception {
		var count = 0;
		for (var i = 0; i < conflicts.length(); i++) {
			count += conflicts.getJSONObject(i).has("resolution") ? 0 : 1;
		}
		return count;
	}

	/**
	 * Writes the merged project in its files, takes the side chosen of its other files, and adds them to the
	 * index: the merge is ready to commit.
	 */
	private static void complete(Git git, String prefix, File projectDir, Document merged, JSONArray fileConflicts, Session session) throws Exception {
		var shrink = BeansDefaultValues.shrinkProject(merged);
		YamlConverter.writeYaml(shrink, new File(projectDir, "c8oProject.yaml"), new File(projectDir, "_c8oProject"));
		for (var path : new String[] { prefix + "c8oProject.yaml", prefix + "_c8oProject" }) {
			git.add().addFilepattern(path).call();
			git.add().addFilepattern(path).setUpdate(true).call();
		}
		for (var i = 0; i < fileConflicts.length(); i++) {
			var path = fileConflicts.getJSONObject(i).getString("path");
			var resolution = session.resolutions.get("file:" + path);
			var stage = "theirs".equals(resolution.choice()) ? CheckoutCommand.Stage.THEIRS : CheckoutCommand.Stage.OURS;
			try {
				git.checkout().setStage(stage).addPath(path).call();
				git.add().addFilepattern(path).call();
			} catch (Exception e) {
				// the side chosen has no such file
				git.rm().addFilepattern(path).call();
			}
		}
	}

	/** the merge of the objects of three versions of a project */
	private static class Merge {
		final Map<String, Bean> base, ours, theirs;
		final Document merged;
		Map<String, Bean> mb;
		final Map<String, Resolution> resolutions;
		final Map<String, DatabaseObject> live;
		final JSONArray conflicts = new JSONArray();
		final JSONArray changes = new JSONArray();
		/** the last conflict added, to which its versions are given */
		JSONObject last;

		Merge(Document base, Document ours, Document theirs, Map<String, Resolution> resolutions, Map<String, DatabaseObject> live) {
			this.base = TreeDiff.beans(base);
			this.ours = TreeDiff.beans(ours);
			this.theirs = TreeDiff.beans(theirs);
			this.merged = (Document) ours.cloneNode(true);
			this.mb = TreeDiff.beans(merged);
			this.resolutions = resolutions;
			this.live = live;
		}

		void run() throws Exception {
			var handled = new HashSet<String>();
			for (var t : theirs.values()) {
				if (handled.contains(t.key)) {
					continue;
				}
				var b = base.get(t.key);
				var o = ours.get(t.key);
				if (b == null && o == null) {
					addTheirs(t, handled);
				} else if (b == null) {
					if (!same(ours, o, theirs, t)) {
						var resolution = conflict("added:" + t.key, "added-both", "Added on both sides, differently", o, t, null, null, null, false);
						versions("mine", ours, o, "theirs", theirs, t);
						if (chose(resolution, "theirs")) {
							replace(o.key, t);
						}
					}
					mark(handled, theirs, t);
				} else if (o == null) {
					if (!same(base, b, theirs, t)) {
						var resolution = conflict("removed:" + t.key, "removed-by-me", "Removed by me, changed by them", b, t, null, null, null, false);
						versions("base", base, b, "theirs", theirs, t);
						if (chose(resolution, "theirs")) {
							restore(t);
						}
					}
					mark(handled, theirs, t);
				} else {
					mergeProperties(b, o, t);
					mergeParent(b, o, t);
				}
			}
			for (var o : ours.values()) {
				var b = base.get(o.key);
				if (b == null || theirs.containsKey(o.key)) {
					continue;
				}
				if (o.parentKey != null && base.containsKey(o.parentKey) && !theirs.containsKey(o.parentKey)) {
					// in a branch removed by them, shown by its top
					continue;
				}
				if (same(base, b, ours, o)) {
					change("removed", o, null);
					remove(o.key);
				} else {
					var resolution = conflict("kept:" + o.key, "removed-by-theirs", "Changed by me, removed by them", o, b, null, null, null, false);
					versions("base", base, b, "mine", ours, o);
					if (chose(resolution, "theirs")) {
						remove(o.key);
					}
				}
			}
		}

		Set<String> conflictIds() throws Exception {
			var ids = new TreeSet<String>();
			for (var i = 0; i < conflicts.length(); i++) {
				ids.add(conflicts.getJSONObject(i).getString("id"));
			}
			return ids;
		}

		int unresolved() throws Exception {
			var count = 0;
			for (var i = 0; i < conflicts.length(); i++) {
				count += conflicts.getJSONObject(i).has("resolution") ? 0 : 1;
			}
			return count;
		}

		private boolean chose(Resolution resolution, String choice) {
			return resolution != null && choice.equals(resolution.choice());
		}

		/**
		 * The properties changed by them only are taken, those changed on both sides differently are
		 * conflicts; the properties of Ionic of an NGX component are merged one by one.
		 */
		private void mergeProperties(Bean b, Bean o, Bean t) throws Exception {
			var m = mb.get(o.key);
			if (m == null) {
				return;
			}
			var names = new TreeSet<String>();
			names.addAll(b.properties.keySet());
			names.addAll(o.properties.keySet());
			names.addAll(t.properties.keySet());
			var taken = new JSONArray();
			for (var name : names) {
				var cb = TreeDiff.canonical(b.properties.get(name));
				var co = TreeDiff.canonical(o.properties.get(name));
				var ct = TreeDiff.canonical(t.properties.get(name));
				if (ct.equals(cb) || ct.equals(co)) {
					continue;
				}
				if ("beanData".equals(name)) {
					mergeIon(b, o, t, m, taken);
					continue;
				}
				if (co.equals(cb)) {
					setProperty(m, name, t.properties.get(name));
					taken.put(TreeDiff.label(o.classname, name));
					continue;
				}
				var editable = valueElement(o.properties.get(name)) != null && valueElement(o.properties.get(name)).hasAttribute("value");
				var id = "property:" + o.key + ":" + name;
				var resolution = conflict(id, "property", "Changed on both sides", o, t, name, TreeDiff.label(o.classname, name),
						new String[] { TreeDiff.display(b.properties.get(name)), TreeDiff.display(o.properties.get(name)), TreeDiff.display(t.properties.get(name)) },
						editable);
				if (chose(resolution, "theirs")) {
					setProperty(m, name, t.properties.get(name));
				} else if (chose(resolution, "edit") && editable) {
					var edited = (Element) o.properties.get(name).cloneNode(true);
					valueElement(edited).setAttribute("value", resolution.value() == null ? "" : resolution.value());
					setProperty(m, name, edited);
				}
			}
			if (taken.length() > 0) {
				change("modified", o, taken);
			}
		}

		private void mergeIon(Bean b, Bean o, Bean t, Bean m, JSONArray taken) throws Exception {
			var labels = new HashMap<String, String>();
			var vb = TreeDiff.ionValues(TreeDiff.value(b.properties.get("beanData")), labels);
			var vo = TreeDiff.ionValues(TreeDiff.value(o.properties.get("beanData")), labels);
			var vt = TreeDiff.ionValues(TreeDiff.value(t.properties.get("beanData")), labels);
			var names = new TreeSet<String>();
			names.addAll(vb.keySet());
			names.addAll(vo.keySet());
			names.addAll(vt.keySet());
			for (var name : names) {
				var sb = vb.getOrDefault(name, "");
				var so = vo.getOrDefault(name, "");
				var st = vt.getOrDefault(name, "");
				if (st.equals(sb) || st.equals(so)) {
					continue;
				}
				var label = labels.getOrDefault(name, name);
				if (so.equals(sb)) {
					setIon(m, name, st);
					taken.put(label);
					continue;
				}
				var resolution = conflict("property:" + o.key + ":beanData." + name, "property", "Changed on both sides", o, t,
						"beanData." + name, label,
						new String[] { TreeDiff.readable(sb), TreeDiff.readable(so), TreeDiff.readable(st) }, true);
				if (chose(resolution, "theirs")) {
					setIon(m, name, st);
				} else if (chose(resolution, "edit")) {
					var mode = so.indexOf(':') < 0 ? "plain" : so.substring(0, so.indexOf(':'));
					setIon(m, name, mode + ":" + (resolution.value() == null ? "" : resolution.value()));
				}
			}
		}

		/**
		 * An object moved by them only goes where they put it; moved on both sides, it is a conflict.
		 */
		private void mergeParent(Bean b, Bean o, Bean t) throws Exception {
			if (Objects.equals(t.parentKey, b.parentKey) || Objects.equals(t.parentKey, o.parentKey)) {
				return;
			}
			var theirsParent = mb.get(t.parentKey);
			if (!Objects.equals(o.parentKey, b.parentKey)) {
				var resolution = conflict("move:" + o.key, "move", "Moved on both sides", o, t, null, null, null, false);
				versions("mine", ours, o, "theirs", theirs, t);
				var parents = new JSONObject().put("name", "parent").put("label", "Parent")
						.put("old", ours.containsKey(o.parentKey) ? ours.get(o.parentKey).name : "")
						.put("new", theirs.containsKey(t.parentKey) ? theirs.get(t.parentKey).name : "");
				last.getJSONObject("versions").getJSONObject("root").put("status", "modified").getJSONArray("properties").put(parents);
				if (!chose(resolution, "theirs")) {
					return;
				}
			}
			var m = mb.get(o.key);
			if (theirsParent != null && m != null) {
				m.element.getParentNode().removeChild(m.element);
				insert(theirsParent.element, m.element, theirs.get(t.parentKey), t.key);
				mb = TreeDiff.beans(merged);
				change("moved", o, null);
			}
		}

		private void addTheirs(Bean t, Set<String> handled) throws Exception {
			mark(handled, theirs, t);
			var parent = mb.get(t.parentKey);
			if (parent == null) {
				// added under an object removed by me: left with it
				return;
			}
			var element = (Element) merged.importNode(t.element, true);
			insert(parent.element, element, theirs.get(t.parentKey), t.key);
			mb = TreeDiff.beans(merged);
			change("added", t, null);
			// an object of the name of another object of the same parent
			for (var sibling : mb.get(parent.key).children) {
				var other = mb.get(sibling);
				if (other != null && !other.key.equals(t.key) && other.classname.equals(t.classname) && Objects.equals(other.name, t.name) && ours.containsKey(other.key)) {
					var resolution = conflict("name:" + t.key, "same-name", "Added by them with the name of an object of mine", other, t, null, null, null, false);
					versions("mine", mb, other, "theirs", theirs, t);
					if (chose(resolution, "mine")) {
						remove(t.key);
					} else if (chose(resolution, "theirs")) {
						remove(other.key);
					} else if (chose(resolution, "both")) {
						var added = mb.get(t.key);
						var name = added.properties.get("name");
						var value = valueElement(name);
						if (value != null && value.hasAttribute("value")) {
							value.setAttribute("value", t.name + "_1");
						}
					}
					break;
				}
			}
		}

		/** restores an object removed by me with its children, as they have it */
		private void restore(Bean t) throws Exception {
			var parent = mb.get(t.parentKey);
			if (parent == null) {
				return;
			}
			insert(parent.element, (Element) merged.importNode(t.element, true), theirs.get(t.parentKey), t.key);
			mb = TreeDiff.beans(merged);
		}

		private void replace(String key, Bean t) throws Exception {
			var m = mb.get(key);
			if (m == null) {
				return;
			}
			m.element.getParentNode().replaceChild(merged.importNode(t.element, true), m.element);
			mb = TreeDiff.beans(merged);
		}

		private void remove(String key) throws Exception {
			var m = mb.get(key);
			if (m != null) {
				m.element.getParentNode().removeChild(m.element);
				mb = TreeDiff.beans(merged);
			}
		}

		/**
		 * Inserts an object after the object before it in the order of its side, else before the first object
		 * of the parent.
		 */
		private void insert(Element parent, Element element, Bean sideParent, String key) {
			Node before = null;
			if (sideParent != null) {
				String previous = null;
				for (var child : sideParent.children) {
					if (child.equals(key)) {
						break;
					}
					if (mb.containsKey(child) && mb.get(child).element.getParentNode() == parent) {
						previous = child;
					}
				}
				if (previous != null) {
					before = mb.get(previous).element.getNextSibling();
				}
			}
			if (before == null && (sideParent == null || sideParent.children.indexOf(key) == 0 || sideParent.children.isEmpty())) {
				for (var child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
					if (child instanceof Element e && e.hasAttribute("classname") && e.hasAttribute("priority")) {
						before = child;
						break;
					}
				}
			}
			parent.insertBefore(element, before);
		}

		private void setProperty(Bean m, String name, Element source) {
			var current = m.properties.get(name);
			if (source == null) {
				if (current != null) {
					current.getParentNode().removeChild(current);
					m.properties.remove(name);
				}
				return;
			}
			var imported = (Element) merged.importNode(source, true);
			if (current != null) {
				current.getParentNode().replaceChild(imported, current);
			} else {
				m.element.insertBefore(imported, m.element.getFirstChild());
			}
			m.properties.put(name, imported);
		}

		/**
		 * Sets the mode and the value of a property of Ionic in the data of an NGX component, as "mode:value",
		 * empty to unset it.
		 */
		private void setIon(Bean m, String name, String modeValue) throws Exception {
			var property = m.properties.get("beanData");
			var value = valueElement(property);
			if (value == null) {
				return;
			}
			var json = value.hasAttribute("value") ? value.getAttribute("value") : value.getTextContent();
			var data = new JSONObject(json == null || json.isBlank() ? "{}" : json);
			var colon = modeValue.indexOf(':');
			var mode = colon < 0 ? "plain" : modeValue.substring(0, colon);
			var text = colon < 0 ? modeValue : modeValue.substring(colon + 1);
			var whole = data.optJSONObject("properties");
			if (whole != null) {
				var p = whole.optJSONObject(name);
				if (p == null) {
					whole.put(name, p = new JSONObject());
				}
				if (modeValue.isEmpty()) {
					p.put("value", false);
				} else {
					p.put("mode", mode);
					p.put("value", text);
				}
			} else if (modeValue.isEmpty()) {
				data.remove(name);
			} else {
				data.put(name, mode + ":" + text);
			}
			if (value.hasAttribute("value")) {
				value.setAttribute("value", data.toString());
			} else {
				value.setTextContent(data.toString());
			}
		}

		private static Element valueElement(Element property) {
			if (property == null) {
				return null;
			}
			for (var child = property.getFirstChild(); child != null; child = child.getNextSibling()) {
				if (child instanceof Element element) {
					return element;
				}
			}
			return null;
		}

		/** whether an object and its children are the same on two sides */
		private boolean same(Map<String, Bean> left, Bean l, Map<String, Bean> right, Bean r) {
			if (l == null || r == null) {
				return l == r;
			}
			var names = new TreeSet<String>();
			names.addAll(l.properties.keySet());
			names.addAll(r.properties.keySet());
			for (var name : names) {
				if (!TreeDiff.canonical(l.properties.get(name)).equals(TreeDiff.canonical(r.properties.get(name)))) {
					return false;
				}
			}
			if (!new HashSet<>(l.children).equals(new HashSet<>(r.children))) {
				return false;
			}
			for (var child : l.children) {
				if (!same(left, left.get(child), right, right.get(child))) {
					return false;
				}
			}
			return true;
		}

		private void mark(Set<String> handled, Map<String, Bean> side, Bean bean) {
			handled.add(bean.key);
			for (var child : bean.children) {
				var c = side.get(child);
				if (c != null) {
					mark(handled, side, c);
				}
			}
		}

		/**
		 * Adds a conflict, with the choice made for it when there is one.
		 * @return the choice made, or null
		 */
		private Resolution conflict(String id, String kind, String description, Bean mine, Bean other, String property, String label, String[] values, boolean editable) throws Exception {
			var conflict = new JSONObject();
			conflict.put("id", id);
			conflict.put("kind", kind);
			conflict.put("description", description);
			var bean = mine != null ? mine : other;
			describe(conflict, bean);
			if (property != null) {
				conflict.put("property", property);
				conflict.put("label", label);
			}
			if (values != null) {
				conflict.put("base", values[0]);
				conflict.put("mine", values[1]);
				conflict.put("theirs", values[2]);
			}
			conflict.put("editable", editable);
			var choices = new JSONArray().put("mine").put("theirs");
			if ("same-name".equals(kind)) {
				choices.put("both");
			}
			if (editable) {
				choices.put("edit");
			}
			conflict.put("choices", choices);
			var resolution = resolutions.get(id);
			if (resolution != null) {
				conflict.put("resolution", resolution.choice());
				if (resolution.value() != null) {
					conflict.put("value", resolution.value());
				}
			}
			conflicts.put(conflict);
			last = conflict;
			return resolution;
		}

		/**
		 * Gives the last conflict the two versions of its object, compared: the properties that differ, and its
		 * children matched by their key, else by their type and name, those on a single side and those
		 * that differ, as {left, right, root: {name, type, status same|modified|left|right, objectId,
		 * properties [{name, label, old, new}], children}}.
		 */
		private void versions(String left, Map<String, Bean> ls, Bean l, String right, Map<String, Bean> rs, Bean r) throws Exception {
			var budget = new int[] { 400 };
			last.put("versions", new JSONObject().put("left", left).put("right", right).put("root", node(ls, l, rs, r, 0, budget)));
		}

		private JSONObject node(Map<String, Bean> ls, Bean l, Map<String, Bean> rs, Bean r, int depth, int[] budget) throws Exception {
			budget[0]--;
			var bean = l != null ? l : r;
			var node = new JSONObject();
			node.put("name", bean.name);
			node.put("type", TreeDiff.typeName(bean.classname));
			var properties = new JSONArray();
			String status;
			if (l == null) {
				status = "right";
			} else if (r == null) {
				status = "left";
			} else if (same(ls, l, rs, r) && l.name.equals(r.name)) {
				status = "same";
			} else {
				status = "modified";
				if (!l.name.equals(r.name)) {
					properties.put(new JSONObject().put("name", "name").put("label", "Name").put("old", l.name).put("new", r.name));
				}
				var changed = TreeDiff.propertyChanges(l, r);
				for (var i = 0; i < changed.length(); i++) {
					properties.put(changed.get(i));
				}
			}
			node.put("status", status);
			node.put("properties", properties);
			// the object as the tree shows it, when the loaded project has it
			var dbo = live.get((ls == ours || ls == mb) && l != null ? l.key : (rs == ours || rs == mb) && r != null ? r.key : "");
			if (dbo != null) {
				node.put("objectId", dbo.getFullQName());
			}
			var children = new JSONArray();
			if (!"same".equals(status) && depth < 8) {
				var matched = new HashSet<String>();
				var rightChildren = r == null ? java.util.List.<String>of() : r.children;
				for (var key : l == null ? java.util.List.<String>of() : l.children) {
					var lc = ls.get(key);
					if (lc == null || budget[0] <= 0) {
						continue;
					}
					Bean rc = null;
					if (rightChildren.contains(key) && !matched.contains(key)) {
						rc = rs.get(key);
					} else {
						for (var other : rightChildren) {
							var candidate = rs.get(other);
							if (candidate != null && !matched.contains(other) && !rightChildren.contains(key)
									&& candidate.classname.equals(lc.classname) && Objects.equals(candidate.name, lc.name)) {
								rc = candidate;
								break;
							}
						}
					}
					if (rc != null) {
						matched.add(rc.key);
					}
					children.put(node(ls, lc, rs, rc, depth + 1, budget));
				}
				for (var key : rightChildren) {
					var rc = rs.get(key);
					if (rc != null && !matched.contains(key) && budget[0] > 0) {
						children.put(node(ls, null, rs, rc, depth + 1, budget));
					}
				}
			}
			node.put("children", children);
			return node;
		}

		/** an object merged from their side, as the tree shows it */
		private void change(String status, Bean bean, JSONArray properties) throws Exception {
			var change = new JSONObject();
			change.put("status", status);
			change.put("origin", "theirs");
			describe(change, bean);
			if (properties != null) {
				change.put("properties", properties);
			}
			changes.put(change);
		}

		/** the object and where the tree of the loaded project shows it */
		private void describe(JSONObject json, Bean bean) throws Exception {
			json.put("key", bean.key);
			json.put("name", bean.name);
			json.put("type", TreeDiff.typeName(bean.classname));
			var dbo = live.get(bean.key);
			if (dbo != null) {
				json.put("objectId", dbo.getFullQName());
			}
			// the nearest parent the loaded project has, and the folder of the object in it
			var parentKey = bean.parentKey;
			DatabaseObject parent = null;
			while (parentKey != null && (parent = live.get(parentKey)) == null) {
				var side = ours.containsKey(parentKey) ? ours : theirs.containsKey(parentKey) ? theirs : base;
				var p = side.get(parentKey);
				parentKey = p == null ? null : p.parentKey;
			}
			if (parent != null) {
				var folder = TreeDiff.folderType(bean.classname);
				json.put("objectParentId", parent.getFullQName());
				json.put("parentId", folder == FolderType.NONE ? parent.getFullQName() : parent.getFullQName() + ':' + folder.shortName());
			}
		}
	}
}
