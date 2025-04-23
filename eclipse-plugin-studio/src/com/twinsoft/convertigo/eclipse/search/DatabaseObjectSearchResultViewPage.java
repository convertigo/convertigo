package com.twinsoft.convertigo.eclipse.search;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.graphics.Image;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewLabelProvider;
import com.twinsoft.convertigo.engine.Engine;

public class DatabaseObjectSearchResultViewPage extends AbstractTextSearchViewPage {
	private ColumnViewer viewer;

	void configureViewer(ColumnViewer viewer) {
		this.viewer = viewer;
		viewer.setContentProvider(new DatabaseObjectTreeContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider() {

			@Override
			public String getText(Object obj) {
				var txt = super.getText(obj);
				if (obj instanceof String qname) { 
					if (getInput() instanceof DatabaseObjectSearchResult r && r.getQuery() instanceof DatabaseObjectSearchQuery q) {
						qname = q.doSubstring(qname);
					}
					txt = qname + " → " + txt;
				}
				return txt;
			}

			@Override
			public Image getImage(Object obj) {
				if (obj instanceof String qname) { 
					try {
						obj = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
					} catch (Exception e) {
					}
				}
				return super.getImage(obj);
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
		if (match.getElement() instanceof String qname) {
			try {
				var dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);

				var pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
				if (pev == null) {
					return;
				}
				var node = pev.findTreeObjectByUserObject(dbo);
				if (node == null) {
					return;
				}
				pev.setSelectedTreeObject(node);
				var structuredSelection = new StructuredSelection(node);
				ConvertigoPlugin.getDefault().getPropertiesView().selectionChanged(getViewPart(), structuredSelection);
			} catch (Exception e) {
			}
		}
	}
}
