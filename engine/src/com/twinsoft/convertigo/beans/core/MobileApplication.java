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

package com.twinsoft.convertigo.beans.core;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.enums.DatabaseObjectTypes;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.HttpUtils;

/**
 * This class manages a host application.
 */
@DboCategoryInfo(
		getCategoryId = "MobileApplication",
		getCategoryName = "Mobile application",
		getIconClassCSS = "convertigo-action-newMobileApplication"
	)
public class MobileApplication extends DatabaseObject {

	private static final long serialVersionUID = 5414379401296015511L;
	
	private static final Pattern p_version = Pattern.compile("(\\d+)(\\.\\d+)?(\\.\\d+)?");
	
	private static XMLVector<XMLVector<String>> initialIcons() {
		XMLVector<XMLVector<String>> xmlv = new XMLVector<XMLVector<String>>();
		XMLVector<String> v = new XMLVector<String>();
		v.add("{\"src\": \"assets\\/icon_512x512.png\",\"sizes\": \"512x512\",\"type\": \"image\\/png\"}");
		xmlv.add(v);
		return xmlv;
	}
	
	public enum FlashUpdateBuildMode {
		full,
		light;
		
		final static String[] buildModes = new String[] {
			full.name(),
			light.name()
		};
	}
	
	public enum SplashRemoveMode {
		beforeUpdate("Before Flash Update"),
		afterUpdate("After Flash Update"),
		manual("Manual");
		
		private final String label;
		
		private SplashRemoveMode(String label) {
			this.label = label;
		}

		@Override
		public String toString() {
			return label;
		}
	}
	
	private boolean enableFlashUpdate = true;
	
	private FlashUpdateBuildMode buildMode = FlashUpdateBuildMode.full;

	private SplashRemoveMode splashRemoveMode = SplashRemoveMode.afterUpdate;
	
	private boolean requireUserConfirmation = false;
	private long flashUpdateTimeout = 30000;
	
	private String applicationId = "";
	private String applicationName = "";
	private String applicationDescription = "";
	private String applicationVersion = "";
	private String applicationAuthorName = "Convertigo";
	private String applicationAuthorEmail = "sales@convertigo.com";
	private String applicationAuthorSite = "https://www.convertigo.com";
	private String applicationBgColor = "#01ccfc";
	private String applicationThemeColor = "#01ccfc";
	private XMLVector<XMLVector<String>> applicationIcons = initialIcons();
	
	private Accessibility accessibility = Accessibility.Public;
	
	private String endpoint = "";
	
	private String authenticationToken = "";
	
	private String fsConnector = "";
	private String fsDesignDocument = "";
	
	public MobileApplication() {
		super();
		databaseType = DatabaseObjectTypes.MobileApplication.name();
	}

	public boolean getEnableFlashUpdate() {
		return enableFlashUpdate;
	}

	public void setEnableFlashUpdate(boolean enableFlashUpdate) {
		this.enableFlashUpdate = enableFlashUpdate;
	}

	public FlashUpdateBuildMode getBuildMode() {
		return buildMode;
	}

	public void setBuildMode(FlashUpdateBuildMode buildMode) {
		this.buildMode = buildMode;
	}

	public SplashRemoveMode getSplashRemoveMode() {
		return splashRemoveMode;
	}

	public void setSplashRemoveMode(SplashRemoveMode splashRemoveMode) {
		this.splashRemoveMode = splashRemoveMode;
	}

	public boolean getRequireUserConfirmation() {
		return requireUserConfirmation;
	}

	public void setRequireUserConfirmation(boolean requireUserConfirmation) {
		this.requireUserConfirmation = requireUserConfirmation;
	}

	public String getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	
	@Override
	public MobileApplication clone() throws CloneNotSupportedException {
		MobileApplication clonedObject = (MobileApplication) super.clone();
		clonedObject.vMobilePlatforms = new LinkedList<MobilePlatform>();
		clonedObject.applicationComponent = null;
		return clonedObject;
	}

	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.addAll(getMobilePlatformList());
		if (applicationComponent != null) rep.add(applicationComponent);
		return rep;
	}

	@Override
	public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof MobilePlatform) {
			addMobilePlatform((MobilePlatform) databaseObject);
		} else if (databaseObject instanceof IApplicationComponent) {
			addApplicationComponent(databaseObject);
		} else {
			throw new EngineException("You cannot add to a mobile application a database object of type " + databaseObject.getClass().getName());
		}
	}

	@Override
	public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof MobilePlatform) {
			removeMobilePlatform((MobilePlatform) databaseObject);
		} else if (databaseObject instanceof IApplicationComponent) {
			removeApplicationComponent(databaseObject);
		} else {
			throw new EngineException("You cannot remove from a mobile application a database object of type " + databaseObject.getClass().getName());
		}
		super.remove(databaseObject);
	}

	/**
	 * The list of available mobile platform for this project.
	 */
	transient private List<MobilePlatform> vMobilePlatforms = new LinkedList<MobilePlatform>();

	protected void addMobilePlatform(MobilePlatform mobilePlatform) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vMobilePlatforms, mobilePlatform.getName(), mobilePlatform.bNew);
		mobilePlatform.setName(newDatabaseObjectName);
		vMobilePlatforms.add(mobilePlatform);
		super.add(mobilePlatform);
	}

	public void removeMobilePlatform(MobilePlatform mobilePlatform) throws EngineException {
		checkSubLoaded();
		vMobilePlatforms.remove(mobilePlatform);
	}

	public List<MobilePlatform> getMobilePlatformList() {
		checkSubLoaded();
		return sort(vMobilePlatforms);
	}

	public MobilePlatform getMobilePlatformByName(String platformName) throws EngineException {
		checkSubLoaded();
		for (MobilePlatform mobilePlatform : vMobilePlatforms)
			if (mobilePlatform.getName().equalsIgnoreCase(platformName)) return mobilePlatform;
		throw new EngineException("There is no mobile platform named \"" + platformName + "\" found into this project.");
	}
	
	/*
	 * The application component
	 */
	private transient DatabaseObject applicationComponent = null;

	public IApplicationComponent getApplicationComponent() {
		checkSubLoaded();
		return (IApplicationComponent) applicationComponent;
	}

	public void addApplicationComponent(DatabaseObject applicationComponent) throws EngineException {
		checkSubLoaded();
		if (this.applicationComponent != null) {
			throw new EngineException("The mobile application \"" + getName() + "\" already contains an application component! Please delete it first.");
		}
		this.applicationComponent = applicationComponent;
		super.add(applicationComponent);
	}

	public void removeApplicationComponent(DatabaseObject applicationComponent) {
		checkSubLoaded();
		if (applicationComponent != null && applicationComponent.equals(this.applicationComponent)) {
			this.applicationComponent = null;
		}
	}

	public String getComputedApplicationId() {
		String applicationId = this.applicationId;
		if ("".equals(applicationId)) {
			applicationId = "com.convertigo.mobile." + getProject().getName();
		}
		else {
			// The user can have setup an application ID that could be non valid:
			// application ID can only contains alpha numeric ASCII characters.
			String[] applicationIdParts = applicationId.split("\\.");
			for (int i = 0; i < applicationIdParts.length; i++) {
				applicationIdParts[i] = com.twinsoft.convertigo.engine.util.StringUtils.normalize(applicationIdParts[i]);
				applicationIdParts[i] = StringUtils.remove(applicationIdParts[i], "_");
			}
			applicationId = com.twinsoft.convertigo.engine.util.StringUtils.join(applicationIdParts, ".");
		}
		return applicationId;
	}
	
	public String getComputedEndpoint() {
		String endpoint = this.endpoint.replaceAll("/+$", "");
		if (StringUtils.isBlank(endpoint)) {
			endpoint = getDefaultServerEnpoint();
		}
		return endpoint;
	}
	
	public String getComputedEndpoint(HttpServletRequest request) {
		String endpoint = this.endpoint.replaceAll("/+$", "");
		if (StringUtils.isBlank(endpoint) && request != null) {
			endpoint = HttpUtils.convertigoRequestURL(request);
		}
		if (StringUtils.isBlank(endpoint)) {
			endpoint = getDefaultServerEnpoint();
		}
		return endpoint;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
	
	public String getComputedApplicationName() {
		return applicationName.length() > 0 ? applicationName : getProject().getName();
	}
	
	public String getApplicationDescription() {
		return applicationDescription;
	}

	public void setApplicationDescription(String applicationDescription) {
		this.applicationDescription = applicationDescription;
	}

	public String getApplicationAuthorName() {
		return applicationAuthorName;
	}

	public void setApplicationAuthorName(String applicationAuthorName) {
		this.applicationAuthorName = applicationAuthorName;
	}

	public String getApplicationAuthorEmail() {
		return applicationAuthorEmail;
	}

	public void setApplicationAuthorEmail(String applicationAuthorEmail) {
		this.applicationAuthorEmail = applicationAuthorEmail;
	}

	public String getApplicationAuthorSite() {
		return applicationAuthorSite;
	}

	public void setApplicationAuthorSite(String applicationAuthorSite) {
		this.applicationAuthorSite = applicationAuthorSite;
	}

	public long getFlashUpdateTimeout() {
		return flashUpdateTimeout;
	}

	public void setFlashUpdateTimeout(long flashUpdateTimeout) {
		this.flashUpdateTimeout = flashUpdateTimeout;
	}

	public Accessibility getAccessibility() {
		return accessibility;
	}

	public void setAccessibility(Accessibility accessibility) {
		this.accessibility = accessibility;
	}
	
	public String getRelativeResourcePath() {
		return "DisplayObjects/mobile";
	}
	
	public File getResourceFolder() {
		return new File(getProject().getDirPath() + "/" + getRelativeResourcePath());
	}
	
	@Override
	public void setParent(DatabaseObject databaseObject) {
		super.setParent(databaseObject);
		if (isOriginal()) {
			checkFolder();
		}
	}
	
	@Override
	public boolean isMaskedProperty(Visibility target, String propertyName) {
		if ("authenticationToken".equals(propertyName)) {
			return true;
		}
		return super.isMaskedProperty(target, propertyName);
	}
	
	private void checkFolder() {
		File folder = getResourceFolder();
		if (!folder.exists()) {
			try {
				folder.mkdirs();
			} catch (Exception e) {
				Engine.logBeans.warn("(MobileApplication) The folder '" + folder.getAbsolutePath() + "' doesn't exist and cannot be created", e);
			}
		}
	}
	
	public String getApplicationVersion() {
		return applicationVersion;
	}

	public void setApplicationVersion(String applicationVersion) {
		this.applicationVersion = applicationVersion;
	}
	
	public String getComputedApplicationVersion() {
		String version = applicationVersion.length() > 0 ? applicationVersion : getProject().getVersion();
		Matcher matcher = p_version.matcher(version);
		if (matcher.find()) {
			version = matcher.group(0);
			if (matcher.group(2) == null) {
				version += ".0.0";
			} else if (matcher.group(3) == null) {
				version += ".0";
			}
		} else {
			version = "0.0.1";
		}
		
		return version;
	}

	public String getAuthenticationToken() {
		return authenticationToken;
	}

	public void setAuthenticationToken(String authenticationToken) {
		this.authenticationToken = authenticationToken;
	}

	public String getComputedAuthenticationToken() {
		return authenticationToken.length() == 0 ?
				EnginePropertiesManager.getProperty(PropertyName.MOBILE_BUILDER_AUTHENTICATION_TOKEN) :
				authenticationToken;
	}

	public String getFsConnector() {
		return fsConnector;
	}

	public void setFsConnector(String fsConnector) {
		this.fsConnector = fsConnector;
	}
	
	public String getFsDefaultDatabase() {
		return getFsConnector().replaceFirst(".*?\\.", ""); // remote project.
	}

	public String getFsDesignDocument() {
		return fsDesignDocument;
	}

	public void setFsDesignDocument(String fsDesignDocument) {
		this.fsDesignDocument = fsDesignDocument;
	}
	
	public String getFsDefaultDesignDocument() {
		return getFsDesignDocument().replaceFirst(".*?\\..*?\\.", ""); // remote project.connector.
	}

	@Override
	public boolean testAttribute(String name, String value) {
		if ("isApplicationProject".equals(name)) {
			return getApplicationComponent() != null;
		}
		return super.testAttribute(name, value);
	}

	public XMLVector<XMLVector<String>> getApplicationIcons() {
		return applicationIcons;
	}

	public void setApplicationIcons(XMLVector<XMLVector<String>> applicationIcons) {
		this.applicationIcons = applicationIcons;
	}

	public String getApplicationBgColor() {
		return applicationBgColor;
	}

	public void setApplicationBgColor(String applicationBgColor) {
		this.applicationBgColor = applicationBgColor;
	}

	public String getApplicationThemeColor() {
		return applicationThemeColor;
	}

	public void setApplicationThemeColor(String applicationThemeColor) {
		this.applicationThemeColor = applicationThemeColor;
	}
	
	public static String getDefaultServerEnpoint() {
		var endPointUrl = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_ENDPOINT);
		if (StringUtils.isBlank(endPointUrl)) {
			endPointUrl = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
		}
		return endPointUrl;
	}
}