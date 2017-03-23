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
 * $URL: svn://devus.twinsoft.fr/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/views/projectexplorer/model/ReferenceTreeObject.java $
 * $Author: nathalieh $
 * $Revision: 39934 $
 * $Date: 2015-06-11 19:30:12 +0200 (jeu., 11 juin 2015) $
 */

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import org.eclipse.jface.viewers.Viewer;
import com.twinsoft.convertigo.beans.core.MobileComponent;

public class MobileComponentTreeObject extends DatabaseObjectTreeObject {
	
	public MobileComponentTreeObject(Viewer viewer, MobileComponent object) {
		super(viewer, object);
	}

	public MobileComponentTreeObject(Viewer viewer, MobileComponent object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public MobileComponent getObject() {
		return (MobileComponent) super.getObject();
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		return super.testAttribute(target, name, value);
	}
}
