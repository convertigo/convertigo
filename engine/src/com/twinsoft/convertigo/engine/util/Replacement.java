/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.util;

public class Replacement {
	private String strSource;
	private String strTarget;
	private String startsWith;
	
	public Replacement() {
		this("","");
	}
	
	public Replacement(String strSource, String strTarget) {
		this(strSource, strTarget, null);
	}
	
	Replacement(String strSource, String strTarget, String startsWith) {
		this.setSource(strSource);
		this.setTarget(strTarget);
		this.setStartsWith(startsWith);
	}

	public String getSource() {
		return strSource;
	}

	public void setSource(String strSource) {
		this.strSource = strSource;
	}

	public String getTarget() {
		return strTarget;
	}

	public void setTarget(String strTarget) {
		this.strTarget = strTarget;
	}
	
	public String getStartsWith() {
		return startsWith;
	}

	public void setStartsWith(String startsWith) {
		this.startsWith = startsWith;
	}
}
