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

package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.engine.Engine;

public class FileUtils extends org.apache.commons.io.FileUtils {
	private static Pattern CrlfPattern = Pattern.compile("\\r\\n");

	public static void mergeDirectories(File srcDir, File destDir) throws IOException {
		mergeDirectories(srcDir, destDir, true);
	}

	public static void mergeDirectories(File srcDir, File destDir, boolean preserveFileDate) throws IOException {
		File[] files = srcDir.listFiles();
		if (files == null) { // null if security restricted
			throw new IOException("Failed to list contents of " + srcDir);
		}
		if (destDir.exists()) {
			if (destDir.isDirectory() == false) {
				throw new IOException("Destination '" + destDir + "' exists but is not a directory");
			}
		} else {
			if (destDir.mkdirs() == false) {
				throw new IOException("Destination '" + destDir + "' directory cannot be created");
			}
			String message = "> Directory '" + destDir + "' created";
			if (Engine.logEngine != null) {
				Engine.logEngine.info(message);
			} else {
				System.out.println(message);
			}
		}
		if (destDir.canWrite() == false) {
			throw new IOException("Destination '" + destDir + "' cannot be written to");
		}
		for (File file : files) {
			File copiedFile = new File(destDir, file.getName());
			if (file.isDirectory()) {
				mergeDirectories(file, copiedFile, preserveFileDate);
			} else {
				if (!copiedFile.exists()) {
					FileUtils.copyFile(file, copiedFile, preserveFileDate);
					String message = "+ File '" + file + "' copied from " + srcDir;
					if (Engine.logEngine != null) {
						Engine.logEngine.info(message);
					} else {
						System.out.println(message);
					}
				}
			}
		}
	}

	public static boolean isCRLF() {
		return System.getProperty("line.separator").contains("\r\n");
	}
	
	public static String CrlfToLf(String content) {
		return CrlfPattern.matcher(content).replaceAll("\n");
	}
}