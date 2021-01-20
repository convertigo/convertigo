/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

/**
 * This class manages generic exception type for the Convertigo engine.
 */
public class VersionException extends EngineException {

	private static final long serialVersionUID = 8809647221714944333L;

	private String projectName;
	private String projectPath;
	private String version;

	public VersionException(String version) {
		super(getMessage(version, "n/a", "n/a"));
		this.version = version;
		projectName = "n/a";
		projectPath = "n/a";
	}
	
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	
	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath;
	}
	
	@Override
	public String getMessage() {
		return getMessage(version, projectName, projectPath);
	}
	
	@Override
	public String getLocalizedMessage() {
		return getMessage();
	}

	private static String getMessage(String version, String projectName, String projectPath) {
		return "Unable to load the project '" + projectName + "' for a Convertigo version " + version + " superior to the current Convertigo version "
				+ com.twinsoft.convertigo.beans.Version.version + ", from '" + projectPath + "'.";
	}
}


