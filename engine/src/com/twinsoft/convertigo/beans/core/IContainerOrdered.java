/*
 * Copyright (c) 2001-2024 Convertigo SA.
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
