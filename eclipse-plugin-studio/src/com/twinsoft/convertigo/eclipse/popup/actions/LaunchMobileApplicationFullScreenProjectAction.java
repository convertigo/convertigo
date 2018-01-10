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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.3.x/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/TestCaseExecuteSelectedAction.java $
 * $Author: maximeh $
 * $Revision: 33944 $
 * $Date: 2013-04-05 18:29:40 +0200 (ven., 05 avr. 2013) $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class LaunchMobileApplicationFullScreenProjectAction extends MyAbstractAction {

	public LaunchMobileApplicationFullScreenProjectAction() {
		super();
	}

	@Override
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
		try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
    			Object databaseObject = treeObject.getObject();

    			if ((databaseObject != null) && (databaseObject instanceof MobileApplication)) {
    				MobileApplication mobileApplication = (MobileApplication) databaseObject;
    				String SERVER_C8O_URL = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
    				Program.launch( SERVER_C8O_URL
    								+ "/projects/" 
    								+ mobileApplication.getProject().getName() + "/DisplayObjects/mobile/index.html");
    			}
    		}
        	
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to launch the mobile application selected!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}

}
