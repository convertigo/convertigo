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
import com.twinsoft.convertigo.engine.enums.StoreFiles;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.ZipUtils;

@ServiceDefinition(
		name = "UploadCustomStore",
		roles = { Role.WEB_ADMIN, Role.STORE_CONFIG },
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
			ServiceUtils.addMessage(document, document.getDocumentElement(), "The upload of the custom Store "
					+ item.getName() + " has failed. The archive file is not valid (.zip required).", "error",
					false);
		}

		super.doUpload(request, document, item);

		final String STORE_ARCHIVE_PATH = getRepository() + storeArchive;
		
		File tmpDir = File.createTempFile("store", null);
		tmpDir.delete();
		tmpDir.mkdirs();
		
		File srcStoreDir = null;
		try {
			ZipUtils.expandZip(STORE_ARCHIVE_PATH, tmpDir.getCanonicalPath());
			
			try {
				if (tmpDir.listFiles().length == 1) {
					srcStoreDir = tmpDir.listFiles()[0];
					String name = srcStoreDir.getName();

					// Throw IllegalArgumentException if not exist
					StoreFiles.valueOf(name.equals(StoreFiles.index.filename()) ? StoreFiles.index.name() : name);
				}

				srcStoreDir = tmpDir;
			}
			catch (IllegalArgumentException e) {
				// keep srcStoreDir
			}
			
			File storeDir = new File(getRepository(), StoreFiles.STORE_DIRECTORY_NAME);
			FileUtils.deleteDirectory(storeDir);
			
			if (!srcStoreDir.renameTo(storeDir)) {
				FileUtils.copyDirectory(srcStoreDir, storeDir, true);
				FileUtils.deleteDirectory(srcStoreDir);
			}
		} finally {
			new File(STORE_ARCHIVE_PATH).delete();
			FileUtils.deleteDirectory(tmpDir);
		}

		String message = "The custom Store has been successfully deployed.";
		Engine.logAdmin.info(message);
		ServiceUtils.addMessage(document, document.getDocumentElement(), message, "message", false);
	}
}
