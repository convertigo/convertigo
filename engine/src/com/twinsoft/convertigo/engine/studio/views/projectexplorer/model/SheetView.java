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
