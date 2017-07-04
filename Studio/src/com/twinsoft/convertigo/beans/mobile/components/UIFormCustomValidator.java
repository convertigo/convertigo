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
		return  "\t\t\t//return c.value !== '' ? null : {\n"	+
				"\t\t\t//\t__functionName__: {\n"				+
				"\t\t\t//\t\tvalid: false\n"					+
				"\t\t\t//\t}\n"									+
				"\t\t\t//}\n"									+
				"\t\t\treturn null;//means valid";

	}
	
	/*
	 * The validator name (function name)
	 */
	private String validatorName = "";
	
	public String getValidatorName() {
		return validatorName;
	}

	public void setValidatorName(String validatorName) {
		this.validatorName = validatorName;
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
			return validatorName;
		}
		return "";
	}

	@Override
	public String computeFunction() {
		if (isEnabled()) {
			if (!validatorName.isEmpty()) {
				String computed = "";
				String parameter = getParent() instanceof UIForm ? "g: FormGroup":"c: FormControl";
				computed += "\t\tfunction "+ this.validatorName +"("+parameter+") {"+ System.lineSeparator();
				computed += computeValidatorContent();
				computed += System.lineSeparator() + "\t\t}";
				computed += System.lineSeparator();
				return computed;
			}
		}
		return "";
	}

	@Override
	public String computeTemplate() {
		return "";
	}

	private String computeValidatorContent() {
		String s = "";
		s += "\t\t/*Begin_c8o_function:"+ this.validatorName +"*/" + System.lineSeparator();
		s += validatorValue.getString().replace("__functionName__", validatorName) + System.lineSeparator();
		s += "\t\t/*End_c8o_function:"+ this.validatorName +"*/";
		return s;
	}

	@Override
	public String toString() {
		return validatorName.isEmpty() ? "?" : validatorName + "()";
	}
	
}
