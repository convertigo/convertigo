package com.twinsoft.convertigo.eclipse.wizards.setup;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.engine.util.Crypto2;

public class PscKeyPage extends WizardPage {
	
	private Composite container;
	
	private Text pscKey;
	private Label infoLabel;
	
	public void setInfo(String text) {
		infoLabel.setText(text);
	}
	
	public PscKeyPage () {
		super("PSC");
		setTitle("Personal Studio Configuration");
		setDescription("Once you filled the form on the Internet in order\n" +
				"to automatically create a Convertigo Forum account to get a Personal Studio Configuration.");
	}

	public void createControl(Composite parent) {
		
		container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 1;
		layout.marginWidth = 30;
		
		GridData layoutDataText = new GridData(GridData.FILL_HORIZONTAL);
		layoutDataText.verticalIndent = 5;
		
		Label title = new Label (container, SWT.NONE);
		title.setFont(new Font(container.getDisplay(),"Arial", 14, SWT.BOLD));
		Color color = new  Color(container.getDisplay(), 51,102,255);
		title.setForeground(color);
		title.setLayoutData(layoutDataText);
		title.setText("PSC");
		
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 30;
		gd.widthHint = 820;
		
		infoLabel = new Label(container, SWT.WRAP);	
		if (infoLabel.getText().length() == 0) {
			infoLabel.setText("Fill the form accessible on the following page (www.convertigo.com), you will receive an email including" +
							" your Personal Studio Configuration. Please copy your PSC in the following text area:");
		}
		infoLabel.setLayoutData(gd);
		
		GridData gdlayout = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);

		
		pscKey = new Text(container, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
		pscKey.setFont(new Font(container.getDisplay(),"Arial", 8, SWT.NONE));
		pscKey.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				if (pscKey.getText().length() != 0) {
					String psc = pscKey.getText();
					String decipheredPSC = Crypto2.decodeFromHexString("registration", psc);
					
					if (decipheredPSC == null) {
						setErrorMessage("Invalid PSC (unable to decipher)!");
						setPageComplete(false);
					}
					else if (decipheredPSC.startsWith("# PSC file")) {
						setErrorMessage(null);
						setMessage(getDescription());
						setPageComplete(true);
					} else {
						setErrorMessage("Invalid PSC (wrong format)!");
						setPageComplete(false);
					}
				} else {
					setErrorMessage("Please enter your PSC!");
					setPageComplete(false);
				}
			}
		});
		pscKey.setLayoutData(gdlayout);
		
		// Required to avoid an error in the system
		setControl(container);
		setPageComplete(false);
	}

	public String getCertificateKey() {
		return pscKey.getText();
	}
	
}
