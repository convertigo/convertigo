package com.twinsoft.convertigo.engine.studio.views.projectexplorer.model;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.steps.SimpleStep;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.responses.projectexplorer.actions.OpenEditableEditorActionResponse;
import com.twinsoft.convertigo.engine.util.FileUtils;

public class StepView extends DatabaseObjectView implements IEditableTreeViewWrap {

    private final static String JSCRIPT_STEP_EDITOR = "c8o_jscriptstepeditor";

    public StepView(Step dbo, WrapStudio studio) {
        super(dbo, studio);
    }

    @Override
    public void launchEditor(String editorType) {
        // Retrieve the project name
       // String projectName = ((DatabaseObject) getObject()).getProject().getName();
//        try {
            // Refresh project resource
            //IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);

            // Get editor type
            if (editorType == null) {
                if (((DatabaseObject) getObject()) instanceof SimpleStep) {
                    editorType = "JscriptStepEditor";
                }
            }

            // Open editor
            if ((editorType != null) && (editorType.equals("JscriptStepEditor"))) {
                openJscriptStepEditor();
            }
//            if ((editorType != null) && (editorType.equals("XMLTransactionStepEditor"))) {
//                //openXMLTransactionStepEditor(project);
//            }
//            if ((editorType != null) && (editorType.equals("XMLSequenceStepEditor"))) {
//                //openXMLSequenceStepEditor(project);
//            }
//        }
//        catch (CoreException e) {
//            ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
//        }
    }

    private void openJscriptStepEditor() {
        Project project = dbo.getProject();
        try {
            // Create private folder if it does not exist
            FileUtils.createFolderIfNotExist(project.getDirPath(), "_private");

            String fileName = FileUtils.createTmpFileWithUTF8Data(
                project.getDirPath(),
                "_private" + "/" + Base64.encodeBase64URLSafeString(DigestUtils.sha1(dbo.getQName())) + " " + dbo.getName() + "." + JSCRIPT_STEP_EDITOR,
                ((SimpleStep) dbo).getExpression()
            );

            studio.createResponse(
                new OpenEditableEditorActionResponse(
                    project.getQName() + "/" + "_private" + "/" +  fileName,
                    JSCRIPT_STEP_EDITOR
                ).toXml(studio.getDocument(), getObject().getQName())
            );
        }
        catch (Exception e) {
        }
    }
}
