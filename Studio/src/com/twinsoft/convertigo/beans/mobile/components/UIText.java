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

import org.apache.commons.lang3.StringEscapeUtils;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class UIText extends UIComponent implements ITagsProperty {
	
	private static final long serialVersionUID = 4062617301596626610L;

	public UIText() {
		super();
	}

	@Override
	public UIText clone() throws CloneNotSupportedException {
		UIText cloned = (UIText) super.clone();
		return cloned;
	}

	@Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);
		
		NodeList properties = element.getElementsByTagName("property");
		Element property = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "textValue");
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
	
	private MobileSmartSourceType textValue = new MobileSmartSourceType("some text");
	
	public MobileSmartSourceType getTextSmartType() {
		return textValue;
	}

	public void setTextSmartType(MobileSmartSourceType textValue) {
		this.textValue = textValue;
	}

	@Override
	public void add(DatabaseObject databaseObject) throws EngineException {
		add(databaseObject, null);
	}
	
    @Override
    public void add(DatabaseObject databaseObject, Long after) throws EngineException {
        throw new EngineException("You cannot add to a text component a database object of type " + databaseObject.getClass().getName());
    }

	protected String getTextValue() {
		String value = textValue.getValue();
		if (!Mode.PLAIN.equals(textValue.getMode())) {
			value = "{{" + value + "}}";
		}
		return value;
	}
	
	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			return StringEscapeUtils.escapeHtml4(getTextValue()) + System.getProperty("line.separator");
		}
		else {
			return "";
		}
	}

	@Override
	public String toString() {
		String label = getTextValue();
		return label.isEmpty() ? "?" : label;
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("textValue")) {
			return new String[] {""};
		}
		return new String[0];
	}
}
