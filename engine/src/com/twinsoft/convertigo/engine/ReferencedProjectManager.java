/*
 * Copyright (c) 2001-2024 Convertigo SA.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.BeansDefaultValues;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.GitUtils;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;
import com.twinsoft.convertigo.engine.util.YamlConverter;
import com.twinsoft.convertigo.engine.util.ZipUtils;

public class ReferencedProjectManager {
	private Map<File, Object> dirLock = new HashMap<>();
	
	private Object getLock(File dir) {
		synchronized (dirLock) {
			Object lock = dirLock.get(dir);
			if (lock == null) {
				dirLock.put(dir, lock = new Object());
			}
			return lock;
		}
	}
	
	public boolean check() {
		List<String> names = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
		boolean loaded = check(names);
		if (loaded) {
			Engine.theApp.fireMigrationFinished(new EngineEvent(""));
		}
		return loaded;
	}
	
	boolean check(Project project) {
		Map<String, ProjectUrlParser> refs = new HashMap<>();
		check(project, refs);
		return check(refs);
	}
	
	private boolean check(Project project, Map<String, ProjectUrlParser> refs) {
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
		return check(refs);
	}
	
	private boolean check(Map<String, ProjectUrlParser> refs) {
		List<String> loaded = new LinkedList<>();
		for (Entry<String, ProjectUrlParser> entry: refs.entrySet()) {
			String projectName = entry.getKey();
			try {
				ProjectUrlParser parser = entry.getValue();
				Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(parser.getProjectName(), false);
				Project nProject = importProject(parser); 
				if (nProject != null && nProject != project) {
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
	
	private boolean check(List<String> names) {
		boolean loaded = false;
		Map<String, ProjectUrlParser> refs = new HashMap<>();
		for (String name: names) {
			try {
				Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name, true);
				if (project != null) {
					loaded |= check(project, refs);
				}
			} catch (Exception e) {
				Engine.logEngine.error("Failed to load " + name, e);
			}
		}
		return loaded;
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
				prjRef.setProjectName(projectName + "=https://github.com/convertigo/c8oprj-mobilebuilder-tpl/archive/" + projectName + ".zip");
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
		Project targetProject = project.getName().equals(projectName) ? project : Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
		if (targetProject == null) {
			ProjectSchemaReference ref = getReferenceFromProject(project, projectName);
			if (ref != null) {
				targetProject = importProject(ref.getParser());
			}
		}
		return targetProject;
	}

	public Project importProject(ProjectUrlParser parser) throws Exception {
		return importProject(parser, false);
	}
	
	public Project importProject(ProjectUrlParser parser, boolean force) throws Exception {
		String projectName = parser.getProjectName();
		Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
		File dir = null;
		File prjDir = null;
		boolean cloneDone = false;
		if (parser.getGitRepo() == null) {
			if ((!force && project != null) || parser.getGitUrl() == null) {
				return project;
			} else {
				return Engine.theApp.databaseObjectsManager.deployProject(parser.getGitUrl(), projectName, true);
			}
		}
		if (project != null) {
			prjDir = project.getDirFile();
			dir = GitUtils.getWorkingDir(project.getDirFile());
			if (dir != null) {
				Engine.logEngine.debug("(ReferencedProjectManager) " + projectName + " has repo " + dir);
			} else {
				Engine.logEngine.debug("(ReferencedProjectManager) " + projectName + " exists without repo");
			}
		} else {
			File gitContainer = GitUtils.getGitContainer();
			String suffix = parser.getGitBranch() != null ? "_" + parser.getGitBranch() : "";
			dir = new File(gitContainer, parser.getGitRepo() + suffix);
			synchronized (getLock(dir)) {
				if (dir.exists()) {
					if (GitUtils.asRemoteAndBranch(dir, parser.getGitUrl(), parser.getGitBranch())) {
						Engine.logEngine.info("(ReferencedProjectManager) folder has remote " + parser.getGitUrl());
					} else {
						Engine.logEngine.info("(ReferencedProjectManager) folder hasn't remote " + parser.getGitUrl());
						int i = 1;
						suffix += "_";
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
					cloneDone = true;
				} else {
					Engine.logEngine.info("(ReferencedProjectManager) Use repo " + dir);
				}
			}
			if (parser.getProjectPath() != null) {
				prjDir = new File(dir, parser.getProjectPath());
			} else {
				prjDir = dir;
			}
		}
		if (dir != null) {
			if (!cloneDone && (force || (parser.isAutoPull() && !Engine.isStudioMode()))) {
				synchronized (getLock(dir)) {
					String exRev = GitUtils.getRev(dir);
					GitUtils.fetch(dir);
					GitUtils.reset(dir, parser.getGitBranch());
					String newRev = GitUtils.getRev(dir);
					if (!exRev.equals(newRev)) {
						project = null;
					}
				}
			}
			if (project == null) {
				project = Engine.theApp.databaseObjectsManager.importProject(new File(prjDir, "c8oProject.yaml"), false);
				if (!projectName.equals(project.getName())) {
					throw new EngineException("Referenced name is '" + projectName + "' but loaded project is '" + project.getName() + "'");
				}
				Engine.logEngine.info("(ReferencedProjectManager) Referenced project is loaded: " + project);
			}
		}
		return project;
	}

	public void check(File projectFile) {
		try {
			for (var ref: references(projectFile)) {
				try {
					importProject(ref.getParser()); //importProject(ref.getParser(), true);
			    } catch (Exception e) {
			        Engine.logEngine.error("Failed to load " + ref.getProjectName(), e);
			    }
			}
		} catch (Exception e) {
			Engine.logEngine.warn("(ReferencedProjectManager) Failed to check " + projectFile + " [" + e.getClass().getSimpleName() + "] " + e.getMessage());
		}
	}
	
	public static Set<ProjectSchemaReference> references(File file) throws Exception {
		if (!file.getName().endsWith(".yaml")) {
			return Collections.emptySet();
		}
		var doc = YamlConverter.readYaml(file, true);
		var nl = doc.getElementsByTagName("bean");
		for (int i = 0; i < nl.getLength();) {
			var n = (Element) nl.item(i);
			var key = n.getAttribute("yaml_key");
			if (key == null || !(key.contains("[core.Project]") || key.contains("[references.ProjectSchemaReference]"))) {
				n.getParentNode().removeChild(n);
			} else {
				i++;
			}
		}
		doc = BeansDefaultValues.unshrinkProject(doc);
		nl = doc.getElementsByTagName("reference");
		var refs = new HashSet<ProjectSchemaReference>(nl.getLength());
		for (int i = 0; i < nl.getLength(); i++) {
			var n = nl.item(i);
			var ref = (ProjectSchemaReference) DatabaseObject.read(n);
			refs.add(ref);
		}
		return refs;
	}
	
	protected void checkForIonicTemplate(String projectName, File projectFile) {
		try {
			if (projectFile != null && projectFile.exists()) {
				File projectDir = projectFile.getParentFile();
				File ionicTplDir = new File(projectDir, "ionicTpl");
				// this is a ionic builder template
				if (ionicTplDir.exists() && ionicTplDir.isDirectory()) {
					// this is a ngx ionic builder template
					if (new File(ionicTplDir,"angular.json").exists()) {
						
						// if template's ion folder does not exist: get and copy default one
						File versionJson = new File(ionicTplDir,"version.json");
						if (versionJson.exists()) {
							String tplVersion = null;
							try {
								String tsContent = FileUtils.readFileToString(versionJson, "UTF-8");
								JSONObject jsonOb = new JSONObject(tsContent);
								tplVersion = jsonOb.getString("version");
							} catch (Exception e) {
								Engine.logEngine.warn("(ReferencedProjectManager) Could not retrieve template's version for " + projectName);
							}
							
							File ngxIonObjects = new File(ionicTplDir,"ion/ion_objects.json");
							if (tplVersion != null && !ngxIonObjects.exists()) {
								String v = tplVersion;
								try {
									v = tplVersion.substring(0, 3);
									File ionZipFile = new File(Engine.TEMPLATES_PATH, "ionic/"+ v +"/ion.zip");
									if (ionZipFile.exists()) {
										Engine.logEngine.info("(ReferencedProjectManager) Copying default "+ v +" ionic objects in template for " + projectName);
										ZipUtils.expandZip(ionZipFile.getAbsolutePath(), ionicTplDir.getAbsolutePath());
									} else {
										Engine.logEngine.warn("(ReferencedProjectManager) Could not retrieve default "+ v +" ionic objects for " + projectName);
									}
								} catch( Exception e) {
									Engine.logEngine.warn("(ReferencedProjectManager) Could not retrieve default "+ v +" ionic objects for " + projectName);
								}
							}
						}
						
						// this template has its own ion_objects.json file
						File ngxIonObjects = new File(ionicTplDir,"ion/ion_objects.json");
						if (ngxIonObjects.exists()) {
							Engine.logEngine.info("(ReferencedProjectManager) Found ionic objects in template for " + projectName);
							com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager
								.addIonicTemplateProject(projectName, projectDir);
							Engine.execute(() -> {
								var dbom = Engine.theApp.databaseObjectsManager;
								for (var name: dbom.getAllProjectNamesList(true)) {
									if (name.equals(projectName)) {
										continue;
									}
									try {
										var prj = dbom.getOriginalProjectByName(name, true);
										if (prj.getMobileApplication() != null && prj.getMobileApplication().getApplicationComponent() != null) {
											var app = prj.getMobileApplication().getApplicationComponent();
											if (app instanceof ApplicationComponent && app.getTplProjectName().equals(projectName)) {
												Engine.logEngine.debug("(ReferencedProjectManager) Reloading project " + name + " ("+ prj.hashCode() +")");
												dbom.getStudioProjects().reloadProject(name);
											}
										}
									} catch (Exception e) {
										Engine.logEngine.warn("(ReferencedProjectManager) Failed to reload project " + name + " [" + e.getClass().getSimpleName() + "] " + e.getMessage());
									}
								}
							});
						}
					}
				}
			}
		} catch (Exception e) {
			Engine.logEngine.warn("(ReferencedProjectManager) Failed to check for ionic template of " + projectName + " [" + e.getClass().getSimpleName() + "] " + e.getMessage());
		}
	}
}
