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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTransactionEditor;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTransactionEditorInput;
import com.twinsoft.convertigo.eclipse.editors.xml.XMLTransactionEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.ProjectUtils;
import com.twinsoft.convertigo.engine.util.Replacement;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.util.StringEx;

public class TransactionTreeObject extends DatabaseObjectTreeObject implements IEditableTreeObject {
	
	private boolean isLearning = false;
	private boolean isDreamface = false;
	
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
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);

		String propertyName = (String)treeObjectEvent.propertyName;
		propertyName = ((propertyName == null) ? "":propertyName);
		
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
    					initializeQueries(true);
    					ConvertigoPlugin.getDefault().getProjectExplorerView().reloadTreeObject(this);
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
		if (name.equals("isDefault")) {
			isDefault = getObject().isDefault;
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isDefault));
		}
		if (name.equals("isLearning")) {
			isLearning = getObject().isLearning;
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isLearning));
		}
		if (name.equals("isDreamface")) {
			isDreamface = !EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_MASHUP_URL).equals("");
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isDreamface));
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
	
	/******************************************************** SQL Query ********************************************************/
	transient private List<String> oldOrderedParametersList = null;
	
	/** SqlQueryInfos class **/
	public class SqlQueryInfos {
		
		private String query = "";
		
		private SqlTransaction sqlTransaction;
		
		private int type;
		
		/** Query's ordered parameter names */
		private List<String> orderedParametersList = null;
		
		/** Just use for the parameterized parameter like {{id}} **/
		private List<String> otherParametersList = null;
		
		/** Query's parameters map (name and value) */
		private Map<String, String> parametersMap = new HashMap<String, String>();
		
		public SqlQueryInfos(String thequery, SqlTransaction sqlTransaction, boolean updateDefinitions){
			this.query = thequery.replaceAll("\n", " ").replaceAll("\r", "").trim();
			this.sqlTransaction = sqlTransaction;
			findType();
			this.query = prepareParameters(updateDefinitions);
		}
		
		private void findType(){
			if (query.toUpperCase().indexOf("SELECT") == 0)
				type = 0;
			else if (query.toUpperCase().indexOf("UPDATE") == 0)
				type = 1;
			else if (query.toUpperCase().indexOf("INSERT") == 0)
				type = 2;
			else if (query.toUpperCase().indexOf("DELETE") == 0)
				type = 3;
			else if (query.toUpperCase().indexOf("REPLACE") == 0)
				type = 4;
			else if (query.toUpperCase().indexOf("CREATE TABLE") == 0)
				type = 5;
			else if (query.toUpperCase().indexOf("DROP TABLE") == 0)
				type = 6;
			else if (query.toUpperCase().indexOf("TRUNCATE TABLE") == 0)
				type = 7;
			else type = 99;
		}
		
		/** We prepare the query and create lists **/
		private String prepareParameters(boolean updateDefinitions){
			String preparedSqlQuery = "";

			if ( query != null && sqlTransaction.bNew || updateDefinitions) {
				preparedSqlQuery = query;
				// Handled the case if we have value like {{id}} or "{{id}}" or '{{id}}' (i.e: table name or instructions)		
				Pattern pattern = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)\\}\\}");
				Matcher matcher = pattern.matcher(query);
				
				// Retrieve parameter names
				orderedParametersList = new ArrayList<String>(); // for parameters like {id}
				otherParametersList = new ArrayList<String>();	 // for parameters like {{id}}
				
				// Clear parameters Map
				parametersMap.clear();
				
				while (matcher.find()) {
					String parameterName = matcher.group(1);
					String parameterValue = getParameterValue(parameterName, sqlTransaction.getVariableVisibility(parameterName)).toString();
					preparedSqlQuery = preparedSqlQuery.replace("{{"+parameterName+"}}", parameterValue);
					
					// Add the parameterName into the ArrayList if is looks like {{id}}.	
					otherParametersList.add(parameterName);
					
					matcher = pattern.matcher(preparedSqlQuery);
					
					updateVariable(updateDefinitions, parameterName);
				}
				
				// Handled the case if we have value like {id} (i.e: parameters value)	
				pattern = Pattern.compile("([\"']?)\\{([a-zA-Z0-9_]+)\\}\\1");
				matcher = pattern.matcher(preparedSqlQuery);
				
				while (matcher.find()) {
					String parameterName = matcher.group(2);	
					String parameterValue = getParameterValue(parameterName, sqlTransaction.getVariableVisibility(parameterName)).toString();
	
					// Add the parameterName into the ArrayList if is looks like {id} and not {{id}}.	
					orderedParametersList.add(parameterName);
					
					// Update the parameters map if needed
					if (!parametersMap.containsKey(parameterName)) {
						parametersMap.put(parameterName, parameterValue);
					}
					
					updateVariable(updateDefinitions, parameterName);
					
				}				
				// Replace parameter by question mark (for parameter value injection)
				preparedSqlQuery = matcher.replaceAll("?");
				
			}
			return preparedSqlQuery;
		}
		
		public String getQuery(){
			return query;
		}
		
		public int getType(){
			return type;
		}
		
		public List<String> getOrderedParametersList(){
			return orderedParametersList;
		}
		
		public List<String> getOtherParametersList(){
			return otherParametersList;
		}
		
		public Map<String, String> getParametersMap(){
			return parametersMap;
		}
	}
	/** End of SqlQueryInfos class **/

	private SqlTransaction sqlTransaction = (SqlTransaction) this.getObject();
	
	private void updateVariable(boolean updateDefinitions, String parameterName){
		if (updateDefinitions && (sqlTransaction.getVariable(parameterName) == null)) {
			try {
				if (!StringUtils.isNormalized(parameterName))
					throw new EngineException("Parameter name is not normalized : \""+parameterName+"\".");
				
				RequestableVariable variable = new RequestableVariable();
				variable.setName(parameterName);
				variable.setDescription(parameterName);
				variable.setWsdl(Boolean.TRUE);
				variable.setCachedKey(Boolean.TRUE);
				sqlTransaction.addVariable(variable);

				variable.bNew = true;
				variable.hasChanged = true;
				sqlTransaction.hasChanged = true;
			} catch(EngineException e) {
				Engine.logBeans.error("Could not add variable '"+parameterName+"' for SqlTransaction '"+ getName() +"'", null);
			}
		}
	}
	
	private List<SqlQueryInfos> initializeQueries(boolean updateDefinitions){
		
		if (sqlTransaction.preparedSqlQueries != null ) {
			sqlTransaction.preparedSqlQueries.clear();
			checkVariables(sqlTransaction.preparedSqlQueries);
		} else {
			sqlTransaction.preparedSqlQueries = new ArrayList<SqlQueryInfos>();
		}
		
		// We split the sqlQuery in list array of multiple sqlQuery
		String[] sqlQueries = sqlTransaction.sqlQuery.split(";");
		
		if ( sqlQueries != null) {
			// We loop every query of the String tab and create SqlQueryInfos element for the preparedSqlQueries list
			for ( int i = 0 ; i < sqlQueries.length ; i++ ){
				if ( sqlQueries[i] != null  && !sqlQueries[i].equals(" ") ) {
					SqlQueryInfos sqlQueryInfos = new SqlQueryInfos(sqlQueries[i], (SqlTransaction) this.getObject(), updateDefinitions);
					sqlTransaction.preparedSqlQueries.add(sqlQueryInfos);
				}
			}
		}
		// We create an another List which permit to compare and update variables
		List<String> allParametersList = createAllParametersList(sqlTransaction.preparedSqlQueries);
		
		// Modify variables definition if needed
		if ( !allParametersList.equals(oldOrderedParametersList) ) {
			if ( updateDefinitions && (oldOrderedParametersList != null )) {
				for ( String parameterName : oldOrderedParametersList ) {
					if ( !allParametersList.contains( parameterName ) ) {
						Variable variable = ((SqlTransaction) this.getObject()).getVariable(parameterName);
						if (variable != null) {
							try {
								MessageBox messageBox = new MessageBox(new Shell(), SWT.ICON_QUESTION
						            | SWT.YES | SWT.NO);
						        messageBox.setMessage("Do you really want to delete the variable \""+variable.getName()+"\"?");
						        messageBox.setText("Delete \""+variable.getName()+"\"?");
						        int response = messageBox.open();
						        if (response == SWT.YES) {
							        variable.delete();		
						        }
							} catch (EngineException e) {
								Engine.logBeans.error("(SqlTransaction) Error when we delete the variable.\n"+e.getMessage());
							}
						}
					}
				}
			}
			oldOrderedParametersList = allParametersList;
		}
		
		return sqlTransaction.preparedSqlQueries;
	}
	
	// We create an list with all parameters to permit to update if needed
	private List<String> createAllParametersList(List<SqlQueryInfos> sqlQueries){
		List<String> allParametersList = new ArrayList<String>();
		
		for (SqlQueryInfos sqlQueryInfos : sqlQueries){
			if( sqlQueryInfos.getOtherParametersList() != null && sqlQueryInfos.getOrderedParametersList() != null) {
				allParametersList.addAll(sqlQueryInfos.getOtherParametersList());
				allParametersList.addAll(sqlQueryInfos.getOrderedParametersList());
			}
		}
		return allParametersList;
	}
	
	private Object getParameterValue(String parameterName, int variableVisibility){
		Object variableValue = null;

		// Scope parameter
		if (sqlTransaction.scope != null) {
			variableValue = sqlTransaction.scope.get(parameterName, sqlTransaction.scope);
			if (variableValue instanceof Undefined)
				variableValue = null;
			if (variableValue instanceof UniqueTag && ((UniqueTag) variableValue).equals(UniqueTag.NOT_FOUND)) 
				variableValue = null;
			if (variableValue != null)
				Engine.logBeans.trace("(SqlTransaction) scope value: "+ Visibility.Logs.printValue(variableVisibility,variableValue));
		}
		
		// Otherwise Transaction parameter (USELESS)
		if (variableValue == null) {
			variableValue = sqlTransaction.variables.get(parameterName);
			if (variableValue != null)
				Engine.logBeans.trace("(SqlTransaction) parameter value: "+ Visibility.Logs.printValue(variableVisibility,variableValue));
		}
		
		// Otherwise context parameter
		if (variableValue == null && sqlTransaction.context != null) {
			variableValue = (sqlTransaction.context.get(parameterName) == null ? null : sqlTransaction.context.get(parameterName));
			if (variableValue != null)
				Engine.logBeans.trace("(SqlTransaction) context value: "+ Visibility.Logs.printValue(variableVisibility,variableValue));
		}
		
		// Otherwise default transaction parameter value
		if (variableValue == null) {
			variableValue = sqlTransaction.getVariableValue(parameterName);
			if (variableValue != null)
				Engine.logBeans.trace("(SqlTransaction) default value: " + Visibility.Logs.printValue(variableVisibility,variableValue));
		}
		
		if (variableValue == null)
			Engine.logBeans.trace("(SqlTransaction) "+parameterName+" none value found");
		
		return variableValue = ((variableValue == null)? new String(""):variableValue);
	}
	
	private boolean checkVariables(List<SqlQueryInfos> sqlQueries) {

		if (sqlQueries != null) {
			for(SqlQueryInfos sqlQuery : sqlQueries){
				Map<String, String> variables  = sqlQuery.getParametersMap();
				if (sqlQuery.orderedParametersList != null && variables != null){
					if (sqlQuery.orderedParametersList.size() != 0 && variables.size() != 0){
						for (String key : sqlQuery.orderedParametersList){
							if( !getParameterValue( key, this.getVariableVisibility(key) ).toString().equals( variables.get(key) ) )
								return false;	
						}
					}
				}
				
				if (sqlQuery.otherParametersList != null && sqlQuery.otherParametersList.size() != 0) {
					for (String key : sqlQuery.otherParametersList){
						if( !getParameterValue( key, this.getVariableVisibility(key) ).toString().equals( variables.get(key) ) )
							return false;	
					}
				}
			}
		} else {
			initializeQueries(true);
			return checkVariables(sqlQueries);
		}
		
		return true;
	}
	
	public int getVariableVisibility(String requestedVariableName) {
		Variable variable = sqlTransaction.getVariable(requestedVariableName);
		if (variable != null)
			return variable.getVisibility();
		return 0;
	}
	
}