/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.engine.enums;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;

public enum HttpMethodType {
	GET(HttpGet.class),
	POST(HttpPost.class),
	PUT(HttpPut.class),
	DELETE(HttpDelete.class),
	HEAD(HttpHead.class),
	TRACE(HttpTrace.class),
	OPTIONS(HttpOptions.class);
	
	Class<? extends HttpRequestBase> methodClass;
	
	HttpMethodType(Class<? extends HttpRequestBase> methodClass) {
		this.methodClass = methodClass;
	}
	
	public HttpRequestBase newInstance() {
		try {
			return methodClass.getConstructor().newInstance();
		} catch (Exception e) {
			return null;
		}
	}
}