/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.sequence.SequenceEditorInput;

public class SequenceTreeObject extends DatabaseObjectTreeObject implements IClosableTreeObject {

	public SequenceTreeObject(Viewer viewer, Sequence object) {
		this(viewer, object, false);
	}
	
	public SequenceTreeObject(Viewer viewer, Sequence object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public Sequence getObject(){
		return (Sequence) super.getObject();
	}
	
	public void launchEditor() {
		// Retrieve the project name
		String projectName = getObject().getProject().getName();
		try {
			// Refresh project resource
			ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			
			// Open editor
			openSequenceEditor();

		} catch (CoreException e) {
			ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
		}
	}
	
	public void openSequenceEditor() {
		Sequence sequence = getObject();
		synchronized (sequence) {
			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (activePage != null) {
				IEditorPart editorPart = getSequenceEditor(activePage, sequence);
				
				if (editorPart != null) {
					activePage.activate(editorPart);
				}
				else {
					try {
						editorPart = activePage.openEditor(new SequenceEditorInput(sequence),
										"com.twinsoft.convertigo.eclipse.editors.sequence.SequenceEditor");
					} catch (PartInitException e) {
						ConvertigoPlugin.logException(e,
								"Error while loading the sequence editor '"
										+ sequence.getName() + "'");
					}
				}
			}
		}
	}
	
	private IEditorPart getSequenceEditor(IWorkbenchPage activePage, Sequence sequence) {
		IEditorPart editorPart = null;
		if (activePage != null) {
			if (sequence != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i=0;i<editorRefs.length;i++) {
					IEditorReference editorRef = (IEditorReference)editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if ((editorInput != null) && (editorInput instanceof SequenceEditorInput)) {
							if (((SequenceEditorInput)editorInput).is(sequence)) {
								editorPart = editorRef.getEditor(false);
								break;
							}
						}
					} catch(PartInitException e) {
						//ConvertigoPlugin.logException(e, "Error while retrieving the sequence editor '" + editorRef.getName() + "'");
					}
				}
			}
		}
		return editorPart;
	}
	
	@Override
	public void closeAllEditors(boolean save) {
		closeAllJsEditors(getObject(), save);
	}
}