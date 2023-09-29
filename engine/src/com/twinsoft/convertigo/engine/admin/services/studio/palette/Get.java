/**
 * 
 */
package com.twinsoft.convertigo.engine.admin.services.studio.palette;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

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
import com.twinsoft.convertigo.engine.enums.FolderType;

@ServiceDefinition(
		name = "Get",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW },
		parameters = {},
		returnValue = ""
		)
public class Get extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		response.put("id", id);
		response.put("categories", getPalette(id));
	}

	private JSONArray getPalette(String id) throws Exception {
		var reg = Utils.parseQName.matcher(id);
		reg.matches();
		var ft = FolderType.parse(reg.group(2));
		var qname = ft == null ? id : reg.group(1);
		var parentDbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
		
		JSONArray categories = new JSONArray();

		for (DboGroup g: Engine.theApp.getDboExplorerManager().getGroups()) {
			String groupName = g.getName();
			for (DboCategory c: g.getCategories()) {
				String categoryName = c.getName().isEmpty() ? groupName : c.getName();

				for (DboBeans bs: c.getBeans()) {
					String category = bs.getName().isEmpty() ? categoryName : bs.getName();
					
					JSONObject jsonCategory = new JSONObject();
					jsonCategory.put("type", "Category");
					jsonCategory.put("name", category);
					jsonCategory.put("items", new JSONArray());
					categories.put(jsonCategory);
					
					for (DboBean b: bs.getBeans()) {
						String cn = b.getClassName();
						if ((cn.startsWith("com.twinsoft.convertigo.beans.ngx.components.")
								|| cn.startsWith("com.twinsoft.convertigo.beans.mobile.components."))
								&& !cn.endsWith("PageComponent")) {
							continue;
						}

						var isAllowedIn = false;
						try {
							boolean force = false;
							if (parentDbo != null /*isType[0]*/) {
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
						} catch (Exception e) {}
						
						if (!isAllowedIn) continue;
						
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
		List<com.twinsoft.convertigo.beans.ngx.components.dynamic.Component> components = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponents();
		
		for (String group : groups) {
			JSONObject jsonCategory = new JSONObject();
			jsonCategory.put("type", "Category");
			jsonCategory.put("name", group);
			jsonCategory.put("items", new JSONArray());
			categories.put(jsonCategory);
			
			for (com.twinsoft.convertigo.beans.ngx.components.dynamic.Component component : components) {
				var isAllowedIn = parentDbo != null ? component.isAllowedIn(parentDbo) : false;
				
				if (!isAllowedIn) continue;
				
				if (component.getGroup().equals(group)) {
					String cn= "";
					try {
						cn = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(component).getClass().getCanonicalName();
					} catch (Exception e) {}
					
					JSONObject jsonItem = new JSONObject();
					jsonItem.put("type", "Dbo");
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
