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

package com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.IAction;
import com.twinsoft.convertigo.beans.ngx.components.MobileComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource.Filter;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource.SourceData;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource.SourceModel;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UICompVariable;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlDirective;
import com.twinsoft.convertigo.beans.ngx.components.UIControlDirective.AttrDirective;
import com.twinsoft.convertigo.beans.ngx.components.UIControlEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.ngx.components.UIElement;
import com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.beans.ngx.components.UIUseVariable;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.admin.services.studio.dbo.DboFactory;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.mobile.NgxBuilder;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.StringUtils;

/**
 * Extracts NGX components into a new shared component, as the shared component wizard of the Eclipse
 * Studio: the variables and functions of the page, shared component or application the components use
 * become variables of the shared component, set by a use of it that replaces the components, which are
 * kept disabled or removed.
 */
class SharedComponentExtraction {
	private static final Pattern p_var = Pattern.compile("((this|page)(\\.params\\d+)?\\.(\\w+))");
	private static final Pattern d_var = Pattern.compile("(((\\w+)\\s(\\w+)([^\\=]+))(\\=(.+))?)");
	private static final Pattern d_var_let = Pattern.compile("((let\\s)(\\w+)(\\s*\\=))");
	private static final Pattern d_var_as = Pattern.compile("((\\w+)(\\s*as\\s*)(\\w+))");
	private static final Pattern d_func = Pattern.compile("((\\w+)\\s*(\\([^\\(]*\\))\\s*\\{)");

	private final List<UIComponent> objectList;
	private final Map<String, Map<String, String>> ovarMap = new LinkedHashMap<>();
	private final Map<String, String> infoMap = new LinkedHashMap<>();
	private final Map<String, String> main_map = new LinkedHashMap<>();
	private Map<String, String> dlg_map = new HashMap<>();
	private final String defaultName;

	SharedComponentExtraction(List<UIComponent> objectList) throws Exception {
		this.objectList = objectList;
		var first = objectList.get(0);
		var dbo_qname = objectList.size() > 1 ? first.getParent().getQName() + "_Group" : first.getQName();
		defaultName = StringUtils.normalize(dbo_qname.replace(first.getApplication().getQName() + ".", ""));
		for (var uic : objectList) {
			scanForVariables(uic);
		}
	}

	/**
	 * @return whether the object can be extracted, as the Eclipse Studio allows it
	 */
	static boolean allows(DatabaseObject dbo) {
		if (!(dbo instanceof UIElement uie) || uie instanceof UIDynamicAction
				|| uie.getUIForm() != null && !uie.equals(uie.getUIForm())) {
			return false;
		}
		for (DatabaseObject current = dbo; current instanceof UIComponent uic; current = current.getParent()) {
			if (!uic.isEnabled()) {
				return false;
			}
		}
		return true;
	}

	String getDefaultName() {
		return defaultName;
	}

	boolean exists(String name) {
		for (var uisc : objectList.get(0).getApplication().getSharedComponentList()) {
			if (uisc.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the variables found, with the information on where they were found
	 */
	Map<String, String> getItemMap() {
		Map<String, String> map = new LinkedHashMap<>();
		for (var name : infoMap.keySet()) {
			// add class properties/functions & directive variables only
			if (main_map.containsKey(name)) {
				map.put(name, infoMap.get(name));
			}
		}
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Creates the shared component and the use of it replacing the components.
	 *
	 * @param variables the names of the variables of the shared component, by name of found variable
	 * @return the new shared component
	 */
	UISharedRegularComponent extract(String name, boolean keepOriginal, Map<String, String> variables) throws Exception {
		if (exists(name)) {
			throw new EngineException("The application already has a shared component named \"" + name + "\".");
		}
		dlg_map = new LinkedHashMap<>();
		for (var variable : getItemMap().keySet()) {
			var chosen = variables.getOrDefault(variable, variable);
			dlg_map.put(variable, StringUtils.normalize(chosen.isBlank() ? variable : chosen));
		}
		UISharedRegularComponent uisc = null;
		UIUseShared uius = null;
		try {
			uisc = createSharedComponent(name);
			uius = createUseShared(uisc.getQName());
			for (var uic : objectList) {
				if (keepOriginal) {
					uic.setEnabled(false);
					uic.hasChanged = true;
					BuilderUtils.dboChanged(uic, "isEnabled", true, false);
				} else {
					var parent = uic.getParent();
					parent.remove(uic);
					parent.hasChanged = true;
					BuilderUtils.dboRemoved(parent, uic);
				}
			}
			BuilderUtils.dboAdded(uisc);
			BuilderUtils.dboAdded(uius);
			return uisc;
		} catch (Exception e) {
			try {
				if (uisc != null) {
					uisc.getParent().remove(uisc);
				}
				if (uius != null) {
					uius.getParent().remove(uius);
				}
			} catch (Exception ex) {
				// nothing to undo
			}
			throw e;
		}
	}

	private static boolean isInPage(UIComponent uic) {
		return uic.getPage() != null;
	}

	private static boolean isInSharedComponent(UIComponent uic) {
		return uic.getSharedComponent() != null;
	}

	private static boolean isInApplication(UIComponent uic) {
		return !isInPage(uic) && !isInSharedComponent(uic);
	}

	private static boolean isInControlEvent(UIComponent uic) {
		DatabaseObject databaseObject = uic;
		while (databaseObject != null && !(databaseObject instanceof UIControlEvent)) {
			databaseObject = databaseObject.getParent();
		}
		return databaseObject != null;
	}

	private static boolean isSmartSource(java.beans.PropertyDescriptor pd) {
		// the properties the Eclipse Studio edits with its smart source editor
		return pd.getReadMethod() != null && MobileSmartSourceType.class.isAssignableFrom(pd.getPropertyType());
	}

	private static UICompVariable createCompVariable(String varName, String varValue) throws Exception {
		var compVariable = new UICompVariable();
		compVariable.setName(varName);
		compVariable.setVariableValue(varValue);
		compVariable.hasChanged = true;
		compVariable.bNew = true;
		return compVariable;
	}

	private static UIUseVariable createUseVariable(String varName, String varValue) throws Exception {
		var var_msst = new MobileSmartSourceType();
		var_msst.setMode(Mode.SCRIPT);
		var_msst.setSmartValue(varValue);
		var controlVariable = new UIUseVariable();
		controlVariable.setName(varName);
		controlVariable.setVarSmartType(var_msst);
		controlVariable.hasChanged = true;
		controlVariable.bNew = true;
		return controlVariable;
	}

	private static boolean forTemplate(UIComponent uic) {
		var forTemplate = true;
		if (uic instanceof IAction || uic.getParent() instanceof IAction) {
			if (uic.getUIComponentList().size() > 0) {
				forTemplate = false;
			} else {
				forTemplate = !(uic.getParent() instanceof IAction);
			}
		}
		return forTemplate;
	}

	private static String escapeString(String s) {
		if (s != null && !s.isEmpty()) {
			var b = new StringBuilder();
			var doIt = false;
			var len = s.length();
			char c;
			for (int i = 0; i < len; i++) {
				c = s.charAt(i);
				if (c == '\"') {
					b.append("#D#");
					doIt = !doIt;
				} else if (c == '\'' && doIt && (s.charAt(i - 1) != '\\')) {
					b.append("\\\\").append(c);
				} else {
					b.append(c);
				}
			}
			return b.toString().replace("#D#", "'");
		}
		return s;
	}

	private static String simplify(String s) {
		if (s != null && !s.isEmpty()) {
			var b = new StringBuilder();
			var count = 0;
			char c;
			for (int i = 0; i < s.length(); i++) {
				c = s.charAt(i);
				if (c == '{') {
					if (count == 0) {
						b.append(c);
					}
					count++;
				} else if (c == '}') {
					if (count == 1) {
						b.append(c);
					}
					count--;
				} else if (count == 0) {
					b.append(c);
				}
			}
			return b.toString();
		}
		return s;
	}

	private void scanForVariables(final UIComponent origin) throws Exception {
		final Set<String> identifierSet = new HashSet<>();
		try {
			new WalkHelper() {
				private void addMainVariable(String var_name, String var_value) {
					if (var_name != null && !var_name.isEmpty() && !main_map.containsKey(var_name)) {
						main_map.put(var_name, var_value == null ? "''" : var_value);
					}
				}

				private void getMainVariables() {
					try {
						List<String> declarations = new ArrayList<>();
						List<String> functions = new ArrayList<>();
						String c8o_Declarations = "", c8o_Functions = "", prefix = "";

						if (isInPage(origin)) {
							prefix = "Page";
							c8o_Declarations += origin.getPage().getComputedDeclarations() + System.lineSeparator();
							var c8o_UserCustoms = origin.getPage().getScriptContent().getString();
							c8o_Declarations += NgxBuilder.getMarker(c8o_UserCustoms, prefix + "Declaration");
							c8o_Functions += NgxBuilder.getMarker(c8o_UserCustoms, prefix + "Function");
						} else if (isInSharedComponent(origin)) {
							prefix = "Comp";
							for (var var : origin.getSharedComponent().getVariables()) {
								c8o_Declarations += "let " + var.getVariableName() + " = " + var.getVariableValue() + ";" + System.lineSeparator();
							}
							c8o_Declarations += origin.getSharedComponent().getComputedDeclarations() + System.lineSeparator();
							var c8o_UserCustoms = origin.getSharedComponent().getScriptContent().getString();
							c8o_Declarations += NgxBuilder.getMarker(c8o_UserCustoms, prefix + "Declaration");
							c8o_Functions += NgxBuilder.getMarker(c8o_UserCustoms, prefix + "Function");
						} else if (isInApplication(origin)) {
							prefix = "App";
							c8o_Declarations += origin.getApplication().getComputedDeclarations() + System.lineSeparator();
							var c8o_UserCustoms = origin.getApplication().getComponentScriptContent().getString();
							c8o_Declarations += NgxBuilder.getMarker(c8o_UserCustoms, prefix + "Declaration");
							c8o_Functions += NgxBuilder.getMarker(c8o_UserCustoms, prefix + "Function");
						}

						// class variables
						if (!c8o_Declarations.isEmpty()) {
							for (var line : Arrays.asList(c8o_Declarations.split(System.lineSeparator()))) {
								line = line.trim();
								if (!line.isEmpty() && line.indexOf(prefix + "Declaration") == -1) {
									if (line.indexOf(prefix + "@ViewChild") != -1 || line.indexOf(prefix + "@ViewChildren") != -1) {
										line = line.substring(line.indexOf(')'));
									}
									declarations.add(line);
								}
							}
							for (var line : declarations) {
								var matcher = d_var.matcher(line);
								while (matcher.find()) {
									var var_name = matcher.group(4);
									var var_value = matcher.group(7);
									if (var_value != null) {
										var_value = var_value.trim();
										if (var_value.charAt(var_value.length() - 1) == ';') {
											var_value = var_value.substring(0, var_value.length() - 1);
										}
										var_value = escapeString(var_value);
									}
									addMainVariable(var_name, var_value);
								}
							}
						}

						// class functions
						if (!c8o_Functions.isEmpty()) {
							c8o_Functions = simplify(c8o_Functions);
							for (var line : Arrays.asList(c8o_Functions.split(System.lineSeparator()))) {
								line = line.trim();
								if (!line.isEmpty() && line.indexOf(prefix + "Function") == -1) {
									var matcher = d_func.matcher(line);
									while (matcher.find()) {
										functions.add(matcher.group(2));
									}
								}
							}
							for (var func_name : functions) {
								addMainVariable(func_name, "() => {}");
							}
						}
					} catch (Exception e) {
						com.twinsoft.convertigo.engine.Engine.logStudio.debug("(SharedComponentExtraction) no class variables", e);
					}
				}

				private UIControlDirective getForDirective(UIComponent uic) {
					DatabaseObject databaseObject = uic;
					while (databaseObject != null && (!(databaseObject instanceof UIControlDirective directive)
							|| !AttrDirective.isForDirective(directive.getDirectiveName()))) {
						databaseObject = databaseObject.getParent();
					}
					return (UIControlDirective) databaseObject;
				}

				private void getForDirectiveVariables(UIComponent uic) {
					var uicomponent = uic;
					while (uicomponent != null && getForDirective(uicomponent) != null) {
						var uicd = getForDirective(uicomponent);
						if (!uic.equals(uicd)) {
							var item = "item" + uicd.priority;
							addMainVariable(item, "[]");
							addMapVariable(item, item, "this._params_." + item);
							addMapVariable(item, uicd.toString() + " : found variable which stands for the iterator's item");

							var itemName = uicd.getDirectiveItemName();
							addMainVariable(itemName, "{}");
							addMapVariable(itemName, itemName, "this._params_." + itemName);
							addMapVariable(itemName, uicd.toString() + " : found variable which stands for the customized iterator's item");

							var indexName = uicd.getDirectiveIndexName();
							addMainVariable(indexName, "0");
							addMapVariable(indexName, indexName, "this._params_." + indexName);
							addMapVariable(indexName, uicd.toString() + " : found variable which stands for the customized iterator's index");

							var expression = uicd.getDirectiveExpression();
							if (!expression.isEmpty()) {
								for (var s : Arrays.asList(expression.split("\\;"))) {
									var matcher = d_var_let.matcher(s);
									while (matcher.find()) {
										var expvar = matcher.group(3);
										addMainVariable(expvar, "''");
										addMapVariable(expvar, expvar, "this._params_." + expvar);
										addMapVariable(expvar, uicd.toString() + " : found variable used by the customized iterator's expression");
									}
									matcher = d_var_as.matcher(s);
									while (matcher.find()) {
										var expvar = matcher.group(4);
										addMainVariable(expvar, "''");
										addMapVariable(expvar, expvar, "this._params_." + expvar);
										addMapVariable(expvar, uicd.toString() + " : found variable used by the customized iterator's expression");
									}
								}
							}
						}
						var dbo = uicd.getParent();
						uicomponent = dbo instanceof UIComponent parent ? parent : null;
					}
				}

				private boolean checkVariable(String name) {
					return name != null && !name.isEmpty() && !identifierSet.contains(name);
				}

				private void addMapVariable(String name, String target, String replacement) {
					if (checkVariable(name)) {
						var var_name = StringUtils.normalize(name);
						ovarMap.computeIfAbsent(var_name, key -> new HashMap<>())
								.put(target, replacement.replace("_params_." + name, "_params_.") + var_name);
					}
				}

				private void addMapVariable(String name, String infos) {
					if (checkVariable(name)) {
						infoMap.putIfAbsent(StringUtils.normalize(name), infos);
					}
				}

				private void scanSmartSource(UIComponent uic, String p_name, MobileSmartSourceType msst) throws Exception {
					var extended = !forTemplate(uic);
					String s = null;
					try {
						if (Mode.SCRIPT.equals(msst.getMode())) {
							s = msst.getValue(extended);
						}
						if (Mode.SOURCE.equals(msst.getMode())) {
							s = msst.getSmartSource().toJsonString();
						}
					} catch (Exception e) {
						// no source
					}
					if (s != null) {
						var infos = uic.toString() + " : found variable used by '" + p_name + "' property";
						Matcher matcher = p_var.matcher(s);
						while (matcher.find()) {
							var name = matcher.group(4);
							var target = matcher.group(1);
							var replacement = matcher.group(2) + "._params_." + name;
							if (isInControlEvent(uic)) {
								replacement = forTemplate(uic) ? "_params_." + name : "scope._params_." + name;
							}
							addMapVariable(name, target, replacement);
							addMapVariable(name, infos);
						}
					}
				}

				@Override
				public void init(DatabaseObject databaseObject) throws Exception {
					getMainVariables();
					if (getForDirective(origin) != null) {
						getForDirectiveVariables(origin);
					}
					super.init(databaseObject);
				}

				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {
					if (databaseObject instanceof UIComponent uic && uic.isEnabled()) {
						if (databaseObject instanceof UIDynamicElement element && !element.getIdentifier().isEmpty()) {
							identifierSet.add(element.getIdentifier());
						}
						for (var pd : CachedIntrospector.getBeanInfo(databaseObject).getPropertyDescriptors()) {
							if (isSmartSource(pd) && pd.getReadMethod().invoke(databaseObject) instanceof MobileSmartSourceType msst
									&& (Mode.SCRIPT.equals(msst.getMode()) || Mode.SOURCE.equals(msst.getMode()))) {
								scanSmartSource(uic, pd.getName(), msst);
							}
						}
						if (databaseObject instanceof UIDynamicElement uide && uide.getIonBean() != null) {
							for (var property : uide.getIonBean().getProperties().values()) {
								if (!property.getValue().equals(false)) {
									var msst = property.getSmartType();
									if (Mode.SCRIPT.equals(msst.getMode()) || Mode.SOURCE.equals(msst.getMode())) {
										scanSmartSource(uide, property.getName(), msst);
									}
								}
							}
						}
						super.walk(databaseObject);
					}
				}
			}.init(origin);
		} catch (Exception e) {
			throw new EngineException("Unable to scan for variables", e);
		}
	}

	private static String replace(String smart_value, String source, String replacement) {
		var s = smart_value;
		// replace considering world boundaries
		s = s.replaceAll("\\b" + source + "\\b", Matcher.quoteReplacement(replacement));
		s = s.replaceAll("(this\\.)+", "this.");
		// treat special TS case initializing a js object passing current object variable's name and value
		s = s.replaceAll(replacement + "\\s*\\:\\s*" + replacement, source + ": " + replacement);
		return s;
	}

	private void updateMobileSmartSources(final UISharedRegularComponent uisc) throws Exception {
		final var priority = "" + uisc.priority;
		try {
			new WalkHelper() {
				private boolean checkVariable(String name) {
					return name != null && !name.isEmpty() && dlg_map.get(name) != null;
				}

				private MobileSmartSourceType updateMobileSmartSourceType(boolean forTemplate, MobileSmartSourceType msst) throws Exception {
					var extended = !forTemplate;
					if (Mode.SCRIPT.equals(msst.getMode())) {
						var smart_value = msst.getValue(extended);
						for (var name : ovarMap.keySet()) {
							if (checkVariable(name)) {
								var m = ovarMap.get(name);
								for (var target : m.keySet()) {
									var replacement = m.get(target).replace("_params_." + name, dlg_map.get(name));
									smart_value = replace(smart_value, target, replacement);
								}
							}
						}
						var new_msst = new MobileSmartSourceType();
						new_msst.setMode(Mode.SCRIPT);
						new_msst.setSmartValue(smart_value);
						return new_msst;
					}
					if (Mode.SOURCE.equals(msst.getMode())) {
						var mss = msst.getSmartSource();
						if (mss != null) {
							SourceModel model = mss.getModel();
							MobileSmartSource new_mss = null;
							SourceModel mew_model = null;
							if (mss.getFilter().equals(Filter.Iteration)) {
								mew_model = MobileSmartSource.emptyModel(Filter.Shared);
								mew_model.setPath(model.getPath());
								mew_model.setPrefix(model.getPrefix());
								mew_model.setSuffix(model.getSuffix());
								mew_model.setCustom(model.getCustom());
								mew_model.setUseCustom(model.getUseCustom());
								List<SourceData> dataList = model.getSourceData();
								if (dataList.size() > 0) {
									var found = false;
									for (var data : dataList) {
										for (var name : ovarMap.keySet()) {
											if (checkVariable(name)) {
												for (var target : ovarMap.get(name).keySet()) {
													if (target.equals(data.getValue()) && !found) {
														found = true;
														var shared = Filter.Shared.toSourceData(mss.getProjectName(), "params" + priority);
														mew_model.addSourceData(shared);
														mew_model.setPath("?." + dlg_map.get(name));
														mew_model.setSuffix(model.getPath() + model.getSuffix());
													}
												}
											}
										}
									}
								}
								if (mew_model.getSourceData().size() > 0) {
									new_mss = new MobileSmartSource(Filter.Shared, mss.getProjectName(), mss.getInput(), mew_model.toJson());
								}
							}
							if (new_mss == null) {
								new_mss = MobileSmartSource.valueOf(mss.toJsonString());
							}
							mew_model = new_mss.getModel();
							for (var name : ovarMap.keySet()) {
								if (checkVariable(name)) {
									var m = ovarMap.get(name);
									for (var target : m.keySet()) {
										var replacement = m.get(target).replace("_params_." + name, "_params_." + dlg_map.get(name));
										replacement = replacement.replace("_params_", "params" + priority);
										mew_model.setPrefix(replace(model.getPrefix(), target, replacement));
										mew_model.setSuffix(replace(model.getSuffix(), target, replacement));
										mew_model.setCustom(replace(model.getCustom(), target, replacement));
									}
								}
							}
							var new_msst = new MobileSmartSourceType();
							new_msst.setMode(Mode.SOURCE);
							new_msst.setSmartValue(new_mss.toJsonString());
							return new_msst;
						}
					}
					return msst;
				}

				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {
					if (databaseObject instanceof UIComponent uic && uic.isEnabled() && !isInControlEvent(uic)) {
						var forTemplate = forTemplate(uic);
						for (var pd : CachedIntrospector.getBeanInfo(databaseObject).getPropertyDescriptors()) {
							if (isSmartSource(pd) && pd.getWriteMethod() != null
									&& pd.getReadMethod().invoke(databaseObject) instanceof MobileSmartSourceType msst
									&& (Mode.SCRIPT.equals(msst.getMode()) || Mode.SOURCE.equals(msst.getMode()))) {
								pd.getWriteMethod().invoke(databaseObject, updateMobileSmartSourceType(forTemplate, msst));
							}
						}
						if (databaseObject instanceof UIDynamicElement uide && uide.getIonBean() != null) {
							var ionBean = uide.getIonBean();
							for (var property : ionBean.getProperties().values()) {
								if (!property.getValue().equals(false)) {
									var msst = property.getSmartType();
									if (Mode.SCRIPT.equals(msst.getMode()) || Mode.SOURCE.equals(msst.getMode())) {
										ionBean.setPropertyValue(property.getName(), updateMobileSmartSourceType(forTemplate, msst));
									}
								}
							}
						}
						super.walk(databaseObject);
					}
				}
			}.init(uisc);
		} catch (Exception e) {
			throw new EngineException("Unable to update mobile smart sources", e);
		}
	}

	private String getVariablesDefaultValue(String var_name) {
		var var_value = var_name == null ? null : main_map.get(var_name);
		return var_value == null ? "''" : var_value;
	}

	private UISharedRegularComponent createSharedComponent(String name) throws Exception {
		var uisc = new UISharedRegularComponent();
		uisc.setName(name);
		uisc.hasChanged = true;
		uisc.bNew = true;
		// added before the copies of the components
		objectList.get(0).getApplication().add(uisc);
		for (var uic : objectList) {
			uisc.add(DboFactory.copyOf(uic));
		}
		updateMobileSmartSources(uisc);
		for (var variable : dlg_map.keySet()) {
			uisc.add(createCompVariable(dlg_map.get(variable), getVariablesDefaultValue(variable)));
		}
		return uisc;
	}

	private UIUseShared createUseShared(String qname) throws Exception {
		var uius = new UIUseShared();
		uius.setSharedComponentQName(qname);
		uius.hasChanged = true;
		uius.bNew = true;
		for (var variable : dlg_map.keySet()) {
			uius.add(createUseVariable(dlg_map.get(variable), ovarMap.get(variable).keySet().iterator().next()));
		}
		var uic = objectList.get(objectList.size() - 1);
		var mc = (MobileComponent) uic.getParent();
		if (mc instanceof ApplicationComponent parent) {
			parent.add(uius, uic.priority);
		} else if (mc instanceof PageComponent parent) {
			parent.add(uius, uic.priority);
		} else if (mc instanceof UIComponent parent) {
			parent.add(uius, uic.priority);
		}
		return uius;
	}
}
