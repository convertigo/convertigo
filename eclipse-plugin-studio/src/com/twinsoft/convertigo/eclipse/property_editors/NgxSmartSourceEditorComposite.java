/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.eclipse.views.mobile.NgxPickerComposite;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

public class NgxSmartSourceEditorComposite extends AbstractDialogComposite {
	private NgxPickerComposite npc;
	
	public NgxSmartSourceEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		
		DatabaseObjectTreeObject dbto = cellEditor.databaseObjectTreeObject;
		String source = (String) cellEditor.getEditorData();
		setLayout(GridLayoutFactory.fillDefaults().create());
		npc = new NgxPickerComposite(this, true);
		npc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		npc.setCurrentInput(dbto, source);
		getShell().setMinimumSize(775, 600);
		getShell().layout();
	}

	@Override
	public void dispose() {
		npc.dispose();
		super.dispose();
	}

	@Override
	public Object getValue() {
		String jsonString = npc.getSmartSourceString();
		return jsonString == null || jsonString.isEmpty() ? "{}" : jsonString;
	}

}
