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

package com.twinsoft.convertigo.eclipse.editors.connector;

import java.util.EventObject;

import javax.swing.event.EventListenerList;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Composite;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ConnectorEvent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.editors.CompositeListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ConnectorTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TransactionTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.EngineException;

public abstract class AbstractConnectorComposite extends Composite implements ISelectionChangedListener, ILearnable {

	protected ConnectorEditorPart connectorEditorPart;
	protected Connector connector;
	
	protected ProjectExplorerView projectExplorerView = null;
	
	public AbstractConnectorComposite(ConnectorEditorPart connectorEditorPart, Connector connector, Composite parent, int style) {
		super(parent, style);
		this.connectorEditorPart = connectorEditorPart;
		this.connector = connector;
		
		// add ProjectExplorerView to the listeners of the composite connector
		projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (projectExplorerView != null) {
			projectExplorerView.addSelectionChangedListener(this);
			addCompositeListener(projectExplorerView);
		}
		
		initialize();
	}

	protected void toolBarSetEnable(String toolItemId, boolean enable) {
		connectorEditorPart.toolBarSetEnable(toolItemId, enable);
	}
	
	protected void toolBarSetSelection(String toolItemId, boolean select) {
		connectorEditorPart.toolBarSetSelection(toolItemId, select);
	}

	protected Object getLastDetectedScreenClass() {
		return connectorEditorPart.getLastDetectedScreenClass();
	}
	
	protected boolean checkEventSource(EventObject event) {
		boolean isSourceFromConnector = false;
		Object source = event.getSource();
		if (event instanceof ConnectorEvent) {
			if (source instanceof DatabaseObject) {
				Connector connector = ((DatabaseObject)source).getConnector();
				if ((connector != null) && (connector.equals(this.connector)))
					isSourceFromConnector = true;
			}
		}
		return isSourceFromConnector;
	}
	
	protected abstract void clearContent();
	
	protected abstract void initialize();
	
	public abstract void initConnector(Transaction transaction);

	/**
	 * Renews the connector, i.e. recreates all internal structures
	 * in order to "restart" the connector.
	 */
	public abstract void renew();

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
	
	public void setAccumulate(boolean accumulate) {
		connector.setAccumulate(accumulate);
	}
	
	public void startLearn() {
		if (connector.isLearning())
			stopLearn();
		
		// set learning flag
		connector.markAsLearning(true);
		
		Transaction transaction = connector.getLearningTransaction();
		if (transaction == null) {
			if (projectExplorerView != null) {
				Object object = projectExplorerView.getFirstSelectedDatabaseObject();
				if ((object != null) && (object instanceof Transaction)) {
					try {
						transaction = ((Transaction)object);
						transaction.markAsLearning(true);
						ConvertigoPlugin.logDebug2("("+ connector.getClass().getName() +") start learning transaction named '"+ transaction.getName() +"'");
					}
					catch (Exception e) {}
				}
			}
		}
		toolBarSetEnable("Learn", true);
		toolBarSetSelection("Learn", true);
	}

	public void stopLearn() {
		if (!connector.isLearning())
			return;
		
		Transaction transaction = connector.getLearningTransaction();

		// unset learning flag
		connector.markAsLearning(false);
		ConvertigoPlugin.logDebug2("("+ connector.getClass().getName() +") stop learning transaction named '"+ transaction.getName() +"'");
		
		try {
			transaction.markAsLearning(false);
		} catch (EngineException e) {}
		
		toolBarSetEnable("Learn", false);
		toolBarSetSelection("Learn", false);
	}
	
	/**
	 * Handles tree view selection
	 */
	
	public void selectionChanged(SelectionChangedEvent event) {
		if (event.getSource() instanceof ISelectionProvider) {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			TreeObject treeObject = (TreeObject) selection.getFirstElement();
			if (treeObject != null) {
				if (ILearnable.class.isAssignableFrom(this.getClass()) && (!HttpConnectorComposite.class.equals(this.getClass()))) {
					ConnectorTreeObject connectorTreeObject = treeObject.getConnectorTreeObject();
					if (connectorTreeObject != null) {
						Connector connector =  (Connector)connectorTreeObject.getObject();
						if (connector.equals(this.connector)) {
							if (treeObject instanceof TransactionTreeObject) {
								if (!this.connector.isLearning())
									toolBarSetEnable("Learn", true);
							}
							else {
								if (!this.connector.isLearning())
									toolBarSetEnable("Learn", false);
							}
						}
						else {
							if (!this.connector.isLearning())
								toolBarSetEnable("Learn", false);
						}
					}
				}
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
