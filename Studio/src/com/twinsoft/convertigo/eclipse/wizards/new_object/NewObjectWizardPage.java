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

package com.twinsoft.convertigo.eclipse.wizards.new_object;


import java.beans.BeanInfo;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.beans.core.DatabaseObject;

public class NewObjectWizardPage extends WizardPage {
	private Class<DatabaseObject> beanClass = null;
	private Object parentObject = null;
	private Composite composite = null;
	
	public NewObjectWizardPage(String pageName, Object parentObject, Class<DatabaseObject> beanClass) {
		super(pageName);
		this.beanClass = beanClass;
		this.parentObject = parentObject;
	}

	public void createControl(Composite parent) {
		composite = new ObjectsExplorerComposite(parent, SWT.NULL, parentObject, beanClass);
		setControl(composite);
	}
	
    public BeanInfo getCurrentSelectedBeanInfo() {
    	BeanInfo bi = null;
    	if (composite != null) {
   			bi = ((ObjectsExplorerComposite)composite).getCurrentSelectedBeanInfo();
    	}
    	return bi;
    }
	
}
