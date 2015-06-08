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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.beans.references;

import java.net.MalformedURLException;
import java.net.URL;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public abstract class RemoteFileReference extends FileReference implements IUrlReference {
	
	private static final long serialVersionUID = 717504231825643840L;

	public String urlpath = "";
	
	private boolean needAuthentication = false;
	private String authUser = "";
	private String authPassword = "";
	
	public boolean needAuthentication() {
		return needAuthentication;
	}

	public void setNeedAuthentication(boolean needAuthentication) {
		this.needAuthentication = needAuthentication;
	}
	
	public String getUrlpath() {
		return urlpath;
	}

	public void setUrlpath(String urlpath) {
		this.urlpath = urlpath;
	}

	public URL getUrl() throws MalformedURLException {
		String wsdlUrl = getUrlpath();
		// change URL for server mode
		if (wsdlUrl.startsWith("http://localhost:18080/convertigo")) {
			wsdlUrl = wsdlUrl.replaceAll("http://localhost:18080/convertigo", EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL));
		}
		return new URL(wsdlUrl);
	}
	
	protected URL getReferenceUrl() throws MalformedURLException {
		URL url = super.getReferenceUrl();
		return url != null ? url : getUrl();
	}

	public String getAuthUser() {
		return authUser;
	}

	public void setAuthUser(String authUser) {
		this.authUser = authUser;
	}

	public String getAuthPassword() {
		return authPassword;
	}

	public void setAuthPassword(String authPassword) {
		this.authPassword = authPassword;
	}
}
