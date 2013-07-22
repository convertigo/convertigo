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

package com.twinsoft.convertigo.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.browser.DreamfaceEditorInput;

public class DreamfaceAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void run(IAction action) {
		IWorkbenchPage activePage = window.getActivePage();
		if (activePage != null) {
			try {
				IEditorPart editorPart = null;
				
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i=0;i<editorRefs.length;i++) {
					IEditorReference editorRef = (IEditorReference)editorRefs[i];
					IEditorInput editorInput = editorRef.getEditorInput();
					if ((editorInput != null) && (editorInput instanceof DreamfaceEditorInput)) {
						editorPart = editorRef.getEditor(false);
						activePage.activate(editorPart);
						break;
					}
				}
				if (editorPart == null) {
					editorPart = activePage.openEditor(new DreamfaceEditorInput(),
										"com.twinsoft.convertigo.eclipse.editors.browser.DreamfaceEditor");
				}
				
			} catch (PartInitException e) {
				ConvertigoPlugin.logException(e,
						"Error while loading the DreamFace editor");
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {

	}

}
