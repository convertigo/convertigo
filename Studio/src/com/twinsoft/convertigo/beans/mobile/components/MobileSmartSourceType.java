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

import java.io.Serializable;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.common.XMLizable;

public class MobileSmartSourceType implements XMLizable, Serializable, Cloneable {

	private static final long serialVersionUID = 5900498168891260122L;

	public enum Mode {
		PLAIN("TX", "Plain text", ""),
		SCRIPT("TS", "TypeScript expression", ""),
		SOURCE("SC", "Source definition", "");
		
		String label;
		String tooltip;
		String prefix;
		
		Mode(String label, String tooltip, String prefix) {
			this.label = label;
			this.tooltip = tooltip;
			this.prefix = prefix;
		}
		
		public String label() {
			return label;
		}
		
		public String tooltip() {
			return tooltip;
		}
		
		public String prefix() {
			return prefix;
		}
	}
	
	private Mode mode = Mode.PLAIN;
	private String plainValue = "";
	private String scriptValue = "";
	private String sourceValue = "{}";
	
	public MobileSmartSourceType() {
		
	}

	public MobileSmartSourceType(String plainValue) {
		this.plainValue = plainValue;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MobileSmartSourceType) {
			MobileSmartSourceType smt = (MobileSmartSourceType) obj;
			return mode.equals(smt.mode) 
					&& plainValue.equals(smt.plainValue)
					&& scriptValue.equals(smt.scriptValue)
					&& sourceValue.equals(smt.sourceValue);
		}
		return false;
	}

	@Override
	public MobileSmartSourceType clone() {
		try {
			return (MobileSmartSourceType) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	public Mode getMode() {
		return mode;
	}
	
	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public Object getEditorData() {
		return getSmartValue();
	}
	
	public String getSmartValue() {
		if (Mode.PLAIN.equals(mode)) {
			return plainValue;
		}
		if (Mode.SCRIPT.equals(mode)) {
			return scriptValue;
		}
		if (Mode.SOURCE.equals(mode)) {
			return sourceValue;
		}
		return "";
	}
	
	public void setSmartValue(String value) {
		if (Mode.PLAIN.equals(mode)) {
			plainValue = value == null ? "":value;
			scriptValue = "";
			sourceValue = "{}";
		}
		if (Mode.SCRIPT.equals(mode)) {
			plainValue = "";
			scriptValue = value == null ? "":value;
			sourceValue = "{}";
		}
		if (Mode.SOURCE.equals(mode)) {
			plainValue = "";
			scriptValue = "";
			sourceValue = value == null || value.isEmpty() ? "{}":value;
		}
	}

	public MobileSmartSource getSmartSource() {
		return Mode.SOURCE.equals(mode) ?  MobileSmartSource.valueOf(sourceValue) : null;
	}
			
	public String getValue() {
		if (Mode.PLAIN.equals(mode)) {
			return plainValue.toString();
		}
		if (Mode.SCRIPT.equals(mode)) {
			return scriptValue.toString();
		}
		if (Mode.SOURCE.equals(mode)) {
			MobileSmartSource cs = getSmartSource();
			return cs != null ? cs.getValue():"";
		}
		return "";
	}
	
	@Override
	public String toString() {
		return getValue();
	}

	@Override
	public Node writeXml(Document document) throws Exception {
		Element self = document.createElement(getClass().getSimpleName());
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("mode", mode.name().toLowerCase());
			jsonObject.put("value", getSmartValue());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		self.setTextContent(jsonObject.toString());
		return self;
	}

	@Override
	public void readXml(Node node) throws Exception {
		try {
			Element self = (Element) node;
			JSONObject jsonObject = new JSONObject(self.getTextContent());
			setMode(Mode.valueOf(jsonObject.getString("mode").toUpperCase()));
			setSmartValue(jsonObject.getString("value"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
