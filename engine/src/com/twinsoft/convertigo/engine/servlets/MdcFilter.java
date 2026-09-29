/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.servlets;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.MDC;

import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.util.Log4jHelper;

/** Owns the MDC for the whole HTTP dispatch, including the other filters. */
public class MdcFilter implements Filter {
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		Log4jHelper.mdcClear();
		try {
			// Set request flags before aliases can forward past ProjectsDataFilter.
			if (request instanceof HttpServletRequest httpRequest) {
				String query = httpRequest.getQueryString();
				if (HeaderName.XConvertigoNoLog.has(httpRequest)
						|| (query != null && query.matches("(.*&)?__nolog=true(&.*)?"))
						|| httpRequest.getRequestURI().contains("/system/projects/")) {
					MDC.put("nolog", true);
				}
			}
			chain.doFilter(request, response);
		} finally {
			Log4jHelper.mdcClear();
		}
	}
}
