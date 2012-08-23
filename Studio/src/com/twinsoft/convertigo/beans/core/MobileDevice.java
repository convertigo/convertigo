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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.core;

/**
 * The MobileDevice class is the base class for all mobile devices.
 */
public abstract class MobileDevice extends DatabaseObject {

	private static final long serialVersionUID = 8006681009945420375L;
	public static String RESOURCES_PATH = "DisplayObjects/mobile";
	
	public MobileDevice() {
        super();
		databaseType = "MobileDevice";
	}
	
	@Override
	public MobileDevice clone() throws CloneNotSupportedException {
		MobileDevice clonedObject = (MobileDevice) super.clone();
		return clonedObject;
	}
	
	private long screenWidth = 0;
	
	public long getScreenWidth() {
		return screenWidth;
	}

	public void setScreenWidth(long screenWidth) {
		this.screenWidth = screenWidth;
	}

	private long screenHeight = 0;
	
	public long getScreenHeight() {
		return screenHeight;
	}

	public void setScreenHeight(long screenHeight) {
		this.screenHeight = screenHeight;
	}
	
	private String resourcesPath = "";
	
	public String getResourcesPath() {
		return resourcesPath;
	}

	public void setResourcesPath(String resourcesPath) {
		this.resourcesPath = resourcesPath;
	}
}