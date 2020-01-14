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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.studio.CheStudio;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.property_editors.StepSourceXpathEvaluatorCompositeWrap;
import com.twinsoft.convertigo.engine.studio.responses.sourcepicker.SourcePickerViewFillHelpContentResponse;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.StepSourceEventWrap;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.StepSourceListenerWrap;

public class SourcePickerViewWrap implements StepSourceListenerWrap {

    private final static String show_step_source = "Show step's source";
    private final static String show_variable_source = "Show variable's source";

    private CheStudio studio;
    private SourcePickerHelperWrap sourcePicker;
    private DatabaseObject selectedDbo;

    public SourcePickerViewWrap(WrapStudio studio) {
        this.studio = (CheStudio) studio;
        sourcePicker = new SourcePickerHelperWrap(studio);
        createXPathEvaluator();
    }

    @Override
    public void sourceSelected(StepSourceEventWrap stepSourceEvent) {
        sourcePicker.getTwsDomTree().removeAll();
        sourcePicker.getXpathEvaluator().removeAnchor();
        sourcePicker.getXpathEvaluator().setXpathText("");

        DatabaseObject dbo = (DatabaseObject) stepSourceEvent.getSource();
        String xpath  = stepSourceEvent.getXPath();
        String priority = "" + dbo.priority;
        XMLVector<String> stepSourceDefinition = new XMLVector<String>();
        stepSourceDefinition.add(priority);
        stepSourceDefinition.add(xpath);
        sourcePicker.setStepSourceDefinition(stepSourceDefinition);
        selectedDbo = dbo;
        fillHelpContent();
        sourcePicker.displayTargetWsdlDom(dbo);
    }

    private void fillHelpContent() {
        String tag = "", type = "", name = "", comment = "";
        String textBtn = show_step_source;
        boolean enableBtn = false;
        if (selectedDbo != null) {
            if (selectedDbo instanceof Step) {
                tag = ((Step) selectedDbo).getStepNodeName();
            }

            type = selectedDbo.getClass().getSimpleName();
            name = selectedDbo.getName();
            comment = selectedDbo.getComment();
            textBtn = (selectedDbo instanceof Step) ? show_step_source : show_variable_source;
            if (selectedDbo instanceof IStepSourceContainer) {
                enableBtn = !((IStepSourceContainer) selectedDbo).getSourceDefinition().isEmpty();
            }
        }

        synchronized (studio) {
            try {
                String qname = selectedDbo == null ? null : selectedDbo.getQName();

                studio.createResponse(
                    new SourcePickerViewFillHelpContentResponse(tag, type, name, comment, textBtn, enableBtn)
                        .toXml(studio.getDocument(), qname)
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

    private void createXPathEvaluator() {
        sourcePicker.createXPathEvaluator(new StepSourceXpathEvaluatorCompositeWrap(studio, sourcePicker));
    }

    public Element getXpathTree(String nodeId) {
        return sourcePicker.getXpathTree(nodeId);
    }

    public void updateStudio(WrapStudio studio) {
        this.studio = (CheStudio) studio;
        sourcePicker.updateStudio(this.studio);
    }

    public String getXpath(String nodeId) {
        return sourcePicker.getXpath(nodeId);
    }

    public String getAnchor() {
        return sourcePicker.getAnchor();
    }

    public Document getDom() {
        return sourcePicker.getDom();
    }

    public void modifyXpathText(String xpath) {
        sourcePicker.modifyXpathText(xpath);
    }

    public Object getDragData() {
        return sourcePicker.getDragData();
    }
}
