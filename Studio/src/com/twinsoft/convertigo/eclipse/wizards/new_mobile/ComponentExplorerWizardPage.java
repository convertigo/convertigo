/*
 * Copyright (c) 2001-2016 Convertigo SA.
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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.wizards.new_mobile;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.Component;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

public class ComponentExplorerWizardPage extends WizardPage {
	private Class<DatabaseObject> beanClass = null;
	private Object parentObject = null;
	private Composite composite = null;
	private DatabaseObject newBean = null;
	private String helpString = null;
	
	public ComponentExplorerWizardPage(Object parentObject, Class<DatabaseObject> beanClass) {
		super("ComponentExplorerWizardPage");
		this.beanClass = beanClass;
		this.parentObject = parentObject;
	}

	public void createControl(Composite parent) {
		composite = new ComponentExplorerComposite(this, parent, SWT.NULL, parentObject, beanClass);
		setControl(composite);
	}
	
	@Override
	public void performHelp() {
		String href = null;
		/*BeanInfo bi = getCurrentSelectedBeanInfo();
		if (bi != null) {
			String displayName = bi.getBeanDescriptor().getDisplayName();
			if ((displayName != null) && !displayName.equals(""))
				href = getBeanHelpHref(displayName);
		}*/
		
		if ((href == null) || href.equals(""))
			href = "convertigoObjects.html";
		
		String helpPageUri = "/com.twinsoft.convertigo.studio.help/help/helpRefManual/"+ href;
		PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(helpPageUri);
	}

	private String getBeanHelpHref(String displayName) {
		String href = null;
		try {
			int i, j, k, z;
			String s = getHelpString();
			if (s != null) {
				i = s.indexOf(">"+displayName+"<");
				if (i != -1) {
					s = s.substring(0, i+1);
					j = s.lastIndexOf("href=\"");
					if (j != 1) {
						k = j+"href=\"".length();
						z = s.indexOf("\"", k);
						if (z != -1) {
							href = s.substring(k, z);
						}
					}
				}
			}
		}
		catch (Exception e) {
			href = null;
			ConvertigoPlugin.logWarning(e, "Error while analyzing help file \"convertigoObjects.html\"", Boolean.FALSE);
		}
		return href;
	}
	
	private String getHelpString() {
		if (helpString == null) {
			InputStream is = null;
			try {
				Bundle bundle = Platform.getBundle("com.twinsoft.convertigo.studio.help"); 
				is = FileLocator.openStream(bundle, new Path("help/helpRefManual/convertigoObjects.html"), false);
				if (is != null) {
					String line;
					BufferedReader br = new BufferedReader(new InputStreamReader(is));
					while((line = br.readLine()) != null) {
						if (helpString == null) helpString = "";
						helpString += line +"\n";
					}
				}
			}
			catch (Exception e) {
				helpString = null;
				ConvertigoPlugin.logWarning(e, "Error while parsing help file \"convertigoObjects.html\"", Boolean.FALSE);
			}
			finally {
				try {if (is != null) is.close();}
				catch (Exception e) {}
			}
		}
		return helpString;
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
		return (getCurrentSelectedComponent() != null);
	}
	
	@Override
	public IWizardPage getNextPage() {
		if (isPageComplete()) {
			createBean();
			setInfoBeanName();
		}
		
		return super.getNextPage();
	}
	
	public void showNextPage() {
		if (isPageComplete()) {
			createBean();
			setInfoBeanName();

			IWizardPage page = getNextPage();
			if ((page != null) && isPageComplete()) {
				getWizard().getContainer().showPage(page);
			}
		}
	}

}
