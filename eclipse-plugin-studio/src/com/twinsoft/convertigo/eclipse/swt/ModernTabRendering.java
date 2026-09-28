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
 * Draws each part stack as a rounded panel on the background of the window, with flat tabs: the
 * selected tab is a rounded pill, underlined with the Convertigo accent in the active part stack.
 */
public class ModernTabRendering extends CTabRendering {
	private static final int MARGIN = 3;
	private static final int PADDING = 4;
	private static final int RADIUS = 8;
	private static final Color ACCENT = new Color(0, 200, 247);

	public ModernTabRendering(CTabFolder parent) {
		super(parent);
	}

	@Override
	protected Rectangle computeTrim(int part, int state, int x, int y, int width, int height) {
		var trim = super.computeTrim(part, state, x, y, width, height);
		switch (part) {
		case PART_BORDER:
			trim.x -= MARGIN + PADDING;
			trim.width += 2 * (MARGIN + PADDING);
			trim.y -= MARGIN;
			trim.height += 2 * MARGIN;
			break;
		case PART_HEADER:
			trim.x -= MARGIN + PADDING;
			trim.width += 2 * (MARGIN + PADDING);
			break;
		case PART_BODY:
			trim.x -= MARGIN + PADDING;
			trim.width += 2 * (MARGIN + PADDING);
			trim.y -= MARGIN;
			trim.height += 2 * MARGIN + PADDING;
			break;
		default:
		}
		return trim;
	}

	@Override
	protected void draw(int part, int state, Rectangle bounds, GC gc) {
		switch (part) {
		case PART_BODY:
			// computes the shapes used to draw the tabs
			super.draw(part, state, bounds, gc);
			gc.setBackground(parent.getParent().getBackground());
			gc.fillRectangle(bounds);
			if (parent.getItemCount() == 0) {
				// an empty editor area shows the background of the window
				break;
			}
			gc.setAdvanced(true);
			gc.setAntialias(SWT.ON);
			gc.setBackground(parent.getBackground());
			gc.fillRoundRectangle(bounds.x + MARGIN, bounds.y + MARGIN, bounds.width - 2 * MARGIN, bounds.height - 2 * MARGIN, 2 * RADIUS, 2 * RADIUS);
			break;
		case PART_HEADER:
			break;
		default:
			super.draw(part, state, bounds, gc);
			if (0 <= part && part < parent.getItemCount()) {
				// the lines of the tabs reach the edges of the folder, outside of the panel
				var size = parent.getSize();
				gc.setBackground(parent.getParent().getBackground());
				gc.fillRectangle(0, MARGIN + RADIUS, MARGIN, size.y - 2 * (MARGIN + RADIUS));
				gc.fillRectangle(size.x - MARGIN, MARGIN + RADIUS, MARGIN, size.y - 2 * (MARGIN + RADIUS));
				if ((state & SWT.SELECTED) != 0) {
					drawPill(bounds, gc);
				}
			}
		}
	}

	private void drawPill(Rectangle bounds, GC gc) {
		gc.setAdvanced(true);
		gc.setAntialias(SWT.ON);
		var alpha = gc.getAlpha();
		gc.setAlpha(SwtUtils.isDark() ? 22 : 16);
		gc.setBackground(parent.getDisplay().getSystemColor(SwtUtils.isDark() ? SWT.COLOR_WHITE : SWT.COLOR_BLACK));
		gc.fillRoundRectangle(bounds.x + 2, bounds.y + 4, bounds.width - 4, bounds.height - 7, 12, 12);
		gc.setAlpha(alpha);
		if (isActive()) {
			gc.setBackground(ACCENT);
			gc.fillRoundRectangle(bounds.x + 10, bounds.y + bounds.height - 3, bounds.width - 20, 2, 2, 2);
		}
	}

	private boolean isActive() {
		return parent.getData(SwtUtils.CSS_CLASS_KEY) instanceof String css && (" " + css + " ").contains(" active ");
	}
}
