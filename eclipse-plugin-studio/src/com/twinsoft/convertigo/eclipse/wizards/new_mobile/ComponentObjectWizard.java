/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.RouteActionComponent;
import com.twinsoft.convertigo.beans.mobile.components.RouteEventComponent;
import com.twinsoft.convertigo.beans.mobile.components.RouteComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ObjectWithSameNameException;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class ComponentObjectWizard extends Wizard {
	
	private String className = "java.lang.Object";
	private DatabaseObject parentObject = null; 
	private int folderType = -1;
	
    private ComponentExplorerWizardPage objectExplorerPage = null;
    private ComponentInfoWizardPage objectInfoPage = null;
    
    public DatabaseObject newBean = null;

    public ComponentObjectWizard(DatabaseObject selectedDatabaseObject, String newClassName) {
    	this(selectedDatabaseObject, newClassName, -1);
    }
    
    public ComponentObjectWizard(DatabaseObject selectedDatabaseObject, String newClassName, int folderType) {
		super();
		this.parentObject = selectedDatabaseObject;
		this.className = newClassName;
		this.folderType = folderType;
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
			else if (beanClass.equals(RouteComponent.class)) {
				objectExplorerPageTitle = "New Route Component";
				objectExplorerPageMessage = "Please select a route component template.";
			}
			else if (beanClass.equals(RouteActionComponent.class)) {
				objectExplorerPageTitle = "New Action Component";
				objectExplorerPageMessage = "Please select an action component template.";
			}
			else if (beanClass.equals(RouteEventComponent.class)) {
				objectExplorerPageTitle = "New Event Component";
				objectExplorerPageMessage = "Please select an event component template.";
			}
			else if (beanClass.equals(PageComponent.class)) {
				objectExplorerPageTitle = "New Page Component";
				objectExplorerPageMessage = "Please select a page component template.";
			}
			else if (beanClass.equals(UIComponent.class)) {
				objectExplorerPageTitle = "New UI Component";
				objectExplorerPageMessage = "Please select a UI component template.";
			}
			
			objectExplorerPage = new ComponentExplorerWizardPage(parentObject, beanClass, folderType);
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
	
	@Override
	public boolean performCancel() {
		if (objectExplorerPage != null) {
			objectExplorerPage.doCancel();
		}
		newBean = null;
		return super.performCancel();
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
            	
		        // Verify if a child object with same name exist and change name
				while (bContinue) {
					if (index == 0) name = dboName;
					else name = dboName + index;
					newBean.setName(name);
					newBean.hasChanged = true;
					newBean.bNew = true;
					
			        try {
			        	new WalkHelper() {
			        		boolean root = true;
			        		boolean find = false;
							
							@Override
							protected boolean before(DatabaseObject dbo, Class<? extends DatabaseObject> dboClass) {
								boolean isInstance = dboClass.isInstance(newBean);
								find |= isInstance;
								return isInstance;
							}
							
							@Override
							protected void walk(DatabaseObject dbo) throws Exception {
								if (root) {
									root = false;
									super.walk(dbo);
									if (!find) {
										throw new EngineException("You cannot add to a " + newBean.getClass().getSimpleName() + " a database object of type " + parentObject.getClass().getSimpleName());
									}
								} else {
									if (newBean.getName().equalsIgnoreCase(dbo.getName())) {
										throw new ObjectWithSameNameException("Unable to add the object because an object with the same name already exists in target.");
									}
								}
							}

			        	}.init(parentObject);
			        	bContinue = false;
			        } catch (ObjectWithSameNameException owsne) {
						// Silently ignore
						index++;
			        } catch (EngineException ee) {
			        	throw ee;
					} catch (Exception e) {
						throw new EngineException("Exception in create", e);
					}
				}
				
				// Now add bean to target
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
            else {
            	throw new Exception("Could not instantiate bean!");
            }
		}
		catch (Exception e) {
            String message = "Unable to create a new object from class '"+ this.className +"'.";
            ConvertigoPlugin.logException(e, message);
    		if (objectExplorerPage != null) {
    			objectExplorerPage.doCancel();
    		}
    		newBean = null;
		}
	}

}
