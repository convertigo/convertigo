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

package com.twinsoft.convertigo.eclipse.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class ToggleButton extends Composite {	
	private static Color colorOn = null;
	private static Color colorOff = null;
	
	private Button button;

	public ToggleButton(Composite parent, int style) {
		super(parent, style);
		
		if (colorOn == null) {
			colorOn = getDisplay().getSystemColor(SWT.COLOR_GREEN);
			colorOff = getDisplay().getSystemColor(SWT.COLOR_RED);
		}
		
		FillLayout layout = new FillLayout();
		layout.marginHeight = layout.marginWidth =  2;
		setLayout(layout);
		button = new Button(this, SWT.TOGGLE);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateColor();
			}
		});
	}

	public void addSelectionListener (SelectionListener listener) {
		button.addSelectionListener(listener);
	}
	
	public boolean getSelection () {
		return button.getSelection();
	}
	
	public void setSelection (boolean selected) {
		button.setSelection(selected);
		updateColor();
	}
	
	public void setImage (Image image) {
		button.setImage(image);
	}
	
	private void updateColor() {
		setBackground(button.getSelection() ? colorOn : colorOff);
	}
}
