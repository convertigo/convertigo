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

package com.twinsoft.convertigo.engine.admin.services.mobiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.text.StringEscapeUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.MobileApplication.FlashUpdateBuildMode;
import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.MinificationManager.ResourceBundle;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.util.ZipUtils;

public class MobileResourceHelper {
	enum Keys {
		project,
		platform,
		uuid,
		flashUpdateEnabled,
		requireUserConfirmation;
		
		String value(HttpServletRequest request) {
			return request.getParameter(name());
		}
	};
	
	final Pattern pScript = Pattern.compile("(?:(?:<script .*?src)|(?:<link .*?href))=\"(.*?)\"");
	
	public static final IOFileFilter defaultFilter = new IOFileFilter() {
		
		public boolean accept(File file) {
			String filename = file.getName();
			return !filename.startsWith(".") && !filename.equalsIgnoreCase("thumbs.db");
		}

		public boolean accept(File file, String path) {
			return accept(new File(file, path));
		}
		
	};
	
	private final Project project;
	final MobileApplication mobileApplication;
	final MobilePlatform mobilePlatform;
	private final File projectDir;
	private final List<File> mobileDir;
	final File destDir;
	private File currentMobileDir;
	private String endpoint;
	
	MobileResourceHelper(HttpServletRequest request, String buildFolder) throws EngineException, ServiceException {		
		this(request, buildFolder, Keys.project.value(request), Keys.platform.value(request));
	}
	
	public MobileResourceHelper(HttpServletRequest request, String buildFolder, String project, String platform) throws EngineException, ServiceException {		
		this(getMobilePlatform(project, platform), "_private/" + buildFolder + "_" + platform);
		endpoint = mobileApplication.getComputedEndpoint(request);
	}
	
	public MobileResourceHelper(MobilePlatform mobilePlatform, String destSubDir) throws EngineException, ServiceException {
		this.mobilePlatform = mobilePlatform;
		mobileApplication = mobilePlatform.getParent();
		project = mobileApplication.getProject();
		
		endpoint = mobileApplication.getComputedEndpoint();
		
		projectDir = new File(project.getDirPath());
		File destDir = new File(destSubDir);
		this.destDir = destDir.exists() ? destDir : new File(projectDir, destSubDir);
		mobileDir = Arrays.asList(mobileApplication.getResourceFolder(), mobilePlatform.getResourceFolder());
	}
	
	private void prepareFiles() throws ServiceException {
		prepareFiles(defaultFilter);
	}
	
	private void prepareFiles(FileFilter fileFilterForCopy) throws ServiceException {
		try {
			String endPoint = this.endpoint + "/projects/" + project.getName();
			String applicationID = mobileApplication.getComputedApplicationId();
			
			if (!endPoint.endsWith("/")) {
				endPoint += "/";
			}
			
			// Check forbidden characters in application ID (a-zA-Z0-9.-)
			if (!Pattern.matches("[\\.\\w]*", applicationID)) {
				throw new ServiceException("The application ID is not valid: '" + applicationID
						+ "'.\nThe only valid characters are upper and lower letters (A-Z, a-z), "
						+ "digits (0-9), a period (.) and a hyphen (-).");
			}

			// Delete no existing files
			FileUtils.deleteQuietly(destDir);
			for (File directory: mobileDir) {
				currentMobileDir = directory;
				if (directory.exists()) {
					FileUtils.copyDirectory(directory, destDir, fileFilterForCopy, true);
				}
			}
			currentMobileDir = null;

			List<File> filesToDelete = new LinkedList<File>();

			for (File htmlFile : FileUtils.listFiles(destDir, new String[] {"html"}, true)) {
				String htmlContent = FileUtils.readFileToString(htmlFile, StandardCharsets.UTF_8);
				StringBuffer sbIndexHtml = new StringBuffer();
				BufferedReader br = new BufferedReader(new StringReader(htmlContent));
				String includeChar = null;
				String includeBuf = null;
				for (String line = br.readLine(); line != null; line = br.readLine()) {
					if (!line.contains("<!--")) {
						if (includeChar == null && line.contains("<base ")) {
							line = "  <base href=\"./\">";
						} else if (includeChar == null && line.contains("\"../../../../")) {
							String file = line.replaceFirst(".*\"\\.\\./\\.\\./\\.\\./\\.\\./(.*?)\".*", "$1");

							if (file.endsWith("c8o.cordova.js")) {
								file = file.replace("c8o.cordova.js", "c8o.cordova.device.js");
							}

							File inFile = new File(Engine.WEBAPP_PATH + "/" + file);

							try (InputStream is = inFile.exists() ? new FileInputStream(inFile) : getClass().getResourceAsStream("res/" + inFile.getName())) {
								if (is != null) {
									file = file.replace("scripts/", "js/");
									File outFile = new File(destDir, file);
									if (!outFile.exists()) {
										FileUtils.copyInputStreamToFile(is, outFile);
										outFile.setLastModified(1);
									}
									line = line.replaceFirst("\"\\.\\./\\.\\./\\.\\./\\.\\./.*?\"", "\"" + file + "\"");

									if (inFile.exists()) {
										handleJQMcssFolder(inFile, outFile);
									}

									if (file.matches(".*/jquery\\.mobilelib\\..*?js")) {
										String sJs = FileUtils.readFileToString(outFile, StandardCharsets.UTF_8);
										sJs = sJs.replaceAll(Pattern.quote("url : \"../../\""), "url : \"" + endPoint + "\"");
										writeStringToFile(outFile, sJs);
									} else if (file.matches(".*/c8o\\.core\\..*?js")) {
										String sJs = FileUtils.readFileToString(outFile, StandardCharsets.UTF_8);
										sJs = sJs.replaceAll(Pattern.quote("endpoint_url: \"\""), "endpoint_url: \"" + endPoint + "\"");
										writeStringToFile(outFile, sJs);
									}

									if (file.matches(".*/flashupdate_.*?\\.css")) {
										if (inFile.exists()) {
											FileUtils.copyDirectory(new File(inFile.getParentFile(), "flashupdate_fonts"), new File(outFile.getParentFile(), "flashupdate_fonts"), defaultFilter, true);
											FileUtils.copyDirectory(new File(inFile.getParentFile(), "flashupdate_images"), new File(outFile.getParentFile(), "flashupdate_images"), defaultFilter, true);
										} else {
											for (String path: Arrays.asList(
													"flashupdate_fonts/fontfaceROBOTO.css",
													"flashupdate_fonts/GoogleAndroidLicense.txt",
													"flashupdate_fonts/Roboto-Bold-webfont.eot",
													"flashupdate_fonts/Roboto-Bold-webfont.svg",
													"flashupdate_fonts/Roboto-Bold-webfont.ttf",
													"flashupdate_fonts/Roboto-Bold-webfont.woff",
													"flashupdate_fonts/Roboto-Regular-webfont.eot",
													"flashupdate_fonts/Roboto-Regular-webfont.svg",
													"flashupdate_fonts/Roboto-Regular-webfont.ttf",
													"flashupdate_fonts/Roboto-Regular-webfont.woff",
													"flashupdate_images/bg_ios.jpg"
												)) {
												File outRes = new File(outFile.getParentFile(), path);
												FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("res/" + path), outRes);
												outRes.setLastModified(0);
											}
										}
									}
								}
							}
						} else {
							/** Handle multilines <script> and <link> urls for the resourceCompressorManager */
							if (includeChar == null) {
								Matcher mStart = Pattern.compile("(?:(?:<script .*?src)|(?:<link .*?href))\\s*=\\s*(\"|')(.*?)(\\1|$)").matcher(line);

								if (mStart.find()) {
									String end = mStart.group(3);
									if (end.length() == 0) {
										includeChar = mStart.group(1);
									}
									includeBuf = mStart.group(2);
								} else {
									includeBuf = null;
								}
							} else {
								int index = line.indexOf(includeChar);
								if (index != -1) {
									includeBuf += line.substring(0, index);
									includeChar = null;
								} else {
									includeBuf += line;
								}
							}

							if (includeChar == null && includeBuf != null) {
								String uri = includeBuf;
								if (htmlFile.getAbsolutePath().startsWith(projectDir.getAbsolutePath())) {
									uri = htmlFile.getParentFile().getAbsolutePath().substring(projectDir.getParentFile().getAbsolutePath().length() + 1) + "/" + uri;
								}
								ResourceBundle resourceBundle = Engine.theApp.minificationManager != null ? Engine.theApp.minificationManager.process(uri) : null;
								if (resourceBundle != null) {
									synchronized (resourceBundle) {
										String prepend = "";
										for (File file: resourceBundle.getFiles()) {
											String filename = file.getName();
											if (filename.matches("c8o\\.core\\..*?js")) {
												prepend += "C8O.vars.endpoint_url=\"" + endPoint + "\";";
											} else if (handleJQMcssFolder(file, resourceBundle.getVirtualFile())) {
											}
										}
										if (prepend.isEmpty()) {
											resourceBundle.writeFile();
										} else {
											resourceBundle.writeFile(prepend);
										}
										for (File file: resourceBundle.getFiles()) {
											if (file.getPath().indexOf(projectDir.getPath()) == 0) {
												filesToDelete.add(file);
											}
										}
									}
								}
							}
						}
					}

					sbIndexHtml.append(line + "\n");
				}

				htmlContent = sbIndexHtml.toString();

				writeStringToFile(htmlFile, htmlContent);
			}

			for (File file: filesToDelete) {
				FileUtils.deleteQuietly(file);
			}

			long latestFile = 0;
			File lastFile = null;
			for (File file: FileUtils.listFiles(destDir, TrueFileFilter.INSTANCE, FileFilterUtils.notFileFilter(FileFilterUtils.nameFileFilter("res")))) {
				if (!file.getName().equals("config.xml")
						&& !file.getName().equals("env.json")) {
					long curFile = file.lastModified();
					if (latestFile < curFile) {
						latestFile = curFile;
						lastFile = file;
					}
				}
			}
			Engine.logEngine.info("(MobileResourceHelper) prepareFiles, lastestFile is '" + latestFile + "' for " + lastFile);
			destDir.setLastModified(latestFile);
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new ServiceException(e.getClass().getName(), e);
		}
	}
	
	public void listFiles(JSONObject response) throws JSONException, IOException {
		File canonicalDir = destDir.getCanonicalFile();
		int uriDirectoryLength = canonicalDir.toURI().toString().length();
		JSONArray jArray = new JSONArray();
		
		for (File f : FileUtils.listFiles(canonicalDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {
			File canonnicalF = f.getCanonicalFile();
			String uri = URLDecoder.decode(canonnicalF.toURI().toString().substring(uriDirectoryLength), StandardCharsets.UTF_8);
			if (!uri.startsWith("res/") && !uri.equals("config.xml")) {
				JSONObject jObj = new JSONObject();
				jObj.put("uri", uri);
				jObj.put("date", canonnicalF.lastModified());
				jObj.put("size", canonnicalF.length());
				jArray.put(jObj);
			}
		}
		response.put("files", jArray);
		response.put("date", destDir.lastModified());
	}
	
	private void writeStringToFile(File file, String content) throws IOException {
		long lastModified = file.exists() ? file.lastModified() : System.currentTimeMillis();
		FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
		file.setLastModified(lastModified);
	}
	
	public File preparePackage() throws Exception {
		
		FlashUpdateBuildMode buildMode = mobileApplication.getBuildMode();
		
		String finalApplicationName = mobileApplication.getComputedApplicationName();
		
		JSONObject json = new JSONObject();
		
		if (buildMode == FlashUpdateBuildMode.full) {
			fixMobileBuilderTimes();
			prepareFiles();
		} else if (buildMode == FlashUpdateBuildMode.light) {
			fixMobileBuilderTimes();
			prepareFiles(new FileFilter() {
				
				public boolean accept(File pathname) {
					try {
						File dir;
						boolean ok = MobileResourceHelper.defaultFilter.accept(pathname) && (
							new File(currentMobileDir, "index.html").equals(pathname) ||
							new File(currentMobileDir, "config.xml").equals(pathname) ||
							new File(currentMobileDir, "icon.png").equals(pathname) ||
							(dir = new File(currentMobileDir, "res")).equals(pathname) ||
							(dir.exists() && FileUtils.directoryContains(dir, pathname)) ||
							(dir = new File(currentMobileDir, "flashupdate")).equals(pathname) ||
							(dir.exists() && FileUtils.directoryContains(dir, pathname)));
						return ok;
					} catch(Exception e) {
						return false;
					}
				}
				
			});
			json.put("lightBuild", true);
		} else {
			throw new ServiceException("Unknow build mode: " + buildMode);
		}
		
		var endpointMatcher = Pattern.compile("(.*?)://([^:/]+).*").matcher(endpoint);
		if (!endpointMatcher.matches()) {
			throw new ServiceException("Invalid endpoint: " + endpoint);
		}
		var endpointScheme = endpointMatcher.group(1);
		var endpointHostname = endpointMatcher.group(2);
		
		// Update config.xml
		File configFile = new File(destDir, "config.xml");
		String configText = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
		long revision = destDir.lastModified();
		configText = configText
				.replace("$(ApplicationID)$", mobileApplication.getComputedApplicationId())
				.replace("$(ApplicationVersion)$", mobileApplication.getComputedApplicationVersion())
				.replace("$(ApplicationName)$", StringEscapeUtils.escapeXml11(finalApplicationName))
				.replace("$(ApplicationDescription)$", StringEscapeUtils.escapeXml11(mobileApplication.getApplicationDescription()))
				.replace("$(ApplicationAuthorName)$", StringEscapeUtils.escapeXml11(mobileApplication.getApplicationAuthorName()))
				.replace("$(ApplicationAuthorEmail)$", mobileApplication.getApplicationAuthorEmail())
				.replace("$(ApplicationAuthorWebsite)$", mobileApplication.getApplicationAuthorSite())
				.replace("$(ApplicationBackgroundColor)$", mobileApplication.getApplicationBgColor())
				.replace("$(ApplicationThemeColor)$", mobileApplication.getApplicationThemeColor())
				.replace("$(PlatformName)$", mobilePlatform.getName())
				.replace("$(PlatformType)$", mobilePlatform.getType())
				.replace("$(CordovaPlatform)$", mobilePlatform.getCordovaPlatform())
				.replace("$(EndpointScheme)$", endpointScheme)
				.replace("$(EndpointHostname)$", endpointHostname);

		File pluginsFile = new File(destDir, "plugins.txt");
		if (pluginsFile.exists()) {
			String mandatoryPlugins = FileUtils.readFileToString(pluginsFile, StandardCharsets.UTF_8);
			if (!mandatoryPlugins.isEmpty()) {
				mandatoryPlugins = "<!-- Application mandatory plugins -->"+ System.lineSeparator() + mandatoryPlugins;
				configText = configText.replace("<!-- Application mandatory plugins -->", mandatoryPlugins);
			}
			FileUtils.deleteQuietly(pluginsFile);
		}
		
		FileUtils.write(configFile, configText, StandardCharsets.UTF_8);
		configFile.setLastModified(revision);
		destDir.setLastModified(revision);
		
		listFiles(json);
		write("files.json", json.toString());
		
		String remoteBase = endpoint + "/projects/" + project.getName() + "/_private/mobile/flashupdate_" + this.mobilePlatform.getName();
		
		String env = read("env.json");
		try {
			json = new JSONObject(env);
		} catch (Exception e) {
			json = new JSONObject();
		}
		json.put("applicationAuthorName", mobileApplication.getApplicationAuthorName());
		json.put("applicationAuthorEmail", mobileApplication.getApplicationAuthorEmail());
		json.put("applicationAuthorWebsite", mobileApplication.getApplicationAuthorSite());
		json.put("applicationDescription", mobileApplication.getApplicationDescription());
		json.put("applicationId", mobileApplication.getComputedApplicationId());
		json.put("applicationName", finalApplicationName);
		json.put("builtRevision", revision);
		json.put("builtVersion", mobileApplication.getComputedApplicationVersion());
		json.put("currentRevision", revision);
		json.put("currentVersion", mobileApplication.getComputedApplicationVersion());
		json.put("endPoint", endpoint);
		json.put("platform", mobilePlatform.getCordovaPlatform());
		json.put("platformName", mobilePlatform.getName());
		json.put("projectName", project.getName());
		json.put("remoteBase", remoteBase);
		json.put("timeout", mobileApplication.getFlashUpdateTimeout());
		json.put("splashRemoveMode", mobileApplication.getSplashRemoveMode().name());
		
		write("env.json", json.toString(4));
		
		destDir.setLastModified(revision);
		
		return destDir;
	}
	
	File makeZipPackage() throws Exception {
		preparePackage();

		// Build the ZIP file for the mobile device
		File mobileArchiveFile = new File(destDir.getParentFile(), project.getName() + "_" + mobilePlatform.getName() + "_SourcePackage.zip");
		ZipUtils.makeZip(mobileArchiveFile.getPath(), destDir.getPath(), null);
		
		return mobileArchiveFile;
	}

	void prepareFilesForFlashupdate() throws ServiceException {
		boolean changed = false;
		final File lastEndpoint = new File(destDir, ".endpoint");
		fixMobileBuilderTimes();
		if (Engine.isStudioMode() && destDir.exists()) {
			try {
				for (File directory: mobileDir) {
					FileUtils.listFiles(directory, new IOFileFilter() {

						public boolean accept(File file) {
							if (MobileResourceHelper.defaultFilter.accept(file)) {
								if (FileUtils.isFileNewer(file, destDir)) {
									Engine.logEngine.info("(MobileResourceHelper) prepareFilesForFlashupdate, '" + file.lastModified() + "' newer than '" + destDir.lastModified() + "' for: " + file);
									throw new RuntimeException();
								}
								return true;
							} else {
								return false;
							}
						}

						public boolean accept(File file, String path) {
							return accept(new File(file, path));
						}
						
					}, MobileResourceHelper.defaultFilter);
				}
				
				changed = !endpoint.equals(FileUtils.readFileToString(lastEndpoint, StandardCharsets.UTF_8));
			} catch (Exception e) {
				changed = true;
			}
		}

		if (!destDir.exists() || changed) {
			prepareFiles(new FileFilter() {
				
				public boolean accept(File pathname) {
					boolean ok = MobileResourceHelper.defaultFilter.accept(pathname) &&
						! new File(currentMobileDir, "plugins.txt").equals(pathname) &&
						! new File(currentMobileDir, "config.xml").equals(pathname) &&
						! new File(currentMobileDir, "res").equals(pathname) &&
						! lastEndpoint.equals(pathname);
					return ok;
				}
				
			});
			
			try {
				write(lastEndpoint, endpoint);
			} catch (IOException e) {
				throw new ServiceException("Failed to write last endpoint", e);
			}
		}
	}

	public String getRevision() {
		return Long.toString(destDir.lastModified());
	}
	
	private static boolean handleJQMcssFolder(File inFile, File outFile) throws IOException {
		if (inFile.getName().matches("jquery\\.mobile\\.(?:min\\.)?css")) {
			File inImages = new File(inFile.getParentFile(), "images");
			File outImages = new File(outFile.getParentFile(), "images");
			FileUtils.copyDirectory(inImages, outImages, defaultFilter, true);
			return true;
		}
		return false;
	}
	
	private static MobilePlatform getMobilePlatform(String projectName, String platform) throws ServiceException, EngineException {
		if (!Engine.theApp.databaseObjectsManager.existsProject(projectName)) {
			throw new ServiceException("Unable to get resources of the application '" + projectName
					+ "'; reason: the project does not exist");
		}
		
		Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
		
		MobileApplication mobileApplication = project.getMobileApplication();
		
		if (mobileApplication == null) {
			throw new ServiceException("The application " + project.getName() + " doesn't contain a mobileApplication object.");
		}
		
		return mobileApplication.getMobilePlatformByName(platform);
	}
	
	private String read(String filename) {
		try {
			return FileUtils.readFileToString(new File(destDir, filename), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return null;
		}
	}
	
	private void write(String filename, String content) throws IOException {
		write(new File(destDir, filename), content);
	}
	
	private void write(File file, String content) throws IOException {
		long lastModified = destDir.lastModified();
		FileUtils.write(file, content, StandardCharsets.UTF_8);
		destDir.setLastModified(lastModified);
	}
	
	private void fixMobileBuilderTimes() {
		try {
			Path resourcePath = mobileApplication.getResourceFolder().toPath();
			if (!Files.exists(resourcePath.resolve("build")) && !Files.exists(resourcePath.resolve("main.js"))) {
				return;
			}
			MobileBuilder mb = mobileApplication.getProject().getMobileBuilder();
			if (mb != null) {
				mb.waitBuildFinished();
			}
			Path fuPath = Paths.get(project.getDirPath(), "Flashupdate");
			Files.createDirectories(fuPath);
			Path pathMD5 = fuPath.resolve("md5.json");
			JSONObject[] jsonMD5 = {null};
			if (Files.exists(pathMD5)) {
				try {
					jsonMD5[0] = new JSONObject(FileUtils.readFileToString(pathMD5.toFile(), StandardCharsets.UTF_8));
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			
			if (jsonMD5[0] == null) {
				jsonMD5[0] = new JSONObject();
			}
			
			long[] latest = {0};
			
			Files.walk(resourcePath).filter(p ->
				!p.endsWith("md5.json"))
			.forEach(p -> {
				if (Files.isDirectory(p)) {
					return;
				}
				File f = p.toFile();
				String key = resourcePath.relativize(p).toString().replace('\\', '/');
				try {
					JSONObject entryMD5 = jsonMD5[0].has(key) ? jsonMD5[0].getJSONObject(key) : null;
					if (entryMD5 == null || FileUtils.isFileNewer(f, entryMD5.getLong("ts"))) {
						try (FileInputStream fis = new FileInputStream(f)) {
							String md5 = DigestUtils.md5Hex(fis);
							String lastMD5 = entryMD5 == null ? null : entryMD5.getString("md5");
							if (md5.equals(lastMD5)) {
								Engine.logEngine.trace("(MobileResourceHelper) restore " + f.getName() + " " + md5 + " from " + f.lastModified() + " to " + entryMD5.getLong("ts"));
								f.setLastModified(entryMD5.getLong("ts"));
							} else {
								Engine.logEngine.trace("(MobileResourceHelper) changed " + f.getName() + " " + md5 + " != " + lastMD5 + " from " + (entryMD5 == null ? null : entryMD5.getLong("ts")) + " to "+ f.lastModified());
								entryMD5 = new JSONObject();
								entryMD5.put("md5", md5);
								entryMD5.put("ts", f.lastModified());
								jsonMD5[0].put(key, entryMD5);
							}
						}
					}
				} catch (Exception e) {
					Engine.logEngine.debug("(MobileResourceHelper) fixMobileBuilderTimes failed to handle '" + f + "' : " + e, e);
				}
				if (!f.getName().equals("service-worker.js")) {
					latest[0] = Math.max(latest[0], f.lastModified());
				}
			});
			
			if (latest[0] > 0) {
				Files.walk(resourcePath).forEach(p -> {
					File f = p.toFile();
					if (f.lastModified() > latest[0]) {
						f.setLastModified(latest[0]);
					}
				});
			}
			
			FileUtils.write(pathMD5.toFile(), jsonMD5[0].toString(2), StandardCharsets.UTF_8);
		} catch (Exception e) {
			Engine.logEngine.debug("(MobileResourceHelper) fixMobileBuilderTimes failed : " + e, e);
		}
	}
}
