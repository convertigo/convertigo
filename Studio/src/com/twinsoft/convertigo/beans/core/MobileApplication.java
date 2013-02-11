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
public class MobileApplication extends DatabaseObject {

	private static final long serialVersionUID = 5414379401296015511L;

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

	public int getBuildMode() {
		return buildMode;
	}

	public void setBuildMode(int buildMode) {
		this.buildMode = buildMode;
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

	private boolean enableFlashUpdate = true; 

	public static final String[] FLASHUPDATE_BUILD_MODES = { "full", "light" };
	
	public static final int FLASHUPDATE_BUILD_MODE_FULL = 0;
	public static final int FLASHUPDATE_BUILD_MODE_LIGHT = 1;
	
	private int buildMode = FLASHUPDATE_BUILD_MODE_FULL;
	
	private boolean requireUserConfirmation = false;
	
	private String applicationId = "";
	
	private String endpoint = "";
	
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
}