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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.eclipse.property_editors.EmulatorTechnologyEditor;
import com.twinsoft.convertigo.eclipse.wizards.new_object.ObjectExplorerWizardPage;
import com.twinsoft.convertigo.engine.EngineException;

public class EmulatorTechnologyWizardPage extends WizardPage {
	private Combo emulatorTechnologyCombo;
	
	public EmulatorTechnologyWizardPage() {
		super("EmulatorTechnologyWizardPage");
		setTitle("Emulator technology");
		setDescription("Please choose the emulator technology for connector object.");
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 2;
		layout.verticalSpacing = 9;

		Label label = new Label(container, SWT.NULL);
		label.setText("&Emulator technology:");

		emulatorTechnologyCombo = new Combo(container, SWT.NONE);
		String[] tags = EmulatorTechnologyEditor.getTags(null);
		for (int i = 0 ; i < tags.length ; i ++) {
			emulatorTechnologyCombo.add(tags[i]);
		}
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		emulatorTechnologyCombo.setLayoutData(gd);
		emulatorTechnologyCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				dialogChanged();
			}
		});
		
		initialize();
		//dialogChanged();
		setControl(container);
	}
	
	@Override
	public void performHelp() {
		getPreviousPage().performHelp();
	}

	private void initialize() {
		emulatorTechnologyCombo.select(emulatorTechnologyCombo.indexOf(EmulatorTechnologyEditor.IBM5250));
	}
	
	private void dialogChanged() {
		String emTech = getEmulatorTechnology();
		if (emTech.length() == 0) {
			updateStatus("Emulator technology must be specified");
			return;
		}
		
		try {
			DatabaseObject dbo = ((ObjectExplorerWizardPage)getWizard().getPage("ObjectExplorerWizardPage")).getCreatedBean();
			if (dbo != null) {
				if (dbo instanceof JavelinConnector) {
					((JavelinConnector)dbo).setEmulatorTechnology(emTech);
					((JavelinConnector)dbo).emulatorID = ((JavelinConnector)dbo).findEmulatorId();
				}
			}
			ServiceCodeWizardPage servCodeWP = (ServiceCodeWizardPage)getWizard().getPage("ServiceCodeWizardPage");
			servCodeWP.update();
		} catch (NullPointerException e) {
			updateStatus("New Bean has not been instantiated");
			return;
		} catch (EngineException e) {
			updateStatus("Emulmator id couldn't be found from emulator technology.");
			return;
		}
		
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	
	/**
	 * Returns the emulatorTechnology String value (from EmulatorTechnologyEditor class)
	 * corresponding to the item selected in the combobox.
	 * @return the emulatorTechnology String value from EmulatorTechnologyEditor class
	 */
	public String getEmulatorTechnology() {
		String selectedValue = emulatorTechnologyCombo.getText();
		String returnValue = "";
		String[] tags = EmulatorTechnologyEditor.getTags(null);
		String[] classNames = EmulatorTechnologyEditor.getEmulatorClassNames(null);
		int i = 0;
		boolean found = false;
		while(i < tags.length && !found) {
			if (tags[i].equals(selectedValue))
				found = true;
			else
				i ++;
		}
		if (found)
			returnValue = classNames[i]; 
		return returnValue;
	}
	
	public void setEmulatorTechnology(String emTech) {
		emulatorTechnologyCombo.select(emulatorTechnologyCombo.indexOf(emTech));
	}
}