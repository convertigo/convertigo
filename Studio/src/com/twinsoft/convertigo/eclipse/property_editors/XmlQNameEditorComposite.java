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

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
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
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.views.schema.SchemaViewContentProvider;
import com.twinsoft.convertigo.eclipse.views.schema.SchemaViewLabelDecorator;
import com.twinsoft.convertigo.eclipse.views.schema.SchemaViewLabelProvider;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;

public class XmlQNameEditorComposite extends AbstractDialogComposite {
	private XmlSchemaCollection collection;
	private String currentNamespace;
	
	private Text tNamespace;
	private Text tLocalName;
	private Label lSummary;
	private boolean useType;
	private boolean useComplexType;
	private boolean useSimpleType;
	private boolean useRef;
	
	public XmlQNameEditorComposite(final Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		
		try {
			String propertyName = "" + cellEditor.propertyDescriptor.getId();
			DatabaseObject dbo = cellEditor.databaseObjectTreeObject.getObject();
			Project project = dbo.getProject();
			collection = Engine.theApp.schemaManager.getSchemasForProject(project.getName());
			currentNamespace = project.getTargetNamespace();
			if ("xmlTypeAffectation".equals(propertyName)) {
				// useComplexType = true; // TODO: add complex type support for input variables 
				useSimpleType = true;
			} else {
				useComplexType = "xmlComplexTypeAffectation".equals(propertyName);
				useSimpleType = "xmlSimpleTypeAffectation".equals(propertyName);
			}
			useType = useComplexType || useSimpleType;
			useRef = "xmlElementRefAffectation".equals(propertyName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		this.setLayoutData(new GridData(GridData.FILL_BOTH));
		this.setLayout(new GridLayout(1, false));
		
		new Label(this, style).setText("Existing objects");
		
		final TreeViewer treeViewer = new TreeViewer(this);
		treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		treeViewer.setContentProvider(new SchemaViewContentProvider() {
			
			@Override
			protected void filter(XmlSchemaObject xso, List<XmlSchemaObject> children, XmlSchemaObject subObject) {
				if (xso instanceof XmlSchema) {
					if ((useSimpleType && subObject instanceof XmlSchemaSimpleType) ||
						useComplexType && subObject instanceof XmlSchemaComplexType ||
						useRef && subObject instanceof XmlSchemaElement) {
							super.filter(xso, children, subObject);
					}
				} else {
					super.filter(xso, children, subObject);
				}
			}

			@Override
			public Object[] getChildren(Object object) {
				Object[] children = super.getChildren(object);
				for (Object child : children) {
					if (child instanceof NamedList) {
						if (useType && "Types".equals(((NamedList) child).getName())) {
							return new Object[] { child };
						}
						else if (useRef && "Elements".equals(((NamedList) child).getName())) {
							return new Object[] { child };
						}
					} else {
						return children;
					}
				}
				return children;
			}
			
		});
		
		treeViewer.setComparer(new IElementComparer() {
			
			public int hashCode(Object element) {
				return element.hashCode();
			}
			
			public boolean equals(Object a, Object b) {
				return a == b;
			}
			
		});

		DecoratingLabelProvider dlp = new DecoratingLabelProvider(new SchemaViewLabelProvider(), new SchemaViewLabelDecorator());
		treeViewer.setLabelProvider(dlp);
		treeViewer.setInput(collection);
		treeViewer.expandToLevel(3);
		
		new Label(this, SWT.NONE).setText("Namespace");
		
		tNamespace = new Text(this, SWT.NONE);
		tNamespace.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		tNamespace.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		tNamespace.setEditable(false);
		
		new Label(this, SWT.NONE).setText("Local name");
		
		tLocalName = new Text(this, SWT.NONE);
		tLocalName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		final Button bNone = new Button(this, SWT.NONE);
		bNone.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		bNone.setText("No "+ (useComplexType ? "type":(useRef ? "element":"object")));
		
		new Label(this, SWT.NONE).setText("Summary");
		
		lSummary = new Label(this, SWT.WRAP);
		lSummary.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		lSummary.setText("No change.");
		lSummary.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {		
			public void selectionChanged(SelectionChangedEvent event) {
				TreePath[] path = ((ITreeSelection) event.getSelection()).getPaths();
				
				XmlSchemaObject object = null;
				QName qName = null;
				if (path.length > 0 && path[0].getSegmentCount() > 2 && path[0].getSegment(2) instanceof XmlSchemaType) {
					object = (XmlSchemaType) path[0].getSegment(2);
					qName = ((XmlSchemaType)object).getQName();
				}
				if (path.length > 0 && path[0].getSegmentCount() > 2 && path[0].getSegment(2) instanceof XmlSchemaElement) {
					object = (XmlSchemaElement) path[0].getSegment(2);
					qName = ((XmlSchemaElement)object).getQName();
				}
				
				if (object != null) {
					String obText = (useType ? "type" : (useRef ? "element" : "object"));
					tLocalName.setText(qName.getLocalPart());
					tNamespace.setText(qName.getNamespaceURI());
					updateLabel(obText, qName, object);
					XmlQNameEditorComposite.this.layout(true);
				}
			}
		});

		bNone.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tNamespace.setText("");
				tLocalName.setText("");
				lSummary.setText("No "+(useType ? "type" : (useRef ? "element" : "object")) + " set.");
				lSummary.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
			}
		});
		
		tLocalName.addVerifyListener(new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if (e.text.contains(" ")) {
					e.doit = false;
				}
			}
		});
		
		tLocalName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String txt = tLocalName.getText();
				if (txt.length() == 0) {
					//bNone.notifyListeners(SWT.Selection, null); // commented to prevent stack over flow
				} else {
					tNamespace.setText(currentNamespace);
					QName qName = new QName(currentNamespace, txt);
					XmlSchemaType type = collection.getTypeByQName(qName);
					XmlSchemaElement element = collection.getElementByQName(qName);
					
					XmlSchemaObject object = type == null ? element:type;
					String obText = (useType ? "type":(useRef ? "element":"object"));
					if (object == null) {
						lSummary.setText("Create the dynamic "+obText+" : \n" + qName.toString());
						lSummary.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_MAGENTA));
						XmlQNameEditorComposite.this.layout(true);
					} else {
						updateLabel(obText, qName, object);
					}
				}
			}
		});
		
		if (useSimpleType) {
			tLocalName.setEnabled(false);
			bNone.setEnabled(false);
		}
		
		XmlQName schemaDefinition = (XmlQName) cellEditor.getValue();
		
		if (schemaDefinition != null) {
			QName qName = schemaDefinition.getQName();
			
			if (useType) {
				XmlSchemaType type = collection.getTypeByQName(qName);
				if (type != null) {
					treeViewer.setSelection(new StructuredSelection(type), true);
				}
			}
			
			if (useRef) {
				XmlSchemaElement element = collection.getElementByQName(qName);
				if (element != null) {
					treeViewer.setSelection(new StructuredSelection(element), true);
				}
			}
		}
	}

	@Override
	public XmlQName getValue() {
		String namespace = tNamespace.getText();
		String localName = tLocalName.getText();
		if (localName.length() == 0) {
			return new XmlQName();
		} else {
			return new XmlQName(new QName(namespace, localName));
		}
	}
	
	private void updateLabel(String obText, QName qName, XmlSchemaObject object) {
		if (SchemaMeta.isDynamic(object)) {
			if (SchemaMeta.isReadOnly(object)) {
				lSummary.setText("Use the dynamic read-only "+obText+" : \n" + qName.toString());
				lSummary.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
			} else {
				lSummary.setText("Use the dynamic "+obText+" : \n" + qName.toString());
				lSummary.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
			}
		} else {
			lSummary.setText("Use the static "+obText+" : \n" + qName.toString());
			lSummary.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
		}
	}
}