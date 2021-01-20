/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

package com.twinsoft.convertigo.engine.studio.editors.connectors.htmlconnector;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.studio.CheStudio;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.responses.sourcepicker.TwsDomTreeFillDomTreeResponse;
import com.twinsoft.convertigo.engine.studio.responses.sourcepicker.TwsDomTreeRemoveAllResponse;

public class TwsDomTreeWrap {

    private CheStudio studio;
    private Map<String, Node> idToNodes;
    private int nbNodes;

    public TwsDomTreeWrap(WrapStudio studio) {
        this.studio = (CheStudio) studio;
        idToNodes = new HashMap<>();
        nbNodes = 0;
    }

    public void removeAll() {
        synchronized (studio) {
            idToNodes.clear();
            nbNodes = 0;
            try {
                studio.createResponse(
                    new TwsDomTreeRemoveAllResponse()
                        .toXml(studio.getDocument(), null)
                );
            }
            catch (Exception e) {
            }

            studio.notify();

            try {
                studio.wait();
            }
            catch (InterruptedException e) {
            }
        }
    }

    public void fillDomTree(final Document document) {
        //selectedTreeItem = null;

        removeAll();
        if (document != null) {
            synchronized (studio) {
                try {
                    Element domTree = getTree(document);
                    studio.createResponse(
                        new TwsDomTreeFillDomTreeResponse(domTree)
                            .toXml(studio.getDocument(), null)
                    );
                }
                catch (Exception e) {
                }

                studio.notify();

                try {
                    studio.wait();
                }
                catch (InterruptedException e) {
                }
            } 
        }
    }

    private Element getTree(Document document) {
        Element domTree = studio.getDocument().createElement("dom_tree");
        addNodeInTree(document.getChildNodes().item(0), studio.getDocument(), domTree);
        return domTree;
    }

    private void addNodeInTree(Node node, Document document, Element parent) {
        Element currentElement = createElement(document, node);
        currentElement.setAttribute("type", Short.toString(node.getNodeType()));

        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
                currentElement.setAttribute("text", node.getNodeName());
                NamedNodeMap map = node.getAttributes();
                if (node.hasAttributes()) {
                    Element attributes = createElement(document, node, false);
                    attributes.setAttribute("text", "Attributes");
                    attributes.setAttribute("type", Short.toString(Node.ATTRIBUTE_NODE));
                    currentElement.appendChild(attributes);

                    for (int i = 0; i < map.getLength(); ++i) {
                        Element attribute = createElement(document, map.item(i));
                        attribute.setAttribute("text", map.item(i).getNodeName() + "=\"" + map.item(i).getNodeValue() + "\"");
                        attribute.setAttribute("type", Short.toString(Node.ATTRIBUTE_NODE));
                        attributes.appendChild(attribute);
                    }
                }
                break;

            case Node.TEXT_NODE:
                currentElement.setAttribute("text", node.getNodeValue() == null ? "" : node.getNodeValue().trim());
                break;

            case Node.ENTITY_NODE:
                currentElement.setAttribute("text", "[Entity]");
                break;

            case Node.ENTITY_REFERENCE_NODE:
                currentElement.setAttribute("text", "[Entityref]");
                break;

            case Node.PROCESSING_INSTRUCTION_NODE:
                currentElement.setAttribute("text", "[Pi]");
                break;

            case Node.COMMENT_NODE:
                currentElement.setAttribute("text", "[Comment]");
                break;

            case Node.DOCUMENT_FRAGMENT_NODE:
                currentElement.setAttribute("text", "[Docfgmt]");
                break;

            case Node.DOCUMENT_TYPE_NODE:
                currentElement.setAttribute("text", "[Doctype]");
                break;

            case Node.NOTATION_NODE:
                currentElement.setAttribute("text", "[Notation]");
                break;

            default:
                break;
        }
        parent.appendChild(currentElement);

        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);
            addNodeInTree(currentNode, document, currentElement);
        }
    }

    private Element createElement(Document document, Node node) {
        return createElement(document, node, true);
    }

    private Element createElement(Document document, Node node, boolean isRealNode) {
        Element newElement = document.createElement("node");
        String nodeId = "dom_tree_node" + Integer.toString(++nbNodes);
        newElement.setAttribute("id", nodeId);
        if (isRealNode) {
            newElement.setAttribute("is_real", "true");
            idToNodes.put(nodeId, node);
        }
        else {
            newElement.setAttribute("is_real", "false");
        }
        return newElement;
    }

    public static Element getTree2(Document document, Node node, String rootTagName) {
        Element treeElement = document.createElement(rootTagName);
        return TwsDomTreeWrap.getTree2(node, document, treeElement);
    }

    private static Element getTree2(Node node, Document document, Element parent) {
        int [] index = new int[1];
        index[0] = 0;
        addNodeInTree2(node, document, parent, index);
        return parent;
    }

    private static void addNodeInTree2(Node node, Document document, Element parent, int [] index) {
        Element currentElement = createElement2(document, node, index);
        currentElement.setAttribute("type", Short.toString(node.getNodeType()));

        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
                currentElement.setAttribute("text", node.getNodeName());
                NamedNodeMap map = node.getAttributes();
                if (node.hasAttributes()) {
                    Element attributes = createElement2(document, node, index);
                    attributes.setAttribute("text", "Attributes");
                    attributes.setAttribute("type", Short.toString(Node.ATTRIBUTE_NODE));
                    currentElement.appendChild(attributes);

                    for (int i = 0; i < map.getLength(); ++i) {
                        Element attribute = createElement2(document, map.item(i), index);
                        attribute.setAttribute("text", map.item(i).getNodeName() + "=\"" + map.item(i).getNodeValue() + "\"");
                        attribute.setAttribute("type", Short.toString(Node.ATTRIBUTE_NODE));
                        attributes.appendChild(attribute);
                    }
                }
                break;
    
            case Node.TEXT_NODE:
                currentElement.setAttribute("text", node.getNodeValue() == null ? "" : node.getNodeValue().trim());
                break;
    
            case Node.ENTITY_NODE:
                currentElement.setAttribute("text", "[Entity]");
                break;
    
            case Node.ENTITY_REFERENCE_NODE:
                currentElement.setAttribute("text", "[Entityref]");
                break;
    
            case Node.PROCESSING_INSTRUCTION_NODE:
                currentElement.setAttribute("text", "[Pi]");
                break;
    
            case Node.COMMENT_NODE:
                currentElement.setAttribute("text", "[Comment]");
                break;
    
            case Node.DOCUMENT_FRAGMENT_NODE:
                currentElement.setAttribute("text", "[Docfgmt]");
                break;
    
            case Node.DOCUMENT_TYPE_NODE:
                currentElement.setAttribute("text", "[Doctype]");
                break;
    
            case Node.NOTATION_NODE:
                currentElement.setAttribute("text", "[Notation]");
                break;
    
            default:
                break;
        }
        parent.appendChild(currentElement);

        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);
            addNodeInTree2(currentNode, document, currentElement, index);
        }
    }

    private static Element createElement2(Document document, Node node, int [] index) {
        Element newElement = document.createElement("node");
        String nodeId = "dom_tree_node" + Integer.toString(++index[0]);
        newElement.setAttribute("id", nodeId);
        return newElement;
    }

    public Node getNode(String nodeId) {
        return idToNodes.get(nodeId);        
    }

    public void updateStudio(WrapStudio studio) {
        this.studio = (CheStudio) studio;
    }

    public String findTreeItem(Node elt) {
        for (Entry<String, Node> idToNode: idToNodes.entrySet()) {
            if (elt.isSameNode(idToNode.getValue())) {
                return idToNode.getKey();
            }
        }

        return null;
    }
}
