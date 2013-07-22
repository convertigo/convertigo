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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface IPropertyTreeObject {
	public boolean isInherited();
	public TreeParent getTreeObjectOwner();
	public IPropertyTreeObject add(Object object, boolean bChangeName);
	public void remove(Object object);
	public Element toXml(Document document);
	public String getPath();
	public TreeParent getParent();
}
