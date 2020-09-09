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

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import com.twinsoft.convertigo.engine.CLI;
import com.twinsoft.convertigo.engine.localbuild.BuildLocally;

public class LocalBuild extends ConvertigoTask {
	List<String> platforms = Collections.emptyList();
	String mode = "debug";
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
	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
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

	public LocalBuild() {
		try {
			String platforms = getProject().getProperties().get("convertigo.localBuild.platforms").toString();
			setPlatforms(platforms);
		} catch (Exception e) {}
		
		try {
			authenticationToken = getProject().getProperties().get("convertigo.localBuild.authenticationToken").toString();
		} catch (Exception e) {}
		
		try {
			platformURL = getProject().getProperties().get("convertigo.localBuild.platformURL").toString();
		} catch (Exception e) {}
		
		try {
			iosCertificateTitle = getProject().getProperties().get("convertigo.localBuild.iosCertificateTitle").toString();
		} catch (Exception e) {}
		
		try {
			iosCertificatePassword = getProject().getProperties().get("convertigo.localBuild.iosCertificatePassword").toString();
		} catch (Exception e) {}
		
		try {
			androidCertificateTitle = getProject().getProperties().get("convertigo.localBuild.androidCertificateTitle").toString();
		} catch (Exception e) {}
		
		try {
			androidCertificatePassword = getProject().getProperties().get("convertigo.localBuild.androidCertificatePassword").toString();
		} catch (Exception e) {}
		
		try {
			androidCertificateKeystorePassword = getProject().getProperties().get("convertigo.localBuild.androidCertificateKeystorePassword").toString();
		} catch (Exception e) {}
	}
	
	@TaskAction
	void taskAction() throws Exception {
		CLI cli = plugin.getCLI();
		List<BuildLocally> builds = cli.installCordova(plugin.load.getConvertigoProject(), platforms);
		cli.cordovaBuild(builds, mode);
	}
}
