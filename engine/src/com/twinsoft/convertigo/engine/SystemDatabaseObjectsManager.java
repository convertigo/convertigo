package com.twinsoft.convertigo.engine;

import java.io.File;
import java.util.Properties;

public class SystemDatabaseObjectsManager extends DatabaseObjectsManager {
	
	StudioProjects systemProjects = new StudioProjects() {
		
		@Override
		public File getProject(String projectName) {
			return null;
		}
		
	};
	
	@Override
	public void init() throws EngineException {
		super.init();
		
		importProject(Engine.WEBAPP_PATH + "/system/projects/lib_FlowViewer/c8oProject.yaml", false);
	}

	@Override
	protected void symbolsInit() {
		symbolsProperties = new Properties();
	}
	
	public StudioProjects getStudioProjects() {
		return systemProjects;
	}
}
