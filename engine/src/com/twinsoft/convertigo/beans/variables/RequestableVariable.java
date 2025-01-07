/*
 * Copyright (c) 2001-2025 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.variables;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.constants.Constants;

import com.twinsoft.convertigo.beans.common.XmlQName;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.Variable;

public class RequestableVariable extends Variable implements ITagsProperty {

	private static final long serialVersionUID = 1999336848736513573L;

	private String schemaType = "xsd:string";
	private boolean wsdl = true;
	private boolean personalizable = false;
	private boolean cachedKey = true;
	private boolean isFileUpload = false;
    private XmlQName xmlTypeAffectation = new XmlQName(Constants.XSD_STRING);
	
	public RequestableVariable() {
        super();
	}

	@Override
	public RequestableVariable clone() throws CloneNotSupportedException {
		RequestableVariable clonedObject = (RequestableVariable)super.clone();
		return clonedObject;
	}
	
	public boolean isWsdl() {
		return wsdl;
	}

	public void setWsdl(boolean wsdl) {
		this.wsdl = wsdl;
	}

	public boolean isPersonalizable() {
		return personalizable;
	}

	public void setPersonalizable(boolean personalizable) {
		this.personalizable = personalizable;
	}

	public boolean isCachedKey() {
		return cachedKey;
	}

	public void setCachedKey(boolean cachedKey) {
		this.cachedKey = cachedKey;
	}

	public String getSchemaType() {
		return schemaType;
	}

	public void setSchemaType(String schemaType) {
		this.schemaType = schemaType;
	}

	@Override
	public boolean getIsFileUpload() {
		return isFileUpload;
	}

	public void setIsFileUpload(boolean isFileUpload) {
		this.isFileUpload = isFileUpload;
	}

	public String[] getTagsForProperty(String propertyName) {
		return new String[0];
	}

	public XmlQName getXmlTypeAffectation() {
		return xmlTypeAffectation;
	}

	public void setXmlTypeAffectation(XmlQName xmlTypeAffectation) {
		this.xmlTypeAffectation = xmlTypeAffectation;
	}
	
	public QName getTypeAffectation() {
		QName qName = getXmlTypeAffectation().getQName();
		if (qName.getLocalPart().length() == 0) {
			qName = Constants.XSD_STRING;
		}
		return qName;
	}

}
