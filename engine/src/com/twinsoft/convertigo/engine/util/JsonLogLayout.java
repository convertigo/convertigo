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
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.bridge.LayoutWrapper;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.message.Message;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * One JSON object per line, for the standard output read by log collectors that only split JSON lines into fields.
 * The contextual parameters of the line become separate fields and the stack trace stays inside the line. The
 * statistics line written at the end of each request gives its total time as duration_ms.
 */
public final class JsonLogLayout extends AbstractStringLayout {
	static final String CONTEXTUAL_PARAMETERS = "ContextualParameters";

	/** the parameters of Log4jHelper.mdcKeys, written as top-level fields */
	static final Set<String> CONTEXT_FIELDS = Set.of("project", "sequence", "transaction", "connector", "user",
			"contextid", "clientip", "clienthostname", "uid", "uuid");

	/** the logger and the "Total" row of the table that GenericRequester logs with EngineStatistics.printStatistics() */
	static final String STATISTICS_LOGGER = "cems.Statistics";
	private static final Pattern STATISTICS_TOTAL = Pattern.compile("\\| Total +\\| +(-?\\d+) ms");

	private static final JsonFactory JSON_FACTORY = new JsonFactory()
			.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

	private final DateTimeFormatter timeFormat;
	private final String instance;

	public JsonLogLayout() {
		this(ZoneId.systemDefault(), System.getenv("HOSTNAME"));
	}

	JsonLogLayout(ZoneId zone, String instance) {
		super(StandardCharsets.UTF_8);
		this.timeFormat = TIME_FORMAT.withZone(zone);
		this.instance = instance == null || instance.isBlank() ? null : instance;
	}

	@Override
	public String toSerializable(LogEvent event) {
		var writer = new StringWriter(512);
		try (var json = JSON_FACTORY.createGenerator(writer)) {
			json.writeStartObject();
			var instant = event.getInstant();
			json.writeStringField("time", timeFormat.format(
					java.time.Instant.ofEpochSecond(instant.getEpochSecond(), instant.getNanoOfSecond())));
			json.writeStringField("level", String.valueOf(event.getLevel()));
			String logger = event.getLoggerName();
			json.writeStringField("logger", logger);
			json.writeStringField("thread", event.getThreadName());
			Message message = event.getMessage();
			String text = message == null ? "" : String.valueOf(message.getFormattedMessage());
			json.writeStringField("message", text);

			Object parameters = event.getContextData().getValue(CONTEXTUAL_PARAMETERS);
			if (parameters != null) {
				writeContext(json, parseContextualParameters(parameters.toString()));
			}
			if (STATISTICS_LOGGER.equals(logger)) {
				var total = STATISTICS_TOTAL.matcher(text);
				if (total.find()) {
					json.writeNumberField("duration_ms", Long.parseLong(total.group(1)));
				}
			}

			Throwable thrown = event.getThrown();
			if (thrown != null) {
				json.writeStringField("exception_class", thrown.getClass().getName());
				json.writeStringField("exception", stackTrace(thrown));
			}
			json.writeStringField("app", "convertigo");
			if (instance != null) {
				json.writeStringField("instance", instance);
			}
			json.writeEndObject();
		} catch (IOException | RuntimeException e) {
			// a StringWriter does not fail: keep a valid line anyway
			return "{\"level\":\"ERROR\",\"message\":\"JSON log line failed\",\"app\":\"convertigo\"}\n";
		}
		writer.write('\n');
		return writer.toString();
	}

	private static void writeContext(JsonGenerator json, Map<String, String> parameters) throws IOException {
		Map<String, String> others = null;
		for (var entry : parameters.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (CONTEXT_FIELDS.contains(key)) {
				json.writeStringField(key, "user".equals(key) ? unquote(value) : value);
			} else {
				if (others == null) {
					others = new LinkedHashMap<>();
				}
				others.put(key, value);
			}
		}
		// other parameters, such as the environment sent with the client logs, stay apart from the line fields
		if (others != null) {
			json.writeObjectFieldStart("context");
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
			super(new JsonLogLayout());
		}
	}
}
