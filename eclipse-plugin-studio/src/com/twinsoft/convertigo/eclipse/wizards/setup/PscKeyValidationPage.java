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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin.PscException;
import com.twinsoft.convertigo.eclipse.DeploymentKey;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils.SelectionListener;
import com.twinsoft.convertigo.eclipse.wizards.setup.SetupWizard.SummaryGenerator;

class PscKeyValidationPage extends WizardPage implements SummaryGenerator {
	
	private Composite container;
	
	private Text pscKey;
	private Link infoLink;
	
	public PscKeyValidationPage () {
		super("PscKeyPage");
		setTitle("Personal Studio Configuration");
		setDescription("Paste your PSC...");
	}

	@Override
	public IWizard getWizard() {
		setErrorMessage(null);
		setMessage(getDescription());
		
		SetupWizard wizard = (SetupWizard) super.getWizard();
		wizard.postRegisterState(this.getClass().getSimpleName().toLowerCase());

		return super.getWizard();
	}

	public void createControl(final Composite parent) {	
		ScrolledComposite sc = new ScrolledComposite(parent, SWT.V_SCROLL);
		
		container = new Composite(sc, SWT.NONE);
		GridLayout layout = new GridLayout(1, true);
		layout.marginWidth = 30;
		container.setLayout(layout);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		GridData layoutDataText = new GridData(GridData.FILL_HORIZONTAL);
		layoutDataText.verticalIndent = 5;
		
		Link details = new Link(container, SWT.WRAP);
		details.setText(
				"\nAfter signing up, you'll receive a PSC by email. It is mandatory to deploy projects on your Convertigo Cloud account.\n" +
				"Could you paste it below to configure your Studio?"
		);
		details.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		infoLink = new Link(container, SWT.WRAP);
		infoLink.setText("Please paste your PSC here and click the 'Next >' button...");
		infoLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				
		pscKey = new Text(container, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
		pscKey.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
		pscKey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite buttons = new Composite(container, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		buttons.setLayout(new GridLayout(3, false));
		
		Label label = new Label(buttons, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Button paste = new Button(buttons, SWT.NONE);
		paste.setText("Paste from clipboard");
		
		Button clear = new Button(buttons, SWT.NONE);
		clear.setText("Clear");
		
		pscKey.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String psc = getCertificateKey();
				if (psc.length() != 0) {
					try {
						ConvertigoPlugin.decodePsc(psc);
						SetupWizard wizard = (SetupWizard) getWizard();
						wizard.psc = psc;
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
		
		paste.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Clipboard clipboard = new Clipboard(container.getDisplay());
				String data = (String) clipboard.getContents(TextTransfer.getInstance());
				if (data != null && data.length() > 0) {
					pscKey.setText(data.trim());
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		clear.addSelectionListener((SelectionListener) e -> {
			pscKey.setText("");
		});
		
		parent.addListener(SWT.Resize, e -> {
			container.setSize(parent.getSize());
		});

		sc.setMinSize(400, 500);
		sc.setContent(container);
    	sc.setExpandVertical(true);
		
		// Required to avoid an error in the system
		setControl(sc);
		setPageComplete(false);
	}

	public String getCertificateKey() {
		return pscKey.getText().trim();
	}

	public String getSummary() {
		StringBuffer summary = new StringBuffer("PSC server configuration for:\n");

		try {
			var decodedPSC = ConvertigoPlugin.decodePsc(((SetupWizard) getWizard()).psc);
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
		} catch (PscException e) {
			e.printStackTrace();
		}

		return summary.toString();
	}
	
}
