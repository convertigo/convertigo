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
	private String flowBlockName = null;
	private String flowRuntime = null;
	private String flowBlockDescription = null;
	private String flowItemData = null;
	
	public PaletteSource(DatabaseObject dbo) {
		this.dbo = dbo;
	}

	private PaletteSource(String flowBlockName, String flowBlockDescription) {
		this.flowItemType = "node";
		this.flowBlockName = flowBlockName;
		this.flowBlockDescription = flowBlockDescription;
	}

	private PaletteSource(String flowItemType, String flowBlockName, String flowRuntime, String flowBlockDescription) {
		this.flowItemType = flowItemType;
		this.flowBlockName = flowBlockName;
		this.flowRuntime = flowRuntime;
		this.flowBlockDescription = flowBlockDescription;
	}

	private PaletteSource(String flowItemType, String flowItemData, String flowBlockDescription) {
		this.flowItemType = flowItemType;
		this.flowItemData = flowItemData;
		this.flowBlockDescription = flowBlockDescription;
	}

	public static PaletteSource flowBlock(String blockName, String description) {
		return new PaletteSource(blockName, description);
	}

	public static PaletteSource flowBlockDefinition(String runtime, String description) {
		return new PaletteSource("blockDefinition", "", runtime, description);
	}

	public static PaletteSource flowTypeDefinition(String description) {
		return new PaletteSource("typeDefinition", "", "", description);
	}

	public static PaletteSource flowPropertyDefinition(String description) {
		return new PaletteSource("propertyDefinition", "", "", description);
	}

	public static PaletteSource flowHelperDefinition(String description) {
		return new PaletteSource("helperDefinition", "", "", description);
	}

	public static PaletteSource frontendBlock(String itemData, String description) {
		return new PaletteSource("frontendBlock", itemData, description);
	}

	public static PaletteSource frontendBlockDefinition(String runtime, String description) {
		return new PaletteSource("frontendBlockDefinition", "", runtime, description);
	}
	
	public String getXmlData() {
		if (xmlData == null) {
			xmlData = "<xml/>";
		}
		return xmlData;
	}

	public String getBrowserDragData() {
		if (!isFrontendBlock() || flowItemData == null || flowItemData.isBlank()) {
			return getXmlData();
		}
		try {
			return new JSONObject()
					.put("type", "paletteData")
					.put("data", new JSONObject(flowItemData).put("type", "FrontendBlock"))
					.put("options", new JSONObject())
					.toString();
		} catch (Exception e) {
			return getXmlData();
		}
	}
	
	public DatabaseObject getDatabaseObject() {
		return dbo;
	}

	public boolean isFlowBlock() {
		return "node".equals(flowItemType) && flowBlockName != null;
	}

	public boolean isFlowBlockDefinition() {
		return "blockDefinition".equals(flowItemType);
	}

	public boolean isFlowTypeDefinition() {
		return "typeDefinition".equals(flowItemType);
	}

	public boolean isFlowPropertyDefinition() {
		return "propertyDefinition".equals(flowItemType);
	}

	public boolean isFlowHelperDefinition() {
		return "helperDefinition".equals(flowItemType);
	}

	public boolean isFrontendBlock() {
		return "frontendBlock".equals(flowItemType);
	}

	public boolean isFrontendBlockDefinition() {
		return "frontendBlockDefinition".equals(flowItemType);
	}

	public String getFlowBlockName() {
		return flowBlockName;
	}

	public String getFlowBlockDescription() {
		return flowBlockDescription;
	}

	public String getFlowRuntime() {
		return flowRuntime;
	}

	public String getFlowItemData() {
		return flowItemData;
	}
}
