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

package com.twinsoft.convertigo.eclipse.wizards;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

public class NewProjectWizardComposite10 extends Composite {
	private ModifyListener modifyListener;
	public ProgressBar progressBar = null;
	public Label labelProgression = null;
	public Combo combo = null;
	
	public NewProjectWizardComposite10(Composite parent, int style, ModifyListener ml, IWizard wizard) {
		super(parent, style);
		modifyListener = ml;
		initialize();
	}

	protected void initialize() {
		Label description = new Label(this, SWT.NONE);
		description.setText("Please enter a valid WSDL url:");
		GridData data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		description.setLayoutData (data);
		
		combo = new Combo(this, SWT.NONE);
		combo.add("http://www.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL");
		combo.add("http://aspalliance.com/quickstart/aspplus/samples/services/MathService/VB/MathService.asmx?wsdl");
		combo.add("http://demo.convertigo.net/cems/projects/globalCompany_HR_WS/.wsl?wsdl");
		combo.add("http://demo.convertigo.net/cems/projects/globalCompany_accounting_WS/.wsl?wsdl");
		combo.select(0);
		if (modifyListener != null)
			combo.addModifyListener(modifyListener);
		GridData data0 = new GridData ();
		data0.horizontalAlignment = GridData.FILL;
		data0.grabExcessHorizontalSpace = true;
		combo.setLayoutData (data0);

		GridLayout gridLayout = new GridLayout();
		this.setLayout(gridLayout);
		//setSize(new Point(402, 99));
	}
}
