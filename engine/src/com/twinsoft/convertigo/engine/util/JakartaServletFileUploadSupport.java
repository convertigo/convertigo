/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.UploadContext;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.twinsoft.convertigo.engine.Engine;

public class JakartaServletFileUploadSupport {
	private static final String UPLOAD_FOLDER = "convertigo.multipart.folder";

	private JakartaServletFileUploadSupport() {
	}

	/**
	 * A new temporary folder for the items of a multipart request, kept by the request. The folder is only created to
	 * write an item, by the factory over its size threshold or for an uploaded file. The response can stream one of
	 * the uploaded files, as a binary attachment: {@link #removeUploadFolder} removes them once the response is written.
	 */
	public static File newUploadFolder(HttpServletRequest request) throws IOException {
		var folder = File.createTempFile("c8o-multipart-files", ".tmp");
		folder.delete();
		request.setAttribute(UPLOAD_FOLDER, folder);
		return folder;
	}

	/** Removes the upload folder of the request with its files, if it was created, once the response is written. */
	public static void removeUploadFolder(HttpServletRequest request) {
		if (request.getAttribute(UPLOAD_FOLDER) instanceof File folder) {
			request.removeAttribute(UPLOAD_FOLDER);
			if (folder.exists()) {
				try {
					Engine.logEngine.debug("(JakartaServletFileUploadSupport) Removing the temporary upload folder : " + folder.getAbsolutePath());
					FileUtils.deleteDirectory(folder);
				} catch (IOException | RuntimeException e) {
					Engine.logEngine.warn("(JakartaServletFileUploadSupport) Failed to delete: " + folder + " [" + e.getMessage() + "]");
				}
			}
		}
	}

	public static boolean isMultipartContent(HttpServletRequest request) {
		return ServletFileUpload.isMultipartContent(new JakartaUploadContext(request));
	}

	public static List<FileItem> parseRequest(ServletFileUpload upload, HttpServletRequest request) throws FileUploadException {
		return GenericUtils.cast(upload.parseRequest(new JakartaUploadContext(request)));
	}

	private static class JakartaUploadContext implements UploadContext {
		private final HttpServletRequest request;

		private JakartaUploadContext(HttpServletRequest request) {
			this.request = request;
		}

		@Override
		public String getCharacterEncoding() {
			return request.getCharacterEncoding();
		}

		@Override
		public int getContentLength() {
			return request.getContentLength();
		}

		@Override
		public long contentLength() {
			return request.getContentLengthLong();
		}

		@Override
		public String getContentType() {
			return request.getContentType();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return request.getInputStream();
		}
	}
}
