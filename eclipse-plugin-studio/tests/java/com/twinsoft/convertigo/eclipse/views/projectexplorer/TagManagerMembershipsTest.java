package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.twinsoft.convertigo.engine.tags.TagDocument;
import com.twinsoft.convertigo.engine.tags.TagManager;

/** Real SWT controls and the production renderer; no workspace mutation or Engine needed. */
public final class TagManagerMembershipsTest {
    public static void main(String[] args) throws Exception {
        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setText("Convertigo tag list regression — isolated fixture");
        shell.setLayout(new FillLayout());
        shell.setSize(420, 260);
        Group group = new Group(shell, SWT.NONE);
        group.setLayout(new FillLayout());
        Table table = new Table(group, SWT.CHECK | SWT.SINGLE | SWT.BORDER);
        try {
            shell.open();
            while (display.readAndDispatch()) { }
            for (var scope : List.of(TagManager.Scope.workspaceProjects, TagManager.Scope.projectObjects)) {
                exercise(shell, table, scope);
            }
            System.out.println("TagManagerMembershipsTest: project and sequence rows, scroll, selection and authoritative checks passed");
        } finally {
            shell.dispose();
            display.dispose();
        }
    }

    private static void exercise(Shell shell, Table table, TagManager.Scope scope) throws Exception {
        var dialog = new TagManagerDialog(shell, scope, "Fixture", List.of(), ignored -> { });
        ObjectNode snapshot = TagDocument.JSON.createObjectNode();
        String key = scope == TagManager.Scope.workspaceProjects ? "projects" : "targets";
        var names = snapshot.putArray(key);
        var assignments = snapshot.putObject("assignments");
        for (int i = 0; i < 60; i++) names.add(scope == TagManager.Scope.workspaceProjects
                ? "Project" + i : "Fixture.sq~Sequence" + i);
        field(dialog, "targets", table);
        field(dialog, "snapshot", snapshot);
        field(dialog, "selected", "tag");
        Method render = TagManagerDialog.class.getDeclaredMethod("renderMemberships");
        render.setAccessible(true);
        render.invoke(dialog);
        table.setSelection(30);
        table.setTopIndex(24);
        table.setFocus();
        while (shell.getDisplay().readAndDispatch()) { }
        int top = table.getTopIndex();
        require(top > 0, "fixture must actually be scrolled");
        TableItem row = table.getItem(30);
        String name = names.get(30).asText();
        for (boolean checked : List.of(true, false, true)) {
            if (checked) assignments.putArray(name).add("tag");
            else assignments.remove(name);
            table.setEnabled(false); // The async command disables editing before applying its snapshot.
            render.invoke(dialog);
            require(!row.isDisposed() && table.getItem(30) == row, "membership update rebuilt the rows");
            require(table.getTopIndex() == top, "membership update reset the scroll position");
            require(table.getSelectionIndex() == 30, "membership update lost the row selection");
            require(row.getChecked() == checked, "checkbox does not match the authoritative snapshot");
            require(((Group) table.getParent()).getText().endsWith(checked ? "(1)" : "(0)"), "member count is stale");
        }
        field(dialog, "selected", "anotherTag");
        render.invoke(dialog);
        require(!row.getChecked() && !row.isDisposed(), "switching tags must update checks without rebuilding rows");
        snapshot.put("readOnly", true);
        render.invoke(dialog);
        require(!table.getEnabled(), "read-only state must still disable membership editing");
        snapshot.put("readOnly", false);
        names.removeAll();
        render.invoke(dialog);
        require(table.getItemCount() == 0, "removed targets must disappear");
        names.add("NewTarget");
        render.invoke(dialog);
        require(table.getItemCount() == 1 && "NewTarget".equals(table.getItem(0).getData()), "new targets must appear");
        if (scope == TagManager.Scope.workspaceProjects) {
            field(dialog, "selected", "");
            dialog.createFromReferences("NewTarget");
            snapshot.putArray("referenceTargets").add("NewTarget");
            render.invoke(dialog);
            require(table.getItem(0).getChecked() && !table.getEnabled(), "reference preview must be checked without assigning before Create tag");
        }
    }

    private static void field(Object owner, String name, Object value) throws Exception {
        Field field = TagManagerDialog.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(owner, value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
