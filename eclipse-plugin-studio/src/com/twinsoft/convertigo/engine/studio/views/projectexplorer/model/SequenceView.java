package com.twinsoft.convertigo.engine.studio.views.projectexplorer.model;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.editors.IEditorPartWrap;
import com.twinsoft.convertigo.engine.studio.responses.sequences.SequenceExecuteSelectedOpenSequenceEditorResponse;

public class SequenceView extends DatabaseObjectView {

    public SequenceView(Sequence sequence, WrapStudio studio) {
        super(sequence, studio);
    }

    public ProjectView getProjectView() {
        return (ProjectView) getParent();
    }

    @Override
    public Sequence getObject() {
        return (Sequence) super.getObject();
    }

    public void launchEditor() {
//        // Retrieve the project name
//        String projectName = getObject().getProject().getName();
//        try {
//            // Refresh project resource
//            ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
//
            // Open editor
            openSequenceEditor();

//        } catch (CoreException e) {
//            ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
//        }
    }
    
    public void openSequenceEditor() {
        // To add listeners
        getSequenceEditor(getObject());

        synchronized (studio) {
            try {
                studio.createResponse(
                    new SequenceExecuteSelectedOpenSequenceEditorResponse(getObject())
                        .toXml(studio.getDocument(), getObject().getQName())
                );
            }
            catch (Exception e1) {
            }

            studio.notify();

            try {
                studio.wait();
            }
            catch (InterruptedException e) {
            }
        }
    }

    private IEditorPartWrap getSequenceEditor(Sequence sequence) {
        return getProjectView().getSequenceEditor(sequence);
    }
}
