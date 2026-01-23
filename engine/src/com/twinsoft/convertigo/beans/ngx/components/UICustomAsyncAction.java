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

public class UICustomAsyncAction extends UICustomAction {

	private static final long serialVersionUID = 2788516745693437706L;

	public UICustomAsyncAction() {
		super();
		isAsync = true;
	}
	
	@Override
	protected String getDefaultActionValue() {
		return "\t\ttry {\n\t\t\tpage.c8o.log.debug('[MB] '+ props.actionFunction +': '+ props.actionName);\n\t\t} catch (e) {\n\t\t\n\t\t} finally {\n\t\t\treturn;\n\t\t}\n";
	}
	
	@Override
	public UICustomAsyncAction clone() throws CloneNotSupportedException {
		UICustomAsyncAction cloned = (UICustomAsyncAction) super.clone();
		return cloned;
	}
}
