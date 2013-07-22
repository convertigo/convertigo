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

/*******************************************************************************
 * Copyright (C) 2004 by Friederich Kupzog Elektronik & Software All rights
 * reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Friederich Kupzog - initial API and implementation
 * fkmk@kupzog.de www.kupzog.de/fkmk
 ******************************************************************************/

public interface KTableCellSelectionListener {

  /**
   * Is called if a non-fixed cell is selected (gets the focus).
   * 
   * @see KTable for an explanation of the term "fixed cells".
   * @param col
   *            the column of the cell
   * @param row
   *            the row of the cell
   * @param statemask
   *            the modifier keys that where pressed when the selection
   *            happened.
   */
  public void cellSelected(int col, int row, int statemask);

  /**
   * Is called if a fixed cell is selected (is clicked).
   * 
   * @see KTable for an explanation of the term "fixed cells".
   * @param col
   *            the column of the cell
   * @param row
   *            the row of the cell
   * @param statemask
   *            the modifier keys that where pressed when the selection
   *            happened.
   */
  public void fixedCellSelected(int col, int row, int statemask);

}
