/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Present;
import org.snakeyaml.engine.v2.api.lowlevel.Serialize;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.resolver.JsonScalarResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.events.CollectionStartEvent;
import org.yaml.snakeyaml.events.Event.ID;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.ScalarEvent;

/**
 * Standard YAML syntax with the legacy project's ordered bean/property/XML shape.
 * Scalar lexemes become DOM strings, without implicit numeric or boolean coercion.
 * SnakeYAML 2.6 supplies syntax events (including the standard \L and \P escapes,
 * unsupported by SnakeYAML Engine 3.2). Its YAML 1.1 object resolver is never used:
 * scalar types follow the YAML 1.2 JSON schema. Engine 3.2 emits the YAML nodes.
 * Parsing consumes one complete document, rejects duplicate keys, aliases and custom tags,
 * and never constructs application classes or returns a partially read DOM.
 */
final class StandardYamlConverter {
	private static final int MAX_DEPTH = 256;
	private static final String FILE_DATA = "convertigo.yaml.file";
	private static final org.w3c.dom.UserDataHandler COPY_SOURCE = new org.w3c.dom.UserDataHandler() {
		@Override public void handle(short operation, String key, Object data, Node source, Node destination) {
			if (destination != null) destination.setUserData(key, data, this);
		}
	};
	private static LoaderOptions loadOptions() {
		var options = new LoaderOptions();
		options.setCodePointLimit(32 * 1024 * 1024);
		options.setNestingDepthLimit(MAX_DEPTH);
		return options;
	}
	private static final DumpSettings DUMP = DumpSettings.builder().setBestLineBreak("\n").setSplitLines(false)
			.setIndent(2).build();
	private static final org.yaml.snakeyaml.resolver.Resolver YAML_11 = new org.yaml.snakeyaml.resolver.Resolver();
	private static final JsonScalarResolver RESOLVER = new JsonScalarResolver();

	private StandardYamlConverter() { }

	/** A bounded event reader: no YAML constructors, alias expansion or implicit scalar coercion. */
	private static final class Reader {
		private final Iterator<Event> events;
		private Event event;

		Reader(Iterator<Event> events) throws IOException { this.events = events; advance(); }
		void advance() throws IOException {
			if (!events.hasNext()) throw error("Unexpected end of YAML stream");
			event = events.next();
		}
		void take(ID id) throws IOException {
			if (event.getEventId() != id) throw error("Expected " + id);
			advance();
		}
		IOException error(String message) {
			var mark = event.getStartMark();
			return new IOException(message + (mark == null ? "" : " at line " + (mark.getLine() + 1)
					+ ", column " + (mark.getColumn() + 1)));
		}
		org.snakeyaml.engine.v2.nodes.Node node(int depth) throws IOException {
			if (depth > MAX_DEPTH) throw error("YAML nesting exceeds " + MAX_DEPTH);
			if (event instanceof ScalarEvent scalar) {
				String tag = scalar.getTag() == null ? RESOLVER.resolve(scalar.getValue(), scalar.isPlain()).getValue() : scalar.getTag();
				if (!Set.of(Tag.STR.getValue(), Tag.BOOL.getValue(), Tag.INT.getValue(), Tag.FLOAT.getValue(), Tag.NULL.getValue()).contains(tag)) {
					throw error("Unsupported scalar tag");
				}
				var value = new ScalarNode(Tag.STR, scalar.getValue(), ScalarStyle.PLAIN);
				advance();
				return value;
			}
			if (event instanceof CollectionStartEvent collection) {
				boolean mapping = event.getEventId() == ID.MappingStart;
				Tag tag = mapping ? Tag.MAP : Tag.SEQ;
				if (collection.getTag() != null && !tag.getValue().equals(collection.getTag())) {
					throw error("Unsupported collection tag");
				}
				advance();
				if (mapping) {
					var map = mapping();
					var keys = new HashSet<String>();
					while (event.getEventId() != ID.MappingEnd) {
						String key = string(node(depth + 1));
						if (!keys.add(key)) throw error("Duplicate mapping key");
						put(map, key, node(depth + 1));
					}
					advance();
					return map;
				}
				var list = new ArrayList<org.snakeyaml.engine.v2.nodes.Node>();
				while (event.getEventId() != ID.SequenceEnd) list.add(node(depth + 1));
				advance();
				return sequence(list);
			}
			throw error("Expected a scalar, mapping or sequence (aliases are not supported)");
		}
	}

	static Map<String, org.snakeyaml.engine.v2.nodes.Node> load(Path file) throws IOException {
		try (var stream = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			var reader = new Reader(new Yaml(loadOptions()).parse(stream).iterator());
			reader.take(ID.StreamStart);
			reader.take(ID.DocumentStart);
			var root = fields(reader.node(0));
			reader.take(ID.DocumentEnd);
			if (reader.event.getEventId() != ID.StreamEnd) throw reader.error("Exactly one YAML document is required");
			return root;
		} catch (MarkedYAMLException e) {
			// Parser snippets can contain passwords or script literals. Report coordinates, never the input.
			var mark = e.getProblemMark();
			String location = mark == null ? "" : " at line " + (mark.getLine() + 1) + ", column " + (mark.getColumn() + 1);
			throw new IOException("Invalid standard YAML in " + file.getFileName() + location);
		} catch (YAMLException e) {
			throw new IOException("Invalid standard YAML in " + file.getFileName());
		} catch (IOException e) {
			throw new IOException("Cannot read standard YAML " + file.getFileName() + ": " + e.getMessage());
		}
	}

	@FunctionalInterface
	interface SourceReader {
		Map<String, org.snakeyaml.engine.v2.nodes.Node> read(Path path) throws IOException;
	}

	static Document readYaml(File yaml, boolean onlyRoot) throws Exception {
		return readYaml(yaml, onlyRoot, StandardYamlConverter::load);
	}

	static Document readYaml(File yaml, boolean onlyRoot, SourceReader sourceReader) throws Exception {
		var root = sourceReader.read(yaml.toPath());
		String version = string(root.remove("convertigo"));
		if (!version.matches("\\d+\\.\\d+\\.\\d+\\.m\\d+")) throw new IOException("Invalid minimum engine version in standard YAML");
		if (!version.equals(YamlConverter.minimumVersion(version, YamlConverter.Format.STANDARD))) {
			throw new IOException("Standard YAML requires engine 8.5.0 or later");
		}
		Document document = XMLUtils.createDom();
		String namespace = root.containsKey("↑") ? string(root.remove("↑")) : null;
		Element element = namespace == null ? document.createElement("convertigo") : document.createElementNS(namespace, "convertigo");
		document.appendChild(element);
		element.setAttribute("convertigo", version);
		var directory = yaml.toPath().toAbsolutePath().getParent().resolve("_c8oProject");
		var context = new ReadContext(document, directory, onlyRoot, sourceReader);
		for (var entry : root.entrySet()) context.entry(element, entry.getKey(), entry.getValue(), 0);
		int projects = 0;
		for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element bean && bean.getAttribute("yaml_key").endsWith(" [core.Project]")) projects++;
			else if (node instanceof Element) throw new IOException("Expected a project bean at the document root");
		}
		if (projects != 1) throw new IOException("Expected exactly one project bean");
		return document;
	}

	private static final class ReadContext {
		final Document document;
		final Path directory;
		final boolean onlyRoot;
		final SourceReader reader;
		final Set<Path> active = new HashSet<>();

		ReadContext(Document document, Path directory, boolean onlyRoot, SourceReader reader) {
			this.document = document; this.directory = directory; this.onlyRoot = onlyRoot; this.reader = reader;
		}

		void entry(Element parent, String key, org.snakeyaml.engine.v2.nodes.Node value, int depth) throws IOException {
			if (depth > MAX_DEPTH) throw new IOException("Project nesting exceeds " + MAX_DEPTH);
			try {
				if (key.equals("↑")) {
					if (!string(value).equals(parent.getNamespaceURI())) throw new IOException("Invalid element namespace");
				} else if (key.startsWith("↑")) {
					String name = key.substring(1);
					if (parent.hasAttribute(name)) throw new IOException("Duplicate XML attribute");
					if (value instanceof MappingNode) {
						var qualified = fields(value);
						keys(qualified, "↑", "→→");
						parent.setAttributeNS(string(qualified.get("↑")), name, string(qualified.get("→→")));
					} else parent.setAttribute(name, string(value));
				} else if (key.equals("→")) {
					parent.appendChild(document.createCDATASection(string(value)));
				} else if (key.equals("→→")) {
					parent.appendChild(document.createTextNode(string(value)));
				} else if (key.equals("→#")) {
					parent.appendChild(document.createComment(string(value)));
				} else if (key.startsWith("→?")) {
					parent.appendChild(document.createProcessingInstruction(key.substring(2), string(value)));
				} else {
					boolean bean = key.startsWith("↓");
					if (key.equals("🗏") || key.startsWith("→")) throw new IOException("Unexpected reserved YAML key");
					String namespace = namespace(value);
					Element child = namespace == null ? document.createElement(bean ? "bean" : key)
							: document.createElementNS(namespace, bean ? "bean" : key);
					if (bean) {
						String identity = key.substring(1);
						if (!identity.matches(".+ \\[.+\\]")) throw new IOException("Invalid bean identity");
						child.setAttribute("yaml_key", identity);
					}
					parent.appendChild(child);
					content(child, value, bean, depth + 1);
				}
			} catch (org.w3c.dom.DOMException e) { throw new IOException("Invalid XML node name or namespace"); }
		}

		String namespace(org.snakeyaml.engine.v2.nodes.Node value) throws IOException {
			if (value instanceof MappingNode) {
				var entries = fields(value);
				return entries.containsKey("↑") ? string(entries.get("↑")) : null;
			}
			String namespace = null;
			if (value instanceof SequenceNode sequence) {
				for (var item : sequence.getValue()) {
					var entries = fields(item);
					if (entries.containsKey("↑")) {
						if (namespace != null) throw new IOException("Duplicate element namespace");
						namespace = string(entries.get("↑"));
					}
				}
			}
			return namespace;
		}

		void content(Element element, org.snakeyaml.engine.v2.nodes.Node value, boolean bean, int depth) throws IOException {
			if (value instanceof ScalarNode) {
				String text = string(value);
				if (bean && !text.isEmpty()) {
					Path file = source(directory, text);
					element.setUserData(FILE_DATA, text, COPY_SOURCE);
					if (onlyRoot) return;
					if (!active.add(file)) throw new IOException("Cyclic YAML source reference");
					try {
						var included = reader.read(file);
						for (var entry : included.entrySet()) entry(element, entry.getKey(), entry.getValue(), depth + 1);
					} finally { active.remove(file); }
				} else element.setTextContent(text);
			} else if (value instanceof MappingNode) {
				for (var entry : fields(value).entrySet()) entry(element, entry.getKey(), entry.getValue(), depth + 1);
			} else if (value instanceof SequenceNode sequence) {
				if (bean) throw new IOException("Bean properties must be a mapping");
				for (var item : sequence.getValue()) {
					var entries = fields(item);
					if (entries.size() != 1) throw new IOException("XML content requires single-key mappings");
					var entry = entries.entrySet().iterator().next();
					entry(element, entry.getKey(), entry.getValue(), depth + 1);
				}
			} else throw new IOException("Invalid element content");
		}
	}

	/** References are local to _c8oProject, including when an existing directory is a symlink. */
	private static Path source(Path directory, String reference) throws IOException {
		Path base = directory.toAbsolutePath().normalize();
		Path relative;
		try { relative = Path.of(reference); }
		catch (java.nio.file.InvalidPathException e) { throw new IOException("Invalid YAML source path"); }
		Path path = base.resolve(relative).normalize();
		if (reference.isEmpty() || reference.codePoints().anyMatch(Character::isISOControl)
				|| reference.contains("\\") || relative.isAbsolute()
				|| !path.startsWith(base) || path.equals(base) || !reference.endsWith(".yaml")) {
			throw new IOException("Invalid YAML source path");
		}
		Path ancestor = path;
		while (ancestor != null && !Files.exists(ancestor)) ancestor = ancestor.getParent();
		Path baseAncestor = base;
		while (!Files.exists(baseAncestor)) baseAncestor = baseAncestor.getParent();
		Path realBase = baseAncestor.toRealPath().resolve(baseAncestor.relativize(base));
		if (ancestor != null && !ancestor.toRealPath().resolve(ancestor.relativize(path)).startsWith(realBase)) {
			throw new IOException("YAML source path escapes the project directory");
		}
		return path;
	}

	static void writeYaml(Document document, File yaml, File subdir) throws IOException {
		var root = document.getDocumentElement();
		var data = mapping();
		String version = root.getAttribute("convertigo");
		for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element e && "convertigo".equals(e.getAttribute("yaml_attr"))) version = e.getTextContent();
		}
		if (version.isEmpty()) throw new IOException("Missing minimum engine version");
		put(data, "convertigo", value(YamlConverter.minimumVersion(version, YamlConverter.Format.STANDARD)));
		var outputs = new LinkedHashMap<Path, String>();
		Path directory = subdir == null ? null : subdir.toPath();
		if (root.getNamespaceURI() != null) put(data, "↑", value(root.getNamespaceURI()));
		for (int i = 0; i < root.getAttributes().getLength(); i++) {
			Node attribute = root.getAttributes().item(i);
			if (!attribute.getNodeName().equals("convertigo")) put(data, "↑" + attribute.getNodeName(), attributeValue(attribute));
		}
		for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element e && "convertigo".equals(e.getAttribute("yaml_attr"))) continue;
			var entry = writeNode(child, directory, outputs, true);
			put(data, string(entry.getKeyNode()), entry.getValueNode());
		}
		outputs.put(yaml.toPath().toAbsolutePath().normalize(), emit(data));
		// Finish rendering and validate all references before modifying any source file.
		var existing = subdir == null ? new HashSet<File>() : FileUtils.indexExistingFiles(subdir);
		existing.removeIf(file -> !file.getName().endsWith(".yaml"));
		var backups = new LinkedHashMap<Path, byte[]>();
		for (Path path : outputs.keySet()) backups.put(path, Files.exists(path) ? Files.readAllBytes(path) : null);
		var changed = new ArrayList<Path>();
		try {
			for (var output : outputs.entrySet()) {
				byte[] bytes = output.getValue().getBytes(StandardCharsets.UTF_8);
				if (!java.util.Arrays.equals(bytes, backups.get(output.getKey()))) {
					replace(output.getKey(), bytes);
					changed.add(output.getKey());
				}
				existing.removeIf(file -> file.toPath().toAbsolutePath().normalize().equals(output.getKey()));
			}
		} catch (IOException failure) {
			for (int i = changed.size() - 1; i >= 0; i--) {
				Path path = changed.get(i);
				try {
					byte[] before = backups.get(path);
					if (before == null) Files.deleteIfExists(path); else replace(path, before);
				} catch (IOException rollback) { failure.addSuppressed(rollback); }
			}
			throw failure;
		}
		FileUtils.deleteFiles(existing);
	}

	/** Replace complete files; restore earlier files if a subsequent write fails. */
	private static void replace(Path path, byte[] bytes) throws IOException {
		Files.createDirectories(path.getParent());
		Path temporary = Files.createTempFile(path.getParent(), ".c8o-yaml-", ".tmp");
		try {
			Files.write(temporary, bytes);
			try {
				Files.move(temporary, path, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			} catch (java.nio.file.AtomicMoveNotSupportedException e) {
				Files.move(temporary, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
		} finally { Files.deleteIfExists(temporary); }
	}

	private static NodeTuple writeNode(Node node, Path directory, Map<Path, String> outputs, boolean reference) throws IOException {
		String key;
		org.snakeyaml.engine.v2.nodes.Node value;
		if (node instanceof Element element) {
			boolean bean = element.getTagName().equals("bean") && element.hasAttribute("yaml_key");
			key = bean ? "↓" + element.getAttribute("yaml_key") : element.getTagName();
			String file = element.getAttribute("yaml_file");
			if (file.isEmpty() && element.getUserData(FILE_DATA) instanceof String stored) file = stored;
			if (bean && reference && directory != null && !file.isBlank()) {
				Path path = source(directory, file);
				var content = elementContent(element, directory, outputs, true);
				// A legacy empty source has no entries. A standard source must still be a complete mapping.
				if (content instanceof ScalarNode) content = mapping();
				if (outputs.putIfAbsent(path, emit(content)) != null) throw new IOException("Duplicate YAML source path");
				value = value(file);
			} else value = elementContent(element, directory, outputs, bean);
		} else {
			key = switch (node.getNodeType()) {
				case Node.TEXT_NODE -> "→→";
				case Node.CDATA_SECTION_NODE -> "→";
				case Node.COMMENT_NODE -> "→#";
				case Node.PROCESSING_INSTRUCTION_NODE -> "→?" + node.getNodeName();
				default -> throw new IOException("Unsupported XML node type " + node.getNodeType());
			};
			value = value(node.getNodeValue());
		}
		return new NodeTuple(key(key), value);
	}

	private static org.snakeyaml.engine.v2.nodes.Node elementContent(Element element, Path directory,
			Map<Path, String> outputs, boolean bean) throws IOException {
		var entries = new ArrayList<NodeTuple>();
		if (element.getNamespaceURI() != null) entries.add(new NodeTuple(key("↑"), value(element.getNamespaceURI())));
		for (int i = 0; i < element.getAttributes().getLength(); i++) {
			Node attribute = element.getAttributes().item(i);
			String name = attribute.getNodeName();
			if (!bean || (!name.equals("yaml_key") && !name.equals("yaml_file"))) {
				entries.add(new NodeTuple(key("↑" + name), attributeValue(attribute)));
			}
		}
		Node first = element.getFirstChild();
		if (entries.isEmpty() && first == null) return value("");
		if (!bean && entries.isEmpty() && first != null && first.getNextSibling() == null
				&& first.getNodeType() == Node.TEXT_NODE && !first.getNodeValue().isEmpty()) return value(first.getNodeValue());
		for (Node child = first; child != null; child = child.getNextSibling()) entries.add(writeNode(child, directory, outputs, true));
		if (bean || (entries.size() == 1 && "→".equals(string(entries.get(0).getKeyNode())))) {
			var mapping = mapping();
			for (var entry : entries) put(mapping, string(entry.getKeyNode()), entry.getValueNode());
			return mapping;
		}
		var content = new ArrayList<org.snakeyaml.engine.v2.nodes.Node>();
		for (var entry : entries) content.add(new MappingNode(Tag.MAP, List.of(entry), FlowStyle.BLOCK));
		return sequence(content);
	}

	private static org.snakeyaml.engine.v2.nodes.Node attributeValue(Node attribute) throws IOException {
		if (attribute.getNamespaceURI() == null) return value(attribute.getNodeValue());
		var qualified = new MappingNode(Tag.MAP, new ArrayList<>(),
				literal(attribute.getNamespaceURI()) || literal(attribute.getNodeValue()) ? FlowStyle.BLOCK : FlowStyle.FLOW);
		put(qualified, "↑", value(attribute.getNamespaceURI()));
		put(qualified, "→→", value(attribute.getNodeValue()));
		return qualified;
	}

	private static MappingNode mapping() { return new MappingNode(Tag.MAP, new ArrayList<>(), FlowStyle.BLOCK); }
	private static SequenceNode sequence(List<org.snakeyaml.engine.v2.nodes.Node> nodes) { return new SequenceNode(Tag.SEQ, nodes, FlowStyle.BLOCK); }
	/** Characters that a literal block cannot preserve through all supported YAML readers. */
	private static boolean escaped(String value) {
		return value.codePoints().anyMatch(c -> c == '\r' || c == 0x2028 || c == 0x2029
				|| (c < 0x20 && c != '\t' && c != '\n') || (c >= 0x7f && c < 0xa0)
				|| (c >= 0xd800 && c <= 0xdfff) || c == 0xfffe || c == 0xffff);
	}
	private static boolean literal(String value) { return value.contains("\n") && !escaped(value); }
	private static ScalarNode key(String key) {
		return new ScalarNode(Tag.STR, key, escaped(key) ? ScalarStyle.DOUBLE_QUOTED : ScalarStyle.PLAIN);
	}
	private static ScalarNode value(String value) {
		if (escaped(value)) return new ScalarNode(Tag.STR, value, ScalarStyle.DOUBLE_QUOTED);
		if (value.contains("\n")) return new ScalarNode(Tag.STR, value, ScalarStyle.LITERAL);
		if (value.equals("true") || value.equals("false")) return new ScalarNode(Tag.BOOL, value, ScalarStyle.PLAIN);
		if (value.matches("0|-?[1-9][0-9]*")) return new ScalarNode(Tag.INT, value, ScalarStyle.PLAIN);
		boolean ambiguous = !org.yaml.snakeyaml.nodes.Tag.STR.equals(YAML_11.resolve(org.yaml.snakeyaml.nodes.NodeId.scalar, value, true));
		return new ScalarNode(Tag.STR, value, ambiguous || value.isEmpty() ? ScalarStyle.SINGLE_QUOTED : ScalarStyle.PLAIN);
	}
	private static void put(MappingNode map, String key, org.snakeyaml.engine.v2.nodes.Node value) throws IOException {
		for (var existing : map.getValue()) if (key.equals(string(existing.getKeyNode()))) throw new IOException("Duplicate mapping key");
		map.getValue().add(new NodeTuple(key(key), value));
	}
	private static String emit(org.snakeyaml.engine.v2.nodes.Node node) throws IOException {
		var events = new Serialize(DUMP).serializeOne(node);
		// Engine's scalar analyser refuses tabs and trailing spaces in literal blocks.
		// Emit unique plain placeholders, then replace their parser-delimited ranges with
		// literal blocks. The scalar data and the surrounding YAML syntax stay independent.
		String marker = "C8O_LITERAL_";
		while (contains(events, marker)) marker += "_";
		var literals = new LinkedHashMap<String, String>();
		for (int i = 0; i < events.size(); i++) {
			if (events.get(i) instanceof org.snakeyaml.engine.v2.events.ScalarEvent scalar
					&& scalar.getScalarStyle() == ScalarStyle.LITERAL) {
				String token = marker + literals.size();
				literals.put(token, scalar.getValue());
				events.set(i, new org.snakeyaml.engine.v2.events.ScalarEvent(scalar.getAnchor(), scalar.getTag(),
						scalar.getImplicit(), token, ScalarStyle.PLAIN));
			}
		}
		// These separators only occur in double-quoted scalars: escape them for YAML 1.1 tools too.
		String yaml = new Present(DUMP).emitToString(events.iterator())
				.replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
		if (hasSequence(node)) yaml = indentXmlSequences(yaml);
		if (!literals.isEmpty()) yaml = insertLiterals(yaml, literals);
		return !lastLiteralEndsWithBreak(node) && yaml.endsWith("\n") ? yaml.substring(0, yaml.length() - 1) : yaml;
	}

	private record Replacement(int start, int end, String text) { }

	private static String insertLiterals(String yaml, Map<String, String> literals) throws IOException {
		var replacements = new ArrayList<Replacement>();
		var indents = new java.util.ArrayDeque<Integer>();
		int previousIndex = 0, previousOffset = 0;
		try {
			for (var event : new Yaml(loadOptions()).parse(new java.io.StringReader(yaml))) {
				if (event.getEventId() == ID.MappingStart) indents.push(event.getStartMark().getColumn());
				else if (event.getEventId() == ID.MappingEnd) indents.pop();
				else if (event instanceof ScalarEvent scalar && literals.containsKey(scalar.getValue())) {
					// Parser indexes count Unicode code points, whereas StringBuilder uses UTF-16 offsets.
					int startIndex = scalar.getStartMark().getIndex(), endIndex = scalar.getEndMark().getIndex();
					int start = yaml.offsetByCodePoints(previousOffset, startIndex - previousIndex);
					int end = yaml.offsetByCodePoints(start, endIndex - startIndex);
					previousIndex = endIndex; previousOffset = end;
					replacements.add(new Replacement(start, end,
							literalBlock(literals.get(scalar.getValue()), indents.peek() + 2)));
				}
			}
		} catch (YAMLException e) { throw new IOException("Unable to present literal YAML values"); }
		if (replacements.size() != literals.size()) throw new IOException("Missing literal YAML value");
		var result = new StringBuilder(yaml);
		for (int i = replacements.size() - 1; i >= 0; i--) {
			var replacement = replacements.get(i);
			result.replace(replacement.start(), replacement.end(), replacement.text());
		}
		return result.toString();
	}

	/** One physical source line per value line, with exact whitespace and LF chomping. */
	private static String literalBlock(String value, int indent) {
		boolean endsWithBreak = value.endsWith("\n");
		String chomp = !endsWithBreak ? "-" : value.equals("\n") || value.endsWith("\n\n") ? "+" : "";
		var block = new StringBuilder("|");
		if (value.startsWith(" ") || value.startsWith("\n")) block.append('2');
		block.append(chomp).append('\n');
		String[] lines = value.split("\n", -1);
		int count = lines.length - (endsWithBreak ? 1 : 0);
		String padding = " ".repeat(indent);
		for (int i = 0; i < count; i++) {
			if (!lines[i].isEmpty()) block.append(padding).append(lines[i]);
			block.append('\n');
		}
		// The placeholder's existing line break terminates the last physical content line.
		return block.substring(0, block.length() - 1);
	}

	private static boolean contains(List<org.snakeyaml.engine.v2.events.Event> events, String text) {
		for (var event : events) if (event instanceof org.snakeyaml.engine.v2.events.ScalarEvent scalar
				&& scalar.getValue().contains(text)) return true;
		return false;
	}

	private static boolean hasSequence(org.snakeyaml.engine.v2.nodes.Node node) {
		if (node instanceof SequenceNode) return true;
		if (node instanceof MappingNode mapping) {
			for (var entry : mapping.getValue()) if (hasSequence(entry.getValueNode())) return true;
		}
		return false;
	}

	/** Keep the legacy XML indentation: indent the outer sequence, not each nested sequence. */
	private static String indentXmlSequences(String yaml) throws IOException {
		String[] lines = yaml.split("\n", -1);
		int depth = 0, start = 0;
		try {
			for (var event : new Yaml(loadOptions()).parse(new java.io.StringReader(yaml))) {
				if (event.getEventId() == ID.SequenceStart) {
					if (depth++ == 0) start = event.getStartMark().getLine();
				} else if (event.getEventId() == ID.SequenceEnd && --depth == 0) {
					int end = Math.min(lines.length, event.getStartMark().getLine());
					for (int i = start; i < end; i++) if (!lines[i].isEmpty()) lines[i] = "  " + lines[i];
				}
			}
		} catch (YAMLException e) { throw new IOException("Unable to format XML sequences as standard YAML"); }
		return String.join("\n", lines);
	}

	private static boolean lastLiteralEndsWithBreak(org.snakeyaml.engine.v2.nodes.Node node) {
		if (node instanceof ScalarNode scalar) return scalar.getScalarStyle() == ScalarStyle.LITERAL && scalar.getValue().endsWith("\n");
		if (node instanceof MappingNode mapping && !mapping.getValue().isEmpty()) return lastLiteralEndsWithBreak(mapping.getValue().get(mapping.getValue().size() - 1).getValueNode());
		if (node instanceof SequenceNode sequence && !sequence.getValue().isEmpty()) return lastLiteralEndsWithBreak(sequence.getValue().get(sequence.getValue().size() - 1));
		return false;
	}

	private static String string(org.snakeyaml.engine.v2.nodes.Node node) throws IOException {
		if (node instanceof ScalarNode scalar && Tag.STR.equals(node.getTag())) return scalar.getValue();
		throw new IOException("Expected a string value");
	}
	private static Map<String, org.snakeyaml.engine.v2.nodes.Node> fields(org.snakeyaml.engine.v2.nodes.Node node) throws IOException {
		if (!(node instanceof MappingNode mapping)) throw new IOException("Expected a mapping");
		var fields = new LinkedHashMap<String, org.snakeyaml.engine.v2.nodes.Node>();
		for (var entry : mapping.getValue()) fields.put(string(entry.getKeyNode()), entry.getValueNode());
		return fields;
	}
	private static void keys(Map<String, ?> fields, String... allowed) throws IOException {
		if (!Set.of(allowed).containsAll(fields.keySet())) throw new IOException("Unknown field in standard YAML schema");
	}
}
