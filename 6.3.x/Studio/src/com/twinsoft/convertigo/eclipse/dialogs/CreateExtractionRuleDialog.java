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

import java.beans.BeanInfo;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.eclipse.wizards.new_object.ObjectsExplorerComposite;

public class CreateExtractionRuleDialog extends Dialog {

	private String dialogTitle;
	private Object parentObject = null;
	public Class<?> beanClass = null;
	
	public CreateExtractionRuleDialog(Shell parentShell, Object parentObject) {
		this(parentShell, ObjectsExplorerComposite.class, "Extraction rule creation", parentObject);
	}
	public CreateExtractionRuleDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle, Object parentObject) {
		super(parentShell);
		this.dialogTitle = dialogTitle;
		this.parentObject = parentObject;
	}
	
	private ObjectsExplorerComposite objectsExplorerComposite = null;

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		try {
			GridData gridData = new GridData (GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_BOTH);
			
			objectsExplorerComposite = new ObjectsExplorerComposite(composite, SWT.NONE,
					parentObject, ExtractionRule.class);
			objectsExplorerComposite.setLayoutData(gridData);
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
		BeanInfo bi = objectsExplorerComposite.getCurrentSelectedBeanInfo();
		if (bi != null) {
			beanClass = bi.getBeanDescriptor().getBeanClass();
		}
		super.okPressed();
	}
}
