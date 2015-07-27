/*
 * Copyright (c) 2001-2011 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.dialogs;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.DeploymentConfiguration;
import com.twinsoft.convertigo.eclipse.DeploymentConfigurationReadOnly;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.RemoteAdmin;
import com.twinsoft.convertigo.engine.util.RemoteAdminException;

public class ProjectDeployDialog extends MyAbstractDialog implements Runnable {

	private ProjectDeployDialogComposite projectDeployDialogComposite;
	
	private ProgressBar progressBar = null;
	private Label labelProgression = null;

	private boolean bFinished = false;
	String convertigoServer = "?";
	String convertigoUserName = "";
	String convertigoUserPassword = "";
	boolean trustAllCertificates = false;
	boolean isHttps = false;
	boolean bAssembleXsl = false;
	private List<TestCase> listTestCasesSelected = new ArrayList<TestCase>(); 
	
	public ProjectDeployDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle, List<TestCase> listTestCasesSelected) {
		super(parentShell, dialogAreaClass, dialogTitle, 460, 500);
		this.listTestCasesSelected = listTestCasesSelected;
	}
	
	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttonBar =  super.createButtonBar(parent);
		projectDeployDialogComposite = (ProjectDeployDialogComposite) dialogComposite;		
		getButton(IDialogConstants.OK_ID).setText("Deploy");
		projectDeployDialogComposite.setOkButton(getButton(IDialogConstants.OK_ID));
		return buttonBar;
	}

	@Override
	protected void cancelPressed() {		
		try {
			ConvertigoPlugin.deploymentConfigurationManager.save();
		} catch (IOException e) {
			ConvertigoPlugin.logException(e, "Unable to save the deployment configurations");
		}	
		super.cancelPressed();
	}
	
	@Override
	protected void okPressed() {
		
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			progressBar = projectDeployDialogComposite.progressBar;
			labelProgression = projectDeployDialogComposite.labelProgress;

			convertigoServer = projectDeployDialogComposite.convertigoServer.getText();
	        if ((convertigoServer == null) || (convertigoServer.equals(""))) return;
	        
	        convertigoUserName = projectDeployDialogComposite.convertigoAdmin.getText();
	        if (convertigoUserName == null) convertigoUserName = "";
	        
	        convertigoUserPassword = projectDeployDialogComposite.convertigoPassword.getText();
	        if (convertigoUserPassword == null) convertigoUserPassword = "";
	        
	        isHttps = projectDeployDialogComposite.checkBox.getSelection();
	        trustAllCertificates = projectDeployDialogComposite.checkTrustAllCertificates.getSelection();
	        bAssembleXsl = projectDeployDialogComposite.assembleXsl.getSelection();
        
	        close();
			Job deployBack = new Job("Deployment in progress...") {
				
				@Override
				protected IStatus run(IProgressMonitor arg0) {
					try {
						boolean doubleFound = false;
							
				        Set<String> deploymentConfigurationNames = new HashSet<String>();  
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
				        	
			//	            list.add(convertigoServer);
				            ConvertigoPlugin.deploymentConfigurationManager.add(dc);
				            ConvertigoPlugin.deploymentConfigurationManager.setDefault(currentProjectName, dc.getServer());
			//		        if (list.getItem(0).equals(ProjectDeployDialogComposite.messageList)) {
			//		        	list.remove(0);
			//		        }
				            projectDeployDialogComposite.fillList();
				        }
			
				        File projectDir = new File(Engine.PROJECTS_PATH + "/" + ConvertigoPlugin.projectManager.currentProject.getName() + "/_private");
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
				                ConvertigoPlugin.logException(e, message);
				                return Status.CANCEL_STATUS;
				            }
				        }
			
				        ConvertigoPlugin.deploymentConfigurationManager.save();
				        
				        deployInBack();
					}
					catch (Throwable e) {
						ConvertigoPlugin.logException(e, "Unable to deploy project!");
						return Status.CANCEL_STATUS;
					}
					
					return Status.OK_STATUS;
				}
		};

		deployBack.setUser(true);
		deployBack.schedule(); 
	}
	
	private void deployInBack(){
		run();
	}
	
	public void run() {
		final Display display = getParentShell().getDisplay();
		Thread progressBarThread = new Thread("Progress Bar thread") {
			
			@Override
			public void run() {
				int i = 0;
				while (true) {
					try {
						i += 5;
						if (i >= 100) i = 0;
						final int j = i;
						display.asyncExec(new Runnable() {
							public void run() {
								if (!progressBar.isDisposed())
									progressBar.setSelection(j);
							}
						});
						
						sleep(500);
					}
					catch(InterruptedException e) {
						break;
					}
				}
			}
			
		};
		
		try {
			progressBarThread.start();
			deploy();
			display.asyncExec(new Runnable() {
				public void run() {
					setReturnCode(OK);
					close();
				}
			});

			display.syncExec(new Runnable() {
				public void run() {
					String projectName = ConvertigoPlugin.projectManager.currentProject.getName();
					String projectURL = (isHttps ? "https" : "http") + "://" + convertigoServer + "/projects/" + projectName;
					Shell shell = display.getActiveShell();
					ProjectDeploySuccessfulDialog projectDeploySuccessfulDialog = new ProjectDeploySuccessfulDialog(shell, projectURL);
					projectDeploySuccessfulDialog.open();
				};
			});
		}
		catch (final EngineException e) {
			final String errorMessage = e.getMessage();
			final String causeStackTrace;
			
			if (e instanceof RemoteAdminException) {
				RemoteAdminException rae = (RemoteAdminException) e;
				causeStackTrace = rae.stackTrace;
			}
			else {
				Throwable cause = e.getCause();
				if (cause != null) {
					Writer result = new StringWriter();
					PrintWriter printWriter = new PrintWriter(result);
					e.printStackTrace(printWriter);
					causeStackTrace = result.toString();
				}
				else {
					causeStackTrace = null;
				}
			}
	
			if (causeStackTrace != null) {
				ConvertigoPlugin.logDeployException(e, errorMessage, causeStackTrace);
			}
			else {
				ConvertigoPlugin.logError(e.getMessage(), true);
			}
		}
		finally {
			progressBarThread.interrupt();
			display.asyncExec(new Runnable() {
				public void run() {
					if (!progressBar.isDisposed())
						progressBar.setSelection(0);
				}
			});
		}
	}

	public void setTextLabel(String text) {
		final Display display = getParentShell().getDisplay();
		final String labelText = text;
		display.asyncExec(new Runnable() {
			public void run() {
				if (!labelProgression.isDisposed())
					labelProgression.setText(labelText);
			}
		});
	}

	private void deploy() throws EngineException, RemoteAdminException {
		try {
			String projectName = ConvertigoPlugin.projectManager.currentProject.getName();

			setTextLabel("Archive creation");
			ConvertigoPlugin.logDebug("Creation of the archive...");
			try {				
				if (listTestCasesSelected.size() > 0) {
					CarUtils.makeArchive(ConvertigoPlugin.projectManager.currentProject, listTestCasesSelected);
				} else {
					CarUtils.makeArchive(ConvertigoPlugin.projectManager.currentProject);
				}
			}
			catch(com.twinsoft.convertigo.engine.EngineException e) {
				throw new com.twinsoft.convertigo.engine.EngineException("The archive creation has failed: (EngineException) "+ e.getMessage());
			}
		
			ConvertigoPlugin.logDebug("Archive successfully generated!");
            
			setTextLabel("Authenticating to the Convertigo server");
			
			if (convertigoServer.indexOf('/') == -1) convertigoServer += "/convertigo";
			
			RemoteAdmin remoteAdmin = new RemoteAdmin(convertigoServer, isHttps, trustAllCertificates);
	
			ConvertigoPlugin.logDebug("Trying to connect to the Convertigo remote server...");
     		ConvertigoPlugin.logDebug("Username: " + convertigoUserName);
			ConvertigoPlugin.logDebug("Password: " + convertigoUserPassword);

			setTextLabel("Connection to the Convertigo server");
		   
			remoteAdmin.login(convertigoUserName, convertigoUserPassword);
			
			setTextLabel("Deployment of the archive on the Convertigo server");
            
			File file = new File(Engine.PROJECTS_PATH + "/" + projectName + ".car");
			
			remoteAdmin.deployArchive(file,bAssembleXsl);

			bFinished = true;

			ConvertigoPlugin.logDebug("Deployment successfull!");
		}
		finally {
			final Display display = getParentShell().getDisplay();
			if (display != null)
				display.asyncExec(new Runnable() {
					public void run() {
						if (bFinished) {
							setTextLabel("The archive deployment has been correctly done.");
							if (getButton(IDialogConstants.OK_ID) != null) {
								getButton(IDialogConstants.OK_ID).setEnabled(false);
								getButton(IDialogConstants.CANCEL_ID).setText("Finish");
							}
						}
						else {
							if (getButton(IDialogConstants.OK_ID) != null)
								getButton(IDialogConstants.OK_ID).setEnabled(true);
							setTextLabel("Progression");
						}
					}
				});
		}
	}
	
    //Create an ADBBean and provide it as the test object
	public org.apache.axis2.databinding.ADBBean getTestObject(Class<?> type) throws java.lang.Exception {
	   return (org.apache.axis2.databinding.ADBBean) type.newInstance();
	}
}
