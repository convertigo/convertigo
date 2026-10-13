/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.eclipse.views.palette;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;

import com.twinsoft.convertigo.eclipse.views.projectexplorer.TagTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ObjectsFolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

/** Exercises the production selection resolver with real tag occurrences, without a native display. */
public final class PaletteSelectionTest {
	public static void main(String[] args) {
		TreeParent project = new TreeParent(null, "project") {};
		var sequences = new ObjectsFolderTreeObject(null, ObjectsFolderTreeObject.FOLDER_TYPE_SEQUENCES);
		sequences.parent = project;
		TreeParent sequence = new TreeParent(null, "sequence") {};
		sequence.parent = sequences;
		var steps = new ObjectsFolderTreeObject(null, ObjectsFolderTreeObject.FOLDER_TYPE_STEPS);
		steps.parent = sequence;
		TreeObject step = new TreeObject(null, "step") {};
		step.parent = steps;

		var group = new TagTreeObject(null, "tag", sequences, null, null);
		var occurrence = new TagTreeObject(null, "member", group, sequence, null);
		var stepsOccurrence = new TagTreeObject(null, "steps", occurrence, steps, null);
		var stepOccurrence = new TagTreeObject(null, "step", stepsOccurrence, step, null);
		var secondGroup = new TagTreeObject(null, "second-tag", sequences, null, null);
		var secondOccurrence = new TagTreeObject(null, "second-member", secondGroup, sequence, null);

		assertTarget(sequence, new StructuredSelection(sequence), "ordinary structured selection");
		assertTarget(sequence, selection(group, occurrence), "tagged sequence");
		assertTarget(sequence, selection(secondGroup, secondOccurrence), "second occurrence of the same sequence");
		assertTarget(steps, selection(group, occurrence, stepsOccurrence), "folder below a tagged sequence");
		assertTarget(step, selection(group, occurrence, stepsOccurrence, stepOccurrence), "descendant below a tagged sequence");
		assertTarget(sequences, selection(group), "tag group uses its real collection");
		var nestedGroup = new TagTreeObject(null, "nested-tag", stepsOccurrence, null, null);
		assertTarget(steps, selection(group, occurrence, stepsOccurrence, nestedGroup), "nested group resolves its collection occurrence");
		assertTarget(null, StructuredSelection.EMPTY, "empty selection");
		assertTarget(null, new StructuredSelection("unrelated"), "non-tree selection");
		assertTarget(null, new StructuredSelection(new TagTreeObject(null, "detached", null, null, null)), "detached group");
		if (stepOccurrence.parent != stepsOccurrence || step.parent != steps || sequence.parent != sequences) {
			throw new AssertionError("Resolving the palette context must not change the real or presentation tree");
		}
		System.out.println("PaletteSelectionTest: 11 checks passed");
	}

	private static TreeSelection selection(Object... path) {
		return new TreeSelection(new TreePath(path));
	}

	private static void assertTarget(TreeObject expected, ISelection selection, String scenario) {
		if (PaletteView.selectedTreeObject(selection) != expected) {
			throw new AssertionError("Wrong palette context: " + scenario);
		}
	}
}
