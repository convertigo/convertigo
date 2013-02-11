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

package com.twinsoft.convertigo.eclipse.wizards.import_export;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.engine.Engine;

public class ExportWizardPage extends WizardPage {
	
	private IStructuredSelection selection = null;
	protected ProjectFileFieldEditor editor = null;
	protected String filePath = "";
	
	public ExportWizardPage(IStructuredSelection selection) {
		super("Export","Export a Convertigo project",null);
		this.selection = selection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite fileSelectionArea = new Composite(parent, SWT.NONE);
		GridData fileSelectionData = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.FILL_HORIZONTAL);
		fileSelectionArea.setLayoutData(fileSelectionData);

		GridLayout fileSelectionLayout = new GridLayout();
		fileSelectionLayout.numColumns = 3;
		fileSelectionLayout.makeColumnsEqualWidth = false;
		fileSelectionLayout.marginWidth = 0;
		fileSelectionLayout.marginHeight = 0;
		fileSelectionArea.setLayout(fileSelectionLayout);
		
		String projectName = getSelectedProject();
		projectName = (projectName == null) ? "":Engine.PROJECTS_PATH + "/" + projectName + ".car";
		editor = new ProjectFileFieldEditor("fileSelect","Select File: ",fileSelectionArea);
		editor.getTextControl(fileSelectionArea).addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				IPath path = new Path(ExportWizardPage.this.editor.getStringValue());
				filePath = path.toString();
				updateStatus();
			}
		});
		editor.getTextControl(fileSelectionArea).setText(projectName);
		fileSelectionArea.moveAbove(null);
		updateStatus();
		setControl(fileSelectionArea);		
	}

	private String getSelectedProject() {
		if (!selection.isEmpty() && (selection instanceof TreeSelection)) {
			Object object = ((TreeSelection)selection).getFirstElement();
			if ((object instanceof ProjectTreeObject) || (object instanceof UnloadedProjectTreeObject)) {
				return ((TreeObject)object).getName();
			}
		}
		return null;
	}
	
	private void updateStatus() {
		String projectName = getSelectedProject();
		String message = null;
		if (projectName == null) {
			message = "A Convertigo project should be selected in \"Projects\" tree view";
		}
		else {
			if (filePath.equals(""))
				message = "Please select a file";
			else if (!filePath.endsWith(".xml") && !filePath.endsWith(".car"))
				message = "Please select a compatible file extension";
			else if ((projectName != null) && (filePath.indexOf("/" + projectName) == -1))
				message = "Please select a compatible file name";
		}
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	
	protected String getFilePath() {
		return filePath;
	}
}
