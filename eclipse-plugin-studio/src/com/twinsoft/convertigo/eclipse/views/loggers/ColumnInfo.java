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

package com.twinsoft.convertigo.eclipse.views.loggers;

class ColumnInfo implements Cloneable {
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
	
	private boolean bVisible;

	public void setVisible(boolean bVisible) {
		this.bVisible = bVisible;
	}

	public boolean isVisible() {
		return bVisible;
	}
	
	public ColumnInfo(String name, boolean bVisible, int size) {
		this.name = name;
		this.bVisible = bVisible;
		this.size = size;
	}

	@Override
	protected Object clone() {
		ColumnInfo columnInfo = new ColumnInfo(name, bVisible, size);

		return columnInfo;
	}

	public String toString() {
		return name + "=" + bVisible + "," + size;
	}
}
