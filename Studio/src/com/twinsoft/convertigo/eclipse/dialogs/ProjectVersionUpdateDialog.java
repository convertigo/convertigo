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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class ProjectVersionUpdateDialog extends Dialog {

	private ProjectVersionUpdateDialogComposite dlgComposite = null;
	private String version = "";
	public String result = null;
	private boolean checkTestCases = false;
	
	public ProjectVersionUpdateDialog(Shell parent, String version) {
		super(parent);
		this.version = version;
		this.result = version;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		try {
			GridData gridData = new GridData (GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_BOTH);
			
			dlgComposite = new ProjectVersionUpdateDialogComposite(composite, SWT.NONE, version);
			dlgComposite.setLayoutData(gridData);
			dlgComposite.getTextControl().addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent arg0) {
					enableOK(!version.equals(dlgComposite.getValue(null)));
				}
			});
			
			final Button checkTestCases = dlgComposite.getCheckBoxControl();
			checkTestCases.addSelectionListener(new SelectionListener() {				
				@Override
				public void widgetSelected(SelectionEvent e) {
					Button button = (Button) e.widget;
					setCheckTestCases(button.getSelection()); 
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {}
			});
		}
		catch (Exception e) {};
		return composite;
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttonBar =  super.createButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setText("Update");
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		Button cancel = getButton(IDialogConstants.CANCEL_ID);
		cancel.setText("Skip");
		cancel.setFocus();
		return buttonBar;
	}
	
	public void enableOK(boolean enabled) {
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Version update");
	}
	
	@Override
	protected int getShellStyle() {
		return SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL;
	}
	
	@Override
	protected void okPressed() {
		if (dlgComposite != null) {
			result = ((String)dlgComposite.getValue(null));
		}
		super.okPressed();
	}
	
	public void setCheckTestCases(boolean value) {
		this.checkTestCases = value;
	}
	
	public boolean isCheckTestCases() {
		return checkTestCases;
	}
}
