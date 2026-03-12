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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.UploadContext;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class JakartaServletFileUploadSupport {

	private JakartaServletFileUploadSupport() {
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
