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

import org.w3c.dom.Comment;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.EngineStatistics;

public class XmlServletRequester extends ServletRequester {

    public XmlServletRequester() {
    }

    public String getName() {
        return "XmlServletRequester";
    }

	public void setStyleSheet(Document document) {
        // No sheet should be inserted
        return;
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
