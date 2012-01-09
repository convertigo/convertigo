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

public enum MobileHighEndDevice {
	ANDROID("Android", MobileLook.ANDROID),
	BB6("BlackBerry6", MobileLook.BB),
	IPAD("iPad", MobileLook.IOS),
	IPHONE3("iPhone3", MobileLook.IOS),
	IPHONE4("iPhone4", MobileLook.IOS);

	
	final static private String[] displayNames;
	static {
		MobileHighEndDevice[] devices = MobileHighEndDevice.values();
		displayNames = new String[devices.length];
		for (int i = 0 ; i < devices.length ; i++) {
			displayNames[i] = devices[i].displayName;
		}
	}
	
	private String displayName;
	private MobileLook look;
	
	MobileHighEndDevice() {
		displayName = name();
	}
	
	MobileHighEndDevice(String displayName, MobileLook look) {
		this.displayName = displayName;
		this.look = look;
	}
	
	static public String[] displayNames() {
		return displayNames;
	}
	
	public int index() {
		return Arrays.binarySearch(MobileHighEndDevice.values(), this);
	}
	
	public String displayName() {
		return displayName;
	}
	
	public MobileLook look() {
		return look;
	}
}