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

package com.twinsoft.convertigo.eclipse.wizards.deploy;

import java.util.HashMap;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

class ProjectDeployOptionsWizardPage extends WizardPage {
	private HashMap<String, Object> data = null;
	
	private ProjectDeployOptionsComposite composite = null;
	
	ProjectDeployOptionsWizardPage() {
		super("ProjectDeployOptionsWizardPage", "Deployment options", null);
		this.data = new HashMap<String, Object>();
		this.setPageComplete(false);
	}
	
	@Override
	public void createControl(Composite parent) {
		composite = new ProjectDeployOptionsComposite(this, parent, SWT.NONE);
		composite.setOkButton(null);
		setControl(composite);
	}

	private HashMap<String, Object> getData() {
		if (composite != null) {
			String convertigoServer = composite.convertigoServer.getText();
			//if ((convertigoServer == null) || (convertigoServer.equals(""))) return;
			
			String convertigoUserName = composite.convertigoAdmin.getText();
			if (convertigoUserName == null) convertigoUserName = "";
	
			String convertigoUserPassword = composite.convertigoPassword.getText();
			if (convertigoUserPassword == null) convertigoUserPassword = "";
			
			boolean isHttps = composite.checkBox.getSelection();
			boolean trustAllCertificates = composite.checkTrustAllCertificates.getSelection();
			boolean bAssembleXsl = composite.assembleXsl.getSelection();
			
			data.put("convertigoServer", convertigoServer);
			data.put("convertigoUserName", convertigoUserName);
			data.put("convertigoUserPassword", convertigoUserPassword);
			data.put("isHttps", Boolean.valueOf(isHttps));
			data.put("trustAllCertificates", Boolean.valueOf(trustAllCertificates));
			data.put("bAssembleXsl", Boolean.valueOf(bAssembleXsl));
		}
		return data;
	}
	
	@Override
	public void setPageComplete(boolean complete) {
		super.setPageComplete(complete);
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (!visible && isControlCreated() && !isCurrentPage()) {
			try {
				if (getContainer().getCurrentPage().equals(getWizard().getNextPage(this))) {
					((ProjectDeployResultWizardPage)getWizard().getNextPage(this)).setOptionsData(getData());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean canFlipToNextPage() {
		return super.canFlipToNextPage();
	}

	@Override
	public IWizardPage getNextPage() {
		return super.getNextPage();
	}

	@Override
	public boolean isPageComplete() {
		return super.isPageComplete();
	}	
}
