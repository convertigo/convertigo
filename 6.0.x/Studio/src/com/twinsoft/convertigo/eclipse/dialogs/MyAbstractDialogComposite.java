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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public abstract class MyAbstractDialogComposite extends Composite {
	
	protected Dialog parentDialog;
	public String dialogTitle = "";
	
	public MyAbstractDialogComposite(Composite parent, int style) {
		super(parent, style);
	}
	
	protected void initialize() {
		GridLayout gridLayout = new GridLayout();
		this.setLayout(gridLayout);
//		setSize(new org.eclipse.swt.graphics.Point(402,289));
	}
	
	public abstract Object getValue(String name);
	
	protected void setParentDialog(Dialog parentDialog) {
		this.parentDialog = parentDialog;
	}
}
