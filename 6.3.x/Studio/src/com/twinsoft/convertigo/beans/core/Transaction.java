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

import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XSDExtractor;

/**
 * This is the base interface from a Convertigo transaction. A transaction is
 * an algorithm defining how to produce the XML document required by the Convertigo
 * user.
 */
public abstract class Transaction extends RequestableObject {
    
	private static final long serialVersionUID = 8629312962446057509L;
	
    public static final String EVENT_TRANSACTION_STARTED = "TransactionStarted";
	public static final String EVENT_XML_GENERATED = "XmlGenerated";

	/**
     * Asks the algorithm to cancel the runCore() method.
     */
    public static final String RETURN_CANCEL = "cancel";
    
    public static final int ATTRIBUTE_NAME = 0;
    public static final int ATTRIBUTE_TYPE = 1;
    public static final int ATTRIBUTE_COLUMN = 2;
    public static final int ATTRIBUTE_LINE = 3;
    public static final int ATTRIBUTE_FOREGROUND = 4;
    public static final int ATTRIBUTE_BACKGROUND = 5;
    public static final int ATTRIBUTE_REVERSE = 6;
    public static final int ATTRIBUTE_BLINK = 7;
    public static final int ATTRIBUTE_UNDERLINE = 8;
    public static final int ATTRIBUTE_INTENSE = 9;
    public static final int ATTRIBUTE_OPTIONAL = 10;
    
	/**
	 * The String containing code of ScreenClass handlers.
	 */
	transient public String handlers = "";    
    
    /**
     * Constructs a Transaction object.
     */
    public Transaction() {
        super();
        databaseType = "Transaction";
    }
    
    /**
     * The boolean which specify if Transaction is the one
     * for the project which is learned.
     */
    transient public boolean isLearning = false;
    
    /**
     * Sets the transaction to be the default one.
     */
    public final void markAsLearning(boolean learningMode) throws EngineException {
    	((Connector) parent).setLearningTransaction(learningMode ? this:null);
    }

    /**
     * The boolean which specify if Transaction is the default one
     * for the project to which it belongs.
     */
    transient public boolean isDefault = false;
    
    /**
     * Sets the transaction to be the default one.
     */
    public final void setByDefault() throws EngineException {
        try {
            ((Connector) parent).setDefaultTransaction(this);
        }
        catch(NullPointerException e) {
            throw new EngineException("You should first add this transaction to a project in order to be able to set it by default.");
        }
    }

	public void abort() {
		if (runningThread.bContinue) {
			Engine.logBeans.debug("Transaction '"+ getName() + "' is aborting...");
			runningThread.bContinue = false;
		}
	}
    
	public boolean hasToRunCore() {
		return (context.getConnector() instanceof JavelinConnector 		// if javelin, the test is done in the runCore method => always call the runCore 
				|| !handlerResult.equalsIgnoreCase(RETURN_CANCEL)); // or if transactionStarted handler didn't return cancel 

	}
	
    public void prepareForRequestable(Context context, org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException {
        
    	((Connector) parent).prepareForTransaction(context);

		if ((handlers != null) && (handlers.length() > 0)) {
			Engine.logBeans.trace("(Transaction) Loading handlers:\n" + handlers);

			// Javelin  Transaction have mains in handlers, so insert in scope 
        	insertObjectsInScope();
			
        	try {
				javascriptContext.evaluateString(scope, handlers, getName(), 1, null);
				Engine.logBeans.debug("(Transaction) Handlers main code executed");
        	}
			catch(EcmaError e) {
				EngineException ee = new EngineException(
					"Unable to execute the main Javascript code.\n" +
					"Transaction: \"" + getName() + "\"\n" +
					"A Javascript error has occured at line " + e.lineNumber() + ", column " + e.columnNumber() + ": " +
					e.getMessage() + "\n" + e.lineSource(), e
				);
				throw ee;
			}
			catch(EvaluatorException e) {
				EngineException ee = new EngineException(
					"Unable to execute the main Javascript code.\n" +
					"Transaction: \"" + getName() + "\"\n" +
					"A Javascript evaluation error has occured at line " + e.lineNumber() + ", column " + e.columnNumber() + ": " +
					e.getMessage() + "\n" + e.lineSource(), e
				);
				throw ee;
			}
			catch(JavaScriptException e) {
				EngineException ee = new EngineException(
					"Unable to evaluate the Javascript code of the main Javascript code.\n" +
					"Transaction: \"" + getName() + "\"\n" +
					"A Javascript exception has occured at line " + e.lineNumber() + ": " + e.getMessage(), e
				);
				throw ee;
			}
		} else {
			// HTML Transaction and sequences do not have mains in handlers. 
        	insertObjectsInScope();
		}
    }
    
    public void handleRequestableEvent(String eventType, org.mozilla.javascript.Context javascriptContext) throws EngineException {
    	if (eventType.equalsIgnoreCase(RequestableObject.EVENT_REQUESTABLE_STARTED)) {
    		executeHandler(Transaction.EVENT_TRANSACTION_STARTED, javascriptContext);
    	}
    	else if (eventType.equalsIgnoreCase(RequestableObject.EVENT_REQUESTABLE_XML_GENERATED)) {
    		executeHandler(Transaction.EVENT_XML_GENERATED, javascriptContext);
    	}
    }
    
    public void fireRequestableEvent(String eventType) {
    	if (eventType.equalsIgnoreCase(RequestableObject.EVENT_REQUESTABLE_STARTED)) {
    		Engine.theApp.fireTransactionStarted(new EngineEvent(this));
    	}
    	else if (eventType.equalsIgnoreCase(RequestableObject.EVENT_REQUESTABLE_FINISHED)) {
    		Engine.theApp.fireTransactionFinished(new EngineEvent(this));
    	}
    }
    
	/** Holds value of property includedTagAttributes. */
    private boolean[] includedTagAttributes = new boolean[] { true, true, true, true, true, true, true, true, true, true, true };
    
    /** Getter for property includedTagAttributes.
     * @return Value of property includedTagAttributes.
     */
    public boolean[] getIncludedTagAttributes() {
        return includedTagAttributes;
    }
    
    /** Setter for property includedTagAttributes.
     * @param includedTagAttributes New value of property includedTagAttributes.
     */
    public void setIncludedTagAttributes(boolean[] includedTagAttributes) {
        this.includedTagAttributes = includedTagAttributes;
    }
    
    @Override
    public Element toXml(Document document) throws EngineException {
        Element element = super.toXml(document);
        
        // Storing the transaction "default" flag
        element.setAttribute("default", new Boolean(isDefault).toString());
        
        // Storing the transaction handlers
        try {
            Element handlersElement = document.createElement("handlers");
            if (handlers != null) {
                CDATASection cDATASection = document.createCDATASection(handlers);
                handlersElement.appendChild(cDATASection);
                element.appendChild(handlersElement);
            }
        }
        catch(NullPointerException e) {
            // Silently ignore
        }
        
        return element;
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

		if (VersionUtils.compare(version, "3.0.2") < 0) {
			NodeList properties = element.getElementsByTagName("property");
			Element propValue = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "responseTimeout");

			Node xmlNode = null;
			NodeList nl = propValue.getChildNodes();
			int len_nl = nl.getLength();
			for (int j = 0 ; j < len_nl ; j++) {
				xmlNode = nl.item(j);
				if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
					Long iResponseTimeout = (Long) XMLUtils.readObjectFromXml((Element) xmlNode);
					setResponseTimeout(iResponseTimeout.intValue() / 1000);
					continue;
				}
			}
			
			hasChanged = true;
			Engine.logBeans.warn("(Transaction) The object \"" + getName() + "\" has been updated to version 3.0.2");
		}

		try {
            isDefault = new Boolean(element.getAttribute("default")).booleanValue();
        }
        catch(Exception e) {
            throw new EngineException("Unable to configure the property 'By default' of the transaction \"" + getName() + "\".", e);
        }

        try {
            NodeList childNodes = element.getElementsByTagName("handlers");
            int len = childNodes.getLength();
            if (len > 0) {
                Node childNode = childNodes.item(0);
                Node cdata = XMLUtils.findChildNode(childNode, Node.CDATA_SECTION_NODE);
                if (cdata != null) handlers = cdata.getNodeValue();
            }
        }
        catch(Exception e) {
            throw new EngineException("Unable to configure the handlers of the transaction \"" + getName() + "\".", e);
        }
    }

    @Override
    public Transaction clone() throws CloneNotSupportedException {
        Transaction clonedObject = (Transaction) super.clone();
        return clonedObject;
    }

	transient protected String handlerResult = "";
	transient protected Function function = null;
	transient protected String handlerName = "";

	public void executeHandler(String handlerType, org.mozilla.javascript.Context javascriptContext) throws EngineException {
		handlerName = "n/a";
		handlerResult = "";
		
		if ((handlers == null) || (handlers.length() == 0)) {
			Engine.logBeans.debug("(Transaction) No handlers to execute");
			return;
		}
		
		try {
			executeHandlerCore(handlerType, javascriptContext);
			Engine.logBeans.debug("(Transaction) Handler returned: '" + handlerResult + "'");
		}
		catch(EcmaError e) {
			EngineException ee = new EngineException(
				"Unable to execute the " + handlerType + " handler.\n" +
				"Transaction: \"" + getName() + "\"\n" +
				"Handler name: \"" + handlerName + "\"\n" +
				"A Javascript error has occured at line " + e.lineNumber() + ", column " + e.columnNumber() + ": " + e.getMessage() + "\n" + e.lineSource(), e
			);
			throw ee;
		}
		catch(EvaluatorException e) {
			EngineException ee = new EngineException(
				"Unable to execute the " + handlerType + " handler.\n" +
				"Transaction: \"" + getName() + "\"\n" +
				"Handler name: \"" + handlerName + "\"\n" +
				"A Javascript evaluation error has occured at line " + e.lineNumber() + ", column " + e.columnNumber() + ": " + e.getMessage() + "\n" + e.lineSource(), e
			);
			throw ee;
		}
		catch(JavaScriptException e) {
			EngineException ee = new EngineException(
				"Unable to evaluate the Javascript code of the " + handlerType + " handler.\n" +
				"Transaction: \"" + getName() + "\"\n" +
				"Handler name: \"" + handlerName + "\"\n" +
				"A Javascript exception has occured at line " + e.lineNumber() + ": " + e.getMessage(), e
			);
			throw ee;
		}
	}

	protected void executeHandlerCore(String handlerType, org.mozilla.javascript.Context myJavascriptContext) throws EcmaError, EvaluatorException, JavaScriptException, EngineException {
		if ((!Transaction.EVENT_XML_GENERATED.equals(handlerType)) &&
			(!Transaction.EVENT_TRANSACTION_STARTED.equals(handlerType))) {
			throw new IllegalArgumentException("Unknown handler type: " + handlerType);
		}

		executeSimpleHandlerCore(handlerType, myJavascriptContext);
	}
	
	protected void executeSimpleHandlerCore(String handlerType, org.mozilla.javascript.Context myJavascriptContext) throws EcmaError, EvaluatorException, JavaScriptException, EngineException {
		handlerName = "on" + handlerType;

		Engine.logBeans.trace("(Transaction) Searching the " + handlerType + " handler (" + handlerName + ")");
		Object object = scope.get(handlerName, scope);
		Engine.logBeans.trace("(Transaction) Rhino returned: [" + object.getClass().getName() + "] " + object.toString());
        
		if (!(object instanceof Function)) {
			Engine.logBeans.debug("(Transaction) No " + handlerType + " handler (" + handlerName + ") found");
			return;
		}
		else {
			Engine.logBeans.debug("(Transaction) Execution of the " + handlerType + " handler (" + handlerName + ") for the transaction '" + getName() + "'");
		}

		function = (Function) object;

		Object returnedValue = function.call(myJavascriptContext, scope, scope, null);
		if (returnedValue instanceof org.mozilla.javascript.Undefined) {
			handlerResult = "";
		}
		else {
			handlerResult = returnedValue.toString();
		}
	}

	@Override
	public String getXsdTypePrefix() {
		return getXsdTypePrefix(getParent());
	}
	
	@Override
	public String getXsdTypePrefix(DatabaseObject parentObject) {
		Connector connector = (Connector)parentObject;
		//String connectorPrefix = (connector.isDefault ? "":connector.getName() + "__");
		String prefix = connector.getName() + "__";
		return prefix;
	}

	@Override
	public String getXsdExtractPrefix() {
		Connector connector = (Connector)getParent();
		String connectorPrefix = connector.getName() + "_" + getName() + "_";
		return connectorPrefix;
	}

	@Override
	public String generateXsdArrayOfData() throws Exception {
		String xsdArrayData = "";
		return xsdArrayData;
	}
	
	@Override
	public String generateXsdRequestData() throws Exception {
    	String xsdRequestData = "  <xsd:complexType name=\""+ getXsdTypePrefix() + getName() + "RequestData\"/>\n";
    	return xsdRequestData;
    }
	
	@Override
	protected String generateXsdResponseData(Document document, boolean extract) throws Exception {
    	String xsdResponseData = "  <xsd:complexType name=\""+ getXsdTypePrefix() + getName() + "ResponseData\"/>\n";
    	return xsdResponseData;
    }
	
	@Override
	protected String extractXsdType(Document document) throws Exception {
		if (document == null)
			return "";
		
		String ePrefix = getXsdExtractPrefix();
		Document xsdDom = XSDExtractor.extractXSD(ePrefix, document);
		
		// Add Convertigo error element (Fix #1099)
		Element complex, sequence, error;
		NodeList list = xsdDom.getElementsByTagName("xsd:complexType");
		for (int i=0; i<list.getLength(); i++) {
			complex = (Element)list.item(i);
			if (complex.getAttribute("name").equals(ePrefix+"documentType")) {
				NodeList children = complex.getElementsByTagName("xsd:sequence");
				if (children.getLength()>0) {
					sequence = (Element)children.item(0);
				}
				else {
					sequence = xsdDom.createElement("xsd:sequence");
					complex.appendChild(sequence);
				}
				if (sequence != null) {
					error = xsdDom.createElement("xsd:element");
					error.setAttribute("name", "error");
					error.setAttribute("minOccurs", "0");
					error.setAttribute("maxOccurs", "1");
					error.setAttribute("type", "p_ns:ConvertigoError");
					sequence.appendChild(error);
				}
			}
		}
		
		String tPrefix = getXsdTypePrefix();
		String prettyPrintedText = XMLUtils.prettyPrintDOM(xsdDom);
		int index = prettyPrintedText.indexOf("<xsd:schema>");
		if (index != -1) {
			prettyPrintedText = prettyPrintedText.substring(prettyPrintedText.indexOf("<xsd:schema>") + "<xsd:schema>".length());
			prettyPrintedText = prettyPrintedText.substring(0, prettyPrintedText.indexOf("</xsd:schema>"));
			prettyPrintedText = prettyPrintedText.replaceAll("<xsd:element name=\"document\" type=\"p_ns:"+ePrefix+"documentType\"/>", "");
			prettyPrintedText = prettyPrintedText.replaceAll("<xsd:complexType name=\""+ePrefix+"documentType\">", "<xsd:complexType name=\""+ tPrefix + getName() + "Response\">");
		}
		else {
			prettyPrintedText  = "<xsd:complexType name=\""+ tPrefix + getName() + "Response\">";
			prettyPrintedText += "\t<xsd:sequence>\n";
			prettyPrintedText += "\t\t<xsd:element name=\"error\" minOccurs=\"0\" maxOccurs=\"1\" type=\"p_ns:ConvertigoError\"/>\n";
			prettyPrintedText += "\t</xsd:sequence>\n";
			prettyPrintedText += "</xsd:complexType>\n";
		}
		return prettyPrintedText;
	}
	
	@Override
	protected String getWsdlBackupDir() throws Exception {
		String backupDir = super.getWsdlBackupDir();
		backupDir += "/" + getConnector().getName();
		return backupDir;
	}
	
	@Override
	protected String getWsdlBackupDir(Element element) throws Exception {
		String backupDir = super.getWsdlBackupDir(element);
		
		Element connectorNode = (Element) element.getParentNode();
    	NodeList properties = connectorNode.getElementsByTagName("property");
		Element pName = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "name");
		String connectorName = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(pName, Node.ELEMENT_NODE));
		backupDir += "/" + connectorName;
		return backupDir;
	}
	
	@Override
    public String generateWsdlType(Document document) throws Exception {
    	return extractXsdType(document);
/*    	
        //System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
        //TransformerFactory tFactory = TransformerFactory.newInstance();
    	TransformerFactory tFactory = new org.apache.xalan.xsltc.trax.TransformerFactoryImpl();
        StreamSource streamSource = new StreamSource(new File(Engine.TEMPLATES_DIRECTORY + "/soap/wsdl.xsl").toURI().toASCIIString());
        Transformer transformer = tFactory.newTransformer(streamSource);
        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(sw));

        String s = sw.getBuffer().toString();
        String prettyPrintedText = "";
        
        try {
			String s2 = "<?xml version=\"1.0\" encoding=\""+ getEncodingCharSet() +"\"?>" + s;
			prettyPrintedText = XMLUtils.prettyPrintDOM(s2);

			// Delete first line (xml declaration)
			prettyPrintedText = prettyPrintedText.substring(prettyPrintedText.indexOf("<xsd:"));
        }
        catch(Exception e) {
        	prettyPrintedText = XMLUtils.simplePrettyPrintDOM(s);
        }
		return prettyPrintedText;
*/
    }
}
