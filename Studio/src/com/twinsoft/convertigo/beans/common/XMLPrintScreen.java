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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.extractionrules.HtmlExtractionRule;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.enums.ImageFormat;
import com.twinsoft.convertigo.engine.parsers.PrintScreenRequest;
import com.twinsoft.convertigo.engine.parsers.PrintScreenResponse;

public class XMLPrintScreen extends HtmlExtractionRule implements ITagsProperty {

	private static final long serialVersionUID = 6747023789584983524L;

	private String tagName = "PrintScreen";
	private int height = -1;
	private int width = -1;
	private int top = 0;
	private int left = 0;
	private float scale = 1.0f;
	private String imageFormat = ImageFormat.png.name();
	private boolean includeDataUrl = true;
	private long minDelay = 100;
	
	public XMLPrintScreen() {
		super();
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}
	
	@Override
	public boolean apply(Document xmlDom, Context context) {
		try {
			HtmlConnector htmlConnector = (HtmlConnector) context.getConnector();
			PrintScreenResponse printScreenResponse = htmlConnector.getHtmlParser().makePrintScreen(context, new PrintScreenRequest(height, width, top, left, scale, getImageFormatEnum(), true, minDelay));
			
			if (printScreenResponse != null) {
				Document doc = context.outputDocument;
				Element root = doc.getDocumentElement();
				Element elt = doc.createElement((tagName.equals("") ? getName() : tagName));
				
				elt.setAttribute("height", Integer.toString(printScreenResponse.height));
				elt.setAttribute("width", Integer.toString(printScreenResponse.width));
				elt.setAttribute("scale", Float.toString(printScreenResponse.scale));
				elt.setAttribute("imageFormat", printScreenResponse.imageFormat.name());
				
				if (includeDataUrl) {
					elt.setTextContent(printScreenResponse.dataUrlPrefix + printScreenResponse.dataBase64);
				} else {
					elt.setTextContent(printScreenResponse.dataBase64);
				}
				
				root.appendChild(elt);
			}
			return true;
		}
		catch (Exception ex) {}
		return false;
	}
	
	public String getSchema(String tns) {
		return "<xsd:element minOccurs=\"0\" maxOccurs=\"1\" name=\""+ (tagName.equals("") ? getName():tagName) + "\" type=\"xsd:string\" />\n";
	}
	
	public String getSchemaElementName() {
		return (tagName.equals("") ? getName():tagName);
	}

	public String getSchemaElementType() {
		return "string";
	}
	
	public String getSchemaElementNSType(String tns) {
		return "xsd:string";
	}

	public ImageFormat getImageFormatEnum() {
		return ImageFormat.valueOf(imageFormat);
	}

	public String[] getTagsForProperty(String propertyName) {
		if ("imageFormat".equals(propertyName)) {
			return ImageFormat.imageFormats;
		}
		return null;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getTop() {
		return top;
	}

	public void setTop(int top) {
		this.top = top;
	}

	public int getLeft() {
		return left;
	}

	public void setLeft(int left) {
		this.left = left;
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public String getImageFormat() {
		return imageFormat;
	}

	public void setImageFormat(String imageFormat) {
		this.imageFormat = imageFormat;
	}

	public boolean isIncludeDataUrl() {
		return includeDataUrl;
	}

	public void setIncludeDataUrl(boolean includeDataUrl) {
		this.includeDataUrl = includeDataUrl;
	}

	public long getMinDelay() {
		return minDelay;
	}

	public void setMinDelay(long minDelay) {
		this.minDelay = minDelay;
	}
}
