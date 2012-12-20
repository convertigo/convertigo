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

package com.twinsoft.convertigo.eclipse;

import java.io.*;

public class DeploymentConfiguration implements Serializable {

	private static final long serialVersionUID = -9001695822957177261L;
	
	protected String server = "?";
    protected String username = "?";
    protected String userpassword = "?";
    protected boolean bHttps = false;
    protected boolean bTrustAllCertificates;
	protected boolean bAssembleXsl = false;
    
    public DeploymentConfiguration(String server, String username, String userpassword, boolean bHttps, boolean bTrustAllCertificates ,boolean bAssembleXsl) {
        this.server = server;
        this.username = username;
        this.userpassword = userpassword;
        this.bHttps = bHttps;
        this.bTrustAllCertificates = bTrustAllCertificates;
        this.bAssembleXsl = bAssembleXsl;
    }

    @Override
    public String toString() {
        return server;
    }

	/**
	 * @return the bHttps
	 */
	public boolean isBHttps() {
		return bHttps;
	}

	/**
	 * @param https the bHttps to set
	 */
	public void setBHttps(boolean https) {
		this.bHttps = https;
	}
	
	/**
	 * @return the bTrustAllCertificates
	 */
    public boolean isBTrustAllCertificates() {
		return bTrustAllCertificates;
	}

    /**
	 * @param trustAllCertificates the bTrustAllCertificates to set
	 */
	public void setBTrustAllCertificates(boolean bTrustAllCertificates) {
		this.bTrustAllCertificates = bTrustAllCertificates;
	}

	/**
	 * @return the bAssembleXsl
	 */
	public boolean isBAssembleXsl() {
		return bAssembleXsl;
	}

	/**
	 * @param bAssembleXsl the bAssembleXsl to set
	 */
	public void setBAssembleXsl(boolean bAssembleXsl) {
		this.bAssembleXsl = bAssembleXsl;
	}
	
	/**
	 * @return the server
	 */
	public String getServer() {
		return server;
	}

	/**
	 * @param server the server to set
	 */
	public void setServer(String server) {
		this.server = server;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the userpassword
	 */
	public String getUserpassword() {
		return userpassword;
	}

	/**
	 * @param userpassword the userpassword to set
	 */
	public void setUserpassword(String userpassword) {
		this.userpassword = userpassword;
	}
}
