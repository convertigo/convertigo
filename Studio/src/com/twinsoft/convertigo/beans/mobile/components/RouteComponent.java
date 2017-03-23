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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IContainerOrdered;
import com.twinsoft.convertigo.beans.core.MobileComponent;
import com.twinsoft.convertigo.engine.EngineException;

public class RouteComponent extends MobileComponent implements IRouteGenerator, IContainerOrdered {
	
	private static final long serialVersionUID = -8928033403518219727L;

	public RouteComponent() {
		super();
		
		this.priority = getNewOrderValue();
		this.newPriority = priority;
		
		orderedActions = new XMLVector<XMLVector<Long>>();
		orderedActions.add(new XMLVector<Long>());

		orderedEvents = new XMLVector<XMLVector<Long>>();
		orderedEvents.add(new XMLVector<Long>());
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
	
	private boolean isEnabled = true;
	
	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
	
	private XMLVector<XMLVector<Long>> orderedActions = new XMLVector<XMLVector<Long>>();
	private XMLVector<XMLVector<Long>> orderedEvents = new XMLVector<XMLVector<Long>>();
	
	public XMLVector<XMLVector<Long>> getOrderedActions() {
		return orderedActions;
	}
    
	public void setOrderedActions(XMLVector<XMLVector<Long>> orderedActions) {
		this.orderedActions = orderedActions;
	}
	
    private void insertOrderedAction(RouteActionComponent component, Long after) {
    	List<Long> ordered = orderedActions.get(0);
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
    
    private void removeOrderedAction(Long value) {
        Collection<Long> ordered = orderedActions.get(0);
        ordered.remove(value);
        hasChanged = true;
    }
    
	public XMLVector<XMLVector<Long>> getOrderedEvents() {
		return orderedEvents;
	}
    
	public void setOrderedEvents(XMLVector<XMLVector<Long>> orderedEvents) {
		this.orderedEvents = orderedEvents;
	}
	
    private void insertOrderedEvent(RouteEventComponent component, Long after) {
    	List<Long> ordered = orderedEvents.get(0);
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
    
    private void removeOrderedEvent(Long value) {
        Collection<Long> ordered = orderedEvents.get(0);
        ordered.remove(value);
        hasChanged = true;
    }
	
	public void insertAtOrder(DatabaseObject databaseObject, long priority) throws EngineException {
		increaseOrder(databaseObject, new Long(priority));
	}
    
    private void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
    	List<Long> ordered = null;
    	Long value = new Long(databaseObject.priority);
    	
    	if (databaseObject instanceof RouteActionComponent)
    		ordered = orderedActions.get(0);
    	else if (databaseObject instanceof RouteEventComponent)
    		ordered = orderedEvents.get(0);
    	
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
    	
    	if (databaseObject instanceof RouteActionComponent)
    		ordered = orderedActions.get(0);
    	else if (databaseObject instanceof RouteEventComponent)
    		ordered = orderedEvents.get(0);
    	
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
		if (databaseObject instanceof RouteActionComponent || databaseObject instanceof RouteEventComponent)
			increaseOrder(databaseObject,null);
	}

	public void decreasePriority(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof RouteActionComponent || databaseObject instanceof RouteEventComponent)
			decreaseOrder(databaseObject,null);
	}
    
    /**
     * Get representation of order for quick sort of a given database object.
     */
	@Override
    public Object getOrder(Object object) throws EngineException	{
        if (object instanceof RouteActionComponent) {
        	List<Long> ordered = orderedActions.get(0);
        	long time = ((RouteActionComponent)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted action for application \""+ getName() +"\". RouteActionComponent \""+ ((RouteActionComponent)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        }
        else if (object instanceof RouteEventComponent) {
        	List<Long> ordered = orderedEvents.get(0);
        	long time = ((RouteEventComponent)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted event for application \""+ getName() +"\". RouteEventComponent \""+ ((RouteEventComponent)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        }
        else return super.getOrder(object);
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
		
		insertOrderedEvent(routeEventComponent, null);
		
		if (routeEventComponent.bNew) {
			getProject().getMobileBuilder().appChanged();
		}
	}

	public void removeRouteEventComponent(RouteEventComponent routeEventComponent) throws EngineException {
		checkSubLoaded();
		vRouteEventComponents.remove(routeEventComponent);
		
		removeOrderedEvent(routeEventComponent.priority);
		
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
		
		insertOrderedAction(routeActionComponent, null);
		
		if (routeActionComponent.bNew) {
			getProject().getMobileBuilder().appChanged();
		}
		
	}

	public void removeRouteActionComponent(RouteActionComponent routeActionComponent) throws EngineException {
		checkSubLoaded();
		vRouteActionComponents.remove(routeActionComponent);
		
		removeOrderedAction(routeActionComponent.priority);
		
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
	public String computeRoute() {
		StringBuilder sb = new StringBuilder();
		if (isEnabled()) {
			int size = orderedEvents.size();
			if (size > 0) {
				// Add events
				boolean hasEvents = false;
				sb.append("new C8oRouteListener([");
				Iterator<RouteEventComponent> ite = getRouteEventComponentList().iterator();
				while (ite.hasNext()) {
					RouteEventComponent event = ite.next();
					String tpl = event.computeRoute();
					if (!tpl.isEmpty()) {
						sb.append((hasEvents ? ",":"") + "\""+ tpl + "\"");
						hasEvents = true;
					}
				}
				sb.append("])");
				
				// Add route actions
				if (hasEvents) {
					size = orderedActions.size();
					if (size > 0) {
						Iterator<RouteActionComponent> ita = getRouteActionComponentList().iterator();
						while (ita.hasNext()) {
							RouteActionComponent action = ita.next();
							String tpl = action.computeRoute();
							if (!tpl.isEmpty()) {
								if (action instanceof RouteDataActionComponent) {
									sb.append(".addRoute("+ tpl +")");
								}
								else if (action instanceof RouteExceptionActionComponent) {
									sb.append(".addFailedRoute("+ tpl +")");
								}
							}
						}
					}
				}
			}
		}
		return sb.toString();
	}

}
