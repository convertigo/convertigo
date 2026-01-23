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

package com.twinsoft.convertigo.eclipse.wizards.deploy;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class ProgressWizardDialog extends WizardDialog {

	public ProgressWizardDialog(Shell parentShell, IWizard wizard) {
       super(parentShell, wizard);
    }
    
	@Override
    protected Control createContents(Composite parent) {
       return super.createContents(parent);
    }
    
    @Override
    protected void nextPressed() {
       super.nextPressed();
       updateSize();
       IWizardPage pbwp = getWizard().getPage("ProjectBuildWizardPage");
       if (pbwp != null && getSelectedPage().equals(pbwp)) {
    	   ((ProjectBuildWizardPage)pbwp).doProcess();
       }
    }

    @Override
    protected void backPressed() {
       super.backPressed();
       updateSize();
    }

}
