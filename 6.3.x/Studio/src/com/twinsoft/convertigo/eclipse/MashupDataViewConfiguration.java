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

package com.twinsoft.convertigo.eclipse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MashupDataViewConfiguration implements Serializable {

	private static final long serialVersionUID = -6288005285115715932L;

	private List<String> dataviews = new ArrayList<String>();
	
	public MashupDataViewConfiguration() {
		
	}

	public void addDataView(String name) {
		if (!dataviews.contains(name))
			dataviews.add(name);
	}
	
	public void removeDataView(String name) {
		if (dataviews.contains(name))
			dataviews.remove(name);
	}

	public List<String> getDataViews() {
		return dataviews;
	}
	
	public void setDataViews(List<String> dataviews) {
		this.dataviews = dataviews;
	}
}
