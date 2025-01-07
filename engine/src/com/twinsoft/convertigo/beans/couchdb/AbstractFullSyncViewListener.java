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

package com.twinsoft.convertigo.beans.couchdb;


abstract public class AbstractFullSyncViewListener extends AbstractFullSyncListener {

	private static final long serialVersionUID = -7361533107225235685L;

	protected String targetView = "";
	
	public AbstractFullSyncViewListener() {
		super();
	}

	@Override
	public AbstractFullSyncViewListener clone() throws CloneNotSupportedException {
		AbstractFullSyncViewListener clonedObject =  (AbstractFullSyncViewListener) super.clone();
		return clonedObject;
	}
	
	public String getTargetView() {
		return targetView;
	}

	public void setTargetView(String targetView) {
		this.targetView = targetView;
	}

	protected String getTargetDocName() {
		if (targetView != null) {
			try {
				return targetView.split("\\.")[2];
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	protected String getTargetViewName() {
		if (targetView != null) {
			try {
				return targetView.split("\\.")[3];
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
