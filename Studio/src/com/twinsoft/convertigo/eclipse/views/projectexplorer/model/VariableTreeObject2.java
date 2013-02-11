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

import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;

public class VariableTreeObject2 extends DatabaseObjectTreeObject implements IOrderableTreeObject {

	public boolean isChildOfJavelinTransaction = false;
	public boolean isMultiValued = false;
	
	public VariableTreeObject2(Viewer viewer, Variable object) {
		this(viewer, object, false);
	}
	
	public VariableTreeObject2(Viewer viewer, Variable object, boolean inherited) {
		super(viewer, object, inherited);
		isMultiValued = object.isMultiValued();
	}

	@Override
	public Variable getObject(){
		return (Variable) super.getObject();
	}
	
	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("isChildOfJavelinTransaction")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isChildOfJavelinTransaction));
		}
		if (name.equals("isMultiValued")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isMultiValued));
		}
		return super.testAttribute(target, name, value);
	}

	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);
	}
	
}
