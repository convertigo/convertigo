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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;


public class NewProjectWizardPage11 extends WizardPage {
	private String targetURL;
	private boolean bTrustAllCertificates;
	
	public NewProjectWizardPage11(ISelection selection) {
		super("wizardPage");
		setTitle("Clipper target host");
		setDescription("This step allows you to define the target host url");
	}

	public void createControl(Composite parent) {
		Composite container = new NewProjectWizardComposite11(parent, SWT.NULL,
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
	
	protected String getTargetURL() {
		return targetURL;
	}
	
	protected boolean isTrustAllServerCertificates() {
		return bTrustAllCertificates;
	}
	
	private boolean isValidURL() {
		return targetURL.startsWith("http://") || targetURL.startsWith("https://");
	}
	
	private void dialogChanged() {
		targetURL = ((NewProjectWizardComposite11) getControl()).targetUrl.getText();
		bTrustAllCertificates = ((NewProjectWizardComposite11) getControl()).trustCertificates.getSelection();
		
		if (targetURL.length() == 0) {
			updateStatus("Please enter the target host url");
			return;
		}
		if (!isValidURL()) {
			updateStatus("Please enter a valid url");
			return;
		}

		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
}
	
