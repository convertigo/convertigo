/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.property_editors;

import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;


public class StringOrNullEditor extends TextCellEditor implements INullEditor {

	private Boolean isNull = false;
	private Boolean wasNull = false;
	private Composite editor;
	private Button buttonNullCtrl;
	
	public StringOrNullEditor(Composite parent) {
		this(parent, SWT.NONE);
	}

	public StringOrNullEditor(Composite parent, int style) {
		super(parent, style);
	}

	public Boolean isNullProperty() {
		return isNull;
	}
	
	public void setNullProperty(Boolean isNull) {
		this.isNull = isNull;
		wasNull = isNull;
	}
	
	@Override
	protected Control createControl(Composite parent) {
		Font font = parent.getFont();
        Color bg = parent.getBackground();
 
        editor = new Composite(parent, getStyle()) {

			@Override
			public boolean isFocusControl() {
				return true;
			}
        	
        };
        editor.setFont(font);
        editor.setBackground(bg);
        
        GridLayout gl = new GridLayout(99, false);
		gl.horizontalSpacing = gl.marginHeight = gl.marginWidth = gl.verticalSpacing = 0;
        editor.setLayout(gl);

        super.createControl(editor);
        
        text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    	buttonNullCtrl = new Button(editor, SWT.PUSH);
        buttonNullCtrl.setToolTipText("Set null value");
        buttonNullCtrl.setText("null");
        
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
        
        buttonNullCtrl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
        return editor;
	}

	private void handleButtonSelected() {
		text.setText("");
		isNull = true;
		fireApplyEditorValue();
		deactivate();		
	}
	
	@Override
	protected Object doGetValue() {
		wasNull = isNull;
		if (isNull) {
			return "<value is null>";
		}
		return text.getText();
	}
	
	@Override
	protected void doSetValue(Object value) {
		if (wasNull || value == null) {
			isNull = true;
			text.setText("");
		} else {
			text.setText(value.toString());
		}
	}
	
	@Override
	public void activate() {
		super.activate();
		isNull = false;
	}
	
}
