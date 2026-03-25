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

package com.twinsoft.convertigo.engine.sync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AbstractRunnableManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.cache.CacheManager;
import com.twinsoft.convertigo.engine.util.InstanceIdentity;
import com.twinsoft.convertigo.engine.util.PropertiesUtils;
import com.twinsoft.tas.KeyManager;

public class SharedWorkspaceSyncManager extends AbstractRunnableManager {
	private enum MarkerKind {
		properties("properties", "properties.marker"),
		roles("roles", "roles.marker"),
		symbols("symbols", "symbols.marker"),
		keys("keys", "keys.marker"),
		cacheConfig("cache-config", "cache-config.marker"),
		cacheClear("cache-clear", "cache-clear.marker"),
		project("project", null);

		private final String kind;
		private final String fileName;

		MarkerKind(String kind, String fileName) {
			this.kind = kind;
			this.fileName = fileName;
		}

		private static MarkerKind fromKind(String kind) {
			for (MarkerKind markerKind : values()) {
				if (markerKind.kind.equals(kind)) {
					return markerKind;
				}
			}
			return null;
		}
	}

	private static class Marker {
		private final Path path;
		private final MarkerKind kind;
		private final String target;
		private final String operation;
		private final String eventId;
		private final String updatedAt;
		private final String instanceId;

		private Marker(Path path, MarkerKind kind, String target, String operation, String eventId, String updatedAt, String instanceId) {
			this.path = path;
			this.kind = kind;
			this.target = target;
			this.operation = operation;
			this.eventId = eventId;
			this.updatedAt = updatedAt;
			this.instanceId = instanceId;
		}

		private String fingerprint() {
			return eventId + "|" + updatedAt + "|" + instanceId + "|" + operation;
		}
	}

	private static final long POLL_INTERVAL_MS = 5000L;
	private static final String MARKER_DIRECTORY = "cluster-sync";
	private static final String PROJECT_MARKER_DIRECTORY = "projects";
	private static final String MARKER_EXTENSION = ".marker";
	private static final String OPERATION_DELETE = "delete";
	private static final String OPERATION_RELOAD = "reload";
	private static final String INSTANCE_ID = InstanceIdentity.getLocalInstanceId();

	private final Map<String, String> markerStates = new HashMap<>();

	public static boolean isEnabled() {
		return Engine.isEngineMode()
				&& !Engine.isCliMode()
				&& EnginePropertiesManager.getPropertyAsBoolean(PropertyName.SESSION_SHARED_WORKSPACE_SYNC_ENABLED);
	}

	public static void markProjectReload(String projectName) {
		writeMarker(MarkerKind.project, projectName, OPERATION_RELOAD);
	}

	public static void markProjectDelete(String projectName) {
		writeMarker(MarkerKind.project, projectName, OPERATION_DELETE);
	}

	public static void markPropertiesChanged() {
		writeMarker(MarkerKind.properties, null, null);
	}

	public static void markRolesChanged() {
		writeMarker(MarkerKind.roles, null, null);
	}

	public static void markSymbolsChanged() {
		writeMarker(MarkerKind.symbols, null, null);
	}

	public static void markKeysChanged() {
		writeMarker(MarkerKind.keys, null, null);
	}

	public static void markCacheConfigChanged() {
		writeMarker(MarkerKind.cacheConfig, null, null);
	}

	public static void markCacheCleared() {
		writeMarker(MarkerKind.cacheClear, null, null);
	}

	public void init() {
		isRunning = true;
		initializeMarkersState();
		var thread = new Thread(this);
		executionThread = thread;
		thread.setName("SharedWorkspaceSyncManager");
		thread.setDaemon(true);
		thread.start();
		Engine.logEngine.info("Shared workspace sync manager started");
	}

	@Override
	public void run() {
		while (isRunning) {
			try {
				Thread.sleep(POLL_INTERVAL_MS);
			} catch (InterruptedException e) {
				if (!isRunning) {
					break;
				}
			}

			if (!isRunning) {
				break;
			}

			try {
				pollMarkers();
			} catch (Exception e) {
				Engine.logEngine.warn("Shared workspace sync polling failed", e);
			}
		}
	}

	private void initializeMarkersState() {
		markerStates.clear();
		for (Marker marker : listMarkers()) {
			markerStates.put(marker.path.toString(), marker.fingerprint());
		}
	}

	private void pollMarkers() {
		processMarker(readMarker(markerPath(MarkerKind.properties)));
		processMarker(readMarker(markerPath(MarkerKind.roles)));
		processMarker(readMarker(markerPath(MarkerKind.symbols)));
		processMarker(readMarker(markerPath(MarkerKind.keys)));
		processMarker(readMarker(markerPath(MarkerKind.cacheConfig)));
		processMarker(readMarker(markerPath(MarkerKind.cacheClear)));

		for (Marker marker : listProjectMarkers()) {
			processMarker(marker);
		}
	}

	private void processMarker(Marker marker) {
		if (marker == null) {
			return;
		}

		var key = marker.path.toString();
		var fingerprint = marker.fingerprint();
		if (fingerprint.equals(markerStates.get(key))) {
			return;
		}

		if (INSTANCE_ID.equals(marker.instanceId)) {
			markerStates.put(key, fingerprint);
			return;
		}

		try {
			applyMarker(marker);
			markerStates.put(key, fingerprint);
		} catch (Exception e) {
			Engine.logEngine.warn("Shared workspace sync failed for " + marker.path + ", will retry on next poll", e);
		}
	}

	private void applyMarker(Marker marker) throws Exception {
		switch (marker.kind) {
		case properties:
			var changedProperties = EnginePropertiesManager.syncPropertiesFromFile();
			Engine.logEngine.info("Shared workspace sync applied properties marker [" + changedProperties.size() + " changed]");
			break;
		case roles:
			Engine.authenticatedSessionManager.invalidateUsersCache();
			Engine.logEngine.info("Shared workspace sync invalidated users cache");
			break;
		case symbols:
			Engine.theApp.databaseObjectsManager.reloadSymbolsFromFile();
			Engine.logEngine.info("Shared workspace sync reloaded symbols");
			break;
		case keys:
			KeyManager.init(EnginePropertiesManager.getProperty(PropertyName.CARIOCA_URL));
			Engine.logEngine.info("Shared workspace sync reloaded keys");
			break;
		case cacheConfig:
			if (Engine.theApp.cacheManager != null) {
				Engine.theApp.cacheManager.destroy();
			}
			Engine.theApp.cacheManager = CacheManager.createConfigured();
			Engine.theApp.cacheManager.init();
			Engine.logEngine.info("Shared workspace sync restarted cache manager");
			break;
		case cacheClear:
			Engine.theApp.cacheManager.clearCacheEntries();
			Engine.logEngine.info("Shared workspace sync cleared cache entries");
			break;
		case project:
			applyProjectMarker(marker);
			break;
		default:
			break;
		}
	}

	private void applyProjectMarker(Marker marker) throws Exception {
		var projectName = marker.target;
		Engine.theApp.schemaManager.clearCache(projectName);
		Engine.theApp.databaseObjectsManager.clearCache(projectName);

		if (OPERATION_DELETE.equals(marker.operation)) {
			Engine.logEngine.info("Shared workspace sync unloaded deleted project \"" + projectName + "\"");
			return;
		}

		var project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
		if (project == null) {
			throw new EngineException("Project \"" + projectName + "\" is not yet visible on the shared workspace");
		}

		Project.executeAutoStartSequences(projectName);
		Engine.logEngine.info("Shared workspace sync reloaded project \"" + projectName + "\"");
	}

	private List<Marker> listMarkers() {
		var markers = new ArrayList<Marker>();
		for (var kind : new MarkerKind[] { MarkerKind.properties, MarkerKind.roles, MarkerKind.symbols, MarkerKind.keys, MarkerKind.cacheConfig, MarkerKind.cacheClear }) {
			var marker = readMarker(markerPath(kind));
			if (marker != null) {
				markers.add(marker);
			}
		}
		markers.addAll(listProjectMarkers());
		return markers;
	}

	private List<Marker> listProjectMarkers() {
		var markers = new ArrayList<Marker>();
		var projectsPath = projectsMarkerDirectory();
		if (!Files.isDirectory(projectsPath)) {
			return markers;
		}

		try (Stream<Path> stream = Files.list(projectsPath)) {
			stream
				.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(MARKER_EXTENSION))
				.sorted(Comparator.comparing(path -> path.getFileName().toString()))
				.map(this::readMarker)
				.filter(marker -> marker != null)
				.forEach(markers::add);
		} catch (IOException e) {
			Engine.logEngine.warn("Unable to list project sync markers in " + projectsPath, e);
		}
		return markers;
	}

	private Marker readMarker(Path path) {
		if (path == null || !Files.isRegularFile(path)) {
			return null;
		}

		var properties = new Properties();
		try (InputStream inputStream = Files.newInputStream(path)) {
			PropertiesUtils.load(properties, inputStream);
		} catch (IOException e) {
			Engine.logEngine.warn("Unable to read shared workspace sync marker " + path, e);
			return null;
		}

		var eventId = properties.getProperty("eventId");
		if (eventId == null || eventId.isBlank()) {
			return null;
		}

		var kindName = properties.getProperty("kind");
		var kind = MarkerKind.fromKind(kindName);
		if (kind == null) {
			if (path.startsWith(projectsMarkerDirectory())) {
				kind = MarkerKind.project;
			} else {
				return null;
			}
		}

		var target = properties.getProperty("target");
		if (kind == MarkerKind.project && (target == null || target.isBlank())) {
			var filename = path.getFileName().toString();
			target = filename.endsWith(MARKER_EXTENSION) ? filename.substring(0, filename.length() - MARKER_EXTENSION.length()) : filename;
		}

		var operation = properties.getProperty("operation", kind == MarkerKind.project ? OPERATION_RELOAD : "");
		var updatedAt = properties.getProperty("updatedAt", "0");
		var instanceId = properties.getProperty("instanceId", "");
		return new Marker(path, kind, target, operation, eventId, updatedAt, instanceId);
	}

	private static void writeMarker(MarkerKind kind, String target, String operation) {
		if (!isEnabled()) {
			return;
		}

		var path = markerPath(kind, target);
		var properties = new Properties();
		properties.setProperty("eventId", UUID.randomUUID().toString());
		properties.setProperty("updatedAt", Long.toString(System.currentTimeMillis()));
		properties.setProperty("instanceId", INSTANCE_ID);
		properties.setProperty("kind", kind.kind);
		if (target != null && !target.isBlank()) {
			properties.setProperty("target", target);
		}
		if (operation != null && !operation.isBlank()) {
			properties.setProperty("operation", operation);
		}

		try {
			writeMarkerFile(path, properties);
		} catch (IOException e) {
			Engine.logEngine.warn("Unable to write shared workspace sync marker " + path, e);
		}
	}

	private static void writeMarkerFile(Path path, Properties properties) throws IOException {
		Files.createDirectories(path.getParent());
		var tmpPath = path.resolveSibling(path.getFileName().toString() + ".tmp-" + UUID.randomUUID());
		try (OutputStream outputStream = Files.newOutputStream(tmpPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
			PropertiesUtils.store(properties, outputStream, "shared workspace sync marker");
		}

		try {
			Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static Path markerPath(MarkerKind kind) {
		return markerPath(kind, null);
	}

	private static Path markerPath(MarkerKind kind, String target) {
		if (kind == MarkerKind.project) {
			return projectsMarkerDirectory().resolve(target + MARKER_EXTENSION);
		}
		return rootDirectory().resolve(kind.fileName);
	}

	private static Path rootDirectory() {
		return Paths.get(Engine.CONFIGURATION_PATH, MARKER_DIRECTORY);
	}

	private static Path projectsMarkerDirectory() {
		return rootDirectory().resolve(PROJECT_MARKER_DIRECTORY);
	}
}
