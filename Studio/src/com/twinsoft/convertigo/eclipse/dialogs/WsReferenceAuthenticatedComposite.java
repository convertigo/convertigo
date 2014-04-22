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
 */

package com.twinsoft.convertigo.eclipse.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class WsReferenceAuthenticatedComposite extends Composite {
	public Button useAuthentication = null;
	public Text loginText = null;
	public Text passwordText = null;
	
	private GridData data = null;
	
	public WsReferenceAuthenticatedComposite(Composite parent, int style, GridData gridData) {
		super(parent, style);
		this.data = gridData;
		initialize();
	}

	private void initialize() {
		this.setLayout(new GridLayout());
		this.setLayoutData(data);
		
		useAuthentication = new Button(this, SWT.CHECK);
		useAuthentication.setText("WSDL URL need an authenfication");
		useAuthentication.setSelection(false);
		useAuthentication.setLayoutData(data);
		
		useAuthentication.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				loginText.setEnabled(useAuthentication.getSelection());
				passwordText.setEnabled(useAuthentication.getSelection());
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				loginText.setEnabled(useAuthentication.getSelection());
				passwordText.setEnabled(useAuthentication.getSelection());
			}
		});
		
		loginText = new Text(this, SWT.NONE);
		loginText.setMessage("Login");
		loginText.setEnabled(false);
		loginText.setLayoutData(data);
		
		passwordText = new Text(this, SWT.SINGLE | SWT.PASSWORD);
		passwordText.setMessage("Password");
		passwordText.setEnabled(false);
		passwordText.setLayoutData(data);
		
	}
}
