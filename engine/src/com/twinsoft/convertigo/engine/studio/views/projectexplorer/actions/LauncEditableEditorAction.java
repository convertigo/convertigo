package com.twinsoft.convertigo.engine.studio.views.projectexplorer.actions;

import com.twinsoft.convertigo.engine.studio.AbstractRunnableAction;
import com.twinsoft.convertigo.engine.studio.WrapStudio;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.IEditableTreeViewWrap;
import com.twinsoft.convertigo.engine.studio.views.projectexplorer.model.WrapDatabaseObject;

public class LauncEditableEditorAction extends AbstractRunnableAction {

    public LauncEditableEditorAction(WrapStudio studio) {
        super(studio);
    }

    @Override
    protected void run2() throws Exception {
        try {
            WrapDatabaseObject treeObject = (WrapDatabaseObject) studio.getFirstSelectedTreeObject();
            ((IEditableTreeViewWrap) treeObject).launchEditor(null);
        }
        catch (Exception e) {
            throw e;
        }
    }
}
