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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.twinsoft.convertigo.beans.common.XMLVector;

public class ArrayEditorRowList {

	private XMLVector<Object> lines = null;
	private List<ArrayEditorRow> rows = null;
	private Set<IArrayListViewer> changeListeners = new HashSet<IArrayListViewer>();
	
	public ArrayEditorRowList(XMLVector<Object> lines) {
		super();
		this.lines = new XMLVector<Object>(lines);
		initData();
	}

	private void initData() {
		rows = new LinkedList<ArrayEditorRow>();
		for(Object line : lines)
			rows.add(new ArrayEditorRow(line));
	}
	
	public List<ArrayEditorRow> getRows() {
		return rows;
	}
	
	public void addRow(ArrayEditorRow row) {
		rows.add(row);
		for(IArrayListViewer listener : changeListeners)
			listener.addRow(row);
	}
	
	public void insertRow(ArrayEditorRow row, int position) {
		if (position >= 0)
			rows.add(position, row);
		else	rows.add(row);
		for(IArrayListViewer listener : changeListeners)
			listener.insertRow(row, position);
	}

	public void removeRow(ArrayEditorRow row) {
		rows.remove(row);
		for(IArrayListViewer listener : changeListeners)
			listener.removeRow(row);
	}

	public void rowChanged(ArrayEditorRow row) {
		for(IArrayListViewer listener : changeListeners)
			listener.updateRow(row);
	}

	public void removeChangeListener(IArrayListViewer viewer) {
		changeListeners.remove(viewer);
	}

	public void addChangeListener(IArrayListViewer viewer) {
		changeListeners.add(viewer);
	}
}
