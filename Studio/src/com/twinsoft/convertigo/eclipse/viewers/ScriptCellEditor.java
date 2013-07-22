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

package com.twinsoft.convertigo.eclipse.viewers;

import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

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
	protected Control createControl(Composite parent) {
		super.createControl(parent);
        text.setFont(getFont());
        text.setBackground(getBackground());		
		return text;
	}

	/**
	 * The background color to use for this cell editor.
	 */
	private Color background = null;
	
	private Color getBackground() {
		if (background == null) {
			background = new Color(Display.getCurrent(), 162, 194, 250);
		}
		return background;
	}

	/**
	 * The font to use for this cell editor.
	 */
	private Font font = null;
	
	private Font getFont() {
		if (font == null) {
			int fontSize;
			try {
				fontSize = Display.getCurrent().getSystemFont().getFontData()[0].getHeight();
			} catch (Exception e) {
				fontSize = 10;
			}
			font = new Font(Display.getCurrent(), "lucida console", fontSize, SWT.NONE);
		}
		return font;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.CellEditor#dispose()
	 */
	public void dispose() {
		if (background != null) {
			background.dispose();
		}
		if (font != null) {
			font.dispose();
		}
		super.dispose();
	}
}
