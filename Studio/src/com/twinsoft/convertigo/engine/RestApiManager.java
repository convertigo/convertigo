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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.UrlMapper;

public class RestApiManager implements AbstractManager {

	private Map<String, UrlMapper> urlMapperMap = null;
	
	private static RestApiManager instance = null;
	
	private RestApiManager() {
		urlMapperMap = new LinkedHashMap<String, UrlMapper>();
	}
	
	public static RestApiManager getInstance() {
		if (instance == null) {
			instance = new RestApiManager();
		}
		return instance;
	}
	
	@Override
	public void init() throws EngineException {
	}

	@Override
	public void destroy() throws EngineException {
		urlMapperMap.clear();
		urlMapperMap = null;
		instance = null;
	}

	public void putUrlMapper(Project project) {
		if (project == null)
			return;
		
		String projectName = project.getName();
		UrlMapper urlMapper = project.getUrlMapper();
		
		synchronized (urlMapperMap) {
			if (urlMapper != null) {
				urlMapperMap.put(projectName, urlMapper);
			}
		}
	}
	
	public void removeUrlMapper(String projectName) {
		if (projectName == null)
			return;
		
		synchronized (urlMapperMap) {
			urlMapperMap.remove(projectName);
		}
	}
	
	public UrlMapper getUrlMapper(String projectName) {
		if (projectName == null)
			return null;
		
		UrlMapper urlMapper = null;
		synchronized (urlMapperMap) {
			urlMapper = urlMapperMap.get(projectName);
		}
		return urlMapper;
	}
	
	public Collection<UrlMapper> getUrlMappers() {
		Collection<UrlMapper> collection = null;
		synchronized (urlMapperMap) {
			collection = urlMapperMap.values();
		}
		return collection;
	}
}
