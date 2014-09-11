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
 * $URL: http://sourceus.twinsoft.fr/svn/CEMS/trunk/Studio/src/com/twinsoft/convertigo/eclipse/wizards/new_project/ConfigureSAPConnectorPage.java $
 * $Author: julienda $
 * $Revision: 28379 $
 * $Date: 2011-09-27 11:38:59 +0200 (mar., 27 sept. 2011) $
 */

package com.twinsoft.convertigo.eclipse.wizards.new_project;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;

public class ConfigureSAPConnectorPage extends WizardPage {

	private String asHost;
	private String systemNumber = "00";
	private String client = "000";
	private String user = "SAP*";
	private String password;
	private String language = "en";
	
	/**
	 * Constructor for ConfigureSAPConnectorPage
	 * @param selection
	 */
	public ConfigureSAPConnectorPage(ISelection selection) {
		super("wizardPage");
		setTitle("Define SAP connector parameters");
		setDescription("This step configures the SAP connector parameters");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new ConfigureSAPConnectorComposite(parent, SWT.NULL,
				new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						dialogChanged();
					}
				});
		initialize();
		setControl(container);
		dialogChanged();
	}

	private void initialize() {}

	private void dialogChanged() {
		asHost = ((ConfigureSAPConnectorComposite) getControl()).getAsHost();
		if (asHost.length() == 0) {
			updateStatus("Please enter a \"Application Server Host\".");
			return;
		}
		
		systemNumber = ((ConfigureSAPConnectorComposite) getControl()).getSystemNumber();
		if (systemNumber.length() == 0) {
			updateStatus("Please enter a \"System Number\".");
			return;
		}
		
		client = ((ConfigureSAPConnectorComposite) getControl()).getClient();
		if (client.length() == 0) {
			updateStatus("Please enter a \"Client\".");
			return;
		}
		
		user = ((ConfigureSAPConnectorComposite) getControl()).getUser();
		if (user.length() == 0) {
			updateStatus("Please enter an \"User\".");
			return;
		}
		
		password = ((ConfigureSAPConnectorComposite) getControl()).getPassword();
		if (password.length() == 0) {
			updateStatus("Please enter a \"Password\".");
			return;
		}
		
		language = ((ConfigureSAPConnectorComposite) getControl()).getLanguage();
		if (language.length() == 0) {
			updateStatus("Please enter a valid \"Language\".");
			return;
		}
		
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public String getAsHost() {
		return asHost;
	}

	public void setAsHost(String asHost) {
		this.asHost = asHost;
	}

	public String getSystemNumber() {
		return systemNumber;
	}

	public void setSystemNumber(String systemNumber) {
		this.systemNumber = systemNumber;
	}

	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}
}
