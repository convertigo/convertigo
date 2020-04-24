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

package com.twinsoft.convertigo.beans.ngx.components.dynamic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class IonBean {
	
	enum Key {
		classname,
		tplVersion,
		tag,
		tags,
		name,
		displayName,
		label,
		autoClose,
		group,
		description,
		icon16,
		icon32,
		properties,
		events,
		config
		;
	}
	
	private JSONObject jsonBean;
	private String beanData;
	private IonConfig ionConfig;
	
	public IonBean() {
		try {
			jsonBean = new JSONObject()
				.put(Key.classname.name(), "com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement")
				.put(Key.tplVersion.name(), "1.0.88")
				.put(Key.name.name(), "bean")
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
				.put(Key.config.name(), new JSONObject())
				;
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public IonBean(String jsonString) throws JSONException {
		this();
		JSONObject jsonOb = new JSONObject(jsonString);
		for (Key k: Key.values()) {
			if (jsonOb.has(k.name())) {
				try {
					jsonBean.put(k.name(), jsonOb.get(k.name()));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public synchronized String toBeanData() {
		if (beanData != null) {
			return beanData;
		}
		beanData = jsonBean.toString();
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
		return jsonBean.toString();
	}
	
	public JSONObject getJSONObject() {
		return jsonBean;
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
			jsonBean.put(Key.name.name(), name);
		} catch (JSONException e) {
			e.printStackTrace();
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
	public boolean isSelfClose() {
		try {
			return jsonBean.getBoolean(Key.autoClose.name());
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
		return "/com/twinsoft/convertigo/beans/mobile/components/dynamic/images/"+ getIcon16();
	}
	public String getIconColor32Path() {
		return "/com/twinsoft/convertigo/beans/mobile/components/dynamic/images/"+ getIcon32();
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
						IonProperty property = new IonProperty((JSONObject)ob);
						property.setName(pkey);
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
				jsonProperties.put(property.getName(), property.getJSONObject());
				beanData = null;
				ionConfig = null;
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public IonEvent getEvent(String eventName) {
		return getEvents().get(eventName);
	}
	
	public Map<String, IonEvent> getEvents() {
		Map<String, IonEvent> events = new HashMap<String, IonEvent>();
		try {
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
		} catch (JSONException e) {
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
	 
}
