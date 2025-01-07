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

package com.twinsoft.convertigo.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.ComboEnum;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.util.HttpUtils;

public class MinificationManager implements AbstractManager {
	private static final File minificationCacheDirectory = new File(Engine.USER_WORKSPACE_PATH + "/minification");
	private static final Pattern requestPattern = Pattern.compile("(.*?/projects/|^)(((.*?)/.*?([^/]*?\\.(?:(js)|(css))))(?:\\?(.*)|$))");
	private static final Pattern tailCssUrl = Pattern.compile("([^?]*/).*?\\.css(?:\\?.*)?");
	
	// Sample of use:
	// <script src="js/all.js?{'minification':'strong', 'resources':['/jquery.min','/jquery.mobilelib','/ctf.core','custom','/jquery.mobile.min']}"></script>
	
	private enum RequestPart {
		fullRequest(0), beforeProject(1), fullFromProject(2), pathFromProject(3), projectName(4), fileName(5), jsCase(6), cssCase(7), query(8);
		
		int group;
		
		RequestPart(int group) {
			this.group = group;
		}
		
		String value(Matcher requestMatcher) {
			return requestMatcher.group(group);
		}
	}
	
	private enum GlobalOptions {
		minification, files, stats, filenames, version
	}
	
	private enum FileOptions {
		minification, file, encoding
	}
	
	private enum MinificationOptions implements ComboEnum {
		common(null),
		none("none (no minification)"),
		lines("lines (no minification + lines in comment)"),
		light("light (express minification)"),
		strong("strong (strong minification)");
		
		final String display;
		
		MinificationOptions(String display) {
			this.display = display;
		}

		@Override
		public String getDisplay() {
			return display;
		}

		@Override
		public String getValue() {
			return name();
		}
	}
	
	private enum ResourceType {
		js, css;
		
		static ResourceType get(Matcher requestMatcher) {
			if (RequestPart.jsCase.value(requestMatcher) != null) {
				return js;
			} else {
				return css;
			}
		}
	}
	
	private class ResourceEntry {
		private File file;
		private long lastChange = -1;
		private String encoding;
		private MinificationOptions minification;
		
		private ResourceEntry(File file, String encoding, MinificationOptions minification) {
			this.file = file;
			this.encoding = encoding;
			this.minification = minification;
		}
		
		private boolean isNewer() {
			return file.lastModified() > lastChange;
		}
		
		private void update() {
			lastChange = file.lastModified();
		}
		
		private File getFile() {
			return file;
		}
		
		private String getEncoding() {
			return encoding;
		}
		
		private MinificationOptions getMinification() {
			return minification;
		}
	}
	
	public class ResourceBundle {
		private List<ResourceEntry> resources;
		private File cacheFile;
		private ResourceType resourceType;
		private File virtualFile;
		private boolean filenames = true;
		
		private ResourceBundle(ResourceType resourceType, File virtualFile, String key) {
			this.resourceType= resourceType; 
			this.virtualFile = virtualFile;
			key = key.replaceFirst("(.*?)/_private/(?:flashupdate|mobile)/", "$1/DisplayObjects/mobile/");
			key = DigestUtils.md5Hex(key) + DigestUtils.sha1Hex(key) + "." + resourceType.name();
			cacheFile = new File(minificationCacheDirectory, key);
		}
		
		private void init(int size) {
			resources = new ArrayList<MinificationManager.ResourceEntry>(size);
		}
		
		private boolean isInit() {
			return resources != null;
		}
		
		private void add(ResourceEntry resourceEntry) {
			resources.add(resourceEntry);
		}
		
		private boolean checkNewer() {
			for (ResourceEntry resourceEntry : resources) {
				if (resourceEntry.isNewer()) {
					return true;
				}
			}
			return false;
		}
		
		public String getResult() throws IOException {
			String result = "";
			if (!cacheFile.exists() || checkNewer()) {
				StringWriter sources = new StringWriter();
				
				if (filenames) {
					sources.append("/*/!\\ C8O resource minification: ");
				
					for (ResourceEntry resourceEntry : resources) {
						sources.append(resourceEntry.getFile().getName() + " ");
					}
					
					sources.append("*/\n");
				}
				
				for (ResourceEntry resourceEntry : resources) {
					File file = resourceEntry.getFile();
					MinificationOptions minification = resourceEntry.getMinification();
					
					if (filenames) {
						sources.append("/*/!\\ <" + file.getName() + "> */\n");
					}
					if (minification == MinificationOptions.lines) {
						BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), resourceEntry.getEncoding()));
						int lineCpt = 1;
						
						for (String line = br.readLine(); line != null; line = br.readLine()) {
							sources.append(StringUtils.rightPad(line, 110, ' ') + String.format(" // %03d", lineCpt++));
							if (filenames) {
								if (lineCpt % 50 == 1) {
									sources.append(" [" + file.getName() + "]");
								}
							}
							sources.append('\n');
						}
						br.close();
					} else {
						String raw = FileUtils.readFileToString(file, resourceEntry.getEncoding());						
						sources.append(raw);
					}
					if (filenames) {
						sources.append("\n/*/!\\ </" + file.getName() + "> */\n");
					}
				}
				
				result = sources.toString();
				
				FileUtils.write(cacheFile, result, "utf-8");
				
				for (ResourceEntry resourceEntry : resources) {
					resourceEntry.update();
				}
			} else {
				Engine.logEngine.trace("(MinificationManager) Serve existing minified result from: " + cacheFile);
				result = FileUtils.readFileToString(cacheFile, "utf-8");
				cacheFile.setLastModified(System.currentTimeMillis());
			}
			return result;
		}
		
		public List<File> getFiles() {
			List<File> files = new ArrayList<File>(resources.size());
			for (ResourceEntry entry : resources) {
				files.add(entry.getFile());
			}
			return files;
		}
		
		public void writeFile() throws IOException {
			FileUtils.write(virtualFile, getResult(), "utf-8");
		}
		
		public void writeFile(String prepend) throws IOException {
			FileUtils.write(virtualFile, getResult() + "\n" + prepend, "utf-8");
		}
		
		public ResourceType getResourceType() {
			return resourceType;
		}
		
		public File getVirtualFile() {
			return virtualFile;
		}

		private File getCommonFolder(String version) {
			File commonFolder = MinificationManager.getCommonFolder(resourceType);
			File versionFolder;
			if (version.length() > 0 && (versionFolder = new File(commonFolder, version)).exists()) {
				return versionFolder;
			} else {
				return commonFolder;
			}
		}
		
		public void setFilenames(boolean filenames) {
			this.filenames = filenames;
		}
	}
	
	private Map<String, ResourceBundle> cache = new HashMap<String, MinificationManager.ResourceBundle>();
	
	public boolean check(HttpServletRequest request, HttpServletResponse response) {
		String requestURI = HttpUtils.originalRequestURI(request);
		String query = request.getQueryString();
		if (query != null) {
			try {
				requestURI += "?" + URLDecoder.decode(query, "utf-8");
			} catch (UnsupportedEncodingException e) {
				requestURI += "?" + query;
			}
		}
		ResourceBundle resourceBundle = process(requestURI);
		if (resourceBundle != null) {
			try {
				response.setContentType(resourceBundle.getResourceType() == ResourceType.js ? MimeType.Javascript.value() : MimeType.Css.value());
				response.setCharacterEncoding("utf-8");
				response.getWriter().write(resourceBundle.getResult());
			} catch (IOException e) {
				Engine.logEngine.error("(MinificationManager) Failed to write minified result", e);
			}
			return true;
		} else {			
			return false;
		}
	}
	
	public ResourceBundle process(String requestedFile) {
		Engine.logEngine.trace("(MinificationManager) Check request compatibility of '" + requestedFile + "'");
		Matcher requestMatcher = requestPattern.matcher(requestedFile);
		return requestMatcher == null ? null : process(requestMatcher);
	}
	
	private ResourceBundle process(Matcher requestMatcher) {
		try {
			if (requestMatcher.matches() && RequestPart.query.value(requestMatcher) != null) {
				String key = RequestPart.fullFromProject.value(requestMatcher);
				MinificationOptions minification = MinificationOptions.common;
				MinificationOptions commonMinification = MinificationOptions.none;
				String version = "";
				
				ResourceBundle resourceBundle;
				synchronized (cache) {
					resourceBundle = cache.get(key);
					if (resourceBundle == null) {
						String virtualFilePath = Engine.PROJECTS_PATH + "/" + RequestPart.pathFromProject.value(requestMatcher);
						virtualFilePath = Engine.resolveProjectPath(virtualFilePath);
						resourceBundle = new ResourceBundle(ResourceType.get(requestMatcher), new File(virtualFilePath), key);
						cache.put(key, resourceBundle);
						Engine.logEngine.debug("(MinificationManager) Create resourceBundle '" + key + "'");
					} else {
						Engine.logEngine.trace("(MinificationManager) Re-use resourceBundle '" + key + "'");
					}
				}
				synchronized (resourceBundle) {
					if (!resourceBundle.isInit()) {
						JSONArray files = null;
						JSONObject options = null;
						String query = RequestPart.query.value(requestMatcher);
						Engine.logEngine.trace("(MinificationManager) Parse JSON query '" + query + "'");
						try {
							options = new JSONObject(query);
							
							if (options.has(GlobalOptions.files.name())) {
								files = options.getJSONArray(GlobalOptions.files.name());
								if (options.has(GlobalOptions.minification.name())) {
									try {
										minification = MinificationOptions.valueOf(options.getString(GlobalOptions.minification.name()).toLowerCase());
									} catch (IllegalArgumentException e) {
										Engine.logEngine.info("(MinificationManager) Bad minification level requested: " + options.getString(FileOptions.minification.name()) + " (use common setting)");
									}
								}
							}
							
							if (options.has(GlobalOptions.version.name())) {
								version = options.getString(GlobalOptions.version.name());
							}
							
							try {
								boolean filenames = Boolean.parseBoolean(options.getString(GlobalOptions.filenames.name()));
								resourceBundle.setFilenames(filenames);
							} catch (Exception e) {};
							
						} catch (JSONException e1) {
							
						}
						try {
							if (files == null) {
								files = new JSONArray(query);
							}
						} catch (JSONException e2) {
							files = new JSONArray();
							if (options != null) {
								files.put(options);
							} else if (query.length() == 0) {
								files.put(RequestPart.fileName.value(requestMatcher));
							} else {
								files.put(query);
							}
						}
						
						File relativeFile = new File(Engine.PROJECTS_PATH + "/" + RequestPart.pathFromProject.value(requestMatcher)).getParentFile();
						relativeFile = Engine.resolveProjectPath(relativeFile);
						Engine.logEngine.trace("(MinificationManager) Solve relative resource from '" + relativeFile.toString() + "'");
						
						int optionsLength = files.length();
						resourceBundle.init(optionsLength);
						for (int i = 0; i < optionsLength; i++) {
							Object optionObject = files.get(i);
							String filepath = null;
							String encoding = "utf-8";
							MinificationOptions resourceMinification = null;
							if (optionObject instanceof JSONObject) {
								JSONObject option = (JSONObject) optionObject;
								if (option.has(FileOptions.file.name())) {
									filepath = option.getString(FileOptions.file.name());
								}
								if (option.has(FileOptions.encoding.name())) {
									encoding = option.getString(FileOptions.encoding.name());
								}
								if (option.has(FileOptions.minification.name())) {
									try {
										resourceMinification = MinificationOptions.valueOf(option.getString(FileOptions.minification.name()));
									} catch (IllegalArgumentException e) {
										Engine.logEngine.info("(MinificationManager) Bad minification level requested: " + option.getString(FileOptions.minification.name()) + " (use common setting)");
									}
								}
							} else {
								filepath = optionObject.toString();
							}
							
							if (filepath != null) {
								String fileExtension = '.' + resourceBundle.resourceType.name();
								if (!filepath.endsWith(fileExtension)) {
									filepath += fileExtension;
								}
								if (resourceMinification == null) {
									resourceMinification = filepath.endsWith(".min" + fileExtension) ? MinificationOptions.none : minification;
								}
								
								File file = new File(filepath.startsWith("/") ? resourceBundle.getCommonFolder(version) : relativeFile, filepath);
								
								if (file.exists()) {
									Engine.logEngine.trace("(MinificationManager) Add file '" + file + "' (" + encoding +")");
									if (resourceMinification == MinificationOptions.common) {
										resourceMinification = commonMinification;
									}
									resourceBundle.add(new ResourceEntry(file, encoding, resourceMinification));
								} else if (!filepath.startsWith("!")) {
									Engine.logEngine.debug("(MinificationManager) Failed to add file '" + file + "' [" + filepath +"]");
								}
							}
						}
					}
					
					// clear old entries
					synchronized (minificationCacheDirectory) {
						long now = System.currentTimeMillis();
						if (now - minificationCacheDirectory.lastModified() > 3600000) { // check 1 time / hour
							Engine.logEngine.trace("(MinificationManager) Check cache files to suppress");
							for (File file : minificationCacheDirectory.listFiles()) {
								if (now - file.lastModified() > 172800000) { // delete older than 48H
									Engine.logEngine.debug("(MinificationManager) Suppress old cache file: " + file);
									FileUtils.deleteQuietly(file);
								}
							}
							minificationCacheDirectory.setLastModified(now);
						}
					}
					
					if (resourceBundle.resources.isEmpty()) {
						resourceBundle = null;
					}
					
					return resourceBundle;
				}
			}
		} catch (Exception e) {
			Engine.logEngine.warn("(MinificationManager) Failed to process minified resources", e);
		}
		
		return null;
	}
	
	public void init() throws EngineException {
		minificationCacheDirectory.mkdirs();
	}
	
	public void destroy() throws EngineException {
		cache.clear();
	}
	
	static private File getCommonFolder(ResourceType resourceType) {
		return new File(Engine.WEBAPP_PATH + (resourceType == ResourceType.js ? "/scripts" : "/css"));
	}
	
	static public File getCommonCssResource(HttpServletRequest request) {
		String referer = request.getHeader("Referer");
		if (referer != null) {
			Matcher mRefererTail = tailCssUrl.matcher(referer);
			if (mRefererTail.matches()) {
				String requestURL = HttpUtils.originalRequestURL(request);
				String refererTail = mRefererTail.group(1);
				if (requestURL.startsWith(refererTail)) {
					String relativePath = requestURL.substring(refererTail.length());
					return new File(getCommonFolder(ResourceType.css), relativePath);
				}
			}
		}
		return null;
	}
}
