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

public class NewProjectWizardComposite5 extends Composite {

	private Label label = null;
	private Text ctgName = null;
	private Label label1 = null;
	private ModifyListener modifyListener;
	private Text ctgServer = null;
	private Label label5 = null;
	private Label label2 = null;
	private Text ctgPort = null;
	public NewProjectWizardComposite5(Composite parent, int style, ModifyListener ml, NewProjectWizard wizard) {
		super(parent, style);
		modifyListener = ml;
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
        GridData gridData11 = new org.eclipse.swt.layout.GridData();
        gridData11.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
        gridData11.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
        GridData gridData2 = new org.eclipse.swt.layout.GridData();
        gridData2.horizontalSpan = 2;
        label1 = new Label(this, SWT.NONE);
        label1.setText("The chosen project template includes a CICS type connector. \n\nThis connector needs a CICS Transaction Gateway (CTG) name, server address and server port.\n\nPlease provide this information here\n\n");
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
        label.setText("CTG Configuration name");
        label.setLayoutData(gridData);
        ctgName = new Text(this, SWT.BORDER);
        ctgName.setLayoutData(gridData1);
        label2 = new Label(this, SWT.NONE);
        label2.setText("CTG Sever address");
        ctgServer = new Text(this, SWT.BORDER);
        ctgServer.setLayoutData(gridData11);
        label5 = new Label(this, SWT.NONE);
        label5.setText("CTG Server port");
        ctgPort = new Text(this, SWT.BORDER);
        ctgName.addModifyListener(modifyListener);
        ctgPort.addModifyListener(modifyListener);
        ctgServer.addModifyListener(modifyListener);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        this.setLayout(gridLayout);
	}

	public ModifyListener getModifyListener() {
		return modifyListener;
	}

	public void setModifyListener(ModifyListener modifyListener) {
		this.modifyListener = modifyListener;
	}

	public Text getCtgName() {
		return ctgName;
	}

	public void setCtgName(Text ctgName) {
		this.ctgName = ctgName;
	}

	public Text getCtgPort() {
		return ctgPort;
	}

	public void setCtgPort(Text ctgPort) {
		this.ctgPort = ctgPort;
	}

	public Text getCtgServer() {
		return ctgServer;
	}

	public void setCtgServer(Text ctgServer) {
		this.ctgServer = ctgServer;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
