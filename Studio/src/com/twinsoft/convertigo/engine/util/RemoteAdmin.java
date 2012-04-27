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

package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class RemoteAdmin {

	private HostConfiguration hostConfiguration = null;

	private String serverBaseUrl;
	private boolean bHttps;
	private boolean bTrustAllCertificates;
	private HttpClient httpClient;
	private int serverPort = 0;
	private String host;
	private URL url;

	public RemoteAdmin(String serverBaseUrl, boolean bHttps, boolean bTrustAllCertificates) throws RemoteAdminException {
		this.serverBaseUrl = serverBaseUrl;
		this.bHttps = bHttps;
		this.bTrustAllCertificates = bTrustAllCertificates;
		this.httpClient = new HttpClient();
		
		try {
			url = new URL("http" + (bHttps ? "s" : "") + "://" + serverBaseUrl);
			this.serverPort = url.getPort();
			this.host = url.getHost();
		} catch (MalformedURLException e) {
			throw new RemoteAdminException(
					"The Convertigo server is not valid: " + serverBaseUrl + "\n"
							+ e.getMessage());
		}
	}

	public void login(String username, String password)
			throws RemoteAdminException {
		PostMethod loginMethod = null;
	
		try {
			String loginServiceURL = (bHttps ? "https" : "http") + "://"
					+ serverBaseUrl + "/admin/services/engine.Authenticate";

			Protocol myhttps = null;					
			
			if (bHttps && bTrustAllCertificates) {	
				ProtocolSocketFactory socketFactory = new EasySSLProtocolSocketFactory();
				myhttps = new Protocol("https", socketFactory, serverPort);
				Protocol.registerProtocol("https", myhttps);

				hostConfiguration = httpClient.getHostConfiguration();
				hostConfiguration.setHost(host, serverPort, myhttps);
				httpClient.setHostConfiguration(hostConfiguration);	
			} 
				
			if (("").equals(username) || username == null) {
				throw new RemoteAdminException(
				"Unable to connect to the Convertigo server: \"Server administrator\" field is empty.");
			}
			if (("").equals(password) || password == null) {
				throw new RemoteAdminException(
						"Unable to connect to the Convertigo server: \"Password\" field is empty.");
			}
			
			loginMethod = new PostMethod(loginServiceURL);
			loginMethod.addParameter("authType", "login");
			loginMethod.addParameter("authUserName", username);
			loginMethod.addParameter("authPassword", password);
			
			int returnCode = httpClient.executeMethod(loginMethod);
			String httpResponse = loginMethod.getResponseBodyAsString();

			if (returnCode == HttpStatus.SC_OK) {
				Document domResponse;
				try {
					DocumentBuilder parser = DocumentBuilderFactory
							.newInstance().newDocumentBuilder();
					domResponse = parser.parse(new InputSource(
							new StringReader(httpResponse)));
					domResponse.normalize();

					NodeList nodeList = domResponse
							.getElementsByTagName("error");

					if (nodeList.getLength() != 0) {
						throw new RemoteAdminException(
								"Unable to connect to the Convertigo server: wrong username or password.");
					}
				} catch (ParserConfigurationException e) {
					throw new RemoteAdminException(
							"Unable to parse the Convertigo server response: \n"
									+ e.getMessage() + ".\n"
									+ "Received response: " + httpResponse);
				} catch (IOException e) {
					throw new RemoteAdminException(
							"An unexpected error has occured during the Convertigo server login.\n"
									+ "(IOException) " + e.getMessage() + "\n"
									+ "Received response: " + httpResponse, e);
				} catch (SAXException e) {
					throw new RemoteAdminException(
							"Unable to parse the Convertigo server response: "
									+ e.getMessage() + ".\n"
									+ "Received response: " + httpResponse);
				}
			} else {
				decodeResponseError(httpResponse);
			}
		} catch (HttpException e) {
			throw new RemoteAdminException(
					"An unexpected error has occured during the Convertigo server login.\n"
							+ "Cause: " + e.getMessage(), e);
		} catch (UnknownHostException e) {
			throw new RemoteAdminException(
					"Unable to find the Convertigo server (unknown host): "
							+ e.getMessage());
		} catch (IOException e) {
			String message = e.getMessage();
			
			if (message.indexOf("unable to find valid certification path") != -1) {
				throw new RemoteAdminException(
						"The SSL certificate of the Convertigo server is not trusted.\nPlease check the 'Trust all certificates' checkbox.");
			}
			else throw new RemoteAdminException(
					"Unable to reach the Convertigo server: \n"
							+ "(IOException) " + e.getMessage(), e);
		} catch (GeneralSecurityException e) {
			throw new RemoteAdminException(
					"Unable to reach the Convertigo server: \n"
							+ "(GeneralSecurityException) " + e.getMessage(), e);
		} finally {
			Protocol.unregisterProtocol("https");
			if (loginMethod != null)
				loginMethod.releaseConnection();
		}
	}

	public void deployArchive(File archiveFile, boolean bAssembleXsl) throws RemoteAdminException {

		String deployServiceURL = (bHttps ? "https" : "http") + "://"
				+ serverBaseUrl
				+ "/admin/services/projects.Deploy?bAssembleXsl="
				+ bAssembleXsl;

		PostMethod deployMethod = null;
		Protocol myhttps = null;
		
		try {
			if (bHttps && bTrustAllCertificates) {	
				ProtocolSocketFactory socketFactory = new EasySSLProtocolSocketFactory();
				myhttps = new Protocol("https", socketFactory, serverPort);
				Protocol.registerProtocol("https", myhttps);

				hostConfiguration = httpClient.getHostConfiguration();
				hostConfiguration.setHost(host, serverPort,myhttps);
				httpClient.setHostConfiguration(hostConfiguration);
			}

			deployMethod = new PostMethod(deployServiceURL);

			Part[] parts = { new FilePart(archiveFile.getName(), archiveFile) };
			deployMethod.setRequestEntity(new MultipartRequestEntity(parts, deployMethod.getParams()));

			int returnCode = httpClient.executeMethod(deployMethod);
			String httpResponse = deployMethod.getResponseBodyAsString();
			
			if (returnCode == HttpStatus.SC_OK) {
				Document domResponse;
				try {
					DocumentBuilder parser = DocumentBuilderFactory
							.newInstance().newDocumentBuilder();
					domResponse = parser.parse(new InputSource(
							new StringReader(httpResponse)));
					domResponse.normalize();
	
					NodeList nodeList = domResponse
							.getElementsByTagName("error");
	
					if (nodeList.getLength() != 0) {
						Element errorNode = (Element) nodeList.item(0);
						
						Element errorMessage = (Element) errorNode
								.getElementsByTagName("message").item(0);

						Element exceptionName = (Element) errorNode
								.getElementsByTagName("exception").item(0);

						Element stackTrace = (Element) errorNode
								.getElementsByTagName("stacktrace").item(0);
						
						if (errorMessage != null) {
							throw new RemoteAdminException(
									errorMessage.getTextContent(),
									exceptionName.getTextContent(),
									stackTrace.getTextContent());
						}
						else {
							throw new RemoteAdminException(
									"An unexpected error has occured during the Convertigo project deployment: \n"
											+ "Body content: \n\n"
											+ XMLUtils
													.prettyPrintDOMWithEncoding(
															domResponse,
															"UTF-8"));
						}
					}
				} catch (ParserConfigurationException e) {
					throw new RemoteAdminException(
							"Unable to parse the Convertigo server response: \n"
									+ e.getMessage() + ".\n"
									+ "Received response: " + httpResponse);
				} catch (IOException e) {
					throw new RemoteAdminException(
							"An unexpected error has occured during the Convertigo project deployment.\n"
									+ "(IOException) " + e.getMessage() + "\n"
									+ "Received response: " + httpResponse, e);
				} catch (SAXException e) {
					throw new RemoteAdminException(
							"Unable to parse the Convertigo server response: "
									+ e.getMessage() + ".\n"
									+ "Received response: " + httpResponse);
				}
			} else {
				decodeResponseError(httpResponse);
			}
		} catch (HttpException e) {
			throw new RemoteAdminException(
					"An unexpected error has occured during the Convertigo project deployment.\n"
							+ "Cause: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new RemoteAdminException(
					"Unable to reach the Convertigo server: \n"
							+ "(IOException) " + e.getMessage(), e);
		} catch (GeneralSecurityException e) {
			throw new RemoteAdminException(
					"Unable to reach the Convertigo server: \n"
							+ "(GeneralSecurityException) " + e.getMessage(), e);
		} finally {
			Protocol.unregisterProtocol("https");
			if (deployMethod != null)
				deployMethod.releaseConnection();
		}
	}
	
	private void decodeResponseError(String httpResponse) throws RemoteAdminException {
		Document domResponse;
		try {
			DocumentBuilder parser = DocumentBuilderFactory
					.newInstance().newDocumentBuilder();
			domResponse = parser.parse(new InputSource(
					new StringReader(httpResponse)));
			domResponse.normalize();

			NodeList nodeList = domResponse
					.getElementsByTagName("error");

			if (nodeList.getLength() != 0) {
				Element errorNode = (Element) nodeList.item(0);

				Element errorMessage = (Element) errorNode
						.getElementsByTagName("message").item(0);

				Element exceptionName = (Element) errorNode
						.getElementsByTagName("exception").item(0);

				Element stackTrace = (Element) errorNode
						.getElementsByTagName("stacktrace").item(0);

				throw new RemoteAdminException(
						errorMessage.getTextContent(),
						exceptionName == null ? "" : exceptionName.getTextContent(),
						stackTrace == null ? "" : stackTrace.getTextContent());
			}		
		} catch (ParserConfigurationException e) {
			throw new RemoteAdminException(
					"Unable to parse the Convertigo server response: \n"
							+ e.getMessage() + ".\n"
							+ "Received response: " + httpResponse);
		} catch (IOException e) {
			throw new RemoteAdminException(
					"An unexpected error has occured during the Convertigo project deployment.\n"
							+ "(IOException) " + e.getMessage() + "\n"
							+ "Received response: " + httpResponse, e);
		} catch (SAXException e) {
			throw new RemoteAdminException(
					"Unable to parse the Convertigo server response: "
							+ e.getMessage() + ".\n"
							+ "Received response: " + httpResponse);
		}
	}
}