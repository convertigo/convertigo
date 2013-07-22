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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class FileCacheManager extends CacheManager {
	
	protected long getNextIndex() {
		long index = 0;
		
		try {
			CacheEntry cacheEntryIndex = cacheIndex.get("__index");

			if (cacheEntryIndex != null)
				index = cacheEntryIndex.expiryDate;
		}
		catch(Exception e) {
			Engine.logCacheManager.warn("Unable to retrieve the last index", e);
		}

		CacheEntry cacheEntryNewIndex = new FileCacheEntry();
		cacheEntryNewIndex.expiryDate = index + 1;
		cacheIndex.put("__index", cacheEntryNewIndex);
			
		return index;
	}

	protected void makeDirectory() {
		File dir = new File(Engine.CACHE_PATH + "/proxy/");
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
	
	@Override
	public synchronized CacheEntry storeResponse(String resourceUrl, byte[] data) throws EngineException {
		long index = getNextIndex();
	
		String fileName = Engine.CACHE_PATH + "/proxy/" + Long.toHexString(index) + ".dat";

		try {
			makeDirectory();

			File file = new File(fileName);
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			fileOutputStream.write(data);
			fileOutputStream.close();
			
			FileCacheEntry cacheEntry = new FileCacheEntry();
			cacheEntry.resourceUrl = resourceUrl;
			cacheEntry.fileName = fileName;
			cacheEntry.expiryDate = -1;

			cacheIndex.put(resourceUrl, cacheEntry);
			Engine.logCacheManager.debug("(FileCacheManager) The response has been stored: [" + cacheEntry + "]");

			return (CacheEntry) cacheEntry;
		}
		catch(IOException e) {
			throw new EngineException("Unable to store the response! (resourceUrl: " + resourceUrl + ", file: " + fileName + ")", e);
		}
	}

	@Override
	protected InputStream getStoredResponse(CacheEntry cacheEntry) throws EngineException {
		FileCacheEntry fileCacheEntry = (FileCacheEntry) cacheEntry;

		Engine.logCacheManager.debug("(FileCacheManager) cacheEntry=[" + cacheEntry.toString() + "]");

		try {
			File file = new File(fileCacheEntry.fileName);
			
			FileInputStream fileInputStream = new FileInputStream(file);

			Engine.logCacheManager.debug("(FileCacheManager) Response built from the cache");
			
			return fileInputStream;
		}
		catch(FileNotFoundException e) {
			Engine.logCacheManager.debug("(FileCacheManager) Unable to find the response [" + cacheEntry.toString() + "] into the cache; the stored response is invalidated.");
			return null;
		}
		catch(Exception e) {
			throw new EngineException("Unable to get the response [" + cacheEntry.toString() + "] from the cache!", e);
		}
	}

	@Override
	protected synchronized void removeStoredResponse(CacheEntry cacheEntry) throws EngineException {
		FileCacheEntry fileCacheEntry = (FileCacheEntry) cacheEntry;
		File file = new File(fileCacheEntry.fileName);
		if ((file.exists()) && (!file.delete())) {
			throw new EngineException("Unable to remove the cache entry [" + cacheEntry.toString() + "] from the cache!");
		}
		Engine.logCacheManager.debug("(FileCacheManager) The cache entry [" + cacheEntry.toString() + "] has been successfully removed.");		
	}

	@Override
	public void restoreCacheIndex() throws EngineException {
		try {
			makeDirectory();

			String indexFileName = Engine.CACHE_PATH + "/proxy/index.dat";

			// If a backup file exists, it means an error has occured during the
			// last save of the index file; so restore the last good backup.
			File file = new File(Engine.CACHE_PATH + "/proxy/index.sav");
			if (file.exists()) {
				indexFileName = Engine.CACHE_PATH + "/proxy/index.sav";
			}
			
			ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(indexFileName));
			cacheIndex = GenericUtils.cast(objectInputStream.readObject());
			objectInputStream.close();
			
			Engine.logCacheManager.debug("(FileCacheManager) The cache index has been reloaded; index=" + cacheIndex.get("index"));
		}
		catch(FileNotFoundException e) {
			// Ignore it! (use default cache index)
		}
		catch(Exception e) {
			throw new EngineException("Unable to restore the cache index.", e);
		}
	}

	@Override
	protected void saveCacheIndex() throws EngineException {
		String indexFileName = Engine.CACHE_PATH + "/proxy/index.dat";

		File file = new File(indexFileName);
		File backupFile = null;
		if (file.exists()) {
			backupFile = new File(Engine.CACHE_PATH + "/proxy/index.sav");
			if (!file.renameTo(backupFile)) {
				throw new EngineException("Unable to backup the cache index.");
			} 
		}
			
		try {
			makeDirectory();

			ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(indexFileName));
			objectOutputStream.writeObject(cacheIndex);
			objectOutputStream.flush();
			objectOutputStream.close();
		}
		catch(Exception e) {
			throw new EngineException("Unable to save the cache index.", e);
		}
		
		if (backupFile != null) {
			if (!backupFile.delete()) {
				throw new EngineException("Unable to delete the backup cache index file.");
			}
		}
	}
	
	@Override
	public void garbageCollectCacheRepository() throws EngineException {
		// Do nothing for the moment
	}
}
