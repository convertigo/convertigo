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

package com.twinsoft.convertigo.engine.localbuild;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;

import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.beans.mobileplatforms.Android;
import com.twinsoft.convertigo.beans.mobileplatforms.IOs;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.admin.services.mobiles.MobileResourceHelper;
import com.twinsoft.convertigo.engine.util.ProcessUtils;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class BuildLocally {

	private static final String cordovaDir = "cordova";
	/** know which icon goes with which name on ios platform in function of height and width */
	// private static final Map<String, String> iOSIconsCorrespondences;
	/** know which splash goes with which name on ios platform in function of height and width */
	// private static final Map<String, String> iOSSplashCorrespondences;

	/** Mobile platform */
	protected final MobilePlatform mobilePlatform;

	private String cmdOutput;
	private String errorLines = null;

	private boolean processCanceled = false;
	private Process process;

	private final static String cordovaInstallsPath = Engine.USER_WORKSPACE_PATH + File.separator + "cordovas";
	private String cordovaBinPath;

	private File jdkDir = null;
	private File androidSdkDir = null;
	private File gradleDir = null;
	private File nodeDir = null;
	private String preferedAndroidBuildTools = null;
	private int preferedJDK = 8;

	private File mobilePackage = null;
	private Status lastStatus = null;

	private String iosProvisioningProfileUUID = null;
	private String iosSignIdentity = null;

	private File androidKeystore = null;
	private String androidKeystorePassword = null;
	private String androidPassword = null;
	private String androidAlias = null;

	public BuildLocally(MobilePlatform mobilePlatform) {
		this.mobilePlatform = mobilePlatform;
		this.cordovaBinPath = null;
		File cordovaInstallsDir = new File(BuildLocally.cordovaInstallsPath);
		if (!cordovaInstallsDir.exists()) {
			cordovaInstallsDir.mkdir();
		}
	}

	public MobilePlatform getMobilePlatform() {
		return mobilePlatform;
	}

	private String runCommand(File launchDir, String command, List<String> parameters, boolean mergeError) throws Throwable {
		if (command.endsWith("cordova") && Engine.isWindows()) {
			command += ".cmd";
		}
		String paths = getLocalBuildAdditionalPath();
		if (nodeDir != null) {
			paths = nodeDir.getAbsolutePath() + File.pathSeparator + paths;
		} else {
			paths = ProcessUtils.getNodeDir(ProcessUtils.getDefaultNodeVersion()).getAbsolutePath() + File.pathSeparator + paths;
		}
		if (jdkDir != null) {
			paths = new File(jdkDir, "bin").getAbsolutePath() + File.pathSeparator + paths;
		}
		if (gradleDir != null) {
			paths = new File(gradleDir, "bin").getAbsolutePath() + File.pathSeparator + paths;
		}

		parameters.add(0, command);
		ProcessBuilder pb = command.equals("npm") ?
				ProcessUtils.getNpmProcessBuilder(paths, parameters)
				: ProcessUtils.getProcessBuilder(paths, parameters);
		// Set the directory from where the command will be executed
		pb.directory(launchDir.getCanonicalFile());

		pb.redirectErrorStream(mergeError);

		if (jdkDir != null) {
			pb.environment().put("JAVA_HOME", jdkDir.getAbsolutePath());
		}
		if (androidSdkDir != null) {
			pb.environment().put("ANDROID_HOME", androidSdkDir.getAbsolutePath());
			pb.environment().put("ANDROID_SDK_ROOT", androidSdkDir.getAbsolutePath());
		}

		var lang = System.getenv("LANG");
		if (lang != null && lang.contains("UTF-8")) {
			pb.environment().put("LANG", lang);
		} else {
			pb.environment().put("LANG", "en_US.UTF-8");
		}

		Engine.logEngine.info("Executing command : " + parameters + "\nEnv:" + pb.environment());

		process = pb.start();

		boolean[] done = {false};

		cmdOutput = "";
		// Logs the output
		Engine.execute(() -> {
			String line;
			processCanceled = false;

			try	(BufferedReader bis = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				while ((line = bis.readLine()) != null) {
					Engine.logEngine.info(line);
					BuildLocally.this.cmdOutput += line;
				}
			} catch (IOException e) {
				Engine.logEngine.error("Error while executing command", e);
			}
			synchronized (done) {
				done[0] = true;
				done.notify();
			}
		});

		if (!mergeError) {
			// Logs the error output
			new Thread(() -> {
				String line;
				processCanceled = false;

				try (BufferedReader bis = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
					while ((line = bis.readLine()) != null) {
						Engine.logEngine.error(line);
						errorLines += line;
					}
				} catch (IOException e) {
					Engine.logEngine.error("Error while executing command", e);
				}
			}).start();
		}

		int exitCode = process.waitFor();

		synchronized (done) {
			if (!done[0]) {
				done.wait();
			}
		}

		if (exitCode != 0 && exitCode != 127) {
			throw new Exception("Exit code " + exitCode + " when running the command '" + command + 
					"' with parameters : '" + parameters + "'. The output of the command is : '" 
					+ cmdOutput + "'");
		}


		return cmdOutput;
	}

	/***
	 * Function which permit to run cordova command
	 * @param projectDir
	 * @param commands
	 * @return
	 * @throws Throwable
	 */
	private String runCordovaCommand(File projectDir, String... commands) throws Throwable {
		List<String> commandsList = new LinkedList<String>();
		Collections.addAll(commandsList, commands);
		return runCordovaCommand(projectDir, commandsList);
	}

	/***
	 * Runs a Cordova command and returns the output stream. This will wait until the command is finished. 
	 * Output stream and error stream are logged in  the console.
	 * @param Command
	 * @param projectDir
	 * @return
	 * @throws Throwable
	 */
	private String runCordovaCommand(File projectDir, List<String> cordovaCommands) throws Throwable {

		String command = "cordova";
		if (this.cordovaBinPath != null) {
			command = this.cordovaBinPath;
		}

		return this.runCommand(projectDir, command, cordovaCommands, false);
	}

	/***
	 * Explore "config.xml", handle plugins and copy needed resources to appropriate platforms folders.
	 * @param wwwDir
	 * @param platform
	 * @param cordovaDir
	 */
	private void processConfigXMLResources(File wwwDir, File cordovaDir) throws Throwable {
		try {

			File configFile = new File(cordovaDir, "config.xml");
			Document doc = XMLUtils.loadXml(configFile);

			TwsCachedXPathAPI xpathApi = new TwsCachedXPathAPI();

			// Changes icons and splashs src in config.xml file because it was moved to the parent folder
			NodeIterator nodeIterator = xpathApi.selectNodeIterator(doc, "//*[local-name()='splash' or local-name()='icon']");
			Element singleElement = (Element) nodeIterator.nextNode();
			while (singleElement != null) {
				String src = singleElement.getAttribute("src");
				src = "www/" + src;
				File file = new File(cordovaDir, src);
				if (file.exists()) {
					singleElement.setAttribute("src", src);
				}

				singleElement = (Element) nodeIterator.nextNode();
			}

			//ANDROID
			if (mobilePlatform instanceof Android) {
				singleElement = (Element) xpathApi.selectSingleNode(doc, "/widget/name");
				if (singleElement != null) {
					String name = singleElement.getTextContent();
					name = name.replace("\\", "\\\\");
					name = name.replace("'", "\\'");
					name = name.replace("\"", "\\\"");
					singleElement.setTextContent(name);
				}
				singleElement = (Element) xpathApi.selectSingleNode(doc, "//preference[@name='AndroidWindowSplashScreenAnimatedIcon']");
				if (singleElement != null && singleElement.hasAttribute("value")) {
					var value = singleElement.getAttribute("value");
					value = "www/" + value;
					if (new File(cordovaDir, value).exists()) {
						singleElement.setAttribute("value", value);
					}
				}
			}

			// We have to add the root config.xml all our app's config.xml preferences.
			// Cordova will use this file to generates the platform specific config.xml

			NodeIterator preferences = xpathApi.selectNodeIterator(doc, "//preference");

			NodeList preferencesList = doc.getElementsByTagName("preference");

			// Remove old preferences
			while ( preferencesList.getLength() > 0 ) { 
				Element pathNode = (Element) preferencesList.item(0);
				// Remove empty lines
				Node prev = pathNode.getPreviousSibling();
				if (prev != null && prev.getNodeType() == Node.TEXT_NODE &&
						prev.getNodeValue().trim().length() == 0) {
					doc.getDocumentElement().removeChild(prev);
				}
				doc.getDocumentElement().removeChild(pathNode);
			}

			for (Element preference = (Element) preferences.nextNode(); preference != null; preference = (Element) preferences.nextNode()) {
				String name = preference.getAttribute("name");
				String value = preference.getAttribute("value");

				Element elt = doc.createElement("preference");
				elt.setAttribute("name", name);
				elt.setAttribute("value", value);

				Engine.logEngine.info("Adding preference'" + name + "' with value '" + value + "'");

				doc.getDocumentElement().appendChild(elt);
			}	

			Engine.logEngine.trace("New config.xml is: " + XMLUtils.prettyPrintDOM(doc));
			File resXmlFile = new File(cordovaDir, "config.xml");
			// FileUtils.deleteQuietly(resXmlFile);
			XMLUtils.saveXml(doc, resXmlFile.getAbsolutePath());

			// Last part, as all resources has been copied to the correct location, we can remove
			// our "www/res" directory before packaging to save build time and size...
			// FileUtils.deleteDirectory(new File(wwwDir, "res"));

		} catch (Exception e) {
			logException(e, "Unable to process config.xml in your project, check the file's validity");
		}
	}

	/***
	 * Return the absolute path of built application file
	 * @param mobilePlatform
	 * @param buildMode
	 * @return
	 */
	protected File getAbsolutePathOfBuiltFile(MobilePlatform mobilePlatform, String buildMode) {
		String cordovaPlatform = mobilePlatform.getCordovaPlatform();
		String builtPath = File.separator + "platforms" + File.separator + cordovaPlatform + File.separator;

		String extension = "";
		File f = new File(getCordovaDir(), builtPath);		

		if (f.exists()) {

			// Android
			if (mobilePlatform instanceof Android) {
				builtPath = builtPath + "ant-build" + File.separator;
				File f2 = new File(getCordovaDir(), builtPath);
				if (!f2.exists()) {
					builtPath = File.separator + "platforms" + File.separator + cordovaPlatform + 
							File.separator + "build" + File.separator + "outputs" + File.separator + "apk" + File.separator;
				}
				extension = "apk";

				// iOS
			} else if (mobilePlatform instanceof IOs){
				extension = "xcworkspace";
			} else {
				return null;
			}
		}

		f = new File(getCordovaDir(), builtPath);
		if (f.exists()) {
			String[] filesNames = f.list();
			int i = filesNames.length - 1;
			boolean find = false;
			while (i > 0 && !find && !extension.isEmpty()) {
				String fileName = filesNames[i];
				if (fileName.endsWith(extension)) {
					builtPath += fileName;
					find = true;
				}
				i--;
			}
		} else {
			builtPath = File.separator + "platforms" + File.separator + cordovaPlatform + File.separator;
		}

		return new File (getCordovaDir(), builtPath);
	}

	/***
	 * Dialog yes/no which ask to user if we want
	 * remove the cordova directory present into "_private" directory
	 * We also explain, what we do and how to recreate the cordova environment
	 */
	public void removeCordovaDirectory() {
		String mobilePlatformName = mobilePlatform.getName();

		//Step 1: Recover the "cordova" directory	
		final File cordovaDirectory = getCordovaDir();

		//Step 2: Remove the "cordova" directory
		if (cordovaDirectory.exists()) {
			if (FileUtils.deleteQuietly(cordovaDirectory)){
				Engine.logEngine.info("The Cordova environment of \"" + mobilePlatformName + "\" has been successfull removed.");
				return;
			}
			Engine.logEngine.warn("The Cordova environment of \"" + mobilePlatformName + "\" has been partially removed.");			
		} else {
			Engine.logEngine.error("The Cordova environment of \"" + mobilePlatformName + "\" not removed because doesn't exist.");
			return;
		}
	}

	/***
	 * Return the Cordova directory
	 * @return File
	 */
	public File getCordovaDir() {
		return new File(getPrivateDir(), 
				"localbuild" + File.separator + 
				mobilePlatform.getName() + File.separator + BuildLocally.cordovaDir);
	}

	/***
	 * Return the Private directory
	 * @return File
	 */
	private File getPrivateDir() {
		return new File(mobilePlatform.getProject().getDirPath() + "/_private");
	}

	public enum Status {
		OK,
		CANCEL
	}

	public Status runBuild(String option, boolean run, String target) {
		try {			
			File cordovaDir = getCordovaDir();
			File wwwDir = new File(cordovaDir,"www");
			wwwDir.mkdirs();

			if (!wwwDir.exists()) {
				throw new EngineException("Cannot create the build folder '" + wwwDir.getAbsolutePath() + "', check the current user can create files or change the Eclipse preference: 'Convertigo/Studio/Local Build Folder'.");
			}

			// Cordova environment is already created, we have to build
			// Step 1: Call Mobile packager to prepare the source package
			MobileResourceHelper mobileResourceHelper = new MobileResourceHelper(mobilePlatform, wwwDir.getAbsolutePath());

			wwwDir = mobileResourceHelper.preparePackage();

			// Step 2: Add platform and read config.xml to copy needed icons and splash resources

			String cordovaPlatform = mobilePlatform.getCordovaPlatform();

			//
			FileUtils.copyFile(new File(wwwDir, "config.xml"), new File(cordovaDir, "config.xml"));
			FileUtils.deleteQuietly(new File(wwwDir, "config.xml"));

			if (iosProvisioningProfileUUID != null) {
				boolean release = "release".equals(option);
				JSONObject json = new JSONObject();
				if (iosSignIdentity != null) {
					json.put("codeSignIdentity", iosSignIdentity);
				} else {
					json.put("codeSignIdentity", release ? "iPhone Distribution" : "iPhone Developer");
				}				
				json.put("provisioningProfile", iosProvisioningProfileUUID);
				json = new JSONObject().put(release ? "release" : "debug", json);
				json = new JSONObject().put("ios", json);
				FileUtils.write(new File(cordovaDir, "build.json"), json.toString(2), "utf-8");
			}

			if (androidKeystore != null) {
				boolean release = "release".equals(option);
				JSONObject json = new JSONObject();
				json.put("keystore", androidKeystore.getAbsolutePath());
				json.put("storePassword", androidKeystorePassword);
				json.put("alias", androidAlias);
				json.put("password", androidPassword);
				json.put("keystoreType", "");		
				json = new JSONObject().put(release ? "release" : "debug", json);
				json = new JSONObject().put("android", json);
				FileUtils.write(new File(cordovaDir, "build.json"), json.toString(2), "utf-8");
			}

			processConfigXMLResources(wwwDir, cordovaDir);

			List<String> commandsList = new LinkedList<String>();

			jdkDir = null;
			if ("android".equals(mobilePlatform.getCordovaPlatform())) {
				Engine.logEngine.info("Check or install for the JDK to build the Android application");
				jdkDir = ProcessUtils.getJDK(preferedJDK, (pBytesRead, pContentLength, pItems) -> {
					Engine.logEngine.info("download JDK: " + Math.round(100f * pBytesRead / pContentLength) + "% [" + pBytesRead + "/" + pContentLength + "]");
				});
				androidSdkDir = ProcessUtils.getAndroidSDK(preferedAndroidBuildTools, (pBytesRead, pContentLength, pItems) -> {
					Engine.logEngine.info("download Android SDK: " + Math.round(100f * pBytesRead / pContentLength) + "% [" + pBytesRead + "/" + pContentLength + "]");
				});
				gradleDir = ProcessUtils.getGradle((pBytesRead, pContentLength, pItems) -> {
					Engine.logEngine.info("download Gradle: " + Math.round(100f * pBytesRead / pContentLength) + "% [" + pBytesRead + "/" + pContentLength + "]");
				});
			}

			runCordovaCommand(cordovaDir, "prepare", cordovaPlatform);

			// Step 3: Build or Run using Cordova the specific platform.
			if (run) {
				commandsList.add("run");
				commandsList.add(cordovaPlatform);
				commandsList.add("--" + option);
				commandsList.add("--" + target);

				runCordovaCommand(cordovaDir, commandsList);
			} else {
				commandsList.add("build");
				commandsList.add(cordovaPlatform);
				commandsList.add("--" + option);

				if (mobilePlatform instanceof Android android && "release".equals(option)) {
					commandsList.add("--");
					commandsList.add("--packageType=" + android.getReleasePackageType().name());
				}

				String out = runCordovaCommand(cordovaDir, commandsList);

				Matcher m = Pattern.compile("[^\\s]+(?:\\.apk|/build/device)").matcher(out);
				if (m.find()) {
					File pkg;
					do {
						pkg = new File(m.group());
						if (pkg.exists()) {
							if (pkg.getName().equals("device")) {
								for (File f: pkg.listFiles()) {
									if (f.getName().endsWith(".ipa")) {
										pkg = f;
										break;
									}
								}
							}
							mobilePackage = pkg;
						}
					} while (m.find());
				}

				// Step 4: Show dialog with path to apk/ipa/xap
				if (!processCanceled) {
					showLocationInstallFile(mobilePlatform, process.exitValue(), errorLines, option);
				}
			}

			return Status.OK;
		} catch (Throwable e) {
			logException(e, "Error when processing Cordova build: " + e);

			return Status.CANCEL;
		}
	}

	public void cancelBuild(boolean run) {
		//Only for the "Run On Device" action
		if (run) {
			if (mobilePlatform instanceof IOs) {
				//kill the lldb process only for ios build platform
				try {
					Runtime.getRuntime().exec(new String[] {"pkill", "lldb"}).waitFor();
				} catch (Exception e) {
					Engine.logEngine.error("Error during kill of process \"lldb\"\n" + e.getMessage(), e);
				}
			}
		}

		processCanceled = true;

		// Others OS
		process.destroy();
	}

	public Status installCordova() {
		try {
			File resourceFolder = mobilePlatform.getResourceFolder();
			File configFile = new File(resourceFolder, "config.xml");
			Document doc = XMLUtils.loadXml(configFile);
			TwsCachedXPathAPI xpathApi = new TwsCachedXPathAPI();

			Element singleElement = (Element) xpathApi.selectSingleNode(doc, "/widget/preference[@name='nodejs-version']");
			String nodeVersion = (singleElement != null) ? singleElement.getAttribute("value") : ProcessUtils.getDefaultNodeVersion();
			nodeDir = ProcessUtils.getNodeDir(nodeVersion, (r , t, x) -> {
				Engine.logEngine.info("Downloading nodejs " + nodeVersion + ": " + Math.round((r * 100f) / t) + "%");
			});
			Engine.logEngine.info("Checking if nodejs and npm are installed.");
			List<String> parameters = new LinkedList<String>();
			parameters.add("--version");
			String npmVersion = runCommand(resourceFolder, "npm", parameters, false);
			Pattern pattern = Pattern.compile("^([0-9])+\\.([0-9])+\\.([0-9])+$");
			Matcher matcher = pattern.matcher(npmVersion);
			if (!matcher.find()){
				throw new Exception("node.js is not installed ('npm --version' returned '" + npmVersion + "')\nYou must download nodes.js from https://nodejs.org/en/download/");
			}
			Engine.logEngine.info("OK, nodejs (" + nodeVersion + ") and npm (" + npmVersion + ") are installed.");

			singleElement = (Element) xpathApi.selectSingleNode(doc, "/widget/preference[@name='prefered-android-build-tools']");
			if (singleElement != null && singleElement.hasAttribute("value")) {
				preferedAndroidBuildTools = singleElement.getAttribute("value"); 
			}

			singleElement = (Element) xpathApi.selectSingleNode(doc, "/widget/preference[@name='prefered-android-jdk']");
			if (singleElement != null && singleElement.hasAttribute("value")) {
				preferedJDK = NumberUtils.toInt(singleElement.getAttribute("value"), preferedJDK);
			}

			Engine.logEngine.info("Checking if this cordova version is already installed.");
			singleElement = (Element) xpathApi.selectSingleNode(doc, "/widget/preference[@name='cordova-version']");
			if (singleElement == null) {
				singleElement = (Element) xpathApi.selectSingleNode(doc, "/widget/preference[@name='phonegap-version']");
			}
			if (singleElement != null) {
				String cliVersion = singleElement.getAttribute("value");
				if (cliVersion != null) {

					pattern = Pattern.compile("^(?:cli-)?([0-9]+\\.[0-9]+\\.[0-9]+)$");
					matcher = pattern.matcher(cliVersion);			
					if (!matcher.find()){
						throw new Exception("The cordova version is specified but its value has not the right format.");
					}

					// Remove 'cli-' from 'cli-x.x.x'
					cliVersion = matcher.group(1);
					String cordovaInstallPath = BuildLocally.cordovaInstallsPath + File.separator + 
							"cordova" + cliVersion;
					File cordovaBinFile = new File(cordovaInstallPath + File.separator + 
							"node_modules" + File.separator + 
							"cordova" + File.separator + 
							"bin" + File.separator + "cordova"
							);
					// If cordova is not installed
					if (!cordovaBinFile.exists()) {

						Engine.logEngine.info("Installing cordova " + cliVersion + " This can take some time....");

						File cordovaInstallDir = new File(cordovaInstallPath);
						cordovaInstallDir.mkdir();

						parameters = new LinkedList<String>();
						parameters.add("--prefix");
						parameters.add(cordovaInstallDir.getAbsolutePath());
						parameters.add("--unsafe-perm=true");
						parameters.add("install");
						parameters.add("cordova@" + cliVersion);

						this.runCommand(cordovaInstallDir, "npm", parameters, true);
					}

					Engine.logEngine.info("Cordova (" + cliVersion + ") is now installed.");

					this.cordovaBinPath = cordovaBinFile.getAbsolutePath();
				} else {
					Engine.logEngine.info("The phonegap-version prefrence version is not specified in config.xml.");
					throw new Exception("The phonegap-version prefrence version is not specified in config.xml.");
				}
			} else {
				throw new Exception("The phonegap-version preference not found in config.xml.");
			}

		} catch (Throwable e) {
			Engine.logEngine.info("Error when installing Cordova: " + e);
			logException(e, "Error when installing Cordova");			
			return Status.CANCEL;
		}

		return Status.OK;
	}

	public boolean isProcessCanceled() {
		return this.processCanceled;
	}

	abstract protected String getLocalBuildAdditionalPath();
	abstract protected void logException(Throwable e, String message);
	/***
	 * Show the dialog with built application file 
	 * @param mobilePlatform
	 * @param exitValue
	 * @param errorLines
	 * @param buildOption
	 */
	abstract protected void showLocationInstallFile(final MobilePlatform mobilePlatform, 
			final int exitValue, final String errorLines, final String buildOption);

	public File getMobilePackage() {
		return mobilePackage;
	}

	public Status getLastStatus() {
		return lastStatus;
	}

	public void configureSignIOS(File provisioningProfile, String signId) throws Exception {
		if (!(mobilePlatform instanceof IOs && Engine.isMac())) {
			return;
		}
		String line;
		String uuid = null;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(provisioningProfile)))) {
			while ((line = br.readLine()) != null) {
				if (line.contains("UUID")) {
					line = br.readLine();
					Matcher m = Pattern.compile("[-A-F0-9]{36}", Pattern.CASE_INSENSITIVE).matcher(line);
					if (m.find()) {
						uuid = m.group();
						break;
					} else {
						throw new EngineException("Cannot match UUID from " + provisioningProfile);
					}
				}
			}
		}
		if (uuid == null) {
			throw new EngineException("No UUID found in " + provisioningProfile);
		}

		File dir = new File(System.getProperty("user.home"), "Library/MobileDevice/Provisioning Profiles");
		dir.mkdirs();
		FileUtils.copyFile(provisioningProfile, new File(dir, uuid + ".mobileprovision"));
		iosProvisioningProfileUUID = uuid;
		iosSignIdentity = signId;
	}

	public void configureSignAndroid(File keystore, String keystorePassword, String alias, String password) {
		if (mobilePlatform instanceof Android) {
			androidKeystore = keystore;
			androidKeystorePassword = keystorePassword;
			androidAlias = alias;
			androidPassword = password;
		}
	}
}
