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

package com.twinsoft.convertigo.eclipse.editors.connector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.genericeditor.ExtensionBasedTextEditor;
import org.eclipse.ui.part.EditorPart;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

public class ConnectorEditor extends ExtensionBasedTextEditor implements ISaveablePart2  {
	private boolean dirty = false;
	
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	public void close() {
		connectorEditorPart.close();
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof ConnectorEditorInput))
			throw new PartInitException("Invalid input: must be ConnectorEditorInput");
		super.init(site, input);
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}
	
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
		firePropertyChange(EditorPart.PROP_DIRTY);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	ConnectorEditorPart connectorEditorPart;
	
	void createEditorControl(Composite parent) {
		super.createPartControl(parent);
	}

	@Override
	public void createPartControl(Composite parent) {
		try {
			parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
			GridLayout gridLayout = new GridLayout();
			parent.setLayout(gridLayout);
			gridLayout.horizontalSpacing = 0;
			gridLayout.marginWidth = 0;
			gridLayout.marginHeight = 0;
			gridLayout.verticalSpacing = 0;

			GridData gridData = new GridData();
			gridData.horizontalAlignment = GridData.FILL;
			gridData.verticalAlignment = GridData.FILL;
			gridData.grabExcessHorizontalSpace = true;
			gridData.grabExcessVerticalSpace = true;
			
			ConnectorEditorInput connectorEditorInput = (ConnectorEditorInput) getEditorInput();
			
			connectorEditorPart = new ConnectorEditorPart(this, connectorEditorInput.connector, parent, SWT.None);
			connectorEditorPart.setLayoutData(gridData);
		}
		catch(Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to create editor part");
		}
	}

	@Override
	public void setFocus() {
		connectorEditorPart.setFocus();
	}

	public ConnectorEditorPart getConnectorEditorPart() {
		return connectorEditorPart;
	}
	
	public void getDocument(String transactionName, boolean isStubRequested) {
		getDocument(transactionName, null, isStubRequested);
	}
	
	public void getDocument(String transactionName, String testcaseName, boolean isStubRequested) {
		connectorEditorPart.getDocument(transactionName, testcaseName, null, isStubRequested);
	}

	public void getDocument(String transactionName, String testcaseName, String stubFileName, boolean isStubRequested) {
		connectorEditorPart.getDocument(transactionName, testcaseName, stubFileName, isStubRequested);
	}

	public Document getLastGeneratedDocument() {
		return connectorEditorPart.lastGeneratedDocument;
	}
	
	public ScreenClass getLastDetectedScreenClass() {
		return connectorEditorPart.getLastDetectedScreenClass();
	}

	@Override
	public int promptToSaveOnClose() {
		MessageBox messageBox = new MessageBox(Display.getCurrent().getActiveShell(), SWT.ICON_WARNING);
		messageBox.setText("Convertigo");
		messageBox.setMessage("A transaction is currently running.\nThe connector editor can't be closed.");
		messageBox.open();
		return CANCEL;
	}

	@Override
	public boolean isEditable() {
		return false;
	}

	@Override
	public boolean isEditorInputReadOnly() {
		return true;
	}
}
