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

	public String getFormGroupName() {
		return "form"+ this.priority;
	}

	@Override
	protected StringBuilder initAttributes() {
		StringBuilder attributes = super.initAttributes();
		String formGroupName = getFormGroupName();
		attributes.append(" [formGroup]=").append("\"").append(formGroupName).append("\"")
					.append(" novalidate");
		return attributes;
	}	
	
	@Override
	public String computeJsonModel() {
		if (isEnabled()) {
			StringBuilder models = new StringBuilder();
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof UIElement) {
					String model = ((UIElement)component).computeJsonModel();
					models.append(models.length() > 0 && !model.isEmpty() ? ",":"").append(model);
				}
			}
			StringBuilder sb = new StringBuilder();
			if (models.length() > 0) {
				sb.append("{")
				  .append("\"controls\":{").append(models).append("}").append(",")
				  .append("\"errors\":\"\"").append(",")
				  .append("\"status\":\"\"").append(",")
				  .append("\"valid\":\"\"").append(",")
				  .append("\"value\":\"\"")
				  .append("}");
				return sb.toString();
			}
		}
		return "";
	}
	
	@Override
	protected String computeConstructor() {
		if (isEnabled()) {
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
			
			StringBuilder cartridge = new StringBuilder();
			cartridge.append("\t\t/**").append(System.lineSeparator())
						.append("\t\t * "+ getName()).append(System.lineSeparator());
			for (String commentLine : getComment().split(System.lineSeparator())) {
				cartridge.append("\t\t *   ").append(commentLine).append(System.lineSeparator());
			}
			cartridge.append("\t\t */").append(System.lineSeparator());
			
			StringBuilder sb = new StringBuilder();
			String formGroupName = getFormGroupName();
			sb.append(cartridge);
			sb.append("\t\tthis."+ formGroupName + " = new FormGroup({");
			sb.append(constructors).append(System.lineSeparator()).append("\t\t}");
			sb.append(validator.length() > 0 ? ",":"").append(validator).append(");");
			sb.append(System.lineSeparator());
			return sb.toString().replaceAll(",+", ",");
		}
		return "";
	}
	
	@Override
	protected String computeFunction() {
		if (isEnabled()) {
			StringBuilder sb = new StringBuilder();
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
		return "";
	}
	
	@Override
	public void computeScripts(JSONObject jsonScripts) {
		IScriptComponent main = getMainScriptComponent();
		if (main == null) {
			return;
		}
		
		if (isEnabled()) {
			try {
				String imports = jsonScripts.getString("imports");
				if (main.addImport("FormGroup", "@angular/forms")) {
					imports += "import { FormGroup } from '@angular/forms';" + System.lineSeparator();
				}
				if (main.addImport("FormControl", "@angular/forms")) {
					imports += "import { FormControl } from '@angular/forms';" + System.lineSeparator();
				}
				if (main.addImport("Validators", "@angular/forms")) {
					imports += "import { Validators } from '@angular/forms';" + System.lineSeparator();
				}
				
				jsonScripts.put("imports", imports);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			try {
				String declarations = jsonScripts.getString("declarations");
				
				String dname = "d_" + getFormGroupName();
				String dcode = "public "+ getFormGroupName() + " :  FormGroup;";
				if (main.addDeclaration(dname, dcode)) {
					declarations += dcode + System.lineSeparator();
				}
				jsonScripts.put("declarations", declarations);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			try {
				String constructors = jsonScripts.getString("constructors");
				
				String fname = "f_"+ getFormGroupName();
				String fcode = computeFunction();
				if (main.addConstructor(fname, fcode)) {
					constructors += fcode + System.lineSeparator();
				}
				jsonScripts.put("constructors", constructors);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			try {
				String constructors = jsonScripts.getString("constructors");
				
				String cname = "c_"+ getFormGroupName();
				String ccode = computeConstructor();
				if (main.addConstructor(cname, ccode)) {
					constructors += ccode + System.lineSeparator();
				}
				jsonScripts.put("constructors", constructors);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			super.computeScripts(jsonScripts);
		}
	}
}
