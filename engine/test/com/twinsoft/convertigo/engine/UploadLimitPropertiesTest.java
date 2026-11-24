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

package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.util.HashSet;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.AdminUploadLimits;

public class UploadLimitPropertiesTest {
	private static final String REQUEST = PropertyName.FILE_UPLOAD_MAX_REQUEST_SIZE.getKey();
	private static final String FILE = PropertyName.FILE_UPLOAD_MAX_FILE_SIZE.getKey();

	@Before public void init() throws Exception {
		EnginePropertiesManager.initProperties();
		Engine.logEngine = null;
	}

	private static long request() { return EnginePropertiesManager.getPropertyAsLong(PropertyName.FILE_UPLOAD_MAX_REQUEST_SIZE); }
	private static long file() { return EnginePropertiesManager.getPropertyAsLong(PropertyName.FILE_UPLOAD_MAX_FILE_SIZE); }

	@Test public void everyPropertyHasItsOwnKey() {
		var keys = new HashSet<String>();
		for (var property : PropertyName.values()) {
			assertTrue(property.getKey(), keys.add(property.getKey()));
		}
	}

	@Test public void defaultsKeepTenMebibytesPerFileAndNoLimitForAdministrationUploads() {
		assertEquals(-1, request());
		assertEquals(10L << 20, file());
		assertEquals(-1, AdminUploadLimits.filesBytes());
		assertEquals(-1, AdminUploadLimits.requestBytes());
	}

	@Test public void requestAndFileLimitsAreSetIndependently() throws Exception {
		EnginePropertiesManager.syncProperties(REQUEST + "=1000\n" + FILE + "=10\n");
		assertEquals(1000, request());
		assertEquals(10, file());
	}

	@Test public void aConfigurationWithTheRequestKeyOnlyKeepsLimitingEachFileWithIt() throws Exception {
		EnginePropertiesManager.syncProperties(REQUEST + "=52428800\n");
		assertEquals(52428800, request());
		assertEquals(52428800, file());
	}

	@Test public void changingTheRequestLimitKeepsTheFileLimit() {
		EnginePropertiesManager.setProperty(PropertyName.FILE_UPLOAD_MAX_REQUEST_SIZE, "52428800");
		assertEquals(52428800, request());
		assertEquals(10L << 20, file());
	}

	@Test public void theAdministrationLimitIsOptionalAndLeavesRoomForTheMultipartEnvelope() throws Exception {
		EnginePropertiesManager.syncProperties(PropertyName.ADMIN_UPLOAD_MAX_SIZE.getKey() + "=0\n");
		assertEquals(-1, AdminUploadLimits.filesBytes());
		assertEquals(-1, AdminUploadLimits.requestBytes());
		EnginePropertiesManager.syncProperties(PropertyName.ADMIN_UPLOAD_MAX_SIZE.getKey() + "=2147483648\n");
		assertEquals(2147483648L, AdminUploadLimits.filesBytes());
		assertEquals(2147483648L + (1L << 20), AdminUploadLimits.requestBytes());
	}

	@Test public void aParserWithoutAdministrationLimitAcceptsAnySize() {
		var upload = new ServletFileUpload();
		for (long none : new long[] { -1, 0, Long.MAX_VALUE }) {
			AdminUploadLimits.configure(upload, none);
			assertEquals(-1, upload.getFileSizeMax());
			assertEquals(-1, upload.getSizeMax());
		}
		AdminUploadLimits.configure(upload, 10);
		assertEquals(10, upload.getFileSizeMax());
		assertEquals(10 + (1L << 20), upload.getSizeMax());
	}
}
