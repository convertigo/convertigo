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
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;
import org.apache.ws.commons.schema.constants.Constants;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
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
import com.twinsoft.convertigo.eclipse.views.schema.model.FolderNode;
import com.twinsoft.convertigo.eclipse.views.schema.model.SchemaNode;
import com.twinsoft.convertigo.engine.Engine;

public class SchemaView extends ViewPart implements IPartListener, ISelectionListener, TreeObjectListener {

	private TreeViewer treeViewer;
	private boolean needRefresh;
	private String projectName;
	
	public SchemaView() {
		projectName = null;
	}

	@Override
	public void createPartControl(Composite parent) {
		treeViewer = new TreeViewer(parent);
		treeViewer.setContentProvider(new SchemaViewContentProvider());
		DecoratingLabelProvider dlp = new DecoratingLabelProvider(new SchemaViewLabelProvider(), new SchemaViewLabelDecorator());
		treeViewer.setLabelProvider(dlp);
		treeViewer.setInput(null);

		getSite().setSelectionProvider(treeViewer);
		getSite().getPage().addSelectionListener(this);
		
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(this);
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(this);
		super.dispose();
	}
	
	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	private FolderNode root;
	
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			if (part instanceof ProjectExplorerView) {
				Object firstElement = ((IStructuredSelection) selection).getFirstElement();
				if (firstElement instanceof DatabaseObjectTreeObject) {
					DatabaseObjectTreeObject dboTreeObject = (DatabaseObjectTreeObject)firstElement;
					String currentProjectName = dboTreeObject.getProjectTreeObject().getName();
					if (needRefresh || (projectName == null) || (!projectName.equals(currentProjectName))) {
						projectName = currentProjectName;
						needRefresh = false;
						root = new FolderNode(null,"root");
						XmlSchemaCollection xmlSchemaCollection;
						try {
							xmlSchemaCollection = Engine.theApp.schemaManager.getSchemasForProject(projectName);
							XmlSchema[] schemas = xmlSchemaCollection.getXmlSchemas();
							for (int i=0; i < schemas.length; i++) {
								handleSchema(schemas[i]);
							}
							treeViewer.setInput(root);
							treeViewer.expandToLevel(3);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			else if (part.equals(this)) {
				
			}
		}
	}

	private void handleSchema(XmlSchema xmlSchema) {
		String tns = xmlSchema.getTargetNamespace();
		if (!tns.equals(Constants.URI_2001_SCHEMA_XSD)) {
			try {
				Document doc = xmlSchema.getSchemaDocument();
				//System.out.println(XMLUtils.prettyPrintDOM(doc));
				SchemaNode schema = new SchemaNode(root, doc.getDocumentElement());
				root.addChild(schema);
				
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
