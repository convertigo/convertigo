package com.twinsoft.convertigo.eclipse.views.palette;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** No SWT/workbench required: exercise the key actually used by PaletteView. */
public class PaletteFilterKeyTest {
	public static void main(String[] args) {
		var baseline = new PaletteFilterKey("", "type", "target", "project", Set.of(), List.of(), List.of(), true, true);
		var same = new PaletteFilterKey("", "type", "target", "project", Set.of(), List.of(), List.of(), true, true);
		if (!baseline.equals(same)) throw new AssertionError("Unchanged filters must reuse their result");
		var changed = List.of(
			new PaletteFilterKey("search", "type", "target", "project", Set.of(), List.of(), List.of(), true, true),
			new PaletteFilterKey("", "other", "target", "project", Set.of(), List.of(), List.of(), true, true),
			new PaletteFilterKey("", "type", "other", "project", Set.of(), List.of(), List.of(), true, true),
			new PaletteFilterKey("", "type", "target", "other", Set.of(), List.of(), List.of(), true, true),
			new PaletteFilterKey("", "type", "target", "project", Set.of("hidden"), List.of(), List.of(), true, true),
			new PaletteFilterKey("", "type", "target", "project", Set.of(), List.of("favorite"), List.of(), true, true),
			new PaletteFilterKey("", "type", "target", "project", Set.of(), List.of(), List.of("used"), true, true),
			new PaletteFilterKey("", "type", "target", "project", Set.of(), List.of(), List.of(), false, true),
			new PaletteFilterKey("", "type", "target", "project", Set.of(), List.of(), List.of(), true, false));
		for (var key : changed) if (baseline.equals(key)) throw new AssertionError("Changed filter was skipped: " + key);
		var categories = new HashSet<String>();
		var favorites = new ArrayList<String>();
		var used = new ArrayList<String>();
		var captured = new PaletteFilterKey("", "type", "target", "project", categories, favorites, used, true, true);
		categories.add("new"); favorites.add("new"); used.add("new");
		if (!baseline.equals(captured)) throw new AssertionError("A mutable collection changed the cached snapshot");
		var context = PaletteFilterKey.context("virtual.bean", "object", "provider.button");
		if (context.equals(PaletteFilterKey.context("virtual.bean", "object", "provider.config"))
				|| context.equals(PaletteFilterKey.context("virtual.bean", "scope", "provider.button"))
				|| context.equals(PaletteFilterKey.context("other.bean", "object", "provider.button"))) {
			throw new AssertionError("Different descriptor types must reset a stale reveal/search filter");
		}
		System.out.println("PaletteFilterKeyTest OK: every filter invalidates the immutable snapshot");
	}
}
