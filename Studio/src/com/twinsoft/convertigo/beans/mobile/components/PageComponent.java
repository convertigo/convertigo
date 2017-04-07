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

public class PageComponent extends MobileComponent implements IStyleGenerator, ITemplateGenerator, IContainerOrdered {

	private static final long serialVersionUID = 188562781669238824L;
	
	private XMLVector<XMLVector<Long>> orderedComponents = new XMLVector<XMLVector<Long>>();
	
	public PageComponent() {
		super();
		
		orderedComponents = new XMLVector<XMLVector<Long>>();
		orderedComponents.add(new XMLVector<Long>());
	}

	@Override
	public PageComponent clone() throws CloneNotSupportedException {
		PageComponent cloned = (PageComponent) super.clone();
		cloned.vUIComponents = new LinkedList<UIComponent>();
		cloned.computedTemplate = null;
		cloned.computedStyle = null;
		cloned.isRoot = false;
		return cloned;
	}

	transient public boolean isRoot = false;
	
	@Override
	public Element toXml(Document document) throws EngineException {
		Element element = super.toXml(document);
        
		// Storing the page "isRoot" flag
		element.setAttribute("isRoot", new Boolean(isRoot).toString());
        
		return element;
	}
	
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		try {
			isRoot = new Boolean(element.getAttribute("isRoot")).booleanValue();
		}catch(Exception e) {
			throw new EngineException("Unable to configure the property 'isRoot' of the page \"" + getName() + "\".", e);
		}
	}
	
	public XMLVector<XMLVector<Long>> getOrderedComponents() {
		return orderedComponents;
	}
    
	public void setOrderedComponents(XMLVector<XMLVector<Long>> orderedComponents) {
		this.orderedComponents = orderedComponents;
	}
	
    private void insertOrderedComponent(UIComponent component, Long after) {
    	List<Long> ordered = orderedComponents.get(0);
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
    
    private void removeOrderedComponent(Long value) {
        Collection<Long> ordered = orderedComponents.get(0);
        ordered.remove(value);
        hasChanged = true;
    }
    
	public void insertAtOrder(DatabaseObject databaseObject, long priority) throws EngineException {
		increaseOrder(databaseObject, new Long(priority));
	}
    
    private void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
    	List<Long> ordered = null;
    	Long value = new Long(databaseObject.priority);
    	
    	if (databaseObject instanceof UIComponent)
    		ordered = orderedComponents.get(0);
    	
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
    	
    	if (databaseObject instanceof UIStyle) {
    		markStyleAsDirty();
    	}
    	else {
    		markTemplateAsDirty();
    	}
    }
    
    private void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
    	List<Long> ordered = null;
    	long value = databaseObject.priority;
    	
    	if (databaseObject instanceof UIComponent)
    		ordered = orderedComponents.get(0);
    	
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
    	
    	if (databaseObject instanceof UIStyle) {
    		markStyleAsDirty();
    	}
    	else {
    		markTemplateAsDirty();
    	}
    	
    }
    
	public void increasePriority(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof UIComponent)
			increaseOrder(databaseObject,null);
	}

	public void decreasePriority(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof UIComponent)
			decreaseOrder(databaseObject,null);
	}
    
    /**
     * Get representation of order for quick sort of a given database object.
     */
	@Override
    public Object getOrder(Object object) throws EngineException	{
        if (object instanceof UIComponent) {
        	List<Long> ordered = orderedComponents.get(0);
        	long time = ((UIComponent)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted component for page \""+ getName() +"\". UIComponent \""+ ((UIComponent)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        }
        else return super.getOrder(object);
    }
    
	/**
	 * The list of available page component for this application.
	 */
	transient private List<UIComponent> vUIComponents = new LinkedList<UIComponent>();
	
	protected void addUIComponent(UIComponent uiComponent) throws EngineException {
		checkSubLoaded();
		
		boolean isNew = uiComponent.bNew;
		boolean isCut = !isNew && uiComponent.getParent() == null;
		
		String newDatabaseObjectName = getChildBeanName(vUIComponents, uiComponent.getName(), uiComponent.bNew);
		uiComponent.setName(newDatabaseObjectName);
		
		vUIComponents.add(uiComponent);
		uiComponent.setParent(this);
		
        insertOrderedComponent(uiComponent,null);
        
        if (isNew || isCut) {
        	if (uiComponent instanceof UIStyle) {
        		markStyleAsDirty();
        	}
        	else {
        		markTemplateAsDirty();
        		if (uiComponent.hasStyle()) {
        			markStyleAsDirty();
        		}
        	}
        }
	}

	protected void removeUIComponent(UIComponent uiComponent) throws EngineException {
		checkSubLoaded();
		
		vUIComponents.remove(uiComponent);
		uiComponent.setParent(null);
		
        removeOrderedComponent(uiComponent.priority);
        
    	if (uiComponent instanceof UIStyle) {
    		markStyleAsDirty();
    	}
    	else {
    		markTemplateAsDirty();
    		if (uiComponent.hasStyle()) {
    			markStyleAsDirty();
    		}
    	}
	}

	public List<UIComponent> getUIComponentList() {
		checkSubLoaded();
		return sort(vUIComponents);
	}

	public UIComponent getUIComponentByName(String uiName) throws EngineException {
		checkSubLoaded();
		for (UIComponent uiComponent : vUIComponents)
			if (uiComponent.getName().equalsIgnoreCase(uiName)) return uiComponent;
		throw new EngineException("There is no UI component named \"" + uiName + "\" found into this page.");
	}
	
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.addAll(getUIComponentList());
		return rep;
	}

	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof UIComponent) {
			addUIComponent((UIComponent) databaseObject);
		} else {
			throw new EngineException("You cannot add to a page component a database object of type " + databaseObject.getClass().getName());
		}
    }

    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof UIComponent) {
			removeUIComponent((UIComponent) databaseObject);
		} else {
			throw new EngineException("You cannot remove from a page component a database object of type " + databaseObject.getClass().getName());
		}
		super.remove(databaseObject);
    }
    
    private String segment = "";

	public String getSegment() {
		return segment;
	}

	public void setSegment(String segment) {
		this.segment = segment;
	}

	transient private String computedTemplate = null;
	
	public String getComputedTemplate() {
		if (computedTemplate == null) {
			doComputeTemplate();
		}
		return computedTemplate;
	}
	
	protected synchronized void doComputeTemplate() {
		computedTemplate = computeTemplate();
	}
	
	@Override
	public String computeTemplate() {
		StringBuilder sb = new StringBuilder();
		Iterator<UIComponent> it = getUIComponentList().iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (!(component instanceof UIStyle)) {
				String tpl = component.computeTemplate();
				if (!tpl.isEmpty()) {
					sb.append(tpl).append(System.getProperty("line.separator"));
				}
			}
		}
		return sb.toString();
	}

	transient private String computedStyle = null;
	
	public String getComputedStyle() {
		if (computedStyle == null) {
			doComputeStyle();
		}
		return computedStyle;
	}
	
	protected synchronized void doComputeStyle() {
		computedStyle = computeStyle();
	}
	
	@Override
	public String computeStyle() {
		StringBuilder sb = new StringBuilder();
		sb.append("page-"+ getName().toLowerCase()).append(" {")
			.append(System.getProperty("line.separator"));
		
		Iterator<UIComponent> it = getUIComponentList().iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (component instanceof UIStyle) {
				String tpl = component.computeTemplate();
				if (!tpl.isEmpty()) {
					sb.append(tpl).append(System.getProperty("line.separator"));
				}
			}
			else if (component instanceof UIElement) {
				String tpl = ((UIElement)component).computeStyle();
				if (!tpl.isEmpty()) {
					sb.append(tpl).append(System.getProperty("line.separator"));
				}
			}
		}
		
		sb.append("}")
			.append(System.getProperty("line.separator"));
		
		return sb.toString();
	}

	public void markStyleAsDirty() throws EngineException {
		String oldComputed = getComputedStyle();
		doComputeStyle();
		String newComputed = getComputedStyle();
		
		if (!newComputed.equals(oldComputed)) {
			getProject().getMobileBuilder().pageStyleChanged(this);
		}
	}
	
	public void markTemplateAsDirty() throws EngineException {
		String oldComputed = getComputedTemplate();
		doComputeTemplate();
		String newComputed = getComputedTemplate();
		
		if (!newComputed.equals(oldComputed)) {
			getProject().getMobileBuilder().pageComputed(this);
		}
	}
	
	@Override
	public boolean testAttribute(String name, String value) {
		if (name.equals("isRoot")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isRoot));
		}
		return super.testAttribute(name, value);
	}

}
