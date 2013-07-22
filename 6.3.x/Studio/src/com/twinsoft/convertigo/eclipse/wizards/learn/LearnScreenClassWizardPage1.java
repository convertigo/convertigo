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
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.beans.core.ScreenClass;


public class LearnScreenClassWizardPage1 extends WizardPage {
	private ScreenClass detectedScreenClass = null;
	
	/**
	 * Constructor for SampleNewWizardPage.
	 * @param pageName
	 */
	public LearnScreenClassWizardPage1(ScreenClass detectedScreenClass) {
		super("wizardPage");
		setTitle("New Screen Class");
		setDescription("This wizard help you to create new screen class in learning mode.");
		
		this.detectedScreenClass = detectedScreenClass;
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new LearnScreenClassWizardComposite1(parent, SWT.NULL, detectedScreenClass);
		initialize();
		setControl(container);
	}
	
	
	private void initialize() {
	}
	
}
	
