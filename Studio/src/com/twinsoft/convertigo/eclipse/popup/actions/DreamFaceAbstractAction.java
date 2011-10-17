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

package com.twinsoft.convertigo.eclipse.popup.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.MashupInformation;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.util.StringEx;

public class DreamFaceAbstractAction extends MyAbstractAction {

	protected static final String requestSufix = "Test";
	protected static final int ALL_DATAVIEW_TYPE 	= 0;
	protected static final int ADD_ACTION_TYPE 		= 1;
	protected static final int UPDATE_ACTION_TYPE 	= 2;
	protected static final int DELETE_ACTION_TYPE 	= 3;
	protected static final int ADD_EVENT_TYPE 		= 4;
	protected static final int UPDATE_EVENT_TYPE	= 5;
	protected static final int DELETE_EVENT_TYPE	= 6;
	
	private HttpClient httpClient = null;
	
	public DreamFaceAbstractAction() {
		super();
	}

	protected boolean securedLogin() {
		GetMethod getMethod = null;
		int statuscode = -1;
		try {
			this.httpClient = new HttpClient();
			String url = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_MASHUP_URL) + "/df/convertigo";
			getMethod = new GetMethod(url);
			statuscode = httpClient.executeMethod(getMethod);
		}
		catch (Exception e) {}
		finally {
			if (getMethod != null) getMethod.releaseConnection();
		}
		return (statuscode != -1);
	}
	
	protected boolean securedLogout() {
		GetMethod getMethod = null;
		int statuscode = -1;
		try {
			if (httpClient != null) {
				String url = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_MASHUP_URL) + "/df/dfe/interface/logout";
				getMethod = new GetMethod(url);
				statuscode = httpClient.executeMethod(getMethod);
				httpClient = null;
			}
		}
		catch (Exception e) {}
		finally {
			if (getMethod != null) getMethod.releaseConnection();
		}
		return (statuscode != -1);
	}

	protected String addClass(Hashtable<String, String> fields) throws HttpException, IOException {
		return executeDfRequest("add",fields);
	}
	protected String updateClass(Hashtable<String, String> fields) throws HttpException, IOException {
		return executeDfRequest("update",fields);
	}
	protected String deleteClass(Hashtable<String, String> fields) throws HttpException, IOException {
		return executeDfRequest("delete",fields);
	}
	
	protected Document getDfClassDocument(String keyword) throws HttpException, IOException, ParserConfigurationException, SAXException {
		Document classDoc = null;
		String sXML = getDfXml("entity=catalog&identifierName=FieldClassName&identifierValue="+ keyword);
		if (!sXML.equals(""))
			return XMLUtils.parseDOM("java", sXML);
		return classDoc;
	}
	
	protected boolean existClass(String keyword) throws HttpException, IOException {
		String sXML = getDfXml("entity=catalog&identifierName=FieldClassName&identifierValue="+ keyword);
		return !(sXML.equals(""));
	}
	
	protected boolean existClassInScreen(String keyword) throws HttpException, IOException {
		String sXML = getDfXml("entity=pages");
		if (sXML.indexOf(keyword) != -1)
			return true;
		return false;
	}
	
	private String executeDfRequest(String keyword, Hashtable<String, String> fields) throws HttpException, IOException {
		PostMethod postMethod = null;
		String dfResponse = "";
		try {
			 //HttpClient client = new HttpClient();
			 String url = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_MASHUP_URL) + "/df/convertigo";
			 postMethod = new PostMethod(url);
			 
			 postMethod.setParameter("dfURI","/df/dfe/interface/"+ keyword +"/class");
			 
			 String fieldName, fieldValue;
			 for (Enumeration<String> e = fields.keys() ; e.hasMoreElements() ;) {
				 fieldName = e.nextElement();
				 fieldValue = fields.get(fieldName);
				 postMethod.setParameter(fieldName,fieldValue);
			 }
			 
			 // Execute the POST method
			 //int statuscode = client.executeMethod(postMethod);
			 int statuscode = httpClient.executeMethod(postMethod);
			 if( statuscode != -1 ) {
				 dfResponse = postMethod.getResponseBodyAsString();
			 }
			 else {
				 dfResponse = postMethod.getResponseBodyAsString();
			 }
			 
			 dfResponse = dfResponse.replaceAll("<dreamface-response>", "");
			 dfResponse = dfResponse.replaceAll("</dreamface-response>", "");
		}
		finally {
			if (postMethod != null)
				postMethod.releaseConnection();
		}
		return dfResponse;
	}
	
	private String getDfXml(String query) throws HttpException, IOException {
		GetMethod getMethod = null;
		String dfResponse = "";
		try {
			 //HttpClient client = new HttpClient();
			 String url = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_MASHUP_URL) + "/df/dfe/interface?"+ query;
			 getMethod = new GetMethod(url);
			 
			 // Execute the GET method
			 //int statuscode = client.executeMethod(getMethod);
			 int statuscode = httpClient.executeMethod(getMethod);
			 if( statuscode != -1 ) {
				 dfResponse = getMethod.getResponseBodyAsString();
			 }
			 else {
				 dfResponse = getMethod.getResponseBodyAsString();
			 }
		}
		finally {
			if (getMethod != null)
				getMethod.releaseConnection();
		}
		return dfResponse.trim();
	}
	
	/************************************************************************************************************/
	/*																											*/
	/************************************************************************************************************/
	protected Hashtable<String, String> getRequestFields(RequestableObject requestable, String dataviewName) {
		DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		
        Project project = requestable.getProject();
		Connector connector = requestable.getConnector();
		
        String connectorName = (connector != null) ? connector.getName():"";
        String projectName = project.getName();
        
        String fieldType = "ConvertigoRequest";
        String fieldCategory = "Convertigo";
        String fieldUser = "John Doe";
        String fieldDateTime = df.format(new Date());
        String fieldDatasource = "<datasource></datasource>";
        String fieldDSParameters = "<datasource-parameters></datasource-parameters>";
        String fieldPersonalization = "<personalization rootelement=\""+ dataviewName +"Preference\"></personalization>";
        
        String fieldParameters = "";
        fieldParameters += "<parameters>";
        fieldParameters += addRequestParameters(requestable);
        fieldParameters += "</parameters>";
        
        String fieldEvents = "<events>";
        fieldEvents += addRequestEvent(requestable);
        fieldEvents += "</events>";
        
        String fieldActions = "<actions></actions>";
        
        Hashtable<String, String> fields = new Hashtable<String, String>(9);
        fields.put("FieldName",dataviewName);
        fields.put("FieldDescription", dataviewName);
        fields.put("FieldCategory",fieldCategory);
        fields.put("FieldType",fieldType);
        fields.put("FieldUser",fieldUser);
        fields.put("FieldDateTime",fieldDateTime);
        fields.put("FieldRank","3");
        fields.put("FieldTags","Convertigo,"+ projectName + (!connectorName.equals("") ? "," + connectorName:""));
        fields.put("FieldIcon","standard.gif");
        //fields.put("FieldParameters",fieldParameters);
        //fields.put("FieldPersonalization", fieldPersonalization);
        //fields.put("FieldEvents", fieldEvents);
        //fields.put("FieldActions", fieldActions);
        //fields.put("FieldDatasource",fieldDatasource);
        //fields.put("FieldDSParameters",fieldDSParameters);
        fields.put("FieldParameters",fieldParameters+fieldPersonalization+fieldEvents+fieldActions+fieldDatasource+fieldDSParameters);
        
        return fields;
		
	}
	
	private String addRequestParameters(RequestableObject requestable) {
		boolean isMultiValued;
		String parameters, variable;
		Object value;
		
		parameters = "<parameter name=\"Variables\">";
		if (requestable instanceof IVariableContainer) {
			IVariableContainer container = (IVariableContainer)requestable;
			int size = container.numberOfVariables();
			for (int i = 0 ; i < size ; i++) {
				RequestableVariable rVariable = (RequestableVariable)container.getVariable(i);
				if (rVariable != null) {
					variable = rVariable.getName();
					value = rVariable.getDefaultValue().toString();
					isMultiValued = rVariable.isMultiValued();
					parameters += "<tvariable name=\""+ variable+"\" defval=\""+ value +"\" multivaluated=\""+ (isMultiValued ? "true":"false") +"\" />";
				}
			}
		}
		parameters += "</parameter>";
		return parameters;
	}
		
	private String addRequestEvent(RequestableObject requestable) {
		String variable, description;
		String variables = "", event = "";
		int size = 0;
		
		if (requestable instanceof IVariableContainer) {
			IVariableContainer container = (IVariableContainer)requestable;
			size = container.numberOfVariables();
			RequestableVariable rVariable;
			for (int i = 0 ; i < size ; i++) {
				rVariable = (RequestableVariable)container.getVariable(i);
				if (rVariable != null) {
					variable = rVariable.getName();
					description = normalizeForSQL(rVariable.getDescription());
					variables += "<variable name=\""+ variable+"\" description=\""+ description +"\"/>";
				}
			}
		}
		
		if (size > 0) {
			event = "<event name=\"FormSubmitted\" description=\"Sent when a Convertigo request is submitted\"><variables>"+ variables + "</variables></event>";
		}
		return event;
	}

	protected String normalizeForSQL(String s) {
		StringEx sx = new StringEx(s);
		sx.replaceAll("\"", "'");
		sx.replaceAll("'", "''");
		return sx.toString();
	}
	
	/************************************************************************************************************/
	/*																											*/
	/************************************************************************************************************/
	protected Hashtable<String, String> getResponseFields(int type, RequestableObject requestable, String dataviewName) throws ParserConfigurationException, SAXException, IOException, DreamFaceInvalidTemplateException {
		Document document = getDfClassDocument(dataviewName);
		if (document == null) document = loadResponseTemplate(dataviewName);
		Element template = (Element)document.getElementsByTagName("template").item(0);
		if (template == null) template = (Element)document.getElementsByTagName("class").item(0);
		Element datasource = (Element)document.getElementsByTagName("datasource").item(0);
		Element dsparameters = (Element)document.getElementsByTagName("datasource-parameters").item(0);
		Element personalization = (Element)document.getElementsByTagName("personalization").item(0);
		Element parameters = (Element)document.getElementsByTagName("parameters").item(0);	
		Element events = (Element)document.getElementsByTagName("events").item(0);
		Element actions = (Element)document.getElementsByTagName("actions").item(0);
		
		if (type == ALL_DATAVIEW_TYPE) {
			Connector connector = requestable.getConnector();
			
	        String connectorName = (connector != null) ? connector.getName():"";
	        String projectName = requestable.getProject().getName();
			String sTags = "Convertigo,"+ projectName + (!connectorName.equals("") ? "," + connectorName:"");
			template.setAttribute("tags", sTags);
	        addResponsePreferences(personalization, requestable);
	        addResponseParameters(parameters, requestable);
	        addResponseEvent(events, null);
	        addResponseAction(actions, requestable);
		}
		else {
			if (parameters.getElementsByTagName("parameter").getLength() <= 0) {
				deleteResponseTemplate(dataviewName);
				throw new DreamFaceInvalidTemplateException("Dataview named \""+ dataviewName +"\" has no more valid template. Please recreate it first.");
			}
			
			if ((type == ADD_ACTION_TYPE) || (type == UPDATE_ACTION_TYPE)) {
				addResponseAction(actions, requestable);
			}
			else if (type == DELETE_ACTION_TYPE) {
				deleteResponseAction(actions, requestable);
			}
			else if ((type == ADD_EVENT_TYPE) || (type == UPDATE_EVENT_TYPE)) {
				addResponseEvent(events, requestable);
			}
			else if (type == DELETE_EVENT_TYPE) {
				deleteResponseEvent(events, requestable);
			}
		}
		
        saveResponseTemplate(document, dataviewName);
        
		DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		
        Hashtable<String, String> fields = new Hashtable<String, String>(9);
        fields.put("FieldName",dataviewName);
        fields.put("FieldDescription",dataviewName);
        fields.put("FieldCategory","Convertigo");
        fields.put("FieldType","ConvertigoResponse");
        fields.put("FieldUser","John Doe");
        fields.put("FieldDateTime",df.format(new Date()));
        fields.put("FieldRank","3");
        fields.put("FieldTags",template.getAttribute("tags"));
        fields.put("FieldIcon","standard.gif");
        //fields.put("FieldParameters",prettyPrintElement(parameters));
        //fields.put("FieldPersonalization", prettyPrintElement(personalization));
        //fields.put("FieldEvents", prettyPrintElement(events));
        //fields.put("FieldActions", prettyPrintElement(actions));
        //fields.put("FieldDatasource",prettyPrintElement(datasource));
        //fields.put("FieldDSParameters",prettyPrintElement(dsparameters));
        
        String fieldParameters = prettyPrintElement(parameters);
        String fieldPersonalization = prettyPrintElement(personalization);
        String fieldEvents = prettyPrintElement(events);
        String fieldActions = prettyPrintElement(actions);
        String fieldDatasource = prettyPrintElement(datasource);
        String fieldDSParameters = prettyPrintElement(dsparameters);
        fields.put("FieldParameters",fieldParameters+fieldPersonalization+fieldEvents+fieldActions+fieldDatasource+fieldDSParameters);
        
        return fields;
	}

	private void addResponseParameters(Element parameters, RequestableObject requestable) {
		
        Document document = parameters.getOwnerDocument();
        
        // Remove all parameters
		while (parameters.hasChildNodes()) {
			Node node = parameters.getLastChild();
			parameters.removeChild(node);
		}
        
		Connector connector = requestable.getConnector();
		
        String requestableName = requestable.getName();
        String connectorName = (connector != null) ? connector.getName():"";
        String projectName = requestable.getProject().getName();
        //int size = (requestable instanceof IVariableContainer) ? ((IVariableContainer)requestable).numberOfVariables():0;

        // project
        Element pProject = document.createElement("parameter");
        pProject.setAttribute("name", "ParamProject");
        pProject.appendChild(document.createTextNode(projectName));
        parameters.appendChild(pProject);
        // connector
        Element pConnector = document.createElement("parameter");
        pConnector.setAttribute("name", "ParamConnector");
        pConnector.appendChild(document.createTextNode(connectorName));
        parameters.appendChild(pConnector);
        // transaction
        Element pTransaction = document.createElement("parameter");
        pTransaction.setAttribute("name", "ParamTransaction");
        pTransaction.appendChild(document.createTextNode((requestable instanceof Transaction) ? requestableName:""));
        parameters.appendChild(pTransaction);
        // sequence
        /*Element pSequence = document.createElement("parameter");
        pSequence.setAttribute("name", "ParamSequence");
        pSequence.appendChild(document.createTextNode((requestable instanceof Sequence) ? requestableName:""));
        parameters.appendChild(pSequence);*/
        // requester
        Element pRequester = document.createElement("parameter");
        pRequester.setAttribute("name", "ParamRequester");
        pRequester.appendChild(document.createTextNode("index.jsp"));
        parameters.appendChild(pRequester);
        // autostart
        Element pAutostart = document.createElement("parameter");
        pAutostart.setAttribute("name", "ParamVariables");
        pAutostart.appendChild(document.createTextNode("0")); // always autostart
        parameters.appendChild(pAutostart);
		
	}

	private void addResponsePreferences(Element personalization, RequestableObject requestable) {
		String variable, description;
		
		// Remove all preferences
		while (personalization.hasChildNodes()) {
			Node node = personalization.getLastChild();
			personalization.removeChild(node);
		}
		
		if (!(requestable instanceof IVariableContainer))
			return;
		
		IVariableContainer container = (IVariableContainer)requestable;
		RequestableVariable rVariable;
		
		Document document = personalization.getOwnerDocument();
		int size = container.numberOfVariables();
		for (int i = 0 ; i < size ; i++) {
			rVariable = (RequestableVariable)container.getVariable(i);
			if (rVariable != null) {
				variable = rVariable.getName();
				description = normalizeForSQL(rVariable.getDescription());
				if (rVariable.isPersonalizable().booleanValue()) {
					Element preference =  document.createElement("preference");
					preference.setAttribute("name", variable);
					preference.setAttribute("label", description);
					preference.setAttribute("type", "text");
					preference.setAttribute("style", "width:150");
					personalization.appendChild(preference);
				}
			}
		}
	}
	
	private void addResponseEvent(Element events, RequestableObject requestable) {
		Document document = events.getOwnerDocument();

		if (requestable == null) {
			// Remove 'ItemClicked' event
			Node node = XMLUtils.findNodeByAttributeValue(events.getElementsByTagName("event"),"name", "ItemClicked");
			if (node != null) events.removeChild(node);
				
			Element event = document.createElement("event");
			event.setAttribute("name", "ItemClicked");
			event.setAttribute("description", "Sent when an hyperlink of a Convertigo response is clicked");
			event.appendChild(document.createElement("variables"));
			events.appendChild(event);
		}
		else {
			String requestableName = requestable.getName().replaceAll(" ", "");
			String eventName = "event_" + requestableName;
			
			// Remove 'event_' event
			Node node = XMLUtils.findNodeByAttributeValue(events.getElementsByTagName("event"),"name", eventName);
			if (node != null) events.removeChild(node);
				
			Element event = document.createElement("event");
			event.setAttribute("name", eventName);
			event.setAttribute("description", "May be sent when a clic occured on a Convertigo response");
			
			Element variables = document.createElement("variables");
			if (requestable instanceof IVariableContainer) {
				IVariableContainer container = (IVariableContainer)requestable;
				
				String varname, description;
				Element variable;
				RequestableVariable rVariable;
				int size = container.numberOfVariables();
				for (int i = 0 ; i < size ; i++) {
					rVariable = (RequestableVariable)container.getVariable(i);
					if (rVariable != null) {
						varname = rVariable.getName();
						description = normalizeForSQL(rVariable.getDescription());
						variable = document.createElement("variable");
						variable.setAttribute("name", varname);
						variable.setAttribute("description", description);
						variables.appendChild(variable);
					}
				}
			}
			event.appendChild(variables);
			events.appendChild(event);
			
		}
	}

	private void deleteResponseEvent(Element events, RequestableObject requestable) {
		String requestableName = requestable.getName().replaceAll(" ", "");
		String eventName = "event_" + requestableName;
		
		// Remove 'event_' event
		Node node = XMLUtils.findNodeByAttributeValue(events.getElementsByTagName("event"),"name", eventName);
		if (node != null) events.removeChild(node);
	}
	
	private void addResponseAction(Element actions, RequestableObject requestable) {
		String variable, description;
		
        String requestableComment = requestable.getComment();
        String requestableName = requestable.getName().replaceAll(" ", "");
		String requestableDescription = normalizeForSQL((requestableComment.equals("") ? requestable.getName():requestableComment));

		Document document = actions.getOwnerDocument();
		
		// Remove action for this requestable
		Node node = XMLUtils.findNodeByAttributeValue(actions.getElementsByTagName("action"),"name", requestableName);
		if (node != null) actions.removeChild(node);
		
		Element action = document.createElement("action");
		action.setAttribute("name", requestableName);
		action.setAttribute("description", requestableDescription);
		action.setAttribute("command", "resources/commands/CallConvertigo.xml");
		
		Element parameters = document.createElement("parameters");
		if (requestable instanceof IVariableContainer) {
			IVariableContainer container = (IVariableContainer)requestable;
			RequestableVariable rVariable;
			Element parameter;
			int size = container.numberOfVariables();
			for (int i = 0 ; i < size ; i++) {
				rVariable = (RequestableVariable)container.getVariable(i);
				if (rVariable != null) {
					variable = rVariable.getName();
					description = normalizeForSQL(rVariable.getDescription());
					parameter = document.createElement("parameter");
					parameter.setAttribute("name", variable);
					parameter.setAttribute("description", description);
					parameters.appendChild(parameter);
				}
			}
		}
		action.appendChild(parameters);
		actions.appendChild(action);
	}
	
	private void deleteResponseAction(Element actions, RequestableObject requestable) {
        String requestableName = requestable.getName().replaceAll(" ", "");
		Node node = XMLUtils.findNodeByAttributeValue(actions.getElementsByTagName("action"),"name", requestableName);
		if (node != null) actions.removeChild(node);
	}
	
	public static final String MASHUP_TEMPLATE_SUBPATH = "/studio/mashup";
	
	private void createResponseTemplate(String dataviewName) throws ParserConfigurationException, IOException {
		String templatePath = Engine.USER_WORKSPACE_PATH + DreamFaceAbstractAction.MASHUP_TEMPLATE_SUBPATH + "/" + dataviewName.trim() + ".xml";

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		Document document = documentBuilder.newDocument();
		
        Element template = document.createElement("template");
        
        document.appendChild(template);
        template.appendChild(document.createElement("datasource"));
        template.appendChild(document.createElement("datasource-parameters"));
        template.appendChild(document.createElement("personalization"));
        template.appendChild(document.createElement("parameters"));
        template.appendChild(document.createElement("events"));
        template.appendChild(document.createElement("actions"));
        
        ((Element)document.getElementsByTagName("personalization").item(0)).setAttribute("rootelement", dataviewName +"Preference");
        
        XMLUtils.saveXml(document, templatePath);
	}
	
	protected void deleteResponseTemplate(String dataviewName) {
		String templatePath = Engine.USER_WORKSPACE_PATH + DreamFaceAbstractAction.MASHUP_TEMPLATE_SUBPATH + "/" + dataviewName.trim() + ".xml";
		File f = new File(templatePath);
		if (f.exists()) {
			f.delete();
		}
	}
	
	private void saveResponseTemplate(Document document, String dataviewName) throws IOException {
		String templatePath = Engine.USER_WORKSPACE_PATH + DreamFaceAbstractAction.MASHUP_TEMPLATE_SUBPATH + "/" + dataviewName.trim() + ".xml";
		XMLUtils.saveXml(document, templatePath);
	}
	
	private Document loadResponseTemplate(String dataviewName) throws ParserConfigurationException, SAXException, IOException {
        File mashupDir = new File(Engine.USER_WORKSPACE_PATH + DreamFaceAbstractAction.MASHUP_TEMPLATE_SUBPATH);
        if (!mashupDir.exists()) {
        	ConvertigoPlugin.logInfo("Creating \"mashup\" directory");
            try {
                mashupDir.mkdirs();
            }
            catch(Exception e) {
                ConvertigoPlugin.logException(e, "Unable to create the convertigo mashup directory");
            }
        }
		
		String templatePath = Engine.USER_WORKSPACE_PATH + DreamFaceAbstractAction.MASHUP_TEMPLATE_SUBPATH + "/" + dataviewName.trim() + ".xml";
		File f = new File(templatePath);
		if (!f.exists()) {
			createResponseTemplate(dataviewName);
		}
			
		return XMLUtils.loadXml(templatePath);
	}

    private String prettyPrintElement(Element element) throws IOException {
        /*OutputFormat format = new OutputFormat(element.getOwnerDocument(), "ISO-8859-1", true);
        format.setMethod("xml");
        format.setLineWidth(0);
        format.setIndenting(false);
        format.setOmitDocumentType(true);
		format.setOmitXMLDeclaration(true);
        StringWriter sw = new StringWriter();
        XMLSerializer serializer = new XMLSerializer(sw, format);
        serializer.serialize(element);
        return sw.toString();
        */
    	
    	String s = XMLUtils.prettyPrintElement(element, true, false);
    	return s;
    }
	
	/************************************************************************************************************/
	/*																											*/
	/************************************************************************************************************/
    protected MashupInformation getMashupInformation(String projectName) {
    	MashupInformation mashupInformation = null;
        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(new FileInputStream(Engine.PROJECTS_PATH + "/" + projectName + "/_private/mashup.ser"));
            mashupInformation = (MashupInformation) objectInputStream.readObject();
        }
        catch(Exception e) {
        	mashupInformation = new MashupInformation();
        }
        return mashupInformation;
    }
	
    protected void storeMashupInformation(MashupInformation mashupInformation, String projectName) {
        File projectDir = new File(Engine.PROJECTS_PATH + "/" + projectName + "/_private");
        if (!projectDir.exists()) {
        	ConvertigoPlugin.logInfo("Creating \"_private\" project directory");
            try {
                projectDir.mkdirs();
            }
            catch(Exception e) {
                String message = java.text.MessageFormat.format(
                    "Unable to create the private project directory \"{0}\"..",
                    new Object[] { ConvertigoPlugin.projectManager.currentProject.getName() }
                );
                ConvertigoPlugin.logException(e, message);
            }
        }

        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(new FileOutputStream(Engine.PROJECTS_PATH + "/" + projectName + "/_private/mashup.ser"));
            objectOutputStream.writeObject(mashupInformation);
            objectOutputStream.flush();
            objectOutputStream.close();
        }
        catch(Exception e) {
        	ConvertigoPlugin.logException(e, "Unable to store mashup information.");
        }
    }
    
}
