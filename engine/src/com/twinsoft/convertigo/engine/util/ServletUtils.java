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

package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;

public class ServletUtils {
	public static void handleFileFilter(File file, HttpServletRequest request, HttpServletResponse response, FilterConfig filterConfig, FilterChain chain) throws IOException, ServletException {
		if (file.exists()) {
			Engine.logContext.debug("Static file");

			// Warning date comparison: 'If-Modified-Since' header precision is second,
			// although file date precision is milliseconds on Windows
			long clientDate = request.getDateHeader("If-Modified-Since") / 1000;
			Engine.logContext.debug("If-Modified-Since: " + clientDate);
			long fileDate = file.lastModified() / 1000;
			Engine.logContext.debug("File date: " + fileDate);
			if (clientDate == fileDate) {
				Engine.logContext.debug("Returned HTTP 304 Not Modified");
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			}
			else {
				long maxAge = EnginePropertiesManager.getPropertyAsLong(PropertyName.NET_MAX_AGE);

				// Serve static files if they exist in the projects repository.
				String mimeType = filterConfig.getServletContext().getMimeType(file.getName());
				Engine.logContext.debug("Found MIME type: " + mimeType);
				HeaderName.ContentType.setHeader(response, mimeType);
				HeaderName.CacheControl.setHeader(response, "max-age=" + maxAge	);
				HeaderName.ContentLength.setHeader(response, "" + file.length());
				response.setDateHeader(HeaderName.LastModified.value(), file.lastModified());

				FileInputStream fileInputStream = null;
				OutputStream output = response.getOutputStream();
				try {
					fileInputStream = new FileInputStream(file);
					IOUtils.copy(fileInputStream, output);
				}
				finally {
					if (fileInputStream != null) {
						fileInputStream.close();
					}
				}
			}
		}
		else {
			Engine.logContext.debug("Convertigo request => follow the normal filter chain");
			chain.doFilter(request, response);
		}
	}

	public static void applyCustomHeaders(HttpServletRequest request, HttpServletResponse response) {
		Map<String, String> headers = RequestAttribute.responseHeader.get(request);
		String user = SessionAttribute.authenticatedUser.string(request.getSession(false));
		if (user != null) {
			user = Base64.encodeBase64String(DigestUtils.sha1(request.getSession().getId() + user));
			HeaderName.XConvertigoAuthenticated.addHeader(response, user);
			HeaderName.AccessControlExposeHeaders.addHeader(response, HeaderName.XConvertigoAuthenticated.value());
		}
		if (headers != null) {
			Engine.logContext.debug("Setting custom response headers (" + headers.size() + ")");
			for (Entry<String, String> header : headers.entrySet()) {
				Engine.logContext.debug("Setting custom response header: " + header.getKey() + "=" + header.getValue());
				response.setHeader(header.getKey(), header.getValue());
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static void applyCustomStatus(HttpServletRequest request, HttpServletResponse response) {
		Map<Integer, String> status = RequestAttribute.responseStatus.get(request);
		if (status != null) {
			Engine.logContext.debug("Setting custom response status");
			if (!status.isEmpty()) {
				Entry<Integer, String> entry = status.entrySet().iterator().next();
				Engine.logContext.debug("Setting custom response status: " + entry.getKey() + "=" + entry.getValue());
				response.setStatus(entry.getKey(), entry.getValue());
				//response.setStatus(entry.getKey());
			}
		}
	}

}
