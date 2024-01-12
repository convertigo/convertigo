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

package com.twinsoft.convertigo.beans.mobile.components;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.EngineException;

public class UIFormControlValidator extends UIFormValidator implements ITagsProperty {

	private static final long serialVersionUID = 6681781152189937335L;

	public UIFormControlValidator() {
		super();
	}

	@Override
	public UIFormControlValidator clone() throws CloneNotSupportedException {
		UIFormControlValidator cloned = (UIFormControlValidator) super.clone();
		return cloned;
	}
	
	private String required = "not set";
	
	public String getRequired() {
		return required;
	}

	public void setRequired(String required) {
		this.required = required;
	}

	private String requiredTrue = "not set";
	
	public String getRequiredTrue() {
		return requiredTrue;
	}

	public void setRequiredTrue(String requiredTrue) {
		this.requiredTrue = requiredTrue;
	}
	
	private String email = "not set";
	
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	private String minLength = "not set";
	
	public String getMinLength() {
		return minLength;
	}

	public void setMinLength(String minLength) {
		this.minLength = minLength;
	}

	private String maxLength = "not set";
	
	public String getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(String maxLength) {
		this.maxLength = maxLength;
	}
	
	private String pattern = "not set";
	
	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	@Override
	public void add(DatabaseObject databaseObject) throws EngineException {
		add(databaseObject, null);
	}
	
    @Override
    public void add(DatabaseObject databaseObject, Long after) throws EngineException {
        throw new EngineException("You cannot add to a validator a database object of type " + databaseObject.getClass().getName());
    }

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("required") 
				|| propertyName.equals("requiredTrue")
					|| propertyName.equals("email")) {
			return new String[] {"not set", "set"};
		}
		if (propertyName.equals("minLength")
				|| propertyName.equals("maxLength")
					|| propertyName.equals("pattern")) {
			return new String[] {"not set"};
		}
		return new String[0];
	}
    
	@Override
	public String computeConstructor() {
		if (isEnabled()) {
			StringBuffer validators = new StringBuffer();
			if (!required.equals("not set")) {
				validators.append(validators.length() > 0 ? ", ":"").append("Validators.required");
			}
			if (!requiredTrue.equals("not set")) {
				validators.append(validators.length() > 0 ? ",":"").append("Validators.requiredTrue");
			}
			if (!email.equals("not set")) {
				validators.append(validators.length() > 0 ? ",":"").append("Validators.email");
			}
			if (!minLength.equals("not set")) {
				validators.append(validators.length() > 0 ? ",":"").append("Validators.minLength("+ minLength.toString() +")");
			}
			if (!maxLength.equals("not set")) {
				validators.append(validators.length() > 0 ? ",":"").append("Validators.maxLength("+ maxLength.toString() +")");
			}
			if (!pattern.equals("not set")) {
				validators.append(validators.length() > 0 ? ",":"").append("Validators.pattern('"+ pattern.toString() +"')");
			}
			return validators.toString();
		}
		return "";
	}
	
	@Override
	public String computeFunction() {
		return "";
	}
	
	@Override
	public String computeTemplate() {
		return "";
	}
}
