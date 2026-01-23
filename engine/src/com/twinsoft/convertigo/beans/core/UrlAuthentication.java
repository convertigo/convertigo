/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.beans.core;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.twinsoft.convertigo.beans.core.DatabaseObject.DboFolderType;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.DatabaseObjectTypes;
import com.twinsoft.convertigo.engine.enums.FolderType;

@DboFolderType(type = FolderType.AUTHENTICATION)
public abstract class UrlAuthentication extends DatabaseObject {


	private static final long serialVersionUID = -7507684542477852819L;

	public enum AuthenticationType {
		Basic
	}
	
	
	abstract public AuthenticationType getType();
	abstract public String handleAuthRequest(HttpServletRequest request, HttpServletResponse response) throws EngineException;
		
	public UrlAuthentication() {
		super();
		databaseType = DatabaseObjectTypes.UrlAuthentication.name();
	}

	@Override
	public UrlAuthentication clone() throws CloneNotSupportedException {
		UrlAuthentication clonedObject = (UrlAuthentication)super.clone();
		return clonedObject;
	}

	private String authRequestable = "";
	
	public String getAuthRequestable() {
		return authRequestable;
	}

	public void setAuthRequestable(String authRequestable) {
		this.authRequestable = authRequestable;
	}
	
	@Override
	public String toString() {
		return getType().name();
	}	
}
