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

package com.twinsoft.convertigo.beans.ngx.components;

import com.twinsoft.convertigo.beans.core.DatabaseObject.DboFolderType;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.FolderType;

@DboFolderType(type = FolderType.ATTRIBUTE)
public class UIDynamicAttr extends UIDynamicElement {

	private static final long serialVersionUID = -4701183821950621927L;

	public UIDynamicAttr() {
		super();
		selfClose = true;
	}
	
	public UIDynamicAttr(String tagName) {
		super(tagName);
		selfClose = true;
	}

	@Override
	public UIDynamicAttr clone() throws CloneNotSupportedException {
		UIDynamicAttr cloned = (UIDynamicAttr) super.clone();
		return cloned;
	}

	@Override
	protected void addUIComponent(UIComponent uiComponent, Long after) throws EngineException {
         throw new EngineException("You cannot add component to this component");
	}
	
	@Override
	public String computeTemplate() {
		return "";
	}
}
