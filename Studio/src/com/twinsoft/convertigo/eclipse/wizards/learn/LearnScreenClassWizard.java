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

package com.twinsoft.convertigo.eclipse.wizards.learn;


import org.eclipse.jface.wizard.Wizard;

import com.twinsoft.convertigo.beans.common.XPath;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.screenclasses.HtmlScreenClass;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class LearnScreenClassWizard extends Wizard {
	
	private ScreenClass detectedScreenClass = null; 
    private HtmlScreenClass htmlScreenClass = null;
    
    private LearnScreenClassWizardPage1 page1 = null;
    private LearnScreenClassWizardPage2 page2 = null;
    
    public LearnScreenClassWizard(ScreenClass detectedScreenClass) {
		super();
		this.detectedScreenClass = detectedScreenClass;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		try {
			page1 = new LearnScreenClassWizardPage1(detectedScreenClass);
			addPage(page1);

			page2 = new LearnScreenClassWizardPage2(detectedScreenClass);
			addPage(page2);
		}
        finally {
        	;
        }
	}

	public boolean performFinish() {
		String screenClassName = null;
		String criteriaName = null;
		XPath criteria = null;
		try {
			DatabaseObject parentObject = page2.getParentObject();
			if (parentObject != null) {
				// create new screen class
				htmlScreenClass = new HtmlScreenClass();
	            if (htmlScreenClass != null) {
            		screenClassName = page2.getScreenClassName();
					if (!StringUtils.isNormalized(screenClassName))
						throw new EngineException("Screenclass name is not normalized : \""+screenClassName+"\".");
            		
					htmlScreenClass.setName(screenClassName);
					htmlScreenClass.hasChanged = true;
					htmlScreenClass.priority = parentObject.priority + 1;
					parentObject.add(htmlScreenClass);
					ConvertigoPlugin.logInfo("New screen class named '" + screenClassName + "' has been added!");
					Engine.logBeans.debug("New screen class named '" + screenClassName + "' has been added!", null);
	            }
	            
				
				if (htmlScreenClass != null) {
					criteria = new XPath();
					if (criteria != null) {
						criteriaName = criteria.getName();
						criteria.hasChanged = true;
						try {
							htmlScreenClass.add(criteria);
							ConvertigoPlugin.logInfo("New criteria named '" + criteriaName + "' has been added!");
							Engine.logBeans.debug("New criteria named '" + criteriaName + "' has been added!", null);
						}
						catch(EngineException e) {
							throw e;
						}
					}
				}
			}
		}
		catch (EngineException e) {
            String message = "Unable to create new screenclass class!";
            ConvertigoPlugin.logException(e, message);
		}
		
		return true;
	}

	public ScreenClass getCreatedScreenClass() {
		return htmlScreenClass;
	}
}
