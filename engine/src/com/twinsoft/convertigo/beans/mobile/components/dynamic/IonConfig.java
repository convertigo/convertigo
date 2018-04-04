/*
 * Copyright (c) 2001-2018 Convertigo SA.
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

package com.twinsoft.convertigo.beans.mobile.components.dynamic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class IonConfig implements Cloneable {
	
	enum Key {
		action_ts_imports,
		module_ts_imports,
		module_ng_imports,
		module_ng_providers,
		module_ng_declarations,
		module_ng_components,
		package_dependencies,
		cordova_plugins,
		;
	}
	
	private JSONObject jsonConfig;
	
	public IonConfig() {
		jsonConfig = new JSONObject();
	}

	public IonConfig(JSONObject jsonOb) {
		this();
		for (Key k: Key.values()) {
			if (jsonOb.has(k.name())) {
				try {
					jsonConfig.put(k.name(), jsonOb.get(k.name()));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public JSONObject getJSONObject() {
		return jsonConfig;
	}
	
	public Map<String, List<String>> getActionTsImports() {
		return getTsImports(Key.action_ts_imports);
	}
	
	public Map<String, List<String>> getModuleTsImports() {
		return getTsImports(Key.module_ts_imports);
	}
	
	public Set<String> getModuleNgImports() {
		return getNgSet(Key.module_ng_imports);
	}
	
	public Set<String> getModuleNgProviders() {
		return getNgSet(Key.module_ng_providers);
	}
	
	public Set<String> getModuleNgDeclarations() {
		return getNgSet(Key.module_ng_declarations);
	}

	public Set<String> getModuleNgComponents() {
		return getNgSet(Key.module_ng_components);
	}

	public Map<String, String> getPackageDependencies() {
		return getCfgImports(Key.package_dependencies, "package", "version");
	}
	

	public Map<String, String> getConfigPlugins() {
		return getCfgPlugins(Key.cordova_plugins, "plugin");
	}
	
	protected Map<String, List<String>> getTsImports(Key key) {
		try {
			Map<String, List<String>> map = new HashMap<String, List<String>>();
			JSONArray ar = jsonConfig.getJSONArray(key.name());
			for (int i=0; i<ar.length(); i++) {
				Object ob = ar.get(i);
				if (ob instanceof JSONObject) {
					JSONObject jsonImport = (JSONObject)ob;
					String from = jsonImport.getString("from");
					if (!from.isEmpty()) {
						List<String> list = map.get(from);
						if (list == null) {
							list = new ArrayList<String>();
						}
						
						JSONArray arc = jsonImport.getJSONArray("components");
						for (int j=0; j<arc.length(); j++) {
							String s = arc.getString(j);
							if (!s.isEmpty()) {
								list.add(s);
							}
						}
						
						map.put(from, list);
					}
				}
			}
			return map;
		} catch (JSONException e) {
			return new HashMap<String, List<String>>();
		}
	}
	
	protected Set<String> getNgSet(Key key) {
		try {
			Set<String> set = new HashSet<String>();
			JSONArray ar = jsonConfig.getJSONArray(key.name());
			for (int i=0; i<ar.length(); i++) {
				String s = ar.getString(i);
				if (!s.isEmpty()) {
					set.add(s);
				}
			}
			return set;
		} catch (JSONException e) {
			return new HashSet<String>();
		}
	}
	
	protected Map<String, String> getCfgPlugins(Key key, String keyId) {
		try {
			Map<String, String> map = new HashMap<String, String>();
			JSONArray ar = jsonConfig.getJSONArray(key.name());
			for (int i=0; i<ar.length(); i++) {
				Object ob = ar.get(i);
				if (ob instanceof JSONObject) {
					JSONObject jsonImport = (JSONObject)ob;
					String plugin = jsonImport.getString(keyId);
					if (!plugin.isEmpty() && !jsonImport.toString().isEmpty()) {
						map.put(plugin, jsonImport.toString());
					}
				}
			}
			return map;
		} catch (JSONException e) {
			return new HashMap<String, String>();
		}
	}
	
	protected Map<String, String> getCfgImports(Key key, String key1, String key2) {
		try {
			Map<String, String> map = new HashMap<String, String>();
			JSONArray ar = jsonConfig.getJSONArray(key.name());
			for (int i=0; i<ar.length(); i++) {
				Object ob = ar.get(i);
				if (ob instanceof JSONObject) {
					JSONObject jsonImport = (JSONObject)ob;
					String val1 = jsonImport.getString(key1);
					String val2 = jsonImport.getString(key2);
					if (!val1.isEmpty() && !val2.isEmpty()) {
						map.put(val1, val2);
					}
				}
			}
			return map;
		} catch (JSONException e) {
			return new HashMap<String, String>();
		}
	}
	
	public String toString() {
		return jsonConfig.toString();
	}
}
