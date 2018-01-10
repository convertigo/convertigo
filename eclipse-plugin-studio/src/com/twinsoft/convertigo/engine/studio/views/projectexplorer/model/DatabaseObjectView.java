package com.twinsoft.convertigo.engine.studio.views.projectexplorer.model;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.studio.CheStudio;
import com.twinsoft.convertigo.engine.studio.Studio;
import com.twinsoft.convertigo.engine.studio.WrapStudio;

public class DatabaseObjectView implements WrapDatabaseObject {

    protected DatabaseObject dbo;
	protected CheStudio studio;

	public DatabaseObjectView(DatabaseObject dbo, WrapStudio studio) {
		this.dbo = dbo;
        this.studio = (CheStudio) studio;
	}

	@Override
	public DatabaseObject getObject() {
	    return dbo;
	}

	@Override
	public WrapDatabaseObject getParent() {
	    return Studio.getViewFromDbo(dbo.getParent(), studio);
	}

	@Override
	public String getName() {
		return dbo.getName();
	}

	@Override
	public void hasBeenModified(boolean hasBeenModified) {
	}

    @Override
    public ProjectView getProjectViewObject() {
        return new ProjectView(dbo.getProject(), studio);
    }
}
