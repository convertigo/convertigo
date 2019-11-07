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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.EngineException;

public class UIActionStack extends UIComponent implements IShared {

	private static final long serialVersionUID = -5668525501858865747L;

	private transient UIActionErrorEvent errorEvent = null;
	private transient UIActionFinallyEvent finallyEvent = null;
	
	public UIActionStack() {
		super();
	}

	@Override
	public UIActionStack clone() throws CloneNotSupportedException {
		UIActionStack cloned = (UIActionStack) super.clone();
		cloned.errorEvent = null;
		cloned.finallyEvent = null;
		return cloned;
	}

	@Override
	protected void addUIComponent(UIComponent uiComponent, Long after) throws EngineException {
		checkSubLoaded();
		
		if (uiComponent instanceof UIActionErrorEvent) {
    		if (this.errorEvent != null) {
    			throw new EngineException("The action \"" + getName() + "\" already contains an error handler! Please delete it first.");
    		}
    		else {
    			this.errorEvent = (UIActionErrorEvent)uiComponent;
    			after = -1L;// to be first
    		}
		}
		if (uiComponent instanceof UIActionFinallyEvent) {
    		if (this.finallyEvent != null) {
    			throw new EngineException("The action \"" + getName() + "\" already contains a finally handler! Please delete it first.");
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
	
	protected UIActionErrorEvent getErrorEvent() {
		return this.errorEvent;
	}
	
	protected UIActionFinallyEvent getFinallyEvent() {
		return this.finallyEvent;
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
	
	public String getActionName() {
		return "STS"+ this.priority;
	}
	
	private String getFunctionName() {
		return getActionName();
	}

	@Override
	public String computeTemplate() {
		return null;
	}
	
	public List<UIStackVariable> getVariables() {
		List<UIStackVariable> list = new ArrayList<>();
		Iterator<UIComponent> it = getUIComponentList().iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (component instanceof UIStackVariable) {
				list.add((UIStackVariable)component);
			}
		}
		return Collections.unmodifiableList(list);
	}
	
	protected String computeStackParams() {
		StringBuilder sbParams = new StringBuilder();
		if (isEnabled()) {
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof UIStackVariable) {
					UIStackVariable param = (UIStackVariable)component;
					
					Object paramValue = param.getVariableValue();
					if (!paramValue.toString().isEmpty()) {
						paramValue = paramValue.toString().replaceAll("this\\.", "page.");
						sbParams.append(sbParams.length() > 0 ? ", " : "");
						sbParams.append(param.getVariableName()).append(": ");
						sbParams.append("get('"+ param.getVariableName() +"', `"+ paramValue +"`)");
					}
				}
			}
		}
		return "{"+ sbParams +"}";
	}
	
	protected String computeStackFunction() {
		String computed = "";
		if (isEnabled()) {
			StringBuilder sbCatch = new StringBuilder();
			if (handleError()) {
				UIActionErrorEvent errorEvent = getErrorEvent();
				sbCatch.append(errorEvent.computeEvent());
			}
			StringBuilder sbFinally = new StringBuilder();
			if (handleFinally()) {
				UIActionFinallyEvent finallyEvent = getFinallyEvent();
				sbFinally.append(finallyEvent.computeEvent());
			}
			
			String cafPageType = compareToTplVersion("7.5.2.0") >= 0 ? "C8oPageBase":"C8oPage";
			
			StringBuilder cartridge = new StringBuilder();
			cartridge.append("\t/**").append(System.lineSeparator())
						.append("\t * Function "+ getName()).append(System.lineSeparator());
			for (String commentLine : getComment().split(System.lineSeparator())) {
				cartridge.append("\t *   ").append(commentLine).append(System.lineSeparator());
			}
			cartridge.append("\t * ").append(System.lineSeparator());
			cartridge.append("\t * @param page  , the current page").append(System.lineSeparator());
			cartridge.append("\t * @param props , the object which holds properties key-value pairs").append(System.lineSeparator());
			cartridge.append("\t * @param vars  , the object which holds variables key-value pairs").append(System.lineSeparator());
			cartridge.append("\t * @param event , the current event object").append(System.lineSeparator());
			cartridge.append("\t */").append(System.lineSeparator());
			
			String functionName = getFunctionName();
			
			StringBuilder parameters = new StringBuilder();
			parameters.append("page: "+ cafPageType +", props, vars, event: any");
			
			computed += System.lineSeparator();
			computed += cartridge;
			computed += "\t"+ functionName + "("+ parameters +"): Promise<any> {" + System.lineSeparator();
			computed += "\t\tlet c8oPage : "+ cafPageType +" = page;" + System.lineSeparator();
			computed += "\t\tlet hasStack = props[\"stack\"] != undefined;" + System.lineSeparator();
			computed += "\t\tlet stack = hasStack ? props[\"stack\"] : {root: {scope: {}, in: {}, out: event}};" + System.lineSeparator();
			computed += "\t\tlet scope = stack[\"root\"].scope;" + System.lineSeparator();
			computed += "\t\tlet parent = props[\"parent\"];" + System.lineSeparator();
			computed += "\t\tlet out = event;" + System.lineSeparator();
			computed += "\t\tlet self;" + System.lineSeparator();
			computed += "\t\t" + System.lineSeparator();
			computed += computeInnerGet("page",functionName);
			computed += "\t\t" + System.lineSeparator();
			
			computed += "\t\tlet params = { ..."+ computeStackParams() +", ...vars};" + System.lineSeparator();
			
			computed += "\t\t" + System.lineSeparator();
			computed += "\t\tpage.c8o.log.debug(\"[MB] "+functionName+": started\");" + System.lineSeparator();
			computed += "\t\treturn new Promise((resolveP, rejectP)=>{" + System.lineSeparator();
			computed += "\t\tparent = self = stack[\""+ getName() +"\"] = {};"+ System.lineSeparator();
			computed += "\t\tself.in = {props: props, vars: params};"+ System.lineSeparator();
			computed += "\t\t" + System.lineSeparator();
			computed += ""+ computeStackContent();
			if (sbCatch.length() > 0) {
				String sCatch = sbCatch.toString();
				sCatch = sCatch.replaceAll("this", "page");
				sCatch = sCatch.replaceAll("page\\.actionBeans\\.", "this.");
				
				computed += "\t\t.catch((error:any) => {"+ System.lineSeparator();
				computed += "\t\tparent = self;"+ System.lineSeparator();
				computed += "\t\tparent.out = error;"+ System.lineSeparator();
				computed += "\t\tout = parent.out;"+ System.lineSeparator();
				computed += "\t\t"+ sCatch;
				computed += "\t\t})"+ System.lineSeparator();
			}
			computed += "\t\t.catch((error:any) => {page.c8o.log.debug(\"[MB] "+functionName+": An error occured : \",error.message); resolveP(false);})" + System.lineSeparator();
			if (sbFinally.length() > 0) {
				String sFinally = sbFinally.toString();
				sFinally = sFinally.replaceAll("this", "page");
				sFinally = sFinally.replaceAll("page\\.actionBeans\\.", "this.");
				
				computed += "\t\t.then((res:any) => {"+ System.lineSeparator();
				computed += "\t\tparent = self;"+ System.lineSeparator();
				computed += "\t\tparent.out = res;"+ System.lineSeparator();
				computed += "\t\tout = parent.out;"+ System.lineSeparator();
				computed += "\t\t"+ sFinally;
				computed += "\t\t})"+ System.lineSeparator();
				computed += "\t\t.catch((error:any) => {page.c8o.log.debug(\"[MB] "+functionName+": An error occured : \",error.message); resolveP(false);})" + System.lineSeparator();
			}
			computed += "\t\t.then((res:any) => {page.c8o.log.debug(\"[MB] "+functionName+": ended\"); resolveP(res)});" + System.lineSeparator();
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
		
	protected String computeStackContent() {
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
							s = ((UIDynamicAction)component).computeActionContent();
						}
						if (component instanceof UICustomAction) {
							s = ((UICustomAction)component).computeActionContent();
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
			
			tsCode = tsCode.replaceAll("this", "page");
			tsCode = tsCode.replaceAll("page\\.actionBeans\\.", "this.");
			return tsCode;
		}
		return "";
	}

	@Override
	protected Contributor getContributor() {
		return new Contributor() {
			@Override
			public Map<String, String> getActionTsFunctions() {
				Map<String, String> functions = new HashMap<String, String>();
				String functionName = getFunctionName();
				String functionCode = computeStackFunction();
				if (compareToTplVersion("7.5.2.0") < 0 ) {
					functionCode = functionCode.replaceFirst("C8oPageBase", "C8oPage");
					functionCode = functionCode.replaceAll("C8oCafUtils\\.merge", "page.merge");
				}
				functions.put(functionName, functionCode);
				return functions;
			}

			@Override
			public Map<String, String> getActionTsImports() {
				Map<String, String> imports = new HashMap<String, String>();
				imports.put("* as ts", "typescript");
				return imports;
			}

			@Override
			public Map<String, File> getCompBeanDir() {
				return new HashMap<String, File>();
			}

			@Override
			public Map<String, String> getModuleTsImports() {
				return new HashMap<String, String>();
			}

			@Override
			public Set<String> getModuleNgImports() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgProviders() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgDeclarations() {
				return new HashSet<String>();
			}
			
			@Override
			public Set<String> getModuleNgComponents() {
				return new HashSet<String>();
			}
			
			@Override
			public Map<String, String> getPackageDependencies() {
				return new HashMap<String, String>();
			}

			@Override
			public Map<String, String> getConfigPlugins() {
				return new HashMap<String, String>();
			}
			
		};
	}

	@Override
	protected void addContributors(Set<UIComponent> done, List<Contributor> contributors) {
		super.addContributors(done, contributors);
	}

}
