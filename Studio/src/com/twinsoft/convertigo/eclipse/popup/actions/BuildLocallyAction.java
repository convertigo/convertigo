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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.3.x/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/TestCaseExecuteSelectedAction.java $
 * $Author: maximeh $
 * $Revision: 33944 $
 * $Date: 2013-04-05 18:29:40 +0200 (ven., 05 avr. 2013) $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.xpath.CachedXPathAPI;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.beans.mobileplatforms.Android;
import com.twinsoft.convertigo.beans.mobileplatforms.BlackBerry10;
import com.twinsoft.convertigo.beans.mobileplatforms.IOs;
import com.twinsoft.convertigo.beans.mobileplatforms.Windows8;
import com.twinsoft.convertigo.beans.mobileplatforms.WindowsPhone7;
import com.twinsoft.convertigo.beans.mobileplatforms.WindowsPhone8;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.BuildLocallyEndingDialog;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.mobiles.MobileResourceHelper;
import com.twinsoft.convertigo.engine.util.ImageUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class BuildLocallyAction extends MyAbstractAction {
	
	private enum OS {
		generic,
		linux,
		mac,
		win32,
		solaris;
	}

	static final String cordovaDir = "cordova";
	
	/** know which icon goes with which name on ios platform in function of height and width */
	static final Map<String, String> iOSIconsCorrespondences;
	
	static {
		Map<String, String> m = new HashMap<String, String>();
		// iOS 8.0+ 
        // iPhone 6 Plus
		m.put("180x180","icon-60@3x.png");
		//iOS 7.0+ 
		// iPhone/iPod Touch
		m.put("60x60", "icon-60.png");
		m.put("120x120", "icon-60@2x.png");
		//iPad
		m.put("76x76", "icon-76.png");
		m.put("152x152", "icon-76@2x.png");
		//iOS 6.1+
		//Spotlight Icon
		m.put("40x40", "icon-40.png");
		m.put("80x80", "icon-40@2x.png");
		//iPhone/iPod Touch
		m.put("57x57", "icon.png");
		m.put("114x114", "icon@2x.png");
		//iPad
		m.put("72x72", "icon-72.png");
		m.put("144x144", "icon-72@2x.png");
		//iPhone Spotlight and Settings Icon
		m.put("29x29", "icon-small.png");
		m.put("58x58", "icon-small@2x.png");
		//iPad Spotlight and Settings Icon
		m.put("50x50", "icon-50.png");
		m.put("100x100", "icon-50@2x.png");
		
		iOSIconsCorrespondences = Collections.unmodifiableMap(m);		
	}
	
	/**  know which splash goes with which name on ios platform in function of height and width */
	static final Map<String, String> iOSSplashCorrespondences;
	
	static {
		Map<String, String> m = new HashMap<String, String>();
		
		// iPhone
		m.put("1242x2208", "Default-736h.png");
		m.put("750x1334", "Default-667h@2x~iphone.png");
		m.put("640x1136", "Default-568h@2x~iphone.png");
		m.put("640x960", "Default@2x~iphone.png");
		m.put("320x480", "Default~iphone.png");

		//iPad
		m.put("2208x1242", "Default-Landscape-736h.png");
		m.put("2048x1496", "Default-Landscape@2x~ipad.png");
		m.put("1024x748", "Default-Landscape~ipad.png");
		m.put("768x1004", "Default-Portrait~ipad.png");
		m.put("1536x2008", "Default-Portrait@2x~ipad.png");
		
		iOSSplashCorrespondences = Collections.unmodifiableMap(m);
	}
	
	// For minimal version of cordova required 3.4.x
	final int versionMinimalRequiredDecimalPart = 3;
	final int versionMinimalRequiredFractionalPart = 4;
	
	private String cordovaVersion = null;
	
	private String errorLines = null;
	
	private Process process;
	private boolean processCanceled = false;
    String cmdOutput;
    
	private OS osLocal = null;
	
	public BuildLocallyAction() {
		super();
	}

	private String runCordovaCommand(File projectDir, String... commands) throws Throwable {
		List<String> commandsList = new LinkedList<String>();
		Collections.addAll(commandsList, commands);
		return runCordovaCommand(projectDir, commandsList);
	}
	
	/**
	 * Runs a Cordova command and returns the output stream. This will wait until the command is finished. 
	 * Output stream and error stream are logged in  the console.
	 * 
	 * @param Command
	 * @param projectDir
	 * @return
	 * @throws Throwable
	 */
	
	private String runCordovaCommand(File projectDir, List<String> cordovaCommands) throws Throwable {
		String shell = is(OS.win32) ? "cordova.cmd" : "cordova";
		
		String paths = ConvertigoPlugin.getLocalBuildAdditionalPath();
		paths = (paths.length() > 0 ? paths + File.pathSeparator : "") + System.getenv("PATH");
		
		String shellFullpath = getFullPath(paths, shell);
		
		if (shellFullpath == null) {
			String defaultPaths = null;
			if (is(OS.mac) || is(OS.linux)) {
				defaultPaths = "/usr/local/bin";
			} else if (is(OS.win32)) {
				String programFiles = System.getenv("ProgramW6432");
				if (programFiles != null && programFiles.length() > 0) {
					defaultPaths = programFiles + File.separator + "nodejs";
				}
				
				programFiles = System.getenv("ProgramFiles");
				if (programFiles != null && programFiles.length() > 0) {
					defaultPaths = (defaultPaths == null ? "" : defaultPaths + File.pathSeparator) + programFiles + File.separator + "nodejs";
				}
				
				String appData = System.getenv("APPDATA");
				if (appData != null && appData.length() > 0) {
					defaultPaths = (defaultPaths == null ? "" : defaultPaths + File.pathSeparator) + appData + File.separator + "npm";
				}
			}
			shellFullpath = defaultPaths == null ? null : getFullPath(defaultPaths, shell);
			if (shellFullpath == null) {
				shellFullpath = shell;
			} else {
				paths += File.pathSeparator + defaultPaths;
			}
		}
		
		cordovaCommands.add(0, shellFullpath);
		
		ProcessBuilder processBuilder = new ProcessBuilder(cordovaCommands);
		processBuilder.directory(projectDir.getCanonicalFile());
		
		Map<String, String> pbEnv = processBuilder.environment();
		
		// must set "Path" for Windows 8.1 64
		pbEnv.put(pbEnv.get("PATH") == null ? "Path" : "PATH", paths);
		
		process = processBuilder.start();
		
		InputStream is = process.getInputStream();
		InputStream es = process.getErrorStream();
		
		final BufferedReader bis = new BufferedReader(new InputStreamReader(is));
		final BufferedReader bes = new BufferedReader(new InputStreamReader(es));

		cmdOutput = "";
		Thread readOutputThread = new Thread(new Runnable() {
			@Override
	        public void run() {
				try {
					String line;
					processCanceled = false;
					
					while ((line = bis.readLine()) != null) {
						Engine.logEngine.info(line);
						BuildLocallyAction.this.cmdOutput += line;
					}
					errorLines = "";
					while ((line = bes.readLine()) != null) {
						Engine.logEngine.error(line);
						errorLines += new String(line.getBytes()) + "\n";
					}
				} catch (IOException e) {
					Engine.logEngine.error("Error while executing cordova command", e);
				}
			}
		});
		readOutputThread.start();
		process.waitFor();
		return cmdOutput;
	}
		
	/**
	 * Explore Config.xml, handle plugins and copy needed resources to appropriate platforms folders.
	 * 
	 * @param wwwDir
	 * @param platform
	 * @param cordovaDir
	 */
	private void processConfigXMLResources(File wwwDir, MobilePlatform mobilePlatform, File cordovaDir) throws Throwable {
		try {
			Document doc = XMLUtils.loadXml(new File(wwwDir, "config.xml"));
			CachedXPathAPI xpathApi = new CachedXPathAPI();
			String platform = mobilePlatform.getCordovaPlatform();
			File defaultSplash = null;
			File defaultIcon = null;
			
			/*
			 * Handle plugins in the config.xml file and test to see if the plugin is not already installed
			 */
			Engine.logEngine.info("Checking installed plugins... ");
			String installedPlugins = runCordovaCommand(cordovaDir, "plugin", "list").toLowerCase();
			
			NodeIterator plugins = xpathApi.selectNodeIterator(doc, "//*[local-name()='plugin']");
			
			Element singleElement = (Element) xpathApi.selectSingleNode(doc, "//*[local-name()='splash' and @src and not(@platform)]");
			if (singleElement != null) {
				defaultSplash = new File(wwwDir, singleElement.getAttribute("src"));
				if (!defaultSplash.exists()) {
					defaultSplash = null;
				}
			}
			
			singleElement = (Element) xpathApi.selectSingleNode(doc, "//*[local-name()='icon' and @src and not(@platform)]");
			if (singleElement != null) {
				defaultIcon = new File(wwwDir, singleElement.getAttribute("src"));
				if (!defaultIcon.exists()) {
					defaultIcon = null;
				}
			}
			
			for (Element plugin = (Element) plugins.nextNode(); plugin != null; plugin = (Element) plugins.nextNode()) {
				List<String> options = new LinkedList<String>();
				String pluginName = plugin.getAttribute("name");
				String gitUrl = null;
				String version = null;

				/**
				 * Build an optional --variable <NAME>=<VALUE> list
				 */
				NodeIterator params = xpathApi.selectNodeIterator(plugin, "./param");
				for (Element param = (Element) params.nextNode(); param != null; param = (Element) params.nextNode()) {
					if (param.hasAttribute("name") && param.hasAttribute("value")) {
						options.add("--variable");
						options.add(param.getAttributes().getNamedItem("name").getTextContent() + "=" + param.getAttributes().getNamedItem("value").getTextContent());
					}
				}
				
				if (plugin.hasAttribute("git")) {
					gitUrl = plugin.getAttribute("git");
				}
				
				if (plugin.hasAttribute("version")) {
					version = plugin.getAttribute("version");
				}
				
				if (!installedPlugins.contains(pluginName.toLowerCase())) {
					Engine.logEngine.info("Adding plugin " + pluginName);
					// if we have a gitUrl use it in priority
					
					List<String> arguments = new LinkedList<String>();
					
					if (gitUrl != null) {
						arguments.add("plugin");
						arguments.add("add");
						arguments.add(gitUrl);
					} else {
						arguments.add("plugin");
						arguments.add("add");
						arguments.add(pluginName + (version != null ? "@" + version : ""));
					}
					
					arguments.addAll(options);
					
					runCordovaCommand(cordovaDir, arguments);
				}	
			}

			//ANDROID
			if (mobilePlatform instanceof Android) {
				File resFolder = new File(cordovaDir, "platforms/" + platform + "/res");
				
				if (defaultIcon != null) {
					FileUtils.copyFile(defaultIcon, new File(resFolder, "drawable/icon.png"));
				}
				
				// Copy the icons to the correct res directory
				NodeIterator icons = xpathApi.selectNodeIterator(doc, "//icon[@platform = 'android']");
				for (Element icon = (Element) icons.nextNode(); icon != null; icon = (Element) icons.nextNode()) {
					String source = icon.getAttribute("src");
					String gapAttrib = icon.getAttribute(icon.hasAttribute("gap:qualifier") ? "gap:qualifier" : "gap:density");
					
					File iconSrc = new File(wwwDir, source);
					File dest = new File(resFolder, "drawable-" + gapAttrib + "/icon.png");
					
					Engine.logEngine.debug("Copying " + iconSrc.getAbsolutePath() + " to " + dest.getAbsolutePath());
					
					FileUtils.copyFile(iconSrc, dest);
					if (gapAttrib.equalsIgnoreCase("ldpi")) {
						// special case for ldpi assume it goes also in the drawable folder
						dest = new File(resFolder, "drawable/icon.png");
						
						Engine.logEngine.debug("Copying " + iconSrc.getAbsolutePath() + " to " + dest.getAbsolutePath());
						
						FileUtils.copyFile(iconSrc, dest);
					}
				}
				
				if (defaultSplash != null) {
					FileUtils.copyFile(defaultSplash, new File(resFolder, "drawable/splash.png"));
				}
				
				// now the stuff for splashes
				// for splashes, as there is the the 'gap:' name space use the local-name xpath function instead 
				NodeIterator splashes = xpathApi.selectNodeIterator(doc, "//*[local-name()='splash' and @platform = 'android']");
				for (Element splash = (Element) splashes.nextNode(); splash != null; splash = (Element) splashes.nextNode()) {
					String source = splash.getAttribute("src");
					String gapAttrib = splash.getAttribute(splash.hasAttribute("gap:qualifier") ? "gap:qualifier" : "gap:density");
					
					File splashSrc = new File(wwwDir, source);
					File dest = new File(resFolder, "drawable-" + gapAttrib + "/splash.png");
					
					Engine.logEngine.debug("Copying " + splashSrc.getAbsolutePath() + " to " + dest.getAbsolutePath());
					
					FileUtils.copyFile(splashSrc, dest);
					if (gapAttrib.equalsIgnoreCase("ldpi")) {
						// special case for ldpi assume it goes also in the drawable folder
						dest = new File(resFolder, "drawable/splash.png");
						
						Engine.logEngine.debug("Copying " + splashSrc.getAbsolutePath() + " to " + dest.getAbsolutePath());
						
						FileUtils.copyFile(splashSrc, dest);
					}
				}
			}
			
			//iOS
			if (mobilePlatform instanceof  IOs) {
				String applicationName = mobilePlatform.getParent().getComputedApplicationName();
				
				File iconFolder = new File(cordovaDir, "platforms/" + platform + "/" + applicationName + "/Resources/icons");
				
				if (defaultIcon != null) {
					for (String iconName: iOSIconsCorrespondences.values()) {
						FileUtils.copyFile(defaultIcon, new File(iconFolder, iconName));
					}
				}
				
				// Copy the icons to the correct res directory
				NodeIterator icons = xpathApi.selectNodeIterator(doc, "//icon[@platform = 'ios']");
				for (Element icon = (Element) icons.nextNode(); icon != null; icon = (Element) icons.nextNode()) {
					String source = icon.getAttribute("src");
					String height = icon.getAttribute("height");
					String width = icon.getAttribute("width");

					File iconSrc = new File(wwwDir, source);
					
					String iconName = iOSIconsCorrespondences.get(width + "x" + height);
					File dest = new File(iconFolder, iconName );

					Engine.logEngine.debug("Copying " + iconSrc.getAbsolutePath() + " to " + dest.getAbsolutePath());
					
					FileUtils.copyFile(iconSrc, dest);
				}
				
				File splashFolder = new File(cordovaDir, "platforms/" + platform + "/" + applicationName + "/Resources/splash");
				
				if (defaultSplash != null) {
					for (String splashName: iOSSplashCorrespondences.values()) {
						FileUtils.copyFile(defaultSplash, new File(splashFolder, splashName));
					}
				}
				
				// now the stuff for splashes
				// for splashes, as there is the the 'gap:' name space use the local-name xpath function instead 
				NodeIterator splashes = xpathApi.selectNodeIterator(doc, "//*[local-name()='splash' and @platform = 'ios']");
				for (Element splash = (Element) splashes.nextNode(); splash != null; splash = (Element) splashes.nextNode()) {
					String source = splash.getAttribute("src");
					String height = splash.getAttribute("height");
					String width = splash.getAttribute("width");
					
					File splashSrc = new File(wwwDir, source);
					
					String splashName = iOSSplashCorrespondences.get(width + "x" + height);
					File dest = new File(splashFolder, splashName);
					
					Engine.logEngine.debug("Copying " + splashSrc.getAbsolutePath() + " to " + dest.getAbsolutePath());
					
					FileUtils.copyFile(splashSrc, dest);
				}
			}
			
			//WINPHONE
			if (mobilePlatform instanceof WindowsPhone7 || mobilePlatform instanceof WindowsPhone8) {
				File resFolder = new File(cordovaDir, "platforms/" + platform);
				File destIcon = new File(resFolder, "ApplicationIcon.png");
				File destBackground = new File(resFolder, "Background.png");
				File destSplashScreen = new File(resFolder, "SplashScreenImage.jpg");
				
				if (defaultIcon != null) {
					FileUtils.copyFile(defaultIcon, destIcon);
					FileUtils.copyFile(defaultIcon, destBackground);
				}

				NodeIterator icons = xpathApi.selectNodeIterator(doc, "//icon[@platform = 'winphone']");
				for (Element icon = (Element) icons.nextNode(); icon != null; icon = (Element) icons.nextNode()) {
					String source = icon.getAttribute("src");
					String role = icon.hasAttribute("gap:role") ? icon.getAttribute("gap:role") : "";
					
					File iconSrc = new File(wwwDir, source);
					
					Engine.logEngine.debug("Copying " + iconSrc.getAbsolutePath() + " to " + destIcon.getAbsolutePath());
					
					FileUtils.copyFile(iconSrc, destIcon);
					
					if (role.equalsIgnoreCase("background")) {
						// special case for background 
						Engine.logEngine.debug("Copying " + iconSrc.getAbsolutePath() + " to " + destBackground.getAbsolutePath());
						
						FileUtils.copyFile(iconSrc, destBackground);
					}
				}
				
				if (defaultSplash != null) {
					ImageUtils.pngToJpg(defaultSplash, destSplashScreen);
				}
				
				// now the stuff for splashes
				// for splashes, as there is the the 'gap:' name space use the local-name xpath function instead 
				NodeIterator splashes = xpathApi.selectNodeIterator(doc, "//*[local-name()='splash' and @platform = 'winphone']");
				for (Element splash = (Element) splashes.nextNode(); splash != null; splash = (Element) splashes.nextNode()) {
					String source = splash.getAttribute("src");
					
					File splashSrc = new File(wwwDir, source);
					
					Engine.logEngine.debug("Copying " + splashSrc.getAbsolutePath() + " to " + destSplashScreen.getAbsolutePath());
					
					FileUtils.copyFile(splashSrc, destSplashScreen);
				}
			}

			// TODO : Add platform BB10
			if (mobilePlatform instanceof BlackBerry10) {
				
			}
			// TODO : Add platform Windows 8
			if (mobilePlatform instanceof Windows8) {
				
			}
			
			// We have to add the the root Config.xml all our app's config.xml preferences.
			// Cordova will use this file to generates the platform specific config.xml
			
			// get preferences from current config.xml
			NodeIterator preferences = xpathApi.selectNodeIterator(doc, "//preference");
			
			doc = XMLUtils.loadXml(new File(cordovaDir, "config.xml"));  // The root config.xml
			
			NodeList preferencesList = doc.getElementsByTagName("preference");
			
			//Remove old preferences
			while ( preferencesList.getLength() > 0 ) { 
	            Element pathNode = (Element) preferencesList.item(0);
	            //Remove empty lines
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
			FileUtils.deleteQuietly(resXmlFile);
			XMLUtils.saveXml(doc, resXmlFile.getAbsolutePath());
			
			// Last part , as all resources has been copied to the correct location, we can remove
			// our www/res directory before packaging to save build time and size...
			// FileUtils.deleteDirectory(new File(wwwDir, "res"));
			
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to process config.xml in your project, check the file's validity");
		}
	}
	
	/**
	 * Check is the current os can build the specified platform.
	 * 
	 * @param platform
	 * @return
	 * @throws Throwable 
	 */
	private boolean checkPlatformCompatibility(MobilePlatform platform) throws Throwable {	    
		// Implement Compatibility matrix
		// Step 1: Check cordova version, compatibility over 3.4.x
		String version = getCordovaVersion();
		
		Pattern pattern = Pattern.compile("^(\\d)+\\.(\\d)+\\.");
		Matcher matcher = pattern.matcher(version);
		
		if (matcher.find()){
			// We check first just the decimal part
			if (Integer.parseInt(matcher.group(1)) < versionMinimalRequiredDecimalPart) {
				return false;
			// Next we check the fractional part
			} else if (Integer.parseInt(matcher.group(1)) == versionMinimalRequiredDecimalPart && 
					Integer.parseInt(matcher.group(2)) < versionMinimalRequiredFractionalPart) {
				return false;
			}
			
		} else {
			return false;
		}

		// Step 2: Check build local platform with mobile platform
		if (platform instanceof Android) {
			return true;
		} else if (platform instanceof IOs) {
			return is(OS.mac);
		} else if (platform.getType().startsWith("Windows")) {
			return is(OS.win32);
		}
		
		return false;
	}
	
	private String getCordovaVersion() throws Throwable {
		if (cordovaVersion == null) {
			cordovaVersion = runCordovaCommand(getPrivateDir(), "-v");
		}
		return cordovaVersion;
	}

	@Override
	public void run() {
		String actionID = action.getId();
		Engine.logEngine.debug("Running " + actionID + " action");
		
		if (actionID.equals("convertigo.action.buildLocallyRelease")){
			buildLocally("release", false, "");
		}
		
		if (actionID.equals("convertigo.action.buildLocallyDebug")){
			buildLocally("debug", false, "");
		}
		
		if (actionID.equals("convertigo.action.runLocally")){
			buildLocally("debug", true, "device");
		}
		
		if (actionID.equals("convertigo.action.emulateLocally")){
			buildLocally("debug", true, "emulator");
		}
		
		if (actionID.equals("convertigo.action.removeCordovaPlatform")){
			removeCordovaPlatform();
		}
		
		if (actionID.equals("convertigo.action.removeCordovaDirectory")){
			removeCordovaDirectory();
		}
	}
	
	/**
	 * 
	 */
	private void buildLocally(final String option, final boolean run, final String target) {
		Cursor waitCursor = new Cursor(ConvertigoPlugin.getDisplay(), SWT.CURSOR_WAIT);
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
		try {
			final MobilePlatform mobilePlatform = getMobilePlatform();
			
			if (mobilePlatform != null) {
				//Check endpoint url is empty or not
				MobileApplication mobileApplication = mobilePlatform.getParent();

				if (mobileApplication.getEndpoint().equals("")) {
					MessageBox informDialog = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
					informDialog.setText("Endpoint URL are empty");
					informDialog.setMessage(
						"You need to have an endpoint URL to continue the local build.\n" +
						"Please enter a valid endpoint URL in the property \"Convertigo server endpoint\" present on \"" + 
						mobileApplication.getName() + "\" object.");
					informDialog.open();
					return;
				}

				//Check compatibility with platform mobile and os where we build
				if (!checkPlatformCompatibility(mobilePlatform)) {   					
					MessageBox informDialog = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
					informDialog.setText("This platform cannot be built");
					informDialog.setMessage(
						"You need at least cordova 3.3\n"
						+ "\n"
						+ "On Windows workstations you can build:\n"
						+ " - Android\n"
						+ " - Windows Phone 8\n"
						+ " - Windows Phone 7\n"
						+ " - Windows 8 \n"
						+ " - Blackberry 10 \n"
						+ "\n"
						+ "On Mac OS workstations you can build:\n"
						+ " - iOS\n"
						+ " - Blackberry 10 \n"
						+ " - Android\n"
						+ "\n"
						+ "On Linux workstations you can build:\n"
						+ " - Blackberry 10 \n"
						+ " - Android\n"
						+ "\n"
						+ "For the moment, these platforms cannot be \"build locally\":\n"
						+ " - Blackberry 10\n"
						+ " - Windows 8\n");

					informDialog.open();
					return;
				}

				// Cordova Env will be created in the _private directory
				final File privateDir = new File(Engine.PROJECTS_PATH + "/" + ConvertigoPlugin.projectManager.currentProject.getName() + "/_private");

				// Just in case .. check that the private directory exists...
				if (!privateDir.exists()) {
					ConvertigoPlugin.logInfo("Creating \"_private\" project directory");
					try {
						privateDir.mkdirs();
					} catch(Exception e) {
						String message = java.text.MessageFormat.format(
							"Unable to create the private project directory \"{0}\"..",
							new Object[] {ConvertigoPlugin.projectManager.currentProject.getName()});
						ConvertigoPlugin.logException(e, message);
						return;
					}
				}

				// Test to see if the Cordova application has been created		        
				if (!new File(privateDir, cordovaDir).exists()) {

					// no Cordova directory has been found ask the user if he wants to create it
					MessageBox customDialog = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.YES | SWT.NO);
					customDialog.setText("Create a Cordova environment");
					customDialog.setMessage(
						"The cordova environment for this project has not been created yet. Creating the\n" +
						"environment must be done once by project. This project's environment will be shared\n" +
						"by all mobile devices for local build.\n\n" +
						"You have to install Cordova on your local machine to be able to build locally.\n" +
						"If Cordova is not yet installed, click 'No' and download cordova from :\n" +
						"http://cordova.apache.org . Be sure to follow all instruction on Cordova's\n" +
						"Website to setup your local Cordova build system. \n\n" +
						"Do you want to create a Cordova environment for your project now ?");

					if (customDialog.open() == SWT.YES) {
						//create a local Cordova Environment
						runCordovaCommand(privateDir, "create", BuildLocallyAction.cordovaDir, 
								mobileApplication.getComputedApplicationId(), 
								mobileApplication.getComputedApplicationName() );

						Engine.logEngine.info("Cordova environment is now ready.");
					} else {
						return;
					}
				}

				// OK we are sure we have a Cordova environment.. Start the build
				Job buildJob = new Job("Local Cordova Build " + (run ? "and Run " : "") + "in progress...") {
					
					@Override
					protected IStatus run(IProgressMonitor progressMonitor) {
						try {
							// Cordova environment is already created, we have to build
							// Step 1: call Mobile packager to prepare the source package
							MobileResourceHelper mobileResourceHelper = new MobileResourceHelper(mobilePlatform, "_private/" + BuildLocallyAction.cordovaDir + "/www");
							File wwwDir = mobileResourceHelper.preparePackage();

							// Step 2: Add platform and Read And process Config.xml to copy needed icons and splash resources
							File cordovaDir = getCordovaDir();
							String cordovaPlatform = mobilePlatform.getCordovaPlatform();
							
							if (mobilePlatform instanceof Android && getCordovaVersion().startsWith("3.5.0")) {
								runCordovaCommand(cordovaDir, "platform", "add", cordovaPlatform + "@3.5.1", "--usenpm");
							} else {
								runCordovaCommand(cordovaDir, "platform", "add", cordovaPlatform);
							}

							processConfigXMLResources(wwwDir, mobilePlatform, cordovaDir);

							// Step 3: Build or Run using Cordova the specific platform.
							if (run) {
								runCordovaCommand(cordovaDir, "run", cordovaPlatform, "--" + option, "--" + target);
							} else {
								runCordovaCommand(cordovaDir, "build", cordovaPlatform, "--" + option);

								// Step 4: Show dialog with path to apk/ipa/xap
								if (!processCanceled) {
									showLocationInstallFile(mobilePlatform, process.exitValue(), errorLines, option);
								}
							}
							
							return org.eclipse.core.runtime.Status.OK_STATUS;
						} catch (Throwable e) {
							ConvertigoPlugin.logException(e, "Error when processing Cordova build");
							return org.eclipse.core.runtime.Status.CANCEL_STATUS;
						}
					}

					@Override
					protected void canceling() {

						//Only for the "Run On Device" action
						if (run) {
							if (is(OS.win32) && (mobilePlatform instanceof WindowsPhone7 || mobilePlatform instanceof WindowsPhone8) ) {
								//kill the CordovaDeploy.exe program only for Windows Phone 7 & 8 build platform
								try {
									Runtime.getRuntime().exec("taskkill /IM CordovaDeploy.exe").waitFor();
								} catch (Exception e) {
									Engine.logEngine.error("Error during kill of process \"CordovaDeploy\"\n" + e.getMessage(), e);
								}
							} else if (mobilePlatform instanceof IOs) {
								//kill the lldb process only for ios build platform
								try {
									Runtime.getRuntime().exec("pkill lldb").waitFor();
								} catch (Exception e) {
									Engine.logEngine.error("Error during kill of process \"lldb\"\n" + e.getMessage(), e);
								}
							}
						}

						processCanceled = true;

						// Others OS
						process.destroy();
					}

				};

				buildJob.setUser(true);
				buildJob.schedule();

			}
		} catch (IOException ee) {
			MessageBox customDialog = new MessageBox(
					shell,
					SWT.ICON_INFORMATION | SWT.OK);
			customDialog.setText("Cordova installation not found");
			customDialog.setMessage("In order to use local build you must install on your workstation a valid\n" +
					"Cordova build system. You can download and install Cordova from \n" +
					"http://cordova.apache.org . Be sure to follow all instruction on Cordova\n" +
					"Website to setup your local Cordova build system. \n\n" +
					"This message can also appear if cordova is not in your PATH."
					);
			customDialog.open();
		} catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to build locally with Cordova");
		}
		finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}
	
	private void showLocationInstallFile(final MobilePlatform mobilePlatform, final int exitValue, final String errorLines, final String buildOption) {
		
		ConvertigoPlugin.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				File builtFile = getAbsolutePathOfBuiltFile(mobilePlatform, buildOption);
				
				BuildLocallyEndingDialog buildSuccessDialog = new BuildLocallyEndingDialog(
					ConvertigoPlugin.getMainShell(), builtFile, exitValue, errorLines, mobilePlatform
				);
				
				buildSuccessDialog.open();
			}
        });
    	
	}
	
	private File getAbsolutePathOfBuiltFile(MobilePlatform mobilePlatform, String buildMode) {
		String cordovaPlatform = mobilePlatform.getCordovaPlatform();
		String builtPath = "/platforms/" + cordovaPlatform + "/";
		String buildMd = buildMode.equals("debug") ? "Debug" : "Release";
		
		String extension = "";
		File f = new File(getCordovaDir(), builtPath);		
		
		if (f.exists()) {
		
			// Android
			if (mobilePlatform instanceof Android) {
				builtPath = builtPath + "ant-build/";
				extension = "apk";
			// iOS
			} else if (mobilePlatform instanceof IOs){
				extension = "xcodeproj";
			// Windows Phone 8
			} else if (mobilePlatform instanceof WindowsPhone8) {
				builtPath = builtPath + "Bin/" + buildMd + "/";
				extension = "xap";
			// Windows Phone 7
			} else if (mobilePlatform instanceof WindowsPhone7) {
				builtPath = builtPath + "Bin/" + buildMd + "/";
				extension = "xap";
			// Blackberry 10
			} else if (mobilePlatform instanceof BlackBerry10) {
				//TODO : Handle BB10
			// Windows 8
			} else if (mobilePlatform instanceof Windows8){
				//TODO : Handle Windows 8
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
			builtPath = "/platforms/" + cordovaPlatform + "/";
		}
		
		return new File (getCordovaDir(), builtPath);
	}

	/**
	 * Removes the CordovaPlatform...
	 * 
	 * Used to clean a broken cordova environment.
	 */
	private void removeCordovaPlatform () {
		final MobilePlatform mobilePlatform = getMobilePlatform();
		
		if (mobilePlatform != null && getCordovaDir().exists()) {
			final String platformName = mobilePlatform.getCordovaPlatform();
			
			Job removeCordovaPlatformJob = new Job("Remove " + platformName + " platform on cordova in progress...") {
				@Override
				protected IStatus run(IProgressMonitor arg0) {
					try {
						runCordovaCommand(getCordovaDir(), "platform", "rm", platformName);

						return org.eclipse.core.runtime.Status.OK_STATUS;

					} catch (Throwable thr) {
						Engine.logEngine
								.error("Error when removing the required mobile platform!",
										thr);
						return org.eclipse.core.runtime.Status.CANCEL_STATUS;
					}
				}
				
				@Override
				protected void canceling() {
					process.destroy();
				}
			};
			removeCordovaPlatformJob.setUser(true);
			removeCordovaPlatformJob.schedule();
		} else {
			Engine.logEngine.error("The platform isn't removed because the Cordova environment doesn't exist.");
		}
	}
	
	/**
	 * Dialog yes/no which ask to user if we want
	 * remove the cordova directory present into "_private" directory
	 * We also explain, what we do and how to recreate the cordova environment
	 */
	private void removeCordovaDirectory() {
		
		MessageBox customDialog = new MessageBox(getParentShell(),
				SWT.ICON_INFORMATION | SWT.YES | SWT.NO);
    	customDialog.setText("Remove cordova directory");
    	customDialog
				.setMessage("Do you want to remove the Cordova directory located in \"_private\" directory?\n\n" +
						"It will also remove this project's Cordova environment!\n\n" +
						"To recreate the project's Cordova environment, you just need to run a new local build."
		);
		
		if (customDialog.open() == SWT.YES) {
			//Step 1: Recover the "cordova" directory	
	        final File cordovaDirectory = getCordovaDir();
			
			//Step 2: Remove the "cordova" directory
	        if (cordovaDirectory.exists()) {
	        	if (FileUtils.deleteQuietly(cordovaDirectory)){
					Engine.logEngine.info("The Cordova environment has been successfull removed.");
				}      		        	
				
	        } else {
				Engine.logEngine.error("The Cordova environment not removed because doesn't exist.");
				return;
	        }

		} else {
			return;
		}		
	}
	
	private MobilePlatform getMobilePlatform() {
		ProjectExplorerView explorerView = getProjectExplorerView();
		
		if (explorerView != null) {
			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
			Object databaseObject = treeObject.getObject();

			if ((databaseObject != null) && (databaseObject instanceof MobilePlatform)) {
				 return (MobilePlatform) treeObject.getObject();
			} 
		}
		return null;
	}
	
	private File getCordovaDir() {
		return new File(getPrivateDir(), BuildLocallyAction.cordovaDir);
	}
	
	private File getPrivateDir() {
		return new File(Engine.PROJECTS_PATH + "/"
				+ ConvertigoPlugin.projectManager.currentProject.getName()
				+ "/_private");
	}

	private OS getOsLocal() {
		if (osLocal == null) {
			String osname = System.getProperty("os.name", "generic").toLowerCase();
			
			if (osname.startsWith("windows")) {
				osLocal = OS.win32;
			} else if (osname.startsWith("linux")) {
				osLocal = OS.linux;
			} else if (osname.startsWith("sunos")) {
				osLocal = OS.solaris;
			} else if (osname.startsWith("mac") || osname.startsWith("darwin")) {
				osLocal = OS.mac;
			} else {
				osLocal = OS.generic;
			}
		}
		return osLocal;
	}
	
	private boolean is(OS os) {
		return getOsLocal() == os;
	}

	private static String getFullPath(String paths, String command) throws IOException {
		for (String path: paths.split(Pattern.quote(File.pathSeparator))) {
			File candidate = new File(path, command);
			if (candidate.exists()) {
				return candidate.getCanonicalPath();
			}
		}
		return null;
	}
}
