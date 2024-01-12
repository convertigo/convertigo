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

package com.twinsoft.convertigo.eclipse.wizards.new_object;


import java.beans.BeanInfo;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ObjectWithSameNameException;

public class ObjectExplorerWizardPage extends WizardPage {
	private Class<DatabaseObject> beanClass = null;
	private Object parentObject = null;
	private Composite composite = null;
	private DatabaseObject newBean = null;

	ObjectExplorerWizardPage(Object parentObject, Class<DatabaseObject> beanClass) {
		super("ObjectExplorerWizardPage");
		this.beanClass = beanClass;
		this.parentObject = parentObject;
	}

	public void createControl(Composite parent) {
		composite = new ObjectsExplorerComposite(this, parent, SWT.NULL, parentObject, beanClass);
		setControl(composite);
	}

	public BeanInfo getCurrentSelectedBeanInfo() {
		BeanInfo bi = null;
		if (composite != null) {
			bi = ((ObjectsExplorerComposite)composite).getCurrentSelectedBeanInfo();
		}
		return bi;
	}

	private void createBean() {
		BeanInfo bi = getCurrentSelectedBeanInfo();
		if (bi != null) {
			try {
				newBean = (DatabaseObject) bi.getBeanDescriptor().getBeanClass().getConstructor().newInstance();
				if (parentObject instanceof DatabaseObject) {
					newBean.setParent((DatabaseObject) parentObject);
				}
				
				// case we create a javelinConnector
				if (newBean instanceof JavelinConnector) {
					((JavelinConnector)newBean).setEmulatorTechnology(com.twinsoft.api.Session.AS400);
					((JavelinConnector)newBean).emulatorID = ((JavelinConnector)newBean).findEmulatorId();
					((JavelinConnector)newBean).setServiceCode(",DIR|localhost:23");
				}

				if (newBean instanceof FullSyncConnector && parentObject instanceof Project) {
					boolean bContinue = true;
					String name = ((Project) parentObject).getName().toLowerCase() + "_fullsync";
					while (bContinue) {
						try {
							newBean.setName(name);
							bContinue = false;
						} catch (ObjectWithSameNameException e) {
							name = DatabaseObject.incrementName(name);
						}
					}
				}
			} catch (Exception e) {
				newBean = null;
			}
		}
	}

	public DatabaseObject getCreatedBean() {
		return newBean;
	}

	public Class<?> getCreatedBeanClass() {
		if (newBean != null) {
			return newBean.getClass();
		}
		return null;
	}

	private void setInfoBeanName() {
		if (newBean != null) {
			try {
				String name = newBean.getName();
				((ObjectInfoWizardPage)getWizard().getPage("ObjectInfoWizardPage")).setBeanName(name);
				((ObjectInfoWizardPage)getWizard().getPage("ObjectInfoWizardPage")).fillTree(newBean.getClass());
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
