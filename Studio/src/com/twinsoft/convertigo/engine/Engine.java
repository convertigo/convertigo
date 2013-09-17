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

package com.twinsoft.convertigo.engine;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.security.Provider;
import java.security.Security;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.swing.event.EventListenerList;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.twinsoft.api.SessionManager;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.cache.CacheManager;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.externalbrowser.ExternalBrowserManager;
import com.twinsoft.convertigo.engine.plugins.AbstractBiller;
import com.twinsoft.convertigo.engine.plugins.PluginsManager;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.scheduler.SchedulerManager;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.Crypto2;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.LogWrapper;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.tas.ApplicationException;
import com.twinsoft.tas.Authentication;
import com.twinsoft.tas.KeyManager;
import com.twinsoft.tas.ParsingException;
import com.twinsoft.util.Log;
import com.twinsoft.util.TWSKey;

public class Engine {
	
	public static final String JVM_PROPERTY_USER_WORKSPACE = "convertigo.cems.user_workspace_path";
	public static final String JVM_PROPERTY_GLOBAL_SYMBOLS_FILE = "convertigo.cems.global_symbols_file";
	public static final String JVM_PROPERTY_GLOBAL_SYMBOLS_FILE_COMPATIBILITY = "convertigo_global_symbols";
	
	public static String USER_WORKSPACE_PATH = System.getProperty(JVM_PROPERTY_USER_WORKSPACE, System.getProperty("user.home") + "/convertigo");
	public static String PROJECTS_PATH = "";
	public static String CERTIFICATES_PATH = "";
	public static String LOG_PATH = "";
	public static String XSL_PATH = "";
	public static String DTD_PATH = "";
	public static String TEMPLATES_PATH = "";
	public static String WEBAPP_PATH = "";
	public static String CACHE_PATH = "";
	public static String CONFIGURATION_PATH = "";

	static {
		// Do not forget to retrieve the canonical paths, i.e. the path
		// where ".." and "." are resolved to unique paths.
		try {
			USER_WORKSPACE_PATH = new File(USER_WORKSPACE_PATH).getCanonicalPath();
		} catch (IOException e) {
		}
	}
	/**
	 * This is the application reference.
	 */
	public static Engine theApp;

	public static AuthenticatedSessionManager authenticatedSessionManager;

	/**
	 * Defines if application is started.
	 */
	public static boolean isStarted = false;

	/**
	 * Defines if application is started.
	 */
	public static boolean isStartFailed = false;

	/**
	 * Defines if application is Service Oriented.
	 */
	public static boolean isSOA = false;

	/**
	 * Defines if that Convertigo is a Studio.
	 */
	private static boolean bStudioMode = false;

	/**
	 * The database objects manager.
	 */
	public DatabaseObjectsManager databaseObjectsManager;

	/**
	 * The SQL connections manager.
	 */
	public JdbcConnectionManager sqlConnectionManager;

	/**
	 * The files manager
	 */
	public FilePropertyManager filePropertyManager;

	/**
	 * The TracePlayer manager
	 */
	public TracePlayerManager tracePlayerManager;

	/**
	 * The context manager.
	 */
	public ContextManager contextManager;

	/**
	 * The thread manager.
	 */
	public ThreadManager threadManager;

	/**
	 * The security token manager.
	 */
	public SecurityTokenManager securityTokenManager;

	/**
	 * The cache manager.
	 */
	public CacheManager cacheManager;

	/**
	 * The usage monitor.
	 */
	public UsageMonitor usageMonitor;

	/**
	 * The billing manager
	 */
	public BillingManager billingManager;

	/**
	 * The proxy manager
	 */
	public ProxyManager proxyManager;
	
	/**
	 * The external browser manager
	 */
	public ExternalBrowserManager externalBrowserManager;
	
	/**
	 * The external browser manager
	 */
	public SchemaManager schemaManager;
	
	/**
	 * The external browser manager
	 */
	public ResourceCompressorManager resourceCompressorManager;

	/**
	 * The plugins manager
	 */
	public PluginsManager pluginsManager;
	
	/**
	 * Loggers
	 */
	public static Logger logConvertigo;
	public static Logger logEngine;
	public static Logger logBillers;
	public static Logger logAdmin;
	public static Logger logBeans;
	public static Logger logContext;
	public static Logger logEmulators;
	public static Logger logUser;
	public static Logger logContextManager;
	public static Logger logCacheManager;
	public static Logger logTracePlayerManager;
	public static Logger logDatabaseObjectManager;
	public static Logger logJobManager;
	public static Logger logCertificateManager;
	public static Logger logProxyManager;
	public static Logger logUsageMonitor;
	public static Logger logStatistics;
	public static Logger logScheduler;
	public static Logger logSiteClipper;
	public static Logger logExternalBrowser;
	public static Logger logAudit;

	/**
	 * The Log object for the application.
	 */
	@Deprecated
	public static LogWrapper log;

	/**
	 * Defines the cloud customer_name.
	 */
	final public static String cloud_customer_name;

	/**
	 * The engine start/stop date.
	 */
	public static long startStopDate;

	/**
	 * The scheduler for running jobs.
	 */
	public SchedulerManager schedulerManager;

	/**
	 * The event manager for dispatching events.
	 */
	public EventManager eventManager;

	public HttpClient httpClient;
	public MultiThreadedHttpConnectionManager connectionManager;
	public RsaManager rsaManager;

	static {
		try {
			Engine.authenticatedSessionManager = new AuthenticatedSessionManager();
			Engine.authenticatedSessionManager.init();
		} catch (EngineException e) {
			e.printStackTrace();
		}

		cloud_customer_name = System.getProperty("convertigo.cloud.customer_name");
	}
	
	private static boolean bInitPathsDone = false;

	public static void initPaths(String webappPath) throws IOException {
		if (bInitPathsDone) return;

		// Create/update the user workspace if needed
		File userWorkspaceDirectory = new File(Engine.USER_WORKSPACE_PATH);
		File defaultUserWorkspaceDirectory = new File(webappPath + "/WEB-INF/default_user_workspace");
		System.out.println("Updating the user workspace from '" + defaultUserWorkspaceDirectory.toString()
				+ "' to '" + Engine.USER_WORKSPACE_PATH + "'");
		try {
			FileUtils.mergeDirectories(defaultUserWorkspaceDirectory, userWorkspaceDirectory);
		} catch (Exception e) {
			System.out.println("Error while updating the user workspace");
			e.printStackTrace();
		}
		
		if (Engine.PROJECTS_PATH.length() == 0) {
			Engine.PROJECTS_PATH = new File(Engine.USER_WORKSPACE_PATH + "/projects").getCanonicalPath();
		}
		
		Engine.CACHE_PATH = new File(Engine.USER_WORKSPACE_PATH + "/cache").getCanonicalPath();
		Engine.CONFIGURATION_PATH = new File(Engine.USER_WORKSPACE_PATH + "/configuration").getCanonicalPath();
		Engine.CERTIFICATES_PATH = new File(Engine.USER_WORKSPACE_PATH + "/certificates").getCanonicalPath();

		Engine.WEBAPP_PATH = new File(webappPath).getCanonicalPath();
		Engine.XSL_PATH = new File(Engine.WEBAPP_PATH + "/xsl").getCanonicalPath();
		Engine.DTD_PATH = new File(Engine.WEBAPP_PATH + "/dtd").getCanonicalPath();
		Engine.TEMPLATES_PATH = new File(Engine.WEBAPP_PATH + "/templates").getCanonicalPath();
		
		bInitPathsDone = true;
	}

	public static synchronized void start() throws EngineException {
		if (Engine.theApp == null) {
			System.out.println("Starting Convertigo Enterprise Mashup Server");

			// If the engine has been stopped by the admin, we must reload
			// properties
			EnginePropertiesManager.loadProperties();

			boolean bProjectsDataCompatibilityMode = Boolean.parseBoolean(EnginePropertiesManager
					.getProperty(PropertyName.PROJECTS_DATA_COMPATIBILITY_MODE));

			if (bProjectsDataCompatibilityMode) {
				System.out.println("Projects data compatibility mode required");
				try {
					Engine.PROJECTS_PATH = new File(Engine.WEBAPP_PATH + "/projects").getCanonicalPath();
					File projectsDir = new File(Engine.PROJECTS_PATH);
					projectsDir.mkdir();
				} catch (IOException e) {
					throw new EngineException("Unable to update projects path for compatibility mode", e);
				}
			}

			isStarted = false;
			isStartFailed = false;
			RequestableObject.nbCurrentWorkerThreads = 0;

			Engine.startStopDate = System.currentTimeMillis();

			System.out.println("Creating/updating loggers");

			String logFile = EnginePropertiesManager
					.getProperty(PropertyName.LOG4J_APPENDER_CEMSAPPENDER_FILE);
			System.out.println("Log file: " + logFile);

			// Main loggers
			Engine.logConvertigo = Logger.getLogger("cems");
			Engine.logEngine = Logger.getLogger("cems.Engine");
			Engine.logAdmin = Logger.getLogger("cems.Admin");
			Engine.logBeans = Logger.getLogger("cems.Beans");
			Engine.logBillers = Logger.getLogger("cems.Billers");
			Engine.logEmulators = Logger.getLogger("cems.Emulators");
			Engine.logContext = Logger.getLogger("cems.Context");
			Engine.logUser = Logger.getLogger("cems.Context.User");
			Engine.logUsageMonitor = Logger.getLogger("cems.UsageMonitor");
			Engine.logStatistics = Logger.getLogger("cems.Statistics");
			Engine.logScheduler = Logger.getLogger("cems.Scheduler");
			Engine.logSiteClipper = Logger.getLogger("cems.SiteClipper");
			/** #3437 : Disabled ExternalBrowser feature because it's not terminated
			Engine.logExternalBrowser = Logger.getLogger("cems.ExternalBrowser");
			*/
			Engine.logAudit = Logger.getLogger("cems.Context.Audit");
			
			// Managers
			Engine.logContextManager = Logger.getLogger("cems.ContextManager");
			Engine.logCacheManager = Logger.getLogger("cems.CacheManager");
			Engine.logTracePlayerManager = Logger.getLogger("cems.TracePlayerManager");
			Engine.logJobManager = Logger.getLogger("cems.JobManager");
			Engine.logCertificateManager = Logger.getLogger("cems.CertificateManager");
			Engine.logDatabaseObjectManager = Logger.getLogger("cems.DatabaseObjectManager");
			Engine.logProxyManager = Logger.getLogger("cems.ProxyManager");

			// Logger for compatibility issues
			Engine.log = new LogWrapper(Engine.logConvertigo);

			try {
				Engine.logEngine.info("===========================================================");
				Engine.logEngine.info("Web app home: " + Engine.WEBAPP_PATH);
				Engine.logEngine.info("User workspace: " + Engine.USER_WORKSPACE_PATH);
				Engine.logEngine.info("Configuration path: " + Engine.CONFIGURATION_PATH);

				Engine.logEngine.info("Projects path: " + Engine.PROJECTS_PATH);
				if (bProjectsDataCompatibilityMode) {
					Engine.logEngine.warn("Projects data compatibility mode required");
				}

				Engine.logEngine.info("Log: " + Engine.LOG_PATH);

				if (cloud_customer_name != null) {
					Engine.logEngine.info("Cloud customer: " + cloud_customer_name);
				}

				// Check environment and native dependencies
				if (!isStudioMode()) {
					StartupDiagnostics.run();
				}
				
				// Initializing the engine
				Engine.theApp = new Engine();
				
				CachedIntrospector.prefetchDatabaseObjectsAsync();

				try {
					Engine.theApp.usageMonitor = new UsageMonitor();
					Engine.theApp.usageMonitor.init();

					Thread vulture = new Thread(Engine.theApp.usageMonitor);
					vulture.setName("UsageMonitor");
					vulture.setDaemon(true);
					vulture.start();
				} catch (Exception e) {
					Engine.logEngine.error("Unable to launch the usage monitor.", e);
				}

				Engine.theApp.eventManager = new EventManager();
				Engine.theApp.eventManager.init();

				Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager();
				Engine.theApp.databaseObjectsManager.init();

				Engine.theApp.sqlConnectionManager = new JdbcConnectionManager();
				Engine.theApp.sqlConnectionManager.init();

				Engine.theApp.filePropertyManager = new FilePropertyManager();
				Engine.theApp.filePropertyManager.init();

				try {
					Engine.theApp.proxyManager = new ProxyManager();
					Engine.theApp.proxyManager.init();
				} catch (Exception e) {
					Engine.logEngine.error("Unable to launch the proxy manager.", e);
				}

				try {
					Engine.theApp.billingManager = new BillingManager();
					Engine.theApp.billingManager.init();
				} catch (Exception e) {
					Engine.logEngine.error("Unable to launch the billing manager.", e);
				}

				// Initialization of the trace player
				try {
					Engine.theApp.tracePlayerManager = new TracePlayerManager();
					Engine.theApp.tracePlayerManager.init();
				} catch (Exception e) {
					Engine.logEngine.error("Unable to run the trace player.", e);
				}
				
				try {
					Engine.theApp.resourceCompressorManager = new ResourceCompressorManager();
					Engine.theApp.resourceCompressorManager.init();
				} catch (Exception e) {
					Engine.logEngine.error("Unable to run the resource compressor.", e);
				} 
				
				try {
					Engine.theApp.pluginsManager = new PluginsManager();
					Engine.theApp.pluginsManager.init();
				} catch (Exception e) {
					Engine.logEngine.error("Unable to run the plugins manager.", e);
				}
				
				Engine.logEngine
						.info("Current working directory is '" + System.getProperty("user.dir") + "'.");

				// Creating the Carioca Authentication objects
				Engine.logEngine.debug("Creating the Carioca Authentication objects");
				String cariocaUserName = EnginePropertiesManager
						.getProperty(PropertyName.CARIOCA_DEFAULT_USER_NAME);
				String cariocaPassword = EnginePropertiesManager
						.getProperty(PropertyName.CARIOCA_DEFAULT_USER_PASSWORD);
				Engine.theApp.authentications = new Hashtable<String, Authentication>();

				// Initialization of the default TAS properties
				try {
					KeyManager.log = new LogWrapper(Engine.logEngine);
					Authentication auth = Engine.theApp.getAuthenticationObject("", null);
					auth.login(cariocaUserName, cariocaPassword);
				} catch (Exception e) {
					Engine.logEngine.error("The authentication to Carioca has failed (user name: \""
							+ cariocaUserName + "\", user password: \"" + cariocaPassword + "\").", e);
				}

				// TODO : retrieve SOA flag from KeyManager
				isSOA = true;

				// Creation of the session manager
				Engine.theApp.sessionManager = new SessionManager();
				Engine.theApp.sessionManager.setLog(new LogWrapper(Engine.logEngine));
				Engine.theApp.sessionManager.setProductCode(SessionManager.CONVERTIGO);

				String s = EnginePropertiesManager.getProperty(PropertyName.CONNECTORS_MONITORING);
				Engine.theApp.setMonitored(s.equalsIgnoreCase("true"));

				// Create Projects directory if needed
				File projectsDirFile = new File(Engine.PROJECTS_PATH);
				try {
					if (!projectsDirFile.exists())
						projectsDirFile.mkdirs();
				} catch (Exception e) {
					Engine.logEngine.error("An error occured while creating projects directory.", e);
				}

				// Starts projects migration process
				MigrationManager.performProjectsMigration();

				// Security providers registration
				try {
					File dir = new File(Engine.CERTIFICATES_PATH);
					String[] files = dir.list(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.endsWith((".pkcs11"));
						}
					});

					if (files != null && files.length > 0) {
						Engine.logEngine.info("Registering security providers...");
						try {
							Class<?> sunPKCS11Class = Class.forName("sun.security.pkcs11.SunPKCS11");
							String file;

							for (int i = 0; i < files.length; i++) {
								file = Engine.CERTIFICATES_PATH + "/" + files[i];
								try {
									Constructor<?> constructor = sunPKCS11Class
											.getConstructor(new Class[] { String.class });
									Provider provider = (Provider) constructor
											.newInstance(new Object[] { file });
									Security.addProvider(provider);
									Engine.logEngine.info("Provider '" + provider.getName()
											+ "' has been successfully registered.");
								} catch (Exception e) {
									Engine.logEngine
											.error("Unable to register security provider from file: "
													+ file
													+ " . Please check that the implementation library is in the Java lib path.");
								}
							}
						} catch (ClassNotFoundException e) {
							Engine.logEngine
									.error("Unable to find sun.security.pkcs11.SunPKCS11 class! PKCS#11 authentication won't be available. You must use JVM 1.5 or higher in order to use PKCS#11 authentication.");
						}
					}

					Provider[] providers = Security.getProviders();
					String sProviders = "";
					for (int i = 0; i < providers.length; i++) {
						sProviders += providers[i].getName() + ", ";
					}
					Engine.logEngine.debug("Registered providers: " + sProviders);
				} catch (Exception e) {
					Engine.logEngine.error("Unable to register security providers.", e);
				}

				// Launch the cache manager
				try {
					String cacheManagerClassName = EnginePropertiesManager
							.getProperty(PropertyName.CACHE_MANAGER_CLASS);
					Engine.logEngine.debug("Cache manager class: " + cacheManagerClassName);
					Engine.theApp.cacheManager = (CacheManager) Class.forName(cacheManagerClassName)
							.newInstance();
					Engine.theApp.cacheManager.init();

					Thread vulture = new Thread(Engine.theApp.cacheManager);
					Engine.theApp.cacheManager.executionThread = vulture;
					vulture.setName("CacheManager");
					vulture.setDaemon(true);
					vulture.start();
				} catch (Exception e) {
					Engine.logEngine.error("Unable to launch the cache manager.", e);
				}

				// Launch the thread manager
				try {
					Engine.theApp.threadManager = new ThreadManager();
					Engine.theApp.threadManager.init();

					Thread vulture = new Thread(Engine.theApp.threadManager);
					Engine.theApp.threadManager.executionThread = vulture;
					vulture.setName("ThreadManager");
					vulture.setDaemon(true);
					vulture.start();
				} catch (Exception e) {
					Engine.theApp.threadManager = null;
					Engine.logEngine.error("Unable to launch the thread manager.", e);
				}

				// Launch the context manager
				try {
					Engine.theApp.contextManager = new ContextManager();
					Engine.theApp.contextManager.init();

					Thread vulture = new Thread(Engine.theApp.contextManager);
					Engine.theApp.contextManager.executionThread = vulture;
					vulture.setName("ContextManager");
					vulture.setDaemon(true);
					vulture.start();
				} catch (Exception e) {
					Engine.theApp.contextManager = null;
					Engine.logEngine.error("Unable to launch the context manager.", e);
				}

				// Launch the security token manager
				Engine.theApp.securityTokenManager = new SecurityTokenManager();
				Engine.theApp.securityTokenManager.init();
				
				// Initialize the HttpClient
				try {
					Engine.logEngine.debug("HttpClient initializing...");
					Engine.theApp.connectionManager = new MultiThreadedHttpConnectionManager();
					// Engine.theApp.connectionManager.setConnectionStaleCheckingEnabled(true);

					int maxTotalConnections = 100;
					try {
						maxTotalConnections = new Integer(
								EnginePropertiesManager
										.getProperty(PropertyName.HTTP_CLIENT_MAX_TOTAL_CONNECTIONS))
								.intValue();
					} catch (NumberFormatException e) {
						Engine.logEngine
								.warn("Unable to retrieve the max number of connections; defaults to 100.");
					}

					int maxConnectionsPerHost = 50;
					try {
						maxConnectionsPerHost = new Integer(
								EnginePropertiesManager
										.getProperty(PropertyName.HTTP_CLIENT_MAX_CONNECTIONS_PER_HOST))
								.intValue();
					} catch (NumberFormatException e) {
						Engine.logEngine
								.warn("Unable to retrieve the max number of connections per host; defaults to 100.");
					}

					// Engine.theApp.connectionManager.setMaxConnectionsPerHost(maxTotalConnections);
					// Engine.theApp.connectionManager.setMaxTotalConnections(maxConnectionsPerHost);

					HttpConnectionManagerParams httpConnectionManagerParams = new HttpConnectionManagerParams();
					httpConnectionManagerParams.setDefaultMaxConnectionsPerHost(maxConnectionsPerHost);
					httpConnectionManagerParams.setMaxTotalConnections(maxTotalConnections);
					Engine.theApp.connectionManager.setParams(httpConnectionManagerParams);

					HttpClientParams httpClientParams = ((HttpClientParams) HttpClientParams
							.getDefaultParams());
					httpClientParams.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
					/** #741 : belambra wants only one Set-Cookie header */
					httpClientParams.setParameter("http.protocol.single-cookie-header", Boolean.TRUE);

					Engine.theApp.httpClient = new HttpClient(Engine.theApp.connectionManager);

					Engine.logEngine.debug("HttpClient initialized!");
				} catch (Exception e) {
					Engine.theApp.connectionManager = null;
					Engine.logEngine.error("Unable to initialize the HttpClient.", e);
				}

				// Initialization of the schedule manager
				Engine.theApp.schedulerManager = new SchedulerManager(true);

				// Initialization of the RSA manager
				Engine.theApp.rsaManager = new RsaManager();
				Engine.theApp.rsaManager.init();

				// Initialization of the External Browser manager
				/** #3437 : Disabled ExternalBrowser feature because it's not terminated
				Engine.theApp.externalBrowserManager = new ExternalBrowserManager();
				Engine.theApp.externalBrowserManager.init();
				*/
				
				// Initialization of the Schema manager
				Engine.theApp.schemaManager = new SchemaManager();
				Engine.theApp.schemaManager.init();
				
				// XUL initialization
				String xulrunner_url = System.getProperty("org.eclipse.swt.browser.XULRunnerPath");
				if (xulrunner_url == null || xulrunner_url.equals("")) {
					xulrunner_url = EnginePropertiesManager.getProperty(PropertyName.XULRUNNER_URL);
				}

				File f = new File(xulrunner_url);
				if (f.exists()) {
					xulrunner_url = f.getAbsolutePath();
					Engine.logEngine.debug("initMozillaSWT: org.eclipse.swt.browser.XULRunnerPath=" + xulrunner_url);
					System.setProperty("org.eclipse.swt.browser.XULRunnerPath", xulrunner_url);
				} else {
					Engine.logEngine.error("Error in initMozillaSWT: " + xulrunner_url + " doesn't exist, fix it with xulrunner.url");
				}

				if (Engine.isEngineMode() && Engine.isLinux()
						&& "true".equals(EnginePropertiesManager.getProperty(PropertyName.LINUX_LAUNCH_XVNC))) {
					
					Engine.logEngine.debug("initMozillaSWT: org.eclipse.swt.browser.XULRunnerPath=" + xulrunner_url);
					final String display = System.getenv("DISPLAY");
					if (display != null) {
						try {
							String port = System.getProperty("xvnc.port");
							if (port == null) {
								port = "" + (Integer.parseInt(display.replaceAll(".*:(\\d*)", "$1")) + 5900);
								System.setProperty("xvnc.port", port);
							}
							Engine.logEngine.debug("Xvnc should listen on " + port + " port");
						} catch (Exception e) {}
						try {							
							File vncDir = new File(Engine.WEBAPP_PATH + "/WEB-INF/xvnc");
							File Xvnc = new File(vncDir, "/Xvnc");
							File fonts = new File(vncDir, "/fonts");
							File wm = new File(vncDir, "/matchbox-window-manager");
							if (vncDir.exists() && Xvnc.exists() && fonts.exists() && wm.exists()) {
								for (File file : GenericUtils.<File> asList(Xvnc, wm)) {
									new ProcessBuilder("/bin/chmod", "u+x", file.getAbsolutePath()).start().waitFor();
								}
								String depth = EnginePropertiesManager.getProperty(PropertyName.LINUX_XVNC_DEPTH);
								String geometry = EnginePropertiesManager.getProperty(PropertyName.LINUX_XVNC_GEOMETRY);
								Engine.logEngine.debug("Xvnc will use depth " + depth + " and geometry " + geometry);
								Process pr_xvnc = new ProcessBuilder(Xvnc.getAbsolutePath(), display, "-fp",
										fonts.getAbsolutePath(), "-depth", depth, "-geometry", geometry)
										.start();
								Thread.sleep(500);
								try {
									int exit = pr_xvnc.exitValue();
									InputStream err = pr_xvnc.getErrorStream();
									byte[] buf = new byte[err.available()];
									err.read(buf);
									Engine.logEngine.debug("Xvnc failed to run with exit code " + exit
											+ " and this error : <<" + new String(buf, "UTF-8") + ">>");
								} catch (Exception e) {
									new ProcessBuilder(wm.getAbsolutePath()).start();
									Engine.logEngine.debug("Xvnc successfully started !");
								}
							} else {
								Engine.logEngine.info(vncDir.getAbsolutePath() + " not found or incomplet, can't start Xvnc");
							}
						} catch (Exception e) {
							Engine.logEngine.info("failed to launch Xvnc (maybe already launched", e);
						}
					} else
						Engine.logEngine.warn("Trying to start Xvnc on Linux without DISPLAY environment variable !");
				}

				isStarted = true;

				Engine.logEngine.info("Convertigo engine started");
			} catch (Throwable e) {
				isStartFailed = true;
				Engine.logEngine.error("Unable to successfully start the engine.", e);
			}
		} else {
			Engine.logEngine.info("Convertigo engine already started");
		}
	}
	
	public static synchronized void stop() throws EngineException {
		if (Engine.theApp != null) {
			if (!MigrationManager.isMigrationFinished())
				throw new EngineException("Migration process of projects is still running.");

			try {
				Engine.logEngine.info("Stopping the engine");
				
				// Temporary reset the start/stop date in order to unlink the requestable's
				// running thread engine ID.
				Engine.startStopDate = 0;
				
				Engine.logEngine.info("Removing all contexts");
				if (Engine.theApp.contextManager != null)
					Engine.theApp.contextManager.destroy();

				Engine.logEngine.info("Resetting statistics");
				EngineStatistics.reset();

				Engine.logEngine.info("Removing the usage monitor");
				Engine.theApp.usageMonitor.destroy();

				Engine.logEngine.info("Removing the SQL connections manager");
				if (Engine.theApp.sqlConnectionManager != null) {
					Engine.theApp.sqlConnectionManager.destroy();
				}

				Engine.logEngine.info("Removing the file property manager");
				if (Engine.theApp.filePropertyManager != null) {
					Engine.theApp.filePropertyManager.destroy();
				}

				Engine.logEngine.info("Removing the billing manager");
				if (Engine.theApp.billingManager != null) {
					Engine.theApp.billingManager.destroy();
				}

				Engine.logEngine.info("Removing the TracePlayer manager");
				if (Engine.theApp.tracePlayerManager != null) {
					Engine.theApp.tracePlayerManager.destroy();
				}

				Engine.logEngine.info("Removing the cache manager");
				if (Engine.theApp.cacheManager != null) {
					Engine.theApp.cacheManager.destroy();
				}

				Engine.logEngine.info("Removing the proxy manager");
				if (Engine.theApp.proxyManager != null) {
					Engine.theApp.proxyManager.destroy();
				}
				
				if (Engine.theApp.rsaManager != null) {
					Engine.theApp.rsaManager.destroy();
				}
				
				if (Engine.theApp.externalBrowserManager != null) {
					Engine.theApp.externalBrowserManager.destroy();
				}
				
				if (Engine.theApp.schemaManager != null) {
					Engine.theApp.schemaManager.destroy();
				}
				
				if (Engine.theApp.resourceCompressorManager != null) {
					Engine.theApp.resourceCompressorManager.destroy();
				}

				if (Engine.theApp.pluginsManager != null) {
					Engine.theApp.pluginsManager.destroy();
				}
				
				// Closing the session manager
				if (Engine.theApp.sessionManager != null) {
					Engine.logEngine.info("Closing the session manager");
					Engine.theApp.sessionManager.removeAllSessions();
					Engine.theApp.sessionManager = null;
				}

				Engine.logEngine.info("Resetting the key manager");
				KeyManager.reset();

				if (Engine.theApp.schedulerManager != null)
					Engine.theApp.schedulerManager.destroy();

				Engine.logEngine.info("Removing the database objects manager");
				if (Engine.theApp.databaseObjectsManager != null)
					Engine.theApp.databaseObjectsManager.destroy();

				Engine.logEngine.info("Removing the thread manager");
				if (Engine.theApp.threadManager != null)
					Engine.theApp.threadManager.destroy();

				Engine.logEngine.info("Removing the security token manager");
				if (Engine.theApp.securityTokenManager != null)
					Engine.theApp.securityTokenManager.destroy();

				Engine.logEngine.info("The Convertigo Engine has been successfully stopped.");
			} finally {
				Engine.startStopDate = System.currentTimeMillis();
				Engine.theApp.eventManager = null;

				isStarted = false;
				RequestableObject.nbCurrentWorkerThreads = 0;

				EnginePropertiesManager.unload();

				Engine.theApp = null;

				System.gc();
			}
		} else {
			Engine.logEngine.info("Convertigo engine already stopped");
		}
	}

	/**
	 * The Carioca authentication objects map.
	 */
	public Map<String, Authentication> authentications = null;

	/**
	 * The session manager for Javelin sessions.
	 */
	public SessionManager sessionManager;

	private boolean bMonitored = false;

	public ConnectorsMonitor connectorsMonitor;

	public void setMonitored(boolean bMonitored) {
		if (bMonitored != this.bMonitored) {
			this.bMonitored = bMonitored;

			if (bMonitored) {
				if (connectorsMonitor == null)
					connectorsMonitor = new ConnectorsMonitor();
				connectorsMonitor.setVisible(true);
				sessionManager.setMonitorFrame(connectorsMonitor);
				sessionManager.setMonitor(true);
				// TODO: ajouter tous les connecteurs déjà chargés dans les
				// contextes
			} else if (connectorsMonitor != null) {
				connectorsMonitor.setVisible(false);
			}
		}
	}

	public boolean isMonitored() {
		return bMonitored;
	}

	/**
	 * Constructs a new Engine object.
	 */
	public Engine() {
		Engine.logEngine.info("===========================================================");
		Engine.logEngine.info(" Convertigo Enterprise Mashup Server "
				+ com.twinsoft.convertigo.engine.Version.fullProductVersion);
		Engine.logEngine.info("    engine " + Version.version);
		Engine.logEngine.info("    beans " + com.twinsoft.convertigo.beans.Version.version);
		Engine.logEngine.info("===========================================================");

		Engine.logEngine.info("Start date: " + (new Date(startStopDate)).toString());
		Engine.logEngine.info("Class loader: " + getClass().getClassLoader());
		Engine.logEngine.info("Java Runtime " + System.getProperty("java.version") + " (classes: "
				+ System.getProperty("java.class.version") + ", vendor: " + System.getProperty("java.vendor")
				+ ")");

		try {
			Engine.logEngine.info("Java XML engine: " + org.apache.xerces.impl.Version.getVersion());
		} catch (Throwable e) {
			Engine.logEngine.warn("Unable to detect the XML engine");
		}

		try {
			Engine.logEngine.info("Java XSL engine: " + org.apache.xalan.Version.getVersion());
		} catch (Throwable e) {
			Engine.logEngine.warn("Unable to detect the XSL engine");
		}

		// Enumeration of the properties
		try {
			Engine.logEngine.debug(EnginePropertiesManager.getPropertiesAsString("System properties",
					System.getProperties()));
		} catch (Exception e) {
			Engine.logEngine.error("Unable to retrieve the sorted system properties list.", e);
		}

		// Enumeration of the properties
		try {
			Engine.logEngine.debug(EnginePropertiesManager.getPropertiesAsString("Engine properties", null));
		} catch (Exception e) {
			Engine.logEngine.error("Unable to retrieve the sorted engine properties list.", e);
		}
	}

	public Authentication getAuthenticationObject(String tasVirtualServer, String authName)
			throws ParsingException, com.twinsoft.tas.SQLException, ApplicationException {
		String name = (tasVirtualServer.equals("") ? "carioca" : tasVirtualServer) + "_" + authName;
		Authentication auth = (Authentication) authentications.get(name);

		if (auth == null) {
			Properties tasProperties = new Properties();
			tasProperties.put("tasRoot",
					EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.CARIOCA_URL));
			tasProperties.put("tasLoginPage", "login.asp");
			tasProperties.put("tasServerScriptsDirectory", "Java");
			tasProperties.put("tasVirtualServer", tasVirtualServer);

			auth = new Authentication(tasProperties);
			authentications.put(name, auth);
			Engine.logEngine.debug("Creation of the Authentication object for the Carioca virtual server \""
					+ (tasVirtualServer.equals("") ? "carioca" : tasVirtualServer) + "\".");
		} else {
			Engine.logEngine.debug("Retrieving the Authentication object for the Carioca virtual server \""
					+ (tasVirtualServer.equals("") ? "carioca" : tasVirtualServer) + "\".");
		}

		return auth;
	}

	public static ObjectsProvider objectsProvider = null;

	public static void setObjectsProvider(ObjectsProvider objectsProvider) {
		Engine.objectsProvider = objectsProvider;
	}

	/**
	 * Retrieves the XML document according to the given context.
	 * 
	 * @param requester
	 *            the calling requester.
	 * @param context
	 *            the request context.
	 * 
	 * @return the generated XML document.
	 */
	public Document getDocument(Requester requester, Context context) throws EngineException {
		Document outputDom = null;

		String t = context.statistics.start(EngineStatistics.GET_DOCUMENT);

		try {
			Engine.logContext.trace("Engine.getDocument: started");

			// Are we in the studio context?
			if (isStudioMode()) {
				Engine.logContext.debug("The requested object will be processed in the studio context.");
			}

			// Checking whether the asynchronous mode has been requested.
			if ((context.isAsync) && (JobManager.jobExists(context.contextID))) {
				Engine.logContext
						.debug("The requested object is working and is asynchronous; requesting job status...");

				HttpServletRequest request = (HttpServletRequest) requester.inputData;
				if (request.getParameter(Parameter.Abort.getName()) != null) {
					Engine.logContext.debug("Job abortion has been required");
					return JobManager.abortJob(context.contextID);
				}
				return JobManager.getJobStatus(context.contextID);
			}

			// Loading project
			if (context.projectName == null)
				throw new EngineException("The project name has been specified!");
            // Checking whether the asynchronous mode has been requested.
            if ((context.isAsync) && (JobManager.jobExists(context.contextID))) {
            	Engine.logContext.debug("The requested object is working and is asynchronous; requesting job status...");
            	
            	HttpServletRequest request = (HttpServletRequest)requester.inputData;
            	if (request.getParameter(Parameter.Abort.getName()) != null) {
            		Engine.logContext.debug("Job abortion has been required");
            		return JobManager.abortJob(context.contextID);
            	}
            	return JobManager.getJobStatus(context.contextID);
            }
            
            // Loading project
            if (context.projectName == null) throw new EngineException("The project name has been specified!");

			Project currentProject;
			if (isStudioMode()) {
				currentProject = objectsProvider.getProject(context.projectName);
				if (currentProject == null) {
					throw new EngineException(
							"No project has been opened in the Studio. A project should be opened in the Studio in order that the Convertigo engine can work.");
				} else if (!currentProject.getName().equalsIgnoreCase(context.projectName)) {
					throw new EngineException(
							"The requested project (\""
									+ context.projectName
									+ "\") does not match with the opened project (\""
									+ currentProject.getName()
									+ "\") in the Studio.\nYou cannot make a request on a different project than the one opened in the Studio.");
				}
				Engine.logContext.debug("Using project from Studio");
				context.project = currentProject;
			} else {
				if ((context.project == null) || (context.isNewSession)) {
					Engine.logEngine.debug("New project requested: '" + context.projectName + "'");
					context.project = Engine.theApp.databaseObjectsManager
							.getProjectByName(context.projectName);
					Engine.logContext.debug("Project loaded: " + context.project.getName());
				}
			}

			// Loading sequence
			if (context.sequenceName != null) {
				context.loadSequence();
			} else {
				// Loading connector
				context.loadConnector();

				// Loading transaction
				// Load default transaction if no overidden transaction is
				// provided
				if (context.transactionName == null) {
					context.requestedObject = context.getConnector().getDefaultTransaction();
					context.transaction = (Transaction) context.requestedObject;
					context.transactionName = context.requestedObject.getName();
					Engine.logContext.debug("Default transaction loaded: " + context.transactionName);
				}
				// Try to load overriden transaction
				else {
					context.requestedObject = context.getConnector().getTransactionByName(
							context.transactionName);
					context.transaction = (Transaction) context.requestedObject;
					if (context.requestedObject == null) {
						throw new EngineException("Unknown transaction \"" + context.transactionName + "\"");
					}
					Engine.logContext.debug("Transaction loaded: " + context.requestedObject.getName());
				}

				if (context.getConnector().isTasAuthenticationRequired()) {
					if (context.tasSessionKey == null) {
						throw new EngineException(
								"A Carioca authentication is required in order to process the transaction.");
					} else {
						// Checking VIC information if needed
						if (context.isRequestFromVic) {
							Engine.logContext.debug("[Engine.getDocument()] Checking VIC session key");

							String s = Crypto2.decodeFromHexString(context.tasSessionKey);
							int i = s.indexOf(',');
							if (i == -1)
								throw new EngineException(
										"Unable to decrypt the VIC session key (reason: #1)!");

							try {
								long t0 = Long.parseLong(s.substring(0, i));
								Engine.logContext.debug("[VIC key check] t0=" + t0);
								long t1 = System.currentTimeMillis();
								Engine.logContext.debug("[VIC key check] t1=" + t1);
								long d = Math.abs(t1 - t0);
								Engine.logContext.debug("[VIC key check] d=" + d);

								String user = s.substring(i + 1);
								Engine.logContext.debug("[VIC key check] user: " + user);

								long deltaT = 1000 * 60 * 10;
								if (d > deltaT)
									throw new EngineException("The VIC session key has expired.");
								if (!user.equals(context.tasUserName))
									throw new EngineException("Wrong user name!");
							} catch (NumberFormatException e) {
								throw new EngineException(
										"Unable to decrypt the VIC session key (reason: #2)!");
							}
							Engine.logContext.debug("[VIC key check] VIC session key OK");
						}
						// Checking Carioca session key
						else if (context.isTrustedRequest) {
							if (!context.tasSessionKeyVerified) {
								Engine.logContext.debug("[Engine.getDocument()] Checking Carioca session key");

								TWSKey twsKey = new TWSKey();
								twsKey.CreateKey(1);
								int cariocaSessionKeyLifeTime = 60;
								try {
									cariocaSessionKeyLifeTime = Integer
											.parseInt(EnginePropertiesManager
													.getProperty(EnginePropertiesManager.PropertyName.CARIOCA_SESSION_KEY_LIFE_TIME));
								} catch (NumberFormatException e) {
									Engine.logContext
											.warn("The Carioca session key life time value is not valid (not a number)! Setting default to 60s.");
								}
								Engine.logContext.debug("Carioca session key lifetime: "
										+ cariocaSessionKeyLifeTime + " second(s)");

								String result = checkCariocaSessionKey(context, context.tasSessionKey,
										context.tasServiceCode, 0, cariocaSessionKeyLifeTime);
								if (result != null)
									throw new EngineException(result);

								Engine.logContext.debug("[ContextManager] Carioca session key OK");
								context.tasSessionKeyVerified = true;
							}
						}
					}
				}

			}

			// Check requestable accessibility
			requester.checkAccessibility();

			// Check requestable access policy
			requester.checkSecuredConnection();
			
			// Check authenticated context requirement 
		 	requester.checkAuthenticatedContext(); 
			
		 	RequestableObject requestedObject = context.requestedObject;

			if (context.isAsync) {
				outputDom = JobManager.addJob(cacheManager, requestedObject, requester, context);
			} else {
				outputDom = cacheManager.getDocument(requester, context);
			}

			Element documentElement = outputDom.getDocumentElement();
			documentElement.setAttribute("version", Version.fullProductVersion);
			documentElement.setAttribute("context", context.name);
			documentElement.setAttribute("contextId", context.contextID);

			DatabaseObject lastDetectedObject = (DatabaseObject) context.lastDetectedObject;
			if (lastDetectedObject != null) {
				documentElement.setAttribute("screenclass", lastDetectedObject.getName());
				// TODO :
				// documentElement.setAttribute(lastDetectedObject.getClass().getName().toLowerCase(),
				// lastDetectedObject.getName());
			}

			context.documentSignatureSent = System.currentTimeMillis();
			documentElement.setAttribute("signature", Long.toString(context.documentSignatureSent));

			// Add the user reference if any
			if (context.userReference != null) {
				documentElement.setAttribute("userReference", context.userReference);
			}

			fireDocumentGenerated(new EngineEvent(outputDom));
		} catch (EngineException e) {
			String message = "[Engine.getDocument()] Context ID#" + context.contextID
					+ " - Unable to build the XML document.";
			message += "\n[" + e.getClass().getName() + "] " + e.getMessage();
			if (System.getProperty("java.specification.version").compareTo("1.4") >= 0) {
				Throwable eCause = e;
				while ((eCause = eCause.getCause()) != null) {
					if (!(eCause instanceof ConvertigoException)) {
						message += "\n" + Log.getStackTrace(eCause);
					} else {
						message += "\n[" + eCause.getClass().getName() + "] " + eCause.getMessage();
					}
				}
			}
			Engine.logContext.error(message);

			// Just re-throw the exception
			throw (EngineException) e;
		} catch (Throwable e) {
			Engine.logEngine.error("Context ID#" + context.contextID
					+ " - An unexpected error has occured while building the XML document.", e);

			throw new EngineException("An unexpected error has occured while building the XML document."
					+ "Please contact Convertigo support, providing the following information:", e);
		} finally {
			if (context.requestedObject != null) {
				Engine.logContext.debug("Requested object is billable: "
						+ context.requestedObject.isBillable());
				if (context.requestedObject.isBillable()) {
					// Fix regression for Teamlog's billing abortion!
					// In case of exception thrown, outputDom is Null so use
					// context.outputDocument!
					if (context.outputDocument == null) {
						Engine.logContext.warn("Billing aborted because the generated XML document is null");
					} else {
						String billingClassName = context.getConnector().getBillingClassName();
						try {
							Engine.logContext.debug("Billing class name required: " + billingClassName);
							AbstractBiller biller = (AbstractBiller) Class.forName(billingClassName)
									.newInstance();
							Engine.logContext.debug("Executing the biller");
							biller.insertBilling(context);
						} catch (Throwable e) {
							Engine.logContext
									.warn("Unable to execute the biller (the billing is thus ignored): ["
											+ e.getClass().getName() + "] " + e.getMessage());
						}
					}
				}
			}

			context.statistics.stop(t);

			if (context.requestedObject != null) {
				try {
					Engine.theApp.billingManager.insertBilling(context);
				} catch (Exception e) {
					Engine.logContext.warn("Unable to insert billing ticket (the billing is thus ignored): ["
							+ e.getClass().getName() + "] " + e.getMessage());
				}
			}

			Engine.logContext.trace("Engine.getDocument: finished");
		}

		XMLUtils.logXml(outputDom, Engine.logContext, "Generated XML");

		return outputDom;
	}

	public String checkCariocaSessionKey(Context context, String sKey, String sServiceCode, long idUser,
			int iDelta) {
		String sSplit = null;

		if (sKey == null)
			return "Carioca session key is null!";

		sSplit = Crypto2.decodeFromHexString(sKey);
		if (sSplit == null)
			return "Unable to decode the Carioca session key!";

		long d;
		long i;
		String v;
		StringTokenizer strtok = new StringTokenizer(sSplit, ";");

		if (strtok.hasMoreTokens()) {
			d = Long.valueOf(strtok.nextToken()).longValue();
			Engine.logContext.debug("Engine.checkCariocaSessionKey() d=" + d);
			long c = System.currentTimeMillis();
			Engine.logContext.debug("Engine.checkCariocaSessionKey() c=" + c);
			if (strtok.hasMoreTokens()) {
				i = Long.valueOf(strtok.nextToken()).longValue();
				Engine.logContext.debug("Engine.checkCariocaSessionKey() i=" + i);
				if (strtok.hasMoreTokens()) {
					v = strtok.nextToken();
					Engine.logContext.debug("Engine.checkCariocaSessionKey() v=" + v);
					if (v.equals("#"))
						v = new String("");

					// time expired
					// if ((d > System.currentTimeMillis()) ||
					// (System.currentTimeMillis() > (d+(iDelta*1000))))
					Engine.logContext.debug("Engine.checkCariocaSessionKey() Math.abs(c - d)="
							+ Math.abs(c - d));
					if (Math.abs(c - d) > iDelta * 1000)
						return "The Carioca session key has expired!";

					// check service service
					if (sServiceCode.compareTo(v) != 0)
						return "The service code does not match! (expected service code: " + sServiceCode
								+ ")";

					if ((idUser != 0) && (idUser != i))
						return "The user id does not match!";

					return null;
				}
			}
		}

		return "Wrong Carioca session key format";
	}

	public Document getErrorDocument(Throwable e, Requester requester, Context context) throws Exception {
		// Generate the XML document for error
		Document document = requester.createDOM("UTF-8");
		context.outputDocument = document;

		Text text;

		Element doc = document.createElement("document");
		document.appendChild(doc);
		
		Element error = document.createElement("error");
		doc.appendChild(error);
		
		if ( EnginePropertiesManager.getProperty( PropertyName.HIDING_ERROR_INFORMATION ).equals( "false" ) ){
			error.setAttribute("project", (context.projectName == null ? "?" : context.projectName));
			error.setAttribute("connector", (context.connectorName == null ? "?" : context.connectorName));
			error.setAttribute("transaction", (context.transactionName == null ? "?" : context.transactionName));
		
			Element econtext = document.createElement("context");
			for (String key : context.keys()) {
				Object value = context.get(key);
				if ((value != null) && (value instanceof String)) {
					Element variable = document.createElement("variable");
					variable.setAttribute("name", key);
					variable.setAttribute("value", (String) value);
					econtext.appendChild(variable);
				}
			}
	
			error.appendChild(econtext);
	
			Element exception = document.createElement("exception");
			text = document.createTextNode(e.getClass().getName());
			exception.appendChild(text);
			error.appendChild(exception);
	
			Element message = document.createElement("message");
			text = document.createTextNode(e.getMessage());
			message.appendChild(text);
			error.appendChild(message);
	
			Element stackTrace = document.createElement("stacktrace");
			String jss = Log.getStackTrace(e);
			jss = jss.replace('\r', ' ');
			text = document.createTextNode(jss);
			stackTrace.appendChild(text);
			error.appendChild(stackTrace);
	
			fireDocumentGenerated(new EngineEvent(context.outputDocument));
		}
		return document;
	}

	public static String getExceptionSchema() {
		String exceptionSchema = "";
		exceptionSchema += "  <xsd:complexType name=\"ConvertigoError\">\n";
		exceptionSchema += "    <xsd:sequence>\n";
		exceptionSchema += "      <xsd:element name=\"context\" type=\"p_ns:ConvertigoErrorContext\" />\n";
		exceptionSchema += "      <xsd:element name=\"exception\" type=\"xsd:string\" />\n";
		exceptionSchema += "      <xsd:element name=\"message\" type=\"xsd:string\" />\n";
		exceptionSchema += "      <xsd:element name=\"stacktrace\" type=\"xsd:string\" />\n";
		exceptionSchema += "    </xsd:sequence>\n";
		exceptionSchema += "  </xsd:complexType>\n";
		exceptionSchema += "  <xsd:complexType name=\"ConvertigoErrorContext\">\n";
		exceptionSchema += "    <xsd:sequence>\n";
		exceptionSchema += "      <xsd:element name=\"variable\" maxOccurs=\"unbounded\" minOccurs=\"0\" type=\"p_ns:ConvertigoErrorContextVariable\" />\n";
		exceptionSchema += "    </xsd:sequence>\n";
		exceptionSchema += "  </xsd:complexType>\n";
		exceptionSchema += "  <xsd:complexType name=\"ConvertigoErrorContextVariable\">\n";
		exceptionSchema += "    <xsd:attribute name=\"name\" type=\"xsd:string\" />\n";
		exceptionSchema += "    <xsd:attribute name=\"value\" type=\"xsd:string\" />\n";
		exceptionSchema += "  </xsd:complexType>\n";
		return exceptionSchema;
	}

	public static String getArrayOfSchema(String schemaType) {
		String arraySchema = "";
		arraySchema += "<xsd:complexType name=\"ArrayOf_" + schemaType.replaceAll(":", "_") + "\">\n";
		arraySchema += "    <xsd:complexContent>\n";
		arraySchema += "        <xsd:restriction base=\"soapenc:Array\">\n";
		arraySchema += "            <xsd:attribute ref=\"soapenc:arrayType\" wsdl:arrayType=\"" + schemaType
				+ "[]\"/>\n";
		arraySchema += "        </xsd:restriction>\n";
		arraySchema += "    </xsd:complexContent>\n";
		arraySchema += "</xsd:complexType>\n";
		return arraySchema;
	}

	private EventListenerList migrationListeners = new EventListenerList();

	public void addMigrationListener(MigrationListener migrationListener) {
		migrationListeners.add(MigrationListener.class, migrationListener);
	}

	public void removeMigrationListener(MigrationListener migrationListener) {
		migrationListeners.remove(MigrationListener.class, migrationListener);
	}

	public void fireProjectMigrated(EngineEvent engineEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = migrationListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == MigrationListener.class) {
				((MigrationListener) listeners[i + 1]).projectMigrated(engineEvent);
			}
		}
	}

	public void fireMigrationFinished(EngineEvent engineEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = migrationListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == MigrationListener.class) {
				((MigrationListener) listeners[i + 1]).migrationFinished(engineEvent);
			}
		}
	}

	private EventListenerList engineListeners = new EventListenerList();

	public void addEngineListener(EngineListener engineListener) {
		engineListeners.add(EngineListener.class, engineListener);
	}

	public void removeEngineListener(EngineListener engineListener) {
		engineListeners.remove(EngineListener.class, engineListener);
	}

	public void fireBlocksChanged(EngineEvent engineEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = engineListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == EngineListener.class) {
				((EngineListener) listeners[i + 1]).blocksChanged(engineEvent);
			}
		}
	}

	public void fireObjectDetected(EngineEvent engineEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = engineListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == EngineListener.class) {
				((EngineListener) listeners[i + 1]).objectDetected(engineEvent);
			}
		}
	}

	public void fireDocumentGenerated(EngineEvent engineEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = engineListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == EngineListener.class) {
				((EngineListener) listeners[i + 1]).documentGenerated(engineEvent);
			}
		}
	}

	public void fireStepReached(EngineEvent engineEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = engineListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == EngineListener.class) {
				((EngineListener) listeners[i + 1]).stepReached(engineEvent);
			}
		}
	}

	public void fireTransactionStarted(EngineEvent engineEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = engineListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == EngineListener.class) {
				((EngineListener) listeners[i + 1]).transactionStarted(engineEvent);
			}
		}
	}

	public void fireTransactionFinished(EngineEvent engineEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = engineListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == EngineListener.class) {
				((EngineListener) listeners[i + 1]).transactionFinished(engineEvent);
			}
		}
	}

	public void fireSequenceStarted(EngineEvent engineEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = engineListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == EngineListener.class) {
				((EngineListener) listeners[i + 1]).sequenceStarted(engineEvent);
			}
		}
	}

	public void fireSequenceFinished(EngineEvent engineEvent) {
		// Guaranteed to return a non-null array
		Object[] listeners = engineListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == EngineListener.class) {
				((EngineListener) listeners[i + 1]).sequenceFinished(engineEvent);
			}
		}
	}

	public static boolean isCloudMode() {
		return cloud_customer_name != null;
	}

	public static boolean isEngineMode() {
		return !Engine.isStudioMode();
	}

	public static boolean isStudioMode() {
		return bStudioMode;
	}

	public static void setStudioMode() {
		Engine.bStudioMode = true;
	}

	public static boolean isLinux() {
		return System.getProperty("os.name").indexOf("Linux") != -1;
	}
}