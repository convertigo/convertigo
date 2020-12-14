/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

import java.util.Iterator;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.util.EnumUtils;

public class UIPageEvent extends UIComponent implements IEventGenerator, ITagsProperty {

	private static final long serialVersionUID = -5699915260997234123L;

	protected transient UIActionErrorEvent errorEvent = null;
	protected transient UIActionFinallyEvent finallyEvent = null;
	
	public enum ViewEvent {
		onDidLoad("ionViewDidLoad"),		// ngOnInit
		onWillEnter("ionViewWillEnter"),
		onDidEnter("ionViewDidEnter"),
		onWillLeave("ionViewWillLeave"),
		onDidLeave("ionViewDidLeave"),
		onWillUnload("ionViewWillUnload")	// ngOnDestroy
		;
		
		String event;
		ViewEvent(String event) {
			this.event = event;
		}
		
		String computeEvent(List<UIPageEvent> eventList) {
			StringBuffer children = new StringBuffer();
			for (UIPageEvent pageEvent : eventList) {
				if (pageEvent.getViewEvent().equals(this)) {
//					String computed = pageEvent.computeEvent();
//					if (!computed.isEmpty()) {
//						children.append(computed.replace("$event", "'"+this.event+"'"));
//					}
					String functionCall = "this." + pageEvent.getEventFunctionName() + "({root: {scope:{}, in:{}, out:'"+ this.event +"'}})";
					children.append("\t\t\t\t\t" + functionCall).append(System.lineSeparator());
				}
			}
			
			boolean hasSuper = !ViewEvent.onDidLoad.equals(this) && !ViewEvent.onWillUnload.equals(this);
			StringBuffer sb = new StringBuffer();
			if (children.length() > 0) {
				sb.append(System.lineSeparator());
				sb.append("\t"+event).append("() {").append(System.lineSeparator());
				if (hasSuper) {
					sb.append("\t\tsuper.").append(event).append("();").append(System.lineSeparator());
				}
				sb.append("\t\tthis.getInstance(Platform).ready().then(()=>{").append(System.lineSeparator());				
				sb.append(children);	
				sb.append("\t\t});").append(System.lineSeparator());
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
	
	private String getEventFunctionName() {
		return "ETS" + priority;
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
			
			String cafPageType = compareToTplVersion("7.5.2.0") >= 0 ? "C8oPageBase":"C8oPage";
			String functionName = getEventFunctionName();
			
			computed += System.lineSeparator();
			computed += cartridge;
			computed += "\t"+ functionName + "("+ parameters +"): Promise<any> {" + System.lineSeparator();
			computed += "\t\tlet c8oPage : "+ cafPageType +" = this;" + System.lineSeparator();
			computed += "\t\tlet parent;" + System.lineSeparator();
			computed += "\t\tlet scope;" + System.lineSeparator();
			//computed += "\t\tlet self;" + System.lineSeparator();
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
			computed += "\t\t});"+System.lineSeparator();
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
//		if (isEnabled()) {
//			List<String> list = new ArrayList<String>();
//			Iterator<UIComponent> it = getUIComponentList().iterator();
//			while (it.hasNext()) {
//				UIComponent component = (UIComponent)it.next();
//				if (component instanceof IAction) {
//					String action = component.computeTemplate();
//					if (!action.isEmpty()) {
//						list.add("this."+action);
//					}
//				}
//			}
//			
//			StringBuilder sb = new StringBuilder();
//			if (!list.isEmpty()) {
//				for (String s: list) {
//					sb.append("\t\t\t").append(s).append(";").append(System.lineSeparator());
//				}
//			}
//			return sb.toString();
//		}
//		return "";
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
	
	@Override
	public FolderType getFolderType() {
		return FolderType.EVENT;
	}
}
