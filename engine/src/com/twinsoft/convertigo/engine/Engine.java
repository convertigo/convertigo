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

package com.twinsoft.convertigo.engine;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.security.Provider;
import java.security.Security;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.swing.event.EventListenerList;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.twinsoft.api.SessionManager;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.cache.CacheManager;
import com.twinsoft.convertigo.engine.dbo_explorer.DboExplorerManager;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.mobile.ComponentRefManager;
import com.twinsoft.convertigo.engine.mobile.ComponentRefManager.Mode;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbManager;
import com.twinsoft.convertigo.engine.providers.sapjco.SapJcoDestinationDataProvider;
import com.twinsoft.convertigo.engine.requesters.HttpSessionListener;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.scheduler.SchedulerManager;
import com.twinsoft.convertigo.engine.servlets.DelegateServlet;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.Crypto2;
import com.twinsoft.convertigo.engine.util.DirClassLoader;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils.HttpClientInterface;
import com.twinsoft.convertigo.engine.util.LogCleaner;
import com.twinsoft.convertigo.engine.util.LogWrapper;
import com.twinsoft.convertigo.engine.util.RhinoUtils;
import com.twinsoft.convertigo.engine.util.SimpleMap;
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
	public static String LOG_ENGINE_NAME = "";
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
		
		System.setProperty("log4j1.compatibility", "true");
		System.setProperty("log4j2.isThreadContextMapInheritable", "true");
		RhinoUtils.init();
	}
	/**
	 * This is the application reference.
	 */
	public static Engine theApp;

	private final static ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setName("ConvertigoExecutor-" + thread.getId());
			thread.setDaemon(true);
			return thread;
		}
	});

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

	static boolean bCliMode = false;

	/**
	 * The database objects manager.
	 */
	public DatabaseObjectsManager databaseObjectsManager;
	private SystemDatabaseObjectsManager systemDatabaseObjectsManager;

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
	 * The cache manager.
	 */
	public CacheManager cacheManager;

	/**
	 * The usage monitor.
	 */
	public UsageMonitor usageMonitor;

	/**
	 * The biller token manager
	 */
	public BillerTokenManager billerTokenManager;

	/**
	 * The billing manager
	 */
	public BillingManager billingManager;

	/**
	 * The proxy manager
	 */
	public ProxyManager proxyManager;


	/**
	 * The schema manager
	 */
	public SchemaManager schemaManager;

	/**
	 * The resource compressor manager for minification
	 */
	public MinificationManager minificationManager;

	/**
	 * The plugins manager
	 */
	public PluginsManager pluginsManager;

	/**
	 * The CouchDb manager
	 */
	public CouchDbManager couchDbManager;

	/**
	 * The REST api manager
	 */
	public RestApiManager restApiManager;

	public ReferencedProjectManager referencedProjectManager;
	
	public ReverseProxyManager reverseProxyManager;
	/**
	 * Loggers
	 */
	public static Logger logAdmin;
	public static Logger logAudit;
	public static Logger logBeans;
	public static Logger logBillers;
	public static Logger logCacheManager;
	public static Logger logCertificateManager;
	public static Logger logContext;
	public static Logger logContextManager;
	public static Logger logConvertigo;
	public static Logger logCouchDbManager;
	public static Logger logDatabaseObjectManager;
	public static Logger logDevices;
	public static Logger logEmulators;
	public static Logger logEngine;
	public static Logger logJobManager;
	public static Logger logProxyManager;
	public static Logger logScheduler;
	public static Logger logSecurityFilter;
	public static Logger logSecurityTokenManager;
	public static Logger logSiteClipper;
	public static Logger logStatistics;
	public static Logger logStudio;
	public static Logger logTracePlayerManager;
	public static Logger logUsageMonitor;
	public static Logger logUser;

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

	private static ClassLoader engineClassLoader;

	/**
	 * The scheduler for running jobs.
	 */
	public SchedulerManager schedulerManager;

	/**
	 * The event manager for dispatching events.
	 */
	public EventManager eventManager;

	public HttpClient httpClient;

	public HttpClientInterface httpClient4;

	public RsaManager rsaManager;

	private SimpleMap sharedServerMap = new SimpleMap();
	private Map<String, SimpleMap> sharedProjectMap = new HashMap<>();

	static {
		try {
			Engine.authenticatedSessionManager = new AuthenticatedSessionManager();
			Engine.authenticatedSessionManager.init();
		} catch (EngineException e) {
			e.printStackTrace();
		}

		cloud_customer_name = System.getProperty("convertigo.cloud.customer_name");

		AuthPolicy.registerAuthScheme(AuthPolicy.NTLM, com.jivesoftware.authHelper.customescheme.ntlm2.CustomNTLM2Scheme.class);
	}

	private static boolean bInitPathsDone = false;
	private static ServletContext servletContext = null;

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

		new File(Engine.USER_WORKSPACE_PATH + "/libs/classes").mkdirs();
		engineClassLoader = new DirClassLoader(new File(Engine.USER_WORKSPACE_PATH + "/libs"), Engine.class.getClassLoader());

		bInitPathsDone = true;
	}

	public static void initServletContext(ServletContext servletContext) {
		if (Engine.servletContext == null) {
			Engine.servletContext  = servletContext;
		}
	}

	public static synchronized void start() throws EngineException {
		if (Engine.theApp == null) {
			System.out.println("Starting Convertigo Enterprise Mobility Server");
			System.out.println("Version: " + ProductVersion.fullProductVersion);

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
			Engine.logSecurityFilter = Logger.getLogger("cems.SecurityFilter");
			Engine.logStudio = Logger.getLogger("cems.Studio");
			Engine.logAudit = Logger.getLogger("cems.Context.Audit");

			// Managers
			Engine.logContextManager = Logger.getLogger("cems.ContextManager");
			Engine.logCacheManager = Logger.getLogger("cems.CacheManager");
			Engine.logTracePlayerManager = Logger.getLogger("cems.TracePlayerManager");
			Engine.logJobManager = Logger.getLogger("cems.JobManager");
			Engine.logCertificateManager = Logger.getLogger("cems.CertificateManager");
			Engine.logDatabaseObjectManager = Logger.getLogger("cems.DatabaseObjectManager");
			Engine.logProxyManager = Logger.getLogger("cems.ProxyManager");
			Engine.logDevices = Logger.getLogger("cems.Devices");
			Engine.logCouchDbManager = Logger.getLogger("cems.CouchDbManager");
			Engine.logSecurityTokenManager = Logger.getLogger("cems.SecurityTokenManager");

			// Logger for compatibility issues
			Engine.log = new LogWrapper(Engine.logConvertigo);
			LogWrapper.initWrapper(Engine.logEmulators);

			LogCleaner.start();

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
				Engine.theApp.databaseObjectsManager.addDatabaseObjectListener(ComponentRefManager.get(Mode.start));

				Engine.theApp.systemDatabaseObjectsManager = new SystemDatabaseObjectsManager();
				Engine.theApp.systemDatabaseObjectsManager.init();

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
					Engine.theApp.billerTokenManager = new BillerTokenManager();
					Engine.theApp.billerTokenManager.init();
				} catch (Exception e) {
					Engine.logEngine.error("Unable to launch the biller token manager.", e);
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
					Engine.theApp.minificationManager = new MinificationManager();
					Engine.theApp.minificationManager.init();
				} catch (Exception e) {
					Engine.logEngine.error("Unable to run the minification manager.", e);
				}

				try {
					Engine.theApp.couchDbManager = new CouchDbManager();
					Engine.theApp.couchDbManager.init();
				} catch (Exception e) {
					Engine.logEngine.error("Unable to run the couchDbProxy manager.", e);
				}

				try {
					Engine.theApp.pluginsManager = new PluginsManager();
					Engine.theApp.pluginsManager.init();
				} catch (Exception e) {
					Engine.logEngine.error("Unable to run the plugins manager.", e);
				}

				try {
					Engine.theApp.restApiManager = RestApiManager.getInstance();
					Engine.theApp.restApiManager.init();
				} catch (Exception e) {
					Engine.logEngine.error("Unable to run the rest api manager.", e);
				}

				Engine.logEngine.info("Current working directory is '" + System.getProperty("user.dir") + "'.");

				// Creating the Carioca Authentication objects
				Engine.logEngine.debug("Creating the Carioca Authentication objects");
				String cariocaUserName = EnginePropertiesManager
						.getProperty(PropertyName.CARIOCA_DEFAULT_USER_NAME);
				String cariocaPassword = EnginePropertiesManager
						.getProperty(PropertyName.CARIOCA_DEFAULT_USER_PASSWORD);
				Engine.theApp.authentications = new HashMap<String, Authentication>();

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
							.getConstructor().newInstance();
					Engine.theApp.cacheManager.init();
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

				// Initialize the HttpClient
				try {
					Engine.logEngine.debug("HttpClient initializing...");

					HttpMethodParams.getDefaultParams().setParameter(HttpMethodParams.CREDENTIAL_CHARSET, "UTF-8");
					Engine.theApp.httpClient = HttpUtils.makeHttpClient3(true);
					Engine.theApp.httpClient4 = HttpUtils.makeHttpClient(true);

					Engine.logEngine.debug("HttpClient initialized!");
				} catch (Exception e) {
					Engine.logEngine.error("Unable to initialize the HttpClient.", e);
				}

				// Initialization of the schedule manager
				Engine.theApp.schedulerManager = new SchedulerManager(true);

				// Initialization of the RSA manager
				Engine.theApp.rsaManager = new RsaManager();
				Engine.theApp.rsaManager.init();

				// Initialization of the Schema manager
				Engine.theApp.schemaManager = new SchemaManager();
				Engine.theApp.schemaManager.init();

				Engine.theApp.referencedProjectManager = new ReferencedProjectManager();

				Engine.theApp.reverseProxyManager = new ReverseProxyManager();

				isStarted = true;

				Engine.logEngine.info("Convertigo engine started");

				if (Engine.isEngineMode()) {
					theApp.addMigrationListener(new MigrationListener() {

						@Override
						public void projectMigrated(EngineEvent engineEvent) {
						}

						@Override
						public void migrationFinished(EngineEvent engineEvent) {
							List<String> names = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
							Engine.logEngine.info("Convertigo engine will load: " + names);
							for (String name: names) {
								try {
									Engine.theApp.databaseObjectsManager.getProjectByName(name);
								} catch (Exception e) {
									Engine.logEngine.error("Failed to load " + name, e);
								}
							}
							boolean newProjectLoaded = Engine.theApp.referencedProjectManager.check();

							if (!newProjectLoaded && Thread.currentThread().getName().equalsIgnoreCase("Migration")) {
								Engine.logEngine.info("Convertigo will run auto start Sequences.");
								for (String name: names) {
									Project.executeAutoStartSequences(name);
								}
							}
						}
					});
				}

				if (DelegateServlet.canDelegate()) {
					execute(() -> {
						try {
							Engine.logEngine.info("Call delegate action 'engineStarted'");
							JSONObject json = new JSONObject();
							json.put("action", "engineStarted");
							DelegateServlet.delegate(json);
						} catch (JSONException e) {
						}
					});
				}
			} catch (Throwable e) {
				isStartFailed = true;
				Engine.logEngine.error("Unable to successfully start the engine.", e);
			}
		} else {
			Engine.logEngine.info("Convertigo engine already started");
		}
	}

	public static synchronized void stop() throws EngineException {
		if (Engine.isStudioMode() && isStarted) {
			throw new EngineException("Cannot stop a Convertigo Studio Engine");
		}
		if (Engine.theApp != null) {
			if (!MigrationManager.isMigrationFinished())
				throw new EngineException("Migration process of projects is still running.");

			try {
				Engine.logEngine.info("Stopping the engine");

				// Temporary reset the start/stop date in order to unlink the requestable's
				// running thread engine ID.
				Engine.startStopDate = 0;

				if (Engine.theApp.contextManager != null) {
					Engine.logEngine.info("Removing all contexts");
					Engine.theApp.contextManager.destroy();
				}

				Engine.logEngine.info("Resetting statistics");
				EngineStatistics.reset();

				if (Engine.theApp.usageMonitor != null) {
					Engine.logEngine.info("Removing the usage monitor");
					Engine.theApp.usageMonitor.destroy();
				}

				if (Engine.theApp.sqlConnectionManager != null) {
					Engine.logEngine.info("Removing the SQL connections manager");
					Engine.theApp.sqlConnectionManager.destroy();
				}

				if (Engine.theApp.filePropertyManager != null) {
					Engine.logEngine.info("Removing the file property manager");
					Engine.theApp.filePropertyManager.destroy();
				}

				if (Engine.theApp.billingManager != null) {
					Engine.logEngine.info("Removing the billing manager");
					Engine.theApp.billingManager.destroy();
				}

				if (Engine.theApp.tracePlayerManager != null) {
					Engine.logEngine.info("Removing the TracePlayer manager");
					Engine.theApp.tracePlayerManager.destroy();
				}

				if (Engine.theApp.cacheManager != null) {
					Engine.logEngine.info("Removing the cache manager");
					Engine.theApp.cacheManager.destroy();
				}

				if (Engine.theApp.proxyManager != null) {
					Engine.logEngine.info("Removing the proxy manager");
					Engine.theApp.proxyManager.destroy();
				}

				if (Engine.theApp.minificationManager != null) {
					Engine.logEngine.info("Removing the minification manager");
					Engine.theApp.minificationManager.destroy();
				}

				if (Engine.theApp.couchDbManager != null) {
					Engine.logEngine.info("Removing the couchdb manager");
					Engine.theApp.couchDbManager.destroy();
				}

				if (Engine.theApp.rsaManager != null) {
					Engine.logEngine.info("Removing the rsa manager");
					Engine.theApp.rsaManager.destroy();
				}

				if (Engine.theApp.schemaManager != null) {
					Engine.logEngine.info("Removing the schema manager");
					Engine.theApp.schemaManager.destroy();
				}

				if (Engine.theApp.pluginsManager != null) {
					Engine.logEngine.info("Removing the plugins manager");
					Engine.theApp.pluginsManager.destroy();
				}

				if (Engine.theApp.restApiManager != null) {
					Engine.logEngine.info("Removing the rest api manager");
					Engine.theApp.restApiManager.destroy();
				}

				// Closing the session manager
				if (Engine.theApp.sessionManager != null) {
					Engine.logEngine.info("Closing the session manager");
					Engine.theApp.sessionManager.removeAllSessions();
					Engine.theApp.sessionManager = null;
				}

				Engine.logEngine.info("Resetting the key manager");
				KeyManager.reset();

				if (Engine.theApp.schedulerManager != null) {
					Engine.logEngine.info("Removing the scheduler manager");
					Engine.theApp.schedulerManager.destroy();
				}

				if (Engine.theApp.databaseObjectsManager != null) {
					Engine.logEngine.info("Removing the database objects manager");
					Engine.theApp.databaseObjectsManager.removeDatabaseObjectListener(ComponentRefManager.get(Mode.stop));
					Engine.theApp.databaseObjectsManager.destroy();
				}

				if (Engine.theApp.systemDatabaseObjectsManager != null) {
					Engine.logEngine.info("Removing the system database objects manager");
					Engine.theApp.systemDatabaseObjectsManager.destroy();
				}

				if (Engine.theApp.threadManager != null) {
					Engine.logEngine.info("Removing the thread manager");
					Engine.theApp.threadManager.destroy();
				}

				Engine.logEngine.info("Unregistering the SAP destination provider");
				try {
					SapJcoDestinationDataProvider.destroy();
				}
				catch (NoClassDefFoundError e) {
					// java.lang.NoClassDefFoundError: com/sap/conn/jco/ext/DestinationDataProvider
				}
				catch (Throwable e) {
					Engine.logEngine.error("Error while unregistering SAP destination provider", e);
				}

				HttpSessionListener.removeAllSession();

				Engine.logEngine.info("The Convertigo Engine has been successfully stopped.");
			} finally {
				Engine.startStopDate = System.currentTimeMillis();

				if (Engine.theApp.eventManager != null) {
					Engine.logEngine.info("Removing the event manager");
					Engine.theApp.eventManager.destroy();
				}
				Engine.theApp.eventManager = null;

				LogCleaner.stop();

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
				// TODO: add all connectors already loaded in contexts
			} else if (connectorsMonitor != null) {
				connectorsMonitor.setVisible(false);
			}
		}
	}

	public boolean isMonitored() {
		return bMonitored;
	}

	private DboExplorerManager dboExplorerManager = null;

	public synchronized DboExplorerManager getDboExplorerManager() throws SAXException, IOException, ParserConfigurationException {
		if (dboExplorerManager == null) {
			dboExplorerManager = new DboExplorerManager();
		}
		return dboExplorerManager;
	};

	/**
	 * Constructs a new Engine object.
	 */
	public Engine() {
		Engine.logEngine.info("===========================================================");
		Engine.logEngine.info(" Convertigo Enterprise Mobility Server "
				+ com.twinsoft.convertigo.engine.Version.fullProductVersion);
		Engine.logEngine.info("    engine " + Version.version);
		Engine.logEngine.info("    beans " + com.twinsoft.convertigo.beans.Version.version);
		Engine.logEngine.info("===========================================================");

		Engine.logEngine.info("Start date: " + (new Date(startStopDate)).toString());
		Engine.logEngine.info("Class loader: " + engineClassLoader);
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
			if ("system".equals(context.contextID)) {
				context.project = currentProject = systemDatabaseObjectsManager.getOriginalProjectByName(context.getProjectName());
			} else if (isStudioMode()) {
				if (objectsProvider == null) {
					throw new EngineException(
							"Is the Projects view opened in the Studio? Failed to load: " + context.projectName);
				}
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
			context.project.checkSymbols();

			if (context.httpServletRequest != null && !RequestAttribute.corsOrigin.has(context.httpServletRequest)) {
				String origin = HeaderName.Origin.getHeader(context.httpServletRequest);
				String corsOrigin = HttpUtils.filterCorsOrigin(context.project.getCorsOrigin(), origin);
				if (corsOrigin != null) {
					context.setResponseHeader(HeaderName.AccessControlAllowOrigin.value(), corsOrigin);
					context.setResponseHeader(HeaderName.AccessControlAllowCredentials.value(), "true");
					Engine.logContext.trace("Add CORS header for: " + corsOrigin);
				}
				RequestAttribute.corsOrigin.set(context.httpServletRequest, corsOrigin == null ? "" : corsOrigin);
			}

			// Loading sequence
			if (context.sequenceName != null) {

				context.loadSequence();
			} else {
				// Loading connector
				context.loadConnector();

				// Loading transaction
				// Load default transaction if no overridden transaction is
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
				context.transaction.checkSymbols();

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

			requester.checkParentContext();

			RequestableObject requestedObject = context.requestedObject;

			String contextResponseExpiryDate = (String) context.get(Parameter.ResponseExpiryDate.getName());
			String oldResponseExpiryDate = null;
			if (contextResponseExpiryDate != null) {
				oldResponseExpiryDate = requestedObject.getResponseExpiryDate();
				requestedObject.setResponseExpiryDate(contextResponseExpiryDate);
				context.remove(Parameter.ResponseExpiryDate.getName());
			}

			try {
				if (context.isAsync) {
					outputDom = JobManager.addJob(cacheManager, requestedObject, requester, context);
				} else {
					outputDom = cacheManager.getDocument(requester, context);
				}
			} finally {
				context.remove(Parameter.StubFilename.getName());
				if (oldResponseExpiryDate!=null) {
					requestedObject.setResponseExpiryDate(oldResponseExpiryDate);
				}

				onDocumentRetrieved(context, outputDom);
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
									.getConstructor().newInstance();
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

	private void onDocumentRetrieved(Context context, Document outputDom) {
		if (context.httpSession != null && outputDom != null) {
			Object controller = context.httpSession.getAttribute("customController");
			if (controller != null && controller instanceof CustomController) {
				((CustomController)controller).modifyDocument(context, outputDom);
			}
		}
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
		Document document = buildErrorDocument(e, requester, context);
		context.outputDocument = document;
		fireDocumentGenerated(new RequestableEngineEvent(context.outputDocument, context.projectName,
				context.sequenceName, context.connectorName));

		return document;
	}

	public static Document buildErrorDocument(Throwable e, Requester requester, Context context) throws Exception {
		boolean bHide = EnginePropertiesManager.getProperty(PropertyName.HIDING_ERROR_INFORMATION ).equals("true");
		return ConvertigoError.get(e).buildErrorDocument(requester, context, bHide);
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

	public static ServletContext getServletContext() {
		return servletContext;
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

	public SimpleMap getShareServerMap() {
		return sharedServerMap;
	}

	public SimpleMap getShareProjectMap(Project project) {
		synchronized (sharedProjectMap) {
			SimpleMap map = sharedProjectMap.get(project.getName());
			if (map == null) {
				sharedProjectMap.put(project.getName(), map = new SimpleMap());
			}
			return map;
		}
	}

	public SystemDatabaseObjectsManager getSystemDatabaseObjectsManager() {
		return systemDatabaseObjectsManager;
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

	public static boolean isCliMode() {
		return bCliMode;
	}

	public static void setStudioMode() {
		Engine.bStudioMode = true;
	}

	public static boolean isLinux() {
		return System.getProperty("os.name").toLowerCase().indexOf("nux") != -1;
	}

	public static boolean isMac() {
		return System.getProperty("os.name").toLowerCase().indexOf("mac") != -1;
	}

	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().indexOf("win") != -1;
	}

	public static void execute(Runnable runnable) {
		executor.execute(() -> {
			Thread th = Thread.currentThread();
			String name = th.getName();
			try {
				runnable.run();
			} catch (Throwable t) {
				if (Engine.logEngine != null) {
					Engine.logEngine.trace("Convertigo executor terminated with a throwable.", t);
				} else {
					System.err.println("Convertigo executor terminated with a throwable.\n" + t);
				}
			} finally {
				if (!name.equals(th.getName())) {
					th.setName(name);
				}
			}
		});
	}

	private static Map<Class<? extends Runnable>, boolean[]> executeThreshold = new HashMap<>();
	public static void execute(Runnable runnable, long threshold) {
		synchronized (executeThreshold) {
			boolean[] doit = executeThreshold.get(runnable.getClass());
			if (doit != null) {
				synchronized (doit) {
					doit[0] = false;
					doit.notify();
				}
			}
			boolean[] _doit = {true};
			synchronized (_doit) {
				executeThreshold.put(runnable.getClass(), _doit);
				executor.execute(() -> {
					Thread th = Thread.currentThread();
					String name = th.getName();
					try {
						synchronized (_doit) {
							_doit.notify();
							_doit.wait(threshold);
						}
						if (_doit[0]) {
							synchronized (executeThreshold) {
								executeThreshold.remove(runnable.getClass());
							}
							runnable.run();
						}
					} catch (Throwable t) {
						if (Engine.logEngine != null) {
							Engine.logEngine.trace("Convertigo executor terminated with a throwable.", t);
						} else {
							System.err.println("Convertigo executor terminated with a throwable.\n" + t);
						}
					} finally {
						if (!name.equals(th.getName())) {
							th.setName(name);
						}
					}
				});
				try {
					_doit.wait();
				} catch (Exception e) {
				}
			}
		}
	}

	public static File projectFile(String projectName) {
		File file = DatabaseObjectsManager.studioProjects.getProject(projectName);
		if (file == null) {
			File f = new File(Engine.PROJECTS_PATH,  projectName);
			if (f.exists() && f.isFile()) {
				try {
					f = new File(FileUtils.readFileToString(f, "UTF-8"));
					file = new File(f, projectName + ".xml");
				} catch (IOException e) {
				}
			}
		}
		if (file == null) {
			file = new File(Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".xml");
		}
		if (!file.exists() && file.getName().endsWith(".xml")) {
			file = new File(file.getParentFile(), "c8oProject.yaml");
		}
		return file;
	}

	public static File projectYamlFile(String projectName) {
		File file = DatabaseObjectsManager.studioProjects.getProject(projectName);
		if (file == null) {
			File f = new File(Engine.PROJECTS_PATH,  projectName);
			if (f.exists() && f.isFile()) {
				try {
					f = new File(FileUtils.readFileToString(f, "UTF-8"));
					file = new File(f, "c8oProject.yaml");
				} catch (IOException e) {
				}
			}
		}
		if (file == null) {
			file = new File(Engine.PROJECTS_PATH + "/" + projectName + "/c8oProject.yaml");
		}
		return file;
	}

	public static String projectDir(String projectName) {
		File file = projectFile(projectName).getParentFile();
		try {
			return file.getCanonicalPath();
		} catch (IOException e) {
			return file.getAbsolutePath();
		}
	}

	public static String resolveProjectPath(String path) {
		File file = new File(path);
		file = resolveProjectPath(file);
		try {
			path = file.getCanonicalPath();
		} catch (IOException e) {
			path = file.getAbsolutePath();
		}
		return path;
	}

	public static File resolveProjectPath(File file) {
		String path;
		try {
			path = file.getCanonicalPath();
		} catch (IOException e) {
			path = file.getAbsolutePath();
		}
		String projectPath = Engine.PROJECTS_PATH + File.separator;
		if (path.startsWith(projectPath)) {
			path = path.substring(projectPath.length());
			Pattern reProject = Pattern.compile("(.*?)(" + Pattern.quote(File.separator) + ".*|$)");
			Matcher mProject = reProject.matcher(path);
			if (mProject.matches()) {
				String projectName = mProject.group(1);
				path = Engine.projectDir(projectName) + mProject.group(2);
				file = new File(path);
			}
		}
		return file;
	}

	public static boolean isProjectFile(String filePath) {
		return filePath.endsWith(".xml") || new File(filePath).getName().equals("c8oProject.yaml");
	}

	public static ClassLoader getEngineClassLoader() {
		return engineClassLoader;
	}
}
