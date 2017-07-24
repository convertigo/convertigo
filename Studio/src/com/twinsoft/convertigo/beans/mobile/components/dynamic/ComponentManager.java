/*
 * Copyright (c) 2001-2016 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.mobile.components.dynamic;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIAttribute;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIControlAction;
import com.twinsoft.convertigo.beans.mobile.components.UIControlAttr;
import com.twinsoft.convertigo.beans.mobile.components.UIControlCustomAction;
import com.twinsoft.convertigo.beans.mobile.components.UIControlCallFullSync;
import com.twinsoft.convertigo.beans.mobile.components.UIControlCallSequence;
import com.twinsoft.convertigo.beans.mobile.components.UIControlDirective;
import com.twinsoft.convertigo.beans.mobile.components.UIControlEvent;
import com.twinsoft.convertigo.beans.mobile.components.UICustom;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenu;
import com.twinsoft.convertigo.beans.mobile.components.UIElement;
import com.twinsoft.convertigo.beans.mobile.components.UIForm;
import com.twinsoft.convertigo.beans.mobile.components.UIStyle;
import com.twinsoft.convertigo.beans.mobile.components.UIText;
import com.twinsoft.convertigo.beans.mobile.components.UITheme;
import com.twinsoft.convertigo.beans.mobile.components.UIFormControlValidator;
import com.twinsoft.convertigo.beans.mobile.components.UIFormCustomValidator;
import com.twinsoft.convertigo.beans.mobile.components.UIFormValidator;
import com.twinsoft.convertigo.beans.mobile.components.UIControlVariable;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class ComponentManager {
	private static ComponentManager instance = new ComponentManager();
	
	private SortedMap<String, IonProperty> pCache = new TreeMap<String, IonProperty>();
	private SortedMap<String, IonBean> bCache = new TreeMap<String, IonBean>();
	private SortedMap<String, IonTemplate> tCache = new TreeMap<String, IonTemplate>();
	
	private ComponentManager() {
		loadModels();
	}
	
	private void loadModels() {
		clear();
		InputStream inputstream = null;
		try {
			System.out.println("(ComponentManager) Start loading Ionic objects");
			
			inputstream = getClass().getResourceAsStream("/ion_objects.json");
			String json = IOUtils.toString(inputstream, "UTF-8");
			System.out.println(json);
			
			JSONObject root = new JSONObject(json);
			readPropertyModels(root);
			readBeanModels(root);
			readTemplateModels(root);
			
			System.out.println("(ComponentManager) End loading Ionic objects");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			if (inputstream != null)
				IOUtils.closeQuietly(inputstream);
		}
	}
	
	private void clear() {
		pCache.clear();
		bCache.clear();
		tCache.clear();
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
									IonProperty original = pCache.get(pkey);
									if (original != null) {
										String jsonString = original.getJSONObject().toString();
										IonProperty property = new IonProperty(new JSONObject(jsonString));
										property.setValue(value);
										bean.putProperty(property);
									}
								}
							}
						}
					}
					bCache.put(key, bean);
					
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
		IonBean model = instance.bCache.get(modelName);
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
					// new property
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
		List<String> groups = new ArrayList<String>(10);
		groups.add("Customs");
		for (IonBean bean: instance.bCache.values()) {
			if (!groups.contains(bean.getGroup())) {
				groups.add(bean.getGroup());
			}
		}
		
		groups.add("Controls");
		groups.add("Actions");
//		groups.add("Sources");
		return Collections.unmodifiableList(groups);
	}
	
	public static List<Component> getComponents() {
		List<Component> components = new ArrayList<Component>(10);
		
		try {
			String group;
			// Add Customs
			group = "Customs";
			components.add(getDboComponent(UIElement.class,group));
			components.add(getDboComponent(UIAttribute.class,group));
			components.add(getDboComponent(UICustom.class,group));
			components.add(getDboComponent(UIText.class,group));
			components.add(getDboComponent(UIStyle.class,group));
			components.add(getDboComponent(UITheme.class,group));
			
			// Add Controls
			group = "Controls";
			components.add(getDboComponent(UIControlEvent.class,group));
			components.add(getDboComponent(UIControlDirective.class,group));
			
			// Add Actions
			group = "Actions";
			components.add(getDboComponent(UIControlCustomAction.class,group));
			components.add(getDboComponent(UIControlCallSequence.class,group));
			components.add(getDboComponent(UIControlCallFullSync.class,group));
			components.add(getDboComponent(UIControlVariable.class,group));
			
//			// Add Sources
//			group = "Sources";
//			components.add(getDboComponent(UIControlCustomSource.class,group));
//			components.add(getDboComponent(UIControlListenSequenceSource.class,group));
//			components.add(getDboComponent(UIControlListenFullSyncSource.class,group));
			
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
							UIDynamicMenu menu = ((UIComponent)parent).getMenu();
							return menu != null;
						}
					}
					
					if (parent instanceof PageComponent)
						return true;
					if (parent instanceof UIDynamicElement)
						return true;
					if (parent instanceof UIElement)
						return true;
					if (parent instanceof UIControlDirective)
						return true;
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
				protected DatabaseObject createBean() {
					DatabaseObject dbo = bean.createBean();
					if (dbo != null && dbo instanceof UIDynamicElement) {
						IonBean ionBean = ((UIDynamicElement)dbo).getIonBean();
						if (ionBean != null && ionBean.hasProperty("FormControlName")) {
							ionBean.setPropertyValue("FormControlName", new MobileSmartSourceType("var"+dbo.priority));
						}
					}
					return dbo;
				}
			});
		}
		return Collections.unmodifiableList(components);
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
			public boolean isAllowedIn(DatabaseObject parent) {
				if (parent instanceof ApplicationComponent) {
					if (UIStyle.class.isAssignableFrom(dboClass)) {
						return true;
					}
				}
				if (parent instanceof PageComponent) {
					if (!UITheme.class.isAssignableFrom(dboClass) &&
						!UIFormValidator.class.isAssignableFrom(dboClass) &&
						!UIAttribute.class.isAssignableFrom(dboClass) &&
						!UIControlAction.class.isAssignableFrom(dboClass)) {
						return true;
					}
				}
				if (parent instanceof UIElement) {
					if (!UITheme.class.isAssignableFrom(dboClass) &&
						!(UIControlAction.class.isAssignableFrom(dboClass))) {
							return true;
					}
				}
				if (parent instanceof UIControlAttr) {
					if (UIControlAction.class.isAssignableFrom(dboClass)) {
						return true;
					}
				}
				if (parent instanceof UIControlAction) {
					if (UIControlVariable.class.isAssignableFrom(dboClass)) {
						return true;
					}
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
}
