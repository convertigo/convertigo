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
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class ClipboardPasteAction extends ClipboardAction {

	public ClipboardPasteAction() {
		super(ConvertigoPlugin.clipboardManagerSystem);
	}
	
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
        
		try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		TreeObject selectedTreeObject = explorerView.getFirstSelectedTreeObject();
    		
    		String source = null;
    		if (!clipboardManager.isCut) {
	        	Clipboard clipboard = new Clipboard(display);
	        	TextTransfer textTransfer = TextTransfer.getInstance();
	        	source = (String)clipboard.getContents(textTransfer);
	        	clipboard.dispose();
    		}
    		
    		if (explorerView.isEditing()) {
    			explorerView.setEditingText(source);
    		}
    		else paste(source, shell, explorerView, selectedTreeObject);
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to paste!");
		}
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
