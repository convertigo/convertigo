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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.beans.core.IContainerOrdered;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.MobileComponent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

@DboCategoryInfo(
		getCategoryId = "ApplicationComponent",
		getCategoryName = "Application",
		getIconClassCSS = "convertigo-action-newApplicationComponent"
	)
public class ApplicationComponent extends MobileComponent implements IStyleGenerator, ITemplateGenerator, IRouteGenerator, IContainerOrdered, ITagsProperty {
	
	private static final long serialVersionUID = 6142350115354549719L;

	private XMLVector<XMLVector<Long>> orderedComponents = new XMLVector<XMLVector<Long>>();
	private XMLVector<XMLVector<Long>> orderedRoutes = new XMLVector<XMLVector<Long>>();
	private XMLVector<XMLVector<Long>> orderedPages = new XMLVector<XMLVector<Long>>();
	private XMLVector<XMLVector<Long>> orderedMenus = new XMLVector<XMLVector<Long>>();
	
	private String tplProjectName = "";
	
	public ApplicationComponent() {
		super();
		
		orderedMenus = new XMLVector<XMLVector<Long>>();
		orderedMenus.add(new XMLVector<Long>());

		orderedPages = new XMLVector<XMLVector<Long>>();
		orderedPages.add(new XMLVector<Long>());
		
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
		cloned.vMenuComponents = new LinkedList<UIDynamicMenu>();
		cloned.vUIComponents = new LinkedList<UIComponent>();
		cloned.computedContents = null;
		cloned.rootPage = null;
		cloned.theme = null;
		
		cloned.c8o_version = c8o_version;
		return cloned;
	}

	
	@Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);
		
		try {
			NodeList properties = element.getElementsByTagName("property");
			
			// migration of componentScriptContent from String to FormatedContent
			Element propElement = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "componentScriptContent");
			if (propElement != null) {
				Element valueElement = (Element) XMLUtils.findChildNode(propElement, Node.ELEMENT_NODE);
				if (valueElement != null) {
					Document document = valueElement.getOwnerDocument();
					Object content = XMLUtils.readObjectFromXml(valueElement);
					if (content instanceof String) {
						FormatedContent formated = new FormatedContent((String) content);
						Element newValueElement = (Element)XMLUtils.writeObjectToXml(document, formated);
						propElement.replaceChild(newValueElement, valueElement);
						hasChanged = true;
						Engine.logBeans.warn("(ApplicationComponent) 'componentScriptContent' has been updated for the object \"" + getName() + "\"");
					}
				}
			}
		}
        catch(Exception e) {
            throw new EngineException("Unable to preconfigure the application component \"" + getName() + "\".", e);
        }
	}

	
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);

		String p_version = element.getOwnerDocument().getDocumentElement().getAttribute("version");
		if (p_version == null) {
			p_version = "0.0.0";
		}
		
		c8o_version = p_version;
	}

	@Override
	public MobileApplication getParent() {
		return (MobileApplication) super.getParent();
	}

	protected FormatedContent componentScriptContent = new FormatedContent("");
	
	public FormatedContent getComponentScriptContent() {
		return componentScriptContent;
	}
	
	public void setComponentScriptContent(FormatedContent componentScriptContent) {
		this.componentScriptContent = componentScriptContent;
	}
	
	public XMLVector<XMLVector<Long>> getOrderedMenus() {
		return orderedMenus;
	}
    
	public void setOrderedMenus(XMLVector<XMLVector<Long>> orderedMenus) {
		this.orderedMenus = orderedMenus;
	}
	
    private void insertOrderedMenu(UIDynamicMenu component, Long after) {
    	List<Long> ordered = orderedMenus.get(0);
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
    
    private void removeOrderedMenu(Long value) {
        Collection<Long> ordered = orderedMenus.get(0);
        ordered.remove(value);
        hasChanged = true;
    }
	
	public XMLVector<XMLVector<Long>> getOrderedPages() {
		return orderedPages;
	}
    
	public void setOrderedPages(XMLVector<XMLVector<Long>> orderedPages) {
		this.orderedPages = orderedPages;
	}
	
    private void insertOrderedPage(PageComponent component, Long after) {
    	List<Long> ordered = orderedPages.get(0);
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
    
    private void removeOrderedPage(Long value) {
        Collection<Long> ordered = orderedPages.get(0);
        ordered.remove(value);
        hasChanged = true;
    }
	
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
    	
    	if (databaseObject instanceof PageComponent)
    		ordered = orderedPages.get(0);
    	else if (databaseObject instanceof RouteComponent)
    		ordered = orderedRoutes.get(0);
    	else if (databaseObject instanceof UIDynamicMenu)
    		ordered = orderedMenus.get(0);
    	else if (databaseObject instanceof UIComponent)
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
    	
    	markApplicationAsDirty();
    }
    
    private void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
    	List<Long> ordered = null;
    	long value = databaseObject.priority;
    	
    	if (databaseObject instanceof PageComponent)
    		ordered = orderedPages.get(0);
    	else if (databaseObject instanceof RouteComponent)
    		ordered = orderedRoutes.get(0);
    	else if (databaseObject instanceof UIDynamicMenu)
    		ordered = orderedMenus.get(0);
    	else if (databaseObject instanceof UIComponent)
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
    	
    	markApplicationAsDirty();
    }
    
	public void increasePriority(DatabaseObject databaseObject) throws EngineException {
		increaseOrder(databaseObject,null);
		if (databaseObject instanceof PageComponent || databaseObject instanceof RouteComponent) {
			markComponentTsAsDirty();
		} else {
			markApplicationAsDirty();
		}
	}

	public void decreasePriority(DatabaseObject databaseObject) throws EngineException {
		decreaseOrder(databaseObject,null);
		if (databaseObject instanceof PageComponent || databaseObject instanceof RouteComponent) {
			markComponentTsAsDirty();
		} else {
			markApplicationAsDirty();
		}
	}
    
    /**
     * Get representation of order for quick sort of a given database object.
     */
	@Override
    public Object getOrder(Object object) throws EngineException	{
        if (object instanceof PageComponent) {
        	List<Long> ordered = orderedPages.get(0);
        	long time = ((PageComponent)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted page for application \""+ getName() +"\". PageComponent \""+ ((PageComponent)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        }
        else if (object instanceof RouteComponent) {
        	List<Long> ordered = orderedRoutes.get(0);
        	long time = ((RouteComponent)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted route for application \""+ getName() +"\". RouteComponent \""+ ((RouteComponent)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        }
        else if (object instanceof UIDynamicMenu) {
        	List<Long> ordered = orderedMenus.get(0);
        	long time = ((UIDynamicMenu)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted menu for application \""+ getName() +"\". MenuComponent \""+ ((UIDynamicMenu)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        }
        else if (object instanceof UIComponent) {
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
			markApplicationAsDirty();
		}
	}

	protected void removeRouteComponent(RouteComponent routeComponent) throws EngineException {
		checkSubLoaded();
		vRouteComponents.remove(routeComponent);
		
		removeOrderedRoute(routeComponent.priority);
		
		markApplicationAsDirty();
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
		addPageComponent(pageComponent, null);
	}
	
	protected void addPageComponent(PageComponent pageComponent, Long after) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vPageComponents, pageComponent.getName(), pageComponent.bNew);
		pageComponent.setName(newDatabaseObjectName);
		vPageComponents.add(pageComponent);
		if (pageComponent.isRoot) {
			setRootPage(pageComponent);
		}
		if (pageComponent.getTitle().isEmpty() || pageComponent.bNew) {
			pageComponent.setTitle("Title for "+ newDatabaseObjectName);
		}
		if (pageComponent.getSegment().isEmpty() || pageComponent.bNew) {
			pageComponent.setSegment(newDatabaseObjectName);
		}
		super.add(pageComponent);
		
		insertOrderedPage(pageComponent, after);
		
		if (pageComponent.bNew) {
			pageComponent.doComputeContents();
			getProject().getMobileBuilder().pageAdded(pageComponent);
		}
	}

	protected void removePageComponent(PageComponent pageComponent) throws EngineException {
		checkSubLoaded();
		vPageComponents.remove(pageComponent);
		
		removeOrderedPage(pageComponent.priority);
		
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
	
	/**
	 * The list of available menu component for this application.
	 */
	transient private List<UIDynamicMenu> vMenuComponents = new LinkedList<UIDynamicMenu>();
	
	protected void addMenuComponent(UIDynamicMenu menuComponent) throws EngineException {
		addMenuComponent(menuComponent, null);
	}
	
	protected void addMenuComponent(UIDynamicMenu menuComponent, Long after) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vMenuComponents, menuComponent.getName(), menuComponent.bNew);
		menuComponent.setName(newDatabaseObjectName);
		vMenuComponents.add(menuComponent);
		/*if (menuComponent.isRoot) {
			setRootMenu(menuComponent);
		}*/
		super.add(menuComponent);
		
		insertOrderedMenu(menuComponent, after);
		
		if (menuComponent.bNew) {
			markApplicationAsDirty();
		}
	}

	protected void removeMenuComponent(UIDynamicMenu menuComponent) throws EngineException {
		checkSubLoaded();
		vMenuComponents.remove(menuComponent);
		
		removeOrderedMenu(menuComponent.priority);
		
		markApplicationAsDirty();
	}
	
	public List<UIDynamicMenu> getMenuComponentList() {
		checkSubLoaded();
		return sort(vMenuComponents);
	}

	public UIDynamicMenu getMenuComponentByName(String menuName) throws EngineException {
		checkSubLoaded();
		for (UIDynamicMenu menuComponent : vMenuComponents)
			if (menuComponent.getName().equalsIgnoreCase(menuName)) return menuComponent;
		throw new EngineException("There is no menu component named \"" + menuName + "\" found into this application.");
	}
	
	transient private UIDynamicMenu rootMenu = null;
	
	public UIDynamicMenu getRootMenu() throws EngineException {
		if (rootMenu == null) {
			checkSubLoaded();
			/*for (UIDynamicMenu menuComponent : vMenuComponents) {
				if (menuComponent.isRoot) {
					rootMenu = menuComponent;
					break;
				}
			}*/
		}
		if (rootMenu == null) {
			if (Engine.isEngineMode()) {
				// Fire exception in Engine mode only!
				//throw new EngineException("There is no root menu defined for application \"" + getName() + "\".");
			}
			else {
				// In Studio mode we must be able to set a root menu!
			}
		}
		return rootMenu;
	}
	
	public synchronized void setRootMenu(UIDynamicMenu menuComponent) throws EngineException {
		if (menuComponent == null)
			throw new IllegalArgumentException("The value of argument 'menuComponent' is null");
		checkSubLoaded();
		if (vMenuComponents.contains(menuComponent)) {
			/*if (rootMenu == null) getRootMenu();
			if (rootMenu != null) rootMenu.isRoot = false;
			menuComponent.isRoot = true;
			rootMenu = menuComponent;*/
		} else throw new IllegalArgumentException("The value of argument 'menuComponent' is invalid: the menu does not belong to the application");
	}
	
	
	private String getThemeTplScss() {
		File appThemeTpl = new File(getIonicTplDir(), "src/theme/variables.scss");
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
    			UITheme uiTheme = (UITheme)uiComponent;
    			if (uiTheme.bNew) {
    				if (uiTheme.styleContent.getString().equals("")) {
    					uiTheme.styleContent = new FormatedContent(getThemeTplScss());
    				}
    			}
    			this.theme = uiTheme;
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
        	markApplicationAsDirty();
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
        }
        markApplicationAsDirty();
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
		rep.addAll(getMenuComponentList());
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
		} else if (databaseObject instanceof UIDynamicMenu) {
			addMenuComponent((UIDynamicMenu) databaseObject);
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
		} else if (databaseObject instanceof UIDynamicMenu) {
			removeMenuComponent((UIDynamicMenu) databaseObject);
		} else if (databaseObject instanceof UIComponent) {
			removeUIComponent((UIComponent) databaseObject);
		} else {
			throw new EngineException("You cannot remove from an application component a database object of type " + databaseObject.getClass().getName());
		}
		super.remove(databaseObject);
    }

    private transient String c8o_version = "";
    
    public String getC8oVersion() {
    	return c8o_version;
    }
    
	private transient JSONObject computedContents = null;
	
	private JSONObject initJsonComputed() {
		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject()
						.put("style", "")
						.put("theme", "")
						.put("route", "")
						.put("template", "");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonObject;
	}
	
	public JSONObject getComputedContents() {
		if (computedContents == null) {
			doComputeContents();
		}
		return computedContents;
	}
	
	protected synchronized void doComputeContents() {
		try {
			JSONObject newComputedContent = initJsonComputed();
			
			newComputedContent.put("style", computeStyle());
			newComputedContent.put("theme", computeTheme());
			newComputedContent.put("route", computeRoute());
			newComputedContent.put("template", computeTemplate());
			
			computedContents = newComputedContent;
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
    public void markApplicationAsDirty() throws EngineException {
		if (isImporting) {
			return;
		}
    	
		try {
			JSONObject oldComputedContent = computedContents == null ? 
					null :new JSONObject(computedContents.toString());
			
			doComputeContents();
			
			JSONObject newComputedContent = computedContents == null ? 
					null :new JSONObject(computedContents.toString());
			
			if (oldComputedContent != null && newComputedContent != null) {
				if (!(newComputedContent.getString("style")
						.equals(oldComputedContent.getString("style")))) {
					getProject().getMobileBuilder().appStyleChanged(this);
				}
			}
			if (oldComputedContent != null && newComputedContent != null) {
				if (!(newComputedContent.getString("theme")
						.equals(oldComputedContent.getString("theme")))) {
					getProject().getMobileBuilder().appThemeChanged(this);
				}
			}
			if (oldComputedContent != null && newComputedContent != null) {
				if (!(newComputedContent.getString("route")
						.equals(oldComputedContent.getString("route")))) {
					getProject().getMobileBuilder().appRouteChanged(this);
				}
			}
			if (oldComputedContent != null && newComputedContent != null) {
				if (!(newComputedContent.getString("template")
						.equals(oldComputedContent.getString("template")))) {
					getProject().getMobileBuilder().appTemplateChanged(this);
				}
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
    }
    
	public void markRootAsDirty() throws EngineException {
		getProject().getMobileBuilder().appRootChanged(this);
	}
	
	public void markComponentTsAsDirty() throws EngineException {
		getProject().getMobileBuilder().appCompTsChanged(this);
	}

	public void markModuleTsAsDirty() throws EngineException {
		getProject().getMobileBuilder().appModuleTsChanged(this);
	}
	
    /*
     * The computed template (see app.html)
     */
	public String getComputedTemplate() {
		try {
			return getComputedContents().getString("template");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "";
	}
    
	@Override
	public String computeTemplate() {
		StringBuilder sb = new StringBuilder();
		
		Iterator<UIDynamicMenu> it = getMenuComponentList().iterator();
		while (it.hasNext()) {
			UIDynamicMenu menu = it.next();
			String menuTemplate = menu.computeTemplate();
			if (!menuTemplate.isEmpty()) {
				sb.append(menuTemplate).append(System.lineSeparator());
			}
		}
		
		sb.append("<ion-nav [root]=\"rootPage\" #content swipeBackEnabled=\"false\"></ion-nav>");
		sb.append(System.lineSeparator());
		
		return sb.toString();
	}

	/*
	 * The computed routing table (see app.component.ts)
	 */
	public String getComputedRoute() {
		try {
			return getComputedContents().getString("route");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "";
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
    
	public String getComputedStyle() {
		try {
			return getComputedContents().getString("style");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "";
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

	public String getComputedTheme() {
		try {
			return getComputedContents().getString("theme");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "";
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

	public File getIonicTplDir() {
		String tplProjectName = getTplProjectName();
		if (StringUtils.isBlank(tplProjectName)) {
			tplProjectName = getProject().getName();
		}
		return new File(Engine.PROJECTS_PATH + "/" + tplProjectName + "/ionicTpl");
	}
	
	public String getTplProjectName() {
		return tplProjectName;
	}

	public void setTplProjectName(String tplProjectName) {
		this.tplProjectName = tplProjectName;
	}

	private boolean hasCompatibleTemplate(String project) {
		// TODO: to be changed after 7.5.0 release...
		File tplDir = new File(Engine.PROJECTS_PATH + "/" + project + "/ionicTpl");
		if (tplDir.exists()) {
			if (new File(tplDir,"src/services/actionbeans.service.ts").exists()) {
				return true;
			}
		}
		return false;
	}
	@Override
	public String[] getTagsForProperty(String propertyName) {
		if ("tplProjectName".equals(propertyName)) {
			List<String> projects = new LinkedList<>();
			projects.add("");
			
			for (String project: Engine.theApp.databaseObjectsManager.getAllProjectNamesList(false)) {
				if (hasCompatibleTemplate(project)) {
					projects.add(project);
				};
			}
			
			return projects.toArray(new String[projects.size()]);
		}
		return null;
	}
	
	public Map<String, Set<String>> getInfoMap() {
		Map<String, Set<String>> map = new HashMap<String, Set<String>>();
		for (PageComponent page : getPageComponentList()) {
			map.putAll(page.getInfoMap());
		}
		return map;
	}
	
	@Override
	public void setParent(DatabaseObject databaseObject) {
		super.setParent(databaseObject);
		checkFolder();
	}
	
	private void checkFolder() {
		File folder = new File(getProject().getDirPath() + "/Flashupdate");
		if (!folder.exists()) {
			try {
				File templateFolder = new File(Engine.TEMPLATES_PATH, "base/Flashupdate");
				FileUtils.copyDirectory(templateFolder, folder);
			} catch (Exception e) {
				Engine.logBeans.warn("(MobileApplication) The folder '" + folder.getAbsolutePath() + "' doesn't exist and cannot be created", e);
			}
		}
	}
}
