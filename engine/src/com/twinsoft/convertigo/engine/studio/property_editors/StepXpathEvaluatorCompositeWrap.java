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
