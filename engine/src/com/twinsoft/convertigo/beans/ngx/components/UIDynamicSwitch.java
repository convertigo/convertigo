/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.engine.EngineException;

public class UIDynamicSwitch extends UIDynamicAction {

	private static final long serialVersionUID = 4240057045248477382L;
	
	private transient UIActionCaseEvent defaultCaseEvent = null;
	
	public UIDynamicSwitch() {
		super();
	}

	public UIDynamicSwitch(String tagName) {
		super(tagName);
	}

	@Override
	public UIDynamicSwitch clone() throws CloneNotSupportedException {
		UIDynamicSwitch cloned = (UIDynamicSwitch) super.clone();
		cloned.defaultCaseEvent = null;
		return cloned;
	}
	
	protected boolean hasDefaultCaseEvent() {
		checkSubLoaded();
		return this.defaultCaseEvent != null;
	}
	
	protected boolean hasFailureEvent() {
		checkSubLoaded();
		return this.failureEvent != null;
	}
	
	@Override
	protected void addUIComponent(UIComponent uiComponent, Long after) throws EngineException {
		checkSubLoaded();
		
		if (uiComponent instanceof UIActionCaseEvent) {
			boolean isDefaultCase = ((UIActionCaseEvent)uiComponent).isDefaultCase();
    		if (isDefaultCase) {
    			if (hasDefaultCaseEvent()) {
    				throw new EngineException("The action \"" + getName() + "\" already contains a default case event! Please delete it first.");
    			} else {
    				this.defaultCaseEvent = (UIActionCaseEvent)uiComponent;
    			}
    		}
    		
    		if (uiComponent.bNew) {
	    		if (hasFailureEvent()) {
	    			after = this.failureEvent.priority;
	    		}
	    		if (!isDefaultCase && hasDefaultCaseEvent()) {
	    			int pos = getOrderedComponents().get(0).indexOf(this.defaultCaseEvent.priority);
	    			try {
	    				after = getOrderedComponents().get(0).get(pos-1);
	    			} catch (Exception e) {
	    				after = -1L;
	    			}
	    		}
    		}
		}
		
		super.addUIComponent(uiComponent, after);
	}
	
	@Override
	protected void removeUIComponent(UIComponent uiComponent) throws EngineException {
		super.removeUIComponent(uiComponent);
		
        if (uiComponent != null && uiComponent.equals(this.defaultCaseEvent)) {
    		this.defaultCaseEvent = null;
        }
	}
	
	@Override
	protected void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
		if (databaseObject.equals(this.defaultCaseEvent)) {
			return;
		}
		
		int pos = getOrderedComponents().get(0).indexOf(databaseObject.priority);
		if ((hasFailureEvent() ? pos-1 : pos) <= 0) {
			return;
		}
		
		super.increaseOrder(databaseObject, before);
	}
	
	@Override
	protected void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
		if (databaseObject.equals(this.defaultCaseEvent)) {
			return;
		}
		
		if (hasDefaultCaseEvent()) {
			int pos = getOrderedComponents().get(0).indexOf(databaseObject.priority);
			int posd = getOrderedComponents().get(0).indexOf(this.defaultCaseEvent.priority);
			if (pos + 1 == posd) {
				return;
			}
		}
		
		super.decreaseOrder(databaseObject, after);
	}
	
	protected boolean isStacked() {
		return super.isStacked();
	}
	
	static public String escapeStringForTs(String s) {
		String escaped = s;
		if (escaped.indexOf("\"") != -1) {
			escaped = escaped.replaceAll("\"", "'");
		}
		if (escaped.indexOf("'") != -1) {
			escaped = escaped.replaceAll("([\\\\])++'", "'");
			escaped = escaped.replaceAll("([^\\\\])'", "$1\\\\'");
		}
		return escaped;
	}
	
	protected String computeActionContent() {
		if (isEnabled()) {
			IonBean ionBean = getIonBean();
			if (ionBean != null) {
				int numThen = numberOfActions();
				String actionName = getActionName();
				String inputs = computeActionInputs(false);
				
				StringBuilder sbCase = new StringBuilder();
				StringBuilder sbCatch = new StringBuilder();
				StringBuilder sbThen = new StringBuilder();  
				List<String> passthroughCases = new ArrayList<String>();
				Iterator<UIComponent> it = getUIComponentList().iterator();
				while (it.hasNext()) {
					UIComponent component = (UIComponent)it.next();
					if (component.isEnabled()) {
						String sCase = "", sCatch="", sThen = "";
						if (component instanceof UIDynamicAction) {
							sThen = ((UIDynamicAction)component).computeActionContent();
						}
						if (component instanceof UICustomAction) {
							sThen = ((UICustomAction)component).computeActionContent();
						}
						if (component instanceof UIActionFailureEvent) {
							sCatch = ((UIActionFailureEvent)component).computeEvent();
						}
						if (component instanceof UIActionCaseEvent) {
							UIActionCaseEvent caseEvent = (UIActionCaseEvent)component;
							String caseFn = caseEvent.getCaseFn();
							String caseValue = caseEvent.getCaseValue();
							
							int len = caseValue.length();
							char c1 = caseValue.charAt(0);
							char c2 = caseValue.charAt(len-1);
							if (c1 == c2 && Character.toString(c1).matches("[\'\"]")) {
								caseValue = "'" + escapeStringForTs(caseValue.substring(1, len-1)) + "'";
							} else {
								caseValue = escapeStringForTs(caseValue);
								caseValue = "'" + caseValue + "'";
							}
							
							if (caseEvent.isEnabled()) {
								sCase += System.lineSeparator();
								sCase += caseEvent.computeEvent();
								sCase += "\t\tcases["+ caseValue +"] = cases["+ caseValue +"] || []" + System.lineSeparator();
								sCase += "\t\tcases["+ caseValue +"].push("+caseFn+")" + System.lineSeparator();
								for (String cv: passthroughCases) {
									if (!cv.equals(caseValue)) {
										sCase += "\t\tcases["+ cv +"].push("+caseFn+")" + System.lineSeparator();
									}
								}
								
								if (caseEvent.isPassThrough()) {
									if (!passthroughCases.contains(caseValue)) {
										passthroughCases.add(caseValue);
									}
								} else {
									passthroughCases.clear();
								}
							}
						}
						
						if (!sCase.isEmpty()) {
							sbCase.append(sCase);
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
				tsCode += "\t\t" + "let cases = {};" + System.lineSeparator();
				
				if (sbCase.length() > 0) {
					tsCode += sbCase.toString();
				} else {
					; // does nothing
				}
				tsCode += "\t\t" + System.lineSeparator();
				
				tsCode += "\t\tlet self: any = stack[\""+ getName() +"\"] = stack[\""+ priority +"\"] = {event: event};"+ System.lineSeparator();
				tsCode += "\t\tself.in = "+ inputs +";"+ System.lineSeparator();				
//				tsCode +="\t\treturn this.actionBeans."+actionName+
//						"(this, self.in.props, {...stack[\"root\"].in, ...self.in.vars}, cases)"+ System.lineSeparator();
				if (getSharedAction() != null) {
					tsCode +="\t\treturn this.actionBeans."+actionName+
							"(this, {...{stack: stack, parent: parent, out: out}, ...self.in.props}, "+ 
									"{...stack[\"root\"].in, ...params, ...self.in.vars}, cases)"+ 
										System.lineSeparator();
					
				} else {
					tsCode +="\t\treturn this.actionBeans."+actionName+
							"(this, {...{stack: stack, parent: parent, out: out}, ...self.in.props}, "+ 
									"{...stack[\"root\"].in, ...self.in.vars}, cases)"+ 
										System.lineSeparator();
				}
				
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
