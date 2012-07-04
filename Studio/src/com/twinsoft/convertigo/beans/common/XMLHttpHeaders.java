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

import org.apache.commons.httpclient.Header;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.extractionrules.HtmlExtractionRule;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;

public class XMLHttpHeaders extends HtmlExtractionRule {

	private static final long serialVersionUID = 8979406027000414349L;
	
	private static final String tagName = "HttpHeaders";
	private static final String recordTypeName = "httpHeadersType";
	
	private transient Header[] contextHeaders;
	
	public XMLHttpHeaders() {
		super();
		xpath = "/";
	}

	@Override
	public XMLHttpHeaders clone() throws CloneNotSupportedException {
		XMLHttpHeaders xmlHttpHeaders = (XMLHttpHeaders)super.clone();
		return xmlHttpHeaders;
	}
	
	@Override
	public boolean apply(Document xmlDom, Context context) {
		contextHeaders = context.getResponseHeaders();
		return super.apply(xmlDom, context);
	}

	@Override
	protected void appendToOutputDom(NodeList nodeList, Document outputDom) {
		
		Element doc = outputDom.getDocumentElement();
		Element httpHeaders = outputDom.createElement(tagName);
		Element headerElement;
		Header header;
		int length = contextHeaders.length;
		
		for (int i = 0 ; i < length ; i++) {
			if (!isRequestedObjectRunning()) break;
			
			header = contextHeaders[i];
			headerElement = outputDom.createElement("header");
			headerElement.setAttribute("headerName", header.getName());
			headerElement.setAttribute("headerValue", header.getValue());
	        httpHeaders.appendChild(headerElement);
			Engine.logBeans.trace("HttpHeader '" + headerElement.getAttribute("headerName") + " : " 
								+ headerElement.getAttribute("headerValue") + "' added to httpHeaders.");
		}
		doc.appendChild(httpHeaders);
		Engine.logBeans.trace("HttpHeaders added to result document.");
	}
	
	public String getSchema(String tns) {
		String recordName = getSchemaElementName();
		
		addWsType(recordTypeName, getRecordTypeSchema(tns));
		
		return "<xsd:element minOccurs=\"0\" maxOccurs=\"unbounded\" name=\""+ recordName + "\" type=\""+tns+":"+recordTypeName+"\" />\n";
	}
	
	public String getSchemaElementName() {
		return tagName;
	}

	public String getSchemaElementType() {
		return recordTypeName;
	}
	
	public String getSchemaElementNSType(String tns) {
		return tns+":"+ getSchemaElementType();
	}
	
	private String getRecordTypeSchema(String tns){
		return
		"<xsd:complexType name=\""+recordTypeName+"\">"+
			"<xsd:sequence>"+
				"<xsd:element name=\"header\" type=\""+tns+":headerType\" maxOccurs=\"unbounded\" minOccurs=\"0\"/>"+
			"</xsd:sequence>"+
		"</xsd:complexType>"+
		"<xsd:complexType name=\"headerType\">"+
			"<xsd:attribute name=\"headerName\" type=\"xsd:string\" use=\"required\"/>"+
			"<xsd:attribute name=\"headerValue\" type=\"xsd:string\" use=\"required\"/>"+
		"</xsd:complexType>";
	}
}
