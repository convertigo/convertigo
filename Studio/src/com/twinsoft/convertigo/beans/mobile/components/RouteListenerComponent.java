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

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobileComponent;
import com.twinsoft.convertigo.engine.EngineException;

public class RouteListenerComponent extends MobileComponent {

	private static final long serialVersionUID = -8928033403518219727L;

	public RouteListenerComponent() {
		super();
	}
	
	@Override
	public RouteListenerComponent clone() throws CloneNotSupportedException {
		RouteListenerComponent cloned = (RouteListenerComponent)super.clone();
		cloned.vRouteEventComponents = new LinkedList<RouteEventComponent>();
		cloned.vRouteComponents = new LinkedList<RouteActionComponent>();
		return cloned;
	}

	@Override
	public RoutingTableComponent getParent() {
		return (RoutingTableComponent) super.getParent();
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
	transient private List<RouteActionComponent> vRouteComponents = new LinkedList<RouteActionComponent>();
	
	protected void addRouteComponent(RouteActionComponent routeComponent) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vRouteComponents, routeComponent.getName(), routeComponent.bNew);
		routeComponent.setName(newDatabaseObjectName);
		vRouteComponents.add(routeComponent);
		super.add(routeComponent);
		
		if (routeComponent.bNew) {
			getProject().getMobileBuilder().appChanged();
		}
		
	}

	public void removeRouteComponent(RouteActionComponent routeComponent) throws EngineException {
		checkSubLoaded();
		vRouteComponents.remove(routeComponent);
		
		getProject().getMobileBuilder().appChanged();
	}

	public List<RouteActionComponent> getRouteComponentList() {
		checkSubLoaded();
		return sort(vRouteComponents);
	}
	
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.addAll(getRouteEventComponentList());
		rep.addAll(getRouteComponentList());
		return rep;
	}

	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof RouteEventComponent) {
			addRouteEventComponent((RouteEventComponent) databaseObject);
		}
		else if (databaseObject instanceof RouteActionComponent) {
			addRouteComponent((RouteActionComponent) databaseObject);
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
			removeRouteComponent((RouteActionComponent) databaseObject);
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
