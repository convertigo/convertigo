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

public class RouteDataActionComponent extends RouteActionComponent {

	private static final long serialVersionUID = -1240983643118635049L;

	public RouteDataActionComponent() {
		super();
	}
	
	@Override
	public RouteDataActionComponent clone() throws CloneNotSupportedException {
		RouteDataActionComponent cloned = (RouteDataActionComponent)super.clone();
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
				label = targetAction + " " + (targetAction.equals(Action.toast.name()) ? "'Your message'":"?");
			}
			else {
				label = targetAction + " " + targetPage;
			}
		}
		return label;
	}
	
	@Override
	public String computeRoute() {
		StringBuilder sb = new StringBuilder();
		String condition = getCondition();
		String targetPage = getPageName();
		String targetAction = getAction();
		if (!condition.isEmpty()) {
			sb.append("new C8oRoute((data:any)=>{return "+ condition +"})");
			if (!targetAction.isEmpty()) {
				if (!targetPage.isEmpty()) {
					sb.append(".setTarget(\""+targetAction+"\", "+targetPage+")");
				}
				else {
					sb.append(".setTarget(\""+targetAction+"\")");
				}
				
				if (targetAction.equals(Action.toast.name())) {
					sb.append(".setToastMesage(\""+ "Your message" +"\")")
						.append(".setToastPosition(\""+ "bottom" +"\")")
						.append(".setToastDuration("+ "5000" +")");
				}
			}
		}
		return sb.toString();
	}

}
