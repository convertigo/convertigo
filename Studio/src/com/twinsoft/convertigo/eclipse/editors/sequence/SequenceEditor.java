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

package com.twinsoft.convertigo.eclipse.editors.sequence;

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
import org.eclipse.ui.part.EditorPart;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

public class SequenceEditor extends EditorPart implements ISaveablePart2 {
	private boolean dirty = false;
	
	public void doSave(IProgressMonitor monitor) {
	}

	public void doSaveAs() {
	}

	public void close() {
		sequenceEditorPart.close();
	}
	
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
	     if (!(input instanceof SequenceEditorInput))
			throw new PartInitException("Invalid input: must be SequenceEditorInput");
		setSite(site);
		setInput(input);

		SequenceEditorInput sequenceEditorInput = (SequenceEditorInput) getEditorInput();
		setPartName(sequenceEditorInput.sequence.getParent().getName() + " [S: " + sequenceEditorInput.sequence.getName()+"]");
	}

	public boolean isDirty() {
		return dirty;
	}
	
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
		firePropertyChange(EditorPart.PROP_DIRTY);
	}

	public boolean isSaveAsAllowed() {
		return true;
	}

	SequenceEditorPart sequenceEditorPart;
	
	public void dispose() {
		super.dispose();
	}
	
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
			
			SequenceEditorInput sequenceEditorInput = (SequenceEditorInput) getEditorInput();
			
			sequenceEditorPart = new SequenceEditorPart(this, sequenceEditorInput.sequence, parent, SWT.None);
			sequenceEditorPart.setLayoutData(gridData);
		}
		catch(Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to create editor part");
		}
	}

	public void setFocus() {
		sequenceEditorPart.compositeSequence.setFocus();
	}

	public SequenceEditorPart getSequenceEditorPart() {
		return sequenceEditorPart;
	}
	
	public void getDocument(String sequenceName, boolean isStubRequested) {
		getDocument(sequenceName, null, isStubRequested);
	}
	
	public void getDocument(String sequenceName, String testcaseName, boolean isStubRequested) {
		sequenceEditorPart.getDocument(sequenceName, testcaseName, isStubRequested);
	}
	
	public Document getLastGeneratedDocument() {
		return sequenceEditorPart.lastGeneratedDocument;
	}

	@Override
	public int promptToSaveOnClose() {
		MessageBox messageBox = new MessageBox(Display.getCurrent().getActiveShell(), SWT.ICON_WARNING);
		messageBox.setText("Convertigo");
		messageBox.setMessage("A sequence is currently running.\nThe sequence editor can't be closed.");
		messageBox.open();
		
		return CANCEL;
	}
}
