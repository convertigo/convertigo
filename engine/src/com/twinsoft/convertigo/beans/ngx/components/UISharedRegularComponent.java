/*
 * Copyright (c) 2001-2021 Convertigo SA.
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
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;

public class UISharedRegularComponent extends UISharedComponent implements ISharedComponent, IScriptComponent, IStyleGenerator {

	private static final long serialVersionUID = -2506259541566203802L;

	transient private Runnable _markCompAsDirty;
	
	public UISharedRegularComponent() {
		super(true);
	}
	
	@Override
	public String getIdentifier() {
		return identifier = "comp"+ priority;
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
	
	@Override
	public List<Contributor> getContributors() {
		if (contributors == null) {
			doGetContributors();
		}
		return contributors;
	}
	
	protected synchronized void doGetContributors() {
		contributors = new ArrayList<>();
		Set<UIComponent> done = new HashSet<>();
		//if (isEnabled()) { // Commented until we can delete page folder again... : see forceEnable in MobileBuilder 
			for (UIComponent uiComponent : getUIComponentList()) {
				uiComponent.addContributors(done, contributors);
			}
		//}		
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
	
	public JSONObject getComputedContents() {
		if (computedContents == null) {
			doComputeContents();
		}
		return computedContents;
	}
	
	@Override
	protected synchronized void doComputeContents() {
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
		if (isEnabled()) {
			Iterator<UIComponent> it;
			
			String events = "";
			String params = "";
			//String params_interfaces = "scope?: any";
			
			it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof UICompVariable) {
					UICompVariable uicv = (UICompVariable)component;
					if (uicv.isEnabled()) {
						//params_interfaces += params_interfaces.length() > 0 ? ", " : "";
						//params_interfaces += uicv.getVariableName() + "?: any";
						String varName = uicv.getVariableName();
						String varValue = uicv.getVariableValue();
						//params += params.length() > 0 ? ", " : "";
						params += "\t@Input() "+ varName + ": any = " + (varValue.isEmpty() ? "undefined":varValue) + System.lineSeparator();
						events += "\t@Output() "+ varName +"Change = new EventEmitter<any>();"+ System.lineSeparator();
					}
				}
				if (component instanceof UICompEvent) {
					UICompEvent uice = (UICompEvent)component;
					if (uice.isEnabled()) {
						String eventName = uice.getAttrName();
						if (!eventName.isBlank()) {
							events += "\t@Output() "+ eventName +" = new EventEmitter<any>();"+ System.lineSeparator();
						}
					}
				}
			}
			try {
				//String interfaces = jsonScripts.getString("interfaces");
				//interfaces += "interface IParams {"+ params_interfaces +"}"+ System.lineSeparator();
				//jsonScripts.put("interfaces", interfaces);
				
				String declarations = jsonScripts.getString("declarations");
				declarations += "@Input() owner : C8oPageBase = undefined"+ System.lineSeparator();
				//declarations += "\t@Input() params"+ priority + " : IParams = {" + params + "}"+ System.lineSeparator();
				declarations += params;
				declarations += events;
				//declarations += "\tpublic params : IParams = {" + params + "}"+ System.lineSeparator();
				jsonScripts.put("declarations", declarations);
				
				String initializations = jsonScripts.getString("initializations");
				//initializations += "this.params = this.params"+ priority + System.lineSeparator();
				jsonScripts.put("initializations", initializations);

				String dispositions = jsonScripts.getString("dispositions");
				jsonScripts.put("dispositions", dispositions);
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			
			it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				component.computeScripts(jsonScripts);
			}
		}
	}

	@Override
	protected void computeScripts(UIUseShared uiUse, JSONObject jsonScripts) {
		// TODO Auto-generated method stub
		//super.computeScripts(uiUse, jsonScripts);
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
		StringBuilder sb = new StringBuilder();
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
		return sb.toString();
	}
	
	@Override
	protected String computeStyle(UIUseShared uiUse) {
		return "";
	}

	@Override
	protected void addContributors(Set<UIComponent> done, List<Contributor> contributors) {
		if (!done.add(this)) {
			return;
		}
		if (isEnabled()) {
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
	}

	@Override
	protected void addContributors(UIUseShared uiUse, Set<UIComponent> done, List<Contributor> contributors) {
		if (!done.add(this)) {
			return;
		}
		if (isEnabled()) {
			Contributor contributor = getContributor();
			if (contributor != null) {
				if (!contributors.contains(contributor)) {
					contributors.add(contributor);
				}
			}
			/*for (UIComponent uic : getUIComponentList()) {
				uic.addContributors(done, contributors);
			}*/
		}
	}
	
	@Override
	protected Contributor getContributor() {
		final Project project = getProject();
		final String compName = getName();
		final String c8o_CompName = compName;
		final String c8o_CompModuleName = compName + "Module";

		return new Contributor() {
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
			public Map<String, String> getModuleTsImports(MobileComponent container) {
				String c8o_CompModulePath;
				
				Map<String, String> imports = new HashMap<String, String>();
				try {
					Path modulePath = Paths.get(new File (project.getDirFile(), "_private/ionic/src/app/components/"+c8o_CompName.toLowerCase() 
											+ "/" +c8o_CompName.toLowerCase() + ".module").getCanonicalPath());
					c8o_CompModulePath = getContainerPath(container).relativize(modulePath).toString().replace('\\', '/');
				} catch (Exception e) {
					c8o_CompModulePath = "../components/"+ c8o_CompName.toLowerCase() + "/" +c8o_CompName.toLowerCase() + ".module";
				}
				imports.put(c8o_CompModuleName, c8o_CompModulePath);
				return imports;
			}

			@Override
			public Set<String> getModuleNgImports() {
				Set<String> ngImports = new HashSet<String>();
				ngImports.add(c8o_CompModuleName);
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
		};
	}
	
	public void markCompTsAsDirty() throws EngineException {
		getProject().getMobileBuilder().compTsChanged(this, false);
	}
	
	@Override
	public void markCompAsDirty() throws EngineException {
		if (_markCompAsDirty == null) {
			_markCompAsDirty = () -> {
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
						if (!(newComputedContent.getJSONObject("scripts").toString()
								.equals(oldComputedContent.getJSONObject("scripts").toString()))) {
							getProject().getMobileBuilder().compTsChanged(this, true);
						}
					}
					if (oldComputedContent != null && newComputedContent != null) {
						if (!(newComputedContent.getString("style")
								.equals(oldComputedContent.getString("style")))) {
							getProject().getMobileBuilder().compStyleChanged(this);
						}
					}
					if (oldComputedContent != null && newComputedContent != null) {
						if (!(newComputedContent.getString("template")
								.equals(oldComputedContent.getString("template")))) {
							getProject().getMobileBuilder().compTemplateChanged(this);
						}
					}
					
					String oldContributors = contributors == null ? null: contributors.toString();
					doGetContributors();
					String newContributors = contributors == null ? null: contributors.toString();
					if (oldContributors != null && newContributors != null) {
						if (!(oldContributors.equals(newContributors))) {
							//getProject().getMobileBuilder().appContributorsChanged(this.getApplication());
							getProject().getMobileBuilder().compModuleTsChanged(this);
						}
					}
					
					
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			};
		}
		checkBatchOperation(_markCompAsDirty);
	}	
}
