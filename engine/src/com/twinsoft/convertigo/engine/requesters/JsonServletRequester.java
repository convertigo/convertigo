/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

package com.twinsoft.convertigo.engine.requesters;

import java.io.UnsupportedEncodingException;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.ConvertigoError;
import com.twinsoft.convertigo.engine.enums.JsonOutput;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.convertigo.engine.util.XMLUtils;

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
	
	@Override
	public String postGetDocument(Document document) throws Exception {
		ConvertigoError.cleanDocument(context.getXpathApi(), document);
		boolean useType = context.project != null && context.project.getJsonOutput() == JsonOutput.useType;
		JsonRoot jsonRoot = context.project != null ? context.project.getJsonRoot() : JsonRoot.docNode;
		return XMLUtils.XmlToJson(document.getDocumentElement(), true, useType, jsonRoot);
	}
	
	protected Object addStatisticsAsData(Object result) { 
		return result; 
	} 
	
	protected Object addStatisticsAsText(String stats, Object result) throws UnsupportedEncodingException{ 
		return result; 
	} 
}