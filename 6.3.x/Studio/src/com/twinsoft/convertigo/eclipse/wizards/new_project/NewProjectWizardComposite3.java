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

public class NewProjectWizardComposite3 extends Composite {

	private Label label = null;
	private Text server = null;
	private Label label1 = null;
	private ModifyListener modifyListener;
	private Text port = null;
	private Label label5 = null;
	public NewProjectWizardComposite3(Composite parent, int style, ModifyListener ml, NewProjectWizard wizard) {
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
        gridData2.horizontalSpan = 2;
        label1 = new Label(this, SWT.NONE);
        label1.setText("The chosen project template includes a ''screen type'' connector. \n\nThis connector needs a destination address as an hostname (or IP adress) and optionally a port.\n ");
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
        label.setText("Server address");
        label.setLayoutData(gridData);
        server = new Text(this, SWT.BORDER);
        server.setLayoutData(gridData1);
        label5 = new Label(this, SWT.NONE);
        label5.setText("Port");
        port = new Text(this, SWT.BORDER);
        server.addModifyListener(modifyListener);
        port.addModifyListener(modifyListener);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        this.setLayout(gridLayout);
	}

	public Text getServer() {
		return server;
	}

	public ModifyListener getModifyListener() {
		return modifyListener;
	}

	public void setModifyListener(ModifyListener modifyListener) {
		this.modifyListener = modifyListener;
	}

	public Text getPort() {
		return port;
	}

	public void setPort(Text port) {
		this.port = port;
	}

	public void setServer(Text server) {
		this.server = server;
	}
}  //  @jve:decl-index=0:visual-constraint="10,10"
