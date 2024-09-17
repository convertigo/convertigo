/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.wizards.setup;

import org.apache.commons.io.IOUtils;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.swt.SwtUtils.SelectionListener;
import com.twinsoft.convertigo.eclipse.wizards.setup.SetupWizard.SummaryGenerator;

class LicensePage extends WizardPage implements SummaryGenerator {
	
	private Composite container;
	
	public LicensePage () {
		super("LicensePage");
		setTitle("Product license agreement");
		setDescription("You should first read and accept the Convertigo EMS license.");
	}
	
	@Override
	public IWizard getWizard() {
		SetupWizard wizard = (SetupWizard) super.getWizard();
		wizard.postRegisterState(this.getClass().getSimpleName().toLowerCase());
		return wizard;
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, true));
		
		Text licenseText = new Text(container, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		licenseText.setEditable(false);
		Font terminalFont = JFaceResources.getFont(JFaceResources.TEXT_FONT);
		licenseText.setFont(terminalFont);
		GridData gd = new GridData(GridData.FILL_BOTH);
		licenseText.setLayoutData(gd);
		
		try {			
			licenseText.setText(IOUtils.toString(this.getClass().getResourceAsStream("license.txt"), "utf8"));
		} catch (Exception e) {
			licenseText.setText("Unable to get the license text!\n" + e.getMessage());
		}
	
		Label acceptation = new Label(container, SWT.WRAP);
		acceptation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		acceptation.setText("BY INDICATING YOUR ACCEPTANCE BY CLICKING “Accept license” BELOW, "
				+ "OR INSTALLING OR USING THE SOFTWARE, YOU ARE AGREEING TO BE BOUND "
				+ "BY THE TERMS OF THIS AGREEMENT.");
		
		Button acceptLicense = new Button(container, SWT.CHECK);
		acceptLicense.setText("Accept license");
		acceptLicense.addSelectionListener((SelectionListener) e -> {
			setPageComplete(((Button) e.widget).getSelection());
		});
		
		setControl(container);
		setPageComplete(false);
	}

	public String getSummary() {
		return "License:\n" +
				"\taccepted\n";
	}
}