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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.engine.util.XMLUtils;

public class TwsDomTree extends TreeWrapper {
	
	private	TreeItem selectedTreeItem = null;
	private Image imageAttrib = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/attrib.png"));
	private Image imageNode = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/node.png"));
	private Image imageText = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/text.png"));
	private List<MenuMaker> menuMakers = new ArrayList<MenuMaker>();
	private List<KeyAccelerator> keyAccelerators =  new ArrayList<KeyAccelerator>();
	
	public interface MenuMaker {
		void makeMenu(TwsDomTree tree, TreeItem treeItem, MouseEvent e, Menu menu);
	}
	
	public interface KeyAccelerator {
		boolean doAction(TwsDomTree tree, KeyEvent e);
	}
	
	public TwsDomTree(Composite parent, int style) {
		super(parent, style|SWT.VIRTUAL);
		addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseDown(MouseEvent e) {
				buildContextMenu(e);
			}
			
		});
		addKeyListener(new KeyListener() {
			
			public void keyPressed(KeyEvent e) {
				doKeyAction(e);
			}
			
			public void keyReleased(KeyEvent e) {	
			}
			
		});
		addListener(SWT.SetData, new Listener() {
			public void handleEvent(Event event) {
				TreeItem item = (TreeItem) event.item;
				TreeItem parent = item.getParentItem();
				Node node;
				Node[] nodes;
				Integer dec;
				if (parent != null) {
					node = (Node) parent.getData();
					nodes = (Node[]) parent.getData("childs");
					dec = (Integer) parent.getData("dec");
				} else { // RACINE
					node = (Node) item.getParent().getData();
					nodes = (Node[]) item.getParent().getData("childs");
					dec = (Integer) item.getParent().getData("dec");
				}
				int d = (dec != null) ? dec.intValue() : 0;
				if (d == 1 && event.index == 0) { // create fake 'Attributes' item
					item.setText("Attributes");
					item.setImage(imageAttrib);
					item.setItemCount(node.getAttributes().getLength());
				} else {
					if (node == null) { // parent is 'Attributes'
						node = (Node) parent.getParentItem().getData();
						node = node.getAttributes().item(event.index);
					} else {
						node = nodes[event.index - d];
					}
					addNodeInTree(item, node, null);
					if (parent == null) {
						item.setExpanded(true);
					}
				}
			}
		});
		addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) { }
			
			public void focusLost(FocusEvent e) {
				if (selectedTreeItem != null) {
					deselectAll();
				}
			}
		});
	}

	/**
	 * Fills an SWT Tree object with a parsed XML document
	 * 
	 * @param document		the parsed XML document
	 */
	public void fillDomTree(final Document document) {
		selectedTreeItem = null;
		
		removeAll();
		if (document != null) {
			Tree t = getTree();
			Node[] childs = XMLUtils.toNodeArray(document.getChildNodes());
			t.setData(document);
			t.setData("childs", childs);
			t.setItemCount(childs.length);
		}
	}
	
	/**
	 * Adds a node in the visual tree. This method is used by the @see fillDomTree method 
	 *  
	 * @param parent 	the parent (Can be the tree or a parent TreeItem)
	 * @param node		the node to be added
	 */
	public boolean addNodeInTree(Object parent, Node node, IProgressMonitor monitor) {
		TreeItem tItem = (TreeItem) parent;
		String[] values = new String[2];
		tItem.setData(node);
		// calc the node text according to the node type
		switch (node.getNodeType()) {
		case Node.ELEMENT_NODE :
			int dec = 0;
			if (node.hasAttributes()) {// add a fake first node for 'Attributes' item
				tItem.setData("dec", new Integer(dec = 1));
			}
			Node[] childs = XMLUtils.toNodeArray(node.getChildNodes());
			tItem.setData("childs", childs);
			tItem.setItemCount(childs.length + dec);
			
			values[0] = node.getNodeName();
			values[1] = getTextValue(node);
			
			tItem.setText(values);
			tItem.setImage(imageNode);
			break;
		case Node.TEXT_NODE  :
			tItem.setImage(imageText);
			tItem.setText(node.getNodeValue().trim());
			break;
		case Node.ATTRIBUTE_NODE:
			tItem.setImage(imageAttrib);
			String str = node.getNodeName() + "=\"" + node.getNodeValue() + "\"";
			tItem.setText(new String[] {str, str});
			break;
		case Node.ENTITY_NODE:
			tItem.setText("[Entity]");
			break;
		case Node.ENTITY_REFERENCE_NODE :
			tItem.setText("[Entityref]");
			break;
		case Node.PROCESSING_INSTRUCTION_NODE :
			tItem.setText("[Pi]");
			break;
		case Node.COMMENT_NODE :
			tItem.setText("[Comment]");
			break;
		case Node.DOCUMENT_FRAGMENT_NODE :
			tItem.setText("[Docfgmt]");
			break;
		case Node.DOCUMENT_TYPE_NODE :
			tItem.setText("[Doctype]");
			break;
		case Node.NOTATION_NODE :
			tItem.setText("[Notation]");
			break;
		default: break;
		}
		
		return true;
	}
	
	/**
	 * Recurses to the first text node and gets its text value
	 * 
	 * @param node	the node to get the text value from.
	 * @return
	 */
	private String getTextValue(Node node) {
		if (node != null) {
			if (node.getFirstChild() != null) {
				if (node.getFirstChild().getNodeType() == Node.TEXT_NODE ) {
					return node.getFirstChild().getNodeValue();
				}
			}
		}
		return "";
	}
	
	public TreeItem findTreeItem(Node elt) {
		return findTreeItem(elt, this);
	}
	
	public TreeItem findTreeItem(Node elt, Object parent) {
		Tree tree = getTree();
		TreeItem treeItem;
		TreeItem[] items;

		try {
			// according to the class of the parent, get the fisrt items
			if (parent instanceof TwsDomTree) {
				items = tree.getItems();
			} else {
				treeItem = (TreeItem) parent;
				items = treeItem.getItems(); 
			}

			// scan all items to match the one that have as data our searched node
			for (int i =0 ; i< items.length; i++) {
				Node data = getTreeItemData(items[i]);
				if (data != null) {
					if (data.equals(elt)) {
						return items[i];
					}
				}

				// Node was not found in this level, try in childs
				TreeItem foundItem = findTreeItem(elt, items[i]);
				if (foundItem != null ) {
					return foundItem;
				}
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}
	
	public void selectTreeItem(TreeItem ti) {
		if (selectedTreeItem != null) {
			selectedTreeItem.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		}
		selectedTreeItem = ti;
		selectedTreeItem.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
		//deselectAll();
	}
	
	public Node selectElementInTree(Node elt) {
		return selectElementInTree(elt, this);
	}
	
	public Node selectElementInTree(Node elt, Object parent) {
		Tree tree = getTree();
		TreeItem treeItem;
		TreeItem[] items;

		try {
			// according to the class of the parent, get the fisrt items
			if (parent instanceof TwsDomTree) {
				items = tree.getItems();
			} else {
				treeItem = (TreeItem) parent;
				items = treeItem.getItems(); 
			}

			// scan all items to match the one that have as data our searched node
			for (int i = 0; i < items.length; i++) {
				Node data = getTreeItemData(items[i]);
				if (data != null) {
					if (data.equals(elt)) {
						// node was found, return it
						//tree.setSelection(items[i]);
						tree.showItem(items[i]);
						Event event = new Event();
						event.item = items[i];
						tree.notifyListeners(SWT.Selection, event);
						return data;
					}
				}

				// Node was not found in this level, try in childs
				Node selectedNode = selectElementInTree(elt, items[i]);
				if (selectedNode != null ) {
					return selectedNode;
				}
			}
			return null;
		} catch (Exception e) {	}
		return null;
	}
	
	private void buildContextMenu(MouseEvent e) {
		Point point = new Point(e.x, e.y);
		TreeItem  treeItem = getTree().getItem(point);
		Menu menu = new Menu((Control)e.getSource());
		Iterator<MenuMaker> i = menuMakers.iterator();
		int cpt = 0;
		while(i.hasNext()) {
			i.next().makeMenu(this, treeItem, e, menu);
			if (i.hasNext() && menu.getItemCount() != cpt) {
				new MenuItem(menu, SWT.SEPARATOR);
				cpt = menu.getItemCount();
			}
		}
		if (menu.getItemCount() > 0) {
			menu.setVisible(true);
		} else {
			menu.dispose();
		}
	}
	
	public void addMenuMaker(MenuMaker menuMaker) {
		menuMakers.add(menuMaker);
	}
	
	public void addKeyAccelerator(KeyAccelerator keyAccelerator) {
		keyAccelerators.add(keyAccelerator);
	}
	
	public Document getDocument() {
		TreeItem top = (getItemCount() > 0) ? getItem(0) : null;
		
		if (top != null) {
			Node obj = getTreeItemData(top);
			if (obj != null) {
				return obj.getOwnerDocument();
			}
		}
		return null;
	}
	
	private void doKeyAction(KeyEvent e) {
		boolean run = true;
		Iterator<KeyAccelerator> i = keyAccelerators.iterator();
		while(i.hasNext() && run) {
			run = i.next().doAction(this, e);
		}
	}
	
	private Node getTreeItemData(TreeItem ti) {
		Node ret = (Node) ti.getData();
		if (ret == null) {
			ti.getItems();
			ret = (Node) ti.getData();
		}
		return ret;
	}

	public void collapseAll() {
		getTopItem().setExpanded(false);
	}
	
	public void expandAll(TreeItem treeItem) {
		TreeItem[] treeItems = treeItem.getItems();
		treeItem.setExpanded(true);
		for (TreeItem sub : treeItems) {
			expandAll(sub);
		}
	}

	public void expandAll() {
		expandAll(getTopItem());
	}
}