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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.twinsoft.convertigo.beans.core.UrlMapping;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter.Type;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;

public class PathMapping extends UrlMapping {

	private static final long serialVersionUID = 1966653914675161306L;

	transient private Map<HttpMethodType, UrlMappingOperation> operationMap = new EnumMap<HttpMethodType, UrlMappingOperation>(HttpMethodType.class);
			
	public PathMapping() {
		super();
	}

	private String path = "/";
	
	public String getPath() {
		return path;
	}

	public void setPath(String mappingPath) {
		this.path = mappingPath;
	}

	@Override
	public UrlMapping clone() throws CloneNotSupportedException {
		PathMapping clonedObject = (PathMapping) super.clone();
		clonedObject.operationMap = new EnumMap<HttpMethodType, UrlMappingOperation>(HttpMethodType.class);
		return clonedObject;
	}

	@Override
	protected void addOperation(UrlMappingOperation operation) throws EngineException {
		if (operation instanceof AbstractRestOperation) {
			String method = operation.getMethod();
			if (operationMap.containsKey(HttpMethodType.valueOf(method))) {
				throw new EngineException("The Path mapping already contains an operation of type " + method);
			}
			super.addOperation(operation);
			operationMap.put(HttpMethodType.valueOf(method), operation);
		} else {
			throw new EngineException("You cannot add to a Path mapping an operation of type " + operation.getClass().getName());
		}
	}

	@Override
	public void removeOperation(UrlMappingOperation operation) throws EngineException {
		if (operation instanceof AbstractRestOperation) {
			String method = operation.getMethod();
			if (!operationMap.containsKey(HttpMethodType.valueOf(method))) {
				throw new EngineException("The Path mapping does not contain any operation of type " + method);
			}
			super.removeOperation(operation);
			operationMap.remove(HttpMethodType.valueOf(method));
		} else {
			throw new EngineException("You cannot remove from a Path mapping an operation of type " + operation.getClass().getName());
		}
	}

	@Override
	public UrlMappingOperation getMatchingOperation(HttpServletRequest request) {
		checkSubLoaded();
		
		// Check if mapping path is matching request path
		String requestPath = request.getPathInfo();
		String path_regex = path.replaceAll("\\{([a-zA-Z0-9_]+)\\}", "([^/]+?)");
		Pattern path_pattern = Pattern.compile(path_regex);
		Matcher path_matcher = path_pattern.matcher(requestPath);
		if (path_matcher.matches()) {
			Engine.logBeans.debug("(PathMapping) Found mapping \""+path+"\" matching the request");
			
			// Check if mapping has an operation for request method
			UrlMappingOperation operation = operationMap.get(HttpMethodType.valueOf(request.getMethod()));
			if (operation != null) {
				Engine.logBeans.debug("(PathMapping) Found operation \""+operation.getName()+"\" matching the request method");
				
				// Check for required operation parameters
				for (UrlMappingParameter param :operation.getParameterList()) {
					if (param.isRequired() && (param.getType() == Type.Query || param.getType() == Type.Form)) {
						if (request.getParameter(param.getName()) == null) {
							Engine.logBeans.debug("(PathMapping) Missing required operation's parameter \""+param.getName()+"\"");
							return null;
						}
					}
				}
				
				return operation;
			}
		};
		
		return null;
	}
}
