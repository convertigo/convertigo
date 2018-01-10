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

public class TableEditorRowList {

	private XMLVector<XMLVector<Object>> lines = null;
	private List<TableEditorRow> rows = null;
	private Set<IRowListViewer> changeListeners = new HashSet<IRowListViewer>();
	
	public TableEditorRowList(XMLVector<XMLVector<Object>> lines) {
		super();
		this.lines = new XMLVector<XMLVector<Object>>(lines);
		initData();
	}

	private void initData() {
		rows = new LinkedList<TableEditorRow>();
		for(XMLVector<Object> line : lines)
			rows.add(new TableEditorRow(line));
	}
	
	public List<TableEditorRow> getRows() {
		return rows;
	}
	
	public void addRow(TableEditorRow row) {
		rows.add(row);
		for(IRowListViewer listener : changeListeners)
			listener.addRow(row);
	}
	
	public void insertRow(TableEditorRow row, int position) {
		if (position >= 0)
			rows.add(position, row);
		else	rows.add(row);
		for(IRowListViewer listener : changeListeners)
			listener.insertRow(row, position);
	}

	public void removeRow(TableEditorRow row) {
		rows.remove(row);
		for(IRowListViewer listener : changeListeners)
			listener.removeRow(row);
	}

	public void rowChanged(TableEditorRow row) {
		for(IRowListViewer listener : changeListeners)
			listener.updateRow(row);
	}

	public void removeChangeListener(IRowListViewer viewer) {
		changeListeners.remove(viewer);
	}

	public void addChangeListener(IRowListViewer viewer) {
		changeListeners.add(viewer);
	}
}
