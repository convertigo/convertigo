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

package com.twinsoft.convertigo.engine.admin.services.mobiles;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.URLUtils;

@ServiceDefinition(name = "LaunchBuild", roles = { Role.ANONYMOUS }, parameters = {}, returnValue = "")
public class LaunchBuild extends XmlService {

	private static final Object buildLock = new Object();
	
	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		synchronized(buildLock) {
			String application = request.getParameter("application");
			final MobileResourceHelper mobileResourceHelper = new MobileResourceHelper(application, "_private/mobile/www");
			String finalApplicationName = mobileResourceHelper.mobileApplication.getComputedApplicationName();
			
			File mobileArchiveFile = MobileResourceHelper.makeZipPackage(request);
			
			// Login to the mobile builder platform
			String mobileBuilderPlatformURL = EnginePropertiesManager
					.getProperty(PropertyName.MOBILE_BUILDER_PLATFORM_URL);
			String mobileBuilderPlatformUsername = EnginePropertiesManager
					.getProperty(PropertyName.MOBILE_BUILDER_USERNAME);
			String mobileBuilderPlatformPassword = EnginePropertiesManager
					.getProperty(PropertyName.MOBILE_BUILDER_PASSWORD);
			
			String mobileBuilderIOSCertificateTitle = EnginePropertiesManager
					.getProperty(PropertyName.MOBILE_BUILDER_IOS_CERTIFICATE_TITLE);
			String mobileBuilderIOSCertificatePw = EnginePropertiesManager
					.getProperty(PropertyName.MOBILE_BUILDER_IOS_CERTIFICATE_PW);
			String mobileBuilderAndroidCertificateTitle = EnginePropertiesManager
					.getProperty(PropertyName.MOBILE_BUILDER_ANDROID_CERTIFICATE_TITLE);
			String mobileBuilderAndroidCertificatePw = EnginePropertiesManager
					.getProperty(PropertyName.MOBILE_BUILDER_ANDROID_CERTIFICATE_PW);
			String mobileBuilderAndroidKeystorePw = EnginePropertiesManager
					.getProperty(PropertyName.MOBILE_BUILDER_ANDROID_KEYSTORE_PW);
			String mobileBuilderBBKeyTitle = EnginePropertiesManager
					.getProperty(PropertyName.MOBILE_BUILDER_BB_KEY_TITLE);
			String mobileBuilderBBKeyPw = EnginePropertiesManager
					.getProperty(PropertyName.MOBILE_BUILDER_BB_KEY_PW);
			
			PostMethod method;
			int methodStatusCode;
			InputStream methodBodyContentInputStream;
			
			Map<String, String[]> params = new HashMap<String, String[]>();
			params.put("application", new String[]{finalApplicationName});
			params.put("username", new String[]{mobileBuilderPlatformUsername});
			params.put("password", new String[]{mobileBuilderPlatformPassword});
			
			params.put("iOSCertificateTitle", new String[]{mobileBuilderIOSCertificateTitle});
			params.put("iOSCertificatePw", new String[]{mobileBuilderIOSCertificatePw});
			params.put("androidCertificateTitle", new String[]{mobileBuilderAndroidCertificateTitle});
			params.put("androidCertificatePw", new String[]{mobileBuilderAndroidCertificatePw});
			params.put("androidKeystorePw", new String[]{mobileBuilderAndroidKeystorePw});
			params.put("bbKeyTitle", new String[]{mobileBuilderBBKeyTitle});
			params.put("bbKeyPw", new String[]{mobileBuilderBBKeyPw});

			// Launch the mobile build
			URL url = new URL(mobileBuilderPlatformURL + "/build?" + URLUtils.mapToQuery(params));
			
			HostConfiguration hostConfiguration = new HostConfiguration();
			hostConfiguration.setHost(new URI(url.toString(), true));
			HttpState httpState = new HttpState();
			Engine.theApp.proxyManager.setProxy(hostConfiguration, httpState, url);
			
			method = new PostMethod(url.toString());
			
			FileRequestEntity entity = new FileRequestEntity(mobileArchiveFile, null);
			method.setRequestEntity(entity);

			methodStatusCode = Engine.theApp.httpClient.executeMethod(hostConfiguration, method, httpState);
			methodBodyContentInputStream = method.getResponseBodyAsStream();
			byte[] httpBytes = IOUtils.toByteArray(methodBodyContentInputStream);
			String sResult = new String(httpBytes, "UTF-8");

			if (methodStatusCode != HttpStatus.SC_OK) {
				throw new ServiceException("Unable to build application '" + finalApplicationName + "'; reason: " + sResult);
			}

			JSONObject jsonObject = new JSONObject(sResult);
			Element statusElement = document.createElement("application");
			statusElement.setAttribute("id", jsonObject.getString("id"));
			document.getDocumentElement().appendChild(statusElement);
		}
	}
}
