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

import com.twinsoft.convertigo.beans.common.FormatedContent;

public class UIFormCustomValidator extends UIFormValidator {

	private static final long serialVersionUID = 6390440293477523849L;

	public UIFormCustomValidator() {
		super();
	}

	@Override
	public UIFormCustomValidator clone() throws CloneNotSupportedException {
		UIFormCustomValidator cloned = (UIFormCustomValidator) super.clone();
		return cloned;
	}
	
	private static String sample() {
		return  "\t\t\t//For FormControl: you can access control value using : c.value\n" +
				"\t\t\t//For FormGroup: you can access control value using : g.get('<control_name>').value\n" +
				"\t\t\t//return any json structure to specify errors\n"	+
				"\t\t\t//return null if valid\n"	+
				"\t\t\treturn null;//means valid";
	}
	
	public String getValidatorName() {
		return "validate"+ this.priority;
	}
	
	/*
	 * The validator value (function contents)
	 */
	private FormatedContent validatorValue = new FormatedContent(sample());
	
	public FormatedContent getValidatorValue() {
		return validatorValue;
	}

	public void setValidatorValue(FormatedContent validatorValue) {
		this.validatorValue = validatorValue;
	}

	@Override
	public String computeConstructor() {
		if (isEnabled()) {
			return getValidatorName();
		}
		return "";
	}

	@Override
	public String computeFunction() {
		if (isEnabled()) {
			boolean underForm = getParent() instanceof UIForm;
			String validatorName = getValidatorName();

			StringBuilder cartridge = new StringBuilder();
			cartridge.append("\t\t/**").append(System.lineSeparator())
						.append("\t\t * "+ getName()).append(System.lineSeparator());
			for (String commentLine : getComment().split(System.lineSeparator())) {
				cartridge.append("\t\t *   ").append(commentLine).append(System.lineSeparator());
			}
			cartridge.append("\t\t * ").append(System.lineSeparator());
			
			if (underForm)
				cartridge.append("\t\t * @param g:FormGroup, the form").append(System.lineSeparator());
			else
				cartridge.append("\t\t * @param c:FormControl, the control").append(System.lineSeparator());
			cartridge.append("\t\t */").append(System.lineSeparator());
			
			String parameter =  underForm ? "g: FormGroup":"c: FormControl";
			
			String computed = "";
			computed += System.lineSeparator();
			computed += cartridge;
			computed += "\t\tfunction "+ validatorName +"("+parameter+") {"+ System.lineSeparator();
			computed += computeValidatorContent();
			computed += System.lineSeparator() + "\t\t}";
			computed += System.lineSeparator();
			return computed;
		}
		return "";
	}

	@Override
	public String computeTemplate() {
		return "";
	}

	private String computeValidatorContent() {
		String validatorName = getValidatorName();
		String s = "";
		s += "\t\t/*Begin_c8o_function:"+ validatorName +"*/" + System.lineSeparator();
		s += validatorValue.getString() + System.lineSeparator();
		s += "\t\t/*End_c8o_function:"+ validatorName +"*/";
		return s;
	}

}
