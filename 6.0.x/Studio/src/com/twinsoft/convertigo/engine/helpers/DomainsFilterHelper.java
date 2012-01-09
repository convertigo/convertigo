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

package com.twinsoft.convertigo.engine.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IDomainsFilterContainer;

public class DomainsFilterHelper {
	private static class ListEntry {
		private Pattern regexPattern;
		private ListType type;
		
		enum ListType {
			whiteList,
			blackList;
			
			boolean isAllowed() {
				return (this == whiteList) ? true : false;
			}
			
			static ListType get(boolean blacklisted) {
				return blacklisted ? blackList : whiteList;
			}
		}
		
		ListEntry(String regexp, String value) {
			this.regexPattern = Pattern.compile(regexp);
			this.type = ListType.get(Boolean.parseBoolean(value));
		}

		boolean isMatching(String url) {
			return regexPattern.matcher(url).find();
		}
		
		boolean isAllowed() {
			return type.isAllowed(); 
		}
	}
	
	private List<ListEntry> entries = null;
	private  IDomainsFilterContainer domainsFilter = null;
	
	public DomainsFilterHelper(IDomainsFilterContainer domainsFilter) {
		this.domainsFilter = domainsFilter;
	}
	
	public boolean shouldRewrite(String url) {
		if (url == null || url.equals("")) {
			return false;
		}
		for (ListEntry entry: getEntriesList()) {
			if (entry.isMatching(url)) {
				return entry.isAllowed();
			}
		}
		return true;
	}
	
	public void reset() {
		entries = null;
	}
	
	private List<ListEntry> getEntriesList() {
		if (entries == null) {
			XMLVector<XMLVector<String>> domainsListing = domainsFilter.getDomainsListing();
			entries = new ArrayList<ListEntry>(domainsListing.size());
			for (XMLVector<String> xmlv : domainsListing) {
				ListEntry entry = new ListEntry(xmlv.get(0), xmlv.get(1));
				entries.add(entry);
			}
		}
		return entries;
	}
}
