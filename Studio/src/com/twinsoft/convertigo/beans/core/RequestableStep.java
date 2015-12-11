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

package com.twinsoft.convertigo.beans.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.connectors.ConnectionException;
import com.twinsoft.convertigo.beans.variables.RequestableMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.beans.variables.StepMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.HttpPool;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.Log4jHelper;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.util.StringEx;

public abstract class RequestableStep extends Step implements IVariableContainer, IContainerOrdered, ISchemaParticleGenerator {
	
	private static final long serialVersionUID = 3948128175718822695L;

	public static final String SOURCE_SEPARATOR = ".";

	private XMLVector<XMLVector<Long>> orderedVariables = new XMLVector<XMLVector<Long>>();
	
	transient private List<StepVariable> vVariables = new LinkedList<StepVariable>();
	transient private List<StepVariable> vAllVariables = null;
	transient protected Document xmlHttpDocument = null;
	transient protected PostMethod method = null;
	transient protected HostConfiguration hostConfiguration = null;
	transient protected String targetUrl = "";
	
	private String contextName = "";
	
	private boolean bInternalInvoke = true;
	
	transient protected String projectName = "";
	
	transient protected Map<String, Object> request;
	
	transient public String wsdlType = "";
	
	public RequestableStep() {
		super();
		xml = true;
		
		hostConfiguration = new HostConfiguration();
		
		orderedVariables = new XMLVector<XMLVector<Long>>();
		orderedVariables.add(new XMLVector<Long>());
	}

	@Override
	public RequestableStep clone() throws CloneNotSupportedException {
		RequestableStep clonedObject = (RequestableStep) super.clone();
		clonedObject.vVariables = new LinkedList<StepVariable>();
		clonedObject.vAllVariables = null;
		clonedObject.xmlHttpDocument = null;
		clonedObject.hostConfiguration = new HostConfiguration();
		clonedObject.targetUrl = null;
		clonedObject.method = null;
		return clonedObject;
	}
	
	@Override
	public Object copy() throws CloneNotSupportedException {
		RequestableStep copiedObject = (RequestableStep)super.copy();
		copiedObject.vVariables = getVariablesCopy(copiedObject);
		return copiedObject;
	}

	public String getProjectName() {
		return projectName;
	}
	
	@Override
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += getLabel();
		} catch (EngineException e) { }
		return getName() + (label.equals("") ? "":" ") + label + (!text.equals("") ? " // "+text:"");
	}
	
	@Override
	public String toJsString() {
		return "";
	}
	
	protected Project getTargetProject(String projectName) throws EngineException {
		return getSequence().getLoadedProject(projectName);
	}
	
	public String getStepContextName() {
		return contextName;
	}
	
	public void setStepContextName(String contextName) {
		this.contextName = contextName;
	}
	
	public boolean isInternalInvoke() {
		return bInternalInvoke;
	}

	public void setInternalInvoke(boolean internalInvoke) {
		bInternalInvoke = internalInvoke;
	}
	
	private List<StepVariable> getVariablesCopy(RequestableStep copiedObject) throws CloneNotSupportedException {
		List<StepVariable> v = new ArrayList<StepVariable>(vVariables.size());
		for (Variable var : vVariables ) {
			StepVariable stepVariableCopy = (StepVariable) var.clone();
			stepVariableCopy.parent = copiedObject;
			v.add(stepVariableCopy);
		}
		return v;
	}
	
	@Override
	protected void cleanCopy() {
		super.cleanCopy();
		if (vVariables != null) {
			vVariables.clear();
			vVariables = null;
		}
		if (vAllVariables != null) {
			vAllVariables.clear();
			vAllVariables = null;
		}
	}
	
	@Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);
		
		String version = element.getAttribute("version");
		
		if (VersionUtils.compare(version, "4.6.0") < 0) {
			NodeList properties = element.getElementsByTagName("property");
			
			Element propName = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "name");
			String objectName = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(propName, Node.ELEMENT_NODE));
			
			Element propVarDef = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "variablesDefinition");
			if (propVarDef != null) {
				propVarDef.setAttribute("name", "orderedVariables");
				hasChanged = true;
				Engine.logBeans.warn("[RequestableStep] The object \""+objectName+"\" has been updated to version 4.6.0 (property \"variablesDefinition\" changed to \"orderedVariables\")");
			}
		}
	}
	
	@Override
	public void configure(Element element) throws Exception {
        super.configure(element);

		String version = element.getAttribute("version");
        
		if (version == null) {
			String s = XMLUtils.prettyPrintDOM(element);
			EngineException ee = new EngineException("Unable to find version number for the database object \"" + getName() + "\".\nXML data: " + s);
			throw ee;
		}
		
        try {
            NodeList childNodes = element.getElementsByTagName("wsdltype");
            int len = childNodes.getLength();
            if (len > 0) {
                Node childNode = childNodes.item(0);
                Node cdata = XMLUtils.findChildNode(childNode, Node.CDATA_SECTION_NODE);
                if (cdata != null) {
                    wsdlType = cdata.getNodeValue();
                    Engine.logBeans.trace("(RequestableStep) configure() : wsdltype has been successfully set");
                } else Engine.logBeans.trace("(RequestableStep) configure() : wsdltype is empty");
            }
        } catch(Exception e) {
            throw new EngineException("Unable to retrieve the wsdltype for the sequence step \"" + getName() + "\".", e);
        }
        
        if (VersionUtils.compare(version, "4.6.0") < 0) {
			// Backup wsdlTypes to file
			try {
				backupWsdlTypes(element);
				if (!wsdlType.equals("")) {
					wsdlType = "";
					hasChanged = true;
					Engine.logBeans.warn("[RequestableStep] Successfully backup wsdlTypes for step \""+ getName() +"\" (v 4.6.0)");
				}
				else {
					Engine.logBeans.warn("[RequestableStep] Empty wsdlTypes for step \""+ getName() +"\", none backup done (v 4.6.0)");
				}
	    	}
	    	catch (Exception e) {
	    		Engine.logBeans.error("[RequestableStep] Could not backup wsdlTypes for step \""+ getName() +"\" (v 4.6.0)", e);
	    	}
        }
	}
	
    protected String getWsdlBackupDir() throws Exception {
    	return getProject().getDirPath() + "/backup-wsdl";
    }
	
    protected String getWsdlBackupDir(Element element) throws Exception {
    	Element rootElement = element.getOwnerDocument().getDocumentElement();
    	Element projectNode = (Element) XMLUtils.findChildNode(rootElement, Node.ELEMENT_NODE);
    	NodeList properties = projectNode.getElementsByTagName("property");
		Element pName = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "name");
		String projectName = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(pName, Node.ELEMENT_NODE));
    	return Engine.PROJECTS_PATH + "/"+ projectName + "/backup-wsdl";
    }
    
    protected void backupWsdlTypes(Element element) throws TransformerFactoryConfigurationError, Exception {
    	if (wsdlType.equals(""))
    		return;
    	
		StringEx sx = new StringEx(wsdlType);
		sx.replaceAll("<cdata>","<![CDATA[");
		sx.replaceAll("</cdata>","]]>");
		sx.replaceAll("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>", "");
		String sDom = sx.toString();
		DocumentBuilder documentBuilder = XMLUtils.getDefaultDocumentBuilder();
		Document document = documentBuilder.parse(new InputSource(new StringReader(sDom)));
		
		String wsdlBackupDir = getWsdlBackupDir(element);
        File dir = new File(wsdlBackupDir);
		if (!dir.exists())
			dir.mkdirs();

		File file = new File(wsdlBackupDir + "/step-" + priority + ".xml");
		XMLUtils.saveXml(document, file);
    }
	
	public XMLVector<XMLVector<Long>> getOrderedVariables() {
		return orderedVariables;
	}
    
	public void setOrderedVariables(XMLVector<XMLVector<Long>> orderedVariables) {
		this.orderedVariables = orderedVariables;
	}
	
	@Override
	public void add(DatabaseObject databaseObject) throws EngineException {
        if (databaseObject instanceof StepVariable)
            addVariable((StepVariable) databaseObject);
        else throw new EngineException("You cannot add to a requestable step a database object of type " + databaseObject.getClass().getName());
	}

	@Override
	public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof StepVariable)
			removeVariable((StepVariable) databaseObject);
		else throw new EngineException("You cannot remove from a requestable step a database object of type " + databaseObject.getClass().getName());
	}
	
    public List<StepVariable> getVariables(boolean reset) {
    	if (reset)
    		vAllVariables = null;
    	return getVariables();
    }
	
    public List<StepVariable> getVariables() {
    	checkSubLoaded();    	
    	if ((vAllVariables == null) || hasChanged)
    		vAllVariables = getAllVariables();
    	return vAllVariables;
    }
    
    public List<StepVariable> getAllVariables() {
    	checkSubLoaded();
    	return sort(vVariables);
    }
	
	public Variable getVariable(int index) {
		checkSubLoaded();
		try {
			return vVariables.get(index);
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}
	
	public Variable getVariable(String variableName) {
		checkSubLoaded();
		for(StepVariable variable : vVariables)
			if (variable.getName().equals(variableName))
				return variable;
		return null;
	}
	
	public Object getVariableValue(String requestedVariableName) {
		Object value = null, valueToPrint = null;
		StepVariable stepVariable = (StepVariable)getVariable(requestedVariableName);
		if (stepVariable != null) {
			value = stepVariable.getValueOrNull();
			valueToPrint = Visibility.Logs.printValue(stepVariable.getVisibility(), value);
			if ((value != null) && (value instanceof String))
				Engine.logBeans.debug("Default value: " + requestedVariableName + " = \"" + valueToPrint + "\"");
			else
				Engine.logBeans.debug("Default value: " + requestedVariableName + " = " + valueToPrint);
		}
		return value;
	}
	
	public boolean hasVariables() {
		checkSubLoaded();
		return vVariables.size() > 0;
	}
    
	public int numberOfVariables() {
		checkSubLoaded();
		return vVariables.size();
	}
	
    public void addVariable(StepVariable variable) throws EngineException {
    	checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vVariables, variable.getName(), variable.bNew);
		variable.setName(newDatabaseObjectName);
		vVariables.add(variable);
        variable.setParent(this);
        insertOrderedVariable(variable,null);
    }
	
    private void insertOrderedVariable(Variable variable, Long after) {
    	List<Long> ordered = orderedVariables.get(0);
    	int size = ordered.size();
    	
    	if (ordered.contains(variable.priority))
    		return;
    	
    	if (after == null) {
    		after = new Long(0);
    		if (size>0)
    			after = ordered.get(ordered.size()-1);
    	}
    	
   		int order = ordered.indexOf(after);
    	ordered.add(order+1, variable.priority);
    	hasChanged = true;
    }
    
    public void removeVariable(StepVariable variable) {
    	checkSubLoaded();
    	vVariables.remove(variable);
    	variable.setParent(null);
        removeOrderedVariable(variable.priority);
    }
    
    private void removeOrderedVariable(Long value) {
        Collection<Long> ordered = orderedVariables.get(0);
        ordered.remove(value);
        hasChanged = true;
    }

	public void insertAtOrder(DatabaseObject databaseObject, long priority) throws EngineException {
		increaseOrder(databaseObject, new Long(priority));
	}
    
    private void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
    	List<Long> ordered = null;
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
    	List<Long> ordered = null;
    	long value = databaseObject.priority;
    	
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
        	else throw new EngineException("Corrupted variable for step \""+ getName() +"\". Variable \""+ ((Variable)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        }
        else return super.getOrder(object);
    }
    
	@Override
    public void stepMoved(StepEvent stepEvent) {
		StepVariable stepVariable;
		for (int i = 0 ; i < numberOfVariables() ; i++) {
			stepVariable = (StepVariable)getVariable(i);
			if (stepVariable != null) {
				XMLVector<String> sourceDefinition = stepVariable.getSourceDefinition();
				if (sourceDefinition.size() > 0) {
					StepSource source = new StepSource(this, sourceDefinition);
					if (source != null) {
						source.updateTargetStep((Step)stepEvent.getSource(), (String)stepEvent.data);
					}
				}
			}
		}
    }
    
	@Override
    protected String getSpecificLabel() throws EngineException {
		try {
			StepVariable stepVariable;
			for (int i = 0 ; i < numberOfVariables() ; i++) {
				stepVariable = (StepVariable)getVariable(i);
				if (stepVariable != null) {
					XMLVector<String> sourceDefinition = stepVariable.getSourceDefinition();
					if (sourceDefinition.size() > 0) {
						StepSource source = new StepSource(this, sourceDefinition);
						if (source != null) {
							if (source.getLabel().equals("! broken source !"))
								return " (! broken source in variable !)";
						}
					}
				}
			}
		}
		catch (EngineException e) {}
		return "";
    }
    
    public void importVariableDefinition(RequestableObject requestable) throws EngineException {
    	if (!(requestable instanceof IVariableContainer))
    		return;
    	
    	IVariableContainer container = (IVariableContainer)requestable;
    	
		int size = container.numberOfVariables();
		for (int i=0; i<size; i++) {
			RequestableVariable variable = (RequestableVariable)container.getVariable(i);
			if (variable != null) {
				String variableName = variable.getName();
				if (getVariable(variableName) == null) {
					if (!StringUtils.isNormalized(variableName))
						throw new EngineException("Variable name is not normalized : \""+variableName+"\".");
					
					StepVariable stepVariable = variable.isMultiValued() ? new StepMultiValuedVariable():new StepVariable();
					stepVariable.setName(variableName);
					stepVariable.setDescription(variable.getDescription());
					stepVariable.setSourceDefinition(new XMLVector<String>());
					stepVariable.setValueOrNull(variable.getValueOrNull());
					stepVariable.setVisibility(variable.getVisibility());
					addVariable(stepVariable);

					stepVariable.bNew = true;
					stepVariable.hasChanged = true;
					hasChanged = true;
				}
			}
		}
    }
    
    public void exportVariableDefinition() throws EngineException {
    	for (StepVariable stepVariable: getVariables()) {
    		String variableName = stepVariable.getName();
    		if (sequence.getVariable(variableName) == null) {
				if (!StringUtils.isNormalized(variableName))
					throw new EngineException("Variable name is not normalized : \""+variableName+"\".");
				
	    		RequestableVariable requestableVariable = stepVariable.isMultiValued() ? new RequestableMultiValuedVariable():new RequestableVariable();
	    		requestableVariable.setName(variableName);
	    		requestableVariable.setDescription(stepVariable.getDescription());
	    		requestableVariable.setValueOrNull(stepVariable.getValueOrNull());
	    		requestableVariable.setVisibility(stepVariable.getVisibility());
	    		sequence.addVariable(requestableVariable);
	    		
	    		requestableVariable.bNew = true;
	    		requestableVariable.hasChanged = true;
				sequence.hasChanged = true;
    		}
    	}
    }
    
	protected String getPostQuery(Scriptable scope) throws EngineException {
		StepVariable stepVariable;
		String postQuery = "";
		
		int len = numberOfVariables();
		String variableName;
		int variableVisibility;
		
		for (int i=0; i<len; i++) {
			stepVariable = (StepVariable)getVariable(i);
			variableName = stepVariable.getName();
			variableVisibility = stepVariable.getVisibility();
			try {
				// Source value
				Object variableValue = stepVariable.getSourceValue();
				if (variableValue != null)
					Engine.logBeans.trace("(RequestableStep) found value from source: " + Visibility.Logs.printValue(variableVisibility,variableValue));
				
				// Otherwise Scope parameter
				if (variableValue == null) {
					Scriptable searchScope = scope;
					while ((variableValue == null) && (searchScope != null)) {
						variableValue = searchScope.get(variableName,searchScope);
						Engine.logBeans.trace("(RequestableStep) found value from scope: " + Visibility.Logs.printValue(variableVisibility,variableValue));
						if (variableValue instanceof Undefined)
							variableValue = null;
						if (variableValue instanceof UniqueTag && ((UniqueTag) variableValue).equals(UniqueTag.NOT_FOUND)) 
							variableValue = null;
						
						if (variableValue == null)
							searchScope = searchScope.getParentScope();// looks up in parent's scope
					}
				}
				
				// Otherwise context parameter
				if (variableValue == null) {
					variableValue = (sequence.context.get(variableName) == null ? null : sequence.context.get(variableName));
					if (variableValue != null)
						Engine.logBeans.trace("(RequestableStep) found value from context: " + Visibility.Logs.printValue(variableVisibility,variableValue));
				}
				
				// Otherwise sequence step default value
				if (variableValue == null) {
					variableValue = getVariableValue(variableName);
					if (variableValue != null)
						Engine.logBeans.trace("(RequestableStep) found default value from step: " + Visibility.Logs.printValue(variableVisibility,variableValue));
				}
				
				// otherwise value not found
				if (variableValue == null) {
					Engine.logBeans.trace("(RequestableStep)  Did not find any value for \""+variableName+"\", ignore it");
				}
				else {
					if (bInternalInvoke) {
						request.put(variableName, variableValue);
					} else {
						String parameterValue;
						if (variableValue instanceof NodeList) {
							NodeList list = (NodeList)variableValue;
							if (list != null) {
								if (list.getLength()==0) { // Specifies here empty multivalued variable (HTTP invoque only)
									postQuery = addParamToPostQuery(variableName, "_empty_array_", postQuery);
								}
								else {
									for (int j=0; j<list.getLength();j++) {
										parameterValue = getNodeValue(list.item(j));
										postQuery = addParamToPostQuery(variableName, parameterValue, postQuery);
									}
								}
							}
						}
						else if (variableValue instanceof NativeJavaArray) {
							Object object = ((NativeJavaArray)variableValue).unwrap();
							List<String> list = GenericUtils.toString(Arrays.asList((Object[])object));
							if (list.size()==0) { // Specifies here empty multivalued variable (HTTP invoque only)
								postQuery = addParamToPostQuery(variableName, "_empty_array_", postQuery);
							} else {
								for (String value : list) {
									postQuery = addParamToPostQuery(variableName, value, postQuery);
								}
							}
						}
						else if (variableValue instanceof Collection<?>) {
							List<String> list = GenericUtils.toString((Collection<?>)variableValue);
							if (list.size()==0) { // Specifies here empty multivalued variable (HTTP invoque only)
								postQuery = addParamToPostQuery(variableName, "_empty_array_", postQuery);
							}
							else {
								for (String value : list) {
									postQuery = addParamToPostQuery(variableName, value, postQuery);
								}
							}
						}
						else if (variableValue instanceof String) {
							parameterValue = variableValue.toString();
							postQuery = addParamToPostQuery(variableName, parameterValue, postQuery);
						}
						else {
							parameterValue = variableValue.toString();
							postQuery = addParamToPostQuery(variableName, parameterValue, postQuery);
						}
					}
				}
			}
			catch(ClassCastException e) {
				Engine.logBeans.warn("(RequestableStep) Ignoring parameter '" + variableName + "' because its value is not a string");
			}
		}
		if (bInternalInvoke) {
			return null;
		}
		else {
			if (Engine.logBeans.isTraceEnabled()) {
				Engine.logBeans.trace("(RequestableStep) postQuery :" + Visibility.Logs.replaceVariables(getVariables(), postQuery));
			}
			return postQuery;
		}
	}

	protected String addParamToPostQuery(String variableName, String parameterValue, String postQuery) {
		if (parameterValue != null) {
			postQuery += ((postQuery.length() != 0) ? "&":"");
			postQuery += variableName + "=" + encodeValue(parameterValue);
		}
		return postQuery;
	}

	protected int doExecuteMethod() throws ConnectionException, URIException, MalformedURLException {
		int statuscode = -1;
		
		if (sequence.runningThread.bContinue) {
			// Tells the method to automatically handle authentication.
			method.setDoAuthentication(true);
			
			// Tells the method to automatically handle redirection.
			method.setFollowRedirects(false);
			
			try {
				displayCookies();
				
				HttpUtils.logCurrentHttpConnection(Engine.theApp.httpClient, hostConfiguration, HttpPool.global);
				Engine.logBeans.debug("(RequestableStep) HttpClient: executing method...");
				statuscode = Engine.theApp.httpClient.executeMethod(hostConfiguration, method, httpState);
				Engine.logBeans.debug("(RequestableStep) HttpClient: end of method successfull");
				
				displayCookies();
				
			}
			catch(IOException e) {
				try {
					HttpUtils.logCurrentHttpConnection(Engine.theApp.httpClient, hostConfiguration, HttpPool.global);
					Engine.logBeans.warn("(RequestableStep) HttpClient: connection error to " + targetUrl + ": " + e.getMessage() + "; retrying method");
					statuscode = Engine.theApp.httpClient.executeMethod(hostConfiguration, method, httpState);
					Engine.logBeans.debug("(RequestableStep) HttpClient: end of method successfull");
				}
				catch(IOException ee) {
					throw new ConnectionException("Connection error to " + targetUrl, ee);
				}
			}
		}
		return statuscode;
	}
	
	protected void displayCookies() {
        // Display the cookies
        Cookie[] cookies = httpState.getCookies();
        if (Engine.logBeans.isTraceEnabled())
        	Engine.logBeans.trace("(RequestableStep) HttpClient cookies:" + Arrays.asList(cookies).toString());
	}

	abstract protected byte[] executeMethod() throws IOException, URIException, MalformedURLException, EngineException;
	
	abstract protected void prepareForRequestable(Context javascriptContext, Scriptable scope) throws MalformedURLException, EngineException;
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.stepExecute(javascriptContext, scope)) {
	            try {
	            	request = new HashMap<String, Object>();
	    			prepareForRequestable(javascriptContext, scope);
	    			
	            	if (bInternalInvoke) {
	            		Engine.logBeans.debug("(RequestableStep) Internal invoke requested");
		            	InternalRequester internalRequester = new InternalRequester();
		            	internalRequester.setStrictMode(getProject().isStrictMode());
		            	
		            	internalRequester.inputData = request;
		        		Object result = internalRequester.processRequest(request);

		        		// MDC log parameters must return to their original values, because
		            	// the internal requester has been executed on the same thread as us.
		            	Log4jHelper.mdcSet(sequence.context.logParameters);

		            	if (result != null) {
			            	xmlHttpDocument = (Document) result;
			            	if (Engine.isStudioMode()) {
			            		((Sequence)sequence.getOriginal()).fireDataChanged(new SequenceEvent(this, result));
			            	}
			            	else {
				            	sequence.fireDataChanged(new SequenceEvent(this, result));
			            	}
			            	flushDocument();
		            	}
	            	}
	            	else {
		            	Engine.logBeans.debug("(RequestableStep) requesting : "+ method.getURI());
		            	byte[] result = executeMethod();
		            	Engine.logBeans.debug("(RequestableStep) Total read bytes: " + ((result != null) ? result.length:0));
		            	if (result != null) {
			            	makeDocument(result);
			            	if (Engine.isStudioMode()) {
			            		((Sequence)sequence.getOriginal()).fireDataChanged(new SequenceEvent(this, result));
			            	}
			            	else {
				            	sequence.fireDataChanged(new SequenceEvent(this, result));			            		
			            	}
			            	flushDocument();
		            	}
	            	}
	            	
	            } catch (Exception e) {
	            	setErrorStatus(true);
	                Engine.logBeans.error("An error occured while invoking transaction step \""+ RequestableStep.this.getName() +"\"", e);
	            } finally {
	            	if (!bInternalInvoke && (method != null))
	            		method.releaseConnection();
	            }
	        	return true;
			}
		}
		return false;
	}
	
	private void flushDocument() throws EngineException {
		if (sequence.runningThread.bContinue) {
			sequence.flushStepDocument(executeTimeID, xmlHttpDocument);
		}
	}

	private void makeDocument(byte[] result) throws Exception {
		if (sequence.runningThread.bContinue) {
			xmlHttpDocument = XMLUtils.parseDOM(new ByteArrayInputStream(result));
			
			// Checks if returned document is a Convertigo error document
			NodeList errors = xmlHttpDocument.getDocumentElement().getElementsByTagName("error");
			if (errors.getLength() > 0) {
				Element error = (Element)errors.item(0);
				if (error.getElementsByTagName("exception").getLength() > 0)
					Engine.logBeans.warn("(SequenceStep) Retrieved document for step '"+getName()+"' ("+executeTimeID+") is in error");
			}
		}
    }
	
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep=super.getAllChildren();
		List<StepVariable> stepVariables=getAllVariables();		
		for(StepVariable stepVariable:stepVariables){
			rep.add(stepVariable);
		}		
		return rep;
	}
	
	@Override
	public XmlSchemaParticle getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);
		String namespace = Project.getProjectTargetNamespace(projectName);
		String localpart = getRequestableName() + "ResponseType";
		element.setSchemaTypeName(new QName(namespace, localpart));
		return element;
	}
	
	public boolean isGenerateElement() {
		return true;
	}
	
	abstract protected String getRequestableName();
}
