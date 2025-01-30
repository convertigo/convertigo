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

import com.twinsoft.convertigo.engine.util.GenericUtils;

public class StudioEvent implements BaseEvent {
	private String type;
	private Object payload;
	
	public StudioEvent(String type, Object payload) {
		this.type = type;
		this.payload = payload;
	}
	
	public String type() {
		return type;
	}

	public <T> T payload() {
		return GenericUtils.cast(payload);
	}
}
