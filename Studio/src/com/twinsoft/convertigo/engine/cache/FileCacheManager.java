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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class FileCacheManager extends MemoryCacheManager {
	
	public FileCacheManager() {
		Engine.logCacheManager.debug("Using a file cache manager: " + Engine.CACHE_PATH);
	}
	
	private static final String KEY_INDEX = "Convertigo.FileCacheManager: index";
	
	protected long getNextIndex() {
		long index = 1;
		
		try {
			CacheEntry cacheEntryIndex = cacheIndex.get(FileCacheManager.KEY_INDEX);

			if (cacheEntryIndex != null) {
				index = -cacheEntryIndex.expiryDate;
			}
		}
		catch(Exception e) {
			Engine.logCacheManager.warn("Unable to retrieve the last index", e);
		}

		CacheEntry cacheEntryNewIndex = new FileCacheEntry();
		cacheEntryNewIndex.requestString = FileCacheManager.KEY_INDEX;
		cacheEntryNewIndex.expiryDate = -(index + 1);
		cacheIndex.put(FileCacheManager.KEY_INDEX, cacheEntryNewIndex);
			
		return index;
	}

	protected void makeDirectory() {
		File dir = new File(Engine.CACHE_PATH);
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
	
	protected synchronized CacheEntry storeResponseToRepository(Document response, String requestString, long expiryDate) throws EngineException {
		long index = getNextIndex();
		
		String fileName = Engine.CACHE_PATH + "/" + Long.toHexString(index) + ".xml";

		try {
			makeDirectory();

			XMLUtils.saveXml(response, fileName, true);
			
			FileCacheEntry cacheEntry = new FileCacheEntry();
			cacheEntry.requestString = requestString;
			cacheEntry.fileName = fileName;
			cacheEntry.expiryDate = expiryDate;

			Engine.logCacheManager.debug("The response has been stored: [" + cacheEntry + "]");

			return cacheEntry;
		}
		catch(IOException e) {
			throw new EngineException("Unable to store the response! (requestString: " + requestString + ", file: " + fileName + ")", e);
		}
	}

	protected Document getStoredResponseFromRepository(Requester requester, CacheEntry cacheEntry) throws EngineException {
		FileCacheEntry fileCacheEntry = (FileCacheEntry) cacheEntry;

		Engine.logCacheManager.debug("cacheEntry=[" + cacheEntry.toString() + "]");

		try {
			File file = new File(fileCacheEntry.fileName);
			
			Document document = XMLUtils.parseDOM(file);

			Engine.logCacheManager.debug("Response built from the cache");
			
			return document;
		}
		catch(FileNotFoundException e) {
			Engine.logCacheManager.warn("Unable to find the response [" + cacheEntry.toString() + "] into the cache; the stored response is invalidated.");
			return null;
		}
		catch(Exception e) {
			throw new EngineException("Unable to get the response [" + cacheEntry.toString() + "] from the cache!", e);
		}
	}

	protected synchronized void removeStoredResponseImpl(CacheEntry cacheEntry) throws EngineException {
		FileCacheEntry fileCacheEntry = (FileCacheEntry) cacheEntry;
		
		// Cache entry for index? Then ignore
		if (cacheEntry.requestString == null) return;
		
		File file = new File(fileCacheEntry.fileName);
		if ((file.exists()) && (!file.delete())) {
			throw new EngineException("Unable to remove the cache entry [" + cacheEntry.toString() + "] from the cache!");
		}
		Engine.logCacheManager.debug("The cache entry [" + cacheEntry.toString() + "] has been successfully removed.");		
	}

	public void restoreCacheIndex() throws EngineException {
		try {
			makeDirectory();

			String indexFileName = Engine.CACHE_PATH + "/index.dat";

			// If a backup file exists, it means an error has occurred during the
			// last save of the index file; so restore the last good backup.
			File file = new File(Engine.CACHE_PATH + "/index.sav");
			if (file.exists()) {
				indexFileName = Engine.CACHE_PATH + "/index.sav";
			}
			
			ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(indexFileName));
			Object serializedCacheIndex = objectInputStream.readObject();
			if (serializedCacheIndex != null) cacheIndex = GenericUtils.cast(serializedCacheIndex); 
			objectInputStream.close();
			
			Engine.logCacheManager.debug("The cache index has been reloaded; index=" + cacheIndex.get("index"));
		}
		catch(Exception e) {
			// Ignore it! (use default cache index)
		}
	}

	protected void saveCacheIndex() throws EngineException {
		String indexFileName = Engine.CACHE_PATH + "/index.dat";

		File file = new File(indexFileName);
		File backupFile = null;
		if (file.exists()) {
			backupFile = new File(Engine.CACHE_PATH + "/index.sav");
			if (backupFile.exists()) {
				if (!backupFile.delete()) {
					throw new EngineException("Unable to delete the backup cache index file.");
				}
			}
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
	
	public void garbageCollectCacheRepository() throws EngineException {
		// Do nothing for the moment
	}
}
