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

import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;

public abstract class TableEditor extends AbstractDialogCellEditor {

    /**
     * The data vector.
     */
    protected List<List<Object>> data = null;

    /**
     * The template data used for creating new rows.
     */
    protected Object[] templateData = null;

    /**
     * The column names.
     */
    protected String[] columnNames = null;
    
    /**
     * The column sizes.
     */
    protected int[] columnSizes = null;
    
    /**
     * The column alignments.
     */
    protected int[] columnAlignments = null;
    
    /**
     * The column editors
     */
    protected CellEditor[] columnEditors = null;

    public TableEditor(Composite parent) {
    	super(parent);
        dialogCompositeClass = TableEditorComposite.class;
    }

    public CellEditor[] getColumnEditors(Composite parent) {
    	columnEditors = new CellEditor[columnNames.length];
		for (int i = 0 ; i < columnNames.length ; i++)  {
			columnEditors[i] = new TextCellEditor(parent);
		}
		return columnEditors;
    }
}
