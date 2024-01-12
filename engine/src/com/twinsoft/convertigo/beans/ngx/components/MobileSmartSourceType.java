/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.beans.ngx.components;

import java.io.Serializable;

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
		if (Mode.SOURCE.equals(mode)) {
			if (sourceValue != null && !sourceValue.isEmpty() && !sourceValue.equals("{}")) {
				return MobileSmartSource.valueOf(sourceValue);
			}
		}
		return null;
	}
	
	public String getValue() {
		return getValue(false);
	}
	
	public String getValue(boolean extended) {
		if (Mode.PLAIN.equals(mode)) {
			return plainValue.toString();
		}
		if (Mode.SCRIPT.equals(mode)) {
			return scriptValue.toString();
		}
		if (Mode.SOURCE.equals(mode)) {
			MobileSmartSource cs = getSmartSource();
			String value = cs != null ? cs.getValue(extended):"";
			if (extended) {
				String keyThis = MobileSmartSource.keyThis;
				String keyThat = MobileSmartSource.keyThat;
				value = value.replaceAll(keyThis + "\\.item", "scope.item");
				value = value.replaceAll(keyThis + "\\.params", "scope.params");
				value = value.replaceAll(keyThis + "\\.", "this.");
				value = value.replaceAll(keyThat + "\\.", "this.");
			}
			return value;
		}
		return "";
	}
	
	public String getLabel() {
		String label = getValue();
		if (Mode.SOURCE.equals(mode)) {
			try {
				label = label.replaceAll("\\?\\.", ".");
				label = label.replaceAll("fs\\://", "");
				
				if (label.startsWith("item")) {
					label = "->" + label.substring(label.indexOf('.')+1);
					
				} else if (label.startsWith("listen([")) {
					int index = label.indexOf("])");
					String rs = label.substring("listen([".length(), index);
					label = rs.replaceAll("'", "") + label.substring(index+2).replaceFirst("\\.", "->");
				}
			} catch (Exception e) {}
		}
		return label;
	}
	
	@Override
	public String toString() {
		return getLabel();
	}

	@Override
	public Node writeXml(Document document) throws Exception {
		Element self = document.createElement(getClass().getSimpleName());
		String value = mode.name().toLowerCase() + ":" + getSmartValue();
		self.setTextContent(value);
		return self;
	}

	@Override
	public void readXml(Node node) throws Exception {
		try {
			Element self = (Element) node;
			String value = self.getTextContent();
			try {
				JSONObject jsonObject = new JSONObject(value);
				setMode(Mode.valueOf(jsonObject.getString("mode").toUpperCase()));
				setSmartValue(jsonObject.getString("value"));
			} catch (Exception e) {
				String[] v = value.split(":", 2);
				setMode(Mode.valueOf(v[0].toUpperCase()));
				setSmartValue(v[1]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static public String escapeStringForTpl(String s) {
		String escaped = s;
		if (escaped.indexOf("'") != -1) {
			escaped = escaped.replaceAll("([\\\\])++'", "'");
			escaped = escaped.replaceAll("([^\\\\])'", "$1\\\\'");
		}
		return escaped;
	}
	static public String escapeStringForTs(String s) {
		String escaped = s;
		if (escaped.indexOf("'") != -1) {
			escaped = escaped.replaceAll("([\\\\])++'", "'");
			escaped = escaped.replaceAll("([^\\\\])'", "$1\\\\\\\\'");
		}
		return escaped;
	}
	
}
