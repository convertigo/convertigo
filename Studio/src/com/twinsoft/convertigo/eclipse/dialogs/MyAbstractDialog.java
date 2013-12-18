/*
 * Copyright (c) 2001-2013 Convertigo SA.
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

import java.lang.reflect.Constructor;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

public class MyAbstractDialog extends Dialog {

	private Class<? extends Composite> dialogAreaClass;
	private String dialogTitle;
	private int nWidth = 500;
	private int nHeight = 400;
	
	protected MyAbstractDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle) {
		super(parentShell);
		this.setShellStyle(SWT.APPLICATION_MODAL);
		this.dialogAreaClass = dialogAreaClass;
		this.dialogTitle = dialogTitle;
	}
	
	protected MyAbstractDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle, int width, int height) {
		super(parentShell);
		this.setShellStyle(SWT.APPLICATION_MODAL);
		this.dialogAreaClass = dialogAreaClass;
		this.dialogTitle = dialogTitle;
		nWidth = width;
		nHeight = height;
	}

	protected MyAbstractDialogComposite dialogComposite = null;
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		try {
			Constructor<? extends Composite> constructor = dialogAreaClass.getConstructor(new Class[] { Composite.class, int.class });

			GridData gridData = new GridData (GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_BOTH);
			
			dialogComposite = (MyAbstractDialogComposite) constructor.newInstance(new Object[] { composite, new Integer(SWT.NONE) });
			dialogComposite.setLayoutData(gridData);
			dialogComposite.initialize();
			dialogComposite.setParentDialog(this);
		}
		catch(Exception e) {
			ConvertigoPlugin.logException(e, "Unexpected exception");
		}
		
		return composite;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		// TODO Auto-generated method stub
		return super.createContents(parent);
	}

	@Override
	public void configureShell(Shell newShell) {
		super.configureShell(newShell);
		
		newShell.setText(dialogTitle);	
		
		int nLeft = 0;
		int nTop = 0;
		 
		Display display = newShell.getDisplay();

//		// mods jmc 22/10/2013
//		nWidth = newShell.getSize().x;
//		nHeight = newShell.getSize().y;
		
		Point pt = display.getCursorLocation();
	    Monitor [] monitors = display.getMonitors();

	    for (int i= 0; i<monitors.length; i++) {
	          if (monitors[i].getBounds().contains(pt)) {
	             Rectangle rect = monitors[i].getClientArea();
	             
	             if (rect.x < 0)
	         		nLeft = ((rect.width - nWidth) / 2) + rect.x;
	             else
	         		nLeft = (rect.width - nWidth) / 2;

	             if (rect.y < 0)
	         		nTop = ((rect.height - nHeight) / 2) + rect.y;
	             else
	         		nTop = (rect.height - nHeight) / 2;
	             
	             break;
	          }
	    }

	    newShell.setBounds(nLeft, nTop, nWidth, nHeight);
	}	

	@Override
	protected int getShellStyle() {
		return SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL;
	}

	protected void enableOK(boolean enabled) {
		Button OkButton = getButton(IDialogConstants.OK_ID);
		if (OkButton != null)
			OkButton.setEnabled(enabled);
	}
	
	@Override
	protected void okPressed() {
    	super.okPressed();
    }
}
