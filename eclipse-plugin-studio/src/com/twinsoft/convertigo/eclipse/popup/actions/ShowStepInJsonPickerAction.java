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

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.jsonpicker.JsonPickerDialog;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StepTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

/** Shows the JSON a native step (or a JSON to XML step) produced at its last execution, in the JSON picker. */
public class ShowStepInJsonPickerAction extends MyAbstractAction {

	public ShowStepInJsonPickerAction() {
		super();
	}

	@Override
	public void run() {
		try {
			ProjectExplorerView explorerView = getProjectExplorerView();
			if (explorerView != null) {
				TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
				if (treeObject instanceof StepTreeObject stepTreeObject) {
					Step step = stepTreeObject.getObject();
					new JsonPickerDialog(getParentShell(), step).open();
				}
			}
		} catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to show the step in the JSON picker!");
		}
	}
}
