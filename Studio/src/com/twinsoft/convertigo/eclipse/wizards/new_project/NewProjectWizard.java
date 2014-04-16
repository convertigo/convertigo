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

package com.twinsoft.convertigo.eclipse.wizards.new_project;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.twinsoft.api.Session;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.ImportWsReference;
import com.twinsoft.convertigo.engine.util.ProjectUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.ZipUtils;

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
	public static final int TEMPLATE_MISC_EMPTY_PROJECT = 0;
	public static final int TEMPLATE_WEB_HTML_IBM_3270 = 100;
	public static final int TEMPLATE_WEB_HTML_IBM_5250 = 101;
	public static final int TEMPLATE_WEB_HTML_BULL_DKU_7107 = 102;
	public static final int TEMPLATE_WEB_GREENSCREEN_IBM_3270 = 150;
	public static final int TEMPLATE_WEB_GREENSCREEN_IBM_5250 = 151;
	public static final int TEMPLATE_MOBILE_IBM_3270 = 200;
	public static final int TEMPLATE_MOBILE_IBM_5250 = 201;
	public static final int TEMPLATE_EAI_IBM_3270 = 300;
	public static final int TEMPLATE_EAI_IBM_5250 = 301;
	public static final int TEMPLATE_EAI_BULL_DKU_7107 = 302;
	public static final int TEMPLATE_EAI_MINITEL = 303;
	public static final int TEMPLATE_EAI_UNIX_VT220 = 304;
	public static final int TEMPLATE_EAI_HTTP = 305;
	public static final int TEMPLATE_EAI_CICS_COMMEAREA = 306;
	public static final int TEMPLATE_EAI_HTML_WEB_SITE = 307;
	public static final int TEMPLATE_WEB_CLIPPING_CONNECTOR = 400;
	public static final int TEMPLATE_SEQUENCE_CONNECTOR = 500;
	public static final int TEMPLATE_WEB_SERVICE_REFERENCE = 700;
	public static final int TEMPLATE_SQL_CONNECTOR = 701;
	// generic sample
	public static final int SAMPLE_HELLO_WORLD = 620;
	// documentation samples
	public static final int SAMPLE_DOCUMENTATION_CLI = 600;
	public static final int SAMPLE_DOCUMENTATION_CLP = 601;
	public static final int SAMPLE_DOCUMENTATION_CWI = 602;
	public static final int SAMPLE_DOCUMENTATION_CWC = 603;
	public static final int SAMPLE_DOCUMENTATION_CMS = 604;
	public static final int SAMPLE_DOCUMENTATION_CMC = 605;
	public static final int SAMPLE_DOCUMENTATION_SITECLIPPER = 607;
	// reference manual samples
	public static final int SAMPLE_REFMANUAL_SITECLIPPER = 650;
	public static final int SAMPLE_REFMANUAL_STATEMENTS = 651;
	public static final int SAMPLE_REFMANUAL_STEPS = 652;
	public static final int SAMPLE_REFMANUAL_VARIABLES = 653;
	public static final int SAMPLE_REFMANUAL_HTTP = 654;
	public static final int SAMPLE_REFMANUAL_WEBCLIPPER = 655;
	// SQL samples
	public static final int SAMPLE_SQL_HSQLDB = 660;
	public static final int SAMPLE_SQL_XLS = 661;
	// libraries
	public static final int LIBRARY_GOOGLE_MAPS = 800;
	public static final int LIBRARY_GOOGLE_SPREADSHEETS = 801;
	public static final int LIBRARY_GOOGLE_DOCS = 802;
	public static final int LIBRARY_KEYRING = 803;
	public static final int DEMOS_SALESFORCE = 900;
	public static final int DEMOS_USDIRECTORY = 901;
	public static final int DEMOS_LEGACYCRM = 902;
	public static final int DEMOS_MASHUP = 903;
	public static final int TEMPLATE_INTERACTION_HUB = 1000;
	public static final int TEMPLATE_SITE_CLIPPER = 1100;
	public static final int TEMPLATE_MOBILE_EMPTY_JQUERYMOBILE = 1300;
	// mobile samples
	public static final int SAMPLE_MOBILE_VACATION = 1310;
	public static final int SAMPLE_MOBILE_SIMPLE_APP = 1311;
	public static final int SAMPLE_MOBILE_CTF_GALLERY = 1312;
	public static final int SAMPLE_MOBILE_USDIRECTORY = 1313;
	public static final int SAMPLE_MOBILE_MOBTV = 1314;
	public static final int SAMPLE_MOBILE_C8O_WEB_MOBILE = 1315;
	// sequencer samples
	public static final int SAMPLE_SEQUENCER_1 = 1400;

	// FILE NAMES
	// to import blank project
	public static final String JAVELIN_PUBLISHER_TEMPLATE_PROJECT_FILE_NAME = "template_javelin.car";
	public static final String DKU_PUBLISHER_TEMPLATE_PROJECT_FILE_NAME = "template_javelinDKU.car";
	public static final String WEB_CLIPPING_TEMPLATE_PROJECT_FILE_NAME = "template_webClipping.car";
	public static final String JAVELIN_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME = "template_javelinIntegration.car";
	public static final String WEB_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME = "template_webIntegration.car";
	public static final String HTTP_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME = "template_HTTP.car";
	public static final String CICS_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME = "template_CICS.car";
	public static final String SEQUENCE_TEMPLATE_PROJECT_FILE_NAME = "template_sequence.car";
	public static final String SQL_TEMPLATE_PROJECT_FILE_NAME = "template_SQL.car";
	public static final String INTERACTION_HUB_TEMPLATE_PROJECT_FILE_NAME = "template_interactionHub.car";
	public static final String SITE_CLIPPER_TEMPLATE_PROJECT_FILE_NAME = "template_siteClipper.car";
	public static final String JQUERYMOBILE_MOBILE_EMPTY_TEMPLATE_PROJECT_FILE_NAME = "template_mobileJQueryMobile.car";
	// documentation samples
	public static final String CLI_DOC_PROJECT_FILE_NAME = "sample_documentation_CLI.car";
	public static final String CWI_DOC_PROJECT_FILE_NAME = "sample_documentation_CWI.car";
	public static final String CLP_DOC_PROJECT_FILE_NAME = "sample_documentation_CLP.car";
	public static final String CWC_DOC_PROJECT_FILE_NAME = "sample_documentation_CWC.car";
	public static final String CMS_DOC_PROJECT_FILE_NAME = "sample_documentation_CMS.car";
	public static final String CMC_DOC_PROJECT_FILE_NAME = "sample_documentation_CMC.car";
	public static final String SITECLIPPER_DOC_PROJECT_FILE_NAME = "sampleDocumentationSiteClipper.car";
	// mobile samples
	public static final String SAMPLE_MOBILE_VACATION_PROJECT_FILE_NAME = "sampleMobileVacation.car";
	public static final String SAMPLE_MOBILE_SIMPLE_APP_PROJECT_FILE_NAME = "sampleMobileJQuery.car";
	public static final String SAMPLE_MOBILE_CTF_GALLERY_PROJECT_FILE_NAME = "sampleMobileCtfGallery.car";
	public static final String SAMPLE_MOBILE_USDIRECTORY_PROJECT_FILE_NAME = "sampleMobileUsDirectoryDemo.car";
	public static final String SAMPLE_MOBILE_MOB_TV_PROJECT_FILE_NAME = "sampleMobileMobTV.car";
	public static final String SAMPLE_MOBILE_C8O_WEB_MOBILE_PROJECT_FILE_NAME = "sampleMobileConvertigoWebMobile.car";
	// libraries
	public static final String GOOGLE_MAPS_LIBRARY_PROJECT_FILE_NAME = "lib_GoogleMaps.car";
	public static final String GOOGLE_SPREADSHEETS_LIBRARY_PROJECT_FILE_NAME = "lib_GoogleSpreadsheets.car";
	public static final String GOOGLE_DOCS_LIBRARY_PROJECT_FILE_NAME = "lib_GoogleDocs.car";
	public static final String KEYRING_LIBRARY_PROJECT_FILE_NAME = "lib_Keyring.car";
	// mashup demo
	public static final String SALESFORCE_DEMO_PROJECT_FILE_NAME = "demo_SalesForce.car";
	public static final String USDIRECTORY_DEMO_PROJECT_FILE_NAME = "demo_usDirectory.car";
	public static final String LEGACYCRM_DEMO_PROJECT_FILE_NAME = "demo_legacyCRM.car";
	public static final String MASHUP_DEMO_PROJECT_FILE_NAME = "demo_mashup.car";
	// referemnce manual samples
	public static final String REFMANUAL_SITECLIPPER_PROJECT_FILE_NAME = "sample_refManual_siteClipper.car";
	public static final String REFMANUAL_STATEMENTS_PROJECT_FILE_NAME = "sample_refManual_statements.car";
	public static final String REFMANUAL_STEPS_PROJECT_FILE_NAME = "sample_refManual_steps.car";
	public static final String REFMANUAL_VARIABLES_PROJECT_FILE_NAME = "sample_refManual_variables.car";
	public static final String REFMANUAL_HTTP_PROJECT_FILE_NAME = "sample_refManual_http.car";
	public static final String REFMANUAL_WEBCLIPPER_PROJECT_FILE_NAME = "sample_refManual_webClipper.car";
	// SQL samples
	public static final String SQL_HSQLDB_PROJECT_FILE_NAME = "sample_database_HSQLDB.car";
	public static final String SQL_XLS_PROJECT_FILE_NAME = "sample_database_XLS.car";
	// generic sample
	public static final String SAMPLE_HELLO_WORLD_PROJECT_FILE_NAME = "sample_HelloWorld.car";
	// sequencer samples
	public static final String SEQUENCER_1_PROJECT_FILE_NAME = "sample_sequencer.car";

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
		case SAMPLE_HELLO_WORLD:
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
		case SAMPLE_MOBILE_VACATION:
		case SAMPLE_MOBILE_SIMPLE_APP:
		case SAMPLE_MOBILE_CTF_GALLERY:
		case SAMPLE_MOBILE_USDIRECTORY:
		case SAMPLE_MOBILE_MOBTV:
		case SAMPLE_MOBILE_C8O_WEB_MOBILE:
		case LIBRARY_GOOGLE_MAPS:
		case LIBRARY_GOOGLE_SPREADSHEETS:
		case LIBRARY_GOOGLE_DOCS:
		case LIBRARY_KEYRING:
		case DEMOS_SALESFORCE:
		case DEMOS_USDIRECTORY:
		case DEMOS_LEGACYCRM:
		case DEMOS_MASHUP:
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
		case TEMPLATE_EAI_MINITEL:
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

		case TEMPLATE_EAI_HTML_WEB_SITE:
		case TEMPLATE_WEB_CLIPPING_CONNECTOR:
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

		case TEMPLATE_WEB_SERVICE_REFERENCE:
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

		case TEMPLATE_MOBILE_EMPTY_JQUERYMOBILE:
		case TEMPLATE_SEQUENCE_CONNECTOR:
		case TEMPLATE_INTERACTION_HUB:
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
			case TEMPLATE_WEB_CLIPPING_CONNECTOR:
			case TEMPLATE_EAI_BULL_DKU_7107:
			case TEMPLATE_EAI_IBM_3270:
			case TEMPLATE_EAI_IBM_5250:
			case TEMPLATE_EAI_MINITEL:
			case TEMPLATE_EAI_UNIX_VT220:
			case TEMPLATE_EAI_HTML_WEB_SITE:
			case TEMPLATE_EAI_HTTP:
			case TEMPLATE_EAI_CICS_COMMEAREA:
			case TEMPLATE_SEQUENCE_CONNECTOR:
			case TEMPLATE_SQL_CONNECTOR:
			case TEMPLATE_INTERACTION_HUB:
			case TEMPLATE_SITE_CLIPPER:
			case TEMPLATE_MOBILE_EMPTY_JQUERYMOBILE:
				projectName = page1.getProjectName();
				monitor.beginTask("Creating project " + projectName, 7);
				createFromBlankProject(monitor);
				return;

			case SAMPLE_HELLO_WORLD:
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
			case SAMPLE_MOBILE_VACATION:
			case SAMPLE_MOBILE_SIMPLE_APP:
			case SAMPLE_MOBILE_CTF_GALLERY:
			case SAMPLE_MOBILE_USDIRECTORY:
			case SAMPLE_MOBILE_MOBTV:
			case SAMPLE_MOBILE_C8O_WEB_MOBILE:
			case LIBRARY_GOOGLE_MAPS:
			case LIBRARY_GOOGLE_SPREADSHEETS:
			case LIBRARY_GOOGLE_DOCS:
			case LIBRARY_KEYRING:
			case DEMOS_SALESFORCE:
			case DEMOS_USDIRECTORY:
			case DEMOS_LEGACYCRM:
			case DEMOS_MASHUP:
				monitor.beginTask("Creating project", 7);
				createFromArchiveProject(monitor);
				return;

			case TEMPLATE_WEB_SERVICE_REFERENCE:
				projectName = page1.getProjectName();
				monitor.beginTask("Creating project " + projectName, 7);
				Project project = createFromBlankProject(monitor);

				String wsdlURL = page10.getWsdlURL();
				ImportWsReference wsr = new ImportWsReference(wsdlURL);
				
				HttpConnector httpConnector = null;
				if (!page10.useAuthentication()) {
					httpConnector = wsr.importInto(project);
				} else {
					httpConnector = wsr.importIntoAuthenticated(project, page10.getLogin(), 
							page10.getPassword());
				}
				
				if (httpConnector != null) {
					Connector defaultConnector = project.getDefaultConnector();
					project.setDefaultConnector(httpConnector);
					defaultConnector.delete();
				}
				return;
				
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
		Project project = null;
		String projectArchivePath = "";
		String newProjectName = projectName;
		String oldProjectName = "";

		switch (templateId) {
		case TEMPLATE_SQL_CONNECTOR:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SQL_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = SQL_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					SQL_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case TEMPLATE_SEQUENCE_CONNECTOR:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SEQUENCE_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = SEQUENCE_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					SEQUENCE_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case TEMPLATE_WEB_CLIPPING_CONNECTOR:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + WEB_CLIPPING_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = WEB_CLIPPING_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					WEB_CLIPPING_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
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
		case TEMPLATE_EAI_MINITEL:
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
		case TEMPLATE_WEB_SERVICE_REFERENCE:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/"
					+ HTTP_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = HTTP_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					HTTP_INTEGRATION_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case TEMPLATE_INTERACTION_HUB:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/"
					+ INTERACTION_HUB_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = INTERACTION_HUB_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					INTERACTION_HUB_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case TEMPLATE_SITE_CLIPPER:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SITE_CLIPPER_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = SITE_CLIPPER_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					SITE_CLIPPER_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case TEMPLATE_MOBILE_EMPTY_JQUERYMOBILE:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/"
					+ JQUERYMOBILE_MOBILE_EMPTY_TEMPLATE_PROJECT_FILE_NAME;
			oldProjectName = JQUERYMOBILE_MOBILE_EMPTY_TEMPLATE_PROJECT_FILE_NAME.substring(0,
					JQUERYMOBILE_MOBILE_EMPTY_TEMPLATE_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		default:
			return null;
		}

		String temporaryDir = new File(Engine.USER_WORKSPACE_PATH + "/temp").getCanonicalPath();
		String tempProjectDir = temporaryDir + "/" + oldProjectName;
		String newProjectDir = Engine.PROJECTS_PATH + "/" + newProjectName;

		try {
			try {
				File f = null;

				monitor.setTaskName("Creating new project");
				monitor.worked(1);
				try {
					// Check if project already exists
					if (Engine.theApp.databaseObjectsManager.existsProject(newProjectName))
						throw new EngineException(
								"Unable to create new project ! A project with the same name (\""
										+ newProjectName + "\") already exists.");

					// Create temporary directory if needed
					f = new File(temporaryDir);
					if (f.mkdir()) {
						monitor.setTaskName("Temporary directory created: " + temporaryDir);
						monitor.worked(1);
					}
				} catch (Exception e) {
					throw new EngineException("Unable to create the temporary directory \"" + temporaryDir
							+ "\".", e);
				}

				// Decompress Convertigo archive to temporary directory
				ZipUtils.expandZip(projectArchivePath, temporaryDir, oldProjectName);

				monitor.setTaskName("Project archive expanded to temporary directory");
				monitor.worked(1);

				// Rename temporary project directory
				f = new File(tempProjectDir);
				if (!f.renameTo(new File(newProjectDir))) {
					throw new ConvertigoException("Unable to rename the directory path \"" + tempProjectDir
							+ "\" to \"" + newProjectDir + "\"."
							+ "\n This directory already exists or is probably locked by another application.");
				}
			} catch (Exception e) {
				throw new EngineException("Unable to deploy the project from the file \"" + projectArchivePath
						+ "\".", e);
			}

			String xmlFilePath = newProjectDir + "/" + oldProjectName + ".xml";
			String newXmlFilePath = newProjectDir + "/" + newProjectName + ".xml";
			File xmlFile = new File(xmlFilePath);

			// load xml content of file in dom
			Document dom = null;

			// création du docBuilderFactory
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			docBuilderFactory.setIgnoringElementContentWhitespace(true);
			docBuilderFactory.setNamespaceAware(true);

			// création du docBuilder
			DocumentBuilder docBuilder;
			try {
				docBuilder = docBuilderFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				throw new EngineException("Wrong parser configuration.", e);
			}

			// parsing du fichier
			try {
				dom = docBuilder.parse(xmlFile);
			} catch (SAXException e) {
				throw new EngineException("Wrong XML file structure.", e);
			} catch (IOException e) {
				throw new EngineException("Could not read source file.", e);
			}

			monitor.setTaskName("Xml file parsed");
			monitor.worked(1);

			// set values of elements to configure on the new project
			String newEmulatorTechnology = "";
			String emulatorTechnologyName = "";
			String newIbmTerminalType = "";
			switch (templateId) {
			case TEMPLATE_SEQUENCE_CONNECTOR:
			case TEMPLATE_INTERACTION_HUB:
			case TEMPLATE_WEB_CLIPPING_CONNECTOR:
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
			case TEMPLATE_EAI_MINITEL:
				newEmulatorTechnology = Session.VDX;
				emulatorTechnologyName = "Minitel"; //$NON-NLS-1$
				break;
			case TEMPLATE_EAI_UNIX_VT220:
				newEmulatorTechnology = Session.VT;
				emulatorTechnologyName = "UnixVT220"; //$NON-NLS-1$
				break;
			default:
				break;
			}

			// rename project in .xml file for all projects
			Element projectElem = (Element) dom.getDocumentElement().getElementsByTagName("project").item(0);
			NodeList projectProperties = projectElem.getElementsByTagName("property");

			Element property = (Element) XMLUtils
					.findNodeByAttributeValue(projectProperties, "name", "name");
			((Element) property.getElementsByTagName("java.lang.String").item(0)).removeAttribute("value");
			((Element) property.getElementsByTagName("java.lang.String").item(0)).setAttribute("value",
					newProjectName);
			monitor.setTaskName("Project renamed");
			monitor.worked(1);

			// empty project version
			property = (Element) XMLUtils.findNodeByAttributeValue(projectProperties, "name", "version");
			((Element) property.getElementsByTagName("java.lang.String").item(0)).removeAttribute("value");
			((Element) property.getElementsByTagName("java.lang.String").item(0)).setAttribute("value","");
			
			// rename connector in .xml file for all projects
			String oldConnectorName = "unknown";
			String newConnectorName = "NewConnector";
			// interactionHub project connector name is by default set to "void"
			switch (templateId) {
			case TEMPLATE_MOBILE_EMPTY_JQUERYMOBILE:
			case TEMPLATE_SEQUENCE_CONNECTOR:
			case TEMPLATE_INTERACTION_HUB:
				newConnectorName = "void";
				break;
			default:
				newConnectorName = (page2 == null) ? "NewConnector" : page2.getConnectorName();
				break;
			}
			Element connectorElem = (Element) dom.getDocumentElement().getElementsByTagName("connector")
					.item(0);
			NodeList connectorProperties = connectorElem.getElementsByTagName("property");

			property = (Element) XMLUtils.findNodeByAttributeValue(
					connectorProperties, "name", "name");
			oldConnectorName = ((Element) property.getElementsByTagName("java.lang.String").item(0))
					.getAttribute("value");
			((Element) property.getElementsByTagName("java.lang.String").item(0)).removeAttribute("value");
			((Element) property.getElementsByTagName("java.lang.String").item(0)).setAttribute("value",
					newConnectorName);
			monitor.setTaskName("Connector renamed");
			monitor.worked(1);
			
			// configure connector properties
			switch (templateId) {
			case TEMPLATE_WEB_HTML_BULL_DKU_7107:
			case TEMPLATE_WEB_HTML_IBM_3270:
			case TEMPLATE_WEB_HTML_IBM_5250:
			case TEMPLATE_EAI_BULL_DKU_7107:
			case TEMPLATE_EAI_IBM_3270:
			case TEMPLATE_EAI_IBM_5250:
			case TEMPLATE_EAI_MINITEL:
			case TEMPLATE_EAI_UNIX_VT220:
				// change emulator technology
				// and change service code
				property = (Element) XMLUtils.findNodeByAttributeValue(
						connectorProperties, "name", "serviceCode");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).removeAttribute("value");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).setAttribute("value",
						page7.getServiceCode());
				monitor.setTaskName("Connector service code updated");
				monitor.worked(1);

				property = (Element) XMLUtils.findNodeByAttributeValue(
						connectorProperties, "name", "emulatorTechnology");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).removeAttribute("value");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).setAttribute("value",
						newEmulatorTechnology);
				monitor.setTaskName("Connector emulator technology updated");
				monitor.worked(1);

				property = (Element) XMLUtils.findNodeByAttributeValue(
						connectorProperties, "name", "ibmTerminalType");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).removeAttribute("value");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).setAttribute("value",
						newIbmTerminalType);
				monitor.setTaskName("Terminal type updated");
				monitor.worked(1);

				// rename emulatorTechnology criteria
				Element criteriaElem = (Element) dom.getDocumentElement().getElementsByTagName("criteria")
						.item(0);
				NodeList criteriaProperties = criteriaElem.getElementsByTagName("property");

				property = (Element) XMLUtils.findNodeByAttributeValue(
						criteriaProperties, "name", "name");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).removeAttribute("value");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).setAttribute("value",
						emulatorTechnologyName);
				monitor.setTaskName("Emulator technology criteria renamed");
				monitor.worked(1);
				break;

			case TEMPLATE_WEB_CLIPPING_CONNECTOR:
			case TEMPLATE_EAI_HTML_WEB_SITE:
			case TEMPLATE_EAI_HTTP:
				// change connector server and port,
				// change https mode
				// and change proxy server and proxy port
				property = (Element) XMLUtils.findNodeByAttributeValue(
						connectorProperties, "name", "server");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).removeAttribute("value");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).setAttribute("value",
						page6.getHttpServer());
				monitor.setTaskName("Connector server updated");
				monitor.worked(1);

				property = (Element) XMLUtils.findNodeByAttributeValue(
						connectorProperties, "name", "port");
				((Element) property.getElementsByTagName("java.lang.Integer").item(0))
						.removeAttribute("value");
				((Element) property.getElementsByTagName("java.lang.Integer").item(0)).setAttribute("value",
						page6.getHttpPort());
				monitor.setTaskName("Connector port updated");
				monitor.worked(1);

				property = (Element) XMLUtils.findNodeByAttributeValue(
						connectorProperties, "name", "https");
				((Element) property.getElementsByTagName("java.lang.Boolean").item(0))
						.removeAttribute("value");
				((Element) property.getElementsByTagName("java.lang.Boolean").item(0)).setAttribute("value", 
						Boolean.toString(page6.isBSSL()));
				monitor.setTaskName("Connector https mode updated");
				monitor.worked(1);
				break;

			case TEMPLATE_EAI_CICS_COMMEAREA:
				property = (Element) XMLUtils.findNodeByAttributeValue(
						connectorProperties, "name", "mainframeName");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).removeAttribute("value");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).setAttribute("value",
						page5.getCtgName());
				monitor.setTaskName("Connector mainframe name updated");
				monitor.worked(1);

				property = (Element) XMLUtils.findNodeByAttributeValue(
						connectorProperties, "name", "server");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).removeAttribute("value");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).setAttribute("value",
						page5.getCtgServer());
				monitor.setTaskName("Connector server updated");
				monitor.worked(1);

				property = (Element) XMLUtils.findNodeByAttributeValue(
						connectorProperties, "name", "port");
				((Element) property.getElementsByTagName("java.lang.Integer").item(0))
						.removeAttribute("value");
				((Element) property.getElementsByTagName("java.lang.Integer").item(0)).setAttribute("value",
						page5.getCtgPort());
				monitor.setTaskName("Connector port updated");
				monitor.worked(1);
				break;
				
			case TEMPLATE_SQL_CONNECTOR:
				// change emulator technology
				// and change service code
				property = (Element) XMLUtils.findNodeByAttributeValue(
						connectorProperties, "name", "jdbcURL");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).setAttribute("value",
						configureSQLConnectorPage.getJdbcURL());
				monitor.setTaskName("JDBC URL updated");
				monitor.worked(1);

				property = (Element) XMLUtils.findNodeByAttributeValue(
						connectorProperties, "name", "jdbcDriverClassName");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).setAttribute("value",
						configureSQLConnectorPage.getJdbcDriver());
				monitor.setTaskName("JDBC driver updated");
				monitor.worked(1);

				property = (Element) XMLUtils.findNodeByAttributeValue(
						connectorProperties, "name", "jdbcUserName");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).setAttribute("value",
						configureSQLConnectorPage.getUsername());
				monitor.setTaskName("Username updated");
				monitor.worked(1);

				property = (Element) XMLUtils.findNodeByAttributeValue(
						connectorProperties, "name", "jdbcUserPassword");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).setAttribute("value",
						configureSQLConnectorPage.getPassword());
				monitor.setTaskName("Password updated");
				monitor.worked(1);
							
				break;
				
			case TEMPLATE_SITE_CLIPPER:
				property = (Element) XMLUtils.findNodeByAttributeValue(
						connectorProperties, "name", "trustAllServerCertificates");
				((Element) property.getElementsByTagName("java.lang.Boolean").item(0))
						.removeAttribute("value");
				((Element) property.getElementsByTagName("java.lang.Boolean").item(0)).setAttribute("value",
						Boolean.toString(page11.isTrustAllServerCertificates()));
				monitor.setTaskName("Connector certificates policy updated");
				monitor.worked(1);
				break;
				
			default:
				break;
			}

			// Configure connector's default transaction properties
			Element transactionElem = (Element) dom.getDocumentElement().getElementsByTagName("transaction")
					.item(0);
			NodeList transactionProperties = transactionElem.getElementsByTagName("property");
			
			switch (templateId) {
			case TEMPLATE_SITE_CLIPPER:
				property = (Element) XMLUtils.findNodeByAttributeValue(
						transactionProperties, "name", "targetURL");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).removeAttribute("value");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).setAttribute("value",
						page11.getTargetURL());
				monitor.setTaskName("Host url updated");
				monitor.worked(1);
				break;
			case TEMPLATE_SQL_CONNECTOR:
				property = (Element) XMLUtils.findNodeByAttributeValue(
						transactionProperties, "name", "sqlQuery");
				((Element) property.getElementsByTagName("java.lang.String").item(0)).removeAttribute("value");
				monitor.setTaskName("SQL queries updated");
				monitor.worked(1);
				break;
			default:
				break;
			}

			// write the new .xml file
			// prepare the string source to write
			String doc = XMLUtils.prettyPrintDOM(dom);
			// create the output file
			File newXmlFile = new File(newXmlFilePath);
			if (!newXmlFile.createNewFile()) {
				throw new ConvertigoException("Unable to create the .xml file \"" + newProjectName + ".xml\".");
			}
			// write the file to the disk
			byte data[] = doc.getBytes();
			FileOutputStream fos = new FileOutputStream(newXmlFilePath);
			BufferedOutputStream dest = new BufferedOutputStream(fos, data.length);
			try {
				dest.write(data, 0, data.length);
			} finally {
				dest.flush();
				dest.close();
			}

			monitor.setTaskName("New xml file created and saved");
			monitor.worked(1);

			// delete the old .xml file
			if (!xmlFile.delete()) {
				throw new ConvertigoException("Unable to delete the .xml file \"" + oldProjectName + ".xml\".");
			}

			monitor.setTaskName("Old xml file deleted");
			monitor.worked(1);
			
			//TODO: Remove this code when template projects will be migrated
			String projectPath = newProjectDir + "/" + newProjectName;
			try {
				// Update XSD file
				try {
					ProjectUtils.renameXsdFile(Engine.PROJECTS_PATH, oldProjectName, newProjectName);
					ProjectUtils.renameConnector(projectPath + ".xsd", oldConnectorName,
							newConnectorName);
	
					monitor.setTaskName("Project XSD file updated");
					monitor.worked(1);
				} catch (Exception e) {
					throw new ConvertigoException("Unable update the .xsd file \"" + oldProjectName + ".xsd\".", e);
				}
	
				// Update WSDL file
				try {
					ProjectUtils.renameWsdlFile(Engine.PROJECTS_PATH, oldProjectName, newProjectName);
					ProjectUtils.renameConnector(projectPath + ".wsdl", oldConnectorName,
							newConnectorName);

					monitor.setTaskName("Project WSDL file updated");
					monitor.worked(1);
				} catch (Exception e) {
					throw new ConvertigoException("Unable update the .wsdl file \"" + oldProjectName + ".wsdl\".",
							e);
				}
			} catch (ConvertigoException e) { }
			// End TODO
			
			// Import the project from the new .xml file
			project = Engine.theApp.databaseObjectsManager.importProject(projectPath + ".xml");
			
			// In the case we want to predefine with the new project wizard a SQL query
			switch(templateId) {
				case TEMPLATE_SQL_CONNECTOR :
					try {																	
						SqlConnector connector = (SqlConnector) project
								.getDefaultConnector();
						SqlTransaction transaction = connector
								.getDefaultTransaction();

						String sqlQuery = transaction.getSqlQuery();
						transaction.setSqlQuery(sqlQuery);
						CarUtils.exportProject(project, projectPath + ".xml");
					} catch (Exception e) {
						Engine.logDatabaseObjectManager.error("An error occured while initialize SQL project \""+projectName+"\"",e);
					}
				break;
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
				// TODO : see if we can delete oldProjectName : a real project
				// could exist with this oldProjectName ?
				// Engine.theApp.databaseObjectsManager.deleteProject(oldProjectName,
				// false, false);
				Engine.theApp.databaseObjectsManager.deleteProject(newProjectName, false, false);
				projectName = null; // avoid load of project in view
				project = null;
			} catch (Exception ex) {
			}

			throw new Exception("Unable to create project from template", e);
		}

		return project;
	}

	private Project createFromArchiveProject(IProgressMonitor monitor) throws Exception {
		Project project = null;
		String projectArchivePath = "";

		switch (templateId) {
		case SAMPLE_HELLO_WORLD:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SAMPLE_HELLO_WORLD_PROJECT_FILE_NAME;
			projectName = SAMPLE_HELLO_WORLD_PROJECT_FILE_NAME.substring(0, SAMPLE_HELLO_WORLD_PROJECT_FILE_NAME.indexOf(".car"));
			break;
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
		case LIBRARY_GOOGLE_MAPS:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + GOOGLE_MAPS_LIBRARY_PROJECT_FILE_NAME;
			projectName = GOOGLE_MAPS_LIBRARY_PROJECT_FILE_NAME.substring(0, GOOGLE_MAPS_LIBRARY_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case LIBRARY_GOOGLE_SPREADSHEETS:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + GOOGLE_SPREADSHEETS_LIBRARY_PROJECT_FILE_NAME;
			projectName = GOOGLE_SPREADSHEETS_LIBRARY_PROJECT_FILE_NAME.substring(0, GOOGLE_SPREADSHEETS_LIBRARY_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case LIBRARY_GOOGLE_DOCS:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + GOOGLE_DOCS_LIBRARY_PROJECT_FILE_NAME;
			projectName = GOOGLE_DOCS_LIBRARY_PROJECT_FILE_NAME.substring(0, GOOGLE_DOCS_LIBRARY_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case LIBRARY_KEYRING:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + KEYRING_LIBRARY_PROJECT_FILE_NAME;
			projectName = KEYRING_LIBRARY_PROJECT_FILE_NAME.substring(0, KEYRING_LIBRARY_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case DEMOS_SALESFORCE:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SALESFORCE_DEMO_PROJECT_FILE_NAME;
			projectName = SALESFORCE_DEMO_PROJECT_FILE_NAME.substring(0, SALESFORCE_DEMO_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case DEMOS_USDIRECTORY:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + USDIRECTORY_DEMO_PROJECT_FILE_NAME;
			projectName = USDIRECTORY_DEMO_PROJECT_FILE_NAME.substring(0, USDIRECTORY_DEMO_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case DEMOS_LEGACYCRM:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + LEGACYCRM_DEMO_PROJECT_FILE_NAME;
			projectName = LEGACYCRM_DEMO_PROJECT_FILE_NAME.substring(0, LEGACYCRM_DEMO_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case DEMOS_MASHUP:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + MASHUP_DEMO_PROJECT_FILE_NAME;
			projectName = MASHUP_DEMO_PROJECT_FILE_NAME.substring(0, MASHUP_DEMO_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_MOBILE_VACATION:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SAMPLE_MOBILE_VACATION_PROJECT_FILE_NAME;
			projectName = SAMPLE_MOBILE_VACATION_PROJECT_FILE_NAME.substring(0, SAMPLE_MOBILE_VACATION_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_MOBILE_SIMPLE_APP:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SAMPLE_MOBILE_SIMPLE_APP_PROJECT_FILE_NAME;
			projectName = SAMPLE_MOBILE_SIMPLE_APP_PROJECT_FILE_NAME.substring(0, SAMPLE_MOBILE_SIMPLE_APP_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_MOBILE_CTF_GALLERY:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SAMPLE_MOBILE_CTF_GALLERY_PROJECT_FILE_NAME;
			projectName = SAMPLE_MOBILE_CTF_GALLERY_PROJECT_FILE_NAME.substring(0, SAMPLE_MOBILE_CTF_GALLERY_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_MOBILE_USDIRECTORY:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SAMPLE_MOBILE_USDIRECTORY_PROJECT_FILE_NAME;
			projectName = SAMPLE_MOBILE_USDIRECTORY_PROJECT_FILE_NAME.substring(0, SAMPLE_MOBILE_USDIRECTORY_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_MOBILE_MOBTV:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SAMPLE_MOBILE_MOB_TV_PROJECT_FILE_NAME;
			projectName = SAMPLE_MOBILE_MOB_TV_PROJECT_FILE_NAME.substring(0, SAMPLE_MOBILE_MOB_TV_PROJECT_FILE_NAME.indexOf(".car"));
			break;
		case SAMPLE_MOBILE_C8O_WEB_MOBILE:
			projectArchivePath = Engine.TEMPLATES_PATH + "/project/" + SAMPLE_MOBILE_C8O_WEB_MOBILE_PROJECT_FILE_NAME;
			projectName = SAMPLE_MOBILE_C8O_WEB_MOBILE_PROJECT_FILE_NAME.substring(0, SAMPLE_MOBILE_C8O_WEB_MOBILE_PROJECT_FILE_NAME.indexOf(".car"));
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