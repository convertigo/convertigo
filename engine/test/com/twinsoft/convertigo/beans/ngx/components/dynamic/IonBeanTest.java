/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details: <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.ngx.components.dynamic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.engine.Engine;

/** Standalone regression suite: ion bean instances share the definition of their template model. */
public final class IonBeanTest {
	private static final String ION_OBJECTS = """
		{
			"Props": {
				"Disabled": {"attr": "disabled", "label": "Disabled", "category": "@Properties", "type": "boolean", "value": "false", "values": ["false", "true"]}
			},
			"Beans": {
				"Button": {
					"classname": "com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement",
					"tag": "ion-button", "label": "Button", "group": "Components", "description": "A button",
					"config": {"module_ng_imports": ["ButtonModule"]},
					"properties": {
						"Color": {"attr": "color", "label": "Color", "value": "primary", "values": ["primary", "secondary"]},
						"Disabled": "false"
					}
				}
			},
			"C8oBeans": {}
		}""";
	private static final String SAVED = "{\"name\":\"Button\",\"properties\":{"
			+ "\"Color\":{\"name\":\"Color\",\"mode\":\"script\",\"value\":\"this.color\"},"
			+ "\"Disabled\":{\"name\":\"Disabled\",\"mode\":\"plain\",\"value\":\"false\"}}}";
	private static int checks;

	public static void main(String[] args) throws Exception {
		Engine.logEngine = org.apache.log4j.Logger.getLogger("ion-bean-test");
		Engine.logBeans = Engine.logEngine;
		Path tpl = Files.createTempDirectory("convertigo-ion-bean-test-");
		try {
			Path ion = Files.createDirectories(tpl.resolve("ionicTpl/ion"));
			Files.writeString(ion.resolve("ion_objects.json"), ION_OBJECTS);
			ComponentManager.addIonicTemplateProject("IonBeanTestTpl", tpl.toFile());
			ComponentManager cm = ComponentManager.of("IonBeanTestTpl");

			instancesKeepTheirOwnValues(cm);
			modelIsNeverModified(cm);
			beanDataRoundTrip(cm);
			System.out.println("IonBean: " + checks + " checks passed");
		} finally {
			FileUtils.deleteQuietly(tpl.toFile());
		}
	}

	private static void instancesKeepTheirOwnValues(ComponentManager cm) throws Exception {
		IonBean a = cm.loadBean(SAVED);
		IonBean b = cm.loadBean(SAVED);
		IonBean defaults = cm.loadBean("{\"name\":\"Button\"}");

		check("script".equals(a.getProperty("Color").getMode()), "saved mode is loaded");
		check("this.color".equals(a.getProperty("Color").getValue()), "saved value is loaded");
		check("Color".equals(a.getProperty("Color").getLabel()), "definition comes from the model");
		check("primary".equals(defaults.getProperty("Color").getValue()), "missing value falls back to the model");
		check("plain".equals(defaults.getProperty("Color").getMode()), "missing mode falls back to the model");

		a.setPropertyValue("Color", new MobileSmartSourceType("secondary"));
		check("secondary".equals(a.getProperty("Color").getValue()), "value is updated");
		check("plain".equals(a.getProperty("Color").getMode()), "mode is updated");
		check("this.color".equals(b.getProperty("Color").getValue()), "other instances keep their value");
		check("primary".equals(defaults.getProperty("Color").getValue()), "default instances keep the model value");
		check(a.getConfig().getModuleNgImports().contains("ButtonModule"), "config comes from the model");
	}

	private static void modelIsNeverModified(ComponentManager cm) throws Exception {
		IonBean a = cm.loadBean(SAVED);
		a.getJSONObject().getJSONObject("properties").getJSONObject("Disabled").put("label", "changed");
		a.getJSONObject().put("label", "changed");
		IonBean other = cm.loadBean(SAVED);
		check("Disabled".equals(other.getProperty("Disabled").getLabel()), "property definition is not shared mutable state");
		check("Button".equals(other.getLabel()), "bean definition is not shared mutable state");
		check("Disabled".equals(a.getProperty("Disabled").getLabel()), "instance definition is not changed through a copy");
	}

	private static void beanDataRoundTrip(ComponentManager cm) throws Exception {
		IonBean a = cm.loadBean(SAVED);
		a.setPropertyValue("Disabled", new MobileSmartSourceType("true"));
		String beanData = a.toBeanData();
		JSONObject json = new JSONObject(beanData);
		JSONObject properties = json.getJSONObject("properties");
		check(keys(json).equals(Set.of("name", "properties")), "bean data only keeps the name and the properties");
		check(keys(properties.getJSONObject("Color")).equals(Set.of("name", "mode", "value")), "bean data only keeps property values");
		check("this.color".equals(properties.getJSONObject("Color").getString("value")), "bean data keeps loaded values");
		check("true".equals(properties.getJSONObject("Disabled").getString("value")), "bean data keeps updated values");
		check(beanData.equals(cm.loadBean(beanData).toBeanData()), "bean data is stable across reloads");
	}

	private static Set<String> keys(JSONObject json) {
		Set<String> keys = new TreeSet<>();
		for (Iterator<?> it = json.keys(); it.hasNext();) {
			keys.add((String) it.next());
		}
		return keys;
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
		checks++;
	}
}
