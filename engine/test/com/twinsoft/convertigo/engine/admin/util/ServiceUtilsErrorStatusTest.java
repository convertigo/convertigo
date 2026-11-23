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

package com.twinsoft.convertigo.engine.admin.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.junit.Test;

import com.twinsoft.convertigo.engine.util.AdminUploadLimits;

public class ServiceUtilsErrorStatusTest {
	private final AtomicInteger status = new AtomicInteger();
	private final StringWriter body = new StringWriter();

	private HttpServletResponse response() {
		var writer = new PrintWriter(body);
		return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { HttpServletResponse.class },
				(proxy, method, args) -> switch (method.getName()) {
			case "setStatus" -> { status.set((Integer) args[0]); yield null; }
			case "getWriter" -> writer;
			case "isCommitted" -> false;
			default -> null;
		});
	}

	private static HttpServletRequest request() {
		return (HttpServletRequest) Proxy.newProxyInstance(ServiceUtilsErrorStatusTest.class.getClassLoader(),
				new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> null);
	}

	@Test public void anUploadOverItsLimitIsRefusedWith413() throws Exception {
		ServiceUtils.handleError(AdminUploadLimits.tooLarge(2, 1), request(), response());
		assertEquals(413, status.get());
		assertTrue(body.toString(), body.toString().contains("net.upload.admin.max_size"));
	}

	@Test public void aWrappedUploadOverItsLimitIsRefusedWith413() throws Exception {
		ServiceUtils.handleError(new IOException("wrapped", AdminUploadLimits.tooLarge(2, 1)), request(), response());
		assertEquals(413, status.get());
	}

	@Test public void aFileOverItsLimitIsRefusedWith413() throws Exception {
		ServiceUtils.handleError(new FileSizeLimitExceededException("too large", 2, 1), request(), response());
		assertEquals(413, status.get());
	}

	@Test public void otherFailuresStayServerErrors() throws Exception {
		ServiceUtils.handleError(new IOException("broken"), request(), response());
		assertEquals(500, status.get());
	}
}
