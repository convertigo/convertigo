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

import com.twinsoft.twinj.iJavelin;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;


public class JavelinAttributeEditorComposite extends AbstractDialogComposite {

	private Button checkBoxNone = null;
	private Group groupColor = null;
	private Group groupDecoration = null;
	private Button checkBoxText = null;
	private Combo comboText = null;
	private Button checkBoxBkgrd = null;
	private Combo comboBkgrd = null;
	private Composite compositeIntense = null;
	private Composite compositeReverse = null;
	private Composite compositeUnderlined = null;
	private Composite compositeBlink = null;
	private Button radioButtonIntenseOn = null;
	private Button radioButtonIntenseOff = null;
	private Button radioButtonIntenseDontCare = null;
	private Button radioButtonUnderlinedOn = null;
	private Button radioButtonUnderlinedOff = null;
	private Button radioButtonUnderlinedDontCare = null;
	private Button radioButtonReverseOn = null;
	private Button radioButtonReverseOff = null;
	private Button radioButtonReverseDontCare = null;
	private Button radioButtonBlinkOn = null;
	private Button radioButtonBlinkOff = null;
	private Button radioButtonBlinkDontCare = null;
	private Label label = null;
	public JavelinAttributeEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		initialize();
		
		String value = (String)cellEditor.databaseObjectTreeObject.getPropertyValue(cellEditor.propertyDescriptor.getId());
		int attribute = Integer.parseInt(value,10);
        if (attribute == -1) {
        	checkBoxNone.setSelection(true);
        	checkBoxText.setSelection(true);
        	comboText.select(0);
        	checkBoxBkgrd.setSelection(true);
        	comboBkgrd.select(0);
        	radioButtonIntenseOff.setSelection(true);
        	radioButtonReverseOff.setSelection(true);
        	radioButtonUnderlinedOff.setSelection(true);
        	radioButtonBlinkOff.setSelection(true);
        	setUIState(false);
        }
        else {
        	checkBoxNone.setSelection(false);
			
			if ((attribute & JavelinExtractionRule.DONT_CARE_FOREGROUND_ATTRIBUTE) != 0) {
				comboText.select(0);
				comboText.setEnabled(false);
				checkBoxText.setSelection(false);
			}
			else {
				checkBoxText.setSelection(true);
				comboText.select(attribute & iJavelin.AT_INK);
			}

			if ((attribute & JavelinExtractionRule.DONT_CARE_BACKGROUND_ATTRIBUTE) != 0) {
				comboBkgrd.select(0);
				comboBkgrd.setEnabled(false);
				checkBoxBkgrd.setSelection(false);
			}
			else {
				checkBoxBkgrd.setSelection(true);
				comboBkgrd.select((attribute & iJavelin.AT_PAPER) >> 3);
			}

			if ((attribute & JavelinExtractionRule.DONT_CARE_INTENSE_ATTRIBUTE) != 0) {
				radioButtonIntenseDontCare.setSelection(true);
			}
			else if ((attribute & iJavelin.AT_BOLD) != 0) {
				radioButtonIntenseOn.setSelection(true);
			}
			else {
				radioButtonIntenseOff.setSelection(true);
			}

			if ((attribute & JavelinExtractionRule.DONT_CARE_UNDERLINED_ATTRIBUTE) != 0) {
				radioButtonUnderlinedDontCare.setSelection(true);
			}
			else if ((attribute & iJavelin.AT_UNDERLINE) != 0) {
				radioButtonUnderlinedOn.setSelection(true);
			}
			else {
				radioButtonUnderlinedOff.setSelection(true);
			}

			if ((attribute & JavelinExtractionRule.DONT_CARE_REVERSE_ATTRIBUTE) != 0) {
				radioButtonReverseDontCare.setSelection(true);
			}
			else if ((attribute & iJavelin.AT_INVERT) != 0) {
				radioButtonReverseOn.setSelection(true);
			}
			else {
				radioButtonReverseOff.setSelection(true);
			}

			if ((attribute & JavelinExtractionRule.DONT_CARE_BLINK_ATTRIBUTE) != 0) {
				radioButtonBlinkDontCare.setSelection(true);
			}
			else if ((attribute & iJavelin.AT_BLINK) != 0) {
				radioButtonBlinkOn.setSelection(true);
			}
			else {
				radioButtonBlinkOff.setSelection(true);
			}
        }
	}

	private void initialize() {
		checkBoxNone = new Button(this, SWT.CHECK);
		checkBoxNone.setText("none");
		checkBoxNone
				.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
						setUIState(!((Button)e.getSource()).getSelection());
					}
				});
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		this.setLayout(gridLayout);
		label = new Label(this, SWT.NONE);
		label.setText("");
		createGroupColor();
		createGroupDecoration();
		this.setSize(new org.eclipse.swt.graphics.Point(253,241));
	}

	protected void setUIState(boolean bSelected) {
		checkBoxText.setEnabled(bSelected);
		comboText.setEnabled(bSelected && checkBoxText.getSelection());
		
		checkBoxBkgrd.setEnabled(bSelected);
		comboBkgrd.setEnabled(bSelected && checkBoxBkgrd.getSelection());

		radioButtonIntenseOn.setEnabled(bSelected);
		radioButtonIntenseOff.setEnabled(bSelected);
		radioButtonIntenseDontCare.setEnabled(bSelected);

		radioButtonReverseOn.setEnabled(bSelected);
		radioButtonReverseOff.setEnabled(bSelected);
		radioButtonReverseDontCare.setEnabled(bSelected);

		radioButtonUnderlinedOn.setEnabled(bSelected);
		radioButtonUnderlinedOff.setEnabled(bSelected);
		radioButtonUnderlinedDontCare.setEnabled(bSelected);

		radioButtonBlinkOn.setEnabled(bSelected);
		radioButtonBlinkOff.setEnabled(bSelected);
		radioButtonBlinkDontCare.setEnabled(bSelected);
	}
	
	public Object getValue() {
		int attribute = -1;
        if (!checkBoxNone.getSelection()) {
            attribute = 0;
            attribute |= (checkBoxText.getSelection() ? comboText.getSelectionIndex() : JavelinExtractionRule.DONT_CARE_FOREGROUND_ATTRIBUTE);
			attribute |= (checkBoxBkgrd.getSelection() ? (comboBkgrd.getSelectionIndex() << 3) : JavelinExtractionRule.DONT_CARE_BACKGROUND_ATTRIBUTE);
			attribute |= (radioButtonIntenseOn.getSelection() ? iJavelin.AT_BOLD : (radioButtonIntenseOff.getSelection() ? 0 : JavelinExtractionRule.DONT_CARE_INTENSE_ATTRIBUTE));
			attribute |= (radioButtonReverseOn.getSelection() ? iJavelin.AT_INVERT : (radioButtonReverseOff.getSelection() ? 0 : JavelinExtractionRule.DONT_CARE_REVERSE_ATTRIBUTE));
			attribute |= (radioButtonUnderlinedOn.getSelection() ? iJavelin.AT_UNDERLINE : (radioButtonUnderlinedOff.getSelection() ? 0 : JavelinExtractionRule.DONT_CARE_UNDERLINED_ATTRIBUTE));
			attribute |= (radioButtonBlinkOn.getSelection() ? iJavelin.AT_BLINK : (radioButtonBlinkOff.getSelection() ? 0 : JavelinExtractionRule.DONT_CARE_BLINK_ATTRIBUTE));
        }
        return new Integer(attribute).toString();
	}

	/**
	 * This method initializes groupColor	
	 *
	 */
	private void createGroupColor() {
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData.horizontalSpan = 2;
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		groupColor = new Group(this, SWT.NONE);
		groupColor.setText("Color");
		groupColor.setLayoutData(gridData);
		checkBoxText = new Button(groupColor, SWT.CHECK);
		checkBoxText.setBounds(new org.eclipse.swt.graphics.Rectangle(12,22,80,16));
		checkBoxText.setText("Foreground");
		checkBoxText
				.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
						comboText.setEnabled(((Button)e.getSource()).getSelection());
					}
				});
		createComboText();
		checkBoxBkgrd = new Button(groupColor, SWT.CHECK);
		checkBoxBkgrd.setBounds(new org.eclipse.swt.graphics.Rectangle(12,52,81,16));
		checkBoxBkgrd.setText("Background");
		checkBoxBkgrd
				.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
						comboBkgrd.setEnabled(((Button)e.getSource()).getSelection());
					}
				});
		createComboBkgrd();
	}

	/**
	 * This method initializes groupDecoration	
	 *
	 */
	private void createGroupDecoration() {
		GridData gridData1 = new org.eclipse.swt.layout.GridData();
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.horizontalSpan = 2;
		gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		groupDecoration = new Group(this, SWT.NONE);
		groupDecoration.setText("Decoration");
		createCompositeIntense();
		groupDecoration.setLayoutData(gridData1);
		createCompositeReverse();
		createCompositeUnderline();
		createCompositeBlink();
		
		
	}

	/**
	 * This method initializes comboText	
	 *
	 */
	private void createComboText() {
		comboText = new Combo(groupColor, SWT.NONE);
		comboText.setBounds(new org.eclipse.swt.graphics.Rectangle(100,22,93,21));
		comboText.setItems(new String[] { "black", "red", "green", "yellow", "blue", "magenta", "cyan", "white" });
	}

	/**
	 * This method initializes comboBkgrd	
	 *
	 */
	private void createComboBkgrd() {
		comboBkgrd = new Combo(groupColor, SWT.NONE);
		comboBkgrd.setBounds(new org.eclipse.swt.graphics.Rectangle(100,51,93,21));
		comboBkgrd.setItems(new String[] { "black", "red", "green", "yellow", "blue", "magenta", "cyan", "white" });
	}

	/**
	 * This method initializes compositeIntense	
	 *
	 */
	private void createCompositeIntense() {
		compositeIntense = new Composite(groupDecoration, SWT.NONE);
		compositeIntense.setBounds(new org.eclipse.swt.graphics.Rectangle(5,14,231,21));
		radioButtonIntenseOn = new Button(compositeIntense, SWT.RADIO);
		radioButtonIntenseOn.setBounds(new org.eclipse.swt.graphics.Rectangle(8,4,59,16));
		radioButtonIntenseOn.setText("bold");
		radioButtonIntenseOff = new Button(compositeIntense, SWT.RADIO);
		radioButtonIntenseOff.setBounds(new org.eclipse.swt.graphics.Rectangle(85,4,56,16));
		radioButtonIntenseOff.setText("normal");
		radioButtonIntenseDontCare = new Button(compositeIntense, SWT.RADIO);
		radioButtonIntenseDontCare.setBounds(new org.eclipse.swt.graphics.Rectangle(157,4,68,16));
		radioButtonIntenseDontCare.setText("don't care");
	}

	/**
	 * This method initializes compositeReverse	
	 *
	 */
	private void createCompositeReverse() {
		compositeReverse = new Composite(groupDecoration, SWT.NONE);
		compositeReverse.setBounds(new org.eclipse.swt.graphics.Rectangle(5,36,231,21));
		radioButtonReverseOn = new Button(compositeReverse, SWT.RADIO);
		radioButtonReverseOn.setBounds(new org.eclipse.swt.graphics.Rectangle(8,4,59,16));
		radioButtonReverseOn.setText("reverse");
		radioButtonReverseOff = new Button(compositeReverse, SWT.RADIO);
		radioButtonReverseOff.setBounds(new org.eclipse.swt.graphics.Rectangle(85,4,56,16));
		radioButtonReverseOff.setText("normal");
		radioButtonReverseDontCare = new Button(compositeReverse, SWT.RADIO);
		radioButtonReverseDontCare.setBounds(new org.eclipse.swt.graphics.Rectangle(157,4,68,16));
		radioButtonReverseDontCare.setText("don't care");
	}

	/**
	 * This method initializes compositeUnderline	
	 *
	 */
	private void createCompositeUnderline() {
		compositeUnderlined = new Composite(groupDecoration, SWT.NONE);
		compositeUnderlined.setBounds(new org.eclipse.swt.graphics.Rectangle(5,59,231,21));
		radioButtonUnderlinedOn = new Button(compositeUnderlined, SWT.RADIO);
		radioButtonUnderlinedOn.setBounds(new org.eclipse.swt.graphics.Rectangle(8,4,71,16));
		radioButtonUnderlinedOn.setText("underlined");
		radioButtonUnderlinedOff = new Button(compositeUnderlined, SWT.RADIO);
		radioButtonUnderlinedOff.setBounds(new org.eclipse.swt.graphics.Rectangle(86,4,54,16));
		radioButtonUnderlinedOff.setText("normal");
		radioButtonUnderlinedDontCare = new Button(compositeUnderlined, SWT.RADIO);
		radioButtonUnderlinedDontCare.setBounds(new org.eclipse.swt.graphics.Rectangle(157,4,68,16));
		radioButtonUnderlinedDontCare.setText("don't care");
	}

	/**
	 * This method initializes compositeBlink	
	 *
	 */
	private void createCompositeBlink() {
		compositeBlink = new Composite(groupDecoration, SWT.NONE);
		compositeBlink.setBounds(new org.eclipse.swt.graphics.Rectangle(5,82,231,21));
		radioButtonBlinkOn = new Button(compositeBlink, SWT.RADIO);
		radioButtonBlinkOn.setBounds(new org.eclipse.swt.graphics.Rectangle(8,4,43,16));
		radioButtonBlinkOn.setText("blink");
		radioButtonBlinkOff = new Button(compositeBlink, SWT.RADIO);
		radioButtonBlinkOff.setBounds(new org.eclipse.swt.graphics.Rectangle(86,4,57,16));
		radioButtonBlinkOff.setText("normal");
		radioButtonBlinkDontCare = new Button(compositeBlink, SWT.RADIO);
		radioButtonBlinkDontCare.setBounds(new org.eclipse.swt.graphics.Rectangle(157,4,68,16));
		radioButtonBlinkDontCare.setText("don't care");
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
