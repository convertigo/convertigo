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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;

public class DomTreeUtils {
	private	TreeItem selectedTreeItem = null;
	private Image imageAttrib = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/attrib.png"));
	private Image imageNode = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/node.png"));
	
	/**
	 * Fills an SWT Tree object with a parsed XML document
	 * 
	 * @param tree			the SWT Tree Object
	 * @param document		the parsed XML document
	 */
	public void fillDomTree(final Tree tree, final Document document){
		selectedTreeItem = null;
		IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
		if (progressService == null)
			return ;
		try {
			progressService.runInUI(
					PlatformUI.getWorkbench().getProgressService(),
					new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) {
							monitor.beginTask("Filling tree with HTML data", 1000);
							tree.removeAll();	
							monitor.subTask("Existing data cleared");
							monitor.worked(1);
							NodeList nodes = document.getChildNodes();
							for (int i=0; i < nodes.getLength() ; i++) {
								if (!addNodeInTree(tree, nodes.item(i), monitor))
									return;
							}
							monitor.worked(1000);
							monitor.done();
						}
					},
					null
			);
		} catch (InvocationTargetException e) {
			ConvertigoPlugin.logException(e, "Invokation target exception");
		} catch (InterruptedException e) {
			ConvertigoPlugin.logDebug("Interrupted exception");
		}
	}
	
	/**
	 * Adds a node in the visual tree. This method is used by the @see fillDomTree method 
	 *  
	 * @param parent 	the parent (Can be the tree or a parent TreeItem)
	 * @param node		the node to be added
	 */
	public boolean addNodeInTree(Object parent, Node node, IProgressMonitor monitor){
		TreeItem 	tItem;
		String[]	values = new String[2];

		if (parent instanceof Tree) {
			tItem = new TreeItem((Tree)parent, SWT.NONE);
		}
		else {
			tItem = new TreeItem((TreeItem)parent,  SWT.NONE);
		}

		// associate our node with the tree Item.
		tItem.setData(node);

		// calc the node text according to the node type
		switch (node.getNodeType()) {
		case Node.ELEMENT_NODE :
			if (node.hasAttributes()) {
				TreeItem attributesItem = new TreeItem(tItem, SWT.NONE);
				attributesItem.setText("Attributes");
				attributesItem.setImage(imageAttrib);
				NamedNodeMap nnm = node.getAttributes();
				for (int i=0; i< nnm.getLength() ; i++) {
					TreeItem attrItem = new TreeItem(attributesItem, SWT.NONE);
					attrItem.setText(nnm.item(i).getNodeName()+ "=\"" +nnm.item(i).getNodeValue()+"\"");
					attrItem.setImage(imageAttrib);
					attrItem.setData(nnm.item(i));
				}
			}
			//  monitor.subTask("Importing node : " + node.getNodeName() + " ("+getTextValue(node).trim()+")");
			monitor.worked(1);
			if (monitor.isCanceled())
				return false;

			values[0] = node.getNodeName();
			values[1] = getTextValue(node);

			tItem.setText(values);
			tItem.setImage(imageNode);
			break;

		case Node.TEXT_NODE  :
			if (node.getNodeValue().trim().length() == 0)  {
				tItem.dispose();
			} else 
				tItem.setText(node.getNodeValue().trim());
			break;

		case Node.ATTRIBUTE_NODE:
			tItem.setText("[Attrib]");
			break;

		case Node.ENTITY_NODE:
			tItem.setText("[Entity]");
			break;

		case Node.ENTITY_REFERENCE_NODE :
			tItem.setText("[Entityref]");
			break;

		case Node.PROCESSING_INSTRUCTION_NODE   :
			tItem.setText("[Pi]");
			break;

		case Node.COMMENT_NODE   :
			tItem.setText("[Comment]");
			break;

		case Node.DOCUMENT_FRAGMENT_NODE   :
			tItem.setText("[Docfgmt]");
			break;

		case Node.DOCUMENT_TYPE_NODE  :
			tItem.setText("[Doctype]");
			break;

		case Node.NOTATION_NODE   :
			tItem.setText("[Notation]");
			break;

		default:
			break;
		}

		// now recurse if we have child nodes
		if (node.hasChildNodes()) {
			NodeList nl = node.getChildNodes();
			for (int i =0; i< nl.getLength() ; i++) {
				addNodeInTree(tItem, nl.item(i), monitor);
			}
		}
		return true;
	}
	
	/**
	 * Recurses to the first text node and gets its text value
	 * 
	 * @param node	the node to get the text value from.
	 * @return
	 */
	private String getTextValue(Node node)
	{
		if (node != null)
			if (node.getFirstChild() != null)
				if (node.getFirstChild().getNodeType() == Node.TEXT_NODE )
					return (node.getFirstChild().getNodeValue());
		return "";
	}
	
	/**
	 * Highlights an element in the XHTML display tree. This method is called
	 * recursivly to parse all the tree child elements.
	 *  
	 * @param elt		element to be highlighted
	 * @param parent	SWT parent (the TreeObject itself)
	 * @return			null if no element found, the XHTML node if found
	 */
	public Node highLightElementInTree(Node elt, Object parent, int colour)
	{
		TreeItem 	treeItem;
		Tree	 	tree;
		TreeItem[] 	items;

		try {
			// according to the class of the parent, get the fisrt items
			if (parent instanceof Tree) {
				tree = (Tree)parent;
				items = tree.getItems();
			}
			else {
				treeItem = (TreeItem)parent;
				items = treeItem.getItems(); 
			}

			// scan all items to match the one that have as data our searched node
			for (int i =0 ; i< items.length; i++) {
				if (((Node)items[i].getData()) != null) {
					if (((Node)items[i].getData()).equals(elt)) {
						// Set previous Item to normal colour if exists
						if (selectedTreeItem != null) 
							highLightTreeItem(selectedTreeItem, null);

						// Set TreeItem to Special Colour
						highLightTreeItem(items[i], Display.getDefault().getSystemColor(colour));

						// Scroll the item in view
						items[i].getParent().showItem(items[i]);

						// prepare for next selection
						selectedTreeItem = items[i]; 

						// node was found, return it
						return ((Node)items[i].getData());
					}
				}

				// Node was not found in this level, try in childs
				Node selectedNode = highLightElementInTree(elt, items[i], colour);
				if (selectedNode != null ) {
					return selectedNode;
				}
			}
			return null;
		} catch (Exception e) {
			//log.exception(e, "error in highlighting the item");
			return null;
		}
	}
	
	/**
	 * highlights the specified TreeItem with a special colour . The node and all
	 * its childs will be highlighted
	 * 
	 * @param item		the item to be highlighted
	 * @param color		the color to be used, null to set as default colour
	 */
	private void highLightTreeItem(TreeItem item, Color color)
	{
		item.setBackground(color);
		for (int i =0; i < item.getItemCount(); i++) {
			highLightTreeItem(item.getItem(i), color);
		}
	}
}
