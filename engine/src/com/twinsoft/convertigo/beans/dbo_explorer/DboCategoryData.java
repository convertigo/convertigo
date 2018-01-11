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
