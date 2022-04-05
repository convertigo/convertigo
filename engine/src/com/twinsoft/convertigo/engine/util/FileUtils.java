/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.engine.Engine;


@SuppressWarnings("deprecation")
public class FileUtils extends org.apache.commons.io.FileUtils {
	private static Pattern CrlfPattern = Pattern.compile("\\r\\n");
	public static final String UTF8_BOM = "\uFEFF";

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

	public static void saveProperties(Map<String, String> map, File file, String encoding) throws IOException {
		PrintStream ps = new PrintStream(file, encoding);
		for (Entry<String, String> entry : map.entrySet()) {
			ps.println(entry.getKey());
			ps.println(entry.getValue());
			ps.println();
		}
		ps.close();
	}

	public static void loadProperties(Map<String, String> map, File file, String encoding) throws IOException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
			String key = null;
			String line;
			int cpt = 1;
			while ((line = br.readLine()) != null) {
				if (line.length() != 0) {
					if (cpt % 3 == 1) {
						key = line;
					} else if (cpt % 3 == 2) {
						map.put(key, line);
					} else {
						throw new IOException("The line number " + cpt + " must be empty (" + line + ")");
					}
				} else {
					if (cpt % 3 != 0) {
						throw new IOException("The line number " + cpt + " must not be empty (last key =" + key + ")");
					}
				}
				cpt++;
			}
		} finally {
			if (br != null) br.close();
		}
	}

	public static String toUriString(File file) throws MalformedURLException {
		String fileUrl = file.toURI().toURL().toString();
		return fileUrl;
	}

	public static String createTmpFileWithUTF8Data(String parentPath, String childPath, String content) throws FileNotFoundException, IOException {
		File tempEditorFile = new File(parentPath, childPath);
		try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(tempEditorFile), StandardCharsets.UTF_8)) {
			osw.append(content);
		}
		return tempEditorFile.getName();
	}

	public static boolean createFolderIfNotExist(String parent, String child) {
		File privateFolder = new File(parent, child);
		if (!privateFolder.exists()) {
			return privateFolder.mkdir();
		}
		return false;
	}

	public static void deleteDirectory(File dir) throws IOException {
		if (dir.exists()) {
			if (dir.isDirectory()) {
				int code = -1;
				try {
					if (Engine.isWindows()) {
						code = new ProcessBuilder("cmd.exe", "/C", "rmdir", "/s", "/q", dir.getCanonicalPath()).start().waitFor();
					} else {
						code = new ProcessBuilder("rm", "-rf", dir.getCanonicalPath()).start().waitFor();
					}
				} catch (Exception e) {
					try {
						if (Engine.isWindows()) {
							code = new ProcessBuilder("cmd.exe", "/C", "rmdir", "/s", "/q", dir.getCanonicalPath()).start().waitFor();
						} else {
							code = new ProcessBuilder("rm", "-rf", dir.getCanonicalPath()).start().waitFor();
						}
					} catch (Exception e2) {
						Engine.logEngine.warn("System remove dir failed to delete the folder " + dir + ". Use the Java version. Error is: " + e2);
					}
				}

				if (code != 0) {
					org.apache.commons.io.FileUtils.deleteDirectory(dir);
				}
			}
		}
	}

	public static boolean deleteQuietly(File f) {
		if (f != null && f.isDirectory()) {
			try {
				FileUtils.deleteDirectory(f);
				return true;
			} catch (Exception e) {
			}
		}
		return org.apache.commons.io.FileUtils.deleteQuietly(f);
	}

	
	private static final Pattern incrementFilenamePattern = Pattern.compile("(.*?)(?:(_)(\\d+))?(?:(\\.\\w*?)(\\d+)?)?$");
	public static File incrementFilename(File file) {
		while (file.exists()) {
			String filename = file.getName();
			Matcher matcher = incrementFilenamePattern.matcher(filename);
			if (matcher.matches()) {
				filename = matcher.group(1);
				if (matcher.group(5) != null) {
					String cpt = org.apache.commons.lang3.StringUtils.leftPad(Integer.toString(Integer.parseInt(matcher.group(5)) + 1), matcher.group(5).length(), '0');
					if (matcher.group(2) != null) {
						filename += matcher.group(2) + matcher.group(3);
					}
					filename += matcher.group(4) + cpt;
				} else {
					if (matcher.group(3) != null) {
						String cpt = org.apache.commons.lang3.StringUtils.leftPad(Integer.toString(Integer.parseInt(matcher.group(3)) + 1), matcher.group(3).length(), '0');
						filename += matcher.group(2) + cpt;
					} else {
						filename += "_1";
					}
					if (matcher.group(4) != null) {
						filename += matcher.group(4);
					}
				}
			} else {
				break;
			}
			
			file = new File(file.getParentFile(), filename);
		}
		return file;
	}
	
	public static String removeBOM(String str) {
		if (str != null && str.startsWith(UTF8_BOM)) {
			str = str.substring(1);
		}
		return str;
	}
}
