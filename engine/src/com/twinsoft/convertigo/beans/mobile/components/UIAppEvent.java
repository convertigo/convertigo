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
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.EnumUtils;

public class UIAppEvent extends UIComponent implements ITagsProperty {

	private static final long serialVersionUID = 2861783522824694904L;
	
	private transient UIActionErrorEvent errorEvent = null;
	
	public enum AppEventType {
		ionicPromise,
		ionicObservable,
		c8oObservable
	}
	
	public enum AppEvent {
		//onAppReady("ready", AppEventType.ionicPromise),
		onAppPause("pause", AppEventType.ionicObservable),
		onAppResume("resume", AppEventType.ionicObservable),
		onAppResize("resize", AppEventType.ionicObservable),
		onSessionLost("handleSessionLost()", AppEventType.c8oObservable),
		onNetworkChanged("handleNetworkEvents()", AppEventType.c8oObservable)
		;
		
		String event;
		AppEventType type;
		AppEvent(String event, AppEventType type) {
			this.event = event;
			this.type = type;
		}
		
		String computeConstructor(String functionName) {
			if (type.equals(AppEventType.ionicObservable)) {
				return "\t\tplatform."+ event +".subscribe((data) => {this."+ functionName +"(data)});"+ System.lineSeparator();
			}
			if (type.equals(AppEventType.c8oObservable)) {
				return "\t\tthis.c8o."+ event +".subscribe((data) => {this."+ functionName +"(data)});"+ System.lineSeparator();
			}
			if (type.equals(AppEventType.ionicPromise)) {
				//TODO
			}
			return "";
		}
		
		String computeDestructor() {
			if (type.equals(AppEventType.ionicObservable)) {
				return "\t\tthis.getInstance(Platform)."+ event +".unsubscribe();"+ System.lineSeparator();
			}
			if (type.equals(AppEventType.c8oObservable)) {
				return "\t\tthis.c8o."+ event +".unsubscribe();"+ System.lineSeparator();
			}
			return "";
		}
	}
	
	public UIAppEvent() {
		super();
	}
	
	@Override
	public UIAppEvent clone() throws CloneNotSupportedException {
		UIAppEvent cloned = (UIAppEvent) super.clone();
		cloned.errorEvent = null;
		return cloned;
	}

	@Override
	protected String getRequiredTplVersion() {
		return "7.6.0.1";
	}
	
	private AppEvent appEvent = AppEvent.onAppPause;

	public AppEvent getAppEvent() {
		return appEvent;
	}

	public void setAppEvent(AppEvent appEvent) {
		this.appEvent = appEvent;
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
	
	public String getFunctionName() {
		return "ETS"+ this.priority;
	}
	
	@Override
	public String computeTemplate() {
		return "";
	}

	@Override
	public void computeScripts(JSONObject jsonScripts) {
		if (isEnabled()) {
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
							.append("({root: {scope:{}, in:{}, out:event}})")
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
			cartridge.append("\t * @param data , the event data").append(System.lineSeparator());
			cartridge.append("\t */").append(System.lineSeparator());
			
			String eventName = appEvent.name();
			
			computed += System.lineSeparator();
			computed += cartridge;
			computed += "\t"+ functionName + "(data) {" + System.lineSeparator();
			computed += "\t\tthis.c8o.log.debug(\"[MB] "+functionName+": '"+ eventName +"' received\");" + System.lineSeparator();
			computed += sb.toString();
			computed += "\t}";
		}
		return computed;
	}
	
	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("appEvent")) {
			return EnumUtils.toNames(AppEvent.class);
		}
		return new String[0];
	}
	
	@Override
	public String toString() {
		String label = appEvent.name();
		return label.isEmpty() ? "?":label;
	}
}
