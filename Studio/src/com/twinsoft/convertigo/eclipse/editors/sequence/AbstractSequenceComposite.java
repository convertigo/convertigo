/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.editors.sequence;

import java.util.EventObject;

import javax.swing.event.EventListenerList;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.SequenceEvent;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.editors.CompositeListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public abstract class AbstractSequenceComposite extends Composite implements ISelectionChangedListener {

	protected SequenceEditorPart sequenceEditorPart;
	protected Sequence sequence;
	
	protected ProjectExplorerView projectExplorerView = null;
	
	public AbstractSequenceComposite(SequenceEditorPart SequenceEditorPart, Sequence sequence, Composite parent, int style) {
		super(parent, style);
		this.sequenceEditorPart = SequenceEditorPart;
		this.sequence = sequence;
		
		// add ProjectExplorerView to the listeners of the composite sequence
		projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (projectExplorerView != null) {
			projectExplorerView.addSelectionChangedListener(this);
			addCompositeListener(projectExplorerView);
		}
		
		initialize();
	}

	protected void toolBarSetEnable(String toolItemId, boolean enable) {
		sequenceEditorPart.toolBarSetEnable(toolItemId, enable);
	}
	
	protected void toolBarSetSelection(String toolItemId, boolean select) {
		sequenceEditorPart.toolBarSetSelection(toolItemId, select);
	}

	protected boolean checkEventSource(EventObject event) {
		boolean isSourceFromSequence = false;
		Object source = event.getSource();
		if (event instanceof SequenceEvent) {
			if ((source instanceof Sequence) || (source instanceof Step)) {
				Sequence sequence = null;
				if (source instanceof Sequence) sequence = (Sequence)source;
				if (source instanceof Step) sequence = ((Step)source).getParentSequence();
				if ((sequence != null) && (sequence.equals(this.sequence) || sequence.getOriginal().equals(this.sequence)))
					isSourceFromSequence = true;
			}
		}
		return isSourceFromSequence;
	}
	
	protected abstract void initialize();
	
	public abstract void initSequence(Sequence sequence);
	
	protected abstract void clearContent();

	/**
	 * Called when editor part is about to be closed.
	 * May be overidden to process garbage collect.
	 */
	public void close() {
		// Remove ProjectExplorerView from listeners current composite view
		if (projectExplorerView != null) {
			projectExplorerView.removeSelectionChangedListener(this);
			removeCompositeListener(projectExplorerView);
		}
	}
	
	
	/**
	 * Handles tree view selection
	 */
	
	public void selectionChanged(SelectionChangedEvent event) {
		if (event.getSource() instanceof ISelectionProvider) {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			TreeObject treeObject = (TreeObject) selection.getFirstElement();
			if (treeObject != null) {
			}
		}
	}
	
	/**
	 * Notify changes for tree view 
	 */
	
	private EventListenerList compositeListeners = new EventListenerList();
    
    public void addCompositeListener(CompositeListener compositeListener) {
    	compositeListeners.add(CompositeListener.class, compositeListener);
    }
    
    public void removeCompositeListener(CompositeListener compositeListener) {
    	compositeListeners.remove(CompositeListener.class, compositeListener);
    }
    
    public void fireObjectSelected(CompositeEvent compositeEvent) {
        // Guaranteed to return a non-null array
        Object[] listeners = compositeListeners.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2 ; i >= 0 ; i-=2) {
        	if (listeners[i] == CompositeListener.class) {
                ((CompositeListener) listeners[i+1]).objectSelected(compositeEvent);
            }
        }
    }
	
    public void fireObjectChanged(CompositeEvent compositeEvent) {
        // Guaranteed to return a non-null array
        Object[] listeners = compositeListeners.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2 ; i >= 0 ; i-=2) {
        	if (listeners[i] == CompositeListener.class) {
                ((CompositeListener) listeners[i+1]).objectChanged(compositeEvent);
            }
        }
    }
    
}
