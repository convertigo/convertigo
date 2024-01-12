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

package com.twinsoft.convertigo.beans.core;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboFolderType;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

@DboCategoryInfo(
		getCategoryId = "Listener",
		getCategoryName = "Listener",
		getIconClassCSS = "convertigo-action-newListener"
	)
@DboFolderType(type = FolderType.LISTENER)
public abstract class Listener extends DatabaseObject implements IEnableAble {

	private static final long serialVersionUID = 9010595374464464408L;
	
	private boolean isEnabled = true;
	
	public Listener() {
		super();
		databaseType = "Listener";
	}

	@Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);
		
		String version = element.getAttribute("version");
		if (VersionUtils.compare(version, "7.5.0") < 0) {
			NodeList properties = element.getElementsByTagName("property");
			
			Element propName = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "isEnable");
			if (propName != null) {
				propName.setAttribute("name", "isEnabled");
				hasChanged = true;
			}

			Engine.logBeans.warn("[Listener] The object \"" + getName() + "\" has been updated to version 7.5.0 (property \"isEnable\" changed to \"isEnabled\")");
		}
	}
	
	@Override
	public Listener clone() throws CloneNotSupportedException {
		Listener clonedObject =  (Listener) super.clone();
		return clonedObject;
	}
	
	public String getRenderer() {
		return "ListenerTreeObject";
	}
	
	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Override
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
	
	@Override
	public boolean testAttribute(String name, String value) {
		if (name.equals("isEnable")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isEnabled()));
		}
		return super.testAttribute(name, value);
	}
}
