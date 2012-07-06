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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.swt.ToggleButton;
import com.twinsoft.convertigo.eclipse.wizards.enums.MobileHighEndDevice;

public class NewProjectWizardComposite12 extends Composite {

	private Label label = null;
	private Text projectName = null;
	private Label label1 = null;
	private Group group;
	private ModifyListener modifyListener;
	private SelectionListener selectionListener;
	private ToggleButton[] devices;
	private List<MobileHighEndDevice> listDevices = new ArrayList<MobileHighEndDevice>();

	public NewProjectWizardComposite12(Composite parent, int style, ModifyListener ml,SelectionListener sl) {
		super(parent, style);
		modifyListener = ml;
		selectionListener = sl;
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
        GridData gridData2 = new org.eclipse.swt.layout.GridData();
        gridData2.horizontalSpan = 3;
        label1 = new Label(this, SWT.NONE);
        label1.setText(" - Please use a relevant project name. Use only alphanumeric characters. \n - Avoid the use of special characters  (âàéèêù...)\n and punctuation characters as space, pound or others. We suggest you use only lowercase letters. \n - If you use uppercase letters, be sure use the same letter case when you will call\ntransactions using the convertigo's url interface.\n\n The project name also defines the Convertigo's physical and virtual directories:\n\n- All your project's ressources will be held in the <your_workspace>/convertigo/projects/<your_project_name> directory.\n- Your project's URL will be http://<server_name>:<port>/convertigo/projects/<your_project_name>\n\n");
        label1.setLayoutData(gridData2);
        GridData gridData1 = new org.eclipse.swt.layout.GridData();
        gridData1.grabExcessHorizontalSpace = true;
        gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
        gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
        GridData gridData = new org.eclipse.swt.layout.GridData();
        gridData.grabExcessHorizontalSpace = false;
        gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
        gridData.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
        gridData.grabExcessVerticalSpace = false;
        label = new Label(this, SWT.NONE);
        label.setText("Project's name");
        label.setLayoutData(gridData);
        projectName = new Text(this, SWT.BORDER);
        projectName.setLayoutData(gridData1);
        projectName.addModifyListener(modifyListener);
        projectName.setFocus();

        createGroup();

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        this.setLayout(gridLayout);
        this.setSize(new org.eclipse.swt.graphics.Point(514,264));
	}
	
	private void createGroup() {
		GridLayout gridLayout1 = new GridLayout();
		gridLayout1.numColumns = 5;
		GridData gridData3 = new GridData();
		gridData3.horizontalAlignment = GridData.FILL;
		gridData3.horizontalSpan = 2;
		gridData3.grabExcessHorizontalSpace = true;
		gridData3.verticalAlignment = GridData.FILL;
		group = new Group(this, SWT.NONE);
		group.setText("Select target devices");
		group.setLayoutData(gridData3);
		group.setLayout(gridLayout1);

		devices = new ToggleButton[MobileHighEndDevice.values().length];
		for (MobileHighEndDevice device :MobileHighEndDevice.values()) {
			ToggleButton deviceButton = devices[device.index()] = new ToggleButton(group, SWT.NONE);
			deviceButton.addSelectionListener(selectionListener);
			Image image = new Image(getDisplay(), getClass().getResourceAsStream("images/device_" + device.displayName() +".png"));
			deviceButton.setImage(image);
			deviceButton.setToolTipText("Click to select the " + device.displayName() + " mobile device");
			deviceButton.setSelection(true);
		}
		
		Composite composite = new Composite(group, SWT.NONE);
		Button selectAll = new Button(composite, SWT.NONE);
		selectAll.setText("Select all");
		Button deselectAll = new Button(composite, SWT.NONE);
		deselectAll.setText("Deselect all");
           
		GridLayout layoutComposite = new GridLayout();
		layoutComposite.numColumns = 2;
		composite.setLayout(layoutComposite);
    
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.END;
		gridData.horizontalSpan = 5;
		composite.setLayoutData(gridData);
		
		selectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
		    	for (ToggleButton device :devices) {
					device.setSelection(true);
				}
		    	selectionListener.widgetSelected(e);
		     }
		  });
		
		deselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (ToggleButton device :devices) {
					device.setSelection(false);
				}
				selectionListener.widgetSelected(e);
			}
		});
	}

	public Text getProjectName() {
		return projectName;
	}
	
	public ToggleButton[] getDevices() {
		return devices;
	}
	
	public List<MobileHighEndDevice> getMobileDevices() {
		listDevices.clear();
		for (MobileHighEndDevice device :MobileHighEndDevice.values()) {
			if (devices[device.index()].getSelection()) {
				listDevices.add(device);
			}
		}
		return listDevices;
	}
	
	public Boolean selected() {
		for (ToggleButton device :devices) {
			if (device.getSelection()) {
				return true;
			}
		}
		return false;
	}
}