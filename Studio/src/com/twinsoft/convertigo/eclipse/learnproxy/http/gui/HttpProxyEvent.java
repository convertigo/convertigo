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

package com.twinsoft.convertigo.eclipse.learnproxy.http.gui;

public class HttpProxyEvent {

	private String contextID;
	
	private String request;

	private String response;

	private long elapsedTime;

	private String status;

	private long requestStarted;

	private long size;

	private String path;

	private String method;

	private boolean https;
	
	
	public HttpProxyEvent() {
		
	}
	
	public HttpProxyEvent(String request, String response, String path,
			String status, long elapsedTime, long requestStarted,
			String method, long size) {
		
		this(request, response, path, status, elapsedTime, requestStarted, method, size, false);
	}

	public HttpProxyEvent(String request, String response, String path,
			String status, long elapsedTime, long requestStarted,
			String method, long size, boolean https) {
		this(null, request, response, path, status, elapsedTime, requestStarted, method, size, false);
		
	}
	
	public HttpProxyEvent(String contextID, String request, String response, String path,
						String status, long elapsedTime, long requestStarted,
						String method, long size, boolean https) {
		this.contextID = contextID;
		this.request = request;
		this.response = response;
		this.status = status;
		this.elapsedTime = elapsedTime;
		this.requestStarted = requestStarted;
		this.method = method;
		this.size = size;
		this.path = path;
		this.https = https;
	}
	
	/**
	 * @return the elapsedTime
	 */
	public long getElapsedTime() {
		return elapsedTime;
	}

	/**
	 * @param elapsedTime the elapsedTime to set
	 */
	public void setElapsedTime(long elapsedTime) {
		this.elapsedTime = elapsedTime;
	}

	/**
	 * @return the https
	 */
	public boolean isHttps() {
		return https;
	}

	/**
	 * @param https the https to set
	 */
	public void setHttps(boolean https) {
		this.https = https;
	}

	/**
	 * @return the method
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * @param method the method to set
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return the request
	 */
	public String getRequest() {
		return request;
	}

	/**
	 * @param request the request to set
	 */
	public void setRequest(String request) {
		this.request = request;
	}

	/**
	 * @return the requestStarted
	 */
	public long getRequestStarted() {
		return requestStarted;
	}

	/**
	 * @param requestStarted the requestStarted to set
	 */
	public void setRequestStarted(long requestStarted) {
		this.requestStarted = requestStarted;
	}

	/**
	 * @return the response
	 */
	public String getResponse() {
		return response;
	}

	/**
	 * @param response the response to set
	 */
	public void setResponse(String response) {
		this.response = response;
	}

	/**
	 * @return the size
	 */
	public long getSize() {
		return size;
	}

	/**
	 * @param size the size to set
	 */
	public void setSize(long size) {
		this.size = size;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return the contextID
	 */
	public String getContextID() {
		return contextID;
	}

	/**
	 * @param contextID the contextID to set
	 */
	public void setContextID(String contextID) {
		this.contextID = contextID;
	}

}
