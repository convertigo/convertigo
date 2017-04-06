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

import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.core.IScreenClassContainer;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.ConvertigoException;

public class ScreenClassTreeObject extends DatabaseObjectTreeObject {

	public ScreenClassTreeObject(Viewer viewer, ScreenClass object) {
		this(viewer, object, false);
	}

	public ScreenClassTreeObject(Viewer viewer, ScreenClass object, boolean inherited) {
		super(viewer, object, inherited);
	}
	
	public ScreenClass getObject(){
		return (ScreenClass) super.getObject();
	}
	
	public boolean rename(String newName, Boolean bDialog) {
		ScreenClass sc = getObject();
		IScreenClassContainer<?> scc = (IScreenClassContainer<?>)sc.getConnector();
		if(!sc.getName().equalsIgnoreCase(newName) && scc.getScreenClassByName(newName)!=null){
			ConvertigoPlugin.logException(new ConvertigoException("The name \"" + newName + "\" is already used by another screen class."), "Unable to change the object name.", bDialog);
            return false;
		}
		return super.rename(newName, bDialog);
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.views.projectexplorer.DatabaseObjectTreeObject#testAttribute(java.lang.Object, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (getObject().testAttribute(name, value)) {
			return true;
		}
		return super.testAttribute(target, name, value);
	}

}
