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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Constants;
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
 * <li>action: status (default), init, diff, stage, unstage, commit, pull or push</li>
 * <li>paths: the files to stage or unstage, as a JSON array of paths in the repository; path: the file to
 * compare</li>
 * <li>message: the message of the commit</li>
 * </ul>
 */
@ServiceDefinition(name = "SourceControl", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class SourceControl extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
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
				if (!result.isSuccessful()) {
					throw new ServiceException("The pull did not succeed: " + result.getMergeResult().getMergeStatus() + ".");
				}
				status(git, prefix, response);
			}
			case "push" -> {
				git.push().call();
				status(git, prefix, response);
			}
			default -> throw new ServiceException("Unknown action " + action);
			}
		}
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
