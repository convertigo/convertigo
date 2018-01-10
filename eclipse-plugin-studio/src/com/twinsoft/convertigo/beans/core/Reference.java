package com.twinsoft.convertigo.beans.core;

import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;

@DboCategoryInfo(
		getCategoryId = "Reference",
		getCategoryName = "Reference",
		getIconClassCSS = "convertigo-action-newReference"
	)
public abstract class Reference extends DatabaseObject {
	private static final long serialVersionUID = -1201316885732909011L;
	
	public Reference() {
		super();
		databaseType = "Reference";
	}
}
