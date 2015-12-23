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

package com.twinsoft.convertigo.beans.rest;

import com.twinsoft.convertigo.beans.core.IMappingRefModel;
import com.twinsoft.convertigo.beans.core.UrlMappingResponse;

public class AbstractRestResponse extends UrlMappingResponse implements IMappingRefModel {

	private static final long serialVersionUID = -4708224923159051529L;

	
	@Override
	public UrlMappingResponse clone() throws CloneNotSupportedException {
		UrlMappingResponse clonedObject = (UrlMappingResponse)super.clone();
		return clonedObject;
	}

	private String statusCode = "200";
	
	@Override
	public String getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(String statusCode) {
		try {
			if (statusCode.length() != 3) return;
			if (Integer.valueOf(statusCode, 10) > 0) {
				this.statusCode = statusCode;
			}
		} catch (Exception e) {}
	}

	private String statusText = "successful operation";
	
	@Override
	public String getStatusText() {
		return statusText;
	}

	public void setStatusText(String statusText) {
		this.statusText = statusText;
	}

	private String modelReference = "";
	
	@Override
	public String getModelReference() {
		return modelReference;
	}

	public void setModelReference(String modelReference) {
		this.modelReference = modelReference;
	}
}
