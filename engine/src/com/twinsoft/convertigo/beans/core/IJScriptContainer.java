/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.beans.core;

import com.twinsoft.convertigo.engine.util.StringUtils;

public interface IJScriptContainer {
	public String getExpression();
	public void setExpression(String expression);
	public String getName();
	
	public default DatabaseObject getDatabaseObject() {
		if (this instanceof DatabaseObject) {
			return (DatabaseObject) this;
		}
		return null;
	}
	
	public default String getFullName() {
		DatabaseObject dbo = getDatabaseObject();
		if (dbo != null) {
			String qn = dbo.getQName(true).replace(':', '-');
			int i = qn.lastIndexOf('.');
			if (i > 0) {
				return StringUtils.hash(qn.substring(0, i)) + qn.substring(i); 
			} else {
				return qn;
			}
		}
		return getName();
	}
	
	public default String getEditorName() {
		return getName();
	}
}
