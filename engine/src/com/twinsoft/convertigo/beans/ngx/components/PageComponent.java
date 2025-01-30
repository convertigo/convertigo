/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboFolderType;
import com.twinsoft.convertigo.beans.core.IContainerOrdered;
import com.twinsoft.convertigo.beans.core.IEnableAble;
import com.twinsoft.convertigo.beans.core.IPageComponent;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.ngx.components.UIPageEvent.ViewEvent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.util.EnumUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

@DboCategoryInfo(
		getCategoryId = "PageComponent",
		getCategoryName = "Page",
		getIconClassCSS = "convertigo-action-newPageComponent"
	)
@DboFolderType(type = FolderType.PAGE)
public class PageComponent extends MobileComponent implements IPageComponent, ITagsProperty, IScriptComponent, IStyleGenerator, ITemplateGenerator, IScriptGenerator, IContainerOrdered, IEnableAble {

	private static final long serialVersionUID = 188562781669238824L;
	
	public static final String SEGMENT_PREFIX = "path-to-";
	public static final String TITLE_PREFIX = "Title for ";
	
	transient private XMLVector<XMLVector<Long>> orderedComponents = new XMLVector<XMLVector<Long>>();
	
	public PageComponent() {
		super();
		
		this.priority = getNewOrderValue();
		
		orderedComponents = new XMLVector<XMLVector<Long>>();
		orderedComponents.add(new XMLVector<Long>());
	}

	@Override
	public PageComponent clone() throws CloneNotSupportedException {
		PageComponent cloned = (PageComponent) super.clone();
		cloned.vUIComponents = new LinkedList<UIComponent>();
		cloned.pageImports = new HashMap<String, String>();
		cloned.pageDeclarations = new HashMap<String, String>();
		cloned.pageConstructors = new HashMap<String, String>();
		cloned.pageFunctions = new HashMap<String, String>();
		cloned.pageTemplates = new HashMap<String, String>();
		cloned.computedContents = null;
		cloned.contributors = null;
		cloned.isRoot = isRoot;
		return cloned;
	}

	transient public boolean isRoot = false;
	
	@Override
	public Element toXml(Document document) throws EngineException {
		Element element = super.toXml(document);
		
		// Storing the page "isRoot" flag
		element.setAttribute("isRoot", Boolean.valueOf(isRoot).toString());
        
		return element;
	}
	
	@Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);
		
		try {
			long priority = Long.valueOf(element.getAttribute("priority")).longValue();
			if (priority == 0L) {
				priority = getNewOrderValue();
				element.setAttribute("priority", ""+priority);
			}
			
			NodeList properties = element.getElementsByTagName("property");
			
			// migration of scriptContent from String to FormatedContent
			Element propElement = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "scriptContent");
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
						Engine.logBeans.warn("(PageComponent) 'scriptContent' has been updated for the object \"" + getName() + "\"");
					}
				}
			}
		}
        catch(Exception e) {
            throw new EngineException("Unable to preconfigure the page component \"" + getName() + "\".", e);
        }
	}
	
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
		try {
			isRoot = Boolean.valueOf(element.getAttribute("isRoot")).booleanValue();
		} catch(Exception e) {
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
    
	public void insertAtOrder(DatabaseObject databaseObject, long priority) throws EngineException {
		increaseOrder(databaseObject, priority);
	}
    
    private void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
    	List<Long> ordered = null;
    	Long value = Long.valueOf(databaseObject.priority);
    	
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
    
    @Override
    public Object getOrderedValue() {
    	return priority;
    }
	
	/**
	 * The list of available page component for this application.
	 */
	transient private List<UIComponent> vUIComponents = new LinkedList<UIComponent>();
	
	protected void addUIComponent(UIComponent uiComponent, Long after) throws EngineException {
		checkSubLoaded();
		
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
	}

	public List<UIComponent> getUIComponentList() {
		checkSubLoaded();
		return sort(vUIComponents);
	}

	public List<UIPageEvent> getUIPageEventList() {
		Set<UIComponent> done = new HashSet<>();
		List<UIPageEvent> eventList = new ArrayList<>();
		for (UIComponent uiComponent : getUIComponentList()) {
			if (uiComponent.isEnabled()) {
				if (uiComponent instanceof UIPageEvent) {
					eventList.add((UIPageEvent) uiComponent);
				} else {
					uiComponent.addPageEvent(done, eventList);
				}
			}
		}
		return eventList;
	}

	public List<UIEventSubscriber> getUIEventSubscriberList() {
		List<UIEventSubscriber> eventList = new ArrayList<>();
		for (UIComponent uiComponent : getUIComponentList()) {
			if (uiComponent.isEnabled()) {
				if (uiComponent instanceof UIEventSubscriber) {
					eventList.add((UIEventSubscriber) uiComponent);
				}
			}
		}
		return eventList;
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
	public void add(DatabaseObject databaseObject, Long after) throws EngineException {
		if (databaseObject instanceof UIComponent) {
			addUIComponent((UIComponent) databaseObject, after);
		} else {
			throw new EngineException("You cannot add to a page component a database object of type " + databaseObject.getClass().getName());
		}		
	}
	
	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		add(databaseObject, null);
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

	private String title = "";
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	private String icon = "";
	
	public String getIcon() {
		return icon;
	}
	
	public void setIcon(String icon) {
		this.icon = icon;
	}
	
	private String iconPosition = "";
	
	public String getIconPosition() {
		return iconPosition;
	}

	public void setIconPosition(String iconPosition) {
		this.iconPosition = iconPosition;
	}
	
	private boolean inAutoMenu = true;
	
	public boolean isInAutoMenu() {
		return inAutoMenu;
	}

	public void setInAutoMenu(boolean inAutoMenu) {
		this.inAutoMenu = inAutoMenu;
	}

	private String startMenu = "";
	
	public String getStartMenu() {
		return startMenu;
	}

	public void setStartMenu(String menu) {
		this.startMenu = menu;
	}
	
	private String endMenu = "";
	
	public String getEndMenu() {
		return endMenu;
	}

	public void setEndMenu(String menu) {
		this.endMenu = menu;
	}
	
	protected String getStartMenuId() {
		String menuId = getApplication().getDefaultStartMenuId();
		if (!startMenu.isEmpty() && startMenu.startsWith(getProject().getName())) {
			try {
				menuId = startMenu.substring(startMenu.lastIndexOf('.')+1);
			} catch (IndexOutOfBoundsException e) {}
		}
		return menuId;
	}
	
	protected String getEndMenuId() {
		String menuId = getApplication().getDefaultEndMenuId();
		if (!endMenu.isEmpty() && endMenu.startsWith(getProject().getName())) {
			try {
				menuId = endMenu.substring(endMenu.lastIndexOf('.')+1);
			} catch (IndexOutOfBoundsException e) {}
		}
		return menuId;
	}
	
	protected FormatedContent scriptContent = new FormatedContent("");

	public FormatedContent getScriptContent() {
		return scriptContent;
	}

	public void setScriptContent(FormatedContent scriptContent) {
		this.scriptContent = scriptContent;
	}
	
	private String preloadPriority = "low";
	
	public String getPreloadPriority() {
		return preloadPriority;
	}

	public void setPreloadPriority(String preloadPriority) {
		this.preloadPriority = preloadPriority;
	}

	private String defaultHistory = "[]";
	
	public String getDefaultHistory() {
		return defaultHistory;
	}

	public void setDefaultHistory(String defaultHistory) {
		this.defaultHistory = defaultHistory;
	}
	
	private ChangeDetection changeDetection = ChangeDetection.Default;
	
	public ChangeDetection getChangeDetection() {
		return changeDetection;
	}

	public void setChangeDetection(ChangeDetection changeDetection) {
		this.changeDetection = changeDetection;
	}
	
	public String getChangeDetectionStrategy() {
		return getChangeDetection().strategy();
	}
	
	private transient Map<String, String> pageImports = new HashMap<String, String>();
	
	private boolean hasImport(String name) {
		return pageImports.containsKey(name) ||
				getProject().getMobileBuilder().hasTplPageTsImport(name);
	}
	
	private boolean hasCustomImport(String name) {
		synchronized (scriptContent) {
			String c8o_UserCustoms = scriptContent.getString();
			String importMarker = MobileBuilder.getMarker(c8o_UserCustoms, "PageImport");
			Map<String, String> map = new HashMap<String, String>(10);
			MobileBuilder.initMapImports(map, importMarker);
			return map.containsKey(name);
		}
	}
	
	@Override
	public boolean addImport(String name, String path) {
		if (name != null && path != null && !name.isEmpty() && !path.isEmpty()) {
			synchronized (pageImports) {
				if (!hasImport(name) && !hasCustomImport(name)) {
					pageImports.put(name, path);
					return true;
				}
			}
		}
		return false;
	}
	
	private transient Map<String, String> pageFunctions = new HashMap<String, String>();
	
	private boolean hasFunction(String name) {
		return pageFunctions.containsKey(name);
	}
	
	@Override
	public boolean addFunction(String name, String code) {
		if (name != null && code != null && !name.isEmpty() && !code.isEmpty()) {
			synchronized (pageFunctions) {
				if (!hasFunction(name)) {
					pageFunctions.put(name, code);
					return true;
				}
			}
		}
		return false;
	}
	
	private transient Map<String, String> pageDeclarations = new HashMap<String, String>();
	
	private boolean hasDeclaration(String name) {
		return pageDeclarations.containsKey(name);
	}
	
	@Override
	public boolean addDeclaration(String name, String code) {
		if (name != null && code != null && !name.isEmpty() && !code.isEmpty()) {
			synchronized (pageDeclarations) {
				if (!hasDeclaration(name)) {
					pageDeclarations.put(name, code);
					return true;
				}
			}
		}
		return false;
	}
	
	private transient Map<String, String> pageConstructors = new HashMap<String, String>();
	
	private boolean hasConstructor(String name) {
		return pageConstructors.containsKey(name);
	}
	
	@Override
	public boolean addConstructor(String name, String code) {
		if (name != null && code != null && !name.isEmpty() && !code.isEmpty()) {
			synchronized (pageConstructors) {
				if (!hasConstructor(name)) {
					pageConstructors.put(name, code);
					return true;
				}
			}
		}
		return false;
	}
	
	private transient Map<String, String> pageTemplates = new HashMap<String, String>();
	
	private boolean hasTemplate(String name) {
		return pageTemplates.containsKey(name);
	}
	
	@Override
	public boolean addTemplate(String name, String code) {
		if (name != null && code != null && !name.isEmpty() && !code.isEmpty()) {
			synchronized (pageTemplates) {
				if (!hasTemplate(name)) {
					pageTemplates.put(name, code);
					return true;
				}
			}
		}
		return false;
	}
	
	protected Map<String, Set<String>> getInfoMap() {
		Set<UIComponent> done = new HashSet<>();
		Map<String, Set<String>> map = new HashMap<String, Set<String>>();
		for (UIComponent uiComponent : getUIComponentList()) {
			uiComponent.addInfos(done, map);
		}
		return map;
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
		for (UIComponent uiComponent : getUIComponentList()) {
			uiComponent.addContributors(done, contributors);
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
						.put("template", "")
						.put("style", "");
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
			pageImports.clear();
			pageDeclarations.clear();
			pageConstructors.clear();
			pageFunctions.clear();
			pageTemplates.clear();
			JSONObject newComputedContent = initJsonComputed();
			
			JSONObject jsonScripts = newComputedContent.getJSONObject("scripts");
			computeScripts(jsonScripts);
			
			newComputedContent.put("style", computeStyle());
			newComputedContent.put("template", computeTemplate());
			
			computedContents = newComputedContent;
			
		} catch (JSONException e) {
			e.printStackTrace();
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

	private String computeEventConstructors(List<UIEventSubscriber> subscriberList) {
		String computed = "";
		if (!subscriberList.isEmpty()) {
			for (UIEventSubscriber subscriber: subscriberList) {
				String constructor = subscriber.computeConstructor();
				computed += constructor.isEmpty() ? "": constructor;
			}
			computed += "\t\t";
		}
		return computed;
	}
	
	private boolean hasEvent(ViewEvent viewEvent) {
		for (UIPageEvent pageEvent: getUIPageEventList()) {
			if (pageEvent.getViewEvent().equals(viewEvent)) {
				return true;
			}
		}
		return false;
	}
	
	private String computeNgInit() {
		String computed = "";
		computed += "\tngOnInit() {"+ System.lineSeparator();
		if (hasEvent(ViewEvent.onWillLoad)) {
			computed += "\t\tthis."+ ViewEvent.onWillLoad.event + "()" + System.lineSeparator();
		}
		computed += "\t}"+ System.lineSeparator();
		computed += "\t";
		return computed;
	}
	
	private String computeNgAfterViewInit() {
		String computed = "";
		computed += "ngAfterViewInit() {"+ System.lineSeparator();
		if (hasEvent(ViewEvent.onDidLoad)) {
			computed += "\t\tthis."+ ViewEvent.onDidLoad.event + "()" + System.lineSeparator();
		}
		computed += "\t}"+ System.lineSeparator();
		computed += "\t";
		return computed;
	}
	
	private String computeNgDestroy() {
		String computed = "";
		computed += "ngOnDestroy() {"+ System.lineSeparator();
		if (hasEvent(ViewEvent.onWillUnload)) {
			computed += "\t\tthis."+ ViewEvent.onWillUnload.event + "()" + System.lineSeparator();
		}
		List<UIEventSubscriber> subscriberList = getUIEventSubscriberList();
		if (!subscriberList.isEmpty()) {
			for (UIEventSubscriber subscriber: subscriberList) {
				String desctructor = subscriber.computeDestructor();
				computed += desctructor.isEmpty() ? "" : desctructor;
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
		// Page menus
		String startMenuId = getStartMenuId();
		if (!startMenuId.isEmpty()) {
			try {
				String constructors = jsonScripts.getString("constructors");
				String cname = "startMenuId";
				String ccode = System.lineSeparator() + "\t\tthis.startMenuId = '" + startMenuId +"';" + System.lineSeparator() + "\t\t";
				if (addConstructor(cname, ccode)) {
					constructors += ccode;
				}
				jsonScripts.put("constructors", constructors);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		String endMenuId = getEndMenuId();
		if (!endMenuId.isEmpty()) {
			try {
				String constructors = jsonScripts.getString("constructors");
				String cname = "endMenuId";
				String ccode = System.lineSeparator() + "\t\tthis.endMenuId = '" + endMenuId +"';" + System.lineSeparator() + "\t\t";
				if (addConstructor(cname, ccode)) {
					constructors += ccode;
				}
				jsonScripts.put("constructors", constructors);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		// Page subscribers
		List<UIEventSubscriber> subscriberList = getUIEventSubscriberList();
		if (!subscriberList.isEmpty()) {
			try {
				String declarations = jsonScripts.getString("declarations");
				String dname = "nbInstance";
				String dcode = "public static nbInstance = 0;";
				if (addDeclaration(dname, dcode)) {
					declarations += dcode + System.lineSeparator();
				}
				jsonScripts.put("declarations", declarations);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			
			try {
				String constructors = jsonScripts.getString("constructors");
				String cname = "subscribers";
				String ccode = computeEventConstructors(subscriberList);
				if (addConstructor(cname, ccode)) {
					constructors += ccode + (ccode.isEmpty() ? "" : System.lineSeparator() + "\t\t");
				}
				jsonScripts.put("constructors", constructors);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		// Page events
		List<UIPageEvent> eventList = getUIPageEventList();
		for (ViewEvent viewEvent: ViewEvent.values()) {
			String computedEvent = viewEvent.computeEvent(this, eventList);
			if (!computedEvent.isEmpty()) {
				try {
					String functions = jsonScripts.getString("functions");
					String fname = viewEvent.name();
					String fcode = computedEvent;
					if (addFunction(fname, fcode)) {
						functions += fcode + System.lineSeparator();
					}
					jsonScripts.put("functions", functions);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		
		// ngOnInit, ngAfterViewInit, ngOnDestroy
		try {
			String functions = jsonScripts.getString("functions");
			String fname = "ngOnInit";
			String fcode = computeNgInit();
			if (addFunction(fname, fcode)) {
				functions += fcode + (fcode.isEmpty() ? "" : System.lineSeparator() + "\t");
			}
			jsonScripts.put("functions", functions);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		try {
			String functions = jsonScripts.getString("functions");
			String fname = "ngAfterViewInit";
			String fcode = computeNgAfterViewInit();
			if (addFunction(fname, fcode)) {
				functions += fcode + (fcode.isEmpty() ? "" : System.lineSeparator() + "\t");
			}
			jsonScripts.put("functions", functions);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		try {
			String functions = jsonScripts.getString("functions");
			String fname = "ngOnDestroy";
			String fcode = computeNgDestroy();
			if (addFunction(fname, fcode)) {
				functions += fcode + (fcode.isEmpty() ? "" : System.lineSeparator() + "\t");
			}
			jsonScripts.put("functions", functions);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		// Component typescripts
		if (isEnabled()) {
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				component.computeScripts(jsonScripts);
			}
		}
	}
	
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
		if (!isEnabled()) {
			return "";
		}
		
		// compute page template
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
		StringBuilder styles = new StringBuilder();
		StringBuilder others = new StringBuilder();
		
		for (UIComponent component: getUIComponentList()) {
			if (component instanceof UIStyle) {
				String tpl = component.computeTemplate();
				if (!tpl.isEmpty()) {
					styles.append(tpl).append(System.getProperty("line.separator"));
				}
			} else if (component instanceof UIUseShared) {
				String tpl = ((UIUseShared)component).computeStyle();
				if (!tpl.isEmpty()) {
					others.append(tpl);
				}
			} else if (component instanceof UIElement) {
				String tpl = ((UIElement)component).computeStyle();
				if (!tpl.isEmpty()) {
					others.append(tpl).append(System.getProperty("line.separator"));
				}
			}
		}
		
		StringBuilder sb = new StringBuilder();
		if (others.length() > 0) {
			sb.append(others).append(System.getProperty("line.separator"));
		}
		if (styles.length() > 0) {
			sb.append(styles).append(System.getProperty("line.separator"));
		}
		return cleanStyle(sb.toString());
	}
	
	@Override
	public boolean testAttribute(String name, String value) {
		if (name.equals("isRoot")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isRoot));
		}
		if (name.equals("isEnabled")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isEnabled()));
		}
		return super.testAttribute(name, value);
	}

	private boolean isEnabled = true;
	
	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Override
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	public boolean updateSmartSources(String oldString, String newString) {
		boolean updated = false;
		for (UIComponent uic : getUIComponentList()) {
			if (uic.updateSmartSources(oldString, newString)) {
				updated = true;
			}
		}
		return updated;
	}
	
	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("icon")) {
			String[] icons = EnumUtils.toStrings(IonIcon.class);
			List<String> iconList = Arrays.asList(icons).stream()
									.filter(icon -> !icon.endsWith("-outline") && !icon.endsWith("-sharp"))
									.collect(Collectors.toList());
			String[] arr = new String[iconList.size()];
			return iconList.toArray(arr);
		}
		if (propertyName.equals("iconPosition")) {
			return new String[] {"start","end"};
		}
		if (propertyName.equals("preloadPriority")) {
			return new String[] {"high","low","off"};
		}
		if (propertyName.equals("changeDetection")) {
			return EnumUtils.toNames(ChangeDetection.class);
		}
		return new String[0];
	}
	
	@Override
	public String requiredTplVersion(Set<MobileComponent> done) {
		// initialize with page component min version required
		String tplVersion = getRequiredTplVersion();
		if (done.add(this)) {
			minTplVersion = tplVersion;
			// overwrites with target child component min version required
			for (UIComponent uic : getUIComponentList()) {
				String uicTplVersion = uic.requiredTplVersion(done);
				if (MobileBuilder.compareVersions(tplVersion, uicTplVersion) <= 0) {
					tplVersion = uicTplVersion;
				}
			}
			minTplVersion = tplVersion;
		} else {
			tplVersion = minTplVersion;
		}
		return tplVersion;
	}

	protected boolean containsDeprecated() {
		for (UIComponent uic : getUIComponentList()) {
			if (uic.containsDeprecated()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected void onBeanNameChanged(String oldName, String newName) {
		if ((TITLE_PREFIX + oldName).equals(title)) {
			title = TITLE_PREFIX + newName;
			hasChanged = true;
		}
		if ((SEGMENT_PREFIX + oldName.toLowerCase()).equals(segment)) {
			segment = SEGMENT_PREFIX + newName.toLowerCase();
			hasChanged = true;
		}
	}
}
