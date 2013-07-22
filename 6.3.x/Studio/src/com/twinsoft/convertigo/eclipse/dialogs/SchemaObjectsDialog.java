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

package com.twinsoft.convertigo.eclipse.dialogs;

import java.util.Hashtable;

import javax.xml.namespace.QName;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.IteratorStep;
import com.twinsoft.convertigo.beans.steps.XMLAttributeStep;
import com.twinsoft.convertigo.beans.steps.XMLComplexStep;
import com.twinsoft.convertigo.beans.steps.XMLElementStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.wizards.new_object.ObjectsExplorerComposite;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XSDUtils.XSD;
import com.twinsoft.convertigo.engine.util.XSDUtils.XmlGenerationDescription;

public class SchemaObjectsDialog extends Dialog implements Runnable {

	private String dialogTitle;
	private Object parentObject = null;
	private XSD xsd = null;
	private QName qname = null;
	public Object result = null;
	
	public SchemaObjectsDialog(Shell parentShell, Object parentObject, XSD xsd) {
		this(parentShell, ObjectsExplorerComposite.class, "Schema elements", parentObject);
		this.xsd = xsd;
	}
	
	public SchemaObjectsDialog(Shell parentShell, Class<? extends Composite> dialogAreaClass, String dialogTitle, Object parentObject) {
		super(parentShell);
		this.dialogTitle = dialogTitle;
		this.parentObject = parentObject;
	}
	
	private SchemaObjectsDialogComposite schemaObjectsDialogComposite = null;
	private ProgressBar progressBar = null;
	private Label labelProgression = null;
	private Hashtable<String, Step> stepsMap = new Hashtable<String, Step>(50);
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		
		try {
			GridData gridData = new GridData (GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_BOTH);
			
			schemaObjectsDialogComposite = new SchemaObjectsDialogComposite(composite,SWT.NONE,parentObject, xsd);
			schemaObjectsDialogComposite.setLayoutData(gridData);
			schemaObjectsDialogComposite.list.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					enableOK(schemaObjectsDialogComposite.list.getSelectionCount() > 0);
				}
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			labelProgression = schemaObjectsDialogComposite.labelProgression;
			progressBar = schemaObjectsDialogComposite.progressBar;
		}
		catch(Exception e) {
			
		}
		
		return composite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttonBar =  super.createButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setText("Import");
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		return buttonBar;
	}
	
	public void enableOK(boolean enabled) {
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(dialogTitle);
	}
	
	@Override
	protected int getShellStyle() {
		return SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL;
	}
	
	@Override
	protected void okPressed() {
		try {
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			getButton(IDialogConstants.CANCEL_ID).setEnabled(false);
			
			if (schemaObjectsDialogComposite != null) {
				qname = (QName)schemaObjectsDialogComposite.getValue(null);
			}
			
			Thread thread = new Thread(this);
			thread.start();
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to import objects");
		}
		finally {
			getButton(IDialogConstants.OK_ID).setEnabled(true);
			getButton(IDialogConstants.CANCEL_ID).setEnabled(true);
		}
	}
	
	public void run() {
		final Display display = getParentShell().getDisplay();
		Thread progressBarThread = new Thread("Progress Bar thread") {
			@Override
			public void run() {
				int i = 0;
				while (true) {
					try {
						i += 5;
						if (i >= 100) i = 0;
						final int j = i;
						display.asyncExec(new Runnable() {
							public void run() {
								if (!progressBar.isDisposed())
									progressBar.setSelection(j);
							}
						});
						
						sleep(500);
					}
					catch(InterruptedException e) {
						break;
					}
				}
			}
		};
		
		Throwable ex = null;
		try {
			progressBarThread.start();
			if (qname != null) {
				setTextLabel("Generating xml structure from schema object");
				XmlGenerationDescription xmlDescription = xsd.getXmlGenerationDescription();
				xmlDescription.setOutputOccurences(false);
				xmlDescription.setOutputSchemaTypeCData(false);
				xmlDescription.setOutputOccursAttribute(true);
				xmlDescription.setOutputElementWithNS(true);
				xmlDescription.setOutputOccursAttribute(true);
				
        		Document document = xsd.generateElementXmlStructure(qname);
        		//String s = XMLUtils.prettyPrintDOM(document);
        		
        		setTextLabel("Creating steps from xml");
        		Element root = document.getDocumentElement();
        		Element firstChild = (Element)root.getFirstChild();
        		if (firstChild != null)
        			result = createStep(null, firstChild);
			}
		}
		catch (Throwable e) {
			ex = e;
		}
		finally {
			stepsMap.clear();
			try {
				progressBarThread.interrupt();
				
				display.asyncExec(new Runnable() {
					public void run() {
						setReturnCode(OK);
						close();
					}
				});
				
			}
			catch (Throwable e) {}
			
			if (ex != null) {
				ConvertigoPlugin.logException(ex, "Unable to import objects");
			}
		}
	}
	
	private Step createStep(Object parent, Node node) throws EngineException {
		Step step = null;
		
		int nodeType = node.getNodeType();
		switch (nodeType) {
			case Node.ELEMENT_NODE:
				Element element = (Element)node;
				String tagname = element.getTagName();
				if (stepsMap.containsKey(tagname)) {
					step = deepClone(parent, (Step)stepsMap.get(tagname));
				}
				
				if (step == null) {
					step = createElementStep(parent,element);
					if (step != null) {
						// Add attributes
						NamedNodeMap map = element.getAttributes();
						for (int i=0; i<map.getLength(); i++) {
							createStep(step, map.item(i));
						}
						// Add elements
						NodeList children = element.getChildNodes();
						for (int i=0; i<children.getLength(); i++) {
							createStep(step, children.item(i));
						}
						
						if (parent != null) stepsMap.put(tagname, step);
					}
				}
				break;
			case Node.ATTRIBUTE_NODE:
				step = createAttributeStep(parent,(Attr)node);
				break;
			default:
				break;
		}
		
		return step;
	}
	
	private Step deepClone(Object parent, Step step) throws EngineException {
		Step cloned = null;
		try {
			cloned = (Step)step.clone();
			cloned.priority = cloned.getNewOrderValue();
			cloned.bNew = true;
			addStepToParent(parent, cloned);
			
			if (step instanceof StepWithExpressions) {
				StepWithExpressions swe = (StepWithExpressions)step;
				for (Step child: swe.getSteps()) {
					deepClone(cloned, child);
				}
			}
			
		} catch (CloneNotSupportedException e) {}
		return cloned;
	}
	
	private Step createElementStep(Object parent, Element element) throws EngineException {
		Step step = null;
		if (element != null) {
			if (parent != null) {
				String occurs = element.getAttribute(xsd.getXmlGenerationDescription().getOccursAttribute());
				if (!occurs.equals("")) {
					if (Long.parseLong(occurs, 10) > 1) {
						parent = createIteratorStep(parent, element);
					}
				}
			}
			
			String tagName = element.getTagName();
			String localName = element.getLocalName();
			String elementNodeName = (localName == null) ? tagName:localName;
			Node firstChild = element.getFirstChild();
			boolean isComplex = ((firstChild != null) && (firstChild.getNodeType() != Node.TEXT_NODE));
			
			setTextLabel("Creating \""+elementNodeName+"\" step");
			if (isComplex){
				step = new XMLComplexStep();
				((XMLComplexStep)step).setNodeName(elementNodeName);
			}
			else {
				step = new XMLElementStep();
				((XMLElementStep)step).setNodeName(elementNodeName);
			}
			step.bNew = true;
			addStepToParent(parent, step);
		}
		return step;
	}
	
	private void addStepToParent(Object parent, Step step) throws EngineException {
		if (step != null) {
			if (parent == null)
				step.setSequence((Sequence)parentObject);
			else
				((StepWithExpressions)parent).addStep(step);
		}
	}
	
	private Step createIteratorStep(Object parent, Element element) throws EngineException {
		Step step = (Step)parent;
		if (parent != null) {
			step = new IteratorStep();
			step.bNew = true;
			addStepToParent(parent, step);
		}
		return step;
	}
	
	private Step createAttributeStep(Object parent, Attr attr) throws EngineException {
		XMLAttributeStep step = null;
		if (attr != null) {
			String attrName = attr.getName();
			String localName = attr.getLocalName();
			String attributeNodeName = (localName == null) ? attrName:localName;
			if (!attributeNodeName.equals(xsd.getXmlGenerationDescription().getDoneAttribute()) &&
				!attributeNodeName.equals(xsd.getXmlGenerationDescription().getOccursAttribute())) {
				step = new XMLAttributeStep();
				step.setNodeName(attributeNodeName);
				step.bNew = true;
				addStepToParent(parent, step);
			}
		}
		return step;
	}
	
	public void setTextLabel(String text) {
		final Display display = getParentShell().getDisplay();
		final String labelText = text;
		display.asyncExec(new Runnable() {
			public void run() {
				if (!labelProgression.isDisposed())
					labelProgression.setText(labelText);
			}
		});
	}
	
}
