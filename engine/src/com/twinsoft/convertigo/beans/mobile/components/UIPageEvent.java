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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.EnumUtils;

public class UIPageEvent extends UIComponent implements IEventGenerator, ITagsProperty {

	private static final long serialVersionUID = -5699915260997234123L;

	private transient UIActionErrorEvent errorEvent = null;
	private transient UIActionFinallyEvent finallyEvent = null;
	
	public enum ViewEvent {
		onDidLoad("ionViewDidLoad"),
		onWillEnter("ionViewWillEnter"),
		onDidEnter("ionViewDidEnter"),
		onWillLeave("ionViewWillLeave"),
		onDidLeave("ionViewDidLeave"),
		onWillUnload("ionViewWillUnload"),
		onCanEnter("ionViewCanEnter"),
		onCanLeave("ionViewCanLeave");
		
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
				sb.append(System.lineSeparator());
				
				//Supporting ionViewCan Events
				if (this.equals(ViewEvent.onCanEnter) || this.equals(ViewEvent.onCanLeave)) {
					sb.append("\t"+event).append("() {").append(System.lineSeparator());
					sb.append("\t\tsuper.").append(event).append("();").append(System.lineSeparator());
					sb.append("\t\treturn new Promise((resolve, reject)=>{").append(System.lineSeparator());
					sb.append("\t\t\tthis.getInstance(Platform).ready().then(()=>{").append(System.lineSeparator());
					sb.append("\t\t\t\tPromise.all([").append(System.lineSeparator());
					sb.append(children);
					sb.append("\t\t\t\t])").append(System.lineSeparator());
					sb.append("\t\t\t\t.then((resp)=>{").append(System.lineSeparator());
					sb.append("\t\t\t\t\tlet ret = resp.find((item) => {return item === false;});").append(System.lineSeparator());
					sb.append("\t\t\t\t\tresolve(ret === false ? false : true);").append(System.lineSeparator());
					sb.append("\t\t\t\t});").append(System.lineSeparator());
					sb.append("\t\t\t});").append(System.lineSeparator());
					sb.append("\t\t});").append(System.lineSeparator());
					sb.append("\t}").append(System.lineSeparator());
				}
				else {
					sb.append("\t"+event).append("() {").append(System.lineSeparator());
					sb.append("\t\tsuper.").append(event).append("();").append(System.lineSeparator());
					sb.append("\t\tthis.getInstance(Platform).ready().then(()=>{").append(System.lineSeparator());				
					sb.append(children);	
					sb.append("\t\t});").append(System.lineSeparator());
					sb.append("\t}").append(System.lineSeparator());
				}
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
		cloned.errorEvent = null;
		cloned.finallyEvent = null;
		return cloned;
	}

	private ViewEvent viewEvent = ViewEvent.onDidEnter;

	public ViewEvent getViewEvent() {
		return viewEvent;
	}

	public void setViewEvent(ViewEvent viewEvent) {
		this.viewEvent = viewEvent;
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
	public String computeTemplate() {
		return "";
	}

	@Override
	public String computeEvent() {
		if (isEnabled()) {
			List<String> list = new ArrayList<String>();
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof IAction) {
					String action = component.computeTemplate();
					if (!action.isEmpty()) {
						list.add("this."+action);
					}
				}
			}
			
			StringBuilder sb = new StringBuilder();
			if (!list.isEmpty()) {
				String last = list.get(list.size()-1);
				for (String s: list) {
					if (viewEvent.equals(ViewEvent.onCanEnter) || viewEvent.equals(ViewEvent.onCanLeave)) {
						sb.append("\t\t\t\t\t").append(s).append(s.equals(last) ? "":",").append(System.lineSeparator());
					} else {
						sb.append("\t\t\t").append(s).append(";").append(System.lineSeparator());
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
