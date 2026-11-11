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

package com.twinsoft.convertigo.engine.studio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.JsonStreamRowsStep;
import com.twinsoft.convertigo.beans.steps.JsonToXmlStep;
import com.twinsoft.convertigo.beans.steps.JsonTransformStep;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.json.NativeStepOutput;

/**
 * The JSON native sequences for the web Studio, as the Eclipse Studio sees them (its NativeJson): which sequences and
 * steps are native, which XML steps read a native step through its XML view, and what each native step produced in
 * the executions of the web Studio.
 * <p>
 * An execution of the web Studio asks the JSON of the native steps with the request parameter
 * {@value #REQUEST_PARAMETER} (a token of its own) from a session of an administrator; the outputs of the steps are
 * then kept for this token (the rows of the execution) and by step (the samples of the pickers, the last JSON each
 * step produced).
 */
public final class NativeJsonSupport {
	/** the request parameter of an execution of the web Studio that follows its native steps */
	public static final String REQUEST_PARAMETER = "__native";

	private static final int MAX_RUNS = 16;
	private static final int MAX_ROWS = 2000;
	/** above this total, the JSON of the next rows of a run is not kept (the rows are) */
	private static final long MAX_RUN_CHARS = 64L * 1024 * 1024;
	private static final Pattern ITEM = Pattern.compile("item\\[(\\d+)\\]");
	private static final Pattern NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_\\-]*");

	/** one native step execution of a run */
	public static class Row {
		public final int index;
		public final NativeStepOutput output;
		/** false when the run had too much JSON to keep this one */
		public final boolean kept;

		Row(int index, NativeStepOutput output, boolean kept) {
			this.index = index;
			this.output = output;
			this.kept = kept;
		}
	}

	/** the native step executions of an execution of the web Studio */
	public static class Run {
		public final List<Row> rows = new ArrayList<>();
		long chars = 0;
	}

	private static final Map<String, Run> runs = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Run> eldest) {
			return size() > MAX_RUNS;
		}
	});

	/** the last outputs by step (project, sequence, priority): the steps of an execution are copies of the project ones */
	private static final Map<String, NativeStepOutput> samples = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, NativeStepOutput> eldest) {
			return size() > 64;
		}
	});

	private NativeJsonSupport() {
	}

	/**
	 * the token of the execution of the web Studio the step runs in, when it follows its native steps, else null; the
	 * request keeps the answer (RequestAttribute.studioNativeJson)
	 */
	public static String studioToken(Step step) {
		try {
			var sequence = step.getSequence();
			var request = sequence == null || sequence.context == null ? null : sequence.context.httpServletRequest;
			if (request == null) {
				return null;
			}
			var known = RequestAttribute.studioNativeJson.get(request);
			if (known instanceof String token) {
				return token.isEmpty() ? null : token;
			}
			var token = followedToken(request);
			RequestAttribute.studioNativeJson.set(request, token == null ? "" : token);
			return token;
		} catch (Exception e) {
			return null;
		}
	}

	private static String followedToken(HttpServletRequest request) {
		var token = request.getParameter(REQUEST_PARAMETER);
		if (token == null || token.isBlank() || token.length() > 64) {
			return null;
		}
		var session = request.getSession(false);
		if (session == null || !Engine.authenticatedSessionManager.hasRole(session, Role.WEB_ADMIN)) {
			return null;
		}
		return token;
	}

	/** keeps what a native step produced in an execution of the web Studio */
	public static void record(String token, NativeStepOutput output) {
		if (output.getNote() == null || !output.getJson().isEmpty()) {
			samples.put(keyOf(output.getStep()), output);
		}
		if (token == null) {
			return;
		}
		Run run;
		synchronized (runs) {
			run = runs.computeIfAbsent(token, k -> new Run());
		}
		synchronized (run) {
			if (run.rows.size() >= MAX_ROWS) {
				return;
			}
			var length = output.getJson().length();
			var kept = run.chars + length <= MAX_RUN_CHARS;
			if (kept) {
				run.chars += length;
			}
			run.rows.add(new Row(run.rows.size(), output, kept));
		}
	}

	/** the native step executions of the run of the web Studio, from the index given */
	public static List<Row> rows(String token, int from) {
		var run = runs.get(token);
		if (run == null) {
			return List.of();
		}
		synchronized (run) {
			return from >= run.rows.size() ? List.of() : new ArrayList<>(run.rows.subList(Math.max(0, from), run.rows.size()));
		}
	}

	/** one native step execution of the run, or null */
	public static Row row(String token, int index) {
		var run = runs.get(token);
		if (run == null) {
			return null;
		}
		synchronized (run) {
			return index < 0 || index >= run.rows.size() ? null : run.rows.get(index);
		}
	}

	/** the JSON the step produced at its last execution in the web Studio, or null */
	public static NativeStepOutput getSample(Step step) {
		return step == null ? null : samples.get(keyOf(step));
	}

	/** the same key for a step of the project and for its copies executed */
	public static String keyOf(Step step) {
		try {
			return step.getProject().getName() + "/" + step.getSequence().getName() + "/" + step.priority;
		} catch (Exception e) {
			return step.getQName();
		}
	}

	/** a sequence the Studio runs as JSON: native execution or native JSON output */
	public static boolean isNativeSequence(Sequence sequence) {
		return sequence != null && (sequence.isNativeExecution() || sequence.isJsonNativeOutput());
	}

	/** a step that works on JSON data without XML */
	public static boolean isNativeStep(Step step) {
		return step instanceof JsonTransformStep || step instanceof JsonStreamRowsStep;
	}

	/** a step whose value is JSON for the next steps: a native step, or a JSON to XML step kept native */
	public static boolean isJsonProducer(Step step) {
		return isNativeStep(step) || step instanceof JsonToXmlStep;
	}

	/** a sequence, or one of its steps in blocks, holds a native step */
	public static boolean hasNativeSteps(Sequence sequence) {
		return sequence != null && hasNativeSteps(sequence.getSteps());
	}

	private static boolean hasNativeSteps(List<Step> steps) {
		for (var step : steps) {
			if (isNativeStep(step) || (step instanceof StepWithExpressions block && hasNativeSteps(block.getSteps()))) {
				return true;
			}
		}
		return false;
	}

	/** the variable (or response key) a JSON step gives its value to */
	public static String getVariableName(Step step) {
		if (step instanceof JsonTransformStep transform) {
			return transform.getVariableName();
		}
		var sample = getSample(step);
		if (sample != null) {
			return sample.getVariableName();
		}
		return step.getName();
	}

	/** the native steps an XML step reads by an XPath source: each read builds an XML view of their JSON */
	public static List<Step> getBridgedSources(Step step) {
		return getSourceSteps(step, false);
	}

	/** the JSON steps a step reads by a JSON path source ($...): no XML view of their JSON */
	public static List<Step> getJsonPathSources(Step step) {
		return getSourceSteps(step, true);
	}

	private static List<Step> getSourceSteps(Step step, boolean jsonPath) {
		var found = new ArrayList<Step>();
		if (step == null || isNativeStep(step)) {
			return found;
		}
		try {
			for (StepSource source : step.getSources()) {
				if (source.isJsonPath() != jsonPath) {
					continue;
				}
				var sourceStep = source.getStep();
				// a JSON path also reads the JSON text answered by a raw HTTP transaction (RequestableStep)
				if ((jsonPath ? isJsonProducer(sourceStep) || sourceStep instanceof RequestableStep
						: sourceStep instanceof JsonTransformStep) && !found.contains(sourceStep)) {
					found.add(sourceStep);
				}
			}
		} catch (Exception e) {
			// a broken source: nothing found
		}
		return found;
	}

	/** the XML steps of a sequence that read a native step through its XML view, with the native steps they read */
	public static Map<Step, List<Step>> getBridges(Sequence sequence) {
		var bridges = new LinkedHashMap<Step, List<Step>>();
		for (var step : sequence.getSteps()) {
			collectBridges(step, bridges);
		}
		return bridges;
	}

	private static void collectBridges(Step step, Map<Step, List<Step>> bridges) {
		var bridged = getBridgedSources(step);
		if (!bridged.isEmpty()) {
			bridges.put(step, bridged);
		}
		if (step instanceof StepWithExpressions block) {
			for (var child : block.getSteps()) {
				collectBridges(child, bridges);
			}
		}
	}

	/** the steps of the sequence before this one (in execution order) that give JSON to the next steps */
	public static List<Step> getPreviousJsonSteps(Step step) {
		var found = new ArrayList<Step>();
		var sequence = step.getSequence();
		if (sequence != null) {
			collectPrevious(sequence.getSteps(), step, found);
		}
		return found;
	}

	private static boolean collectPrevious(List<Step> steps, Step stop, List<Step> found) {
		for (var step : steps) {
			if (step == stop || step.priority == stop.priority) {
				return true;
			}
			if (isJsonProducer(step)) {
				found.add(step);
			}
			if (step instanceof StepWithExpressions block && collectPrevious(block.getSteps(), stop, found)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * the column names known for the rows of a step, as the list editors of the Eclipse Studio propose them, in the last
	 * samples of the steps its rows and its schema read: the keys of the rows, the names of the fields of a schema
	 */
	public static List<String> getColumnNames(Step step) {
		var names = new java.util.LinkedHashSet<String>();
		var rows = new ArrayList<String>();
		var schemas = new ArrayList<String>();
		if (step instanceof JsonTransformStep transform) {
			rows.add(transform.getRows());
		}
		if (step instanceof JsonStreamRowsStep stream) {
			rows.add(stream.getRows());
			schemas.add(stream.getSchema());
		}
		try {
			schemas.add((String) step.getClass().getMethod("getSchema").invoke(step));
		} catch (Exception e) {
			// no schema
		}
		for (var previous : getPreviousJsonSteps(step)) {
			var variable = getVariableName(previous);
			var schema = schemas.contains(variable);
			if (!schema && !rows.contains(variable)) {
				continue;
			}
			var sample = getSample(previous);
			if (sample == null || sample.getJson().isEmpty()) {
				continue;
			}
			try {
				var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(sample.getJson());
				if (node.isArray()) {
					for (int i = 0; i < Math.min(node.size(), 20); i++) {
						var item = node.get(i);
						if (schema && item.isObject() && item.has("name") && item.get("name").isTextual() && item.size() > 1
								&& !item.has("id")) {
							// a field of a schema
							names.add(item.get("name").asText());
						} else if (item.isObject()) {
							item.fieldNames().forEachRemaining(names::add);
						}
					}
				}
			} catch (Exception e) {
				// not JSON
			}
		}
		return new ArrayList<>(names);
	}

	/**
	 * the mark of the tree of the Eclipse Studio, without its brackets: a JSON native sequence with its XML bridges
	 * ("JSON native output, XML bridge: 2 steps"), an XML step that reads the XML view of native steps ("XML view of
	 * A, B"), a step that reads the JSON of steps ("JSON of C"); empty for another object
	 */
	public static List<String> treeMarks(Object dbo) {
		var marks = new ArrayList<String>();
		try {
			if (dbo instanceof Sequence sequence && isNativeSequence(sequence)) {
				int bridges = getBridges(sequence).size();
				marks.add("JSON native" + (sequence.isNativeExecution() ? "" : " output")
						+ (bridges > 0 ? ", XML bridge: " + bridges + " step" + (bridges > 1 ? "s" : "") : ""));
			} else if (dbo instanceof Step step) {
				for (boolean jsonPath : new boolean[] { false, true }) {
					var steps = jsonPath ? getJsonPathSources(step) : getBridgedSources(step);
					if (!steps.isEmpty()) {
						var names = new ArrayList<String>();
						for (var source : steps) {
							names.add(source.getName());
						}
						marks.add((jsonPath ? "JSON of " : "XML view of ") + String.join(", ", names));
					}
				}
			}
		} catch (Exception e) {
			// no mark
		}
		return marks;
	}

	/**
	 * the JSON path that reads, on the value of a native step, what a simple XPath reads on its XML view:
	 * <code>.</code>, <code>./key</code>, <code>./item[n]</code>, a last <code>text()</code>; null for another XPath
	 * (axes, functions, predicates on values, attributes, several items)
	 */
	public static String toJsonPath(String xpath) {
		if (xpath == null) {
			return null;
		}
		var x = xpath.trim();
		if (x.equals(".")) {
			return "$";
		}
		if (!x.startsWith("./")) {
			return null;
		}
		var segments = x.substring(2).split("/", -1);
		var path = new StringBuilder("$");
		for (int i = 0; i < segments.length; i++) {
			var segment = segments[i];
			var item = ITEM.matcher(segment);
			if (segment.equals("text()") && i == segments.length - 1 && i > 0) {
				break;
			} else if (item.matches() && Integer.parseInt(item.group(1)) > 0) {
				path.append('[').append(Integer.parseInt(item.group(1)) - 1).append(']');
			} else if (NAME.matcher(segment).matches() && !segment.equals("item")) {
				path.append('.').append(segment);
			} else {
				return null;
			}
		}
		return path.toString();
	}
}
