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

package com.twinsoft.convertigo.eclipse.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ProjectVersionUpdateDialogComposite extends MyAbstractDialogComposite {

	private String version = "";
	public Text text =  null;
	public Button checkBoxTestCase = null;
	
	public ProjectVersionUpdateDialogComposite(Composite parent, int style, String version) {
		super(parent, style);
		this.version = version == null ? "":version;
		
		initialize();
	}

	protected void initialize() {
		GridData labelData = new GridData ();
		labelData.horizontalAlignment = GridData.FILL;
		labelData.grabExcessHorizontalSpace = true;
		
		Label label = new Label (this, SWT.NONE);
		label.setText("You can update the version of your project before export or deployment.\n" +
				"If you wish to, please change the value below :");
		label.setLayoutData(labelData);
		
		GridData textData = new GridData ();
		textData.horizontalAlignment = GridData.FILL;
		textData.grabExcessHorizontalSpace = true;
		
		text = new Text (this, SWT.BORDER);
		text.setText (version);
		text.setLayoutData(textData);
		
		GridData checkData = new GridData ();
		checkData.horizontalAlignment = GridData.END;
		checkData.grabExcessHorizontalSpace = true;
		
		checkBoxTestCase = new Button(this, SWT.CHECK);
		checkBoxTestCase.setText("Choose test cases to export");
		checkBoxTestCase.setLayoutData(checkData);
		
		GridLayout gridLayout = new GridLayout();
		setLayout(gridLayout);
	}
	
	public Text getTextControl() {
		return text;
	}

	public Button getCheckBoxControl(){
		return checkBoxTestCase;
	}
	
	@Override
	public Object getValue(String name) {
		return version = text.getText();
	}

}
