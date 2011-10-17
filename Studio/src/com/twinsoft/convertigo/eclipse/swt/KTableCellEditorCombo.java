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
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public class KTableCellEditorCombo extends KTableCellEditor {
	/*******************************************************************************
	 * Copyright (C) 2004 by Friederich Kupzog Elektronik & Software All rights
	 * reserved. This program and the accompanying materials are made available
	 * under the terms of the Eclipse Public License v1.0 which accompanies this
	 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
	 * 
	 * Contributors: Friederich Kupzog - initial API and implementation
	 * fkmk@kupzog.de www.kupzog.de/fkmk
	 ******************************************************************************/

	  private CCombo m_Combo;

	  private String m_Items[];

	  public void open(KTable table, int row, int col, Rectangle rect) {
	    super.open(table, row, col, rect);
	    m_Combo.setFocus();
	    m_Combo.setText((String) m_Model.getContentAt(m_Col, m_Row));
	  }

	  public void close(boolean save) {
	    if (save)
	      m_Model.setContentAt(m_Col, m_Row, m_Combo.getText());
	    super.close(save);
	    m_Combo = null;
	  }

	  protected Control createControl() {
	    m_Combo = new CCombo(m_Table, SWT.READ_ONLY);
	    m_Combo.setBackground(Display.getCurrent().getSystemColor(
	        SWT.COLOR_LIST_BACKGROUND));
	    if (m_Items != null)
	      m_Combo.setItems(m_Items);
	    m_Combo.addKeyListener(new KeyAdapter() {
	      public void keyPressed(KeyEvent e) {
	        try {
	          onKeyPressed(e);
	        } catch (Exception ex) {
	        }
	      }
	    });
	    /*
	     * m_Combo.addTraverseListener(new TraverseListener() { public void
	     * keyTraversed(TraverseEvent arg0) { onTraverse(arg0); } });
	     */
	    return m_Combo;
	  }

	  public void setBounds(Rectangle rect) {
	    super.setBounds(new Rectangle(rect.x, rect.y + 1, rect.width,
	        rect.height - 2));
	  }

	  public void setItems(String items[]) {
	    m_Items = items;
	  }

}
