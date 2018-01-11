package com.twinsoft.convertigo.engine.studio.views.projectexplorer.actions;

import com.twinsoft.convertigo.engine.studio.AbstractRunnableAction;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.SequenceView;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapDatabaseObject;

public class LaunchSequenceEditorAction extends AbstractRunnableAction {

    public LaunchSequenceEditorAction(WrapStudio studio) {
        super(studio);
    }

    @Override
    protected void run2() throws Exception {
        try {
            WrapDatabaseObject treeObject = (WrapDatabaseObject) studio.getFirstSelectedTreeObject();
            ((SequenceView) treeObject).launchEditor();
        }
        catch (Exception e) {
            throw e;
        }
    }
}
