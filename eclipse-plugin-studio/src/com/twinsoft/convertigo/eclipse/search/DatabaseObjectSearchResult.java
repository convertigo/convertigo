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
