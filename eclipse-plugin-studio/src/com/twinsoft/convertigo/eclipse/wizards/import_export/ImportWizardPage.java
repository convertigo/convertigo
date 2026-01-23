/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.wizards.import_export;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.twinsoft.convertigo.eclipse.swt.ProjectReferenceComposite;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;


class ImportWizardPage extends WizardPage {
	private ProjectFileFieldEditor editor = null;
	private String filePath = "";
	private ProjectReferenceComposite projectReferenceComposite;
	
	public ImportWizardPage() {
		super("Import","Import a Convertigo project",null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite page = new Composite(parent, SWT.NONE);
		page.setLayout(new GridLayout(2, false));

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		
		Composite fileSelectionArea = new Composite(page, SWT.NONE);
		fileSelectionArea.setLayoutData(gd);
		
		editor = new ProjectFileFieldEditor("fileSelect","Select File: ", fileSelectionArea);
		editor.getTextControl(fileSelectionArea).addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				IPath path = new Path(ImportWizardPage.this.editor.getStringValue());
				filePath = path.toString();
				updateStatus();
			}
		});
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		new Label(page, SWT.HORIZONTAL | SWT.SEPARATOR).setLayoutData(gd);
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		Label label = new Label(page , SWT.NONE);
		label.setLayoutData(gd);
		label.setText("Project can also be imported by a \"Project remote URL\":");
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		projectReferenceComposite = new ProjectReferenceComposite(page, SWT.NONE, new ProjectUrlParser(""), () -> updateStatus());
		projectReferenceComposite.setLayoutData(gd);
				
		updateStatus();
		setControl(page);
	}

	private void updateStatus() {
		String message = null;
		ProjectUrlParser parser = getParser();
		if (parser != null) {
			if (StringUtils.isEmpty(parser.getProjectUrl())) {
				if (filePath.equals("")) {
					message = "Please select a file";
				} else if (!Engine.isProjectFile(filePath) && !filePath.endsWith(".car") && !filePath.endsWith(".zip")) {
					message = "Please select a compatible file extension";
				} else if (!new File(filePath).exists()) {
					message = "Please select an existing compatible file";
				} else {
					try {
						String projectName = DatabaseObjectsManager.getProjectName(new File(filePath));
						if (StringUtils.isNotBlank(projectName)) {
							setMessage("Current project to import is '" + projectName + "'.");
						}
					} catch (Exception e) {
					}
				}
			}
		}
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	
	protected String getFilePath() {
		return filePath;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#getNextPage()
	 */
	@Override
	public IWizardPage getNextPage() {
		if (getParser().isValid()) return null;
		if (Engine.isProjectFile(filePath)) return null;
		return super.getNextPage();
	}
	
	public ProjectUrlParser getParser() {
		if (projectReferenceComposite != null) {
			return projectReferenceComposite.getParser();
		}
		return null;
	}
	
}
