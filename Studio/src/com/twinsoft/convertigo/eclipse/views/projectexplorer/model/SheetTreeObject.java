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
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.xsl.XslFileEditorInput;

public class SheetTreeObject extends DatabaseObjectTreeObject implements IEditableTreeObject {

	public SheetTreeObject(Viewer viewer, Sheet object) {
		this(viewer, object, false);
	}

	public SheetTreeObject(Viewer viewer, Sheet object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public Sheet getObject(){
		return (Sheet) super.getObject();
	}
	
	public void launchEditor(String editorType) {
		// Retrieve the project name
		String projectName = getObject().getProject().getName();
		try {
			// Refresh project resource
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);

			// Open editor
			openXslEditor(project);
			
		} catch (CoreException e) {
			ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
		}
	}
	
	public void openXslEditor(IProject project) {
		Sheet sheet = getObject();
		String projectName = sheet.getProject().getName();
		String parentStyleSheet = sheet.getUrl();
		Path filePath = new Path(sheet.getUrl());
		IFile file = project.getFile(filePath);
		
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			try {
				activePage.openEditor(new XslFileEditorInput(file, projectName, sheet),
						"com.twinsoft.convertigo.eclipse.editors.xsl.XslRuleEditor");
			}
			catch(PartInitException e) {
				ConvertigoPlugin.logException(e, "Error while loading the xsl editor '" + parentStyleSheet + "'");
			}
		}
	}
}
