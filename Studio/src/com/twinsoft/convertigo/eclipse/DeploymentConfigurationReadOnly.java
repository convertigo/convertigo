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


public class DeploymentConfigurationReadOnly extends DeploymentConfiguration {

	private static final long serialVersionUID = -9001695822957177262L;
    
    public DeploymentConfigurationReadOnly(String server, String username, String userpassword, boolean bHttps, boolean bTrustAllCertificates ,boolean bAssembleXsl) {
        super(server, username, userpassword, bHttps, bTrustAllCertificates ,bAssembleXsl);
    }
    
	/**
	 * @param https the bHttps to set
	 */
	public void setBHttps(boolean https) {
		throw new IllegalArgumentException("Read Only");
	}

    /**
	 * @param trustAllCertificates the bTrustAllCertificates to set
	 */
	public void setBTrustAllCertificates(boolean bTrustAllCertificates) {
		throw new IllegalArgumentException("Read Only");
	}

	/**
	 * @param bAssembleXsl the bAssembleXsl to set
	 */
	public void setBAssembleXsl(boolean bAssembleXsl) {
		throw new IllegalArgumentException("Read Only");
	}

	/**
	 * @param server the server to set
	 */
	public void setServer(String server) {
		throw new IllegalArgumentException("Read Only");
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		throw new IllegalArgumentException("Read Only");
	}

	/**
	 * @param userpassword the userpassword to set
	 */
	public void setUserpassword(String userpassword) {
		throw new IllegalArgumentException("Read Only");
	}
}
