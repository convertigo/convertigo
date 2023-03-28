/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.engine.billing;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.NOPLogger;

import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.MySSLSocketFactory;
import com.twinsoft.convertigo.engine.ProxyManager;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.util.UuidUtils;
import com.twinsoft.tas.Key;
import com.twinsoft.tas.KeyManager;

public class GoogleAnalyticsTicketManager implements ITicketManager {
	private Logger log;
	protected ProxyManager proxyManager;
	private HttpClient GAClient;
	private String[] urls = { "http://www.google-analytics.com/collect" };
	private String analyticsID;
	private String overrideCustomer = null;
	private long nextCheck = 0;
	
	private HttpClient prepareHttpClient(String[] url) throws EngineException,	MalformedURLException {
		final Pattern scheme_host_pattern = Pattern.compile("https://(.*?)(?::([\\d]*))?(/.*|$)");

		HttpClient client = new HttpClient();
		HostConfiguration hostConfiguration = client.getHostConfiguration();
		HttpState httpState = new HttpState();
		client.setState(httpState);
		if (proxyManager != null) {
			proxyManager.getEngineProperties();
			proxyManager.setProxy(hostConfiguration, httpState, new URL(url[0]));
		}
		
		Matcher matcher = scheme_host_pattern.matcher(url[0]);
		if (matcher.matches()) {
			String host = matcher.group(1);
			String sPort = matcher.group(2);
			int port = 443;
		
			try {
				port = Integer.parseInt(sPort);
			} catch (Exception e) {
			}
		
			try {
				Protocol myhttps = new Protocol("https",
												MySSLSocketFactory.getSSLSocketFactory(null, null, null, null, true),
												port);
				
				hostConfiguration.setHost(host, port, myhttps);
				url[0] = matcher.group(3);
			} catch (Exception e) {
				e.printStackTrace();
				e.printStackTrace();
			}
		}
		return client;
	}
	
	public GoogleAnalyticsTicketManager(String analyticsID, Logger log) throws BillingException {
		if (analyticsID.length() == 0) {
			throw new BillingException("Google Analytics ID must not be empty");
		}
		this.analyticsID = analyticsID;
		this.log = log;
		this.log.info("(GoogleAnalyticsTicketManager) initialized");
		try {
			GAClient = prepareHttpClient(urls);
		} catch (EngineException e) {
			log.error("(GoogleAnalyticsTicketManager) error creating GAanalytics HttpClient", e);
		} catch (MalformedURLException e) {
			log.error("(GoogleAnalyticsTicketManager) error creating GAanalytics HttpClient, bad url", e);
		}
	}

	public synchronized void addTicket(Ticket ticket) throws BillingException {
		if (log.isDebugEnabled()) {
			log.debug("(GoogleAnalyticsTicketManager) addTicket " + ticket);
		}
		PostMethod method = new PostMethod(urls[0]);
		HeaderName.ContentType.setRequestHeader(method, MimeType.WwwForm.value());

		// set parameters for POST method
		method.setParameter("v", "1");
		method.setParameter("tid", analyticsID);
		
		String cid = ticket.getDeviceUUID();
		if (cid.isEmpty()) {
			cid = ticket.getSessionID();
		}
		try {
			cid = UuidUtils.toUUID(cid).toString();
		} catch (Exception e) {
			log.debug("(GoogleAnalyticsTicketManager) failed to get DeviceUUID", e);
		}
		
		method.setParameter("cid", cid);
		method.setParameter("uid", ticket.getUserName());
		method.setParameter("uip", ticket.getClientIp());
		
		method.setParameter("t", "event");
		method.setParameter("an", ticket.getProjectName());
		method.setParameter("ec", ticket.getProjectName());
		method.setParameter("el", getCustomer(ticket.getCustomerName()));
		
		StringBuffer requestableName = new StringBuffer(ticket.getConnectorName());
		if (requestableName.length() > 0) {
			requestableName.append('.');
		}
		method.setParameter("ea", requestableName.append(ticket.getRequestableName()).toString());
		method.setParameter("ev", Long.toString(ticket.getResponseTime()));
		method.setParameter("ua", ticket.getUserAgent());
		
		// execute HTTP post with parameters
		if (GAClient != null) {
			try {
				int httpCode = GAClient.executeMethod(method);
				String body = method.getResponseBodyAsString();
				log.debug("[" + httpCode + "] " + body);
			} catch (MalformedURLException e) {
				log.error("(GoogleAnalyticsTicketManager) error creating GAanalytics HttpClient, bad url", e);
			} catch (HttpException e) {
				log.error("(GoogleAnalyticsTicketManager) error creating GAanalytics HttpClient, Error in HTTP request", e);
			} catch (IOException e) {
				log.error("(GoogleAnalyticsTicketManager) error creating GAanalytics HttpClient, I/O error", e);
			}
		}
	}

	public synchronized void removeTicket(Ticket ticket) throws BillingException {
		log.warn("(GoogleAnalyticsTicketManager) removeTicket not implemenented");
	}
	
	public Ticket newTicket() throws BillingException {
		return new Ticket();
	}
	
	public synchronized void destroy() throws BillingException {
	}
	
	private String getCustomer(String customer) {
		if (!(log instanceof NOPLogger)) {
			return customer;
		}
		
		long now = System.currentTimeMillis();
		if (now < nextCheck) {
			return overrideCustomer;
		}
		
		if ("CONVERTIGO Server".equals(customer)) {
			Iterator<?> iter = KeyManager.keys.values().iterator();
			Key seKey = null;
			while (iter.hasNext()) {
				Key key = (Key) iter.next();
				if (key.emulatorID == com.twinsoft.api.Session.EmulIDSE) {
					// check (unlimited key or currentKey expiration date later than previous)
					if ((seKey == null) || (key.expiration == 0) || (key.expiration >= seKey.expiration)) {
						seKey = key;
					}
				}
			}
			if (seKey != null) {
				overrideCustomer = Integer.toString(990000000 + seKey.licence);
			} else {
				overrideCustomer = customer;
			}
		} else {
			overrideCustomer = customer;
		}
		
		nextCheck = now + 600000;
		return overrideCustomer;
	}
}
