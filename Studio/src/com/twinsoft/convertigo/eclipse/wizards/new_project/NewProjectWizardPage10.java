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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;


public class NewProjectWizardPage10 extends WizardPage {
	private String wsdlURL;
	
	public NewProjectWizardPage10(ISelection selection) {
		super("wizardPage");
		setTitle("Import web service reference");
		setDescription("This step creates a new http connector to invoke the remote web service");
	}

	public void createControl(Composite parent) {
		Composite container = new NewProjectWizardComposite10(parent, SWT.NULL,
				new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						dialogChanged();
					}
				}, this.getWizard());
				
		setControl(container);
		wsdlURL = ((NewProjectWizardComposite10) getControl()).combo.getText();
		setPageComplete(isValidURL());
	}
	
	protected String getWsdlURL() {
		return wsdlURL;
	}
	
	private boolean isValidURL() {
		return wsdlURL.startsWith("http://") || wsdlURL.startsWith("https://") || wsdlURL.startsWith("file://");
	}
	
	private void dialogChanged() {
		wsdlURL = ((NewProjectWizardComposite10) getControl()).combo.getText();
		if (wsdlURL.length() == 0) {
			updateStatus("Please enter the WSDL url of remote web service");
			return;
		}
		if (!isValidURL()) {
			updateStatus("Please enter a valid WSDL url");
			return;
		}
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
}
	
