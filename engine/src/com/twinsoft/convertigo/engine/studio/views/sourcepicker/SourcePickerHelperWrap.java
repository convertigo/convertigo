/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.engine.studio.views.sourcepicker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaObject;
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
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.SchemaManager.Option;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.dnd.StepSourceWrap;
import com.twinsoft.convertigo.engine.studio.editors.connectors.htmlconnector.TwsDomTreeWrap;
import com.twinsoft.convertigo.engine.studio.property_editors.IStepSourceEditorWrap;
import com.twinsoft.convertigo.engine.studio.property_editors.StepXpathEvaluatorCompositeWrap;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;
import com.twinsoft.util.StringEx;

public class SourcePickerHelperWrap implements IStepSourceEditorWrap {

    private TwsDomTreeWrap twsDomTree;
    private Document currentDom;
    private StepXpathEvaluatorCompositeWrap xpathEvaluator;

    private XMLVector<String> stepSourceDefinition;
    private TwsCachedXPathAPI twsCachedXPathAPI = new TwsCachedXPathAPI();
    private final static String REGEXP_FOR_PREDICATES = "\\[\\D{1,}\\]";

    public SourcePickerHelperWrap(WrapStudio studio) {
        twsDomTree = new TwsDomTreeWrap(studio);
    }

    public void setStepSourceDefinition(XMLVector<String> stepSourceDefinition) {
        this.stepSourceDefinition = stepSourceDefinition;
    }

    public void displayTargetWsdlDom(DatabaseObject dbo) {
        try {
            if (dbo instanceof Step) {
                Step step = (Step) dbo;
                String xpath = getSourceXPath();
                String anchor = step.getAnchor();
                Document stepDoc = null;

                Step targetStep = step;
                while (targetStep instanceof IteratorStep) {
                    targetStep = getTargetStep(targetStep);
                }

                if (targetStep != null) {
                    Project project = step.getProject();
                    XmlSchema schema = Engine.theApp.schemaManager.getSchemaForProject(project.getName(), Option.fullSchema);
                    XmlSchemaObject xso = SchemaMeta.getXmlSchemaObject(schema, targetStep);
                    if (xso != null) {
                        stepDoc = XmlSchemaUtils.getDomInstance(xso);   
                    }
                }

                if (stepDoc != null/* && !(targetStep instanceof IteratorStep)*/) { // stepDoc can be null for non "xml" step : e.g jIf
                    Document doc = step.getSequence().createDOM();
                    Element root = (Element) doc.importNode(stepDoc.getDocumentElement(), true);
                    doc.replaceChild(root, doc.getDocumentElement());
                    removeUserDefinedNodes(doc.getDocumentElement());
                    boolean shouldDisplayDom = (!(!step.isXml() && (step instanceof StepWithExpressions) && !(step instanceof IteratorStep)));
                    if ((doc != null) && (shouldDisplayDom)) {
                        xpath = onDisplayXhtml(xpath);
                        displayXhtml(doc);
                        xpathEvaluator.removeAnchor();
                        xpathEvaluator.displaySelectionXpathWithAnchor(twsDomTree, anchor, xpath);
                        return;
                    }
                }
            }
        }
        catch (Exception e) {
            //ConvertigoPlugin.logException(e, StringUtils.readStackTraceCauses(e));
        }
        clean();        
    }

    @Override
    public void selectItemsInTree(String[] items) {
        // TODO Auto-generated method stub

    }

    @Override
    public String[] findTreeItems(String xpath) {
        String[] items = new String[]{};
        try {
            xpath = xpath.replaceAll(REGEXP_FOR_PREDICATES, "");

            NodeList nl = twsCachedXPathAPI.selectNodeList(currentDom, xpath);
            if (nl.getLength() > 0) {
                String tItem = twsDomTree.findTreeItem(nl.item(0));
                List<String> v = new ArrayList<>();
                while (tItem != null) {
                    v.add(tItem);
                    //tItem = tItem.getParentItem();
                }
                items = v.toArray(new String[]{});
            }
        }
        catch (TransformerException e) {
            //ConvertigoPlugin.logException(e, "Error while finding items in tree");
        }
        return items;
    }
    
    private void removeUserDefinedNodes(Element parent) {
        HashSet<Node> toRemove = new HashSet<>();

        NodeList list = parent.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                ((Element) node).removeAttribute("done");
                ((Element) node).removeAttribute("hashcode");
                if (node.getNodeName().equals("schema-type")) {
                    toRemove.add(node);
                }
                else {
                    removeUserDefinedNodes((Element) node);
                }
            }
        }
        Iterator<Node> it = toRemove.iterator();
        while (it.hasNext()) {
            parent.removeChild(it.next());
        }
        toRemove.clear();
    }

    protected void clean() {
        setSourceXPath(".");
        twsDomTree.removeAll();
        xpathEvaluator.removeAnchor();
    }

    private void setSourceXPath(String xpath) {
        stepSourceDefinition.set(1, xpath);
    }

    public void displayXhtml(Document dom){
        try {
            currentDom = dom;
            twsDomTree.fillDomTree(dom);
        }
        catch (Exception e) {
            //ConvertigoPlugin.logException(e, "Error while filling DOM tree");
        }
    }

    public String getSourceXPath() {
        return stepSourceDefinition.get(1);
    }

    private Step getTargetStep(Step step) throws EngineException {
        if (step != null && (step instanceof IStepSourceContainer)) {
            com.twinsoft.convertigo.beans.core.StepSource source = new com.twinsoft.convertigo.beans.core.StepSource(step, ((IStepSourceContainer) step).getSourceDefinition());
            if (source != null && !source.isEmpty()) {
                return source.getStep();
            }
            else {
                return null;
            }
        }
        return step;
    }

    protected String onDisplayXhtml(String xpath) {
        return xpath;
    }

    @Override
    public Document getDom() {
        return currentDom;
    }

    public TwsDomTreeWrap getTwsDomTree() {
        return twsDomTree;
    }

    public void createXPathEvaluator(StepXpathEvaluatorCompositeWrap xpathEvaluatorComposite) {
        xpathEvaluator = xpathEvaluatorComposite;
    }

    public Element getXpathTree(String nodeId) {
        Node node = twsDomTree.getNode(nodeId);
        String xpath = getXpathEvaluator().generateAbsoluteXpath(true, node, true);
        modifyXpathText(xpath);
        Document doc = getXpathData(currentDom, xpath);
        Node[] childs = XMLUtils.toNodeArray(doc.getChildNodes());
        return TwsDomTreeWrap.getTree2(doc, childs[0], "xpath_tree");
    }

    public StepXpathEvaluatorCompositeWrap getXpathEvaluator() {
        return xpathEvaluator;
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

            NodeList nl = twsCachedXPathAPI.selectNodeList(document, xPath);
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

    public void updateStudio(WrapStudio studio) {
        xpathEvaluator.updateStudio(studio);
        twsDomTree.updateStudio(studio);
    }

    public String getXpath(String nodeId) {
        Node node = twsDomTree.getNode(nodeId);
        return xpathEvaluator.generateAbsoluteXpath(true, node, true);
    }

    public String getAnchor() {
        return xpathEvaluator.getAnchor();
    }

    public void modifyXpathText(String xpath) {
        xpathEvaluator.setXpathText(xpath, true);
        String anchor = xpathEvaluator.getAnchor();
        StringEx sx = new StringEx(xpathEvaluator.getXpath());
        sx.replace(anchor, ".");
        String text = sx.toString();
        if (!text.equals("")) {
            setSourceXPath(text);
        }        
    }

    public Object getDragData() {
        return new StepSourceWrap(getStepSourceDefinition().get(0), getSourceXPath());
    }

    public XMLVector<String> getStepSourceDefinition() {
        return stepSourceDefinition;
    }
}
