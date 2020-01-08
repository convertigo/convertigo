/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

@DboCategoryInfo(
		getCategoryId = "UrlMappingOperation",
		getCategoryName = "Operation",
		getIconClassCSS = "convertigo-action-newUrlMappingOperation"
	)
public abstract class UrlMappingOperation extends DatabaseObject implements IContainerOrdered {

	private static final long serialVersionUID = -160544540810026807L;

	transient private XMLVector<XMLVector<Long>> orderedParameters = new XMLVector<XMLVector<Long>>();
	
	public UrlMappingOperation() {
		super();
		databaseType = "UrlMappingOperation";
		
		orderedParameters = new XMLVector<XMLVector<Long>>();
		orderedParameters.add(new XMLVector<Long>());
	}
	
	@Override
	public UrlMappingOperation clone() throws CloneNotSupportedException {
		UrlMappingOperation clonedObject = (UrlMappingOperation)super.clone();
		clonedObject.isChangeTo = false;
		clonedObject.parameters = new LinkedList<UrlMappingParameter>();
		clonedObject.responses = new LinkedList<UrlMappingResponse>();
		return clonedObject;
	}

	abstract public String getMethod();
	abstract protected boolean canAddParameter(UrlMappingParameter parameter);
	abstract public String handleRequest(HttpServletRequest request, HttpServletResponse response) throws EngineException;

	protected transient boolean isChangeTo = false;
	
	private String targetRequestable = "";
	
	public String getTargetRequestable() {
		return targetRequestable;
	}

	public void setTargetRequestable(String targetRequestable) {
		this.targetRequestable = targetRequestable;
	}
	
	public boolean isTargetAuthenticationContextRequired() {
		RequestableObject ro = getTargetRequestableObject();
		if (ro != null) {
			return ro.getAuthenticatedContextRequired();
		}
		return false;
	}
	
	protected RequestableObject getTargetRequestableObject() {
		String targetRequestableQName = getTargetRequestable();
		if (!targetRequestableQName.isEmpty()) {
			try {
				DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(targetRequestableQName);
				if (dbo != null && dbo instanceof RequestableObject) {
					return (RequestableObject)dbo;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.addAll(getParameterList());
		rep.addAll(getResponseList());
		return rep;
	}
	
	public void changeTo(DatabaseObject databaseObject) throws EngineException {
		try {
			isChangeTo = true;
			add(databaseObject);
		} finally {
			isChangeTo = false;
		}
	}

	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		add(databaseObject, null);
    }
	
	@Override
    public void add(DatabaseObject databaseObject, Long after) throws EngineException {
		if (databaseObject instanceof UrlMappingParameter) {
			addParameter((UrlMappingParameter) databaseObject, after);
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
		addParameter(parameter, null);
	}
	
	protected void addParameter(UrlMappingParameter parameter, Long after) throws EngineException {
		if (!canAddParameter(parameter)) {
			throw new EngineException("You cannot add to this URL mapping operation a database object of type " + parameter.getClass().getName());
		}
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(parameters, parameter.getName(), parameter.bNew);
		parameter.setName(newDatabaseObjectName);
		parameters.add(parameter);
		parameter.setParent(this);
		insertOrderedParameter(parameter, after);
	}

	protected void removeParameter(UrlMappingParameter parameter) throws EngineException {
		checkSubLoaded();
		parameters.remove(parameter);
		parameter.setParent(null);
        removeOrderedParameter(parameter.priority);
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
			if (response.getStatusCode().equals(String.valueOf(statusCode))) return response;
		throw new EngineException("There is no response \"" + statusCode + "\" status code found into this operation.");
	}
	
	public XMLVector<XMLVector<Long>> getOrderedParameters() {
		return orderedParameters;
	}
    
	public void setOrderedParameters(XMLVector<XMLVector<Long>> orderedParameters) {
		this.orderedParameters = orderedParameters;
	}
	
    private void insertOrderedParameter(UrlMappingParameter parameter, Long after) {
    	List<Long> ordered = orderedParameters.get(0);
    	int size = ordered.size();
    	
    	if (ordered.contains(parameter.priority))
    		return;
    	
    	if (after == null) {
    		after = 0L;
    		if (size > 0)
    			after = ordered.get(ordered.size()-1);
    	}
    	
   		int order = ordered.indexOf(after);
    	ordered.add(order+1, parameter.priority);
    	hasChanged = !isImporting;
    }
    
    private void removeOrderedParameter(Long value) {
        Collection<Long> ordered = orderedParameters.get(0);
        ordered.remove(value);
        hasChanged = true;
    }
    
	public void insertAtOrder(DatabaseObject databaseObject, long priority) throws EngineException {
		increaseOrder(databaseObject, priority);
	}
    
    protected void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
    	List<Long> ordered = null;
    	Long value = databaseObject.priority;
    	
    	if (databaseObject instanceof UrlMappingParameter)
    		ordered = orderedParameters.get(0);
    	
    	if (ordered == null || !ordered.contains(value))
    		return;
    	int pos = ordered.indexOf(value);
    	if (pos == 0)
    		return;
    	
    	if (before == null)
    		before = ordered.get(pos-1);
    	int pos1 = ordered.indexOf(before);
    	
    	ordered.add(pos1, value);
    	ordered.remove(pos+1);
    	hasChanged = true;
    }
    
    protected void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
    	List<Long> ordered = null;
    	long value = databaseObject.priority;
    	
    	if (databaseObject instanceof UrlMappingParameter)
    		ordered = orderedParameters.get(0);
    	
    	if (ordered == null || !ordered.contains(value))
    		return;
    	int pos = ordered.indexOf(value);
    	if (pos+1 == ordered.size())
    		return;
    	
    	if (after == null)
    		after = ordered.get(pos+1);
    	int pos1 = ordered.indexOf(after);
    	
    	ordered.add(pos1+1, value);
    	ordered.remove(pos);
    	hasChanged = true;
    }
    
	public void increasePriority(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof UrlMappingParameter)
			increaseOrder(databaseObject,null);
	}

	public void decreasePriority(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof UrlMappingParameter)
			decreaseOrder(databaseObject,null);
	}
    
    /**
     * Get representation of order for quick sort of a given database object.
     */
	@Override
    public Object getOrder(Object object) throws EngineException	{
        if (object instanceof UrlMappingParameter) {
        	List<Long> ordered = orderedParameters.get(0);
        	long time = ((UrlMappingParameter)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted parameter for operation \""+ getName() +"\". UrlMappingParameter \""+ ((UrlMappingParameter)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        }
        else return super.getOrder(object);
    }
}
