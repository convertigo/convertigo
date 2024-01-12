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

package com.twinsoft.convertigo.eclipse.wizards.deploy;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.beans.core.IApplicationComponent;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.MobileBuilderBuildMode;
import com.twinsoft.convertigo.engine.enums.NgxBuilderBuildMode;
import com.twinsoft.convertigo.engine.util.ProcessUtils;

class ProjectBuildWizardPage extends WizardPage {
	private static final int BUILD_DONE 	= 1;
	private static final int BUILD_CANCELED = 2;
	private static final int BUILD_FAILED 	= 3;
	
	private Project project = null;
	private boolean buildDone = false;
	
	private ProjectBuildComposite composite = null;
	
	ProjectBuildWizardPage(Project project) {
		super("ProjectBuildWizardPage", "Application build", null);
		this.project = project;
	}
	
	@Override
	public void createControl(Composite parent) {
		composite = new ProjectBuildComposite(parent, SWT.NONE);
		setControl(composite);
	}
	
	protected void doProcess() {
		if (getUnbuiltMessage() != null) {
			setBuildDone(false);
			updateLabelText("");
			process();
		}
	}
	
	private void process() {
		try {
			getWizard().getContainer().run(true, true, runnable);
		} catch (Exception e) {
			setBuildDone(false);
			updateLabelText("Unexpected error: "+ e.getMessage());
		} finally {
			setPageComplete(buildDone);
		}
	}
	
	private IRunnableWithProgress runnable = new IRunnableWithProgress() {
		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			String infoText = "--";
			try {
				ConvertigoPlugin.logInfo("For "+ project.getName() + " project: application build started");
				
				Pattern pRemoveEchap = Pattern.compile("\\x1b\\[\\d+m");
				File ionicDir = new File(project.getDirPath() + "/_private/ionic");
				if (!ionicDir.exists()) {
					throw new EngineException("Failed to perform NodeJS build, no folder: " + ionicDir);
				}
				
				boolean b_ngx = project.getMobileApplication().getApplicationComponent() instanceof com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
				boolean ngx = b_ngx;
				
				//Download NodeJS
				String nodeVersion = ProcessUtils.getNodeVersion(project);
				ConvertigoPlugin.logInfo("Requested nodeVersion: " + nodeVersion);
				monitor.beginTask("Download NodeJS " + nodeVersion, 1);
				File nodeDir = ProcessUtils.getNodeDir(nodeVersion, (pBytesRead, pContentLength, pItems) -> {
					//ConvertigoPlugin.logInfo("download NodeJS " + nodeVersion + ": " + Math.round(100f * pBytesRead / pContentLength) + "% [" + pBytesRead + "/" + pContentLength + "]");
				});
				monitor.worked(1);
				String nodePath = nodeDir.getAbsolutePath();
				
				// Installing node_modules
				monitor.beginTask("Installing node_modules", IProgressMonitor.UNKNOWN);
				ProcessBuilder pb;
				BufferedReader br;
				String line;
				if (ngx) {
					File packageLockTpl = new File(ionicDir, "package-lock-tpl.json");
					File packageLock = new File(ionicDir, "package-lock.json");
					if (packageLockTpl.exists() && !packageLock.exists()) {
						com.twinsoft.convertigo.engine.util.FileUtils.copyFile(packageLockTpl, packageLock);
					}
					pb = ProcessUtils.getNpmProcessBuilder(nodePath, "npm", "install", "--legacy-peer-deps");
				} else {
					pb = ProcessUtils.getNpmProcessBuilder(nodePath, "npm", "install", ionicDir.toString(), "--no-shrinkwrap", "--no-package-lock");
				}
				pb.redirectErrorStream(true);
				pb.directory(ionicDir);
				Process p = pb.start();
				br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				while (!monitor.isCanceled() && (line = br.readLine()) != null) {
					line = pRemoveEchap.matcher(line).replaceAll("");
					if (StringUtils.isNotBlank(line)) {
						ConvertigoPlugin.logInfo(line);
						monitor.subTask(line);
						monitor.worked(1);
					}
				}
				if (monitor.isCanceled()) {
					throw new Exception("Install canceled");
				}
				int code = p.waitFor();
				if (code != 0) {
					throw new EngineException("Installation return a '" + code + "' failure code");
				}
				monitor.done();
				
				//Remove previous build directory
				monitor.beginTask("Removing previous build directory", 5);
				monitor.worked(1);
				File displayObjectsMobile = new File(project.getDirPath(), "DisplayObjects/mobile");
				displayObjectsMobile.mkdirs();
				monitor.worked(1);
				for (File f: displayObjectsMobile.listFiles()) {
					if (!f.getName().equals("assets")) {
						com.twinsoft.convertigo.engine.util.FileUtils.deleteQuietly(f);
					}
				}
				monitor.worked(3);
				
				// Building
				pb = null; p = null;
				ngx = b_ngx;
				if (ngx) {
					monitor.beginTask("Launching the " + NgxBuilderBuildMode.prod.label() + " build", 200);
					String endPointUrl = project.getMobileApplication().getEndpoint();
					if (endPointUrl.isBlank()) {
						endPointUrl = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_ENDPOINT);
						if (endPointUrl.isBlank()) {
							endPointUrl = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
						}
					}
					
					String appBaseHref = "/convertigo/projects/"+ project.getName() +"/DisplayObjects/mobile/";
					try {
						appBaseHref = (endPointUrl.isEmpty() ? "/convertigo": endPointUrl.replaceFirst("https?://.*?/", "/").replaceFirst("(/.*)/.*?$", "$1")) + 
											"/projects/"+ project.getName() +"/DisplayObjects/mobile/";
					} catch (Exception e) {}
					
					pb = ProcessUtils.getNpmProcessBuilder(nodePath, "npm", "run", NgxBuilderBuildMode.prod.command(), "--nobrowser");
					pb.environment().put("NODE_OPTIONS", "max-old-space-size=8192");
					List<String> cmd = pb.command();
					cmd.add("--");
					cmd.add("--base-href="+ appBaseHref); // #393 add base href for project's web app
					
					pb.redirectErrorStream(true);
					pb.directory(ionicDir);
					p = pb.start();
					Matcher matcher = Pattern.compile("(\\d+)% (.*)").matcher("");
					int lastProgress = 0;
					br = new BufferedReader(new InputStreamReader(p.getInputStream()));
					while (!monitor.isCanceled() && (line = br.readLine()) != null) {
						line = pRemoveEchap.matcher(line).replaceAll("");
						if (StringUtils.isNotBlank(line)) {
							matcher.reset(line);
							if (matcher.find()) {
								if (lastProgress == 0) {
									monitor.beginTask("Webpack in progress", 200);
								}
								int progress = Integer.parseInt(matcher.group(1));
								int diff = progress - lastProgress;
								lastProgress = progress;
								monitor.subTask(matcher.group(2));
								monitor.worked(diff);
								if (progress == 100) {
									lastProgress = 0;
									monitor.beginTask("Build almost finish", 200);
								}
							} else {
								monitor.worked(1);
							}
							ConvertigoPlugin.logInfo(line);
						}
					}
				} else {
					monitor.beginTask("Launching the " + MobileBuilderBuildMode.production.label() + " build", IProgressMonitor.UNKNOWN);
					pb = ProcessUtils.getNpmProcessBuilder(nodeDir.getAbsolutePath(), "npm", "run", MobileBuilderBuildMode.production.command(), "--nobrowser");
					pb.redirectErrorStream(true);
					pb.directory(ionicDir);
					p = pb.start();
					br = new BufferedReader(new InputStreamReader(p.getInputStream()));
					while (!monitor.isCanceled() && (line = br.readLine()) != null) {
						line = pRemoveEchap.matcher(line).replaceAll("");
						if (StringUtils.isNotBlank(line)) {
							ConvertigoPlugin.logDebug(line);
							monitor.subTask(line);
							monitor.worked(1);
						}
					}
				}
				
				if (monitor.isCanceled()) {
					throw new Exception("Build canceled");
				}
				code = p.waitFor();
				if (code != 0) {
					throw new EngineException("Build return a '" + code + "' failure code");
				}
				setBuildDone(true);
				infoText = getInfoText(BUILD_DONE);
				ConvertigoPlugin.logInfo("For "+ project.getName() + " project: application build is successfull");
	    	} catch(Throwable e) {
	    		setBuildDone(true);
	    		String msg = e.getMessage();
				if (monitor.isCanceled()) {
		    		infoText = getInfoText(BUILD_CANCELED);
					ConvertigoPlugin.logInfo("For "+ project.getName() + " project: unabled to finish application build, "+ msg);
				} else {
		    		infoText = getInfoText(BUILD_FAILED);
					ConvertigoPlugin.logInfo("For "+ project.getName() + " project: unabled to build application, "+ msg);
					ConvertigoPlugin.logException(e, "Unabled to build application!");
				}
	    	} finally {
	    		monitor.done();
				updateLabelText(infoText);
	    	}
		}
	};
	
	private String getInfoText(int status) {
		String text = "--";
		switch (status) {
			case BUILD_CANCELED:text = "Build canceled!"; break;
			case BUILD_FAILED: 	text = "Build failed ! Have a look at the log files for more information."; break;
		}
		if (status == BUILD_DONE) {
			return String.format("%1$s%n%2$s%n", 
					"Build ended successfully.", 
					"Click on next to select the export options.");
		} else {
			return String.format("%1$s%n%2$s%n%3$s%n", 
					text, 
					"Be aware that your application will not work if you deploy it as is!", 
					"If you still want to continue the deployment, click on next to select the export options.");
		}
	}
	
	private void setBuildDone(boolean done) {
		buildDone = done;
	}
	
	private void updateLabelText(String message) {
		if (composite != null) {
			composite.updateLabelText(message);
		}
	}
	
	private String getUnbuiltMessage() {
		if (project != null) {
			IApplicationComponent app = project.getMobileApplication() != null ? project.getMobileApplication().getApplicationComponent() : null;
			return app != null ? app.getUnbuiltMessage() : null;
		}
		return null;
	}

	
	@Override
	public IWizardPage getPreviousPage() {
		if (getUnbuiltMessage() == null) {
			return null;
		}
		return super.getPreviousPage();
	}
	
	@Override
	public boolean isPageComplete() {
		return buildDone;
	}
}
