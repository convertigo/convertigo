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
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;

import com.twinsoft.convertigo.beans.common.XMLVector;

//public class ArrayOrNullEditor extends TableEditor implements INullEditor {
public class ArrayOrNullEditor extends ArrayEditor implements INullEditor {

	private Boolean isNull = false;
	private Button buttonOpenCtrl;
	private Button buttonNullCtrl;
	private Label labelCtrl;
	private Composite editor;
	private Object value;
	
	public ArrayOrNullEditor(Composite parent) {
		this(parent, SWT.NONE);
	}
	
	public ArrayOrNullEditor(Composite parent, int style) {
		super(parent, style);
		
        dialogTitle = "Array";
        columnNames = new String[] { "Value" };
        templateData = new Object[] { "val" };
        columnSizes = new int[] { 200 };
	}

	public Boolean isNullProperty() {
		return isNull;
	}
	
	public void setNullProperty(Boolean isNull) {
		this.isNull = isNull;
	}

	@Override
	protected Control createControl(Composite parent) {
		editor = (Composite)super.createControl(parent);
		editor.setLayout(new DialogCellLayout());
		
        buttonNullCtrl = new Button(editor, SWT.TOGGLE);
        buttonNullCtrl.setToolTipText("Set/unset Null value");
        buttonNullCtrl.setText("X");
        
        buttonNullCtrl.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
				handleButtonSelected();
			}
			public void widgetSelected(SelectionEvent arg0) {
				handleButtonSelected();
			}});
        
        buttonNullCtrl.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent keyEvent) {
			}
			public void keyReleased(KeyEvent keyEvent) {
				if (keyEvent.character == '\u001b' || 	// Escape character
					keyEvent.character == '\r') { 		// Return key
					if (isNull) fireCancelEditor();
		        }
			}});
		return editor;
	}

	@Override
	protected Button createButton(Composite parent) {
		buttonOpenCtrl = super.createButton(parent);
		return buttonOpenCtrl;
	}

	@Override
	protected Control createContents(Composite cell) {
		labelCtrl = (Label)super.createContents(cell);
		return labelCtrl;
	}

	@Override
	protected Object doGetValue() {
		value = super.doGetValue();
		return value;
	}
	
	@Override
	protected void doSetValue(Object value) {
		this.value = value;
		super.doSetValue(value);
	}
	
	@Override
	protected void doSetFocus() {
		if (isNull)
			buttonNullCtrl.setFocus();
		else
			super.doSetFocus();
	}

	@Override
	public void activate() {
		super.activate();
	    labelCtrl.setEnabled(!isNull);
	    buttonOpenCtrl.setEnabled(!isNull);
	    buttonNullCtrl.setSelection(isNull);
	}

	@Override
	public Object getEditorData() {
		return value;
	}
	
	private void handleButtonSelected() {
		isNull = buttonNullCtrl.getSelection();
		labelCtrl.setEnabled(!isNull);
		buttonOpenCtrl.setEnabled(!isNull);
		XMLVector<Object> xmlv = new XMLVector<Object>();
		if (isNull) xmlv.add("null");
		doSetValue(xmlv);
		fireApplyEditorValue();
		deactivate();		
	}
	
	private class DialogCellLayout extends Layout {
		public void layout(Composite editor, boolean force) {
			Rectangle bounds = editor.getClientArea();
			Point buttonOpenSize = buttonOpenCtrl.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
	        Point buttonNullSize = buttonNullCtrl.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
	        if (labelCtrl != null) {
	        	labelCtrl.setBounds(0, 0, bounds.width - (buttonOpenSize.x + buttonNullSize.x), bounds.height);
	        }
	        buttonOpenCtrl.setBounds(bounds.width - (buttonOpenSize.x + buttonNullSize.x), 0, buttonOpenSize.x, bounds.height);
	        buttonNullCtrl.setBounds(bounds.width - buttonNullSize.x, 0, buttonNullSize.x, bounds.height);
		}
	 
        public Point computeSize(Composite editor, int wHint, int hHint, boolean force) {
             if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT) {
                 return new Point(wHint, hHint);
             }
             Point textSize = labelCtrl.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
             Point buttonNullSize = buttonNullCtrl.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
             
             // Just return the button width to ensure the button is not clipped if the text is long.
             // The text will just use whatever extra width there is
             Point result = new Point(buttonNullSize.x, Math.max(textSize.y, buttonNullSize.y));
             return result;
         }
    }
}
