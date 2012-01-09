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

package com.twinsoft.convertigo.eclipse.editors.jscript;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

import com.twinsoft.convertigo.beans.steps.SimpleStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.engine.Engine;

public class JscriptStepEditor extends EditorPart implements IPropertyListener {
	private IFile file;
	private IEditorSite eSite;
	private IEditorInput eInput;
	private ListenerList listenerList;
	private SimpleStep step;
	private MyJScriptEditor jsEditor;
	
	public JscriptStepEditor() {
		super();
	}

	public void dispose() {
		jsEditor.removePropertyListener(this);
		jsEditor.dispose();
		super.dispose();

		// When the editor is closed, delete the temporary file created when we opened the editor
		if (file.exists()) {
			try {
				file.delete(true, null);
			} catch (CoreException e) {
				//ConvertigoPlugin.logWarning(e,"Error while deleting temporary file", Boolean.FALSE);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
		jsEditor.doSave(monitor);
		try {
			// Get the jsEditor content and transfer it to the step object
			InputStream is = file.getContents();
			byte[] array = new byte[is.available()];
			is.read(array);
			step.setExpression(new String (array));
			step.write();
			Engine.theApp.databaseObjectsManager.cacheUpdateObject(step);
		} catch (Exception e) {
			ConvertigoPlugin.logWarning("Error writing step jscript code '" + eInput.getName() + "' : "+e.getMessage());
		}
		
		// Refresh tree
		ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (projectExplorerView != null)
			projectExplorerView.updateDatabaseObject(step);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	public void doSaveAs() {
		jsEditor.doSaveAs();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
	public boolean isDirty() {
		return (jsEditor.isDirty());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 */
	public void createPartControl(Composite parent) {
		jsEditor = new MyJScriptEditor();
		jsEditor.setSourceViewerConfiguration();
		jsEditor.addPropertyListener(this);
		try {
			jsEditor.init(eSite, eInput);
		} catch (PartInitException e) {
			ConvertigoPlugin.logException(e, "Error inialiazing  Javascript editor'" + eInput.getName() + "'");
		}
		jsEditor.createPartControl(parent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		jsEditor.setFocus();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite,
	 *      org.eclipse.ui.IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		// Get from the input the necessary objects as the temp IFile to create to hold the jscript code itself.
		file = ((FileEditorInput) input).getFile();
		step = (SimpleStep)((JscriptStepEditorInput)input).getStep();
		
		// Create a temp  file to hold step jscript code
		InputStream sbisHandlersStream = new ByteArrayInputStream(step.getExpression().getBytes());
		
		// Overrides temp file with step jscript code
		if (file.exists()) {
			try {
				file.setContents(sbisHandlersStream, true, false, null);
			} catch (CoreException e) {
				ConvertigoPlugin.logException(e, "Error while editing step jscript code");
			}
		}
		// Create a temp file to hold step jscript code
		else {
			try {
				file.create(sbisHandlersStream, true, null);
			} catch (CoreException e) {
				ConvertigoPlugin.logException(e, "Error while editing the step jscript code");
			}
		}
		
		setSite(site);
		setInput(input);
		eSite = site;
		eInput = input;
		String[] splits = file.getName().split(" ");
		setPartName(splits[splits.length-1]);
	}

	public void addPropertyListener(IPropertyListener l) {
		if (listenerList == null)
			listenerList = new ListenerList();
		listenerList.add(l);
	}

	public void removePropertyListener(IPropertyListener l) {
		if (listenerList != null) {
			listenerList.remove(l);
		}
	}

	public void propertyChanged(Object source, int propId) {
		ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		
		// When a property from the jsEditor Changes, walk the list all the listeners and notify them.
		Object listeners[] = listenerList.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			IPropertyListener listener = (IPropertyListener) listeners[i];
			listener.propertyChanged(this, propId);
		}
		if (IEditorPart.PROP_DIRTY == propId) {
			if (jsEditor.isDirty()) {
				step.hasChanged = true;
				if (projectExplorerView != null)
					projectExplorerView.updateDatabaseObject(step);
			}
		}
	}
	
	public void reload() {
		reload("");
	}
	
	public void reload(String toAppend) {
		IDocument doc = jsEditor.getDocumentProvider().getDocument(getEditorInput());
		doc.set(step.getExpression() + toAppend);
	}
}
