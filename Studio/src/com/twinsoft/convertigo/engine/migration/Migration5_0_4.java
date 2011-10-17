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

//import java.com.twinsoft.convertigo.engine.migrationeOutputStream;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;

import java.util.Collection;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class Migration5_0_4 {

	public static Element migrate(Document document, Element projectNode) throws EngineException {
		try {
    		/** variables are migrated to multi valuated variables (New 5.0.4 feature)**/
   			updateVariables(document, projectNode.getChildNodes());
    	}
    	catch (Exception e) {
    		throw new EngineException("[Migration 5.0.4] Unable to migrate project",e);
    	}
		
		return projectNode;
	}
	
    private static void updateVariables(Document document, NodeList children) {
    	String name;
    	Node child;
		for (int i = 0 ; i < children.getLength() ; i++) {
			child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				name = child.getNodeName();
				if ("connector".equals(name)	||
					"sequence".equals(name) 	||
					"transaction".equals(name) 	||
					"step".equals(name) 		||
					"statement".equals(name)	||
					"testcase".equals(name)) {
					
					updateVariables(document, ((Element)child).getChildNodes());
				}
				else if ("variable".equals(name)) {
					try {
						updateVariable(document, (Element) child);
					} catch (Exception e) {
						if (Engine.logDatabaseObjectManager != null)
						Engine.logDatabaseObjectManager.error("[Migration 5.0.4] Unable to migrate variable",e);
					}
				}
			}
		}
    	
    }
	
    private static boolean isMultiValuedVariable(Document document, Element element, String variableName) {
    	if (element == null)
    		return false;
    	
    	//String version = element.getAttribute("version");
    	String elementClassName = element.getAttribute("classname");
    	
    	Class<?> beanClass;
		try {
			beanClass = Class.forName(elementClassName);
			if (com.twinsoft.convertigo.beans.core.Transaction.class.isAssignableFrom(beanClass) ||
				com.twinsoft.convertigo.beans.core.Sequence.class.isAssignableFrom(beanClass)) {
				NodeList nodeList = element.getChildNodes();
		    	String name;
		    	Node child;
				for (int i = 0 ; i < nodeList.getLength() ; i++) {
					child = nodeList.item(i);
					if (child.getNodeType() == Node.ELEMENT_NODE) {
						name = child.getNodeName();
						if ("variable".equals(name)) {
							NodeList children = child.getChildNodes();
							// Retrieve "name" property
							Element nameElement = MigrationUtils.findChildElementByAttributeValue(children, "property", "name");
							Element nameElementNode = (Element) XMLUtils.findChildNode(nameElement, Node.ELEMENT_NODE);
							String elementName = (String)XMLUtils.readObjectFromXml(nameElementNode);
							
							if (variableName.equals(elementName)) {
								if (VersionUtils.compare(((Element)child).getAttribute("version"), "5.0.3") < 0) {
									// Retrieve "multi" property
									Element multiElement = MigrationUtils.findChildElementByAttributeValue(children, "property", "multi");
									if (multiElement != null) {
										Element multiElementNode = (Element) XMLUtils.findChildNode(multiElement, Node.ELEMENT_NODE);
										return ((Boolean)XMLUtils.readObjectFromXml(multiElementNode)).booleanValue();
									}
								}
								else {
									return (((Element)child).getAttribute("classname").indexOf("MultiValuedVariable")!=-1);
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			;
		}
		return false;
    }
    
	private static void updateVariable(Document document, Element element) {
    	String version = element.getAttribute("version");
    	String elementClassName = element.getAttribute("classname");
    	
    	Class<?> beanClass;
		try {
			beanClass = Class.forName(elementClassName);
			
			// Update variable to a multi valuated one if needed
			if (com.twinsoft.convertigo.beans.core.Variable.class.isAssignableFrom(beanClass)) {
				NodeList children = element.getChildNodes();
				
				// Retrieve "name" property
				Element nameElement = MigrationUtils.findChildElementByAttributeValue(children, "property", "name");
				Element nameElementNode = (Element) XMLUtils.findChildNode(nameElement, Node.ELEMENT_NODE);
				String elementName = (String)XMLUtils.readObjectFromXml(nameElementNode);

				if (VersionUtils.compare(version, "5.0.3") < 0) {
					// Retrieve "multi" property
					Boolean isMulti = Boolean.FALSE;
					Element multiElement = MigrationUtils.findChildElementByAttributeValue(children, "property", "multi");
					if (multiElement != null) {
						Element multiElementNode = (Element) XMLUtils.findChildNode(multiElement, Node.ELEMENT_NODE);
						isMulti = (Boolean)XMLUtils.readObjectFromXml(multiElementNode);
					}
					// For Step and TestCase variable : need to retrieve "multi" property from targeted bean
					else {
						Element parent = null, target = null;
						if (com.twinsoft.convertigo.beans.variables.TestCaseVariable.class.isAssignableFrom(beanClass)) {
							target = (Element)element.getParentNode().getParentNode();
							isMulti = isMultiValuedVariable(document, target, elementName);
						}
						else if (com.twinsoft.convertigo.beans.variables.StepVariable.class.isAssignableFrom(beanClass)) {
							Element project = XMLUtils.findSingleElement(document.getDocumentElement(), "project");
							Element projectElement = MigrationUtils.findChildElementByAttributeValue(project.getChildNodes(), "property", "name");
							Element projectElementNode = (Element) XMLUtils.findChildNode(projectElement, Node.ELEMENT_NODE);
							String projectElementName = (String)XMLUtils.readObjectFromXml(projectElementNode);
							
							parent = (Element)element.getParentNode();
							Element targetProjectElement = MigrationUtils.findChildElementByAttributeValue(parent.getChildNodes(), "property", "projectName");
							Element targetProjectElementNode = (Element) XMLUtils.findChildNode(targetProjectElement, Node.ELEMENT_NODE);
							String targetProjectElementName = (String)XMLUtils.readObjectFromXml(targetProjectElementNode);
							
							// check CallStep target same project
							if (targetProjectElementName.equals(projectElementName)) {
								if (parent.getAttribute("classname").indexOf("TransactionStep") != -1) {
									Element targetConnectorElement = MigrationUtils.findChildElementByAttributeValue(parent.getChildNodes(), "property", "connectorName");
									Element targetConnectorElementNode = (Element) XMLUtils.findChildNode(targetConnectorElement, Node.ELEMENT_NODE);
									String targetConnectorElementName = (String)XMLUtils.readObjectFromXml(targetConnectorElementNode);
									
									Element targetTransactionElement = MigrationUtils.findChildElementByAttributeValue(parent.getChildNodes(), "property", "transactionName");
									Element targetTransactionElementNode = (Element) XMLUtils.findChildNode(targetTransactionElement, Node.ELEMENT_NODE);
									String targetTransactionElementName = (String)XMLUtils.readObjectFromXml(targetTransactionElementNode);
									
									target = MigrationUtils.findChildElementByProperty(project.getChildNodes(),"connector", "name", targetConnectorElementName);
									if (target != null) target = MigrationUtils.findChildElementByProperty(target.getChildNodes(),"transaction", "name", targetTransactionElementName);
									isMulti = isMultiValuedVariable(document, target, elementName);
								}
								else if (parent.getAttribute("classname").indexOf("SequenceStep") != -1) {
									Element targetSequenceElement = MigrationUtils.findChildElementByAttributeValue(parent.getChildNodes(), "property", "sequenceName");
									Element targetSequenceElementNode = (Element) XMLUtils.findChildNode(targetSequenceElement, Node.ELEMENT_NODE);
									String targetSequenceElementName = (String)XMLUtils.readObjectFromXml(targetSequenceElementNode);
									
									target = MigrationUtils.findChildElementByProperty(project.getChildNodes(),"sequence", "name", targetSequenceElementName);
									isMulti = isMultiValuedVariable(document, target, elementName);
								}
								
							}
							else {
								// Update isn't automatically possible
							}
						}
					}
					
					if (isMulti) {
						// Change variable class name
						String newVariableClassName = null;
						if (elementClassName.equals("com.twinsoft.convertigo.beans.variables.HttpStatementVariable"))
							newVariableClassName = "com.twinsoft.convertigo.beans.variables.HttpStatementMultiValuedVariable";
						else if (elementClassName.equals("com.twinsoft.convertigo.beans.variables.RequestableHttpVariable"))
							newVariableClassName = "com.twinsoft.convertigo.beans.variables.RequestableHttpMultiValuedVariable";
						else if (elementClassName.equals("com.twinsoft.convertigo.beans.variables.RequestableVariable"))
							newVariableClassName = "com.twinsoft.convertigo.beans.variables.RequestableMultiValuedVariable";
						else if (elementClassName.equals("com.twinsoft.convertigo.beans.variables.TestCaseVariable"))
							newVariableClassName = "com.twinsoft.convertigo.beans.variables.TestCaseMultiValuedVariable";
						else if (elementClassName.equals("com.twinsoft.convertigo.beans.variables.StepVariable"))
							newVariableClassName = "com.twinsoft.convertigo.beans.variables.StepMultiValuedVariable";
						
						if (newVariableClassName != null)
							element.setAttribute("classname", newVariableClassName);
								
						// Change variable value
						Element valueElement = MigrationUtils.findChildElementByAttributeValue(children, "property", "value");
						if (valueElement != null) {
							Element valueElementNode = (Element) XMLUtils.findChildNode(valueElement, Node.ELEMENT_NODE);
							Object object = XMLUtils.readObjectFromXml(valueElementNode);
							Object value = getNewValue(object);
							Element newValueElement = (Element)XMLUtils.writeObjectToXml(document, value);
							valueElement.replaceChild(newValueElement, valueElementNode);
						}
						
						if (Engine.logDatabaseObjectManager != null)
							Engine.logDatabaseObjectManager.info("[Migration 5.0.4] Variable \""+ elementName + "\" ("+ elementClassName +") has been updated to ("+ elementClassName +")");							
					}
				}
				
				element.setAttribute("version", "5.0.3");
				if (Engine.logDatabaseObjectManager != null)
					Engine.logDatabaseObjectManager.warn("[Migration 5.0.4] The object \"" + elementName + "\" has been updated to beans version 5.0.3");
			}
		}
		catch (Exception e) {
			if (Engine.logDatabaseObjectManager != null)
			Engine.logDatabaseObjectManager.error("Unable to update variable", e);
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private static Object getNewValue(Object value) {
		XMLVector<Object> xmlv = new XMLVector<Object>();
		if (value != null) {
			if (value instanceof XMLVector) {
				xmlv = new XMLVector<Object>((XMLVector<Object>)value);
			}
			if (value instanceof Collection) {
				for (Object ob: (Collection<Object>)value) xmlv.add(ob);
			}
			else if (value.getClass().isArray()) {
				for (Object item: (Object[])value) xmlv.add(item);
			}
			else {
				if (!value.equals("")) xmlv.add(value);
			}
		}
		return xmlv;
	}
	
/*
    private static void testMigration(String projectXmlFilePath) throws Exception {
    	if ((projectXmlFilePath != null) && (!projectXmlFilePath.equals(""))) {
    		File f = new File(projectXmlFilePath);
    		if (f.exists()) {
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = documentBuilder.parse(new File(projectXmlFilePath));
        		
                Element rootElement = document.getDocumentElement();

                Element projectNode = (Element) XMLUtils.findChildNode(rootElement, Node.ELEMENT_NODE);

                String engineVersion = ((Element) projectNode).getAttribute("version");

                if (!engineVersion.startsWith("5.0.3")) {
                	projectNode = migrate(document, projectNode);
                }
                
                try {
                	String migratedFileName = projectXmlFilePath.substring(0, projectXmlFilePath.indexOf(".xml")) + "_5.0.3_.xml";
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
			testMigration("C:/Development/SVN/Cems/tomcat/webapps/convertigo/projects/conversion/conversion.xml");
		} catch (Exception e) {
			Engine.logEngine.error("Unexpected exception", e);
		}
	}
*/
}
