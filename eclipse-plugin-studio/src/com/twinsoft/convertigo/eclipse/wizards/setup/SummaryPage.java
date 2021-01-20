/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.wizards.setup.SetupWizard.SummaryGenerator;

public class SummaryPage extends WizardPage {
	
	private Composite container;
	
	public SummaryPage () {
		super("SummaryPage");
		setTitle("Setup summary");
		setDescription("Your Convertigo studio is going to be installed with the following parameters.");
	}
	
	private Text summaryText ;

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		
		summaryText = new Text(container, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		summaryText.setEditable(false);
		summaryText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		setControl(container);
		setPageComplete(false);
	}
	
	@Override
	public IWizard getWizard() {
		SetupWizard wizard = (SetupWizard) super.getWizard();

		wizard.postRegisterState(this.getClass().getSimpleName().toLowerCase());

		StringBuffer summary = new StringBuffer();
		
		for (IWizardPage page : wizard.getPages()) {
			if (page instanceof SummaryGenerator) {
				summary.append(((SummaryGenerator) page).getSummary() + "\n");
			}
		}
		
		summaryText.setText(summary.toString());
		
		setPageComplete(true);
		return wizard;
	}
}
