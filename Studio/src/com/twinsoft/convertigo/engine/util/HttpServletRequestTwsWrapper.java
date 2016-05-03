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

package com.twinsoft.convertigo.engine.util;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class HttpServletRequestTwsWrapper extends HttpServletRequestWrapper {	
	protected Map<String, String[]> parameters = new HashMap<String, String[]>();
	
	public HttpServletRequestTwsWrapper(HttpServletRequest request) {
		super(request);
		try {
			if (request.getCharacterEncoding() == null) {
				request.setCharacterEncoding("UTF-8");
			}
			// parse GET parameters
			addQuery(request.getQueryString());
			
			// retrieve POST parameters ( == not defined in GET )
			for (Entry<String, String[]> entry : GenericUtils.<Map<String, String[]>>cast(request.getParameterMap()).entrySet()) {
				if (!parameters.containsKey(entry.getKey())) {
					parameters.put(entry.getKey(), entry.getValue());
				}
			}
		} catch (UnsupportedEncodingException e) {
			parameters.clear();
			parameters.putAll(GenericUtils.<Map<String, String[]>>cast(request.getParameterMap()));
		}
	}
	
	public Map<String, String[]> getParameterMap() {
		return Collections.unmodifiableMap(parameters);
	}

	public Enumeration<String> getParameterNames() {
		return Collections.enumeration(parameters.keySet());
	}

	@Override
	public String getParameter(String key) {
		String[] list = parameters.get(key);
		return (list != null && list.length > 0) ? list[0] : null;
	}
	
	@Override
	public String[] getParameterValues(String key) {
		String[] list = parameters.get(key);
		return (list != null) ? list.clone() : null;
	}
	
	public void addParameter(String key, String value) {
		String[] list = parameters.get(key);
		if (list != null) {
			list = GenericUtils.copyOf(list, list.length + 1);
			list[list.length - 1] = value;
		} else {
			list = new String[]{value};
		}
		parameters.put(key, list);
	}
	
	public void clearParameters() {
		parameters.clear();
	}
	
	public void addQuery(String query) {
		if (query != null) {
			parameters = URLUtils.queryToMap(query);
		}
	}
}