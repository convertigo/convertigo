package com.twinsoft.convertigo.engine.studio.wrappers;

import com.twinsoft.convertigo.beans.core.DatabaseObject;

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
}
