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

package com.twinsoft.convertigo.beans.variables;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.Variable;

public class HttpStatementVariable extends Variable implements ITagsProperty {

	private static final long serialVersionUID = 3554331811470682344L;

	private String httpMethod = "POST";
	private String httpName = "";
	
	public HttpStatementVariable() {
		super();
	}

	@Override
	public HttpStatementVariable clone() throws CloneNotSupportedException {
		HttpStatementVariable clonedObject = (HttpStatementVariable)super.clone();
		return clonedObject;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}

	public String getHttpName() {
		return httpName;
	}

	public void setHttpName(String httpName) {
		this.httpName = httpName;
	}

	public String[] getTagsForProperty(String propertyName) {
		if(propertyName.equals("httpMethod")){
			return new String[]{
					"",
					"GET",
					"POST"
			};
		}
		return new String[0];
	}
	
}
