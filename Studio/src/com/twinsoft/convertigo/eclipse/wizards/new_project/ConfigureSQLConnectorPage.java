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
 * $URL: http://sourceus.twinsoft.fr/svn/CEMS/trunk/Studio/src/com/twinsoft/convertigo/eclipse/wizards/NewProjectWizardPage3.java $
 * $Author: fabienb $
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

public class ConfigureSQLConnectorPage extends WizardPage {

	private String jdbcURL;
	private String jdbcDriver;
	private String username;
	private String password;
	
	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public ConfigureSQLConnectorPage(ISelection selection) {
		super("wizardPage");
		setTitle("Define SQL connector parameters");
		setDescription("This step configures the SQL connector parameters");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new ConfigureSQLConnectorComposite(parent, SWT.NULL,
				new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						dialogChanged();
					}
				});
		initialize();
		setControl(container);
		dialogChanged();
	}

	private void initialize() {
	}

	private void dialogChanged() {
		jdbcDriver = ((ConfigureSQLConnectorComposite) getControl()).getJdbcDriver();

		jdbcURL = ((ConfigureSQLConnectorComposite) getControl()).getJdbcURL();
		if (jdbcURL.length() == 0) {
			updateStatus("Please enter a valid JDBC URL");
			return;
		}

		username = ((ConfigureSQLConnectorComposite) getControl()).getUsername();

		password = ((ConfigureSQLConnectorComposite) getControl()).getPassword();

		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public String getJdbcURL() {
		return jdbcURL;
	}

	public String getJdbcDriver() {
		return jdbcDriver;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
