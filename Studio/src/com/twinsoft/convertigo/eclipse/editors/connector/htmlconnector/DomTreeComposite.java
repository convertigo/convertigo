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

package com.twinsoft.convertigo.eclipse.editors.connector.htmlconnector;

import javax.xml.transform.TransformerException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.TreeItem;
import org.mozilla.interfaces.nsIDOMDocument;
import org.mozilla.interfaces.nsIDOMNodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.HtmlConnectorDesignComposite;
import com.twinsoft.convertigo.engine.parsers.AbstractXulWebViewer;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class DomTreeComposite extends Composite {

	private HtmlConnectorDesignComposite htmlDesign;
	private ToolBar treeToolBar = null;
	private TwsDomTree twsDomTree = null;
	private Image imageSyncTree = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/sync_tree.png"));
	private Image imageParentNode = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/parent_node.png"));
	private Image imagePreviewNode = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/preview_node.png"));
	private Image imageNextNode = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/next_node.png"));
	private Image imageRemoveAlerts = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/remove_alerts.png"));
	private Document currentDom;
	
	public DomTreeComposite(Composite parent, int style, HtmlConnectorDesignComposite htmlDesign) {
		super(parent, style);
		this.htmlDesign = htmlDesign;

		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginHeight = gridLayout.marginWidth = 0;
		gridLayout.verticalSpacing = gridLayout.marginRight = 5;
		
		setLayout(gridLayout);
		createToolBar();
		createXhtmlTree();
	}

	private void refreshDOMDisplay() {
		Thread th = new Thread() {
			
			@Override
			public void run() {
				this.setName("Document completed Update");
				htmlDesign.getWebViewer().setDomDirty();
				final Document dom = htmlDesign.getWebViewer().getDom();

				Display.getDefault().asyncExec(new Runnable() {
					public void run () {
						displayXhtml(dom);
					}
				});
			}
		};
		th.start();
	}
	
	protected void createToolBar() {
		treeToolBar = new ToolBar(this, SWT.NONE);

		ToolItem toolSync = new ToolItem(treeToolBar, SWT.PUSH);
		toolSync.setToolTipText("SyncTree");
		toolSync.setImage(imageSyncTree);
		toolSync.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
			
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				refreshDOMDisplay();
			}
			
			public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
			}
			
		});
		
		// Tree toolbar


		ToolItem tbChild = new ToolItem(treeToolBar, SWT.PUSH);
		tbChild.setToolTipText("Parent node");
		tbChild.setImage(imageParentNode);
		tbChild.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
			
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				htmlDesign.getWebViewer().selectParent();
			}

			public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
			}
			
		});

		ToolItem tbPrevious = new ToolItem(treeToolBar, SWT.PUSH);
		tbPrevious.setToolTipText("Previous node");
		tbPrevious.setImage(imagePreviewNode);
		tbPrevious.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
			
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				htmlDesign.getWebViewer().selectPreviousSibling();
			}
			
			public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
			}
			
		});

		ToolItem tbNext = new ToolItem(treeToolBar, SWT.PUSH);
		tbNext.setToolTipText("Next node");
		tbNext.setImage(imageNextNode);
		tbNext.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
			
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				htmlDesign.getWebViewer().selectNextSibling();
			}
			
			public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
			}
			
		});
		
		ToolItem tbRemoveAlertNodes = new ToolItem(treeToolBar, SWT.PUSH);
		tbRemoveAlertNodes.setToolTipText("Remove all \"ALERT\" nodes");
		tbRemoveAlertNodes.setImage(imageRemoveAlerts);
		tbRemoveAlertNodes.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
			
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				AbstractXulWebViewer webviewer = (AbstractXulWebViewer) htmlDesign.getWebViewer();
				nsIDOMDocument document;
				if (webviewer != null && (document = webviewer.getNsDom()) != null) {
					nsIDOMNodeList nl = document.getElementsByTagName("ALERT");
					while (nl.getLength() != 0) {
						if (nl.item(0).getNodeType() == Node.ELEMENT_NODE) {
							document.getDocumentElement().removeChild(nl.item(0));
						}
					}
					refreshDOMDisplay();
				}
			}
			
			public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
			}
		});
	}
	
	@Override
	public void dispose(){
		imageParentNode.dispose();
		imageNextNode.dispose();
		imagePreviewNode.dispose();
		imageSyncTree.dispose();
		super.dispose();
	}

	/**
	 * This method initializes xhtmlTree	
	 *
	 */
	private void createXhtmlTree() {
		twsDomTree = new TwsDomTree(this, SWT.MULTI);
		twsDomTree.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		twsDomTree.getTree().addSelectionListener(
			new org.eclipse.swt.events.SelectionListener() {
				
				public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
					//ConvertigoPlugin.logDebug(e.item.toString());
					Node node = (Node) e.item.getData();
					if (node != null) {
						String actualXpath = XMLUtils.calcXpath(node);
						htmlDesign.setCurrentAbsoluteXpath(actualXpath);
						htmlDesign.getWebViewer().setSelectedXpath(actualXpath, false);
						twsDomTree.selectTreeItem((TreeItem) e.item);
					}
				}
				
				public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
				}
				
			}
		);

		// DND support
		int ops = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] transfers = new Transfer[] {TextTransfer.getInstance()};

		DragSource source = new DragSource(twsDomTree.getTree(), ops);
		source.setTransfer(transfers);
		source.addDragListener(new DragSourceAdapter() {
			
			@Override
			public void dragSetData(DragSourceEvent event) {
				// Provide the data of the requested type.
				if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
					XpathEvaluatorComposite xpathEvaluator = htmlDesign.getXpathEvaluator();
					String anchor = xpathEvaluator.getAnchor();
					String XPath;
					if (anchor == null) {
						xpathEvaluator.generateSelectionXpath(true, twsDomTree);
						XPath = xpathEvaluator.getXpath().getText();
					} else {
						xpathEvaluator.generateAbsoluteXpath(true, (Node) twsDomTree.getSelection()[0].getData());
						XPath = xpathEvaluator.getXpath().getText().substring(anchor.length());
					}
					event.data = XPath;
				}
			}
			
		});
	}

	public void displayXhtml(Document dom) {
		if (dom != null) {
			try {
				ConvertigoPlugin.logDebug3("Display Html : fill DomTree start");
				htmlDesign.getHtmlConnector().setCurrentXmlDocument(dom);
				currentDom = dom;
				twsDomTree.fillDomTree(dom);
				ConvertigoPlugin.logDebug3("Display Html : fill DomTree stop");
			}
			catch (Exception e) {
				ConvertigoPlugin.logException(e, "Error while filling DOM tree");
			}
		}
	}
	
	public TwsDomTree getTwsDomTree() {
		return twsDomTree;
	}

	
	public Document getCurrentDom() {
		return currentDom;
	}

	/**
	 * @param xpath
	 */
	public void selectElementInTree(String xpath) {
		try {
			NodeList nl = htmlDesign.getXpathApi().selectNodeList(currentDom, xpath);
			if (nl.getLength() > 0) {
				twsDomTree.selectElementInTree(nl.item(0));
			}
		} catch (TransformerException e) {
			ConvertigoPlugin.logException(e, "Error while select tree item");
		}
	}
}
