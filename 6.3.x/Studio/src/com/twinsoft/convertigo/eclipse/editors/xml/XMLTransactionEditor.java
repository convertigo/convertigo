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

package com.twinsoft.convertigo.eclipse.editors.xml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wst.xml.ui.internal.tabletree.XMLMultiPageEditorPart;

import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;

public class XMLTransactionEditor extends EditorPart implements IPropertyListener {
	private IFile file;
	private IEditorSite eSite;
	private IEditorInput eInput;
	private ListenerList listenerList;
	private Transaction transaction;
	private XMLMultiPageEditorPart xmlEditor;
	
	public XMLTransactionEditor() {
		super();
	}

	@Override
	public void dispose() {
		xmlEditor.removePropertyListener(this);
		xmlEditor.dispose();
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
		xmlEditor.doSave(monitor);
		try {
			// Get the xmlEditor content and transfer it to the transaction object
			InputStream is = file.getContents();
			byte[] array = new byte[is.available()];
			is.read(array);
			transaction.wsdlType = new String(array, "ISO-8859-1");
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while writing transaction wsdl type.'" + eInput.getName() + "'");
		}

		transaction.hasChanged = true;
		// Refresh tree
		ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (projectExplorerView != null)
			projectExplorerView.updateDatabaseObject(transaction);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	public void doSaveAs() {
		xmlEditor.doSaveAs();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
	public boolean isDirty() {
		return (xmlEditor.isDirty());
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
		xmlEditor = new XMLMultiPageEditorPart();
		xmlEditor.addPropertyListener(this);
		try {
			xmlEditor.init(eSite, eInput);
			xmlEditor.setInitializationData(getConfigurationElement(), null, null);
		} catch (PartInitException e) {
			ConvertigoPlugin.logException(e, "Error inialiazing XML editor '" + eInput.getName() + "'");
		}
		xmlEditor.createPartControl(parent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		xmlEditor.setFocus();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite,
	 *      org.eclipse.ui.IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		// Get from the input the necessary objects as the temp IFile to create to hold the Wsdl data and the Transaction object itself.
		file = ((FileEditorInput) input).getFile();
		transaction = (Transaction)((XMLTransactionEditorInput)input).getTransaction();
		
		// Retrieve Wsdl type
		InputStream sbisHandlersStream = null;
		try {
			sbisHandlersStream = new ByteArrayInputStream(transaction.wsdlType.getBytes("ISO-8859-1"));
		} catch (UnsupportedEncodingException e) {
			ConvertigoPlugin.logException(e, "Error in the transaction wsdl type encoding");
		}

		// Overrides temp file with transaction's wsdl data
		if (file.exists()) {
			try {
				file.setCharset("ISO-8859-1", null);
				file.setContents(sbisHandlersStream, true, false, null);
			} catch (CoreException e) {
				ConvertigoPlugin.logException(e, "Error while editing the transaction wsdl type");
			}
		}
		// Create a temp file to hold transaction's wsdl data
		else {
			try {
				file.create(sbisHandlersStream, true, null);
				file.setCharset("ISO-8859-1", null);
			} catch (CoreException e) {
				ConvertigoPlugin.logException(e, "Error while editing the transaction wsdl type");
			}
		}
		
		setSite(site);
		setInput(input);
		eSite = site;
		eInput = input;
		setPartName(file.getName());
	}

	@Override
	public void addPropertyListener(IPropertyListener l) {
		if (listenerList == null)
			listenerList = new ListenerList();
		listenerList.add(l);
	}

	@Override
	public void removePropertyListener(IPropertyListener l) {
		if (listenerList != null) {
			listenerList.remove(l);
		}
	}
	
	public void propertyChanged(Object source, int propId) {
		// When a property from the xmlEditor Changes, walk the list all the listeners and notify them.
		Object listeners[] = listenerList.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			IPropertyListener listener = (IPropertyListener) listeners[i];
			listener.propertyChanged(this, propId);
		}
	}
}
