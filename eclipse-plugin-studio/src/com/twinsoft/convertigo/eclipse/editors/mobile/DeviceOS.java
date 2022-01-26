/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.editors.mobile;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public enum DeviceOS {
	android("Android"),
	ios("iOS", "iPhone"),
	windows("Windows Phone");
	
	private String displayName;
	private String agent;
	private Image image = null;
	
	DeviceOS(String displayName) {
		this(displayName, displayName);
	}
	
	DeviceOS(String displayName, String agent) {
		this.displayName = displayName;
		this.agent = agent;
	}
	
	public Image image() {
		return image;
	}
	
	public String displayName() {
		return displayName;
	}
	
	public String agent() {
		return agent;
	}
	
	public static void init(Display display) {
		for (DeviceOS device: values()) {
			if (device.image == null) {
				device.image = new Image(display, device.getClass().getResourceAsStream("/com/twinsoft/convertigo/beans/mobileplatforms/images/" + device.name() + "_color_16x16.png"));
			}
		}
	}
}
