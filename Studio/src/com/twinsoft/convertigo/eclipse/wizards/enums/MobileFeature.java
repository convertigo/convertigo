/*
 * Copyright (c) 2001-2011 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.wizards.enums;

import java.util.Arrays;

public enum MobileFeature {
	LOGIN_FORM("Login form", "loginForm.js", "login,"),
	SEARCH_FORM("Search form", "searchForm.js", "form,"),
	LIST_DISPLAY("List display", "listDisplay.js", "list,"),
	DETAILS("Details", "details.js", "details"),
	GOOGLE_MAP("Google map", "googleMap.js", "map,"),
	TOP_TOOLBAR("Top toolbar", "topToolbar.js", "");
	
	
	final static private String[] displayNames;
	static {
		MobileFeature[] features = MobileFeature.values();
		displayNames = new String[features.length];
		for (int i = 0 ; i < features.length ; i++) {
			displayNames[i] = features[i].displayName;
		}
	}
	
	private String displayName;
	private String fileName;
	private String itemName;
	
	MobileFeature() {
		displayName = name();
	}
	
	MobileFeature(String displayName, String fileName, String itemName) {
		this.displayName = displayName;
		this.fileName = fileName;
		this.itemName = itemName;
	}
	
	static public String[] displayNames() {
		return displayNames;
	}
	
	public int index() {
		return Arrays.binarySearch(MobileFeature.values(), this);
	}
	
	public String displayName() {
		return displayName;
	}
	
	public String fileName() {
		return fileName;
	}
	
	public String itemName() {
		return itemName;
	}
}