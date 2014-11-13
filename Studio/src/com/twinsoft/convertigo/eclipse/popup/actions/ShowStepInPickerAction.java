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

import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.StepSourceEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.sourcepicker.SourcePickerView;

public class ShowStepInPickerAction extends MyAbstractAction {

	protected boolean showSource = false;
	
	public ShowStepInPickerAction() {
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
    			if (treeObject != null) {
    				if (treeObject instanceof DatabaseObjectTreeObject) {
    					DatabaseObject selectedDbo = ((DatabaseObjectTreeObject)treeObject).getObject();
	    				if (selectedDbo != null) {
	    					StepSourceEvent event = null;
	    					if (showSource) {
	    						if (selectedDbo instanceof Step) {
	    							Step step = (Step) selectedDbo;
	    							Set<StepSource> sources = step.getSources();
	    							if (!sources.isEmpty()) {
	    								event = new StepSourceEvent(sources.iterator().next());
	    							} else {
	            						throw new Exception("No Source defined"); 
	            					}
	    						}
	    					} else {
	    						event = new StepSourceEvent(selectedDbo);
	    					}
	    					
	    					if (event != null) {
	            				SourcePickerView spv = ConvertigoPlugin.getDefault().getSourcePickerView();
	            				if (spv == null) {
	            					spv = (SourcePickerView) getActivePage().showView("com.twinsoft.convertigo.eclipse.views.sourcepicker.SourcePickerView");
	            				}
	            				if (spv != null) {
	            					spv.sourceSelected(event);
	            				}
	    					}
	    				}
    				}
    			}
			}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to show object in Picker!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
