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

import org.w3c.dom.*;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.translators.Translator;
import com.twinsoft.convertigo.engine.translators.XmlServletTranslator;

public class ClientXsltServletRequester extends ServletRequester {

    public ClientXsltServletRequester() {
    }

    @Override
	public Translator getTranslator() {
		return new XmlServletTranslator();
	}

    public String getName() {
        return "ClientXsltServletRequester";
    }

	public void preGetDocument() throws EngineException {
		super.preGetDocument();
		context.isXsltRequest = true;
	}

    public void setStyleSheet(Document document) {
        if (context.sheetUrl == null) {
            // No sheet has been defined: it may be a pure xml producer
            // without sheet request...
            return;
        }

        if (context.absoluteSheetUrl.startsWith(Engine.XSL_PATH + "/")) {
        	if (!context.sheetUrl.startsWith("../../xsl/")) {
        		context.sheetUrl = "../../xsl/" + context.sheetUrl;
        	}
        }

		// Adding the stylesheet processing instruction
		ProcessingInstruction pi = document.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"" + context.sheetUrl + "\"");
		Element documentElement = document.getDocumentElement();
		document.insertBefore(pi, documentElement);
    }

    protected Object performXSLT(Document document) throws Exception {
    	// Because it is a client XSLT process, just return the pretty print DOM.
    	return document;
    }
	protected Object addStatisticsAsData(Object result) { 
		return EngineStatistics.addStatisticsAsXML(context, result); 
	} 
	
	protected Object addStatisticsAsText(String stats, Object result) throws UnsupportedEncodingException{ 
		if (result != null) { 
                if (stats == null) stats = context.statistics.printStatistics(); 
                if (result instanceof Document) { 
                        Document document = (Document) result; 
                        Comment comment = document.createComment("\n" + stats); 
                        document.appendChild(comment); 
                } 
                else if (result instanceof byte[]) { 
                        String encodingCharSet = "UTF-8"; 
                        if (context.requestedObject != null) 
                                encodingCharSet = context.requestedObject.getEncodingCharSet(); 
                        String sResult = new String((byte[]) result, encodingCharSet); 
                        sResult += "<!--\n" + stats + "\n-->"; 
                        result = sResult.getBytes(encodingCharSet); 
                } 
        } 
        return result;
	} 
}
