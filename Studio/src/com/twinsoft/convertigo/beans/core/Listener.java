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

package com.twinsoft.convertigo.beans.core;

public abstract class Listener extends DatabaseObject {

	private static final long serialVersionUID = 9010595374464464408L;
	
	public Listener() {
		super();
		databaseType = "Listener";
	}

	@Override
	public Listener clone() throws CloneNotSupportedException {
		Listener clonedObject =  (Listener) super.clone();
		return clonedObject;
	}
	
	public String getRenderer() {
		return "ListenerTreeObject";
	}
		
}
