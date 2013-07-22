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

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class NewProjectWizardComposite8 extends Composite  {

	private NewProjectWizard wz;
	private Label label = null;
	private Label label1 = null;
	private Label label5 = null;
	private Label label3 = null;
	private Label label6 = null;
	private Label connectorName = null;
	private Label projectName = null;
	private Label targetServer = null;

	public NewProjectWizardComposite8(Composite parent, int style, IWizard wizard) {
		super(parent, style);
		wz = (NewProjectWizard)wizard;
		initialize();
		((NewProjectWizardComposite1) wz.page1.getControl()).getProjectName().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				projectName.setText(wz.page1.getProjectName());
			}
		});
		((NewProjectWizardComposite2) wz.page2.getControl()).getConnectorName().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				connectorName.setText(wz.page2.getConnectorName());
			}
		});
		
		ModifyListener ml = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateServerValues();
			}
		};
		
		SelectionListener sl = new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				updateServerValues();
			}
			public void widgetSelected(SelectionEvent e) {
				updateServerValues();
			}
		};
		
		((NewProjectWizardComposite6) wz.page6.getControl()).getHttpServer().addModifyListener(ml);
		((NewProjectWizardComposite6) wz.page6.getControl()).getHttpPort().addModifyListener(ml);
		((NewProjectWizardComposite6) wz.page6.getControl()).getSsl().addSelectionListener(sl);
		
		updateServerValues();
	}

	private void updateServerValues() {
		// HTTP server
		String svr = wz.page6.getHttpServer();
		String path = "";
		int i = svr.indexOf("/");
		if (i>0) {
			svr = wz.page6.getHttpServer().substring(0, i);
			path = wz.page6.getHttpServer().substring(i);
		}
		String tmp = "http";
		if (wz.page6.isBSSL())
			tmp += "s";
		tmp += "://" + svr + ":" + wz.page6.getHttpPort() + path;
		targetServer.setText(tmp);
	}
	
	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
		GridData gridData6 = new org.eclipse.swt.layout.GridData();
		gridData6.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData6.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridData gridData5 = new org.eclipse.swt.layout.GridData();
		gridData5.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData5.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridData gridData4 = new org.eclipse.swt.layout.GridData();
		gridData4.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData4.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridData gridData3 = new org.eclipse.swt.layout.GridData();
		gridData3.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData3.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridData gridData21 = new org.eclipse.swt.layout.GridData();
		gridData21.horizontalAlignment = org.eclipse.swt.layout.GridData.END;
		gridData21.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridData gridData1 = new org.eclipse.swt.layout.GridData();
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.END;
		gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridData gridData2 = new org.eclipse.swt.layout.GridData();
		gridData2.horizontalSpan = 2;
		gridData2.grabExcessHorizontalSpace = true;
		gridData2.horizontalAlignment = GridData.FILL;
		label1 = new Label(this, SWT.NONE);
		label1.setText("Here are all the configuration options you chose during the project setup.\nClick \"finish\" to create the project or \"back\" to review parameters.\n ");
		label1.setLayoutData(gridData2);
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.grabExcessHorizontalSpace = false;
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		gridData.grabExcessVerticalSpace = false;
		label = new Label(this, SWT.NONE);
		label.setText("Project name:");
		label.setLayoutData(gridData);
		projectName = new Label(this, SWT.NONE);
		projectName.setText("projectName");
		projectName.setLayoutData(gridData4);
		label5 = new Label(this, SWT.NONE);
		label5.setText("Connector name:");
		connectorName = new Label(this, SWT.NONE);
		connectorName.setText("connectorName");
		connectorName.setLayoutData(gridData3);
		GridData gridData7 = new org.eclipse.swt.layout.GridData();
		gridData7.horizontalSpan = 2;
		gridData7.grabExcessHorizontalSpace = true;
		gridData7.horizontalAlignment = GridData.FILL;
		label3 = new Label(this, SWT.NONE);
		label3.setText("Connector configuration");
		label3.setLayoutData(gridData7);
		label6 = new Label(this, SWT.NONE);
		label6.setText("Target server:");
		label6.setLayoutData(gridData21);
		targetServer = new Label(this, SWT.NONE);
		targetServer.setText("target server");
		targetServer.setLayoutData(gridData5);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		this.setLayout(gridLayout);
		this.setSize(new org.eclipse.swt.graphics.Point(403, 177));
	}

} // @jve:decl-index=0:visual-constraint="10,10"
