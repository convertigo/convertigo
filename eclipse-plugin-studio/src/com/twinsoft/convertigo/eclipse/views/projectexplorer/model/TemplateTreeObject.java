/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

public class TemplateTreeObject extends TreeObject implements IEditableTreeObject {
	private String xslPath;

	public TemplateTreeObject(Viewer viewer, Object object, String path) {
		super(viewer, object);
		xslPath = path;
	}

	public void launchEditor(String editorType) {
		// Retrieve the workspace project
		try {
			String projectName = ((DatabaseObject) parent.getObject()).getProject().getName();
			
			// Refresh project resource
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);

			// Gets the xsl file to be edited
			IFile file = project.getFile(new Path(xslPath));

			// Launch the XslStyleSheet Editor
			PlatformUI
					.getWorkbench()
					.getActiveWorkbenchWindow()
					.getActivePage()
					.openEditor(new FileEditorInput(file),
							"org.eclipse.wst.xml.ui.internal.tabletree.XMLMultiPageEditorPart");
		} catch (Exception pei) {
        	ConvertigoPlugin.logException(pei, "Unexpceted exception");
		}
	}
	
	@Override
	public void closeAllEditors(boolean save) {
		//TODO: closeAllJsEditors(getObject(), save);
	}
}
