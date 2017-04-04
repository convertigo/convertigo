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

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.EngineException;

public class UIStyle extends UIComponent {

	private static final long serialVersionUID = -2762443938903661515L;

	public UIStyle() {
		super();
	}

	@Override
	public UIStyle clone() throws CloneNotSupportedException {
		UIStyle cloned = (UIStyle) super.clone();
		return cloned;
	}
	
	@Override
	public void add(DatabaseObject databaseObject) throws EngineException {
		throw new EngineException("You cannot add to a custom component a database object of type " + databaseObject.getClass().getName());
	}

	protected String styleContent = "";

	public String getStyleContent() {
		return styleContent;
	}

	public void setStyleContent(String styleContent) {
		this.styleContent = styleContent;
	}
	
	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			if (!styleContent.isEmpty())
				return styleContent;
		}
		return "";
	}
}
