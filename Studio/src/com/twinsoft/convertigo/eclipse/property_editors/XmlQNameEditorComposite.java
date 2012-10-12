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

package com.twinsoft.convertigo.eclipse.property_editors;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.twinsoft.convertigo.beans.common.XmlQName;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.views.schema.SchemaViewContentProviderBis;
import com.twinsoft.convertigo.eclipse.views.schema.SchemaViewLabelDecoratorBis;
import com.twinsoft.convertigo.eclipse.views.schema.SchemaViewLabelProviderBis;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;

public class XmlQNameEditorComposite extends AbstractDialogComposite {	
	private XmlSchemaCollection collection;
	private String currentNamespace;
	
	private Text tNamespace;
	private Text tTypeName;
	private Label lSummary;
	
	public XmlQNameEditorComposite(final Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		XmlQName schemaDefinition = (XmlQName) cellEditor.getValue();
		
		try {
			Project project = cellEditor.databaseObjectTreeObject.getObject().getProject();
			collection = Engine.theApp.schemaManager.getSchemasForProject(project.getName());
			currentNamespace = project.getTargetNamespace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.setLayoutData(new GridData(GridData.FILL_BOTH));
		this.setLayout(new GridLayout(1, false));
		
		new Label(this, style).setText("Existing types");
		
		final TreeViewer bisTreeViewer = new TreeViewer(this);
		bisTreeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		bisTreeViewer.setContentProvider(new SchemaViewContentProviderBis() {

			@Override
			public Object[] getChildren(Object object) {
				Object[] children = super.getChildren(object);
				for (Object child : children) {
					if (child instanceof NamedList) {
						if ("Types".equals(((NamedList) child).getName())) {
							return new Object[] { child };
						}
					} else {
						return children;
					}
				}
				return children;
			}
			
		});

		DecoratingLabelProvider dlp = new DecoratingLabelProvider(new SchemaViewLabelProviderBis(), new SchemaViewLabelDecoratorBis());
		bisTreeViewer.setLabelProvider(dlp);
		bisTreeViewer.setInput(collection);
		bisTreeViewer.expandToLevel(3);
		
		new Label(this, SWT.NONE).setText("Namespace");
		
		tNamespace = new Text(this, SWT.NONE);
		tNamespace.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		tNamespace.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		tNamespace.setEditable(false);
		
		new Label(this, SWT.NONE).setText("Type name");
		
		tTypeName = new Text(this, SWT.NONE);
		tTypeName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		final Button bNone = new Button(this, SWT.NONE);
		bNone.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		bNone.setText("No type");
		
		new Label(this, SWT.NONE).setText("Summary");
		
		lSummary = new Label(this, SWT.WRAP);
		lSummary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lSummary.setText("No change.");
		lSummary.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		if (schemaDefinition != null) {
			QName qName = schemaDefinition.getQName();
			tTypeName.setText(qName.getLocalPart());
			String namespace = qName.getNamespaceURI();
			tNamespace.setText(namespace == null || namespace.length() == 0 ? currentNamespace : qName.getNamespaceURI());			
		}
		
		bisTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {		
			public void selectionChanged(SelectionChangedEvent event) {
				TreePath[] path = ((ITreeSelection) event.getSelection()).getPaths();
				if (path.length > 0 && path[0].getSegmentCount() > 2 && path[0].getSegment(2) instanceof XmlSchemaType) {
					XmlSchemaType type = (XmlSchemaType) path[0].getSegment(2);
					QName qName = type.getQName();
					tTypeName.setText(qName.getLocalPart());
					tNamespace.setText(qName.getNamespaceURI());
					if (SchemaMeta.isDynamic(type)) {
						lSummary.setText("Use the dynamic type : \n" + qName.toString());
						lSummary.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
					} else {
						lSummary.setText("Use the static type : \n" + qName.toString());
						lSummary.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
					}
					XmlQNameEditorComposite.this.layout(true);
				}
			}
		});

		bNone.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tNamespace.setText("");
				tTypeName.setText("");
				lSummary.setText("No type set.");
				lSummary.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
			}
		});
		
		tTypeName.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if (e.text.contains(" ")) {
					e.doit = false;
				}
			}
		});
		
		tTypeName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String txt = tTypeName.getText();
				if (txt.length() == 0) {
					bNone.notifyListeners(SWT.Selection, null);
				} else {
					tNamespace.setText(currentNamespace);
					QName qName = new QName(currentNamespace, txt);
					XmlSchemaType type = collection.getTypeByQName(qName);
					if (type == null) {
						lSummary.setText("Create the dynamic type : \n" + qName.toString());
						lSummary.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_MAGENTA));
						XmlQNameEditorComposite.this.layout(true);
					} else {
						if (SchemaMeta.isDynamic(type)) {
							lSummary.setText("Use the dynamic type : \n" + qName.toString());
							lSummary.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
						} else {
							lSummary.setText("Use the static type : \n" + qName.toString());
							lSummary.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
						}
					}
				}
			}
		});
		
		
	}

	@Override
	public XmlQName getValue() {
		String namespace = tNamespace.getText();
		String typeName = tTypeName.getText();
		if (typeName.length() == 0) {
			return new XmlQName();
		} else {
			return new XmlQName(new QName(namespace, typeName));
		}
	}
}