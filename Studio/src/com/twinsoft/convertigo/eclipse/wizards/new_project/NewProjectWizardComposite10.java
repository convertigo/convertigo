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

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.dialogs.IWsReferenceComposite;
import com.twinsoft.convertigo.eclipse.dialogs.WsReferenceComposite;
import com.twinsoft.convertigo.eclipse.wizards.util.FileFieldEditor;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.FileUtils;

public class NewProjectWizardComposite10 extends Composite implements IWsReferenceComposite {
	public Combo combo = null;	
	private FileFieldEditor editor = null;	
	private String filePath = "";
	private String urlPath = "";
	private String projectName = "";
	private WizardPage parentWizard = null;
	
	private WsReferenceComposite wsRefAuthenticated = null;
	public Button useAuthentication = null;
	public Text loginText = null, passwordText = null;
	
	public NewProjectWizardComposite10(Composite parent, String projectName, int style, WizardPage wizard) {
		super(parent, style);
		this.parentWizard = wizard;
		this.projectName = projectName;
		initialize();
	}

	protected void initialize() {
		final Composite container = this;
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.makeColumnsEqualWidth = false;
		
		container.setLayout(gridLayout);
				
		GridData data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		data.grabExcessHorizontalSpace = true;		
		
		/* Authenticated Composite for import WS Reference */
		wsRefAuthenticated = new WsReferenceComposite(this, SWT.NONE, data);
		
		combo = wsRefAuthenticated.getCombo();
		combo.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				comboChanged();
			}
		});
		
		editor = wsRefAuthenticated.getEditor();		
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
		
		Composite fileSelectionArea = wsRefAuthenticated.getFileSelectionArea();
		
		editor.getTextControl(fileSelectionArea).addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				editorChanged();
			}
		});	
	}
	
	@Override
	public void dialogChanged() {
		String message = null;
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
							String xsdFilePath = new File(filePath).getCanonicalPath();
							String projectPath = (new File(Engine.PROJECTS_PATH +"/"+ projectName)).getCanonicalPath();
							String workspacePath = (new File(Engine.USER_WORKSPACE_PATH)).getCanonicalPath();
							
							boolean isExternal = !xsdFilePath.startsWith(projectPath) && !xsdFilePath.startsWith(workspacePath);
							
							if (isExternal) {
								String uriFile = FileUtils.toUriString(file);
								if(!Arrays.asList(combo.getItems()).contains(uriFile)){
									combo.add(uriFile);
									combo.select(combo.getItemCount()-1);
								} else {
									combo.select(wsRefAuthenticated.getItem(uriFile));
								}
							}
							else {
								if (xsdFilePath.startsWith(projectPath))
									xsdFilePath = "./" + xsdFilePath.substring(projectPath.length());
								else if (xsdFilePath.startsWith(workspacePath))
									xsdFilePath = "." + xsdFilePath.substring(workspacePath.length());
								xsdFilePath = xsdFilePath.replaceAll("\\\\", "/");
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
		if(message==null && !wsRefAuthenticated.isValidURL()){
			parentWizard.setErrorMessage("Please enter a valid URL");	
		} else {
			parentWizard.setErrorMessage(message);	
		}

		if (wsRefAuthenticated.isValidURL()){
			parentWizard.setPageComplete(message==null);
		} else {
			parentWizard.setPageComplete(false);
		}
	}
}
