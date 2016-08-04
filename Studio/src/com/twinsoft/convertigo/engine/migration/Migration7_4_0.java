package com.twinsoft.convertigo.engine.migration;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.mobileplatforms.Android;
import com.twinsoft.convertigo.beans.mobileplatforms.IOs;
import com.twinsoft.convertigo.beans.mobileplatforms.WindowsPhone8;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class Migration7_4_0 {
	
	public static void migrate(String projectName) {
		try {
		
			Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
			
			MobileApplication mobileApplication = project.getMobileApplication();
			if (mobileApplication != null) {

				TwsCachedXPathAPI xpathApi = new TwsCachedXPathAPI();
				
				// Tags to change for all platforms
				List<XPathToCheck> xPathToCheckDefault = new LinkedList<XPathToCheck>();
				xPathToCheckDefault.add(new XPathToCheck("/widget/preference[@name='phonegap-version']", "", true));
				xPathToCheckDefault.add(new XPathToCheck("/widget/preference[@name='SplashScreen']", "", true));
				xPathToCheckDefault.add(new XPathToCheck("/widget/preference[@name='ShowSplashScreenSpinner']", "", true));

				String[][] pluginList = new String[][] {	{"org.apache.cordova.device", "org.apache.cordova.file", "org.apache.cordova.file-transfer", "org.apache.cordova.splashscreen", 
					"cordova-plugin-whitelist", "org.apache.cordova.console"}, 
					 {"cordova-plugin-device", "cordova-plugin-file", "cordova-plugin-file-transfer", "cordova-plugin-splashscreen", 
						 "cordova-plugin-whitelist", "cordova-plugin-console"}};
				for (int i = 0; i < pluginList[0].length; i++) {
					xPathToCheckDefault.add(new XPathToCheck("/widget/*[local-name()='plugin' and @name='" + pluginList[1][i] + "']", "/widget/plugin[@name='" + pluginList[1][i] + "']", true));
					xPathToCheckDefault.add(new XPathToCheck("/widget/*[local-name()='plugin' and @name='" + pluginList[0][i] + "']", "/widget/plugin[@name='" + pluginList[1][i] + "']", true));
				}

				pluginList = new String[][] {	{"com.couchbase.lite.phonegap", "org.apache.cordova.battery-status", "org.apache.cordova.camera", 
					 "org.apache.cordova.media-capture", "org.apache.cordova.contacts", "org.apache.cordova.device-motion", "org.apache.cordova.device-orientation", "org.apache.cordova.dialogs", 
					 "org.apache.cordova.geolocation", "org.apache.cordova.globalization", "org.apache.cordova.inappbrowser", "org.apache.cordova.media", "org.apache.cordova.network-information", 
					 "org.apache.cordova.vibration", "org.apache.cordova.statusbar", "com.phonegap.plugins.pushplugin", "com.phonegap.plugins.barcodescanner"}, 
					 {"couchbase-lite-phonegap-plugin", "cordova-plugin-battery-status", "cordova-plugin-camera", 
						 "cordova-plugin-media-capture", "cordova-plugin-contacts", "cordova-plugin-device-motion", "cordova-plugin-device-orientation", "cordova-plugin-dialogs", 
						 "cordova-plugin-geolocation", "cordova-plugin-globalization", "cordova-plugin-inappbrowser", "cordova-plugin-media", "cordova-plugin-network-information", 
						 "cordova-plugin-vibration", "cordova-plugin-statusbar", "phonegap-plugin-push", "phonegap-plugin-barcodescanner"}};

				// Tags to change only for Android
				List<XPathToCheck> xPathToCheckAndroid = new LinkedList<XPathToCheck>();
				xPathToCheckAndroid.addAll(xPathToCheckDefault);
				
				xPathToCheckAndroid.add(new XPathToCheck("/widget/platform[@name='android' and not(*)]", "/widget/engine[@name='$(CordovaPlatform)$']", true));
				
				xPathToCheckAndroid.add(new XPathToCheck("/widget/preference[@name='android-minSdkVersion']", "", true));
				xPathToCheckAndroid.add(new XPathToCheck("/widget/preference[@name='android-build-tool']", "", true));
				
				String[] androidResList = new String[] {"ldpi", "mdpi", "hdpi", "xhdpi"};
				for (String androidRes : androidResList) {
					xPathToCheckAndroid.add(new XPathToCheck("/widget/icon[@gap:platform='android' and @gap:qualifier='" + androidRes + "']", "/widget/platform[@name='android']/icon[@density='" + androidRes + "']", false));
				}				
				androidResList = new String[] {"ldpi", "mdpi", "hdpi", "xhdpi", "land-ldpi", "land-mdpi", "land-hdpi", "land-xhdpi"};
				for (String androidRes : androidResList) {
					xPathToCheckAndroid.add(new XPathToCheck("/widget/splash[@gap:platform='android' and @gap:qualifier='" + androidRes + "']", "/widget/platform[@name='android']/splash[@density='" + androidRes + "']", false));
				}
				
				// Tags to change only for iOS
				List<XPathToCheck> xPathToCheckIOs = new LinkedList<XPathToCheck>();
				xPathToCheckIOs.addAll(xPathToCheckDefault);

				xPathToCheckIOs.add(new XPathToCheck("/widget/platform[@name='ios']", "/widget/engine[@name='$(CordovaPlatform)$']", true));

				xPathToCheckIOs.add(new XPathToCheck("/widget/preference[@name='target-device' and @value='universal']", null, true));
				
				String[] iosResList = new String[] {"180", "120", "57", "72", "114", "144"};
				for (String iosRes : iosResList) {
					xPathToCheckIOs.add(new XPathToCheck("/widget/icon[@gap:platform='ios' and @width='" + iosRes + "']", "/widget/platform[@name='ios']/icon[@width='" + iosRes + "']", false));
				}
				iosResList = new String[] {"2208", "1334", "1136", "1496", "748", "2008", "1004", "960", "480"};
				for (String iosRes : iosResList) {
					xPathToCheckIOs.add(new XPathToCheck("/widget/splash[@gap:platform='ios' and @height='" + iosRes + "']", "/widget/platform[@name='ios']/splash[@height='" + iosRes + "']", false));
				}
				
				xPathToCheckIOs.add(new XPathToCheck("/widget/access", "", true));
				
				// Tags to change only for Windows Phone 8
				List<XPathToCheck> xPathToCheckWinPhone8 = new LinkedList<XPathToCheck>();
				xPathToCheckWinPhone8.addAll(xPathToCheckDefault);

				xPathToCheckWinPhone8.add(new XPathToCheck("/widget/platform[@name='winphone' and not(*)]", "/widget/engine[@name='$(CordovaPlatform)$']", true));
				
				xPathToCheckWinPhone8.add(new XPathToCheck("/widget/icon[@gap:platform='winphone' and not(@gap:role)]", "/widget/platform[@name='wp8']/icon[not(@role)]", false));
				xPathToCheckWinPhone8.add(new XPathToCheck("/widget/icon[@gap:platform='winphone' and @gap:role='background']", "/widget/platform[@name='wp8']/icon[@role='background']", false));
				
				xPathToCheckWinPhone8.add(new XPathToCheck("/widget/splash[@gap:platform='winphone']", "/widget/platform[@name='wp8']/splash[@src]", false));
				
				// Iterates on each platforms
				List<MobilePlatform> mobilePlatformList = mobileApplication.getMobilePlatformList();
				for (MobilePlatform mobilePlatform : mobilePlatformList) {
					
					// Get the config.xml file and load the xml
					File configFile = new File (mobilePlatform.getResourceFolder(), "config.xml");
					if (configFile.exists()) {
						Document oldDoc = XMLUtils.loadXml(configFile);
						
						// Gets the tags to change depending to the platform
						List<XPathToCheck> xPathToCheckList = null;
						String platformName = null;
						if (mobilePlatform instanceof Android) {
							xPathToCheckList = xPathToCheckAndroid;
							platformName = "Android";							
						} else if (mobilePlatform instanceof IOs) {
							xPathToCheckList = xPathToCheckIOs;
							platformName = "IOs";
						} else if (mobilePlatform instanceof WindowsPhone8) {
							xPathToCheckList = xPathToCheckWinPhone8;
							platformName = "WindowsPhone8";
						} else {
							continue;
						}
						
						// Gets the template config.xml file and load the xml
						String configTemplatePath = Engine.TEMPLATES_PATH + "/base/DisplayObjects/platforms/" + platformName + "/config.xml";
						File configTemplateFile = new File (configTemplatePath);
						if (!configTemplateFile.exists()) {
							throw new Exception("Can't find template config.xml at " + configTemplatePath);
						}
						Document templateDoc = XMLUtils.loadXml(configTemplateFile);						
						// String xmlStr = XMLUtils.prettyPrintDOM(templateDoc);
						
						for (XPathToCheck xPathToCheck : xPathToCheckList) {
							Element oldElement = (Element) xpathApi.selectSingleNode(oldDoc, xPathToCheck.oldXpath);
							
							// If the goal is to remove the old node
							if (xPathToCheck.templateXpath == null) {
								if (oldElement != null) {
									oldElement.getParentNode().removeChild(oldElement);
								}
								continue;
							}
							
							Element templateElement = (Element) xpathApi.selectSingleNode(templateDoc, xPathToCheck.templateXpath);
							
							// Can't do anything if the element is not found in the template file
							if (templateElement == null) {
								throw new Exception("XPath incorrect : " + xPathToCheck.templateXpath);
							}
							
							// Replace the old element by the template 
							if (oldElement != null || xPathToCheck.required) {
								
								// If the template is already in the old config.xml
								Element templateOldElement = (Element) xpathApi.selectSingleNode(oldDoc, xPathToCheck.templateXpath);
								if (templateOldElement != null && templateElement.isEqualNode(templateOldElement)) {
									continue;
								}
								
								String xPathTemplateParent = xPathToCheck.templateXpath.substring(0, xPathToCheck.templateXpath.lastIndexOf('/'));
								Node parentNode = createSameTree(oldDoc, templateElement.getParentNode(), xPathTemplateParent, xpathApi);
								Node newNode = oldDoc.adoptNode(templateElement.cloneNode(true));
								if (oldElement != null) {									
									if (parentNode.isSameNode(oldElement.getParentNode())) {
										parentNode.replaceChild(newNode, oldElement);
									} else {
										parentNode.appendChild(newNode);
										oldElement.getParentNode().removeChild(oldElement);
									}
								} else {
									parentNode.appendChild(newNode);
								}
							}							
						}
						
						for (int i = 0; i < pluginList[0].length; i++) {
							Node comment = xpathApi.selectNode(oldDoc, "//comment()[contains(.,\"" + pluginList[1][i] + "\")]");
							if (comment != null) {
								comment = xpathApi.selectNode(oldDoc, "//comment()[contains(.,\"" + pluginList[0][i] + "\")]");
							}
							if (comment != null) {
								Node newComment = xpathApi.selectNode(templateDoc, "//comment()[contains(.,\"" + pluginList[1][i] + "\")]");
								comment.getParentNode().replaceChild(oldDoc.adoptNode(newComment), comment);
							}
						}
						
						File oldConfigFile = new File(configFile.getParent(), "config.xml.old");
						if (!oldConfigFile.exists()) {
							oldConfigFile.createNewFile();
						}
						FileUtils.copyFile(configFile, oldConfigFile);
						File newConfigFile = new File(mobilePlatform.getResourceFolder(), "config.xml");
						if (!newConfigFile.exists()) {
							newConfigFile.createNewFile();
						}
						XMLUtils.saveXml(oldDoc, newConfigFile.getAbsolutePath());
					}					
				}		
			}
		}
		catch (Exception e) {
			Engine.logDatabaseObjectManager.error("[Migration 7.4.0] An error occured while migrating project \""+projectName+"\"",e);
		}
		
	}
	
	/**
	 * Create the same tree if it does not already exist.
	 * 
	 * @return
	 * @throws Exception 
	 */
	private static Node createSameTree(Document doc, Node externalNode, String xPath, TwsCachedXPathAPI xpathApi) throws Exception {
		
		if (xPath.indexOf('/') < 0 || externalNode == null) {
			// error root reached
		}
		
		Node internalNode = xpathApi.selectSingleNode(doc, xPath);
		
		if (internalNode != null) {
			return internalNode;
		} else {
			String xPathParent = xPath.substring(0, xPath.lastIndexOf('/'));
			Node externalNodeParent = externalNode.getParentNode();
			
			Node internalNodeParent = createSameTree(doc, externalNodeParent, xPathParent, xpathApi);
			
			internalNode =  doc.adoptNode(externalNode.cloneNode(false));
			internalNodeParent.appendChild(internalNode);
			
			return internalNode;
		}
	}

}

class XPathToCheck {
	
	public String templateXpath;
	public String oldXpath;
	public boolean required;
	
	/**
	 * 
	 * @param oldXpath
	 * @param templateXpath ("" same as oldXpath, null remove the node found with oldXpath)
	 * @param required
	 */
	public XPathToCheck(String oldXpath, String templateXpath, boolean required) {
		this.oldXpath = oldXpath;
		if (templateXpath != null && templateXpath.equals("")) {
			templateXpath = oldXpath;
		}
		this.templateXpath = templateXpath;
		this.required = required;
	}
	
	@Override
	public String toString() {
		return "(" + oldXpath + "; " + templateXpath + "; " + required + ")";
	}
	
}
