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
		cloned.vRouteComponents = new LinkedList<RouteComponent>();
		return cloned;
	}

	@Override
	public RoutingTableComponent getParent() {
		return (RoutingTableComponent) super.getParent();
	}
	
	/**
	 * The list of available routes for this application.
	 */
	transient private List<RouteComponent> vRouteComponents = new LinkedList<RouteComponent>();
	
	protected void addRouteComponent(RouteComponent routeComponent) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vRouteComponents, routeComponent.getName(), routeComponent.bNew);
		routeComponent.setName(newDatabaseObjectName);
		vRouteComponents.add(routeComponent);
		super.add(routeComponent);
	}

	public void removeRouteComponent(RouteComponent routeComponent) throws EngineException {
		checkSubLoaded();
		vRouteComponents.remove(routeComponent);
	}

	public List<RouteComponent> getRouteComponentList() {
		checkSubLoaded();
		return sort(vRouteComponents);
	}
	
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.addAll(getRouteComponentList());
		return rep;
	}

	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof RouteComponent) {
			addRouteComponent((RouteComponent) databaseObject);
		} else {
			throw new EngineException("You cannot add to a route component a database object of type " + databaseObject.getClass().getName());
		}
    }

    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof RouteComponent) {
			removeRouteComponent((RouteComponent) databaseObject);
		} else {
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
