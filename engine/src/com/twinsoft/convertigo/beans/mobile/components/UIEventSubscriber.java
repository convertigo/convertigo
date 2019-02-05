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

import java.util.Iterator;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.EngineException;

public class UIEventSubscriber extends UIComponent implements IEventListener {

	private static final long serialVersionUID = -7552967959256066078L;

	public UIEventSubscriber() {
		super();
	}

	@Override
	public UIEventSubscriber clone() throws CloneNotSupportedException {
		UIEventSubscriber cloned = (UIEventSubscriber) super.clone();
		cloned.errorEvent = null;
		return cloned;
	}
	
	private transient UIActionErrorEvent errorEvent = null;
	
	protected UIActionErrorEvent getErrorEvent() {
		return this.errorEvent;
	}
	
	private String topic = "";
	
	/**
	 * @return the topic
	 */
	public String getTopic() {
		return topic;
	}

	@Override
	protected String getRequiredTplVersion() {
		return "7.5.2.0";// since _tpl_7_5_2
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
	
	/**
	 * @param topic the topic to set
	 */
	public void setTopic(String topic) {
		this.topic = topic;
	}

	@Override
	public String toString() {
		String label = getTopic();
		return "on(" + (label.isEmpty() ? "?":label) + ")";
	}
	
	public String getFunctionName() {
		return "ETS"+ this.priority;
	}
	
	@Override
	public String computeTemplate() {
		return "";
	}

	static public String computeConstructors(String nbi, List<UIEventSubscriber> subscriberList) {
		String computed = ""+nbi+"++;"+System.lineSeparator();
		computed += "\t\tif ("+nbi+" == 1) {"+System.lineSeparator();
		for (UIEventSubscriber subscriber: subscriberList) {
			if (subscriber.isEnabled() && !subscriber.getTopic().isEmpty()) {
				computed += "\t\t\tthis.events.subscribe('"+subscriber.getTopic()+"', "
							+ "(data) => {this."+subscriber.getFunctionName()+"(data)});"+ System.lineSeparator();
			}
		}
		computed += "\t\t}"+ System.lineSeparator();
		computed += "\t\t";
		return computed;
	}

	static public String computeNgDestroy(String nbi, List<UIEventSubscriber> subscriberList) {
		String computed = "ngOnDestroy() {"+ System.lineSeparator();
		computed += "\t\t"+nbi+"--;"+ System.lineSeparator();
		computed += "\t\tif ("+nbi+" <= 0) {"+ System.lineSeparator();
		for (UIEventSubscriber subscriber: subscriberList) {
			if (subscriber.isEnabled() && !subscriber.getTopic().isEmpty()) {
				computed += "\t\t\tthis.events.unsubscribe('"+subscriber.getTopic()+"');"+ System.lineSeparator();
			}
		}
		computed += "\t\t\t"+nbi+" = 0;"+ System.lineSeparator();
		computed += "\t\t}"+ System.lineSeparator();
		computed += "\t\tsuper.ngOnDestroy();"+ System.lineSeparator();
		computed += "\t}"+ System.lineSeparator();
		computed += "\t";
		return computed;
	}

	@Override
	public void computeScripts(JSONObject jsonScripts) {
		if (isEnabled() && !topic.isEmpty()) {
			try {
				String functions = jsonScripts.getString("functions") + System.lineSeparator() + computeListenerFunction();
				jsonScripts.put("functions", functions);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			super.computeScripts(jsonScripts);
		}
	}

	private String computeListenerFunction() {
		String computed = "";
		if (isEnabled()) {
			String functionName = getFunctionName();
			
			StringBuilder sb = new StringBuilder();
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof IAction) {
					if (component.isEnabled()) {
						sb.append("\t\tthis.").append(((IAction)component).getFunctionName())
							.append("({root: {scope:{}, in:{}, out:data}})")
								.append(";").append(System.lineSeparator());
					}
				}
			}
			
			StringBuilder cartridge = new StringBuilder();
			cartridge.append("\t/**").append(System.lineSeparator())
						.append("\t * Function "+ functionName).append(System.lineSeparator());
			for (String commentLine : getComment().split(System.lineSeparator())) {
				cartridge.append("\t *   ").append(commentLine).append(System.lineSeparator());
			}
			cartridge.append("\t * ").append(System.lineSeparator());
			cartridge.append("\t * @param data , the event data object").append(System.lineSeparator());
			cartridge.append("\t */").append(System.lineSeparator());
			
			computed += System.lineSeparator();
			computed += cartridge;
			computed += "\t"+ functionName + "(data) {" + System.lineSeparator();
			computed += "\t\tthis.c8o.log.debug(\"[MB] "+functionName+": '"+topic+"' received\");" + System.lineSeparator();
			computed += sb.toString();
			computed += "\t}";
		}
		return computed;
	}
	
}
