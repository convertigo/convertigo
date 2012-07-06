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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NewProjectWizardComposite2 extends Composite {

	private Label label = null;
	private Text connectorName = null;
	private Label label1 = null;
	private ModifyListener modifyListener;
	private NewProjectWizard wz;
	
	public NewProjectWizardComposite2(Composite parent, int style, ModifyListener ml, NewProjectWizard wizard) {
		super(parent, style);
		modifyListener = ml;
		wz = wizard;
		initialize();
		((NewProjectWizardComposite1) wz.page1.getControl()).getProjectName().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				connectorName.setText(wz.page1.getProjectName() + "Connector");
			}
		});
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
        GridData gridData2 = new org.eclipse.swt.layout.GridData();
        gridData2.horizontalSpan = 3;
        label1 = new Label(this, SWT.NONE);
        label1.setText("A connector establishes the connection between a data source and Convertigo.\nThe connector is used by Convertigo to collect the data that will be formatted as an XML document.\n\nYou will be able later on to add new connectors to your project.\n\nPlease choose a name for this connector.\n");
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
        label.setText("Connector name");
        label.setLayoutData(gridData);
        connectorName = new Text(this, SWT.BORDER);
        connectorName.setLayoutData(gridData1);
        connectorName.addModifyListener(modifyListener);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        this.setLayout(gridLayout);
	}

	public Text getConnectorName() {
		return connectorName;
	}

	public void setConnectorName(Text connectorName) {
		this.connectorName = connectorName;
	}

	public ModifyListener getModifyListener() {
		return modifyListener;
	}

	public void setModifyListener(ModifyListener modifyListener) {
		this.modifyListener = modifyListener;
	}

	
}  //  @jve:decl-index=0:visual-constraint="10,10"
