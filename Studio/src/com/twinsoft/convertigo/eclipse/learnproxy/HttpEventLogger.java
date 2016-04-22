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

package com.twinsoft.convertigo.eclipse.learnproxy;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import com.twinsoft.convertigo.eclipse.learnproxy.http.gui.HttpProxyEvent;
import com.twinsoft.convertigo.eclipse.learnproxy.http.gui.HttpProxyEventListener;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class HttpEventLogger implements  HttpProxyEventListener {

	public void modelChanged(HttpProxyEvent event) {
			Hashtable<String, Object> htResponse = parseHttpHeaders(event.getResponse(), false);
			String	contentType = (String)htResponse.get(HeaderName.ContentType.value().toLowerCase());
			

			// record only text/html or null Content-Type ...
			if (!MimeType.Html.is(contentType)) {
				return;
			}
			
			Hashtable<String, Object> htRequest  = parseHttpHeaders(event.getRequest(), true);
			Engine.logEngine.debug("________________________________ Request ______________________________________________________________");
			Engine.logEngine.debug(event.getRequest());	
			dumpHeaders(htRequest);	
			//Engine.logEngine.debug("________________________________ Response        ______________________________________________________");
			//Engine.logEngine.debug(event.getResponse());	
			Engine.logEngine.debug("________________________________ Parsed Response  Headers _____________________________________________");
			dumpHeaders(htResponse);
			Engine.logEngine.debug("________________________________  Info   ______________________________________________________________");
			Engine.logEngine.debug("elapsedTime   : " + event.getElapsedTime());
			Engine.logEngine.debug("status        : " + event.getStatus());
			Engine.logEngine.debug("requestStarted: " + event.getRequestStarted());
			Engine.logEngine.debug("size          : " + event.getSize());
			Engine.logEngine.debug("path          : " + event.getPath());
			Engine.logEngine.debug("method        : " + event.getMethod());
			Engine.logEngine.debug("=======================================================================================================");
	}

	public void operationChanged(String operation) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Parses a HTTP request or response. The result is returned in a Hashtable containg all the headers and one element containg the POST
	 * data named 'data'. The post data is also an Hashtable containg all the name/values couples.
	 * Set bParseData to false to prevent parsing the http data (Usually set bParseData to true only for requests).
	 *  
	 * @param 	data
	 * @param 	bParseData
	 * @return	Hashtable containing the parsed data
	 */
	private Hashtable<String, Object> parseHttpHeaders(String data, boolean bParseData)
	{
		Hashtable<String, Object> ht = new Hashtable<String, Object>();
		String		element;
		int			index;
		String		httpHeader;
		String		httpValue;
		try {
			StringTokenizer st = new StringTokenizer(data, "\r");
			while (st.hasMoreTokens()) {
				element =  st.nextToken();
				if (element.trim().length() > 0) {
					// line is not empty ==> This can be header or HTTP verb
					if ((index = element.indexOf(":")) != -1) {
						// this is an Header ==> Crack the form <\r><headername>:<space><headervalue>
						httpHeader = element.substring(1, index).toLowerCase(); 	// skip the first '\r'
						httpValue  = element.substring(index+2).toLowerCase();	    // skip the space after ':'
						ht.put(httpHeader, httpValue);
					}
				} else {
					// there was an empty line ==> Next token is DATA
					if (bParseData) {
						element =  st.nextToken();
						// now parse the data (This can be a POST)
						Hashtable<String, String> htData = new Hashtable<String, String>();
						StringTokenizer stData = new StringTokenizer(element, "&");
						String	dataElement, dataName, dataValue;
						while (stData.hasMoreTokens()) {
							dataElement = stData.nextToken();
							if (dataElement.indexOf('=') != -1) {
								dataName = dataElement.substring(0, dataElement.indexOf('='));
								if (dataElement.indexOf('=') < dataElement.length()) {
									dataValue = dataElement.substring(dataElement.indexOf('=')+1);
									htData.put(dataName.trim(), dataValue.trim());
								} else
									htData.put(dataName.trim(), "");
							}
						}
						ht.put("data", htData);
					}
					break;
				}
			}
		}
		catch (Exception e) {
			;
		}
		return ht;
	}
	
	void dumpHeaders(Hashtable<String, Object> ht)
	{
		Enumeration<String> keys = ht.keys();
		Engine.logEngine.debug("Headers");
		Hashtable<String, String> htData;
		Enumeration<String> dataKeys;
		String headerName, dataVariable;
		while (keys.hasMoreElements()) {
			headerName = keys.nextElement();
			if (headerName.equalsIgnoreCase("data")) {
				htData = GenericUtils.cast(ht.get("data"));
				dataKeys = htData.keys();
				Engine.logEngine.debug("        POST Data:");
				while (dataKeys.hasMoreElements()) {
					dataVariable = (String)dataKeys.nextElement();
					Engine.logEngine.debug("           " + dataVariable + "=[" + htData.get(dataVariable)+"]");
				}
			} else 
				Engine.logEngine.debug("        " + headerName + ": " + ht.get(headerName));
		}
	}
}
