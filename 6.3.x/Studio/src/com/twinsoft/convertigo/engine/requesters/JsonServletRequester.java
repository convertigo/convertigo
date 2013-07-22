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

package com.twinsoft.convertigo.engine.requesters;

import java.io.UnsupportedEncodingException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.Engine;

public class JsonServletRequester extends ServletRequester {

    public JsonServletRequester() {
    }

    @Override
    public String getName() {
        return "JsonServletRequester";
    }

	public void setStyleSheet(Document document) {
        return;
    }
	
	private void handleElement(Element elt, JSONObject obj) throws JSONException {
		String key = elt.getTagName();
		
		JSONObject value = new JSONObject();
		NodeList nl = elt.getChildNodes();
		for(int i=0;i<nl.getLength();i++) {
			Node node = nl.item(i);
			if(node.getNodeType()==Node.ELEMENT_NODE) {
				Element child = (Element) node;
				handleElement(child, value);
			}
		}
		
		JSONObject attr = new JSONObject();
		NamedNodeMap nnm = elt.getAttributes();
		for(int i=0;i<nnm.getLength();i++) {
			Node node = nnm.item(i);
			attr.accumulate(node.getNodeName(), node.getNodeValue());
		}
		
		if(value.length()==0) {
			String content = elt.getTextContent();
			if(attr.length()==0) obj.accumulate(key, content);
			else value.accumulate("text", content);
		}
		
		if(attr.length()!=0)
			value.accumulate("attr", attr);
		
		if(value.length()!=0)
			obj.accumulate(key, value);
	}
	
	@Override
	public String postGetDocument(Document document) throws Exception {
		JSONObject json = new JSONObject();
		handleElement(document.getDocumentElement(), json);
		String result = json.toString(1);
		if (Engine.logContext.isDebugEnabled())
			Engine.logContext.debug("Json string:\n"+result);
		return result;
	}
	
	protected Object addStatisticsAsData(Object result) {
		return result;
	}
	
	protected Object addStatisticsAsText(String stats, Object result) throws UnsupportedEncodingException{
		return result;
	}
}