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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IContainerOrdered;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.MobileComponent;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.FileUtils;

@DboCategoryInfo(
		getCategoryId = "ApplicationComponent",
		getCategoryName = "Application",
		getIconClassCSS = "convertigo-action-newApplicationComponent"
	)
public class ApplicationComponent extends MobileComponent implements IStyleGenerator, ITemplateGenerator, IRouteGenerator, IContainerOrdered {
	
	private static final long serialVersionUID = 6142350115354549719L;

	private XMLVector<XMLVector<Long>> orderedComponents = new XMLVector<XMLVector<Long>>();
	
	public ApplicationComponent() {
		super();
		
		orderedRoutes = new XMLVector<XMLVector<Long>>();
		orderedRoutes.add(new XMLVector<Long>());
		
		orderedComponents = new XMLVector<XMLVector<Long>>();
		orderedComponents.add(new XMLVector<Long>());
	}

	@Override
	public ApplicationComponent clone() throws CloneNotSupportedException {
		ApplicationComponent cloned = (ApplicationComponent) super.clone();
		cloned.vRouteComponents = new LinkedList<RouteComponent>();
		cloned.vPageComponents = new LinkedList<PageComponent>();
		cloned.vUIComponents = new LinkedList<UIComponent>();
		cloned.computedTemplate = null;
		cloned.computedStyle = null;
		cloned.computedTheme = null;
		cloned.computedRoute = null;
		cloned.rootPage = null;
		cloned.theme = null;
		return cloned;
	}

	
	@Override
	public MobileApplication getParent() {
		return (MobileApplication) super.getParent();
	}

	protected String componentScriptContent = "";
	
	public String getComponentScriptContent() {
		return componentScriptContent;
	}
	
	public void setComponentScriptContent(String componentScriptContent) {
		this.componentScriptContent = componentScriptContent;
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
    	
    	if (databaseObject instanceof RouteComponent)
    		ordered = orderedRoutes.get(0);
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
    	
    	if (databaseObject instanceof RouteComponent) {
    		markRouteAsDirty();
    	} else if (databaseObject instanceof UITheme) {
   			markThemeAsDirty();
    	} else if (databaseObject instanceof UIStyle) {
   			markStyleAsDirty();
    	} else if (databaseObject instanceof UIComponent) {
    		markTemplateAsDirty();
    	}
    }
    
    private void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
    	List<Long> ordered = null;
    	long value = databaseObject.priority;
    	
    	if (databaseObject instanceof RouteComponent)
    		ordered = orderedRoutes.get(0);
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
    	
    	if (databaseObject instanceof RouteComponent) {
    		markRouteAsDirty();
    	} else if (databaseObject instanceof UITheme) {
   			markThemeAsDirty();
    	} else if (databaseObject instanceof UIStyle) {
   			markStyleAsDirty();
    	} else if (databaseObject instanceof UIComponent) {
    		markTemplateAsDirty();
    	}
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
        if (object instanceof UIComponent) {
        	List<Long> ordered = orderedComponents.get(0);
        	long time = ((UIComponent)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted component for application \""+ getName() +"\". UIComponent \""+ ((UIComponent)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        }
        else return super.getOrder(object);
    }
	
	/**
	 * The list of available routes for this application.
	 */
	transient private List<RouteComponent> vRouteComponents = new LinkedList<RouteComponent>();
	
	protected void addRouteComponent(RouteComponent routeComponent) throws EngineException {
		addRouteComponent(routeComponent, null);
	}
	
	protected void addRouteComponent(RouteComponent routeComponent, Long after) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vRouteComponents, routeComponent.getName(), routeComponent.bNew);
		routeComponent.setName(newDatabaseObjectName);
		vRouteComponents.add(routeComponent);
		super.add(routeComponent);
		
		insertOrderedRoute(routeComponent, after);
		
		if (routeComponent.bNew) {
			markRouteAsDirty();
		}
	}

	protected void removeRouteComponent(RouteComponent routeComponent) throws EngineException {
		checkSubLoaded();
		vRouteComponents.remove(routeComponent);
		
		removeOrderedRoute(routeComponent.priority);
		
		markRouteAsDirty();
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
			pageComponent.doComputeStyle();
			pageComponent.doComputeTemplate();
			getProject().getMobileBuilder().pageAdded(pageComponent);
		}
	}

	protected void removePageComponent(PageComponent pageComponent) throws EngineException {
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
	
	
	private String getThemeTplScss() {
		File projectDir = new File(getProject().getDirPath());
		File appThemeTpl = new File(projectDir, "ionicTpl/src/theme/variables.scss");
		try {
			return FileUtils.readFileToString(appThemeTpl, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	/**
	 * The application's theme
	 */
	private transient UITheme theme = null;
	
	/**
	 * The list of available ui components for this application.
	 */
	transient private List<UIComponent> vUIComponents = new LinkedList<UIComponent>();
	
	protected void addUIComponent(UIComponent uiComponent, Long after) throws EngineException {
		checkSubLoaded();
		
    	if (uiComponent instanceof UITheme) {
    		if (this.theme != null) {
    			throw new EngineException("The mobile application \"" + getName() + "\" already contains a theme! Please delete it first.");
    		}
    		else {
    			((UITheme)uiComponent).styleContent = getThemeTplScss();
    		}
    	}
		
		boolean isNew = uiComponent.bNew;
		boolean isCut = !isNew && uiComponent.getParent() == null;
		
		String newDatabaseObjectName = getChildBeanName(vUIComponents, uiComponent.getName(), uiComponent.bNew);
		uiComponent.setName(newDatabaseObjectName);
		
		vUIComponents.add(uiComponent);
		uiComponent.setParent(this);
		
        insertOrderedComponent(uiComponent, after);
        
        if (isNew || isCut) {
        	if (uiComponent instanceof UITheme) {
        		this.theme = (UITheme)uiComponent;
       			markThemeAsDirty();
        	} else if (uiComponent instanceof UIStyle) {
       			markStyleAsDirty();
        	} else {
        		markTemplateAsDirty();
        		if (uiComponent.hasStyle()) {
        			markStyleAsDirty();
        		}
        	}
        }
	}
	
	protected void addUIComponent(UIComponent uiComponent) throws EngineException {
		addUIComponent(uiComponent, null);
	}

	protected void removeUIComponent(UIComponent uiComponent) throws EngineException {
		checkSubLoaded();
		
		vUIComponents.remove(uiComponent);
		uiComponent.setParent(null);
		
        removeOrderedComponent(uiComponent.priority);
        
        if (uiComponent != null && uiComponent.equals(this.theme)) {
    		this.theme = null;
    		markThemeAsDirty();
        }
        else if (uiComponent instanceof UIStyle) {
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
		if (theme != null) rep.add(theme);
		rep.addAll(getRouteComponentList());
		rep.addAll(getPageComponentList());
		rep.addAll(getUIComponentList());
		return rep;
	}

	@Override
	public void add(DatabaseObject databaseObject, Long after) throws EngineException {
		if (databaseObject instanceof RouteComponent) {
			addRouteComponent((RouteComponent) databaseObject, after);
		} else if (databaseObject instanceof PageComponent) {
			addPageComponent((PageComponent) databaseObject);
		} else if (databaseObject instanceof UIComponent) {
			addUIComponent((UIComponent) databaseObject, after);
		} else {
			throw new EngineException("You cannot add to an application component a database object of type " + databaseObject.getClass().getName());
		}
	}
	
	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		add(databaseObject, null);
    }

    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof RouteComponent) {
			removeRouteComponent((RouteComponent) databaseObject);
		} else if (databaseObject instanceof PageComponent) {
			removePageComponent((PageComponent) databaseObject);
		} else if (databaseObject instanceof UIComponent) {
			removeUIComponent((UIComponent) databaseObject);
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
			doComputeTemplate();
		}
		return computedTemplate;
	}
    
	protected synchronized void doComputeTemplate() {
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
	
	protected synchronized void doComputeRoute() {
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
		Iterator<UIComponent> it = getUIComponentList().iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (component instanceof UIStyle && !(component instanceof UITheme)) {
				String tpl = component.computeTemplate();
				if (!tpl.isEmpty()) {
					sb.append(tpl).append(System.getProperty("line.separator"));
				}
			}
		}
		
		return sb.toString();
	}

	transient private String computedTheme = null;
	
	public String getComputedTheme() {
		if (computedTheme == null) {
			doComputeTheme();
		}
		return computedTheme;
	}
	
	protected synchronized void doComputeTheme() {
		computedTheme = computeTheme();
	}

	public String computeTheme() {
		StringBuilder sb = new StringBuilder();
		if (theme != null) {
			String tpl = theme.computeTemplate();
			if (!tpl.isEmpty()) {
				sb.append(tpl);
			}
		}
		
		if (sb.length() == 0) {
			sb.append(getThemeTplScss());
		}
		
		return sb.toString();
	}
	
	public void markThemeAsDirty() throws EngineException {
		String oldComputed = getComputedTheme();
		doComputeTheme();
		String newComputed = getComputedTheme();
		
		if (!newComputed.equals(oldComputed) || theme == null) {
			getProject().getMobileBuilder().appThemeChanged(this);
		}
	}
	
	public void markStyleAsDirty() throws EngineException {
		String oldComputed = getComputedStyle();
		doComputeStyle();
		String newComputed = getComputedStyle();
		
		if (!newComputed.equals(oldComputed)) {
			getProject().getMobileBuilder().appStyleChanged(this);
		}
	}
	
	public void markTemplateAsDirty() throws EngineException {
		String oldComputed = getComputedTemplate();
		doComputeTemplate();
		String newComputed = getComputedTemplate();
		
		if (!newComputed.equals(oldComputed)) {
			//getProject().getMobileBuilder().appComputed(this);
		}
	}
	
	public void markRouteAsDirty() throws EngineException {
		String oldComputed = getComputedRoute();
		doComputeRoute();
		String newComputed = getComputedRoute();
		
		if (!newComputed.equals(oldComputed)) {
			getProject().getMobileBuilder().appRouteChanged(this);
		}
	}
	
	public void markRootAsDirty() throws EngineException {
		getProject().getMobileBuilder().appRootChanged(this);
	}
	
	public void markComponentTsAsDirty() throws EngineException {
		getProject().getMobileBuilder().appCompTsChanged(this);
	}
}
