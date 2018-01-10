package com.twinsoft.convertigo.beans.core;

import com.twinsoft.convertigo.engine.EngineException;

public interface IContainerOrdered {
	public void increasePriority(DatabaseObject databaseObject) throws EngineException;
	public void decreasePriority(DatabaseObject databaseObject) throws EngineException;

	/**
	 * Add a DatabaseObject at a specific position.
	 * 
	 * @param databaseObject the database object to add.
	 * @param after the Priority property of the DatabaseObject.
     *              The new DatabaseObject will be inserted after it.
     *              If after is 0, the new DatabaseObject at will be inserted the beginning.
     *              If after null, the new DatabaseObject at will be inserted the end.
     *              
     * @exception EngineException if an engine exception occurred.
	 */
	public void add(DatabaseObject databaseObject, Long after) throws EngineException;
}
