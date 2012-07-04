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

package com.twinsoft.convertigo.beans.common;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.beans.core.ITablesProperty;
import com.twinsoft.convertigo.engine.Engine;

public class XMLRecord extends AbstractXMLReferer implements ITablesProperty {

	private static final long serialVersionUID = -5580937012912705994L;

	private String tagName = null;
	
	private XMLVector<XMLVector<Object>> description = null;
	
	public XMLRecord() {
		super();
		tagName = "XMLRecord";
		description = new XMLVector<XMLVector<Object>>();
	}

	@Override
	public XMLRecord clone() throws CloneNotSupportedException {
		XMLRecord xmlRecord = (XMLRecord)super.clone();
		return xmlRecord;
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}
	
	public XMLVector<Object> add(String eltName, String eltXpath, boolean bExtractChilds) {
		XMLVector<Object> v = new XMLVector<Object>();
		v.add(eltName);
		v.add(eltXpath);
		v.add(new Boolean(bExtractChilds));
		description.add(v);
		return v;
	}
	
	public void add(XMLVector<Object> v) {
		if (v != null && !description.contains(v))
			description.add(v);
	}

	public void remove(XMLVector<Object> v) {
		if (v != null)
			description.remove(v);
	}
	
	public XMLVector<XMLVector<Object>> getDescription() {
		return description;
	}

	public void setDescription(XMLVector<XMLVector<Object>> description) {
		this.description = description;
	}
	
	private String getName(List<Object> v) {
		return (String)v.get(0);
	}
	
	private String getXPath(List<Object> v) {
		return (String)v.get(1);
	}
	
	private boolean hasToExtract(List<Object> v) {
		return ((Boolean)v.get(2)).booleanValue();
	}
	
	@Override
	protected void appendToOutputDom(NodeList nodeList, Document outputDom) {
		Element doc, element, record;
		String recordName, reXPath;
		Node node, rn;
		NodeList nl;
		Text text;
		int length;
		
		doc = outputDom.getDocumentElement();
		
		length = nodeList.getLength();
		for (int i=0; i< length; i++) {
			if (!isRequestedObjectRunning()) break;
			
			node = nodeList.item(i);
			recordName = (tagName.equals("") ? name:tagName);
			record = outputDom.createElement(recordName);
	        for(List<Object> re : description) {
	        	nl = null;
	        	try {
	        		reXPath = getXPath(re);
					nl = xpathApi.selectNodeList(node, reXPath);
				}
	        	catch (TransformerException e) {;}
	        	
	        	if (nl != null)
		        	for (int k=0; k < nl.getLength(); k++) {
		        		rn = nl.item(k);
		        		element = outputDom.createElement(getName(re));
		            	text = outputDom.createTextNode(getStringValue(rn, hasToExtract(re)));
		            	element.appendChild(text);
		            	record.appendChild(element);
		        	}
	        }
	        if (isDisplayReferer())
				addReferer(record);
			doc.appendChild(record);
			Engine.logBeans.trace("XMLRecord '" + node.getNodeName() + "' added to result document.");
		}
	}

	public String getSchema(String tns) {
		return getRecordSchema(tns);
	}
	
	private  String getRecordSchema(String tns) {
		String recordName, recordTypeName, recordType, maxOccurs;
		String recordSchema, recordTypeSchema, schema;
		Set<String> schemas = new HashSet<String>();
		maxOccurs = description.size()>1 ? "":"maxOccurs=\"unbounded\"";
		recordName = getSchemaElementName();
		recordTypeName = getSchemaElementType();
		recordType = tns+":"+ recordTypeName;
		recordTypeSchema = "<xsd:complexType name=\""+ recordTypeName +"\">\n";
		recordTypeSchema += "<xsd:sequence>\n";
		for (List<Object> data : description) {
			schema = "<xsd:element minOccurs=\"0\" "+maxOccurs+" name=\""+ getName(data) + "\" type=\"xsd:string\" />\n";
			// do not append doublet
			if(schemas.add(schema)) recordTypeSchema += schema;
		}
		recordTypeSchema += "</xsd:sequence>\n";
		recordTypeSchema += "<xsd:attribute name=\"referer\" type=\"xsd:string\" use=\"optional\" />\n";
		recordTypeSchema += "</xsd:complexType>\n";
		addWsType(recordTypeName, recordTypeSchema);
		recordSchema = "<xsd:element minOccurs=\"0\" maxOccurs=\"unbounded\" name=\""+ recordName + "\" type=\""+ recordType +"\" />\n";
		return recordSchema;
	}
	
	public String getSchemaElementName() {
		return (tagName.equals("") ? name:tagName);
	}

	public String getSchemaElementType() {
		return getSchemaElementName() + "RecordType";
	}
	
	public String getSchemaElementNSType(String tns) {
		return tns+":"+ getSchemaElementType();
	}

	public String[] getTablePropertyNames() {
		return new String[] {"description"};
	}

	public XMLVector<XMLVector<Object>> getTableData(String propertyName) {
		if (propertyName.equals("description"))
			return getDescription();
		return null;
	}
	
	public String getTableRenderer(String propertyName) {
		if (propertyName.equals("description"))
			return "XMLRecordDescriptionTreeObject";
		return null;
	}
}
