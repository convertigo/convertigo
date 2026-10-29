package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.twinsoft.convertigo.beans.BeansDefaultValues;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.YamlConverter.Format;

public class StandardYamlConverterTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private static final String HEADER = "convertigo: 8.5.0.m006\n";

	@Before public void logging() {
		Engine.logEngine = Engine.logBeans = Engine.logDatabaseObjectManager = Logger.getLogger("standard-yaml-test");
	}

	private Document project() throws Exception {
		Document document = XMLUtils.createDom();
		Element root = document.createElement("convertigo");
		document.appendChild(root);
		root.setAttribute("convertigo", "8.5.0.m006");
		Element project = document.createElement("bean");
		project.setAttribute("yaml_key", "Demo [core.Project]");
		root.appendChild(project);
		YamlConverter.setFormat(document, Format.STANDARD);
		return document;
	}

	private File write(Document document) throws Exception {
		Path dir = folder.newFolder().toPath();
		File yaml = dir.resolve("c8oProject.yaml").toFile();
		YamlConverter.writeYaml(document, yaml, dir.resolve("_c8oProject").toFile());
		assertTrue(Files.readString(yaml.toPath()).startsWith("convertigo: 8.5.0.m006\n"));
		return yaml;
	}

	private static void same(Document expected, Document actual) {
		assertTrue("DOM node types, names, attributes, order and scalar values must survive", expected.isEqualNode(actual));
	}

	private static void rewrite(Path project, boolean jackson, boolean flow) throws Exception {
		try (var paths = Files.walk(project)) {
			for (Path path : paths.filter(p -> p.toString().endsWith(".yaml")).toList()) {
				String content = Files.readString(path);
				if (jackson) {
					var mapper = new YAMLMapper();
					content = mapper.writeValueAsString(mapper.readValue(content, Map.class));
				} else {
					Object data = new Load(LoadSettings.builder().build()).loadFromString(content);
					content = new Dump(DumpSettings.builder().setExplicitStart(true)
							.setDefaultScalarStyle(ScalarStyle.DOUBLE_QUOTED)
							.setDefaultFlowStyle(flow ? FlowStyle.FLOW : FlowStyle.BLOCK).build()).dumpToString(data);
				}
				Files.writeString(path, "# Rewritten by a standard YAML tool\n" + content);
			}
		}
	}

	@Test public void allXmlNodeKindsAndExactScalarsSurviveBothLibraries() throws Exception {
		Document expected = project();
		Element bean = (Element) expected.getDocumentElement().getFirstChild();
		var nested = expected.createElement("value");
		nested.setAttribute("class", "java.lang.String");
		nested.setAttribute("escaped", "\r\t\u2028'\" leading trailing ");
		bean.appendChild(nested);
		Element qualified = expected.createElementNS("urn:example", "ex:qualified");
		qualified.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ex", "urn:example");
		qualified.setAttributeNS("urn:example", "ex:attribute", "value");
		nested.appendChild(qualified);
		for (String value : List.of("", " ", " leading", "trailing ", "both  ", "line\rreturn\r\nend", "\t tab\t", "a\u2028b\u2029c\u0085d",
				"'single' and \"double\"", "one\ntwo", "one\n", "one\n\n", "true", "null", "01", "yes", "2001-01-01", "é漢🙂", "&*{}[]: # | >", "\\L\\P")) {
			nested.appendChild(expected.createTextNode(value));
			nested.appendChild(expected.createCDATASection(value));
			Element repeat = expected.createElement("repeated");
			repeat.appendChild(expected.createTextNode(value));
			nested.appendChild(repeat);
		}
		nested.appendChild(expected.createComment("XML comment"));
		nested.appendChild(expected.createProcessingInstruction("target", "data"));
		File yaml = write(expected);
		same(expected, YamlConverter.readYaml(yaml));
		for (boolean jackson : List.of(false, true)) {
			rewrite(yaml.toPath().getParent(), jackson, !jackson);
			same(expected, YamlConverter.readYaml(yaml));
		}
	}

	@Test public void foldedChompedQuotedAndFlowSyntaxIsStandardYaml() throws Exception {
		File yaml = folder.newFile("syntax.yaml");
		Files.writeString(yaml.toPath(), "\uFEFF---\n" + HEADER + "↓Demo [core.Project]:\n"
				+ "  quoted: \"tab\\tCR\\rLS\\L\\\"quote\"\n"
				+ "  folded: >-\n    first\n    second\n"
				+ "  keep: |+\n    last\n\n");
		Document doc = YamlConverter.readYaml(yaml);
		assertEquals("tab\tCR\rLS\u2028\"quote", doc.getElementsByTagName("quoted").item(0).getTextContent());
		assertEquals("first second", doc.getElementsByTagName("folded").item(0).getTextContent());
		assertEquals("last\n\n", doc.getElementsByTagName("keep").item(0).getTextContent());
		same(doc, YamlConverter.readYaml(write(doc)));
	}

	@Test public void scriptLayoutKeepsTabsChompingAndWhitespaceData() throws Exception {
		Document expected = project();
		Element bean = (Element) expected.getDocumentElement().getFirstChild();
		for (String value : List.of("\tfunction run() {\n\t\treturn '\uE000\uE001';\n\t}",
				"first\nsecond\n", "first\nsecond\n\n", "first \n\tsecond\t\n", "\n\n")) {
			Element property = expected.createElement("script" + bean.getChildNodes().getLength());
			Element xml = expected.createElement("xmlizable");
			xml.setAttribute("classname", "com.twinsoft.convertigo.beans.common.FormatedContent");
			Element content = expected.createElement("content");
			content.appendChild(expected.createCDATASection(value));
			xml.appendChild(content); property.appendChild(xml); bean.appendChild(property);
		}
		File yaml = write(expected);
		String text = Files.readString(yaml.toPath());
		assertTrue("Tabs inside clean scripts should keep literal block presentation", text.contains("\tfunction run()"));
		assertTrue("Significant trailing spaces stay literal", text.contains("first \n"));
		same(expected, YamlConverter.readYaml(yaml));
		for (boolean jackson : List.of(false, true)) {
			rewrite(yaml.toPath().getParent(), jackson, false);
			same(expected, YamlConverter.readYaml(yaml));
		}
	}

	@Test public void referencesAreResolvedOnlyWhenRequestedAndSurviveStandardRewriters() throws Exception {
		Document expected = project();
		Element sequence = expected.createElement("bean");
		sequence.setAttribute("yaml_key", "Main [sequences.GenericSequence]");
		sequence.setAttribute("yaml_file", "sequences/Main.yaml");
		Element comment = expected.createElement("comment");
		comment.setTextContent("payload"); sequence.appendChild(comment);
		expected.getDocumentElement().getFirstChild().appendChild(sequence);
		File yaml = write(expected);
		sequence.removeAttribute("yaml_file");
		same(expected, YamlConverter.readYaml(yaml));
		for (boolean jackson : List.of(false, true)) {
			rewrite(yaml.toPath().getParent(), jackson, false);
			same(expected, YamlConverter.readYaml(yaml));
		}
		Document parsed = YamlConverter.readYaml(yaml);
		YamlConverter.writeYaml(parsed, yaml, new File(yaml.getParentFile(), "_c8oProject"));
		assertTrue(Files.readString(yaml.toPath()).contains("↓Main [sequences.GenericSequence]: sequences/Main.yaml"));
		Files.delete(yaml.toPath().getParent().resolve("_c8oProject/sequences/Main.yaml"));
		assertEquals("Demo", DatabaseObjectsManager.getProjectName(yaml));
		assertEquals("8.5.0.m006", DatabaseObjectsManager.getProjectVersion(yaml));
		assertEquals(0, YamlConverter.readYaml(yaml, true).getElementsByTagName("comment").getLength());
		assertThrows(IOException.class, () -> YamlConverter.readYaml(yaml));
	}

	@Test public void literalBlocksKeepEveryWhitespaceLineThroughAllThreeReaders() throws Exception {
		Document expected = project();
		Element bean = (Element) expected.getDocumentElement().getFirstChild();
		Element comment = expected.createElement("comment"); comment.setTextContent("🙂 C8O_LITERAL_0"); bean.appendChild(comment);
		var values = List.of("\tstart \n\t\tend\t", "  first\n    second\n", " \nnext", " \n", "  \n \n",
				"\n  content\n", "\n\n", "\n", "a\n", "a\n\n", "a\n\n\n", "\t\n\t\n",
				"\n\nlast", "a\n ", "a\n \n", "a\n\t\n", "---\n...\n# script\n↓text: |\n", "{\n  \"json\": true \n}\n");
		for (int i = 0; i < values.size(); i++) {
			Element script = expected.createElement("script" + i);
			script.appendChild(expected.createCDATASection(values.get(i))); bean.appendChild(script);
		}
		// Namespaced attributes must also leave flow style when their values need a block.
		Element qualified = expected.createElementNS("urn:example", "ex:element");
		qualified.setAttributeNS("urn:example", "ex:attribute", " first \n\tsecond\n"); bean.appendChild(qualified);
		File yaml = write(expected);
		String text = Files.readString(yaml.toPath());
		assertTrue(text.contains("|2"));
		var parser = new org.yaml.snakeyaml.Yaml(new org.yaml.snakeyaml.constructor.SafeConstructor(new org.yaml.snakeyaml.LoaderOptions()));
		Map<?, ?> snake = parser.load(text);
		Map<?, ?> engine = (Map<?, ?>) new Load(LoadSettings.builder().build()).loadFromString(text);
		Map<?, ?> jackson = new YAMLMapper().readValue(text, Map.class);
		for (Map<?, ?> data : List.of(snake, engine, jackson)) {
			Map<?, ?> properties = (Map<?, ?>) data.get("↓Demo [core.Project]");
			for (int i = 0; i < values.size(); i++) assertEquals(values.get(i), ((Map<?, ?>) properties.get("script" + i)).get("→"));
		}
		var literalValues = new java.util.ArrayList<String>();
		for (var event : parser.parse(new java.io.StringReader(text))) {
			if (event instanceof org.yaml.snakeyaml.events.ScalarEvent scalar
					&& scalar.getScalarStyle() == org.yaml.snakeyaml.DumperOptions.ScalarStyle.LITERAL) literalValues.add(scalar.getValue());
		}
		assertTrue("Every ordinary multiline value must use a literal block", literalValues.containsAll(values));
		same(expected, YamlConverter.readYaml(yaml));
		for (boolean useJackson : List.of(false, true)) {
			rewrite(yaml.toPath().getParent(), useJackson, !useJackson);
			same(expected, YamlConverter.readYaml(yaml));
		}
	}

	@Test public void controlCharactersRemainEscapedWithoutChangingTheirValues() throws Exception {
		Document expected = project();
		Element bean = (Element) expected.getDocumentElement().getFirstChild();
		var values = List.of("a\rb\nc", "a\r\nb", "a\u0085b\n", "a\u2028b\n", "a\u2029b\n", "a\0b\n", "a\u0007b\n", "a\u0080b\n");
		for (int i = 0; i < values.size(); i++) {
			Element property = expected.createElement("control" + i); property.setTextContent(values.get(i)); bean.appendChild(property);
		}
		File yaml = write(expected);
		String text = Files.readString(yaml.toPath());
		assertFalse(text.contains(": |"));
		Map<?, ?> snake = new org.yaml.snakeyaml.Yaml(new org.yaml.snakeyaml.constructor.SafeConstructor(new org.yaml.snakeyaml.LoaderOptions())).load(text);
		for (int i = 0; i < values.size(); i++) assertEquals(values.get(i), ((Map<?, ?>) snake.get("↓Demo [core.Project]")).get("control" + i));
		same(expected, YamlConverter.readYaml(yaml));
		for (boolean jackson : List.of(false, true)) {
			rewrite(yaml.toPath().getParent(), jackson, !jackson);
			same(expected, YamlConverter.readYaml(yaml));
		}
	}

	@Test public void scalarReferencesAndEmptyBeansAreUnambiguous() throws Exception {
		File yaml = folder.newFile("references.yaml");
		Path sources = yaml.toPath().getParent().resolve("_c8oProject"); Files.createDirectories(sources);
		Files.writeString(sources.resolve("Main.yaml"), "comment: included\n");
		Files.writeString(yaml.toPath(), HEADER + "↓Demo [core.Project]:\n"
				+ "  comment: missing.yaml\n  ↓Main [sequences.GenericSequence]: Main.yaml\n"
				+ "  ↓Empty [sequences.GenericSequence]: ''\n  ↓EmptyMap [sequences.GenericSequence]: {}\n");
		Document document = YamlConverter.readYaml(yaml);
		assertEquals(4, document.getElementsByTagName("bean").getLength());
		assertEquals("missing.yaml", document.getElementsByTagName("comment").item(0).getTextContent());
		assertEquals("included", document.getElementsByTagName("comment").item(1).getTextContent());
		YamlConverter.writeYaml(document, yaml, sources.toFile());
		String text = Files.readString(yaml.toPath());
		assertTrue(text.contains("↓Main [sequences.GenericSequence]: Main.yaml"));
		assertTrue(text.contains("↓EmptyMap [sequences.GenericSequence]: ''"));
		assertFalse(text.contains("🗏"));
		for (String value : List.of("plain text", "file.yml", "../outside.yaml", "/outside.yaml", "\"bad\\nfile.yaml\"",
				"\"bad\\tfile.yaml\"", "\"bad\\\\file.yaml\"", "{🗏: Main.yaml}")) {
			Files.writeString(yaml.toPath(), HEADER + "↓Demo [core.Project]: " + value + "\n");
			assertThrows(IOException.class, () -> YamlConverter.readYaml(yaml, true));
		}
	}

	@Test public void aSaveWritesStandardOnceTheBeansRequire85() {
		assertEquals(Format.LEGACY, YamlConverter.savedFormat(Format.LEGACY, "7.9.0.m006"));
		assertEquals(Format.LEGACY, YamlConverter.savedFormat(Format.LEGACY, "8.4.7.m006"));
		assertEquals(Format.STANDARD, YamlConverter.savedFormat(Format.LEGACY, "8.5.0.m006"));
		assertEquals(Format.STANDARD, YamlConverter.savedFormat(Format.LEGACY, "8.5.0"));
		assertEquals(Format.STANDARD, YamlConverter.savedFormat(Format.LEGACY, "8.10.0.m007"));
		assertEquals(Format.STANDARD, YamlConverter.savedFormat(Format.LEGACY, "10.0.0.m006"));
		assertEquals("a standard project stays standard", Format.STANDARD, YamlConverter.savedFormat(Format.STANDARD, "7.9.0.m006"));
	}

	@Test public void minimumVersionAndFormatSurviveShrinkCloneUnshrinkAndProjectExport() throws Exception {
		assertEquals(Format.STANDARD, new Project().getYamlFormat());
		assertEquals("8.5.0.m006", YamlConverter.minimumVersion("7.9.0.m006", Format.STANDARD));
		assertEquals("8.7.1.m007", YamlConverter.minimumVersion("8.7.1.m007", Format.STANDARD));
		assertEquals("7.9.0.m006", YamlConverter.minimumVersion("7.9.0.m006", Format.LEGACY));
		for (Format format : Format.values()) {
			Document input = project();
			YamlConverter.setFormat(input, format);
			Document expanded = BeansDefaultValues.unshrinkProject(input);
			assertEquals(format, YamlConverter.getFormat(expanded));
			assertEquals(format, YamlConverter.getFormat((Document) expanded.cloneNode(true)));
			Element projectNode = (Element) expanded.getDocumentElement().getFirstChild();
			projectNode.setAttribute("version", input.getDocumentElement().getAttribute("convertigo"));
			Project project = new Project();
			project.preconfigure(projectNode);
			assertEquals(format, project.getYamlFormat());
			assertEquals(format, YamlConverter.getFormat(CarUtils.exportProjectDocument(project)));
			Document shrunk = BeansDefaultValues.shrinkProject(expanded);
			assertEquals(format, YamlConverter.getFormat(shrunk));
			String minimum = shrunk.getDocumentElement().getFirstChild().getTextContent();
			if (format == Format.STANDARD) assertTrue(VersionUtils.compare(minimum, "8.5.0") >= 0);
		}
	}

	@Test public void renameUsesDecodedValuesAndPreservesReferencesWhenAsked() throws Exception {
		for (boolean keep : List.of(false, true)) {
			Document input = project();
			Element value = input.createElement("source");
			value.appendChild(input.createCDATASection("Demo.Sequence"));
			input.getDocumentElement().getFirstChild().appendChild(value);
			File yaml = write(input);
			rewrite(yaml.toPath().getParent(), true, false);
			ProjectUtils.renameProjectFile(yaml, "Renamed", keep);
			Document output = YamlConverter.readYaml(yaml);
			assertEquals("Renamed", YamlConverter.projectName(output));
			assertEquals((keep ? "Demo" : "Renamed") + ".Sequence", output.getElementsByTagName("source").item(0).getTextContent());
			assertEquals(Format.STANDARD, YamlConverter.getFormat(output));
		}
	}

	@Test public void malformedDocumentsFailWithoutReturningPartialProjectsOrEchoingTheirContent() throws Exception {
		String valid = HEADER + "↓Demo [core.Project]: ''\n";
		for (String malformed : List.of("", "---\n", valid + "unknown: secret-sentinel\n", valid + "convertigo: 8.5.0.m006\n",
				valid + "---\n" + valid, valid + "broken: [secret-sentinel\n", valid + "  misplaced: secret-sentinel\n",
				HEADER + "↓Demo [core.Project]: true\n",
				HEADER + "↓Demo [core.Project]: {comment: !custom secret-sentinel}\n",
				HEADER + "↓Demo [core.Project]: &cycle [*cycle]\n", HEADER + "↓Demo [core.Project]: []\n",
				valid.replace("8.5.0", "7.9.0"), valid.replace("convertigo:", "format:"),
				HEADER + "↓Demo [core.Project]: {comment: one, comment: two}\n",
				HEADER + "↓Demo [core.Project]: '../outside.yaml'\n",
				HEADER + "↓Demo [core.Project]: \"bad\\0.yaml\"\n")) {
			File file = folder.newFile(); Files.writeString(file.toPath(), malformed);
			Exception error = assertThrows(Exception.class, () -> YamlConverter.readYaml(file));
			assertFalse(error.toString().contains("secret-sentinel"));
			assertNull(error.getCause());
		}
		File deep = folder.newFile();
		Files.writeString(deep.toPath(), HEADER + "↓Demo [core.Project]: " + "[".repeat(300) + "'end'" + "]".repeat(300));
		assertThrows(IOException.class, () -> YamlConverter.readYaml(deep));
	}

	@Test public void invalidIncludedDocumentsAndCyclesAreRejected() throws Exception {
		File yaml = write(project());
		Path dir = yaml.toPath().getParent().resolve("_c8oProject"); Files.createDirectories(dir);
		Files.writeString(yaml.toPath(), HEADER + "↓Demo [core.Project]: node.yaml\n");
		for (String node : List.of("{invalid name: value}", "↓Loop [sequences.GenericSequence]: node.yaml",
				"comment: one\ncomment: two", "🗏: bad.yaml")) {
			Files.writeString(dir.resolve("node.yaml"), node);
			assertThrows(IOException.class, () -> YamlConverter.readYaml(yaml));
		}
	}

	@Test public void sourceSymlinksCannotEscapeTheProjectAndOtherDocumentsSurvive() throws Exception {
		File yaml = write(project());
		Path sources = yaml.toPath().getParent().resolve("_c8oProject"); Files.createDirectories(sources);
		Path outside = folder.newFolder().toPath(); Files.writeString(outside.resolve("node.yaml"), "outside");
		Files.createSymbolicLink(sources.resolve("link"), outside);
		Files.writeString(yaml.toPath(), HEADER + "↓Demo [core.Project]: link/node.yaml\n");
		assertThrows(IOException.class, () -> YamlConverter.readYaml(yaml));
		Files.delete(sources.resolve("link"));
		Files.writeString(sources.resolve("tags.json"), "{\"schemaVersion\":1}");
		Files.writeString(sources.resolve("Removed.yaml"), "obsolete");
		YamlConverter.writeYaml(project(), yaml, sources.toFile());
		assertFalse(Files.exists(sources.resolve("Removed.yaml")));
		assertEquals("{\"schemaVersion\":1}", Files.readString(sources.resolve("tags.json")));
	}

	@Test public void standardDocumentCanBeWrittenWithoutASourceDirectory() throws Exception {
		Document expected = project();
		File yaml = folder.newFile();
		YamlConverter.writeYaml(expected, yaml);
		same(expected, YamlConverter.readYaml(yaml));
	}

	@Test public void aLaterWriteFailureRestoresEarlierSources() throws Exception {
		Document document = project();
		Element first = document.createElement("bean");
		first.setAttribute("yaml_key", "First [sequences.GenericSequence]");
		first.setAttribute("yaml_file", "sequences/First.yaml");
		Element comment = document.createElement("comment"); comment.setTextContent("before"); first.appendChild(comment);
		document.getDocumentElement().getFirstChild().appendChild(first);
		File yaml = write(document);
		Path sources = yaml.toPath().getParent().resolve("_c8oProject");
		byte[] beforeRoot = Files.readAllBytes(yaml.toPath());
		byte[] beforeSource = Files.readAllBytes(sources.resolve("sequences/First.yaml"));
		comment.setTextContent("changed");
		Element second = document.createElement("bean");
		second.setAttribute("yaml_key", "Second [sequences.GenericSequence]");
		second.setAttribute("yaml_file", "blocked/Second.yaml");
		document.getDocumentElement().getFirstChild().appendChild(second);
		Files.writeString(sources.resolve("blocked"), "not a directory");
		assertThrows(IOException.class, () -> YamlConverter.writeYaml(document, yaml, sources.toFile()));
		assertArrayEquals(beforeRoot, Files.readAllBytes(yaml.toPath()));
		assertArrayEquals(beforeSource, Files.readAllBytes(sources.resolve("sequences/First.yaml")));
	}

	@Test public void correctedLegacyShapeSurvivesIndependentYamlRewriters() throws Exception {
		Path legacy = folder.newFolder().toPath();
		Files.createDirectories(legacy.resolve("_c8oProject/sequences"));
		Files.writeString(legacy.resolve("c8oProject.yaml"), "↑convertigo: 7.9.0.m006\n↓Demo [core.Project]: \n"
				+ "  comment: A small service\n  ↓Main [sequences.GenericSequence]: 🗏 sequences/Main.yaml");
		Files.writeString(legacy.resolve("_c8oProject/sequences/Main.yaml"), "responseTimeout: 120\n"
				+ "↓Compute [steps.SimpleStep-123]: \n  expression: |\n    const result = 1;\n    result++;\n"
				+ "↓value [variables.RequestableVariable-124]: \n  value: ''");
		Document expected = YamlConverter.readYaml(legacy.resolve("c8oProject.yaml").toFile());
		expected.getDocumentElement().setAttribute("convertigo", "8.5.0.m006");
		// An editor fixes scalar syntax and the reference, keeping the same tree and correct chomping.
		Files.writeString(legacy.resolve("c8oProject.yaml"), "# Standard YAML\n---\n" + HEADER
				+ "↓Demo [core.Project]:\n  comment: A small service\n"
				+ "  ↓Main [sequences.GenericSequence]: sequences/Main.yaml\n");
		Files.writeString(legacy.resolve("_c8oProject/sequences/Main.yaml"), "responseTimeout: 120\n"
				+ "↓Compute [steps.SimpleStep-123]:\n  expression: |-\n    const result = 1;\n    result++;\n"
				+ "↓value [variables.RequestableVariable-124]:\n  value: ''\n");
		same(expected, YamlConverter.readYaml(legacy.resolve("c8oProject.yaml").toFile()));
		for (boolean jackson : List.of(false, true)) {
			rewrite(legacy, jackson, !jackson);
			same(expected, YamlConverter.readYaml(legacy.resolve("c8oProject.yaml").toFile()));
		}
	}

	@Test public void unchangedSourcesKeepTheirDatesAndOnlyTheChangedSourceIsReplaced() throws Exception {
		Document document = project();
		for (String name : List.of("First", "Second")) {
			Element bean = document.createElement("bean"); bean.setAttribute("yaml_key", name + " [sequences.GenericSequence]");
			bean.setAttribute("yaml_file", "sequences/" + name + ".yaml");
			Element comment = document.createElement("comment"); comment.setTextContent(name); bean.appendChild(comment);
			document.getDocumentElement().getFirstChild().appendChild(bean);
		}
		File yaml = write(document);
		Path directory = yaml.toPath().getParent();
		var date = java.nio.file.attribute.FileTime.fromMillis(1234567890000L);
		List<Path> files;
		try (var paths = Files.walk(directory)) { files = paths.filter(Files::isRegularFile).toList(); }
		for (Path file : files) Files.setLastModifiedTime(file, date);
		YamlConverter.writeYaml(document, yaml, directory.resolve("_c8oProject").toFile());
		for (Path file : files) assertEquals("Unchanged file date", date, Files.getLastModifiedTime(file));
		document.getElementsByTagName("comment").item(0).setTextContent("changed");
		YamlConverter.writeYaml(document, yaml, directory.resolve("_c8oProject").toFile());
		for (Path file : files) {
			if (file.getFileName().toString().equals("First.yaml")) assertNotEquals(date, Files.getLastModifiedTime(file));
			else assertEquals("Unrelated file date", date, Files.getLastModifiedTime(file));
			assertFalse("No trailing whitespace", Files.readString(file).lines().anyMatch(line -> line.endsWith(" ") || line.endsWith("\t")));
		}
		Path second = directory.resolve("_c8oProject/sequences/Second.yaml");
		Files.writeString(second, "comment: external edit\n"); Files.setLastModifiedTime(second, date);
		YamlConverter.writeYaml(document, yaml, directory.resolve("_c8oProject").toFile());
		assertTrue("External changes are never mistaken for an unchanged source", Files.readString(second).contains("Second"));
	}

	@Test public void everyRepositoryProjectRoundTripsInBothFormatsAndThroughBothLibraries() throws Exception {
		Path repository = Path.of(System.getProperty("yaml.repository", "..")).toAbsolutePath().normalize();
		List<Path> corpus = new java.util.ArrayList<>();
		try (var files = Files.walk(repository)) {
			corpus.addAll(files.filter(p -> p.getFileName().toString().equals("c8oProject.yaml"))
					.filter(p -> !p.toString().contains(File.separator + "build" + File.separator))
					.filter(p -> !p.toString().contains(File.separator + "node_modules" + File.separator)).toList());
		}
		assertTrue("The repository project corpus must not be empty", corpus.size() >= 2);
		for (String property : List.of("yaml.templateCorpus", "yaml.sampleCorpus")) {
			String extra = System.getProperty(property);
			if (extra != null) {
				try (var files = Files.walk(Path.of(extra))) {
					corpus.addAll(files.filter(p -> p.getFileName().toString().equals("c8oProject.yaml")).toList());
				}
			}
		}
		System.out.println("Project YAML corpus: " + corpus.size() + " projects");
		for (Path source : corpus) {
			Document expected = YamlConverter.readYaml(source.toFile());
			if (YamlConverter.getFormat(expected) == Format.LEGACY) {
				// Adapt the read DOM to the legacy writer's historical header representation.
				Document writable = (Document) expected.cloneNode(true);
				Element header = writable.createElement("bean"); header.setAttribute("yaml_attr", "convertigo");
				header.setTextContent(expected.getDocumentElement().getAttribute("convertigo"));
				writable.getDocumentElement().insertBefore(header, writable.getDocumentElement().getFirstChild());
				File legacy = new File(folder.newFolder(), "c8oProject.yaml");
				YamlConverter.writeYaml(writable, legacy, new File(legacy.getParentFile(), "_c8oProject"));
				same(expected, YamlConverter.readYaml(legacy));
			}
			expected.getDocumentElement().setAttribute("convertigo", YamlConverter.minimumVersion(
					expected.getDocumentElement().getAttribute("convertigo"), Format.STANDARD));
			YamlConverter.setFormat(expected, Format.STANDARD);
			File standard = write(expected); same(expected, YamlConverter.readYaml(standard));
			for (boolean jackson : List.of(false, true)) {
				rewrite(standard.toPath().getParent(), jackson, !jackson);
				same(expected, YamlConverter.readYaml(standard));
			}
		}
	}
}
