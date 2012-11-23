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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.wizards.references;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.references.WebServiceReference;
import com.twinsoft.convertigo.eclipse.wizards.new_object.ObjectExplorerWizardPage;

public class WebServiceWizardPage extends WizardPage {
//	private Object parentObject = null;
	
	private Text url = null;
	private String urlPath = "";
	
	public WebServiceWizardPage(Object parentObject) {
		super("WebServiceWizardPage");
//		this.parentObject = parentObject;
		setTitle("WebService");
		setDescription("Please enter an the Web service URL.");
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 2;
		layout.horizontalSpacing = 15;
		layout.verticalSpacing = 9;

		Label label1 = new Label(container, SWT.NULL);
		label1.setText("&Enter URL:");
		
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		url = new Text(container, SWT.BORDER);
		url.setLayoutData(data);
		url.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				urlPath = WebServiceWizardPage.this.url.getText();
				dialogChanged();
			}
		});
		
		initialize();
		dialogChanged();
		setControl(container);
	}

	private void initialize() {
		
	}
	
	private void dialogChanged() {
		String message = null;
		if (!urlPath.equals("")) {
			try {
				new URL(urlPath);
				DatabaseObject dbo = ((ObjectExplorerWizardPage)getWizard().getPage("ObjectExplorerWizardPage")).getCreatedBean();
				WebServiceReference webServiceReference = (WebServiceReference)dbo;
				webServiceReference.setUrlpath(urlPath);
			} catch (MalformedURLException e) {
				message = "Please enter a valid URL";
			} catch (NullPointerException e) {
				message = "New Bean has not been instantiated";
			} catch (Exception e) {
				message = e.getMessage();
			}
			
		}
		updateStatus(message);
	}
	
	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	
	@Override
	public void performHelp() {
		getPreviousPage().performHelp();
	}

	@Override
	public IWizardPage getNextPage() {
		return null;
	}
	
	
}
