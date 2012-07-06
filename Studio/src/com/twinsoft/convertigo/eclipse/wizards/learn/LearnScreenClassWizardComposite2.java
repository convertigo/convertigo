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
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Button;

public class LearnScreenClassWizardComposite2 extends Composite {

	private Label label = null;
	private Text screenClassName = null;
	private Label label1 = null;
	private ModifyListener modifyListener;
	private Group group = null;
	private Button radioButton1 = null;
	private Button radioButton2 = null;
	
	public LearnScreenClassWizardComposite2(Composite parent, int style, ModifyListener ml, String name) {
		super(parent, style);
		modifyListener = ml;
		initialize();
		screenClassName.setText(name);
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
        GridData gridData2 = new org.eclipse.swt.layout.GridData();
        gridData2.horizontalSpan = 3;
        label1 = new Label(this, SWT.NONE);
        label1.setText("Please choose what kind of screen class you would like to create\nbased one detected one");
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
        createGroup();
        label = new Label(this, SWT.NONE);
        label.setText("Give a screen class name");
        label.setLayoutData(gridData);
        screenClassName = new Text(this, SWT.BORDER);
        screenClassName.setLayoutData(gridData1);
        screenClassName.addModifyListener(modifyListener);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        this.setLayout(gridLayout);
	}

	public Text getScreenClassName() {
		return screenClassName;
	}

	public ModifyListener getModifyListener() {
		return modifyListener;
	}

	public void setModifyListener(ModifyListener modifyListener) {
		this.modifyListener = modifyListener;
	}

	public boolean isSisterClass() {
		return radioButton1.getSelection();
	}
	
	/**
	 * This method initializes group	
	 *
	 */
	private void createGroup() {
		GridData gridData3 = new org.eclipse.swt.layout.GridData();
		gridData3.grabExcessHorizontalSpace = false;
		gridData3.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		gridData3.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		group = new Group(this, SWT.NONE);
		group.setLayoutData(gridData3);
		radioButton1 = new Button(group, SWT.RADIO);
		radioButton1.setBounds(new org.eclipse.swt.graphics.Rectangle(143,18,109,16));
		radioButton1.setText("Sister screen class");
		radioButton2 = new Button(group, SWT.RADIO);
		radioButton2.setBounds(new org.eclipse.swt.graphics.Rectangle(12,18,126,16));
		radioButton2.setText("Inherited screen class");
		radioButton2.setSelection(true);
	}

	
}  //  @jve:decl-index=0:visual-constraint="10,10"
