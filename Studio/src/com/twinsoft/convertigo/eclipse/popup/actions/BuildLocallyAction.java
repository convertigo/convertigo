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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.apache.commons.io.FileUtils;
import org.apache.xpath.CachedXPathAPI;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.BuildLocalSuccessfulDialog;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.mobiles.MobileResourceHelper;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.ZipUtils;

public class BuildLocallyAction extends MyAbstractAction {

	static final String cordovaDir = "cordova";
	// For minimal version of cordova required 3.4.x
	final int versionMinimalRequiredDecimalPart = 3;
	final int versionMinimalRequiredFractionalPart = 4;
	
	private String projectName = null;
	private String errorLines = null;
	
	private Process process;
	private boolean processCanceled = false;
    String cmdOutput;
    
	private String osLocal;
	private String cordovaPlatform;
	
	/**
	 * 
	 * @author opic
	 * This is a Fake HttpServeltRequest class implementation as the MobileResourceHelper.makeZipPackage takes a
	 * HttpServletRequest as argument.
	 * 
	 * The only Important Methods are getParameter() and getRequestUrl() used by the MakeZipPackage
	 * 	
	 */
	private class InternalRequest implements HttpServletRequest 
	{
		Map<String, String> parameters = new java.util.HashMap<String, String>();
		
		public void setParameter(String name, String value)
		{
			parameters.put(name, value);
		}
		
		@Override
		public Locale getLocale() {
			return null;
		}

		@Override
		public Enumeration<Locale> getLocales() {
			return null;
		}

		@Override
		public AsyncContext getAsyncContext() {
			return null;
		}

		@Override
		public Object getAttribute(String arg0) {
			return null;
		}

		@Override
		public Enumeration<String> getAttributeNames() {
			return null;
		}

		@Override
		public String getCharacterEncoding() {
			return null;
		}

		@Override
		public int getContentLength() {
			return 0;
		}

		@Override
		public String getContentType() {
			return null;
		}

		@Override
		public DispatcherType getDispatcherType() {
			return null;
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			return null;
		}

		@Override
		public String getLocalAddr() {
			return null;
		}

		@Override
		public String getLocalName() {
			return null;
		}

		@Override
		public int getLocalPort() {
			return 0;
		}

		@Override
		public String getParameter(String name) {
			return parameters.get(name);
		}

		@Override
		public Map<String, String[]> getParameterMap() {
			return null;
		}

		@Override
		public Enumeration<String> getParameterNames() {
			return null;
		}

		@Override
		public String[] getParameterValues(String arg0) {
			return null;
		}

		@Override
		public String getProtocol() {
			return null;
		}

		@Override
		public BufferedReader getReader() throws IOException {
			return null;
		}

		@Override
		public String getRealPath(String arg0) {
			return null;
		}

		@Override
		public String getRemoteAddr() {
			return null;
		}

		@Override
		public String getRemoteHost() {
			return null;
		}

		@Override
		public int getRemotePort() {
			return 0;
		}

		@Override
		public RequestDispatcher getRequestDispatcher(String arg0) {
			return null;
		}

		@Override
		public String getScheme() {
			return null;
		}

		@Override
		public String getServerName() {
			
			return null;
		}

		@Override
		public int getServerPort() {
			
			return 0;
		}

		@Override
		public ServletContext getServletContext() {
			
			return null;
		}

		@Override
		public boolean isAsyncStarted() {
			
			return false;
		}

		@Override
		public boolean isAsyncSupported() {
			
			return false;
		}

		@Override
		public boolean isSecure() {
			
			return false;
		}

		@Override
		public void removeAttribute(String arg0) {
			
			
		}

		@Override
		public void setAttribute(String arg0, Object arg1) {
			
			
		}

		@Override
		public void setCharacterEncoding(String arg0)
				throws UnsupportedEncodingException {
			
			
		}

		@Override
		public AsyncContext startAsync() {
			
			return null;
		}

		@Override
		public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) {
			
			return null;
		}

		@Override
		public boolean authenticate(HttpServletResponse arg0)
				throws IOException, ServletException {
			
			return false;
		}

		@Override
		public String getAuthType() {
			
			return null;
		}

		@Override
		public String getContextPath() {
			
			return null;
		}

		@Override
		public Cookie[] getCookies() {
			
			return null;
		}

		@Override
		public long getDateHeader(String arg0) {
			
			return 0;
		}

		@Override
		public String getHeader(String arg0) {
			
			return null;
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			
			return null;
		}

		@Override
		public Enumeration<String> getHeaders(String arg0) {
			
			return null;
		}

		@Override
		public int getIntHeader(String arg0) {
			
			return 0;
		}

		@Override
		public String getMethod() {
			
			return null;
		}

		@Override
		public Part getPart(String arg0) throws IOException,
				IllegalStateException, ServletException {
			
			return null;
		}

		@Override
		public Collection<Part> getParts() throws IOException,
				IllegalStateException, ServletException {
			
			return null;
		}

		@Override
		public String getPathInfo() {
			
			return null;
		}

		@Override
		public String getPathTranslated() {
			
			return null;
		}

		@Override
		public String getQueryString() {
			
			return null;
		}

		@Override
		public String getRemoteUser() {
			
			return null;
		}

		@Override
		public String getRequestURI() {
			
			return null;
		}

		@Override
		public StringBuffer getRequestURL() {
			// TODO : For the moment the Url is fixed with <address> This will be used for the endpoint
			// calculation.. We have to override this with the real address...
			return new StringBuffer("http://<address>/convertigo/admin/services/mobiles.GetSourcePackage");
		}

		@Override
		public String getRequestedSessionId() {
			
			return null;
		}

		@Override
		public String getServletPath() {
			
			return null;
		}

		@Override
		public HttpSession getSession() {
			
			return null;
		}

		@Override
		public HttpSession getSession(boolean arg0) {
			
			return null;
		}

		@Override
		public Principal getUserPrincipal() {
			
			return null;
		}

		@Override
		public boolean isRequestedSessionIdFromCookie() {
			
			return false;
		}

		@Override
		public boolean isRequestedSessionIdFromURL() {
			
			return false;
		}

		@Override
		public boolean isRequestedSessionIdFromUrl() {
			
			return false;
		}

		@Override
		public boolean isRequestedSessionIdValid() {
			
			return false;
		}

		@Override
		public boolean isUserInRole(String arg0) {
			
			return false;
		}

		@Override
		public void login(String arg0, String arg1) throws ServletException {
			
			
		}

		@Override
		public void logout() throws ServletException {
			
			
		}

	}
	
	public BuildLocallyAction() {
		super();
	}

	private boolean delete(File f)  {
		if (f.isDirectory()) {
			for (File c : f.listFiles())
				delete(c);
			}
		if (!f.delete()){
		    Engine.logEngine.error("Failed to delete file: " + f);
			return false;
		}else {
			return true;
		}
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
	private String runCordovaCommand(String Command, File projectDir) throws Throwable {
		String[] envp = null;
		Map<String, String> envmap;
		envmap = System.getenv();
		envp = new String[envmap.size()];
		int i =0;
		for (Map.Entry<String, String> entry : envmap.entrySet())
			envp[i++] = entry.getKey() + "=" + entry.getValue();
		
		String shell = getShellCommand("cordova");
		
		process = Runtime.getRuntime().exec(shell + " " + Command,
											envp,
											projectDir
		);
		
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
					
					while ((line = bis.readLine()) != null) {
						Engine.logEngine.info(line);
						BuildLocallyAction.this.cmdOutput += line;
					}
					while ((line = bes.readLine()) != null) {
						Engine.logEngine.error(line);
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
	private void ProcessConfigXMLResources(File wwwDir, String platform, File cordovaDir) throws Throwable
	{
		try {
			Document doc = XMLUtils.loadXml(new File(wwwDir, "config.xml"));
			CachedXPathAPI xpathApi = new CachedXPathAPI();
			
			/*
			 * Handle plugins in the config.xml file and test to see if the plugin is not already installed
			 */
			Engine.logEngine.info("Checking installed plugins... ");
			String installedPlugins = runCordovaCommand("plugin list ", cordovaDir);
			NodeList plugins = xpathApi.selectNodeList(doc.getDocumentElement(), "//*[local-name()='plugin']");
			for(int i=0; i< plugins.getLength(); i++) {
				String options = "";
				Node plugin = plugins.item(i);
				String pluginName = plugin.getAttributes().getNamedItem("name").getTextContent();
				String gitUrl = null;
				String version = null;

				/**
				 * Build an optional --variable <NAME>=<VALUE> list
				 */
				NodeList pluginParams = xpathApi.selectNodeList(plugin, "//param");
				if (pluginParams.getLength() > 0) {
					for (int j=0; j< pluginParams.getLength(); j++) {
						Node param = pluginParams.item(j);
						if (param.getAttributes().getNamedItem("name") != null && param.getAttributes().getNamedItem("value") != null) {
							options += " --variable " + param.getAttributes().getNamedItem("name").getTextContent() + "=" + param.getAttributes().getNamedItem("value").getTextContent(); 
						}
					}
				}
				
				if (plugin.getAttributes().getNamedItem("git") != null)
					gitUrl     = plugin.getAttributes().getNamedItem("git").getTextContent();
				if (plugin.getAttributes().getNamedItem("version") != null)
					version    = plugin.getAttributes().getNamedItem("version").getTextContent();
				
				if (installedPlugins.toLowerCase().indexOf(pluginName.toLowerCase()) == -1) {
					Engine.logEngine.info("Adding plugin " + pluginName);
					// if we have a gitUrl use it in priority
					if (gitUrl != null)
						runCordovaCommand("plugin add " + gitUrl + options, cordovaDir);
					else
						runCordovaCommand("plugin add " + pluginName + (version != null ? "@" + version: "") + options, cordovaDir);
				}	
			}

			//ANDROID
			if (platform.equalsIgnoreCase("android")) {
				NodeList icons = xpathApi.selectNodeList(doc.getDocumentElement(), "//icon[@platform = 'android']");
				// for splashes, as there is the the 'gap:' name space use the local-name xpath function instead 
				NodeList splashes = xpathApi.selectNodeList(doc.getDocumentElement(), "//*[local-name()='splash' and @platform = 'android']");
				
				// Copy the icons to the correct res directory
				for(int i=0; i< icons.getLength(); i++) {
					Node icon = icons.item(i);
					NamedNodeMap nodeMap = icon.getAttributes();
					String source = nodeMap.getNamedItem("src").getTextContent();
					String density = nodeMap.getNamedItem("gap:density").getTextContent();
					File iconSrc = new File(wwwDir, source);
					File dest = new File(cordovaDir, "platforms/" + platform + "/res/drawable-" + density + "/icon.png");
					Engine.logEngine.debug("Copying " + iconSrc.getAbsolutePath() + " to " + dest.getAbsolutePath());
					FileUtils.copyFile(iconSrc, dest);
					if (density.equalsIgnoreCase("ldpi")) {
						// special case for ldpi assume it goes also in the drawable folder
						dest = new File(cordovaDir, "platforms/" + platform + "/res/drawable/icon.png");
						Engine.logEngine.debug("Copying " + iconSrc.getAbsolutePath() + " to " + dest.getAbsolutePath());
						FileUtils.copyFile(iconSrc, dest);
					}
				}
				
				// now the stuff for splashes
				for(int i=0; i< splashes.getLength(); i++) {
					Node splash = splashes.item(i);
					NamedNodeMap nodeMap = splash.getAttributes();
					String source = nodeMap.getNamedItem("src").getTextContent();
					String density = nodeMap.getNamedItem("gap:density").getTextContent();
					File splashSrc = new File(wwwDir, source);
					File dest = new File(cordovaDir, "platforms/" + platform + "/res/drawable-" + density + "/splash.png");
					Engine.logEngine.debug("Copying " + splashSrc.getAbsolutePath() + " to " + dest.getAbsolutePath());
					FileUtils.copyFile(splashSrc, dest);
					if (density.equalsIgnoreCase("ldpi")) {
						// special case for ldpi assume it goes also in the drawable folder
						dest = new File(cordovaDir, "platforms/" + platform + "/res/drawable/splash.png");
						Engine.logEngine.debug("Copying " + splashSrc.getAbsolutePath() + " to " + dest.getAbsolutePath());
						FileUtils.copyFile(splashSrc, dest);
					}
				}
			}
			
			//iOS
			if (platform.equalsIgnoreCase("ios")) {
				Map<String, String> iconsCorrespondences, splashCorrespondences = new HashMap<String, String>();
				iconsCorrespondences = getiOSIconsCorrespondences();
				splashCorrespondences = getiOSSplashCorrespondences();
				
				NodeList icons = xpathApi.selectNodeList(doc.getDocumentElement(), "//icon[@platform = 'ios']");
				// for splashes, as there is the the 'gap:' name space use the local-name xpath function instead 
				NodeList splashes = xpathApi.selectNodeList(doc.getDocumentElement(), "//*[local-name()='splash' and @platform = 'ios']");
				
				// Copy the icons to the correct res directory
				for(int i=0; i< icons.getLength(); i++) {
					Node icon = icons.item(i);
					NamedNodeMap nodeMap = icon.getAttributes();
					String source = nodeMap.getNamedItem("src").getTextContent();
					String height = nodeMap.getNamedItem("height").getTextContent();
					String width = nodeMap.getNamedItem("width").getTextContent();
					String iconName = iconsCorrespondences.get(width + "x" + height);
					File iconSrc = new File(wwwDir, source);
					
					File dest = new File(cordovaDir, "platforms/" + platform + "/" + projectName + "/Resources/icons/" + iconName );
					//File dest = new File(cordovaDir, "platforms/" + platform + "/www/res/icon/" + iconName );

					Engine.logEngine.debug("Copying " + iconSrc.getAbsolutePath() + " to " + dest.getAbsolutePath());
					FileUtils.copyFile(iconSrc, dest);
				}
				
				// now the stuff for splashes
				for(int i=0; i< splashes.getLength(); i++) {
					Node splash = splashes.item(i);
					NamedNodeMap nodeMap = splash.getAttributes();
					String source = nodeMap.getNamedItem("src").getTextContent();
					String height = nodeMap.getNamedItem("height").getTextContent();
					String width = nodeMap.getNamedItem("width").getTextContent();
					File splashSrc = new File(wwwDir, source);
					String splashName = splashCorrespondences.get(width + "x" + height);
					
					File dest = new File(cordovaDir, "platforms/" + platform + "/" + projectName + "/Resources/splash/" + splashName);
					//File dest = new File(cordovaDir, "platforms/" + platform + "/www/res/splash/" + splashName);
					
					Engine.logEngine.debug("Copying " + splashSrc.getAbsolutePath() + " to " + dest.getAbsolutePath());
					FileUtils.copyFile(splashSrc, dest);
				}
			}
			
			//WINPHONE
			if (platform.equalsIgnoreCase("wp7") || platform.equalsIgnoreCase("wp8")) {
				NodeList icons = xpathApi.selectNodeList(doc.getDocumentElement(), "//icon[@platform = 'winphone']");
				// for splashes, as there is the the 'gap:' name space use the local-name xpath function instead 
				NodeList splashes = xpathApi.selectNodeList(doc.getDocumentElement(), "//*[local-name()='splash' and @platform = 'winphone']");
				for(int i=0; i< icons.getLength(); i++) {
					Node icon = icons.item(i);
					NamedNodeMap nodeMap = icon.getAttributes();
					String source = nodeMap.getNamedItem("src").getTextContent();
					String role = (nodeMap.getNamedItem("gap:role") != null) ? nodeMap.getNamedItem("gap:role").getTextContent() : "";
					File iconSrc = new File(wwwDir, source);
					File dest = new File(cordovaDir, "platforms/" + platform + "/ApplicationIcon.png");
					Engine.logEngine.debug("Copying " + iconSrc.getAbsolutePath() + " to " + dest.getAbsolutePath());
					FileUtils.copyFile(iconSrc, dest);
					if (role.equalsIgnoreCase("background")) {
						// special case for background 
						dest = new File(cordovaDir, "platforms/" + platform + "/Background.png");
						Engine.logEngine.debug("Copying " + iconSrc.getAbsolutePath() + " to " + dest.getAbsolutePath());
						FileUtils.copyFile(iconSrc, dest);
					}
				}
				
				// now the stuff for splashes
				for(int i=0; i< splashes.getLength(); i++) {
					Node splash = splashes.item(i);
					NamedNodeMap nodeMap = splash.getAttributes();
					String source = nodeMap.getNamedItem("src").getTextContent();
					File splashSrc = new File(wwwDir, source);
					File dest = new File(cordovaDir, "platforms/" + platform + "/SplashScreenImage.jpg");
					Engine.logEngine.debug("Copying " + splashSrc.getAbsolutePath() + " to " + dest.getAbsolutePath());
					FileUtils.copyFile(splashSrc, dest);
				}
			}

			// TODO : Add platform BB10
			if (platform.equalsIgnoreCase("wp7") || platform.equalsIgnoreCase("wp8")) {
				
			}
			// TODO : Add platform Windows 8
			if (platform.equalsIgnoreCase("wp7") || platform.equalsIgnoreCase("wp8")) {
				
			}
			
			// We have to add the the root Config.xml all our app's config.xml preferences.
			// Cordova will use this file to generates the platform specific config.xml
			
			Document rootConfigXML = XMLUtils.loadXml(new File(cordovaDir, "config.xml"));  // The root config.xml
			NodeList preferences = xpathApi.selectNodeList(doc.getDocumentElement(), "//preference"); 
			for (int i=0; i< preferences.getLength(); i++) {
				Node preference = preferences.item(i);
				Element elt = rootConfigXML.createElement("preference");
				String name = preference.getAttributes().getNamedItem("name").getTextContent();
				String value = preference.getAttributes().getNamedItem("value").getTextContent();
				elt.setAttribute("name", name);
				elt.setAttribute("value", value);
				Engine.logEngine.info("Adding preference'" + name + "' with value '" + value + "'");					
				rootConfigXML.importNode(elt, true);
				rootConfigXML.getFirstChild().appendChild(elt);
				
			}
			Engine.logEngine.trace("New config.xml is: " + XMLUtils.prettyPrintDOM(rootConfigXML));
			File resXmlFile = new File(cordovaDir, "config.xml");
			FileUtils.deleteQuietly(resXmlFile);
			XMLUtils.saveXml(rootConfigXML, resXmlFile.getAbsolutePath());
			
			// Last part , as all resources has been copied to the correct location, we can remove
			// our www/res directory before packaging to save build time and size...
			// FileUtils.deleteDirectory(new File(wwwDir, "res"));
			
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to process config.xml in your project, check the file's validity");
		}
	}
	
	/**
	 * Methods implements to know which icon goes with which name on ios platform
	 * in function of height and width
	 * @return 
	 */
	private Map<String, String> getiOSIconsCorrespondences() {
		Map<String, String> iconsCorrespondences = new HashMap<String, String>();
		//iOS 7.0+ 
		// iPhone/iPod Touch
		iconsCorrespondences.put("60x60", "icon-60.png");
		iconsCorrespondences.put("120x120", "icon-60@2x.png");
		//iPad
		iconsCorrespondences.put("76x76", "icon-76.png");
		iconsCorrespondences.put("152x152", "icon-76@2x.png");
		//iOS 6.1+
		//Spotlight Icon
		iconsCorrespondences.put("40x40", "icon-40.png");
		iconsCorrespondences.put("80x80", "icon-40@2x.png");
		//iPhone/iPod Touch
		iconsCorrespondences.put("57x57", "icon.png");
		iconsCorrespondences.put("114x114", "icon@2x.png");
		//iPad
		iconsCorrespondences.put("72x72", "icon-72.png");
		iconsCorrespondences.put("144x144", "icon-72@2x.png");
		//iPhone Spotlight and Settings Icon
		iconsCorrespondences.put("29x29", "icon-small.png");
		iconsCorrespondences.put("58x58", "icon-small@2x.png");
		//iPad Spotlight and Settings Icon
		iconsCorrespondences.put("50x50", "icon-50.png");
		iconsCorrespondences.put("100x100", "icon-50@2x.png");
		
		return iconsCorrespondences;
	}
	
	/**
	 * Methods implements to know which splash goes with which name on ios platform
	 * in function of height and width
	 * @return 
	 */
	private Map<String, String> getiOSSplashCorrespondences() {
		Map<String, String> splashCorrespondences = new HashMap<String, String>();
		
		// iPhone
		splashCorrespondences.put("640x1136", "Default-568h@2x~iphone.png");
		splashCorrespondences.put("640x960", "Default@2x~iphone.png");
		splashCorrespondences.put("320x480", "Default~iphone.png");

		//iPad
		splashCorrespondences.put("2048x1496", "Default-Landscape@2x~ipad.png");
		splashCorrespondences.put("1024x748", "Default-Landscape~ipad.png");
		splashCorrespondences.put("768x1004", "Default-Portrait~ipad.png");
		splashCorrespondences.put("1536x2008", "Default-Portrait@2x~ipad.png");
		
		return splashCorrespondences;
	}

	/**
	 * Returns a cordova platform name from the MobilePlatform.type() name
	 * 
	 * @param deviceType
	 * @return
	 */
	
	String computeCordovaPlatform(String deviceType) {
		if (deviceType.equalsIgnoreCase("WindowsPhone8"))
			return "wp8";
		else if (deviceType.equalsIgnoreCase("WindowsPhone7"))
			return "wp7";
		else if (deviceType.equalsIgnoreCase("Windows8"))
			return "win8";
		else
			return deviceType;
	}
	
	/**
	 * Check is the current os can build the specified platform.
	 * 
	 * @param platform
	 * @return
	 * @throws Throwable 
	 */
	private boolean checkPlatformCompatibility(MobilePlatform platform) throws Throwable {
		String osname = System.getProperty("os.name", "generic").toLowerCase();

		
		if (osname.startsWith("windows")) {
			osLocal = "win32";
		} else if (osname.startsWith("linux")) {
			osLocal = "linux";
		} else if (osname.startsWith("sunos")) {
			osLocal = "solaris";
		} else if (osname.startsWith("mac") || osname.startsWith("darwin")) {
			osLocal = "mac";
		} else {
			osLocal = "generic";
		}
	    
		// Implement Compatibility matrix
		// Step 1: Check cordova version, compatibility over 3.3.x
		File privateDir = getPrivateDir();
		String version = runCordovaCommand("-v", privateDir);
		
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
		if (osLocal.equalsIgnoreCase("win32")
				&& platform.getType().equalsIgnoreCase("iOS")) {
			return false;
		}

		if (osLocal.equalsIgnoreCase("mac")
				&& platform.getType().startsWith("Windows")) {
			return false;
		}

		if (osLocal.equalsIgnoreCase("linux")
				&& (platform.getType().startsWith("Windows") || platform
						.getName().equalsIgnoreCase("iOS"))) {
			return false;
		}
		
		// Step 3: Check if platform is possible to build locally.
		String deviceType = computeCordovaPlatform(platform.getType());
    	if (deviceType.equalsIgnoreCase("blackberry10") || deviceType.equals("win8") ){       	
        	return false;
    	}
		
		return true;
	}
	
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
	public void buildLocally(final String option, final boolean run, final String target) {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
		try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
    			Object databaseObject = treeObject.getObject();

    			if ((databaseObject != null) && (databaseObject instanceof MobilePlatform)) {
    				final MobilePlatform mobileDevice = (MobilePlatform) treeObject.getObject();
    				
    				//Check endpoint url is empty or not
    				if (mobileDevice.getParent() != null) {
    					Object mobileApplication = mobileDevice.getParent();
    					if (mobileApplication instanceof MobileApplication) {
    						if (( (MobileApplication) mobileApplication).getEndpoint().equals("")) {
    							MessageBox informDialog = new MessageBox(shell,
    									SWT.ICON_INFORMATION | SWT.OK);
    							informDialog.setText("Endpoint URL are empty");
    							informDialog
    									.setMessage("You need to have an endpoint URL to continue the local build.\n" +
    											"Please enter a valid endpoint URL in the property \"Convertigo server endpoint\" present on \""+((MobileApplication) mobileApplication).getName()+"\" object.");

    							informDialog.open();
    							return;
    						}
    					}
    				}
    				
    				//Check compatibility with platform mobile and os where we build
    				if (!checkPlatformCompatibility(mobileDevice)) {   					
						MessageBox informDialog = new MessageBox(shell,
								SWT.ICON_INFORMATION | SWT.OK);
						informDialog.setText("This platform cannot be built");
						informDialog
								.setMessage("You need at least cordova 3.3\n"
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
										+ "For the moment, this platform is not possible to \"build locally\":\n"
										+ " - Blackberry 10\n"
										+ " - Windows 8\n");

						informDialog.open();
    					return;
    				}
    				
    				// get the application name from the Mobile devices's property or if empty the project's name
    				final String applicationName = ((MobileApplication) mobileDevice.getParent()).getApplicationName().isEmpty() ?
    								ConvertigoPlugin.projectManager.currentProject.getName() :
    								((MobileApplication)mobileDevice.getParent()).getApplicationName();
    								
					this.projectName = applicationName;
    								
    				final String applicationId = ((MobileApplication)mobileDevice.getParent()).getApplicationId().isEmpty() ?
							"com.convertigo.mobile." + ConvertigoPlugin.projectManager.currentProject.getName() :
							((MobileApplication)mobileDevice.getParent()).getApplicationId();

    				// Cordova Env will be created in the _private directory
			        final File privateDir = new File(Engine.PROJECTS_PATH + "/" + ConvertigoPlugin.projectManager.currentProject.getName() + "/_private");
			        // Just in case .. check that the private directory exists...
			        if (!privateDir.exists()) {
			        	ConvertigoPlugin.logInfo("Creating \"_private\" project directory");
			            try {
			                privateDir.mkdirs();
			            }
			            catch(Exception e) {
			                String message = java.text.MessageFormat.format(
			                    "Unable to create the private project directory \"{0}\"..",
			                    new Object[] { ConvertigoPlugin.projectManager.currentProject.getName() }
			                );
			                ConvertigoPlugin.logException(e, message);
			                return;
			            }
			        }
			        
			        // Test to see if the Cordova application has been created
			        String[] cordovaDirs =  privateDir.list(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							if (name.equalsIgnoreCase(cordovaDir))
								return true;
							else
								return false;
						}
					});
			        if (cordovaDirs.length == 0) {
			        	// no Cordova directory has been found ask the user if he wants to create it
			        	MessageBox customDialog = new MessageBox(shell,
								SWT.ICON_INFORMATION | SWT.YES | SWT.NO);
			        	customDialog.setText("Create a Cordova environment");
			        	customDialog
								.setMessage("The cordova environment for this project has not been created yet. Creating the\n" +
		    							"environment must be done once by project. This project's environment will be shared\n" +
		    							"by all mobile devices for local build.\n\n" +
		    							"You have to install Cordova on your local machine to be able to build locally.\n" +
		    							"If Cordova is not yet installed, click 'No' and download cordova from :\n" +
		    							"http://cordova.apache.org . Be sure to follow all instruction on Cordova's\n" +
		    							"Website to setup your local Cordova build system. \n\n" +
		    							"Do you want to create a Cordova environment for your project now ?"
		    			);
						
    					if (customDialog.open() == SWT.YES) {
    						//create a local Cordova Environment
    						runCordovaCommand("create " + BuildLocallyAction.cordovaDir + " " + applicationId + " " + applicationName , privateDir);
    						
    						Engine.logEngine.info("Cordova environment is now ready.");
    					} else {
    						return;
    					}
			        } 
			        
			        // OK we are sure we have a Cordova environment.. Start the build
		        	Job buildJob = new Job("Local Cordova Build " + (run ? "and Run ":"") + "in progress...") {
						@Override
						protected IStatus run(IProgressMonitor arg0) {
							try {
					        	// Cordova environment is already created, we have to build
					        	// Step 1 : Delete everything in the Cordova's www directory
					        	File wwwDir = new File(privateDir.getAbsolutePath() + "/" + BuildLocallyAction.cordovaDir + "/www");
				        		delete(wwwDir);
					        	
					        	// Step 2 call Mobile packager to build ZIP package, simulate a fake HttpRequest
					        	InternalRequest myRequest = new InternalRequest();
					        	myRequest.setParameter("project", ConvertigoPlugin.projectManager.currentProject.getName());
					        	myRequest.setParameter("platform", mobileDevice.getName());
					        	MobileResourceHelper mobileResourceHelper = new MobileResourceHelper(myRequest, "mobile/flashupdate");
					        	File mobileArchiveFile = mobileResourceHelper.makeZipPackage();
					        	Engine.logEngine.info("ZIP Build package created in : " + mobileArchiveFile.getAbsolutePath());
					        	
					        	// Step 3 : Unzip in the www directory
					        	ZipUtils.expandZip(mobileArchiveFile.getAbsolutePath(),
					        			           wwwDir.getAbsolutePath());
					        	Engine.logEngine.info("ZIP expanded in : " + wwwDir.getAbsolutePath());
					        	
					        	// Step 3Bis : Add platform and Read And process Config.xml to copy needed icons and splash resources
					        	File cordovaDir = getCordovaDir();
					        	cordovaPlatform = computeCordovaPlatform(mobileDevice.getType().toLowerCase());				        	
								
					        	runCordovaCommand("platform add " + cordovaPlatform, cordovaDir);
					        	ProcessConfigXMLResources(wwwDir, cordovaPlatform, cordovaDir);
					        	
					        	// Step 4: Build or Run using Cordova the specific platform.
					        	if (run) {
					        		runCordovaCommand("run " + cordovaPlatform + " --"+option+ " --" + target , cordovaDir);
					        	} else {
					        		runCordovaCommand("build " + cordovaPlatform + " --"+option, cordovaDir);
					        	}
					        	
					        	// Step 5: Show dialog with path to apk/ipa/xap
					        	showLocationInstallFile(cordovaPlatform, applicationName, option);
					        	
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
								if (osLocal != null) {
									// UNIX OS
									if (osLocal.equals("linux") || osLocal.equals("mac")) {
										if (cordovaPlatform != null && cordovaPlatform.equals("ios")) {
											//kill the lldb process only for ios build platform
											try {
												Runtime.getRuntime().exec("pkill lldb").waitFor();
											} catch (Exception e) {
												Engine.logEngine.error("Error during kill of process \"lldb\"\n" + e.getMessage(), e);
											}
										}
									}
									
									//WINDOWS OS
									if (osLocal.equals("win32")){
										if (cordovaPlatform != null && (cordovaPlatform.equals("wp7") || cordovaPlatform.equals("wp8")) ) {
											//kill the CordovaDeploy.exe program only for Windows Phone 7 & 8 build platform
											try {
												Runtime.getRuntime().exec("taskkill /IM CordovaDeploy.exe").waitFor();
											} catch (Exception e) {
												Engine.logEngine.error("Error during kill of process \"CordovaDeploy\"\n" + e.getMessage(), e);
											}
										}
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
	
	private void showLocationInstallFile(final String cordovaPlatform, 
			final String applicationName, final String buildOption) {
		final Display display = Display.getDefault();
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				File buildedFile = getAbsolutePathOfBuildedFile(applicationName, cordovaPlatform, buildOption);
				
				BuildLocalSuccessfulDialog buildSuccessDialog = new BuildLocalSuccessfulDialog(
						display.getActiveShell(),
						buildedFile.getAbsolutePath(), applicationName,
						cordovaPlatform);
				
				buildSuccessDialog.open();
			}
        });
    	
	}
	
	public File getAbsolutePathOfBuildedFile(String applicationName, String cordovaPlatform, String buildMode) {
		String buildedPath = null;
		String buildMd = buildMode.equals("debug") ? "Debug" : "Release";
		
		if (cordovaPlatform.equals("android")) {
			buildedPath = getCordovaDir().getAbsolutePath() +
					"\\platforms\\" + 
					cordovaPlatform + "\\ant-build\\" + applicationName + "-" + buildMode + ".apk";
		} else if (cordovaPlatform.equals("ios")){
			// iOS
			buildedPath = getCordovaDir().getAbsolutePath() +
					"/platforms/" + cordovaPlatform + "/" + applicationName + ".xcodeproj";
		} else if ((cordovaPlatform.equals("wp7")) || (cordovaPlatform.equals("wp8"))) {
			
			// WP8
			if (cordovaPlatform.equals("wp8")) {
				buildedPath = getCordovaDir().getAbsolutePath() + 
					"\\platforms\\" + 
					cordovaPlatform + "\\Bin\\" + buildMd + "\\CordovaAppProj_" + buildMd + "_AnyCPU.xap";
			}
			
			//WP7
			if (cordovaPlatform.equals("wp7")) {
				buildedPath = getCordovaDir().getAbsolutePath() + 
					"\\platforms\\" + 
					cordovaPlatform + "\\Bin\\" + buildMd + "\\com.convertigo.mobile." + applicationName + ".xap";
			}
			
		} else if (cordovaPlatform.equals("blackberry10")){
			//TODO : Handle BB10
		} else if (cordovaPlatform.equals("win8")){
			//TODO : Handle Windows 8
		} else {
			return null;
		}
		
		return new File (buildedPath);
	}

	/**
	 * Removes the CordovaPlatform...
	 * 
	 * Used to clean a broken cordova environment.
	 */
	public void removeCordovaPlatform(){
		final MobilePlatform mobilePlatform = getMobilePlatform();
		if (mobilePlatform != null) {
			final String platformName = computeCordovaPlatform(mobilePlatform
					.getType().toLowerCase());
			Job removeCordovaPlatformJob = new Job("Remove " + platformName + " platform on cordova in progress...") {
				@Override
				protected IStatus run(IProgressMonitor arg0) {
					try {

						runCordovaCommand("platform rm "
								+ platformName,
								getCordovaDir());

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
		}
		
	}
	
	/**
	 * Dialog yes/no which ask to user if we want
	 * remove the cordova directory present into "_private" directory
	 * We also explain, what we do and how to recreate the cordova environment
	 */
	public void removeCordovaDirectory() {
		
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
	        	if (delete(cordovaDirectory)){
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
	
	private File getCordovaDir(){
		return new File(getPrivateDir().getAbsolutePath() + "/" + BuildLocallyAction.cordovaDir);
	}
	
	private File getPrivateDir() {
		return new File(Engine.PROJECTS_PATH + "/"
				+ ConvertigoPlugin.projectManager.currentProject.getName()
				+ "/_private");
	}
	
	private String getShellCommand(String command){
		// TODO: Handle command for Linux and MacOS (Should be android.sh...)
		
		String system = System.getProperty("os.name");
		String shell = command + ".cmd";
		if (system.toLowerCase().indexOf("windows") != -1) {
			shell = command + ".cmd";
		} else if (system.toLowerCase().indexOf("mac") != -1) {
			shell = command;
		}
		
		return shell;
	}

}
