/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.studio.properties;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ClassUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder.BuilderUtils;
import com.twinsoft.convertigo.engine.util.EnumUtils;

@ServiceDefinition(name = "Set", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Set extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {

		// id: the id of the target bean in tree
		var id = request.getParameter("id");
		if (id == null) {
			throw new ServiceException("missing id parameter");
		}

		var save = request.getParameter("save");
		var props = request.getParameter("props");
		var prop = request.getParameter("prop");
		if (prop != null && props == null) {
			props = "[" + prop + "]";
		}
		if (props == null) {
			throw new ServiceException("missing prop or props parameter");
		}

		boolean done = false;
		DatabaseObject dbo = Utils.getDbo(id);
		if (dbo != null) {
			var jsonArray = new JSONArray(props);
			for (int i = 0; i < jsonArray.length(); i++) {
				Object oldValue = null, newValue = null;
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				var pname = jsonObject.getString("name");
				var pvalue = jsonObject.getString("value");
				var mode = jsonObject.has("mode") ? jsonObject.getString("mode") : "plain";

				MobileSmartSourceType msst = new MobileSmartSourceType(pvalue);
				if ("script".equals(mode)) {
					msst = new MobileSmartSourceType();
					msst.setMode(Mode.SCRIPT);
					msst.setSmartValue(pvalue);
				} else if ("source".equals(mode)) {
					msst = new MobileSmartSourceType();
					msst.setMode(Mode.SOURCE);
					msst.setSmartValue(pvalue);
				}

				BeanInfo beanInfo = Introspector.getBeanInfo(dbo.getClass());
				PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
				for (PropertyDescriptor pd : propertyDescriptors) {
					if (pd.getName().equals(pname)) {
						Method setter = pd.getWriteMethod();
						Method getter = pd.getReadMethod();
						Class<?> pdc = pd.getPropertyEditorClass();
						Class<?> ptc = pd.getPropertyType();

						oldValue = getter.invoke(dbo);

						if (pdc != null && pdc.getSimpleName().equals("NgxSmartSourcePropertyDescriptor")) {
							setter.invoke(dbo, new Object[] { msst });
						} else if (pname.equals("actionValue")) {// CustomAction
							FormatedContent fc = new FormatedContent(pvalue);
							setter.invoke(dbo, new Object[] { fc });
						} else {
							String propertyValue = dbo.compileProperty(pname, pvalue).toString();
							Object oPropertyValue = createObject(ptc, propertyValue);

							if (dbo.isCipheredProperty(pname)) {
								String initialValue = (String) getter.invoke(dbo, (Object[]) null);

								if (oPropertyValue.equals(initialValue)
										|| DatabaseObject.encryptPropertyValue(initialValue).equals(oPropertyValue)) {
									oPropertyValue = initialValue;
									// } else{
									// dbo.hasChanged = true;
								}
							}

							if (oPropertyValue != null) {
								Object args[] = { oPropertyValue };
								setter.invoke(dbo, args);
							}
						}

						newValue = getter.invoke(dbo);

						break;
					}
				}

				if (dbo instanceof UIDynamicElement) {
					IonBean ionBean = ((UIDynamicElement) dbo).getIonBean();
					if (ionBean != null && ionBean.getProperty(pname) != null) {
						oldValue = ionBean.getPropertyValue(pname);
						ionBean.setPropertyValue(pname, msst);
						newValue = ionBean.getPropertyValue(pname);
					}
				}

				if (oldValue != null && newValue != null) {
					done = true;
					dbo.hasChanged = true;

					// notify for app generation
					BuilderUtils.dboChanged(dbo, pname, oldValue, newValue);
				}
			}
			if (save != null && save.equals("true")) {
				Engine.theApp.databaseObjectsManager.exportProject(dbo.getProject());
			}
		}

		if (done) {
			response.put("done", true);
			response.put("id", dbo.getFullQName());
			response.put("state", "success");
			response.put("message", "Properties have been successfully updated!");
		} else {
			response.put("done", false);
		}
	}

	public static Object createObject(Class<?> propertyClass, String value) throws ServiceException {
		Object oPropertyValue = null;

		propertyClass = ClassUtils.primitiveToWrapper(propertyClass);
		if (Number.class.isAssignableFrom(propertyClass) || String.class.isAssignableFrom(propertyClass)) {
			try {
				oPropertyValue = propertyClass.getConstructor(String.class).newInstance(value);
			} catch (Exception e) {
				throw new ServiceException("Error when create the object:\n" + e.getMessage());
			}
		} else if (Boolean.class.isAssignableFrom(propertyClass)) {
			oPropertyValue = Boolean.parseBoolean(value);
		} else if (Enum.class.isAssignableFrom(propertyClass)) {
			oPropertyValue = EnumUtils.valueOf(propertyClass, value);
		} else if (Object.class.equals(propertyClass)) {
			oPropertyValue = value;
		}

		return oPropertyValue;
	}

}
