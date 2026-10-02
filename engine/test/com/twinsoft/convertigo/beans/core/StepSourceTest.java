/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.beans.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.steps.PdfFormStep;
import com.twinsoft.convertigo.beans.steps.SmartType;
import com.twinsoft.convertigo.beans.steps.XMLConcatStep;
import com.twinsoft.convertigo.beans.steps.XMLCopyStep;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class StepSourceTest {
	@BeforeClass
	public static void initializeLogging() {
		Engine.logBeans = Logger.getLogger("step-source-test");
		Engine.logEngine = Engine.logBeans;
	}

	@Test
	public void pdfAggregatesClassicAndSmartTypeSources() {
		var pdf = pdf();
		pdf.setSourceDefinition(definition("-1"));
		pdf.setFields(smartSource("-2"));
		pdf.setTargetFile(smartSource("-3"));
		assertEquals(Set.of("-1", "-2", "-3"), pdf.getSources().stream()
				.map(StepSource::getPriority).collect(Collectors.toSet()));
		assertTrue(pdf.workOnSource());
	}

	@Test
	public void pdfFieldsAlertAppearsAndDisappearsAfterRepair() throws Exception {
		var pdf = pdf();
		pdf.setFields(smartSource("1740057614974"));
		assertEquals(pdf.getName() + " (! broken source !)", pdf.toString());
		assertTrue(pdf.workOnSource());
		assertTrue(pdf.getLabel().contains("! broken source !"));

		var target = new XMLCopyStep();
		target.setParent(pdf.getParentSequence());
		pdf.getParentSequence().loadedSteps.put(target.priority, target);
		pdf.setFields(smartSource(Long.toString(target.priority)));
		assertEquals(pdf.getName(), pdf.toString());
		assertFalse(pdf.getSources().iterator().next().isBroken());
	}

	@Test
	public void everyPdfSmartTypePropertyUpdatesTheAlert() {
		var pdf = pdf();
		assertEquals(pdf.getName(), pdf.toString());
		pdf.setFilePath(smartSource("-1"));
		assertTrue(pdf.toString().contains("! broken source !"));
		pdf.setFilePath(new SmartType());
		assertEquals(pdf.getName(), pdf.toString());
		pdf.setTargetFile(smartSource("-2"));
		assertTrue(pdf.toString().contains("! broken source !"));
		pdf.setTargetFile(new SmartType());
		assertEquals(pdf.getName(), pdf.toString());
		assertFalse(pdf.workOnSource());
	}

	@Test
	public void emptyAndInactiveSourcesAreNotBroken() throws Exception {
		var pdf = pdf();
		pdf.setFields(smartSource("-1"));
		pdf.getFields().setMode(SmartType.Mode.PLAIN);
		assertTrue(pdf.getSources().isEmpty());
		assertFalse(pdf.workOnSource());
		assertEquals(pdf.getName(), pdf.toString());
		for (XMLVector<String> definition : java.util.Arrays.asList(null, new XMLVector<String>())) {
			var source = new StepSource(pdf, definition);
			assertTrue(source.isEmpty());
			assertFalse(source.isBroken());
			assertEquals("", source.getLabel());
		}
		pdf.setFields(smartSource("-1"));
		pdf.getFields().setSourceDefinition(new XMLVector<String>());
		assertFalse(pdf.workOnSource());
		assertTrue(pdf.getSources().isEmpty());
	}

	@Test
	public void invalidPriorityIsReportedWithoutThrowing() throws Exception {
		var pdf = pdf();
		pdf.setFields(smartSource("not-a-priority"));
		assertTrue(pdf.toString().contains("! broken source !"));
		var untyped = new XMLVector<Object>();
		untyped.add(123L);
		var source = new StepSource(pdf, GenericUtils.cast(untyped));
		assertTrue(source.isBroken());
		assertEquals("! broken source !", source.getLabel());
	}

	@Test
	public void multiSourcesSkipTextAndEmptyEntries() {
		var concat = new XMLConcatStep();
		concat.setParent(pdf().getParentSequence());
		var rows = new XMLVector<XMLVector<Object>>();
		for (Object value : new Object[] {"", "literal", new XMLVector<String>(), definition("-1")}) {
			var row = new XMLVector<Object>();
			row.add("description");
			row.add(value);
			row.add("");
			rows.add(row);
		}
		rows.add(new XMLVector<Object>());
		concat.setSourcesDefinition(rows);
		assertEquals(1, concat.getSources().size());
		assertTrue(concat.getSources().iterator().next().isBroken());
	}

	@Test
	public void classicSourceLabelsKeepTheirExistingBehavior() {
		var sequence = pdf().getParentSequence();
		var copy = new XMLCopyStep();
		copy.setParent(sequence);
		assertEquals("copyOf", copy.toString());
		assertFalse(copy.workOnSource());
		copy.setSourceDefinition(definition("-1"));
		assertEquals("copyOf @(! broken source !)", copy.toString());
		assertTrue(copy.workOnSource());
		var target = new XMLCopyStep();
		target.setParent(sequence);
		sequence.loadedSteps.put(target.priority, target);
		copy.setSourceDefinition(definition(Long.toString(target.priority)));
		assertEquals("copyOf @(copyOf)", copy.toString());
		assertFalse(copy.getSources().iterator().next().isBroken());
	}

	@Test
	public void movingAStepRetargetsPdfSmartTypeSources() {
		var pdf = pdf();
		pdf.setFields(smartSource("-1"));
		var target = new XMLCopyStep();
		target.setParent(pdf.getParentSequence());
		pdf.getParentSequence().loadedSteps.put(target.priority, target);
		pdf.stepMoved(new StepEvent(target, "-1"));
		assertEquals(Long.toString(target.priority), pdf.getFields().getSourceDefinition().get(0));
		assertEquals(pdf.getName(), pdf.toString());
	}

	private static PdfFormStep pdf() {
		var sequence = new GenericSequence();
		var pdf = new PdfFormStep();
		pdf.setParent(sequence);
		return pdf;
	}

	private static SmartType smartSource(String priority) {
		var smartType = new SmartType();
		smartType.setMode(SmartType.Mode.SOURCE);
		smartType.setSourceDefinition(definition(priority));
		return smartType;
	}

	private static XMLVector<String> definition(String priority) {
		var definition = new XMLVector<String>();
		definition.add(priority);
		definition.add(".");
		return definition;
	}
}
