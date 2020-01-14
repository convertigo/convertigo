/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.beans.mobile.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;

public class MobileSmartSource {

	public static Pattern listenPattern = Pattern.compile("listen\\(\\['(.*)'\\]\\)(.+)?");
	public static Pattern directivePattern = Pattern.compile("(item\\d+)(.+)?");
	public static Pattern formPattern = Pattern.compile("(form\\d+)(.+)?");
	public static Pattern cafPattern = Pattern.compile("'([^,]+)(,.+)?'");
	public static Pattern globalPattern = Pattern.compile("(router\\.sharedObject)(.+)?");
	
	public enum Filter {
		Sequence,
		Database,
		Iteration,
		Form,
		Global;
	}
	
	public enum Key {
		filter,
		project,
		input
	}
	
	private JSONObject jsonObject = new JSONObject();
	
	public MobileSmartSource(Filter filter, String projectName, String input) {
		try {
			jsonObject.put(Key.filter.name(), filter.name());
			jsonObject.put(Key.project.name(), projectName);
			jsonObject.put(Key.input.name(), input);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	protected MobileSmartSource(String jsonString) {
		try {
			jsonObject = new JSONObject(jsonString);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Filter getFilter() {
		try {
			return Filter.valueOf(jsonObject.getString(Key.filter.name()));
		} catch (JSONException e) {
			//e.printStackTrace();
		}
		return null;
	}

	public String getProjectName() {
		try {
			return jsonObject.getString(Key.project.name());
		} catch (JSONException e) {
			//e.printStackTrace();
		}
		return "";
	}

	public void setProjectName(String newName) {
		try {
			jsonObject.put(Key.project.name(), newName);
		} catch (JSONException e) {
			//e.printStackTrace();
		}
	}

	public String getInput() {
		try {
			return jsonObject.getString(Key.input.name());
		} catch (JSONException e) {
			//e.printStackTrace();
		}
		return "";
	}

	public String getValue() {
		String value = getInput();
		try {
			// remove parameters for caf listen
			int i = -1, j = -1;
			while ((i = value.indexOf(", {")) > 0) {
				if ((j = value.indexOf("}", i)) > 0) {
					value = value.substring(0, i) + value.substring(j+1);
				} else break;
			}
			
			// remove project names for fullsync
			if (Filter.Database.equals(getFilter())) {
				int z = 0;
				while ((i = value.indexOf("'fs://", z)) > 0) {
					z = i+("'fs://").length();
					if ((j = value.indexOf("'", z)) > 0) {
						String requestable = value.substring(z, j);
						String[] ar = requestable.split("\\.");
						if (ar.length > 2) {
							String projectName = ar[0];
							value = value.replaceFirst("fs://"+projectName+"\\.", "fs://");
						}
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return value;
	}
	
	public String toJsonString() {
		try {
			return jsonObject.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public List<String> getSources() {
		return getSources(getInput());
	}
	
	public List<String> getSources(String input) {
		List<String> sources = new ArrayList<String>();
		if (Filter.Iteration.equals(getFilter())) {
			Matcher m = directivePattern.matcher(input);
			if (m.find()) {
				String directive = m.group(1);
				if (directive != null) {
					sources.add(directive);
				}
			}
		} else if (Filter.Form.equals(getFilter())) {
			Matcher m = formPattern.matcher(input);
			if (m.find()) {
				String form = m.group(1);
				if (form != null) {
					sources.add(form);
				}
			}
		} else if (Filter.Global.equals(getFilter())) {
			Matcher m = globalPattern.matcher(input);
			if (m.find()) {
				String gbl = m.group(1);
				if (gbl != null) {
					sources.add(gbl);
				}
			}
		} else {
			Matcher m = listenPattern.matcher(input);
			if (m.find()) {
				String array = m.group(1);
				if (array != null) {
					List<String> items = Arrays.asList(array.split("'\\s*,\\s*'"));
					for (String s : items) {
						sources.add("'"+s.replaceFirst("#[^,]*,,", "")+"'");
					}
				}
			}
		}
		return sources;
	}
	
	public String getModelPath() {
		String modelPath = null;
		if (Filter.Iteration.equals(getFilter())) {
			Matcher m = directivePattern.matcher(getInput());
			if (m.find()) {
				modelPath = m.group(2);
			}
		} else if (Filter.Form.equals(getFilter())) {
			Matcher m = formPattern.matcher(getInput());
			if (m.find()) {
				modelPath = m.group(2);
			}
		} else if (Filter.Global.equals(getFilter())) {
			Matcher m = globalPattern.matcher(getInput());
			if (m.find()) {
				modelPath = m.group(2);
			}
		} else {
			Matcher m = listenPattern.matcher(getInput());
			if (m.find()) {
				modelPath = m.group(2);
			}
		}
		return modelPath == null ? "" : modelPath;
	}
	
	public static MobileSmartSource valueOf(String jsonString) {
		if (jsonString != null && !jsonString.isEmpty()) {
			try {
				return new MobileSmartSource(jsonString);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public DatabaseObject getDatabaseObject(String dboName) {
		List<String> sourceData = getSources();
		String cafInput = sourceData.size() > 0 ? sourceData.get(0):null;
		if (cafInput != null) {
			if (Filter.Iteration.equals(getFilter())) {
				Matcher m = directivePattern.matcher(cafInput);
				if (m.find()) {
					String item = m.group(1);
					try {
						final long priority = Long.valueOf(item.replaceFirst("item", ""), 10);
						String projectName = getProjectName();
						Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
						
						DatabaseObject dbo = null;
						try {
							dbo = project.getMobileApplication().getApplicationComponent().getPageComponentByName(dboName);
						} catch (Exception e1) {
							try {
								dbo = project.getMobileApplication().getApplicationComponent().getMenuComponentByName(dboName);
							} catch (Exception e2) {}
						}
						
						final List<UIControlDirective> directiveList = new ArrayList<UIControlDirective>();
						new WalkHelper() {
							@Override
							protected void walk(DatabaseObject databaseObject) throws Exception {
								if (databaseObject instanceof UIControlDirective && databaseObject.priority == priority) {
									directiveList.add((UIControlDirective)databaseObject);
								}
								if (directiveList.isEmpty()) {
									super.walk(databaseObject);
								}
							}
						}.init(dbo);
						
						return directiveList.isEmpty() ? null:directiveList.get(0);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else if (Filter.Form.equals(getFilter())) {
				Matcher m = formPattern.matcher(cafInput);
				if (m.find()) {
					String form = m.group(1);
					try {
						final long priority = Long.valueOf(form.replaceFirst("form", ""), 10);
						String projectName = getProjectName();
						Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
						
						DatabaseObject dbo = null;
						try {
							dbo = project.getMobileApplication().getApplicationComponent().getPageComponentByName(dboName);
						} catch (Exception e1) {
							try {
								dbo = project.getMobileApplication().getApplicationComponent().getMenuComponentByName(dboName);
							} catch (Exception e2) {}
						}
						
						final List<UIForm> formList = new ArrayList<UIForm>();
						new WalkHelper() {
							@Override
							protected void walk(DatabaseObject databaseObject) throws Exception {
								if (databaseObject instanceof UIForm && databaseObject.priority == priority) {
									formList.add((UIForm)databaseObject);
								}
								if (formList.isEmpty()) {
									super.walk(databaseObject);
								}
							}
						}.init(dbo);
						
						return formList.isEmpty() ? null:formList.get(0);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else if (Filter.Global.equals(getFilter())) {
				Matcher m = formPattern.matcher(cafInput);
				if (m.find()) {
					try {
						String projectName = getProjectName();
						Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
						
						DatabaseObject dbo = project.getMobileApplication().getApplicationComponent();
						return dbo;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			} else if (Filter.Database.equals(getFilter())) {
				Matcher m = cafPattern.matcher(cafInput);
				if (m.find()) {
					try {
						String name = m.group(1);
						name = name.replaceFirst("fs://", "");
						name = name.replaceFirst("\\.view", "");
						name = name.replaceFirst("\\.get", "");
						int i = name.indexOf('#');
						if (i != -1) {
							name = name.substring(0, i);
						}
						
						int index = name.indexOf('.');
						String projectName = index != -1 ? name.substring(0, index) : getProjectName();
						String dbName = index != -1 ? name.substring(index+1) : name;
						
						Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
						FullSyncConnector connector = (FullSyncConnector) project.getConnectorByName(dbName);
						String documentName = getParameters().get("ddoc");
						DesignDocument ddoc = (DesignDocument) connector.getDocumentByName(documentName);
						return ddoc;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else if (Filter.Sequence.equals(getFilter())) {
				Matcher m = cafPattern.matcher(cafInput);
				if (m.find()) {
					try {
						String name = m.group(1);
						int i = name.indexOf('#');
						if (i != -1) {
							name = name.substring(0, i);
						}
						
						int index = name.indexOf('.');
						String projectName = index != -1 ? name.substring(0, index) : getProjectName();
						projectName = projectName.isEmpty() ? getProjectName(): projectName;
						String sequenceName = index != -1 ? name.substring(index+1) : name;
						
						Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
						Sequence sequence = project.getSequenceByName(sequenceName);
						return sequence;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}

	public Map<String, String> getParameters() {
		Map<String, String> map = new HashMap<String, String>();
		List<String> sourceData = getSources();
		String cafInput = sourceData.size() > 0 ? sourceData.get(0):null;
		if (cafInput != null) {
			if (Filter.Iteration.equals(getFilter())) {
				;
			} else if (Filter.Form.equals(getFilter())) {
				;
			} else if (Filter.Global.equals(getFilter())) {
				;
			} else {
				Matcher m = cafPattern.matcher(cafInput);
				if (m.find()) {
					String parameters = m.group(2);
					if (parameters != null) {
						parameters = parameters.replaceFirst("\\{", "");
						parameters = parameters.replaceFirst("\\}", "");
						String[] params = parameters.split(",");
						for (int i=0; i< params.length; i++) {
							String param = params[i];
							if (param.indexOf('=') != -1) {
								String[] values = param.split("=");
								String key = values[0].trim();
								if (!key.isEmpty()) {
									String value = values[1].trim();
									if (value.startsWith("'") && value.endsWith("'")) {
										value = value.substring(1, value.length()-1);
									}
									map.put(key, value);
								}
							}
						}
					}
				}
			}
		}
		return map;
	}
	
}
