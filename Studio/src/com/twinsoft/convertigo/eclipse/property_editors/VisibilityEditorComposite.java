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

package com.twinsoft.convertigo.eclipse.property_editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.twinsoft.convertigo.engine.enums.Visibility;

public class VisibilityEditorComposite extends AbstractDialogComposite {
	private Button checkboxAll;
	private Button checkboxLog;
	private Button checkboxStudio;
	private Button checkboxPlatform;
	private Button checkboxXml;
	private Group groupVisibility;
	
	public VisibilityEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		initialize();
		
		String value = (String)cellEditor.databaseObjectTreeObject.getPropertyValue(cellEditor.propertyDescriptor.getId());
		int visibility = Integer.parseInt(value,10);
		boolean bAll = visibility == -1;
		checkboxAll.setSelection(bAll);
		checkboxLog.setSelection(Visibility.Logs.isMasked(visibility));
		checkboxStudio.setSelection(Visibility.Studio.isMasked(visibility));
		checkboxPlatform.setSelection(Visibility.Platform.isMasked(visibility));
		checkboxXml.setSelection(Visibility.XmlFile.isMasked(visibility));
		setGroupEnable(bAll);
	}

	
	private void initialize() {
		GridLayout gridLayout = new GridLayout();
		this.setLayout(gridLayout);
		
		checkboxAll = new Button(this, SWT.CHECK);
		checkboxAll.setText("Mask value in all");
		checkboxAll
				.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
						setGroupEnable(((Button)e.getSource()).getSelection());
					}
				});
		Label label = new Label(this, SWT.NONE);
		label.setText("");
		createGroupVisibility();
		
		this.setSize(new org.eclipse.swt.graphics.Point(253,241));
		
	}

	private void setGroupEnable(boolean bSelected) {
		checkboxLog.setEnabled(!bSelected);
		checkboxStudio.setEnabled(!bSelected);
		checkboxPlatform.setEnabled(!bSelected);
		checkboxXml.setEnabled(!bSelected);
	}
	
	private void createGroupVisibility() {
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		groupVisibility = new Group(this, SWT.NONE);
		groupVisibility.setText("Mask value in");
		groupVisibility.setLayoutData(gridData);
		
		checkboxLog = new Button(groupVisibility, SWT.CHECK);
		checkboxLog.setBounds(new org.eclipse.swt.graphics.Rectangle(12,42,200,18));
		checkboxLog.setText("log files");
		
		checkboxStudio = new Button(groupVisibility, SWT.CHECK);
		checkboxStudio.setBounds(new org.eclipse.swt.graphics.Rectangle(12,72,200,16));
		checkboxStudio.setText("studio user interface");
		
		checkboxPlatform = new Button(groupVisibility, SWT.CHECK);
		checkboxPlatform.setBounds(new org.eclipse.swt.graphics.Rectangle(12,102,200,16));
		checkboxPlatform.setText("platform user interface");
		
		checkboxXml = new Button(groupVisibility, SWT.CHECK);
		checkboxXml.setBounds(new org.eclipse.swt.graphics.Rectangle(12,132,200,16));
		checkboxXml.setText("project's XML files");
	}

	@Override
	public Object getValue() {
		int visibility = -1;
		if (!checkboxAll.getSelection()) {
			visibility = 0;
			if (checkboxLog.getSelection()) visibility |= Visibility.Logs.getMask();
			if (checkboxStudio.getSelection()) visibility |= Visibility.Studio.getMask();
			if (checkboxPlatform.getSelection()) visibility |= Visibility.Platform.getMask();
			if (checkboxXml.getSelection()) visibility |= Visibility.XmlFile.getMask();
		}
		return new Integer(visibility).toString();
	}

}
