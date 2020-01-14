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

package com.twinsoft.convertigo.engine.studio.views.projectexplorer.model;

import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.responses.projectexplorer.actions.OpenEditableEditorActionResponse;

public class SheetView extends DatabaseObjectView implements IEditableTreeViewWrap {

    private final static String XSL_EDITOR = "c8o_xsleditor";

    public SheetView(Sheet sheet, WrapStudio studio) {
        super(sheet, studio);
    }

    @Override
    public void launchEditor(String editorType) {
//        // Retrieve the project name
//        String projectName = getObject().getProject().getName();
//        try {
//            // Refresh project resource
//            IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);

            // Open editor
            openXslEditor();
//            
//        } catch (CoreException e) {
//            ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
//        }
    }

    @Override
    public Sheet getObject() {
        return (Sheet) super.getObject();
    }

    private void openXslEditor() {
        try {
            studio.createResponse(
                new OpenEditableEditorActionResponse(
                    dbo.getProject().getQName() + "/" + getObject().getUrl(),
                    XSL_EDITOR
                ).toXml(studio.getDocument(), getObject().getQName())
            );
        }
        catch (Exception e) {
        }
    }
}
