/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType.Mode;

public class IonProperty implements Cloneable {
	
	enum Key {
		name,
		attr,
		label,
		editor,
		hidden,
		composite,
		description,
		category,
		mode,
		type,
		value,
		values
		;
	}
	
	private JSONObject jsonProperty;
	
	public IonProperty() {
		try {
			jsonProperty = new JSONObject()
				.put(Key.name.name(), "property")
				.put(Key.attr.name(), "")
				.put(Key.label.name(), "label")
				.put(Key.editor.name(), "")
				.put(Key.hidden.name(), false)
				.put(Key.composite.name(), false)
				.put(Key.description.name(), "description")
				.put(Key.category.name(), "Attributes")
				.put(Key.mode.name(), "plain")
				.put(Key.type.name(), "string")
				.put(Key.value.name(), false)
				.put(Key.values.name(), new JSONArray().put(false).put(true));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public IonProperty(JSONObject jsonOb) {
		this();
		for (Key k: Key.values()) {
			if (jsonOb.has(k.name())) {
				try {
					jsonProperty.put(k.name(), jsonOb.get(k.name()));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public JSONObject getJSONObject() {
		return jsonProperty;
	}
	
	public String getName() {
		try {
			return jsonProperty.getString(Key.name.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "property";
		}
	}
	
	protected void setName(String name) {
		try {
			jsonProperty.put(Key.name.name(), name);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public String getLabel() {
		try {
			return jsonProperty.getString(Key.label.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "label";
		}
	}
	
	public String getEditor() {
		try {
			return jsonProperty.getString(Key.editor.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public boolean isHidden() {
		try {
			return jsonProperty.getBoolean(Key.hidden.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean isComposite() {
		try {
			return jsonProperty.getBoolean(Key.composite.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public String getAttr() {
		try {
			return jsonProperty.getString(Key.attr.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "";
		}
	}
	public String getDescription() {
		try {
			Object desc = jsonProperty.get(Key.description.name());
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
		} catch (JSONException e) {
			e.printStackTrace();
			return "description";
		}
	}
	public String getCategory() {
		try {
			return jsonProperty.getString(Key.category.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "category";
		}
	}
	public Object[] getValues() {
		try {
			JSONArray ar = jsonProperty.getJSONArray(Key.values.name());
			Object[] values = new Object[ar.length()];
			for (int i=0; i<ar.length(); i++) {
				values[i] = ar.get(i);
			}
			return values;
		} catch (JSONException e) {
			e.printStackTrace();
			return new Object[]{false,true};
		}
	}
	
	public Object getValue() {
		try {
			return jsonProperty.get(Key.value.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void setValue(Object value) {
		try {
			jsonProperty.put(Key.value.name(), value);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public String getType() {
		try {
			return jsonProperty.getString(Key.type.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "string";
		}
	}
	
	public String getMode() {
		try {
			return jsonProperty.getString(Key.mode.name());
		} catch (JSONException e) {
			e.printStackTrace();
			return "plain";
		}
	}
	
	public void setMode(String mode) {
		try {
			jsonProperty.put(Key.mode.name(), mode);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public MobileSmartSourceType getSmartType() {
		MobileSmartSourceType msst = new MobileSmartSourceType() {
			
			private static final long serialVersionUID = 5907963275354985836L;
			
			@Override
			public Object getEditorData() {
				String smartValue = getSmartValue();
				return smartValue.equals("not set") ? "":super.getEditorData();
			}
			
		};
		
		String mode = getMode();
		msst.setMode(Mode.valueOf(mode.toUpperCase()));
		Object value = getValue();
		msst.setSmartValue(value.equals(false) ? "not set" : value.toString());
		return msst;
	}
	
	public void setSmartType(MobileSmartSourceType msst) {
		if (msst != null) {
			String mode = msst.getMode().name();
			setMode(mode.toLowerCase());
			String smartValue = msst.getSmartValue();
			setValue(smartValue.equals("not set") ? false : smartValue);
		}
	}
	
	protected boolean isAttrPropertyBind() {
		String attr = getAttr();
		return attr.startsWith("[") && attr.endsWith("]");
	}
	
	protected boolean isAttrEventBind() {
		String attr = getAttr();
		return attr.startsWith("(") && attr.endsWith(")");
	}
	
	public String getSmartValue() {
		MobileSmartSourceType msst = getSmartType();
		String value = msst.getValue();
		if (isAttrPropertyBind()) {
			if (Mode.PLAIN.equals(msst.getMode())) {
				if (getType().equalsIgnoreCase("string")) {
					if (!value.startsWith("'") && !value.endsWith("'")) {
						value = "'" + MobileSmartSourceType.escapeStringForTpl(value) + "'";
					}
				}
			}
		} else if (isAttrEventBind()) {
			
		} else {
			if (!Mode.PLAIN.equals(msst.getMode())) {
				value = "{{" + value + "}}";
			}
		}
		return value;
	}
	
	public String toString() {
		return jsonProperty.toString();
	}
}
