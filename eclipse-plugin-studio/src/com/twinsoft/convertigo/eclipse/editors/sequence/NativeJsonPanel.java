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

package com.twinsoft.convertigo.eclipse.editors.sequence;

import java.beans.BeanInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.jsonpicker.JsonTree;
import com.twinsoft.convertigo.eclipse.views.jsonpicker.NativeJson;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewImageProvider;
import com.twinsoft.convertigo.engine.json.NativeStepOutput;

/**
 * The JSON of the native steps of a sequence executed in the Studio: one row per execution of a native step (in
 * order, loops included), and the JSON it produced, as a tree or as a text. In debug mode, the row of the step just
 * executed is shown while the sequence waits before the next step.
 */
public class NativeJsonPanel extends Composite {
	private static final ObjectMapper mapper = new ObjectMapper();
	/** above this size the text tab shows the JSON as it is, not indented */
	private static final int MAX_PRETTY = 2 * 1024 * 1024;

	private final Sequence sequence;
	private final List<NativeStepOutput> outputs = new ArrayList<>();
	private Label header;
	private Table table;
	private JsonTree tree;
	private StyledText text;
	private CTabFolder tabs;
	private Label bridges;

	public NativeJsonPanel(Composite parent, Sequence sequence) {
		super(parent, SWT.NONE);
		this.sequence = sequence;
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = layout.marginWidth = 2;
		setLayout(layout);

		header = new Label(this, SWT.NONE);
		header.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		SashForm sash = new SashForm(this, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		table = new Table(sash, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		for (String[] c : new String[][] { { "#", "30" }, { "Native step", "160" }, { "Variable", "90" }, { "Items", "60" }, { "Time", "70" } }) {
			TableColumn column = new TableColumn(table, c[0].equals("Items") || c[0].equals("Time") ? SWT.RIGHT : SWT.NONE);
			column.setText(c[0]);
			column.setWidth(Integer.parseInt(c[1]));
		}
		table.addListener(SWT.Selection, e -> show());
		table.addListener(SWT.DefaultSelection, e -> revealStep());

		tabs = new CTabFolder(sash, SWT.BORDER | SWT.BOTTOM);
		CTabItem treeTab = new CTabItem(tabs, SWT.NONE);
		treeTab.setText("JSON tree");
		tree = new JsonTree(tabs, SWT.NONE);
		treeTab.setControl(tree);
		CTabItem textTab = new CTabItem(tabs, SWT.NONE);
		textTab.setText("JSON text");
		text = new StyledText(tabs, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
		text.setFont(org.eclipse.jface.resource.JFaceResources.getTextFont());
		text.setBackground(table.getBackground());
		text.setForeground(table.getForeground());
		textTab.setControl(text);
		tabs.setSelection(treeTab);
		tabs.addListener(SWT.Selection, e -> show());
		sash.setWeights(new int[] { 40, 60 });

		bridges = new Label(this, SWT.WRAP);
		bridges.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		clear();
	}

	/** before an execution */
	public void clear() {
		outputs.clear();
		table.removeAll();
		tree.setJson("", null);
		text.setText("");
		header.setText("Native JSON steps: execute the sequence to see the JSON each native step produces (no XML is built for them).");
		updateBridges();
	}

	/** the XML steps of the sequence that read a native step by XPath: each one gets an XML view of its JSON */
	public void updateBridges() {
		Map<Step, List<Step>> map = NativeJson.getBridges(sequence);
		if (map.isEmpty()) {
			bridges.setText("No XML step reads a native step: no XML is built for the native steps.");
		} else {
			StringBuilder sb = new StringBuilder("XML bridge: ");
			map.forEach((xml, natives) -> {
				if (sb.length() > 12) {
					sb.append(", ");
				}
				sb.append(xml.getName()).append(" reads the XML view of ");
				natives.forEach(n -> sb.append(n.getName()).append(' '));
			});
			bridges.setText(sb.toString().trim() + ". These views are built at their first read, for these steps only.");
		}
		layout(true, true);
	}

	public void add(NativeStepOutput output) {
		outputs.add(output);
		TableItem item = new TableItem(table, SWT.NONE);
		Step step = output.getStep();
		item.setImage(1, ViewImageProvider.getImageFromCache(MySimpleBeanInfo.getIconName(step, BeanInfo.ICON_COLOR_16x16), step));
		String items = output.getNote() != null && output.getJson().isEmpty() ? output.getNote()
				: output.getCount() >= 0 ? Integer.toString(output.getCount()) : "";
		long micros = output.getDurationMicros();
		item.setText(new String[] { Integer.toString(outputs.size()), step.getName(), output.getVariableName(), items,
				micros <= 0 ? "" : micros < 10000 ? micros + " µs" : (micros / 1000) + " ms" });
		table.setSelection(item);
		table.showItem(item);
		header.setText("Native JSON steps: " + outputs.size() + " execution" + (outputs.size() > 1 ? "s" : "")
				+ ", last '" + step.getName() + "' into '" + output.getVariableName() + "'.");
		show();
	}

	/** in debug mode, the step the sequence waits before */
	public void setPausedBefore(Step step) {
		header.setText((outputs.isEmpty() ? "Native JSON steps: none executed yet" : "Native JSON steps: " + outputs.size() + " executed")
				+ "; paused before '" + step.getName() + "'.");
	}

	private NativeStepOutput selected() {
		int i = table.getSelectionIndex();
		return i >= 0 && i < outputs.size() ? outputs.get(i) : null;
	}

	private void show() {
		NativeStepOutput output = selected();
		if (output == null) {
			return;
		}
		String json = output.getJson();
		if (json.isEmpty()) {
			tree.setJson(output.getVariableName(), null);
			text.setText("'" + output.getStep().getName() + "' writes its rows in stream into the response: they are in the JSON output of the sequence.");
			return;
		}
		if (tabs.getSelectionIndex() == 0) {
			tree.setJson(output.getVariableName(), json);
		} else {
			String shown = json;
			if (json.length() <= MAX_PRETTY) {
				try {
					shown = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(json));
				} catch (Exception e) {
					// shown as it is
				}
			}
			text.setText(shown + (output.isTruncated() ? "\n… (cut at " + NativeStepOutput.MAX_LENGTH + " characters)" : ""));
		}
	}

	private void revealStep() {
		NativeStepOutput output = selected();
		if (output == null) {
			return;
		}
		ProjectExplorerView explorer = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (explorer != null) {
			TreeObject treeObject = explorer.findTreeObjectByUserObject(NativeJson.getProjectStep(output.getStep()));
			if (treeObject != null) {
				explorer.setSelectedTreeObject(treeObject);
			}
		}
	}

	public boolean hasOutputs() {
		return !outputs.isEmpty();
	}
}
