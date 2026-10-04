/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.tags;

import java.io.IOException;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import com.twinsoft.convertigo.beans.core.Project;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Declarative, data-only metadata contributions shared by the two Studios. */
public final class TagContributions {
	private final TreeMap<String, ObjectNode> descriptors = new TreeMap<>();
	private final TreeMap<String, Provider> providers = new TreeMap<>();

	public record Context(TagManager.Scope scope, Project project) { }
	@FunctionalInterface public interface Provider {
		/** Return null when the extension is not available in this context. */
		ObjectNode describe(Context context) throws IOException;
	}

	public synchronized void register(String namespace, Provider provider) throws IOException {
		validateNamespace(namespace);
		if (provider == null) throw new IOException("Missing contribution provider");
		providers.put(namespace, provider);
	}

	private void validateNamespace(String namespace) throws IOException {
		if (namespace == null || !namespace.matches("[A-Za-z][A-Za-z0-9_.-]{0,127}")
				|| descriptors.containsKey(namespace) || providers.containsKey(namespace))
			throw new IOException("Invalid or duplicate metadata namespace");
	}

	public synchronized void register(String namespace, ObjectNode descriptor) throws IOException {
		validateNamespace(namespace);
		validateDescriptor(descriptor);
		descriptors.put(namespace, descriptor.deepCopy());
	}

	private static void validateDescriptor(ObjectNode descriptor) throws IOException {
		if (!descriptor.path("label").isTextual() || !descriptor.path("fields").isObject()) throw new IOException("Invalid contribution descriptor");
		if (descriptor.toString().length() > 65536 || descriptor.path("fields").size() > 128) throw new IOException("Contribution descriptor is too large");
		var fields = descriptor.path("fields").fields();
		while (fields.hasNext()) {
			var field = fields.next();
			if (!field.getKey().matches("[A-Za-z][A-Za-z0-9_]{0,127}") || !field.getValue().path("label").isTextual()
					|| !Set.of("string", "boolean", "integer", "number", "array").contains(field.getValue().path("type").asText()))
				throw new IOException("Unsupported contribution field");
			if (field.getValue().has("required") && !field.getValue().path("required").isBoolean()) throw new IOException("Invalid required flag");
			if (field.getValue().has("enum")) {
				JsonNode choices = field.getValue().path("enum");
				if (!field.getValue().path("type").asText().equals("string") || !choices.isArray() || choices.isEmpty() || choices.size() > 128)
					throw new IOException("Enum fields require a nonempty string choice list");
				validateChoices(choices);
			}
			if (field.getValue().path("type").asText().equals("array")) {
				JsonNode items = field.getValue().path("items");
				if (!items.path("type").asText().equals("string") || !items.path("enum").isArray()
						|| !field.getValue().path("uniqueItems").asBoolean())
					throw new IOException("Array contributions require unique string references with a choice list");
				validateChoices(items.path("enum"));
			}
		}
	}

	private static void validateChoices(JsonNode choices) throws IOException {
		if (choices.size() > 1000) throw new IOException("Too many contribution choices");
		Set<String> unique = new java.util.HashSet<>();
		for (var choice : choices) if (!choice.isTextual() || choice.asText().isEmpty()
				|| choice.asText().length() > 4096 || !unique.add(choice.asText())) throw new IOException("Invalid enum choice");
	}

	public synchronized ObjectNode descriptors() {
		ObjectNode result = TagDocument.JSON.createObjectNode();
		descriptors.forEach((key, value) -> result.set(key, value.deepCopy()));
		return result;
	}

	/** Providers run without this monitor or the tag-domain monitor (no extension lock inversion). */
	ObjectNode descriptors(Context context) throws IOException {
		ObjectNode result;
		Map<String, Provider> available;
		synchronized (this) { result = descriptors(); available = new TreeMap<>(providers); }
		for (var entry : available.entrySet()) {
			ObjectNode descriptor = entry.getValue().describe(context);
			if (descriptor != null) { validateDescriptor(descriptor); result.set(entry.getKey(), descriptor.deepCopy()); }
		}
		return result;
	}

	/** Unknown namespaces are preserved, but an absent extension cannot edit them. */
	synchronized void validateEdit(ObjectNode previous, ObjectNode next) throws IOException {
		validateEdit(previous, next, descriptors());
	}

	void validateEdit(ObjectNode previous, ObjectNode next, ObjectNode available) throws IOException {
		var namespaces = next.fields();
		while (namespaces.hasNext()) {
			var namespace = namespaces.next();
			ObjectNode descriptor = available.path(namespace.getKey()) instanceof ObjectNode object ? object : null;
			if (descriptor == null) {
				if (!namespace.getValue().equals(previous.path(namespace.getKey()))) throw new IOException("Metadata contribution unavailable: " + namespace.getKey());
				continue;
			}
			if (!namespace.getValue().isObject()) throw new IOException("Contribution values must be an object");
			var fields = descriptor.path("fields").fields();
			while (fields.hasNext()) {
				var field = fields.next();
				JsonNode value = namespace.getValue().path(field.getKey()), schema = field.getValue();
				if (value.isMissingNode() && !schema.path("required").asBoolean()) continue;
				boolean valid = switch (schema.path("type").asText()) {
					case "string" -> value.isTextual() && value.asText().length() <= 4096;
					case "boolean" -> value.isBoolean();
					case "integer" -> value.isIntegralNumber();
					case "number" -> value.isNumber() && Double.isFinite(value.asDouble());
					case "array" -> validReferences(value, previous.path(namespace.getKey()).path(field.getKey()), schema);
					default -> false;
				};
				if (schema.has("enum")) { boolean found = false; for (var choice : schema.path("enum")) found |= choice.equals(value); valid &= found; }
				if (!valid) throw new IOException("Invalid metadata field: " + namespace.getKey() + "." + field.getKey());
			}
			var values = namespace.getValue().fields();
			while (values.hasNext()) {
				var field = values.next();
				if (!descriptor.path("fields").has(field.getKey()) && !field.getValue().equals(previous.path(namespace.getKey()).path(field.getKey())))
					throw new IOException("Unknown metadata field: " + field.getKey());
			}
		}
		var old = previous.fields();
		while (old.hasNext()) {
			var namespace = old.next();
			ObjectNode descriptor = available.path(namespace.getKey()) instanceof ObjectNode object ? object : null;
			if (descriptor == null && !next.has(namespace.getKey())) throw new IOException("Unavailable metadata must be preserved");
			if (descriptor != null && namespace.getValue().isObject()) {
				var fields = namespace.getValue().fields();
				while (fields.hasNext()) {
					var field = fields.next();
					if (!descriptor.path("fields").has(field.getKey()) && !field.getValue().equals(next.path(namespace.getKey()).path(field.getKey())))
						throw new IOException("Unavailable metadata field must be preserved: " + namespace.getKey() + "." + field.getKey());
				}
			}
		}
	}

	private static boolean validReferences(JsonNode value, JsonNode previous, JsonNode schema) {
		if (!value.isArray() || value.size() > 1000) return false;
		Set<String> names = new java.util.HashSet<>();
		for (var item : value) {
			if (!item.isTextual() || item.asText().isEmpty() || item.asText().length() > 4096 || !names.add(item.asText())) return false;
			boolean known = false;
			for (var choice : schema.path("items").path("enum")) known |= choice.equals(item);
			// A missing reference may be preserved/reordered/removed, never newly introduced.
			if (!known && previous.isArray()) for (var old : previous) known |= old.equals(item);
			if (!known) return false;
		}
		return true;
	}
}
