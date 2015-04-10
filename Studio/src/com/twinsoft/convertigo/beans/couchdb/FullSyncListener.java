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

package com.twinsoft.convertigo.beans.couchdb;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.Listener;
import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractCouchDbTransaction;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class FullSyncListener extends Listener {

	private static final long serialVersionUID = -7580433107225235685L;

	private String targetView;
	private String targetSequence;
	
	public FullSyncListener() {
		super();
	}

	@Override
	public FullSyncListener clone() throws CloneNotSupportedException {
		FullSyncListener clonedObject =  (FullSyncListener) super.clone();
		return clonedObject;
	}

	@Override
	public String getRenderer() {
		return "FullSyncListenerTreeObject";
	}
	
	@Override
	public FullSyncConnector getConnector() {
		return (FullSyncConnector) super.getConnector();
	}
	
	public String getTargetView() {
		return targetView;
	}

	public void setTargetView(String targetView) {
		this.targetView = targetView;
	}

	private String getTargetDocName() {
		if (targetView != null) {
			try {
				return targetView.split("\\.")[2];
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private String getTargetViewName() {
		if (targetView != null) {
			try {
				return targetView.split("\\.")[3];
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public String getTargetSequence() {
		return targetSequence;
	}

	public void setTargetSequence(String targetSequence) {
		this.targetSequence = targetSequence;
	}

	public void onBulkDocs(final List<String> ids) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					if (targetView == null) {
						throw new EngineException("No target view defined");
					}
					
					String designDocName = getTargetDocName();
					if (designDocName == null) {
						throw new EngineException("Target design document name is null");
					}
					
					String view = getTargetViewName();
					if (view == null) {
						throw new EngineException("Target view name is null");
					}

					FullSyncConnector connector = getConnector();
					String db = connector.getDatabaseName();
					String ddoc = designDocName;
					
					int limit = 10;
					try {
						List<NameValuePair> options = new LinkedList<NameValuePair>();
						options.add(new BasicNameValuePair("reduce", "false"));
						options.add(new BasicNameValuePair("include_docs", "true"));
						options.add(new BasicNameValuePair("limit", Integer.toString(limit)));
		 
						JSONObject keys = new JSONObject();
						keys.put("keys", new JSONArray(ids));
						
						String startkey = null, startkey_docid = null;
						boolean bContinue;

						do {
							bContinue = false;
							Engine.logBeans.debug("(FullSyncListener) Listener \""+ getName() +"\" : post view for _id keys "+ keys.toString()+ "\n");
							JSONObject json = connector.getCouchClient().postView(db, ddoc, view, options, keys);
							Engine.logBeans.debug("(FullSyncListener) Listener \""+ getName() +"\" : post view returned following documents :\n"+ json.toString()+ "\n");
							if (json != null) {
								if (json.has("error")) {
									String error = json.getString("error");
									error = error == null ? "unknown" : error;
									String reason = json.getString("reason");
									reason = reason == null ? "unknown" : reason;
									throw new EngineException("View returned error: "+ error + ", reason: "+ reason);
								}
								else {
									JSONArray rows = json.getJSONArray("rows");
									if (rows != null) {
										int lenght = rows.length();
										JSONObject row = null;
				
										// Retrieve the first results
										JSONArray docs = new JSONArray();
										for (int i = 0; i < lenght; i++) {
											row = rows.getJSONObject(i);
											docs.put(row.getJSONObject("doc"));
										}
										
										// launch sequence
										if (lenght > 0 && row != null) {
											executeSequence(docs, false); // true: internal requester, false: http requester
										}
										
										// Retrieve the next results
										if (lenght == limit && row != null) {
											startkey = "\"" + row.getString("key") + "\"";
											startkey_docid = "\"" + row.getString("id") + "\"";
				
											bContinue = true;
											options = options.subList(0, 3);
											options.add(new BasicNameValuePair("start_key", startkey));
											options.add(new BasicNameValuePair("startkey_docid", startkey_docid));
											options.add(new BasicNameValuePair("skip", "1"));
										}
									}
								}
							}
						} while (bContinue);
					} catch (Throwable t) {
						throw new EngineException("Query view named \""+ view +"\" of \""+ designDocName +"\" design document failed", t);
					}
					
				} catch (Exception e) {
					Engine.logBeans.error("Unable to handle 'bulkDocs' event for \""+ getName() +"\" listener", e);
				}
			}
			
		}).start();
		
	}
	
	private void executeSequence(JSONArray docs, boolean isInternalRequest) throws EngineException {
		if (targetSequence == null) {
			throw new EngineException("No target sequence defined");
		}
		
		if (docs == null) {
			throw new EngineException("Parameter 'docs' is null");
		}
		
		try {
			Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
			Element itemsElement = document.createElement("items");
			AbstractCouchDbTransaction.toXml(docs, itemsElement);
			NodeList docList = itemsElement.getElementsByTagName("item");
			
			StringTokenizer st = new StringTokenizer(getTargetSequence(),".");
			String projectName = st.nextToken();
			String sequenceName = st.nextToken();
		
			Engine.logBeans.debug("(FullSyncListener) Listener \""+ getName() +"\" : execute sequence \""+sequenceName+"\"");
			if (isInternalRequest) {
				try {
					Map<String, Object> request = new HashMap<String, Object>();
			    	boolean maintainContext = false;
			    	boolean maintainSession = false;
					
			    	request.put(Parameter.Project.getName(), new String[] { projectName });
			    	request.put(Parameter.Sequence.getName(), new String[] { sequenceName });
			    	request.put(Parameter.Context.getName(), new String[] { "" });
			    	request.put(Parameter.SessionId.getName(), new String[] { "" });
					if (!maintainContext) request.put(Parameter.RemoveContext.getName(), new String[] { "" });
					if (!maintainSession) request.put(Parameter.RemoveSession.getName(), new String[] { "" });
					//request.put("docs", itemsElement);
					request.put("doc", docList);
					
		    		Engine.logBeans.debug("(FullSyncListener) Listener \""+ getName() +"\" : internal invoke requested");
		        	InternalRequester internalRequester = new InternalRequester();
		        	internalRequester.setStrictMode(getProject().isStrictMode());
		        	
		        	internalRequester.inputData = request;
		    		Object result = internalRequester.processRequest(request);
		        	if (result != null) {
		        		Document xmlHttpDocument = (Document) result;
		        		String contents = XMLUtils.prettyPrintDOMWithEncoding(xmlHttpDocument, "UTF-8");
		        		Engine.logBeans.debug("(FullSyncListener) Listener \""+ getName() +"\" : sequence successfully executed with following result\n"+ contents + "\n");
		        	}
				}
				catch (Exception e) {
					throw new EngineException("Sequence named \""+ sequenceName +"\" failed", e);
				}
			}
			else {
				String targetUrl = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
				targetUrl += (targetUrl.endsWith("/") ? "":"/") + "projects/"+ projectName + "/.xml";
	
				PostMethod postMethod = null;
				try {
					URL url = new URL(targetUrl);
					HostConfiguration hostConfiguration = new HostConfiguration();
					hostConfiguration.setHost(url.getHost());
					HttpState httpState = new HttpState();
					
					postMethod = new PostMethod(targetUrl);
					postMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
					postMethod.addParameter(Parameter.Sequence.getName(), sequenceName);
					postMethod.addParameter("__handleComplex", "true");
					//postMethod.addParameter("docs", XMLUtils.prettyPrintElement(itemsElement, true, false));
					for (int i=0; i< docList.getLength(); i++) {
						Element itemElement = (Element) docList.item(i);
						postMethod.addParameter("doc", XMLUtils.prettyPrintElement(itemElement, true, false));
					}
					
					Engine.logBeans.debug("(FullSyncListener) Listener \""+ getName() +"\" : http invoke requested");
					int statusCode = Engine.theApp.httpClient.executeMethod(hostConfiguration, postMethod, httpState);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					IOUtils.copy(postMethod.getResponseBodyAsStream(), baos);
					byte[] result = baos.toByteArray();
					String contents = new String((result != null) ? result : new byte[] {});
					
					if (statusCode != -1) {
						Engine.logBeans.debug("(FullSyncListener) Listener \""+ getName() +"\" : sequence successfully executed with following result\n"+ contents + "\n");
					}
				} catch (Exception e) {
					throw new EngineException("Sequence named \""+ sequenceName +"\" failed", e);
				} finally {
					if (postMethod != null) {
						postMethod.releaseConnection();
					}
				}
				
			}
		} catch (Exception e) {
			throw new EngineException("Unable to execute sequence for \""+ getName() +"\" listener", e);
		}
		
	}
	
}
