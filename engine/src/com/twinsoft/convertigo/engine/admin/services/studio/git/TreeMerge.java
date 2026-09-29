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
import java.nio.charset.StandardCharsets;
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
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.OrTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
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

	/**
	 * The choices made for the conflicts of a project, until the commit they are against changes, kept in
	 * the directory of Git for a start of the engine during the merge; aligned once the project the Studio
	 * shows is the version mine is, the changes made in the Studio then merged as mine.
	 */
	private static class Session {
		String key;
		boolean aligned;
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
				forget(projectName, repository);
				response.put("merging", false);
				if (operation.stopped()) {
					response.put("operation", operation.toJson(git, prefix));
				}
				return;
			}
			// the choices made hold for a commit merged, replayed, picked or reverted, or for conflicts of the index
			var key = operation.kind + ":" + (operation.theirs != null ? operation.theirs.name() : stages(repository, prefix));
			var session = sessions.compute(projectName, (k, current) -> current != null && key.equals(current.key) ? current : restore(repository, projectName, key));

			if ("abort".equals(action)) {
				// the operation is left, as git merge --abort or git rebase --abort do, the projects load again
				var before = SourceControl.snapshot(git);
				var head = repository.resolve("HEAD^{commit}");
				operation.abort(git);
				forget(projectName, repository);
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
				save(repository, projectName, session);
			}

			var tmp = Files.createTempDirectory("c8o-treemerge").toFile();
			try {
				Document base, ours, theirs;
				SourceControl.VersionWriter oursWriter;
				if (operation.theirs != null) {
					base = operation.base == null ? empty() : TreeDiff.documentAt(repository, operation.base, prefix, new File(tmp, "base"));
					ours = operation.ours == null ? empty() : TreeDiff.documentAt(repository, operation.ours, prefix, new File(tmp, "ours"));
					theirs = TreeDiff.documentAt(repository, operation.theirs, prefix, new File(tmp, "theirs"));
					var oursId = operation.ours;
					oursWriter = oursId == null ? null : (dir) -> TreeDiff.writeFiles(repository, oursId, prefix, dir);
				} else {
					// conflicts whose commits are not known, as those of a stash applied: the sides the index keeps
					base = TreeDiff.documentAtStage(repository, 1, prefix, new File(tmp, "base"));
					ours = TreeDiff.documentAtStage(repository, 2, prefix, new File(tmp, "ours"));
					theirs = TreeDiff.documentAtStage(repository, 3, prefix, new File(tmp, "theirs"));
					oursWriter = (dir) -> TreeDiff.writeStageFiles(repository, 2, prefix, dir);
				}
				// mine is the project the Studio shows, with the changes made in it during the merge: it starts as
				// the version the conflicts are against, loaded again when it is another, as a project not
				// loaded again by a rebase of Git
				var shown = TreeDiff.current(project, new File(tmp, "shown"));
				if (!session.aligned) {
					if (oursWriter != null && !sameProject(shown, ours)) {
						SourceControl.loadVersion(projectName, projectDir, oursWriter);
						project = SourceControl.project(projectName);
						shown = TreeDiff.current(project, new File(tmp, "shown-again"));
						response.put("reloaded", true);
					}
					session.aligned = true;
					save(repository, projectName, session);
				}
				ours = shown;
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
					save(repository, projectName, session);
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
					forget(projectName, repository);
					// a rebase goes on with the next commits, a cherry-pick or a revert commits, once the
					// repository has no other conflict; a merge is committed from the Source control view
					var resolved = GitOperation.of(git);
					GitOperation.Outcome outcome = null;
					if (resolved.conflicting.isEmpty() && (GitOperation.REBASE.equals(resolved.kind)
							|| GitOperation.CHERRY_PICK.equals(resolved.kind) || GitOperation.REVERT.equals(resolved.kind))) {
						var reviewed = new JSONArray();
						outcome = resolved.proceed(git, null, reviewed);
						response.put("reviewed", reviewed);
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
	static void forget(String projectName, Repository repository) {
		sessions.remove(projectName);
		FileUtils.deleteQuietly(sessionFile(repository, projectName));
	}

	private static File sessionFile(Repository repository, String projectName) {
		return new File(repository.getDirectory(), "c8o-studio-merge-" + projectName + ".json");
	}

	/**
	 * @return the choices made for the conflicts of a project, as kept in the directory of Git, or new ones
	 *         when they are for other conflicts
	 */
	private static Session restore(Repository repository, String projectName, String key) {
		var session = new Session();
		session.key = key;
		var file = sessionFile(repository, projectName);
		try {
			if (file.isFile()) {
				var json = new JSONObject(FileUtils.readFileToString(file, StandardCharsets.UTF_8));
				if (key.equals(json.optString("key"))) {
					session.aligned = json.optBoolean("aligned");
					var resolutions = json.optJSONObject("resolutions");
					if (resolutions != null) {
						for (var it = resolutions.keys(); it.hasNext();) {
							var id = (String) it.next();
							var resolution = resolutions.getJSONObject(id);
							session.resolutions.put(id, new Resolution(resolution.getString("choice"), resolution.has("value") ? resolution.getString("value") : null));
						}
					}
				} else {
					FileUtils.deleteQuietly(file);
				}
			}
		} catch (Exception e) {
			Engine.logStudio.debug("(TreeMerge) the choices kept for " + projectName + " are not read", e);
		}
		return session;
	}

	private static void save(Repository repository, String projectName, Session session) {
		try {
			var resolutions = new JSONObject();
			for (var entry : session.resolutions.entrySet()) {
				var resolution = new JSONObject().put("choice", entry.getValue().choice());
				if (entry.getValue().value() != null) {
					resolution.put("value", entry.getValue().value());
				}
				resolutions.put(entry.getKey(), resolution);
			}
			var json = new JSONObject().put("key", session.key).put("aligned", session.aligned).put("resolutions", resolutions);
			FileUtils.writeStringToFile(sessionFile(repository, projectName), json.toString(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			Engine.logStudio.debug("(TreeMerge) the choices for " + projectName + " are not kept", e);
		}
	}

	/**
	 * @return whether two versions of a project have the same objects, with the same properties and children
	 */
	private static boolean sameProject(Document left, Document right) {
		var l = TreeDiff.beans(left);
		var r = TreeDiff.beans(right);
		if (!l.keySet().equals(r.keySet())) {
			return false;
		}
		for (var bean : l.values()) {
			var other = r.get(bean.key);
			if (!bean.children.equals(other.children)) {
				return false;
			}
			var names = new HashSet<String>(bean.properties.keySet());
			names.addAll(other.properties.keySet());
			for (var name : names) {
				if (!TreeDiff.canonical(bean.properties.get(name)).equals(TreeDiff.canonical(other.properties.get(name)))) {
					return false;
				}
			}
		}
		return true;
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
			var choices = new JSONArray().put("mine").put("theirs");
			// a text is merged line by line, its conflicts resolved block by block
			var blocks = blocks(git.getRepository(), path);
			if (blocks != null) {
				conflict.put("blocks", blocks);
				choices.put("blocks");
			}
			conflict.put("choices", choices);
			var resolution = session.resolutions.get("file:" + path);
			if (resolution != null) {
				if ("blocks".equals(resolution.choice())) {
					conflict.put("value", resolution.value());
					if (blocks != null && assemble(blocks, resolution.value()) != null) {
						conflict.put("resolution", "blocks");
					}
				} else {
					conflict.put("resolution", resolution.choice());
				}
			}
			conflicts.put(conflict);
		}
		return conflicts;
	}

	/**
	 * @return the blocks of a text file in conflict, merged line by line as Git merges it: the lines merged,
	 *         {kind: same, text}, and the conflicts, {kind: conflict, index, base, mine, theirs}; null when a
	 *         side removed it or is not a text
	 */
	static JSONArray blocks(Repository repository, String path) throws Exception {
		var index = repository.readDirCache();
		var sides = new byte[4][];
		for (var i = 0; i < index.getEntryCount(); i++) {
			var entry = index.getEntry(i);
			if (entry.getStage() > 0 && entry.getPathString().equals(path)) {
				sides[entry.getStage()] = repository.open(entry.getObjectId()).getBytes();
			}
		}
		if (sides[2] == null || sides[3] == null) {
			return null;
		}
		for (var side : sides) {
			if (side != null && (side.length > 2_000_000 || org.eclipse.jgit.diff.RawText.isBinary(side))) {
				return null;
			}
		}
		var texts = java.util.List.of(new org.eclipse.jgit.diff.RawText(sides[1] == null ? new byte[0] : sides[1]),
				new org.eclipse.jgit.diff.RawText(sides[2]), new org.eclipse.jgit.diff.RawText(sides[3]));
		var result = new org.eclipse.jgit.merge.MergeAlgorithm().merge(org.eclipse.jgit.diff.RawTextComparator.DEFAULT, texts.get(0), texts.get(1), texts.get(2));
		var blocks = new JSONArray();
		JSONObject same = null;
		JSONObject conflict = null;
		var count = 0;
		for (var chunk : result) {
			var text = texts.get(chunk.getSequenceIndex()).getString(chunk.getBegin(), chunk.getEnd(), false);
			switch (chunk.getConflictState()) {
			case NO_CONFLICT -> {
				conflict = null;
				if (same == null) {
					blocks.put(same = new JSONObject().put("kind", "same").put("text", ""));
				}
				same.put("text", same.getString("text") + text);
			}
			case FIRST_CONFLICTING_RANGE -> {
				same = null;
				blocks.put(conflict = new JSONObject().put("kind", "conflict").put("index", count++).put("mine", text).put("base", "").put("theirs", ""));
			}
			case BASE_CONFLICTING_RANGE -> {
				if (conflict != null) {
					conflict.put("base", text);
				}
			}
			case NEXT_CONFLICTING_RANGE -> {
				if (conflict != null) {
					conflict.put("theirs", text);
				}
			}
			}
		}
		return blocks;
	}

	/**
	 * @param choices the choice of each conflict, by its index: mine, theirs, both, mine then theirs, both
	 *        theirs first, or {edit: its lines}
	 * @return the text of a file, its blocks put together as chosen, or null while a conflict has no choice
	 */
	static String assemble(JSONArray blocks, String choices) {
		try {
			var chosen = choices == null ? new JSONObject() : new JSONObject(choices);
			var text = new StringBuilder();
			for (var i = 0; i < blocks.length(); i++) {
				var block = blocks.getJSONObject(i);
				if ("same".equals(block.getString("kind"))) {
					text.append(block.getString("text"));
					continue;
				}
				var key = String.valueOf(block.getInt("index"));
				if (!chosen.has(key)) {
					return null;
				}
				var mine = block.getString("mine");
				var theirs = block.getString("theirs");
				var choice = chosen.get(key);
				if (choice instanceof JSONObject edit) {
					var lines = edit.optString("edit");
					text.append(lines.isEmpty() || lines.endsWith("\n") ? lines : lines + "\n");
				} else {
					switch (String.valueOf(choice)) {
					case "mine" -> text.append(mine);
					case "theirs" -> text.append(theirs);
					case "both" -> text.append(mine).append(mine.isEmpty() || mine.endsWith("\n") ? "" : "\n").append(theirs);
					case "theirsFirst" -> text.append(theirs).append(theirs.isEmpty() || theirs.endsWith("\n") ? "" : "\n").append(mine);
					default -> {
						return null;
					}
					}
				}
			}
			return text.toString();
		} catch (Exception e) {
			return null;
		}
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
		write(git, prefix, projectDir, merged);
		for (var i = 0; i < fileConflicts.length(); i++) {
			var path = fileConflicts.getJSONObject(i).getString("path");
			var resolution = session.resolutions.get("file:" + path);
			if ("blocks".equals(resolution.choice())) {
				// the lines merged and the lines chosen of each block
				var text = assemble(fileConflicts.getJSONObject(i).getJSONArray("blocks"), resolution.value());
				var file = new File(git.getRepository().getWorkTree(), path);
				FileUtils.writeStringToFile(file, text, StandardCharsets.UTF_8);
				git.add().addFilepattern(path).call();
				continue;
			}
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

	/**
	 * Once Git merged, picked or reverted a commit, the projects both sides changed, which Git merged line by
	 * line, are merged again object by object: a merge of their lines can be clean where their objects
	 * conflict, as a step moved on both sides into two blocks, found twice then. The merge of the objects is
	 * taken; when it has conflicts, the operation stops on them as on the conflicts of Git.
	 * @param before HEAD before the operation
	 * @param kind the operation, a merge, a cherry-pick, a revert, or a rebase which replayed a commit
	 * @param picked the commit picked, reverted or replayed
	 * @return the projects merged again, and those stopped on conflicts
	 */
	static JSONArray review(Git git, ObjectId before, String kind, ObjectId picked) throws Exception {
		var reviewed = new JSONArray();
		var repository = git.getRepository();
		var head = repository.resolve("HEAD^{commit}");
		if (before == null || head == null) {
			return reviewed;
		}
		var committed = !head.equals(before);
		ObjectId base, theirs;
		RevCommit commit = null;
		try (var walk = new RevWalk(repository)) {
			if (committed) {
				commit = walk.parseCommit(head);
				if (commit.getParentCount() == 0 || !commit.getParent(0).equals(before)) {
					// fast-forward
					return reviewed;
				}
				if (GitOperation.MERGE.equals(kind)) {
					if (commit.getParentCount() != 2) {
						return reviewed;
					}
					theirs = commit.getParent(1);
					walk.setRevFilter(RevFilter.MERGE_BASE);
					walk.markStart(walk.parseCommit(before));
					walk.markStart(walk.parseCommit(theirs));
					base = walk.next();
				} else {
					var p = picked == null ? null : walk.parseCommit(picked);
					if (p == null || p.getParentCount() == 0) {
						return reviewed;
					}
					var forward = !GitOperation.REVERT.equals(kind);
					base = forward ? p.getParent(0) : p;
					theirs = forward ? p : p.getParent(0);
				}
			} else {
				var operation = GitOperation.of(git);
				if (!kind.equals(operation.kind) || operation.theirs == null) {
					return reviewed;
				}
				base = operation.base;
				theirs = operation.theirs;
			}
		}
		if (base == null) {
			return reviewed;
		}
		var operation = committed ? null : GitOperation.of(git);
		var workingDir = repository.getWorkTree().getCanonicalFile();
		var conflicting = new TreeSet<String>();
		var rewritten = false;
		for (var name : Engine.theApp.databaseObjectsManager.getAllProjectNamesList()) {
			var yaml = Engine.projectYamlFile(name);
			if (yaml == null || !yaml.exists()) {
				continue;
			}
			var projectDir = yaml.getParentFile().getCanonicalFile();
			var dir = GitUtils.getWorkingDir(projectDir);
			if (dir == null || !dir.getCanonicalFile().equals(workingDir)) {
				continue;
			}
			var prefix = projectDir.equals(workingDir) ? ""
					: workingDir.toPath().relativize(projectDir.toPath()).toString().replace(File.separatorChar, '/') + "/";
			if (operation != null && operation.conflicts(prefix)) {
				// merged object by object already
				continue;
			}
			var mine = objectFiles(repository, base, before, prefix);
			var other = objectFiles(repository, base, theirs, prefix);
			if (mine.isEmpty() || other.isEmpty()) {
				continue;
			}
			var tmp = Files.createTempDirectory("c8o-treereview").toFile();
			try {
				var merge = new Merge(TreeDiff.documentAt(repository, base, prefix, new File(tmp, "base")),
						TreeDiff.documentAt(repository, before, prefix, new File(tmp, "ours")),
						TreeDiff.documentAt(repository, theirs, prefix, new File(tmp, "theirs")), new HashMap<>(), new HashMap<>());
				merge.run();
				var result = committed ? TreeDiff.documentAt(repository, head, prefix, new File(tmp, "result")) : TreeDiff.read(projectDir);
				if (merge.conflicts.length() > 0) {
					var both = new TreeSet<>(mine);
					both.retainAll(other);
					conflicting.addAll(both.isEmpty() ? Set.of(prefix + "c8oProject.yaml") : both);
					reviewed.put(new JSONObject().put("project", name).put("conflicts", merge.conflicts.length()));
				} else if (!sameProject(merge.merged, result) || !TreeDiff.duplicates(result).isEmpty()) {
					write(git, prefix, projectDir, merge.merged);
					rewritten = true;
					reviewed.put(new JSONObject().put("project", name).put("merged", true));
				}
			} finally {
				FileUtils.deleteQuietly(tmp);
			}
		}
		if (!conflicting.isEmpty()) {
			if (committed) {
				// the commit is undone: the operation stops, as Git stops on its conflicts; a rebase knows the
				// commit it replays, and commits it again once they are resolved
				git.reset().setMode(ResetCommand.ResetType.SOFT).setRef(before.name()).call();
				if (!GitOperation.REBASE.equals(kind)) {
					if (GitOperation.MERGE.equals(kind)) {
						repository.writeMergeHeads(java.util.List.of(theirs));
					} else if (GitOperation.CHERRY_PICK.equals(kind)) {
						repository.writeCherryPickHead(picked);
					} else {
						repository.writeRevertHead(picked);
					}
					repository.writeMergeCommitMsg(commit.getFullMessage());
				}
			}
			markConflicts(repository, conflicting, base, before, theirs);
		} else if (rewritten && committed) {
			git.commit().setAmend(true).setMessage(commit.getFullMessage()).setAuthor(commit.getAuthorIdent()).call();
		}
		return reviewed;
	}

	/**
	 * Resolves the conflicts of Git that the merge of objects resolves, as those of a commit a rebase replays:
	 * the files of the objects of each project in conflict are merged object by object, when that merge has
	 * no conflict and no other file of the repository is in conflict.
	 * @param reviewed the projects merged, added to
	 * @return whether no conflict is left
	 */
	static boolean resolveClean(Git git, JSONArray reviewed) throws Exception {
		var repository = git.getRepository();
		var operation = GitOperation.of(git);
		if (operation.conflicting.isEmpty()) {
			return true;
		}
		if (operation.theirs == null) {
			return false;
		}
		var workingDir = repository.getWorkTree().getCanonicalFile();
		var covered = new HashSet<String>();
		var merged = new java.util.ArrayList<Object[]>();
		for (var name : Engine.theApp.databaseObjectsManager.getAllProjectNamesList()) {
			var yaml = Engine.projectYamlFile(name);
			if (yaml == null || !yaml.exists()) {
				continue;
			}
			var projectDir = yaml.getParentFile().getCanonicalFile();
			var dir = GitUtils.getWorkingDir(projectDir);
			if (dir == null || !dir.getCanonicalFile().equals(workingDir)) {
				continue;
			}
			var prefix = projectDir.equals(workingDir) ? ""
					: workingDir.toPath().relativize(projectDir.toPath()).toString().replace(File.separatorChar, '/') + "/";
			if (!operation.conflicts(prefix)) {
				continue;
			}
			for (var path : operation.conflicting) {
				if (path.startsWith(prefix)) {
					if (!path.equals(prefix + "c8oProject.yaml") && !path.startsWith(prefix + "_c8oProject/")) {
						// a script or another file of the project, merged by the user
						return false;
					}
					covered.add(path);
				}
			}
			var tmp = Files.createTempDirectory("c8o-treeresolve").toFile();
			try {
				var merge = new Merge(operation.base == null ? empty() : TreeDiff.documentAt(repository, operation.base, prefix, new File(tmp, "base")),
						operation.ours == null ? empty() : TreeDiff.documentAt(repository, operation.ours, prefix, new File(tmp, "ours")),
						TreeDiff.documentAt(repository, operation.theirs, prefix, new File(tmp, "theirs")), new HashMap<>(), new HashMap<>());
				merge.run();
				if (merge.conflicts.length() > 0) {
					return false;
				}
				merged.add(new Object[] { name, prefix, projectDir, merge.merged });
			} finally {
				FileUtils.deleteQuietly(tmp);
			}
		}
		if (!covered.containsAll(operation.conflicting)) {
			return false;
		}
		for (var project : merged) {
			write(git, (String) project[1], (File) project[2], (Document) project[3]);
			reviewed.put(new JSONObject().put("project", project[0]).put("merged", true));
		}
		return true;
	}

	/** @return the files of the objects of a project changed between two commits */
	private static Set<String> objectFiles(Repository repository, ObjectId from, ObjectId to, String prefix) throws Exception {
		var paths = new TreeSet<String>();
		try (var walk = new RevWalk(repository); var tree = new TreeWalk(repository)) {
			tree.addTree(walk.parseCommit(from).getTree());
			tree.addTree(walk.parseCommit(to).getTree());
			tree.setRecursive(true);
			tree.setFilter(AndTreeFilter.create(
					OrTreeFilter.create(PathFilter.create(prefix + "c8oProject.yaml"), PathFilter.create(prefix + "_c8oProject")), TreeFilter.ANY_DIFF));
			while (tree.next()) {
				paths.add(tree.getPathString());
			}
		}
		return paths;
	}

	/** puts files in conflict in the index, with their versions of the base, mine and theirs */
	private static void markConflicts(Repository repository, Set<String> paths, ObjectId base, ObjectId ours, ObjectId theirs) throws Exception {
		var sides = new ObjectId[] { base, ours, theirs };
		var trees = new org.eclipse.jgit.revwalk.RevTree[3];
		try (var walk = new RevWalk(repository)) {
			for (var i = 0; i < 3; i++) {
				trees[i] = walk.parseCommit(sides[i]).getTree();
			}
		}
		var cache = repository.lockDirCache();
		try {
			var builder = cache.builder();
			for (var i = 0; i < cache.getEntryCount(); i++) {
				var entry = cache.getEntry(i);
				if (!paths.contains(entry.getPathString())) {
					builder.add(entry);
				}
			}
			for (var path : paths) {
				for (var stage = 1; stage <= 3; stage++) {
					try (var tree = TreeWalk.forPath(repository, path, trees[stage - 1])) {
						if (tree != null) {
							var entry = new DirCacheEntry(path, stage);
							entry.setFileMode(tree.getFileMode(0));
							entry.setObjectId(tree.getObjectId(0));
							builder.add(entry);
						}
					}
				}
			}
			builder.commit();
		} finally {
			cache.unlock();
		}
	}

	/** writes a version of a project in its files, added to the index */
	private static void write(Git git, String prefix, File projectDir, Document document) throws Exception {
		var shrink = BeansDefaultValues.shrinkProject(document);
		YamlConverter.writeYaml(shrink, new File(projectDir, "c8oProject.yaml"), new File(projectDir, "_c8oProject"));
		for (var path : new String[] { prefix + "c8oProject.yaml", prefix + "_c8oProject" }) {
			git.add().addFilepattern(path).call();
			git.add().addFilepattern(path).setUpdate(true).call();
		}
	}

	/**
	 * @return a text changed on both sides merged line by line, or null when the lines they changed overlap
	 */
	static String mergeLines(String base, String mine, String theirs) {
		var texts = java.util.List.of(raw(base), raw(mine), raw(theirs));
		var result = new org.eclipse.jgit.merge.MergeAlgorithm().merge(org.eclipse.jgit.diff.RawTextComparator.DEFAULT, texts.get(0), texts.get(1), texts.get(2));
		if (result.containsConflicts()) {
			return null;
		}
		var merged = new StringBuilder();
		for (var chunk : result) {
			merged.append(texts.get(chunk.getSequenceIndex()).getString(chunk.getBegin(), chunk.getEnd(), false));
		}
		// the last line ends as the side that changed its end has it
		var end = mine.endsWith("\n") == base.endsWith("\n") ? theirs.endsWith("\n") : mine.endsWith("\n");
		if (!end && merged.length() > 0 && merged.charAt(merged.length() - 1) == '\n') {
			merged.setLength(merged.length() - 1);
		}
		return merged.toString();
	}

	/** the lines of a text, the last one ended as the others */
	private static org.eclipse.jgit.diff.RawText raw(String text) {
		return new org.eclipse.jgit.diff.RawText((text.endsWith("\n") || text.isEmpty() ? text : text + "\n").getBytes(StandardCharsets.UTF_8));
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

		/** the names of the objects renamed on each side, by their key in the base */
		final Map<String, String> oursNames = new HashMap<>(), theirsNames = new HashMap<>();

		Merge(Document base, Document ours, Document theirs, Map<String, Resolution> resolutions, Map<String, DatabaseObject> live) {
			this.base = TreeDiff.beans(base);
			// the objects renamed, as a sequence or a connector, whose key is made of their name, merge with
			// the object of the base under its name: they are given their name at the end
			var oursKeys = new HashMap<String, String>();
			var alignedOurs = align(ours, oursNames, oursKeys);
			this.ours = named(alignedOurs, oursNames);
			this.theirs = named(align(theirs, theirsNames, new HashMap<>()), theirsNames);
			this.merged = (Document) alignedOurs.cloneNode(true);
			this.mb = TreeDiff.beans(merged);
			this.resolutions = resolutions;
			this.live = new HashMap<>();
			live.forEach((key, dbo) -> this.live.putIfAbsent(aligned(oursKeys, key), dbo));
		}

		/**
		 * @return a copy of a side whose objects renamed have the name they have in the base
		 * @param names the names they have on the side, by their key in the base
		 * @param keys their keys in the base, by their keys on the side
		 */
		private Document align(Document side, Map<String, String> names, Map<String, String> keys) {
			var copy = (Document) side.cloneNode(true);
			var beans = TreeDiff.beans(copy);
			for (var bean : new java.util.ArrayList<>(beans.values())) {
				if (bean.parentKey == null) {
					align(beans, bean, bean.key, names, keys);
				}
			}
			return copy;
		}

		/**
		 * Finds the children of an object of a side renamed: one added there and one of the base missing there,
		 * of the same type, under the same object, with children in common, or none on both sides and the
		 * same properties.
		 * @param aligned the key of the object in the base, when it has one
		 */
		private void align(Map<String, Bean> side, Bean s, String aligned, Map<String, String> names, Map<String, String> keys) {
			var b = base.get(aligned);
			var alignedKeys = new HashMap<String, String>();
			var added = new java.util.ArrayList<Bean>();
			for (var key : s.children) {
				var c = side.get(key);
				if (c == null) {
					continue;
				}
				var a = byPriority(key) ? key : aligned + "/" + c.classname + ":" + c.name;
				alignedKeys.put(key, a);
				if (!byPriority(key) && !base.containsKey(a)) {
					added.add(c);
				}
			}
			if (b != null && !added.isEmpty()) {
				var present = new HashSet<>(alignedKeys.values());
				var missing = new java.util.ArrayList<Bean>();
				for (var key : b.children) {
					if (!byPriority(key) && !present.contains(key) && base.containsKey(key)) {
						missing.add(base.get(key));
					}
				}
				while (!added.isEmpty() && !missing.isEmpty()) {
					Bean[] best = null;
					var score = -1;
					for (var c : added) {
						for (var m : missing) {
							var common = c.classname.equals(m.classname) ? common(side, c, m) : -1;
							if (common > score && renamed(side, c, m, common)) {
								best = new Bean[] { c, m };
								score = common;
							}
						}
					}
					if (best == null) {
						break;
					}
					var c = best[0];
					var m = best[1];
					added.remove(c);
					missing.remove(m);
					names.put(m.key, c.name);
					keys.put(c.key, m.key);
					alignedKeys.put(c.key, m.key);
					setName(c, m.name);
				}
			}
			for (var key : s.children) {
				var c = side.get(key);
				if (c != null) {
					align(side, c, alignedKeys.get(key), names, keys);
				}
			}
		}

		/** @return the children an object of a side has in common with an object of the base, as its children */
		private int common(Map<String, Bean> side, Bean c, Bean m) {
			var count = 0;
			for (var key : c.children) {
				var child = side.get(key);
				if (child != null && m.children.contains(byPriority(key) ? key : m.key + "/" + child.classname + ":" + child.name)) {
					count++;
				}
			}
			return count;
		}

		private boolean renamed(Map<String, Bean> side, Bean c, Bean m, int common) {
			if (c.children.isEmpty() && m.children.isEmpty()) {
				var names = new TreeSet<String>();
				names.addAll(c.properties.keySet());
				names.addAll(m.properties.keySet());
				names.remove("name");
				for (var name : names) {
					if (!TreeDiff.canonical(c.properties.get(name)).equals(TreeDiff.canonical(m.properties.get(name)))) {
						return false;
					}
				}
				return true;
			}
			return common > 0 && common * 2 >= Math.min(c.children.size(), m.children.size());
		}

		/** @return whether a key is that of an object by its priority, not by its path */
		private static boolean byPriority(String key) {
			return key.startsWith("p:") && key.indexOf('/') < 0;
		}

		/** @return the key in the base of a key of a side, its objects renamed there given their key in the base */
		private static String aligned(Map<String, String> keys, String key) {
			String best = null;
			for (var k : keys.keySet()) {
				if ((key.equals(k) || key.startsWith(k + "/")) && (best == null || k.length() > best.length())) {
					best = k;
				}
			}
			return best == null ? key : keys.get(best) + key.substring(best.length());
		}

		/** the properties that refer to an object of a project by its names, as project.sequence */
		private static final String[] REFERENCES = { "sourceSequence", "sourceTransaction", "sharedcomponent", "stack" };

		/**
		 * The references of the merged objects to objects of the project, as a sequence a step calls: to an
		 * object the merge renames or moves, they refer to it as it is then; to an object the merge removes,
		 * which a side had, it is a conflict.
		 * @param names the names the merge gives the objects renamed, by their key
		 */
		private void mergeReferences(Map<String, String> names) throws Exception {
			java.util.function.Function<Bean, String> merged = (bean) -> names.getOrDefault(bean.key, bean.name);
			for (var m : new java.util.ArrayList<>(mb.values())) {
				for (var name : REFERENCES) {
					var property = m.properties.get(name);
					if (property != null && text(property) != null) {
						mergeReference(m, name, TreeDiff.label(m.classname, name), text(property), merged, (qname) -> {
							var fixed = (Element) property.cloneNode(true);
							var value = valueElement(fixed);
							if (value.hasAttribute("value")) {
								value.setAttribute("value", qname);
							} else {
								value.setTextContent(qname);
							}
							setProperty(m, name, fixed);
						});
					}
				}
				var data = m.properties.get("beanData");
				if (data != null) {
					var labels = new HashMap<String, String>();
					var requestable = TreeDiff.ionValues(TreeDiff.value(data), labels).getOrDefault("requestable", "");
					if (requestable.startsWith("plain:")) {
						mergeReference(m, "beanData.requestable", labels.getOrDefault("requestable", "requestable"), requestable.substring("plain:".length()), merged,
								(qname) -> {
									try {
										setIon(m, "requestable", "plain:" + qname);
									} catch (Exception e) {
										throw new RuntimeException(e);
									}
								});
					}
				}
			}
		}

		private void mergeReference(Bean m, String property, String label, String qname, java.util.function.Function<Bean, String> merged,
				java.util.function.Consumer<String> fix) throws Exception {
			var root = root(mb);
			if (root == null || qname.isEmpty() || !qname.startsWith(root.name + ".") || resolve(mb, qname, merged) != null) {
				return;
			}
			// broken by a side, in its version, it is as that side left it
			for (var side : java.util.List.of(ours, theirs)) {
				if (side.containsKey(m.key) && qname.equals(reference(side, m.key, property)) && resolve(side, qname, (bean) -> bean.name) == null) {
					return;
				}
			}
			// the object it refers to, on a side that has it
			Bean target = null;
			for (var side : java.util.List.of(ours, theirs, base)) {
				if ((target = resolve(side, qname, (bean) -> bean.name)) != null) {
					break;
				}
			}
			if (target == null) {
				// already missing
				return;
			}
			var there = mb.get(target.key);
			if (there != null) {
				fix.accept(qname(there, merged));
				change("modified", ours.containsKey(m.key) ? ours.get(m.key) : m, new JSONArray().put(label));
				return;
			}
			var resolution = conflict("reference:" + m.key + ":" + property, "property", "Refers to " + target.name + ", which the merge removes",
					ours.containsKey(m.key) ? ours.get(m.key) : m, theirs.get(m.key), property, label, new String[] { reference(base, m.key, property), reference(ours, m.key, property), reference(theirs, m.key, property) }, true);
			last.put("choices", new JSONArray().put("mine").put("edit"));
			if (chose(resolution, "edit") && resolution.value() != null && !resolution.value().isBlank()) {
				fix.accept(resolution.value().strip());
			}
		}

		/** @return the reference of an object of a side, or empty */
		private static String reference(Map<String, Bean> side, String key, String property) throws Exception {
			var bean = side.get(key);
			if (bean == null) {
				return "";
			}
			if (property.startsWith("beanData.")) {
				var ion = TreeDiff.ionValues(TreeDiff.value(bean.properties.get("beanData")), new HashMap<>()).getOrDefault(property.substring("beanData.".length()), "");
				return ion.startsWith("plain:") ? ion.substring("plain:".length()) : ion;
			}
			var text = text(bean.properties.get(property));
			return text == null ? "" : text;
		}

		private static Bean root(Map<String, Bean> side) {
			for (var bean : side.values()) {
				if (bean.parentKey == null) {
					return bean;
				}
			}
			return null;
		}

		/** @return the object of a side that names give, from the project, or null */
		private static Bean resolve(Map<String, Bean> side, String qname, java.util.function.Function<Bean, String> nameOf) {
			var parts = qname.split("\\.");
			var current = root(side);
			if (current == null || !nameOf.apply(current).equals(parts[0])) {
				return null;
			}
			for (var i = 1; i < parts.length && current != null; i++) {
				Bean next = null;
				for (var key : current.children) {
					var child = side.get(key);
					if (child != null && nameOf.apply(child).equals(parts[i])) {
						next = child;
						break;
					}
				}
				current = next;
			}
			return current;
		}

		/** @return the names of an object of the merged project, from the project */
		private String qname(Bean bean, java.util.function.Function<Bean, String> nameOf) {
			var names = new java.util.LinkedList<String>();
			for (var current = bean; current != null; current = current.parentKey == null ? null : mb.get(current.parentKey)) {
				names.addFirst(nameOf.apply(current));
			}
			return String.join(".", names);
		}

		/** @return the objects of a side aligned, those renamed shown with their name */
		private static Map<String, Bean> named(Document aligned, Map<String, String> names) {
			var beans = TreeDiff.beans(aligned);
			names.forEach((key, name) -> {
				var bean = beans.get(key);
				if (bean != null) {
					bean.name = name;
				}
			});
			return beans;
		}

		private static void setName(Bean bean, String name) {
			var value = valueElement(bean.properties.get("name"));
			if (value == null) {
				return;
			}
			if (value.hasAttribute("value")) {
				value.setAttribute("value", name);
			} else {
				value.setTextContent(name);
			}
		}

		/**
		 * Gives the objects renamed their name: renamed by them only, theirs; on both sides differently, or
		 * with the name of another object of the same parent, it is a conflict.
		 */
		private void mergeNames() throws Exception {
			var keys = new TreeSet<String>();
			keys.addAll(oursNames.keySet());
			keys.addAll(theirsNames.keySet());
			var named = new java.util.ArrayList<Object[]>();
			for (var key : keys) {
				var m = mb.get(key);
				var b = base.get(key);
				if (m == null || b == null) {
					continue;
				}
				var mine = oursNames.getOrDefault(key, b.name);
				var other = theirsNames.getOrDefault(key, b.name);
				var name = other.equals(b.name) ? mine : mine.equals(b.name) ? other : null;
				var parent = mb.get(m.parentKey);
				var taken = false;
				if (name != null && parent != null && !name.equals(b.name)) {
					for (var sibling : parent.children) {
						var o = mb.get(sibling);
						taken |= o != null && !sibling.equals(key) && o.classname.equals(m.classname) && name.equals(o.name);
					}
				}
				if (name == null || taken) {
					var resolution = conflict("rename:" + key, "property", name == null ? "Renamed on both sides" : "Renamed to the name of another object there",
							ours.containsKey(key) ? ours.get(key) : m, theirs.get(key), "name", "Name", new String[] { b.name, mine, other }, true);
					name = chose(resolution, "theirs") ? other : chose(resolution, "edit") && resolution.value() != null && !resolution.value().isBlank() ? resolution.value() : mine;
				} else if (!name.equals(mine)) {
					change("modified", ours.containsKey(key) ? ours.get(key) : m, new JSONArray().put("Name"));
				}
				if (!name.equals(b.name)) {
					named.add(new Object[] { m, name });
				}
			}
			var names = new HashMap<String, String>();
			for (var entry : named) {
				names.put(((Bean) entry[0]).key, (String) entry[1]);
			}
			mergeReferences(names);
			for (var entry : named) {
				setName((Bean) entry[0], (String) entry[1]);
			}
			if (!named.isEmpty()) {
				mb = TreeDiff.beans(merged);
			}
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
						} else if (chose(resolution, "merge")) {
							combine(ours, o, theirs, t, combination(resolution));
						}
					}
					mark(handled, theirs, t);
				} else if (o == null) {
					// moved by them counts as a change
					if (!same(base, b, theirs, t) || !Objects.equals(b.parentKey, t.parentKey)) {
						var resolution = conflict("removed:" + t.key, "removed-by-me", "Removed by me, changed by them", b, t, null, null, null, false);
						versions("base", base, b, "theirs", theirs, t);
						if (chose(resolution, "theirs")) {
							restore(t);
						}
					}
					markAbsent(handled, t);
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
				if (same(base, b, ours, o) && Objects.equals(b.parentKey, o.parentKey)) {
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
			mergeOrders();
			mergeNames();
		}

		/**
		 * The order of the children of each object kept on the three sides, by kind, as its steps or its
		 * variables: changed by them only, it is taken; changed on both sides differently, it is a conflict.
		 */
		private void mergeOrders() throws Exception {
			for (var b : base.values()) {
				var o = ours.get(b.key);
				var t = theirs.get(b.key);
				if (o == null || t == null || mb.get(b.key) == null) {
					continue;
				}
				var groups = new java.util.LinkedHashMap<FolderType, java.util.List<String>>();
				for (var key : b.children) {
					var child = base.get(key);
					if (child != null && o.children.contains(key) && t.children.contains(key)) {
						groups.computeIfAbsent(TreeDiff.folderType(child.classname), (k) -> new java.util.ArrayList<>()).add(key);
					}
				}
				for (var group : groups.entrySet()) {
					var common = group.getValue();
					if (common.size() < 2) {
						continue;
					}
					var inOurs = o.children.stream().filter(common::contains).toList();
					var inTheirs = t.children.stream().filter(common::contains).toList();
					if (inTheirs.equals(common) || inTheirs.equals(inOurs)) {
						continue;
					}
					if (inOurs.equals(common)) {
						reorder(b.key, inTheirs);
						change("modified", o, new JSONArray().put("Order"));
						continue;
					}
					var id = "order:" + o.key + ":" + group.getKey().name();
					var resolution = conflict(id, "order", "Reordered on both sides, differently", o, t, "order", "Order",
							new String[] { names(base, common), names(ours, inOurs), names(theirs, inTheirs) }, false);
					if (chose(resolution, "theirs")) {
						reorder(b.key, inTheirs);
					}
				}
			}
		}

		private static String names(Map<String, Bean> side, java.util.List<String> keys) {
			var names = new java.util.ArrayList<String>();
			for (var key : keys) {
				names.add(side.get(key).name);
			}
			return String.join("\n", names);
		}

		/**
		 * Puts children of an object of the merged project in an order, in the places they have.
		 */
		private void reorder(String key, java.util.List<String> order) {
			var parent = mb.get(key);
			var current = parent.children.stream().filter(order::contains).toList();
			if (current.size() != order.size()) {
				return;
			}
			var markers = new java.util.ArrayList<Node>();
			for (var child : current) {
				var element = mb.get(child).element;
				var marker = merged.createComment("order");
				element.getParentNode().insertBefore(marker, element);
				markers.add(marker);
			}
			var elements = new java.util.ArrayList<Element>();
			for (var child : order) {
				var element = mb.get(child).element;
				element.getParentNode().removeChild(element);
				elements.add(element);
			}
			for (var i = 0; i < markers.size(); i++) {
				var marker = markers.get(i);
				marker.getParentNode().insertBefore(elements.get(i), marker);
				marker.getParentNode().removeChild(marker);
			}
			mb = TreeDiff.beans(merged);
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
				// a text of lines, as a script, merges when the lines changed on each side are not the same
				var lines = mergeLines(b.properties.get(name), o.properties.get(name), t.properties.get(name));
				if (lines != null) {
					setProperty(m, name, lines);
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
				var modeOf = so.indexOf(':') < 0 ? "" : so.substring(0, so.indexOf(':') + 1);
				if (!modeOf.isEmpty() && st.startsWith(modeOf) && (sb.isEmpty() || sb.startsWith(modeOf)) && (so.indexOf('\n') >= 0 || st.indexOf('\n') >= 0)) {
					var lines = TreeMerge.mergeLines(sb.isEmpty() ? "" : sb.substring(modeOf.length()), so.substring(modeOf.length()), st.substring(modeOf.length()));
					if (lines != null) {
						setIon(m, name, modeOf + lines);
						taken.put(label);
						continue;
					}
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
			var parent = mb.get(t.parentKey);
			if (parent == null) {
				// added under an object removed by me: left with it
				mark(handled, theirs, t);
				return;
			}
			markNew(handled, t);
			// the objects that were there, in the base or mine, which they moved into the new one, as an action
			// they put in a new If, are not copied with it: they move where they put them, with the changes of
			// both sides, and the objects they added into them are added then
			var element = copy(t, (key) -> base.containsKey(key) || ours.containsKey(key));
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
					} else if (chose(resolution, "merge")) {
						// one object, mine, with what was chosen of theirs
						combine(mb, mb.get(other.key), theirs, t, combination(resolution));
						remove(t.key);
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

		/**
		 * Restores an object removed by me with its children, as they have it, but those I have elsewhere, which
		 * they moved into it: they move there, with the changes of both sides.
		 */
		private void restore(Bean t) throws Exception {
			var parent = mb.get(t.parentKey);
			if (parent == null) {
				return;
			}
			insert(parent.element, copy(t, ours::containsKey), theirs.get(t.parentKey), t.key);
			mb = TreeDiff.beans(merged);
		}

		/**
		 * A copy of an object of theirs for the merged project, without the objects below it that are elsewhere,
		 * which are merged where they are and moved into it then.
		 */
		private Element copy(Bean t, java.util.function.Predicate<String> elsewhere) {
			var element = (Element) merged.importNode(t.element, true);
			var holder = merged.createElement("holder");
			holder.appendChild(element);
			var copy = new java.util.LinkedHashMap<String, Bean>();
			TreeDiff.collect(holder, t.parentKey, copy);
			holder.removeChild(element);
			for (var bean : copy.values()) {
				if (!bean.key.equals(t.key) && elsewhere.test(bean.key)) {
					bean.element.getParentNode().removeChild(bean.element);
				}
			}
			return element;
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

		/**
		 * @return the property of mine with its text merged line by line with theirs, or null when it is not a
		 *         text of lines or their lines changed overlap
		 */
		private Element mergeLines(Element base, Element mine, Element theirs) {
			var vb = text(base);
			var vo = text(mine);
			var vt = text(theirs);
			if (vo == null || vt == null || (base != null && vb == null) || (vo.indexOf('\n') < 0 && vt.indexOf('\n') < 0)) {
				return null;
			}
			var merged = TreeMerge.mergeLines(vb == null ? "" : vb, vo, vt);
			if (merged == null) {
				return null;
			}
			var property = (Element) mine.cloneNode(true);
			var value = valueElement(property);
			if (value.hasAttribute("value")) {
				value.setAttribute("value", merged);
			} else {
				value.setTextContent(merged);
			}
			return property;
		}

		/** @return the text of a property, a value or the text of its element, or null when it is not one */
		private static String text(Element property) {
			var value = valueElement(property);
			if (value == null) {
				return null;
			}
			if (value.hasAttribute("value")) {
				return value.getAttribute("value");
			}
			for (var child = value.getFirstChild(); child != null; child = child.getNextSibling()) {
				if (child instanceof Element) {
					return null;
				}
			}
			return value.getTextContent();
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

		/**
		 * Marks handled an object added by them and the objects added with it, but those that were there
		 * before, which they moved into it, and the objects below them.
		 */
		private void markNew(Set<String> handled, Bean bean) {
			handled.add(bean.key);
			for (var child : bean.children) {
				var c = theirs.get(child);
				if (c != null && !base.containsKey(child) && !ours.containsKey(child)) {
					markNew(handled, c);
				}
			}
		}

		/**
		 * Marks handled an object of theirs removed by me and the objects below it, but those I have elsewhere,
		 * which are merged where they are.
		 */
		private void markAbsent(Set<String> handled, Bean bean) {
			handled.add(bean.key);
			for (var child : bean.children) {
				var c = theirs.get(child);
				if (c != null && !ours.containsKey(child)) {
					markAbsent(handled, c);
				}
			}
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
			if ("same-name".equals(kind) || "added-both".equals(kind)) {
				choices.put("merge");
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
			var versions = new JSONObject().put("left", left).put("right", right).put("root", node(ls, l, rs, r, 0, budget));
			// the two versions whole, as two trees, each object with its match on the other side
			var matches = new HashMap<String, String>();
			match(ls, l, rs, r, matches);
			var reverse = new HashMap<String, String>();
			matches.forEach((k, v) -> reverse.put(v, k));
			var trees = new JSONObject();
			if (l != null) {
				trees.put("left", tree(ls, l, rs, matches, new int[] { 1500 }));
			}
			if (r != null) {
				trees.put("right", tree(rs, r, ls, reverse, new int[] { 1500 }));
			}
			versions.put("trees", trees);
			last.put("versions", versions);
		}

		/**
		 * Matches the objects of two versions, by their key, else by their type and name.
		 */
		private void match(Map<String, Bean> ls, Bean l, Map<String, Bean> rs, Bean r, Map<String, String> matches) {
			if (l == null || r == null) {
				return;
			}
			matches.put(l.key, r.key);
			for (var pair : pairs(ls, l, rs, r).left()) {
				if (pair[1] != null) {
					match(ls, pair[0], rs, pair[1], matches);
				}
			}
		}

		/** the children of two versions of an object: those of the left one with their match, or none, and those of the right one only */
		private record Pairs(java.util.List<Bean[]> left, java.util.List<Bean> right) {
		}

		/**
		 * @return the children of two versions of an object, matched by their key, else by their type and name
		 */
		private Pairs pairs(Map<String, Bean> ls, Bean l, Map<String, Bean> rs, Bean r) {
			var left = new java.util.ArrayList<Bean[]>();
			var right = new java.util.ArrayList<Bean>();
			var taken = new HashSet<String>();
			var rightChildren = r == null ? java.util.List.<String>of() : r.children;
			var leftChildren = l == null ? java.util.List.<String>of() : l.children;
			for (var key : leftChildren) {
				var lc = ls.get(key);
				if (lc == null) {
					continue;
				}
				Bean rc = rightChildren.contains(key) ? rs.get(key) : null;
				if (rc == null) {
					for (var other : rightChildren) {
						var candidate = rs.get(other);
						if (candidate != null && !taken.contains(other) && !leftChildren.contains(other)
								&& candidate.classname.equals(lc.classname) && Objects.equals(candidate.name, lc.name)) {
							rc = candidate;
							break;
						}
					}
				}
				if (rc != null && !taken.add(rc.key)) {
					rc = null;
				}
				left.add(new Bean[] { lc, rc });
			}
			for (var key : rightChildren) {
				var rc = rs.get(key);
				if (rc != null && !taken.contains(key)) {
					right.add(rc);
				}
			}
			return new Pairs(left, right);
		}

		/**
		 * Combines two versions of an object in the merged project, where the left one is: the properties
		 * chosen theirs taken, the children of a single side kept or not, as chosen, those matched combined in
		 * turn.
		 * @param value the choices, as {props: {"key#property": mine|theirs}, children: {"L:key"|"R:key": false}}
		 */
		private void combine(Map<String, Bean> ls, Bean l, Map<String, Bean> rs, Bean r, JSONObject value) throws Exception {
			if (mb.get(l.key) == null) {
				return;
			}
			var props = value.optJSONObject("props");
			var children = value.optJSONObject("children");
			var changes = TreeDiff.propertyChanges(l, r);
			for (var i = 0; i < changes.length(); i++) {
				var name = changes.getJSONObject(i).getString("name");
				if (props == null || !"theirs".equals(props.optString(l.key + "#" + name))) {
					continue;
				}
				if (name.startsWith("beanData.")) {
					var ion = name.substring("beanData.".length());
					var values = TreeDiff.ionValues(TreeDiff.value(r.properties.get("beanData")), new HashMap<>());
					setIon(mb.get(l.key), ion, values.getOrDefault(ion, ""));
				} else {
					setProperty(mb.get(l.key), name, r.properties.get(name));
				}
			}
			if (!Objects.equals(l.name, r.name) && props != null && "theirs".equals(props.optString(l.key + "#name"))) {
				setProperty(mb.get(l.key), "name", r.properties.get("name"));
			}
			var pairs = pairs(ls, l, rs, r);
			for (var pair : pairs.left()) {
				if (pair[1] != null) {
					combine(ls, pair[0], rs, pair[1], value);
				} else if (children != null && children.has("L:" + pair[0].key) && !children.getBoolean("L:" + pair[0].key)) {
					remove(pair[0].key);
				}
			}
			for (var rc : pairs.right()) {
				var keep = children == null || !children.has("R:" + rc.key) || children.getBoolean("R:" + rc.key);
				var parent = mb.get(l.key);
				if (keep && parent != null && !mb.containsKey(rc.key)) {
					insert(parent.element, (Element) merged.importNode(rc.element, true), r, rc.key);
					mb = TreeDiff.beans(merged);
				}
			}
		}

		private static JSONObject combination(Resolution resolution) {
			try {
				return resolution.value() == null || resolution.value().isBlank() ? new JSONObject() : new JSONObject(resolution.value());
			} catch (Exception e) {
				return new JSONObject();
			}
		}

		/**
		 * @return a version of an object and its children whole, each with its icon, its properties, those
		 *         that differ from its match, the key of its match, and whether it is only on this side,
		 *         changed, or has changes below it
		 */
		private JSONObject tree(Map<String, Bean> side, Bean bean, Map<String, Bean> other, Map<String, String> matches, int[] budget) throws Exception {
			budget[0]--;
			var node = new JSONObject();
			node.put("key", bean.key);
			node.put("name", bean.name);
			node.put("type", TreeDiff.typeName(bean.classname));
			var matched = matches.containsKey(bean.key) ? other.get(matches.get(bean.key)) : null;
			if (matched != null) {
				node.put("match", matched.key);
			}
			var dbo = side == ours || side == mb ? live.get(bean.key) : null;
			if (dbo == null && matched != null && (other == ours || other == mb)) {
				dbo = live.get(matched.key);
			}
			if (side == ours || side == mb) {
				var mine = live.get(bean.key);
				if (mine != null) {
					node.put("objectId", mine.getFullQName());
				}
			}
			node.put("icon", "studio.dbo.GetIcon?iconPath=" + icon(bean.classname, dbo));
			// its properties, those of Ionic one by one, as the Properties view shows them
			var differ = matched == null ? new HashSet<String>() : new HashSet<String>();
			if (matched != null) {
				var changed = TreeDiff.propertyChanges(bean, matched);
				for (var i = 0; i < changed.length(); i++) {
					differ.add(changed.getJSONObject(i).getString("name"));
				}
			}
			var properties = new JSONArray();
			for (var entry : bean.properties.entrySet()) {
				var name = entry.getKey();
				if ("name".equals(name)) {
					continue;
				}
				if ("beanData".equals(name)) {
					var labels = new HashMap<String, String>();
					for (var ion : TreeDiff.ionValues(TreeDiff.value(entry.getValue()), labels).entrySet()) {
						var value = TreeDiff.readable(ion.getValue());
						properties.put(new JSONObject().put("name", "beanData." + ion.getKey()).put("label", labels.getOrDefault(ion.getKey(), ion.getKey()))
								.put("value", value.length() > 400 ? value.substring(0, 400) + "…" : value).put("differs", differ.contains("beanData." + ion.getKey())));
					}
					continue;
				}
				var ciphered = entry.getValue().hasAttribute("ciphered");
				var value = ciphered ? "••••••" : TreeDiff.display(entry.getValue());
				properties.put(new JSONObject().put("name", name).put("label", TreeDiff.label(bean.classname, name))
						.put("value", value.length() > 400 ? value.substring(0, 400) + "…" : value).put("differs", differ.contains(name)));
			}
			node.put("properties", properties);
			var children = new JSONArray();
			var inner = false;
			for (var key : bean.children) {
				var child = side.get(key);
				if (child == null || budget[0] <= 0) {
					continue;
				}
				var json = tree(side, child, other, matches, budget);
				inner |= !"same".equals(json.getString("status")) || json.optBoolean("inner");
				children.put(json);
			}
			node.put("children", children);
			node.put("status", matched == null ? "only" : !differ.isEmpty() || !Objects.equals(bean.name, matched.name) ? "changed" : "same");
			// a child of the other side missing here counts as a change below
			if (matched != null) {
				for (var key : matched.children) {
					if (!matches.containsValue(key) && !matches.containsKey(key)) {
						inner = true;
						break;
					}
				}
			}
			node.put("inner", inner);
			return node;
		}

		/**
		 * @return the icon of an object: of the loaded one when there is one, else of its type
		 */
		private String icon(String classname, DatabaseObject dbo) {
			try {
				if (dbo == null) {
					dbo = (DatabaseObject) Class.forName(classname).getConstructor().newInstance();
				}
				return com.twinsoft.convertigo.engine.admin.services.studio.treeview.Get.iconPath(dbo);
			} catch (Throwable e) {
				return "/com/twinsoft/convertigo/beans/core/images/databaseobject_color_32x32.png";
			}
		}

		private JSONObject node(Map<String, Bean> ls, Bean l, Map<String, Bean> rs, Bean r, int depth, int[] budget) throws Exception {
			budget[0]--;
			var bean = l != null ? l : r;
			var node = new JSONObject();
			node.put("name", bean.name);
			node.put("type", TreeDiff.typeName(bean.classname));
			// the keys the choices of a combination name the objects by
			if (l != null) {
				node.put("key", l.key);
			}
			if (r != null) {
				node.put("rightKey", r.key);
			}
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
				var pairs = pairs(ls, l, rs, r);
				for (var pair : pairs.left()) {
					if (budget[0] > 0) {
						children.put(node(ls, pair[0], rs, pair[1], depth + 1, budget));
					}
				}
				for (var rc : pairs.right()) {
					if (budget[0] > 0) {
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
