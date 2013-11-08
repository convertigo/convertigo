package com.twinsoft.convertigo.eclipse.wizards.setup;

import java.io.IOException;
import java.util.Properties;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin.PscException;
import com.twinsoft.convertigo.eclipse.DeploymentKey;
import com.twinsoft.convertigo.eclipse.wizards.setup.SetupWizard.RegisterCallback;
import com.twinsoft.convertigo.eclipse.wizards.setup.SetupWizard.SummaryGenerator;

public class PscKeyPage extends WizardPage implements RegisterCallback, SummaryGenerator {
	
	private Composite container;
	
	private Text pscKey;
	private Link infoLink;
	private Properties decodedPSC;
	
	public PscKeyPage () {
		super("PscKeyPage");
		setTitle("Personal Studio Configuration");
		setDescription("Paste your PSC...");
	}

	@Override
	public IWizard getWizard() {
		IWizardPage previousPage = getPreviousPage();
		
		setErrorMessage(null);
		setMessage(getDescription());

		SetupWizard wizard = (SetupWizard) super.getWizard();
		wizard.postRegisterState(this.getClass().getSimpleName().toLowerCase());

		if (previousPage instanceof RegistrationPage) {
			RegistrationPage registrationPage = (RegistrationPage) previousPage;
			if (registrationPage.isConnected()) {
				if (registrationPage.register(this)) {
					infoLink.setText("Online registration in progress, please wait…");
				}
			} else {
				infoLink.setText("Studio offline. Please register manually on " + RegistrationPage.registrationLink + " and paste your PSC here.");
				setErrorMessage("Studio offline! Check your proxy settings");
			}
		} else {
			infoLink.setText("Please paste your previous PSC here and click the 'Next >' button...");
		}
		infoLink.getParent().layout();
		return super.getWizard();
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 30;
		container.setLayout(layout);
		
		GridData layoutDataText = new GridData(GridData.FILL_HORIZONTAL);
		layoutDataText.verticalIndent = 5;
		
		Label label = new Label (container, SWT.NONE);
		
		FontData fontDefaultData = label.getFont().getFontData()[0];
		fontDefaultData.setStyle(SWT.BOLD);
		fontDefaultData.setHeight(fontDefaultData.getHeight() * 2);
		
		label.setFont(new Font(parent.getDisplay(), fontDefaultData));
		Color color = new  Color(container.getDisplay(), 51,102,255);
		label.setForeground(color);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		label.setText("PSC");
				
		infoLink = new Link(container, SWT.WRAP);
		infoLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				
		pscKey = new Text(container, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
		pscKey.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
		pscKey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite buttons = new Composite(container, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		buttons.setLayout(new GridLayout(3, false));
		
		label = new Label(buttons, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Button paste = new Button(buttons, SWT.NONE);
		paste.setText("Paste from clipboard");
		
		Button clear = new Button(buttons, SWT.NONE);
		clear.setText("Clear");
		
		pscKey.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				String psc = getCertificateKey();
				if (psc.length() != 0) {
					try {
						decodedPSC = ConvertigoPlugin.decodePsc(psc);
						setErrorMessage(null);
						setMessage(getDescription());
						setPageComplete(true);
					} catch (PscException exception) {
						setErrorMessage(exception.getMessage());
						setPageComplete(false);
					}
				} else {
					setErrorMessage("Please enter your PSC!");
					setPageComplete(false);
				}
			}
			
		});
		
		infoLink.addListener (SWT.Selection, new Listener () {
			
			public void handleEvent(Event event) {
				org.eclipse.swt.program.Program.launch(event.text);
			}
			
		});
		
		paste.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				Clipboard clipboard = new Clipboard(container.getDisplay());
				String data = (String) clipboard.getContents(TextTransfer.getInstance());
				if (data != null && data.length() > 0) {
					pscKey.setText(data.trim());
				}
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			
		});
		
		clear.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				pscKey.setText("");
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			
		});
		
		// Required to avoid an error in the system
		setControl(container);
		setPageComplete(false);
	}

	public String getCertificateKey() {
		return pscKey.getText().trim();
	}
	
	public void clearCertificateKey() {
		pscKey.setText("");
	}
	
	public void setAnonymousCertificateKey() {
		try {
			pscKey.setText(ConvertigoPlugin.makeAnonymousPsc());
		} catch (IOException e) {
			ConvertigoPlugin.logWarning(e, "Unable to make an anonymous PSC");
		}
	}
	
	public void onRegister(final boolean success, final String message) {
		Display.getDefault().asyncExec(new Runnable() {
			
			public void run() {
				if (success) {
					infoLink.setText("Please click on the link you received by mail and paste the generated PSC in the following text area and click the 'Next >' button...");
				} else {
					infoLink.setText("Some error occure during the online registration: " + message + "\n" +
							"Try to fix in the previous screen or register manually on " + RegistrationPage.registrationLink);
					setErrorMessage("Error during the only registration!");
				}
				infoLink.getParent().layout();
			}
			
		});
	}

	public String getSummary() {
		StringBuffer summary = new StringBuffer("PSC server configuration for:\n");
		
		int i = 0;
		while (++i > 0) {
			String server = DeploymentKey.server.value(decodedPSC, i);
			if (server != null && !server.equals("")) {
				summary.append("\t" + server + "\n");
			} else {
				if (i == 1) {
					summary = new StringBuffer();
				}
				i = -1;
			}
		}	
		
		return summary.toString();
	}
	
}
