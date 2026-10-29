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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.YamlConverter.Format;

public class ProjectConvertYamlAction extends MyAbstractAction {
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		action.setEnabled(selection instanceof IStructuredSelection structured
				&& structured.size() == 1 && structured.getFirstElement() instanceof ProjectTreeObject tree
				&& tree.getObject().getYamlFormat() == Format.LEGACY);
	}

	@Override
	public void run() {
		var explorer = getProjectExplorerView();
		if (explorer == null || !(explorer.getFirstSelectedTreeObject() instanceof ProjectTreeObject tree)
				|| tree.getObject().getYamlFormat() != Format.LEGACY) return;
		var shell = getParentShell();
		if (!MessageDialog.openQuestion(shell, "Convert to standard YAML",
				"Convert \"" + tree.getName() + "\" to standard YAML?\n\n"
				+ "This saves the current project changes and raises its minimum engine version to 8.5. "
				+ "Older engines will no longer be able to open it.")) return;
		var cursor = new Cursor(Display.getDefault(), SWT.CURSOR_WAIT);
		try {
			// Save through the usual path so editor state and modified markers stay consistent.
			if (tree.hasChanged() && !tree.save(false)) return;
			shell.setCursor(cursor);
			Engine.theApp.databaseObjectsManager.convertProjectToStandardYaml(tree.getObject());
			tree.getIProject().refreshLocal(IResource.DEPTH_INFINITE, null);
			explorer.refreshTree();
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to convert project to standard YAML");
		} finally {
			shell.setCursor(null);
			cursor.dispose();
		}
	}

	@Override
	protected boolean canImpactMobileBuilder(TreeObject object) {
		return true;
	}
}
