/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentTreeObject.FilterObject;
import com.twinsoft.convertigo.engine.ConvertigoException;

public class DesignDocumentFilterTreeObject extends DesignDocumentFunctionTreeObject {

	public DesignDocumentFilterTreeObject(Viewer viewer, Object object) {
		super(viewer, object);
	}

	@Override
	public TreeParent getTreeObjectOwner() {
		return getParent().getParent();
	}
	
	@Override
	public DesignDocumentTreeObject getParentDesignTreeObject() {
		return (DesignDocumentTreeObject) getParent().getParent();
	}
	
	@Override
	public FilterObject getObject() {
		return (FilterObject) super.getObject();
	}
	
	@Override
	public boolean rename(String newName, Boolean bDialog) {
		if (getName().equals(newName))
			return true;
		
		DesignDocumentTreeObject dto = getParentDesignTreeObject();
		if (dto.hasFilter(newName)) {
			ConvertigoPlugin.logException(new ConvertigoException("The function named \"" + newName + "\" already exists."), "Unable to change the object name.", bDialog);
			return false;
		}
		
		getObject().setName(newName);
		hasBeenModified();
		
        TreeViewer viewer = (TreeViewer) getAdapter(TreeViewer.class);
    	viewer.update(this, null);
		
		return true;
	}
}
