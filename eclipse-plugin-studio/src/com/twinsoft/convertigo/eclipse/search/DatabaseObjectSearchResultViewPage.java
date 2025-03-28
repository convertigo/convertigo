package com.twinsoft.convertigo.eclipse.search;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewLabelProvider;

public class DatabaseObjectSearchResultViewPage extends AbstractTextSearchViewPage {
	private ColumnViewer viewer;

	void configureViewer(ColumnViewer viewer) {
		this.viewer = viewer;
		viewer.setContentProvider(new DatabaseObjectTreeContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider() {

			@Override
			public String getText(Object obj) {
				var txt = super.getText(obj);
				if (obj instanceof DatabaseObject dbo) {
					var qname = dbo.getFullQName(); 
					if (getInput() instanceof DatabaseObjectSearchResult r && r.getQuery() instanceof DatabaseObjectSearchQuery q) {
						qname = q.doSubstring(qname);
					}
					txt = qname + " → " + txt;
				}
				return txt;
			}

		});
		viewer.getControl().addDisposeListener(e -> viewer.getLabelProvider().dispose());
	}

	@Override
	protected void configureTableViewer(TableViewer viewer) {
		configureViewer(viewer);
	}

	@Override
	protected void configureTreeViewer(TreeViewer viewer) {
		configureViewer(viewer);
	}

	@Override
	protected void elementsChanged(Object[] objects) {
		viewer.refresh();
	}

	@Override
	protected void clear() {
		if (getInput() instanceof DatabaseObjectSearchResult r) {
			r.clear();
		}
		viewer.refresh();
	}

	@Override
	public void showMatch(Match match, int currentOffset, int currentLength, boolean activate) {
		if (match.getElement() instanceof DatabaseObject dbObject) {
			var pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
			pev.objectChanged(new CompositeEvent(dbObject));
			var node = pev.findTreeObjectByUserObject(dbObject);
			pev.setSelectedTreeObject(node);
			var structuredSelection = new StructuredSelection(node);
			ConvertigoPlugin.getDefault().getPropertiesView().selectionChanged(getViewPart(), structuredSelection);
		}
	}
}
