/*
 * Copyright (c) 2001-2018 Convertigo SA.
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
 */

package com.twinsoft.convertigo.beans.mobile.components;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.EngineException;

public class UICustom extends UIComponent {
	
	private static final long serialVersionUID = -6407500310571517432L;

	public UICustom() {
		super();
	}

	@Override
	public UICustom clone() throws CloneNotSupportedException {
		UICustom cloned = (UICustom) super.clone();
		return cloned;
	}

	@Override
	public void add(DatabaseObject databaseObject) throws EngineException {
		add(databaseObject, null);
	}
	
    @Override
    public void add(DatabaseObject databaseObject, Long after) throws EngineException {
        throw new EngineException("You cannot add to a custom component a database object of type " + databaseObject.getClass().getName());
    }

	protected String htmlTemplate = "";

	public String getCustomTemplate() {
		return htmlTemplate;
	}

	public void setCustomTemplate(String htmlTemplate) {
		this.htmlTemplate = htmlTemplate;
	}
	
	@Override
	public String computeTemplate() {
		if (isEnabled())
			return getCustomTemplate();
		else
			return "";
	}

}
