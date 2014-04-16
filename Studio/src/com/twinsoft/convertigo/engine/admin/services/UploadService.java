/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine.admin.services;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public abstract class UploadService extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		// Check that we have a file upload request
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);

		if (!isMultipart)
			throw new IllegalArgumentException("Not multipart content!");

		FileItemFactory factory = new DiskFileItemFactory();

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(factory);

		// Parse the request
		List<FileItem> items = GenericUtils.cast(upload.parseRequest(request));

		// Process the uploaded items
		handleFormFields(request);
		for (FileItem item : items) {
			doUpload(request, document, item);
		}
	}

	protected void handleFormFields(HttpServletRequest request) {
	}

	protected void doUpload(HttpServletRequest request, Document document, FileItem item) throws Exception {
		File fullFile = new File(item.getName());
		File savedFile = new File(getRepository(), fullFile.getName());
		Engine.logAdmin.info("savedFile: "+savedFile.getAbsolutePath());
		item.write(savedFile);
	}

	protected abstract String getRepository();

}
