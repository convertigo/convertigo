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

package com.twinsoft.convertigo.engine.cache;

import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.ProcessingInstruction;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.AbstractRunnableManager;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class CacheManager extends AbstractRunnableManager {
	
	public void init() throws EngineException {
		Engine.logCacheManager.debug("Initializing cache manager...");
	}
	
	public void destroy() throws EngineException {
		super.destroy();
		
		Engine.logCacheManager.debug("Destroying cache manager...");
	}
	
	public Document getDocument(Requester requester, Context context) throws EngineException {
		Document response = null;
		CacheEntry cacheEntry;
		String	supervision = null;
		
		if (context.isStubRequested) {
			String stubFileName = null;
			if (context.requestedObject instanceof Transaction) {
				stubFileName = Engine.PROJECTS_PATH + "/"
						+ context.requestedObject.getProject().getName()
						+ "/stubs/"
						+ context.requestedObject.getParent().getName() + "."
						+ context.requestedObject.getName() + ".xml";
				
			} else if (context.requestedObject instanceof Sequence) {
				stubFileName = Engine.PROJECTS_PATH + "/"
						+ context.requestedObject.getProject().getName()
						+ "/stubs/" + context.requestedObject.getName() + ".xml";
			}
			try {
				response = XMLUtils.parseDOM(stubFileName);
				response.getDocumentElement().setAttribute("fromStub", "true");
				return response;
			} catch (Exception e) {
				Engine.logCacheManager.error("Error while parsing " + stubFileName + " file");
				throw new EngineException("Unable to load response from Stub", e);
			}
		}

		context.requestedObject.parseInputDocument(context);
		if (context.httpServletRequest != null) {
			try {
				supervision = context.httpServletRequest.getParameter(Parameter.Supervision.getName());
			} catch (Exception e) {
				Engine.logCacheManager.warn("Error while getting '"+Parameter.Supervision.getName()+"' parameter, probably in async job ?");
			}
		}
		
		long expiryDate = context.requestedObject.getResponseExpiryDateInMillis();
		
		// Cache not enabled
		if(EnginePropertiesManager.getPropertyAsBoolean(PropertyName.DISABLE_CACHE)){
			Engine.logCacheManager.debug("Cache not enabled explicitly");

			response = context.requestedObject.run(requester, context);
			response.getDocumentElement().setAttribute("fromcache", "false");
			response.getDocumentElement().setAttribute("fromStub", "false");
		}
		// Not cached transaction
		else if (expiryDate <= 0) {
			Engine.logCacheManager.trace("The response is not cachable");
			
			response = context.requestedObject.run(requester, context);
			response.getDocumentElement().setAttribute("fromcache", "false");
			response.getDocumentElement().setAttribute("fromStub", "false");
		}
		// Cached transaction
		else {
			String requestString = context.requestedObject.getRequestString(context);

			if (context.noCache) {
				Engine.logCacheManager.debug("Ignoring cache for request: " + requestString);
				try {
					cacheEntry = (CacheEntry) getCacheEntry(requestString);
					if (cacheEntry != null) removeStoredResponse(cacheEntry);
					cacheEntry = null;
				}
				catch(Exception e) {
					Engine.logCacheManager.error("Unable to remove the stored response from the cache repository!", e);
				}
			}
			else {
				Engine.logCacheManager.debug("Searched request string: " + requestString);

				try {
					cacheEntry = (CacheEntry) getCacheEntry(requestString);
				}
				catch(Exception e) {
					Engine.logCacheManager.error("Unable to find the cache entry!", e);
					cacheEntry = null;
				}

				Engine.logCacheManager.debug("Found cache entry: " + cacheEntry);
				
				if (cacheEntry != null) {
					if (cacheEntryHasExpired(cacheEntry)) {
						Engine.logCacheManager.debug("Response [" + cacheEntry.toString() + "] has expired! Removing the current response and requesting a new response...");
						try {
							removeStoredResponse(cacheEntry);
							cacheEntry = null;
						}
						catch(Exception e) {
							Engine.logCacheManager.error("Unable to remove the stored response from the cache repository!", e);
						}
					}
					else if ((cacheEntry.sheetUrl == null) && (context.isXsltRequest) &&
							(context.requestedObject.getSheetLocation() == Transaction.SHEET_LOCATION_FROM_LAST_DETECTED_OBJECT_OF_REQUESTABLE)) {
						Engine.logCacheManager.debug("Ignoring cache for request: " + requestString + " because a XSLT procees has been required and no sheet information has been stored into the cache entry.");
						try {
							removeStoredResponse(cacheEntry);
							cacheEntry = null;
						}
						catch(Exception e) {
							Engine.logCacheManager.error("(CacheManager) Unable to remove the stored response from the cache repository!", e);
						}
					}
					else {
						context.cacheEntry = cacheEntry;
						
						// Update the statistics events
						context.requestedObject.setStatisticsOfRequestFromCache();
						String t = context.statistics.start(EngineStatistics.GENERATE_DOM);
						
						try {
							response = getStoredResponse(requester, cacheEntry);
							if (response != null) {
								response.getDocumentElement().setAttribute("fromcache", "true");
								response.getDocumentElement().setAttribute("fromStub", "false");
								ProcessingInstruction pi = response.createProcessingInstruction("xml", "version=\"1.0\" encoding=\"" + context.requestedObject.getEncodingCharSet() + "\"");
								response.insertBefore(pi, response.getFirstChild());
								context.outputDocument = response; // response has been overridden - needed by billings!
								
								if (context.requestedObject != null) {
									context.requestedObject.onCachedResponse();
								}
							}
							else{
								Engine.logCacheManager.debug("Response from cache is null: removing the cache entry.");
								removeStoredResponse(cacheEntry);
							}
						}
						catch(Exception e) {
							Engine.logCacheManager.error("Unable to get the stored response from the cache repository!", e);
							response = null;
						}
						finally {
							context.statistics.stop(t);
						}
					}
				}
			}
			
			if (response == null) {
				response = context.requestedObject.run(requester, context);
				if (Engine.logCacheManager.isTraceEnabled())
					Engine.logCacheManager.trace("Cache manager: document returned:\n" + XMLUtils.prettyPrintDOM(response));
				response.getDocumentElement().setAttribute("fromcache", "false");
				response.getDocumentElement().setAttribute("fromStub", "false");

				// Store the response only if the transaction handlers has not
				// disabled the cache feature...
	    		if (supervision != null) {
	    			Engine.logCacheManager.debug("Supervision mode => disable caching");
	    			context.isCacheEnabled = false;
				}

	    		if (context.isCacheEnabled) {
					try {
						response.getDocumentElement().setAttribute("expires", new Date(expiryDate).toString());
						cacheEntry = storeResponse(response, requestString, expiryDate);
						context.cacheEntry = cacheEntry;
						Engine.logCacheManager.info("The expiration Date " +  new Date(expiryDate).toString());
					}
					catch(Exception e) {
						Engine.logCacheManager.error("(CacheManager) Unable to store the response into the cache repository!", e);
					}
				}
				else {
					Engine.logCacheManager.debug("Cache has been disabled!");
				}
			}
		}

		return response;
	}

	/**
	 * Updates the cache entry to the cache repository.
	 * 
	 * @param cacheEntry the cache entry to update.
	 * 
	 * @throws EngineException if any error occurs during the update procedure.
	 */
	public abstract void updateCacheEntry(CacheEntry cacheEntry) throws EngineException;

	/**
	 * Gets the cache entry according to a given request string.
	 * 
	 * @param requestString the request string for the required cache entry
	 * 
	 * @return the found cache entry, null otherwise.
	 * 
	 * @throws EngineException if any error occurs during the get procedure.
	 */
	protected abstract CacheEntry getCacheEntry(String requestString) throws EngineException;

	/**
	 * Determines if a given cache entry has expired.
	 * 
	 * @param cacheEntry the cache entry to inspect.
	 * 
	 * @return true if the cache entry has expired, false otherwise.
	 */
	public boolean cacheEntryHasExpired(CacheEntry cacheEntry) {
		return cacheEntryHasExpired(cacheEntry, System.currentTimeMillis());
	}
	
	public boolean cacheEntryHasExpired(CacheEntry cacheEntry, long time) {
		long t1 = cacheEntry.expiryDate;
		
		// Cache entries with negative expiry date never expire
		if (t1 < 0) return false;
		
		Engine.logCacheManager.trace("t1=" + t1 + " (" + new Date(t1).toString() + ")");
		Engine.logCacheManager.trace("t2=" + time + " (" + new Date(time).toString() + ")");
		return (time > t1);
	}

	/**
	 * Stores a cache entry into the cache repository.
	 * 
	 * @param response the XML document to store
	 * @param requestString the request string related to this response
	 * @param expiryDate the expiry date of the response
	 * 
	 * @return the found cache entry
	 * 
	 * @throws EngineException if any error occurs during the storing procedure.
	 */
	protected abstract CacheEntry storeResponse(Document response, String requestString, long expiryDate) throws EngineException;

	/**
	 * Retrieves a stored response from the cache repository.
	 * 
	 * @param requester the calling requester.
	 * @param cacheEntry the cache entry linked to the required stored response.
	 *
	 * @return the stored response linked to this cache entry.
	 * 
	 * @throws EngineException if any error occurs during the search.
	 */	
	protected abstract Document getStoredResponse(Requester requester, CacheEntry cacheEntry) throws EngineException;

	/**
	 * Removes a cache entry from the cache repository.
	 * 
	 * @param cacheEntry the cache entry to remove from the cache repository.
	 * 
	 * @throws EngineException if any error occurs during the removing procedure.
	 */	
	protected abstract void removeStoredResponse(CacheEntry cacheEntry) throws EngineException;

	/**
	 * Removes all the expired cache entries from the cache repository.
	 * 
	 * @throws EngineException if any error occurs during the restoring procedure.
	 */
	protected abstract void removeExpiredCacheEntries(long time) throws EngineException;
	
	protected void removeExpiredCacheEntries() throws EngineException {
		removeExpiredCacheEntries(System.currentTimeMillis());
	}
	
	public void clearCacheEntries() throws EngineException {
		removeExpiredCacheEntries(Long.MAX_VALUE);
	}
	
	/**
	 * Performs the repository check task during the vulture loop, i.e. saving internal
	 * data, garbage collect the repository...
	 * 
	 * @throws EngineException if any error occurs during the repository check procedure.
	 */	
	protected abstract void checkRepository() throws EngineException;

	public void run() {
		Engine.logCacheManager.info("Starting the vulture thread for cache entry expiration");

		while (isRunning) {
			try {
				Engine.logCacheManager.trace("Executing vulture thread for cache entry expiration");

				removeExpiredCacheEntries();
				checkRepository();

				Engine.logCacheManager.trace("Vulture task done");
			}
			catch(Exception e) {
				Engine.logCacheManager.error("An unexpected error has occured in the CacheManager vulture.", e);
			}
			finally {
				try {
					int delay = 60;
					try {
						delay = Integer.parseInt(EnginePropertiesManager.getProperty(PropertyName.CACHE_MANAGER_SCAN_DELAY));
					}
					catch(NumberFormatException e) {
					}
					Thread.sleep(delay * 1000);
				}
				catch(InterruptedException e) {
					// Ignore
					Engine.logCacheManager.info("InterruptedException received: probably a request for stopping the vulture.");
				}
			}
		}

		Engine.logCacheManager.info("The vulture thread has been stopped.");
	}
}
