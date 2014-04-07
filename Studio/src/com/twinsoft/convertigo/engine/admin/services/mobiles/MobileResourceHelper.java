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
import com.twinsoft.convertigo.beans.mobileplatforms.BlackBerry;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ResourceCompressorManager.ResourceBundle;
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
			return !filename.equals(".DS_Store") && !filename.equalsIgnoreCase("thumbs.db")
					&& !filename.equals(".svn");
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
		this(Keys.project.value(request), Keys.platform.value(request), "_private/" + buildFolder + "_" + Keys.platform.value(request));
		endpoint = mobileApplication.getComputedEndpoint(request);
	}
	
	private MobileResourceHelper(String application, String platform, String destSubDir) throws EngineException, ServiceException {
		if (!Engine.theApp.databaseObjectsManager.existsProject(application)) {
			throw new ServiceException("Unable to get resources of the application '" + application
					+ "'; reason: the project does not exist");
		}
		
		project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(application);
		
		mobileApplication = project.getMobileApplication();
		endpoint = mobileApplication.getEndpoint();
		
		if (mobileApplication == null) {
			throw new ServiceException("The application " + project.getName() + " doesn't contain a mobileApplication object.");
		}
		
		mobilePlatform = mobileApplication.getMobilePlatformByName(platform);
		
		projectDir = new File(Engine.PROJECTS_PATH + "/" + application);
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

			
			if (mobilePlatform instanceof BlackBerry) {
				for (File file : FileUtils.listFilesAndDirs(destDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {
					if (!alphaNumPattern.matcher(file.getName()).matches()) {
						throw new ServiceException("The file or directory '" + file.getAbsolutePath()
								+ "' contains non alpha-numeric characters. "
								+ "BlackBerry build does not allow non alpha-numeric characters in file/directory names. "
								+ "You must rename it with alpha-numeric characters only.");
					}
				}
			}
			
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
				
				for (File htmlFile :FileUtils.listFiles(destDir, new String[] {"html"}, true)) {
					String htmlContent = FileUtils.readFileToString(htmlFile);
					StringBuffer sbIndexHtml = new StringBuffer();
					BufferedReader br = new BufferedReader(new StringReader(htmlContent));
					String line = br.readLine();
					while (line != null) {
						if (!line.contains("<!--")) {
							if (line.contains("\"../../../../")) {
								String file = line.replaceFirst(".*\"\\.\\./\\.\\./\\.\\./\\.\\./(.*?)\".*", "$1");
								File inFile = new File(Engine.WEBAPP_PATH + "/" + file);
								
								if (inFile.exists()) {
									boolean needImages = file.matches(".*jquery\\.mobile\\..*?min\\.css");
									
									file = file.replace("scripts/", "js/");
									File outFile = new File(destDir, file);
									outFile.getParentFile().mkdirs();
									FileUtils.copyFile(inFile, outFile);
									line = line.replaceFirst("\"\\.\\./\\.\\./\\.\\./\\.\\./.*?\"", "\"" + file + "\"");
									
									if (needImages) {
										File inImages = new File(inFile.getParentFile(), "images");
										File outImages = new File(outFile.getParentFile(), "images");
										FileUtils.copyDirectory(inImages, outImages, defaultFilter, true);
									}
									
									if (file.matches(".*/jquery\\.mobilelib\\..*?js")) {
										String sJs = FileUtils.readFileToString(outFile);
										sJs = sJs.replaceAll(Pattern.quote("url : \"../../\""), "url : \"" + endPoint + "\"");
										writeStringToFile(outFile, sJs);
									} else if (file.matches(".*/c8o\\.core\\..*?js")) {
										serverJsFile = outFile;
										String sJs = FileUtils.readFileToString(serverJsFile);
										sJs = sJs.replaceAll(Pattern.quote("endpoint_url: \"\""), "endpoint_url: \"" + endPoint + "\"");
										FileUtils.writeStringToFile(serverJsFile, sJs);
									}
									
									if (file.matches(".*/flashupdate_.*?\\.css")) {
										FileUtils.copyDirectory(new File(inFile.getParentFile(), "flashupdate_fonts"), new File(outFile.getParentFile(), "flashupdate_fonts"), defaultFilter, true);
										FileUtils.copyDirectory(new File(inFile.getParentFile(), "flashupdate_images"), new File(outFile.getParentFile(), "flashupdate_images"), defaultFilter, true);
									}
								}
							} else {
								Matcher mScript = pScript.matcher(line);
								if (mScript.find()) {
									String uri = mScript.group(1);
									uri = htmlFile.getParent().substring(projectDir.getParent().length() + 1) + "/" + uri;
									ResourceBundle resourceBundle = Engine.theApp.resourceCompressorManager.process(uri);
									if (resourceBundle != null) {
										synchronized (resourceBundle) {
											line = line.replaceAll("(?:#|\\?).*?(?<!\\\\)(\")","$1");
											String prepend = null;
											for (File file : resourceBundle.getFiles()) {
												if (file.getName().matches("c8o\\.core\\..*?js")) {
													prepend = "C8O.vars.endpoint_url=\"" + endPoint + "\";";												
													break;
												}
											}
											if (prepend == null) {
												resourceBundle.writeFile();
											} else {
												resourceBundle.writeFile(prepend);
											}
											for (File file : resourceBundle.getFiles()) {
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
						line = br.readLine();
					}
					
					htmlContent = sbIndexHtml.toString();

					writeStringToFile(htmlFile, htmlContent);
				}
				
				for (File file : filesToDelete) {
					FileUtils.deleteQuietly(file);
				}
			}
			
			long latestFile = 0;
			for (File file : FileUtils.listFiles(destDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {
				long curFile = file.lastModified();
				if (latestFile < curFile) {
					latestFile = curFile;
				}
			}
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
	
	File getCurrentMobileDir() {
		return currentMobileDir;
	}
	
	public File makeZipPackage() throws Exception {
		
		FlashUpdateBuildMode buildMode = mobileApplication.getBuildModeEnum();		
		
		String finalApplicationName = mobileApplication.getComputedApplicationName();		
		
		JSONObject json = new JSONObject();
		
		if (buildMode == FlashUpdateBuildMode.full) {
			prepareFiles();
		} else if (buildMode == FlashUpdateBuildMode.light) {
			prepareFiles(new FileFilter() {
				
				public boolean accept(File pathname) {
					try {
						boolean ok = MobileResourceHelper.defaultFilter.accept(pathname) && (
							new File(currentMobileDir, "index.html").equals(pathname) ||
							new File(currentMobileDir, "config.xml").equals(pathname) ||
							new File(currentMobileDir, "icon.png").equals(pathname) ||
							new File(currentMobileDir, "flashupdate").equals(pathname) ||
							FileUtils.directoryContains(new File(currentMobileDir, "flashupdate"), pathname) ||
							new File(currentMobileDir, "res").equals(pathname) ||
							FileUtils.directoryContains(new File(currentMobileDir, "res"), pathname));
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
		FileUtils.write(new File(destDir, "files.json"), json.toString());
		
		json = new JSONObject();
		json.put("applicationId", mobileApplication.getComputedApplicationId());
		json.put("applicationName", finalApplicationName);
		json.put("projectName", project.getName());
		json.put("endPoint", endpoint);
		json.put("timeout", mobileApplication.getFlashUpdateTimeout());
		FileUtils.write(new File(destDir, "env.json"), json.toString());
		
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
				.replace("$(PlatformType)$", mobilePlatform.getType());

		FileUtils.write(configFile, configText, "UTF-8");

		// Build the ZIP file for the mobile device
		File mobileArchiveFile = new File(destDir.getParentFile(), project.getName() + ".zip");
		ZipUtils.makeZip(mobileArchiveFile.getPath(), destDir.getPath(), null);
		
		return mobileArchiveFile;
	}
}
