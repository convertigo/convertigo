/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.beans.common;

import java.io.Serializable;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class FontSource implements XMLizable, Serializable, Cloneable {

	private static final long serialVersionUID = 2489157191050437443L;
	
	private String content = "{}";
	
	public FontSource() {
		
	}

	public FontSource(String fontId, String fontFamily, String fontWeight, String fontStyle, String fontSubset) {
		JSONObject jsonObject = new JSONObject();
		if (fontId != null && !fontId.isBlank()) {
			try {
				jsonObject.put("fontId", fontId);
				
				if (fontFamily != null && !fontFamily.isBlank()) {
					jsonObject.put("fontFamily", fontFamily);
				}
				if (fontWeight != null && !fontWeight.isBlank()) {
					jsonObject.put("fontWeight", fontWeight);
				}
				if (fontStyle != null && !fontStyle.isBlank()) {
					jsonObject.put("fontStyle", fontStyle);
				}
				if (fontSubset != null && !fontSubset.isBlank()) {
					jsonObject.put("fontSubset", fontSubset);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		this.content = jsonObject.toString();
	}

	public String getString() {
		return content;
	}
	
	public void setString(String content) {
		this.content = content;
	}
	
	@Override
	protected FontSource clone() throws CloneNotSupportedException {
		return (FontSource)super.clone();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FontSource) {
			FontSource fcs = (FontSource)obj;
			return fcs.getString().equals(getString());
		}
		return false;
	}

	@Override
	public Node writeXml(Document document) throws Exception {
		Element element = document.createElement(getClass().getName());
        element.appendChild(document.createCDATASection(FileUtils.CrlfToLf(content)));
		return element;
	}

	@Override
	public void readXml(Node node) throws Exception {
        this.content = XMLUtils.readXmlText(node);
	}

	@Override
	public String toString() {
		String label = "inherits";
		JSONObject jsonFont = toJSONObject();
		try {
			label = " " + jsonFont.getString("fontFamily"); //label = jsonFont.getString("fontId");
			label += " (" + jsonFont.getString("fontWeight");
			label += " " + jsonFont.getString("fontStyle");
			label += " " + jsonFont.getString("fontSubset");
			label += ")";
		} catch (JSONException e) {}
		return label;
	}
	
	private JSONObject toJSONObject() {
		try {
			return new JSONObject(content);
		} catch (Exception e) {}
		return new JSONObject();
	}
	
	public boolean isEmpty() {
		return toJSONObject().length() == 0;
	}
	
	public String getFontId() {
		try {
			return toJSONObject().getString("fontId");
		} catch (Exception e) {}
		return null;
	}
	
	public String getFontFamily() {
		try {
			return toJSONObject().getString("fontFamily");
		} catch (Exception e) {}
		return null;
	}
	
	public String getFontWeight() {
		try {
			return toJSONObject().getString("fontWeight");
		} catch (Exception e) {}
		return null;
	}
	
	public String getFontStyle() {
		try {
			return toJSONObject().getString("fontStyle");
		} catch (Exception e) {}
		return null;
	}
	
	public String getFontSubset() {
		try {
			return toJSONObject().getString("fontSubset");
		} catch (Exception e) {}
		return null;
	}
	
	public FormatedContent getStyleContent() {
		String formated = "";
		if (getFontFamily() != null) {
			formated += "font-family: \""+ getFontFamily() +"\";\n";
		}
		if (getFontWeight() != null) {
			formated += "font-weight: "+ getFontWeight() +";\n";
		}
		if (getFontStyle() != null) {
			formated += "font-style: \""+ getFontStyle() +"\";\n";
		}
		if (getFontSubset() != null) {
			formated += "font-variant: \""+ getFontSubset() +"\";\n";
		}
		return new FormatedContent(formated);
	}
	
	public String getStyleCssImport(boolean withTilde) {
		JSONObject jsonFont = toJSONObject();
		try {
			String csspath = "";
			csspath = jsonFont.getString("fontId");
			csspath += "/" + jsonFont.getString("fontSubset");
			csspath += "-" + jsonFont.getString("fontWeight");
			csspath += "-" + jsonFont.getString("fontStyle");
			
			csspath = csspath.replace("-normal", "");
			return "@import \"" + (withTilde ? "~" : "") + "@fontsource/" + csspath + ".css\";";
		} catch (Exception e) {}
		return "";
	}
}
