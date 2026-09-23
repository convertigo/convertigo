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

package com.twinsoft.convertigo.beans.ngx.components.dynamic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement;
import com.twinsoft.convertigo.engine.util.JsonUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class IonBean {
	
	enum Key {
		classname,
		tplVersion,
		deprecatedTplVersion,
		tag,
		tags,
		name,
		component,
		displayName,
		label,
		autoClose,
		group,
		description,
		icon16,
		icon32,
		properties,
		events,
		scss,
		config,
		displayFormat,
		needNgTemplate,
		hint
		;
	}
	
	/** Property mode and value of an instance, when they differ from its model */
	private record PropertyValue(String mode, Object value) {}
	
	private JSONObject jsonBean;
	private String beanData;
	private IonConfig ionConfig;
	private String imageFolder = "/com/twinsoft/convertigo/beans/ngx/components/dynamic/images/";
	
	// An instance shares the jsonBean of its model (never modified) and only holds its own property values
	private IonBean model;
	private Map<String, PropertyValue> propertyValues;
	
	public IonBean() {
		try {
			jsonBean = new JSONObject()
				.put(Key.classname.name(), "com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement")
				.put(Key.tplVersion.name(), "1.0.88")
				.put(Key.name.name(), "bean")
				.put(Key.component.name(), "component")
				.put(Key.deprecatedTplVersion.name(), "")
				.put(Key.displayName.name(), "")
				.put(Key.tag.name(), "tag")
				.put(Key.tags.name(), new JSONArray())
				.put(Key.label.name(), "label")
				.put(Key.autoClose.name(), false)
				.put(Key.group.name(), "Components")
				.put(Key.description.name(), "description")
				.put(Key.icon16.name(), "default_color_16x16.png")
				.put(Key.icon32.name(), "default_color_32x32.png")
				.put(Key.properties.name(), new JSONObject())
				.put(Key.events.name(), new JSONObject())
				.put(Key.scss.name(), new JSONArray())
				.put(Key.config.name(), new JSONObject())
				.put(Key.displayFormat.name(), "")
				.put(Key.needNgTemplate.name(), false)
				.put(Key.hint.name(), new JSONObject())
				;
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public IonBean(String jsonString) throws JSONException {
		this(new JSONObject(jsonString), false);
	}

	public IonBean(JSONObject jsonOb) throws JSONException {
		this(jsonOb, true);
	}

	private IonBean(JSONObject jsonOb, boolean copyValues) throws JSONException {
		this();
		for (Key k: Key.values()) {
			if (jsonOb.has(k.name())) {
				try {
					Object value = jsonOb.get(k.name());
					jsonBean.put(k.name(), copyValues ? JsonUtils.copy(value) : value);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	IonBean(IonBean model) {
		this.model = model;
		this.jsonBean = model.jsonBean;
		this.imageFolder = model.imageFolder;
	}
	
	public void setImageFolder(String imageFolder) {
		this.imageFolder = imageFolder;
	}
	
	public synchronized String toBeanData() {
		if (beanData != null) {
			return beanData;
		}
		beanData = toString();
		try {
			JSONObject jsonOb = new JSONObject(beanData);
			for (Key k: Key.values()) {
				if (k.equals(Key.name))
					continue;
				if (k.equals(Key.properties)) {
					JSONObject jsonProperties = jsonOb.getJSONObject(Key.properties.name());
					if (jsonProperties != null) {
						@SuppressWarnings("unchecked")
						Iterator<String> it = jsonProperties.keys();
						while (it.hasNext()) {
							String pkey = it.next();
							if (!pkey.isEmpty()) {
								Object ob = jsonProperties.get(pkey);
								if (ob instanceof JSONObject) {
									JSONObject jsonProperty = (JSONObject)ob;
									for (IonProperty.Key kp: IonProperty.Key.values()) {
										if (kp.equals(IonProperty.Key.name)) continue;
										if (kp.equals(IonProperty.Key.mode)) continue;
										if (kp.equals(IonProperty.Key.value)) continue;
										jsonProperty.remove(kp.name());
									}
									jsonProperties.put(pkey, jsonProperty);
								}
							}
						}
						jsonOb.put(Key.properties.name(), jsonProperties);
						continue;
					}
				}
				jsonOb.remove(k.name());
			}
			beanData = jsonOb.toString(1);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return beanData;
	}
	
	public String toString() {
		return mergedJSONObject().toString();
	}
	
	public JSONObject getJSONObject() {
		if (model == null) {
			return jsonBean;
		}
		// a copy, so that the shared model is never modified
		try {
			return JsonUtils.copy(mergedJSONObject());
		} catch (JSONException e) {
			e.printStackTrace();
			return mergedJSONObject();
		}
	}
	
	/** The jsonBean, with the property values of an instance applied on a shallow copy of its model */
	private JSONObject mergedJSONObject() {
		if (model == null) {
			return jsonBean;
		}
		JSONObject json = new JSONObject();
		try {
			@SuppressWarnings("unchecked")
			Iterator<String> it = jsonBean.keys();
			while (it.hasNext()) {
				String key = it.next();
				json.put(key, jsonBean.get(key));
			}
			JSONObject jsonProperties = jsonBean.getJSONObject(Key.properties.name());
			JSONObject properties = new JSONObject();
			@SuppressWarnings("unchecked")
			Iterator<String> itp = jsonProperties.keys();
			while (itp.hasNext()) {
				String pkey = itp.next();
				Object ob = jsonProperties.get(pkey);
				PropertyValue value = propertyValues == null ? null : propertyValues.get(pkey);
				if (value != null) {
					ob = toProperty(pkey, (JSONObject) ob, value).getJSONObject();
				}
				properties.put(pkey, ob);
			}
			json.put(Key.properties.name(), properties);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}
	
	private static IonProperty toProperty(String pkey, JSONObject jsonProperty, PropertyValue value) {
		IonProperty property = new IonProperty(jsonProperty);
		property.setName(pkey);
		if (value != null) {
			property.setMode(value.mode());
			property.setValue(value.value());
		}
		return property;
	}
	
	public String getClassName() {
		try {
			return jsonBean.getString(Key.classname.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement";
		}
	}
	
	public String getTplVersion() {
		try {
			return jsonBean.getString(Key.tplVersion.name());
		} catch (JSONException e) {
			return "1.0.88";
		}
	}
	
	public String getName() {
		try {
			return jsonBean.getString(Key.name.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "bean";
		}
	}
	
	protected void setName(String name) {
		try {
			detach();
			jsonBean.put(Key.name.name(), name);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public String getComponent() {
		try {
			return jsonBean.getString(Key.component.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "component";
		}
	}
	
	public String getDisplayName() {
		try {
			String displayName = jsonBean.getString(Key.displayName.name());
			return displayName.isEmpty() ? getName(): displayName;
		} catch (JSONException e) {
			e.printStackTrace();
			return "?";
		}
	}
	
	public String getDisplayFormat() {
		try {
			return jsonBean.getString(Key.displayFormat.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "?";
		}
	}
	
	public String getTag() {
		try {
			return jsonBean.getString(Key.tag.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "tag";
		}
	}
	
	public String[] getTags() {
		try {
			JSONArray ar = jsonBean.getJSONArray(Key.tags.name());
			String[] tags = new String[0];
			if (ar.length() > 0) {
				tags = new String[ar.length()];
				for (int i=0; i<ar.length(); i++) {
					tags[i] = (String) ar.get(i);
				}
			} else {
				tags = new String[1];
				tags[0] = getTag();
			}
			return tags;
		} catch (JSONException e) {
			e.printStackTrace();
			return new String[0];
		}
	}
	
	public String getGroup() {
		try {
			return jsonBean.getString(Key.group.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "Others";
		}
	}
	public String getDeprecatedTplVersion() {
		try {
			return jsonBean.getString(Key.deprecatedTplVersion.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "";
		}
	}
	public boolean isSelfClose() {
		try {
			return jsonBean.getBoolean(Key.autoClose.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
	}
	public boolean needNgTemplate() {
		try {
			return jsonBean.getBoolean(Key.needNgTemplate.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
	}
	public String getLabel() {
		try {
			return jsonBean.getString(Key.label.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "label";
		}
	}
	public String getDescription() {
		try {
			Object desc = jsonBean.get(Key.description.name());
			String description;
			if (desc instanceof JSONArray) {
				JSONArray descs = ((JSONArray) desc);
				StringBuilder sb = new StringBuilder();
				int len = descs.length();
				for (int i = 0; i < len; i++) {
					sb.append(descs.getString(i));
				}
				description = sb.toString();
			} else {
				description = desc.toString();
			}
			return description;
		} catch (Exception e) {
			e.printStackTrace();
			return "description";
		}
	}
	public String getIcon16() {
		try {
			String icon = jsonBean.getString(Key.icon16.name());
			return icon.isEmpty() ? "default_color_16x16.png" : icon;
		} catch (JSONException e) {
			e.printStackTrace();
			return "default_color_16x16.png";
		}
	}
	public String getIcon32() {
		try {
			String icon = jsonBean.getString(Key.icon32.name());
			return icon.isEmpty() ? "default_color_32x32.png" : icon;
		} catch (JSONException e) {
			e.printStackTrace();
			return "default_color_32x32.png";
		}
	}
	public String getIconColor16Path() {
		return imageFolder + getIcon16();
	}
	public String getIconColor32Path() {
		return imageFolder + getIcon32();
	}
	
	public boolean hasProperty(String propertyName) {
		IonProperty property = getProperty(propertyName);
		return property != null;
	}
	
	public Object getPropertyValue(String propertyName) {
		IonProperty property = getProperty(propertyName);
		if (property != null) {
			return property.getSmartType();
		}
		return null;
	}
	
	public void setPropertyValue(String propertyName, Object propertyValue) {
		IonProperty property = getProperty(propertyName);
		if (property != null) {
			property.setSmartType((MobileSmartSourceType) propertyValue);
			putProperty(property);
		}
	}
	
	public IonProperty getProperty(String propertyName) {
		return getProperties().get(propertyName);
	}
		
	public Map<String, IonProperty> getProperties() {
		Map<String, IonProperty> properties = new HashMap<String, IonProperty>();
		try {
			JSONObject jsonProperties = jsonBean.getJSONObject(Key.properties.name());
			@SuppressWarnings("unchecked")
			Iterator<String> it = jsonProperties.keys();
			while (it.hasNext()) {
				String pkey = it.next();
				if (!pkey.isEmpty()) {
					Object ob = jsonProperties.get(pkey);
					if (ob instanceof JSONObject) {
						PropertyValue value = propertyValues == null ? null : propertyValues.get(pkey);
						IonProperty property = toProperty(pkey, (JSONObject) ob, value);
						properties.put(property.getName(), property);
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return properties;
	}
	
	protected synchronized void putProperty(IonProperty property) {
		try {
			JSONObject jsonProperties = jsonBean.getJSONObject(Key.properties.name());
			if (jsonProperties != null) {
				String name = property.getName();
				Object ob = model == null ? null : jsonProperties.opt(name);
				if (ob instanceof JSONObject jsonProperty && !name.isEmpty()) {
					String mode = property.getMode();
					Object value = property.getValue();
					if (mode.equals(jsonProperty.opt(IonProperty.Key.mode.name())) && value.equals(jsonProperty.opt(IonProperty.Key.value.name()))) {
						if (propertyValues != null) {
							propertyValues.remove(name);
						}
					} else {
						if (propertyValues == null) {
							propertyValues = new HashMap<String, PropertyValue>();
						}
						propertyValues.put(name, new PropertyValue(mode.intern(), value));
					}
				} else {
					detach();
					jsonProperties = jsonBean.getJSONObject(Key.properties.name());
					jsonProperties.put(name, property.getJSONObject());
				}
				beanData = null;
				ionConfig = null;
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/** Makes an instance own a copy of its model, before a change the model must not see */
	private void detach() throws JSONException {
		if (model != null) {
			jsonBean = JsonUtils.copy(mergedJSONObject());
			model = null;
			propertyValues = null;
		}
	}
	
	public IonEvent getEvent(String eventName) {
		return getEvents().get(eventName);
	}
	
	public List<String> getScssList() {
		List<String> scss = new ArrayList<String>();
		try {
			if (jsonBean.has(Key.scss.name())) {
				JSONArray arr = jsonBean.getJSONArray(Key.scss.name());
				for (int i= 0; i < arr.length(); i++) {
					scss.add(arr.getString(i));
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return scss;
	}
	
	public Map<String, IonEvent> getEvents() {
		Map<String, IonEvent> events = new HashMap<String, IonEvent>();
		try {
			if (jsonBean.has(Key.events.name())) {
				JSONObject jsonEvents = jsonBean.getJSONObject(Key.events.name());
				@SuppressWarnings("unchecked")
				Iterator<String> it = jsonEvents.keys();
				while (it.hasNext()) {
					String pkey = it.next();
					if (!pkey.isEmpty()) {
						Object ob = jsonEvents.get(pkey);
						if (ob instanceof JSONObject) {
							IonEvent event = new IonEvent((JSONObject)ob);
							event.setName(pkey);
							events.put(event.getName(), event);
						}
					}
				}
			}
		} catch (JSONException e) {
			System.out.print(jsonBean.toString());
			e.printStackTrace();
		}
		return events;
	}
	
	public IonConfig getConfig() {
		if (ionConfig == null) {
			try {
				JSONObject jsonConfig = jsonBean.getJSONObject(Key.config.name());
				ionConfig = IonConfig.get(jsonConfig);
			} catch (JSONException e) {
				e.printStackTrace();
				ionConfig = IonConfig.get();
			}
		}
		return ionConfig;
	}
	
	protected DatabaseObject createBean() {
		UIDynamicElement dbo = null;
		try {
			Object args[] = { getTag() };
			
			String dboclass = getClassName();
			Class<?> c = Class.forName(dboclass);
			dbo = (UIDynamicElement) c.getConstructor(String.class).newInstance(args);
			
			dbo.setName(StringUtils.normalize(getDisplayName()));//dbo.setName(getName());
			dbo.setSelfClose(isSelfClose());
			dbo.setBeanData(getJSONObject().toString());			
			dbo.bNew = true;
			dbo.hasChanged = true;
		} catch (Exception e) {
			e.printStackTrace();
			dbo = null;
		}
		return dbo;
	}

	public JSONObject getHint() {
		try {
			JSONObject jsonHint = jsonBean.getJSONObject(Key.hint.name());
			return JsonUtils.copy(jsonHint);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new JSONObject();
	}
	 
}
	