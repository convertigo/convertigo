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

package com.twinsoft.convertigo.engine;

public class RequestableEngineEvent extends EngineEvent {
	private static final long serialVersionUID = -6581623177818044078L;

	private String projectName;
	private String sequenceName;
	private String connectorName;

	public RequestableEngineEvent(Object source, String projectName, String sequenceName, String connectorName) {
		super(source);
		
		this.projectName = projectName;
		this.sequenceName = sequenceName;
		this.connectorName = connectorName;
	}

	public String getProjectName() {
		return projectName;
	}
	
	public String getSequenceName() {
		return sequenceName;
	}
	
	public String getConnectorName() {
		return connectorName;
	}
}
