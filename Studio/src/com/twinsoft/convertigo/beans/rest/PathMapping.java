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

import java.util.EnumMap;
import java.util.Map;

import com.twinsoft.convertigo.beans.core.UrlMapping;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;

public class PathMapping extends UrlMapping {

	private static final long serialVersionUID = 1966653914675161306L;

	transient private Map<HttpMethodType, UrlMappingOperation> operationMap = new EnumMap<HttpMethodType, UrlMappingOperation>(HttpMethodType.class);
	
	public PathMapping() {
		super();
	}

	@Override
	public UrlMapping clone() throws CloneNotSupportedException {
		PathMapping clonedObject = (PathMapping) super.clone();
		clonedObject.operationMap = new EnumMap<HttpMethodType, UrlMappingOperation>(HttpMethodType.class);
		return clonedObject;
	}

	@Override
	protected void addOperation(UrlMappingOperation operation) throws EngineException {
		if (operation instanceof IRestOperation) {
			HttpMethodType httpMethodType = ((IRestOperation)operation).getHttpMethodType(); 
			if (operationMap.containsKey(httpMethodType)) {
				throw new EngineException("The Path mapping already contains an operation of type " + httpMethodType.name());
			}
			super.addOperation(operation);
			operationMap.put(httpMethodType, operation);
		} else {
			throw new EngineException("You cannot add to a Path mapping an operation of type " + operation.getClass().getName());
		}
	}

	@Override
	public void removeOperation(UrlMappingOperation operation) throws EngineException {
		if (operation instanceof IRestOperation) {
			HttpMethodType httpMethodType = ((IRestOperation)operation).getHttpMethodType(); 
			if (!operationMap.containsKey(httpMethodType)) {
				throw new EngineException("The Path mapping does not contain any operation of type " + httpMethodType.name());
			}
			super.removeOperation(operation);
			operationMap.remove(httpMethodType);
		} else {
			throw new EngineException("You cannot remove from a Path mapping an operation of type " + operation.getClass().getName());
		}
	}

}
