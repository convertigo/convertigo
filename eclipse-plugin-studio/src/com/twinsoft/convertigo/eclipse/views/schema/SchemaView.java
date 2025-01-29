/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.schema;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.htmlconnector.TwsDomTree;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils.SelectionListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.schema.SchemaViewContentProvider.Root;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.SchemaManager.Option;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.EngineListenerHelper;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class SchemaView extends ViewPart implements IPartListener2, ISelectionListener, TreeObjectListener {
	private Composite content;
	private TreeViewer schemaTreeViewer;
	private ISelection schemaTreeViewerSelection;
	private TreePath[] schemaTreeViewerExpandedTreePaths;
	private TreeViewer nodeTreeViewer;
	private ISelection nodeTreeViewerSelection;
	private TreePath[] nodeTreeViewerExpandedTreePaths;
	private TwsDomTree domTree;

	private Label message;
	private ToolItem autoRefresh;
	private ToolItem autoValidate;
	private ToolItem internalSchema;

	private boolean needRefresh;
	private boolean needValidate;
	private String projectName;
	
	private boolean isVisible;

	private Thread workingThread;
	private Queue<Runnable> tasks = new ConcurrentLinkedQueue<Runnable>();
	private EngineListenerHelper engineListener = new EngineListenerHelper() {

		@Override
		public void documentGenerated(final Document document) {
			final Element documentElement = document.getDocumentElement();
			if (documentElement != null) {
				String project = documentElement.getAttribute("project");
				if (project != null && project.equals(projectName)) {
					String sequence = documentElement.getAttribute("sequence");
					String connector = documentElement.getAttribute("connector");
					String transaction = documentElement.getAttribute("transaction");
					final String requestableName = sequence != null && sequence.length() > 0 ? sequence : connector + "__" + transaction;

					if (needValidate) {
						ConvertigoPlugin.asyncExec(() -> {
							message.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
							message.setText("Waiting for " + projectName + " " + requestableName + " XML response validation");
						});

						tasks.add(() -> {
							final Exception[] exception = {null};
							try {
								Engine.theApp.schemaManager.validateResponse(projectName, requestableName, document);
							} catch (SAXException e) {
								exception[0] = e;
							}

							ConvertigoPlugin.asyncExec(() -> {
								if (message != null && !message.isDisposed()) {
									if (exception[0] == null) {
										message.setForeground(Display.getCurrent().getSystemColor(SwtUtils.isDark() ? SWT.COLOR_GREEN : SWT.COLOR_DARK_GREEN));
										message.setText("The " + projectName + " " + requestableName + " XML response is valid.");
									} else {
										message.setForeground(Display.getCurrent().getSystemColor(SwtUtils.isDark() ? SWT.COLOR_RED : SWT.COLOR_DARK_RED));
										message.setText("The " + projectName + " " + requestableName + " XML response is invalid : " + exception[0].toString()/*exception[0].getMessage()*/);
									}
									content.layout(true);
								}
							});
						});
					}
					else {
						ConvertigoPlugin.asyncExec(() -> {
							message.setForeground(Display.getCurrent().getSystemColor(SwtUtils.isDark() ? SWT.COLOR_GREEN : SWT.COLOR_DARK_GREEN));
							message.setText("'" + projectName + "' schema generated.");
						});
					}
				}
			}
		}

	};

	public SchemaView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		workingThread = new Thread(() -> {
			while (workingThread != null) {

				try {
					Runnable task = null;
					synchronized (workingThread) {
						task = tasks.poll();
						if (task == null) {
							workingThread.wait(5000);
						}
					}
					if (task != null) {
						task.run();
					}
				} catch (Throwable e) {
					System.err.println("Exception in " + projectName);
					e.printStackTrace();
				}
			}
		});
		workingThread.setName("SchemaViewThread");
		workingThread.start();

		makeUI(content = new Composite(parent, SWT.NONE));

		getSite().getPage().addSelectionListener(this);
		getSite().getPage().addPartListener(this);
		isVisible = getSite().getPage().isPartVisible(this);

		ConvertigoPlugin.runAtStartup(() -> {
			Engine.theApp.addEngineListener(engineListener);
		});
	}

	private void makeUI(Composite content) {
		content.setLayout(SwtUtils.newGridLayout(1, false, 0, 0, 0, 0));

		// TOP TOOLBAR
		Composite composite = new Composite(content, SWT.BORDER);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		composite.setLayout(SwtUtils.newGridLayout(2, false, 0, 0, 0, 0));
		composite.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		ToolBar toolbar = new ToolBar(composite, SWT.NONE);

		ToolItem toolItem = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(toolItem, "icons/studio/refresh.gif", "R", "Refresh");
		toolItem.addSelectionListener((SelectionListener) (e) -> {
			needRefresh = true;
			Engine.theApp.schemaManager.clearCache(projectName);
			updateSchema((IStructuredSelection) ConvertigoPlugin.getDefault().getProjectExplorerView().viewer.getSelection());
		});

		toolItem = autoRefresh = new ToolItem(toolbar, SWT.CHECK);
		SwtUtils.setToolItemIcon(toolItem, "icons/studio/refresh.d.gif", "AR", "Toggle auto refresh");
		toolItem.setSelection(true);

		toolItem = autoValidate = new ToolItem(toolbar, SWT.CHECK);
		SwtUtils.setToolItemIcon(toolItem, "icons/studio/validate.gif", "AV", "Toggle auto validate");
		toolItem.setSelection(false);
		toolItem.addSelectionListener((SelectionListener) (e) -> {
			needValidate = autoValidate.getSelection();
		});

		toolItem = internalSchema = new ToolItem(toolbar, SWT.CHECK);
		SwtUtils.setToolItemIcon(toolItem, "icons/studio/pretty_print.gif", "IS", "Toggle internal schema");
		toolItem.addSelectionListener((SelectionListener) (e) -> {
			needRefresh = true;
			updateSchema((IStructuredSelection) ConvertigoPlugin.getDefault().getProjectExplorerView().viewer.getSelection());
		});

		message = new Label(composite, SWT.WRAP);
		message.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		message.setText("No schema to generate");

		// MAIN SASH FORM
		SashForm sashForm = new SashForm(content, SWT.NONE);
		sashForm.setOrientation(SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

		// SCHEMA PANE
		schemaTreeViewer = makeTreeViewer(sashForm);
		schemaTreeViewer.setContentProvider(new SchemaViewContentProvider(3));
		schemaTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				Object firstElement = ((IStructuredSelection) event.getSelection()).getFirstElement();
				if (firstElement instanceof XmlSchemaObject &&
						(nodeTreeViewer.getInput() == null || ((Root) nodeTreeViewer.getInput()).get() != firstElement) &&
						!(firstElement instanceof XmlSchema)) {
					nodeTreeViewer.setInput(SchemaViewContentProvider.newRoot(firstElement));

					if (nodeTreeViewerExpandedTreePaths != null && nodeTreeViewerExpandedTreePaths.length > 0) {
						nodeTreeViewer.setExpandedTreePaths(nodeTreeViewerExpandedTreePaths);
						nodeTreeViewer.setSelection(nodeTreeViewerSelection);
						nodeTreeViewerExpandedTreePaths = null;
						nodeTreeViewerSelection = null;
					} else {
						nodeTreeViewer.expandToLevel(5);
						domTree.fillDomTree(XmlSchemaUtils.getDomInstance((XmlSchemaObject) firstElement));
					}
				}
			}
		});

		// DETAIL PANE
		nodeTreeViewer = makeTreeViewer(sashForm);
		nodeTreeViewer.setContentProvider(new SchemaViewContentProvider());
		nodeTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				Object firstElement = ((IStructuredSelection) event.getSelection()).getFirstElement();
				if (firstElement instanceof XmlSchemaObject && !(firstElement instanceof XmlSchema)) {
					domTree.fillDomTree(XmlSchemaUtils.getDomInstance((XmlSchemaObject) firstElement));
				}
			}
		});

		// DOM PANE
		composite = new Composite(sashForm, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setLayout(SwtUtils.newGridLayout(1, false, 0, 0, 0, 0));

		toolbar = new ToolBar(composite, SWT.NONE);
		toolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		toolItem = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(toolItem, "icons/studio/collapse_all_nodes.gif", "C", "Collapse all");
		toolItem.addSelectionListener((SelectionListener) (e) -> {
			domTree.collapseAll();
		});

		toolItem = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(toolItem, "icons/studio/expand_all_nodes.gif", "E", "Expand all");
		toolItem.addSelectionListener((SelectionListener) (e) -> {
			domTree.expandAll();
		});

		domTree = new TwsDomTree(composite, SWT.BORDER);
		domTree.setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	@Override
	public void dispose() {
		workingThread = null;
		try {
			getSite().getPage().removeSelectionListener(this);
			getSite().getPage().removePartListener(this);
			Engine.theApp.removeEngineListener(engineListener);
			var pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
			if (pev != null) {
				pev.removeTreeObjectListener(this);
			}
		}
		catch (Exception e) {};
		content.dispose();
		super.dispose();
	}

	@Override
	public void setFocus() {
		schemaTreeViewer.getControl().setFocus();
	}

    @Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection && part instanceof ProjectExplorerView) {
			if (isVisible && autoRefresh.getSelection()) {
				updateSchema((IStructuredSelection) selection);
			}
		}
	}

    @Override
	public void partOpened(IWorkbenchPartReference part) {
		if (part.getPart(false) instanceof ProjectExplorerView pev) {
			pev.addTreeObjectListener(this);
		}
	}

    @Override
	public void partClosed(IWorkbenchPartReference part) {
    	if (part.getPart(false) instanceof ProjectExplorerView pev) {
			pev.removeTreeObjectListener(this);
		}
	}

    @Override
	public void partBroughtToTop(IWorkbenchPartReference part) {
	}

    @Override
	public void partActivated(IWorkbenchPartReference part) {
    	if (part.getPart(false) instanceof ProjectExplorerView pev) {
			pev.addTreeObjectListener(this);
		}
	}
	
    @Override
    public void partVisible(IWorkbenchPartReference partRef) {
        if (partRef.getId().equals(getViewSite().getId())) {
        	if (!isVisible) {
                isVisible = true;
                ProjectExplorerView pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
				if (pev != null) {
					updateSchema(pev.viewer.getStructuredSelection());
				}
        	}
        }
    }

    @Override
    public void partHidden(IWorkbenchPartReference partRef) {
        if (partRef.getId().equals(getViewSite().getId())) {
            isVisible = false;
        }
    }
    
	public void partDeactivated(IWorkbenchPart part) {
	}

    @Override
	public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
		needRefresh = true;
	}

    @Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		needRefresh = true;
	}

    @Override
	public void treeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		needRefresh = true;
	}

	private TreeViewer makeTreeViewer(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setLayout(SwtUtils.newGridLayout(1, false, 0, 0, 0, 0));

		ToolBar toolbar = new ToolBar(composite, SWT.NONE);
		toolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		final TreeViewer treeViewer = new TreeViewer(composite);

		ToolItem toolItem = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(toolItem, "icons/studio/collapse_all_nodes.gif", "C", "Collapse all");

		toolItem.addSelectionListener((SelectionListener) (e) -> {
			treeViewer.collapseAll();
		});

		toolItem = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(toolItem, "icons/studio/expand_all_nodes.gif", "E", "Expand all");

		toolItem.addSelectionListener((SelectionListener) (e) -> {
			treeViewer.expandToLevel(50);
		});

		treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		treeViewer.setLabelProvider(SchemaViewContentProvider.decoratingLabelProvider);
		treeViewer.setComparer(new IElementComparer() {

			public int hashCode(Object element) {
				String txt = SchemaViewContentProvider.decoratingLabelProvider.getText(element);
				int hash = txt.hashCode();
				if (element instanceof XmlSchemaObject) {
					Iterator<DatabaseObject> ref = SchemaMeta.getReferencedDatabaseObjects((XmlSchemaObject) element).iterator();
					if (ref.hasNext()) {
						hash += ref.next().hashCode();
					}
				}
				return hash;
			}

			public boolean equals(Object a, Object b) {
				boolean ret = false;
				if (a != null && b!= null && a.getClass().equals(b.getClass())) {
					String aTxt = SchemaViewContentProvider.decoratingLabelProvider.getText(a);
					String bTxt = SchemaViewContentProvider.decoratingLabelProvider.getText(b); 
					if (aTxt.equals(bTxt)) {
						if (a instanceof XmlSchemaObject) {
							Iterator<DatabaseObject> aRef = SchemaMeta.getReferencedDatabaseObjects((XmlSchemaObject) a).iterator();
							Iterator<DatabaseObject> bRef = SchemaMeta.getReferencedDatabaseObjects((XmlSchemaObject) b).iterator();
							if (aRef.hasNext() && bRef.hasNext()) {
								ret = aRef.next() == bRef.next();
							}
						} else {
							ret = true;
						}
					}
				}
				return ret;
			}
		});
		return treeViewer;
	}

	private void updateSchema(IStructuredSelection selection) {
		try {
			Object firstElement = selection.getFirstElement();
			if (firstElement instanceof DatabaseObjectTreeObject) {
				DatabaseObjectTreeObject dboTreeObject = (DatabaseObjectTreeObject) firstElement;
				String currentProjectName = dboTreeObject.getObject().getProject().getName();
				if (needRefresh || projectName == null || !projectName.equals(currentProjectName)) {
					needRefresh = false;
					projectName = currentProjectName;

					schemaTreeViewerExpandedTreePaths = schemaTreeViewer.getExpandedTreePaths();
					schemaTreeViewerSelection = schemaTreeViewer.getSelection();

					nodeTreeViewerExpandedTreePaths = nodeTreeViewer.getExpandedTreePaths();
					nodeTreeViewerSelection = nodeTreeViewer.getSelection();

					schemaTreeViewer.setInput(null);
					nodeTreeViewer.setInput(null);
					domTree.fillDomTree(null);
					message.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
					message.setText("Waiting for the " + projectName + " schema generation...");

					final boolean fullSchema = internalSchema.getSelection();

					synchronized (workingThread) {
						tasks.add(() -> {
							try {
								final XmlSchemaCollection xmlSchemaCollection = Engine.theApp.schemaManager.getSchemasForProject(projectName, fullSchema ? Option.fullSchema : null);

								ConvertigoPlugin.asyncExec(() -> {
									schemaTreeViewer.setInput(xmlSchemaCollection);

									if (schemaTreeViewerExpandedTreePaths != null && schemaTreeViewerExpandedTreePaths.length > 0) {
										schemaTreeViewer.setExpandedTreePaths(schemaTreeViewerExpandedTreePaths);
										schemaTreeViewer.setSelection(schemaTreeViewerSelection);
										schemaTreeViewerExpandedTreePaths = null;
										schemaTreeViewerSelection = null;
									} else {
										schemaTreeViewer.expandToLevel(3);
									}
								});

								if (needValidate) {
									final Exception[] exception = {null};
									try {
										XmlSchemaUtils.validate(xmlSchemaCollection);
									} catch (SAXException e) {
										exception[0] = e;
									}

									ConvertigoPlugin.asyncExec(() -> {
										if (exception[0] == null) {
											message.setForeground(Display.getCurrent().getSystemColor(SwtUtils.isDark() ? SWT.COLOR_GREEN : SWT.COLOR_DARK_GREEN));
											message.setText("The " + projectName + " schema is valid.");
										} else {
											message.setForeground(Display.getCurrent().getSystemColor(SwtUtils.isDark() ? SWT.COLOR_RED : SWT.COLOR_DARK_RED));
											message.setText("The " + projectName + " schema is invalid : " + exception[0].toString()/*getMessage()*/);
										}
										content.layout(true);
									});
								}
								else {
									ConvertigoPlugin.asyncExec(() -> {
										message.setForeground(Display.getCurrent().getSystemColor(SwtUtils.isDark() ? SWT.COLOR_GREEN : SWT.COLOR_DARK_GREEN));
										message.setText("'" + projectName + "' schema generated.");
										content.layout(true);
									});
								}
							} catch (Exception e) {
								ConvertigoPlugin.asyncExec(() -> {
									message.setForeground(Display.getCurrent().getSystemColor(SwtUtils.isDark() ? SWT.COLOR_RED : SWT.COLOR_DARK_RED));
									message.setText("An error occured : " + e.getMessage());
									content.layout(true);
								});
							}
						});
						workingThread.notify();
					}
				}
			}
		} catch (Exception e) {
		}
	}
}
