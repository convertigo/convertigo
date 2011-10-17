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

package com.twinsoft.convertigo.eclipse.wizards;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.twinsoft.convertigo.eclipse.swt.ToggleButton;
import com.twinsoft.convertigo.eclipse.wizards.enums.MobileHighEndDevice;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.StringUtils;


public class NewProjectWizardPage12 extends WizardPage {
	private String projectName;
	private static ToggleButton[] devices;
	private static List<MobileHighEndDevice> listDevices;
	private static Boolean selected = false;
	
	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	protected void setProjectTextValue(String projectName) {
		Control control = getControl();
		if (control != null) {
			((NewProjectWizardComposite12)control).getProjectName().setText(projectName);
		}
	}
	
	/**
	 * Constructor for SampleNewWizardPage.
	 * @param pageName
	 */
	public NewProjectWizardPage12(ISelection selection) {
		super("wizardPage");
		setTitle("New Convertigo project");
		setDescription("This wizard creates a new Convertigo Mobile project");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new NewProjectWizardComposite12(parent, SWT.NULL, new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		},
		new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dialogChanged();
			}
		});
		initialize();
		setControl(container);
		dialogChanged();
	}
	
	public void initialize() {
		listDevices = new ArrayList<MobileHighEndDevice>();
		devices = new ToggleButton[MobileHighEndDevice.values().length]; 
	}
	
	/**
	 * Ensures that both text fields are set.
	 */

	public void dialogChanged() {
		projectName = ((NewProjectWizardComposite12)getControl()).getProjectName().getText();
		devices = ((NewProjectWizardComposite12)getControl()).getDevices();
		listDevices = ((NewProjectWizardComposite12)getControl()).getMobileDevices();
		selected = ((NewProjectWizardComposite12)getControl()).selected();
		
		if (projectName.length() == 0) {
			updateStatus("Please enter project name");
			return;
		}
		boolean bProjectAlreadyExists = projectAlreadyExists(projectName);
		if (bProjectAlreadyExists) {
			updateStatus("This project name already exists !");
			return;
		}
		if (!StringUtils.isNormalized(projectName)) {
			updateStatus("Project name must be normalized.\nDon't start with number and don't use non ASCII caracters.");
			return;
		}
		
		if (!selected) {
			updateStatus("Please select targeted devices");
			return;
		}
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	private boolean projectAlreadyExists(String projectName) {
		File file = new File(Engine.PROJECTS_PATH + "/" + projectName); //$NON-NLS-1$
		return file.exists();
	}
	
	public List<MobileHighEndDevice> getMobileDevices() {
		return listDevices;
	}

	public ToggleButton[] getDevices() {
		return devices;
	}
}
	
