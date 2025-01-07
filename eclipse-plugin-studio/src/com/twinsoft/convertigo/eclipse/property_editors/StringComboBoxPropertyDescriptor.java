/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import java.lang.reflect.Method;
import java.text.MessageFormat;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

public class StringComboBoxPropertyDescriptor extends PropertyDescriptor {

	private DatabaseObjectTreeObject databaseObjectTreeObject;

	public StringComboBoxPropertyDescriptor(Object id, String displayName, DatabaseObjectTreeObject databaseObjectTreeObject) {
		super(id, displayName);
		this.databaseObjectTreeObject = databaseObjectTreeObject;
	}

	public CellEditor createPropertyEditor(Composite parent) {
		DatabaseObject dbo = databaseObjectTreeObject.getObject();
		boolean isReadOnly = false;
		try {
			Method method = dbo.getClass().getMethod("isReadOnlyProperty", new Class[] { String.class});
			isReadOnly = (boolean) method.invoke(dbo, new Object[] { (String) getId() });
		} catch (Exception e) {}
		CellEditor editor = new StringComboBoxCellEditor(parent, isReadOnly ? SWT.READ_ONLY:SWT.NONE);
		if (getValidator() != null) {
			editor.setValidator(getValidator());
		}
		return editor;
	}

	class StringComboBoxCellEditor extends CellEditor {
		String[] items;
		CCombo comboBox;

		public StringComboBoxCellEditor(Composite parent, int style) {
			super(parent, style);
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

			comboBox.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					if (!isCorrect(comboBox.getText())) {
						setValueValid(false);
						setErrorMessage(MessageFormat.format(getErrorMessage(),
								new Object[] { comboBox.getText() }));
					} else {
						setValueValid(true);
						setErrorMessage(null);
					}
				}
			});

			comboBox.addVerifyListener(new VerifyListener() {
				@Override
				public void verifyText(VerifyEvent e) {
					String oldS = comboBox.getText();
					String newS = oldS.substring(0, e.start) + e.text + oldS.substring(e.end);
					boolean oldValidState = isValueValid();
					if (!isCorrect(newS)) {
						setValueValid(false);
						setErrorMessage(MessageFormat.format(getErrorMessage(),
								new Object[] { newS }));
						valueChanged(oldValidState, false);
					} else {
						setValueValid(true);
						setErrorMessage(null);
						valueChanged(oldValidState, true);
					}
				}
			});

			comboBox.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					StringComboBoxCellEditor.this.focusLost();
				}
			});
			return comboBox;
		}

		@SuppressWarnings("deprecation")
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
			items = getTags(databaseObjectTreeObject, (String) getId());
			if (comboBox != null && items != null) {
				comboBox.removeAll();
				for (int i = 0; i < items.length; i++) {
					comboBox.add(items[i], i);
				}
				setValueValid(true);
			}
		}

		void applyEditorValueAndDeactivate() {
			// must set the selection before getting value
			int selection = comboBox.getSelectionIndex();
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

	public static String[] getTags(DatabaseObjectTreeObject databaseObjectTreeObject, String propertyName) {
		DatabaseObject bean = (DatabaseObject) databaseObjectTreeObject.getObject();
		ITagsProperty tagsProperty = null;

		if (bean instanceof ITagsProperty) {
			tagsProperty = (ITagsProperty) bean;
		}
		else {
			return new String[] { "" };
		}

		String[] sResults = tagsProperty.getTagsForProperty(propertyName);
		return sResults;
	}
}
