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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/beans/core/Pool.java $
 * $Author: julienda $
 * $Revision: 32343 $
 * $Date: 2012-10-30 15:09:35 +0100 (mar., 30 oct. 2012) $
 */

package com.twinsoft.convertigo.beans.core;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.HttpUtils;

/**
 * This class manages a host application.
 */
public class MobileApplication extends DatabaseObject {

	private static final long serialVersionUID = 5414379401296015511L;
	
	private static final Pattern p_version = Pattern.compile("(\\d+)(\\.\\d+)?(\\.\\d+)?");
	
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
	private long flashUpdateTimeout = 5000;
	
	private String applicationId = "";
	private String applicationName = "";
	private String applicationDescription = "";
	private String applicationVersion = "";
	private String applicationAuthorName = "Convertigo";
	private String applicationAuthorEmail = "sales@convertigo.com";
	private String applicationAuthorSite = "http://www.convertigo.com";
	private Accessibility accessibility = Accessibility.Public;
	
	private String endpoint = "";
	
	private String authenticationToken = "";
	
	private String fsConnector = "";
	private String fsDesignDocument = "";
	
	public MobileApplication() {
        super();
        databaseType = "MobileApplication";
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
		return clonedObject;
	}

	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.addAll(getMobilePlatformList());
		return rep;
	}

	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof MobilePlatform) {
			addMobilePlatform((MobilePlatform) databaseObject);
		} else {
			throw new EngineException("You cannot add to a mobile application a database object of type " + databaseObject.getClass().getName());
		}
    }

    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof MobilePlatform) {
			removeMobilePlatform((MobilePlatform) databaseObject);
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
	
	public String getComputedEndpoint(HttpServletRequest request) {
		String endpoint = this.endpoint;
		if ("".equals(endpoint)) {
			endpoint = HttpUtils.convertigoRequestURL(request);
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
		checkFolder();
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
				File templateFolder = new File(Engine.TEMPLATES_PATH, "base/DisplayObjects/mobile");
				FileUtils.copyDirectory(templateFolder, folder);
			} catch (IOException e) {
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

	public String getFsDesignDocument() {
		return fsDesignDocument;
	}

	public void setFsDesignDocument(String fsDesignDocument) {
		this.fsDesignDocument = fsDesignDocument;
	}
}