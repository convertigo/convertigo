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

import java.util.List;


public abstract class AbstractBase implements Comparable<Object>, Cloneable {
	public static final String prob_emptyName = "the name cannot be empty";
	
	String name = "";
	String description = "";
	boolean enable = true;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = (name == null ? "" : name);
	}
	
	public boolean isEnable() {
		return enable;
	}
	
	public void setEnable(boolean enable) {
		this.enable = enable;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = (description == null ? "" : description);
	}
	
	public int compareTo(Object obj) {
		if (obj instanceof AbstractBase && getName() != null) {
			return getName().compareTo(((AbstractBase)obj).getName());
		} else if (obj instanceof String) {
			return getName().compareTo(obj.toString());
		}
		return 1;
	}
	
	public void checkProblems(List<String> problems) {
		if (getName().length() == 0) {
			problems.add(prob_emptyName);
		}
	}
	
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " : " + name;
	}
}