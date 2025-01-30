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

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Listener;

public class ListenerTreeObject extends DatabaseObjectTreeObject {

	public ListenerTreeObject(Viewer viewer, DatabaseObject object, boolean inherited) {
		super(viewer, object, inherited);
		setEnabled(getObject().isEnabled());
	}

	@Override
	public Listener getObject() {
		return (Listener)super.getObject();
	}
	
	@Override
    public boolean isEnabled() {
		setEnabled(getObject().isEnabled());
    	return super.isEnabled();
    }
	
	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (getObject().testAttribute(name, value)) {
			return true;
		}
		return super.testAttribute(target, name, value);
	}
}
