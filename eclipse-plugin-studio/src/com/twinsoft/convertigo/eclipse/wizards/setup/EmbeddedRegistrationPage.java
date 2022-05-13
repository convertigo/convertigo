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

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.browser.callback.OpenPopupCallback;
import com.teamdev.jxbrowser.browser.callback.OpenPopupCallback.Response;
import com.teamdev.jxbrowser.browser.event.BrowserClosed;
import com.teamdev.jxbrowser.dom.Element;
import com.teamdev.jxbrowser.dom.event.EventType;
import com.teamdev.jxbrowser.event.Subscription;
import com.teamdev.jxbrowser.navigation.event.LoadProgressChanged;
import com.teamdev.jxbrowser.view.swt.BrowserView;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin.PscException;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;

public class EmbeddedRegistrationPage extends WizardPage {
	
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
		Composite root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(2, false));
		
		Composite composite = new Composite(root, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1));
		StackLayout stack = new StackLayout();
		composite.setLayout(stack);
		browser = new C8oBrowser(composite, SWT.NONE);
		stack.topControl = browser;
		Subscription[] subscription = {null};
		subscription[0] = browser.getBrowser().navigation().on(LoadProgressChanged.class, (evt) -> {
			if (evt.progress() >= 1f) {
				System.out.println("evt: " + evt);
				subscription[0].unsubscribe();
				String[] psc = {null};
				browser.getBrowser().mainFrame().get().document().get().addEventListener(EventType.of("DOMSubtreeModified"), event -> {
					try {
						if (psc[0] != null) {
							return;
						}
						Element e_psc = browser.getBrowser().mainFrame().get().document().get().findElementById("psc").get();
						if ((psc[0] = e_psc.attributeValue("psc")) != null) {
							System.out.println("psc: " + psc[0]);
							try {
								ConvertigoPlugin.decodePsc(psc[0]);
								SetupWizard wizard = (SetupWizard) getWizard();
								wizard.psc = psc[0];
								browser.getDisplay().asyncExec(() -> {
									wizard.performFinish();
								});
							} catch (PscException exception) {
								setErrorMessage(exception.getMessage());
								setPageComplete(false);
							}
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
		
		browser.setUrl("https://test-convertigo.convertigo.net/convertigo/projects/convertigo_signup/DisplayObjects/mobile/login");
		
		Button havePSC = new Button(root, SWT.CHECK);
		havePSC.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				setPageComplete(havePSC.getSelection());
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		Label label = new Label(root, SWT.NONE);
		label.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseDown(MouseEvent e) {
				havePSC.setSelection(!havePSC.getSelection());
				havePSC.notifyListeners(SWT.Selection, null);
			}
			
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		label.setText("I want to paste my PSC");
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		root.pack();
		setControl(root);
		setPageComplete(false);
	}

	@Override
	public void dispose() {
		browser.dispose();
		super.dispose();
	}
	
}
