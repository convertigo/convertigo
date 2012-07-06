package com.twinsoft.convertigo.eclipse.wizards.setup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.util.Crypto2;

public class SetupWizard extends Wizard {

	private static final String PROP_VM = "eclipse.vm"; //$NON-NLS-1$
	private static final String PROP_VMARGS = "eclipse.vmargs"; //$NON-NLS-1$
	private static final String PROP_COMMANDS = "eclipse.commands"; //$NON-NLS-1$
	private static final String PROP_EXIT_CODE = "eclipse.exitcode"; //$NON-NLS-1$
	private static final String PROP_EXIT_DATA = "eclipse.exitdata"; //$NON-NLS-1$
	private static final String CMD_DATA = "-data"; //$NON-NLS-1$
	private static final String CMD_VMARGS = "-vmargs"; //$NON-NLS-1$
	private static final String NEW_LINE = "\n"; //$NON-NLS-1$
	
	private IWorkbenchWindow activeWorkbenchWindow;

	protected LicensePage licensePage;
	protected ChooseWorkspaceLocationPage chooseWorkspaceLocationPage;
	protected ConfigureProxyPage configureProxyPage;
	protected SelectSamplesPage selectSamplesPage;
	protected RegistrationPage registrationPage;
	protected SummaryPage summaryPage;

	public SetupWizard() {
		super();
		
		IWorkbench workbench = PlatformUI.getWorkbench();
		activeWorkbenchWindow = workbench.getActiveWorkbenchWindow(); 
		
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		licensePage = new LicensePage();
		addPage(licensePage);
		
		chooseWorkspaceLocationPage = new ChooseWorkspaceLocationPage();
		addPage(chooseWorkspaceLocationPage);
		
		configureProxyPage = new ConfigureProxyPage();
		addPage(configureProxyPage);
		
		selectSamplesPage = new SelectSamplesPage();
		addPage(selectSamplesPage);
		
		summaryPage = new SummaryPage();
		addPage(summaryPage);
	}

	@Override
	public boolean performFinish() {
		// Create new workspace
		String workspaceLocation = chooseWorkspaceLocationPage.getUserWorkspaceLocation();
		File workspace = new File(workspaceLocation);
		workspace.mkdirs();

		// Configure the engine
		File workspaceConfiguration = new File(workspaceLocation + "/configuration");
		workspaceConfiguration.mkdirs();
		
		// Create the engine.properties file
		Properties engineProperties = new Properties();
		engineProperties.setProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_MODE.toString(),
				configureProxyPage.getProxyMode());
		engineProperties.setProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_PORT.toString(),
				configureProxyPage.getProxyPort());
		engineProperties.setProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_HOST.toString(),
				configureProxyPage.getProxyHost());
		engineProperties.setProperty(
				EnginePropertiesManager.PropertyName.PROXY_SETTINGS_BY_PASS_DOMAINS.toString(),
				configureProxyPage.getDoNotApplyProxy());
		engineProperties.setProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_AUTO.toString(),
				configureProxyPage.getProxyAutoConfUrl());
		engineProperties.setProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_METHOD.toString(),
				configureProxyPage.getProxyMethod());
		engineProperties.setProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_USER.toString(),
				configureProxyPage.getProxyUser());
		engineProperties.setProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_PASSWORD.toString(),
				Crypto2.encodeToHexString(configureProxyPage.getProxyPassword()));
		try {
			engineProperties.store(new FileOutputStream(new File(workspaceLocation
					+ "/configuration/engine.properties")), null);
		} catch (IOException e) {
			ConvertigoPlugin.errorMessageBox("Unable to create the engine configuration file.\n"
					+ "Default values will be used, i.e. you will have to reconfigure the proxy afterwards.\n\n"
					+ "[" + e.getClass().getName() + "] " + e.getMessage());
		}

		// Create the eclipse workspace (the 'projects' directory)
		File workspaceProjects = new File(workspaceLocation + "/projects");
		workspaceProjects.mkdirs();

		// Install selected samples
//		Set<String> selectedSamples = selectSamplesPage.getSelectedProjects();
//		File srcFile = new File("");
//		for (String sample : selectedSamples) {
//			//FileUtils.copyFileToDirectory(srcFile, workspaceProjects);
//		}
		
		// Restart the studio with the new eclipse workspace
		restart(workspaceProjects.getPath());
		
		return true;
	}
	
	private void restart(String path) {
		String command_line = buildCommandLine(path);
		if (command_line == null) {
			return;
		}

		System.setProperty(PROP_EXIT_CODE, Integer.toString(24));
		System.setProperty(PROP_EXIT_DATA, command_line);
		
		System.out.println("restart: " + command_line);
		activeWorkbenchWindow.getWorkbench().restart();
	}

	/**
	 * Create and return a string with command line options for eclipse.exe that
	 * will launch a new workbench that is the same as the currently running
	 * one, but using the argument directory as its workspace.
	 * 
	 * @param workspace
	 *            the directory to use as the new workspace
	 * @return a string of command line options or null on error
	 */
	private String buildCommandLine(String workspace) {
		String property = System.getProperty(PROP_VM);
		if (property == null) {
			ConvertigoPlugin.logError("Unable to get eclipse argument " + PROP_VM);
		}

		StringBuffer result = new StringBuffer(512);
		result.append(property);
		result.append(NEW_LINE);

		// append the vmargs and commands. Assume that these already end in \n
		String vmargs = System.getProperty(PROP_VMARGS);
		if (vmargs != null) {
			result.append(vmargs);
		}

		// append the rest of the args, replacing or adding -data as required
		property = System.getProperty(PROP_COMMANDS);
		if (property == null) {
			result.append(CMD_DATA);
			result.append(NEW_LINE);
			result.append(workspace);
			result.append(NEW_LINE);
		} else {
			// find the index of the arg to replace its value
			int cmd_data_pos = property.lastIndexOf(CMD_DATA);
			if (cmd_data_pos != -1) {
				cmd_data_pos += CMD_DATA.length() + 1;
				result.append(property.substring(0, cmd_data_pos));
				result.append(workspace);
				result.append(property.substring(property.indexOf('\n',
						cmd_data_pos)));
			} else {
				result.append(CMD_DATA);
				result.append(NEW_LINE);
				result.append(workspace);
				result.append(NEW_LINE);
				result.append(property);
			}
		}

		// put the vmargs back at the very end (the eclipse.commands property
		// already contains the -vm arg)
		if (vmargs != null) {
			result.append(CMD_VMARGS);
			result.append(NEW_LINE);
			result.append(vmargs);
		}

		return result.toString();
	}

}
