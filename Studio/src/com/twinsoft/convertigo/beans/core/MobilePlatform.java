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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

/**
 * The MobileDevice class is the base class for all mobile devices.
 */
public abstract class MobilePlatform extends DatabaseObject {

	private static final long serialVersionUID = 8006681009945420375L;
	
	protected String cordovaPlatform;
	
	public MobilePlatform() {
        super();
		databaseType = "MobilePlatform";
	}
	
	@Override
	public MobilePlatform clone() throws CloneNotSupportedException {
		MobilePlatform clonedObject = (MobilePlatform) super.clone();
		return clonedObject;
	}
	
	@Override
	public void setName(String name) throws EngineException {
		String oldName = getName();
		
		super.setName(name);
		
		if (parent != null) {
			checkFolder(oldName);
		}
	}
	
	@Override
	public void setParent(DatabaseObject databaseObject) {
		super.setParent(databaseObject);
		checkFolder();
	}
	
	@Override
	public MobileApplication getParent() {
		return (MobileApplication) parent;
	}
	
	public String getRelativeResourcePath() {
		return "DisplayObjects/platforms/" + getName();
	}
	
	public File getResourceFolder() {
		return new File(getProject().getDirPath() + "/" + getRelativeResourcePath());
	}
	
	abstract public String getPackageType();
	
	
	private void checkFolder() {
		File folder = getResourceFolder();
		if (!folder.exists()) {
			try {
				File templateFolder = new File(Engine.TEMPLATES_PATH, "base/DisplayObjects/platforms/" + getClass().getSimpleName());
				FileUtils.copyDirectory(templateFolder, folder);
			} catch (IOException e) {
				Engine.logBeans.warn("(MobilePlatform) The folder '" + folder.getAbsolutePath() + "' doesn't exist and cannot be created", e);
			}
		}
	}
	
	private void checkFolder(String oldName) {
		if (oldName != null && !oldName.equals(getName())) {
			File oldFolder = new File(getProject().getDirPath() + "/DisplayObjects/platforms/" + oldName);
			if (oldFolder.exists()) {
				File newFolder = getResourceFolder();
				if (!newFolder.exists()) {
					oldFolder.renameTo(newFolder);
				} else {
					Engine.logBeans.warn("(MobilePlatform) The folder '" + oldFolder.getAbsolutePath() + "' cannot be moved to the existing '" + newFolder.getAbsolutePath() + "' folder");
				}
				return;
			}
		}
		checkFolder();
	}

	public String getType() {
		return getClass().getSimpleName();
	}
	
	public String getCordovaPlatform() {
		return this.cordovaPlatform;
	}
	
	public void setCordovaPlatform(String cordovaPlatform) {
		this.cordovaPlatform = cordovaPlatform;
	}
}