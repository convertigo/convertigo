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
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.MobileComponent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class ApplicationComponent extends MobileComponent {

	private static final long serialVersionUID = 6142350115354549719L;

	public ApplicationComponent() {
		super();
	}

	@Override
	public ApplicationComponent clone() throws CloneNotSupportedException {
		ApplicationComponent cloned = (ApplicationComponent) super.clone();
		cloned.vPageComponents = new LinkedList<PageComponent>();
		cloned.routingTableComponent = null;
		cloned.computedTemplate = null;
		cloned.rootPage = null;
		return cloned;
	}

	
	@Override
	public MobileApplication getParent() {
		return (MobileApplication) super.getParent();
	}

	/*
	 * The routing table
	 */
	private transient RoutingTableComponent routingTableComponent = null;
	
	public RoutingTableComponent getRoutingTableComponent() {
		return routingTableComponent;
	}
	
    public void addRoutingTableComponent(RoutingTableComponent routingTableComponent) throws EngineException {
    	if (this.routingTableComponent != null) {
    		throw new EngineException("The mobile application \"" + getName() + "\" already contains a routing table! Please delete it first.");
    	}
    	this.routingTableComponent = routingTableComponent;
		super.add(routingTableComponent);
		
		if (routingTableComponent.bNew) {
			getProject().getMobileBuilder().appChanged();
		}
		
    }
    
    public void removeRoutingTableComponent(RoutingTableComponent routingTableComponent) throws EngineException {
    	if (routingTableComponent != null && routingTableComponent.equals(this.routingTableComponent)) {
    		this.routingTableComponent = null;
    		
    		getProject().getMobileBuilder().appChanged();
    	}
    }
	

	/**
	 * The list of available page component for this application.
	 */
	transient private List<PageComponent> vPageComponents = new LinkedList<PageComponent>();
	
	protected void addPageComponent(PageComponent pageComponent) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vPageComponents, pageComponent.getName(), pageComponent.bNew);
		pageComponent.setName(newDatabaseObjectName);
		vPageComponents.add(pageComponent);
		if (pageComponent.isRoot) setRootPage(pageComponent);
		super.add(pageComponent);
		
		if (pageComponent.bNew) {
			getProject().getMobileBuilder().pageAdded(pageComponent);
		}
	}

	public void removePageComponent(PageComponent pageComponent) throws EngineException {
		checkSubLoaded();
		vPageComponents.remove(pageComponent);
		
		getProject().getMobileBuilder().pageRemoved(pageComponent);
	}

	public List<PageComponent> getPageComponentList() {
		checkSubLoaded();
		return sort(vPageComponents);
	}

	public PageComponent getPageComponentByName(String pageName) throws EngineException {
		checkSubLoaded();
		for (PageComponent pageComponent : vPageComponents)
			if (pageComponent.getName().equalsIgnoreCase(pageName)) return pageComponent;
		throw new EngineException("There is no page component named \"" + pageName + "\" found into this application.");
	}
	
	transient private PageComponent rootPage = null;
	
	public PageComponent getRootPage() throws EngineException {
		if (rootPage == null) {
			checkSubLoaded();
			for (PageComponent pageComponent : vPageComponents) {
				if (pageComponent.isRoot) {
					rootPage = pageComponent;
					break;
				}
			}
		}
		if (rootPage == null) {
			if (Engine.isEngineMode()) {
				// Fire exception in Engine mode only!
				throw new EngineException("There is no root page defined for application \"" + getName() + "\".");
			}
			else {
				// In Studio mode we must be able to set a root page!
			}
		}
		return rootPage;
	}
	
	public synchronized void setRootPage(PageComponent pageComponent) throws EngineException {
		if (pageComponent == null)
			throw new IllegalArgumentException("The value of argument 'pageComponent' is null");
		checkSubLoaded();
		if (vPageComponents.contains(pageComponent)) {
			if (rootPage == null) getRootPage();
			if (rootPage != null) rootPage.isRoot = false;
			pageComponent.isRoot = true;
			rootPage = pageComponent;
		} else throw new IllegalArgumentException("The value of argument 'pageComponent' is invalid: the page does not belong to the application");
	}
	
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		if (routingTableComponent != null) rep.add(routingTableComponent);		
		rep.addAll(getPageComponentList());
		return rep;
	}

	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof PageComponent) {
			addPageComponent((PageComponent) databaseObject);
		} else if (databaseObject instanceof RoutingTableComponent) {
			addRoutingTableComponent((RoutingTableComponent)databaseObject);
		} else {
			throw new EngineException("You cannot add to an application component a database object of type " + databaseObject.getClass().getName());
		}
    }

    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof PageComponent) {
			removePageComponent((PageComponent) databaseObject);
		} else if (databaseObject instanceof RoutingTableComponent) {
			removeRoutingTableComponent((RoutingTableComponent)databaseObject);
		} else {
			throw new EngineException("You cannot remove from an application component a database object of type " + databaseObject.getClass().getName());
		}
		super.remove(databaseObject);
    }

	transient private String computedTemplate = null;
	
	public String getComputedTemplate() {
		if (computedTemplate == null) {
			doCompute();
		}
		return computedTemplate;
	}
    
	public synchronized void doCompute() {
		computedTemplate = computeTemplate();
	}
	
	@Override
	protected String computeTemplate() {
		return "";
	}
    
}
