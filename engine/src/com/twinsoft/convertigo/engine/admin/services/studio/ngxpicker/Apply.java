/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it under the terms of the GNU Affero General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 */

package com.twinsoft.convertigo.engine.admin.services.studio.ngxpicker;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource.Filter;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonProperty;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder.BuilderUtils;
import com.twinsoft.convertigo.engine.studio.NgxSourcePickerModel;

@ServiceDefinition(name = "Apply", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Apply extends JSonService {
	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		String id = request.getParameter("id");
		if (id == null) {
			throw new ServiceException("missing id parameter");
		}
		String propertyName = request.getParameter("propertyName");
		if (propertyName == null || propertyName.isBlank()) {
			throw new ServiceException("missing propertyName parameter");
		}
		String sourceData = request.getParameter("sourceData");
		if (sourceData == null || sourceData.isBlank()) {
			throw new ServiceException("missing sourceData parameter");
		}

		DatabaseObject dbo = Utils.getDbo(id);
		if (dbo == null) {
			response.put("done", false);
			response.put("state", "not_found");
			response.put("message", "Target object not found");
			return;
		}

		Filter filter = parseFilter(request.getParameter("filter"));
		MobileSmartSourceType current = NgxSourcePickerModel.readSmartSourceType(dbo, propertyName);
		String input = "";
		if (current != null && current.getSmartSource() != null) {
			input = current.getSmartSource().getInput();
		}
		JSONObject sourceValue = NgxSourcePickerModel.toSourceValue(
				dbo.getProject().getName(),
				filter,
				new JSONObject(sourceData),
				request.getParameter("path"),
				request.getParameter("prefix"),
				request.getParameter("suffix"),
				request.getParameter("custom"),
				Boolean.parseBoolean(request.getParameter("useCustom")),
				input);

		MobileSmartSourceType smartType = new MobileSmartSourceType();
		smartType.setMode(Mode.SOURCE);
		smartType.setSmartValue(sourceValue.toString());

		if (!applySmartType(dbo, propertyName, smartType)) {
			response.put("done", false);
			response.put("state", "not_available");
			response.put("message", "No NGX source property named " + propertyName);
			return;
		}

		response.put("done", true);
		response.put("id", dbo.getFullQName());
		response.put("state", "success");
		response.put("propertyName", propertyName);
		response.put("sourceValue", sourceValue);
		response.put("computedValue", smartType.getValue());
		response.put("message", "Source has been successfully applied!");
	}

	private static Filter parseFilter(String value) throws ServiceException {
		try {
			Filter filter = value == null || value.isBlank() ? Filter.Sequence : Filter.valueOf(value);
			if (!NgxSourcePickerModel.isSupported(filter)) {
				throw new ServiceException("NGX source filter not available yet: " + filter.name());
			}
			return filter;
		} catch (Exception e) {
			if (e instanceof ServiceException serviceException) {
				throw serviceException;
			}
			throw new ServiceException("Unsupported NGX source filter " + value);
		}
	}

	private static boolean applySmartType(DatabaseObject dbo, String propertyName, MobileSmartSourceType smartType)
			throws Exception {
		if (dbo instanceof UIDynamicElement dynamicElement) {
			IonBean ionBean = dynamicElement.getIonBean();
			IonProperty property = ionBean == null ? null : ionBean.getProperty(propertyName);
			if (property != null) {
				Object oldValue = ionBean.getPropertyValue(propertyName);
				ionBean.setPropertyValue(propertyName, smartType);
				Object newValue = ionBean.getPropertyValue(propertyName);
				dbo.hasChanged = true;
				BuilderUtils.dboChanged(dbo, propertyName, oldValue, newValue);
				return true;
			}
		}

		for (PropertyDescriptor descriptor : Introspector.getBeanInfo(dbo.getClass()).getPropertyDescriptors()) {
			if (!descriptor.getName().equals(propertyName) || descriptor.getWriteMethod() == null
					|| descriptor.getReadMethod() == null || descriptor.getPropertyEditorClass() == null
					|| !"NgxSmartSourcePropertyDescriptor".equals(descriptor.getPropertyEditorClass().getSimpleName())) {
				continue;
			}
			Object oldValue = descriptor.getReadMethod().invoke(dbo);
			descriptor.getWriteMethod().invoke(dbo, smartType);
			Object newValue = descriptor.getReadMethod().invoke(dbo);
			dbo.hasChanged = true;
			BuilderUtils.dboChanged(dbo, propertyName, oldValue, newValue);
			return true;
		}
		return false;
	}
}
