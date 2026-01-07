/*
 * Copyright (c) 2001-2025 Convertigo SA.
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
import java.net.URI;

import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.fop.apps.io.ResourceResolverFactory;

public final class FopUtils {

	private FopUtils() {
	}

	public static FopFactory newFopFactory(String projectDirectory, String templatesPath) {
		var basePath = projectDirectory;
		if (basePath == null || basePath.isEmpty()) {
			basePath = templatesPath;
		}
		if (basePath == null || basePath.isEmpty()) {
			basePath = ".";
		}
		var baseUri = toBaseUri(basePath);
		var builder = new FopFactoryBuilder(baseUri);

		if (projectDirectory != null && !projectDirectory.isEmpty()
				&& templatesPath != null && !templatesPath.isEmpty()) {
			var templatesUri = toBaseUri(templatesPath);
			var templatesResolver = ResourceResolverFactory.createDefaultInternalResourceResolver(templatesUri);
			// Match FOP1 behavior: fonts/hyphenation keep using templates base.
			builder.getFontManager().setResourceResolver(templatesResolver);
			builder.setHyphenBaseResourceResolver(templatesResolver);
		}

		return builder.build();
	}

	private static URI toBaseUri(String basePath) {
		var uri = new File(basePath).toURI();
		var uriString = uri.toASCIIString();
		if (!uriString.endsWith("/")) {
			uri = URI.create(uriString + "/");
		}
		return uri;
	}
}
