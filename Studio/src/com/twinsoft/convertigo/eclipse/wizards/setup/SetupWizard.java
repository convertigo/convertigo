package com.twinsoft.convertigo.eclipse.wizards.setup;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.ProxyManager;

public class SetupWizard extends Wizard {
	protected LicensePage licensePage;
	protected WorkspaceMigrationPage workspaceMigrationPage;
	protected WorkspaceCreationPage workspaceCreationPage;
	protected ConfigureProxyPage configureProxyPage;
	protected AlreadyPscKeyPage alreadyPscKeyPage;
	protected RegistrationPage registrationPage;
	protected PscKeyPage pscKeyPage;
	protected SummaryPage summaryPage;
	
	protected ProxyManager proxyManager;

	public SetupWizard() {
		super();
		
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		
		// no license acceptation if already accepted in the Windows installer
		if (!System.getProperties().containsKey("convertigo.license.accepted") &&
				!IPreferenceStore.TRUE.equals(ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_LICENSE_ACCEPTED))) {
			licensePage = new LicensePage();
			addPage(licensePage);
		} else {
			System.getProperties().remove("convertigo.license.accepted");
		}
		
		Engine.CONFIGURATION_PATH = Engine.USER_WORKSPACE_PATH;
		
		// empty workspace folder
		if (new File(Engine.USER_WORKSPACE_PATH).list().length == 0) {
			boolean pre6_2 = false;
			for (String pathToCheck : Arrays.asList(
					"configuration/engine.properties",
					"minime/Java/login.txt",
					"cache",
					"projects",
					"logs"
					)) {
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
			EnginePropertiesManager.loadProperties();
			proxyManager = new ProxyManager();
			proxyManager.init();
		} catch (EngineException e) {
			ConvertigoPlugin.logInfo("Unexpected EngineException : " + e.getMessage());
		}
		
//		configureProxyPage = new ConfigureProxyPage(proxyManager);
//		addPage(configureProxyPage);
		
		alreadyPscKeyPage = new AlreadyPscKeyPage();
		addPage(alreadyPscKeyPage);
		
		registrationPage = new RegistrationPage();
		addPage(registrationPage);
		
		pscKeyPage = new PscKeyPage();
		addPage(pscKeyPage);

		summaryPage = new SummaryPage();
		addPage(summaryPage);
	}

	@Override
	public boolean performFinish() {
		ConvertigoPlugin.setProperty(ConvertigoPlugin.PREFERENCE_LICENSE_ACCEPTED, IPreferenceStore.TRUE);
		
		if (workspaceMigrationPage != null) {
			File userWorkspace = new File(Engine.USER_WORKSPACE_PATH);

			File eclipseWorkspace = new File(Engine.PROJECTS_PATH);

			ConvertigoPlugin.logInfo("The current Eclipse workspace is a pre-6.2.0 CEMS workspace. Migration starting …");

			boolean projectsMoveFailed = false;

			for (File file : eclipseWorkspace.listFiles()) {
				if (!file.getName().equals(".metadata")) {
					try {
						ConvertigoPlugin.logInfo("Migration in progress : moving " + file.getName() + " …");
						FileUtils.moveToDirectory(file, userWorkspace, false);
					} catch (IOException e) {
						projectsMoveFailed = projectsMoveFailed || file.getName().equals("projects");
						ConvertigoPlugin.logInfo("Migration in progress : failed to move " + file.getName() + " ! (" + e.getMessage() + ")");
					}
				}
			}

			if (!projectsMoveFailed) {
				ConvertigoPlugin.logInfo("Migration in progress : move move back CEMS projects to the Eclipse workspace …");
				File exMetadata = new File(userWorkspace, "projects/.metadata");
				try {
					FileUtils.copyDirectoryToDirectory(exMetadata, eclipseWorkspace);
					FileUtils.deleteQuietly(exMetadata);
				} catch (IOException e1) {
					ConvertigoPlugin.logInfo("Migration in progress : failed to merge .metadata ! (" + e1.getMessage() + ")");
				}

				for (File file : new File(userWorkspace, "projects").listFiles()) {
					try {
						ConvertigoPlugin.logInfo("Migration in progress : moving the file " + file.getName() + " into the Eclipse Workspace …");
						FileUtils.moveToDirectory(file, eclipseWorkspace, false);
					} catch (IOException e) {
						ConvertigoPlugin.logInfo("Migration in progress : failed to move " + file.getName() + " ! (" + e.getMessage() + ")");
					}
				}

				ConvertigoPlugin.logInfo("Migration of workspace done !\n" +
						"Migration of the folder : " + eclipseWorkspace.getAbsolutePath() + "\n" +
						"Eclipse Workspace with your CEMS projects : " + eclipseWorkspace.getAbsolutePath() + "\n" +
						"Convertigo Workspace with your CEMS configuration : " + userWorkspace.getAbsolutePath());
			} else {
				ConvertigoPlugin.logInfo("Migration incomplet : cannot move back CEMS projects to the Eclipse workspace !");
			}
		}
		
		File pscFile = new File(Engine.USER_WORKSPACE_PATH, "studio/psc.txt");
		try {
			FileUtils.writeStringToFile(pscFile, pscKeyPage.getCertificateKey(), "utf-8");
		} catch (IOException e) {
			ConvertigoPlugin.logError("Failed to write the PSC file : " + e.getMessage());
		}
		
//		// Create new workspace if needed
//		String currentWorkspaceLocation = Engine.USER_WORKSPACE_PATH;
//		File currentWorkspace = new File(currentWorkspaceLocation);
//		String newWorkspaceLocation = chooseWorkspaceLocationPage.getUserWorkspaceLocation();
//		File newWorkspace = new File(newWorkspaceLocation);
//
//		boolean bRestartRequired = false;
//		if (currentWorkspace.exists()) {
//			if (!currentWorkspace.equals(newWorkspace)) {
//				newWorkspace.mkdirs();
//				bRestartRequired = true;
//			}
//		}
//
//		// Configure the engine
//		File workspaceConfiguration = new File(newWorkspaceLocation + "/configuration");
//		workspaceConfiguration.mkdirs();
//		
//		// Create the engine.properties file
//		Properties engineProperties = new Properties();
//		engineProperties.setProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_MODE.toString(),
//				configureProxyPage.getProxyMode());
//		engineProperties.setProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_PORT.toString(),
//				configureProxyPage.getProxyPort());
//		engineProperties.setProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_HOST.toString(),
//				configureProxyPage.getProxyHost());
//		engineProperties.setProperty(
//				EnginePropertiesManager.PropertyName.PROXY_SETTINGS_BY_PASS_DOMAINS.toString(),
//				configureProxyPage.getDoNotApplyProxy());
//		engineProperties.setProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_AUTO.toString(),
//				configureProxyPage.getProxyAutoConfUrl());
//		engineProperties.setProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_METHOD.toString(),
//				configureProxyPage.getProxyMethod());
//		engineProperties.setProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_USER.toString(),
//				configureProxyPage.getProxyUser());
//		engineProperties.setProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_PASSWORD.toString(),
//				Crypto2.encodeToHexString(configureProxyPage.getProxyPassword()));
//		try {
//			engineProperties.store(new FileOutputStream(new File(newWorkspaceLocation
//					+ "/configuration/engine.properties")), null);
//		} catch (IOException e) {
//			ConvertigoPlugin.errorMessageBox("Unable to create the engine configuration file.\n"
//					+ "Default values will be used, i.e. you will have to reconfigure the proxy afterwards.\n\n"
//					+ "[" + e.getClass().getName() + "] " + e.getMessage());
//		}
//
//		// Create the eclipse workspace (the 'projects' directory)
//		File workspaceProjects = new File(newWorkspaceLocation + "/projects");
//		workspaceProjects.mkdirs();
//
//		// Install selected samples
//		Set<String> selectedSamples = selectSamplesPage.getSelectedProjects();
//		File srcFile = new File("");
//		for (String sample : selectedSamples) {
//			//FileUtils.copyFileToDirectory(srcFile, workspaceProjects);
//		}
//		
//		/**
//		 * Added by julienda - 03/10/2012
//		 * Deploy the configuration with PSC
//		 */
//		String psc = pscKeyPage.getCertificateKey();
//		String decipheredPSC = Crypto2.decodeFromHexString("registration", psc);
//		Properties pscProperties = new Properties();
//		
//		try {
//			pscProperties.load(new ByteArrayInputStream(decipheredPSC.getBytes()));
//		
//			Set<Object> keys = pscProperties.keySet();
//			int i = 1;
//			
//			while(keys.contains("deploy."+i+".server")){			
//				String server = pscProperties.getProperty("deploy." + i + ".server");
//				String user = pscProperties.getProperty("deploy." + i + ".admin.user");
//				String password = pscProperties.getProperty("deploy."+i+".admin.password");
//				boolean bHttps = Boolean.parseBoolean(pscProperties.getProperty("deploy."+i+".ssl.https"));
//	
//				if (server == null) throw new Exception("Invalid registration certificate (missing server)");
//				if (user == null) throw new Exception("Invalid registration certificate (missing user)");
//				if (password == null) throw new Exception("Invalid registration certificate (missing password)");
//				if (!user.equals(SimpleCipher.decode(password))) throw new Exception("Invalid registration certificate (invalid password)");
//				
//				DeploymentConfiguration deploymentConfiguration = new DeploymentConfiguration(server, user, password, bHttps);
//				ConvertigoPlugin.deploymentConfigurationManager.add(deploymentConfiguration);
//				i++;
//			}
//	
//			ConvertigoPlugin.deploymentConfigurationManager.save();
//		}
//		catch(Exception exception) {
//			ConvertigoPlugin.logError("Error when deploy the psc !");
//		}
		EnginePropertiesManager.unload();
		
		return true;
	}
}
