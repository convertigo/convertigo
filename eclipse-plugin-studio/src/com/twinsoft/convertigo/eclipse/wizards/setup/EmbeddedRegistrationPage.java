/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.browser.callback.OpenPopupCallback;
import com.teamdev.jxbrowser.browser.callback.OpenPopupCallback.Response;
import com.teamdev.jxbrowser.browser.event.BrowserClosed;
import com.teamdev.jxbrowser.dom.Element;
import com.teamdev.jxbrowser.dom.event.EventType;
import com.teamdev.jxbrowser.navigation.event.LoadProgressChanged;
import com.teamdev.jxbrowser.view.swt.BrowserView;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.wizards.setup.SetupWizard.RegisterCallback;
import com.twinsoft.convertigo.eclipse.wizards.setup.SetupWizard.SummaryGenerator;
import com.twinsoft.convertigo.engine.Engine;

public class EmbeddedRegistrationPage extends WizardPage implements RegisterCallback, SummaryGenerator {
	
	private C8oBrowser browser;
	
	public EmbeddedRegistrationPage () {
		super("EmbeddedRegistrationPage");
		setTitle("Register or Login now");
		setDescription("Register or login...");
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
		Composite composite = new Composite(parent, SWT.NONE);
		StackLayout stack = new StackLayout();
		composite.setLayout(stack);
		browser = new C8oBrowser(composite, SWT.NONE);
		stack.topControl = browser;
		browser.getBrowser().navigation().on(LoadProgressChanged.class, (evt) -> {
			System.out.println("LoadProgressChanged: " + evt);
			if (evt.progress() >= 1f) {
				Engine.logStudio.error("evt.progress(): " + evt.progress());
				browser.getBrowser().mainFrame().get().document().get().addEventListener(EventType.of("DOMSubtreeModified"), event -> {
					try {
						Element e_psc = browser.getBrowser().mainFrame().get().document().get().findElementByClassName("class1567174301079").get();
						String psc = e_psc.textContent();
						if (StringUtils.isNotBlank(psc)) {
							browser.getDisplay().asyncExec(() -> {
								Label label = new Label(composite, SWT.BORDER);
								label.setText("psc: " + psc);
								stack.topControl = label;
								browser.dispose();
								composite.layout(true);
								this.setPageComplete(true);
							});
						}
					} catch (Exception e) {
					}
				}, false);
			}
		});
		
		browser.getBrowser().set(OpenPopupCallback.class , params -> {
			browser.getDisplay().asyncExec(() -> {
				Browser br = params.popupBrowser();
				BrowserView bv = BrowserView.newInstance(composite, br);
				stack.topControl = bv;
				composite.layout(true);
				br.on(BrowserClosed.class, event ->
					browser.getDisplay().asyncExec(() -> {
						stack.topControl = browser;
						composite.layout(true);
					})
				);
			});
			return Response.proceed();
		});
		
		browser.setUrl("https://www.convertigo.com/signup");
		
		setControl(composite);
		setPageComplete(false);
	}

	public String getCertificateKey() {
		return "";
	}
	
	public void clearCertificateKey() {
	}
	
	public void setAnonymousCertificateKey() {
//		try {
//			pscKey.setText(ConvertigoPlugin.makeAnonymousPsc());
//		} catch (IOException e) {
//			ConvertigoPlugin.logWarning(e, "Unable to make an anonymous PSC");
//		}
	}
	
	public void onRegister(final boolean success, final String message) {
//		Display.getDefault().asyncExec(new Runnable() {
//			
//			public void run() {
//				if (success) {
//					infoLink.setText("Please click on the link you received by mail and paste the generated PSC in the following text area and click the 'Next >' button...");
//				} else {
//					infoLink.setText("Some error occure during the online registration: " + message + "\n" +
//							"Try to fix in the previous screen or register manually on " + RegistrationPage.registrationLink);
//					setErrorMessage("Error during the only registration!");
//				}
//				infoLink.getParent().layout();
//			}
//		});
	}

	public String getSummary() {
		StringBuffer summary = new StringBuffer("PSC server configuration for:\n");
		
//		int i = 0;
//		while (++i > 0) {
//			String server = DeploymentKey.server.value(decodedPSC, i);
//			if (server != null && !server.equals("")) {
//				summary.append("\t" + server + "\n");
//			} else {
//				if (i == 1) {
//					summary = new StringBuffer();
//				}
//				i = -1;
//			}
//		}	
		
		return summary.toString();
	}

	@Override
	public void dispose() {
		browser.dispose();
		super.dispose();
	}
	
}
