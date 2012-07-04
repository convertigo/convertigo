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

package com.twinsoft.convertigo.beans.transactions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
import com.twinsoft.util.StringEx;

public class SqlTransaction extends TransactionWithVariables {

	private static final long serialVersionUID = 5180639998317573920L;

	/** The SqlConnector for transaction. */
	transient private SqlConnector connector = null;

	/** The PreparedStatement to execute queries */
	transient private PreparedStatement preparedStatement = null;
	
	/** The vectors of parameterNames for the query. */
	transient private Set<String> vParameters = null;
	transient private Set<String> vOldParameters = null;
	
	transient private Map<String, Boolean> paramsNeedEscape = null;
	
	/** The type of query. */
	transient private int type = -1;
	
	/** Holds value of property sqlQuery. */
	private String sqlQuery = "";
	
	/** Holds value of property maxResult. */
	private String maxResult = "";
	
	public static int TYPE_SELECT = 0;

	public static int TYPE_UPDATE = 1;
	
	public static int TYPE_INSERT = 2;
	
	public static int TYPE_DELETE = 3;
	
	public static int TYPE_REPLACE = 4;
	
	public static int TYPE_CREATE_TABLE = 5;
	
	public static int TYPE_DROP_TABLE = 6;
	
	public static int TYPE_UNKNOWN = 99;
	
	/** Holds value of property xmlOutput. */
	private int xmlOutput = 0; 
	
	public static int XML_RAW = 0;
	
	public static int XML_AUTO = 1;

	public static int XML_ELEMENT = 2;
	
	public static int XML_ELEMENT_WITH_ATTRIBUTES = 3;

	/** Holds value of property xmlGrouping. */
	private boolean xmlGrouping = true;


	public SqlTransaction() {
		super();
		vPropertiesForAdmin.add("maxResult");
	}

	@Override
    public SqlTransaction clone() throws CloneNotSupportedException {
    	SqlTransaction clonedObject = (SqlTransaction) super.clone();
    	clonedObject.connector = null;
    	clonedObject.preparedStatement = null;
    	clonedObject.vParameters = null;
    	clonedObject.vOldParameters = null;
    	clonedObject.paramsNeedEscape = null;
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
		if (context != null) {
			String requestString = context.projectName + " " + context.transactionName;
			Document doc = context.inputDocument;
			if (doc != null) {
				NodeList list = doc.getElementsByTagName("variable");
				String[] vVariables = new String[list.getLength()]; 
				for (int i=0; i<list.getLength(); i++) {
					Element elt = (Element) list.item(i);
					String name = elt.getAttribute("name");
					String value = elt.getAttribute("value");
					vVariables[i]=name + "=" + value;
				}
				Arrays.sort(vVariables);		
				requestString += " " + Arrays.toString(vVariables);
			}
			return requestString;
		}
		return null;
	}

	private void initializeQuery(boolean updateDefinitions) {
		if ((sqlQuery != null) && (isSubLoaded || (bNew && updateDefinitions))) {
			int start= 0, bIndex=-1, eIndex=-1;
			boolean needEscape = true;
			
			// retrieve query type
			if (sqlQuery.toUpperCase().indexOf("SELECT") == 0)
				type = SqlTransaction.TYPE_SELECT;
			else if (sqlQuery.toUpperCase().indexOf("UPDATE") == 0)
				type = SqlTransaction.TYPE_UPDATE;
			else if (sqlQuery.toUpperCase().indexOf("INSERT") == 0)
				type = SqlTransaction.TYPE_INSERT;
			else if (sqlQuery.toUpperCase().indexOf("DELETE") == 0)
				type = SqlTransaction.TYPE_DELETE;
			else if (sqlQuery.toUpperCase().indexOf("REPLACE") == 0)
				type = SqlTransaction.TYPE_REPLACE;
			else if (sqlQuery.toUpperCase().indexOf("CREATE TABLE") == 0)
				type = SqlTransaction.TYPE_CREATE_TABLE;
			else if (sqlQuery.toUpperCase().indexOf("DROP TABLE") == 0)
				type = SqlTransaction.TYPE_DROP_TABLE;
			else type = TYPE_UNKNOWN;
			
			// retrieve parameter names
			vParameters = new HashSet<String>();
			paramsNeedEscape = new HashMap<String, Boolean>();
			while ((bIndex = sqlQuery.indexOf("{",start)) != -1) {
				if ((eIndex = sqlQuery.indexOf("}",bIndex)) != -1) {
					// retrieve parameter name
					String parameterName = sqlQuery.substring(bIndex+1,eIndex);
					// decide whether the parameter content needs to be escaped or not
					needEscape = (sqlQuery.charAt(bIndex-1) == '\'' && sqlQuery.charAt(eIndex+1) == '\'');
					// if parameter name has not been treated already and add parameter name to vParameters vector
					if (vParameters.add(parameterName)) // add parameter content's need to be escaped to hashtable
						paramsNeedEscape.put(parameterName, needEscape);
			
					// add a new variable with name equal to parameterName
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
							Engine.logBeans.error("Could not add variable '"+parameterName+"' for SqlTransaction '"+ name +"'", null);
						}
					}
					start = eIndex;
				} else	break;
			}
			
			// modify variables definition if needed
			if (!vParameters.equals(vOldParameters)) {
				if (updateDefinitions && (vOldParameters != null))
					for (String parameterName : vOldParameters)
						if (!vParameters.contains(parameterName)) {
							RequestableVariable variable = (RequestableVariable)getVariable(parameterName);
							if (variable != null) {
								try {
									Engine.theApp.databaseObjectsManager.delete(variable);
								} catch (EngineException e) {}
								removeVariable(variable);
								hasChanged = true;
							}
						}
				vOldParameters = vParameters;
			}
		}
	}
	
	private String getParametrizedQuery(List<String> logHiddenValues) {
		if (sqlQuery.indexOf("{") == -1)
			return sqlQuery;
		
		if (vParameters == null)
			initializeQuery(false);
		
		if (vParameters != null) {
			StringEx s = new StringEx(sqlQuery);
			for (String parameterName : vParameters) {
				try {
					Object variableValue = null;
					
					// Retrieve variable's visibility if exists
					int variableVisibility = getVariableVisibility(parameterName);

					// Scope parameter
					if (scope != null) {
						variableValue = scope.get(parameterName,scope);
						if (variableValue instanceof Undefined)
							variableValue = null;
						if (variableValue instanceof UniqueTag && ((UniqueTag) variableValue).equals(UniqueTag.NOT_FOUND)) 
							variableValue = null;
						if (variableValue != null)
							Engine.logBeans.trace("(SqlTransaction) scope value: " + Visibility.Logs.printValue(variableVisibility,variableValue));
						
					}
					// Otherwise Transaction parameter (USELESS)
					if (variableValue == null) {
						variableValue = variables.get(parameterName);
						if (variableValue != null)
							Engine.logBeans.trace("(SqlTransaction) parameter value: " + Visibility.Logs.printValue(variableVisibility,variableValue));
					}
					// Otherwise context parameter
					if (variableValue == null) {
						variableValue = (context.get(parameterName) == null ? null : context.get(parameterName));
						if (variableValue != null)
							Engine.logBeans.trace("(SqlTransaction) context value: " + Visibility.Logs.printValue(variableVisibility,variableValue));
					}
					// Otherwise default transaction parameter value
					if (variableValue == null) {
						variableValue = getVariableValue(parameterName);
						if (variableValue != null)
							Engine.logBeans.trace("(SqlTransaction) default value: " + Visibility.Logs.printValue(variableVisibility,variableValue));
					}
					
					if (variableValue == null)
						Engine.logBeans.trace("(SqlTransaction) none value found");
					
					variableValue = ((variableValue == null)? new String(""):variableValue);
					
					String parameterValue = variableValue.toString();
					
					if (paramsNeedEscape.get(parameterName)){
						// handle escape of ' in values
						if (parameterValue.indexOf("'") != -1) {
							StringEx sVal = new StringEx(parameterValue);
							sVal.replaceAll("'", "''");
							parameterValue = sVal.toString();
						}
					}
					
					if (variableValue != null && Visibility.Logs.isMasked(variableVisibility)) {
						if (!variableValue.equals("")) logHiddenValues.add(parameterValue);
					}
					
					s.replaceAll("{"+parameterName+"}",parameterValue);
				} catch(ClassCastException e) {
					Engine.logBeans.warn("(SqlTransaction) Ignoring parameter '" + parameterName+ "' because its value is not a string.");
				}
			}
			return s.toString();
		}
		return null;
	}

	private String prepareQuery(List<String> logHiddenValues) throws SQLException, ClassNotFoundException, EngineException {
		
		checkSubLoaded();

		logHiddenValues.clear();
		
		// prepare the query
		String query = getParametrizedQuery(logHiddenValues);
		
		// type not retrieved
		if (type == -1) {
			initializeQuery(false);
		}
		
		// limit number of result
		if (type == SqlTransaction.TYPE_SELECT) {
			if (!maxResult.equals("") && query.indexOf("limit") == -1 && query.indexOf("LIMIT") == -1 && query.indexOf("Limit") == -1) {
				if (query.lastIndexOf(';') == -1)
					query += " limit " + maxResult + ";";
				else
					query = query.substring(0, query.lastIndexOf(';')) + " limit " + maxResult + ";";
			}
		}
		
		preparedStatement = connector.prepareStatement(query);
		return query;
	}
	
	@Override
	public void runCore() throws EngineException {
		Document doc = null;
		Element sql_output = null;
		String query = null, prettyPrintedText = "";
		boolean studioMode = Engine.isStudioMode();
		String prefix = getXsdTypePrefix();
		int numberOfResults = 0;
		int nb = 0;
		
		// create an empty list for hidden variable values
		List<String> logHiddenValues = new ArrayList<String>();
		
		try {
			List<List<String>> lines = null;
			List<List<Map<String,String>>> rows = null;
			List<String> columnHeaders = null;
			
			connector = ((SqlConnector) parent);
			
			if (!runningThread.bContinue)
				return;
			
			// prepare the query and retrieve its type
			query = prepareQuery(logHiddenValues);
			if (Engine.logBeans.isDebugEnabled())
				Engine.logBeans.debug("(SqlTransaction) Executing query '" + Visibility.Logs.replaceValues(logHiddenValues, query) + "'.");
			
			// build xsdType
			if (studioMode) {
				xsdType = "";
				doc = createDOM(getEncodingCharSet());
				Element element = doc.createElement("xsd:complexType");
				element.setAttribute("name", prefix + name +"Response");
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
				if ((type != SqlTransaction.TYPE_SELECT) && (type != SqlTransaction.TYPE_UNKNOWN))
					sql_output.setAttribute("type", "xsd:string");
				all.appendChild(sql_output);
			}
			
			if (!runningThread.bContinue)
				return;
	
			// execute the SELECT query
			if (type == SqlTransaction.TYPE_SELECT || type == SqlTransaction.TYPE_UNKNOWN) {
				ResultSet rs = null;
				try {
					rs = preparedStatement.executeQuery();
				}
				// Retry once (should not happens)
				catch(Exception e) {
					if (runningThread.bContinue) {
						if (Engine.logBeans.isTraceEnabled())
							Engine.logBeans.trace("(SqlTransaction) An exception occured :" + e.getMessage());
						if (Engine.logBeans.isDebugEnabled())
							Engine.logBeans.debug("(SqlTransaction) Retry executing query '" + Visibility.Logs.replaceValues(logHiddenValues, query) + "'.");
						query = prepareQuery(logHiddenValues);
						rs = preparedStatement.executeQuery();
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
							Map<String,String> elementTable = new HashMap<String, String>();
							elementTable.put("_tagname",tableName);
							elementTable.put("_level",(++j)+"0");
							for (List<Object> col : entry.getValue()) {
								String columnName = (String)col.get(1);
								int index = (Integer)col.get(0);
								Object ob = null;
								try {
									ob = rs.getObject(index);
								} catch (SQLException e) {
									Engine.logBeans.error("(SqlTransaction) Exception while getting object for column " + index, e);
								}
								String resu = ((ob != null) ? ob.toString():"");
								Engine.logBeans.trace("(SqlTransaction) Retrieved value ("+resu+") for column " + columnName + ", line " + (j-1) + ".");
								line.set(index-1, resu);
								elementTable.put(columnName,resu);
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
							} else if (xmlOutput == XML_AUTO)
								parentElt = (Element)parentElt.appendChild(doc.createElement("xsd:sequence"));
							
							String tableName = entry.getKey();
							child = doc.createElement("xsd:element");
							child.setAttribute("name",((xmlOutput == XML_RAW) ? "row":tableName));
							child.setAttribute("minOccurs","0");
							child.setAttribute("maxOccurs","unbounded");
							if ((firstLoop && (xmlOutput == XML_RAW)) || (xmlOutput != XML_RAW)) {
								parentElt = (Element)parentElt.appendChild(child);
								parentElt = (Element)parentElt.appendChild(doc.createElement("xsd:complexType"));
							}
							
							boolean firstLoop_ = true;
							for (List<Object> col : entry.getValue()) {
								if (firstLoop_ && ((xmlOutput == XML_ELEMENT) || (xmlOutput == XML_ELEMENT_WITH_ATTRIBUTES))) {
									child = doc.createElement("xsd:sequence");
									parentElt = (Element)parentElt.appendChild(child);
								}
								String columnName = (String)col.get(1);
								String columnClassName = (String)col.get(2);
								String type = null;
								
								if (columnClassName.equalsIgnoreCase("java.lang.Integer"))
									type = "xsd:integer";
								else if (columnClassName.equalsIgnoreCase("java.lang.Double"))
									type = "xsd:double";
								else if (columnClassName.equalsIgnoreCase("java.lang.Long"))
									type = "xsd:long";
								else if (columnClassName.equalsIgnoreCase("java.lang.Short"))
									type = "xsd:short";
								else if (columnClassName.equalsIgnoreCase("java.sql.Timestamp"))
									type = "xsd:dateTime";
								else type = "xsd:string";
								
								if (xmlOutput == XML_ELEMENT) {
									child = doc.createElement("xsd:element");
									child.setAttribute("name",columnName);
									child.setAttribute("type",type);
									parentElt.appendChild(child);
								} else if (xmlOutput == XML_ELEMENT_WITH_ATTRIBUTES) {
									child = doc.createElement("xsd:element");
									child.setAttribute("name","column");
									child.setAttribute("type",type);
									parentElt.appendChild(child);
									Element child2 = doc.createElement("xsd:attribute");
									child2.setAttribute("name",columnName);
									child.appendChild(child2);
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
						
				if (rs == null)
					Engine.logBeans.warn("(SqlTransaction) Could not execute query '" + Visibility.Logs.replaceValues(logHiddenValues, query) + "'");
				else
					Engine.logBeans.debug("(SqlTransaction) Query executed successfully (" + numberOfResults + " results)");
			}
			// execute the 'UPDATE/INSERT/DELETE' query
			else {
				try {
					nb = preparedStatement.executeUpdate();
				}
				// Retry once (should not happens)
				catch (Exception e) {
					if (runningThread.bContinue) {
						if (Engine.logBeans.isTraceEnabled())
							Engine.logBeans.trace("(SqlTransaction) An exception occured :" + e.getMessage());
						if (Engine.logBeans.isDebugEnabled())
							Engine.logBeans.debug("(SqlTransaction) Retry executing query '" + Visibility.Logs.replaceValues(logHiddenValues, query) + "'.");
						query = prepareQuery(logHiddenValues);
						nb = preparedStatement.executeUpdate();
					}
				}
				
				if (nb < 0)
					Engine.logBeans.warn("(SqlTransaction) Could not execute query '" + Visibility.Logs.replaceValues(logHiddenValues, query) + "'.");
				else
					Engine.logBeans.debug("(SqlTransaction) Query executed successfully (returned " + nb + ").");
			}
				
			// close statement and resulset if exist
			preparedStatement.close();
				
			if (!runningThread.bContinue)
				return;

			// show results in connector view
			connector.setData(lines, columnHeaders);
				
			// build XML response
			Element sql;
			if (rows != null)
				sql = parseResults(rows, columnHeaders);
			else
				sql = parseResults(nb);
			
			if (sql != null) {
    			Element outputDocumentRootElement = context.outputDocument.getDocumentElement();
				outputDocumentRootElement.appendChild(sql);
				score +=1;
			}
		}
		catch (Exception e) {
			connector.setData(null,null);
			String logQuery = (String) Visibility.Logs.replaceValues(logHiddenValues, query==null ? sqlQuery:query);
			throw new EngineException("An unexpected error occured while executing transaction.\nCould not execute query '" + logQuery + "'.",e);
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
				prettyPrintedText = XMLUtils.prettyPrintDOM(doc);
				prettyPrintedText = prettyPrintedText.substring(prettyPrintedText.indexOf("<xsd:"));
				xsdType = prettyPrintedText;
			}
		}
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
    
/*
	public String generateWsdlType(Document document) throws Exception {
		ResultSet rs = null;
		String prettyPrintedText = "";
		Document doc = createDOM("ISO-8859-1");
		Element element = null , all = null, sql_output = null, cnv_error = null;

		connector = ((SqlConnector) parent);
		
		element = doc.createElement("xsd:complexType");
		if (!connector.isDefault)
			element.setAttribute("name",connector.getName() + "__" + name +"Response");
		else
			element.setAttribute("name",name +"Response");
		
		doc.appendChild(element);
		
		all = doc.createElement("xsd:all");
		element.appendChild(all);

		cnv_error = doc.createElement("xsd:element");
		cnv_error.setAttribute("name","error");
		cnv_error.setAttribute("type", "tns:ConvertigoError");
		cnv_error.setAttribute("minOccurs","0");
		cnv_error.setAttribute("maxOccurs","1");
		all.appendChild(cnv_error);
		
		sql_output = doc.createElement("xsd:element");
		sql_output.setAttribute("name","sql_output");
		sql_output.setAttribute("minOccurs","0");
		sql_output.setAttribute("maxOccurs","1");
		all.appendChild(sql_output);
		
		try {
				
			// prepare the query
			String query = prepareQuery();
	
			// execute the SELECT query
			if (type == SqlTransaction.TYPE_SELECT) {
				Hashtable tables = new Hashtable();
				Vector tableOrder = new Vector();
				rs = preparedStatement.executeQuery();
				if (rs != null) {
					// Retrieve column names, class and table names
					String tableNameToUse = "TABLE";
					ResultSetMetaData rsmd = rs.getMetaData();
					int numberOfColumns = rsmd.getColumnCount();
					for (int i=1; i <= numberOfColumns; i++) {
						Vector columns = null;
						Integer columnIndex = new Integer(i);
						String columnName = rsmd.getColumnName(i);
						String columnClassName = rsmd.getColumnClassName(i);
						String tableName = rsmd.getTableName(i);
						tableNameToUse = (tableName.equals("") ? tableNameToUse:tableName);
						if (tableNameToUse == null) throw new EngineException("Invalid query '" + query + "'");
						
						if (!tables.containsKey(tableNameToUse)) {
							columns = new Vector();
							tables.put(tableNameToUse,columns);
							tableOrder.addElement(tableNameToUse);
						}
						else
							columns = (Vector)tables.get(tableNameToUse);
						Vector column = new Vector();
						column.addElement(columnIndex);
						column.addElement(columnName);
						column.addElement(columnClassName);
						columns.addElement(column);
						//Engine.logBeans.trace(tableName+"."+columnName + " (" + columnClassName + ")");
					}
					
					Element parentElt = sql_output, child = null;
					for (int j=0; j<tableOrder.size(); j++) {
						if (j==0) {
							parentElt = (Element)parentElt.appendChild(doc.createElement("xsd:complexType"));
							parentElt = (Element)parentElt.appendChild(doc.createElement("xsd:sequence"));
						}
						else if (xmlOutput == XML_AUTO) {
							parentElt = (Element)parentElt.appendChild(doc.createElement("xsd:sequence"));
						}
						
						String tableName = (String)tableOrder.elementAt(j);
						child = doc.createElement("xsd:element");
						child.setAttribute("name",((xmlOutput == XML_RAW) ? "row":tableName));
						child.setAttribute("minOccurs","0");
						child.setAttribute("maxOccurs","unbounded");
						if (((j==0) && (xmlOutput == XML_RAW)) || (xmlOutput != XML_RAW)) {
							parentElt = (Element)parentElt.appendChild(child);
							parentElt = (Element)parentElt.appendChild(doc.createElement("xsd:complexType"));
						}
							
						Vector cols = (Vector)tables.get(tableName);
						for (int i=0; i < cols.size(); i++) {
							if ((i==0) && (xmlOutput == XML_ELEMENT)) {
								child = doc.createElement("xsd:sequence");
								parentElt = (Element)parentElt.appendChild(child);
							}
							String columnName = (String)((Vector)cols.elementAt(i)).elementAt(1);
							String columnClassName = (String)((Vector)cols.elementAt(i)).elementAt(2);
							String type = null;
							
							if (columnClassName.equalsIgnoreCase("java.lang.Integer"))
								type = "xsd:integer";
							else if (columnClassName.equalsIgnoreCase("java.lang.Double"))
								type = "xsd:double";
							else if (columnClassName.equalsIgnoreCase("java.lang.Long"))
								type = "xsd:long";
							else if (columnClassName.equalsIgnoreCase("java.lang.Short"))
								type = "xsd:short";
							else if (columnClassName.equalsIgnoreCase("java.sql.Timestamp"))
								type = "xsd:dateTime";
							else
								type = "xsd:string";
							
							if (xmlOutput != XML_ELEMENT) {
								child = doc.createElement("xsd:attribute");
								child.setAttribute("name",columnName);
								child.setAttribute("type",type);
								parentElt.appendChild(child);
							}
							else {
								child = doc.createElement("xsd:element");
								child.setAttribute("name",columnName);
								child.setAttribute("type",type);
								parentElt.appendChild(child);
							}
						}
					}
				}
			}
			// execute the 'UPDATE/INSERT/DELETE' query
			else {
				//nb = preparedStatement.executeUpdate();
			}
				
			// close statement and resulset if exist
			preparedStatement.close();
		}
		catch (Exception e) {
			throw new EngineException(e.getMessage());
		}
		finally {
			try {
				if (preparedStatement != null)
					preparedStatement.close();
			}
			catch(SQLException e) {;}
			preparedStatement = null;
			rs = null;
		}
		
		prettyPrintedText = XMLUtils.prettyPrintDOM(doc);
		prettyPrintedText = prettyPrintedText.substring(prettyPrintedText.indexOf("<xsd:"));
		return prettyPrintedText;
	}
*/
	private Element parseResults(int num) {
		Document doc = createDOM(getEncodingCharSet());
		Node document = doc.appendChild(doc.createElement("document"));
		Node sqlOutput = document.appendChild(doc.createElement("sql_output"));
		sqlOutput.appendChild(doc.createElement("result").appendChild(doc.createTextNode(num+" row(s) updated")));	
		doc.getDocumentElement().appendChild(sqlOutput);
			
		Document output = context.outputDocument;
		Element elt = (Element)output.importNode(sqlOutput,true);
		return elt;
	}
	
	private Element parseResults(List<List<Map<String,String>>> rows, List<String> columnHeaders) {
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
				//int level = Integer.parseInt((String)rowElt.get("_level"),10);
				Engine.logBeans.trace("(SqlTransaction) row"+i+" "+rowElt.toString());
				
				boolean exist = (xmlOutput == XML_RAW)?
						elements.containsKey(tag):
						elements.containsKey(rowElt);
				
				if (exist) {
					if (xmlOutput == XML_RAW) {
						element = (Element)elements.get(tag);
						for(String columnName : columnHeaders)
							if (rowElt.containsKey(columnName)){
								String value = rowElt.get(columnName);
								element.setAttribute(columnName,value);
							}
					} else {
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
				}
				if (!exist) {
					String tagName;
					
					if ((xmlOutput == XML_RAW) || (xmlOutput == XML_ELEMENT_WITH_ATTRIBUTES)) {
						tagName = "row";
					}
					else {
						tagName = rowElt.get("_tagname");
					}
					
					element = doc.createElement(tagName);

					if (xmlOutput == XML_ELEMENT_WITH_ATTRIBUTES) {
						element.setAttribute("name", rowElt.get("_tagname"));
					}

					for(String columnName : columnHeaders) {
						if (rowElt.containsKey(columnName)) {
							String value = rowElt.get(columnName);
							if ((xmlOutput == XML_RAW) || (xmlOutput == XML_AUTO)) {
								element.setAttribute(columnName,value);
							}
							else if (xmlOutput == XML_ELEMENT) {
								Node node = doc.createElement(columnName);
								node.appendChild(doc.createTextNode(value));
								element.appendChild(node);
							}
							else if (xmlOutput == XML_ELEMENT_WITH_ATTRIBUTES) {
								Element node = doc.createElement("column");
								node.setAttribute("name", columnName);
								node.appendChild(doc.createTextNode(value));
								element.appendChild(node);
							}
						}
					}

					if (xmlOutput == XML_RAW) {
						elements.put(tag, element);
					}
					else if ((xmlOutput == XML_ELEMENT) || (xmlOutput == XML_ELEMENT_WITH_ATTRIBUTES) || (xmlOutput == XML_AUTO)) {
						elements.put(rowElt, element);
					}

					parent.appendChild(element);
				}
				if (xmlOutput != XML_RAW) {
					//parentLevel = level;
					parent = element;
				}
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
	 * @return
	 */
	public String getSqlQuery() {
		return sqlQuery;
	}

	/**
	 * Setter for property sqlQuery
	 * @param string
	 */
	public void setSqlQuery(String string) {
		sqlQuery = string;
		initializeQuery(true);
	}

	/**
	 * Getter for property xmlOutput
	 * @return
	 */
	public int getXmlOutput() {
		return xmlOutput;
	}
	
	/**
	 * Setter for property xmlOutput
	 * @param string
	 */
	public void setXmlOutput(int type) {
		xmlOutput = type;
	}

	/**
	 * Getter for property xmlGrouping
	 * @return
	 */
	public boolean getXmlGrouping() {
		return xmlGrouping;
	}
	
	/**
	 * Setter for property xmlGrouping
	 * @param string
	 */
	public void setXmlGrouping(boolean group) {
		xmlGrouping = group;
	}

	public String getMaxResult() {
		return maxResult;
	}

	public void setMaxResult(String maxResult) {
		this.maxResult = maxResult;
	}

}
