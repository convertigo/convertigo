package com.twinsoft.convertigo.eclipse.wizards.setup;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

public class AlreadyPscKeyPage extends WizardPage {
	private boolean alreadyPsc = false;
	private boolean anonPsc = false;
	
	public AlreadyPscKeyPage() {
		super("AlreadyPscKeyPage");
		setTitle("Personal Studio Configuration");
		setDescription("Already own a PSC?");
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, true));
		
		Label description = new Label(container, SWT.WRAP);
		description.setText("A Personal Studio Configuration (PSC) is required to start Convertigo Studio.\n" +
				"A PSC automatically configures your Studio for project deployments on Convertigo Cloud.\n\n" +
				"Note that previous Convertigo \"personal registration certificates\" are also valid PSCs.\n\n");
		description.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		final Label[] areYouSure = {null};
		
		SelectionListener choiceDone = new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				alreadyPsc = e.widget.getData("PSC") != null;
				anonPsc = e.widget.getData("ANON") != null;
				areYouSure[0].setVisible(anonPsc);
				setPageComplete(true);				
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			
		};
		
		Button choice = new Button(container, SWT.RADIO);
		choice.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		choice.addSelectionListener(choiceDone);
		choice.setText("I already have a PSC");
		choice.setData("PSC", "");
		
		choice = new Button(container, SWT.RADIO);
		choice.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		choice.addSelectionListener(choiceDone);
		choice.setText("I do not have a PSC and I want to register now");
		
		choice = new Button(container, SWT.RADIO);
		choice.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		choice.addSelectionListener(choiceDone);
		choice.setText("I do not have a PSC and I will register later (select Convertigo menu, then Configure Studio to run this wizard again)");
		choice.setData("ANON", "");
		
		Link details = new Link(container, SWT.WRAP);
		details.setText(
				"\nIncluded with Convertigo Community Edition, you get access to a free « Convertigo Cloud » account. This cloud is called  <a href=\"http://trial.convertigo.net\">http://trial.convertigo.net</a> and you will be able to deploy your projects on it for free.\n\n" +
				"Choose the \"I do not have a PSC\" option and fill in the creation form in the next page. This form creates automatically the Trial Cloud account as well as a free Convertigo Support Forum account and sends you a PSC by email.\n\n" +
				"You can access the support forum by clicking this link: <a href=\"http://www.convertigo.com/en/how-to/developer-forum.html\">http://www.convertigo.com/en/how-to/developer-forum.html</a>.\n\n" +
				"Choosing not to register will prevent you from getting access to the cloud and the support forum.\n");
		details.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		details.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				org.eclipse.swt.program.Program.launch(e.text);
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {	
			}
			
		});
		
		areYouSure[0] = new Label(container, SWT.WRAP);
		areYouSure[0].setText("Are you sure do not want to register to get your PSC ?");
		areYouSure[0].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		areYouSure[0].setVisible(false);
		
		setControl(container);
		setPageComplete(false);
	}
	
	@Override
	public IWizardPage getNextPage() {
		WizardPage wizardPage = (WizardPage) getWizard().getPage("RegistrationPage");
		if (alreadyPsc || anonPsc) {
			wizardPage.setPageComplete(true);
			wizardPage = (WizardPage) wizardPage.getNextPage();
			if (anonPsc && wizardPage instanceof PscKeyPage) {
				((PscKeyPage) wizardPage).setAnonymousCertificateKey();
				wizardPage = (WizardPage) wizardPage.getNextPage();
			}
		}
		return wizardPage;
	}
}