package com.twinsoft.convertigo.eclipse.wizards.setup;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class LicensePage extends WizardPage {
	
	private Composite container;
	
	public LicensePage () {
		super("LicensePage");
		setTitle("Product license agreement");
		setDescription("You should first read and accept the Convertigo EMS license.");
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 1;
		
		Text licenseText = new Text(container, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		Font terminalFont = JFaceResources.getFont(JFaceResources.TEXT_FONT);
		licenseText.setFont(terminalFont);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 600;
		gd.heightHint = 400;
		licenseText.setLayoutData(gd);
		
		try {
			InputStream is = this.getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/wizards/setup/license.txt");
			InputStreamReader isr = new InputStreamReader(is, "utf8");
			BufferedReader br = new BufferedReader(isr);
			
			String line;
			String sLicense = "";
			while ((line = br.readLine()) != null) {
				sLicense += line + "\n";
			}
			
			licenseText.setText(sLicense);
		} catch (Exception e) {
			licenseText.setText("Unable to get the license text!\n" + e.getMessage());
		}
	
		Button acceptLicense = new Button(container, SWT.CHECK);
		acceptLicense.setText("Accept license");
		acceptLicense.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				setPageComplete(((Button) e.widget).getSelection());				
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		Label acceptation = new Label(container, SWT.NONE);
		acceptation.setText("BY INDICATING YOUR ACCEPTANCE BY CLICKING “YES” BELOW, OR INSTALLING OR USING THE SOFTWARE, YOU ARE AGREEING TO BE BOUND BY THE TERMS OF THIS AGREEMENT.");
		
		setControl(container);
		setPageComplete(false);
	}

	@Override
	public IWizardPage getNextPage() {
		SetupWizard setupWizard = (SetupWizard) getWizard();
		((SummaryPage) setupWizard.getPage("SummaryPage")).updateSummary();
		return super.getNextPage();
	}
	
	@Override
	public IWizardPage getPreviousPage() {
		SetupWizard setupWizard = (SetupWizard) getWizard();
		((SummaryPage) setupWizard.getPage("SummaryPage")).updateSummary();
		return super.getPreviousPage();
	}
}
