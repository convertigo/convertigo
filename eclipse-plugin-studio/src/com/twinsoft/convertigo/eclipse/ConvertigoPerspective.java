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

package com.twinsoft.convertigo.eclipse;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class ConvertigoPerspective implements IPerspectiveFactory {

	public ConvertigoPerspective() {
	}

	public void createInitialLayout(IPageLayout layout) {
//		IWorkbenchWindow workbenchWindow =  PlatformUI.getWorkbench().getActiveWorkbenchWindow();
//		walkMenuManager(((WorkbenchWindow) workbenchWindow).getMenuBarManager(), "");
	}
	
//	private void walkMenuManager(IMenuManager manager, String path) {
//		for (IContributionItem item : manager.getItems()) {
//			String nPath = path + "/" + item.getId();
//			System.out.println(nPath + " : " + item.toString());
//			if (item instanceof ActionContributionItem) {
//				IAction action = ((ActionContributionItem) item).getAction();
//				System.out.println("action: " + action.getActionDefinitionId() + " " + action.getId());
//			}
//			if (item instanceof IActionSetContributionItem) {
//				System.out.println("actionset: " + ((IActionSetContributionItem) item).getActionSetId());
//			}
//			if (item instanceof IMenuManager) {
//				walkMenuManager((IMenuManager) item, nPath);
//			}
//		}
//	}
}

