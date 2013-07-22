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

package com.twinsoft.convertigo.eclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.eclipse.wizards.new_object.ObjectsExplorerComposite;

public class TransactionXSDTypesDialog extends Dialog {

	private String dialogTitle;
	private Object parentObject = null;
	public String result = null;
	
	public TransactionXSDTypesDialog(Shell parentShell, Object parentObject) {
		this(parentShell, ObjectsExplorerComposite.class, "Transaction schema types", parentObject);
	}
	
	public TransactionXSDTypesDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle, Object parentObject) {
		super(parentShell);
		this.dialogTitle = dialogTitle;
		this.parentObject = parentObject;
	}
	
	private TransactionXSDTypesDialogComposite transactionXSDTypesDialogComposite = null;
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		try {
			GridData gridData = new GridData (GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_BOTH);
			
			transactionXSDTypesDialogComposite = new TransactionXSDTypesDialogComposite(composite,SWT.NONE,parentObject);
			transactionXSDTypesDialogComposite.setLayoutData(gridData);
			transactionXSDTypesDialogComposite.list.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					enableOK(transactionXSDTypesDialogComposite.list.getSelectionCount() > 0);
				}
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
		}
		catch(Exception e) {;}
		
		return composite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttonBar =  super.createButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		return buttonBar;
	}
	
	public void enableOK(boolean enabled) {
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(dialogTitle);
	}
	
	@Override
	protected int getShellStyle() {
		return SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL;
	}
	
	@Override
	protected void okPressed() {
		if (transactionXSDTypesDialogComposite != null) {
			transactionXSDTypesDialogComposite.generateXSDTypes();
			result = transactionXSDTypesDialogComposite.getXsdTypes();
		}
		super.okPressed();
	}
}
