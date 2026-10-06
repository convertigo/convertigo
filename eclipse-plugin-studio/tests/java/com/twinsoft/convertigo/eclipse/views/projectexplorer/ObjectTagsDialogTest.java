/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.twinsoft.convertigo.engine.tags.TagDocument;
import com.twinsoft.convertigo.engine.tags.TagManager;

/** Real SWT controls and the production renderer of the tags of an object; no workspace mutation needed. */
public final class ObjectTagsDialogTest {
	private static final String PAY = "Fixture.sq:Pay", CHECKOUT = "Fixture.MobileApplication.Application.pg:Checkout";

	public static void main(String[] args) throws Exception {
		Display display = new Display();
		Shell shell = new Shell(display);
		try {
			var single = open(shell, List.of(PAY));
			Table tags = field(single, "tags");
			require(tags.getItemCount() == 3, "every tag of the project must be listed");
			require(tags.getItem(0).getText().equals("Audit  (0)") && !tags.getItem(0).getChecked(), "a tag of other objects only stays unchecked");
			require(tags.getItem(1).getText().equals("CRM  (1)") && tags.getItem(1).getChecked(), "a tag of the object must be checked");
			require(tags.getItem(2).getText().equals("Payment  (2)") && tags.getItem(2).getChecked() && !tags.getItem(2).getGrayed(), "a tag must show its member count");
			Table order = field(single, "order");
			require(order.getItemCount() == 2 && order.getItem(0).getText().equals("1. Payment") && order.getItem(1).getText().equals("2. CRM"), "the order of the tags of the object must be listed");
			Text filter = field(single, "filter"); Button create = field(single, "create");
			filter.setText("pay");
			require(tags.getItemCount() == 1 && create.getEnabled(), "a label without exact match must find tags and offer its creation");
			filter.setText("PAYMENT");
			require(tags.getItemCount() == 1 && !create.getEnabled(), "an existing label must not create a duplicate tag");
			// Return in the field creates or assigns the typed tag: it never closes the dialog
			require(single.getShell().getDefaultButton() == null, "Close must not be the default button");
			Event traverse = new Event(); traverse.detail = SWT.TRAVERSE_RETURN; traverse.doit = true;
			filter.notifyListeners(SWT.Traverse, traverse);
			require(!traverse.doit && !single.getShell().isDisposed(), "Return in the field must not close the dialog");
			single.close();

			var several = open(shell, List.of(PAY, CHECKOUT));
			tags = field(several, "tags");
			require(tags.getItem(1).getChecked() && tags.getItem(1).getGrayed(), "a tag of some of the selected objects must be partial");
			require(tags.getItem(2).getChecked() && !tags.getItem(2).getGrayed(), "a tag of all the selected objects must be checked");
			require(field(several, "order") == null, "the order of tags belongs to a single object");
			ObjectNode snapshot = field(several, "snapshot"); snapshot.put("readOnly", true); invoke(several, "render");
			require(!tags.getEnabled(), "read-only tags must not be editable");
			several.close();
			System.out.println("ObjectTagsDialogTest: tags of one or several objects, counts, partial checks, order, find or create with Return and read-only checks passed");
		} finally {
			shell.dispose();
			display.dispose();
		}
	}

	private static ObjectTagsDialog open(Shell shell, List<String> targets) throws Exception {
		var dialog = new ObjectTagsDialog(shell, TagManager.Scope.projectObjects, "Fixture", targets, ignored -> { });
		dialog.create();
		ObjectNode snapshot = TagDocument.JSON.createObjectNode().put("revision", "fixture").put("readOnly", false);
		var tags = snapshot.putObject("tags");
		tags.putObject("payment").put("label", "Payment").putObject("presentation").put("color", "#2563EB");
		tags.putObject("audit").put("label", "Audit");
		tags.putObject("crm").put("label", "CRM");
		var assignments = snapshot.putObject("assignments");
		assignments.putArray(PAY).add("payment").add("crm");
		assignments.putArray(CHECKOUT).add("payment");
		var details = snapshot.putObject("targetDetails");
		details.putObject(PAY).put("kind", "Sequence").put("label", "Pay");
		details.putObject(CHECKOUT).put("kind", "Page").put("label", "Checkout");
		var field = ObjectTagsDialog.class.getDeclaredField("snapshot"); field.setAccessible(true); field.set(dialog, snapshot);
		// The fixture replaces the read of the opening, which has no engine here.
		var busy = ObjectTagsDialog.class.getDeclaredField("busy"); busy.setAccessible(true); busy.set(dialog, false);
		invoke(dialog, "render");
		return dialog;
	}

	@SuppressWarnings("unchecked")
	private static <T> T field(Object owner, String name) throws Exception {
		Field field = ObjectTagsDialog.class.getDeclaredField(name);
		field.setAccessible(true);
		return (T) field.get(owner);
	}

	private static void invoke(Object owner, String name) throws Exception {
		Method method = ObjectTagsDialog.class.getDeclaredMethod(name);
		method.setAccessible(true);
		method.invoke(owner);
	}

	private static void require(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
