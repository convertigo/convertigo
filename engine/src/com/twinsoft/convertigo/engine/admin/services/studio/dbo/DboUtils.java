package com.twinsoft.convertigo.engine.admin.services.studio.dbo;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;

public class DboUtils {

	static protected DatabaseObject createDbo(JSONObject jsonData) throws Exception {
		if (jsonData.has("type")) {
			var type = jsonData.getString("type");
			if (type.equals("paletteData")) {
				return createDboFromPalette(jsonData);
			}
			if (type.equals("treeData")) {
				return createDboFromTree(jsonData);
			}
		}
		return null;
	}

	static protected DatabaseObject createDboFromPalette(JSONObject jsonData) throws Exception {
		DatabaseObject dbo = null;

		JSONObject jsonItem = jsonData.getJSONObject("data");

		var dboClassName = jsonItem.getString("classname");
		var dboType = jsonItem.getString("type");
		var dboId = jsonItem.getString("id");

		// case Bean
		if (dboType.equals("Dbo")) {
			dbo = (DatabaseObject) Class.forName(dboClassName).getConstructor().newInstance();
		}
		// case ionBean
		else if (dboType.equals("Ion")) {
			var kind = dboId.split(" ")[0];
			if (kind.equals("ngx")) {
				var ionBeanName = dboId.split(" ")[1];
				com.twinsoft.convertigo.beans.ngx.components.dynamic.Component component = null;
				component = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager
						.getComponentByName(ionBeanName);
				if (component != null) {
					dbo = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager
							.createBeanFromHint(component);
				}
			}
		}

		return dbo;
	}

	static protected DatabaseObject createDboFromTree(JSONObject jsonData) throws Exception {
		return null;
	}
	
}
