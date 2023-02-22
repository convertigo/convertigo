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

package com.twinsoft.convertigo.beans.ngx.components.dynamic;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IApplicationComponent;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.IAction;
import com.twinsoft.convertigo.beans.ngx.components.MobileComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionCaseDefaultEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionCaseEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionElseEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionErrorEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionFailureEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionFinallyEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionLoopEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UIAnimation;
import com.twinsoft.convertigo.beans.ngx.components.UIAppEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIAttribute;
import com.twinsoft.convertigo.beans.ngx.components.UICompVariable;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UICompEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlDirective;
import com.twinsoft.convertigo.beans.ngx.components.UIControlEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlVariable;
import com.twinsoft.convertigo.beans.ngx.components.UICustom;
import com.twinsoft.convertigo.beans.ngx.components.UICustomAction;
import com.twinsoft.convertigo.beans.ngx.components.UICustomAsyncAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicAttr;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicEmit;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicIf;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicIterate;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenu;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenuItem;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicSwitch;
import com.twinsoft.convertigo.beans.ngx.components.UIElement;
import com.twinsoft.convertigo.beans.ngx.components.UIEventSubscriber;
import com.twinsoft.convertigo.beans.ngx.components.UIFont;
import com.twinsoft.convertigo.beans.ngx.components.UIFontStyle;
import com.twinsoft.convertigo.beans.ngx.components.UIForm;
import com.twinsoft.convertigo.beans.ngx.components.UIAppGuard;
import com.twinsoft.convertigo.beans.ngx.components.UIPageEvent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponentEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIStackVariable;
import com.twinsoft.convertigo.beans.ngx.components.UIStyle;
import com.twinsoft.convertigo.beans.ngx.components.UIText;
import com.twinsoft.convertigo.beans.ngx.components.UITheme;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.beans.ngx.components.UIUseVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;
import com.twinsoft.convertigo.engine.util.URLUtils;
import com.twinsoft.convertigo.engine.util.WeakValueHashMap;

public class ComponentManager {
	private static ComponentManager instance = new ComponentManager();
	
	private SortedMap<String, IonProperty> pCache = new TreeMap<>();
	private SortedMap<String, IonBean> bCache = new TreeMap<>();
	private Map<String, String> aCache = new WeakValueHashMap<>();
	
	private Map<String, List<Component>> map = new TreeMap<>();
	private Map<String, JSONObject> c8oBeans = new HashMap<String, JSONObject>();
	private Map<String, JSONObject> fontMap = new HashMap<String, JSONObject>();
	
	private List<String> groups;
	private List<Component> orderedComponents;
	private List<Component> components;
	
	private File compbeansDir;
	
	private ComponentManager() {
		loadModels();
		loadFonts();
	}
	
	private synchronized void loadModels() {
		clearModels();
		
		if (Engine.isStarted) {
			Engine.logEngine.info("(ComponentManager) Start loading Ionic objects");
		} else {
			System.out.println("(ComponentManager) Start loading Ionic objects");
		}
		
		try (InputStream inputstream = getClass().getResourceAsStream("ion_objects.json")) {
			String json = IOUtils.toString(inputstream, "UTF-8");
			
			JSONObject root = new JSONObject(json);
			readPropertyModels(root);
			readBeanModels(root);
			readC8oBeanModels(root);
			
			if (Engine.isStarted) {
				Engine.logEngine.info("(ComponentManager) End loading Ionic objects");
			} else {
				System.out.println("(ComponentManager) End loading Ionic objects");
			}
		} catch (Exception e) {
			if (Engine.isStarted) {
				Engine.logEngine.error("(ComponentManager) Could not load Ionic objects", e);
			} else {
				System.out.println("(ComponentManager) Could not load Ionic objects:");
				e.printStackTrace();
			}
		}
	}
	
	private void clear() {
		clearModels();
		clearFonts();
	}
	
	private void clearFonts() {
		fontMap.clear();		
	}
	
	private void clearModels() {
		pCache.clear();
		bCache.clear();
		aCache.clear();
		map.clear();
		c8oBeans.clear();
		
		groups = null;
		orderedComponents = null;
		components = null;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void finalize() throws Throwable {
		clear();
		super.finalize();
	}
	
	private void readPropertyModels(JSONObject root) {
		try {
			JSONObject props = root.getJSONObject("Props");
			@SuppressWarnings("unchecked")
			Iterator<String> it = props.keys();
			while (it.hasNext()) {
				String key = it.next();
				if (!key.isEmpty()) {
					IonProperty property = new IonProperty(props.getJSONObject(key));
					property.setName(key);
					pCache.put(key, property);
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void readBeanModels(JSONObject root) {
		try {
			JSONObject beans = root.getJSONObject("Beans");
			@SuppressWarnings("unchecked")
			Iterator<String> it = beans.keys();
			while (it.hasNext()) {
				String key = it.next();
				if (!key.isEmpty()) {
					JSONObject jsonObject = beans.getJSONObject(key);
					
					if (!jsonObject.has("enabled") || Boolean.TRUE.equals(jsonObject.remove("enabled"))) {					
						JSONObject jsonProperties = (JSONObject) jsonObject.remove("properties");
						
						IonBean bean = new IonBean(jsonObject.toString());
						bean.setName(key);
						if (jsonProperties != null) {
							@SuppressWarnings("unchecked")
							Iterator<String> itp = jsonProperties.keys();
							while (itp.hasNext()) {
								String pkey = itp.next();
								if (!pkey.isEmpty()) {
									Object value = jsonProperties.get(pkey);
									// This is a bean property (available for this bean only)
									if (value instanceof JSONObject) {
										IonProperty property = new IonProperty((JSONObject) value);
										property.setName(pkey);
										bean.putProperty(property);
									}
									else {
										// This is model property (available for all beans)
										final IonProperty original = pCache.get(pkey);
										if (original != null) {
											String jsonString = original.getJSONObject().toString();
											IonProperty property = new IonProperty(new JSONObject(jsonString));
											property.setValue(value);
											bean.putProperty(property);
										} else {
											System.out.println("(ComponentManager) Ion property \""+pkey+"\" does not exist anymore in cache.");
											if (Engine.isStarted) {
												Engine.logEngine.warn("(ComponentManager) Ion property \""+pkey+"\" does not exist anymore in cache.");
											}
										}
									}
								}
							}
						}
						bCache.put(key, bean);
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void readC8oBeanModels(JSONObject root) {
		try {
			JSONObject beans = root.getJSONObject("C8oBeans");
			@SuppressWarnings("unchecked")
			Iterator<String> it = beans.keys();
			while (it.hasNext()) {
				String key = it.next();
				if (!key.isEmpty()) {
					c8oBeans.put(key, beans.getJSONObject(key));
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public static Map<String, JSONObject> getC8oBeans() {
		return Collections.unmodifiableMap(instance.c8oBeans);
	}
	
	public static IonBean loadBean(String jsonString) throws Exception {
		JSONObject jsonBean = new JSONObject(jsonString);
		String modelName = "Unknown";
		if (jsonBean.has(IonBean.Key.name.name())) {
			modelName = jsonBean.getString(IonBean.Key.name.name());
		}
		final IonBean model = instance.bCache.get(modelName);
		// The model exists
		if (model != null) {
			boolean hasChanged = false;
			IonBean dboBean = new IonBean(jsonString);
			
			IonBean ionBean = new IonBean(model.toString());
			for (IonProperty ionProperty: ionBean.getProperties().values()) {
				String propertyName = ionProperty.getName(); 
				IonProperty dboProperty = dboBean.getProperty(propertyName);
				if (dboProperty != null) {
					MobileSmartSourceType msst = dboProperty.getSmartType();
					if (msst != null) {
						ionProperty.setSmartType(msst);
						ionBean.putProperty(ionProperty);
					}
				}
				else {
					System.out.println("(ComponentManager) Ion property \""+propertyName+"\" not found for model \""+modelName+"\": ignore it.");
					if (Engine.isStarted) {
						Engine.logBeans.warn("(ComponentManager) Ion property \""+propertyName+"\" not found for model \""+modelName+"\": ignore it.");
					}
					hasChanged = true;
				}
			}
			if (hasChanged) {
				//TODO
			}
			return ionBean;
		}
		// The model doesn't exist (anymore)
		else {
			System.out.println("(ComponentManager) Model \""+modelName+"\" does not exist anymore in cache ("+jsonString+").");
			if (Engine.isStarted) {
				Engine.logBeans.warn("(ComponentManager) Model \""+modelName+"\" does not exist anymore in cache ("+jsonString+").");
			}
			return new IonBean(jsonString);
		}
	}
	
	public static DatabaseObject createBean(Component c) {
		return c != null ? c.createBean():null;
	}
	
	public static DatabaseObject createBeanFromHint(Component c) {
		DatabaseObject dbo = null;
		try {
			dbo = createBeanFromHint(c.getHint());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dbo == null ? c.createBean() : dbo;
	}
	
	private static DatabaseObject createBeanFromHint(JSONObject jsonHint) {
		try {
			@SuppressWarnings("unchecked")
			Iterator<String> it = jsonHint.keys();
			if (it.hasNext()) {
				String key = it.next();
				Component c = getComponentByName(key);
				if (c != null && jsonHint.has(key)) {
					return createBeanFromJson(c, jsonHint.getJSONObject(key));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static DatabaseObject createBeanFromJson(Component c, JSONObject jsonObject) {
		DatabaseObject dbo = c.createBean();
		
		try {
			JSONObject jsonProperties = (JSONObject) jsonObject.remove("properties");
			if (jsonProperties != null) {
				@SuppressWarnings("unchecked")
				Iterator<String> it = jsonProperties.keys();
				while (it.hasNext()) {
					String pname = it.next();
					
					try {
						String value = jsonProperties.getString(pname);
						
						MobileSmartSourceType msst = new MobileSmartSourceType(value);
						if (value.startsWith("TS=")) {
							msst = new MobileSmartSourceType();
							msst.setMode(Mode.SCRIPT);
							msst.setSmartValue(value.replace("TS=", ""));
						}
						
						BeanInfo beanInfo = Introspector.getBeanInfo(dbo.getClass());
						PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
						for (PropertyDescriptor pd: propertyDescriptors) {
							Method setter = pd.getWriteMethod();
							if (pd.getName().equals(pname)) {
								Class<?> pdc = pd.getPropertyEditorClass();
								if (pdc != null && pdc.getSimpleName().equals("NgxSmartSourcePropertyDescriptor")) {
									setter.invoke(dbo, new Object[] { msst });
								} else if (pname.equals("actionValue")) {// CustomAction
									FormatedContent fc = new FormatedContent(value);
									setter.invoke(dbo, new Object[] { fc });
								} else {
									setter.invoke(dbo, new Object[] { value });
								}
							}
						}
						
						if (dbo instanceof UIDynamicElement) {
							IonBean ionBean = ((UIDynamicElement)dbo).getIonBean();
							if (ionBean != null) {
								ionBean.setPropertyValue(pname, msst);
							}
						}
						
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			@SuppressWarnings("unchecked")
			Iterator<String> it = jsonObject.keys();
			while (it.hasNext()) {
				String key = it.next();
				Object ob = jsonObject.get(key);
				Component child = getComponentByName(key);
				if (child != null) {
					if (ob instanceof JSONArray) {
						JSONArray jsonArray = (JSONArray)ob;
						for (int i = 0; i < jsonArray.length(); i++) {
							Object aOb = jsonArray.get(i);
							if (aOb instanceof JSONObject) {
								dbo.add(createBeanFromJson(child, (JSONObject)aOb));
							} else {
								dbo.add(createBeanFromJson(child, new JSONObject()));
							}
						}
					} else if (ob instanceof JSONObject) {
						dbo.add(createBeanFromJson(child, (JSONObject)ob));
					} else {
						dbo.add(createBeanFromJson(child, new JSONObject()));
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dbo;
	}
	
	public static void reloadModels() {
		instance.loadModels();
	}

	public static void reloadComponents() {
		instance.groups = null;
		instance.orderedComponents = null;
		instance.components = null;
		
		instance.makeGroups();
		instance.makeComponentsByGroup();
	}

	public static List<String> getGroups() {
		return instance.makeGroups();
	}
	
	private static String GROUP_SHARED_ACTIONS = "Shared Actions";
	private static String GROUP_SHARED_COMPONENTS = "Shared Components";
	private static String GROUP_CUSTOMS = "Customs";
	private static String GROUP_CONTROLS = "Controls";
	private static String GROUP_ACTIONS = "Actions";
	
	private synchronized List<String> makeGroups() {
		if (groups != null) {
			return groups;
		}
		groups = new ArrayList<String>(10);
		groups.add(GROUP_CUSTOMS);
		for (final IonBean bean: instance.bCache.values()) {
			if (!groups.contains(bean.getGroup())) {
				groups.add(bean.getGroup());
			}
		}
		
		groups.remove(GROUP_SHARED_COMPONENTS);
		groups.add(GROUP_SHARED_COMPONENTS);
		
		groups.remove(GROUP_SHARED_ACTIONS);
		groups.add(GROUP_SHARED_ACTIONS);
		
		groups.remove(GROUP_CONTROLS);
		groups.add(GROUP_CONTROLS);
		
		groups.remove(GROUP_ACTIONS);
		groups.add(GROUP_ACTIONS);
		
		makeComponents();
		for (String group: map.keySet()) {
			if (!groups.contains(group)) {
				groups.add(group);
			}
		}
		
		return groups = Collections.unmodifiableList(groups);
	}
	
	public static List<Component> getComponentsByGroup() {
		return instance.makeComponentsByGroup();
	}
	
	private synchronized List<Component> makeComponentsByGroup() {
		if (orderedComponents != null) {
			return orderedComponents;
		}
		orderedComponents = new ArrayList<Component>(10);
		List<Component> components = getComponents();
		
		for (String group : getGroups()) {
			for (Component component : components) {
				if (component.getGroup().equals(group)) {
					orderedComponents.add(component);
				}
			}
		}
		
		return orderedComponents = Collections.unmodifiableList(orderedComponents);
	}
	
	public static List<Component> getComponents() {
		return instance.makeComponents();
	}
	
	private synchronized List<Component> makeComponents() {
		if (components != null) {
			return components;
		}
		components = new ArrayList<Component>(10);
		
		try {
			/*-------------------------- BUILTINS --------------------------*/
			String group;
			// Add Customs
			group = GROUP_CUSTOMS;
			components.add(getDboComponent(UIElement.class,group));
			components.add(getDboComponent(UIAttribute.class,group));
			components.add(getDboComponent(UIAnimation.class,group));
			components.add(getDboComponent(UICustom.class,group));
			components.add(getDboComponent(UIText.class,group));
			components.add(getDboComponent(UIStyle.class,group));
			components.add(getDboComponent(UITheme.class,group));
			components.add(getDboComponent(UIFont.class,group));
			components.add(getDboComponent(UIFontStyle.class,group));
			
			// Add shared components
			group = GROUP_SHARED_COMPONENTS;
			components.add(getDboComponent(UISharedRegularComponent.class,group));
			components.add(getDboComponent(UIUseShared.class,group));
			components.add(getDboComponent(UICompVariable.class,group));
			components.add(getDboComponent(UIUseVariable.class,group));
			components.add(getDboComponent(UICompEvent.class,group));
			
			// Add shared actions
			group = GROUP_SHARED_ACTIONS;
			components.add(getDboComponent(UIActionStack.class,group));
			components.add(getDboComponent(UIStackVariable.class,group));
			
			// Add Controls
			group = GROUP_CONTROLS;
			components.add(getDboComponent(UIControlEvent.class,group));
			components.add(getDboComponent(UIAppEvent.class,group));
			components.add(getDboComponent(UIPageEvent.class,group));
			components.add(getDboComponent(UISharedComponentEvent.class,group));
			components.add(getDboComponent(UIAppGuard.class,group));
			components.add(getDboComponent(UIEventSubscriber.class,group));
			components.add(getDboComponent(UIActionErrorEvent.class,group));
			components.add(getDboComponent(UIActionFailureEvent.class,group));
			components.add(getDboComponent(UIActionFinallyEvent.class,group));
			components.add(getDboComponent(UIActionLoopEvent.class,group));
			components.add(getDboComponent(UIActionCaseEvent.class,group));
			components.add(getDboComponent(UIActionCaseDefaultEvent.class,group));
			components.add(getDboComponent(UIActionElseEvent.class,group));
			components.add(getDboComponent(UIControlDirective.class,group));
			
			// Add Actions
			group = GROUP_ACTIONS;
			components.add(getDboComponent(UIControlVariable.class,group));
			components.add(getDboComponent(UICustomAction.class,group));
			components.add(getDboComponent(UICustomAsyncAction.class,group));
			
			components.add(getDboComponent(UIForm.class,"Forms"));
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		for (final IonBean bean: instance.bCache.values()) {
			components.add(new Component() {
				
				@Override
				public boolean isAllowedIn(DatabaseObject parent) {
					if (bean.getTag().equals("ion-menu")) {
						return parent instanceof ApplicationComponent;
					}
					
					if (bean.getClassName().startsWith("com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenuItem")) {
						if (parent instanceof UIComponent) {
							if (parent instanceof UIDynamicMenuItem) return false;
							UIDynamicMenu menu = ((UIComponent)parent).getMenu();
							return menu != null;
						}
					}
					
					Class<?> dboClass;
					try {
						dboClass = Class.forName(bean.getClassName());
						if (acceptDatabaseObjects(parent, dboClass)) {
							return isTplCompatible(parent, createBean());
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					return false;
				}
				
				@Override
				public String getLabel() {
					return bean.getLabel();
				}
				
				@Override
				public String getImagePath() {
					return bean.getIconColor32Path();
				}
				
				@Override
				public String getGroup() {
					return bean.getGroup();
				}
				
				@Override
				public String getDescription() {
					return bean.getDescription();
				}
				
				@Override
				public String getName() {
					return bean.getName();
				}
				
				@Override
				public String getTag() {
					return bean.getTag();
				}
				
				@Override
				public String getPropertiesDescription() {
					String propertiesDescription = "";
					List<IonProperty> properties = new ArrayList<IonProperty>();
					properties.addAll(bean.getProperties().values());
					
					Collections.sort(properties, new Comparator<IonProperty>() {
						@Override
						public int compare(IonProperty p1, IonProperty p2) {
							return p1.getLabel().compareTo(p2.getLabel());
						}				
					} );
					
					for (IonProperty ionProperty: properties) {
						if (!ionProperty.isHidden()) {
							propertiesDescription += "<li><i>"+ ionProperty.getLabel() +"</i>" ;
							propertiesDescription += "</br>"+ ionProperty.getDescription() +"</li>";
						}
					}
					
					Class<?> dboClass;
					try {
						dboClass = Class.forName(bean.getClassName());
						BeanInfo beanInfo = Introspector.getBeanInfo(dboClass);
						PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
						
						propertyDescriptors = propertyDescriptors.clone();
						Arrays.sort(propertyDescriptors, (o1, o2) -> {
							if(o1.isExpert() == o2.isExpert()) {
								return o1.getDisplayName().compareTo(o2.getDisplayName());
							} else if(o1.isExpert()) {
								return 1;
							} else { 
								return -1;
							}
						});
						
						for (PropertyDescriptor dbopd : propertyDescriptors) {
							if (!dbopd.isHidden() && !Boolean.TRUE.equals(dbopd.getValue(MySimpleBeanInfo.DISABLE))) {
								propertiesDescription += "<li><i>"+ dbopd.getDisplayName() +"</i>" ;
								propertiesDescription += "</br>"+ dbopd.getShortDescription().replace("|", "") +"</li>";
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					return propertiesDescription.isEmpty() ? "": "<ul>"+propertiesDescription+"</ul>";
				}
				
				@Override
				protected DatabaseObject createBean() {
					DatabaseObject dbo = bean.createBean();
					return dbo;
				}

				@Override
				protected JSONObject getHint() {
					return bean.getHint();
				}

				@Override
				public boolean isBuiltIn() {
					return true;
				}

			});
		}
		
		/*-------------------------- ADDITIONALS --------------------------*/
		if (Engine.isStarted) {
			try {
				List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
				for (String projectName : projectNames) {
					if (!Engine.theApp.databaseObjectsManager.existsProject(projectName)) {
						continue;
					}
					Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
					String readmeUrl = ProjectUrlParser.getReadmeUrl(project);
					if (project.getMobileApplication() != null) {
						IApplicationComponent ac = project.getMobileApplication().getApplicationComponent();
						if (ac != null && ac instanceof ApplicationComponent) {
							ApplicationComponent app = (ApplicationComponent)ac;
							for (UIActionStack action: app.getSharedActionList()) {
								if (action.isEnabled() && action.isExposed()) {
									components.add(new Component() {
	
										@Override
										public String getDescription() {
											String description = action.getComment();
											if (description.isEmpty()) {
												description = "A "+ action.getName() + " action.";
											}
											if (description.indexOf(" | ") == -1) {
												description += " | ";
											}
											if (!readmeUrl.isEmpty()) {
												description += "<br>For more informations: <a href=\""+readmeUrl+"\">readme</a>";
											}
											return description;
										}
	
										@Override
										public String getName() {
											return action.getName();
										}
	
										@Override
										public String getGroup() {
											try {
												String group = action.getProject().getName();
												if (group.startsWith("lib_")) {
													group = group.substring("lib_".length());
												}
												return group;
											} catch (Exception e) {
												return "";
											}
										}
	
										@Override
										public String getLabel() {
											return action.getName();
										}
	
										@Override
										public String getTag() {
											return "";
										}
	
										@Override
										public String getImagePath() {
											return null;
										}
	
										@Override
										public String getPropertiesDescription() {
											String propertiesDescription = "";
											for (UIStackVariable variable: action.getVariables()) {
												propertiesDescription += "<li><i>"+ variable.getName() +"</i>" ;
												propertiesDescription += "</br>"+ variable.getComment() +"</li>";
											}
											
											return propertiesDescription;
										}
	
										@Override
										public boolean isAllowedIn(DatabaseObject parent) {
											try {
												Class<?> dboClass = Class.forName("com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke");
												if (acceptDatabaseObjects(parent, dboClass)) {
													return true;
												}
											} catch (Exception e) {
												e.printStackTrace();
											}
											return false;
										}
	
										@Override
										protected JSONObject getHint() {
											return super.getHint();
										}
										
										@Override
										public boolean isBuiltIn() {
											return false;
										}
	
										@Override
										protected DatabaseObject createBean() {
											DatabaseObject invokeAction = ComponentManager.createBean(getComponentByName("InvokeAction"));
											UIDynamicInvoke uidi = GenericUtils.cast(invokeAction);
											if (uidi != null) {
												uidi.setSharedActionQName(action.getQName());
												uidi.bNew = true;
												uidi.hasChanged = true;
											}
											return uidi;
										}
										
									});
								}
							}
							
							for (UISharedComponent usc: app.getSharedComponentList()) {
								if (usc.isRegular() && usc.isEnabled() && usc.isExposed()) {
									final UISharedRegularComponent uisrc = (UISharedRegularComponent)usc;
									components.add(new Component() {
										
										@Override
										public boolean isAllowedIn(DatabaseObject parent) {
											try {
												Class<?> dboClass = Class.forName("com.twinsoft.convertigo.beans.ngx.components.UIUseShared");
												if (acceptDatabaseObjects(parent, dboClass)) {
													return true;
												}
											} catch (Exception e) {
												e.printStackTrace();
											}
											return false;
										}
										
										@Override
										public String getTag() {
											return uisrc.getSelector();
										}
										
										@Override
										public String getPropertiesDescription() {
											String propertiesDescription = "";
											List<UICompVariable> list = uisrc.getVariables();
											propertiesDescription += list.size() > 0 ? "<br><b>variables</b><br>":"";
											for (UICompVariable variable: list) {
												propertiesDescription += "<li><i>"+ variable.getName() +"</i>" ;
												propertiesDescription += "</br>"+ variable.getComment() +"</li>";
											}
											List<UICompEvent> liste = uisrc.getUICompEventList();
											propertiesDescription += liste.size() > 0 ? "<br><b>events</b><br>":"";
											for (UICompEvent event: liste) {
												propertiesDescription += "<li><i>"+ event.getName() +"</i>" ;
												propertiesDescription += "</br>"+ event.getComment() +"</li>";
											}
											return propertiesDescription;
										}
										
										@Override
										public String getName() {
											return uisrc.getName();
										}
										
										@Override
										public String getLabel() {
											return uisrc.getName();
										}
										
										@Override
										public String getImagePath() {
											String defaultImagePath = "/com/twinsoft/convertigo/beans/ngx/components/images/uisharedcomponent_32x32.png";
											try {
												File f = new File(project.getDirPath(), uisrc.getIconFileName());
												if (f.exists()) {
													return f.getCanonicalPath();
												}
											} catch (Exception e) {}
											return defaultImagePath;
										}
										
										@Override
										public String getGroup() {
											try {
												String group = uisrc.getProject().getName();
												if (group.startsWith("lib_")) {
													group = group.substring("lib_".length());
												}
												return group;
											} catch (Exception e) {
												return "";
											}
										}
										
										@Override
										public String getDescription() {
											String description = uisrc.getComment();
											if (description.isEmpty()) {
												description = "A "+ uisrc.getName() + " component.";
											}
											if (description.indexOf(" | ") == -1) {
												description += " | ";
											}
											if (!readmeUrl.isEmpty()) {
												description += "<br>For more informations: <a href=\""+readmeUrl+"\">readme</a>";
											}
											return description;
										}
										
										@Override
										protected DatabaseObject createBean() {
											com.twinsoft.convertigo.beans.ngx.components.UIUseShared use = new com.twinsoft.convertigo.beans.ngx.components.UIUseShared();
											if (use != null) {
												use.setSharedComponentQName(uisrc.getQName());
												use.bNew = true;
												use.hasChanged = true;
											}
											return use;
										}

										@Override
										protected JSONObject getHint() {
											return super.getHint();
										}

										@Override
										public boolean isBuiltIn() {
											return false;
										}
									});
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		Collections.sort(components, new Comparator<Component>() {
			@Override
			public int compare(Component c1, Component c2) {
				return c1.getLabel().compareTo(c2.getLabel());
			}				
		} );
		
		for (Component component : components) {
			String group = component.getGroup();
			if (!map.containsKey(group)) {
				map.put(group, new ArrayList<Component>());
			}
			if (!map.get(group).contains(component)) {
				map.get(group).add(component);
			}
		}
		
		return components = Collections.unmodifiableList(components);
	}
	
	public static boolean acceptDatabaseObjects(DatabaseObject parentDatabaseObject, DatabaseObject databaseObject) {
		if (parentDatabaseObject instanceof MobileComponent && databaseObject instanceof MobileComponent) {
			return acceptDatabaseObjects(parentDatabaseObject, databaseObject.getClass());
		}
		return true;
	}

	public static boolean isTplCompatible(DatabaseObject parentDatabaseObject, DatabaseObject databaseObject) {
		if (parentDatabaseObject.getParent() != null && parentDatabaseObject instanceof MobileComponent && databaseObject instanceof MobileComponent) {
			boolean compatible = ((MobileComponent)parentDatabaseObject).compareToTplVersion(getTplRequired(databaseObject)) >= 0;
			if (!compatible) {
				Engine.logStudio.warn("The '"+databaseObject.getName()+"' component isn't compatible with your Template project."
						+ " Please change your Template project for a newer one to use it.");
				return false;
			}
		}
		return true;
	}
	
	public static String getTplRequired(DatabaseObject databaseObject) {
		if (databaseObject instanceof MobileComponent) {
			return ((MobileComponent)databaseObject).requiredTplVersion();
		}
		return "";
	}
	
	protected static boolean acceptDatabaseObjects(DatabaseObject dboParent, Class<?> dboClass) {
		if (UIComponent.class.isAssignableFrom(dboClass)) {
			if (UIDynamicEmit.class.isAssignableFrom(dboClass)) {
				if (dboParent instanceof UIComponent) {
					UISharedComponent uisc = ((UIComponent)dboParent).getSharedComponent();
					return uisc != null && uisc instanceof UISharedRegularComponent;
				}
				return false;
			}
			
			if (dboParent instanceof ApplicationComponent) {
				if (UIStyle.class.isAssignableFrom(dboClass) ||
					UIDynamicMenu.class.isAssignableFrom(dboClass) ||
					UIActionStack.class.isAssignableFrom(dboClass) ||
					UISharedComponent.class.isAssignableFrom(dboClass) ||
					UIAppEvent.class.isAssignableFrom(dboClass)) {
					return true;
				}
				if (UIEventSubscriber.class.isAssignableFrom(dboClass)) {
					ApplicationComponent app = (ApplicationComponent)dboParent;
					if (UIAppGuard.class.isAssignableFrom(dboClass)) {
						if (app.compareToTplVersion("7.9.0.6") >= 0) {
							return true;
						}
					} else if (app.compareToTplVersion("7.6.0.1") >= 0) {
						return true;
					}
				}
			} else if (dboParent instanceof PageComponent) {
				if (!UITheme.class.isAssignableFrom(dboClass) &&
					!UIDynamicMenu.class.isAssignableFrom(dboClass) &&
					!UIDynamicMenuItem.class.isAssignableFrom(dboClass) &&
					!UIAppEvent.class.isAssignableFrom(dboClass) &&
					!UIAppGuard.class.isAssignableFrom(dboClass) &&
					!UIActionStack.class.isAssignableFrom(dboClass) &&
					!UISharedComponent.class.isAssignableFrom(dboClass) &&
					!UIAttribute.class.isAssignableFrom(dboClass) &&
					!UIDynamicAttr.class.isAssignableFrom(dboClass) &&
					!UIControlVariable.class.isAssignableFrom(dboClass) &&
					!UIActionEvent.class.isAssignableFrom(dboClass) &&
					!IAction.class.isAssignableFrom(dboClass)) {
					return true;
				}
			} else if (dboParent instanceof UIComponent) {
				UIDynamicMenu menu = ((UIComponent)dboParent).getMenu();
				if (menu != null) {
					if (UIControlEvent.class.isAssignableFrom(dboClass)) {
						if (menu.compareToTplVersion("7.5.2.0") < 0) {
							return false;
						}
					}
				}
				
				if (dboParent instanceof UIActionStack) {
					if (UIActionErrorEvent.class.isAssignableFrom(dboClass) ||
						UIActionFinallyEvent.class.isAssignableFrom(dboClass) ||
						UIStackVariable.class.isAssignableFrom(dboClass) ||
						IAction.class.isAssignableFrom(dboClass)) {
						return true;
					}
				}
				else if (dboParent instanceof UISharedComponent) {
					if (UIText.class.isAssignableFrom(dboClass) ||
						UICustom.class.isAssignableFrom(dboClass) ||
						UIElement.class.isAssignableFrom(dboClass) ||
						UIPageEvent.class.isAssignableFrom(dboClass) ||
						UISharedComponentEvent.class.isAssignableFrom(dboClass) ||
						UIEventSubscriber.class.isAssignableFrom(dboClass) ||
						UICompEvent.class.isAssignableFrom(dboClass) ||
						UICompVariable.class.isAssignableFrom(dboClass) || 
						UIStyle.class.isAssignableFrom(dboClass)) {
						if (!IAction.class.isAssignableFrom(dboClass) && 
							!UIAppGuard.class.isAssignableFrom(dboClass)) {
							return true;
						}
					}					
				}
				else if (dboParent instanceof UIAppEvent ||
						dboParent instanceof UIPageEvent || 
						dboParent instanceof UISharedComponentEvent || 
						dboParent instanceof UIControlEvent ||
						dboParent instanceof UIEventSubscriber) {
					if (UIActionErrorEvent.class.isAssignableFrom(dboClass) ||
						UIActionFinallyEvent.class.isAssignableFrom(dboClass) ||
						IAction.class.isAssignableFrom(dboClass)) {
						return true;
					}
				}
				else if (dboParent instanceof UIActionEvent) {
					if (IAction.class.isAssignableFrom(dboClass)) {
						return true;
					}
				}
				else if (dboParent instanceof IAction) {
					if (UIActionFailureEvent.class.isAssignableFrom(dboClass) ||
						UIControlVariable.class.isAssignableFrom(dboClass) ||
						IAction.class.isAssignableFrom(dboClass)) {
						return true;
					}
					if (dboParent instanceof UIDynamicIterate) {
						if (UIActionLoopEvent.class.isAssignableFrom(dboClass)) {
							return true;
						}
					}
					if (dboParent instanceof UIDynamicSwitch) {
						if (UIActionCaseEvent.class.isAssignableFrom(dboClass)) {
							return true;
						}
					}
					if (dboParent instanceof UIDynamicIf) {
						if (UIActionElseEvent.class.isAssignableFrom(dboClass)) {
							return true;
						}
					}					
				} else if (dboParent instanceof UIDynamicMenuItem) {
					if (UIAttribute.class.isAssignableFrom(dboClass)) {
						return true;
					}
				} else if (dboParent instanceof UIUseShared) {
					if (UIUseVariable.class.isAssignableFrom(dboClass) ||
						UIAttribute.class.isAssignableFrom(dboClass) ||
						(UIControlEvent.class.isAssignableFrom(dboClass))) {
						return true;
					}
				} else if (dboParent instanceof UIDynamicAttr) {
					return false;
				} else if (dboParent instanceof UIElement) {
					if (UIDynamicMenuItem.class.isAssignableFrom(dboClass)) {
						return menu != null;
					}
					
					if (!UIControlVariable.class.isAssignableFrom(dboClass) &&
						!UIStackVariable.class.isAssignableFrom(dboClass) &&
						!UICompVariable.class.isAssignableFrom(dboClass) &&
						!UIAppEvent.class.isAssignableFrom(dboClass) &&
						!UIPageEvent.class.isAssignableFrom(dboClass) &&
						!UISharedComponentEvent.class.isAssignableFrom(dboClass) &&
						!UIEventSubscriber.class.isAssignableFrom(dboClass) &&
						!UIActionEvent.class.isAssignableFrom(dboClass) &&
						!UITheme.class.isAssignableFrom(dboClass) &&
						!IAction.class.isAssignableFrom(dboClass)) {
							return true;
					}
				} else if (dboParent instanceof UICustom) {
					if (UIText.class.isAssignableFrom(dboClass) ||
						UICustom.class.isAssignableFrom(dboClass) ||
						UIElement.class.isAssignableFrom(dboClass)) {
						return true;
					}					
				}
			}
			return false;
		}
		return true;
	}
	
	protected static Component getDboComponent(final Class<? extends DatabaseObject> dboClass, final String group) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException {
		String className = dboClass.getName();
		String beanInfoClassName = className + "BeanInfo";
		
		Class<BeanInfo> beanInfoClass = GenericUtils.cast(Class.forName(beanInfoClassName));
		final BeanInfo bi = beanInfoClass.getConstructor().newInstance();
		final BeanDescriptor bd = bi.getBeanDescriptor();
		
		return new Component() {

			@Override
			public String getDescription() {
				String description = bd.getShortDescription();
				return bd != null ? description : dboClass.getSimpleName();
			}

			@Override
			public String getGroup() {
				return group;
			}

			@Override
			public String getName() {
				return bd != null ? bd.getName() : dboClass.getSimpleName();
			}
			
			@Override
			public String getLabel() {
				return bd != null ? bd.getDisplayName() : dboClass.getSimpleName();
			}

			@Override
			public String getImagePath() {
				return MySimpleBeanInfo.getIconName(bi, BeanInfo.ICON_COLOR_32x32);
			}
			
			@Override
			public String getTag() {
				return "";
			}

			@Override
			public String getPropertiesDescription() {
				BeanInfo beanInfo;
				try {
					beanInfo = Introspector.getBeanInfo(dboClass);
					PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
					
					propertyDescriptors = propertyDescriptors.clone();
					Arrays.sort(propertyDescriptors, (o1, o2) -> {
						if(o1.isExpert() == o2.isExpert()) {
							return o1.getDisplayName().compareTo(o2.getDisplayName());
						} else if(o1.isExpert()) {
							return 1;
						} else { 
							return -1;
						}
					});
					
					String propertiesDescription = "";
					for (PropertyDescriptor dbopd : propertyDescriptors) {
						if (!dbopd.isHidden()) {
							propertiesDescription += "<li><i>"+ dbopd.getDisplayName() +"</i>" ;
							propertiesDescription += "</br>"+ dbopd.getShortDescription().replace("|", "") +"</li>";
						}
					}
					return propertiesDescription.isEmpty() ? "": "<ul>"+propertiesDescription+"</ul>";
					
				} catch (IntrospectionException e) {
				}
				
				return "";
			}
			
			@Override
			public boolean isAllowedIn(DatabaseObject parent) {
				try {
					if (acceptDatabaseObjects(parent, dboClass)) {
						return isTplCompatible(parent, createBean());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}

			@Override
			protected DatabaseObject createBean() {
				try {
					DatabaseObject dbo = dboClass.getConstructor().newInstance();
					dbo.bNew = true;
					dbo.hasChanged = true;
					return dbo;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected JSONObject getHint() {
				try {
					JSONObject jsonOb = getC8oBeans().get(dboClass.getSimpleName());
					if (jsonOb != null && jsonOb.has("hint")) {
						JSONObject jsonHint =  jsonOb.getJSONObject("hint");
						return new JSONObject(jsonHint.toString()); // copy
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return super.getHint();
			}

			@Override
			public boolean isBuiltIn() {
				return true;
			}
			
		};
	}

	public static Component getComponentByName(String name) {
		for (Component component : getComponents()) {
			if (name.startsWith("com.twinsoft.convertigo.beans.ngx.components.")) {
				name = name.replace("com.twinsoft.convertigo.beans.ngx.components.", "");
			}
			if (component.getName().equals(name)) {
				return component;
			}
		}
		return null;
	}
	
	public static String getActionTsCode(String name) {
		String code = instance.aCache.get(name);
		if (code == null) {
			try (InputStream inputstream = instance.getClass().getResourceAsStream("actionbeans/"+ name +".ts")) {
				code = IOUtils.toString(inputstream, "UTF-8");
				instance.aCache.put(name, code);
			} catch (Exception e) {
				code = "";
				if (Engine.isStarted) {
					Engine.logBeans.warn("(ComponentManager) Missing action typescript file for pseudo-bean '"+ name +"' !");
				} else {
					System.out.println("(ComponentManager) Missing action typescript file for pseudo-bean '"+ name +"' !");
				}
			}
		}
		return code;
	}
	
	public static File getCompBeanDir(String name) {
		try {
			if (instance.compbeansDir == null) {
				String path = URLUtils.getFullpathRessources(instance.getClass(), "compbeans");
				if (path != null) {
					instance.compbeansDir = new File(path);
				}
			}
			if (instance.compbeansDir != null) {
				return new File(instance.compbeansDir, name);
			}
		} catch (Exception e) {
			if (Engine.isStarted) {
				Engine.logBeans.warn("(ComponentManager) Missing component folder for pseudo-bean '"+ name +"' !");
			} else {
				System.out.println("(ComponentManager) Missing component folder for pseudo-bean '"+ name +"' !");
			}
		}
		return null;
	}
	
	public static Map<String, IonBean> getIonBeans() {
		return Collections.unmodifiableMap(instance.bCache);
	}
	
	protected static void printIcons() {
		IonProperty property = instance.pCache.get("IconName");
		for (Object name : property.getValues()) {
			if (name instanceof String) {
				String icon = "icon_" + ((String)name).replaceAll("-", "_");
				String key = icon + "(\""+ name +"\"),";
				System.out.println(key);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		try {
			if (args.length > 0) {
				File output = new File(args[0]);
				if (output.exists() && output.isDirectory()) {
					List<IonBean> beans = new ArrayList<IonBean>();
					beans.addAll(getIonBeans().values());
					Collections.sort(beans, new Comparator<IonBean>() {
						@Override
						public int compare(IonBean b1, IonBean b2) {
							return b1.getName().toLowerCase().compareTo(b2.getName().toLowerCase());
						}				
					} );
					
					for (IonBean bean: beans) {
						JSONObject jsonBean = new JSONObject();
						jsonBean.put(IonBean.Key.tag.name(), bean.getTag());
						
						List<IonProperty> properties = new ArrayList<IonProperty>();
						properties.addAll(bean.getProperties().values());
						Collections.sort(properties, new Comparator<IonProperty>() {
							@Override
							public int compare(IonProperty p1, IonProperty p2) {
								return p1.getName().toLowerCase().compareTo(p2.getName().toLowerCase());
							}				
						} );
					
						JSONObject jsonProperties = new JSONObject();
						jsonBean.put(IonBean.Key.properties.name(), jsonProperties);
						for (IonProperty property: properties) {
							JSONObject jsonProperty = new JSONObject();
							jsonProperty.put(IonProperty.Key.attr.name(), property.getAttr());
							jsonProperty.put(IonProperty.Key.type.name(), property.getType());
							jsonProperty.put(IonProperty.Key.mode.name(), property.getMode());
							jsonProperty.put(IonProperty.Key.value.name(), property.getValue());
							jsonProperty.put(IonProperty.Key.values.name(), property.getJSONObject().getJSONArray(IonProperty.Key.values.name()));
							jsonProperties.put(property.getName(), jsonProperty);
						}
								
						String jsonString = jsonBean.toString(1);
						FileUtils.write(new File(output, "c8o-beans/ion5/"+ bean.getName() +".json"), jsonString, "UTF-8");
					}
				}
			}
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	private JSONObject loadFont(String fontId) {
		return loadFonts().get(fontId);
	}
	
	protected JSONObject loadFontFromApi(String fontId) {
		JSONObject jsonFont = null;
		if (fontId != null && !fontId.isBlank()) {
			if (fontMap.get(fontId) == null || !fontMap.get(fontId).has("variants")) {
				try {
					String url = "https://api.fontsource.org/v1/fonts/"+ fontId;
					HttpGet get = new HttpGet(url);
					try (CloseableHttpResponse response = Engine.theApp.httpClient4.execute(get)) {
						int code = response.getStatusLine().getStatusCode();
						if (code != 200) {
							throw new EngineException("Code " + code + " for " + url);
						}
						String sContent = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
						fontMap.put(fontId, new JSONObject(sContent));
					}
				} catch (Exception e) {
					Engine.logEngine.warn("(ComponentManager) Unabled to get font with id "+ fontId + " : " + e.getMessage());
				}
			}
			jsonFont = fontMap.get(fontId);
		}
		return jsonFont;
	}
	
	private Map<String, JSONObject> loadFonts() {
		return loadFontsFromFile();
	}
	
	protected Map<String, JSONObject> loadFontsFromApi() {
		if (fontMap.isEmpty()) {
			JSONArray jsonFonts = null;
			try {
				String url = "https://api.fontsource.org/v1/fonts";
				HttpGet get = new HttpGet(url);
				try (CloseableHttpResponse response = Engine.theApp.httpClient4.execute(get)) {
					int code = response.getStatusLine().getStatusCode();
					if (code != 200) {
						throw new EngineException("Code " + code + " for " + url);
					}
					String sContent = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
					jsonFonts = new JSONArray(sContent);
				}
				
				if (jsonFonts != null) {
					for (int i = 0; i < jsonFonts.length(); i++) {
						JSONObject jsonFont = jsonFonts.getJSONObject(i);
						String fontId = jsonFont.getString("id");
						fontMap.put(fontId, jsonFont);
					}
				}
			} catch (Exception e) {
				if (Engine.isStarted) {
					Engine.logEngine.warn("(ComponentManager) Unabled to load fonts : " + e.getMessage());
				} else {
					System.out.println("(ComponentManager) Unabled to load fonts : " + e.getMessage());
				}
				fontMap.clear();
			}
		}
		return fontMap;
	}
	
	private Map<String, JSONObject> loadFontsFromFile() {
		if (fontMap.isEmpty()) {
			try (InputStream inputstream = getClass().getResourceAsStream("font-sources.json")) {
				String json = IOUtils.toString(inputstream, "UTF-8");
				
				JSONArray jsonFonts = new JSONArray(json);
				for (int i = 0; i < jsonFonts.length(); i++) {
					JSONObject jsonFont = jsonFonts.getJSONObject(i);
					String fontId = jsonFont.getString("id");
					fontMap.put(fontId, jsonFont);
				}
				
				if (Engine.isStarted) {
					Engine.logEngine.info("(ComponentManager) Font sources loaded from file");
				} else {
					System.out.println("(ComponentManager) Font sources loaded from file");
				}
			} catch (Exception e) {
				if (Engine.isStarted) {
					Engine.logEngine.warn("(ComponentManager) Unabled to load fonts from file: " + e.getMessage());
				} else {
					System.out.println("(ComponentManager) Unabled to load fonts from file: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
		return fontMap;
	}
	
	protected void storeFonts() {
		Engine.execute(() -> {
			try {
				File file = new File("C://Dev/font-sources-bis.json");
				if (!file.exists()) {
					Map<String, JSONObject> map = instance.loadFontsFromFile();
					System.out.println("(ComponentManager) retrieving "+ map.size() +" fonts...");
					
					List<String> fontIds = map.keySet().stream().sorted((e1, e2) -> 
					e1.toString().compareTo(e2.toString())).collect(Collectors.toList());
					
					JSONArray array = new JSONArray();
					for (String fontId: fontIds) {
						JSONObject jsonOb = getFont(fontId);
						if (jsonOb == null) {
							System.out.println("(ComponentManager) Unabled to retrieve font " + fontId);
							jsonOb = new JSONObject();
							jsonOb.put("id", fontId);
						} else {
							System.out.println("(ComponentManager) Retrieved font " + fontId);
						}
						array.put(jsonOb);
					}
					
					String content = array.toString(1);
					FileUtils.write(file, content, "UTF-8");
					System.out.println("(ComponentManager) font-sources-bis.json written");
				} else {
					String content = FileUtils.readFileToString(file, "UTF-8");
					JSONArray array = new JSONArray(content);
					System.out.println("(ComponentManager) checking "+ array.length() +" fonts...");
					for (int i = 0; i < array.length(); i++) {
						JSONObject jsonFont = array.getJSONObject(i);
						String fontId = jsonFont.getString("id");
						if (jsonFont.length() <= 1) {
							System.out.println("(ComponentManager) Invalid font " + fontId);
						}
					}
					System.out.println("(ComponentManager) check done");
				}
			} catch (Exception e) {
				System.out.println("(ComponentManager) storeFonts: exception occured " + e.getMessage());
			}
		});
	}
	
	static public JSONObject getFont(String fontId) {
		return instance.loadFont(fontId);
	}
	
	static public Map<String, JSONObject> getFonts() {
		return instance.loadFonts();
	}
}
