/*
* Copyright (c) 2009-2014 Convertigo. All Rights Reserved.
*
* The copyright to the computer  program(s) herein  is the property
* of Convertigo.
* The program(s) may  be used  and/or copied  only with the written
* permission  of  Convertigo  or in accordance  with  the terms and
* conditions  stipulated  in the agreement/contract under which the
* program(s) have been supplied.
*
* Convertigo makes  no  representations  or  warranties  about  the
* suitability of the software, either express or implied, including
* but  not  limited  to  the implied warranties of merchantability,
* fitness for a particular purpose, or non-infringement. Convertigo
* shall  not  be  liable for  any damage  suffered by licensee as a
* result of using,  modifying or  distributing this software or its
* derivatives.
*/

/*
 * $URL: http://sourceus.twinsoft.fr/svn/convertigo/CEMS_opensource/branches/6.3.x/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/CustomStatsWizard.java $
 * $Author: jmc $
 * $Revision: 33092 $
 * $Date: 2014-01-15 12:44:33 +0100 (Thu, 02 Jan 2014) $
 */

package com.twinsoft.convertigo.eclipse.wizards;

import org.eclipse.jface.wizard.Wizard;

public class CustomStatsWizard extends Wizard {
	private String projectName;
	private CustomStatsWizardPage cawp;
	
	public CustomStatsWizard(String projectName) {
		super();
		this.projectName = projectName;
		setWindowTitle("Compute project statistics");
	}
	
	@Override
	public void addPages() {
		cawp = new CustomStatsWizardPage("CustomStatsWizardPage");
		cawp.setTitle("Project statistics");
		cawp.setMessage("Statistics about the selected project");
		this.addPage(cawp);
	}

	@Override
	public boolean canFinish() {
		return getContainer().getCurrentPage().getNextPage() == null;
	}

	@Override
	public boolean performFinish() {
		return true;
	}
}
