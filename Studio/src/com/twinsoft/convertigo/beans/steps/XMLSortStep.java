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
 * You should have received a sort of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/beans/steps/XMLSortStep.java $
 * $Author: julienda $
 * $Revision: 32580 $
 * $Date: 2012-11-16 13:28:28 +0100 (lun., 10 juin 2013) $
 */

package com.twinsoft.convertigo.beans.steps;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.xpath.CachedXPathAPI;
import org.apache.xpath.objects.XObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class XMLSortStep extends XMLCopyStep implements IStepSourceContainer {

	private static final long serialVersionUID = 4871778624030668414L;
	
	/* COMBO BOX */
	public enum Order {
		Ascending,
		Descending
	}
	
	public enum TypeOrder {
		Number,
		Date,
		String
	}
	
	/* PROPERTIES */
	private XMLVector<String> sourceDefinition = new XMLVector<String>();
	private String sortXPATHDefinition = "";
	private String optionSort = "";
	private Order orderSort = Order.Ascending;
	private TypeOrder typeSort = TypeOrder.String;
	
	public XMLSortStep() {
		super();
		setOutput(true);
		this.xml = true;
	}

	@Override
    public XMLSortStep clone() throws CloneNotSupportedException {
    	XMLSortStep clonedObject = (XMLSortStep) super.clone();
        return clonedObject;
    }

	@Override
    public XMLSortStep copy() throws CloneNotSupportedException {
    	XMLSortStep copiedObject = (XMLSortStep) super.copy();
        return copiedObject;
    }

	@Override
	public String toString() {
		String label = "";
		try {
			label += (sourceDefinition.size() > 0) ? " @("+ getLabel()+")":"";
		} catch (EngineException e) {
		}
		return "sortOf" + label;
	}
	
	@Override
	public String toJsString() {
		return optionSort;
	}
	
	public XMLVector<String> getSourceDefinition() {
		return sourceDefinition;
	}

	public void setSourceDefinition(XMLVector<String> sourceDefinition) {
		this.sourceDefinition = sourceDefinition;
	}
	
	public String getSortXPATHDefinition() {
		return sortXPATHDefinition;
	}

	public void setSortXPATHDefinition(String sortXPATHDefinition) {
		this.sortXPATHDefinition = sortXPATHDefinition;
	}

	public Order getOrderSort() {
		return orderSort;
	}
	
	public void setOrderSort(Order orderSort) {
		this.orderSort = orderSort;
	}
	
	public TypeOrder getTypeSort() {
		return typeSort;
	}
	
	public void setTypeSort(TypeOrder typeSort) {
		this.typeSort = typeSort;
	}
	
	public String getOptionSort() {
		return optionSort;
	}

	public void setOptionSort(String optionSort) {
		this.optionSort = optionSort;
	}

	@Override
	public NodeList getContextValues() throws EngineException {
		NodeList list = super.getContextValues();
		
		if (list != null) {
			
			final CachedXPathAPI cachedXPathAPI = new CachedXPathAPI();
			
			// Convert the NodeList to ArrayList
			List<Node> nodes = XMLUtils.toArrayList(list);
			
			// Comparator which permit to sort
			Comparator<Node> comparator =  new Comparator<Node>() {
				public int compare(Node n1, Node n2) {
					String s1 = "", s2 = "";
					String xpathExpression = getSortXPATHDefinition();
					
					s1 = getNodeValue(n1, xpathExpression);
					s2 = getNodeValue(n2, xpathExpression);
				
					// CASE: NUMBER
					if (typeSort == TypeOrder.Number) {
						return Double.compare(getDoubleValue(s1), getDoubleValue(s2));
					}
					// CASE: DATE	
					else if (typeSort == TypeOrder.Date) {
						Date d1 = null, d2 = null;
						DateFormat dateFormat;
						
						if ( evaluated != null && !evaluated.equals("") ){
							dateFormat = new SimpleDateFormat( evaluated.toString() );
						} else {
							dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
						}
						dateFormat.setLenient(false);
						
						try {
							d1 = dateFormat.parse(s1);
							d2 = dateFormat.parse(s2);
						} catch (Exception e) {
							throw new RuntimeException("Error when parsing Date: \n"+e.getMessage());
						}

						return d1.compareTo(d2);
					}
					// CASE: STRING
					else {
						return s1.compareTo(s2);
					}
				}
				
				// Get the value of the node in function of the XPATH Expression
				private String getNodeValue(Node n, String xpathExpression){
					try {
						XObject xo = cachedXPathAPI.eval(n, xpathExpression);
						return xo.toString();
					} catch (TransformerException e) {
						throw new RuntimeException( "Error during sorting of the XML document: "+e.getMessage() );
					}
				}
				
				// Convert the "string" to "double"
				private double getDoubleValue(String str){
					str = str.replaceAll(".*?(-?[\\d]+(?:\\.|,)?[\\d]*).*", "$1");
					
					try {
						return Double.parseDouble( str.replace(",", ".") );
					} catch (NumberFormatException e) {
						throw new RuntimeException( "Error during parsing double: \n"+e.getMessage());
					}
				}
			};
			
			// We sort the ArrayList in mode ascending or descending 
			// for that we use the function "compare()"
			if (orderSort == Order.Ascending){
				Collections.sort(nodes, comparator);
			} else {
				Collections.sort(nodes, Collections.reverseOrder(comparator));
			}
			
			return XMLUtils.toNodeList(nodes);
		}
		
		return null;
	}
	
	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope)
			throws EngineException {
		if (isEnable()) {
			evaluate(javascriptContext, scope, getOptionSort(), "optionSort", true);
			return super.stepExecute(javascriptContext, scope);
		}
		return false;
	}
	
	@Override
	protected StepSource getTargetSource() throws EngineException {
		StepSource source = getSource();
		if (!source.isEmpty()) {
			Step sourceStep = source.getStep();
			if (sourceStep instanceof IteratorStep) {
				source = ((IteratorStep) sourceStep).getSource();
			}
		}
		return source;
	}
	
	@Override
	protected String getTargetXPath() throws EngineException {
		String xpath = "";
		StepSource source = getSource();
		if (!source.isEmpty()) {
			Step sourceStep = source.getStep();
			if (sourceStep instanceof IteratorStep) {
				xpath = source.getXpath().substring(1);
			}
		}
		return xpath;
	}
	
	@Override
	public XmlSchemaObject getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		//TODO
//		try {
//			StepSource source = getTargetSource();
//			if (!source.isEmpty()) {
//				XmlSchemaObject object = SchemaMeta.getXmlSchemaObject(schema, source.getStep());
//				if (object != null) {
//					SchemaMeta.setSchema(object, schema);
//					String xpath = source.getXpath();
//					String anchor = source.getAnchor() + getTargetXPath();
//					if (!".".equals(xpath)) {
//						Map<Node, XmlSchemaObject> references = new HashMap<Node, XmlSchemaObject>();
//						Document doc = XmlSchemaUtils.getDomInstance(object, references);
//						NodeList list = getXPathAPI().selectNodeList(doc.getDocumentElement(), anchor);
//						if (list != null) {
//							boolean isList = false;
//							if (list.getLength() > 1) {
//								isList = true;
//								object = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAll());
//							}
//							
//							for (int i = 0; i < list.getLength(); i++) {
//								Node node = list.item(i);
//								XmlSchemaObject referenced = references.get(node);
//								if (referenced != null) {
//									if (isList) {
//										XmlSchemaAll xmlSchemaAll = (XmlSchemaAll)object;
//										xmlSchemaAll.getItems().add(referenced);
//									}
//									else {
//										object = referenced;
//									}
//								}
//							}
//						}
//					}
//					return object;
//				}
//			}
//		} catch (Exception e) {
//			// TODO: handle exception
//			e.printStackTrace();
//		}
		return super.getXmlSchemaObject(collection, schema);
	}
}
