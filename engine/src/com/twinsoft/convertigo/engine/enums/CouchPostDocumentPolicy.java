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

package com.twinsoft.convertigo.engine.enums;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;

public enum CouchPostDocumentPolicy {
	none,
	create,
	override,
	merge;
	
	public Map<List<String>, String> mergeRules(String rules) {
		Map<List<String>, String> mergeRules = this == CouchPostDocumentPolicy.merge ? new HashMap<>() : null;
		if (mergeRules != null) {
			try {
				JSONObject json = new JSONObject(rules);
				String separator = ".";
				if (json.has("_separator")) {
					separator = json.getString("_separator");
				}
				for (java.util.Iterator<?> i = json.keys(); i.hasNext();) {
					String key = (String) i.next();
					if (key.equals("_separator")) {
						continue;
					}
					String[] path = key.split(Pattern.quote(separator));
					mergeRules.put(Arrays.asList(path), json.getString(key));
				}
			} catch (Exception e) {
				Engine.logEngine.error("failed to get merge policies", e);
			}
		}
		return mergeRules;
	}
}
