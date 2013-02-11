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

import java.io.File;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IActionFilter;

import com.twinsoft.convertigo.engine.Engine;

public class UnloadedProjectTreeObject extends TreeObject implements IActionFilter {
	public boolean isLoadable = true;
	
	public UnloadedProjectTreeObject(Viewer viewer, String object) {
		super(viewer, object);
        String fileName = Engine.PROJECTS_PATH + "/" + object + "/" + object + ".xml";
		File file = new File(fileName);
		isLoadable = file.exists();
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("isLoadable")) {
			boolean bTest = (isLoadable == Boolean.valueOf(value).booleanValue());
			return bTest;
		}
		return super.testAttribute(target, name, value);
	}
}
