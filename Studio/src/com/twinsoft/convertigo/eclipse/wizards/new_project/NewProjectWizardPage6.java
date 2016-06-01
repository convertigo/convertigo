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

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (mpe).
 */

public class NewProjectWizardPage6 extends WizardPage {
	private String  httpServer;
	private String  httpPort;
	private boolean bSSL;

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public NewProjectWizardPage6(ISelection selection) {
		super("wizardPage");
		setTitle("Define HTTP or HTML Connector parameters");
		setDescription("This step configures the HTTP or Web connector parameters");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new NewProjectWizardComposite6(parent, SWT.NULL,
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
		initialize();
		setControl(container);
		dialogChanged();
	}

	private void initialize() {
	}

	private void dialogChanged() {
		httpServer = ((NewProjectWizardComposite6) getControl()).getHttpServer().getText();
		httpPort = ((NewProjectWizardComposite6) getControl()).getHttpPort().getText();
		bSSL = ((NewProjectWizardComposite6) getControl()).getSsl().getSelection();
		if (httpServer.length() == 0) {
			updateStatus("Please enter the http server address, default port 80 will be used if no port specified");
			return;
		}
		if (httpServer.toLowerCase().startsWith("http://") || httpServer.toLowerCase().startsWith("https://")) {
			updateStatus("Please enter the http server address, without 'https://' or 'http://'");
			return;
		}

		if (httpPort.length() == 0) {
			if (!bSSL)
				httpPort = "80";
			else
				httpPort = "443";
		}
		
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public boolean isBSSL() {
		return bSSL;
	}

	public void setBSSL(boolean bssl) {
		bSSL = bssl;
	}

	public String getHttpPort() {
		return httpPort;
	}

	public void setHttpPort(String httpPort) {
		this.httpPort = httpPort;
	}

	public String getHttpServer() {
		return httpServer;
	}

	public void setHttpServer(String httpServer) {
		this.httpServer = httpServer;
	}
}
