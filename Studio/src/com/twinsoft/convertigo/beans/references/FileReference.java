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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.beans.references;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.engine.Engine;

public abstract class FileReference extends Reference implements IFileReference {
	
	private static final long serialVersionUID = -1457688807905967050L;

	public String filepath = "";

	public String getFilepath() {
		return filepath;
	}

	public void setFilepath(String filepath) {
		this.filepath = filepath;
	}

	public File getFile() {
		return Engine.theApp.filePropertyManager.getFileFromProperty(getFilepath(), getProject().getName());
	}

	protected URL getFileUrl() throws MalformedURLException {
		File file = getFile();
		if (file != null && file.isFile() && file.exists()) {
			return file.toURI().toURL();
		}
		return null;
	}
	
	protected URL getReferenceUrl() throws MalformedURLException {
		return getFileUrl();
	}
}
