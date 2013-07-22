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

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.ScrollBar;

public class KTable extends Canvas {

	/*******************************************************************************
	 * Copyright (C) 2004 by Friederich Kupzog Elektronik & Software All rights
	 * reserved. This program and the accompanying materials are made available
	 * under the terms of the Eclipse Public License v1.0 which accompanies this
	 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
	 * 
	 * Contributors: Friederich Kupzog - initial API and implementation
	 * fkmk@kupzog.de www.kupzog.de/fkmk
	 ******************************************************************************/

	/**
	 * Custom drawn tabel widget for SWT GUIs.
	 * 
	 * 
	 * @see de.kupzog.ktable.KTableModel
	 * @see de.kupzog.ktable.KTableCellRenderer
	 * @see de.kupzog.ktable.KTableCellEditor
	 * @see de.kupzog.ktable.KTableCellSelectionListener
	 * 
	 * The idea of KTable is to have a flexible grid of cells to display data in it.
	 * The class focuses on displaying data and not on collecting the data to
	 * display. The latter is done by the KTableModel which has to be implemented
	 * for each specific case. The table asks the table model for the amount of
	 * columns and rows, the sizes of columns and rows and for the content of the
	 * cells which are currently drawn. Even if the table has a million rows, it
	 * won't get slower because it only requests those cells it currently draws.
	 * Only a bad table model can influence the drawing speed negatively.
	 * 
	 * When drawing a cell, the table calls a KTableCellRenderer to do this work.
	 * The table model determines which cell renderer is used for which cell. A
	 * default renderer is available (KTableCellRenderer.defaultRenderer), but the
	 * creation of self-written renderers for specific purposes is assumed.
	 * 
	 * KTable allows to resize columns and rows. Each column can have an individual
	 * size while the rows are all of the same height except the first row. Multiple
	 * column and row headers are possible. These "fixed" cells will not be scrolled
	 * out of sight. The column and row count always starts in the upper left corner
	 * with 0, independent of the number of column headers or row headers.
	 * 
	 * @author Friederich Kupzog
	 * 
	 */

	  // Daten und Datendarstellung
	  protected KTableModel m_Model;

	  protected KTableCellEditor m_CellEditor;

	  // aktuelle Ansicht
	  protected int m_TopRow;

	  protected int m_LeftColumn;

	  // Selection
	  protected boolean m_RowSelectionMode;

	  protected boolean m_MultiSelectMode;

	  protected HashMap<Object, Object> m_Selection;

	  protected int m_FocusRow;

	  protected int m_FocusCol;

	  protected int m_ClickColumnIndex;

	  protected int m_ClickRowIndex;

	  // wichtige MaBe
	  protected int m_RowsVisible;

	  protected int m_RowsFullyVisible;

	  protected int m_ColumnsVisible;

	  protected int m_ColumnsFullyVisible;

	  // SpaltengroBe
	  protected int m_ResizeColumnIndex;

	  protected int m_ResizeColumnLeft;

	  protected int m_ResizeRowIndex;

	  protected int m_ResizeRowTop;

	  protected int m_NewRowSize;

	  protected boolean m_Capture;

	  protected Image m_LineRestore;

	  protected int m_LineX;

	  protected int m_LineY;

	  // sonstige
	  protected GC m_GC;

	  protected Display m_Display;

	  protected ArrayList<KTableCellSelectionListener> cellSelectionListeners;

	  protected ArrayList<KTableCellResizeListener> cellResizeListeners;

	  protected boolean flatStyleSpecified;

	  // ////////////////////////////////////////////////////////////////////////////
	  // KONSTRUKTOR
	  // ////////////////////////////////////////////////////////////////////////////

	  /**
	   * Creates a new KTable.
	   * 
	   * possible styles: SWT.V_SCROLL - show vertical scrollbar and allow
	   * vertical scrolling by arrow keys SWT.H_SCROLL - show horizontal scrollbar
	   * and allow horizontal scrolling by arrow keys SWT.FLAT - no border
	   * drawing.
	   * 
	   * After creation a table model should be added using setModel().
	   */
	  public KTable(Composite parent, int style) {
	    // Oberklasse initialisieren
	    super(parent, SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE | style);

	    // inits
	    m_GC = new GC(this);
	    m_Display = Display.getCurrent();
	    m_Selection = new HashMap<Object, Object>();
	    m_CellEditor = null;

	    flatStyleSpecified = ((style | SWT.FLAT) == style);

	    m_RowSelectionMode = false;
	    m_MultiSelectMode = false;
	    m_TopRow = 0;
	    m_LeftColumn = 0;
	    m_FocusRow = 0;
	    m_FocusCol = 0;
	    m_RowsVisible = 0;
	    m_RowsFullyVisible = 0;
	    m_ColumnsVisible = 0;
	    m_ColumnsFullyVisible = 0;
	    m_ResizeColumnIndex = -1;
	    m_ResizeRowIndex = -1;
	    m_ResizeRowTop = -1;
	    m_NewRowSize = -1;
	    m_ResizeColumnLeft = -1;
	    m_Capture = false;
	    m_ClickColumnIndex = -1;
	    m_ClickRowIndex = -1;

	    m_LineRestore = null;
	    m_LineX = 0;
	    m_LineY = 0;

	    cellSelectionListeners = new ArrayList<KTableCellSelectionListener>(10);
	    cellResizeListeners = new ArrayList<KTableCellResizeListener>(10);

	    // Listener
	    createListeners();

	  }

	  protected void createListeners() {

	    addPaintListener(new PaintListener() {
	      public void paintControl(PaintEvent event) {
	        onPaint(event);
	      }
	    });

	    addControlListener(new ControlAdapter() {
	      public void controlResized(ControlEvent e) {
	        redraw();
	      }
	    });

	    addMouseListener(new MouseAdapter() {
	      public void mouseDown(MouseEvent e) {
	        onMouseDown(e);
	      }

	      public void mouseUp(MouseEvent e) {
	        onMouseUp(e);
	      }

	      public void mouseDoubleClick(MouseEvent e) {
	        onMouseDoubleClick(e);
	      }
	    });

	    addMouseMoveListener(new MouseMoveListener() {
	      public void mouseMove(MouseEvent e) {
	        onMouseMove(e);
	      }
	    });

	    if (getVerticalBar() != null) {
	      getVerticalBar().addSelectionListener(new SelectionAdapter() {
	        public void widgetSelected(SelectionEvent e) {
	          m_TopRow = getVerticalBar().getSelection();
	          redraw();
	        }

	      });
	    }

	    if (getHorizontalBar() != null) {
	      getHorizontalBar().addSelectionListener(new SelectionAdapter() {
	        public void widgetSelected(SelectionEvent e) {
	          m_LeftColumn = getHorizontalBar().getSelection();
	          redraw();
	        }
	      });
	    }
	    addKeyListener(new KeyAdapter() {
	      public void keyPressed(KeyEvent e) {
	        onKeyDown(e);
	      }
	    });
	  }

	  // ////////////////////////////////////////////////////////////////////////////
	  // Berechnungen
	  // ////////////////////////////////////////////////////////////////////////////

	  protected int getFixedWidth() {
	    int width = 0;
	    for (int i = 0; i < m_Model.getFixedColumnCount(); i++)
	      width += m_Model.getColumnWidth(i);
	    return width;
	  }

	  protected int getColumnLeft(int index) {
	    if (index < m_Model.getFixedColumnCount()) {
	      int x = 0;
	      for (int i = 0; i < index; i++) {
	        x += m_Model.getColumnWidth(i);
	      }
	      return x;
	    }
	    if (index < m_LeftColumn)
	      return -1;
	    int x = getFixedWidth();
	    for (int i = m_LeftColumn; i < index; i++) {
	      x += m_Model.getColumnWidth(i);
	    }
	    return x;
	  }

	  protected int getColumnRight(int index) {
	    if (index < 0)
	      return 0;
	    return getColumnLeft(index) + m_Model.getColumnWidth(index);
	  }

	  protected int getLastColumnRight() {
	    return getColumnRight(m_Model.getColumnCount() - 1);
	  }

	  protected void doCalculations() {
	    if (m_Model == null) {
	      ScrollBar sb = getHorizontalBar();
	      if (sb != null) {
	        sb.setMinimum(0);
	        sb.setMaximum(1);
	        sb.setPageIncrement(1);
	        sb.setThumb(1);
	        sb.setSelection(1);
	      }
	      sb = getVerticalBar();
	      if (sb != null) {
	        sb.setMinimum(0);
	        sb.setMaximum(1);
	        sb.setPageIncrement(1);
	        sb.setThumb(1);
	        sb.setSelection(1);
	      }
	      return;
	    }

	    int m_HeaderHeight = m_Model.getFirstRowHeight();
	    int m_RowHeight = m_Model.getRowHeight();

	    Rectangle rect = getClientArea();
	    if (m_LeftColumn < m_Model.getFixedColumnCount()) {
	      m_LeftColumn = m_Model.getFixedColumnCount();
	    }

	    if (m_TopRow < m_Model.getFixedRowCount()) {
	      m_TopRow = m_Model.getFixedRowCount();
	    }

	    //int fixedWidth = getFixedWidth();
	    int fixedHeight = m_HeaderHeight + (m_Model.getFixedRowCount() - 1)
	        * m_Model.getRowHeight();
	    m_ColumnsVisible = 0;
	    m_ColumnsFullyVisible = 0;

	    if (m_Model.getColumnCount() > m_Model.getFixedColumnCount()) {
	      int runningWidth = getColumnLeft(m_LeftColumn);
	      for (int col = m_LeftColumn; col < m_Model.getColumnCount(); col++) {
	        if (runningWidth < rect.width + rect.x)
	          m_ColumnsVisible++;
	        runningWidth += m_Model.getColumnWidth(col);
	        if (runningWidth < rect.width + rect.x)
	          m_ColumnsFullyVisible++;
	        else
	          break;
	      }
	    }

	    ScrollBar sb = getHorizontalBar();
	    if (sb != null) {
	      if (m_Model.getColumnCount() <= m_Model.getFixedColumnCount()) {
	        sb.setMinimum(0);
	        sb.setMaximum(1);
	        sb.setPageIncrement(1);
	        sb.setThumb(1);
	        sb.setSelection(1);
	      } else {
	        sb.setMinimum(m_Model.getFixedColumnCount());
	        sb.setMaximum(m_Model.getColumnCount());
	        sb.setIncrement(1);
	        sb.setPageIncrement(2);
	        sb.setThumb(m_ColumnsFullyVisible);
	        sb.setSelection(m_LeftColumn);
	      }
	    }

	    m_RowsFullyVisible = Math.max(0, (rect.height - fixedHeight)
	        / m_RowHeight);
	    m_RowsFullyVisible = Math.min(m_RowsFullyVisible, m_Model.getRowCount()
	        - m_Model.getFixedRowCount());
	    m_RowsFullyVisible = Math.max(0, m_RowsFullyVisible);

	    m_RowsVisible = m_RowsFullyVisible + 1;

	    if (m_TopRow + m_RowsFullyVisible > m_Model.getRowCount()) {
	      m_TopRow = Math.max(m_Model.getFixedRowCount(), m_Model
	          .getRowCount()
	          - m_RowsFullyVisible);
	    }

	    if (m_TopRow + m_RowsFullyVisible >= m_Model.getRowCount()) {
	      m_RowsVisible--;
	    }

	    sb = getVerticalBar();
	    if (sb != null) {
	      if (m_Model.getRowCount() <= m_Model.getFixedRowCount()) {
	        sb.setMinimum(0);
	        sb.setMaximum(1);
	        sb.setPageIncrement(1);
	        sb.setThumb(1);
	        sb.setSelection(1);
	      } else {
	        sb.setMinimum(m_Model.getFixedRowCount());
	        sb.setMaximum(m_Model.getRowCount());
	        sb.setPageIncrement(m_RowsVisible);
	        sb.setIncrement(1);
	        sb.setThumb(m_RowsFullyVisible);
	        sb.setSelection(m_TopRow);
	      }
	    }
	  }

	  /**
	   * Returns the area that is occupied by the given cell
	   * 
	   * @param col
	   * @param row
	   * @return Rectangle
	   */
	  public Rectangle getCellRect(int col, int row) {
	    int m_HeaderHeight = m_Model.getFirstRowHeight();
	    if ((col < 0) || (col >= m_Model.getColumnCount()))
	      return new Rectangle(-1, -1, 0, 0);

	    int x = getColumnLeft(col) + 1;
	    int y;

	    if (row == 0)
	      y = 0;
	    else if (row < m_Model.getFixedRowCount())
	      y = m_HeaderHeight + ((row - 1) * m_Model.getRowHeight());
	    else
	      y = m_HeaderHeight
	          + (m_Model.getFixedRowCount() - 1 + row - m_TopRow)
	          * m_Model.getRowHeight();
	    int width = m_Model.getColumnWidth(col) - 1;
	    int height = m_Model.getRowHeight() - 1;
	    if (row == 0)
	      height = m_Model.getFirstRowHeight() - 1;

	    return new Rectangle(x, y, width, height);
	  }

	  protected boolean canDrawCell(int col, int row, Rectangle clipRect) {
	    Rectangle r = getCellRect(col, row);
	    return canDrawCell(r, clipRect);
	  }

	  protected boolean canDrawCell(Rectangle r, Rectangle clipRect) {
	    if (r.y + r.height < clipRect.y)
	      return false;
	    if (r.y > clipRect.y + clipRect.height)
	      return false;
	    if (r.x + r.width < clipRect.x)
	      return false;
	    if (r.x > clipRect.x + clipRect.width)
	      return false;
	    return true;
	  }

	  // ////////////////////////////////////////////////////////////////////////////
	  // ZEICHNEN
	  // ////////////////////////////////////////////////////////////////////////////

	  // Paint-Ereignis

	  protected void onPaint(PaintEvent event) {
	    Rectangle rect = getClientArea();
	    GC gc = event.gc;

	    doCalculations();

	    if (m_Model != null) {

	      drawBottomSpace(gc);
	      drawCells(gc, gc.getClipping(), 0, m_Model.getFixedColumnCount(),
	          0, m_Model.getFixedRowCount());
	      drawCells(gc, gc.getClipping(), m_LeftColumn, m_Model
	          .getColumnCount(), 0, m_Model.getFixedRowCount());
	      drawCells(gc, gc.getClipping(), 0, m_Model.getFixedColumnCount(),
	          m_TopRow, m_TopRow + m_RowsVisible);
	      drawCells(gc, gc.getClipping(), m_LeftColumn, m_Model
	          .getColumnCount(), m_TopRow, m_TopRow + m_RowsVisible);
	    } else {
	      gc.fillRectangle(rect);
	    }
	  }

	  // Bottom-Space

	  protected void drawBottomSpace(GC gc) {
	    Rectangle r = getClientArea();
	    if (m_Model.getRowCount() > 0) {
	      r.y = m_Model.getFirstRowHeight()
	          + (m_Model.getFixedRowCount() - 1 + m_RowsVisible)
	          * m_Model.getRowHeight() + 1;
	    }

	    gc.setBackground(getBackground());
	    gc.fillRectangle(r);
	    gc.fillRectangle(getLastColumnRight() + 2, 0, r.width, r.height);

	    if (m_Model.getRowCount() > 0) {
	      if (flatStyleSpecified)
	        // gc.setForeground(this.getBackground());
	        gc.setForeground(m_Display
	            .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
	      else
	        gc.setForeground(m_Display
	            .getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
	      // Linke Schattenlinie
	      gc.drawLine(0, 0, 0, r.y - 1);
	    }

	    if (!flatStyleSpecified)
	      gc.setForeground(this.getBackground());
	    else
	      gc.setForeground(m_Display.getSystemColor(SWT.COLOR_WHITE));
	    // Untere Abschlusslinie
	    gc.drawLine(0, r.y - 1, getLastColumnRight() + 1, r.y - 1);

	    // Rechte Abschlusslinie
	    gc.drawLine(getLastColumnRight() + 1, 0, getLastColumnRight() + 1,
	        r.y - 1);
	  }

	  // Cells

	  /**
	   * Redraws the the cells only in the given area.
	   * 
	   * @param cellsToRedraw
	   *            Defines the area to redraw. The rectangles elements are not
	   *            pixels but cell numbers.
	   */
	  public void redraw(Rectangle cellsToRedraw) {
	    redraw(cellsToRedraw.x, cellsToRedraw.y, cellsToRedraw.width,
	        cellsToRedraw.height);
	  }

	  /**
	   * Redraws the the cells only in the given area.
	   * 
	   * @param firstCol
	   * @param firstRow
	   * @param numOfCols
	   * @param numOfRows
	   */
	  public void redraw(int firstCol, int firstRow, int numOfCols, int numOfRows) {
	    Rectangle clipRect = getClientArea();
	    drawCells(new GC(this), clipRect, firstCol, firstCol + numOfCols,
	        firstRow, firstRow + numOfRows);
	  }

	  protected void drawCells(GC gc, Rectangle clipRect, int fromCol, int toCol,
	      int fromRow, int toRow) {
//	    int cnt = 0;
	    Rectangle r;

	    if (m_CellEditor != null) {
	      if (!isCellVisible(m_CellEditor.m_Col, m_CellEditor.m_Row)) {
	        Rectangle hide = new Rectangle(-101, -101, 100, 100);
	        m_CellEditor.setBounds(hide);
	      } else {
	        m_CellEditor.setBounds(getCellRect(m_CellEditor.m_Col,
	            m_CellEditor.m_Row));
	      }
	    }

	    for (int row = fromRow; row < toRow; row++) {
	      r = getCellRect(0, row);
	      if (r.y + r.height < clipRect.y) {
	        continue;
	      }
	      if (r.y > clipRect.y + clipRect.height) {
	        break;
	      }

	      for (int col = fromCol; col < toCol; col++) {
	        r = getCellRect(col, row);
	        if (r.x > clipRect.x + clipRect.width) {
	          break;
	        }
	        if (canDrawCell(col, row, clipRect)) {
	          drawCell(gc, col, row);
//	          cnt++;
	        }
	      }
	    }
	  }

	  protected void drawCell(GC gc, int col, int row) {
	    if ((row < 0) || (row >= m_Model.getRowCount())) {
	      return;
	    }

	    Rectangle rect = getCellRect(col, row);

	    m_Model.getCellRenderer(col, row).drawCell(
	        gc,
	        rect,
	        col,
	        row,
	        m_Model.getContentAt(col, row),
	        showAsSelected(col, row),
	        col < m_Model.getFixedColumnCount()
	            || row < m_Model.getFixedRowCount(),
	        col == m_ClickColumnIndex && row == m_ClickRowIndex);

	  }

	  protected boolean showAsSelected(int col, int row) {
	    // A cell with an open editor should be drawn without focus
	    if (m_CellEditor != null) {
	      if (col == m_CellEditor.m_Col && row == m_CellEditor.m_Row)
	        return false;
	    }
	    return isCellSelected(col, row);
	  }

	  protected void drawRow(GC gc, int row) {
	    //Rectangle clipRect = getClientArea();
	    drawCells(gc, getClientArea(), 0, m_Model.getFixedColumnCount(), row,
	        row + 1);
	    drawCells(gc, getClientArea(), m_LeftColumn, m_Model.getColumnCount(),
	        row, row + 1);
	  }

	  protected void drawCol(GC gc, int col) {
	    Rectangle clipRect = getClientArea();
	    drawCells(gc, clipRect, col, col + 1, 0, m_Model.getFixedRowCount());
	    drawCells(gc, clipRect, col, col + 1, m_TopRow, m_TopRow
	        + m_RowsVisible);
	  }

	  // ////////////////////////////////////////////////////////////////////////////
	  // REAKTION AUF BENUTZER
	  // ////////////////////////////////////////////////////////////////////////////

	  /* gibt die Nummer einer Modellspalte zuruck */
	  protected int getColumnForResize(int x, int y) {
	    if (m_Model == null)
	      return -1;
	    if ((y <= 0)
	        || (y >= m_Model.getFirstRowHeight()
	            + (m_Model.getFixedRowCount() - 1)
	            * m_Model.getRowHeight()))
	      return -1;

	    if (x < getFixedWidth() + 3) {
	      for (int i = 0; i < m_Model.getFixedColumnCount(); i++)
	        if (Math.abs(x - getColumnRight(i)) < 3) {
	          if (m_Model.isColumnResizable(i))
	            return i;
	          return -1;
	        }
	    }

	    for (int i = m_LeftColumn; i < m_Model.getColumnCount(); i++) {
	      int left = getColumnLeft(i);
	      int right = left + m_Model.getColumnWidth(i);
	      if (Math.abs(x - right) < 3) {
	        if (m_Model.isColumnResizable(i))
	          return i;
	        return -1;
	      }
	      if ((x >= left + 3) && (x <= right - 3))
	        break;
	    }
	    return -1;
	  }

	  /* gibt die Nummer einer Zeile der Ansicht(!) zuruck */
	  protected int getRowForResize(int x, int y) {
	    if (m_Model == null)
	      return -1;
	    if ((x <= 0) || (x >= getFixedWidth()))
	      return -1;

	    if (y < m_Model.getFirstRowHeight())
	      return -1;

	    int row = 1 + ((y - m_Model.getFirstRowHeight()) / m_Model
	        .getRowHeight());
	    int rowY = m_Model.getFirstRowHeight() + row * m_Model.getRowHeight();

	    if (Math.abs(rowY - y) < 3 && m_Model.isRowResizable())
	      return row;

	    return -1;
	  }

	  /**
	   * Returns the number of the row that is present at position y or -1, if out
	   * of area.
	   * 
	   * @param y
	   * @return int
	   */
	  public int calcRowNum(int y) {
	    if (m_Model == null)
	      return -1;
	    if (y < m_Model.getFirstRowHeight())
	      return (m_Model.getFixedRowCount() == 0 ? m_TopRow : 0);
	    y -= m_Model.getFirstRowHeight();
	    int row = 1 + (y / m_Model.getRowHeight());
	    if ((row < 0) || (row >= m_Model.getRowCount()))
	      return -1;
	    if (row >= m_Model.getFixedRowCount())
	      return m_TopRow + row - m_Model.getFixedRowCount();
	    return row;
	  }

	  /**
	   * Returns the number of the column that is present at position x or -1, if
	   * out of area.
	   * 
	   * @param y
	   * @return int
	   */
	  public int calcColumnNum(int x) {
	    if (m_Model == null)
	      return -1;
	    int col = 0;

	    int z = 0;
	    for (int i = 0; i < m_Model.getFixedColumnCount(); i++) {
	      if ((x >= z) && (x <= z + m_Model.getColumnWidth(i))) {
	        return i;
	      }
	      z += m_Model.getColumnWidth(i);
	    }

	    col = -1;
	    z = getFixedWidth();
	    for (int i = m_LeftColumn; i < m_Model.getColumnCount(); i++) {
	      if ((x >= z) && (x <= z + m_Model.getColumnWidth(i))) {
	        col = i;
	        break;
	      }
	      z += m_Model.getColumnWidth(i);
	    }
	    return col;
	  }

	  public boolean isCellVisible(int col, int row) {
	    if (m_Model == null)
	      return false;
	    return ((col >= m_LeftColumn && col < m_LeftColumn + m_ColumnsVisible
	        && row >= m_TopRow && row < m_TopRow + m_RowsVisible)

	    || (col < m_Model.getFixedColumnCount() && row < m_Model
	        .getFixedRowCount()));
	  }

	  public boolean isCellFullyVisible(int col, int row) {
	    if (m_Model == null)
	      return false;
	    return ((col >= m_LeftColumn
	        && col < m_LeftColumn + m_ColumnsFullyVisible
	        && row >= m_TopRow && row < m_TopRow + m_RowsFullyVisible)

	    || (col < m_Model.getFixedColumnCount() && row < m_Model
	        .getFixedRowCount()));
	  }

	  public boolean isRowVisible(int row) {
	    if (m_Model == null)
	      return false;
	    return ((row >= m_TopRow && row < m_TopRow + m_RowsVisible) || row < m_Model
	        .getFixedRowCount());

	  }

	  public boolean isRowFullyVisible(int row) {
	    if (m_Model == null)
	      return false;
	    return ((row >= m_TopRow && row < m_TopRow + m_RowsFullyVisible) || row < m_Model
	        .getFixedRowCount());
	  }

	  /*
	   * Focusses the given Cell. Assumes that the given cell is in the viewable
	   * area. Does all neccessary redraws.
	   */
	  protected void focusCell(int col, int row, int stateMask) {
	    GC gc = new GC(this);

	    // close cell editor if active
	    if (m_CellEditor != null)
	      m_CellEditor.close(true);

	    /*
	     * Special rule: in row selection mode the selection if a fixed cell in
	     * a non-fixed row is allowed and handled as a selection of a non-fixed
	     * cell.
	     */

	    if (row >= m_Model.getFixedRowCount()
	        && (col >= m_Model.getFixedColumnCount() || m_RowSelectionMode)) {

	      if ((stateMask & SWT.CTRL) == 0 && (stateMask & SWT.SHIFT) == 0) {
	        // case: no modifier key
	        boolean redrawAll = (m_Selection.size() > 1);
	        int oldFocusRow = m_FocusRow;
	        int oldFocusCol = m_FocusCol;
	        clearSelectionWithoutRedraw();
	        addToSelection(col, row);
	        m_FocusRow = row;
	        m_FocusCol = col;

	        if (redrawAll)
	          redraw();
	        else if (m_RowSelectionMode) {
	          if (isRowVisible(oldFocusRow))
	            drawRow(gc, oldFocusRow);
	          if (isRowVisible(m_FocusRow))
	            drawRow(gc, m_FocusRow);
	        } else {
	          if (isCellVisible(oldFocusCol, oldFocusRow))
	            drawCell(gc, oldFocusCol, oldFocusRow);
	          if (isCellVisible(m_FocusCol, m_FocusRow))
	            drawCell(gc, m_FocusCol, m_FocusRow);
	        }
	      }

	      else if ((stateMask & SWT.CTRL) != 0) {
	        // case: CTRL key pressed
	        if (toggleSelection(col, row)) {
	          m_FocusCol = col;
	          m_FocusRow = row;
	        }

	        if (m_RowSelectionMode) {
	          drawRow(gc, row);
	        } else {
	          drawCell(gc, col, row);
	        }
	      }

	      else if ((stateMask & SWT.SHIFT) != 0) {
	        // case: SHIFT key pressed

	        if (m_RowSelectionMode) {
	          if (row < m_FocusRow) {
	            // backword selection
	            while (row != m_FocusRow) {
	              addToSelection(0, --m_FocusRow);
	            }
	          } else {
	            // foreward selection
	            while (row != m_FocusRow) {
	              addToSelection(0, ++m_FocusRow);
	            }
	          }
	        } else // cell selection mode
	        {
	          if (row < m_FocusRow
	              || (row == m_FocusRow && col < m_FocusCol)) {
	            // backword selection
	            while (row != m_FocusRow || col != m_FocusCol) {
	              m_FocusCol--;
	              if (m_FocusCol < m_Model.getFixedColumnCount()) {
	                m_FocusCol = m_Model.getColumnCount();
	                m_FocusRow--;
	              }
	              addToSelection(m_FocusCol, m_FocusRow);
	            }
	          } else {
	            // foreward selection
	            while (row != m_FocusRow || col != m_FocusCol) {
	              m_FocusCol++;
	              if (m_FocusCol == m_Model.getColumnCount()) {
	                m_FocusCol = m_Model.getFixedColumnCount();
	                m_FocusRow++;
	              }
	              addToSelection(m_FocusCol, m_FocusRow);
	            }
	          }

	        }

	        redraw();
	      }

	      // notify non-fixed cell listeners
	      fireCellSelection(col, row, stateMask);
	    } else {
	      // a fixed cell was focused
	      drawCell(gc, col, row);
	      // notify fixed cell listeners
	      fireFixedCellSelection(col, row, stateMask);
	    }

	    gc.dispose();
	  }

	  protected void onMouseDown(MouseEvent e) {
	    if (e.button == 1) {
	      // deactivateEditor(true);
	      setCapture(true);
	      m_Capture = true;

	      // Resize column?
	      int columnIndex = getColumnForResize(e.x, e.y);
	      if (columnIndex >= 0) {
	        m_ResizeColumnIndex = columnIndex;
	        m_ResizeColumnLeft = getColumnLeft(columnIndex);
	        return;
	      }

	      // Resize row?
	      int rowIndex = getRowForResize(e.x, e.y);
	      if (rowIndex >= 0) {
	        m_ResizeRowIndex = rowIndex;
	        m_ResizeRowTop = m_Model.getFirstRowHeight() + (rowIndex - 1)
	            * m_Model.getRowHeight();
	        m_NewRowSize = m_Model.getRowHeight();
	        return;
	      }
	    }

	    // focus change
	    int col = calcColumnNum(e.x);
	    int row = calcRowNum(e.y);

	    if (col == -1 || row == -1)
	      return;

	    m_ClickColumnIndex = col;
	    m_ClickRowIndex = row;

	    focusCell(col, row, e.stateMask);

	  }

	  protected void onMouseMove(MouseEvent e) {
	    if (m_Model == null)
	      return;

	    // show resize cursor?
	    if ((m_ResizeColumnIndex != -1) || (getColumnForResize(e.x, e.y) >= 0))
	      setCursor(new Cursor(m_Display, SWT.CURSOR_SIZEWE));
	    else if ((m_ResizeRowIndex != -1) || (getRowForResize(e.x, e.y) >= 0))
	      setCursor(new Cursor(m_Display, SWT.CURSOR_SIZENS));
	    else
	      setCursor(null);

	    if (e.button == 1) {
	      // extend selection?
	      if (m_ClickColumnIndex != -1 && m_MultiSelectMode) {
	        int row = calcRowNum(e.y);
	        int col = calcColumnNum(e.x);

	        if (row >= m_Model.getFixedRowCount()
	            && col >= m_Model.getFixedColumnCount()) {

	          m_ClickColumnIndex = col;
	          m_ClickRowIndex = row;

	          focusCell(col, row, (e.stateMask | SWT.SHIFT));
	        }
	      }

	    }
	    // column resize?
	    if (m_ResizeColumnIndex != -1) {
	      Rectangle rect = getClientArea();
	      int oldSize = m_Model.getColumnWidth(m_ResizeColumnIndex);
	      if (e.x > rect.x + rect.width - 1)
	        e.x = rect.x + rect.width - 1;
	      int newSize = e.x - m_ResizeColumnLeft;
	      if (newSize < 5)
	        newSize = 5;

	      int leftX = getColumnLeft(m_ResizeColumnIndex);
	      int rightX = getColumnRight(m_ResizeColumnIndex);

	      m_Model.setColumnWidth(m_ResizeColumnIndex, newSize);
	      newSize = m_Model.getColumnWidth(m_ResizeColumnIndex);

	      GC gc = new GC(this);
	      gc.copyArea(rightX, 0, rect.width - rightX, rect.height, leftX
	          + newSize, 0);
	      drawCol(gc, m_ResizeColumnIndex);
	      if (newSize < oldSize) {
	        int delta = oldSize - newSize;
	        redraw(rect.width - delta, 0, delta, rect.height, false);
	      }
	      gc.dispose();
	    }

	    // row resize?
	    if (m_ResizeRowIndex != -1) {
	      Rectangle rect = getClientArea();
	      GC gc = new GC(this);

	      // calculate new size
	      if (e.y > rect.y + rect.height - 1)
	        e.y = rect.y + rect.height - 1;
	      m_NewRowSize = e.y - m_ResizeRowTop;
	      if (m_NewRowSize < m_Model.getRowHeightMinimum())
	        m_NewRowSize = m_Model.getRowHeightMinimum();

	      // restore old line area
	      if (m_LineRestore != null) {
	        gc.drawImage(m_LineRestore, m_LineX, m_LineY);
	      }

	      // safe old picture and draw line
	      gc.setForeground(Display.getCurrent().getSystemColor(
	          SWT.COLOR_BLACK));
	      int lineEnd = getColumnRight(m_LeftColumn + m_ColumnsVisible - 1);
	      m_LineRestore = new Image(m_Display, lineEnd, 1);
	      m_LineX = rect.x + 1;
	      m_LineY = m_ResizeRowTop + m_NewRowSize - 1;
	      gc.copyArea(m_LineRestore, m_LineX, m_LineY);
	      gc.drawLine(m_LineX, m_LineY, rect.x + lineEnd, m_LineY);
	      gc.dispose();

	    }

	  }

	  protected void onMouseUp(MouseEvent e) {
	    // if (e.button == 1)
	    {
	      if (m_Model == null)
	        return;

	      setCapture(false);
	      m_Capture = false;

	      if (m_ResizeColumnIndex != -1) {
	        fireColumnResize(m_ResizeColumnIndex, m_Model
	            .getColumnWidth(m_ResizeColumnIndex));
	        m_ResizeColumnIndex = -1;
	        redraw();

	      }
	      if (m_ResizeRowIndex != -1) {
	        m_ResizeRowIndex = -1;
	        m_Model.setRowHeight(m_NewRowSize);
	        m_LineRestore = null;
	        fireRowResize(m_NewRowSize);
	        redraw();
	      }
	      if (m_ClickColumnIndex != -1) {
	        int col = m_ClickColumnIndex;
	        int row = m_ClickRowIndex;
	        m_ClickColumnIndex = -1;
	        m_ClickRowIndex = -1;
	        if (m_CellEditor == null) {
	          drawCell(new GC(this), col, row);
	        }
	      }

	    }
	  }

	  protected void onKeyDown(KeyEvent e) {
	    boolean focusChanged = false;
	    int newFocusRow = m_FocusRow;
	    int newFocusCol = m_FocusCol;

	    if (m_Model == null)
	      return;

	    if ((e.character == ' ') || (e.character == '\r')) {
	      openEditorInFocus();
	      return;
	    } else if (e.keyCode == SWT.HOME) {
	      newFocusCol = m_Model.getFixedColumnCount();
	      if (newFocusRow == -1)
	        newFocusRow = m_Model.getFixedRowCount();
	      focusChanged = true;
	    } else if (e.keyCode == SWT.END) {
	      newFocusCol = m_Model.getColumnCount() - 1;
	      if (newFocusRow == -1)
	        newFocusRow = m_Model.getFixedRowCount();
	      focusChanged = true;
	    } else if (e.keyCode == SWT.ARROW_LEFT) {
	      if (!m_RowSelectionMode) {
	        if (newFocusCol > m_Model.getFixedColumnCount())
	          newFocusCol--;
	      }
	      focusChanged = true;
	    } else if (e.keyCode == SWT.ARROW_RIGHT) {
	      if (!m_RowSelectionMode) {
	        if (newFocusCol == -1) {
	          newFocusCol = m_Model.getFixedColumnCount();
	          newFocusRow = m_Model.getFixedRowCount();
	        } else if (newFocusCol < m_Model.getColumnCount() - 1)
	          newFocusCol++;
	      }
	      focusChanged = true;
	    } else if (e.keyCode == SWT.ARROW_DOWN) {
	      if (newFocusRow == -1) {
	        newFocusRow = m_Model.getFixedRowCount();
	        newFocusCol = m_Model.getFixedColumnCount();
	      } else if (newFocusRow < m_Model.getRowCount() - 1)
	        newFocusRow++;
	      focusChanged = true;
	    } else if (e.keyCode == SWT.ARROW_UP) {
	      if (newFocusRow > m_Model.getFixedRowCount())
	        newFocusRow--;
	      focusChanged = true;
	    } else if (e.keyCode == SWT.PAGE_DOWN) {
	      newFocusRow += m_RowsVisible - 1;
	      if (newFocusRow >= m_Model.getRowCount())
	        newFocusRow = m_Model.getRowCount() - 1;
	      if (newFocusCol == -1)
	        newFocusCol = m_Model.getFixedColumnCount();
	      focusChanged = true;
	    } else if (e.keyCode == SWT.PAGE_UP) {
	      newFocusRow -= m_RowsVisible - 1;
	      if (newFocusRow < m_Model.getFixedRowCount())
	        newFocusRow = m_Model.getFixedRowCount();
	      if (newFocusCol == -1)
	        newFocusCol = m_Model.getFixedColumnCount();
	      focusChanged = true;
	    }

	    if (focusChanged) {

	      focusCell(newFocusCol, newFocusRow, e.stateMask);

	      if (!isCellFullyVisible(m_FocusCol, m_FocusRow))
	        scrollToFocus();
	    }
	  }

	  protected void onMouseDoubleClick(MouseEvent e) {
	    if (m_Model == null)
	      return;
	    if (e.button == 1) {

	      if (e.y < m_Model.getFirstRowHeight()
	          + ((m_Model.getFixedRowCount() - 1) * m_Model
	              .getRowHeight())) {
	        // double click in header area
	        int columnIndex = getColumnForResize(e.x, e.y);
	        resizeColumnOptimal(columnIndex);
	        return;
	      } else
	        openEditorInFocus();
	    }
	  }

	  /**
	   * Resizes the given column to its optimal width.
	   * 
	   * Is also called if user doubleclicks in the resize area of a resizable
	   * column.
	   * 
	   * The optimal width is determined by asking the CellRenderers for the
	   * visible cells of the column for the optimal with and taking the minimum
	   * of the results. Note that the optimal width is only determined for the
	   * visible area of the table because otherwise this could take very long
	   * time.
	   * 
	   * @param column
	   *            The column to resize
	   * @return int The optimal with that was determined or -1, if column out of
	   *         range.
	   */
	  public int resizeColumnOptimal(int column) {
	    if (column >= 0 && column < m_Model.getColumnCount()) {
	      int optWidth = 5;
	      for (int i = 0; i < m_Model.getFixedRowCount(); i++) {
	        int width = m_Model.getCellRenderer(column, i).getOptimalWidth(
	            m_GC, column, i, m_Model.getContentAt(column, i), true);
	        if (width > optWidth)
	          optWidth = width;
	      }
	      for (int i = m_TopRow; i < m_TopRow + m_RowsVisible; i++) {
	        int width = m_Model.getCellRenderer(column, i).getOptimalWidth(
	            m_GC, column, i, m_Model.getContentAt(column, i), true);
	        if (width > optWidth)
	          optWidth = width;
	      }
	      m_Model.setColumnWidth(column, optWidth);
	      redraw();
	      return optWidth;
	    }
	    return -1;
	  }

	  /**
	   * This method activated the cell editor on the current focus cell, if the
	   * table model allows cell editing for this cell.
	   */
	  public void openEditorInFocus() {
	    m_CellEditor = m_Model.getCellEditor(m_FocusCol, m_FocusRow);
	    if (m_CellEditor != null) {
	      Rectangle r = getCellRect(m_FocusCol, m_FocusRow);
	      m_CellEditor.open(this, m_FocusCol, m_FocusRow, r);
	    }
	  }

	  /*
	   * Tries to open KTableCellEditor on the given cell. If the cell exists, the
	   * model is asked for an editor and if there is one, the table scrolls the
	   * cell into the view and openes the editor on the cell. @param col @param
	   * row
	   * 
	   * public void tryToOpenEditorAt(int col, int row) { if (col >= 0 && col <
	   * m_Model.getColumnCount() && row >= 0 && row < m_Model.getRowCount()) {
	   * m_CellEditor = m_Model.getCellEditor(col, row); if (m_CellEditor != null) {
	   * m_FocusCol = col; m_FocusRow = row; scrollToFocus(); Rectangle r =
	   * getCellRect(col, row); m_CellEditor.open(this, m_FocusCol, m_FocusRow,
	   * r); } } }
	   */

	  protected void scrollToFocus() {
	    boolean change = false;

	    // vertical scroll allowed?
	    if (getVerticalBar() != null) {
	      if (m_FocusRow < m_TopRow) {
	        m_TopRow = m_FocusRow;
	        change = true;
	      }

	      if (m_FocusRow >= m_TopRow + m_RowsFullyVisible) {
	        m_TopRow = m_FocusRow - m_RowsFullyVisible + 1;
	        change = true;
	      }

	    }

	    // horizontal scroll allowed?
	    if (getHorizontalBar() != null) {
	      if (m_FocusCol < m_LeftColumn) {
	        m_LeftColumn = m_FocusCol;
	        change = true;
	      }

	      if (m_FocusCol >= m_LeftColumn + m_ColumnsFullyVisible) {
	        int oldLeftCol = m_LeftColumn;
	        Rectangle rect = getClientArea();
	        while (m_LeftColumn < m_FocusCol
	            && getColumnRight(m_FocusCol) > rect.width + rect.x) {
	          m_LeftColumn++;
	        }
	        change = (oldLeftCol != m_LeftColumn);
	      }
	    }

	    if (change)
	      redraw();
	  }

	  protected void fireCellSelection(int col, int row, int statemask) {
	    for (int i = 0; i < cellSelectionListeners.size(); i++) {
	      cellSelectionListeners.get(i)
	          .cellSelected(col, row, statemask);
	    }
	  }

	  protected void fireFixedCellSelection(int col, int row, int statemask) {
	    for (int i = 0; i < cellSelectionListeners.size(); i++) {
	      cellSelectionListeners.get(i)
	          .fixedCellSelected(col, row, statemask);
	    }
	  }

	  protected void fireColumnResize(int col, int newSize) {
	    for (int i = 0; i < cellResizeListeners.size(); i++) {
	      cellResizeListeners.get(i)
	          .columnResized(col, newSize);
	    }
	  }

	  protected void fireRowResize(int newSize) {
	    for (int i = 0; i < cellResizeListeners.size(); i++) {
	      cellResizeListeners.get(i)
	          .rowResized(newSize);
	    }
	  }

	  /**
	   * Adds a listener that is notified when a cell is selected.
	   * 
	   * This can happen either by a click on the cell or by arrow keys. Note that
	   * the listener is not called for each cell that the user selects in one
	   * action using Shift+Click. To get all these cells use the listener and
	   * getCellSelecion() or getRowSelection().
	   * 
	   * @param listener
	   */
	  public void addCellSelectionListener(KTableCellSelectionListener listener) {
	    cellSelectionListeners.add(listener);
	  }

	  /**
	   * Adds a listener that is notified when a cell is resized. This happens
	   * when the mouse button is released after a resizing.
	   * 
	   * @param listener
	   */
	  public void addCellResizeListener(KTableCellResizeListener listener) {
	    cellResizeListeners.add(listener);
	  }

	  /**
	   * Removes the listener if present. Returns true, if found and removed from
	   * the list of listeners.
	   */
	  public boolean removeCellSelectionListener(
	      KTableCellSelectionListener listener) {
	    return cellSelectionListeners.remove(listener);
	  }

	  /**
	   * Removes the listener if present. Returns true, if found and removed from
	   * the list of listeners.
	   */
	  public boolean removeCellResizeListener(KTableCellResizeListener listener) {
	    return cellResizeListeners.remove(listener);
	  }

	  // ////////////////////////////////////////////////////////////////////////////
	  // SELECTION
	  // ////////////////////////////////////////////////////////////////////////////

	  /**
	   * Sets the "Row Selection Mode". The current selection is cleared when this
	   * method is called.
	   * 
	   * @param rowSelectMode
	   *            In the "Row Selection Mode", the table always selects a
	   *            complete row. Otherwise, each individual cell can be selected.
	   * 
	   * This mode can be combined with the "Multi Selection Mode".
	   * 
	   */
	  public void setRowSelectionMode(boolean rowSelectMode) {
	    m_RowSelectionMode = rowSelectMode;
	    clearSelection();
	  }

	  /**
	   * Sets the "Multi Selection Mode". The current selection is cleared when
	   * this method is called.
	   * 
	   * @param multiSelectMode
	   *            In the "Multi Select Mode", more than one cell or row can be
	   *            selected. The user can achieve this by shift-click and
	   *            ctrl-click. The selected cells/rows can be scattored ofer the
	   *            complete table. If you pass false, only a single cell or row
	   *            can be selected.
	   * 
	   * This mode can be combined with the "Row Selection Mode".
	   */
	  public void setMultiSelectionMode(boolean multiSelectMode) {
	    m_MultiSelectMode = multiSelectMode;
	    clearSelection();
	  }

	  /**
	   * Returns true if in "Row Selection Mode".
	   * 
	   * @see setSelectionMode
	   * @return boolean
	   */
	  public boolean isRowSelectMode() {
	    return m_RowSelectionMode;
	  }

	  /**
	   * Returns true if in "Multi Selection Mode".
	   * 
	   * @see setSelectionMode
	   * @return boolean
	   */
	  public boolean isMultiSelectMode() {
	    return m_MultiSelectMode;
	  }

	  protected void clearSelectionWithoutRedraw() {
	    m_Selection.clear();
	  }

	  /**
	   * Clears the current selection (in all selection modes).
	   */
	  public void clearSelection() {
	    /*
	     * if (m_MultiSelectMode) { if (m_Selection.size() < m_RowsFullyVisible *
	     * m_ColumnsVisible) { if (m_RowSelectionMode) { for (Iterator iter =
	     * m_Selection.values().iterator(); iter.hasNext();) { int row =
	     * ((Integer) iter.next()).intValue(); if (row >= m_TopRow && row <
	     * m_TopRow+m_RowsFullyVisible) { } } } else { for (Iterator iter =
	     * m_Selection.values().iterator(); iter.hasNext();) { Point element =
	     * (Point) iter.next(); } } } }
	     */
	    clearSelectionWithoutRedraw();
	    m_FocusCol = -1;
	    m_FocusRow = -1;
	    if (m_MultiSelectMode)
	      redraw();
	  }

	  /*
	   * works in both modes: Cell and Row Selection. Has no redraw functionality!
	   * 
	   * Returns true, if added to selection.
	   */
	  protected boolean toggleSelection(int col, int row) {

	    if (m_MultiSelectMode) {
	      Object o;
	      if (m_RowSelectionMode) {
	        o = new Integer(row);
	      } else {
	        o = new Point(col, row);
	      }
	      if (m_Selection.get(o) != null) {
	        m_Selection.remove(o);
	        return false;
	      } else {
	        m_Selection.put(o, o);
	        return true;
	      }
	    }
	    return false;
	  }

	  /*
	   * works in both modes: Cell and Row Selection. Has no redraw functionality!
	   */
	  protected void addToSelection(int col, int row) {
	    if (m_MultiSelectMode) {
	      if (m_RowSelectionMode) {
	        Integer o = new Integer(row);
	        m_Selection.put(o, o);
	      } else {
	        Point o = new Point(col, row);
	        m_Selection.put(o, o);
	      }
	    }
	    // System.out.println(m_Selection.size()+" "+col+"/"+row);
	  }

	  /**
	   * Selects the given cell. If scroll is true, it scrolls to show this cell
	   * if neccessary. In Row Selection Mode, the given row is selected and a
	   * scroll to the given column is done. Does nothing if the cell does not
	   * exist.
	   * 
	   * @param col
	   * @param row
	   * @param scroll
	   */
	  public void setSelection(int col, int row, boolean scroll) {
	    if (col < m_Model.getColumnCount()
	        && col >= m_Model.getFixedColumnCount()
	        && row < m_Model.getRowCount()
	        && row >= m_Model.getFixedRowCount()) {
	      focusCell(col, row, 0);
	      if (scroll) {
	        scrollToFocus();
	      }
	    }
	  }

	  /**
	   * Returns true, if the given cell is selected. Works also in Row Selection
	   * Mode.
	   * 
	   * @param col
	   * @param row
	   * @return boolean
	   */
	  public boolean isCellSelected(int col, int row) {
	    if (!m_MultiSelectMode) {
	      if (m_RowSelectionMode)
	        return (row == m_FocusRow);
	      return (col == m_FocusCol && row == m_FocusRow);
	    }

	    if (m_RowSelectionMode)
	      return (m_Selection.get(new Integer(row)) != null);
	    else
	      return (m_Selection.get(new Point(col, row)) != null);
	  }

	  /**
	   * Returns true, if the given row is selected. Returns always false if not
	   * in Row Selection Mode!
	   * 
	   * @param row
	   * @return boolean
	   */
	  public boolean isRowSelected(int row) {
	    return (m_Selection.get(new Integer(row)) != null);
	  }

	  /**
	   * Returns an array of the selected row numbers. Returns null if not in Row
	   * Selection Mode. Returns an array with one or none element if not in Multi
	   * Selection Mode.
	   * 
	   * @return int[]
	   */
	  public int[] getRowSelection() {
	    if (!m_RowSelectionMode)
	      return null;
	    if (!m_MultiSelectMode) {
	      if (m_FocusRow < 0)
	        return new int[0];
	      int[] tmp = new int[1];
	      tmp[0] = m_FocusRow;
	      return tmp;
	    }

	    Object[] ints = m_Selection.values().toArray();
	    int[] erg = new int[ints.length];

	    for (int i = 0; i < erg.length; i++) {
	      erg[i] = ((Integer) ints[i]).intValue();
	    }
	    return erg;
	  }

	  /**
	   * Returns an array of the selected cells as Point[]. The columns are stored
	   * in the x fields, rows in y fields. Returns null if in Row Selection Mode.
	   * Returns an array with one or none element if not in Multi Selection Mode.
	   * 
	   * @return int[]
	   */
	  public Point[] getCellSelection() {
	    if (m_RowSelectionMode)
	      return null;
	    if (!m_MultiSelectMode) {
	      if (m_FocusRow < 0 || m_FocusCol < 0)
	        return new Point[0];
	      Point[] tmp = new Point[1];
	      tmp[0] = new Point(m_FocusCol, m_FocusRow);
	      return tmp;
	    }

	    return (Point[]) m_Selection.values().toArray(new Point[1]);
	  }

	  // ////////////////////////////////////////////////////////////////////////////
	  // MODEL
	  // ////////////////////////////////////////////////////////////////////////////

	  /**
	   * Sets the table model. The table model provides data to the table.
	   * 
	   * @see de.kupzog.ktable.KTableModel for more information.
	   * @param model
	   */
	  public void setModel(KTableModel model) {
	    m_Model = model;
	    m_FocusCol = -1;
	    m_FocusRow = -1;
	    clearSelectionWithoutRedraw();
	    redraw();
	  }

	  /**
	   * returns the current table model
	   * 
	   * @return KTableModel
	   */
	  public KTableModel getModel() {
	    return m_Model;
	  }

	  public static void main(String[] args) {
		    // create a shell...
		    Display display = new Display();
		    Shell shell = new Shell(display);
		    shell.setLayout(new FillLayout());
		    shell.setText("KTable examples");

		    // put a tab folder in it...
		    TabFolder tabFolder = new TabFolder(shell, SWT.NONE);

		    // Item 1: a Text Table
		    TabItem item1 = new TabItem(tabFolder, SWT.NONE);
		    item1.setText("Text Table");
		    Composite comp1 = new Composite(tabFolder, SWT.NONE);
		    item1.setControl(comp1);
		    comp1.setLayout(new FillLayout());

		    // put a table in tabItem1...
		    KTable table = new KTable(comp1, SWT.V_SCROLL | SWT.H_SCROLL);
		    table.setRowSelectionMode(true);
		    //table.setMultiSelectionMode(true);
		    table.setModel(new KTableModelExample());

		    // display the shell...
		    shell.setSize(600, 600);
		    shell.open();
		    while (!shell.isDisposed()) {
		      if (!display.readAndDispatch())
		        display.sleep();
		    }
		    display.dispose();
		  }
	  
	}
