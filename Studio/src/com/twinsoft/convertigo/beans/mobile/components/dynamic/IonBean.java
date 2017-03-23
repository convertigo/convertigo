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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicElement;

public class IonBean {
	
	enum Key {
		tag,
		name,
		label,
		autoClose,
		group,
		description,
		icon16,
		icon32,
		properties
		;
	}
	
	private JSONObject jsonBean;
	
	public IonBean() {
		try {
			jsonBean = new JSONObject()
				.put(Key.name.name(), "bean")
				.put(Key.tag.name(), "tag")
				.put(Key.label.name(), "label")
				.put(Key.autoClose.name(), false)
				.put(Key.group.name(), "Components")
				.put(Key.description.name(), "description")
				.put(Key.icon16.name(), "default_color_16x16.png")
				.put(Key.icon32.name(), "default_color_32x32.png")
				.put(Key.properties.name(), new JSONObject());
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
	
	public String toString() {
		return jsonBean.toString();
	}
	
	public JSONObject getJSONObject() {
		return jsonBean;
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
	
	public String getTag() {
		try {
			return jsonBean.getString(Key.tag.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "tag";
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
			return jsonBean.getString(Key.description.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "description";
		}
	}
	public String getIcon16() {
		try {
			return jsonBean.getString(Key.icon16.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "default_color_16x16.png";
		}
	}
	public String getIcon32() {
		try {
			return jsonBean.getString(Key.icon32.name());
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
			Object value = property.getValue();
			return value.equals(false) ? "not set":value.toString();
		}
		return null;
	}
	
	public void setPropertyValue(String propertyName, Object propertyValue) {
		IonProperty property = getProperty(propertyName);
		if (property != null) {
			if (propertyValue instanceof String) {
				if (propertyValue.equals("not set")) {
					propertyValue = false;
				}
				property.setValue(propertyValue);
				putProperty(property);
			}
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
						properties.put(property.getName(), property);
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return properties;
	}
	
	protected void putProperty(IonProperty property) {
		try {
			JSONObject jsonProperties = jsonBean.getJSONObject(Key.properties.name());
			if (jsonProperties != null) {
				jsonProperties.put(property.getName(), property.getJSONObject());
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected DatabaseObject createBean() {
		UIDynamicElement dbo = null;
		try {
			dbo = new UIDynamicElement(getTag());
			dbo.setName(getName());
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
