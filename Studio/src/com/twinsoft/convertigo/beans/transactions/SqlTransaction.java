/*
 * Copyright (c) 2009-2014 Convertigo. All Rights Reserved.
 *
 * The copyright to the computer  program(s) herein  is the property
 * of Convertigo.
 * The program(s) may  be used  and/or copied  only with the written
 * permission  of  Convertigo  or in accordance  with  the terms and
 * conditions  stipulated  in the agreement/contract under which the
 * program(s) have been supplied.
 *
 * Convertigo makes  no  representations  or  warranties  about  the
 * suitability of the software, either express or implied, including
 * but  not  limited  to  the implied warranties of merchantability,
 * fitness for a particular purpose, or non-infringement. Convertigo
 * shall  not  be  liable for  any damage  suffered by licensee as a
 * result of using,  modifying or  distributing this software or its
 * derivatives.
 */

/*
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.transactions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class SqlTransaction extends TransactionWithVariables {

	private static final long serialVersionUID = 5180639998317573920L;
	
	enum keywords {
		_tagname,
		_level;
		
		static boolean contains(String key) {
			try {
				keywords.valueOf(key);
			} catch (IllegalArgumentException e) {
				return false;
			}
			
			return true;
		}
		
	}	

	/** The SqlConnector for transaction. */
	transient private SqlConnector connector = null;

	/** The PreparedStatement to execute queries */
	transient private PreparedStatement preparedStatement = null;
	
	/** The type of query. */
	transient private SqlKeywords type = SqlKeywords.unknown;
	
	/** List of SqlQuery with type, the query and parameters **/
	transient private List<SqlQueryInfos> preparedSqlQueries = null;

	/** Holds value of property sqlQuery. */
	private String sqlQuery = "";
	
	/** Holds value of property maxResult. */
	private String maxResult = "";

	private enum SqlKeywords {
		select, update, insert, delete, replace, create_table, drop_table, truncate_table, commit, rollback, unknown;
	}	

	private enum AutoCommitMode {
		autocommit_off, autocommit_each, autocommit_end; 
	}
	
	/** Holds value of property Auto-commit. */
	public static final int AUTOCOMMIT_OFF = 0; 	

	public static final int AUTOCOMMIT_EACH = 1;	// jmc 14/05/06

	public static final int AUTOCOMMIT_END = 2;
	
	private int autoCommit = AutoCommitMode.autocommit_each.ordinal(); 
	
	/** Holds value of property xmlOutput. */
	private int xmlOutput = 0;
	private transient XmlMode xmlMode = XmlMode.parse(xmlOutput);
	
	enum XmlMode {
		raw,						// 0
		auto,						// 1
		element,					// 2
		element_with_attributes,	// 3
		flat_element;				// 4
		
		static XmlMode parse(int i) {
			return XmlMode.values()[i];
		}
		
		boolean is(XmlMode mode) {
			return this == mode;
		}
		
		boolean is(XmlMode... modes) {
			for (XmlMode mode: modes) {
				if (mode == this) {
					return true;
				}
			}
			return false;
		}
		
		boolean isElement() {
			return is(element, element_with_attributes, flat_element);
		}
		
		boolean isFlat() {
			return is(raw, flat_element);
		}
		
		boolean useRowTagName() {
			return is(raw, element_with_attributes, flat_element);
		}
		
		boolean useColumnName() {
			return is(element, flat_element);
		}
	}
	
	private String errorMessageSQL = "";

	/** Holds value of property xmlGrouping. */
	private boolean xmlGrouping = false;
	
	private String xmlDefaultRowTagname = "row";
	
	private String xmlDefaultColumnTagname = "column";
	
	/** SqlQueryInfos class **/
	public class SqlQueryInfos {
		
		private String query = "";
		
		private SqlTransaction sqlTransaction;
		
		private SqlKeywords type;
		
		/** Query's ordered parameter names */
		private List<String> orderedParametersList = null;
		
		/** Just use for the parameterized parameter like {{id}} **/
		private List<String> otherParametersList = null;
		
		/** Query's parameters map (name and value) */
		private Map<String, String> parametersMap = new HashMap<String, String>();

		private void findType(){
			if (query.toUpperCase().indexOf("SELECT") == 0)
				type = SqlKeywords.select;
			else if (query.toUpperCase().indexOf("UPDATE") == 0)
				type = SqlKeywords.update;
			else if (query.toUpperCase().indexOf("INSERT") == 0)
				type = SqlKeywords.insert;
			else if (query.toUpperCase().indexOf("DELETE") == 0)
				type = SqlKeywords.delete;
			else if (query.toUpperCase().indexOf("REPLACE") == 0)
				type = SqlKeywords.replace;
			else if (query.toUpperCase().indexOf("CREATE TABLE") == 0)
				type = SqlKeywords.create_table;
			else if (query.toUpperCase().indexOf("DROP TABLE") == 0)
				type = SqlKeywords.drop_table;
			else if (query.toUpperCase().indexOf("TRUNCATE TABLE") == 0)
				type = SqlKeywords.truncate_table;
			else if (query.toUpperCase().indexOf("COMMIT") == 0)
				type = SqlKeywords.commit;
			else if (query.toUpperCase().indexOf("ROLLBACK") == 0)
				type = SqlKeywords.rollback;
			else 
				type = SqlKeywords.unknown;
		}
		
		public SqlQueryInfos(String thequery, SqlTransaction sqlTransaction, boolean updateDefinitions){
			this.query = thequery.replaceAll("\n", " ").replaceAll("\r", "").trim();
			this.sqlTransaction = sqlTransaction;
			findType();
			this.query = prepareParameters(updateDefinitions);
		}
		
		/** We prepare the query and create lists **/
		private String prepareParameters(boolean updateDefinitions){
			String preparedSqlQuery = "";
			
			if ( query != null && (bNew || updateDefinitions)) {
				preparedSqlQuery = query;
								
				// Handled the case if we have value like {{id}} or "{{id}}" or '{{id}}' (i.e: table name or instructions)		
				Pattern pattern = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)\\}\\}");
				Matcher matcher = pattern.matcher(preparedSqlQuery);
				
				// Retrieve parameter names
				orderedParametersList = new ArrayList<String>(); // for parameters like {id}
				otherParametersList = new ArrayList<String>();	 // for parameters like {{id}}
				
				// Clear parameters Map
				parametersMap.clear();
				
				while (matcher.find()) {
					String parameterName = matcher.group(1);
					String parameterValue = getParameterValue(parameterName, 
							sqlTransaction.getVariableVisibility(parameterName)).toString();
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
					String parameterValue = getParameterValue(parameterName, 
							sqlTransaction.getVariableVisibility(parameterName)).toString();
	
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
		
		public SqlKeywords getType(){
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
	
	public SqlTransaction() {
		super();
	}

	@Override
    public SqlTransaction clone() throws CloneNotSupportedException {
    	SqlTransaction clonedObject = (SqlTransaction) super.clone();
    	clonedObject.connector = null;
    	clonedObject.preparedStatement = null;
    	clonedObject.type = type;
        return clonedObject;
    }
	
	@Override
	protected void finalize() throws Throwable {
		if (preparedStatement != null)
			preparedStatement.close();
		
		super.finalize();
	}

	@Override
	public void setStatisticsOfRequestFromCache() {
		// Nothing to do
	}

	@Override
	public String getRequestString(Context context) {
		return super.getRequestString(context);
	}
	
	public List<SqlQueryInfos> initializeQueries(boolean updateDefinitions){
		if (preparedSqlQueries != null ) {
			preparedSqlQueries.clear();
		} else {
			preparedSqlQueries = new ArrayList<SqlQueryInfos>();
		}
		
		// We split the sqlQuery in list array of multiple sqlQuery
		String[] sqlQueries = sqlQuery.split(";");
		
		if ( sqlQueries != null) {
			// We loop every query of the String tab and create SqlQueryInfos element for the preparedSqlQueries list
			for ( String query : sqlQueries ){
				if ( query != null && !query.trim().replaceAll(" ", "").equals("")) {
					SqlQueryInfos sqlQueryInfos = new SqlQueryInfos(query, this, updateDefinitions);
					preparedSqlQueries.add(sqlQueryInfos);
				}
			}
		}
		
		return preparedSqlQueries;
	}
	
	private Object getParameterValue(String parameterName, int variableVisibility){
		Object variableValue = null;

		// Scope parameter
		if (scope != null) {
			variableValue = scope.get(parameterName, scope);
			if (variableValue instanceof Undefined)
				variableValue = null;
			if (variableValue instanceof UniqueTag && ((UniqueTag) variableValue).equals(UniqueTag.NOT_FOUND)) 
				variableValue = null;
			if (variableValue != null)
				Engine.logBeans.trace("(SqlTransaction) scope value: "+ Visibility.Logs.printValue(variableVisibility,variableValue));
		}
		
		// Otherwise Transaction parameter (USELESS)
		if (variableValue == null) {
			variableValue = variables.get(parameterName);
			if (variableValue != null)
				Engine.logBeans.trace("(SqlTransaction) parameter value: "+ Visibility.Logs.printValue(variableVisibility,variableValue));
		}
		
		// Otherwise context parameter
		if (variableValue == null && context != null) {
			variableValue = (context.get(parameterName) == null ? null : context.get(parameterName));
			if (variableValue != null)
				Engine.logBeans.trace("(SqlTransaction) context value: "+ Visibility.Logs.printValue(variableVisibility,variableValue));
		}
		
		// Otherwise default transaction parameter value
		if (variableValue == null) {
			variableValue = getVariableValue(parameterName);
			if (variableValue != null)
				Engine.logBeans.trace("(SqlTransaction) default value: " + Visibility.Logs.printValue(variableVisibility,variableValue));
		}
		
		if (variableValue == null)
			Engine.logBeans.trace("(SqlTransaction) "+parameterName+" none value found");
		
		return variableValue = ((variableValue == null)? new String(""):variableValue);
	}

	private String prepareQuery(List<String> logHiddenValues, SqlQueryInfos sqlQueryInfos) throws 
		SQLException, ClassNotFoundException, EngineException {
		
		checkSubLoaded();

		logHiddenValues.clear();
		
		String preparedSqlQuery = sqlQueryInfos.getQuery();
		
		// Limit number of result
		if (sqlQueryInfos.getType() == SqlKeywords.select) {
			if (!maxResult.equals("") && preparedSqlQuery.toUpperCase().indexOf("LIMIT") == -1) {
				if (preparedSqlQuery.lastIndexOf(';') == -1)
					preparedSqlQuery += " limit " + maxResult + ";";
				else
					preparedSqlQuery = preparedSqlQuery.substring(0, preparedSqlQuery.lastIndexOf(';')) + " limit " + maxResult + ";";
			}
		}
		
		if (Engine.logBeans.isDebugEnabled())
			Engine.logBeans.debug("(SqlTransaction) Preparing query '" + Visibility.Logs.replaceValues(logHiddenValues, preparedSqlQuery) + "'.");
		
		preparedStatement = connector.prepareStatement(preparedSqlQuery, sqlQueryInfos.getParametersMap(), sqlQueryInfos.getOrderedParametersList());
		return preparedSqlQuery;
	}
	
	private void updateVariable(boolean updateDefinitions, String parameterName){
		if (updateDefinitions && (getVariable(parameterName) == null)) {
			try {
				if (!StringUtils.isNormalized(parameterName))
					throw new EngineException("Parameter name is not normalized : \""+parameterName+"\".");
				
				RequestableVariable variable = new RequestableVariable();
				variable.setName(parameterName);
				variable.setDescription(parameterName);
				variable.setWsdl(Boolean.TRUE);
				variable.setCachedKey(Boolean.TRUE);
				addVariable(variable);

				variable.bNew = true;
				variable.hasChanged = true;
				hasChanged = true;
			} catch(EngineException e) {
				Engine.logBeans.error("Could not add variable '"+parameterName+"' for SqlTransaction '"+ getName() +"'", null);
			}
		}
	}
	
	@Override
	public void runCore() throws EngineException {
		Document doc = null;
		Element sql_output = null;
		boolean studioMode = Engine.isStudioMode();
		boolean rollbackDone = false;
		String prefix = getXsdTypePrefix();
		int numberOfResults = 0;
		int nb = 0;
		
		// Create an empty list for hidden variable values
		List<String> logHiddenValues = new ArrayList<String>();
		
		try {
			List<List<String>> lines = null;
			List<List<Map<String,String>>> rows = null;
			List<String> columnHeaders = null;
			
			connector = ((SqlConnector) parent);
			errorMessageSQL = "";
			
			if (!runningThread.bContinue)
				return;
			
			// We check variables and initialize queries if we have a change
			if (!checkVariables(preparedSqlQueries)) {
				preparedSqlQueries = initializeQueries(true);
			}
					
			if (!preparedSqlQueries.get(0).getParametersMap().isEmpty()) {
				preparedSqlQueries.get(0).getParametersMap().get(preparedSqlQueries.get(0).getOrderedParametersList().get(0));
			}

			for (SqlQueryInfos sqlQueryInfos : preparedSqlQueries){

				if (errorMessageSQL.equals("")) {			
					// Prepare the query and retrieve its type
					String query = prepareQuery(logHiddenValues, sqlQueryInfos);
					
					// Build xsdType
					if (studioMode) {
						xsdType = "";
						doc = createDOM(getEncodingCharSet());
						Element element = doc.createElement("xsd:complexType");
						element.setAttribute("name", prefix + getName() +"Response");
						doc.appendChild(element);
						
						Element all = doc.createElement("xsd:all");
						element.appendChild(all);
		
						Element cnv_error = doc.createElement("xsd:element");
						cnv_error.setAttribute("name","error");
						cnv_error.setAttribute("type", "p_ns:ConvertigoError");
						cnv_error.setAttribute("minOccurs","0");
						cnv_error.setAttribute("maxOccurs","1");
						all.appendChild(cnv_error);
						
						sql_output = doc.createElement("xsd:element");
						sql_output.setAttribute("name","sql_output");
						sql_output.setAttribute("minOccurs","0");
						sql_output.setAttribute("maxOccurs","1");
						if ((sqlQueryInfos.getType() != SqlKeywords.select) && (sqlQueryInfos.getType() != SqlKeywords.unknown))
							sql_output.setAttribute("type", "xsd:string");
						all.appendChild(sql_output);
					}
					
					if (!runningThread.bContinue)
						return;
					
					// Execute the SELECT query
					switch(sqlQueryInfos.getType()) {
						case commit:
							try {
								// We set the auto-commit in function of the SqlTransaction parameter
								connector.connection.setAutoCommit(autoCommit == AutoCommitMode.autocommit_each.ordinal());

								connector.connection.commit();
								nb = 0;
				            } catch(SQLException excep) {
				            	if (Engine.logBeans.isTraceEnabled()) {
									// We get the exception error message
									errorMessageSQL = excep.getMessage();
				            	}
				            }
							break;
							
						case rollback:
							try {
								// We set the auto-commit in function of the SqlTransaction parameter
								connector.connection.setAutoCommit(autoCommit == AutoCommitMode.autocommit_each.ordinal());

								connector.connection.rollback();

				                if (Engine.logBeans.isTraceEnabled())
									Engine.logBeans.trace("(SqlTransaction) Explicit rollback");
				            } catch(SQLException excep) {
				            	if (Engine.logBeans.isTraceEnabled()) {
									// We get the exception error message
									errorMessageSQL = excep.getMessage();
				            	}
				            }										
							
							rollbackDone = true;									
							break;
					
						case select:
						case unknown:
							ResultSet rs = null;
							try {
								// We set the auto-commit in function of the SqlTransaction parameter
								connector.connection.setAutoCommit(autoCommit == AutoCommitMode.autocommit_each.ordinal());

								// We execute the query
								preparedStatement.execute();
								rs = preparedStatement.getResultSet();
							}
							// Retry once (should not happens)
							catch(Exception e) {
								if (runningThread.bContinue) {
									if (Engine.logBeans.isTraceEnabled())
										Engine.logBeans.trace("(SqlTransaction) An exception occured :" + e.getMessage());
									if (Engine.logBeans.isDebugEnabled())
										Engine.logBeans.debug("(SqlTransaction) Retry executing query '" + Visibility.Logs.replaceValues(logHiddenValues, query) + "'.");
									query = prepareQuery(logHiddenValues, sqlQueryInfos);
									try {
										// We execute the query
										preparedStatement.execute();
										rs = preparedStatement.getResultSet();
									} catch(Exception e1) {
										// We get the exception error message
										errorMessageSQL = e1.getMessage();

										// We rollback if error and if in auto commit false mode
										if (autoCommit != AutoCommitMode.autocommit_each.ordinal()) {
											try {
								                connector.connection.rollback();

								                if (Engine.logBeans.isTraceEnabled())
													Engine.logBeans.trace("(SqlTransaction) An exception occured : Transactions are being rolled back");
								            } catch(SQLException excep) {
								            	if (Engine.logBeans.isTraceEnabled()) {
													// We get the exception error message
													errorMessageSQL = excep.getMessage();
								            	}
								            }										
											
											rollbackDone = true;									
										}
										
										if (Engine.logBeans.isTraceEnabled())
											Engine.logBeans.trace("(SqlTransaction) An exception occured :" + errorMessageSQL);
									}
								}
							}
							
							if (rs != null) {
								Map<String,List<List<Object>>> tables = new LinkedHashMap<String, List<List<Object>>>();
								columnHeaders = new LinkedList<String>();
								lines = new LinkedList<List<String>>();
								rows = new LinkedList<List<Map<String,String>>>();
									
								// Retrieve column names, class and table names
								String tableNameToUse = "TABLE";
								ResultSetMetaData rsmd = rs.getMetaData();
								
								if (rsmd == null) throw new EngineException("Invalid query '" + query + "'");
								
								int numberOfColumns = rsmd.getColumnCount();
								for (int i=1; i <= numberOfColumns; i++) {
									List<List<Object>> columns = null;
									Integer columnIndex = new Integer(i);
									String columnName = rsmd.getColumnLabel(i);
									String columnClassName = rsmd.getColumnClassName(i);
									String tableName = rsmd.getTableName(i);
									tableNameToUse = (tableName.equals("") ? tableNameToUse:tableName);
									if (tableNameToUse == null) throw new EngineException("Invalid query '" + query + "'");
									
									if (!tables.containsKey(tableNameToUse)) {
										columns = new LinkedList<List<Object>>();
										tables.put(tableNameToUse, columns);
									}
									else
										columns = tables.get(tableNameToUse);
									columnHeaders.add(columnName);
									List<Object> column = new ArrayList<Object>(3);
									column.add(columnIndex);
									column.add(columnName);
									column.add(columnClassName);
									columns.add(column);
									Engine.logBeans.trace("(SqlTransaction) "+ tableNameToUse+"."+columnName + " (" + columnClassName + ")");
								}
							
								// Retrieve results
								while (rs.next()) {
									List<String> line = new ArrayList<String>(Collections.nCopies(numberOfColumns, ""));
									
									List<Map<String,String>> row = new ArrayList<Map<String,String>>(tables.size());
									int j = 0;
									for(Entry<String,List<List<Object>>> entry : tables.entrySet()) {
										String tableName = entry.getKey();
										Map<String,String> elementTable = new LinkedHashMap<String, String>();
										elementTable.put(keywords._tagname.name(), tableName);
										elementTable.put(keywords._level.name(), (++j) + "0");
										for (List<Object> col : entry.getValue()) {
											String columnName = (String) col.get(1);
											int index = (Integer) col.get(0);
											Object ob = null;
											try {
												ob = rs.getObject(index);
											} catch (SQLException e) {
												Engine.logBeans.error("(SqlTransaction) Exception while getting object for column " + index, e);
											}
											String resu = "";
											if (ob != null) {
												if (ob instanceof byte[]) resu = new String((byte[]) ob, "UTF-8"); // See #3043
												else resu = ob.toString();
											}
											Engine.logBeans.trace("(SqlTransaction) Retrieved value ("+resu+") for column " + columnName + ", line " + (j-1) + ".");
											line.set(index-1, resu);
											
											int cpt = 1;
											String t = "";
											while (elementTable.containsKey(columnName + t)) {
												t ="_" + String.valueOf(cpt);											
												cpt++;
											}										
											
											elementTable.put(columnName + t, resu);
										}
										row.add(elementTable);
									}
									Engine.logBeans.trace("(SqlTransaction) "+ line.toString());
									lines.add(line);
									rows.add(row);
									numberOfResults++;
								}
								
								if (studioMode) {
									Element parentElt = sql_output, child = null;
									boolean firstLoop = true;
									for(Entry<String,List<List<Object>>> entry : tables.entrySet()) {
										if (firstLoop) {
											parentElt = (Element)parentElt.appendChild(doc.createElement("xsd:complexType"));
											parentElt = (Element)parentElt.appendChild(doc.createElement("xsd:sequence"));
										} else if (xmlMode.is(XmlMode.auto)) {
											parentElt = (Element)parentElt.appendChild(doc.createElement("xsd:sequence"));
										}
										
										String tableName = entry.getKey();
										child = doc.createElement("xsd:element");
										child.setAttribute("name", xmlMode.useRowTagName() ? getRowTagname() : tableName);
										child.setAttribute("minOccurs","0");
										child.setAttribute("maxOccurs","unbounded");
										if (!xmlMode.isFlat() || firstLoop) {
											parentElt = (Element)parentElt.appendChild(child);
											parentElt = (Element)parentElt.appendChild(doc.createElement("xsd:complexType"));
											
											if (xmlMode.is(XmlMode.element_with_attributes)) {
												child = doc.createElement("xsd:attribute");
												child.setAttribute("name", "name");
												child.setAttribute("fixed", tableName);
												parentElt.appendChild(child);
											}
										}
										
										boolean firstLoop_ = true;
										for (List<Object> col : entry.getValue()) {
											if (firstLoop_ && xmlMode.isElement()) {
												child = doc.createElement("xsd:sequence");
												parentElt = (Element) parentElt.appendChild(child);
											}
											String columnName = (String) col.get(1);
											String columnClassName = (String) col.get(2);
											String type = null;
											
											if (columnClassName.equalsIgnoreCase("java.lang.Integer")) {
												type = "xsd:integer";
											} else if (columnClassName.equalsIgnoreCase("java.lang.Double")) {
												type = "xsd:double";
											} else if (columnClassName.equalsIgnoreCase("java.lang.Long")) {
												type = "xsd:long";
											} else if (columnClassName.equalsIgnoreCase("java.lang.Short")) {
												type = "xsd:short";
											} else if (columnClassName.equalsIgnoreCase("java.sql.Timestamp")) {
												type = "xsd:dateTime";
											} else {
												type = "xsd:string";
											}
											
											if (xmlMode.useColumnName()) {
												child = doc.createElement("xsd:element");
												child.setAttribute("name",columnName);
												child.setAttribute("type",type);
												parentElt.appendChild(child);
											} else if (xmlMode.is(XmlMode.element_with_attributes)) {
												Element xsdElement = doc.createElement("xsd:element");
												xsdElement.setAttribute("name",getColumnTagname());
												parentElt.appendChild(xsdElement);
												
												Element xsdComplexType = doc.createElement("xsd:complexType");
												xsdElement.appendChild(xsdComplexType);
												
												Element xsdSimpleContent = doc.createElement("xsd:simpleContent");
												xsdComplexType.appendChild(xsdSimpleContent);
												
												Element xsdExtension = doc.createElement("xsd:extension");
												xsdExtension.setAttribute("base", type);
												xsdSimpleContent.appendChild(xsdExtension);
												
												Element xsdAttribute = doc.createElement("xsd:attribute");
												xsdAttribute.setAttribute("name", "name");
												xsdAttribute.setAttribute("fixed", columnName);
												xsdAttribute.setAttribute("type", "xsd:string");
												xsdExtension.appendChild(xsdAttribute);
											} else {
												child = doc.createElement("xsd:attribute");
												child.setAttribute("name",columnName);
												child.setAttribute("type",type);
												parentElt.appendChild(child);
											}
											firstLoop_ = false;
										}
										firstLoop = false;
									}
								}
							}
									
							if ((rs == null) && (sqlQueryInfos.getType() == SqlKeywords.select)) {
								Engine.logBeans.warn("(SqlTransaction) Could not execute query '" + Visibility.Logs.replaceValues(logHiddenValues, query) + "'"); 
								// We put the number of results at -1, because we have an error
								numberOfResults = -1;
							} else {
								Engine.logBeans.debug("(SqlTransaction) Query executed successfully (" + numberOfResults + " results)"); 
							}
							break;
							
						case replace:
						case create_table:
						case drop_table:
						case truncate_table:
						case update:
						case insert:
						case delete:
							try {
								// We set the auto-commit in function of the SqlTransaction parameter
								connector.connection.setAutoCommit(autoCommit == AutoCommitMode.autocommit_each.ordinal());
								
								// We execute the query
								nb = preparedStatement.executeUpdate();
							}
							// Retry once (should not happens)
							catch (Exception e) {
								if (runningThread.bContinue) {
									if (Engine.logBeans.isTraceEnabled())
										Engine.logBeans.trace("(SqlTransaction) An exception occured :" + e.getMessage());
									if (Engine.logBeans.isDebugEnabled())
										Engine.logBeans.debug("(SqlTransaction) Retry executing query '" + Visibility.Logs.replaceValues(logHiddenValues, query) + "'.");
									query = prepareQuery(logHiddenValues, sqlQueryInfos);
									try {
										nb = preparedStatement.executeUpdate();
									} catch (Exception e1) {									
										// We get the exception error message
										errorMessageSQL = e1.getMessage();
										nb = -1;
										
										// We rollback if error and if in auto commit false mode
										if (autoCommit != AutoCommitMode.autocommit_each.ordinal()) {
											try {
								                connector.connection.rollback();

								                if (Engine.logBeans.isTraceEnabled())
													Engine.logBeans.trace("(SqlTransaction) An exception occured : Transactions are being rolled back");
								            } catch(SQLException excep) {
								            	if (Engine.logBeans.isTraceEnabled()) {
													// We get the exception error message
													errorMessageSQL = excep.getMessage();
								            	}
								            }										
											
											rollbackDone = true;									
										}

										if (Engine.logBeans.isTraceEnabled())
											Engine.logBeans.trace("(SqlTransaction) An exception occured :" + errorMessageSQL);
									}
								}
							}
							
							if (nb < 0)
								Engine.logBeans.warn("(SqlTransaction) Could not execute query '" + Visibility.Logs.replaceValues(logHiddenValues, query) + "'.");
							else
								Engine.logBeans.debug("(SqlTransaction) Query executed successfully (returned " + nb + ").");
							
							break;
							
						default:
							break;
					}
				}
				
				// Close statement and resulset if exist
				if ( preparedStatement != null )
					preparedStatement.close();
					
				if (!runningThread.bContinue)
					return;
	
				// Show results in connector view
				connector.setData(lines, columnHeaders);
					
				// Build XML response
				Element sql;
				if ((rows != null) && errorMessageSQL.equals("")){
					if (xmlMode.isFlat()) {
						sql = parseResultsFlat(lines, columnHeaders);
					} else {
						sql = parseResultsHierarchical(rows, columnHeaders);
					}					
					
				//In case of error during the execution of the request we pull up the error node
				} else if (!errorMessageSQL.equals("")) {
					sql = parseResults(-1);
				} else {
					sql = parseResults(nb);
				}
				
				if (sql != null) {
	    			Element outputDocumentRootElement = context.outputDocument.getDocumentElement();
					outputDocumentRootElement.appendChild(sql);
					score +=1;
				}
				
				if (!errorMessageSQL.equals(""))
					break;
			}
			
			// We commit if auto-commit parameter is false			
			if (!rollbackDone && (autoCommit == AutoCommitMode.autocommit_end.ordinal())) {
				try {
					connector.connection.commit();
	            } catch(SQLException excep) {
	            	if (Engine.logBeans.isTraceEnabled()) {
						// We get the exception error message
						errorMessageSQL = excep.getMessage();
	            	}
	            }										
			}
		}
		catch (Exception e) {
			connector.setData(null,null);
			throw new EngineException("An unexpected error occured while executing transaction. Could not execute the SQL query.",e);
		}
		finally {
			try {
				if (preparedStatement != null)
					preparedStatement.close();
			}
			catch(SQLException e) {;}
			preparedStatement = null;
		}
	
		if (studioMode) {
			if (doc != null) {
				String prettyPrintedText = XMLUtils.prettyPrintDOM(doc);
				prettyPrintedText = prettyPrintedText.substring(prettyPrintedText.indexOf("<xsd:"));
				xsdType = prettyPrintedText;
			}
		}
	}
	
	private boolean checkVariables(List<SqlQueryInfos> sqlQueries) {

		if (sqlQueries != null) {
			for(SqlQueryInfos sqlQuery : sqlQueries){
				Map<String, String> variables  = sqlQuery.getParametersMap();
				if (sqlQuery.orderedParametersList != null && variables != null){
					if (sqlQuery.orderedParametersList.size() != 0 && variables.size() != 0){
						for (String key : sqlQuery.orderedParametersList){
							String valueKey = getParameterValue( key, this.getVariableVisibility(key) ).toString();
							String valueVar = variables.get(key);
							if( !valueKey.equals(valueVar))
								return false;	
						}
					}
				}
				
				if (sqlQuery.otherParametersList != null && sqlQuery.otherParametersList.size() != 0) {
					for (String key : sqlQuery.otherParametersList){
						String valueKey = getParameterValue( key, this.getVariableVisibility(key) ).toString();
						String valueVar = variables.get(key);
						if( !valueKey.equals(valueVar))
							return false;	
					}
				}
			}
		} else {
			initializeQueries(true);
			return checkVariables(preparedSqlQueries);
		}
		
		return true;
	}
	
	public List<SqlQueryInfos> getPreparedSqlQueries(){
		return preparedSqlQueries;
	}
	

	@Override
    protected void cleanup() {
    	if (connector == null)
    		connector = ((SqlConnector) parent);
    	connector.close();
    }

    private transient String xsdType = null;

    @Override
    public String generateWsdlType(Document document) throws Exception {
    	if (xsdType == null) {
    		return super.generateWsdlType(document);
    	}
    	return xsdType;
    }
    
	private Element parseResults(int num) {
		Document doc = createDOM(getEncodingCharSet());
		Node document = doc.appendChild(doc.createElement("document"));
		Node sqlOutput  = document.appendChild(doc.createElement("sql_output"));
		if( num >= 0) {
			sqlOutput.appendChild(doc.createElement("result").appendChild(doc.createTextNode(num+" row(s) updated")));
		// We add the SQL error message into the XML output
		} else {
			sqlOutput = document.appendChild(doc.createElement("error"));
			sqlOutput.appendChild(doc.createElement("result").appendChild(doc.createTextNode(errorMessageSQL)));
		}
		doc.getDocumentElement().appendChild(sqlOutput);
			
		Document output = context.outputDocument;
		Element elt = (Element)output.importNode(sqlOutput,true);
		return elt;
	}
	
	private Element parseResultsFlat(List<List<String>> lines, List<String> columnHeaders) {
		Document doc = createDOM(getEncodingCharSet());
		Element document = (Element) doc.appendChild(doc.createElement("document"));
		Element sqlOutput = (Element) document.appendChild(doc.createElement("sql_output"));
		Element element = null;
		
		for (List<String> line : lines) {
			element = doc.createElement(getRowTagname());

			Iterator<String> lineIterator = line.iterator();
			for (String columnName : columnHeaders) {
				String normalizedColumnName = StringUtils.normalize(columnName);
				String value = lineIterator.next();

				if (xmlMode.is(XmlMode.raw)) {
					int x = 1;
					String t = "";
					while (!element.getAttribute(normalizedColumnName + t).equals("")) {
						t ="_" + String.valueOf(x);											
						x++;
					}

					element.setAttribute(normalizedColumnName + t,value);
				} else if (xmlMode.is(XmlMode.flat_element)) {
					Node node = doc.createElement(normalizedColumnName);
					node.setTextContent(value);
					element.appendChild(node);
				}
			}

			sqlOutput.appendChild(element);
		}
		
		doc.getDocumentElement().appendChild(sqlOutput);
		
		Document output = context.outputDocument;
		Element elt = (Element) output.importNode(sqlOutput, true);
		return elt;
	}
	
	private Element parseResultsHierarchical(List<List<Map<String,String>>> rows, List<String> columnHeaders) {
		Document doc = createDOM(getEncodingCharSet());
		Node document = doc.appendChild(doc.createElement("document"));
		Node sqlOutput = document.appendChild(doc.createElement("sql_output"));
		Map<Object, Element> elements = new HashMap<Object, Element>();
		Element element = null;
		int i = -1;
		
		for(List<Map<String,String>> row : rows) {
			Element parent = (Element)sqlOutput;
			//int parentLevel = 0;
			++i;
			
			for(Map<String,String> rowElt : row) {
				String tag = "row" + i;
				//int level = Integer.parseInt((String)rowElt.get(keywords._level.name()),10);
				Engine.logBeans.trace("(SqlTransaction) row" + i + " " + rowElt.toString());
				
				boolean exist = xmlMode.isFlat() ? elements.containsKey(tag) : elements.containsKey(rowElt);
				
				if (exist) {
					element = (Element)elements.get(rowElt);
					if (xmlGrouping) {
						if (!parent.equals(sqlOutput)) {
							Node topOfElement = element;
							while (!topOfElement.getParentNode().equals(sqlOutput))
								topOfElement = topOfElement.getParentNode();
							Node topOfParent = parent;
							while (!topOfParent.getParentNode().equals(sqlOutput))
								topOfParent = topOfParent.getParentNode();
					
							if (!topOfParent.equals(topOfElement))
								exist = false;
						}
					} else exist = false;
				}
				if (!exist) {
					String tagName;
					
					if (xmlMode.is(XmlMode.element_with_attributes)) {
						tagName = getRowTagname();
					}
					else {
						tagName = StringUtils.normalize(rowElt.get(keywords._tagname.name()));
					}
					
					element = doc.createElement(tagName);

					if (xmlMode.is(XmlMode.element_with_attributes)) {
						element.setAttribute("name", rowElt.get(keywords._tagname.name()));
					}
					
					for (String columnName : rowElt.keySet()) {
						if (!keywords.contains(columnName)) {
							
							String value = rowElt.get(columnName);
							if (xmlMode.is(XmlMode.auto)) {
								element.setAttribute(StringUtils.normalize(columnName),value);
							}
							else if (xmlMode.is(XmlMode.element)) {
								Node node = doc.createElement(StringUtils.normalize(columnName));
								node.appendChild(doc.createTextNode(value));
								element.appendChild(node);
							}
							else if (xmlMode.is(XmlMode.element_with_attributes)) {
								Element node = doc.createElement(getColumnTagname());
								node.setAttribute("name", columnName);
								node.appendChild(doc.createTextNode(value));
								element.appendChild(node);
							}
						}
					}
					
					if (xmlMode.is(XmlMode.element, XmlMode.element_with_attributes, XmlMode.auto)) {
						elements.put(rowElt, element);
					}
					
					parent.appendChild(element);			
					Engine.logBeans.trace("(SqlTransaction) parent.appendChild(" + element.toString() + ")");
					
				}
				
				//parentLevel = level;
				parent = element;
			}
		}
	
		doc.getDocumentElement().appendChild(sqlOutput);
			
		Document output = context.outputDocument;
		Element elt = (Element)output.importNode(sqlOutput,true);
		return elt;
	}

	public Document createDOM(String encodingCharSet) {
		Engine.logBeans.debug("(SqlTransaction) XalanServlet: creating DOM");

		Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
        
		Engine.logBeans.debug("(SqlTransaction) XML class: " + document.getClass().getName());

		ProcessingInstruction pi = document.createProcessingInstruction("xml", "version=\"1.0\" encoding=\"" + encodingCharSet + "\"");
		document.appendChild(pi);

		return document;
	}

	/**
	 * Getter for property sqlQuery
	 * @return String
	 */
	public String getSqlQuery() {
		return sqlQuery;
	}

	/**
	 * Setter for property sqlQuery
	 * @param String
	 */
	public void setSqlQuery(String string) {
		sqlQuery = string;
	}

	/**
	 * Getter for property xmlOutput
	 * @return int
	 */
	public int getXmlOutput() {
		return xmlOutput;
	}
	
	/**
	 * Setter for property xmlOutput
	 * @param String
	 */
	public void setXmlOutput(int type) {
		xmlOutput = type;
		xmlMode = XmlMode.parse(xmlOutput);
	}

	/**
	 * Getter for property xmlGrouping
	 * @return boolean
	 */
	public boolean getXmlGrouping() {
		return xmlGrouping;
	}
	
	/**
	 * Setter for property xmlGrouping
	 * @param boolean
	 */
	public void setXmlGrouping(boolean group) {
		xmlGrouping = group;
	}
	/**
	 * Getter for MaxResult
	 * @return String
	 */
	public String getMaxResult() {
		return maxResult;
	}
	/**
	 * Setter for MaxResult
	 * @param String
	 */
	public void setMaxResult(String maxResult) {
		this.maxResult = maxResult;
	}

	public int getAutoCommit() {
		return autoCommit;
	}

	public void setAutoCommit(int autoCommit) {
		this.autoCommit = autoCommit;
	}

	public String getXmlDefaultRowTagname() {
		return xmlDefaultRowTagname;
	}

	public void setXmlDefaultRowTagname(String xmlDefaultRowTagname) {
		this.xmlDefaultRowTagname = xmlDefaultRowTagname;
	}

	public String getXmlDefaultColumnTagname() {
		return xmlDefaultColumnTagname;
	}

	public void setXmlDefaultColumnTagname(String xmlDefaultColumnTagname) {
		this.xmlDefaultColumnTagname = xmlDefaultColumnTagname;
	}
	
	private String getRowTagname() {
		String tagname = getXmlDefaultRowTagname();
		tagname = tagname.equals("") ? "row":tagname;
		return StringUtils.normalize(tagname);
	}

	private String getColumnTagname() {
		String tagname = getXmlDefaultColumnTagname();
		tagname = tagname.equals("") ? "column":tagname;
		return StringUtils.normalize(tagname);
	}
}

