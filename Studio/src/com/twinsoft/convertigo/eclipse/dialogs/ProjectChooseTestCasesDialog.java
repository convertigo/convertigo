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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.TestCase;

public class ProjectChooseTestCasesDialog extends Dialog {

	private ProjectChooseTestCasesDialogComposite dlgComposite = null;
	private List<TestCase> mapListTestCases = new ArrayList<TestCase>();
	private Project project = null;
	
	private org.eclipse.swt.widgets.List listTCSequence = null;
	private org.eclipse.swt.widgets.List listTCTransaction = null;
	
	public ProjectChooseTestCasesDialog(Shell parent, Project project) {
		super(parent);
		this.project = project;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		final GridLayout gridLayout = new GridLayout();
		composite.setLayout(gridLayout);
		
		try {
			dlgComposite = new ProjectChooseTestCasesDialogComposite(composite, SWT.FILL, project);
			listTCSequence = dlgComposite.getListTCSequence();			
			listTCTransaction = dlgComposite.getListTCTransaction();	
		
		} catch (Exception e) {};
		
		return composite;
	}

	private void createMap(org.eclipse.swt.widgets.List listTestCase) {
		int[] selection = listTestCase.getSelectionIndices();
		
		for (int i = 0; i < selection.length; ++i){
			String str = listTestCase.getItem(selection[i]);
			TestCase testCase = (TestCase) listTestCase.getData(str);
			mapListTestCases.add(testCase);
		}
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttonBar =  super.createButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setText("OK");
//		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.CANCEL_ID).setText("Cancel");
		return buttonBar;
	}
	
	public void enableOK(boolean enabled) {
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Choose test cases");
	}
	
	@Override
	protected int getShellStyle() {
		return SWT.RESIZE | SWT.TITLE | SWT.APPLICATION_MODAL;
	}
	
	@Override
	protected void okPressed() {
		createMap(listTCSequence);
		createMap(listTCTransaction);
		super.okPressed();
	}

	public List<TestCase> getTestCasesMap() {
		return mapListTestCases;
	}
}
