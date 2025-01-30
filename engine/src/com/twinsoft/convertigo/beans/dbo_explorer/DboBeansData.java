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

package com.twinsoft.convertigo.beans.dbo_explorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DboBeansData {

	private String name;
	private List<DboBeanData> dboBeanDataList;

	public DboBeansData(String name) {
		this.name = name;
		dboBeanDataList = new ArrayList<>();
	}

	public List<DboBeanData> getAllDboBean() {
		return getAllDboBean(false);
	}

	public List<DboBeanData> getAllDboBean(boolean sort) {
		if (sort) {
			Collections.sort(dboBeanDataList, new Comparator<DboBeanData>() {
				@Override
				public int compare(DboBeanData o1, DboBeanData o2) {
	                String name1 = o1.getDisplayName().toLowerCase();
	                String name2 = o2.getDisplayName().toLowerCase();
					return name1.compareTo(name2);
				}
			});
		}

		return dboBeanDataList;
	}

	public void addDboBean(DboBeanData dboBean) {
		dboBeanDataList.add(dboBean);
	}

	public String getName() {
		return dboBeanDataList.size() > 1 && !name.endsWith("s") ? name + "s" : name;
	}

}
