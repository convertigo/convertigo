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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPlaceholderFolderLayout;

import com.twinsoft.convertigo.eclipse.views.assistant.AssistantView;

public class TigoPerspective implements IPerspectiveFactory {

	public static final String ID = "com.twinsoft.convertigo.eclipse.TigoPerspective";

	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.addView(AssistantView.ID, IPageLayout.LEFT, 0.35f, editorArea);
		layout.addView("com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView",
				IPageLayout.BOTTOM, 0.72f, AssistantView.ID);

		// Keep diagnostics available without opening them in the initial workspace.
		IPlaceholderFolderLayout diagnostics = layout.createPlaceholderFolder("tigo.diagnostics", IPageLayout.BOTTOM, 0.75f, editorArea);
		diagnostics.addPlaceholder("com.twinsoft.convertigo.eclipse.views.loggers.EngineLogView");
		diagnostics.addPlaceholder("org.eclipse.ui.console.ConsoleView");
		diagnostics.addPlaceholder("com.twinsoft.convertigo.eclipse.views.mobile.MobileDebugView");

		layout.addShowViewShortcut(AssistantView.ID);
		layout.addShowViewShortcut("com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView");
		layout.addShowViewShortcut("com.twinsoft.convertigo.eclipse.views.loggers.EngineLogView");
		layout.addShowViewShortcut("com.twinsoft.convertigo.eclipse.views.mobile.MobileDebugView");
		layout.addShowViewShortcut("com.twinsoft.convertigo.eclipse.views.marketplace.MarketplaceView");
		layout.addShowViewShortcut("com.twinsoft.convertigo.eclipse.views.admin.AdminView");
		layout.addPerspectiveShortcut(ConvertigoPlugin.PLUGIN_PERSPECTIVE_ID);
	}
}
