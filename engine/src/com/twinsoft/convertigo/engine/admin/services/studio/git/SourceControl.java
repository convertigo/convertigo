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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jgit.api.CherryPickResult.CherryPickStatus;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevWalk;
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
 * <li>action: status (default), init, diff, stage, unstage, discard, commit, pull (mode merge or rebase),
 * push, fetch, branches, checkout (branch, create), merge (branch), rebase (branch), cherryPick (commit), revert
 * (commit), continue (message), skip or abort of the operation stopped, log (ref), show (commit, path), reset
 * (commit, mode soft, mixed or hard), createBranch (branch, commit, checkout), deleteBranch (branch, force),
 * renameBranch (branch, name), tags, tag (name, commit, annotation), deleteTag (name), stashes, stash (message,
 * untracked), stashApply or stashPop (index), stashDrop (index), remotes, addRemote or setRemoteUrl (name, url),
 * removeRemote (name), deleteRemoteBranch (branch), push (remote, publish, tags, force), fetch (remote),
 * rebaseTodo (upstream) or rebaseInteractive (upstream, steps); or
 * decorations,
 * without projectName, for the branch and the changed files of each project in a repository</li>
 * <li>paths: the files to stage or unstage, as a JSON array of paths in the repository; path: the file to
 * compare</li>
 * <li>message: the message of the commit, amend: true to amend the last one</li>
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
				// a new commit, or the last one amended with the changes staged and its new message
				var message = request.getParameter("message");
				if (message == null || message.isBlank()) {
					throw new ServiceException("The commit needs a message.");
				}
				var commit = git.commit().setMessage(message).setAmend("true".equals(request.getParameter("amend"))).call();
				response.put("commit", commit.abbreviate(7).name());
				status(git, prefix, response);
			}
			case "show" -> {
				// a commit: its message, its author, its parents and the files of the project it changed, or the
				// differences of one of them
				var repository = git.getRepository();
				var id = commit(repository, request.getParameter("commit"));
				try (var walk = new RevWalk(repository)) {
					var commit = walk.parseCommit(id);
					var parent = commit.getParentCount() > 0 ? walk.parseCommit(commit.getParent(0)) : null;
					var path = request.getParameter("path");
					if (path != null) {
						response.put("diff", diff(repository, parent, commit, path(path, prefix)));
					} else {
						var parents = new JSONArray();
						for (var p : commit.getParents()) {
							parents.put(p.abbreviate(7).name());
						}
						response.put("commit", new JSONObject()
								.put("id", commit.abbreviate(7).name())
								.put("fullId", commit.name())
								.put("subject", commit.getShortMessage())
								// not "message", which the Studio shows as a message
								.put("body", commit.getFullMessage())
								.put("author", commit.getAuthorIdent().getName())
								.put("email", commit.getAuthorIdent().getEmailAddress())
								.put("time", commit.getAuthorIdent().getWhenAsInstant().toEpochMilli())
								.put("committer", commit.getCommitterIdent().getName())
								.put("parents", parents));
						var files = new TreeMap<String, String>();
						try (var formatter = new DiffFormatter(org.eclipse.jgit.util.io.NullOutputStream.INSTANCE)) {
							formatter.setRepository(repository);
							formatter.setDetectRenames(true);
							for (var entry : formatter.scan(parent == null ? null : parent.getTree(), commit.getTree())) {
								var changed = entry.getChangeType() == org.eclipse.jgit.diff.DiffEntry.ChangeType.DELETE ? entry.getOldPath() : entry.getNewPath();
								if (changed.startsWith(prefix)) {
									files.put(changed, switch (entry.getChangeType()) {
									case ADD, COPY -> "added";
									case DELETE -> "deleted";
									case RENAME -> "renamed";
									default -> "modified";
									});
								}
							}
						}
						response.put("files", files(files));
					}
				}
				status(git, prefix, response);
			}
			case "reset" -> {
				// the current branch moved to a commit: soft keeps the changes staged, mixed keeps them in the
				// files, hard drops them
				var repository = git.getRepository();
				var id = commit(repository, request.getParameter("commit"));
				var mode = request.getParameter("mode") == null ? "mixed" : request.getParameter("mode");
				var before = snapshot(git);
				var head = repository.resolve("HEAD^{commit}");
				git.reset().setRef(id.name()).setMode(switch (mode) {
				case "soft" -> ResetType.SOFT;
				case "hard" -> ResetType.HARD;
				default -> ResetType.MIXED;
				}).call();
				afterOperation(git, prefix, project.getName(), head, before, response);
				status(git, prefix, response);
			}
			case "createBranch" -> {
				// a new branch at a commit, checked out or not
				var repository = git.getRepository();
				var branch = request.getParameter("branch");
				if (branch == null || branch.isBlank()) {
					throw new ServiceException("missing branch parameter");
				}
				var id = commit(repository, request.getParameter("commit"));
				if ("false".equals(request.getParameter("checkout"))) {
					git.branchCreate().setName(branch).setStartPoint(id.name()).call();
				} else {
					var before = snapshot(git);
					var head = repository.resolve("HEAD^{commit}");
					git.checkout().setCreateBranch(true).setName(branch).setStartPoint(id.name()).call();
					afterOperation(git, prefix, project.getName(), head, before, response);
				}
				status(git, prefix, response);
			}
			case "deleteBranch" -> {
				// a local branch removed, forced when its commits are not merged
				var branch = request.getParameter("branch");
				try {
					git.branchDelete().setBranchNames(Constants.R_HEADS + branch).setForce("true".equals(request.getParameter("force"))).call();
				} catch (org.eclipse.jgit.api.errors.NotMergedException e) {
					response.put("notMerged", true);
				}
				status(git, prefix, response);
			}
			case "renameBranch" -> {
				var branch = request.getParameter("branch");
				var name = request.getParameter("name");
				if (branch == null || name == null || name.isBlank()) {
					throw new ServiceException("missing branch or name parameter");
				}
				git.branchRename().setOldName(branch).setNewName(name.strip()).call();
				status(git, prefix, response);
			}
			case "tags" -> {
				var repository = git.getRepository();
				var tags = new JSONArray();
				try (var walk = new RevWalk(repository)) {
					for (var ref : git.tagList().call()) {
						var tag = new JSONObject().put("name", Repository.shortenRefName(ref.getName()));
						var object = walk.parseAny(ref.getObjectId());
						if (object instanceof org.eclipse.jgit.revwalk.RevTag annotated) {
							tag.put("annotation", annotated.getShortMessage());
						}
						var peeled = walk.peel(object);
						tag.put("commit", peeled.abbreviate(7).name());
						tags.put(tag);
					}
				}
				response.put("tags", tags);
				status(git, prefix, response);
			}
			case "tag" -> {
				// a tag at a commit, annotated when it has a message
				var repository = git.getRepository();
				var name = request.getParameter("name");
				if (name == null || name.isBlank()) {
					throw new ServiceException("missing name parameter");
				}
				var id = commit(repository, request.getParameter("commit"));
				var annotation = request.getParameter("annotation");
				try (var walk = new RevWalk(repository)) {
					var tag = git.tag().setName(name.strip()).setObjectId(walk.parseCommit(id));
					if (annotation != null && !annotation.isBlank()) {
						tag.setAnnotated(true).setMessage(annotation);
					} else {
						tag.setAnnotated(false);
					}
					tag.call();
				}
				status(git, prefix, response);
			}
			case "stashes" -> {
				var stashes = new JSONArray();
				var index = 0;
				for (var stash : git.stashList().call()) {
					stashes.put(new JSONObject()
							.put("index", index++)
							.put("id", stash.abbreviate(7).name())
							// not "message", which the Studio shows as a message
							.put("subject", stash.getShortMessage())
							.put("time", stash.getCommitTime() * 1000L));
				}
				response.put("stashes", stashes);
				status(git, prefix, response);
			}
			case "stash" -> {
				// the changes set aside, the files given back as HEAD has them
				var before = snapshot(git);
				var head = git.getRepository().resolve("HEAD^{commit}");
				var stash = git.stashCreate().setIncludeUntracked("true".equals(request.getParameter("untracked")));
				var message = request.getParameter("message");
				if (message != null && !message.isBlank()) {
					stash.setWorkingDirectoryMessage(message.strip());
				}
				if (stash.call() == null) {
					throw new ServiceException("No change to stash.");
				}
				afterOperation(git, prefix, project.getName(), head, before, response);
				status(git, prefix, response);
			}
			case "stashApply", "stashPop" -> {
				// the changes of a stash applied again, dropped once applied by a pop; conflicts are merged object by
				// object, the stash kept
				var index = Integer.parseInt(request.getParameter("index") == null ? "0" : request.getParameter("index"));
				var ref = "stash@{" + index + "}";
				var stash = git.getRepository().resolve(ref);
				if (stash == null) {
					throw new ServiceException("The stash " + ref + " does not exist.");
				}
				var before = snapshot(git);
				var head = git.getRepository().resolve("HEAD^{commit}");
				var applied = true;
				try {
					git.stashApply().setStashRef(ref).call();
				} catch (org.eclipse.jgit.api.errors.StashApplyFailureException e) {
					applied = false;
					GitOperation.applied(git.getRepository(), ref + ": " + git.getRepository().parseCommit(stash).getShortMessage());
				}
				if (applied && "stashPop".equals(action)) {
					git.stashDrop().setStashRef(index).call();
				}
				var operation = afterOperation(git, prefix, project.getName(), head, before, response);
				status(git, prefix, response);
				if (!applied && !operation.stopped()) {
					response.put("error", "The stash does not apply on the changes of the files: commit or stash them first.");
				}
				response.put("applied", applied);
			}
			case "stashDrop" -> {
				var index = Integer.parseInt(request.getParameter("index") == null ? "0" : request.getParameter("index"));
				git.stashDrop().setStashRef(index).call();
				status(git, prefix, response);
			}
			case "deleteTag" -> {
				git.tagDelete().setTags(request.getParameter("name")).call();
				status(git, prefix, response);
			}
			case "pull" -> {
				// merged, or rebased, as the configuration of the branch says or as asked
				var before = snapshot(git);
				var head = git.getRepository().resolve("HEAD^{commit}");
				var pull = git.pull();
				var mode = request.getParameter("mode");
				if ("rebase".equals(mode) || "merge".equals(mode)) {
					pull.setRebase("rebase".equals(mode));
				}
				var result = pull.call();
				var operation = afterOperation(git, prefix, project.getName(), head, before, response);
				status(git, prefix, response);
				if (!result.isSuccessful() && !operation.stopped()) {
					var error = GitOperation.rebaseError(result.getRebaseResult());
					var cause = result.getMergeResult() != null ? result.getMergeResult().getMergeStatus()
							: result.getRebaseResult() != null ? result.getRebaseResult().getStatus() : "failed";
					response.put("error", error != null ? error : "The pull did not succeed: " + cause + ".");
				}
			}
			case "merge" -> {
				// a branch merged into the current one, as a pull merges the remote one
				var branch = request.getParameter("branch");
				var id = branch == null ? null : git.getRepository().resolve(branch);
				if (id == null) {
					throw new ServiceException("The branch " + branch + " does not exist.");
				}
				var before = snapshot(git);
				var head = git.getRepository().resolve("HEAD^{commit}");
				var result = git.merge().include(branch, id).setMessage("Merge branch '" + branch + "'").call();
				var operation = afterOperation(git, prefix, project.getName(), head, before, response);
				status(git, prefix, response);
				if (!result.getMergeStatus().isSuccessful() && !operation.stopped()) {
					response.put("error", "The merge did not succeed: " + result.getMergeStatus() + ".");
				}
			}
			case "rebase" -> {
				// the commits of the current branch replayed on another one
				var branch = request.getParameter("branch");
				var id = branch == null ? null : git.getRepository().resolve(branch);
				if (id == null) {
					throw new ServiceException("The branch " + branch + " does not exist.");
				}
				var before = snapshot(git);
				var head = git.getRepository().resolve("HEAD^{commit}");
				var result = git.rebase().setUpstream(id).setUpstreamName(branch).call();
				afterOperation(git, prefix, project.getName(), head, before, response);
				status(git, prefix, response);
				GitOperation.Outcome.of(result).put(response);
			}
			case "rebaseTodo" -> {
				// the commits a rebase on a commit replays, to choose what to do of each
				response.put("todo", GitOperation.rebaseTodo(git.getRepository(), commit(git.getRepository(), request.getParameter("upstream"))));
				status(git, prefix, response);
			}
			case "rebaseInteractive" -> {
				// the commits after a commit replayed in the order and with the actions chosen: picked, reworded,
				// stopped to edit, squashed, fixed up or dropped
				var repository = git.getRepository();
				var upstream = commit(repository, request.getParameter("upstream"));
				var steps = new JSONArray(request.getParameter("steps") == null ? "[]" : request.getParameter("steps"));
				var before = snapshot(git);
				var head = repository.resolve("HEAD^{commit}");
				var handler = GitOperation.interactive(repository, upstream, steps);
				org.eclipse.jgit.api.RebaseResult result;
				try {
					result = git.rebase().setUpstream(upstream).runInteractively(handler).call();
				} catch (Exception e) {
					// the rebase is not left half started
					if (repository.getRepositoryState().isRebasing()) {
						git.rebase().setOperation(org.eclipse.jgit.api.RebaseCommand.Operation.ABORT).call();
					}
					throw new ServiceException("The rebase did not start: " + e.getMessage(), e);
				}
				afterOperation(git, prefix, project.getName(), head, before, response);
				status(git, prefix, response);
				GitOperation.Outcome.of(result).put(response);
			}
			case "cherryPick", "revert" -> {
				// a commit applied on the current branch, or its changes undone by a new commit
				var name = request.getParameter("commit");
				var id = name == null ? null : git.getRepository().resolve(name + "^{commit}");
				if (id == null) {
					throw new ServiceException("The commit " + name + " does not exist.");
				}
				var before = snapshot(git);
				var head = git.getRepository().resolve("HEAD^{commit}");
				String error = null;
				if ("cherryPick".equals(action)) {
					var result = git.cherryPick().include(id).call();
					if (result.getStatus() == CherryPickStatus.FAILED) {
						error = "The cherry-pick would overwrite the changes of " + String.join(", ", result.getFailingPaths().keySet()) + ": commit or stash them first.";
					}
				} else {
					var revert = git.revert().include(id);
					revert.call();
					var failing = revert.getFailingResult();
					if (failing != null && failing.getFailingPaths() != null) {
						error = "The revert would overwrite the changes of " + String.join(", ", failing.getFailingPaths().keySet()) + ": commit or stash them first.";
					}
				}
				afterOperation(git, prefix, project.getName(), head, before, response);
				status(git, prefix, response);
				if (error != null) {
					response.put("error", error);
				}
			}
			case "continue", "skip", "abort" -> {
				// the operation stopped goes on, skips the commit it stopped at, or is abandoned
				var operation = GitOperation.of(git);
				if (!operation.stopped()) {
					throw new ServiceException("No operation is in progress.");
				}
				var before = snapshot(git);
				var head = git.getRepository().resolve("HEAD^{commit}");
				GitOperation.Outcome outcome = null;
				if ("abort".equals(action)) {
					operation.abort(git);
				} else if ("skip".equals(action)) {
					outcome = operation.skip(git);
				} else {
					outcome = operation.proceed(git, request.getParameter("message"));
				}
				TreeMerge.forget(project.getName());
				afterOperation(git, prefix, project.getName(), head, before, response);
				status(git, prefix, response);
				if (outcome != null) {
					outcome.put(response);
				}
			}
			case "push" -> {
				// the current branch pushed to its remote, published to a remote when it has none, with the tags,
				// or forced when the remote has not changed since the last fetch
				var repository = git.getRepository();
				var branch = repository.getBranch();
				var full = repository.getFullBranch();
				var remote = request.getParameter("remote");
				var tracked = repository.getConfig().getString("branch", branch, "remote");
				var publish = "true".equals(request.getParameter("publish")) || tracked == null;
				if (remote == null || remote.isBlank()) {
					remote = tracked != null ? tracked : repository.getRemoteNames().contains("origin") ? "origin"
							: repository.getRemoteNames().stream().findFirst().orElse(null);
				}
				if (remote == null) {
					throw new ServiceException("The repository has no remote to push to.");
				}
				var push = git.push().setRemote(remote);
				if (full != null && full.startsWith(Constants.R_HEADS)) {
					var target = publish ? full : repository.getConfig().getString("branch", branch, "merge");
					push.setRefSpecs(new org.eclipse.jgit.transport.RefSpec(full + ":" + (target == null ? full : target)));
					if ("true".equals(request.getParameter("force"))) {
						// force with lease: refused when the remote changed since the branch of the remote was fetched
						var tracking = repository.resolve(Constants.R_REMOTES + remote + "/" + Repository.shortenRefName(target == null ? full : target));
						push.setForce(true).setRefLeaseSpecs(new org.eclipse.jgit.transport.RefLeaseSpec(target == null ? full : target,
								tracking == null ? ObjectId.zeroId().name() : tracking.name()));
					}
				}
				if ("true".equals(request.getParameter("tags"))) {
					push.setPushTags();
				}
				// a push the remote refuses, as a push that is not a fast-forward, still ends without exception
				var refused = new ArrayList<String>();
				for (var result : push.call()) {
					for (var update : result.getRemoteUpdates()) {
						var updateStatus = update.getStatus();
						if (updateStatus != RemoteRefUpdate.Status.OK && updateStatus != RemoteRefUpdate.Status.UP_TO_DATE) {
							refused.add(Repository.shortenRefName(update.getRemoteName()) + ": " + updateStatus
									+ (update.getMessage() == null ? "" : " (" + update.getMessage() + ")"));
						}
					}
				}
				if (refused.isEmpty() && tracked == null && full != null && full.startsWith(Constants.R_HEADS)) {
					// the branch published follows its branch of the remote, when it followed none
					var config = repository.getConfig();
					config.setString("branch", branch, "remote", remote);
					config.setString("branch", branch, "merge", full);
					config.save();
				}
				status(git, prefix, response);
				if (!refused.isEmpty()) {
					response.put("error", "The push was refused: " + String.join(", ", refused) + ".");
				}
			}
			case "fetch" -> {
				// all the remotes, the branches they no longer have removed
				var remote = request.getParameter("remote");
				for (var name : remote == null || remote.isBlank() ? git.getRepository().getRemoteNames() : Set.of(remote)) {
					git.fetch().setRemote(name).setRemoveDeletedRefs(true).call();
				}
				status(git, prefix, response);
			}
			case "remotes" -> {
				var remotes = new JSONArray();
				for (var config : git.remoteList().call()) {
					remotes.put(new JSONObject()
							.put("name", config.getName())
							.put("url", config.getURIs().isEmpty() ? "" : config.getURIs().get(0).toString())
							.put("pushUrl", config.getPushURIs().isEmpty() ? "" : config.getPushURIs().get(0).toString()));
				}
				response.put("remotes", remotes);
				status(git, prefix, response);
			}
			case "addRemote", "setRemoteUrl" -> {
				var name = request.getParameter("name");
				var url = request.getParameter("url");
				if (name == null || name.isBlank() || url == null || url.isBlank()) {
					throw new ServiceException("missing name or url parameter");
				}
				var uri = new org.eclipse.jgit.transport.URIish(url.strip());
				if ("addRemote".equals(action)) {
					git.remoteAdd().setName(name.strip()).setUri(uri).call();
				} else {
					git.remoteSetUrl().setRemoteName(name).setRemoteUri(uri).call();
				}
				status(git, prefix, response);
			}
			case "removeRemote" -> {
				// the remote and its branches, as git remote remove does
				var name = request.getParameter("name");
				git.remoteRemove().setRemoteName(name).call();
				var repository = git.getRepository();
				for (var ref : repository.getRefDatabase().getRefsByPrefix(Constants.R_REMOTES + name + "/")) {
					var update = repository.updateRef(ref.getName());
					update.setForceUpdate(true);
					update.delete();
				}
				status(git, prefix, response);
			}
			case "deleteRemoteBranch" -> {
				// a branch of a remote removed from it, by a push
				var branch = request.getParameter("branch");
				var slash = branch == null ? -1 : branch.indexOf('/');
				if (slash < 1) {
					throw new ServiceException("The branch " + branch + " is not a branch of a remote.");
				}
				var remote = branch.substring(0, slash);
				var refused = new ArrayList<String>();
				for (var result : git.push().setRemote(remote).setRefSpecs(new org.eclipse.jgit.transport.RefSpec(":" + Constants.R_HEADS + branch.substring(slash + 1))).call()) {
					for (var update : result.getRemoteUpdates()) {
						if (update.getStatus() != RemoteRefUpdate.Status.OK && update.getStatus() != RemoteRefUpdate.Status.NON_EXISTING) {
							refused.add(update.getStatus() + (update.getMessage() == null ? "" : " (" + update.getMessage() + ")"));
						}
					}
				}
				if (refused.isEmpty()) {
					git.branchDelete().setBranchNames(Constants.R_REMOTES + branch).setForce(true).call();
				}
				status(git, prefix, response);
				if (!refused.isEmpty()) {
					response.put("error", "The remote refused to delete " + branch + ": " + String.join(", ", refused) + ".");
				}
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
				if (repository.findRef(Constants.R_HEADS + branch) == null && repository.findRef(Constants.R_REMOTES + branch) == null
						&& !"true".equals(request.getParameter("create"))) {
					// a commit, HEAD detached at it
					checkout = git.checkout().setName(commit(repository, branch).name());
				} else if ("true".equals(request.getParameter("create"))) {
					checkout.setCreateBranch(true);
				} else if (repository.findRef(Constants.R_HEADS + branch) == null
						&& repository.findRef(Constants.R_REMOTES + branch) != null) {
					// a branch of the remote gets a local branch following it
					var local = branch.substring(branch.indexOf('/') + 1);
					checkout = git.checkout().setName(local).setCreateBranch(repository.findRef(Constants.R_HEADS + local) == null)
							.setStartPoint(branch).setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK);
				}
				var before = snapshot(git);
				var head = repository.resolve("HEAD^{commit}");
				checkout.call();
				afterOperation(git, prefix, project.getName(), head, before, response);
				status(git, prefix, response);
			}
			case "discard" -> {
				// the files come back as the last commit has them, the new ones are removed
				var paths = new JSONArray(request.getParameter("paths") == null ? "[]" : request.getParameter("paths"));
				var before = snapshot(git);
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
				afterOperation(git, prefix, project.getName(), git.getRepository().resolve("HEAD^{commit}"), before, response);
				status(git, prefix, response);
			}
			case "log" -> {
				// the commits of HEAD, or of another branch, those HEAD has not to cherry-pick
				var commits = new JSONArray();
				var repository = git.getRepository();
				var head = repository.resolve("HEAD^{commit}");
				var ref = request.getParameter("ref");
				var start = ref == null || ref.isBlank() ? head : repository.resolve(ref + "^{commit}");
				if (start != null) {
					var outside = new java.util.HashSet<ObjectId>();
					if (head != null && !start.equals(head)) {
						try (var walk = new RevWalk(repository)) {
							walk.markStart(walk.parseCommit(start));
							walk.markUninteresting(walk.parseCommit(head));
							for (var commit : walk) {
								outside.add(commit.copy());
							}
						}
					}
					// the branches and the tags at each commit
					var refs = new java.util.HashMap<ObjectId, JSONArray>();
					for (var at : repository.getRefDatabase().getRefs()) {
						var name = at.getName();
						if (!name.startsWith(Constants.R_HEADS) && !name.startsWith(Constants.R_REMOTES) && !name.startsWith(Constants.R_TAGS) || name.endsWith("/HEAD")) {
							continue;
						}
						var peeled = repository.getRefDatabase().peel(at);
						var target = peeled.getPeeledObjectId() != null ? peeled.getPeeledObjectId() : at.getObjectId();
						if (target != null) {
							refs.computeIfAbsent(target, (k) -> new JSONArray()).put(name.startsWith(Constants.R_TAGS) ? "tag: " + Repository.shortenRefName(name) : Repository.shortenRefName(name));
						}
					}
					var log = git.log().add(start).setMaxCount(50);
					if (!prefix.isEmpty()) {
						log.addPath(prefix.substring(0, prefix.length() - 1));
					}
					for (var commit : log.call()) {
						commits.put(new JSONObject()
								.put("id", commit.abbreviate(7).name())
								.put("subject", commit.getShortMessage())
								.put("author", commit.getAuthorIdent().getName())
								.put("time", commit.getCommitTime() * 1000L)
								.put("inHead", !outside.contains(commit))
								.put("merge", commit.getParentCount() > 1)
								.put("refs", refs.getOrDefault(commit.getId(), new JSONArray())));
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
					var decoration = new JSONObject().put("project", name).put("branch", branch(repository))
							.put("changes", changes);
					if (status.getConflicting().stream().anyMatch((path) -> path.startsWith(prefix))) {
						// an operation stopped on conflicts, which the Studio merges object by object
						decoration.put("merging", true);
					}
					if (repository.getRepositoryState() != RepositoryState.SAFE) {
						decoration.put("operation", repository.getRepositoryState().name());
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
	 * @return the branch, and the operation stopped, as the prompt of Git shows them: main|REBASE 1/3
	 */
	private static String branch(Repository repository) throws Exception {
		var branch = repository.getBranch();
		var state = repository.getRepositoryState();
		var label = switch (state) {
		case MERGING, MERGING_RESOLVED -> "MERGING";
		case CHERRY_PICKING, CHERRY_PICKING_RESOLVED -> "CHERRY-PICKING";
		case REVERTING, REVERTING_RESOLVED -> "REVERTING";
		case REBASING, REBASING_REBASING, REBASING_MERGE, REBASING_INTERACTIVE, APPLY -> "REBASE";
		case BISECTING -> "BISECTING";
		default -> "";
		};
		if (label.isEmpty()) {
			return branch;
		}
		if ("REBASE".equals(label)) {
			var dir = new File(repository.getDirectory(), "rebase-merge");
			if (!dir.isDirectory()) {
				dir = new File(repository.getDirectory(), "rebase-apply");
			}
			try {
				var headName = new File(dir, "head-name");
				if (headName.isFile()) {
					branch = Repository.shortenRefName(Files.readString(headName.toPath()).strip());
				}
				var done = new File(dir, "done");
				var todo = new File(dir, "git-rebase-todo");
				var step = done.isFile() ? repository.readRebaseTodo(dir.getName() + "/done", false).size() : 0;
				var steps = step + (todo.isFile() ? repository.readRebaseTodo(dir.getName() + "/git-rebase-todo", false).size() : 0);
				if (steps > 0) {
					label += " " + step + "/" + steps;
				}
			} catch (Exception e) {
				// the step is not shown
			}
		}
		return branch + "|" + label;
	}

	/**
	 * @return whether the files of a project have conflicts, of a merge, a rebase, a cherry-pick, a revert
	 *         or a stash applied, which the Studio merges object by object
	 */
	static boolean merging(Git git, String prefix) throws Exception {
		for (var path : git.status().call().getConflicting()) {
			if (path.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Loads the project again from its files.
	 */
	static void reload(String projectName) throws Exception {
		var manager = Engine.theApp.databaseObjectsManager;
		Engine.theApp.schemaManager.clearCache(projectName);
		manager.clearCache(projectName);
		manager.importProject(Engine.projectYamlFile(projectName), true);
	}

	/**
	 * @return the directory of the repository of a project, without opening it
	 */
	private static File workingDir(File dir) {
		while (dir != null && !new File(dir, ".git").isDirectory()) {
			dir = dir.getParentFile();
		}
		return dir;
	}

	/**
	 * @return the projects of the repository, with the state of the files of their objects, to load again
	 *         those an operation changes
	 */
	static Map<String, String> snapshot(Git git) throws Exception {
		var snapshot = new TreeMap<String, String>();
		var workingDir = git.getRepository().getWorkTree().getCanonicalFile();
		for (var name : Engine.theApp.databaseObjectsManager.getAllProjectNamesList()) {
			var yaml = Engine.projectYamlFile(name);
			if (yaml == null || !yaml.exists()) {
				continue;
			}
			var projectDir = yaml.getParentFile().getCanonicalFile();
			var dir = workingDir(projectDir);
			if (dir == null || !dir.getCanonicalFile().equals(workingDir)) {
				continue;
			}
			var files = new ArrayList<File>();
			files.add(yaml);
			var objects = new File(projectDir, "_c8oProject");
			if (objects.isDirectory()) {
				files.addAll(FileUtils.listFiles(objects, null, true));
			}
			files.sort(null);
			var state = new StringBuilder();
			for (var file : files) {
				state.append(file.getPath()).append(':').append(file.lastModified()).append(':').append(file.length()).append('\n');
			}
			snapshot.put(name, state.toString());
		}
		return snapshot;
	}

	/**
	 * Once an operation changed the files of the repository, its projects whose files changed load again,
	 * but those in conflict: the Studio merges their objects rather than loading the files Git filled with its
	 * markers, and loads the version of HEAD when the operation moved it, as a rebase.
	 * @param headBefore HEAD before the operation
	 * @param before the snapshot of the projects before the operation
	 * @return the operation the repository stopped at, or none
	 */
	static GitOperation afterOperation(Git git, String prefix, String projectName, ObjectId headBefore, Map<String, String> before,
			JSONObject response) throws Exception {
		var operation = GitOperation.of(git);
		var moved = !Objects.equals(headBefore, git.getRepository().resolve("HEAD^{commit}"));
		var workingDir = git.getRepository().getWorkTree().getCanonicalFile();
		var reloaded = new JSONArray();
		var self = false;
		for (var entry : snapshot(git).entrySet()) {
			var name = entry.getKey();
			var projectDir = Engine.projectYamlFile(name).getParentFile().getCanonicalFile();
			var projectPrefix = projectDir.equals(workingDir) ? ""
					: workingDir.toPath().relativize(projectDir.toPath()).toString().replace(File.separatorChar, '/') + "/";
			if (operation.conflicts(projectPrefix)) {
				if (!moved) {
					continue;
				}
				loadCommitted(git, name, projectDir, projectPrefix);
			} else if (entry.getValue().equals(before.get(name))) {
				continue;
			} else {
				reload(name);
			}
			reloaded.put(name);
			self |= name.equals(projectName);
		}
		response.put("reloaded", self);
		response.put("reloadedProjects", reloaded);
		response.put("merging", operation.conflicts(prefix));
		return operation;
	}

	/**
	 * Loads a project in conflict as HEAD has it, the files Git filled with its markers given back once
	 * loaded: the Studio merges its objects on the version its conflicts are against.
	 */
	static void loadCommitted(Git git, String projectName, File projectDir, String prefix) throws Exception {
		var repository = git.getRepository();
		var head = repository.resolve("HEAD^{commit}");
		if (head == null) {
			return;
		}
		var yaml = new File(projectDir, "c8oProject.yaml");
		var objects = new File(projectDir, "_c8oProject");
		var saved = new LinkedHashMap<File, byte[]>();
		if (yaml.isFile()) {
			saved.put(yaml, Files.readAllBytes(yaml.toPath()));
		}
		if (objects.isDirectory()) {
			for (var file : FileUtils.listFiles(objects, null, true)) {
				saved.put(file, Files.readAllBytes(file.toPath()));
			}
		}
		try {
			FileUtils.deleteQuietly(yaml);
			FileUtils.deleteQuietly(objects);
			TreeDiff.writeFiles(repository, head, prefix, projectDir);
			if (yaml.isFile()) {
				reload(projectName);
			}
		} finally {
			FileUtils.deleteQuietly(yaml);
			FileUtils.deleteQuietly(objects);
			for (var file : saved.entrySet()) {
				file.getKey().getParentFile().mkdirs();
				Files.write(file.getKey().toPath(), file.getValue());
			}
		}
	}

	/**
	 * @return a commit, by its id, a branch or a tag
	 */
	private static ObjectId commit(Repository repository, String name) throws Exception {
		var id = name == null || name.isBlank() ? null : repository.resolve(name + "^{commit}");
		if (id == null) {
			throw new ServiceException("The commit " + name + " does not exist.");
		}
		return id;
	}

	/**
	 * @return the differences of a file in a commit, from its first parent, as a unified diff
	 */
	private static String diff(Repository repository, org.eclipse.jgit.revwalk.RevCommit parent, org.eclipse.jgit.revwalk.RevCommit commit, String path) throws Exception {
		var out = new ByteArrayOutputStream();
		try (var formatter = new DiffFormatter(out)) {
			formatter.setRepository(repository);
			formatter.setPathFilter(PathFilter.create(path));
			formatter.format(parent == null ? null : parent.getTree(), commit.getTree());
		}
		return out.toString(StandardCharsets.UTF_8);
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
		var operation = GitOperation.of(git);
		// the branch rebased rather than the commit HEAD is at
		response.put("branch", GitOperation.REBASE.equals(operation.kind) && !operation.branch.isEmpty() ? operation.branch : repository.getBranch());
		var head = repository.resolve(Constants.HEAD);
		response.put("head", head == null ? "" : head.abbreviate(7).name());
		var full = repository.getFullBranch();
		response.put("detached", head != null && full != null && !full.startsWith(Constants.R_HEADS) && !GitOperation.REBASE.equals(operation.kind));
		// the remote the branch follows, else origin, else the first one
		var branchRemote = repository.getConfig().getString("branch", repository.getBranch(), "remote");
		var remotes = repository.getRemoteNames();
		var remote = branchRemote != null ? branchRemote : remotes.contains("origin") ? "origin" : remotes.stream().findFirst().orElse(null);
		response.put("remote", remote == null ? null : repository.getConfig().getString("remote", remote, "url"));
		response.put("remoteName", remote);
		response.put("remoteCount", remotes.size());
		var merge = repository.getConfig().getString("branch", repository.getBranch(), "merge");
		if (branchRemote != null && merge != null) {
			response.put("upstream", branchRemote + "/" + Repository.shortenRefName(merge));
		}
		var tracking = BranchTrackingStatus.of(repository, repository.getBranch());
		if (tracking != null) {
			response.put("ahead", tracking.getAheadCount());
			response.put("behind", tracking.getBehindCount());
		}
		response.put("prefix", prefix);
		if (operation.stopped()) {
			response.put("operation", operation.toJson(git, prefix));
		}
		var rebase = repository.getConfig().getString("branch", repository.getBranch(), "rebase");
		if (rebase == null) {
			rebase = repository.getConfig().getString("pull", null, "rebase");
		}
		response.put("pullRebase", rebase != null && !"false".equals(rebase));
		var stashes = 0;
		if (head != null) {
			for (var stash : git.stashList().call()) {
				stashes += stash != null ? 1 : 0;
			}
		}
		response.put("stashCount", stashes);
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
