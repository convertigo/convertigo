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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;

/**
 * Draws the rows of a tree taller, with a rounded selection and a highlight under the mouse instead of
 * the native ones. The selection is tinted with the Convertigo accent while the tree has the focus.
 */
public class ModernTree {
	private static final String KEY = "c8oModernTree";
	private static final int ROW_HEIGHT = 26;
	private static final Color DARK_FOCUSED = new Color(24, 72, 92);
	private static final Color DARK_SELECTED = new Color(50, 53, 58);
	private static final Color DARK_HOVER = new Color(42, 44, 48);
	private static final Color LIGHT_FOCUSED = new Color(210, 241, 252);
	private static final Color LIGHT_SELECTED = new Color(228, 230, 234);
	private static final Color LIGHT_HOVER = new Color(243, 244, 246);

	private ModernTree() {
	}

	public static void install(Tree tree) {
		if (tree.getData(KEY) != null) {
			return;
		}
		tree.setData(KEY, true);
		int[] hotRow = {-1};
		Listener hover = e -> {
			int y = -1;
			var top = tree.getTopItem();
			if (e.type == SWT.MouseMove && top != null) {
				int first = top.getBounds().y;
				int height = tree.getItemHeight();
				if (e.y >= first) {
					y = first + (e.y - first) / height * height;
				}
			}
			if (y != hotRow[0]) {
				hotRow[0] = y;
				tree.redraw();
			}
		};
		tree.addListener(SWT.MouseMove, hover);
		tree.addListener(SWT.MouseExit, hover);
		tree.addListener(SWT.FocusIn, e -> tree.redraw());
		tree.addListener(SWT.FocusOut, e -> tree.redraw());
		tree.addListener(SWT.MeasureItem, e -> e.height = Math.max(e.height, ROW_HEIGHT));
		tree.addListener(SWT.EraseItem, e -> {
			boolean selected = (e.detail & SWT.SELECTED) != 0;
			boolean hot = !selected && e.y == hotRow[0];
			e.detail &= ~(SWT.SELECTED | SWT.HOT);
			if (selected || hot) {
				boolean dark = SwtUtils.isDark();
				var area = tree.getClientArea();
				e.gc.setAdvanced(true);
				e.gc.setAntialias(SWT.ON);
				e.gc.setBackground(hot ? (dark ? DARK_HOVER : LIGHT_HOVER)
						: tree.isFocusControl() ? (dark ? DARK_FOCUSED : LIGHT_FOCUSED) : (dark ? DARK_SELECTED : LIGHT_SELECTED));
				e.gc.fillRoundRectangle(area.x + 4, e.y + 1, area.width - 8, e.height - 2, 12, 12);
			}
		});
		tree.redraw();
	}
}
