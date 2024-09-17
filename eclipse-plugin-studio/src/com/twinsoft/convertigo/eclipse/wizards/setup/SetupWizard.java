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

package com.twinsoft.convertigo.eclipse.wizards.setup;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.io.FileUtils;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.MySSLSocketFactory;
import com.twinsoft.convertigo.engine.ProductVersion;
import com.twinsoft.convertigo.engine.ProxyManager;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;

public class SetupWizard extends Wizard {
	private static final Pattern scheme_host_pattern = Pattern
			.compile("https://(.*?)(?::([\\d]*))?(/.*|$)");
	private static String uniqueID;

	interface SummaryGenerator {
		public String getSummary();
	}

	interface CheckConnectedCallback {
		public void onCheckConnected(boolean isConnected, String message);
	}

	private LicensePage licensePage;
	private WorkspaceMigrationPage workspaceMigrationPage;
	private WorkspaceCreationPage workspaceCreationPage;
	private ConfigureProxyPage configureProxyPage;
	private EmbeddedRegistrationPage embeddedRegistrationPage;
	private PscKeyValidationPage pscKeyValidationPage;
	private SummaryPage summaryPage;

	private ProxyManager proxyManager;

	private String previousPageName = "";
	
	String psc = "";

	public SetupWizard() {
		super();
		generateUniqueID();
		setNeedsProgressMonitor(true);
		setWindowTitle("Personal Studio Configuration");
	}

	private void generateUniqueID() {
		uniqueID = "" + System.currentTimeMillis()
				+ Math.round(300 * Math.random());
	}

	@Override
	public void addPages() {
		Engine.CONFIGURATION_PATH = Engine.USER_WORKSPACE_PATH;

		// empty workspace folder
		if (new File(Engine.USER_WORKSPACE_PATH).list().length == 0) {
			// no license acceptation if already accepted in the Windows
			// installer
			if (!System.getProperties().containsKey(
					"convertigo.license.accepted")) {
				licensePage = new LicensePage();
				addPage(licensePage);
			} else {
				System.getProperties().remove("convertigo.license.accepted");
			}

			boolean pre6_2 = false;
			for (String pathToCheck : Arrays.asList(
					"configuration/engine.properties", "minime/Java/login.txt",
					"cache", "projects", "logs")) {
				pre6_2 = new File(Engine.PROJECTS_PATH, pathToCheck).exists();
				if (!pre6_2) {
					break;
				}
			}

			if (pre6_2) {
				Engine.CONFIGURATION_PATH = Engine.PROJECTS_PATH;
				workspaceMigrationPage = new WorkspaceMigrationPage();
				addPage(workspaceMigrationPage);
			} else {
				workspaceCreationPage = new WorkspaceCreationPage();
				addPage(workspaceCreationPage);
			}
		}

		Engine.CONFIGURATION_PATH += "/configuration";

		try {
			EnginePropertiesManager.loadProperties(false);
			proxyManager = new ProxyManager();
			proxyManager.init();
		} catch (EngineException e) {
			ConvertigoPlugin.logInfo("Unexpected EngineException: "
					+ e.getMessage());
		}

		configureProxyPage = new ConfigureProxyPage(proxyManager);
		addPage(configureProxyPage);
		
		embeddedRegistrationPage = new EmbeddedRegistrationPage();
		addPage(embeddedRegistrationPage);
		
		pscKeyValidationPage = new PscKeyValidationPage();
		addPage(pscKeyValidationPage);

		summaryPage = new SummaryPage();
		addPage(summaryPage);
	}

	@Override
	public boolean performFinish() {
		try {
			EnginePropertiesManager.saveProperties(false);
		} catch (Exception e) {
		}

		if (workspaceMigrationPage != null) {
			File userWorkspace = new File(Engine.USER_WORKSPACE_PATH);

			File eclipseWorkspace = new File(Engine.PROJECTS_PATH);

			ConvertigoPlugin
					.logInfo("The current Eclipse workspace is a pre-6.2.0 CEMS workspace. Migration starting …");

			boolean projectsMoveFailed = false;

			for (File file : eclipseWorkspace.listFiles()) {
				if (!file.getName().equals(".metadata")) {
					try {
						ConvertigoPlugin
								.logInfo("Migration in progress: moving "
										+ file.getName() + " …");
						FileUtils.moveToDirectory(file, userWorkspace, false);
					} catch (IOException e) {
						projectsMoveFailed = projectsMoveFailed
								|| file.getName().equals("projects");
						ConvertigoPlugin
								.logInfo("Migration in progress: failed to move "
										+ file.getName()
										+ " ! ("
										+ e.getMessage() + ")");
					}
				}
			}

			if (!projectsMoveFailed) {
				ConvertigoPlugin
						.logInfo("Migration in progress: move move back CEMS projects to the Eclipse workspace …");
				File exMetadata = new File(userWorkspace, "projects/.metadata");
				try {
					FileUtils.copyDirectoryToDirectory(exMetadata,
							eclipseWorkspace);
					FileUtils.deleteQuietly(exMetadata);
				} catch (IOException e1) {
					ConvertigoPlugin
							.logInfo("Migration in progress: failed to merge .metadata ! ("
									+ e1.getMessage() + ")");
				}

				for (File file : new File(userWorkspace, "projects")
						.listFiles()) {
					try {
						ConvertigoPlugin
								.logInfo("Migration in progress: moving the file "
										+ file.getName()
										+ " into the Eclipse Workspace …");
						FileUtils
								.moveToDirectory(file, eclipseWorkspace, false);
					} catch (IOException e) {
						ConvertigoPlugin
								.logInfo("Migration in progress: failed to move "
										+ file.getName()
										+ " ! ("
										+ e.getMessage() + ")");
					}
				}

				ConvertigoPlugin.logInfo("Migration of workspace done !\n"
						+ "Migration of the folder: "
						+ eclipseWorkspace.getAbsolutePath() + "\n"
						+ "Eclipse Workspace with your CEMS projects: "
						+ eclipseWorkspace.getAbsolutePath() + "\n"
						+ "Convertigo Workspace with your CEMS configuration: "
						+ userWorkspace.getAbsolutePath());
			} else {
				ConvertigoPlugin
						.logInfo("Migration incomplet: cannot move back CEMS projects to the Eclipse workspace !");
			}
		}

		File pscFile = new File(Engine.USER_WORKSPACE_PATH, "studio/psc.txt");
		try {
			FileUtils.writeStringToFile(pscFile, psc, "utf-8");
		} catch (IOException e) {
			ConvertigoPlugin.logError("Failed to write the PSC file: "
					+ e.getMessage());
		}

		if (!Engine.isStarted) {
			EnginePropertiesManager.unload();
		} else {
			ConvertigoPlugin.configureDeployConfiguration();
		}
		WizardDialog container = (WizardDialog) getContainer();
		container.close();
		return true;
	}

	private HttpClient prepareHttpClient(String[] url) throws EngineException,
			MalformedURLException {
		HttpClient client = new HttpClient();

		HostConfiguration hostConfiguration = client.getHostConfiguration();

		HttpState httpState = new HttpState();
		client.setState(httpState);

		if (proxyManager != null) {
			proxyManager.getEngineProperties();
			proxyManager
					.setProxy(hostConfiguration, httpState, new URL(url[0]));
		}

		Matcher matcher = scheme_host_pattern.matcher(url[0]);
		if (matcher.matches()) {
			String host = matcher.group(1);
			String sPort = matcher.group(2);
			int port = 443;

			try {
				port = Integer.parseInt(sPort);
			} catch (Exception e) {
			}

			try {
				Protocol myhttps = new Protocol(
						"https",
						MySSLSocketFactory.getSSLSocketFactory(null, null, null, null, true),
						port);
				hostConfiguration.setHost(host, port, myhttps);
				url[0] = matcher.group(3);
			} catch (Exception e) {
				e.printStackTrace();
				e.printStackTrace();
			}
		}

		return client;
	}

	void checkConnected(final CheckConnectedCallback callback) {
		Thread th = new Thread(new Runnable() {

			public void run() {
				synchronized (SetupWizard.this) {
					boolean isConnected = false;
					String message;

					try {
						String[] urlSource = { "https://c8ocloud.convertigo.net/convertigo/index.html" };

						HttpClient client = prepareHttpClient(urlSource);
						GetMethod method = new GetMethod(urlSource[0]);

						int statusCode = client.executeMethod(method);
						if (statusCode == HttpStatus.SC_OK) {
							isConnected = true;
							message = "You are currently online";
						} else {
							message = "Bad response: HTTP status " + statusCode;
						}
					} catch (HttpException e) {
						message = "HTTP failure: " + e.getMessage();
					} catch (IOException e) {
						message = "IO failure: " + e.getMessage();
					} catch (Exception e) {
						message = "Generic failure: "
								+ e.getClass().getSimpleName() + ", "
								+ e.getMessage();
					}

					callback.onCheckConnected(isConnected, message);
				}
			}

		});
		th.setDaemon(true);
		th.setName("SetupWizard.checkConnected");
		th.start();
	}

	void postRegisterState(final String page) {
		if (!page.equals(previousPageName)) {
			previousPageName = page;
			Thread th = new Thread(new Runnable() {

				public void run() {
					synchronized (SetupWizard.this) {

						try {
							String[] url = { "http://www.google-analytics.com/collect" };
							HttpClient client = prepareHttpClient(url);
							PostMethod method = new PostMethod(url[0]);
							HeaderName.ContentType.setRequestHeader(method, MimeType.WwwForm.value());

							// set parameters for POST method
							method.setParameter("v", "1");
							method.setParameter("tid", "UA-660091-6");
							method.setParameter("cid", getUniqueID());
							method.setParameter("t", "pageview");
							method.setParameter("dh",
									"http://www.convertigo.com");
							method.setParameter("dp",
									"/StudioRegistrationWizard_" + page
											+ ".html");
							method.setParameter("dt", page + "_"
									+ ProductVersion.productVersion);

							// execute HTTP post with parameters
							if (client != null) {
								client.executeMethod(method);
							}
						} catch (Exception e) {
							// ConvertigoPlugin.logWarning(e,
							// "Error while trying to send registration");
						}
					}
				}
			});
			th.setDaemon(true);
			th.setName("SetupWizard.register_steps");
			th.start();
		} else {
			previousPageName = page;
		}
	}

	private static String getUniqueID() {
		return uniqueID;
	}

	@Override
	public boolean canFinish() {
		return getContainer().getCurrentPage().getNextPage() == null;
	}
}
