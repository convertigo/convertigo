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

import java.util.List;

import com.twinsoft.convertigo.beans.screenclasses.JavelinScreenClass;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.helpers.ScreenClassHelper;

/**
 * The Connector class is the base class for all connectors.
 */
public abstract class ConnectorWithScreenClasses extends Connector implements IScreenClassContainer<JavelinScreenClass> {

	private static final long serialVersionUID = -7420910147197300870L;

	transient private ScreenClassHelper<JavelinScreenClass> screenClassHelper = new ScreenClassHelper<JavelinScreenClass>(this);
	
	public ConnectorWithScreenClasses() {
        super();
	}

	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.Connector#clone()
	 */
	@Override
	public ConnectorWithScreenClasses clone() throws CloneNotSupportedException {
		ConnectorWithScreenClasses clonedObject = (ConnectorWithScreenClasses) super.clone();
		clonedObject.screenClassHelper = new ScreenClassHelper<JavelinScreenClass>(clonedObject);
		return clonedObject;
	}

	public JavelinScreenClass getDefaultScreenClass() {
		return screenClassHelper.getDefaultScreenClass();
	}
    
	public void setDefaultScreenClass(ScreenClass defaultScreenClass) throws EngineException {
		screenClassHelper.setDefaultScreenClass(defaultScreenClass);
	}
    
	public List<JavelinScreenClass> getAllScreenClasses() {
		return sort(screenClassHelper.getAllScreenClasses());
	}
    
	public JavelinScreenClass getScreenClassByName(String screenClassName) {
		return screenClassHelper.getScreenClassByName(screenClassName);
	}

	@Override
	public void add(DatabaseObject databaseObject) throws EngineException {
		if (!screenClassHelper.add(databaseObject)) {
			super.add(databaseObject);
		}
	}

	/**
	 * Returns the current screen class.
	 * 
	 * @return the current screen class.
	 * 
	 * @EngineException exception if any error occurs.
	 */
	public final JavelinScreenClass getCurrentScreenClass() {
		JavelinScreenClass rootScreenClass = getDefaultScreenClass();
		return findScreenClass(rootScreenClass);
	}
    
	private JavelinScreenClass findScreenClass(JavelinScreenClass currentScreenClass) {
		Engine.logBeans.trace("Analyzing screen class " + currentScreenClass.getName());
		for (Criteria criteria : currentScreenClass.getCriterias()) {
			if (criteria.getParent() == currentScreenClass) {
				Engine.logBeans.trace("  Testing criteria " + criteria.getName());
				if (!criteria.isMatching(this)) {
					Engine.logBeans.trace("  !!! Criteria " + criteria.getName() + " not matching; aborting screen class analyze");
					return null;
				}
			}
		}
        
		JavelinScreenClass bestFoundScreenClass = currentScreenClass;
		int screenClassNumberOfLocalCriterias;
		int bestFoundScreenClassNumberOfLocalCriterias  = bestFoundScreenClass.getNumberOfLocalCriterias();
		JavelinScreenClass screenClass;
		for (JavelinScreenClass inheritedScreenClass : screenClassHelper.getInheritedScreenClasses(currentScreenClass)) {
			screenClass = findScreenClass(inheritedScreenClass);
			if (screenClass != null) {
				screenClassNumberOfLocalCriterias = screenClass.getNumberOfLocalCriterias();
				// Avoid the "folder" screen classes, i.e. those with no local criterias
				if (screenClassNumberOfLocalCriterias != 0) {
					if ((screenClass.getDepth() > bestFoundScreenClass.getDepth()) ||
					(screenClassNumberOfLocalCriterias > bestFoundScreenClassNumberOfLocalCriterias)) {
						bestFoundScreenClass = screenClass;
						bestFoundScreenClassNumberOfLocalCriterias  = screenClassNumberOfLocalCriterias;
						Engine.logBeans.trace("  >>> Best found screen class " + screenClass.getName());
					}
				}
			}
		}
        
		if (bestFoundScreenClass != null) {
			// Setting the returned screen class to the best found one
			currentScreenClass = bestFoundScreenClass;
		}
        
		Engine.logBeans.trace("  <<< Returning " + currentScreenClass.getName());
		return currentScreenClass;
	}
	
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.add(0, getDefaultScreenClass());
		return rep;
	}

}
