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

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NgxApplicationComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NgxComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NgxPageComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NgxUIComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class EditNgxComponentClassAction extends MyAbstractAction {

	public EditNgxComponentClassAction() {
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
    			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
    			if (treeObject instanceof NgxComponentTreeObject) {
    				if (treeObject instanceof NgxApplicationComponentTreeObject) {
    					NgxApplicationComponentTreeObject mpcto = GenericUtils.cast(treeObject);
    					mpcto.editAppComponentTsFile();
    				}
    				else if (treeObject instanceof NgxPageComponentTreeObject) {
    					NgxPageComponentTreeObject mpcto = GenericUtils.cast(treeObject);
    					mpcto.editPageTsFile();
    				}
    				else if (treeObject instanceof NgxUIComponentTreeObject) {
    					NgxUIComponentTreeObject mpcto = GenericUtils.cast(treeObject);
    					mpcto.editCompTsFile();
    				}
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to edit ngx component class!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
