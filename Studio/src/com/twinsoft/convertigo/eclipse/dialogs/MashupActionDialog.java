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

import com.twinsoft.convertigo.eclipse.MashupActionConfiguration;
import com.twinsoft.convertigo.eclipse.MashupDataViewConfiguration;

public class MashupActionDialog extends MyAbstractDialog {
	
	public static final int TYPE_ADD = 0;
	public static final int TYPE_UPDATE = 1;
	public static final int TYPE_DELETE = 2;
	
	private MashupActionDialogComposite mashupDialogComposite = null;
	private MashupDataViewConfiguration mdc = null;
	private MashupActionConfiguration mac = null;
	private int type = 0;
	
	public String dataview = null;
	
	public MashupActionDialog(Shell parentShell, int type, MashupDataViewConfiguration mdc, MashupActionConfiguration mac) {
		this(parentShell, MashupActionDialogComposite.class, "Mashup dataviews integration");
		this.type = type;
		this.mdc = mdc;
		this.mac = mac;
	}
	
	protected MashupActionDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle) {
		super(parentShell, dialogAreaClass, dialogTitle, 450, 300);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control dialogArea =  super.createDialogArea(parent);
		if (mashupDialogComposite == null) {
			mashupDialogComposite = (MashupActionDialogComposite)dialogComposite;
			mashupDialogComposite.fillList(type, mdc, mac);
		}
		return dialogArea;
	}
	
	@Override
	protected void okPressed() {
		dataview = mashupDialogComposite.list.getSelection()[0];
		super.okPressed();
	}
}
