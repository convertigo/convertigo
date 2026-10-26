/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.BeansDoc;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject.ExportOption;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.sequences.GenericSequenceBeanInfo;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBeans;
import com.twinsoft.convertigo.engine.dbo_explorer.DboCategory;
import com.twinsoft.convertigo.engine.dbo_explorer.DboExplorerManager;
import com.twinsoft.convertigo.engine.dbo_explorer.DboGroup;

/**
 * One engine marker (experimental="true" in database_objects.xml, the descriptor value "experimental") and one switch
 * (the system property convertigo.experimental.features, off by default) for the experimental features: while it is
 * off, the reference documentation, the palette and the property sheet of the Web Studio show neither the native
 * steps nor the JSON native properties of a sequence that is not native; a sequence already native keeps them.
 */
public class ExperimentalFeaturesTest {
	private static final Set<String> NATIVE_STEPS = Set.of(
			"com.twinsoft.convertigo.beans.steps.JsonFormatCellsStep",
			"com.twinsoft.convertigo.beans.steps.JsonDistinctStep",
			"com.twinsoft.convertigo.beans.steps.JsonGroupStep",
			"com.twinsoft.convertigo.beans.steps.JsonSelectColumnsStep",
			"com.twinsoft.convertigo.beans.steps.JsonStreamRowsStep",
			"com.twinsoft.convertigo.beans.steps.JsonReadStep");

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@BeforeClass
	public static void loggers() throws Exception {
		Engine.logBeans = Engine.logContext = Engine.logEngine = Logger.getLogger(ExperimentalFeaturesTest.class);
		EnginePropertiesManager.initProperties();
	}

	@Before
	@After
	public void off() {
		System.clearProperty(ExperimentalFeatures.SYSTEM_PROPERTY);
	}

	private static Set<String> experimentalBeans() throws Exception {
		Set<String> experimental = new TreeSet<>();
		for (DboGroup group : new DboExplorerManager().getGroups()) {
			for (DboCategory category : group.getCategories()) {
				for (DboBeans beans : category.getBeans()) {
					for (DboBean bean : beans.getBeans()) {
						if (bean.isExperimental()) {
							experimental.add(bean.getClassName());
						}
					}
				}
			}
		}
		return experimental;
	}

	private static DboBean bean(String className) throws Exception {
		for (DboGroup group : new DboExplorerManager().getGroups()) {
			for (DboCategory category : group.getCategories()) {
				for (DboBeans beans : category.getBeans()) {
					for (DboBean bean : beans.getBeans()) {
						if (bean.getClassName().equals(className)) {
							return bean;
						}
					}
				}
			}
		}
		throw new AssertionError(className);
	}

	private static PropertyDescriptor descriptor(String name) throws Exception {
		for (PropertyDescriptor descriptor : CachedIntrospector.getBeanInfo(GenericSequence.class).getPropertyDescriptors()) {
			if (descriptor.getName().equals(name)) {
				return descriptor;
			}
		}
		throw new AssertionError(name);
	}

	@Test
	public void theSwitchIsOffByDefault() {
		assertFalse(ExperimentalFeatures.isShown());
		ExperimentalFeatures.setShown(true);
		assertTrue(ExperimentalFeatures.isShown());
		ExperimentalFeatures.setShown(false);
		assertFalse(ExperimentalFeatures.isShown());
	}

	@Test
	public void theSixNativeStepsAreTheExperimentalBeans() throws Exception {
		assertEquals(new TreeSet<>(NATIVE_STEPS), experimentalBeans());
		DboBean step = bean("com.twinsoft.convertigo.beans.steps.JsonGroupStep");
		DboBean classic = bean("com.twinsoft.convertigo.beans.steps.JsonObjectStep");
		assertTrue(ExperimentalFeatures.isHiddenBean(step));
		assertFalse(ExperimentalFeatures.isHiddenBean(classic));
		ExperimentalFeatures.setShown(true);
		assertFalse(ExperimentalFeatures.isHiddenBean(step));
	}

	@Test
	public void theTwoJsonNativePropertiesOfASequenceAreExperimental() throws Exception {
		Set<String> experimental = new TreeSet<>();
		for (PropertyDescriptor descriptor : CachedIntrospector.getBeanInfo(GenericSequence.class).getPropertyDescriptors()) {
			if (ExperimentalFeatures.isExperimental(descriptor)) {
				experimental.add(descriptor.getName());
			}
		}
		assertEquals(new TreeSet<>(Set.of("jsonNativeOutput", "nativeExecution")), experimental);
	}

	@Test
	public void anExperimentalPropertyIsHiddenOnlyWhenOff() throws Exception {
		GenericSequence sequence = new GenericSequence();
		PropertyDescriptor nativeExecution = descriptor("nativeExecution");
		PropertyDescriptor jsonNativeOutput = descriptor("jsonNativeOutput");
		assertTrue(ExperimentalFeatures.isHiddenProperty(nativeExecution));
		assertTrue(ExperimentalFeatures.isHiddenProperty(sequence, nativeExecution));
		assertTrue(ExperimentalFeatures.isHiddenProperty(sequence, "jsonNativeOutput"));
		assertFalse(ExperimentalFeatures.isHiddenProperty(sequence, descriptor("autoStart")));

		// a sequence already native keeps the properties that make it native
		sequence.setNativeExecution(true);
		assertFalse(ExperimentalFeatures.isHiddenProperty(sequence, nativeExecution));
		assertTrue(ExperimentalFeatures.isHiddenProperty(sequence, jsonNativeOutput));
		sequence.setJsonNativeOutput(true);
		assertFalse(ExperimentalFeatures.isHiddenProperty(sequence, "jsonNativeOutput"));

		ExperimentalFeatures.setShown(true);
		GenericSequence classic = new GenericSequence();
		assertFalse(ExperimentalFeatures.isHiddenProperty(nativeExecution));
		assertFalse(ExperimentalFeatures.isHiddenProperty(classic, nativeExecution));
		assertFalse(ExperimentalFeatures.isHiddenProperty(classic, "jsonNativeOutput"));
	}

	private static String paletteDescription() throws Exception {
		var palette = new com.twinsoft.convertigo.engine.admin.services.studio.palette.Get();
		Method method = palette.getClass().getDeclaredMethod("getPropertiesDescription", BeanInfo.class);
		method.setAccessible(true);
		return (String) method.invoke(palette, new GenericSequenceBeanInfo());
	}

	@Test
	public void theWebStudioPaletteDescribesTheJsonNativePropertiesOnlyWhenShown() throws Exception {
		String off = paletteDescription();
		assertTrue(off, off.contains(descriptor("autoStart").getDisplayName()));
		assertFalse(off, off.contains("JSON native output"));
		assertFalse(off, off.contains("Native execution"));
		ExperimentalFeatures.setShown(true);
		String on = paletteDescription();
		assertTrue(on, on.contains("JSON native output"));
		assertTrue(on, on.contains("Native execution"));
	}

	private static JSONObject propertySheet(DatabaseObject dbo) throws Exception {
		Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		Element element = dbo.toXml(document, ExportOption.bIncludeBlackListedElements, ExportOption.bIncludeCompiledValue,
				ExportOption.bIncludeDisplayName, ExportOption.bIncludeEditorClass, ExportOption.bIncludeShortDescription,
				ExportOption.bHidePassword);
		var properties = new com.twinsoft.convertigo.engine.admin.services.studio.properties.Get();
		Method add = properties.getClass().getDeclaredMethod("addDboProperties", DatabaseObject.class, JSONObject.class, Element.class);
		add.setAccessible(true);
		JSONObject props = new JSONObject();
		for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Element e && e.getNodeName().equals("property")) {
				add.invoke(properties, dbo, props, e);
			}
		}
		return props;
	}

	@Test
	public void theWebStudioPropertySheetHidesTheJsonNativePropertiesThatAreOff() throws Exception {
		GenericSequence sequence = new GenericSequence();
		sequence.setName("Classic");
		JSONObject off = propertySheet(sequence);
		assertTrue(off.toString(), off.has(descriptor("autoStart").getDisplayName()));
		assertFalse(off.toString(), off.has("JSON native output"));
		assertFalse(off.toString(), off.has("Native execution"));

		sequence.setNativeExecution(true);
		JSONObject nativeOff = propertySheet(sequence);
		assertTrue(nativeOff.toString(), nativeOff.has("Native execution"));
		assertFalse(nativeOff.toString(), nativeOff.has("JSON native output"));

		ExperimentalFeatures.setShown(true);
		JSONObject on = propertySheet(new GenericSequence());
		assertTrue(on.toString(), on.has("JSON native output"));
		assertTrue(on.toString(), on.has("Native execution"));
	}

	private File beansDoc(String name) throws Exception {
		File dir = folder.newFolder(name);
		File output = new File(dir, "reference-manual/convertigo-objects");
		Constructor<BeansDoc> constructor = BeansDoc.class.getDeclaredConstructor(File.class, File.class, int.class);
		constructor.setAccessible(true);
		BeansDoc doc = constructor.newInstance(output, new File(dir, "images/beans"), -1);
		Method run = BeansDoc.class.getDeclaredMethod("run");
		run.setAccessible(true);
		java.io.PrintStream out = System.out;
		// BeansDoc prints a line per object
		System.setOut(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
		try {
			run.invoke(doc);
		} finally {
			System.setOut(out);
		}
		return output;
	}

	private static String all(File dir) throws Exception {
		StringBuilder sb = new StringBuilder();
		try (Stream<java.nio.file.Path> files = Files.walk(dir.toPath())) {
			for (java.nio.file.Path file : (Iterable<java.nio.file.Path>) files.filter(Files::isRegularFile)::iterator) {
				sb.append(file).append('\n').append(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).append('\n');
			}
		}
		return sb.toString();
	}

	@Test
	public void theReferenceDocumentationHasTheExperimentalFeaturesOnlyWhenShown() throws Exception {
		File off = beansDoc("off");
		String offText = all(off);
		String sequence = new String(Files.readAllBytes(new File(off, "sequencer/generic-sequence.md").toPath()), StandardCharsets.UTF_8);
		assertFalse(sequence, sequence.contains("JSON native output"));
		assertFalse(sequence, sequence.contains("Native execution"));
		for (String className : NATIVE_STEPS) {
			assertFalse(className, offText.contains("ObjClass: " + className + "\n"));
		}
		assertTrue(offText.contains("ObjClass: com.twinsoft.convertigo.beans.steps.JsonObjectStep\n"));

		ExperimentalFeatures.setShown(true);
		File on = beansDoc("on");
		String onText = all(on);
		String onSequence = new String(Files.readAllBytes(new File(on, "sequencer/generic-sequence.md").toPath()), StandardCharsets.UTF_8);
		assertTrue(onSequence.contains("JSON native output"));
		assertTrue(onSequence.contains("Native execution"));
		for (String className : NATIVE_STEPS) {
			assertTrue(className, onText.contains("ObjClass: " + className + "\n"));
		}
	}
}
