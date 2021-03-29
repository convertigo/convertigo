/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.utils;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "EchoHttp",
		roles = { Role.ANONYMOUS },
		parameters = {},
		returnValue = "",
		admin = false
		)
public class EchoHttp extends XmlService {

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element root = document.getDocumentElement();

		Element method = document.createElement("method");
		method.setTextContent(request.getMethod());
		root.appendChild(method);

		Element url = document.createElement("url");
		url.setTextContent(request.getRequestURL().toString());
		root.appendChild(url);
		
		String queryString = request.getQueryString();
		if (StringUtils.isNotEmpty(queryString)) {
			Element query = document.createElement("query");
			query.setTextContent(queryString);
			root.appendChild(query);
		}
		
		Element headers = document.createElement("headers");

		for (Enumeration<String> headerEnum = request.getHeaderNames(); headerEnum.hasMoreElements();) {
			String headerName = headerEnum.nextElement();

			for (Enumeration<String> valueEnum = request.getHeaders(headerName); valueEnum.hasMoreElements();) {
				String headerValue = valueEnum.nextElement();

				Element header = document.createElement("header");
				header.setAttribute("name", headerName);
				header.setTextContent(headerValue);
				headers.appendChild(header);
			}
		}

		root.appendChild(headers);
	}
}
