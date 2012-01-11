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

package com.twinsoft.convertigo.engine.dbo_explorer;

import java.util.Collection;
import java.util.Collections;

public class DboBean {

	private final String className;
	private final boolean bEnable;
	private final boolean bDocumented;
	private final boolean bDefault;
	private final Collection<DboParent> parents;
	private final Collection<String> emulatorTechnologies;

	public DboBean(String className, boolean bEnable, boolean bDocumented, boolean bDefault,
			Collection<DboParent> parents, Collection<String> emulatorTechnologies) {
		this.className = className;
		this.bEnable = bEnable;
		this.bDocumented = bDocumented;
		this.bDefault = bDefault;
		this.parents = Collections.unmodifiableCollection(parents);
		this.emulatorTechnologies = Collections.unmodifiableCollection(emulatorTechnologies);
	}

	public String getClassName() {
		return className;
	}

	public Boolean isEnable() {
		return bEnable;
	}
	
	public Boolean isDocumented() {
		return bDocumented;
	}
	
	public Boolean isDefault() {
		return bDefault;
	}

	public Collection<DboParent> getParents() {
		return parents;
	}

	public Collection<String> getEmulatorTechnologies() {
		return emulatorTechnologies;
	}

}
