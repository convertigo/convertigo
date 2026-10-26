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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.eclipse.views.jsonpicker.JsonPickerComposite;

public class JsonExpressionEditorComposite extends AbstractDialogComposite {
	private JsonPickerComposite picker;

	public JsonExpressionEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		setLayout(new GridLayout(1, false));
		Step step = (Step) cellEditor.databaseObjectTreeObject.getObject();
		picker = new JsonPickerComposite(this, step, cellEditor.propertyDescriptor.getDisplayName());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 900;
		gd.heightHint = 520;
		picker.setLayoutData(gd);
		Object value = cellEditor.getEditorData();
		picker.setExpression(value == null ? "" : value.toString());
	}

	@Override
	public Object getValue() {
		return picker.getExpression();
	}
}
