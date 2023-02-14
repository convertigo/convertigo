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

package com.twinsoft.convertigo.engine.mobile;

import java.io.File;
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
import java.util.concurrent.BlockingQueue;
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
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.MobileBuilderBuildMode;
import com.twinsoft.convertigo.engine.mobile.ComponentRefManager.Mode;
import com.twinsoft.convertigo.engine.util.EventHelper;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.ZipUtils;

public class NgxBuilder extends MobileBuilder {
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

		@SuppressWarnings("unused")
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
					boolean hasMovedCompFiles = false;
					boolean hasMovedCfgFiles = false;
					boolean hasMovedFiles = false;
					boolean hasDeletedFiles = false;

					Engine.logEngine.debug("(MobileBuilder) Start to handle " + map.size() + " files.");

					// Move all files
					int cw = 0, cd = 0;
					List<String> pathList = new ArrayList<String>(map.keySet());
					List<String> toWrite = new ArrayList<String>();
					List<String> toDelete = new ArrayList<String>();
					Collections.sort(pathList, new AppFileComparator());
					for (String path: pathList /*map.keySet()*/) {
						if (isDeletedFile(path)) {
							toDelete.add(path);
							cd++;
						} else {
							toWrite.add(path);
							cw++;
						}
					}
					for (String path: toWrite) {
						try {
							FileUtils.write(new File(path), map.get(path), "UTF-8");
							Engine.logEngine.debug("(MobileBuilder) Moved " + path);
							hasMovedFiles = true;
							
							if (isAppFile(path) || isServiceFile(path)) {
								hasMovedAppOrServFiles = true;
							}
							if (isCompFile(path)) {
								hasMovedCompFiles = true;
							}
							if (isPageFile(path)) {
								hasMovedPageFiles = true;
							}
							if (path.endsWith("package.json") || path.endsWith("angular.json")) {
								hasMovedCfgFiles = true;
							}
						} catch (Exception e) {
							Engine.logEngine.warn("(MobileBuilder) Failed to write the new content of " + path, e);							
						}
					}
					if (cw > 0) Engine.logEngine.debug("(MobileBuilder) End to move " + cw + " files.");
					for (String path: toDelete) {
						try {
							File f = new File(path);
							File dir = f.getParentFile();
							//FileUtils.deleteDirectory(dir);
							org.apache.commons.io.FileUtils.forceDelete(dir);
							Engine.logEngine.debug("(MobileBuilder) Deleted " + dir.getPath());
							hasDeletedFiles = true;
						} catch (Exception e) {}
					}
					if (cd > 0) Engine.logEngine.debug("(MobileBuilder) End to delete " + cd + " files.");
					
					if (hasMovedFiles || hasDeletedFiles) {
						NgxBuilder.this.updateEnvFile();
					}

					// Need package installation
					if (hasMovedCfgFiles && getNeedPkgUpdate()) {
						hasMovedFiles = false;
						NgxBuilder.this.firePackageUpdated();
					}

					if (hasMovedFiles || hasDeletedFiles) {
						if (buildMutex != null) {
							synchronized (buildMutex) {
								try {
									buildMutex.wait(6000);//buildMutex.wait(60000);
								} catch (InterruptedException e) {}							
							}
							Engine.logEngine.debug("(MobileBuilder) build finished.");
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

	Map<String,String> tpl_appCompTsImports = null;
	Map<String,String> tpl_pageTsImports = null;
	Map<String,String> tpl_compTsImports = null;

	Map<String,String> tpl_appModuleTsImports = null;
	Map<String,String> tpl_pageModuleTsImports = null;
	Map<String,String> tpl_compModuleTsImports = null;

	Map<String,String> tpl_serviceActionTsImports = null;

	String tpl_appModuleNgImports = null;
	String tpl_appModuleNgProviders = null;
	String tpl_appModuleNgDeclarations = null;
	String tpl_appModuleNgComponents = null;

	String tpl_pageModuleNgImports = null;
	String tpl_pageModuleNgProviders = null;
	String tpl_pageModuleNgDeclarations = null;
	String tpl_pageModuleNgComponents = null;
	String tpl_pageModuleNgRoutes = null;

	String tpl_compModuleNgImports = null;
	String tpl_compModuleNgProviders = null;
	String tpl_compModuleNgDeclarations = null;
	String tpl_compModuleNgComponents = null;

	File appDir, pagesDir, interfacesDir, servicesDir, providersDir, componentsDir;
	File assetsDir, envDir, themeDir;
	File srcDir;

	DirectoryWatcherService watcherService = null;
	MbWorker worker = null;

	protected static String FakeDeleted = "fake_deleted.ts";
	
	static class AppFileComparator implements Comparator<String> {
		public int compare(String path1, String path2) {
			String pound1 = isCompFile(path1) ? "0" : isPageFile(path1) ? "1" : "2";
			String pound2 = isCompFile(path2) ? "0" : isPageFile(path2) ? "1" : "2";
	    	return pound1.compareTo(pound2);
	    }
	}
	
	static private boolean isDeletedFile(String path) {
		return path.endsWith(FakeDeleted);
	}
	 
	static private boolean isAppFile(String path) {
		String search = File.separator + "src" + File.separator + "app" + File.separator + "app.";
		return path == null ? false : path.indexOf(search) != -1;
	}
	
	static private boolean isPageFile(String path) {
		String search = File.separator + "src" + File.separator + "app" + File.separator + "pages" + File.separator;
		return path == null ? false : path.indexOf(search) != -1;
	}
	
	static private boolean isCompFile(String path) {
		String search = File.separator + "src" + File.separator + "app" + File.separator + "components" + File.separator;
		return path == null ? false : path.indexOf(search) != -1;
	}
	
	static private boolean isServiceFile(String path) {
		String search = File.separator + "src" + File.separator + "app" + File.separator + "services" + File.separator;
		return path == null ? false : path.indexOf(search) != -1;
	}
	
	static boolean existTargetComp(String compDirName) {
	   return compQName(compDirName) != null;
	}
	    
    static protected String compQName(String compDirName) {
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
					// TODO Auto-generated catch block
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

		assetsDir = new File(srcDir, "assets");
		envDir = new File(srcDir, "environments");
		themeDir = new File(srcDir, "theme");
		appDir = new File(srcDir, "app");

		componentsDir = new File(appDir, "components");
		interfacesDir = new File(appDir, "interfaces");
		providersDir = new File(appDir, "providers");
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
				//project.getMobileApplication().getApplicationComponent().markPwaAsDirty();
				appPwaChanged(project.getMobileApplication().getApplicationComponent());
			} catch (Exception e) {
				Engine.logEngine.warn("(MobileBuilder) enabled to change build mode");
			}
		}
	}

	private void addPage(PageComponent page) throws EngineException {
		MobileApplication mobileApplication = project.getMobileApplication();
		if (mobileApplication != null) {
			ApplicationComponent application = (ApplicationComponent) mobileApplication.getApplicationComponent();
			if (application != null) {
				writePageSourceFiles(page);
				writeAppSourceFiles(application);
			}
		}
	}

	private void addComp(UISharedComponent comp) throws EngineException {
		MobileApplication mobileApplication = project.getMobileApplication();
		if (mobileApplication != null) {
			ApplicationComponent application = (ApplicationComponent) mobileApplication.getApplicationComponent();
			if (application != null) {
				writeCompSourceFiles(comp);
				writeAppSourceFiles(application);
			}
		}
	}

	@Override
	public void pageEnabled(final IPageComponent pageComponent) throws EngineException {
		PageComponent page = (PageComponent)pageComponent;
		if (page != null && page.isEnabled() && initDone) {
			synchronized (page) {
				addPage(page);
				moveFiles();
				Engine.logEngine.trace("(MobileBuilder) Handled 'pageEnabled'");
			}
		}
	}

	@Override
	public void pageDisabled(final IPageComponent pageComponent) throws EngineException {
		PageComponent page = (PageComponent)pageComponent;
		if (page != null && !page.isEnabled() && initDone) {
			synchronized (page) {
				MobileApplication mobileApplication = project.getMobileApplication();
				if (mobileApplication != null) {
					ApplicationComponent application = (ApplicationComponent) mobileApplication.getApplicationComponent();
					if (application != null) {
						writePageSourceFiles(page);
						writeAppSourceFiles(application);
						moveFiles();
						Engine.logEngine.trace("(MobileBuilder) Handled 'pageDisabled'");
					}
				}
			}
		}
	}

	@Override
	public void pageAdded(final IPageComponent pageComponent) throws EngineException {
		PageComponent page = (PageComponent)pageComponent;
		if (page != null && page.isEnabled() && page.bNew && initDone) {
			synchronized (page) {
				addPage(page);
				moveFiles();
				Engine.logEngine.trace("(MobileBuilder) Handled 'pageAdded'");
			}
		}
	}

	@Override
	public void compAdded(final ISharedComponent sharedComponent) throws EngineException {
		UISharedComponent comp = (UISharedComponent)sharedComponent;
		if (comp != null && comp.bNew && initDone) {
			synchronized (comp) {
				addComp(comp);
				moveFiles();
				Engine.logEngine.trace("(MobileBuilder) Handled 'compAdded'");
			}
		}
	}

	@Override
	public void pageRemoved(final IPageComponent pageComponent) throws EngineException {
		PageComponent page = (PageComponent)pageComponent;
		if (page != null && page.isEnabled() && initDone) {
			synchronized (page) {
				MobileApplication mobileApplication = project.getMobileApplication();
				if (mobileApplication != null) {
					ApplicationComponent application = (ApplicationComponent) mobileApplication.getApplicationComponent();
					if (application != null) {
						writeAppSourceFiles(application);
						deleteUselessPageDir(page.getName());
						moveFiles();
						Engine.logEngine.trace("(MobileBuilder) Handled 'pageRemoved'");
					}
				}
			}
		}
	}

	@Override
	public void compRemoved(final ISharedComponent sharedComponent) throws EngineException {
		UISharedComponent comp = (UISharedComponent)sharedComponent;
		if (comp != null && initDone) {
			synchronized (comp) {
				MobileApplication mobileApplication = project.getMobileApplication();
				if (mobileApplication != null) {
					ApplicationComponent application = (ApplicationComponent) mobileApplication.getApplicationComponent();
					if (application != null) {
						writeAppSourceFiles(application);
						deleteUselessCompDir(comp.getName(), comp.getQName());
						moveFiles();
						Engine.logEngine.trace("(MobileBuilder) Handled 'compRemoved'");

						ComponentRefManager.get(Mode.use).removeKey(comp.getQName());
					}
				}
			}
		}
	}

	@Override
	public void pageRenamed(final IPageComponent pageComponent, final String oldName) throws EngineException {
		PageComponent page = (PageComponent)pageComponent;
		if (page != null && page.isEnabled() && initDone) {
			synchronized (page) {
				MobileApplication mobileApplication = project.getMobileApplication();
				if (mobileApplication != null) {
					ApplicationComponent application = (ApplicationComponent) mobileApplication.getApplicationComponent();
					if (application != null) {
						writePageSourceFiles(page);
						writeAppSourceFiles(application);
						deleteUselessPageDir(oldName);
						moveFiles();
						Engine.logEngine.trace("(MobileBuilder) Handled 'pageRenamed'");
					}
				}
			}
		}
	}

	@Override
	public void compRenamed(final ISharedComponent sharedComponent, final String oldName) throws EngineException {
		UISharedComponent comp = (UISharedComponent)sharedComponent;
		if (comp != null && initDone) {
			synchronized (comp) {
				String newName = comp.getName();
				String newQName = comp.getQName();
				String oldQName = newQName.replace("."+newName, "."+oldName);

				ComponentRefManager.get(Mode.use).copyKey(oldQName, newQName);

				MobileApplication mobileApplication = project.getMobileApplication();
				if (mobileApplication != null) {
					ApplicationComponent application = (ApplicationComponent) mobileApplication.getApplicationComponent();
					if (application != null) {
						writeCompSourceFiles(comp);
						writeAppSourceFiles(application);
						deleteUselessCompDir(oldName, oldQName);
						moveFiles();
						Engine.logEngine.trace("(MobileBuilder) Handled 'compRenamed'");
					}
				}
			}
		}
	}

	@Override
	public void pageTemplateChanged(final IPageComponent pageComponent) throws EngineException {
		PageComponent page = (PageComponent)pageComponent;
		if (page != null && page.isEnabled() && initDone) {
			synchronized (page) {
				writePageTemplate(page);
				moveFiles();
				Engine.logEngine.trace("(MobileBuilder) Handled 'pageTemplateChanged'");
			}
		}
	}

	@Override
	public void compTemplateChanged(final ISharedComponent sharedComponent) throws EngineException {
		UISharedComponent comp = (UISharedComponent)sharedComponent;
		if (comp != null && initDone) {
			synchronized (comp) {
				writeCompTemplate(comp);
				moveFiles();
				Engine.logEngine.trace("(MobileBuilder) Handled 'compTemplateChanged'");
			}
		}
	}

	@Override
	public void pageStyleChanged(final IPageComponent pageComponent) throws EngineException {
		PageComponent page = (PageComponent)pageComponent;
		if (page != null && page.isEnabled() && initDone) {
			synchronized (page) {
				writePageStyle(page);
				moveFiles();
				Engine.logEngine.trace("(MobileBuilder) Handled 'pageStyleChanged'");
			}
		}
	}


	@Override
	public void compStyleChanged(ISharedComponent sharedComponent) throws EngineException {
		UISharedComponent comp = (UISharedComponent)sharedComponent;
		if (comp != null && initDone) {
			synchronized (comp) {
				writeCompStyle(comp);
				moveFiles();
				Engine.logEngine.trace("(MobileBuilder) Handled 'compStyleChanged'");
			}
		}
	}

	@Override
	public void appContributorsChanged(final IApplicationComponent appComponent) throws EngineException {
		ApplicationComponent app = (ApplicationComponent)appComponent;
		if (app != null && initDone) {
			synchronized (app) {
				writeAppSourceFiles(app);
				moveFiles();
				Engine.logEngine.trace("(MobileBuilder) Handled 'appContributorsChanged'");
			}
		}
	}

	@Override
	public void pageTsChanged(final IPageComponent pageComponent, boolean forceTemp) throws EngineException {
		PageComponent page = (PageComponent)pageComponent;
		if (page != null && page.isEnabled() && initDone) {
			synchronized (page) {
				writePageTs(page);
				moveFiles();

				File pageDir = pageDir(page);
				File tempTsFile = new File(pageDir, page.getName().toLowerCase() + ".temp.ts");
				if (forceTemp && tempTsFile.exists()) {
					writePageTempTs(page);
				}

				Engine.logEngine.trace("(MobileBuilder) Handled 'pageTsChanged'");
			}
		}
	}

	@Override
	public void compTsChanged(ISharedComponent sharedComponent, boolean forceTemp) throws EngineException {
		UISharedComponent comp = (UISharedComponent)sharedComponent;
		if (comp != null && initDone) {
			synchronized (comp) {
				writeCompTs(comp);
				moveFiles();

				/*File compDir = compDir(comp);
				File tempTsFile = new File(compDir, comp.getName().toLowerCase() + ".temp.ts");
				if (forceTemp && tempTsFile.exists()) {
					writeCompTempTs(comp);
				}*/

				Engine.logEngine.trace("(MobileBuilder) Handled 'compTsChanged'");
			}
		}
	}

	@Override
	public void pageModuleTsChanged(final IPageComponent pageComponent) throws EngineException {
		PageComponent page = (PageComponent)pageComponent;
		if (page != null && page.isEnabled() && initDone) {
			synchronized (page) {
				writePageModuleTs(page);
				moveFiles();
				Engine.logEngine.trace("(MobileBuilder) Handled 'pageModuleTsChanged'");
			}
		}
	}

	@Override
	public void compModuleTsChanged(ISharedComponent sharedComponent) throws EngineException {
		UISharedComponent comp = (UISharedComponent)sharedComponent;
		if (comp != null && initDone) {
			synchronized (comp) {
				writeCompModuleTs(comp);
				moveFiles();
				Engine.logEngine.trace("(MobileBuilder) Handled 'compModuleTsChanged'");
			}
		}
	}

	public void appChanged() throws EngineException {
		updateSourceFiles();
	}
	
	@Override
	public void appTsChanged(final IApplicationComponent appComponent, boolean forceTemp) throws EngineException {
		ApplicationComponent app = (ApplicationComponent)appComponent;
		if (app != null && initDone) {
			synchronized (app) {
				writeAppComponentTs(app);
				moveFiles();

				File tempTsFile = new File(appDir, "app.component.temp.ts");
				if (forceTemp && tempTsFile.exists()) {
					writeAppComponentTempTs(app);
				}

				Engine.logEngine.trace("(MobileBuilder) Handled 'appTsChanged'");
			}
		}
	}

	@Override
	public void appStyleChanged(final IApplicationComponent appComponent) throws EngineException {
		ApplicationComponent app = (ApplicationComponent)appComponent;
		if (app != null && initDone) {
			synchronized (app) {
				writeAppStyle(app);
				moveFiles();
				Engine.logEngine.trace("(MobileBuilder) Handled 'appStyleChanged'");
			}
		}
	}

	@Override
	public void appTemplateChanged(final IApplicationComponent appComponent) throws EngineException {
		ApplicationComponent app = (ApplicationComponent)appComponent;
		if (app != null && initDone) {
			synchronized (app) {
				writeAppTemplate(app);
				moveFiles();
				Engine.logEngine.trace("(MobileBuilder) Handled 'appTemplateChanged'");
			}
		}
	}

	@Override
	public void appThemeChanged(final IApplicationComponent appComponent) throws EngineException {
		ApplicationComponent app = (ApplicationComponent)appComponent;
		if (app != null && initDone) {
			synchronized (app) {
				writeAppTheme(app);
				moveFiles();
				Engine.logEngine.trace("(MobileBuilder) Handled 'appThemeChanged'");
			}
		}
	}

	@Override
	public void appCompTsChanged(final IApplicationComponent appComponent) throws EngineException {
		ApplicationComponent app = (ApplicationComponent)appComponent;
		if (app != null && initDone) {
			synchronized (app) {
				writeAppComponentTs(app);
				moveFiles();
				Engine.logEngine.trace("(MobileBuilder) Handled 'appCompTsChanged'");
			}
		}
	}

	@Override
	public void appModuleTsChanged(final IApplicationComponent appComponent) throws EngineException {
		ApplicationComponent app = (ApplicationComponent)appComponent;
		if (app != null && initDone) {
			synchronized (app) {
				writeAppModuleTs(app);
				moveFiles();
				Engine.logEngine.trace("(MobileBuilder) Handled 'appModuleTsChanged'");
			}
		}
	}

	@Override
	public void appPwaChanged(final IApplicationComponent appComponent) throws EngineException {
		ApplicationComponent app = (ApplicationComponent)appComponent;
		if (app != null && initDone) {
			synchronized (app) {
				configurePwaApp(app);		// for worker
				writeAppComponentTs(app);	// for prod mode
				writeAppModuleTs(app); 		// for worker
				moveFiles();
				Engine.logEngine.trace("(MobileBuilder) Handled 'appPwaChanged'");
			}
		}
	}

	@Override
	public void appRootChanged(final IApplicationComponent appComponent) throws EngineException {
		ApplicationComponent app = (ApplicationComponent)appComponent;
		if (app != null && initDone) {
			synchronized (app) {
				writeAppComponentTs(app);
				moveFiles();

				File appComponentTsFile = new File(appDir, "app.component.temp.ts");
				if (appComponentTsFile.exists()) {
					writeAppComponentTempTs(app);
				}
				Engine.logEngine.trace("(MobileBuilder) Handled 'appRootChanged'");
			}
		}
	}

	@Override
	public void appRouteChanged(final IApplicationComponent appComponent) throws EngineException {
		ApplicationComponent app = (ApplicationComponent)appComponent;
		if (app != null && initDone) {
			synchronized (app) {
				writeAppComponentTs(app);
				moveFiles();

				File appComponentTsFile = new File(appDir, "app.component.temp.ts");
				if (appComponentTsFile.exists()) {
					writeAppComponentTempTs(app);
				}
				Engine.logEngine.trace("(MobileBuilder) Handled 'appRouteChanged'");
			}
		}
	}

	@Override
	protected synchronized void init() throws EngineException {
		if (initDone) {
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
			Engine.logEngine.debug("(NgxBuilder) Initializing builder for ionic project '"+ project.getName() +"' ...");

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

			// Studio mode : start worker for build process
			if (Engine.isStudioMode() || Engine.isCliMode()) {
				if (pushedFiles == null) {
					pushedFiles = new HashMap<String, CharSequence>();
				}

				/*if (queue == null) {
					queue = new LinkedBlockingQueue<Map<String, CharSequence>>();
				}

				if (worker == null) {
					worker = new MbWorker(queue);
					if (Engine.isStudioMode()) {
						worker.start();
					} else {
						worker.process();
					}
				}*/

				if (watcherService == null) {
					updateConsumer();
					updateConsumers();

					/*if (Engine.isStudioMode()) {
						try {
							watcherService = new DirectoryWatcherService(project, true);
							watcherService.start();
						} catch (Exception e) {
							Engine.logEngine.warn(e.getMessage());
						}
					}*/
				}
			}

			initDone = true;
			Engine.logEngine.debug("(NgxBuilder) Initialized builder for ionic project '"+ project.getName() +"'");
		}
	}

	public void updateConsumer() {
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

	// for deleted files
//	private void updateConsumers(String compName, String compQName) {
//		for (String useQName: ComponentRefManager.get(Mode.use).getAllConsumers(compQName)) {
//			if (projectName(useQName).equals(project.getName()))
//				continue;
//			try {
//				File dest = new File(Engine.projectDir(projectName(useQName)), UISharedComponent.getNsCompDirPath(compQName, compName));
//				File src = new File(project.getDirPath(), UISharedComponent.getNsCompDirPath(compQName, compName));
//				if (!src.exists() && dest.exists()) {
//					FileUtils.deleteQuietly(dest);
//					ComponentRefManager.get(Mode.use).removeConsumer(compQName, useQName);
//				}
//			} catch (Exception e) {
//				Engine.logEngine.warn("["+project.getName()+"] MB unabled to update consumers for "+ projectName(useQName) + ": " + e.getMessage());
//			}
//		}
//	}

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
			Engine.logEngine.trace("(MobileBuilder) Initialized env.json for ionic project "+ project.getName());

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
			Engine.logEngine.trace("(MobileBuilder) Updated env.json for ionic project "+ project.getName());
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
		if (!initDone) {
			return;
		}

		if (isIonicTemplateBased()) {
			Engine.logEngine.info("(NgxBuilder) Releasing builder for ionic project '"+ project.getName() +"' ...");
			isReleasing = true;
			
			moveFilesForce();

			if (watcherService != null) {
				watcherService.stop();
				watcherService = null;
			}

			/*if (worker != null) {
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
			}*/

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

			storeEnvFile();

			isReleasing = false;
			initDone = false;
			Engine.logEngine.info("(NgxBuilder) Released builder for ionic project '"+ project.getName() +"'");
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
				Engine.logEngine.trace("(MobileBuilder) Assets files copied for ionic project '"+ project.getName() +"'");
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
/*
	private Callable<String> newCallable(final MobileComponent mbc, boolean useInnerCall) {
		return new Callable<String>() {
			@Override
			public String call() throws Exception {
				String s = Thread.currentThread().getName();
				long t0 = System.currentTimeMillis();
				if (mbc instanceof UISharedComponent) {
					if (useInnerCall) {
						call_writeCompSourceFiles((UISharedComponent)mbc);
					} else {
						writeCompSourceFiles((UISharedComponent)mbc);
					}
				}
				else if (mbc instanceof PageComponent) {
					if (useInnerCall) {
						call_writePageSourceFiles((PageComponent)mbc);
					} else {
						writePageSourceFiles((PageComponent)mbc);
					}
				}
				long t1 = System.currentTimeMillis();
				return ("["+s+"] writeSourceFiles for "+ mbc.getClass().getName() + " " + mbc.getName() + " done in "+ (t1-t0) + "ms");
			}
		};
	}
	
	private void updateSourceFiles() throws EngineException {
		ExecutorService executor = null;
		boolean useInnerCall = false;
		try {
			final MobileApplication mobileApplication = project.getMobileApplication();
			if (mobileApplication != null) {
				final ApplicationComponent application = (ApplicationComponent) mobileApplication.getApplicationComponent();
				if (application != null) {
					String appTplVersion = application.requiredTplVersion();
					if (compareVersions(tplVersion, appTplVersion) >= 0) {
						long t0 = System.currentTimeMillis();
						
						List<Callable<String>> cList = new ArrayList<Callable<String>>();
						for (UISharedComponent uisc: application.getSharedComponentList()) {
							if (uisc.isReset()) {
								cList.add(newCallable(uisc, useInnerCall));
							}
						}
						List<Callable<String>> pList = new ArrayList<Callable<String>>();
						for (PageComponent page: application.getPageComponentList()) {
							if (page.isReset()) {
								pList.add(newCallable(page, useInnerCall));
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
									if (useInnerCall) {
										call_writeAppSourceFiles(application);
									} else {
										writeAppSourceFiles(application);
									}
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
						
					    List<Future<String>> resultList = null;
						if (cList.size() > 0) {
						    try {
						      resultList = executor.invokeAll(cList);
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
						if (pList.size() > 0) {
						    try {
						      resultList = executor.invokeAll(pList);
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
						
					    try {
					      resultList = executor.invokeAll(aList);
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
				    	
					    cList.clear();
					    cList = null;
					    pList.clear();
					    pList = null;
					    aList.clear();
					    aList = null;
					    
						long t1 = System.currentTimeMillis();
						Engine.logEngine.debug("(MobileBuilder) Application source files updated for ionic project '"+ project.getName() +"' in "+ (t1-t0) + "ms");
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
		} finally {
			if (executor != null) {
				executor.shutdown();
			}
		}
	}
*/

	private void updateSourceFiles() throws EngineException {
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
						Engine.logEngine.debug("(MobileBuilder) Application source files updated for ionic project '"+ project.getName() +"' in "+ (t1-t0) + "ms");
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
			if (page != null /*&& page.isEnabled()*/) {
				String pageName = page.getName();
				File pageDir = pageDir(page);
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

	private void writeCompTemplate(UISharedComponent comp) throws EngineException {
		try {
			if (comp != null) {
				File compHtmlFile = new File(compDir(comp), compFileName(comp) + ".html");
				String computedTemplate = comp.getComputedTemplate();
				writeFile(compHtmlFile, computedTemplate, "UTF-8");

				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Ionic template file generated for component '"+comp.getName()+"'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page template file",e);
		}
	}

	private void writePageStyle(PageComponent page) throws EngineException {
		try {
			if (page != null /*&& page.isEnabled()*/) {
				String pageName = page.getName();
				File pageDir = pageDir(page);
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

	private void writeCompStyle(UISharedComponent comp) throws EngineException {
		try {
			if (comp != null) {
				File compScssFile = new File(compDir(comp), compFileName(comp) + ".scss");
				String computedScss = comp.getComputedStyle();
				writeFile(compScssFile, computedScss, "UTF-8");

				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Ionic scss file generated for component '"+comp.getName()+"'");
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

				// Write file (do not need delay)
				tsContent = LsPattern.matcher(tsContent).replaceAll(System.lineSeparator());
				File tempTsFile = new File(compDir, compFileName(comp) + ".temp.ts");
				FileUtils.write(tempTsFile, tsContent, "UTF-8");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic component temp ts file",e);
		}
	}

	private void writePageTs(PageComponent page) throws EngineException {
		try {
			if (page != null /*&& page.isEnabled()*/) {
				String pageName = page.getName();
				File pageDir = pageDir(page);
				File pageTsFile = new File(pageDir, pageName.toLowerCase() + ".ts");
				writeFile(pageTsFile, getPageTsContent(page, true), "UTF-8");

				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Ionic ts file generated for page '"+pageName+"'");
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

				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Ionic ts file generated for component '"+comp.getName()+"'");
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
				File pageDir = pageDir(page);
				String pageName = page.getName();

				String c8o_PageRoutingModuleName =  page.getName() + "RoutingModule";

				String c8o_PageImport = "import { "+pageName+" } from \"./"+pageName.toLowerCase()+"\";" + System.lineSeparator();

				String c8o_PageChildRoute = "";
				String c8o_PageChildRoutes = "";
				//List<Contributor> contributors = ;
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

				File pageRoutingTpl = new File(ionicTplDir, "src/page-routing.module.tpl");
				String mContent = FileUtils.readFileToString(pageRoutingTpl, "UTF-8");
				mContent = mContent.replaceAll("/\\*\\=c8o_PageImport\\*/", c8o_PageImport);
				mContent = mContent.replaceAll("/\\*\\=c8o_PageRoutes\\*/", c8o_PageRoutes);
				mContent = mContent.replaceAll("/\\*\\=c8o_PageRoutingModuleName\\*/", c8o_PageRoutingModuleName);

				File pageRoutingTsFile = new File(pageDir, pageName.toLowerCase() + "-routing.module.ts");
				writeFile(pageRoutingTsFile, mContent, "UTF-8");


				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Ionic routing module ts file generated for page '"+ page.getName()+"'");
				}

			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page routing ts file",e);
		}
	}

	private void writePageModuleTs(PageComponent page) throws EngineException {
		try {
			if (page != null /*&& page.isEnabled()*/) {
				if (page.compareToTplVersion("7.7.0.2") >= 0) {
					String pageName = page.getName();
					File pageDir = pageDir(page);
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

	private void writeCompModuleTs(UISharedComponent comp) throws EngineException {
		try {
			if (comp != null) {
				File compModuleTsFile = new File(compDir(comp), compFileName(comp) + ".module.ts");
				writeFile(compModuleTsFile, getCompModuleTsContent(comp), "UTF-8");

				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Ionic module file generated for component '"+comp.getName()+"'");
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
		//String c8o_PageIonicName = pageName;
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
		
		String scriptcontentstring = page.getScriptContent().getString();
		String c8o_UserCustoms = checkEnable ? (page.isEnabled() ? scriptcontentstring : "") : scriptcontentstring;

		File pageTplTs = new File(ionicTplDir, "src/page.tpl");
		String tsContent = FileUtils.readFileToString(pageTplTs, "UTF-8");
		//tsContent = tsContent.replaceAll("/\\*\\=c8o_PageIonicName\\*/","'"+c8o_PageIonicName+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PagePriority\\*/","'"+c8o_PagePriority+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageSegment\\*/","'"+c8o_PageSegment+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageHistory\\*/",c8o_PageHistory);

		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageSelector\\*/","'"+c8o_PageSelector+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageTplUrl\\*/","'"+c8o_PageTplUrl+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageStyleUrls\\*/","'"+c8o_PageStyleUrls+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_PageChangeDetection\\*/",c8o_PageChangeDetection);
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

		File compTplTs = new File(ionicTplDir, "src/comp.tpl");
		String tsContent = FileUtils.readFileToString(compTplTs, "UTF-8");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompSelector\\*/","'"+c8o_CompSelector+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompTplUrl\\*/","'"+c8o_CompTplUrl+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompStyleUrls\\*/","'"+c8o_CompStyleUrls+"'");
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompChangeDetection\\*/",c8o_CompChangeDetection);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompName\\*/",c8o_CompName);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompImports\\*/",c8o_CompImports);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompInterfaces\\*/",c8o_CompInterfaces);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompDeclarations\\*/",c8o_CompDeclarations);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompConstructors\\*/",c8o_CompConstructors);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompInitializations\\*/",c8o_CompInitializations);
		tsContent = tsContent.replaceAll("/\\*\\=c8o_CompFinallizations\\*/",c8o_CompFinallizations);
		
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

		return tsContent;
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
							Engine.logEngine.warn("(NgxBuilder) For App angular build options: "+ e.getMessage());
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
							Engine.logEngine.warn("(NgxBuilder) For App angular build configurations: "+ e.getMessage());
						}
						
						setNeedPkgUpdate(true);

						File angularJson = new File(ionicWorkDir, "angular.json");
						String aContent = jsonObject.toString(1);
						writeFile(angularJson, aContent, "UTF-8");
					}
				}

				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) App angular json file generated");
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
						if (comp.indexOf(" as ") == -1) {
							String comPath = action_ts_imports.get(comp).replace("./pages", "../pages");
							c8o_ActionTsImports += "import { "+comp+" } from '"+ comPath +"';"+ System.lineSeparator();
						} else {
							c8o_ActionTsImports += "import "+comp+" from '"+ action_ts_imports.get(comp) +"';"+ System.lineSeparator();
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
					Engine.logEngine.trace("(MobileBuilder) Ionic service ts file generated for 'app'");
				}
			}
		} catch (Exception e) {
			throw new EngineException("Unable to write ionic app service ts file",e);
		}
	}

	private void writeAppRoutingTs(ApplicationComponent app) throws EngineException {
		try {
			if (app != null) {
				String c8o_AppRoutes = "";
				int i=1;

				//Pages contributors
				List<PageComponent> pages = app.getPageComponentList();//getEnabledPages(app);
				for (PageComponent page : pages) {
					synchronized (page) {
						String pageDirName = pageDir(page).getName();
						String pageModuleName =  page.getName() + "Module";
						String pageModulePath = "./pages/" + pageDirName + "/" + page.getName().toLowerCase() + ".module";
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

				File appRoutingTpl = new File(ionicTplDir, "src/app-routing.module.tpl");
				String mContent = FileUtils.readFileToString(appRoutingTpl, "UTF-8");
				mContent = mContent.replaceAll("/\\*\\=c8o_AppRoutes\\*/", c8o_AppRoutes);

				File appRoutingTsFile = new File(appDir, "app-routing.module.ts");
				writeFile(appRoutingTsFile, mContent, "UTF-8");


				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Ionic routing module ts file generated for 'app'");
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
							if (comp.indexOf(" as ") != -1) {
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
		String c8o_PageArrayDef = "Array<>";
		String c8o_Version = app.getC8oVersion();
		String c8o_AppComponentMarkers = app.getComponentScriptContent().getString();
		String c8o_AppImports = app.getComputedImports();
		String c8o_AppDeclarations = app.getComputedDeclarations();
		String c8o_AppConstructors = app.getComputedConstructors();
		String c8o_AppFunctions = app.getComputedFunctions();
		int i=1;

		if (app.compareToTplVersion("7.9.0.2") >= 0) {
			//c8o_PageArrayDef = "Array<{title: string, titleKey: string, icon: string, iconPos: string, component: any, name: string, includedInAutoMenu?: boolean}>";
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

				i++;
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

				File appComponentTsFile = new File(appDir, "app.component.ts");
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

				File tempTsFile = new File(appDir, "app.component.temp.ts");

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
				File appHtmlFile = new File(appDir, "app.component.html");
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
				File appScssFile = new File(appDir, "app.component.scss");
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
				File themeScssFile = new File(themeDir, "variables.scss");
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

	private void deleteUselessPageDir(String pageName) {
		File pageDir = new File(pagesDir, pageName.toLowerCase());
		deleteDir(pageDir);
	}

	private void deleteUselessCompDir(String compName, String compQName) {
		File compDir = new File(componentsDir, UISharedComponent.getNsCompDirName(compQName, compName));
		deleteDir(compDir);
		if (initDone) {
			//updateConsumers(compName, compQName);
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
						//FileUtils.deleteDirectory(dir);
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
							//FileUtils.deleteDirectory(dir);
							deleteSourceDir(dir);
						}
					}
					catch(Exception e) {}
				}
			}
		}
	}

	private void call_writeAppSourceFiles(ApplicationComponent application) throws EngineException {
		ExecutorService executor = null;
		try {
			if (application == null) return;
			
			FileUtils.deleteQuietly(new File(appDir, "app.component.temp.ts"));
			
			List<Callable<String>> callList = new ArrayList<Callable<String>>();
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					long t0 = System.currentTimeMillis();
					writeAppPackageJson(application);
					long t1 = System.currentTimeMillis();
					return "["+Thread.currentThread().getName()+"] writeAppPackageJson for app done in "+ (t1-t0) + "ms [t0="+t0+"]";
				}
			});
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					long t0 = System.currentTimeMillis();
					writeAppBuildSettings(application);
					long t1 = System.currentTimeMillis();
					return "["+Thread.currentThread().getName()+"] writeAppBuildSettings for app done in "+ (t1-t0) + "ms [t0="+t0+"]";
				}
			});
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					long t0 = System.currentTimeMillis();
					writeAppPluginsConfig(application);
					long t1 = System.currentTimeMillis();
					return "["+Thread.currentThread().getName()+"] writeAppPluginsConfig for app done in "+ (t1-t0) + "ms [t0="+t0+"]";
				}
			});
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					long t0 = System.currentTimeMillis();
					writeAppServiceTs(application);
					long t1 = System.currentTimeMillis();
					return "["+Thread.currentThread().getName()+"] writeAppServiceTs for app done in "+ (t1-t0) + "ms [t0="+t0+"]";
				}
			});
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					long t0 = System.currentTimeMillis();
					writeAppModuleTs(application);
					long t1 = System.currentTimeMillis();
					return "["+Thread.currentThread().getName()+"] writeAppModuleTs for app done in "+ (t1-t0) + "ms [t0="+t0+"]";
				}
			});
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					long t0 = System.currentTimeMillis();
					writeAppComponentTs(application);
					long t1 = System.currentTimeMillis();
					return "["+Thread.currentThread().getName()+"] writeAppComponentTs for app done in "+ (t1-t0) + "ms [t0="+t0+"]";
				}
			});
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					long t0 = System.currentTimeMillis();
					writeAppTemplate(application);
					long t1 = System.currentTimeMillis();
					return "["+Thread.currentThread().getName()+"] writeAppTemplate for app done in "+ (t1-t0) + "ms [t0="+t0+"]";
				}
			});
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					long t0 = System.currentTimeMillis();
					writeAppStyle(application);
					long t1 = System.currentTimeMillis();
					return "["+Thread.currentThread().getName()+"] writeAppStyle for app done in "+ (t1-t0) + "ms [t0="+t0+"]";
				}
			});
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					long t0 = System.currentTimeMillis();
					writeAppTheme(application);
					long t1 = System.currentTimeMillis();
					return "["+Thread.currentThread().getName()+"] writeAppTheme for app done in "+ (t1-t0) + "ms [t0="+t0+"]";
				}
			});
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					long t0 = System.currentTimeMillis();
					writeAppRoutingTs(application);
					long t1 = System.currentTimeMillis();
					return "["+Thread.currentThread().getName()+"] writeAppRoutingTs for app done in "+ (t1-t0) + "ms [t0="+t0+"]";
				}
			});
			
			executor = Executors.newFixedThreadPool(10);
		    List<Future<String>> resultList = null;
			if (callList.size() > 0) {
			    try {
			      resultList = executor.invokeAll(callList);
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
			callList.clear();
			callList = null;
			
			if (initDone) {
				Engine.logEngine.trace("(MobileBuilder) Application source files generated for ionic project '"+ project.getName() +"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write application source files for ionic project '"+ project.getName() +"'",e);
		} finally {
			if (executor != null) {
				executor.shutdown();
			}
		}
	}
	
	private void writeAppSourceFiles(ApplicationComponent application) throws EngineException {
		try {
			if (application != null) {
				FileUtils.deleteQuietly(new File(appDir, "app.component.temp.ts"));

				writeAppPackageJson(application);
				writeAppBuildSettings(application);
				writeAppPluginsConfig(application);
				writeAppServiceTs(application);
				writeAppModuleTs(application);
				writeAppComponentTs(application);
				writeAppTemplate(application);
				writeAppStyle(application);
				writeAppTheme(application);
				writeAppRoutingTs(application);

				Engine.logEngine.trace("(MobileBuilder) Application source files generated for ionic project '"+ project.getName() +"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write application source files for ionic project '"+ project.getName() +"'",e);
		}
	}

	private void call_writePageSourceFiles(PageComponent page) throws EngineException {
		ExecutorService executor = null;
		String pageName = page.getName();
		try {
			File pageDir = pageDir(page);
			pageDir.mkdirs();

			FileUtils.deleteQuietly(new File(pageDir, pageName.toLowerCase() + ".temp.ts"));

			List<Callable<String>> callList = new ArrayList<Callable<String>>();
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					String s = Thread.currentThread().getName();
					long t0 = System.currentTimeMillis();
					writePageTs(page);
					long t1 = System.currentTimeMillis();
					return ("["+s+"] writePageTs for page " + page.getName() + " done in "+ (t1-t0) + "ms");
				}
			});
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					String s = Thread.currentThread().getName();
					long t0 = System.currentTimeMillis();
					writePageModuleTs(page);
					long t1 = System.currentTimeMillis();
					return ("["+s+"] writePageModuleTs for page " + page.getName() + " done in "+ (t1-t0) + "ms");
				}
			});
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					String s = Thread.currentThread().getName();
					long t0 = System.currentTimeMillis();
					writePageRoutingTs(page);
					long t1 = System.currentTimeMillis();
					return ("["+s+"] writePageRoutingTs for page " + page.getName() + " done in "+ (t1-t0) + "ms");
				}
			});
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					String s = Thread.currentThread().getName();
					long t0 = System.currentTimeMillis();
					writePageStyle(page);
					long t1 = System.currentTimeMillis();
					return ("["+s+"] writePageStyle for page " + page.getName() + " done in "+ (t1-t0) + "ms");
				}
			});
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					String s = Thread.currentThread().getName();
					long t0 = System.currentTimeMillis();
					writePageTemplate(page);
					long t1 = System.currentTimeMillis();
					return ("["+s+"] writePageTemplate for page " + page.getName() + " done in "+ (t1-t0) + "ms");
				}
			});
			
			executor = Executors.newFixedThreadPool(5);
		    List<Future<String>> resultList = null;
			if (callList.size() > 0) {
			    try {
			      resultList = executor.invokeAll(callList);
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
			callList.clear();
			callList = null;
			
			if (initDone) {
				Engine.logEngine.trace("(MobileBuilder) Ionic source files generated for page '"+pageName+"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write source files for page '"+pageName+"'",e);
		} finally {
			if (executor != null) {
				executor.shutdown();
			}
		}
	}
	
	private void writePageSourceFiles(PageComponent page) throws EngineException {
		String pageName = page.getName();
		try {
			File pageDir = pageDir(page);
			pageDir.mkdirs();

			FileUtils.deleteQuietly(new File(pageDir, pageName.toLowerCase() + ".temp.ts"));

			writePageTs(page);
			writePageModuleTs(page);
			writePageRoutingTs(page);
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

	private void deleteSourceDir(File dir) throws EngineException {
		try {
			if (dir != null && dir.exists()) {
				File deletedTsFile = new File(dir, FakeDeleted);
				writeFile(deletedTsFile, "", "UTF-8");

				if (initDone) {
					Engine.logEngine.trace("(MobileBuilder) Deleted directory '"+dir+"'");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page ts file",e);
		}
	}
	
	private void call_writeCompSourceFiles(UISharedComponent comp) throws EngineException {
		ExecutorService executor = null;
		String compName = comp.getName();
		try {
			File compDir = compDir(comp);
			compDir.mkdirs();

			FileUtils.deleteQuietly(new File(compDir, compFileName(comp) + ".temp.ts"));

			List<Callable<String>> callList = new ArrayList<Callable<String>>();
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					String s = Thread.currentThread().getName();
					long t0 = System.currentTimeMillis();
					writeCompTs(comp);
					long t1 = System.currentTimeMillis();
					return ("["+s+"] writeCompTs for component " + comp.getName() + " done in "+ (t1-t0) + "ms");
				}
			});
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					String s = Thread.currentThread().getName();
					long t0 = System.currentTimeMillis();
					writeCompModuleTs(comp);
					long t1 = System.currentTimeMillis();
					return ("["+s+"] writeCompModuleTs for component " + comp.getName() + " done in "+ (t1-t0) + "ms");
				}
			});
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					String s = Thread.currentThread().getName();
					long t0 = System.currentTimeMillis();
					writeCompStyle(comp);
					long t1 = System.currentTimeMillis();
					return ("["+s+"] writeCompStyle for component " + comp.getName() + " done in "+ (t1-t0) + "ms");
				}
			});
			callList.add(new Callable<String>() {
				@Override
				public String call() throws Exception {
					String s = Thread.currentThread().getName();
					long t0 = System.currentTimeMillis();
					writeCompTemplate(comp);
					long t1 = System.currentTimeMillis();
					return ("["+s+"] writeCompTemplate for component " + comp.getName() + " done in "+ (t1-t0) + "ms");
				}
			});
			
			executor = Executors.newFixedThreadPool(4);
		    List<Future<String>> resultList = null;
			if (callList.size() > 0) {
			    try {
			      resultList = executor.invokeAll(callList);
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
			callList.clear();
			callList = null;
			
			if (initDone) {
				Engine.logEngine.trace("(MobileBuilder) Ionic source files generated for component '"+compName+"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write source files for component '"+compName+"'",e);
		} finally {
			if (executor != null) {
				executor.shutdown();
			}
		}
	}

	private void writeCompSourceFiles(UISharedComponent comp) throws EngineException {
		String compName = comp.getName();
		try {
			File compDir = compDir(comp);
			compDir.mkdirs();

			FileUtils.deleteQuietly(new File(compDir, compFileName(comp) + ".temp.ts"));

			writeCompTs(comp);
			writeCompModuleTs(comp);
			writeCompStyle(comp);
			writeCompTemplate(comp);

			if (initDone) {
				Engine.logEngine.trace("(MobileBuilder) Ionic source files generated for component '"+compName+"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write source files for component '"+compName+"'",e);
		}
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
				// Deletion DOES NOT WORK for now
				/*Engine.logEngine.debug("(MobileBuilder) Defers the deletion of directory " + dir.getPath());
				dirsToDelete.add(dir);

				File nDir = toTmpFile(dir);
				if (nDir.exists()) {
					try {
						FileUtils.deleteDirectory(nDir);
					} catch (IOException e) {
						Engine.logEngine.warn("(MobileBuilder) Failed to delete temporary directory " + nDir.getPath(), e);
					}
				}*/

				// Replace segment in old page.ts to avoid deeplinks errors
				String oldPage = dir.getName();
				File oldPageDir = new File(pagesDir, oldPage);
				File oldPageTsFile = new File(oldPageDir, oldPage.toLowerCase() + ".ts");
				if (oldPageTsFile.exists()) {
					synchronized (writtenFiles) {
						if (writtenFiles.contains(oldPageTsFile)) {
							File oldPageTsFileTmp = toTmpFile(oldPageTsFile);
							if (oldPageTsFileTmp.exists()) {
								oldPageTsFile = oldPageTsFileTmp;
							}
						}
					}
					try {
						String tsContent = FileUtils.readFileToString(oldPageTsFile, "UTF-8");
						String oldSegment = PageComponent.SEGMENT_PREFIX + oldPage.toLowerCase();
						tsContent = tsContent.replaceFirst("segment\\s*\\:\\s*'(.+)'", "segment: '"+ oldSegment +"'");
						writeFile(oldPageTsFile, tsContent, "UTF-8");
					} catch (IOException e) {
						Engine.logEngine.warn("(MobileBuilder) Failed to defer write of " + oldPageTsFile.getPath(), e);
					}
				}
			}
		} else {
			try {
				FileUtils.deleteDirectory(dir);
			} catch (IOException e) {
				Engine.logEngine.warn("(MobileBuilder) Failed to delete directory " + dir.getPath(), e);
			}
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

			String pwaAppName = app.getParent().getComputedApplicationName();
			pwaAppName = pwaAppName.isEmpty() ? app.getName() : pwaAppName;
			index_content = index_content.replace("<!--c8o_App_Name-->", pwaAppName);

			File index = new File(ionicWorkDir, "src/index.html");
			writeFile(index, index_content, "UTF-8");

			// Set application name (manifest.webmanifest)
			File manifestFile = new File(ionicWorkDir, "src/manifest.webmanifest");
			if (manifestFile.exists()) {
				String jsonContent = FileUtils.readFileToString(manifestFile, "UTF-8");
				JSONObject jsonOb = new JSONObject(jsonContent);
				jsonOb.put("name", pwaAppName);
				jsonOb.put("short_name", pwaAppName);
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
						Engine.logEngine.trace("(MobileBuilder) No change for " + file.getPath());
						return;
					}
				}
				
				// write file
				if (buildMutex == null) {
					if (file.getPath().endsWith(FakeDeleted)) {
						try {
							File dir = file.getParentFile();
							FileUtils.deleteDirectory(dir);
							Engine.logEngine.debug("(MobileBuilder) Deleted dir " + dir.getPath());
						} catch (Exception e) {}
					} else {
						FileUtils.write(file, content, encoding);
						Engine.logEngine.debug("(MobileBuilder) Wrote file " + file.getPath());
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
						Engine.logEngine.trace("(MobileBuilder) Defers the deletion of " + nFile.getParentFile().getPath());
					} else {
						Engine.logEngine.trace("(MobileBuilder) Defers the write of " + content.length() + " chars to " + nFile.getPath());
					}
					if (pushedFiles != null) {
						synchronized (pushedFiles) {
							pushedFiles.put(file.getPath(), content);
						}
					}
				}
			}
		} else {
			//FileUtils.write(file, content, encoding);
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
		if (pushedFiles != null /*&& queue != null*/) {
			
			if (!isReleasing) {
				// retrieve necessary external component files
				Engine.logEngine.debug("(NgxBuilder) >>> moveFilesForce : updateConsumer");
				updateConsumer();
			}
			
			synchronized (pushedFiles) {
				int size = pushedFiles.size();
				if (size > 0) {
					Engine.logEngine.debug("(NgxBuilder) >>> moveFilesForce : "+ size +" files");
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
								//FileUtils.deleteDirectory(dir);
								org.apache.commons.io.FileUtils.forceDelete(dir);
								Engine.logEngine.debug("(MobileBuilder) Deleted dir " + dir.getPath());
								hasDeletedFiles = true;
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else {
							try {
								FileUtils.write(file, map.get(path), "UTF-8");
								Engine.logEngine.debug("(MobileBuilder) Wrote file " + file.getPath());
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
