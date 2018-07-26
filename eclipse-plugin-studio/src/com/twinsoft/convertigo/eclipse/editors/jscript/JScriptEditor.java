/*
 * Copyright (c) 2001-2018 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.editors.jscript;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IJScriptContainer;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;

@SuppressWarnings("restriction")
public class JScriptEditor extends EditorPart implements IPropertyListener {
	private IEditorSite eSite;
	private JScriptEditorInput eInput;
	private ListenerList<IPropertyListener> listenerList;
	private IJScriptContainer jsContainer;
	private MyJScriptEditor jsEditor;
	
	public JScriptEditor() {
		super();
	}

	@Override
	public void dispose() {
		if (jsEditor != null) {
			jsEditor.removePropertyListener(this);
			jsEditor.dispose();
		}
		try {
			eInput.getFile().delete(true, null);
		} catch (CoreException e) {
		}
		super.dispose();
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
			jsContainer.setExpression(jsEditor.getDocumentProvider().getDocument(jsEditor.getEditorInput()).get());
		} catch (Exception e) {
			ConvertigoPlugin.logWarning("Error writing step jscript code '" + eInput.getName() + "' : "+e.getMessage());
		}
		
		// Refresh tree
		ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (projectExplorerView != null) {
			projectExplorerView.updateDatabaseObject(jsContainer.getDatabaseObject());
		}
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
		return jsEditor.isDirty();
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
		reload();
		jsEditor.doSave(null);
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
		try {			
			setSite(site);
			setInput(input);
			eSite = site;
			eInput = (JScriptEditorInput) input;
			jsContainer = eInput.getJScriptContainer();
			setPartName(jsContainer.getEditorName());
		} catch (Exception e) {
			throw new PartInitException("Unable to create JS editor", e);
		}
	}

	@Override
	public void addPropertyListener(IPropertyListener l) {
		if (listenerList == null) {
			listenerList = new ListenerList<IPropertyListener>();
		}
		listenerList.add(l);
	}

	@Override
	public void removePropertyListener(IPropertyListener l) {
		if (listenerList != null) {
			listenerList.remove(l);
		}
	}

	public void propertyChanged(Object source, int propId) {
		// When a property from the jsEditor Changes, walk the list all the listeners and notify them.
		Object listeners[] = listenerList.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			IPropertyListener listener = (IPropertyListener) listeners[i];
			listener.propertyChanged(this, propId);
		}
	}
	
	public void reload() {
		reload("");
	}
	
	public void reload(String toAppend) {
		IDocument doc = jsEditor.getDocumentProvider().getDocument(getEditorInput());
		doc.set(jsContainer.getExpression() + toAppend);
	}

	public MyJScriptEditor getEditor() {
		return jsEditor;
	}
	
	public DatabaseObject getDatabaseObject() {
		return jsContainer.getDatabaseObject();
	}
}
