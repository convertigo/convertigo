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

package com.twinsoft.convertigo.engine.admin.util;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.util.Log;

/**
 * DOM utils functions
 */
public class DOMUtils {

	public static Document createDocument() throws ParserConfigurationException {
		Document document = XMLUtils.documentBuilderDefault.newDocument();
        return document;
	}

	public static Document handleError(Throwable t) throws ParserConfigurationException {
		Exception e = t instanceof Exception ? (Exception) t : new Exception("Unexpected exception", t);
		return handleError(e);
	}
	
	public static Document handleError(Exception e) throws ParserConfigurationException {
        Document document = handleError(e.getMessage());

        Element error = (Element) document.getElementsByTagName("error").item(0);

        Element exception = document.createElement("exception");
        exception.appendChild(document.createTextNode(e.getClass().getName()));
        error.appendChild(exception);

        Element stackTrace = document.createElement("stacktrace");
        String jss = Log.getStackTrace(e);
        jss = jss.replace('\r', ' ');
        stackTrace.appendChild(document.createCDATASection(jss));
        error.appendChild(stackTrace);
        
        return document;
	}

	public static Document handleError(String sMessage) throws ParserConfigurationException {
		Document document = DOMUtils.createDocument();

        Element error = document.createElement("error");
        document.appendChild(error);

        Element message = document.createElement("message");
        Text text = document.createTextNode(sMessage == null ? "" : sMessage);
        message.appendChild(text);
        error.appendChild(message);

        return document;
	}
}
