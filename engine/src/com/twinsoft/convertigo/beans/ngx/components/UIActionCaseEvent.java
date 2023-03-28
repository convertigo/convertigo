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

public class UIActionCaseEvent extends UIActionEvent {

	private static final long serialVersionUID = -3164849653359291496L;

	public UIActionCaseEvent() {
		super();
	}
	
	@Override
	public UIActionCaseEvent clone() throws CloneNotSupportedException {
		UIActionCaseEvent cloned = (UIActionCaseEvent) super.clone();
		return cloned;
	}
	
	protected boolean isDefault = false;
	
	public boolean isDefaultCase() {
		return isDefault;
	}
	
	protected String caseValue = "'some value'";
	
	public String getCaseValue() {
		return caseValue;
	}

	public void setCaseValue(String caseValue) {
		this.caseValue = caseValue;
	}

	protected boolean passThrough = false;
	
	public boolean isPassThrough() {
		return passThrough;
	}

	public void setPassThrough(boolean passThrough) {
		this.passThrough = passThrough;
	}

	public String getCaseFn() {
		return "doCase"+ priority;
	}
	
	@Override
	public String toString() {
		return "case: "+ caseValue;
	}

	@Override
	public String computeEvent() {
		if (isEnabled()) {
			String caseFn = getCaseFn();
			String caseIn = super.computeEvent();
			
			String tsCode = "";
			tsCode += "\t\tconst "+caseFn+" = (c8oPage : C8oPageBase) : Promise<any> => {" + System.lineSeparator();
			tsCode += computeInnerGet("c8oPage", "doCase");
			tsCode += caseIn.isEmpty() ? "\t\t\treturn Promise.resolve()"+ System.lineSeparator() : caseIn;
			tsCode += "\t\t}" + System.lineSeparator();
			return tsCode;
		}
		return "";
	}
}
