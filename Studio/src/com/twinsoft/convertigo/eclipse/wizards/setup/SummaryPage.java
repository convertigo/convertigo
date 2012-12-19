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
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 1;
		layout.marginWidth = 8;
		
		summaryText = new Text(container, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		summaryText.setEditable(false);
		summaryText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		setControl(container);
		setPageComplete(false);
	}
	
	@Override
	public IWizard getWizard() {
		SetupWizard wizard = (SetupWizard) super.getWizard();
		
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
