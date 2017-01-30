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

import java.util.LinkedList;
import java.util.List;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobileComponent;
import com.twinsoft.convertigo.engine.EngineException;

public class ApplicationComponent extends MobileComponent {

	private static final long serialVersionUID = 6142350115354549719L;

	public ApplicationComponent() {
		super();
	}

	@Override
	public ApplicationComponent clone() throws CloneNotSupportedException {
		ApplicationComponent cloned = (ApplicationComponent) super.clone();
		cloned.vPageComponents = new LinkedList<PageComponent>();
		return cloned;
	}

	/**
	 * The list of available page component for this application.
	 */
	transient private List<PageComponent> vPageComponents = new LinkedList<PageComponent>();
	
	protected void addPageComponent(PageComponent pageComponent) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vPageComponents, pageComponent.getName(), pageComponent.bNew);
		pageComponent.setName(newDatabaseObjectName);
		vPageComponents.add(pageComponent);
		super.add(pageComponent);
	}

	public void removePageComponent(PageComponent pageComponent) throws EngineException {
		checkSubLoaded();
		vPageComponents.remove(pageComponent);
	}

	public List<PageComponent> getPageComponentList() {
		checkSubLoaded();
		return sort(vPageComponents);
	}

	public PageComponent getPageComponentByName(String pageName) throws EngineException {
		checkSubLoaded();
		for (PageComponent pageComponent : vPageComponents)
			if (pageComponent.getName().equalsIgnoreCase(pageName)) return pageComponent;
		throw new EngineException("There is no page component named \"" + pageName + "\" found into this application.");
	}
	
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.addAll(getPageComponentList());
		return rep;
	}

	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof PageComponent) {
			addPageComponent((PageComponent) databaseObject);
		} else {
			throw new EngineException("You cannot add to an application component a database object of type " + databaseObject.getClass().getName());
		}
    }

    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof PageComponent) {
			removePageComponent((PageComponent) databaseObject);
		} else {
			throw new EngineException("You cannot remove from an application component a database object of type " + databaseObject.getClass().getName());
		}
		super.remove(databaseObject);
    }

	@Override
	protected String computeTemplate() {
		return "";
	}
    
}
