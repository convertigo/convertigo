/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

package com.twinsoft.convertigo.beans.mobile.components.dynamic;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobileComponent;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.IAction;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIActionElseEvent;
import com.twinsoft.convertigo.beans.mobile.components.UIActionErrorEvent;
import com.twinsoft.convertigo.beans.mobile.components.UIActionEvent;
import com.twinsoft.convertigo.beans.mobile.components.UIActionFailureEvent;
import com.twinsoft.convertigo.beans.mobile.components.UIActionFinallyEvent;
import com.twinsoft.convertigo.beans.mobile.components.UIActionLoopEvent;
import com.twinsoft.convertigo.beans.mobile.components.UIAnimation;
import com.twinsoft.convertigo.beans.mobile.components.UIAppEvent;
import com.twinsoft.convertigo.beans.mobile.components.UIAttribute;
import com.twinsoft.convertigo.beans.mobile.components.UICompVariable;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIControlDirective;
import com.twinsoft.convertigo.beans.mobile.components.UIControlEvent;
import com.twinsoft.convertigo.beans.mobile.components.UIControlVariable;
import com.twinsoft.convertigo.beans.mobile.components.UICustom;
import com.twinsoft.convertigo.beans.mobile.components.UICustomAction;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicIf;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicIterate;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenu;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenuItem;
import com.twinsoft.convertigo.beans.mobile.components.UIUseShared;
import com.twinsoft.convertigo.beans.mobile.components.UIActionStack;
import com.twinsoft.convertigo.beans.mobile.components.UIElement;
import com.twinsoft.convertigo.beans.mobile.components.UIEventSubscriber;
import com.twinsoft.convertigo.beans.mobile.components.UIForm;
import com.twinsoft.convertigo.beans.mobile.components.UIFormControlValidator;
import com.twinsoft.convertigo.beans.mobile.components.UIFormCustomValidator;
import com.twinsoft.convertigo.beans.mobile.components.UIFormValidator;
import com.twinsoft.convertigo.beans.mobile.components.UIPageEvent;
import com.twinsoft.convertigo.beans.mobile.components.UISharedComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIStackVariable;
import com.twinsoft.convertigo.beans.mobile.components.UIStyle;
import com.twinsoft.convertigo.beans.mobile.components.UIText;
import com.twinsoft.convertigo.beans.mobile.components.UITheme;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.URLUtils;
import com.twinsoft.convertigo.engine.util.WeakValueHashMap;

public class ComponentManager {
	private static ComponentManager instance = new ComponentManager();
	
	private SortedMap<String, IonProperty> pCache = new TreeMap<>();
	private SortedMap<String, IonBean> bCache = new TreeMap<>();
	private Map<String, String> aCache = new WeakValueHashMap<>();
	
	private List<String> groups;
	private List<Component> orderedComponents;
	private List<Component> components;
	
	private File compbeansDir;
	
	private ComponentManager() {
		loadModels();
	}
	
	private synchronized void loadModels() {
		clear();
		
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
			readTemplateModels(root);
			
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
		pCache.clear();
		bCache.clear();
		aCache.clear();
		
		groups = null;
		orderedComponents = null;
		components = null;
	}
	
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
	
	private void readTemplateModels(JSONObject root) {
		// TODO Auto-generated method stub
		
	}

	public static IonBean loadBean(String jsonString) throws Exception {
		JSONObject jsonBean = new JSONObject(jsonString);
		String modelName = jsonBean.getString(IonBean.Key.name.name());
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
			System.out.println("(ComponentManager) Model \""+modelName+"\" does not exist anymore in cache.");
			if (Engine.isStarted) {
				Engine.logBeans.warn("(ComponentManager) Model \""+modelName+"\" does not exist anymore in cache.");
			}
			return new IonBean(jsonString);
		}
	}
	
	public static DatabaseObject createBean(Component c) {
		return c != null ? c.createBean():null;
	}
	
	public static void refresh() {
		instance.loadModels();
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
			
			// Add shared components
			group = GROUP_SHARED_COMPONENTS;
			components.add(getDboComponent(UISharedComponent.class,group));
			components.add(getDboComponent(UIUseShared.class,group));
			components.add(getDboComponent(UICompVariable.class,group));
			
			// Add shared actions
			group = GROUP_SHARED_ACTIONS;
			components.add(getDboComponent(UIActionStack.class,group));
			components.add(getDboComponent(UIStackVariable.class,group));
			
			// Add Controls
			group = GROUP_CONTROLS;
			components.add(getDboComponent(UIControlEvent.class,group));
			components.add(getDboComponent(UIAppEvent.class,group));
			components.add(getDboComponent(UIPageEvent.class,group));
			components.add(getDboComponent(UIEventSubscriber.class,group));
			components.add(getDboComponent(UIActionErrorEvent.class,group));
			components.add(getDboComponent(UIActionFailureEvent.class,group));
			components.add(getDboComponent(UIActionFinallyEvent.class,group));
			components.add(getDboComponent(UIActionLoopEvent.class,group));
			components.add(getDboComponent(UIActionElseEvent.class,group));
			components.add(getDboComponent(UIControlDirective.class,group));
			
			// Add Actions
			group = GROUP_ACTIONS;
			components.add(getDboComponent(UIControlVariable.class,group));
			components.add(getDboComponent(UICustomAction.class,group));
			
			components.add(getDboComponent(UIForm.class,"Forms"));
			components.add(getDboComponent(UIFormControlValidator.class,"Forms"));
			components.add(getDboComponent(UIFormCustomValidator.class,"Forms"));
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		for (final IonBean bean: instance.bCache.values()) {
			components.add(new Component() {
				
				@Override
				public boolean isAllowedIn(DatabaseObject parent) {
					if (bean.getTag().equals("ion-menu")) {
						return parent instanceof ApplicationComponent;
					}
					if (bean.getClassName().startsWith("com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenuItem")) {
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
							if (!dbopd.isHidden() && !Boolean.TRUE.equals(dbopd.getValue("disable"))) {
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

			});
		}
		
		Collections.sort(components, new Comparator<Component>() {
			@Override
			public int compare(Component c1, Component c2) {
				return c1.getLabel().compareTo(c2.getLabel());
			}				
		} );
		
		return components = Collections.unmodifiableList(components);
	}
	
	public static boolean acceptDatabaseObjects(DatabaseObject parentDatabaseObject, DatabaseObject databaseObject) {
		if (parentDatabaseObject instanceof MobileComponent && databaseObject instanceof MobileComponent) {
			return acceptDatabaseObjects(parentDatabaseObject, databaseObject.getClass());
		}
		return true;
	}

	public static boolean isTplCompatible(DatabaseObject parentDatabaseObject, DatabaseObject databaseObject) {
		if (parentDatabaseObject instanceof MobileComponent && databaseObject instanceof MobileComponent) {
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
					if (app.compareToTplVersion("7.6.0.1") >= 0) {
						return true;
					}
				}
			} else if (dboParent instanceof PageComponent) {
				if (!UITheme.class.isAssignableFrom(dboClass) &&
					!UIDynamicMenu.class.isAssignableFrom(dboClass) &&
					!UIDynamicMenuItem.class.isAssignableFrom(dboClass) &&
					!UIAppEvent.class.isAssignableFrom(dboClass) &&
					!UIActionStack.class.isAssignableFrom(dboClass) &&
					!UISharedComponent.class.isAssignableFrom(dboClass) &&
					!UIFormValidator.class.isAssignableFrom(dboClass) &&
					!UIAttribute.class.isAssignableFrom(dboClass) &&
					!UIControlVariable.class.isAssignableFrom(dboClass) &&
					!UIActionEvent.class.isAssignableFrom(dboClass) &&
					!IAction.class.isAssignableFrom(dboClass)) {
					return true;
				}
			} else if (dboParent instanceof UIComponent) {
				UIDynamicMenu menu = ((UIComponent)dboParent).getMenu();
				if (menu != null) {
					if (UIControlEvent.class.isAssignableFrom(dboClass) ||
						UIFormValidator.class.isAssignableFrom(dboClass)) {
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
						UIEventSubscriber.class.isAssignableFrom(dboClass) ||
						UICompVariable.class.isAssignableFrom(dboClass)) {
						if (!IAction.class.isAssignableFrom(dboClass)) {
							return true;
						}
					}					
				}
				else if (dboParent instanceof UIAppEvent ||
						dboParent instanceof UIPageEvent || 
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
					if (UIControlVariable.class.isAssignableFrom(dboClass)) {
						return true;
					}
				} else if (dboParent instanceof UIElement) {
					if (UIDynamicMenuItem.class.isAssignableFrom(dboClass)) {
						return menu != null;
					}
					
					if (UIFormValidator.class.isAssignableFrom(dboClass)) {
						return ((UIComponent)dboParent).getUIForm() != null;
					}
					
					if (!UIControlVariable.class.isAssignableFrom(dboClass) &&
						!UIStackVariable.class.isAssignableFrom(dboClass) &&
						!UICompVariable.class.isAssignableFrom(dboClass) &&
						!UIAppEvent.class.isAssignableFrom(dboClass) &&
						!UIPageEvent.class.isAssignableFrom(dboClass) &&
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
	
	protected static Component getDboComponent(final Class<? extends DatabaseObject> dboClass, final String group) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		String className = dboClass.getName();
		String beanInfoClassName = className + "BeanInfo";
		
		Class<BeanInfo> beanInfoClass = GenericUtils.cast(Class.forName(beanInfoClassName));
		final BeanInfo bi = beanInfoClass.newInstance();
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
					DatabaseObject dbo = dboClass.newInstance();
					dbo.bNew = true;
					dbo.hasChanged = true;
					return dbo;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		};
	}

	public static Component getComponentByName(String name) {
		for (Component component : getComponents()) {
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
}
