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

package com.twinsoft.convertigo.eclipse.wizards.new_mobile;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.engine.util.StringUtils;

class SharedComponentWizardPage1 extends WizardPage {
	private SharedComponentWizard wizard;
	
	private String sharedComponentName;
	private boolean keepComponent = true;
	private boolean canCustomizeVariables = false;
	
	protected String getSharedComponentName() {
		return sharedComponentName;
	}

	protected boolean keepComponent() {
		return keepComponent;
	}
	
	protected boolean canCustomizeVariables() {
		return canCustomizeVariables;
	}
	
	/**
	 * Constructor for SampleNewWizardPage.
	 * @param pageName
	 */
	SharedComponentWizardPage1(SharedComponentWizard wizard) {
		super("wizardPage");
		this.wizard = wizard;
		setTitle("New shared component");
		setDescription("This page helps creating your new shared component");
		
		this.sharedComponentName = wizard.getSharedComponentName();
		this.canCustomizeVariables = wizard.canCustomizeVariables();
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new SharedComponentWizardPage1Composite(parent, SWT.NULL, this);
		initialize();
		setControl(container);
		dialogChanged();
	}
	
	private void initialize() {
		
	}
	
	protected void dialogChanged() {
		SharedComponentWizardPage1Composite composite = (SharedComponentWizardPage1Composite)getControl();
		if (composite == null)
			return;
		
		keepComponent = composite.keepComponent();
		
		sharedComponentName = composite.getSharedComponentName();
		if (sharedComponentName.length() == 0) {
			updateStatus("Please enter a name");
			return;
		}
		boolean bComponentAlreadyExists = wizard.sharedComponentAlreadyExists(sharedComponentName);
		if (bComponentAlreadyExists) {
			updateStatus("This component name already exists !");
			return;
		}
		if (!StringUtils.isNormalized(sharedComponentName)) {
			updateStatus("Component name must be normalized.\nDon't start with number and don't use non ASCII caracters.");
			return;
		}
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

}
	
