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

package com.twinsoft.convertigo.engine.helpers;

import java.util.ArrayList;
import java.util.List;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class ScreenClassHelper<SC extends ScreenClass> {
	private SC defaultScreenClass = null;
	private Connector connector = null;
	
	public ScreenClassHelper(Connector connector) {
		this.connector = connector;
	}

	public boolean add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof ScreenClass) {
			setDefaultScreenClass((ScreenClass) databaseObject);
			return true;
		}
		return false;
	}

	public List<SC> getAllScreenClasses() {
		connector.checkSubLoaded();
		
		SC defaultScreenClass = getDefaultScreenClass();
		List<SC> v = getAllScreenClasses(defaultScreenClass);
		v.add(defaultScreenClass);
		return v;
	}
	
	private List<SC> getAllScreenClasses(SC screenClass) {
		connector.checkSubLoaded();
		
		List<SC> inheritedScreenClasses = getInheritedScreenClasses(screenClass);
		
		for (SC inheritedScreenClass  : new ArrayList<SC>(inheritedScreenClasses)) {
			inheritedScreenClasses.addAll(getAllScreenClasses(inheritedScreenClass));
		}
        
		return inheritedScreenClasses;
	}

	public SC getDefaultScreenClass() {
		connector.checkSubLoaded();
		
		return defaultScreenClass;
	}

	public SC getScreenClassByName(String screenClassName) {
		connector.checkSubLoaded();
		
		for (SC screenClass : getAllScreenClasses()) {
			if (screenClass.getName().equalsIgnoreCase(screenClassName)) {
				return screenClass;
			}
		}
		return null;
	}

	public void setDefaultScreenClass(ScreenClass defaultScreenClass) throws EngineException {
		connector.checkSubLoaded();
		
		if (this.defaultScreenClass != null) {
			throw new EngineException("Unable to set the root screen class: there is already one!");
		}
		defaultScreenClass.setParent(connector);
		try {
			this.defaultScreenClass = GenericUtils.<SC>cast(defaultScreenClass);
		} catch (ClassCastException e) {
			throw new EngineException("(ScreenClassHelper) cast failed", e);
		}
	}
	
	public SC getCurrentScreenClass() {
		return findScreenClass(getDefaultScreenClass());
	}
	
	public List<SC> getInheritedScreenClasses(SC screenClass) {
		return GenericUtils.cast(screenClass.getInheritedScreenClasses());
	}
	
	private  SC findScreenClass(SC currentScreenClass) {
		int scDepth = currentScreenClass.getDepth() * 3;
		String tab = scDepth == 0 ? "" : String.format("%" + scDepth + "s", "");
		Engine.logBeans.trace(tab + "Analyzing screen class " + currentScreenClass.getName());

		for (Criteria criteria : currentScreenClass.getLocalCriterias()) {
			if (criteria.isMatching(connector)) {
				Engine.logBeans.trace(tab + ". Criteria " + criteria.getName() + " is matching");
			} else {
				Engine.logBeans.trace(tab + ". Criteria " + criteria.getName() + " not matching; aborting screen class analyze");
				return null;
			}
		}

		Engine.logBeans.trace(tab + "::: Screen class " + currentScreenClass.getName() + " is matching");

		SC bestFoundScreenClass = currentScreenClass;
		int bestFoundScreenClassNumberOfLocalCriterias  = bestFoundScreenClass.getNumberOfLocalCriterias();
		SC screenClass;
		for (SC inheritedScreenClass : getInheritedScreenClasses(currentScreenClass)) 
			if ((screenClass = findScreenClass(inheritedScreenClass)) != null) {
				int screenClassNumberOfLocalCriterias = screenClass.getNumberOfLocalCriterias();
				// Avoid the "folder" screen classes, i.e. those with no local criterias
				if (screenClassNumberOfLocalCriterias != 0) {
					if ((screenClass.getDepth() > bestFoundScreenClass.getDepth()) ||
					(screenClassNumberOfLocalCriterias > bestFoundScreenClassNumberOfLocalCriterias)) {
						bestFoundScreenClass = screenClass;
						bestFoundScreenClassNumberOfLocalCriterias  = screenClassNumberOfLocalCriterias;
						Engine.logBeans.trace(tab + "   >>> Found a better screen class " + screenClass.getName());
					}
				}
			}
        
		if (bestFoundScreenClass != null) // Setting the returned screen class to the best found one
			currentScreenClass = bestFoundScreenClass;
        
		Engine.logBeans.trace(tab + "<<< Returning " + currentScreenClass.getName());
		return currentScreenClass;
	}
}
