/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.property_editors;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;

public class SmtpAttachmentEditor extends TableEditor {

	public SmtpAttachmentEditor(Composite parent) {
		super(parent);

        dialogTitle = "SMTP Attachments";
        columnNames = new String[] { "Filepath", "Filename" };
        templateData = new Object[] { "\"\"", "" };
    }
    
    public CellEditor[] getColumnEditors(Composite parent) {
    	columnEditors = new CellEditor[2];
    	columnEditors[0] = new ScriptCellEditor(parent);
    	columnEditors[1] = new ScriptCellEditor(parent);
		return columnEditors;
    }

}
