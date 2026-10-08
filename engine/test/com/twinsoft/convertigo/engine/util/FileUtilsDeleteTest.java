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

import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

public class FileUtilsDeleteTest {
	@Test
	public void deletesATreeWithAReadOnlyFolder() throws Exception {
		var base = Files.createTempDirectory("file-utils-delete").toFile();
		var tree = new File(base, "_remove_project_____");
		var readOnly = new File(tree, "DisplayObjects/mobile/tinymce");
		readOnly.mkdirs();
		Files.writeString(new File(readOnly, "license.txt").toPath(), "license");
		assumeTrue("a file system with permissions", readOnly.setWritable(false, false));
		try {
			FileUtils.deleteDirectory(tree);
			assertFalse("the tree is deleted, read-only folder included", tree.exists());
		} finally {
			readOnly.setWritable(true, true);
			org.apache.commons.io.FileUtils.deleteQuietly(base);
		}
	}
}
