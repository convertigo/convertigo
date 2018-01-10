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
