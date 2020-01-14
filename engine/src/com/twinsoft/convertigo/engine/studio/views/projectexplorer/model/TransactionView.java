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

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.responses.projectexplorer.actions.OpenEditableEditorActionResponse;
import com.twinsoft.convertigo.engine.util.FileUtils;

public class TransactionView extends DatabaseObjectView implements IEditableTreeViewWrap {

    private final static String JSCRIPT_TRANSACTION_EDITOR = "c8o_jscripttransactioneditor";
    
    public TransactionView(Transaction transaction, WrapStudio studio) {
        super(transaction, studio);
    }

    public ConnectorView getConnectorTreeObject() {
        return new ConnectorView(getObject().getConnector(), studio);
    }

    public ProjectView getProjectView() {
        return (ProjectView) getParent().getParent();
    }

    @Override
    public Transaction getObject() {
        return (Transaction) super.getObject();
    }

    @Override
    public void launchEditor(String editorType) {
//        // Retrieve the project name
//        String projectName = getObject().getProject().getName();
//        try {
//            // Refresh project resource
//            IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
//
            // Open editor
            if (editorType == null || (editorType != null && editorType.equals("JscriptTransactionEditor"))) {
                openJscriptTransactionEditor();
            }
//            if ((editorType != null) && (editorType.equals("XMLTransactionEditor")))
//                openXMLTransactionEditor(project);
//            
//        } catch (CoreException e) {
//            ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
//        }
    }

    private void openJscriptTransactionEditor() {
        Project project = dbo.getProject();
        try {
            FileUtils.createFolderIfNotExist(project.getDirPath(), "_private");

            String fileName = FileUtils.createTmpFileWithUTF8Data(
                project.getDirPath(),
                "_private" + "/" + dbo.getProject().getName() + "__" + getObject().getConnector().getName() + "__" + getObject().getName() + "." + JSCRIPT_TRANSACTION_EDITOR,
                getObject().handlers
            );

            studio.createResponse(
                new OpenEditableEditorActionResponse(
                    project.getQName() + "/_private/" + fileName,
                    JSCRIPT_TRANSACTION_EDITOR
                ).toXml(studio.getDocument(), getObject().getQName())
            );
        }
        catch (Exception e) {
        }
    }
}
