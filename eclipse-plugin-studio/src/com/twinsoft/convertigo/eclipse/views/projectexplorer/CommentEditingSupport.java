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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.jface.viewers.TreeViewer;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

class CommentEditingSupport extends EditingSupport {

	private CellEditor cellEditor;
	
	CommentEditingSupport(ProjectExplorerView explorerView, TreeViewer viewer) {
		super(viewer);
		cellEditor = new TextCellEditor(viewer.getTree()) {
			@Override
			protected Control createControl(Composite parent) {
				var control = super.createControl(parent);
				if (control instanceof Text text) {
					explorerView.setInlineEditingTextCtrl(text);
					control.addDisposeListener(event -> explorerView.clearInlineEditingTextCtrl(text));
				}
				return control;
			}

			@Override
			public void activate() {
				super.activate();
				if (getControl() instanceof Text text && !text.isDisposed()) {
					explorerView.setInlineEditingTextCtrl(text);
				}
			}

			@Override
			public void deactivate() {
				if (getControl() instanceof Text text) {
					explorerView.clearInlineEditingTextCtrl(text);
				}
				super.deactivate();
			}
		};
	}

	@Override
	protected void setValue(Object element, Object value) {
		if (canEdit(element)) {
			String comment = "" + ((DatabaseObjectTreeObject) element).getPropertyValue("comment");
			int i = comment.indexOf('\n');
			if (i != -1) {
				value = value + comment.substring(i);
			}
			((DatabaseObjectTreeObject) element).setPropertyValue("comment", value);
		}
	}
	
	@Override
	protected Object getValue(Object element) {
		if (canEdit(element)) {
			((DatabaseObjectTreeObject) element).isEditingComment = true;
			
			String comment = "" + ((DatabaseObjectTreeObject) element).getPropertyValue("comment");
			int i = comment.indexOf('\n');
			if (i != -1) {
				comment = comment.substring(0, i);
			}
			return comment;
		}
		return "";
	}
	
	@Override
	protected CellEditor getCellEditor(Object element) {
		return cellEditor;
	}
	
	@Override
	protected boolean canEdit(Object element) {
		if (element instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject) element;
			DatabaseObject dbo = databaseObjectTreeObject.getObject();
			IFolder folder = databaseObjectTreeObject.getProjectTreeObject().getFolder(
					"_private/editor/" + dbo.getShortQName() + "-comment");
			return !folder.exists();
		};
		return false;
	}

}
