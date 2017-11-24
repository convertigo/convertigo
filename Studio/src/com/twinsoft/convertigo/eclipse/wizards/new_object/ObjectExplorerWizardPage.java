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

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IXPathable;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.statements.XpathableStatement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

public class ObjectExplorerWizardPage extends WizardPage {
	private Class<DatabaseObject> beanClass = null;
	private Object parentObject = null;
	private Composite composite = null;
	private DatabaseObject newBean = null;
	private String xpath = null;
	private String helpString = null;
	
	public ObjectExplorerWizardPage(Object parentObject, Class<DatabaseObject> beanClass, String xpath) {
		super("ObjectExplorerWizardPage");
		this.beanClass = beanClass;
		this.parentObject = parentObject;
		this.xpath = xpath;
	}

	public void createControl(Composite parent) {
		composite = new ObjectsExplorerComposite(this, parent, SWT.NULL, parentObject, beanClass);
		setControl(composite);
	}
	
	@Override
	public void performHelp() {
		String href = null;
		BeanInfo bi = getCurrentSelectedBeanInfo();
		if (bi != null) {
			String displayName = bi.getBeanDescriptor().getDisplayName();
			if ((displayName != null) && !displayName.equals(""))
				href = getBeanHelpHref(displayName);
		}
		
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
				newBean = (DatabaseObject) bi.getBeanDescriptor().getBeanClass().newInstance();
				if (parentObject instanceof DatabaseObject) {
					newBean.setParent((DatabaseObject) parentObject);
				}
				
				if (xpath != null) {
					if (newBean instanceof IXPathable) {
						((IXPathable)newBean).setXpath(xpath);
					}
					// case we create an "javascriptable" statement
					if (newBean instanceof XpathableStatement) {
						((XpathableStatement)newBean).setPureXpath(xpath);
					}
				}
				// case we create a javelinConnector
				if (newBean instanceof JavelinConnector) {
					((JavelinConnector)newBean).setEmulatorTechnology(com.twinsoft.api.Session.AS400);
					((JavelinConnector)newBean).emulatorID = ((JavelinConnector)newBean).findEmulatorId();
					((JavelinConnector)newBean).setServiceCode(",DIR|localhost:23");
				}
				
				if (newBean instanceof FullSyncConnector && parentObject instanceof Project) {
					newBean.setName(((Project) parentObject).getName().toLowerCase() + "_fullsync");
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
		return (getCurrentSelectedBeanInfo() != null);
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
