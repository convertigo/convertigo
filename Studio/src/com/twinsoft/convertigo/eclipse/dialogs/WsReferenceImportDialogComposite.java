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

package com.twinsoft.convertigo.eclipse.dialogs;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.wizards.util.FileFieldEditor;
import com.twinsoft.convertigo.engine.util.FileUtils;

public class WsReferenceImportDialogComposite extends MyAbstractDialogComposite implements IWsReferenceComposite{

	private FileFieldEditor editor = null;
	private Combo combo = null;
	private String filePath = "";
	private String urlPath = "";
	private WsReferenceComposite wsRefAuthenticated = null;
	private Button OKButton = null;
	
	public Button useAuthentication = null;
	public Text loginText = null, passwordText = null;
	public ProgressBar progressBar = null;
	public Label labelInformation = null;
	
	/**
	 * @param parent
	 * @param style
	 */
	public WsReferenceImportDialogComposite(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	protected void initialize() {
		final Composite container = this;
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.makeColumnsEqualWidth = false;

		container.setLayout(gridLayout);
		
		/* Authenticated Composite for import WS Reference */
		GridData data1 = new GridData ();
		data1.horizontalAlignment = GridData.FILL;
		data1.grabExcessHorizontalSpace = true;
		data1.horizontalSpan = 2;
		
		labelInformation = new Label(this, SWT.NONE);
		//Set font text to BOLD
		FontData fontData = labelInformation.getFont().getFontData()[0];
		Font font = new Font(labelInformation.getDisplay(), new FontData(fontData.getName(), fontData
		    .getHeight(), SWT.BOLD));
		labelInformation.setFont(font);  
		labelInformation.setLayoutData(data1);
		
		data1 = new GridData ();
		data1.horizontalAlignment = GridData.FILL;
		data1.grabExcessHorizontalSpace = true;
		
		wsRefAuthenticated = new WsReferenceComposite(this, SWT.NONE, data1);
		
		combo = wsRefAuthenticated.getCombo();
		combo.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				comboChanged();
			}
		});
		
		editor = wsRefAuthenticated.getEditor();
		Composite fileSelectionArea = wsRefAuthenticated.getFileSelectionArea();
		editor.getTextControl(fileSelectionArea).addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				editorChanged();
			}
		});
		
		useAuthentication = wsRefAuthenticated.getUseAuthentication();
		useAuthentication.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dialogChanged();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				dialogChanged();
			}
		});
		
		ModifyListener ml = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		};
		
		loginText = wsRefAuthenticated.getLoginText();
		loginText.addModifyListener(ml);
		passwordText = wsRefAuthenticated.getPasswordText();
		passwordText.addModifyListener(ml);

		progressBar = new ProgressBar(this, SWT.NONE);	
		progressBar.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true, 2, 1));
		
		urlPath = combo.getText();
	}

	@Override
	public Object getValue(String name) {
		return null;
	}
	
	public void setOKButton(Button OKButton) {
		this.OKButton = OKButton;
	}
	
	public String getURL() {
		return combo.getText();
	}

	@Override
	public void dialogChanged() {
		String message = "";
		if (!urlPath.equals("")) {
			try {
				new URL(urlPath);
			}
			catch (Exception e) {
				message = "Please enter a valid URL";
			}
		}
		else if (!filePath.equals("")) {
			File file = new File(filePath);
			if (!file.exists()) {
				message = "Please select an existing file";
			} else {
				String[] filterExtensions = wsRefAuthenticated.getFilterExtension()[0].split(";");
				for (String fileFilter: filterExtensions) {
					String fileExtension = fileFilter.substring(fileFilter.lastIndexOf("."));
					if (filePath.endsWith(fileExtension)) {
						try {					
							String uriFile = FileUtils.toUriString(file);
							if(!Arrays.asList(combo.getItems()).contains(uriFile)){
								combo.add(uriFile);
								combo.select(combo.getItemCount()-1);
							} else {
								combo.select(wsRefAuthenticated.getItem(uriFile));
							}
						} catch (Exception e) {
							message = e.getMessage();
						}
					} else {
						message = "Please select a compatible file";
					}
				}
			}
		} else {
			message = "Please enter an URL!";
		}
		
		if (useAuthentication.getSelection() && 
				(loginText.getText().equals("") || passwordText.getText().equals("")) ) {
			message = "Please enter login and password";
		} 
		
		setTextStatus(message);
	}

	@Override
	public void comboChanged() {
		urlPath = combo.getText();
		if (urlPath.replaceAll(" ", "").equals("")) {
			filePath = "";
		}
		dialogChanged();
	}

	@Override
	public void editorChanged() {
		IPath path = new Path(editor.getStringValue());
		filePath = path.toString();
		File f = new File(filePath);
		if(f.exists()) {
			urlPath = "";
			dialogChanged();
		} else {
			editor.setStringValue(null);
			setTextStatus("Please select an existing file");
		}
	}

	@Override
	public void setTextStatus(String message) {
		WsReferenceImportDialog dialog = (WsReferenceImportDialog) parentDialog;
		OKButton = dialog.getButtonOK();
		
		//Set text color to red
		labelInformation.setForeground(new Color(labelInformation.getDisplay(), 255, 0, 0));
		
		if(message.equals("") && !wsRefAuthenticated.isValidURL()){
			labelInformation.setText("Please enter a valid URL");	
		} else {
			labelInformation.setText(message);	
		}

		if (wsRefAuthenticated.isValidURL()){
			OKButton.setEnabled(message.equals(""));
		} else {
			OKButton.setEnabled(false);
		}
	}
}
