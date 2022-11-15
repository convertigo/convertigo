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

package com.twinsoft.convertigo.eclipse.wizards.new_mobile;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.IAction;
import com.twinsoft.convertigo.beans.mobile.components.MobileComponent;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSource;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSource.Filter;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSource.SourceData;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSource.SourceModel;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.UICompVariable;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIControlDirective;
import com.twinsoft.convertigo.beans.mobile.components.UIControlDirective.AttrDirective;
import com.twinsoft.convertigo.beans.mobile.components.UIControlEvent;
import com.twinsoft.convertigo.beans.mobile.components.UIControlVariable;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.mobile.components.UISharedComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIUseShared;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonProperty;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.mobile.Ionic3Builder;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class SharedComponentWizard extends Wizard {

	private static Pattern p_var = Pattern.compile("((this|page)(\\.params\\d+)?\\.(\\w+))");

	private static Pattern d_var = Pattern.compile("(((\\w+)\\s(\\w+)([^\\=]+))(\\=([^\\=]+))?)");
	private static Pattern d_var_let = Pattern.compile("((let\\s)(\\w+)(\\s*\\=))");
	private static Pattern d_var_as = Pattern.compile("((\\w+)(\\s*as\\s*)(\\w+))");

	private String className = "com.twinsoft.convertigo.beans.mobile.components.UISharedComponent";
	private List<DatabaseObject> objectList = null;

	private SharedComponentWizardPage1 page1;
	private SharedComponentWizardPage2 page2;

	private Map<String, Map<String, String>> ovarMap = new LinkedHashMap<String, Map<String,String>>();
	private Map<String, String> infoMap = new LinkedHashMap<String,String>();
	private Map<String, String> main_map = new LinkedHashMap<String, String>();
	private Map<String, String> dlg_map = new HashMap<String, String>();
	private String shared_comp_name = null;
	private boolean keep_original = true;
	private boolean ignore_callbacks = true;

	public DatabaseObject newBean = null;

	public SharedComponentWizard(List<DatabaseObject> objectList) throws Exception {
		super();
		this.objectList = objectList;
		setWindowTitle("Create a new shared component");
		setNeedsProgressMonitor(true);

		computeSharedComponentName();
		for (DatabaseObject dbo: objectList) {
			scanForVariables((UIComponent)dbo);
		}
	}

	@Override
	public void addPages() {
		try {
			// Page1: component name and options
			page1 = new SharedComponentWizardPage1(this);
			this.addPage(page1);

			// Page2: variable names customization
			if (canCustomizeVariables()) {
				page2 = new SharedComponentWizardPage2(this);
				this.addPage(page2);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			;
		}
	}

	@Override
	public boolean canFinish() {
		return getContainer().getCurrentPage().isPageComplete();
	}

	@Override
	public boolean performFinish() {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};

		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}

	@Override
	public boolean performCancel() {
		newBean = null;
		return super.performCancel();
	}

	protected boolean canCustomizeVariables() {
		return getItemMap().size() > 0;
	}

	protected String getSharedComponentName() {
		return this.shared_comp_name;
	}

	private DatabaseObject getFirstInList() {
		return objectList.get(0);
	}

	private DatabaseObject getLastInList() {
		return objectList.get(sizeOfList() - 1);
	}

	private int sizeOfList() {
		return objectList.size();
	}

	private void computeSharedComponentName() {
		DatabaseObject firstDbo = getFirstInList();
		String dbo_qname = sizeOfList() > 1 ? firstDbo.getParent().getQName() + "_Group" : firstDbo.getQName();
		String app_qname = ((UIComponent)firstDbo).getApplication().getQName();
		dbo_qname = dbo_qname.replace(app_qname+".", "");
		shared_comp_name = StringUtils.normalize(dbo_qname);
	}

	protected boolean sharedComponentAlreadyExists(String sharedComponentName) {
		UIComponent uic = (UIComponent) getFirstInList();
		MobileApplication ma = uic.getProject().getMobileApplication();
		ApplicationComponent app = (ApplicationComponent)ma.getApplicationComponent();
		for (UISharedComponent uisc: app.getSharedComponentList()) {
			if (uisc.getName().equals(sharedComponentName)) {
				return  true;
			}
		}
		return false;
	}

	private boolean isInPage(UIComponent uic) {
		return uic.getPage() != null;
	}

	private boolean isInSharedComponent(UIComponent uic) {
		return uic.getSharedComponent() != null;
	}

	private boolean isInApplication(UIComponent uic) {
		return !isInPage(uic) && !isInSharedComponent(uic);
	}

	private boolean isInControlEvent(UIComponent uic) {
		DatabaseObject databaseObject = uic;
		while (databaseObject != null && !(databaseObject instanceof UIControlEvent)) {
			databaseObject = databaseObject.getParent();
		}
		return databaseObject != null;
	}

	private UICompVariable createCompVariable(String varName, String varValue) throws Exception {
		UICompVariable compVariable = new UICompVariable();
		compVariable.setName(varName);
		compVariable.setVariableValue(varValue);
		compVariable.hasChanged = true;
		compVariable.bNew = true;
		return compVariable;
	}

	private UIControlVariable createControlVariable(String varName, String varValue) throws Exception {
		MobileSmartSourceType var_msst = new MobileSmartSourceType();
		var_msst.setMode(Mode.SCRIPT);
		var_msst.setSmartValue(varValue);

		UIControlVariable controlVariable = new UIControlVariable();
		controlVariable.setName(varName);
		controlVariable.setVarSmartType(var_msst);
		controlVariable.hasChanged = true;
		controlVariable.bNew = true;
		return controlVariable;
	}

	protected Map<String, String> getItemMap() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		if (ignore_callbacks) {
			for (String name: infoMap.keySet()) {
				// add declared variables and directive variables only
				if (main_map.containsKey(name)) {
					map.put(name, infoMap.get(name));
				}
			}
			return Collections.unmodifiableMap(map);
		}
		return Collections.unmodifiableMap(infoMap);
	}

	private boolean forTemplate(UIComponent uic) {
		boolean forTemplate = true;
		if (uic instanceof IAction || uic.getParent() instanceof IAction) {
			if (uic.getUIComponentList().size() > 0) {
				forTemplate = false;
			} else {
				forTemplate = ! (uic.getParent() instanceof IAction);
			}
		}
		return forTemplate;
	}

	private static String escapeString(String s) {
		if (s != null && !s.isEmpty()) {
			StringBuilder b = new StringBuilder();
			boolean doIt = false;
			int len = s.length();
			char c;
			for (int i = 0; i < len; i++) {
				c = s.charAt(i);
				if (c == '\"') {
					b.append("#D#");
					doIt = !doIt;
				} else if (c == '\'' && doIt && (s.charAt(i-1) != '\\')) {
					b.append("\\\\").append(c);
				} else {
					b.append(c);
				}
			}
			return b.toString().replace("#D#", "'");
		}
		return s;
	}

	private void scanForVariables(final UIComponent origin) throws Exception {
		final Set<String> identifierSet = new HashSet<String>();

		try {

			new WalkHelper() {
				private void addDeclaration(String var_name, String var_value) {
					if (var_name != null && !var_name.isEmpty() && !main_map.containsKey(var_name)) {
						main_map.put(var_name, var_value == null ? "''" : var_value);
					}
				}

				private void getMainDeclarations() {
					try {
						List<String> declarations = new ArrayList<String>();
						String c8o_Declarations = "", markerId = "";

						if (isInPage(origin)) {
							markerId = "PageDeclaration";
							String c8o_UserCustoms = origin.getPage().getScriptContent().getString();
							c8o_Declarations = Ionic3Builder.getMarker(c8o_UserCustoms, markerId);
						} else if (isInSharedComponent(origin)) {
							markerId = "SharedCompDeclaration";
							UISharedComponent uisc = origin.getSharedComponent();
							for (UICompVariable var: uisc.getVariables()) {
								c8o_Declarations += "let " + var.getVariableName() + " = " + var.getVariableValue() + ";"+ System.lineSeparator();
							}
						} else if (isInApplication(origin)) {
							markerId = "AppDeclaration";
							String c8o_UserCustoms = origin.getApplication().getComponentScriptContent().getString();
							c8o_Declarations = Ionic3Builder.getMarker(c8o_UserCustoms, markerId);
						}

						if (!c8o_Declarations.isEmpty()) {
							for (String line: Arrays.asList(c8o_Declarations.split(System.lineSeparator()))) {
								line = line.trim();
								if (!line.isEmpty() && line.indexOf(markerId) == -1) {
									declarations.add(line);
								}
							}
						}

						for (String line: declarations) {
							Matcher matcher = d_var.matcher(line);//"(((\\w+)\\s(\\w+)([^\\=]+))(\\=([^\\=]+))?)"
							while (matcher.find()) {
								String var_name = matcher.group(4);

								String var_value = matcher.group(7);
								if (var_value != null) {
									var_value = var_value.trim();
									if (var_value.charAt(var_value.length() - 1) == ';') {
										var_value = var_value.substring(0, var_value.length() - 1);
									}
									var_value = escapeString(var_value);
								}

								addDeclaration(var_name, var_value);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				private boolean isInForDirective(UIComponent uic) {
					return getForDirective(uic) != null;
				}

				private UIControlDirective getForDirective(UIComponent uic) {
					DatabaseObject databaseObject = uic;
					while (databaseObject != null &&
							(!(databaseObject instanceof UIControlDirective) ||
									!AttrDirective.ForEach.name().equals(((UIControlDirective) databaseObject).getDirectiveName()))) {
						databaseObject = databaseObject.getParent();
					}

					if (databaseObject == null)
						return null;
					else
						return (UIControlDirective) databaseObject;
				}

				private void getForDirectiveVariables(UIComponent uic) {
					UIComponent uicomponent = uic;
					while (isInForDirective(uicomponent)) {
						UIControlDirective uicd = getForDirective(uicomponent);
						if (!uic.equals(uicd)) {
							String item = "item"+ uicd.priority;
							addDeclaration(item, "[]");
							addMapVariable(item, item, "this._params_."+item);
							addMapVariable(item, uicd.toString() + " : found  variable which stands for the iterator's item");

							String itemName = uicd.getDirectiveItemName();
							addDeclaration(itemName, "{}");
							addMapVariable(itemName, itemName, "this._params_."+itemName);
							addMapVariable(itemName, uicd.toString() + " : found variable which stands for the customized iterator's item");

							String indexName = uicd.getDirectiveIndexName();
							addDeclaration(indexName, "0");
							addMapVariable(indexName, indexName, "this._params_."+indexName);
							addMapVariable(indexName, uicd.toString() + " : found variable which stands for the customized iterator's index");

							String expression = uicd.getDirectiveExpression();
							if (!expression.isEmpty()) {
								Matcher matcher = null;
								List<String> list = Arrays.asList(expression.split("\\;"));
								for (String s: list) {
									matcher = d_var_let.matcher(s);
									while (matcher.find()) {
										String expvar = matcher.group(3);
										addDeclaration(expvar, "''");
										addMapVariable(expvar, expvar, "this._params_."+expvar);
										addMapVariable(expvar, uicd.toString() + " : found variable used by the customized iterator's expression");
									}
									matcher = d_var_as.matcher(s);
									while (matcher.find()) {
										String expvar = matcher.group(4);
										addDeclaration(expvar, "''");
										addMapVariable(expvar, expvar, "this._params_."+expvar);
										addMapVariable(expvar, uicd.toString() + " : found variable used by the customized iterator's expression");
									}
								}
							}
						}

						DatabaseObject dbo = uicd.getParent();
						uicomponent = dbo instanceof UIComponent ? (UIComponent) dbo : null;
					}
				}

				private boolean checkVariable(String name) {
					if (name == null || name.isEmpty())
						return false;

					if (identifierSet.contains(name)) {
						return false;
					}

					if ("global".equals(name))
						return false;
					if ("router".equals(name))
						return false;

					return true;
				}

				private void addMapVariable(String name, String target, String replacement) {
					if (checkVariable(name)) {
						String normalized_name = StringUtils.normalize(name);
						String var_name = normalized_name;
						if (ovarMap.containsKey(var_name)) {
							//System.out.println("var_name: "+ var_name + " already in ovarMap");
						} else {
							ovarMap.put(var_name, new HashMap<String, String>());
						}

						ovarMap.get(var_name).put(target, replacement.replace("_params_."+name, "_params_.") + var_name);
					}
				}

				private void addMapVariable(String name, String infos) {
					if (checkVariable(name)) {
						String normalized_name = StringUtils.normalize(name);
						String var_name = normalized_name;
						if (infoMap.containsKey(var_name)) {
							//System.out.println("var_name: "+ var_name + " already in infoMap");
						} else {
							infoMap.put(var_name, infos);
						}
					}
				}

				private void scanSmartSource(UIComponent uic, String p_name, MobileSmartSourceType msst) throws Exception {
					boolean extended = !forTemplate(uic);

					String s = null;
					if (Mode.SCRIPT.equals(msst.getMode())) {
						s = msst.getValue(extended);
					}
					if (Mode.SOURCE.equals(msst.getMode())) {
						s = msst.getSmartSource().toJsonString();
					}

					if (s != null) {
						String infos = uic.toString() + " : found variable used by '"+ p_name +"' property";

						Matcher matcher = p_var.matcher(s);
						while (matcher.find()) {
							String group1 = matcher.group(1);
							String group2 = matcher.group(2);
							//String group3 = matcher.group(3);
							String group4 = matcher.group(4);

							String name = group4;
							String target = group1;
							String replacement = group2 +"._params_." + name;

							if (isInControlEvent(uic)) {
								if (forTemplate(uic)) {
									replacement = "_params_." + name;
								} else {
									replacement = "scope._params_." + name;
								}
							}

							addMapVariable(name, target, replacement);
							addMapVariable(name, infos);
						}
					}
				}


				@Override
				public void init(DatabaseObject databaseObject) throws Exception {
					getMainDeclarations();

					if (isInForDirective(origin)) {
						getForDirectiveVariables(origin);
					}

					super.init(databaseObject);
				}

				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {
					if (databaseObject instanceof UIComponent) {
						UIComponent uic = (UIComponent) databaseObject;

						if (uic.isEnabled() && !isInControlEvent(uic)) {
							if (databaseObject instanceof UIDynamicElement) {
								String identifier = ((UIDynamicElement)databaseObject).getIdentifier();
								if (!identifier.isEmpty()) {
									identifierSet.add(identifier);
								}
							}

							for (java.beans.PropertyDescriptor pd: CachedIntrospector.getBeanInfo(databaseObject).getPropertyDescriptors()) {
								if (pd.getPropertyEditorClass() != null) {
									if (pd.getPropertyEditorClass().getSimpleName().equals("MobileSmartSourcePropertyDescriptor")) {
										Method getter = pd.getReadMethod();
										Object value = getter.invoke(databaseObject, new Object[] {});
										if (value != null && value instanceof MobileSmartSourceType) {
											MobileSmartSourceType msst = (MobileSmartSourceType)value;
											if (Mode.SCRIPT.equals(msst.getMode()) || Mode.SOURCE.equals(msst.getMode())) {
												scanSmartSource(uic, pd.getName(), msst);
											}
										}
									}
								}
							}

							if (databaseObject instanceof UIDynamicElement) {
								UIDynamicElement uide = (UIDynamicElement)databaseObject;
								IonBean ionBean = uide.getIonBean();
								if (ionBean != null) {
									for (IonProperty property : ionBean.getProperties().values()) {
										Object p_value = property.getValue();
										if (!p_value.equals(false)) {
											MobileSmartSourceType msst = property.getSmartType();
											if (Mode.SCRIPT.equals(msst.getMode()) || Mode.SOURCE.equals(msst.getMode())) {
												scanSmartSource(uide, property.getName(), msst);
											}
										}
									}
								}
							}

							super.walk(databaseObject);
						}
					}
				}
			}.init(origin);
		} catch (Exception e) {
			throw new Exception("Unable to scan for variables", e);
		}
	}

	private void updateMobileSmartSources(final UISharedComponent uisc) throws Exception {
		final String priority = "" + uisc.priority;

		try {
			new WalkHelper() {
				private boolean checkVariable(String name) {
					if (name == null || name.isEmpty())
						return false;
					return dlg_map.get(name) != null;
				}

				private MobileSmartSourceType updateMobileSmartSourceType(boolean forTemplate, MobileSmartSourceType msst) throws Exception {
					boolean extended = !forTemplate;

					if (Mode.SCRIPT.equals(msst.getMode())) {
						String smart_value = msst.getValue(extended);
						for (String name: ovarMap.keySet()) {
							if (checkVariable(name)) {
								Map<String, String> m = ovarMap.get(name);
								for (String target: m.keySet()) {
									String replacement = m.get(target).replace("_params_."+ name, "_params_."+ dlg_map.get(name));
									replacement = replacement.replace("_params_", "params"+priority);
									smart_value = smart_value.replace(target, replacement);
								}
							}
						}

						MobileSmartSourceType new_msst = new MobileSmartSourceType();
						new_msst.setMode(Mode.SCRIPT);
						new_msst.setSmartValue(smart_value);
						return new_msst;
					}

					if (Mode.SOURCE.equals(msst.getMode())) {
						MobileSmartSource mss = msst.getSmartSource();
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
								boolean found = false;
								for (SourceData data : dataList) {
									for (String name: ovarMap.keySet()) {
										if (checkVariable(name)) {
											Map<String, String> m = ovarMap.get(name);
											for (String target: m.keySet()) {
												if (target.equals(data.getValue())) {
													if (!found) {
														found = true;
														SourceData shared = Filter.Shared.toSourceData(mss.getProjectName(), "params"+priority);
														mew_model.addSourceData(shared);
														mew_model.setPath("?." + dlg_map.get(name));
														mew_model.setSuffix(model.getPath() + (model.getSuffix().isEmpty() ? "":" ") + model.getSuffix());
													}
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
						for (String name: ovarMap.keySet()) {
							if (checkVariable(name)) {
								Map<String, String> m = ovarMap.get(name);
								for (String target: m.keySet()) {
									String replacement = m.get(target).replace("_params_."+ name, "_params_."+ dlg_map.get(name));
									replacement = replacement.replace("_params_", "params"+priority);
									mew_model.setPrefix(model.getPrefix().replace(target, replacement));
									mew_model.setSuffix(model.getSuffix().replace(target, replacement));
									mew_model.setCustom(model.getCustom().replace(target, replacement));
								}
							}
						}

						MobileSmartSourceType new_msst = new MobileSmartSourceType();
						new_msst.setMode(Mode.SOURCE);
						new_msst.setSmartValue(new_mss.toJsonString());
						return new_msst;
					}

					return msst;
				}


				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {
					if (databaseObject instanceof UIComponent) {
						UIComponent uic = (UIComponent) databaseObject;

						if (uic.isEnabled() && !isInControlEvent(uic)) {
							boolean forTemplate = forTemplate(uic);

							for (java.beans.PropertyDescriptor pd: CachedIntrospector.getBeanInfo(databaseObject).getPropertyDescriptors()) {
								if (pd.getPropertyEditorClass() != null) {
									if (pd.getPropertyEditorClass().getSimpleName().equals("MobileSmartSourcePropertyDescriptor")) {
										Method getter = pd.getReadMethod();
										Method setter = pd.getWriteMethod();
										Object value = getter.invoke(databaseObject, new Object[] {});
										if (value != null && value instanceof MobileSmartSourceType) {
											MobileSmartSourceType msst = (MobileSmartSourceType)value;
											if (Mode.SCRIPT.equals(msst.getMode()) || Mode.SOURCE.equals(msst.getMode())) {
												setter.invoke(databaseObject, new Object[] {updateMobileSmartSourceType(forTemplate, msst)});
											}
										}
									}
								}
							}

							if (databaseObject instanceof UIDynamicElement) {
								UIDynamicElement uide = (UIDynamicElement)databaseObject;
								IonBean ionBean = uide.getIonBean();
								if (ionBean != null) {
									for (IonProperty property : ionBean.getProperties().values()) {
										String p_name = property.getName();
										Object p_value = property.getValue();
										if (!p_value.equals(false)) {
											MobileSmartSourceType msst = property.getSmartType();
											if (Mode.SCRIPT.equals(msst.getMode()) || Mode.SOURCE.equals(msst.getMode())) {
												ionBean.setPropertyValue(p_name, updateMobileSmartSourceType(forTemplate, msst));
											}
										}
									}
								}
							}

							super.walk(databaseObject);
						}
					}
				}
			}.init(uisc);
		} catch (Exception e) {
			throw new Exception("Unable to update mobile smart sources", e);
		}
	}

	private String getVariablesDefaultValue(String var_name) {
		String var_value = null;
		if (var_name != null) {
			var_value = main_map.get(var_name);
		}
		return var_value == null ? "''" : var_value;
	}

	private UISharedComponent createSharedComponent() throws Exception {
		UIComponent uic = (UIComponent) getFirstInList();

		UISharedComponent uisc = new UISharedComponent();
		uisc.setName(shared_comp_name);
		uisc.hasChanged = true;
		uisc.bNew = true;
		uic.getApplication().add(uisc); // must be added before copy/paste !

		for (DatabaseObject dbo: objectList) {
			ConvertigoPlugin.clipboardManagerSystem.reset();
			ConvertigoPlugin.clipboardManagerSystem.isCopy = true;
			String sXml = ConvertigoPlugin.clipboardManagerSystem.copy(dbo);
			ConvertigoPlugin.clipboardManagerSystem.paste(sXml, uisc, false);
		}

		updateMobileSmartSources(uisc);

		for (String name: dlg_map.keySet()) {
			String value = getVariablesDefaultValue(name);
			uisc.add(createCompVariable(dlg_map.get(name), value));
		}
		return uisc;
	}

	private UIUseShared createUseShared(String qname) throws Exception {
		UIUseShared uius = new UIUseShared();
		uius.setSharedComponentQName(qname);
		uius.hasChanged = true;
		uius.bNew = true;

		for (String name: dlg_map.keySet()) {
			String value = ovarMap.get(name).keySet().iterator().next();
			uius.add(createControlVariable(dlg_map.get(name), value));
		}

		UIComponent uic = (UIComponent) getLastInList();
		MobileComponent mc = (MobileComponent) uic.getParent();
		if (mc instanceof ApplicationComponent) {
			ApplicationComponent parent = (ApplicationComponent)mc;
			parent.add((DatabaseObject)uius, uic.priority);
		} else if (mc instanceof PageComponent) {
			PageComponent parent = (PageComponent)mc;
			parent.add((DatabaseObject)uius, uic.priority);
		} else if (mc instanceof UIComponent) {
			UIComponent parent = (UIComponent)mc;
			parent.add((DatabaseObject)uius, uic.priority);
		}
		return uius;
	}

	private void doFinish(IProgressMonitor monitor) throws CoreException {
		UISharedComponent uisc = null;
		UIUseShared uius = null;

		try {
			if (page1 != null) {
				shared_comp_name = page1.getSharedComponentName();
				keep_original = page1.keepComponent();
			}

			if (page2 != null) {
				dlg_map = page2.getVariableMap();
			}

			// Create shared component
			uisc = createSharedComponent();
			monitor.setTaskName("SharedComponent created");
			monitor.worked(1);

			// Create UseShared
			uius = createUseShared(uisc.getQName());
			monitor.setTaskName("UseShared component created");
			monitor.worked(1);

			// Disable or Remove selected databaseObject(s)
			for (DatabaseObject dbo: objectList) {
				UIComponent uic = (UIComponent)dbo;
				if (keep_original) {
					uic.setEnabled(false);
					uic.hasChanged = true;
				} else {
					MobileComponent mc = (MobileComponent) uic.getParent();
					if (mc instanceof ApplicationComponent) {
						ApplicationComponent parent = (ApplicationComponent)mc;
						parent.remove(uic);
						parent.hasChanged = true;
					} else if (mc instanceof PageComponent) {
						PageComponent parent = (PageComponent)mc;
						parent.remove(uic);
						parent.hasChanged = true;
					} else if (mc instanceof UIComponent) {
						UIComponent parent = (UIComponent)mc;
						parent.remove(uic);
						parent.hasChanged = true;
					}
				}
			}

			// Set newBean to new shared component
			newBean = uisc;
		}
		catch (Exception e) {
			try {
				if (uisc != null) {
					uisc.getParent().remove(uisc);
				}
				if (uius != null) {
					uius.getParent().remove(uius);
				}
			} catch (Exception ex) {}

			String message = "Unable to create a new object from class '"+ this.className +"'.";
			ConvertigoPlugin.logException(e, message);
			newBean = null;
		}
	}
}
