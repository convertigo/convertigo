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

import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import com.twinsoft.convertigo.eclipse.ColorEnum;

public class ScriptCellEditor extends TextCellEditor {

	public ScriptCellEditor() {
	}

	public ScriptCellEditor(Composite parent) {
		super(parent);
	}

	public ScriptCellEditor(Composite parent, int style) {
		super(parent, style);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.TextCellEditor#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createControl(Composite parent) {
		var control = super.createControl(parent);
		control.addDisposeListener(e -> dispose());
		parent.getDisplay().asyncExec(() -> {
			text.setFont(getFont());
			text.setForeground(text.getDisplay().getSystemColor(SWT.COLOR_BLACK));
			text.setBackground(getBackground());
		});
		return text;
	}

	/**
	 * The background color to use for this cell editor.
	 */
	
	private Color getBackground() {
		return ColorEnum.JAVASCRIPTABLE.get();
	}

	/**
	 * The font to use for this cell editor.
	 */
	private Font font = null;
	
	private Font getFont() {
		if (font == null) {
			font = new Font(Display.getCurrent(), "courier new", 8, SWT.BOLD);
		}
		return font;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.CellEditor#dispose()
	 */
	@Override
	public void dispose() {
		if (font != null) {
			font.dispose();
		}
		super.dispose();
	}
	
	
}
