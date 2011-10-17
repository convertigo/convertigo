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

package com.twinsoft.convertigo.eclipse.dialogs;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.eclipse.MashupDataViewConfiguration;

public class MashupDialog extends MyAbstractDialog {
	private MashupDialogComposite mashupDialogComposite = null;
	private MashupDataViewConfiguration mdc = null;
	
	public String dataview = null;
	
	public MashupDialog(Shell parentShell, MashupDataViewConfiguration mdc) {
		this(parentShell, MashupDialogComposite.class, "Mashup dataviews integration");
		this.mdc = mdc;
	}
	
	protected MashupDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle) {
		super(parentShell, dialogAreaClass, dialogTitle, 450, 300);
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control =  super.createContents(parent);
		enableOK(false);
		return control;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control dialogArea =  super.createDialogArea(parent);
		if (mashupDialogComposite == null) {
			mashupDialogComposite = (MashupDialogComposite)dialogComposite;
			mashupDialogComposite.fillCombo(mdc);
		}

		return dialogArea;
	}
	
	@Override
	protected void okPressed() {
		dataview = mashupDialogComposite.combo.getText();
		super.okPressed();
	}
}
