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

public class UIControlCustomSource extends UIControlSource {
	
	private static final long serialVersionUID = 6307932237163058850L;

	public UIControlCustomSource() {
		super();
	}
	
	@Override
	public UIControlCustomSource clone() throws CloneNotSupportedException {
		UIControlCustomSource cloned = (UIControlCustomSource)super.clone();
		return cloned;
	}
	
	/*
	 * The directive value
	 */
	private String sourceValue = "";
	
	public String getSourceValue() {
		return sourceValue;
	}

	public void setSourceValue(String sourceValue) {
		this.sourceValue = sourceValue;
	}

	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			String computedTemplate = getSourceValue();
			return computedTemplate.isEmpty() ? "":computedTemplate;
		}
		return "";
	}
	
	@Override
	public String toString() {
		String label = getSourceValue();
		return label.isEmpty() ? "?":label;
	}
}
