/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.beans.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * The min and max of a date column (lib_BaseRow formssource_GetTableData, grouped without the c8oforms Baserow plugin):
 * the dates were compared as numbers, parseFloat("2024-03-05") giving the year only, and the min was the first date
 * read of the earliest year.
 */
public class JsonGroupStepDateTest {

	private static Map<String, Object> map(Object... entries) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		for (int i = 0; i < entries.length; i += 2) {
			map.put((String) entries[i], entries[i + 1]);
		}
		return map;
	}

	/** the min and max of the column When of rows grouped by G, the column of the given field type */
	private static Object[] minMax(Map<String, Object> field, Object... values) {
		List<Object> rows = new ArrayList<Object>();
		for (Object value : values) {
			rows.add(map("G", map("value", "a"), "When", value == null ? null : map("value", value)));
		}
		List<Object> aggregations = Arrays.asList(
				map("type", "min", "column", "When", "key", "min_When", "label", ""),
				map("type", "max", "column", "When", "key", "max_When", "label", ""));
		Map<String, Object> schemaField = new LinkedHashMap<String, Object>(field);
		schemaField.put("name", "When");
		String[] error = new String[1];
		List<Object> groups = new JsonGroupStep().group(rows, Arrays.asList("G"), aggregations,
				Arrays.asList((Object) map("name", "G", "type", "text"), schemaField), error);
		assertNull(error[0]);
		assertEquals(1, groups.size());
		Map<?, ?> group = (Map<?, ?>) groups.get(0);
		return new Object[] { ((Map<?, ?>) group.get("min_When")).get("value"), ((Map<?, ?>) group.get("max_When")).get("value") };
	}

	@Test
	public void datesOfTheSameYearAreComparedInTimeOrder() {
		Object[] dates = minMax(map("type", "date"), "2024-06-11", "2024-01-03", null, "2023-12-31", "2024-12-01");
		assertEquals("2023-12-31", dates[0]);
		assertEquals("2024-12-01", dates[1]);
		dates = minMax(map("type", "date"), "2024-06-11", "2024-01-03");
		assertEquals("2024-01-03", dates[0]);
		assertEquals("2024-06-11", dates[1]);
	}

	@Test
	public void timesWithFractionsAndOffsetsAreComparedInTimeOrder() {
		Object[] times = { "2024-05-01T08:07:00.5Z", "2024-05-01T08:07:00Z", "2024-05-01T08:07:00.123456Z", "2024-05-01T09:00:00+02:00" };
		for (Map<String, Object> field : Arrays.asList(map("type", "last_modified"), map("type", "created_on"),
				map("type", "formula", "formula_type", "date"), map("type", "date"))) {
			Object[] minMax = minMax(field, times);
			assertEquals(field.toString(), "2024-05-01T09:00:00+02:00", minMax[0]);
			assertEquals(field.toString(), "2024-05-01T08:07:00.5Z", minMax[1]);
		}
	}

	@Test
	public void numbersAndTextsKeepTheirOrder() {
		Object[] numbers = minMax(map("type", "number"), "10", "9.5", "100");
		assertEquals("9.5", numbers[0]);
		assertEquals("100", numbers[1]);
		Object[] texts = minMax(map("type", "text"), "b", "a", "c");
		assertEquals("a", texts[0]);
		assertEquals("c", texts[1]);
	}

	@Test
	public void sortKeyOfADateOfBaserow() {
		assertEquals("2024-03-05", JsonGroupStep.dateSortKey("2024-03-05"));
		assertEquals("2024-03-05T08:07:00.000000000Z", JsonGroupStep.dateSortKey("2024-03-05T08:07:00Z"));
		assertEquals("2024-03-05T08:07:00.000000000Z", JsonGroupStep.dateSortKey("2024-03-05T08:07Z"));
		assertEquals("2024-03-05T08:07:00.123456000Z", JsonGroupStep.dateSortKey("2024-03-05T08:07:00.123456Z"));
		assertEquals("2024-03-05T08:07:00.100000000Z", JsonGroupStep.dateSortKey("2024-03-05 08:07:00.1+00:00"));
		assertEquals("2024-03-05T07:07:00.000000000Z", JsonGroupStep.dateSortKey("2024-03-05T08:07:00+01:00"));
		assertEquals("2024-03-05T07:07:00.000000000Z", JsonGroupStep.dateSortKey("2024-03-05T08:07:00+0100"));
		assertNull(JsonGroupStep.dateSortKey("05/03/2024"));
		assertNull(JsonGroupStep.dateSortKey("2024"));
	}
}
