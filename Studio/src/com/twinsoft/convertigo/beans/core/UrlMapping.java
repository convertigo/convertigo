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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.twinsoft.convertigo.engine.EngineException;

public abstract class UrlMapping extends DatabaseObject {

	private static final long serialVersionUID = -1685983179181274444L;

	public UrlMapping() {
		super();
		databaseType = "UrlMapping";
	}
	
	@Override
	public UrlMapping clone() throws CloneNotSupportedException {
		UrlMapping clonedObject = (UrlMapping)super.clone();
		clonedObject.operations = new LinkedList<UrlMappingOperation>();
		return clonedObject;
	}

	public abstract String getPath();
	public abstract void setPath(String path);
	public abstract UrlMappingOperation getMatchingOperation(HttpServletRequest request);
	
	@Override
	public String toString() {
		return getPath();
	}

	@Override
	public Object getOrderedValue() {
		return getPath();
	}

	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.addAll(getOperationList());
		return rep;
	}
	
	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof UrlMappingOperation) {
			addOperation((UrlMappingOperation) databaseObject);
		} else {
			throw new EngineException("You cannot add to an URL mapping a database object of type " + databaseObject.getClass().getName());
		}
    }

    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof UrlMappingOperation) {
			removeOperation((UrlMappingOperation) databaseObject);
		} else {
			throw new EngineException("You cannot remove from an URL mapping a database object of type " + databaseObject.getClass().getName());
		}
		super.remove(databaseObject);
    }
    
	/**
	 * The list of available operations for this mapping.
	 */
	transient private List<UrlMappingOperation> operations = new LinkedList<UrlMappingOperation>();
		
	protected void addOperation(UrlMappingOperation operation) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(operations, operation.getName(), operation.bNew);
		operation.setName(newDatabaseObjectName);
		operations.add(operation);
		super.add(operation);
	}

	protected void removeOperation(UrlMappingOperation operation) throws EngineException {
		checkSubLoaded();
		operations.remove(operation);
	}
	
	public List<UrlMappingOperation> getOperationList() {
		checkSubLoaded();
		return sort(operations);
	}

	public UrlMappingOperation getOperationByName(String operationName) throws EngineException {
		checkSubLoaded();
		for (UrlMappingOperation operation : operations)
			if (operation.getName().equalsIgnoreCase(operationName)) return operation;
		throw new EngineException("There is no operation named \"" + operationName + "\" found into this mapping.");
	}

	public List<String> getPathVariableNames() {
		List<String> varList = new ArrayList<String>();
		Pattern pattern = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");
		Matcher matcher = pattern.matcher(getPath());
		while (matcher.find()) {
			String path_varname = matcher.group(1);
			if (!path_varname.isEmpty() && !varList.contains(path_varname)) {
				varList.add(path_varname);
			}
		}
		return varList;
	}
	
	public Map<String, String> getPathVariableValues(HttpServletRequest request) {
		String requestPath = request.getPathInfo();
		String url_regex = getPath().replaceAll("\\{([a-zA-Z0-9_]+)\\}", "([^/]+?)");
		Pattern url_pattern = Pattern.compile(url_regex);
		Matcher url_matcher = url_pattern.matcher(requestPath);
		Map<String, String> varMap = new LinkedHashMap<String, String>();
		if (url_matcher.matches()) {
			Pattern var_pattern = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");
			Matcher var_matcher = var_pattern.matcher(getPath());
			int i = 1;
			while (var_matcher.find()) {
				try {
					String varName = var_matcher.group(1);
					String varValue = url_matcher.group(i++);
					if (varName != null && !varName.isEmpty()) {
						varMap.put(varName, varValue);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return varMap;
	}
	
	public boolean isMatching(HttpServletRequest request) {
		String requestPath = request.getPathInfo();
		String url_regex = getPath().replaceAll("\\{([a-zA-Z0-9_]+)\\}", "([^/]+?)");
		Pattern url_pattern = Pattern.compile(url_regex);
		Matcher url_matcher = url_pattern.matcher(requestPath);
		return url_matcher.matches();
	}
}
