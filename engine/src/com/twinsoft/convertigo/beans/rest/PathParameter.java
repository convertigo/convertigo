/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.beans.rest;

import com.twinsoft.convertigo.beans.core.UrlMappingParameter;

public class PathParameter extends UrlMappingParameter {

	private static final long serialVersionUID = 3416827473456844736L;

	public PathParameter() {
		super();
		this.required = Boolean.TRUE;
	}

	@Override
	public PathParameter clone() throws CloneNotSupportedException {
		PathParameter clonedObject = (PathParameter) super.clone();
		return clonedObject;
	}

	@Override
	public Type getType() {
		return Type.Path;
	}

	@Override
	public Boolean isRequired() {
		return true;
	}

	@Override
	public Boolean isExposed() {
		return true;
	}
	
}
