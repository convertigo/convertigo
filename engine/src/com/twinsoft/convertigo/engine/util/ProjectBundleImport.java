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

package com.twinsoft.convertigo.engine.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.util.ProjectBundlePlan.ProjectInfo;
import com.twinsoft.convertigo.engine.util.ProjectBundleZip.Budget;
import com.twinsoft.convertigo.engine.util.ProjectBundleZip.Entry;
import com.twinsoft.convertigo.engine.util.ProjectBundleZip.Limits;

/** Validates and stages the complete selection before handing any archive to a deployer. */
public final class ProjectBundleImport implements AutoCloseable {
	private final Path staging;
	private final Budget budget;
	private final Budget canonicalBudget;
	private final List<ProjectInfo> descriptions = new ArrayList<>();
	private final Map<String, Path> archives = new LinkedHashMap<>();
	private final List<String> warnings = new ArrayList<>();
	private record Manifest(Path path, Set<String> projects) {}
	private final List<Manifest> manifests = new ArrayList<>();
	private ProjectBundlePlan plan;

	private ProjectBundleImport(Path staging, Limits limits) {
		this.staging = staging;
		this.budget = new Budget(limits);
		this.canonicalBudget = new Budget(limits);
	}

	public static ProjectBundleImport prepare(Collection<Path> uploads, Path temporaryRoot, Limits limits) throws Exception {
		if (uploads.isEmpty() || uploads.size() > limits.uploads()) throw new IOException("Invalid number of project archives");
		long total = 0;
		for (var upload : uploads) {
			long size = Files.size(upload);
			if (size > limits.uploadBytes() - total) throw new IOException("The upload exceeds its size limit");
			total += size;
		}
		var prepared = new ProjectBundleImport(Files.createTempDirectory(temporaryRoot, "project-bundle-"), limits);
		try {
			for (var upload : uploads) prepared.read(upload, true);
			prepared.plan = ProjectBundlePlan.create(prepared.descriptions, prepared.manifestOrders());
			for (var group : prepared.plan.groups()) if (group.cyclic()) prepared.warnings.add(
					"Dependency group: " + String.join(", ", group.projects().stream().map(ProjectInfo::name).toList())
					+ ". All group sources are installed before loading its projects.");
			prepared.checkManifests();
			return prepared;
		} catch (Exception | Error failure) {
			try { prepared.close(); } catch (IOException cleanup) { failure.addSuppressed(cleanup); }
			throw failure;
		}
	}

	/** All declared references remain required, even when deliberately excluded from an export. */
	public void checkDependencies(java.util.function.Predicate<String> available) throws IOException {
		var included = plan.projects().stream().map(ProjectInfo::name).collect(java.util.stream.Collectors.toSet());
		for (var project : plan.projects()) for (var dependency : project.dependencies()) {
			if (!included.contains(dependency) && !available.test(dependency)) {
				throw new IOException("Required project " + dependency + " referenced by " + project.name()
						+ " is absent from the bundle and the destination server");
			}
		}
	}

	public ProjectBundlePlan plan() { return plan; }
	public List<String> warnings() { return List.copyOf(warnings); }
	public Path archive(String name) { return archives.get(name); }
	public long expandedBytes() { return budget.expandedBytes(); }
	public long entries() { return budget.entries(); }

	private void read(Path archive, boolean allowBundle) throws Exception {
		var entries = ProjectBundleZip.inspect(archive, budget);
		var names = new TreeSet<String>();
		for (var entry : entries) if (!entry.directory() && !noise(entry.name())) names.add(entry.name());
		String rootDescriptor = descriptor(names, "");
		var directories = new TreeSet<String>();
		for (var name : names) if (name.contains("/")) directories.add(name.substring(0, name.indexOf('/')));
		// A single normal project wins over descriptors embedded deeper in its assets.
		String singlePrefix = null;
		if (rootDescriptor == null && directories.size() == 1) {
			String candidate = directories.first() + "/";
			if (names.stream().allMatch(name -> name.startsWith(candidate)) && descriptor(names, candidate) != null) singlePrefix = candidate;
		}
		boolean single = rootDescriptor != null || singlePrefix != null;
		if (!single && !allowBundle) throw new IOException("An internal archive must contain a project; nested bundles are not supported");
		if (!single) {
			validateBundleLayout(names, directories);
			for (var entry : entries) if (entry.name().equals("bundle.json") && entry.size() > (1L << 20)) throw new IOException("The bundle manifest is too large");
		}
		var extracted = Files.createTempDirectory(staging, "content-");
		String prefix = singlePrefix == null ? "" : singlePrefix;
		Set<String> projectRoots = single ? Set.of(prefix) : directories.stream().map(name -> name + "/").collect(java.util.stream.Collectors.toSet());
		ProjectBundleZip.extract(archive, extracted, entries, budget, name -> {
			if (noise(name)) return true;
			for (var projectRoot : projectRoots) if (name.startsWith(projectRoot)
					&& localProjectStatePath(name.substring(projectRoot.length()))) return true;
			return false;
		});
		if (entries.stream().anyMatch(entry -> noise(entry.name()))) warnings.add("Ignored archive metadata files");
		if (single) {
			addProject(extracted.resolve(prefix), extracted.resolve(rootDescriptor != null ? rootDescriptor : descriptor(names, prefix)));
		} else {
			var previousNames = Set.copyOf(archives.keySet());
			for (var directory : directories) {
				String projectPrefix = directory + "/";
				addProject(extracted.resolve(directory), extracted.resolve(descriptor(names, projectPrefix)));
			}
			for (var name : names) {
				if (!name.contains("/") && !name.equals("bundle.json")) {
					read(extracted.resolve(name), false);
					Files.delete(extracted.resolve(name));
				}
			}
			if (names.contains("bundle.json")) {
				var scope = new TreeSet<>(archives.keySet());
				scope.removeAll(previousNames);
				var manifest = Files.createTempFile(staging, "manifest-", ".json");
				Files.copy(extracted.resolve("bundle.json"), manifest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				manifests.add(new Manifest(manifest, Set.copyOf(scope)));
			}
		}
		org.apache.commons.io.FileUtils.deleteDirectory(extracted.toFile());
	}

	private static void validateBundleLayout(Set<String> names, Set<String> directories) throws IOException {
		for (var name : names) {
			if (!name.contains("/") && !name.equals("bundle.json") && !name.toLowerCase(java.util.Locale.ROOT).matches(".+\\.(car|zip)")) {
				throw new IOException("A bundle may only contain project archives, project folders and bundle.json");
			}
		}
		for (var directory : directories) {
			if (descriptor(names, directory + "/") == null) throw new IOException("A bundle folder must contain a project descriptor");
		}
	}

	private static String descriptor(Set<String> names, String prefix) throws IOException {
		if (names.contains(prefix + "c8oProject.yaml")) return prefix + "c8oProject.yaml";
		var candidates = new ArrayList<String>();
		for (var name : names) {
			if (name.startsWith(prefix) && !name.substring(prefix.length()).contains("/") && name.endsWith(".xml")) candidates.add(name);
		}
		if (!prefix.isEmpty()) {
			String directory = prefix.substring(0, prefix.length() - 1);
			if (names.contains(prefix + directory + ".xml")) return prefix + directory + ".xml";
		}
		return candidates.size() == 1 ? candidates.get(0) : null;
	}

	private void addProject(Path directory, Path descriptor) throws Exception {
		if (descriptions.size() >= budget.limits().projects()) throw new IOException("The bundle contains too many projects");
		var info = ProjectBundleDescriptor.read(descriptor);
		if (archives.containsKey(info.name())) throw new IOException("The bundle contains duplicate project names or versions: " + info.name());
		// Only validated files are re-archived; deployment never sees the original ZIP view.
		long required = 64L << 20;
		try (var files = Files.walk(directory)) {
			for (var file : files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)).toList()) {
				required += Files.size(file) + 1024;
			}
		}
		if (Files.getFileStore(staging).getUsableSpace() < required) throw new IOException("Insufficient space to prepare the project archive");
		var archive = staging.resolve(info.name() + ".car");
		try (var zip = new ZipOutputStream(Files.newOutputStream(archive, java.nio.file.StandardOpenOption.CREATE_NEW));
				var files = Files.walk(directory)) {
			for (var file : files.filter(path -> !path.equals(directory)).sorted().toList()) {
				if (Files.isSymbolicLink(file)) throw new IOException("Unexpected project entry type");
				String name = directory.relativize(file).toString().replace(java.io.File.separatorChar, '/');
				if (file.equals(descriptor) && name.endsWith(".xml")) name = info.name() + ".xml";
				if (localProjectStatePath(name)) continue;
				ProjectBundleZip.validateName(name);
				boolean isDirectory = Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS);
				zip.putNextEntry(new ZipEntry(info.name() + "/" + name + (isDirectory ? "/" : "")));
				if (!isDirectory) Files.copy(file, zip);
				zip.closeEntry();
			}
		}
		ProjectBundleZip.inspect(archive, canonicalBudget);
		descriptions.add(info);
		archives.put(info.name(), archive);
		org.apache.commons.io.FileUtils.deleteDirectory(directory.toFile());
	}

	private List<List<String>> manifestOrders() throws Exception {
		var result = new ArrayList<List<String>>();
		for (var manifest : manifests) {
			var json = new JSONObject(Files.readString(manifest.path(), StandardCharsets.UTF_8));
			var values = json.optJSONArray("order");
			if (json.optInt("formatVersion", -1) != 1 || values == null || values.length() != manifest.projects().size()) continue;
			var order = new ArrayList<String>();
			var seen = new java.util.HashSet<String>();
			for (int i = 0; i < values.length(); i++) {
				String name = values.optString(i);
				if (!manifest.projects().contains(name) || !seen.add(name)) break;
				order.add(name);
			}
			if (order.size() == values.length()) result.add(List.copyOf(order));
		}
		return result;
	}

	private void checkManifests() throws Exception {
		for (var declaredManifest : manifests) {
			var manifest = declaredManifest.path();
			if (Files.size(manifest) > (1L << 20)) throw new IOException("The bundle manifest is too large");
			var json = new JSONObject(Files.readString(manifest, StandardCharsets.UTF_8));
			var listed = json.optJSONArray("projects");
			boolean matches = json.optInt("formatVersion", -1) == 1 && listed != null && listed.length() <= budget.limits().projects();
			var declared = new HashMap<String, String>();
			if (listed != null) {
				for (int i = 0; i < listed.length(); i++) {
					var item = listed.getJSONObject(i);
					String name = item.optString("name", ""), version = item.optString("version", "");
					if (name.length() > 128 || version.length() > 4096 || declared.putIfAbsent(name, version) != null) matches = false;
				}
			}
			var actual = new HashMap<String, String>();
			for (var info : descriptions) if (declaredManifest.projects().contains(info.name())) actual.put(info.name(), info.version());
			matches &= actual.equals(declared);
			var actualOrder = plan.projects().stream().map(ProjectInfo::name).filter(declaredManifest.projects()::contains).toList();
			var order = json.optJSONArray("order");
			matches &= order != null && order.length() == actualOrder.size();
			if (order != null) for (int i = 0; i < Math.min(order.length(), actualOrder.size()); i++) matches &= actualOrder.get(i).equals(order.optString(i));
			if (!matches) warnings.add("The bundle manifest differs from the project descriptors; descriptor names, versions and references were used");
		}
	}

	static boolean localProjectStatePath(String path) {
		String first = path.contains("/") ? path.substring(0, path.indexOf('/')) : path;
		first = java.text.Normalizer.normalize(first, java.text.Normalizer.Form.NFC).toLowerCase(java.util.Locale.ROOT);
		return java.util.Set.of("_private", "_data", ".project", ".settings", ".git", ".svn", "cvs").contains(first);
	}

	private static boolean noise(String path) {
		return path.equals("__MACOSX") || path.startsWith("__MACOSX/")
				|| path.endsWith("/.DS_Store") || path.equals(".DS_Store") || path.endsWith("/Thumbs.db") || path.equals("Thumbs.db");
	}

	@Override
	public void close() throws IOException {
		org.apache.commons.io.FileUtils.deleteDirectory(staging.toFile());
	}
}
