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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.beans.mobile.components.UIControlDirective.AttrDirective;
import com.twinsoft.convertigo.beans.mobile.components.UIControlEvent.AttrEvent;
import com.twinsoft.convertigo.engine.EngineException;

public class UICustomAction extends UIComponent implements IAction {

	private static final long serialVersionUID = 4203444295012733219L;

	private transient UIActionFailureEvent failureEvent = null;
	
	public UICustomAction() {
		super();
	}
	
	@Override
	public UICustomAction clone() throws CloneNotSupportedException {
		UICustomAction cloned = (UICustomAction) super.clone();
		cloned.failureEvent = null;
		return cloned;
	}
	
	@Override
	protected void addUIComponent(UIComponent uiComponent, Long after) throws EngineException {
		checkSubLoaded();
		
		if (uiComponent instanceof UIActionFailureEvent) {
    		if (this.failureEvent != null) {
    			throw new EngineException("The action \"" + getName() + "\" already contains a failure event! Please delete it first.");
    		}
    		else {
    			this.failureEvent = (UIActionFailureEvent)uiComponent;
    			after = -1L;// to be first
    		}
		}
		
		super.addUIComponent(uiComponent, after);
	}
	
	@Override
	protected void removeUIComponent(UIComponent uiComponent) throws EngineException {
		super.removeUIComponent(uiComponent);
		
        if (uiComponent != null && uiComponent.equals(this.failureEvent)) {
    		this.failureEvent = null;
        }
	}
	
	@Override
	protected void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
		if (databaseObject.equals(this.failureEvent)) {
			return;
		} else if (this.failureEvent != null) {
			int pos = getOrderedComponents().get(0).indexOf(databaseObject.priority);
			if (pos-1 <= 0) {
				return;
			}
		}
		super.increaseOrder(databaseObject, before);
	}
	
	@Override
	protected void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
		if (databaseObject.equals(this.failureEvent)) {
			return;
		}
		super.decreaseOrder(databaseObject, after);
	}
	
	@Override
	public String getFunctionName() {
		return "ATS"+ this.priority;
	}
	
	public String getActionName() {
		return "CTS"+ this.priority;
	}

	/*
	 * The needed page imports
	 */
	private XMLVector<XMLVector<String>> page_ts_imports = new XMLVector<XMLVector<String>>();
	
	public XMLVector<XMLVector<String>> getPageTsImports() {
		return page_ts_imports;
	}
	
	public void setPageTsImports(XMLVector<XMLVector<String>> page_ts_imports) {
		this.page_ts_imports = page_ts_imports;
	}
	
	/*
	 * The needed module imports
	 */
	private XMLVector<XMLVector<String>> module_ts_imports = new XMLVector<XMLVector<String>>();
	
	public XMLVector<XMLVector<String>> getModuleTsImports() {
		return module_ts_imports;
	}
	
	public void setModuleTsImports(XMLVector<XMLVector<String>> module_ts_imports) {
		this.module_ts_imports = module_ts_imports;
	}

	/*
	 * The needed ngModule imports
	 */
	private XMLVector<XMLVector<String>> module_ng_imports = new XMLVector<XMLVector<String>>();
	
	public XMLVector<XMLVector<String>> getModuleNgImports() {
		return module_ng_imports;
	}
	
	public void setModuleNgImports(XMLVector<XMLVector<String>> module_ng_imports) {
		this.module_ng_imports = module_ng_imports;
	}

	/*
	 * The needed ngModule providers
	 */
	private XMLVector<XMLVector<String>> module_ng_providers = new XMLVector<XMLVector<String>>();
	
	public XMLVector<XMLVector<String>> getModuleNgProviders() {
		return module_ng_providers;
	}
	
	public void setModuleNgProviders(XMLVector<XMLVector<String>> module_ng_providers) {
		this.module_ng_providers = module_ng_providers;
	}

	/*
	 * The needed package dependencies
	 */
	private XMLVector<XMLVector<String>> package_dependencies = new XMLVector<XMLVector<String>>();
	
	public XMLVector<XMLVector<String>> getPackageDependencies() {
		return package_dependencies;
	}
	
	public void setPackageDependencies(XMLVector<XMLVector<String>> package_dependencies) {
		this.package_dependencies = package_dependencies;
	}

	/*
	 * The needed cordova plugins
	 */
	private XMLVector<XMLVector<String>> cordova_plugins = new XMLVector<XMLVector<String>>();
	
	public XMLVector<XMLVector<String>> getCordovaPlugins() {
		return cordova_plugins;
	}
	
	public void setCordovaPlugins(XMLVector<XMLVector<String>> cordova_plugins) {
		this.cordova_plugins = cordova_plugins;
	}

	/*
	 * The action value
	 */
	private FormatedContent actionValue = new FormatedContent("\t\tpage.c8o.log.debug(event ? event.toString():'no event');\n\t\tresolve();\n");
	
	public FormatedContent getActionValue() {
		return actionValue;
	}

	public void setActionValue(FormatedContent actionValue) {
		this.actionValue = actionValue;
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
	
	protected boolean handleFailure() {
		boolean handleFailure = false;
		if (this.failureEvent != null) {
			if (this.failureEvent.isEnabled()) {
				if (this.failureEvent.numberOfActions() > 0) {
					handleFailure = true;
				}
			}
		}
		return handleFailure;
	}
	
	protected boolean handleError() {
		boolean handleError = false;
		UIActionErrorEvent errorEvent = getParentErrorEvent();
		if (errorEvent != null && errorEvent.isEnabled()) {
			if (errorEvent.numberOfActions() > 0) {
				handleError = true;
			}
		}
		return handleError;
	}

	private UIActionErrorEvent getParentErrorEvent() {
		DatabaseObject parent = getParent();
		if (parent != null ) {
			if (parent instanceof UIControlEvent) {
				UIControlEvent uiControlEvent = (UIControlEvent)parent;
				if (uiControlEvent.isEnabled()) {
					return uiControlEvent.getErrorEvent();
				}
			} else if (parent instanceof UIAppEvent) {
				UIAppEvent uiAppEvent = (UIAppEvent)parent;
				if (uiAppEvent.isEnabled()) {
					return uiAppEvent.getErrorEvent();
				}
			} else if (parent instanceof UIPageEvent) {
				UIPageEvent uiPageEvent = (UIPageEvent)parent;
				if (uiPageEvent.isEnabled()) {
					return uiPageEvent.getErrorEvent();
				}
			} else if (parent instanceof UIEventSubscriber) {
				UIEventSubscriber uiEventSubscriber = (UIEventSubscriber)parent;
				if (uiEventSubscriber.isEnabled()) {
					return uiEventSubscriber.getErrorEvent();
				}
			}
		}
		return null;
	}

	protected boolean isStacked() {
		return handleError() || handleFailure() || numberOfActions() > 0 || 
				getParent() instanceof UIAppEvent || getParent() instanceof UIPageEvent ||
				getParent() instanceof UIEventSubscriber;
	}
	
	protected String getScope() {
		UICustomAction original = (UICustomAction) getOriginal();
		UISharedComponent sharedComponent = original.getSharedComponent();
		boolean isInSharedComponent = sharedComponent  != null;
		
		String scope = "";
		
		DatabaseObject parent = getParent();
		while (parent != null && !(parent instanceof UIAppEvent) && !(parent instanceof UIPageEvent)&& !(parent instanceof UIEventSubscriber)) {
			if (parent instanceof UIUseShared) {
				UISharedComponent uisc = ((UIUseShared) parent).getTargetSharedComponent();
				if (uisc != null) {
					scope += !scope.isEmpty() ? ", ":"";
					scope += "params"+uisc.priority + ": "+ "params"+uisc.priority;
				}
				if (isInSharedComponent) {
					break;
				}
			}
			if (parent instanceof UIControlDirective) {
				UIControlDirective uicd = (UIControlDirective)parent;
				if (AttrDirective.ForEach.equals(AttrDirective.getDirective(uicd.getDirectiveName()))) {
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
				scope = "merge(merge({}, params"+ sharedComponent.priority +".scope), {"+ scope +"})";
			} else {
				scope = "merge({}, {"+ scope +"})";
			}
		} else {
			scope = "{}";
		}
		
		return scope;
	}
	
	private boolean underSubmitEvent() {
		DatabaseObject dbo = getParent();
		if (dbo != null && dbo instanceof UIControlEvent) {
			return ((UIControlEvent)dbo).getAttrName().equals(AttrEvent.onSubmit.event());
		}
		return false;
	}
	
	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			String formGroupName = null;
			if (underSubmitEvent()) {
				UIForm uiForm = getUIForm();
				if (uiForm != null) {
					formGroupName = uiForm.getFormGroupName();
				}
			}
			
			String scope = getScope();
			String in = formGroupName == null ? "{}": "merge({},"+formGroupName +".value)";
			if (isStacked()) {
				return getFunctionName() + "({root: {scope:"+ scope +", in:"+ in +", out:$event}})";
			} else {
				String props = "{}", vars = "{}";
				String inputs = computeActionInputs(true);
				Pattern pattern = Pattern.compile("\\{props:(\\{.*\\}), vars:(\\{.*\\})\\}");
				Matcher matcher = pattern.matcher(inputs);
				if (matcher.matches()) {
					props = matcher.group(1);
					vars = matcher.group(2);
				}
				
				if (formGroupName != null) {
					vars = "merge(merge({},"+formGroupName +".value), "+ vars +")";
				}
				
				String stack = "{stack:{root: {scope:"+ scope +", in:"+ in +", out:$event}}}";
				props = "merge(merge({},"+ props  +"), "+ stack +")";
				
				String actionName = getActionName();
				return ""+ actionName + "(this,"+ props + ","+ vars +", $event)";
			}
		}
		return "";
	}

	protected String computeActionInputs(boolean forTemplate) {
		if (isEnabled()) {
			StringBuilder sbProps = new StringBuilder();
			
			StringBuilder sbVars = new StringBuilder();
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof UIControlVariable) {
					UIControlVariable uicv = (UIControlVariable)component;
					if (uicv.isEnabled()) {
						// Case code generated in HTML
						if (forTemplate) {
							String varValue = uicv.getVarValue();
							if (!varValue.isEmpty()) {
								sbVars.append(sbVars.length() > 0 ? ", ":"");
								sbVars.append(uicv.getVarName()).append(": ");
								sbVars.append(varValue);
							}
						}
						// Case code generated in TS
						else {
							MobileSmartSourceType msst = uicv.getVarSmartType();
							
							String smartValue = msst.getValue();
							if (Mode.PLAIN.equals(msst.getMode())) {
								smartValue = "\'" + MobileSmartSourceType.escapeStringForTs(smartValue) + "\'";
							}
							
							if (Mode.SOURCE.equals(msst.getMode())) {
								MobileSmartSource mss = msst.getSmartSource();
								if (mss != null) {
									if (mss.getFilter().equals(MobileSmartSource.Filter.Iteration)) {
										smartValue = "scope."+ smartValue;
									}
									else {
										smartValue = "this."+ smartValue;
									}
								}
							}
							smartValue = smartValue.replaceAll("\\?\\.", ".");
							smartValue = smartValue.replaceAll("this\\.", "c8oPage.");
							if (paramsPattern.matcher(smartValue).lookingAt()) {
								smartValue = "scope."+ smartValue;
							}
							
							if (!smartValue.isEmpty()) {
								sbVars.append(sbVars.length() > 0 ? ", ":"");
								sbVars.append(uicv.getVarName()).append(": ");
								sbVars.append("get('"+ uicv.getVarName() +"', `"+smartValue+"`)");
							}
						}
					}
				}
			}
			
			return "{props:{"+sbProps+"}, vars:{"+sbVars+"}}";
		}
		return "";
	}
	
	@Override
	public void computeScripts(JSONObject jsonScripts) {
		IScriptComponent main = getMainScriptComponent();
		if (main == null) {
			return;
		}
		
		try {
			String imports = jsonScripts.getString("imports");
			for (XMLVector<String> v : page_ts_imports) {
				String name = v.get(0).trim();
				String path = v.get(1).trim();
				if (main.addImport(name, path)) {
					if (name.indexOf(" as ") != -1) {
						imports += "import "+name+" from '"+path+"';" + System.lineSeparator();
					} else {
						imports += "import { "+name+" } from '"+path+"';" + System.lineSeparator();
					}
				}
			}
			
			if (main.addImport("* as ts", "typescript")) {
				imports += "import * as ts from 'typescript';" + System.lineSeparator();
			}
			
			jsonScripts.put("imports", imports);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		DatabaseObject parent = getParent();
		if (parent != null && !(parent instanceof IAction) && !(parent instanceof UIActionEvent)) {
			try {
				String functions = jsonScripts.getString("functions");
				String fname = getFunctionName();
				String fcode = computeActionFunction();
				if (main.addFunction(fname, fcode)) {
					functions += System.lineSeparator() + fcode;
				}
				jsonScripts.put("functions", functions);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		try {
			String functions = jsonScripts.getString("functions");
			String fname = getActionName();
			String fcode = computeActionMain();
			if (main.addFunction(fname, fcode)) {
				functions += System.lineSeparator() + fcode;
			}
			jsonScripts.put("functions", functions);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		super.computeScripts(jsonScripts);
	}

	protected String computeActionFunction() {
		String computed = "";
		if (isEnabled() && isStacked()) {
			StringBuilder sbCatch = new StringBuilder();
			if (handleError()) {
				UIActionErrorEvent errorEvent = getParentErrorEvent();
				sbCatch.append(errorEvent.computeEvent());
			}
			
			StringBuilder parameters = new StringBuilder();
			parameters.append("stack");
			
			StringBuilder cartridge = new StringBuilder();
			cartridge.append("\t/**").append(System.lineSeparator())
						.append("\t * Function "+ getFunctionName()).append(System.lineSeparator());
			for (String commentLine : getComment().split(System.lineSeparator())) {
				cartridge.append("\t *   ").append(commentLine).append(System.lineSeparator());
			}
			cartridge.append("\t * ").append(System.lineSeparator());
			cartridge.append("\t * @param stack , the object which holds actions stack").append(System.lineSeparator());
			cartridge.append("\t */").append(System.lineSeparator());
			
			String cafPageType = compareToTplVersion("7.5.2.0") >= 0 ? "C8oPageBase":"C8oPage";
			String functionName = getFunctionName();
			
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
			computed += ""+ computeActionContent();
			if (sbCatch.length() > 0) {
				computed += "\t\t.catch((error:any) => {"+ System.lineSeparator();
				computed += "\t\tparent = self;"+ System.lineSeparator();
				computed += "\t\tparent.out = error;"+ System.lineSeparator();
				computed += "\t\tout = parent.out;"+ System.lineSeparator();
				computed += "\t\t"+ sbCatch.toString();
				computed += "\t\t})"+ System.lineSeparator();
			}			
			computed += "\t\t.catch((error:any) => {this.c8o.log.debug(\"[MB] "+functionName+": An error occured : \",error.message); resolveP(false);})" + System.lineSeparator();
			computed += "\t\t.then((res:any) => {this.c8o.log.debug(\"[MB] "+functionName+": ended\"); resolveP(res)});" + System.lineSeparator();
			computed += "\t\t});"+System.lineSeparator();
			computed += "\t}";
		}
		return computed;
	}
	
	protected String computeActionContent() {
		if (isEnabled()) {
			int numThen = numberOfActions();
			String beanName = getName();
			String actionName = getActionName();
			String inputs = computeActionInputs(false);
			
			StringBuilder sbCatch = new StringBuilder();
			StringBuilder sbThen = new StringBuilder();  
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component.isEnabled()) {
					String sCatch="", sThen = "";
					if (component instanceof UIDynamicAction) {
						sThen = ((UIDynamicAction)component).computeActionContent();
					}
					if (component instanceof UICustomAction) {
						sThen = ((UICustomAction)component).computeActionContent();
					}
					if (component instanceof UIActionFailureEvent) {
						sCatch = ((UIActionFailureEvent)component).computeEvent();
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
			tsCode += "\t\tlet self: any = stack[\""+ beanName +"\"] = {};"+ System.lineSeparator();
			tsCode += "\t\tself.in = "+ inputs +";"+ System.lineSeparator();
			
			if (getSharedAction() != null) {
				tsCode +="\t\treturn this.actionBeans."+ actionName +
						"(this, {...{stack: stack, parent: parent, out: out}, ...self.in.props}, "+ 
									"{...stack[\"root\"].in, ...params, ...self.in.vars}, event)"+ 
										System.lineSeparator();
			} else {
				tsCode +="\t\treturn this."+ actionName +
						"(this, {...{stack: stack, parent: parent, out: out}, ...self.in.props}, "+ 
									"{...stack[\"root\"].in, ...self.in.vars}, event)"+ 
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
		return "";
	}
	
	protected String computeActionMain() {
		String computed = "";
		if (isEnabled()) {
			StringBuilder cartridge = new StringBuilder();
			cartridge.append("\t/**").append(System.lineSeparator())
						.append("\t * Function "+ getName()).append(System.lineSeparator());
			for (String commentLine : getComment().split(System.lineSeparator())) {
				cartridge.append("\t *   ").append(commentLine).append(System.lineSeparator());
			}
			cartridge.append("\t * ").append(System.lineSeparator());
			
			String cafPageType = compareToTplVersion("7.5.2.0") >= 0 ? "C8oPageBase":"C8oPage";
			
			StringBuilder parameters = new StringBuilder();
			parameters.append("page: "+ cafPageType +", props, vars, event: any");
			cartridge.append("\t * @param page  , the current page").append(System.lineSeparator());
			cartridge.append("\t * @param props , the object which holds properties key-value pairs").append(System.lineSeparator());
			cartridge.append("\t * @param vars  , the object which holds variables key-value pairs").append(System.lineSeparator());
			cartridge.append("\t * @param event , the current event object").append(System.lineSeparator());
			cartridge.append("\t */").append(System.lineSeparator());
			
			String actionName = getActionName();
			
			computed += System.lineSeparator();
			computed += cartridge;
			computed += "\t"+ actionName + "("+ parameters +") : Promise<any> {" + System.lineSeparator();
			computed += "\t\treturn new Promise((resolve, reject) => {"+ System.lineSeparator();
			computed += "\t\t/*Begin_c8o_function:"+ actionName +"*/" + System.lineSeparator();
			computed += actionValue.getString();
			computed += "\t\t/*End_c8o_function:"+ actionName +"*/" + System.lineSeparator();
			computed += "\t\t});"+ System.lineSeparator();
			computed += "\t}" + System.lineSeparator();
		}
		return computed;
	}
	
	public Contributor getActionContributor() {
		return getContributor();
	}
	
	protected Contributor getContributor() {
		return new Contributor() {
			
			@Override
			public boolean isNgModuleForApp() {
				if (!getModuleNgImports().isEmpty()) {
					return true;
				}
				return false;
			}
			
			@Override
			public Map<String, String> getActionTsFunctions() {
				Map<String, String> functions = new HashMap<String, String>();
				if (getSharedAction() != null || getSharedComponent() != null) {
					String actionName = getActionName();
					String actionCode = computeActionMain();
					if (compareToTplVersion("7.5.2.0") < 0 ) {
						actionCode = actionCode.replaceFirst("C8oPageBase", "C8oPage");
						actionCode = actionCode.replaceAll("C8oCafUtils\\.merge", "page.merge");
					}
					functions.put(actionName, actionCode);
				}
				return functions;
			}

			@Override
			public Map<String, String> getActionTsImports() {
				Map<String, String> imports = new HashMap<String, String>();
				if (getSharedAction() != null  || getSharedComponent() != null) {
					for (XMLVector<String> v : page_ts_imports) {
						imports.put(v.get(0).trim(), v.get(1).trim());
					}
				}
				return imports;
			}

			@Override
			public Map<String, File> getCompBeanDir() {
				return new HashMap<String, File>();
			}

			@Override
			public Map<String, String> getModuleTsImports() {
				Map<String, String> imports = new HashMap<String, String>();
				for (XMLVector<String> v : module_ts_imports) {
					String name = v.get(0).trim();
					String path = v.get(1).trim();
					if (!name.isEmpty() && !path.isEmpty()) {
						if (!imports.containsKey(name)) {
							imports.put(name, path);
						}
					}
				}
				return imports;
			}

			@Override
			public Set<String> getModuleNgImports() {
				Set<String> modules = new HashSet<String>();
				for (XMLVector<String> v : module_ng_imports) {
					String module = v.get(0).trim();
					if (!module.isEmpty()) {
						if (!modules.contains(module)) {
							modules.add(module);
						}
					}
				}
				return modules;
			}

			@Override
			public Set<String> getModuleNgProviders() {
				Set<String> providers = new HashSet<String>();
				for (XMLVector<String> v : module_ng_providers) {
					String provider = v.get(0).trim();
					if (!provider.isEmpty()) {
						if (!providers.contains(provider)) {
							providers.add(provider);
						}
					}
				}
				return providers;
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
				Map<String, String> dependencies = new HashMap<String, String>();
				for (XMLVector<String> v : package_dependencies) {
					String pckg = v.get(0).trim();
					String version = v.get(1).trim();
					if (!pckg.isEmpty() && !version.isEmpty()) {
						if (!dependencies.containsKey(pckg)) {
							dependencies.put(pckg, version);
						}
					}
				}
				return dependencies;
			}

			@Override
			public Map<String, String> getConfigPlugins() {
				Map<String, String> plugins = new HashMap<String, String>();
				for (XMLVector<String> v : cordova_plugins) {
					String plugin = v.get(0).trim();
					String version = v.get(1).trim();
					String variables = v.size() > 2 ? v.get(2).trim():"{}";
					if (!plugin.isEmpty() && !version.isEmpty()) {
						if (!plugins.containsKey(plugin)) {
							JSONObject json = new JSONObject();
							try {
								json.put("plugin", version);
								json.put("version", version);
								json.put("variables", new JSONObject(variables));
								plugins.put(plugin, json.toString());
							} catch (Exception e) {}
						}
					}
				}
				return plugins;
			}
			
		};
	}	
}
