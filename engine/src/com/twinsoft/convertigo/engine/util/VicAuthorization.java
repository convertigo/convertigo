/*
 * Copyright (c) 2001-2026 Convertigo SA.
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
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/** Compatibility bridge to the optional legacy VIC authorization provider. */
public final class VicAuthorization {

	private VicAuthorization() {
	}

	public static boolean isServiceAuthorized(String userName, String virtualServer, String service)
			throws IOException {
		try {
			Class<?> providerClass = Class.forName("com.twinsoft.convertigo.engine.plugins.VicApi");
			Object provider = providerClass.getConstructor().newInstance();
			return Boolean.TRUE.equals(providerClass
					.getMethod("isServiceAuthorized", String.class, String.class, String.class)
					.invoke(provider, userName, virtualServer, service));
		} catch (InvocationTargetException e) {
			throw new IOException("The VIC authorization provider failed.", e.getCause());
		} catch (ReflectiveOperationException | LinkageError e) {
			throw new IOException("The optional VIC authorization provider is unavailable. "
					+ "Install a compatible provider plugin before using VIC authorization.", e);
		}
	}
}
