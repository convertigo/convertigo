/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileInPlaceEditorInput;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.steps.SmartType;
import com.twinsoft.convertigo.beans.steps.SmartType.Mode;
import com.twinsoft.convertigo.eclipse.ColorEnum;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class SmartTypeCellEditor extends TextGenericCellEditor {

	public SmartTypeCellEditor(Composite parent, DatabaseObjectTreeObject databaseObjectTreeObject, PropertyDescriptor propertyDescriptor) {
		super(parent, SWT.MULTI, databaseObjectTreeObject, propertyDescriptor);
	}

	private SmartType value;
	private List<Button> buttons;

	private enum DataKeys {
		TEXT_COLOR,
		SMART_TYPE,
		EDITOR_CLASS
	}

	@Override
	protected Control createControl(Composite parent) {
		Composite control = (Composite) super.createControl(parent);
		
		FontData fontDefaultData = parent.getFont().getFontData()[0];
		fontDefaultData.setStyle(SWT.BOLD);
		fontDefaultData.setHeight(Math.round(fontDefaultData.getHeight() * 0.7f));
		Font fontTitle = new Font(parent.getDisplay(), fontDefaultData);
		
		text.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {
				doGetValue();
				super.focusLost(e);
			}
			
		});
		buttons = new ArrayList<Button>(3);
		SelectionListener selectionListener = new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				for (Button button : buttons) {
					button.setSelection(false);
				}

				Button button = (Button) e.widget; 
				button.setSelection(true);

				text.getDisplay().asyncExec(() -> {
					text.setBackground((Color) button.getData(DataKeys.TEXT_COLOR.name()));
					text.setForeground(text.getDisplay().getSystemColor(SWT.COLOR_BLACK));
					text.setFocus();
					int l = text.getText().length();
					text.setSelection(l, l);
				});
				
				Mode mode = (Mode) button.getData(DataKeys.SMART_TYPE.name());
				value.setMode(mode);
				text.setText(value.toStringContent());
				text.setEditable(value.isUseExpression());
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};

		Button button;
		button = new Button(control, SWT.TOGGLE | SWT.FLAT);
		button.setForeground(ColorEnum.DARK_YELLOW.get());
		button.setData(DataKeys.TEXT_COLOR.name(), ColorEnum.LIGHT_YELLOW.get());
		button.setData(DataKeys.SMART_TYPE.name(), SmartType.Mode.PLAIN);
		button.setData(DataKeys.EDITOR_CLASS.name(), TextEditorComposite.class);
		buttons.add(button);

		button = new Button(control, SWT.TOGGLE | SWT.FLAT);
		button.setForeground(ColorEnum.DARK_BLUE.get());
		button.setData(DataKeys.TEXT_COLOR.name(), ColorEnum.JAVASCRIPTABLE.get());
		button.setData(DataKeys.SMART_TYPE.name(), SmartType.Mode.JS);
		button.setData(DataKeys.EDITOR_CLASS.name(), TextEditorComposite.class);
		buttons.add(button);

		button = new Button(control, SWT.TOGGLE | SWT.FLAT);
		button.setForeground(ColorEnum.DARK_GREEN.get());
		button.setData(DataKeys.TEXT_COLOR.name(), ColorEnum.LIGHT_GREEN.get());
		button.setData(DataKeys.SMART_TYPE.name(), SmartType.Mode.SOURCE);
		button.setData(DataKeys.EDITOR_CLASS.name(), StepSourceEditorComposite.class);
		buttons.add(button);

		for (Button bt : buttons) {
			Mode mode = (Mode) bt.getData(DataKeys.SMART_TYPE.name());
			bt.setText(mode.label());
			bt.setToolTipText(mode.tooltip());
			bt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
			bt.setFont(fontTitle);
			bt.addSelectionListener(selectionListener);
			bt.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					openEditor();
				}
			});
		}

		return control;
	}

	@Override
	protected Object doGetValue() {
		if (value.isUseExpression()) {
			value.setExpression(text.getText());
		}
		value.pack();
		return value;
	}

	@Override
	protected void doSetValue(Object value) {
		this.value = ((SmartType) value).clone();
		Mode mode = this.value.getMode();

		for (Button button : buttons) {
			if (button.getData(DataKeys.SMART_TYPE.name()) == mode) {
				button.notifyListeners(SWT.Selection, null);
				break;
			}
		}
	}
	
	@Override
	protected void setEditable(boolean editable) {
		super.setEditable(editable);
		for (Button button: buttons) {
			button.setEnabled(editable);
		}
	}

	@Override
	protected void setNewValue(Object newValue) {
		if (newValue != null) {
			if (newValue instanceof String) {
				value.setExpression((String) newValue);
			} else {
				value.setSourceDefinition(GenericUtils.<XMLVector<String>>cast(newValue));
			}
			if (!text.isDisposed()) {
				text.setText(value.toStringContent());
			}
		}
		super.setNewValue(value);
	}
	
	@Override
	protected String editorInitValue() {
		return value.getExpression();
	}
	
	@Override
	protected void openEditor() {
		if (value.isUseSource()) {
			StepSourceEditorComposite[] editor = {null};
			Dialog dialog = new Dialog(PlatformUI.getWorkbench().getModalDialogShellProvider()) {
				@Override
				protected Control createDialogArea(Composite parent) {
					editor[0] = new StepSourceEditorComposite(parent, SWT.NONE, (Step) databaseObjectTreeObject.getObject(), value.getSourceDefinition());
					return editor[0];
				}
			};
			
			int userResponse = dialog.open();
			if (userResponse == Window.OK) {
				setNewValue(editor[0].getValue());
			}
		} else {
			super.openEditor();
		}
	}
	
	@Override
	protected FileInPlaceEditorInput getInput() {
		super.getInput();
		IFile file = input.getFile();
		if (value.getMode() == Mode.JS && "txt".equals(file.getFileExtension())) {
			file = ((IFolder) file.getParent()).getFile(file.getName().replaceFirst("\\.txt$", ".js"));
			input = new FileInPlaceEditorInput(file);
		} else if (value.getMode() == Mode.PLAIN && "js".equals(file.getFileExtension())) {
			file = ((IFolder) file.getParent()).getFile(file.getName().replaceFirst("\\.js$", ".txt"));
			input = new FileInPlaceEditorInput(file);
		}
		return input;
	}
}
