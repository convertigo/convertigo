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

package com.twinsoft.convertigo.engine;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.util.GitUtils;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;

public class ReferencedProjectManager {


	public boolean check() {
		List<String> names = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
		boolean loaded = check(names);
		if (loaded) {
			Engine.theApp.fireMigrationFinished(new EngineEvent(""));
		}
		return loaded;
	}
	
	private boolean check(List<String> names) {
		Map<String, ProjectUrlParser> refs = new HashMap<>();
		for (String name: names) {
			try {
				Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name, true);
				project.getReferenceList().forEach(r -> {
					if (r instanceof ProjectSchemaReference) {
						ProjectSchemaReference ref = (ProjectSchemaReference) r;
						String url = ref.getProjectName();
						ProjectUrlParser parser = new ProjectUrlParser(url);
						if (parser.isValid()) {
							refs.put(parser.getProjectName(), parser);
						}
					}
				});
			} catch (Exception e) {
				Engine.logEngine.error("Failed to load " + name, e);
			}
		}
		List<String> loaded = new LinkedList<>();
		for (Entry<String, ProjectUrlParser> entry: refs.entrySet()) {
			String projectName = entry.getKey();
			try {
				ProjectUrlParser parser = entry.getValue();
				Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(parser.getProjectName(), false);
				if (project == null && importProject(parser) != null) {
					loaded.add(projectName);
				}
			} catch (Exception e) {
				Engine.logEngine.warn("(ReferencedProjectManager) Failed to load '" + projectName + "'", e);
			}
		}
		if (!loaded.isEmpty()) {
			check(loaded);
			return true;
		}
		return false;
	}
	
	public ProjectSchemaReference getReferenceFromProject(Project project, String projectName) throws EngineException {
		ProjectSchemaReference prjRef = null;
		for (Reference ref: project.getReferenceList()) {
			if (ref instanceof ProjectSchemaReference) {
				prjRef = (ProjectSchemaReference) ref;
				if (projectName.equals(prjRef.getParser().getProjectName())) {
					break;
				} else {
					prjRef = null;
				}
			}
		}
		
		if (prjRef == null) {
			prjRef = new ProjectSchemaReference();
			if (projectName.startsWith("mobilebuilder_tpl_")) {
				prjRef.setProjectName(projectName + "=https://github.com/convertigo/c8oprj-mobilebuilder-tpl.git:branch=" + projectName);
			} else {
				prjRef.setProjectName(projectName);
			}
			project.add(prjRef);
			project.changed();
			project.hasChanged = true;
		}
		
		return prjRef;
	}
	
	public Project importProjectFrom(Project project, String projectName) throws Exception {
		if (project.getName().equals(projectName)) {
			return project;
		}
		return importProject(getReferenceFromProject(project, projectName).getParser());
	}
	
	public Project importProject(ProjectUrlParser parser) throws Exception {
		String projectName = parser.getProjectName();
		Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
		File dir = null;
		File prjDir = null;
		if (project != null) {
			prjDir = project.getDirFile();
			dir = GitUtils.getWorkingDir(project.getDirFile());
			if (dir != null) {
				Engine.logEngine.info("(ReferencedProjectManager) " + projectName + " has repo " + dir);
			} else {
				Engine.logEngine.info("(ReferencedProjectManager) " + projectName + " exists without repo");
			}
		} else {
			File gitContainer = GitUtils.getGitContainer();
			dir = new File(gitContainer, parser.getGitRepo());
			if (dir.exists()) {
				if (GitUtils.asRemoteAndBranch(dir, parser.getGitUrl(), parser.getGitBranch())) {
					Engine.logEngine.info("(ReferencedProjectManager) folder has remote " + parser.getGitUrl());
				} else {
					Engine.logEngine.info("(ReferencedProjectManager) folder hasn't remote " + parser.getGitUrl());
					int i = 1;
					String suffix = "_";
					if (parser.getGitBranch() != null) {
						suffix += parser.getGitBranch();
						dir = new File(gitContainer, parser.getGitRepo() + suffix);
						if (!dir.exists() || GitUtils.asRemoteAndBranch(dir, parser.getGitUrl(), parser.getGitBranch())) {
							i = 0;
						}
						suffix += "_";
					}
					while (i > 0 && (dir = new File(gitContainer, parser.getGitRepo() + suffix + i++)).exists()) {
						if (GitUtils.asRemoteAndBranch(dir, parser.getGitUrl(), parser.getGitBranch())) {
							i = 0;
						}
					}
					Engine.logEngine.info("(ReferencedProjectManager) new folder " + dir);
				}
			}
			if (!dir.exists()) {
				GitUtils.clone(parser.getGitUrl(), parser.getGitBranch(), dir);
			} else {
				Engine.logEngine.info("(ReferencedProjectManager) Use repo " + dir);
			}
			if (parser.getProjectPath() != null) {
				prjDir = new File(dir, parser.getProjectPath());
			} else {
				prjDir = dir;
			}
		}
		if (dir != null) {
			//GitUtils.pull(dir);
			if (project == null) {
				project = Engine.theApp.databaseObjectsManager.importProject(new File(prjDir, "c8oProject.yaml"));
				Engine.logEngine.info("(ReferencedProjectManager) Referenced project is loaded: " + project);
			}
		}
		return project;
	}
}
