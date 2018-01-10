package com.twinsoft.convertigo.engine.admin.services.studio.palette;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;

@ServiceDefinition(
		name = "GetIconsCSS",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG, Role.PROJECT_DBO_VIEW },
		parameters = {},
		returnValue = ""
	)
public class GetIconsCSS extends DownloadService {

	@Override
	protected void writeResponseResult(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Set<String> classNames = CachedIntrospector.getClassNames();
		StringBuilder sb = new StringBuilder();
		for (String className : classNames) {
			sb.append(".")
			  .append(className.replaceAll("\\.", "-"))
			  .append("-32")
			  .append(" {background-image:url(")

			  .append("../../admin/services/database_objects.GetIcon?className=")
			  .append(className)
			  .append("&large=true")
			  .append(") !important;")
			  .append("}\r\n");
		}

		response.setContentType(MimeType.Css.value());
		IOUtils.write(sb.toString(), response.getOutputStream(), "UTF-8");			
	}
}
