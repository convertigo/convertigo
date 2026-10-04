/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.tags;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** The portable, versioned tag source. No database-object properties are serialized here. */
public final class TagDocument {
	public static final int MAX_BYTES = 2 * 1024 * 1024;
	public static final ObjectMapper JSON = new ObjectMapper();
	static {
		JSON.getFactory().setStreamReadConstraints(com.fasterxml.jackson.core.StreamReadConstraints.builder()
				.maxNestingDepth(64).maxStringLength(65_536).build());
	}
	final boolean workspace;
	final TreeMap<String, ObjectNode> tags = new TreeMap<>();
	// Membership order is source data, independent of tag-folder presentation sorting.
	final TreeMap<String, LinkedHashSet<String>> assignments = new TreeMap<>();
	final TreeMap<String, ObjectNode> projectTags = new TreeMap<>();
	private ObjectNode extensions = JSON.createObjectNode();

	TagDocument(boolean workspace) { this.workspace = workspace; }

	public static ObjectNode parseObject(String value) throws IOException {
		return parse(value.getBytes(StandardCharsets.UTF_8));
	}

	private static ObjectNode parse(byte[] bytes) throws IOException {
		if (bytes.length > MAX_BYTES) throw new IOException("Tag document exceeds 2 MiB");
		String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
		try (var parser = JSON.getFactory().createParser(text)) {
			parser.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
			JsonNode root = JSON.readTree(parser);
			if (!(root instanceof ObjectNode object) || parser.nextToken() != null)
				throw new IOException("Expected one strict JSON object");
			return object;
		}
	}

	static TagDocument read(Path path, boolean workspace) throws IOException {
		if (!Files.exists(path)) return new TagDocument(workspace);
		if (Files.size(path) > MAX_BYTES) throw new IOException("Tag document exceeds 2 MiB");
		return from(parse(Files.readAllBytes(path)), workspace);
	}

	static TagDocument from(ObjectNode root, boolean workspace) throws IOException {
		if (!root.path("schemaVersion").isIntegralNumber() || root.path("schemaVersion").intValue() != 1)
			throw new IOException("Unsupported tag schemaVersion");
		TagDocument document = new TagDocument(workspace);
		document.extensions = root.deepCopy();
		document.extensions.remove(Set.of("schemaVersion", "tags", "assignments", "projectTags"));
		readDefinitions(root.path("tags"), document.tags, workspace);
		if (!workspace) readDefinitions(root.path("projectTags"), document.projectTags, false);
		JsonNode assignments = root.path("assignments");
		if (!assignments.isObject()) throw new IOException("assignments must be an object");
		var fields = assignments.fields();
		while (fields.hasNext()) {
			var field = fields.next();
			validateTarget(field.getKey());
			if (!field.getValue().isArray()) throw new IOException("Tag membership must be an array");
			LinkedHashSet<String> ids = new LinkedHashSet<>();
			for (var value : field.getValue()) {
				if (!value.isTextual()) throw new IOException("Tag ID must be a string");
				validateId(value.asText());
				if (!document.tags.containsKey(value.asText())) throw new IOException("Unknown tag ID: " + value.asText());
				ids.add(value.asText());
			}
			if (!ids.isEmpty()) document.assignments.put(field.getKey(), ids);
		}
		if (document.assignments.size() > 100_000) throw new IOException("Too many tagged targets");
		return document;
	}

	private static void readDefinitions(JsonNode node, Map<String, ObjectNode> target, boolean workspace) throws IOException {
		if (!node.isObject()) throw new IOException("Tag definitions must be an object");
		if (node.size() > 5_000) throw new IOException("Too many tag definitions");
		var fields = node.fields();
		while (fields.hasNext()) {
			var field = fields.next();
			validateId(field.getKey());
			if (!(field.getValue() instanceof ObjectNode definition)) throw new IOException("Invalid tag definition");
			validateDefinition(definition, workspace);
			target.put(field.getKey(), definition.deepCopy());
		}
	}

	public static void validateId(String id) throws IOException {
		try {
			UUID uuid = UUID.fromString(id);
			if (!uuid.toString().equals(id) || uuid.version() != 4 || uuid.variant() != 2) throw new IllegalArgumentException();
		} catch (RuntimeException e) { throw new IOException("Expected a canonical UUID v4 tag ID"); }
	}

	static void validateTarget(String target) throws IOException {
		if (target == null || target.isBlank() || target.length() > 2048 || target.contains("/") || target.contains("\\")
				|| target.chars().anyMatch(Character::isISOControl)) throw new IOException("Invalid tag target");
	}

	static void validateDefinition(ObjectNode definition, boolean workspace) throws IOException {
		JsonNode label = definition.path("label");
		if (!label.isTextual() || label.asText().isBlank() || label.asText().length() > 256
				|| label.asText().chars().anyMatch(Character::isISOControl)) throw new IOException("Tag label must contain 1–256 visible characters");
		if (definition.has("description") && (!definition.path("description").isTextual()
				|| definition.path("description").asText().length() > 4096)) throw new IOException("Invalid tag description");
		if (definition.has("presentation")) {
			JsonNode presentation = definition.path("presentation");
			if (!presentation.isObject()) throw new IOException("Invalid tag presentation");
			if (presentation.has("color") && (!presentation.path("color").isTextual()
					|| !presentation.path("color").asText().matches("#[0-9a-fA-F]{6}"))) throw new IOException("Expected a #RRGGBB color");
		}
		if (definition.has("metadata") && !definition.path("metadata").isObject()) throw new IOException("metadata must be an object");
		if (definition.has("shared") && (!workspace || !definition.path("shared").isBoolean())) throw new IOException("shared belongs to workspace project tags");
		if (JSON.writeValueAsBytes(definition).length > 65_536) throw new IOException("Tag definition exceeds 64 KiB");
	}

	void choosePresentation(String id, ObjectNode definition) throws IOException {
		extensions.withObject("presentationChoices").put(id, fingerprint(JSON.writeValueAsBytes(sorted(definition))));
	}
	boolean presentationChosen(String id, ObjectNode definition) throws IOException {
		return extensions.path("presentationChoices").path(id).asText().equals(fingerprint(JSON.writeValueAsBytes(sorted(definition))));
	}
	TagDocument copy() {
		TagDocument copy = new TagDocument(workspace);
		copy.extensions = extensions.deepCopy();
		tags.forEach((id, definition) -> copy.tags.put(id, definition.deepCopy()));
		projectTags.forEach((id, definition) -> copy.projectTags.put(id, definition.deepCopy()));
		assignments.forEach((target, ids) -> copy.assignments.put(target, new LinkedHashSet<>(ids)));
		return copy;
	}

	ObjectNode object() {
		ObjectNode root = extensions.deepCopy();
		root.put("schemaVersion", 1);
		ObjectNode definitions = root.putObject("tags");
		tags.forEach((id, definition) -> definitions.set(id, sorted(definition)));
		ObjectNode members = root.putObject("assignments");
		assignments.forEach((target, ids) -> { var array = members.putArray(target); ids.forEach(array::add); });
		if (!workspace) {
			ObjectNode portable = root.putObject("projectTags");
			projectTags.forEach((id, definition) -> portable.set(id, sorted(definition)));
		}
		return root;
	}

	private static JsonNode sorted(JsonNode node) {
		if (node.isObject()) {
			ObjectNode result = JSON.createObjectNode();
			TreeMap<String, JsonNode> fields = new TreeMap<>();
			node.fields().forEachRemaining(field -> fields.put(field.getKey(), field.getValue()));
			fields.forEach((key, value) -> result.set(key, sorted(value)));
			return result;
		}
		if (node.isArray()) { var array = JSON.createArrayNode(); node.forEach(value -> array.add(sorted(value))); return array; }
		return node.deepCopy();
	}

	byte[] bytes() throws IOException {
		// Enforce the same limits for commands and clipboard imports as for source reads.
		from(object(), workspace);
		var pretty = new DefaultPrettyPrinter();
		pretty.indentObjectsWith(new DefaultIndenter("  ", "\n"));
		pretty.indentArraysWith(new DefaultIndenter("  ", "\n"));
		byte[] bytes = (JSON.writer(pretty).writeValueAsString(object()) + "\n").getBytes(StandardCharsets.UTF_8);
		if (bytes.length > MAX_BYTES) throw new IOException("Tag document exceeds 2 MiB");
		return bytes;
	}

	static String fingerprint(Path path) throws IOException {
		if (!Files.exists(path)) return "absent";
		if (Files.size(path) > MAX_BYTES) throw new IOException("Tag document exceeds 2 MiB");
		return fingerprint(Files.readAllBytes(path));
	}

	static String fingerprint(byte[] bytes) {
		try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
		catch (Exception e) { throw new IllegalStateException(e); }
	}

	static void write(Path path, byte[] bytes, String expectedFingerprint) throws IOException {
		if (!fingerprint(path).equals(expectedFingerprint)) throw new IOException("Tag source changed externally; reload before saving");
		if (Files.exists(path) && java.util.Arrays.equals(Files.readAllBytes(path), bytes)) return;
		Files.createDirectories(path.getParent());
		Path temporary = Files.createTempFile(path.getParent(), ".tags-", ".tmp");
		try {
			Files.write(temporary, bytes);
			if (!fingerprint(path).equals(expectedFingerprint)) throw new IOException("Concurrent tag source update");
			Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} finally { Files.deleteIfExists(temporary); }
	}
}
