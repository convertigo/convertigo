/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboFolderType;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.util.EnumUtils;

@DboFolderType(type = FolderType.EVENT)
public class UISharedComponentEvent extends UIComponent implements IEventGenerator, ITagsProperty {

	private static final long serialVersionUID = 4054671808848891791L;
	
	protected transient UIActionErrorEvent errorEvent = null;
	protected transient UIActionFinallyEvent finallyEvent = null;
	
	public enum ComponentEvent {
		onChanges("onChanges","compChanges"),
		onInit("onInit","compInit"),
		onDoCheck("onDoCheck","compDoCheck"),
		onAfterContentInit("onAfterContentInit","compAfterContentInit"),
		onAfterContentChecked("onAfterContentChecked","compAfterContentChecked"),
		onAfterViewInit("onAfterViewInit","compAfterViewInit"),
		onAfterViewChecked("onAfterViewChecked","compAfterViewChecked"),
		onDestroy("onDestroy","compDestroy")
		;
		
		String label;
		String event;
		ComponentEvent(String event, String label) {
			this.event = event;
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
		
		String computeEvent(MobileComponent mc, List<UISharedComponentEvent> eventList) {
			StringBuffer children = new StringBuffer();
			Set<String> done = new HashSet<String>();
			for (UISharedComponentEvent compEvent : eventList) {
				if (compEvent.getComponentEvent().equals(this)) {
					String functionCall = "";
					String scope = this.equals(ComponentEvent.onChanges) ? "changes: changes" : "";
					if (mc instanceof UISharedComponent) {
						IScriptComponent main = compEvent.getMainScriptComponent();
						if (mc.equals(((UISharedComponent)main))) {
							if (((UISharedComponent)mc).isEnabled()) {
								functionCall = "this." + compEvent.getEventFunctionName() + "({root: {scope:{"+scope+"}, in:{}, out:'"+ this.event +"'}})";
							}
						} else {
							String identifier = ((UISharedComponent)main).getNsIdentifier();
							if (done.add(identifier)) {
								functionCall = "this.all_"+ identifier +".forEach(x => x."+ this.event + "())";
							}
						}
					} else {
						IScriptComponent main = compEvent.getMainScriptComponent();
						if (main instanceof UISharedComponent) {
							String identifier = ((UISharedComponent)main).getNsIdentifier();
							if (done.add(identifier)) {
								functionCall = "this.all_"+ identifier +".forEach(x => x."+ this.event + "())";
							}
						} else {
							functionCall = "this." + compEvent.getEventFunctionName() + "({root: {scope:{"+scope+"}, in:{}, out:'"+ this.event +"'}})";
						}
					}
					if (!functionCall.isBlank()) {
						children.append("\t\t\t" + functionCall).append(System.lineSeparator());
					}
				}
			}
			
			String params = ComponentEvent.onChanges.equals(this) ? "changes: SimpleChanges":"";
			StringBuffer sb = new StringBuffer();
			sb.append(System.lineSeparator());
			sb.append("\t"+event).append("("+params+") {").append(System.lineSeparator());
			sb.append("\t\ttry {").append(System.lineSeparator());
			sb.append(children);	
			sb.append("\t\t} catch(e) {").append(System.lineSeparator());
			sb.append("\t\t\tconsole.log(e)").append(System.lineSeparator());
			sb.append("\t\t}").append(System.lineSeparator());
			sb.append("\t}").append(System.lineSeparator());
			return sb.toString();
		}
	}
	
	public UISharedComponentEvent() {
		super();
	}

	@Override
	public UISharedComponentEvent clone() throws CloneNotSupportedException {
		UISharedComponentEvent cloned = (UISharedComponentEvent) super.clone();
		cloned.errorEvent = null;
		cloned.finallyEvent = null;
		return cloned;
	}

	private ComponentEvent componentEvent = ComponentEvent.onAfterViewInit;

	public ComponentEvent getComponentEvent() {
		return componentEvent;
	}

	public void setComponentEvent(ComponentEvent componentEvent) {
		this.componentEvent = componentEvent;
	}
	
	protected void setChildOf(DatabaseObject dbo) {
		this.parent = dbo;
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
			if (compareToTplVersion("8.4.0.0") >= 0) {
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
		JSONObject jsonModel = new JSONObject();
		try {
			jsonModel.put("out", new JSONObject());
		} catch (JSONException e) {}
		return jsonModel.toString();
	}
	
	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("componentEvent")) {
			return EnumUtils.toStrings(ComponentEvent.class);
		}
		return new String[0];
	}
	
	@Override
	public String toString() {
		String label = componentEvent.label;
		return label.isEmpty() ? "?":label;
	}
}
