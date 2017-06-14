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
	
	/*
	 * The event value
	 */
	private String actionValue = "";
	
	public String getActionValue() {
		return actionValue;
	}

	public void setActionValue(String actionValue) {
		this.actionValue = actionValue;
	}

	@Override
	public String computeTemplate() {
		if (isEnabled()) {
			StringBuilder parameters = new StringBuilder();
			
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof UIControlVariable) {
					UIControlVariable variable = (UIControlVariable)component;
					String parameterValue = variable.getVarValue();
					parameters.append(parameters.length()> 0 ? ", ":"").append(parameterValue);
				}
			}
			
			String computed = "CTS"+ this.priority + "($event, "+ parameters +")";
			return computed;
		}
		return "";
	}

	@Override
	public String computeScriptContent() {
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
			computed += getScriptContent();
			computed += System.lineSeparator() + "\t}";
		}
		return computed;
	}
	
	private String getScriptContent() {
		String s = "";
		s += "\t/*Begin_c8o_CTS"+ this.priority +"*/" + System.lineSeparator();
		s += this.actionValue + System.lineSeparator();
		s += "\t/*End_c8o_CTS"+ this.priority +"*/";
		return s;
	}
	
}
