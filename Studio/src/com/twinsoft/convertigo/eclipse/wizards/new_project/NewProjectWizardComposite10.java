/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.wizards.new_project;

import java.io.File;
import java.net.MalformedURLException;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.WsReferenceAuthenticatedComposite;

public class NewProjectWizardComposite10 extends Composite {
	private ModifyListener modifyListener;
	public ProgressBar progressBar = null;
	public Label labelProgression = null;
	public Combo combo = null;
	public Button browseButton = null;
	
	private WsReferenceAuthenticatedComposite wsRefAuthenticated = null;
	public Button useAuthentication = null;
	public Text loginText = null, passwordText = null;
	
	public NewProjectWizardComposite10(Composite parent, int style, ModifyListener ml, IWizard wizard) {
		super(parent, style);
		modifyListener = ml;
		initialize();
	}

	protected void initialize() {
		final Composite container = this;
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.makeColumnsEqualWidth = false;
		
		container.setLayout(gridLayout);
		
		Label description = new Label(this, SWT.NONE);
		description.setText("Please enter a valid WSDL url:");
		GridData data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		data.grabExcessHorizontalSpace = true;
		description.setLayoutData (data);
		
		combo = new Combo(this, SWT.NONE);
		combo.add("http://www.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL");
		combo.add("http://demo.convertigo.net/cems/projects/globalCompany_HR_WS/.wsl?wsdl");
		combo.add("http://demo.convertigo.net/cems/projects/globalCompany_accounting_WS/.wsl?wsdl");
		if (modifyListener != null)
			combo.addModifyListener(modifyListener);
		data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;		
		combo.setLayoutData(data);
		
		browseButton = new Button(this, SWT.NONE);
		browseButton.setText("Browse...");
		data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		combo.setLayoutData (data);
		
		//Event click browse button
		browseButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(container.getShell(), SWT.NULL);
				dialog.setFilterExtensions(new String[]{"*.wsdl", "*.xml"});
				dialog.setText("Select your WSDL file");
				String path = dialog.open();
				if (path != null) {
					File file = new File(path);
					if (file.isFile()) {
						try {
							String fileUrl = file.toURI().toURL().toString();
							combo.add(fileUrl.replaceAll("file:/", "file:///"));
						} catch (MalformedURLException e1) {
							ConvertigoPlugin.logException(e1, "Unexpected exception");
						}
						combo.select(combo.getItemCount()-1);
					}
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		data.grabExcessHorizontalSpace = true;		
		
		/* Authenticated Composite for import WS Reference */
		wsRefAuthenticated = new WsReferenceAuthenticatedComposite(this, SWT.NONE, data);
		
		useAuthentication = wsRefAuthenticated.useAuthentication;
		loginText = wsRefAuthenticated.loginText;
		passwordText = wsRefAuthenticated.passwordText;
	}
}
