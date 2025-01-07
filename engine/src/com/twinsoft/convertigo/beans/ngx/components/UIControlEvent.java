/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.beans.ngx.components;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.UIControlDirective.AttrDirective;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.EnumUtils;

public class UIControlEvent extends UIControlAttr implements IControl, IEventGenerator {

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
		
		public static String asJson(UIControlEvent uic, String eventName) {
			AttrEvent bindEvent = null;
			try {
				bindEvent = AttrEvent.valueOf(eventName);
			} catch (Exception e) {};
			
			String json = null;
			try {
				String tag = getTag(uic.getParent());
				if (tag != null) {
					String ename = eventName.replace("(", "").replace(")", "");
					json = AttrEvent.getEventJsonString(uic, tag + "." + ename + ".json");
					if (json == null) {
						ename = bindEvent != null ? bindEvent.event():eventName;
						ename = ename.replace("(", "").replace(")", "");
						json = AttrEvent.getEventJsonString(uic, tag + "." + ename + ".json");
					}
					
					if (json != null && tag.equals("ion-calendar") && eventName.equals("onChange")) {
						try {
							String pickMode = ((UIDynamicElement)uic.getParent())
									.getIonBean().getProperty("pickMode").getValue().toString();
							if (pickMode.equals("single")) {
								json = "{json: ''}";
							} else if (pickMode.equals("multi")) {
								json = "{json: ['']}";
							} else if (pickMode.equals("range")) {
								json = "{json: {from: '', to: ''}}";
							}
						} catch (Exception e) {}
					}
				}
				if (json == null) {
					String ename = bindEvent != null ? bindEvent.event():eventName;
					ename = ename.replace("(", "").replace(")", "");
					json = AttrEvent.getEventJsonString(uic, ename + ".json");
				}
			} catch (Exception e) {};
			
			return json;
		}
		
		private static String getTag(DatabaseObject dbo) throws IntrospectionException {
			if (dbo != null) {
				if (dbo instanceof UIDynamicElement) {
					IonBean ionBean = ((UIDynamicElement)dbo).getIonBean();
					if (ionBean != null) {
						return ionBean.getTag();
					}
				}
			}
			return null;
		}
		
		private static String getEventJsonString(UIControlEvent uic, String fileName) throws IOException {
			InputStream inputstream = uic.getClass().getResourceAsStream("events/"+ fileName);
			if (inputstream != null) {
				return IOUtils.toString(inputstream, "UTF-8");
			}
			return null;
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
	private String eventName = AttrEvent.onClick.name();

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	protected UIActionErrorEvent getErrorEvent() {
		checkSubLoaded();
		return this.errorEvent;
	}
	
	protected UIActionFinallyEvent getFinallyEvent() {
		checkSubLoaded();
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
        }
        if (uiComponent != null && uiComponent.equals(this.finallyEvent)) {
    		this.finallyEvent = null;
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
		if (parent != null && parent instanceof UIUseShared) {
			String eventAttr = ((UIUseShared)parent).getEventAttr(eventName);
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

	protected String computeEventFunction() {
		String computed = "";
		if (isEnabled()) {
			
			StringBuilder sbCatch = new StringBuilder();
			if (handleError()) {
				sbCatch.append(this.errorEvent.computeEvent());
			}
			StringBuilder sbFinally = new StringBuilder();
			if (handleFinally()) {
				sbFinally.append(this.finallyEvent.computeEvent());
			}
			
			StringBuilder parameters = new StringBuilder();
			parameters.append("stack");
			
			StringBuilder cartridge = new StringBuilder();
			cartridge.append("\t/**").append(System.lineSeparator())
						.append("\t * Function "+ getEventFunctionName()).append(System.lineSeparator());
			for (String commentLine : getComment().split(System.lineSeparator())) {
				cartridge.append("\t *   ").append(commentLine).append(System.lineSeparator());
			}
			cartridge.append("\t * ").append(System.lineSeparator());
			cartridge.append("\t * @param stack , the object which holds actions stack").append(System.lineSeparator());
			cartridge.append("\t */").append(System.lineSeparator());
			
			String cafPageType = "C8oPageBase";
			String functionName = getEventFunctionName();
			
			computed += System.lineSeparator();
			computed += cartridge;
			computed += "\t"+ functionName + "("+ parameters +"): Promise<any> {" + System.lineSeparator();
			computed += "\t\tlet c8oPage : "+ cafPageType +" = this;" + System.lineSeparator();
			computed += "\t\tlet parent;" + System.lineSeparator();
			computed += "\t\tlet scope;" + System.lineSeparator();
			computed += "\t\tlet out;" + System.lineSeparator();
			computed += "\t\tlet event;" + System.lineSeparator();
			computed += "\t\t" + System.lineSeparator();
			computed += computeInnerGet("c8oPage",functionName);
			computed += "\t\t" + System.lineSeparator();
			computed += "\t\tparent = stack[\"root\"];" + System.lineSeparator();
			computed += "\t\tevent = stack[\"root\"].out;" + System.lineSeparator();
			computed += "\t\tscope = stack[\"root\"].scope;" + System.lineSeparator();
			computed += "\t\tout = event;" + System.lineSeparator();
			computed += "\t\t" + System.lineSeparator();
			
			if (isSubmitEvent()) {
				computed += "\t\tif (stack[\"root\"].out?.target?.tagName?.toLowerCase() == \"form\") {" + System.lineSeparator();
				computed += "\t\t\tlet formData = new FormData(stack[\"root\"].out.target) as any;" + System.lineSeparator();
				computed += "\t\t\tfor (let key of formData.keys()) {" + System.lineSeparator();
				computed += "\t\t\t\tif (key in stack[\"root\"].in) {" + System.lineSeparator();
				computed += "\t\t\t\t\tstack[\"root\"].in[key] = formData.get(key); " + System.lineSeparator();
				computed += "\t\t\t\t}" + System.lineSeparator();
				computed += "\t\t\t}" + System.lineSeparator();
				computed += "\t\t}" + System.lineSeparator();
				computed += "\t\t" + System.lineSeparator();
			}
			
			computed += "\t\tthis.c8o.log.debug(\"[MB] "+functionName+": started\");" + System.lineSeparator();
			computed += "\t\treturn new Promise((resolveP, rejectP)=>{" + System.lineSeparator();
			computed += ""+ computeEvent();
			if (sbCatch.length() > 0) {
				computed += "\t\t.catch((error:any) => {"+ System.lineSeparator();
				computed += "\t\tparent = self;"+ System.lineSeparator();
				computed += "\t\tparent.out = error;"+ System.lineSeparator();
				computed += "\t\tout = parent.out;"+ System.lineSeparator();
				computed += "\t\t"+ sbCatch.toString() + System.lineSeparator();
				computed += "\t\t})"+ System.lineSeparator();
			}			
			computed += "\t\t.catch((error:any) => {this.c8o.log.debug(\"[MB] "+functionName+": An error occured : \",error.message); resolveP(false);})" + System.lineSeparator();
			if (sbFinally.length() > 0) {
				computed += "\t\t.then((res:any) => {"+ System.lineSeparator();
				computed += "\t\tparent = self;"+ System.lineSeparator();
				computed += "\t\tparent.out = res;"+ System.lineSeparator();
				computed += "\t\tout = parent.out;"+ System.lineSeparator();
				computed += "\t\t"+ sbFinally.toString() + System.lineSeparator();
				computed += "\t\t})"+ System.lineSeparator();
				computed += "\t\t.catch((error:any) => {this.c8o.log.debug(\"[MB] "+functionName+": An error occured : \",error.message); resolveP(false);})" + System.lineSeparator();
			}			
			computed += "\t\t.then((res:any) => {this.c8o.log.debug(\"[MB] "+functionName+": ended\"); resolveP(res)});" + System.lineSeparator();
			// zoneless support
			if (compareToTplVersion("8.3.2.0") >= 0) {
				computed += "\t\t}).finally(() => {this.ref.markForCheck();});"+System.lineSeparator();
			}
			else {
				computed += "\t\t});"+System.lineSeparator();
			}
			computed += "\t}";
		}
		return computed;
	}
	
	protected int numberOfActions() {
		int num = 0;
		Iterator<UIComponent> it = getUIComponentList().iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (component instanceof UIDynamicAction || component instanceof UICustomAction) {
				if (component.isEnabled()) {
					num++;
				}
			}
		}
		return num;
	}
	
	@Override
	public String computeEvent() {
		if (isEnabled()) {
			int num = numberOfActions();
			StringBuilder sb = new StringBuilder();
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component.isEnabled()) {
					if (component instanceof IAction) {
						String s = "";
						if (component instanceof UIDynamicAction) {
							UIDynamicAction uda = (UIDynamicAction)component;
							s = uda.computeActionContent();
						}
						if (component instanceof UICustomAction) {
							UICustomAction uca = (UICustomAction)component;
							s = uca.computeActionContent();
						}
						
						if (!s.isEmpty()) {
							sb.append(sb.length()>0 && num > 1 ? "\t\t,"+ System.lineSeparator() :"")
							.append(s);
						}
					}
				}
			}
			
			String tsCode = "";
			if (sb.length() > 0) {
				if (num > 1) {
					tsCode += "\t\treturn Promise.all(["+ System.lineSeparator();
					tsCode += sb.toString();
					tsCode += "\t\t])"+ System.lineSeparator();
				} else {
					tsCode += "\t\treturn "+ sb.toString().replaceFirst("\t\t", "");
				}
			} else {
				tsCode += "\t\tPromise.resolve(true)"+ System.lineSeparator();
			}
			
			//tsCode = tsCode.replaceAll("this", "page");
			//tsCode = tsCode.replaceAll("page\\.actionBeans\\.", "this.");
			return tsCode;
		}
		return "";
	}

	protected boolean handleError() {
		boolean handleError = false;
		UIActionErrorEvent errorEvent = getErrorEvent();
		if (errorEvent != null && errorEvent.isEnabled()) {
			if (errorEvent.numberOfActions() > 0) {
				handleError = true;
			}
		}
		return handleError;
	}
	
	protected boolean handleFinally() {
		boolean handleFinally = false;
		UIActionFinallyEvent finallyEvent = getFinallyEvent();
		if (finallyEvent != null && finallyEvent.isEnabled()) {
			if (finallyEvent.numberOfActions() > 0) {
				handleFinally = true;
			}
		}
		return handleFinally;
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
				String fname = getEventFunctionName();
				String fcode = computeEventFunction();
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
	
	@Override
	public String computeJsonModel() {
		try {
			String json = AttrEvent.asJson(this, eventName);
			if (json != null) {
				JSONObject jsonModel = new JSONObject();
				jsonModel.put("out", new JSONObject(json).get("json"));
				return jsonModel.toString();
			} else {
				JSONObject jsonEvent = new JSONObject();
				jsonEvent.put("detail", new JSONObject().put("value", ""));
				jsonEvent.put("type", "");
				
				JSONObject jsonModel = new JSONObject();
				jsonModel.put("out", jsonEvent);
				return jsonModel.toString();
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "{}";
	}
	
	private String getEventFunctionName() {
		return "ETS" + priority;
	}

	protected String getScope() {
		
		UIControlEvent original = (UIControlEvent) getOriginal();
		UISharedComponent sharedComponent = original.getSharedComponent();
		boolean isInSharedComponent = sharedComponent != null;
		
		String scope = "";
		
		DatabaseObject parent = getParent();
		while (parent != null && !(parent instanceof UIAppEvent) && !(parent instanceof UIPageEvent) && !(parent instanceof UISharedComponentEvent) && !(parent instanceof UIEventSubscriber)) {
			if (parent instanceof UISharedComponent) {
				UISharedComponent uisc = (UISharedComponent)parent;
				if (uisc.isRegular()) {
					//scope += !scope.isEmpty() ? ", ":"";
					//scope += "comp"+uisc.priority + ": "+ "comp"+uisc.priority;
					break;
				}
			}
			if (parent instanceof UIUseShared) {
				UISharedComponent uisc = ((UIUseShared) parent).getTargetSharedComponent();
				if (uisc != null) {
					if (uisc.isRegular() && !uisc.equals(sharedComponent)) {
						//scope += !scope.isEmpty() ? ", ":"";
						//scope += "comp"+uisc.priority + ": "+ "comp"+uisc.priority;
					}
				}
			}
			
			if (parent instanceof UIControlDirective) {
				UIControlDirective uicd = (UIControlDirective)parent;
				if (AttrDirective.isForDirective(uicd.getDirectiveName())) {
					scope += !scope.isEmpty() ? ", ":"";
					scope += "item"+uicd.priority + ": "+ "item"+uicd.priority;
					
					String item = uicd.getDirectiveItemName();
					if (!item.isEmpty()) {
						scope += !scope.isEmpty() ? ", ":"";
						scope += item + ": "+ item;
					}
					String index = uicd.getDirectiveIndexName();
					if (!index.isEmpty()) {
						scope += !scope.isEmpty() ? ", ":"";
						scope += index + ":" + index;
					}
				}
			}
			if (parent instanceof UIElement) {
				String identifier = ((UIElement)parent).getIdentifier();
				if (!identifier.isEmpty()) {
					scope += !scope.isEmpty() ? ", ":"";
					scope += identifier+ ": "+ identifier;
				}			
			}
			
			parent = parent.getParent();
		}
		
		if (!scope.isEmpty()) {
			if (isInSharedComponent) {
				//scope = "merge(merge({}, params"+ sharedComponent.priority +".scope), {"+ scope +"})";
				scope = "merge({}, {"+ scope +"})";
			} else {
				scope = "merge({}, {"+ scope +"})";
			}
		} else {
			scope = "{}";
		}
		return scope;
	}
	
	private boolean isSubmitEvent() {
		return getAttrName().equals(AttrEvent.onSubmit.event());
	}
	
	@Override
	public String getAttrValue() {
		String formIdentifier = null;
		if (isSubmitEvent()) {
			UIForm uiForm = getUIForm();
			if (uiForm != null) {
				if (!uiForm.getIdentifier().isBlank()) {
					formIdentifier = uiForm.getIdentifier();
				}
			}
		}
		
		String scope = getScope();
		String in = formIdentifier == null ? "{}": "merge({},"+formIdentifier +".value)";
		String attrValue = getEventFunctionName() + "({root: {scope:"+ scope +", in:"+ in +", out:$event}})";;
		return attrValue;
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("eventName")) {
			String[] attrEvents = EnumUtils.toNames(AttrEvent.class);
			if (parent != null) {
				if (parent instanceof UIDynamicElement) {
					String[] eventNames = ((UIDynamicElement)parent).getEventNames();
		    		if (eventNames.length > 0) {
		    			eventNames = ArrayUtils.add(eventNames, "");
		    		}
					return ArrayUtils.addAll(eventNames, attrEvents);
				}
				if (parent instanceof UIUseShared) {
					List<String> list = ((UIUseShared)parent).getEventNames();
					return list.toArray(new String[list.size()]);
				}
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
