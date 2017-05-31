/*
 * Copyright (c) 2001-2016 Convertigo SA.
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

package com.twinsoft.convertigo.beans.mobile.components;

import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class UIAttribute extends UIComponent implements ITagsProperty {

	private static final long serialVersionUID = 4407761661788130893L;
	
	public UIAttribute() {
		super();
	}

	@Override
	public UIAttribute clone() throws CloneNotSupportedException {
		UIAttribute cloned = (UIAttribute) super.clone();
		return cloned;
	}

	@Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);
		
		NodeList properties = element.getElementsByTagName("property");
		Element property = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "attrValue");
		Element pelement = (Element) XMLUtils.findChildNode(property, Node.ELEMENT_NODE);
		Object value = XMLUtils.readObjectFromXml(pelement);
		
		if (value != null && value instanceof String) {
			String s = (String) value;
			try {
				new JSONObject(s);
			} catch (Exception e) {
				boolean isScriptValue = s.startsWith("{{") && s.endsWith("}}");
				MobileSmartSourceType msst = new MobileSmartSourceType();
				msst.setMode(isScriptValue ? Mode.SCRIPT : Mode.PLAIN);
				msst.setSmartValue(isScriptValue ? s.substring(2, s.length()-2) : s);
				Element nelement = (Element) XMLUtils.writeObjectToXml(property.getOwnerDocument(), msst);
				property.replaceChild(nelement, pelement);
				hasChanged = true;
			}
		}
	}
	
	private String attrName = "attr";
	
	public String getAttrName() {
		return attrName;
	}

	public void setAttrName(String attrName) {
		this.attrName = attrName;
	}
	
	private MobileSmartSourceType attrValue = new MobileSmartSourceType("value");
	
	public MobileSmartSourceType getAttrSmartType() {
		return attrValue;
	}

	public void setAttrSmartType(MobileSmartSourceType attrValue) {
		this.attrValue = attrValue;
	}
	
	protected String getAttrValue() {
		String value = attrValue.getValue();
		if (!Mode.PLAIN.equals(attrValue.getMode())) {
			value = "{{" + value + "}}";
		}
		return value;
	}
	
	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			String attrVal = getAttrValue();
	        if (attrName.isEmpty()) {
	        	return attrVal.isEmpty() ? "":" "+ attrVal;
	        }
	        else {
	        	return (" "+attrName+"=\""+ attrVal +"\"");
	        }
		}
		else
			return "";
	}

	@Override
	public String toString() {
		String label = attrName;
		label = label + (label.isEmpty() ? "":"=") + getAttrValue();
		return label;
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("attrValue")) {
			return new String[] {""};
		}
		return new String[0];
	}
	
}
