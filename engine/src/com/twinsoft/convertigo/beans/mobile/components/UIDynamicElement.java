/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.beans.mobile.components;

import java.beans.BeanInfo;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StrSubstitutor;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.IDynamicBean;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonEvent;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonProperty;
import com.twinsoft.convertigo.engine.EngineException;

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
		loadBean();
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
			ionBean = ComponentManager.loadBean(beanData);
		}
	}
	
	protected void saveBean() {
		if (ionBean != null) {
			beanData = ionBean.toBeanData();
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
	public String getDynamicIconName(int iconType) {
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
	
	@Override
	public String toString() {
		String id = getIdentifier();
		return getName() + (id.isEmpty() ? "":" #"+id);
	}
	
	protected static boolean isComposedValue(String val) {
		Pattern pattern = Pattern.compile("\\$\\[(.*)\\]");
		Matcher matcher = pattern.matcher(val);
		return matcher.find();
	}
	
	@Override
	protected StringBuilder initAttributes() {
		StringBuilder attributes = super.initAttributes();

		IonBean ionBean = getIonBean();
		
    	if (ionBean != null) {
    		String formControlVarName = getFormControlName();

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
			
			StrSubstitutor sub = new StrSubstitutor(vm,"$[","]");
			sub.setEnableSubstitutionInVariables(true);
			
			for (IonProperty property : ionBean.getProperties().values()) {
				String name = property.getName();
				String attr = property.getAttr();
				Object value = property.getValue();
				boolean isComposite = property.isComposite();
				
				// case value is set
				if (!value.equals(false)) {
					if (name.equals("AutoDisable")) {
						UIForm form = getUIForm();
						if (form != null) {
							String formGroupName = form.getFormGroupName();
							attributes.append(" [disabled]=\"!").append(formGroupName).append(".valid\"");
						}
					} else {
						if (name.equals("FormControlName") && formControlVarName.isEmpty()) {
							continue;
						}
						if (attr.equals("[ngModel]")) {
							String tagName = getTagName();
							if (tagName.equals("ion-checkbox") || tagName.equals("ion-toggle")) {
								attr = formControlVarName.isEmpty() ? "[checked]":"[ngModel]";
							} else if (tagName.equals("ion-datetime")) {
								attr = formControlVarName.isEmpty() ? "value":"[ngModel]";
							}
						}
						
						String smartValue = property.getSmartValue();
						smartValue = sub.replace(smartValue);
						
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
						if (attr.equals("[ngModel]")) {
							String tagName = getTagName();
							if (formControlVarName.isEmpty() 
									|| tagName.equals("ion-checkbox") || tagName.equals("ion-toggle")) {
								continue;
							} else {
								attributes.append(" [ngModel]=\"null\"");
							}
						}
					}
				}
			}
    	}
		return attributes;
	}

	@Override
	protected String getFormControlName() {
		IonBean ionBean = getIonBean();
		if (ionBean != null && ionBean.hasProperty("FormControlName")) {
			MobileSmartSourceType msst = (MobileSmartSourceType) ionBean.getPropertyValue("FormControlName");
			if (msst != null && !msst.getValue().equals("not set") && !msst.getValue().isEmpty()) {
				return msst.getValue();
			}
		}
		return super.getFormControlName();
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
		IonBean ionBean = getIonBean();
		if (ionBean != null) {
			return ionBean.getTplVersion();
		}
		return super.getRequiredTplVersion();
	}

	@Override
	protected Contributor getContributor() {
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
			public Map<String, String> getModuleTsImports() {
				Map<String, String> imports = new HashMap<String, String>();
				IonBean ionBean = getIonBean();
				if (ionBean != null) {
					Map<String, List<String>> map = ionBean.getConfig().getModuleTsImports();
					if (map.size() > 0) {
						for (String from : map.keySet()) {
							for (String component: map.get(from)) {
								imports.put(component.trim(), from);
							}
						}
					}
				}
				return imports;
			}

			@Override
			public Set<String> getModuleNgImports() {
				IonBean ionBean = getIonBean();
				if (ionBean != null) {
					return ionBean.getConfig().getModuleNgImports();
				}
				return new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgProviders() {
				IonBean ionBean = getIonBean();
				if (ionBean != null) {
					return ionBean.getConfig().getModuleNgProviders();
				}
				return new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgDeclarations() {
				IonBean ionBean = getIonBean();
				if (ionBean != null) {
					return ionBean.getConfig().getModuleNgDeclarations();
				}
				return new HashSet<String>();
			}
			
			@Override
			public Set<String> getModuleNgComponents() {
				IonBean ionBean = getIonBean();
				if (ionBean != null) {
					return ionBean.getConfig().getModuleNgComponents();
				}
				return new HashSet<String>();
			}

			@Override
			public Map<String, String> getPackageDependencies() {
				IonBean ionBean = getIonBean();
				if (ionBean != null) {
					return ionBean.getConfig().getPackageDependencies();
				}
				return new HashMap<String, String>();
			}

			@Override
			public Map<String, String> getConfigPlugins() {
				IonBean ionBean = getIonBean();
				if (ionBean != null) {
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

		};
	}
	
}
