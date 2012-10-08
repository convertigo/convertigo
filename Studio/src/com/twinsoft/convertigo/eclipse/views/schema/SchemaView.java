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

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;
import org.apache.ws.commons.schema.constants.Constants;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectListener;
import com.twinsoft.convertigo.eclipse.views.schema.model.ElementNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.GroupNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.SchemaNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.SchemaTreeRoot;
import com.twinsoft.convertigo.eclipse.views.schema.model.TreeRootNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.UnresolvedNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.XsdNode;
import com.twinsoft.convertigo.engine.Engine;

public class SchemaView extends ViewPart implements IPartListener, ISelectionListener, TreeObjectListener {

	private TreeViewer schemaTreeViewer;
	private SchemaTreeRoot schemaTreeRoot;
	private TreeViewer nodeTreeViewer;
	private TreeRootNode nodeTreeRoot;
	private XsdNode selectedXsdNode;
	private TreeViewer schemaTreeViewerBis;
	private TreeViewer nodeTreeViewerBis;
	
	private boolean needRefresh;
	private String projectName;
	
	public SchemaView() {
		projectName = null;
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout gl = new GridLayout(2, false);
		Composite content = new Composite(parent, SWT.NONE);
		content.setLayout(gl);
		
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		gd.horizontalSpan = 2;
		SashForm mainSashForm = new SashForm(content, SWT.NONE);
		mainSashForm.setOrientation(SWT.HORIZONTAL);
		mainSashForm.setLayoutData(gd);
		
		createSchemaForm(mainSashForm);
		createNodeForm(mainSashForm);
		
		
		mainSashForm = new SashForm(content, SWT.NONE);
		mainSashForm.setOrientation(SWT.HORIZONTAL);
		mainSashForm.setLayoutData(gd);
		
		createSchemaFormBis(mainSashForm);
		createNodeFormBis(mainSashForm);
		
		getSite().getPage().addSelectionListener(this);
		
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(this);
	}

	private void createSchemaFormBis(Composite parent) {
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		
		SashForm schemaSashForm = new SashForm(parent, SWT.NONE);
		schemaSashForm.setOrientation(SWT.VERTICAL);
		schemaSashForm.setLayoutData(gd);
		
		schemaTreeViewerBis = new TreeViewer(schemaSashForm);
		schemaTreeViewerBis.setContentProvider(new SchemaViewContentProviderBis(3));
		DecoratingLabelProvider dlp = new DecoratingLabelProvider(new SchemaViewLabelProviderBis(), new SchemaViewLabelDecoratorBis());
		schemaTreeViewerBis.setLabelProvider(dlp);
		schemaTreeViewerBis.setInput(null);
	}
	
	private void createNodeFormBis(Composite parent) {
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		
		SashForm nodeSashForm = new SashForm(parent, SWT.NONE);
		nodeSashForm.setOrientation(SWT.VERTICAL);
		nodeSashForm.setLayoutData(gd);
		
		nodeTreeViewerBis = new TreeViewer(nodeSashForm);
		nodeTreeViewerBis.setContentProvider(new SchemaViewContentProviderBis());
		DecoratingLabelProvider dlp = new DecoratingLabelProvider(new SchemaViewLabelProviderBis(), new SchemaViewLabelDecoratorBis());
		nodeTreeViewerBis.setLabelProvider(dlp);
		nodeTreeViewerBis.setInput(null);
	}
	
	private void createSchemaForm(Composite parent) {
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		
		SashForm schemaSashForm = new SashForm(parent, SWT.NONE);
		schemaSashForm.setOrientation(SWT.VERTICAL);
		schemaSashForm.setLayoutData(gd);
		schemaTreeViewer = createTreeViewer(schemaSashForm);
	}

	private void createNodeForm(Composite parent) {
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		
		SashForm nodeSashForm = new SashForm(parent, SWT.NONE);
		nodeSashForm.setOrientation(SWT.VERTICAL);
		nodeSashForm.setLayoutData(gd);
		nodeTreeViewer = createTreeViewer(nodeSashForm);
	}
	
	private TreeViewer createTreeViewer(Composite parent) {
		TreeViewer treeViewer = new TreeViewer(parent);
		treeViewer.setContentProvider(new SchemaViewContentProvider());
		DecoratingLabelProvider dlp = new DecoratingLabelProvider(new SchemaViewLabelProvider(), new SchemaViewLabelDecorator());
		treeViewer.setLabelProvider(dlp);
		treeViewer.setInput(null);
		return treeViewer;
	}
	
	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(this);
		super.dispose();
	}
	
	@Override
	public void setFocus() {
		schemaTreeViewer.getControl().setFocus();
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			if (part instanceof ProjectExplorerView) {
				Object firstElement = ((IStructuredSelection) selection).getFirstElement();
				if (firstElement instanceof DatabaseObjectTreeObject) {
					DatabaseObjectTreeObject dboTreeObject = (DatabaseObjectTreeObject)firstElement;
					String currentProjectName = dboTreeObject.getProjectTreeObject().getName();
					if (needRefresh || (projectName == null) || (!projectName.equals(currentProjectName))) {
						needRefresh = false;
						projectName = currentProjectName;
						schemaTreeRoot = new SchemaTreeRoot(null,"schemaTreeRoot");
						try {
							final XmlSchemaCollection xmlSchemaCollection = Engine.theApp.schemaManager.getSchemasForProject(projectName);
							schemaTreeViewerBis.setInput(xmlSchemaCollection);
							schemaTreeViewerBis.expandToLevel(3);
							nodeTreeViewerBis.setInput(null);
							schemaTreeViewerBis.addSelectionChangedListener(new ISelectionChangedListener() {
								public void selectionChanged(SelectionChangedEvent event) {
									Object firstElement = ((IStructuredSelection) event.getSelection()).getFirstElement();
									if (firstElement instanceof XmlSchemaObject && !(firstElement instanceof XmlSchema)) {
										nodeTreeViewerBis.setInput(xmlSchemaCollection);
										nodeTreeViewerBis.setInput(SchemaViewContentProviderBis.newRoot(firstElement));
										nodeTreeViewerBis.expandToLevel(5);
									}
								}
							});
							
							XmlSchema[] schemas = xmlSchemaCollection.getXmlSchemas();
							for (int i=0; i < schemas.length; i++) {
								handleSchema(schemas[i]);
							}
							schemaTreeViewer.setInput(schemaTreeRoot);
							nodeTreeViewer.setInput(null);
							schemaTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
								public void selectionChanged(SelectionChangedEvent event) {
									Object firstElement = ((IStructuredSelection) event.getSelection()).getFirstElement();
									if (firstElement instanceof XsdNode) {
										if (firstElement instanceof SchemaNode) return;
										if ((selectedXsdNode == null) || !selectedXsdNode.equals((XsdNode)firstElement)) {
											selectedXsdNode = (XsdNode)firstElement;
											
											nodeTreeRoot = new TreeRootNode(null,"nodeTreeRoot");
											nodeTreeRoot.addChild(selectedXsdNode.handleNode());
											nodeTreeViewer.setInput(nodeTreeRoot);
											
											nodeTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
												public void doubleClick(DoubleClickEvent event) {
													Object firstElement = ((IStructuredSelection) event.getSelection()).getFirstElement();
													if (firstElement instanceof XsdNode) {
														XsdNode selectedNode = (XsdNode)firstElement;
														String qname = null, ns, localName; 
														if (selectedNode.useType()) {
															qname = selectedNode.getObject().getAttribute("type");
														}
														else if (selectedNode.useRef()) {
															qname = selectedNode.getObject().getAttribute("ref");
														}
														
														if (qname != null) {
															ns = selectedNode.findNamespaceURI(qname);
															localName = selectedNode.findLocalName(qname);
															if (!ns.equals(Constants.URI_2001_SCHEMA_XSD)) {
																if (selectedNode.findChild(localName) == null) {
																	XsdNode xsdNode = null;
																	if (selectedNode instanceof ElementNode) {
																		if (selectedNode.useType())
																			xsdNode = schemaTreeRoot.findType(ns, localName);
																		else if (selectedNode.useRef())
																			xsdNode = schemaTreeRoot.findElement(ns, localName);
																	}
																	else if (selectedNode instanceof GroupNode) {
																		if (selectedNode.useRef())
																			xsdNode = schemaTreeRoot.findGroup(ns, localName);
																	}
																	
																	if (xsdNode != null)
																		selectedNode.addChild(xsdNode.handleNode());
																	else
																		selectedNode.addChild(new UnresolvedNode(selectedNode, qname));
																	nodeTreeViewer.refresh(firstElement);
																}
															}
														}
													}
												}
											});
										}
									}
								}
							});
							
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	private void handleSchema(XmlSchema xmlSchema) {
		String tns = xmlSchema.getTargetNamespace();
		if (!tns.equals(Constants.URI_2001_SCHEMA_XSD)) {
			try {
				Document doc = xmlSchema.getSchemaDocument();
				//System.out.println(XMLUtils.prettyPrintDOM(doc));
				SchemaNode schema = new SchemaNode(schemaTreeRoot, doc.getDocumentElement());
				schemaTreeRoot.addChild(schema);
				
			} catch (XmlSchemaSerializerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
}
