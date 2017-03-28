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

public class UIControlDirectiveValue extends UIComponent {
	
	private static final long serialVersionUID = 6307932237163058850L;

	public UIControlDirectiveValue() {
		super();
	}
	
	@Override
	public UIControlDirectiveValue clone() throws CloneNotSupportedException {
		UIControlDirectiveValue cloned = (UIControlDirectiveValue)super.clone();
		return cloned;
	}
	
	/*
	 * The directive value
	 */
	private String directiveValue = "";
	
	public String getDirectiveValue() {
		return directiveValue;
	}

	public void setDirectiveValue(String directiveValue) {
		this.directiveValue = directiveValue;
	}

	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			String computedTemplate = getDirectiveValue();
			return computedTemplate.isEmpty() ? "":computedTemplate;
		}
		return "";
	}
	
	@Override
	public String toString() {
		String label = getDirectiveValue();
		return label.isEmpty() ? "?":label;
	}
}
