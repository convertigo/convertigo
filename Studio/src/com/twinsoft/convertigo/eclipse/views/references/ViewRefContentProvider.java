package com.twinsoft.convertigo.eclipse.views.references;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.eclipse.views.references.model.AbstractNode;

public class ViewRefContentProvider implements ITreeContentProvider {

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public Object[] getChildren(Object parentElement) {
		return ((AbstractNode) parentElement).getChildren().toArray();
	}

	public Object getParent(Object element) {
		return ((AbstractNode) element).getParent();
	}

	public boolean hasChildren(Object element) {
		return ((AbstractNode) element).hasChildren();
	}

}
