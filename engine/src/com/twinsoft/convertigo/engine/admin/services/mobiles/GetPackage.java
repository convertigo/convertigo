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

package com.twinsoft.convertigo.engine.admin.services.mobiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.AuthenticationException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.mobiles.MobileResourceHelper.Keys;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;

@ServiceDefinition(
		name = "GetBuildUrl",
		roles = { Role.ANONYMOUS },
		parameters = {},
		returnValue = ""
		)
public class GetPackage extends DownloadService {

	@Override
	protected void writeResponseResult(HttpServletRequest request, HttpServletResponse response) throws  Exception {
		String project = Keys.project.value(request);

		MobileApplication mobileApplication = GetBuildStatus.getMobileApplication(project);

		if (mobileApplication == null) {
			throw new ServiceException("no such mobile application");
		} else {
			boolean bTpPrivateRole = Engine.authenticatedSessionManager.hasRole(request.getSession(), Role.TEST_PLATFORM_PRIVATE);
			if (!bTpPrivateRole && mobileApplication.getAccessibility() == Accessibility.Private) {
				throw new AuthenticationException("Authentication failure: user has not sufficient rights!");
			}
		}

		String platformName = Keys.platform.value(request);
		HttpMethod method = null;
		try {
			method = perform(mobileApplication, platformName, request);

			try {
				String contentDisposition = method.getResponseHeader(HeaderName.ContentDisposition.value()).getValue();
				HeaderName.ContentDisposition.setHeader(response, contentDisposition);
			} catch (Exception e) {
				HeaderName.ContentDisposition.setHeader(response, "attachment; filename=\"" + project + "\"");
			} 

			try {
				response.setContentType(method.getResponseHeader(HeaderName.ContentType.value()).getValue());
			} catch (Exception e) {
				response.setContentType(MimeType.OctetStream.value());
			} 

			OutputStream responseOutputStream = response.getOutputStream();
			IOUtils.copy(method.getResponseBodyAsStream(), responseOutputStream);
		} catch (IOException ioex) { // Fix for ticket #4698
			if (!ioex.getClass().getSimpleName().equalsIgnoreCase("ClientAbortException")) {
				// fix for #5042
				throw ioex;
			}
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}
	}

	public static HttpMethod perform(MobileApplication mobileApplication, String platformName, HttpServletRequest request) throws Exception {
		String finalApplicationName = mobileApplication.getComputedApplicationName();		
		String mobileBuilderPlatformURL = EnginePropertiesManager.getProperty(PropertyName.MOBILE_BUILDER_PLATFORM_URL);

		PostMethod method;
		int methodStatusCode;
		InputStream methodBodyContentInputStream;

		URL url = new java.net.URI(mobileBuilderPlatformURL + "/getpackage").toURL();

		HostConfiguration hostConfiguration = new HostConfiguration();
		hostConfiguration.setHost(new URI(url.toString(), true));
		HttpState httpState = new HttpState();
		Engine.theApp.proxyManager.setProxy(hostConfiguration, httpState, url);

		method = new PostMethod(url.toString());
		HeaderName.ContentType.setRequestHeader(method, MimeType.WwwForm.value());
		method.setRequestBody(new NameValuePair[] {
				new NameValuePair("application", finalApplicationName),
				new NameValuePair("platformName", platformName),
				new NameValuePair("auth_token", mobileApplication.getComputedAuthenticationToken()),
				new NameValuePair("endpoint", mobileApplication.getComputedEndpoint(request))
		});

		methodStatusCode = Engine.theApp.httpClient.executeMethod(hostConfiguration, method, httpState);
		methodBodyContentInputStream = method.getResponseBodyAsStream();

		if (methodStatusCode != HttpStatus.SC_OK) {
			byte[] httpBytes = IOUtils.toByteArray(methodBodyContentInputStream);
			String sResult = new String(httpBytes, "UTF-8");
			throw new ServiceException("Unable to get package for project '" + mobileApplication.getProject() + "' (final app name: '" + finalApplicationName + "').\n" + sResult);
		}
		return method;
	}
}
