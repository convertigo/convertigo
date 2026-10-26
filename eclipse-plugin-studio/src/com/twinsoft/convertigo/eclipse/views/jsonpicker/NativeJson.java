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

package com.twinsoft.convertigo.eclipse.views.jsonpicker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.steps.JsonStreamRowsStep;
import com.twinsoft.convertigo.beans.steps.JsonToXmlStep;
import com.twinsoft.convertigo.beans.steps.JsonTransformStep;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineListener;
import com.twinsoft.convertigo.engine.json.NativeStepOutput;

/**
 * The JSON native sequences in the Studio: which sequences and steps are native, which XML steps read a native step
 * through its XML view, and the last JSON each native step produced in a Studio execution (the samples of the JSON
 * picker and of the XML view of the source picker).
 */
public class NativeJson {
	/** the last outputs by step (project, sequence, priority): the steps of an execution are copies of the project ones */
	private static final Map<String, NativeStepOutput> samples = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, NativeStepOutput> eldest) {
			return size() > 64;
		}
	});
	private static final List<Consumer<NativeStepOutput>> listeners = new ArrayList<>();
	private static boolean registered = false;

	private NativeJson() {
	}

	/** listens to the engine once, from the first use */
	public static synchronized void register() {
		if (!registered && Engine.theApp != null) {
			registered = true;
			Engine.theApp.addEngineListener(new EngineListener() {
				public void blocksChanged(EngineEvent e) {}
				public void objectDetected(EngineEvent e) {}
				public void documentGenerated(EngineEvent e) {}
				public void stepReached(EngineEvent e) {}
				public void transactionStarted(EngineEvent e) {}
				public void transactionFinished(EngineEvent e) {}
				public void sequenceStarted(EngineEvent e) {}
				public void sequenceFinished(EngineEvent e) {}
				public void clearEditor(EngineEvent e) {}

				@Override
				public void nativeStepExecuted(EngineEvent e) {
					if (e.getSource() instanceof NativeStepOutput output) {
						if (output.getNote() == null || !output.getJson().isEmpty()) {
							samples.put(keyOf(output.getStep()), output);
						}
						Consumer<NativeStepOutput>[] ls;
						synchronized (listeners) {
							ls = GenericListeners.toArray(listeners);
						}
						for (Consumer<NativeStepOutput> l : ls) {
							l.accept(output);
						}
					}
				}
			});
		}
	}

	public static void addListener(Consumer<NativeStepOutput> listener) {
		register();
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	public static void removeListener(Consumer<NativeStepOutput> listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	/** the JSON the step produced at its last execution in the Studio, or null */
	public static NativeStepOutput getSample(Step step) {
		register();
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

	/** the step of the project for a step executed (a copy) */
	public static Step getProjectStep(Step step) {
		try {
			Sequence sequence = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(step.getProject().getName())
					.getSequenceByName(step.getSequence().getName());
			Step found = findByPriority(sequence.getSteps(), step.priority);
			return found != null ? found : step;
		} catch (Exception e) {
			return step;
		}
	}

	private static Step findByPriority(List<Step> steps, long priority) {
		for (Step step : steps) {
			if (step.priority == priority) {
				return step;
			}
			if (step instanceof StepWithExpressions block) {
				Step found = findByPriority(block.getSteps(), priority);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
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

	/** the variable (or response key) a JSON step gives its value to */
	public static String getVariableName(Step step) {
		if (step instanceof JsonTransformStep transform) {
			return transform.getVariableName();
		}
		NativeStepOutput sample = getSample(step);
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
		List<Step> found = new ArrayList<>();
		if (step == null || isNativeStep(step)) {
			return found;
		}
		try {
			for (StepSource source : step.getSources()) {
				if (source.isJsonPath() != jsonPath) {
					continue;
				}
				Step sourceStep = source.getStep();
				// a JSON path also reads the JSON text answered by a raw HTTP transaction (RequestableStep)
				if ((jsonPath ? isJsonProducer(sourceStep) || sourceStep instanceof com.twinsoft.convertigo.beans.core.RequestableStep
						: sourceStep instanceof JsonTransformStep) && !found.contains(sourceStep)) {
					found.add(sourceStep);
				}
			}
		} catch (Exception e) {
			// a broken source: nothing found
		}
		return found;
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
		String x = xpath.trim();
		if (x.equals(".")) {
			return "$";
		}
		if (!x.startsWith("./")) {
			return null;
		}
		String[] segments = x.substring(2).split("/", -1);
		StringBuilder path = new StringBuilder("$");
		for (int i = 0; i < segments.length; i++) {
			String segment = segments[i];
			java.util.regex.Matcher item = java.util.regex.Pattern.compile("item\\[(\\d+)\\]").matcher(segment);
			if (segment.equals("text()") && i == segments.length - 1 && i > 0) {
				break;
			} else if (item.matches() && Integer.parseInt(item.group(1)) > 0) {
				path.append('[').append(Integer.parseInt(item.group(1)) - 1).append(']');
			} else if (segment.matches("[A-Za-z_][A-Za-z0-9_\\-]*") && !segment.equals("item")) {
				path.append('.').append(segment);
			} else {
				return null;
			}
		}
		return path.toString();
	}

	/** the XML steps of a sequence that read a native step through its XML view, with the native steps they read */
	public static Map<Step, List<Step>> getBridges(Sequence sequence) {
		Map<Step, List<Step>> bridges = new LinkedHashMap<>();
		for (Step step : sequence.getSteps()) {
			collectBridges(step, bridges);
		}
		return bridges;
	}

	private static void collectBridges(Step step, Map<Step, List<Step>> bridges) {
		List<Step> bridged = getBridgedSources(step);
		if (!bridged.isEmpty()) {
			bridges.put(step, bridged);
		}
		if (step instanceof StepWithExpressions block) {
			for (Step child : block.getSteps()) {
				collectBridges(child, bridges);
			}
		}
	}

	/** the steps of the sequence before this one (in execution order) that give JSON to the next steps */
	public static List<Step> getPreviousJsonSteps(Step step) {
		List<Step> found = new ArrayList<>();
		Sequence sequence = step.getSequence();
		if (sequence != null) {
			collectPrevious(sequence.getSteps(), step, found);
		}
		return found;
	}

	private static boolean collectPrevious(List<Step> steps, Step stop, List<Step> found) {
		for (Step step : steps) {
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

	/** small helper to copy the listeners without unchecked warnings at each use */
	private static class GenericListeners {
		@SuppressWarnings("unchecked")
		static Consumer<NativeStepOutput>[] toArray(List<Consumer<NativeStepOutput>> list) {
			return list.toArray(new Consumer[list.size()]);
		}
	}
}
