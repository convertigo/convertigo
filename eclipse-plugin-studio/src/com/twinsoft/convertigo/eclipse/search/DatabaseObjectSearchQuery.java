package com.twinsoft.convertigo.eclipse.search;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.YamlConverter;

public class DatabaseObjectSearchQuery implements ISearchQuery {
	private String searchString;
	private boolean matchCase;
	private boolean useRegExp;
	private String objectType;
	private DatabaseObjectSearchResult searchResult;
	private DatabaseObject root;
	private String rootQName;
	private boolean searching = true;


	public DatabaseObjectSearchQuery(DatabaseObject root, String searchString, boolean matchCase, boolean useRegExp, String objectType) {
		this.root = root;
		this.searchString = searchString;
		this.matchCase = matchCase;
		this.useRegExp = useRegExp;
		this.objectType = objectType;
		this.searchResult = new DatabaseObjectSearchResult(this);
		rootQName = root == null ? "workspace" : root.getFullQName();
	}

	@Override
	public String getLabel() {
		return (useRegExp ? "regular expression" : "substring") + " '" + searchString + "' from " + rootQName;
	}

	@Override
	public boolean canRerun() {
		return true;
	}

	@Override
	public boolean canRunInBackground() {
		return true;
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		IStatus[] status = {Status.OK_STATUS};
		try {
			searchResult.clear();
			searching = true;
			
			var substring = new String[]{searchString};
			var matcher = useRegExp ? 
					(matchCase ? Pattern.compile(searchString) : Pattern.compile(searchString, Pattern.CASE_INSENSITIVE)).matcher("") 
					: null;
			if (!matchCase) {
				substring[0] = substring[0].toLowerCase();
			}
			
			var dbom = Engine.theApp.databaseObjectsManager;
			var list = root != null ? Arrays.asList(root) : dbom.getAllProjectNamesList(true);
			
			for (var i: list) {
				if (monitor.isCanceled()) {
					status[0] = Status.CANCEL_STATUS;
					break;
				}
				
				var dbo = i instanceof String s ? dbom.getOriginalProjectByName(s, true) : (DatabaseObject) i;
				
				new WalkHelper() {
					@Override
					protected void walk(DatabaseObject databaseObject) throws Exception {
						if (monitor.isCanceled()) {
							status[0] = Status.CANCEL_STATUS;
							return;
						}
						var match = "*".equals(objectType) || databaseObject.getDatabaseType().equals(objectType);
						if (match) {
							String text;
							try {
								text = YamlConverter.toYaml(databaseObject.toXml(XMLUtils.createDom()));
							} catch (Exception e) {
								text = databaseObject.toString();
							}
							var found = false;
							if (useRegExp) {
								matcher.reset(text);
								found = matcher.find();
							} else {
								text = matchCase ? text : text.toLowerCase();
								found = text.contains(substring[0]);
							}

							if (found) {
								searchResult.addResult(databaseObject.getFullQName());
							}
						}
						monitor.worked(1);
						super.walk(databaseObject);
					}
				}.init(dbo);
			}
		} catch (Exception e) {
			ConvertigoPlugin.errorMessageBox(e.getClass().getName() + ":\n" + e.getMessage());
			status[0] = Status.CANCEL_STATUS;
		}
		searching = false;
		return status[0];
	}

	@Override
	public ISearchResult getSearchResult() {
		return searchResult;
	}
	
	public String doSubstring(String txt) {
		if (root == null || txt.length() < rootQName.length()) {
			return txt;
		}
		return "…" + txt.substring(rootQName.length());
	}
	
	public boolean isSearching() {
		return searching;
	}
}
