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

package com.twinsoft.convertigo.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.PropertyConfigurator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.JsonLogLayout;
import com.twinsoft.convertigo.engine.util.Log4jHelper;

/** The stdout appender built as in production, through the Log4j 1.x bridge, in its text and JSON formats. */
public class StdoutLogFormatTest {
	private static final String PATTERN = PropertyName.LOG4J_APPENDER_CEMSAPPENDER_LAYOUT_CONVERSIONPATTERN.getDefaultValue();
	private static final JsonFactory JSON = new JsonFactory().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

	private static PrintStream originalOut;
	private static final ByteArrayOutputStream out = new ByteArrayOutputStream();
	private static Logger logger;

	@BeforeClass
	public static void initialize() throws Exception {
		// the console appender takes System.out when it is created
		originalOut = System.out;
		System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
		for (String name : List.of("properties", "system_properties")) {
			var field = EnginePropertiesManager.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(null, new Properties());
		}
		logger = Logger.getLogger("cems.Test");
		if (Engine.theApp == null) {
			// a Context needs an Engine; its logs stay out of the cems loggers written on stdout
			Engine.isEngineMode();
			Engine.logEngine = Engine.logContext = Engine.logStatistics = Engine.logDevices = Logger.getLogger("stdout-test");
			Engine.theApp = new Engine();
		}
	}

	@AfterClass
	public static void restore() {
		System.setOut(originalOut);
	}

	@Before
	public void before() {
		Log4jHelper.mdcClear();
		out.reset();
	}

	@After
	public void after() {
		Log4jHelper.mdcClear();
	}

	private static Properties configure(String format) {
		var properties = new Properties();
		properties.put("log4j.logger.cems", "INFO");
		EnginePropertiesManager.addStdoutAppender(properties, format, PATTERN);
		PropertyConfigurator.configure(properties);
		out.reset();
		return properties;
	}

	/** what the stdout appender wrote, its buffer included (the text format does not flush each line) */
	private static String output() {
		var context = (LoggerContext) LogManager.getContext(false);
		Appender appender = context.getConfiguration().getAppender("stdout");
		if (appender instanceof ConsoleAppender console) {
			console.getManager().flush();
		}
		System.out.flush();
		return out.toString(StandardCharsets.UTF_8);
	}

	/** the only line written, parsed strictly (no duplicate field) */
	private static Map<String, Object> jsonLine() throws IOException {
		String text = output();
		assertTrue("one line ending with a new line: " + text, text.endsWith("\n"));
		assertEquals("a single line: " + text, text.length() - 1, text.indexOf('\n'));
		try (var parser = JSON.createParser(text)) {
			assertEquals(JsonToken.START_OBJECT, parser.nextToken());
			var object = readObject(parser);
			assertNull(parser.nextToken());
			return object;
		}
	}

	private static Map<String, Object> readObject(JsonParser parser) throws IOException {
		var object = new LinkedHashMap<String, Object>();
		while (parser.nextToken() == JsonToken.FIELD_NAME) {
			String name = parser.currentName();
			JsonToken token = parser.nextToken();
			object.put(name, switch (token) {
				case START_OBJECT -> readObject(parser);
				case VALUE_STRING -> parser.getText();
				case VALUE_NUMBER_INT -> parser.getLongValue();
				case VALUE_TRUE, VALUE_FALSE -> parser.getBooleanValue();
				default -> throw new AssertionError("unexpected " + token + " for " + name);
			});
		}
		return object;
	}

	private static void request(String project, String user) {
		var parameters = new LogParameters();
		Log4jHelper.mdcSet(parameters);
		Log4jHelper.mdcPut(Log4jHelper.mdcKeys.Project, project);
		Log4jHelper.mdcPut(Log4jHelper.mdcKeys.User, user);
	}

	@Test
	public void textFormatKeepsTheLegacyAppender() {
		for (String format : new String[] { null, "", "text", "TEXT", "xml" }) {
			var properties = new Properties();
			properties.put("log4j.logger.cems", "INFO");
			EnginePropertiesManager.addStdoutAppender(properties, format, PATTERN);
			var legacy = new Properties();
			legacy.put("log4j.logger.cems", "INFO, stdout");
			legacy.put("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
			legacy.put("log4j.appender.stdout.target", "System.out");
			legacy.put("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
			legacy.put("log4j.appender.stdout.layout.ConversionPattern", PATTERN);
			assertEquals("format " + format, legacy, properties);
		}
	}

	@Test
	public void jsonFormatsUseTheirLayouts() {
		for (var format : Map.of("json", JsonLogLayout.Bridge.class, "JSON", JsonLogLayout.Bridge.class,
				"json-ecs", JsonLogLayout.EcsBridge.class, " Json-ECS ", JsonLogLayout.EcsBridge.class).entrySet()) {
			var properties = new Properties();
			properties.put("log4j.logger.cems", "INFO");
			EnginePropertiesManager.addStdoutAppender(properties, format.getKey(), PATTERN);
			assertEquals(format.getKey(), format.getValue().getName(), properties.get("log4j.appender.stdout.layout"));
			assertEquals("true", properties.get("log4j.appender.stdout.ImmediateFlush"));
			assertNull(properties.get("log4j.appender.stdout.layout.ConversionPattern"));
			assertEquals("INFO, stdout", properties.get("log4j.logger.cems"));
		}
	}

	@Test
	public void textFormatWritesTheEngineLogLine() {
		configure("text");
		request("Shop", "'alice'");
		logger.info("hello \"world\"");
		String line = output();
		assertTrue(line, line.matches("!Test {24} \\| \\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d,\\d{3} \\| INFO  \\| .{32,} \\| "
				+ "\\$project=Shop \\| \\$user='alice' \\| hello \"world\"\n"));
	}

	@Test
	public void simpleLine() throws IOException {
		configure("json");
		logger.warn("simple message");
		var line = jsonLine();
		assertTrue(line.toString(), ((String) line.get("time")).matches("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d\\.\\d{3}(Z|[+-]\\d\\d:\\d\\d)"));
		assertEquals("WARN", line.get("level"));
		assertEquals("cems.Test", line.get("logger"));
		assertEquals(Thread.currentThread().getName(), line.get("thread"));
		assertEquals("simple message", line.get("message"));
		assertEquals("convertigo", line.get("app"));
		assertFalse(line.containsKey("exception"));
		assertFalse(line.containsKey("project"));
	}

	@Test
	public void contextualParametersAreFields() throws IOException {
		configure("json");
		request("Shop", "'alice'");
		for (var key : Log4jHelper.mdcKeys.values()) {
			if (key != Log4jHelper.mdcKeys.Project && key != Log4jHelper.mdcKeys.User) {
				Log4jHelper.mdcPut(key, key.name() + "-value");
			}
		}
		Log4jHelper.mdcPut(Log4jHelper.mdcKeys.Connector, null);
		((LogParameters) MDC.get("ContextualParameters")).put("platform", "android");
		Log4jHelper.mdcSet((LogParameters) MDC.get("ContextualParameters"));
		logger.error("failed");
		var line = jsonLine();
		assertEquals("Shop", line.get("project"));
		assertEquals("alice", line.get("user"));
		assertEquals("Sequence-value", line.get("sequence"));
		assertEquals("Transaction-value", line.get("transaction"));
		assertEquals("ContextID-value", line.get("contextid"));
		assertEquals("ClientIP-value", line.get("clientip"));
		assertEquals("ClientHostName-value", line.get("clienthostname"));
		assertEquals("UID-value", line.get("uid"));
		assertEquals("UUID-value", line.get("uuid"));
		assertFalse("null parameters are left out", line.containsKey("connector"));
		assertEquals(Map.of("platform", "android"), line.get("context"));
		assertEquals("ERROR", line.get("level"));
	}

	@Test
	public void structuredContextKeepsLiteralValuesInBothFormats() throws IOException {
		for (var format : List.of("json", "json-ecs")) {
			configure(format);
			out.reset();
			var parameters = new LogParameters();
			parameters.put("project", "Inventory");
			parameters.put("platform", "macOS | $edition=desktop");
			parameters.put("version", "null");
			parameters.put("optional", null);
			Log4jHelper.mdcSet(parameters);
			logger.info("client event");
			var line = jsonLine();
			String prefix = format.equals("json-ecs") ? "convertigo." : "";
			assertEquals("Inventory", line.get(prefix + "project"));
			assertEquals(Map.of("platform", "macOS | $edition=desktop", "version", "null"), line.get(prefix + "context"));
		}
	}

	@Test
	public void multilineExceptionStaysInTheLine() throws IOException {
		configure("json");
		var failure = new EngineException("outer failure", new IllegalStateException("inner\nsecond line"));
		logger.error("request failed\nwith details", failure);
		var line = jsonLine();
		assertEquals("request failed\nwith details", line.get("message"));
		assertEquals(EngineException.class.getName(), line.get("exception_class"));
		String exception = (String) line.get("exception");
		assertTrue(exception, exception.startsWith(EngineException.class.getName() + ": outer failure"));
		assertTrue(exception, exception.contains("\n\tat com.twinsoft.convertigo.engine.StdoutLogFormatTest.multilineExceptionStaysInTheLine("));
		assertTrue(exception, exception.contains("Caused by: java.lang.IllegalStateException: inner\nsecond line"));
		assertFalse(exception, exception.endsWith("\n"));
	}

	@Test
	public void charactersAreEscaped() throws IOException {
		configure("json");
		String message = "tab\t cr\r lf\n back\\slash nul\u0000 bell\u0007 é€😀 ls\u2028 </script> ${x} | $project=fake";
		request("p\\\"q\t", "'o\"brien'");
		logger.info(message);
		var line = jsonLine();
		assertEquals(message, line.get("message"));
		assertEquals("p\\\"q\t", line.get("project"));
		assertEquals("o\"brien", line.get("user"));
		String raw = output();
		assertFalse("no raw control character", raw.substring(0, raw.length() - 1).chars().anyMatch(c -> c < 0x20));
	}

	@Test
	public void quotesInTheMessage() throws IOException {
		configure("json");
		logger.info("He said \"stop\" and '\"left\"'");
		assertEquals("He said \"stop\" and '\"left\"'", jsonLine().get("message"));
	}

	@Test
	public void statisticsLineGivesTheTotalTime() throws Exception {
		configure("json");
		request("Shop", "'alice'");
		var statistics = new EngineStatistics();
		statistics.add(EngineStatistics.GET_DOCUMENT, 30);
		statistics.add(EngineStatistics.REQUEST, 42);
		String table = statistics.printStatistics();
		// the line written by GenericRequester at the end of each request
		Logger.getLogger("cems.Statistics").info("Statistics:\n" + table);
		var line = jsonLine();
		assertEquals("cems.Statistics", line.get("logger"));
		assertEquals(42L, line.get("duration_ms"));
		assertEquals("Statistics:\n" + table, line.get("message"));
		assertEquals("Shop", line.get("project"));
		assertEquals("alice", line.get("user"));

		out.reset();
		logger.info("| Total          |      42 ms        |");
		assertFalse("only the statistics logger", jsonLine().containsKey("duration_ms"));
	}

	@Test
	public void ecsSimpleLine() throws IOException {
		configure("json-ecs");
		logger.warn("simple message");
		String raw = output();
		assertTrue("@timestamp, log.level and message first: " + raw,
				raw.startsWith("{\"@timestamp\":\"") && raw.indexOf("\"log.level\":\"WARN\",\"message\":\"simple message\",") > 0);
		var line = jsonLine();
		assertTrue(line.toString(), ((String) line.get("@timestamp")).matches("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d\\.\\d{3}Z"));
		assertEquals("WARN", line.get("log.level"));
		assertEquals("cems.Test", line.get("log.logger"));
		assertEquals(Thread.currentThread().getName(), line.get("process.thread.name"));
		assertEquals("simple message", line.get("message"));
		assertEquals(JsonLogLayout.ECS_VERSION, line.get("ecs.version"));
		assertEquals("convertigo", line.get("service.name"));
		assertEquals("convertigo.log", line.get("event.dataset"));
		for (String name : new String[] { "time", "level", "logger", "thread", "app", "instance", "error.type" }) {
			assertFalse(name, line.containsKey(name));
		}
	}

	@Test
	public void ecsContextualParameters() throws IOException {
		configure("json-ecs");
		request("Shop", "'alice'");
		for (var key : Log4jHelper.mdcKeys.values()) {
			if (key != Log4jHelper.mdcKeys.Project && key != Log4jHelper.mdcKeys.User) {
				Log4jHelper.mdcPut(key, key.name() + "-value");
			}
		}
		Log4jHelper.mdcPut(Log4jHelper.mdcKeys.ClientIP, "2001:db8::7");
		((LogParameters) MDC.get("ContextualParameters")).put("platform", "android");
		Log4jHelper.mdcSet((LogParameters) MDC.get("ContextualParameters"));
		logger.error("failed");
		var line = jsonLine();
		assertEquals("alice", line.get("user.name"));
		assertEquals("2001:db8::7", line.get("client.ip"));
		assertEquals("ClientHostName-value", line.get("client.domain"));
		assertEquals("Shop", line.get("convertigo.project"));
		assertEquals("Sequence-value", line.get("convertigo.sequence"));
		assertEquals("Transaction-value", line.get("convertigo.transaction"));
		assertEquals("Connector-value", line.get("convertigo.connector"));
		assertEquals("ContextID-value", line.get("convertigo.contextid"));
		assertEquals("UID-value", line.get("convertigo.uid"));
		assertEquals("UUID-value", line.get("convertigo.uuid"));
		assertEquals(Map.of("platform", "android"), line.get("convertigo.context"));
		for (String name : new String[] { "project", "user", "clientip", "context" }) {
			assertFalse(name, line.containsKey(name));
		}

		out.reset();
		Log4jHelper.mdcPut(Log4jHelper.mdcKeys.ClientIP, "unknown");
		logger.info("not an address");
		line = jsonLine();
		assertFalse("client.ip only takes an address", line.containsKey("client.ip"));
		assertEquals("unknown", line.get("client.address"));
	}

	@Test
	public void ecsErrorAndDuration() throws Exception {
		configure("json-ecs");
		var failure = new EngineException("outer failure", new IllegalStateException("inner\nsecond line"));
		logger.error("request failed", failure);
		var line = jsonLine();
		assertEquals(EngineException.class.getName(), line.get("error.type"));
		assertEquals("outer failure", line.get("error.message"));
		String stack = (String) line.get("error.stack_trace");
		assertTrue(stack, stack.startsWith(EngineException.class.getName() + ": outer failure\n\tat "));
		assertTrue(stack, stack.contains("Caused by: java.lang.IllegalStateException: inner\nsecond line"));
		assertFalse(line.containsKey("exception"));

		out.reset();
		var statistics = new EngineStatistics();
		statistics.add(EngineStatistics.GET_DOCUMENT, 30);
		statistics.add(EngineStatistics.REQUEST, 42);
		Logger.getLogger("cems.Statistics").info("Statistics:\n" + statistics.printStatistics());
		line = jsonLine();
		assertEquals("event.duration is in nanoseconds", 42_000_000L, line.get("event.duration"));
		assertEquals(42L, line.get("convertigo.duration_ms"));
		assertFalse(line.containsKey("duration_ms"));
	}

	@Test
	public void reconfigurationSwitchesTheFormat() throws IOException {
		configure("json");
		logger.info("first");
		assertEquals("first", jsonLine().get("message"));
		configure("text");
		logger.info("second");
		assertTrue(output(), output().startsWith("!Test "));
		configure("json-ecs");
		logger.info("third");
		assertEquals("INFO", jsonLine().get("log.level"));
		configure("json");
		logger.info("fourth");
		assertEquals("INFO", jsonLine().get("level"));
	}
}
