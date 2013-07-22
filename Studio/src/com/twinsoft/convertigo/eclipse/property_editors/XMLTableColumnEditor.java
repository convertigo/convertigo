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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class XMLTableColumnEditor extends TableEditor {

	public XMLTableColumnEditor(Composite parent) {
		super(parent);

        dialogTitle = "XMLTableColumn description";
        columnNames = new String[] { "Column tag name", "XPath", "Childs?" };
        templateData = new Object[] { "column", "./TD", Boolean.TRUE };
    }
    
    public CellEditor[] getColumnEditors(Composite parent) {
    	columnEditors = new CellEditor[3];
    	columnEditors[0] = new TextCellEditor(parent);
    	columnEditors[1] = new TextCellEditor(parent);
    	columnEditors[2] = new ComboBoxCellEditor(parent, new String[]{"true","false"});
		return columnEditors;
    }

	public Object getEditorData() {
		XMLVector<Object> xmlVector = GenericUtils.cast(getValue());
		return xmlVector;
	}
}
