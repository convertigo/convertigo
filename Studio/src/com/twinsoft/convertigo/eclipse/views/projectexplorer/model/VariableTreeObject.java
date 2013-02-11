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

import java.util.Vector;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IActionFilter;

import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.transactions.JavelinTransaction;

public class VariableTreeObject extends TreeObject implements IActionFilter {
	public boolean isChildOfJavelinTransaction = false;
	
    public VariableTreeObject(Viewer viewer, RequestableObject requestable, Vector<String> variable) {
    	super(viewer, null);

		isChildOfJavelinTransaction = requestable instanceof JavelinTransaction;
		
        String variableName = (String) variable.elementAt(0);
        String variableDefaultValue = (String) variable.elementAt(2);
		
		if ((variableDefaultValue == null) || (variableDefaultValue.length() == 0)) {
			super.setObject(variableName);
		}
		else {
			super.setObject(variableName + " (" + variableDefaultValue + ")");
		}
    }
    
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("isChildOfJavelinTransaction")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isChildOfJavelinTransaction));
		}
		return false;
	}
}
