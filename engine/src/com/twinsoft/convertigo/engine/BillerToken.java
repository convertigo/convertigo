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

package com.twinsoft.convertigo.engine;

import java.util.Map;

public class BillerToken {
	private String tokenID;
	private String project;
	private String user;
	private Map<String, String> data;

	public BillerToken() {
		
	}

	public BillerToken(String tokenID, String project, String user, Map<String, String> data) {
		this.tokenID = tokenID;
		this.project = project;
		this.user = user;
		this.data = data;
	}
	
	public String getTokenID() {
		return tokenID;
	}

	public void setTokenID(String tokenID) {
		this.tokenID = tokenID;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public Map<String, String> getData() {
		return data;
	}

	public void setData(Map<String, String> data) {
		this.data = data;
	}

	public void putData(String key, String value) {
		data.put(key, value);
	}
	
	@Override
	public String toString() {
		return "tokenID: " + tokenID + ", project: " + project + ", user: " + user + ", data: " + data.toString();
	}	
}
