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

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.beans.steps.JsonTransformStep;
import com.twinsoft.convertigo.beans.steps.SimpleSourceStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.jsonpicker.NativeJson;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.StepTreeObject;
import com.twinsoft.convertigo.engine.json.JsonPath;
import com.twinsoft.convertigo.engine.json.NativeStepOutput;

/**
 * Turns the XPath source of a step on a native step into the JSON path that reads the same value without the XML
 * view of the native step, when the XPath is simple enough for the result to be the same.
 */
public class ConvertSourceToJsonPathAction extends MyAbstractAction {
	private String jsonPath;

	public ConvertSourceToJsonPathAction() {
		super();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		jsonPath = null;
		String text = "Convert source to JSON path";
		Object first = ((IStructuredSelection) selection).getFirstElement();
		if (first instanceof StepTreeObject stepTreeObject && stepTreeObject.getObject() instanceof IStepSourceContainer container) {
			Step step = stepTreeObject.getObject();
			StepSource source = new StepSource(step, container.getSourceDefinition());
			try {
				if (source.isEmpty() || !(source.getStep() instanceof JsonTransformStep)) {
					text += " (only for a source on a native JSON step)";
				} else if (source.isJsonPath()) {
					text += " (already " + source.getXpath() + ")";
				} else if ((jsonPath = NativeJson.toJsonPath(source.getXpath())) == null) {
					text += " (XPath " + source.getXpath() + " has no exact JSON path)";
				} else if (step instanceof SimpleSourceStep && !isScalar(source.getStep(), jsonPath)) {
					// the text of an element and the JSON of an object differ: only for a value read as text
					text += " (execute the sequence first: the value must be a text, a number or a boolean)";
					jsonPath = null;
				} else {
					text += ": " + source.getXpath() + " → " + jsonPath;
				}
			} catch (Exception e) {
				jsonPath = null;
				text += " (broken source)";
			}
		}
		action.setText(text);
		action.setEnabled(jsonPath != null);
	}

	private static boolean isScalar(Step nativeStep, String path) {
		NativeStepOutput sample = NativeJson.getSample(nativeStep);
		if (sample == null || sample.getJson().isEmpty()) {
			return false;
		}
		try {
			String text = JsonPath.parse(path).selectText(sample.getJson());
			return text != null && !text.startsWith("{") && !text.startsWith("[");
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void run() {
		try {
			ProjectExplorerView explorerView = getProjectExplorerView();
			if (explorerView != null && jsonPath != null
					&& explorerView.getFirstSelectedTreeObject() instanceof StepTreeObject stepTreeObject
					&& stepTreeObject.getObject() instanceof IStepSourceContainer container) {
				XMLVector<String> definition = container.getSourceDefinition();
				String oldValue = definition.toString();
				definition.set(1, jsonPath);
				container.setSourceDefinition(definition);
				Step step = stepTreeObject.getObject();
				step.hasChanged = true;
				stepTreeObject.hasBeenModified(true);
				explorerView.refreshTreeObject(stepTreeObject);
				explorerView.fireTreeObjectPropertyChanged(new TreeObjectEvent(stepTreeObject, "sourceDefinition", oldValue, definition.toString()));
				ConvertigoPlugin.logInfo("Source of '" + step.getName() + "' read by the JSON path " + jsonPath);
			}
		} catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to convert the source to a JSON path!");
		}
	}
}
