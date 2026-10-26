/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.eclipse.ColorEnum;

/**
 * A dialog cell editor whose value is also typed in place: a JavaScript expression, with a dialog that helps
 * writing it.
 */
public abstract class InlineDialogCellEditor extends AbstractDialogCellEditor {
	protected Text text;

	InlineDialogCellEditor(Composite parent) {
		super(parent);
	}

	@Override
	protected Control createContents(Composite cell) {
		text = new Text(cell, SWT.SINGLE);
		text.setFont(cell.getFont());
		text.setForeground(ColorEnum.BLACK.get());
		text.setBackground(ColorEnum.JAVASCRIPTABLE.get());
		text.addTraverseListener(e -> {
			if (e.detail == SWT.TRAVERSE_RETURN) {
				e.doit = false;
				markDirty();
				fireApplyEditorValue();
				deactivate();
			} else if (e.detail == SWT.TRAVERSE_ESCAPE) {
				e.doit = false;
				fireCancelEditor();
			}
		});
		text.addModifyListener(e -> {
			markDirty();
			valueChanged(true, true);
		});
		text.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				text.getDisplay().asyncExec(() -> {
					Control control = getControl();
					if (text.isDisposed() || control == null || control.isDisposed() || !isActivated()) {
						return;
					}
					for (Control focus = text.getDisplay().getFocusControl(); focus != null; focus = focus.getParent()) {
						if (focus == control) {
							return;
						}
					}
					InlineDialogCellEditor.this.focusLost();
				});
			}
		});
		return text;
	}

	@Override
	protected void updateContents(Object value) {
		if (text != null && !text.isDisposed()) {
			String s = value == null ? "" : value.toString();
			if (!s.equals(text.getText())) {
				text.setText(s);
			}
		}
	}

	@Override
	protected Object doGetValue() {
		return text != null && !text.isDisposed() ? text.getText() : super.doGetValue();
	}

	@Override
	protected void doSetFocus() {
		if (text != null && !text.isDisposed()) {
			text.setFocus();
			text.selectAll();
		}
	}

	/** the value of the property, as the dialog starts from it: the text typed in place */
	@Override
	public Object getEditorData() {
		return text != null && !text.isDisposed() ? text.getText() : super.getEditorData();
	}
}
