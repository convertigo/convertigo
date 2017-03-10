/*
 * Copyright (c) 2001-2016 Convertigo SA.
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

package com.twinsoft.convertigo.beans.mobile.components;

public class RouteDataComponent extends RouteComponent {

	private static final long serialVersionUID = -1240983643118635049L;

	public RouteDataComponent() {
		super();
	}
	
	@Override
	public RouteDataComponent clone() throws CloneNotSupportedException {
		RouteDataComponent cloned = (RouteDataComponent)super.clone();
		return cloned;
	}

	private String getPageName() {
		String pageName = getPage();
		if (!pageName.isEmpty() && pageName.startsWith(getProject().getName())) {
			try {
				pageName = pageName.substring(pageName.lastIndexOf('.')+1);
			} catch (IndexOutOfBoundsException e) {}
		}
		return pageName;
	}
	
	@Override
	public String toString() {
		String label = "?";
		String targetAction = getAction();
		if (!targetAction.isEmpty()) {
			String targetPage = getPageName();
			if (targetPage.isEmpty()) {
				label = targetAction + " ?";
			}
			else {
				label = targetAction + " " + targetPage;
			}
		}
		return label;
	}
	
	@Override
	protected String computeTemplate() {
		// TODO Auto-generated method stub
		return null;
	}

}
