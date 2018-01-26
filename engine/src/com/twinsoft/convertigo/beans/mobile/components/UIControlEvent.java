/*
 * Copyright (c) 2001-2017 Convertigo SA.
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

import org.apache.commons.lang3.ArrayUtils;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.EnumUtils;

public class UIControlEvent extends UIControlAttr implements IControl {

	private static final long serialVersionUID = 4756891044178409988L;

	private transient UIActionErrorEvent errorEvent = null;
	
	public enum AttrEvent {
		onClick("(click)"),
		onInput("(input)"),
		onTap("(tap)"),
		onPress("(press)"),
		onChange("(change)"),
		onPan("(pan)"),
		onSubmit("(ngSubmit)"),
		onSwipe("(swipe)"),
		onRotate("(rotate)"),
		onPinch("(pinch)"),
		ionSlideAutoplay("(ionSlideAutoplay)"),
		ionSlideAutoplayStart("(ionSlideAutoplayStart)"),
		ionSlideAutoplayStop("(ionSlideAutoplayStop)"),
		ionSlideDidChange("(ionSlideDidChange)"),
		ionSlideDoubleTap("(ionSlideDoubleTap)"),
		ionSlideDrag("(ionSlideDrag)"),
		ionSlideNextEnd("(ionSlideNextEnd)"),
		ionSlideNextStart("(ionSlideNextStart)"),
		ionSlidePrevEnd("(ionSlidePrevEnd)"),
		ionSlidePrevStart("(ionSlidePrevStart)"),
		ionSlideReachEnd("(ionSlideReachEnd)"),
		ionSlideReachStart("(ionSlideReachStart)"),
		ionSlideTap("(ionSlideTap)"),
		ionSlideWillChange("(ionSlideWillChange)"),
		ionInput("(ionInput)"),
		ionCancel("(ionCancel)"),
		ionClear("(ionClear)"),
		ionPull("(ionPull)"),
		ionRefresh("(ionRefresh)"),
		ionStart("(ionStart)"),
		;
		
		String event;
		AttrEvent(String event) {
			this.event = event;
		}
		
		String event() {
			return event;
		}
		
		public static String getEvent(String eventName) {
			AttrEvent bindEvent = null;
			try {
				bindEvent = AttrEvent.valueOf(eventName);
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
		cloned.errorEvent = null;
		return cloned;
	}

	/*
	 * The event to bind
	 */
	private String eventName = AttrEvent.onClick.name();

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	protected UIActionErrorEvent getErrorEvent() {
		return this.errorEvent;
	}
	
	@Override
	protected void addUIComponent(UIComponent uiComponent, Long after) throws EngineException {
		checkSubLoaded();
		
		if (uiComponent instanceof UIActionErrorEvent) {
    		if (this.errorEvent != null) {
    			throw new EngineException("The action \"" + getName() + "\" already contains an error event! Please delete it first.");
    		}
    		else {
    			this.errorEvent = (UIActionErrorEvent)uiComponent;
    			after = -1L;// to be first
    		}
		}
		
		super.addUIComponent(uiComponent, after);
	}
	
	@Override
	protected void removeUIComponent(UIComponent uiComponent) throws EngineException {
		super.removeUIComponent(uiComponent);
		
        if (uiComponent != null && uiComponent.equals(this.errorEvent)) {
    		this.errorEvent = null;
    		markAsDirty();
        }
	}
	
	@Override
	protected void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
		if (databaseObject.equals(this.errorEvent)) {
			return;
		} else if (this.errorEvent != null) {
			int pos = getOrderedComponents().get(0).indexOf(databaseObject.priority);
			if (pos-1 <= 0) {
				return;
			}
		}
		super.increaseOrder(databaseObject, before);
	}
	
	@Override
	protected void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
		if (databaseObject.equals(this.errorEvent)) {
			return;
		}
		super.decreaseOrder(databaseObject, after);
	}
	
	@Override
	public String getAttrName() {
		if (parent != null && parent instanceof UIDynamicElement) {
			String eventAttr = ((UIDynamicElement)parent).getEventAttr(eventName);
			if (!eventAttr.isEmpty()) {
				return eventAttr;
			}
		}
		return AttrEvent.getEvent(eventName);
	}

	@Override
	public String getAttrValue() {
		return super.getAttrValue();
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("eventName")) {
			String[] attrEvents = EnumUtils.toNames(AttrEvent.class);
			if (parent != null && parent instanceof UIDynamicElement) {
				String[] eventNames = ((UIDynamicElement)parent).getEventNames();
	    		if (eventNames.length > 0) {
	    			eventNames = ArrayUtils.add(eventNames, "");
	    		}
				return ArrayUtils.addAll(eventNames, attrEvents);
			}
			return attrEvents;
		}
		return new String[0];
	}

	@Override
	public String toString() {
		String label = getEventName();
		return label.isEmpty() ? "?":label;
	}

}
