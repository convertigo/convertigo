/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

package com.twinsoft.convertigo.engine.cache;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.events.BaseEventListener;
import com.twinsoft.convertigo.engine.events.PropertyChangeEvent;
import com.twinsoft.convertigo.engine.events.PropertyChangeEventListener;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class MemoryCacheManager extends CacheManager implements BaseEventListener {
	
	protected Map<String, CacheEntry> cacheIndex;
	private WeakReference<Map<String, Document>> weakCache;
	private boolean useWeakCache = false;
	
	public void init() throws EngineException {
		super.init();

		useWeakCache = EnginePropertiesManager.getPropertyAsBoolean(PropertyName.CACHE_MANAGER_USE_WEAK);
		
		// Default cache index
		cacheIndex = Collections.synchronizedMap(new HashMap<String, CacheEntry>());
		
		// Trying to restore the previous cache index if any
		restoreCacheIndex();

		Engine.theApp.eventManager.addListener(this, PropertyChangeEventListener.class);
	}
	
	public void destroy() throws EngineException {
		super.destroy();

		Engine.theApp.eventManager.removeListener(this, PropertyChangeEventListener.class);
		cacheIndex = null;
	} 

	public CacheEntry getCacheEntry(String requestString) throws EngineException {
		CacheEntry cacheEntry = cacheIndex.get(requestString);
		return cacheEntry;
	}

	public void updateCacheEntry(CacheEntry cacheEntry) throws EngineException {
		// Do nothing
	}
	
	private synchronized void storeWeakResponse(Document response, String requestString) {
		Map<String, Document> wc = null;
		if (weakCache != null) {
			wc = weakCache.get();
		}
		if (wc == null) {
			weakCache = new WeakReference<Map<String,Document>>(wc = Collections.synchronizedMap(new HashMap<>()));
		}
		wc.put(requestString, response);
	}
	
	protected CacheEntry storeResponse(Document response, String requestString, long expiryDate) throws EngineException {
		if (useWeakCache) {
			storeWeakResponse(response, requestString);
		}
		CacheEntry cacheEntry = storeResponseToRepository(response, requestString, expiryDate);
		cacheIndex.put(requestString, cacheEntry);
		return cacheEntry;
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
	protected abstract CacheEntry storeResponseToRepository(Document response, String requestString, long expiryDate) throws EngineException;

	protected Document getStoredResponse(Requester requester, CacheEntry cacheEntry) throws EngineException {
		Document response = null;
		if (useWeakCache && weakCache != null) {
			Map<String, Document> wc = weakCache.get();
			if (wc != null) {
				Document cached = wc.get(cacheEntry.requestString);
				if (cached != null) {
					try {
						response = XMLUtils.createDom();
						response.appendChild(response.importNode(cached.getDocumentElement(), true));
					} catch (Exception e) {
						response = null;
					}
				}
			}
		}
		if (response == null) {
			response = getStoredResponseFromRepository(requester, cacheEntry);
			if (response != null) {
				storeWeakResponse(response, cacheEntry.requestString);
			}
		}
		
		// If the stored response has been invalidated by the cache implementation,
		// remove it from the cache index.
		if (response == null) {
			Engine.logCacheManager.debug("The stored response has been invalidated!");
			cacheIndex.remove(cacheEntry.requestString);
			cacheEntry = null;
		}
		
		return response;
	}

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
	protected abstract Document getStoredResponseFromRepository(Requester requester, CacheEntry cacheEntry) throws EngineException;
	
	/**
	 * Removes a cache entry from the cache repository.
	 * 
	 * @param cacheEntry the cache entry to remove from the cache repository.
	 * 
	 * @throws EngineException if any error occurs during the removing procedure.
	 */	
	protected synchronized void removeStoredResponse(CacheEntry cacheEntry) throws EngineException {
		cacheIndex.remove(cacheEntry.requestString);
		Map<String, Document> wc;
		if (weakCache != null && (wc = weakCache.get()) != null) {
			wc.remove(cacheEntry.requestString);
		}
		removeStoredResponseImpl(cacheEntry);
	}

	/**
	 * Removes a cache entry from the cache repository.
	 * 
	 * @param cacheEntry the cache entry to remove from the cache repository.
	 * 
	 * @throws EngineException if any error occurs during the removing procedure.
	 */	
	protected abstract void removeStoredResponseImpl(CacheEntry cacheEntry) throws EngineException;
	
	/**
	 * Restores the cache map to the cache repository.
	 * 
	 * @throws EngineException if any error occurs during the restoring procedure.
	 */	
	protected abstract void restoreCacheIndex() throws EngineException;

	/**
	 * Saves the cache map to the cache repository.
	 * 
	 * @throws EngineException if any error occurs during the saving procedure.
	 */	
	protected abstract void saveCacheIndex() throws EngineException;
	
	protected synchronized void checkRepository() throws EngineException {
		if (cacheIndex != null) saveCacheIndex();
	}

	protected synchronized void removeExpiredCacheEntries(long time) throws EngineException {
		Collection<CacheEntry> expired = new LinkedList<CacheEntry>();

		for (CacheEntry cacheEntry : cacheIndex.values())
			try {
				if (cacheEntryHasExpired(cacheEntry, time))
					expired.add(cacheEntry);
			} catch(ClassCastException e) {
				// Ignore (index case)
			} catch(Exception e) {
				Engine.logCacheManager.error("An unexpected error has occured in the MemoryCacheManager vulture while analyzing the cache entry \"" + cacheEntry.toString() + "\".", e);
			}

		for (CacheEntry cacheEntry : expired)
			try {
				removeStoredResponse(cacheEntry);
			} catch(ClassCastException e) {
				// Ignore (index case)
			} catch(Exception e) {
				Engine.logCacheManager.error("An unexpected error has occured in the MemoryCacheManager vulture while removing the cache entry \"" + cacheEntry.toString() + "\".", e);
			}
	}
	
	public void onEvent(PropertyChangeEvent event) {
		PropertyName name = event.getKey();
		if (name.equals(PropertyName.CACHE_MANAGER_USE_WEAK)) {
			useWeakCache = EnginePropertiesManager.getPropertyAsBoolean(PropertyName.CACHE_MANAGER_USE_WEAK);
			if (!useWeakCache) {
				weakCache = null;
			}
		}
	}
}
