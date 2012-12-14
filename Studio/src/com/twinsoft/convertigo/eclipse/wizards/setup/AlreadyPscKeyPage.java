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

public class AlreadyPscKeyPage extends WizardPage {
	private boolean alreadyPsc = false;
	
	public AlreadyPscKeyPage() {
		super("AlreadyPscKeyPage");
		setTitle("Personal Studio Configuration");
		setDescription("Already own a PSC ?");
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, true));
		
		Label description = new Label(container, SWT.WRAP);
		description.setText("A Personal Studio Configuration is required to start Convertigo Studio.\n" +
				"A PSC automatically configures your Studio for project deployments on Convertigo Cloud.\n\n" +
				"Note that previous Convertigo “personal registration certificates” are also valid PSCs.\n\n");
		description.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		SelectionListener choiceDone = new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				alreadyPsc = e.widget.getData("PSC") != null;
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
		choice.setText("I do not have a PSC");
		
		setControl(container);
		setPageComplete(false);
	}
	
	@Override
	public IWizardPage getNextPage() {
		RegistrationPage registrationPage = (RegistrationPage) getWizard().getPage("RegistrationPage");
		if (alreadyPsc) {
			registrationPage.ignore();
			return registrationPage.getNextPage();
		}
		return registrationPage;
	}
}