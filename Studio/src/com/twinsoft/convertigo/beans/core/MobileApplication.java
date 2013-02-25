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

import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.HttpUtils;

/**
 * This class manages a host application.
 */
public class MobileApplication extends DatabaseObject implements ITagsProperty {

	private static final long serialVersionUID = 5414379401296015511L;
	
	public enum FlashUpdateBuildMode {
		full,
		light;
		
		final static String[] buildModes = new String[] {
			full.name(),
			light.name()
		};
	}
	
	public enum PhoneGapFeatures {
		device,
		camera,
		contacts,
		file,
		geolocation,
		media,
		network,
		notification
	}
	
	private boolean enableFlashUpdate = true;
	
	private String buildMode = FlashUpdateBuildMode.full.name();
	
	private boolean requireUserConfirmation = false;
	
	private String applicationId = "";
	private String applicationName = "";
	private String applicationDescription = "";
	private String applicationAuthorName = "Convertigo";
	private String applicationAuthorEmail = "sales@convertigo.com";
	private String applicationAuthorSite = "http://www.convertigo.com";
	private boolean applicationFeatureDevice = false;
	private boolean applicationFeatureCamera = false;
	private boolean applicationFeatureContacts = false;
	private boolean applicationFeatureFile = false;
	private boolean applicationFeatureGeolocation = false;
	private boolean applicationFeatureMedia = false;
	private boolean applicationFeatureNetwork = false;
	private boolean applicationFeatureNotification= false;
	
	private String endpoint = "";

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

	public String getBuildMode() {
		return buildMode;
	}

	public void setBuildMode(String buildMode) {
		this.buildMode = buildMode;
	}
	
	public FlashUpdateBuildMode getBuildModeEnum() {
		return FlashUpdateBuildMode.valueOf(this.buildMode);
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
		clonedObject.vMobileDevices = new LinkedList<MobileDevice>();
		return clonedObject;
	}

	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.addAll(getMobileDeviceList());
		return rep;
	}

	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof MobileDevice) {
			addMobileDevice((MobileDevice) databaseObject);
		} else {
			throw new EngineException("You cannot add to a mobile application a database object of type " + databaseObject.getClass().getName());
		}
    }

    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof MobileDevice) {
			removeMobileDevice((MobileDevice) databaseObject);
		} else {
			throw new EngineException("You cannot remove from a mobile application a database object of type " + databaseObject.getClass().getName());
		}
		super.remove(databaseObject);
    }
	
	/**
	 * The list of available mobile device for this project.
	 */
	transient private List<MobileDevice> vMobileDevices = new LinkedList<MobileDevice>();

	protected void addMobileDevice(MobileDevice device) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vMobileDevices, device.getName(), device.bNew);
		device.setName(newDatabaseObjectName);
		vMobileDevices.add(device);
		super.add(device);
	}

	public void removeMobileDevice(MobileDevice device) throws EngineException {
		checkSubLoaded();
		vMobileDevices.remove(device);
	}

	public List<MobileDevice> getMobileDeviceList() {
		checkSubLoaded();
		return sort(vMobileDevices);
	}

	public MobileDevice getMobileDeviceByName(String deviceName) throws EngineException {
		checkSubLoaded();
		for (MobileDevice device : vMobileDevices)
			if (device.getName().equalsIgnoreCase(deviceName)) return device;
		throw new EngineException("There is no mobile device named \"" + deviceName + "\" found into this project.");
	}
	
	public String getComputedApplicationId() {
		String applicationId = this.applicationId;
		if ("".equals(applicationId)) {
			applicationId = "com.convertigo.mobile." + getProject().getName();
		}
		else {
			// The user can have setup an application ID that could be non valid:
			// application ID can only contains alpha numeric ASCII characters.
			applicationId = com.twinsoft.convertigo.engine.util.StringUtils.normalize(applicationId);
			applicationId = StringUtils.remove(applicationId, "_");
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

	public String[] getTagsForProperty(String propertyName) {
		if ("buildMode".equals(propertyName)) {
			return FlashUpdateBuildMode.buildModes;
		}
		return new String[0];
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

	public boolean isApplicationFeatureDevice() {
		return applicationFeatureDevice;
	}

	public void setApplicationFeatureDevice(boolean applicationFeatureDevice) {
		this.applicationFeatureDevice = applicationFeatureDevice;
	}

	public boolean isApplicationFeatureCamera() {
		return applicationFeatureCamera;
	}

	public void setApplicationFeatureCamera(boolean applicationFeatureCamera) {
		this.applicationFeatureCamera = applicationFeatureCamera;
	}

	public boolean isApplicationFeatureContacts() {
		return applicationFeatureContacts;
	}

	public void setApplicationFeatureContacts(boolean applicationFeatureContacts) {
		this.applicationFeatureContacts = applicationFeatureContacts;
	}

	public boolean isApplicationFeatureFile() {
		return applicationFeatureFile;
	}

	public void setApplicationFeatureFile(boolean applicationFeatureFile) {
		this.applicationFeatureFile = applicationFeatureFile;
	}

	public boolean isApplicationFeatureGeolocation() {
		return applicationFeatureGeolocation;
	}

	public void setApplicationFeatureGeolocation(boolean applicationFeatureGeolocation) {
		this.applicationFeatureGeolocation = applicationFeatureGeolocation;
	}

	public boolean isApplicationFeatureMedia() {
		return applicationFeatureMedia;
	}

	public void setApplicationFeatureMedia(boolean applicationFeatureMedia) {
		this.applicationFeatureMedia = applicationFeatureMedia;
	}

	public boolean isApplicationFeatureNetwork() {
		return applicationFeatureNetwork;
	}

	public void setApplicationFeatureNetwork(boolean applicationFeatureNetwork) {
		this.applicationFeatureNetwork = applicationFeatureNetwork;
	}

	public boolean isApplicationFeatureNotification() {
		return applicationFeatureNotification;
	}

	public void setApplicationFeatureNotification(boolean applicationFeatureNotification) {
		this.applicationFeatureNotification = applicationFeatureNotification;
	}
	
	public boolean isFeature(PhoneGapFeatures feature) {
		switch (feature) {
		case camera: return applicationFeatureCamera;
		case contacts: return applicationFeatureContacts;
		case device: return applicationFeatureDevice;
		case file: return applicationFeatureFile;
		case geolocation: return applicationFeatureGeolocation;
		case media: return applicationFeatureMedia;
		case network: return applicationFeatureNetwork;
		case notification: return applicationFeatureNotification;
		}
		return false;
	}
}