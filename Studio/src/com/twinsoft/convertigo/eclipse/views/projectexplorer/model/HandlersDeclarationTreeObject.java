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
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;

import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTransactionEditor;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTransactionEditorInput;
import com.twinsoft.convertigo.eclipse.editors.jscript.MyJScriptEditor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;

public class HandlersDeclarationTreeObject extends TreeObject implements IEditableTreeObject {
	public static final int TYPE_ROOT = 0;
	public static final int TYPE_FUNCTION_SCREEN_CLASS_ENTRY = 10;
	public static final int TYPE_FUNCTION_SCREEN_CLASS_EXIT  = 11;
	public static final int TYPE_OTHER = 100;
	
	public int type;
	public int lineNumber;
	
    public HandlersDeclarationTreeObject(Viewer viewer, Object object, int type, int lineNumber) {
        super(viewer, object);
        this.type = type;
        this.lineNumber = lineNumber;
    }

	public void launchEditor(String editorType) {
		// TODO Auto-generated method stub

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
		
		ProjectExplorerView explorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		
		TreeObject selectedHandler = explorerView.getFirstSelectedTreeObject();
		TreeObject object = selectedHandler; 

		while (!(object instanceof TransactionTreeObject)) {
			object = object.getParent();
		}
		
		Transaction transaction = (Transaction)object.getObject();
		
		String tempFileName = 	"_private/"+project.getName()+
								"__"+getConnectorTreeObject().getName()+
								"__"+object.getName();
		
		IFile file = project.getFile(tempFileName);

		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			try {
				activePage.openEditor(new JscriptTransactionEditorInput(file,transaction),
										"com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTransactionEditor");
				moveTo(selectedHandler.getName());
			} catch(PartInitException e) {
				ConvertigoPlugin.logException(e, "Error while loading the transaction editor '" + transaction.getName() + "'");
			} 
		}
	}
	
	public void moveTo(String handlerName) {
		
		IEditorPart editor =  PlatformUI
									.getWorkbench()
									.getActiveWorkbenchWindow()
									.getActivePage()
									.getActiveEditor();
		
		if (editor instanceof JscriptTransactionEditor) {
			JscriptTransactionEditor myEditor = (JscriptTransactionEditor) editor;
			MyJScriptEditor jsEditor = myEditor.getEditor();
			IDocumentProvider provider = jsEditor.getDocumentProvider();
			IDocument document = provider.getDocument(editor.getEditorInput());
			String content = document.get();
			int index = content.indexOf(handlerName);
			jsEditor.selectAndReveal(index, handlerName.length());
		}
	}
}
