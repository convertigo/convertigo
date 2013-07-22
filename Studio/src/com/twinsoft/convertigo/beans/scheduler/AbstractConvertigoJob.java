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

package com.twinsoft.convertigo.beans.scheduler;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.twinsoft.convertigo.engine.enums.Parameter;

public abstract class AbstractConvertigoJob extends AbstractJob {
	public static final String prob_noProject = "the project name cannot be empty";
	
	String projectName = null;
	private String contextName = "";
	Map<String, String> parameters = new TreeMap<String, String>();
	private boolean writeOutput = false;
	
	public String getProjectName() {
		return projectName;
	}
	
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	
	public String getContextName() {
		return contextName;
	}
	
	public void setContextName(String contextName) {
		this.contextName = contextName;
	}
	
	public Map<String, String> getParameters() {
		return parameters;
	}
	
	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}
	
	public boolean isWriteOutput() {
		return writeOutput;
	}
	
	public void setWriteOutput(boolean writeOutput) {
		this.writeOutput = writeOutput;
	}
	
	protected void addParameters(Map<String, String> parameters) {
		if (contextName != null && contextName.length() > 0) {
			parameters.put(Parameter.Context.getName(), contextName);
		}
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			parameters.put(entry.getKey(), entry.getValue());
		}
	}
	
	public String getConvertigoURL() {
		Map<String, String> parameters = new LinkedHashMap<String, String>();
		addParameters(parameters);
		parameters.putAll(this.parameters);
		StringBuffer sb = new StringBuffer();
		sb.append("/projects/").append(projectName).append("/.xml?");
		for (Entry<String, String> entry : parameters.entrySet()) {
			try {
				sb.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append('=').append(URLEncoder.encode(entry.getValue(), "UTF-8")).append('&');
			} catch (UnsupportedEncodingException e) {
				// improbable case
			}
		}
		return sb.substring(0, sb.length() - 1); // suppress the last & or ?
	}
	
	@Override
	public void checkProblems(List<String> problems) {
		super.checkProblems(problems);
		if (projectName == null || projectName.length() == 0) {
			problems.add(prob_noProject);
		}
	}
}