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

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import com.twinsoft.convertigo.engine.CLI;

public class ProjectDeploy extends ConvertigoTask {
	private String server = "";
	private String user = "admin";
	private String password = "admin";
	private boolean trustAllCertificates = false;
	private boolean assembleXsl = false;
	private int retry = 5;
	
	@Input @Optional
	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	@Input @Optional
	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	@Input @Optional
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Input @Optional
	public boolean isTrustAllCertificates() {
		return trustAllCertificates;
	}

	public void setTrustAllCertificates(boolean trustAllCertificates) {
		this.trustAllCertificates = trustAllCertificates;
	}

	@Input @Optional
	public boolean isAssembleXsl() {
		return assembleXsl;
	}

	public void setAssembleXsl(boolean assembleXsl) {
		this.assembleXsl = assembleXsl;
	}

	@Input @Optional
	public int getRetry() {
		return retry;
	}

	public void setRetry(int retry) {
		this.retry = retry;
	}

	public ProjectDeploy() {
		try {
			server = getProject().getProperties().get("convertigo.deploy.server").toString();
		} catch (Exception e) {}
		try {
			user = getProject().getProperties().get("convertigo.deploy.user").toString();
		} catch (Exception e) {}
		try {
			password = getProject().getProperties().get("convertigo.deploy.password").toString();
		} catch (Exception e) {}
		try {
			trustAllCertificates = Boolean.parseBoolean(getProject().getProperties().get("convertigo.deploy.trustAllCertificates").toString());
		} catch (Exception e) {}
		try {
			assembleXsl = Boolean.parseBoolean(getProject().getProperties().get("convertigo.deploy.assembleXsl").toString());
		} catch (Exception e) {}
		try {
			retry = Integer.parseInt(getProject().getProperties().get("convertigo.deploy.retry").toString());
		} catch (Exception e) {}
	}
	
	@TaskAction
	void taskAction() throws Exception {
		CLI cli = plugin.getCLI();
		cli.deploy(plugin.car.getDestinationFile(), server, user, password, trustAllCertificates, assembleXsl, retry);
	}
}
