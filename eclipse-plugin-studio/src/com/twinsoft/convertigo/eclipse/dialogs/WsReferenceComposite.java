/*
 * Copyright (c) 2001-2025 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.dialogs;

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.wizards.util.FileFieldEditor;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.util.WsReference;
import com.twinsoft.convertigo.engine.util.WsReference.CreateSequenceOptions;

public class WsReferenceComposite extends Composite {
	private String[] filterExtension = new String[]{"*.wsdl", "*.xml"};
	private String[] filterNames =  new String[]{"WSDL files", "XML files"};
	private Button useAuthentication = null;
	private Text loginText = null;
	private Text passwordText = null;
	
	private Combo combo = null;
	private FileFieldEditor editor = null;
	private Composite fileSelectionArea = null;	
	private GridData data = null;
	
	private static final List<String> soapUrls = Arrays.asList(
					"http://www.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL", 
					"https://demo.convertigo.net/convertigo/projects/globalCompany_HR_WS/.wsl?wsdl", 
					"https://demo.convertigo.net/convertigo/projects/globalCompany_accounting_WS/.wsl?wsdl");
	
	private static final List<String> restUrls = Arrays.asList(
			"https://petstore3.swagger.io/api/v3/openapi.yaml",
			"https://petstore3.swagger.io/api/v3/openapi.json");
	
	public WsReferenceComposite(Composite parent, int style, GridData gridData) {
		super(parent, style);
		this.data = gridData;
		initialize();
	}
	
	private void initialize() {
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		this.setLayout(layout);
		this.setLayoutData(data);
		
		GridData gridData = new GridData();
		
		Label label1 = new Label(this, SWT.NULL);
		label1.setText("Enter URL:  ");
		label1.setLayoutData(gridData);

		gridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);

		/** IMPORT WS REFERENCE PART **/

		combo = new Combo(this, SWT.BORDER);
		combo.setLayoutData(gridData);

		fileSelectionArea = new Composite(this, SWT.NONE);
		GridData fileSelectionData = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.FILL_HORIZONTAL);
		fileSelectionData.horizontalSpan = 2;
		fileSelectionArea.setLayoutData(fileSelectionData);

		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 2;
		
		editor = new FileFieldEditor("fileSelect", "Select File: ", fileSelectionArea);
		editor.setFilterExtensions(filterExtension);
		if (filterExtension[0].equals("*.wsdl")) {
			for (String url : soapUrls) {
				combo.add(url);
			}
			combo.select(0);
		}
		else if (filterExtension[0].equals("*.yaml")) {
			for (String url : restUrls) {
				combo.add(url);
			}
			combo.select(0);
		}
		editor.setFilterNames(filterNames);
		editor.getTextControl(fileSelectionArea).setEnabled(false);

		/** AUTHENTICATION PART **/
		
		useAuthentication = new Button(this, SWT.CHECK);
		useAuthentication.setText("URL need an authentication");
		useAuthentication.setSelection(false);
		useAuthentication.setLayoutData(gridData);
		
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
		
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		
		Label loginLabel = new Label(this, SWT.NULL);
		loginLabel.setText("Login:  ");
		loginText = new Text(this, SWT.NONE | SWT.BORDER);
		loginText.setMessage("login");
		loginText.setEnabled(false);
		loginText.setLayoutData(gridData);
		
		Label passwordLabel = new Label(this, SWT.NULL);
		passwordLabel.setText("Password:  ");
		passwordText = new Text(this, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
		passwordText.setMessage("password");
		passwordText.setEnabled(false);
		passwordText.setLayoutData(gridData);
		
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.heightHint = 20;
		gridData.horizontalSpan = 2;
		
		var separator = new Label(this, SWT.HORIZONTAL | SWT.SEPARATOR);
	    separator.setLayoutData(gridData);
		
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 2;
	    
		WsReference.nextCreateSequences = null;
		var createSequences = new Button(this, SWT.CHECK);
		createSequences.setText("Create related Sequences");
		createSequences.setSelection(false);
		createSequences.setLayoutData(gridData);
		
		RowLayout rl;
		var seqOpt = new Composite(this, SWT.NONE);
		seqOpt.setVisible(false);
		seqOpt.setLayoutData(gridData = new GridData(GridData.FILL_HORIZONTAL));
		gridData.horizontalSpan = 2;
		
		seqOpt.setLayout(rl = new RowLayout(SWT.VERTICAL));
		rl.spacing = 10;
		
		var label = new Label(seqOpt, SWT.NONE);
		label.setText("Default security settings for created sequences (not for updated):");
		
		var accessibility = new Composite(seqOpt, SWT.NONE);
		accessibility.setLayout(rl = new RowLayout());
		rl.center = true;
		label = new Label(accessibility, SWT.NONE);
		label.setText("Accessibility: ");
		var combo = new Combo(accessibility, SWT.READ_ONLY);
		for (Accessibility a: Accessibility.values()) {
			combo.add(a.name());
		}
		combo.setText(Accessibility.Hidden.name());
		
		label = new Label(seqOpt, SWT.NONE);
		label.setText("↓ Check this to set authenticated session MANDATORY ↓");
		
		var auth = new Button(seqOpt, SWT.CHECK);
		auth.setText(" Authentication required");
		auth.setSelection(true);
		
		var listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (createSequences.getSelection()) {
					WsReference.nextCreateSequences = new CreateSequenceOptions(Accessibility.valueOf(combo.getText()), auth.getSelection());
					seqOpt.setVisible(true);
				} else {
					WsReference.nextCreateSequences = null;
					seqOpt.setVisible(false);
				}
			}
		};
		
		auth.addSelectionListener(listener);
		combo.addSelectionListener(listener);		
		createSequences.addSelectionListener(listener);
		
		layout();
	}
	
	public void setFilterExtension(String[] filterExtension) {
		if (filterExtension != null && filterExtension.length > 0) {
			this.filterExtension = filterExtension;
			editor.setFilterExtensions(filterExtension);
			combo.removeAll();
			if (filterExtension[0].equals("*.wsdl")) {
				for (String url : soapUrls) {
					if(!Arrays.asList(combo.getItems()).contains(url)) {
						combo.add(url);
					}
				}
				combo.select(0);
			} else if (filterExtension[0].equals("*.yaml")) {
				for (String url : restUrls) {
					if(!Arrays.asList(combo.getItems()).contains(url)) {
						combo.add(url);
					}
				}
				combo.select(0);
			}
		}
	}
	
	public void setFilterNames(String[] filterNames){
		if (filterNames != null && filterNames.length > 0) {
			this.filterNames = filterNames;
			editor.setFilterNames(filterNames);
		}
	}
	
	public String[] getFilterExtension() {
		return filterExtension;
	}

	public Composite getFileSelectionArea() {
		return fileSelectionArea;
	}
		
	public Combo getCombo() {
		return combo;
	}
	
	public FileFieldEditor getEditor(){
		return editor;
	}
	
	public Button getUseAuthentication() {
		return useAuthentication;
	}
 

	public Text getLoginText() {
		return loginText;
	}

	public Text getPasswordText() {
		return passwordText;
 	}
	
	public boolean isValidURL() {
		String url = combo.getText();
		return ((url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file:/")) && !(url.replaceAll(" ", "").equals("")));
	}

	
	public int getItem(String uriFile){
		for (int i = 0; i < combo.getItemCount(); i++){
			if(combo.getItem(i).equals(uriFile)){
				return i;
			}
		}
		return -1;
	}
}
