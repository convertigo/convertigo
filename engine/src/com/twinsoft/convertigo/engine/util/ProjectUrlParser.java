/*
 * Copyright (c) 2001-2025 Convertigo SA.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Engine;

public class ProjectUrlParser {
	private static final Pattern patternGit = Pattern.compile("(.+?)=(.+/(?:(.*?)/\\.git)|(?:.*/(.*?)\\.git))(.*)");
	private static final Pattern patternOpt = Pattern.compile(":(.+?)=([^:]*)");
	private static final Pattern patternHttp = Pattern.compile("(?:(.+?)=)?(https?://.*/(.*?)\\.(?i:zip|car).*?)(:.*)?");
	private String projectName;
	private String gitUrl;
	private String gitRepo;
	private String projectPath;
	private String gitBranch;
	private boolean autoPull = false;
	private Matcher matcherGit = patternGit.matcher("");
	private Matcher matcherOpt = patternOpt.matcher("");
	private Matcher matcherHttp = patternHttp.matcher("");

	public ProjectUrlParser(String projectUrl) {
		setUrl(projectUrl);
	}

	public boolean isValid() {
		return StringUtils.isNotBlank(projectName);
	}

	public void setUrl(String projectUrl) {
		projectName = gitUrl = gitRepo = projectPath = gitBranch = null;
		autoPull = false;

		synchronized (matcherGit) {
			matcherGit.reset(projectUrl);
			if (matcherGit.matches()) {
				projectName = matcherGit.group(1);
				gitUrl = matcherGit.group(2);
				gitRepo = matcherGit.group(3) != null ? matcherGit.group(3) : matcherGit.group(4);
				matcherOpt.reset(matcherGit.group(5));
				while (matcherOpt.find()) {
					String key = matcherOpt.group(1);
					String value = matcherOpt.group(2);
					switch (key) {
					case "path": projectPath = value; break;
					case "branch": gitBranch = value; break;
					case "autoPull": autoPull = "true".equalsIgnoreCase(value); break;
					}
				} 
			} else {
				matcherHttp.reset(projectUrl);
				if (matcherHttp.matches()) {
					projectName = matcherHttp.group(1) != null ? matcherHttp.group(1) : matcherHttp.group(3);
					gitUrl = matcherHttp.group(2);
				} else {
					projectName = projectUrl.replaceFirst("=.*", "");
				}
			}
		}
	}

	public String getProjectName() {
		return projectName;
	}

	public String getGitUrl() {
		return gitUrl;
	}

	public String getGitRepo() {
		return gitRepo;
	}

	public String getProjectPath() {
		return projectPath;
	}

	public String getGitBranch() {
		return gitBranch;
	}

	public String getProjectUrl() {
		return makeUrl(projectName, gitUrl, projectPath, gitBranch, autoPull);
	}

	public boolean isAutoPull() {
		return autoPull;
	}

	public String toString() {
		return isValid() ? getProjectUrl() : getProjectName();
	}

	public void setProjectName(String projectName) {
		setUrl(makeUrl(projectName, gitUrl, projectPath, gitBranch, autoPull));
	}

	public void setGitUrl(String gitUrl) {
		setUrl(makeUrl(projectName, gitUrl, projectPath, gitBranch, autoPull));
	}

	public void setProjectPath(String projectPath) {
		setUrl(makeUrl(projectName, gitUrl, projectPath, gitBranch, autoPull));
	}

	public void setGitBranch(String gitBranch) {
		setUrl(makeUrl(projectName, gitUrl, projectPath, gitBranch, autoPull));
	}

	public void setAutoPull(boolean autoPull) {
		setUrl(makeUrl(projectName, gitUrl, projectPath, gitBranch, autoPull));
	}

	static private String makeUrl(String projectName, String gitUrl, String projectPath, String gitBranch, boolean autoPull) {
		StringBuilder url = new StringBuilder(projectName);
		if (StringUtils.isNotBlank(gitUrl)) {
			url.append('=').append(gitUrl);
			if (StringUtils.isNotBlank(projectPath)) {
				url.append(":path=").append(projectPath);
			}
			if (StringUtils.isNotBlank(gitBranch)) {
				url.append(":branch=").append(gitBranch);
			}
			if (autoPull) {
				url.append(":autoPull=true");
			}
		}
		return url.toString();
	}

	static public String getUrl(String projectName) {
		try {
			Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
			if (project != null) {
				return getUrl(project);
			}
		} catch (Exception e) {
			// skip
		}
		return projectName;
	}

	static public String getUrl(Project project) {
		String projectName = project.getName();
		try {
			File prjDir = project.getDirFile();
			File wrkDir = GitUtils.getWorkingDir(prjDir);
			String remote = GitUtils.getRemote(wrkDir);
			if (remote != null) {
				String path = prjDir.getCanonicalPath().substring(wrkDir.getCanonicalPath().length());
				if (!path.isEmpty()) {
					remote += ":path=" + path.substring(1).replace('\\', '/');
				}
				String branch = GitUtils.getBranch(wrkDir);
				if (!StringUtils.isEmpty(branch)) {
					remote += ":branch=" + branch;
				}
				projectName = projectName + "=" + remote;
			}
		} catch (Exception e) {
			// skip
		}
		return projectName;
	}
	
	static public String getContributeUrl(Project project) {
		return getUrl(project);
	}
	
	static public String getUsageUrl(Project project) {
		String projectName = project.getName();
		try {
			File prjDir = project.getDirFile();
			File wrkDir = GitUtils.getWorkingDir(prjDir);
			String remote = GitUtils.getRemote(wrkDir);
			if (remote != null) {
				String branch = GitUtils.getBranch(wrkDir);
				if (!StringUtils.isEmpty(branch)) {
					remote = remote.replace(".git", "") + "/archive/" + branch + ".zip";
				}
				String path = prjDir.getCanonicalPath().substring(wrkDir.getCanonicalPath().length());
				if (!path.isEmpty()) {
					remote += ":path=" + path.substring(1).replace('\\', '/');
				}
				projectName = projectName + "=" + remote;
			}
		} catch (Exception e) {
			// skip
		}
		return projectName;
	}
	
	static private String getReadmeUrl(File prjDir) {
		String readmeUrl = "";
		try {
			File wrkDir = GitUtils.getWorkingDir(prjDir);
			String remote = GitUtils.getRemote(wrkDir);
			if (remote != null) {
				remote = remote.replace("git@github.com:","https://github.com/").replace(".git", "");
				readmeUrl = remote + "#readme";
				String branch = GitUtils.getBranch(wrkDir);
				if (!StringUtils.isEmpty(branch)) {
					readmeUrl = remote + "/tree/" + branch + "#readme";
				}
			}
		} catch (Exception e) {
			// skip
		}
		return readmeUrl;
	}
	
	static public String getReadmeUrl(String projectName) {
		return getReadmeUrl(Engine.projectFile(projectName).getParentFile());
	}
	
	static public String getReadmeUrl(Project project) {
		return getReadmeUrl(project.getDirFile());
	}
}
