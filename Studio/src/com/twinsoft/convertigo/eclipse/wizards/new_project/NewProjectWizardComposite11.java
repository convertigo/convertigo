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
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NewProjectWizardComposite11 extends Composite {
	private ModifyListener modifyListener;
	private SelectionListener selectionListener;
	public Text targetUrl = null;
	public Button trustCertificates = null;
	
	public NewProjectWizardComposite11(Composite parent, int style, ModifyListener ml, SelectionListener sl, IWizard wizard) {
		super(parent, style);
		modifyListener = ml;
		selectionListener = sl;
		initialize();
	}

	protected void initialize() {
		Label urlDescription = new Label(this, SWT.NONE);
		urlDescription.setText("Please enter a valid target host url :");
		GridData data0 = new GridData ();
		data0.horizontalAlignment = GridData.FILL;
		data0.grabExcessHorizontalSpace = true;
		data0.horizontalSpan = 2;
		urlDescription.setLayoutData (data0);
		
		targetUrl = new Text(this, SWT.NONE);
		targetUrl.setText("http://www.convertigo.com");
		GridData data1 = new GridData ();
		data1.horizontalAlignment = GridData.FILL;
		data1.grabExcessHorizontalSpace = true;
		data1.horizontalSpan = 2;
		targetUrl.setLayoutData (data1);

		GridData gridData4 = new GridData();
		gridData4.verticalIndent = 5;
		gridData4.grabExcessHorizontalSpace = false;
		
		Label label6 = new Label(this, SWT.NONE);
		label6.setText("Trust all certificates");
		label6.setLayoutData(gridData4);
		
		GridData gridData5 = new GridData();
		gridData5.verticalIndent = 5;
		gridData5.horizontalIndent = 2;

		trustCertificates = new Button(this, SWT.CHECK);
		trustCertificates.setLayoutData(gridData5);

		if (modifyListener != null) {
			targetUrl.addModifyListener(modifyListener);
			trustCertificates.addSelectionListener(selectionListener);
		}
		
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		this.setLayout(gridLayout);
		//setSize(new Point(402, 99));
	}
}
