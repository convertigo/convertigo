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

import com.twinsoft.convertigo.eclipse.swt.ToggleButton;
import com.twinsoft.convertigo.eclipse.wizards.enums.MobileFeature;
import com.twinsoft.convertigo.eclipse.wizards.enums.MobileLook;


public class NewProjectWizardPage13 extends WizardPage {

	private int look;
	private List<MobileFeature> features;
	private List<MobileFeature> selectedFeatures;
	
	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public NewProjectWizardPage13(ISelection selection) {
		super("wizardPage");
		setTitle("Define Mobile Project parameters");
		setDescription("This step configures the wanted features to add to the project");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new NewProjectWizardComposite13(parent, SWT.NULL,
				new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						dialogChanged();
					}
				}, 
				new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						dialogChanged();
					}
				},
				(NewProjectWizard) this.getWizard());
		initialize();
		setControl(container);
		dialogChanged();
	}

	private void initialize() {
		features = new ArrayList<MobileFeature>();
		selectedFeatures = new ArrayList<MobileFeature>();
	}

	private void dialogChanged() {
		setFeatures(((NewProjectWizardComposite13) getControl()).getFeatures());
		setLook(((NewProjectWizardComposite13) getControl()).getLook());
		
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public void setLook(int look) {
		this.look = look;
	}

	public int getLook() {
		return look;
	}

	public void setFeatures(ToggleButton[] bfeatures) {
		this.features.clear();
		if (bfeatures.length > 0) {
			for (MobileFeature feature :MobileFeature.values()) {
				if (!(bfeatures[feature.index()].getSelection())) {
					this.features.add(feature);
				}
				else {
					this.selectedFeatures.add(feature);
				}
			}
		}
	}

	public List<MobileFeature> getFeaturesToBeRemoved() {
		return features;
	}
	
	public List<MobileFeature> getSelectedFeatures() {
		return selectedFeatures;
	}
	
	public List<String> getCssToBeRemoved() {
		return MobileLook.getUselessCssList(getLook());
	}
}
