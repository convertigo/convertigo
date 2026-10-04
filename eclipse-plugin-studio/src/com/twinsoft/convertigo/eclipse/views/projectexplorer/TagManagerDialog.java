/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.twinsoft.convertigo.engine.tags.TagDocument;
import com.twinsoft.convertigo.engine.tags.TagManager;

/** Select one tag and check its members; optional configuration stays collapsed. */
public final class TagManagerDialog extends TitleAreaDialog {
	private final TagManager.Scope scope;
	private final String project;
	private final List<String> initialTargets;
	private final java.util.function.Consumer<ObjectNode> refresh;
	private ObjectNode snapshot, definition = TagDocument.JSON.createObjectNode();
	private String selected = "";
	private String referenceProject = "";
	private Table tags, targets, suggestions, orderedTags;
	private Combo orderTarget;
	private final List<String> orderTargets = new ArrayList<>();
	private Button orderUp, orderDown;
	private String orderedTarget = "", orderedTag = "";
	private Text filter, label, description;
	private Composite details, metadata, conflicts, sharingContent, advancedContent;
	private ScrolledComposite scroll;
	private ExpandBar optional;
	private ExpandItem sharingItem, advancedItem;
	private Label status, publication, identity;
	private Button shared, update, colorButton;
	private org.eclipse.swt.graphics.Color tagColor;
	private final List<Control> editControls = new ArrayList<>();
	private final Map<String, Control> metadataEditors = new TreeMap<>();
	private boolean busy;

	public TagManagerDialog(Shell shell, TagManager.Scope scope, String project, List<String> targets, java.util.function.Consumer<ObjectNode> refresh) {
		super(shell); this.scope = scope; this.project = project; this.initialTargets = List.copyOf(targets); this.refresh = refresh;
		orderedTarget = targets.isEmpty() ? "" : targets.get(0);
		setShellStyle(getShellStyle() | SWT.RESIZE); setBlockOnOpen(false);
	}
	public TagManagerDialog selectTag(String id) { selected = id; return this; }
	public TagManagerDialog createFromReferences(String name) { referenceProject = name; selected = ""; return this; }
	@Override protected Point getInitialSize() { return new Point(780, 620); }
	@Override protected Control createDialogArea(Composite parent) {
		setTitle(scope == TagManager.Scope.workspaceProjects ? "Project tags" : "Sequence tags — " + project);
		setMessage("Select a tag, then check the " + (scope == TagManager.Scope.workspaceProjects ? "projects" : "sequences") + " that belong to it.");
		Composite area = (Composite) super.createDialogArea(parent), content = new Composite(area, SWT.NONE);
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true)); content.setLayout(new GridLayout(1, false));
		SashForm columns = new SashForm(content, SWT.HORIZONTAL); columns.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Composite left = new Composite(columns, SWT.NONE); left.setLayout(new GridLayout(1, false));
		filter = new Text(left, SWT.SEARCH | SWT.BORDER); filter.setMessage("Find a tag"); filter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false)); filter.addModifyListener(event -> renderTags());
		tags = table(left, SWT.SINGLE, 200); tags.addListener(SWT.Selection, event -> { if (!busy && event.item instanceof TableItem item) edit((String) item.getData()); });
		button(left, "New tag", () -> { selected = referenceProject = ""; tags.deselectAll(); definition = TagDocument.JSON.createObjectNode().put("label", ""); renderEditor(); label.setFocus(); });
		scroll = new ScrolledComposite(columns, SWT.V_SCROLL); scroll.setExpandHorizontal(true); scroll.setExpandVertical(true);
		details = new Composite(scroll, SWT.NONE); details.setLayout(new GridLayout(1, false)); scroll.setContent(details);
		Composite nameRow = new Composite(details, SWT.NONE); nameRow.setLayout(new GridLayout(2, false)); nameRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		new Label(nameRow, SWT.NONE).setText("Label"); new Label(nameRow, SWT.NONE).setText("Color");
		label = text(nameRow, SWT.SINGLE); label.setTextLimit(256);
		colorButton = button(nameRow, "Choose…", () -> { ColorDialog picker = new ColorDialog(getShell()); var rgb = picker.open(); if (rgb != null) { definition.withObject("presentation").put("color", String.format("#%02X%02X%02X", rgb.red, rgb.green, rgb.blue)); renderColor(); } });
		GridData colorData = new GridData(SWT.LEFT, SWT.CENTER, false, false); colorData.widthHint = 95; colorButton.setLayoutData(colorData);
		new Label(details, SWT.NONE).setText("Description"); description = text(details, SWT.MULTI | SWT.WRAP); description.setTextLimit(4096); ((GridData) description.getLayoutData()).heightHint = 48;
		update = button(details, "Update tag", this::saveDefinition); update.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		Group memberGroup = new Group(details, SWT.NONE); memberGroup.setText(scope == TagManager.Scope.workspaceProjects ? "Projects" : "Sequences"); memberGroup.setLayout(new GridLayout(1, false)); memberGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		targets = table(memberGroup, SWT.CHECK | SWT.SINGLE, 130); editControls.add(targets);
		targets.addListener(SWT.Selection, event -> { if (!busy && event.item instanceof TableItem item) {
			orderedTarget = (String) item.getData(); renderOrder();
			if (event.detail == SWT.CHECK && !selected.isEmpty()) {
				ObjectNode input = TagDocument.JSON.createObjectNode(); input.putArray("targets").add(orderedTarget); input.putArray("tagIds").add(selected); command(item.getChecked() ? "assign" : "remove", input);
			}
		} });
		Group orderGroup = new Group(details, SWT.NONE); orderGroup.setText("Tag order"); orderGroup.setLayout(new GridLayout(2, false)); orderGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		new Label(orderGroup, SWT.NONE).setText(scope == TagManager.Scope.workspaceProjects ? "Project" : "Sequence");
		orderTarget = new Combo(orderGroup, SWT.DROP_DOWN | SWT.READ_ONLY); orderTarget.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false)); editControls.add(orderTarget);
		orderTarget.addListener(SWT.Selection, event -> { if (!busy && orderTarget.getSelectionIndex() >= 0) { orderedTarget = orderTargets.get(orderTarget.getSelectionIndex()); renderOrder(); } });
		Label orderHint = new Label(orderGroup, SWT.WRAP); orderHint.setText("Applied from top to bottom. Later tags take precedence when an extension combines values."); orderHint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		orderedTags = table(orderGroup, SWT.SINGLE, 80); editControls.add(orderedTags);
		orderedTags.addListener(SWT.Selection, event -> { if (event.item instanceof TableItem item) { orderedTag = (String) item.getData(); updateOrderButtons(); } });
		Composite orderButtons = new Composite(orderGroup, SWT.NONE); orderButtons.setLayout(new GridLayout(1, false));
		orderUp = button(orderButtons, "Move up", () -> moveOrder(-1)); orderDown = button(orderButtons, "Move down", () -> moveOrder(1));
		optional = new ExpandBar(details, SWT.NONE); optional.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		if (scope == TagManager.Scope.workspaceProjects) {
			sharingContent = new Composite(optional, SWT.NONE); sharingContent.setLayout(new GridLayout(1, false));
			shared = new Button(sharingContent, SWT.CHECK); shared.setText("Share with other workspaces"); editControls.add(shared);
			shared.addListener(SWT.Selection, event -> { if (selected.isEmpty() || busy) return;
				if (!MessageDialog.openConfirm(getShell(), "Share project tags", "Shared tags travel with their projects. Prepare changes for " + memberNames() + "? Save each modified project to publish them.")) { shared.setSelection(definition.path("shared").asBoolean()); return; }
				command("share", TagDocument.JSON.createObjectNode().put("id", selected).put("shared", shared.getSelection()).put("confirmed", true));
			});
			publication = new Label(sharingContent, SWT.WRAP); publication.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			button(sharingContent, "Prepare republication", () -> { if (!selected.isEmpty() && definition.path("shared").asBoolean() && MessageDialog.openConfirm(getShell(), "Republish", "Prepare declarations for " + memberNames() + "? Save each project afterward.")) command("republish", TagDocument.JSON.createObjectNode().put("id", selected).put("confirmed", true)); });
			conflicts = new Composite(sharingContent, SWT.NONE); conflicts.setLayout(new GridLayout(1, false)); conflicts.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			sharingItem = new ExpandItem(optional, SWT.NONE); sharingItem.setText("Sharing"); sharingItem.setControl(sharingContent);
		}
		advancedContent = new Composite(optional, SWT.NONE); advancedContent.setLayout(new GridLayout(1, false));
		metadata = new Composite(advancedContent, SWT.NONE); metadata.setLayout(new GridLayout(2, false)); metadata.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		identity = new Label(advancedContent, SWT.WRAP); identity.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		button(advancedContent, "Delete tag…", () -> { if (!selected.isEmpty() && MessageDialog.openConfirm(getShell(), "Delete tag", "Remove this tag and its " + memberCount() + " membership(s)? Objects remain intact.")) command("delete", TagDocument.JSON.createObjectNode().put("id", selected).put("memberCount", memberCount()).put("confirmed", true)); });
		suggestions = table(advancedContent, SWT.SINGLE, 70);
		button(advancedContent, "Copy selected suggestion", () -> { if (suggestions.getSelectionCount() == 0) return; ObjectNode input = TagDocument.JSON.createObjectNode(); input.set("definition", ((JsonNode) suggestions.getSelection()[0].getData()).path("definition")); command("create", input); });
		advancedItem = new ExpandItem(optional, SWT.NONE); advancedItem.setText("Advanced"); advancedItem.setControl(advancedContent);
		optional.addListener(SWT.Expand, event -> getShell().getDisplay().asyncExec(this::layoutDetails)); optional.addListener(SWT.Collapse, event -> getShell().getDisplay().asyncExec(this::layoutDetails));
		scroll.addListener(SWT.Resize, event -> layoutDetails()); columns.setWeights(32, 68);
		Composite footer = new Composite(content, SWT.NONE); footer.setLayout(new GridLayout(2, false)); footer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		status = new Label(footer, SWT.WRAP); status.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button reload = new Button(footer, SWT.PUSH); reload.setText("Refresh tags"); reload.addListener(SWT.Selection, event -> load());
		load(); return area;
	}
	@Override protected void createButtonsForButtonBar(Composite parent) { createButton(parent, CANCEL, "Close", true); }
	@Override protected boolean isResizable() { return true; }
	@Override public boolean close() { if (tagColor != null) tagColor.dispose(); return super.close(); }
	private Table table(Composite parent, int style, int height) { Table table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION | style); GridData data = new GridData(SWT.FILL, SWT.FILL, true, true); data.heightHint = height; data.widthHint = 180; table.setLayoutData(data); return table; }
	private Text text(Composite parent, int style) { Text text = new Text(parent, SWT.BORDER | style); text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false)); editControls.add(text); return text; }
	private Button button(Composite parent, String caption, Runnable action) { Button button = new Button(parent, SWT.PUSH); button.setText(caption); button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false)); button.addListener(SWT.Selection, event -> { if (!busy) action.run(); }); editControls.add(button); return button; }
	private void layoutDetails() {
		if (details == null || details.isDisposed()) return;
		for (ExpandItem item : optional.getItems()) item.setHeight(item.getControl().computeSize(Math.max(220, scroll.getClientArea().width - 20), SWT.DEFAULT).y);
		int height = 0; for (ExpandItem item : optional.getItems()) height += item.getHeaderHeight() + (item.getExpanded() ? item.getHeight() : 0);
		((GridData) optional.getLayoutData()).heightHint = height + optional.getSpacing() * (optional.getItemCount() + 1);
		details.layout(true, true); scroll.setMinSize(details.computeSize(Math.max(220, scroll.getClientArea().width), SWT.DEFAULT));
	}
	private void load() { String references = referenceProject; run("Read tags", () -> { ObjectNode value = TagManager.get().read(scope, project, references); if (scope == TagManager.Scope.projectObjects) value.set("suggestions", TagManager.get().suggestions(project).path("suggestions")); return value; }); }
	@FunctionalInterface private interface Work { ObjectNode run() throws Exception; }
	private void run(String name, Work work) { run(name, work, false); }
	private void run(String name, Work work, boolean keepDetails) {
		if (busy) return; busy = true; Display display = getShell().getDisplay();
		for (var control : editControls) if (!control.isDisposed()) control.setEnabled(false);
		for (var control : metadataEditors.values()) if (!control.isDisposed()) control.setEnabled(false);
		Job.create(name, monitor -> {
			try { ObjectNode result = work.run(); display.asyncExec(() -> { if (getShell() == null || getShell().isDisposed()) return; busy = false;
				if (snapshot != null && !result.has("suggestions") && snapshot.has("suggestions")) result.set("suggestions", snapshot.get("suggestions"));
				snapshot = result;
				if (result.has("id") && !result.path("id").asText().isEmpty()) selected = result.path("id").asText();
				if (selected.isEmpty() && referenceProject.isEmpty() && !snapshot.path("tags").isEmpty()) {
					selected = initialTargets.stream().flatMap(target -> java.util.stream.StreamSupport.stream(snapshot.path("assignments").path(target).spliterator(), false)).map(JsonNode::asText).findFirst().orElse(snapshot.path("tags").fieldNames().next());
				}
				render(keepDetails); refresh.accept(result); }); }
			catch (Exception e) { display.asyncExec(() -> { if (getShell() == null || getShell().isDisposed()) return; busy = false; if (snapshot != null) render(); else for (var control : editControls) if (!control.isDisposed()) control.setEnabled(true); setErrorMessage(e.getMessage()); }); }
		}).schedule();
	}
	private void command(String action, ObjectNode input) { if (snapshot == null || snapshot.path("readOnly").asBoolean()) return; String revision = snapshot.path("revision").asText(); run("Update tags", () -> TagManager.get().mutate(scope, project, revision, action, input), List.of("assign", "remove", "reorder").contains(action)); }
	private void render() { render(false); }
	private void render(boolean keepDetails) {
		setErrorMessage(null); renderTags(); suggestions.removeAll();
		for (var suggestion : snapshot.path("suggestions")) { var item = new TableItem(suggestions, SWT.NONE); item.setText(suggestion.path("definition").path("label").asText() + " — " + suggestion.path("project").asText()); item.setData(suggestion); }
		if (keepDetails && !selected.isEmpty() && snapshot.path("tags").has(selected)) renderMemberships();
		else if (!selected.isEmpty() && snapshot.path("tags").has(selected)) edit(selected); else { selected = ""; definition = TagDocument.JSON.createObjectNode().put("label", referenceProject); renderEditor(); }
		StringBuilder message = new StringBuilder(scope == TagManager.Scope.workspaceProjects ? "Memberships are saved in this workspace." : snapshot.path("dirty").asBoolean() ? "Modified — save the project to keep these tags." : "Sequence tags are saved with the project.");
		for (var diagnostic : snapshot.path("diagnostics")) message.append('\n').append(diagnostic.asText()); status.setText(message.toString());
		for (var control : editControls) if (!control.isDisposed()) control.setEnabled(!snapshot.path("readOnly").asBoolean());
		for (var control : metadataEditors.values()) if (!control.isDisposed()) control.setEnabled(!snapshot.path("readOnly").asBoolean());
		targets.setEnabled(!selected.isEmpty() && !snapshot.path("readOnly").asBoolean()); if (shared != null) shared.setEnabled(!selected.isEmpty() && !snapshot.path("readOnly").asBoolean());
		renderOrder();
		getShell().layout(true, true); layoutDetails();
	}
	private void renderTags() {
		if (snapshot == null || tags == null) return; tags.removeAll();
		var definitions = new ArrayList<Map.Entry<String, JsonNode>>(); snapshot.path("tags").fields().forEachRemaining(definitions::add);
		definitions.sort(java.util.Comparator.comparing((Map.Entry<String, JsonNode> entry) -> entry.getValue().path("label").asText()).thenComparing(Map.Entry::getKey));
		for (var entry : definitions) if (entry.getValue().path("label").asText().toLowerCase().contains(filter.getText().toLowerCase())) {
			var item = new TableItem(tags, SWT.NONE); item.setText(entry.getValue().path("label").asText()); item.setData(entry.getKey()); if (entry.getKey().equals(selected)) tags.setSelection(item);
		}
	}
	private void edit(String id) { selected = id; referenceProject = ""; definition = (ObjectNode) snapshot.path("tags").path(id).deepCopy(); renderEditor(); }
	private String targetLabel(String target) { return scope == TagManager.Scope.workspaceProjects ? target : target.replaceFirst("^.*\\.sq[:~]", ""); }
	private int memberCount() { return (int) java.util.Arrays.stream(targets.getItems()).filter(TableItem::getChecked).count(); }
	private String memberNames() { return String.join(", ", java.util.Arrays.stream(targets.getItems()).filter(TableItem::getChecked).map(item -> (String) item.getData()).toList()); }
	private void renderColor() {
		if (tagColor != null) { tagColor.dispose(); tagColor = null; }
		String color = definition.path("presentation").path("color").asText();
		if (color.matches("#[0-9A-Fa-f]{6}")) { tagColor = new org.eclipse.swt.graphics.Color(getShell().getDisplay(), Integer.parseInt(color.substring(1,3),16), Integer.parseInt(color.substring(3,5),16), Integer.parseInt(color.substring(5,7),16)); colorButton.setBackground(tagColor); }
		else colorButton.setBackground(null);
	}
	private void renderMemberships() {
		JsonNode names = snapshot == null ? TagDocument.JSON.createArrayNode() : snapshot.path(scope == TagManager.Scope.workspaceProjects ? "projects" : "targets");
		// Keep existing rows so checking a member does not reset scrolling or selection.
		targets.setItemCount(names.size());
		int index = 0;
		for (var target : names) {
			String name = target.asText(); var item = targets.getItem(index++); item.setText(targetLabel(name)); item.setData(name); item.setChecked(false);
			for (var id : snapshot.path("assignments").path(name)) if (id.asText().equals(selected)) item.setChecked(true);
			if (selected.isEmpty() && !referenceProject.isEmpty()) for (var member : snapshot.path("referenceTargets")) if (name.equals(member.asText())) item.setChecked(true);
		}
		targets.setEnabled(!selected.isEmpty() && snapshot != null && !snapshot.path("readOnly").asBoolean());
		((Group) targets.getParent()).setText((scope == TagManager.Scope.workspaceProjects ? "Projects" : "Sequences") + " (" + memberCount() + ")");
	}
	private void renderOrder() {
		if (snapshot == null || orderTarget == null) return;
		orderTargets.clear(); orderTarget.removeAll(); orderedTags.removeAll();
		for (var target : snapshot.path(scope == TagManager.Scope.workspaceProjects ? "projects" : "targets")) { orderTargets.add(target.asText()); orderTarget.add(targetLabel(target.asText())); }
		if (!orderTargets.contains(orderedTarget)) orderedTarget = orderTargets.isEmpty() ? "" : orderTargets.get(0);
		orderTarget.select(orderTargets.indexOf(orderedTarget));
		int index = 0;
		for (var id : snapshot.path("assignments").path(orderedTarget)) {
			var item = new TableItem(orderedTags, SWT.NONE); item.setData(id.asText()); item.setText(++index + ". " + snapshot.path("tags").path(id.asText()).path("label").asText());
			if (id.asText().equals(orderedTag)) orderedTags.setSelection(item);
		}
		updateOrderButtons();
	}
	private void updateOrderButtons() {
		int index = orderedTags.getSelectionIndex(); boolean editable = !busy && snapshot != null && !snapshot.path("readOnly").asBoolean();
		orderUp.setEnabled(editable && index > 0); orderDown.setEnabled(editable && index >= 0 && index < orderedTags.getItemCount() - 1);
	}
	private void moveOrder(int offset) {
		int index = orderedTags.getSelectionIndex(); if (busy || index < 0 || index + offset < 0 || index + offset >= orderedTags.getItemCount()) return;
		var ids = new ArrayList<String>(); for (var item : orderedTags.getItems()) ids.add((String) item.getData());
		java.util.Collections.swap(ids, index, index + offset);
		ObjectNode input = TagDocument.JSON.createObjectNode(); input.putArray("targets").add(orderedTarget); var array = input.putArray("tagIds"); ids.forEach(array::add); command("reorder", input);
	}

	private void renderEditor() {
		setMessage(referenceProject.isEmpty() ? "Select a tag, then check the " + (scope == TagManager.Scope.workspaceProjects ? "projects" : "sequences") + " that belong to it."
				: "Create a local tag for " + referenceProject + " and its direct and indirect project references. Only workspace projects are included; unavailable references are listed below. Memberships remain editable afterward and do not track future reference changes.");
		label.setText(definition.path("label").asText()); description.setText(definition.path("description").asText()); renderColor(); update.setText(selected.isEmpty() ? "Create tag" : "Update tag"); identity.setText(selected.isEmpty() ? "" : "ID: " + selected);
		renderMemberships(); renderOrder();
		if (shared != null) { shared.setSelection(definition.path("shared").asBoolean()); shared.setEnabled(!selected.isEmpty() && snapshot != null && !snapshot.path("readOnly").asBoolean()); }
		if (conflicts != null && snapshot != null) {
			for (var control : conflicts.getChildren()) { editControls.remove(control); control.dispose(); }
			StringBuilder message = new StringBuilder("Save each modified project to publish shared tags.");
			for (var item : snapshot.path("publication")) if (item.path("tagId").asText().equals(selected)) message.append('\n').append(item.path("project").asText()).append(": ").append(item.path("status").asText().equals("pendingSave") ? "Save required" : item.path("status").asText()); publication.setText(message.toString());
			for (var conflict : snapshot.path("conflicts")) if (conflict.path("tagId").asText().equals(selected)) {
				new Label(conflicts, SWT.WRAP).setText("Shared definitions differ. Choose which presentation to use here.");
				button(conflicts, "Keep current local presentation", () -> resolve("", false));
				conflict.path("sources").fields().forEachRemaining(source -> button(conflicts, "Use " + source.getValue().path("label").asText() + " from " + source.getKey(), () -> resolve(source.getKey(), false)));
				button(conflicts, "Prepare alignment of member sources", () -> resolve("", true));
			}
		}
		for (var control : metadata.getChildren()) control.dispose(); metadataEditors.clear();
		if (snapshot == null) return;
		snapshot.path("contributions").fields().forEachRemaining(namespace -> {
			new Label(metadata, SWT.NONE).setText(namespace.getValue().path("label").asText()); new Label(metadata, SWT.NONE);
			namespace.getValue().path("fields").fields().forEachRemaining(field -> {
				new Label(metadata, SWT.NONE).setText(field.getValue().path("label").asText()); JsonNode value = definition.path("metadata").path(namespace.getKey()).path(field.getKey());
				Control editor;
				if (field.getValue().path("type").asText().equals("array")) { editor = new TagReferenceList(metadata, field.getValue(), value); }
				else if (field.getValue().path("type").asText().equals("boolean")) { var checkbox = new Button(metadata, SWT.CHECK); checkbox.setSelection(value.asBoolean()); editor = checkbox; }
				else if (field.getValue().has("enum")) { var combo = new Combo(metadata, SWT.DROP_DOWN | SWT.READ_ONLY); for (var choice : field.getValue().path("enum")) combo.add(choice.asText()); combo.setText(value.asText()); editor = combo; }
				else { var text = new Text(metadata, SWT.BORDER); text.setText(value.asText()); editor = text; }
				editor.setEnabled(!snapshot.path("readOnly").asBoolean());
				editor.setData("original", editor instanceof TagReferenceList list ? list.value().toString() : value.asText());
				editor.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false)); metadataEditors.put(namespace.getKey() + "/" + field.getKey(), editor);
				if (field.getValue().has("description")) {
					Label hint = new Label(metadata, SWT.WRAP); hint.setText(field.getValue().path("description").asText());
					hint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
				}
			});
		});
		definition.path("metadata").fields().forEachRemaining(namespace -> {
			JsonNode descriptor = snapshot.path("contributions").path(namespace.getKey());
			ObjectNode unknown = TagDocument.JSON.createObjectNode();
			boolean unavailable = descriptor.isMissingNode() || !namespace.getValue().isObject();
			if (!unavailable) namespace.getValue().fields().forEachRemaining(field -> { if (!descriptor.path("fields").has(field.getKey())) unknown.set(field.getKey(), field.getValue()); });
			if (unavailable || !unknown.isEmpty()) {
				new Label(metadata, SWT.NONE).setText(namespace.getKey() + " — unavailable fields (preserved)");
				Text value = new Text(metadata, SWT.READ_ONLY | SWT.MULTI | SWT.BORDER); value.setText((unavailable ? namespace.getValue() : unknown).toPrettyString()); value.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			}
		});
		metadata.layout(true, true); layoutDetails();
	}
	private void resolve(String source, boolean align) {
		if (MessageDialog.openConfirm(getShell(), "Resolve shared tag presentation", align ? "Prepare alignment of all known member sources? Save each project explicitly afterward." : "Choose this local presentation? Portable sources keep their definitions.")) command("resolve", TagDocument.JSON.createObjectNode().put("id", selected).put("fromProject", source).put("alignSources", align).put("confirmed", true));
	}
	private void saveDefinition() {
		ObjectNode next = definition.deepCopy(); next.put("label", label.getText()); next.put("description", description.getText());
		try {
			for (var entry : metadataEditors.entrySet()) {
				String[] path = entry.getKey().split("/", 2); JsonNode schema = snapshot.path("contributions").path(path[0]).path("fields").path(path[1]);
				Control editor = entry.getValue();
				if (editor instanceof TagReferenceList list) {
					var value = list.value();
					if (!value.toString().equals(editor.getData("original"))) next.withObject("metadata").withObject(path[0]).set(path[1], value);
					continue;
				}
				String valueText = editor instanceof Button checkbox ? Boolean.toString(checkbox.getSelection()) : editor instanceof Combo combo ? combo.getText() : ((Text) editor).getText();
				String original = (String) editor.getData("original");
				if (valueText.equals(original) || (original.isEmpty() && valueText.equals("false"))) continue;
				ObjectNode values = next.withObject("metadata").withObject(path[0]);
				if (valueText.isEmpty() && !schema.path("required").asBoolean()) { values.remove(path[1]); continue; }
				if (editor instanceof Button checkbox) values.put(path[1], checkbox.getSelection());
				else { String value = editor instanceof Combo combo ? combo.getText() : ((Text) editor).getText();
					if (schema.path("type").asText().equals("integer")) values.put(path[1], Long.parseLong(value));
					else if (schema.path("type").asText().equals("number")) values.put(path[1], Double.parseDouble(value));
					else values.put(path[1], value); }
			}
			ObjectNode input = TagDocument.JSON.createObjectNode().put("id", selected).put("project", referenceProject); input.set("definition", next);
			command(selected.isEmpty() ? referenceProject.isEmpty() ? "create" : "createFromReferences" : "update", input);
		} catch (RuntimeException e) { setErrorMessage("Invalid metadata value: " + e.getMessage()); }
	}
}
