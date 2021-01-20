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

package com.convertigo.gradle;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import com.twinsoft.convertigo.engine.CLI;
import com.twinsoft.convertigo.engine.localbuild.BuildLocally;

public class LocalBuild extends ConvertigoTask {
	List<String> platforms = Collections.emptyList();
	String mode = "debug";

	File iosProvisioningProfile = null;
	String iosSignIdentity = null;
	
	File androidKeystore = null;
	String androidKeystorePassword = null;
	String androidPassword = null;
	String androidAlias = null;
	
	File packageDestinationDir = null;

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
	public File getPackageDestinationDir() {
		return packageDestinationDir;
	}

	public void setPackageDestinationDir(File packageDestinationDir) {
		this.packageDestinationDir = packageDestinationDir;
	}
	
	public File getIosProvisioningProfile() {
		return iosProvisioningProfile;
	}

	public void setIosProvisioningProfile(File iosProvisioningProfile) {
		this.iosProvisioningProfile = iosProvisioningProfile;
	}

	public String getIosSignIdentity() {
		return iosSignIdentity;
	}

	public void setIosSignIdentity(String iosSignIdentity) {
		this.iosSignIdentity = iosSignIdentity;
	}

	public File getAndroidKeystore() {
		return androidKeystore;
	}

	public void setAndroidKeystore(File androidKeystore) {
		this.androidKeystore = androidKeystore;
	}

	public String getAndroidKeystorePassword() {
		return androidKeystorePassword;
	}

	public void setAndroidKeystorePassword(String androidKeystorePassword) {
		this.androidKeystorePassword = androidKeystorePassword;
	}

	public String getAndroidPassword() {
		return androidPassword;
	}

	public void setAndroidPassword(String androidPassword) {
		this.androidPassword = androidPassword;
	}

	public String getAndroidAlias() {
		return androidAlias;
	}

	public void setAndroidAlias(String androidAlias) {
		this.androidAlias = androidAlias;
	}

	public LocalBuild() {
		try {
			String platforms = getProject().getProperties().get("convertigo.localBuild.platforms").toString();
			setPlatforms(platforms);
		} catch (Exception e) {}
		
		iosProvisioningProfile = new File(getProject().getBuildDir(), "ios.mobileprovision");
		try {
			iosProvisioningProfile = new File(getProject().getProperties().get("convertigo.localBuild.iosProvisioningProfile").toString());
		} catch (Exception e) {}
		
		try {
			iosSignIdentity = getProject().getProperties().get("convertigo.localBuild.iosSignIdentity").toString();
		} catch (Exception e) {}
		
		androidKeystore = new File(getProject().getBuildDir(), "android.keystore");
		try {
			androidKeystore = new File(getProject().getProperties().get("convertigo.localBuild.androidKeystore").toString());
		} catch (Exception e) {}
		
		try {
			androidKeystorePassword = getProject().getProperties().get("convertigo.localBuild.androidKeystorePassword").toString();
		} catch (Exception e) {}
		
		try {
			androidAlias = getProject().getProperties().get("convertigo.localBuild.androidAlias").toString();
		} catch (Exception e) {}
		
		try {
			androidPassword = getProject().getProperties().get("convertigo.localBuild.androidPassword").toString();
		} catch (Exception e) {}
		
		try {
			mode = getProject().getProperties().get("convertigo.localBuild.mode").toString();
		} catch (Exception e) {}
		
		packageDestinationDir = new File(getProject().getBuildDir(), "localBuild");
		try {
			packageDestinationDir = new File(getProject().getProperties().get("convertigo.localBuild.packageDestinationDir").toString());
		} catch (Exception e) {}
	}
	
	@TaskAction
	void taskAction() throws Exception {
		CLI cli = plugin.getCLI();
		Map<String, BuildLocally> builds = cli.installCordova(plugin.load.getConvertigoProject(), platforms);
		cli.configureSignIOS(builds, iosProvisioningProfile, iosSignIdentity);
		cli.configureSignAndroid(builds, androidKeystore, androidKeystorePassword, androidAlias, androidPassword);
		cli.cordovaBuild(builds, mode);
		cli.movePackage(builds, packageDestinationDir);
	}
}
