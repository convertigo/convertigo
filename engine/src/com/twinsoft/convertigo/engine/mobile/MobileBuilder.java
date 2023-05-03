/*
 * Copyright (c) 2001-2023 Convertigo SA.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IApplicationComponent;
import com.twinsoft.convertigo.beans.core.IPageComponent;
import com.twinsoft.convertigo.beans.core.ISharedComponent;
import com.twinsoft.convertigo.beans.core.IUIComponent;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.MobileBuilderBuildMode;
import com.twinsoft.convertigo.engine.helpers.BatchOperationHelper;
import com.twinsoft.convertigo.engine.util.EventHelper;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;

public abstract class MobileBuilder {

	protected static Pattern LsPattern = Pattern.compile("\\R");
	protected static Pattern CacheVersion = Pattern.compile("const\\sCACHE_VERSION\\s\\=\\s\\d+");

	protected Project project = null;
	protected Object buildMutex = null;
	
	// Until we can delete page folder again, we need to retrieve contributors of
	// all pages for action.beans.service, otherwise we get compilation errors in
	// page.ts files for (deleted/disabled) pages containing pseudo-actions
	protected boolean forceEnable = true;
	
	private boolean needPkgUpdate = false;
	protected boolean initDone = false;
	protected boolean isReleasing = false;
	protected boolean autoBuild = true;
	protected boolean isPWA = false;
	
	protected File projectDir, ionicTplDir, ionicWorkDir;
	protected Set<File> writtenFiles = new HashSet<File>();
	protected Map<String, CharSequence> pushedFiles = null;
	protected BlockingQueue<Map<String, CharSequence>> queue = null;
	protected String tplVersion = null;
	
	private boolean[] isBuilding = {false};

	protected MobileBuilderBuildMode buildMode = MobileBuilderBuildMode.fast;
	
	static public void initBuilder(Project project) {
		initBuilder(project, false);
	}
	
	static public void initBuilder(Project project, boolean force) {
		if ((Engine.isStudioMode() || force) && project != null && project.getMobileApplication() != null && project.getMobileApplication().getApplicationComponent() != null) {
			try {
				project.getMobileBuilder().init();
			} catch (Exception e) {
				String message = e.getMessage();
				message = message == null ? "unknown":message;
				if (Engine.isCliMode()) {
					throw new RuntimeException("Failed to initialize mobile builder for project '" + project.getName() + "'\n" + message, e);
				}
				if (message.startsWith("Missing template project") || message.contains("is required for this")) {
					Engine.logEngine.error("Failed to initialize mobile builder for project '" + project.getName() + "'\n" + message);
				} else {
					Engine.logEngine.error("Failed to initialize mobile builder for project '" +project.getName() + "'", e);
				}
			}
		}
	}
	
	static public void releaseBuilder(Project project) {
		releaseBuilder(project, false);
	}

	static public void releaseBuilder(Project project, boolean force) {
		if ((Engine.isStudioMode() || force) && project != null && project.getMobileApplication() != null && project.getMobileApplication().getApplicationComponent() != null) {
			try {
				project.getMobileBuilder().release();
			} catch (Exception e) {
				Engine.logEngine.error("Failed to release mobile builder for project \""+project.getName()+"\"", e);
			}
		}
	}
	
	static public MobileBuilder getBuilderOf(Object object) {
		try {
			if (object != null && object instanceof DatabaseObject) {
				return ((DatabaseObject)object).getProject().getMobileBuilder();
			}
		} catch (Exception e) {}
		return null;
	}
	
    static String projectName(String qname) {
    	return ComponentRefManager.projectName(qname);
    }
    
	static public int compareVersions(String v1, String v2) {
		return VersionUtils.compare(v1, v2);
	}
	
	static public String getMarkers(String content) {
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
	
	static public String getMarker(String s, String markerId) {
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
	
	static public String getFormatedContent(String marker, String markerId) {
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
	
	static public void initMapImports(Map<String,String> map, String tsContent) {
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
	
	static protected Map<String,String> initTplImports(File file) {
		Map<String, String> map = new HashMap<String, String>(10);
		try {
			String tsContent = FileUtils.readFileToString(file, "UTF-8");
			initMapImports(map, tsContent);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}
	
	static public MobileBuilder getInstance(Project project) throws EngineException {
		IApplicationComponent app;
		try {
			app = project.getMobileApplication().getApplicationComponent();
		} catch (NullPointerException e) {
			// for project without mobile application
			return null;
		}
		// IONIC-ANGULAR : Ionic3Builder
		if ("com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent".equals(app.getClass().getName())) {
			return new Ionic3Builder(project);
		}
		// ANGULAR : NgxBuilder
		else if ("com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent".equals(app.getClass().getName())) {
			return new NgxBuilder(project);
		}
		throw new EngineException("Builder for "+ app.getClass().getName() + " isn't implemented yet");
	}
	
	private String builderType = "MobileBuilder";
	
	protected MobileBuilder(Project project) {
		this.project = project;
		builderType = this.getClass().getSimpleName();
		projectDir = new File(project.getDirPath());
		ionicWorkDir = new File(projectDir,"_private/ionic");
	}
	
	protected EventHelper eventHelper;
	
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

	synchronized void firePackageUpdated() {
		if (eventHelper != null) {
			for (MobileEventListener mobileEventListener: eventHelper.getListeners(MobileEventListener.class)) {
				mobileEventListener.onPackageUpdated();
			}
		}
	}

	protected void updateEnvFile() {
		
	}
	
	public boolean isInitialized() {
		return initDone;
	}
	
	protected boolean isAppPwaAble() {
		return isPWA;
	}
	
	public boolean isAutoBuild() {
		return autoBuild;
	}
	
	public void setAutoBuild(boolean autoBuild) {
		Engine.logEngine.debug("("+ builderType +") AutoBuild mode set to "+ (autoBuild ? "ON":"OFF"));
		this.autoBuild = autoBuild;
		if (autoBuild) {
			moveFilesForce();
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

	public boolean isIonicTemplateBased() {
		return ionicTplDir.exists();
	}
	
	public void setAppBuildMode(MobileBuilderBuildMode buildMode) {
		this.buildMode = buildMode;
	}
	
	public void setBuildMutex(Object mutex) {
		buildMutex = mutex;
		FileUtils.deleteQuietly(new File(projectDir,"_private/ionic_tmp"));
	}
	
	public void setNeedPkgUpdate(boolean needPkgUpdate) {
		this.needPkgUpdate = needPkgUpdate;
	}
	
	public boolean getNeedPkgUpdate() {
		return this.needPkgUpdate;
	}
		
	protected void cleanDirectories() {
		FileUtils.deleteQuietly(new File(projectDir,"_private/ionic/src"));
		FileUtils.deleteQuietly(new File(projectDir,"_private/ionic/version.json"));
		FileUtils.deleteQuietly(new File(projectDir,"_private/ionic_tmp"));
		Engine.logEngine.trace("("+ builderType +") Directories cleaned for ionic project '"+ project.getName() +"'");
	}
	
	protected void copyTemplateFiles() throws EngineException {
		try {
			FileUtils.copyDirectory(ionicTplDir, ionicWorkDir);
			Engine.logEngine.trace("("+ builderType +") Template files copied for ionic project '"+ project.getName() +"'");
		}
		catch (Exception e) {
			throw new EngineException("Unable to copy ionic template files for ionic project '"+ project.getName() +"'",e);
		}
	}
	
	protected void copyAssetsToBuildDir() throws EngineException {
		
	}
	
	protected void updateConfigurationFiles() throws EngineException {
		
	}
	
	protected void updateTplVersion() {
		if (tplVersion == null) {
			File versionJson = new File(ionicWorkDir, "version.json"); // since 7.5.2
			if (versionJson.exists()) {
				try {
					String tsContent = FileUtils.readFileToString(versionJson, "UTF-8");
					JSONObject jsonOb = new JSONObject(tsContent);
					tplVersion = jsonOb.getString("version");
					Engine.logEngine.debug("("+ builderType +") Template version: "+ tplVersion+ " for ionic project '"+ project.getName() +"'");
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
		updateTplVersion();
		return tplVersion;
	}
	
	public boolean hasTplAppCompTsImport(String name) {
		return getTplAppCompTsImports().containsKey(name);
	}
	
	public boolean hasTplPageTsImport(String name) {
		return getTplPageTsImports().containsKey(name);
	}
	
	protected void writeWorker(File file) throws IOException {
		writeWorker(file, false);
	}
	
	protected static File toTmpFile(File file) {
		return new File(file.getAbsolutePath().replaceFirst("_private(/|\\\\)ionic", "_private$1ionic_tmp"));
	}
		
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
					FileUtils.write(file, content, encoding);
					Engine.logEngine.debug("("+ builderType +") Wrote file " + file.getPath());
				}
				// defers the write of file
				else {
					// write to temporary directory (for file edition)
					writtenFiles.add(file);
					File nFile = toTmpFile(file);
					nFile.getParentFile().mkdirs();
					FileUtils.write(nFile, content, encoding);
					
					// store files modifications
					Engine.logEngine.debug("("+ builderType +") Defers the write of " + content.length() + " chars to " + nFile.getPath());
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
	
	protected void moveFiles() {
		if (autoBuild) {
			StackTraceElement parentMethod = Thread.currentThread().getStackTrace()[3];
			if (!parentMethod.getClassName().equals("com.twinsoft.convertigo.engine.mobile.MobileBuilder")) {
				moveFilesForce();// // written files in queue are pushed to build dir
			} else {
				if (buildMutex != null) {
					Engine.logEngine.warn("("+ builderType +") moveFilesForce not called", new Throwable("Invalid stack call"));
				}
			}
		}
	}
	
	protected synchronized void moveFilesForce() {
		// push files modifications to queue
		if (pushedFiles != null && queue != null) {
			synchronized (pushedFiles) {
				int size = pushedFiles.size();
				if (size > 0) {
					Engine.logEngine.debug("("+ builderType +") >>> moveFilesForce : "+ size +" files");
					Map<String, CharSequence> map = new HashMap<String, CharSequence>();
					map.putAll(Collections.unmodifiableMap(pushedFiles));
					if (queue.offer(map)) {
						pushedFiles.clear();
					}
				}
			}
		}
	}
	
	public void prepareBatchBuild() {
		if (isAutoBuild()) {
			setAutoBuild(false);
			BatchOperationHelper.prepareEnd(() -> {
				setAutoBuild(true);
			});
		}
	}

	protected abstract void init() throws EngineException;
	protected abstract void release() throws EngineException;
	
	protected abstract void writeWorker(File file, boolean bForce) throws IOException;
	protected abstract Map<String, String> getTplAppCompTsImports();
	protected abstract Map<String, String> getTplPageTsImports();
	protected abstract Map<String, String> getTplCompTsImports();
	
	public abstract void pageAdded(final IPageComponent pageComponent) throws EngineException;
	public abstract void pageRemoved(final IPageComponent pageComponent) throws EngineException;
	public abstract void pageRenamed(final IPageComponent object, String oldName) throws EngineException;
	public abstract void pageEnabled(final IPageComponent pageComponent) throws EngineException;
	public abstract void pageDisabled(final IPageComponent pageComponent) throws EngineException;
	public abstract void pageTsChanged(final IPageComponent pageComponent, boolean b) throws EngineException;
	public abstract void pageModuleTsChanged(final IPageComponent pageComponent) throws EngineException;
	public abstract void pageStyleChanged(final IPageComponent pageComponent) throws EngineException;
	public abstract void pageTemplateChanged(final IPageComponent pageComponent) throws EngineException;
	
	public abstract void compAdded(final ISharedComponent sharedComponent) throws EngineException;
	public abstract void compRemoved(final ISharedComponent sharedComponent) throws EngineException;
	public abstract void compRenamed(final ISharedComponent sharedComponent, String oldName) throws EngineException;
	public abstract void compTsChanged(final ISharedComponent sharedComponent, boolean b) throws EngineException;
	public abstract void compModuleTsChanged(final ISharedComponent sharedComponent) throws EngineException;
	public abstract void compTemplateChanged(final ISharedComponent sharedComponent) throws EngineException;
	public abstract void compStyleChanged(final ISharedComponent sharedComponent) throws EngineException;
	
	public abstract void appContributorsChanged(final IApplicationComponent applicationComponent) throws EngineException;
	public abstract void appTsChanged(final IApplicationComponent applicationComponent, boolean b) throws EngineException;
	public abstract void appStyleChanged(final IApplicationComponent applicationComponent) throws EngineException;
	public abstract void appThemeChanged(final IApplicationComponent applicationComponent) throws EngineException;
	public abstract void appRouteChanged(final IApplicationComponent applicationComponent) throws EngineException;
	public abstract void appTemplateChanged(final IApplicationComponent applicationComponent) throws EngineException;
	public abstract void appRootChanged(final IApplicationComponent applicationComponent) throws EngineException;
	public abstract void appCompTsChanged(final IApplicationComponent applicationComponent) throws EngineException;
	public abstract void appModuleTsChanged(final IApplicationComponent applicationComponent) throws EngineException;
	public abstract void appPwaChanged(final IApplicationComponent applicationComponent) throws EngineException;

	public abstract String getFunctionTempTsRelativePath(final IUIComponent uiComponent) throws EngineException;
	public abstract void writeFunctionTempTsFile(final IUIComponent uiComponent, String functionMarker) throws EngineException;
	public abstract String getTempTsRelativePath(final IApplicationComponent applicationComponent) throws EngineException;
	public abstract void writeAppComponentTempTs(final IApplicationComponent applicationComponent) throws EngineException;

	public abstract String getTempTsRelativePath(final IPageComponent pageComponent) throws EngineException;
	public abstract String getTempTsRelativePath(final ISharedComponent sharedComponent) throws EngineException;
	public abstract void writePageTempTs(final IPageComponent pageComponent) throws EngineException;
	public abstract void writeCompTempTs(final ISharedComponent sharedComponent) throws EngineException;

	public void appChanged() throws EngineException {
		
	}
}
