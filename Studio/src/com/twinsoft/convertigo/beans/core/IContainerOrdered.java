package com.twinsoft.convertigo.beans.core;

import com.twinsoft.convertigo.engine.EngineException;

public interface IContainerOrdered {
	public void increasePriority(DatabaseObject databaseObject) throws EngineException;
	public void decreasePriority(DatabaseObject databaseObject) throws EngineException;
}
