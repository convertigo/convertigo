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
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;

public class BrowserDefinitionEditor extends TableEditor {
    public BrowserDefinitionEditor(Composite parent) {
        super(parent);

        dialogTitle = "Browsers definition";
        templateData = new Object[] { "label", "keyword" };
        columnNames = new String[] { "Label", "Keyword" };
        columnSizes = new int[] { 200, 100 };
        columnEditors = new CellEditor[] { new TextCellEditor(), new TextCellEditor() };
    }
}
