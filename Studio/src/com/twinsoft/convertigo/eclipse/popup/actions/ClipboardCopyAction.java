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

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;

public class ClipboardCopyAction extends ClipboardAction {
	
	public ClipboardCopyAction() {
		super(ConvertigoPlugin.clipboardManagerSystem);
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
        
		try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			String sXml;
    			if (explorerView.isEditing()) {
    				sXml = explorerView.getEditingText();
    			}
    			else {
	    			// copy to clipboard manager
	   				sXml = copy(explorerView);
    			}
    			
   				// copy to system clipboard
    			if (sXml != null) {
       				Clipboard clipboard = new Clipboard(display);
       				TextTransfer textTransfer = TextTransfer.getInstance();
       				clipboard.setContents(new String[]{sXml}, new Transfer[]{textTransfer});
       				clipboard.dispose();
    			}
    		}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to copy!");
		}
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
