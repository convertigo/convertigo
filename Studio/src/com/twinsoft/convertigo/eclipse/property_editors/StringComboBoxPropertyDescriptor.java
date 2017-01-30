/*
 * Copyright (c) 2001-2016 Convertigo SA.
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

import java.text.MessageFormat;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class StringComboBoxPropertyDescriptor extends PropertyDescriptor {

	private String[] labels;
	private boolean readOnly = false;
	
	public StringComboBoxPropertyDescriptor(Object id, String displayName, String[] labels, boolean readOnly) {
		super(id, displayName);
		this.labels = labels;
		this.readOnly = readOnly;
	}

    public CellEditor createPropertyEditor(Composite parent) {
    	CellEditor editor = new StringComboBoxCellEditor(parent, labels, readOnly ? SWT.READ_ONLY:SWT.NONE);
        return editor;
    }
	
    class StringComboBoxCellEditor extends CellEditor {
    	String[] items;
    	CCombo comboBox;
    	int selection;
    	
    	public StringComboBoxCellEditor(Composite parent, String[] items, int style) {
    		super(parent, style);
    		this.items = items;
    		populateComboBoxItems();
    	}
    	
		@Override
		public void activate(ColumnViewerEditorActivationEvent activationEvent) {
			super.activate(activationEvent);
			boolean dropDown = true;
			if (dropDown) {
				getControl().getDisplay().asyncExec(new Runnable() {
					public void run() {
						((CCombo) getControl()).setListVisible(true);
					}
				});

			}
		}

		@Override
		protected Control createControl(Composite parent) {
			comboBox = new CCombo(parent, getStyle());
			comboBox.setFont(parent.getFont());

			populateComboBoxItems();

			comboBox.addKeyListener(new KeyAdapter() {
				// hook key pressed - see PR 14201
				@Override
				public void keyPressed(KeyEvent e) {
					keyReleaseOccured(e);
				}
			});

			comboBox.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent event) {
					applyEditorValueAndDeactivate();
				}

				@Override
				public void widgetSelected(SelectionEvent event) {
					selection = comboBox.getSelectionIndex();
				}
			});

			comboBox.addTraverseListener(new TraverseListener() {
				@Override
				public void keyTraversed(TraverseEvent e) {
					if (e.detail == SWT.TRAVERSE_ESCAPE
							|| e.detail == SWT.TRAVERSE_RETURN) {
						e.doit = false;
					}
				}
			});

			comboBox.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					StringComboBoxCellEditor.this.focusLost();
				}
			});
			return comboBox;		}
		
		public LayoutData getLayoutData() {
			LayoutData layoutData = super.getLayoutData();
			if ((comboBox == null) || comboBox.isDisposed()) {
				layoutData.minimumWidth = 60;
			} else {
				// make the comboBox 10 characters wide
				GC gc = new GC(comboBox);
				layoutData.minimumWidth = (gc.getFontMetrics()
						.getAverageCharWidth() * 10) + 10;
				gc.dispose();
			}
			return layoutData;
		}

		private void populateComboBoxItems() {
			if (comboBox != null && items != null) {
				comboBox.removeAll();
				for (int i = 0; i < items.length; i++) {
					comboBox.add(items[i], i);
				}

				setValueValid(true);
				selection = 0;
			}
		}
	
		void applyEditorValueAndDeactivate() {
			// must set the selection before getting value
			selection = comboBox.getSelectionIndex();
			Object newValue = doGetValue();
			markDirty();
			boolean isValid = isCorrect(newValue);
			setValueValid(isValid);

			if (!isValid) {
				// Only format if the 'index' is valid
				if (items.length > 0 && selection >= 0 && selection < items.length) {
					// try to insert the current value into the error message.
					setErrorMessage(MessageFormat.format(getErrorMessage(),
							new Object[] { items[selection] }));
				} else {
					// Since we don't have a valid index, assume we're using an
					// 'edit'
					// combo so format using its text value
					setErrorMessage(MessageFormat.format(getErrorMessage(),
							new Object[] { comboBox.getText() }));
				}
			}

			fireApplyEditorValue();
			deactivate();
		}
		
		protected void keyReleaseOccured(KeyEvent keyEvent) {
			if (keyEvent.character == '\u001b') { // Escape character
				fireCancelEditor();
			} else if (keyEvent.character == '\t') { // tab key
				applyEditorValueAndDeactivate();
			}
		}
		
		protected void focusLost() {
			if (isActivated()) {
				applyEditorValueAndDeactivate();
			}
		}
		
		@Override
		protected void doSetFocus() {
			comboBox.setFocus();
		}

		@Override
		protected Object doGetValue() {
			String text = comboBox.getText();
			return text;
		}

		@Override
		protected void doSetValue(Object value) {
			String text = value.toString();
			comboBox.setText(text);
		}
    	
    }
}
