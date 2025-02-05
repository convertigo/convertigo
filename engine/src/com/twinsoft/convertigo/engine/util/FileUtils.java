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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.engine.Engine;


@SuppressWarnings("deprecation")
public class FileUtils extends org.apache.commons.io.FileUtils {
	private static final int BUFFER_SIZE = 4096;

	private static Pattern CrlfPattern = Pattern.compile("\\r\\n");
	public static final String UTF8_BOM = new String(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, StandardCharsets.UTF_8);

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
					System.out.println(message);
					if (Engine.logEngine != null) {
						Engine.logEngine.info(message);
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
				ProcessBuilder pb;
				try {
					pb = Engine.isWindows() ?
							new ProcessBuilder("cmd.exe", "/C", "rmdir", "/s", "/q", dir.getCanonicalPath())
							:new ProcessBuilder("rm", "-rf", dir.getCanonicalPath());
					code = pb.inheritIO().start().waitFor();
				} catch (Exception e) {
				}

				if (code == -1) {
					try {
						Thread.sleep(500);
						pb = Engine.isWindows() ?
								new ProcessBuilder("cmd.exe", "/C", "rmdir", "/s", "/q", dir.getCanonicalPath())
								:new ProcessBuilder("rm", "-rf", dir.getCanonicalPath());
						code = pb.inheritIO().start().waitFor();
					} catch (Exception e) {
					}
				}

				if (code != 0 || dir.exists()) {
					org.apache.commons.io.FileUtils.deleteDirectory(dir);
				}
				if (dir.exists()) {
					org.apache.commons.io.FileUtils.forceDelete(dir);
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

	public static FileInputStream newFileInputStreamSkipBOM(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		try {
			byte[] buf = new byte[3];
			if (fis.read(buf) == buf.length && new String(buf, StandardCharsets.UTF_8).equals(UTF8_BOM)) {
				return fis;
			}
		} catch (Exception e) {
		}
		try {
			fis.close();
		} catch (Exception e) {
		}
		fis = new FileInputStream(file);
		return fis;
	}

	public static boolean areFilesIdentical(File file, String newContent, Charset charset) throws IOException {
		if (file == null || !file.exists()) {
			return false;
		}
		try (var fileStream = new BufferedInputStream(new FileInputStream(file));
				InputStream contentStream = new ByteArrayInputStream(newContent.getBytes(charset))) {

			byte[] fileBuffer = new byte[BUFFER_SIZE];
			byte[] contentBuffer = new byte[BUFFER_SIZE];

			int fileRead, contentRead;
			while ((fileRead = fileStream.read(fileBuffer)) != -1) {
				contentRead = contentStream.read(contentBuffer);

				if (contentRead != fileRead || Arrays.compare(fileBuffer, 0, fileRead, contentBuffer, 0, contentRead) != 0) {
					return false;
				}
			}
			return contentStream.read() == -1;
		}
	}

	public static void writeFile(File file, String content, Charset charset) throws IOException {
		if (areFilesIdentical(file, content, charset)) {
			return;
		}
		Files.createDirectories(file.toPath().getParent());
		Files.writeString(file.toPath(), content, charset);
	}

	public static Set<File> indexExistingFiles(File rootDir) {
		if (rootDir == null) {
			return null;
		}
		var files = new HashSet<File>();
		if (rootDir.exists() && rootDir.isDirectory()) {
			try (var paths = Files.walk(rootDir.toPath())) {
				paths.filter(Files::isRegularFile).map(Path::toFile).forEach(files::add);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return files;
	}

	public static void copyFileIfNeeded(File srcFile, File destFile, Set<File> existingFiles) throws IOException {
		if (existingFiles != null) {
			existingFiles.remove(destFile);
		}
		if (destFile.exists()) {
			if (Files.mismatch(srcFile.toPath(), destFile.toPath()) == -1) {
				return;
			}
		}

		Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
	}


	public static void copyDirectoryOptimized(File srcDir, File destDir, Set<File> existingFiles) throws IOException {
		copyDirectoryOptimized(srcDir, destDir, p -> true, existingFiles);
	}

	public static void copyDirectoryOptimized(File srcDir, File destDir, FileFilter filter, Set<File> existingFiles) throws IOException {
		if (!srcDir.exists()) {
			return;
		}

		destDir.mkdirs();

		var files = srcDir.listFiles(filter);
		if (files == null) {
			return;
		}

		for (var srcFile : files) {
			var destFile = new File(destDir, srcFile.getName());

			if (srcFile.isDirectory()) {
				copyDirectoryOptimized(srcFile, destFile, filter, existingFiles);
			} else {
				copyFileIfNeeded(srcFile, destFile, existingFiles);
			}
		}
	}

	public static boolean deleteWithParents(File file) {
		if (file.delete()) {
			return deleteWithParents(file.getParentFile());
		}
		return false;
	}

	public static void deleteFiles(Set<File> existingFiles) {
		for (var file : existingFiles) {
			deleteWithParents(file);
		}
	}
}
