/*
 * Copyright (c) 2001-2012 Convertigo SA.
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

package com.twinsoft.convertigo.engine.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.variables.HttpStatementVariable;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.engine.servlets.WebServiceServlet;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.SOAPUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public enum Visibility {
	Logs(0x01),		// for log files
	Studio(0x02),	// for Studio UI
	Platform(0x04),	// for Platform UI
	XmlFile(0x08);	// for project's XML files
	
	int mask;
	Visibility(int mask) {
		this.mask = mask;
	}
	
	public static final String STRING_MASK = "******";
	
	public int getMask() {
		return mask;
	}
	
	public boolean isMasked(int visibility) {
		return (visibility & mask) != 0 ? true : false;
	}
	
	public String printValue(int visibility, Object value) {
		if (value == null) return null;
		return (visibility & mask) == 0 ? toString(value) : maskValue(value);
	}
	
	private String toString(Object object) {
		String s = null;
		if (object != null) {
			if (object instanceof NodeList) {
				NodeList list = (NodeList)object;
				s = list.toString();
			}
			else if (object instanceof Collection<?>) {
				List<String> list = GenericUtils.toString((Collection<?>)object);
				s = list.toString();
			}
			else if (object instanceof NativeJavaArray) {
				Object ob = ((NativeJavaArray)object).unwrap();
				List<String> list = GenericUtils.toString(Arrays.asList((Object[])ob));
				s = list.toString();
			}
			else if (object instanceof NativeArray) {
				s = (String)((NativeArray)object).getDefaultValue(String.class);
			}
			else if (object instanceof NativeJavaObject) {
				s = (String)((NativeJavaObject)object).getDefaultValue(String.class);
			}
			else if (object.getClass().isArray()) {
				s = Arrays.toString((Object[])object);
			}
			else
				s = object.toString();
		}
		return s;
	}
	
	public Object replaceValues(List<String> hiddenValues, Object object) {
		if (object == null) return null;
		if (hiddenValues == null) return object;
		if (hiddenValues.isEmpty()) return object;
		
		try {
			// Case of postQuery from XML requestTemplate
			if (object instanceof String) {
				String toPrint = String.valueOf(object);
				for (String value: hiddenValues) {
					if (value != null && !value.equals(""))
						toPrint = toPrint.replaceAll(value, STRING_MASK);
				}
				return toPrint;
			}
		}
		catch (Exception e) {}
		return object;
	}
	
	public Object replaceVariables(List<? extends Variable> variableList, Object object) {
		if (object == null) return null;
		if (variableList == null) return object;
		
		try {
			// Case of queryString | postQuery : variable_name=variable_value&variable_name=variable_value
			if (object instanceof String) {
				String toPrint = String.valueOf(object);
				for (Variable variable: variableList) {
					if (variable != null && isMasked(variable.getVisibility())) {
						for (String key : getVariableKeyNames(variable))
							toPrint = toPrint.replaceAll(key+"=[^\\&]*", key+"=\""+ STRING_MASK +"\"");
					}
				}
				return toPrint;
			}
			// Case of soapEnvelope (jmc 25/05/2012)
			if (object instanceof HttpServletRequest) {				
				SOAPMessage requestMessage = (SOAPMessage)((HttpServletRequest)object).getAttribute(WebServiceServlet.REQUEST_MESSAGE_ATTRIBUTE);				
				
				SOAPPart sp = requestMessage.getSOAPPart();
				SOAPEnvelope se = sp.getEnvelope();
				SOAPBody sb = se.getBody();
								
				SOAPElement method, parameter;
				Iterator<?> iterator = sb.getChildElements();
				Object element;
				
				while (iterator.hasNext()) {
					element = iterator.next();
					if (element instanceof SOAPElement) {
						method = (SOAPElement) element;
						Iterator<?> iterator2 = method.getChildElements();
						String parameterName, parameterValue;

						while (iterator2.hasNext()) {
							element = iterator2.next();
							if (element instanceof SOAPElement) {
								parameter = (SOAPElement) element;
								parameterName = parameter.getElementName().getLocalName();
								parameterValue = parameter.getValue();
								if (parameterValue == null) parameterValue = "";
								for (Variable variable: variableList) {
									if (variable != null && isMasked(variable.getVisibility())) {
										for (String key : getVariableKeyNames(variable)) {
											if (parameterName.equals(key) ) {
												
												//We delete parent node and re-create to permit to remove all child nodes
												if (parameter.hasChildNodes()) {
													SOAPElement parent = parameter.getParentElement();
													parent.removeChild(parameter);
													parameter = parent.addChildElement(parameterName);
												} 
												
												parameter.setValue(STRING_MASK);
												
											}
										}
									}
								}									
							}
						}
					}
				}
				
				return SOAPUtils.toString(requestMessage, ((HttpServletRequest)object).getCharacterEncoding());
			}
			
			// Case of variables Map : {variable_name,variable_value}
			if (object instanceof Map<?, ?>) {
				Map<String, Object> toPrint = GenericUtils.cast(GenericUtils.clone(object));
				for (Variable variable: variableList) {
					if (variable != null && isMasked(variable.getVisibility())) {
						for (String key : getVariableKeyNames(variable)) {
							if (toPrint.get(key) != null)
								toPrint.put(key, STRING_MASK);
						}
					}
				}
				return toPrint;
			}
			// Case of Document : inputDocument | XSL requestTemplate
			if (object instanceof Document) {
				Document toPrint = XMLUtils.createDom("java");
				toPrint.appendChild(toPrint.importNode(((Document)object).getDocumentElement(), true));
				NodeList variableNodeList = toPrint.getElementsByTagName("variable");
				
				Element variableElement;
				NodeList valueNodeList;
				Attr valueAttrNode;
				String variableName;
				Node node;
				Element firstElement = (Element) variableNodeList.item(0);
				//Permit to identify if we have an input document or a XSL requestTemplate
				
				boolean isInputDoc = false;
				if (firstElement.getParentNode() != null && firstElement.getParentNode().getNodeName().equals("transaction-variables")){
					if (firstElement.getParentNode().getParentNode() != null && firstElement.getParentNode().getParentNode().getNodeName().equals("input")) {
						isInputDoc = true;
					}
				}
				
				for (int i=0; i<variableNodeList.getLength(); i++) {
					variableElement = (Element) variableNodeList.item(i);
					variableName = variableElement.getAttribute("name");
					valueAttrNode = variableElement.getAttributeNode("value");
					valueNodeList = variableElement.getChildNodes();
					for (Variable variable: variableList) {
						if (variable != null && isMasked(variable.getVisibility())) {
							for (String key : getVariableKeyNames(variable)) {
								if (variableName.equals(key)) {
									// inputDocument with attribute value
									if (valueAttrNode != null) {
										valueAttrNode.setNodeValue(STRING_MASK);
									}
									// inputDocument without attribute value but with child nodes | XSL requestTemplate
									else if (valueNodeList != null) {
										if (isInputDoc) {
											//Loop we permit to remove all childs
											for (Node child; (child = variableElement.getFirstChild()) != null; variableElement.removeChild(child));
											variableElement.setTextContent(STRING_MASK);
										} else {
											for (int j=0; j<valueNodeList.getLength(); j++) {
												node = valueNodeList.item(j);
												if (node.getNodeType() == Node.ELEMENT_NODE && 
													((Element)node).getNodeName().equals("value")) {
														((Element)node).setTextContent(STRING_MASK);
												}
											}
										}										
									}
								}
							}
						}
					}
				}
				return toPrint;
			}
		}
		catch (Exception e) {}
		return object;
	}
	
	public static String maskValue(Object value) {
		if (value == null) return null;
		if (value.equals("")) return "";
		String regexp = value instanceof String ? ".":"[^\\[\\]\\,]";
		try {
			return value.toString().replaceAll(regexp, "*");
		} catch (Exception e) {}
		return STRING_MASK;
	}
	
	private static List<String> getVariableKeyNames(Variable variable) {
		List<String> keys = new ArrayList<String>();
		try {
			keys.add(variable.getName());
			Class<?> c = variable.getClass();
			if (c.getName().indexOf(".RequestableHttp") != -1 || c.getName().indexOf(".HttpStatement") != -1) {
				//java.lang.reflect.Method method = c.getMethod("getHttpName");
				//String key = (String)method.invoke(c);
				String key = "";
				if (variable instanceof RequestableHttpVariable) {
					key = ((RequestableHttpVariable) variable).getHttpName();
				} else {
					key = ((HttpStatementVariable) variable).getHttpName();
				}
				if (!keys.contains(key)) keys.add(key);
			}
		} catch (Exception e) {}
		return keys;
	}
}
