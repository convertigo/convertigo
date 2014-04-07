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

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;

public class MobilePlatformTreeObject extends DatabaseObjectTreeObject {

	public MobilePlatformTreeObject(Viewer viewer, MobilePlatform object) {
		super(viewer, object);
	}

	public MobilePlatformTreeObject(Viewer viewer, MobilePlatform object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public MobilePlatform getObject() {
		return (MobilePlatform) super.getObject();
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		return super.testAttribute(target, name, value);
	}

	@Override
	public void setParent(TreeParent parent) {
		super.setParent(parent);
		if (parent != null) {
			refreshResourceFolder();
		}
	}

	@Override
	public boolean rename(String newName, boolean bDialog) {
		boolean renamed = super.rename(newName, bDialog);
		if (renamed) {
			refreshResourceFolder();
		}
		return renamed;
	}
	
	private void refreshResourceFolder() {
		try {
			getProjectTreeObject().getIProject().getFolder(getObject().getRelativeResourcePath()).refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			ConvertigoPlugin.logWarning(e, "Failed to refresh the mobile platform folder in resource view");
		}
	}
}
