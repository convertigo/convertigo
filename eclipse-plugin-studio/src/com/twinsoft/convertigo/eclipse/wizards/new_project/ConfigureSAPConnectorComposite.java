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
 * $URL: http://sourceus.twinsoft.fr/svn/CEMS/trunk/Studio/src/com/twinsoft/convertigo/eclipse/wizards/new_project/ConfigureSAPConnectorComposite.java $
 * $Author: julienda $
 * $Revision: 28379 $
 * $Date: 2011-09-27 11:38:59 +0200 (mar., 27 sept. 2011) $
 */

package com.twinsoft.convertigo.eclipse.wizards.new_project;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ConfigureSAPConnectorComposite extends Composite {
	
	private Text asHost = null;
	private Text systemNumber = null;
	private Text client = null;
	private Text user = null;
	private Text password = null;
	private Text language = null;

	private ModifyListener modifyListener = null;
	
	public ConfigureSAPConnectorComposite(Composite parent, int style, ModifyListener modifyListener) {
		super(parent, style);
		this.modifyListener = modifyListener;
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
		Label label = new Label(this, SWT.NONE);
		label.setText("Please set the SAP connector properties.\n\n");
		label.setLayoutData(new GridData (GridData.FILL, GridData.CENTER, false, false, 2, 0) );
		
		// Application Server Host
		label = new Label(this, SWT.NONE);
		label.setText("Application Server Host");
		label.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, false, false) );
		
		asHost = new Text(this, SWT.NONE);
		asHost.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, true, false));
		asHost.addModifyListener(modifyListener);
		
		// System Number
		label = new Label(this, SWT.NONE);
		label.setText("System Number");
		label.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, false, false) );
		
		systemNumber = new Text(this, SWT.NONE);
		systemNumber.setText("00");
		systemNumber.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, true, false));
		systemNumber.addModifyListener(modifyListener);
		
		// Client
		label = new Label(this, SWT.NONE);
		label.setText("Client");
		label.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, false, false) );
		
		client = new Text(this, SWT.NONE);
		client.setText("000");
		client.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, true, false));
		client.addModifyListener(modifyListener);
		
		// User
		label = new Label(this, SWT.NONE);
		label.setText("User");
		label.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, false, false) );
		
		user = new Text(this, SWT.NONE);
		user.setText("SAP*");
		user.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, true, false));
		user.addModifyListener(modifyListener);
		
		// Password
		label = new Label(this, SWT.NONE);
		label.setText("Password");
		label.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, false, false) );
		
		password = new Text(this, SWT.PASSWORD);
		password.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, true, false));
		password.addModifyListener(modifyListener);
		
		// Language
		label = new Label(this, SWT.NONE);
		label.setText("Language");
		label.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, false, false) );
		
		language = new Text(this, SWT.NONE);
		language.setText("en");
		language.setLayoutData( new GridData (GridData.FILL, GridData.CENTER, true, false));
		language.addModifyListener(modifyListener);
		
		// Layout
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

	public String getAsHost() {
		return asHost.getText();
	}

	public String getSystemNumber() {
		return systemNumber.getText();
	}

	public String getClient() {
		return client.getText();
	}

	public String getUser() {
		return user.getText();
	}

	public String getPassword() {
		return password.getText();
	}

	public String getLanguage() {
		return language.getText();
	}

	
	
}