/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.engine.util;

import java.util.HashMap;
import java.util.Map;

public class SimpleMap {
	private Map<String, Object> map = new HashMap<>();
	
	public void set(String key, Object value) {
		synchronized (map) {
			if (value == null) {
				map.remove(key);
			} else {
				map.put(key, value);
			}
		}
	}
	
	public Object get(String key) {
		synchronized (map) {
			return map.get(key);
		}
	}
}
