/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.DoFileUploadMode;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class RequestableHttpVariable extends RequestableVariable {

	private static final long serialVersionUID = -8730510144092552400L;

	private String httpMethod = "GET";
	private String httpName = "";
	private DoFileUploadMode doFileUploadMode = DoFileUploadMode.none;
	private String doFileUploadContentType = "";
	private String doFileUploadCharset = "UTF-8";
	
	public RequestableHttpVariable() {
		super();
	}

	@Override
	public RequestableHttpVariable clone() throws CloneNotSupportedException {
		RequestableHttpVariable clonedObject = (RequestableHttpVariable)super.clone();
		return clonedObject;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}

	public String getHttpName() {
		return httpName;
	}

	public void setHttpName(String httpName) {
		this.httpName = httpName;
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("httpMethod")) {
			return new String[]{
					"",
					"GET",
					"POST"
			};
		}
		return super.getTagsForProperty(propertyName);
	}

	public DoFileUploadMode getDoFileUploadMode() {
		return doFileUploadMode;
	}

	public void setDoFileUploadMode(DoFileUploadMode doFileUploadMode) {
		this.doFileUploadMode = doFileUploadMode;
	}
	
	public String getMtomCid(String value) {
		return getName() + "_" + value.hashCode();
	}

	public String getDoFileUploadContentType() {
		return doFileUploadContentType;
	}

	public void setDoFileUploadContentType(String doFileUploadContentType) {
		this.doFileUploadContentType = doFileUploadContentType;
	}

	public String getDoFileUploadCharset() {
		return doFileUploadCharset;
	}

	public void setDoFileUploadCharset(String doFileUploadCharset) {
		this.doFileUploadCharset = doFileUploadCharset;
	}
	
    @Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
        String version = element.getAttribute("version");
        
        if (version == null) {
            String s = XMLUtils.prettyPrintDOM(element);
            EngineException ee = new EngineException(
                "Unable to find version number for the database object \"" + getName() + "\".\n" +
                "XML data: " + s
            );
            throw ee;
        }
        
        if (VersionUtils.compare(version, "7.4.7") < 0) {
        	doFileUploadContentType = "application/octet-stream";
			hasChanged = true;
			Engine.logBeans.warn("[HttpVariable] The object \"" + getName()+ "\" has been updated to version 7.4.7");
        }
    }
	
	@Override
	public String toString() {
		String label = "";
		try {
			label = getLabel();
		} catch (EngineException e) {}
		if (!getName().equals(httpName) && !httpName.isEmpty()) {
			return getName() + " → " + httpName + label;
		}
		return super.toString();
	}
}
