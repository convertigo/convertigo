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

package com.twinsoft.convertigo.engine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FilePropertyManager implements AbstractManager {	
	private Map<String, Counter> mutex;

	private class Counter{
		private long cpt = 0;
		
		private long inc() {
			return ++cpt;
		}
		
		private long dec() {
			return --cpt;
		}
		
		private boolean zero() {
			return cpt == 0;
		}
	}

	synchronized public void init() throws EngineException {
		mutex = new HashMap<String, Counter>();
	}
	
	synchronized public void destroy() throws EngineException {
		if (mutex != null) {
			mutex.clear();
		}
		mutex = null;
	}

	public String getFilepathFromProperty(String filepath, String projectName){
		if (!filepath.matches(".*[/\\\\].*")) {
			filepath = ".//"+filepath;
		}
		if (filepath.startsWith(".//")) {
			filepath = Engine.PROJECTS_PATH + "/" + projectName + filepath.substring(2);
		} else if(filepath.startsWith("./")) {
			filepath = Engine.USER_WORKSPACE_PATH + filepath.substring(1);
		}
		return filepath;
	}
	
	public File getFileFromProperty(String filepath, String projectName) {
		return new File(getFilepathFromProperty(filepath, projectName)); 
	}
	
	synchronized public Object getMutex(String filepath) {
		Counter cpt = mutex.get(filepath);
		if (cpt == null) {
			mutex.put(filepath, cpt = new Counter());
		}
		cpt.inc();
		return cpt;
	}
	
	synchronized public Object getMutex(File file) {
		return getMutex(file.getPath());
	}
	
	synchronized public void releaseMutex(String filepath) {
		Counter cpt = mutex.get(filepath);
		if (cpt != null) {
			cpt.dec();
			if (cpt.zero()) {
				mutex.remove(filepath);
			}
		}
	}
	
	synchronized public void releaseMutex(File file){
		releaseMutex(file.getPath());
	}
}