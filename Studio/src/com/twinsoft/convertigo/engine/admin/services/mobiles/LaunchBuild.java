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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.beans.mobileplatforms.Android;
import com.twinsoft.convertigo.beans.mobileplatforms.BlackBerryKeyProvider;
import com.twinsoft.convertigo.beans.mobileplatforms.IOs;
import com.twinsoft.convertigo.beans.mobileplatforms.WindowsPhoneKeyProvider;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.AuthenticationException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.util.URLUtils;

@ServiceDefinition(name = "LaunchBuild", roles = { Role.ANONYMOUS }, parameters = {}, returnValue = "")
public class LaunchBuild extends XmlService {

	private static final Object buildLock = new Object();
	
	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		synchronized(buildLock) {
			final MobileResourceHelper mobileResourceHelper = new MobileResourceHelper(request, "mobile/www");
			MobileApplication mobileApplication = mobileResourceHelper.mobileApplication;
			
			if (mobileApplication == null) {
				throw new ServiceException("no such mobile application");
			} else {
				boolean bAdminRole = Engine.authenticatedSessionManager.hasRole(request.getSession(), Role.WEB_ADMIN);
				if (!bAdminRole && mobileApplication.getAccessibility() == Accessibility.Private) {
					throw new AuthenticationException("Authentication failure: user has not sufficient rights!");
				}
			}
			
			MobilePlatform mobilePlatform = mobileResourceHelper.mobilePlatform;
			
			String finalApplicationName = mobileApplication.getComputedApplicationName();
			File mobileArchiveFile = mobileResourceHelper.makeZipPackage();
			
			// Login to the mobile builder platform
			String mobileBuilderPlatformURL = EnginePropertiesManager.getProperty(PropertyName.MOBILE_BUILDER_PLATFORM_URL);
			
			Map<String, String[]> params = new HashMap<String, String[]>();
			
			params.put("application", new String[]{finalApplicationName});
			params.put("platformName", new String[]{mobilePlatform.getName()});
			params.put("platformType", new String[]{mobilePlatform.getType()});
			params.put("auth_token", new String[]{mobileApplication.getComputedAuthenticationToken()});
			
			//iOS
			if (mobilePlatform instanceof IOs) {
				IOs ios = (IOs) mobilePlatform;
				
				String pw, title = ios.getiOSCertificateTitle();
				
				if (!title.equals("")) {
					pw = ios.getiOSCertificatePw();
				} else {
					title = EnginePropertiesManager.getProperty(PropertyName.MOBILE_BUILDER_IOS_CERTIFICATE_TITLE);
					pw = EnginePropertiesManager.getProperty(PropertyName.MOBILE_BUILDER_IOS_CERTIFICATE_PW);
				}
				
				params.put("iOSCertificateTitle", new String[]{title});
				params.put("iOSCertificatePw", new String[]{pw});
			}
			
			//Android
			if (mobilePlatform instanceof Android) {
				Android android = (Android) mobilePlatform;
				
				String certificatePw, keystorePw, title = android.getAndroidCertificateTitle();
				
				if (!title.equals("")) {
					certificatePw = android.getAndroidCertificatePw();
					keystorePw = android.getAndroidKeystorePw();
				} else {
					title = EnginePropertiesManager.getProperty(PropertyName.MOBILE_BUILDER_ANDROID_CERTIFICATE_TITLE);
					certificatePw = EnginePropertiesManager.getProperty(PropertyName.MOBILE_BUILDER_ANDROID_CERTIFICATE_PW);
					keystorePw = EnginePropertiesManager.getProperty(PropertyName.MOBILE_BUILDER_ANDROID_KEYSTORE_PW);
				}
				
				params.put("androidCertificateTitle", new String[]{title});
				params.put("androidCertificatePw", new String[]{certificatePw});
				params.put("androidKeystorePw", new String[]{keystorePw});
			}
			
			//Blackberry
			if (mobilePlatform instanceof BlackBerryKeyProvider) { 
				BlackBerryKeyProvider blackberry = (BlackBerryKeyProvider) mobilePlatform;
				
				String pw, title = blackberry.getBbKeyTitle();
				
				if (!title.equals("")) {
					pw = blackberry.getBbKeyPw();
				} else {
					title = EnginePropertiesManager.getProperty(PropertyName.MOBILE_BUILDER_BB_KEY_TITLE);
					pw = EnginePropertiesManager.getProperty(PropertyName.MOBILE_BUILDER_BB_KEY_PW);
				}
				
				params.put("bbKeyTitle", new String[]{title});
				params.put("bbKeyPw", new String[]{pw});
			}
			
			//Windows Phone
			if (mobilePlatform instanceof WindowsPhoneKeyProvider) { 
				WindowsPhoneKeyProvider windowsPhone = (WindowsPhoneKeyProvider) mobilePlatform;
				
				String title = windowsPhone.getWinphonePublisherIdTitle();
				
				if (title.equals("")) {
					title = EnginePropertiesManager.getProperty(PropertyName.MOBILE_BUILDER_WINDOWSPHONE_PUBLISHER_ID_TITLE);
				}
				
				params.put("winphonePublisherIdTitle", new String[]{title});
			}
			
			// Launch the mobile build
			URL url = new URL(mobileBuilderPlatformURL + "/build?" + URLUtils.mapToQuery(params));
			
			HostConfiguration hostConfiguration = new HostConfiguration();
			hostConfiguration.setHost(url.getHost());
			HttpState httpState = new HttpState();
			Engine.theApp.proxyManager.setProxy(hostConfiguration, httpState, url);
			
			PostMethod method = new PostMethod(url.toString());
			
			FileRequestEntity entity = new FileRequestEntity(mobileArchiveFile, null);
			method.setRequestEntity(entity);

			int methodStatusCode = Engine.theApp.httpClient.executeMethod(hostConfiguration, method, httpState);
			String sResult = IOUtils.toString(method.getResponseBodyAsStream(), "UTF-8");
			
			if (methodStatusCode != HttpStatus.SC_OK) {
				throw new ServiceException("Unable to build application '" + finalApplicationName + "'.\n" + sResult);
			}
			
			JSONObject jsonObject = new JSONObject(sResult);
			Element statusElement = document.createElement("application");
			statusElement.setAttribute("id", jsonObject.getString("id"));
			document.getDocumentElement().appendChild(statusElement);
		}
	}
}
