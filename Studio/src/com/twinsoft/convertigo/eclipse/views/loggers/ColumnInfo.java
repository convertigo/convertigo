/*
 * Copyright (c) 2001-2011 Convertigo SA.
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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.2.x/Studio/src/com/twinsoft/convertigo/eclipse/views/projectexplorer/ClipboardManager2.java $
 * $Author: nicolasa $
 * $Revision: 31165 $
 * $Date: 2012-07-20 17:45:54 +0200 (ven., 20 juil. 2012) $
 */

package com.twinsoft.convertigo.eclipse.views.loggers;

class ColumnInfo {
	private String name;
	
	public String getName() {
		return name;
	}
	
	private int size;
	
	public void setSize(int size) {
		this.size = size;
	}

	public int getSize() {
		return size;
	}
	
	private boolean visibility;

	public void setVisibility(boolean visibility) {
		this.visibility = visibility;
	}

	public boolean isVisible() {
		return visibility;
	}
	
	public ColumnInfo(String name, boolean visibility, int size) {
		this.name = name;
		this.visibility = visibility;
		this.size = size;
	}

	public static ColumnInfo parse(String name, String params) {
		String[] _params = params.split(",");
		return new ColumnInfo(name, Boolean.parseBoolean(_params[0]), Integer.parseInt(_params[1]));
	}

	public String toString() {
		return visibility + "," + size;
	}
}
