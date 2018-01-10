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

import java.util.Iterator;
import java.util.List;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.util.EnumUtils;

public class UIPageEvent extends UIComponent implements IEventGenerator, ITagsProperty {

	private static final long serialVersionUID = -5699915260997234123L;

	public enum ViewEvent {
		onDidLoad("ionViewDidLoad"),
		onWillEnter("ionViewWillEnter"),
		onDidEnter("ionViewDidEnter"),
		onWillLeave("ionViewWillLeave"),
		onDidLeave("ionViewDidLeave"),
		onWillUnload("ionViewWillUnload"),
		;
		
		String event;
		ViewEvent(String event) {
			this.event = event;
		}
		
		String computeEvent(List<UIPageEvent> eventList) {
			StringBuffer children = new StringBuffer();
			for (UIPageEvent pageEvent : eventList) {
				if (pageEvent.getViewEvent().equals(this)) {
					String 	computed = pageEvent.computeEvent();
					computed = computed.replace("$event", "'"+this.event+"'");
					children.append(computed);
				}
			}
			
			StringBuffer sb = new StringBuffer();
			if (children.length() > 0) {
				sb.append(event).append("() {").append(System.lineSeparator());
				sb.append("\t\tsuper.").append(event).append("();").append(System.lineSeparator());
				sb.append("\t\tthis.getInstance(Platform).ready().then(()=>{").append(System.lineSeparator());
				sb.append(children);
				sb.append("\t\t})").append(System.lineSeparator());
				sb.append("\t}").append(System.lineSeparator());
			}
			return sb.toString();
		}
	}
	
	public UIPageEvent() {
		super();
	}
	
	@Override
	public UIPageEvent clone() throws CloneNotSupportedException {
		UIPageEvent cloned = (UIPageEvent) super.clone();
		return cloned;
	}

	private ViewEvent viewEvent = ViewEvent.onDidEnter;

	public ViewEvent getViewEvent() {
		return viewEvent;
	}

	public void setViewEvent(ViewEvent viewEvent) {
		this.viewEvent = viewEvent;
	}
	
	@Override
	public String computeTemplate() {
		return "";
	}

	@Override
	public String computeEvent() {
		if (isEnabled()) {
			StringBuilder sb = new StringBuilder();
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				//if (component instanceof UIControlAction) {
				if (component instanceof IAction) {
					String action = component.computeTemplate();
					if (!action.isEmpty()) {
						sb.append("\t\tthis.").append(action).append(";").append(System.lineSeparator());
					}
				}
			}
			return sb.toString();
		}
		return "";
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("viewEvent")) {
			return EnumUtils.toNames(ViewEvent.class);
		}
		return new String[0];
	}
	
	@Override
	public String toString() {
		String label = viewEvent.name();
		return label.isEmpty() ? "?":label;
	}
}
