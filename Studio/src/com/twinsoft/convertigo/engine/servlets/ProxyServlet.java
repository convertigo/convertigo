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

package com.twinsoft.convertigo.engine.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.proxy.translated.ParameterShuttle;
import com.twinsoft.convertigo.engine.proxy.translated.ProxyServletRequester;
import com.twinsoft.convertigo.engine.requesters.Requester;

public class ProxyServlet extends GenericServlet {

	private static final long serialVersionUID = -6850744063612272691L;

	public String getName() {
        return "ProxyServlet";
    }

    public String getDefaultContentType() {
    	return MimeType.Html.value();
    }

    public String getServletInfo() {
        return "Twinsoft Convertigo ProxyServlet";
    }
    
    public String getDocumentExtension() {
        return ".proxy";
    }
    
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
		try {
			ParameterShuttle infoShuttle = ProxyServletRequester.threadParameterShuttle.get();
			infoShuttle.userPostData = "";
			InputStream in = request.getInputStream();
			int c = in.read();
			while (c != -1) {
				infoShuttle.userPostData += (char) c;
				c = in.read();
			}

			Engine.logEngine.debug("(ProxyServlet) POST data: " + infoShuttle.userPostData);
		}
		catch(Exception e) {
			Engine.logEngine.error("Unexpected exception", e);
		}
		
		doRequest(request, response);
	}

    protected void doRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
    	try {
        	response.setBufferSize(0);

    		Object result = processRequest(request);
    		
			ParameterShuttle infoShuttle = ProxyServletRequester.threadParameterShuttle.get();

			// set headers
			Engine.logEngine.debug("(ProxyServlet) Outcoming HTTP headers:");
			String headerName, headerValue;
			for (int k = 0, kSize = infoShuttle.siteHeaderNames.size() ; k < kSize ; k++) {
				headerName = (String) infoShuttle.siteHeaderNames.get(k);
				headerValue = (String) infoShuttle.siteHeaderValues.get(k);
				response.setHeader(headerName, headerValue);
				Engine.logEngine.debug(headerName + "=" + headerValue);
			}

			response.setStatus(infoShuttle.httpCode);
    		Engine.logEngine.debug("(ProxyServlet) Response HTTP code: " + infoShuttle.httpCode);
			response.setContentType(infoShuttle.siteContentType);
    		Engine.logEngine.debug("(ProxyServlet) Response Content-Type: " + response.getContentType());
			response.setContentLength(infoShuttle.siteContentSize);
    		Engine.logEngine.debug("(ProxyServlet) Response Content-Length: " + infoShuttle.siteContentSize);
			
			try {
				// relay output
				if (result instanceof String) {
		    		HeaderName.Expires.addHeader(response, "-1");
		    		HeaderName.Pragma.addHeader(response, "no-cache");
		    		HeaderName.CacheControl.addHeader(response, "no-cache");
		    		Engine.logEngine.debug("(ProxyServlet) Response expires now");
		    		response.getWriter().write((String) result);
				}
				else {
					Calendar calendar = Calendar.getInstance();
					calendar.add(Calendar.YEAR, 1);
					int month = calendar.get(Calendar.MONTH) + 1;
					String expires = "Mon, " + calendar.get(Calendar.DATE) + "-" + (month < 10 ? "0" + month : "" + month) + "-" + calendar.get(Calendar.YEAR) + " 00:00:00 GMT";
		    		response.addHeader("Expires", expires);
		    		Engine.logEngine.debug("(ProxyServlet) Response expires: " + expires);

		    		response.getOutputStream().write((byte[]) result);
				}
			}
			catch(IOException e) {
				// The connection has probably been reset by peer
	    		Engine.logEngine.warn("[ProxyServlet] The connection has probably been reset by peer (IOException): " + e.getMessage());
			}
			
			infoShuttle.clear();
    	}
    	catch(Exception e) {
    		Engine.logEngine.error("Unable to process the request!", e);
    		request.getSession().setAttribute("proxy_exception", e);
    		RequestDispatcher rd = request.getRequestDispatcher("error.jsp");
    		rd.forward(request, response);
    	}
    }

    public Requester getRequester() {
		return new ProxyServletRequester();
    }

}