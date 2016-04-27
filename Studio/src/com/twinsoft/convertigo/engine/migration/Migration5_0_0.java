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

package com.twinsoft.convertigo.engine.migration;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.transactions.AbstractHttpTransaction;
import com.twinsoft.convertigo.beans.variables.HttpStatementMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.HttpStatementVariable;
import com.twinsoft.convertigo.beans.variables.RequestableHttpMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.beans.variables.RequestableMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.util.StringEx;

/**
 * Variables to Beans migration
 *
 */
public class Migration5_0_0 {

	private static Hashtable<String, String> nameReplacements;
	//private static Hashtable<String, String> xpathReplacements;
	private static ArrayList<Element> wsdltypeContainers;
	private static ArrayList<Element> handlersContainers;
	//private static ArrayList<Element> scStatements;
	private static Element defaultConnector;
	private static Element defaultTransaction;
	
    public static synchronized Element migrate(Document document, Element projectNode) throws EngineException {
    	try {
    		nameReplacements = new Hashtable<String, String>(5);
    		//xpathReplacements = new Hashtable<String, String>(5);
    		wsdltypeContainers = new ArrayList<Element>();
    		handlersContainers = new ArrayList<Element>();
    		//scStatements = new ArrayList<Element>();
    		
    		/** Check project has a default connector, and each connector as a default transaction (see ticket #401) **/
    		checkForDefault(projectNode);
    		
    		/** Normalize bean names (Fix ticket #329) **/ 
    		// Beans should have normalized name, especially for those which are part Convertigo requests
    		// like project, connector, sequence and transaction
    		normalizeNames(projectNode);
    		makeNamesReplacements();
    		
    		/** Change source xpath of step for sequence schema (see ticket #394) **/
    		// If source of step is a TransactionStep, replace ./xxx by ./transaction/document/xxx
    		// If source of step is a SequenceStep, replace ./xxx by ./sequence/document/xxx
    		//makeXpathReplacements(document);
    		
    		/** variablesDefinition property is migrated to real variables (New 4.6.0 feature)**/
   			createVariables(document, projectNode.getChildNodes());
    		
    		nameReplacements.clear();
    		//xpathReplacements.clear();
    		wsdltypeContainers.clear();
    		handlersContainers.clear();
    		//scStatements.clear();
    		defaultConnector = null;
    		defaultTransaction = null;

	    	return projectNode;
    	}
    	catch (Exception e) {
    		throw new EngineException("[Migration 4.6.0] Unable to migrate project",e);
    	}
    }
	
    private static void checkForDefault(Element element) {
    	String elementClassName = element.getAttribute("classname");
    	NodeList children = element.getChildNodes();
    	
    	if (elementClassName.equals("com.twinsoft.convertigo.beans.core.Project")) {
    		defaultConnector = null;
    		checkForDefault(children, "connector");
    		if (defaultConnector == null) {
    			try {    				
	    			((Element)element.getElementsByTagName("connector").item(0)).setAttribute("default", "true");
	    			Engine.logDatabaseObjectManager.warn("[Migration 4.6.0] None default connector was defined for project, first one has been set as default!");
    			}
    			catch (Exception e) {}
    		}
    	}
    	else if (elementClassName.startsWith("com.twinsoft.convertigo.beans.connectors.")) {
    		if (element.getAttribute("default").equals("true")) {
    			defaultConnector = element;
    		}
    		
    		defaultTransaction = null;
    		checkForDefault(children, "transaction");
    		if (defaultTransaction == null) {
    			try {
    				Element propertyName = MigrationUtils.findChildElementByAttributeValue(children, "property", "name");
    		    	String connectorName = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(propertyName, Node.ELEMENT_NODE));
    				
	    			((Element)element.getElementsByTagName("transaction").item(0)).setAttribute("default", "true");
	    			Engine.logDatabaseObjectManager.warn("[Migration 4.6.0] None default transaction was defined for connector \""+connectorName+"\", first one has been set as default!");
    			}
    			catch (Exception e) {}
    		}
    	}
    	else if (elementClassName.startsWith("com.twinsoft.convertigo.beans.transactions.")) {
    		if (element.getAttribute("default").equals("true")) {
    			defaultTransaction = element;
    		}
    	}
    }
    
    private static void normalizeNames(Element element) {
		String elementClassName = element.getAttribute("classname");
    	NodeList children = element.getChildNodes();
    	
    	normalizeProperty(children, elementClassName, "name");
    	
    	
    	if (elementClassName.equals("com.twinsoft.convertigo.beans.core.Project")) {
    		normalizeNames(children, "pool");
    		normalizeNames(children, "connector");
    		normalizeNames(children, "sequence");
    	}
    	else if (elementClassName.startsWith("com.twinsoft.convertigo.beans.sequences.")) {
    		wsdltypeContainers.add(element);
    		normalizeNames(children, "step");
    		normalizeNames(children, "sheet");
    		normalizeNames(children, "variable");
    		
    	}
    	else if (elementClassName.startsWith("com.twinsoft.convertigo.beans.connectors.")) {
    		normalizeProperty(children, elementClassName, "endTransactionName");
    		
    		normalizeNames(children, "pool");
    		normalizeNames(children, "transaction");
    		normalizeNames(children, "screenclass");
   			
    	}
    	else if (elementClassName.startsWith("com.twinsoft.convertigo.core.Pool")) {
    		normalizeProperty(children, elementClassName, "startTransaction");
    		normalizeProperty(children, elementClassName, "initialScreenClass");
    	}
    	else if (elementClassName.startsWith("com.twinsoft.convertigo.beans.transactions.")) {
    		wsdltypeContainers.add(element);
    		if (!elementClassName.equals("com.twinsoft.convertigo.beans.transactions.HtmlTransaction"))
    			handlersContainers.add(element);
    		
    		normalizeNames(children, "statement");
    		normalizeNames(children, "sheet");
    		normalizeNames(children, "variable");
    	}
    	else if (elementClassName.startsWith("com.twinsoft.convertigo.beans.statements.")) {
    		if (elementClassName.startsWith("com.twinsoft.convertigo.beans.statements.Sc")) {
    			//scStatements.add(element);
    			normalizeProperty(children, elementClassName, "normalizedScreenClassName");
    		}
    		
    		normalizeNames(children, "statement");
    		normalizeNames(children, "variable");
    	}
    	else if (elementClassName.startsWith("com.twinsoft.convertigo.beans.steps.")) {
    		wsdltypeContainers.add(element);
    		
    		//String priority = element.getAttribute("priority");
    		
    		if (elementClassName.equals("com.twinsoft.convertigo.beans.steps.TransactionStep")) {
    			//xpathReplacements.put(priority, "transaction");
    			normalizeProperty(children, elementClassName, "projectName");
    			normalizeProperty(children, elementClassName, "connectorName");
    			normalizeProperty(children, elementClassName, "transactionName");
    		}
    		else if (elementClassName.equals("com.twinsoft.convertigo.beans.steps.SequenceStep")) {
    			//xpathReplacements.put(priority, "sequence");
    			normalizeProperty(children, elementClassName, "projectName");
    			normalizeProperty(children, elementClassName, "sequenceName");
    		}
    		
    		normalizeNames(children, "step");
    		normalizeNames(children, "variable");
    	}
    	else if (elementClassName.startsWith("com.twinsoft.convertigo.beans.screenclasses.")) {
    		normalizeNames(children, "criteria");
    		normalizeNames(children, "extractionrule");
    		normalizeNames(children, "sheet");
    		normalizeNames(children, "screenclass");
    	}
    	else if (elementClassName.startsWith("com.twinsoft.convertigo.beans.extractionrules.")) {
    		normalizeProperty(children, elementClassName, "transactionName");
    	}
    	else {
    		;
    	}
    }
    
    private static void makeNamesReplacements() {
    	if (!nameReplacements.isEmpty()) {
    		
    		for (Element element : wsdltypeContainers) {
    			makeWsdltypeReplacements(element);
    		}
    		Engine.logDatabaseObjectManager.info("[Migration 4.6.0] Normalized names replaced in wsdltype");
    		
    		for (Element element : handlersContainers) {
    			makeHandlersReplacements(element);
    		}
    		Engine.logDatabaseObjectManager.info("[Migration 4.6.0] Normalized names replaced in handlers");
    		
    		/*for (Element element : scStatements) {
    			makeScReplacements(element);
    		}*/
    	}
    }
    
    private static void makeWsdltypeReplacements(Element element) {
    	String elementClassName = element.getAttribute("classname");
    	NodeList children = element.getChildNodes();
    	
    	String name = null;
    	try {
        	Element propertyName = MigrationUtils.findChildElementByAttributeValue(children, "property", "name");
	    	name = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(propertyName, Node.ELEMENT_NODE));
    	}
    	catch (Exception e) {}
    	
        try {
            Element wsdltype = MigrationUtils.findChildElementByTagName(children, "wsdltype");
            if (wsdltype != null)
            	makeCdataReplacements(wsdltype);
		} catch (Exception e) {
	        Engine.logDatabaseObjectManager.warn("[Migration 4.6.0] Coud not replace normalized names in wsdltype for \""+name+"\" ("+elementClassName+")");
		}
    }
    
    private static void makeHandlersReplacements(Element element) {
    	String elementClassName = element.getAttribute("classname");
    	NodeList children = element.getChildNodes();
    	
    	String name = null;
    	try {
        	Element propertyName = MigrationUtils.findChildElementByAttributeValue(children, "property", "name");
	    	name = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(propertyName, Node.ELEMENT_NODE));
    	}
    	catch (Exception e) {}
    	
        try {
            Element handlers = MigrationUtils.findChildElementByTagName(children, "handlers");
            if (handlers != null)
            	makeCdataReplacements(handlers);
		} catch (Exception e) {
			Engine.logDatabaseObjectManager.warn("[Migration 4.6.0] Coud not replace normalized names in handlers for \""+name+"\" ("+elementClassName+")");
		}
    }

//    private static void makeScReplacements(Element element) {
//    	String elementClassName = element.getAttribute("classname");
//    	NodeList children = element.getChildNodes();
//    	Element propertyName = MigrationUtils.findChildElementByAttributeValue(children, "property", "name");
//    	Element nameElement = (Element) XMLUtils.findChildNode(propertyName, Node.ELEMENT_NODE);
//    	String name = null;
//    	try {
//	    	name = (String) XMLUtils.readObjectFromXml(nameElement);
//	    	StringEx sx = new StringEx(name);
//	    	
//	    	boolean bChanged = false;
//	        Enumeration<String> keys = nameReplacements.keys();
//	        String key, value;
//	        while (keys.hasMoreElements()) {
//	        	key = keys.nextElement();
//	        	value = nameReplacements.get(key);
//	        	if (name.indexOf(key) != -1) {
//	        		sx.replaceAll(key, value);
//	        		bChanged = true;
//	        	}
//	        }
//	        if (bChanged) {
//	        	String newName = sx.toString();
//    			nameElement.setAttribute("value", newName);
//    			Engine.logDatabaseObjectManager.info("[Migration 4.6.0] ScHandlerStatement \""+name+"\" has been replaced by \""+newName+"\"");
//	        }
//    	}
//    	catch (Exception e) {
//    		Engine.logDatabaseObjectManager.warn("[Migration 4.6.0] Could not change name \""+name+"\" ("+elementClassName+")! Will remain unchanged");
//    	}
//    }
    
    private static void makeCdataReplacements(Element element) throws Exception {
    	try {
            Node cdata = XMLUtils.findChildNode(element, Node.CDATA_SECTION_NODE);
            if (cdata != null) {
                String s = cdata.getNodeValue();
                if (!s.equals("")) {
                    StringEx sx = new StringEx(s);
                    Enumeration<String> keys = nameReplacements.keys();
                    String key, value;
                    while (keys.hasMoreElements()) {
                    	key = keys.nextElement();
                    	value = nameReplacements.get(key);
                    	sx.replaceAll(key, value);
                    }
                    s = sx.toString();
                    cdata.setNodeValue(s);
                }
            }
    	}
    	catch (Exception e) {
    		throw new Exception("Unable to make replacements",e);
    	}
    }

/*
    private static void makeXpathReplacements(Document document) {
    	if (!xpathReplacements.isEmpty()) {
    		
    		for (Element element : wsdltypeContainers) {
    			makeSourceXpathReplacements(document, element);
    		}
    		Engine.logDatabaseObjectManager.info("[Migration 4.6.0] source xpath replaced in steps");
    	}
    }

    private static void makeSourceXpathReplacements(Document document, Element element) {
    	NodeList children = element.getChildNodes();
    	String elementClassName = element.getAttribute("classname");
    	
    	if (elementClassName.startsWith("com.twinsoft.convertigo.beans.steps.")) {
    		String elementName = null;
    		try {
    			Element pName = findChildElementByAttributeValue(children, "property", "name");
        		elementName = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(pName, Node.ELEMENT_NODE));
    			
        		// Case of : IteratorStep, SmtpStep, SourceStep, TestStep, WriteFileStep, XMLAttributeStep, XMLCopyStep, XMLElementStep
        		Element sourceDefinition = findChildElementByAttributeValue(children, "property", "sourceDefinition");
        		if (sourceDefinition != null) {
        			replaceSourceXpath(document, sourceDefinition);
        		}
        		
        		// Case of : XMLgenerateDatesStep
        		Element startDefinition = findChildElementByAttributeValue(children, "property", "startDefinition");
        		if (startDefinition != null) {
        			replaceSourceXpath(document, startDefinition);
        		}
        		Element stopDefinition = findChildElementByAttributeValue(children, "property", "stopDefinition");
        		if (stopDefinition != null) {
        			replaceSourceXpath(document, stopDefinition);
        		}
        		Element daysDefinition = findChildElementByAttributeValue(children, "property", "daysDefinition");
        		if (daysDefinition != null) {
        			replaceSourceXpath(document, daysDefinition);
        		}
    		}
			catch (Exception e) {
				Engine.logDatabaseObjectManager.warn("[Migration 4.6.0] Unable change source xpath for object \""+ elementName + "\" ("+ elementClassName +")", e);
			}
    	}
    }
    
    private static void replaceSourceXpath(Document document, Element definition) throws Exception {
   		Node node = XMLUtils.findChildNode(definition, Node.ELEMENT_NODE);
   		XMLVector xmlv = (XMLVector) XMLUtils.readObjectFromXml((Element) node);
    	if (xmlv.size() > 0) {
			String priority = (String)xmlv.elementAt(0);
			String xpath = (String)xmlv.elementAt(1);
			xpath = xpath.replaceAll("./transaction/document/", "./");
			xpath = xpath.replaceAll("./sequence/document/", "./");
			if (xpathReplacements.containsKey(priority) && (xpath.startsWith("./"))) {
				String newxpath = "./" + xpathReplacements.get(priority) + "/document/" + xpath.substring(2);
				xmlv.setElementAt(newxpath, 1);
				definition.replaceChild(XMLUtils.writeObjectToXml(document, xmlv), node);
			}
    	}
    }
*/    
    private static void normalizeNames(NodeList children, String childTagName) {
    	Node child;
		for (int i = 0 ; i < children.getLength() ; i++) {
			child = children.item(i);
			if ((child.getNodeType() == Node.ELEMENT_NODE) && (childTagName.equals(child.getNodeName()))) {
				normalizeNames((Element) child);
			}
		}
    }

    private static void checkForDefault(NodeList children, String childTagName) {
    	Node child;
		for (int i = 0 ; i < children.getLength() ; i++) {
			child = children.item(i);
			if ((child.getNodeType() == Node.ELEMENT_NODE) && (childTagName.equals(child.getNodeName()))) {
				checkForDefault((Element) child);
			}    	
		}
    }

    private static void normalizeProperty(NodeList children, String elementClassName, String prop) {
		Element propertyProp = MigrationUtils.findChildElementByAttributeValue(children, "property", prop);
    	if (propertyProp != null) {
        	Element propElement = (Element) XMLUtils.findChildNode(propertyProp, Node.ELEMENT_NODE);
        	if (propElement != null) {
            	String propValue = null;
            	try {
        	    	propValue = (String) XMLUtils.readObjectFromXml(propElement);
        	    	if ((propValue!=null) && !propValue.equals("")) {
        		    	String normalizedValue;
        		    	if (!StringUtils.isNormalized(propValue)) {
        		    		normalizedValue = StringUtils.normalize(propValue);
        		    		
        		    		// special case for project (fix ticket #788 : Cannot import project 213.car)
        		    		if (elementClassName.equals("com.twinsoft.convertigo.beans.core.Project")) {
        		    			normalizedValue = "project" + normalizedValue;
        		    		}
        		    		
        		    		//if (!replacements.containsKey(propValue) || !prop.equals("name")) {
        		    			propElement.setAttribute("value", normalizedValue);
        		    			nameReplacements.put(propValue, normalizedValue);
        		    			Engine.logDatabaseObjectManager.warn("[Migration 4.6.0] Non normalized "+prop+" \""+propValue+"\" has been replaced by \""+normalizedValue+"\" ("+elementClassName+")");
        		    		//}
        		    	}
        	    	}
            	}
            	catch (Exception e) {
            		Engine.logDatabaseObjectManager.warn("[Migration 4.6.0] Could not normalize "+prop+" \""+propValue+"\" ("+elementClassName+")! Will remain unchanged");
            	}
        	}
    	}
    }
    
    private static void createVariables(Document document, NodeList children) {
    	String name;
    	Node child;
		for (int i = 0 ; i < children.getLength() ; i++) {
			child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				name = child.getNodeName();
				if ("sequence".equals(name) 	||
					"transaction".equals(name) 	||
					"step".equals(name) 		||
					"statement".equals(name)) {
					try {
						createVariables(document, (Element) child);
					} catch (Exception e) {
						Engine.logDatabaseObjectManager.error("[Migration 4.6.0] Unable to migrate variablesDefinition property to beans",e);
					}
					
					createVariables(document, ((Element)child).getChildNodes());
				}
				else if ("connector".equals(name)) {
					createVariables(document, ((Element)child).getChildNodes());
				}
			}
		}
    	
    }
    
    private static void createVariables(Document document, Element element) throws Exception {
    	Element pName, pVariablesDef;
//    	NodeList properties;
    	XMLVector<XMLVector<?>> xmlv;
    	XMLVector<?> row;
    	XMLVector<Long> ordered;
    	Node node, newChild, newNode;
    	Variable variable;
    	String elementName, elementClassName;
    	
    	// first update element (see preconfigure() and configure())
    	updateElement(element);
    	
    	NodeList children = element.getChildNodes();
    	
		elementClassName = element.getAttribute("classname");
		
		pName = MigrationUtils.findChildElementByAttributeValue(children, "property", "name");
		elementName = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(pName, Node.ELEMENT_NODE));
		
		pVariablesDef = MigrationUtils.findChildElementByAttributeValue(children, "property", "variablesDefinition");
		if (pVariablesDef != null) {
    		node = XMLUtils.findChildNode(pVariablesDef, Node.ELEMENT_NODE);
    		xmlv = GenericUtils.cast(XMLUtils.readObjectFromXml((Element) node));
    		ordered = new XMLVector<Long>();
			for (int j=0; j<xmlv.size(); j++) {
				row = (XMLVector<?>)xmlv.get(j);
				if (!row.isEmpty()) {
    				try {
    					variable = newVariable(elementClassName, row, j);
    					newChild = variable.toXml(document);
    					ordered.add(new Long(variable.priority));
    					element.appendChild(newChild);
    					Engine.logDatabaseObjectManager.info("[Migration 4.6.0] Added variable \""+ variable.getName() +"\" for object \""+ elementName + "\" ("+ elementClassName +")");
    				} catch (EngineException e) {
    					Engine.logDatabaseObjectManager.error("[Migration 4.6.0] Unable to migrate variable defined at row"+ j + " for object \""+ elementName + "\" ("+ elementClassName +")", e);
    				}
				}
				else {
					Engine.logDatabaseObjectManager.info("[Migration 4.6.0] No variable defined for object \""+ elementName + "\" ("+ elementClassName +")");
				}
			}
			
			xmlv = new XMLVector<XMLVector<?>>();
			xmlv.add(ordered);
    		newNode = XMLUtils.writeObjectToXml(document, xmlv);
    		pVariablesDef.replaceChild(newNode, node);
		}
    }
    
    private static void updateElement(Element element) {
    	String version = element.getAttribute("version");
    	String elementClassName = element.getAttribute("classname");
    	
    	Class<?> beanClass;
		try {
			NodeList children = element.getChildNodes();
			
			Element nameElement = MigrationUtils.findChildElementByAttributeValue(children, "property", "name");
			Element nameElementNode = (Element) XMLUtils.findChildNode(nameElement, Node.ELEMENT_NODE);
			String elementName = (String)XMLUtils.readObjectFromXml(nameElementNode);

			beanClass = GenericUtils.cast(Class.forName(elementClassName));

			// Case of Transaction
			if (com.twinsoft.convertigo.beans.core.Transaction.class.isAssignableFrom(beanClass)) {
				
				if (VersionUtils.compare(version, "3.0.2") < 0) {
					Element responseTimeout = MigrationUtils.findChildElementByAttributeValue(children, "property", "responseTimeout");
					if (responseTimeout != null) {
						Element responseTimeoutNode = (Element) XMLUtils.findChildNode(responseTimeout, Node.ELEMENT_NODE);
						Long iResponseTimeout = (Long) XMLUtils.readObjectFromXml(responseTimeoutNode);
						Element newNode = (Element)XMLUtils.writeObjectToXml(element.getOwnerDocument(), iResponseTimeout.intValue() / 1000);
						responseTimeout.replaceChild(newNode, responseTimeoutNode);
					}
					element.setAttribute("version", "3.0.2");
					Engine.logDatabaseObjectManager.warn("[Migration 4.6.0] The object \"" + elementName + "\" has been updated to version 3.0.2");
				}
				
				// Case of TransactionWithVariables
				if (com.twinsoft.convertigo.beans.core.TransactionWithVariables.class.isAssignableFrom(beanClass)) {
					Element variablesDefinition = MigrationUtils.findChildElementByAttributeValue(children, "property", "variables");
					if (variablesDefinition == null) {
						variablesDefinition = MigrationUtils.findChildElementByAttributeValue(children, "property", "variablesDefinition");
					}
					
					if (variablesDefinition != null) {
						Element variablesDefinitionNode = (Element) XMLUtils.findChildNode(variablesDefinition, Node.ELEMENT_NODE);
						XMLVector<XMLVector<Object>> xmlv = GenericUtils.cast(XMLUtils.readObjectFromXml(variablesDefinitionNode));
	
						if (VersionUtils.compare(version, "3.1.8") < 0) {
							// Case of HttpTransaction
							if (AbstractHttpTransaction.class.isAssignableFrom(beanClass)) {
								Element httpVariables = MigrationUtils.findChildElementByAttributeValue(children, "property", "httpVariables");
								if (httpVariables != null) {
									Element httpVariablesNode = (Element) XMLUtils.findChildNode(httpVariables, Node.ELEMENT_NODE);
									XMLVector<XMLVector<Object>> xmlv1 = GenericUtils.cast(XMLUtils.readObjectFromXml(httpVariablesNode));
									
									/*Node xmlNode = null;
									NodeList nl = propValue.getChildNodes();
									int len_nl = nl.getLength();
									for (int j = 0 ; j < len_nl ; j++) {
										xmlNode = nl.item(j);
										if (xmlNode.getNodeType() == Node.ELEMENT_NODE) {
											httpVariables = (XMLVector) XMLUtils.readObjectFromXml((Element) xmlNode);
											continue;
										}
									}*/
									
									int len = xmlv.size();
									for (int i = 0 ; i < len ; i++) {
										XMLVector<Object> line = xmlv.get(i);
										if (xmlv1.size()>0) {
											line.add(xmlv1.get(i).get(1));
											line.add(xmlv1.get(i).get(2));
										}
									}
								}
							}
							
							int len = xmlv.size();
							for (int i = 0 ; i < len ; i++) {
								XMLVector<Object> line = xmlv.get(i);
								if (line.size()>0) {
									// Include in WSDL by default
									line.add(Boolean.TRUE);
								}
							}
							element.setAttribute("version", "3.1.8");
							Engine.logDatabaseObjectManager.warn("[Migration 4.6.0] The object \"" + elementName + "\" has been updated to version 3.1.8");
						}
						
						if (VersionUtils.compare(version, "3.2.4") < 0) {
							int len = xmlv.size();
							for (int i = 0 ; i < len ; i++) {
								XMLVector<Object> line = xmlv.get(i);
								if (line.size()>0) {
									// Defaults to non multivalued variable
									line.insertElementAt(Boolean.FALSE, 3);
								}
							}
							
							element.setAttribute("version", "3.2.4");
							Engine.logDatabaseObjectManager.warn("[Migration 4.6.0] The object \"" + elementName + "\" has been updated to version 3.2.4");
						}
						
						if (VersionUtils.compare(version, "4.1.5") < 0) {
							// Case of JavelinTransaction
							if (com.twinsoft.convertigo.beans.transactions.JavelinTransaction.class.isAssignableFrom(beanClass)) {
								Element propValue = MigrationUtils.findChildElementByAttributeValue(children, "property", "executeExtractionRulesInPanels");
								if (propValue == null) {
									Document document = element.getOwnerDocument();
									propValue = document.createElement("property");
									propValue.setAttribute("name", "executeExtractionRulesInPanels");
									propValue.appendChild((Element)XMLUtils.writeObjectToXml(document, "false"));
								}
								
								element.setAttribute("version", "4.1.5");
								Engine.logDatabaseObjectManager.warn("[Migration 4.6.0] The object \"" + elementName + "\" has been updated to version 4.1.5");
							}
						}
						
				    	if (VersionUtils.compare(version, "4.2.0") < 0) {
							int len = xmlv.size();
							for (int i = 0 ; i < len ; i++) {
								XMLVector<Object> line = xmlv.get(i);
								if (line.size()>0) {
									// Do not set as Personalizable by default
									line.insertElementAt(Boolean.FALSE, 4);
									// Sets description to variable name by default
									line.insertElementAt(line.get(0), 1);
								}
							}
								
							element.setAttribute("version", "4.2.0");
							Engine.logDatabaseObjectManager.warn("[Migration 4.6.0] The object \"" + elementName + "\" has been updated to version 4.2.0");
				    	}
				    	
						if (VersionUtils.compare(version, "4.3.0") < 0) {
							int len = xmlv.size();
							for (int i = 0 ; i < len ; i++) {
								XMLVector<Object> line = xmlv.get(i);
								if (line.size()>0) {
									// Set cached key
									line.insertElementAt(Boolean.TRUE, 6);
								}
							}
							
							element.setAttribute("version", "4.3.0");
							Engine.logDatabaseObjectManager.warn("[Migration 4.6.0] The object \"" + elementName + "\" has been updated to version 4.3.0");
						}
						
			    		Element newNode = (Element)XMLUtils.writeObjectToXml(element.getOwnerDocument(), xmlv);
			    		variablesDefinition.replaceChild(newNode, variablesDefinitionNode);
			    		
			    		variablesDefinition.setAttribute("name", "variablesDefinition");
					}
				}
			}
			// Case of HTTPStatement
			else if (com.twinsoft.convertigo.beans.statements.HTTPStatement.class.isAssignableFrom(beanClass)) {
				if (VersionUtils.compare(version, "4.2.0") < 0) {
					Element variablesDefinition = MigrationUtils.findChildElementByAttributeValue(children, "property", "data");
					if (variablesDefinition != null) {
						Element variablesDefinitionNode = (Element) XMLUtils.findChildNode(variablesDefinition, Node.ELEMENT_NODE);
						XMLVector<XMLVector<Object>> xmlv = GenericUtils.cast(XMLUtils.readObjectFromXml(variablesDefinitionNode));
						
						int len = xmlv.size();
						for (int i = 0 ; i < len ; i++) {
							XMLVector<String> line = GenericUtils.cast(xmlv.get(i));
							if (line.size()>0) {
								// Sets empty description by default
								line.add(1 ,"");
							}
						}
						
			    		Element newNode = (Element)XMLUtils.writeObjectToXml(element.getOwnerDocument(), xmlv);
			    		variablesDefinition.replaceChild(newNode, variablesDefinitionNode);

						variablesDefinition.setAttribute("name", "variablesDefinition");
					}
					
					Element requestUri = MigrationUtils.findChildElementByAttributeValue(children, "property", "requestUri");
					if (requestUri != null) {
						Element requestUriNode = (Element) XMLUtils.findChildNode(requestUri, Node.ELEMENT_NODE);
						String requestUriValue = (String)XMLUtils.readObjectFromXml(requestUriNode);
						Element newNode = (Element)XMLUtils.writeObjectToXml(element.getOwnerDocument(), "'" + requestUriValue + "'");
						requestUri.replaceChild(newNode, requestUriNode);
					}
					
					element.setAttribute("version", "4.2.0");
					Engine.logDatabaseObjectManager.warn("[Migration 4.6.0] The object \"" + elementName + "\" has been updated to version 4.2.0");
				}
			}
		}
		catch (Exception e) {
			Engine.logDatabaseObjectManager.error("Unable to update element", e);
		}
    	
    }
    
    private static Variable newVariable(String classname, XMLVector<?> xmlv, int index) throws EngineException {
    	Class<? extends DatabaseObject> beanClass;
		try {
			beanClass = GenericUtils.cast(Class.forName(classname));
			
	    	if (AbstractHttpTransaction.class.isAssignableFrom(beanClass)) {
	    		Boolean isMulti = (Boolean)xmlv.get(4);
	    		RequestableHttpVariable variable;
	    		variable = (isMulti ? new RequestableHttpMultiValuedVariable():new RequestableHttpVariable());
				variable.setName((String)xmlv.get(0));
				variable.setDescription((String)xmlv.get(1));
				variable.setValueOrNull(xmlv.get(2));
				variable.setWsdl(((Boolean)xmlv.get(3)));
				variable.setPersonalizable(((Boolean)xmlv.get(5)));
				variable.setCachedKey(((Boolean)xmlv.get(6)));
				variable.setHttpMethod((String)xmlv.get(7));
				variable.setHttpName((String)xmlv.get(8));
				variable.bNew = true;
				variable.hasChanged = true;
				return variable;
			}
	    	else if (TransactionWithVariables.class.isAssignableFrom(beanClass) ||
	    			Sequence.class.isAssignableFrom(beanClass)) {
	    		Boolean isMulti = (Boolean)xmlv.get(4);
				RequestableVariable variable = (isMulti ? new RequestableMultiValuedVariable():new RequestableVariable());
				variable.setName((String)xmlv.get(0));
				variable.setDescription((String)xmlv.get(1));
				variable.setValueOrNull(xmlv.get(2));
				variable.setWsdl(((Boolean)xmlv.get(3)));
				variable.setPersonalizable(((Boolean)xmlv.get(5)));
				variable.setCachedKey(((Boolean)xmlv.get(6)));
				variable.bNew = true;
				variable.hasChanged = true;
				return variable;
	    	}
	    	else if (RequestableStep.class.isAssignableFrom(beanClass)) {
				StepVariable variable = new StepVariable();
				variable.setName((String)xmlv.get(0));
				variable.setDescription((String)xmlv.get(1));
				variable.setSourceDefinition(GenericUtils.<XMLVector<String>>cast(xmlv.get(2)));
				variable.setValueOrNull(xmlv.get(3));
				variable.bNew = true;
				variable.hasChanged = true;
				return variable;
	    	}
	    	else if (com.twinsoft.convertigo.beans.statements.HTTPStatement.class.isAssignableFrom(beanClass)) {
	    		Boolean isMulti = (Boolean)xmlv.get(3);
	    		HttpStatementVariable variable;
	    		variable = (isMulti ? new HttpStatementMultiValuedVariable():new HttpStatementVariable());
				try {
					variable.setName((String)xmlv.get(0));
				}
				catch (Exception e) {
					variable.setName("variable"+index);
					Engine.logDatabaseObjectManager.warn("[Migration 4.6.0] For variable at index "+index+", empty name has been replaced by 'variable"+index+"'!");
				}
				variable.setDescription((String)xmlv.get(1));
				variable.setValueOrNull(xmlv.get(2));
				variable.setHttpMethod((String)xmlv.get(4));
				variable.setHttpName((String)xmlv.get(5));
				variable.bNew = true;
				variable.hasChanged = true;
				return variable;
	    	}
	    	else {
	    		throw new EngineException("[Migration 4.6.0] Unsupported classname \""+ classname +"\"");
	    	}
			
		} catch (Exception e) {
			throw new EngineException("[Migration 4.6.0] Unable to create variable bean", e);
		}
		
    }

/*
    private static void testMigration(String projectXmlFilePath) throws Exception {
    	if ((projectXmlFilePath != null) && (!projectXmlFilePath.equals(""))) {
    		File f = new File(projectXmlFilePath);
    		if (f.exists()) {
                Document document = XMLUtils.getDefaultDocumentBuilder().parse(new File(projectXmlFilePath));
        		
                Element rootElement = document.getDocumentElement();

                Element projectNode = (Element) XMLUtils.findChildNode(rootElement, Node.ELEMENT_NODE);

                String engineVersion = ((Element) projectNode).getAttribute("version");

                if (!engineVersion.startsWith("4.6")) {
                	projectNode = migrate(document, projectNode);
                }
                
                try {
                	String migratedFileName = projectXmlFilePath.substring(0, projectXmlFilePath.indexOf(".xml")) + "_4.6.0_.xml";
                    FileOutputStream fos = new FileOutputStream(migratedFileName);
                    String s = XMLUtils.prettyPrintDOM(document);
                    fos.write(s.getBytes("ISO-8859-1"));
                    fos.close();
                }
                catch(Exception e) {
					Engine.logEngine.error("Unexpected exception", e);
                }
    		}
    	}
    }
    
	public static void main(String[] args) {
		try {
			testMigration("C:/Development/SVN/Convertigo4/tomcat/webapps/convertigo/projects/SalesForceDemo/SalesForceDemo.xml");
		} catch (Exception e) {
			Engine.logEngine.error("Unexpected exception", e);
		}
	}
*/
}
