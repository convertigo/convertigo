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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;

public class UIAppEvent extends UIComponent implements ITagsProperty {

	private static final long serialVersionUID = 2861783522824694904L;
	
	private transient UIActionErrorEvent errorEvent = null;
	private transient UIActionFinallyEvent finallyEvent = null;
	
	public enum AppEventType {
		ionicPromise,
		ionicObservable,
		c8oObservable
	}
	
	public enum AppEvent {
		//onAppReady("ready", AppEventType.ionicPromise, ?),
		onAppPause("pause", AppEventType.ionicObservable, "7.6.0.1"),
		onAppResume("resume", AppEventType.ionicObservable, "7.6.0.1"),
		onAppResize("resize", AppEventType.ionicObservable, "7.6.0.1"),
		onSessionLost("handleSessionLost()", AppEventType.c8oObservable, "7.6.0.2"),
		onNetworkReachable("handleNetworkEvents()", AppEventType.c8oObservable, "7.6.0.3"),
		onNetworkUnreachable("handleNetworkEvents()", AppEventType.c8oObservable, "7.6.0.3"),
		onNetworkOffline("handleNetworkEvents()", AppEventType.c8oObservable, "7.6.0.3"),
		onAutoLogin("handleAutoLoginResponse()", AppEventType.c8oObservable, "7.7.0.1")
		;
		
		String event;
		String tplVersion;
		AppEventType type;
		AppEvent(String event, AppEventType type, String tplVersion) {
			this.event = event;
			this.type = type;
			this.tplVersion = tplVersion;
		}
		
		static String[] getTagsForProperty(String tplVersion) {
			TreeSet<String> eventSet = new TreeSet<String>();
			if (tplVersion != null) {
				for (AppEvent appEvent: AppEvent.values()) {
					if (MobileBuilder.compareVersions(tplVersion, appEvent.tplVersion) >= 0) {
						eventSet.add(appEvent.name());
					}
				}
			}
			return eventSet.toArray(new String[eventSet.size()]);
		}
		
		String computeConstructor(String functionName) {
			if (type.equals(AppEventType.ionicObservable)) {
				return "\t\tplatform."+ event +".subscribe((data) => {this."+ functionName +"(data)});"+ System.lineSeparator();
			}
			if (type.equals(AppEventType.c8oObservable)) {
				if (this.equals(onSessionLost)) {
					return "\t\tthis.c8o."+ event +".subscribe((data) => {this."+ functionName +"(data)});"+ System.lineSeparator();
				}				
				if (this.equals(onNetworkReachable)) {
					return "\t\tthis.c8o."+ event +".subscribe((data) => {if (data == C8oNetworkStatus.Reachable) {this."+ functionName +"(data)}});"+ System.lineSeparator();
				}
				if (this.equals(onNetworkUnreachable)) {
					return "\t\tthis.c8o."+ event +".subscribe((data) => {if (data == C8oNetworkStatus.NotReachable) {this."+ functionName +"(data)}});"+ System.lineSeparator();
				}
				if (this.equals(onNetworkOffline)) {
					return "\t\tthis.c8o."+ event +".subscribe((data) => {if (data == C8oNetworkStatus.Offline) {this."+ functionName +"(data)}});"+ System.lineSeparator();
				}
				if (this.equals(onAutoLogin)) {
					return "\t\tthis.c8o."+ event +".subscribe((data) => {this."+ functionName +"(data)});"+ System.lineSeparator();
				}				
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
		cloned.finallyEvent = null;
		return cloned;
	}

	@Override
	protected String getRequiredTplVersion() {
		return appEvent.tplVersion;
	}
	
	public boolean isAvailable() {
		return compareToTplVersion(getRequiredTplVersion()) >= 0;
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
	
	public String getFunctionName() {
		return "ETS"+ this.priority;
	}
	
	protected Map<String, Set<String>> getInfoMap() {
		Set<UIComponent> done = new HashSet<>();
		Map<String, Set<String>> map = new HashMap<String, Set<String>>();
		for (UIComponent uiComponent : getUIComponentList()) {
			uiComponent.addInfos(done, map);
		}
		return map;
	}
	
	@Override
	public String computeTemplate() {
		return "";
	}

	@Override
	public void computeScripts(JSONObject jsonScripts) {
		if (isEnabled()) {
			IScriptComponent main = getMainScriptComponent();
			if (main == null) {
				return;
			}
			
			try {
				String functions = jsonScripts.getString("functions");
				String fname = getFunctionName();
				String fcode = computeListenerFunction();
				if (main.addFunction(fname, fcode)) {
					functions += System.lineSeparator() + fcode;
				}
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
			return AppEvent.getTagsForProperty(getTplVersion());
		}
		return new String[0];
	}
	
	@Override
	public String toString() {
		String label = appEvent.name();
		return label.isEmpty() ? "?":label;
	}
}
