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

package com.twinsoft.convertigo.eclipse.editors;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.eclipse.viewers.ScriptCellEditor;

public class HTTPStatementEditor {
	static public String[] getColumnNames() {
		return new String[] { "Variable", "Description", "Default value", "Multi", "Method", "HTTP variable" };
	}
	
	static public Object[] getDefaultData() {
		return new Object[] { "variable", "description", "", Boolean.FALSE, "GET", "variable" };
	}
	
	static public CellEditor[] getColumnEditor(Composite parent) {
		CellEditor[] columnEditors = new CellEditor[6];
    	columnEditors[0] = new ScriptCellEditor(parent);
    	columnEditors[1] = new TextCellEditor(parent);
    	columnEditors[2] = new TextCellEditor(parent);
    	columnEditors[3] = new ComboBoxCellEditor(parent, new String[]{"true","false"});
    	columnEditors[4] = new ComboBoxCellEditor(parent, new String[]{"","GET","POST"});
    	columnEditors[5] = new TextCellEditor(parent);
		return columnEditors;
	}
}
