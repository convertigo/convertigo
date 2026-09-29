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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.GitUtils;

/**
 * The source control of a project in a Git repository, as the Git staging view of the Eclipse Studio: its
 * changed files, their differences, staging, commit, pull and push.
 * <ul>
 * <li>projectName: the project</li>
 * <li>action: status (default), init, diff, stage, unstage, discard, commit, pull, push, fetch, branches,
 * checkout (branch, create) or log; or decorations, without projectName, for the branch and the changed files
 * of each project in a repository</li>
 * <li>paths: the files to stage or unstage, as a JSON array of paths in the repository; path: the file to
 * compare</li>
 * <li>message: the message of the commit</li>
 * </ul>
 */
@ServiceDefinition(name = "SourceControl", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class SourceControl extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		if ("decorations".equals(request.getParameter("action"))) {
			response.put("projects", decorations());
			return;
		}
		var projectName = request.getParameter("projectName");
		var project = projectName == null ? null : Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
		if (project == null) {
			throw new ServiceException("The project " + projectName + " does not exist.");
		}
		var projectDir = project.getDirFile().getCanonicalFile();
		var action = request.getParameter("action") == null ? "status" : request.getParameter("action");
		if ("init".equals(action)) {
			if (GitUtils.getWorkingDir(projectDir) == null) {
				try (var git = Git.init().setDirectory(projectDir).call()) {
					git.add().addFilepattern(".").call();
					git.commit().setMessage("Initial commit").call();
				}
			}
			action = "status";
		}
		var workingDir = GitUtils.getWorkingDir(projectDir);
		if (workingDir == null) {
			response.put("repository", false);
			return;
		}
		workingDir = workingDir.getCanonicalFile();
		// the files of the project, as paths of the repository
		var prefix = projectDir.equals(workingDir) ? ""
				: workingDir.toPath().relativize(projectDir.toPath()).toString().replace(File.separatorChar, '/') + "/";
		try (var git = Git.open(workingDir)) {
			switch (action) {
			case "status" -> status(git, prefix, response);
			case "diff" -> response.put("diff", diff(git, path(request.getParameter("path"), prefix)));
			case "stage", "unstage" -> {
				var paths = new JSONArray(request.getParameter("paths") == null ? "[]" : request.getParameter("paths"));
				var hasHead = git.getRepository().resolve(Constants.HEAD) != null;
				for (int i = 0; i < paths.length(); i++) {
					var path = path(paths.getString(i), prefix);
					var exists = new File(workingDir, path).exists();
					if ("stage".equals(action)) {
						if (exists) {
							git.add().addFilepattern(path).call();
						} else {
							git.rm().addFilepattern(path).setCached(true).call();
						}
					} else if (hasHead) {
						git.reset().addPath(path).call();
					} else {
						git.rm().addFilepattern(path).setCached(true).call();
					}
				}
				status(git, prefix, response);
			}
			case "commit" -> {
				var message = request.getParameter("message");
				if (message == null || message.isBlank()) {
					throw new ServiceException("The commit needs a message.");
				}
				var commit = git.commit().setMessage(message).call();
				response.put("commit", commit.abbreviate(7).name());
				status(git, prefix, response);
			}
			case "pull" -> {
				var result = git.pull().call();
				// the project loads again from its pulled files; a merge stopped on a conflict of its objects keeps
				// the project loaded, the Studio merging them rather than loading the files Git filled with markers
				var merging = merging(git, prefix);
				if (!merging) {
					reload(project.getName());
					response.put("reloaded", true);
				}
				response.put("merging", merging);
				status(git, prefix, response);
				if (!result.isSuccessful() && !merging) {
					var cause = result.getMergeResult() != null ? result.getMergeResult().getMergeStatus()
							: result.getRebaseResult() != null ? result.getRebaseResult().getStatus() : "failed";
					response.put("error", "The pull did not succeed: " + cause + ".");
				}
			}
			case "merge" -> {
				// a branch merged into the current one, as a pull merges the remote one
				var branch = request.getParameter("branch");
				var id = branch == null ? null : git.getRepository().resolve(branch);
				if (id == null) {
					throw new ServiceException("The branch " + branch + " does not exist.");
				}
				var result = git.merge().include(branch, id).setMessage("Merge branch '" + branch + "'").call();
				var merging = merging(git, prefix);
				if (!merging) {
					reload(project.getName());
					response.put("reloaded", true);
				}
				response.put("merging", merging);
				status(git, prefix, response);
				if (!result.getMergeStatus().isSuccessful() && !merging) {
					response.put("error", "The merge did not succeed: " + result.getMergeStatus() + ".");
				}
			}
			case "push" -> {
				// a push the remote refuses, as a push that is not a fast-forward, still ends without exception
				var refused = new ArrayList<String>();
				for (var result : git.push().call()) {
					for (var update : result.getRemoteUpdates()) {
						var updateStatus = update.getStatus();
						if (updateStatus != RemoteRefUpdate.Status.OK && updateStatus != RemoteRefUpdate.Status.UP_TO_DATE) {
							refused.add(Repository.shortenRefName(update.getRemoteName()) + ": " + updateStatus
									+ (update.getMessage() == null ? "" : " (" + update.getMessage() + ")"));
						}
					}
				}
				status(git, prefix, response);
				if (!refused.isEmpty()) {
					response.put("error", "The push was refused: " + String.join(", ", refused) + ".");
				}
			}
			case "fetch" -> {
				git.fetch().call();
				status(git, prefix, response);
			}
			case "branches" -> {
				var local = new JSONArray();
				for (var ref : git.branchList().call()) {
					local.put(Repository.shortenRefName(ref.getName()));
				}
				var remote = new JSONArray();
				for (var ref : git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call()) {
					var name = ref.getName().replaceFirst("^refs/remotes/", "");
					if (!name.endsWith("/HEAD")) {
						remote.put(name);
					}
				}
				response.put("local", local);
				response.put("remoteBranches", remote);
				status(git, prefix, response);
			}
			case "checkout" -> {
				// another branch, or a new one from the current commit, as the switch of the Eclipse Studio
				var branch = request.getParameter("branch");
				if (branch == null || branch.isBlank()) {
					throw new ServiceException("missing branch parameter");
				}
				var repository = git.getRepository();
				var checkout = git.checkout().setName(branch);
				if ("true".equals(request.getParameter("create"))) {
					checkout.setCreateBranch(true);
				} else if (repository.findRef(Constants.R_HEADS + branch) == null
						&& repository.findRef(Constants.R_REMOTES + branch) != null) {
					// a branch of the remote gets a local branch following it
					var local = branch.substring(branch.indexOf('/') + 1);
					checkout = git.checkout().setName(local).setCreateBranch(repository.findRef(Constants.R_HEADS + local) == null)
							.setStartPoint(branch).setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK);
				}
				checkout.call();
				reload(project.getName());
				response.put("reloaded", true);
				status(git, prefix, response);
			}
			case "discard" -> {
				// the files come back as the last commit has them, the new ones are removed
				var paths = new JSONArray(request.getParameter("paths") == null ? "[]" : request.getParameter("paths"));
				var untracked = git.status().call().getUntracked();
				var hasHead = git.getRepository().resolve(Constants.HEAD) != null;
				for (int i = 0; i < paths.length(); i++) {
					var path = path(paths.getString(i), prefix);
					if (untracked.contains(path) || !hasHead) {
						new File(workingDir, path).delete();
					} else {
						git.checkout().setStartPoint(Constants.HEAD).addPath(path).call();
					}
				}
				reload(project.getName());
				response.put("reloaded", true);
				status(git, prefix, response);
			}
			case "log" -> {
				var commits = new JSONArray();
				if (git.getRepository().resolve(Constants.HEAD) != null) {
					var log = git.log().setMaxCount(50);
					if (!prefix.isEmpty()) {
						log.addPath(prefix.substring(0, prefix.length() - 1));
					}
					for (var commit : log.call()) {
						commits.put(new JSONObject()
								.put("id", commit.abbreviate(7).name())
								.put("subject", commit.getShortMessage())
								.put("author", commit.getAuthorIdent().getName())
								.put("time", commit.getCommitTime() * 1000L));
					}
				}
				response.put("commits", commits);
				status(git, prefix, response);
			}
			default -> throw new ServiceException("Unknown action " + action);
			}
		}
	}

	/**
	 * @return the branch, the changed files and the commits to push or pull of each project in a
	 *         repository, as the decorations of the Git projects in the tree of the Eclipse Studio
	 */
	private static JSONArray decorations() throws Exception {
		// a list, since a project named as a key of a message, as "error", would be shown as one
		var projects = new JSONArray();
		var statuses = new java.util.HashMap<File, org.eclipse.jgit.api.Status>();
		for (var name : Engine.theApp.databaseObjectsManager.getAllProjectNamesList()) {
			try {
				var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name, false);
				var projectDir = project == null ? null : project.getDirFile().getCanonicalFile();
				var workingDir = projectDir == null ? null : GitUtils.getWorkingDir(projectDir);
				if (workingDir == null) {
					continue;
				}
				workingDir = workingDir.getCanonicalFile();
				var prefix = projectDir.equals(workingDir) ? ""
						: workingDir.toPath().relativize(projectDir.toPath()).toString().replace(File.separatorChar, '/') + "/";
				try (var git = Git.open(workingDir)) {
					var repository = git.getRepository();
					var status = statuses.get(workingDir);
					if (status == null) {
						status = git.status().call();
						statuses.put(workingDir, status);
					}
					var changes = 0;
					for (var path : status.getUncommittedChanges()) {
						changes += path.startsWith(prefix) ? 1 : 0;
					}
					for (var path : status.getUntracked()) {
						changes += path.startsWith(prefix) ? 1 : 0;
					}
					var decoration = new JSONObject().put("project", name).put("branch", repository.getBranch())
							.put("changes", changes);
					var heads = repository.readMergeHeads();
					if (heads != null && !heads.isEmpty() && status.getConflicting().stream().anyMatch((path) -> path.startsWith(prefix))) {
						// a merge stopped on its conflicts, which the Studio merges object by object
						decoration.put("merging", true);
					}
					var tracking = BranchTrackingStatus.of(repository, repository.getBranch());
					if (tracking != null) {
						decoration.put("ahead", tracking.getAheadCount()).put("behind", tracking.getBehindCount());
					}
					projects.put(decoration);
				}
			} catch (Exception e) {
				Engine.logStudio.debug("(SourceControl) no decoration for " + name, e);
			}
		}
		return projects;
	}

	/**
	 * Loads the project again from its files, which a checkout changed.
	 */
	/**
	 * @return whether a merge stopped on a conflict of the files of the project
	 */
	static boolean merging(Git git, String prefix) throws Exception {
		var heads = git.getRepository().readMergeHeads();
		if (heads == null || heads.isEmpty()) {
			return false;
		}
		for (var path : git.status().call().getConflicting()) {
			if (path.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	static void reload(String projectName) throws Exception {
		var manager = Engine.theApp.databaseObjectsManager;
		manager.clearCache(projectName);
		manager.importProject(Engine.projectYamlFile(projectName), true);
	}

	/**
	 * @return the path of a file of the project in the repository, refusing the others
	 */
	private static String path(String path, String prefix) throws ServiceException {
		if (path == null || !path.startsWith(prefix) || path.contains("..")) {
			throw new ServiceException("The file " + path + " is not a file of the project.");
		}
		return path;
	}

	private static void status(Git git, String prefix, JSONObject response) throws Exception {
		var repository = git.getRepository();
		var status = git.status().call();
		response.put("repository", true);
		response.put("branch", repository.getBranch());
		var head = repository.resolve(Constants.HEAD);
		response.put("head", head == null ? "" : head.abbreviate(7).name());
		response.put("remote", repository.getConfig().getString("remote", "origin", "url"));
		var tracking = BranchTrackingStatus.of(repository, repository.getBranch());
		if (tracking != null) {
			response.put("ahead", tracking.getAheadCount());
			response.put("behind", tracking.getBehindCount());
		}
		response.put("prefix", prefix);
		var staged = new TreeMap<String, String>();
		var unstaged = new TreeMap<String, String>();
		put(staged, status.getAdded(), "added", prefix);
		put(staged, status.getChanged(), "modified", prefix);
		put(staged, status.getRemoved(), "deleted", prefix);
		put(unstaged, status.getModified(), "modified", prefix);
		put(unstaged, status.getMissing(), "deleted", prefix);
		put(unstaged, status.getUntracked(), "untracked", prefix);
		put(unstaged, status.getConflicting(), "conflicting", prefix);
		response.put("staged", files(staged));
		response.put("changes", files(unstaged));
	}

	private static void put(TreeMap<String, String> files, Set<String> paths, String kind, String prefix) {
		for (var path : paths) {
			if (path.startsWith(prefix)) {
				files.put(path, kind);
			}
		}
	}

	private static JSONArray files(TreeMap<String, String> files) throws Exception {
		var array = new JSONArray();
		for (var file : files.entrySet()) {
			// not "status", which the Studio shows as a message
			array.put(new JSONObject().put("path", file.getKey()).put("kind", file.getValue()));
		}
		return array;
	}

	/**
	 * @return the differences of a file between the last commit and the working tree, as a unified diff
	 */
	private static String diff(Git git, String path) throws Exception {
		var repository = git.getRepository();
		var out = new ByteArrayOutputStream();
		try (var formatter = new DiffFormatter(out); var reader = repository.newObjectReader()) {
			formatter.setRepository(repository);
			formatter.setPathFilter(PathFilter.create(path));
			var head = repository.resolve("HEAD^{tree}");
			var oldTree = head == null ? new EmptyTreeIterator() : new CanonicalTreeParser(null, reader, head);
			List<org.eclipse.jgit.diff.DiffEntry> entries = new ArrayList<>(formatter.scan(oldTree, new FileTreeIterator(repository)));
			formatter.format(entries);
		}
		return out.toString(StandardCharsets.UTF_8);
	}
}
