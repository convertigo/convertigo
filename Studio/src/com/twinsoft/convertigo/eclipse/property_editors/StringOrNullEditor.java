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
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;


public class StringOrNullEditor extends AbstractDialogCellEditor implements INullEditor {

	private Boolean isNull = false;
	private Composite editor;
	private Button buttonNullCtrl;
	private Text textCtrl;
	
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
	}
	
	@Override
	protected Control createControl(Composite parent) {
		Font font = parent.getFont();
        Color bg = parent.getBackground();
 
        editor = new Composite(parent, getStyle());
        editor.setFont(font);
        editor.setBackground(bg);
        
        GridLayout gl = new GridLayout(99, false);
		gl.horizontalSpacing = gl.marginHeight = gl.marginWidth = gl.verticalSpacing = 0;
        editor.setLayout(gl);

        textCtrl = new Text(editor, SWT.NONE);
        textCtrl.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent keyEvent) {
				keyReleaseOccured(keyEvent);
			}
			public void keyReleased(KeyEvent keyEvent) {
				if (!isNull) {
					if ((keyEvent.stateMask & SWT.CTRL) != 0) {
						if (keyEvent.keyCode == 99)
							performCopy();		// CTRL+C
						if (keyEvent.keyCode == 120)
							performCut();		// CTRL+X
						if (keyEvent.keyCode == 118)
							performPaste();		// CTRL+V
			        }
				}
			}});
        
        textCtrl.addTraverseListener(new TraverseListener () {
    		public void keyTraversed(TraverseEvent e) {
    			switch (e.detail) {
    				case SWT.TRAVERSE_TAB_NEXT:
    				case SWT.TRAVERSE_TAB_PREVIOUS: {
    					e.doit = false;
    				}
    			}
    		}
    	});
        textCtrl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    	buttonNullCtrl = new Button(editor, SWT.TOGGLE | SWT.FLAT);
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
        
        buttonNullCtrl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
        return editor;
	}

	private void handleButtonSelected() {
		isNull = buttonNullCtrl.getSelection();
		textCtrl.setText(isNull?"null":"");
		textCtrl.setEnabled(!isNull);
		fireApplyEditorValue();
		deactivate();		
	}
	
	@Override
	protected Object doGetValue() {
		if (isNull) {
			return null;
		}
		return textCtrl.getText();
	}
	
	@Override
	protected void doSetValue(Object value) {
		if (value == null) {
			isNull = true;
			textCtrl.setText("");
		} else {
			isNull = false;
			textCtrl.setText(value.toString());
		}
	}

	@Override
	protected void doSetFocus() {
		if (isNull) {
			buttonNullCtrl.setFocus();
		} else {
			textCtrl.setFocus();
		}
	}
	
	@Override
	public void activate() {
		super.activate();
	    textCtrl.setEnabled(!isNull);
	    buttonNullCtrl.setSelection(isNull);
	}
}
