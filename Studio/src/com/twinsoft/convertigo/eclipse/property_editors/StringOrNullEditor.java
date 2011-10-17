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

import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;


public class StringOrNullEditor extends TextCellEditor implements INullEditor {

	private Boolean isNull = false;
	private Composite editor;
	private Button buttonNullCtrl;
	private Text textCtrl;
	private Object value;
	
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
        editor.setLayout(new DialogCellLayout());

        textCtrl = (Text)super.createControl(editor);
        textCtrl.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent keyEvent) {
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

	private void handleButtonSelected() {
		isNull = buttonNullCtrl.getSelection();
		textCtrl.setText(isNull?"null":"");
		textCtrl.setEnabled(!isNull);
		fireApplyEditorValue();
		deactivate();		
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
	protected void focusLost() {
		super.focusLost();
	}
	
	@Override
	public void activate() {
		super.activate();
	    textCtrl.setEnabled(!isNull);
	    buttonNullCtrl.setSelection(isNull);
	}

	private class DialogCellLayout extends Layout {
		public void layout(Composite editor, boolean force) {
			Rectangle bounds = editor.getClientArea();
	        Point buttonNullSize = buttonNullCtrl.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
	        if (textCtrl != null) {
	        	textCtrl.setBounds(0, 0, bounds.width - buttonNullSize.x, bounds.height);
	        }
	        buttonNullCtrl.setBounds(bounds.width - buttonNullSize.x, 0, buttonNullSize.x, bounds.height);
		}
	 
        public Point computeSize(Composite editor, int wHint, int hHint, boolean force) {
             if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT) {
                 return new Point(wHint, hHint);
             }
             Point textSize = textCtrl.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
             Point buttonNullSize = buttonNullCtrl.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
             
             // Just return the button width to ensure the button is not clipped if the text is long.
             // The text will just use whatever extra width there is
             Point result = new Point(buttonNullSize.x, Math.max(textSize.y, buttonNullSize.y));
             return result;
         }
    }

}
