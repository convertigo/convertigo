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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class UIControlCustomAction extends UIControlAction {
	
	private static final long serialVersionUID = 1629185375344957613L;

	public UIControlCustomAction() {
		super();
	}
	
	@Override
	public UIControlCustomAction clone() throws CloneNotSupportedException {
		UIControlCustomAction cloned = (UIControlCustomAction)super.clone();
		return cloned;
	}
	
	@Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);
		
		try {
			NodeList properties = element.getElementsByTagName("property");
			
			// migration of scriptContent from String to FormatedContent
			Element propElement = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "actionValue");
			if (propElement != null) {
				Element valueElement = (Element) XMLUtils.findChildNode(propElement, Node.ELEMENT_NODE);
				if (valueElement != null) {
					Document document = valueElement.getOwnerDocument();
					Object content = XMLUtils.readObjectFromXml(valueElement);
					if (content instanceof String) {
						FormatedContent formated = new FormatedContent((String) content);
						Element newValueElement = (Element)XMLUtils.writeObjectToXml(document, formated);
						propElement.replaceChild(newValueElement, valueElement);
						hasChanged = true;
						Engine.logBeans.warn("(UIControlCustomAction) 'actionValue' has been updated for the object \"" + getName() + "\"");
					}
				}
			}
		}
        catch(Exception e) {
            throw new EngineException("Unable to preconfigure the ui component \"" + getName() + "\".", e);
        }
	}
	
	/*
	 * The action value
	 */
	private FormatedContent actionValue = new FormatedContent("");
	
	public FormatedContent getActionValue() {
		return actionValue;
	}

	public void setActionValue(FormatedContent actionValue) {
		this.actionValue = actionValue;
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
			
			String computed = "CTS"+ this.priority + "("+ parameters +")";
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
			
			computed += System.lineSeparator();
			computed += cartridge;
			computed += "\tCTS"+ this.priority + "("+ parameters +") {" + System.lineSeparator();
			computed += computeActionContent();
			computed += System.lineSeparator() + "\t}";
		}
		return computed;
	}
	
	private String computeActionContent() {
		String s = "";
		s += "\t/*Begin_c8o_function:CTS"+ this.priority +"*/" + System.lineSeparator();
		s += actionValue.getString() + System.lineSeparator();
		s += "\t/*End_c8o_function:CTS"+ this.priority +"*/";
		return s;
	}
	
}
