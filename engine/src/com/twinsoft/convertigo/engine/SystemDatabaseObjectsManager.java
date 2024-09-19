/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.engine;

import java.io.File;
import java.util.Properties;

class SystemDatabaseObjectsManager extends DatabaseObjectsManager {
	
	private StudioProjects systemProjects = new StudioProjects() {
		
		@Override
		public File getProject(String projectName) {
			return null;
		}

		@Override
		public void reloadProject(String name) throws EngineException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void renameProject(String oldName, String newName) {
			// TODO Auto-generated method stub
			
		}
		
	};
	
	@Override
	public void init() throws EngineException {
		super.init();
		
//		try {
//			importProject(Engine.WEBAPP_PATH + "/system/projects/lib_FlowViewer/c8oProject.yaml", false);
//		} catch (Exception e) {
//			Engine.logEngine.fatal("(SystemDatabaseObjectsManager) Failed to load system lib_FlowViewer", e);
//		}
	}

	@Override
	protected void symbolsInit() {
		symbolsProperties = new Properties();
	}
	
	public StudioProjects getStudioProjects() {
		return systemProjects;
	}
}
