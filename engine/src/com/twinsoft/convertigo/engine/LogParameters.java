/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class LogParameters {

	private final Map<String, String> parametersMap = new TreeMap<String, String>();

	public synchronized void clear() {
		parametersMap.clear();
	}
	
	@JsonAnySetter
	public synchronized void put(String key, Object value) {
		parametersMap.put(key, value == null ? null : value.toString());
	}

	@JsonAnyGetter
	public synchronized Map<String, String> asMap() {
		return new TreeMap<String, String>(parametersMap);
	}
	
	@Override
	public synchronized Object clone() {
		return this;
	}
	
	@Override
    public synchronized String toString() {
		String logParameters = "";
		
		for (String key : parametersMap.keySet()) {
			Object value = parametersMap.get(key);
			logParameters += "$" + key + "=" + value + " | ";
		}
		
		return logParameters;		
	}
	
}
