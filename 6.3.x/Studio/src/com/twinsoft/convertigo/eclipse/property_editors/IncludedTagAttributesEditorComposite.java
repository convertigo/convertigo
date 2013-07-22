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

import com.twinsoft.convertigo.beans.core.Transaction;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.GridData;

public class IncludedTagAttributesEditorComposite extends AbstractDialogComposite {

	private Group groupDefinition = null;
	private Group groupPosition = null;
	private Group groupColor = null;
	private Group groupDecoration = null;
	private Button checkBoxName = null;
	private Button checkBoxType = null;
	private Button checkBoxLine = null;
	private Button checkBoxColumn = null;
	private Button checkBoxForeground = null;
	private Button checkBoxBackground = null;
	private Button checkBoxIntense = null;
	private Button checkBoxReverse = null;
	private Button checkBoxUnderline = null;
	private Button checkBoxBlink = null;
	private Button checkBoxOptional = null;

	public IncludedTagAttributesEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		initialize();
		
		boolean[] includedTagAttributes = (boolean[])cellEditor.databaseObjectTreeObject.getPropertyValue(cellEditor.propertyDescriptor.getId());
		//boolean[] includedTagAttributes = new boolean[]{true,true,true,true,true,true,true,true,true,true,true};
        
        checkBoxName.setSelection(includedTagAttributes[Transaction.ATTRIBUTE_NAME]);
        checkBoxType.setSelection(includedTagAttributes[Transaction.ATTRIBUTE_TYPE]);
        checkBoxColumn.setSelection(includedTagAttributes[Transaction.ATTRIBUTE_COLUMN]);
        checkBoxLine.setSelection(includedTagAttributes[Transaction.ATTRIBUTE_LINE]);
        checkBoxForeground.setSelection(includedTagAttributes[Transaction.ATTRIBUTE_FOREGROUND]);
        checkBoxBackground.setSelection(includedTagAttributes[Transaction.ATTRIBUTE_BACKGROUND]);
        checkBoxBlink.setSelection(includedTagAttributes[Transaction.ATTRIBUTE_BLINK]);
        checkBoxIntense.setSelection(includedTagAttributes[Transaction.ATTRIBUTE_INTENSE]);
        checkBoxUnderline.setSelection(includedTagAttributes[Transaction.ATTRIBUTE_UNDERLINE]);
        checkBoxReverse.setSelection(includedTagAttributes[Transaction.ATTRIBUTE_REVERSE]);
        checkBoxOptional.setSelection(includedTagAttributes[Transaction.ATTRIBUTE_OPTIONAL]);
		
	}

	private void initialize() {
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		this.setLayout(gridLayout);
		createGroupDefinition();
		createGroupPosition();
		createGroupColor();
		createGroupDecoration();
		checkBoxOptional = new Button(this, SWT.CHECK);
		checkBoxOptional.setText("optional");
		checkBoxOptional.setLayoutData(gridData);
		this.setSize(new org.eclipse.swt.graphics.Point(264,190));
	}
	
	public Object getValue() {
        boolean[] includedTagAttributes = new boolean[11];
        
        includedTagAttributes[Transaction.ATTRIBUTE_NAME] = checkBoxName.getSelection();
        includedTagAttributes[Transaction.ATTRIBUTE_TYPE] = checkBoxType.getSelection();
        includedTagAttributes[Transaction.ATTRIBUTE_COLUMN] = checkBoxColumn.getSelection();
        includedTagAttributes[Transaction.ATTRIBUTE_LINE] = checkBoxLine.getSelection();
        includedTagAttributes[Transaction.ATTRIBUTE_FOREGROUND] = checkBoxForeground.getSelection();
        includedTagAttributes[Transaction.ATTRIBUTE_BACKGROUND] = checkBoxBackground.getSelection();
        includedTagAttributes[Transaction.ATTRIBUTE_REVERSE] = checkBoxReverse.getSelection();
        includedTagAttributes[Transaction.ATTRIBUTE_BLINK] = checkBoxBlink.getSelection();
        includedTagAttributes[Transaction.ATTRIBUTE_UNDERLINE] = checkBoxUnderline.getSelection();
        includedTagAttributes[Transaction.ATTRIBUTE_INTENSE] = checkBoxIntense.getSelection();
        includedTagAttributes[Transaction.ATTRIBUTE_OPTIONAL] = checkBoxOptional.getSelection();

        return includedTagAttributes;
	}

	/**
	 * This method initializes groupDefinition	
	 *
	 */
	private void createGroupDefinition() {
		groupDefinition = new Group(this, SWT.NONE);
		groupDefinition.setText("Definition");
		checkBoxName = new Button(groupDefinition, SWT.CHECK);
		checkBoxName.setBounds(new org.eclipse.swt.graphics.Rectangle(8,18,50,16));
		checkBoxName.setText("name");
		checkBoxType = new Button(groupDefinition, SWT.CHECK);
		checkBoxType.setBounds(new org.eclipse.swt.graphics.Rectangle(8,41,51,16));
		checkBoxType.setText("type");
		checkBoxType
				.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
						if (!checkBoxType.getSelection()) {
							//com.twinsoft.convertigo.studio.Studio.theApp.warning(java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/studio/res/editors/IncludedTagAttributesEditorPanel").getString("type_warning"));
						}
					}
				});
	}

	/**
	 * This method initializes groupPosition	
	 *
	 */
	private void createGroupPosition() {
		groupPosition = new Group(this, SWT.NONE);
		groupPosition.setText("Position");
		checkBoxLine = new Button(groupPosition, SWT.CHECK);
		checkBoxLine.setBounds(new org.eclipse.swt.graphics.Rectangle(8,18,44,16));
		checkBoxLine.setText("line");
		checkBoxColumn = new Button(groupPosition, SWT.CHECK);
		checkBoxColumn.setBounds(new org.eclipse.swt.graphics.Rectangle(8,42,45,16));
		checkBoxColumn.setText("column");
	}

	/**
	 * This method initializes groupColor	
	 *
	 */
	private void createGroupColor() {
		groupColor = new Group(this, SWT.NONE);
		groupColor.setText("Color");
		checkBoxForeground = new Button(groupColor, SWT.CHECK);
		checkBoxForeground.setBounds(new org.eclipse.swt.graphics.Rectangle(8,21,81,16));
		checkBoxForeground.setText("foreground");
		checkBoxBackground = new Button(groupColor, SWT.CHECK);
		checkBoxBackground.setBounds(new org.eclipse.swt.graphics.Rectangle(8,43,78,16));
		checkBoxBackground.setText("background");
	}

	/**
	 * This method initializes groupDecoration	
	 *
	 */
	private void createGroupDecoration() {
		groupDecoration = new Group(this, SWT.NONE);
		groupDecoration.setText("Decoration");
		checkBoxIntense = new Button(groupDecoration, SWT.CHECK);
		checkBoxIntense.setBounds(new org.eclipse.swt.graphics.Rectangle(8,20,46,16));
		checkBoxIntense.setText("bold");
		checkBoxReverse = new Button(groupDecoration, SWT.CHECK);
		checkBoxReverse.setBounds(new org.eclipse.swt.graphics.Rectangle(8,42,61,16));
		checkBoxReverse.setText("reverse");
		checkBoxUnderline = new Button(groupDecoration, SWT.CHECK);
		checkBoxUnderline.setBounds(new org.eclipse.swt.graphics.Rectangle(75,20,68,16));
		checkBoxUnderline.setText("underline");
		checkBoxBlink = new Button(groupDecoration, SWT.CHECK);
		checkBoxBlink.setBounds(new org.eclipse.swt.graphics.Rectangle(75,42,53,16));
		checkBoxBlink.setText("blink");
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
