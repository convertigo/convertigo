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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.wizards.util.FileFieldEditor;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.FileUtils;

public class WsReferenceImportDialogComposite extends MyAbstractDialogComposite {

	private FileFieldEditor editor = null;
	private Combo combo = null;
	private String filePath = "";
	private WsReferenceComposite wsRefAuthenticated = null;
	private Object parentObject = null;
	
	public Button useAuthentication = null;
	public Text loginText = null, passwordText = null;
	public ProgressBar progressBar = null;
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
		
		wsRefAuthenticated = new WsReferenceComposite(this, SWT.NONE, data1);
		
		combo = wsRefAuthenticated.getCombo();
		
		editor = wsRefAuthenticated.getEditor();
		Composite fileSelectionArea = wsRefAuthenticated.getFileSelectionArea();
		editor.getTextControl(fileSelectionArea).addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				IPath path = new Path(WsReferenceImportDialogComposite.this.editor.getStringValue());
				filePath = path.toString();
				dialogChanged();
			}
		});
		
		useAuthentication = wsRefAuthenticated.getUseAuthentication();
		loginText = wsRefAuthenticated.getLoginText();
		passwordText = wsRefAuthenticated.getPasswordText();
		
		progressBar = new ProgressBar(this, SWT.NONE);	
		progressBar.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true, 2, 1));
	}

	@Override
	public Object getValue(String name) {
		return null;
	}

	private void dialogChanged() {
		if (!filePath.equals("")) {
			File file = new File(filePath);
			if (file.exists()) {
				String[] filterExtensions = wsRefAuthenticated.getFilterExtension()[0].split(";");
				for (String fileFilter: filterExtensions) {
					String fileExtension = fileFilter.substring(fileFilter.lastIndexOf("."));
					if (filePath.endsWith(fileExtension)) {
						try {
							String xsdFilePath = new File(filePath).getCanonicalPath();
							String projectPath = (new File(Engine.PROJECTS_PATH +"/"+ getProjectName())).getCanonicalPath();
							String workspacePath = (new File(Engine.USER_WORKSPACE_PATH)).getCanonicalPath();
							
							boolean isExternal = !xsdFilePath.startsWith(projectPath) && !xsdFilePath.startsWith(workspacePath);
							
							if (isExternal) {
								combo.add(FileUtils.toUriString(file));
								combo.select(combo.getItemCount()-1);
							} else {
								if (xsdFilePath.startsWith(projectPath))
									xsdFilePath = "./" + xsdFilePath.substring(projectPath.length());
								else if (xsdFilePath.startsWith(workspacePath))
									xsdFilePath = "." + xsdFilePath.substring(workspacePath.length());
								xsdFilePath = xsdFilePath.replaceAll("\\\\", "/");
							}
						} catch (Exception e) {
							Engine.logBeans.error("An error occured while creating project with WS reference", e);
						}
					}
				}
			}
		} 
	}
	
	private String getProjectName() {
		if (parentObject instanceof Project) {
			return ((Project)parentObject).getName();
		}
		return "";
	}
	
	public void setParentObject(Object parentObject) {
		this.parentObject = parentObject;
	}
	
	public String getURL() {
		return combo.getText();
	}
}
