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

package com.twinsoft.convertigo.eclipse.wizards.deploy;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Composite;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.DeploymentConfiguration;
import com.twinsoft.convertigo.eclipse.DeploymentConfigurationReadOnly;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.ArchiveExportOption;
import com.twinsoft.convertigo.engine.events.StudioEvent;
import com.twinsoft.convertigo.engine.events.StudioEventListener;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.RemoteAdmin;

class ProjectDeployResultWizardPage extends WizardPage {
	private Project project = null;
	private boolean deployDone = false;

	private ProjectDeployResultComposite composite = null;
	private boolean canProcess = false;
	
	private Set<ArchiveExportOption> archiveExportOptions;
	private String convertigoServer;
	private boolean isHttps;
	private boolean trustAllCertificates;
	private String convertigoUserName;
	private String convertigoUserPassword;
	private boolean bAssembleXsl;
	
	ProjectDeployResultWizardPage(Project project) {
		super("ProjectDeployResultWizardPage", "Deployment result", null);
		this.project = project;
	}

	@Override
	public void createControl(Composite parent) {
		composite = new ProjectDeployResultComposite(parent, SWT.NONE);
		composite.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (canProcess) {
					canProcess = false;
					parent.getDisplay().asyncExec(() -> {
						doDeploy();
					});
				}
			}
		});
		
		setControl(composite);
		
	}

	private void doDeploy() {
		try {
			getWizard().getContainer().run(true, true, runnable);
		} catch (Exception e) {
			updateLabelText("Deploy in error: "+ e.getMessage());
			setDeployDone(false);
			e.printStackTrace();
		} finally {
			setPageComplete(deployDone);
		}
	}
	
	private IRunnableWithProgress runnable = new IRunnableWithProgress() {
	    public void run(IProgressMonitor monitor) throws InterruptedException {
	    	try {
				boolean doubleFound = false;

				Set<String> deploymentConfigurationNames = new HashSet<>();
				deploymentConfigurationNames = ConvertigoPlugin.deploymentConfigurationManager.getAllDeploymentConfigurationNames();
				String currentProjectName = ConvertigoPlugin.projectManager.currentProjectName;

				for (String deploymentConfigurationName: deploymentConfigurationNames) {
					DeploymentConfiguration deploymentConfiguration = null;
					if (convertigoServer.equals(deploymentConfigurationName)) {
						deploymentConfiguration = ConvertigoPlugin.deploymentConfigurationManager.get(deploymentConfigurationName);
						if (deploymentConfiguration != null && !(deploymentConfiguration instanceof DeploymentConfigurationReadOnly)) {
							deploymentConfiguration.setBAssembleXsl(bAssembleXsl);
							deploymentConfiguration.setBHttps(isHttps);
							deploymentConfiguration.setUsername(convertigoUserName);
							deploymentConfiguration.setUserpassword(convertigoUserPassword);
							deploymentConfiguration.setBTrustAllCertificates(trustAllCertificates);
							doubleFound = true;
							ConvertigoPlugin.deploymentConfigurationManager.setDefault(currentProjectName, deploymentConfiguration.getServer());
						}
					}
				}

				if (!doubleFound) {
					DeploymentConfiguration dc = new DeploymentConfiguration(
							convertigoServer,
							convertigoUserName,
							convertigoUserPassword,
							isHttps,
							trustAllCertificates,
							bAssembleXsl
							);
					
					ConvertigoPlugin.deploymentConfigurationManager.add(dc);
					ConvertigoPlugin.deploymentConfigurationManager.setDefault(currentProjectName, dc.getServer());
				}

				File projectDir = new File(ConvertigoPlugin.projectManager.currentProject.getDirPath() + "/_private");
				if (!projectDir.exists()) {
					ConvertigoPlugin.logInfo("Creating \"_private\" project directory");
					try {
						projectDir.mkdirs();
					}
					catch(Exception e) {
						String message = java.text.MessageFormat.format(
								"Unable to create the private project directory \"{0}\"..",
								new Object[] { ConvertigoPlugin.projectManager.currentProject.getName() }
								);
						throw new com.twinsoft.convertigo.engine.EngineException(message);
					}
				}

				ConvertigoPlugin.deploymentConfigurationManager.save();
				
	    		monitor.beginTask("Deploying", 4);
	    		
				monitor.subTask("Archive creation");
				ConvertigoPlugin.logDebug("Creation of the archive...");
				File file;
				try {
					file = CarUtils.makeArchive(new File(Engine.PROJECTS_PATH, project.getName() + ".car"), project, archiveExportOptions);
				} catch(com.twinsoft.convertigo.engine.EngineException e) {
					throw new com.twinsoft.convertigo.engine.EngineException("The archive creation has failed: (EngineException) "+ e.getMessage());
				}
				monitor.worked(1);
				ConvertigoPlugin.logDebug("Archive successfully generated!");

				monitor.subTask("Authenticating to the Convertigo server");
				RemoteAdmin remoteAdmin = new RemoteAdmin(convertigoServer, isHttps, trustAllCertificates);
				monitor.worked(1);
				
				monitor.subTask("Connection to the Convertigo server");
				ConvertigoPlugin.logDebug("Trying to connect to the Convertigo remote server...");
				ConvertigoPlugin.logDebug("Username: " + convertigoUserName);
				ConvertigoPlugin.logDebug("Password: " + "**************");
				remoteAdmin.login(convertigoUserName, convertigoUserPassword);
				monitor.worked(1);
				
				monitor.subTask("Deployment of the archive on the Convertigo server");
				remoteAdmin.deployArchive(file, bAssembleXsl);
				monitor.worked(1);
				
				String projectName = ConvertigoPlugin.projectManager.currentProject.getName();
				String projectURL = (isHttps ? "https" : "http") + "://" + convertigoServer + "/projects/" + projectName;
				updateLinkText("Your project has been successfully deployed.\n\nYou can try it with this URL:\n<a href=\""+ projectURL + "\">" + projectURL + "</a>");
				
				setDeployDone(true);
				ConvertigoPlugin.logDebug("Deployment successfull!");
				Engine.theApp.eventManager.dispatchEvent(new StudioEvent("deployment", project.getName()), StudioEventListener.class);
				
	    	} catch (Throwable e) {
				setDeployDone(false);
				updateLabelText(String.format("%1$s%n%2$s%n", "Deployment failed!", "Please have a look at the error log"));
				updateLinkText(null);
				ConvertigoPlugin.logException(e, "Unable to deploy project!");
			} finally {
	    		monitor.done();
	    	}
	    }
	};
	
	protected void setOptionsData(HashMap<String, Object> data) {
		this.archiveExportOptions = ArchiveExportOption.load(project.getDirFile());
		this.convertigoServer = (String) data.get("convertigoServer");
		this.convertigoUserName = (String) data.get("convertigoUserName");
		this.convertigoUserPassword = (String) data.get("convertigoUserPassword");
		this.isHttps = (Boolean) data.get("isHttps");
		this.trustAllCertificates = (Boolean) data.get("trustAllCertificates");
		this.bAssembleXsl = (Boolean) data.get("bAssembleXsl");
		
		canProcess = true;
	}
	
	private void setDeployDone(boolean done) {
		this.deployDone = done;
	}
	
	private void updateLabelText(String message) {
		if (composite != null) {
			composite.updateLabelText(message);
		}
	}
	
	private void updateLinkText(String text) {
		if (composite != null) {
			composite.updateLinkText(text);
		}
	}

	@Override
	public boolean isPageComplete() {
		return deployDone;
	}
}
