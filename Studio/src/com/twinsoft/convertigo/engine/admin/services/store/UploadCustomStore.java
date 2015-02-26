package com.twinsoft.convertigo.engine.admin.services.store;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.UploadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.util.ZipUtils;

@ServiceDefinition(
		name = "UploadCustomStore",
		roles = { Role.WEB_ADMIN },
		parameters = {},
		returnValue = ""
	)
public class UploadCustomStore extends UploadService {
	@Override
	protected String getRepository() {
		return Engine.USER_WORKSPACE_PATH + "/";
	}

	@Override
	protected void doUpload(HttpServletRequest request, Document document, FileItem item) throws Exception {
		String storeArchive = item.getName();

		if (!storeArchive.endsWith(".zip")) {
			ServiceUtils.addMessage(document, document.getDocumentElement(), "The upload of the custom store "
					+ item.getName() + " has failed. The archive file is not valid (.zip required).", "error",
					false);
		}

		super.doUpload(request, document, item);

		final String STORE_ARCHIVE_PATH = getRepository() + storeArchive;
		try {
			ZipUtils.expandZip(STORE_ARCHIVE_PATH, getRepository() + "store");
		}
		finally {
			new File(STORE_ARCHIVE_PATH).delete();
		}

		String message = "The custom store has been successfully deployed.";
		Engine.logAdmin.info(message);
		ServiceUtils.addMessage(document, document.getDocumentElement(), message, "message", false);
	}
}
