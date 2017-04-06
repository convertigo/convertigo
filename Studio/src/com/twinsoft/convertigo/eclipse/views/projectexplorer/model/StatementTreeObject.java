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

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IScreenClassContainer;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.statements.CallFunctionStatement;
import com.twinsoft.convertigo.beans.statements.ContinueWithSiteClipperStatement;
import com.twinsoft.convertigo.beans.statements.FunctionStatement;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScHandlerStatement;
import com.twinsoft.convertigo.beans.statements.SimpleStatement;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptStatementEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.parsers.triggers.AbstractTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.ITriggerOwner;
import com.twinsoft.convertigo.engine.parsers.triggers.ScreenClassTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.TriggerXMLizer;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class StatementTreeObject extends DatabaseObjectTreeObject implements IEditableTreeObject, IOrderableTreeObject {
	
	public StatementTreeObject(Viewer viewer, Statement object) {
		this(viewer, object, false);
	}
	
	public StatementTreeObject(Viewer viewer, Statement object, boolean inherited) {
		super(viewer, object, inherited);
		setEnabled(getObject().isEnabled());
		
		if ((!object.bNew) && (object instanceof HandlerStatement)) {
			String normalizedScreenClassName =((object instanceof ScHandlerStatement) ? ((ScHandlerStatement)object).getNormalizedScreenClassName():"");
			String handlerType = ((HandlerStatement)object).getHandlerType();
			
			String beanName = ((object instanceof ScHandlerStatement) ? "on" + normalizedScreenClassName + handlerType: object.getName());
			String objectName = object.getName();
			if (!beanName.equals(objectName)) {
				ConvertigoPlugin.logError("Incorrect handler statement name \""+ objectName +"\" ! Changing it to \""+ beanName +"\"");
				try {
					object.setName(beanName);
					hasBeenModified(true);
				} catch (EngineException e) {
					// TODO Auto-generated catch block
				}
			}
		}
	}
	
	@Override
	public Statement getObject(){
		return (Statement) super.getObject();
	}

	@Override
    public boolean isEnabled() {
		setEnabled(getObject().isEnabled());
    	return super.isEnabled();
    }
	
	
	@Override
	public void treeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		super.treeObjectRemoved(treeObjectEvent);
		
		// Avoid the cast between "UnloadedProjectTreeObject" and "DatabaseObjectTreeObject
		if (!(treeObjectEvent.getSource() instanceof UnloadedProjectTreeObject)) {
			
			DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject)treeObjectEvent.getSource();
			DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
			Statement statement = getObject();
			boolean change = false;
			
			// Case this is a screen class
			if (databaseObject instanceof ScreenClass) {
				ScreenClassTreeObject sto = (ScreenClassTreeObject)treeObjectEvent.getSource();
				String screenClassName = StringUtils.normalize(databaseObject.getName());
				
				// ScreenClass and Statement must have the same connector!
				if (statement.getConnector().equals(sto.getConnectorTreeObject().getObject())) {
					if (statement instanceof ITriggerOwner){
						ITriggerOwner ito = (ITriggerOwner) statement;
						AbstractTrigger atrigger = ito.getTrigger().getTrigger();
						if (atrigger instanceof ScreenClassTrigger){
							ScreenClassTrigger sct = (ScreenClassTrigger) atrigger;
							List<String> screenClasses = sct.getScreenClasses();
							for (int i=0;i<screenClasses.size();i++) {
								if (screenClasses.get(i).equals(screenClassName)) {
									screenClasses.remove(i);
									change=true;
								}
							}
							// Add default root screen class if all have been removed
							if (screenClasses.isEmpty()) {
								IScreenClassContainer<?> iscc = (IScreenClassContainer<?>)sto.getConnectorTreeObject().getObject();
								String defaultScreenClassName = StringUtils.normalize(iscc.getDefaultScreenClass().getName());
								screenClasses.add(defaultScreenClassName);
								change=true;
							}
								
							if (change)
								ito.setTrigger(new TriggerXMLizer(sct));
						}
					}
				}
			}
			
			if (change) try {
				hasBeenModified(true);
				ConvertigoPlugin.getDefault().getProjectExplorerView().refreshTreeObject(this);
			} catch (Exception e) {
				ConvertigoPlugin.logWarning(e, "Could not refresh in tree ScHandlerStatement \""+statement.getName()+"\" !");
			}
		}
	}

	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);
		
		String propertyName = (String)treeObjectEvent.propertyName;
		propertyName = ((propertyName == null) ? "":propertyName);
		
		// If a bean name has changed
		if (propertyName.equals("name")) {
			handlesBeanNameChanged(treeObjectEvent);
		}
	}

	protected void handlesBeanNameChanged(TreeObjectEvent treeObjectEvent) {
		DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject)treeObjectEvent.getSource();
		DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
		Object oldValue = treeObjectEvent.oldValue;
		Object newValue = treeObjectEvent.newValue;
		Statement statement = getObject();
		boolean change = false;

		if (databaseObject instanceof ScreenClass) {
			String oldScreenClassName = StringUtils.normalize((String)oldValue);
			String newScreenClassName = StringUtils.normalize((String)newValue);

			// ScreenClass and Statement must have the same connector!
			if (statement.getConnector().equals(databaseObject.getConnector())) {
				// Modify screenclass handlers name
				if (statement instanceof ScHandlerStatement) {
					ScHandlerStatement scHandlerStatement = (ScHandlerStatement) statement;

					if (scHandlerStatement.getNormalizedScreenClassName().equals(oldScreenClassName)) {
						String handlerType = scHandlerStatement.getHandlerType();
						String beanName = "on" + newScreenClassName + handlerType;
						try {
							scHandlerStatement.setName(beanName);
							scHandlerStatement.setNormalizedScreenClassName(newScreenClassName);
							change = true;
						} catch (EngineException e) {
							ConvertigoPlugin.logWarning(e, "Could not rename ScHandlerStatement from \""+scHandlerStatement.getName()+"\" to \""+beanName+"\" !");
						}
					}
				}

				if(statement instanceof ITriggerOwner){
					ITriggerOwner ito = (ITriggerOwner) statement;
					AbstractTrigger atrigger = ito.getTrigger().getTrigger();
					if(atrigger instanceof ScreenClassTrigger){
						ScreenClassTrigger sct = (ScreenClassTrigger) atrigger;
						List<String> screenClasses = sct.getScreenClasses();
						for(int i=0;i<screenClasses.size();i++)
							if(screenClasses.get(i).equals(oldScreenClassName) && (change=true))
								screenClasses.set(i, newScreenClassName);
						if(change)
							ito.setTrigger(new TriggerXMLizer(sct));
					}
				}
			}
		}
		// Case of connector rename
		else if (databaseObject instanceof SiteClipperConnector) {
			if (statement instanceof ContinueWithSiteClipperStatement){
				boolean isLocalProject = statement.getProject().equals(databaseObject.getProject());
				boolean isSameValue = ((ContinueWithSiteClipperStatement)statement).getSiteClipperConnectorName().equals(oldValue);
				if (isSameValue && isLocalProject) {					
					((ContinueWithSiteClipperStatement)statement).setSiteClipperConnectorName((String)newValue);
					hasBeenModified(true);
					viewer.refresh();						
					getDescriptors();// refresh editors (e.g labels in combobox)
				}
			}
		}
		
		if(statement instanceof CallFunctionStatement &&
				databaseObject.getClass().equals(FunctionStatement.class) &&
				((FunctionStatement)databaseObject).getParentTransaction().equals(statement.getParentTransaction())){
			CallFunctionStatement callfunction = (CallFunctionStatement) statement;
			if(callfunction.getFunctionName().equals(oldValue) && (change=true)) callfunction.setFunctionName(newValue.toString());
		}
			
		
		
		if(change) try {
				hasBeenModified(true);
				ConvertigoPlugin.getDefault().getProjectExplorerView().refreshTreeObject(this);
		} catch (Exception e) {
			ConvertigoPlugin.logWarning(e, "Could not refresh in tree ScHandlerStatement \""+statement.getName()+"\" !");
		}
	}
	
	public void launchEditor(String editorType) {
		// Retrieve the project name
		String projectName = getObject().getProject().getName();
		try {
			// Refresh project resource
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);

			// Get editor type
			if (editorType == null) {
				editorType = "UnknownEditor";
				if (getObject() instanceof SimpleStatement)
					editorType = "JscriptStatementEditor";
			}
				
			// Open editor
			if (editorType.equals("JscriptStatementEditor"))
				openJscriptStatementEditor(project);
			
		} catch (CoreException e) {
			ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
		}
	}
	
	private void openJscriptStatementEditor(IProject project) {
		Statement statement = this.getObject();

		IFile file = project.getFile("/_private/" + statement.getQName() + " " + statement.getName());

		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			try {
				activePage.openEditor(new JscriptStatementEditorInput(file, statement),
										"com.twinsoft.convertigo.eclipse.editors.jscript.JscriptStatementEditor");
			}
			catch(PartInitException e) {
				ConvertigoPlugin.logException(e, "Error while loading the statement editor '" + statement.getName() + "'");
			} 
		}
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (getObject().testAttribute(name, value)) {
			return true;
		}
		return super.testAttribute(target, name, value);
	}
}
