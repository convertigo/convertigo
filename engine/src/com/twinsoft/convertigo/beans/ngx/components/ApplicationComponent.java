/*
 * Copyright (c) 2001-2024 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.ngx.components;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import com.twinsoft.convertigo.beans.core.IApplicationComponent;
import com.twinsoft.convertigo.beans.core.IContainerOrdered;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.UIAppGuard.AppGuardType;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

@DboCategoryInfo(
		getCategoryId = "ApplicationComponent",
		getCategoryName = "Application",
		getIconClassCSS = "convertigo-action-newApplicationComponent"
	)
public class ApplicationComponent extends MobileComponent implements IApplicationComponent, IScriptComponent, IScriptGenerator, IStyleGenerator, ITemplateGenerator, IContainerOrdered, ITagsProperty {
	
	private static final long serialVersionUID = 6142350115354549719L;
	public static final String defaultTplProjectName = "mobilebuilder_tpl_8_0_0_ngx";

	transient private XMLVector<XMLVector<Long>> orderedComponents = new XMLVector<XMLVector<Long>>();
	transient private XMLVector<XMLVector<Long>> orderedPages = new XMLVector<XMLVector<Long>>();
	transient private XMLVector<XMLVector<Long>> orderedMenus = new XMLVector<XMLVector<Long>>();
	transient private XMLVector<XMLVector<Long>> orderedSharedActions = new XMLVector<XMLVector<Long>>();
	transient private XMLVector<XMLVector<Long>> orderedSharedComponents = new XMLVector<XMLVector<Long>>();
	
	transient private String tplProjectVersion = null;
	
	private String tplProjectName = defaultTplProjectName;
	private String splitPaneLayout = "not set";
	private boolean isPWA = false;
	private boolean useClickForTap = false;
	
	transient private Runnable _updateSourceFiles;
	
	public ApplicationComponent() {
		super();
		
		orderedMenus = new XMLVector<XMLVector<Long>>();
		orderedMenus.add(new XMLVector<Long>());

		orderedPages = new XMLVector<XMLVector<Long>>();
		orderedPages.add(new XMLVector<Long>());
		
		orderedComponents = new XMLVector<XMLVector<Long>>();
		orderedComponents.add(new XMLVector<Long>());
		
		orderedSharedActions = new XMLVector<XMLVector<Long>>();
		orderedSharedActions.add(new XMLVector<Long>());
		
		orderedSharedComponents = new XMLVector<XMLVector<Long>>();
		orderedSharedComponents.add(new XMLVector<Long>());
		
		if (Engine.theApp == null) {
			tplProjectName = "";
		}
	}

	@Override
	public ApplicationComponent clone() throws CloneNotSupportedException {
		ApplicationComponent cloned = (ApplicationComponent) super.clone();
		cloned.vPageComponents = new LinkedList<PageComponent>();
		cloned.vMenuComponents = new LinkedList<UIDynamicMenu>();
		cloned.vSharedActions = new LinkedList<UIActionStack>();
		cloned.vSharedComponents = new LinkedList<UISharedComponent>();
		cloned.vUIComponents = new LinkedList<UIComponent>();
		cloned.appImports = new HashMap<String, String>();
		cloned.appDeclarations = new HashMap<String, String>();
		cloned.appConstructors = new HashMap<String, String>();
		cloned.appFunctions = new HashMap<String, String>();
		cloned.appTemplates = new HashMap<String, String>();
		cloned.computedContents = null;
		cloned.contributors = null;
		cloned.rootPage = null;
		cloned.theme = null;
		cloned.tplProjectVersion = null;
		
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
    		after = 0L;
    		if (size > 0)
    			after = ordered.get(ordered.size()-1);
    	}
    	
   		int order = ordered.indexOf(after);
    	ordered.add(order+1, component.priority);
    	hasChanged = !isImporting;
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
    		after = 0L;
    		if (size > 0)
    			after = ordered.get(ordered.size()-1);
    	}
    	
   		int order = ordered.indexOf(after);
    	ordered.add(order+1, component.priority);
    	hasChanged = !isImporting;
    }
    
    private void removeOrderedPage(Long value) {
        Collection<Long> ordered = orderedPages.get(0);
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
    		after = 0L;
    		if (size > 0)
    			after = ordered.get(ordered.size()-1);
    	}
    	
   		int order = ordered.indexOf(after);
    	ordered.add(order+1, component.priority);
    	hasChanged = !isImporting;
    }
    
    private void removeOrderedComponent(Long value) {
        Collection<Long> ordered = orderedComponents.get(0);
        ordered.remove(value);
        hasChanged = true;
    }
    
	public XMLVector<XMLVector<Long>> getOrderedSharedActions() {
		return orderedSharedActions;
	}
    
	public void setOrderedSharedActions(XMLVector<XMLVector<Long>> orderedStacks) {
		this.orderedSharedActions = orderedStacks;
	}
	
    private void insertOrderedSharedAction(UIActionStack component, Long after) {
    	List<Long> ordered = orderedSharedActions.get(0);
    	int size = ordered.size();
    	
    	if (ordered.contains(component.priority))
    		return;
    	
    	if (after == null) {
    		after = 0L;
    		if (size > 0)
    			after = ordered.get(ordered.size()-1);
    	}
    	
   		int order = ordered.indexOf(after);
    	ordered.add(order+1, component.priority);
    	hasChanged = !isImporting;
    }
    
    private void removeOrderedSharedAction(Long value) {
        Collection<Long> ordered = orderedSharedActions.get(0);
        ordered.remove(value);
        hasChanged = true;
    }
    
	public XMLVector<XMLVector<Long>> getOrderedSharedComponents() {
		return orderedSharedComponents;
	}
    
	public void setOrderedSharedComponents(XMLVector<XMLVector<Long>> orderedComponents) {
		this.orderedSharedComponents = orderedComponents;
	}
	
    private void insertOrderedSharedComponent(UISharedComponent component, Long after) {
    	List<Long> ordered = orderedSharedComponents.get(0);
    	int size = ordered.size();
    	
    	if (ordered.contains(component.priority))
    		return;
    	
    	if (after == null) {
    		after = 0L;
    		if (size > 0)
    			after = ordered.get(ordered.size()-1);
    	}
    	
   		int order = ordered.indexOf(after);
    	ordered.add(order+1, component.priority);
    	hasChanged = !isImporting;
    }
    
    private void removeOrderedSharedComponent(Long value) {
        Collection<Long> ordered = orderedSharedComponents.get(0);
        ordered.remove(value);
        hasChanged = true;
    }
    
    public void insertAtOrder(DatabaseObject databaseObject, long priority) throws EngineException {
		increaseOrder(databaseObject, priority);
	}
    
    private void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
    	List<Long> ordered = null;
    	Long value = Long.valueOf(databaseObject.priority);
    	
    	if (databaseObject instanceof PageComponent)
    		ordered = orderedPages.get(0);
    	else if (databaseObject instanceof UIDynamicMenu)
    		ordered = orderedMenus.get(0);
    	else if (databaseObject instanceof UIActionStack)
    		ordered = orderedSharedActions.get(0);
    	else if (databaseObject instanceof UISharedComponent)
    		ordered = orderedSharedComponents.get(0);
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
    }
    
    private void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
    	List<Long> ordered = null;
    	long value = databaseObject.priority;
    	
    	if (databaseObject instanceof PageComponent)
    		ordered = orderedPages.get(0);
    	else if (databaseObject instanceof UIDynamicMenu)
    		ordered = orderedMenus.get(0);
    	else if (databaseObject instanceof UIActionStack)
    		ordered = orderedSharedActions.get(0);
    	else if (databaseObject instanceof UISharedComponent)
    		ordered = orderedSharedComponents.get(0);
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
    }
    
	public void increasePriority(DatabaseObject databaseObject) throws EngineException {
		increaseOrder(databaseObject,null);
	}

	public void decreasePriority(DatabaseObject databaseObject) throws EngineException {
		decreaseOrder(databaseObject,null);
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
        else if (object instanceof UIDynamicMenu) {
        	List<Long> ordered = orderedMenus.get(0);
        	long time = ((UIDynamicMenu)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted menu for application \""+ getName() +"\". MenuComponent \""+ ((UIDynamicMenu)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        }
        else if (object instanceof UIActionStack) {
        	List<Long> ordered = orderedSharedActions.get(0);
        	long time = ((UIActionStack)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted stack for application \""+ getName() +"\". SharedAction \""+ ((UIActionStack)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        }
        else if (object instanceof UISharedComponent) {
        	List<Long> ordered = orderedSharedComponents.get(0);
        	long time = ((UISharedComponent)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted stack for application \""+ getName() +"\". SharedComponent \""+ ((UISharedComponent)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
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
			pageComponent.setTitle(PageComponent.TITLE_PREFIX + newDatabaseObjectName);
		}
		if (pageComponent.getSegment().isEmpty() || pageComponent.bNew) {
			pageComponent.setSegment(PageComponent.SEGMENT_PREFIX + newDatabaseObjectName.toLowerCase());
		}
		super.add(pageComponent);
		
		insertOrderedPage(pageComponent, after);
	}

	protected void removePageComponent(PageComponent pageComponent) throws EngineException {
		checkSubLoaded();
		vPageComponents.remove(pageComponent);
		
		removeOrderedPage(pageComponent.priority);
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
	}

	protected void removeMenuComponent(UIDynamicMenu menuComponent) throws EngineException {
		checkSubLoaded();
		vMenuComponents.remove(menuComponent);
		removeOrderedMenu(menuComponent.priority);
	}
	
	public List<UIDynamicMenu> getMenuComponentList() {
		checkSubLoaded();
		return sort(vMenuComponents);
	}

	public String getDefaultStartMenuId() {
		for (UIDynamicMenu menu: getMenuComponentList()) {
			if ("start".equals(menu.getSide())) {
				return menu.getId();
			}
		}
		return "";
	}
	
	public String getDefaultEndMenuId() {
		for (UIDynamicMenu menu: getMenuComponentList()) {
			if ("end".equals(menu.getSide())) {
				return menu.getId();
			}
		}
		return "";
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
	
	public boolean hasGuard(AppGuardType guardType) {
		for (UIComponent uic: getUIComponentList()) {
			if (uic instanceof UIAppGuard) {
				if (guardType.equals(((UIAppGuard)uic).getGuardType())) {
					return true;
				}
			}
		}
		return false;
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
    	
    	if (uiComponent instanceof UIAppGuard) {
    		UIAppGuard guard = (UIAppGuard)uiComponent;
    		if (guard.bNew) {
    			AppGuardType guardType = guard.getGuardType();
    			if (hasGuard(AppGuardType.onCanActivate) && hasGuard(AppGuardType.onCanDeactivate)) {
    				throw new EngineException("The mobile application \"" + getName() + "\" already contains a \""+ guardType.getTopic() +"\" guard! Please delete it first.");
    			}
    			if (guardType.equals(AppGuardType.onCanActivate) && hasGuard(AppGuardType.onCanActivate)) {
    				guard.setGuardType(AppGuardType.onCanDeactivate);
    			}
    			if (guardType.equals(AppGuardType.onCanDeactivate) && hasGuard(AppGuardType.onCanDeactivate)) {
    				guard.setGuardType(AppGuardType.onCanActivate);
    			}
    		}
    	}
		
		String newDatabaseObjectName = getChildBeanName(vUIComponents, uiComponent.getName(), uiComponent.bNew);
		uiComponent.setName(newDatabaseObjectName);
		
		vUIComponents.add(uiComponent);
		uiComponent.setParent(this);
        insertOrderedComponent(uiComponent, after);
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
	
	public List<UIAppEvent> getUIAppEventList() {
		List<UIAppEvent> eventList = new ArrayList<>();
		for (UIComponent uiComponent : getUIComponentList()) {
			if (uiComponent instanceof UIAppEvent) {
				eventList.add((UIAppEvent) uiComponent);
			}
		}
		return eventList;
	}
	
	public List<UIEventSubscriber> getUIEventSubscriberList() {
		List<UIEventSubscriber> eventList = new ArrayList<>();
		for (UIComponent uiComponent : getUIComponentList()) {
			if (uiComponent instanceof UIEventSubscriber) {
				eventList.add((UIEventSubscriber) uiComponent);
			}
		}
		return eventList;
	}
	
	public List<UIFont> getUIFontList() {
		List<UIFont> eventList = new ArrayList<>();
		for (UIComponent uiComponent : getUIComponentList()) {
			if (uiComponent instanceof UIFont) {
				eventList.add((UIFont) uiComponent);
			}
		}
		return eventList;
	}
	
	/**
	 * The list of available stack of shared actions for this application.
	 */
	transient private List<UIActionStack> vSharedActions = new LinkedList<UIActionStack>();
	
	protected void addSharedAction(UIActionStack stackComponent) throws EngineException {
		addSharedAction(stackComponent, null);
	}
	
	protected void addSharedAction(UIActionStack stackComponent, Long after) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vSharedActions, stackComponent.getName(), stackComponent.bNew);
		stackComponent.setName(newDatabaseObjectName);
		vSharedActions.add(stackComponent);
		super.add(stackComponent);
		
		insertOrderedSharedAction(stackComponent, after);
	}

	protected void removeSharedAction(UIActionStack stackComponent) throws EngineException {
		checkSubLoaded();
		vSharedActions.remove(stackComponent);
		removeOrderedSharedAction(stackComponent.priority);
	}

	public List<UIActionStack> getSharedActionList() {
		checkSubLoaded();
		return sort(vSharedActions);
	}
	
	/**
	 * The list of available stack of shared components for this application.
	 */
	transient private List<UISharedComponent> vSharedComponents = new LinkedList<UISharedComponent>();
	
	protected void addSharedComponent(UISharedComponent stackComponent) throws EngineException {
		addSharedComponent(stackComponent, null);
	}
	
	protected void addSharedComponent(UISharedComponent stackComponent, Long after) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vSharedComponents, stackComponent.getName(), stackComponent.bNew);
		stackComponent.setName(newDatabaseObjectName);
		vSharedComponents.add(stackComponent);
		super.add(stackComponent);
		
		insertOrderedSharedComponent(stackComponent, after);
	}

	protected void removeSharedComponent(UISharedComponent stackComponent) throws EngineException {
		checkSubLoaded();
		vSharedComponents.remove(stackComponent);
		removeOrderedSharedComponent(stackComponent.priority);
	}

	public List<UISharedComponent> getSharedComponentList() {
		checkSubLoaded();
		return sort(vSharedComponents);
	}
	
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		if (theme != null) rep.add(theme);
		rep.addAll(getMenuComponentList());
		rep.addAll(getPageComponentList());
		rep.addAll(getSharedActionList());
		rep.addAll(getSharedComponentList());
		rep.addAll(getUIComponentList());
		return rep;
	}

	@Override
	public void add(DatabaseObject databaseObject, Long after) throws EngineException {
		if (databaseObject instanceof PageComponent) {
			addPageComponent((PageComponent) databaseObject, after);
		} else if (databaseObject instanceof UIDynamicMenu) {
			addMenuComponent((UIDynamicMenu) databaseObject, after);
		} else if (databaseObject instanceof UIActionStack) {
			addSharedAction((UIActionStack) databaseObject, after);
		} else if (databaseObject instanceof UISharedComponent) {
			addSharedComponent((UISharedComponent) databaseObject, after);
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
		if (databaseObject instanceof PageComponent) {
			removePageComponent((PageComponent) databaseObject);
		} else if (databaseObject instanceof UIDynamicMenu) {
			removeMenuComponent((UIDynamicMenu) databaseObject);
		} else if (databaseObject instanceof UIActionStack) {
			removeSharedAction((UIActionStack) databaseObject);
		} else if (databaseObject instanceof UISharedComponent) {
			removeSharedComponent((UISharedComponent) databaseObject);
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
    
	private transient Map<String, String> appImports = new HashMap<String, String>();
	
	private boolean hasImport(String name) {
		return appImports.containsKey(name) ||
				getProject().getMobileBuilder().hasTplAppCompTsImport(name);
	}
	
	private boolean hasCustomImport(String name) {
		synchronized (componentScriptContent) {
			String c8o_UserCustoms = componentScriptContent.getString();
			String importMarker = MobileBuilder.getMarker(c8o_UserCustoms, "AppImport");
			Map<String, String> map = new HashMap<String, String>(10);
			MobileBuilder.initMapImports(map, importMarker);
			return map.containsKey(name);
		}
	}
	
	
	public boolean addImport(String name, String path) {
		if (name != null && path != null && !name.isEmpty() && !path.isEmpty()) {
			synchronized (appImports) {
				if (!hasImport(name) && !hasCustomImport(name)) {
					appImports.put(name, path);
					return true;
				}
			}
		}
		return false;
	}
    
	private transient Map<String, String> appFunctions = new HashMap<String, String>();
	
	private boolean hasFunction(String name) {
		return appFunctions.containsKey(name);
	}
	
	@Override
	public boolean addFunction(String name, String code) {
		if (name != null && code != null && !name.isEmpty() && !code.isEmpty()) {
			synchronized (appFunctions) {
				if (!hasFunction(name)) {
					appFunctions.put(name, code);
					return true;
				}
			}
		}
		return false;
	}
	
	private transient Map<String, String> appDeclarations = new HashMap<String, String>();
	
	private boolean hasDeclaration(String name) {
		return appDeclarations.containsKey(name);
	}
	
	@Override
	public boolean addDeclaration(String name, String code) {
		if (name != null && code != null && !name.isEmpty() && !code.isEmpty()) {
			synchronized (appDeclarations) {
				if (!hasDeclaration(name)) {
					appDeclarations.put(name, code);
					return true;
				}
			}
		}
		return false;
	}
	
	private transient Map<String, String> appConstructors = new HashMap<String, String>();
	
	private boolean hasConstructor(String name) {
		return appConstructors.containsKey(name);
	}
	
	@Override
	public boolean addConstructor(String name, String code) {
		if (name != null && code != null && !name.isEmpty() && !code.isEmpty()) {
			synchronized (appConstructors) {
				if (!hasConstructor(name)) {
					appConstructors.put(name, code);
					return true;
				}
			}
		}
		return false;
	}
	
	private transient Map<String, String> appTemplates = new HashMap<String, String>();
	
	private boolean hasTemplate(String name) {
		return appTemplates.containsKey(name);
	}
	
	@Override
	public boolean addTemplate(String name, String code) {
		if (name != null && code != null && !name.isEmpty() && !code.isEmpty()) {
			synchronized (appTemplates) {
				if (!hasTemplate(name)) {
					appTemplates.put(name, code);
					return true;
				}
			}
		}
		return false;
	}
	
	private transient List<Contributor> contributors = null;
	
	public synchronized List<Contributor> getContributors() {
		if (contributors == null) {
			doGetContributors();
		}
		return contributors;
	}
	
	protected void doGetContributors() {
		contributors = new ArrayList<>();
		Set<UIComponent> done = new HashSet<>();
		for (UIFont uiFont : getUIFontList()) {
			uiFont.addContributors(done, contributors);
		}
		for (UIDynamicMenu uiMenu : getMenuComponentList()) {
			uiMenu.addContributors(done, contributors);
		}
		for (UIEventSubscriber suscriber : getUIEventSubscriberList()) {
			suscriber.addContributors(done, contributors);
		}
		for (UIAppEvent appEvent : getUIAppEventList()) {
			appEvent.addContributors(done, contributors);
		}
		for (UIActionStack actionStack: getSharedActionList()) {
			actionStack.addContributors(done, contributors);
		}
	}
    
	private transient JSONObject computedContents = null;
	
	private JSONObject initJsonComputed() {
		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject()
						.put("scripts", 
								new JSONObject().put("imports", "")
												.put("declarations", "")
												.put("constructors", "")
												.put("functions", ""))
						.put("style", "")
						.put("theme", "")
						.put("route", "")
						.put("template", "");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonObject;
	}
	
	public synchronized void reset() {
		if (contributors != null) {
			contributors.clear();
		}
		contributors = null;
		computedContents = null;
	}
	
	public synchronized boolean isReset() {
		return contributors == null;
	}
	
	public synchronized JSONObject getComputedContents() {
		if (computedContents == null) {
			doComputeContents();
		}
		return computedContents;
	}
	
	protected void doComputeContents() {
		try {
			appImports.clear();
			appDeclarations.clear();
			appConstructors.clear();
			appFunctions.clear();
			appTemplates.clear();
			JSONObject newComputedContent = initJsonComputed();
			
			JSONObject jsonScripts = newComputedContent.getJSONObject("scripts");
			computeScripts(jsonScripts);
			
			newComputedContent.put("style", computeStyle());
			newComputedContent.put("theme", computeTheme());
			newComputedContent.put("template", computeTemplate());
			
			computedContents = newComputedContent;
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
    public void updateSourceFiles() throws EngineException {
    	if (_updateSourceFiles == null) {
    		_updateSourceFiles = () -> {
				if (isImporting) {
					return;
				}
				try {
					getProject().getMobileBuilder().appChanged();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
    		};
    	}
    	checkBatchOperation(_updateSourceFiles);
    }
	
	public void markPwaAsDirty() throws EngineException {
		updateSourceFiles();
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
		
		// compute app template
		String layout = getSplitPaneLayout();
		boolean hasSplitPane = !layout.equals("not set");
		if (hasSplitPane) {
			if (layout.isEmpty())
				sb.append("<ion-split-pane content-id=\"main-content\">").append(System.lineSeparator());
			else
				sb.append("<ion-split-pane when=\""+ layout +"\" content-id=\"main-content\">").append(System.lineSeparator());
		}
		
		Iterator<UIDynamicMenu> it = getMenuComponentList().iterator();
		while (it.hasNext()) {
			UIDynamicMenu menu = it.next();
			String menuTemplate = menu.computeTemplate();
			if (!menuTemplate.isEmpty()) {
				sb.append(menuTemplate).append(System.lineSeparator());
			}
		}
		
		sb.append("<ion-router-outlet id=\"main-content\"></ion-router-outlet>");
		sb.append(System.lineSeparator());
		
		if (hasSplitPane) {
			sb.append("</ion-split-pane>").append(System.lineSeparator());
		}
		
		// then add all necessary shared component templates
		String sharedTemplates = "";
		if (!appTemplates.isEmpty()) {
			sharedTemplates += "<!-- ====== SHARED TEMPLATES ====== -->"+ System.lineSeparator();
			for (String sharedTemplate: appTemplates.values()) {
				sharedTemplates += sharedTemplate;
			}
			sharedTemplates += "<!-- ============================== -->"+ System.lineSeparator();
			sharedTemplates += System.lineSeparator();
		}
		
		String computedTemplate = "";
		computedTemplate += "<ion-app>"+ System.lineSeparator();
		computedTemplate += sharedTemplates + sb.toString();
		computedTemplate += "</ion-app>"+ System.lineSeparator();
		return computedTemplate;
	}

	private String computeEventConstructors() {
		String computed = "";
		for (UIEventSubscriber subscriber: getUIEventSubscriberList()) {
			if (subscriber.isEnabled()) {
				computed += subscriber.computeConstructor();
			}
		}
		for (UIAppEvent event: getUIAppEventList()) {
			if (event.isEnabled() && event.isAvailable()) {
				computed += event.getAppEvent().computeConstructor(event.getFunctionName());
			}
		}
		return computed;
	}
	
	private String computeNgDestroy() {
		String computed = ""+ System.lineSeparator();
		computed += "\tngOnDestroy() {"+ System.lineSeparator();
		for (UIEventSubscriber subscriber: getUIEventSubscriberList()) {
			if (subscriber.isEnabled()) {
				computed += subscriber.computeDestructor();
			}
		}
		for (UIAppEvent event: getUIAppEventList()) {
			if (event.isEnabled() && event.isAvailable()) {
				computed += event.getAppEvent().computeDestructor();
			}
		}
		computed += "\t\tthis.subscriptions = {};"+ System.lineSeparator();
		computed += "\t\tsuper.ngOnDestroy();"+ System.lineSeparator();
		computed += "\t}"+ System.lineSeparator();
		computed += "\t";
		return computed;
	}
	
	@Override
	public void computeScripts(JSONObject jsonScripts) {
		// App menus
		Iterator<UIDynamicMenu> it = getMenuComponentList().iterator();
		while (it.hasNext()) {
			UIDynamicMenu menu = (UIDynamicMenu)it.next();
			menu.computeScripts(jsonScripts);
		}
		
		// App events
		if (compareToTplVersion("7.6.0.1") >= 0) {
			try {
				String constructors = jsonScripts.getString("constructors");
				String cname = "subscribers";
				String ccode = computeEventConstructors();
				if (addConstructor(cname, ccode)) {
					constructors += ccode;
				}
				jsonScripts.put("constructors", constructors);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			try {
				String functions = jsonScripts.getString("functions");
				String fname = "ngOnDestroy";
				String fcode = computeNgDestroy();
				if (addFunction(fname, fcode)) {
					functions += fcode;
				}
				jsonScripts.put("functions", functions);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			// Component typescripts
			for (UIEventSubscriber subscriber: getUIEventSubscriberList()) {
				subscriber.computeScripts(jsonScripts);
			}
			for (UIAppEvent event: getUIAppEventList()) {
				if (event.isEnabled() && event.isAvailable()) {
					event.computeScripts(jsonScripts);
				}
			}
		}
	}
	
	public String getComputedImports() {
		try {
			return getComputedContents().getJSONObject("scripts").getString("imports");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public String getComputedDeclarations() {
		try {
			return getComputedContents().getJSONObject("scripts").getString("declarations");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getComputedConstructors() {
		try {
			return getComputedContents().getJSONObject("scripts").getString("constructors");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public String getComputedFunctions() {
		try {
			return getComputedContents().getJSONObject("scripts").getString("functions");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "";
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
		
//		// Pages scss
//		for (PageComponent pc: getPageComponentList()) {
//			String pageName = pc.getName();
//			String pageDir = pageName.toLowerCase();
//			sb.append("@use \"./pages/"+pageDir+"/"+pageName.toLowerCase()+".scss\";").append(System.getProperty("line.separator"));
//		}
//		sb.append(System.lineSeparator());
		
		// App fonts
		StringBuilder sbFamilies = new StringBuilder();
		for (UIFont font: getUIFontList()) {
			String fontImport = font.computeStyle();
			if (!fontImport.isEmpty()) {
				sb.append(fontImport).append(System.getProperty("line.separator"));
			}
			if (font.isDefault()) {
				String fontFamily = font.getFontSource().getFontFamily();
				if (!fontFamily.isEmpty()) {
					sbFamilies.append(sbFamilies.length() > 0 ? ", ": "");
					sbFamilies.append("\""+ fontFamily +"\"");
				}
			}
		}
		if (sbFamilies.length() > 0) {
			sb.append(System.getProperty("line.separator"));
			sb.append("html {").append(System.getProperty("line.separator"));
			sb.append("\t--ion-font-family: ").append(sbFamilies).append(";").append(System.getProperty("line.separator"));
			sb.append("}").append(System.getProperty("line.separator"));
			sb.append(System.getProperty("line.separator"));
		}
		
		// App styles
		for (UIComponent component: getUIComponentList()) {
			if (component instanceof UIStyle && !(component instanceof UITheme) && !(component instanceof UIFont)) {
				String tpl = component.computeTemplate();
				if (!tpl.isEmpty()) {
					sb.append(tpl).append(System.getProperty("line.separator"));
				}
			}
		}
		
		// App menu styles
		for (UIDynamicMenu menu: getMenuComponentList()) {
			String menuStyle = menu.computeStyle();
			if (!menuStyle.isEmpty()) {
				sb.append(menuStyle).append(System.lineSeparator());
			}
		}
		
		return cleanStyle(sb.toString());
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
		return new File(Engine.projectDir(tplProjectName) + "/ionicTpl");
	}
	
	public String getTplProjectName() {
		return tplProjectName;
	}

	public void setTplProjectName(String tplProjectName) {
		this.tplProjectName = tplProjectName;
	}

	public String getTplProjectVersion() {
		if (tplProjectVersion == null) {
			try {
				//String tplVersion = getTplVersion();
				String tplVersion = null;
				Project p = getProject();
				if (p != null) {
					ComponentManager cm = ComponentManager.of(p);
					if (!cm.isInstance()) {
						tplVersion = cm.getTemplateProjectVersion();
						Engine.logBeans.trace("(getTplProjectVersion()) version "+ tplVersion +" retrieved from CM for component " + getQName());
					}
				}
				if (tplVersion == null) {
					tplVersion = super.getTplVersionFromBuilder();
					Engine.logBeans.trace("(getTplProjectVersion()) version "+ tplVersion +" retrieved from super MB for component " + getQName());
				}
				if (tplVersion != null) {
					tplProjectVersion = tplVersion;
				}
			} catch (NullPointerException e) {
				// ignore error for BeansDefaultValues
			}
		}
		return tplProjectVersion;
	}

	public void setTplProjectVersion(String tplProjectVersion) {
		// does nothing
	}
	
	public String getSplitPaneLayout() {
		return splitPaneLayout;
	}

	public void setSplitPaneLayout(String splitPaneLayout) {
		this.splitPaneLayout = splitPaneLayout;
	}

	public boolean isPWA() {
		return isPWA;
	}
	
	public void setPWA(boolean isPWA) {
		this.isPWA = isPWA;
	}
	
	public boolean getUseClickForTap() {
		return useClickForTap;
	}
	
	public void setUseClickForTap(boolean useClickForTap) {
		this.useClickForTap = useClickForTap;
	}
	
	private boolean isCompatibleTemplate(String project) {
		File tplDir = new File(Engine.projectDir(project) + "/ionicTpl");
		if (tplDir.exists()) {
			if (new File(tplDir,"angular.json").exists()) {
				try {
					File versionJson = new File(tplDir, "version.json");
					String tsContent = FileUtils.readFileToString(versionJson, "UTF-8");
					String tplVersion = new JSONObject(tsContent).getString("version");
					String appVersion = requiredTplVersion();
					return MobileBuilder.compareVersions(tplVersion, appVersion) >= 0;
				} catch (Exception e) {
				}
			}
		}
		return false;
	}
	
	@Override
	public String[] getTagsForProperty(String propertyName) {
		if ("tplProjectName".equals(propertyName)) {
			TreeSet<String> projects = new TreeSet<String>();
			projects.add(this.tplProjectName);
			
			for (String project: Engine.theApp.databaseObjectsManager.getAllProjectNamesList(false)) {
				if (isCompatibleTemplate(project)) {
					projects.add(project);
				};
			}
			
			return projects.descendingSet().toArray(new String[projects.size()]);
		}
		if (propertyName.equals("splitPaneLayout")) {
			return new String[] {"not set","xs","sm","md","lg","xl"};
		}
		return new String[0];
	}
	
	private void putAllInfos(Map<String, Set<String>> toMap, Map<String, Set<String>> fromMap) {
		if (toMap != null && fromMap != null) {
			for (String key: fromMap.keySet()) {
				Set<String> infos = toMap.get(key);
				if (infos == null) {
					infos = new HashSet<String>();
				}
				
				infos.addAll(fromMap.get(key));
				toMap.put(key, infos);
			}
		}
	}
	
	public Map<String, Set<String>> getInfoMap() {
		Map<String, Set<String>> map = new HashMap<String, Set<String>>();
		for (UIDynamicMenu menu: getMenuComponentList()) {
			putAllInfos(map, menu.getInfoMap());
		}
		for (UIEventSubscriber suscriber : getUIEventSubscriberList()) {
			putAllInfos(map, suscriber.getInfoMap());
		}
		for (UIAppEvent appEvent: getUIAppEventList()) {
			putAllInfos(map, appEvent.getInfoMap());
		}
		for (PageComponent page : getPageComponentList()) {
			putAllInfos(map, page.getInfoMap());
		}
		// do not add from shared : will be done by invoke shared action or use shared component
		return map;
	}
	
	@Override
	public void setParent(DatabaseObject databaseObject) {
		super.setParent(databaseObject);
		checkFolder();
	}
	
	public void checkFolder() {
		File templateFolder = null;
		File folder = new File(getProject().getDirPath() + "/Flashupdate");
		File index = new File(getParent().getResourceFolder(), "index.html");
		if (!folder.exists() || !index.exists()) {
			try {
				var tplDir = ComponentManager.of(this).getTemplateProjectDir();
				templateFolder = new File(tplDir, "ionicTpl/flashupdate/Flashupdate");
				if (!templateFolder.exists()) {
					templateFolder = null;
				}
			} catch (Exception e) {
			}
			if (templateFolder == null) {
				if (Engine.isCliMode()) {
					return;
				}
				templateFolder = new File(Engine.TEMPLATES_PATH, "base/Flashupdate");
			}
		}
		if (!folder.exists()) {
			try {
				FileUtils.copyDirectory(templateFolder, folder);
			} catch (Exception e) {
				Engine.logBeans.warn("(MobileApplication) The folder '" + folder.getAbsolutePath() + "' doesn't exist and cannot be created", e);
			}
		}
		if (!index.exists()) {
			try {
				index.getParentFile().mkdirs();
				File templateIndex = new File(templateFolder.getParentFile(), "index_mb.html");
				FileUtils.copyFile(templateIndex, index);
				
				File indexFu = new File(getParent().getResourceFolder(), "index-fu.html");
				if (!indexFu.exists()) {
					FileUtils.copyFile(templateIndex, indexFu);
				}
			} catch (Exception e) {
				Engine.logBeans.warn("(MobileApplication) The file '" + index.getAbsolutePath() + "' doesn't exist and cannot be created", e);
			}
		}
	}

	@Override
	public String requiredTplVersion(Set<MobileComponent> done) {
		// initialize with app component min version required
		String tplVersion = getRequiredTplVersion();
		
		if (done.add(this)) {
			minTplVersion = tplVersion;
			
			// overwrites with target child component min version required
			for (UIDynamicMenu menu : getMenuComponentList()) {
				String menuTplVersion = menu.requiredTplVersion(done);
				if (MobileBuilder.compareVersions(tplVersion, menuTplVersion) <= 0) {
					tplVersion = menuTplVersion;
				}
			}
			
			for (UIActionStack sa : getSharedActionList()) {
				String saTplVersion = sa.requiredTplVersion(done);
				if (MobileBuilder.compareVersions(tplVersion, saTplVersion) <= 0) {
					tplVersion = saTplVersion;
				}
			}
			
			for (UISharedComponent sc : getSharedComponentList()) {
				String scTplVersion = sc.requiredTplVersion(done);
				if (MobileBuilder.compareVersions(tplVersion, scTplVersion) <= 0) {
					tplVersion = scTplVersion;
				}
			}
			
			for (UIComponent uic : getUIComponentList()) {
				String uicTplVersion = uic.requiredTplVersion(done);
				if (uic instanceof UIEventSubscriber) {
					uicTplVersion = "7.6.0.1";
				}
				
				if (MobileBuilder.compareVersions(tplVersion, uicTplVersion) <= 0) {
					tplVersion = uicTplVersion;
				}
			}
			
			for (PageComponent page : getPageComponentList()) {
				String pageTplVersion = page.requiredTplVersion(done);
				if (MobileBuilder.compareVersions(tplVersion, pageTplVersion) <= 0) {
					tplVersion = pageTplVersion;
				}
			}
			
			minTplVersion = tplVersion;
		} else {
			tplVersion = minTplVersion;
		}
		
		return tplVersion;
	}
	
	public boolean needsMigration() {
		return containsDeprecated();
	}
	
	protected boolean containsDeprecated() {
		for (UIComponent uic : getUIComponentList()) {
			if (uic.containsDeprecated()) {
				return true;
			}
		}
		for (PageComponent page : getPageComponentList()) {
			if (page.containsDeprecated()) {
				return true;
			}
		}
		for (UIDynamicMenu menu : getMenuComponentList()) {
			if (menu.containsDeprecated()) {
				return true;
			}
		}
		for (UIActionStack sa : getSharedActionList()) {
			if (sa.containsDeprecated()) {
				return true;
			}
		}
		for (UISharedComponent sc : getSharedComponentList()) {
			if (sc.containsDeprecated()) {
				return true;
			}
		}
		return false;
	}

	public long getGenerationTime() {
		try {
			String str = FileUtils.readFileToString(new File(getProject().getDirFile(), "_private/ionic/src/env.json"), "UTF-8");
			long time = new JSONObject(str).getLong("appGenerationTime");
			return time;
		} catch (Exception e) {
		}
		return -1;
	}
	
	public long getBuiltGenerationTime() {
		try {
			String str = FileUtils.readFileToString(new File(getProject().getDirFile(), "DisplayObjects/mobile/env.json"), "UTF-8");
			long time = new JSONObject(str).getLong("appGenerationTime");
			return time;
		} catch (Exception e) {
		}
		return -1;
	}
	
	public boolean isBuiltUpToDate() {
		long gen = getGenerationTime();
		long built = getBuiltGenerationTime();
		return gen != -1 && gen == built;
	}

	@Override
	public String getUnbuiltMessage() {
		boolean isMobileUnbuilt = false;
		boolean isMobileUpToDate = true;
		long gen = getGenerationTime();
		long built = getBuiltGenerationTime();
		String msg = null;
		isMobileUnbuilt = built == -1;
		if (!isMobileUnbuilt) {
			isMobileUpToDate = gen == -1 || gen == built;
		}
		if (isMobileUnbuilt || !isMobileUpToDate) {
			msg = "The NGX Application isn't " + (isMobileUnbuilt ? "build yet" : "up-to-date") + ".\n"
				+ "You should built it before continuing:\n"
				+ "Open the mobile editor and run a watch or a production build from the sidebar.";
		}
		return msg;
	}

	@Override
	public boolean testAttribute(String name, String value) {
		if ("isApplicationProject".equals(name)) {
			return true;
		}
		return super.testAttribute(name, value);
	}
}
