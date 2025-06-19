/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.LogParameters;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.requesters.BinaryServletRequester;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.Log4jHelper;
import com.twinsoft.convertigo.engine.util.Log4jHelper.mdcKeys;

public class BinaryServlet extends GenericServlet {
	private static final long serialVersionUID = 8273215871882400280L;

	public BinaryServlet() {
	}

	public String getName() {
		return "BinaryServlet";
	}

	@Override
	public String getDefaultContentType() {
		return MimeType.OctetStream.value();
	}

	@Override
	public String getServletInfo() {
		return "Twinsoft Convertigo BinaryServlet";
	}

	@Override
	public String getDocumentExtension() {
		return ".bin";
	}

	@Override
	public Requester getRequester() {
		return new BinaryServletRequester();
	}

	@Override
	protected void doRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if ("true".equals(request.getParameter("__proxy_mode"))) {
			handleRemoteData(request, response, request.getParameter("remoteDataUrl"));
		} else {
			super.doRequest(request, response);
		}
	}

	private void handleRemoteData(HttpServletRequest request, HttpServletResponse response, String remoteDataUrl) throws ServletException, IOException {
		GetMethod method = null;
		try {
			HttpSession httpSession = request.getSession();

			LogParameters logParameters = GenericUtils.cast(httpSession.getAttribute(BinaryServlet.class.getCanonicalName()));
			if (logParameters == null) {
				httpSession.setAttribute(BinaryServlet.class.getCanonicalName(), logParameters = new LogParameters());
				logParameters.put(mdcKeys.ContextID.toString().toLowerCase(), httpSession.getId());
			}
			logParameters.put(mdcKeys.ClientIP.toString().toLowerCase(), request.getRemoteAddr());
			Log4jHelper.mdcSet(logParameters);

			if (SessionAttribute.authenticatedUser.string(request.getSession()) != null) {
				if (remoteDataUrl == null  || remoteDataUrl.isEmpty() ||
						(remoteDataUrl.indexOf("/.bin?") != -1 && remoteDataUrl.indexOf("__proxy_mode=true") != -1)) {
					throw new EngineException("Invalid remote data url: "+ remoteDataUrl);
				}

				Engine.logEngine.debug("(BinaryServlet) Requested remote data url: "+ remoteDataUrl);
				URL url = new java.net.URI(remoteDataUrl).toURL();

				HostConfiguration hostConfiguration = new HostConfiguration();
				hostConfiguration.setHost(new URI(url.toString(), true));
				HttpState httpState = new HttpState();
				Engine.theApp.proxyManager.setProxy(hostConfiguration, httpState, url);

				method = new GetMethod(url.toString());
				method.setFollowRedirects(false);

				int statuscode = Engine.theApp.httpClient.executeMethod(hostConfiguration, method, httpState);
				if (   statuscode == HttpStatus.SC_MOVED_TEMPORARILY
						|| statuscode == HttpStatus.SC_MOVED_PERMANENTLY
						|| statuscode == HttpStatus.SC_SEE_OTHER
						|| statuscode == HttpStatus.SC_TEMPORARY_REDIRECT) {

					String location = method.getResponseHeader("Location").getValue();
					Engine.logEngine.debug("(BinaryServlet) Redirecting to url: "+ location);
					method.releaseConnection();
					handleRemoteData(request, response, location);

				} else if (statuscode == HttpStatus.SC_OK) {
					Header[] headers = method.getResponseHeaders();
					StringBuffer buf = new StringBuffer();
					for (Header header: headers) {
						buf.append(header.getName() + " = " + header.getValue()).append(System.lineSeparator());
					}
					Engine.logEngine.debug("(BinaryServlet) Response headers: " + System.lineSeparator() + buf.toString());

					response.setStatus(statuscode);
					try {
						int contentLength = Integer.parseInt(method.getResponseHeader(HeaderName.ContentLength.value()).getValue(), 10);
						response.setContentLength(contentLength);
					} catch (Exception e) {}
					try {
						String contentDisposition = method.getResponseHeader(HeaderName.ContentDisposition.value()).getValue();
						response.setHeader(HeaderName.ContentDisposition.value(), contentDisposition);
					} catch (Exception e) {}
					try {
						String contentType = method.getResponseHeader(HeaderName.ContentType.value()).getValue();
						response.setContentType(contentType);
					} catch (Exception e) {
						response.setContentType(MimeType.Plain.value());
						Engine.logEngine.debug("(BinaryServlet) Data retrieved - content type set to " + response.getContentType());
					}

					OutputStream outputStream = response.getOutputStream();
					try (InputStream inputStream = method.getResponseBodyAsStream()) {
						IOUtils.copy(inputStream, outputStream );
					}

					Engine.logEngine.debug("(BinaryServlet) Data sent from remote url: "+ remoteDataUrl);
				} else {
					throw new EngineException("Unabled to retrieve remote data from " + remoteDataUrl + ". Remote server returned with status "+ statuscode);
				}
			} else {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				throw new EngineException("Authentication is required");
			}
		} catch (Exception e) {
			processException(request, response, e);
		} finally {
			Log4jHelper.mdcClear();
			removeSession(request, 1);
			if (method != null) {
				method.releaseConnection();
			}
		}
	}

}