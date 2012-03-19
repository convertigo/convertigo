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
 * $URL: http://sourceus.twinsoft.fr/svn/CEMS/trunk/Studio/src/com/twinsoft/convertigo/eclipse/wizards/NewProjectWizardComposite3.java $
 * $Author: fabienb $
 * $Revision: 28379 $
 * $Date: 2011-09-27 11:38:59 +0200 (mar., 27 sept. 2011) $
 */

package com.twinsoft.convertigo.eclipse.wizards;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ConfigureSQLConnectorComposite extends Composite {

	private Text jdbcURL = null;
	private ModifyListener modifyListener;
	private Combo jdbcDriver = null;
	private Text username = null;
	private Text password = null;

	private Map<String, String> jdbcDrivers;

	public ConfigureSQLConnectorComposite(Composite parent, int style, ModifyListener modifyListener) {
		super(parent, style);
		this.modifyListener = modifyListener;
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
		Label label;

		GridData gridData2 = new GridData();
		gridData2.horizontalSpan = 2;
		label = new Label(this, SWT.NONE);
		label.setText("Please configure the SQL server name or IP address and port. If needed, you can also specify a username/password.");
		label.setLayoutData(gridData2);

		GridData gridData1 = new GridData();
		gridData1.grabExcessHorizontalSpace = true;
		gridData1.verticalAlignment = GridData.CENTER;
		gridData1.horizontalAlignment = GridData.FILL;

		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = false;
		gridData.horizontalAlignment = GridData.BEGINNING;
		gridData.verticalAlignment = GridData.CENTER;
		gridData.grabExcessVerticalSpace = false;

		label = new Label(this, SWT.NONE);
		label.setText("JDBC driver");
		label.setLayoutData(gridData);
		jdbcDriver = new Combo(this, SWT.BORDER);
		jdbcDriver.setLayoutData(gridData1);

		try {
			Properties properties = new Properties();
			properties.load(getClass().getResourceAsStream("/jdbc_drivers.properties"));

			jdbcDrivers = new Hashtable<String, String>(properties.size());
			for (Object sDriverName : properties.values()) {
				String[] t = ((String) sDriverName).split(",");
				jdbcDriver.add(t[0]);
				jdbcDrivers.put(t[0], t[1]);
			}
			jdbcDriver.select(1);
		} catch (IOException e) {
		}

		label = new Label(this, SWT.NONE);
		label.setText("JDBC URL");
		label.setLayoutData(gridData);
		jdbcURL = new Text(this, SWT.BORDER);
		jdbcURL.setLayoutData(gridData1);
		jdbcURL.setText(jdbcDrivers.get(jdbcDriver.getText()));

		label = new Label(this, SWT.NONE);
		label.setText("Username");
		label.setLayoutData(gridData);
		username = new Text(this, SWT.BORDER);
		username.setLayoutData(gridData1);

		label = new Label(this, SWT.NONE);
		label.setText("Password");
		label.setLayoutData(gridData);
		password = new Text(this, SWT.PASSWORD | SWT.BORDER);
		password.setLayoutData(gridData1);

		jdbcURL.addModifyListener(modifyListener);
		jdbcDriver.addModifyListener(modifyListener);
		jdbcDriver.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						jdbcURL.setText(jdbcDrivers.get(jdbcDriver.getText()));
					}
				});
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		username.addModifyListener(modifyListener);
		password.addModifyListener(modifyListener);

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		this.setLayout(gridLayout);
	}

	public ModifyListener getModifyListener() {
		return modifyListener;
	}

	public void setModifyListener(ModifyListener modifyListener) {
		this.modifyListener = modifyListener;
	}

	public String getJdbcURL() {
		return jdbcURL.getText();
	}

	public String getJdbcDriver() {
		return jdbcDriver.getText();
	}

	public String getUsername() {
		return username.getText();
	}

	public String getPassword() {
		return password.getText();
	}

}