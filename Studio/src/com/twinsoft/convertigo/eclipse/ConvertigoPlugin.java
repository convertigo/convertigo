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

package com.twinsoft.convertigo.eclipse;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IViewPart;
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
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.views.properties.PropertySheet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.core.Pool;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.actions.SetupAction;
import com.twinsoft.convertigo.eclipse.dialogs.GlobalsSymbolsWarnDialog;
import com.twinsoft.convertigo.eclipse.dialogs.ProjectDeployErrorDialog;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditorInput;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTransactionEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ClipboardManager;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectManager;
import com.twinsoft.convertigo.eclipse.views.references.ReferencesView;
import com.twinsoft.convertigo.eclipse.views.sourcepicker.SourcePickerView;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.ProductVersion;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.Crypto2;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.SimpleCipher;
import com.twinsoft.util.Log;

/**
 * The main plugin class to be used in the desktop.
 */
public class ConvertigoPlugin extends AbstractUIPlugin implements IStartup {
	
	public static final String PLUGIN_UNIQUE_ID = "com.twinsoft.convertigo.eclipse.ConvertigoPlugin"; //$NON-NLS-1$
	
	public static final String PLUGIN_PERSPECTIVE_ID = "com.twinsoft.convertigo.eclipse.ConvertigoPerspective.API"; //$NON-NLS-1$

	public static ProjectManager projectManager = null;
	
	public static ClipboardManager clipboardManagerDND = null;
	public static ClipboardManager clipboardManagerSystem = null;
	
	public static DeploymentConfigurationManager deploymentConfigurationManager = null;
		
    public static final String SYSTEM_PROP_PREFIX = "convertigo.studio.";
    
    public static final String PREFERENCE_LOG_LEVEL = "log.level";
    public static final String PREFERENCE_TREE_HIGHLIGHT_DETECTED = "tree.highlight.detected";
    public static final String PREFERENCE_OPENED_CONSOLES = "opened.consoles";
    public static final String PREFERENCE_TRACEPLAYER_PORT = "traceplayer.port";
    public static final String PREFERENCE_IGNORE_NEWS = "news.ignore";
    public static final String PREFERENCE_SHOW_ENGINE_INTO_CONSOLE = "engine.into.console";
    public static final String PREFERENCE_ENGINE_LOAD_ALL_PROJECTS = "engine.load.all.projects";
    public static final String PREFERENCE_LOCAL_BUILD_ADDITIONAL_PATH = "localBuild.additionalPath";
    
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
    		mainShell = getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();
    	}
    	return mainShell;
    }
    
    public static class PscException extends Exception {
		private static final long serialVersionUID = -3828463232797723301L;

		public PscException(String cause) {
    		super(cause);
    	}
    }
    
    public static String getProperty(String key) {
    	IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore();
    	logDebug2("Looking for property : \"" + key + "\"");

    	String result = preferenceStore.getString(key);
    	
        logDebug("==> Getting property " + key + ": \"" + result + "\"");
        
        return result;
    }
    
    public static void setProperty(String key, String value) {
    	IPreferenceStore preferenceStore = ConvertigoPlugin.getDefault().getPreferenceStore();
    	preferenceStore.setValue(key, value);
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
	
	//Get IProjects in memory 
	private static Map<String, IProject> cacheIProject = new Hashtable<String, IProject>();
	
	public static void logException(Throwable e, String message) {
		logException(e, message, true);
	}
	
	public static void logDeployException(Throwable e, String errorMessage, String stackTrace) {
		log.log(new Status(Status.ERROR, ConvertigoPlugin.PLUGIN_UNIQUE_ID, Status.OK, errorMessage, e));
		projectDeployErrorDialog(errorMessage, stackTrace);
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
	
	public static void logWarning(String message, boolean dialog) {
		logWarning(null, message, dialog);
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
		
		getDisplay().syncExec(
			new Runnable() {
				public void run() {
					try {
						int level = ModalContext.getModalLevel();
						if (level > 0) {
							// prevents double modal windows: dead lock on linux/gtk studio
							getDisplay().syncExec(this);
							return;
						} 
						
						GlobalsSymbolsWarnDialog dialogGlobalSymbols = new GlobalsSymbolsWarnDialog(getDisplay().getActiveShell(), projectName,
								objectName, objectType,
								propertyName, propertyValue,
								undefinedSymboles, showCheckBox);
						dialogGlobalSymbols.open();
						
						result[0] = dialogGlobalSymbols.getCreateAction();
						result[1] = dialogGlobalSymbols.getCheckButtonSelection();
					} catch (Exception e){
						ConvertigoPlugin.logException(e, "Error while trying to open warning global symbols box");
					}
				}
			}
		);
		return result;
	}

	public static void infoMessageBox(final String message) {
		ConvertigoPlugin.messageBoxWithoutReturnCode(message, SWT.OK | SWT.ICON_INFORMATION);
	}

	private static void messageBoxWithoutReturnCode(final String message, int options) {
		final Display display = getDisplay();
		
		Runnable runnable = new Runnable() {
			public void run() {
				try {
					messageBox(null, message, SWT.OK | SWT.ICON_INFORMATION);
				}
				catch (Exception e){
					ConvertigoPlugin.logException(e, "Error while trying to open message box");
				}
			};
		};
		
		if (display.getThread() != Thread.currentThread()) {
			display.asyncExec(runnable);		
		} else {
			display.syncExec(runnable);
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
	
	public static void projectDeployErrorDialog(String message, String stackTrace) {
		final String errorMessage = message;
		final String causeStackTrace = stackTrace;
		final Display display = getDisplay();
		
		display.asyncExec(new Runnable() {
			
			public void run() {
				try {
		        	ProjectDeployErrorDialog projectDeployErrorDialog = new ProjectDeployErrorDialog(getMainShell(), errorMessage, causeStackTrace);
		        	projectDeployErrorDialog.open();
		    		if (projectDeployErrorDialog.getReturnCode() != Window.CANCEL) {
		    		}
				}
				catch (Exception e){
					ConvertigoPlugin.logException(e, "Error while trying to open project deploy dialog", false);
				}
			};
			
		});
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
	
	private Button checkBox, closeButton;

	private void displayWaitScreen() {
		final Display display = getDisplay();
		
		display.asyncExec(new Runnable() {
			public void run() {
				
				try {
					
					final Shell shell = new Shell(display, SWT.BORDER | SWT.APPLICATION_MODAL);
					final long[] timeout = {System.currentTimeMillis() + 10000};
					
					GridLayout gridLayout = new GridLayout();
					gridLayout.numColumns = 1;
					gridLayout.horizontalSpacing = 0;
					gridLayout.verticalSpacing = 0;
					gridLayout.marginHeight = 0;
					gridLayout.marginWidth = 0;
					shell.setLayout(gridLayout);
					
					shell.setText("Convertigo Studio");
					
					Image image = getStudioIcon("icons/splash_wait_rss.png");
					
					Composite compositeHeader = new Composite(shell, SWT.NONE);
					compositeHeader.setBackgroundImage(image);
					compositeHeader.setLayout(new GridLayout());
					
					GridData gridData = new GridData ();
					gridData.horizontalAlignment = GridData.FILL;
					gridData.verticalAlignment = GridData.VERTICAL_ALIGN_FILL;
					gridData.heightHint = image.getBounds().height;
					compositeHeader.setLayoutData(gridData);
					
					Composite compositeBar = new Composite(compositeHeader, SWT.NONE);
					FillLayout fillLayout = new FillLayout();
					fillLayout.marginHeight = 0;
					fillLayout.marginWidth = 0;
					compositeBar.setLayout(fillLayout);

					gridData = new GridData();
					gridData.horizontalIndent = 172;
					gridData.verticalIndent = 65;
					gridData.widthHint = 300;
					compositeBar.setLayoutData(gridData);
						
					ProgressBar progressBar = new ProgressBar(compositeBar, SWT.INDETERMINATE);
					
					//initialize browser
					initializeBrowser(shell, new ProgressListener() {
						
						public void completed(ProgressEvent event) {
							timeout[0] = 0;
						}
						
						public void changed(ProgressEvent event) {
						}
					});
				
					Composite toolComposite = new Composite(shell, SWT.NONE);
					gridLayout = new GridLayout();
					gridLayout.numColumns = 2;
					gridLayout.marginHeight = 3;
					gridLayout.marginWidth = 10;
					toolComposite.setLayout(gridLayout);
					
					gridData = new GridData();
					gridData.horizontalAlignment = GridData.FILL;
					gridData.grabExcessHorizontalSpace = true;
					toolComposite.setLayoutData(gridData);
					
					checkBox = new Button(toolComposite, SWT.CHECK);
					checkBox.setText("Dismiss automatically");		
					if (ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_IGNORE_NEWS).equalsIgnoreCase("true")) {
						checkBox.setSelection(true);
					}
					
					image = getStudioIcon("icons/studio/unloadable_project.gif");
					
					closeButton = new Button(toolComposite, SWT.PUSH);
					closeButton.setText("Wait...");
					closeButton.setEnabled(false);
					closeButton.setImage(image);
					
					gridData = new GridData();
					gridData.grabExcessHorizontalSpace = true;
					gridData.horizontalAlignment = GridData.END;
					closeButton.setLayoutData(gridData);
					

					int w = 664;
					int h = 400;
					try {
						// mod jmc 31/07/2013
						
						int x = 0;
						int y = 0;
						 
						Point pt = display.getCursorLocation();
					    Monitor [] monitors = display.getMonitors();

					    for (int i= 0; i<monitors.length; i++) {
					          if (monitors[i].getBounds().contains(pt)) {
					             Rectangle rect = monitors[i].getClientArea();
					             
					             if (rect.x < 0)
					         		x = ((rect.width - w) / 2) + rect.x;
					             else
					         		x = (rect.width - w) / 2;

					             if (rect.y < 0)
					         		y = ((rect.height - h) / 2) + rect.y;
					             else
					         		y = (rect.height - h) / 2;
					             
					             break;
					          }
					    }

					    shell.setBounds(x, y, w, h);						
						
						
					}
					catch (Exception e) {
						shell.setBounds(100, 100, w, h);
					}
					shell.open();
					
					while (!shell.isDisposed() && !Engine.isStartFailed && !Engine.isStarted) {
						if (!display.readAndDispatch()) {
							Thread.sleep(100);
						}
					}
					
					progressBar.dispose();
					compositeBar.dispose();
					
					image = getStudioIcon("icons/splash_ready_rss.png");
					compositeHeader.setBackgroundImage(image);
					
					gridData = new GridData ();
					gridData.horizontalAlignment = GridData.FILL;
					gridData.verticalAlignment = GridData.VERTICAL_ALIGN_FILL;
					gridData.heightHint = 75;
					compositeHeader.setLayoutData(gridData);
					
					closeButton.addSelectionListener(new SelectionListener() {
						
						public void widgetDefaultSelected(SelectionEvent e) {		
						}
						
						public void widgetSelected(SelectionEvent e) {
							if (checkBox.getSelection()) {
								ConvertigoPlugin.setProperty(ConvertigoPlugin.PREFERENCE_IGNORE_NEWS, "true");
							}
							else {
								ConvertigoPlugin.setProperty(ConvertigoPlugin.PREFERENCE_IGNORE_NEWS, "false");
							}
							shell.close();
						}
						
					});
					
					while (!shell.isDisposed() && System.currentTimeMillis() < timeout[0]) {
						if (!display.readAndDispatch()) {
							Thread.sleep(100);
						}
					}
					
					closeButton.setText("Close");
					closeButton.setEnabled(true);
					
					shell.layout();
					
					if (ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_IGNORE_NEWS).equalsIgnoreCase("true")) {
						shell.close();
					}
					
					ProjectExplorerView pew = getProjectExplorerView();
					pew.initialize();
				}
				catch (Exception e) {
					ConvertigoPlugin.logException(e, "Error during splash wait screen");
				}
			};
		});
	}

	private void initializeBrowser(Composite parent, ProgressListener progressListener) throws IOException {
		
		final Browser browser;
		try {
			browser = new Browser(parent, Engine.isLinux() ? SWT.MOZILLA : SWT.NONE);
		} catch (SWTError e) {
			System.out.println("Could not instantiate Browser: " + e.getMessage());
			return;
		}
		
		GridData gridData = new GridData();
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		browser.setLayoutData(gridData);
		
		String username = "n/a";
		try {
			Properties properties = decodePsc();
			username = DeploymentKey.adminUser.value(properties, 1);
		} catch (Exception e) {}

		String url = "http://www.convertigo.com/index.php?option=com_content&view=article&id=269&Itemid=364&lang=en&ConvertigoStudio=true";
		url += "&user=" + URLEncoder.encode(username, "utf8");
		url += "&version=" + URLEncoder.encode(ProductVersion.fullProductVersion, "utf8");
		
		browser.addProgressListener(progressListener);
		browser.setUrl(url);
	}
	
	/**
	 * This method is called upon plug-in activation
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);

		// Version check
		if (!com.twinsoft.convertigo.eclipse.Version.productVersion.equals(com.twinsoft.convertigo.beans.Version.productVersion)) {
			throw new Exception("The product version numbers of Eclipse Plugin and Objects libraries are differents.");
		} else if (!com.twinsoft.convertigo.eclipse.Version.productVersion.equals(com.twinsoft.convertigo.engine.Version.productVersion)) {
			throw new Exception("The product version numbers of Eclipse Plugin and Engine libraries are differents.");
		}
		
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
		
		studioLog = new Log(ConvertigoPlugin.getDefault().studioConsoleStream);
		studioLog.logLevel = Log.LOGLEVEL_DEBUG;
		
		try {
			studioLog.logLevel = new Integer(ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_LOG_LEVEL)).intValue();
		}
		catch(NumberFormatException e) {
			studioLog.warning("Unable to retrieve the log level; using default log level (4).");
		}

		studioLog.message("Starting the Convertigo studio eclipse plugin");

		try {
			highlightDetectedObject = new Boolean(ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_TREE_HIGHLIGHT_DETECTED)).booleanValue();
		}
		catch(NumberFormatException e) {
			studioLog.warning("Unable to retrieve the highlight option; using default highlight option (true).");
		}

		// In STUDIO, the Convertigo User Workspace is in the current Eclipse Workspace/.metadata/.plugins/com.twinsoft.convertigo.studio
		Engine.USER_WORKSPACE_PATH = getDefault().getStateLocation().toFile().getCanonicalPath();

		// In STUDIO, the Convertigo Projects directory is the current Eclipse Workspace
		Engine.PROJECTS_PATH = ResourcesPlugin.getWorkspace().getRoot().getRawLocation().toFile().getCanonicalPath();
		
//		checkPre_6_2_0_Migration();
		
		// Adds listeners
		addListeners();
		
		DatabaseObjectsManager.openableProject = new DatabaseObjectsManager.OpenableProject() {
			
			@Override
			public boolean canOpen(String projectName) {
				if ("true".equals(ConvertigoPlugin.getProperty(PREFERENCE_ENGINE_LOAD_ALL_PROJECTS))) {
					return true;
				}
				return isProjectOpened(projectName);
			}
			
		};
		
		final Exception afterPscException[] = { null };
		final Runnable afterPscOk = new Runnable() {

			public void run() {
				try {
					// Create embedded Tomcat
					studioLog.message("Starting the embedded Tomcat");
					System.setProperty("org.apache.commons.logging.log", "org.apache.commons.logging.impl.Jdk14Logger");

					Path path = new Path("tomcat");
					URL tomcatHomeUrl = FileLocator.find(context.getBundle(), path, null);
					
					String xulrunner_url = System.getProperty("org.eclipse.swt.browser.XULRunnerPath");
					if (xulrunner_url == null || xulrunner_url.equals("")) {
						Bundle[] bundles = Platform.getFragments(context.getBundle());
						for (Bundle bundle : bundles) {
							String symbolicName = bundle.getSymbolicName();
							if (symbolicName.startsWith("com.twinsoft.convertigo.studio.xulrunner")) {
								URL url = FileLocator.find(bundle, new Path("xulrunner"), null);
								xulrunner_url = FileLocator.toFileURL(url).getPath();
								System.setProperty("org.eclipse.swt.browser.XULRunnerPath", xulrunner_url);
								break;
							}
						}
					}

					String tomcatHome = FileLocator.toFileURL(tomcatHomeUrl).getPath();

					int index = (System.getProperty("os.name").indexOf("Windows") != -1) ? 1 : 0;
					tomcatHome = tomcatHome.substring(index);

					embeddedTomcat = new EmbeddedTomcat(tomcatHome);
					
					configureDeployConfiguration();

					displayWaitScreen();

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
								Display.getDefault().asyncExec(runnable);
							}
							runAtStartup.clear();
						}
						
					}, "Wait Embedded Tomcat started").start();
					
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
			Display display = getDisplay();
			display.asyncExec(new Runnable() {
				
				public void run() {
					try {
						boolean success = SetupAction.runSetup();
						if (success) {
							Engine.isStartFailed = false;
							new Thread(afterPscOk, "Start embedded Tomcat").start();
						} else {
							IWorkbench workbench = PlatformUI.getWorkbench();
							workbench.close();
						}
					} catch (Throwable t) {
						studioLog.exception(t, "Failure during the Convertigo setup wizard");
					}
				}
				
			});
		}
        
		studioLog.message("Convertigo studio started");
	}

	private ConvertigoWorkbenchListener workbenchListener = null;
	private ConvertigoWindowListener windowListener = null;
	private ConvertigoPartListener partListener = null;
	private ConvertigoPerspectiveListener perspectiveListener = null;
	
	public void addListeners() {
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
			
		}
		catch (IllegalStateException e) {
			studioLog.error("Could not add listeners to plugin."+ e.getMessage());
		}
		
	}
	
	public void earlyStartup() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getDisplay().asyncExec(new Runnable() { 
			
			public void run() {
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
	
	static public void setLogLevel(int logLevel) {
		studioLog.logLevel = logLevel;
	}
	
	static public int getLogLevel() {
		return studioLog.logLevel;
	}
	
	private static boolean 	highlightDetectedObject;
	
	public static void setHighlightDetectedObject(boolean highlight) {
		highlightDetectedObject = highlight;
	}

	public static boolean getHighlightDetectedObject() {
		return highlightDetectedObject;
	}

	private static boolean showEnginIntoConsole;
	
	public static void setShowEngineIntoConsole(boolean show) {
		showEnginIntoConsole = show;
	}

	public static boolean getShowEngineIntoConsole() {
		return showEnginIntoConsole;
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
		
		cacheIProject.clear();
		
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
		
		if (embeddedTomcat != null)
			embeddedTomcat.stop();
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

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = ConvertigoPlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

    private Map<String, Image> icons = new HashMap<String, Image>();

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
        else {
            iconBaseName = "default";
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

	public ConsolePipes consolePipes = null;
	
	public MessageConsole connectorConsole;
	public MessageConsole engineConsole;
	public MessageConsole studioConsole;
	public MessageConsole stdoutConsole;
	public MessageConsole debugConsole;
	public MessageConsole traceConsole;
	public MessageConsole tomcatConsole;
	
	public MessageConsoleStream studioConsoleStream;
	public MessageConsoleStream engineConsoleStream;
	public MessageConsoleStream stdoutConsoleStream;
	public MessageConsoleStream stderrConsoleStream;
	public MessageConsoleStream traceConsoleStream;
	public MessageConsoleStream debugConsoleStream;
	
	private Color stderrConsoleStreamColor = new Color(null, 200, 0, 0);
	
	private static int TAB_WIDTH = 5;
	private void createConsoles() {
		ConsolePlugin consolePlugin = ConsolePlugin.getDefault();
		IConsoleManager consoleManager = consolePlugin.getConsoleManager();

		studioConsole = new MessageConsole("Studio", ImageDescriptor.createFromFile(getClass(), "/consoles/convertigo.gif"));
		studioConsole.setTabWidth(TAB_WIDTH);
		studioConsoleStream = studioConsole.newMessageStream();
		
		stdoutConsole = new MessageConsole("Stdout", ImageDescriptor.createFromFile(getClass(), "/consoles/stdout.gif"));
		stdoutConsole.setTabWidth(TAB_WIDTH);
		stdoutConsoleStream = stdoutConsole.newMessageStream();
		stderrConsoleStream = stdoutConsole.newMessageStream();
		stderrConsoleStream.setColor(stderrConsoleStreamColor);
		
		traceConsole = new MessageConsole("Trace [no trace file]", ImageDescriptor.createFromFile(getClass(), "/consoles/trace.gif"));
		traceConsole.setTabWidth(TAB_WIDTH);
		traceConsoleStream = traceConsole.newMessageStream();
		
		engineConsole = new MessageConsole("Engine", ImageDescriptor.createFromFile(getClass(), "/consoles/engine.gif"));
		engineConsole.setTabWidth(TAB_WIDTH);
		engineConsoleStream = engineConsole.newMessageStream();
		
		debugConsole = new MessageConsole("Debug [no current session]", ImageDescriptor.createFromFile(getClass(), "/consoles/tomcat.gif"));
		debugConsole.setTabWidth(TAB_WIDTH);
		debugConsoleStream = debugConsole.newMessageStream();

		consoleManager.addConsoles(new IConsole[] {
			engineConsole,
			stdoutConsole,
			traceConsole,
			debugConsole,
			studioConsole,
		});		
		
		// Restore previously opened console views
		/*
		final String consoles = properties.getProperty(ConvertigoPlugin.ConfigurationProperties.OPENED_CONSOLES);
		final IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				String[] array = consoles.split(",");
				//System.out.println(consoles);//
				
				int counter = 1;
				for (int i=0; i<array.length; i++) {
					String consoleName = array[i];
	            	
		            try {
		            	ConsoleView consoleView = null;
						
		            	// restore console
		            	if (consoleName.indexOf("connector") == 0) {
		            		consoleView = (ConsoleView)page.showView(IConsoleConstants.ID_CONSOLE_VIEW, "Console View #" + counter++, 1);
							consoleView.consolesAdded(new IConsole[] { connectorConsole});
		            	}
						else if (consoleName.indexOf("studio") == 0) {
							consoleView = (ConsoleView)page.showView(IConsoleConstants.ID_CONSOLE_VIEW, "Console View #" + counter++, 1);
							consoleView.consolesAdded(new IConsole[] { studioConsole});
						}
						else if (consoleName.indexOf("engine") == 0) {
							consoleView = (ConsoleView)page.showView(IConsoleConstants.ID_CONSOLE_VIEW, "Console View #" + counter++, 1);
							consoleView.consolesAdded(new IConsole[] { engineConsole});
						}
						else if (consoleName.indexOf("tomcat") == 0) {
							consoleView = (ConsoleView)page.showView(IConsoleConstants.ID_CONSOLE_VIEW, "Console View #" + counter++, 1);
							consoleView.consolesAdded(new IConsole[] { tomcatConsole});
						}
						else if (consoleName.indexOf("trace") == 0) {
							consoleView = (ConsoleView)page.showView(IConsoleConstants.ID_CONSOLE_VIEW, "Console View #" + counter++, 1);
							consoleView.consolesAdded(new IConsole[] { traceConsole});
						}
						else if (consoleName.indexOf("debug") == 0) {
							consoleView = (ConsoleView)page.showView(IConsoleConstants.ID_CONSOLE_VIEW, "Console View #" + counter++, 1);
							consoleView.consolesAdded(new IConsole[] { debugConsole});
						}
						else if (consoleName.indexOf("stdout") == 0) {
							consoleView = (ConsoleView)page.showView(IConsoleConstants.ID_CONSOLE_VIEW, "Console View #" + counter++, 1);
							consoleView.consolesAdded(new IConsole[] { stdoutConsole});
						}
					} catch (PartInitException e) {
						ConvertigoPlugin.logException(e, "Unexpected exception");
					}

					if (counter >= 7)
		            	break;
				}
			}});
		*/
		consolePipes = new ConsolePipes();
	}
	
	/*private IEditorReference[] editorReferences = new IEditorReference[] {};
	
	public IEditorReference[] getEditorReferences() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getDisplay().syncExec(new Runnable() {
			public void run() {
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				editorReferences = page.getEditorReferences();
			}});
		return editorReferences;
	}

	
	private IViewReference[] viewReferences = new IViewReference[] {};
	
	public IViewReference[] getViewReferences() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getDisplay().syncExec(new Runnable() {
			public void run() {
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
				if (window != null) {
					IWorkbenchPage page = window.getActivePage();
					viewReferences = page.getViewReferences();
				}
			}});
		return viewReferences;
	}*/
	
	private IWorkbenchPage getActivePage() {
		return PlatformUI
				.getWorkbench()
				.getActiveWorkbenchWindow()
				.getActivePage();
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
	 * Gets the jscript editor associated with given transaction.
	 * !!MUST BE CALLED IN A UI-THREAD!!
	 * @return IEditorPart : the found jscript editor or null
	 */
	public IEditorPart getJscriptTransactionEditor(Transaction transaction) {
		IEditorPart editorPart = null;
		IWorkbenchPage activePage = getActivePage();
		if (activePage != null) {
			if (transaction != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i=0;i<editorRefs.length;i++) {
					IEditorReference editorRef = (IEditorReference)editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if ((editorInput != null) && (editorInput instanceof JscriptTransactionEditorInput)) {
							if (((JscriptTransactionEditorInput)editorInput).transaction.equals(transaction)) {
								editorPart = editorRef.getEditor(false);
								break;
							}
						}
					}
					catch(PartInitException e) {
						//ConvertigoPlugin.logException(e, "Error while retrieving the jscript transaction editor '" + editorRef.getName() + "'");
					}
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
							if (((ConnectorEditorInput)editorInput).connector.equals(connector)) {
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
	public PropertyDescriptor getSelectedPropertyDescriptor(BeanInfo databaseObjectBeanInfo) {
		PropertyDescriptor propertyDescriptor = null;
		
		// gets the properties editor
		PropertySheet view = ConvertigoPlugin.getDefault().getPropertiesView();
		Tree tree = (Tree) view.getCurrentPage().getControl();
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
		return propertyDescriptor;
	}
	
	public IProject createProjectPluginResource(String projectName) throws CoreException {
		return createProjectPluginResource(projectName, null);
	}
	
	public boolean isProjectOpened(String projectName) {
		boolean isOpen = false;
		try {
			IProject resourceProject = ConvertigoPlugin.getDefault().createProjectPluginResource(projectName);
			isOpen = resourceProject != null && resourceProject.isOpen();
		} catch (CoreException e) {
			logWarning(e, "Error when checking if '" + projectName + "' is open", false);
		}
		return isOpen;
	}
	
	public IProject createProjectPluginResource(String projectName, IProgressMonitor monitor) throws CoreException {
		IWorkspace myWorkspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot myWorkspaceRoot = myWorkspace.getRoot();
		IProject resourceProject = myWorkspaceRoot.getProject(projectName);
		
		if (!resourceProject.exists()) {		
			if(myWorkspaceRoot.getLocation().toFile().equals(new Path(Engine.PROJECTS_PATH).toFile())){
				logDebug("createProjectPluginResource : project is in the workspace folder");
				
				resourceProject.create(monitor);
			}else{
				logDebug("createProjectPluginResource: project isn't in the workspace folder");
		
				IPath projectPath = new Path(Engine.PROJECTS_PATH + "/" + projectName).makeAbsolute();
				IProjectDescription description = myWorkspace.newProjectDescription(projectName);
				description.setLocation(projectPath);
				resourceProject.create(description, monitor);
			}
		}
		
		return resourceProject;
	}

	public IProject getProjectPluginResource(String projectName) throws CoreException {
		return getProjectPluginResource(projectName, null);
	}
	
	public IProject getProjectPluginResource(String projectName, IProgressMonitor monitor) throws CoreException {
		if (cacheIProject.containsKey(projectName)) return (IProject)cacheIProject.get(projectName);
		
		IProject resourceProject = createProjectPluginResource(projectName);
		if (resourceProject.exists()) {
			resourceProject.refreshLocal(IResource.DEPTH_INFINITE, monitor);			
			if (!resourceProject.isOpen())
				resourceProject.open(monitor);
		}
		cacheIProject.put(projectName, resourceProject);
		
		return resourceProject;
	}
	
	public void moveProjectPluginResource(String projectName, String newName) throws CoreException {
		cacheIProject.remove(projectName);
		IWorkspace myWorkspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot myWorkspaceRoot = myWorkspace.getRoot();
		IProject resourceProject = myWorkspaceRoot.getProject(projectName);
		
		if (resourceProject.exists()) {
			IPath newProjectPath = new Path(Engine.PROJECTS_PATH + "/" + newName).makeAbsolute();
        	IProjectDescription description = myWorkspace.newProjectDescription(newName);
			description.setLocation(newProjectPath);
        	resourceProject.move(description, false, null);
		}
	}

	public void closeProjectPluginResource(String projectName) throws CoreException {
		cacheIProject.remove(projectName);
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
		cacheIProject.remove(projectName);
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
						if (session == null) {
							request = new InternalHttpServletRequest();
							session = request.getSession();
						} else {
							request = new InternalHttpServletRequest(session);
						}
						
						new InternalRequester(GenericUtils.<Map<String, Object>>cast(parameters), request).processRequest();
					} catch (Exception e) {
						logException(e, "Failed to run the requestable of project " + projectName);
					}
				}
	        	
	        }).start();
		} else {
			logInfo("Cannot run the requestable of project " + projectName + ", the embedded tomcat is not correctly started.");
		}
	}
	
	static private Properties decodePsc() throws PscException {
		return decodePscFromWorkspace(Engine.USER_WORKSPACE_PATH);
	}
	
	static public Properties decodePscFromWorkspace(String convertigoWorkspace) throws PscException {
		File pscFile = new File(convertigoWorkspace, "studio/psc.txt");
		if (pscFile.exists()) {
			try {
				String psc = FileUtils.readFileToString(pscFile, "utf-8");
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
						properties.load(IOUtils.toInputStream(decipheredPSC, "utf8"));
					} catch (IOException e) {
						throw new PscException("Invalid PSC (cannot load properties)!");
					}
				}
			} catch (PscException e) {
				try {
					String decipheredPSC = SimpleCipher.decode(psc);
					
					properties.load(IOUtils.toInputStream(decipheredPSC, "utf8"));
					
					String server = properties.getProperty("server");
					String user = properties.getProperty("admin.user");
					String password = properties.getProperty("admin.password");
					
					if (server == null && user == null && password == null) {
						throw e;
					}
					
					if (server == null || user == null || password == null) {
						throw new PscException("Invalid PSC (incomplet data)!");
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
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		properties.store(os, " PSC file");
		String psc = new String(os.toByteArray(), "iso8859-1");
		psc = Crypto2.encodeToHexString("registration", psc);
		
		return psc;
	}
	
	static public void runAtStartup(Runnable runnable) {
		if (Engine.isStarted) {
			runnable.run();
		} else {
			plugin.runAtStartup.add(runnable);
		}
		
	}
}
