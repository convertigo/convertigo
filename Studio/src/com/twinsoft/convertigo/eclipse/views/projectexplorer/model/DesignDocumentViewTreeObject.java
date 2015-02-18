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

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import com.google.gson.JsonObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentTreeObject.FunctionObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentTreeObject.ViewObject;
import com.twinsoft.convertigo.engine.ConvertigoException;

public class DesignDocumentViewTreeObject extends TreeParent {

	public DesignDocumentViewTreeObject(Viewer viewer, Object object) {
		super(viewer, object);
		loadFunctions();
	}

	@Override
	public ViewObject getObject() {
		return (ViewObject)super.getObject();
	}

	public TreeParent getTreeObjectOwner() {
		return getParent().getParent();
	}
	
	public boolean rename(String newName, Boolean bDialog) {
		if (getName().equals(newName))
			return true;
		if (getDesignDocumentTreeObject().hasView(newName)) {
			ConvertigoPlugin.logException(new ConvertigoException("The document named \"" + newName + "\" already exists."), "Unable to change the object name.", bDialog);
			return false;
		}
		
		getObject().setName(newName);
		hasBeenModified();
		
        TreeViewer viewer = (TreeViewer) getAdapter(TreeViewer.class);
    	viewer.update(this, null);
		
		return true;
	}
	
	private void loadFunctions() {
		ViewObject view = getObject();
		if (view.hasMap())
			addChild(new DesignDocumentFunctionTreeObject(viewer, view.getMap()));
		if (view.hasReduce())
			addChild(new DesignDocumentFunctionTreeObject(viewer, view.getReduce()));
	}

	private DesignDocumentTreeObject getDesignDocumentTreeObject() {
		return (DesignDocumentTreeObject) getTreeObjectOwner();
	}
	
	protected void hasBeenModified() {
		JsonObject functions = new JsonObject();
		for (TreeObject to : getChildren()) {
			DesignDocumentFunctionTreeObject ddfto = (DesignDocumentFunctionTreeObject)to;
			FunctionObject fo = ddfto.getObject();
			functions.addProperty(fo.name, fo.getStringObject());
		}
		
		getObject().setJsonObject(functions);
		
		DesignDocumentTreeObject ddto = getDesignDocumentTreeObject();
		if (ddto != null) {
			ddto.hasBeenModified();
		}
	}
}
