/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

import javax.xml.transform.TransformerException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.studio.CheStudio;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.responses.connectors.htmlconnector.XpathEvaluatorCompositeRemoveAnchorResponse;
import com.twinsoft.convertigo.engine.studio.responses.connectors.htmlconnector.XpathEvaluatorCompositeSetXpathTextResponse;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class XpathEvaluatorCompositeWrap {

    protected String currentAnchor;
    private String xpath;
    private String lastEval;
//    private List<String> xpathHistory;
//    private int currentHistory = 0;
    private boolean isAnchorDisabled;
    protected boolean noPredicate = false;
    protected TwsDomTreeWrap nodesResult;
    protected CheStudio studio;

    public XpathEvaluatorCompositeWrap(WrapStudio studio) {
        //xpathHistory = new LinkedList<>();
        nodesResult = new TwsDomTreeWrap(studio);
        refreshButtonsEnable();
        this.studio = (CheStudio) studio;
    }

    protected String getAnchor() {
        return currentAnchor;
    }

    abstract public TwsCachedXPathAPI getXpathApi();

    public static Document getXpathData(Document document, String xPath) {
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

            NodeList nl = new TwsCachedXPathAPI().selectNodeList(document, xPath);
            if (nl != null)
                for (int i = 0; i < nl.getLength(); i++) {
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
        }
        catch (TransformerException e) {
            //ConvertigoPlugin.logWarning("Error for xpath : '" + xPath + "'\r " + e.getMessage());
            return null;
        }
    }

    public void removeAnchor() {
        synchronized (studio) {
            isAnchorDisabled = false;
            currentAnchor = null;
            lastEval = null;

            try {
                studio.createResponse(new XpathEvaluatorCompositeRemoveAnchorResponse().toXml(studio.getDocument(), null));
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

        refreshButtonsEnable();
    }

    protected void setAnchor(boolean disabled) {
        //TODO:Button anchor = (Button) buttonsMap.get("anchor");
        isAnchorDisabled = disabled;
        if (currentAnchor == null) {
            currentAnchor = xpath; // xpath.getText();
            //xpath.setStyleRange(new StyleRange(0,currentAnchor.length(),xpath.getForeground(),highlightColor));
            //TODO:anchor.setBackground(highlightColor);
        } else if (!isAnchorDisabled) {
            currentAnchor = null;
            //xpath.setStyleRange(null);
            refreshButtonsEnable();
        }
    }

    public void setXpathText(String nodeXpath) {
        setXpathText(nodeXpath, false);
    }

    public void setXpathText(String nodeXpath, boolean fromService) {
        //TODO:if (currentAnchor != null) nodeXpath = currentAnchor + calcRelativeXpath(htmlDesign.getWebViewer().getDom(), currentAnchor, nodeXpath);
        if (!fromService) {
            synchronized (studio) {
                xpath = nodeXpath; //xpath.setText(nodeXpath);

                lastEval = null;
                refreshButtonsEnable();
                try {
                    studio.createResponse(new XpathEvaluatorCompositeSetXpathTextResponse(xpath, currentAnchor).toXml(studio.getDocument(), null));
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
        else {
            xpath = nodeXpath; //xpath.setText(nodeXpath);
            lastEval = null;
            refreshButtonsEnable();
        }
    }

    abstract public Document getDom();

    protected String generateAbsoluteXpath(boolean overwrite, Node node) {
        return generateAbsoluteXpath(overwrite, node, false);
    }

    protected String generateAbsoluteXpath(boolean overwrite, Node node, boolean fromService) {
        String newXpath = overwrite ? XMLUtils.calcXpath(node) : makeAbsoluteXPath(node);
        if (currentAnchor != null) {
            newXpath = currentAnchor + calcRelativeXpath(getDom(), currentAnchor, newXpath);
        }
        setXpathText(newXpath, fromService);
        performCalcXpath();
        return newXpath;
    }

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

            while (res == null && parent_anchor != null) {
                parent_node = ex_parent_node = node;
                while (res == null && parent_node != null) {
                    if (parent_anchor.equals(parent_node)) {
                        NodeList anchorList = parent_node.getChildNodes();
                        int i = 0, j = 0, i_anchor = -1, i_node = -1, anchorListLength = anchorList.getLength();
                        res = "";
                        while ((i < anchorListLength) && (i_anchor == -1 || i_node == -1)) {
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
                        return res + ((xpathFromParent.length() > 0) ? '/' + xpathFromParent : "");
                    }
                    ex_parent_node = parent_node;
                    parent_node = getParentOrOwner(parent_node);
                }
                ex_parent_anchor = parent_anchor;
                parent_anchor = getParentOrOwner(parent_anchor);
                level++;
            }
        } catch (TransformerException e) {
            //ConvertigoPlugin.logException(e, "error during calcRelativeXpath");
        }
        return "";
    }

    private Node getParentOrOwner(Node node) {
        return (node instanceof Attr) ? ((Attr) node).getOwnerElement() : node.getParentNode();
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

    public void performCalcXpath()  {
        //ConvertigoPlugin.logDebug3("Get xpath node list start");
        Document nodesSelection = getXpathData(getDom(), xpath /*xpath.getText()*/);
        //ConvertigoPlugin.logDebug3("Get xpath node list end");
        if (nodesSelection != null ) {
            //ConvertigoPlugin.logDebug3("Fill xpath node list dom tree start");
            //nodesResult.fillDomTree(nodesSelection);
            lastEval = xpath; //xpath.getText();
//            xpathHistory.add(lastEval);
//            currentHistory = 0;
            refreshButtonsEnable();
            //ConvertigoPlugin.logDebug3("Fill xpath node list dom tree end");
        } else {
            //ConvertigoPlugin.logDebug3("Remove all start");
            nodesResult.removeAll();
            //ConvertigoPlugin.logDebug3("Remove all End");
        }
        //nodeData.setText("");
    }

    protected void refreshButtonsEnable() {

    }

    public void updateStudio(WrapStudio studio) {
        this.studio = (CheStudio) studio;
    }

    public String getXpath() {
        return xpath;
    }
}
