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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.3.x/Studio/src/com/twinsoft/convertigo/eclipse/popup/actions/TestCaseExecuteSelectedAction.java $
 * $Author: maximeh $
 * $Revision: 33944 $
 * $Date: 2013-04-05 18:29:40 +0200 (ven., 05 avr. 2013) $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.localbuild.BuildLocally;

public class BuildLocallyAction extends MyAbstractAction {
	
	private BuildLocally buildLocally = null;
	
	public BuildLocallyAction() {
		super();
	}

	@Override
	public void run() {
		String actionID = action.getId();
		Engine.logEngine.debug("Running " + actionID + " action");
		
		buildLocally = new BuildLocally(getMobilePlatform(), getParentShell());
		
		if (actionID.equals("convertigo.action.buildLocallyRelease")){
			buildLocally.build("release", false, "");
		}
		
		if (actionID.equals("convertigo.action.buildLocallyDebug")){
			buildLocally.build("debug", false, "");
		}
		
		if (actionID.equals("convertigo.action.runLocally")){
			buildLocally.build("debug", true, "device");
		}
		
		if (actionID.equals("convertigo.action.emulateLocally")){
			buildLocally.build("debug", true, "emulator");
		}
		
		if (actionID.equals("convertigo.action.removeCordovaDirectory")){
			buildLocally.removeCordovaDirectory();
		}
	}

	private MobilePlatform getMobilePlatform() {
		ProjectExplorerView explorerView = getProjectExplorerView();
		
		if (explorerView != null) {
			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
			Object databaseObject = treeObject.getObject();

			if ((databaseObject != null) && (databaseObject instanceof MobilePlatform)) {
				 return (MobilePlatform) treeObject.getObject();
			} 
		}
		return null;
	}
}
