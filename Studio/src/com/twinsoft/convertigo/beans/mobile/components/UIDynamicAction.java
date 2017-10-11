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

	public String getActionName() {
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
			StringBuilder parameters = new StringBuilder();
			parameters.append("$event");
			
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof UIControlVariable) {
					UIControlVariable variable = (UIControlVariable)component;
					String parameterValue = variable.getVarValue();
					parameters.append(parameters.length()> 0 ? ", ":"").append(parameterValue);
				}
			}
			
			String actionName = getActionName();
			String computed = actionName + "("+ parameters +")";
			return computed;
		}
		return "";
	}

	@Override
	public void computeScripts(JSONObject jsonScripts) {
		String function = computeActionFunction();
		try {
			String functions = jsonScripts.getString("functions") + function;
			jsonScripts.put("functions", functions);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public String computeActionFunction() {
		String computed = "";
		if (isEnabled()) {
			StringBuilder cartridge = new StringBuilder();
			cartridge.append("\t/**").append(System.lineSeparator())
						.append("\t * Function "+ getName()).append(System.lineSeparator());
			for (String commentLine : getComment().split(System.lineSeparator())) {
				cartridge.append("\t *   ").append(commentLine).append(System.lineSeparator());
			}
			cartridge.append("\t * ").append(System.lineSeparator());
			
			StringBuilder parameters = new StringBuilder();
			parameters.append("event");
			cartridge.append("\t * @param event , the event received").append(System.lineSeparator());
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof UIControlVariable) {
					UIControlVariable variable = (UIControlVariable)component;
					String paramName = variable.getVarName();
					String paramComment = variable.getComment();
					parameters.append(parameters.length()> 0 ? ", ":"").append(paramName);
					cartridge.append("\t * @param "+paramName);
					boolean firstLine = true;
					for (String commentLine : paramComment.split(System.lineSeparator())) {
						cartridge.append(firstLine ? " , ":"\t *   ").append(commentLine).append(System.lineSeparator());
						firstLine = false;
					}
				}
			}
			cartridge.append("\t */").append(System.lineSeparator());
			
			String actionName = getActionName();
			
			computed += System.lineSeparator();
			computed += cartridge;
			computed += "\t"+ actionName + "("+ parameters +") {" + System.lineSeparator();
			computed += "\t"+ computeActionContent() + System.lineSeparator();
			computed += "\t\t.catch((error:any) => {console.log('[MB] An error occured : ',error.message)});" + System.lineSeparator();
			computed += "\t}";
		}
		return computed;
	}
	
	protected String computeActionContent() {
		IonBean ionBean = getIonBean();
		if (ionBean != null) {
			String tsCode = ComponentManager.getActionTsCode(ionBean.getName());
			for (IonProperty property : ionBean.getProperties().values()) {
				String p_name = property.getName();
				Object p_value = property.getValue();
				
				// case value is set
				if (!p_value.equals(false)) {
					MobileSmartSourceType msst = property.getSmartType();
					String smartValue = msst.getValue();
					if (Mode.PLAIN.equals(msst.getMode()) && property.getType().equalsIgnoreCase("string")) {
						smartValue = "\"" + smartValue + "\"";
					}
					String regex = "\\$"+p_name+"\\$";
					tsCode = tsCode.replaceAll(regex, smartValue);
				} else {
					String regex = "\\$"+p_name+"\\$";
					tsCode = tsCode.replaceAll(regex, "null");
				}
			}
			
			int numThen = numberOfActions();
			
			StringBuilder sbVars = new StringBuilder(); 
			StringBuilder sbThen = new StringBuilder();  
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component.isEnabled()) {
					if (component instanceof UIDynamicAction) {
						String s = ((UIDynamicAction)component).computeActionContent();
						if (!s.isEmpty()) {
							sbThen.append(sbThen.length()>0 && numThen > 1 ? ","+ System.lineSeparator() :"")
								.append(s);//.append(System.lineSeparator());
						}
					} else if (component instanceof UIControlVariable) {
						String parameter = component.computeTemplate();
						if (!parameter.isEmpty()) {
							sbVars.append(sbVars.length() > 0 ? ", ":"").append(parameter);
						}
					}
				}
			}
			
			String s = "";
			if (!tsCode.isEmpty()) {
				tsCode = tsCode.replaceFirst("/\\*\\=c8o_Vars\\*/", sbVars.toString());
				if (sbThen.length() > 0) {
					if (numThen > 1) {
						tsCode = tsCode.replaceFirst("/\\*\\=c8o_Then\\*/", "return Promise.all(["+sbThen.toString()+"])");
					} else {
						tsCode = tsCode.replaceFirst("/\\*\\=c8o_Then\\*/", "return "+ sbThen.toString());
					}
				} else {
					tsCode = tsCode.replaceFirst("/\\*\\=c8o_Then\\*/","");
				}
				s = tsCode;
			}
			return s;
		}
		return "";
	}
}
