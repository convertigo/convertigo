/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

package com.twinsoft.convertigo.beans.dbo_explorer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DboCategoryData {

	private String id;
	private String name;
	private String iconClassCSS;
	private Map<String, DboBeansData> dboBeansMap;

	public DboCategoryData(String id, String name, String iconClassCSS) {
		this.id = id;
		this.name = name;
		this.iconClassCSS = iconClassCSS;
		dboBeansMap = new LinkedHashMap<>();
	}

	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}

	public String getIconClassCSS() {
		return iconClassCSS;
	}

	public Map<String, DboBeansData> getAllDboBeans() {
		return dboBeansMap;
	}

	public DboBeansData getDboBeans(String dboBeansName) {
		return dboBeansMap.get(dboBeansName);
	}

	public void addDboBeans(String dboBeansName, DboBeansData dboBeans) {
		dboBeansMap.put(dboBeansName, dboBeans);
	}

	public List<DboBeanData> getAllDboBean() {
		List<DboBeanData> dboBeanList = new ArrayList<>();
		for (DboBeansData dboBeans: getAllDboBeans().values()) {
			dboBeanList.addAll(dboBeans.getAllDboBean(false));
		}

		return dboBeanList;
	}

	public List<DboBeanData> getAllDboBean(boolean sort) {
		List<DboBeanData> dboBeanList = new ArrayList<>();
		for (DboBeansData dboBeans: getAllDboBeans().values()) {
			dboBeanList.addAll(dboBeans.getAllDboBean(sort));
		}

		return dboBeanList;
	}

	public Element toXml(Document document) {
		// Create category
		Element eCategory = document.createElement("category");
		eCategory.setAttribute("id", id);
		eCategory.setAttribute("name", name);
		eCategory.setAttribute("icon", iconClassCSS);

		// Create beans
		for (DboBeansData dboBeansData: getAllDboBeans().values()) {
			Element eBeans = document.createElement("beans");
			eBeans.setAttribute("name", dboBeansData.getName());
			eCategory.appendChild(eBeans);
			
			for (DboBeanData dboBeanData: dboBeansData.getAllDboBean(true)) {
				Element eBean = document.createElement("bean");
				eBean.setAttribute("displayName", dboBeanData.getDisplayName());

				String classname =  dboBeanData.getClassname();
				eBean.setAttribute("classname", classname);
				eBean.setAttribute("icon", classname.replaceAll("\\.", "-") + "-32");

				eBean.setAttribute("description", dboBeanData.getHtmlDescription());
				eBean.setAttribute("selectedByDefault", Boolean.toString(dboBeanData.isSelectedByDefault()));

				eBeans.appendChild(eBean);
			}
		}

		return eCategory;
	}

}
