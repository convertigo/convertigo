/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */
package com.twinsoft.convertigo.beans.transactions.couchdb;

import java.util.Arrays;
import java.util.List;

import com.twinsoft.convertigo.engine.EngineException;

public class PostReplicateTransaction extends AbstractDatabaseTransaction {
	
	private static final long serialVersionUID = -2917791679287718055L;

	public PostReplicateTransaction() {
		super();
	}

	@Override
	public PostReplicateTransaction clone() throws CloneNotSupportedException {
		PostReplicateTransaction clonedObject =  (PostReplicateTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	public List<CouchDbParameter> getDeclaredParameters() {
		return Arrays.asList(new CouchDbParameter[] {});
	}

	@Override
	protected Object invoke() throws Exception {
		throw new EngineException("Not yet implemented");
	}

}
