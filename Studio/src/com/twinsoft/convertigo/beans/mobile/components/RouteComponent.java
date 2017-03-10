/*
 * Copyright (c) 2001-2016 Convertigo SA.
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

package com.twinsoft.convertigo.beans.mobile.components;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobileComponent;
import com.twinsoft.convertigo.engine.EngineException;

public class RouteComponent extends MobileComponent {

	private static final long serialVersionUID = -8928033403518219727L;

	public RouteComponent() {
		super();
		
		this.priority = getNewOrderValue();
		this.newPriority = priority;
	}
	
	@Override
	public RouteComponent clone() throws CloneNotSupportedException {
		RouteComponent cloned = (RouteComponent)super.clone();
		cloned.newPriority = newPriority;
		cloned.vRouteEventComponents = new LinkedList<RouteEventComponent>();
		cloned.vRouteActionComponents = new LinkedList<RouteActionComponent>();
		return cloned;
	}

	@Override
	public Element toXml(Document document) throws EngineException {
		Element element =  super.toXml(document);
		
        element.setAttribute("newPriority", new Long(newPriority).toString());
		
		return element;
	}
	
    @Override
    public Object getOrderedValue() {
    	return new Long(priority);
    }
    
	@Override
	public ApplicationComponent getParent() {
		return (ApplicationComponent) super.getParent();
	}
	
	/**
	 * The list of available events for this listener.
	 */
	transient private List<RouteEventComponent> vRouteEventComponents = new LinkedList<RouteEventComponent>();
	
	protected void addRouteEventComponent(RouteEventComponent routeEventComponent) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vRouteEventComponents, routeEventComponent.getName(), routeEventComponent.bNew);
		routeEventComponent.setName(newDatabaseObjectName);
		vRouteEventComponents.add(routeEventComponent);
		super.add(routeEventComponent);
		
		if (routeEventComponent.bNew) {
			getProject().getMobileBuilder().appChanged();
		}
	}

	public void removeRouteEventComponent(RouteEventComponent routeEventComponent) throws EngineException {
		checkSubLoaded();
		vRouteEventComponents.remove(routeEventComponent);
		
		getProject().getMobileBuilder().appChanged();
	}

	public List<RouteEventComponent> getRouteEventComponentList() {
		checkSubLoaded();
		return sort(vRouteEventComponents);
	}
	
	/**
	 * The list of available routes for this application.
	 */
	transient private List<RouteActionComponent> vRouteActionComponents = new LinkedList<RouteActionComponent>();
	
	protected void addRouteActionComponent(RouteActionComponent routeActionComponent) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vRouteActionComponents, routeActionComponent.getName(), routeActionComponent.bNew);
		routeActionComponent.setName(newDatabaseObjectName);
		vRouteActionComponents.add(routeActionComponent);
		super.add(routeActionComponent);
		
		if (routeActionComponent.bNew) {
			getProject().getMobileBuilder().appChanged();
		}
		
	}

	public void removeRouteActionComponent(RouteActionComponent routeActionComponent) throws EngineException {
		checkSubLoaded();
		vRouteActionComponents.remove(routeActionComponent);
		
		getProject().getMobileBuilder().appChanged();
	}

	public List<RouteActionComponent> getRouteActionComponentList() {
		checkSubLoaded();
		return sort(vRouteActionComponents);
	}
	
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.addAll(getRouteEventComponentList());
		rep.addAll(getRouteActionComponentList());
		return rep;
	}

	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof RouteEventComponent) {
			addRouteEventComponent((RouteEventComponent) databaseObject);
		}
		else if (databaseObject instanceof RouteActionComponent) {
			addRouteActionComponent((RouteActionComponent) databaseObject);
		}
		else {
			throw new EngineException("You cannot add to a route component a database object of type " + databaseObject.getClass().getName());
		}
    }

    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof RouteEventComponent) {
			removeRouteEventComponent((RouteEventComponent) databaseObject);
		}
		else if (databaseObject instanceof RouteActionComponent) {
			removeRouteActionComponent((RouteActionComponent) databaseObject);
		}
		else {
			throw new EngineException("You cannot remove from a route component a database object of type " + databaseObject.getClass().getName());
		}
		super.remove(databaseObject);
    }

	@Override
	protected String computeTemplate() {
		// TODO Auto-generated method stub
		return null;
	}

}
