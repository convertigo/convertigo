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

package com.twinsoft.convertigo.eclipse.wizards.new_project;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.api.Git;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

import com.twinsoft.api.Session;
import com.twinsoft.convertigo.beans.connectors.CicsConnector;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.connectors.SapJcoConnector;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.references.RemoteFileReference;
import com.twinsoft.convertigo.beans.references.RestServiceReference;
import com.twinsoft.convertigo.beans.references.WebServiceReference;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.ImportWsReference;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;
import com.twinsoft.convertigo.engine.util.ProjectUtils;

class NewProjectWizard extends Wizard implements INewWizard, IExecutableExtension {
	// Wizard Pages
	private boolean doPage1;
	private boolean doPage2;
	private boolean doPage4;
	private boolean doPage5;
	private boolean doPage6;
	private boolean doPage7;
	private boolean doPage8;
	private boolean doPage9;
	private boolean doPage10;
	private boolean doPage11;
	private boolean doPageSummarySampleProject;
	private boolean doConfigureSQLConnectorPage;
	private boolean doConfigureSAPConnectorPage;
	
	NewProjectWizardPage1 page1;
	NewProjectWizardPage2 page2;
	private NewProjectWizardPage4 page4;
	NewProjectWizardPage5 page5;
	NewProjectWizardPage6 page6;
	ServiceCodeWizardPage page7;
	private NewProjectWizardPage8 page8;
	private NewProjectWizardPage9 page9;
	private NewProjectWizardPage10 page10;
	private NewProjectWizardPage11 page11;
	private NewProjectWizardPageSummarySampleProject pageSummarySampleProject;
	private ConfigureSQLConnectorPage configureSQLConnectorPage;
	private ConfigureSAPConnectorPage configureSAPConnectorPage;

	// Holds the current selection when the wizard was called
	private ISelection selection;

	// The wizard pages will be used to configure this data
	String wizardId;
	private ProjectUrlParser projectUrlParser;
	private String projectName;

	/**
	 * Constructor for SampleNewWizard.
	 */
	public NewProjectWizard() {
		super();
		setNeedsProgressMonitor(true);
		setWindowTitle("Create a new project");
	}

	/**
	 * Add the pages to the wizard according to the templateID
	 */
	public void addPages() {
		if (!Engine.isStarted) {
			addPage(new NoEngineWizardPage(selection));
			return;
		}
		if (doPageSummarySampleProject) {
			pageSummarySampleProject = new NewProjectWizardPageSummarySampleProject(selection);
			addPage(pageSummarySampleProject);
		}
		if (doPage1) {
			page1 = new NewProjectWizardPage1(selection);
			addPage(page1);
		}
		if (doPage2) {
			page2 = new NewProjectWizardPage2(selection);
			addPage(page2);
		}
		if (doConfigureSQLConnectorPage) {
			configureSQLConnectorPage = new ConfigureSQLConnectorPage(selection);
			addPage(configureSQLConnectorPage);
		}
		if (doConfigureSAPConnectorPage) {
			configureSAPConnectorPage = new ConfigureSAPConnectorPage(selection);
			addPage(configureSAPConnectorPage);
		}
		if (doPage7) {
			page7 = new ServiceCodeWizardPage(selection);
			addPage(page7);
		}
		if (doPage4) {
			page4 = new NewProjectWizardPage4(selection);
			addPage(page4);
		}
		if (doPage5) {
			page5 = new NewProjectWizardPage5(selection);
			addPage(page5);
		}
		if (doPage6) {
			page6 = new NewProjectWizardPage6(selection);
			addPage(page6);
		}
		if (doPage8) {
			page8 = new NewProjectWizardPage8(selection);
			addPage(page8);
		}
		if (doPage9) {
			page9 = new NewProjectWizardPage9(selection);
			addPage(page9);
		}
		if (doPage10) {
			page10 = new NewProjectWizardPage10(selection);
			addPage(page10);
		}
		if (doPage11) {
			page11 = new NewProjectWizardPage11(selection);
			addPage(page11);
		}
	}

	/**
	 * This method is called when 'Finish' button is pressed in the wizard. We
	 * will create an operation and run it using wizard as execution context.
	 */
	public boolean performFinish() {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};

		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}

	private void updateProjectTreeView() {
		ConvertigoPlugin.getDisplay().asyncExec(() -> {
			// refresh the project explorer treeview
			ProjectExplorerView view = (ProjectExplorerView) ConvertigoPlugin.getDefault().getProjectExplorerView();

			try {
				if (projectName != null) {
					view.importProjectTreeObject(projectName);
				}
			} catch (CoreException e) {
				ConvertigoPlugin.logException(e, "An error occured while refreshing the tree view");
			}
			view.viewer.refresh();
		});
	}
	/**
	 * The worker method. We create the project here according to the templateId
	 * variable
	 */
	private void doFinish(IProgressMonitor monitor) throws CoreException {
		if (!Engine.isStarted) {
			ConvertigoPlugin.asyncExec(() -> ConvertigoPlugin.getDefault().runSetup());
			return;
		}
		try {
			Project project = null;
			if (page10 != null) {
				projectName = page1.getProjectName();
				monitor.beginTask("Creating project " + projectName, 7);
				project = createFromBlankProject(monitor, false);

				boolean needAuth = page10.useAuthentication();
				String wsURL = page10.getWsdlURL().toString();
				String login = page10.getLogin();
				String password = page10.getPassword();
				
				WebServiceReference webWsReference = null;
				RestServiceReference restWsReference = null;
				RemoteFileReference reference = null;
				
				if (wizardId.equals("com.twinsoft.convertigo.eclipse.wizards.NewWebServiceSoapReferenceWizard")) {
					reference = webWsReference = new WebServiceReference();
				} else if (wizardId.equals("com.twinsoft.convertigo.eclipse.wizards.NewWebServiceSwaggerReferenceWizard")) {
					reference = restWsReference = new RestServiceReference();
				}
				
				reference.setUrlpath(wsURL);
				reference.setNeedAuthentication(needAuth);
				reference.setAuthUser(login == null ? "":login);
				reference.setAuthUser(password == null ? "":password);
				reference.bNew = true;
				
				ImportWsReference wsr = webWsReference != null ? 
						new ImportWsReference(webWsReference) : new ImportWsReference(restWsReference);
				
				HttpConnector httpConnector = wsr.importInto(project);
				if (httpConnector != null) {
					Connector defaultConnector = project.getDefaultConnector();
					project.setDefaultConnector(httpConnector);
					defaultConnector.delete();
					project.hasChanged = true;
				}
				
				updateProjectTreeView();
			}
			
			else if (page1 != null) {
				projectName = page1.getProjectName();
				monitor.beginTask("Creating project " + projectName, 7);
				project = createFromBlankProject(monitor);
			}
			
			else if (pageSummarySampleProject != null) {
				monitor.beginTask("Creating project", 7);
				createFromArchiveProject(monitor);
				return;
			}
			
			if (project != null) {
				String autoCreate = ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_AUTO_CREATE_PROJECT_GIT_REPOSITORY);;
				if (!"true".equalsIgnoreCase(autoCreate)) {
					return;
				}
				try (Git git = Git.init().setDirectory(project.getDirFile()).call()) {
					git.add().addFilepattern(".").call();
					git.commit().setMessage("Initial commit").call();

					@SuppressWarnings("restriction")
					boolean ok = org.eclipse.egit.core.RepositoryUtil.INSTANCE.addConfiguredRepository(git.getRepository().getDirectory());
					if (ok) {
						ConvertigoPlugin.getDisplay().asyncExec(() -> {
							try {
								IProject iproject = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
								iproject.close(null);
								iproject.open(null);
							} catch (Exception e) {
								ConvertigoPlugin.logException(e, "An error occured while refreshing git state for the project", false);
							}
						});
					}
				} catch (Exception e) {
					ConvertigoPlugin.logException(e, "An error occured while create git repository for the project", false);
				}
			}
			
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "An error occured while creating the project", false);
			
			String message = "An error occured while creating the project (see Error log):\n"+ e.getMessage();
			IStatus status = new Status(Status.ERROR, ConvertigoPlugin.PLUGIN_UNIQUE_ID, message, e);
			throw new CoreException(status);
		}
	}
	
	private Project createFromBlankProject(IProgressMonitor monitor) throws Exception {
		return createFromBlankProject(monitor, true);
	}
	
	private Project createFromBlankProject(IProgressMonitor monitor, boolean updateTreeView) throws Exception {
		String newProjectName = projectName;
		String oldProjectName = projectUrlParser.getProjectName();
		
		monitor.setTaskName("Creating new project");
		monitor.worked(1);
		
		if (Engine.theApp.databaseObjectsManager.existsProject(newProjectName)) {
			throw new EngineException(
					"Unable to create new project ! A project with the same name (\""
							+ newProjectName + "\") already exists.");
		}
		monitor.setTaskName("Loading the projet");
		monitor.worked(1);
		projectUrlParser.setProjectName(projectName);
		Project newProject = Engine.theApp.referencedProjectManager.importProject(projectUrlParser, true);
		monitor.worked(1);

		try {
			// set values of elements to configure on the new project
			String newEmulatorTechnology = "";
			String emulatorTechnologyName = "";
			String newIbmTerminalType = "";
			
			switch (wizardId) {
			case "com.twinsoft.convertigo.eclipse.wizards.New3270ConnectorWizard":
			case "com.twinsoft.convertigo.eclipse.wizards.New3270WebWizard":
				newEmulatorTechnology = Session.SNA;
				newIbmTerminalType = "IBM-3279";
				emulatorTechnologyName = "IBM3270";
				break;
			case "com.twinsoft.convertigo.eclipse.wizards.New5250ConnectorWizard":
			case "com.twinsoft.convertigo.eclipse.wizards.New5250WebWizard":
				newEmulatorTechnology = Session.AS400;
				newIbmTerminalType = "IBM-3179";
				emulatorTechnologyName = "IBM5250";
				break;
			case "com.twinsoft.convertigo.eclipse.wizards.NewDKUConnectorWizard":
			case "com.twinsoft.convertigo.eclipse.wizards.NewDKUWebWizard":
				newEmulatorTechnology = Session.DKU;
				emulatorTechnologyName = "BullDKU7107";
				break;
			case "com.twinsoft.convertigo.eclipse.wizards.NewVT220ConnectorWizard":
				newEmulatorTechnology = Session.VT;
				emulatorTechnologyName = "UnixVT220";
				break;
			}
			
			monitor.setTaskName("Reset project version");
			monitor.worked(1);
			newProject.setVersion("");
			
			monitor.setTaskName("Change connector name");
			monitor.worked(1);
			String oldConnectorName = "void";
			String newConnectorName = "void";
			boolean connectorChanged = false;
			
			if (page2 != null) {
				newConnectorName = page2.getConnectorName();
				monitor.setTaskName("Connector renamed");
				monitor.worked(1);
				oldConnectorName = newProject.getDefaultConnector().getName();
				newProject.getDefaultConnector().setName(newConnectorName);
				connectorChanged = true;
			}

			
			if (page5 != null) {
				CicsConnector cicsConnector = (CicsConnector) newProject.getDefaultConnector();
				monitor.setTaskName("Update connector mainframe name");
				monitor.worked(1);
				cicsConnector.setMainframeName(page5.getCtgName());
				
				monitor.setTaskName("Update connector server");
				monitor.worked(1);
				cicsConnector.setServer(page5.getCtgServer());
				
				monitor.setTaskName("Update connector port");
				monitor.worked(1);
				try {
					cicsConnector.setPort(Integer.parseInt(page5.getCtgPort()));
				} catch (Exception e) {
				}
			}
			
			if (page6 != null) {
				monitor.setTaskName("Update connector server");
				monitor.worked(1);
				// change connector server and port,
				// change https mode
				// and change proxy server and proxy port
				HttpConnector httpConnector = (HttpConnector) newProject.getDefaultConnector();
				httpConnector.setServer(page6.getHttpServer());
				
				monitor.setTaskName("Update connector port");
				monitor.worked(1);
				try {
					httpConnector.setPort(Integer.parseInt(page6.getHttpPort()));
				} catch (Exception e) {
				}
				
				monitor.setTaskName("Update connector https mode");
				monitor.worked(1);

				httpConnector.setHttps(page6.isBSSL());
			}
			
			if (page7 != null) {
				// change emulator technology
				// and change service code
				monitor.setTaskName("Set connector service code");
				monitor.worked(1);
				JavelinConnector javelinConnector = (JavelinConnector) newProject.getDefaultConnector();
				javelinConnector.setServiceCode(page7.getServiceCode());
				
				monitor.setTaskName("Set connector emulator technology");
				monitor.worked(1);
				javelinConnector.setEmulatorTechnology(newEmulatorTechnology);
				
				monitor.setTaskName("Set terminal type");
				monitor.worked(1);
				javelinConnector.setIbmTerminalType(newIbmTerminalType);
				
				// rename emulatorTechnology criteria
				monitor.setTaskName("Rename emulator technology criteria");
				monitor.worked(1);
				javelinConnector.getDefaultScreenClass().getLocalCriterias().get(0).setName(emulatorTechnologyName);
			}
			
			if (page11 != null) {
				SiteClipperConnector scConnector = (SiteClipperConnector) newProject.getDefaultConnector();
				monitor.setTaskName("Update connector certificates policy");
				monitor.worked(1);
				scConnector.setTrustAllServerCertificates(page11.isTrustAllServerCertificates());
				
				monitor.setTaskName("Update host url");
				monitor.worked(1);
				scConnector.getDefaultTransaction().setTargetURL(page11.getTargetURL());
			}
			
			if (configureSQLConnectorPage != null) {
				SqlConnector sqlConnector = (SqlConnector) newProject.getDefaultConnector();
				monitor.setTaskName("Update JDBC URL");
				monitor.worked(1);
				sqlConnector.setJdbcDriverClassName(configureSQLConnectorPage.getJdbcDriver());
				monitor.setTaskName("Update Username");
				monitor.worked(1);
				sqlConnector.setJdbcURL(configureSQLConnectorPage.getJdbcURL());
				monitor.setTaskName("Update JDBC driver");
				monitor.worked(1);
				sqlConnector.setJdbcUserName(configureSQLConnectorPage.getUsername());
				monitor.setTaskName("Update Password");
				monitor.worked(1);
				sqlConnector.setJdbcUserPassword(configureSQLConnectorPage.getPassword());
			}
			
			if (configureSAPConnectorPage != null) {
				SapJcoConnector sapConnector = (SapJcoConnector) newProject.getDefaultConnector();
				// Application Server Host
				monitor.setTaskName("Update application Server Host");
				monitor.worked(1);
				sapConnector.setAsHost(configureSAPConnectorPage.getAsHost());
				
				// System Number
				monitor.setTaskName("Update system number");
				monitor.worked(1);
				sapConnector.setSystemNumber(configureSAPConnectorPage.getSystemNumber());
				
				// Client
				monitor.setTaskName("Update client");
				monitor.worked(1);
				sapConnector.setClient(configureSAPConnectorPage.getClient());
				
				// User
				monitor.setTaskName("Update user");
				monitor.worked(1);
				sapConnector.setUser(configureSAPConnectorPage.getUser());
				// Password
				monitor.setTaskName("Update password");
				monitor.worked(1);
				sapConnector.setPassword(configureSAPConnectorPage.getPassword());
				// Language
				monitor.setTaskName("Update language");
				monitor.worked(1);
				sapConnector.setLanguage(configureSAPConnectorPage.getLanguage());
			}
			
			monitor.setTaskName("Saving updated project");
			monitor.worked(1);
			
			Engine.theApp.databaseObjectsManager.exportProject(newProject);

			monitor.setTaskName("New project saved");
			monitor.worked(1);
			
			try {
				File eProject = new File(newProject.getDirPath(), ".project");
				if (eProject.exists()) {
					String txt = FileUtils.readFileToString(eProject, StandardCharsets.UTF_8);
					txt = txt.replaceFirst("(<name>)(.*?)(</name>)", "$1" + newProjectName + "$3");
					FileUtils.writeStringToFile(eProject, txt, StandardCharsets.UTF_8);
				}
				String xsdInternalPath = newProject.getDirPath() + "/" + Project.XSD_FOLDER_NAME + "/" + Project.XSD_INTERNAL_FOLDER_NAME;
				File xsdInternalDir = new File(xsdInternalPath).getCanonicalFile();
				if (xsdInternalDir.exists() && connectorChanged) {
					boolean needConnectorRename = !oldConnectorName.equals(newConnectorName);
					if (needConnectorRename) {
						File srcDir = new File(xsdInternalDir, oldConnectorName);
						File destDir = new File(xsdInternalDir, newConnectorName);
						
						if (oldConnectorName.equalsIgnoreCase(newConnectorName)) {
							File destDirTmp = new File(xsdInternalDir, "tmp" + oldConnectorName).getCanonicalFile();
							FileUtils.moveDirectory(srcDir, destDirTmp);
							srcDir = destDirTmp;
						}
						FileUtils.moveDirectory(srcDir, destDir);
					}
	
					for (File connectorDir : xsdInternalDir.listFiles()) {
						if (connectorDir.isDirectory()) {
							String connectorName = connectorDir.getName();
							for (File transactionXsdFile : connectorDir.listFiles()) {
								String xsdFilePath = transactionXsdFile.getCanonicalPath();
								ProjectUtils.xsdRenameProject(xsdFilePath, oldProjectName, newProjectName);
								if (needConnectorRename && connectorName.equals(newConnectorName)) {
									ProjectUtils.xsdRenameConnector(xsdFilePath, oldConnectorName, newConnectorName);
								}
							}
						}
					}
				}
				
				monitor.setTaskName("Schemas updated");
				monitor.worked(1);
				
				if (updateTreeView) {
					updateProjectTreeView();
				}
			} catch (Exception e) {
				Engine.logDatabaseObjectManager.error("An error occured while updating transaction schemas", e);
			}
			
		} catch (Exception e) {
			// Delete everything
			try {
				Engine.logBeans
						.error("An error occured while creating project, everything will be deleted. Please see Studio logs for more informations.",
								null);
				// TODO : see if we can delete oldProjectName : a real project
				// could exist with this oldProjectName ?
				// Engine.theApp.databaseObjectsManager.deleteProject(oldProjectName,
				// false, false);
				Engine.theApp.databaseObjectsManager.deleteProject(newProjectName, false, false);
				projectName = null;
			} catch (Exception ex) {
			}

			throw new Exception("Unable to create project from template", e);
		}

		return newProject;
	}

	private void createFromArchiveProject(IProgressMonitor monitor) throws Exception {
		monitor.setTaskName("Creating new project");
		monitor.worked(1);

		if (Engine.theApp.databaseObjectsManager.existsProject(projectName)) {
			String message = "Unable to create new project ! A project with the same name (\"" + projectName
					+ "\") already exists.";
			projectName = null; // avoid load of project in view
			throw new EngineException(message);
		}
		Job.create("Loading " + projectName, (mon) -> {
			try {
				mon.beginTask("Loading " + projectName, IProgressMonitor.UNKNOWN);
				try {
					Engine.theApp.referencedProjectManager.importProject(projectUrlParser, true);
				} catch (Exception e) {
					// Catch exception because part of project could have been
					// deployed
					// User will be able to remove it from the Studio
					ConvertigoPlugin.logException(e, "Unable to import project '" + projectName + "'!");
				}

				updateProjectTreeView();
			} catch (Exception e) {
				// Delete everything
				try {
					Engine.logBeans
					.error("An error occured while creating project, everything will be deleted. Please see Studio logs for more informations.",
							null);
					Engine.theApp.databaseObjectsManager.deleteProject(projectName, false, false);
				} catch (Exception ex) {
				}
				ConvertigoPlugin.errorMessageBox("Unable to create project " + projectName + ": " + e.getMessage());
				projectName = null;
			}
			mon.done();
		}).schedule();
	}

	/**
	 * We will accept the selection in the workbench to see if we can initialize
	 * from it.
	 * 
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException {
		String value = config.getValue();
		try {
			wizardId = config.getAttribute("id");
			JSONObject json = new JSONObject(value);
			String url = json.getString("url");
			projectUrlParser = new ProjectUrlParser(url);
			projectName = projectUrlParser.getProjectName();
			doPage1 = json.has("doPage1") && json.getBoolean("doPage1");
			doPage2 = json.has("doPage2") && json.getBoolean("doPage2");
			doPage4 = json.has("doPage4") && json.getBoolean("doPage4");
			doPage5 = json.has("doPage5") && json.getBoolean("doPage5");
			doPage6 = json.has("doPage6") && json.getBoolean("doPage6");
			doPage7 = json.has("doPage7") && json.getBoolean("doPage7");
			doPage8 = json.has("doPage8") && json.getBoolean("doPage8");
			doPage9 = json.has("doPage9") && json.getBoolean("doPage9");
			doPage10 = json.has("doPage10") && json.getBoolean("doPage10");
			doPage11 = json.has("doPage11") && json.getBoolean("doPage11");
			doPageSummarySampleProject = json.has("doPageSummarySampleProject") && json.getBoolean("doPageSummarySampleProject");
			doConfigureSQLConnectorPage = json.has("doConfigureSQLConnectorPage") && json.getBoolean("doConfigureSQLConnectorPage");
			doConfigureSAPConnectorPage = json.has("doConfigureSAPConnectorPage") && json.getBoolean("doConfigureSAPConnectorPage");
		} catch (JSONException e) {
		}
	}

}