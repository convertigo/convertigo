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
import java.util.Collections;
import java.util.List;

import com.twinsoft.convertigo.beans.core.MobileDevice;

public enum MobileSencha {
	INDEX_HTML("index.html", MobileDevice.RESOURCES_PATH),
	APP_JS("app.js", MobileDevice.RESOURCES_PATH + "/sources"),
	SENCHA_TOUCH_JS("senchatouch.js", MobileDevice.RESOURCES_PATH + "/js"),
	SENCHA_DEBUG_W_JS("senchatouchdebugwcomments.js", MobileDevice.RESOURCES_PATH + "/js");

	final static private String[] displayNames;
	static {
		MobileSencha[] files = MobileSencha.values();
		displayNames = new String[files.length];
		for (int i = 0 ; i < files.length ; i++) {
			displayNames[i] = files[i].displayName;
		}
	}
	
	public static String JS_DIRECTORY =  MobileDevice.RESOURCES_PATH + "/js";
	public static String CSS_DIRECTORY =  MobileDevice.RESOURCES_PATH + "/css";
	public static String SRC_DIRECTORY =  MobileDevice.RESOURCES_PATH + "/sources";
	public static String DATA_DIRECTORY =  MobileDevice.RESOURCES_PATH + "/data";
	
	private String displayName;
	private String directory;
	
	MobileSencha() {
		displayName = name();
	}
	
	MobileSencha(String displayName, String directory) {
		this.displayName = displayName;
		this.directory = directory;
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
	
	public String directory() {
		return directory;
	}
	
	public static List<String> getJSFiles() {
		List<String> jsFiles = Collections.unmodifiableList(Arrays.asList(MobileSencha.SENCHA_TOUCH_JS.displayName(), MobileSencha.SENCHA_DEBUG_W_JS.displayName()));
		return jsFiles;
	}
}