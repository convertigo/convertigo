/*
 * Copyright (c) 2001-2016 Convertigo SA.
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

package com.twinsoft.convertigo.beans.mobile.components;

import com.twinsoft.convertigo.engine.util.EnumUtils;

public class UIControlEvent extends UIControlAttr {
	
	private static final long serialVersionUID = 4756891044178409988L;

	public enum BindEvent {
		onClick("click"),
		onInput("input"),
		;
		
		String event;
		BindEvent(String event) {
			this.event = event;
		}
		
		String event() {
			return event;
		}
		
		public static String getEvent(String eventName) {
			BindEvent bindEvent = null;
			try {
				bindEvent = BindEvent.valueOf(eventName);
			} catch (Exception e) {};
			return bindEvent != null ? bindEvent.event():eventName;
		}
	}
	
	public UIControlEvent() {
		super();
	}

	@Override
	public UIControlEvent clone() throws CloneNotSupportedException {
		UIControlEvent cloned = (UIControlEvent) super.clone();
		return cloned;
	}

	@Override
	protected BindType getBindType() {
		return BindType.event;
	}

	/*
	 * The event to bind
	 */
	private String eventName = BindEvent.onClick.name();

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	@Override
	public String getAttrName() {
		return BindEvent.getEvent(eventName);
	}

	@Override
	public String getAttrValue() {
		return super.getAttrValue();
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("eventName")) {
			return EnumUtils.toNames(BindEvent.class);
		}
		return new String[0];
	}

	@Override
	public String toString() {
		String label = getEventName();
		return label.isEmpty() ? "?":label;
	}

}
