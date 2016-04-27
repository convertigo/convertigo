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

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.wizards.new_object.ObjectsExplorerComposite;
import com.twinsoft.convertigo.engine.EngineException;

public class CreateHandlerDialog extends Dialog {
	private String dialogTitle;
	private Object parentObject = null;
	public List<?> result = null;
	
	public CreateHandlerDialog(Shell parentShell, Object parentObject) {
		this(parentShell, ObjectsExplorerComposite.class, "New transaction handler", parentObject);
	}
	
	public CreateHandlerDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle, Object parentObject) {
		super(parentShell);
		this.dialogTitle = dialogTitle;
		this.parentObject = parentObject;
	}
	
	private CreateHandlerDialogComposite createHandlerDialogComposite = null;
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		try {
			GridData gridData = new GridData (GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_BOTH);
			
			createHandlerDialogComposite = new CreateHandlerDialogComposite(composite,SWT.NONE,parentObject);
			createHandlerDialogComposite.setLayoutData(gridData);
		}
		catch(Exception e) {;}
		
		return composite;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(dialogTitle);
	}
	
	@Override
	protected int getShellStyle() {
		return SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL;
	}
	
	@Override
	protected void okPressed() {
		try {
			result = createHandlerDialogComposite.generateHandler();
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e, "Error while creating the handler", true);
		}
		super.okPressed();
	}
}
