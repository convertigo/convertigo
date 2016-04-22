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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.MySSLSocketFactory;
import com.twinsoft.convertigo.engine.ProductVersion;
import com.twinsoft.convertigo.engine.ProxyManager;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class SetupWizard extends Wizard {
	public static final String registrationServiceUrl = "https://c8o.convertigo.net/cems/projects/studioRegistration/.xml";

	private static final Pattern scheme_host_pattern = Pattern
			.compile("https://(.*?)(?::([\\d]*))?(/.*|$)");
	private static String uniqueID;

	interface SummaryGenerator {
		public String getSummary();
	}

	interface CheckConnectedCallback {
		public void onCheckConnected(boolean isConnected, String message);
	}

	interface RegisterCallback {
		public void onRegister(boolean success, String message);
	}

	protected LicensePage licensePage;
	protected WorkspaceMigrationPage workspaceMigrationPage;
	protected WorkspaceCreationPage workspaceCreationPage;
	protected ConfigureProxyPage configureProxyPage;
//	protected AlreadyPscKeyPage alreadyPscKeyPage;
	protected PscKeyValidationPage pscKeyValidationPage;
//	protected RegistrationPage registrationPage;
//	protected PscKeyPage pscKeyPage;
	protected SummaryPage summaryPage;

	protected ProxyManager proxyManager;

	private String previousPageName = "";

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

//		alreadyPscKeyPage = new AlreadyPscKeyPage();
//		addPage(alreadyPscKeyPage);

		pscKeyValidationPage = new PscKeyValidationPage();
		addPage(pscKeyValidationPage);
		
//		registrationPage = new RegistrationPage();
//		addPage(registrationPage);

//		pscKeyPage = new PscKeyPage();
//		addPage(pscKeyPage);

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
			FileUtils.writeStringToFile(pscFile,
//					pscKeyPage.getCertificateKey(), "utf-8");
					pscKeyValidationPage.getCertificateKey(), "utf-8");
		} catch (IOException e) {
			ConvertigoPlugin.logError("Failed to write the PSC file: "
					+ e.getMessage());
		}

		if (!Engine.isStarted) {
			EnginePropertiesManager.unload();
		} else {
			ConvertigoPlugin.configureDeployConfiguration();
		}

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

	public void checkConnected(final CheckConnectedCallback callback) {
		Thread th = new Thread(new Runnable() {

			public void run() {
				synchronized (SetupWizard.this) {
					boolean isConnected = false;
					String message;

					try {
						String[] urlSource = { "https://c8o.convertigo.net/cems/index.html" };

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

	public void register(final String username, final String password,
			final String firstname, final String lastname, final String email,
			final String country, final String company,
			final String companyHeadcount, final RegisterCallback callback) {
		Thread th = new Thread(new Runnable() {

			public void run() {
				synchronized (SetupWizard.this) {
					boolean success = false;
					String message;

					try {
						String[] url = { registrationServiceUrl };
						HttpClient client = prepareHttpClient(url);
						PostMethod method = new PostMethod(url[0]);
						HeaderName.ContentType.setRequestHeader(method, MimeType.WwwForm.value());
						// set parameters for POST method
						method.setParameter("__sequence", "checkEmail");
						method.setParameter("username", username);
						method.setParameter("password", password);
						method.setParameter("firstname", firstname);
						method.setParameter("lastname", lastname);
						method.setParameter("email", email);
						method.setParameter("country", country);
						method.setParameter("company", company);
						method.setParameter("companyHeadcount",
								companyHeadcount);

						// execute HTTP post with parameters
						int statusCode = client.executeMethod(method);
						if (statusCode == HttpStatus.SC_OK) {
							Document document = XMLUtils.parseDOM(method
									.getResponseBodyAsStream());
							NodeList nd = document
									.getElementsByTagName("errorCode");

							if (nd.getLength() > 0) {
								Node node = nd.item(0);
								String errorCode = node.getTextContent();

								if ("0".equals(errorCode)) {
									success = true;
									message = "Registration submited, please check your email.";
								} else {
									method = new PostMethod(
											registrationServiceUrl);

									// set parameters for POST method to get the
									// details of error messages
									method.setParameter("__sequence",
											"getErrorMessages");
									client.executeMethod(method);
									document = XMLUtils.parseDOM(method
											.getResponseBodyAsStream());
									nd = document.getElementsByTagName("label");
									Node nodeDetails = nd.item(Integer
											.parseInt(errorCode));

									ConvertigoPlugin.logError(nodeDetails
											.getTextContent());
									message = "Failed to register: "
											+ nodeDetails.getTextContent();
								}
							} else {
								success = true;
								message = "debug";
							}
						} else {
							message = "Unexpected HTTP status: " + statusCode;
						}
					} catch (Exception e) {
						message = "Generic failure: "
								+ e.getClass().getSimpleName() + ", "
								+ e.getMessage();
						ConvertigoPlugin.logException(e,
								"Error while trying to send registration");
					}
					callback.onRegister(success, message);
				}
			}

		});
		th.setDaemon(true);
		th.setName("SetupWizard.register");
		th.start();
	}

	public void postRegisterState(final String page) {
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

	public static String getUniqueID() {
		return uniqueID;
	}
}
