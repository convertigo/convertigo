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

package com.twinsoft.convertigo.eclipse.wizards.new_project;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;

public class NewProjectWizardPage14 extends WizardPage {
	private static int senchaCombo;
	private static Boolean frameworkSelected;
	private static String senchaPath;
	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public NewProjectWizardPage14(ISelection selection) {
		super("wizardPage");
		setTitle("Checking Sencha framework");
		setDescription("This step configures the Mobile Project");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new NewProjectWizardComposite14(parent, SWT.NULL,
				new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						dialogChanged();
					}
				}, 
				new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						dialogChanged();
					}
				},
				(NewProjectWizard) this.getWizard());
		setControl(container);
		dialogChanged();
	}

	private void dialogChanged() {
		senchaCombo = ((NewProjectWizardComposite14)getControl()).getSenchaCombo().getSelectionIndex();
		frameworkSelected = ((NewProjectWizardComposite14)getControl()).getFrameworkSelected();
		if (!frameworkSelected) {
			updateStatus("Please select a Sencha Touch framework");
			return;
		}
		setSenchaPath(((NewProjectWizardComposite14)getControl()).getSenchaPath().get(senchaCombo));
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public static void setSenchaPath(String senchaPath) {
		NewProjectWizardPage14.senchaPath = senchaPath;
	}

	public String getSenchaPath() {
		return senchaPath;
	}
}
