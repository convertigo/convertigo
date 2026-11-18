package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.BeansDefaultValues;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.variables.*;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.ProductVersion;
import com.twinsoft.convertigo.engine.enums.Visibility;

public class VariableYamlTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private Engine previousApp;
	private Logger previousBeans, previousEngine;
	private Path yaml;

	@Before public void setup() throws Exception {
		previousApp = Engine.theApp;
		previousBeans = Engine.logBeans;
		previousEngine = Engine.logEngine;
		Engine.logBeans = Engine.logEngine = Logger.getLogger("variable-yaml-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager();
		yaml = folder.newFile("c8oProject.yaml").toPath();
	}

	@After public void restore() {
		Engine.theApp = previousApp;
		Engine.logBeans = previousBeans;
		Engine.logEngine = previousEngine;
	}

	@Test public void absentValuesStayAbsentRegardlessOfLogVisibility() throws Exception {
		for (Variable variable : variables()) {
			for (String name : List.of("__header_X", "password", "ordinary")) {
				for (int mask : List.of(0, Visibility.Logs.getMask())) {
					variable.setName(name);
					variable.setVisibility(mask);
					variable.setValueOrNull(null);
					write(variable);
					assertFalse(variable.getClass().getSimpleName(), Files.readString(yaml).contains("\n  value:"));
					assertNull(read().getValueOrNull());
				}
			}
		}
	}

	@Test public void attributeOnlyValuesFromOlderExportsAreReadAsNull() throws Exception {
		for (Variable variable : variables()) {
			for (String attributes : List.of("    - ↑traceable: false\n", "    - ↑traceable: false\n    - ↑ciphered: true\n")) {
				Files.writeString(yaml, header(variable) + "  value: \n" + attributes);
				var loaded = read();
				assertNull(variable.getClass().getSimpleName(), loaded.getValueOrNull());
				write(loaded);
				assertFalse(Files.readString(yaml).contains("\n  value:"));
			}
		}
	}

	@Test public void explicitEmptyScalarsRemainEmptyWithAndWithoutAttributes() throws Exception {
		for (String value : List.of("  value: \n", "  value: ''\n", "  value: ''\n    - ↑traceable: false\n",
				"  value: \n    - ↑traceable: false\n    - →→: ''\n")) {
			Files.writeString(yaml, header(new RequestableHttpVariable()) + value);
			var variable = read();
			assertEquals("", variable.getValueOrNull());
			write(variable);
			assertEquals("", read().getValueOrNull());
		}
	}

	@Test public void explicitEmptyValuesHaveAnUnambiguousSerializedRepresentation() throws Exception {
		for (Variable variable : variables()) {
			variable.setName("__header_X");
			variable.setValueOrNull(variable.isMultiValued() ? new XMLVector<>() : "");
			write(variable);
			assertEquals(variable.getValueOrNull(), read().getValueOrNull());
			assertTrue(Files.readString(yaml).contains("\n  value:"));
			if (!variable.isMultiValued()) {
				assertTrue(Files.readString(yaml).contains("- →→: ''"));
			}
		}
	}

	@Test public void populatedAndWhitespaceValuesSurviveWithTheirMaskingMetadata() throws Exception {
		for (String value : List.of("synthetic-header-value", " ")) {
			var variable = new RequestableHttpVariable();
			variable.setName("__header_X");
			variable.setValueOrNull(value);
			write(variable);
			assertTrue(Files.readString(yaml).contains("↑traceable: false"));
			var loaded = read();
			assertEquals(value, loaded.getValueOrNull());
			assertFalse(loaded.isTraceableProperty("value"));
		}
	}

	@Test public void populatedVectorsKeepAllTheirValues() throws Exception {
		for (Variable variable : variables()) {
			if (variable.isMultiValued()) {
				variable.setName("__header_X");
				variable.setValueOrNull(List.of("synthetic-header-value", "", " "));
				write(variable);
				assertEquals(variable.getValueOrNull(), read().getValueOrNull());
			}
		}
	}

	private List<Variable> variables() {
		return List.of(new RequestableHttpVariable(), new RequestableHttpMultiValuedVariable(),
				new RequestableVariable(), new RequestableMultiValuedVariable(), new StepVariable(),
				new StepMultiValuedVariable(), new TestCaseVariable(), new TestCaseMultiValuedVariable());
	}

	private String header(Variable variable) {
		return "↑convertigo: " + ProductVersion.productVersion + "\n↓__header_X [variables."
				+ variable.getClass().getSimpleName() + "-1]: \n";
	}

	private void write(Variable variable) throws Exception {
		var document = XMLUtils.createDom();
		var root = document.createElement("convertigo");
		document.appendChild(root);
		root.setAttribute("beans", ProductVersion.productVersion);
		root.appendChild(variable.toXml(document));
		YamlConverter.writeYaml(BeansDefaultValues.shrinkProject(document), yaml.toFile(),
				Files.createDirectories(yaml.getParent().resolve("_c8oProject")).toFile());
	}

	private Variable read() throws Exception {
		var document = BeansDefaultValues.unshrinkProject(YamlConverter.readYaml(yaml.toFile()));
		return (Variable) DatabaseObject.read((Element) document.getDocumentElement().getFirstChild());
	}
}
