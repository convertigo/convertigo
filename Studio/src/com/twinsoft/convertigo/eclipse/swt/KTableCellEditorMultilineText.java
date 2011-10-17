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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

public class KTableCellEditorMultilineText extends KTableCellEditor {
	/*******************************************************************************
	 * Copyright (C) 2004 by Friederich Kupzog Elektronik & Software All rights
	 * reserved. This program and the accompanying materials are made available
	 * under the terms of the Eclipse Public License v1.0 which accompanies this
	 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
	 * 
	 * Contributors: Friederich Kupzog - initial API and implementation
	 * fkmk@kupzog.de www.kupzog.de/fkmk
	 ******************************************************************************/
	  private Text m_Text;

	  public void open(KTable table, int col, int row, Rectangle rect) {
	    super.open(table, col, row, rect);
	    m_Text.setText(m_Model.getContentAt(m_Col, m_Row).toString());
	    m_Text.selectAll();
	    m_Text.setVisible(true);
	    m_Text.setFocus();
	  }

	  public void close(boolean save) {
	    if (save)
	      m_Model.setContentAt(m_Col, m_Row, m_Text.getText());
	    m_Text = null;
	    super.close(save);
	  }

	  protected Control createControl() {
	    m_Text = new Text(m_Table, SWT.MULTI | SWT.V_SCROLL);
	    m_Text.addKeyListener(new KeyAdapter() {
	      public void keyPressed(KeyEvent e) {
	        try {
	          onKeyPressed(e);
	        } catch (Exception ex) {
	        }
	      }
	    });
	    m_Text.addTraverseListener(new TraverseListener() {
	      public void keyTraversed(TraverseEvent arg0) {
	        onTraverse(arg0);
	      }
	    });
	    return m_Text;
	  }

	  /*
	   * overridden from superclass
	   */
	  public void setBounds(Rectangle rect) {
	    super.setBounds(new Rectangle(rect.x, rect.y, rect.width, rect.height));
	  }

}
