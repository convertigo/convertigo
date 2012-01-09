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

import com.twinsoft.convertigo.beans.connectors.HttpConnector;

public class HttpHeaderForwardEditor extends TableEditor {
    public HttpHeaderForwardEditor(Composite parent) {
        super(parent);

        dialogTitle = "HTTP Header forwarded";
        columnNames = new String[] { "Header Name", "Forward policy"};
        templateData = new Object[] { "header-name", HttpConnector.HTTP_HEADER_FORWARD_POLICY_REPLACE };
    }
    
    @Override
    public CellEditor[] getColumnEditors(Composite parent) {
    	columnEditors = new CellEditor[2];
    	columnEditors[0] = new TextCellEditor(parent);
    	columnEditors[1] = new ComboBoxCellEditor(parent, new String[] {
    		HttpConnector.HTTP_HEADER_FORWARD_POLICY_MERGE,
    		HttpConnector.HTTP_HEADER_FORWARD_POLICY_IGNORE,
    		HttpConnector.HTTP_HEADER_FORWARD_POLICY_REPLACE
    	});
		return columnEditors;
    }
}
