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

package com.twinsoft.convertigo.eclipse.wizards.learn;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ScreenClass;


public class LearnScreenClassWizardPage2 extends WizardPage {
	private String newScreenClassName = "Learned Screen Class";
	private ScreenClass detectedScreenClass = null;
	private DatabaseObject parentObject = null;
	private boolean isSisterClass = false;
	
	/**
	 * Constructor for SampleNewWizardPage.
	 * @param pageName
	 */
	public LearnScreenClassWizardPage2(ScreenClass detectedScreenClass) {
		super("wizardPage");
		setTitle("New Screen Class");
		setDescription("This wizard help you to create new screen class in learning mode.");
		
		this.detectedScreenClass = detectedScreenClass;
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new LearnScreenClassWizardComposite2(parent, SWT.NULL,
					new ModifyListener() {
						public void modifyText(ModifyEvent e) {
							dialogChanged();
						}
					}
					, newScreenClassName);
		initialize();
		setControl(container);
		dialogChanged();
	}
	
	
	private void initialize() {
	}
	
	
	/**
	 * Ensures that both text fields are set.
	 */

	private void dialogChanged() {
		updateStatus(null);
		
		newScreenClassName = ((LearnScreenClassWizardComposite2)getControl()).getScreenClassName().getText();
		if (newScreenClassName.length() == 0) {
			updateStatus("Please enter screen class name");
			return;
		}
		isSisterClass = ((LearnScreenClassWizardComposite2)getControl()).isSisterClass();
		
		boolean bScreenClassAlreadyExists = screenClassAlreadyExists(newScreenClassName);
		if (bScreenClassAlreadyExists) {
			updateStatus("This screen class name already exists !");
			return;
		}
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	
	public String getScreenClassName() {
		return newScreenClassName;
	}

	public void setScreenClassName(String screenClassName) {
		this.newScreenClassName = screenClassName;
	}
	
	public DatabaseObject getParentObject() {
		return parentObject;
	}
	
	private boolean screenClassAlreadyExists(String screenClassName) {
		if (detectedScreenClass instanceof ScreenClass) {
			parentObject = detectedScreenClass;
			if (isSisterClass)
				parentObject = detectedScreenClass.getParent();
			
			ScreenClass parentScreenClass = (ScreenClass)parentObject;
	        for (ScreenClass inheritedScreenClass : parentScreenClass.getInheritedScreenClasses()) {
	            if (screenClassName.equals(inheritedScreenClass.getName())) {
	                return true;
	            }
	        }
		}
		return false;
	}
}
	
