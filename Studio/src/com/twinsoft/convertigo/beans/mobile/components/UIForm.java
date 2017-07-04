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

package com.twinsoft.convertigo.beans.mobile.components;

import java.util.Iterator;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class UIForm extends UIElement {

	private static final long serialVersionUID = 4115423575037640826L;

	public UIForm() {
		super("form");
	}

	@Override
	public UIForm clone() throws CloneNotSupportedException {
		UIForm cloned = (UIForm) super.clone();
		return cloned;
	}

	private String formGroupName = "fgForm";

	public String getFormGroupName() {
		return formGroupName;
	}

	public void setFormGroupName(String formGroupName) {
		this.formGroupName = formGroupName;
	}
	
	@Override
	public String toString() {
		return "form : "+ (formGroupName.isEmpty() ? "?":formGroupName);
	}

	@Override
	protected StringBuilder initAttributes() {
		StringBuilder attributes = super.initAttributes();
		if (!formGroupName.isEmpty()) {
			attributes.append(" [formGroup]=").append("\"").append(formGroupName).append("\"")
						.append(" novalidate");
		}
		return attributes;
	}	
	
	@Override
	protected String computeConstructor() {
		if (isEnabled()) {
			StringBuilder sb = new StringBuilder();
			String formGroupName = getFormGroupName();
			if (!formGroupName.isEmpty()) {
				StringBuilder constructors = new StringBuilder();
				StringBuilder validator = new StringBuilder();
				Iterator<UIComponent> it = getUIComponentList().iterator();
				while (it.hasNext()) {
					UIComponent component = (UIComponent)it.next();
					if (component instanceof UIElement) {
						String constructor = ((UIElement)component).computeConstructor();
						constructors.append(constructors.length() > 0 && !constructor.isEmpty() ? ",":"").append(constructor);
					} else if (component instanceof UIFormCustomValidator) {
						if (validator.length() == 0) {// only one
							validator.append(((UIFormCustomValidator)component).computeConstructor());
						}
					}
				}
				
				sb.append("this."+ formGroupName + " = new FormGroup({");
				sb.append(constructors).append(System.lineSeparator()).append("\t\t}");
				sb.append(validator.length() > 0 ? ",":"").append(validator).append(");");
				sb.append(System.lineSeparator());
				return sb.toString();
			}
		}
		return "";
	}
	
	@Override
	protected String computeFunction() {
		if (isEnabled()) {
			StringBuilder sb = new StringBuilder();
			String formGroupName = getFormGroupName();
			if (!formGroupName.isEmpty()) {
				Iterator<UIComponent> it = getUIComponentList().iterator();
				while (it.hasNext()) {
					UIComponent component = (UIComponent)it.next();
					if (component instanceof UIElement) {
						sb.append(((UIElement)component).computeFunction());
					} else if (component instanceof UIFormCustomValidator) {
						sb.append(((UIFormValidator)component).computeFunction());
					}
				}
				return sb.toString();
			}
		}
		return "";
	}
	
	@Override
	public void computeScripts(JSONObject jsonScripts) {
		if (isEnabled()) {
			try {
				String imports = jsonScripts.getString("imports");
				
				String search = "import { FormGroup, FormControl, Validators}\tfrom '@angular/forms';";
				if (imports.indexOf(search) == -1) {
					imports += search + System.lineSeparator();
				}
				jsonScripts.put("imports", imports);
			} catch (JSONException e) {
				e.printStackTrace();
			}
	
			if (!formGroupName.isEmpty()) {
				String declaration = "public "+ formGroupName + " :  FormGroup;" + System.lineSeparator();
				try {
					String declarations = jsonScripts.getString("declarations") + declaration;
					jsonScripts.put("declarations", declarations);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				String constructor = computeConstructor() + System.lineSeparator();
				try {
					String constructors = jsonScripts.getString("constructors") + constructor;
					jsonScripts.put("constructors", constructors);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				String function = computeFunction() + System.lineSeparator();
				try {
					String constructors = jsonScripts.getString("constructors") + function;
					jsonScripts.put("constructors", constructors);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			super.computeScripts(jsonScripts);
		}
	}
}
