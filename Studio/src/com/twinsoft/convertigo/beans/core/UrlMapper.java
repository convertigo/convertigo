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

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class UrlMapper extends DatabaseObject {

	private static final long serialVersionUID = 7109554030130695052L;

	public UrlMapper() {
		super();
		databaseType = "UrlMapper";
	}
	
	@Override
	public UrlMapper clone() throws CloneNotSupportedException {
		UrlMapper clonedObject = (UrlMapper)super.clone();
		clonedObject.mappings = new LinkedList<UrlMapping>();
		return clonedObject;
	}

	private String models = "";
	
	public String getModels() {
		return models;
	}

	public void setModels(String models) {
		this.models = models;
	}

	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.addAll(getMappingList());
		return rep;
	}

	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof UrlMapping) {
			addMapping((UrlMapping) databaseObject);
		} else {
			throw new EngineException("You cannot add to an URL mapper a database object of type " + databaseObject.getClass().getName());
		}
    }

    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof UrlMapping) {
			removeMapping((UrlMapping) databaseObject);
		} else {
			throw new EngineException("You cannot remove from an URL mapper a database object of type " + databaseObject.getClass().getName());
		}
		super.remove(databaseObject);
    }
	
	/**
	 * The list of available mappings for this mapper.
	 */
	transient private List<UrlMapping> mappings = new LinkedList<UrlMapping>();
		
	protected void addMapping(UrlMapping mapping) throws EngineException {
		boolean hasMappingPath = getMappingByPath(mapping.getPath()) != null;
		//if (hasMappingPath) {
		//	throw new EngineException("The mapper already contains a mapping with given path \""+mapping.getPath()+"\"");
		//}
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(mappings, mapping.getName(), mapping.bNew);
		mapping.setName(newDatabaseObjectName);
		mappings.add(mapping);
		super.add(mapping);
		if (hasMappingPath) {
			Engine.logBeans.warn("The mapper already contains a mapping with given path \""+mapping.getPath()+"\". Only the first one will be taken into account.");
		}
	}

	public void removeMapping(UrlMapping mapping) throws EngineException {
		checkSubLoaded();
		mappings.remove(mapping);
	}
	
	public List<UrlMapping> getMappingList() {
		checkSubLoaded();
		return sort(mappings);
	}

	public UrlMapping getMappingByName(String mappingName) throws EngineException {
		for (UrlMapping mapping : getMappingList()) {
			if (mapping.getName().equalsIgnoreCase(mappingName)) {
				return mapping;
			}
		}
		throw new EngineException("There is no mapping named \"" + mappingName + "\" found into this project.");
	}
	
	public UrlMapping getMappingByPath(String mappingPath) {
		for (UrlMapping mapping : getMappingList()) {
			if (mapping.getPath().equalsIgnoreCase(mappingPath)) {
				return mapping;
			}
		}
		return null;
	}
	
	public UrlMappingOperation getMatchingOperation(HttpServletRequest request) {
		for (UrlMapping mapping : getMappingList()) {
			UrlMappingOperation urlMappingOperation = mapping.getMatchingOperation(request);
			if (urlMappingOperation != null) {
				return urlMappingOperation;
			}
		}
		return null;
	}
}
