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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.wizards.references;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.IWizardPage;
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

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.references.RemoteFileReference;
import com.twinsoft.convertigo.beans.references.RestServiceReference;
import com.twinsoft.convertigo.beans.references.WebServiceReference;
import com.twinsoft.convertigo.eclipse.dialogs.IWsReferenceComposite;
import com.twinsoft.convertigo.eclipse.dialogs.WsReferenceComposite;
import com.twinsoft.convertigo.eclipse.wizards.new_object.ObjectExplorerWizardPage;
import com.twinsoft.convertigo.eclipse.wizards.util.FileFieldEditor;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.FileUtils;

public abstract class RemoteFileWizardPage extends WizardPage implements IWsReferenceComposite {
	private String[] filterExtension = new String[]{"*"};
	private String[] filterNames = new String[]{"All files"};
	private Object parentObject = null;
	
	private WsReferenceComposite wsRefAuthenticated = null;
	private Button useAuthentication = null;
	private Text loginText = null, passwordText = null;
	
	private FileFieldEditor editor = null;
	private Combo combo = null;
	private String filePath = "";
	private String urlPath = "";
	
	public RemoteFileWizardPage(Object parentObject, String pageName) {
		super(pageName);
		this.parentObject = parentObject;
		setTitle("Schema File");
		setDescription("Please enter an url OR choose a file.");
	}

	public String[] getFilterExtension() {
		return filterExtension;
	}

	public void setFilterExtension(String[] filterExtension) {
		this.filterExtension = filterExtension;
	}
	
	public String[] getFilterNames() {
		return filterNames;
	}

	public void setFilterNames(String[] filterNames) {
		this.filterNames = filterNames;
	}
	
	public Text getPasswordText() {
		return passwordText;
	}
	
	public Text getLoginText() {
		return loginText;
	}
	
	public Button getUseAuthentication() {
		return useAuthentication;
	}
	
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.horizontalSpacing = 15;
		layout.verticalSpacing = 9;
		
		/* Composite for import WS Reference */
		GridData data2 = new GridData ();
		data2.horizontalAlignment = GridData.FILL;
		data2.horizontalSpan = 3;
		data2.grabExcessHorizontalSpace = true;
		
		wsRefAuthenticated = new WsReferenceComposite(container, SWT.NONE, data2);
		wsRefAuthenticated.setFilterNames(filterNames);
		wsRefAuthenticated.setFilterExtension(filterExtension);
		
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
		
		dialogChanged();
		setControl(container);
	}

	@Override
	public void comboChanged() {
		urlPath = combo.getText();
		if (!filePath.isEmpty() && urlPath.indexOf(filePath) == -1) {
			editor.setStringValue(null);
		}
		dialogChanged();
	}

	@Override
	public void editorChanged() {
		IPath path = new Path(editor.getStringValue());
		filePath = path.toString();
		File f = new File(filePath);
		if(f.exists()) {
			try {
				addToCombo(FileUtils.toUriString(f));
			} catch (MalformedURLException e) {}
		} else {
			editor.setStringValue(null);
			setTextStatus("Please select an existing file");
		}
	}
	
	private void addToCombo(String uriFile) {
		if(!Arrays.asList(combo.getItems()).contains(uriFile)){
			combo.add(uriFile);
			combo.select(combo.getItemCount()-1);
		} else {
			combo.select(wsRefAuthenticated.getItem(uriFile));
		}
	}
	
	public void dialogChanged() {
		String message = null;
		
		if (!urlPath.isEmpty()) {
			try {
				URL url = new URL(urlPath);
				
				if (urlPath.startsWith("file:/")) {
					if (new File(url.getPath()).exists()) {
						String[] filterExtensions = wsRefAuthenticated.getFilterExtension();//wsRefAuthenticated.getFilterExtension()[0].split(";");
						for (String fileFilter: filterExtensions) {
							String fileExtension = fileFilter.substring(fileFilter.lastIndexOf("."));
							if (!urlPath.endsWith(fileExtension)) {
								message = "Please select a compatible file";
							}
							else {
								message = null;
								break;
							}
						}
					}
					else {
						message = "Please select an existing file";
					}
				}
			}
			catch (Exception e) {
				message = "Please enter a valid URL";
			}
		}
		else if (!filePath.equals("")) {
			File file = new File(filePath);
			if (!file.exists()) {
				message = "Please select an existing file";
			}
			else {
				String[] filterExtensions = getFilterExtension();//filterExtension[0].split(";");
				for (String fileFilter: filterExtensions) {
					String fileExtension = fileFilter.substring(fileFilter.lastIndexOf("."));
					if (!filePath.endsWith(fileExtension)) {
						message = "Please select a compatible file";
					}
					else {
						message = null;
						break;
					}
				}
			}
		}
		else {
			message = "Please enter an URL!";
		}
		
		if (message == null) {
			if (useAuthentication.getSelection() && 
					(loginText.getText().equals("") || passwordText.getText().equals("")) ) {
				message = "Please enter login and password";
			} 
		}
		
		if (message == null) {
			try {
				RemoteFileReference reference = (RemoteFileReference)getDbo();
				
				String localPath = "";
				if (urlPath.startsWith("file:/")) {
					localPath = getLocalFilePath(urlPath.substring("file:/".length()));
				}
				
				if (reference instanceof WebServiceReference || reference instanceof RestServiceReference) {
					reference.setUrlpath(urlPath);
					reference.setFilepath("");
				}
				else {
					reference.setUrlpath(localPath.isEmpty() ? urlPath : "");
					reference.setFilepath(localPath.isEmpty() ? "" : localPath);
				}
				
				if (useAuthentication.getSelection()) {
					reference.setNeedAuthentication(true);
					reference.setAuthUser(loginText.getText());
					reference.setAuthPassword(passwordText.getText());
				}
			} catch (Exception e) {
				message = e.getMessage();
			}
		}
		
		setTextStatus(message);
	}

	protected String getLocalFilePath(String path) {
		if (path != null && !path.isEmpty()) {
			try {
				String canonicalPath = new File(path).getCanonicalPath();
				String projectPath = (new File(Engine.PROJECTS_PATH +"/"+ getProjectName())).getCanonicalPath();
				String workspacePath = (new File(Engine.USER_WORKSPACE_PATH)).getCanonicalPath();
				boolean isLocal = canonicalPath.startsWith(projectPath) || canonicalPath.startsWith(workspacePath);
				if (isLocal) {
					if (canonicalPath.startsWith(projectPath)) {
						canonicalPath = "./" + canonicalPath.substring(projectPath.length());
					} else if (canonicalPath.startsWith(workspacePath)) {
						canonicalPath = "." + canonicalPath.substring(workspacePath.length());
					}
					return canonicalPath.replaceAll("\\\\", "/");
				}
			} catch (Exception e) {}
		}
		return "";
	}
	
	protected DatabaseObject getDbo() {
		return ((ObjectExplorerWizardPage)getWizard().getPage("ObjectExplorerWizardPage")).getCreatedBean();
	}
	
	private String getProjectName() {
		if (parentObject instanceof Project) {
			return ((Project)parentObject).getName();
		}
		return null;
	}
		
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			comboChanged();
		}
	}

	@Override
	public void setTextStatus(String message) {	
		if (!urlPath.isEmpty() && wsRefAuthenticated.isValidURL()) {
			setErrorMessage(message);
			setPageComplete(message==null);
		} else {
			setErrorMessage("Please enter a valid URL");
			setPageComplete(false);
		}
	}
	@Override
	public void performHelp() {
		getPreviousPage().performHelp();
	}

	public IWizardPage getNextPage() {
		return null;
	}
	
}
