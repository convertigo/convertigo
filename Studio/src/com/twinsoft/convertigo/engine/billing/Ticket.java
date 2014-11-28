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

package com.twinsoft.convertigo.engine.billing;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

@Entity
public class Ticket {
	private long id;
	protected long creationDate		= 0L;
	protected long responseTime		= 0L;
	protected long score				= 0L;
	
	protected String clientIp 			= "";
	protected String customerName		= "";
	protected String userName			= "";
	protected String projectName		= "";
	protected String connectorName		= "";
	protected String connectorType		= "";
	protected String requestableName	= "";
	protected String requestableType	= "";
	protected String sessionID			= "";
	protected String uiID				= "";
	protected String deviceUUID			= "";
	
	@Transient
	protected String userAgent			= "";

	public Ticket() {
	}

	@Id
	@GeneratedValue
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public long getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(long responseTime) {
		this.responseTime = responseTime;
	}

	public long getScore() {
		return score;
	}

	public void setScore(long score) {
		this.score = score;
	}

	public String getClientIp() {
		return clientIp;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getConnectorName() {
		return connectorName;
	}

	public void setConnectorName(String connectorName) {
		this.connectorName = connectorName;
	}

	public String getConnectorType() {
		return connectorType;
	}

	public void setConnectorType(String connectorType) {
		this.connectorType = connectorType;
	}

	public String getRequestableName() {
		return requestableName;
	}

	public void setRequestableName(String requestableName) {
		this.requestableName = requestableName;
	}

	public String getRequestableType() {
		return requestableType;
	}

	public void setRequestableType(String requestableType) {
		this.requestableType = requestableType;
	}

	public String getSessionID() {
		return sessionID;
	}

	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}

	public String getUiID() {
		return uiID;
	}

	public void setUiID(String uiID) {
		this.uiID = uiID;
	}

	public String getDeviceUUID() {
		return deviceUUID;
	}

	public void setDeviceUUID(String deviceUUID) {
		this.deviceUUID = deviceUUID;
	}
	
	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	@Override
	public String toString() {
		return 	"{creationDate: '" + creationDate + "', responseTime: '" + responseTime + "', score: '" + score + "', clientIp: '" + clientIp + "', customerName: '"
		+ customerName + "', userName: '" + userName + "', projectName: '" + projectName + "', connectorName: '" + connectorName + "', connectorType: '"
		+ connectorType + "', requestableName: '" + requestableName + "', requestableType: '" + requestableType + "', sessionID: '"
		+ sessionID + "', uiID: '" + uiID + "', deviceUUID: '" + deviceUUID + "'}";
	}
}