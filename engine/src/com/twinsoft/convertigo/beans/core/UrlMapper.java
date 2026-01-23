/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.beans.core;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.beans.core.UrlAuthentication.AuthenticationType;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.DatabaseObjectTypes;

@DboCategoryInfo(
		getCategoryId = "UrlMapper",
		getCategoryName = "URL mapper",
		getIconClassCSS = "convertigo-action-newUrlMapper"
	)
public class UrlMapper extends DatabaseObject {

	private static final long serialVersionUID = 7109554030130695052L;

	public UrlMapper() {
		super();
		databaseType = DatabaseObjectTypes.UrlMapper.name();
	}
	
	@Override
	public UrlMapper clone() throws CloneNotSupportedException {
		UrlMapper clonedObject = (UrlMapper)super.clone();
		clonedObject.authentications = new LinkedList<UrlAuthentication>();
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

	private String prefix = "";
	
	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.addAll(getAuthenticationList());
		rep.addAll(getMappingList());
		return rep;
	}

	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof UrlAuthentication) {
			addAuthentication((UrlAuthentication)databaseObject);
		} else if (databaseObject instanceof UrlMapping) {
			addMapping((UrlMapping) databaseObject);
		} else {
			throw new EngineException("You cannot add to an URL mapper a database object of type " + databaseObject.getClass().getName());
		}
    }

    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof UrlAuthentication) {
			removeAuthentication((UrlAuthentication)databaseObject);
		} else if (databaseObject instanceof UrlMapping) {
			removeMapping((UrlMapping) databaseObject);
		} else {
			throw new EngineException("You cannot remove from an URL mapper a database object of type " + databaseObject.getClass().getName());
		}
		super.remove(databaseObject);
    }
	
	/**
	 * The list of available authentications for this mapper.
	 */
	transient private List<UrlAuthentication> authentications = new LinkedList<UrlAuthentication>();
		
	protected void addAuthentication(UrlAuthentication authentication) throws EngineException {
		boolean hasAuthenticationType = getAuthenticationByType(authentication.getType()) != null;
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(authentications, authentication.getName(), authentication.bNew);
		authentication.setName(newDatabaseObjectName);
		authentications.add(authentication);
		super.add(authentication);
		if (hasAuthenticationType) {
			Engine.logBeans.warn("The mapper already contains an authentication with given type \""+authentication.getType()+"\". Only the first one will be taken into account.");
		}
	}

	public void removeAuthentication(UrlAuthentication authentication) throws EngineException {
		checkSubLoaded();
		authentications.remove(authentication);
	}
	
	public List<UrlAuthentication> getAuthenticationList() {
		checkSubLoaded();
		return sort(authentications);
	}
    
	private Object getAuthenticationByType(AuthenticationType authType) {
		for (UrlAuthentication authentication : getAuthenticationList()) {
			if (authentication.getType().equals(authType)) {
				return authentication;
			}
		}
		return null;
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
	
	public void addMatchingMethods(HttpServletRequest request, Set<String> methods) {		
		for (UrlMapping mapping : getMappingList()) {
			if (mapping.isMatching(request)) {
				for (UrlMappingOperation op : mapping.getOperationList()) {
					String method = op.getMethod();
					if (method != null) {
						methods.add(method.toUpperCase());
					}
				}
			}
		}
	}
}
