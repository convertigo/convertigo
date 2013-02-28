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

package com.twinsoft.convertigo.eclipse.views.sourcepicker;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.transform.TransformerException;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.IteratorStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dnd.StepSource;
import com.twinsoft.convertigo.eclipse.dnd.StepSourceTransfer;
import com.twinsoft.convertigo.eclipse.editors.connector.htmlconnector.TwsDomTree;
import com.twinsoft.convertigo.eclipse.property_editors.IStepSourceEditor;
import com.twinsoft.convertigo.eclipse.property_editors.StepSourceXpathEvaluatorComposite;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.StepSourceEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.StepSourceListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;
import com.twinsoft.util.StringEx;

public class SourcePickerView extends ViewPart implements IStepSourceEditor, StepSourceListener {

	private Composite content;
	private SashForm mainSashForm, treesSashForm, xpathSashForm;
	private TwsDomTree twsDomTree;
	private StepSourceXpathEvaluatorComposite xpathEvaluator;
	private XMLVector<String> stepSourceDefinition = null;
	private TwsCachedXPathAPI twsCachedXPathAPI = null;
	private Document currentDom = null;
	private String regexpForPredicates = "\\[\\D{1,}\\]";
	private DatabaseObject selectedDbo = null;
	
	private final String show_step_source 		= "   Show step's source   ";
	private final String show_variable_source 	= "Show variable's source";
	private final String remove_source 			= "Remove source";
	
	public SourcePickerView() {
		twsCachedXPathAPI = new TwsCachedXPathAPI();
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout gl = new GridLayout(3,false);
		content = new Composite(parent, SWT.NONE);
		content.setLayout(gl);
		createHelpContent();
		createSashForm();
	}
	
	private void createSashForm() {
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		gd.horizontalSpan = 3;
		mainSashForm = new SashForm(content, SWT.NONE);
		mainSashForm.setOrientation(SWT.VERTICAL );
		mainSashForm.setLayoutData(gd);
		createTreeSashForm();
		createXpathSashForm();
	}
	
	private void createTreeSashForm() {
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		treesSashForm = new SashForm(mainSashForm, SWT.NONE);
		treesSashForm.setOrientation(SWT.HORIZONTAL );
		treesSashForm.setLayoutData(gd);
		createXhtmlTree();
	}
	
	private void createXpathSashForm() {
		GridData gd = new org.eclipse.swt.layout.GridData();
		gd.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		xpathSashForm = new SashForm(mainSashForm, SWT.NONE);
		xpathSashForm.setOrientation(SWT.HORIZONTAL );
		xpathSashForm.setLayoutData(gd);
		createXPathEvaluator();
	}
	
	private Text stepTag, stepType, stepName, stepComment;
	private Button showBtn, remBtn;
	
	private void createHelpContent() {
		// Tag
		Label tagLabel = new Label (content, SWT.NONE);
		tagLabel.setText("Tag :");
		
		GridData stepTagData = new GridData();
		stepTagData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		stepTag = new Text(content, SWT.NONE);
		stepTag.setText("");
		stepTag.setEnabled(false);
		stepTag.setLayoutData(stepTagData);

		// Help
		GridData helpTextData = new GridData();
		helpTextData.verticalSpan = 5;
		helpTextData.grabExcessHorizontalSpace = true;
		helpTextData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		Text helpText = new Text(content, SWT.BORDER|SWT.MULTI);
		helpText.setEditable(false);
		helpText.setText("Note :\nDrag items to a step \nin the Projects view \nto link the source.");
		helpText.setLayoutData(helpTextData);

		// Type
		Label typeLabel = new Label(content, SWT.NONE);
		typeLabel.setText("Type :");
		
		GridData stepTypeData = new GridData();
		stepTypeData.minimumWidth = 250;
		stepTypeData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		stepType = new Text(content, SWT.NONE);
		stepType.setText("");
		stepType.setEnabled(false);
		stepType.setLayoutData(stepTypeData);

		// Name
		Label nameLabel = new Label(content, SWT.NONE);
		nameLabel.setText("Name :");
		
		GridData stepNameData = new GridData();
		stepNameData.minimumWidth = 250;
		stepNameData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		stepName = new Text(content, SWT.NONE);
		stepName.setText("");
		stepName.setEnabled(false);
		stepName.setLayoutData(stepNameData);

		// Comment
		GridData commentLabelData = new GridData();
		commentLabelData.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		Label commentLabel = new Label(content, SWT.NONE);
		commentLabel.setText("Comment :");
		commentLabel.setLayoutData(commentLabelData);
		
		GridData stepCommentData = new GridData();
		stepCommentData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		stepComment = new Text(content, SWT.MULTI|SWT.WRAP);
		stepComment.setText("\n\n");
		stepComment.setEnabled(false);
		stepComment.setLayoutData(stepCommentData);
		
		// Buttons
		FillLayout fl = new FillLayout();
		fl.spacing = 10;
		GridData data4 = new GridData();
		data4.horizontalSpan=2;
		data4.verticalAlignment = org.eclipse.swt.layout.GridData.BEGINNING;
		data4.horizontalAlignment = org.eclipse.swt.layout.GridData.END;
		Composite cbtns = new Composite(content, SWT.NONE);
		cbtns.setLayout(fl);
		cbtns.setLayoutData(data4);
		
		showBtn = new Button(cbtns, SWT.NONE | SWT.TOGGLE);
		showBtn.setText(show_step_source);
		showBtn.setEnabled(false);
		showBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				remBtn.setEnabled(!showBtn.getSelection());
				showStep(selectedDbo, showBtn.getSelection());
			}
		});
		
		
		remBtn = new Button(cbtns, SWT.NONE);
		remBtn.setText(remove_source);
		remBtn.setEnabled(false);
		remBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeSource();
			}
		});
	}
	
	private void createXhtmlTree() {
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.FILL;
		gd.grabExcessVerticalSpace = true;
		gd.grabExcessHorizontalSpace = true;
		
		twsDomTree = new TwsDomTree(treesSashForm, SWT.BORDER | SWT.MULTI);
		twsDomTree.getTree().setLayoutData(gd);
		
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
		
		// DND support
		int ops = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] transfers = new Transfer[] {StepSourceTransfer.getInstance()};
		
		DragSource source = new DragSource(twsDomTree.getTree(), ops);
		source.setTransfer(transfers);
		source.addDragListener(new DragSourceAdapter() {	 	   	
			public void dragSetData(DragSourceEvent event) {
				if (StepSourceTransfer.getInstance().isSupportedType(event.dataType)) {
					event.data = getDragData();
				}
		 	}
		});
		
	}
	
	private void createXPathEvaluator() {
		xpathEvaluator = new StepSourceXpathEvaluatorComposite(xpathSashForm,SWT.NONE,this);
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.FILL;
		gd.grabExcessVerticalSpace = true;
		gd.grabExcessHorizontalSpace = true;
		xpathEvaluator.setLayoutData(gd);
		
		String text = xpathEvaluator.getLabel().getToolTipText();
		xpathEvaluator.getLabel().setToolTipText(text+"\nYou can drag the xPath edit zone to the project tree on :\n - A step to map its source");
		
		xpathEvaluator.getXpath().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String anchor = xpathEvaluator.getAnchor();
				StringEx sx = new StringEx(xpathEvaluator.getXpath().getText());
				sx.replace(anchor, ".");
				String text = sx.toString();
				if (!text.equals("")) {
					setSourceXPath(text);
				}
			}
		});
	}
	
	@Override
	public void setFocus() {

	}

	public TreeItem[] findTreeItems(String xpath) {
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
				items = v.toArray(new TreeItem[]{});
			}
		} catch (TransformerException e) {
			ConvertigoPlugin.logException(e, "Error while finding items in tree");
		}
		return items;
	}

	public Document getDom() {
		return currentDom;
	}

	private String getSourcePriority() {
		return stepSourceDefinition.elementAt(0);
	}
	
//	private void setSourcePriority(long priority) {
//		stepSourceDefinition.setElementAt(""+priority, 0);
//	}
	
	private String getSourceXPath() {
		return stepSourceDefinition.elementAt(1);
	}

	private void setSourceXPath(String xpath) {
		stepSourceDefinition.setElementAt(xpath, 1);
	}

	/* Removed buggy method: see #1680 */
//	public void selectElementsInTree(String xpath) {
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

	public void selectItemsInTree(TreeItem[] items) {
		Tree tree = twsDomTree.getTree();
		tree.setSelection(items);
		tree.setFocus();
	}

	public void sourceSelected(StepSourceEvent stepSourceEvent) {
		twsDomTree.removeAll();
		xpathEvaluator.removeAnchor();
		xpathEvaluator.getXpath().setText("");
		
		DatabaseObject dbo = (DatabaseObject)stepSourceEvent.getSource();
		String xpath  = stepSourceEvent.getXPath();
		String priority = ""+ dbo.priority;
		stepSourceDefinition = new XMLVector<String>();
		stepSourceDefinition.add(priority);
		stepSourceDefinition.add(xpath);
		selectedDbo = dbo;
		fillHelpContent();
		displayTargetWsdlDom(dbo);
	}

	private void fillHelpContent() {
		String tag = "", type = "", name = "", comment = "";
		String textBtn = show_step_source;
		boolean enableBtn = false;
		if (selectedDbo != null) {
			if (selectedDbo instanceof Step)
				tag = ((Step)selectedDbo).getStepNodeName();
			type = selectedDbo.getClass().getSimpleName();
			name = selectedDbo.getName();
			comment = selectedDbo.getComment();
			textBtn = (selectedDbo instanceof Step) ? show_step_source:show_variable_source;
			if (selectedDbo instanceof IStepSourceContainer)
				enableBtn = !((IStepSourceContainer)selectedDbo).getSourceDefinition().isEmpty();
		}
		
		stepTag.setText(tag);
		stepType.setText(type);
		stepName.setText(name);
		stepComment.setText(comment);
		showBtn.setText(textBtn);
		showBtn.setEnabled(enableBtn);
		showBtn.setSelection(false);
		remBtn.setEnabled(enableBtn);
	}
	
	private void showStep(DatabaseObject dbo, boolean showSource) {
		if (selectedDbo == null) return;
		
		String priority, xpath;
		DatabaseObject dboToShow = null;
		if (showSource) {
			XMLVector<String> sourceDefinition = ((IStepSourceContainer) dbo).getSourceDefinition();
			if (!sourceDefinition.isEmpty()) {
				Long key = new Long(sourceDefinition.firstElement());
				priority = ""+key;
				xpath = sourceDefinition.lastElement();
				Step step = dbo instanceof Step ? (Step)dbo:(Step)dbo.getParent();
				dboToShow = step.getSequence().loadedSteps.get(key);
				if (dboToShow == null) {
					ConvertigoPlugin.infoMessageBox("Source is not valid!");
					return;
				}
			}
			else {
				ConvertigoPlugin.infoMessageBox("Source is empty!");
				return;
			}
		}
		else {
			dboToShow = dbo;
			priority = ""+ dboToShow.priority;
			xpath  = ".";
		}
		
		twsDomTree.removeAll();
		xpathEvaluator.removeAnchor();
		xpathEvaluator.getXpath().setText("");
		
		stepSourceDefinition = new XMLVector<String>();
		stepSourceDefinition.add(priority);
		stepSourceDefinition.add(xpath);
		displayTargetWsdlDom(dboToShow);
		
		try {
			TreeObject treeObject = ConvertigoPlugin.getDefault().getProjectExplorerView().findTreeObjectByUserObject(dboToShow);
			ConvertigoPlugin.getDefault().getProjectExplorerView().setSelectedTreeObject(treeObject);
		}
		catch (Throwable t) {}
	}
	
	private void removeSource() {
		if (selectedDbo != null) {
			if (selectedDbo instanceof IStepSourceContainer) {
				try {
					DatabaseObjectTreeObject dboTreeObject = (DatabaseObjectTreeObject)ConvertigoPlugin.getDefault().getProjectExplorerView().findTreeObjectByUserObject(selectedDbo);
					ConvertigoPlugin.getDefault().getProjectExplorerView().setSelectedTreeObject(dboTreeObject);
					dboTreeObject.setPropertyValue("sourceDefinition", new XMLVector<String>());
					showBtn.setEnabled(false);
					showBtn.setSelection(false);
					remBtn.setEnabled(false);
				}
				catch (Throwable t) {}
			}
		}
	}
	
	private void displayTargetWsdlDom(DatabaseObject dbo) {
		try {
			if (dbo instanceof Step) {
				Step step = (Step)dbo;
				String xpath = getSourceXPath();
				String anchor = step.getAnchor();
				
				Project project = step.getProject();
				XmlSchema schema = Engine.theApp.schemaManager.getSchemaForProject(project.getName(), true);
				XmlSchemaObject xso = SchemaMeta.getXmlSchemaObject(schema, step);
				Document stepDoc = XmlSchemaUtils.getDomInstance(xso);
//				Document stepDoc = step.getWsdlDom();
				if (stepDoc != null) { // stepDoc can be null for non "xml" step : e.g jIf
					Document doc = step.getSequence().createDOM();
					Element root = (Element)doc.importNode(stepDoc.getDocumentElement(), true);
					doc.replaceChild(root, doc.getDocumentElement());
					removeUserDefinedNodes(doc.getDocumentElement());
					boolean shouldDisplayDom = (!(!step.isXml() && (step instanceof StepWithExpressions) && !(step instanceof IteratorStep)));
					if ((doc != null) && (shouldDisplayDom)) {
						displayXhtml(doc);
						xpathEvaluator.removeAnchor();
						xpathEvaluator.displaySelectionXpathWithAnchor(twsDomTree, anchor, xpath);
					}
					else clean();
				}
				else clean();
			}
			else clean();
		} catch (Exception e) {
			clean();
			ConvertigoPlugin.logException(e, StringUtils.readStackTraceCauses(e));
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

	private void clean() {
		setSourceXPath(".");
		twsDomTree.removeAll();
		xpathEvaluator.removeAnchor();
	}
	
	public void close() {
		selectedDbo = null;
		stepSourceDefinition = null;
		fillHelpContent();
		twsDomTree.removeAll();
		xpathEvaluator.removeAnchor();
		xpathEvaluator.getXpath().setText("");
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

	public Object getDragData() {
		return new StepSource(getSourcePriority(), getSourceXPath());
	}

	public Object getObject() {
		return selectedDbo;
	}
}
