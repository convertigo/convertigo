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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.RouteComponent;
import com.twinsoft.convertigo.beans.mobile.components.RouteListenerComponent;
import com.twinsoft.convertigo.beans.mobile.components.RoutingTableComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class ComponentObjectWizard extends Wizard {
	
	private String className = "java.lang.Object";
	private DatabaseObject parentObject = null; 
	
    private ComponentExplorerWizardPage objectExplorerPage = null;
    private ComponentInfoWizardPage objectInfoPage = null;
    
    public DatabaseObject newBean = null;

    public ComponentObjectWizard(DatabaseObject selectedDatabaseObject, String newClassName) {
		super();
		this.parentObject = selectedDatabaseObject;
		this.className = newClassName;
		setWindowTitle("Create a new component");
		setNeedsProgressMonitor(true);
		setHelpAvailable(true);
	}

	public void addPages() {
		try {
			String objectExplorerPageTitle = "?", objectExplorerPageMessage = "?";
			Class<DatabaseObject> beanClass = GenericUtils.cast(Class.forName(className));
			if (beanClass.equals(ApplicationComponent.class)) {
				objectExplorerPageTitle = "New Application Component";
				objectExplorerPageMessage = "Please select a application component template.";
			}
			else if (beanClass.equals(RoutingTableComponent.class)) {
				objectExplorerPageTitle = "New Routing Table Component";
				objectExplorerPageMessage = "Please select a routing table component template.";
			}
			else if (beanClass.equals(RouteListenerComponent.class)) {
				objectExplorerPageTitle = "New Route Listener Component";
				objectExplorerPageMessage = "Please select a route listener component template.";
			}
			else if (beanClass.equals(RouteComponent.class)) {
				objectExplorerPageTitle = "New Route Component";
				objectExplorerPageMessage = "Please select a route component template.";
			}
			else if (beanClass.equals(PageComponent.class)) {
				objectExplorerPageTitle = "New Page Component";
				objectExplorerPageMessage = "Please select a page component template.";
			}
			else if (beanClass.equals(UIComponent.class)) {
				objectExplorerPageTitle = "New UI Component";
				objectExplorerPageMessage = "Please select a UI component template.";
			}
			
			objectExplorerPage = new ComponentExplorerWizardPage(parentObject, beanClass);
			objectExplorerPage.setTitle(objectExplorerPageTitle);
			objectExplorerPage.setMessage(objectExplorerPageMessage);
			this.addPage(objectExplorerPage);
			
			//if (!beanClass.equals(UIComponent.class)) {
				objectInfoPage = new ComponentInfoWizardPage(parentObject);
				this.addPage(objectInfoPage);
			//}
			
		} catch (ClassNotFoundException e) {
            String message = java.text.MessageFormat.format("Unable to find the \"{0}\" class.", new Object[] {className});
            ConvertigoPlugin.logWarning(message);
		}
		finally {
			;
		}
	}

	
	private DatabaseObject getCreatedBean() {
		DatabaseObject dbo = null;
		if (objectExplorerPage != null) {
			dbo = objectExplorerPage.getCreatedBean();
		}
		return dbo;
	}
	
	public boolean canFinish() {
		return getContainer().getCurrentPage().getNextPage() == null;
	}

	public boolean performFinish() {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};

		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}
	
	private void doFinish(IProgressMonitor monitor) throws CoreException {
		String dboName, name;
		boolean bContinue = true;
		int index = 0;

		try {
			newBean = getCreatedBean();
            if (newBean != null) {
    			monitor.setTaskName("Object created");
    			monitor.worked(1);
    			
            	dboName = newBean.getName();
				if (!StringUtils.isNormalized(dboName))
					throw new EngineException("Bean name is not normalized : \""+dboName+"\".");
            	
				while (bContinue) {
					if (index == 0) name = dboName;
					else name = dboName + index;
					newBean.setName(name);
					newBean.hasChanged = true;
					newBean.bNew = true;
					
					try {
						parentObject.add(newBean);
		    			monitor.setTaskName("Object added");
		    			monitor.worked(1);
						
						ConvertigoPlugin.logInfo("New object class '"+ this.className +"' named '" + newBean.getName() + "' has been added");
		    			monitor.setTaskName("Object setted up");
		    			monitor.worked(1);

						bContinue = false;
					}
					catch(com.twinsoft.convertigo.engine.ObjectWithSameNameException owsne) {
						if (newBean instanceof HandlerStatement) {
							throw owsne;
						}
						index++;
					}
				}
            }
            else {
            	throw new Exception("Could not instantiate bean!");
            }
		}
		catch (Exception e) {
            String message = "Unable to create a new object from class '"+ this.className +"'.";
            ConvertigoPlugin.logException(e, message);
            newBean = null;
		}
	}

}
