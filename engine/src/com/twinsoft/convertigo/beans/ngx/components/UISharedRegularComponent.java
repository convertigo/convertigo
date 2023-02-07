/*
 * Copyright (c) 2001-2022 Convertigo SA.
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.ISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIPageEvent.ViewEvent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponentEvent.ComponentEvent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class UISharedRegularComponent extends UISharedComponent implements ISharedComponent, IScriptComponent, IStyleGenerator {

	private static final long serialVersionUID = -2506259541566203802L;

	transient private Runnable _markCompAsDirty;
	
	public UISharedRegularComponent() {
		super(true);
	}
	
	@Override
	public void setName(String name) throws EngineException {
		if (StringUtils.normalize(name).startsWith("_")) {
			throw new EngineException("Name must begin with a letter");
		}
		super.setName(name);
	}
	
	public String getIconFileName() {
		return getIconFileName(getName());
	}
	
	public String getIconFileName(String name) {
		return (name + "_icon_32x32.png").toLowerCase();
	}
	
	@Override
	public String getIdentifier() {
		return "comp"+ priority;
	}

	public String getNsIdentifier() {
		return UISharedComponent.getNsCompIdentifier(this);
	}
	
	@Override
	public String getSelector() {
		return UISharedComponent.getNsCompFileName(this);
	}
	
	@Override
	public List<UICompEvent> getUICompEventList() {
		List<UICompEvent> compEventList = new ArrayList<UICompEvent>();
		for (UIComponent uic: getUIComponentList()) {
			if (uic instanceof UICompEvent) {
				compEventList.add((UICompEvent)uic);
			}
		}
		return compEventList;
	}
	
	@Override
	public UISharedRegularComponent clone() throws CloneNotSupportedException {
		UISharedRegularComponent cloned = (UISharedRegularComponent) super.clone();
		cloned.pageImports = new HashMap<String, String>();
		cloned.pageDeclarations = new HashMap<String, String>();
		cloned.pageConstructors = new HashMap<String, String>();
		cloned.pageFunctions = new HashMap<String, String>();
		cloned.pageTemplates = new HashMap<String, String>();
		cloned.computedContents = null;
		cloned.contributors = null;
		cloned.contributorsShot = null;
		return cloned;
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
	
	private transient String contributorsShot = null;
	
	private Object lock1 = new Object();
	
	@Override
	public List<Contributor> getContributors() {
		if (contributors == null) {
			synchronized (lock1) {
				doGetContributors();
			}
		}
		return contributors;
	}
	
//	protected synchronized void doGetContributors() {
	protected void doGetContributors() {
		if (contributors != null) return;
		
		contributors = new ArrayList<>();
		Set<UIComponent> done = new HashSet<>();
		for (UIComponent uiComponent : getUIComponentList()) {
			uiComponent.addContributors(done, contributors);
		}
			
		//contributorsShot = contributors.toString();
	}
	
	private transient JSONObject computedContents = null;
	
	private JSONObject initJsonComputed() {
		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject()
						.put("scripts", 
								new JSONObject().put("imports", "")
												.put("interfaces", "")
												.put("declarations", "")
												.put("constructors", "")
												.put("initializations", "")
												.put("dispositions", "")
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
		contributorsShot = null;
	}
	
	private Object lock2 = new Object();
	
	public JSONObject getComputedContents() {
		if (computedContents == null) {
			synchronized (lock2) {
				doComputeContents();
			}
		}
		return computedContents;
	}
	
	@Override
	protected void doComputeContents() {
		try {
			if (computedContents != null) return;
			
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
	
	@Override
	public String getComputedImports() {
		try {
			return getComputedContents().getJSONObject("scripts").getString("imports");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	@Override
	public String getComputedInterfaces() {
		try {
			return getComputedContents().getJSONObject("scripts").getString("interfaces");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	@Override
	public String getComputedDeclarations() {
		try {
			return getComputedContents().getJSONObject("scripts").getString("declarations");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public String getComputedConstructors() {
		try {
			return getComputedContents().getJSONObject("scripts").getString("constructors");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	@Override
	public String getComputedInitializations() {
		try {
			return getComputedContents().getJSONObject("scripts").getString("initializations");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	@Override
	public String getComputedDispositions() {
		try {
			return getComputedContents().getJSONObject("scripts").getString("dispositions");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public String getComputedFunctions() {
		try {
			return getComputedContents().getJSONObject("scripts").getString("functions");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	@Override
	public void computeScripts(JSONObject jsonScripts) {
		Iterator<UIComponent> it;
		
		String events = "";
		String params = "";
		
		it = getUIComponentList().iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (component instanceof UICompVariable) {
				UICompVariable uicv = (UICompVariable)component;
				if (uicv.isEnabled()) {
					String varName = uicv.getVariableName();
					String varValue = uicv.getVariableValue();
					if (uicv.isAutoEmit()) {
						params += "\tpublic _"+ varName + ": any = " + (varValue.isEmpty() ? "undefined":varValue) + System.lineSeparator();
						params += "\t@Input() public get "+ varName + "() {"+ System.lineSeparator();
						params += "\t\treturn this._"+ varName + ";" + System.lineSeparator();
						params += "\t}"+ System.lineSeparator();
						params += "\tpublic set "+ varName + "(val: any) {"+ System.lineSeparator();
						params += "\t\tif (val !== undefined && this._"+ varName +" !== val) {" + System.lineSeparator();
						params += "\t\t\tthis._"+ varName + " = val;" + System.lineSeparator();
						params += "\t\t\tthis."+ varName + "Change.emit(val);" + System.lineSeparator();
						params += "\t\t}" + System.lineSeparator();
						params += "\t}"+ System.lineSeparator();
					} else {
						params += "\t@Input() "+ varName + ": any = " + (varValue.isEmpty() ? "undefined":varValue) + System.lineSeparator();
					}
					events += "\t@Output() "+ varName +"Change = new EventEmitter<any>();"+ System.lineSeparator();
				}
			}
			if (component instanceof UICompEvent) {
				UICompEvent uice = (UICompEvent)component;
				if (uice.isEnabled()) {
					String eventName = uice.getAttrName();
					if (!eventName.isBlank()) {
						if (events.indexOf("@Output() "+ eventName +" =") == -1) {
							events += "\t@Output() "+ eventName +" = new EventEmitter<any>();"+ System.lineSeparator();
						}
					}
				}
			}
		}
		try {
			String declarations = jsonScripts.getString("declarations");
			declarations += params;
			declarations += events;
			jsonScripts.put("declarations", declarations);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		// Component events
		List<UISharedComponentEvent> compEventList = getUISharedComponentEventList();
		for (ComponentEvent componentEvent: ComponentEvent.values()) {
			String computedEvent = componentEvent.computeEvent(this, compEventList);
			if (!computedEvent.isEmpty()) {
				try {
					String functions = jsonScripts.getString("functions");
					String fname = componentEvent.name();
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
		
		// Page events
		List<UIPageEvent> pageEventList = getUIPageEventList();
		for (ViewEvent viewEvent: ViewEvent.values()) {
			String computedEvent = viewEvent.computeEvent(this, pageEventList);
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
		
		if (isEnabled()) {
			// Comp subscribers
			List<UIEventSubscriber> subscriberList = getUIEventSubscriberList();
			if (!subscriberList.isEmpty()) {
				try {
					String initializations = jsonScripts.getString("initializations");
					String ccode = computeEventConstructors(subscriberList);
					initializations += ccode + (ccode.isEmpty() ? "" : System.lineSeparator() + "\t\t");
					jsonScripts.put("initializations", initializations);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				try {
					String dispositions = jsonScripts.getString("dispositions");
					String ccode = computeEventDestructors(subscriberList);
					dispositions += ccode + (ccode.isEmpty() ? "" : System.lineSeparator() + "\t\t");
					jsonScripts.put("dispositions", dispositions);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				component.computeScripts(jsonScripts);
			}
		}
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

	private String computeEventDestructors(List<UIEventSubscriber> subscriberList) {
		String computed = "";
		if (!subscriberList.isEmpty()) {
			for (UIEventSubscriber subscriber: subscriberList) {
				String desctructor = subscriber.computeDestructor();
				computed += desctructor.isEmpty() ? "" : desctructor;
			}
			computed += "\t\t";
		}
		return computed;
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
	
	public List<UISharedComponentEvent> getUISharedComponentEventList() {
		List<UISharedComponentEvent> eventList = new ArrayList<>();
		for (UIComponent uiComponent : getUIComponentList()) {
			if (uiComponent.isEnabled()) {
				if (uiComponent instanceof UISharedComponentEvent) {
					eventList.add((UISharedComponentEvent) uiComponent);
				}
			}
		}
		return eventList;
	}
	
	@Override
	protected void addInfos(UIUseShared uiUse, Set<UIComponent> done, Map<String, Set<String>> infoMap) {
		if (!done.add(this)) {
			return;
		}
		for (UIComponent uiComponent : getUIComponentList()) {
			uiComponent.addInfos(done, infoMap);
		}
	}
	
	@Override
	public void addPageEvent(UIUseShared uiUse, Set<UIComponent> done, List<UIPageEvent> eventList) {
		if (!done.add(this)) {
			return;
		}
		List<UIPageEvent> list = new ArrayList<UIPageEvent>();
		for (UIComponent uic : getUIComponentList()) {
			try {
				if (uic instanceof UIPageEvent && uic.isEnabled()) {
					list.add((UIPageEvent)uic);
				}
			} catch (Exception e) {
				Engine.logBeans.warn("(UISharedComponent) addPageEvent: enabled to add \""+ uic.getName() +"\" component for \""+ uiUse.toString() +"\" component");
			}
		}
		
		for (ViewEvent viewEvent: ViewEvent.values()) {
			boolean found = false;
			for (UIPageEvent evt: list) {
				if (viewEvent.equals(evt.getViewEvent())) {
					found = true;
					break;
				}
			}
			if (!found) {
				UIPageEvent upe = new UIPageEvent();
				upe.setViewEvent(viewEvent);
				upe.setChildOf(this);
				list.add(upe);
			}
		}
		
		eventList.addAll(list);
	}

	@Override
	public void addEventSubscriber(UIUseShared uiUse, Set<UIComponent> done, List<UIEventSubscriber> eventList) {
		if (!done.add(this)) {
			return;
		}
		for (UIComponent uic : getUIComponentList()) {
			try {
				if (uic instanceof UIEventSubscriber && uic.isEnabled()) {
					eventList.add((UIEventSubscriber)uic);
				}
			} catch (Exception e) {
				Engine.logBeans.warn("(UISharedComponent) addEventSubscriber: enabled to add \""+ uic.getName() +"\" component for \""+ uiUse.toString() +"\" component");
			}
		}
	}
	
	@Override
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

	@Override
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
		if (!isEnabled()) {
			return "";
		}
		
		StringBuilder families = new StringBuilder();
		StringBuilder styles = new StringBuilder();
		StringBuilder others = new StringBuilder();
		
		for (UIComponent component: getUIComponentList()) {
			if (component instanceof UIFont) {
				UIFont font = (UIFont)component;
				String fontImport = font.computeStyle();
				if (!fontImport.isEmpty()) {
					styles.append(fontImport).append(System.getProperty("line.separator"));
				}
				if (font.isDefault()) {
					String fontFamily = font.getFontSource().getFontFamily();
					if (!fontFamily.isEmpty()) {
						families.append(families.length() > 0 ? ", ": "");
						families.append("\""+ fontFamily +"\"");
					}
				}
			}
			else if (component instanceof UIStyle) {
				String tpl = component.computeTemplate();
				if (!tpl.isEmpty()) {
					styles.append(tpl).append(System.getProperty("line.separator"));
				}
			}
			else if (component instanceof UIUseShared) {
				String tpl = ((UIUseShared)component).computeStyle();
				if (!tpl.isEmpty()) {
					others.append(tpl);
				}
			}
			else if (component instanceof UIElement) {
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
		if (families.length() > 0) {
			sb.append(System.getProperty("line.separator"));
			sb.append(getSelector() +" {").append(System.getProperty("line.separator"));
			sb.append("\tfont-family: ").append(families).append(";").append(System.getProperty("line.separator"));
			sb.append("}").append(System.getProperty("line.separator"));
			sb.append(System.getProperty("line.separator"));
		}
		if (styles.length() > 0) {
			sb.append(styles).append(System.getProperty("line.separator"));
		}
		return cleanStyle(sb.toString());
	}
	
	@Override
	protected String computeStyle(UIUseShared uiUse) {
		String c8o_CompScssPath;
		
		//if (!isEnabled()) return "";
		
		MobileComponent container = (MobileComponent) uiUse.getMainScriptComponent();
		try {
			Path scssPath = Paths.get(new File (container.getProject().getDirFile(), UISharedComponent.getNsCompDirPath(this)
									+ "/" +UISharedComponent.getNsCompFileName(this) + ".scss").getCanonicalPath());
			c8o_CompScssPath = getContributor().getContainerPath(container).relativize(scssPath).toString().replace('\\', '/');
		} catch (Exception e) {
			c8o_CompScssPath = "../components/"+ UISharedComponent.getNsCompDirName(this) + "/" +UISharedComponent.getNsCompFileName(this) + ".scss";
		}
		
		return "@use \""+ c8o_CompScssPath + "\";" + System.lineSeparator();
	}

	@Override
	protected void addContributors(Set<UIComponent> done, List<Contributor> contributors) {
		if (!done.add(this)) {
			return;
		}
		
		if (!isEnabled()) return;
		
		Contributor contributor = getContributor();
		if (contributor != null) {
			if (!contributors.contains(contributor)) {
				contributors.add(contributor);
			}
		}
		for (UIComponent uic : getUIComponentList()) {
			uic.addContributors(done, contributors);
		}
	}

	@Override
	protected void addContributors(UIUseShared uiUse, Set<UIComponent> done, List<Contributor> contributors) {
		if (!done.add(this)) {
			return;
		}
		
		//if (!isEnabled()) return;
		
		Contributor contributor = getContributor(uiUse);
		if (contributor != null) {
			if (!contributors.contains(contributor)) {
				contributors.add(contributor);
			}
		}
		for (UIComponent uic : getUIComponentList()) {
			uic.addContributors(done, contributors);
		}
	}
	
	@Override
	protected Contributor getContributor() {
		return getContributor(null);
	}
	
	protected Contributor getContributor(UIUseShared uiUse) {
		final String c8o_CompModuleName = UISharedComponent.getNsCompName(UISharedRegularComponent.this) + "Module";
		final UIUseShared use = uiUse;
		
		//if (!isEnabled()) return null;
		
		return new Contributor() {
			
			private boolean accept() {
				if (getContainer() == null) {
					return true;
				} else if (use != null) {
					return ((MobileComponent)use.getMainScriptComponent()).equals(getContainer());
				}
				return false;
			}
			
			@Override
			public Map<String, String> getActionTsFunctions() {
				return new HashMap<String, String>();
			}

			@Override
			public Map<String, String> getActionTsImports() {
				return new HashMap<String, String>();
			}

			@Override
			public Map<String, File> getCompBeanDir() {
				return new HashMap<String, File>();
			}
			
			@Override
			public Map<String, String> getModuleTsImports() {
				Map<String, String> imports = new HashMap<String, String>();
				
				if (accept()) {
					MobileComponent container = getContainer();
					String c8o_CompModulePath;
					try {
						Path modulePath = Paths.get(new File (container.getProject().getDirFile(), UISharedComponent.getNsCompDirPath(UISharedRegularComponent.this)
												+ "/" + UISharedComponent.getNsCompFileName(UISharedRegularComponent.this) + ".module").getCanonicalPath());
						c8o_CompModulePath = getContainerPath(container).relativize(modulePath).toString().replace('\\', '/');
					} catch (Exception e) {
						c8o_CompModulePath = "../components/"+ UISharedComponent.getNsCompDirName(UISharedRegularComponent.this) + "/" + UISharedComponent.getNsCompFileName(UISharedRegularComponent.this) + ".module";
					}
					imports.put(c8o_CompModuleName, c8o_CompModulePath);
				}
				return imports;
			}

			@Override
			public Set<String> getModuleNgImports() {
				Set<String> ngImports = new HashSet<String>();
				if (accept()) {
					ngImports.add(c8o_CompModuleName);
				}
				return ngImports;
			}

			@Override
			public Set<String> getModuleNgProviders() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgDeclarations() {
				return new HashSet<String>();
			}
			
			@Override
			public Set<String> getModuleNgComponents() {
				return new HashSet<String>();
			}

			@Override
			public Map<String, String> getPackageDependencies() {
				return new HashMap<String, String>();
			}

			@Override
			public Map<String, String> getConfigPlugins() {
				return new HashMap<String, String>();
			}

			@Override
			public Set<String> getModuleNgRoutes(String pageSegment) {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getBuildAssets() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getBuildScripts() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getBuildStyles() {
				return new HashSet<String>();
			}
		};
	}
	
//	public void markCompTsAsDirty() throws EngineException {
//		getProject().getMobileBuilder().compTsChanged(this, false);
//	}
	
//	@Override
//	public void markCompAsDirty() throws EngineException {
//		if (_markCompAsDirty == null) {
//			_markCompAsDirty = () -> {
//				if (isImporting) {
//					return;
//				}
//				try {
//					JSONObject oldComputedContent = computedContents == null ? 
//							null :new JSONObject(computedContents.toString());
//					
//					doComputeContents();
//					
//					JSONObject newComputedContent = computedContents == null ? 
//							null :new JSONObject(computedContents.toString());
//					
//					if (oldComputedContent != null && newComputedContent != null) {
//						if (!(newComputedContent.getJSONObject("scripts").toString()
//								.equals(oldComputedContent.getJSONObject("scripts").toString()))) {
//							getProject().getMobileBuilder().compTsChanged(this, true);
//						}
//					}
//					if (oldComputedContent != null && newComputedContent != null) {
//						if (!(newComputedContent.getString("style")
//								.equals(oldComputedContent.getString("style")))) {
//							getProject().getMobileBuilder().compStyleChanged(this);
//						}
//					}
//					if (oldComputedContent != null && newComputedContent != null) {
//						if (!(newComputedContent.getString("template")
//								.equals(oldComputedContent.getString("template")))) {
//							getProject().getMobileBuilder().compTemplateChanged(this);
//						}
//					}
//					
//					String oldContributors = contributorsShot == null ? null:contributorsShot;
//					doGetContributors();
//					String newContributors = contributorsShot == null ? null:contributorsShot;
//					if (oldContributors != null && newContributors != null) {
//						if (!(oldContributors.equals(newContributors))) {
//							getProject().getMobileBuilder().appContributorsChanged(this.getApplication());
//							getProject().getMobileBuilder().compModuleTsChanged(this);
//						}
//					}
//					
//					
//				} catch (JSONException e) {
//					e.printStackTrace();
//				} catch (Exception e) {
//					throw new RuntimeException(e);
//				}
//			};
//		}
//		checkBatchOperation(_markCompAsDirty);
//	}	
}
