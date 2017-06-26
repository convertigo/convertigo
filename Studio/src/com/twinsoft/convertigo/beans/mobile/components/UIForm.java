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

	@Override
	public String toString() {
		return "form : "+ getFormName();
	}

	protected String getFormName() {
		return "fg"+ getName();
	}

	private String getDeclaration() {
		return "public "+ getFormName() + " =  new FormGroup({});" + System.lineSeparator();
	}
	
	@Override
	protected StringBuilder initAttributes() {
		StringBuilder attributes = new StringBuilder();
		attributes.append(" [formGroup]=").append("\"").append(getFormName()).append("\"")
					.append(" novalidate");
		return attributes;
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
	
			String declaration = getDeclaration();
			try {
				String declarations = jsonScripts.getString("declarations") + declaration;
				jsonScripts.put("declarations", declarations);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			super.computeScripts(jsonScripts);
		}
	}
	
}
