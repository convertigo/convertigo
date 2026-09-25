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

package com.twinsoft.convertigo.eclipse.swt;

import org.eclipse.e4.ui.workbench.renderers.swt.CTabRendering;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Draws the part stacks flat, as the panels of Cursor or Visual Studio Code: each stack is separated from
 * the next ones by a line on its right and bottom edges, and its tabs from its content by a line. The tabs
 * of the editors are separated by lines, the selected one being on the background of the editor with a top
 * line of the Convertigo accent in the active stack; the selected tab of a view is underlined.
 */
public class ModernTabRendering extends CTabRendering {
	private static final Color ACCENT = new Color(0, 200, 247);
	private static final Color DARK_LINE = new Color(43, 43, 43);
	private static final Color LIGHT_LINE = new Color(229, 229, 229);
	private static final Color DARK_UNDERLINE = new Color(204, 204, 204);
	private static final Color LIGHT_UNDERLINE = new Color(64, 64, 64);

	public ModernTabRendering(CTabFolder parent) {
		super(parent);
	}

	@Override
	protected void draw(int part, int state, Rectangle bounds, GC gc) {
		super.draw(part, state, bounds, gc);
		var dark = SwtUtils.isDark();
		var size = parent.getSize();
		gc.setAlpha(255);
		gc.setLineWidth(1);
		if (part == PART_BODY) {
			gc.setForeground(dark ? DARK_LINE : LIGHT_LINE);
			gc.drawLine(size.x - 1, 0, size.x - 1, size.y - 1);
			gc.drawLine(0, size.y - 1, size.x - 1, size.y - 1);
			if (parent.getItemCount() > 0) {
				var header = parent.getItem(0).getBounds();
				int y = header.y + header.height;
				var selection = parent.getSelection();
				if (isEditorStack() && selection != null) {
					// the selected tab of an editor opens on its content
					var tab = selection.getBounds();
					gc.drawLine(0, y, tab.x - 1, y);
					gc.drawLine(tab.x + tab.width, y, size.x - 1, y);
				} else {
					gc.drawLine(0, y, size.x - 1, y);
				}
			}
		} else if (0 <= part && part < parent.getItemCount()) {
			boolean selected = (state & SWT.SELECTED) != 0;
			if (isEditorStack()) {
				gc.setForeground(dark ? DARK_LINE : LIGHT_LINE);
				gc.drawLine(bounds.x + bounds.width - 1, bounds.y, bounds.x + bounds.width - 1, bounds.y + bounds.height);
				if (selected && isActive()) {
					gc.setBackground(ACCENT);
					gc.fillRectangle(bounds.x, bounds.y, bounds.width - 1, 1);
				}
			} else if (selected) {
				gc.setBackground(isActive() ? ACCENT : dark ? DARK_UNDERLINE : LIGHT_UNDERLINE);
				gc.fillRectangle(bounds.x + 8, bounds.y + bounds.height - 2, bounds.width - 16, 1);
			}
		}
	}

	private boolean isEditorStack() {
		return hasCssClass("EditorStack");
	}

	private boolean isActive() {
		return hasCssClass("active");
	}

	private boolean hasCssClass(String name) {
		return parent.getData(SwtUtils.CSS_CLASS_KEY) instanceof String css && (" " + css + " ").contains(" " + name + " ");
	}
}
