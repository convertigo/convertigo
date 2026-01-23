/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

import java.beans.BeanInfo;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;
import org.codehaus.jettison.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.IDynamicBean;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonConfig;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonEvent;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonProperty;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.RhinoUtils;

public class UIDynamicElement extends UIElement implements IDynamicBean {

	private static final long serialVersionUID = -614143305710906263L;
	
	public UIDynamicElement() {
		super();
	}
	
	public UIDynamicElement(String tagName) {
		super(tagName);
	}

	@Override
	public UIDynamicElement clone() throws CloneNotSupportedException {
		UIDynamicElement cloned = (UIDynamicElement) super.clone();
		cloned.ionBean = ionBean; // needed by UISharedComponent for UIUseShared !
		return cloned;
	}

	private String beanData = "";
	
	public String getBeanData() {
		return beanData;
	}

	public void setBeanData(String beanData) {
		this.beanData = beanData;
	}
	
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);

		// load bean data
		//loadBean();
	}
	
	@Override
	public Element toXml(org.w3c.dom.Document document) throws EngineException {
    	// store bean data 
		saveBean();
		
		return super.toXml(document);
	}

	transient protected IonBean ionBean = null;
	
	protected void loadBean() throws Exception {
		if (ionBean == null && beanData != null) {
			if (this.parent == null) {
				System.out.println("WARN - loading beanData without cm");
			}
			ionBean = ComponentManager.of(this).loadBean(beanData);
		}
	}
	
	public void saveBean() {
		if (ionBean != null) {
			beanData = ionBean.toBeanData();
    	}
	}
	
	public void loadBean(ComponentManager cm) throws Exception {
		if (cm != null /*&& ionBean == null*/ && beanData != null) {
			ionBean = cm.loadBean(beanData);
		}
	}
	
	public IonBean getIonBean() {
		try {
			loadBean();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ionBean;
	}

	@Override
	protected boolean needNgTemplate() {
		IonBean ionBean = getIonBean();
		if (ionBean != null) {
			return ionBean.needNgTemplate();
		}
		return false;
	}
	
	@Override
	public String getDynamicIconName(int iconType) {
		IonBean ionBean = getIonBean();
    	if (ionBean != null) {
			if (iconType == BeanInfo.ICON_COLOR_16x16) {
				return ionBean.getIconColor16Path();
			}
			else if (iconType == BeanInfo.ICON_COLOR_32x32) {
				return ionBean.getIconColor32Path();
			}
    	}
		return null;
	}
	
	protected String getFormatedLabel() {
		if (ionBean != null) {
			String source = ionBean.getDisplayFormat();
			if (!source.isEmpty()) {
				String sourceName = "displayFormat", message = null;
				Context javascriptContext = org.mozilla.javascript.Context.enter();
				Scriptable scope = javascriptContext.initStandardObjects(null);
				for (IonProperty property : ionBean.getProperties().values()) {
					String p_name = property.getName();
					//Object p_value = property.getValue();
					String smartValue = property.getSmartType().getLabel().replace("{{", "").replace("}}", "").replace("?.", ".");
					Scriptable jsObject = ((smartValue == null) ? null:org.mozilla.javascript.Context.toObject(smartValue, scope));
					scope.put(p_name, scope, jsObject);
				}
				try {
					Object ob = RhinoUtils.evalInterpretedJavascript(javascriptContext, scope, source, sourceName, 1, null);
					if (ob instanceof Function) {
						Object returnedValue = ((Function) ob).call(javascriptContext, scope, scope, RhinoUtils.EMPTY_ARGS);
						return returnedValue.toString();
					}
				}
				catch(EcmaError e) {
					message = "Unable to evaluate code for '"+ sourceName +"'.\n" +
					"UIDynamicElement: \"" + getName() + "\"\n" +
					"A Javascript runtime error has occured at line " + 
					e.lineNumber() + ", column " + e.columnNumber() + ": " +
					e.getMessage() + " \n" + e.lineSource();
				}
				catch(EvaluatorException e) {
					message = "Unable to evaluate code for '"+ sourceName +"'.\n" +
					"UIDynamicElement: \"" + getName() + "\"\n" +
					"A Javascript evaluation error has occured: " + e.getMessage();
				}
				catch(JavaScriptException e) {
					message = "Unable to evaluate code for '"+ sourceName +"'.\n" +
					"UIDynamicElement: \"" + getName() + "\"\n" +
					"A Javascript error has occured: " + e.getMessage();
				}
				finally {
					if (javascriptContext != null) {
						org.mozilla.javascript.Context.exit();
					}
					
					if (message != null) {
						System.out.println(message);
						Engine.logBeans.warn(message);
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		String formatedLabel = getFormatedLabel();
		if (formatedLabel != null) {
			return formatedLabel;
		}
		
		String id = getIdentifier();
		return getName() + (id.isEmpty() ? "":" #"+id);
	}
	
	protected static boolean isComposedValue(String val) {
		Pattern pattern = Pattern.compile("\\$\\[(.*)\\]");
		Matcher matcher = pattern.matcher(val);
		return matcher.find();
	}
	
	@Override
	protected StringBuilder initAttrClasses() {
		StringBuilder attrclasses = super.initAttrClasses();
		IonBean ionBean = getIonBean();
		
    	if (ionBean != null) {
			for (IonProperty property : ionBean.getProperties().values()) {
				//String name = property.getName();
				String attr = property.getAttr();
				Object value = property.getValue();
				
				// case value is set
				if (!value.equals(false)) {
					String smartValue = property.getSmartValue();
					if (attr.equals("class")) {
						attrclasses.append(attrclasses.length()> 0 ? " ":"").append(smartValue);
					}
				}
			}
    	}
		
		return attrclasses;
	}
	
	@Override
	protected StringBuilder initAttributes() {
		StringBuilder attributes = super.initAttributes();
		
		IonBean ionBean = getIonBean();
		
    	if (ionBean != null) {
			UIForm form = getUIForm();
			boolean underForm = form != null;

    		Map<String, String> vm = new HashMap<String, String>();
			for (IonProperty property : ionBean.getProperties().values()) {
				String name = property.getName();
				Object value = property.getValue();
				if (!value.equals(false)) {
					MobileSmartSourceType msst = property.getSmartType();
					String smartValue = msst.getValue();
					if (Mode.PLAIN.equals(msst.getMode())) {
						if (property.getType().equalsIgnoreCase("string")) {
							if (!isComposedValue(smartValue)) {
								smartValue = "\'" + MobileSmartSourceType.escapeStringForTpl(smartValue) + "\'";
							}
						}
					}
					vm.put(name, smartValue);
				} else {
					vm.put(name, "null");
				}
			}
			
			StringSubstitutor sub = new StringSubstitutor(vm,"$[","]");
			sub.setEnableSubstitutionInVariables(true);
			
			for (IonProperty property : ionBean.getProperties().values()) {
				String name = property.getName();
				String attr = property.getAttr();
				Object value = property.getValue();
				boolean isComposite = property.isComposite();
				
				// case value is set
				if (!value.equals(false)) {
					if (attr.equals("class")) {
						continue; // already handle in initAttrClasses
					}
					
					if (name.equals("AutoDisable")) {
						if (underForm) {
							String formIdentifier = form.getIdentifier();
							if (!formIdentifier.isBlank()) {
								attributes.append(" [disabled]=\"").append(formIdentifier).append(".invalid\"");
							}
						}
					} else {
						String smartValue = property.getSmartValue();
						smartValue = sub.replace(smartValue);
						
						if (name.equals("DoubleBinding")) {
							smartValue = smartValue.replaceAll("\\?\\.\\[", "[").replaceAll("\\?\\.", ".");
						}
						
						if (!isComposite) {
							attributes.append(" ");
							if (attr.isEmpty()) {
								attributes.append(smartValue);
							}
							else if (attr.indexOf("%%") != -1){
								attributes.append(attr.replaceFirst("%%", smartValue));
							}
							else {
								attributes.append(attr).append("=");
								attributes.append("\"").append(smartValue).append("\"");
							}
						}
					}
				}
				// case value is not set
				else {
					if (!isComposite) {
						if (underForm) {
							if (name.equals("DoubleBinding")) {
								String defval = null;
								if (tagName.equals("ion-checkbox") || tagName.equals("ion-toggle")) {
									defval = vm.get("Checked");
								} else {
									defval = vm.get("Value");
								}
								
								// fix #707 : do not generate anymore one way binging based on empty default control value
								if (defval != null && !defval.equals("null") && !defval.isBlank()) {
									attributes.append(" [ngModel]=\"" + defval + "\""); // one way binding
								} else {
									attributes.append(" ngModel");
								}
							}
						}
					}
				}
			}
    	}
		return attributes;
	}

	@Override
	public boolean updateSmartSourceModelPath(MobileSmartSource oldSource, String newPath) {
		boolean updated = false;
		if (beanData != null) {
			IonBean ionbean = null;
			try {
				ionbean = new IonBean(beanData);
				if (ionbean != null) {
					for (IonProperty property : ionbean.getProperties().values()) {
						MobileSmartSource mss = property.getSmartType().getSmartSource();
						if (mss != null) {
							MobileSmartSource newMss = mss.from(oldSource, newPath);
							if (newMss != null) {
								String oldVal = StringEscapeUtils.escapeJson(mss.toJsonString());
								String newVal = StringEscapeUtils.escapeJson(newMss.toJsonString());
								beanData = beanData.replace(oldVal, newVal);
								ionBean = null;
								updated = this.hasChanged = true;							
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return updated;
	}
	
	@Override
	public boolean updateSmartSource(String oldString, String newString) {
		boolean updated = false;
		if (beanData != null) {
			if (beanData.indexOf(oldString) != -1 || Pattern.compile(oldString).matcher(beanData).find()) {
				beanData = beanData.replaceAll(oldString, newString);
				ionBean = null;
				updated = this.hasChanged = true;
			}
		}
		return updated;
	}
	
	@Override
	protected String getFormControlName() {
		IonBean ionBean = getIonBean();
		if (ionBean != null && ionBean.hasProperty("ControlName")) {
			MobileSmartSourceType msst = (MobileSmartSourceType) ionBean.getPropertyValue("ControlName");
			if (msst != null && !msst.getValue().equals("not set") && !msst.getValue().isEmpty()) {
				return msst.getValue();
			}
		}
		return super.getFormControlName();
	}
	
	protected String getEventAttr(String eventName) {
		IonBean ionBean = getIonBean();
		if (ionBean != null) {
			IonEvent ionEvent = ionBean.getEvent(eventName);
			if (ionEvent != null) {
				return ionEvent.getAttr();
			}
		}
		return "";
	}
	
	@Override
	protected String getCustomScss() {
		IonBean ionBean = getIonBean();
		
		String scss = "";
		if (ionBean != null) {
			for (String s: ionBean.getScssList()) {
				scss += s + System.lineSeparator();
			}
			if (!scss.isEmpty()) {
				String cartridge = "";
				cartridge += "/**\n";
				cartridge += " * Custom properties (sometimes referred to as CSS variables or cascading variables)\n";
				cartridge += " * are entities defined by CSS authors that contain specific values to be reused throughout a document.\n";
				cartridge += " * They are set using custom property notation, e.g.: --main-color: black;\n";
				cartridge += " * and are accessed using the var() function, e.g.: color: var(--main-color);\n";
				cartridge += " * You can find below your component's properties you can customize within the page.\n";
				cartridge += " * If you'd like to make some customizations for whole app, please see your app Style & Theme components.\n";
				cartridge += " * For more informations see https://ionicframework.com/docs/theming.\n";
				cartridge += "**/\n";
				
				scss = cartridge + scss;
			}
		}
		return scss;
	}
	
	protected String[] getEventNames() {
		IonBean ionBean = getIonBean();
		
		String[] eventNames = new String[0];
    	if (ionBean != null) {
    		eventNames = ionBean.getEvents().keySet().toArray(new String[0]);
    		Arrays.sort(eventNames);
    	}
    	return eventNames;
	}

	@Override
	protected String getRequiredTplVersion() {
		if (parent == null && ionBean == null) {
			if (beanData != null) {
				try {
					IonBean ion = new IonBean(beanData);
					return ion.getTplVersion();
				} catch (Exception e) {}
			}
			return "";
		}
		
		IonBean ionBean = getIonBean();
		if (ionBean != null) {
			return ionBean.getTplVersion();
		}
		return super.getRequiredTplVersion();
	}

	@Override
	protected String getDeprecatedTplVersion() {
		IonBean ionBean = getIonBean();
		if (ionBean != null) {
			return ionBean.getDeprecatedTplVersion();
		}
		return "";
	}
	
	protected IonConfig getBeanConfig() {
		IonBean ionBean = getIonBean();
		if (ionBean != null && !isDeprecated()) {
			if (this.compareToTplVersion("8.4.0.0") < 0) { // treats here ion config migration of TPL < 8.4.0.0
				try {
					JSONObject jsonObject = ionBean.getConfig().getJSONObject();
					if (!jsonObject.has("action_ts_imports")) { // not ion UIDynamicAction
						if (jsonObject.has("module_ts_imports")) {
							jsonObject.put("local_module_ts_imports", jsonObject.remove("module_ts_imports"));
						}
						if (jsonObject.has("module_ng_imports")) {
							jsonObject.put("local_module_ng_imports", jsonObject.remove("module_ng_imports"));
						}
						if (jsonObject.has("module_ng_providers")) {
							jsonObject.put("local_module_ts_providers", jsonObject.remove("module_ts_providers"));
						}
						return IonConfig.get(jsonObject);
					}
				} catch (Exception e) {}
			}
			return ionBean.getConfig();
		}
		return null;
	}
	
	@Override
	protected Contributor getContributor() {
		final boolean isTplLowerThan8400 = this.compareToTplVersion("8.4.0.0") < 0;
		final boolean tplIsStandalone = UIDynamicElement.this.isTplStandalone();

		return new Contributor() {
			
			private boolean doit() {
				try {
					return isAppContainer() || isContainer((MobileComponent)getMainScriptComponent());
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
			
			private boolean usesRouterLink() {
				IonProperty prop = ionBean.getProperty("LinkRouterPath");
				if (prop != null && !prop.getValue().equals(false)) {
					if (!prop.getSmartValue().isBlank()) {
						return true;
					}
				}
				return false;
			}
			
			private void addMapImport(Map<String, List<String>> map, String name, String path) {
				if (map != null) {
					List<String> list = map.get(path);
					if (list == null) {
						list = new ArrayList<String>();
					}
					if (!list.contains(name)) {
						list.add(name);
						map.put(path, list);
					}
				}
			}
			
			@Override
			public boolean isNgModuleForApp() {
				if (!getModuleNgImports().isEmpty() || !getModuleNgProviders().isEmpty()) {
					return true;
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
				IonConfig ionConfig = getBeanConfig();
				if (ionConfig != null) {
					if (doit()) {
						Map<String, List<String>> map = new HashMap<String, List<String>>();
						if (isAppContainer()) {
							map.putAll(ionConfig.getModuleTsImports());
						}
						if (!isMainTs() && isContainer((MobileComponent)getMainScriptComponent())) {
							map.putAll(ionConfig.getLocalModuleTsImports());
							
							if (tplIsStandalone) {
								if (usesRouterLink()) {
									addMapImport(map, "{ RouterLink }", "@angular/router");
									addMapImport(map, "{ IonRouterLink }", "@ionic/angular/standalone");
								}
							}
						}
						
						if (map.size() > 0) {
							for (String from : map.keySet()) {
								for (String component: map.get(from)) {
									String entry = component.trim();
									if (!entry.isEmpty() && !from.isEmpty()) {
										if (isTplLowerThan8400) {
											entry = "{ "+ entry + " }";
										}
										if (!imports.containsKey(entry)) {
											imports.put(entry, from);
										}
									}
								}
							}
						}
					}
				}				
				return imports;
			}

			@Override
			public Set<String> getModuleNgImports() {
				IonConfig ionConfig = getBeanConfig();
				if (ionConfig != null) {
					if (doit()) {
						Set<String> set = new HashSet<String>();
						if (isAppContainer()) {
							set.addAll(ionConfig.getModuleNgImports());
						}
						if (!isMainTs() && isContainer((MobileComponent)getMainScriptComponent())) {
							set.addAll(ionConfig.getLocalModuleNgImports());
							
							if (tplIsStandalone) {
								if (usesRouterLink()) {
									set.add("RouterLink");
									set.add("IonRouterLink");
								}
							}
						}
						return set;
					}
				}
				return new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgProviders() {
				IonConfig ionConfig = getBeanConfig();
				if (ionConfig != null) {
					if (doit()) {
						Set<String> set = new HashSet<String>();
						if (isAppContainer()) {
							set.addAll(ionConfig.getModuleNgProviders());
						}
						if (!isMainTs() && isContainer((MobileComponent)getMainScriptComponent())) {
							set.addAll(ionConfig.getLocalModuleNgProviders());
						}
						return set;
					}
				}
				return new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgDeclarations() {
				IonBean ionBean = getIonBean();
				if (ionBean != null && !isDeprecated()) {
					return ionBean.getConfig().getModuleNgDeclarations();
				}
				return new HashSet<String>();
			}
			
			@Override
			public Set<String> getModuleNgComponents() {
				IonBean ionBean = getIonBean();
				if (ionBean != null && !isDeprecated()) {
					return ionBean.getConfig().getModuleNgComponents();
				}
				return new HashSet<String>();
			}

			@Override
			public Map<String, String> getPackageDependencies() {
				IonBean ionBean = getIonBean();
				if (ionBean != null && !isDeprecated()) {
					return ionBean.getConfig().getPackageDependencies();
				}
				return new HashMap<String, String>();
			}

			@Override
			public Map<String, String> getConfigPlugins() {
				IonBean ionBean = getIonBean();
				if (ionBean != null && !isDeprecated()) {
					Map<String, String> map = ionBean.getConfig().getConfigPlugins();
					for (String plugin: map.keySet()) {
						try {
							JSONObject json = new JSONObject(map.get(plugin));
							if (json.has("variables")) {
								boolean hasChanged = false;
								JSONObject jsonVars = json.getJSONObject("variables");
								@SuppressWarnings("unchecked")
								Iterator<String> it = jsonVars.keys();
								while (it.hasNext()) {
									String varkey = it.next();
									String varval = jsonVars.getString(varkey);
									if (varval.startsWith("@")) {// value = @propertyName
										String propertyName = varval.substring(1);
										if (ionBean.hasProperty(propertyName)) {
											IonProperty ionProperty = ionBean.getProperty(propertyName);
											Object p_value = ionProperty.getValue();
											String value = "";
											if (!p_value.equals(false)) {
												MobileSmartSourceType msst = ionProperty.getSmartType();
												String smartValue = msst.getValue();
												if (Mode.PLAIN.equals(msst.getMode())) {
													value = smartValue;
												}
											}
											
											jsonVars.put(varkey, value);
											hasChanged = true;
										}
									}
								}
								if (hasChanged) {
									json.put("variables", jsonVars);
									map.put(plugin, json.toString());
								}
							}
						} catch (Exception e) {}
					}
					return map;
				}
				return new HashMap<String, String>();
			}

			@Override
			public Set<String> getModuleNgRoutes(String pageSegment) {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getBuildAssets() {
				IonBean ionBean = getIonBean();
				if (ionBean != null) {
					return ionBean.getConfig().getBuildAssets();
				}
				return new HashSet<String>();
			}

			@Override
			public Set<String> getBuildScripts() {
				IonBean ionBean = getIonBean();
				if (ionBean != null) {
					return ionBean.getConfig().getBuildScripts();
				}
				return new HashSet<String>();
			}

			@Override
			public Set<String> getBuildStyles() {
				IonBean ionBean = getIonBean();
				if (ionBean != null) {
					return ionBean.getConfig().getBuildStyles();
				}
				return new HashSet<String>();
			}
		};
	}
	
}
