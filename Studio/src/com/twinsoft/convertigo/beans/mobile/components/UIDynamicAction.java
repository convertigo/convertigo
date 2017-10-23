/*
 * Copyright (c) 2001-2016 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.mobile.components;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonProperty;

public class UIDynamicAction extends UIDynamicElement implements IAction {

	private static final long serialVersionUID = 5988583131428053374L;

	public UIDynamicAction() {
		super();
	}

	public UIDynamicAction(String tagName) {
		super(tagName);
	}

	@Override
	public UIDynamicAction clone() throws CloneNotSupportedException {
		UIDynamicAction cloned = (UIDynamicAction) super.clone();
		return cloned;
	}

	@Override
	protected StringBuilder initAttributes() {
		return new StringBuilder();
	}

	public String getInputId() {
		return "_"+ this.priority;
	}
	
	public String getFunctionName() {
		return "ATS"+ this.priority;
	}

	protected int numberOfActions() {
		int num = 0;
		Iterator<UIComponent> it = getUIComponentList().iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (component instanceof UIDynamicAction) {
				if (component.isEnabled()) {
					num++;
				}
			}
		}
		return num;
	}
	
	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			String inputs = computeActionInputs();
			
			if (numberOfActions() > 0 || getParent() instanceof UIPageEvent) {
				return getFunctionName() + "($event, {"+ inputs +"})";
			} else {
				IonBean ionBean = getIonBean();
				if (ionBean != null) {
					String actionName = ionBean.getName();
					int i = inputs.indexOf("props:")+"props:".length();
					int j = inputs.indexOf("vars:")+"vars:".length();
					String props = inputs.substring(i, inputs.indexOf('}',i)+1);
					String vars = inputs.substring(j, inputs.indexOf('}',j)+1);
					return actionName + "("+ props + ","+ vars +")";
				}
			}
		}
		return "";
	}

	protected String computeActionInputs() {
		if (isEnabled()) {
			IonBean ionBean = getIonBean();
			if (ionBean != null) {
				StringBuilder sbProps = new StringBuilder();
				for (IonProperty property : ionBean.getProperties().values()) {
					String p_name = property.getName();
					Object p_value = property.getValue();
					
					sbProps.append(sbProps.length() > 0 ? ", ":"");
					sbProps.append(p_name).append(": ");
					// case value is set
					if (!p_value.equals(false)) {
						MobileSmartSourceType msst = property.getSmartType();
						String smartValue = msst.getValue();
						if (Mode.PLAIN.equals(msst.getMode()) && property.getType().equalsIgnoreCase("string")) {
							smartValue = "\'" + smartValue + "\'";
						}
						sbProps.append(smartValue);
					}
					// case value is not set
					else {
						sbProps.append("null");
					}
				}
				
				StringBuilder sbVars = new StringBuilder();
				StringBuilder sbActions = new StringBuilder();
				Iterator<UIComponent> it = getUIComponentList().iterator();
				while (it.hasNext()) {
					UIComponent component = (UIComponent)it.next();
					if (component instanceof UIControlVariable) {
						String parameter = component.computeTemplate();
						if (!parameter.isEmpty()) {
							sbVars.append(sbVars.length() > 0 ? ", ":"").append(parameter);
						}
					} else if (component instanceof UIDynamicAction) {
						String s = ((UIDynamicAction)component).computeActionInputs();
						if (!s.isEmpty()) {
							sbActions.append(", ").append(s);
						}
					}
				}
				
				return getInputId() +": {props:{"+sbProps+"}, vars:{"+sbVars+"}}" + sbActions;
			}
		}
		return "";
	}
	
	@Override
	public void computeScripts(JSONObject jsonScripts) {
		String function = computeActionFunction(jsonScripts);
		try {
			String functions = jsonScripts.getString("functions") + System.lineSeparator() + function;
			jsonScripts.put("functions", functions);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	protected String computeActionFunction(JSONObject jsonScripts) {
		String computed = "";
		if (isEnabled()) {
			StringBuilder parameters = new StringBuilder();
			parameters.append("event").append(", actions");
			
			StringBuilder cartridge = new StringBuilder();
			cartridge.append("\t/**").append(System.lineSeparator())
						.append("\t * Function "+ getFunctionName()).append(System.lineSeparator());
			for (String commentLine : getComment().split(System.lineSeparator())) {
				cartridge.append("\t *   ").append(commentLine).append(System.lineSeparator());
			}
			cartridge.append("\t * ").append(System.lineSeparator());
			cartridge.append("\t * @param event , the event received").append(System.lineSeparator());
			cartridge.append("\t * @param actions , the object which holds action inputs").append(System.lineSeparator());
			cartridge.append("\t */").append(System.lineSeparator());
			
			String functionName = getFunctionName();
			
			computed += System.lineSeparator();
			computed += cartridge;
			computed += "\t"+ functionName + "("+ parameters +") {" + System.lineSeparator();
			computed += ""+ computeActionContent(jsonScripts);
			computed += "\t\t.catch((error:any) => {console.log(\"[MB] An error occured : \",error.message)});" + System.lineSeparator();
			computed += "\t}";
		}
		return computed;
	}
	
	protected String computeActionContent(JSONObject jsonScripts) {
		IonBean ionBean = getIonBean();
		if (ionBean != null) {
			int numThen = numberOfActions();
			String actionName = ionBean.getName();
			String actionInputId = getInputId();
			String actionCode = ComponentManager.getActionTsCode(actionName);
			
			PageComponent page = getPage();
			
			try {
				
				String imports = jsonScripts.getString("imports");
				Map<String, List<String>> map = ionBean.getConfig().getPageImports();
				if (map.size() > 0) {
					for (String from : map.keySet()) {
						for (String component: map.get(from)) {
							if (page.addImport(component, from)) {
								imports += "import { "+ component +" } from '"+ from +"';" + System.lineSeparator();
							}
						}
					}
					jsonScripts.put("imports", imports);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			if (page.addFunction(actionName, actionCode)) {
				try {
					String functions = jsonScripts.getString("functions") + System.lineSeparator() + actionCode;
					jsonScripts.put("functions", functions);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			StringBuilder sbThen = new StringBuilder();  
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component.isEnabled()) {
					if (component instanceof UIDynamicAction) {
						String s = ((UIDynamicAction)component).computeActionContent(jsonScripts);
						if (!s.isEmpty()) {
							sbThen.append(sbThen.length()>0 && numThen > 1 ? "\t\t,"+ System.lineSeparator() :"").append(s);
						}
					}
				}
			}

			String tsCode = "";
			tsCode +="\t\tthis."+actionName+"(actions."+actionInputId+".props, actions."+actionInputId+".vars)"+ System.lineSeparator();
			tsCode += "\t\t.then((res:any) => {"+ System.lineSeparator();
			if (sbThen.length() > 0) {
				if (numThen > 1) {
					tsCode += "\t\treturn Promise.all(["+ System.lineSeparator();
					tsCode += sbThen.toString();
					tsCode += "\t\t])"+ System.lineSeparator();
				} else {
					tsCode += "\t\treturn "+ sbThen.toString().replaceFirst("\t\t", "");
				}
			} else {
				tsCode += "";
			}
			tsCode += "\t\t}, (error: any) => {console.log(\"[MB] "+actionName+" : \", error.message);throw new Error(error);})"+ System.lineSeparator();
			
			return tsCode;
		}
		return "";
	}
}
