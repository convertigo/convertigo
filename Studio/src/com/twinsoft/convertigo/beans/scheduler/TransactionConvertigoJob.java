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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.scheduler;

import java.util.List;
import java.util.Map;

import com.twinsoft.convertigo.engine.enums.Parameter;

public class TransactionConvertigoJob extends AbstractConvertigoJob {
	public static final String prob_noConnector = "the connector name cannot be empty";
	public static final String prob_noTransaction = "the transaction name cannot be empty";
	
	private String connectorName = null;
	private String transactionName = null;
	
	public String getConnectorName() {
		return connectorName;
	}
	
	public void setConnectorName(String connectorName) {
		this.connectorName = connectorName;
	}
	
	public String getTransactionName() {
		return transactionName;
	}
	
	public void setTransactionName(String transactionName) {
		this.transactionName = transactionName;
	}
	
	@Override
	protected void addParameters(Map<String, String[]> parameters) {
		if (connectorName != null && connectorName.length() > 0) {
			parameters.put(Parameter.Connector.getName(), new String[]{connectorName});
		}
		if (transactionName != null && transactionName.length() > 0) {
			parameters.put(Parameter.Transaction.getName(), new String[]{transactionName});
		}
		super.addParameters(parameters);
	}
	
	@Override
	public void checkProblems(List<String> problems) {
		super.checkProblems(problems);
		if (connectorName == null || connectorName.length() == 0) {
			problems.add(prob_noConnector);
		}
		if (transactionName == null || transactionName.length() == 0) {
			problems.add(prob_noTransaction);
		}
	}
}
