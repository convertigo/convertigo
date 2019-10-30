/*
 * Copyright (c) 2001-2019 Convertigo SA.
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.Contributor;
import com.twinsoft.convertigo.beans.mobile.components.IScriptComponent;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIActionStack;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.beans.mobile.components.UICustomAction;
import com.twinsoft.convertigo.beans.mobile.components.UISharedComponent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.MobileBuilderBuildMode;
import com.twinsoft.convertigo.engine.util.EventHelper;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;

public class MobileBuilder {
	class MbWorker implements Runnable {
		private BlockingQueue<Map<String, CharSequence>> wq;
		List<Map<String, CharSequence>> list = new ArrayList<Map<String, CharSequence>>();
		Map<String, CharSequence> map = new HashMap<String, CharSequence>();
		protected boolean isRunning = true;
		private boolean inProcess = false;
		Thread thread;
		
		protected MbWorker(BlockingQueue<Map<String, CharSequence>> queue) {
			this.wq = queue;
		}
		
		public void start() {
			if (thread == null) {
				thread = new Thread(worker);
				thread.setName("MbWorker-"+ project.getName());
				thread.start();
			}
		}
		
		public void join() throws InterruptedException {
			if (thread != null) {
				thread.join();
			}
		}
		
		void process() {
			if (!inProcess) {
				// retrieve all in queue
				list.clear();
				wq.drainTo(list);
				
				// process or not
				if (list.size() == 0) {
					try {
						Thread.sleep(100L);
					} catch (InterruptedException e) {}
				} else {
					inProcess = true;
					map.clear();
					for (Map<String, CharSequence> m: list) {
						map.putAll(m);
					}
					
					boolean hasMovedAppOrServFiles = false;
					boolean hasMovedPageFiles = false;
					boolean hasMovedCfgFiles = false;
					boolean hasMovedFiles = false;
					
					Engine.logEngine.debug("(MobileBuilder) Start to move " + map.size() + " files.");
					
					// FIRST: move all files
					for (String path: map.keySet()) {
						try {
							FileUtils.write(new File(path), map.get(path), "UTF-8");
							Engine.logEngine.debug("(MobileBuilder) Moved " + path);
							hasMovedFiles = true;
							
							if (isAppFile(path) || isServiceFile(path)) {
								hasMovedAppOrServFiles = true;
							}
							if (isPageFile(path)) {
								hasMovedPageFiles = true;
							}
							if (path.endsWith("package.json")) {
								hasMovedCfgFiles = true;
							}
						} catch (IOException e) {
							Engine.logEngine.warn("(MobileBuilder) Failed to copy the new content of " + path, e);
						}
					}
					Engine.logEngine.debug("(MobileBuilder) End to move " + map.size() + " files.");
					
					// Need package installation
					if (hasMovedCfgFiles && getNeedPkgUpdate()) {
						hasMovedFiles = false;
						MobileBuilder.this.firePackageUpdated();
					}
					
					if (hasMovedFiles) {
						if (buildMutex != null) {
							synchronized (buildMutex) {
								try {
									buildMutex.wait(60000);
								} catch (InterruptedException e) {}							
							}
							Engine.logEngine.debug("(MobileBuilder) build finished.");
						}
						
						// THEN: move again app or service files 
						if (hasMovedPageFiles && hasMovedAppOrServFiles) {
							int count = 0;
							for (String path: map.keySet()) {
								if (isAppFile(path) || isServiceFile(path)) {
									try {
										FileUtils.write(new File(path), map.get(path), "UTF-8");
										Engine.logEngine.debug("(MobileBuilder) Moved again " + path);
										count++;
									} catch (IOException e) {
										Engine.logEngine.warn("(MobileBuilder) Failed to copy the new content of " + path, e);
									}
								}
							}
							
							if (count > 0) {
								Engine.logEngine.debug("(MobileBuilder) End to move again " + count + " files.");
								
								if (buildMutex != null) {
									synchronized (buildMutex) {
										try {
											buildMutex.wait(60000);
										} catch (InterruptedException e) {}							
									}
									Engine.logEngine.debug("(MobileBuilder) build finished.");
								}
							}
						}
					}
					inProcess = false;
				}
			}
		}
		
		@Override
		public void run() {
			while (isRunning) {
				try {
					process();
				} catch (Throwable t) {
					Engine.logEngine.error("(MobileBuilder) Throwable catched", t);
				} finally {
					inProcess = false;
				}
			}
		}
	}
	
	private static Pattern LsPattern = Pattern.compile("\\R");
	private static Pattern CacheVersion = Pattern.compile("const\\sCACHE_VERSION\\s\\=\\s\\d+");
	
	private BlockingQueue<Map<String, CharSequence>> queue = null;
	private Map<String, CharSequence> pushedFiles = null;
	private MbWorker worker = null;
	
	private Project project = null;
	private boolean[] isBuilding = {false};
	boolean needPkgUpdate = false;
	boolean initDone = false;
	boolean autoBuild = true;
	Object buildMutex = null;
	
	MobileBuilderBuildMode buildMode = MobileBuilderBuildMode.fast;
	
	// Until we can delete page folder again, we need to retrieve contributors of
	// all pages for action.beans.service, otherwise we get compilation errors in
	// page.ts files for (deleted/disabled) pages containing pseudo-actions
	boolean forceEnable = true;
	
	Map<String,String> tpl_appCompTsImports = null;
	Map<String,String> tpl_pageTsImports = null;
	Map<String,String> tpl_appModuleTsImports = null;
	Map<String,String> tpl_pageModuleTsImports = null;
	Map<String,String> tpl_serviceActionTsImports = null;
	String tpl_appModuleNgImports = null;
	String tpl_appModuleNgProviders = null;
	String tpl_appModuleNgDeclarations = null;
	String tpl_appModuleNgComponents = null;
	String tpl_pageModuleNgImports = null;
	String tpl_pageModuleNgProviders = null;
	String tpl_pageModuleNgDeclarations = null;
	String tpl_pageModuleNgComponents = null;
	String tplVersion = null;
	
	boolean isPWA = false;
	
	Set<File> writtenFiles = new HashSet<File>();
	
	File projectDir, ionicTplDir, ionicWorkDir;
	
	static private boolean isAppFile(String path) {
		String search = File.separator + "src" + File.separator + "app" + File.separator;
		return path == null ? false : path.indexOf(search) != -1;
	}
	
	static private boolean isPageFile(String path) {
		String search = File.separator + "src" + File.separator + "pages" + File.separator;
		return path == null ? false : path.indexOf(search) != -1;
	}
	
	static private boolean isServiceFile(String path) {
		String search = File.separator + "src" + File.separator + "services" + File.separator;
		return path == null ? false : path.indexOf(search) != -1;
	}
		
	static public void initBuilder(Project project) {
		if ((Engine.isStudioMode() || Engine.isCliMode()) && project != null && project.getMobileApplication() != null && project.getMobileApplication().getApplicationComponent() != null) {
			try {
				project.getMobileBuilder().init();
			} catch (Exception e) {
				String message = e.getMessage();
				if (message.startsWith("Missing template project") || message.contains("is required for this")) {
					Engine.logEngine.error("Failed to initialize mobile builder for project '" + project.getName() + "'\n" + message);
				} else {
					Engine.logEngine.error("Failed to initialize mobile builder for project '" +project.getName() + "'", e);
				}
			}
		}
	}
	
	static public void releaseBuilder(Project project) {
		if (Engine.isStudioMode() && project != null && project.getMobileApplication() != null && project.getMobileApplication().getApplicationComponent() != null) {
			try {
				project.getMobileBuilder().release();
			} catch (Exception e) {
				Engine.logEngine.error("Failed to release mobile builder for project \""+project.getName()+"\"", e);
			}
		}
	}
	
	public MobileBuilder(Project project) {
		this.project = project;
		
		projectDir = new File(project.getDirPath());
		ionicWorkDir = new File(projectDir,"_private/ionic");
	}
	
	private EventHelper eventHelper;
	
	public synchronized void addMobileEventListener(MobileEventListener mobileEventListener) {
		if (eventHelper != null) {
			eventHelper.addListener(MobileEventListener.class, mobileEventListener);
		}
	}
	
	public synchronized void removeMobileEventListener(MobileEventListener mobileEventListener) {
		if (eventHelper != null) {
			eventHelper.removeListener(MobileEventListener.class, mobileEventListener);
		}
	}

	public synchronized void firePackageUpdated() {
		if (eventHelper != null) {
			for (MobileEventListener mobileEventListener: eventHelper.getListeners(MobileEventListener.class)) {
				mobileEventListener.onPackageUpdated();
			}
		}
	}

	public void setBuildMutex(Object mutex) {
		buildMutex = mutex;
		FileUtils.deleteQuietly(new File(projectDir,"_private/ionic_tmp"));
	}
	
	public void setAppBuildMode(MobileBuilderBuildMode buildMode) {
		this.buildMode = buildMode;
		
		if (project != null) {
			try {
				project.getMobileApplication().getApplicationComponent().markComponentTsAsDirty();
			} catch (Exception e) {
				Engine.logEngine.warn("(MobileBuilder) enabled to change build mode");
			}
		}
	}
	
	public void setNeedPkgUpdate(boolean needPkgUpdate) {
		this.needPkgUpdate = needPkgUpdate;
	}
	
	public boolean getNeedPkgUpdate() {
		return this.needPkgUpdate;
	}
	
	public boolean hasNodeModules() {
		File nodeModulesDir = new File(ionicWorkDir,"node_modules");
		return nodeModulesDir.exists();
	}
		
	public synchronized void appPwaChanged(final ApplicationComponent app) throws EngineException {
		if (app != null && initDone) {
			configurePwaApp(app);
			moveFiles();
			Engine.logEngine.trace("(MobileBuilder) Handled 'appPwaChanged'");
		}
	}
	
	public synchronized void appRootChanged(final ApplicationComponent app) throws EngineException {
		if (app != null && initDone) {
			writeAppComponentTs(app);
			moveFiles();
			
			File appComponentTsFile = new File(ionicWorkDir, "src/app/app.component.temp.ts");
			if (appComponentTsFile.exists()) {
				writeAppComponentTempTs(app);
			}
			Engine.logEngine.trace("(MobileBuilder) Handled 'appRootChanged'");
		}
	}

	public synchronized void appRouteChanged(final ApplicationComponent app) throws EngineException {
		if (app != null && initDone) {
			writeAppComponentTs(app);
			moveFiles();
			
			File appComponentTsFile = new File(ionicWorkDir, "src/app/app.component.temp.ts");
			if (appComponentTsFile.exists()) {
				writeAppComponentTempTs(app);
			}
			Engine.logEngine.trace("(MobileBuilder) Handled 'appRouteChanged'");
		}
	}
	
	private void addPage(PageComponent page) throws EngineException {
		MobileApplication mobileApplication = project.getMobileApplication();
		if (mobileApplication != null) {
			ApplicationComponent application = mobileApplication.getApplicationComponent();
			if (application != null) {
				writePageSourceFiles(page);
				writeAppSourceFiles(application);
			}
		}
	}
	
	public synchronized void pageEnabled(final PageComponent page) throws EngineException {
		if (page != null && page.isEnabled() && initDone) {
			addPage(page);
			moveFiles();
			Engine.logEngine.trace("(MobileBuilder) Handled 'pageEnabled'");
		}
	}
	
	public synchronized void pageDisabled(final PageComponent page) throws EngineException {
		if (page != null && !page.isEnabled() && initDone) {
			MobileApplication mobileApplication = project.getMobileApplication();
			if (mobileApplication != null) {
				ApplicationComponent application = mobileApplication.getApplicationComponent();
				if (application != null) {
					writePageSourceFiles(page);
					writeAppSourceFiles(application);
					moveFiles();
					Engine.logEngine.trace("(MobileBuilder) Handled 'pageDisabled'");
				}
			}
		}
	}

	public synchronized void pageAdded(final PageComponent page) throws EngineException {
		if (page != null && page.isEnabled() && page.bNew && initDone) {
			addPage(page);
			moveFiles();
			Engine.logEngine.trace("(MobileBuilder) Handled 'pageAdded'");
		}
	}
	
	public synchronized void pageRemoved(final PageComponent page) throws EngineException {
		if (page != null && page.isEnabled() && initDone) {
			MobileApplication mobileApplication = project.getMobileApplication();
			if (mobileApplication != null) {
				ApplicationComponent application = mobileApplication.getApplicationComponent();
				if (application != null) {
					writeAppSourceFiles(application);
					deleteUselessDir(page.getName());
					moveFiles();
					Engine.logEngine.trace("(MobileBuilder) Handled 'pageRemoved'");
				}
			}
		}
	}
	
	public synchronized void pageRenamed(final PageComponent page, final String oldName) throws EngineException {
		if (page != null && page.isEnabled() && initDone) {
			MobileApplication mobileApplication = project.getMobileApplication();
			if (mobileApplication != null) {
				ApplicationComponent application = mobileApplication.getApplicationComponent();
				if (application != null) {
					writePageSourceFiles(page);
					writeAppSourceFiles(application);
					deleteUselessDir(oldName);
					moveFiles();
					Engine.logEngine.trace("(MobileBuilder) Handled 'pageRenamed'");
				}
			}
		}
	}
	
	public synchronized void pageTemplateChanged(final PageComponent page) throws EngineException {
		if (page != null && page.isEnabled() && initDone) {
			writePageTemplate(page);
			moveFiles();
			Engine.logEngine.trace("(MobileBuilder) Handled 'pageTemplateChanged'");
		}
	}
	
	public synchronized void pageStyleChanged(final PageComponent page) throws EngineException {
		if (page != null && page.isEnabled() && initDone) {
			writePageStyle(page);
			moveFiles();
			Engine.logEngine.trace("(MobileBuilder) Handled 'pageStyleChanged'");
		}
	}

	public synchronized void appContributorsChanged(final ApplicationComponent app) throws EngineException {
		if (app != null && initDone) {
			writeAppPackageJson(app);
			writeAppPluginsConfig(app);
			writeAppServiceTs(app);
			writeAppModuleTs(app);
			moveFiles();
			Engine.logEngine.trace("(MobileBuilder) Handled 'appContributorsChanged'");
		}
	}
	
	public synchronized void pageTsChanged(final PageComponent page, boolean forceTemp) throws EngineException {
		if (page != null && page.isEnabled() && initDone) {
			writePageTs(page);
			moveFiles();
			
			String pageName = page.getName();
			File pageDir = new File(ionicWorkDir, "src/pages/"+pageName);
			File tempTsFile = new File(pageDir, pageName.toLowerCase() + ".temp.ts");
			if (forceTemp && tempTsFile.exists()) {
				writePageTempTs(page);
			}
			
			Engine.logEngine.trace("(MobileBuilder) Handled 'pageTsChanged'");
		}
	}

	public synchronized void pageModuleTsChanged(final PageComponent page) throws EngineException {
		if (page != null && page.isEnabled() && initDone) {
			writePageModuleTs(page);
			moveFiles();
			Engine.logEngine.trace("(MobileBuilder) Handled 'pageModuleTsChanged'");
		}
	}
	
	public synchronized void appTsChanged(final ApplicationComponent app, boolean forceTemp) throws EngineException {
		if (app != null && initDone) {
			writeAppComponentTs(app);
			moveFiles();
			
			File tempTsFile = new File(ionicWorkDir, "src/app/app.component.temp.ts");
			if (forceTemp && tempTsFile.exists()) {
				writeAppComponentTempTs(app);
			}
			
			Engine.logEngine.trace("(MobileBuilder) Handled 'appTsChanged'");
		}
	}

	public synchronized void appStyleChanged(final ApplicationComponent app) throws EngineException {
		if (app != null && initDone) {
			writeAppStyle(app);
			moveFiles();
			Engine.logEngine.trace("(MobileBuilder) Handled 'appStyleChanged'");
		}
	}

	public synchronized void appTemplateChanged(final ApplicationComponent app) throws EngineException {
		if (app != null && initDone) {
			writeAppTemplate(app);
			moveFiles();
			Engine.logEngine.trace("(MobileBuilder) Handled 'appTemplateChanged'");
		}
	}
	
	public synchronized void appThemeChanged(final ApplicationComponent app) throws EngineException {
		if (app != null && initDone) {
			writeAppTheme(app);
			moveFiles();
			Engine.logEngine.trace("(MobileBuilder) Handled 'appThemeChanged'");
		}
	}
	
	public synchronized void appCompTsChanged(final ApplicationComponent app) throws EngineException {
		if (app != null && initDone) {
			writeAppComponentTs(app);
			moveFiles();
			Engine.logEngine.trace("(MobileBuilder) Handled 'appCompTsChanged'");
		}
	}
	
	public synchronized void appModuleTsChanged(final ApplicationComponent app) throws EngineException {
		if (app != null && initDone) {
			writeAppModuleTs(app);
			moveFiles();
			Engine.logEngine.trace("(MobileBuilder) Handled 'appModuleTsChanged'");
		}
	}
	
	public boolean isIonicTemplateBased() {
		return ionicTplDir.exists();
	}
	
	private synchronized void init() throws EngineException {
		if (initDone) {
			return;
		}
		
		ApplicationComponent application = project.getMobileApplication().getApplicationComponent();
		
		ionicTplDir = application.getIonicTplDir();
		if (!ionicTplDir.exists()) {
			throw new EngineException("Missing template project '" + application.getTplProjectName() + "'\nThe template folder should be in: " + ionicTplDir.getPath());
		}
		
		if (isIonicTemplateBased()) {
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
			
			// PWA
			configurePwaApp(application);
			
			// Write source files (based on bean components)
			updateSourceFiles();

			// Studio mode : start worker for build process
			if (Engine.isStudioMode() || Engine.isCliMode()) {
				if (pushedFiles == null) {
					pushedFiles = new HashMap<String, CharSequence>();
				}
				
				if (queue == null) {
					queue = new LinkedBlockingQueue<Map<String, CharSequence>>();
				}
				
				if (worker == null) {
					worker = new MbWorker(queue);
					if (Engine.isStudioMode()) {
						worker.start();
					} else {
						worker.process();
					}
				}
			}
						
			initDone = true;
			Engine.logEngine.debug("(MobileBuilder) Initialized builder for ionic project '"+ project.getName() +"'");
		}
	}
	
	private synchronized void release() throws EngineException {
		/*if (!initDone) {
			return;
		}*/
		
		if (isIonicTemplateBased()) {
			moveFilesForce();
			
			if (worker != null) {
				worker.isRunning = false;
				try {
					worker.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				worker = null;
			}
			
			if (queue != null) {
				try {
					queue.clear();
				} catch (Exception e) {
					e.printStackTrace();
				}
				queue = null;
			}
			
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
			
			initDone = false;
			Engine.logEngine.debug("(MobileBuilder) Released builder for ionic project '"+ project.getName() +"'");
		}
	}
	
	private void cleanDirectories() {
		FileUtils.deleteQuietly(new File(projectDir,"_private/ionic/src"));
		FileUtils.deleteQuietly(new File(projectDir,"_private/ionic/version.json"));
		FileUtils.deleteQuietly(new File(projectDir,"_private/ionic_tmp"));
		Engine.logEngine.trace("(MobileBuilder) Directories cleaned for ionic project '"+ project.getName() +"'");
	}
	
	private void copyTemplateFiles() throws EngineException {
		try {
			FileUtils.copyDirectory(ionicTplDir, ionicWorkDir);
			Engine.logEngine.trace("(MobileBuilder) Template files copied for ionic project '"+ project.getName() +"'");
		}
		catch (Exception e) {
			throw new EngineException("Unable to copy ionic template files for ionic project '"+ project.getName() +"'",e);
		}
	}
	
	private void copyAssetsToBuildDir() throws EngineException {
		try {
			File tAssets = new File(ionicTplDir, "src/assets");
			File bAssets = new File(ionicWorkDir, "../../DisplayObjects/mobile/assets");
			FileUtils.copyDirectory(tAssets, bAssets);
			Engine.logEngine.trace("(MobileBuilder) Assets files copied for ionic project '"+ project.getName() +"'");
		}
		catch (Exception e) {
			throw new EngineException("Unable to copy ionic assats files for ionic project '"+ project.getName() +"'",e);
		}
	}
	
	private void updateConfigurationFiles() throws EngineException {
		try {
			IOFileFilter fileFilter = FileFilterUtils.or(FileFilterUtils.suffixFileFilter("json"),FileFilterUtils.suffixFileFilter("xml"),FileFilterUtils.suffixFileFilter("js"));
			IOFileFilter dirFilter = FileFilterUtils.or(FileFilterUtils.nameFileFilter("config"));
			for (File f: FileUtils.listFiles(ionicWorkDir, fileFilter, dirFilter)) {
				String content = FileUtils.readFileToString(f, "UTF-8");
				content = content.replaceAll("\\.\\./DisplayObjects","../../DisplayObjects");
				content = content.replaceAll("\\.\\./Flashupdate","../../Flashupdate");
				// prevent assets copy : already done by copyAssetsToBuildDir()
				content = content.replaceAll("/assets/\\*\\*/\\*","/_fake_/assets/**/*");
				writeFile(f, content, "UTF-8");
			}
			Engine.logEngine.trace("(MobileBuilder) Configuration files updated for ionic project '"+ project.getName() +"'");
		}
		catch (Exception e) {
			throw new EngineException("Unable to update configuration files for ionic project '"+ project.getName() +"'",e);
		}
	}
	
	private List<PageComponent> getEnabledPages(final ApplicationComponent application) {
		List<PageComponent> pages = new ArrayList<PageComponent>();
		for (PageComponent page : application.getPageComponentList()) {
			if (page.isEnabled()) {
				pages.add(page);
			}
		}
		return pages;
	}
	
	static public int compareVersions(String v1, String v2) {
		String s1 = VersionUtils.normalizeVersionString(v1.trim().toLowerCase(), ".", 4);
		String s2 = VersionUtils.normalizeVersionString(v2.trim().toLowerCase(), ".", 4);
		int cmp = s1.compareTo(s2);
		return cmp;
	}
	
	private void updateSourceFiles() throws EngineException {
		try {
			MobileApplication mobileApplication = project.getMobileApplication();
			if (mobileApplication != null) {
				ApplicationComponent application = mobileApplication.getApplicationComponent();
				if (application != null) {
					String appTplVersion = application.requiredTplVersion();
					if (compareVersions(tplVersion, appTplVersion) >= 0) {
						for (PageComponent page : getEnabledPages(application)) {
							writePageSourceFiles(page);
						}
						writeAppSourceFiles(application);
						removeUselessPages(application);
						
						Engine.logEngine.trace("(MobileBuilder) Application source files updated for ionic project '"+ project.getName() +"'");
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
			if (page != null && page.isEnabled()) {
				String pageName = page.getName();
				File pageDir = new File(ionicWorkDir, "src/pages/"+pageName);
				File pageHtmlFile = new File(pageDir, pageName.toLowerCase() + ".html");
				String computedTemplate = page.getComputedTemplate();
				writeFile(pageHtmlFile, computedTemplate, "UTF-8");
				
				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Ionic template file generated for page '"+pageName+"'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page template file",e);
		}
	}

	private void writePageStyle(PageComponent page) throws EngineException {
		try {
			if (page != null && page.isEnabled()) {
				String pageName = page.getName();
				File pageDir = new File(ionicWorkDir, "src/pages/"+pageName);
				File pageScssFile = new File(pageDir, pageName.toLowerCase() + ".scss");
				String computedScss = page.getComputedStyle();
				writeFile(pageScssFile, computedScss, "UTF-8");
				
				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Ionic scss file generated for page '"+pageName+"'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page scss file",e);
		}
	}
	
	public String getTempTsRelativePath(PageComponent page) throws EngineException {
		try {
			if (page != null) {
				String pageName = page.getName();
				File pageDir = new File(ionicWorkDir, "src/pages/"+pageName);
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
	
	public String getFunctionTempTsRelativePath(UIComponent uic) {
		IScriptComponent main = uic.getMainScriptComponent();
		if (main != null) {
			if (main instanceof ApplicationComponent) {
				UIActionStack stack = uic.getSharedAction();
				File tempTsFile = stack == null ? new File(ionicWorkDir, "src/app/app.component.function.temp.ts") :
													new File(ionicWorkDir, "src/services/actionbeans.service.function.temp.ts");
				return tempTsFile.getPath().replace(projectDir.getPath(), File.separator);
			}
			if (main instanceof PageComponent) {
				PageComponent page = (PageComponent)main;
				String pageName = page.getName();
				File pageDir = new File(ionicWorkDir, "src/pages/"+pageName);
				File tempTsFile = new File(pageDir, pageName.toLowerCase() + ".function.temp.ts");
				return tempTsFile.getPath().replace(projectDir.getPath(), File.separator);
			}
		}
		return null;
	}
	
	public void writeFunctionTempTsFile(UIComponent uic, String functionMarker) throws EngineException {
		try {
			IScriptComponent main = uic.getMainScriptComponent();
			if (main != null) {
				String tempTsFileName = null, tsContent = null;
				File tempTsDir = null;
				UIActionStack sharedAction = null;
				UISharedComponent sharedComp = null;
				
				if (main instanceof ApplicationComponent) {
					sharedAction = uic.getSharedAction();
					sharedComp = uic.getSharedComponent();
					
					tempTsDir = sharedAction == null ? new File(ionicWorkDir, "src/app") : new File(ionicWorkDir, "src/services");
					tempTsFileName = sharedAction == null ? "app.component.function.temp.ts" : "actionbeans.service.function.temp.ts";
					
					File appTsFile = sharedAction == null ? new File(ionicWorkDir, "src/app/app.component.ts") : 
														new File(ionicWorkDir, "src/services/actionbeans.service.ts");
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
					tempTsDir = new File(ionicWorkDir, "src/pages/"+pageName);
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
				
				// Replace all Begin_c8o_XXX, End_c8o_XXX except for functionMarker
				boolean found = false;
				Pattern pattern = Pattern.compile("/\\*Begin_c8o_(.+)\\*/");
				Matcher matcher = pattern.matcher(tsContent);
				while (matcher.find()) {
					String markerId = matcher.group(1);
					if (!markerId.equals(functionMarker)) {
						String beginMarker = "/*Begin_c8o_" + markerId + "*/";
						String endMarker = "/*End_c8o_" + markerId + "*/";
						tsContent = tsContent.replace(beginMarker, "//---"+markerId+"---");
						tsContent = tsContent.replace(endMarker, "//---"+markerId+"---");
					} else {
						found = true;
					}
				}
				
				// Always be able to edit a CustomAction within a SharedAction or a SharedComponent
				if (!found && uic instanceof UICustomAction && (sharedAction != null || sharedComp != null)) {
					UICustomAction uica = (UICustomAction)uic;
					Contributor contributor = uica.getActionContributor();
					int index = -1;
					
					Map<String, String> mapi = contributor.getActionTsImports();
					String imports = "";
					for (String comp : mapi.keySet()) {
						if (!getTplServiceActionTsImports().containsKey(comp)) {
							if (comp.indexOf(" as ") == -1)
								imports += "import { "+comp+" } from '"+ mapi.get(comp) +"';"+ System.lineSeparator();
							else
								imports += "import "+comp+" from '"+ mapi.get(comp) +"';"+ System.lineSeparator();
						}
					}
					index = tsContent.indexOf("@Injectable");
					if (index != -1) {
						tsContent = tsContent.substring(0, index -1) + System.lineSeparator() 
							+ Matcher.quoteReplacement(imports) + System.lineSeparator()
							+ tsContent.substring(index);
					}
					
					Map<String, String> mapf = contributor.getActionTsFunctions();
					String code = mapf.get(uica.getActionName());
					index = tsContent.lastIndexOf("}");
					if (index != -1) {
						tsContent = tsContent.substring(0, index -1) + System.lineSeparator() 
									+ code + System.lineSeparator()
									+ tsContent.substring(index);
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
	
	public void writePageTempTs(PageComponent page) throws EngineException {
		try {
			if (page != null) {
				String pageName = page.getName();
				File pageDir = new File(ionicWorkDir, "src/pages/"+pageName);
				
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
				
				// Write file (do not need delay)
				tsContent = LsPattern.matcher(tsContent).replaceAll(System.lineSeparator());
				File tempTsFile = new File(pageDir, pageName.toLowerCase() + ".temp.ts");
				FileUtils.write(tempTsFile, tsContent, "UTF-8");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page temp ts file",e);
		}
	}
	
	private void writePageTs(PageComponent page) throws EngineException {
		try {
			if (page != null && page.isEnabled()) {
				String pageName = page.getName();
				File pageDir = new File(ionicWorkDir, "src/pages/"+pageName);
				File pageTsFile = new File(pageDir, pageName.toLowerCase() + ".ts");
				writeFile(pageTsFile, getPageTsContent(page), "UTF-8");
				
				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Ionic ts file generated for page '"+pageName+"'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page ts file",e);
		}
	}
	
	private void writePageModuleTs(PageComponent page) throws EngineException {
		try {
			if (page != null && page.isEnabled()) {
				if (page.compareToTplVersion("7.7.0.2") >= 0) {
					String pageName = page.getName();
					File pageDir = new File(ionicWorkDir, "src/pages/"+pageName);
					File pageModuleTsFile = new File(pageDir, pageName.toLowerCase() + ".module.ts");
					writeFile(pageModuleTsFile, getPageModuleTsContent(page), "UTF-8");
					
					if (initDone) {
						Engine.logEngine.trace("(MobileBuilder) Ionic module file generated for page '"+pageName+"'");
					}
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page module file",e);
		}
	}
	
	private void updateTplVersion() {
		if (tplVersion == null) {
			File versionJson = new File(ionicWorkDir, "version.json"); // since 7.5.2
			if (versionJson.exists()) {
				try {
					String tsContent = FileUtils.readFileToString(versionJson, "UTF-8");
					JSONObject jsonOb = new JSONObject(tsContent);
					tplVersion = jsonOb.getString("version");
					Engine.logEngine.debug("(MobileBuilder) Template version: "+ tplVersion+ " for ionic project '"+ project.getName() +"'");
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				File pkgJson = new File(ionicWorkDir, "package.json"); 
				if (pkgJson.exists()) {
					try {
						String tsContent = FileUtils.readFileToString(pkgJson, "UTF-8");
						JSONObject jsonOb = new JSONObject(tsContent);
						JSONObject jsonDeps = jsonOb.getJSONObject("dependencies");
						tplVersion = jsonDeps.getString("c8ocaf");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public String getTplVersion() {
		return tplVersion;
	}
	
	
	public boolean hasTplAppCompTsImport(String name) {
		return getTplAppCompTsImports().containsKey(name);
	}
	
	public boolean hasTplPageTsImport(String name) {
		return getTplPageTsImports().containsKey(name);
	}
	
	private Map<String,String> getTplAppCompTsImports() {
		if (tpl_appCompTsImports == null) {
			tpl_appCompTsImports = initTplImports(new File(ionicTplDir, "src/app/app.component.ts"));
		}
		return tpl_appCompTsImports;
	}
	
	private Map<String,String> getTplPageTsImports() {
		if (tpl_pageTsImports == null) {
			tpl_pageTsImports = initTplImports(new File(ionicTplDir, "src/page.tpl"));
		}
		return tpl_pageTsImports;
	}
	
	private Map<String,String> getTplAppModuleTsImports() {
		if (tpl_appModuleTsImports == null) {
			tpl_appModuleTsImports = initTplImports(new File(ionicTplDir, "src/app/app.module.ts"));
		}
		return tpl_appModuleTsImports;
	}

	private Map<String,String> getTplPageModuleTsImports() {
		if (tpl_appModuleTsImports == null) {
			tpl_appModuleTsImports = initTplImports(new File(ionicTplDir, "src/page.module.tpl"));
		}
		return tpl_appModuleTsImports;
	}
	
	private String getTplAppModuleNgImports() {
		if (tpl_appModuleNgImports == null) {
			try {
				String tsContent = FileUtils.readFileToString(new File(ionicTplDir, "src/app/app.module.ts"), "UTF-8");
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
	
	private String getTplAppModuleNgProviders() {
		if (tpl_appModuleNgProviders == null) {
			try {
				String tsContent = FileUtils.readFileToString(new File(ionicTplDir, "src/app/app.module.ts"), "UTF-8");
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
	
	private Map<String,String> getTplServiceActionTsImports() {
		if (tpl_serviceActionTsImports == null) {
			tpl_serviceActionTsImports = initTplImports(new File(ionicTplDir, "src/services/actionbeans.service.ts"));
		}
		return tpl_serviceActionTsImports;
	}

	public static void initMapImports(Map<String,String> map, String tsContent) {
		if (map != null && tsContent != null) {
			// case : import {...} from '...'
			Pattern pattern = Pattern.compile("[\\s\\t]*import[\\s\\t]*\\{(.*?)\\}[\\s\\t]*from[\\s\\t]*['\"](.*?)['\"]", Pattern.DOTALL);
			Matcher matcher = pattern.matcher(tsContent);
			while (matcher.find()) {
				String names = matcher.group(1);
				String path = matcher.group(2);
				for (String name : names.split(",")) {
					name = name.trim();
					if (!map.containsKey(name)) {
						map.put(name, path);
					}
				}
			}
			
			// case : import ... as ... from '...'
			Pattern pattern1 = Pattern.compile("[\\s\\t]*import[\\s\\t]*([^\\{\\}]*?)[\\s\\t]*from[\\s\\t]*['\"](.*?)['\"]", Pattern.DOTALL);
			Matcher matcher1 = pattern1.matcher(tsContent);
			while (matcher1.find()) {
				String names = matcher1.group(1);
				String path = matcher1.group(2);
				for (String name : names.split(",")) {
					name = name.trim();
					if (!map.containsKey(name)) {
						map.put(name, path);
					}
				}
			}
		}
	}
	
	private static Map<String,String> initTplImports(File file) {
		Map<String, String> map = new HashMap<String, String>(10);
		try {
			String tsContent = FileUtils.readFileToString(file, "UTF-8");
			initMapImports(map, tsContent);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}
	
	private String getPageTsContent(PageComponent page) throws IOException {
		String pageName = page.getName();
		String c8o_PageName = pageName;
		//String c8o_PageIonicName = pageName;
		String c8o_PageHistory = page.getDefaultHistory();
		String c8o_PagePriority = page.getPreloadPriority();
		String c8o_PageSegment = page.getSegment();
		String c8o_PageTplUrl = pageName.toLowerCase() + ".html";
		String c8o_PageSelector = "page-"+pageName.toLowerCase();
		String c8o_PageImports = page.getComputedImports();
		String c8o_PageDeclarations = page.getComputedDeclarations();
		String c8o_PageConstructors = page.getComputedConstructors();
		String c8o_PageFunctions = page.getComputedFunctions();
		String c8o_UserCustoms = page.getScriptContent().getString();
		
		File pageTplTs = new File(ionicTplDir, "src/page.tpl");
		String tsContent = FileUtils.readFileToString(pageTplTs, "UTF-8");
		//tsContent = tsContent.replaceAll("/\\*\\=c8o_PageIonicName\\*/","'"+c8o_PageIonicName+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PagePriority\\*/","'"+c8o_PagePriority+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageSegment\\*/","'"+c8o_PageSegment+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageHistory\\*/",c8o_PageHistory);
		
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageSelector\\*/","'"+c8o_PageSelector+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageTplUrl\\*/","'"+c8o_PageTplUrl+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageName\\*/",c8o_PageName);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageImports\\*/",c8o_PageImports);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageDeclarations\\*/",c8o_PageDeclarations);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageConstructors\\*/",c8o_PageConstructors);
		
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
		
		//tsContent = tsContent.replaceAll("/\\*\\=c8o_PageFunctions\\*/",c8o_PageFunctions);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageFunctions\\*/", Matcher.quoteReplacement(c8o_PageFunctions));
		
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
			comp_beans_dirs.putAll(contributor.getCompBeanDir());
			
			module_ts_imports.putAll(contributor.getModuleTsImports());
			module_ng_imports.addAll(contributor.getModuleNgImports());
			module_ng_providers.addAll(contributor.getModuleNgProviders());
			module_ng_declarations.addAll(contributor.getModuleNgDeclarations());
			module_ng_components.addAll(contributor.getModuleNgComponents());
		}

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
				c8o_ModuleNgImports = System.lineSeparator() + c8o_ModuleNgImports;
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
		String c8o_PageImport = "import { "+pageName+" } from \"./"+pageName.toLowerCase()+"\";" + System.lineSeparator();
		
		File pageTplTs = new File(ionicTplDir, "src/page.module.tpl");
		String tsContent = FileUtils.readFileToString(pageTplTs, "UTF-8");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageName\\*/",c8o_PageName);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageModuleName\\*/",c8o_PageModuleName);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageImport\\*/",c8o_PageImport);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_ModuleTsImports\\*/",c8o_ModuleTsImports);
		tsContent = tsContent.replaceAll("/\\*Begin_c8o_NgModules\\*/",c8o_ModuleNgImports);
		tsContent = tsContent.replaceAll("/\\*End_c8o_NgModules\\*/","");
		tsContent = tsContent.replaceAll("/\\*Begin_c8o_NgProviders\\*/",c8o_ModuleNgProviders);
		tsContent = tsContent.replaceAll("/\\*End_c8o_NgProviders\\*/","");
		tsContent = tsContent.replaceAll("/\\*Begin_c8o_NgDeclarations\\*/",c8o_ModuleNgDeclarations);
		tsContent = tsContent.replaceAll("/\\*End_c8o_NgDeclarations\\*/","");
		tsContent = tsContent.replaceAll("/\\*Begin_c8o_NgComponents\\*/",c8o_ModuleNgComponents);
		tsContent = tsContent.replaceAll("/\\*End_c8o_NgComponents\\*/","");
		
		for (String compbean : comp_beans_dirs.keySet()) {
			File srcDir = comp_beans_dirs.get(compbean);
			for (File f: srcDir.listFiles()) {
				String fContent = FileUtils.readFileToString(f, "UTF-8");
				File destFile = new File(ionicWorkDir, "src/components/"+ compbean+ "/"+ f.getName());
				writeFile(destFile, fContent, "UTF-8");
			}
		}
		
		return tsContent;
	}
	
	public String getTempTsRelativePath(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				File appComponentTsFile = new File(ionicWorkDir, "src/app/app.component.temp.ts");
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
					List<Contributor> contributors = page.getContributors();
					for (Contributor contributor : contributors) {
						cfg_plugins.putAll(contributor.getConfigPlugins());
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
				
				File appPlgConfig = new File(ionicWorkDir, "src/plugins.txt");
				writeFile(appPlgConfig, mandatoryPlugins, "UTF-8");
				
				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) App plugins config file generated");
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
					List<Contributor> contributors = page.getContributors();
					for (Contributor contributor : contributors) {
						pkg_dependencies.putAll(contributor.getPackageDependencies());
					}
				}
				
				File appPkgJsonTpl = new File(ionicTplDir, "package.json");
				String mContent = FileUtils.readFileToString(appPkgJsonTpl, "UTF-8");
				mContent = mContent.replaceAll("../DisplayObjects","../../DisplayObjects");
				
				JSONObject jsonPackage = new JSONObject(mContent);
				JSONObject jsonDependencies = jsonPackage.getJSONObject("dependencies");
				for (String pkg : pkg_dependencies.keySet()) {
					jsonDependencies.put(pkg, pkg_dependencies.get(pkg));
					if (!existPackage(pkg)) {
						setNeedPkgUpdate(true);
					}
				}
				
				File appPkgJson = new File(ionicWorkDir, "package.json");
				writeFile(appPkgJson, jsonPackage.toString(2), "UTF-8");
				
				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Ionic package json file generated");
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
				
				//Menus contributors
				for (Contributor contributor : app.getContributors()) {
					action_ts_imports.putAll(contributor.getActionTsImports());
					action_ts_functions.putAll(contributor.getActionTsFunctions());
				}
				
				//Pages contributors
				List<PageComponent> pages = forceEnable ? 
												app.getPageComponentList() :
														getEnabledPages(app);
				for (PageComponent page : pages) {
					List<Contributor> contributors = page.getContributors();
					for (Contributor contributor : contributors) {
						action_ts_imports.putAll(contributor.getActionTsImports());
						action_ts_functions.putAll(contributor.getActionTsFunctions());
					}
				}
				
				String c8o_ActionTsImports = "";
				for (String comp : action_ts_imports.keySet()) {
					if (!getTplServiceActionTsImports().containsKey(comp)) {
						if (comp.indexOf(" as ") == -1)
							c8o_ActionTsImports += "import { "+comp+" } from '"+ action_ts_imports.get(comp) +"';"+ System.lineSeparator();
						else
							c8o_ActionTsImports += "import "+comp+" from '"+ action_ts_imports.get(comp) +"';"+ System.lineSeparator();
					}
				}
				
				String c8o_ActionTsFunctions = System.lineSeparator();
				for (String function : action_ts_functions.values()) {
					c8o_ActionTsFunctions += function + System.lineSeparator();
				}
				
				File appServiceTpl = new File(ionicTplDir, "src/services/actionbeans.service.ts");
				String mContent = FileUtils.readFileToString(appServiceTpl, "UTF-8");
				mContent = mContent.replaceAll("/\\*\\=c8o_ActionTsImports\\*/",Matcher.quoteReplacement(c8o_ActionTsImports));
				mContent = mContent.replaceAll("/\\*\\=c8o_ActionTsFunctions\\*/",Matcher.quoteReplacement(c8o_ActionTsFunctions));
				File appServiceTsFile = new File(ionicWorkDir, "src/services/actionbeans.service.ts");
				writeFile(appServiceTsFile, mContent, "UTF-8");
				
				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Ionic service ts file generated for 'app'");
				}
			}
		} catch (Exception e) {
			throw new EngineException("Unable to write ionic app service ts file",e);
		}
	}
	
	private void writeAppModuleTs(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				String c8o_PagesImport = "";
				String c8o_PagesLinks = "";
				String c8o_PagesDeclarations = "";
				int i=1;
				
				Map<String, File> comp_beans_dirs = new HashMap<>();
				Map<String, String> module_ts_imports = new HashMap<>();
				Set<String> module_ng_imports =  new HashSet<String>();
				Set<String> module_ng_providers =  new HashSet<String>();
				Set<String> module_ng_declarations =  new HashSet<String>();
				Set<String> module_ng_components =  new HashSet<String>();
				
				//App contributors
				for (Contributor contributor : app.getContributors()) {
					comp_beans_dirs.putAll(contributor.getCompBeanDir());

					module_ts_imports.putAll(contributor.getModuleTsImports());
					module_ng_imports.addAll(contributor.getModuleNgImports());
					module_ng_providers.addAll(contributor.getModuleNgProviders());
					module_ng_declarations.addAll(contributor.getModuleNgDeclarations());
					module_ng_components.addAll(contributor.getModuleNgComponents());
				}
				
				//Pages contributors
				List<PageComponent> pages = getEnabledPages(app);
				for (PageComponent page : pages) {
					String pageName = page.getName();
					String pageSegment = page.getSegment();
					boolean isLastPage = i == pages.size();
					
					if (app.compareToTplVersion("7.7.0.2") < 0) {
						c8o_PagesImport += "import { "+pageName+" } from \"../pages/"+pageName+"/"+pageName.toLowerCase()+"\";"+ System.lineSeparator();
						c8o_PagesLinks += " { component: "+pageName+", name: \""+pageName+"\", segment: \""+pageSegment+"\" }" + (isLastPage ? "":",");
						c8o_PagesDeclarations += " " + pageName + (isLastPage ? "":",");
						
						List<Contributor> contributors = page.getContributors();
						for (Contributor contributor : contributors) {
							comp_beans_dirs.putAll(contributor.getCompBeanDir());
							
							module_ts_imports.putAll(contributor.getModuleTsImports());
							module_ng_imports.addAll(contributor.getModuleNgImports());
							module_ng_providers.addAll(contributor.getModuleNgProviders());
							module_ng_declarations.addAll(contributor.getModuleNgDeclarations());
							module_ng_components.addAll(contributor.getModuleNgComponents());
						}
					} else {

						List<Contributor> contributors = page.getContributors();
						for (Contributor contributor : contributors) {
							if (contributor.isNgModuleForApp()) {
								comp_beans_dirs.putAll(contributor.getCompBeanDir());
								
								module_ts_imports.putAll(contributor.getModuleTsImports());
								module_ng_imports.addAll(contributor.getModuleNgImports());
								module_ng_providers.addAll(contributor.getModuleNgProviders());
								module_ng_declarations.addAll(contributor.getModuleNgDeclarations());
								module_ng_components.addAll(contributor.getModuleNgComponents());
							}
						}
						
						writePageModuleTs(page);
					}
					
					i++;
				}
				
				String c8o_ModuleTsImports = "";
				Map<String, String> tpl_ts_imports = getTplAppModuleTsImports();
				if (!module_ts_imports.isEmpty()) {
					for (String comp : module_ts_imports.keySet()) {
						if (!tpl_ts_imports.containsKey(comp)) {
							if (comp.indexOf(" as ") != -1) {
								c8o_ModuleTsImports += "import "+comp+" from '"+ module_ts_imports.get(comp) +"';"+ System.lineSeparator();
							} else {
								c8o_ModuleTsImports += "import { "+comp+" } from '"+ module_ts_imports.get(comp) +"';"+ System.lineSeparator();
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
						c8o_ModuleNgImports = System.lineSeparator() + c8o_ModuleNgImports;
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
				mContent = mContent.replaceAll("/\\*Begin_c8o_NgModules\\*/",c8o_ModuleNgImports);
				mContent = mContent.replaceAll("/\\*End_c8o_NgModules\\*/","");
				mContent = mContent.replaceAll("/\\*Begin_c8o_NgProviders\\*/",c8o_ModuleNgProviders);
				mContent = mContent.replaceAll("/\\*End_c8o_NgProviders\\*/","");
				mContent = mContent.replaceAll("/\\*Begin_c8o_NgDeclarations\\*/",c8o_ModuleNgDeclarations);
				mContent = mContent.replaceAll("/\\*End_c8o_NgDeclarations\\*/","");
				mContent = mContent.replaceAll("/\\*Begin_c8o_NgComponents\\*/",c8o_ModuleNgComponents);
				mContent = mContent.replaceAll("/\\*End_c8o_NgComponents\\*/","");
				
				File appModuleTsFile = new File(ionicWorkDir, "src/app/app.module.ts");
				writeFile(appModuleTsFile, mContent, "UTF-8");
				
				for (String compbean : comp_beans_dirs.keySet()) {
					File srcDir = comp_beans_dirs.get(compbean);
					for (File f: srcDir.listFiles()) {
						String fContent = FileUtils.readFileToString(f, "UTF-8");
						File destFile = new File(ionicWorkDir, "src/components/"+ compbean+ "/"+ f.getName());
						writeFile(destFile, fContent, "UTF-8");
					}
				}
				
				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Ionic module ts file generated for 'app'");
				}
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
		String c8o_PageArrayDef = "";
		String c8o_Version = app.getC8oVersion();
		String c8o_AppComponentMarkers = app.getComponentScriptContent().getString();
		String c8o_AppImports = app.getComputedImports();
		String c8o_AppDeclarations = app.getComputedDeclarations();
		String c8o_AppConstructors = app.getComputedConstructors();
		String c8o_AppFunctions = app.getComputedFunctions();
		int i=1;
		
		if (app.compareToTplVersion("7.7.0.6") < 0) {
			c8o_PageArrayDef = "Array<{title: string, icon: string, iconPos: string, component: any, name: string, includedInAutoMenu?: boolean}>";
		} else {
			c8o_PageArrayDef = "Array<{title: string, titleKey: string, icon: string, iconPos: string, component: any, name: string, includedInAutoMenu?: boolean}>";
		}
		
		List<PageComponent> pages = getEnabledPages(app);
		for (PageComponent page : pages) {
			String pageName = page.getName();
			String pageIcon = page.getIcon();
			String pageIconPos = page.getIconPosition();
			String pageTitle = page.getTitle();
			String pageTitleKey = TranslateUtils.getComputedKey(project, page.getTitle());
			boolean isRootPage = page.isRoot;
			boolean isMenuPage = page.isInAutoMenu();
			boolean isLastPage = i == pages.size();
			
			if (isRootPage) {
				c8o_RootPage = pageName;
			}
			
			if (app.compareToTplVersion("7.7.0.2") < 0) {
				c8o_PagesImport += "import { "+pageName+" } from \"../pages/"+pageName+"/"+pageName.toLowerCase()+"\";" + System.lineSeparator();
				if (app.compareToTplVersion("7.5.2.1") < 0) {
					c8o_PagesVariables += " { title: \""+pageTitle+"\", icon: \""+ pageIcon +"\", component: "+pageName+", includedInAutoMenu: "+ isMenuPage+"}" + (isLastPage ? "":",");
				} else {
					c8o_PagesVariables += " { title: \""+pageTitle+"\", icon: \""+ pageIcon +"\", iconPos: \""+ pageIconPos +"\", component: "+pageName+", includedInAutoMenu: "+ isMenuPage+"}" + (isLastPage ? "":",");
				}
				c8o_PagesVariablesKeyValue += pageName+":"+ pageName+ (isLastPage ? "":",");
			} else if (app.compareToTplVersion("7.7.0.6") < 0) {
				if (isRootPage) {
					c8o_RootPage = "'"+ c8o_RootPage + "'";
					
					c8o_PagesVariables += " { title: \""+pageTitle+"\", icon: \""+ pageIcon +"\", iconPos: \""+ pageIconPos +"\", component: "+ "this.rootPage" +", name: \""+pageName+"\", includedInAutoMenu: "+ isMenuPage+"}" + (isLastPage ? "":",");
					c8o_PagesVariablesKeyValue += pageName+":"+ "this.rootPage" + (isLastPage ? "":",");
				} else {
					c8o_PagesVariables += " { title: \""+pageTitle+"\", icon: \""+ pageIcon +"\", iconPos: \""+ pageIconPos +"\", component: \""+pageName+"\", name: \""+pageName+"\", includedInAutoMenu: "+ isMenuPage+"}" + (isLastPage ? "":",");
					c8o_PagesVariablesKeyValue += pageName+":"+ "null" + (isLastPage ? "":",");
				}
			} else {
				if (isRootPage) {
					c8o_RootPage = "'"+ c8o_RootPage + "'";
					
					c8o_PagesVariables += " { title: \""+pageTitle+"\", titleKey: \""+ pageTitleKey +"\", icon: \""+ pageIcon +"\", iconPos: \""+ pageIconPos +"\", component: "+ "this.rootPage" +", name: \""+pageName+"\", includedInAutoMenu: "+ isMenuPage+"}" + (isLastPage ? "":",");
					c8o_PagesVariablesKeyValue += pageName+":"+ "this.rootPage" + (isLastPage ? "":",");
				} else {
					c8o_PagesVariables += " { title: \""+pageTitle+"\", titleKey: \""+ pageTitleKey +"\", icon: \""+ pageIcon +"\", iconPos: \""+ pageIconPos +"\", component: \""+pageName+"\", name: \""+pageName+"\", includedInAutoMenu: "+ isMenuPage+"}" + (isLastPage ? "":",");
					c8o_PagesVariablesKeyValue += pageName+":"+ "null" + (isLastPage ? "":",");
				}
			}
			
			
			i++;
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
		cContent = cContent.replaceAll("/\\*\\=c8o_AppImports\\*/",c8o_AppImports);
		cContent = cContent.replaceAll("/\\*\\=c8o_AppDeclarations\\*/",c8o_AppDeclarations);
		cContent = cContent.replaceAll("/\\*\\=c8o_AppConstructors\\*/",c8o_AppConstructors);
		cContent = cContent.replaceAll("/\\*\\=c8o_AppProdMode\\*/",c8o_AppProdMode);
		
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
				
				File appComponentTsFile = new File(ionicWorkDir, "src/app/app.component.ts");
				writeFile(appComponentTsFile, getAppComponentTsContent(app), "UTF-8");
				
				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Ionic component ts file generated for 'app'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic app component ts file",e);
		}
	}
	
	public void writeAppComponentTempTs(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				File appTsFile = new File(ionicWorkDir, "src/app/app.component.ts");
				synchronized (writtenFiles) {
					if (writtenFiles.contains(appTsFile)) {
						File appTsFileTmp = toTmpFile(appTsFile);
						if (appTsFileTmp.exists()) {
							appTsFile = appTsFileTmp;
						}
					}
				}
				
				File tempTsFile = new File(ionicWorkDir, "src/app/app.component.temp.ts");
				
				// Write file (do not need delay)
				FileUtils.copyFile(appTsFile, tempTsFile);
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
				File appHtmlFile = new File(ionicWorkDir, "src/app/app.html");
				String computedTemplate = app.getComputedTemplate();
				writeFile(appHtmlFile, computedTemplate, "UTF-8");
				
				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Ionic template file generated for app '"+appName+"'");
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
				File appScssFile = new File(ionicWorkDir, "src/app/app.scss");
				String computedScss = app.getComputedStyle();
				writeFile(appScssFile, computedScss, "UTF-8");
				
				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Ionic scss file generated for app '"+appName+"'");
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
				File themeScssFile = new File(ionicWorkDir, "src/theme/variables.scss");
				String tContent = app.getComputedTheme();
				writeFile(themeScssFile, tContent, "UTF-8");
				
				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Ionic theme scss file generated for app '"+appName+"'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic theme scss file",e);
		}
	}

	private void deleteUselessDir(String pageName) {
		File pageDir = new File(ionicWorkDir,"src/pages/"+ pageName);
		deleteDir(pageDir);
	}
	
	private void removeUselessPages(ApplicationComponent application) {
		if (application != null) {
			File ionicPagesDir = new File(ionicWorkDir,"src/pages");
			List<String> pageDirectories = new ArrayList<String>();
			pageDirectories.add(ionicPagesDir.getAbsolutePath());
			
			List<PageComponent> pages = application.getPageComponentList();
			for (PageComponent page : pages) {
				File pageDir = new File(ionicPagesDir, page.getName());
				pageDirectories.add(pageDir.getAbsolutePath());
			}
			for (File dir: FileUtils.listFilesAndDirs(ionicPagesDir, FalseFileFilter.INSTANCE, DirectoryFileFilter.DIRECTORY)) {
				if (!pageDirectories.contains(dir.getAbsolutePath())) {
					try {
						FileUtils.deleteDirectory(dir);
					}
					catch(Exception e) {}
				}
			}
		}
	}
	
	private void writeAppSourceFiles(ApplicationComponent application) throws EngineException {
		try {
			if (application != null) {
				FileUtils.deleteQuietly(new File(ionicWorkDir, "src/app/app.component.temp.ts"));
				
				writeAppPackageJson(application);
				writeAppPluginsConfig(application);
				writeAppServiceTs(application);
				writeAppModuleTs(application);
				writeAppComponentTs(application);
				writeAppTemplate(application);
				writeAppStyle(application);
				writeAppTheme(application);

				Engine.logEngine.trace("(MobileBuilder) Application source files generated for ionic project '"+ project.getName() +"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write application source files for ionic project '"+ project.getName() +"'",e);
		}
	}
	
	private void writePageSourceFiles(PageComponent page) throws EngineException {
		String pageName = page.getName();
		try {
			File pageDir = new File(ionicWorkDir,"src/pages/"+pageName);
			pageDir.mkdirs();
			
			FileUtils.deleteQuietly(new File(pageDir, pageName.toLowerCase() + ".temp.ts"));
			
			writePageTs(page);
			writePageModuleTs(page);
			writePageStyle(page);
			writePageTemplate(page);
			
			if (initDone) {
				Engine.logEngine.trace("(MobileBuilder) Ionic source files generated for page '"+pageName+"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write source files for page '"+pageName+"'",e);
		}
	}
	
	public void setAutoBuild(boolean autoBuild) {
		Engine.logEngine.debug("(MobileBuilder) AutoBuild mode set to "+ (autoBuild ? "ON":"OFF"));
		this.autoBuild = autoBuild;
		if (autoBuild) {
			moveFilesForce();
		}
		
	}
	
	public boolean isAutoBuild() {
		return autoBuild;
	}
	
	public static String getMarkers(String content) {
		String markers = "";
		Pattern pattern = Pattern.compile("/\\*Begin_c8o_(.+)\\*/"); // begin c8o marker
		Matcher matcher = pattern.matcher(content);
		while (matcher.find()) {
			String markerId = matcher.group(1);
			String marker = getMarker(content, markerId);
			if (!marker.isEmpty() && markers.indexOf(markerId) == -1) {
				markers += marker + System.lineSeparator();
			}
		}
		return markers;
	}
	
	public static String getMarker(String s, String markerId) {
		String beginMarker = "/*Begin_c8o_" + markerId + "*/";
		String endMarker = "/*End_c8o_" + markerId + "*/";
		int beginIndex = s.indexOf(beginMarker);
		if (beginIndex != -1) {
			int endIndex = s.indexOf(endMarker, beginIndex);
			if (endIndex != -1) {
				//return s.substring(beginIndex, endIndex) + endMarker;
				String comment = "/*inner marker removed!*/";
				String content = s.substring(beginIndex + beginMarker.length(), endIndex);
				content = content.replaceAll("/\\*Begin_c8o_(.+)\\*/", comment).replaceAll("/\\*End_c8o_(.+)\\*/", comment);
				return beginMarker + content + endMarker;
			}
		}
		return "";
	}
	
	public static String getFormatedContent(String marker, String markerId) {
		String content = "";
		if (!marker.isEmpty()) {
			String line;
			String[] lines = marker.split("\\R");
			for (int i=0; i<lines.length; i++) {
				line = lines[i];
				if (line.indexOf("/*Begin_c8o_") == -1 && line.indexOf("/*End_c8o_") == -1) {
					content += line + System.lineSeparator();
				}
			}
		}
		return content;
	}
	
	private static File toTmpFile(File file) {
		return new File(file.getAbsolutePath().replaceFirst("_private(/|\\\\)ionic", "_private$1ionic_tmp"));
	}
	
	private void deleteDir(File dir) {
		if (initDone && Engine.isStudioMode()) {
			// delete dir
			if (buildMutex == null) {
				try {
					FileUtils.deleteDirectory(dir);
					Engine.logEngine.debug("(MobileBuilder) Deleted dir " + dir.getPath());
				} catch (IOException e) {
					Engine.logEngine.warn("(MobileBuilder) Failed to delete directory " + dir.getPath(), e);
				}
			}
			// defers the dir deletion
			else {
				/*File nDir = toTmpFile(dir);
				if (nDir.exists()) {
					Engine.logEngine.debug("(MobileBuilder) Defers the deletion of directory " + dir.getPath());
					try {
						FileUtils.deleteDirectory(nDir);
						dirsToDelete.add(dir);
					} catch (IOException e) {
						Engine.logEngine.warn("(MobileBuilder) Failed to delete directory " + dir.getPath(), e);
					}
				}*/
			}
		} else {
			try {
				FileUtils.deleteDirectory(dir);
			} catch (IOException e) {
				Engine.logEngine.warn("(MobileBuilder) Failed to delete directory " + dir.getPath(), e);
			}
		}
	}
	
	public boolean isAppWpaAble() {
		return this.isPWA;
	}
	
	private void configurePwaApp(ApplicationComponent app) {
		this.isPWA = app.isPWA();
		
		try {
			File tpl_index = new File(ionicTplDir, "src/index.html");
			String tpl_content = FileUtils.readFileToString(tpl_index, "UTF-8");
			String content = tpl_content;
			
			// Register service worker
			if (isPWA) {
				String replacement = "";
				replacement += "<script>\n"
								+"\tif ('serviceWorker' in navigator) {\n"
									+"\t\tnavigator.serviceWorker.register('service-worker.js')\n"
										+"\t\t\t.then(() => console.log('service worker installed'))\n"
										+"\t\t\t.catch(err => console.log('Error', err));\n"
								+"\t}\n"
								+"</script>\n";
				content = tpl_content.replace("<!--c8o_PWA-->", replacement);
			}
			
			// Set application name
			String pwaAppName = app.getParent().getApplicationName();
			content = content.replace("<!--c8o_App_Name-->", pwaAppName);
			
			File index = new File(ionicWorkDir, "src/index.html");
			writeFile(index, content, "UTF-8");
			if (!initDone || !isPWA) {
				writeWorker(index, true);
			}
		} catch (Exception e) {
			;
		}
	}
	
	private void writeWorker(File file) throws IOException {
		writeWorker(file, false);
	}
	
	private void writeWorker(File file, boolean bForce) throws IOException {
		File jsworker = new File(ionicWorkDir, "src/service-worker.js");
		if ((isAppWpaAble() || bForce) && jsworker.exists()) {
			long time = System.currentTimeMillis();
			String content = FileUtils.readFileToString(jsworker, "UTF-8");
			content = CacheVersion.matcher(content).replaceFirst("const CACHE_VERSION = "+ time);
			if (initDone && Engine.isStudioMode()) {
				if (!file.getPath().equals(jsworker.getPath())) {
					writeFile(jsworker, content, "UTF-8");
				}
			} else {
				FileUtils.write(jsworker, content, "UTF-8");
			}
		}
	}
	
	private void writeFile(File file, CharSequence content, String encoding) throws IOException {
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
						Engine.logEngine.trace("(MobileBuilder) No change for " + file.getPath());
						return;
					}
				}
				
				// write file
				if (buildMutex == null) {
					FileUtils.write(file, content, encoding);
					Engine.logEngine.debug("(MobileBuilder) Wrote file " + file.getPath());
				}
				// defers the write of file
				else {
					// write to temporary directory (for file edition)
					writtenFiles.add(file);
					File nFile = toTmpFile(file);
					nFile.getParentFile().mkdirs();
					FileUtils.write(nFile, content, encoding);
					
					// store files modifications
					Engine.logEngine.debug("(MobileBuilder) Defers the write of " + content.length() + " chars to " + nFile.getPath());
					if (pushedFiles != null) {
						synchronized (pushedFiles) {
							pushedFiles.put(file.getPath(), content);
						}
					}
				}
			}
		} else {
			FileUtils.write(file, content, encoding);
		}
		
		writeWorker(file);
	}
	
	private void moveFiles() {
		if (autoBuild) {
			StackTraceElement parentMethod = Thread.currentThread().getStackTrace()[3];
			if (!parentMethod.getClassName().equals("com.twinsoft.convertigo.engine.mobile.MobileBuilder")) {
				moveFilesForce();// // written files in queue are pushed to build dir
			} else {
				if (buildMutex != null) {
					Engine.logEngine.warn("(MobileBuilder) moveFilesForce not called", new Throwable("Invalid stack call"));
				}
			}
		}
	}
	
	private synchronized void moveFilesForce() {
		// push files modifications to queue
		if (pushedFiles != null && queue != null) {
			synchronized (pushedFiles) {
				int size = pushedFiles.size();
				if (size > 0) {
					Engine.logEngine.debug("(MobileBuilder) >>> moveFilesForce : "+ size +" files");
					Map<String, CharSequence> map = new HashMap<String, CharSequence>();
					map.putAll(Collections.unmodifiableMap(pushedFiles));
					if (queue.offer(map)) {
						pushedFiles.clear();
					}
				}
			}
		}
	}

	public void startBuild() {
		synchronized (isBuilding) {
			isBuilding[0] = true;
		}
	}
	
	public void buildFinished() {
		synchronized (isBuilding) {
			isBuilding[0] = false;
			isBuilding.notify();
		}
	}
	
	public void waitBuildFinished() {
		synchronized (isBuilding) {
			if (isBuilding[0]) {
				try {
					isBuilding.wait(60000);
				} catch (InterruptedException e) {
				}
			}
		}
	}
}
