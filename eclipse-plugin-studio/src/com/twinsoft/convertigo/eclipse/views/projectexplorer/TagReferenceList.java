/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.twinsoft.convertigo.engine.tags.TagDocument;

/** Data-only editor for a descriptor's ordered, unique string references. No extension-specific UI. */
final class TagReferenceList extends Composite {
	private final List<String> choices = new ArrayList<>(), values = new ArrayList<>();
	private final Combo choice;
	private final Table table;
	private final Button add, remove, up, down;
	private final Label warning;

	TagReferenceList(Composite parent, JsonNode descriptor, JsonNode initial) {
		super(parent, SWT.NONE);
		setLayout(new GridLayout(2, false));
		for (var value : descriptor.path("items").path("enum")) choices.add(value.asText());
		if (initial.isArray()) for (var value : initial) values.add(value.asText());
		choice = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY); choice.setToolTipText("Choose a reference");
		choice.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		add = button(this, "Add", () -> { if (!choice.getText().isEmpty() && !values.contains(choice.getText())) { values.add(choice.getText()); render(values.size() - 1); } });
		choice.addListener(SWT.Selection, event -> updateButtons());
		table = new Table(this, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		var data = new GridData(SWT.FILL, SWT.FILL, true, true); data.heightHint = 80; data.widthHint = 220; table.setLayoutData(data);
		Composite buttons = new Composite(this, SWT.NONE); buttons.setLayout(new GridLayout(1, false));
		remove = button(buttons, "Remove", () -> { int index = table.getSelectionIndex(); if (index >= 0) { values.remove(index); render(Math.min(index, values.size() - 1)); } });
		up = button(buttons, "Move up", () -> move(-1));
		down = button(buttons, "Move down", () -> move(1));
		table.addListener(SWT.Selection, event -> updateButtons());
		warning = new Label(this, SWT.WRAP); warning.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		render(-1);
	}
	private Button button(Composite parent, String label, Runnable action) {
		var button = new Button(parent, SWT.PUSH); button.setText(label);
		button.addListener(SWT.Selection, event -> { if (isEnabled()) action.run(); });
		return button;
	}
	private void move(int offset) {
		int index = table.getSelectionIndex(), next = index + offset;
		if (index < 0 || next < 0 || next >= values.size()) return;
		java.util.Collections.swap(values, index, next); render(next);
	}
	private void render(int selected) {
		table.removeAll();
		for (int index = 0; index < values.size(); index++) {
			var item = new TableItem(table, SWT.NONE); item.setText((index + 1) + ". " + values.get(index));
		}
		if (selected >= 0) table.select(selected);
		String previous = choice.getText(); choice.removeAll();
		for (String value : choices) if (!values.contains(value)) choice.add(value);
		if (!previous.isEmpty()) choice.setText(previous);
		List<String> missing = values.stream().filter(value -> !choices.contains(value)).toList();
		warning.setText(missing.isEmpty() ? "" : "Unavailable references: " + String.join(", ", missing) + ". Remove them or restore their definitions.");
		updateButtons(); layout(true, true);
	}
	private void updateButtons() {
		int index = table.getSelectionIndex(); boolean enabled = isEnabled();
		choice.setEnabled(enabled && choice.getItemCount() > 0); table.setEnabled(enabled);
		add.setEnabled(enabled && !choice.getText().isEmpty());
		remove.setEnabled(enabled && index >= 0); up.setEnabled(enabled && index > 0);
		down.setEnabled(enabled && index >= 0 && index < values.size() - 1);
	}
	@Override public void setEnabled(boolean enabled) { super.setEnabled(enabled); if (table != null) updateButtons(); }
	ArrayNode value() { var array = TagDocument.JSON.createArrayNode(); values.forEach(array::add); return array; }
}
