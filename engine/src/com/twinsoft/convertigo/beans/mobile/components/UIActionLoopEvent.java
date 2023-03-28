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

package com.twinsoft.convertigo.beans.mobile.components;

public class UIActionLoopEvent extends UIActionEvent {

	private static final long serialVersionUID = 5156967743573867701L;

	public UIActionLoopEvent() {
		super();
	}
	
	@Override
	public UIActionLoopEvent clone() throws CloneNotSupportedException {
		UIActionLoopEvent cloned = (UIActionLoopEvent) super.clone();
		return cloned;
	}
	
	@Override
	public String toString() {
		return "onNextItem";
	}

	private String varItemName = "item";
	
	public String getVarItemName() {
		return varItemName;
	}

	public void setVarItemName(String varItemName) {
		this.varItemName = varItemName;
	}

	private String varIndexName = "index";
	
	public String getVarIndexName() {
		return varIndexName;
	}

	public void setVarIndexName(String varIndexName) {
		this.varIndexName = varIndexName;
	}

	@Override
	public String computeEvent() {
		String loopIn = super.computeEvent();
		if (!loopIn.isEmpty()) {
			String item = varItemName.isEmpty() ? "item" : varItemName;
			String index = varIndexName.isEmpty() ? "index" : varIndexName;
			
			String tsCode = "";
			tsCode += "\t\tconst doLoop = (c8oPage : C8oPageBase, "+ item +" : any, "+ index +" : number) : Promise<any> => {" + System.lineSeparator();
			tsCode += computeInnerGet("c8oPage", "doLoop");
			tsCode += loopIn;
			tsCode += "\t\t}" + System.lineSeparator();
			
			return tsCode;
		}
		return "";
	}
}
