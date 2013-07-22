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

import java.util.*;

import org.w3c.dom.*;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.requesters.Requester;

public abstract class MemoryCacheManager extends CacheManager {
	
	protected Map<String, CacheEntry> cacheIndex;
	
	public void init() throws EngineException {
		super.init();

		// Default cache index
		cacheIndex = Collections.synchronizedMap(new HashMap<String, CacheEntry>());
		
		// Trying to restore the previous cache index if any
		restoreCacheIndex();
	}
	
	public void destroy() throws EngineException {
		super.destroy();

		cacheIndex = null;
	} 

	public synchronized CacheEntry getCacheEntry(String requestString) throws EngineException {
		CacheEntry cacheEntry = cacheIndex.get(requestString);
		return cacheEntry;
	}

	public void updateCacheEntry(CacheEntry cacheEntry) throws EngineException {
		// Do nothing
	}
	
	protected synchronized CacheEntry storeResponse(Document response, String requestString, long expiryDate) throws EngineException {
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

	protected synchronized Document getStoredResponse(Requester requester, CacheEntry cacheEntry) throws EngineException {
		Document response = getStoredResponseFromRepository(requester, cacheEntry);
		
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
}
