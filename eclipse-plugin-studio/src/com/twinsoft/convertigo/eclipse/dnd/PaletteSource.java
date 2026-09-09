/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.dnd;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;

public class PaletteSource {
	private String xmlData = null;
	private DatabaseObject dbo = null;
	private String flowItemType = null;
	private String flowItemDescription = null;
	private String flowItemData = null;
	
	public PaletteSource(DatabaseObject dbo) {
		this.dbo = dbo;
	}

	private PaletteSource(String flowItemType, String flowItemData, String flowItemDescription) {
		this.flowItemType = flowItemType;
		this.flowItemData = flowItemData;
		this.flowItemDescription = flowItemDescription;
	}

	public static PaletteSource flowItem(String itemData, String description) {
		return new PaletteSource("virtualItem", itemData, description);
	}
	
	public String getXmlData() {
		if (xmlData == null) {
			xmlData = "<xml/>";
		}
		return xmlData;
	}

	public String getBrowserDragData() {
		if (!isFlowItem() || flowItemData == null || flowItemData.isBlank()) {
			return getXmlData();
		}
		try {
			return new JSONObject()
					.put("type", "paletteData")
					.put("data", new JSONObject(flowItemData))
					.put("options", new JSONObject())
					.toString();
		} catch (Exception e) {
			return getXmlData();
		}
	}
	
	public DatabaseObject getDatabaseObject() {
		return dbo;
	}

	public boolean isFlowItem() {
		return "virtualItem".equals(flowItemType);
	}

	public String getFlowItemDescription() {
		return flowItemDescription;
	}

	public String getFlowItemData() {
		return flowItemData;
	}
}
