package com.twinsoft.convertigo.engine.admin.services.mobiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
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
	
	private static final Pattern alphaNumPattern = Pattern.compile("[\\.\\w]*");
	Pattern pScript = Pattern.compile("(?:(?:<script .*?src)|(?:<link .*?href))=\"(.*?)\"");
	
	public static final IOFileFilter defaultFilter = new IOFileFilter() {
		
		public boolean accept(File file) {
			String filename = file.getName();
			return !filename.startsWith(".") && !filename.equalsIgnoreCase("thumbs.db");
		}

		public boolean accept(File file, String path) {
			return accept(new File(file, path));
		}
		
	};
	
	final Project project;
	final MobileApplication mobileApplication;
	final MobilePlatform mobilePlatform;
	final File projectDir;
	final List<File> mobileDir;
	final File destDir;
	private File currentMobileDir;
	private String endpoint;
	
	public MobileResourceHelper(HttpServletRequest request, String buildFolder) throws EngineException, ServiceException {		
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
		
		endpoint = mobileApplication.getEndpoint();
		
		projectDir = new File(project.getDirPath());
		destDir = new File(projectDir, destSubDir);
		mobileDir = Arrays.asList(mobileApplication.getResourceFolder(), mobilePlatform.getResourceFolder());
	}
	
	public void prepareFiles() throws ServiceException {
		prepareFiles(defaultFilter);
	}
	
	public void prepareFiles(FileFilter fileFilterForCopy) throws ServiceException {
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
				FileUtils.copyDirectory(directory, destDir, fileFilterForCopy, true);
			}
			currentMobileDir = null;

			File serverJsFile = new File(destDir, "sources/server.js");
			
			if (serverJsFile.exists()) {
				// Sencha 1 case
				
				File indexHtmlFile = new File(destDir, "index.html");
				String sIndexHtml = FileUtils.readFileToString(indexHtmlFile);
				
				String sServerJsHtml = FileUtils.readFileToString(serverJsFile);
				sServerJsHtml = sServerJsHtml.replaceAll("/\\* DO NOT REMOVE THIS LINE endpoint \\: '' \\*/", "endpoint : '" + endPoint + "'");
				writeStringToFile(serverJsFile, sServerJsHtml);
				
				// Update sencha script reference (to non debug version)
				
				sIndexHtml = sIndexHtml.replaceAll("js/senchatouchdebugwcomments\\.js", "js/senchatouch.js");
				// "js/mobilelib.js" "../../../../scripts/mobilelib.js"
				File debugSenchaJsLibFile = new File(destDir, "js/senchatouchdebugwcomments.js");
				debugSenchaJsLibFile.delete();
				
				// Update mobilelib script reference and copy from commom scripts if needed
				
				File destFile = new File(destDir, "js/mobilelib.js");
				if (!destFile.exists()) {
					String origin = "/scripts/mobilelib.js";
					FileUtils.copyFile(new File(Engine.WEBAPP_PATH + origin), destFile, true);
					sIndexHtml = sIndexHtml.replaceAll(Pattern.quote("../../../.." + origin), "js/mobilelib.js");
				}
				
				writeStringToFile(indexHtmlFile, sIndexHtml);
			} else {
				// jQuery Mobile case
				
				List<File> filesToDelete = new LinkedList<File>();
				
				for (File htmlFile : FileUtils.listFiles(destDir, new String[] {"html"}, true)) {
					String htmlContent = FileUtils.readFileToString(htmlFile);
					StringBuffer sbIndexHtml = new StringBuffer();
					BufferedReader br = new BufferedReader(new StringReader(htmlContent));
					String includeChar = null;
					String includeBuf = null;
					for(String line = br.readLine(); line != null; line = br.readLine()) {
						if (!line.contains("<!--")) {
							if (includeChar == null && line.contains("\"../../../../")) {
								String file = line.replaceFirst(".*\"\\.\\./\\.\\./\\.\\./\\.\\./(.*?)\".*", "$1");
								
								if (file.endsWith("c8o.cordova.js")) {
									file = file.replace("c8o.cordova.js", "c8o.cordova.device.js");
								}
								
								File inFile = new File(Engine.WEBAPP_PATH + "/" + file);
								
								if (inFile.exists()) {
									file = file.replace("scripts/", "js/");
									File outFile = new File(destDir, file);
									outFile.getParentFile().mkdirs();
									FileUtils.copyFile(inFile, outFile, true);
									line = line.replaceFirst("\"\\.\\./\\.\\./\\.\\./\\.\\./.*?\"", "\"" + file + "\"");
									
									handleJQMcssFolder(inFile, outFile);
									
									if (file.matches(".*/jquery\\.mobilelib\\..*?js")) {
										String sJs = FileUtils.readFileToString(outFile);
										sJs = sJs.replaceAll(Pattern.quote("url : \"../../\""), "url : \"" + endPoint + "\"");
										writeStringToFile(outFile, sJs);
									} else if (file.matches(".*/c8o\\.core\\..*?js")) {
										String sJs = FileUtils.readFileToString(outFile);
										sJs = sJs.replaceAll(Pattern.quote("endpoint_url: \"\""), "endpoint_url: \"" + endPoint + "\"");
										writeStringToFile(outFile, sJs);
//									} else if (file.matches(".*/c8o\\.fullsync\\..*?js")) {
//										String fsDatabase = mobileApplication.getFsDefaultDatabase();
//										String fsDesignDocument = mobileApplication.getFsDefaultDesignDocument();
//										
//										if (StringUtils.isNotEmpty(fsDatabase) || StringUtils.isNotEmpty(fsDesignDocument)) {
//											String sJs = FileUtils.readFileToString(outFile);
//											if (StringUtils.isNotEmpty(fsDatabase)) {
//												sJs = sJs.replaceAll(Pattern.quote("fs_default_db: null"), "fs_default_db: \"" + fsDatabase + "\"");
//											}
//											if (StringUtils.isNotEmpty(fsDesignDocument)) {
//												sJs = sJs.replaceAll(Pattern.quote("fs_default_design: null"), "fs_default_design: \"" + fsDesignDocument + "\"");
//											}
//											writeStringToFile(outFile, sJs);
//										}
									}
									
									if (file.matches(".*/flashupdate_.*?\\.css")) {
										FileUtils.copyDirectory(new File(inFile.getParentFile(), "flashupdate_fonts"), new File(outFile.getParentFile(), "flashupdate_fonts"), defaultFilter, true);
										FileUtils.copyDirectory(new File(inFile.getParentFile(), "flashupdate_images"), new File(outFile.getParentFile(), "flashupdate_images"), defaultFilter, true);
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
									uri = htmlFile.getParent().substring(projectDir.getParent().length() + 1) + "/" + uri;
									ResourceBundle resourceBundle = Engine.theApp.minificationManager.process(uri);
									if (resourceBundle != null) {
										synchronized (resourceBundle) {
											String prepend = "";
											for (File file: resourceBundle.getFiles()) {
												String filename = file.getName();
												if (filename.matches("c8o\\.core\\..*?js")) {
													prepend += "C8O.vars.endpoint_url=\"" + endPoint + "\";";
//												} else if (filename.matches("c8o\\.fullsync\\..*?js")) {
//													String fsDatabase = mobileApplication.getFsDefaultDatabase();
//													String fsDesignDocument = mobileApplication.getFsDefaultDesignDocument();
//													
//													if (StringUtils.isNotEmpty(fsDatabase) || StringUtils.isNotEmpty(fsDesignDocument)) {
//														if (StringUtils.isNotEmpty(fsDatabase)) {
//															prepend += "if (C8O.vars.fs_default_db == null) C8O.vars.fs_default_db=\"" + fsDatabase + "\";";
//														}
//														if (StringUtils.isNotEmpty(fsDesignDocument)) {
//															prepend += "if (C8O.vars.fs_default_design == null) C8O.vars.fs_default_design=\"" + fsDesignDocument + "\";";
//														}
//													}
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
			}
			
			long latestFile = 0;
			File lastFile = null;
			for (File file: FileUtils.listFiles(destDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {
				long curFile = file.lastModified();
				if (latestFile < curFile) {
					latestFile = curFile;
					lastFile = file;
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
			JSONObject jObj = new JSONObject();
			jObj.put("uri", URLDecoder.decode(canonnicalF.toURI().toString().substring(uriDirectoryLength), "UTF-8"));
			jObj.put("date", canonnicalF.lastModified());
			jObj.put("size", canonnicalF.length());
			jArray.put(jObj);
		}
		response.put("files", jArray);
		response.put("date", destDir.lastModified());
	}
	
	private void writeStringToFile(File file, String content) throws IOException {
		long lastModified = file.exists() ? file.lastModified() : System.currentTimeMillis();
		FileUtils.writeStringToFile(file, content);
		file.setLastModified(lastModified);
	}
	
	public File preparePackage() throws Exception {
		
		FlashUpdateBuildMode buildMode = mobileApplication.getBuildMode();		
		
		String finalApplicationName = mobileApplication.getComputedEscapededApplicationName(mobilePlatform);
		
		JSONObject json = new JSONObject();
		
		if (buildMode == FlashUpdateBuildMode.full) {
			prepareFiles();
		} else if (buildMode == FlashUpdateBuildMode.light) {
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
		
		listFiles(json);
		write("files.json", json.toString());
		
		String remoteBase = endpoint + "/projects/" + project.getName() + "/_private/mobile/flashupdate_" + this.mobilePlatform.getName();
				
		json = new JSONObject();
		json.put("applicationAuthorName", mobileApplication.getApplicationAuthorName());
		json.put("applicationAuthorEmail", mobileApplication.getApplicationAuthorEmail());
		json.put("applicationAuthorWebsite", mobileApplication.getApplicationAuthorSite());
		json.put("applicationDescription", mobileApplication.getApplicationDescription());
		json.put("applicationId", mobileApplication.getComputedApplicationId());
		json.put("applicationName", finalApplicationName);
		json.put("builtRevision", destDir.lastModified());
		json.put("builtVersion", mobileApplication.getComputedApplicationVersion());
		json.put("currentRevision", destDir.lastModified());
		json.put("currentVersion", mobileApplication.getComputedApplicationVersion());
		json.put("endPoint", endpoint);
		json.put("platformName", mobilePlatform.getName());
		json.put("projectName", project.getName());
		json.put("remoteBase", remoteBase);
		json.put("timeout", mobileApplication.getFlashUpdateTimeout());
		
		write("env.json", json.toString());
		
		File configFile = new File(destDir, "config.xml");
		
		// Update config.xml
		String configText = FileUtils.readFileToString(configFile, "UTF-8");
		
		configText = configText
				.replace("$(ApplicationID)$", mobileApplication.getComputedApplicationId())
				.replace("$(ApplicationVersion)$", mobileApplication.getComputedApplicationVersion())
				.replace("$(ApplicationName)$", finalApplicationName)
				.replace("$(ApplicationDescription)$", mobileApplication.getApplicationDescription())
				.replace("$(ApplicationAuthorName)$", mobileApplication.getApplicationAuthorName())
				.replace("$(ApplicationAuthorEmail)$", mobileApplication.getApplicationAuthorEmail())
				.replace("$(ApplicationAuthorWebsite)$", mobileApplication.getApplicationAuthorSite())
				.replace("$(PlatformName)$", mobilePlatform.getName())
				.replace("$(PlatformType)$", mobilePlatform.getType())
				.replace("$(CordovaPlatform)$", mobilePlatform.getCordovaPlatform());

		FileUtils.write(configFile, configText, "UTF-8");
		
		return destDir;
	}
	
	public File makeZipPackage() throws Exception {
		preparePackage();

		// Build the ZIP file for the mobile device
		File mobileArchiveFile = new File(destDir.getParentFile(), project.getName() + ".zip");
		ZipUtils.makeZip(mobileArchiveFile.getPath(), destDir.getPath(), null);
		
		return mobileArchiveFile;
	}

	public void prepareFilesForFlashupdate() throws ServiceException {
		boolean changed = false;
		final File lastEndpoint = new File(destDir, ".endpoint");
		
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
				
				changed = !endpoint.equals(FileUtils.readFileToString(lastEndpoint, "UTF-8"));
			} catch (Exception e) {
				changed = true;
			}
		}

		if (!destDir.exists() || changed) {
			prepareFiles(new FileFilter() {
				
				public boolean accept(File pathname) {
					boolean ok = MobileResourceHelper.defaultFilter.accept(pathname) &&
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
	
	private void write(String filename, String content) throws IOException {
		write(new File(destDir, filename), content);
	}
	
	private void write(File file, String content) throws IOException {
		long lastModified = destDir.lastModified();
		FileUtils.write(file, content, "UTF-8");
		destDir.setLastModified(lastModified);
	}
}
