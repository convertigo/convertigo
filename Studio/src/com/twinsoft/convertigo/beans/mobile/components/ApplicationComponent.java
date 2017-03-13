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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IContainerOrdered;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.MobileComponent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class ApplicationComponent extends MobileComponent implements ITemplateGenerator, IRouteGenerator, IContainerOrdered {

	private static final long serialVersionUID = 6142350115354549719L;

	public ApplicationComponent() {
		super();
		
		orderedRoutes = new XMLVector<XMLVector<Long>>();
		orderedRoutes.add(new XMLVector<Long>());
	}

	@Override
	public ApplicationComponent clone() throws CloneNotSupportedException {
		ApplicationComponent cloned = (ApplicationComponent) super.clone();
		cloned.vRouteComponents = new LinkedList<RouteComponent>();
		cloned.vPageComponents = new LinkedList<PageComponent>();
		cloned.computedTemplate = null;
		cloned.computedRoute = null;
		cloned.rootPage = null;
		return cloned;
	}

	
	@Override
	public MobileApplication getParent() {
		return (MobileApplication) super.getParent();
	}

	private XMLVector<XMLVector<Long>> orderedRoutes = new XMLVector<XMLVector<Long>>();
	
	public XMLVector<XMLVector<Long>> getOrderedRoutes() {
		return orderedRoutes;
	}
    
	public void setOrderedRoutes(XMLVector<XMLVector<Long>> orderedRoutes) {
		this.orderedRoutes = orderedRoutes;
	}
	
    private void insertOrderedRoute(RouteComponent component, Long after) {
    	List<Long> ordered = orderedRoutes.get(0);
    	int size = ordered.size();
    	
    	if (ordered.contains(component.priority))
    		return;
    	
    	if (after == null) {
    		after = new Long(0);
    		if (size>0)
    			after = ordered.get(ordered.size()-1);
    	}
    	
   		int order = ordered.indexOf(after);
    	ordered.add(order+1, component.priority);
    	hasChanged = true;
    }
    
    private void removeOrderedRoute(Long value) {
        Collection<Long> ordered = orderedRoutes.get(0);
        ordered.remove(value);
        hasChanged = true;
    }
    
	public void insertAtOrder(DatabaseObject databaseObject, long priority) throws EngineException {
		increaseOrder(databaseObject, new Long(priority));
	}
    
    private void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
    	List<Long> ordered = null;
    	Long value = new Long(databaseObject.priority);
    	
    	if (databaseObject instanceof RouteComponent)
    		ordered = orderedRoutes.get(0);
    	
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
    
    private void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
    	List<Long> ordered = null;
    	long value = databaseObject.priority;
    	
    	if (databaseObject instanceof RouteComponent)
    		ordered = orderedRoutes.get(0);
    	
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
		if (databaseObject instanceof RouteComponent)
			increaseOrder(databaseObject,null);
	}

	public void decreasePriority(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof RouteComponent)
			decreaseOrder(databaseObject,null);
	}
    
    /**
     * Get representation of order for quick sort of a given database object.
     */
	@Override
    public Object getOrder(Object object) throws EngineException	{
        if (object instanceof RouteComponent) {
        	List<Long> ordered = orderedRoutes.get(0);
        	long time = ((RouteComponent)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted route for application \""+ getName() +"\". RouteComponent \""+ ((RouteComponent)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        }
        else return super.getOrder(object);
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
		
		insertOrderedRoute(routeComponent,null);
		
		if (routeComponent.bNew) {
			getProject().getMobileBuilder().appChanged();
		}
		
	}

	public void removeRouteComponent(RouteComponent routeComponent) throws EngineException {
		checkSubLoaded();
		vRouteComponents.remove(routeComponent);
		
		removeOrderedRoute(routeComponent.priority);
		
		getProject().getMobileBuilder().appChanged();
	}

	public List<RouteComponent> getRouteComponentList() {
		checkSubLoaded();
		return sort(vRouteComponents);
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
		rep.addAll(getRouteComponentList());
		rep.addAll(getPageComponentList());
		return rep;
	}

	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof RouteComponent) {
			addRouteComponent((RouteComponent) databaseObject);
		} else if (databaseObject instanceof PageComponent) {
			addPageComponent((PageComponent) databaseObject);
		} else {
			throw new EngineException("You cannot add to an application component a database object of type " + databaseObject.getClass().getName());
		}
    }

    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof RouteComponent) {
			removeRouteComponent((RouteComponent) databaseObject);
		} else if (databaseObject instanceof PageComponent) {
			removePageComponent((PageComponent) databaseObject);
		} else {
			throw new EngineException("You cannot remove from an application component a database object of type " + databaseObject.getClass().getName());
		}
		super.remove(databaseObject);
    }

    /*
     * The computed template (see app.html)
     */
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
	public String computeTemplate() {
		//TODO
		return "";
	}

	/*
	 * The computed routing table (see app.component.ts)
	 */
	transient private String computedRoute = null;
	
	public String getComputedRoute() {
		if (computedRoute == null) {
			doComputeRoute();
		}
		return computedRoute;
	}
	
	public synchronized void doComputeRoute() {
		computedRoute = computeRoute();
	}
    
	@Override
	public String computeRoute() {
		StringBuilder sb = new StringBuilder();
		int size = orderedRoutes.size();
		if (size > 0) {
			Iterator<RouteComponent> itr = getRouteComponentList().iterator();
			while (itr.hasNext()) {
				RouteComponent route = itr.next();
				String tpl = route.computeRoute();
				if (!tpl.isEmpty()) {
					sb.append("this.router.addRouteListener(").append(tpl).append(")")
						.append(";").append(System.getProperty("line.separator"));
				}
			}
		}
		return sb.toString();
	}
    
}
