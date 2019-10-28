/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.sourcepicker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
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
import com.twinsoft.convertigo.eclipse.editors.connector.htmlconnector.TwsDomTree;
import com.twinsoft.convertigo.eclipse.property_editors.IStepSourceEditor;
import com.twinsoft.convertigo.eclipse.property_editors.StepXpathEvaluatorComposite;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.SchemaManager.Option;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;
import com.twinsoft.util.StringEx;

public class SourcePickerHelper implements IStepSourceEditor {
	private TwsDomTree twsDomTree = null;

	private XMLVector<String> stepSourceDefinition = null;
	private StepXpathEvaluatorComposite xpathEvaluator;

	private Document currentDom = null;
	private TwsCachedXPathAPI twsCachedXPathAPI = new TwsCachedXPathAPI();
	private String regexpForPredicates = "\\[\\D{1,}\\]";

	protected String onDisplayXhtml(String xpath) {
		return xpath;
	}
	
	public void displayTargetWsdlDom(final DatabaseObject dbo) {
		try {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					Display display = Display.getDefault();
					Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
					
					Shell shell = getParentShell();
					shell.setCursor(waitCursor);
					
					boolean needToClean = true;
					try {
						if (dbo instanceof Step) {
							Step step = (Step)dbo;
							String xpath = getSourceXPath();
							String anchor = step.getAnchor();
							Document stepDoc = null;
							
							Step targetStep = step;
							while (targetStep instanceof IteratorStep) {
								targetStep = getTargetStep(targetStep);
							}
							
							if (targetStep != null) {
								Project project = step.getProject();
								String projectName = project.getName();
								Engine.theApp.schemaManager.getSchemasForProject(projectName);// force schemas generation for project
								XmlSchema schema = Engine.theApp.schemaManager.getSchemaForProject(projectName, Option.fullSchema);
								XmlSchemaObject xso = SchemaMeta.getXmlSchemaObject(schema, targetStep);
								if (xso != null) {
									stepDoc = XmlSchemaUtils.getDomInstance(xso);	
								} else {
									System.out.println("Could not retrieve schema for step \""+ targetStep.getName() + "\"!");
								}
							} else {
								System.out.println("Could not retrieve schema : targeted step is null!");
							}
							
							if (stepDoc != null/* && !(targetStep instanceof IteratorStep)*/) { // stepDoc can be null for non "xml" step : e.g jIf
								Document doc = step.getSequence().createDOM();
								Element root = (Element)doc.importNode(stepDoc.getDocumentElement(), true);
								doc.replaceChild(root, doc.getDocumentElement());
								removeUserDefinedNodes(doc.getDocumentElement());
								boolean shouldDisplayDom = (!(!step.isXml() && (step instanceof StepWithExpressions) && !(step instanceof IteratorStep)));
								if ((doc != null) && (shouldDisplayDom)) {
									needToClean = false;
									xpath = onDisplayXhtml(xpath);
									displayXhtml(doc);
									xpathEvaluator.removeAnchor();
									xpathEvaluator.displaySelectionXpathWithAnchor(twsDomTree, anchor, xpath);
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						needToClean = true;
					} finally {
						if (needToClean) {
							clean();
						}
						onDisplayDone();
						
						shell.setCursor(null);
						waitCursor.dispose();
			        }
				}
			});
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, StringUtils.readStackTraceCauses(e));
			clean();
		}
	}
	
	protected void onDisplayDone() {
		// does nothing
	}
	
	private Shell getParentShell() {
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			return activePage.getActivePart().getSite().getShell();
		} else {
			return ConvertigoPlugin.getMainShell();
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
	
	public String getSourceXPath() {
		return stepSourceDefinition.get(1);
	}
	
	protected void clean() {
		setSourceXPath(".");
		twsDomTree.removeAll();
		xpathEvaluator.removeAnchor();
	}
	
	private void setSourceXPath(String xpath) {
		stepSourceDefinition.set(1, xpath);
	}
	
	private Step getTargetStep(Step step) throws EngineException {
		if (step != null && (step instanceof IStepSourceContainer)) {
			com.twinsoft.convertigo.beans.core.StepSource source = new com.twinsoft.convertigo.beans.core.StepSource(step,((IStepSourceContainer)step).getSourceDefinition());
			if (source != null && !source.isEmpty()) {
				return source.getStep();
			} else {
				return null;
			}
		}
		return step;
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
	
	public Document getDom() {
		return currentDom;
	}
	
	public void setStepSourceDefinition(XMLVector<String> stepSourceDefinition) {
		this.stepSourceDefinition = stepSourceDefinition;
	}
	
	public XMLVector<String> getStepSourceDefinition() {
		return stepSourceDefinition;
	}
	
	public void selectItemsInTree(TreeItem[] items) {
		Tree tree = twsDomTree.getTree();
		tree.setSelection(items);
		tree.setFocus();
	}

	public TreeItem[] findTreeItems(String xpath) {
		TreeItem[] items = new TreeItem[]{};
		try {
			xpath = xpath.replaceAll(regexpForPredicates, "");
			
			NodeList nl = twsCachedXPathAPI.selectNodeList(currentDom, xpath);
			if (nl.getLength()>0) {
				TreeItem tItem = twsDomTree.findTreeItem(nl.item(0));
				List<TreeItem> v = new ArrayList<TreeItem>();
				while (tItem != null) {
					v.add(tItem);
					tItem = tItem.getParentItem();
				}
				items = v.toArray(new TreeItem[]{});
			}
		} catch (TransformerException e) {
			ConvertigoPlugin.logException(e, "Error while finding items in tree");
		}
		return items;
	}
	
	public void createXhtmlTree(Composite parent) {
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.FILL;
		gd.grabExcessVerticalSpace = true;
		gd.grabExcessHorizontalSpace = true;
		
		twsDomTree = new TwsDomTree(parent, SWT.BORDER | SWT.MULTI);
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
	}
	
	public void createXPathEvaluator(StepXpathEvaluatorComposite xpathEvaluatorComposite) {
		xpathEvaluator = xpathEvaluatorComposite;
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
	
	public TwsDomTree getTwsDomTree() {
		return twsDomTree;
	}
	
	public StepXpathEvaluatorComposite getXpathEvaluator() {
		return xpathEvaluator;
	}
	
	@Override
	public Object getDragData() {
		return new StepSource(getStepSourceDefinition().get(0), getSourceXPath());
	}
}
