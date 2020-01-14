/*
 * Copyright (c) 2001-2020 Convertigo SA.
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
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;

import com.twinsoft.convertigo.engine.Engine;

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
				String br = git.getRepository().getBranch();
				if (br != null && br.equals(branch)) {
					return true;
				}
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
		StoredConfig conf = git.getRepository().getConfig();
		for (String name: git.getRepository().getRemoteNames()) {
			String rUrl = conf.getString(ConfigConstants.CONFIG_REMOTE_SECTION, name, "url");
			if (url.equals(rUrl)) {
				return true;
			}
		}
		return false;
	}
	
	public static File getGitContainer() {
		return new File(System.getProperty("user.home"), "git");
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
		};
	}

	public static void pull(File dir) throws Exception {
		try (Git git = Git.open(dir)) {
			boolean pulled = git.pull().call().isSuccessful();
			Engine.logEngine.info("(ReferencedProjectManager) Pull from " + dir + " is " + pulled);
		}
	}
}
