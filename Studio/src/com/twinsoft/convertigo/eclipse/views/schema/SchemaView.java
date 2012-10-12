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

package com.twinsoft.convertigo.eclipse.views.schema;

import java.io.IOException;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.htmlconnector.TwsDomTree;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectListener;
import com.twinsoft.convertigo.eclipse.views.schema.SchemaViewContentProvider.Root;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class SchemaView extends ViewPart implements IPartListener, ISelectionListener, TreeObjectListener {
	private TreeViewer schemaTreeViewer;
	private TreeViewer nodeTreeViewer;
	private TwsDomTree domTree;
	
	private Label message;
	private ToolItem autoRefresh;
	private ToolItem internalSchema;
	
	private boolean needRefresh;
	private String projectName;
	private XmlSchemaCollection xmlSchemaCollection;
	
	private Thread workingThread;
	private Runnable task;
	
	public SchemaView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		workingThread = new Thread(new Runnable() {

			public void run() {
				while (workingThread != null) {

					try {
						synchronized (workingThread) {
							if (task != null) {
								task.run();
								task = null;
							}
							workingThread.wait(5000);
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

		});
		workingThread.setName("SchemaViewThread");
		workingThread.start();
	
		makeUI(new Composite(parent, SWT.NONE));
		
		getSite().getPage().addSelectionListener(this);
		
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(this);
	}
	
	private void makeUI(Composite content) {
		content.setLayout(SwtUtils.newGridLayout(1, false, 0, 0, 0, 0));
		
		// TOP TOOLBAR
		Composite composite = new Composite(content, SWT.BORDER);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		composite.setLayout(SwtUtils.newGridLayout(2, false, 0, 0, 0, 0));
		composite.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		
		SelectionListener selectionListener = new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				needRefresh = true;
				updateSchema((IStructuredSelection) ConvertigoPlugin.getDefault().getProjectExplorerView().viewer.getSelection());
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			
		};
		
		ToolBar toolbar = new ToolBar(composite, SWT.NONE);
		
		ToolItem toolItem = new ToolItem(toolbar, SWT.PUSH);
		setToolItemIcon(toolItem, "icons/studio/refresh.gif", "R", "Refresh");
		toolItem.addSelectionListener(selectionListener);
		
		toolItem = autoRefresh = new ToolItem(toolbar, SWT.CHECK);
		setToolItemIcon(toolItem, "icons/studio/refresh.d.gif", "AR", "Toggle auto refresh");
		toolItem.setSelection(true);
		
		toolItem = internalSchema = new ToolItem(toolbar, SWT.CHECK);
		setToolItemIcon(toolItem, "icons/studio/pretty_print.gif", "IS", "Toggle internal schema");
		toolItem.addSelectionListener(selectionListener);
		
		message = new Label(composite, SWT.NONE);
		message.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		message.setText("No schema to validate");
		
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
					nodeTreeViewer.setInput(xmlSchemaCollection);
					nodeTreeViewer.setInput(SchemaViewContentProvider.newRoot(firstElement));
					nodeTreeViewer.expandToLevel(5);

					domTree.fillDomTree(XmlSchemaUtils.getDomInstance((XmlSchemaObject) firstElement));
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
		setToolItemIcon(toolItem, "icons/studio/collapse_all_nodes.gif", "C", "Collapse all");
		
		toolItem = new ToolItem(toolbar, SWT.PUSH);
		setToolItemIcon(toolItem, "icons/studio/expand_all_nodes.gif", "E", "Expand all");
		
		domTree = new TwsDomTree(composite, SWT.BORDER);
		domTree.setLayoutData(new GridData(GridData.FILL_BOTH));
	}
	
	@Override
	public void dispose() {
		workingThread = null;
		getSite().getPage().removeSelectionListener(this);
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(this);
		super.dispose();
	}
	
	@Override
	public void setFocus() {
		schemaTreeViewer.getControl().setFocus();
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection && part instanceof ProjectExplorerView) {
			if (autoRefresh.getSelection()) {
				updateSchema((IStructuredSelection) selection);
			}
		}
	}

	public void partOpened(IWorkbenchPart part) {
		if (part instanceof ProjectExplorerView) {
			((ProjectExplorerView) part).addTreeObjectListener(this);
		}
	}

	public void partClosed(IWorkbenchPart part) {
		if (part instanceof ProjectExplorerView) {
			((ProjectExplorerView) part).removeTreeObjectListener(this);
		}
	}

	public void partBroughtToTop(IWorkbenchPart part) {
	}

	public void partActivated(IWorkbenchPart part) {
		if (part instanceof ProjectExplorerView) {
			((ProjectExplorerView) part).addTreeObjectListener(this);
		}
	}

	public void partDeactivated(IWorkbenchPart part) {
	}

	public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
		needRefresh = true;
	}

	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		needRefresh = true;
	}

	public void treeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		needRefresh = true;
	}
	
	private void setToolItemIcon(ToolItem toolItem, String iconPath, String text, String tooltip) {
		try {
			toolItem.setImage(ConvertigoPlugin.getDefault().getStudioIcon(iconPath));
		} catch (IOException e1) {
			toolItem.setText(text);
		}
		toolItem.setToolTipText(tooltip);
	}
	
	private TreeViewer makeTreeViewer(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setLayout(SwtUtils.newGridLayout(1, false, 0, 0, 0, 0));
		
		ToolBar toolbar = new ToolBar(composite, SWT.NONE);
		toolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		final TreeViewer treeViewer = new TreeViewer(composite);
		
		ToolItem toolItem = new ToolItem(toolbar, SWT.PUSH);
		setToolItemIcon(toolItem, "icons/studio/collapse_all_nodes.gif", "C", "Collapse all");
		
		toolItem.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				treeViewer.collapseAll();
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {	
			}
		});
		
		toolItem = new ToolItem(toolbar, SWT.PUSH);
		setToolItemIcon(toolItem, "icons/studio/expand_all_nodes.gif", "E", "Expand all");
		
		toolItem.addSelectionListener(new SelectionListener() {
			
			public void widgetSelected(SelectionEvent e) {
				treeViewer.expandToLevel(50);
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {	
			}
		});
		
		treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		treeViewer.setLabelProvider(SchemaViewContentProvider.decoratingLabelProvider);	
		return treeViewer;
	}
	
	private void updateSchema(IStructuredSelection selection) {
		Object firstElement = selection.getFirstElement();
		if (firstElement instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject dboTreeObject = (DatabaseObjectTreeObject) firstElement;
			String currentProjectName = dboTreeObject.getProjectTreeObject().getName();
			if (needRefresh || (projectName == null) || (!projectName.equals(currentProjectName))) {
				needRefresh = false;
				projectName = currentProjectName;
				
				schemaTreeViewer.setInput(null);
				nodeTreeViewer.setInput(null);
				domTree.fillDomTree(null);
				message.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
				message.setText("Waiting for schema validation");

				final boolean fullSchema = internalSchema.getSelection();

				synchronized (workingThread) {
					task = new Runnable() {

						public void run() {
							try {
								XmlSchema schema = Engine.theApp.schemaManager.getSchemaForProject(projectName, fullSchema);

								final XmlSchemaCollection xmlSchemaCollection = SchemaMeta.getCollection(schema);
								
								Display.getDefault().asyncExec(new Runnable() {

									public void run() {
										schemaTreeViewer.setInput(xmlSchemaCollection);
										schemaTreeViewer.expandToLevel(3);
									}
								
								});
								
								final Exception[] exception = {null};
								try {
									XmlSchemaUtils.validate(schema);
								} catch (SAXException e) {
									exception[0] = e;
								}

								Display.getDefault().asyncExec(new Runnable() {

									public void run() {
										if (exception[0] == null) {
											message.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
											message.setText("The current schema is valid.");
										} else {
											message.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED));
											message.setText("The current schema is invalid : " + exception[0].getMessage());
										}
									}
									
								});
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					};
					workingThread.notify();
				}
			}
		}
	}
}
