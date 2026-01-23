/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.util.EnumUtils;

public class UIAppGuard extends UIEventSubscriber implements ITagsProperty {

	private static final long serialVersionUID = 7348235843036873987L;

	public enum AppGuardType {
		onCanActivate("canActivate"),
		onCanDeactivate("canDeactivate")
		;
		
		String function;
		AppGuardType(String function) {
			this.function = function;
		}
		
		String getFunctionName() {
			return this.function;
		}
		
		String getTopic() {
			return this.function;
		}
	}
	
	public UIAppGuard() {
		super();
		this.topic = this.guardType.getTopic();
	}

	@Override
	public UIAppGuard clone() throws CloneNotSupportedException {
		UIAppGuard cloned = (UIAppGuard)super.clone();
		return cloned;
	}
	
	@Override
	protected String getRequiredTplVersion() {
		return "7.9.0.6";
	}
	
	private AppGuardType guardType = AppGuardType.onCanActivate;
	
	public AppGuardType getGuardType() {
		return guardType;
	}

	public void setGuardType(AppGuardType guardType) {
		this.guardType = guardType;
	}
	
	@Override
	public String getTopic() {
		return this.guardType.getTopic();
	}
	
	@Override
	public String getFunctionName() {
		return this.guardType.getFunctionName();
	}
	
	@Override
	public String toString() {
		return this.guardType.name();
	}
	
	@Override
	protected String computeListenerFunction() {
		String computed = "";
		if (isEnabled()) {
			String functionName = getFunctionName();
			
			StringBuilder sb = new StringBuilder();
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof IAction) {
					if (component.isEnabled()) {
						sb.append("\t\tthis.").append(((IAction)component).getFunctionName())
							.append("({root: {scope:{}, in:{}, out:data}})")
								.append(",").append(System.lineSeparator());
					}
				}
			}
			
			StringBuilder cartridge = new StringBuilder();
			cartridge.append("\t/**").append(System.lineSeparator())
						.append("\t * Function "+ functionName).append(System.lineSeparator());
			for (String commentLine : getComment().split(System.lineSeparator())) {
				cartridge.append("\t *   ").append(commentLine).append(System.lineSeparator());
			}
			cartridge.append("\t * ").append(System.lineSeparator());
			cartridge.append("\t * @param data , the event data object").append(System.lineSeparator());
			cartridge.append("\t */").append(System.lineSeparator());
			
			computed += System.lineSeparator();
			computed += cartridge;
			computed += "\t"+ functionName + "(data) {" + System.lineSeparator();
			computed += "\t\tthis.c8o.log.debug(\"[MB] "+functionName+": '"+topic+"' received\");" + System.lineSeparator();
			computed += "\t\treturn new Promise((resolve, reject)=>{" + System.lineSeparator();
			computed += "\t\t\tthis.getInstance(Platform).ready().then(()=>{" + System.lineSeparator();
			computed += "\t\t\t\tPromise.all([" + System.lineSeparator();
			computed += sb.toString();
			computed += "\t\t\t\t])" + System.lineSeparator();
			computed += "\t\t\t\t.then((resp)=>{" + System.lineSeparator();
			computed += "\t\t\t\t\tlet ret = resp.find((item) => {return item === false;});" + System.lineSeparator();
			computed += "\t\t\t\t\tresolve(ret === false ? false : true);" + System.lineSeparator();
			computed += "\t\t\t\t});" + System.lineSeparator();
			computed += "\t\t\t});" + System.lineSeparator();
			computed += "\t\t});" + System.lineSeparator();
			computed += "\t}";
		}
		return computed;
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("guardType")) {
			return EnumUtils.toNames(AppGuardType.class);
		}
		return new String[0];
	}
}
