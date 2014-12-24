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

import com.twinsoft.convertigo.engine.util.ParameterUtils;

public class GetServerUpdatesTransaction extends AbstractServerTransaction {

	private static final long serialVersionUID = -4009927560249117525L;
	
	public GetServerUpdatesTransaction() {
		super();
	}

	@Override
	public GetServerUpdatesTransaction clone() throws CloneNotSupportedException {
		GetServerUpdatesTransaction clonedObject =  (GetServerUpdatesTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	public List<CouchDbParameter> getDeclaredParameters() {
		return Arrays.asList(new CouchDbParameter[] {var_feed, var_timeout, var_heartbeat});
	}
	
	@Override
	protected Object invoke() {
		String sFeed = ParameterUtils.toString(getParameterValue(var_feed));
		String feed = (sFeed == null) ? null:String.valueOf(sFeed);
		String sTimeout = ParameterUtils.toString(getParameterValue(var_timeout));
		Integer timeout = (sTimeout == null) ? null:Integer.valueOf(sTimeout);
		String sHeartbeat = ParameterUtils.toString(getParameterValue(var_heartbeat));
		Boolean heartbeat = (sHeartbeat == null) ? null:Boolean.valueOf(sHeartbeat);
		return getCouchDbContext().updates(feed, timeout, heartbeat);
	}
}
