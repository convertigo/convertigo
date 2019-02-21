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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.beans.mobile.components.UIControlDirective.AttrDirective;
import com.twinsoft.convertigo.beans.mobile.components.UIControlEvent.AttrEvent;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonProperty;
import com.twinsoft.convertigo.engine.EngineException;

public class UIDynamicAction extends UIDynamicElement implements IAction {

	private static final long serialVersionUID = 5988583131428053374L;

	private transient UIActionFailureEvent failureEvent = null;
	
	public UIDynamicAction() {
		super();
	}

	public UIDynamicAction(String tagName) {
		super(tagName);
	}

	@Override
	public UIDynamicAction clone() throws CloneNotSupportedException {
		UIDynamicAction cloned = (UIDynamicAction) super.clone();
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
	protected StringBuilder initAttributes() {
		return new StringBuilder();
	}

	/*public String getInputId() {
		return "_"+ this.priority;
	}*/
	
	@Override
	public String getFunctionName() {
		return "ATS"+ this.priority;
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
				getParent() instanceof UIPageEvent || getParent() instanceof UIEventSubscriber;
	}
	
	protected String getScope() {
		String scope = "";
		
		DatabaseObject parent = getParent();
		while (parent != null && !(parent instanceof UIPageEvent) && !(parent instanceof UIEventSubscriber)) {
			if (parent instanceof UIControlDirective) {
				UIControlDirective uicd = (UIControlDirective)parent;
				if (AttrDirective.ForEach.equals(AttrDirective.getDirective(uicd.getDirectiveName()))) {
					scope += !scope.isEmpty() ? ", ":"";
					scope += "item"+uicd.priority + ": "+ "item"+uicd.priority;
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
			
			//String cafMerge = compareToTplVersion("7.5.2.0") >= 0 ? "C8oCafUtils.merge":"merge";
			
			if (isStacked()) {
				String scope = getScope();
				//String in = formGroupName == null ? "{}": cafMerge + "("+formGroupName +".value, {})";
				String in = formGroupName == null ? "{}": "merge("+formGroupName +".value, {})";
				return getFunctionName() + "({root: {scope:{"+scope+"}, in:"+ in +", out:$event}})";
			} else {
				IonBean ionBean = getIonBean();
				if (ionBean != null) {
					String actionName = ionBean.getName();
					
					String props = "{}", vars = "{}";
					String inputs = computeActionInputs(true);
					Pattern pattern = Pattern.compile("\\{props:(\\{.*\\}), vars:(\\{.*\\})\\}");
					Matcher matcher = pattern.matcher(inputs);
					if (matcher.matches()) {
						props = matcher.group(1);
						vars = matcher.group(2);
					}
					
					if (formGroupName != null) {
						//vars = cafMerge + "("+formGroupName +".value, "+ vars +")";
						vars = "merge("+formGroupName +".value, "+ vars +")";
					}
					
					if (compareToTplVersion("1.0.91") >= 0) {
						return "resolveError(actionBeans."+ actionName + "(this,"+ props + ","+ vars +"))";
					} else {
						return "actionBeans."+ actionName + "(this,"+ props + ","+ vars +")";
					}
				}
			}
		}
		return "";
	}

	protected StringBuilder initProps(boolean forTemplate) {
		return new StringBuilder();
	}
	
	protected String computeActionInputs(boolean forTemplate) {
		if (isEnabled()) {
			IonBean ionBean = getIonBean();
			if (ionBean != null) {
				StringBuilder sbProps = initProps(forTemplate);
				for (IonProperty property : ionBean.getProperties().values()) {
					String p_name = property.getName();
					Object p_value = property.getValue();
					
					sbProps.append(sbProps.length() > 0 ? ", ":"");
					sbProps.append(p_name).append(": ");
					// case value is set
					if (!p_value.equals(false)) {
						MobileSmartSourceType msst = property.getSmartType();
						String smartValue = msst.getValue();
						
						if (Mode.PLAIN.equals(msst.getMode())) {
							if (property.getType().equalsIgnoreCase("string")) {
								smartValue = forTemplate ?
										"\'" + MobileSmartSourceType.escapeStringForTpl(smartValue) + "\'":
											"\'" + MobileSmartSourceType.escapeStringForTs(smartValue) + "\'";
							}
						}
						
						if (forTemplate) {
							smartValue = ""+smartValue;
						} else {
							if (Mode.SOURCE.equals(msst.getMode())) {
								MobileSmartSource mss = msst.getSmartSource();
								if (mss.getFilter().equals(MobileSmartSource.Filter.Iteration)) {
									smartValue = "scope."+ smartValue;
								}
								else {
									smartValue = "this."+ smartValue;
								}
							}
							smartValue = smartValue.replaceAll("\\?\\.", ".");
							smartValue = smartValue.replaceAll("this\\.", "c8oPage.");
							smartValue = "get(`"+smartValue+"`)";
						}
						
						sbProps.append(smartValue);
					}
					// case value is not set
					else {
						sbProps.append("null");
					}
				}
				
				StringBuilder sbVars = new StringBuilder();
				Iterator<UIComponent> it = getUIComponentList().iterator();
				while (it.hasNext()) {
					UIComponent component = (UIComponent)it.next();
					if (component instanceof UIControlVariable) {
						UIControlVariable uicv = (UIControlVariable)component;
						if (uicv.isEnabled()) {
							sbVars.append(sbVars.length() > 0 ? ", ":"");
							sbVars.append(uicv.getVarName()).append(": ");
							
							if (forTemplate) {
								sbVars.append(uicv.getVarValue());
							} else {
								MobileSmartSourceType msst = uicv.getVarSmartType();
								
								String smartValue = msst.getValue();
								if (Mode.PLAIN.equals(msst.getMode())) {
									smartValue = "\'" + MobileSmartSourceType.escapeStringForTs(smartValue) + "\'";
								}
								
								if (Mode.SOURCE.equals(msst.getMode())) {
									MobileSmartSource mss = msst.getSmartSource();
									if (mss.getFilter().equals(MobileSmartSource.Filter.Iteration)) {
										smartValue = "scope."+ smartValue;
									}
									else {
										smartValue = "this."+ smartValue;
									}
								}
								smartValue = smartValue.replaceAll("\\?\\.", ".");
								smartValue = smartValue.replaceAll("this\\.", "c8oPage.");
								smartValue = "get(`"+smartValue+"`)";
								
								sbVars.append(smartValue);
							}
						}
					}
				}
				return "{props:{"+sbProps+"}, vars:{"+sbVars+"}}";
			}
		}
		return "";
	}
	
	@Override
	public void computeScripts(JSONObject jsonScripts) {
		try {
			IScriptComponent main = getMainScriptComponent();
			
			String imports = jsonScripts.getString("imports");
			
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
				String function = computeActionFunction();
				
				String functions = jsonScripts.getString("functions") + System.lineSeparator() + function;
				jsonScripts.put("functions", functions);
			} catch (JSONException e) {
				e.printStackTrace();
			}
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
			computed += "\t\tlet self;" + System.lineSeparator();
			computed += "\t\tlet out;" + System.lineSeparator();
			computed += "\t\tlet event;" + System.lineSeparator();
			computed += "\t\t" + System.lineSeparator();
			computed += "\t\tlet get = function(key) {let val=undefined;try {val=eval(ts.transpile('(' + key + ')') );}catch(e){c8oPage.c8o.log.warn(\"[MB] "+functionName+": \"+e.message)}return val;}" + System.lineSeparator();
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
			IonBean ionBean = getIonBean();
			if (ionBean != null) {
				int numThen = numberOfActions();
				String actionName = ionBean.getName();
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
	
				String cafMerge = compareToTplVersion("7.5.2.0") >= 0 ? "C8oCafUtils.merge":"this.merge";
				
				String tsCode = "";
				tsCode += "\t\tnew Promise((resolve, reject) => {"+ System.lineSeparator();
				tsCode += "\t\tself = stack[\""+ getName() +"\"] = {};"+ System.lineSeparator();
				tsCode += "\t\tself.in = "+ inputs +";"+ System.lineSeparator();
				
				tsCode +="\t\treturn this.actionBeans."+actionName+"(this, self.in.props, "+ cafMerge +"(self.in.vars, stack[\"root\"].in))"+ System.lineSeparator();
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

	public UIControlVariable getVariable(String variableName) {
		Iterator<UIComponent> it = getUIComponentList().iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (component instanceof UIControlVariable) {
				UIControlVariable variable = (UIControlVariable)component;
				if (variable.getName().equals(variableName)) {
					return variable;
				}
			}
		}
		return null;
	}
	
	@Override
	protected Contributor getContributor() {
		Contributor contributor = super.getContributor();
		return new Contributor() {
			@Override
			public Map<String, String> getActionTsFunctions() {
				Map<String, String> functions = new HashMap<String, String>();
				IonBean ionBean = getIonBean();
				if (ionBean != null) {
					String actionName = ionBean.getName();
					
					String actionCode = ComponentManager.getActionTsCode(actionName);
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
				IonBean ionBean = getIonBean();
				if (ionBean != null) {
					Map<String, List<String>> map = ionBean.getConfig().getActionTsImports();
					if (map.size() > 0) {
						for (String from : map.keySet()) {
							for (String component: map.get(from)) {
								imports.put(component.trim(), from);
							}
						}
					}
				}
				return imports;
			}

			@Override
			public Map<String, File> getCompBeanDir() {
				return contributor.getCompBeanDir();
			}

			@Override
			public Map<String, String> getModuleTsImports() {
				return contributor.getModuleTsImports();
			}

			@Override
			public Set<String> getModuleNgImports() {
				return contributor.getModuleNgImports();
			}

			@Override
			public Set<String> getModuleNgProviders() {
				return contributor.getModuleNgProviders();
			}

			@Override
			public Set<String> getModuleNgDeclarations() {
				return contributor.getModuleNgDeclarations();
			}
			
			@Override
			public Set<String> getModuleNgComponents() {
				return contributor.getModuleNgComponents();
			}
			
			@Override
			public Map<String, String> getPackageDependencies() {
				return contributor.getPackageDependencies();
			}

			@Override
			public Map<String, String> getConfigPlugins() {
				return contributor.getConfigPlugins();
			}
			
		};
	}

	@Override
	protected void addInfos(Map<String, Set<String>> infoMap) {
		IonBean ionBean = getIonBean();
		if (ionBean != null) {
			String beanName = ionBean.getName(); 
			if (ionBean.hasProperty("marker")) {
				JSONObject json = new JSONObject();
				String key = null;
				
				for (IonProperty property : ionBean.getProperties().values()) {
					MobileSmartSourceType msst = property.getSmartType();
					String p_name = property.getName();
					Object p_value = property.getValue();
					
					if (!p_value.equals(false)) {
						if (beanName.equals("FullSyncViewAction")) {
							if (p_name.equals("fsview")) {
								key = p_value.toString() + ".view";
							}
						} else if (beanName.equals("FullSyncGetAction")) {
							if (p_name.equals("requestable")) {
								key = p_value.toString() + ".get";
							}
						} else if (beanName.equals("CallSequenceAction")) {
							if (p_name.equals("requestable")) {
								key = p_value.toString();
							}
						} else if (beanName.equals("CallFullSyncAction")) {
							if (p_name.equals("requestable")) {
								key = p_value.toString();
								Object p_verb = ionBean.getProperty("verb").getValue();
								if (!p_verb.equals(false)) {
									key += "."+ p_verb.toString();
								}
							}
						}
					}
					
					try {
						if (p_name.equals("marker")) {
							json.put(p_name, !p_value.equals(false) ? msst.getValue():"");
						}
						if (p_name.equals("include_docs")) {
							json.put(p_name, !p_value.equals(false) ? msst.getValue():"false");
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				
				if (key != null && !key.isEmpty()) {
					Set<String> infos = infoMap.get(key);
					if (infos == null) {
						infos = new HashSet<String>();
					}
					String info = json.toString();
					if (!info.isEmpty()) {
						infos.add(info);
					}
					infoMap.put(key, infos);
				}
			}
		}
		
		super.addInfos(infoMap);
	}	
}
