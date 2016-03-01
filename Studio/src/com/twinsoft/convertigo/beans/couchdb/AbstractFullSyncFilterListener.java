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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.couchdb;


abstract public class AbstractFullSyncFilterListener extends AbstractFullSyncListener {

	private static final long serialVersionUID = -7361533107225235685L;

	protected String targetFilter = "_doc_ids";
	
	public AbstractFullSyncFilterListener() {
		super();
	}

	@Override
	public AbstractFullSyncFilterListener clone() throws CloneNotSupportedException {
		AbstractFullSyncFilterListener clonedObject =  (AbstractFullSyncFilterListener) super.clone();
		return clonedObject;
	}
	
	public String getTargetFilter() {
		return targetFilter;
	}

	public void setTargetFilter(String targetFilter) {
		this.targetFilter = targetFilter;
	}

	protected String getTargetDocName() {
		if (targetFilter != null) {
			try {
				return targetFilter.split("\\.")[2];
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	protected String getTargetFilterName() {
		if (targetFilter != null) {
			try {
				return targetFilter.split("\\.")[3];
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
