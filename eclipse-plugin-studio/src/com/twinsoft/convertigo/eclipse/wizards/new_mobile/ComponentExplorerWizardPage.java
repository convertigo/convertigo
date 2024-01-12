/*
 * Copyright (c) 2001-2024 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.wizards.new_mobile;


import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.Component;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.engine.EngineException;

class ComponentExplorerWizardPage extends WizardPage {
	private Class<DatabaseObject> beanClass = null;
	private Object parentObject = null;
	private Composite composite = null;
	private DatabaseObject newBean = null;
	private int folderType = -1;

	ComponentExplorerWizardPage(Object parentObject, Class<DatabaseObject> beanClass, int folderType) {
		super("ComponentExplorerWizardPage");
		this.beanClass = beanClass;
		this.parentObject = parentObject;
		this.folderType = folderType;
	}

	public void createControl(Composite parent) {
		composite = new ComponentExplorerComposite(this, parent, SWT.NULL, parentObject, beanClass, folderType);
		setControl(composite);
	}

	public Component getCurrentSelectedComponent() {
		Component c = null;
		if (composite != null) {
			c = ((ComponentExplorerComposite)composite).getCurrentSelectedComponent();
		}
		return c;
	}

	private void createBean() {
		Component c = getCurrentSelectedComponent();
		if (c != null) {
			try {
				newBean = ComponentManager.createBean(c);
			} catch (Exception e) {
				newBean = null;
			}
		}
	}

	public DatabaseObject getCreatedBean() {
		return newBean;
	}

	private void setInfoBeanName() {
		if (newBean != null) {
			try {
				String name = newBean.getName();
				((ComponentInfoWizardPage)getWizard().getPage("ComponentInfoWizardPage")).setBeanName(name);
				((ComponentInfoWizardPage)getWizard().getPage("ComponentInfoWizardPage")).fillTree(newBean.getClass());
			} catch (Exception e) {}
		}
	}

	@Override
	public void setPageComplete(boolean complete) {
		super.setPageComplete(complete);
	}

	@Override
	public boolean isPageComplete() {
		return true;
	}

	@Override
	public IWizardPage getNextPage() {
		if (isPageComplete()) {
			createBean();
			setInfoBeanName();
		}

		return super.getNextPage();
	}

	void showNextPage() {
		if (isPageComplete()) {
			createBean();
			setInfoBeanName();

			IWizardPage page = getNextPage();
			if ((page != null) && isPageComplete()) {
				getWizard().getContainer().showPage(page);
			}
		}
	}

	void doCancel() {
		if (newBean != null) {
			DatabaseObject dbo = newBean.getParent();
			if (dbo != null) {
				try {
					dbo.remove(newBean);
				} catch (EngineException e) {}
				dbo.hasChanged = false;
			}
		}
		newBean = null;
	}
}
