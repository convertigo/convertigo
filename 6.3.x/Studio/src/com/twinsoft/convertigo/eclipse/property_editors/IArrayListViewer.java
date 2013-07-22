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

package com.twinsoft.convertigo.eclipse.property_editors;

public interface IArrayListViewer {
	
	/**
	 * Update the view to reflect the fact that a row was added 
	 * to the row list
	 * 
	 * @param row
	 */
	public void addRow(ArrayEditorRow row);
	
	/**
	 * Update the view to reflect the fact that a row was inserted 
	 * into the row list
	 * 
	 * @param row
	 */
	public void insertRow(ArrayEditorRow row, int position);

	/**
	 * Update the view to reflect the fact that a row was removed 
	 * from the row list
	 * 
	 * @param row
	 */
	public void removeRow(ArrayEditorRow row);
	
	/**
	 * Update the view to reflect the fact that one of the rows
	 * was modified 
	 * 
	 * @param row
	 */
	public void updateRow(ArrayEditorRow row);

}
