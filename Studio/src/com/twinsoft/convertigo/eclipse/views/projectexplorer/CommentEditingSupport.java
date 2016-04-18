package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;

public class CommentEditingSupport extends EditingSupport {

	CellEditor cellEditor;
	
	public CommentEditingSupport(TreeViewer viewer) {
		super(viewer);
		cellEditor = new TextCellEditor(viewer.getTree());
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
		return element instanceof DatabaseObjectTreeObject;
	}

}
