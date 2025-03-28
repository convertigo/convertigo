/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.UIText;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;

public class I18nUIComponentAction extends MyAbstractAction {

	public I18nUIComponentAction() {
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
				boolean enable = this.getId().contains("Enable");

				TreeObject[] treeObjects = explorerView.getSelectedTreeObjects();
				Set<DatabaseObject> found = new HashSet<>();
				for (int i = treeObjects.length - 1 ; i>=0  ; i--) {
					var treeObject = (DatabaseObjectTreeObject) treeObjects[i];
						new WalkHelper() {
							@Override
							public void walk(DatabaseObject dbo) throws Exception {
								if (dbo instanceof UIText) {
									var txt = (UIText) dbo;
									if (txt.isI18n() != enable) {
										txt.setI18n(enable);
										dbo.hasChanged = true;
										found.add(dbo);
										found.add(dbo.getProject());
									}
								} else if (dbo instanceof com.twinsoft.convertigo.beans.mobile.components.UIText) {
									var txt = (com.twinsoft.convertigo.beans.mobile.components.UIText) dbo;
									if (txt.isI18n() != enable) {
										txt.setI18n(enable);
										dbo.hasChanged = true;
										found.add(dbo);
										found.add(dbo.getProject());
									}
								}
								super.walk(dbo);
							}
						}.init(treeObject.getObject());
				}
				if (!found.isEmpty()) {
					for (var d :found) {
						explorerView.updateDatabaseObject(d);
					}
				}
			}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to change i18n settings!");
		}
		finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}

	@Override
	protected boolean canImpactMobileBuilder(TreeObject ob) {
		return true;
	}
}
