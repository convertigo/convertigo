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

public abstract class AbstractServerTransaction extends AbstractCouchDbTransaction {

	private static final long serialVersionUID = 2498769820246402945L;

	protected static final CouchDbParameter var_section = CouchDbParameter.Path_section;
	protected static final CouchDbParameter var_key 	= CouchDbParameter.Path_key;
	protected static final CouchDbParameter var_value 	= CouchDbParameter.Path_value;
	protected static final CouchDbParameter var_bytes 	= CouchDbParameter.Param_log_bytes;
	protected static final CouchDbParameter var_offset 	= CouchDbParameter.Param_log_offset;
	protected static final CouchDbParameter var_feed 	= CouchDbParameter.Param_feed;
	protected static final CouchDbParameter var_timeout = CouchDbParameter.Param_timeout;
	protected static final CouchDbParameter var_heartbeat = CouchDbParameter.Param_heartbeat;
	protected static final CouchDbParameter var_count 	= CouchDbParameter.Param_count;
	protected static final CouchDbParameter var_name 	= CouchDbParameter.Param_user_name;
	protected static final CouchDbParameter var_password = CouchDbParameter.Param_user_password;
	
	
	public AbstractServerTransaction() {
		super();
	}

	@Override
	public AbstractServerTransaction clone() throws CloneNotSupportedException {
		AbstractServerTransaction clonedObject =  (AbstractServerTransaction) super.clone();
		return clonedObject;
	}
}
