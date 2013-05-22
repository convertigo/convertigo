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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class SmartTypeEditor extends AbstractDialogCellEditor {
    
	public SmartTypeEditor(Composite parent) {
        super(parent);

        dialogTitle = "Smart Type";
        dialogCompositeClass = StepSourceEditorComposite.class;
    }
	
	public Object getEditorData() {
		XMLVector<String> xmlVector = GenericUtils.cast(getValue());
		return xmlVector;
	}

	@Override
	protected Button createButton(Composite parent) {
		// TODO Auto-generated method stub
		return super.createButton(parent);
	}

	@Override
	protected Control createContents(Composite cell) {
		Text text = new Text(cell, SWT.NONE);
		text.setText("lol");
		Control control = super.createContents(cell);
		control.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
		return control;
	}

	@Override
	protected Control createControl(Composite parent) {
		// TODO Auto-generated method stub
		return super.createControl(parent);
	}	
	
}
