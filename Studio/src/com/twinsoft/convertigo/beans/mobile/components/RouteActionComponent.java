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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.MobileComponent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.EnumUtils;

public abstract class RouteActionComponent extends MobileComponent implements ITagsProperty {

	private static final long serialVersionUID = 2529010701434894046L;

	public enum Action {
		push,
		root,
		setRoot,
		toast,
	}
	
	public RouteActionComponent() {
		super();
		
		this.priority = getNewOrderValue();
		this.newPriority = priority;
	}
	
	@Override
	public RouteActionComponent clone() throws CloneNotSupportedException {
		RouteActionComponent cloned = (RouteActionComponent)super.clone();
		cloned.newPriority = newPriority;
		return cloned;
	}

	@Override
	public Element toXml(Document document) throws EngineException {
		Element element =  super.toXml(document);
		
        element.setAttribute("newPriority", new Long(newPriority).toString());
		
		return element;
	}
	
    @Override
    public Object getOrderedValue() {
    	return new Long(priority);
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
	
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("action")) {
			return EnumUtils.toNames(Action.class);
		}
		return new String[0];
	}
	
	@Override
	protected String computeTemplate() {
		// TODO Auto-generated method stub
		return null;
	}

}
