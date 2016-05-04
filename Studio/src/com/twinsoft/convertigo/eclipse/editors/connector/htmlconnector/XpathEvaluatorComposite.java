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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.htmlconnector.TwsDomTree.KeyAccelerator;
import com.twinsoft.convertigo.eclipse.editors.connector.htmlconnector.TwsDomTree.MenuMaker;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.XMLUtils;

abstract public class XpathEvaluatorComposite extends Composite {
	static final int allowKeyInAnchor[] = {
			SWT.ARROW_DOWN , SWT.ARROW_UP, SWT.ARROW_RIGHT, SWT.ARROW_LEFT, SWT.END, SWT.HOME, SWT.PAGE_UP, SWT.PAGE_DOWN
	};
	
	static {
		Arrays.sort(allowKeyInAnchor);
	}
	
	private StyledText xpath = null;
	private Label lab = null;
	protected String lastEval = null;
	protected String currentAnchor = null;
	private Map<String, Button> buttonsMap = null;
	private Text nodeData = null;
	protected TwsDomTree nodesResult = null;
	private Color highlightColor = new Color(Display.getDefault(),255,255,121);
	private List<String> xpathHistory = null;
	private int currentHistory = 0;
	protected boolean noPredicate = false;
	
	public XpathEvaluatorComposite(Composite parent, int style) {
		super(parent, style);
		initialize();
		AddDndSupport();

		xpathHistory = new LinkedList<String>();
		nodesResult.addMenuMaker(makeShowInBrowserMenuMaker());
		nodesResult.addMenuMaker(makeXPathMenuMaker(false));
		nodesResult.addKeyAccelerator(makeXPathKeyAccelerator(false));
		nodesResult.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}

			public void widgetSelected(SelectionEvent e) {
				Node node = (Node)e.item.getData();
				if (node != null) {
					// display the normalized text ..
					String temp = XMLUtils.getNormalizedText(node);
					temp = temp.replace('\n', ' ').trim();
					nodeData.setText(temp);
				}
			}
		});
		
		ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (projectExplorerView != null) {
			projectExplorerView.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					refreshButtonsEnable();
				}
			});
		}
		
		refreshButtonsEnable();
	}
	
	protected void refreshButtonsEnable() {
		for (Button button : buttonsMap.values()) {
			if (button != null && !button.isDisposed()) {
				boolean enable = true;
				String name = (String) button.getData("name");
				if (name.equals("anchor")) {
					enable = (lastEval != null || currentAnchor != null) && !isAnchorDisabled;
				} else if (name.equals("calcxpath")) {
					enable = lastEval == null && xpath.getText().length() > 0;
				} else if (name.equals("forward")) {
					enable = currentHistory != 0;
				} else if (name.equals("backward")) {
					int size = xpathHistory.size();
					enable = size != 0 && currentHistory != (size - 1);
				} else {
					enable = isButtonEnabled(name);
				}
				
				if (enable &&(name.equals("anchor"))) {
					Node root = nodesResult.getDocument().getFirstChild();
					enable = root.getChildNodes().getLength() != 0 ?
						true
						: root.getAttributes().getLength() != 0;
				}
				
				enableButton(button, enable);
			}
		}
	}
	
	abstract protected boolean isButtonEnabled(String name);
	
	protected void enableButton(Button button, boolean enable) {
		Boolean b = Boolean.valueOf(enable);
		if (button.getData("enable") == null || !button.getData("enable").equals(b)) {
			String imageURL = "" + button.getData("image_url");
			String tooltip = "" + button.getData("tooltip");
			if (!enable) {
				if (button.getData("disable_msg") != null) {
					tooltip += " (To enable button, " + button.getData("disable_msg") + ")";
				}
				int index = imageURL.lastIndexOf('.');
				imageURL = imageURL.substring(0, index) + ".d" + imageURL.substring(index);
			}
			button.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream(imageURL)));
			button.setToolTipText(tooltip);
			button.setData("enable", b);
		}
	}
	
	protected void enableButton(String name, boolean enable) {
		enableButton(buttonsMap.get(name), enable);
	}
	
	abstract protected void buttonSelected(String name);
	
	protected String[][][] getButtonsDefinition() {
		return new String[][][]{};
	}
	
	protected void initialize() {
		GridLayoutFactory gdf = GridLayoutFactory.swtDefaults();
		gdf.margins(1, 1).spacing(1, 1).equalWidth(false);
		
		setLayout(gdf.numColumns(2).create());
		//TODO:setBackground(new Color(Display.getDefault(),255,0,121));

		Composite buttons = new Composite(this, SWT.NONE);
		buttons.setLayoutData(new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_END));
		
		String[] buttonsDefNames = new String[]{"name", "tooltip", "disable_msg", "image_url", "other"};
		String [][][] buttonsDefinition = getButtonsDefinition();
		int numButtonsDefinition = buttonsDefinition.length;
		String [][][] buttonsDef = new String[numButtonsDefinition + 1][][];
		for (int i = 0; i < numButtonsDefinition; i++) buttonsDef[i]= buttonsDefinition[i];
		buttonsDef[numButtonsDefinition] = new String [][] {
			//name			, tooltip					, disable_msg			, image_url																	, other
			{"calcxpath"	, "Evaluate Xpath"			, "modify the Xpath"	, "/com/twinsoft/convertigo/eclipse/editors/images/calc_xpath.png", null},
			{"backward"		, "Backward Xpath history"	, null					, "/com/twinsoft/convertigo/eclipse/editors/images/backward_history.png", null},
			{"forward"		, "Forward Xpath history"	, null					, "/com/twinsoft/convertigo/eclipse/editors/images/forward_history.png", null},
			{"anchor"		, "Set anchor"				, "evaluate the Xpath"	, "/com/twinsoft/convertigo/eclipse/editors/images/anchor.png", null}
		};
		
		SelectionListener listener = new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}
			public void widgetSelected(SelectionEvent e) {
				boolean enable = ((Boolean)e.widget.getData("enable")).booleanValue();
				if (enable) {
					String name = (String)e.widget.getData("name");
					if (name.equals("calcxpath")) {
						performCalcXpath();
					} else if (name.equals("backward")) {
						moveHistory(true);
					} else if (name.equals("forward")) {
						moveHistory(false);
					} else if (name.equals("anchor")) {
						setAnchor(isAnchorDisabled);
					}
					else {
						buttonSelected(name);
					}
				}
			}
		};

		buttons.setLayout(gdf.numColumns(buttonsDef.length).create());

		buttonsMap = new HashMap<String, Button>();
		for (int i = 0; i < buttonsDef.length; i++) {
			String[][] columnDef = buttonsDef[i];
			Composite column = new Composite(buttons, SWT.NONE);
			column.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
			column.setLayout(new FillLayout(SWT.VERTICAL));
			
			for (int j = 0; j < columnDef.length; j++) {
				String[] buttonDef = columnDef[j];
				Button button = new Button(column, SWT.FLAT);
				buttonsMap.put(buttonDef[0], button);
				for (int k = 0; k < buttonsDefNames.length; k++) {
					button.setData(buttonsDefNames[k], buttonDef[k]);
				}
				button.addSelectionListener(listener);
				enableButton(button, false);
			}
		}
		
		Composite evaluator = new Composite(this, SWT.NONE);
		evaluator.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		evaluator.setLayout(gdf.numColumns(1).create());
		
		Composite evaluator_up = new Composite(evaluator, SWT.NONE);
		evaluator_up.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		evaluator_up.setLayout(gdf.numColumns(2).create());
		
		lab = new Label(evaluator_up, SWT.NONE);
		lab.setText("xPath");
		lab.setToolTipText("ctrl+up : backward history\nctrl+down : forward history\nYou can drag the xPath edit zone to the project tree on :\n - \"Inherited screen classes\" folder to create ScreenClasses\n - \"Criterias\" folder to create Criterias\n - \"Extraction Rules\" folder to create ExtractionRules");
		
		xpath = new StyledText(evaluator_up, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		xpath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		xpath.setWordWrap(true);
		xpath.addVerifyKeyListener(new VerifyKeyListener() {
			public void verifyKey(VerifyEvent event) {
				if (event.stateMask == SWT.CTRL) {
					if (event.keyCode == SWT.ARROW_DOWN) {
						moveHistory(false);
						return;
					} else if (event.keyCode == SWT.ARROW_UP) {
						moveHistory(true);
						return;
					}
				}
				
				if (event.character == '\r') {
					event.doit = false;
					performCalcXpath();
				} else if (currentAnchor != null) {
					int anchorStart = xpath.getText().indexOf(currentAnchor);
					if (anchorStart >= 0) {
						int caret = xpath.getCaretOffset();
						if (caret > anchorStart && caret < anchorStart + currentAnchor.length()) {
							event.doit = Arrays.binarySearch(allowKeyInAnchor, event.keyCode) >= 0;
						}
					}
				}
			}
		});

		xpath.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				lastEval = null;
				refreshButtonsEnable();
			}
		});

		SashForm evaluator_down = new SashForm(evaluator, SWT.HORIZONTAL);
		evaluator_down.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		nodesResult = new TwsDomTree(evaluator_down, SWT.MULTI);
		new TreeColumn(nodesResult.getTree(), SWT.LEFT).setText("Document");
		new TreeColumn(nodesResult.getTree(), SWT.RIGHT).setText("Value");
		nodesResult.setHeaderVisible(true);
		
		nodesResult.getColumn(0).setWidth(400);
		
		nodeData = new Text(evaluator_down, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
		nodeData.setEditable(false);
		
		evaluator_down.setWeights(new int[] {70, 30});
	}
	
	protected void AddDndSupport() {
		// DND support
		int ops = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] transfers = new Transfer[] {TextTransfer.getInstance()};
		
		DropTarget target = new DropTarget(xpath, ops);
		target.setTransfer(transfers);
		target.addDropListener(new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent droptargetevent) {
				xpath.setText(droptargetevent.data.toString());
				performCalcXpath();
			}
		});
		
		DragSource source;
		source = new DragSource(xpath, ops);
		source.setTransfer(transfers);
		source.addDragListener(new DragSourceAdapter() {	 	   	
			@Override
			public void dragSetData(DragSourceEvent event) {
		 	     // Provide the data of the requested type.
		 	     if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
		 	    	 String XPath = (currentAnchor == null) ?
	 	    			 xpath.getText()
	 	    			 : xpath.getText().substring(currentAnchor.length());
		 	         event.data = XPath;
		 	     }
		 	}
			@Override
			public void dragStart(DragSourceEvent event) {
				event.doit = !xpath.getText().equals("");
			}
		});
		
		source = new DragSource(lab, ops);
		source.setTransfer(transfers);
		source.addDragListener(new DragSourceAdapter() {
			@Override
			public void dragSetData(DragSourceEvent event) {
		 	     // Provide the data of the requested type.
		 	     if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
		 	    	 String XPath = currentAnchor == null ?
	 	    			 xpath.getText()
	 	    			 : xpath.getText().substring(currentAnchor.length());
		 	         event.data = XPath;
		 	     }
		 	}
			@Override
			public void dragStart(DragSourceEvent event) {
				event.doit = !xpath.getText().equals("");
			}
		});
	}
	
	public void setXpathText(String nodeXpath) {
		//TODO:if (currentAnchor != null) nodeXpath = currentAnchor + calcRelativeXpath(htmlDesign.getWebViewer().getDom(), currentAnchor, nodeXpath);
		xpath.setText(nodeXpath);
		lastEval = null;
		refreshButtonsEnable();
		if (currentAnchor != null) {
			int start = nodeXpath.indexOf(currentAnchor);
			if (start >= 0) {
				xpath.setStyleRange(new StyleRange(start, currentAnchor.length(), xpath.getForeground(), highlightColor));
			}
		}
	}
	
	protected void removeAnchor() {
		isAnchorDisabled = false;
		currentAnchor = null;
		lastEval = null;
		xpath.setStyleRange(null);
		refreshButtonsEnable();
	}
	
	protected void setAnchor(boolean disabled) {
		//TODO:Button anchor = (Button) buttonsMap.get("anchor");
		isAnchorDisabled = disabled;
		if (currentAnchor == null) {
			currentAnchor = xpath.getText();
			xpath.setStyleRange(new StyleRange(0,currentAnchor.length(),xpath.getForeground(),highlightColor));
			//TODO:anchor.setBackground(highlightColor);
		} else if (!isAnchorDisabled) {
			currentAnchor = null;
			xpath.setStyleRange(null);
			refreshButtonsEnable();
		}
	}
	
	private boolean isAnchorDisabled = false;
	
	abstract public Document getDom();
	
	public void performCalcXpath()	{
		ConvertigoPlugin.logDebug3("Get xpath node list start");
		Document nodesSelection = getXpathData(getDom(), xpath.getText());
		ConvertigoPlugin.logDebug3("Get xpath node list end");
		if (nodesSelection != null ) {
			ConvertigoPlugin.logDebug3("Fill xpath node list dom tree start");
			nodesResult.fillDomTree(nodesSelection);
			lastEval = xpath.getText();
			xpathHistory.add(lastEval);
			currentHistory = 0;
			refreshButtonsEnable();
			ConvertigoPlugin.logDebug3("Fill xpath node list dom tree end");
		} else {
			ConvertigoPlugin.logDebug3("Remove all start");
			nodesResult.removeAll();
			ConvertigoPlugin.logDebug3("Remove all End");
		}
		nodeData.setText("");
	}

	public String getSelectionXpath() {
		return xpath.getText();
	}

	public String generateSelectionXpath(TreeItem[] selection)
	{
		TreeItem treeItem, parentTreeItem, lastParentTreeItem = null;
		String 	xpath, filter, lastRootXPath = "", lastFirstChildrenXPath = null, selectionXpath = "",parentNodeName,nodeName;
		Node parentNode, lastRootNode = null;
		Object o = null;
		boolean	bRecurse, bFirstText = true, bFirstAttr = true;

		int len = selection.length;
		for (int i = 0; i< len; i++) {
			treeItem = selection[i];
			o = treeItem.getData();
			if (o instanceof Node) {
				Node node = (Node) o;
				nodeName = XMLUtils.xpathEscapeColon(node.getNodeName());
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					parentTreeItem = treeItem.getParentItem();
					xpath = XMLUtils.calcXpath(node, null);
					filter = getFilter((Element) node);
					bRecurse = ((lastParentTreeItem == null) || (!parentTreeItem.equals(lastParentTreeItem)));
					if (bRecurse) {
						if (lastParentTreeItem == null) {
							lastRootXPath = xpath;
							lastRootNode = node;
							selectionXpath = "//" +nodeName;
						} else {
							if (!selectionXpath.equals("") && (!bFirstText || !bFirstAttr)) {
								selectionXpath += "]";
							}
							if (!xpath.startsWith(lastRootXPath)) {
								selectionXpath += " | ";
							} else if ((lastFirstChildrenXPath != null) && (!xpath.startsWith(lastFirstChildrenXPath))) {
								selectionXpath += " | //" + lastRootNode.getNodeName();
								lastFirstChildrenXPath = xpath;
							}
							selectionXpath += "//" + nodeName;
						}
					} else if (parentTreeItem.equals(lastParentTreeItem)) {
						if (!selectionXpath.equals("") && (!bFirstText || !bFirstAttr)) {
							selectionXpath += "]";
						}
						selectionXpath  += "/" + nodeName;
					}
					
					selectionXpath  += filter;
					
					if ((lastParentTreeItem != null) && (lastFirstChildrenXPath == null)) {
						lastFirstChildrenXPath = xpath;
					}
					
					lastParentTreeItem = treeItem;
					if (filter.equals("")) {
						bFirstText = true;
						bFirstAttr = true;
					} else if (filter.indexOf("@") != -1) {
						bFirstAttr = false;
					} else if (filter.indexOf("text()") != -1) {
						bFirstText = false;
					}
				} else if (node.getNodeType() == Node.TEXT_NODE) {
					parentTreeItem = treeItem.getParentItem();
					parentNode = (Node) parentTreeItem.getData();
					xpath = XMLUtils.calcXpath(parentNode, null) + "/text()";
					parentNodeName = XMLUtils.xpathEscapeColon(parentNode.getNodeName());						
					bRecurse = ((lastParentTreeItem == null) || (!parentTreeItem.equals(lastParentTreeItem)));
					if (bRecurse) {
						if (lastParentTreeItem == null) {
							lastRootXPath = xpath;
							lastRootNode = parentNode;
							selectionXpath = "//" + parentNodeName;
						} else {
							if (!selectionXpath.equals("") && (!bFirstText || !bFirstAttr)) {
								selectionXpath += "]";
							}
							if (!xpath.startsWith(lastRootXPath)) {
								selectionXpath += " | ";
							}
							selectionXpath += "//" + parentNodeName;
						}
						selectionXpath += "[";
						lastParentTreeItem = parentTreeItem;
					} else if (parentTreeItem.equals(lastParentTreeItem)) {
						selectionXpath += bFirstText && bFirstAttr ? "[" : " and ";
					}
					selectionXpath  += "contains(text()," + XMLUtils.xpathGenerateConcat(node.getNodeValue().trim()) + ")";
					bFirstText = false;
				} else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
					parentTreeItem = treeItem.getParentItem().getParentItem();
					parentNode = (Node)parentTreeItem.getData();
					parentNodeName = XMLUtils.xpathEscapeColon(parentNode.getNodeName());	
					

					//TODO: fix problems when we have simple or/and double quotes
					String nodeValue = XMLUtils.xpathGenerateConcat(node.getNodeValue().trim());
					
					xpath = XMLUtils.calcXpath(parentNode, null) + "[@" + node.getNodeName().trim() + "=\"" + nodeValue + "\"]";
					bRecurse = ((lastParentTreeItem == null) || (!parentTreeItem.equals(lastParentTreeItem)));
					if (bRecurse) {
						if (lastParentTreeItem == null) {
							lastRootXPath = xpath;
							lastRootNode = parentNode;
							selectionXpath = "//" + parentNodeName;
						} else {
							if (!selectionXpath.equals("") && (!bFirstText || !bFirstAttr)) {
								selectionXpath += "]";
							}
							if (!xpath.startsWith(lastRootXPath)) {
								selectionXpath += " | ";
							}
							selectionXpath += "//" + parentNodeName;
						}
						selectionXpath += "[";
						lastParentTreeItem = parentTreeItem;
					} else if (parentTreeItem.equals(lastParentTreeItem)) {
						selectionXpath += bFirstAttr && bFirstText ? "[" : " and ";
					}
					selectionXpath += "@" + node.getNodeName().trim() + "=" + nodeValue;
					bFirstAttr = false;
				}
			}

		}
		if (!selectionXpath.equals("") && (!bFirstText || !bFirstAttr)) {
			selectionXpath += "]";
		}

		return selectionXpath;
	}

	private String getFilter(Element element) {
		String filter = "";
		if (element != null) {
			// Looks for attributes
			if (element.hasAttributes()) {
				List<String> list = Arrays.asList(new String[] {"id", "name"});
				Iterator<String> it = list.iterator();
				while (it.hasNext()) {
					Attr attr = element.getAttributeNode(it.next());
					if (filter.equals("") && (attr != null)) {
						filter += "[@" + attr.getNodeName().trim() + "=\""+ attr.getNodeValue().trim() + "\"";
						break;
					}
				}
			}
			
			// Looks for Text child node
			if (filter.equals("")) {
				
			}
		}
		
		return filter;
	}
	
	public void dispose() {
		/*TODO:imageAnchor.dispose();
		imageCalcXpath.dispose();
		imageScreenclass.dispose();
		imageCriteria.dispose();
		imageExtractionRule.dispose();*/
		super.dispose();
	}

	abstract public TwsCachedXPathAPI getXpathApi();
	
	private String calcRelativeXpath(Node dom,String anchor_s,String node_s) {
		try {
			Node parent_anchor, ex_parent_anchor, node, parent_node, ex_parent_node;

			String regexpForPredicates = "\\[\\D{1,}\\]";
			if (noPredicate) {
				anchor_s = anchor_s.replaceAll(regexpForPredicates, "");
				node_s = node_s.replaceAll(regexpForPredicates, "");
			}
			
			parent_anchor = ex_parent_anchor = getXpathApi().selectSingleNode(dom, anchor_s);
			node = getXpathApi().selectSingleNode(dom, node_s);

			String res_sibling, res = null;
			int level = 0;

			while(res == null && parent_anchor != null) {
				parent_node = ex_parent_node = node;
				while(res == null && parent_node != null) {
					if (parent_anchor.equals(parent_node)) {
						NodeList anchorList = parent_node.getChildNodes();
						int i = 0, j = 0, i_anchor = -1, i_node = -1, anchorListLength = anchorList.getLength();
						res = "";
						while((i < anchorListLength) && (i_anchor == -1 || i_node == -1)) {
							Node item = anchorList.item(i);
							if (item.getNodeType() == Node.ELEMENT_NODE) {
								if (item == ex_parent_anchor)i_anchor = j;
								if (item == ex_parent_node)i_node = j;
								j++;
							}
							i++;
						}
						for (i = 0; i < level - 1; i++) {
							res += "/..";
						}
						if (i_anchor != -1 && i_node != -1) {
							res_sibling = i_anchor < i_node ?
									"following-sibling::*[" + (i_node - i_anchor) + "]"
									: "preceding-sibling::*[" + (i_anchor - i_node) + "]";
							res += '/' + res_sibling;
						} else if (i_anchor != -1 || level == 1) {
							res += "/..";
						}
						String xpathFromParent = XMLUtils.calcXpath(node, getParentOrOwner(getXpathApi().selectSingleNode(dom,anchor_s + res)));
						return res + ((xpathFromParent.length() > 0)? '/' + xpathFromParent : "");
					}
					ex_parent_node = parent_node;
					parent_node = getParentOrOwner(parent_node);
				}
				ex_parent_anchor = parent_anchor;
				parent_anchor = getParentOrOwner(parent_anchor);
				level++;
			}
		} catch (TransformerException e) {
			ConvertigoPlugin.logException(e, "error during calcRelativeXpath");
		}
		return "";
	}
	
	private Node getParentOrOwner(Node node) {
		return (node instanceof Attr)?((Attr)node).getOwnerElement():node.getParentNode();
	}
	
	private Document getXpathData(Document document, String xPath) {
		if (document == null) {
			return null;
		}
		if (xPath == null) {
			return null;
		}

		try {
			Document doc = XMLUtils.getDefaultDocumentBuilder().newDocument();
			Element root =  (Element) doc.createElement("root"); 
			doc.appendChild(root);
			
			NodeList nl = getXpathApi().selectNodeList(document, xPath);
			if (nl != null)
				for (int i = 0; i< nl.getLength(); i++) {
					Node node = doc.importNode(nl.item(i), true);
					Element elt = doc.getDocumentElement();
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						elt.appendChild(node);
					}
					if (node.getNodeType() == Node.TEXT_NODE) {
						elt.appendChild(node);
					}
					if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
						elt.setAttribute(node.getNodeName() + "_" + i, node.getNodeValue());
					}
				}

			return doc;
		} catch (TransformerException e) {
			ConvertigoPlugin.logWarning("Error for xpath : '" + xPath + "'\r " + e.getMessage());
			return null;
		}
	}

	protected String makeAbsoluteXPath(Node node) {
		class Nodes {
			Object obj;
			Nodes(Object obj) {
				this.obj = obj;
			}
			int getLength() {
				return obj instanceof NodeList ?
					((NodeList) obj).getLength()
					: ((NamedNodeMap) obj).getLength();
			}
			Node item(int index) {
				return obj instanceof NodeList ?
					((NodeList) obj).item(index)
					: ((NamedNodeMap)obj).item(index);
			}
		}
		
		String newXpath = XMLUtils.calcXpath(node);
		Node root = node.getOwnerDocument().getFirstChild();
		int len = 0;
		for (int j = 0; j < 2 && len == 0; j++) {
			Nodes nodes = new Nodes(j == 0 ? root.getChildNodes() : root.getAttributes());
			len = nodes.getLength();
			int index = newXpath.indexOf('/', 5);
			newXpath = index != -1 ? newXpath.substring(index) : "";
			
			if (len > 1) {
				Node cur = node;
				int i = len;
				while (i == len && !cur.equals(root)) {
					for (i = 0; i < len && !cur.equals(nodes.item(i)); i++);
					cur = cur instanceof Attr ? ((Attr) cur).getOwnerElement() : cur.getParentNode();
				}
				if (i != len) {
					newXpath = "(" + lastEval + ")[" + (i + 1) + "]" + newXpath;
				}
			}
		}
		if (len == 1) {
			newXpath = lastEval + newXpath;
		}
		
		return newXpath;
	}
	
	abstract public void setSelectedXpath(String newXpath);
	
	protected MenuMaker makeShowInBrowserMenuMaker() {
		return new MenuMaker() {
			public void makeMenu(final TwsDomTree tree, TreeItem treeItem, MouseEvent e, Menu menu) {
				if (e.button == 3 && treeItem != null) {
					final Object object = treeItem.getData();
					if (object instanceof Node) {
						MenuItem item = new MenuItem(menu, SWT.NONE);
						item.setText("Show in browser");
						item.addSelectionListener(new SelectionListener() {
							public void widgetSelected(SelectionEvent e) { 
								String newXpath = makeAbsoluteXPath((Node) object);
								setSelectedXpath(newXpath);
							}
							public void widgetDefaultSelected(SelectionEvent e) {
							}
						});
					}
				}
			}
		};
	}

	protected void generateSelectionXpath(boolean overwrite, TwsDomTree tree) {
		String newXpath = generateSelectionXpath(tree.getSelection());
		if (!overwrite && lastEval != null) {
			newXpath = lastEval + newXpath;
		}
		setXpathText(newXpath);
		performCalcXpath();		
	}
	
	protected void generateAbsoluteXpath(boolean overwrite, Node node) {
		String newXpath = overwrite ? XMLUtils.calcXpath(node) : makeAbsoluteXPath(node);
		if (currentAnchor != null) {
			newXpath = currentAnchor + calcRelativeXpath(getDom(), currentAnchor, newXpath);
		}
		setXpathText(newXpath);
		performCalcXpath();		
	}
	
	private boolean canGenerateSelectionXpath(boolean overwrite, Node node) {
		boolean bSelectionMenu = (currentAnchor == null);
		
		if (bSelectionMenu && !overwrite) {
			Node curNode = node instanceof Attr ? ((Attr) node).getOwnerElement() : node;
			Node root = node.getOwnerDocument().getFirstChild();
			if (curNode.equals(root)) {
				bSelectionMenu = false;
			} else {
				NodeList nodeList = root.getChildNodes();
				for (int i = 0; i < nodeList.getLength() && bSelectionMenu; i++) {
					bSelectionMenu = !nodeList.item(i).equals(curNode);
				}
			}
		}
		return bSelectionMenu;
	}
	
	public MenuMaker makeXPathMenuMaker(final boolean overwrite) {
		return new MenuMaker() {
			public void makeMenu(final TwsDomTree tree, TreeItem treeItem, MouseEvent e, Menu menu) {
				if ((e.button == 3) && (treeItem != null)) {
					Object object = treeItem.getData();
					if (object instanceof Node) {
						final Node node = (Node)object;
						MenuItem item;
						
						if (overwrite || lastEval != null) {
							if (canGenerateSelectionXpath(overwrite, node)) { // check if show SelectionMenu is necessary
								item = new MenuItem(menu, SWT.NONE);
								item.setText("Generate selection Xpath (Enter)");
								item.addSelectionListener(new SelectionListener() {
									public void widgetSelected(SelectionEvent e) {
										generateSelectionXpath(overwrite, tree);
									}
									public void widgetDefaultSelected(SelectionEvent e) {
									}
								});
							}
							
							item = new MenuItem(menu, SWT.NONE);
							item.setText("Generate absolute Xpath (Shift+Enter)");
							item.addSelectionListener(new SelectionListener() {
								public void widgetSelected(SelectionEvent e) { 
									generateAbsoluteXpath(overwrite, node);
								}
								public void widgetDefaultSelected(SelectionEvent e) {
								}
							});
						}
					}
				}
			}

		};
	}
	
	public KeyAccelerator makeXPathKeyAccelerator(final boolean overwrite) {
		return new KeyAccelerator() {
			public boolean doAction(TwsDomTree tree, KeyEvent e) {
				boolean doNext = true;
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					if (tree.getSelection().length > 0) {
						Node node = (Node)tree.getSelection()[0].getData();
						if ((e.stateMask & SWT.SHIFT) == 0) {//without SHIFT > relative
							if (canGenerateSelectionXpath(overwrite, node)) {
								generateSelectionXpath(overwrite, tree);
								doNext = false;
							}
						} else {//with SHIFT > absolute
							generateAbsoluteXpath(overwrite, node);
							doNext = false;
						}
					}
				}
				return doNext;
			}
		};
	}
	
	protected void moveHistory(boolean backward) {
		int newHistory = backward ?
				Math.min(xpathHistory.size(), currentHistory + 1)
				: Math.max(0, currentHistory - 1);
		if (newHistory != currentHistory) {
			currentHistory = newHistory;
			if (!isAnchorDisabled) {
				currentAnchor = null;
			}
			setXpathText(xpathHistory.get(xpathHistory.size() - 1 - currentHistory));
			refreshButtonsEnable();
		}
	}

	public TwsDomTree getNodesResult() {
		return nodesResult;
	}

	protected String getAnchor() {
		return currentAnchor;
	}
	
	public StyledText getXpath() {
		return xpath;
	}
	
	public Label getLabel() {
		return lab;
	}
}
