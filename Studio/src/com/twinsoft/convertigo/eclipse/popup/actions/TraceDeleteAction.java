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

package com.twinsoft.convertigo.eclipse.popup.actions;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TraceTreeObject;

public class TraceDeleteAction extends MyAbstractAction {

	public TraceDeleteAction() {
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
    			TraceTreeObject traceObject = (TraceTreeObject)explorerView.getFirstSelectedTreeObject();
    			
				MessageBox messageBox = new MessageBox(shell,SWT.YES | SWT.NO | SWT.CANCEL | SWT.ICON_QUESTION | SWT.APPLICATION_MODAL);
				String message = java.text.MessageFormat.format("Do you really want to delete the trace \"{0}\"?", new Object[] {traceObject.getName()});
	        	messageBox.setMessage(message);
	        	if (messageBox.open() == SWT.YES) {
	        		File file = (File) traceObject.getObject();
	        		if (file.exists()) {
	        			if (file.delete()) {
	        				TreeParent treeParent = traceObject.getParent();
	        				treeParent.removeChild(traceObject);
	        				explorerView.refreshTreeObject(treeParent);
	        			}
	        			else {
	        				throw new Exception("Unable to delete file \""+ file.getAbsolutePath() + "\"");
	        			}
	        		}
	        	}
    			
    		}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to delete the trace file!");
		}
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
	
}
