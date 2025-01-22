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

package com.twinsoft.convertigo.engine.mobile;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IApplicationComponent;
import com.twinsoft.convertigo.beans.core.IPageComponent;
import com.twinsoft.convertigo.beans.core.ISharedComponent;
import com.twinsoft.convertigo.beans.core.IUIComponent;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.Contributor;
import com.twinsoft.convertigo.beans.ngx.components.IScriptComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileComponent;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UICustomAction;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.MobileBuilderBuildMode;
import com.twinsoft.convertigo.engine.util.EventHelper;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.ZipUtils;

public class NgxBuilder extends MobileBuilder {
	private Map<String,String> tpl_appCompTsImports = null;
	private Map<String,String> tpl_MainTsImports = null;
	private Map<String,String> tpl_pageTsImports = null;
	private Map<String,String> tpl_compTsImports = null;

	private Map<String,String> tpl_appModuleTsImports = null;
	private Map<String,String> tpl_pageModuleTsImports = null;
	private Map<String,String> tpl_compModuleTsImports = null;

	private Map<String,String> tpl_serviceActionTsImports = null;

	private String tpl_appModuleNgImports = null;
	private String tpl_appModuleNgProviders = null;
	private String tpl_appModuleNgDeclarations = null;
	private String tpl_appModuleNgComponents = null;

	private String tpl_pageModuleNgImports = null;
	private String tpl_pageModuleNgProviders = null;
	private String tpl_pageModuleNgDeclarations = null;
	private String tpl_pageModuleNgComponents = null;
	String tpl_pageModuleNgRoutes = null;

	private String tpl_compModuleNgImports = null;
	private String tpl_compModuleNgProviders = null;
	private String tpl_compModuleNgDeclarations = null;
	private String tpl_compModuleNgComponents = null;

	private File appDir, pagesDir, servicesDir, componentsDir;
	private File themeDir;
	private File srcDir;

	private static String FakeDeleted = "fake_deleted.ts";
	
	static class AppFileComparator implements Comparator<String> {
		public int compare(String path1, String path2) {
			String pound1 = isCompFile(path1) ? "0" : isPageFile(path1) ? "1" : "2";
			String pound2 = isCompFile(path2) ? "0" : isPageFile(path2) ? "1" : "2";
	    	return pound1.compareTo(pound2);
	    }
	}
	
	static private boolean isPageFile(String path) {
		String search = File.separator + "src" + File.separator + "app" + File.separator + "pages" + File.separator;
		return path == null ? false : path.indexOf(search) != -1;
	}
	
	static private boolean isCompFile(String path) {
		String search = File.separator + "src" + File.separator + "app" + File.separator + "components" + File.separator;
		return path == null ? false : path.indexOf(search) != -1;
	}
	
	private static boolean existTargetComp(String compDirName) {
	   return compQName(compDirName) != null;
	}
	    
    static private String compQName(String compDirName) {
    	if (compDirName != null && !compDirName.isEmpty()) {
	    	String pname = compDirName.substring(0, compDirName.indexOf('.'));
	    	
	    	String projectName = null;
	    	List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
	    	for (String s : projectNames) {
	    		if (s.toLowerCase().equals(pname)) {
	    			projectName = s;
	    			break;
	    		}
	    	}
	    	
	    	if (projectName != null) {
		    	Project project = null;
		    	try {
					project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
				} catch (EngineException e) {
					e.printStackTrace();
				}
		    	if (project != null) {
			    	MobileApplication mobileApplication = project.getMobileApplication();
			    	ApplicationComponent app = (ApplicationComponent)mobileApplication.getApplicationComponent();
			    	for (UISharedComponent uisc: app.getSharedComponentList()) {
			    		if (compDirName.equals(UISharedComponent.getNsCompDirName(uisc))) {
			    			return uisc.getQName();
			    		}
			    	}
		    	}
	    	}
    	}
    	return null;
    }
	    
	protected NgxBuilder(Project project) {
		super(project);
		initDirs("src");
	}

	private void initDirs(String src) {
		srcDir = new File(ionicWorkDir, src);
		themeDir = new File(srcDir, "theme");
		appDir = new File(srcDir, "app");

		componentsDir = new File(appDir, "components");
		servicesDir = new File(appDir, "services");
		pagesDir = new File(appDir, "pages");
	}

	private File pageDir(PageComponent page) {
		return new File(pagesDir, page.getName().toLowerCase());
	}

	private File compDir(UISharedComponent uisc) {
		return new File(componentsDir, UISharedComponent.getNsCompDirName(uisc));
	}

	private String compFileName(UISharedComponent uisc) {
		return UISharedComponent.getNsCompFileName(uisc);
	}

	private String compName(UISharedComponent uisc) {
		return UISharedComponent.getNsCompName(uisc);
	}

	@Override
	public void setAppBuildMode(MobileBuilderBuildMode buildMode) {
		super.setAppBuildMode(buildMode);

		if (project != null) {
			try {
				appPwaChanged(project.getMobileApplication().getApplicationComponent());
			} catch (Exception e) {
				Engine.logEngine.warn("("+ builderType +") enabled to change build mode");
			}
		}
	}

	@Override
	public void pageEnabled(final IPageComponent pageComponent) throws EngineException {
	}

	@Override
	public void pageDisabled(final IPageComponent pageComponent) throws EngineException {
	}

	@Override
	public void pageAdded(final IPageComponent pageComponent) throws EngineException {
	}

	@Override
	public void compAdded(final ISharedComponent sharedComponent) throws EngineException {
	}

	@Override
	public void pageRemoved(final IPageComponent pageComponent) throws EngineException {
	}

	@Override
	public void compRemoved(final ISharedComponent sharedComponent) throws EngineException {
	}

	@Override
	public void pageRenamed(final IPageComponent pageComponent, final String oldName) throws EngineException {
	}

	@Override
	public void compRenamed(final ISharedComponent sharedComponent, final String oldName) throws EngineException {
	}

	@Override
	public void pageTemplateChanged(final IPageComponent pageComponent) throws EngineException {
	}

	@Override
	public void compTemplateChanged(final ISharedComponent sharedComponent) throws EngineException {
	}

	@Override
	public void pageStyleChanged(final IPageComponent pageComponent) throws EngineException {
	}


	@Override
	public void compStyleChanged(ISharedComponent sharedComponent) throws EngineException {
	}

	@Override
	public void appContributorsChanged(final IApplicationComponent appComponent) throws EngineException {
	}

	@Override
	public void pageTsChanged(final IPageComponent pageComponent, boolean forceTemp) throws EngineException {
	}

	@Override
	public void compTsChanged(ISharedComponent sharedComponent, boolean forceTemp) throws EngineException {
	}

	@Override
	public void pageModuleTsChanged(final IPageComponent pageComponent) throws EngineException {
	}

	@Override
	public void compModuleTsChanged(ISharedComponent sharedComponent) throws EngineException {
	}

	public void appChanged() throws EngineException {
		updateSourceFiles();
	}
	
	@Override
	public void appTsChanged(final IApplicationComponent appComponent, boolean forceTemp) throws EngineException {
	}

	@Override
	public void appStyleChanged(final IApplicationComponent appComponent) throws EngineException {
	}

	@Override
	public void appTemplateChanged(final IApplicationComponent appComponent) throws EngineException {
	}

	@Override
	public void appThemeChanged(final IApplicationComponent appComponent) throws EngineException {
	}

	@Override
	public void appCompTsChanged(final IApplicationComponent appComponent) throws EngineException {
	}

	@Override
	public void appModuleTsChanged(final IApplicationComponent appComponent) throws EngineException {
	}

	@Override
	public void appPwaChanged(final IApplicationComponent appComponent) throws EngineException {
		ApplicationComponent app = (ApplicationComponent)appComponent;
		if (app != null && initDone) {
			synchronized (app) {
				configurePwaApp(app);		// for worker
				writeAppComponentTs(app);	// for prod mode
				if(app.compareToTplVersion("8.4.0.3") < 0) {
					writeAppModuleTs(app); // for worker
				}
				else {
					writeAppMainTs(app); // for worker
				}
				moveFiles();
				Engine.logEngine.trace("("+ builderType +") Handled 'appPwaChanged'");
			}
		}
	}

	@Override
	public void appRootChanged(final IApplicationComponent appComponent) throws EngineException {
	}

	@Override
	public void appRouteChanged(final IApplicationComponent appComponent) throws EngineException {
	}

	@Override
	protected synchronized void init() throws EngineException {
		String projectID = Project.formatNameWithHash(project);
		
		if (initDone) {
			Engine.logEngine.warn("("+ builderType +") Builder already initialized for ionic project "+ projectID +". Skipping");
			return;
		}

		ApplicationComponent application = (ApplicationComponent) project.getMobileApplication().getApplicationComponent();
		String tplName = application.getTplProjectName();
		if (!project.getName().equals(tplName)) {
			try {
				Engine.theApp.referencedProjectManager.getReferenceFromProject(project, tplName);
				Engine.theApp.referencedProjectManager.importProjectFrom(project, tplName);
			} catch (Exception e) {
				throw new EngineException("Failed to import referenced template: " + tplName + " :" + e.getMessage(), e);
			}
		}

		ionicTplDir = application.getIonicTplDir();
		if (!ionicTplDir.exists()) {
			throw new EngineException("Missing template project '" + application.getTplProjectName() + "'\nThe template folder should be in: " + ionicTplDir.getPath());
		}

		if (isIonicTemplateBased()) {
			Engine.logEngine.debug("("+ builderType +") Start initializing builder for ionic project "+ projectID);
			
			if (eventHelper == null) {
				eventHelper = new EventHelper();
			}

			setNeedPkgUpdate(false);

			// Clean directories 
			cleanDirectories();

			// Copy template directory to working directory
			copyTemplateFiles();

			// Copy template assets to build directory
			copyAssetsToBuildDir();

			// Modify configuration files
			updateConfigurationFiles();

			// Tpl version
			updateTplVersion();

			// Modify env.json
			initEnvFile();

			// PWA
			configurePwaApp(application);

			// Write source files (based on bean components)
			updateSourceFiles();

			if (Engine.isStudioMode() || Engine.isCliMode()) {
				// map for deferring write of source files
				if (pushedFiles == null) {
					pushedFiles = new HashMap<String, CharSequence>();
				}

				// retrieve/set necessary external component files
				updateConsumer();
				updateConsumers();
			}

			initDone = true;
			Engine.logEngine.debug("("+ builderType +") End initializing builder for ionic project "+ projectID);
		}
	}

	private void updateConsumer() {
		for (String pname: ComponentRefManager.getProjectsForUpdate(project.getName())) {
			try {
				Project p = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(pname, false);
				if (p != null) {
					((NgxBuilder)p.getMobileBuilder()).updateConsumers(project);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Engine.logEngine.warn("Unable to update consummer for project "+ pname + ": "+ e.getMessage());
			}
		}
	}

	private void updateConsumers() {
		updateConsumers(null);
	}

	private void updateConsumers(Project to) {
		MobileApplication mobileApplication = project.getMobileApplication();
		ApplicationComponent app = (ApplicationComponent)mobileApplication.getApplicationComponent();
		for (UISharedComponent uisc: app.getSharedComponentList()) {
			updateConsumers(uisc, to);
		}
	}

	// for created or modified files
	private void updateConsumers(UISharedComponent uisc, Project to) {
		String compName = uisc.getName();
		String compQName = uisc.getQName();

		for (String useQName: ComponentRefManager.getCompConsumersForUpdate(compQName, project, to)) {
			try {
				File dest = new File(Engine.projectDir(projectName(useQName)), UISharedComponent.getNsCompDirPath(compQName, compName));
				File src = new File(project.getDirPath(), UISharedComponent.getNsCompDirPath(compQName, compName));

				if (src.exists() && shouldUpdate(src, dest)) {
					Project dest_project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName(useQName), false);
					boolean isDestMobileBuilderInitialized = dest_project != null && dest_project.isMobileBuilderInitialized();
					if (initDone && isDestMobileBuilderInitialized) {
						if (uisc.isEnabled() && !ComponentRefManager.isEnabled(useQName)) {
							Engine.logEngine.trace("["+project.getName()+"] For "+useQName+" ignoring " + compQName + " modifications");
							continue;
						}
					}
					Engine.logEngine.trace("["+project.getName()+"] For "+useQName+" taking into account " + compQName + " modifications");
					Engine.logEngine.debug("["+project.getName()+"] MB copying " + src + " to " + dest);
					FileUtils.copyDirectory(src, dest, ComponentRefManager.copyFileFilter, true);

					if (isDestMobileBuilderInitialized) {
						dest_project.getMobileBuilder().updateEnvFile();
					}
				}
			} catch (Exception e) {
				Engine.logEngine.warn("["+project.getName()+"] MB unabled to update consumers for "+ projectName(useQName) + ": " + e.getMessage());
			}
    	}
	}

	static private boolean shouldUpdate(File dirSrc, File dirDest) {
		for (final File src : dirSrc.listFiles()) {
			if (src.isFile()) {
				if (src.getName().endsWith(".temp.ts")) {
					continue;
				}
				File dest = new File(dirDest, src.getName());
				if (dest.exists()) {
					if (src.lastModified() != dest.lastModified()) {
						return true;
					}
				} else {
					return true;
				}
			}
		}
		return false;
	}

	
	@Override
	protected void copyTemplateFiles() throws EngineException {
		FileFilter ff = new FileFilter() {
			@Override
			public boolean accept(File file) {
				var name = file.getName();
				return (file.isDirectory() && (name.equals("ion") || name.equals("platforms"))) ? false : true;
			}
		};
		try {
			FileUtils.copyDirectory(ionicTplDir, ionicWorkDir, ff, true);
			Engine.logEngine.trace("("+ builderType +") Template files copied for ionic project '"+ project.getName() +"'");
		}
		catch (Exception e) {
			throw new EngineException("Unable to copy ionic template files for ionic project '"+ project.getName() +"'",e);
		}
	}

	@Override
	protected void updateTplVersion() {
		super.updateTplVersion();
	}

	private void initEnvFile() {
		try {
			String appTemplateVersion = getTplVersion() != null ? this.tplVersion : "";

			Long storedGenerationTime = (Long) getStoredEnvKey("appGenerationTime");
			Long appGenerationTime = storedGenerationTime == null ? System.currentTimeMillis() : storedGenerationTime;

			JSONObject envJSON = new JSONObject();
			envJSON.put("appTemplateVersion", appTemplateVersion);
			envJSON.put("appGenerationTime", appGenerationTime);
			envJSON.put("remoteBase", EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL) + "/projects/" + project.getName() + "/_private");
			FileUtils.write(new File(ionicWorkDir, "src/env.json"), envJSON.toString(4), "UTF-8");
			Engine.logEngine.trace("("+ builderType +") Initialized env.json for ionic project "+ project.getName());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void updateEnvFile() {
		JSONObject envJSON = new JSONObject();
		try {
			envJSON.put("appTemplateVersion", getTplVersion() != null ? this.tplVersion : "");
			envJSON.put("appGenerationTime", System.currentTimeMillis());
			envJSON.put("remoteBase", EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL) + "/projects/" + project.getName() + "/_private");
			FileUtils.write(new File(ionicWorkDir, "src/env.json"), envJSON.toString(4), "UTF-8");
			Engine.logEngine.trace("("+ builderType +") Updated env.json for ionic project "+ project.getName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Object getStoredEnvKey(String key) {
		try {
			File storedEnvFile = new File(projectDir, "_private/env.json");
			if (storedEnvFile.exists()) {
				String jsonContent = FileUtils.readFileToString(storedEnvFile, "UTF-8");
				JSONObject jsonOb = new JSONObject(jsonContent);
				if (jsonOb.has(key)) {
					return jsonOb.get(key);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void storeEnvFile() {
		try {
			File src = new File(ionicWorkDir, "src/env.json");
			if (src.exists()) {
				File dest = new File(projectDir, "_private/env.json");
				FileUtils.copyFile(src, dest, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected synchronized void release() throws EngineException {
		String projectID = Project.formatNameWithHash(project);
		
		if (!initDone) {
			Engine.logEngine.warn("("+ builderType +") Builder already released for ionic project "+ projectID +". Skipping");
			return;
		}

		if (isIonicTemplateBased()) {
			Engine.logEngine.debug("("+ builderType +") Start releasing builder for ionic project "+ projectID);
			isReleasing = true;
			
			moveFilesForce();

			if (pushedFiles != null) {
				pushedFiles.clear();
				pushedFiles = null;
			}

			if (writtenFiles != null) {
				writtenFiles.clear();
			}

			if (tpl_appCompTsImports != null) {
				tpl_appCompTsImports.clear();
				tpl_appCompTsImports = null;
			}
			
			if (tpl_MainTsImports != null) {
				tpl_MainTsImports.clear();
				tpl_MainTsImports = null;
			}
			
			if (tpl_pageTsImports != null) {
				tpl_pageTsImports.clear();
				tpl_pageTsImports = null;
			}
			if (tpl_appModuleTsImports != null) {
				tpl_appModuleTsImports.clear();
				tpl_appModuleTsImports = null;
			}
			if (tpl_pageModuleTsImports != null) {
				tpl_pageModuleTsImports.clear();
				tpl_pageModuleTsImports = null;
			}
			if (tpl_serviceActionTsImports != null) {
				tpl_serviceActionTsImports.clear();
				tpl_serviceActionTsImports = null;
			}
			if (tplVersion != null) {
				tplVersion = null;
			}
			if (eventHelper != null) {
				eventHelper = null;
			}
			setNeedPkgUpdate(false);

			storeEnvFile();

			resetAll();
			
			isReleasing = false;
			initDone = false;
			Engine.logEngine.debug("("+ builderType +") End releasing builder for ionic project "+ projectID);
		}
	}

	@Override
	protected void copyAssetsToBuildDir() throws EngineException {
		super.copyAssetsToBuildDir();
		try {
			File tAssets = new File(ionicTplDir, "src/assets");
			File bAssets = new File(ionicWorkDir, "../../DisplayObjects/mobile/assets");
			if (tAssets.exists() && bAssets.exists()) {
				FileUtils.mergeDirectories(tAssets, bAssets);
				Engine.logEngine.trace("("+ builderType +") Assets files copied for ionic project '"+ project.getName() +"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to copy ionic assets files for ionic project '"+ project.getName() +"'",e);
		}
	}

	@Override
	protected void updateConfigurationFiles() throws EngineException {
		super.updateConfigurationFiles();
	}

	private List<PageComponent> getEnabledPages(final ApplicationComponent application) {
		List<PageComponent> pages = new ArrayList<PageComponent>();
		for (PageComponent page : application.getPageComponentList()) {
			synchronized (page) {
				if (page.isEnabled()) {
					pages.add(page);
				}
			}
		}
		return pages;
	}

	private boolean updateUseCallables = true;
	
	private void updateSourceFiles() throws EngineException {
		if (updateUseCallables) {
			call_updateSourceFiles();
		} else {
			do_updateSourceFiles();
		}
	}
	
	private Callable<String> newCallable(final MobileComponent mbc) {
		return new Callable<String>() {
			@Override
			public String call() throws Exception {
				String s = Thread.currentThread().getName();
				long t0 = System.currentTimeMillis();
				if (mbc instanceof UISharedComponent) {
					writeCompSourceFiles((UISharedComponent)mbc);
				}
				else if (mbc instanceof PageComponent) {
					writePageSourceFiles((PageComponent)mbc);
				}
				long t1 = System.currentTimeMillis();
				return ("["+s+"] writeSourceFiles for "+ mbc.getClass().getName() + " " + mbc.getName() + " done in "+ (t1-t0) + "ms");
			}
		};
	}
	
	private static void invokeAll(ExecutorService executor, List<Callable<String>> list) {
		if (executor != null && list != null) {
			if (list.size() > 0) {
			    List<Future<String>> resultList = null;
			    try {
			      resultList = executor.invokeAll(list);
			    } catch (InterruptedException e) {
			      e.printStackTrace();
			    }
			 
			    if (resultList != null) {
			    	if (Engine.logEngine.isTraceEnabled()) {
				        for (int i = 0; i < resultList.size(); i++) {
				            Future<String> future = resultList.get(i);
				            try {
				              String result = future.get();
				              Engine.logEngine.trace(result);
				            } catch (Exception e) {
				              e.printStackTrace();
				            }
				        }
			    	}
			        resultList.clear();
				    resultList = null;
			    }
			}
		    list.clear();
		    list = null;
		}
	}
	
	private void resetAll() {
		try {
			final MobileApplication mobileApplication = project.getMobileApplication();
			if (mobileApplication != null) {
				final ApplicationComponent application = (ApplicationComponent) mobileApplication.getApplicationComponent();
				if (application != null) {
					application.reset();
					for (UISharedComponent uisc: application.getSharedComponentList()) {
						uisc.reset();
					}
					for (PageComponent page: application.getPageComponentList()) {
						page.reset();
					}
				}
			}
		} catch (Exception e) {}
	}
	
	private void call_updateSourceFiles() throws EngineException {
		ExecutorService executor = null;
		try {
			final MobileApplication mobileApplication = project.getMobileApplication();
			if (mobileApplication != null) {
				final ApplicationComponent application = (ApplicationComponent) mobileApplication.getApplicationComponent();
				if (application != null) {
					String usedTplVersion = getTplVersion();
					Engine.logEngine.debug("("+ builderType +") Template version used: " + usedTplVersion 
							+ " for project '"+ application.getProject().getName() +"'.");					String appTplVersion = application.requiredTplVersion();
					Engine.logEngine.debug("("+ builderType +") Min template version required: " + appTplVersion 
							+ " for project '"+ application.getProject().getName() +"'.");
					if (compareVersions(usedTplVersion, appTplVersion) >= 0) {
						long t0 = System.currentTimeMillis();
						
						List<Callable<String>> cList = new ArrayList<Callable<String>>();
						for (UISharedComponent uisc: application.getSharedComponentList()) {
							if (uisc.isReset()) {
								cList.add(newCallable(uisc));
							}
						}
						List<Callable<String>> pList = new ArrayList<Callable<String>>();
						for (PageComponent page: application.getPageComponentList()) {
							if (page.isReset()) {
								pList.add(newCallable(page));
							}
						}
						
						List<Callable<String>> aList = new ArrayList<Callable<String>>();
						if (application.isReset()) {
							aList.add(new Callable<String>() {
								@Override
								public String call() throws Exception {
									String s = Thread.currentThread().getName();
									long t0 = System.currentTimeMillis();
									removeUselessComps(application);
									long t1 = System.currentTimeMillis();
									return ("["+s+"] removeUselessComps for application " + application.getName() + " done in "+ (t1-t0) + "ms");
								}
							});
							aList.add(new Callable<String>() {
								@Override
								public String call() throws Exception {
									String s = Thread.currentThread().getName();
									long t0 = System.currentTimeMillis();
									removeUselessPages(application);
									long t1 = System.currentTimeMillis();
									return ("["+s+"] removeUselessPages for application " + application.getName() + " done in "+ (t1-t0) + "ms");
								}
							});
							aList.add(new Callable<String>() {
								@Override
								public String call() throws Exception {
									String s = Thread.currentThread().getName();
									long t0 = System.currentTimeMillis();
									writeAppSourceFiles(application);
									long t1 = System.currentTimeMillis();
									return ("["+s+"] writeAppSourceFiles for application " + application.getName() + " done in "+ (t1-t0) + "ms");
								}
							});
							if (initDone && buildMutex == null) {
								aList.add(new Callable<String>() {
									@Override
									public String call() throws Exception {
										String s = Thread.currentThread().getName();
										long t0 = System.currentTimeMillis();
										updateConsumer();
										long t1 = System.currentTimeMillis();
										return ("["+s+"] updateConsumer for application " + application.getName() + " done in "+ (t1-t0) + "ms");
									}
								});
							}
						}
						
						executor = Executors.newCachedThreadPool();
						invokeAll(executor, cList);
						invokeAll(executor, pList);
						invokeAll(executor, aList);
						
						if (initDone && autoBuild && buildMutex != null) {
							Engine.logEngine.trace("(NgxBuilder@"+ project.getName()+") start moveFilesForce for consumer update");
							moveFilesForce();
							Engine.logEngine.trace("(NgxBuilder@"+ project.getName()+") end moveFilesForce for consumer update");
						}
						
						long t1 = System.currentTimeMillis();
						Engine.logEngine.debug("("+ builderType +") Application source files updated for ionic project '"+ project.getName() +"' in "+ (t1-t0) + "ms");
					} else {
						cleanDirectories();
						throw new EngineException("Template project minimum "+ appTplVersion +" is required for this project. Current used is "+ usedTplVersion +"\n" +
								"You can change template by configuring the 'Template project' property of your project's 'Application' object.\n" + 
								"Then, be sure to update the project node modules packages (Application Right Click->Update packages and execute) \n");
					}
				}
			}
		}
		catch (EngineException e) {
			throw e;
		}
		catch (Exception e) {
			throw new EngineException("Unable to update application source files for ionic project '"+ project.getName() +"'",e);
		} finally {
			if (executor != null) {
				executor.shutdown();
			}
		}
	}

	private void do_updateSourceFiles() throws EngineException {
		try {
			MobileApplication mobileApplication = project.getMobileApplication();
			if (mobileApplication != null) {
				ApplicationComponent application = (ApplicationComponent) mobileApplication.getApplicationComponent();
				if (application != null) {
					String appTplVersion = application.requiredTplVersion();
					if (compareVersions(tplVersion, appTplVersion) >= 0) {
						long t0 = System.currentTimeMillis();
						for (UISharedComponent comp: application.getSharedComponentList()) {
							if (comp.isReset()) {
								writeCompSourceFiles(comp);
							}
						}
						for (PageComponent page : application.getPageComponentList()) {
							if (page.isReset()) {
								writePageSourceFiles(page);
							}
						}
						if (application.isReset()) {
							removeUselessComps(application);
							removeUselessPages(application);
							writeAppSourceFiles(application);
							if (initDone && buildMutex == null) {
								updateConsumer();
							}
						}
						
						long t1 = System.currentTimeMillis();
						Engine.logEngine.debug("("+ builderType +") Application source files updated for ionic project '"+ project.getName() +"' in "+ (t1-t0) + "ms");
					} else {
						cleanDirectories();
						throw new EngineException("Template project minimum "+ appTplVersion +" is required for this project.\n" +
								"You can change template by configuring the 'Template project' property of your project's 'Application' object.\n" + 
								"Then, be sure to update the project node modules packages (Application Right Click->Update packages and execute) \n");
					}

				}
			}
		}
		catch (EngineException e) {
			throw e;
		}
		catch (Exception e) {
			throw new EngineException("Unable to update application source files for ionic project '"+ project.getName() +"'",e);
		}
	}
	
	private void writePageTemplate(PageComponent page) throws EngineException {
		try {
			if (page != null) {
				String pageName = page.getName();
				File pageDir = pageDir(page);
				File pageHtmlFile = new File(pageDir, pageName.toLowerCase() + ".html");
				String computedTemplate = page.getComputedTemplate();
				writeFile(pageHtmlFile, computedTemplate, "UTF-8");

				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") Ionic template file generated for page '"+pageName+"'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page template file",e);
		}
	}

	private void writeCompTemplate(UISharedComponent comp) throws EngineException {
		try {
			if (comp != null) {
				File compHtmlFile = new File(compDir(comp), compFileName(comp) + ".html");
				String computedTemplate = comp.getComputedTemplate();
				writeFile(compHtmlFile, computedTemplate, "UTF-8");

				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") Ionic template file generated for component '"+comp.getName()+"'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page template file",e);
		}
	}

	private void writePageStyle(PageComponent page) throws EngineException {
		try {
			if (page != null) {
				String pageName = page.getName();
				File pageDir = pageDir(page);
				File pageScssFile = new File(pageDir, pageName.toLowerCase() + ".scss");
				String computedScss = page.getComputedStyle();
				writeFile(pageScssFile, computedScss, "UTF-8");

				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") Ionic scss file generated for page '"+pageName+"'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page scss file",e);
		}
	}

	private void writeCompStyle(UISharedComponent comp) throws EngineException {
		try {
			if (comp != null) {
				File compScssFile = new File(compDir(comp), compFileName(comp) + ".scss");
				String computedScss = comp.getComputedStyle();
				writeFile(compScssFile, computedScss, "UTF-8");

				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") Ionic scss file generated for component '"+comp.getName()+"'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page scss file",e);
		}
	}

	@Override
	public String getTempTsRelativePath(final IPageComponent pageComponent) throws EngineException {
		PageComponent page = (PageComponent) pageComponent;
		try {
			if (page != null) {
				String pageName = page.getName();
				File pageDir = pageDir(page);
				File tempTsFile = new File(pageDir, pageName.toLowerCase() + ".temp.ts");
				String filePath = tempTsFile.getPath().replace(projectDir.getPath(), File.separator);
				return filePath;
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write page temp ts file",e);
		}
		return null;
	}

	@Override
	public String getTempTsRelativePath(final ISharedComponent compComponent) throws EngineException {
		UISharedComponent comp = (UISharedComponent) compComponent;
		try {
			if (comp != null) {
				File tempTsFile = new File(compDir(comp), compFileName(comp) + ".temp.ts");
				String filePath = tempTsFile.getPath().replace(projectDir.getPath(), File.separator);
				return filePath;
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write component temp ts file",e);
		}
		return null;
	}

	@Override
	public String getFunctionTempTsRelativePath(final IUIComponent uiComponent) throws EngineException {
		UIComponent uic = (UIComponent)uiComponent;
		IScriptComponent main = uic.getMainScriptComponent();
		if (main != null) {
			File tempTsDir = null;
			String tempTsFileName = null;

			if (main instanceof ApplicationComponent) {
				UIActionStack stack = uic.getSharedAction();
				tempTsDir = stack == null ? appDir : servicesDir;
				tempTsFileName = stack == null ? "app.component.function.temp.ts" : "actionbeans.service.function.temp.ts";
			}
			if (main instanceof PageComponent) {
				PageComponent page = (PageComponent)main;
				String pageName = page.getName();
				tempTsDir = pageDir(page);
				tempTsFileName = pageName.toLowerCase() + ".function.temp.ts";
			}
			if (main instanceof UISharedComponent) {
				UISharedComponent comp = (UISharedComponent)main;
				tempTsDir = compDir(comp);
				tempTsFileName = compFileName(comp) + ".function.temp.ts";
			}

			if (tempTsDir != null && tempTsFileName != null) {
				if (uiComponent instanceof UICustomAction) {
					tempTsFileName = "CTS" + ((UICustomAction) uiComponent).priority + ".temp.ts";
				}

				File tempTsFile = new File(tempTsDir, tempTsFileName);
				return tempTsFile.getPath().replace(projectDir.getPath(), File.separator);
			}			
		}
		return null;
	}

	@Override
	public void writeFunctionTempTsFile(final IUIComponent uiComponent, String functionMarker) throws EngineException {
		UIComponent uic = (UIComponent)uiComponent;
		try {
			IScriptComponent main = uic.getMainScriptComponent();
			if (main != null) {
				String tempTsFileName = null, tsContent = null;
				File tempTsDir = null;
				UIActionStack sharedAction = null;

				if (main instanceof ApplicationComponent) {
					sharedAction = uic.getSharedAction();

					tempTsDir = sharedAction == null ? appDir : servicesDir;
					tempTsFileName = sharedAction == null ? "app.component.function.temp.ts" : "actionbeans.service.function.temp.ts";

					File appTsFile = sharedAction == null ? new File(appDir, "app.component.ts") : 
						new File(servicesDir, "actionbeans.service.ts");
					synchronized (writtenFiles) {
						if (writtenFiles.contains(appTsFile)) {
							File appTsFileTmp = toTmpFile(appTsFile);
							if (appTsFileTmp.exists()) {
								appTsFile = appTsFileTmp;
							}
						}
					}

					tsContent = FileUtils.readFileToString(appTsFile, "UTF-8");
				}
				if (main instanceof PageComponent) {
					PageComponent page = (PageComponent)main;
					String pageName = page.getName();
					tempTsDir = pageDir(page);
					tempTsFileName = pageName.toLowerCase() + ".function.temp.ts";

					if (page.isEnabled()) {
						File pageTsFile = new File(tempTsDir, pageName.toLowerCase() + ".ts");
						synchronized (writtenFiles) {
							if (writtenFiles.contains(pageTsFile)) {
								File pageTsFileTmp = toTmpFile(pageTsFile);
								if (pageTsFileTmp.exists()) {
									pageTsFile = pageTsFileTmp;
								}
							}
						}

						tsContent = FileUtils.readFileToString(pageTsFile, "UTF-8");
					} else {
						tsContent = getPageTsContent(page);
					}
				}
				if (main instanceof UISharedComponent) {
					UISharedComponent comp = (UISharedComponent)main;
					tempTsDir = compDir(comp);
					tempTsFileName = compFileName(comp) + ".function.temp.ts";

					boolean isEnabled = true;
					if (isEnabled) {
						File compTsFile = new File(tempTsDir, compFileName(comp) + ".ts");
						synchronized (writtenFiles) {
							if (writtenFiles.contains(compTsFile)) {
								File compTsFileTmp = toTmpFile(compTsFile);
								if (compTsFileTmp.exists()) {
									compTsFile = compTsFileTmp;
								}
							}
						}

						tsContent = FileUtils.readFileToString(compTsFile, "UTF-8");
					} else {
						tsContent = getCompTsContent(comp);
					}
				}

				// Replace all Begin_c8o_XXX, End_c8o_XXX except for functionMarker
				Pattern pattern = Pattern.compile("/\\*Begin_c8o_(.+)\\*/");
				Matcher matcher = pattern.matcher(tsContent);
				while (matcher.find()) {
					String markerId = matcher.group(1);
					if (!markerId.equals(functionMarker)) {
						String beginMarker = "/*Begin_c8o_" + markerId + "*/";
						String endMarker = "/*End_c8o_" + markerId + "*/";
						tsContent = tsContent.replace(beginMarker, "//---"+markerId+"---");
						tsContent = tsContent.replace(endMarker, "//---"+markerId+"---");
					}
				}

				// CustomAction : reduce code lines (action's function only)
				if (tempTsDir != null && tempTsFileName != null) {
					if (uiComponent instanceof UICustomAction) {
						UICustomAction uica = (UICustomAction)uic;
						tempTsFileName = "CTS" + uica.priority + ".temp.ts";
						int index = tsContent.indexOf("export class ");
						if (index != -1) {
							String classType = uica.getMainClassType();
							String sImport = "";
							if (main instanceof ApplicationComponent) {
								if (uic.getSharedAction() == null) {
									sImport = "import { "+ classType +" } from './app.component';";
								}
							} else if (main instanceof PageComponent) {
								sImport = "import { "+ classType +" } from './"+ classType.toLowerCase() +"';";
							} else if (main instanceof UISharedComponent) {
								sImport = "import { "+ compName((UISharedComponent)main) +" } from './"+ compFileName((UISharedComponent)main) +"';";
							}

							int i = tsContent.indexOf("{", index);
							tsContent = tsContent.substring(0, i+1) + System.lineSeparator() +
									uica.getActionCode() + System.lineSeparator() +
									"}" + System.lineSeparator();

							tsContent = tsContent.replace("@Component", sImport + System.lineSeparator() + "@Component");
							tsContent = tsContent.replace("export class "+ classType +" ", "export class _Type_ ");
						}
					}
				}

				// Write file (do not need delay)
				tsContent = LsPattern.matcher(tsContent).replaceAll(System.lineSeparator());
				File tempTsFile = new File(tempTsDir, tempTsFileName);
				FileUtils.write(tempTsFile, tsContent, "UTF-8");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write function temp ts file",e);
		}
	}

	@Override
	public void writePageTempTs(final IPageComponent pageComponent) throws EngineException {
		PageComponent page = (PageComponent) pageComponent;
		try {
			if (page != null) {
				String pageName = page.getName();
				File pageDir = pageDir(page);

				String tsContent;
				if (page.isEnabled()) {
					File pageTsFile = new File(pageDir, pageName.toLowerCase() + ".ts");

					synchronized (writtenFiles) {
						if (writtenFiles.contains(pageTsFile)) {
							File pageTsFileTmp = toTmpFile(pageTsFile);
							if (pageTsFileTmp.exists()) {
								pageTsFile = pageTsFileTmp;
							}
						}
					}

					tsContent = FileUtils.readFileToString(pageTsFile, "UTF-8");
				} else {
					tsContent = getPageTsContent(page);
				}

				// Replace all Begin_c8o_function:XXX, End_c8o_function:XXX
				Pattern pattern = Pattern.compile("/\\*Begin_c8o_function:(.+)\\*/");
				Matcher matcher = pattern.matcher(tsContent);
				while (matcher.find()) {
					String markerId = matcher.group(1);
					String beginMarker = "/*Begin_c8o_function:" + markerId + "*/";
					String endMarker = "/*End_c8o_function:" + markerId + "*/";
					tsContent = tsContent.replace(beginMarker, "//---"+markerId+"---");
					tsContent = tsContent.replace(endMarker, "//---"+markerId+"---");
				}

				// Remove all CTSXXX
				int index = tsContent.indexOf("/*End_c8o_PageFunction*/");
				if (index != -1) {
					tsContent = tsContent.substring(0, index) + "/*End_c8o_PageFunction*/"
							+ System.lineSeparator() + "}";
				}

				// Write file if needed (do not need delay)
				tsContent = LsPattern.matcher(tsContent).replaceAll(System.lineSeparator());
				File tempTsFile = new File(pageDir, pageName.toLowerCase() + ".temp.ts");
				if (!tempTsFile.exists() || !tsContent.equals(FileUtils.readFileToString(tempTsFile, "UTF-8"))) {
					FileUtils.write(tempTsFile, tsContent, "UTF-8");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page temp ts file",e);
		}
	}

	@Override
	public void writeCompTempTs(final ISharedComponent compComponent) throws EngineException {
		UISharedComponent comp = (UISharedComponent) compComponent;
		try {
			if (comp != null) {
				File compDir = compDir(comp);

				String tsContent;
				boolean isEnabled = true;
				if (isEnabled) {
					File compTsFile = new File(compDir, compFileName(comp) + ".ts");

					synchronized (writtenFiles) {
						if (writtenFiles.contains(compTsFile)) {
							File compTsFileTmp = toTmpFile(compTsFile);
							if (compTsFileTmp.exists()) {
								compTsFile = compTsFileTmp;
							}
						}
					}

					tsContent = FileUtils.readFileToString(compTsFile, "UTF-8");
				} else {
					tsContent = getCompTsContent(comp);
				}

				// Replace all Begin_c8o_function:XXX, End_c8o_function:XXX
				Pattern pattern = Pattern.compile("/\\*Begin_c8o_function:(.+)\\*/");
				Matcher matcher = pattern.matcher(tsContent);
				while (matcher.find()) {
					String markerId = matcher.group(1);
					String beginMarker = "/*Begin_c8o_function:" + markerId + "*/";
					String endMarker = "/*End_c8o_function:" + markerId + "*/";
					tsContent = tsContent.replace(beginMarker, "//---"+markerId+"---");
					tsContent = tsContent.replace(endMarker, "//---"+markerId+"---");
				}

				// Remove all CTSXXX
				int index = tsContent.indexOf("/*End_c8o_CompFunction*/");
				if (index != -1) {
					tsContent = tsContent.substring(0, index) + "/*End_c8o_CompFunction*/"
							+ System.lineSeparator() + "}";
				}

				// Write file if needed (do not need delay)
				tsContent = LsPattern.matcher(tsContent).replaceAll(System.lineSeparator());
				File tempTsFile = new File(compDir, compFileName(comp) + ".temp.ts");
				if (!tempTsFile.exists() || !tsContent.equals(FileUtils.readFileToString(tempTsFile, "UTF-8"))) {
					FileUtils.write(tempTsFile, tsContent, "UTF-8");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic component temp ts file",e);
		}
	}

	private void writePageTs(PageComponent page) throws EngineException {
		try {
			if (page != null) {
				String pageName = page.getName();
				File pageDir = pageDir(page);
				File pageTsFile = new File(pageDir, pageName.toLowerCase() + ".ts");
				writeFile(pageTsFile, getPageTsContent(page, true), "UTF-8");

				File tempTsFile = new File(pageDir, pageName.toLowerCase() + ".temp.ts");
				if (tempTsFile.exists()) {
					writePageTempTs(page);
				}
				
				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") Ionic ts file generated for page '"+pageName+"'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page ts file",e);
		}
	}

	private void writeCompTs(UISharedComponent comp) throws EngineException {
		try {
			if (comp != null) {
				File compTsFile = new File(compDir(comp), compFileName(comp) + ".ts");
				writeFile(compTsFile, getCompTsContent(comp, true), "UTF-8");

				File tempTsFile = new File(compDir(comp), compFileName(comp) + ".temp.ts");
				if (tempTsFile.exists()) {
					writeCompTempTs((UISharedRegularComponent)comp);
				}
				
				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") Ionic ts file generated for component '"+comp.getName()+"'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page ts file",e);
		}
	}

	private void writePageRoutingTs(PageComponent page) throws EngineException {
		try {
			if (page != null) {
				boolean tplIsLowerThan8043 = page.compareToTplVersion("8.4.0.3") < 0;
				File pageDir = pageDir(page);
				String pageName = page.getName();

				String c8o_PageRoutingModuleName =  page.getName() + (tplIsLowerThan8043 ? "RoutingModule" : "Route");

				String c8o_PageImport = "import { "+pageName+" } from \"./"+pageName.toLowerCase()+"\";" + System.lineSeparator();

				String c8o_PageChildRoute = "";
				String c8o_PageChildRoutes = "";
				for (Contributor contributor : page.getContributors()) {
					for (String route: contributor.getModuleNgRoutes(page.getSegment())) {
						if (route.indexOf("redirectTo") == -1) {
							c8o_PageChildRoutes += c8o_PageChildRoutes.isEmpty() ? System.lineSeparator() : "";
							c8o_PageChildRoutes += "\t"+ route + "," + System.lineSeparator();
						} else {
							if (c8o_PageChildRoute.isEmpty()) {
								c8o_PageChildRoute += "\t"+ route + "," + System.lineSeparator();
							}
						}
					}
				}

				String c8o_PageRoutes = "";
				c8o_PageRoutes += "{ path: '', component: "+ pageName +", canActivate: [GuardsService], canDeactivate: [GuardsService], children: [";
				c8o_PageRoutes += c8o_PageChildRoutes;
				c8o_PageRoutes += c8o_PageChildRoute;
				c8o_PageRoutes += "]}," + System.lineSeparator();

				File pageRoutingTpl = new File(ionicTplDir, tplIsLowerThan8043 ? "src/page-routing.module.tpl": "src/page.routes.tpl");
				String mContent = FileUtils.readFileToString(pageRoutingTpl, "UTF-8");
				mContent = mContent.replaceAll("/\\*\\=c8o_PageImport\\*/", c8o_PageImport);
				mContent = mContent.replaceAll("/\\*\\=c8o_PageRoutes\\*/", c8o_PageRoutes);
				mContent = mContent.replaceAll("/\\*\\=c8o_PageRoutingModuleName\\*/", c8o_PageRoutingModuleName);

				File pageRoutingTsFile = new File(pageDir, pageName.toLowerCase() + (tplIsLowerThan8043 ? "-routing.module.ts" : ".routes.ts"));
				writeFile(pageRoutingTsFile, mContent, "UTF-8");


				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") Ionic routing module ts file generated for page '"+ page.getName()+"'");
				}

			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page routing ts file",e);
		}
	}

	private void writePageModuleTs(PageComponent page) throws EngineException {
		try {
			if (page != null) {
				if (page.compareToTplVersion("7.7.0.2") >= 0 && page.compareToTplVersion("8.4.0.3") < 0) {
					String pageName = page.getName();
					File pageDir = pageDir(page);
					File pageModuleTsFile = new File(pageDir, pageName.toLowerCase() + ".module.ts");
					writeFile(pageModuleTsFile, getPageModuleTsContent(page), "UTF-8");

					if (initDone) {
						Engine.logEngine.trace("("+ builderType +") Ionic module file generated for page '"+pageName+"'");
					}
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page module file",e);
		}
	}

	private void writeCompModuleTs(UISharedComponent comp) throws EngineException {
		try {
			if (comp != null) {
				if (comp.compareToTplVersion("8.4.0.3") < 0) {
					File compModuleTsFile = new File(compDir(comp), compFileName(comp) + ".module.ts");
					writeFile(compModuleTsFile, getCompModuleTsContent(comp), "UTF-8");
	
					if (initDone) {
						Engine.logEngine.trace("("+ builderType +") Ionic module file generated for component '"+comp.getName()+"'");
					}
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page module file",e);
		}
	}

	@Override
	protected Map<String,String> getTplAppCompTsImports() {
		if (tpl_appCompTsImports == null) {
			tpl_appCompTsImports = initTplImports(new File(ionicTplDir, "src/app/app.component.ts"));
		}
		return tpl_appCompTsImports;
	}
	
	protected Map<String,String> getTplMainTsImports() {
		if (tpl_appCompTsImports == null) {
			tpl_appCompTsImports = initTplImports(new File(ionicTplDir, "src/main.ts"));
		}
		return tpl_appCompTsImports;
	}

	@Override
	protected Map<String,String> getTplPageTsImports() {
		if (tpl_pageTsImports == null) {
			tpl_pageTsImports = initTplImports(new File(ionicTplDir, "src/page.tpl"));
		}
		return tpl_pageTsImports;
	}

	@Override
	protected Map<String,String> getTplCompTsImports() {
		if (tpl_compTsImports == null) {
			tpl_compTsImports = initTplImports(new File(ionicTplDir, "src/comp.tpl"));
		}
		return tpl_compTsImports;
	}

	private Map<String,String> getTplAppModuleTsImports() {
		if (tpl_appModuleTsImports == null) {
			tpl_appModuleTsImports = initTplImports(new File(ionicTplDir, "src/app/app.module.ts"));
		}
		return tpl_appModuleTsImports;
	}

	private Map<String,String> getTplPageModuleTsImports() {
		if (tpl_pageModuleTsImports == null) {
			tpl_pageModuleTsImports = initTplImports(new File(ionicTplDir, "src/page.module.tpl"));
		}
		return tpl_pageModuleTsImports;
	}

	private Map<String,String> getTplCompModuleTsImports() {
		if (tpl_compModuleTsImports == null) {
			tpl_compModuleTsImports = initTplImports(new File(ionicTplDir, "src/comp.module.tpl"));
		}
		return tpl_compModuleTsImports;
	}

	private String getTplAppModuleNgImports() {
		return this.getTplAppNgImports("src/app/app.module.ts");
	}

	private String getTplAppNgImports(String path) {
		if (tpl_appModuleNgImports == null) {
			try {
				String tsContent = FileUtils.readFileToString(new File(ionicTplDir, path), "UTF-8");
				tpl_appModuleNgImports = getMarker(tsContent, "NgModules")
						.replaceAll("/\\*Begin_c8o_NgModules\\*/","")
						.replaceAll("/\\*End_c8o_NgModules\\*/","")
						.replaceAll("\r\n", "").replaceAll("\n", "")
						.replaceAll("\t", "")
						.replaceAll("\\s", "");
			} catch (Exception e) {
				e.printStackTrace();
				tpl_appModuleNgImports = "";
			}
		}
		return tpl_appModuleNgImports;
	}

	private String getTplPageModuleNgImports() {
		if (tpl_pageModuleNgImports == null) {
			try {
				String tsContent = FileUtils.readFileToString(new File(ionicTplDir, "src/page.module.tpl"), "UTF-8");
				tpl_pageModuleNgImports = getMarker(tsContent, "NgModules")
						.replaceAll("/\\*Begin_c8o_NgModules\\*/","")
						.replaceAll("/\\*End_c8o_NgModules\\*/","")
						.replaceAll("\r\n", "").replaceAll("\n", "")
						.replaceAll("\t", "")
						.replaceAll("\\s", "");
			} catch (Exception e) {
				e.printStackTrace();
				tpl_pageModuleNgImports = "";
			}
		}
		return tpl_pageModuleNgImports;
	}

	private String getTplCompModuleNgImports() {
		if (tpl_compModuleNgImports == null) {
			try {
				String tsContent = FileUtils.readFileToString(new File(ionicTplDir, "src/comp.module.tpl"), "UTF-8");
				tpl_compModuleNgImports = getMarker(tsContent, "NgModules")
						.replaceAll("/\\*Begin_c8o_NgModules\\*/","")
						.replaceAll("/\\*End_c8o_NgModules\\*/","")
						.replaceAll("\r\n", "").replaceAll("\n", "")
						.replaceAll("\t", "")
						.replaceAll("\\s", "");
			} catch (Exception e) {
				e.printStackTrace();
				tpl_compModuleNgImports = "";
			}
		}
		return tpl_compModuleNgImports;
	}
	
	private String getTplAppModuleNgProviders() {
		return this.getTplAppNgProviders("src/app/app.module.ts");
	}
	
	private String getTplAppNgProviders(String path) {
		if (tpl_appModuleNgProviders == null) {
			try {
				String tsContent = FileUtils.readFileToString(new File(ionicTplDir, path), "UTF-8");
				tpl_appModuleNgProviders = getMarker(tsContent, "NgProviders")
						.replaceAll("/\\*Begin_c8o_NgProviders\\*/","")
						.replaceAll("/\\*End_c8o_NgProviders\\*/","")
						.replaceAll("\r\n", "").replaceAll("\n", "")
						.replaceAll("\t", "")
						.replaceAll("\\s", "");
			} catch (Exception e) {
				e.printStackTrace();
				tpl_appModuleNgProviders = "";
			}
		}
		return tpl_appModuleNgProviders;
	}

	private String getTplPageModuleNgProviders() {
		if (tpl_pageModuleNgProviders == null) {
			try {
				String tsContent = FileUtils.readFileToString(new File(ionicTplDir, "src/page.module.tpl"), "UTF-8");
				tpl_pageModuleNgProviders = getMarker(tsContent, "NgProviders")
						.replaceAll("/\\*Begin_c8o_NgProviders\\*/","")
						.replaceAll("/\\*End_c8o_NgProviders\\*/","")
						.replaceAll("\r\n", "").replaceAll("\n", "")
						.replaceAll("\t", "")
						.replaceAll("\\s", "");
			} catch (Exception e) {
				e.printStackTrace();
				tpl_pageModuleNgProviders = "";
			}
		}
		return tpl_pageModuleNgProviders;
	}

	private String getTplCompModuleNgProviders() {
		if (tpl_compModuleNgProviders == null) {
			try {
				String tsContent = FileUtils.readFileToString(new File(ionicTplDir, "src/comp.module.tpl"), "UTF-8");
				tpl_compModuleNgProviders = getMarker(tsContent, "NgProviders")
						.replaceAll("/\\*Begin_c8o_NgProviders\\*/","")
						.replaceAll("/\\*End_c8o_NgProviders\\*/","")
						.replaceAll("\r\n", "").replaceAll("\n", "")
						.replaceAll("\t", "")
						.replaceAll("\\s", "");
			} catch (Exception e) {
				e.printStackTrace();
				tpl_compModuleNgProviders = "";
			}
		}
		return tpl_compModuleNgProviders;
	}

	private String getTplAppModuleNgDeclarations() {
		if (tpl_appModuleNgDeclarations == null) {
			try {
				String tsContent = FileUtils.readFileToString(new File(ionicTplDir, "src/app/app.module.ts"), "UTF-8");
				tpl_appModuleNgDeclarations = getMarker(tsContent, "NgDeclarations")
						.replaceAll("/\\*Begin_c8o_NgDeclarations\\*/","")
						.replaceAll("/\\*End_c8o_NgDeclarations\\*/","")
						.replaceAll("\r\n", "").replaceAll("\n", "")
						.replaceAll("\t", "")
						.replaceAll("\\s", "");
			} catch (Exception e) {
				e.printStackTrace();
				tpl_appModuleNgDeclarations = "";
			}
		}
		return tpl_appModuleNgDeclarations;
	}

	private String getTplPageModuleNgDeclarations() {
		if (tpl_pageModuleNgDeclarations == null) {
			try {
				String tsContent = FileUtils.readFileToString(new File(ionicTplDir, "src/page.module.tpl"), "UTF-8");
				tpl_pageModuleNgDeclarations = getMarker(tsContent, "NgDeclarations")
						.replaceAll("/\\*Begin_c8o_NgDeclarations\\*/","")
						.replaceAll("/\\*End_c8o_NgDeclarations\\*/","")
						.replaceAll("\r\n", "").replaceAll("\n", "")
						.replaceAll("\t", "")
						.replaceAll("\\s", "");
			} catch (Exception e) {
				e.printStackTrace();
				tpl_pageModuleNgDeclarations = "";
			}
		}
		return tpl_pageModuleNgDeclarations;
	}

	private String getTplCompModuleNgDeclarations() {
		if (tpl_compModuleNgDeclarations == null) {
			try {
				String tsContent = FileUtils.readFileToString(new File(ionicTplDir, "src/comp.module.tpl"), "UTF-8");
				tpl_compModuleNgDeclarations = getMarker(tsContent, "NgDeclarations")
						.replaceAll("/\\*Begin_c8o_NgDeclarations\\*/","")
						.replaceAll("/\\*End_c8o_NgDeclarations\\*/","")
						.replaceAll("\r\n", "").replaceAll("\n", "")
						.replaceAll("\t", "")
						.replaceAll("\\s", "");
			} catch (Exception e) {
				e.printStackTrace();
				tpl_compModuleNgDeclarations = "";
			}
		}
		return tpl_compModuleNgDeclarations;
	}

	private String getTplAppModuleNgComponents() {
		if (tpl_appModuleNgComponents == null) {
			try {
				String tsContent = FileUtils.readFileToString(new File(ionicTplDir, "src/app/app.module.ts"), "UTF-8");
				tpl_appModuleNgComponents = getMarker(tsContent, "NgComponents")
						.replaceAll("/\\*Begin_c8o_NgComponents\\*/","")
						.replaceAll("/\\*End_c8o_NgComponents\\*/","")
						.replaceAll("\r\n", "").replaceAll("\n", "")
						.replaceAll("\t", "")
						.replaceAll("\\s", "");
			} catch (Exception e) {
				e.printStackTrace();
				tpl_appModuleNgComponents = "";
			}
		}
		return tpl_appModuleNgComponents;
	}

	private String getTplPageModuleNgComponents() {
		if (tpl_pageModuleNgComponents == null) {
			try {
				String tsContent = FileUtils.readFileToString(new File(ionicTplDir, "src/page.module.tpl"), "UTF-8");
				tpl_pageModuleNgComponents = getMarker(tsContent, "NgComponents")
						.replaceAll("/\\*Begin_c8o_NgComponents\\*/","")
						.replaceAll("/\\*End_c8o_NgComponents\\*/","")
						.replaceAll("\r\n", "").replaceAll("\n", "")
						.replaceAll("\t", "")
						.replaceAll("\\s", "");
			} catch (Exception e) {
				e.printStackTrace();
				tpl_pageModuleNgComponents = "";
			}
		}
		return tpl_pageModuleNgComponents;
	}

	private String getTplCompModuleNgComponents() {
		if (tpl_compModuleNgComponents == null) {
			try {
				String tsContent = FileUtils.readFileToString(new File(ionicTplDir, "src/comp.module.tpl"), "UTF-8");
				tpl_compModuleNgComponents = getMarker(tsContent, "NgComponents")
						.replaceAll("/\\*Begin_c8o_NgComponents\\*/","")
						.replaceAll("/\\*End_c8o_NgComponents\\*/","")
						.replaceAll("\r\n", "").replaceAll("\n", "")
						.replaceAll("\t", "")
						.replaceAll("\\s", "");
			} catch (Exception e) {
				e.printStackTrace();
				tpl_compModuleNgComponents = "";
			}
		}
		return tpl_compModuleNgComponents;
	}

	private Map<String,String> getTplServiceActionTsImports() {
		if (tpl_serviceActionTsImports == null) {
			tpl_serviceActionTsImports = initTplImports(new File(ionicTplDir, "src/app/services/actionbeans.service.ts"));
		}
		return tpl_serviceActionTsImports;
	}

	private String getPageTsContent(PageComponent page) throws IOException {
		return getPageTsContent(page, false);
	}
	
	private String getPageTsContent(PageComponent page, boolean checkEnable) throws IOException {
		String pageName = page.getName();
		String c8o_PageName = pageName;
		String c8o_PageHistory = page.getDefaultHistory();
		String c8o_PagePriority = page.getPreloadPriority();
		String c8o_PageSegment = page.getSegment();
		String c8o_PageChangeDetection = page.getChangeDetectionStrategy();
		String c8o_PageTplUrl = pageName.toLowerCase() + ".html";
		String c8o_PageStyleUrls = pageName.toLowerCase() + ".scss";
		String c8o_PageSelector = "page-"+pageName.toLowerCase();
		String c8o_PageImports = page.getComputedImports();
		String c8o_PageDeclarations = page.getComputedDeclarations();
		String c8o_PageConstructors = page.getComputedConstructors();
		String c8o_PageFunctions = page.getComputedFunctions();
		
		boolean tplIsLowerThan8043 = page.compareToTplVersion("8.4.0.3") < 0;
		
		String c8o_ModuleTsImports = "";
		String c8o_ModuleNgImports = "";
		String c8o_ModuleNgProviders = "";
		if(!tplIsLowerThan8043) {
			Map<String, String> module_ts_imports = new HashMap<>();
			Set<String> module_ng_imports =  new HashSet<String>();
			Set<String> module_ng_providers =  new HashSet<String>();
			
			List<Contributor> contributors = page.getContributors();
			for (Contributor contributor : contributors) {
				contributor.forContainer(page, () -> {
					module_ts_imports.putAll(contributor.getModuleTsImports());
					module_ng_imports.addAll(contributor.getModuleNgImports());
					module_ng_providers.addAll(contributor.getModuleNgProviders());
				});
			}
			// fix for BrowserAnimationsModule until it will be handled in config
			module_ts_imports.remove("BrowserAnimationsModule");
			module_ng_imports.remove("BrowserAnimationsModule");
			
			HashMap<String, String> mapRemovedModule = new HashMap<>();
			
			Map<String, String> tpl_ts_imports = getTplPageModuleTsImports();
			if (!module_ts_imports.isEmpty()) {
				for (String comp : module_ts_imports.keySet()) {
					if (!tpl_ts_imports.containsKey(comp)) {
						String from = module_ts_imports.get(comp);
						String compM = comp;
						String fromM = from;
						if(from.contains("components/")) {
							compM = comp.replaceAll("Module", "");
							fromM = from.replaceAll("Module", "").replaceAll(".module", "");
							mapRemovedModule.put(comp, compM);
						}
						if(from.contains("../") && !from.contains("../components/")) {
							compM = comp.replaceAll("Module", "");
							fromM = from.replaceAll("Module", "").replaceAll(".module", "");
							mapRemovedModule.put(comp, compM);
						}
						String pattern = "\\{\\s*" + Pattern.quote(compM) + "\\s*\\}";
						Pattern compiledPattern = Pattern.compile(pattern);
				        Matcher matcher = compiledPattern.matcher(c8o_PageImports);
				        Matcher matcher2 = compiledPattern.matcher(c8o_ModuleTsImports);
				        
						if (!matcher2.find() && !matcher.find()) {
							String[] parted = fromM.split("__c8o_separator__");
				        	fromM = parted[0];
							String directImport = parted.length > 1 ? parted[1] : "false";
							if (comp.indexOf(" as ") != -1 || "true".equalsIgnoreCase(directImport)) {
								c8o_ModuleTsImports += "import "+compM+" from '"+ fromM +"';"+ System.lineSeparator();
							} else {
								fromM = (fromM.startsWith("../components/") ? "../":"") + fromM;
								c8o_ModuleTsImports += "import { "+compM+" } from '"+ fromM +"';"+ System.lineSeparator();
							}
						}
					}
				}
			}
			String tpl_ng_imports = getTplPageModuleNgImports();
			if (!module_ng_imports.isEmpty()) {
				for (String module: module_ng_imports) {
					String moduleM = mapRemovedModule.get(module);
					moduleM = moduleM != null ? moduleM : module;
					if (!tpl_ng_imports.contains(moduleM)) {
						
						c8o_ModuleNgImports += "\t" + moduleM + "," + System.lineSeparator();
					}
				}
				if (!c8o_ModuleNgImports.isEmpty()) {
					c8o_ModuleNgImports = System.lineSeparator() + c8o_ModuleNgImports + System.lineSeparator();
				}
			}
			String tpl_ng_providers = getTplPageModuleNgProviders();
			if (!module_ng_providers.isEmpty()) {
				for (String provider: module_ng_providers) {
					if (!tpl_ng_providers.contains(provider)) {
						c8o_ModuleNgProviders += "\t" + provider + "," + System.lineSeparator();
					}
				}
				if (!c8o_ModuleNgProviders.isEmpty()) {
					c8o_ModuleNgProviders = System.lineSeparator() + c8o_ModuleNgProviders;
				}
			}
		}	
		
		String scriptcontentstring = page.getScriptContent().getString();
		String c8o_UserCustoms = checkEnable ? (page.isEnabled() ? scriptcontentstring : "") : scriptcontentstring;

		File pageTplTs = new File(ionicTplDir, "src/page.tpl");
		String tsContent = FileUtils.readFileToString(pageTplTs, "UTF-8");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PagePriority\\*/","'"+c8o_PagePriority+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageSegment\\*/","'"+c8o_PageSegment+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageHistory\\*/",c8o_PageHistory);

		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageSelector\\*/","'"+c8o_PageSelector+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageTplUrl\\*/","'"+c8o_PageTplUrl+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageStyleUrls\\*/","'"+c8o_PageStyleUrls+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageChangeDetection\\*/",c8o_PageChangeDetection);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageName\\*/",c8o_PageName);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageImports\\*/",c8o_PageImports + (tplIsLowerThan8043 ? "" : c8o_ModuleTsImports));
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageDeclarations\\*/",c8o_PageDeclarations);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageConstructors\\*/",c8o_PageConstructors);
		if(!tplIsLowerThan8043) {
			tsContent = tsContent.replaceAll("/\\*c8o_StandAloneNgModules\\*/", c8o_ModuleNgImports);
//			tsContent = tsContent.replaceAll("/\\*Begin_c8o_NgProviders\\*/",c8o_ModuleNgProviders);
//			tsContent = tsContent.replaceAll("/\\*End_c8o_NgProviders\\*/","");
		}

		Pattern pattern = Pattern.compile("/\\*Begin_c8o_(.+)\\*/"); // begin c8o marker
		Matcher matcher = pattern.matcher(tsContent);
		while (matcher.find()) {
			String markerId = matcher.group(1);
			String tplMarker = getMarker(tsContent, markerId);
			String customMarker = getMarker(c8o_UserCustoms, markerId);
			if (!customMarker.isEmpty()) {
				tsContent = tsContent.replace(tplMarker, customMarker);
			}
		}

		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageFunctions\\*/", Matcher.quoteReplacement(c8o_PageFunctions));

		return tsContent;
	}
	
	private String getCompTsContent(UISharedComponent comp) throws IOException {
		return getCompTsContent(comp, false);
	}

	private String getCompTsContent(UISharedComponent comp, boolean checkEnable) throws IOException {
		String c8o_CompName = compName(comp);
		String c8o_CompChangeDetection = "ChangeDetectionStrategy.Default"; //comp.getChangeDetectionStrategy();
		String c8o_CompTplUrl = compFileName(comp) + ".html";
		String c8o_CompStyleUrls = compFileName(comp) + ".scss";
		String c8o_CompSelector = comp.getSelector();
		String c8o_CompImports = comp.getComputedImports();
		String c8o_CompInterfaces = comp.getComputedInterfaces();
		String c8o_CompDeclarations = comp.getComputedDeclarations();
		String c8o_CompConstructors = comp.getComputedConstructors();
		String c8o_CompInitializations = comp.getComputedInitializations();
		String c8o_CompFinallizations = comp.getComputedDispositions();
		String c8o_CompFunctions = comp.getComputedFunctions();
		
		
		String scriptcontentstring = comp.getScriptContent().getString();
		String c8o_UserCustoms = checkEnable ? (comp.isEnabled() ? scriptcontentstring : "") : scriptcontentstring;
		
		boolean tplIsLowerThan8043 = comp.compareToTplVersion("8.4.0.3") < 0;
		String c8o_ModuleTsImports = "";
		String c8o_ModuleNgImports = "";
		String c8o_ModuleNgProviders = "";
		if(!tplIsLowerThan8043) {
			Map<String, String> module_ts_imports = new HashMap<>();
			Set<String> module_ng_imports =  new HashSet<String>();
			Set<String> module_ng_providers =  new HashSet<String>();
			
			List<Contributor> contributors = comp.getContributors();
			for (Contributor contributor : contributors) {
				contributor.forContainer(comp, () -> {
					module_ts_imports.putAll(contributor.getModuleTsImports());
					module_ng_imports.addAll(contributor.getModuleNgImports());
					module_ng_providers.addAll(contributor.getModuleNgProviders());
				});
			}
			// fix for BrowserAnimationsModule until it will be handled in config
			module_ts_imports.remove("BrowserAnimationsModule");
			module_ng_imports.remove("BrowserAnimationsModule");
			
			
			HashMap<String, String> mapRemovedModule = new HashMap<>();
			
			Map<String, String> tpl_ts_imports = getTplPageModuleTsImports();
			if (!module_ts_imports.isEmpty()) {
				for (String comps : module_ts_imports.keySet()) {
					if (!tpl_ts_imports.containsKey(comps)) {
						String from = module_ts_imports.get(comps);
						String compM = comps;
						String fromM = from;
						if(from.contains("components/") || from.contains("pages/") || from.contains("../")) {
							compM = comps.replaceAll("Module", "");
							fromM = from.replaceAll("Module", "").replaceAll(".module", "").replaceAll("../components", "..");
							mapRemovedModule.put(comps, compM);
						}
						String pattern = "\\{\\s*" + Pattern.quote(compM) + "\\s*\\}";
						Pattern compiledPattern = Pattern.compile(pattern);
						Matcher matcher = compiledPattern.matcher(c8o_CompImports);
				        Matcher matcher2 = compiledPattern.matcher(c8o_ModuleTsImports);
				        
						if (!matcher.find() && !matcher2.find()) {
							String[] parted = fromM.split("__c8o_separator__");
				        	fromM = parted[0];
							String directImport = parted.length > 1 ? parted[1] : "false";
							if (comps.indexOf(" as ") != -1 || "true".equalsIgnoreCase(directImport)) {
								c8o_ModuleTsImports += "import "+compM+" from '"+ fromM +"';"+ System.lineSeparator();
							} else {
								//from = (from.startsWith("../components/") ? "../":"") + from;
								c8o_ModuleTsImports += "import { "+compM+" } from '"+ fromM +"';"+ System.lineSeparator();
							}
						}
					}
				}
			}
			
			String tpl_ng_imports = getTplPageModuleNgImports();
			if (!module_ng_imports.isEmpty()) {
				for (String module: module_ng_imports) {
					if (!tpl_ng_imports.contains(module)) {
						String moduleM = mapRemovedModule.get(module);
						moduleM = moduleM != null ? moduleM : module;
						c8o_ModuleNgImports += "\t" + moduleM + "," + System.lineSeparator();
					}
				}
				if (!c8o_ModuleNgImports.isEmpty()) {
					c8o_ModuleNgImports = System.lineSeparator() + c8o_ModuleNgImports + System.lineSeparator();
				}
			}
			
			String tpl_ng_providers = getTplPageModuleNgProviders();
			if (!module_ng_providers.isEmpty()) {
				for (String provider: module_ng_providers) {
					if (!tpl_ng_providers.contains(provider)) {
						c8o_ModuleNgProviders += "\t" + provider + "," + System.lineSeparator();
					}
				}
				if (!c8o_ModuleNgProviders.isEmpty()) {
					c8o_ModuleNgProviders = System.lineSeparator() + c8o_ModuleNgProviders;
				}
			}
		}

		File compTplTs = new File(ionicTplDir, "src/comp.tpl");
		String tsContent = FileUtils.readFileToString(compTplTs, "UTF-8");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompSelector\\*/","'"+c8o_CompSelector+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompTplUrl\\*/","'"+c8o_CompTplUrl+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompStyleUrls\\*/","'"+c8o_CompStyleUrls+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompChangeDetection\\*/",c8o_CompChangeDetection);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompName\\*/",c8o_CompName);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompImports\\*/",c8o_CompImports + (tplIsLowerThan8043 ? "" : c8o_ModuleTsImports));
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompInterfaces\\*/",c8o_CompInterfaces);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompDeclarations\\*/",c8o_CompDeclarations);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompConstructors\\*/",c8o_CompConstructors);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompInitializations\\*/",c8o_CompInitializations);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompFinallizations\\*/",c8o_CompFinallizations);
		if(!tplIsLowerThan8043) {
			tsContent = tsContent.replaceAll("/\\*c8o_StandAloneNgModules\\*/",c8o_ModuleNgImports);
//			tsContent = tsContent.replaceAll("/\\*Begin_c8o_NgProviders\\*/",c8o_ModuleNgProviders);
//			tsContent = tsContent.replaceAll("/\\*End_c8o_NgProviders\\*/","");
		}
		
		Pattern pattern = Pattern.compile("/\\*Begin_c8o_(.+)\\*/"); // begin c8o marker
		Matcher matcher = pattern.matcher(tsContent);
		while (matcher.find()) {
			String markerId = matcher.group(1);
			String tplMarker = getMarker(tsContent, markerId);
			String customMarker = getMarker(c8o_UserCustoms, markerId);
			if (!customMarker.isEmpty()) {
				tsContent = tsContent.replace(tplMarker, customMarker);
			}
		}

		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompFunctions\\*/", Matcher.quoteReplacement(c8o_CompFunctions));
		
		if(!tplIsLowerThan8043) {
			createCompBeansDirFiles(comp);
		}
		
		return tsContent;
	}

	private String getPageModuleTsContent(PageComponent page) throws IOException {
		// contributors
		Map<String, File> comp_beans_dirs = new HashMap<>();
		Map<String, String> module_ts_imports = new HashMap<>();
		Set<String> module_ng_imports =  new HashSet<String>();
		Set<String> module_ng_providers =  new HashSet<String>();
		Set<String> module_ng_declarations =  new HashSet<String>();
		Set<String> module_ng_components =  new HashSet<String>();

		List<Contributor> contributors = page.getContributors();
		for (Contributor contributor : contributors) {
			contributor.forContainer(page, () -> {
				comp_beans_dirs.putAll(contributor.getCompBeanDir());
				module_ts_imports.putAll(contributor.getModuleTsImports());
				module_ng_imports.addAll(contributor.getModuleNgImports());
				module_ng_providers.addAll(contributor.getModuleNgProviders());
				module_ng_declarations.addAll(contributor.getModuleNgDeclarations());
				module_ng_components.addAll(contributor.getModuleNgComponents());
			});
		}
		// fix for BrowserAnimationsModule until it will be handled in config
		module_ts_imports.remove("BrowserAnimationsModule");
		module_ng_imports.remove("BrowserAnimationsModule");

		String c8o_ModuleTsImports = "";
		Map<String, String> tpl_ts_imports = getTplPageModuleTsImports();
		if (!module_ts_imports.isEmpty()) {
			for (String comp : module_ts_imports.keySet()) {
				if (!tpl_ts_imports.containsKey(comp)) {
					String from = module_ts_imports.get(comp);
					if (comp.indexOf(" as ") != -1) {
						c8o_ModuleTsImports += "import "+comp+" from '"+ from +"';"+ System.lineSeparator();
					} else {
						from = (from.startsWith("../components/") ? "../":"") + from;
						c8o_ModuleTsImports += "import { "+comp+" } from '"+ from +"';"+ System.lineSeparator();
					}
				}
			}
		}

		String c8o_ModuleNgImports = "";
		String tpl_ng_imports = getTplPageModuleNgImports();
		if (!module_ng_imports.isEmpty()) {
			for (String module: module_ng_imports) {
				if (!tpl_ng_imports.contains(module)) {
					c8o_ModuleNgImports += "\t" + module + "," + System.lineSeparator();
				}
			}
			if (!c8o_ModuleNgImports.isEmpty()) {
				c8o_ModuleNgImports = System.lineSeparator() + c8o_ModuleNgImports + System.lineSeparator();
			}
		}

		String c8o_ModuleNgProviders = "";
		String tpl_ng_providers = getTplPageModuleNgProviders();
		if (!module_ng_providers.isEmpty()) {
			for (String provider: module_ng_providers) {
				if (!tpl_ng_providers.contains(provider)) {
					c8o_ModuleNgProviders += "\t" + provider + "," + System.lineSeparator();
				}
			}
			if (!c8o_ModuleNgProviders.isEmpty()) {
				c8o_ModuleNgProviders = System.lineSeparator() + c8o_ModuleNgProviders;
			}
		}

		String c8o_ModuleNgDeclarations = "";
		String tpl_ng_declarations = getTplPageModuleNgDeclarations();
		if (!module_ng_declarations.isEmpty()) {
			for (String declaration: module_ng_declarations) {
				if (!tpl_ng_declarations.contains(declaration)) {
					c8o_ModuleNgDeclarations += "\t" + declaration + "," + System.lineSeparator();
				}
			}
			if (!c8o_ModuleNgDeclarations.isEmpty()) {
				c8o_ModuleNgDeclarations = System.lineSeparator() + c8o_ModuleNgDeclarations;
			}
		}

		String c8o_ModuleNgComponents = "";
		String tpl_ng_components = getTplPageModuleNgComponents();
		if (!module_ng_components.isEmpty()) {
			for (String component: module_ng_components) {
				if (!tpl_ng_components.contains(component)) {
					c8o_ModuleNgComponents += "\t" + component + "," + System.lineSeparator();
				}
			}
			if (!c8o_ModuleNgComponents.isEmpty()) {
				c8o_ModuleNgComponents = System.lineSeparator() + c8o_ModuleNgComponents;
			}
		}

		String pageName = page.getName();
		String c8o_PageName = pageName;
		String c8o_PageModuleName = pageName + "Module";
		String c8o_PageRoutingModuleName = pageName + "RoutingModule";
		String c8o_PageImport = "";

		c8o_PageImport += "import { "+pageName+" } from \"./"+pageName.toLowerCase()+"\";" + System.lineSeparator();
		c8o_PageImport += "import { "+pageName+"RoutingModule } from \"./"+pageName.toLowerCase()+"-routing.module\";" + System.lineSeparator();

		File pageTplTs = new File(ionicTplDir, "src/page.module.tpl");
		String tsContent = FileUtils.readFileToString(pageTplTs, "UTF-8");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageName\\*/",c8o_PageName);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageModuleName\\*/",c8o_PageModuleName);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageRoutingModuleName\\*/",c8o_PageRoutingModuleName);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageImport\\*/",c8o_PageImport);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_ModuleTsImports\\*/",c8o_ModuleTsImports);
		tsContent = tsContent.replaceAll("/\\*Begin_c8o_NgModules\\*/","");
		tsContent = tsContent.replaceAll("/\\*End_c8o_NgModules\\*/",c8o_ModuleNgImports);
		tsContent = tsContent.replaceAll("/\\*Begin_c8o_NgProviders\\*/",c8o_ModuleNgProviders);
		tsContent = tsContent.replaceAll("/\\*End_c8o_NgProviders\\*/","");
		tsContent = tsContent.replaceAll("/\\*Begin_c8o_NgDeclarations\\*/",c8o_ModuleNgDeclarations);
		tsContent = tsContent.replaceAll("/\\*End_c8o_NgDeclarations\\*/","");
		tsContent = tsContent.replaceAll("/\\*Begin_c8o_NgComponents\\*/",c8o_ModuleNgComponents);
		tsContent = tsContent.replaceAll("/\\*End_c8o_NgComponents\\*/","");

		for (String compbean : comp_beans_dirs.keySet()) {
			try {
				File srcCompDir = comp_beans_dirs.get(compbean);
				File destCompDir = new File(componentsDir, compbean);
				Matcher m = Pattern.compile("file:(/.*?)!/(.*)").matcher(srcCompDir.getPath().replace('\\', '/'));
				if (m.matches()) {
					ZipUtils.expandZip(m.group(1), destCompDir.getAbsolutePath(), m.group(2));
				} else {
					for (File f: srcCompDir.listFiles()) {
						String fContent = FileUtils.readFileToString(f, "UTF-8");
						File destFile = new File(componentsDir, compbean+ "/"+ f.getName());
						writeFile(destFile, fContent, "UTF-8");
					}
				}
			} catch (Exception e) {
				Engine.logEngine.warn("("+ builderType +") Missing component folder for pseudo-bean '"+ compbean +"' !");
			}
		}
		return tsContent;		
	}

	private String getCompModuleTsContent(UISharedComponent comp) throws IOException {
		// contributors
		Map<String, File> comp_beans_dirs = new HashMap<>();
		Map<String, String> module_ts_imports = new HashMap<>();
		Set<String> module_ng_imports =  new HashSet<String>();
		Set<String> module_ng_providers =  new HashSet<String>();
		Set<String> module_ng_declarations =  new HashSet<String>();
		Set<String> module_ng_components =  new HashSet<String>();

		if (comp.isEnabled()) {
		List<Contributor> contributors = comp.getContributors();
		for (Contributor contributor : contributors) {
			contributor.forContainer(comp, () -> {
				comp_beans_dirs.putAll(contributor.getCompBeanDir());
				module_ts_imports.putAll(contributor.getModuleTsImports());
				module_ng_imports.addAll(contributor.getModuleNgImports());
				module_ng_providers.addAll(contributor.getModuleNgProviders());
				module_ng_declarations.addAll(contributor.getModuleNgDeclarations());
				module_ng_components.addAll(contributor.getModuleNgComponents());
			});
		}
		}
		
		// fix for BrowserAnimationsModule until it will be handled in config
		module_ts_imports.remove("BrowserAnimationsModule");
		module_ng_imports.remove("BrowserAnimationsModule");

		String c8o_ModuleTsImports = "";
		Map<String, String> tpl_ts_imports = getTplCompModuleTsImports();
		if (!module_ts_imports.isEmpty()) {
			for (String compo : module_ts_imports.keySet()) {
				if (!tpl_ts_imports.containsKey(compo)) {
					String from = module_ts_imports.get(compo);
					if (compo.indexOf(" as ") != -1) {
						c8o_ModuleTsImports += "import "+compo+" from '"+ from +"';"+ System.lineSeparator();
					} else {
						from = (from.startsWith("../components/") ? "../":"") + from;
						c8o_ModuleTsImports += "import { "+compo+" } from '"+ from +"';"+ System.lineSeparator();
					}
				}
			}
		}

		String c8o_ModuleNgImports = "";
		String tpl_ng_imports = getTplCompModuleNgImports();
		if (!module_ng_imports.isEmpty()) {
			for (String module: module_ng_imports) {
				if (!tpl_ng_imports.contains(module)) {
					try {
						module = module.substring(0, module.indexOf("."));
					} catch (Exception e) {}
					c8o_ModuleNgImports += "\t" + module + "," + System.lineSeparator();
				}
			}
			if (!c8o_ModuleNgImports.isEmpty()) {
				c8o_ModuleNgImports = System.lineSeparator() + c8o_ModuleNgImports + System.lineSeparator();
			}
		}

		String c8o_ModuleNgExports = c8o_ModuleNgImports.isEmpty() ? "" : c8o_ModuleNgImports;

		String c8o_ModuleNgProviders = "";
		String tpl_ng_providers = getTplCompModuleNgProviders();
		if (!module_ng_providers.isEmpty()) {
			for (String provider: module_ng_providers) {
				if (!tpl_ng_providers.contains(provider)) {
					c8o_ModuleNgProviders += "\t" + provider + "," + System.lineSeparator();
				}
			}
			if (!c8o_ModuleNgProviders.isEmpty()) {
				c8o_ModuleNgProviders = System.lineSeparator() + c8o_ModuleNgProviders;
			}
		}

		String c8o_ModuleNgDeclarations = "";
		String tpl_ng_declarations = getTplCompModuleNgDeclarations();
		if (!module_ng_declarations.isEmpty()) {
			for (String declaration: module_ng_declarations) {
				if (!tpl_ng_declarations.contains(declaration)) {
					c8o_ModuleNgDeclarations += "\t" + declaration + "," + System.lineSeparator();
				}
			}
			if (!c8o_ModuleNgDeclarations.isEmpty()) {
				c8o_ModuleNgDeclarations = System.lineSeparator() + c8o_ModuleNgDeclarations;
			}
		}

		String c8o_ModuleNgComponents = "";
		String tpl_ng_components = getTplCompModuleNgComponents();
		if (!module_ng_components.isEmpty()) {
			for (String component: module_ng_components) {
				if (!tpl_ng_components.contains(component)) {
					c8o_ModuleNgComponents += "\t" + component + "," + System.lineSeparator();
				}
			}
			if (!c8o_ModuleNgComponents.isEmpty()) {
				c8o_ModuleNgComponents = System.lineSeparator() + c8o_ModuleNgComponents;
			}
		}

		String c8o_CompName = compName(comp);
		String c8o_CompModuleName = compName(comp) + "Module";
		String c8o_CompImport = "";

		c8o_CompImport += "import { "+compName(comp)+" } from \"./"+compFileName(comp)+"\";" + System.lineSeparator();

		File pageTplTs = new File(ionicTplDir, "src/comp.module.tpl");
		String tsContent = FileUtils.readFileToString(pageTplTs, "UTF-8");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompName\\*/",c8o_CompName);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompModuleName\\*/",c8o_CompModuleName);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompImport\\*/",c8o_CompImport);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_ModuleTsImports\\*/",c8o_ModuleTsImports);
		tsContent = tsContent.replaceAll("/\\*Begin_c8o_NgModules\\*/","");
		tsContent = tsContent.replaceAll("/\\*End_c8o_NgModules\\*/",c8o_ModuleNgImports);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_ModuleNgExports\\*/", c8o_ModuleNgExports);
		tsContent = tsContent.replaceAll("/\\*Begin_c8o_NgProviders\\*/",c8o_ModuleNgProviders);
		tsContent = tsContent.replaceAll("/\\*End_c8o_NgProviders\\*/","");
		tsContent = tsContent.replaceAll("/\\*Begin_c8o_NgDeclarations\\*/",c8o_ModuleNgDeclarations);
		tsContent = tsContent.replaceAll("/\\*End_c8o_NgDeclarations\\*/","");
		tsContent = tsContent.replaceAll("/\\*Begin_c8o_NgComponents\\*/",c8o_ModuleNgComponents);
		tsContent = tsContent.replaceAll("/\\*End_c8o_NgComponents\\*/","");

		this.createCompBeansDirFiles(comp_beans_dirs);

		return tsContent;
	}
	
	private void createCompBeansDirFiles(UISharedComponent comp) throws IOException {
		// contributors
		Map<String, File> comp_beans_dirs = new HashMap<>();

		if (comp.isEnabled()) {
			List<Contributor> contributors = comp.getContributors();
			for (Contributor contributor : contributors) {
				contributor.forContainer(comp, () -> {
					comp_beans_dirs.putAll(contributor.getCompBeanDir());
				});
			}
		}
		this.createCompBeansDirFiles(comp_beans_dirs);
	}
	
	private void createCompBeansDirFiles(Map<String, File> comp_beans_dirs) throws IOException {
		for (String compbean : comp_beans_dirs.keySet()) {
			File srcCompDir = comp_beans_dirs.get(compbean);
			File destCompDir = new File(componentsDir, compbean);
			Matcher m = Pattern.compile("file:(/.*?)!/(.*)").matcher(srcCompDir.getPath().replace('\\', '/'));
			if (m.matches()) {
				ZipUtils.expandZip(m.group(1), destCompDir.getAbsolutePath(), m.group(2));
			} else {
				for (File f: srcCompDir.listFiles()) {
					String fContent = FileUtils.readFileToString(f, "UTF-8");
					File destFile = new File(componentsDir, compbean+ "/"+ f.getName());
					writeFile(destFile, fContent, "UTF-8");
				}
			}
		}
	}

	@Override
	public String getTempTsRelativePath(IApplicationComponent appComponent) throws EngineException {
		ApplicationComponent app = (ApplicationComponent)appComponent;
		try {
			if (app != null) {
				File appComponentTsFile = new File(appDir, "app.component.temp.ts");
				String filePath = appComponentTsFile.getPath().replace(projectDir.getPath(), File.separator);
				return filePath;
			}
		}
		catch (Exception e) {}
		return null;
	}

	private boolean existPackage(String pkg) {
		File nodeModules = new File(ionicWorkDir, "node_modules");
		if (pkg != null && !pkg.isEmpty()) {
			File pkgDir = new File(nodeModules,pkg);
			return pkgDir.exists() && pkgDir.isDirectory();
		}
		return true;
	}

	private void writeAppBuildSettings(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				Set<String> build_assets = new HashSet<String>();
				Set<String> build_scripts = new HashSet<String>();
				Set<String> build_styles = new HashSet<String>();

				//Menus contributors
				for (Contributor contributor : app.getContributors()) {
					build_assets.addAll(contributor.getBuildAssets());
					build_scripts.addAll(contributor.getBuildScripts());
					build_styles.addAll(contributor.getBuildStyles());
				}

				//Pages contributors
				List<PageComponent> pages = forceEnable ? 
						app.getPageComponentList() :
							getEnabledPages(app);
				for (PageComponent page : pages) {
					synchronized (page) {
						List<Contributor> contributors = page.getContributors();
						for (Contributor contributor : contributors) {
							build_assets.addAll(contributor.getBuildAssets());
							build_scripts.addAll(contributor.getBuildScripts());
							build_styles.addAll(contributor.getBuildStyles());
						}
					}
				}

				boolean hasSettings = !build_assets.isEmpty() || !build_scripts.isEmpty() || !build_styles.isEmpty();
				if (hasSettings) {
					File tplAngularJson = new File(ionicTplDir, "angular.json");
					if (tplAngularJson.exists()) {
						String content = FileUtils.readFileToString(tplAngularJson, "UTF-8");
						JSONObject jsonObject = new JSONObject(content);

						JSONArray jsonArray = null;
						try {
							JSONObject jsonOptions = jsonObject
									.getJSONObject("projects")
									.getJSONObject("app")
									.getJSONObject("architect")
									.getJSONObject("build")
									.getJSONObject("options");

							// Assets
							jsonArray = jsonOptions.getJSONArray("assets");
							for (String asset: build_assets) {
								if (jsonArrayContains(jsonArray, asset)) {
									continue;
								}
								try {
									JSONObject jsonAsset = new JSONObject(asset);
									jsonArray.put(jsonAsset);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
							// Scripts
							jsonArray = jsonOptions.getJSONArray("scripts");
							for (String script: build_scripts) {
								if (jsonArrayContains(jsonArray, script)) {
									continue;
								}
								try {
									jsonArray.put(script);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
							// Styles
							jsonArray = jsonOptions.getJSONArray("styles");
							for (String style: build_styles) {
								if (jsonArrayContains(jsonArray, style)) {
									continue;
								}
								try {
									jsonArray.put(style);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						} catch (Exception e) {
							Engine.logEngine.warn("("+ builderType +") For App angular build options: "+ e.getMessage());
						}

						try {
							JSONObject jsonServe = jsonObject
									.getJSONObject("projects")
									.getJSONObject("app")
									.getJSONObject("architect")
									.getJSONObject("build")
									.getJSONObject("configurations")
									.getJSONObject("serve");
							
							// Assets
							jsonArray = jsonServe.getJSONArray("assets");
							for (String asset: build_assets) {
								if (jsonArrayContains(jsonArray, asset)) {
									continue;
								}
								try {
									JSONObject jsonAsset = new JSONObject(asset);
									jsonArray.put(jsonAsset);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						} catch (Exception e) {
							Engine.logEngine.warn("("+ builderType +") For App angular build configurations: "+ e.getMessage());
						}
						
						setNeedPkgUpdate(true);

						File angularJson = new File(ionicWorkDir, "angular.json");
						String aContent = jsonObject.toString(1);
						writeFile(angularJson, aContent, "UTF-8");
					}
				}

				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") App angular json file generated");
				}
			}
		} catch (Exception e) {
			throw new EngineException("Unable to write angular json file",e);
		}
	}

	static private boolean jsonArrayContains(JSONArray jsonArray, String jsonObToString) {
		try {
			for (int i = 0; i <jsonArray.length(); i++) {
				Object object = jsonArray.get(i);
				if (object.toString().equals(jsonObToString)) {
					return true;
				}
			}
		} catch (Exception e) {}
		return false;
	}
	private void writeAppPluginsConfig(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				Map<String, String> cfg_plugins = new HashMap<>();

				//Menus contributors
				for (Contributor contributor : app.getContributors()) {
					cfg_plugins.putAll(contributor.getConfigPlugins());
				}

				//Pages contributors
				List<PageComponent> pages = forceEnable ? 
						app.getPageComponentList() :
							getEnabledPages(app);
				for (PageComponent page : pages) {
					synchronized (page) {
						List<Contributor> contributors = page.getContributors();
						for (Contributor contributor : contributors) {
							cfg_plugins.putAll(contributor.getConfigPlugins());
						}
					}
				}

				// Shared components contributors
				for (UISharedComponent comp: app.getSharedComponentList()) {
					if (comp.isRegular()) {
						List<Contributor> contributors = comp.getContributors();
						for (Contributor contributor : contributors) {
							cfg_plugins.putAll(contributor.getConfigPlugins());
						}
					}
				}
				
				String mandatoryPlugins = "";
				for (String plugin: cfg_plugins.keySet()) {
					try {
						JSONObject json = new JSONObject(cfg_plugins.get(plugin));
						String version = json.getString("version");
						mandatoryPlugins += "\t<plugin name=\""+plugin+"\" spec=\""+version+"\">"+ System.lineSeparator();
						if (json.has("variables")) {
							JSONObject jsonVars = json.getJSONObject("variables");
							@SuppressWarnings("unchecked")
							Iterator<String> it = jsonVars.keys();
							while (it.hasNext()) {
								String variable = it.next();
								if (!variable.isEmpty()) {
									String value = jsonVars.getString(variable);
									mandatoryPlugins += "\t\t<variable name=\""+variable+"\" value=\""+value+"\" />"+ System.lineSeparator();
								}
							}
						}
						mandatoryPlugins += "\t</plugin>"+ System.lineSeparator();
					} catch (Exception e) {}
				}

				File appPlgConfig = new File(srcDir, "plugins.txt");
				writeFile(appPlgConfig, mandatoryPlugins, "UTF-8");

				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") App plugins config file generated");
				}
			}
		} catch (Exception e) {
			throw new EngineException("Unable to write app plugins config file",e);
		}
	}

	private void writeAppPackageJson(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				Map<String, String> pkg_dependencies = new HashMap<>();

				// Menus contributors
				for (Contributor contributor : app.getContributors()) {
					pkg_dependencies.putAll(contributor.getPackageDependencies());
				}

				// Pages contributors
				List<PageComponent> pages = forceEnable ? 
						app.getPageComponentList() :
							getEnabledPages(app);
				for (PageComponent page : pages) {
					synchronized (page) {
						List<Contributor> contributors = page.getContributors();
						for (Contributor contributor : contributors) {
							pkg_dependencies.putAll(contributor.getPackageDependencies());
						}
					}
				}

				// Shared components contributors
				for (UISharedComponent comp: app.getSharedComponentList()) {
					if (comp.isRegular()) {
						List<Contributor> contributors = comp.getContributors();
						for (Contributor contributor : contributors) {
							pkg_dependencies.putAll(contributor.getPackageDependencies());
						}
					}
				}

				File appPkgJsonTpl = new File(ionicTplDir, "package.json");
				String mContent = FileUtils.readFileToString(appPkgJsonTpl, "UTF-8");
				mContent = mContent.replaceAll("\\.\\./DisplayObjects","../../DisplayObjects");
				mContent = mContent.replaceAll("\\{\\{c8o_project\\}\\}", app.getProject().getName());

				JSONObject jsonPackage = new JSONObject(mContent);
				JSONObject jsonDependencies = jsonPackage.getJSONObject("dependencies");
				for (String pkg : pkg_dependencies.keySet()) {
					jsonDependencies.put(pkg, pkg_dependencies.get(pkg));
					if (!existPackage(pkg)) {
						setNeedPkgUpdate(true);
					}
				}

				boolean addNode = !jsonDependencies.has("@types/node");
				if (addNode) {
					try {
						String version = new JSONObject(FileUtils.readFileToString(new File(ionicTplDir, "version.json"), "utf-8")).getString("version");
						addNode = version.matches("7\\.[0-7]\\..*");
					} catch (Exception e) {
					}
					if (addNode) {
						jsonDependencies.put("@types/node", "12.0.10");
					}
				}

				File appPkgJson = new File(ionicWorkDir, "package.json");
				writeFile(appPkgJson, jsonPackage.toString(2), "UTF-8");

				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") Ionic package json file generated");
				}
			}
		} catch (Exception e) {
			throw new EngineException("Unable to write ionic package json file",e);
		}
	}

	private void writeAppServiceTs(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				Map<String, String> action_ts_imports = new HashMap<>();
				Map<String, String> action_ts_functions = new HashMap<>();

				//App contributors
				for (Contributor contributor : app.getContributors()) {
					contributor.forContainer(app, () -> {
						action_ts_imports.putAll(contributor.getActionTsImports());
						action_ts_functions.putAll(contributor.getActionTsFunctions());
					});
				}

				//Shared components
				for (UISharedComponent comp: app.getSharedComponentList()) {
					if (comp.isRegular()) {
						List<Contributor> contributors = comp.getContributors();
						for (Contributor contributor : contributors) {
							contributor.forContainer(app, () -> {
								action_ts_imports.putAll(contributor.getActionTsImports());
								action_ts_functions.putAll(contributor.getActionTsFunctions());
							});
						}
					}
				}

				//Pages contributors
				List<PageComponent> pages = forceEnable ? 
						app.getPageComponentList() :
							getEnabledPages(app);
				for (PageComponent page : pages) {
					synchronized (page) {
						List<Contributor> contributors = page.getContributors();
						for (Contributor contributor : contributors) {
							contributor.forContainer(app, () -> {
								action_ts_imports.putAll(contributor.getActionTsImports());
								action_ts_functions.putAll(contributor.getActionTsFunctions());
							});
						}
					}
				}

				String c8o_ActionTsImports = "";
				for (String comp : action_ts_imports.keySet()) {
					if (!getTplServiceActionTsImports().containsKey(comp)) {
//
						String from = action_ts_imports.get(comp);
						String[] parted = from.split("__c8o_separator__");
						from = parted[0];
						String directImport = parted.length > 1 ? parted[1] : "false";
						if("true".equalsIgnoreCase(directImport)) {
							c8o_ActionTsImports += "import "+comp+" from '"+ from +"';"+ System.lineSeparator();
						}
						else if (comp.indexOf(" as ") == -1) {
							String comPath = from.replace("./pages", "../pages");
							c8o_ActionTsImports += "import { "+comp+" } from '"+ comPath +"';"+ System.lineSeparator();
						} else {
							c8o_ActionTsImports += "import "+comp+" from '"+ from +"';"+ System.lineSeparator();
						}
					}
				}

				String c8o_ActionTsFunctions = System.lineSeparator();
				for (String function : action_ts_functions.values()) {
					c8o_ActionTsFunctions += function + System.lineSeparator();
				}

				File appServiceTpl = new File(ionicTplDir, "src/app/services/actionbeans.service.ts");
				String mContent = FileUtils.readFileToString(appServiceTpl, "UTF-8");
				mContent = mContent.replaceAll("/\\*\\=c8o_ActionTsImports\\*/",Matcher.quoteReplacement(c8o_ActionTsImports));
				mContent = mContent.replaceAll("/\\*\\=c8o_ActionTsFunctions\\*/",Matcher.quoteReplacement(c8o_ActionTsFunctions));
				File appServiceTsFile = new File(servicesDir, "actionbeans.service.ts");
				writeFile(appServiceTsFile, mContent, "UTF-8");

				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") Ionic service ts file generated for 'app'");
				}
			}
		} catch (Exception e) {
			throw new EngineException("Unable to write ionic app service ts file",e);
		}
	}

	private void writeAppRoutingTs(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				boolean tplIsLowerThan8043 = app.compareToTplVersion("8.4.0.3") < 0;
				String c8o_AppRoutes = "";
				int i=1;

				//Pages contributors
				List<PageComponent> pages = app.getPageComponentList();//getEnabledPages(app);
				for (PageComponent page : pages) {
					synchronized (page) {
						String pageDirName = pageDir(page).getName();
						String pageModuleName =  page.getName() + (tplIsLowerThan8043 ? "Module": "Route");
						String pageModulePath = "./pages/" + pageDirName + "/" + page.getName().toLowerCase() + (tplIsLowerThan8043 ? ".module": ".routes");
						String pageSegment = page.getSegment();
						boolean isLastPage = i == pages.size();
						if (page.isRoot) {
							if (pageSegment.indexOf('/') != -1) {
								String rootSegment = pageSegment.substring(0, pageSegment.indexOf('/'));
								c8o_AppRoutes += "{ path: '', redirectTo: '"+ rootSegment +"', pathMatch: 'full' }," + System.lineSeparator();
								c8o_AppRoutes += " { path: '"+rootSegment+"', loadChildren: () => import('"+pageModulePath+"').then( m => m."+ pageModuleName +")}," + System.lineSeparator();
							} else {
								c8o_AppRoutes += "{ path: '', redirectTo: '"+ pageSegment +"', pathMatch: 'full' }," + System.lineSeparator();
							}
						}
						c8o_AppRoutes += " { path: '"+pageSegment+"', loadChildren: () => import('"+pageModulePath+"').then( m => m."+ pageModuleName +")}" + 
								(isLastPage ? "":",") + System.lineSeparator();
					}
				}

				File appRoutingTpl = new File(ionicTplDir, tplIsLowerThan8043 ? "src/app-routing.module.tpl" : "src/app.routes.tpl");
				String mContent = FileUtils.readFileToString(appRoutingTpl, "UTF-8");
				mContent = mContent.replaceAll("/\\*\\=c8o_AppRoutes\\*/", c8o_AppRoutes);

				File appRoutingTsFile = new File(appDir, tplIsLowerThan8043 ? "app-routing.module.ts" : "app.routes.ts");
				writeFile(appRoutingTsFile, mContent, "UTF-8");


				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") Ionic routing module ts file generated for 'app'");
				}

			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic app routing ts file",e);
		}
	}

	private void writeAppModuleTs(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				String c8o_PagesImport = "";
				String c8o_PagesLinks = "";
				String c8o_PagesDeclarations = "";
				String c8o_ServiceWorkerEnabled = app.isPWA() ? "environment.production":"false";
				int i=1;

				Map<String, File> comp_beans_dirs = new HashMap<>();
				Map<String, String> module_ts_imports = new HashMap<>();
				Set<String> module_ng_imports =  new HashSet<String>();
				Set<String> module_ng_providers =  new HashSet<String>();
				Set<String> module_ng_declarations =  new HashSet<String>();
				Set<String> module_ng_components =  new HashSet<String>();

				//App contributors
				for (Contributor contributor : app.getContributors()) {
					contributor.forContainer(app, () -> {
						comp_beans_dirs.putAll(contributor.getCompBeanDir());
						module_ts_imports.putAll(contributor.getModuleTsImports());
						module_ng_imports.addAll(contributor.getModuleNgImports());
						module_ng_providers.addAll(contributor.getModuleNgProviders());
						module_ng_declarations.addAll(contributor.getModuleNgDeclarations());
						module_ng_components.addAll(contributor.getModuleNgComponents());
					});
				}

				//Pages contributors
				List<PageComponent> pages = getEnabledPages(app);
				for (PageComponent page : pages) {
					synchronized (page) {
						String pageName = page.getName();
						String pageSegment = page.getSegment();
						boolean isLastPage = i == pages.size();

						if (app.compareToTplVersion("7.7.0.2") < 0) {
							c8o_PagesImport += "import { "+pageName+" } from \"../pages/"+pageName+"/"+pageName.toLowerCase()+"\";"+ System.lineSeparator();
							c8o_PagesLinks += " { component: "+pageName+", name: \""+pageName+"\", segment: \""+pageSegment+"\" }" + (isLastPage ? "":",");
							c8o_PagesDeclarations += " " + pageName + (isLastPage ? "":",");

							List<Contributor> contributors = page.getContributors();
							for (Contributor contributor : contributors) {
								contributor.forContainer(page, () -> {
									comp_beans_dirs.putAll(contributor.getCompBeanDir());
									module_ts_imports.putAll(contributor.getModuleTsImports());
									module_ng_imports.addAll(contributor.getModuleNgImports());
									module_ng_providers.addAll(contributor.getModuleNgProviders());
									module_ng_declarations.addAll(contributor.getModuleNgDeclarations());
									module_ng_components.addAll(contributor.getModuleNgComponents());
								});
							}
						} else {
							List<Contributor> contributors = page.getContributors();
							for (Contributor contributor : contributors) {
								contributor.forContainer(page, () -> {
									if (contributor.isNgModuleForApp()) {
										comp_beans_dirs.putAll(contributor.getCompBeanDir());
										module_ts_imports.putAll(contributor.getModuleTsImports());
										module_ng_imports.addAll(contributor.getModuleNgImports());
										module_ng_providers.addAll(contributor.getModuleNgProviders());
										module_ng_declarations.addAll(contributor.getModuleNgDeclarations());
										module_ng_components.addAll(contributor.getModuleNgComponents());
									}
								});
							}

							writePageModuleTs(page);
							writePageRoutingTs(page);
						}

						i++;
					}
				}

				String c8o_ModuleTsImports = "";
				Map<String, String> tpl_ts_imports = getTplAppModuleTsImports();
				if (!module_ts_imports.isEmpty()) {
					for (String comp : module_ts_imports.keySet()) {
						if (!tpl_ts_imports.containsKey(comp)) {
							String from = module_ts_imports.get(comp);
							String[] parted = from.split("__c8o_separator__");
							from = parted[0];
							String directImport = parted.length > 1 ? parted[1] : "false";
													
							if (comp.indexOf(" as ") != -1 || "true".equalsIgnoreCase(directImport)) {
								c8o_ModuleTsImports += "import "+comp+" from '"+ from +"';"+ System.lineSeparator();
							} else {
								from = from.startsWith("../components/") ? "."+ from.substring(2) : from;
								from = (from.startsWith("components/") ? "./" : "") + from;
								c8o_ModuleTsImports += "import { "+comp+" } from '"+ from +"';"+ System.lineSeparator();
							}
						}
					}
				}

				String c8o_ModuleNgImports = "";
				String tpl_ng_imports = getTplAppModuleNgImports();
				if (!module_ng_imports.isEmpty()) {
					for (String module: module_ng_imports) {
						if (!tpl_ng_imports.contains(module)) {
							c8o_ModuleNgImports += "\t" + module + "," + System.lineSeparator();
						}
					}
					if (!c8o_ModuleNgImports.isEmpty()) {
						c8o_ModuleNgImports = System.lineSeparator() + c8o_ModuleNgImports + System.lineSeparator();;
					}
				}

				String c8o_ModuleNgProviders = "";
				String tpl_ng_providers = getTplAppModuleNgProviders();
				if (!module_ng_providers.isEmpty()) {
					for (String provider: module_ng_providers) {
						if (!tpl_ng_providers.contains(provider)) {
							c8o_ModuleNgProviders += "\t" + provider + "," + System.lineSeparator();
						}
					}
					if (!c8o_ModuleNgProviders.isEmpty()) {
						c8o_ModuleNgProviders = System.lineSeparator() + c8o_ModuleNgProviders;
					}
				}

				String c8o_ModuleNgDeclarations = "";
				String tpl_ng_declarations = getTplAppModuleNgDeclarations();
				if (!module_ng_declarations.isEmpty()) {
					for (String declaration: module_ng_declarations) {
						if (!tpl_ng_declarations.contains(declaration)) {
							c8o_ModuleNgDeclarations += "\t" + declaration + "," + System.lineSeparator();
						}
					}
					if (!c8o_ModuleNgDeclarations.isEmpty()) {
						c8o_ModuleNgDeclarations = System.lineSeparator() + c8o_ModuleNgDeclarations;
					}
				}

				String c8o_ModuleNgComponents = "";
				String tpl_ng_components = getTplAppModuleNgComponents();
				if (!module_ng_components.isEmpty()) {
					for (String component: module_ng_components) {
						if (!tpl_ng_components.contains(component)) {
							c8o_ModuleNgComponents += "\t" + component + "," + System.lineSeparator();
						}
					}
					if (!c8o_ModuleNgComponents.isEmpty()) {
						c8o_ModuleNgComponents = System.lineSeparator() + c8o_ModuleNgComponents;
					}
				}

				File appModuleTpl = new File(ionicTplDir, "src/app/app.module.ts");
				String mContent = FileUtils.readFileToString(appModuleTpl, "UTF-8");
				mContent = mContent.replaceAll("/\\*\\=c8o_ModuleTsImports\\*/",c8o_ModuleTsImports);
				mContent = mContent.replaceAll("/\\*\\=c8o_PagesImport\\*/",c8o_PagesImport);
				mContent = mContent.replaceAll("/\\*\\=c8o_PagesLinks\\*/",c8o_PagesLinks);
				mContent = mContent.replaceAll("/\\*\\=c8o_PagesDeclarations\\*/",c8o_PagesDeclarations);
				mContent = mContent.replaceAll("/\\*\\=c8o_ServiceWorkerEnabled\\*/",c8o_ServiceWorkerEnabled);
				mContent = mContent.replaceAll("/\\*Begin_c8o_NgModules\\*/","");
				mContent = mContent.replaceAll("/\\*End_c8o_NgModules\\*/",c8o_ModuleNgImports);
				mContent = mContent.replaceAll("/\\*Begin_c8o_NgProviders\\*/",c8o_ModuleNgProviders);
				mContent = mContent.replaceAll("/\\*End_c8o_NgProviders\\*/","");
				mContent = mContent.replaceAll("/\\*Begin_c8o_NgDeclarations\\*/",c8o_ModuleNgDeclarations);
				mContent = mContent.replaceAll("/\\*End_c8o_NgDeclarations\\*/","");
				mContent = mContent.replaceAll("/\\*Begin_c8o_NgComponents\\*/",c8o_ModuleNgComponents);
				mContent = mContent.replaceAll("/\\*End_c8o_NgComponents\\*/","");

				File appModuleTsFile = new File(appDir, "app.module.ts");
				writeFile(appModuleTsFile, mContent, "UTF-8");

				for (String compbean : comp_beans_dirs.keySet()) {
					File srcCompDir = comp_beans_dirs.get(compbean);
					for (File f: srcCompDir.listFiles()) {
						String fContent = FileUtils.readFileToString(f, "UTF-8");
						File destFile = new File(componentsDir, compbean+ "/"+ f.getName());
						writeFile(destFile, fContent, "UTF-8");
					}
				}

				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") Ionic module ts file generated for 'app'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic app module ts file",e);
		}
	}
	
	private void writeAppMainTs(ApplicationComponent app) throws EngineException {
		try {
			String c8o_PagesImport = "";
			String c8o_ServiceWorkerEnabled = app.isPWA() ? "environment.production":"false";
	
			Map<String, File> comp_beans_dirs = new HashMap<>();
			Map<String, String> module_ts_imports = new HashMap<>();
			Set<String> module_ng_imports =  new HashSet<String>();
			Set<String> module_ng_providers =  new HashSet<String>();
	
			//App contributors
			for (Contributor contributor : app.getContributors()) {
				contributor.forContainer(app, () -> {
					comp_beans_dirs.putAll(contributor.getCompBeanDir());
					module_ts_imports.putAll(contributor.getModuleTsImports());
					module_ng_imports.addAll(contributor.getModuleNgImports());
					module_ng_providers.addAll(contributor.getModuleNgProviders());
				});
			}
	
			//Pages contributors
			List<PageComponent> pages = getEnabledPages(app);
			for (PageComponent page : pages) {
				synchronized (page) {
					List<Contributor> contributors = page.getContributors();
					for (Contributor contributor : contributors) {
						contributor.forContainer(page, () -> {
							if (contributor.isNgModuleForApp()) {
								comp_beans_dirs.putAll(contributor.getCompBeanDir());
								module_ts_imports.putAll(contributor.getModuleTsImports());
								module_ng_imports.addAll(contributor.getModuleNgImports());
								module_ng_providers.addAll(contributor.getModuleNgProviders());
							}
						});
					}

					writePageModuleTs(page);
					writePageRoutingTs(page);
	
				}
			}
			
			module_ts_imports.remove("BrowserAnimationsModule");
			HashMap<String, String> mapRemovedModule = new HashMap<>();
			String c8o_ModuleTsImports = "";
			Map<String, String> tpl_ts_imports = getTplMainTsImports();
			if (!module_ts_imports.isEmpty()) {
				for (String comp : module_ts_imports.keySet()) {
					if (!tpl_ts_imports.containsKey(comp)) {
						String from = module_ts_imports.get(comp);
						String compM = comp;
						String fromM = from;
						if(from.contains("components/") || from.contains("pages/")) {
							compM = comp.replaceAll("Module", "");
							fromM = from.replaceAll("Module", "").replaceAll(".module", "");
							mapRemovedModule.put(comp, compM);
						}
						String pattern = "\\{\\s*" + Pattern.quote(compM) + "\\s*\\}";
						Pattern compiledPattern = Pattern.compile(pattern);
//						Matcher matcher = compiledPattern.matcher(c8o_PagesImport);
				        Matcher matcher2 = compiledPattern.matcher(c8o_ModuleTsImports);
//				        Matcher matcher3 = compiledPattern.matcher(c8o_AppImports);
				        if (/*!matcher.find() && */!matcher2.find() /*&& !matcher3.find()*/) {
				        	String[] parted = fromM.split("__c8o_separator__");
				        	fromM = parted[0];
							String directImport = parted.length > 1 ? parted[1] : "false";
							if (comp.indexOf(" as ") != -1 || "true".equalsIgnoreCase(directImport)) {
								c8o_ModuleTsImports += "import "+compM+" from '"+ fromM +"';"+ System.lineSeparator();
							} else {
								fromM = fromM.startsWith("../components/") ? "."+ fromM.substring(2) : fromM;
								fromM = (fromM.startsWith("components/") ? "./" : "") + fromM;
								c8o_ModuleTsImports += "import { "+compM+" } from '"+ fromM +"';"+ System.lineSeparator();
							}
				        }
					}
				}
			}
			
			
			String c8o_ModuleNgProviders = "";
			String tpl_ng_providers = getTplAppNgProviders("src/main.ts");
			if (!module_ng_providers.isEmpty()) {
				for (String provider: module_ng_providers) {
					if (!tpl_ng_providers.contains(provider)) {
						c8o_ModuleNgProviders += "\t" + provider + "," + System.lineSeparator();
					}
				}
				if (!c8o_ModuleNgProviders.isEmpty()) {
					c8o_ModuleNgProviders = System.lineSeparator() + c8o_ModuleNgProviders;
				}
			}
			
			File mainTsFile = new File(ionicTplDir, "src/main.ts");
			String mainContent = FileUtils.readFileToString(mainTsFile, "UTF-8");
			mainContent = mainContent.replaceAll("/\\*\\=c8o_ServiceWorkerEnabled\\*/",c8o_ServiceWorkerEnabled);
			mainContent = mainContent.replaceAll("/\\*Begin_c8o_NgProviders\\*/",c8o_ModuleNgProviders);
			mainContent = mainContent.replaceAll("/\\*c8o_PagesImport\\*/",c8o_PagesImport + c8o_ModuleTsImports);
		
			File appMainTsFile = new File(srcDir, "main.ts");
			writeFile(appMainTsFile, mainContent, "UTF-8");
			
			for (String compbean : comp_beans_dirs.keySet()) {
				File srcCompDir = comp_beans_dirs.get(compbean);
				for (File f: srcCompDir.listFiles()) {
					String fContent = FileUtils.readFileToString(f, "UTF-8");
					File destFile = new File(componentsDir, compbean+ "/"+ f.getName());
					writeFile(destFile, fContent, "UTF-8");
				}
			}

			if (initDone) {
				Engine.logEngine.trace("("+ builderType +") Ionic module ts file generated for 'app'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic app module ts file",e);
		}
	}

	private String getAppComponentTsContent(ApplicationComponent app) throws IOException {
		String c8o_AppProdMode = "";
		String c8o_PagesImport = "";
		String c8o_PagesVariables = "";
		String c8o_PagesVariablesKeyValue = "";
		String c8o_RootPage = "null";
		String c8o_PageArrayDef = "Array<>";
		String c8o_Version = app.getC8oVersion();
		String c8o_AppComponentMarkers = app.getComponentScriptContent().getString();
		String c8o_AppImports = app.getComputedImports();
		String c8o_AppDeclarations = app.getComputedDeclarations();
		String c8o_AppConstructors = app.getComputedConstructors();
		String c8o_AppFunctions = app.getComputedFunctions();
		Set<String> module_ng_imports =  new HashSet<String>();
		Map<String, String> module_ts_imports = new HashMap<>();
		boolean tplIsLowerThan8043 = app.compareToTplVersion("8.4.0.3") < 0;
		
		if(!tplIsLowerThan8043) {
			//App contributors
			for (Contributor contributor : app.getContributors()) {
				contributor.forContainer(app, () -> {
					module_ng_imports.addAll(contributor.getModuleNgImports());
					module_ts_imports.putAll(contributor.getModuleTsImports());
				});
			}
		}
		
		int i=1;

		if (app.compareToTplVersion("7.9.0.2") >= 0) {
			c8o_PageArrayDef = "Array<{title: string, titleKey: string, url: string, icon: string, iconPos: string, name: string, includedInAutoMenu?: boolean}>";
		}

		List<PageComponent> pages = getEnabledPages(app);
		for (PageComponent page : pages) {
			synchronized (page) {
				String pageName = page.getName();
				String pageIcon = page.getIcon();
				String pageIconPos = page.getIconPosition();
				String pageTitle = page.getTitle();
				String pageSegment = page.getSegment();
				String pageTitleKey = TranslateUtils.getComputedKey(project, page.getTitle());
				boolean isRootPage = page.isRoot;
				boolean isMenuPage = page.isInAutoMenu();
				boolean isLastPage = i == pages.size();

				if (isRootPage) {
					c8o_RootPage = pageName;
				}

				if (app.compareToTplVersion("7.9.0.2") >= 0) {
					if (isRootPage) {
						c8o_RootPage = "'"+ c8o_RootPage + "'";
					}
					c8o_PagesVariables += " { title: \""+pageTitle+"\", titleKey: \""+ pageTitleKey +"\", url: \""+ pageSegment +"\", icon: \""+ pageIcon +"\", iconPos: \""+ pageIconPos +"\", name: \""+ pageName +"\", includedInAutoMenu: "+ isMenuPage +"}" + (isLastPage ? "":",");

					c8o_PagesVariablesKeyValue += pageName+":"+ "this.rootPage" + (isLastPage ? "":",");
				}
				if(!tplIsLowerThan8043) {
					List<Contributor> contributors = page.getContributors();
					for (Contributor contributor : contributors) {
						contributor.forContainer(page, () -> {
							if (contributor.isNgModuleForApp()) {
								module_ng_imports.addAll(contributor.getModuleNgImports());
								module_ts_imports.putAll(contributor.getModuleTsImports());
							}
						});
					}
				}

				i++;
			}
		}
		String c8o_ModuleNgImports = "";
		String c8o_ModuleTsImports = "";
		if(!tplIsLowerThan8043) {
			// fix for BrowserAnimationsModule until it will be handled in config
			module_ts_imports.remove("BrowserAnimationsModule");
			module_ng_imports.remove("BrowserAnimationsModule");
			HashMap<String, String> mapRemovedModule = new HashMap<>();
			
			Map<String, String> tpl_ts_imports = getTplAppCompTsImports();
			if (!module_ts_imports.isEmpty()) {
				for (String comp : module_ts_imports.keySet()) {
					if (!tpl_ts_imports.containsKey(comp)) {
						String from = module_ts_imports.get(comp);
						String compM = comp;
						String fromM = from;
						if(from.contains("components/") || from.contains("pages/")) {
							compM = comp.replaceAll("Module", "");
							fromM = from.replaceAll("Module", "").replaceAll(".module", "");
							mapRemovedModule.put(comp, compM);
						}
						String pattern = "\\{\\s*" + Pattern.quote(compM) + "\\s*\\}";
						Pattern compiledPattern = Pattern.compile(pattern);
						Matcher matcher = compiledPattern.matcher(c8o_PagesImport);
				        Matcher matcher2 = compiledPattern.matcher(c8o_ModuleTsImports);
				        Matcher matcher3 = compiledPattern.matcher(c8o_AppImports);
				        if (!matcher.find() && !matcher2.find() && !matcher3.find()) {
				        	String[] parted = fromM.split("__c8o_separator__");
				        	fromM = parted[0];
							String directImport = parted.length > 1 ? parted[1] : "false";
							if (comp.indexOf(" as ") != -1 || "true".equalsIgnoreCase(directImport)) {
								c8o_ModuleTsImports += "import "+compM+" from '"+ fromM +"';"+ System.lineSeparator();
							} else {
								fromM = fromM.startsWith("../components/") ? "."+ fromM.substring(2) : fromM;
								fromM = (fromM.startsWith("components/") ? "./" : "") + fromM;
								c8o_ModuleTsImports += "import { "+compM+" } from '"+ fromM +"';"+ System.lineSeparator();
							}
				        }
					}
				}
			}
			
			
			String tpl_ng_imports = getTplAppNgImports("src/app/app.component.ts");
			if (!module_ng_imports.isEmpty()) {
				for (String module: module_ng_imports) {
					if (!tpl_ng_imports.contains(module)) {
						String moduleM = mapRemovedModule.get(module);
						moduleM = moduleM != null ? moduleM : module;
						c8o_ModuleNgImports += "\t" + moduleM + "," + System.lineSeparator();
					}
				}
				if (!c8o_ModuleNgImports.isEmpty()) {
					c8o_ModuleNgImports = System.lineSeparator() + c8o_ModuleNgImports + System.lineSeparator();;
				}
			}
		}

		String computedRoute = app.getComputedRoute();
		File appComponentTpl = new File(ionicTplDir, "src/app/app.component.ts");
		String cContent = FileUtils.readFileToString(appComponentTpl, "UTF-8");

		if (app.compareToTplVersion("7.7.0.2") >= 0) {
			c8o_AppProdMode = MobileBuilderBuildMode.production.equals(buildMode) ? "enableProdMode();":"";			
		}

		cContent = cContent.replaceAll("/\\*\\=c8o_PagesImport\\*/",c8o_PagesImport);
		cContent = cContent.replaceAll("/\\*\\=c8o_RootPage\\*/",c8o_RootPage);
		cContent = cContent.replaceAll("/\\*\\=c8o_PageArrayDef\\*/",c8o_PageArrayDef);
		cContent = cContent.replaceAll("/\\*\\=c8o_PagesVariables\\*/",c8o_PagesVariables);
		cContent = cContent.replaceAll("/\\*\\=c8o_PagesVariablesKeyValue\\*/",c8o_PagesVariablesKeyValue);
		cContent = cContent.replaceAll("/\\*\\=c8o_RoutingTable\\*/",computedRoute);
		
		cContent = cContent.replaceAll("/\\*\\=c8o_AppDeclarations\\*/",c8o_AppDeclarations);
		cContent = cContent.replaceAll("/\\*\\=c8o_AppConstructors\\*/",c8o_AppConstructors);
		cContent = cContent.replaceAll("/\\*\\=c8o_AppProdMode\\*/",c8o_AppProdMode);
		cContent = cContent.replaceAll("/\\*\\=c8o_AppImports\\*/",c8o_AppImports + (tplIsLowerThan8043 ? "" : c8o_ModuleTsImports));
		if(!tplIsLowerThan8043) {
			cContent = cContent.replaceAll("/\\*Begin_c8o_NgModules\\*/","");
			cContent = cContent.replaceAll("/\\*End_c8o_NgModules\\*/",c8o_ModuleNgImports);
		}
		
		String c8oInit = "settings.addHeader(\"x-convertigo-mb\", \""+c8o_Version+"\");\n\t\tthis.c8o.init(";
		cContent = cContent.replaceFirst("this\\.c8o\\.init\\(", c8oInit);

		Pattern pattern = Pattern.compile("/\\*Begin_c8o_(.+)\\*/"); // begin c8o marker
		Matcher matcher = pattern.matcher(cContent);
		while (matcher.find()) {
			String markerId = matcher.group(1);
			String tplMarker = getMarker(cContent, markerId);
			String customMarker = getMarker(c8o_AppComponentMarkers, markerId);
			if (!customMarker.isEmpty()) {
				cContent = cContent.replace(tplMarker, customMarker);
			}
		}

		cContent = cContent.replaceAll("/\\*\\=c8o_AppFunctions\\*/", Matcher.quoteReplacement(c8o_AppFunctions));

		return cContent;
	}

	private void writeAppComponentTs(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {

				File appComponentTsFile = new File(appDir, "app.component.ts");
				writeFile(appComponentTsFile, getAppComponentTsContent(app), "UTF-8");

				File tempTsFile = new File(appDir, "app.component.temp.ts");
				if (tempTsFile.exists()) {
					writeAppComponentTempTs(app);
				}
				
				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") Ionic component ts file generated for 'app'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic app component ts file",e);
		}
	}

	@Override
	public void writeAppComponentTempTs(final IApplicationComponent appComponent) throws EngineException {
		ApplicationComponent app = (ApplicationComponent)appComponent;
		try {
			if (app != null) {
				File appTsFile = new File(appDir, "app.component.ts");
				synchronized (writtenFiles) {
					if (writtenFiles.contains(appTsFile)) {
						File appTsFileTmp = toTmpFile(appTsFile);
						if (appTsFileTmp.exists()) {
							appTsFile = appTsFileTmp;
						}
					}
				}

				String tsContent = FileUtils.readFileToString(appTsFile, "UTF-8");
				
				// Write file if needed (do not need delay)
				File tempTsFile = new File(appDir, "app.component.temp.ts");
				if (!tempTsFile.exists() || !tsContent.equals(FileUtils.readFileToString(tempTsFile, "UTF-8"))) {
					FileUtils.copyFile(appTsFile, tempTsFile);
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic app component temp ts file",e);
		}
	}

	private void writeAppTemplate(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				String appName = app.getName();
				File appHtmlFile = new File(appDir, "app.component.html");
				String computedTemplate = app.getComputedTemplate();
				writeFile(appHtmlFile, computedTemplate, "UTF-8");

				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") Ionic template file generated for app '"+appName+"'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic app html file",e);
		}
	}

	private void writeAppStyle(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				String appName = app.getName();
				File appScssFile = new File(appDir, "app.component.scss");
				String computedScss = app.getComputedStyle();
				writeFile(appScssFile, computedScss, "UTF-8");

				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") Ionic scss file generated for app '"+appName+"'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic app scss file",e);
		}
	}

	private void writeAppTheme(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				String appName = app.getName();
				File themeScssFile = new File(themeDir, "variables.scss");
				String tContent = app.getComputedTheme();
				writeFile(themeScssFile, tContent, "UTF-8");

				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") Ionic theme scss file generated for app '"+appName+"'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic theme scss file",e);
		}
	}

	private void removeUselessPages(ApplicationComponent application) {
		if (application != null) {
			File ionicPagesDir = pagesDir;
			List<String> pageDirectories = new ArrayList<String>();
			pageDirectories.add(ionicPagesDir.getAbsolutePath());

			List<PageComponent> pages = application.getPageComponentList();//getEnabledPages(application);
			for (PageComponent page : pages) {
				File pageDir = pageDir(page);
				pageDirectories.add(pageDir.getAbsolutePath());
			}
			for (File dir: FileUtils.listFilesAndDirs(ionicPagesDir, FalseFileFilter.INSTANCE, DirectoryFileFilter.DIRECTORY)) {
				if (!pageDirectories.contains(dir.getAbsolutePath())) {
					try {
						deleteSourceDir(dir);
					}
					catch(Exception e) {}
				}
			}
		}
	}

	private void removeUselessComps(ApplicationComponent application) {
		if (application != null) {
			File ionicComponentsDir = componentsDir;
			if (!ionicComponentsDir.exists()) return;
			
			List<String> compDirectories = new ArrayList<String>();
			compDirectories.add(ionicComponentsDir.getAbsolutePath());

			List<UISharedComponent> comps = application.getSharedComponentList();
			for (UISharedComponent comp : comps) {
				File compDir = compDir(comp);
				compDirectories.add(compDir.getAbsolutePath());
			}
			String prefix = project.getName().toLowerCase() + ".";
			for (File dir: FileUtils.listFilesAndDirs(ionicComponentsDir, FalseFileFilter.INSTANCE, DirectoryFileFilter.DIRECTORY)) {
				if (!compDirectories.contains(dir.getAbsolutePath())) {
					try {
						String compDirName = dir.getName();
						if (compDirName.startsWith(prefix) || !existTargetComp(compDirName)) {
							deleteSourceDir(dir);
						}
					}
					catch(Exception e) {}
				}
			}
		}
	}

	private void writeAppSourceFiles(ApplicationComponent application) throws EngineException {
		try {
			if (application != null) {
				if (!initDone) {
					FileUtils.deleteQuietly(new File(appDir, "app.component.temp.ts"));
				}

				writeAppPackageJson(application);
				writeAppBuildSettings(application);
				writeAppPluginsConfig(application);
				writeAppServiceTs(application);
				if(application.compareToTplVersion("8.4.0.3") < 0) {
					writeAppModuleTs(application);
				}
				else {
					writeAppMainTs(application);
				}
				writeAppComponentTs(application);
				writeAppTemplate(application);
				writeAppStyle(application);
				writeAppTheme(application);
				writeAppRoutingTs(application);

				Engine.logEngine.trace("("+ builderType +") Application source files generated for ionic project '"+ project.getName() +"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write application source files for ionic project '"+ project.getName() +"'",e);
		}
	}

	private void writePageSourceFiles(PageComponent page) throws EngineException {
		String pageName = page.getName();
		try {
			File pageDir = pageDir(page);
			pageDir.mkdirs();

			if (!initDone) {
				FileUtils.deleteQuietly(new File(pageDir, pageName.toLowerCase() + ".temp.ts"));
			}

			writePageTs(page);
			writePageModuleTs(page);
			writePageRoutingTs(page);
			writePageStyle(page);
			writePageTemplate(page);

			if (initDone) {
				Engine.logEngine.trace("("+ builderType +") Ionic source files generated for page '"+pageName+"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write source files for page '"+pageName+"'",e);
		}
	}

	private void deleteSourceDir(File dir) throws EngineException {
		try {
			if (dir != null && dir.exists()) {
				File deletedTsFile = new File(dir, FakeDeleted);
				writeFile(deletedTsFile, "", "UTF-8");

				if (initDone) {
					Engine.logEngine.trace("("+ builderType +") Deleted directory '"+dir+"'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page ts file",e);
		}
	}
	
	private void writeCompSourceFiles(UISharedComponent comp) throws EngineException {
		String compName = comp.getName();
		try {
			File compDir = compDir(comp);
			compDir.mkdirs();

			if (!initDone) {
				FileUtils.deleteQuietly(new File(compDir, compFileName(comp) + ".temp.ts"));
			}

			writeCompTs(comp);
			writeCompModuleTs(comp);
			writeCompStyle(comp);
			writeCompTemplate(comp);

			if (initDone) {
				Engine.logEngine.trace("("+ builderType +") Ionic source files generated for component '"+compName+"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write source files for component '"+compName+"'",e);
		}
	}

	

	public boolean isBuildProdMode() {
		return MobileBuilderBuildMode.production.equals(this.buildMode);
	}

	private void configurePwaApp(ApplicationComponent app) {
		this.isPWA = app.isPWA();

		try {
			File workerConfig = new File(ionicWorkDir, "ngsw-config.json");
			if (workerConfig.exists()) {
				// nothing to do
			}

			// Set application name (index.html)
			File tpl_index = new File(ionicTplDir, "src/index.html");
			String tpl_index_content = FileUtils.readFileToString(tpl_index, "UTF-8");
			String index_content = tpl_index_content;

			MobileApplication ma = app.getParent();
			String pwaAppName = ma.getComputedApplicationName();
			pwaAppName = pwaAppName.isEmpty() ? app.getName() : pwaAppName;
			index_content = index_content.replace("<!--c8o_App_Name-->", pwaAppName);

			File index = new File(ionicWorkDir, "src/index.html");
			writeFile(index, index_content, "UTF-8");

			// Set application infos (manifest.webmanifest)
			File manifestFile = new File(ionicWorkDir, "src/manifest.webmanifest");
			if (manifestFile.exists()) {
				String jsonContent = FileUtils.readFileToString(manifestFile, "UTF-8");
				JSONObject jsonOb = new JSONObject(jsonContent);
				
				// Set application name 
				jsonOb.put("name", pwaAppName);
				jsonOb.put("short_name", pwaAppName);
				
				// Set application theme color
				String themeColor = ma.getApplicationThemeColor();
				if (!themeColor.isEmpty()) jsonOb.put("theme_color", themeColor);
				
				// Set application bg color
				String bgColor = ma.getApplicationBgColor();
				if (!bgColor.isEmpty()) jsonOb.put("background_color", bgColor);
				
				// Set application icons
				JSONArray icons = new JSONArray();
				XMLVector<XMLVector<String>> xmlv = ma.getApplicationIcons();
				for (XMLVector<String> v: xmlv) {
					try {
						JSONObject jso = new JSONObject(v.get(0));
						icons.put(jso);
					} catch (Exception e) {}
				}
				if (icons.length() > 0) {
					jsonOb.put("icons", icons);
				}
				
				writeFile(manifestFile, jsonOb.toString(4), "UTF-8");
			}
		} catch (Exception e) {
			;
		}
	}

	@Override
	protected void writeFile(File file, CharSequence content, String encoding) throws IOException {
		// Replace eol characters with system line separators
		content = LsPattern.matcher(content).replaceAll(System.lineSeparator());
		
		if (initDone && Engine.isStudioMode()) {
			synchronized (writtenFiles) {
				// Checks for content changes
				if (file.exists()) {
					String excontent = null;
					if (writtenFiles.contains(file)) {
						File nFile = toTmpFile(file);
						if (nFile.exists()) {
							excontent = FileUtils.readFileToString(nFile, encoding);
						} else {
							excontent = FileUtils.readFileToString(file, encoding);
						}
					} else {
						excontent = FileUtils.readFileToString(file, encoding);
					}
					
					if (content.equals(excontent)) {
						Engine.logEngine.trace("("+ builderType +") No change for " + file.getPath());
						return;
					}
				}
				
				// write file
				if (buildMutex == null) {
					if (file.getPath().endsWith(FakeDeleted)) {
						try {
							File dir = file.getParentFile();
							FileUtils.deleteDirectory(dir);
							Engine.logEngine.debug("("+ builderType +") Deleted dir " + dir.getPath());
						} catch (Exception e) {}
					} else {
						FileUtils.write(file, content, encoding);
						Engine.logEngine.debug("("+ builderType +") Wrote file " + file.getPath());
					}
				}
				// defers the write of file
				else {
					// write to temporary directory (for file edition)
					writtenFiles.add(file);
					File nFile = toTmpFile(file);
					nFile.getParentFile().mkdirs();
					FileUtils.write(nFile, content, encoding);
					
					// store files modifications
					if (file.getPath().endsWith(FakeDeleted)) {
						Engine.logEngine.trace("("+ builderType +") Defers the deletion of " + nFile.getParentFile().getPath());
					} else {
						Engine.logEngine.trace("("+ builderType +") Defers the write of " + content.length() + " chars to " + nFile.getPath());
					}
					if (pushedFiles != null) {
						synchronized (pushedFiles) {
							pushedFiles.put(file.getPath(), content);
						}
					}
				}
			}
		} else {
			if (file.getPath().endsWith(FakeDeleted)) {
				try {
					File dir = file.getParentFile();
					FileUtils.deleteDirectory(dir);
				} catch (Exception e) {}
			} else {
				FileUtils.write(file, content, encoding);
			}
		}
		
		writeWorker(file);
	}
	
	@Override
	protected synchronized void moveFilesForce() {
		if (pushedFiles != null) {
			
			if (!isReleasing) {
				updateConsumer(); // retrieve necessary external component files
			}
			
			synchronized (pushedFiles) {
				int size = pushedFiles.size();
				if (size > 0) {
					Engine.logEngine.debug("("+ builderType +") >>> moveFilesForce : "+ size +" files");
					Map<String, CharSequence> map = new HashMap<String, CharSequence>();
					map.putAll(Collections.unmodifiableMap(pushedFiles));
					pushedFiles.clear();
					
					List<String> pathList = new ArrayList<String>(map.keySet());
					Collections.sort(pathList, new AppFileComparator());
					
					boolean hasMovedFiles = false, hasDeletedFiles = false, hasMovedCfgFiles = false;
					
					// write or delete files
					for (String path: pathList) {
						File file = new File(path);
						if (file.getPath().endsWith(FakeDeleted)) {
							try {
								File dir = file.getParentFile();
								org.apache.commons.io.FileUtils.forceDelete(dir);//FileUtils.deleteDirectory(dir);
								Engine.logEngine.debug("("+ builderType +") Deleted dir " + dir.getPath());
								hasDeletedFiles = true;
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else {
							try {
								FileUtils.write(file, map.get(path), "UTF-8");
								Engine.logEngine.debug("("+ builderType +") Wrote file " + file.getPath());
								hasMovedFiles = true;
								if (path.endsWith("package.json") || path.endsWith("angular.json")) {
									hasMovedCfgFiles = true;
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					
					// update env file
					if (hasMovedFiles || hasDeletedFiles) {
						NgxBuilder.this.updateEnvFile();
					}

					// Need package installation
					if (hasMovedCfgFiles && getNeedPkgUpdate()) {
						NgxBuilder.this.firePackageUpdated();
					}
				}
			}
		}
	}

	@Override
	protected void writeWorker(File file, boolean bForce) throws IOException {
		if (initDone && buildMutex == null && Engine.isStudioMode()) {
			updateEnvFile();
		}
	}
}
