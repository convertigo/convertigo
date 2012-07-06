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
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NewProjectWizardComposite6 extends Composite {

	private Text  httpServer = null;
	private Label label1 = null;
	private ModifyListener modifyListener;
	private SelectionListener selectionListener;
	private Text  httpPort = null;
	private Button ssl = null;
	private Group group = null;
	private Label label = null;
	private Label label2 = null;
	private Label label3 = null;
	
	public NewProjectWizardComposite6(Composite parent, int style, ModifyListener ml, SelectionListener sl, NewProjectWizard wizard) {
		super(parent, style);
		modifyListener = ml;
		selectionListener = sl;
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
		GridData gridData2 = new GridData();
        gridData2.horizontalSpan = 2;
        label1 = new Label(this, SWT.NONE);
        label1.setText("The chosen project template includes a HTTP type connector. \n\nThis connector needs a HTTP server address and a HTTP server port (Default is 80). \nCheck the SSL box is you need an SSLv3 connection to the targer server. \nIf you need a proxy to acces the target server, please configure the proxy in the convertigo's engine preferences.\n\nPlease provide this information here\n\n");
        label1.setLayoutData(gridData2);
        createGroup();
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

	public Text getHttpPort() {
		return httpPort;
	}

	public void setHttpPort(Text httpPort) {
		this.httpPort = httpPort;
	}

	public Text getHttpServer() {
		return httpServer;
	}

	public void setHttpServer(Text httpServer) {
		this.httpServer = httpServer;
	}
	
	public Button getSsl() {
		return ssl;
	}

	/**
	 * This method initializes group	
	 *
	 */
	private void createGroup() {
		GridData gridData1 = new GridData();
		gridData1.horizontalAlignment = GridData.BEGINNING;
		gridData1.grabExcessHorizontalSpace = true;
		gridData1.verticalAlignment = GridData.CENTER;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.CENTER;
		GridLayout gridLayout1 = new GridLayout();
		gridLayout1.numColumns = 2;
		GridData gridData3 = new GridData();
		gridData3.horizontalAlignment = GridData.FILL;
		gridData3.horizontalSpan = 2;
		gridData3.grabExcessHorizontalSpace = true;
		gridData3.grabExcessVerticalSpace = false;
		gridData3.verticalAlignment = GridData.FILL;
		group = new Group(this, SWT.NONE);
		group.setText("Target Server");
		group.setLayoutData(gridData3);
		group.setLayout(gridLayout1);
		label = new Label(group, SWT.NONE);
		label.setText("HTTP Server");
		httpServer = new Text(group, SWT.BORDER);
		httpServer.setLayoutData(gridData);
		
		label2 = new Label(group, SWT.NONE);
		label2.setText("HTTP Port");
		httpPort = new Text(group, SWT.BORDER);
		httpPort.setLayoutData(gridData1);
		label3 = new Label(group, SWT.NONE);
		label3.setText("SSL");
		ssl = new Button(group, SWT.CHECK);
		
		httpServer.addModifyListener(modifyListener);
        httpPort.addModifyListener(modifyListener);
        ssl.addSelectionListener(selectionListener);
	}
}  //  @jve:decl-index=0:visual-constraint="10,10"
