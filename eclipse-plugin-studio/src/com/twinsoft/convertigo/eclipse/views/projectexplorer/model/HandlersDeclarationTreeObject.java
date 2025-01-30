/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.jscript.JScriptEditorInput;

public class HandlersDeclarationTreeObject extends TreeObject implements IEditableTreeObject {
	public static final int TYPE_FUNCTION_SCREEN_CLASS_ENTRY = 10;
	public static final int TYPE_FUNCTION_SCREEN_CLASS_EXIT  = 11;
	public static final int TYPE_OTHER = 100;
	
	public HandlersDeclarationTreeObject(Viewer viewer, Object object) {
        super(viewer, object);
    }

	public void launchEditor(String editorType) {
		TreeObject object = this;
		try {
			while (!(object instanceof TransactionTreeObject)) {
				object = object.getParent();
			}
			TransactionTreeObject tto = (TransactionTreeObject) object;
			JScriptEditorInput.openJScriptEditor(tto, tto.getObject());
			moveTo(getName());
		} catch (PartInitException e) {
			ConvertigoPlugin.logException(e, "Error while loading the transaction editor '" + object.getName() + "'");
		}
	}
	
	private void moveTo(String handlerName) {
		IEditorPart editor =  PlatformUI
									.getWorkbench()
									.getActiveWorkbenchWindow()
									.getActivePage()
									.getActiveEditor();
		if (editor instanceof ITextEditor) {
			ITextEditor myEditor = (ITextEditor) editor;
			IDocumentProvider provider = myEditor.getDocumentProvider();
			IDocument document = provider.getDocument(editor.getEditorInput());
			String content = document.get();
			int index = content.indexOf(handlerName);
			myEditor.selectAndReveal(index, handlerName.length());
		}
	}

	@Override
	public void closeAllEditors(boolean save) {
		closeAllJsEditors((Transaction) getObject(), save);
	}
}
