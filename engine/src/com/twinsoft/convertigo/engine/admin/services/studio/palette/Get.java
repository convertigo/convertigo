/*
 * Copyright (c) 2001-2023 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.admin.services.studio.palette;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBeans;
import com.twinsoft.convertigo.engine.dbo_explorer.DboCategory;
import com.twinsoft.convertigo.engine.dbo_explorer.DboGroup;

@ServiceDefinition(name = "Get", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Get extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		response.put("id", id);
		response.put("categories", getPalette(id));
	}

	private JSONArray getPalette(String id) throws Exception {
		if (id.contains("/")) {
			return new JSONArray();
		}

		var parentDbo = Utils.getDbo(id);
		var folderType = Utils.getFolderType(id);

		JSONArray categories = new JSONArray();

		for (DboGroup g : Engine.theApp.getDboExplorerManager().getGroups()) {
			String groupName = g.getName();
			for (DboCategory c : g.getCategories()) {
				String categoryName = c.getName().isEmpty() ? groupName : c.getName();

				for (DboBeans bs : c.getBeans()) {
					String category = bs.getName().isEmpty() ? categoryName : bs.getName();

					JSONObject jsonCategory = new JSONObject();
					jsonCategory.put("type", "Category");
					jsonCategory.put("name", category);
					jsonCategory.put("items", new JSONArray());
					categories.put(jsonCategory);

					for (DboBean b : bs.getBeans()) {
						String cn = b.getClassName();
						if ((cn.startsWith("com.twinsoft.convertigo.beans.ngx.components.")
								|| cn.startsWith("com.twinsoft.convertigo.beans.mobile.components."))
								&& !cn.endsWith("PageComponent")) {
							continue;
						}

						var isAllowedIn = false;
						try {
							boolean force = false;
							if (parentDbo != null /* isType[0] */) {
								String cls = b.getClassName();
								if (parentDbo instanceof Sequence) {
									force = cls.startsWith("com.twinsoft.convertigo.beans.steps.")
											|| cls.startsWith("com.twinsoft.convertigo.beans.variables.Step");
								} else if (parentDbo instanceof com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent) {
									force = cls.startsWith("com.twinsoft.convertigo.beans.ngx.");
								} else if (parentDbo instanceof com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent) {
									force = cls.startsWith("com.twinsoft.convertigo.beans.mobile.");
								}
							}
							isAllowedIn = force || DatabaseObjectsManager.checkParent(parentDbo.getClass(), b);
							if (folderType != null && isAllowedIn) {
								isAllowedIn = DatabaseObject
										.getFolderType(Class.forName(b.getClassName())) == folderType;
							}
						} catch (Exception e) {
						}

						if (!isAllowedIn) {
							continue;
						}

						String beanInfoClassName = b.getClassName() + "BeanInfo";
						Class<?> beanInfoClass = Class.forName(beanInfoClassName);
						BeanInfo bi = (BeanInfo) beanInfoClass.getConstructor().newInstance();
						BeanDescriptor bd = bi.getBeanDescriptor();
						String description = b.isDocumented() ? bd.getShortDescription() : "Not yet documented |";

						JSONObject jsonItem = new JSONObject();
						jsonItem.put("type", "Dbo");
						jsonItem.put("id", cn);
						jsonItem.put("name", bd.getDisplayName());
						jsonItem.put("classname", cn);
						jsonItem.put("description", description);
						jsonItem.put("icon", MySimpleBeanInfo.getIconName(bi, BeanInfo.ICON_COLOR_32x32));
						jsonItem.put("builtin", true);
						jsonItem.put("additional", false);
						jsonCategory.getJSONArray("items").put(jsonItem);
					}
					if (jsonCategory.getJSONArray("items").length() == 0) {
						categories.remove(jsonCategory);
					}
				}
			}
		}

		List<String> groups = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getGroups();
		List<com.twinsoft.convertigo.beans.ngx.components.dynamic.Component> components = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager
				.getComponents();

		for (String group : groups) {
			JSONObject jsonCategory = new JSONObject();
			jsonCategory.put("type", "Category");
			jsonCategory.put("name", group);
			jsonCategory.put("items", new JSONArray());
			categories.put(jsonCategory);

			for (com.twinsoft.convertigo.beans.ngx.components.dynamic.Component component : components) {
				if (component.getGroup().equals(group)) {
					String cn = "";
					try {
						cn = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(component)
								.getClass().getCanonicalName();
					} catch (Exception e) {
					}

					var isAllowedIn = parentDbo != null ? component.isAllowedIn(parentDbo) : false;
					if (folderType != null && isAllowedIn) {
						isAllowedIn = DatabaseObject.getFolderType(Class.forName(cn)) == folderType;
					}

					if (!isAllowedIn) {
						continue;
					}

					JSONObject jsonItem = new JSONObject();
					jsonItem.put("type", "Ion");
					jsonItem.put("id", "ngx " + component.getName());
					jsonItem.put("name", component.getLabel());
					jsonItem.put("classname", cn);
					jsonItem.put("description", component.getDescription());
					jsonItem.put("icon", component.getImagePath());
					jsonItem.put("builtin", component.isBuiltIn());
					jsonItem.put("additional", component.isAdditional());
					jsonCategory.getJSONArray("items").put(jsonItem);
				}
			}
			if (jsonCategory.getJSONArray("items").length() == 0) {
				categories.remove(jsonCategory);
			}
		}

		return categories;
	}

}
