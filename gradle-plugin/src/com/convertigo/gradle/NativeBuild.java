/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.convertigo.gradle;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class NativeBuild extends ConvertigoTask {
	List<String> platforms = Collections.emptyList();
	String authenticationToken = null;
	String platformURL = null;
	String iosCertificateTitle = null;
	String iosCertificatePassword = null;
	String androidCertificateTitle = null;
	String androidCertificatePassword = null;
	String androidCertificateKeystorePassword = null;
	
	@Input @Optional
	public List<String> getPlatforms() {
		return platforms;
	}

	public void setPlatforms(List<String> platforms) {
		this.platforms = platforms;
	}

	public void setPlatforms(String platforms) {
		this.platforms = Arrays.asList(platforms.split(","));
	}

	@Input @Optional
	public String getAuthenticationToken() {
		return authenticationToken;
	}

	public void setAuthenticationToken(String authenticationToken) {
		this.authenticationToken = authenticationToken;
	}

	@Input @Optional
	public String getPlatformURL() {
		return platformURL;
	}

	public void setPlatformURL(String platformURL) {
		this.platformURL = platformURL;
	}

	@Input @Optional
	public String getIosCertificateTitle() {
		return iosCertificateTitle;
	}

	public void setIosCertificateTitle(String iosCertificateTitle) {
		this.iosCertificateTitle = iosCertificateTitle;
	}

	@Input @Optional
	public String getIosCertificatePassword() {
		return iosCertificatePassword;
	}

	public void setIosCertificatePassword(String iosCertificatePassword) {
		this.iosCertificatePassword = iosCertificatePassword;
	}

	@Input @Optional
	public String getAndroidCertificateTitle() {
		return androidCertificateTitle;
	}

	public void setAndroidCertificateTitle(String androidCertificateTitle) {
		this.androidCertificateTitle = androidCertificateTitle;
	}

	@Input @Optional
	public String getAndroidCertificatePassword() {
		return androidCertificatePassword;
	}

	public void setAndroidCertificatePassword(String androidCertificatePassword) {
		this.androidCertificatePassword = androidCertificatePassword;
	}

	@Input @Optional
	public String getAndroidCertificateKeystorePassword() {
		return androidCertificateKeystorePassword;
	}

	public void setAndroidCertificateKeystorePassword(String androidCertificateKeystorePassword) {
		this.androidCertificateKeystorePassword = androidCertificateKeystorePassword;
	}

	public NativeBuild() {
		try {
			String platforms = getProject().getProperties().get("convertigo.nativeBuild.platforms").toString();
			setPlatforms(platforms);
		} catch (Exception e) {}
		
		try {
			authenticationToken = getProject().getProperties().get("convertigo.nativeBuild.authenticationToken").toString();
		} catch (Exception e) {}
		
		try {
			platformURL = getProject().getProperties().get("convertigo.nativeBuild.platformURL").toString();
		} catch (Exception e) {}
		
		try {
			iosCertificateTitle = getProject().getProperties().get("convertigo.nativeBuild.iosCertificateTitle").toString();
		} catch (Exception e) {}
		
		try {
			iosCertificatePassword = getProject().getProperties().get("convertigo.nativeBuild.iosCertificatePassword").toString();
		} catch (Exception e) {}
		
		try {
			androidCertificateTitle = getProject().getProperties().get("convertigo.nativeBuild.androidCertificateTitle").toString();
		} catch (Exception e) {}
		
		try {
			androidCertificatePassword = getProject().getProperties().get("convertigo.nativeBuild.androidCertificatePassword").toString();
		} catch (Exception e) {}
		
		try {
			androidCertificateKeystorePassword = getProject().getProperties().get("convertigo.nativeBuild.androidCertificateKeystorePassword").toString();
		} catch (Exception e) {}
		
	}
	
	@TaskAction
	void taskAction() throws Exception {
		if (StringUtils.isNotBlank(authenticationToken)) {
			EnginePropertiesManager.setProperty(PropertyName.MOBILE_BUILDER_AUTHENTICATION_TOKEN, authenticationToken);
		}
		if (StringUtils.isNotBlank(platformURL)) {
			EnginePropertiesManager.setProperty(PropertyName.MOBILE_BUILDER_PLATFORM_URL, platformURL);
		}
		if (StringUtils.isNotBlank(iosCertificateTitle)) {
			EnginePropertiesManager.setProperty(PropertyName.MOBILE_BUILDER_IOS_CERTIFICATE_TITLE, iosCertificateTitle);
		}
		if (StringUtils.isNotBlank(iosCertificatePassword)) {
			EnginePropertiesManager.setProperty(PropertyName.MOBILE_BUILDER_IOS_CERTIFICATE_PW, iosCertificatePassword);
		}
		if (StringUtils.isNotBlank(androidCertificateTitle)) {
			EnginePropertiesManager.setProperty(PropertyName.MOBILE_BUILDER_ANDROID_CERTIFICATE_TITLE, androidCertificateTitle);
		}
		if (StringUtils.isNotBlank(androidCertificatePassword)) {
			EnginePropertiesManager.setProperty(PropertyName.MOBILE_BUILDER_ANDROID_CERTIFICATE_PW, androidCertificatePassword);
		}
		if (StringUtils.isNotBlank(androidCertificateKeystorePassword)) {
			EnginePropertiesManager.setProperty(PropertyName.MOBILE_BUILDER_ANDROID_KEYSTORE_PW, androidCertificateKeystorePassword);
		}
	}
}
