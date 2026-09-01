/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.certificates;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.CertificateManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.util.PropertiesUtils;

final class CertificateArchive {
	private static final String FORMAT = "convertigo-certificates";
	private static final int VERSION = 1;
	private static final String MANIFEST_ENTRY = "manifest.json";
	private static final String CERTIFICATES_PREFIX = "certificates/";
	private static final long MAX_ARCHIVE_SIZE = 512L * 1024 * 1024;
	private static final long MAX_ENTRY_SIZE = 64L * 1024 * 1024;
	private static final long MAX_MANIFEST_SIZE = 1024L * 1024;
	private static final int MAX_ENTRIES = 1024;

	private CertificateArchive() {
	}

	static record CertificateEntry(String name, boolean configured, String encryptedPassword, String type,
			String group) {
	}

	static record ExportSelection(Set<String> certificates, Set<String> mappings) {
	}

	static final class Archive implements AutoCloseable {
		private final File temporaryDirectory;
		private final Map<String, CertificateEntry> certificates;
		private final Map<String, String> mappings;
		private final Map<String, File> files;

		private Archive(File temporaryDirectory, Map<String, CertificateEntry> certificates,
				Map<String, String> mappings, Map<String, File> files) {
			this.temporaryDirectory = temporaryDirectory;
			this.certificates = certificates;
			this.mappings = mappings;
			this.files = files;
		}

		@Override
		public void close() {
			FileUtils.deleteQuietly(temporaryDirectory);
		}
	}

	static ExportSelection parseSelection(String selection) throws Exception {
		if (selection == null || selection.isBlank()) {
			throw new ServiceException("The certificate selection is missing");
		}

		JSONArray array = new JSONArray(selection);
		Set<String> certificates = new LinkedHashSet<String>();
		Set<String> mappings = new LinkedHashSet<String>();
		for (int i = 0; i < array.length(); i++) {
			Object selected = array.get(i);
			if (selected instanceof JSONObject) {
				JSONObject element = (JSONObject) selected;
				String category = element.getString("category");
				String name = element.getString("name");
				if ("certificates".equals(category)) {
					certificates.add(validateCertificateName(name));
				} else if ("mappings".equals(category)) {
					mappings.add(validateMappingKey(name));
				} else {
					throw new ServiceException("Invalid certificate export category: " + category);
				}
			} else {
				// Accept the initial certificate-only request format for compatibility.
				certificates.add(validateCertificateName(array.getString(i)));
			}
		}
		if (certificates.isEmpty() && mappings.isEmpty()) {
			throw new ServiceException("No certificate element was selected for export");
		}
		return new ExportSelection(certificates, mappings);
	}

	static void write(ExportSelection selection, OutputStream output) throws Exception {
		File directory = new File(Engine.CERTIFICATES_PATH);
		Properties properties = loadProperties(new File(directory, CertificateManager.STORES_PROPERTIES_FILE_NAME));
		Map<String, File> availableFiles = listCertificateFiles(directory);
		Map<String, CertificateEntry> entries = new LinkedHashMap<String, CertificateEntry>();
		Set<String> certificateNames = new HashSet<String>();
		Set<String> selectedCertificates = new LinkedHashSet<String>(selection.certificates());

		for (String mapping : selection.mappings()) {
			String certificateName = properties.getProperty(mapping);
			if (certificateName == null) {
				throw new ServiceException("The certificate mapping '" + mapping + "' is missing");
			}
			selectedCertificates.add(validateCertificateName(certificateName));
		}

		for (String name : selectedCertificates) {
			if (!certificateNames.add(name.toLowerCase(Locale.ROOT))) {
				throw new ServiceException("Duplicate certificate selection (ignoring case): " + name);
			}
			File certificateFile = availableFiles.get(name);
			if (certificateFile == null) {
				throw new ServiceException("The certificate file '" + name + "' is missing");
			}

			boolean configured = properties.containsKey(name);
			String encryptedPassword = configured ? properties.getProperty(name) : null;
			String type = configured ? properties.getProperty(name + ".type") : null;
			String group = configured ? properties.getProperty(name + ".group") : null;
			if (configured && !isCertificateType(type)) {
				throw new ServiceException("The certificate '" + name + "' has an invalid type");
			}
			entries.put(name, new CertificateEntry(name, configured, encryptedPassword, type, group));
		}

		Map<String, String> mappings = new LinkedHashMap<String, String>();
		for (String key : selection.mappings()) {
			String certificateName = properties.getProperty(key);
			CertificateEntry entry = entries.get(certificateName);
			if (entry == null || !entry.configured()) {
				throw new ServiceException("The mapping '" + key + "' references an unconfigured certificate");
			}
			validateMapping(key, certificateName, entries);
			mappings.put(key, certificateName);
		}

		JSONObject manifest = createManifest(entries, mappings);
		try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
			writeEntry(zip, MANIFEST_ENTRY, manifest.toString(2).getBytes(StandardCharsets.UTF_8), -1);
			for (CertificateEntry entry : entries.values()) {
				File file = availableFiles.get(entry.name());
				writeEntry(zip, CERTIFICATES_PREFIX + entry.name(), file);
			}
		}
	}

	static Archive read(InputStream input, long archiveSize) throws Exception {
		if (archiveSize < 0 || archiveSize > MAX_ARCHIVE_SIZE) {
			throw new ServiceException("The certificate archive is too large");
		}

		File parent = new File(Engine.CERTIFICATES_PATH).getAbsoluteFile().getParentFile();
		File temporaryDirectory = Files.createTempDirectory(parent.toPath(), ".certificates-import-").toFile();
		Map<String, File> files = new LinkedHashMap<String, File>();
		Set<String> archiveEntries = new HashSet<String>();
		Set<String> certificateNames = new HashSet<String>();
		byte[] manifestData = null;
		long totalSize = 0;
		int entryCount = 0;

		try {
			try (ZipInputStream zip = new ZipInputStream(input, StandardCharsets.UTF_8)) {
				ZipEntry entry;
				while ((entry = zip.getNextEntry()) != null) {
					String entryName = entry.getName();
					if (!archiveEntries.add(entryName)) {
						throw new ServiceException("Duplicate ZIP entry: " + entryName);
					}
					if (++entryCount > MAX_ENTRIES) {
						throw new ServiceException("The certificate archive contains too many entries");
					}
					if (entry.isDirectory()) {
						if (!CERTIFICATES_PREFIX.equals(entryName)) {
							throw new ServiceException("Unsupported ZIP directory: " + entryName);
						}
						continue;
					}

					long limit;
					OutputStream target;
					ByteArrayOutputStream manifestOutput = null;
					if (MANIFEST_ENTRY.equals(entryName)) {
						if (manifestData != null) {
							throw new ServiceException("The certificate archive contains several manifests");
						}
						limit = MAX_MANIFEST_SIZE;
						target = manifestOutput = new ByteArrayOutputStream();
					} else if (entryName.startsWith(CERTIFICATES_PREFIX)) {
						String name = validateCertificateName(entryName.substring(CERTIFICATES_PREFIX.length()));
						if (!certificateNames.add(name.toLowerCase(Locale.ROOT))) {
							throw new ServiceException("Duplicate certificate file name (ignoring case): " + name);
						}
						File file = new File(temporaryDirectory, name);
						if (files.put(name, file) != null) {
							throw new ServiceException("Duplicate certificate file: " + name);
						}
						limit = MAX_ENTRY_SIZE;
						target = new FileOutputStream(file);
					} else {
						throw new ServiceException("Unsupported ZIP entry: " + entryName);
					}

					long written;
					try (OutputStream closeableTarget = target) {
						written = copyLimited(zip, closeableTarget, limit);
					}
					totalSize += written;
					if (totalSize > MAX_ARCHIVE_SIZE) {
						throw new ServiceException("The uncompressed certificate archive is too large");
					}
					if (manifestOutput != null) {
						manifestData = manifestOutput.toByteArray();
					}
				}
			}

			if (manifestData == null) {
				throw new ServiceException("The certificate archive manifest is missing");
			}
			return parseManifest(temporaryDirectory, manifestData, files);
		} catch (Exception e) {
			FileUtils.deleteQuietly(temporaryDirectory);
			throw e;
		}
	}

	static File install(Archive imported, boolean replace, boolean serverPriority) throws Exception {
		synchronized (Engine.CERTIFICATES_PATH) {
			File liveDirectory = new File(Engine.CERTIFICATES_PATH).getAbsoluteFile();
			FileUtils.forceMkdir(liveDirectory);
			File livePropertiesFile = new File(liveDirectory, CertificateManager.STORES_PROPERTIES_FILE_NAME);
			Properties serverProperties = loadProperties(livePropertiesFile);
			Map<String, File> serverFiles = listCertificateFiles(liveDirectory);
			Properties resultProperties = new Properties();
			Map<String, File> resultFiles = new LinkedHashMap<String, File>();
			Set<String> acceptedImports = new LinkedHashSet<String>();

			if (!replace) {
				resultProperties.putAll(serverProperties);
				resultFiles.putAll(serverFiles);
			}

			for (CertificateEntry entry : imported.certificates.values()) {
				boolean conflict = containsCertificate(serverProperties, serverFiles, entry.name());
				if (!replace && serverPriority && conflict) {
					continue;
				}
				acceptedImports.add(entry.name());
				removeCertificate(resultProperties, resultFiles, entry.name());
				resultFiles.put(entry.name(), imported.files.get(entry.name()));
				putCertificateConfiguration(resultProperties, entry);
			}

			for (Map.Entry<String, String> mapping : imported.mappings.entrySet()) {
				if (!acceptedImports.contains(mapping.getValue())) {
					continue;
				}
				if (replace || !serverPriority || !resultProperties.containsKey(mapping.getKey())) {
					resultProperties.setProperty(mapping.getKey(), mapping.getValue());
				}
			}

			File stageDirectory = Files.createTempDirectory(liveDirectory.getParentFile().toPath(),
					".certificates-result-").toFile();
			File rollbackDirectory = Files.createTempDirectory(liveDirectory.getParentFile().toPath(),
					".certificates-rollback-").toFile();
			try {
				for (Map.Entry<String, File> resultFile : resultFiles.entrySet()) {
					FileUtils.copyFile(resultFile.getValue(), new File(stageDirectory, resultFile.getKey()));
				}
				File stagePropertiesFile = new File(stageDirectory, CertificateManager.STORES_PROPERTIES_FILE_NAME);
				PropertiesUtils.store(resultProperties, stagePropertiesFile);

				File backup = createBackup(liveDirectory, serverFiles, livePropertiesFile);
				copyCurrentState(serverFiles, livePropertiesFile, rollbackDirectory);
				try {
					applyState(liveDirectory, stageDirectory);
					CertificateManager.invalidate();
					return backup;
				} catch (Exception applyFailure) {
					try {
						applyState(liveDirectory, rollbackDirectory);
					} catch (Exception rollbackFailure) {
						applyFailure.addSuppressed(rollbackFailure);
					}
					throw new ServiceException("Unable to install the certificate archive; the previous configuration was restored",
							applyFailure);
				}
			} finally {
				FileUtils.deleteQuietly(stageDirectory);
				FileUtils.deleteQuietly(rollbackDirectory);
			}
		}
	}

	private static Archive parseManifest(File temporaryDirectory, byte[] manifestData, Map<String, File> files)
			throws Exception {
		JSONObject manifest;
		try {
			manifest = new JSONObject(new String(manifestData, StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new ServiceException("The certificate archive manifest is invalid", e);
		}
		if (!FORMAT.equals(manifest.optString("format")) || manifest.optInt("version", -1) != VERSION) {
			throw new ServiceException("Unsupported certificate archive format or version");
		}

		Map<String, CertificateEntry> certificates = new LinkedHashMap<String, CertificateEntry>();
		JSONArray certificateArray = manifest.getJSONArray("certificates");
		for (int i = 0; i < certificateArray.length(); i++) {
			JSONObject object = certificateArray.getJSONObject(i);
			String name = validateCertificateName(object.getString("name"));
			if (certificates.containsKey(name)) {
				throw new ServiceException("Duplicate certificate in manifest: " + name);
			}
			boolean configured = object.getBoolean("configured");
			String encryptedPassword = configured ? object.getString("encryptedPassword") : null;
			String type = configured ? object.getString("type") : null;
			String group = configured && object.has("group") && !object.isNull("group")
					? object.getString("group") : null;
			if (configured && !isCertificateType(type)) {
				throw new ServiceException("The certificate '" + name + "' has an invalid type");
			}
			certificates.put(name, new CertificateEntry(name, configured, encryptedPassword, type, group));
		}
		if (certificates.isEmpty()) {
			throw new ServiceException("The certificate archive does not contain any certificate");
		}
		if (!files.keySet().equals(certificates.keySet())) {
			Set<String> missing = new LinkedHashSet<String>(certificates.keySet());
			missing.removeAll(files.keySet());
			Set<String> undeclared = new LinkedHashSet<String>(files.keySet());
			undeclared.removeAll(certificates.keySet());
			throw new ServiceException("The certificate archive files do not match its manifest (missing: " + missing
					+ ", undeclared: " + undeclared + ")");
		}

		Map<String, String> mappings = new LinkedHashMap<String, String>();
		JSONArray mappingArray = manifest.getJSONArray("mappings");
		for (int i = 0; i < mappingArray.length(); i++) {
			JSONObject object = mappingArray.getJSONObject(i);
			String key = object.getString("key");
			String certificate = object.getString("certificate");
			if (mappings.put(key, certificate) != null) {
				throw new ServiceException("Duplicate certificate mapping in manifest: " + key);
			}
			validateMapping(key, certificate, certificates);
		}

		return new Archive(temporaryDirectory, certificates, mappings, files);
	}

	private static JSONObject createManifest(Map<String, CertificateEntry> entries, Map<String, String> mappings)
			throws Exception {
		JSONObject manifest = new JSONObject();
		manifest.put("format", FORMAT);
		manifest.put("version", VERSION);
		JSONArray certificates = new JSONArray();
		for (CertificateEntry entry : entries.values()) {
			JSONObject object = new JSONObject();
			object.put("name", entry.name());
			object.put("configured", entry.configured());
			if (entry.configured()) {
				object.put("encryptedPassword", entry.encryptedPassword());
				object.put("type", entry.type());
				if (entry.group() != null) {
					object.put("group", entry.group());
				}
			}
			certificates.put(object);
		}
		manifest.put("certificates", certificates);

		JSONArray mappingArray = new JSONArray();
		for (Map.Entry<String, String> mapping : mappings.entrySet()) {
			mappingArray.put(new JSONObject().put("key", mapping.getKey()).put("certificate", mapping.getValue()));
		}
		manifest.put("mappings", mappingArray);
		return manifest;
	}

	private static void validateMapping(String key, String certificate,
			Map<String, CertificateEntry> certificates) throws ServiceException {
		validateMappingKey(key);
		CertificateEntry entry = certificates.get(certificate);
		if (entry == null || !entry.configured()) {
			throw new ServiceException("The mapping '" + key + "' references a missing or unconfigured certificate");
		}
		if (!key.endsWith("." + entry.type() + ".store")) {
			throw new ServiceException("The mapping '" + key + "' does not match certificate type '" + entry.type() + "'");
		}
	}

	private static String validateMappingKey(String key) throws ServiceException {
		if (!isMappingKey(key) || key.length() > 1024 || key.indexOf('\n') >= 0 || key.indexOf('\r') >= 0) {
			throw new ServiceException("Invalid certificate mapping: " + key);
		}
		return key;
	}

	private static String validateCertificateName(String name) throws ServiceException {
		if (name == null || name.isBlank() || name.length() > 255 || name.indexOf('/') >= 0
				|| name.indexOf('\\') >= 0 || ".".equals(name) || "..".equals(name)) {
			throw new ServiceException("Invalid certificate file name: " + name);
		}
		int dot = name.lastIndexOf('.');
		String extension = dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
		if (!CertificateManager.isCertificateExtension(extension)) {
			throw new ServiceException("Unsupported certificate file extension: " + name);
		}
		return name;
	}

	private static boolean isCertificateType(String type) {
		return "client".equals(type) || "server".equals(type);
	}

	private static boolean isMappingKey(String key) {
		return key != null && (key.startsWith("projects.") || key.startsWith("tas."))
				&& (key.endsWith(".client.store") || key.endsWith(".server.store"));
	}

	private static Map<String, File> listCertificateFiles(File directory) {
		Map<String, File> files = new LinkedHashMap<String, File>();
		File[] children = directory.listFiles();
		if (children == null) {
			return files;
		}
		for (File file : children) {
			if (!file.isFile()) {
				continue;
			}
			String name = file.getName();
			int dot = name.lastIndexOf('.');
			String extension = dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
			if (CertificateManager.isCertificateExtension(extension)) {
				files.put(name, file);
			}
		}
		return files;
	}

	private static Properties loadProperties(File file) throws IOException {
		Properties properties = new Properties();
		if (file.isFile()) {
			PropertiesUtils.load(properties, file);
		}
		return properties;
	}

	private static void removeCertificateConfiguration(Properties properties, String certificateName) {
		properties.remove(certificateName);
		properties.remove(certificateName + ".type");
		properties.remove(certificateName + ".group");
		List<String> mappings = new ArrayList<String>();
		for (String key : properties.stringPropertyNames()) {
			if (isMappingKey(key) && certificateName.equalsIgnoreCase(properties.getProperty(key))) {
				mappings.add(key);
			}
		}
		for (String mapping : mappings) {
			properties.remove(mapping);
		}
	}

	private static boolean containsCertificate(Properties properties, Map<String, File> files,
			String certificateName) {
		for (String fileName : files.keySet()) {
			if (certificateName.equalsIgnoreCase(fileName)) {
				return true;
			}
		}
		for (String propertyName : properties.stringPropertyNames()) {
			if (certificateName.equalsIgnoreCase(propertyName)) {
				return true;
			}
		}
		return false;
	}

	private static void removeCertificate(Properties properties, Map<String, File> files,
			String certificateName) {
		List<String> configuredNames = new ArrayList<String>();
		for (String propertyName : properties.stringPropertyNames()) {
			if (certificateName.equalsIgnoreCase(propertyName)) {
				configuredNames.add(propertyName);
			}
		}
		for (String configuredName : configuredNames) {
			removeCertificateConfiguration(properties, configuredName);
		}

		List<String> fileNames = new ArrayList<String>();
		for (String fileName : files.keySet()) {
			if (certificateName.equalsIgnoreCase(fileName)) {
				fileNames.add(fileName);
			}
		}
		for (String fileName : fileNames) {
			files.remove(fileName);
		}
	}

	private static void putCertificateConfiguration(Properties properties, CertificateEntry entry) {
		if (!entry.configured()) {
			return;
		}
		properties.setProperty(entry.name(), entry.encryptedPassword());
		properties.setProperty(entry.name() + ".type", entry.type());
		if (entry.group() != null) {
			properties.setProperty(entry.name() + ".group", entry.group());
		}
	}

	private static File createBackup(File liveDirectory, Map<String, File> files, File propertiesFile)
			throws Exception {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		String baseName = "certificates_backup_" + dateFormat.format(new Date());
		File backup = new File(liveDirectory, baseName + ".zip");
		int index = 1;
		while (backup.exists()) {
			backup = new File(liveDirectory, baseName + "_" + index++ + ".zip");
		}

		try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(backup), StandardCharsets.UTF_8)) {
			if (propertiesFile.isFile()) {
				writeEntry(zip, "stores.properties", propertiesFile);
			}
			for (Map.Entry<String, File> file : files.entrySet()) {
				writeEntry(zip, CERTIFICATES_PREFIX + file.getKey(), file.getValue());
			}
		}
		return backup;
	}

	private static void copyCurrentState(Map<String, File> files, File propertiesFile, File targetDirectory)
			throws IOException {
		for (Map.Entry<String, File> file : files.entrySet()) {
			FileUtils.copyFile(file.getValue(), new File(targetDirectory, file.getKey()));
		}
		if (propertiesFile.isFile()) {
			FileUtils.copyFile(propertiesFile,
					new File(targetDirectory, CertificateManager.STORES_PROPERTIES_FILE_NAME));
		}
	}

	private static void applyState(File liveDirectory, File sourceDirectory) throws Exception {
		for (File current : listCertificateFiles(liveDirectory).values()) {
			FileUtils.forceDelete(current);
		}
		for (Map.Entry<String, File> source : listCertificateFiles(sourceDirectory).entrySet()) {
			FileUtils.copyFile(source.getValue(), new File(liveDirectory, source.getKey()));
		}

		File liveProperties = new File(liveDirectory, CertificateManager.STORES_PROPERTIES_FILE_NAME);
		File sourceProperties = new File(sourceDirectory, CertificateManager.STORES_PROPERTIES_FILE_NAME);
		if (sourceProperties.isFile()) {
			File temporaryProperties = new File(liveDirectory, ".stores.properties.importing");
			FileUtils.copyFile(sourceProperties, temporaryProperties);
			try {
				Files.move(temporaryProperties.toPath(), liveProperties.toPath(), StandardCopyOption.ATOMIC_MOVE,
						StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temporaryProperties.toPath(), liveProperties.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		} else {
			FileUtils.deleteQuietly(liveProperties);
		}
	}

	private static void writeEntry(ZipOutputStream zip, String name, File file) throws IOException {
		try (InputStream input = new FileInputStream(file)) {
			ZipEntry entry = new ZipEntry(name);
			entry.setTime(file.lastModified());
			zip.putNextEntry(entry);
			input.transferTo(zip);
			zip.closeEntry();
		}
	}

	private static void writeEntry(ZipOutputStream zip, String name, byte[] data, long time) throws IOException {
		ZipEntry entry = new ZipEntry(name);
		if (time >= 0) {
			entry.setTime(time);
		}
		zip.putNextEntry(entry);
		zip.write(data);
		zip.closeEntry();
	}

	private static long copyLimited(InputStream input, OutputStream output, long limit) throws Exception {
		byte[] buffer = new byte[16 * 1024];
		long total = 0;
		int read;
		while ((read = input.read(buffer)) != -1) {
			total += read;
			if (total > limit) {
				throw new ServiceException("A certificate archive entry is too large");
			}
			output.write(buffer, 0, read);
		}
		return total;
	}
}
