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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum MobileLook {
	AUTO("Auto", "auto"),
	IOS("IOS", "apple.css"),
	ANDROID("Android", "android.css"),
	BB("BlackBerry", "bb6.css"),
	SENCHA("Sencha Touch", "senchatouch.css");

	final static private List<String> androidCss;
	final static private List<String> appleCss;
	final static private List<String> bbCss;
	final static private List<String> senchaCss;
	final static List<String> css;
	
	final static private String[] displayNames;
	static {
		MobileLook[] looks = MobileLook.values();
		displayNames = new String[looks.length];
		for (int i = 0 ; i < looks.length ; i++) {
			displayNames[i] = looks[i].displayName;
		}
		androidCss = Collections.unmodifiableList(Arrays.asList(ANDROID.fileName));
		appleCss = Collections.unmodifiableList(Arrays.asList(IOS.fileName));
		bbCss = Collections.unmodifiableList(Arrays.asList(BB.fileName));
		senchaCss = Collections.unmodifiableList(Arrays.asList(SENCHA.fileName));
		css = Collections.unmodifiableList(Arrays.asList(ANDROID.fileName, BB.fileName, IOS.fileName, SENCHA.fileName));
	}

	private String displayName;
	private String fileName;
	
	MobileLook() {
		displayName = name();
	}
	
	MobileLook(String displayName, String fileName) {
		this.displayName = displayName;
		this.setFileName(fileName);
	}
	
	static public String[] displayNames() {
		return displayNames;
	}
	
	public int index() {
		return Arrays.binarySearch(MobileLook.values(), this);
	}
	
	public String displayName() {
		return displayName;
	}
	
	public List<String> getCssFilesNames() {
		return css;
	}
	
	public static List<String> getUselessCssList(int mobileLook) {
		if (mobileLook == IOS.index()) {
			return Collections.unmodifiableList(Arrays.asList(ANDROID.fileName, BB.fileName, SENCHA.fileName));
		}
		else if (mobileLook == ANDROID.index()) {
			return Collections.unmodifiableList(Arrays.asList(BB.fileName, SENCHA.fileName, IOS.fileName));
		}
		else if (mobileLook == BB.index()) {
			return Collections.unmodifiableList(Arrays.asList(SENCHA.fileName, IOS.fileName, ANDROID.fileName));
		}
		else if (mobileLook == SENCHA.index()) {
			return Collections.unmodifiableList(Arrays.asList(BB.fileName, IOS.fileName, ANDROID.fileName));
		}
		else if (mobileLook == AUTO.index()) {
			return Collections.unmodifiableList(Arrays.asList(""));
		}
		return null;
	}
	
	public static List<String> getCssFiles(int mobileLook) {
		if (mobileLook == IOS.index()) {
			return appleCss;
		}
		else if (mobileLook == ANDROID.index()) {
			return androidCss;
		}
		else if (mobileLook == BB.index()) {
			return bbCss;
		}
		else if (mobileLook == SENCHA.index()) {
			return senchaCss;
		}
		else if (mobileLook == AUTO.index()) {
			return css;
		}
		return new ArrayList<String>();
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String fileName() {
		return fileName;
	}
}
