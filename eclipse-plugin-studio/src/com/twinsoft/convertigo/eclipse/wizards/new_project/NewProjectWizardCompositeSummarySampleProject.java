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

package com.twinsoft.convertigo.eclipse.wizards.new_project;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

class NewProjectWizardCompositeSummarySampleProject extends Composite  {

	private Label label1 = null;

	NewProjectWizardCompositeSummarySampleProject(Composite parent, int style) {
		super(parent, style);
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
		GridData gridData2 = new org.eclipse.swt.layout.GridData();
		gridData2.horizontalSpan = 2;
		gridData2.grabExcessHorizontalSpace = true;
		gridData2.horizontalAlignment = GridData.FILL;
		String text = "You chose to open a sample project.\n\n";
		text += "Click \"finish\" to create the project.\n";
		label1 = new Label(this, SWT.NONE);
		label1.setText(text);
		label1.setLayoutData(gridData2);
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.grabExcessHorizontalSpace = false;
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		gridData.grabExcessVerticalSpace = false;
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		this.setLayout(gridLayout);
		this.setSize(new org.eclipse.swt.graphics.Point(403, 177));
	}

} // @jve:decl-index=0:visual-constraint="10,10"
