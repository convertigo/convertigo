/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import org.eclipse.jface.viewers.*;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

/** Workbench commands and properties see each real target once, while the viewer retains occurrence selection. */
final class TagSelectionProvider implements ISelectionProvider {
	private final ISelectionProvider viewer;
	private final Map<ISelectionChangedListener, ISelectionChangedListener> listeners = new HashMap<>();
	TagSelectionProvider(ISelectionProvider viewer) { this.viewer = viewer; }
	static ISelection canonical(ISelection selection) {
		if (!(selection instanceof IStructuredSelection structured)) return selection;
		var targets = new LinkedHashSet<Object>();
		for (Object element : structured.toArray()) targets.add(element instanceof TreeObject tree ? tree.check() : element);
		return new StructuredSelection(targets.toArray());
	}
	@Override public void addSelectionChangedListener(ISelectionChangedListener listener) {
		ISelectionChangedListener adapter = event -> listener.selectionChanged(new SelectionChangedEvent(this, canonical(event.getSelection())));
		listeners.put(listener, adapter); viewer.addSelectionChangedListener(adapter);
	}
	@Override public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		var adapter = listeners.remove(listener); if (adapter != null) viewer.removeSelectionChangedListener(adapter);
	}
	@Override public ISelection getSelection() { return canonical(viewer.getSelection()); }
	@Override public void setSelection(ISelection selection) { viewer.setSelection(selection); }
}
