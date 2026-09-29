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
import java.util.TreeSet;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.lib.CommitConfig;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;

import com.twinsoft.convertigo.engine.admin.services.ServiceException;

/**
 * An operation of a repository stopped before its end, as the Git views of the Eclipse Studio show it: a
 * merge, a rebase, a cherry-pick or a revert, stopped on conflicts, or to edit a commit, or the conflicts of a
 * stash applied. It gives the three sides the Studio merges the objects of, and continues, skips or aborts.
 */
class GitOperation {
	static final String MERGE = "merge";
	static final String REBASE = "rebase";
	static final String CHERRY_PICK = "cherry-pick";
	static final String REVERT = "revert";
	/** conflicts no operation stopped on, as those of a stash applied */
	static final String CONFLICTS = "conflicts";

	/**
	 * How a step of an operation ended: the status of a rebase, as STOPPED on conflicts, EDIT or
	 * NOTHING_TO_COMMIT, OK when a commit ended it, EMPTY when nothing was left to commit, and why it failed.
	 */
	record Outcome(String status, String error) {
		static Outcome of(RebaseResult result) {
			return new Outcome(result.getStatus().name(), rebaseError(result));
		}

		void put(JSONObject response) throws Exception {
			response.put("result", status);
			if (error != null) {
				response.put("error", error);
			}
		}
	}

	/** the stash applied with conflicts, by repository, which the index does not name */
	private static final java.util.Map<String, String> appliedStashes = new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * Names the side of the conflicts of a stash applied.
	 */
	static void applied(Repository repository, String stash) {
		appliedStashes.put(repository.getDirectory().getAbsolutePath(), stash);
	}

	/**
	 * The messages of an interactive rebase, by repository, in the order JGit asks them: the new message of
	 * each commit reworded, null to keep it, and of each squash, null to keep the messages combined. They
	 * outlive the request that started it, for the rebase that goes on after a stop.
	 */
	private static final java.util.Map<String, java.util.LinkedList<String>> rebaseMessages = new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * @return the handler of an interactive rebase: its steps, in the order and with the actions given, as
	 *         {id, action: pick, reword, edit, squash, fixup or drop, message}, every commit of the rebase given
	 */
	static RebaseCommand.InteractiveHandler interactive(Repository repository, ObjectId upstream, JSONArray steps) throws Exception {
		// every commit of the rebase has an action, before JGit starts it
		var todo = rebaseTodo(repository, upstream);
		var ids = new java.util.HashSet<String>();
		for (var i = 0; i < todo.length(); i++) {
			ids.add(todo.getJSONObject(i).getString("id"));
		}
		var given = new java.util.HashSet<String>();
		for (var i = 0; i < steps.length(); i++) {
			var step = steps.getJSONObject(i);
			var id = step.optString("id");
			if (!ids.contains(id) || !given.add(id)) {
				throw new ServiceException("The commit " + id + " is not a commit of the rebase, or is given twice.");
			}
			if (!step.optString("action", "pick").matches("pick|reword|edit|squash|fixup|drop")) {
				throw new ServiceException("Unknown action " + step.optString("action") + " for the commit " + id + ".");
			}
		}
		if (!given.equals(ids)) {
			throw new ServiceException("Each commit of the rebase needs an action.");
		}
		var kept = new java.util.ArrayList<JSONObject>();
		for (var i = 0; i < steps.length(); i++) {
			var step = steps.getJSONObject(i);
			if (!"drop".equals(step.optString("action"))) {
				kept.add(step);
			}
		}
		if (!kept.isEmpty() && kept.get(0).optString("action").matches("squash|fixup")) {
			throw new ServiceException("The first commit kept cannot be squashed: it has no commit before it.");
		}
		var messages = new java.util.LinkedList<String>();
		var squashed = false;
		for (var i = 0; i < kept.size(); i++) {
			var action = kept.get(i).optString("action");
			if ("reword".equals(action)) {
				var message = kept.get(i).optString("message", "");
				messages.add(message.isBlank() ? null : message);
			}
			squashed |= "squash".equals(action);
			var next = i + 1 < kept.size() ? kept.get(i + 1).optString("action") : "";
			if (action.matches("squash|fixup") && !next.matches("squash|fixup")) {
				if (squashed) {
					messages.add(null);
				}
				squashed = false;
			}
		}
		rebaseMessages.put(repository.getDirectory().getAbsolutePath(), messages);
		return new RebaseCommand.InteractiveHandler() {
			@Override
			public void prepareSteps(java.util.List<org.eclipse.jgit.lib.RebaseTodoLine> todo) {
				var lines = new java.util.ArrayList<>(todo);
				todo.clear();
				try {
					for (var i = 0; i < steps.length(); i++) {
						var step = steps.getJSONObject(i);
						var id = step.getString("id");
						var line = lines.stream().filter((l) -> l.getCommit() != null && (l.getCommit().name().startsWith(id) || id.startsWith(l.getCommit().name()))).findFirst().orElse(null);
						if (line == null) {
							throw new IllegalArgumentException("The commit " + id + " is not in the rebase.");
						}
						lines.remove(line);
						if (!"drop".equals(step.optString("action"))) {
							line.setAction(org.eclipse.jgit.lib.RebaseTodoLine.Action.parse(step.optString("action", "pick")));
							todo.add(line);
						}
					}
				} catch (IllegalArgumentException e) {
					throw e;
				} catch (Exception e) {
					throw new IllegalArgumentException(e.getMessage(), e);
				}
				for (var line : lines) {
					if (line.getCommit() != null) {
						throw new IllegalArgumentException("The commit " + line.getCommit().name() + " of the rebase has no action.");
					}
				}
			}

			@Override
			public String modifyCommitMessage(String message) {
				return nextMessage(repository, message);
			}
		};
	}

	/**
	 * @return the handler of an interactive rebase going on: its steps kept, its messages given
	 */
	private static RebaseCommand.InteractiveHandler resumed(Repository repository) {
		return new RebaseCommand.InteractiveHandler() {
			@Override
			public void prepareSteps(java.util.List<org.eclipse.jgit.lib.RebaseTodoLine> todo) {
			}

			@Override
			public String modifyCommitMessage(String message) {
				return nextMessage(repository, message);
			}
		};
	}

	private static String nextMessage(Repository repository, String message) {
		var messages = rebaseMessages.get(repository.getDirectory().getAbsolutePath());
		String next = null;
		if (messages != null) {
			synchronized (messages) {
				next = messages.isEmpty() ? null : messages.removeFirst();
			}
		}
		if (next == null && message != null && message.length() > 1 && message.substring(1).startsWith(" This is a combination of")) {
			// the messages of the commits squashed, separated as Git does, rather than line after line
			var comment = message.charAt(0);
			var parts = new java.util.ArrayList<String>();
			var part = new StringBuilder();
			for (var line : message.split("\n", -1)) {
				if (!line.isEmpty() && line.charAt(0) == comment) {
					if (!part.toString().isBlank()) {
						parts.add(part.toString().strip());
					}
					part.setLength(0);
				} else {
					part.append(line).append('\n');
				}
			}
			if (!part.toString().isBlank()) {
				parts.add(part.toString().strip());
			}
			return String.join("\n\n", parts);
		}
		return next == null ? message : next;
	}

	/**
	 * @return the commits a rebase on a commit replays, oldest first, without the merges it flattens
	 */
	static JSONArray rebaseTodo(Repository repository, ObjectId upstream) throws Exception {
		var todo = new JSONArray();
		try (var walk = new RevWalk(repository)) {
			walk.sort(org.eclipse.jgit.revwalk.RevSort.TOPO);
			walk.sort(org.eclipse.jgit.revwalk.RevSort.REVERSE, true);
			walk.markStart(walk.parseCommit(repository.resolve("HEAD^{commit}")));
			walk.markUninteresting(walk.parseCommit(upstream));
			for (var commit : walk) {
				if (commit.getParentCount() <= 1) {
					todo.put(new JSONObject()
							.put("id", commit.abbreviate(7).name())
							.put("subject", commit.getShortMessage())
							.put("body", commit.getFullMessage())
							.put("author", commit.getAuthorIdent().getName()));
				}
			}
		}
		return todo;
	}

	/** one of the kinds above, empty when the repository has no operation stopped */
	final String kind;
	final RepositoryState state;
	/** the commits of the sides: the base, mine, the commit taken; null when they are not known */
	ObjectId base, ours, theirs;
	/** the names of the sides, as a branch */
	String oursName = "", theirsName = "";
	/** the commit a rebase, a cherry-pick or a revert applies */
	RevCommit commit;
	/** the branch rebased, and where */
	String branch = "", onto = "";
	int step, steps;
	/** the paths in conflict in the repository */
	final TreeSet<String> conflicting = new TreeSet<>();

	private GitOperation(String kind, RepositoryState state) {
		this.kind = kind;
		this.state = state;
	}

	boolean stopped() {
		return !kind.isEmpty();
	}

	/**
	 * @return whether the files of a project have conflicts
	 */
	boolean conflicts(String prefix) {
		return conflicting.stream().anyMatch((path) -> path.startsWith(prefix));
	}

	static GitOperation of(Git git) throws Exception {
		return of(git, git.status().call().getConflicting());
	}

	/**
	 * @param conflicting the paths in conflict, of a status already read
	 */
	static GitOperation of(Git git, java.util.Set<String> conflicting) throws Exception {
		var repository = git.getRepository();
		var state = repository.getRepositoryState();
		var operation = switch (state) {
		case MERGING, MERGING_RESOLVED -> new GitOperation(MERGE, state);
		case CHERRY_PICKING, CHERRY_PICKING_RESOLVED -> new GitOperation(CHERRY_PICK, state);
		case REVERTING, REVERTING_RESOLVED -> new GitOperation(REVERT, state);
		case REBASING, REBASING_REBASING, REBASING_MERGE, REBASING_INTERACTIVE, APPLY -> new GitOperation(REBASE, state);
		default -> new GitOperation(conflicting.isEmpty() ? "" : CONFLICTS, state);
		};
		operation.conflicting.addAll(conflicting);
		var gitDir = repository.getDirectory().getAbsolutePath();
		if (CONFLICTS.equals(operation.kind)) {
			operation.theirsName = appliedStashes.getOrDefault(gitDir, "the changes applied");
		} else {
			appliedStashes.remove(gitDir);
		}
		try (var walk = new RevWalk(repository)) {
			var head = repository.resolve("HEAD^{commit}");
			operation.ours = head;
			operation.oursName = head == null ? "" : branchName(repository);
			switch (operation.kind) {
			case MERGE -> {
				var heads = repository.readMergeHeads();
				if (heads != null && !heads.isEmpty()) {
					operation.theirs = heads.get(0);
					operation.base = mergeBase(walk, head, operation.theirs);
					operation.theirsName = mergedName(repository, operation.theirs);
				}
			}
			case CHERRY_PICK -> operation.picked(walk, repository.readCherryPickHead(), false);
			case REVERT -> operation.picked(walk, repository.readRevertHead(), true);
			case REBASE -> operation.rebasing(walk, repository);
			default -> {
			}
			}
		}
		return operation;
	}

	/**
	 * The sides of a commit applied on HEAD, as its changes since its parent, or reverted, as the changes
	 * back to its parent.
	 */
	private void picked(RevWalk walk, ObjectId id, boolean reverted) throws Exception {
		if (id == null) {
			return;
		}
		commit = walk.parseCommit(id);
		var parent = commit.getParentCount() > 0 ? walk.parseCommit(commit.getParent(0)) : null;
		if (reverted) {
			base = commit;
			theirs = parent;
			theirsName = "revert of " + abbreviate(commit);
		} else {
			base = parent;
			theirs = commit;
			theirsName = abbreviate(commit);
		}
		if (theirs == null) {
			// the revert of a first commit: its files are removed, as the index has them
			base = null;
		}
	}

	/**
	 * A rebase, of JGit or of Git, replays the commits of a branch on another: mine is HEAD, the commits
	 * replayed so far on the other branch, theirs the commit replayed.
	 */
	private void rebasing(RevWalk walk, Repository repository) throws Exception {
		var gitDir = repository.getDirectory();
		var dir = new File(gitDir, "rebase-merge");
		if (!dir.isDirectory()) {
			dir = new File(gitDir, "rebase-apply");
		}
		branch = Repository.shortenRefName(read(new File(dir, "head-name")));
		var ontoName = read(new File(dir, "onto_name"));
		var ontoId = read(new File(dir, "onto"));
		onto = ontoName(repository, ontoName, ontoId);
		oursName = onto;
		// the commit stopped at: Git writes REBASE_HEAD, JGit and the merge of Git stopped-sha
		ObjectId stopped = null;
		for (var file : new File[] { new File(dir, RebaseCommand.STOPPED_SHA), new File(gitDir, "REBASE_HEAD"), new File(dir, "original-commit") }) {
			var name = read(file);
			if (!name.isEmpty() && (stopped = repository.resolve(name)) != null) {
				break;
			}
		}
		var done = count(repository, dir, "done");
		var todo = count(repository, dir, "git-rebase-todo");
		var msgnum = read(new File(dir, "msgnum"));
		var end = read(new File(dir, "end"));
		// the lists done and to do, which JGit keeps up to date as Git does, rather than the counters of Git
		if (done + todo > 0) {
			step = done;
			steps = done + todo;
		} else if (msgnum.matches("\\d+") && end.matches("\\d+")) {
			step = Integer.parseInt(msgnum);
			steps = Integer.parseInt(end);
		}
		if (stopped != null) {
			commit = walk.parseCommit(stopped);
			base = commit.getParentCount() > 0 ? walk.parseCommit(commit.getParent(0)) : null;
			theirs = commit;
			theirsName = abbreviate(commit);
		}
	}

	/**
	 * @return the branch a rebase replays on: a pull names it "branch 'main' of" the address of its remote,
	 *         shown as the branch of the remote, origin/main; a rebase of Git gives only its commit, shown as
	 *         a branch at it
	 */
	private static String ontoName(Repository repository, String name, String id) {
		try {
			var pulled = java.util.regex.Pattern.compile("^branch '([^']+)' of (.+)$").matcher(name);
			if (pulled.matches()) {
				var config = repository.getConfig();
				for (var remote : config.getSubsections("remote")) {
					if (pulled.group(2).equals(config.getString("remote", remote, "url"))) {
						return remote + "/" + Repository.shortenRefName(pulled.group(1));
					}
				}
			}
			if (!name.isEmpty() && !name.matches("[0-9a-f]{40}")) {
				return Repository.shortenRefName(name);
			}
			var commit = repository.resolve(id);
			if (commit != null) {
				for (var ref : repository.getRefDatabase().getRefsByPrefix(Constants.R_HEADS)) {
					if (commit.equals(ref.getObjectId())) {
						return Repository.shortenRefName(ref.getName());
					}
				}
				for (var ref : repository.getRefDatabase().getRefsByPrefix(Constants.R_REMOTES)) {
					if (commit.equals(ref.getObjectId())) {
						return Repository.shortenRefName(ref.getName());
					}
				}
			}
		} catch (Exception e) {
			// named by its commit
		}
		return id.length() > 7 ? id.substring(0, 7) : id;
	}

	private static int count(Repository repository, File dir, String name) {
		try {
			return new File(dir, name).exists() ? repository.readRebaseTodo(dir.getName() + "/" + name, false).size() : 0;
		} catch (Exception e) {
			return 0;
		}
	}

	private static String read(File file) {
		try {
			return file.isFile() ? Files.readString(file.toPath(), StandardCharsets.UTF_8).strip() : "";
		} catch (Exception e) {
			return "";
		}
	}

	static String abbreviate(RevCommit commit) {
		return commit.abbreviate(7).name() + " " + commit.getShortMessage();
	}

	private static String branchName(Repository repository) throws Exception {
		var full = repository.getFullBranch();
		return full != null && full.startsWith(Constants.R_HEADS) ? Repository.shortenRefName(full) : repository.getBranch();
	}

	private static ObjectId mergeBase(RevWalk walk, ObjectId ours, ObjectId theirs) throws Exception {
		if (ours == null) {
			return null;
		}
		walk.reset();
		walk.setRevFilter(RevFilter.MERGE_BASE);
		walk.markStart(walk.parseCommit(ours));
		walk.markStart(walk.parseCommit(theirs));
		var base = walk.next();
		walk.reset();
		walk.setRevFilter(RevFilter.ALL);
		return base == null ? null : base.getId();
	}

	/** the branch merged, as its message names it */
	private static String mergedName(Repository repository, ObjectId theirs) throws Exception {
		var message = repository.readMergeCommitMsg();
		if (message != null && !message.isBlank()) {
			var first = message.strip().split("\n")[0];
			var matcher = java.util.regex.Pattern.compile("Merge (?:remote-tracking )?branch '([^']+)'").matcher(first);
			if (matcher.find()) {
				return matcher.group(1);
			}
			return first;
		}
		return theirs.abbreviate(7).name();
	}

	/**
	 * @return the message of the commit that ends the operation, without the comments Git adds, as the
	 *         conflicts it had
	 */
	String message(Repository repository) throws Exception {
		var message = repository.readMergeCommitMsg();
		if ((message == null || message.isBlank()) && REBASE.equals(kind)) {
			message = read(new File(new File(repository.getDirectory(), "rebase-merge"), "message"));
		}
		if ((message == null || message.isBlank()) && commit != null) {
			message = commit.getFullMessage();
		}
		if (message == null) {
			return "";
		}
		var lines = new StringBuilder();
		for (var line : message.split("\n", -1)) {
			if (!line.startsWith("#")) {
				lines.append(line).append('\n');
			}
		}
		return lines.toString().strip();
	}

	JSONObject toJson(Git git, String prefix) throws Exception {
		var json = new JSONObject();
		json.put("kind", kind);
		json.put("state", state.name());
		json.put("ours", oursName);
		json.put("theirs", theirsName);
		if (commit != null) {
			json.put("commit", new JSONObject()
					.put("id", commit.abbreviate(7).name())
					.put("subject", commit.getShortMessage())
					.put("author", commit.getAuthorIdent().getName()));
		}
		if (REBASE.equals(kind)) {
			json.put("branch", branch);
			json.put("onto", onto);
			json.put("step", step);
			json.put("steps", steps);
		}
		json.put("conflicts", conflicting.size());
		var mine = new JSONArray();
		for (var path : conflicting) {
			if (path.startsWith(prefix)) {
				mine.put(path);
			}
		}
		json.put("projectConflicts", mine);
		var resolved = conflicting.isEmpty();
		json.put("canContinue", resolved && !CONFLICTS.equals(kind));
		json.put("canSkip", REBASE.equals(kind));
		json.put("canAbort", true);
		if (!REBASE.equals(kind)) {
			// not "message", which the Studio shows as a message
			json.put("commitMessage", message(git.getRepository()));
		}
		return json;
	}

	/**
	 * Abandons the operation: the repository, its index and its files, are given back as they were before
	 * it. As git merge --abort, the files the operation changed come back as HEAD has them, the changes of
	 * the other files, not committed, stay; a rebase is abandoned as git rebase --abort does.
	 */
	void abort(Git git) throws Exception {
		var repository = git.getRepository();
		if (REBASE.equals(kind)) {
			git.rebase().setOperation(RebaseCommand.Operation.ABORT).call();
			rebaseMessages.remove(repository.getDirectory().getAbsolutePath());
			return;
		}
		// the files of the operation: in conflict, or staged by it
		var status = git.status().call();
		var paths = new TreeSet<String>();
		paths.addAll(status.getConflicting());
		paths.addAll(status.getAdded());
		paths.addAll(status.getChanged());
		paths.addAll(status.getRemoved());
		var head = repository.resolve("HEAD^{tree}");
		var inHead = new java.util.ArrayList<String>();
		var added = new java.util.ArrayList<String>();
		for (var path : paths) {
			var exists = false;
			if (head != null) {
				try (var walk = org.eclipse.jgit.treewalk.TreeWalk.forPath(repository, path, head)) {
					exists = walk != null;
				}
			}
			(exists ? inHead : added).add(path);
		}
		if (!inHead.isEmpty()) {
			git.checkout().setStartPoint(Constants.HEAD).addPaths(inHead).call();
		}
		if (!added.isEmpty()) {
			var rm = git.rm();
			added.forEach(rm::addFilepattern);
			rm.call();
		}
		repository.writeMergeHeads(null);
		repository.writeCherryPickHead(null);
		repository.writeRevertHead(null);
		repository.writeMergeCommitMsg(null);
	}

	/**
	 * Continues the operation once its conflicts are resolved and added: the merge, the commit picked or
	 * reverted, are committed, a rebase commits the commit replayed and goes on with the next ones.
	 * @param message the message of a merge, or null for the one of Git
	 */
	Outcome proceed(Git git, String message) throws Exception {
		var repository = git.getRepository();
		if (!conflicting.isEmpty()) {
			throw new ServiceException(conflicting.size() + " file" + (conflicting.size() > 1 ? "s have" : " has") + " conflicts to resolve first.");
		}
		switch (kind) {
		case REBASE -> {
			return Outcome.of(rebase(git).setOperation(RebaseCommand.Operation.CONTINUE).call());
		}
		case MERGE, CHERRY_PICK, REVERT -> {
			var status = git.status().call();
			if (!MERGE.equals(kind) && status.getAdded().isEmpty() && status.getChanged().isEmpty() && status.getRemoved().isEmpty()) {
				// nothing left to commit once resolved: the commit picked or reverted is dropped, as Git does
				repository.writeCherryPickHead(null);
				repository.writeRevertHead(null);
				repository.writeMergeCommitMsg(null);
				return new Outcome("EMPTY", null);
			}
			var commitCommand = git.commit()
					.setMessage(message != null && !message.isBlank() ? message : message(repository))
					.setCleanupMode(CommitConfig.CleanupMode.STRIP);
			if (CHERRY_PICK.equals(kind) && commit != null) {
				// the commit picked keeps its author, as Git does
				commitCommand.setAuthor(commit.getAuthorIdent());
			}
			commitCommand.call();
			return new Outcome("OK", null);
		}
		default -> throw new ServiceException("No operation to continue.");
		}
	}

	/**
	 * Skips the commit a rebase stopped at, and goes on with the next ones.
	 */
	Outcome skip(Git git) throws Exception {
		if (!REBASE.equals(kind)) {
			throw new ServiceException("Only a rebase skips a commit.");
		}
		return Outcome.of(rebase(git).setOperation(RebaseCommand.Operation.SKIP).call());
	}

	/**
	 * @return a rebase going on, with its messages when it is interactive
	 */
	private RebaseCommand rebase(Git git) {
		var rebase = git.rebase();
		if (state == RepositoryState.REBASING_INTERACTIVE) {
			rebase.runInteractively(resumed(git.getRepository()));
		}
		return rebase;
	}

	/**
	 * @return why a rebase did not end, or null when it ended or stopped as expected
	 */
	static String rebaseError(RebaseResult result) {
		if (result == null) {
			return null;
		}
		return switch (result.getStatus()) {
		case UNCOMMITTED_CHANGES -> "Commit or stash the changes first: " + String.join(", ", result.getUncommittedChanges()) + ".";
		case CONFLICTS -> "The rebase would overwrite the changes of " + String.join(", ", result.getConflicts()) + ": commit or stash them first.";
		case FAILED -> "The rebase failed on " + String.join(", ", result.getFailingPaths().keySet()) + ".";
		case STASH_APPLY_CONFLICTS -> "The rebase is done, but the changes it stashed have conflicts once applied again.";
		default -> null;
		};
	}
}
