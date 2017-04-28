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
