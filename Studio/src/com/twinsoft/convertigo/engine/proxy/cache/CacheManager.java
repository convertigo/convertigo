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

package com.twinsoft.convertigo.engine.proxy.cache;

import java.io.InputStream;
import java.util.*;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public abstract class CacheManager {
	
	protected Map<String,CacheEntry> cacheIndex;
	
	public void init() throws EngineException {
		Engine.logCacheManager.debug("(CacheManager) Initializing...");

		// Default cache index
		cacheIndex = new WeakHashMap<String, CacheEntry>();
		
		// Trying to restore the previous cache index if any
		restoreCacheIndex();
	}
	
	public void destroy() throws EngineException {
		Engine.logCacheManager.debug("(CacheManager) Destroying...");
		cacheIndex = null;
	}
	
	public InputStream getResource(String resourceUrl) throws EngineException {
		CacheEntry cacheEntry = cacheIndex.get(resourceUrl);
		InputStream result = null;
		if (cacheEntry != null) {
			if (cacheEntryHasExpired(cacheEntry))
				Engine.logCacheManager.debug("(CacheManager) Response [" + cacheEntry.toString() + "] has expired! Requesting a new response...");
			else if ((result = getStoredResponse(cacheEntry)) == null) {
				// If the stored response has been invalidated by the cache implementation, remove it from the cache index.
				Engine.logCacheManager.debug("(CacheManager) The stored response has been invalidated!");
				cacheIndex.remove(cacheEntry.resourceUrl);
			}
		}
		return result;
	}

	public CacheEntry getCacheEntry(String resourceUrl) throws EngineException {
		CacheEntry cacheEntry = (CacheEntry) cacheIndex.get(resourceUrl);
		return cacheEntry;
	}

	public void removeCacheEntry(CacheEntry cacheEntry) {
		cacheIndex.remove(cacheEntry);
	}

	public boolean cacheEntryHasExpired(CacheEntry cacheEntry) {	
		if (cacheEntry.expiryDate == -1) return false;
		
		long t1 = cacheEntry.expiryDate;
		long t2 = Calendar.getInstance().getTime().getTime();
		Engine.logCacheManager.trace("(CacheManager) t1=" + t1 + " (" + new Date(t1).toString() + ")");
		Engine.logCacheManager.trace("(CacheManager) t2=" + t2 + " (" + new Date(t2).toString() + ")");
		return (t2 > t1);
	}

	/**
	 * Stores a cache entry into the cache repository.
	 * 
	 * @param context the Convertigo context of the request.
	 * @param result the DOM to store.
	 * 
	 * @return the cache entry found
	 * 
	 * @throws EngineException if any error occurs during the storing procedure.
	 */	
	protected abstract CacheEntry storeResponse(String resourceUrl, byte[] data) throws EngineException;

	/**
	 * Retrieves a stored response from the cache repository.
	 * 
	 * @param cacheEntry the cache entry linked to the required stored response.
	 *
	 * @return the stored response linked to this cache entry.
	 * 
	 * @throws EngineException if any error occurs during the search.
	 */	
	protected abstract InputStream getStoredResponse(CacheEntry cacheEntry) throws EngineException;

	/**
	 * Removes a cache entry from the cache repository.
	 * 
	 * @param cacheEntry the cache entry to remove from the cache repository.
	 * 
	 * @throws EngineException if any error occurs during the removing procedure.
	 */	
	protected abstract void removeStoredResponse(CacheEntry cacheEntry) throws EngineException;

	/**
	 * Restores the cache hashtable to the cache repository.
	 * 
	 * @throws EngineException if any error occurs during the restoring procedure.
	 */	
	protected abstract void restoreCacheIndex() throws EngineException;

	/**
	 * Garbage collects the cache repository.
	 * 
	 * @throws EngineException if any error occurs during the garbage collection procedure.
	 */	
	protected abstract void garbageCollectCacheRepository() throws EngineException;

	/**
	 * Saves the cache hashtable to the cache repository.
	 * 
	 * @throws EngineException if any error occurs during the saving procedure.
	 */	
	protected abstract void saveCacheIndex() throws EngineException;
}
