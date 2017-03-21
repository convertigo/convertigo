/*
 * Copyright (c) 2001-2016 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine.mobile;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.FileUtils;

public class MobileBuilder {

	private BuilderCommand command = null;
	private Project project = null;
	boolean initDone = false;
	boolean watching = false;
	
	File projectDir, ionicTplDir, ionicWorkDir;
	
	static public void initBuilder(Project project) {
		if (project != null) {
			try {
				project.getMobileBuilder().init();
			} catch (Exception e) {
				Engine.logEngine.error("Failed to initialize mobile builder for project \""+project.getName()+"\"", e);
			}
		}
	}
	
	static public void releaseBuilder(Project project) {
		if (project != null) {
			try {
				project.getMobileBuilder().release();
			} catch (Exception e) {
				Engine.logEngine.error("Failed to release mobile builder for project \""+project.getName()+"\"", e);
			}
		}
	}
	
	public MobileBuilder(Project project) {
		this.project = project;
		this.command = new BuilderCommand();
		
		projectDir = new File(project.getDirPath());
		ionicTplDir = new File(projectDir,"ionicTpl");
		ionicWorkDir = new File(projectDir,"_private/ionic");
	}
	
	public boolean hasNodeModules() {
		File nodeModulesDir = new File(ionicWorkDir,"node_modules");
		return nodeModulesDir.exists();
	}
	
	public boolean isWatching() {
		return watching;
	}
	
	public void cmdStartWatch() throws BuilderException {
		if (!watching) {
			
			Engine.execute(new Runnable() {
				@Override
				public void run() {
					try {
						List<String> parameters = new LinkedList<String>();
						
						// Check for npm installation
						parameters.clear();
						parameters.add("--version");
						String npmVersion = command.run(ionicWorkDir, "npm", parameters, true);
						Pattern pattern = Pattern.compile("^([0-9])+\\.([0-9])+\\.([0-9])+$");
						Matcher matcher = pattern.matcher(npmVersion);			
						Engine.logEngine.debug("(MobileBuilder) npm version is "+ npmVersion);
						if (!matcher.find()){
							throw new MissingNodeJsException("You must download and install nodes.js from https://nodejs.org/en/download/");
						}
						
						// Check for node modules installation
						if (!hasNodeModules()) {
							throw new MissingNodeModules("Node modules not installed");
						}
						
						// Run npm watch for the app
						Engine.logEngine.debug("(MobileBuilder) starting watch...");
						watching = true;
						parameters.clear();
						parameters.add("run");
						parameters.add("watch");
						command.run(ionicWorkDir, "npm", parameters, true);
					}
					catch (Throwable e) {
						Engine.logEngine.error("(MobileBuilder) Start watch failed!", e);
						//throw new BuilderException("Start watch failed", e);
					}
					finally {
						watching = false;
					}
				}
			});
		}
	}
	
	public void cmdStopWatch() {
		if (watching) {
			Engine.logEngine.debug("(MobileBuilder) stopping watch...");
			command.cancel();
		}
	}
	
	public synchronized void appChanged() throws EngineException {
		if (initDone) {
			MobileApplication mobileApplication = project.getMobileApplication();
			if (mobileApplication != null) {
				ApplicationComponent application = mobileApplication.getApplicationComponent();
				if (application != null) {
					writeAppSourceFiles(application);
					Engine.logEngine.debug("(MobileBuilder) Handled 'appChanged'");
				}
			}
		}
	}

	public synchronized void routeChanged() throws EngineException {
		if (initDone) {
			MobileApplication mobileApplication = project.getMobileApplication();
			if (mobileApplication != null) {
				ApplicationComponent application = mobileApplication.getApplicationComponent();
				if (application != null) {
					writeAppSourceFiles(application);
					Engine.logEngine.debug("(MobileBuilder) Handled 'routeChanged'");
				}
			}
		}
	}
	
	public synchronized void pageAdded(final PageComponent page) throws EngineException {
		if (page != null && initDone) {
			if (!page.bNew) return;
			MobileApplication mobileApplication = project.getMobileApplication();
			if (mobileApplication != null) {
				ApplicationComponent application = mobileApplication.getApplicationComponent();
				if (application != null) {
					writePageSourceFiles(page);
					writeAppSourceFiles(application);
					Engine.logEngine.debug("(MobileBuilder) Handled 'pageAdded'");
				}
			}
		}
	}
	
	public synchronized void pageRemoved(final PageComponent page) throws EngineException {
		if (page != null && initDone) {
			MobileApplication mobileApplication = project.getMobileApplication();
			if (mobileApplication != null) {
				ApplicationComponent application = mobileApplication.getApplicationComponent();
				if (application != null) {
					writeAppSourceFiles(application);
					removeUselessPage(page.getName());
					Engine.logEngine.debug("(MobileBuilder) Handled 'pageRemoved'");
				}
			}
		}
	}
	
	public synchronized void pageRenamed(final PageComponent page, final String oldName) throws EngineException {
		if (page != null && initDone) {
			MobileApplication mobileApplication = project.getMobileApplication();
			if (mobileApplication != null) {
				ApplicationComponent application = mobileApplication.getApplicationComponent();
				if (application != null) {
					writePageSourceFiles(page);
					writeAppSourceFiles(application);
					removeUselessPage(oldName);
					Engine.logEngine.debug("(MobileBuilder) Handled 'pageRenamed'");
				}
			}
		}
	}
	
	public synchronized void pageComputed(final PageComponent page) throws EngineException {
		if (page != null && initDone) {
			writePageTemplate(page);
			Engine.logEngine.debug("(MobileBuilder) Handled 'pageComputed'");
		}
	}
	
	private boolean isIonicTemplateBased() {
		return ionicTplDir.exists();
	}
	
	private synchronized void init() throws EngineException {
		if (isIonicTemplateBased()) {
			// Copy template directory to working directory
			copyTemplateFiles();
			
			// Modify configuration files
			updateConfigurationFiles();
			
			// Write source files (based on bean components)
			updateSourceFiles();
		}
		initDone = true;
		Engine.logEngine.debug("(MobileBuilder) Initialized builder for ionic project '"+ project.getName() +"')");
	}
	
	private synchronized void release() throws EngineException {
		if (isIonicTemplateBased()) {
			// TODO ?
		}
		
		cmdStopWatch();
		
		initDone = false;
		Engine.logEngine.debug("(MobileBuilder) Released builder for ionic project '"+ project.getName() +"')");
	}
		
	private void copyTemplateFiles() throws EngineException {
		try {
			FileUtils.copyDirectory(ionicTplDir, ionicWorkDir);
			Engine.logEngine.debug("(MobileBuilder) Ionic template files copied");
		}
		catch (Exception e) {
			throw new EngineException("Unable to copy ionic template files",e);
		}
	}
	
	private void updateConfigurationFiles() throws EngineException {
		try {
			IOFileFilter fileFilter = FileFilterUtils.or(FileFilterUtils.suffixFileFilter("json"),FileFilterUtils.suffixFileFilter("xml"),FileFilterUtils.suffixFileFilter("js"));
			IOFileFilter dirFilter = FileFilterUtils.or(FileFilterUtils.nameFileFilter("config"));
			for (File f: FileUtils.listFiles(ionicWorkDir, fileFilter, dirFilter)) {
				String content = FileUtils.readFileToString(f, "UTF-8");
				content = content.replaceAll("../DisplayObjects","../../DisplayObjects");
				content = content.replaceAll("../Flashupdate","../../Flashupdate");
				FileUtils.write(f, content, "UTF-8");
			}
			Engine.logEngine.debug("(MobileBuilder) Ionic configuration files updated");
		}
		catch (Exception e) {
			throw new EngineException("Unable to update ionic configuration files",e);
		}
	}
	
	private void updateSourceFiles() throws EngineException {
		try {
			MobileApplication mobileApplication = project.getMobileApplication();
			if (mobileApplication != null) {
				ApplicationComponent application = mobileApplication.getApplicationComponent();
				if (application != null) {
					for (PageComponent page : application.getPageComponentList()) {
						writePageSourceFiles(page);
					}
					writeAppSourceFiles(application);
					removeUselessPages(application);
					
					Engine.logEngine.debug("(MobileBuilder) Ionic source files updated");
				}
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to update ionic source files",e);
		}
	}
		
	private void writePageTemplate(PageComponent page) throws EngineException {
		try {
			if (page != null) {
				String pageName = page.getName();
				File pageDir = new File(ionicWorkDir, "src/pages/"+pageName);
				File pageHtmlFile = new File(pageDir, pageName.toLowerCase() + ".html");
				String computedTemplate = page.getComputedTemplate();
				FileUtils.write(pageHtmlFile, computedTemplate, "UTF-8");
				
				Engine.logEngine.debug("(MobileBuilder) Ionic template file written for page '"+pageName+"'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write ionic page template file",e);
		}
	}
	
	private void removeUselessPage(String pageName) {
		File pageDir = new File(ionicWorkDir,"src/pages/"+ pageName);
		try {
			FileUtils.deleteDirectory(pageDir);
		}
		catch(Exception e) {}
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
				String c8o_PagesImport = "";
				String c8o_PagesLinks = "";
				String c8o_PagesVariables = "";
				String c8o_PagesDeclarations = "";
				String c8o_RootPage = "null";
				int i=1;
				
				
				List<PageComponent> pages = application.getPageComponentList();
				for (PageComponent page : pages) {
					String pageName = page.getName();
					boolean isRootPage = page.isRoot;
					boolean isLastPage = i == pages.size();
					if (isRootPage) c8o_RootPage = pageName;
					c8o_PagesImport += "import { "+pageName+" } from \"../pages/"+pageName+"/"+pageName.toLowerCase()+"\";\n";
					c8o_PagesLinks += " { component: "+pageName+", name: '"+pageName+"', segment: '"+pageName+"' }" + (isLastPage ? "":",");
					c8o_PagesVariables += " { title: '"+pageName+"', component: "+pageName+" }" + (isLastPage ? "":",");
					c8o_PagesDeclarations += " " + pageName + (isLastPage ? "":",");
					i++;
				}
				
				File appModuleTpl = new File(ionicTplDir, "src/app/app.module.ts");
				String mContent = FileUtils.readFileToString(appModuleTpl, "UTF-8");
				mContent = mContent.replaceAll("/\\*\\=c8o_PagesImport\\*/",c8o_PagesImport);
				mContent = mContent.replaceAll("/\\*\\=c8o_PagesLinks\\*/",c8o_PagesLinks);
				mContent = mContent.replaceAll("/\\*\\=c8o_PagesDeclarations\\*/",c8o_PagesDeclarations);
				File appModuleTsFile = new File(ionicWorkDir, "src/app/app.module.ts");
				FileUtils.write(appModuleTsFile, mContent, "UTF-8");
		
				String computedRoute = application.getComputedRoute();
				File appComponentTpl = new File(ionicTplDir, "src/app/app.component.ts");
				String cContent = FileUtils.readFileToString(appComponentTpl, "UTF-8");
				cContent = cContent.replaceAll("/\\*\\=c8o_PagesImport\\*/",c8o_PagesImport);
				cContent = cContent.replaceAll("/\\*\\=c8o_RootPage\\*/",c8o_RootPage);
				cContent = cContent.replaceAll("/\\*\\=c8o_PagesVariables\\*/",c8o_PagesVariables);
				cContent = cContent.replaceAll("/\\*\\=c8o_RoutingTable\\*/",computedRoute);
				File appComponentTsFile = new File(ionicWorkDir, "src/app/app.component.ts");
				FileUtils.write(appComponentTsFile, cContent, "UTF-8");
				
				File appHtmlFile = new File(ionicWorkDir, "src/app/app.html");
				//String computedTemplate = application.getComputedTemplate();
				//FileUtils.write(appHtmlFile, computedTemplate, "UTF-8");
				
				Engine.logEngine.debug("(MobileBuilder) Ionic source files generated for application 'app'");
			}
		}
		catch (Exception e) {
			throw new EngineException("Unable to write source files for application 'app'",e);
		}
	}
	
	private void writePageSourceFiles(PageComponent page) throws EngineException {
		String pageName = page.getName();
		try {
			String c8o_PageName = pageName;
			String c8o_PageTplUrl = pageName.toLowerCase() + ".html";
			String c8o_PageSelector = "page-"+pageName.toLowerCase();
			
			File pageDir = new File(ionicWorkDir,"src/pages/"+pageName);
			pageDir.mkdirs();
			
			File pageTplTs = new File(ionicTplDir, "src/page.tpl");
			String tsContent = FileUtils.readFileToString(pageTplTs, "UTF-8");
			tsContent = tsContent.replaceAll("/\\*\\=c8o_PageSelector\\*/","'"+c8o_PageSelector+"'");
			tsContent = tsContent.replaceAll("/\\*\\=c8o_PageTplUrl\\*/","'"+c8o_PageTplUrl+"'");
			tsContent = tsContent.replaceAll("/\\*\\=c8o_PageName\\*/",c8o_PageName);
			File pageTsFile = new File(pageDir, pageName.toLowerCase() + ".ts");
			FileUtils.write(pageTsFile, tsContent, "UTF-8");
			
			File pageScssFile = new File(pageDir, pageName.toLowerCase() + ".scss");
			String scssContent = c8o_PageSelector +" { }";
			FileUtils.write(pageScssFile, scssContent, "UTF-8");
			
			File pageHtmlFile = new File(pageDir, pageName.toLowerCase() + ".html");
			String computedTemplate = page.getComputedTemplate();
			FileUtils.write(pageHtmlFile, computedTemplate, "UTF-8");
			
			Engine.logEngine.debug("(MobileBuilder) Ionic source files generated for page '"+pageName+"'");
		}
		catch (Exception e) {
			throw new EngineException("Unable to write source files for page '"+pageName+"'",e);
		}
	}
	
}
