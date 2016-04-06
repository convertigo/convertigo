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

package com.twinsoft.convertigo.beans.core;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.twinsoft.convertigo.engine.EngineException;

public abstract class UrlMappingOperation extends DatabaseObject {

	private static final long serialVersionUID = -160544540810026807L;

	public UrlMappingOperation() {
		super();
		databaseType = "UrlMappingOperation";
	}
	
	@Override
	public UrlMappingOperation clone() throws CloneNotSupportedException {
		UrlMappingOperation clonedObject = (UrlMappingOperation)super.clone();
		clonedObject.parameters = new LinkedList<UrlMappingParameter>();
		clonedObject.responses = new LinkedList<UrlMappingResponse>();
		return clonedObject;
	}

	abstract public String getMethod();
	abstract protected boolean canAddParameter(UrlMappingParameter parameter);
	abstract public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws EngineException;

	private String targetRequestable = "";
	
	public String getTargetRequestable() {
		return targetRequestable;
	}

	public void setTargetRequestable(String targetRequestable) {
		this.targetRequestable = targetRequestable;
	}
	
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.addAll(getParameterList());
		rep.addAll(getResponseList());
		return rep;
	}
	
	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof UrlMappingParameter) {
			addParameter((UrlMappingParameter) databaseObject);
		} else if (databaseObject instanceof UrlMappingResponse) {
			addResponse((UrlMappingResponse) databaseObject);
		} else {
			throw new EngineException("You cannot add to an URL mapping operation a database object of type " + databaseObject.getClass().getName());
		}
    }

    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof UrlMappingParameter) {
			removeParameter((UrlMappingParameter) databaseObject);
		} else if (databaseObject instanceof UrlMappingResponse) {
			removeResponse((UrlMappingResponse) databaseObject);
		} else {
			throw new EngineException("You cannot remove from an URL mapping operation a database object of type " + databaseObject.getClass().getName());
		}
		super.remove(databaseObject);
    }
    
	/**
	 * The list of available parameters for this operation.
	 */
	transient private List<UrlMappingParameter> parameters = new LinkedList<UrlMappingParameter>();
		
	protected void addParameter(UrlMappingParameter parameter) throws EngineException {
		if (!canAddParameter(parameter)) {
			throw new EngineException("You cannot add to this URL mapping operation a database object of type " + parameter.getClass().getName());
		}
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(parameters, parameter.getName(), parameter.bNew);
		parameter.setName(newDatabaseObjectName);
		parameters.add(parameter);
		super.add(parameter);
	}

	protected void removeParameter(UrlMappingParameter parameter) throws EngineException {
		checkSubLoaded();
		parameters.remove(parameter);
	}
	
	public List<UrlMappingParameter> getParameterList() {
		checkSubLoaded();
		return sort(parameters);
	}

	public UrlMappingParameter getParameterByName(String parameterName) throws EngineException {
		checkSubLoaded();
		for (UrlMappingParameter parameter : parameters)
			if (parameter.getName().equalsIgnoreCase(parameterName)) return parameter;
		throw new EngineException("There is no parameter named \"" + parameterName + "\" found into this operation.");
	}
	
	/**
	 * The list of available responses for this operation.
	 */
	transient private List<UrlMappingResponse> responses = new LinkedList<UrlMappingResponse>();
		
	protected void addResponse(UrlMappingResponse response) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(responses, response.getName(), response.bNew);
		response.setName(newDatabaseObjectName);
		responses.add(response);
		super.add(response);
	}

	protected void removeResponse(UrlMappingResponse response) throws EngineException {
		checkSubLoaded();
		responses.remove(response);
	}
	
	public List<UrlMappingResponse> getResponseList() {
		checkSubLoaded();
		return sort(responses);
	}

	public UrlMappingResponse getResponseByCode(Integer statusCode) throws EngineException {
		checkSubLoaded();
		for (UrlMappingResponse response : responses)
			if (response.getStatusCode().equals(statusCode)) return response;
		throw new EngineException("There is no response \"" + statusCode + "\" status code found into this operation.");
	}
	
}
