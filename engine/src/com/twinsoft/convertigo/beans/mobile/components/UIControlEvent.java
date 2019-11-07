/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

package com.twinsoft.convertigo.beans.mobile.components;

import org.apache.commons.lang3.ArrayUtils;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.EnumUtils;

public class UIControlEvent extends UIControlAttr implements IControl {

	private static final long serialVersionUID = 4756891044178409988L;

	private transient UIActionErrorEvent errorEvent = null;
	private transient UIActionFinallyEvent finallyEvent = null;
	
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
		ionChange("(ionChange)"),
		ionCancel("(ionCancel)"),
		ionClear("(ionClear)"),
		ionPull("(ionPull)"),
		ionRefresh("(ionRefresh)"),
		ionStart("(ionStart)"),
		ionClose("(ionClose)"),
		ionOpen("(ionOpen)"),
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
		cloned.finallyEvent = null;
		return cloned;
	}

	/*
	 * The event to bind
	 */
	private String eventName = AttrEvent.onTap.name();

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	protected UIActionErrorEvent getErrorEvent() {
		return this.errorEvent;
	}
	
	protected UIActionFinallyEvent getFinallyEvent() {
		return this.finallyEvent;
	}
	
	@Override
	protected void addUIComponent(UIComponent uiComponent, Long after) throws EngineException {
		checkSubLoaded();
		
		if (uiComponent instanceof UIActionErrorEvent) {
    		if (this.errorEvent != null) {
    			throw new EngineException("The event \"" + getName() + "\" already contains an error event! Please delete it first.");
    		}
    		else {
    			this.errorEvent = (UIActionErrorEvent)uiComponent;
    			after = -1L;// to be first
    		}
		}
		if (uiComponent instanceof UIActionFinallyEvent) {
    		if (this.finallyEvent != null) {
    			throw new EngineException("The event \"" + getName() + "\" already contains a finally handler! Please delete it first.");
    		}
    		else {
    			this.finallyEvent = (UIActionFinallyEvent)uiComponent;
    			after = this.errorEvent != null ? this.errorEvent.priority : -1L;
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
        if (uiComponent != null && uiComponent.equals(this.finallyEvent)) {
    		this.finallyEvent = null;
    		markAsDirty();
        }
	}
	
	@Override
	protected void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
		if (databaseObject.equals(this.errorEvent) || databaseObject.equals(this.finallyEvent)) {
			return;
		} else if (this.errorEvent != null || this.finallyEvent != null) {
			int num = this.errorEvent != null && this.finallyEvent != null ? 2:1;
			int pos = getOrderedComponents().get(0).indexOf(databaseObject.priority);
			if (pos-num <= 0) {
				return;
			}
		}
		super.increaseOrder(databaseObject, before);
	}
	
	@Override
	protected void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
		if (databaseObject.equals(this.errorEvent) || databaseObject.equals(this.finallyEvent)) {
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
		
		ApplicationComponent app = getApplication();
		String attrName = AttrEvent.getEvent(eventName);
		if (AttrEvent.onTap.name().equals(eventName) && app != null && app.getUseClickForTap()) {
			attrName = "(click)";
		}
		return attrName;
	}

	@Override
	public String getAttrValue() {
		String attrValue = super.getAttrValue();
		String attrName = getAttrName();
		if ("(ionInfinite)".equals(attrName)) {
			attrValue = "$event.waitFor("+ attrValue + ")";
		}
		return attrValue;
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
