package com.twinsoft.convertigo.eclipse.wizards.setup;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ActivationKeyPage extends WizardPage{
	
	private Text certificateKey;
	
	private Composite container;
	
	public ActivationKeyPage () {
		super("Activation");
		setTitle("Product activation key");
		setDescription("Activation key is used to activate the product.");
	}

	@Override
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.marginWidth = 30;
		
		GridData layoutData = new GridData();
		layoutData.verticalSpan = 2;
		layoutData.verticalIndent = 5;
		
		Button convertigo = new Button(container, SWT.ICON);
		convertigo.setImage(new Image(null, "C:/Users/rahmanf/workspace/CemsStudio/icons/boite-convertigo6.png"));
		convertigo.setLayoutData(layoutData);
		
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.CENTER);
		gd.horizontalSpan = 2;
		gd.verticalIndent = 30;
		
		Label label = new Label(container, SWT.NONE);
		label.setText("Enter your certificate key that you received at the end of the registration\n" +
				" process via email in the box below, then click \"Finish\".");
		label.setLayoutData(gd);
		
		
		label = new Label (container, SWT.NONE);
		label.setText("Certificat Key");
		
		GridData gdlayout = new GridData(GridData.FILL_HORIZONTAL);
		
		certificateKey = new Text(container, SWT.BORDER);
		certificateKey.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				if (!certificateKey.getText().isEmpty()) {
					String key = certificateKey.getText();
					if ("azerty".equals(key)) {
						setErrorMessage(null);
						setMessage(getDescription());
						setPageComplete(true);
					} else {
						setErrorMessage("Incorrect activation key!");
					}
				} else {
					setErrorMessage("Please enter your certificate key!");
				}
			}
		});
		certificateKey.setLayoutData(gdlayout);
		
		// Required to avoid an error in the system
		setControl(container);
		setPageComplete(false);
	}
	
	public String getCertifictateKey() {
		return certificateKey.getText();
	}

}
