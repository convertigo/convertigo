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

import java.io.File;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IContainerOrdered;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenu;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UIAttribute;
import com.twinsoft.convertigo.beans.ngx.components.UIAnimation;
import com.twinsoft.convertigo.beans.ngx.components.UICompVariable;
import com.twinsoft.convertigo.beans.ngx.components.UICompEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlVariable;
import com.twinsoft.convertigo.beans.ngx.components.UIControlAttr;
import com.twinsoft.convertigo.beans.ngx.components.UIControlEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlDirective;
import com.twinsoft.convertigo.beans.ngx.components.UICustom;
import com.twinsoft.convertigo.beans.ngx.components.UICustomAction;
import com.twinsoft.convertigo.beans.ngx.components.UICustomAsyncAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicAttr;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UIElement;
import com.twinsoft.convertigo.beans.ngx.components.UIEventSubscriber;
import com.twinsoft.convertigo.beans.ngx.components.UIFont;
import com.twinsoft.convertigo.beans.ngx.components.UIPageEvent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponentEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIStackVariable;
import com.twinsoft.convertigo.beans.ngx.components.UIStyle;
import com.twinsoft.convertigo.beans.ngx.components.UIText;
import com.twinsoft.convertigo.beans.ngx.components.UITheme;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.beans.ngx.components.UIUseVariable;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.Component;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonProperty;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.mobile.NgxBuilder;

public class NgxIonicRoundTripConverter {
	private static final Field DATABASE_OBJECT_PARENT_FIELD = initParentField();

	public static final String MAP_SUFFIX = ".c8o-map.json";
	public static final String ROUNDTRIP_VERSION = "structured-tree-v2";
	public static final String TEMPLATE_NAME = "C8oIonicTemplate";
	public static final String STYLE_NAME = "C8oIonicStyle";
	public static final String ROOT_NAME = "C8oIonicRoot";
	public static final String ROOT_TAG_NAME = "ng-container";

	private static final String PARSE_ROOT_TAG = "c8o-root";
	private static final String APP_COMPONENT_SCSS = "src/app/app.component.scss";
	private static final Pattern INTERPOLATION_PATTERN = Pattern.compile("\\{\\{(.*?)\\}\\}", Pattern.DOTALL);
	private static final Pattern PRIORITY_CLASS_PATTERN = Pattern.compile("(?<!\\S)class(\\d+)(?!\\S)");
	private static final Pattern EVENT_FUNCTION_PATTERN = Pattern.compile("\\bETS(\\d+)\\s*\\(");
	private static final Pattern FOR_INDEX_PATTERN = Pattern.compile("^let\\s+([\\w$]+)\\s*=\\s*index$");
	private static final Pattern FOR_INDEX_AS_PATTERN = Pattern.compile("^index\\s+as\\s+([\\w$]+)$");
	private static final Pattern FOR_OF_PATTERN = Pattern.compile("^let\\s+([\\w$]+)\\s+of\\s+(.+)$", Pattern.DOTALL);
	private static final Pattern SIMPLE_TRANSLATE_PATTERN = Pattern.compile("^(['\"])(.*?)\\1\\s*\\|\\s*translate$");
	private static final Pattern TRANSLATE_EXPRESSION_PATTERN = Pattern.compile("^(['\"])(.*?)\\1\\s*\\|\\s*translate(?:\\s*:(.*))?$", Pattern.DOTALL);
	private static final Pattern ACTION_MARKER_PATTERN = Pattern.compile("/\\*Begin_c8o_function:(CTS\\d+)\\*/", Pattern.DOTALL);
	private static final Pattern STYLE_MARKER_PATTERN = Pattern.compile("/\\*Begin_c8o_style:(\\d+)\\*/\\s*(.*?)\\s*/\\*End_c8o_style:\\1\\*/", Pattern.DOTALL);
	private static final Pattern UNSUPPORTED_BLOCK_SYNTAX = Pattern.compile("(?m)^\\s*@(?:if|for|switch|case|else|defer|placeholder|loading|error|let)\\b");
	private static final Pattern VOID_TAG_PATTERN = Pattern.compile("(?is)<(area|base|br|col|embed|hr|img|input|link|meta|param|source|track|wbr)(\\b[^<>]*?)(?<!/)>");
	private static final Set<String> VOID_TAGS = Set.of("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr");
	private static final String[] PAGE_SCRIPT_MARKERS = {
		"PageImport",
		"PageDeclaration",
		"PageConstructor",
		"PageInitialization",
		"PageAfterViewInit",
		"PageFinalization",
		"PageFunction"
	};
	private static final String[] COMPONENT_SCRIPT_MARKERS = {
		"CompImport",
		"CompDeclaration",
		"CompConstructor",
		"CompInitialization",
		"CompFinalization",
		"CompChanges",
		"CompDoCheck",
		"CompAfterContentInit",
		"CompAfterContentChecked",
		"CompAfterViewInit",
		"CompAfterViewChecked",
		"CompFunction"
	};

	private NgxIonicRoundTripConverter() {
	}

	private static Field initParentField() {
		try {
			Field field = DatabaseObject.class.getDeclaredField("parent");
			field.setAccessible(true);
			return field;
		} catch (Exception e) {
			throw new IllegalStateException("Unable to access DatabaseObject.parent", e);
		}
	}

	private static class ImportContext {
		final ImportReport report;
		final DatabaseObject paletteContext;
		final Map<Long, UIControlEvent> existingEventsByPriority;
		final String targetLabel;
		final boolean recordMismatches;
		final ReusePool reusePool;
		final Set<String> declaredTemplateIds = new HashSet<>();
		String firstMismatch;

		ImportContext(ImportReport report, DatabaseObject paletteContext, Map<Long, UIControlEvent> existingEventsByPriority, String targetLabel) {
			this(report, paletteContext, existingEventsByPriority, targetLabel, true, null);
		}

		ImportContext(ImportReport report, DatabaseObject paletteContext, Map<Long, UIControlEvent> existingEventsByPriority, String targetLabel, boolean recordMismatches) {
			this(report, paletteContext, existingEventsByPriority, targetLabel, recordMismatches, null);
		}

		ImportContext(ImportReport report, DatabaseObject paletteContext, Map<Long, UIControlEvent> existingEventsByPriority, String targetLabel, boolean recordMismatches, ReusePool reusePool) {
			this.report = report;
			this.paletteContext = paletteContext;
			this.existingEventsByPriority = existingEventsByPriority;
			this.targetLabel = targetLabel;
			this.recordMismatches = recordMismatches;
			this.reusePool = reusePool;
		}
	}

	private static class ReusePool {
		final List<UIUseShared> useShared = new ArrayList<>();
		final List<UIControlDirective> directives = new ArrayList<>();
		final List<UIElement> namedTemplates = new ArrayList<>();
	}

	private static class ImportMapTask {
		final Path mapPath;
		final JSONObject map;
		final String kind;
		final JSONObject files;
		final JSONObject create;
		final String qname;

		ImportMapTask(Path mapPath, JSONObject map) {
			this.mapPath = mapPath;
			this.map = map;
			this.kind = map == null ? "" : map.optString("kind");
			this.files = map == null ? null : map.optJSONObject("files");
			this.create = map == null ? null : map.optJSONObject("create");
			this.qname = map == null ? "" : map.optString("qname");
		}

		boolean isValid() {
			if ("invokeSharedAction".equals(kind) || "useSharedComponent".equals(kind)) {
				return !qname.isBlank() && create != null;
			}
			return (!qname.isBlank() || create != null) && files != null;
		}
	}

	private static class UpsertResult<T> {
		final T value;
		final boolean changed;

		UpsertResult(T value, boolean changed) {
			this.value = value;
			this.changed = changed;
		}
	}

	private static class DirectiveSpec {
		final UIControlDirective.AttrDirective kind;
		final String value;

		DirectiveSpec(UIControlDirective.AttrDirective kind, String value) {
			this.kind = kind;
			this.value = value == null ? "" : value;
		}
	}

	private static class ForDirectiveParts {
		String itemName = "";
		String source = "";
		String indexName = "";
		final LinkedHashSet<String> extraClauses = new LinkedHashSet<>();
	}

	private static class ExistingTarget {
		final DatabaseObject container;
		final UIElement managedRoot;
		final List<UIComponent> children;

		ExistingTarget(DatabaseObject container, UIElement managedRoot, List<UIComponent> children) {
			this.container = container;
			this.managedRoot = managedRoot;
			this.children = children;
		}
	}

	private static class ExistingTargetSnapshot {
		final boolean usesManagedRoot;
		final Long afterPriority;
		final List<ComponentSnapshot> children;

		ExistingTargetSnapshot(boolean usesManagedRoot, Long afterPriority, List<ComponentSnapshot> children) {
			this.usesManagedRoot = usesManagedRoot;
			this.afterPriority = afterPriority;
			this.children = children;
		}
	}

	private static class ComponentSnapshot {
		final org.w3c.dom.Element objectXml;
		final List<ComponentSnapshot> children;

		ComponentSnapshot(org.w3c.dom.Element objectXml, List<ComponentSnapshot> children) {
			this.objectXml = objectXml;
			this.children = children;
		}
	}

	private static class ReconcileResult {
		final boolean matched;
		final boolean changed;

		ReconcileResult(boolean matched, boolean changed) {
			this.matched = matched;
			this.changed = changed;
		}

		static ReconcileResult noMatch() {
			return new ReconcileResult(false, false);
		}

		static ReconcileResult match(boolean changed) {
			return new ReconcileResult(true, changed);
		}
	}

	private static class ApplicationStyleImportResult {
		final boolean hasMarkers;
		final boolean changed;

		ApplicationStyleImportResult(boolean hasMarkers, boolean changed) {
			this.hasMarkers = hasMarkers;
			this.changed = changed;
		}
	}

	public static class ImportReport {
		public final String projectName;
		public int processed;
		public int pagesUpdated;
		public int sharedComponentsUpdated;
		public int templatesUpdated;
		public int stylesUpdated;
		public int scriptsUpdated;
		public int skipped;
		public final List<String> warnings = new ArrayList<>();

		ImportReport(String projectName) {
			this.projectName = projectName;
		}

		public boolean hasChanges() {
			return pagesUpdated > 0 || sharedComponentsUpdated > 0
				|| templatesUpdated > 0 || stylesUpdated > 0 || scriptsUpdated > 0;
		}
	}

	public static void writePageMap(Project project, File projectDir, PageComponent page, File htmlFile, File scssFile, File tsFile) {
		writeMap("page", project, projectDir, page.getQName(), page.getName(), htmlFile, scssFile, tsFile);
	}

	public static void writeComponentMap(Project project, File projectDir, UISharedComponent component, File htmlFile, File scssFile, File tsFile) {
		writeMap("sharedComponent", project, projectDir, component.getQName(), component.getName(), htmlFile, scssFile, tsFile);
	}

	public static String computeApplicationStyle(ApplicationComponent application) {
		if (application == null) {
			return "";
		}
		StringBuilder uses = new StringBuilder();
		StringBuilder imports = new StringBuilder();
		StringBuilder others = new StringBuilder();
		Set<String> seenUses = new LinkedHashSet<>();
		Set<String> seenImports = new LinkedHashSet<>();
		String lineSeparator = System.lineSeparator();

		StringBuilder fontFamilies = new StringBuilder();
		for (UIFont font: application.getUIFontList()) {
			appendApplicationStyleLines(uses, imports, others, seenUses, seenImports, font.computeStyle());
			if (font.isDefault()) {
				String fontFamily = font.getFontSource().getFontFamily();
				if (!fontFamily.isEmpty()) {
					fontFamilies.append(fontFamilies.length() > 0 ? ", ": "");
					fontFamilies.append("\""+ fontFamily +"\"");
				}
			}
		}
		if (fontFamilies.length() > 0) {
			others.append("html {").append(lineSeparator);
			others.append("--ion-font-family: ").append(fontFamilies).append(";").append(lineSeparator);
			others.append("}").append(lineSeparator);
		}

		for (UIComponent component: application.getUIComponentList()) {
			if (component instanceof UIStyle style && !(component instanceof UITheme) && !(component instanceof UIFont)) {
				String tpl = style.computeTemplate();
				if (!tpl.isEmpty()) {
					StringBuilder styleUses = new StringBuilder();
					StringBuilder styleImports = new StringBuilder();
					StringBuilder styleBody = new StringBuilder();
					appendApplicationStyleLines(styleUses, styleImports, styleBody, seenUses, seenImports, tpl);
					appendMarkedApplicationStyleBlock(uses, style.priority, styleUses, lineSeparator);
					appendMarkedApplicationStyleBlock(imports, style.priority, styleImports, lineSeparator);
					appendMarkedApplicationStyleBlock(others, style.priority, styleBody, lineSeparator);
				}
			}
		}

		for (UIDynamicMenu menu: application.getMenuComponentList()) {
			appendApplicationStyleLines(uses, imports, others, seenUses, seenImports, menu.computeStyle());
		}

		StringBuilder sb = new StringBuilder();
		if (uses.length() > 0) {
			sb.append(uses).append(lineSeparator);
		}
		if (imports.length() > 0) {
			sb.append(imports).append(lineSeparator);
		}
		if (others.length() > 0) {
			sb.append(others);
		}
		return sb.toString();
	}

	private static void appendMarkedApplicationStyleBlock(StringBuilder target, long priority, StringBuilder content, String lineSeparator) {
		if (content.length() == 0) {
			return;
		}
		target.append("/*Begin_c8o_style:").append(priority).append("*/").append(lineSeparator);
		target.append(content);
		target.append("/*End_c8o_style:").append(priority).append("*/").append(lineSeparator);
	}

	private static void appendApplicationStyleLines(StringBuilder uses, StringBuilder imports, StringBuilder others,
			Set<String> seenUses, Set<String> seenImports, String style) {
		if (style == null || style.isEmpty()) {
			return;
		}
		String lineSeparator = System.lineSeparator();
		for (String rawLine : style.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
			String line = rawLine.trim();
			if (line.isEmpty()) {
				continue;
			}
			if (line.startsWith("@use")) {
				if (seenUses.add(line)) {
					uses.append(line).append(lineSeparator);
				}
			} else if (line.startsWith("@import")) {
				if (seenImports.add(line)) {
					imports.append(line).append(lineSeparator);
				}
			} else {
				others.append(line).append(lineSeparator);
			}
		}
	}

	public static void writeApplicationStyleMap(Project project, File projectDir, ApplicationComponent application, File scssFile) {
		if (project == null || projectDir == null || application == null || scssFile == null) {
			return;
		}
		try {
			JSONObject root = new JSONObject();
			root.put("version", 1);
			root.put("strategy", ROUNDTRIP_VERSION);
			root.put("kind", "applicationStyle");
			root.put("project", project.getName());
			root.put("qname", application.getQName());
			root.put("name", application.getName());
			root.put("generatedHash", sha256(scssFile.toPath()));
			root.put("supportedScopes", new org.codehaus.jettison.json.JSONArray()
				.put("scss-whole-file")
				.put("hash-guarded"));

			JSONObject managedObjects = new JSONObject();
			managedObjects.put("styleName", STYLE_NAME);
			root.put("managedObjects", managedObjects);

			JSONObject files = new JSONObject();
			files.put("scss", relativize(projectDir, scssFile));
			root.put("files", files);

			JSONObject notes = new JSONObject();
			notes.put("scss", "The application scss file is hash-guarded and marked per application UIStyle priority; marked blocks update their existing UIStyle.");
			root.put("notes", notes);

			File mapFile = new File(scssFile.getParentFile(), stripExtension(scssFile.getName()) + MAP_SUFFIX);
			NgxBuilder.keepGeneratedFile(mapFile);
			FileUtils.writeFile(mapFile, root.toString(2), StandardCharsets.UTF_8);
			NgxBuilder.keepGeneratedFile(mapFile);
		} catch (Exception e) {
			Engine.logEngine.warn("(NgxIonicRoundTripConverter) Unable to write Ionic round-trip map for " + application.getQName(), e);
		}
	}

	private static void writeMap(String kind, Project project, File projectDir, String qname, String name, File htmlFile, File scssFile, File tsFile) {
		if (project == null || projectDir == null || qname == null || htmlFile == null || scssFile == null || tsFile == null) {
			return;
		}
		try {
			JSONObject root = new JSONObject();
			root.put("version", 1);
			root.put("strategy", ROUNDTRIP_VERSION);
			root.put("kind", kind);
			root.put("project", project.getName());
			root.put("qname", qname);
			root.put("name", name == null ? "" : name);
			root.put("supportedScopes", new org.codehaus.jettison.json.JSONArray()
				.put("html-structured-tree")
				.put("html-whole-file-fallback")
				.put("scss-whole-file")
				.put("ts-markers-only")
				.put("create-if-missing"));

			JSONObject managedObjects = new JSONObject();
			managedObjects.put("rootName", ROOT_NAME);
			managedObjects.put("rootTagName", ROOT_TAG_NAME);
			managedObjects.put("templateName", TEMPLATE_NAME);
			managedObjects.put("styleName", STYLE_NAME);
			root.put("managedObjects", managedObjects);

			JSONObject files = new JSONObject();
			files.put("html", relativize(projectDir, htmlFile));
			files.put("scss", relativize(projectDir, scssFile));
			files.put("ts", relativize(projectDir, tsFile));
			root.put("files", files);

			JSONObject notes = new JSONObject();
			notes.put("html", "A structured NGX subtree is reimported when the template stays inside the supported Angular/HTML subset. Unsupported syntax falls back to a managed UICustom root.");
			notes.put("scss", "Whole generated scss file is reimported into a managed UIStyle root.");
			notes.put("ts", "Only existing /*Begin_c8o_...*/ marker blocks are reimported into scriptContent, and existing custom-action CTS marker bodies are reimported into actionValue.");
			root.put("notes", notes);

			File mapFile = new File(htmlFile.getParentFile(), stripExtension(htmlFile.getName()) + MAP_SUFFIX);
			NgxBuilder.keepGeneratedFile(mapFile);
			FileUtils.writeFile(mapFile, root.toString(2), StandardCharsets.UTF_8);
			NgxBuilder.keepGeneratedFile(mapFile);
		} catch (Exception e) {
			Engine.logEngine.warn("(NgxIonicRoundTripConverter) Unable to write Ionic round-trip map for " + qname, e);
		}
	}

	public static ImportReport importFromIonic(File projectDir) throws Exception {
		File yaml = new File(projectDir, "c8oProject.yaml");
		if (!yaml.exists()) {
			throw new IllegalStateException("Missing c8oProject.yaml in " + projectDir.getAbsolutePath());
		}
		var loadingData = DatabaseObjectsManager.getProjectLoadingData();
		boolean previousSkipBuilder = loadingData.skipMobileBuilderInit;
		Project project;
		try {
			loadingData.skipMobileBuilderInit = true;
			project = Engine.theApp.databaseObjectsManager.importProject(yaml, true);
		} finally {
			loadingData.skipMobileBuilderInit = previousSkipBuilder;
		}
		ImportReport report = importIntoProject(project, projectDir);
		if (report.hasChanges()) {
			Engine.theApp.databaseObjectsManager.exportProject(project);
		}
		return report;
	}

	public static ImportReport importFromIonic(Project project) throws Exception {
		if (project == null) {
			throw new IllegalArgumentException("Project is null");
		}
		File projectDir = new File(project.getDirPath());
		ImportReport report = importIntoProject(project, projectDir);
		if (report.hasChanges()) {
			Engine.theApp.databaseObjectsManager.exportProject(project);
		}
		return report;
	}

	public static ImportReport importFromIonic(Project project, File target) throws Exception {
		if (project == null) {
			throw new IllegalArgumentException("Project is null");
		}
		if (target == null) {
			return importFromIonic(project);
		}
		File projectDir = new File(project.getDirPath());
		ImportReport report = importIntoProject(project, projectDir, target.toPath());
		if (report.hasChanges()) {
			Engine.theApp.databaseObjectsManager.exportProject(project);
		}
		return report;
	}

	public static ImportReport importIntoProject(Project project, File projectDir) throws Exception {
		return importIntoProject(project, projectDir, null);
	}

	public static ImportReport importIntoProject(Project project, File projectDir, Path targetPath) throws Exception {
		ImportReport report = new ImportReport(project.getName());
		Path root = projectDir.toPath().resolve("_private/ionic");
		if (!Files.exists(root)) {
			report.warnings.add("Missing _private/ionic directory.");
			return report;
		}

		Path appStylePath = root.resolve(APP_COMPONENT_SCSS);
		List<Path> maps = collectImportMaps(projectDir, root, targetPath);
		boolean importAppStyle = shouldImportApplicationStyle(projectDir, root, appStylePath, targetPath)
			&& maps.stream().noneMatch(path -> path.equals(mapPathForGeneratedFile(appStylePath)));
		if (maps.isEmpty() && !importAppStyle) {
			report.warnings.add(targetPath == null
				? "No " + MAP_SUFFIX + " files found under _private/ionic."
				: "No " + MAP_SUFFIX + " file found for Ionic target: " + targetPath);
			return report;
		}

		List<ImportMapTask> tasks = new ArrayList<>();
		for (Path mapPath : maps) {
			report.processed++;
			try {
				ImportMapTask task = new ImportMapTask(mapPath, new JSONObject(Files.readString(mapPath, StandardCharsets.UTF_8)));
				if (!task.isValid()) {
					report.skipped++;
					report.warnings.add("Invalid round-trip map: " + mapPath);
					continue;
				}
				tasks.add(task);
			} catch (Exception e) {
				report.skipped++;
				report.warnings.add("Failed to import " + mapPath + ": " + e.getMessage());
			}
		}

		tasks.sort((left, right) -> {
			int compare = Integer.compare(mapKindOrder(left.kind), mapKindOrder(right.kind));
			return compare != 0 ? compare : left.mapPath.toString().compareTo(right.mapPath.toString());
		});

		for (ImportMapTask task : tasks) {
			try {
				DatabaseObject dbo = resolveOrCreateDatabaseObject(project, task.kind, task.qname, task.create);
				if (dbo == null) {
					report.skipped++;
					report.warnings.add("Unable to resolve map target " + (task.qname.isBlank() ? task.kind : task.qname));
					continue;
				}
				Project owner = dbo.getProject();
				if (owner == null || !project.getName().equals(owner.getName())) {
					continue;
				}

				if ("page".equals(task.kind) && dbo instanceof PageComponent page) {
					Path htmlPath = resolve(projectDir, task.files.optString("html"));
					Path scssPath = resolve(projectDir, task.files.optString("scss"));
					Path tsPath = resolve(projectDir, task.files.optString("ts"));
					importPage(page, htmlPath, scssPath, tsPath, report);
				} else if ("applicationStyle".equals(task.kind) && dbo instanceof ApplicationComponent application) {
					Path scssPath = resolve(projectDir, task.files.optString("scss"));
					applyStyle(application, scssPath, task.map.optString("generatedHash"), report);
				} else if ("sharedComponent".equals(task.kind) && dbo instanceof UISharedComponent component) {
					Path htmlPath = resolve(projectDir, task.files.optString("html"));
					Path scssPath = resolve(projectDir, task.files.optString("scss"));
					Path tsPath = resolve(projectDir, task.files.optString("ts"));
					importSharedComponent(component, htmlPath, scssPath, tsPath, task.create, report);
				} else if ("sharedAction".equals(task.kind) && dbo instanceof UIActionStack stack) {
					Path tsPath = resolve(projectDir, task.files.optString("ts"));
					importSharedAction(stack, tsPath, task.create, report);
				} else if ("invokeSharedAction".equals(task.kind) && (dbo instanceof PageComponent || dbo instanceof UISharedComponent)) {
					importSharedActionInvoke(dbo, task.create, report);
				} else if ("useSharedComponent".equals(task.kind) && (dbo instanceof PageComponent || dbo instanceof UISharedComponent)) {
					importSharedComponentUse(dbo, task.create, report);
				} else {
					report.skipped++;
					report.warnings.add("Unsupported map target " + task.kind + " for " + task.qname);
				}
			} catch (Exception e) {
				report.skipped++;
				report.warnings.add("Failed to import " + task.mapPath + ": " + e.getMessage());
			}
		}
		if (importAppStyle) {
			report.processed++;
			importApplicationStyle(project, appStylePath, report);
		}
		return report;
	}

	private static boolean shouldImportApplicationStyle(File projectDir, Path ionicRoot, Path appStylePath, Path targetPath) {
		if (!Files.isRegularFile(appStylePath)) {
			return false;
		}
		if (targetPath == null) {
			return false;
		}
		Path normalizedTarget = normalizeIonicTarget(projectDir, ionicRoot, targetPath);
		if (Files.isDirectory(normalizedTarget)) {
			return appStylePath.startsWith(normalizedTarget);
		}
		return appStylePath.equals(normalizedTarget);
	}

	private static List<Path> collectImportMaps(File projectDir, Path ionicRoot, Path targetPath) throws Exception {
		if (targetPath == null) {
			return collectAllImportMaps(ionicRoot);
		}
		Path normalizedTarget = normalizeIonicTarget(projectDir, ionicRoot, targetPath);
		if (Files.isDirectory(normalizedTarget)) {
			return collectAllImportMaps(normalizedTarget);
		}
		if (Files.isRegularFile(normalizedTarget) && normalizedTarget.getFileName().toString().endsWith(MAP_SUFFIX)) {
			return List.of(normalizedTarget);
		}
		Path siblingMap = mapPathForGeneratedFile(normalizedTarget);
		if (Files.isRegularFile(siblingMap)) {
			return List.of(siblingMap);
		}
		List<Path> maps = new ArrayList<>();
		for (Path mapPath : collectAllImportMaps(ionicRoot)) {
			try {
				JSONObject map = new JSONObject(Files.readString(mapPath, StandardCharsets.UTF_8));
				JSONObject files = map.optJSONObject("files");
				if (files == null) {
					continue;
				}
				for (String key : List.of("html", "scss", "ts")) {
					String relative = files.optString(key);
					if (relative == null || relative.isBlank()) {
						continue;
					}
					if (resolve(projectDir, relative).equals(normalizedTarget)) {
						maps.add(mapPath);
						break;
					}
				}
			} catch (Exception e) {
			}
		}
		return maps;
	}

	private static List<Path> collectAllImportMaps(Path root) throws Exception {
		List<Path> maps = new ArrayList<>();
		if (!Files.exists(root)) {
			return maps;
		}
		try (var paths = Files.walk(root)) {
			paths.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(MAP_SUFFIX))
				.forEach(maps::add);
		}
		return maps;
	}

	private static Path normalizeIonicTarget(File projectDir, Path ionicRoot, Path targetPath) {
		Path normalized = targetPath;
		if (!normalized.isAbsolute()) {
			normalized = projectDir.toPath().resolve(normalized);
			if (!Files.exists(normalized)) {
				normalized = ionicRoot.resolve(targetPath);
			}
		}
		return normalized.normalize();
	}

	private static Path mapPathForGeneratedFile(Path generatedFile) {
		Path fileName = generatedFile.getFileName();
		if (fileName == null) {
			return generatedFile;
		}
		return generatedFile.resolveSibling(stripExtension(fileName.toString()) + MAP_SUFFIX);
	}

	private static int mapKindOrder(String kind) {
		return ("invokeSharedAction".equals(kind) || "useSharedComponent".equals(kind)) ? 1 : 0;
	}

	private static void importApplicationStyle(Project project, Path scssPath, ImportReport report) throws Exception {
		if (project.getMobileApplication() == null || !(project.getMobileApplication().getApplicationComponent() instanceof ApplicationComponent application)) {
			report.warnings.add("Missing NGX application component for app style import in project " + project.getName());
			return;
		}
		applyStyle(application, scssPath, report);
	}

	private static DatabaseObject resolveDatabaseObject(Project project, String qname) throws Exception {
		if (qname == null || qname.isBlank()) {
			return null;
		}

		if (project != null) {
			String[] parts = qname.split("\\.");
			if (parts.length > 0) {
				DatabaseObject dbo = project;
				int index = 0;
				if (project.getName().equals(parts[0])) {
					index = 1;
				}
				for (int i = index; i < parts.length && dbo != null; i++) {
					dbo = dbo.getDatabaseObjectChild(parts[i]);
				}
				if (dbo != null) {
					return dbo;
				}
			}
		}

		return Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
	}

	private static DatabaseObject resolveOrCreateDatabaseObject(Project project, String kind, String qname, JSONObject create) throws Exception {
		DatabaseObject dbo = resolveDatabaseObject(project, qname);
		if (dbo == null && create != null) {
			dbo = resolveDatabaseObject(project, kind, create);
		}
		if (dbo != null || create == null) {
			return dbo;
		}
		if ("sharedComponent".equals(kind)) {
			return createSharedComponent(project, create);
		}
		if ("sharedAction".equals(kind)) {
			return createSharedAction(project, create);
		}
		return null;
	}

	private static DatabaseObject resolveDatabaseObject(Project project, String kind, JSONObject create) throws Exception {
		if (project == null || kind == null || create == null) {
			return null;
		}
		String name = create.optString("name");
		if (name == null || name.isBlank()) {
			return null;
		}
		ApplicationComponent application = getApplication(project);
		if ("sharedComponent".equals(kind)) {
			for (UISharedComponent component : application.getSharedComponentList()) {
				if (component != null && name.equals(component.getName())) {
					return component;
				}
			}
		}
		if ("sharedAction".equals(kind)) {
			for (UIActionStack stack : application.getSharedActionList()) {
				if (stack != null && name.equals(stack.getName())) {
					return stack;
				}
			}
		}
		return null;
	}

	private static ApplicationComponent getApplication(Project project) throws Exception {
		if (project != null && project.getMobileApplication() != null
				&& project.getMobileApplication().getApplicationComponent() instanceof ApplicationComponent application) {
			return application;
		}
		throw new IllegalStateException("Project " + (project == null ? "" : project.getName()) + " has no NGX application component");
	}

	private static UISharedRegularComponent createSharedComponent(Project project, JSONObject create) throws Exception {
		String name = requiredCreateName(create, "shared component");
		ApplicationComponent application = getApplication(project);
		UISharedRegularComponent component = new UISharedRegularComponent();
		component.setName(name);
		component.setSharedModule(create.optString("sharedModule"));
		component.setExposed(!create.has("exposed") || create.optBoolean("exposed", true));
		component.bNew = true;
		component.hasChanged = true;
		application.add(component);
		return component;
	}

	private static UIActionStack createSharedAction(Project project, JSONObject create) throws Exception {
		String name = requiredCreateName(create, "shared action");
		ApplicationComponent application = getApplication(project);
		UIActionStack stack = new UIActionStack();
		stack.setName(name);
		stack.bNew = true;
		stack.hasChanged = true;
		application.add(stack);
		return stack;
	}

	private static String requiredCreateName(JSONObject create, String label) {
		String name = create == null ? "" : create.optString("name");
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Missing create.name for " + label);
		}
		return name;
	}

	private static void importPage(PageComponent page, Path htmlPath, Path scssPath, Path tsPath, ImportReport report) throws Exception {
		int skippedBefore = report.skipped;
		if (applyTemplate(page, htmlPath, report)) {
			report.pagesUpdated++;
		}
		if (report.skipped > skippedBefore) {
			return;
		}
		applyStyle(page, scssPath, report);
		applyScript(page, tsPath, report);
		applyCustomActions(page, tsPath, report);
	}

	private static void importSharedComponent(UISharedComponent component, Path htmlPath, Path scssPath, Path tsPath, JSONObject create, ImportReport report) throws Exception {
		boolean variablesChanged = syncSharedComponentVariables(component, create);
		int skippedBefore = report.skipped;
		if (applyTemplate(component, htmlPath, report)) {
			report.sharedComponentsUpdated++;
		}
		if (report.skipped > skippedBefore) {
			return;
		}
		applyStyle(component, scssPath, report);
		applyScript(component, tsPath, report);
		applyCustomActions(component, tsPath, report);
		if (variablesChanged) {
			report.scriptsUpdated++;
		}
	}

	private static void importSharedAction(UIActionStack stack, Path tsPath, JSONObject create, ImportReport report) throws Exception {
		if (stack == null) {
			return;
		}
		boolean variablesChanged = syncSharedActionVariables(stack, create);
		UICustomAction action = ensurePrimaryCustomAction(stack, create);
		String imported = readSharedActionBody(tsPath, action, create);
		if (imported == null) {
			if (variablesChanged) {
				report.scriptsUpdated++;
			}
			report.warnings.add("Missing ts body for shared action " + stack.getQName() + ": " + tsPath);
			return;
		}
		String current = action.getActionValue() == null ? "" : action.getActionValue().getString();
		if (variablesChanged || !isEquivalentActionValue(current, imported)) {
			action.setActionValue(new FormatedContent(normalizeActionValue(imported)));
			report.scriptsUpdated++;
		}
	}

	private static boolean syncSharedActionVariables(UIActionStack stack, JSONObject create) throws Exception {
		JSONArray variables = create == null ? null : create.optJSONArray("variables");
		if (stack == null || variables == null) {
			return false;
		}
		boolean changed = false;
		Map<String, UIStackVariable> existing = new HashMap<>();
		for (UIComponent component : stack.getUIComponentList()) {
			if (component instanceof UIStackVariable variable) {
				existing.put(variable.getName(), variable);
			}
		}
		List<String> declared = new ArrayList<>();
		for (int i = 0; i < variables.length(); i++) {
			JSONObject spec = variables.optJSONObject(i);
			if (spec == null) {
				continue;
			}
			String name = spec.optString("name");
			if (name == null || name.isBlank()) {
				continue;
			}
			declared.add(name);
			UIStackVariable variable = existing.get(name);
			if (variable == null) {
				variable = new UIStackVariable();
				variable.setName(name);
				variable.bNew = true;
				variable.hasChanged = true;
				stack.add(variable);
				existing.put(name, variable);
				changed = true;
			}
			String importedValue = buildStackVariableValue(spec);
			if (!safeEquals(variable.getVariableValue(), importedValue)) {
				variable.setVariableValue(importedValue);
				changed = true;
			}
		}
		if (create.optBoolean("replaceVariables", true)) {
			for (UIComponent component : new ArrayList<>(stack.getUIComponentList())) {
				if (component instanceof UIStackVariable variable && !declared.contains(variable.getName())) {
					stack.remove(variable);
					changed = true;
				}
			}
		}
		return changed;
	}

	private static boolean syncSharedComponentVariables(UISharedComponent component, JSONObject create) throws Exception {
		JSONArray variables = create == null ? null : create.optJSONArray("variables");
		if (component == null || variables == null) {
			return false;
		}
		boolean changed = false;
		Map<String, UICompVariable> existing = new HashMap<>();
		for (UIComponent child : component.getUIComponentList()) {
			if (child instanceof UICompVariable variable) {
				existing.put(variable.getName(), variable);
			}
		}
		List<String> declared = new ArrayList<>();
		for (int i = 0; i < variables.length(); i++) {
			JSONObject spec = variables.optJSONObject(i);
			if (spec == null) {
				continue;
			}
			String name = spec.optString("name");
			if (name == null || name.isBlank()) {
				continue;
			}
			declared.add(name);
			UICompVariable variable = existing.get(name);
			if (variable == null) {
				variable = new UICompVariable();
				variable.setName(name);
				variable.bNew = true;
				variable.hasChanged = true;
				component.add(variable);
				existing.put(name, variable);
				changed = true;
			}
			String importedValue = buildStackVariableValue(spec);
			if (!safeEquals(variable.getVariableValue(), importedValue)) {
				variable.setVariableValue(importedValue);
				changed = true;
			}
			boolean autoEmit = spec.optBoolean("autoEmit", false);
			if (variable.isAutoEmit() != autoEmit) {
				variable.setAutoEmit(autoEmit);
				changed = true;
			}
		}
		if (create.optBoolean("replaceVariables", true)) {
			for (UIComponent child : new ArrayList<>(component.getUIComponentList())) {
				if (child instanceof UICompVariable variable && !declared.contains(variable.getName())) {
					component.remove(variable);
					changed = true;
				}
			}
		}
		return changed;
	}

	private static void importSharedActionInvoke(DatabaseObject root, JSONObject create, ImportReport report) throws Exception {
		if (root == null || create == null) {
			return;
		}
		UIComponent target = resolveInvokeTarget(root, create);
		if (!isEventCapableTarget(target)) {
			report.skipped++;
			report.warnings.add("Unable to resolve invoke target under " + root.getQName());
			return;
		}
		String sharedActionQName = resolveSharedActionQName(root.getProject(), create);
		if (sharedActionQName == null || sharedActionQName.isBlank()) {
			report.skipped++;
			report.warnings.add("Unable to resolve shared action for invoke spec under " + root.getQName());
			return;
		}
		String eventName = resolveControlEventName(create);
		if (eventName == null || eventName.isBlank()) {
			report.skipped++;
			report.warnings.add("Unable to resolve event name for invoke spec under " + root.getQName());
			return;
		}
		UpsertResult<UIControlEvent> eventResult = ensureControlEvent(target, eventName, create);
		UpsertResult<UIDynamicInvoke> invokeResult = ensureInvokeAction(eventResult.value, sharedActionQName, create);
		boolean varsChanged = syncInvokeVariables(invokeResult.value, create);
		if (eventResult.changed || invokeResult.changed || varsChanged) {
			report.templatesUpdated++;
		}
	}

	private static void importSharedComponentUse(DatabaseObject root, JSONObject create, ImportReport report) throws Exception {
		if (root == null || create == null) {
			return;
		}
		DatabaseObject target = resolveUseSharedTarget(root, create);
		if (!isUseSharedCapableTarget(target)) {
			report.skipped++;
			report.warnings.add("Unable to resolve use-shared target under " + root.getQName());
			return;
		}
		String sharedComponentQName = resolveSharedComponentQName(root.getProject(), create);
		if (sharedComponentQName == null || sharedComponentQName.isBlank()) {
			report.skipped++;
			report.warnings.add("Unable to resolve shared component for use spec under " + root.getQName());
			return;
		}
		UpsertResult<UIUseShared> useResult = ensureUseShared(target, sharedComponentQName, create);
		boolean varsChanged = syncUseSharedVariables(useResult.value, create);
		if (useResult.changed || varsChanged) {
			report.templatesUpdated++;
		}
	}

	private static UIComponent resolveInvokeTarget(DatabaseObject root, JSONObject create) throws Exception {
		if (root == null || create == null) {
			return null;
		}
		JSONObject target = create.optJSONObject("target");
		String targetQName = firstNonBlank(
			create.optString("targetQName"),
			target == null ? "" : target.optString("qname"));
		if (targetQName != null && !targetQName.isBlank()) {
			DatabaseObject dbo = resolveDatabaseObject(root.getProject(), targetQName);
			if (dbo instanceof UIComponent component && isDescendantOrSelf(root, component)) {
				return component;
			}
		}

		Long targetPriority = parsePrioritySpec(firstNonBlank(
			create.optString("targetPriority"),
			target == null ? "" : target.optString("priority"),
			target == null ? "" : target.optString("anchorPriority")));
		String targetClassToken = firstNonBlank(
			create.optString("targetClass"),
			target == null ? "" : target.optString("classToken"),
			target == null ? "" : target.optString("className"),
			target == null ? "" : target.optString("class"));
		String targetName = firstNonBlank(
			create.optString("targetName"),
			target == null ? "" : target.optString("name"));
		String targetTagName = firstNonBlank(
			create.optString("targetTag"),
			target == null ? "" : target.optString("tagName"));

		for (DatabaseObject dbo : root.getDatabaseObjectChildren(true)) {
			if (!(dbo instanceof UIComponent component) || !isEventCapableTarget(component)) {
				continue;
			}
			if (targetPriority != null && dbo.priority != targetPriority.longValue()) {
				continue;
			}
			if (targetName != null && !targetName.isBlank() && !safeEquals(component.getName(), targetName)) {
				continue;
			}
			if (targetClassToken != null && !targetClassToken.isBlank() && !componentHasClassToken(component, targetClassToken)) {
				continue;
			}
			if (targetTagName != null && !targetTagName.isBlank() && !componentMatchesTag(component, targetTagName)) {
				continue;
			}
			return component;
		}
		return null;
	}

	private static boolean isDescendantOrSelf(DatabaseObject ancestor, DatabaseObject candidate) {
		for (DatabaseObject current = candidate; current != null; current = current.getParent()) {
			if (current.equals(ancestor)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isEventCapableTarget(UIComponent component) {
		return component instanceof UIElement || component instanceof UIUseShared;
	}

	private static DatabaseObject resolveUseSharedTarget(DatabaseObject root, JSONObject create) throws Exception {
		if (root == null || create == null) {
			return null;
		}
		JSONObject target = create.optJSONObject("target");
		String targetQName = firstNonBlank(
			create.optString("targetQName"),
			target == null ? "" : target.optString("qname"));
		if (targetQName != null && !targetQName.isBlank()) {
			DatabaseObject dbo = resolveDatabaseObject(root.getProject(), targetQName);
			if (dbo != null && isDescendantOrSelf(root, dbo) && isUseSharedCapableTarget(dbo)) {
				return dbo;
			}
		}

		Long targetPriority = parsePrioritySpec(firstNonBlank(
			create.optString("targetPriority"),
			target == null ? "" : target.optString("priority"),
			target == null ? "" : target.optString("anchorPriority")));
		String targetClassToken = firstNonBlank(
			create.optString("targetClass"),
			target == null ? "" : target.optString("classToken"),
			target == null ? "" : target.optString("className"),
			target == null ? "" : target.optString("class"));
		String targetName = firstNonBlank(
			create.optString("targetName"),
			target == null ? "" : target.optString("name"));
		String targetTagName = firstNonBlank(
			create.optString("targetTag"),
			target == null ? "" : target.optString("tagName"));

		if (targetPriority == null && targetClassToken.isBlank() && targetName.isBlank() && targetTagName.isBlank()) {
			return isUseSharedCapableTarget(root) ? root : null;
		}

		for (DatabaseObject dbo : root.getDatabaseObjectChildren(true)) {
			if (!(dbo instanceof UIComponent component) || !isUseSharedCapableTarget(component)) {
				continue;
			}
			if (targetPriority != null && dbo.priority != targetPriority.longValue()) {
				continue;
			}
			if (!targetName.isBlank() && !safeEquals(component.getName(), targetName)) {
				continue;
			}
			if (!targetClassToken.isBlank() && !componentHasClassToken(component, targetClassToken)) {
				continue;
			}
			if (!targetTagName.isBlank() && !componentMatchesTag(component, targetTagName)) {
				continue;
			}
			return component;
		}
		return null;
	}

	private static boolean isUseSharedCapableTarget(DatabaseObject target) {
		return target instanceof PageComponent
			|| target instanceof UISharedComponent
			|| target instanceof UIElement
			|| target instanceof UIControlDirective;
	}

	private static Long parsePrioritySpec(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		String normalized = raw.trim();
		if (normalized.startsWith("class")) {
			normalized = normalized.substring("class".length());
		}
		try {
			return Long.valueOf(normalized);
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean componentHasClassToken(UIComponent component, String classToken) {
		Element rendered = parseRenderedElement(component);
		return rendered != null && rendered.classNames().contains(classToken);
	}

	private static boolean componentMatchesTag(UIComponent component, String tagName) {
		Element rendered = parseRenderedElement(component);
		return rendered != null && safeEquals(rendered.tagName(), tagName);
	}

	private static String resolveSharedActionQName(Project project, JSONObject create) throws Exception {
		String candidate = firstNonBlank(
			create.optString("sharedActionQName"),
			create.optString("sharedAction"),
			create.optString("sharedActionName"),
			create.optString("stack"));
		if (candidate == null || candidate.isBlank()) {
			return "";
		}
		if (candidate.indexOf('.') != -1) {
			return candidate;
		}
		ApplicationComponent application = getApplication(project);
		for (UIActionStack stack : application.getSharedActionList()) {
			if (stack != null && candidate.equals(stack.getName())) {
				return stack.getQName();
			}
		}
		return candidate;
	}

	private static String resolveSharedComponentQName(Project project, JSONObject create) throws Exception {
		String candidate = firstNonBlank(
			create.optString("sharedComponentQName"),
			create.optString("sharedComponent"),
			create.optString("sharedComponentName"),
			create.optString("component"));
		if (candidate == null || candidate.isBlank()) {
			return "";
		}
		if (candidate.indexOf('.') != -1) {
			return candidate;
		}
		ApplicationComponent application = getApplication(project);
		for (UISharedComponent component : application.getSharedComponentList()) {
			if (component != null && candidate.equals(component.getName())) {
				return component.getQName();
			}
		}
		return candidate;
	}

	private static String resolveControlEventName(JSONObject create) {
		String candidate = firstNonBlank(
			create.optString("eventName"),
			create.optString("event"),
			create.optString("attrName"));
		if (candidate == null || candidate.isBlank()) {
			return UIControlEvent.AttrEvent.onClick.name();
		}
		String normalized = toControlEventName(candidate);
		if (normalized != null && !normalized.isBlank()) {
			return normalized;
		}
		try {
			return UIControlEvent.AttrEvent.valueOf(candidate).name();
		} catch (Exception e) {
			return null;
		}
	}

	private static UpsertResult<UIControlEvent> ensureControlEvent(UIComponent target, String eventName, JSONObject create) throws Exception {
		String eventNodeName = firstNonBlank(create.optString("eventNodeName"), create.optString("eventNameHint"));
		boolean changed = false;
		UIControlEvent event = null;
		for (UIComponent child : target.getUIComponentList()) {
			if (child instanceof UIControlEvent existing && safeEquals(existing.getEventName(), eventName)) {
				event = existing;
				break;
			}
		}
		if (event == null) {
			event = new UIControlEvent();
			event.setEventName(eventName);
			if (eventNodeName != null && !eventNodeName.isBlank()) {
				event.setName(eventNodeName);
			}
			event.setEnabled(true);
			addChild(target, event, null);
			changed = true;
		} else {
			if (eventNodeName != null && !eventNodeName.isBlank() && !safeEquals(event.getName(), eventNodeName)) {
				event.setName(eventNodeName);
				changed = true;
			}
			if (!event.isEnabled()) {
				event.setEnabled(true);
				changed = true;
			}
		}
		return new UpsertResult<>(event, changed);
	}

	private static UpsertResult<UIDynamicInvoke> ensureInvokeAction(UIControlEvent event, String sharedActionQName, JSONObject create) throws Exception {
		String invokeName = firstNonBlank(create.optString("invokeName"), create.optString("name"));
		boolean changed = false;
		UIDynamicInvoke invoke = null;
		for (UIComponent child : event.getUIComponentList()) {
			if (!(child instanceof UIDynamicInvoke existing)) {
				continue;
			}
			if (invokeName != null && !invokeName.isBlank() && safeEquals(existing.getName(), invokeName)) {
				invoke = existing;
				break;
			}
			if (safeEquals(existing.getSharedActionQName(), sharedActionQName)) {
				invoke = existing;
				break;
			}
		}
		if (invoke == null) {
			invoke = createInvokeBean(event, sharedActionQName);
			if (invokeName != null && !invokeName.isBlank()) {
				invoke.setName(invokeName);
			}
			addChild(event, invoke, null);
			changed = true;
		} else {
			if (invokeName != null && !invokeName.isBlank() && !safeEquals(invoke.getName(), invokeName)) {
				invoke.setName(invokeName);
				changed = true;
			}
			if (!safeEquals(invoke.getSharedActionQName(), sharedActionQName)) {
				invoke.setSharedActionQName(sharedActionQName);
				changed = true;
			}
			if (!invoke.isEnabled()) {
				invoke.setEnabled(true);
				changed = true;
			}
		}
		return new UpsertResult<>(invoke, changed);
	}

	private static UpsertResult<UIUseShared> ensureUseShared(DatabaseObject target, String sharedComponentQName, JSONObject create) throws Exception {
		String useName = firstNonBlank(create.optString("useName"), create.optString("name"));
		boolean changed = false;
		UIUseShared use = null;
		if (target instanceof PageComponent page) {
			for (UIComponent child : page.getUIComponentList()) {
				if (!(child instanceof UIUseShared existing)) {
					continue;
				}
				if (!useName.isBlank() && safeEquals(existing.getName(), useName)) {
					use = existing;
					break;
				}
				if (safeEquals(existing.getSharedComponentQName(), sharedComponentQName)) {
					use = existing;
					break;
				}
			}
		} else if (target instanceof UIComponent parent) {
			for (UIComponent child : parent.getUIComponentList()) {
				if (!(child instanceof UIUseShared existing)) {
					continue;
				}
				if (!useName.isBlank() && safeEquals(existing.getName(), useName)) {
					use = existing;
					break;
				}
				if (safeEquals(existing.getSharedComponentQName(), sharedComponentQName)) {
					use = existing;
					break;
				}
			}
		}
		if (use == null) {
			use = createUseSharedBean(target, sharedComponentQName);
			if (!useName.isBlank()) {
				use.setName(useName);
			}
			addChild(target, use, null);
			changed = true;
		} else {
			if (!useName.isBlank() && !safeEquals(use.getName(), useName)) {
				use.setName(useName);
				changed = true;
			}
			if (!safeEquals(use.getSharedComponentQName(), sharedComponentQName)) {
				use.setSharedComponentQName(sharedComponentQName);
				changed = true;
			}
			if (!use.isEnabled()) {
				use.setEnabled(true);
				changed = true;
			}
		}
		return new UpsertResult<>(use, changed);
	}

	private static UIUseShared createUseSharedBean(DatabaseObject target, String sharedComponentQName) throws Exception {
		UIUseShared use = null;
		ComponentManager manager = ComponentManager.of(target);
		if (manager != null) {
			DatabaseObject bean = manager.createBean(manager.getComponentByName(sharedComponentQName.substring(sharedComponentQName.lastIndexOf('.') + 1)));
			if (bean instanceof UIUseShared shared) {
				use = shared;
			}
		}
		if (use == null) {
			use = new UIUseShared();
		}
		use.setSharedComponentQName(sharedComponentQName);
		use.bNew = true;
		use.hasChanged = true;
		return use;
	}

	private static UIDynamicInvoke createInvokeBean(UIControlEvent event, String sharedActionQName) throws Exception {
		UIDynamicInvoke invoke = null;
		ComponentManager manager = ComponentManager.of(event);
		if (manager != null) {
			DatabaseObject bean = manager.createBean(manager.getComponentByName("InvokeAction"));
			if (bean instanceof UIDynamicInvoke dynamicInvoke) {
				invoke = dynamicInvoke;
			}
		}
		if (invoke == null) {
			invoke = new UIDynamicInvoke();
		}
		invoke.setSharedActionQName(sharedActionQName);
		invoke.bNew = true;
		invoke.hasChanged = true;
		return invoke;
	}

	private static boolean syncInvokeVariables(UIDynamicInvoke invoke, JSONObject create) throws Exception {
		JSONArray variables = create == null ? null : create.optJSONArray("variables");
		if (invoke == null || variables == null) {
			return false;
		}
		boolean changed = false;
		Map<String, UIControlVariable> existing = new HashMap<>();
		for (UIComponent child : invoke.getUIComponentList()) {
			if (child instanceof UIControlVariable variable) {
				existing.put(variable.getName(), variable);
			}
		}
		List<String> declared = new ArrayList<>();
		for (int i = 0; i < variables.length(); i++) {
			JSONObject spec = variables.optJSONObject(i);
			if (spec == null) {
				continue;
			}
			String name = spec.optString("name");
			if (name == null || name.isBlank()) {
				continue;
			}
			declared.add(name);
			UIControlVariable variable = existing.get(name);
			if (variable == null) {
				variable = new UIControlVariable();
				variable.setName(name);
				variable.bNew = true;
				variable.hasChanged = true;
				invoke.add(variable);
				existing.put(name, variable);
				changed = true;
			}
			MobileSmartSourceType imported = buildControlVariableSmartType(spec);
			if (!imported.equals(variable.getVarSmartType())) {
				variable.setVarSmartType(imported);
				changed = true;
			}
		}
		if (create.optBoolean("replaceVariables", true)) {
			for (UIComponent child : new ArrayList<>(invoke.getUIComponentList())) {
				if (child instanceof UIControlVariable variable && !declared.contains(variable.getName())) {
					invoke.remove(variable);
					changed = true;
				}
			}
		}
		return changed;
	}

	private static boolean syncUseSharedVariables(UIUseShared use, JSONObject create) throws Exception {
		JSONArray variables = create == null ? null : create.optJSONArray("variables");
		if (use == null || variables == null) {
			return false;
		}
		boolean changed = false;
		Map<String, UIUseVariable> existing = new HashMap<>();
		for (UIComponent child : use.getUIComponentList()) {
			if (child instanceof UIUseVariable variable) {
				existing.put(variable.getName(), variable);
			}
		}
		List<String> declared = new ArrayList<>();
		for (int i = 0; i < variables.length(); i++) {
			JSONObject spec = variables.optJSONObject(i);
			if (spec == null) {
				continue;
			}
			String name = spec.optString("name");
			if (name == null || name.isBlank()) {
				continue;
			}
			declared.add(name);
			UIUseVariable variable = existing.get(name);
			if (variable == null) {
				variable = new UIUseVariable();
				variable.setName(name);
				variable.bNew = true;
				variable.hasChanged = true;
				use.add(variable);
				existing.put(name, variable);
				changed = true;
			}
			MobileSmartSourceType imported = buildControlVariableSmartType(spec);
			if (!imported.equals(variable.getVarSmartType())) {
				variable.setVarSmartType(imported);
				changed = true;
			}
			String binding = spec.optString("binding");
			if (binding != null && !binding.isBlank()) {
				UIUseVariable.BindingType importedBinding = "twoWayBinding".equals(binding)
					? UIUseVariable.BindingType.twoWayBinding
					: UIUseVariable.BindingType.oneWayBinding;
				if (variable.getBinding() != importedBinding) {
					variable.setBinding(importedBinding);
					changed = true;
				}
			}
		}
		if (create.optBoolean("replaceVariables", true)) {
			for (UIComponent child : new ArrayList<>(use.getUIComponentList())) {
				if (child instanceof UIUseVariable variable && !declared.contains(variable.getName())) {
					use.remove(variable);
					changed = true;
				}
			}
		}
		return changed;
	}

	private static boolean applyTemplate(PageComponent page, Path htmlPath, ImportReport report) throws Exception {
		if (!Files.exists(htmlPath)) {
			report.warnings.add("Missing html file for page " + page.getQName() + ": " + htmlPath);
			return false;
		}
		String html = Files.readString(htmlPath, StandardCharsets.UTF_8);
		if (matchesGeneratedTemplate(page.getComputedTemplate(), html)) {
			return false;
		}
		return applyTemplateTransactional(page, html, report);
	}

	private static boolean applyTemplateDirect(PageComponent page, String html, ImportReport report) throws Exception {
		boolean hasExisting = hasExistingVisualContent(page);
		ExistingTargetSnapshot snapshot = hasExisting ? snapshotExistingTarget(page, getExistingTarget(page)) : null;
		ReconcileResult anchored = tryApplyAnchoredTemplate(page, html, report);
		if (anchored.matched) {
			return anchored.changed;
		}
		if (snapshot != null) {
			restoreExistingTarget(page, snapshot);
		}
		TemplateOutcome structured = tryApplyStructuredTemplate(page, html, report, !hasExisting);
		switch (structured) {
			case CHANGED: return true;
			case UNCHANGED: return false;
			case NOT_MATCHED: break;
		}
			if (hasExisting) {
				if (snapshot != null) {
					restoreExistingTarget(page, snapshot);
				}
				report.skipped++;
				report.warnings.add("Skipped HTML round-trip for page " + page.getQName()
					+ ": the modified HTML could not be mapped to the existing component tree; "
					+ "the project model has been left untouched to avoid losing bean metadata, "
					+ "named children, or isEnabled/i18n flags. See engine logs for the first mismatch trace. "
					+ "Apply the change from the Convertigo designer.");
				return false;
				}
		return applyTemplateFallback(page, html);
	}

	private static boolean applyTemplate(UISharedComponent component, Path htmlPath, ImportReport report) throws Exception {
		if (!Files.exists(htmlPath)) {
			report.warnings.add("Missing html file for component " + component.getQName() + ": " + htmlPath);
			return false;
		}
		String html = Files.readString(htmlPath, StandardCharsets.UTF_8);
		if (matchesGeneratedTemplate(component.getComputedTemplate(), html)) {
			return false;
		}
		return applyTemplateTransactional(component, html, report);
	}

	private static boolean applyTemplateDirect(UISharedComponent component, String html, ImportReport report) throws Exception {
		boolean hasExisting = hasExistingVisualContent(component);
		ExistingTargetSnapshot snapshot = hasExisting ? snapshotExistingTarget(component, getExistingTarget(component)) : null;
		ReconcileResult anchored = tryApplyAnchoredTemplate(component, html, report);
		if (anchored.matched) {
			return anchored.changed;
		}
		if (snapshot != null) {
			restoreExistingTarget(component, snapshot);
		}
		TemplateOutcome structured = tryApplyStructuredTemplate(component, html, report, !hasExisting);
		switch (structured) {
			case CHANGED: return true;
			case UNCHANGED: return false;
			case NOT_MATCHED: break;
		}
			if (hasExisting) {
				if (snapshot != null) {
					restoreExistingTarget(component, snapshot);
				}
				report.skipped++;
				report.warnings.add("Skipped HTML round-trip for shared component " + component.getQName()
					+ ": the modified HTML could not be mapped to the existing component tree; "
					+ "the project model has been left untouched to avoid losing bean metadata, "
					+ "named children, or isEnabled/i18n flags. See engine logs for the first mismatch trace. "
					+ "Apply the change from the Convertigo designer.");
				return false;
				}
		return applyTemplateFallback(component, html);
	}

	private static boolean hasExistingVisualContent(PageComponent page) {
		return !getExistingTarget(page).children.isEmpty();
	}

	private static boolean hasExistingVisualContent(UISharedComponent component) {
		return !getExistingTarget(component).children.isEmpty();
	}

	private static boolean applyTemplateTransactional(PageComponent page, String html, ImportReport report) throws Exception {
		if (!validateUniquePriorityAnchors(html, "page " + page.getQName(), report)) {
			return false;
		}
		ImportReport trial = new ImportReport(report.projectName);
		PageComponent detached = detachedCopy(page);
		applyTemplateDirect(detached, html, trial);
		if (trial.skipped > 0) {
			mergeTrialReport(report, trial);
			return false;
		}
		return applyTemplateDirect(page, html, report);
	}

	private static boolean applyTemplateTransactional(UISharedComponent component, String html, ImportReport report) throws Exception {
		if (!validateUniquePriorityAnchors(html, "shared component " + component.getQName(), report)) {
			return false;
		}
		ImportReport trial = new ImportReport(report.projectName);
		UISharedComponent detached = detachedCopy(component);
		applyTemplateDirect(detached, html, trial);
		if (trial.skipped > 0) {
			mergeTrialReport(report, trial);
			return false;
		}
		return applyTemplateDirect(component, html, report);
	}

	private static void mergeTrialReport(ImportReport report, ImportReport trial) {
		if (trial == null) {
			return;
		}
		report.skipped += trial.skipped;
		report.warnings.addAll(trial.warnings);
	}

	private static boolean validateUniquePriorityAnchors(String html, String label, ImportReport report) {
		Map<Long, List<String>> anchors = new HashMap<>();
		try {
			String xhtml = preprocessVoidTags(html);
			Document document = Jsoup.parse("<" + PARSE_ROOT_TAG + ">" + xhtml + "</" + PARSE_ROOT_TAG + ">", "", Parser.xmlParser());
			Element parseRoot = document.selectFirst(PARSE_ROOT_TAG);
			if (parseRoot == null) {
				return true;
			}
			for (Element element : parseRoot.getAllElements()) {
				if (PARSE_ROOT_TAG.equals(element.tagName()) || !element.hasAttr("class")) {
					continue;
				}
				List<Long> elementAnchors = new ArrayList<>();
				Matcher matcher = PRIORITY_CLASS_PATTERN.matcher(element.attr("class"));
				while (matcher.find()) {
					try {
						Long priority = Long.valueOf(matcher.group(1));
						elementAnchors.add(priority);
						anchors.computeIfAbsent(priority, ignored -> new ArrayList<>()).add(describeAnchorElement(element));
					} catch (Exception e) {
					}
				}
				if (elementAnchors.size() > 1) {
					return skipDuplicatePriorityAnchor(report, label,
						"element " + describeAnchorElement(element) + " contains multiple Convertigo priority anchors " + elementAnchors);
				}
			}
		} catch (Exception e) {
			return true;
		}
		for (Map.Entry<Long, List<String>> entry : anchors.entrySet()) {
			List<String> locations = entry.getValue();
			if (locations.size() > 1) {
				return skipDuplicatePriorityAnchor(report, label,
					"class" + entry.getKey() + " appears " + locations.size() + " times on " + String.join(", ", locations));
			}
		}
		return true;
	}

	private static boolean skipDuplicatePriorityAnchor(ImportReport report, String label, String detail) {
		report.skipped++;
		report.warnings.add("Skipped HTML round-trip for " + label + ": duplicate or ambiguous Convertigo priority anchor detected; "
			+ detail + ". A class<priority> token is an object id and may only stay on its original generated element.");
		return false;
	}

	private static String describeAnchorElement(Element element) {
		if (element == null) {
			return "<unknown>";
		}
		String id = element.id();
		return id == null || id.isBlank()
			? "<" + element.tagName() + ">"
			: "<" + element.tagName() + "#" + id + ">";
	}

	private static PageComponent detachedCopy(PageComponent page) throws Exception {
		PageComponent detached = (PageComponent) detachedCopy((DatabaseObject) page);
		setDetachedParent(detached, page.getParent());
		copyContainerChildren(page, detached);
		return detached;
	}

	private static UISharedComponent detachedCopy(UISharedComponent component) throws Exception {
		UISharedComponent detached = (UISharedComponent) detachedCopy((DatabaseObject) component);
		setDetachedParent(detached, component.getParent());
		copyContainerChildren(component, detached);
		return detached;
	}

	private static DatabaseObject detachedCopy(DatabaseObject databaseObject) throws Exception {
		org.w3c.dom.Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		return DatabaseObject.read(databaseObject.toXml(document, DatabaseObject.ExportOption.bIncludeVersion));
	}

	private static void copyContainerChildren(PageComponent source, PageComponent target) throws Exception {
		clearContainerChildren(target);
		Long after = null;
		for (UIComponent child : source.getUIComponentList()) {
			UIComponent restored = restoreComponentTree(target, snapshotComponentTree(child), after);
			after = restored.priority;
		}
	}

	private static void copyContainerChildren(UISharedComponent source, UISharedComponent target) throws Exception {
		clearContainerChildren(target);
		Long after = null;
		for (UIComponent child : source.getUIComponentList()) {
			UIComponent restored = restoreComponentTree(target, snapshotComponentTree(child), after);
			after = restored.priority;
		}
	}

	private static void clearContainerChildren(PageComponent page) throws Exception {
		for (UIComponent child : new ArrayList<>(page.getUIComponentList())) {
			page.remove(child);
		}
	}

	private static void clearContainerChildren(UISharedComponent component) throws Exception {
		for (UIComponent child : new ArrayList<>(component.getUIComponentList())) {
			component.remove(child);
		}
	}

	private static void setDetachedParent(DatabaseObject child, DatabaseObject parent) throws Exception {
		if (child == null) {
			return;
		}
		DATABASE_OBJECT_PARENT_FIELD.set(child, parent);
	}

	private enum TemplateOutcome {
		CHANGED,
		UNCHANGED,
		NOT_MATCHED
	}

	private static boolean applyTemplateFallback(PageComponent page, String html) throws Exception {
		removeManagedRoot(page);
		UICustom custom = ensureTemplate(page);
		if (!html.equals(custom.getCustomTemplate())) {
			custom.setCustomTemplate(html);
			return true;
		}
		custom.setEnabled(true);
		return false;
	}

	private static boolean applyTemplateFallback(UISharedComponent component, String html) throws Exception {
		removeManagedRoot(component);
		UICustom custom = ensureTemplate(component);
		if (!html.equals(custom.getCustomTemplate())) {
			custom.setCustomTemplate(html);
			return true;
		}
		custom.setEnabled(true);
		return false;
	}

	private static TemplateOutcome tryApplyStructuredTemplate(PageComponent page, String html, ImportReport report, boolean allowRebuild) throws Exception {
		ImportContext context = new ImportContext(report, page, collectExistingEvents(page), "page " + page.getQName());
		List<UIComponent> imported = parseStructuredChildren(html, page, context, "page " + page.getQName());
		if (imported == null) {
			return TemplateOutcome.NOT_MATCHED;
		}
		ExistingTarget target = getExistingTarget(page);
		imported = normalizeImportedChildren(imported, target);
		ReconcileResult reconciled = tryReconcileExisting(target, imported, context);
		if (reconciled.matched) {
			disableTemplate(page);
			if (reconciled.changed) {
				report.templatesUpdated++;
				return TemplateOutcome.CHANGED;
			}
			return TemplateOutcome.UNCHANGED;
		}
		if (!allowRebuild) {
			return TemplateOutcome.NOT_MATCHED;
		}
		removePageVisualComponents(page);
		removeManagedRoot(page);
		for (UIComponent child : imported) {
			page.add(child);
		}
		disableTemplate(page);
		report.templatesUpdated++;
		return TemplateOutcome.CHANGED;
	}

	private static TemplateOutcome tryApplyStructuredTemplate(UISharedComponent component, String html, ImportReport report, boolean allowRebuild) throws Exception {
		ImportContext context = new ImportContext(report, component, collectExistingEvents(component), "shared component " + component.getQName());
		List<UIComponent> imported = parseStructuredChildren(html, component, context, "shared component " + component.getQName());
		if (imported == null) {
			return TemplateOutcome.NOT_MATCHED;
		}
		ExistingTarget target = getExistingTarget(component);
		imported = normalizeImportedChildren(imported, target);
		ReconcileResult reconciled = tryReconcileExisting(target, imported, context);
		if (reconciled.matched) {
			disableTemplate(component);
			if (reconciled.changed) {
				report.templatesUpdated++;
				return TemplateOutcome.CHANGED;
			}
			return TemplateOutcome.UNCHANGED;
		}
		if (!allowRebuild) {
			return TemplateOutcome.NOT_MATCHED;
		}
		removeSharedVisualComponents(component);
		removeManagedRoot(component);
		for (UIComponent child : imported) {
			component.add(child);
		}
		disableTemplate(component);
		report.templatesUpdated++;
		return TemplateOutcome.CHANGED;
	}

	private static ReconcileResult tryApplyAnchoredTemplate(PageComponent page, String html, ImportReport report) throws Exception {
		ExistingTarget target = getExistingTarget(page);
		ImportContext context = new ImportContext(report, page, collectExistingEvents(page), "page " + page.getQName());
		List<Node> nodes = parseDomChildren(html, context, "page " + page.getQName());
		if (nodes == null) {
			return ReconcileResult.noMatch();
		}
		nodes = normalizeAnchoredDomChildren(nodes, target);
		ReconcileResult result = patchAnchoredChildren(page, nodes, target.children, context, "root");
		if (result.matched) {
			disableTemplate(page);
		}
		return result;
	}

	private static ReconcileResult tryApplyAnchoredTemplate(UISharedComponent component, String html, ImportReport report) throws Exception {
		ExistingTarget target = getExistingTarget(component);
		ImportContext context = new ImportContext(report, component, collectExistingEvents(component), "shared component " + component.getQName());
		List<Node> nodes = parseDomChildren(html, context, "shared component " + component.getQName());
		if (nodes == null) {
			return ReconcileResult.noMatch();
		}
		nodes = normalizeAnchoredDomChildren(nodes, target);
		ReconcileResult result = patchAnchoredChildren(component, nodes, target.children, context, "root");
		if (result.matched) {
			disableTemplate(component);
		}
		return result;
	}

	private static List<UIComponent> parseStructuredChildren(String html, DatabaseObject parent, ImportContext context, String label) {
		try {
			if (UNSUPPORTED_BLOCK_SYNTAX.matcher(html).find()) {
				context.report.warnings.add("Structured Ionic import skipped for " + label + ": Angular block syntax is not supported yet.");
				return null;
			}
			String xhtml = preprocessVoidTags(html);
			Document document = Jsoup.parse("<" + PARSE_ROOT_TAG + ">" + xhtml + "</" + PARSE_ROOT_TAG + ">", "", Parser.xmlParser());
				Element parseRoot = document.selectFirst(PARSE_ROOT_TAG);
				if (parseRoot == null) {
					context.report.warnings.add("Structured Ionic import skipped for " + label + ": unable to locate parse root.");
					return null;
				}
				context.declaredTemplateIds.addAll(collectDeclaredTemplateIds(parseRoot));
				return importChildren(parseRoot, parent, context);
		} catch (Exception e) {
			context.report.warnings.add("Structured Ionic import skipped for " + label + ": " + e.getMessage());
			return null;
		}
	}

	private static List<Node> parseDomChildren(String html, ImportContext context, String label) {
		try {
			if (UNSUPPORTED_BLOCK_SYNTAX.matcher(html).find()) {
				return null;
			}
			String xhtml = preprocessVoidTags(html);
			Document document = Jsoup.parse("<" + PARSE_ROOT_TAG + ">" + xhtml + "</" + PARSE_ROOT_TAG + ">", "", Parser.xmlParser());
				Element parseRoot = document.selectFirst(PARSE_ROOT_TAG);
				if (parseRoot == null) {
					return null;
				}
				context.declaredTemplateIds.addAll(collectDeclaredTemplateIds(parseRoot));
				return getMeaningfulDomChildren(parseRoot);
		} catch (Exception e) {
			context.report.warnings.add("Anchored Ionic import skipped for " + label + ": " + e.getMessage());
			return null;
		}
	}

	private static List<Node> getMeaningfulDomChildren(Node source) {
		List<Node> nodes = new ArrayList<>();
			for (Node child : source.childNodes()) {
				if (child instanceof Comment) {
					continue;
				}
				if (child instanceof Element element && isReferenceOnlyTemplateElement(element)) {
					continue;
				}
				if (child instanceof Element element && isTransparentNgContainer(element) && extractDirectives(element.attributes()).isEmpty()) {
					nodes.addAll(getMeaningfulDomChildren(element));
					continue;
			}
			if (child instanceof TextNode textNode && importText(textNode).isEmpty()) {
				continue;
			}
			nodes.add(child);
		}
		return nodes;
	}

	private static List<UIComponent> importChildren(Node source, DatabaseObject parent, ImportContext context) throws Exception {
		List<UIComponent> components = new ArrayList<>();
			for (Node child : source.childNodes()) {
				if (child instanceof TextNode textNode) {
					components.addAll(importText(textNode));
					continue;
				}
				if (child instanceof Element element && isReferenceOnlyTemplateElement(element)) {
					continue;
				}
				if (child instanceof Element element && isTransparentNgContainer(element)) {
				List<DirectiveSpec> directives = extractDirectives(element.attributes());
				if (directives.isEmpty()) {
					components.addAll(importChildren(element, parent, context));
				} else {
					UIComponent imported = importTransparentDirectiveContainer(element, parent, context, directives);
					if (imported != null) {
						components.add(imported);
					}
				}
				continue;
			}
			UIComponent imported = importNode(child, parent, context);
			if (imported != null) {
				components.add(imported);
			}
		}
		return components;
	}

	private static UIComponent importTransparentDirectiveContainer(Element element, DatabaseObject parent, ImportContext context, List<DirectiveSpec> directives) throws Exception {
		if (element == null || directives == null || directives.isEmpty()) {
			return null;
		}
		UIComponent top = null;
		DatabaseObject currentParent = parent;
		UIControlDirective lastDirective = null;
		for (DirectiveSpec directive : directives) {
			UIControlDirective wrapper = reuseDirectiveIfPossible(buildDirective(directive), context);
			if (top == null) {
				top = wrapper;
			} else {
				lastDirective.add(wrapper);
			}
			lastDirective = wrapper;
			currentParent = wrapper;
		}
		for (UIComponent child : importChildren(element, currentParent, context)) {
			currentParent.add(child);
		}
		return top;
	}

	private static UIComponent importNode(Node node, DatabaseObject parent, ImportContext context) throws Exception {
		if (node instanceof Comment) {
			return null;
		}
		if (node instanceof TextNode textNode) {
			List<UIText> texts = importText(textNode);
			if (texts.isEmpty()) {
				return null;
			}
			return texts.get(0);
		}
		if (node instanceof Element element) {
			return importElement(element, parent, context);
		}
		return null;
	}

	private static UIComponent importElement(Element element, DatabaseObject parent, ImportContext context) throws Exception {
		UIComponent actual = createComponentForElement(element, parent, context);
		if (actual instanceof UIUseShared) {
			applyAttributes(actual, element.attributes(), context);
			for (UIComponent child : importChildren(element, actual, context)) {
				actual.add(child);
			}
			return actual;
		}

		List<DirectiveSpec> directives = extractDirectives(element.attributes());
		UIComponent top = null;
		DatabaseObject currentParent = parent;
		UIControlDirective lastDirective = null;
		for (DirectiveSpec directive : directives) {
			UIControlDirective wrapper = reuseDirectiveIfPossible(buildDirective(directive), context);
			if (top == null) {
				top = wrapper;
			} else {
				lastDirective.add(wrapper);
			}
			lastDirective = wrapper;
			currentParent = wrapper;
		}

		actual = createComponentForElement(element, currentParent, context);
		if (top == null) {
			top = actual;
		} else {
			lastDirective.add(actual);
		}

		applyAttributes(actual, element.attributes(), context);
		if (!(actual instanceof UIElement uiElement && uiElement.isSelfClose())) {
			for (UIComponent child : importChildren(element, actual, context)) {
				actual.add(child);
			}
		}
		return top;
	}

	private static List<DirectiveSpec> extractDirectives(Attributes attributes) {
		List<DirectiveSpec> directives = new ArrayList<>();
		for (Attribute attribute : attributes) {
			String key = attribute.getKey();
			if ("*ngIf".equals(key)) {
				directives.add(new DirectiveSpec(UIControlDirective.AttrDirective.If, attribute.getValue()));
			} else if ("*ngFor".equals(key)) {
				directives.add(new DirectiveSpec(UIControlDirective.AttrDirective.ForEach, attribute.getValue()));
			} else if ("*cdkVirtualFor".equals(key)) {
				directives.add(new DirectiveSpec(UIControlDirective.AttrDirective.CdkVirtualFor, attribute.getValue()));
			} else if ("[ngSwitch]".equals(key)) {
				directives.add(new DirectiveSpec(UIControlDirective.AttrDirective.Switch, attribute.getValue()));
			} else if ("*ngSwitchCase".equals(key)) {
				directives.add(new DirectiveSpec(UIControlDirective.AttrDirective.SwitchCase, attribute.getValue()));
			} else if ("*ngSwitchDefault".equals(key)) {
				directives.add(new DirectiveSpec(UIControlDirective.AttrDirective.SwitchDefault, attribute.getValue()));
			}
		}
		return directives;
	}

	private static UIControlDirective buildDirective(DirectiveSpec spec) {
		UIControlDirective directive = new UIControlDirective();
		directive.setDirectiveName(spec.kind.name());
		if (spec.kind == UIControlDirective.AttrDirective.ForEach || spec.kind == UIControlDirective.AttrDirective.CdkVirtualFor) {
			configureForDirective(directive, spec.value);
		} else if (spec.kind != UIControlDirective.AttrDirective.SwitchDefault) {
			directive.setSourceSmartType(newScriptSmartType(spec.value));
		}
		return directive;
	}

	private static void configureForDirective(UIControlDirective directive, String expression) {
		String rawExpression = expression == null ? "" : expression.trim();
		ForDirectiveParts parts = new ForDirectiveParts();
		for (String clause : rawExpression.split(";")) {
			consumeForDirectiveClause(parts, clause, 0);
		}
		if (parts.itemName.isEmpty() && parts.source.isEmpty()) {
			parts.itemName = "item";
		}
		if (parts.source.isEmpty()) {
			parts.source = rawExpression;
		}

		directive.setDirectiveItemName(parts.itemName);
		directive.setDirectiveIndexName(parts.indexName);
		directive.setSourceSmartType(newScriptSmartType(parts.source));
		directive.setDirectiveExpression(String.join("; ", parts.extraClauses));
	}

	private static void consumeForDirectiveClause(ForDirectiveParts parts, String rawClause, int depth) {
		if (parts == null || rawClause == null || depth > 8) {
			return;
		}
		String clause = rawClause.trim();
		if (clause.isEmpty()) {
			return;
		}
		Matcher indexMatcher = FOR_INDEX_PATTERN.matcher(clause);
		if (indexMatcher.matches()) {
			parts.indexName = indexMatcher.group(1).trim();
			return;
		}
		Matcher indexAsMatcher = FOR_INDEX_AS_PATTERN.matcher(clause);
		if (indexAsMatcher.matches()) {
			parts.indexName = indexAsMatcher.group(1).trim();
			return;
		}
		Matcher forOfMatcher = FOR_OF_PATTERN.matcher(clause);
		if (forOfMatcher.matches()) {
			String candidateItem = forOfMatcher.group(1).trim();
			String candidateSource = forOfMatcher.group(2).trim();
			if (isRecoverableNestedForClause(candidateSource)) {
				consumeForDirectiveClause(parts, candidateSource, depth + 1);
				return;
			}
			consumeForDirectiveSource(parts, candidateItem, candidateSource, clause);
			return;
		}
		parts.extraClauses.add(clause);
	}

	private static boolean isRecoverableNestedForClause(String value) {
		if (value == null) {
			return false;
		}
		String trimmed = value.trim();
		return FOR_INDEX_PATTERN.matcher(trimmed).matches()
			|| FOR_INDEX_AS_PATTERN.matcher(trimmed).matches()
			|| FOR_OF_PATTERN.matcher(trimmed).matches();
	}

	private static void consumeForDirectiveSource(ForDirectiveParts parts, String candidateItem, String candidateSource, String clause) {
		boolean syntheticAlias = isSyntheticDirectiveAlias(candidateItem);
		if (parts.source.isEmpty()) {
			parts.source = candidateSource;
			parts.itemName = syntheticAlias ? "" : candidateItem;
			return;
		}
		if (normalizeAngularExpression(parts.source).equals(normalizeAngularExpression(candidateSource))) {
			if ((parts.itemName.isEmpty() || isSyntheticDirectiveAlias(parts.itemName)) && !syntheticAlias) {
				parts.itemName = candidateItem;
			} else if (!safeEquals(parts.itemName, candidateItem) && !syntheticAlias) {
				parts.extraClauses.add(clause);
			}
			return;
		}
		if (!syntheticAlias) {
			parts.extraClauses.add(clause);
		}
	}

	private static boolean isSyntheticDirectiveAlias(String value) {
		return value != null && value.matches("item\\d+");
	}

	private static UIComponent createComponentForElement(Element element, DatabaseObject parent, ImportContext context) throws Exception {
		String tag = element.tagName();
		UIUseShared shared = tryCreateUseShared(tag, parent);
		if (shared != null) {
			shared = reuseUseSharedIfPossible(shared, context);
			shared.setEnabled(true);
			return shared;
		}
		UIComponent palette = tryCreatePaletteComponent(tag, parent, context);
		if (palette != null) {
			if (palette instanceof UIUseShared paletteShared) {
				palette = reuseUseSharedIfPossible(paletteShared, context);
			}
			palette.setEnabled(true);
			return palette;
		}

		UIElement generic = new UIElement();
		generic.setTagName(tag);
		generic.setSelfClose(VOID_TAGS.contains(tag));
		generic.setEnabled(true);
		return reuseNamedTemplateIfPossible(generic, element, context);
	}

	private static ReconcileResult tryReconcileExisting(ExistingTarget target, List<UIComponent> imported, ImportContext context) throws Exception {
		if (target.children.isEmpty()) {
			return ReconcileResult.noMatch();
		}
		return reconcileComponentLists(target.container, imported, target.children, context, "root");
	}

	private static List<UIComponent> normalizeImportedChildren(List<UIComponent> imported, ExistingTarget target) {
		if (target == null || target.managedRoot == null || imported == null || imported.size() != 1) {
			return imported;
		}
		UIComponent first = imported.get(0);
		if (first instanceof UIElement element && ROOT_TAG_NAME.equals(element.getTagName())) {
			return getReconcilableChildren(element);
		}
		return imported;
	}

	private static List<Node> normalizeAnchoredDomChildren(List<Node> nodes, ExistingTarget target) {
		if (target == null || target.managedRoot == null || nodes == null || nodes.size() != 1) {
			return nodes;
		}
		Node first = nodes.get(0);
		if (first instanceof Element element && ROOT_TAG_NAME.equals(element.tagName())) {
			return getMeaningfulDomChildren(element);
		}
		return nodes;
	}

	private static ExistingTarget getExistingTarget(PageComponent page) {
		UIElement root = getEnabledManagedRoot(page);
		if (root != null) {
			return new ExistingTarget(root, root, getReconcilableChildren(root));
		}
		return new ExistingTarget(page, null, getPageVisualChildren(page, true));
	}

	private static ExistingTarget getExistingTarget(UISharedComponent component) {
		UIElement root = getEnabledManagedRoot(component);
		if (root != null) {
			return new ExistingTarget(root, root, getReconcilableChildren(root));
		}
		return new ExistingTarget(component, null, getSharedVisualChildren(component, true));
	}

	private static ExistingTargetSnapshot snapshotExistingTarget(PageComponent page, ExistingTarget target) throws Exception {
		return snapshotExistingTarget(target);
	}

	private static ExistingTargetSnapshot snapshotExistingTarget(UISharedComponent component, ExistingTarget target) throws Exception {
		return snapshotExistingTarget(target);
	}

	private static ExistingTargetSnapshot snapshotExistingTarget(ExistingTarget target) throws Exception {
		List<UIComponent> snapshotChildren = getSnapshotChildren(target);
		if (target == null || snapshotChildren.isEmpty()) {
			return null;
		}
		Long afterPriority = null;
		DatabaseObject first = snapshotChildren.get(0);
		DatabaseObject previous = first == null ? null : first.getPreviousSiblingInFolder();
		if (previous != null) {
			afterPriority = previous.priority;
		}
		List<ComponentSnapshot> children = new ArrayList<>(snapshotChildren.size());
		for (UIComponent child : snapshotChildren) {
			children.add(snapshotComponentTree(child));
		}
		return new ExistingTargetSnapshot(target.managedRoot != null, afterPriority, children);
	}

	private static void restoreExistingTarget(PageComponent page, ExistingTargetSnapshot snapshot) throws Exception {
		if (snapshot == null) {
			return;
		}
			if (snapshot.usesManagedRoot) {
				UIElement root = ensureRoot(page);
				removeStructuralChildren(root);
				restoreChildren(root, snapshot);
				return;
			}
		removePageVisualComponents(page);
		restoreChildren(page, snapshot);
	}

	private static void restoreExistingTarget(UISharedComponent component, ExistingTargetSnapshot snapshot) throws Exception {
		if (snapshot == null) {
			return;
		}
			if (snapshot.usesManagedRoot) {
				UIElement root = ensureRoot(component);
				removeStructuralChildren(root);
				restoreChildren(root, snapshot);
				return;
			}
		removeSharedVisualComponents(component);
		restoreChildren(component, snapshot);
	}

	private static void restoreChildren(DatabaseObject parent, ExistingTargetSnapshot snapshot) throws Exception {
		Long after = snapshot.afterPriority;
		for (ComponentSnapshot child : snapshot.children) {
			UIComponent restored = restoreComponentTree(parent, child, after);
			after = restored.priority;
		}
	}

	private static List<UIComponent> getSnapshotChildren(ExistingTarget target) {
		if (target == null) {
			return List.of();
		}
		if (target.managedRoot != null) {
			return getStructuralChildren(target.managedRoot);
		}
		if (target.container instanceof PageComponent page) {
			return getPageVisualChildren(page, false);
		}
		if (target.container instanceof UISharedComponent component) {
			return getSharedVisualChildren(component, false);
		}
		return List.of();
	}

	private static ComponentSnapshot snapshotComponentTree(UIComponent component) throws Exception {
		org.w3c.dom.Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		org.w3c.dom.Element objectXml = (org.w3c.dom.Element) component
			.toXml(document, DatabaseObject.ExportOption.bIncludeVersion)
			.cloneNode(true);
		List<ComponentSnapshot> children = new ArrayList<>();
		for (UIComponent child : component.getUIComponentList()) {
			children.add(snapshotComponentTree(child));
		}
		return new ComponentSnapshot(objectXml, children);
	}

	private static UIComponent restoreComponentTree(DatabaseObject parent, ComponentSnapshot snapshot, Long after) throws Exception {
		UIComponent restored = (UIComponent) DatabaseObject.read((org.w3c.dom.Node) snapshot.objectXml.cloneNode(true));
		addChild(parent, restored, after);
		Long childAfter = null;
		for (ComponentSnapshot child : snapshot.children) {
			UIComponent restoredChild = restoreComponentTree(restored, child, childAfter);
			childAfter = restoredChild.priority;
		}
		return restored;
	}

	private static Map<Long, UIControlEvent> collectExistingEvents(DatabaseObject root) throws Exception {
		Map<Long, UIControlEvent> events = new HashMap<>();
		if (root == null) {
			return events;
		}
		for (DatabaseObject child : root.getDatabaseObjectChildren(true)) {
			if (child instanceof UIControlEvent event) {
				events.put(event.priority, event);
			}
		}
		return events;
	}

	private static UIElement getEnabledManagedRoot(PageComponent page) {
		for (UIComponent component : page.getUIComponentList()) {
			if (component instanceof UIElement element && ROOT_NAME.equals(component.getName()) && component.isEnabled()) {
				return element;
			}
		}
		return null;
	}

	private static UIElement getEnabledManagedRoot(UISharedComponent component) {
		for (UIComponent child : component.getUIComponentList()) {
			if (child instanceof UIElement element && ROOT_NAME.equals(child.getName()) && child.isEnabled()) {
				return element;
			}
		}
		return null;
	}

	private static List<UIComponent> getReconcilableChildren(UIComponent parent) {
		return getStructuralChildren(parent, true);
	}

	private static List<UIComponent> getStructuralChildren(UIComponent parent) {
		return getStructuralChildren(parent, false);
	}

	private static List<UIComponent> getStructuralChildren(UIComponent parent, boolean enabledOnly) {
		List<UIComponent> children = new ArrayList<>();
		for (UIComponent child : parent.getUIComponentList()) {
			if (child == null || (enabledOnly && !child.isEnabled())) {
				continue;
			}
			if (isNonStructuralChild(child)) {
				continue;
			}
			children.add(child);
		}
		return children;
	}

	private static List<UIComponent> getPageVisualChildren(PageComponent page, boolean enabledOnly) {
		List<UIComponent> children = new ArrayList<>();
		for (UIComponent component : page.getUIComponentList()) {
			if (component == null || (enabledOnly && !component.isEnabled())) {
				continue;
			}
			if (isManaged(component) || component instanceof UIPageEvent || component instanceof UIEventSubscriber || component instanceof UIStyle || component instanceof UICustom) {
				continue;
			}
			children.add(component);
		}
		return children;
	}

	private static List<UIComponent> getSharedVisualChildren(UISharedComponent component, boolean enabledOnly) {
		List<UIComponent> children = new ArrayList<>();
		for (UIComponent child : component.getUIComponentList()) {
			if (child == null || (enabledOnly && !child.isEnabled())) {
				continue;
			}
			if (isManaged(child) || child instanceof UICompVariable || child instanceof UICompEvent || child instanceof UISharedComponentEvent || child instanceof UIStyle || child instanceof UICustom) {
				continue;
			}
			children.add(child);
		}
		return children;
	}

	private static boolean isNonStructuralChild(UIComponent component) {
		return isAttributeComponent(component)
			|| component instanceof UIStyle
			|| component instanceof UIControlEvent
			|| component instanceof UIAnimation
			|| component instanceof UIControlVariable
			|| component instanceof UICompVariable
			|| component instanceof UICompEvent
			|| component instanceof UIPageEvent
			|| component instanceof UIEventSubscriber
			|| component instanceof UISharedComponentEvent;
	}

	private static boolean isAttributeComponent(UIComponent component) {
		return component instanceof UIAttribute || component instanceof UIDynamicAttr;
	}

	private static ReconcileResult reconcileComponentLists(DatabaseObject existingParent, List<UIComponent> imported, List<UIComponent> existing, ImportContext context, String path) throws Exception {
		boolean changed = false;
		int existingIndex = 0;
		Long after = 0L;
			for (int i = 0; i < imported.size(); i++) {
				UIComponent importedChild = imported.get(i);
				String childPath = childPath(path, importedChild, i);
			if (importedChild instanceof UIText importedText) {
				if (existingIndex < existing.size() && existing.get(existingIndex) instanceof UIText existingText) {
					ReconcileResult result = reconcileComponent(importedText, existingText, context, childPath);
					if (!result.matched) {
						return result;
					}
					changed |= result.changed;
					after = existingText.priority;
					existingIndex++;
					continue;
				}
				if (existingParent == null) {
					return noMatch(context, childPath + ": missing parent container for inserted text node");
				}
				addChild(existingParent, importedText, after);
				after = importedText.priority;
					changed = true;
					continue;
				}
				if (isReferenceOnlyTemplateComponent(importedChild)) {
					continue;
				}
				while (existingIndex < existing.size() && existing.get(existingIndex) instanceof UIText removableText) {
				if (existingParent == null) {
					return noMatch(context, childPath + ": missing parent container for removed text node");
				}
				removeChild(existingParent, removableText);
				existingIndex++;
				changed = true;
			}
			Set<Long> remainingPriorities = collectComponentPriorities(imported, i);
			while (existingIndex < existing.size() && shouldUnwrapMissingExistingChild(existing.get(existingIndex), remainingPriorities, context)) {
				unwrapExistingChild(existingParent, existing, existingIndex, after);
				changed = true;
			}
			while (existingIndex < existing.size() && shouldRemoveMissingExistingChild(existing.get(existingIndex), remainingPriorities, context)) {
				if (existingParent == null) {
					return noMatch(context, childPath + ": missing parent container for removed "
						+ describeComponent(existing.get(existingIndex)));
				}
				removeChild(existingParent, existing.get(existingIndex));
				existingIndex++;
				changed = true;
			}
			if (existingIndex >= existing.size()) {
				return noMatch(context, childPath + ": missing existing child for imported " + describeComponent(importedChild));
			}
			ReconcileResult result = reconcileComponent(importedChild, existing.get(existingIndex), context, childPath);
			if (!result.matched) {
				return result;
			}
			changed |= result.changed;
			after = existing.get(existingIndex).priority;
			existingIndex++;
		}
			while (existingIndex < existing.size() && existing.get(existingIndex) instanceof UIText removableText) {
				if (existingParent == null) {
					return noMatch(context, path + ": missing parent container for trailing removed text node");
				}
				removeChild(existingParent, removableText);
				existingIndex++;
				changed = true;
			}
			while (existingIndex < existing.size() && shouldRemoveMissingExistingChild(existing.get(existingIndex), Set.of(), context)) {
				if (existingParent == null) {
					return noMatch(context, path + ": missing parent container for trailing removed "
						+ describeComponent(existing.get(existingIndex)));
				}
				removeChild(existingParent, existing.get(existingIndex));
				existingIndex++;
				changed = true;
			}
			while (existingIndex < existing.size() && canRetainNamedTemplate(existing.get(existingIndex), context)) {
				existingIndex++;
			}
			if (existingIndex != existing.size()) {
				return noMatch(context, path + ": child count mismatch imported=" + imported.size() + ", existing=" + existing.size());
			}
		return ReconcileResult.match(changed);
	}

	private static ReconcileResult reconcileComponent(UIComponent imported, UIComponent existing, ImportContext context, String path) throws Exception {
		if (imported instanceof UIText importedText && existing instanceof UIText existingText) {
			if (hasEquivalentRenderedText(importedText, existingText)) {
				return ReconcileResult.match(false);
			}
			boolean changed = false;
			if (!importedText.getTextSmartType().equals(existingText.getTextSmartType())) {
				existingText.setTextSmartType(importedText.getTextSmartType().clone());
				changed = true;
			}
			if (importedText.isI18n() != existingText.isI18n()) {
				existingText.setI18n(importedText.isI18n());
				changed = true;
			}
			return ReconcileResult.match(changed);
		}
		if (imported instanceof UIUseShared importedShared && existing instanceof UIUseShared existingShared) {
			if (!safeEquals(importedShared.getSharedComponentQName(), existingShared.getSharedComponentQName())) {
				return noMatch(context, path + ": shared component mismatch imported="
					+ importedShared.getSharedComponentQName() + ", existing=" + existingShared.getSharedComponentQName());
			}
					return reconcileComponentLists(existingShared, getReconcilableChildren(importedShared), getReconcilableChildren(existingShared), context, path);
		}
		if (imported instanceof UIControlDirective importedDirective && existing instanceof UIElement existingElement) {
			ReconcileResult absorbed = tryReconcileDirectiveWrappedElement(importedDirective, existingElement, context, path);
			if (absorbed != null) {
				return absorbed;
			}
		}
		if (imported instanceof UIControlDirective importedDirective && existing instanceof UIControlDirective existingDirective) {
			if (!safeEquals(importedDirective.getDirectiveName(), existingDirective.getDirectiveName())) {
				return noMatch(context, path + ": directive mismatch imported="
					+ importedDirective.getDirectiveName() + ", existing=" + existingDirective.getDirectiveName());
			}
			boolean changed = false;
			if (!safeEquals(importedDirective.getDirectiveItemName(), existingDirective.getDirectiveItemName())) {
				existingDirective.setDirectiveItemName(importedDirective.getDirectiveItemName());
				changed = true;
			}
			if (!safeEquals(importedDirective.getDirectiveIndexName(), existingDirective.getDirectiveIndexName())) {
				existingDirective.setDirectiveIndexName(importedDirective.getDirectiveIndexName());
				changed = true;
			}
			if (!safeEquals(importedDirective.getDirectiveExpression(), existingDirective.getDirectiveExpression())) {
				existingDirective.setDirectiveExpression(importedDirective.getDirectiveExpression());
				changed = true;
			}
			if (!hasEquivalentDirectiveSource(existingDirective, importedDirective)) {
				existingDirective.setSourceSmartType(importedDirective.getSourceSmartType().clone());
				changed = true;
			}
			ReconcileResult children = reconcileComponentLists(existingDirective, getReconcilableChildren(importedDirective), getReconcilableChildren(existingDirective), context, path);
			if (!children.matched) {
				return children;
			}
			return ReconcileResult.match(changed || children.changed);
		}
		if (imported instanceof UIElement importedElement && existing instanceof UIElement existingElement) {
			if (!sameStructuralTag(importedElement.getTagName(), existingElement.getTagName())) {
				return noMatch(context, path + ": tag mismatch imported=<" + importedElement.getTagName()
					+ ">, existing=<" + existingElement.getTagName() + ">");
			}
			boolean changed = false;
			if (!safeEquals(importedElement.getIdentifier(), existingElement.getIdentifier())) {
				existingElement.setIdentifier(importedElement.getIdentifier());
				changed = true;
			}
			ReconcileResult children = reconcileComponentLists(existingElement, getReconcilableChildren(importedElement), getReconcilableChildren(existingElement), context, path);
			if (!children.matched) {
				return children;
			}
			return ReconcileResult.match(changed || children.changed);
		}
		return noMatch(context, path + ": incompatible node types imported=" + describeComponent(imported)
			+ ", existing=" + describeComponent(existing));
	}

	private static ReconcileResult patchAnchoredChildren(DatabaseObject parent, List<Node> domNodes, List<UIComponent> existingChildren, ImportContext context, String path) throws Exception {
		if (parent == null || domNodes == null || existingChildren == null) {
			return noMatch(context, path + ": missing anchored reconciliation inputs");
		}
		boolean changed = false;
		int existingIndex = 0;
		Long after = 0L;
			for (int domIndex = 0; domIndex < domNodes.size(); domIndex++) {
				Node domNode = domNodes.get(domIndex);
				String nodePath = childPath(path, domNode, domIndex);
			if (domNode instanceof TextNode textNode) {
				List<UIText> importedTexts = importText(textNode);
				for (int textIndex = 0; textIndex < importedTexts.size(); textIndex++) {
					UIText importedText = importedTexts.get(textIndex);
					String textPath = nodePath + "/#text[" + (textIndex + 1) + "]";
					if (existingIndex < existingChildren.size() && existingChildren.get(existingIndex) instanceof UIText existingText) {
						ReconcileResult result = reconcileComponent(importedText, existingText, context, textPath);
						if (!result.matched) {
							return result;
						}
						changed |= result.changed;
						after = existingText.priority;
						existingIndex++;
						continue;
					}
					addChild(parent, importedText, after);
					after = importedText.priority;
					changed = true;
					}
					continue;
				}
				if (isReferenceOnlyTemplateNode(domNode)) {
					continue;
				}
				Set<Long> remainingDomPriorities = collectAnchoredPriorities(domNodes, domIndex);
				while (existingIndex < existingChildren.size() && shouldUnwrapMissingExistingChild(existingChildren.get(existingIndex), remainingDomPriorities, context)) {
					unwrapExistingChild(parent, existingChildren, existingIndex, after);
					changed = true;
				}
				while (existingIndex < existingChildren.size() && shouldRemoveMissingExistingChild(existingChildren.get(existingIndex), remainingDomPriorities, context)) {
					removeChild(parent, existingChildren.get(existingIndex));
					existingIndex++;
					changed = true;
				}
				while (existingIndex < existingChildren.size() && existingChildren.get(existingIndex) instanceof UIText removableText) {
					removeChild(parent, removableText);
					existingIndex++;
					changed = true;
				}
				if (existingIndex < existingChildren.size()) {
					UIComponent existing = existingChildren.get(existingIndex);
					boolean absorbFollowingSiblings = false;
					Set<Long> remainingAnchors = Set.of();
					List<UIComponent> absorbedSiblings = List.of();
					if (domNode instanceof Element elementNode && existing instanceof UIElement existingElement) {
						absorbFollowingSiblings = !hasPriorityAnchor(domNode)
							&& canReplaceStructuralSubtree(existingElement, elementNode)
							&& !safeEquals(elementNode.tagName(), existingElement.getTagName());
						if (absorbFollowingSiblings) {
							remainingAnchors = collectAnchoredPriorities(domNodes, domIndex + 1);
							absorbedSiblings = collectAbsorbableSiblings(parent, existing, remainingAnchors);
						}
					}
					ImportContext patchContext = hasPriorityAnchor(domNode) ? context : silentMismatchContext(context);
					if (!absorbedSiblings.isEmpty()) {
						patchContext = withReusePool(patchContext, mergeReusePools(patchContext == null ? null : patchContext.reusePool, collectReusableComponents(absorbedSiblings)));
					}
					ReconcileResult result = patchAnchoredNode(domNode, existing, patchContext, nodePath);
					if (result.matched) {
						changed |= result.changed;
						after = existing.priority;
						int absorbedCount = 0;
						if (absorbFollowingSiblings) {
							while (absorbedCount < absorbedSiblings.size()) {
								UIComponent absorbed = absorbedSiblings.get(absorbedCount);
								removeChild(parent, absorbed);
								absorbedCount++;
								changed = true;
							}
						}
						existingIndex += countReconcilableComponents(absorbedSiblings);
						existingIndex++;
						continue;
					}
				}
			if (hasPriorityAnchor(domNode)) {
				String expected = existingIndex < existingChildren.size() ? describeComponent(existingChildren.get(existingIndex)) : "<none>";
				return noMatch(context, nodePath + ": anchored DOM node " + describeNode(domNode)
					+ " could not match existing child " + expected);
			}
			UIComponent imported = importNode(domNode, parent, context);
			if (imported == null) {
				continue;
			}
			addChild(parent, imported, after);
			after = imported.priority;
			changed = true;
		}
			while (existingIndex < existingChildren.size() && existingChildren.get(existingIndex) instanceof UIText removableText) {
				removeChild(parent, removableText);
				existingIndex++;
				changed = true;
			}
			while (existingIndex < existingChildren.size() && shouldRemoveMissingExistingChild(existingChildren.get(existingIndex), Set.of(), context)) {
				removeChild(parent, existingChildren.get(existingIndex));
				existingIndex++;
				changed = true;
			}
			while (existingIndex < existingChildren.size() && canRetainNamedTemplate(existingChildren.get(existingIndex), context)) {
				existingIndex++;
			}
			if (existingIndex != existingChildren.size()) {
				return noMatch(context, path + ": remaining existing child "
					+ describeComponent(existingChildren.get(existingIndex)) + " has no DOM counterpart");
		}
		return ReconcileResult.match(changed);
	}

	private static ReconcileResult patchAnchoredNode(Node node, UIComponent existing, ImportContext context, String path) throws Exception {
		if (node instanceof TextNode textNode && existing instanceof UIText existingText) {
			List<UIText> importedTexts = importText(textNode);
			if (importedTexts.size() != 1) {
				return noMatch(context, path + ": text node expanded to " + importedTexts.size() + " imported nodes");
			}
			return reconcileComponent(importedTexts.get(0), existingText, context, path);
		}
		if (existing instanceof UIControlDirective existingDirective && node instanceof Element elementNode) {
			return patchAnchoredDirective(elementNode, existingDirective, context, path);
		}
		if (existing instanceof UIUseShared existingShared && node instanceof Element elementNode) {
			return patchAnchoredUseShared(elementNode, existingShared, context, path);
		}
		if (existing instanceof UIElement existingElement && node instanceof Element elementNode) {
			return patchAnchoredElement(elementNode, existingElement, context, path);
		}
		return noMatch(context, path + ": incompatible anchored node DOM=" + describeNode(node)
			+ ", existing=" + describeComponent(existing));
	}

	private static ReconcileResult patchAnchoredDirective(Element element, UIControlDirective existing, ImportContext context, String path) throws Exception {
		List<DirectiveSpec> directives = extractDirectives(element.attributes());
		if (directives.isEmpty()) {
			return noMatch(context, path + ": expected directive " + existing.getDirectiveName()
				+ " but DOM element <" + element.tagName() + "> has none");
		}
		DirectiveSpec directive = directives.get(0);
		if (!safeEquals(directive.kind.name(), existing.getDirectiveName())) {
			return noMatch(context, path + ": directive mismatch DOM=" + directive.kind.name()
				+ ", existing=" + existing.getDirectiveName());
		}
		UIControlDirective imported = buildDirective(directive);
		boolean changed = false;
		if (!safeEquals(imported.getDirectiveItemName(), existing.getDirectiveItemName())) {
			existing.setDirectiveItemName(imported.getDirectiveItemName());
			changed = true;
		}
		if (!safeEquals(imported.getDirectiveIndexName(), existing.getDirectiveIndexName())) {
			existing.setDirectiveIndexName(imported.getDirectiveIndexName());
			changed = true;
		}
		if (!safeEquals(imported.getDirectiveExpression(), existing.getDirectiveExpression())) {
			existing.setDirectiveExpression(imported.getDirectiveExpression());
			changed = true;
		}
		if (!hasEquivalentDirectiveSource(existing, imported)) {
			existing.setSourceSmartType(imported.getSourceSmartType().clone());
			changed = true;
		}
		ReconcileResult children = patchAnchoredChildren(existing, getMeaningfulDomChildren(element), getReconcilableChildren(existing), context, path);
		if (!children.matched) {
			return children;
		}
		return ReconcileResult.match(changed || children.changed);
	}

	private static ReconcileResult patchAnchoredUseShared(Element element, UIUseShared existing, ImportContext context, String path) throws Exception {
		Long priority = extractPriorityFromClasses(element);
		if (!matchesAnchoredExisting(element, existing, priority)) {
			return noMatch(context, path + ": shared anchor mismatch DOM priority=" + describePriority(priority)
				+ ", existing priority=" + existing.priority);
		}
		boolean changed = patchAnchoredDirectiveAttributes(element, existing);
		changed |= patchAnchoredUseSharedAttributes(element, existing, context);
		ReconcileResult children = patchAnchoredChildren(existing, getMeaningfulDomChildren(element), getReconcilableChildren(existing), context, path);
		if (!children.matched) {
			return children;
		}
		return ReconcileResult.match(changed || children.changed);
	}

	private static ReconcileResult patchAnchoredElement(Element element, UIElement existing, ImportContext context, String path) throws Exception {
		Long priority = extractPriorityFromClasses(element);
		if (!matchesAnchoredExisting(element, existing, priority)) {
			return noMatch(context, path + ": element anchor mismatch DOM priority=" + describePriority(priority)
				+ ", existing priority=" + existing.priority);
		}
		if (!sameStructuralTag(element.tagName(), existing.getTagName())) {
			return noMatch(context, path + ": tag mismatch DOM=<" + element.tagName()
				+ ">, existing=<" + existing.getTagName() + ">");
		}
		boolean changed = patchAnchoredDirectiveAttributes(element, existing);
		changed |= patchAnchoredAttributes(element, existing, context);
		changed |= ensureCustomClassAttribute(existing, element);
		if (!safeEquals(element.tagName(), existing.getTagName()) && canReplaceStructuralSubtree(existing, element)) {
			return ReconcileResult.match(changed || replaceStructuralSubtree(existing, element, context));
		}
		ReconcileResult children = patchAnchoredChildren(existing, getMeaningfulDomChildren(element), getReconcilableChildren(existing), context, path);
		if (!children.matched) {
			if (canReplaceStructuralSubtree(existing, element)) {
				return ReconcileResult.match(changed || replaceStructuralSubtree(existing, element, context));
			}
			return children;
		}
		return ReconcileResult.match(changed || children.changed);
	}

	private static ReconcileResult noMatch(ImportContext context, String detail) {
		recordMismatch(context, detail);
		return ReconcileResult.noMatch();
	}

	private static void recordMismatch(ImportContext context, String detail) {
		if (context == null || detail == null || detail.isBlank() || context.firstMismatch != null || !context.recordMismatches) {
			return;
		}
		context.firstMismatch = detail;
		String label = context.targetLabel == null || context.targetLabel.isBlank() ? "<unknown target>" : context.targetLabel;
		Engine.logEngine.warn("(NgxIonicRoundTripConverter) HTML round-trip mismatch for " + label + ": " + detail);
	}

	private static ImportContext silentMismatchContext(ImportContext context) {
		if (context == null || !context.recordMismatches) {
			return context;
		}
		ImportContext silent = new ImportContext(context.report, context.paletteContext, context.existingEventsByPriority, context.targetLabel, false, context.reusePool);
		silent.declaredTemplateIds.addAll(context.declaredTemplateIds);
		return silent;
	}

	private static ImportContext withReusePool(ImportContext context, ReusePool reusePool) {
		if (context == null) {
			return null;
		}
		ImportContext imported = new ImportContext(context.report, context.paletteContext, context.existingEventsByPriority, context.targetLabel, context.recordMismatches, reusePool);
		imported.declaredTemplateIds.addAll(context.declaredTemplateIds);
		return imported;
	}

	private static String childPath(String parentPath, UIComponent component, int index) {
		return parentPath + "/" + describeComponent(component) + "[" + (index + 1) + "]";
	}

	private static String childPath(String parentPath, Node node, int index) {
		return parentPath + "/" + describeNode(node) + "[" + (index + 1) + "]";
	}

	private static String describeComponent(UIComponent component) {
		if (component == null) {
			return "<null-component>";
		}
		if (component instanceof UIText) {
			return "#text";
		}
		if (component instanceof UIControlDirective directive) {
			return "*" + directive.getDirectiveName();
		}
		if (component instanceof UIUseShared shared) {
			String qname = shared.getSharedComponentQName();
			return "use:" + (qname == null || qname.isBlank() ? "<unset>" : qname);
		}
		if (component instanceof UIElement element) {
			return element.getTagName();
		}
		return component.getClass().getSimpleName();
	}

	private static String describeNode(Node node) {
		if (node == null) {
			return "<null-node>";
		}
		if (node instanceof TextNode textNode) {
			String text = textNode.getWholeText();
			text = text == null ? "" : text.replaceAll("\\s+", " ").trim();
			if (text.length() > 24) {
				text = text.substring(0, 24) + "...";
			}
			return text.isEmpty() ? "#text" : "#text(\"" + text + "\")";
		}
		if (node instanceof Element element) {
			Long priority = extractPriorityFromClasses(element);
			return priority == null ? "<" + element.tagName() + ">" : "<" + element.tagName() + " class" + priority + ">";
		}
		return node.nodeName();
	}

	private static String describePriority(Long priority) {
		return priority == null ? "<missing>" : String.valueOf(priority.longValue());
	}

	private static boolean hasEquivalentRenderedText(UIText imported, UIText existing) {
		if (imported == null || existing == null) {
			return false;
		}
		String importedMeaning = canonicalTextMeaning(imported);
		String existingMeaning = canonicalTextMeaning(existing);
		if (!importedMeaning.isEmpty() && importedMeaning.equals(existingMeaning)) {
			return true;
		}
		return normalizeAngularTextTemplate(imported.computeTemplate()).equals(normalizeAngularTextTemplate(existing.computeTemplate()));
	}

	private static String canonicalTextMeaning(UIText text) {
		if (text == null || text.getTextSmartType() == null) {
			return "";
		}
		MobileSmartSourceType smartType = text.getTextSmartType();
		String smartValue = smartType.getSmartValue();
		switch (smartType.getMode()) {
		case PLAIN:
			if (text.isI18n()) {
				return "i18n:" + computeTranslationKey(smartValue);
			}
			return "plain:" + normalizeTextLiteral(smartValue);
		case SCRIPT:
			String expression = normalizeAngularExpression(stripOuterParentheses(smartValue));
			Matcher translateMatcher = TRANSLATE_EXPRESSION_PATTERN.matcher(expression);
			if (translateMatcher.matches()) {
				String key = computeTranslationKey(translateMatcher.group(2));
				String params = normalizeAngularExpression(translateMatcher.group(3));
				return params.isEmpty() ? "i18n:" + key : "i18n:" + key + ":" + params;
			}
			return "script:" + expression;
		case SOURCE:
		default:
			return smartType.getMode().name().toLowerCase() + ":" + normalizeAngularExpression(smartValue);
		}
	}

	private static String normalizeAngularTextTemplate(String template) {
		String normalized = normalizeTemplateContent(template);
		if (normalized.isEmpty()) {
			return normalized;
		}
		normalized = normalized.replaceAll("\\{\\{\\s*", "{{");
		normalized = normalized.replaceAll("\\s*\\}\\}", "}}");
		normalized = normalized.replaceAll("\\s*\\|\\s*translate\\s*", "| translate");
		normalized = normalized.replaceAll("\\s+", " ").trim();
		return normalized;
	}

	private static String normalizeAngularExpression(String expression) {
		if (expression == null || expression.isBlank()) {
			return "";
		}
		return normalizeTextContent(expression).replaceAll("\\s+", " ").trim();
	}

	private static String normalizeTextLiteral(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		return normalizeTextContent(value).replaceAll("\\s+", " ").trim();
	}

	private static String computeTranslationKey(String text) {
		if (text == null || text.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (isTranslationKeyCharAllowed(c)) {
				sb.append(c);
			} else {
				sb.append('_');
			}
		}
		String key = sb.toString();
		return key.length() < 40 ? key : key.substring(0, 40);
	}

	private static boolean isTranslationKeyCharAllowed(char c) {
		if (c == '_' || c == '-') {
			return true;
		}
		if (c >= '0' && c <= '9') {
			return true;
		}
		if (c >= 'A' && c <= 'Z') {
			return true;
		}
		return c >= 'a' && c <= 'z';
	}

	private static boolean hasEquivalentRenderedAttribute(UIAttribute existing, MobileSmartSourceType imported) {
		if (existing == null || imported == null) {
			return false;
		}
		if (imported.equals(existing.getAttrSmartType())) {
			return true;
		}
		UIAttribute probe = new UIAttribute();
		probe.setEnabled(true);
		probe.setAttrName(existing.getAttrName());
		probe.setAttrSmartType(imported);
		return normalizeTemplateContent(probe.computeTemplate()).equals(normalizeTemplateContent(existing.computeTemplate()));
	}

	private static boolean isAmbiguousAnchoredAttribute(UIComponent component, UIAttribute attribute) {
		if (component == null || attribute == null) {
			return true;
		}
		int enabledCount = 0;
		for (UIComponent child : component.getUIComponentList()) {
			if (!(child instanceof UIAttribute sibling) || !child.isEnabled()) {
				continue;
			}
			if (!safeEquals(attribute.getAttrName(), sibling.getAttrName())) {
				continue;
			}
			enabledCount++;
			if (enabledCount > 1) {
				return true;
			}
		}
		return false;
	}

	private static Element parseRenderedElement(UIComponent existing) {
		if (existing == null) {
			return null;
		}
		try {
			String template = existing.computeTemplate();
			if (template == null || template.isBlank()) {
				return null;
			}
			String xhtml = preprocessVoidTags(template);
			Document document = Jsoup.parse("<" + PARSE_ROOT_TAG + ">" + xhtml + "</" + PARSE_ROOT_TAG + ">", "", Parser.xmlParser());
			Element parseRoot = document.selectFirst(PARSE_ROOT_TAG);
			if (parseRoot == null) {
				return null;
			}
			for (Node node : getMeaningfulDomChildren(parseRoot)) {
				if (node instanceof Element rendered) {
					return rendered;
				}
			}
		} catch (Exception e) {
		}
		return null;
	}

	private static boolean matchesAnchoredExisting(Element element, UIComponent existing, Long priority) {
		if (element == null || existing == null) {
			return false;
		}
		if (priority != null && priority.longValue() == existing.priority) {
			return true;
		}
		Element rendered = parseRenderedElement(existing);
		return rendered != null && sameStructuralTag(element.tagName(), rendered.tagName());
	}

	private static boolean sameStructuralTag(String importedTag, String existingTag) {
		if (safeEquals(importedTag, existingTag)) {
			return true;
		}
		if (isStructuralContainerTag(importedTag) && isStructuralContainerTag(existingTag)) {
			return true;
		}
		return isTransparentWrapperTag(importedTag) && isTransparentWrapperTag(existingTag);
	}

	private static boolean isStructuralContainerTag(String tagName) {
		return "div".equals(tagName)
			|| "section".equals(tagName)
			|| "article".equals(tagName)
			|| "ion-grid".equals(tagName)
			|| "ion-row".equals(tagName)
			|| "ion-col".equals(tagName);
	}

	private static boolean isTransparentWrapperTag(String tagName) {
		return ROOT_TAG_NAME.equals(tagName) || "tag".equals(tagName);
	}

	private static Set<String> collectDeclaredTemplateIds(Element root) {
		Set<String> ids = new HashSet<>();
		if (root == null) {
			return ids;
		}
		for (Element template : root.select("ng-template")) {
			String identifier = extractTemplateIdentifier(template);
			if (identifier != null && !identifier.isBlank()) {
				ids.add(identifier);
			}
		}
		return ids;
	}

	private static boolean isReferenceOnlyTemplateNode(Node node) {
		return node instanceof Element element && isReferenceOnlyTemplateElement(element);
	}

	private static boolean isReferenceOnlyTemplateElement(Element element) {
		return element != null
			&& "ng-template".equals(element.tagName())
			&& extractTemplateIdentifier(element) != null
			&& getMeaningfulDomChildren(element).isEmpty();
	}

	private static String extractTemplateIdentifier(Element element) {
		if (element == null || !"ng-template".equals(element.tagName())) {
			return null;
		}
		for (Attribute attribute : element.attributes()) {
			String key = attribute.getKey();
			if (key != null && key.startsWith("#") && attribute.getValue().isBlank()) {
				return key.substring(1);
			}
		}
		return null;
	}

	private static boolean isReferenceOnlyTemplateComponent(UIComponent component) {
		return component instanceof UIElement element
			&& "ng-template".equals(element.getTagName())
			&& element.getIdentifier() != null
			&& !element.getIdentifier().isBlank()
			&& getReconcilableChildren(element).isEmpty();
	}

	private static boolean canRetainNamedTemplate(UIComponent component, ImportContext context) {
		if (!(component instanceof UIElement element) || context == null) {
			return false;
		}
		String identifier = element.getIdentifier();
		return "ng-template".equals(element.getTagName())
			&& identifier != null
			&& !identifier.isBlank()
			&& context.declaredTemplateIds.contains(identifier);
	}

	private static ReusePool collectReusableComponents(UIElement root) {
		ReusePool pool = new ReusePool();
		collectReusableComponents(root, pool);
		return pool;
	}

	private static ReusePool collectReusableComponents(List<UIComponent> components) {
		ReusePool pool = new ReusePool();
		if (components == null) {
			return pool;
		}
		for (UIComponent component : components) {
			collectReusableComponents(component, pool);
		}
		return pool;
	}

	private static ReusePool mergeReusePools(ReusePool left, ReusePool right) {
		if (left == null) {
			return right;
		}
		if (right == null) {
			return left;
		}
		ReusePool merged = new ReusePool();
		merged.useShared.addAll(left.useShared);
		merged.useShared.addAll(right.useShared);
		merged.directives.addAll(left.directives);
		merged.directives.addAll(right.directives);
		merged.namedTemplates.addAll(left.namedTemplates);
		merged.namedTemplates.addAll(right.namedTemplates);
		return merged;
	}

	private static List<UIComponent> collectAbsorbableSiblings(DatabaseObject parent, UIComponent existing, Set<Long> remainingAnchors) {
		List<UIComponent> absorbed = new ArrayList<>();
		if (!(parent instanceof UIComponent uiParent) || existing == null) {
			return absorbed;
		}
		boolean foundExisting = false;
		for (UIComponent candidate : getStructuralChildren(uiParent)) {
			if (!foundExisting) {
				foundExisting = candidate == existing;
				continue;
			}
			if (remainingAnchors != null && remainingAnchors.contains(candidate.priority)) {
				break;
			}
			absorbed.add(candidate);
		}
		return absorbed;
	}

	private static int countReconcilableComponents(List<UIComponent> components) {
		int count = 0;
		if (components == null) {
			return count;
		}
		for (UIComponent component : components) {
			if (component == null || !component.isEnabled() || isNonStructuralChild(component)) {
				continue;
			}
			count++;
		}
		return count;
	}

	private static void collectReusableComponents(UIComponent component, ReusePool pool) {
		if (component == null || pool == null) {
			return;
		}
		if (component instanceof UIUseShared shared) {
			pool.useShared.add(shared);
		} else if (component instanceof UIControlDirective directive) {
			pool.directives.add(directive);
		} else if (component instanceof UIElement element && "ng-template".equals(element.getTagName())
			&& element.getIdentifier() != null && !element.getIdentifier().isBlank()) {
			pool.namedTemplates.add(element);
		}
		for (UIComponent child : getStructuralChildren(component)) {
			collectReusableComponents(child, pool);
		}
	}

	private static UIUseShared reuseUseSharedIfPossible(UIUseShared candidate, ImportContext context) throws Exception {
		if (candidate == null || context == null || context.reusePool == null) {
			return candidate;
		}
		for (int i = 0; i < context.reusePool.useShared.size(); i++) {
			UIUseShared reusable = context.reusePool.useShared.get(i);
			if (!safeEquals(candidate.getSharedComponentQName(), reusable.getSharedComponentQName())) {
				continue;
			}
			context.reusePool.useShared.remove(i);
			reusable.setSharedComponentQName(candidate.getSharedComponentQName());
			reusable.setEnabled(true);
			return reusable;
		}
		return candidate;
	}

	private static UIControlDirective reuseDirectiveIfPossible(UIControlDirective candidate, ImportContext context) throws Exception {
		if (candidate == null || context == null || context.reusePool == null) {
			return candidate;
		}
		for (int i = 0; i < context.reusePool.directives.size(); i++) {
			UIControlDirective reusable = context.reusePool.directives.get(i);
			if (!sameDirectiveDefinition(candidate, reusable)) {
				continue;
			}
			context.reusePool.directives.remove(i);
			resetReusedComponent(reusable);
			reusable.setDirectiveName(candidate.getDirectiveName());
			reusable.setDirectiveItemName(candidate.getDirectiveItemName());
			reusable.setDirectiveIndexName(candidate.getDirectiveIndexName());
			reusable.setDirectiveExpression(candidate.getDirectiveExpression());
			reusable.setSourceSmartType(candidate.getSourceSmartType().clone());
			reusable.setEnabled(true);
			return reusable;
		}
		return candidate;
	}

	private static UIComponent reuseNamedTemplateIfPossible(UIElement candidate, Element element, ImportContext context) throws Exception {
		String identifier = extractTemplateIdentifier(element);
		if (candidate == null || context == null || context.reusePool == null || identifier == null || !"ng-template".equals(candidate.getTagName())) {
			return candidate;
		}
		for (int i = 0; i < context.reusePool.namedTemplates.size(); i++) {
			UIElement reusable = context.reusePool.namedTemplates.get(i);
			if (!safeEquals(identifier, reusable.getIdentifier())) {
				continue;
			}
			context.reusePool.namedTemplates.remove(i);
			resetReusedComponent(reusable);
			reusable.setTagName(candidate.getTagName());
			reusable.setSelfClose(candidate.isSelfClose());
			reusable.setIdentifier(identifier);
			reusable.setEnabled(true);
			return reusable;
		}
		return candidate;
	}

	private static boolean sameDirectiveDefinition(UIControlDirective left, UIControlDirective right) {
		return left != null
			&& right != null
			&& safeEquals(left.getDirectiveName(), right.getDirectiveName())
			&& safeEquals(left.getDirectiveItemName(), right.getDirectiveItemName())
			&& safeEquals(left.getDirectiveIndexName(), right.getDirectiveIndexName())
			&& safeEquals(left.getDirectiveExpression(), right.getDirectiveExpression())
			&& sameDirectiveSourceMeaning(left.getSourceSmartType(), right.getSourceSmartType());
	}

	private static boolean hasEquivalentDirectiveSource(UIControlDirective existing, UIControlDirective imported) {
		return existing != null
			&& imported != null
			&& sameDirectiveSourceMeaning(existing.getSourceSmartType(), imported.getSourceSmartType());
	}

	private static boolean sameDirectiveSourceMeaning(MobileSmartSourceType left, MobileSmartSourceType right) {
		if (left == null || right == null) {
			return left == right;
		}
		if (left.equals(right)) {
			return true;
		}
		MobileSmartSourceType.Mode leftMode = left.getMode();
		MobileSmartSourceType.Mode rightMode = right.getMode();
		boolean compatible = (leftMode == MobileSmartSourceType.Mode.PLAIN || leftMode == MobileSmartSourceType.Mode.SCRIPT)
			&& (rightMode == MobileSmartSourceType.Mode.PLAIN || rightMode == MobileSmartSourceType.Mode.SCRIPT);
		if (!compatible) {
			return false;
		}
		return normalizeAngularExpression(stripOuterParentheses(left.getSmartValue()))
			.equals(normalizeAngularExpression(stripOuterParentheses(right.getSmartValue())));
	}

	private static void resetReusedComponent(UIComponent component) throws Exception {
		if (component == null) {
			return;
		}
		for (UIComponent child : new ArrayList<>(component.getUIComponentList())) {
			removeChild(component, child);
		}
	}

	private static ReconcileResult tryReconcileDirectiveWrappedElement(UIControlDirective importedDirective, UIElement existingElement, ImportContext context, String path) throws Exception {
		if (!canAbsorbDirectiveWrapper(importedDirective, existingElement)) {
			return null;
		}
		boolean changed = ensureDirectiveAttribute(existingElement, importedDirective);
		UIComponent wrapped = getReconcilableChildren(importedDirective).get(0);
		ReconcileResult wrappedResult = reconcileComponent(wrapped, existingElement, context, path);
		if (!wrappedResult.matched) {
			return wrappedResult;
		}
		return ReconcileResult.match(changed || wrappedResult.changed);
	}

	private static boolean canAbsorbDirectiveWrapper(UIControlDirective importedDirective, UIElement existingElement) {
		if (importedDirective == null || existingElement == null) {
			return false;
		}
		String attrName = getDirectiveAttributeName(importedDirective);
		if (attrName == null) {
			return false;
		}
		List<UIComponent> wrappedChildren = getReconcilableChildren(importedDirective);
		if (wrappedChildren.size() != 1 || !(wrappedChildren.get(0) instanceof UIElement wrappedElement)) {
			return false;
		}
		return sameStructuralTag(wrappedElement.getTagName(), existingElement.getTagName());
	}

	private static boolean ensureDirectiveAttribute(UIComponent existingElement, UIControlDirective directive) throws Exception {
		String attrName = getDirectiveAttributeName(directive);
		String attrValue = getDirectiveAttributeValue(directive);
		if (attrName == null || attrValue == null) {
			return false;
		}
		MobileSmartSourceType imported = buildAttributeSmartType(attrName, attrValue);
		for (UIComponent child : existingElement.getUIComponentList()) {
			if (child instanceof UIAttribute attribute && child.isEnabled() && safeEquals(attrName, attribute.getAttrName())) {
				if (!hasEquivalentRenderedAttribute(attribute, imported)) {
					attribute.setAttrSmartType(imported);
					return true;
				}
				return false;
			}
		}
		UIAttribute attribute = new UIAttribute();
		attribute.setAttrName(attrName);
		attribute.setAttrSmartType(imported);
		existingElement.add(attribute);
		return true;
	}

	private static String getDirectiveAttributeName(UIControlDirective directive) {
		if (directive == null) {
			return null;
		}
		return switch (directive.getDirectiveName()) {
		case "If" -> "*ngIf";
		case "ForEach" -> "*ngFor";
		case "CdkVirtualFor" -> "*cdkVirtualFor";
		case "Switch" -> "[ngSwitch]";
		case "SwitchCase" -> "*ngSwitchCase";
		case "SwitchDefault" -> "*ngSwitchDefault";
		default -> null;
		};
	}

	private static String getDirectiveAttributeValue(UIControlDirective directive) {
		if (directive == null) {
			return null;
		}
		String source = directive.getSourceSmartType() == null ? "" : directive.getSourceSmartType().getValue();
		String expression = directive.getDirectiveExpression() == null ? "" : directive.getDirectiveExpression();
		if ("ForEach".equals(directive.getDirectiveName()) || "CdkVirtualFor".equals(directive.getDirectiveName())) {
			StringBuilder sb = new StringBuilder();
			String itemName = directive.getDirectiveItemName();
			if (itemName != null && !itemName.isBlank()) {
				sb.append("let ").append(itemName).append(" of ").append(source);
			} else {
				sb.append(source);
			}
			String indexName = directive.getDirectiveIndexName();
			if (indexName != null && !indexName.isBlank()) {
				if (sb.length() > 0) {
					sb.append("; ");
				}
				sb.append("let ").append(indexName).append(" = index");
			}
			if (!expression.isBlank()) {
				if (sb.length() > 0) {
					sb.append("; ");
				}
				sb.append(expression.trim());
			}
			return sb.toString();
		}
		return source + expression;
	}

	private static boolean canReplaceStructuralSubtree(UIElement existing, Element element) {
		return existing != null
			&& element != null
			&& isStructuralContainerTag(existing.getTagName())
			&& isStructuralContainerTag(element.tagName());
	}

	private static boolean replaceStructuralSubtree(UIElement existing, Element element, ImportContext context) throws Exception {
		boolean changed = false;
		if (!safeEquals(existing.getTagName(), element.tagName())) {
			existing.setTagName(element.tagName());
			changed = true;
		}
		boolean selfClose = VOID_TAGS.contains(element.tagName());
		if (existing.isSelfClose() != selfClose) {
			existing.setSelfClose(selfClose);
			changed = true;
		}
			changed |= ensureCustomClassAttribute(existing, element);
			ReusePool reusePool = mergeReusePools(collectReusableComponents(existing), context == null ? null : context.reusePool);
			boolean hadStructuralChildren = !getStructuralChildren(existing).isEmpty();
		if (hadStructuralChildren) {
			removeStructuralChildren(existing);
			changed = true;
		}
		ImportContext importContext = withReusePool(context, reusePool);
		for (UIComponent child : importChildren(element, existing, importContext)) {
			existing.add(child);
			changed = true;
		}
		return changed;
	}

	private static boolean ensureCustomClassAttribute(UIComponent existing, Element element) throws Exception {
		String customClass = extractCustomClassValue(element);
		return ensureCustomClassAttribute(existing, customClass);
	}

	private static boolean ensureCustomClassAttribute(UIComponent existing, String customClass) throws Exception {
		for (UIComponent child : existing.getUIComponentList()) {
			if (child instanceof UIAttribute attribute && child.isEnabled() && "class".equals(attribute.getAttrName())) {
				if (customClass == null || customClass.isBlank()) {
					removeChild(existing, attribute);
					return true;
				}
				MobileSmartSourceType imported = newPlainSmartType(customClass);
				if (!hasEquivalentRenderedAttribute(attribute, imported)) {
					attribute.setAttrSmartType(imported);
					return true;
				}
				return false;
			}
		}
		if (customClass == null || customClass.isBlank()) {
			return false;
		}
		UIAttribute attribute = new UIAttribute();
		attribute.setAttrName("class");
		attribute.setAttrSmartType(newPlainSmartType(customClass));
		existing.add(attribute);
		return true;
	}

	private static String getRenderedAttributeValue(Element element, String attrName) {
		if (element == null || attrName == null) {
			return null;
		}
		if ("class".equals(attrName)) {
			return extractCustomClassValue(element);
		}
		return element.hasAttr(attrName) ? element.attr(attrName) : null;
	}

	private static boolean sameDomAttributeValue(String left, String right) {
		if (left == null || right == null) {
			return left == right;
		}
		return normalizeTemplateContent(left).equals(normalizeTemplateContent(right));
	}

	private static boolean hasPriorityAnchor(Node node) {
		return node instanceof Element element && extractPriorityFromClasses(element) != null;
	}

	private static Set<Long> collectAnchoredPriorities(List<Node> nodes, int fromIndex) {
		Set<Long> priorities = new HashSet<>();
		if (nodes == null) {
			return priorities;
		}
		for (int i = Math.max(0, fromIndex); i < nodes.size(); i++) {
			collectAnchoredPriorities(nodes.get(i), priorities);
		}
		return priorities;
	}

	private static void collectAnchoredPriorities(Node node, Set<Long> priorities) {
		if (node instanceof Element element) {
			Long priority = extractPriorityFromClasses(element);
			if (priority != null) {
				priorities.add(priority);
			}
			for (Node child : element.childNodes()) {
				collectAnchoredPriorities(child, priorities);
			}
		}
	}

	private static Set<Long> collectComponentPriorities(List<UIComponent> components, int fromIndex) {
		Set<Long> priorities = new HashSet<>();
		if (components == null) {
			return priorities;
		}
		for (int i = Math.max(0, fromIndex); i < components.size(); i++) {
			collectComponentPriorities(components.get(i), priorities);
		}
		return priorities;
	}

	private static void collectComponentPriorities(UIComponent component, Set<Long> priorities) {
		if (component == null) {
			return;
		}
		if (component.priority != 0L) {
			priorities.add(component.priority);
		}
		for (UIComponent child : component.getUIComponentList()) {
			collectComponentPriorities(child, priorities);
		}
	}

	private static boolean shouldRemoveMissingExistingChild(UIComponent existing, Set<Long> remainingPriorities, ImportContext context) {
		if (existing == null || existing instanceof UIText || canRetainNamedTemplate(existing, context)) {
			return false;
		}
		Set<Long> existingPriorities = new HashSet<>();
		collectComponentPriorities(existing, existingPriorities);
		if (existingPriorities.isEmpty()) {
			return false;
		}
		for (Long priority : existingPriorities) {
			if (remainingPriorities != null && remainingPriorities.contains(priority)) {
				return false;
			}
		}
		return true;
	}

	private static boolean shouldUnwrapMissingExistingChild(UIComponent existing, Set<Long> remainingPriorities, ImportContext context) {
		if (!(existing instanceof UIElement || existing instanceof UIControlDirective) || existing instanceof UIText || canRetainNamedTemplate(existing, context)) {
			return false;
		}
		if (remainingPriorities == null || remainingPriorities.isEmpty() || remainingPriorities.contains(existing.priority)) {
			return false;
		}
		Set<Long> descendantPriorities = collectDescendantComponentPriorities(existing);
		for (Long priority : descendantPriorities) {
			if (remainingPriorities.contains(priority)) {
				return true;
			}
		}
		return false;
	}

	private static Set<Long> collectDescendantComponentPriorities(UIComponent component) {
		Set<Long> priorities = new HashSet<>();
		if (component == null) {
			return priorities;
		}
		for (UIComponent child : getStructuralChildren(component)) {
			collectComponentPriorities(child, priorities);
		}
		return priorities;
	}

	private static void unwrapExistingChild(DatabaseObject parent, List<UIComponent> existingChildren, int existingIndex, Long after) throws Exception {
		if (parent == null || existingChildren == null || existingIndex < 0 || existingIndex >= existingChildren.size()) {
			return;
		}
		UIComponent wrapper = existingChildren.get(existingIndex);
		List<UIComponent> promoted = new ArrayList<>(getStructuralChildren(wrapper));
		for (UIComponent child : promoted) {
			removeChild(wrapper, child);
		}
		removeChild(parent, wrapper);
		existingChildren.remove(existingIndex);
		Long insertionAfter = after;
		for (int i = 0; i < promoted.size(); i++) {
			UIComponent child = promoted.get(i);
			addChild(parent, child, insertionAfter);
			existingChildren.add(existingIndex + i, child);
			insertionAfter = child.priority;
		}
	}

	private static void addChild(DatabaseObject parent, UIComponent child, Long after) throws Exception {
		if (parent instanceof IContainerOrdered ordered) {
			ordered.add(child, after);
			return;
		}
		parent.add(child);
	}

	private static void removeChild(DatabaseObject parent, UIComponent child) throws Exception {
		if (parent instanceof PageComponent page) {
			page.remove(child);
			return;
		}
		if (parent instanceof UIComponent component) {
			component.remove(child);
			return;
		}
		parent.remove(child);
	}

	private static boolean patchAnchoredAttributes(Element element, UIComponent existing, ImportContext context) throws Exception {
		boolean changed = false;
		Element currentElement = parseRenderedElement(existing);
		Set<String> seenEvents = new HashSet<>();
		Set<String> seenAttributes = new HashSet<>();
		for (Attribute attribute : element.attributes()) {
			String key = attribute.getKey();
			if ("class".equals(key) || key.startsWith("#") || isConsumedDirectiveAttribute(key)) {
				continue;
			}
			String value = attribute.getValue();
			UIControlEvent importedEvent = tryCreateControlEvent(key, attribute.getValue(), context);
			if (importedEvent == null) {
				seenAttributes.add(key);
				if (getEnabledAttribute(existing, key) != null || sameDomAttributeValue(getRenderedAttributeValue(currentElement, key), value)) {
					continue;
				}
				if (applyIonBeanAttribute(existing, key, value)) {
					changed = true;
					continue;
				}
				UIAttribute uiAttribute = new UIAttribute();
				uiAttribute.setAttrName(key);
				uiAttribute.setAttrSmartType(buildAttributeSmartType(key, value));
				addChild(existing, uiAttribute, null);
				changed = true;
				continue;
			}
			String attrName = firstNonBlank(importedEvent.getAttrName(), key);
			seenEvents.add(attrName);
			UIControlEvent existingEvent = findControlEvent(existing, attrName);
			if (existingEvent == null) {
				addChild(existing, importedEvent, null);
				changed = true;
			} else if (!existingEvent.isEnabled()) {
				existingEvent.setEnabled(true);
				changed = true;
			} else if (importedEvent.priority != 0 && existingEvent.priority != importedEvent.priority) {
				removeChild(existing, existingEvent);
				addChild(existing, importedEvent, null);
				changed = true;
			}
		}
		for (UIComponent child : new ArrayList<>(existing.getUIComponentList())) {
			if (child instanceof UIControlEvent event && event.getAttrName() != null
					&& getRenderedAttributeValue(currentElement, event.getAttrName()) != null
					&& !seenEvents.contains(event.getAttrName())) {
				removeChild(existing, event);
				changed = true;
			}
		}
		for (UIComponent child : new ArrayList<>(existing.getUIComponentList())) {
			if (!(child instanceof UIAttribute attribute) || child instanceof UIControlAttr || child instanceof UIDynamicAttr || !child.isEnabled()) {
				continue;
			}
			String attrName = attribute.getAttrName();
			if ("class".equals(attrName) || seenAttributes.contains(attrName) || isConsumedDirectiveAttribute(attrName)) {
				continue;
			}
			if (getRenderedAttributeValue(currentElement, attrName) == null || isAmbiguousAnchoredAttribute(existing, attribute)) {
				continue;
			}
			removeChild(existing, attribute);
			changed = true;
		}
		for (UIComponent child : existing.getUIComponentList()) {
			if (!(child instanceof UIAttribute attribute) || child instanceof UIControlAttr || child instanceof UIDynamicAttr || !child.isEnabled()) {
				continue;
			}
			String attrName = attribute.getAttrName();
			String domValue = getRenderedAttributeValue(element, attrName);
			if (domValue == null) {
				continue;
			}
			String currentDomValue = getRenderedAttributeValue(currentElement, attrName);
			if (sameDomAttributeValue(currentDomValue, domValue)) {
				continue;
			}
			if (isAmbiguousAnchoredAttribute(existing, attribute)) {
				continue;
			}
			MobileSmartSourceType imported = buildAttributeSmartType(attrName, domValue);
			if (!hasEquivalentRenderedAttribute(attribute, imported)) {
				attribute.setAttrSmartType(imported);
				changed = true;
			}
		}
		return changed;
	}

	private static UIControlEvent findControlEvent(UIComponent component, String attrName) {
		for (UIComponent child : component.getUIComponentList()) {
			if (child instanceof UIControlEvent controlEvent && safeEquals(attrName, controlEvent.getAttrName())) {
				return controlEvent;
			}
		}
		return null;
	}

	private static UIAttribute getEnabledAttribute(UIComponent component, String attrName) {
		for (UIComponent child : component.getUIComponentList()) {
			if (child instanceof UIAttribute attribute && child.isEnabled() && safeEquals(attrName, attribute.getAttrName())) {
				return attribute;
			}
		}
		return null;
	}

	private static boolean patchAnchoredDirectiveAttributes(Element element, UIComponent existing) throws Exception {
		boolean changed = false;
		for (DirectiveSpec directive : extractDirectives(element.attributes())) {
			if (ensureDirectiveAttribute(existing, buildDirective(directive))) {
				changed = true;
			}
		}
		return changed;
	}

	private static boolean patchAnchoredUseSharedAttributes(Element element, UIUseShared existing, ImportContext context) throws Exception {
		boolean changed = ensureCustomClassAttribute(existing, element);
		Element currentElement = parseRenderedElement(existing);
		Set<String> seenVariables = new HashSet<>();
		Set<String> seenEvents = new HashSet<>();
		Set<String> seenAttributes = new HashSet<>();
		for (Attribute attribute : element.attributes()) {
			String key = attribute.getKey();
			String value = attribute.getValue();
			if ("class".equals(key) || key.startsWith("#")) {
				continue;
			}
			if ("[owner]".equals(key) || "owner".equals(key) || isConsumedDirectiveAttribute(key)) {
				continue;
			}

			UIControlEvent importedEvent = tryCreateControlEvent(key, value, context);
			if (importedEvent != null) {
				seenEvents.add(importedEvent.getAttrName());
				String currentDomValue = getRenderedAttributeValue(currentElement, key);
				if (sameDomAttributeValue(currentDomValue, value)) {
					continue;
				}
				UIControlEvent existingEvent = null;
				for (UIComponent child : existing.getUIComponentList()) {
					if (child instanceof UIControlEvent controlEvent && safeEquals(key, controlEvent.getAttrName())) {
						existingEvent = controlEvent;
						break;
					}
				}
				if (existingEvent != null) {
					removeChild(existing, existingEvent);
				}
				addChild(existing, importedEvent, null);
				changed = true;
				continue;
			}

			String variableName = extractUseVariableName(key);
			if (variableName != null) {
				seenVariables.add(variableName);
				String currentDomValue = getRenderedAttributeValue(currentElement, key);
				if (sameDomAttributeValue(currentDomValue, value)) {
					continue;
				}
				UIUseVariable variable = existing.getVariable(variableName);
				MobileSmartSourceType imported = buildAttributeSmartType(key, value);
				UIUseVariable.BindingType binding = key.startsWith("[(") && key.endsWith(")]")
					? UIUseVariable.BindingType.twoWayBinding
					: UIUseVariable.BindingType.oneWayBinding;
				if (variable == null) {
					variable = new UIUseVariable();
					variable.setName(variableName);
					addChild(existing, variable, null);
					changed = true;
				}
				if (!imported.equals(variable.getVarSmartType())) {
					variable.setVarSmartType(imported);
					changed = true;
				}
				if (variable.getBinding() != binding) {
					variable.setBinding(binding);
					changed = true;
				}
				continue;
			}

			seenAttributes.add(key);
			String currentDomValue = getRenderedAttributeValue(currentElement, key);
			if (sameDomAttributeValue(currentDomValue, value)) {
				continue;
			}
			UIAttribute uiAttribute = null;
			for (UIComponent child : existing.getUIComponentList()) {
				if (child instanceof UIAttribute existingAttribute && child.isEnabled() && safeEquals(key, existingAttribute.getAttrName())) {
					uiAttribute = existingAttribute;
					break;
				}
			}
			MobileSmartSourceType imported = buildAttributeSmartType(key, value);
			if (uiAttribute == null) {
				uiAttribute = new UIAttribute();
				uiAttribute.setAttrName(key);
				uiAttribute.setAttrSmartType(imported);
				addChild(existing, uiAttribute, null);
				changed = true;
				continue;
			}
			if (!hasEquivalentRenderedAttribute(uiAttribute, imported)) {
				uiAttribute.setAttrSmartType(imported);
				changed = true;
			}
		}
		for (UIComponent child : new ArrayList<>(existing.getUIComponentList())) {
			if (child instanceof UIUseVariable variable && variable.isEnabled()) {
				String attrName = useVariableAttributeName(variable);
				if (!seenVariables.contains(variable.getName()) && getRenderedAttributeValue(currentElement, attrName) != null) {
					removeChild(existing, variable);
					changed = true;
				}
				continue;
			}
			if (child instanceof UIControlEvent event && event.getAttrName() != null
					&& !seenEvents.contains(event.getAttrName())
					&& getRenderedAttributeValue(currentElement, event.getAttrName()) != null) {
				removeChild(existing, event);
				changed = true;
				continue;
			}
			if (child instanceof UIAttribute attribute && !(child instanceof UIControlAttr) && !(child instanceof UIDynamicAttr)
					&& child.isEnabled() && !"class".equals(attribute.getAttrName())
					&& !seenAttributes.contains(attribute.getAttrName())
					&& getRenderedAttributeValue(currentElement, attribute.getAttrName()) != null
					&& !isAmbiguousAnchoredAttribute(existing, attribute)) {
				removeChild(existing, attribute);
				changed = true;
			}
		}
		return changed;
	}

	private static String useVariableAttributeName(UIUseVariable variable) {
		String name = variable == null ? "" : variable.getVarName();
		if (UIUseVariable.BindingType.twoWayBinding.equals(variable.getBinding())) {
			return "[(" + name + ")]";
		}
		return "[" + name + "]";
	}

	private static Long extractPriorityFromClasses(Element element) {
		if (element == null || !element.hasAttr("class")) {
			return null;
		}
		Matcher matcher = PRIORITY_CLASS_PATTERN.matcher(element.attr("class"));
		if (matcher.find()) {
			try {
				return Long.valueOf(matcher.group(1));
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	private static String extractCustomClassValue(Element element) {
		if (element == null || !element.hasAttr("class")) {
			return "";
		}
		List<String> custom = new ArrayList<>();
		for (String token : element.classNames()) {
			if (!token.matches("class\\d+")) {
				custom.add(token);
			}
		}
		return String.join(" ", custom);
	}

	private static String extractCustomClassValue(Attributes attributes) {
		if (attributes == null || !attributes.hasKey("class")) {
			return "";
		}
		List<String> custom = new ArrayList<>();
		for (String token : attributes.get("class").split("\\s+")) {
			if (!token.isBlank() && !token.matches("class\\d+")) {
				custom.add(token);
			}
		}
		return String.join(" ", custom);
	}

	private static boolean isTransparentNgContainer(Element element) {
		if (element == null || !ROOT_TAG_NAME.equals(element.tagName())) {
			return false;
		}
		for (Attribute attribute : element.attributes()) {
			String key = attribute.getKey();
			if (isConsumedDirectiveAttribute(key)) {
				continue;
			}
			if ("class".equals(key) && extractCustomClassValue(element).isEmpty()) {
				continue;
			}
			return false;
		}
		return true;
	}

	private static boolean safeEquals(String left, String right) {
		return left == null ? right == null : left.equals(right);
	}

	private static UIUseShared tryCreateUseShared(String tag, DatabaseObject parent) throws Exception {
		if (!(parent instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent mobileParent)) {
			return null;
		}
		var app = mobileParent.getApplication();
		if (app == null) {
			return null;
		}
		for (UISharedComponent component : app.getSharedComponentList()) {
			if (component == null || !component.isEnabled()) {
				continue;
			}
			if (!tag.equals(component.getSelector())) {
				continue;
			}
			UIUseShared use = new UIUseShared();
			use.setSharedComponentQName(component.getQName());
			return use;
		}
		return null;
	}

	private static UIComponent tryCreatePaletteComponent(String tag, DatabaseObject parent, ImportContext context) throws Exception {
		if (!(parent instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent mobileParent)) {
			return null;
		}
		DatabaseObject paletteContext = context != null && context.paletteContext != null ? context.paletteContext : mobileParent;
		ComponentManager manager = ComponentManager.of(paletteContext);
		for (Component component : manager.getComponents()) {
			if (component == null || !tag.equals(component.getTag())) {
				continue;
			}
			DatabaseObject databaseObject = manager.createBean(component);
			if (databaseObject instanceof UIComponent uiComponent) {
				return uiComponent;
			}
		}
		return null;
	}

	private static void applyAttributes(UIComponent component, Attributes attributes, ImportContext context) throws Exception {
		if (component instanceof UIUseShared useShared) {
			applyUseSharedAttributes(useShared, attributes, context);
			return;
		}
		if (!(component instanceof UIElement uiElement)) {
			return;
		}
		for (Attribute attribute : attributes) {
			String key = attribute.getKey();
			if (isConsumedDirectiveAttribute(key)) {
				continue;
			}
			String value = attribute.getValue();
			if (key.startsWith("#") && value.isBlank()) {
				uiElement.setIdentifier(key.substring(1));
				continue;
			}
			if (applyIonBeanAttribute(component, key, value)) {
				continue;
			}
			UIControlEvent controlEvent = tryCreateControlEvent(key, value, context);
			if (controlEvent != null) {
				component.add(controlEvent);
				continue;
			}
			UIAttribute uiAttribute = new UIAttribute();
			uiAttribute.setAttrName(key);
			uiAttribute.setAttrSmartType(buildAttributeSmartType(key, value));
			component.add(uiAttribute);
		}
	}

	private static void applyUseSharedAttributes(UIUseShared component, Attributes attributes, ImportContext context) throws Exception {
		Set<String> seenVariables = new HashSet<>();
		Set<String> seenAttributes = new HashSet<>();
		Set<String> seenEvents = new HashSet<>();
		ensureCustomClassAttribute(component, extractCustomClassValue(attributes));
		for (Attribute attribute : attributes) {
			String key = attribute.getKey();
			String value = attribute.getValue();
			if ("class".equals(key) || key.startsWith("#")) {
				continue;
			}
			if ("[owner]".equals(key) || "owner".equals(key)) {
				continue;
			}
			UIControlEvent controlEvent = tryCreateControlEvent(key, value, context);
			if (controlEvent != null) {
				if (controlEvent.getAttrName() != null) {
					seenEvents.add(controlEvent.getAttrName());
				}
				if (controlEvent.getParent() != component) {
					component.add(controlEvent);
				}
				continue;
			}
			String variableName = extractUseVariableName(key);
			if (variableName != null) {
				seenVariables.add(variableName);
				UIUseVariable variable = component.getVariable(variableName);
				if (variable == null) {
					variable = new UIUseVariable();
					variable.setName(variableName);
					component.add(variable);
				}
				variable.setVarSmartType(buildAttributeSmartType(key, value));
				if (key.startsWith("[(") && key.endsWith(")]")) {
					variable.setBinding(UIUseVariable.BindingType.twoWayBinding);
				} else {
					variable.setBinding(UIUseVariable.BindingType.oneWayBinding);
				}
				continue;
			}
			seenAttributes.add(key);
			UIAttribute uiAttribute = null;
			for (UIComponent child : component.getUIComponentList()) {
				if (child instanceof UIAttribute existingAttribute && child.isEnabled() && safeEquals(key, existingAttribute.getAttrName())) {
					uiAttribute = existingAttribute;
					break;
				}
			}
			if (uiAttribute == null) {
				uiAttribute = new UIAttribute();
				uiAttribute.setAttrName(key);
				component.add(uiAttribute);
			}
			uiAttribute.setAttrSmartType(buildAttributeSmartType(key, value));
		}
		for (UIComponent child : new ArrayList<>(component.getUIComponentList())) {
			if (child instanceof UIUseVariable variable && !seenVariables.contains(variable.getName())) {
				component.remove(variable);
				continue;
			}
			if (child instanceof UIControlEvent event && event.getAttrName() != null && !seenEvents.contains(event.getAttrName())) {
				component.remove(event);
				continue;
			}
			if (child instanceof UIAttribute uiAttribute && !"class".equals(uiAttribute.getAttrName()) && !seenAttributes.contains(uiAttribute.getAttrName())) {
				component.remove(uiAttribute);
			}
		}
	}

	private static String extractUseVariableName(String key) {
		if (key == null || key.isBlank()) {
			return null;
		}
		if (key.startsWith("[(") && key.endsWith(")]")) {
			return key.substring(2, key.length() - 2);
		}
		if (key.startsWith("[") && key.endsWith("]")) {
			return key.substring(1, key.length() - 1);
		}
		return null;
	}

	private static UIControlEvent tryCreateControlEvent(String key, String value, ImportContext context) throws Exception {
		if (!key.startsWith("(") || !key.endsWith(")")) {
			return null;
		}
		Long priority = extractEventFunctionPriority(value);
		if (priority != null && context != null && context.existingEventsByPriority != null) {
			UIControlEvent existing = context.existingEventsByPriority.get(priority);
			if (existing != null) {
				UIControlEvent cloned = existing.clone();
				cloned.setEnabled(true);
				return cloned;
			}
		}
		String eventName = toControlEventName(key);
		if (eventName == null || eventName.isBlank()) {
			return null;
		}
		UIControlEvent controlEvent = new UIControlEvent();
		controlEvent.setEventName(eventName);
		controlEvent.setEnabled(true);
		return controlEvent;
	}

	private static Long extractEventFunctionPriority(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		Matcher matcher = EVENT_FUNCTION_PATTERN.matcher(value);
		if (!matcher.find()) {
			return null;
		}
		try {
			return Long.valueOf(matcher.group(1));
		} catch (Exception e) {
			return null;
		}
	}

	private static String toControlEventName(String attrName) {
		for (UIControlEvent.AttrEvent attrEvent : UIControlEvent.AttrEvent.values()) {
			if (safeEquals(UIControlEvent.AttrEvent.getEvent(attrEvent.name()), attrName)) {
				return attrEvent.name();
			}
		}
		return null;
	}

	private static boolean applyIonBeanAttribute(UIComponent component, String key, String value) {
		if (!(component instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement dynamicElement)) {
			return false;
		}
		IonBean ionBean = dynamicElement.getIonBean();
		if (ionBean == null) {
			return false;
		}
		for (IonProperty property : ionBean.getProperties().values()) {
			if (property == null || !safeEquals(property.getAttr(), key)) {
				continue;
			}
			property.setSmartType(buildAttributeSmartType(key, value));
			dynamicElement.saveBean();
			return true;
		}
		return false;
	}

	private static MobileSmartSourceType buildAttributeSmartType(String key, String value) {
		if (key.startsWith("[") && key.endsWith("]")) {
			return newScriptSmartType(value);
		}
		if (key.startsWith("(") && key.endsWith(")")) {
			return newScriptSmartType(value);
		}
		if (key.startsWith("#")) {
			return newPlainSmartType(value);
		}
		MobileSmartSourceType interpolated = buildInterpolatedAttributeSmartType(value);
		if (interpolated != null) {
			return interpolated;
		}
		return newPlainSmartType(value);
	}

	private static MobileSmartSourceType buildInterpolatedAttributeSmartType(String value) {
		if (value == null || value.isEmpty() || !value.contains("{{")) {
			return null;
		}
		Matcher matcher = INTERPOLATION_PATTERN.matcher(value);
		int last = 0;
		boolean found = false;
		List<String> expressionParts = new ArrayList<>();
		boolean hasPlainPart = false;
		while (matcher.find()) {
			found = true;
			if (matcher.start() > last) {
				String plain = value.substring(last, matcher.start());
				if (!plain.isEmpty()) {
					expressionParts.add(JSONObject.quote(plain));
					hasPlainPart = true;
				}
			}
			String expr = matcher.group(1) == null ? "" : matcher.group(1).trim();
			expressionParts.add(parenthesizeExpression(expr));
			last = matcher.end();
		}
		if (!found) {
			return null;
		}
		if (last < value.length()) {
			String plain = value.substring(last);
			if (!plain.isEmpty()) {
				expressionParts.add(JSONObject.quote(plain));
				hasPlainPart = true;
			}
		}
		if (expressionParts.isEmpty()) {
			return null;
		}
		if (!hasPlainPart && expressionParts.size() == 1) {
			return newScriptSmartType(stripOuterParentheses(expressionParts.get(0)));
		}
		return newScriptSmartType(String.join(" + ", expressionParts));
	}

	private static boolean isConsumedDirectiveAttribute(String key) {
		return "*ngIf".equals(key)
			|| "*ngFor".equals(key)
			|| "*cdkVirtualFor".equals(key)
			|| "[ngSwitch]".equals(key)
			|| "*ngSwitchCase".equals(key)
			|| "*ngSwitchDefault".equals(key);
	}

	private static List<UIText> importText(TextNode textNode) {
		List<UIText> texts = new ArrayList<>();
		String value = textNode.getWholeText();
		if (value == null || value.isEmpty()) {
			return texts;
		}
		if (value.trim().isEmpty()) {
			if (value.contains("\n") || value.contains("\r")) {
				return texts;
			}
			texts.add(newText(newPlainSmartType(value)));
			return texts;
		}

		Matcher matcher = INTERPOLATION_PATTERN.matcher(value);
		int last = 0;
		boolean found = false;
		List<String> expressionParts = new ArrayList<>();
		while (matcher.find()) {
			found = true;
			if (matcher.start() > last) {
				String plain = normalizeInterpolatedTextPart(value.substring(last, matcher.start()), last == 0, false);
				if (!plain.isEmpty() && !plain.trim().isEmpty()) {
					expressionParts.add(JSONObject.quote(plain));
					texts.add(newText(newPlainSmartType(plain)));
				}
			}
			String expr = matcher.group(1) == null ? "" : matcher.group(1).trim();
			expressionParts.add(parenthesizeExpression(expr));
			texts.add(newTextFromExpression(expr));
			last = matcher.end();
		}
		if (!found) {
			texts.add(newText(newPlainSmartType(value)));
			return texts;
		}
		if (last < value.length()) {
			String plain = normalizeInterpolatedTextPart(value.substring(last), false, true);
			if (!plain.isEmpty() && !plain.trim().isEmpty()) {
				expressionParts.add(JSONObject.quote(plain));
				texts.add(newText(newPlainSmartType(plain)));
			}
		}
			return texts;
		}

	private static UIText newText(MobileSmartSourceType smartType) {
		UIText text = new UIText();
		text.setTextSmartType(smartType);
		text.setEnabled(true);
		return text;
	}

	private static UIText newTextFromExpression(String expression) {
		String expr = stripOuterParentheses(expression == null ? "" : expression.trim());
		Matcher matcher = SIMPLE_TRANSLATE_PATTERN.matcher(expr);
		if (matcher.matches()) {
			UIText text = newText(newPlainSmartType(matcher.group(2)));
			text.setI18n(true);
			return text;
		}
		return newText(newScriptSmartType(expression));
	}

	private static String normalizeInterpolatedTextPart(String plain, boolean leadingEdge, boolean trailingEdge) {
		if (plain == null || plain.isEmpty()) {
			return "";
		}
		String normalized = plain.replace("\r\n", "\n").replace('\r', '\n');
		if (normalized.indexOf('\n') != -1) {
			normalized = normalized.replaceAll("[ \\t]*\\n[ \\t]*", " ");
			if (leadingEdge) {
				normalized = normalized.replaceFirst("^\\s+", "");
			}
			if (trailingEdge) {
				normalized = normalized.replaceFirst("\\s+$", "");
			}
		}
		return normalized;
	}

	private static String parenthesizeExpression(String expression) {
		String expr = expression == null ? "" : expression.trim();
		return "(" + expr + ")";
	}

	private static String stripOuterParentheses(String value) {
		String current = value == null ? "" : value.trim();
		while (current.startsWith("(") && current.endsWith(")")) {
			int depth = 0;
			boolean balanced = true;
			for (int i = 0; i < current.length(); i++) {
				char ch = current.charAt(i);
				if (ch == '(') {
					depth++;
				} else if (ch == ')') {
					depth--;
					if (depth < 0) {
						balanced = false;
						break;
					}
					if (depth == 0 && i < current.length() - 1) {
						balanced = false;
						break;
					}
				}
			}
			if (!balanced || depth != 0) {
				break;
			}
			current = current.substring(1, current.length() - 1).trim();
		}
		return current;
	}

	private static MobileSmartSourceType newPlainSmartType(String value) {
		MobileSmartSourceType smartType = new MobileSmartSourceType();
		smartType.setMode(MobileSmartSourceType.Mode.PLAIN);
		smartType.setSmartValue(value == null ? "" : value);
		return smartType;
	}

	private static MobileSmartSourceType newScriptSmartType(String value) {
		MobileSmartSourceType smartType = new MobileSmartSourceType();
		smartType.setMode(MobileSmartSourceType.Mode.SCRIPT);
		smartType.setSmartValue(value == null ? "" : value);
		return smartType;
	}

	private static MobileSmartSourceType newSourceSmartType(String value) {
		MobileSmartSourceType smartType = new MobileSmartSourceType();
		smartType.setMode(MobileSmartSourceType.Mode.SOURCE);
		smartType.setSmartValue(value == null ? "" : value);
		return smartType;
	}

	private static MobileSmartSourceType buildControlVariableSmartType(JSONObject spec) {
		String smartValue = spec == null ? "" : spec.optString("smartValue");
		if (smartValue != null && !smartValue.isBlank()) {
			if (smartValue.startsWith("plain:")) {
				return newPlainSmartType(smartValue.substring("plain:".length()));
			}
			if (smartValue.startsWith("script:")) {
				return newScriptSmartType(smartValue.substring("script:".length()));
			}
			if (smartValue.startsWith("source:")) {
				return newSourceSmartType(smartValue.substring("source:".length()));
			}
		}
		String mode = spec == null ? "" : spec.optString("mode", "SCRIPT");
		String value = spec == null ? "" : spec.optString("value");
		if ("PLAIN".equalsIgnoreCase(mode)) {
			return newPlainSmartType(value);
		}
		if ("SOURCE".equalsIgnoreCase(mode)) {
			return newSourceSmartType(value);
		}
		return newScriptSmartType(value);
	}

	private static String buildStackVariableValue(JSONObject spec) {
		String smartValue = spec == null ? "" : spec.optString("smartValue");
		if (smartValue != null && !smartValue.isBlank()) {
			if (smartValue.startsWith("plain:")) {
				return toJsStringLiteral(smartValue.substring("plain:".length()));
			}
			if (smartValue.startsWith("script:")) {
				return smartValue.substring("script:".length());
			}
			if (smartValue.startsWith("source:")) {
				return smartValue.substring("source:".length());
			}
		}
		String mode = spec == null ? "" : spec.optString("mode", "SCRIPT");
		String value = spec == null ? "" : spec.optString("value");
		if ("PLAIN".equalsIgnoreCase(mode)) {
			return toJsStringLiteral(value);
		}
		return value == null ? "" : value;
	}

	private static String toJsStringLiteral(String value) {
		String escaped = value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
		return "'" + escaped + "'";
	}

	private static String firstNonBlank(String... values) {
		if (values == null) {
			return "";
		}
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return "";
	}

	private static String preprocessVoidTags(String html) {
		Matcher matcher = VOID_TAG_PATTERN.matcher(html);
		StringBuffer buffer = new StringBuffer();
		while (matcher.find()) {
			String replacement = "<" + matcher.group(1) + matcher.group(2) + " />";
			matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}

	private static void applyStyle(ApplicationComponent application, Path scssPath, ImportReport report) throws Exception {
		applyStyle(application, scssPath, null, report);
	}

	private static void applyStyle(ApplicationComponent application, Path scssPath, String generatedHash, ImportReport report) throws Exception {
		if (!Files.exists(scssPath)) {
			report.warnings.add("Missing scss file for application " + application.getQName() + ": " + scssPath);
			return;
		}
		if (generatedHash != null && !generatedHash.isBlank() && generatedHash.equals(sha256(scssPath))) {
			return;
		}
		String scss = normalizeStyleContent(Files.readString(scssPath, StandardCharsets.UTF_8));
		String computed = normalizeStyleContent(computeApplicationStyle(application));
		if (scss.equals(computed)) {
			return;
		}
		ApplicationStyleImportResult marked = applyMarkedApplicationStyles(application, scss);
		if (marked.hasMarkers) {
			boolean changed = marked.changed;
			String importedUnmarked = stripApplicationStyleMarkers(scss);
			String expectedUnmarked = stripApplicationStyleMarkers(computeApplicationStyle(application));
			String appended = extractAppendedStyleContent(importedUnmarked, expectedUnmarked);
			if (!appended.isEmpty()) {
				UIStyle style = ensureStyle(application);
				String current = normalizeStyleContent(style.getStyleContent() == null ? "" : style.getStyleContent().getString());
				if (!appended.equals(current)) {
					style.setStyleContent(new FormatedContent(appended));
					changed = true;
				}
			} else if (!normalizeStyleContent(importedUnmarked).equals(normalizeStyleContent(expectedUnmarked))) {
				report.warnings.add("Unmapped app.component.scss changes outside Convertigo style markers were ignored for " + application.getQName());
			}
			if (changed) {
				report.stylesUpdated++;
			}
			return;
		}
		UIStyle style = ensureStyle(application);
		String baseline = computeStyleWithoutManagedStyle(application, style);
		String managedScss = extractManagedStyleContent(scss, baseline);
		String current = normalizeStyleContent(style.getStyleContent() == null ? "" : style.getStyleContent().getString());
		if (!managedScss.equals(current)) {
			style.setStyleContent(new FormatedContent(managedScss));
			report.stylesUpdated++;
		}
	}

	private static void applyStyle(PageComponent page, Path scssPath, ImportReport report) throws Exception {
		if (!Files.exists(scssPath)) {
			report.warnings.add("Missing scss file for page " + page.getQName() + ": " + scssPath);
			return;
		}
		String scss = normalizeStyleContent(Files.readString(scssPath, StandardCharsets.UTF_8));
		String computed = normalizeStyleContent(page.getComputedStyle());
		if (scss.equals(computed)) {
			return;
		}
		UIStyle style = ensureStyle(page);
		String current = normalizeStyleContent(style.getStyleContent() == null ? "" : style.getStyleContent().getString());
		if (!scss.equals(current)) {
			style.setStyleContent(new FormatedContent(scss));
			report.stylesUpdated++;
		}
	}

	private static void applyStyle(UISharedComponent component, Path scssPath, ImportReport report) throws Exception {
		if (!Files.exists(scssPath)) {
			report.warnings.add("Missing scss file for component " + component.getQName() + ": " + scssPath);
			return;
		}
		String scss = normalizeStyleContent(Files.readString(scssPath, StandardCharsets.UTF_8));
		String computed = normalizeStyleContent(component.getComputedStyle());
		if (scss.equals(computed)) {
			return;
		}
		UIStyle style = ensureStyle(component);
		String current = normalizeStyleContent(style.getStyleContent() == null ? "" : style.getStyleContent().getString());
		if (!scss.equals(current)) {
			style.setStyleContent(new FormatedContent(scss));
			report.stylesUpdated++;
		}
	}

	private static String normalizeStyleContent(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		return normalizeTextContent(value);
	}

	private static String normalizeTemplateContent(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		return normalizeTextContent(value);
	}

	private static String normalizeScriptContent(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		return normalizeTextContent(value);
	}

	private static String normalizeTextContent(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
		int end = normalized.length();
		while (end > 0 && normalized.charAt(end - 1) == '\n') {
			end--;
		}
		return normalized.substring(0, end);
	}

	private static String computeStyleWithoutManagedStyle(ApplicationComponent application, UIStyle managedStyle) {
		boolean wasEnabled = managedStyle.isEnabled();
		try {
			managedStyle.setEnabled(false);
			return normalizeStyleContent(application.computeStyle());
		} finally {
			managedStyle.setEnabled(wasEnabled);
		}
	}

	private static String extractManagedStyleContent(String scss, String baseline) {
		String imported = normalizeStyleContent(scss);
		String base = normalizeStyleContent(baseline);
		if (imported.equals(base)) {
			return "";
		}
		if (base.isEmpty()) {
			return imported;
		}
		if (imported.startsWith(base)) {
			String delta = imported.substring(base.length());
			while (delta.startsWith("\n")) {
				delta = delta.substring(1);
			}
			return delta;
		}
		return imported;
	}

	private static ApplicationStyleImportResult applyMarkedApplicationStyles(ApplicationComponent application, String scss) {
		Matcher matcher = STYLE_MARKER_PATTERN.matcher(scss == null ? "" : scss);
		boolean hasMarkers = false;
		Map<Long, StringBuilder> markedStyles = new HashMap<>();
		while (matcher.find()) {
			hasMarkers = true;
			Long priority = parsePrioritySpec(matcher.group(1));
			UIStyle style = priority == null ? null : findApplicationStyle(application, priority);
			if (style == null) {
				continue;
			}
			StringBuilder content = markedStyles.computeIfAbsent(priority, ignored -> new StringBuilder());
			String imported = normalizeStyleContent(matcher.group(2));
			if (!imported.isEmpty()) {
				if (content.length() > 0) {
					content.append("\n");
				}
				content.append(imported);
			}
		}
		boolean changed = false;
		for (Map.Entry<Long, StringBuilder> entry : markedStyles.entrySet()) {
			UIStyle style = findApplicationStyle(application, entry.getKey());
			if (style == null) {
				continue;
			}
			String imported = normalizeStyleContent(entry.getValue().toString());
			String current = normalizeStyleContent(style.getStyleContent() == null ? "" : style.getStyleContent().getString());
			if (!imported.equals(current)) {
				style.setStyleContent(new FormatedContent(imported));
				changed = true;
			}
		}
		return new ApplicationStyleImportResult(hasMarkers, changed);
	}

	private static UIStyle findApplicationStyle(ApplicationComponent application, long priority) {
		for (UIComponent component : application.getUIComponentList()) {
			if (component instanceof UIStyle style && style.priority == priority) {
				return style;
			}
		}
		return null;
	}

	private static String stripApplicationStyleMarkers(String scss) {
		return normalizeStyleContent(STYLE_MARKER_PATTERN.matcher(scss == null ? "" : scss).replaceAll(""));
	}

	private static String extractAppendedStyleContent(String scss, String baseline) {
		String imported = normalizeStyleContent(scss);
		String base = normalizeStyleContent(baseline);
		if (imported.equals(base) || base.isEmpty() || !imported.startsWith(base)) {
			return "";
		}
		String delta = imported.substring(base.length());
		while (delta.startsWith("\n")) {
			delta = delta.substring(1);
		}
		return normalizeStyleContent(delta);
	}

	private static void applyScript(PageComponent page, Path tsPath, ImportReport report) throws Exception {
		if (!Files.exists(tsPath)) {
			report.warnings.add("Missing ts file for page " + page.getQName() + ": " + tsPath);
			return;
		}
		String markers = extractScriptMarkers(Files.readString(tsPath, StandardCharsets.UTF_8), PAGE_SCRIPT_MARKERS);
		String current = page.getScriptContent() == null ? "" : page.getScriptContent().getString();
		if (!isEquivalentScriptContent(current, markers, PAGE_SCRIPT_MARKERS, page.isEnabled())) {
			page.setScriptContent(new FormatedContent(normalizeScriptContent(markers)));
			report.scriptsUpdated++;
		}
	}

	private static void applyScript(UISharedComponent component, Path tsPath, ImportReport report) throws Exception {
		if (!Files.exists(tsPath)) {
			report.warnings.add("Missing ts file for component " + component.getQName() + ": " + tsPath);
			return;
		}
		String markers = extractScriptMarkers(Files.readString(tsPath, StandardCharsets.UTF_8), COMPONENT_SCRIPT_MARKERS);
		String current = component.getScriptContent() == null ? "" : component.getScriptContent().getString();
		if (!isEquivalentScriptContent(current, markers, COMPONENT_SCRIPT_MARKERS, component.isEnabled())) {
			component.setScriptContent(new FormatedContent(normalizeScriptContent(markers)));
			report.scriptsUpdated++;
		}
	}

	private static void applyCustomActions(DatabaseObject root, Path tsPath, ImportReport report) throws Exception {
		if (root == null || tsPath == null) {
			return;
		}
		for (DatabaseObject child : root.getDatabaseObjectChildren(true)) {
			if (!(child instanceof UICustomAction action)) {
				continue;
			}
			applyCustomAction(action, resolveCustomActionScriptPath(action, tsPath), report);
		}
	}

	private static void applyCustomAction(UICustomAction action, Path actionTsPath, ImportReport report) throws Exception {
		if (action == null || actionTsPath == null || !Files.exists(actionTsPath)) {
			return;
		}
		String markerId = "function:" + action.getActionName();
		String tsContent = Files.readString(actionTsPath, StandardCharsets.UTF_8);
		String marker = MobileBuilder.getMarker(tsContent, markerId);
		if (marker.isEmpty()) {
			return;
		}
		String imported = normalizeActionValue(extractInlineMarkerBody(marker, markerId));
		String current = action.getActionValue() == null ? "" : action.getActionValue().getString();
		if (!isEquivalentActionValue(current, imported)) {
			action.setActionValue(new FormatedContent(imported));
			report.scriptsUpdated++;
		}
	}

	private static UICustomAction ensurePrimaryCustomAction(UIActionStack stack, JSONObject create) throws Exception {
		for (UIComponent component : stack.getUIComponentList()) {
			if (component instanceof UICustomAction action) {
				return action;
			}
		}
		boolean async = create == null || !create.has("async") || create.optBoolean("async", true);
		UICustomAction action = async ? new UICustomAsyncAction() : new UICustomAction();
		action.setName(create != null && create.has("customActionName") ? create.optString("customActionName") : "CustomAction");
		action.bNew = true;
		action.hasChanged = true;
		stack.add(action);
		return action;
	}

	private static String readSharedActionBody(Path tsPath, UICustomAction action, JSONObject create) throws Exception {
		if (tsPath == null || !Files.exists(tsPath)) {
			return null;
		}
		String tsContent = Files.readString(tsPath, StandardCharsets.UTF_8);
		String markerId = null;
		if (create != null) {
			markerId = create.optString("marker");
		}
		if (markerId == null || markerId.isBlank()) {
			markerId = "function:" + action.getActionName();
		}
		String marker = MobileBuilder.getMarker(tsContent, markerId);
		if (!marker.isEmpty()) {
			return extractInlineMarkerBody(marker, markerId);
		}
		Matcher matcher = ACTION_MARKER_PATTERN.matcher(tsContent);
		if (matcher.find()) {
			String discoveredMarkerId = "function:" + matcher.group(1);
			String discovered = MobileBuilder.getMarker(tsContent, discoveredMarkerId);
			if (!discovered.isEmpty()) {
				return extractInlineMarkerBody(discovered, discoveredMarkerId);
			}
		}
		return tsContent;
	}

	private static Path resolveCustomActionScriptPath(UICustomAction action, Path defaultTsPath) {
		if (action == null || defaultTsPath == null) {
			return null;
		}
		Path tsDir = defaultTsPath.getParent();
		if (tsDir != null) {
			Path tempTsPath = tsDir.resolve(action.getActionName() + ".temp.ts");
			if (Files.exists(tempTsPath)) {
				return tempTsPath;
			}
		}
		if (action.getSharedAction() == null) {
			return defaultTsPath;
		}
		Path appRoot = resolveAppSourceRoot(defaultTsPath);
		if (appRoot == null) {
			return defaultTsPath;
		}
		Path actionBeansTs = appRoot.resolve("services").resolve("actionbeans.service.ts");
		return Files.exists(actionBeansTs) ? actionBeansTs : defaultTsPath;
	}

	private static Path resolveAppSourceRoot(Path tsPath) {
		for (Path current = tsPath.getParent(); current != null; current = current.getParent()) {
			Path fileName = current.getFileName();
			if (fileName != null && "app".equals(fileName.toString())) {
				return current;
			}
		}
		return null;
	}

	private static String extractInlineMarkerBody(String marker, String markerId) {
		if (marker == null || marker.isEmpty() || markerId == null || markerId.isEmpty()) {
			return "";
		}
		String beginMarker = "/*Begin_c8o_" + markerId + "*/";
		String endMarker = "/*End_c8o_" + markerId + "*/";
		return marker.replace(beginMarker, "").replace(endMarker, "");
	}

	private static boolean matchesGeneratedTemplate(String computedTemplate, String html) {
		String normalizedComputed = normalizeTemplateContent(computedTemplate);
		String normalizedHtml = normalizeTemplateContent(html);
		return normalizedComputed.equals(normalizedHtml);
	}

	private static String normalizeTemplateIgnoringPriorityClasses(String template) {
		if (template == null || template.isBlank()) {
			return "";
		}
		try {
			String xhtml = preprocessVoidTags(template);
			Document document = Jsoup.parse("<" + PARSE_ROOT_TAG + ">" + xhtml + "</" + PARSE_ROOT_TAG + ">", "", Parser.xmlParser());
			Element parseRoot = document.selectFirst(PARSE_ROOT_TAG);
			if (parseRoot == null) {
				return normalizeTemplateContent(template);
			}
			for (Element element : parseRoot.getAllElements()) {
				if (!element.hasAttr("class")) {
					continue;
				}
				List<String> classNames = new ArrayList<>();
				for (String token : element.classNames()) {
					if (!token.matches("class\\d+")) {
						classNames.add(token);
					}
				}
				if (classNames.isEmpty()) {
					element.removeAttr("class");
				} else {
					element.attr("class", String.join(" ", classNames));
				}
			}
			return normalizeTemplateContent(parseRoot.html());
		} catch (Exception e) {
			return normalizeTemplateContent(template);
		}
	}

	private static boolean isEquivalentScriptContent(String current, String importedMarkers, String[] markerIds, boolean enabled) {
		String normalizedCurrent = normalizeScriptContent(current);
		String normalizedImported = normalizeScriptContent(importedMarkers);
		if (normalizedCurrent.equals(normalizedImported)) {
			return true;
		}
		String canonicalCurrent = canonicalizeScriptContent(normalizedCurrent, markerIds);
		String canonicalImported = canonicalizeScriptContent(normalizedImported, markerIds);
		if (canonicalCurrent.equals(canonicalImported)) {
			return true;
		}
		return !enabled && hasOnlyEmptyMarkerBodies(normalizedImported, markerIds);
	}

	private static boolean isEquivalentActionValue(String current, String imported) {
		return canonicalizeActionValue(current).equals(canonicalizeActionValue(imported));
	}

	private static String normalizeActionValue(String value) {
		String canonical = canonicalizeActionValue(value);
		return canonical.isEmpty() ? "" : canonical + '\n';
	}

	private static String canonicalizeActionValue(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
		String[] lines = normalized.split("\n", -1);
		int start = 0;
		int end = lines.length;
		while (start < end && lines[start].trim().isEmpty()) {
			start++;
		}
		while (end > start && lines[end - 1].trim().isEmpty()) {
			end--;
		}
		if (start >= end) {
			return "";
		}
		StringBuilder canonical = new StringBuilder();
		for (int i = start; i < end; i++) {
			if (canonical.length() > 0) {
				canonical.append('\n');
			}
			canonical.append(stripTrailingWhitespace(lines[i]));
		}
		return canonical.toString();
	}

	private static String stripTrailingWhitespace(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		int end = value.length();
		while (end > 0) {
			char c = value.charAt(end - 1);
			if (c != ' ' && c != '\t') {
				break;
			}
			end--;
		}
		return value.substring(0, end);
	}

	private static String canonicalizeScriptContent(String content, String[] markerIds) {
		if (content == null || content.isBlank() || markerIds == null || markerIds.length == 0) {
			return "";
		}
		StringBuilder canonical = new StringBuilder();
		for (String markerId : markerIds) {
			String marker = MobileBuilder.getMarker(content, markerId);
			if (marker.isEmpty()) {
				continue;
			}
			String body = normalizeScriptContent(MobileBuilder.getFormatedContent(marker, markerId));
			if (body.isBlank()) {
				continue;
			}
			if (canonical.length() > 0) {
				canonical.append('\n');
			}
			canonical.append(markerId).append('\n').append(body);
		}
		return canonical.toString();
	}

	private static boolean hasOnlyEmptyMarkerBodies(String content, String[] markerIds) {
		if (content == null || content.isBlank() || markerIds == null || markerIds.length == 0) {
			return false;
		}
		boolean found = false;
		for (String markerId : markerIds) {
			String marker = MobileBuilder.getMarker(content, markerId);
			if (marker.isEmpty()) {
				continue;
			}
			found = true;
			String body = normalizeScriptContent(MobileBuilder.getFormatedContent(marker, markerId));
			if (!body.isBlank()) {
				return false;
			}
		}
		return found;
	}

	private static String extractScriptMarkers(String tsContent, String[] markerIds) {
		if (tsContent == null || tsContent.isEmpty() || markerIds == null || markerIds.length == 0) {
			return "";
		}
		StringBuilder markers = new StringBuilder();
		for (String markerId : markerIds) {
			String marker = MobileBuilder.getMarker(tsContent, markerId);
			if (marker.isEmpty()) {
				continue;
			}
			if (markers.length() > 0) {
				markers.append('\n');
			}
			markers.append(normalizeScriptContent(marker));
		}
		return markers.toString();
	}

	private static void disablePageVisualComponents(PageComponent page) {
		for (UIComponent component : page.getUIComponentList()) {
			if (component == null) {
				continue;
			}
			if (isManaged(component) || component instanceof UIPageEvent || component instanceof UIEventSubscriber) {
				continue;
			}
			component.setEnabled(false);
		}
	}

	private static void disableSharedVisualComponents(UISharedComponent component) {
		for (UIComponent child : component.getUIComponentList()) {
			if (child == null) {
				continue;
			}
			if (isManaged(child) || child instanceof UICompVariable || child instanceof UICompEvent || child instanceof UISharedComponentEvent) {
				continue;
			}
			child.setEnabled(false);
		}
	}

	private static void removePageVisualComponents(PageComponent page) throws Exception {
		for (UIComponent component : new ArrayList<>(page.getUIComponentList())) {
			if (component == null) {
				continue;
			}
			if (isManaged(component) || component instanceof UIPageEvent || component instanceof UIEventSubscriber || component instanceof UIStyle || component instanceof UICustom) {
				continue;
			}
			page.remove(component);
		}
	}

	private static void removeSharedVisualComponents(UISharedComponent component) throws Exception {
		for (UIComponent child : new ArrayList<>(component.getUIComponentList())) {
			if (child == null) {
				continue;
			}
			if (isManaged(child) || child instanceof UICompVariable || child instanceof UICompEvent || child instanceof UISharedComponentEvent || child instanceof UIStyle || child instanceof UICustom) {
				continue;
			}
			component.remove(child);
		}
	}

	private static UICustom ensureTemplate(PageComponent page) throws Exception {
		UICustom existing = null;
		for (UIComponent component : page.getUIComponentList()) {
			if (component instanceof UICustom custom && TEMPLATE_NAME.equals(component.getName())) {
				existing = custom;
				break;
			}
		}
		if (existing != null) {
			existing.setEnabled(true);
			return existing;
		}
		UICustom created = new UICustom();
		created.setName(TEMPLATE_NAME);
		created.setCustomTemplate("");
		page.add(created);
		return created;
	}

	private static UICustom ensureTemplate(UISharedComponent component) throws Exception {
		UICustom existing = null;
		for (UIComponent child : component.getUIComponentList()) {
			if (child instanceof UICustom custom && TEMPLATE_NAME.equals(child.getName())) {
				existing = custom;
				break;
			}
		}
		if (existing != null) {
			existing.setEnabled(true);
			return existing;
		}
		UICustom created = new UICustom();
		created.setName(TEMPLATE_NAME);
		created.setCustomTemplate("");
		component.add(created);
		return created;
	}

	private static UIElement ensureRoot(PageComponent page) throws Exception {
		UIElement existing = null;
		for (UIComponent component : page.getUIComponentList()) {
			if (component instanceof UIElement element && ROOT_NAME.equals(component.getName())) {
				existing = element;
				break;
			}
		}
		if (existing != null) {
			existing.setEnabled(true);
			return existing;
		}
		UIElement created = new UIElement();
		created.setName(ROOT_NAME);
		created.setTagName(ROOT_TAG_NAME);
		page.add(created);
		return created;
	}

	private static UIElement ensureRoot(UISharedComponent component) throws Exception {
		UIElement existing = null;
		for (UIComponent child : component.getUIComponentList()) {
			if (child instanceof UIElement element && ROOT_NAME.equals(child.getName())) {
				existing = element;
				break;
			}
		}
		if (existing != null) {
			existing.setEnabled(true);
			return existing;
		}
		UIElement created = new UIElement();
		created.setName(ROOT_NAME);
		created.setTagName(ROOT_TAG_NAME);
		component.add(created);
		return created;
	}

	private static UIStyle ensureStyle(ApplicationComponent application) throws Exception {
		UIStyle existing = null;
		for (UIComponent component : application.getUIComponentList()) {
			if (component instanceof UIStyle style && STYLE_NAME.equals(component.getName())) {
				existing = style;
				break;
			}
		}
		if (existing != null) {
			existing.setEnabled(true);
			return existing;
		}
		UIStyle created = new UIStyle();
		created.setName(STYLE_NAME);
		created.setStyleContent(new FormatedContent(""));
		application.add(created);
		return created;
	}

	private static UIStyle ensureStyle(PageComponent page) throws Exception {
		UIStyle existing = null;
		for (UIComponent component : page.getUIComponentList()) {
			if (component instanceof UIStyle style && STYLE_NAME.equals(component.getName())) {
				existing = style;
				break;
			}
		}
		if (existing != null) {
			existing.setEnabled(true);
			return existing;
		}
		UIStyle created = new UIStyle();
		created.setName(STYLE_NAME);
		created.setStyleContent(new FormatedContent(""));
		page.add(created);
		return created;
	}

	private static UIStyle ensureStyle(UISharedComponent component) throws Exception {
		UIStyle existing = null;
		for (UIComponent child : component.getUIComponentList()) {
			if (child instanceof UIStyle style && STYLE_NAME.equals(child.getName())) {
				existing = style;
				break;
			}
		}
		if (existing != null) {
			existing.setEnabled(true);
			return existing;
		}
		UIStyle created = new UIStyle();
		created.setName(STYLE_NAME);
		created.setStyleContent(new FormatedContent(""));
		component.add(created);
		return created;
	}

	private static void clearChildren(UIComponent parent) throws Exception {
		for (UIComponent child : new ArrayList<>(parent.getUIComponentList())) {
			parent.remove(child);
		}
	}

	private static void removeStructuralChildren(UIComponent parent) throws Exception {
		for (UIComponent child : new ArrayList<>(parent.getUIComponentList())) {
			if (child == null || isNonStructuralChild(child)) {
				continue;
			}
			parent.remove(child);
		}
	}

	private static void disableTemplate(PageComponent page) {
		for (UIComponent component : page.getUIComponentList()) {
			if (component instanceof UICustom && TEMPLATE_NAME.equals(component.getName())) {
				component.setEnabled(false);
			}
		}
	}

	private static void disableTemplate(UISharedComponent component) {
		for (UIComponent child : component.getUIComponentList()) {
			if (child instanceof UICustom && TEMPLATE_NAME.equals(child.getName())) {
				child.setEnabled(false);
			}
		}
	}

	private static void disableRoot(PageComponent page) {
		for (UIComponent component : page.getUIComponentList()) {
			if (component instanceof UIElement && ROOT_NAME.equals(component.getName())) {
				component.setEnabled(false);
			}
		}
	}

	private static void disableRoot(UISharedComponent component) {
		for (UIComponent child : component.getUIComponentList()) {
			if (child instanceof UIElement && ROOT_NAME.equals(child.getName())) {
				child.setEnabled(false);
			}
		}
	}

	private static void removeManagedRoot(PageComponent page) throws Exception {
		for (UIComponent component : new ArrayList<>(page.getUIComponentList())) {
			if (component instanceof UIElement && ROOT_NAME.equals(component.getName())) {
				page.remove(component);
			}
		}
	}

	private static void removeManagedRoot(UISharedComponent component) throws Exception {
		for (UIComponent child : new ArrayList<>(component.getUIComponentList())) {
			if (child instanceof UIElement && ROOT_NAME.equals(child.getName())) {
				component.remove(child);
			}
		}
	}

	private static boolean isManaged(UIComponent component) {
		String name = component.getName();
		return TEMPLATE_NAME.equals(name) || STYLE_NAME.equals(name) || ROOT_NAME.equals(name);
	}

	private static Path resolve(File projectDir, String relative) {
		if (relative == null || relative.isBlank()) {
			return projectDir.toPath();
		}
		return projectDir.toPath().resolve(relative.replace('\\', '/')).normalize();
	}

	private static String relativize(File projectDir, File file) {
		Path root = Paths.get(projectDir.getAbsolutePath()).normalize();
		Path child = Paths.get(file.getAbsolutePath()).normalize();
		return root.relativize(child).toString().replace(File.separatorChar, '/');
	}

	private static String stripExtension(String filename) {
		int index = filename.lastIndexOf('.');
		return index == -1 ? filename : filename.substring(0, index);
	}

	private static String sha256(Path path) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(Files.readAllBytes(path));
		StringBuilder hex = new StringBuilder(hash.length * 2);
		for (byte value : hash) {
			hex.append(String.format("%02x", value & 0xff));
		}
		return hex.toString();
	}
}
