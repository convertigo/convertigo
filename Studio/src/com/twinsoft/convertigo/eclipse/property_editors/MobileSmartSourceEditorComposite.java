/*
 * Copyright (c) 2001-2016 Convertigo SA.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.eclipse.views.mobile.MobilePickerComposite;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

public class MobileSmartSourceEditorComposite extends AbstractDialogComposite {
	private MobilePickerComposite mpc;
	
	public MobileSmartSourceEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		
		DatabaseObjectTreeObject dbto = cellEditor.databaseObjectTreeObject;
		String source = (String)cellEditor.getEditorData();
		
		this.setLayout(new GridLayout(1, false));
		
		mpc = new MobilePickerComposite(this, true);
		mpc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		mpc.setCurrentInput(dbto, source);
	}

	@Override
	public void dispose() {
		mpc.dispose();
		super.dispose();
	}

	@Override
	public Object getValue() {
		String jsonString = mpc.getSmartSourceString();
		return jsonString == null || jsonString.isEmpty() ? "{}":jsonString;
	}

}
