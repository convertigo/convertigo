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

import org.apache.commons.lang3.StringEscapeUtils;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.EngineException;

public class UIText extends UIComponent {
	
	private static final long serialVersionUID = 4062617301596626610L;

	public UIText() {
		super();
	}

	@Override
	public UIText clone() throws CloneNotSupportedException {
		UIText cloned = (UIText) super.clone();
		return cloned;
	}

	private String textValue = "some text";
	
	public String getTextValue() {
		return textValue;
	}

	public void setTextValue(String textValue) {
		this.textValue = textValue;
	}

	@Override
	public void add(DatabaseObject databaseObject) throws EngineException {
		throw new EngineException("You cannot add to a text component a database object of type " + databaseObject.getClass().getName());
	}

	@Override
	public String computeTemplate() {
		if (isEnabled())
			return StringEscapeUtils.escapeHtml4(getTextValue()) + System.getProperty("line.separator");
		else
			return "";
	}

}
