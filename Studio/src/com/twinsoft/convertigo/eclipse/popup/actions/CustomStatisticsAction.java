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
 * $URL: http://sourceus.twinsoft.fr/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/CustomAdditionsAction.java $
 * $Author: fabienb $
 * $Revision: 33546 $
 * $Date: 2013-02-11 15:19:04 +0100 (Mon, 11 Feb 2013) $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.StatisticsDialog;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;

public class CustomStatisticsAction extends MyAbstractAction {

	public CustomStatisticsAction() {
		super();
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			ProjectTreeObject projectTreeObject = (ProjectTreeObject)explorerView.getFirstSelectedTreeObject();
    			if (projectTreeObject != null) {
    				StatisticsDialog stats = new StatisticsDialog(shell, 
    						projectTreeObject.getName(), projectTreeObject.getObject().getComment(), 
    						projectTreeObject.getObject().getVersion());
    			
    				stats.open();
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to compute statistics!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
