/*
 * Copyright (c) 2001-2019 Convertigo SA.
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
				String message = getToastMessage();
				message = message.isEmpty() ? "?":message;
				label = targetAction + " " + (targetAction.equals(Action.toast.name()) ? "'"+message+"'":"?");
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
		if (isEnabled()) {
			String condition = getCondition();
			String targetPage = getPageName();
			String targetAction = getAction();
			
			if (!targetAction.isEmpty()) {
				if (targetAction.equals("root")) {
					targetAction = "setRoot";
				}
				
				sb.append("new C8oRoute((data:any)=>{return "+ (condition.isEmpty() ? "true":condition) +"}, tableOptions)");
				
				if (!targetPage.isEmpty()) {
					String tplVersion = getTplVersion();
					boolean isCustomCafTpl = tplVersion == null ? false : tplVersion.endsWith(".7702"); // tpl caf packages corrected
					if (compareToTplVersion("7.7.0.2") < 0 && !isCustomCafTpl) {
						sb.append(".setTarget(\""+targetAction+"\", "+targetPage+")");
					} else {
						sb.append(".setTarget(\""+targetAction+"\", \""+targetPage+"\")");
					}
				}
				else {
					sb.append(".setTarget(\""+targetAction+"\")");
				}
				
				if (targetAction.equals(Action.toast.name())) {
					String message = getToastMessage();
					sb.append(".setToastMesage(\""+ (message.isEmpty() ? "Your message":message) +"\")");
					
					String position = getToastPosition();
					sb.append(".setToastPosition(\""+ (position.isEmpty() ? "bottom":position) +"\")");
					
					String duration = ""+ getToastDuration();
					sb.append(".setToastDuration("+ (duration.isEmpty() ? "5000":duration) +")");
				}
			}
		}
		return sb.toString();
	}

}
