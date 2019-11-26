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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.events.PropertyChangeEvent;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class FileCacheManager extends MemoryCacheManager {

	public FileCacheManager() {
		Engine.logCacheManager.debug("Using a file cache manager: " + Engine.CACHE_PATH);
	}

	private static final String KEY_INDEX = "Convertigo.FileCacheManager: index";

	@Override
	public void init() throws EngineException {
		File cacheDir = new File(EnginePropertiesManager.getProperty(PropertyName.CACHE_MANAGER_FILECACHE_DIRECTORY));
		try {
			if (cacheDir.exists() && !cacheDir.isDirectory()) {
				Engine.logEngine.error("(FileCacheManager) The 'file cache directory' must be a directory : " + cacheDir);
			} else {
				cacheDir.mkdirs();
				File testWrite = new File(cacheDir, ".testWrite");
				if (testWrite.createNewFile()) {
					testWrite.delete();
					Engine.CACHE_PATH = cacheDir.getCanonicalPath();
				} else {
					Engine.logEngine.error("(FileCacheManager) Failed to write to : " + testWrite);
				}
			}
		} catch (Exception e) {
			Engine.logEngine.error("(FileCacheManager) Failed to write to : " + cacheDir, e);
		}
		super.init();
	}

	@Override
	public void destroy() throws EngineException {
		super.destroy();
	}

	@Override
	public void onEvent(PropertyChangeEvent event) {
		super.onEvent(event);
		PropertyName name = event.getKey();
		String cacheDir = EnginePropertiesManager.getProperty(PropertyName.CACHE_MANAGER_FILECACHE_DIRECTORY);
		if (!cacheDir.equals(Engine.CACHE_PATH) && name.equals(PropertyName.CACHE_MANAGER_FILECACHE_DIRECTORY)) {
			try {
				destroy();
			} catch(EngineException e) {
				Engine.logEngine.error("Error on FileCacheManager.destroy", e);
			}
			try {
				init();
			} catch(EngineException e) {
				Engine.logEngine.error("Error on FileCacheManager.init", e);
			}
		}
	}

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

			try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(indexFileName))) {;
			Object serializedCacheIndex = objectInputStream.readObject();
			if (serializedCacheIndex != null) cacheIndex = GenericUtils.cast(serializedCacheIndex); 
			}

			Engine.logCacheManager.debug("The cache index has been reloaded; index=" + cacheIndex.get("index"));
		}
		catch(Exception e) {
			// Ignore it! (use default cache index)
		}
		if (cacheIndex == null) {
			// Default cache index
			cacheIndex = new ConcurrentHashMap<>();
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
					Engine.logCacheManager.warn("Unable to delete the backup cache index file: " + backupFile);
				}
			}
			if (!file.renameTo(backupFile)) {
				Engine.logCacheManager.warn("Unable to backup the cache index: " + file + " to " + backupFile);
			} 
		}

		try {
			makeDirectory();

			try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(indexFileName))) {
				objectOutputStream.writeObject(cacheIndex);
				objectOutputStream.flush();
			}
		}
		catch(Exception e) {
			throw new EngineException("Unable to save the cache index.", e);
		}

		if (backupFile != null) {
			if (!backupFile.delete()) {
				Engine.logCacheManager.warn("Unable to delete the backup cache index file.");
			}
		}
	}

	public void garbageCollectCacheRepository() throws EngineException {
		// Do nothing for the moment
	}
}
