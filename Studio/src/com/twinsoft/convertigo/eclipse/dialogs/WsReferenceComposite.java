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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.wizards.util.FileFieldEditor;

public class WsReferenceComposite extends Composite {
	private String[] filterExtension = new String[]{"*.wsdl", "*.xml"};
	private String[] filterNames =  new String[]{"WSDL files", "XML files"};
	
	public Button useAuthentication = null;
	public Text loginText = null;
	public Text passwordText = null;
	
	public Combo combo = null;
	public FileFieldEditor editor = null;
	public Composite fileSelectionArea = null;
	
	private GridData data = null;
	
	public WsReferenceComposite(Composite parent, int style, GridData gridData) {
		super(parent, style);
		this.data = gridData;
		initialize();
	}

	public WsReferenceComposite(Composite parent, int style, GridData gridData,
			String[] filterExtension, String[] filterNames) {
		this (parent, style, gridData);
		this.filterExtension = filterExtension;
		this.filterNames = filterNames;
	}
	
	private void initialize() {
		this.setLayout(new GridLayout());
		this.setLayoutData(data);
		
		Label label1 = new Label(this, SWT.NULL);
		label1.setText("&Enter URL:");
		label1.setLayoutData(new GridData());
		
		GridData data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		
		/** IMPORT WS REFERENCE PART **/
		
		combo = new Combo(this, SWT.BORDER);
		if (filterExtension[0].equals("*.wsdl")) {
			combo.add("http://www.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL");
			combo.select(0);
		}
		combo.setLayoutData(data);
		
		fileSelectionArea = new Composite(this, SWT.NONE);
		GridData fileSelectionData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
		fileSelectionData.horizontalSpan = 3;
		fileSelectionArea.setLayoutData(fileSelectionData);

		data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		
		editor = new FileFieldEditor("fileSelect","Select File: ", fileSelectionArea);
		editor.setFilterExtensions(filterExtension);
		editor.setFilterNames(filterNames);
		editor.getTextControl(fileSelectionArea).setEnabled(false);
		
		/** AUTHENTICATION PART **/
		
		useAuthentication = new Button(this, SWT.CHECK);
		useAuthentication.setText("URL need an authenfication");
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
	
	public String[] getFilterExtension() {
		return filterExtension;
	}

	public void setFilterExtension(String[] filterExtension) {
		this.filterExtension = filterExtension;
	}

	public Composite getFileSelectionArea() {
		return fileSelectionArea;
	}

	public void setFileSelectionArea(Composite fileSelectionArea) {
		this.fileSelectionArea = fileSelectionArea;
	}
}
