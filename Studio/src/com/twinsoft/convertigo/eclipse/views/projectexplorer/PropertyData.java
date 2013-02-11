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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import com.twinsoft.convertigo.beans.common.XMLVector;

public class PropertyData {
	private XMLVector<Object> object = null;
	private Class<? extends TreeParent> ownerClass = null;
	public PropertyData(Class<? extends TreeParent> ownerClass, XMLVector<Object> object) {
		this.object = object;
		this.ownerClass = ownerClass;
	}
	public XMLVector<Object> getData() {
		return object;
	}
	public Class<? extends TreeParent> getOwnerClass() {
		return ownerClass;
	}
}
