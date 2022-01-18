/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class SharedComponentWizardPage1Composite extends Composite {

	private Text text_name;
	private Button rbtnKeep;
	private Button rbtnRemove;
	
	private SharedComponentWizardPage1 page;
	private String sharedComponentName = null;
	private boolean canCustomizeVariables = false;
	
	public SharedComponentWizardPage1Composite(Composite parent, int style, SharedComponentWizardPage1 page) {
		super(parent, style);
		this.page = page;
		this.sharedComponentName = page.getSharedComponentName();
		this.canCustomizeVariables = page.canCustomizeVariables();
		
		initialize();
	}

	protected String getSharedComponentName() {
		return text_name.getText();
	}
	
	protected boolean keepComponent() {
		return rbtnKeep.getSelection();
	}
	
	private void initialize() {
		setLayout(new GridLayout(1, false));
		
		Label label1 = new Label(this, SWT.NONE);
		label1.setText("Please enter a name for your new component:");
		
		text_name = new Text(this, SWT.BORDER);
		text_name.setText(sharedComponentName);
		text_name.setFocus();
		text_name.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				page.dialogChanged();
			}
		});
		text_name.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		new Label(this, SWT.NONE);
		
		Label label2 = new Label(this, SWT.NONE);
		label2.setText("Please choose options:");
		
		rbtnKeep = new Button(this, SWT.RADIO);
		rbtnKeep.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				page.dialogChanged();
			}
			@Override
			public void widgetSelected(SelectionEvent e) {
				page.dialogChanged();
			}
		});
		rbtnKeep.setSelection(true);
		rbtnKeep.setText("keep the original component and disable it (recommanded)");
		
		rbtnRemove = new Button(this, SWT.RADIO);
		rbtnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				page.dialogChanged();
			}
			@Override
			public void widgetSelected(SelectionEvent e) {
				page.dialogChanged();
			}
		});
		rbtnRemove.setText("remove the original component");
		new Label(this, SWT.NONE);
		
		Label label3  = new Label(this, SWT.NONE);
		label3.setText("Note: inner disabled components are ignored");
		new Label(this, SWT.NONE);
		
		Label labelInfos = new Label(this, SWT.NONE);
		GridData gd_labelInfos = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_labelInfos.widthHint = 442;
		labelInfos.setLayoutData(gd_labelInfos);
		
		if (canCustomizeVariables) {
			labelInfos.setText("Click the 'Next' button if you'd like to customize names of detected variables.\r\nClick the 'Finish' button if you want to customize names later on.");
		}
		
		setSize(new org.eclipse.swt.graphics.Point(514,264));
		
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
