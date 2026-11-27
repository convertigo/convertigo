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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.bridge.LayoutWrapper;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.message.Message;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.twinsoft.convertigo.engine.LogParameters;

/**
 * One JSON object per line, for the standard output read by log collectors that only split JSON lines into fields.
 * The contextual parameters of the line become separate fields and the stack trace stays inside the line. The
 * statistics line written at the end of each request gives its total time. The names and the order of the fields come
 * from the {@link Format} chosen by log.stdout.format.
 */
public final class JsonLogLayout extends AbstractStringLayout {
	private static final TypeReference<Map<String, String>> CONTEXT_TYPE = new TypeReference<>() {};

	/** the version of the Elastic Common Schema followed by the json-ecs format */
	public static final String ECS_VERSION = "9.5.0";

	/** the logger and the "Total" row of the table that GenericRequester logs with EngineStatistics.printStatistics() */
	static final String STATISTICS_LOGGER = "cems.Statistics";
	private static final Pattern STATISTICS_TOTAL = Pattern.compile("\\| Total +\\| +(-?\\d+) ms");

	private static final Pattern IP_LITERAL = Pattern.compile("[0-9A-Fa-f:.]*[.:][0-9A-Fa-f:.]*");

	private static final JsonFactory JSON_FACTORY = new JsonFactory()
			.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

	/** the parts of a line, written in the order a format gives */
	enum Part {
		TIME, LEVEL, LOGGER, THREAD, MESSAGE, SERVICE, PARAMETERS, DURATION, ERROR
	}

	/**
	 * The names of the fields of a JSON format, null for a field the format leaves out. The parameters are the keys
	 * of Log4jHelper.mdcKeys in lower case; the other contextual parameters go in one object.
	 */
	record Fields(List<Part> order, String time, boolean utc, String level, String logger, String thread,
			String message, List<Map.Entry<String, String>> constants, String instance, Map<String, String> parameters,
			String otherParameters, String durationMs, String durationNs, String errorType, String errorMessage,
			String stackTrace) {
	}

	/**
	 * The JSON values of log.stdout.format. A new convention is a new constant with its own Bridge class: the names
	 * of the existing formats never change.
	 */
	public enum Format {
		/** our names, with the contextual parameters at the top level */
		json("json", new Fields(
				List.of(Part.TIME, Part.LEVEL, Part.LOGGER, Part.THREAD, Part.MESSAGE, Part.PARAMETERS, Part.DURATION,
						Part.ERROR, Part.SERVICE),
				"time", false, "level", "logger", "thread", "message",
				List.of(Map.entry("app", "convertigo")), "instance",
				parameters("project", "project", "sequence", "sequence", "transaction", "transaction",
						"connector", "connector", "user", "user", "contextid", "contextid", "clientip", "clientip",
						"clienthostname", "clienthostname", "uid", "uid", "uuid", "uuid"),
				"context", "duration_ms", null, "exception_class", null, "exception")),

		/**
		 * Elastic Common Schema {@value JsonLogLayout#ECS_VERSION}, with flat dotted keys and @timestamp, log.level and
		 * message first, as the ecs-logging-java encoders write them. The parameters without an ECS field go under
		 * convertigo.*, and event.duration is in nanoseconds.
		 */
		json_ecs("json-ecs", new Fields(
				List.of(Part.TIME, Part.LEVEL, Part.MESSAGE, Part.SERVICE, Part.THREAD, Part.LOGGER, Part.PARAMETERS,
						Part.DURATION, Part.ERROR),
				"@timestamp", true, "log.level", "log.logger", "process.thread.name", "message",
				List.of(Map.entry("ecs.version", ECS_VERSION), Map.entry("service.name", "convertigo"),
						Map.entry("event.dataset", "convertigo.log")), "service.node.name",
				parameters("user", "user.name", "clientip", "client.ip", "clienthostname", "client.domain",
						"project", "convertigo.project", "sequence", "convertigo.sequence",
						"transaction", "convertigo.transaction", "connector", "convertigo.connector",
						"contextid", "convertigo.contextid", "uid", "convertigo.uid", "uuid", "convertigo.uuid"),
				"convertigo.context", "convertigo.duration_ms", "event.duration", "error.type", "error.message",
				"error.stack_trace"));

		private final String value;
		private final Fields fields;

		Format(String value, Fields fields) {
			this.value = value;
			this.fields = fields;
		}

		public String value() {
			return value;
		}

		Fields fields() {
			return fields;
		}

		/** the class to name in a "log4j.appender.X.layout" property */
		public Class<? extends LayoutWrapper> bridge() {
			return this == json ? Bridge.class : EcsBridge.class;
		}

		/** the format of a log.stdout.format value, null for text or an unknown value */
		public static Format of(String value) {
			String name = value == null ? "" : value.trim();
			for (Format format : values()) {
				if (format.value.equalsIgnoreCase(name)) {
					return format;
				}
			}
			return null;
		}

		private static Map<String, String> parameters(String... names) {
			var parameters = new LinkedHashMap<String, String>();
			for (int i = 0; i < names.length; i += 2) {
				parameters.put(names[i], names[i + 1]);
			}
			return parameters;
		}
	}

	private final Fields fields;
	private final DateTimeFormatter timeFormat;
	private final String instance;

	public JsonLogLayout() {
		this(Format.json);
	}

	public JsonLogLayout(Format format) {
		this(format, ZoneId.systemDefault(), System.getenv("HOSTNAME"));
	}

	JsonLogLayout(Format format, ZoneId zone, String instance) {
		super(StandardCharsets.UTF_8);
		this.fields = format.fields();
		this.timeFormat = TIME_FORMAT.withZone(fields.utc() ? ZoneOffset.UTC : zone);
		this.instance = instance == null || instance.isBlank() ? null : instance;
	}

	@Override
	public String toSerializable(LogEvent event) {
		var writer = new StringWriter(512);
		try (var json = JSON_FACTORY.createGenerator(writer)) {
			json.writeStartObject();
			Message message = event.getMessage();
			String text = message == null ? "" : String.valueOf(message.getFormattedMessage());
			for (Part part : fields.order()) {
				write(json, part, event, text);
			}
			json.writeEndObject();
		} catch (IOException | RuntimeException e) {
			// a StringWriter does not fail: keep a valid line anyway
			return "{\"message\":\"JSON log line failed\"}\n";
		}
		writer.write('\n');
		return writer.toString();
	}

	private void write(JsonGenerator json, Part part, LogEvent event, String text) throws IOException {
		switch (part) {
		case TIME -> {
			var instant = event.getInstant();
			json.writeStringField(fields.time(), timeFormat.format(
					java.time.Instant.ofEpochSecond(instant.getEpochSecond(), instant.getNanoOfSecond())));
		}
		case LEVEL -> json.writeStringField(fields.level(), String.valueOf(event.getLevel()));
		case LOGGER -> json.writeStringField(fields.logger(), event.getLoggerName());
		case THREAD -> json.writeStringField(fields.thread(), event.getThreadName());
		case MESSAGE -> json.writeStringField(fields.message(), text);
		case SERVICE -> {
			for (var constant : fields.constants()) {
				json.writeStringField(constant.getKey(), constant.getValue());
			}
			if (instance != null) {
				json.writeStringField(fields.instance(), instance);
			}
		}
		case PARAMETERS -> {
			writeParameters(json, contextualParameters(event));
		}
		case DURATION -> {
			if (STATISTICS_LOGGER.equals(event.getLoggerName())) {
				var total = STATISTICS_TOTAL.matcher(text);
				if (total.find()) {
					long ms = Long.parseLong(total.group(1));
					if (fields.durationNs() != null) {
						json.writeNumberField(fields.durationNs(), ms * 1_000_000L);
					}
					json.writeNumberField(fields.durationMs(), ms);
				}
			}
		}
		case ERROR -> {
			Throwable thrown = event.getThrown();
			if (thrown != null) {
				json.writeStringField(fields.errorType(), thrown.getClass().getName());
				if (fields.errorMessage() != null && thrown.getMessage() != null) {
					json.writeStringField(fields.errorMessage(), thrown.getMessage());
				}
				json.writeStringField(fields.stackTrace(), stackTrace(thrown));
			}
		}
		}
	}

	private static Map<String, String> contextualParameters(LogEvent event) {
		String structured = event.getContextData().getValue(Log4jHelper.STRUCTURED_PARAMETERS);
		if (structured != null) {
			try {
				return Log4jHelper.CONTEXT_MAPPER.readValue(structured, CONTEXT_TYPE);
			} catch (JsonProcessingException e) {
				// Fall back to the legacy MDC of events without a readable structured snapshot.
			}
		}
		Object legacy = event.getContextData().getValue(Log4jHelper.CONTEXTUAL_PARAMETERS);
		return legacy instanceof LogParameters parameters ? parameters.asMap()
				: parseContextualParameters(legacy == null ? null : legacy.toString());
	}

	private void writeParameters(JsonGenerator json, Map<String, String> parameters) throws IOException {
		Map<String, String> others = null;
		for (var entry : parameters.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (value == null) {
				continue;
			}
			String name = fields.parameters().get(key);
			if (name != null) {
				if ("user".equals(key)) {
					value = unquote(value);
				}
				// an ECS ip field only takes an address: anything else is kept as the raw address
				if (name.endsWith(".ip") && !IP_LITERAL.matcher(value).matches()) {
					name = name.substring(0, name.length() - 3) + ".address";
				}
				json.writeStringField(name, value);
			} else {
				if (others == null) {
					others = new LinkedHashMap<>();
				}
				others.put(key, value);
			}
		}
		// other parameters, such as the environment sent with the client logs, stay apart from the line fields
		if (others != null) {
			json.writeObjectFieldStart(fields.otherParameters());
			for (var entry : others.entrySet()) {
				json.writeStringField(entry.getKey(), entry.getValue());
			}
			json.writeEndObject();
		}
	}

	/** user is logged as 'name' in the text lines */
	private static String unquote(String value) {
		if (value != null && value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}

	private static String stackTrace(Throwable thrown) {
		var stack = new StringWriter(2048);
		try (var printer = new PrintWriter(stack)) {
			thrown.printStackTrace(printer);
		}
		String text = stack.toString();
		int end = text.length();
		while (end > 0 && (text.charAt(end - 1) == '\n' || text.charAt(end - 1) == '\r')) {
			end--;
		}
		return text.substring(0, end);
	}

	/**
	 * Reads the text the MDC holds for the LogParameters of the line: "$key=value | $key2=value2 | ". A value
	 * containing " | $" cannot be told apart from the next parameter; "null" values are left out.
	 */
	static Map<String, String> parseContextualParameters(String text) {
		var parameters = new LinkedHashMap<String, String>();
		if (text == null) {
			return parameters;
		}
		int length = text.length();
		int position = 0;
		while (position < length && text.charAt(position) == '$') {
			int equal = text.indexOf('=', position + 1);
			if (equal < 0) {
				break;
			}
			String key = text.substring(position + 1, equal);
			int next = text.indexOf(" | $", equal + 1);
			int end;
			if (next < 0) {
				end = text.endsWith(" | ") && length - 3 > equal ? length - 3 : length;
				position = length;
			} else {
				end = next;
				position = next + 3;
			}
			String value = text.substring(equal + 1, end);
			if (!key.isEmpty() && !"null".equals(value)) {
				parameters.put(key, value);
			}
		}
		return parameters;
	}

	/**
	 * The class to name in a "log4j.appender.X.layout" property: the Log4j 1.x bridge uses the wrapped layout as the
	 * layout of the Log4j 2 appender it builds.
	 */
	public static final class Bridge extends LayoutWrapper {
		public Bridge() {
			super(new JsonLogLayout(Format.json));
		}
	}

	/** {@link Bridge} of the json-ecs format */
	public static final class EcsBridge extends LayoutWrapper {
		public EcsBridge() {
			super(new JsonLogLayout(Format.json_ecs));
		}
	}
}
