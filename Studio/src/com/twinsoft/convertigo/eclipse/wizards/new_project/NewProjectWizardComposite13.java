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

import com.twinsoft.convertigo.eclipse.swt.ToggleButton;
import com.twinsoft.convertigo.eclipse.wizards.enums.MobileFeature;
import com.twinsoft.convertigo.eclipse.wizards.enums.MobileLook;

public class NewProjectWizardComposite13 extends Composite {

	private Label label1 = null;
	private ModifyListener modifyListener;
	private Group group = null;
	private Group group1 = null;
	private Button[] radioLook;
	
	private ToggleButton[] features = new ToggleButton[]{};
	private int selectedLook = 0;
	private SelectionListener selectionListener;

	public NewProjectWizardComposite13(Composite parent, int style, ModifyListener ml, SelectionListener sl, NewProjectWizard wizard) {
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
		GridData gridData2 = new GridData();
        gridData2.horizontalSpan = 2;
        label1 = new Label(this, SWT.NONE);
        label1.setText("The chosen project template may includes some \'default\' features.\n\nPlease configure your Mobile Project here:\n\n");
        label1.setLayoutData(gridData2);
       	createGroup();
        createGroup1();
        GridLayout gridLayout = new GridLayout();
        gridLayout.verticalSpacing = 15;
        gridLayout.numColumns = 2;
        this.setLayout(gridLayout);
	}

	public ModifyListener getModifyListener() {
		return modifyListener;
	}

	public void setModifyListener(ModifyListener modifyListener) {
		this.modifyListener = modifyListener;
	}

	/**
	 * This method initializes group	
	 *
	 */
	private void createGroup() {
		GridLayout gridLayout1 = new GridLayout();
		gridLayout1.numColumns = MobileFeature.values().length;
		GridData gridData3 = new GridData();
		gridData3.horizontalAlignment = GridData.FILL;
		gridData3.horizontalSpan = 2;
		gridData3.grabExcessHorizontalSpace = true;
		gridData3.verticalAlignment = GridData.FILL;
		group = new Group(this, SWT.NONE);
		group.setText("Features");
		group.setLayoutData(gridData3);
		group.setLayout(gridLayout1);
		
		features = new ToggleButton[MobileFeature.values().length];
		for (MobileFeature feature :MobileFeature.values()) {
			ToggleButton featureButton = features[feature.index()] = new ToggleButton(group, SWT.NONE);
			featureButton.addSelectionListener(selectionListener);
			Image image = new Image(getDisplay(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/wizards/images/feature_" + feature.fileName().replaceAll("\\.js", ".png")));
			featureButton.setImage(image);
			featureButton.setToolTipText("Click to select the " + feature.displayName() + " feature");
			featureButton.setSelection(true);
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
		gridData.horizontalSpan = gridLayout1.numColumns;
		composite.setLayoutData(gridData);
		
		selectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
		    	for (ToggleButton feature :features) {
		    		feature.setSelection(true);
				}
		    	selectionListener.widgetSelected(e);
		     }
		  });
		
		deselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (ToggleButton feature :features) {
					feature.setSelection(false);
				}
				selectionListener.widgetSelected(e);
			}
		});
	}
	
	/**
	 * This method initializes group1	
	 *
	 */
	private void createGroup1() {
		GridData gridData4 = new GridData();
		gridData4.horizontalAlignment = GridData.FILL;
		gridData4.grabExcessHorizontalSpace = true;
		gridData4.verticalAlignment = GridData.CENTER;
		GridLayout gridLayout2 = new GridLayout();
		gridLayout2.numColumns = 3;
		group1 = new Group(this, SWT.NONE);
		group1.setText("Look and feel");
		group1.setLayoutData(gridData4);
		group1.setLayout(gridLayout2);
		
		GridData lookData = new GridData();
		lookData.horizontalAlignment = SWT.CENTER;
		lookData.verticalAlignment = SWT.FILL;
		lookData.grabExcessHorizontalSpace = true;
		
		radioLook = new Button[MobileFeature.values().length];
		for (MobileLook look :MobileLook.values()) {
			radioLook[look.index()] = new Button(group1, SWT.RADIO);
			Image image = new Image(getDisplay(), MobileLook.class.getResourceAsStream("/com/twinsoft/convertigo/eclipse/wizards/images/look_" + look.fileName().replaceAll(".css", "") +".png"));
			radioLook[look.index()].setImage(image);
			radioLook[look.index()].setToolTipText("Click to select the " + look.displayName() + " feature");
			radioLook[look.index()].addSelectionListener(selectionListener);
			radioLook[look.index()].setLayoutData(lookData);
		}
		radioLook[MobileLook.AUTO.index()].setSelection(true);
	}
	
	public int getLook() {
		for (MobileLook look :MobileLook.values()) {
			if (radioLook[look.index()].getSelection()){
				selectedLook = look.index();
			}
		}
		return selectedLook;
	}
	
	public ToggleButton[] getFeatures() {
		return features;
	}
}
