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

import java.util.Iterator;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.engine.EngineException;

public class UIDynamicIf extends UIDynamicAction {

	private static final long serialVersionUID = 4624588808588441376L;

	private transient UIActionElseEvent elseEvent = null;
	
	public UIDynamicIf() {
		super();
	}

	public UIDynamicIf(String tagName) {
		super(tagName);
	}

	@Override
	public UIDynamicIf clone() throws CloneNotSupportedException {
		UIDynamicIf cloned = (UIDynamicIf)super.clone();
		cloned.elseEvent = null;
		return cloned;
	}

	protected boolean hasElseEvent() {
		checkSubLoaded();
		return this.elseEvent != null;
	}
	
	@Override
	protected void addUIComponent(UIComponent uiComponent, Long after) throws EngineException {
		checkSubLoaded();
		
		if (uiComponent instanceof UIActionElseEvent) {
    		if (hasElseEvent()) {
    			throw new EngineException("The action \"" + getName() + "\" already contains an else event! Please delete it first.");
    		}
    		
    		this.elseEvent = (UIActionElseEvent)uiComponent;
    		after = null;
		} else {
			try {
				int last = getOrderedComponents().get(0).size() - 1;
				after = hasElseEvent() ? getOrderedComponents().get(0).get(last - 1) : null;
			} catch (Exception e) {
				after = -1L;
			}
		}
		
		super.addUIComponent(uiComponent, after);
	}
	
	@Override
	protected void removeUIComponent(UIComponent uiComponent) throws EngineException {
		super.removeUIComponent(uiComponent);
		
        if (uiComponent != null && uiComponent.equals(this.elseEvent)) {
    		this.elseEvent = null;
        }
	}
	
	@Override
	protected void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
		if (databaseObject.equals(this.elseEvent)) {
			return;
		}
		super.increaseOrder(databaseObject, before);
	}
	
	@Override
	protected void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
		if (databaseObject.equals(this.elseEvent)) {
			return;
		} else if (hasElseEvent()) {
			int pos = getOrderedComponents().get(0).indexOf(databaseObject.priority);
			int last = getOrderedComponents().get(0).size() - 1;
			if (pos+1 >= last) {
				return;
			}			
		}
		super.decreaseOrder(databaseObject, after);
	}
	
	protected boolean isStacked() {
		return super.isStacked();
	}
	
	protected String computeActionContent() {
		if (isEnabled()) {
			IonBean ionBean = getIonBean();
			if (ionBean != null) {
				int numThen = numberOfActions();
				String actionName = getActionName();
				String inputs = computeActionInputs(false);
				
				StringBuilder sbElse = new StringBuilder();
				StringBuilder sbCatch = new StringBuilder();
				StringBuilder sbThen = new StringBuilder();  
				Iterator<UIComponent> it = getUIComponentList().iterator();
				while (it.hasNext()) {
					UIComponent component = (UIComponent)it.next();
					if (component.isEnabled()) {
						String sElse = "", sCatch="", sThen = "";
						if (component instanceof UIDynamicAction) {
							sThen = ((UIDynamicAction)component).computeActionContent();
						}
						if (component instanceof UICustomAction) {
							sThen = ((UICustomAction)component).computeActionContent();
						}
						if (component instanceof UIActionFailureEvent) {
							sCatch = ((UIActionFailureEvent)component).computeEvent();
						}
						if (component instanceof UIActionElseEvent) {
							sElse = ((UIActionElseEvent)component).computeEvent();
						}
						
						if (!sElse.isEmpty()) {
							sbElse.append(sElse);
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
				
				//tsCode += "\t\tlet self: any = stack[\""+ getName() +"\"] = {};"+ System.lineSeparator();
				tsCode += "\t\tlet self: any = stack[\""+ getName() +"\"] = stack[\""+ priority +"\"] = {event: event};"+ System.lineSeparator();
				tsCode += "\t\tself.in = "+ inputs +";"+ System.lineSeparator();
				tsCode +="\t\treturn this.actionBeans."+actionName+
						"(this, self.in.props, {...stack[\"root\"].in, ...self.in.vars})"+ System.lineSeparator();
				
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
				tsCode += "\t\tif (res == true) {"+ System.lineSeparator();
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
				tsCode += "\t\t} else if (res == false) {"+ System.lineSeparator();
				if (sbElse.toString().isEmpty()) {
					tsCode += "\t\tthis.c8o.log.debug(\"For '"+getName()+"' condition is not verified. No Else handler, skipping and resolve false\");" + System.lineSeparator();
					tsCode += "\t\tresolve(false)" + System.lineSeparator();
				} else {
					tsCode += "\t\t"+ sbElse.toString().replaceFirst("\t\t", "");
				}
				tsCode += "\t\t}"+ System.lineSeparator();
				tsCode += "\t\t}, (error: any) => {if (\"c8oSkipError\" === error.message) {resolve(false);} else {this.c8o.log.debug(\"[MB] "+actionName+" : \", error.message);throw new Error(error);}})"+ System.lineSeparator();
				tsCode += "\t\t.then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})"+ System.lineSeparator();
				tsCode += "\t\t})"+ System.lineSeparator();
				return tsCode;
			}
		}
		return "";
	}
	
}
