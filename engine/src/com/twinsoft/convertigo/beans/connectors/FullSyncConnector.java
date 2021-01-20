/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

package com.twinsoft.convertigo.beans.connectors;

import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractDatabaseTransaction;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.FullSyncAnonymousReplication;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;
import com.twinsoft.convertigo.engine.providers.couchdb.FullSyncContext;

public class FullSyncConnector extends CouchDbConnector {
	private static final long serialVersionUID = 4063707392313093177L;
	
	private FullSyncAnonymousReplication anonymousReplication = FullSyncAnonymousReplication.deny; 
	
	@Override
	public CouchClient getCouchClient() {
		return Engine.theApp.couchDbManager.getFullSyncClient();
	}
	
	@Override
	public String getDatabaseName() {
		return getName();
	};
	
	@Override
	public String getTargetDatabase(AbstractDatabaseTransaction couchDbTransaction) {
		return getDatabaseName();
	}
	
	@Override
	public void setName(String name) throws EngineException {
		if (!name.matches("^[a-z][a-z0-9_$()+/-]*$")) {
			throw new EngineException("Must begin with a letter with only lowercase characters, digits and _");
		}
		super.setName(name);
	}

	@Override
	public void beforeTransactionInvoke() {
		FullSyncContext.get().setAuthenticatedUser(context.getAuthenticatedUser());
	}

	@Override
	public void afterTransactionInvoke() {
		FullSyncContext.unset();
	}

	public FullSyncAnonymousReplication getAnonymousReplication() {
		return anonymousReplication;
	}

	public void setAnonymousReplication(FullSyncAnonymousReplication anonymousReplication) {
		this.anonymousReplication = anonymousReplication;
	}
		
}
