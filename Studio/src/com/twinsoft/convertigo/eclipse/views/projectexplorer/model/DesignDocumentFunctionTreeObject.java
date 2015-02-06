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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.core.Document;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTreeFunctionEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DesignDocumentTreeObject.FunctionObject;

public class DesignDocumentFunctionTreeObject extends TreeParent implements IEditableTreeObject, IFunctionTreeObject {

	public DesignDocumentFunctionTreeObject(Viewer viewer, Object object) {
		super(viewer, object);
	}

	public TreeParent getTreeObjectOwner() {
		return getParent().getParent().getParent();
	}
	
	@Override
	public FunctionObject getObject() {
		return (FunctionObject)super.getObject();
	}

	protected void hasBeenModified() {
		DesignDocumentViewTreeObject ddvto = (DesignDocumentViewTreeObject) getParent();
		if (ddvto != null) {
			ddvto.hasBeenModified();
		}
	}
	
	@Override
	public String getFunction() {
		String function = new String(getObject().getStringObject());
		return function;
	}

	@Override
	public void setFunction(String function) {
		getObject().setStringObject(function);
		hasBeenModified();
	}
	
	@Override
	public void launchEditor(String editorType) {
		// Retrieve the project name
		String projectName = getConnectorTreeObject().getObject().getProject().getName();	
	
		try {
			// Refresh project resource
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			
			// Open editor
			if ((editorType == null) || ((editorType != null) && (editorType.equals("JscriptHandlerEditor")))) {
				openJscriptHandlerEditor(project);
			}
		} 
		catch (CoreException e) {
			ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
		}
	}

	public void openJscriptHandlerEditor(IProject project) {
		TreeObject object = getTreeObjectOwner();
		
		Document document = (Document)object.getObject();
		
		String tempFileName = 	"_private/"+project.getName()+
				"__"+getConnectorTreeObject().getName()+
				"__"+document.getName()+
				"__views."+getParent().getName()+"."+getName();

		IFile file = project.getFile(tempFileName);
		
		IWorkbenchPage activePage = PlatformUI
								.getWorkbench()
								.getActiveWorkbenchWindow()
								.getActivePage();
		
		if (activePage != null) {
			try {
				activePage.openEditor(new JscriptTreeFunctionEditorInput(file,this),
										"com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTreeFunctionEditor");
			} catch(PartInitException e) {
				ConvertigoPlugin.logException(e, "Error while loading the document editor '" + document.getName() + "'");
			} 
		}
	}

}
