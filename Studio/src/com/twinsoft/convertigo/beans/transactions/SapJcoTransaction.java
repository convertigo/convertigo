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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.beans.transactions;

import java.util.ArrayList;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.SapJcoConnector;
import com.twinsoft.convertigo.beans.connectors.SapJcoConnector.SapJcoProviderImpl;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;


public class SapJcoTransaction extends TransactionWithVariables {

	private static final long serialVersionUID = -3420141243872478042L;
	
	public static final String unconfigured_bapi_name = "<Configure BAPI Name Here>";
	
	transient SapJcoConnector connector;

	protected String bapiName = unconfigured_bapi_name;

	public String getBapiName() {
		return bapiName;
	}

	public void setBapiName(String bapiName) {
		this.bapiName = bapiName;
	}

	@Override
    public SapJcoTransaction clone() throws CloneNotSupportedException {
    	SapJcoTransaction clonedObject = (SapJcoTransaction) super.clone();
    	clonedObject.connector = null;
        return clonedObject;
    }
		
	@Override
	public void setStatisticsOfRequestFromCache() {
		// TODO Auto-generated method stub
	}
	
	private void appendParameterValue(Element parent, String parameterName, Object parameterValue) {
		if (parent != null && parameterValue != null) {
			if (parameterValue instanceof Object[]) {
				Object[] values = GenericUtils.cast(parameterValue);
				for (int i=0; i<values.length; i++) {
					appendParameterValue(parent, parameterName, values[i]);
				}
			}
			else if (parameterValue instanceof ArrayList) {
				ArrayList<?> list = (ArrayList<?>)parameterValue;
				for (int i=0; i<list.size(); i++) {
					appendParameterValue(parent, parameterName, list.get(i));
				}
			}
			else if (parameterValue instanceof NodeList) {
				Document doc = parent.getOwnerDocument();
				NodeList nl = (NodeList)parameterValue;
				for (int i=0; i<nl.getLength(); i++) {
					parent.appendChild(doc.importNode(nl.item(i),true));
				}
			}
			else if (parameterValue instanceof String) {
				Document doc = parent.getOwnerDocument();
				boolean addVarElement = ((String)parameterValue).indexOf(parameterName) == -1;
				Element varElement = (Element) (addVarElement ? parent.appendChild(doc.createElement(parameterName)):parent);
				varElement.appendChild(doc.createTextNode((String) parameterValue));
			}
		}
	}
	
	@Override
	public Object getParameterValue(String parameterName) throws EngineException {
		Object parameterValue = super.getParameterValue(parameterName);
		if (parameterValue != null) {
			Document doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
			Element sapVariable = doc.createElement("sap_variable");
			appendParameterValue(sapVariable, parameterName, parameterValue);
			doc.appendChild(sapVariable);
			return doc;
		}
		return parameterValue;
	}
	
	@Override
	public void runCore() throws EngineException {
		connector = ((SapJcoConnector) parent);
        try {
        	Engine.logBeans.debug("[SAP transaction] BAPI named '"+ bapiName +"' executing ...");
        	Document jcoDoc = connector.executeTransaction(this);
        	Engine.logBeans.debug("[SAP transaction] BAPI named '"+ bapiName +"' executed.");
        	
        	if (jcoDoc != null) {
        		Element sap_output = jcoDoc.getDocumentElement();
    			if (sap_output != null) {
        			Element outputDocumentRootElement = context.outputDocument.getDocumentElement();
        			Element jcoImported = (Element) context.outputDocument.importNode(sap_output,true);
    				outputDocumentRootElement.appendChild(jcoImported);
    				score +=1;
    			}
        	}
        	
		} catch (Exception e) {
			connector.setData(null);
			throw new EngineException("An unexpected error occured while executing transaction.",e);
		}
        finally {
        	
        }
	}
	
	@Override
	protected String extractXsdType(Document document) throws Exception {
		return generateWsdlType(document);
	}

	@Override
	public String generateWsdlType(Document document) throws Exception {
		String xsdType = "<xsd:complexType name=\""+ getXsdResponseElementName() +"\" />\n";
		try {
    		XmlSchema xmlSchema = createSchema();
    		Document doc = xmlSchema.getSchemaDocument();
    		Element elt = XMLUtils.findNodeByAttributeValue(doc.getDocumentElement().getChildNodes(), "name", getXsdResponseTypeName());
   			xsdType = XMLUtils.prettyPrintElement(elt, true, true);
		}
		catch (Exception e) {
			Engine.logBeans.error("Unable to generate schema for SapJcoTransaction named '"+ getName() +"'", e);
		}
		return xsdType;
	}

	@Override
	protected XmlSchemaComplexType addSchemaResponseDataType(XmlSchema xmlSchema) {
		XmlSchemaComplexType xmlSchemaComplexType = super.addSchemaResponseDataType(xmlSchema);
		XmlSchemaSequence xmlSchemaSequence = (XmlSchemaSequence)xmlSchemaComplexType.getParticle();
		if (xmlSchemaSequence == null) xmlSchemaSequence = new XmlSchemaSequence();
		try {
			connector = ((SapJcoConnector) parent);
			SapJcoProviderImpl provider = connector.getSapJCoProvider();
			XmlSchemaElement sap_output = provider.addJCoFunctionResponseSchema(this, xmlSchema);
			xmlSchemaSequence.getItems().add(sap_output);
		} catch (Exception e) {
			Engine.logBeans.error("Unable to generate response schema for SapJcoTransaction named '"+ getName() +"'", e);
		}
		xmlSchemaComplexType.setParticle(xmlSchemaSequence);
		return xmlSchemaComplexType;
		
	}
	
	public void addSchemaAnnotation(XmlSchemaElement xmlSchemaElement, String description) {
		addSchemaCommentAnnotation(xmlSchemaElement, description);
	}
	
}


