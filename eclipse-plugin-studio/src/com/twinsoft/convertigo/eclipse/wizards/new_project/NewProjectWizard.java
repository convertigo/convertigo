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

package com.twinsoft.convertigo.eclipse.wizards.new_project;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;

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
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.ImportWsReference;
import com.twinsoft.convertigo.engine.util.ProjectUtils;

public class NewProjectWizard extends Wizard implements INewWizard {
	// Wizard Pages
	public NewProjectWizardPage1 page1;
	public NewProjectWizardPage2 page2;
	public NewProjectWizardPage3 page3;
	public NewProjectWizardPage4 page4;
	public NewProjectWizardPage5 page5;
	public NewProjectWizardPage6 page6;
	public ServiceCodeWizardPage page7;
	public NewProjectWizardPage8 page8;
	public NewProjectWizardPage9 page9;
	public NewProjectWizardPage10 page10;
	public NewProjectWizardPage11 page11;
	public NewProjectWizardPageSummarySampleProject pageSummarySampleProject;
	public ConfigureSQLConnectorPage configureSQLConnectorPage;
	public ConfigureSAPConnectorPage configureSAPConnectorPage;

	// Holds the current selection when the wizard was called
	private ISelection selection;

	// The wizard pages will be used to configure this data
	public int templateId;
	public String projectName;
	public String connectorName;
	public String mobileDeviceName;
	public String theme = "default";

	// ENTRIES IN WIZARD
	// Default project templates
	static final int TEMPLATE_WEB_HTML_IBM_3270 = 100;
	static final int TEMPLATE_WEB_HTML_IBM_5250 = 101;
	static final int TEMPLATE_WEB_HTML_BULL_DKU_7107 = 102;
	static final int TEMPLATE_WEB_GREENSCREEN_IBM_3270 = 150;
	static final int TEMPLATE_WEB_GREENSCREEN_IBM_5250 = 151;
	static final int TEMPLATE_MOBILE_IBM_3270 = 200;
	static final int TEMPLATE_MOBILE_IBM_5250 = 201;
	static final int TEMPLATE_EAI_IBM_3270 = 300;
	static final int TEMPLATE_EAI_IBM_5250 = 301;
	static final int TEMPLATE_EAI_BULL_DKU_7107 = 302;
	static final int TEMPLATE_EAI_UNIX_VT220 = 304;
	static final int TEMPLATE_EAI_HTTP = 305;
	static final int TEMPLATE_EAI_CICS_COMMEAREA = 306;
	static final int TEMPLATE_EAI_HTML_WEB_SITE = 307;
	static final int TEMPLATE_SEQUENCE_CONNECTOR = 500;
	static final int TEMPLATE_WEB_SERVICE_REST_REFERENCE = 700;
	static final int TEMPLATE_WEB_SERVICE_SOAP_REFERENCE = 701;
	static final int TEMPLATE_WEB_SERVICE_SWAGGER_REFERENCE = 702;
	static final int TEMPLATE_SQL_CONNECTOR = 400;
	static final int TEMPLATE_SITE_CLIPPER = 1100;
	static final int TEMPLATE_SAP_CONNECTOR = 1200;
	static final int TEMPLATE_MOBILE_BUILDER = 1400;
	
	// documentation samples
	static final int SAMPLE_DOCUMENTATION_CLI = 600;
	static final int SAMPLE_DOCUMENTATION_CLP = 601;
	static final int SAMPLE_DOCUMENTATION_CWI = 602;
	static final int SAMPLE_DOCUMENTATION_CWC = 603;
	static final int SAMPLE_DOCUMENTATION_CMS = 604;
	static final int SAMPLE_DOCUMENTATION_CMC = 605;
	static final int SAMPLE_DOCUMENTATION_SITECLIPPER = 607;
	// reference manual samples
	static final int SAMPLE_REFMANUAL_SITECLIPPER = 650;
	static final int SAMPLE_REFMANUAL_STATEMENTS = 651;
	static final int SAMPLE_REFMANUAL_STEPS = 652;
	static final int SAMPLE_REFMANUAL_VARIABLES = 653;
	static final int SAMPLE_REFMANUAL_HTTP = 654;
	static final int SAMPLE_REFMANUAL_WEBCLIPPER = 655;
	// SQL samples
	static final int SAMPLE_SQL_HSQLDB = 660;
	static final int SAMPLE_SQL_XLS = 661;
	// libraries
	static final int LIBRARY_PUSH_MANAGER = 804;
	static final int LIBRARY_TWITTER = 805;
	static final int LIBRARY_OAUTH = 806;
	static final int LIBRARY_AMAZON_LEX = 807;
	static final int LIBRARY_FILE_TRANSFER = 808;
	static final int LIBRARY_SIGFOX = 809;
	static final int LIBRARY_SALESFORCE = 810;
	static final int LIBRARY_FULLSYNC_GRP = 811;
	static final int LIBRARY_USERMANAGER = 812;
	
	// mobile samples
	static final int SAMPLE_OFFCHAT = 1317;
	static final int SAMPLE_RETAILSTORE = 1318;
	static final int SAMPLE_CONFERENCEAPP = 1319;
	static final int SAMPLE_FLIGHTSHARE = 1320;
	static final int SAMPLE_HELLOWORLD = 1321;
	
	// rest samples
	static final int SAMPLE_MAPPERSHOP = 1330;
	
	// sequencer samples
	static final int SAMPLE_SEQUENCER_1 = 510;

	// FILE NAMES
	// to import blank project
	private static final String JAVELIN_PUBLISHER_TEMPLATE_PROJECT_FILE_NAME = "template_javelin.car";
	private static final String DKU_PUBLISHER_TEMPLATE_PROJECT_FILE_NAME = "template_javelinDKU.car";
	private static final String JAVELIN_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME = "template_javelinIntegration.car";
	private static final String WEB_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME = "template_webIntegration.car";
	private static final String HTTP_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME = "template_HTTP.car";
	private static final String CICS_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME = "template_CICS.car";
	private static final String SEQUENCE_TEMPLATE_PROJECT_FILE_NAME = "template_sequence.car";
	private static final String SQL_TEMPLATE_PROJECT_FILE_NAME = "template_SQL.car";
	private static final String SAP_TEMPLATE_PROJECT_FILE_NAME = "template_SAP.car";
	private static final String SITE_CLIPPER_TEMPLATE_PROJECT_FILE_NAME = "template_siteClipper.car";
	private static final String MOBILE_BUILDER_TEMPLATE_PROJECT_FILE_NAME = "template_mobileBuilderIonic.car";
	// documentation samples
	private static final String CLI_DOC_PROJECT_FILE_NAME = "sample_documentation_CLI.car";
	private static final String CWI_DOC_PROJECT_FILE_NAME = "sample_documentation_CWI.car";
	private static final String CLP_DOC_PROJECT_FILE_NAME = "sample_documentation_CLP.car";
	private static final String CWC_DOC_PROJECT_FILE_NAME = "sample_documentation_CWC.car";
	private static final String CMS_DOC_PROJECT_FILE_NAME = "sample_documentation_CMS.car";
	private static final String CMC_DOC_PROJECT_FILE_NAME = "sample_documentation_CMC.car";
	private static final String SITECLIPPER_DOC_PROJECT_FILE_NAME = "sampleDocumentationSiteClipper.car";
	// mobile samples
	private static final String SAMPLE_OFFCHAT_PROJECT_FILE_NAME = "sampleMobileOffChat.car";
	private static final String SAMPLE_RETAILSTORE_PROJECT_FILE_NAME = "sampleMobileRetailStore.car";
	private static final String SAMPLE_CONFERENCEAPP_PROJECT_FILE_NAME = "IonicConferenceApp.car";
	private static final String SAMPLE_FLIGHTSHARE_PROJECT_FILE_NAME = "FlightShare.car";
	private static final String SAMPLE_HELLOWORLD_PROJECT_FILE_NAME = "sample_HelloWorld.car";
	// rest samples
	private static final String SAMPLE_MAPPERSHOP_PROJECT_FILE_NAME = "sampleMapperShop.car";
	// libraries
	private static final String AMAZON_LEX_LIBRARY_PROJECT_FILE_NAME = "lib_AmazonLEX.car";
	private static final String PUSH_MANAGER_LIBRARY_PROJECT_FILE_NAME = "lib_PushManager.car";
	private static final String TWITTER_LIBRARY_PROJECT_FILE_NAME = "lib_Twitter.car";
	private static final String OAUTH_LIBRARY_PROJECT_FILE_NAME = "lib_OAuth.car";
	private static final String FILE_TRANSFER_LIBRARY_PROJECT_FILE_NAME = "lib_FileTransfer.car";
	private static final String FULLSYNC_GRP_LIBRARY_PROJECT_FILE_NAME = "lib_FullSyncGrp.car";
	private static final String SIGFOX_LIBRARY_PROJECT_FILE_NAME = "lib_Sigfox.car";
	private static final String SALESFORCE_LIBRARY_PROJECT_FILE_NAME = "lib_Salesforce.car";
	private static final String USERMANAGER_LIBRARY_PROJECT_FILE_NAME = "lib_UserManager.car";
	// referemnce manual samples
	private static final String REFMANUAL_SITECLIPPER_PROJECT_FILE_NAME = "sample_refManual_siteClipper.car";
	private static final String REFMANUAL_STATEMENTS_PROJECT_FILE_NAME = "sample_refManual_statements.car";
	private static final String REFMANUAL_STEPS_PROJECT_FILE_NAME = "sample_refManual_steps.car";
	private static final String REFMANUAL_VARIABLES_PROJECT_FILE_NAME = "sample_refManual_variables.car";
	private static final String REFMANUAL_HTTP_PROJECT_FILE_NAME = "sample_refManual_http.car";
	private static final String REFMANUAL_WEBCLIPPER_PROJECT_FILE_NAME = "sample_refManual_webClipper.car";
	// SQL samples
	private static final String SQL_HSQLDB_PROJECT_FILE_NAME = "sample_database_HSQLDB.car";
	private static final String SQL_XLS_PROJECT_FILE_NAME = "sample_database_XLS.car";
	// sequencer samples
	private static final String SEQUENCER_1_PROJECT_FILE_NAME = "sample_sequencer.car";

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
		switch (templateId) {
		case SAMPLE_SEQUENCER_1:
		case SAMPLE_SQL_HSQLDB:
		case SAMPLE_SQL_XLS:
		case SAMPLE_REFMANUAL_SITECLIPPER:
		case SAMPLE_REFMANUAL_STATEMENTS:
		case SAMPLE_REFMANUAL_STEPS:
		case SAMPLE_REFMANUAL_VARIABLES:
		case SAMPLE_REFMANUAL_HTTP:
		case SAMPLE_REFMANUAL_WEBCLIPPER:
		case SAMPLE_DOCUMENTATION_CLI:
		case SAMPLE_DOCUMENTATION_CLP:
		case SAMPLE_DOCUMENTATION_CMC:
		case SAMPLE_DOCUMENTATION_CMS:
		case SAMPLE_DOCUMENTATION_CWC:
		case SAMPLE_DOCUMENTATION_CWI:
		case SAMPLE_DOCUMENTATION_SITECLIPPER:
		case SAMPLE_OFFCHAT:
		case SAMPLE_RETAILSTORE:
		case SAMPLE_FLIGHTSHARE:
		case SAMPLE_HELLOWORLD:
		case SAMPLE_CONFERENCEAPP:
		case SAMPLE_MAPPERSHOP:
		case LIBRARY_AMAZON_LEX:
		case LIBRARY_PUSH_MANAGER:
		case LIBRARY_TWITTER:
		case LIBRARY_OAUTH:
		case LIBRARY_FILE_TRANSFER:
		case LIBRARY_FULLSYNC_GRP:
		case LIBRARY_SIGFOX:
		case LIBRARY_SALESFORCE:
		case LIBRARY_USERMANAGER:
			pageSummarySampleProject = new NewProjectWizardPageSummarySampleProject(selection);
			addPage(pageSummarySampleProject);
			break;

		case TEMPLATE_WEB_HTML_BULL_DKU_7107:
		case TEMPLATE_WEB_HTML_IBM_3270:
		case TEMPLATE_WEB_HTML_IBM_5250:
		case TEMPLATE_EAI_IBM_3270:
		case TEMPLATE_EAI_IBM_5250:
		case TEMPLATE_EAI_BULL_DKU_7107:
		case TEMPLATE_EAI_UNIX_VT220:
			page1 = new NewProjectWizardPage1(selection);
			page2 = new NewProjectWizardPage2(selection);
			page7 = new ServiceCodeWizardPage(selection);
			page4 = new NewProjectWizardPage4(selection);
			addPage(page1);
			addPage(page2);
			addPage(page7);
			addPage(page4);
			break;

		case TEMPLATE_EAI_CICS_COMMEAREA:
			page1 = new NewProjectWizardPage1(selection);
			page2 = new NewProjectWizardPage2(selection);
			page5 = new NewProjectWizardPage5(selection);
			page9 = new NewProjectWizardPage9(selection);
			addPage(page1);
			addPage(page2);
			addPage(page5);
			addPage(page9);
			break;

		case TEMPLATE_WEB_SERVICE_REST_REFERENCE:
		case TEMPLATE_EAI_HTML_WEB_SITE:
		case TEMPLATE_EAI_HTTP:
			page1 = new NewProjectWizardPage1(selection);
			page2 = new NewProjectWizardPage2(selection);
			page6 = new NewProjectWizardPage6(selection);
			page8 = new NewProjectWizardPage8(selection);
			addPage(page1);
			addPage(page2);
			addPage(page6);
			addPage(page8);
			break;

		case TEMPLATE_WEB_SERVICE_SWAGGER_REFERENCE:
		case TEMPLATE_WEB_SERVICE_SOAP_REFERENCE:
			page1 = new NewProjectWizardPage1(selection);
			page10 = new NewProjectWizardPage10(selection);
			addPage(page1);
			addPage(page10);
			break;

		case TEMPLATE_SQL_CONNECTOR:
			page1 = new NewProjectWizardPage1(selection);
			page2 = new NewProjectWizardPage2(selection);
			configureSQLConnectorPage = new ConfigureSQLConnectorPage(selection);
			addPage(page1);
			addPage(page2);
			addPage(configureSQLConnectorPage);
			break;
			
		case TEMPLATE_SAP_CONNECTOR:
			page1 = new NewProjectWizardPage1(selection);
			page2 = new NewProjectWizardPage2(selection);
			configureSAPConnectorPage = new ConfigureSAPConnectorPage(selection);
			addPage(page1);
			addPage(page2);
			addPage(configureSAPConnectorPage);
			break;
			
		case TEMPLATE_MOBILE_BUILDER:
		case TEMPLATE_SEQUENCE_CONNECTOR:
			page1 = new NewProjectWizardPage1(selection);
			addPage(page1);
			break;

		case TEMPLATE_SITE_CLIPPER:
			page1 = new NewProjectWizardPage1(selection);
			page2 = new NewProjectWizardPage2(selection);
			page11 = new NewProjectWizardPage11(selection);
			addPage(page1);
			addPage(page2);
			addPage(page11);
			break;
			
		default:
			break;
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

		// refresh the project explorer treeview
		IWorkbenchPart iwbPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.findView("com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView");

		if (iwbPart instanceof ProjectExplorerView) {
			ProjectExplorerView view = (ProjectExplorerView) iwbPart;

			try {
				if (projectName != null) {
					view.importProjectTreeObject(projectName);
				}
			} catch (CoreException e) {
				ConvertigoPlugin.logException(e, "An error occured while refreshing the tree view");
			}
			view.viewer.refresh();
		}

		return true;
	}

	/**
	 * The worker method. We create the project here according to the templateId
	 * variable
	 */
	private void doFinish(IProgressMonitor monitor) throws CoreException {
		try {
			switch (templateId) {
			case TEMPLATE_WEB_GREENSCREEN_IBM_3270:
			case TEMPLATE_WEB_GREENSCREEN_IBM_5250:
			case TEMPLATE_MOBILE_IBM_3270:
			case TEMPLATE_MOBILE_IBM_5250:
				throw new EngineException("Attempt to create new project, with templateId " + templateId
						+ " not available.");

			case TEMPLATE_WEB_HTML_BULL_DKU_7107:
			case TEMPLATE_WEB_HTML_IBM_3270:
			case TEMPLATE_WEB_HTML_IBM_5250:
			case TEMPLATE_EAI_BULL_DKU_7107:
			case TEMPLATE_EAI_IBM_3270:
			case TEMPLATE_EAI_IBM_5250:
			case TEMPLATE_EAI_UNIX_VT220:
			case TEMPLATE_EAI_HTML_WEB_SITE:
			case TEMPLATE_EAI_HTTP:
			case TEMPLATE_EAI_CICS_COMMEAREA:
			case TEMPLATE_SEQUENCE_CONNECTOR:
			case TEMPLATE_SQL_CONNECTOR:
			case TEMPLATE_SAP_CONNECTOR:
			case TEMPLATE_SITE_CLIPPER:
			case TEMPLATE_MOBILE_BUILDER:
			case TEMPLATE_WEB_SERVICE_REST_REFERENCE:
				projectName = page1.getProjectName();
				monitor.beginTask("Creating project " + projectName, 7);
				createFromBlankProject(monitor);
				return;

			case SAMPLE_SEQUENCER_1:
			case SAMPLE_SQL_HSQLDB:
			case SAMPLE_SQL_XLS:
			case SAMPLE_REFMANUAL_SITECLIPPER:
			case SAMPLE_REFMANUAL_STATEMENTS:
			case SAMPLE_REFMANUAL_STEPS:
			case SAMPLE_REFMANUAL_VARIABLES:
			case SAMPLE_REFMANUAL_HTTP:
			case SAMPLE_REFMANUAL_WEBCLIPPER:
			case SAMPLE_DOCUMENTATION_CLI:
			case SAMPLE_DOCUMENTATION_CLP:
			case SAMPLE_DOCUMENTATION_CMC:
			case SAMPLE_DOCUMENTATION_CMS:
			case SAMPLE_DOCUMENTATION_CWC:
			case SAMPLE_DOCUMENTATION_CWI:
			case SAMPLE_DOCUMENTATION_SITECLIPPER:
			case SAMPLE_OFFCHAT:
			case SAMPLE_RETAILSTORE:
			case SAMPLE_CONFERENCEAPP:
			case SAMPLE_FLIGHTSHARE:
			case SAMPLE_HELLOWORLD:
			case SAMPLE_MAPPERSHOP:
			case LIBRARY_AMAZON_LEX:
			case LIBRARY_SALESFORCE:
			case LIBRARY_PUSH_MANAGER:
			case LIBRARY_TWITTER:
			case LIBRARY_OAUTH:
			case LIBRARY_FILE_TRANSFER:
			case LIBRARY_FULLSYNC_GRP:
			case LIBRARY_SIGFOX:
			case LIBRARY_USERMANAGER:
				monitor.beginTask("Creating project", 7);
				createFromArchiveProject(monitor);
				return;

			case TEMPLATE_WEB_SERVICE_SWAGGER_REFERENCE:
			case TEMPLATE_WEB_SERVICE_SOAP_REFERENCE:
				try {
					projectName = page1.getProjectName();
					monitor.beginTask("Creating project " + projectName, 7);
					Project project = createFromBlankProject(monitor);
	
					boolean needAuth = page10.useAuthentication();
					String wsURL = page10.getWsdlURL().toString();
					String login = page10.getLogin();
					String password = page10.getPassword();
					
					WebServiceReference webWsReference = null;
					RestServiceReference restWsReference = null;
					RemoteFileReference reference = null;
					
					if (templateId == TEMPLATE_WEB_SERVICE_SOAP_REFERENCE)
						reference = webWsReference = new WebServiceReference();
					if (templateId == TEMPLATE_WEB_SERVICE_SWAGGER_REFERENCE)
						reference = restWsReference = new RestServiceReference();
					
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
					}
					return;
				} catch (Exception e) {
					// Delete everything
					try {
						Engine.logBeans
								.error("An error occured while creating project, everything will be deleted. Please see Studio logs for more informations.",
										null);
						Engine.theApp.databaseObjectsManager.deleteProject(projectName, false, false);
						projectName = null; // avoid load of project in view
					} catch (Exception ex) {
					}
	
					throw new Exception("Unable to create new project from given WS file.", e);
				}
			default:
				throw new EngineException("Attempt to create new project, with templateId " + templateId
						+ " unknown.");
			}
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "An error occured while creating the project", false);
			
			String message = "An error occured while creating the project (see Error log):\n"+ e.getMessage();
			IStatus status = new Status(Status.ERROR, ConvertigoPlugin.PLUGIN_UNIQUE_ID, message, e);
			throw new CoreException(status);
		}
	}
	
	private Project createFromBlankProject(IProgressMonitor monitor) throws Exception {
		String projectArchivePath = "";
		String newProjectName = projectName;
		String oldProjectName = "";

		switch (templateId) {
		case TEMPLATE_SQL_CONNECTOR:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SQL_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = SQL_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					SQL_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case TEMPLATE_SAP_CONNECTOR:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SAP_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = SAP_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					SAP_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case TEMPLATE_SEQUENCE_CONNECTOR:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SEQUENCE_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = SEQUENCE_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					SEQUENCE_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case TEMPLATE_EAI_HTML_WEB_SITE:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/"
					+ WEB_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = WEB_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					WEB_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case TEMPLATE_EAI_HTTP:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/"
					+ HTTP_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = HTTP_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					HTTP_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case TEMPLATE_WEB_HTML_BULL_DKU_7107:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/"
					+ DKU_PUBLISHER_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = DKU_PUBLISHER_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					DKU_PUBLISHER_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case TEMPLATE_WEB_HTML_IBM_3270:
		case TEMPLATE_WEB_HTML_IBM_5250:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/"
					+ JAVELIN_PUBLISHER_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = JAVELIN_PUBLISHER_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					JAVELIN_PUBLISHER_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case TEMPLATE_EAI_BULL_DKU_7107:
		case TEMPLATE_EAI_IBM_3270:
		case TEMPLATE_EAI_IBM_5250:
		case TEMPLATE_EAI_UNIX_VT220:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/"
					+ JAVELIN_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = JAVELIN_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					JAVELIN_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case TEMPLATE_EAI_CICS_COMMEAREA:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/"
					+ CICS_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = CICS_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					CICS_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case TEMPLATE_WEB_SERVICE_REST_REFERENCE:
		case TEMPLATE_WEB_SERVICE_SWAGGER_REFERENCE:
		case TEMPLATE_WEB_SERVICE_SOAP_REFERENCE:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/"
					+ HTTP_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = HTTP_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					HTTP_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case TEMPLATE_SITE_CLIPPER:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SITE_CLIPPER_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = SITE_CLIPPER_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					SITE_CLIPPER_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case TEMPLATE_MOBILE_BUILDER:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/"
					+ MOBILE_BUILDER_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = MOBILE_BUILDER_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					MOBILE_BUILDER_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		default:
			return null;
		}
		
		monitor.setTaskName("Creating new project");
		monitor.worked(1);
		
		if (Engine.theApp.databaseObjectsManager.existsProject(newProjectName)) {
			throw new EngineException(
					"Unable to create new project ! A project with the same name (\""
							+ newProjectName + "\") already exists.");
		}
		monitor.setTaskName("Loading the projet");
		monitor.worked(1);
		Project newProject = Engine.theApp.databaseObjectsManager.deployProject(projectArchivePath, newProjectName, true);
		monitor.worked(1);

		try {
			// set values of elements to configure on the new project
			String newEmulatorTechnology = "";
			String emulatorTechnologyName = "";
			String newIbmTerminalType = "";
			switch (templateId) {
			case TEMPLATE_SEQUENCE_CONNECTOR:
			case TEMPLATE_EAI_HTML_WEB_SITE:
			case TEMPLATE_EAI_HTTP:
				break;
			case TEMPLATE_WEB_HTML_BULL_DKU_7107:
			case TEMPLATE_EAI_BULL_DKU_7107:
				newEmulatorTechnology = Session.DKU;
				emulatorTechnologyName = "BullDKU7107"; //$NON-NLS-1$
				break;
			case TEMPLATE_WEB_HTML_IBM_3270:
			case TEMPLATE_EAI_IBM_3270:
				newEmulatorTechnology = Session.SNA;
				newIbmTerminalType = "IBM-3279";
				emulatorTechnologyName = "IBM3270"; //$NON-NLS-1$
				break;
			case TEMPLATE_WEB_HTML_IBM_5250:
			case TEMPLATE_EAI_IBM_5250:
				newEmulatorTechnology = Session.AS400;
				newIbmTerminalType = "IBM-3179";
				emulatorTechnologyName = "IBM5250"; //$NON-NLS-1$
				break;
			case TEMPLATE_EAI_UNIX_VT220:
				newEmulatorTechnology = Session.VT;
				emulatorTechnologyName = "UnixVT220"; //$NON-NLS-1$
				break;
			default:
				break;
			}
			
			monitor.setTaskName("Reset project version");
			monitor.worked(1);
			newProject.setVersion("");
			
			monitor.setTaskName("Change connector name");
			monitor.worked(1);
			String oldConnectorName = "unknown";
			String newConnectorName = "NewConnector";
			switch (templateId) {
			case TEMPLATE_MOBILE_BUILDER:
			case TEMPLATE_SEQUENCE_CONNECTOR:
				newConnectorName = "void";
				break;
			default:
				if (page2 != null) {
					newConnectorName = page2.getConnectorName();
				}
				break;
			}

			monitor.setTaskName("Connector renamed");
			monitor.worked(1);
			oldConnectorName = newProject.getDefaultConnector().getName();
			newProject.getDefaultConnector().setName(newConnectorName);
						
			// configure connector properties
			switch (templateId) {
			case TEMPLATE_WEB_HTML_BULL_DKU_7107:
			case TEMPLATE_WEB_HTML_IBM_3270:
			case TEMPLATE_WEB_HTML_IBM_5250:
			case TEMPLATE_EAI_BULL_DKU_7107:
			case TEMPLATE_EAI_IBM_3270:
			case TEMPLATE_EAI_IBM_5250:
			case TEMPLATE_EAI_UNIX_VT220:
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
				break;
			case TEMPLATE_WEB_SERVICE_REST_REFERENCE:
			case TEMPLATE_EAI_HTML_WEB_SITE:
			case TEMPLATE_EAI_HTTP:
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
				break;

			case TEMPLATE_EAI_CICS_COMMEAREA:
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
				break;
				
			case TEMPLATE_SQL_CONNECTOR:
				// change emulator technology
				// and change service code
				SqlConnector sqlConnector = (SqlConnector) newProject.getDefaultConnector();
				
				monitor.setTaskName("Update JDBC URL");
				monitor.worked(1);
				sqlConnector.setJdbcURL(configureSQLConnectorPage.getJdbcURL());
				monitor.setTaskName("Update JDBC driver");
				monitor.worked(1);
				sqlConnector.setJdbcDriverClassName(configureSQLConnectorPage.getJdbcDriver());
				monitor.setTaskName("Update Username");
				monitor.worked(1);
				sqlConnector.setJdbcUserName(configureSQLConnectorPage.getUsername());
				monitor.setTaskName("Update Password");
				monitor.worked(1);
				sqlConnector.setJdbcUserPassword(configureSQLConnectorPage.getPassword());
				
				break;
				
			case TEMPLATE_SAP_CONNECTOR:
				// change emulator technology
				// and change service code
				
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
				break;
				
			case TEMPLATE_SITE_CLIPPER:
				SiteClipperConnector scConnector = (SiteClipperConnector) newProject.getDefaultConnector();
				monitor.setTaskName("Update connector certificates policy");
				monitor.worked(1);
				scConnector.setTrustAllServerCertificates(page11.isTrustAllServerCertificates());

				monitor.setTaskName("Update host url");
				monitor.worked(1);
				scConnector.getDefaultTransaction().setTargetURL(page11.getTargetURL());
				
				break;
				
			default:
				break;
			}
			
			monitor.setTaskName("Saving updated project");
			monitor.worked(1);
			
			Engine.theApp.databaseObjectsManager.exportProject(newProject);

			monitor.setTaskName("New project saved");
			monitor.worked(1);
			
			try {
				String xsdInternalPath = newProject.getDirPath() + "/" + Project.XSD_FOLDER_NAME + "/" + Project.XSD_INTERNAL_FOLDER_NAME;
				File xsdInternalDir = new File(xsdInternalPath).getCanonicalFile();
				if (xsdInternalDir.exists()) {
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
				
			} catch (ConvertigoException e) {
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

	private Project createFromArchiveProject(IProgressMonitor monitor) throws Exception {
		Project project = null;
		String projectArchivePath = "";

		switch (templateId) {
		case SAMPLE_SEQUENCER_1:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SEQUENCER_1_PROJECT_FILE_NAME;
			projectName = SEQUENCER_1_PROJECT_FILE_NAME.substring(0, SEQUENCER_1_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_SQL_HSQLDB:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SQL_HSQLDB_PROJECT_FILE_NAME;
			projectName = SQL_HSQLDB_PROJECT_FILE_NAME.substring(0, SQL_HSQLDB_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_SQL_XLS:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SQL_XLS_PROJECT_FILE_NAME;
			projectName = SQL_XLS_PROJECT_FILE_NAME.substring(0, SQL_XLS_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_REFMANUAL_SITECLIPPER:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + REFMANUAL_SITECLIPPER_PROJECT_FILE_NAME;
			projectName = REFMANUAL_SITECLIPPER_PROJECT_FILE_NAME.substring(0, REFMANUAL_SITECLIPPER_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_REFMANUAL_STATEMENTS:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + REFMANUAL_STATEMENTS_PROJECT_FILE_NAME;
			projectName = REFMANUAL_STATEMENTS_PROJECT_FILE_NAME.substring(0, REFMANUAL_STATEMENTS_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_REFMANUAL_STEPS:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + REFMANUAL_STEPS_PROJECT_FILE_NAME;
			projectName = REFMANUAL_STEPS_PROJECT_FILE_NAME.substring(0, REFMANUAL_STEPS_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_REFMANUAL_VARIABLES:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + REFMANUAL_VARIABLES_PROJECT_FILE_NAME;
			projectName = REFMANUAL_VARIABLES_PROJECT_FILE_NAME.substring(0, REFMANUAL_VARIABLES_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_REFMANUAL_WEBCLIPPER:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + REFMANUAL_WEBCLIPPER_PROJECT_FILE_NAME;
			projectName = REFMANUAL_WEBCLIPPER_PROJECT_FILE_NAME.substring(0, REFMANUAL_WEBCLIPPER_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_REFMANUAL_HTTP:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + REFMANUAL_HTTP_PROJECT_FILE_NAME;
			projectName = REFMANUAL_HTTP_PROJECT_FILE_NAME.substring(0, REFMANUAL_HTTP_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_DOCUMENTATION_CLI:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + CLI_DOC_PROJECT_FILE_NAME;
			projectName = CLI_DOC_PROJECT_FILE_NAME.substring(0, CLI_DOC_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_DOCUMENTATION_CLP:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + CLP_DOC_PROJECT_FILE_NAME;
			projectName = CLP_DOC_PROJECT_FILE_NAME.substring(0, CLP_DOC_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_DOCUMENTATION_CMC:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + CMC_DOC_PROJECT_FILE_NAME;
			projectName = CMC_DOC_PROJECT_FILE_NAME.substring(0, CMC_DOC_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_DOCUMENTATION_CMS:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + CMS_DOC_PROJECT_FILE_NAME;
			projectName = CMS_DOC_PROJECT_FILE_NAME.substring(0, CMS_DOC_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_DOCUMENTATION_CWC:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + CWC_DOC_PROJECT_FILE_NAME;
			projectName = CWC_DOC_PROJECT_FILE_NAME.substring(0, CWC_DOC_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_DOCUMENTATION_CWI:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + CWI_DOC_PROJECT_FILE_NAME;
			projectName = CWI_DOC_PROJECT_FILE_NAME.substring(0, CWI_DOC_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_DOCUMENTATION_SITECLIPPER:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SITECLIPPER_DOC_PROJECT_FILE_NAME;
			projectName = SITECLIPPER_DOC_PROJECT_FILE_NAME.substring(0, SITECLIPPER_DOC_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case LIBRARY_AMAZON_LEX:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + AMAZON_LEX_LIBRARY_PROJECT_FILE_NAME;
			projectName = AMAZON_LEX_LIBRARY_PROJECT_FILE_NAME.substring(0, AMAZON_LEX_LIBRARY_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case LIBRARY_SALESFORCE:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SALESFORCE_LIBRARY_PROJECT_FILE_NAME;
			projectName = SALESFORCE_LIBRARY_PROJECT_FILE_NAME.substring(0, SALESFORCE_LIBRARY_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case LIBRARY_PUSH_MANAGER:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + PUSH_MANAGER_LIBRARY_PROJECT_FILE_NAME;
			projectName = PUSH_MANAGER_LIBRARY_PROJECT_FILE_NAME.substring(0, PUSH_MANAGER_LIBRARY_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case LIBRARY_TWITTER:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + TWITTER_LIBRARY_PROJECT_FILE_NAME;
			projectName = TWITTER_LIBRARY_PROJECT_FILE_NAME.substring(0, TWITTER_LIBRARY_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case LIBRARY_OAUTH:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + OAUTH_LIBRARY_PROJECT_FILE_NAME;
			projectName = OAUTH_LIBRARY_PROJECT_FILE_NAME.substring(0, OAUTH_LIBRARY_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case LIBRARY_FILE_TRANSFER:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + FILE_TRANSFER_LIBRARY_PROJECT_FILE_NAME;
			projectName = FILE_TRANSFER_LIBRARY_PROJECT_FILE_NAME.substring(0, FILE_TRANSFER_LIBRARY_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case LIBRARY_FULLSYNC_GRP:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + FULLSYNC_GRP_LIBRARY_PROJECT_FILE_NAME;
			projectName = FULLSYNC_GRP_LIBRARY_PROJECT_FILE_NAME.substring(0, FULLSYNC_GRP_LIBRARY_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case LIBRARY_SIGFOX:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SIGFOX_LIBRARY_PROJECT_FILE_NAME;
			projectName = SIGFOX_LIBRARY_PROJECT_FILE_NAME.substring(0, SIGFOX_LIBRARY_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case LIBRARY_USERMANAGER:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + USERMANAGER_LIBRARY_PROJECT_FILE_NAME;
			projectName = USERMANAGER_LIBRARY_PROJECT_FILE_NAME.substring(0, USERMANAGER_LIBRARY_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_OFFCHAT:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SAMPLE_OFFCHAT_PROJECT_FILE_NAME;
			projectName = SAMPLE_OFFCHAT_PROJECT_FILE_NAME.substring(0, SAMPLE_OFFCHAT_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_RETAILSTORE:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SAMPLE_RETAILSTORE_PROJECT_FILE_NAME;
			projectName = SAMPLE_RETAILSTORE_PROJECT_FILE_NAME.substring(0, SAMPLE_RETAILSTORE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_CONFERENCEAPP:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SAMPLE_CONFERENCEAPP_PROJECT_FILE_NAME;
			projectName = SAMPLE_CONFERENCEAPP_PROJECT_FILE_NAME.substring(0, SAMPLE_CONFERENCEAPP_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_FLIGHTSHARE:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SAMPLE_FLIGHTSHARE_PROJECT_FILE_NAME;
			projectName = SAMPLE_FLIGHTSHARE_PROJECT_FILE_NAME.substring(0, SAMPLE_FLIGHTSHARE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_HELLOWORLD:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SAMPLE_HELLOWORLD_PROJECT_FILE_NAME;
			projectName = SAMPLE_HELLOWORLD_PROJECT_FILE_NAME.substring(0, SAMPLE_HELLOWORLD_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_MAPPERSHOP:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SAMPLE_MAPPERSHOP_PROJECT_FILE_NAME;
			projectName = SAMPLE_MAPPERSHOP_PROJECT_FILE_NAME.substring(0, SAMPLE_MAPPERSHOP_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		default:
			return null;
		}

		monitor.setTaskName("Creating new project");
		monitor.worked(1);

		if (Engine.theApp.databaseObjectsManager.existsProject(projectName)) {
			String message = "Unable to create new project ! A project with the same name (\"" + projectName
					+ "\") already exists.";
			projectName = null; // avoid load of project in view
			throw new EngineException(message);
		}

		try {
			try {
				project = Engine.theApp.databaseObjectsManager.deployProject(projectArchivePath, true);
			} catch (Exception e) {
				// Catch exception because part of project could have been
				// deployed
				// User will be able to remove it from the Studio
				ConvertigoPlugin.logException(e, "Unable to import project '" + projectName + "'!");
			}

			monitor.setTaskName("Project loaded");
			monitor.worked(1);

			monitor.setTaskName("Resources created");
			monitor.worked(1);
		} catch (Exception e) {
			// Delete everything
			try {
				Engine.logBeans
						.error("An error occured while creating project, everything will be deleted. Please see Studio logs for more informations.",
								null);
				Engine.theApp.databaseObjectsManager.deleteProject(projectName, false, false);
				projectName = null; // avoid load of project in view
				project = null;
			} catch (Exception ex) {
			}

			throw new Exception("Unable to create project from template", e);
		}

		return project;
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

}