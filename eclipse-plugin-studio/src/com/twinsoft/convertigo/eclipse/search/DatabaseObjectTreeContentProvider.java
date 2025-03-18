package com.twinsoft.convertigo.eclipse.search;

import org.eclipse.jface.viewers.ITreeContentProvider;

public class DatabaseObjectTreeContentProvider implements ITreeContentProvider {
	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof DatabaseObjectSearchResult) {
			return ((DatabaseObjectSearchResult) inputElement).getElements();
		}
		return new Object[0];
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return false;
	}
}
