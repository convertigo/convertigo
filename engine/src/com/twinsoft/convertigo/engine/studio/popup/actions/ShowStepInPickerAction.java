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

package com.twinsoft.convertigo.engine.studio.popup.actions;

import java.util.Set;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.studio.AbstractRunnableAction;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.StepSourceEventWrap;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.DatabaseObjectView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapDatabaseObject;
import com.twinsoft.convertigo.engine.studio.views.sourcepicker.SourcePickerViewWrap;

public class ShowStepInPickerAction extends AbstractRunnableAction {

    protected boolean showSource = false;

    public ShowStepInPickerAction(WrapStudio studio) {
        super(studio);
    }

    @Override
    protected void run2() throws Exception {
        try {
            WrapDatabaseObject treeObject = (WrapDatabaseObject) studio.getFirstSelectedTreeObject();
            if (treeObject != null) {
                if (treeObject.instanceOf(DatabaseObject.class)) {
                    DatabaseObject selectedDbo = ((DatabaseObjectView) treeObject).getObject();
                    if (selectedDbo != null) {
                        StepSourceEventWrap event = null;
                        if (showSource) {
                            if (selectedDbo instanceof Step) {
                                Step step = (Step) selectedDbo;
                                Set<StepSource> sources = step.getSources();
                                if (!sources.isEmpty()) {
                                    event = new StepSourceEventWrap(sources.iterator().next());
                                }
                                else {
                                    throw new Exception("No Source defined");
                                }
                            }
                        }
                        else {
                            event = new StepSourceEventWrap(selectedDbo);
                        }

                        if (event != null) {
                            SourcePickerViewWrap spv = studio.getSourcePickerView();
                            spv.sourceSelected(event);
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            throw e;
            // ConvertigoPlugin.logException(e, "Unable to show object in Picker!");
        }
    }
}
