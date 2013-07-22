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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.twinsoft.convertigo.beans.core.ScreenClass;
import org.eclipse.swt.layout.GridData;

public class LearnScreenClassWizardComposite1 extends Composite {

	private ScreenClass detectedScreenClass = null;
	
	private Label label = null;
	
	public LearnScreenClassWizardComposite1(Composite parent, int style, ScreenClass detectedScreenClass) {
		super(parent, style);
		this.detectedScreenClass = detectedScreenClass;
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
        GridData gridData = new org.eclipse.swt.layout.GridData();
        gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
        label = new Label(this, SWT.NONE);
        label.setLayoutData(gridData);
        label.setText("Screen class '"+ detectedScreenClass.getName() +"' has been detected."
        			+ "\n\nIf you would like to create a new screen class, please click Next."
        			+ "\n\nIf detected screen class is the rigth one, please click Cancel.");
        
        GridLayout gridLayout = new GridLayout();
        this.setLayout(gridLayout);
	}
	
}  //  @jve:decl-index=0:visual-constraint="10,10"
