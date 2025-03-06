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

package com.twinsoft.convertigo.eclipse;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.osgi.framework.BundleContext;

import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.IApplicationComponent;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.core.Pool;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.actions.SetupAction;
import com.twinsoft.convertigo.eclipse.dialogs.ButtonSpec;
import com.twinsoft.convertigo.eclipse.dialogs.CustomDialog;
import com.twinsoft.convertigo.eclipse.dialogs.GlobalsSymbolsWarnDialog;
import com.twinsoft.convertigo.eclipse.editors.StartupEditor;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditorInput;
import com.twinsoft.convertigo.eclipse.editors.jscript.JScriptEditorInput;
import com.twinsoft.convertigo.eclipse.views.mobile.MobileDebugView;
import com.twinsoft.convertigo.eclipse.views.palette.PaletteView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ClipboardManager;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectManager;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.references.ReferencesView;
import com.twinsoft.convertigo.eclipse.views.sourcepicker.SourcePickerView;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager.StudioProjects;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ReferencedProjectManager;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.events.ProgressEventListener;
import com.twinsoft.convertigo.engine.events.StudioEvent;
import com.twinsoft.convertigo.engine.events.StudioEventListener;
import com.twinsoft.convertigo.engine.requesters.HttpSessionListener;
import com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.Crypto2;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.LogWrapper;
import com.twinsoft.convertigo.engine.util.ProcessUtils;
import com.twinsoft.convertigo.engine.util.PropertiesUtils;
import com.twinsoft.convertigo.engine.util.SimpleCipher;
import com.twinsoft.util.Log;

/**
 * The main plugin class to be used in the desktop.
 */
public class ConvertigoPlugin extends AbstractUIPlugin implements IStartup, StudioProjects {

	public static final String PLUGIN_UNIQUE_ID = "com.twinsoft.convertigo.eclipse.ConvertigoPlugin"; //$NON-NLS-1$

	static final String PLUGIN_PERSPECTIVE_ID = "com.twinsoft.convertigo.eclipse.ConvertigoPerspective"; //$NON-NLS-1$
	
	

	public static ProjectManager projectManager = null;

	public static ClipboardManager clipboardManagerDND = null;
	public static ClipboardManager clipboardManagerSystem = null;

	public static DeploymentConfigurationManager deploymentConfigurationManager = null;

	

	public static final String PREFERENCE_LOG_LEVEL = "log.level";
	public static final String PREFERENCE_TREE_HIGHLIGHT_DETECTED = "tree.highlight.detected";
	static final String PREFERENCE_OPENED_CONSOLES = "opened.consoles";
	public static final String PREFERENCE_TRACEPLAYER_PORT = "traceplayer.port";
	public static final String PREFERENCE_IGNORE_NEWS = "news.ignore";
	public static final String PREFERENCE_SHOW_ENGINE_INTO_CONSOLE = "engine.into.console";
	public static final String PREFERENCE_ENGINE_LOAD_ALL_PROJECTS = "engine.load.all.projects";
	public static final String PREFERENCE_LOCAL_BUILD_ADDITIONAL_PATH = "localBuild.additionalPath";
	public static final String PREFERENCE_LOCAL_BUILD_FOLDER = "localBuild.folder";
	public static final String PREFERENCE_AUTO_OPEN_DEFAULT_CONNECTOR = "autoOpen.defaultConnector";
	public static final String PREFERENCE_MOBILE_BUILDER_THRESHOLD = "mobileBuilder.threshold";
	public static final String PREFERENCE_AUTO_CREATE_PROJECT_REFERENCE = "autoCreate.projectReference";
	public static final String PREFERENCE_AUTO_CREATE_PROJECT_GIT_REPOSITORY = "autoCreate.projectGitRepository";
	public static final String PREFERENCE_AUTO_UPDATE_README = "autoUpdate.readme";
	public static final String PREFERENCE_USE_SYSTEM_FLOWVIEWER = "useSystem.flowViewer";
	public static final String PREFERENCE_EDITOR_OUTPUT_MODE = "editor.output.mode";
	public static final String PREFERENCE_HIDE_LIB_PROJECTS = "hide.lib.projects";
	public static final String PREFERENCE_BROWSER_OFFSCREEN = "browser.offscreen";
	
	private static final QualifiedName qnInit = new QualifiedName(PLUGIN_UNIQUE_ID + ".init", "done");
	
	private StudioEventListener studioEventListener = (var event) -> {
		if (StudioEvent.ERROR_MESSAGE.equals(event.type())) {
			getDisplay().asyncExec(() -> {
				ErrorDialog.openError(null, null, null, new Status(IStatus.ERROR, PLUGIN_UNIQUE_ID, event.payload()));
			});
		}
	};
	
	private ProgressEventListener progressEventListener = (var event) -> {
		try {
			Job job = Job.create(event.getName(), monitor -> {
				monitor.beginTask(event.getStatus(), IProgressMonitor.UNKNOWN);
				while (event.waitNextStatus()) {
					monitor.beginTask(event.getStatus(), IProgressMonitor.UNKNOWN);
				}
				monitor.done();
			});
			job.schedule();
		} catch (IllegalStateException e) {
			// job manager is probably stopped
		}
	};
	
	private static Display display = null;
	public static synchronized Display getDisplay() {
		if (display == null) {
			display = Display.getCurrent();
			//may be null if outside the UI thread
			if (display == null) {
				display = Display.getDefault();
			}
		}
		return display;
	}

	private static Shell mainShell = null;
	public static synchronized Shell getMainShell() {
		if (mainShell == null || mainShell.isDisposed()) {
			mainShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		}
		return mainShell;
	}
	
	public static void syncExec(Runnable run) {
		getDisplay().syncExec(() -> {
			try {
				run.run();
			} catch (Exception e) {
				String msg = "(ConvertigoPlugin) syncExec Exception: [" + e.getClass() + "] " + e.getMessage();
				try {
					Engine.logStudio.debug(msg);
				} catch (Exception e2) {
					System.out.println(msg);
				}
			}
		});
	}
	
	public static void asyncExec(Runnable run) {
		getDisplay().asyncExec(() -> {
			try {
				run.run();
			} catch (Exception e) {
				String msg = "(ConvertigoPlugin) asyncExec Exception: [" + e.getClass() + "] " + e.getMessage();
				try {
					Engine.logStudio.debug(msg);
				} catch (Exception e2) {
					System.out.println(msg);
				}
			}
		});
	}

	public static class PscException extends Exception {
		private static final long serialVersionUID = -3828463232797723301L;

		private PscException(String cause) {
			super(cause);
		}
	}

	public static String getProperty(String key) {
		IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore();
		logDebug3("Looking for property : \"" + key + "\"");

		String result = preferenceStore.getString(key);

		logDebug3("==> Getting property " + key + ": \"" + result + "\"");

		return result;
	}

	public static void setProperty(String key, String value) {
		IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore();
		preferenceStore.setValue(key, value);
	}

	static {
		TrayDialog.setDialogHelpAvailable(false);
	}
	
	//The shared instance.
	private static ConvertigoPlugin plugin;

	private HttpSession session;

	//Resource bundle.
	private ResourceBundle resourceBundle;

	private boolean shuttingDown = false;

	private List<Runnable> runAtStartup = new LinkedList<Runnable>();

	private static Log studioLog;

	private static ILog log;

	public static void logException(Throwable e, String message) {
		logException(e, message, true);
	}

	public static void logException(Throwable e, String message, boolean dialog) {
		log.log(new Status(Status.ERROR, ConvertigoPlugin.PLUGIN_UNIQUE_ID, Status.OK, message, e));
		if (dialog) errorMessageBox(message + "\n" + e.getMessage());
	}

	public static void logError(String message) {
		logError(message, false);
	}

	public static void logError(String message, boolean dialog) {
		log.log(new Status(Status.ERROR, ConvertigoPlugin.PLUGIN_UNIQUE_ID, Status.OK, message, null));
		if (dialog) errorMessageBox(message);
	}

	public static void logWarning(String message) {
		logWarning(null, message, true);
	}

	public static void logWarning(Throwable e, String message) {
		logWarning(e, message + ((e!=null)? "\n" + e.getMessage():""), true);
	}

	public static void logWarning(Throwable e, String message, boolean dialog) {
		log.log(new Status(Status.WARNING, ConvertigoPlugin.PLUGIN_UNIQUE_ID, Status.OK, message, e));
		if (dialog) warningMessageBox(message);
	}

	public static void logInfo(String message) {
		logInfo(message, false);
	}

	public static void logInfo(String message, boolean dialog) {
		log.log(new Status(Status.INFO, ConvertigoPlugin.PLUGIN_UNIQUE_ID, Status.OK, message, null));
		if (dialog) infoMessageBox(message);
	}

	public static void logDebug(String message) {
		studioLog.debug(message);
	}

	public static void logDebug2(String message) {
		studioLog.debug2(message);
	}

	public static void logDebug3(String message) {
		studioLog.debug3(message);
	}

	public static void errorMessageBox(String message) {
		ConvertigoPlugin.messageBoxWithoutReturnCode(message, SWT.OK | SWT.ICON_ERROR);
	}

	// Must be called in the display GUI thread
	public static int questionMessageBox(Shell shell, String message){
		return ConvertigoPlugin.messageBox(shell, message, SWT.YES | SWT.NO | SWT.ICON_QUESTION);
	}

	public static void warningMessageBox(String message) {
		ConvertigoPlugin.messageBoxWithoutReturnCode(message, SWT.OK | SWT.ICON_WARNING);
	}

	public static boolean[] warningGlobalSymbols(final String projectName,
			final String objectName, final String objectType,
			final String propertyName, final String propertyValue,
			final Set<String> undefinedSymboles, final boolean showCheckBox) {
		final boolean[] result = {false,false};
		
		Runnable runnable = new Runnable() {
			public void run() {
			try {
				int level = ModalContext.getModalLevel();
				if (level > 0) {
					// prevents double modal windows: dead lock on linux/gtk studio
					asyncExec(this);
					return;
				}

				GlobalsSymbolsWarnDialog dialogGlobalSymbols = new GlobalsSymbolsWarnDialog(getDisplay().getActiveShell(), projectName,
						objectName, objectType,
						propertyName, propertyValue,
						undefinedSymboles, showCheckBox);
				dialogGlobalSymbols.setBlockOnOpen(true);
				dialogGlobalSymbols.open();

				result[0] = dialogGlobalSymbols.getCreateAction();
				result[1] = dialogGlobalSymbols.getCheckButtonSelection();
			} catch (Exception e){
				ConvertigoPlugin.logException(e, "Error while trying to open warning global symbols box");
			}
		}};
		
		if (Thread.currentThread().equals(Display.getDefault().getThread())) {
			runnable.run();
		} else {
			syncExec(runnable);
		}
		return result;
	}

	public static void infoMessageBox(final String message) {
		ConvertigoPlugin.messageBoxWithoutReturnCode(message, SWT.OK | SWT.ICON_INFORMATION);
	}

	private static void messageBoxWithoutReturnCode(final String message, int options) {
		final Display display = getDisplay();

		Runnable runnable = () -> {
			messageBox(null, message, SWT.OK | SWT.ICON_INFORMATION);
		};

		if (display.getThread() != Thread.currentThread()) {
			asyncExec(runnable);
		} else {
			syncExec(runnable);
		}
	}

	private static int messageBox(Shell shell, String message, int options) {
		try {
			if (shell == null) {
				shell = getMainShell();
			}

			MessageBox messageBox = new MessageBox(shell, options);
			messageBox.setText("Convertigo");
			if (message == null) message = "(null message)";
			messageBox.setMessage(message);
			int response = messageBox.open();
			return response;
		}
		catch (Exception e){
			ConvertigoPlugin.logException(e, "Error while trying to open message box", false);
			return -1;
		}
	}

	public static void configureDeployConfiguration() {
		// The embedded Tomcat has been created, so all engine paths have been computed.
		deploymentConfigurationManager = new DeploymentConfigurationManager();

		try {
			deploymentConfigurationManager.load();
		} catch (Exception e) {
			logException(e, "Unable to load deployment configurations");
		}

		try {

			Properties properties = decodePsc();
			for (int i = 1; i < Integer.MAX_VALUE; i++) {
				if (i > 1 && !properties.containsKey(DeploymentKey.adminUser.key(i))) {
					break;
				}
				if (!"".equals(DeploymentKey.server.value(properties, i))) {
					deploymentConfigurationManager.add(new DeploymentConfigurationReadOnly(
							DeploymentKey.server.value(properties, i),
							DeploymentKey.adminUser.value(properties, i),
							DeploymentKey.adminPassword.value(properties, i),
							Boolean.parseBoolean(DeploymentKey.sslHttps.value(properties, i)),
							Boolean.parseBoolean(DeploymentKey.sslTrustCert.value(properties, i)),
							Boolean.parseBoolean(DeploymentKey.assembleXsl.value(properties, i)))
							);
				}
			}
		} catch (Exception e) {
			logException(e, "Unable to load deployment configurations from PSC");
		}
	}

	/**
	 * The constructor.
	 */
	public ConvertigoPlugin() {
		super();
	}

	private EmbeddedTomcat embeddedTomcat = null;
	
	private Runnable afterPscOk = null;
	public void runSetup() {
		try {
			boolean success = SetupAction.runSetup();
			if (afterPscOk != null) {
				if (success) {
					Engine.isStartFailed = false;
					new Thread(afterPscOk, "Start embedded Tomcat").start();
				} else {
					studioLog.message("PSC not validated, please restart the Studio to get the Convertigo Setup wizard again.");
				}
			}
		} catch (Throwable t) {
			studioLog.exception(t, "Failure during the Convertigo setup wizard");
		}
	}
	
	/**
	 * This method is called upon plug-in activation
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		
		Boolean[] needPalette = {null};
		Boolean[] needPicker = {null};
		IWorkbenchPage[] activePage = {null};
		
		IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWindow != null) {
			activePage[0] = activeWindow.getActivePage();
			if (activePage != null) {
				IEditorReference[] editorRefs = activePage[0].getEditorReferences();
				for (int i = 0; i < editorRefs.length; i++) {
					String id = editorRefs[i].getId();
					if (id.startsWith("com.twinsoft.convertigo.eclipse.editors") || id.equals("org.eclipse.ui.internal.emptyEditorTab")) {
						activePage[0].closeEditors(new IEditorReference[] {editorRefs[i]}, false);
					}
				}
				IViewReference[] viewRefs = activePage[0].getViewReferences();
				for (int i = 0; i < viewRefs.length; i++) {
					String id = viewRefs[i].getId();
					boolean closeView = false;
					switch (id) {
					case "com.twinsoft.convertigo.eclipse.views.mobile.MobilePaletteView":
					case "com.twinsoft.convertigo.eclipse.views.mobile.NgxPaletteView":
						if (needPalette[0] == null) {
							needPalette[0] = true;
						}
						closeView = true;
						break;
					case "com.twinsoft.convertigo.eclipse.views.mobile.MobilePickerView":
					case "com.twinsoft.convertigo.eclipse.views.mobile.NgxPickerView":
						if (needPicker[0] == null) {
							needPicker[0] = true;
						}
						closeView = true;
						break;
					case "com.twinsoft.convertigo.eclipse.views.palette.PaletteView":
						needPalette[0] = false;
						break;
					case "com.twinsoft.convertigo.eclipse.views.sourcepicker.SourcePickerView":
						needPicker[0] = false;
						break;
					}
					
					if (closeView) {
						activePage[0].hideView(viewRefs[i]);
					}
				}
			}
		}

		// Version check
		if (!com.twinsoft.convertigo.eclipse.Version.productVersion.equals(com.twinsoft.convertigo.beans.Version.productVersion)) {
			throw new Exception("The product version numbers of Eclipse Plugin and Objects libraries are differents.");
		} else if (!com.twinsoft.convertigo.eclipse.Version.productVersion.equals(com.twinsoft.convertigo.engine.Version.productVersion)) {
			throw new Exception("The product version numbers of Eclipse Plugin and Engine libraries are differents.");
		}
		
		Resource.setNonDisposeHandler(null);

		Engine.setStudioMode();

		plugin = this;
		try {
			resourceBundle = ResourceBundle.getBundle("com.twinsoft.convertigo.eclipse.ConvertigoPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}

		log = getLog();
		projectManager = new ProjectManager();
		clipboardManagerSystem = new ClipboardManager();
		clipboardManagerDND = new ClipboardManager();
		//learnProxy = new LearnProxy();

		// Create consoles
		createConsoles();

		// Redirect stdout and stderr
		System.setOut(new StdoutStream());
		System.setErr(new StderrStream());

		studioLog = new Log(ConvertigoPlugin.getDefault().stdoutConsoleStream);

		runAtStartup(() -> {
			studioLog = new LogWrapper(Engine.logStudio);
		});

		studioLog.logLevel = Log.LOGLEVEL_DEBUG;

		try {
			studioLog.logLevel = Integer.valueOf(ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_LOG_LEVEL)).intValue();
		}
		catch(NumberFormatException e) {
			studioLog.warning("Unable to retrieve the log level; using default log level (4).");
		}

		studioLog.message("Starting the Convertigo studio eclipse plugin");

		try {
			highlightDetectedObject = Boolean.valueOf(ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_TREE_HIGHLIGHT_DETECTED)).booleanValue();
		}
		catch(NumberFormatException e) {
			studioLog.warning("Unable to retrieve the highlight option; using default highlight option (true).");
		}

		try {
			autoOpenDefaultConnector = Boolean.valueOf(ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_AUTO_OPEN_DEFAULT_CONNECTOR)).booleanValue();
		}
		catch(NumberFormatException e) {
			studioLog.warning("Unable to retrieve the auto open default connector option; using default (false).");
		}

		try {
			mobileBuilderThreshold = Integer.valueOf(ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_MOBILE_BUILDER_THRESHOLD)).intValue();
		}
		catch(NumberFormatException e) {
			studioLog.warning("Unable to retrieve the mobile builder threshold option; using default (200).");
		}
		
		
		getPreferenceStore().setDefault(PREFERENCE_AUTO_CREATE_PROJECT_GIT_REPOSITORY, true);

		// In STUDIO, the Convertigo User Workspace is in the current Eclipse Workspace/.metadata/.plugins/com.twinsoft.convertigo.studio
		Engine.USER_WORKSPACE_PATH = getDefault().getStateLocation().toFile().getCanonicalPath();

		// In STUDIO, the Convertigo Projects directory is the current Eclipse Workspace
		Engine.PROJECTS_PATH = ResourcesPlugin.getWorkspace().getRoot().getRawLocation().toFile().getCanonicalPath();

		//		checkPre_6_2_0_Migration();

		// Adds listeners
		addListeners();
		
		runAtStartup(() -> {
			Engine.theApp.eventManager.addListener(progressEventListener, ProgressEventListener.class);
			Engine.theApp.eventManager.addListener(studioEventListener, StudioEventListener.class);
			Engine.execute(() -> Engine.theApp.couchDbManager.getFullSyncClient());
			try {
				if (needPalette[0] == Boolean.TRUE) {
					activePage[0].showView("com.twinsoft.convertigo.eclipse.views.palette.PaletteView");
				}
				
				if (needPicker[0] == Boolean.TRUE) {
					activePage[0].showView("com.twinsoft.convertigo.eclipse.views.sourcepicker.SourcePickerView");
				}
			} catch (Exception e) {
			}
		});

		DatabaseObjectsManager.studioProjects = this;

		final Exception afterPscException[] = { null };
		final Runnable afterPscOk = this.afterPscOk = new Runnable() {

			public void run() {
				try {
					ConvertigoPlugin.this.afterPscOk = null;
					// Create embedded Tomcat
					studioLog.message("Starting the embedded Tomcat");
					System.setProperty("org.apache.commons.logging.log", "org.apache.commons.logging.impl.Jdk14Logger");

					Path path = new Path("tomcat");
					URL tomcatHomeUrl = FileLocator.find(context.getBundle(), path, null);

					String tomcatHome = FileLocator.toFileURL(tomcatHomeUrl).getPath();

					int index = (System.getProperty("os.name").indexOf("Windows") != -1) ? 1 : 0;
					tomcatHome = tomcatHome.substring(index);

					embeddedTomcat = new EmbeddedTomcat(tomcatHome);

					configureDeployConfiguration();

					//displayWaitScreen();
					runAtStartup(() -> {
						ProjectExplorerView pew = getProjectExplorerView();
						pew.initialize();
					});

					new Thread(embeddedTomcat, "Embedded Tomcat").start();
					new Thread(new Runnable() {

						public void run() {
							int nbRetry = 0;
							while (!Engine.isStartFailed && !Engine.isStarted) {
								try {
									Thread.sleep(500);
									nbRetry++;
								} catch (InterruptedException e) {
									// Ignore
								}

								// Aborting if too many retries
								if (nbRetry > 360) return;
							}

							if (Engine.isStartFailed) {
								logError("Unable to start engine; see console for more details");
								return;
							}

							// The console threads must be started AFTER the engine
							consolePipes.startConsoleThreads();

							try {
								deploymentConfigurationManager.doMigration();
							} catch (Exception e) {
								logException(e, "Unable to migrate deployment configurations");
							}

							studioLog.message("Embedded Tomcat started");

							for (Runnable runnable : runAtStartup) {
								asyncExec(runnable);
							}
							runAtStartup.clear();
						}

					}, "Wait Embedded Tomcat started").start();
					asyncExec(() -> launchStartupPage(true));
				} catch (Exception e) {
					afterPscException[0] = e;
				}
			}

		};

		try {
			decodePsc();
			//Engine.isStartFailed = true;
			afterPscOk.run();
			if (afterPscException[0] != null) {
				throw afterPscException[0];
			}
		} catch (PscException e) {
			studioLog.message("No valid PSC, the Engine will start after the registration wizard.\nFailure message : " + e.getMessage());
			Engine.isStartFailed = true;
			asyncExec(() -> runSetup());
		}

		runAtStartup(() -> {
			String nodeVersion = ProcessUtils.getDefaultNodeVersion();
			Job job = Job.create("Retrieve default nodejs " + nodeVersion, monitor -> {
				try {
					monitor.beginTask("In progress", 120);
					monitor.subTask("checking for existing nodejs");
					monitor.worked(1);
					boolean first[] = {true};
					File nodeDir = ProcessUtils.getNodeDir(nodeVersion, new org.apache.commons.fileupload.ProgressListener() {

						@Override
						public void update(long pBytesRead, long pContentLength, int pItems) {
							if (first[0]) {
								monitor.worked(10);
								monitor.subTask("downloading nodejs [" + (pContentLength / (1024 * 1024)) + " MB]");
								first[0] = false;
							}
							Engine.logConvertigo.info("download NodeJS " + nodeVersion + ": " + Math.round(100f * pBytesRead / pContentLength) + "% [" + pBytesRead + "/" + pContentLength + "]");
							monitor.worked(10 + Math.round(100f * pBytesRead / pContentLength));
							if (pBytesRead == pContentLength) {
								monitor.subTask("installing nodejs");
								monitor.worked(110);
							}
						}
					});
					monitor.worked(120);
					File nodeExe = new File(nodeDir, Engine.isWindows() ? "node.exe" : "node");
					Engine.logStudio.warn("node ready: " + nodeExe.getAbsolutePath() + " exists ? " + nodeExe.exists());
					System.setProperty("org.eclipse.wildwebdeveloper.nodeJSLocation", nodeExe.getAbsolutePath());
					ProcessUtils.setDefaultNodeDir(nodeDir);
					monitor.done();
				} catch (Exception e) {
					Engine.logStudio.error("Failed to init NPM: " + e.getMessage(), e);
				}
			});
			job.schedule();
		});
		
		studioLog.message("Convertigo studio started");
	}

	private ConvertigoWorkbenchListener workbenchListener = null;
	private ConvertigoWindowListener windowListener = null;
	private ConvertigoPartListener partListener = null;
	private ConvertigoPerspectiveListener perspectiveListener = null;

	private void addListeners() {
		try {
			IWorkbench workbench = PlatformUI.getWorkbench();

			// Add a WorkbenchListener
			workbenchListener = new ConvertigoWorkbenchListener();
			workbench.addWorkbenchListener(workbenchListener);

			// Add a WindowListener
			windowListener = new ConvertigoWindowListener();
			workbench.addWindowListener(windowListener);

			IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
			if (activeWorkbenchWindow != null) {
				// Add a PerspectiveListener
				if (perspectiveListener == null) {
					perspectiveListener = new ConvertigoPerspectiveListener();
					activeWorkbenchWindow.addPerspectiveListener(perspectiveListener);
				}
				// Add a PartListener
				if (partListener == null) {
					partListener = new ConvertigoPartListener();
					IPartService partService = activeWorkbenchWindow.getPartService();
					partService.addPartListener(partListener);
				}
			}

			Repository.getGlobalListenerList().addWorkingTreeModifiedListener(event -> {
				for (var trace: Thread.currentThread().getStackTrace()) {
					if (trace.getClassName().equals(ReferencedProjectManager.class.getName())) {
						return;
					}
				}
				Engine.logStudio.debug("(Git Event) onWorkingTreeModified " + event);
				File workDir = event.getRepository().getWorkTree();
				Collection<String> files = new TreeSet<>(event.getModified());
				files.addAll(event.getDeleted());
				Set<File> affectedProjects = new HashSet<>();
				for (String f : files) {
					Engine.logStudio.trace("(Git Event) change for " + f);
					if (f.endsWith(".yaml")) {
						File file = new File(workDir, f);
						if (file.getName().equals("c8oProject.yaml")) {
							affectedProjects.add(file.getParentFile());
						} else {
							File parent = file.getParentFile();
							while (parent != null && !parent.getName().equals("_c8oProject")) {
								parent = parent.getParentFile();
							}
							if (parent != null) {
								parent = parent.getParentFile();
								if (new File(parent, "c8oProject.yaml").exists()) {
									affectedProjects.add(parent);
								}
							}
						}
					}
				}
				Engine.logStudio.debug("(Git Event) affected projects: " + affectedProjects);
				asyncExec(() -> {
					ProjectExplorerView pew = ConvertigoPlugin.this.getProjectExplorerView();
					for (ProjectTreeObject treeProject: pew.getOpenedProjects()) {
						if (affectedProjects.contains(new File(treeProject.getObject().getDirPath()))) {
							String name = "'" + treeProject.getName() + "'";
							CustomDialog customDialog = new CustomDialog(
								getMainShell(),
								"Git modified project description of " + name,
								"Reload " + name + " with incoming changes ?\n" +
								"Save " + name + " and ignore incoming changes ?\n" +
								"Do nothing (reload or save by your own) ?",
								500, 180,
								new ButtonSpec("Reload", true),
								new ButtonSpec("Save", false),
								new ButtonSpec("Do nothing", false));
							int response = customDialog.open();
							switch (response) {
							case 0: pew.reloadProject(treeProject); break;
							case 1: treeProject.save(false); break;
							default: treeProject.markAsChanged(true); break;
							}
						}
					}
				});
			});
		}
		catch (IllegalStateException e) {
			studioLog.error("Could not add listeners to plugin."+ e.getMessage());
		}

	}

	public void earlyStartup() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		asyncExec(() -> {
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			if (window != null) {
				if (perspectiveListener == null) {
					perspectiveListener = new ConvertigoPerspectiveListener();
					window.addPerspectiveListener(perspectiveListener);
				}

				if (partListener == null) {
					partListener = new ConvertigoPartListener();
					IPartService partService = window.getPartService();
					partService.addPartListener(partListener);
				}

				// Opens Convertigo perspective
				try {
					studioLog.message("Opening Convertigo perspective.");
					workbench.showPerspective(ConvertigoPlugin.PLUGIN_PERSPECTIVE_ID, window);
				} catch (WorkbenchException e) {
					studioLog.error("Could not open Convertigo perspective.\n" + e.getMessage());
				}
			}
		});
	}

	static public int getTraceplayerPort() {
		IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore();
		return preferenceStore.getInt(ConvertigoPlugin.PREFERENCE_TRACEPLAYER_PORT);
	}

	static public String getLocalBuildAdditionalPath() {
		IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore();
		return preferenceStore.getString(ConvertigoPlugin.PREFERENCE_LOCAL_BUILD_ADDITIONAL_PATH);
	}

	static public String getLocalBuildFolder() {
		IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore();
		return preferenceStore.getString(ConvertigoPlugin.PREFERENCE_LOCAL_BUILD_FOLDER);
	}

	static public boolean getBrowserOffscreen() {
		IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore();
		return preferenceStore.getBoolean(ConvertigoPlugin.PREFERENCE_BROWSER_OFFSCREEN);
	}

	static public void setLogLevel(int logLevel) {
		studioLog.logLevel = logLevel;
	}

	

	private static boolean 	highlightDetectedObject;

	public static void setHighlightDetectedObject(boolean highlight) {
		highlightDetectedObject = highlight;
	}

	public static boolean getHighlightDetectedObject() {
		return highlightDetectedObject;
	}

	public static void setShowEngineIntoConsole(boolean show) {
	}

	

	private static boolean autoOpenDefaultConnector = false;

	public static boolean getAutoOpenDefaultConnector() {
		return autoOpenDefaultConnector;
	}

	public static void setAutoOpenDefaultConnector(boolean autoOpen) {
		autoOpenDefaultConnector = autoOpen;
	}

	private static int mobileBuilderThreshold = 200;

	public static int getMobileBuilderThreshold() {
		return mobileBuilderThreshold;
	}

	public static void setMobileBuilderThreshold(int threshold) {
		mobileBuilderThreshold = threshold;
	}

	/**
	 * Clean plug-in
	 * 
	 * @param context
	 * @throws Exception
	 */
	private void clean(BundleContext context) throws Exception {
		if (consolePipes != null)
			consolePipes.stopConsoleThreads();

		stderrConsoleStreamColor.dispose();

		disposeImages();

		// Removes listeners
		try {
			IWorkbench workbench = PlatformUI.getWorkbench();
			if (windowListener != null)
				workbench.removeWindowListener(windowListener);
			if (workbenchListener != null)
				workbench.removeWorkbenchListener(workbenchListener);

			IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
			if (activeWorkbenchWindow != null) {
				if (perspectiveListener != null)
					activeWorkbenchWindow.removePerspectiveListener(perspectiveListener);
				if (partListener != null)
					activeWorkbenchWindow.getPartService().removePartListener(partListener);
			}
		}
		catch (IllegalStateException e) {}
		try {
			Engine.theApp.eventManager.removeListener(progressEventListener, ProgressEventListener.class);
			Engine.theApp.eventManager.removeListener(studioEventListener, StudioEventListener.class);
		} catch (Exception e) {
		}

		if (embeddedTomcat != null) {
			Engine.isStarted = false;
			embeddedTomcat.stop();
		}
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {

		try {
			clean(context);
		}
		catch (Exception e) {}

		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 */
	public static ConvertigoPlugin getDefault() {
		return plugin;
	}

	

	private Map<String, Image> icons = new HashMap<String, Image>();

	public synchronized Image getIconFromPath(String iconPath, int iconKind) throws IOException {
		Image image = icons.get(iconPath);
		if (image == null) {
			Device device = getDisplay();
			InputStream inputStream = ConvertigoPlugin.class.getResourceAsStream(iconPath);
			if (inputStream != null)
				image = new Image(device, inputStream);
			if (image == null)
				image = getDefaultBeanIcon(null, iconKind);
			icons.put(iconPath, image);
		}
		return image;
	}

	public synchronized Image getStudioIcon(String iconPath) throws IOException {
		Image image = icons.get(iconPath);
		if (image == null) {
			icons.put(iconPath, image = new Image(getDisplay(), FileLocator.find(getBundle(), new Path(iconPath), null).openStream()));
		}
		return image;
	}

	public synchronized Image getBeanIcon(DatabaseObject bean, int iconKind) throws IntrospectionException {
		Class<? extends DatabaseObject> beanClass = bean.getClass();
		BeanInfo bi = CachedIntrospector.getBeanInfo(beanClass);
		return getBeanIcon(bi, iconKind);
	}

	public synchronized Image getBeanIcon(BeanInfo bi, int iconKind) throws IntrospectionException {
		Class<?> beanClass = bi.getBeanDescriptor().getBeanClass();
		String beanClassName = beanClass.getName();

		Image beanIcon = icons.get(beanClassName + iconKind);

		if (beanIcon == null) {
			ConvertigoPlugin.studioLog.debug("Getting icon:" + beanClassName + iconKind);

			String iconName = MySimpleBeanInfo.getIconName(bi, iconKind);
			if (iconName == null) {
				iconName = "/com/twinsoft/convertigo/beans/core/images/default_color_32x32.png";
			}

			Device device = getDisplay();
			InputStream inputStream = ConvertigoPlugin.class.getResourceAsStream(iconName);
			if (inputStream != null)
				beanIcon = new Image(device, inputStream);
			if (beanIcon == null)
				beanIcon = getDefaultBeanIcon(beanClass, iconKind);
			icons.put(beanClassName + iconKind, beanIcon);
		}

		return beanIcon;
	}

	private Image getDefaultBeanIcon(Class<?> beanClass, int iconKind) {
		String iconBaseName, iconType;

		iconBaseName = "default";
		if (beanClass != null) {
			if (Criteria.class.isAssignableFrom(beanClass)) {
				iconBaseName = "criteria";
			}
			else if (ExtractionRule.class.isAssignableFrom(beanClass)) {
				iconBaseName = "extractionrule";
			}
			else if (Transaction.class.isAssignableFrom(beanClass)) {
				iconBaseName = "transaction";
			}
			else if (BlockFactory.class.isAssignableFrom(beanClass)) {
				iconBaseName = "blockfactory";
			}
			else if (Project.class.isAssignableFrom(beanClass)) {
				iconBaseName = "project";
			}
			else if (ScreenClass.class.isAssignableFrom(beanClass)) {
				iconBaseName = "screenclass";
			}
			else if (Sheet.class.isAssignableFrom(beanClass)) {
				iconBaseName = "sheet";
			}
			else if (Pool.class.isAssignableFrom(beanClass)) {
				iconBaseName = "pool";
			}
		}

		switch (iconKind) {
		case java.beans.BeanInfo.ICON_COLOR_16x16:
			iconType = "_color_16x16.png";
			break;
		default:
		case java.beans.BeanInfo.ICON_COLOR_32x32:
			iconType = "_color_32x32.png";
			break;
		case java.beans.BeanInfo.ICON_MONO_16x16:
			iconType = "_mono_16x16.png";
			break;
		case java.beans.BeanInfo.ICON_MONO_32x32:
			iconType = "_mono_32x32.png";
			break;
		}

		Image beanIcon = (Image) icons.get(iconBaseName + iconType);

		if (beanIcon == null) {
			ConvertigoPlugin.studioLog.debug("Getting default icon: " + iconBaseName + iconType);
			String iconName = "/com/twinsoft/convertigo/beans/core/images/"+ iconBaseName + iconType;
			Device device = getDisplay();
			InputStream inputStream = ConvertigoPlugin.class.getResourceAsStream(iconName);
			beanIcon = new Image(device, inputStream);
			icons.put(iconBaseName + iconType, beanIcon);
		}

		return beanIcon;
	}

	private void disposeImages() {
		for (Image beanIcon : icons.values()) {
			if (beanIcon != null)
				beanIcon.dispose();
		}
		icons.clear();
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

	private ConsolePipes consolePipes = null;

	MessageConsole engineConsole;
	MessageConsole stdoutConsole;

	MessageConsoleStream engineConsoleStream;
	MessageConsoleStream stdoutConsoleStream;
	MessageConsoleStream stderrConsoleStream;
	public MessageConsoleStream debugConsoleStream;

	private Color stderrConsoleStreamColor = new Color(null, 200, 0, 0);

	private static int TAB_WIDTH = 5;
	
	private void createConsoles() {
		ConsolePlugin consolePlugin = ConsolePlugin.getDefault();
		IConsoleManager consoleManager = consolePlugin.getConsoleManager();

		stdoutConsole = new MessageConsole("Stdout", ImageDescriptor.createFromFile(getClass(), "/consoles/stdout.gif"));
		stdoutConsole.setTabWidth(TAB_WIDTH);
		stdoutConsoleStream = stdoutConsole.newMessageStream();
		stderrConsoleStream = stdoutConsole.newMessageStream();
		stderrConsoleStream.setColor(stderrConsoleStreamColor);

		engineConsole = new MessageConsole("Engine", ImageDescriptor.createFromFile(getClass(), "/consoles/engine.gif"));
		engineConsole.setTabWidth(TAB_WIDTH);
		engineConsoleStream = engineConsole.newMessageStream();

		consoleManager.addConsoles(new IConsole[] {
				engineConsole,
				stdoutConsole
		});

		consolePipes = new ConsolePipes();

		debugConsoleStream = new MessageConsoleStream(engineConsole) {

			@Override
			public void write(String str) throws IOException {
				if (str.endsWith("\n")) {
					str = str.substring(0, str.length() - 1);
				}
				Engine.logStudio.info("[debug] " + str);
			}

		};
	}

	private IWorkbenchPage getActivePage() {
		IWorkbench wb = PlatformUI.getWorkbench();
		if (wb != null) {
			IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
			if (win != null) {
				IWorkbenchPage page = win.getActivePage();
				return page;
			}
		}
		return null;
	}

	/**
	 * Gets the projects explorer view.
	 * !!MUST BE CALLED IN A UI-THREAD!!
	 * @return ProjectExplorerView : the explorer view of Convertigo Plugin
	 */
	public ProjectExplorerView getProjectExplorerView() {
		ProjectExplorerView projectExplorerView = null;
		IWorkbenchPage activePage = getActivePage();
		if (activePage != null) {
			IViewPart viewPart =  activePage.findView("com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView");
			if (viewPart != null)
				projectExplorerView = (ProjectExplorerView)viewPart;
			else {
				IWorkbench workbench = PlatformUI.getWorkbench();
				try {
					IWorkbenchPage page = workbench.showPerspective(ConvertigoPlugin.PLUGIN_PERSPECTIVE_ID, workbench.getActiveWorkbenchWindow());
					viewPart =  page.findView("com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView");
					if (viewPart != null) {
						projectExplorerView = (ProjectExplorerView)viewPart;
					}
				} catch (WorkbenchException e) {}
			}
		}
		return projectExplorerView;
	}

	/**
	 * Gets the properties view.
	 * !!MUST BE CALLED IN A UI-THREAD!!
	 * @return PropertySheet : the properties view of Convertigo Plugin
	 */
	public PropertySheet getPropertiesView() {
		PropertySheet propertiesView = null;
		IWorkbenchPage activePage = getActivePage();
		if (activePage != null) {
			IViewPart viewPart =  activePage.findView("org.eclipse.ui.views.PropertySheet");
			if (viewPart != null)
				propertiesView = (PropertySheet)viewPart;
		}

		return propertiesView;
	}

	public ReferencesView getReferencesView() {
		ReferencesView referencesView = null;
		IWorkbenchPage activePage = getActivePage();
		if (activePage != null) {
			IViewPart viewPart =  activePage.findView("com.twinsoft.convertigo.eclipse.views.references.ReferencesView");
			if (viewPart != null)
				referencesView = (ReferencesView)viewPart;
		}
		return referencesView;
	}

	public PaletteView getPaletteView() {
		PaletteView paletteView = null;
		IWorkbenchPage activePage = getActivePage();
		if (activePage != null) {
			IViewPart viewPart =  activePage.findView("com.twinsoft.convertigo.eclipse.views.palette.PaletteView");
			if (viewPart != null)
				paletteView = (PaletteView)viewPart;
		}
		return paletteView;
	}

	/**
	 * Gets the source picker view.
	 * !!MUST BE CALLED IN A UI-THREAD!!
	 * @return SourcePickerView : the source picker view of Convertigo Plugin
	 */
	public SourcePickerView getSourcePickerView() {
		SourcePickerView sourcePickerView = null;
		IWorkbenchPage activePage = getActivePage();
		if (activePage != null) {
			IViewPart viewPart =  activePage.findView("com.twinsoft.convertigo.eclipse.views.sourcepicker.SourcePickerView");
			if (viewPart != null)
				sourcePickerView = (SourcePickerView)viewPart;
		}
		return sourcePickerView;
	}

	/**
	 * Gets the source picker view.
	 * !!MUST BE CALLED IN A UI-THREAD!!
	 * @return SourcePickerView : the source picker view of Convertigo Plugin
	 * @throws
	 */
	public MobileDebugView getMobileDebugView(boolean force) {
		MobileDebugView mobileDebugView = null;
		try {
			IWorkbenchPage activePage = getActivePage();
			if (activePage != null) {
				IViewPart viewPart =  activePage.findView("com.twinsoft.convertigo.eclipse.views.mobile.MobileDebugView");
				if (viewPart != null)
					mobileDebugView = (MobileDebugView) viewPart;
			}
			if (mobileDebugView == null && force) {
				mobileDebugView = (MobileDebugView) getActivePage().showView("com.twinsoft.convertigo.eclipse.views.mobile.MobileDebugView");
			}
		} catch (PartInitException e) {
			logException(e, "Failed to get the MobileDebugView");
		}
		return mobileDebugView;
	}
	
	public JScriptEditorInput getJScriptEditorInput(Transaction transaction) {
		JScriptEditorInput jScriptEditorInput = null;
		IWorkbenchPage activePage = getActivePage();
		if (activePage != null && transaction != null) {
			for (IEditorReference editorRef: activePage.getEditorReferences()) {
				try {
					IEditorInput editorInput = editorRef.getEditorInput();
					if (editorInput != null && editorInput instanceof JScriptEditorInput) {
						if (((JScriptEditorInput) editorInput).is(transaction)) {
							jScriptEditorInput = (JScriptEditorInput) editorInput;
							break;
						}
					}
				} catch(PartInitException e) {
					//ConvertigoPlugin.logException(e, "Error while retrieving the jscript transaction editor '" + editorRef.getName() + "'");
				}
			}
		}
		return jScriptEditorInput;
	}

	public IEditorPart getApplicationComponentEditor(IApplicationComponent iApp) {
		IEditorPart editorPart = null;
		IWorkbenchPage activePage = getActivePage();
		if (activePage != null && iApp != null) {
			IEditorReference[] editorRefs = activePage.getEditorReferences();
			for (int i=0;i<editorRefs.length;i++) {
				IEditorReference editorRef = (IEditorReference)editorRefs[i];
				try {
					IEditorInput editorRefInput = editorRef.getEditorInput();
					if (editorRefInput != null) {
						if (editorRefInput instanceof com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditorInput) {
							com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditorInput editorInput = GenericUtils.cast(editorRefInput);
							if (editorInput.is(GenericUtils.cast(iApp))) {
								editorPart = editorRef.getEditor(false);
								break;
							}
						} else if (editorRefInput instanceof com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditorInput) {
							com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditorInput editorInput = GenericUtils.cast(editorRefInput);
							if (editorInput.is(GenericUtils.cast(iApp))) {
								editorPart = editorRef.getEditor(false);
								break;
							}
						}
					}
				}
				catch(PartInitException e) {
					//ConvertigoPlugin.logException(e, "Error while retrieving the connector editor '" + editorRef.getName() + "'");
				}
			}
		}
		return editorPart;
	}

	/**
	 * Gets the editor associated with given connector.
	 * !!MUST BE CALLED IN A UI-THREAD!!
	 * @return ConnectorEditor : the found connector editor or null
	 */
	public ConnectorEditor getConnectorEditor(Connector connector) {
		ConnectorEditor connectorEditor = null;
		IWorkbenchPage activePage = PlatformUI
				.getWorkbench()
				.getActiveWorkbenchWindow()
				.getActivePage();
		if (activePage != null) {
			if (connector != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i=0;i<editorRefs.length;i++) {
					IEditorReference editorRef = (IEditorReference)editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if ((editorInput != null) && (editorInput instanceof ConnectorEditorInput)) {
							if (((ConnectorEditorInput)editorInput).is(connector)) {
								connectorEditor = (ConnectorEditor)editorRef.getEditor(true);
								break;
							}
						}
					}
					catch(PartInitException e) {
						ConvertigoPlugin.logException(e, "Error while retrieving the connector editor '" + editorRef.getName() + "'");
					}
				}
			}
		}
		return connectorEditor;
	}

	/**
	 * Gets the property descriptor of the selected property for this databaseObjectBeanInfo
	 * @param databaseObjectBeanInfo : BeanInfo of the selected databaseObject in the TreeExplorerView
	 * @return PropertyDescriptor
	 */
	private ShowInContext showInContext;
	public PropertyDescriptor getSelectedPropertyDescriptor(BeanInfo databaseObjectBeanInfo) {
		PropertyDescriptor propertyDescriptor = null;

		// gets the properties editor
		PropertySheet view = ConvertigoPlugin.getDefault().getPropertiesView();
		Control ctr = view.getCurrentPage().getControl();
		if (!(ctr instanceof Tree)) {
			view.show(showInContext);
			ctr = view.getCurrentPage().getControl();
		}
		if (ctr instanceof Tree) {
			showInContext = view.getShowInContext();
			Tree tree = (Tree) ctr;
			// gets the property selected in the property editor if one is selected
			TreeItem[] items = tree.getSelection();
			if (items.length > 0) {
				TreeItem selectedItem = items[0];

				// gets the local name of the selected property
				String text = selectedItem.getText();

				// gets the PropertyDescriptors of this databaseObject
				PropertyDescriptor[] descriptors = databaseObjectBeanInfo.getPropertyDescriptors();

				String displayName = null;
				int i = 0;

				// gets the PropertyDescriptor of the selected property
				while (i < descriptors.length && propertyDescriptor == null) {
					displayName = descriptors[i].getDisplayName();
					if (displayName.equals(text))
						propertyDescriptor = descriptors[i];
					i++;
				}
			}
		}
		return propertyDescriptor;
	}

	public IProject createProjectPluginResource(String projectName, String projectDir) throws CoreException {
		return createProjectPluginResource(projectName, projectDir, null);
	}

	public IProject createProjectPluginResource(String projectName, String projectDir, IProgressMonitor monitor) throws CoreException {
		IProject resourceProject;
		StringBuilder sb = new StringBuilder();
		try {
			sb.append("createProjectPluginResource for projet '" + projectName + "'");
			if (projectDir != null) {
				sb.append(" from: " + projectDir);
			}
			
			IWorkspace myWorkspace = ResourcesPlugin.getWorkspace();
			IWorkspaceRoot myWorkspaceRoot = myWorkspace.getRoot();
			resourceProject = myWorkspaceRoot.getProject(projectName);
			
			String existingProjectName;
			try {
				existingProjectName = resourceProject.getDescription().getName();
			} catch (Exception e) {
				try {
					resourceProject.delete(false, true, monitor);
				} catch (Exception e2) {
				}
				existingProjectName = null;
			}
			
			if (existingProjectName == null) {
				if (projectDir == null) {
					sb.append(" in the workspace folder.");
					resourceProject.create(monitor);
				} else {
					sb.append(" isn't in the workspace folder.");
					IPath projectPath = new Path(projectDir).makeAbsolute();
					IProjectDescription description = myWorkspace.newProjectDescription(projectName);
					description.setLocation(projectPath);
					resourceProject.create(description, monitor);
				}
				openProject(resourceProject, monitor);
			} else {
				if (resourceProject.isOpen() && !projectName.equals(existingProjectName)) {
					resourceProject.delete(false, true, monitor);
					return createProjectPluginResource(projectName, projectDir, monitor);
				} else {
					sb = null;
				}
			}
		} finally {
			if (sb != null) {
				logInfo(sb.toString());
			}
		}
		return resourceProject;
	}
	
	private void openProject(IProject iproject, IProgressMonitor monitor) throws CoreException {
		boolean doInit = !iproject.isOpen() || iproject.getSessionProperty(qnInit) == null;
		if (doInit) {
			if (iproject.getFilters().length == 0) {
				iproject.createFilter(
						IResourceFilterDescription.EXCLUDE_ALL
						| IResourceFilterDescription.FOLDERS
						| IResourceFilterDescription.INHERITABLE,
						new FileInfoMatcherDescription("org.eclipse.ui.ide.multiFilter", "1.0-name-matches-false-false-node_modules"),
						IResource.BACKGROUND_REFRESH, null);
			}
			iproject.open(monitor);
			iproject.setSessionProperty(qnInit, true);
		}
	}

	public IProject getProjectPluginResource(String projectName) throws CoreException {
		return getProjectPluginResource(projectName, null);
	}

	public IProject getProjectPluginResource(String projectName, IProgressMonitor monitor) throws CoreException {
		Project project = null;
		try {
			project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
		} catch (EngineException e1) {
		}
		if (project == null) {
			return null;
		}
		IProject resourceProject = createProjectPluginResource(projectName, project.getDirPath());
		if (resourceProject.exists()) {
			try {
				openProject(resourceProject, monitor);
			} catch (CoreException e) {
				// case of missing .project on existing project
				IPath projectPath = resourceProject.getLocation();
				IWorkspace myWorkspace = ResourcesPlugin.getWorkspace();
				IProjectDescription description = myWorkspace.newProjectDescription(projectName);
				description.setLocation(projectPath);
				resourceProject.delete(false, false, monitor);
				resourceProject.create(description, monitor);
				openProject(resourceProject, monitor);
			}
		}
		return resourceProject;
	}

	

	public void closeProjectPluginResource(String projectName) throws CoreException {
		IWorkspace myWorkspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot myWorkspaceRoot = myWorkspace.getRoot();
		IProject resourceProject = myWorkspaceRoot.getProject(projectName);

		if (resourceProject.exists()) {
			resourceProject.close(null);
		}
	}

	public void deleteProjectPluginResource(String projectName) throws CoreException {
		deleteProjectPluginResource(true, projectName);
	}

	public void deleteProjectPluginResource(boolean deleteContent, String projectName) throws CoreException {
		IWorkspace myWorkspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot myWorkspaceRoot = myWorkspace.getRoot();
		IProject resourceProject = myWorkspaceRoot.getProject(projectName);

		if (resourceProject.exists()) {
			resourceProject.delete(deleteContent, false, null);
		}
	}

	public void setShuttingDown(boolean b) {
		this.shuttingDown = b;
	}

	public boolean isShuttingDown() {
		return this.shuttingDown;
	}

	public void runRequestable(final String projectName, final Map<String, String[]> parameters) {
		if (!Engine.isStartFailed && Engine.isStarted) {
			parameters.put(Parameter.Project.getName(), new String[] {projectName});
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						InternalHttpServletRequest request;
						if (session == null || session.getMaxInactiveInterval() <= 1) {
							request = new InternalHttpServletRequest();
							session = request.getSession("studio");
						} else {
							request = new InternalHttpServletRequest(session);
						}

						InternalRequester requester = new InternalRequester(GenericUtils.<Map<String, Object>>cast(parameters), request);
						HttpSessionListener.checkSession(requester.getHttpServletRequest());
						requester.processRequest();
					} catch (Exception e) {
						logException(e, "Failed to run the requestable of project " + projectName);
					}
				}

			}).start();
		} else {
			logInfo("Cannot run the requestable of project " + projectName + ", the embedded tomcat is not correctly started.");
		}
	}

	public EmbeddedTomcat getEmbeddedTomcat() {
		return embeddedTomcat;
	}

	static public Properties decodePsc() throws PscException {
		return decodePscFromWorkspace(Engine.USER_WORKSPACE_PATH);
	}

	static private Properties decodePscFromWorkspace(String convertigoWorkspace) throws PscException {
		File pscFile = new File(convertigoWorkspace, "studio/psc.txt");
		if (pscFile.exists()) {
			try {
				String psc = FileUtils.readFileToString(pscFile, StandardCharsets.UTF_8);
				return decodePsc(psc);
			} catch (IOException e) {
				throw new PscException("Invalid PSC (failed to read the file '" + pscFile.getAbsolutePath() + "' because of a '" + e.getClass().getSimpleName() + " : " + e.getMessage() +"')!");
			}
		} else {
			throw new PscException("Invalid PSC (the file '" + pscFile.getAbsolutePath() + "' doesn't exist)!");
		}
	}

	static public Properties decodePsc(String psc)  throws PscException {
		if (psc.length() == 0) {
			throw new PscException("Invalid PSC (empty)!");
		} else {
			Properties properties = new Properties();
			try {
				String decipheredPSC = Crypto2.decodeFromHexString("registration", psc);

				if (decipheredPSC == null) {
					throw new PscException("Invalid PSC (unable to decipher)!");
				} else if (!decipheredPSC.startsWith("# PSC file")) {
					throw new PscException("Invalid PSC (wrong format)!");
				} else {

					try {
						properties.load(new StringReader(decipheredPSC));
					} catch (IOException e) {
						throw new PscException("Invalid PSC (cannot load properties)!");
					}
				}
			} catch (PscException e) {
				try {
					String decipheredPSC = SimpleCipher.decode(psc);

					properties.load(new StringReader(decipheredPSC));

					String server = properties.getProperty("server");
					String user = properties.getProperty("admin.user");
					String password = properties.getProperty("admin.password");

					if (server == null && user == null && password == null) {
						throw e;
					}

					if (server == null || user == null || password == null) {
						throw new PscException("Invalid PSC (incomplete data)!");
					}

					if (!user.equals(SimpleCipher.decode(password))) {
						throw new PscException("Invalid PSC (altered data)");
					}

					boolean bHttps = Boolean.parseBoolean(properties.getProperty("https"));

					properties.clear();

					DeploymentKey.adminUser.setValue(properties, 1, user);
					DeploymentKey.adminPassword.setValue(properties, 1, password);
					DeploymentKey.server.setValue(properties, 1, server);
					DeploymentKey.sslHttps.setValue(properties, 1, Boolean.toString(bHttps));
				} catch (PscException ex) {
					throw ex;
				} catch (Exception ex) {
					throw e;
				}
			}

			int i = 0;
			while (++i > 0) {
				if (i > 1 && !properties.containsKey(DeploymentKey.adminUser.key(i))) {
					i = -1;
				} else {
					for (DeploymentKey key : DeploymentKey.values()) {
						if (!properties.containsKey(key.key(i))) {
							if (!key.hasDefault()) {
								throw new PscException("Invalid PSC (altered data)");
							}
							key.setValue(properties, i);
						}
					}
				}
			}

			return properties;
		}
	}

	static public String makeAnonymousPsc() throws IOException {
		Properties properties = new Properties();
		String anonEmail = Long.toString(System.currentTimeMillis(), Character.MAX_RADIX) +
				Long.toString(Math.round(Math.random()) % Character.MAX_RADIX, Character.MAX_RADIX) +
				"@anonym.ous";

		DeploymentKey.adminUser.setValue(properties, 1, anonEmail);
		DeploymentKey.adminPassword.setValue(properties, 1, "");
		DeploymentKey.server.setValue(properties, 1, "");
		String psc;
		try (StringWriter sw = new StringWriter()) {
			PropertiesUtils.store(properties, sw, " PSC file");
			psc = Crypto2.encodeToHexString("registration", sw.toString());
		}
		return psc;
	}

	static public void runAtStartup(Runnable runnable) {
		if (Engine.isStarted) {
			runnable.run();
		} else {
			plugin.runAtStartup.add(runnable);
		}

	}

	private IProject getIProject(String projectName) {
		IWorkspace myWorkspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot myWorkspaceRoot = myWorkspace.getRoot();
		return myWorkspaceRoot.getProject(projectName);
	}

	private File getProjectFile(IProject iProject) {
		File file = null;
		try {
			String name = iProject.getName();
			IPath iPath = iProject.getLocation();
			if (iPath != null) {
				String sPath = iPath.toOSString();
				File folder = file = new File(sPath);
				file = new File(folder, "c8oProject.yaml");
				if (!file.exists()) {
					file = new File(folder, name + ".xml");
				}
				var fname = DatabaseObjectsManager.getProjectName(file);
				if (fname == null) {
					file = null;
				} else if (!name.equals(fname)) {
					Engine.logStudio.warn("project name isn't the same '" + name + "' for in this file '" + file + "'.");
				}
			}
		} catch (EngineException e) {
			file = null;
		}
		return file;
	}

	@Override
	public File getProject(String projectName) {
		IProject iProject = getIProject(projectName);
		return getProjectFile(iProject);
	}

	public boolean isProjectOpened(String projectName) {
		boolean isOpen = false;
		try {
			IProject iProject = getIProject(projectName);
			if (iProject != null && iProject.exists()) {
				File projectFile = getProjectFile(iProject);
				if (projectFile == null || !projectFile.exists()) {
					iProject.delete(false, true, null);
					iProject = createProjectPluginResource(projectName, null);
					projectFile = getProjectFile(iProject);
				}
				isOpen = projectFile != null && projectFile.exists() && iProject.isOpen();
			}
		} catch (Exception e) {
			logWarning(e, "Error when checking if '" + projectName + "' is open", false);
		}
		return isOpen;
	}

	@Override
	public void declareProject(String projectName, File projectFile) {
		if (projectFile != null && projectFile.exists()) {
			try {
				createProjectPluginResource(projectName, projectFile.getParentFile().getAbsolutePath());
			} catch (CoreException e) {
				ConvertigoPlugin.logException(e, "Failed to declare the project from " + projectFile.getAbsolutePath());
			}
		}
	}

	@Override
	public boolean canOpen(String projectName) {
		if ("true".equals(ConvertigoPlugin.getProperty(PREFERENCE_ENGINE_LOAD_ALL_PROJECTS))) {
			return true;
		}
		return isProjectOpened(projectName);
	}

	@Override
	public Map<String, File> getProjects(boolean checkOpenable) {
		IWorkspace myWorkspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot myWorkspaceRoot = myWorkspace.getRoot();
		IProject[] iProjects = myWorkspaceRoot.getProjects();
		Map<String, File> projects = new HashMap<>(iProjects.length);
		checkOpenable &= !"true".equals(ConvertigoPlugin.getProperty(PREFERENCE_ENGINE_LOAD_ALL_PROJECTS));
		for (IProject iProject: iProjects) {
			if (!checkOpenable || iProject.isOpen()) {
				File file = getProjectFile(iProject);
				if (file != null && file.exists()) {
					projects.put(iProject.getName(), file);
				}
			}
		}
		return projects;
	}
	
	public void refreshPropertiesView() {
		PropertySheet view = getPropertiesView();
		if (view != null) {
			PropertySheetPage page = (PropertySheetPage) view.getCurrentPage();
			if (page != null) {
				page.refresh();
			}
		}
	}
	
	public void refreshPaletteView() {
		PaletteView paletteView = getPaletteView();
		if (paletteView != null) {
			paletteView.refresh();
		}
	}
	
	public void projectLoaded(Project project) {
		asyncExec(() -> {
			ProjectExplorerView pew = getProjectExplorerView();
			if (pew == null) {
				return;
			}
			try {
				TreeObject treeProject = pew.getProjectRootObject(project.getName());
				if (treeProject == null) {
					pew.importProjectTreeObject(project.getName());
				} else if (!project.equals(treeProject.getObject())) {
					if (treeProject instanceof ProjectTreeObject) {
						// should not happened
						Engine.logStudio.warn("[projectLoaded] Project '" + project.getName() + "' loaded and project in ProjectTree is different: reloading the ProjectTree!");
					} else if (treeProject instanceof UnloadedProjectTreeObject) {
						// case of standard ProjectLoadingJob in progress or case of unloaded project which part of another project dependencies
						Engine.logStudio.info("[projectLoaded] Unloaded project '" + project.getName() + "' needs to be loaded or reloaded in TreeView");
					}
					pew.reloadProject(treeProject);
				}
			} catch (Exception e) {
			}
		});
	}
	
	public void launchStartupPage(boolean autoClose) {
		try {
			String username = "n/a";
			String site = "n/a";
			try {
				Properties properties = decodePsc();
				username = properties.getProperty("owner.email", DeploymentKey.adminUser.value(properties, 1));
				site = properties.getProperty("deploy.1.server", "n/a").replace("(.*?)\\..*", "$1");
			} catch (Exception e) {}
			getActivePage().openEditor(StartupEditor.makeInput(username, site, autoClose), StartupEditor.ID);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void reloadProject(String name) throws EngineException {
		EngineException[] ex = {null};
		syncExec(() -> {
			try {
				var pev = getProjectExplorerView();
				pev.reloadProject(pev.getProjectRootObject(name));
			} catch (EngineException e) {
				ex[0] = e;
			}
		});
		if (ex[0] != null) {
			throw ex[0];
		}
	}

	@Override
	public void renameProject(String oldName, String newName) {
		var project = getIProject(oldName);
		try {
			var desc = project.getDescription();
			desc.setName(newName);
			project.move(desc, IResource.FORCE | IResource.SHALLOW, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
}
