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

package com.twinsoft.convertigo.engine.events;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;

public class PropertyChangeEvent implements BaseEvent {
	private EnginePropertiesManager.PropertyName key;
	private String value;
	
	public PropertyChangeEvent(EnginePropertiesManager.PropertyName key, String value) {
		this.key = key;
		this.value = value;
	}

	public EnginePropertiesManager.PropertyName getKey(){
		return key;
	}
	
	public String getValue(){
		return value;
	}
}
