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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NewProjectWizardComposite1 extends Composite {

	private Label label = null;
	private Text projectName = null;
	private Label label1 = null;
	private ModifyListener modifyListener;
	
	public NewProjectWizardComposite1(Composite parent, int style, ModifyListener ml) {
		super(parent, style);
		modifyListener = ml;
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
        GridData gridData2 = new org.eclipse.swt.layout.GridData();
        gridData2.horizontalSpan = 3;
        label1 = new Label(this, SWT.NONE);
        label1.setText("Please use a relevant project name. Avoid the use of special characters  (âàéèêù...)\nand punctuation characters as space, pound or others. We suggest you use only lowercase letters. \nIf you use uppercase letters, be sure use the same letter case when you will call\ntransactions using the convertigo's url interface.\n\nThe project name also defines the Convertigo's physical and virtual directories:\n\n- All your project's ressources will be held in the <your_workspace>/convertigo/projects/<your_project_name> directory.\n- Your project's URL will be http://<server_name>:<port>/convertigo/projects/<your_project_name>\n\n");
        label1.setLayoutData(gridData2);
        GridData gridData1 = new org.eclipse.swt.layout.GridData();
        gridData1.grabExcessHorizontalSpace = true;
        gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
        gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
        GridData gridData = new org.eclipse.swt.layout.GridData();
        gridData.grabExcessHorizontalSpace = false;
        gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
        gridData.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
        gridData.grabExcessVerticalSpace = false;
        label = new Label(this, SWT.NONE);
        label.setText("Project's name");
        label.setLayoutData(gridData);
        projectName = new Text(this, SWT.BORDER);
        projectName.setLayoutData(gridData1);
        projectName.addModifyListener(modifyListener);
        projectName.setFocus();
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        this.setLayout(gridLayout);
        this.setSize(new org.eclipse.swt.graphics.Point(514,264));
			
	}

	public Text getProjectName() {
		return projectName;
	}

	public void setProjectName(Text projectName) {
		this.projectName = projectName;
	}

	public ModifyListener getModifyListener() {
		return modifyListener;
	}

	public void setModifyListener(ModifyListener modifyListener) {
		this.modifyListener = modifyListener;
	}

	
}  //  @jve:decl-index=0:visual-constraint="10,10"
