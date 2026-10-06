/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.twinsoft.convertigo.engine.tags.TagDocument;
import com.twinsoft.convertigo.engine.tags.TagManager;

/**
 * The tags of the selected objects or projects: check the existing tags of their scope, or create one for them.
 * The editor of a tag shows the other point of view, every member of that tag.
 */
public final class ObjectTagsDialog extends TitleAreaDialog {
	private final TagManager.Scope scope;
	private final String project;
	private final List<String> targets;
	private final Consumer<ObjectNode> refresh;
	private ObjectNode snapshot;
	private Text filter;
	private Table tags, order;
	private Group orderGroup;
	private Button create, edit, orderUp, orderDown;
	private Label status;
	private String orderedTag = "";
	private final List<Control> editControls = new ArrayList<>();
	private final Map<String, Color> colors = new HashMap<>();
	private boolean busy;

	public ObjectTagsDialog(Shell shell, TagManager.Scope scope, String project, List<String> targets, Consumer<ObjectNode> refresh) {
		super(shell); this.scope = scope; this.project = project; this.targets = List.copyOf(targets); this.refresh = refresh;
		setShellStyle(getShellStyle() | SWT.RESIZE); setBlockOnOpen(false);
	}
	@Override protected Point getInitialSize() { return new Point(520, 600); }
	@Override protected boolean isResizable() { return true; }
	// Close is not the default button: Return in the field creates or assigns the typed tag.
	@Override protected void createButtonsForButtonBar(Composite parent) { createButton(parent, CANCEL, "Close", false); }
	@Override public boolean close() { colors.values().forEach(Color::dispose); return super.close(); }

	@Override protected Control createDialogArea(Composite parent) {
		renderTitle();
		Composite area = (Composite) super.createDialogArea(parent), content = new Composite(area, SWT.NONE);
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true)); content.setLayout(new GridLayout(2, false));
		filter = new Text(content, SWT.SEARCH | SWT.BORDER); filter.setMessage("Find or create a tag"); filter.setTextLimit(256);
		filter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false)); editControls.add(filter);
		filter.addModifyListener(event -> renderTags());
		filter.addListener(SWT.Traverse, event -> { if (event.detail == SWT.TRAVERSE_RETURN) { event.doit = false; createTag(); } });
		filter.addListener(SWT.DefaultSelection, event -> createTag());
		create = button(content, "Create tag", this::createTag); create.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		tags = new Table(content, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION | SWT.SINGLE); editControls.add(tags);
		GridData tagsData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1); tagsData.heightHint = 200; tags.setLayoutData(tagsData);
		tags.addListener(SWT.Selection, event -> { if (event.item instanceof TableItem item) { if (event.detail == SWT.CHECK) toggle(item); updateButtons(); } });
		tags.addListener(SWT.DefaultSelection, event -> editTag());
		Composite actions = new Composite(content, SWT.NONE); actions.setLayout(new GridLayout(2, true)); actions.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		edit = button(actions, "Edit tag…", this::editTag);
		button(actions, "Manage tags…", () -> openEditor(""));
		if (targets.size() == 1) {
			orderGroup = new Group(content, SWT.NONE); orderGroup.setText("Tag order"); orderGroup.setLayout(new GridLayout(2, false));
			orderGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
			Label hint = new Label(orderGroup, SWT.WRAP); hint.setText("Applied from top to bottom. Later tags take precedence when an extension combines values.");
			hint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			order = new Table(orderGroup, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE); editControls.add(order);
			GridData orderData = new GridData(SWT.FILL, SWT.FILL, true, true); orderData.heightHint = 80; order.setLayoutData(orderData);
			order.addListener(SWT.Selection, event -> { if (event.item instanceof TableItem item) { orderedTag = (String) item.getData(); updateButtons(); } });
			Composite moves = new Composite(orderGroup, SWT.NONE); moves.setLayout(new GridLayout(1, false));
			orderUp = button(moves, "Move up", () -> moveOrder(-1)); orderDown = button(moves, "Move down", () -> moveOrder(1));
		}
		Composite footer = new Composite(content, SWT.NONE); footer.setLayout(new GridLayout(2, false)); footer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		status = new Label(footer, SWT.WRAP); status.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button reload = new Button(footer, SWT.PUSH); reload.setText("Refresh tags"); reload.addListener(SWT.Selection, event -> load());
		load(); return area;
	}

	private Button button(Composite parent, String caption, Runnable action) {
		Button button = new Button(parent, SWT.PUSH); button.setText(caption); button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button.addListener(SWT.Selection, event -> { if (!busy) action.run(); }); editControls.add(button); return button;
	}

	private String targetLabel(String target) {
		if (scope == TagManager.Scope.workspaceProjects) return target;
		JsonNode details = snapshot == null ? null : snapshot.path("targetDetails").path(target);
		return details == null || details.isMissingNode() ? target.replaceFirst("^.*\\.[a-z]{2}[:~]", "")
				: details.path("label").asText() + " (" + details.path("kind").asText() + ")";
	}
	private void renderTitle() {
		setTitle("Tags of " + (targets.size() == 1 ? targetLabel(targets.get(0))
				: targets.size() + (scope == TagManager.Scope.workspaceProjects ? " projects" : " objects")));
		setMessage((targets.size() == 1 ? "Check its tags" : "Check their tags") + ", or type a new label to create one. All the "
				+ (scope == TagManager.Scope.workspaceProjects ? "projects of this workspace" : "objects of " + project) + " share these tags.");
	}

	/** The selected targets having a tag: none, some or all of them. */
	private int members(String id) {
		int count = 0;
		for (String target : targets) for (var assigned : snapshot.path("assignments").path(target)) if (assigned.asText().equals(id)) count++;
		return count;
	}
	private int memberCount(String id) {
		int count = 0;
		for (var assigned : snapshot.path("assignments")) for (var tag : assigned) if (tag.asText().equals(id)) count++;
		return count;
	}
	private List<Map.Entry<String, JsonNode>> definitions() {
		var definitions = new ArrayList<Map.Entry<String, JsonNode>>(); snapshot.path("tags").fields().forEachRemaining(definitions::add);
		definitions.sort(java.util.Comparator.comparing((Map.Entry<String, JsonNode> entry) -> entry.getValue().path("label").asText().toLowerCase()).thenComparing(Map.Entry::getKey));
		return definitions;
	}
	private String exactMatch() {
		String text = filter.getText().trim();
		for (var entry : definitions()) if (entry.getValue().path("label").asText().equalsIgnoreCase(text)) return entry.getKey();
		return null;
	}

	private void render() {
		renderTitle(); setErrorMessage(null); renderTags(); renderOrder();
		StringBuilder message = new StringBuilder(scope == TagManager.Scope.workspaceProjects ? "Memberships are saved in this workspace."
				: snapshot.path("dirty").asBoolean() ? "Modified — save the project to keep these tags." : "Object tags are saved with the project.");
		for (var diagnostic : snapshot.path("diagnostics")) message.append('\n').append(diagnostic.asText());
		status.setText(message.toString());
		for (var control : editControls) if (!control.isDisposed()) control.setEnabled(!snapshot.path("readOnly").asBoolean());
		updateButtons(); getShell().layout(true, true);
	}
	private void renderTags() {
		if (snapshot == null || tags == null) return;
		String selected = tags.getSelectionCount() == 0 ? "" : (String) tags.getSelection()[0].getData();
		String search = filter.getText().trim().toLowerCase();
		var shown = definitions().stream().filter(entry -> entry.getValue().path("label").asText().toLowerCase().contains(search)).toList();
		// Keep existing rows so checking a tag does not reset scrolling or selection.
		tags.setItemCount(shown.size());
		for (int index = 0; index < shown.size(); index++) {
			var entry = shown.get(index); var item = tags.getItem(index); int members = members(entry.getKey());
			item.setData(entry.getKey());
			item.setText(entry.getValue().path("label").asText() + "  (" + memberCount(entry.getKey()) + ")");
			item.setChecked(members > 0); item.setGrayed(members > 0 && members < targets.size());
			item.setForeground(color(entry.getValue().path("presentation").path("color").asText()));
			if (entry.getKey().equals(selected)) tags.setSelection(index);
		}
		updateButtons();
	}
	private void renderOrder() {
		if (order == null || snapshot == null) return;
		order.removeAll(); int index = 0;
		for (var id : snapshot.path("assignments").path(targets.get(0))) {
			var item = new TableItem(order, SWT.NONE); item.setData(id.asText());
			item.setText(++index + ". " + snapshot.path("tags").path(id.asText()).path("label").asText());
			if (id.asText().equals(orderedTag)) order.setSelection(item);
		}
		// An order matters from two tags.
		boolean shown = order.getItemCount() > 1;
		((GridData) orderGroup.getLayoutData()).exclude = !shown; orderGroup.setVisible(shown);
	}
	private void updateButtons() {
		boolean editable = !busy && snapshot != null && !snapshot.path("readOnly").asBoolean();
		if (create != null) create.setEnabled(editable && !filter.getText().trim().isEmpty() && exactMatch() == null);
		if (edit != null) edit.setEnabled(!busy && tags.getSelectionCount() > 0);
		if (order != null) {
			int index = order.getSelectionIndex();
			orderUp.setEnabled(editable && index > 0); orderDown.setEnabled(editable && index >= 0 && index < order.getItemCount() - 1);
		}
	}
	private Color color(String value) {
		if (!value.matches("#[0-9A-Fa-f]{6}")) return null;
		return colors.computeIfAbsent(value, color -> new Color(getShell().getDisplay(), new RGB(Integer.parseInt(color.substring(1, 3), 16),
				Integer.parseInt(color.substring(3, 5), 16), Integer.parseInt(color.substring(5, 7), 16))));
	}

	private void toggle(TableItem item) {
		String id = (String) item.getData();
		// A tag of only some of the selected targets is added to all of them.
		boolean assign = members(id) < targets.size();
		ObjectNode input = TagDocument.JSON.createObjectNode(); var array = input.putArray("targets"); targets.forEach(array::add); input.putArray("tagIds").add(id);
		command(assign ? "assign" : "remove", input);
	}
	private void createTag() {
		String label = filter.getText().trim();
		if (busy || snapshot == null || snapshot.path("readOnly").asBoolean() || label.isEmpty()) return;
		String existing = exactMatch();
		if (existing != null) {
			if (members(existing) < targets.size()) { ObjectNode input = TagDocument.JSON.createObjectNode(); var array = input.putArray("targets"); targets.forEach(array::add); input.putArray("tagIds").add(existing); command("assign", input); }
			return;
		}
		String revision = snapshot.path("revision").asText();
		run("Create tag", () -> {
			ObjectNode definition = TagDocument.JSON.createObjectNode().put("label", label); definition.putObject("metadata");
			ObjectNode created = TagManager.get().mutate(scope, project, revision, "create", (ObjectNode) TagDocument.JSON.createObjectNode().set("definition", definition));
			ObjectNode input = TagDocument.JSON.createObjectNode(); var array = input.putArray("targets"); targets.forEach(array::add); input.putArray("tagIds").add(created.path("id").asText());
			return TagManager.get().mutate(scope, project, created.path("revision").asText(), "assign", input);
		}, () -> filter.setText(""));
	}
	private void moveOrder(int offset) {
		int index = order.getSelectionIndex(); if (busy || index < 0 || index + offset < 0 || index + offset >= order.getItemCount()) return;
		var ids = new ArrayList<String>(); for (var item : order.getItems()) ids.add((String) item.getData());
		java.util.Collections.swap(ids, index, index + offset);
		ObjectNode input = TagDocument.JSON.createObjectNode(); input.putArray("targets").add(targets.get(0)); var array = input.putArray("tagIds"); ids.forEach(array::add);
		command("reorder", input);
	}
	private void editTag() { if (!busy && tags.getSelectionCount() > 0) openEditor((String) tags.getSelection()[0].getData()); }
	/** The editor of a tag: its definition and all its members. Its changes are shown here too. */
	private void openEditor(String id) {
		var editor = new TagManagerDialog(getShell(), scope, project, targets, result -> { refresh.accept(result); if (getShell() != null && !getShell().isDisposed()) load(); });
		if (!id.isEmpty()) editor.selectTag(id);
		editor.open();
	}

	private void load() { run("Read tags", () -> TagManager.get().read(scope, project), null); }
	private void command(String action, ObjectNode input) {
		if (snapshot == null || snapshot.path("readOnly").asBoolean()) return;
		String revision = snapshot.path("revision").asText();
		run("Update tags", () -> TagManager.get().mutate(scope, project, revision, action, input), null);
	}
	@FunctionalInterface private interface Work { ObjectNode run() throws Exception; }
	private void run(String name, Work work, Runnable done) {
		if (busy) return; busy = true; Display display = getShell().getDisplay();
		for (var control : editControls) if (!control.isDisposed()) control.setEnabled(false);
		Job.create(name, monitor -> {
			try {
				ObjectNode result = work.run();
				display.asyncExec(() -> { if (getShell() == null || getShell().isDisposed()) return; busy = false; snapshot = result; if (done != null) done.run(); render();
					if (result.path("done").asBoolean()) refresh.accept(result); });
			} catch (Exception e) {
				display.asyncExec(() -> { if (getShell() == null || getShell().isDisposed()) return; busy = false;
					if (snapshot != null) render(); else for (var control : editControls) if (!control.isDisposed()) control.setEnabled(true);
					setErrorMessage(e.getMessage()); });
			}
		}).schedule();
	}
}
