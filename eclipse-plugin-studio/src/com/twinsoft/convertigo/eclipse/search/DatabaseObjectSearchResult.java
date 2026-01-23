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

package com.twinsoft.convertigo.eclipse.search;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;

public class DatabaseObjectSearchResult extends AbstractTextSearchResult {
	private DatabaseObjectSearchQuery query;
	private List<String> results = new ArrayList<>();

	public DatabaseObjectSearchResult(DatabaseObjectSearchQuery query) {
		this.query = query;
	}
	
	public void clear() {
		results.clear();
	}
	
	public void addResult(String result) {
		results.add(result);
		addMatch(new Match(result, 0, 0));
	}

	@Override
	public ISearchQuery getQuery() {
		return query;
	}

	@Override
	public Object[] getElements() {
		return results.toArray();
	}

	@Override
	public String getLabel() {
		return (query.isSearching() ? "Searching: " :  "Results: ") + query.getLabel() + " [" + results.size() + " found]";
	}

	@Override
	public String getTooltip() {
		return null;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public IEditorMatchAdapter getEditorMatchAdapter() {
		return null;
	}

	@Override
	public IFileMatchAdapter getFileMatchAdapter() {
		return null;
	}
}
