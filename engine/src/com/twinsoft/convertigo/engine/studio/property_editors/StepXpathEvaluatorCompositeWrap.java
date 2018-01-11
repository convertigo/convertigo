package com.twinsoft.convertigo.engine.studio.property_editors;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.editors.connectors.htmlconnector.TwsDomTreeWrap;
import com.twinsoft.convertigo.engine.studio.editors.connectors.htmlconnector.XpathEvaluatorCompositeWrap;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;

public class StepXpathEvaluatorCompositeWrap extends XpathEvaluatorCompositeWrap {

    private IStepSourceEditorWrap stepSourceEditorComposite;
    private TwsCachedXPathAPI twsCachedXPathAPI;

    public StepXpathEvaluatorCompositeWrap(WrapStudio studio, IStepSourceEditorWrap stepSourceEditorComposite) {
        super(studio);
        this.stepSourceEditorComposite = stepSourceEditorComposite;
        noPredicate = true;
    }

    @Override
    public String getAnchor() {
        return currentAnchor;
    }

    public void removeAnchor() {
        super.removeAnchor();
    }

    public String[] findTreeItems(String newXpath) {
        return stepSourceEditorComposite.findTreeItems(newXpath);
    }

    public void displaySelectionXpathWithAnchor(TwsDomTreeWrap twsDomTree, String anchor, String xpath) {
//        String[] items;
//
//        items = findTreeItems(anchor);
//        if (items.length > 0) {
//            stepSourceEditorComposite.selectItemsInTree(items);
//        }
        setXpathText(anchor);
        setAnchor(true);

        if (!xpath.equals("")) {
            xpath = xpath.replaceFirst("\\.", anchor);
//            items = findTreeItems(xpath);
//            if (items.length > 0) {
//                stepSourceEditorComposite.selectItemsInTree(items);
//            }
            setXpathText(xpath);
        }        
    }

    public String generateAbsoluteXpath(boolean overwrite, Node node, boolean fromService) {
        return super.generateAbsoluteXpath(overwrite, node, fromService);
    }

    public String generateAbsoluteXpath(boolean overwrite, Node node) {
        return super.generateAbsoluteXpath(overwrite, node);
    }

    @Override
    public TwsCachedXPathAPI getXpathApi() {
        if (twsCachedXPathAPI == null) {
            twsCachedXPathAPI = new TwsCachedXPathAPI();
        }

        return twsCachedXPathAPI;
    }

    public Document getDom() {
        return stepSourceEditorComposite.getDom();
    }
}
