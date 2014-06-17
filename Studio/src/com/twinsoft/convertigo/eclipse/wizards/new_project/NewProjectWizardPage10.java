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
	private String projectName;
	
	public NewProjectWizardPage10(ISelection selection, String projectName) {
		super("wizardPage");
		setTitle("Import web service reference");
		setDescription("This step creates a new http connector to invoke the remote web service");
	}

	public void createControl(Composite parent) {
		Composite container = new NewProjectWizardComposite10(parent, projectName, SWT.NULL,
				new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						dialogChanged();
					}
				}, this.getWizard());
				
		setControl(container);
		
		setPageComplete(isValidURL());
	}
	
	protected String getWsdlURL() {
		final String[] wsdlURL = new String[1];
		
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				wsdlURL[0] = ((NewProjectWizardComposite10) getControl()).combo.getText();
			}
			
		});
		return wsdlURL[0];
	}
	
	protected boolean useAuthentication() {
		final boolean useAuthentication[] = new boolean[1];
		
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				useAuthentication[0] = ((NewProjectWizardComposite10) getControl()).useAuthentication.getSelection();
			}
			
		});
		return useAuthentication[0];
	}
	
	protected String getLogin(){
		final String login[] = new String[1];
		
		getShell().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				login[0] = ((NewProjectWizardComposite10) getControl()).loginText.getText();
			}
			
		});
		return login[0];
	}
	
	protected String getPassword(){
		final String password[] = new String[1];
		
		getShell().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				password[0] = ((NewProjectWizardComposite10) getControl()).passwordText.getText();
			}
			
		});
		return password[0];
	}
	
	private boolean isValidURL() {
		return getWsdlURL().startsWith("http://") || getWsdlURL().startsWith("https://") || getWsdlURL().startsWith("file:/");
	}
	
	private void dialogChanged() {
		String wsdlURL = getWsdlURL();
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
	
