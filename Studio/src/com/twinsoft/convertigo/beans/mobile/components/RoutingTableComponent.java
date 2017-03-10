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

public class RoutingTableComponent extends MobileComponent {

	private static final long serialVersionUID = -4291779628634650260L;

	public RoutingTableComponent() {
		super();
	}
	
	@Override
	public RoutingTableComponent clone() throws CloneNotSupportedException {
		RoutingTableComponent cloned = (RoutingTableComponent) super.clone();
		cloned.vRouteListenerComponents = new LinkedList<RouteListenerComponent>();
		return cloned;
	}

	@Override
	public ApplicationComponent getParent() {
		return (ApplicationComponent) super.getParent();
	}

	/**
	 * The list of available route listener for this application.
	 */
	transient private List<RouteListenerComponent> vRouteListenerComponents = new LinkedList<RouteListenerComponent>();
	
	protected void addRouteListenerComponent(RouteListenerComponent routeListenerComponent) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vRouteListenerComponents, routeListenerComponent.getName(), routeListenerComponent.bNew);
		routeListenerComponent.setName(newDatabaseObjectName);
		vRouteListenerComponents.add(routeListenerComponent);
		super.add(routeListenerComponent);
		
		if (routeListenerComponent.bNew) {
			getProject().getMobileBuilder().appChanged();
		}
		
	}

	public void removeRouteListenerComponent(RouteListenerComponent routeListenerComponent) throws EngineException {
		checkSubLoaded();
		vRouteListenerComponents.remove(routeListenerComponent);
		
		getProject().getMobileBuilder().appChanged();
	}

	public List<RouteListenerComponent> getRouteListenerComponentList() {
		checkSubLoaded();
		return sort(vRouteListenerComponents);
	}
	
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.addAll(getRouteListenerComponentList());
		return rep;
	}

	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof RouteListenerComponent) {
			addRouteListenerComponent((RouteListenerComponent) databaseObject);
		} else {
			throw new EngineException("You cannot add to a routing table component a database object of type " + databaseObject.getClass().getName());
		}
    }

    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof RouteListenerComponent) {
			removeRouteListenerComponent((RouteListenerComponent) databaseObject);
		} else {
			throw new EngineException("You cannot remove from a routing table component a database object of type " + databaseObject.getClass().getName());
		}
		super.remove(databaseObject);
    }
	
	@Override
	protected String computeTemplate() {
		// TODO Auto-generated method stub
		return null;
	}

}
