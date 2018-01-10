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

package com.twinsoft.convertigo.eclipse.wizards.new_project;

import java.io.IOException;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

public class ConfigureSQLConnectorComposite extends Composite {

	private Text jdbcURL = null;
	private ModifyListener modifyListener;
	private Combo jdbcDriver = null;
	private Text username = null;
	private Text password = null;
	private Button testConnection = null;

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
		
		label = new Label(this, SWT.NONE);
		label.setText("Please configure the SQL server name or IP address and port. If needed, you can also specify a username/password.\n\n");
		label.setLayoutData(new GridData (GridData.FILL, GridData.CENTER, false, false, 2, 0) );
		
		label = new Label(this, SWT.NONE);
		label.setText("JDBC driver");
		label.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, false, false) );
		jdbcDriver = new Combo(this, SWT.BORDER | SWT.READ_ONLY);
		jdbcDriver.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, true, false) );

		try {
			Properties properties = new Properties();
			properties.load(getClass().getResourceAsStream("/jdbc_drivers.properties"));

			jdbcDrivers = new HashMap<String, String>(properties.size());
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
		label.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, false, false) );
		jdbcURL = new Text(this, SWT.BORDER);
		jdbcURL.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, true, false) );
		jdbcURL.setText(jdbcDrivers.get(jdbcDriver.getText()));

		label = new Label(this, SWT.NONE);
		label.setText("Username");
		label.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, false, false) );
		username = new Text(this, SWT.BORDER);
		username.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, true, false) );

		label = new Label(this, SWT.NONE);
		label.setText("Password");
		label.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, false, false) );
		password = new Text(this, SWT.PASSWORD | SWT.BORDER);
		password.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, true, false) );
		
		testConnection = new Button(this, SWT.NONE);
		testConnection.setText("Test Connection");
		testConnection.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				try {
					Class.forName(getJdbcDriver()).newInstance();

					DriverManager.getConnection( getJdbcURL(), 
							getUsername(), getPassword());
					
					MessageBox mb = new MessageBox(getParent().getShell(), SWT.ICON_WORKING | SWT.OK);
					mb.setMessage("Connection parameters are correct.");
					mb.open();
					
				} catch (Exception e1) {
					MessageBox mb = new MessageBox(getParent().getShell(), SWT.ICON_ERROR | SWT.OK);
					mb.setMessage("Failed to Connect to MySQL!");
					mb.open();
				}	
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
	
		testConnection.setLayoutData( new GridData (GridData.END, GridData.CENTER, false, false, 2, 0) );
		
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
		gridLayout.verticalSpacing = 15;
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