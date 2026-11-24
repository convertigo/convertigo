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

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.apache.commons.fileupload.FileUploadBase.SizeLimitExceededException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/**
 * Optional size limit of the administration uploads (project archives, bundles, imports). There is none by default:
 * the reverse proxy or ingress in front of Convertigo limits the requests. When the engine property is set, it drives
 * the upload services and the forward of administration requests between instances alike.
 */
public final class AdminUploadLimits {
	/** Room for the multipart envelope (boundaries, part headers and form fields) above the uploaded files. */
	public static final long ENVELOPE_BYTES = 1L << 20;

	private AdminUploadLimits() {
	}

	/** Maximum total size of the files of one administration request, in bytes, or -1 without limit (default). */
	public static long filesBytes() {
		long value = EnginePropertiesManager.getPropertyAsLong(PropertyName.ADMIN_UPLOAD_MAX_SIZE);
		return limited(value) ? value : -1;
	}

	/** Maximum size of a complete administration request (the files and their multipart envelope), or -1. */
	public static long requestBytes() {
		return requestBytes(filesBytes());
	}

	/** Maximum size of a request carrying at most {@code filesBytes} bytes of files, or -1 without limit. */
	public static long requestBytes(long filesBytes) {
		return limited(filesBytes) && filesBytes <= Long.MAX_VALUE - ENVELOPE_BYTES ? filesBytes + ENVELOPE_BYTES : -1;
	}

	/** Applies a limit of the files ({@code -1} or {@code Long.MAX_VALUE}: none) to a multipart parser: each file and the whole request. */
	public static void configure(ServletFileUpload upload, long filesBytes) {
		upload.setFileSizeMax(limited(filesBytes) ? filesBytes : -1);
		upload.setSizeMax(requestBytes(filesBytes));
	}

	private static boolean limited(long bytes) {
		return bytes > 0 && bytes < Long.MAX_VALUE;
	}

	/** The error of an administration request over the limit, answered with HTTP 413. */
	public static SizeLimitExceededException tooLarge(long actual, long permitted) {
		return new SizeLimitExceededException("The upload (" + actual + " bytes) exceeds the administration upload limit ("
				+ permitted + " bytes of files, engine property " + PropertyName.ADMIN_UPLOAD_MAX_SIZE.getKey() + ")", actual, permitted);
	}

	/** Parses an administration upload: an upload over the limits of the parser is reported with the property that sets them. */
	public static List<FileItem> parse(ServletFileUpload upload, HttpServletRequest request) throws FileUploadException {
		try {
			return JakartaServletFileUploadSupport.parseRequest(upload, request);
		} catch (SizeLimitExceededException e) {
			throw tooLarge(e.getActualSize(), upload.getFileSizeMax());
		} catch (FileSizeLimitExceededException e) {
			throw tooLarge(e.getActualSize(), upload.getFileSizeMax());
		}
	}

	/** True when the error, or one of its causes, is an upload over a size limit. */
	public static boolean isTooLarge(Throwable throwable) {
		for (int depth = 0; throwable != null && depth < 20; depth++, throwable = throwable.getCause()) {
			if (throwable instanceof SizeLimitExceededException || throwable instanceof FileSizeLimitExceededException) {
				return true;
			}
		}
		return false;
	}
}
