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
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

public abstract class KTableCellEditor {
	/*******************************************************************************
	 * Copyright (C) 2004 by Friederich Kupzog Elektronik & Software All rights
	 * reserved. This program and the accompanying materials are made available
	 * under the terms of the Eclipse Public License v1.0 which accompanies this
	 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
	 * 
	 * Contributors: Friederich Kupzog - initial API and implementation
	 * fkmk@kupzog.de www.kupzog.de/fkmk
	 ******************************************************************************/

	  protected KTableModel m_Model;

	  protected KTable m_Table;

	  protected Rectangle m_Rect;

	  protected int m_Row;

	  protected int m_Col;

	  protected Control m_Control;

	  protected String toolTip;

	  /**
	   * disposes the editor and its components
	   */
	  public void dispose() {
	    if (m_Control != null) {
	      m_Control.dispose();
	      m_Control = null;
	    }
	  }

	  /**
	   * Activates the editor at the given position.
	   * 
	   * @param row
	   * @param col
	   * @param rect
	   */
	  public void open(KTable table, int col, int row, Rectangle rect) {
	    m_Table = table;
	    m_Model = table.getModel();
	    m_Rect = rect;
	    m_Row = row;
	    m_Col = col;
	    if (m_Control == null) {
	      m_Control = createControl();
	      m_Control.setToolTipText(toolTip);
	      m_Control.addFocusListener(new FocusAdapter() {
	        public void focusLost(FocusEvent arg0) {
	          close(true);
	        }
	      });
	    }
	    setBounds(m_Rect);
	    GC gc = new GC(m_Table);
	    m_Table.drawCell(gc, m_Col, m_Row);
	    gc.dispose();
	  }

	  /**
	   * Deactivates the editor.
	   * 
	   * @param save
	   *            If true, the content is saved to the underlying table.
	   */
	  public void close(boolean save) {
	    m_Table.m_CellEditor = null;
	    // m_Control.setVisible(false);
	    GC gc = new GC(m_Table);
	    m_Table.drawCell(gc, m_Col, m_Row);
	    gc.dispose();
	    this.dispose();
	  }

	  /**
	   * Returns true if the editor has the focus.
	   * 
	   * @return boolean
	   */
	  public boolean isFocused() {
	    if (m_Control == null)
	      return false;
	    return m_Control.isFocusControl();
	  }

	  /**
	   * Sets the editor's position and size
	   * 
	   * @param rect
	   */
	  public void setBounds(Rectangle rect) {
	    if (m_Control != null)
	      m_Control.setBounds(rect);
	  }

	  /*
	   * Creates the editor's control. Has to be overwritten by useful editor
	   * implementations.
	   */
	  protected abstract Control createControl();

	  protected void onKeyPressed(KeyEvent e) {
	    if ((e.character == '\r') && ((e.stateMask & SWT.SHIFT) == 0)) {
	      close(true);
	    } else if (e.character == SWT.ESC) {
	      close(false);
	    } else {
	      m_Table.scrollToFocus();
	    }
	  }

	  protected void onTraverse(TraverseEvent e) {
	    close(true);
	    // m_Table.tryToOpenEditorAt(m_Col+1, m_Row);
	  }

	  /**
	   * @param toolTip
	   */
	  public void setToolTipText(String toolTip) {
	    this.toolTip = toolTip;
	  }

}
