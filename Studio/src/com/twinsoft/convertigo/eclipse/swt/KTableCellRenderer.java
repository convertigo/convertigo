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

package com.twinsoft.convertigo.eclipse.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class KTableCellRenderer {
	/*******************************************************************************
	 * Copyright (C) 2004 by Friederich Kupzog Elektronik & Software All rights
	 * reserved. This program and the accompanying materials are made available
	 * under the terms of the Eclipse Public License v1.0 which accompanies this
	 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
	 * 
	 * Contributors: Friederich Kupzog - initial API and implementation
	 * fkmk@kupzog.de www.kupzog.de/fkmk
	 ******************************************************************************/
	  public static KTableCellRenderer defaultRenderer = new KTableCellRenderer();

	  /**
	   * 
	   */
	  protected Display m_Display;

	  public KTableCellRenderer() {
	    m_Display = Display.getCurrent();
	  }

	  /**
	   * Returns the optimal width of the given cell (used by column resizing)
	   * 
	   * @param col
	   * @param row
	   * @param content
	   * @param fixed
	   * @return int
	   */
	  public int getOptimalWidth(GC gc, int col, int row, Object content,
	      boolean fixed) {
	    return gc.stringExtent(content.toString()).x + 8;
	  }

	  /**
	   * Standard implementation for CellRenderer. Draws a cell at the given
	   * position. Uses the .getString() method of content to get a String
	   * representation to draw.
	   * 
	   * @param gc
	   *            The gc to draw on
	   * @param rect
	   *            The coordinates and size of the cell (add 1 to width and hight
	   *            to include the borders)
	   * @param col
	   *            The column
	   * @param row
	   *            The row
	   * @param content
	   *            The content of the cell (as given by the table model)
	   * @param focus
	   *            True if the cell is selected
	   * @param fixed
	   *            True if the cell is fixed (unscrollable header cell)
	   * @param clicked
	   *            True if the cell is currently clicked (useful e.g. to paint a
	   *            pressed button)
	   */
	  public void drawCell(GC gc, Rectangle rect, int col, int row,
	      Object content, boolean focus, boolean fixed, boolean clicked) {
	    if (fixed) {

	      rect.height += 1;
	      rect.width += 1;
	      gc.setForeground(Display.getCurrent().getSystemColor(
	          SWT.COLOR_LIST_FOREGROUND));
	      if (clicked) {
	        SWTX
	            .drawButtonDown(gc, content.toString(),
	                SWTX.ALIGN_HORIZONTAL_LEFT
	                    | SWTX.ALIGN_VERTICAL_CENTER, null,
	                SWTX.ALIGN_HORIZONTAL_RIGHT
	                    | SWTX.ALIGN_VERTICAL_CENTER, rect);
	      } else {
	        SWTX
	            .drawButtonUp(gc, content.toString(),
	                SWTX.ALIGN_HORIZONTAL_LEFT
	                    | SWTX.ALIGN_VERTICAL_CENTER, null,
	                SWTX.ALIGN_HORIZONTAL_RIGHT
	                    | SWTX.ALIGN_VERTICAL_CENTER, rect);
	      }

	      return;
	    }

	    Color textColor;
	    Color backColor;
	    Color vBorderColor;
	    Color hBorderColor;

	    if (focus) {
	      textColor = m_Display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
	      backColor = (m_Display.getSystemColor(SWT.COLOR_LIST_SELECTION));
	      vBorderColor = m_Display.getSystemColor(SWT.COLOR_LIST_SELECTION);
	      hBorderColor = m_Display.getSystemColor(SWT.COLOR_LIST_SELECTION);
	    } else {
	      textColor = m_Display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);
	      backColor = m_Display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
	      vBorderColor = m_Display
	          .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
	      hBorderColor = m_Display
	          .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
	    }

	    gc.setForeground(hBorderColor);
	    gc.drawLine(rect.x, rect.y + rect.height, rect.x + rect.width, rect.y
	        + rect.height);

	    gc.setForeground(vBorderColor);
	    gc.drawLine(rect.x + rect.width, rect.y, rect.x + rect.width, rect.y
	        + rect.height);

	    gc.setBackground(backColor);
	    gc.setForeground(textColor);

	    gc.fillRectangle(rect);

	    SWTX.drawTextImage(gc, content.toString(), SWTX.ALIGN_HORIZONTAL_CENTER
	        | SWTX.ALIGN_VERTICAL_CENTER, null,
	        SWTX.ALIGN_HORIZONTAL_CENTER | SWTX.ALIGN_VERTICAL_CENTER,
	        rect.x + 3, rect.y, rect.width - 3, rect.height);

	  }

}
