/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.TagOpt;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class GitUtils {

	
	public static File getWorkingDir(File file) {
		try (Git git = getGit(file)) {
			if (git != null) {
				return git.getRepository().getWorkTree();
			}
		}
		return null;
	}
	
	private static Git getGit(File file) {
		while (file != null && !new File(file, ".git").isDirectory()) {
			file = file.getParentFile();
		}
		try {
			return file != null ? Git.open(file) : null;
		} catch (Exception e) {
			return null;
		}
	}
	
	public static boolean asRemoteAndBranch(File dir, String url, String branch) throws IOException {
		try (Git git = Git.open(dir)) {
			if (git != null && asRemote(git, url)) {
				Repository repo = git.getRepository();
				String br = repo.getBranch();
				if (br != null && br.equals(branch)) {
					return true;
				}
				try {
					Ref ref = repo.findRef(branch);
					Ref head = repo.findRef("HEAD");
					ObjectId refid = ref.getObjectId();
					ObjectId headid = head.getObjectId();
					if (headid.equals(refid)) {
						return true;
					}
					refid = ref.getPeeledObjectId();
					if (headid.equals(refid)) {
						return true;
					}
				} catch (Exception e) {}
			}
			return false;
		}
	}

	public static String getRemote(File dir) throws IOException {
		if (dir == null) {
			return null;
		}
		try (Git git = Git.open(dir)) {
			Repository repo = git.getRepository();
			StoredConfig conf = repo.getConfig();
			String remote = conf.getString(ConfigConstants.CONFIG_REMOTE_SECTION, "origin", "url");
			if (remote == null) {
				for (String name: git.getRepository().getRemoteNames()) {
					remote = conf.getString(ConfigConstants.CONFIG_REMOTE_SECTION, name, "url");
					break;
				}	
			}
			if (remote == null) {
				remote = repo.getDirectory().getAbsolutePath().replace('\\', '/');
			}
			return remote;
		}
	}

	public static String getBranch(File dir) throws IOException {
		if (dir == null) {
			return null;
		}
		try (Git git = Git.open(dir)) {
			Repository repo = git.getRepository();
			return repo.getBranch();
		}
	}
	
	private static boolean asRemote(Git git, String url) {
		try (Repository repo = git.getRepository()) {
			StoredConfig conf = repo.getConfig();
			for (String name: repo.getRemoteNames()) {
				String rUrl = conf.getString(ConfigConstants.CONFIG_REMOTE_SECTION, name, "url");
				if (url.equals(rUrl)) {
					return true;
				}
			}
			return false;
		}
	}
	
	public static File getGitContainer() {
		return new File(EnginePropertiesManager.getProperty(PropertyName.GIT_CONTAINER));
	}
	
	public static void clone(String url, String branch, File dir) throws Exception {
		Engine.logEngine.info("(ReferencedProjectManager) clone " + url + " to " + dir);
		try (Git git = Git.cloneRepository().setDirectory(dir).setURI(url).setBranch(branch).setProgressMonitor(new ProgressMonitor() {

			@Override
			public void update(int completed) {
			}

			@Override
			public void start(int totalTasks) {
				Engine.logEngine.info("(ReferencedProjectManager) progress start: " + totalTasks);
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public void endTask() {
				Engine.logEngine.info("(ReferencedProjectManager) progress endTask");
			}

			@Override
			public void beginTask(String title, int totalWork) {
				Engine.logEngine.info("(ReferencedProjectManager) progress beginTask: " + title + " " + totalWork);
			}
		}).call()) {
			Engine.logEngine.info("(ReferencedProjectManager) Clone done !");
		} catch (Exception e) {
			Engine.logEngine.warn("(ReferencedProjectManager) clone " + url + " to " + dir + " failed: [" + e.getClass() + "] " + e.getMessage());
			throw e;
		};
	}

	public static void pull(File dir) throws Exception {
		try (Git git = Git.open(dir)) {
			boolean pulled = git.pull().call().isSuccessful();
			Engine.logEngine.info("(ReferencedProjectManager) Pull from " + dir + " is " + pulled);
		} catch (Exception e) {
			Engine.logEngine.warn("(ReferencedProjectManager) pull " + dir + " failed: [" + e.getClass() + "] " + e.getMessage());
			throw e;
		}
	}

	public static void fetch(File dir) throws Exception {
		try (Git git = Git.open(dir)) {
			git.fetch().setForceUpdate(true).setTagOpt(TagOpt.FETCH_TAGS).call();
			Engine.logEngine.info("(ReferencedProjectManager) Fetch from " + dir);
		} catch (Exception e) {
			Engine.logEngine.warn("(ReferencedProjectManager) fetch " + dir + " failed: [" + e.getClass() + "] " + e.getMessage());
			throw e;
		}
	}

	public static void reset(File dir) throws Exception {
		try (Git git = Git.open(dir)) {
			git.reset().setMode(ResetType.HARD).call();
			Engine.logEngine.info("(ReferencedProjectManager) Reset from " + dir);
		} catch (Exception e) {
			Engine.logEngine.warn("(ReferencedProjectManager) reset " + dir + " failed: [" + e.getClass() + "] " + e.getMessage());
			throw e;
		}
	}
	
	public static void reset(File dir, String branch) throws Exception {
		if (branch == null) {
			reset(dir);
			return;
		}
		try (Git git = Git.open(dir)) {
			Ref rev = git.getRepository().findRef(branch);
			if (rev == null || rev.getName().startsWith("refs/tags/")) {
				git.reset().setMode(ResetType.HARD).setRef(branch).call();	
			} else {
				git.reset().setMode(ResetType.HARD).setRef("origin/" + branch).call();
			}
			Engine.logEngine.info("(ReferencedProjectManager) Reset from " + dir + " to " + branch);
		} catch (Exception e) {
			Engine.logEngine.warn("(ReferencedProjectManager) reset " + dir + " to " + branch + " failed: [" + e.getClass() + "] " + e.getMessage());
			throw e;
		}
	}
	
	public static String getRev(File dir) throws Exception {
		try (Git git = Git.open(dir)) {
			ObjectId o = git.getRepository().resolve(Constants.HEAD);
			return o != null ? o.getName() : "";
		}
	}
}
