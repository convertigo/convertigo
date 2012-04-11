package com.twinsoft.convertigo.eclipse.views.references;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.eclipse.views.references.model.AbstractNode;

public class ViewRefContentProvider implements ITreeContentProvider {

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return ((AbstractNode) parentElement).getChildren().toArray();
	}

	@Override
	public Object getParent(Object element) {
		return ((AbstractNode) element).getParent();
	}

	@Override
	public boolean hasChildren(Object element) {
		return ((AbstractNode) element).hasChildren();
	}

}
