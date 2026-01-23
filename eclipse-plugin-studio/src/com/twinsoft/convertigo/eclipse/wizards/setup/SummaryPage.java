/*
 * Copyright (c) 2001-2026 Convertigo SA.
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
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.swt.SwtUtils.SelectionListener;
import com.twinsoft.convertigo.eclipse.wizards.setup.SetupWizard.SummaryGenerator;

class SummaryPage extends WizardPage {
	
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
		
		Link details2 = new Link(container, SWT.WRAP);
		details2.setText("For support:\n\n" +
				"- Here are all the resources you may need: <a href=\"https://www.convertigo.com/convertigo-startup-page-8-3\">https://www.convertigo.com/convertigo-startup-page-8-3</a>\n\n" +
				"- The Convertigo Community Support: <a href=\"https://convertigo.atlassian.net/wiki/spaces/CK/overview\">https://convertigo.atlassian.net/wiki/spaces/CK/overview</a>\n\n" +
				"- Get a free 30-minute \"onboarding\" session with one of our engineers.\n" +
				"The session schedule link will be included in the email containing your PSC."
		);
		details2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		SelectionListener goToTheLink = e -> {
			org.eclipse.swt.program.Program.launch(e.text);
		};
		
		details2.addSelectionListener(goToTheLink);

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
		var wd = (WizardDialog) getContainer();
		var bar = (Composite) ((Composite) wd.buttonBar).getChildren()[0];
		bar.getChildren()[0].setVisible(false);
		return wizard;
	}
}
