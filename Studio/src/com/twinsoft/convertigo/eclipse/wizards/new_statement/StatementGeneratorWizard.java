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

package com.twinsoft.convertigo.eclipse.wizards.new_statement;

import org.eclipse.jface.wizard.Wizard;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

public class StatementGeneratorWizard extends Wizard {
	
	private StatementWithExpressions parentObject = null; 
	private String xpath = null;
	private boolean[] possibleActions = null;
	
    private StatementGeneratorWizardPage statementGeneratorPage = null;
    private StatementInfoWizardPage statementInfoPage = null;
    
    public DatabaseObject newBean = null;

    public StatementGeneratorWizard(StatementWithExpressions selectedDatabaseObject, String xpath, boolean[] possibleActions) {
    	this(selectedDatabaseObject);
		this.xpath = xpath;
		this.possibleActions = possibleActions;
	}

    public StatementGeneratorWizard(StatementWithExpressions selectedDatabaseObject) {
		super();
		this.parentObject = selectedDatabaseObject;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		String pageTitle = "", pageMessage = "";
		pageTitle = "Generate statement";
		pageMessage = "Please select a statement generator.";
		
		statementGeneratorPage = new StatementGeneratorWizardPage(parentObject, xpath, possibleActions);
		statementGeneratorPage.setTitle(pageTitle);
		statementGeneratorPage.setMessage(pageMessage);
		this.addPage(statementGeneratorPage);
		
		statementInfoPage = new StatementInfoWizardPage();
		this.addPage(statementInfoPage);
		
	}

	
	private DatabaseObject getCreatedBean() {
		DatabaseObject dbo = null;
		if (statementGeneratorPage != null) {
			dbo = statementGeneratorPage.getCreatedBean();
		}
		return dbo;
	}
	
	public boolean canFinish() {
		return getContainer().getCurrentPage().getNextPage() == null;
	}

	public boolean performFinish() {
//		String name, statementName;
//		boolean bContinue = true;
//		int index = 0;

		try {
			newBean = getCreatedBean();
            if (newBean != null) {
            	/*statementName = newBean.getName();
            	
				while (bContinue) {
					if (index == 0) name = dboName;
					else name = dboName + index;
					newBean.setName(name);
					newBean.hasChanged = true;
					newBean.bNew = true;
					
					try {
						if ((newBean instanceof Statement) && (parentObject instanceof Transaction))
							newBean.priority = 0;
						
						if (newBean instanceof ScreenClass)
							newBean.priority = parentObject.priority + 1;
							
						parentObject.add(newBean);
						
						if (newBean instanceof HTTPStatement) {
							HTTPStatement httpStatement = (HTTPStatement)newBean;
							HtmlConnector connector = (HtmlConnector)httpStatement.getParentTransaction().getParent();
							httpStatement.setMethodType(HTTPStatement.HTTP_GET);
							httpStatement.setHost(connector.getServer());
							httpStatement.setPort(connector.getPort());
							httpStatement.setHttps(connector.isHttps());
						}

						if (newBean instanceof Connector) {
							Project project = (Project)parentObject;
							if (project.getDefaultConnector() == null)
								project.setDefaultConnector((Connector)newBean);
							
							this.setupConnector(newBean);
						}
						
						ConvertigoPlugin.logInfo("New statement named '" + newBean.getName() + "' has been added");

						bContinue = false;
					}
					catch(com.twinsoft.convertigo.engine.ObjectWithSameNameException owsne) {
						index++;
					}
				}
	*/
            } else {
            	throw new Exception("Could not instantiate bean!");
            }
		}
		catch (Exception e) {
            String message = "Unable to create new statement.";
            ConvertigoPlugin.logException(e, message);
		}
		
		return true;
	}

}
