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

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.engine.EngineException;

public class UIDynamicIterate extends UIDynamicAction {

	private static final long serialVersionUID = 9181345960598150591L;

	private transient UIActionLoopEvent loopEvent = null;
	
	public UIDynamicIterate() {
		super();
	}

	public UIDynamicIterate(String tagName) {
		super(tagName);
	}

	@Override
	public UIDynamicIterate clone() throws CloneNotSupportedException {
		UIDynamicIterate cloned = (UIDynamicIterate) super.clone();
		cloned.loopEvent = null;
		return cloned;
	}
	
	
	protected boolean hasLoopEvent() {
		checkSubLoaded();
		return this.loopEvent != null;
	}
	
	protected boolean hasFailureEvent() {
		checkSubLoaded();
		return this.failureEvent != null;
	}
	
	@Override
	protected void addUIComponent(UIComponent uiComponent, Long after) throws EngineException {
		checkSubLoaded();
		
		if (uiComponent instanceof UIActionLoopEvent) {
    		if (hasLoopEvent()) {
    			throw new EngineException("The action \"" + getName() + "\" already contains a loop event! Please delete it first.");
    		}
    		else {
    			this.loopEvent = (UIActionLoopEvent)uiComponent;
    			after = hasFailureEvent() ? this.failureEvent.priority : -1L;
    		}
		}
		
		super.addUIComponent(uiComponent, after);
	}
	
	@Override
	protected void removeUIComponent(UIComponent uiComponent) throws EngineException {
		super.removeUIComponent(uiComponent);
		
        if (uiComponent != null && uiComponent.equals(this.loopEvent)) {
    		this.loopEvent = null;
        }
	}
	
	@Override
	protected void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
		if (databaseObject.equals(this.loopEvent)) {
			return;
		} else if (hasLoopEvent()) {
			int pos = getOrderedComponents().get(0).indexOf(databaseObject.priority);
			if ((hasFailureEvent() ? pos-2 : pos-1) <= 0) {
				return;
			}
		}
		super.increaseOrder(databaseObject, before);
	}
	
	@Override
	protected void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
		if (databaseObject.equals(this.loopEvent)) {
			return;
		}
		super.decreaseOrder(databaseObject, after);
	}
	
	protected boolean handleLoop() {
		boolean handleLoop = false;
		if (hasLoopEvent()) {
			if (this.loopEvent.isEnabled()) {
				if (this.loopEvent.numberOfActions() > 0) {
					handleLoop = true;
				}
			}
		}
		return handleLoop;
	}
	
	protected boolean isStacked() {
		return handleLoop() || super.isStacked();
	}
	
	protected String computeActionContent() {
		if (isEnabled()) {
			IonBean ionBean = getIonBean();
			if (ionBean != null) {
				int numThen = numberOfActions();
				String actionName = getActionName();
				String inputs = computeActionInputs(false);
				
				StringBuilder sbLoop = new StringBuilder();
				StringBuilder sbCatch = new StringBuilder();
				StringBuilder sbThen = new StringBuilder();  
				Iterator<UIComponent> it = getUIComponentList().iterator();
				while (it.hasNext()) {
					UIComponent component = (UIComponent)it.next();
					if (component.isEnabled()) {
						String sLoop = "", sCatch="", sThen = "";
						if (component instanceof UIDynamicAction) {
							sThen = ((UIDynamicAction)component).computeActionContent();
						}
						if (component instanceof UICustomAction) {
							sThen = ((UICustomAction)component).computeActionContent();
						}
						if (component instanceof UIActionFailureEvent) {
							sCatch = ((UIActionFailureEvent)component).computeEvent();
						}
						if (component instanceof UIActionLoopEvent) {
							sLoop = ((UIActionLoopEvent)component).computeEvent();
						}
						
						if (!sLoop.isEmpty()) {
							sbLoop.append(sLoop);
						}
						if (!sCatch.isEmpty()) {
							sbCatch.append(sCatch);
						}
						if (!sThen.isEmpty()) {
							sbThen.append(sbThen.length()>0 && numThen > 1 ? "\t\t,"+ System.lineSeparator() :"")
							.append(sThen);
						}
					}
				}
	
				String tsCode = "";
				tsCode += "\t\tnew Promise((resolve, reject) => {"+ System.lineSeparator();
				tsCode += "\t\t" + System.lineSeparator();
				
				if (sbLoop.length() > 0) {
					tsCode += sbLoop.toString();
				} else {
					tsCode += "\t\tconst doLoop = (c8oPage : C8oPageBase, item : any, index : number) : Promise<any> => {" + System.lineSeparator();
					tsCode += "\t\treturn Promise.reject('no loop handler');" + System.lineSeparator();
					tsCode += "\t\t}" + System.lineSeparator();
				}
				tsCode += "\t\t" + System.lineSeparator();
				
				//tsCode += "\t\tlet self: any = stack[\""+ getName() +"\"] = {};"+ System.lineSeparator();
				tsCode += "\t\tlet self: any = stack[\""+ getName() +"\"] = stack[\""+ priority +"\"] = {};"+ System.lineSeparator();
				tsCode += "\t\tself.in = "+ inputs +";"+ System.lineSeparator();
				tsCode +="\t\treturn this.actionBeans."+actionName+
						"(this, self.in.props, {...stack[\"root\"].in, ...self.in.vars}, doLoop)"+ System.lineSeparator();
				
				tsCode += "\t\t.catch((error:any) => {"+ System.lineSeparator();
				tsCode += "\t\tparent = self;"+ System.lineSeparator();
				tsCode += "\t\tparent.out = error;"+ System.lineSeparator();
				tsCode += "\t\tout = parent.out;"+ System.lineSeparator();
				if (sbCatch.length() > 0) {
					tsCode += "\t\t"+ sbCatch.toString();
				} else {
					tsCode += "\t\treturn Promise.reject(error);"+ System.lineSeparator();
				}
				tsCode += "\t\t})"+ System.lineSeparator();
				tsCode += "\t\t.then((res:any) => {"+ System.lineSeparator();
				tsCode += "\t\tparent = self;"+ System.lineSeparator();
				tsCode += "\t\tparent.out = res;"+ System.lineSeparator();
				tsCode += "\t\tout = parent.out;"+ System.lineSeparator();
				if (sbThen.length() > 0) {
					if (numThen > 1) {
						tsCode += "\t\treturn Promise.all(["+ System.lineSeparator();
						tsCode += sbThen.toString();
						tsCode += "\t\t])"+ System.lineSeparator();
					} else {
						tsCode += "\t\treturn "+ sbThen.toString().replaceFirst("\t\t", "");
					}
				} else {
					tsCode += "\t\treturn Promise.resolve(res);"+ System.lineSeparator();
				}
				tsCode += "\t\t}, (error: any) => {this.c8o.log.debug(\"[MB] "+actionName+" : \", error.message);throw new Error(error);})"+ System.lineSeparator();
				tsCode += "\t\t.then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})"+ System.lineSeparator();
				tsCode += "\t\t})"+ System.lineSeparator();
				return tsCode;
			}
		}
		return "";
	}
}
