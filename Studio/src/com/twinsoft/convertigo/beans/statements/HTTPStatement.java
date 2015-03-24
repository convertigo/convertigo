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

package com.twinsoft.convertigo.beans.statements;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.apache.xpath.XPathAPI;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IContainerOrdered;
import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.variables.HttpStatementMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.HttpStatementVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.parsers.triggers.DocumentCompletedTrigger;
import com.twinsoft.convertigo.engine.parsers.triggers.ITriggerOwner;
import com.twinsoft.convertigo.engine.parsers.triggers.TriggerXMLizer;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.util.StringEx;

/**
 * This class defines a HTTP statement class.
 */
public class HTTPStatement extends Statement implements IVariableContainer, ITriggerOwner, IContainerOrdered {

	private static final long serialVersionUID = 6762922098877290999L;
		
	/** Holds value of property https. */
	private boolean https = false;
	
	/** Holds value of property host. */
	private String host = "";

	/** Holds value of property port. */
	private int port = 80;
	
	/** Holds value of property uri. */
	private String requestUri = "";
	
	/** Holds value of property version. */
	private String httpVersion = "HTTP/1.1";

	/** Holds value of property headers. */
	private XMLVector<XMLVector<String>> headers = new XMLVector<XMLVector<String>>();
    
	/** Holds value of property data. */
	private XMLVector<XMLVector<Long>> orderedVariables = new XMLVector<XMLVector<Long>>();

	/** Holds the value of property form. */
	transient private String formName = "";
	
    /** Holds value of property httpVerb. */
    private HttpMethodType httpVerb = HttpMethodType.GET;
	
    /** The result of statement execution. */
    private transient byte[] result = new byte[]{};
    
    protected transient Context javascriptContext = null;
    
    protected transient Scriptable scope = null;
    
    private TriggerXMLizer trigger = new TriggerXMLizer(new DocumentCompletedTrigger(1,60000));
    
    private transient List<HttpStatementVariable> vVariables = new Vector<HttpStatementVariable>();
    transient private List<HttpStatementVariable> vAllVariables = null;

	private String urlEncodingCharset = "";
	
	private String customHttpVerb = "";
    
	/**
     * Constructs a new empty HTTPStatement object.
     */
	public HTTPStatement() {
		super();
		
		orderedVariables = new XMLVector<XMLVector<Long>>();
		orderedVariables.add(new XMLVector<Long>());
	}

    @Override
	public HTTPStatement clone() throws CloneNotSupportedException {
		HTTPStatement clonedObject = (HTTPStatement)super.clone();
		clonedObject.vVariables = new Vector<HttpStatementVariable>();
		clonedObject.vAllVariables = null;
//		clonedObject.variables = new Hashtable(16);
		clonedObject.javascriptContext = null;
		clonedObject.scope = null;
		clonedObject.result = null;
		return clonedObject;
	}

    @Override
	public void add(DatabaseObject databaseObject) throws EngineException {
        if (databaseObject instanceof HttpStatementVariable) {
            addVariable((HttpStatementVariable) databaseObject);
        } else {
        	throw new EngineException("You cannot add to an http statement a database object of type " + databaseObject.getClass().getName());
        }
	}

    @Override
	public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof HttpStatementVariable) {
			removeVariable((HttpStatementVariable) databaseObject);
		} else {
        	throw new EngineException("You cannot remove from an http statement a database object of type " + databaseObject.getClass().getName());
        }
		
	}
	
    public List<HttpStatementVariable> getVariables(boolean reset) {
    	if (reset)
    		vAllVariables = null;
    	return getVariables();
    }
	
    public List<HttpStatementVariable> getVariables() {
    	checkSubLoaded();
    	
    	if ((vAllVariables == null) || hasChanged)
    		vAllVariables = getAllVariables();
    	return vAllVariables;
    }
    
    public List<HttpStatementVariable> getAllVariables() {
    	checkSubLoaded();
    	
    	return sort(vVariables);
    }

	public Variable getVariable(int index) {
		checkSubLoaded();
		
		try {
			return vVariables.get(index);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}
	
	public Variable getVariable(String variableName) {
		checkSubLoaded();
		
		for (int i=0; i<vVariables.size(); i++) {
			HttpStatementVariable variable = vVariables.get(i);
			if (variable.getName().equals(variableName)) {
				return variable;
			}
		}
		return null;
	}
	
	public Object getVariableValue(String requestedVariableName) {
		Object value = null, valueToPrint = null;
		Variable variable = getVariable(requestedVariableName);
		if (variable != null) {
			value = variable.getValueOrNull();
			valueToPrint = Visibility.Logs.printValue(variable.getVisibility(), value);
			if ((value != null) && (value instanceof String))
				Engine.logBeans.debug("Default value: " + requestedVariableName + " = \"" + valueToPrint + "\"");
			else
				Engine.logBeans.debug("Default value: " + requestedVariableName + " = " + valueToPrint);
		}
		return value;
	}
	
	public boolean hasVariables() {
		checkSubLoaded();
		
		return (vVariables.size() > 0);
	}
    
	public int numberOfVariables() {
		checkSubLoaded();
		
		return vVariables.size();
	}
	
    public void addVariable(HttpStatementVariable variable) throws EngineException {
    	checkSubLoaded();
    	
		String newDatabaseObjectName = getChildBeanName(vVariables, variable.getName(), variable.bNew);
		variable.setName(newDatabaseObjectName);
        
		vVariables.add(variable);
        
        variable.setParent(this);
        
        insertOrderedVariable(variable,null);
    }
	
    private void insertOrderedVariable(Variable variable, Long after) {
    	XMLVector<Long> ordered = orderedVariables.get(0);
    	int size = ordered.size();
    	
    	Long value = new Long(variable.priority);
    	
    	if (ordered.contains(value))
    		return;
    	
    	if (after == null) {
    		after = new Long(0);
    		if (size>0)
    			after = ordered.get(size-1);
    	}
    	
   		int order = ordered.indexOf(after);
    	ordered.add(order+1, value);
    	hasChanged = true;
    }
    
    public void removeVariable(HttpStatementVariable variable) {
    	checkSubLoaded();
    	
    	vVariables.remove(variable);
    	variable.setParent(null);
    	
    	Long value = new Long(variable.priority);
        removeOrderedVariable(value);
    	
    }
    
    private void removeOrderedVariable(Long value) {
    	XMLVector<Long> ordered = orderedVariables.get(0);
        ordered.remove(value);
        hasChanged = true;
    }

	public void insertAtOrder(DatabaseObject databaseObject, long priority) throws EngineException {
		increaseOrder(databaseObject, new Long(priority));
	}

	private void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
    	XMLVector<Long> ordered = null;
    	Long value = new Long(databaseObject.priority);
    	
    	if (databaseObject instanceof Variable)
    		ordered = orderedVariables.get(0);
    	
    	if (!ordered.contains(value))
    		return;
    	int pos = ordered.indexOf(value);
    	if (pos == 0)
    		return;
    	
    	if (before == null)
    		before = ordered.get(pos-1);
    	int pos1 = ordered.indexOf(before);
    	
    	ordered.add(pos1, value);
    	ordered.remove(pos+1);
    	hasChanged = true;
    }
    
    private void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
    	XMLVector<Long> ordered = null;
    	Long value = new Long(databaseObject.priority);
    	
    	if (databaseObject instanceof Variable)
    		ordered = orderedVariables.get(0);
    	
    	if (!ordered.contains(value))
    		return;
    	int pos = ordered.indexOf(value);
    	if (pos+1 == ordered.size())
    		return;
    	
    	if (after == null)
    		after = ordered.get(pos+1);
    	int pos1 = ordered.indexOf(after);
    	
    	ordered.add(pos1+1, value);
    	ordered.remove(pos);
    	hasChanged = true;
    }
    
	public void increasePriority(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof Variable)
			increaseOrder(databaseObject,null);
	}

	public void decreasePriority(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof Variable)
			decreaseOrder(databaseObject,null);
	}
    
    /**
     * Get representation of order for quick sort of a given database object.
     */
	@Override
    public Object getOrder(Object object) throws EngineException	{
        if (object instanceof Variable) {
        	List<Long> ordered = orderedVariables.get(0);
        	long time = ((Variable)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted variable for statement \""+ getName() +"\". Variable \""+ ((Variable)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        } else return super.getOrder(object);
    }
    
	public void addHeader(String headername, String value){
		if ((headername != null) && !(headername.equals(""))) {
			XMLVector<String> v = new XMLVector<String>();
			v.add(headername);
			v.add((value == null) ? "":value);
			headers.add(v);
		}
	}
	
	public void addData(String variable, Object value, boolean bmulti, String method)
	{
		if ((variable != null) && !(variable.equals(""))) {
			String variableName = parse(variable);
			HttpStatementVariable httpStatementVariable = (bmulti ? new HttpStatementMultiValuedVariable():new HttpStatementVariable());
			try {
				httpStatementVariable.setName(StringUtils.normalize(variableName));
				httpStatementVariable.setDescription("");
				httpStatementVariable.setValueOrNull(value);
				httpStatementVariable.setHttpMethod(method);
				httpStatementVariable.setHttpName(variableName);
				
				httpStatementVariable.bNew = true;
				httpStatementVariable.hasChanged = true;
				hasChanged = true;
				
			} catch (EngineException e) {
				Engine.logBeans.info("Could not update variable");
			}
		}
	}

	/**
	 * @return Returns the httpVersion.
	 */
	public String getHttpVersion() {
		return httpVersion;
	}

	/**
	 * @return Returns the formName.
	 */
	public String getFormName() {
		return formName;
	}

	/**
	 * @param formName The formName to set.
	 */
	public void setFormName(String formName) {
		this.formName = formName;
	}

	/**
	 * @param httpVersion The httpVersion to set.
	 */
	public void setHttpVersion(String httpVersion) {
		this.httpVersion = httpVersion;
	}

//	public String getMethod() {
//		switch (methodType) {
//			case 8: return "DELETE";
//			case 7: return "CONNECT";
//			case 6: return "OPTIONS";
//			case 5: return "TRACE";
//			case 4: return "PUT";
//			case 3: return "HEAD";
//			case 2: return "POST";
//			case 1: return "GET";
//			default: return "";
//		}
//	}
	
	/**
	 * @return Returns the method.
	 */
//	public int getMethodType() {
//		return methodType;
//	}

	/**
	 * @param method The method to set.
	 */
//	public void setMethodType(int method) {
//		this.methodType = method;
//	}

	/**
	 * @return Returns the host.
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host The host to set.
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return Returns the requestUri.
	 */
	public String getRequestUri() {
		return requestUri;
	}

	/**
	 * @param requestUri The requestUri to set.
	 */
	public void setRequestUri(String requestUri) {
		this.requestUri = requestUri;
	}

	/**
	 * @return the https
	 */
	public boolean isHttps() {
		return https;
	}

	/**
	 * @param https the https to set
	 */
	public void setHttps(boolean https) {
		this.https = https;
	}
	
	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	/**
	 * @return Returns the headers.
	 */
	public XMLVector<XMLVector<String>> getHeaders() {
		return headers;
	}

	/**
	 * @param headers The headers to set.
	 */
	public void setHeaders(XMLVector<XMLVector<String>> headers) {
		this.headers = headers;
	}

	/**
	 * @return Returns the orderedVariables.
	 */
	public XMLVector<XMLVector<Long>> getOrderedVariables() {
		return orderedVariables;
	}

	/**
	 * @param orderedVariables The orderedVariables to set.
	 */
	public void setOrderedVariables(XMLVector<XMLVector<Long>> orderedVariables) {
		this.orderedVariables = orderedVariables;
	}
	
    @Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);
		
		String version = element.getAttribute("version");
		
		if (VersionUtils.compare(version, "4.2.0") < 0) {
			NodeList properties = element.getElementsByTagName("property");
			
			Element propName = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "name");
			String objectName = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(propName, Node.ELEMENT_NODE));
			
			Element propVarDef = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "data");
			if (propVarDef != null) {
				propVarDef.setAttribute("name", "variablesDefinition");
				hasChanged = true;
				Engine.logBeans.warn("[HTTPStatement] The object \""+objectName+"\" has been updated to version 4.2.0 (property \"data\" changed to \"variablesDefinition\")");
			}
		}
		
		if (VersionUtils.compare(version, "4.6.0") < 0) {
			NodeList properties = element.getElementsByTagName("property");
			
			Element propName = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "name");
			String objectName = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(propName, Node.ELEMENT_NODE));
			
			Element propVarDef = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "variablesDefinition");
			if (propVarDef != null) {
				propVarDef.setAttribute("name", "orderedVariables");
				hasChanged = true;
				Engine.logBeans.warn("[HTTPStatement] The object \""+objectName+"\" has been updated to version 4.6.0 (property \"variablesDefinition\" changed to \"orderedVariables\")");
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.Statement#configure(org.w3c.dom.Element)
	 */
    @Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
        String version = element.getAttribute("version");
        
        if (version == null) {
            String s = XMLUtils.prettyPrintDOM(element);
            EngineException ee = new EngineException(
                "Unable to find version number for the database object \"" + getName() + "\".\n" +
                "XML data: " + s
            );
            throw ee;
        }
        
        if (VersionUtils.compare(version, "4.0.2") < 0) {
        	requestUri = "'" + requestUri + "'";
			hasChanged = true;
			Engine.logBeans.warn("[HttpStatement] The object \"" + getName()+ "\" has been updated to version 4.0.2");
        }
        
		if (VersionUtils.compare(version, "4.2.0") < 0) {
			int len = orderedVariables.size();
			XMLVector<Object> line;
			for (int i = 0 ; i < len ; i++) {
				line = GenericUtils.cast(orderedVariables.get(i));
				if (line.size()>0) {
					// Sets empty description by default
					line.add(1 ,"");
				}
			}
			
			hasChanged = true;
			Engine.logBeans.warn("[HTTPStatement] The object \"" + getName() + "\" has been updated to version 4.2.0");
		}
		
		try {
			Node node = XPathAPI.selectSingleNode(element, "property[@name='httpVerb']/java.lang.Integer/@value");
			if (node != null) {
				httpVerb = HttpMethodType.values()[Integer.parseInt(node.getNodeValue())];
				hasChanged = true;
				Engine.logBeans.warn("[HttpStatement] The object \"" + getName() + "\" has been updated to use the new 'httpVerb' format");
			}
		} catch (Throwable t) {
			// ignore migration errors
		}
	}

    @Override
	public String toJsString() {
		return "";
	}

	public boolean isHandleCookie() {
		HtmlTransaction htmlTransaction = getParentTransaction();
		return htmlTransaction.isHandleCookie();
	}

//	public void setMethodType(String method) {
//		int type = HTTP_NONE;
//		if (method != null) {
//			if (method.equalsIgnoreCase("GET"))
//				type = HTTP_GET;
//			if (method.equalsIgnoreCase("POST"))
//				type = HTTP_POST;
//			if (method.equalsIgnoreCase("HEAD"))
//				type = HTTP_HEAD;
//			if (method.equalsIgnoreCase("PUT"))
//				type = HTTP_PUT;
//			if (method.equalsIgnoreCase("TRACE"))
//				type = HTTP_TRACE;
//			if (method.equalsIgnoreCase("OPTIONS"))
//				type = HTTP_OPTIONS;
//			if (method.equalsIgnoreCase("CONNECT"))
//				type = HTTP_CONNECT;
//			if (method.equalsIgnoreCase("DELETE"))
//				type = HTTP_DELETE;
//		}
//		setMethodType(type);
//	}
	
	public XMLVector<XMLVector<String>> getHttpParameters() {
		XMLVector<XMLVector<String>> v = new XMLVector<XMLVector<String>>();
		for(XMLVector<String> header : headers)
			v.add(header);
		return v;
	}

	public String getUrl(boolean connectorIsHttps, String connectorServer, int connectorPort) throws EngineException {
		
		String uri = getRequestUri();
		
		// evaluate uri in case of it's a javascript expression
		if ((!uri.equals("")) && (javascriptContext != null) && (scope != null)) {
			evaluate(javascriptContext, scope, uri, "URI", true);
			if (evaluated != null)
				uri = evaluated.toString();
		}
		
		String sUrl = uri;
		int id, jd;
		
		if((id=sUrl.indexOf("://"))==-1 || ((jd=sUrl.indexOf('?'))!=-1 && id > jd)){
			if (host.equals("")) {
				sUrl = "http" + ((connectorIsHttps) ? "s":"");
				sUrl += "://" + connectorServer ;
				if ((connectorIsHttps && (connectorPort != 443)) || (!connectorIsHttps && (connectorPort != 80))) {
					sUrl += ":" + connectorPort;
				}
				sUrl += uri;
			} else {
				if (sUrl.indexOf(host) == -1) {
					sUrl = "http" + ((https) ? "s":"");
					sUrl += "://" + host ;
					if ((https && (port != 443)) || (!https && (port != 80))) {
						sUrl += ":" + port;
					}
					sUrl += uri;
				}
			}
		}
		return sUrl;
	}

	public void parseInputDocument(com.twinsoft.convertigo.engine.Context context) {
	}
	
	private String parse(String httpVariableName) {
		StringEx variableName = new StringEx(httpVariableName);
		variableName.replaceAll(".", "");
		variableName.replaceAll("[", "");
		variableName.replaceAll("]", "");
		variableName.replaceAll("(", "");
		variableName.replaceAll(")", "");
		return variableName.toString();
	}
	
	/*public String getScriptableValue(String requestedHttpVariableName) {
		int index = getHttpVariableDefinitionIndex(requestedHttpVariableName);
		if (index != -1) {
			return getVariableDefinitionName(index);
		}
		return null;
	}*/

	public String getQueryString(com.twinsoft.convertigo.engine.Context context) throws EngineException {
		return makeQuery(context, "GET");
	}

	public String getPostQuery(com.twinsoft.convertigo.engine.Context context) throws EngineException {		
		// Posting all input variables marked as POST
		return makeQuery(context, "POST");
	}

	private String makeQuery(com.twinsoft.convertigo.engine.Context context, String methodToAnalyse) throws EngineException {
		String variable, httpVariable, httpVariableValue, method, query = "";
		int len = numberOfVariables();
		String urlEncodingCharset = getUrlEncodingCharset();
		if (urlEncodingCharset == null || urlEncodingCharset.length() == 0) {
			urlEncodingCharset = getParentTransaction().getComputedUrlEncodingCharset();
		}
		
		try {
			for (int i = 0 ; i < len ; i++) {
				HttpStatementVariable httpStatementVariable = (HttpStatementVariable)getVariable(i);
				if (httpStatementVariable != null) {
					variable = httpStatementVariable.getName();
					method = httpStatementVariable.getHttpMethod();
					httpVariable = httpStatementVariable.getHttpName();
					
					if (method.equals(methodToAnalyse)) {
						if (query.length() != 0) {
							query += "&";
						}

						try {
						// evaluate method can throw EngineException
						// try catch to get the default value in this case
							evaluate(javascriptContext, scope, variable, httpVariable, false);
			    			Engine.logBeans.debug("Javascript evaluation of httpVariable named '"+ httpVariable +"' executed");
			    			
			    			// if no Engine Exception has been thown until here, normal execution    			
			    			if (evaluated != null) {
								if (evaluated instanceof NativeJavaArray) {
									Object object = ((NativeJavaArray)evaluated).unwrap();
									List<Object> list = Arrays.asList((Object[])object);
									for (int j=0; j<list.size(); j++) {
										Object item = list.get(j);
										httpVariableValue = item.toString();
										query = addVariableToQuery(methodToAnalyse, httpVariable, httpVariableValue, query, urlEncodingCharset, j != 0);
									}
								}
								else if (evaluated instanceof NativeJavaObject) {
									NativeJavaObject nativeJavaObject = (NativeJavaObject)evaluated;
									Object javaObject = nativeJavaObject.unwrap();
									if (javaObject instanceof Vector) {
										Vector<String> v = GenericUtils.cast(javaObject);
										for (int j=0; j<v.size(); j++) {
											httpVariableValue = v.get(j);
											query = addVariableToQuery(methodToAnalyse, httpVariable, httpVariableValue, query, urlEncodingCharset, j != 0);
										}
									}
									else {
										httpVariableValue = (String)nativeJavaObject.getDefaultValue(String.class);
										query = addVariableToQuery(methodToAnalyse, httpVariable, httpVariableValue, query, urlEncodingCharset, false);
									}
								}
								else if (evaluated instanceof NativeArray) {
									NativeArray array = (NativeArray)evaluated;
									for (int j=0; j<array.getLength(); j++) {
										Object item = array.get(j,array);
										httpVariableValue = item.toString();
										query = addVariableToQuery(methodToAnalyse, httpVariable, httpVariableValue, query, urlEncodingCharset, j != 0);
									}
								}
								else if (evaluated instanceof Vector) {
									Vector<String> v = GenericUtils.cast(evaluated);
									for (int j=0; j<v.size(); j++) {
										httpVariableValue = v.get(j);
										query = addVariableToQuery(methodToAnalyse, httpVariable, httpVariableValue, query, urlEncodingCharset, j != 0);
									}
								}
								else if (evaluated instanceof Undefined) {
									throw new EngineException("Undefined");
								}
								else {
									httpVariableValue = evaluated.toString();
									query = addVariableToQuery(methodToAnalyse, httpVariable, httpVariableValue, query, urlEncodingCharset, false);
								}
							}
						} catch(EngineException e) {
							// Engine Exception has been thrown ==> get variable default value
							Object value = getVariableValue(variable);
							if (value != null) {
								if (value instanceof Collection) {
									List<String> list = GenericUtils.toString((Collection<?>)value);
									for (String val : list) {
										httpVariableValue = val;
										query = addVariableToQuery(methodToAnalyse, httpVariable, httpVariableValue, query, urlEncodingCharset, false);
									}
								}
								else {
									httpVariableValue = value.toString();
									query = addVariableToQuery(methodToAnalyse, httpVariable, httpVariableValue, query, urlEncodingCharset, false);
								}
							}
						}
					} 
				}
			}
		} catch (UnsupportedEncodingException e) {
			throw new EngineException("UTF-8 encoding is not supported.", e);
		}

		return query;
	}

	
	protected String addVariableToQuery(String methodToAnalyse, String httpVariable, String httpVariableValue, String query, String urlEncodingCharset, boolean firstParam) throws UnsupportedEncodingException {
		// if the variable name is in the form __header_<header name>, extract the header name and set it's value in the headers
		
		if (httpVariable.startsWith(Parameter.HttpHeader.getName())) {
			XMLVector<String> header = new XMLVector<String>();
			header.add(httpVariable.substring(Parameter.HttpHeader.getName().length()));
			header.add(httpVariableValue);
			headers.add(header);
			return "";
		}
		
		if (httpVariable.equals(""))
			return query;
		
		if (methodToAnalyse.equalsIgnoreCase("POST"))
			query += (firstParam ? "&" : "") + URLEncoder.encode(httpVariable, urlEncodingCharset) + "=" + URLEncoder.encode(httpVariableValue, urlEncodingCharset);
		else if (methodToAnalyse.equalsIgnoreCase("GET"))
			query += (firstParam ? "&" : "") + httpVariable + "=" + URLEncoder.encode(httpVariableValue, urlEncodingCharset);
		return query;
	}
	
	public byte[] getResult() {
		return result;
	}

    @Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.execute(javascriptContext, scope)) {
				this.javascriptContext = javascriptContext;
				this.scope = scope;
				
				HtmlTransaction htmlTransaction = getParentTransaction();
				HttpMethodType exVerb = htmlTransaction.getHttpVerb();
				String exCustomVerb = htmlTransaction.getCustomHttpVerb();
				long exTimeout = htmlTransaction.getResponseTimeout();
				
				try {
					htmlTransaction.setHttpVerb(getHttpVerb());
					htmlTransaction.setCustomHttpVerb(getCustomHttpVerb());
					try {
						htmlTransaction.setResponseTimeout(getTrigger().getTrigger().getTimeout() / 1000);
					} finally {}
					
					htmlTransaction.applyUserRequest(this);
					return true;
				} finally {
					htmlTransaction.setHttpVerb(exVerb);
					htmlTransaction.setCustomHttpVerb(exCustomVerb);
					htmlTransaction.setResponseTimeout(exTimeout);
				}
			}
		}
		return false;
	}

	public TriggerXMLizer getTrigger() {
		return trigger;
	}

	public void setTrigger(TriggerXMLizer trigger) {
		this.trigger = trigger;
	}
	
    public HttpMethodType getHttpVerb() {
		return httpVerb;
	}

	public void setHttpVerb(HttpMethodType httpVerb) {
		this.httpVerb = httpVerb;
	}
	
	public String getMethod() {
		return httpVerb.name();
	}
	
	public void setMethod(String method) {
		httpVerb = HttpMethodType.valueOf(method);
	}

	public String getUrlEncodingCharset() {
		return urlEncodingCharset ;
	}

	public void setUrlEncodingCharset(String urlEncodingCharset) {
		this.urlEncodingCharset = urlEncodingCharset;
	}

	public String getCustomHttpVerb() {
		return customHttpVerb;
	}

	public void setCustomHttpVerb(String customHttpVerb) {
		this.customHttpVerb = customHttpVerb;
	}
}