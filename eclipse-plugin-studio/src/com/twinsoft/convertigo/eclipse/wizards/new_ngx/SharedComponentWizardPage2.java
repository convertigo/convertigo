/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.wizards.new_ngx;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

class SharedComponentWizardPage2 extends WizardPage {
	private SharedComponentWizard wizard;

	private Map<String, String> variableMap = null;
	
	SharedComponentWizardPage2(SharedComponentWizard wizard) {
		super("wizardPage");
		this.wizard = wizard;
		setTitle("Variables");
		setDescription("Variables creation");
	}

	public void createControl(Composite parent) {
		Composite container = new SharedComponentWizardPage2Composite(parent, SWT.NULL, this);
		initialize();
		setControl(container);
		updateStatus(null, true);
	}
	
	private void initialize() {
		
	}
	
	protected void updateStatus(String message, boolean complete) {
		setErrorMessage(message);
		setPageComplete(complete && message == null);
	}
	
	protected Map<String, String> initTableMap() {
		try {
			return wizard.getItemMap();
		} catch (Exception e) {
			updateStatus("An error occured"+ e.getMessage(), false);
			return new HashMap<String, String>();
		}
	}
	
	protected Map<String, String> getVariableMap() {
		return variableMap;
	}

	@Override
	public void setPageComplete(boolean complete) {
		SharedComponentWizardPage2Composite composite = (SharedComponentWizardPage2Composite)getControl();
		if (composite != null && complete) {
			variableMap = composite.getVariableMap();
		}
		super.setPageComplete(complete);
	}
	
}
	
