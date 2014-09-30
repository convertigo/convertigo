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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.transform.TransformerException;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.steps.IteratorStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.htmlconnector.TwsDomTree;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;
import com.twinsoft.util.Log;
import com.twinsoft.util.StringEx;

public class StepSourceEditorComposite extends AbstractDialogComposite implements IStepSourceEditor {

	private SashForm sashForm = null;
	private SashForm treesSashForm = null;
	private SashForm xpathSashForm = null;
	private TwsDomTree twsDomTree = null;
	private TreeItem lastSelectedItem = null;
	private TreeItem lastItem = null;
	private TreeItem lastSelectableItem = null;
	private Tree tree = null;
	private Document currentDom = null;
	private Step step = null;
	private Button buttonNew = null;
	private Button buttonRemove = null;
	private Label noPreviousLabel = null;
	private StepXpathEvaluatorComposite xpathEvaluator = null;
	private TwsCachedXPathAPI twsCachedXPathAPI = null;
	private XMLVector<String> stepSourceDefinition = null;
	private String regexpForPredicates = "\\[\\D{1,}\\]";
	
	public StepSourceEditorComposite(Composite parent, int style, AbstractDialogCellEditor cellEditor) {
		super(parent, style, cellEditor);
		
		if (cellEditor.databaseObjectTreeObject == null) {
			Composite parentEditorComposite = cellEditor.getControl().getParent();
			while (!(parentEditorComposite instanceof TableEditorComposite)) {
				parentEditorComposite = parentEditorComposite.getParent();
			}
			step = (Step)((TableEditorComposite)parentEditorComposite).cellEditor.databaseObjectTreeObject.getObject();
		}
		else {
			Object object = cellEditor.databaseObjectTreeObject.getObject();
			if (object instanceof Step)
				step = (Step)object;
			else // Variable
				step = (Step)((Variable)object).getParent();
		}
		
		//stepSourceDefinition = (XMLVector)((XMLVector)cellEditor.getEditorData()).clone();
		stepSourceDefinition  = GenericUtils.cast(GenericUtils.clone(cellEditor.getEditorData()));
		
		twsCachedXPathAPI = new TwsCachedXPathAPI();
		
		initialize();
	}

	private void initialize() {
		GridLayout gridLayout = new GridLayout ();
		gridLayout.numColumns = 8;
		this.setLayout (gridLayout);

		createButtons();
		createSashForm();
		selectResult();
		
		if (lastSelectableItem != null) {
			noPreviousLabel.setVisible(false);
		}
		
		// Generates xpath when item is selected with mouse clic
		twsDomTree.addMouseListener(new MouseAdapter(){
			public void mouseDown(MouseEvent e){
				Point point = new Point(e.x, e.y);
				TreeItem  treeItem = twsDomTree.getTree().getItem(point);
				if (treeItem!=null) {
					Object object = treeItem.getData();
					if ((object != null) && (object instanceof Node)) {
						xpathEvaluator.generateAbsoluteXpath(true, (Node)object);
					}
				}
			}
		});
		twsDomTree.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				try {
					TreeItem treeItem = twsDomTree.getTree().getSelection()[0];
					if (treeItem!=null) {
						Object object = treeItem.getData();
						if ((object != null) && (object instanceof Node)) {
							xpathEvaluator.generateAbsoluteXpath(true, (Node)object);
						}
					}
				}
				catch (Exception ex) {}
			}
		});
		
		//setSize(new org.eclipse.swt.graphics.Point(402,289));
	}

	private void addNewSource() {
		stepSourceDefinition = new XMLVector<String>();
		stepSourceDefinition.addElement("0");
		stepSourceDefinition.addElement(".");
	}
	
	private void setSourcePriority(long priority) {
		stepSourceDefinition.setElementAt(""+priority, 0);
	}
	
	private void setSourceXPath(String xpath) {
		stepSourceDefinition.setElementAt(xpath, 1);
	}

	private String getSourceXPath() {
		return (String)stepSourceDefinition.elementAt(1);
	}

	public Document getDom() {
		return currentDom;
	}
	
	public Object getValue() {
		return stepSourceDefinition;
	}
	
	private void createButtons() {
		int nbResults = stepSourceDefinition.size();
		
		buttonNew = new Button(this, SWT.PUSH);
		buttonNew.setText("New Source");
		buttonNew.setEnabled(nbResults == 0);
		buttonNew.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				createSource();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		buttonRemove = new Button(this, SWT.PUSH);
		buttonRemove.setText("Remove Source");
		buttonRemove.setEnabled(nbResults > 0);
		buttonRemove.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				removeSource();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		noPreviousLabel = new Label(this, SWT.NONE);
		noPreviousLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		noPreviousLabel.setText("no previous step is available for source selection");
		
		GridData gd = new GridData ();
		gd.horizontalSpan = 6;
		noPreviousLabel.setLayoutData(gd);
	}
	
	public void selectItemsInTree(TreeItem[] items) {
		Tree tree = twsDomTree.getTree();
		tree.setSelection(items);
		tree.setFocus();
	}
	
	private void selectResult() {
		lastSelectedItem = null;
		if (stepSourceDefinition.size() > 0) {
			tree.setEnabled(true);
			long priority = Long.parseLong((String)stepSourceDefinition.elementAt(0),10);
			TreeItem tItem = null;
			try {
				tItem = (priority == 0) ? findParentStepInTree():findStepInTree(null,priority);
			}
			catch (Exception e) {}
			
			if (tItem != null) {
				tree.setSelection(tItem);
				Event event = new Event();
				event.item = tItem;
				tree.notifyListeners(SWT.Selection, event);
				tree.setFocus();
			}
			else {
				buttonRemove.setText("Remove Broken Source!");
				layout(true);
				tree.setEnabled(false);
			}
		}
	}
	
	private TreeItem findParentStepInTree() {
		try {
			long priority = ((Step)step.getParent()).priority;
			return findStepInTree(null,priority);
		} catch (Exception e) {
			if (lastSelectableItem.getForeground().equals(Display.getCurrent().getSystemColor(SWT.COLOR_RED))) {
				return null;
			}
			return lastSelectableItem;
		}
	}
	
	private void createSource() {
		addNewSource();
		tree.deselectAll();
		twsDomTree.getTree().removeAll();
		buttonNew.setEnabled(stepSourceDefinition.size() == 0);
		buttonRemove.setEnabled(true);
		selectResult();
	}
	
	private void removeSource() {
		lastSelectedItem = null;
		tree.deselectAll();
		tree.setEnabled(false);
		twsDomTree.getTree().removeAll();
		
		xpathEvaluator.removeAnchor();
		stepSourceDefinition = new XMLVector<String>();

		buttonRemove.setEnabled(false);
		buttonNew.setEnabled(true);
		buttonRemove.setText("Remove Source");
		
		enableOK(true);
	}
	
	private void createSashForm() {
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		gd.horizontalSpan = 8;
		sashForm = new SashForm(this, SWT.NONE);
		sashForm.setOrientation(SWT.VERTICAL );
		sashForm.setLayoutData(gd);
		createTreesSashForm();
		createXpathSashForm();
		sashForm.setWeights(new int[]{70,30});
	}
	
	private void createTreesSashForm() {
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		treesSashForm = new SashForm(sashForm, SWT.NONE);
		treesSashForm.setOrientation(SWT.HORIZONTAL );
		treesSashForm.setLayoutData(gd);
		createSequenceTree();
		createXhtmlTree();
		treesSashForm.setWeights(new int[]{40,60});
	}
	
	private void createXpathSashForm() {
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		xpathSashForm = new SashForm(sashForm, SWT.NONE);
		xpathSashForm.setOrientation(SWT.HORIZONTAL );
		xpathSashForm.setLayoutData(gd);
		createXPathEvaluator();
	}
	
	private void createSequenceTree() {
		GridData gd = new GridData ();
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;

		tree = new Tree(treesSashForm, SWT.BORDER);
		tree.setLayoutData (gd);
		tree.setEnabled(stepSourceDefinition.size()>0);
		tree.addSelectionListener(
			new org.eclipse.swt.events.SelectionListener() {
				public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
					TreeItem tItem = (TreeItem)e.item;
					if (tItem.getForeground().equals(Display.getCurrent().getSystemColor(SWT.COLOR_RED))) {
						if (lastSelectedItem != null) tree.setSelection(lastSelectedItem);
						return;
					}
					if ((lastSelectedItem == null) || ((lastSelectedItem != null) && !lastSelectedItem.equals(tItem))) {
						DatabaseObject databaseObject = (DatabaseObject)tItem.getData();
						if (databaseObject instanceof Step) {
							setSourcePriority(databaseObject.priority);
							displayTargetWsdlDom((Step)databaseObject);
						}
					}
					lastSelectedItem = tItem;
				}
				public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
				}
			}
		);
		
		addStepsInTree(tree, step.getParentSequence());
		disableStepsInTree();
		tree.showItem(lastItem);
	}
	
	public static String getClassName(Class<?> c) {
	    String FQClassName = c.getName();
	    int firstChar;
	    firstChar = FQClassName.lastIndexOf ('.') + 1;
	    if ( firstChar > 0 ) FQClassName = FQClassName.substring ( firstChar );
	    return FQClassName;
    }

	private static boolean stepFound;
	private static TreeItem stepItem = null;
	
	private void addStepsInTree(Object parent, DatabaseObject databaseObject) {
		TreeItem tItem;
		
		if (parent instanceof Tree) {
			stepFound = false;
			tItem = new TreeItem((Tree)parent, SWT.NONE);
			tItem.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		}
		else {
			tItem = new TreeItem((TreeItem)parent,  SWT.NONE);
			lastItem = tItem;
			
			if (databaseObject instanceof Step) {
				Step step = (Step) databaseObject;
				
				if (!step.isEnable() || !step.isXml()) {
					tItem.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				}
			}
		}

		// associate our object with the tree Item.
		tItem.setData(databaseObject);
		
		if (databaseObject.priority == step.priority) {
			tItem.setText("* " + databaseObject.toString());
			stepFound = true;
			stepItem = tItem;
		} else {
			tItem.setText(databaseObject.toString());
		}
		
		if (!stepFound && (lastSelectableItem == null)) {
			if (!(tItem.getForeground().equals(Display.getCurrent().getSystemColor(SWT.COLOR_RED)))) {
				lastSelectableItem = tItem;
			}
		}
		
		// disable all steps after founded one
		if (stepFound) {
			tItem.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		}
		
		// now recurse on children steps
		if (databaseObject instanceof StepWithExpressions) {
			for (Step step: ((StepWithExpressions) databaseObject).getSteps()) {
				addStepsInTree(tItem, step);			
			}
		} else if (databaseObject instanceof Sequence) {
			for (Step step: ((Sequence) databaseObject).getSteps()) {
				addStepsInTree(tItem, step);			
			}
		}
	}
	
	private void disableStepsInTree() {
		if (stepItem != null) {
			// disable parents 'XML' steps if needed
			if (step.isXml()) {
				TreeItem tItem = stepItem;
				while (tItem.getParentItem().getData() instanceof Step) {
					tItem = tItem.getParentItem();
					if (((Step) tItem.getData()).isXml()) {
						tItem.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
					}
				}
			}
		}
	}
	
	private TreeItem findStepInTree(Object parent, long priority) {
		TreeItem[] items = null;
		
		if (parent == null) {
			items = tree.getItems();
		}
		else {
			DatabaseObject databaseObject = (DatabaseObject)((TreeItem)parent).getData();
			if (databaseObject instanceof Step) {
				if ((databaseObject).priority == priority) {
					return (TreeItem)parent;
				}
			}
			items = ((TreeItem)parent).getItems();
		}
		
		for (int i=0; i<items.length; i++) {
			TreeItem tItem = findStepInTree(items[i],priority);
			if (tItem != null) return tItem;
		}
		return null;
	}
	
	private Step sourceStep = null;
	private String sourceXpath = null;
	private boolean sourceChanged = false;
	
	private Step getTargetStep(Step step) throws EngineException {
		if (step != null && (step instanceof IStepSourceContainer)) {
			StepSource source = new StepSource(step,((IStepSourceContainer)step).getSourceDefinition());
			if (source != null && !source.isEmpty()) {
				return source.getStep();
			}
		}
		return step;
	}
	
	private void displayTargetWsdlDom(Step step) {
		try {
			String xpath = getSourceXPath();
			String anchor = step.getAnchor();
			
			Step targetStep = null;
			if (step instanceof IteratorStep) {
				targetStep = getTargetStep(step);
			}
			
			Project project = step.getProject();
			XmlSchema schema = Engine.theApp.schemaManager.getSchemaForProject(project.getName(), true);
			XmlSchemaObject xso = SchemaMeta.getXmlSchemaObject(schema, targetStep == null ? step:targetStep);
			Document stepDoc = XmlSchemaUtils.getDomInstance(xso);
			
			if (stepDoc != null) { // stepDoc can be null for non "xml" step : e.g jIf
				Document doc = step.getSequence().createDOM();
				Element root = (Element)doc.importNode(stepDoc.getDocumentElement(), true);
				doc.replaceChild(root, doc.getDocumentElement());
				removeUserDefinedNodes(doc.getDocumentElement());
				
				boolean shouldDisplayDom = (!(!step.isXml() && (step instanceof StepWithExpressions) && !(step instanceof IteratorStep)));
				if ((doc != null) && (shouldDisplayDom)) {
					if (lastSelectedItem == null) {
						sourceStep = step;
						sourceXpath = xpath;
					}
					else {
						sourceChanged = true;
						if (sourceStep == null)
							sourceStep = step;
						if (sourceXpath == null)
							sourceXpath = xpath;
					}
					
					xpath = ((!sourceChanged || (step.priority == sourceStep.priority)) ? sourceXpath:".");
					
					if (ConvertigoPlugin.getLogLevel() == Log.LOGLEVEL_DEBUG3)
						ConvertigoPlugin.logDebug3(XMLUtils.prettyPrintDOM(doc));
					displayXhtml(doc);
					xpathEvaluator.removeAnchor();
					xpathEvaluator.displaySelectionXpathWithAnchor(twsDomTree, anchor, xpath);
					enableOK(true);
				}
				else clean();
			}
			else clean();
		} catch (Exception e) {
			clean();
			ConvertigoPlugin.errorMessageBox(StringUtils.readStackTraceCauses(e));
		}
	}
	
	private void clean() {
		setSourceXPath(".");
		twsDomTree.removeAll();
		xpathEvaluator.removeAnchor();
		enableOK(false);
	}
	
	private void enableOK(boolean enabled) {
		if (parentDialog != null) {
			((EditorFrameworkDialog)parentDialog).enableOK(enabled);
		}
	}
	
	private void removeUserDefinedNodes(Element parent) {
		HashSet<Node> toRemove = new HashSet<Node>();
		
		NodeList list = parent.getChildNodes();
		for (int i=0; i<list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				((Element)node).removeAttribute("done");
				((Element)node).removeAttribute("hashcode");
				if (node.getNodeName().equals("schema-type")) {
					toRemove.add(node);
				}
				else {
					removeUserDefinedNodes((Element)node);
				}
			}
		}
		Iterator<Node> it = toRemove.iterator();
		while (it.hasNext()) {
			parent.removeChild(it.next());
		}
		toRemove.clear();
	}
	
	/**
	 * This method initializes xhtmlTree	
	 *
	 */
	private void createXhtmlTree() {
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.FILL;
		gd.grabExcessVerticalSpace = true;
		gd.grabExcessHorizontalSpace = true;
		
		twsDomTree = new TwsDomTree(treesSashForm, SWT.BORDER | SWT.MULTI);
		twsDomTree.getTree().setLayoutData(gd);
	}

	public void displayXhtml(Document dom){
		try {
			currentDom = dom;
			twsDomTree.fillDomTree(dom);
		}
		catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while filling DOM tree");
		}
	}
	
	public TreeItem[] findTreeItems(String xpath){
		TreeItem[] items = new TreeItem[]{};
		try {
			xpath = xpath.replaceAll(regexpForPredicates, "");
			
			NodeList nl = twsCachedXPathAPI.selectNodeList(currentDom, xpath);
			if (nl.getLength()>0) {
				TreeItem tItem = twsDomTree.findTreeItem(nl.item(0));
				Vector<TreeItem> v = new Vector<TreeItem>();
				while (tItem != null) {
					v.addElement(tItem);
					tItem = tItem.getParentItem();
				}
				items = (TreeItem[])v.toArray(new TreeItem[]{});
			}
		} catch (TransformerException e) {
			ConvertigoPlugin.logException(e, "Error while finding items in tree");
		}
		return items;
	}

	/* Removed buggy method: see #1680 */
//	public void selectElementsInTree(String xpath){
//		try {
//			String newXpath = xpathEvaluator.getAnchor() + xpath.substring(1);
//			newXpath = newXpath.replaceAll(regexpForPredicates, "");
//			
//			NodeList nl = twsCachedXPathAPI.selectNodeList(currentDom, newXpath);
//			for(int i=0;i<nl.getLength();i++)
//				twsDomTree.selectElementInTree(nl.item(i));
//		} catch (TransformerException e) {
//			ConvertigoPlugin.logException(e, "Error while selecting in tree");
//		}
//	}
	
	private void createXPathEvaluator() {
		xpathEvaluator = new StepXpathEvaluatorComposite(xpathSashForm,SWT.NONE,this);
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.FILL;
		gd.grabExcessVerticalSpace = true;
		gd.grabExcessHorizontalSpace = true;
		xpathEvaluator.setLayoutData(gd);
		
		xpathEvaluator.getXpath().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String anchor = xpathEvaluator.getAnchor();
				StringEx sx = new StringEx(xpathEvaluator.getXpath().getText());
				sx.replace(anchor, ".");
				String text = sx.toString();
				if (!text.equals("")) {
					setSourceXPath(text);
				}
				//TODO: disable/enable OK button
			}
		});
	}

	public Object getDragData() {
		return null;
	}
}
