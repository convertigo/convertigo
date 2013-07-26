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

import java.lang.reflect.Constructor;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

public class EditorFrameworkDialog extends Dialog {

	private Class<? extends Composite> dialogAreaClass;
	private AbstractDialogCellEditor cellEditor;
	public Object newValue;

	protected EditorFrameworkDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass,
			AbstractDialogCellEditor cellEditor) {
		super(parentShell);
		this.dialogAreaClass = dialogAreaClass;
		this.cellEditor = cellEditor;
		newValue = null;
	}

	private AbstractDialogComposite dialogComposite = null;

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		try {
			Constructor<? extends Composite> constructor = dialogAreaClass.getConstructor(new Class[] {
					Composite.class, int.class, AbstractDialogCellEditor.class });

			GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_BOTH);

			dialogComposite = (AbstractDialogComposite) constructor.newInstance(new Object[] { composite,
					new Integer(SWT.NONE), cellEditor });
			dialogComposite.setLayoutData(gridData);
			dialogComposite.setParentDialog(this);
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unexpected exception");
		}

		
		return composite;
	}

	@Override
	protected Control createContents(Composite parent) {
		Control composite = super.createContents(parent);
		dialogComposite.performPostDialogCreation();
		return composite;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);

		newShell.setText(cellEditor.dialogTitle);

		/*
		 *	Display display = newShell.getDisplay();
		 *	
		 *	Point pt = display.getCursorLocation();
		 *   Monitor [] monitors = display.getMonitors();
		 *
		 *	for (int i= 0; i<monitors.length; i++) {
		 *		if (monitors[i].getBounds().contains(pt)) {
		 *			Rectangle rect = monitors[i].getClientArea();
		 *
		 *           if (rect.x < 0)
		 *        		nLeft = ((rect.width - nWidth) / 2) + rect.x;
		 *			else
		 *         		nLeft = (rect.width - nWidth) / 2;
		 *
		 *	        if (rect.y < 0)
		 *	        	nTop = ((rect.height - nHeight) / 2) + rect.y;
		 *	        else
		 *	        	nTop = (rect.height - nHeight) / 2;
		 *	             
		 *	        break;
		 *	      }
		 *	}
		 *
		 *	newShell.setBounds(nLeft, nTop, nWidth, nHeight);
		 */
	}

	protected void enableOK(boolean enabled) {
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}

	@Override
	protected int getShellStyle() {
		return SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL;
	}

	@Override
	protected void okPressed() {
		newValue = (dialogComposite == null ? null : dialogComposite.getValue());
		super.okPressed();
	}
}
