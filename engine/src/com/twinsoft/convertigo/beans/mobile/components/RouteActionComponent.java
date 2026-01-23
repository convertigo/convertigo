/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboFolderType;
import com.twinsoft.convertigo.beans.core.IEnableAble;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.util.EnumUtils;

@DboCategoryInfo(
		getCategoryId = "RouteAction",
		getCategoryName = "Action",
		getIconClassCSS = "convertigo-action-newRouteActionComponent"
	)
@DboFolderType(type = FolderType.ACTION)
public abstract class RouteActionComponent extends MobileComponent implements IRouteGenerator, ITagsProperty, IEnableAble {
	
	private static final long serialVersionUID = 2529010701434894046L;

	public enum Action {
		push, 	// push
		root, 	// setRoot
		toast,	// toast
	}
	
	public RouteActionComponent() {
		super();
		
		this.priority = getNewOrderValue();
	}
	
	@Override
	public RouteActionComponent clone() throws CloneNotSupportedException {
		RouteActionComponent cloned = (RouteActionComponent)super.clone();
		return cloned;
	}
	
    @Override
    public Object getOrderedValue() {
    	return priority;
    }
	
	@Override
	public RouteComponent getParent() {
		return (RouteComponent) super.getParent();
	}	

	/*
	 * The condition to be verified on data 
	 */
	private String condition = "";
	
	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}
	
	/*
	 * The action to trigger
	 */
	protected String action = "";
	
	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	/*
	 * The page associated with action
	 */
	private String page = "";
	
	public String getPage() {
		return page;
	}

	public void setPage(String page) {
		this.page = page;
	}
	
	/*
	 * The toast message
	 */
	private String toastMessage = "Your message";
	
	public String getToastMessage() {
		return toastMessage;
	}

	public void setToastMessage(String toastMessage) {
		this.toastMessage = toastMessage;
	}

	/*
	 * The toast duration
	 */
	private int toastDuration = 5000;
	
	public int getToastDuration() {
		return toastDuration;
	}

	public void setToastDuration(int toastDuration) {
		this.toastDuration = toastDuration;
	}

	/*
	 * The toast position
	 */
	private String toastPosition = "bottom";
	
	public String getToastPosition() {
		return toastPosition;
	}

	public void setToastPosition(String toastPosition) {
		this.toastPosition = toastPosition;
	}

	/*
	 * The enabled state
	 */
	private boolean isEnabled = true;
	
	@Override
	public boolean isEnabled() {
		return isEnabled;
	}
	
	@Override
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
	
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("action")) {
			return EnumUtils.toNames(Action.class);
		}
		return new String[0];
	}
	
	@Override
	public boolean testAttribute(String name, String value) {
		if (name.equals("isEnabled")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isEnabled()));
		}
		return super.testAttribute(name, value);
	}
}
