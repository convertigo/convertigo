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
import org.eclipse.swt.widgets.Composite;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (mpe).
 */

public class NewProjectWizardPage5 extends WizardPage {
	private String ctgName;

	private String ctgServer;
	private String ctgPort;

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public NewProjectWizardPage5(ISelection selection) {
		super("wizardPage");
		setTitle("Define CICS connector parameters");
		setDescription("This step configures the CICS Transaction gateway (CTG) connection parameters");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new NewProjectWizardComposite5(parent, SWT.NULL,
				new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						dialogChanged();
					}
				}, (NewProjectWizard) this.getWizard());
		initialize();
		setControl(container);
		dialogChanged();
	}

	private void initialize() {
	}

	private void dialogChanged() {
		ctgName = ((NewProjectWizardComposite5) getControl()).getCtgName().getText();
		ctgServer = ((NewProjectWizardComposite5) getControl()).getCtgServer().getText();
		ctgPort = ((NewProjectWizardComposite5) getControl()).getCtgPort().getText();
		if (ctgName.length() == 0) {
			updateStatus("Please enter the name defined in the CTG configuration");
			return;
		}
		if (ctgServer.length() == 0) {
			updateStatus("Please enter the CTG server address");
			return;
		}
		if (ctgPort.length() == 0) {
			ctgPort = "1006";
		}
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public String getCtgName() {
		return ctgName;
	}

	public void setCtgName(String ctgName) {
		this.ctgName = ctgName;
	}

	public String getCtgPort() {
		return ctgPort;
	}

	public void setCtgPort(String ctgPort) {
		this.ctgPort = ctgPort;
	}

	public String getCtgServer() {
		return ctgServer;
	}

	public void setCtgServer(String ctgServer) {
		this.ctgServer = ctgServer;
	}
}
