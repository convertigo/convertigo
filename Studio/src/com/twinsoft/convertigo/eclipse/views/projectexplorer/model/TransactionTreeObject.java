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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.transactions.AbstractHttpTransaction;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.ButtonSpec;
import com.twinsoft.convertigo.eclipse.dialogs.CustomDialog;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTransactionEditor;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTransactionEditorInput;
import com.twinsoft.convertigo.eclipse.editors.xml.XMLTransactionEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.ProjectUtils;
import com.twinsoft.convertigo.engine.util.Replacement;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.util.StringEx;

public class TransactionTreeObject extends DatabaseObjectTreeObject implements IEditableTreeObject {
		
	public TransactionTreeObject(Viewer viewer, Transaction object) {
		this(viewer, object, false);
	}

	public TransactionTreeObject(Viewer viewer, Transaction object, boolean inherited) {
		super(viewer, object, inherited);
		isDefault = ((Transaction)object).isDefault;
	}

	@Override
	public Transaction getObject(){
		return (Transaction) super.getObject();
	}
	
	@Override
	public void hasBeenModified(boolean modified) {
		super.hasBeenModified(modified);
		if (modified && (getObject() instanceof HtmlTransaction)) {
			HtmlConnector htmlConnector = (HtmlConnector)((HtmlTransaction)getObject()).getConnector();
			htmlConnector.checkForStateless();
		}
	}

	
	@Override
	public void treeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		super.treeObjectRemoved(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (!(treeObject.equals(this)) && (treeObject.getParents().contains(this))) {
			if (treeObject instanceof DatabaseObjectTreeObject) {
				DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
				
				// A variable has been removed
				if (databaseObject instanceof Variable) {
					String variableName = databaseObject.getName();
					
					// This is an AbstractHttpTransaction
					if (getObject() instanceof AbstractHttpTransaction) {
						AbstractHttpTransaction httpTransaction = (AbstractHttpTransaction)getObject();
						String transactionSubDir = httpTransaction.getSubDir();
						List<String> pathVariableList = AbstractHttpTransaction.getPathVariableList(httpTransaction.getSubDir());
						
						// Update transaction SubDir property
						if (pathVariableList.contains(variableName)) {
    						transactionSubDir = transactionSubDir.replaceAll("\\{"+variableName+"\\}", "");
    						httpTransaction.setSubDir(transactionSubDir);
    						httpTransaction.hasChanged = true;
							
		    		    	try {
		    					ConvertigoPlugin.getDefault().getProjectExplorerView().updateTreeObject(this);
		    				} catch (Exception e) {
		    					ConvertigoPlugin.logWarning(e, "Could not update in tree Transaction \""+databaseObject.getName()+"\" !");
		    				}
							
						}
					}
				}
			}
		}
	}

	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);

		String propertyName = (String)treeObjectEvent.propertyName;
		propertyName = ((propertyName == null) ? "" : propertyName);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
			
			// If a bean name has changed
			if ("name".equals(propertyName)) {
				handlesBeanNameChanged(treeObjectEvent);
				
			}
			else if ("sqlQuery".equals(propertyName)) {
				if (treeObject.equals(this)) {
    		    	try {
    		    		SqlTransaction sqlTransaction = (SqlTransaction) databaseObject;
    		    		sqlTransaction.initializeQueries(true);
    		    		
    		    		String oldValue = (String)treeObjectEvent.oldValue;
    		    		detectVariables( sqlTransaction.getSqlQuery(), oldValue,
    		    				sqlTransaction.getVariablesList());
    		    		
    					ConvertigoPlugin.getDefault().getProjectExplorerView().reloadTreeObject(this);
    				} catch (Exception e) {
    					ConvertigoPlugin.logWarning(e, "Could not reload in tree Transaction \""+databaseObject.getName()+"\" !");
    				}
				}
			}
			else if ("subDir".equals(propertyName)) {
				if (treeObject.equals(this)) {
					try {
						Object oldValue = treeObjectEvent.oldValue;
						Object newValue = treeObjectEvent.newValue;
						
						AbstractHttpTransaction httpTransaction = (AbstractHttpTransaction) databaseObject;
						List<String> oldPathVariableList = AbstractHttpTransaction.getPathVariableList(oldValue.toString());
						List<String> newPathVariableList = AbstractHttpTransaction.getPathVariableList(newValue.toString());
						
						// Check for variables to be renamed
						if (oldValue.toString().replaceAll("\\{([a-zA-Z0-9_]+)\\}", "{}").equals(
							newValue.toString().replaceAll("\\{([a-zA-Z0-9_]+)\\}", "{}"))) {
							for (int i=0; i<oldPathVariableList.size(); i++) {
								String oldVariableName = oldPathVariableList.get(i);
								String newVariableName = newPathVariableList.get(i);
								if (!oldVariableName.equals(newVariableName)) {
									RequestableHttpVariable httpVariable = (RequestableHttpVariable) httpTransaction.getVariable(oldVariableName);
									if (httpVariable != null) {
					    		    	try {
											
											VariableTreeObject2 vto = (VariableTreeObject2) findTreeObjectByUserObject(httpVariable);
											int update = TreeObjectEvent.UPDATE_NONE;
											CustomDialog customDialog = new CustomDialog(
													viewer.getControl().getShell(),
													"Update object references",
													"Do you want to update "
															+ "variable"
															+ " references ?\n You can replace '"
															+ oldVariableName
															+ "' by '"
															+ newVariableName
															+ "' in all loaded projects \n or replace '"
															+ oldVariableName
															+ "' by '"
															+ newVariableName
															+ "' in current project only.",
															670, 200,
													new ButtonSpec("Replace in all loaded projects", true),
													new ButtonSpec("Replace in current project", false),
													new ButtonSpec("Do not replace anywhere", false));
											int response = customDialog.open();
											if (response == 0) {
												update = TreeObjectEvent.UPDATE_ALL;
											}
											if (response == 1) {
												update = TreeObjectEvent.UPDATE_LOCAL;
											}
											if (update != 0) {
												httpVariable.setName(newVariableName);
												httpVariable.hasChanged = true;
												TreeObjectEvent toEvent = new TreeObjectEvent(vto, "name", oldVariableName, newVariableName, update);
												ConvertigoPlugin.getDefault().getProjectExplorerView().fireTreeObjectPropertyChanged(toEvent);
											}
					    				} catch (Exception e) {
					    					ConvertigoPlugin.logWarning(e, "Could not rename variable for Transaction \""+databaseObject.getName()+"\" !");
					    				}
									}
								}
							}
						}
						else {
							// Check for variables to be added
							for (String variableName : newPathVariableList) {
								if (httpTransaction.getVariable(variableName) == null) {
									RequestableHttpVariable httpVariable = new RequestableHttpVariable();
									httpVariable.setName(variableName);
									httpVariable.setHttpMethod("GET");
									httpVariable.setHttpName("");
									httpVariable.bNew = true;
									httpVariable.hasChanged = true;
									httpTransaction.addVariable(httpVariable);
									httpTransaction.hasChanged = true;
								}
							}
							// Check for variables to be deleted
							for (String variableName : oldPathVariableList) {
								RequestableHttpVariable httpVariable = (RequestableHttpVariable) httpTransaction.getVariable(variableName);
								if (httpVariable != null) {
									if (!newPathVariableList.contains(variableName)) {
										try {
											MessageBox messageBox = new MessageBox(viewer.getControl().getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO); 
											messageBox.setMessage("Do you want to delete the variable \""+variableName+"\"?");
											messageBox.setText("Delete \""+variableName+"\"?");
											if (messageBox.open() == SWT.YES) {
												httpVariable.delete();
												httpTransaction.hasChanged = true;
											}
										} catch (EngineException e) {
											ConvertigoPlugin.logException(e, "Error when deleting the variable \""+variableName+"\"");
										}
									}
								}
							}
						}
						
						if (httpTransaction.hasChanged) {
							ConvertigoPlugin.getDefault().getProjectExplorerView().reloadTreeObject(this);
						}
						
    				} catch (Exception e) {
    					ConvertigoPlugin.logWarning(e, "Could not reload in tree Transaction \""+databaseObject.getName()+"\" !");
    				}
				}
			}
		}
	}
	
	protected void handlesBeanNameChanged(TreeObjectEvent treeObjectEvent) {
		DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject)treeObjectEvent.getSource();
		DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
		Object oldValue = treeObjectEvent.oldValue;
		Object newValue = treeObjectEvent.newValue;
		
		if (databaseObject instanceof ScreenClass) {
			String oldName = StringUtils.normalize((String)oldValue);
			String newName = StringUtils.normalize((String)newValue);

			Transaction transaction = getObject();
			
			// Modify Screenclass name in Transaction handlers
			if (!(transaction instanceof HtmlTransaction)) {
            	
				// ScreenClass and Transaction must have the same connector!
				if (transaction.getConnector().equals(databaseObject.getConnector())) {
					String oldHandlerPrefix = "on" + StringUtils.normalize(oldName);
	            	String newHandlerPrefix = "on" + StringUtils.normalize(newName);

	            	if (transaction.handlers.indexOf(oldHandlerPrefix) != -1) {
	    				StringEx sx = new StringEx(transaction.handlers);
	            		// Updating comments
	            		sx.replaceAll("handler for screen class \"" + oldName + "\"", "handler for screen class \"" + newName + "\"");
	            		// Updating functions def & calls
	            		sx.replaceAll(oldHandlerPrefix + "Entry", newHandlerPrefix + "Entry");
	            		sx.replaceAll(oldHandlerPrefix + "Exit", newHandlerPrefix + "Exit");
	            		String newHandlers = sx.toString();
	            		
	            		if (!newHandlers.equals(transaction.handlers)) {
	                		transaction.handlers = newHandlers;
	                		hasBeenModified(true);
	            		}
	            		
	    				// Updating the opened handlers editor if any
	    				IEditorPart jspart = ConvertigoPlugin.getDefault().getJscriptTransactionEditor(transaction);
	    				if ((jspart != null) && (jspart instanceof JscriptTransactionEditor)) {
	    					JscriptTransactionEditor jscriptTransactionEditor = (JscriptTransactionEditor)jspart;
	    					jscriptTransactionEditor.reload();
	    				}
	    				
	    		    	try {
	    					ConvertigoPlugin.getDefault().getProjectExplorerView().reloadTreeObject(this);
	    				} catch (Exception e) {
	    					ConvertigoPlugin.logWarning(e, "Could not reload in tree Transaction \""+databaseObject.getName()+"\" !");
	    				}
	            	}
				}
			}
		}
		
		
		if (databaseObject instanceof Variable) {
			String oldVariableName = oldValue.toString();
			String newVariableName = newValue.toString();
			
			// A variable of this transaction has been renamed
			if (getObject().equals(databaseObject.getParent())) {
				if (getObject() instanceof AbstractHttpTransaction) {
					AbstractHttpTransaction httpTransaction = (AbstractHttpTransaction)getObject();
    		    	try {
    		    		// Check for variables to be renamed in SubDir property
    					String transactionSubDir = httpTransaction.getSubDir();
    					List<String> pathVariableList = AbstractHttpTransaction.getPathVariableList(transactionSubDir);
    					if (pathVariableList.contains(oldVariableName)) {
    						transactionSubDir = transactionSubDir.replaceAll("\\{"+oldVariableName+"\\}", "{"+newVariableName+"}");
    						httpTransaction.setSubDir(transactionSubDir);
    						httpTransaction.hasChanged = true;
    					}
    					
    					ConvertigoPlugin.getDefault().getProjectExplorerView().refreshTreeObject(this);
    				} catch (Exception e) {
    					ConvertigoPlugin.logWarning(e, "Could not reload in tree Transaction \""+databaseObject.getName()+"\" !");
    				}
				}
			}
		}
		
		// Case of this transaction rename : update transaction's schema
		if (treeObject.equals(this)) {
			String path = Project.XSD_FOLDER_NAME +"/"
						+ Project.XSD_INTERNAL_FOLDER_NAME + "/"
						+ getConnectorTreeObject().getName();
			
			String oldPath = path + "/" + (String)oldValue + ".xsd";
			String newPath = path + "/" + (String)newValue + ".xsd";
			
			IFile file = getProjectTreeObject().getFile(oldPath);
			if (file.exists()) {
				try {
					// rename file (xsd/internal/connector/transaction.xsd)
					file.move(new Path((String)newValue+".xsd"), true, null);
					
					// make replacements in schema files
					List<Replacement> replacements = new ArrayList<Replacement>();
					replacements.add(new Replacement("__"+(String)oldValue, "__"+(String)newValue));
					IFile newFile = file.getParent().getFile(new Path((String)newValue+".xsd"));
					String newFilePath = newFile.getLocation().makeAbsolute().toString();
					try {
						ProjectUtils.makeReplacementsInFile(replacements, newFilePath);
					} catch (Exception e) {
						ConvertigoPlugin.logWarning(e, "Could not rename \""+oldValue+"\" to \""+newValue+"\" in schema file \""+newPath+"\" !");
					}
					
					// refresh file
					file.refreshLocal(IResource.DEPTH_ZERO, null);
					
					Engine.theApp.schemaManager.clearCache(getProjectTreeObject().getName());
					
				} catch (Exception e) {
					ConvertigoPlugin.logWarning(e, "Could not rename schema file from \""+oldPath+"\" to \""+newPath+"\" !");
				}
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

	public void launchEditor(String editorType) {
		// Retrieve the project name
		String projectName = getObject().getProject().getName();
		try {
			// Refresh project resource
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);

			// Open editor
			if ((editorType == null) || ((editorType != null) && (editorType.equals("JscriptTransactionEditor"))))
				openJscriptTransactionEditor(project);
			if ((editorType != null) && (editorType.equals("XMLTransactionEditor")))
				openXMLTransactionEditor(project);
			
		} catch (CoreException e) {
			ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
		}
	}

	public void openJscriptTransactionEditor(IProject project) {
		Transaction transaction = (Transaction)this.getObject();
		
		String tempFileName = 	"_private/"+project.getName()+
								"__"+transaction.getConnector().getName()+
								"__"+transaction.getName();
		
		IFile file = project.getFile(tempFileName);

		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			try {
				activePage.openEditor(new JscriptTransactionEditorInput(file,transaction),
										"com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTransactionEditor");
			} catch(PartInitException e) {
				ConvertigoPlugin.logException(e, "Error while loading the transaction editor '" + transaction.getName() + "'");
			} 
		}
	}

	public void openXMLTransactionEditor(IProject project) {
		Transaction transaction = (Transaction)this.getObject();
		
		IFile	file = project.getFile("_private/"+transaction.getName()+".xml");
		
		
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			try {
				activePage.openEditor(new XMLTransactionEditorInput(file,transaction),
										"com.twinsoft.convertigo.eclipse.editors.xml.XMLTransactionEditor");
			} catch(PartInitException e) {
				ConvertigoPlugin.logException(e, "Error while loading the transaction editor '" + transaction.getName() + "'");
			} 
		}
	}
	
	/** SQL TRANSACTION **/
	private void detectVariables( String queries, String oldQueries, 
			List<RequestableVariable> listVariables ){
		
		if (queries != null && !queries.equals("")) {
			// We create an another List which permit to compare and update variables
			Set<String> newSQLQueriesVariablesNames = getSetVariableNames(queries);
			Set<String> oldSQLQueriesVariablesNames = getSetVariableNames(oldQueries);
			
			// Modify variables definition if needed
			if ( listVariables != null && 
					!oldSQLQueriesVariablesNames.equals(newSQLQueriesVariablesNames) ) {
				
				for ( RequestableVariable variable : listVariables ) {
					String variableName = variable.getName();
					
					if (oldSQLQueriesVariablesNames.contains(variableName) &&
							!newSQLQueriesVariablesNames.contains(variableName)) {
						
						try {
							MessageBox messageBox = new MessageBox(viewer.getControl().getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO); 
							messageBox.setMessage("Do you really want to delete the variable \""+variableName+"\"?");
							messageBox.setText("Delete \""+variableName+"\"?");
							
							if (messageBox.open() == SWT.YES) {
								variable.delete();
							}
						} catch (EngineException e) {
							ConvertigoPlugin.logException(e, "Error when deleting the variable \""+variableName+"\"");
						}
						
					}
				}
			}
		}
	}
	
	// Permit to get all variables names from SQL query(ies)
	private Set<String> getSetVariableNames(String sqlQueries){
		String[] arrayQueries = sqlQueries.split(";");
		Set<String> listVariablesNames = new HashSet<String>();
		
		for (String query: arrayQueries) {
			if (!query.trim().replaceAll(" ", "").equals("")) {				
				Pattern pattern = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)\\}\\}");
				Matcher matcher = pattern.matcher(sqlQueries);
				
				while (matcher.find()) {
					String variableName = matcher.group(1);
					listVariablesNames.add(variableName);
				}
				sqlQueries = sqlQueries.replaceAll("\\{\\{[a-zA-Z0-9_]+\\}\\}", "");
				
				pattern = Pattern.compile("([\"']?)\\{([a-zA-Z0-9_]+)\\}\\1");
				matcher = pattern.matcher(sqlQueries);
				
				while (matcher.find()) {
					String variableName = matcher.group(2);
					listVariablesNames.add(variableName);
				}
			}
		}
		
		return listVariablesNames;
	}
}