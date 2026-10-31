/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.beans.flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.twinsoft.convertigo.beans.core.DatabaseObject;

/** The virtual children projected for a key (sources and cache generation), read and replaced as one value. */
record FlowVirtualChildren(String key, List<DatabaseObject> children) {
	FlowVirtualChildren {
		children = Collections.unmodifiableList(new ArrayList<>(children));
	}
}
