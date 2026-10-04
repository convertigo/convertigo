/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.util.ArrayList;

import org.eclipse.swt.custom.StyleRange;

/** Exercises the production style-range construction without a native SWT display. */
public class TagLabelStyleTest {
	public static void main(String[] args) {
		assertRange("Project [test] [main ↑7]", "[test]", 8, 6);
		assertRange("# Test environment (3)", "# Test environment", 0, 18);
		assertRange("🔒 Sequence [équipe 🧪]", "[équipe 🧪]", 12, 11);
		assertRange("[test] Project [test] [main]", "[test]", 15, 6);

		var ranges = new ArrayList<StyleRange>();
		DecoratingColumnLabelProvider.addTagStyle(ranges, "Project [first] [second]", "[first]", null);
		DecoratingColumnLabelProvider.addTagStyle(ranges, "Project [first] [second]", "[second]", null);
		if (ranges.size() != 2 || ranges.get(0).start + ranges.get(0).length > ranges.get(1).start) {
			throw new AssertionError("Separate badges must retain independent, non-overlapping styles");
		}
		DecoratingColumnLabelProvider.addTagStyle(ranges, "Project", "[absent]", null);
		if (ranges.size() != 2) throw new AssertionError("Missing badges must not produce invalid ranges");
		System.out.println("TagLabelStyleTest: 6 checks passed");
	}

	private static void assertRange(String text, String label, int start, int length) {
		var ranges = new ArrayList<StyleRange>();
		DecoratingColumnLabelProvider.addTagStyle(ranges, text, label, null);
		if (ranges.size() != 1 || ranges.get(0).start != start || ranges.get(0).length != length
				|| !text.substring(start, start + length).equals(label)) {
			throw new AssertionError("Expected complete tag label at " + start + ": " + label);
		}
	}
}
