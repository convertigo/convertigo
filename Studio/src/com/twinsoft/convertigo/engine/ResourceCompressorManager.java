package com.twinsoft.convertigo.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
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

import ro.isdc.wro.extensions.processor.js.UglifyJsProcessor;
import ro.isdc.wro.model.resource.processor.impl.css.CssCompressorProcessor;
import ro.isdc.wro.model.resource.processor.impl.css.CssMinProcessor;
import ro.isdc.wro.model.resource.processor.impl.js.JSMinProcessor;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.ComboEnum;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyCategory;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.events.PropertyChangeEvent;
import com.twinsoft.convertigo.engine.events.PropertyChangeEventListener;
import com.twinsoft.convertigo.engine.util.HttpUtils;

public class ResourceCompressorManager implements AbstractManager, PropertyChangeEventListener {
	static final File compressorCacheDirectory = new File(Engine.USER_WORKSPACE_PATH + "/compressor");
	static final Pattern requestPattern = Pattern.compile("(.*?/projects/|^)(((.*?)/.*?([^/]*?\\.(?:(js)|(css))))(?:\\?(.*)|$))");
	static final Pattern tailCssUrl = Pattern.compile("([^?]*/).*?\\.css(?:\\?.*)?");
	
	// Sample of use:
	// <script src="js/all.js?{compression:'strong', resources:['/jquery.min','/jquery.mobilelib','/ctf.core','custom','/jquery.mobile.min']}"></script>
	
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
		compression, resources, stats, filenames
	}
	
	private enum FileOptions {
		compression, file, encoding
	}
	
	public enum CompressionOptions implements ComboEnum {
		common(null),
		none("none (no minification)"),
		lines("lines (no minification + lines)"),
		light("light (express minification)"),
		strong("strong (strong minification)");
		
		final String display;
		
		CompressionOptions(String display) {
			this.display = display;
		}
		
		public String getDisplay() {
			return display;
		}
		
		public String getValue() {
			return name();
		}
	}
	
	public enum ResourceType {
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
		private CompressionOptions compression;
		
		private ResourceEntry(File file, String encoding, CompressionOptions compression) {
			this.file = file;
			this.encoding = encoding;
			this.compression = compression;
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
		
		private CompressionOptions getCompression() {
			return compression;
		}
	}
	
	public class ResourceBundle {
		private List<ResourceEntry> resources;
		private File cacheFile;
		private ResourceType resourceType;
		private File virtualFile;
		
		private ResourceBundle(ResourceType resourceType, File virtualFile, String key) {
			this.resourceType= resourceType; 
			this.virtualFile = virtualFile;
			key = key.replaceFirst("(.*?)/_private/(?:flashupdate|mobile)/", "$1/DisplayObjects/mobile/");
			key = DigestUtils.md5Hex(key) + DigestUtils.shaHex(key) + "." + resourceType.name();
			cacheFile = new File(compressorCacheDirectory, key);
		}
		
		private void init(int size) {
			resources = new ArrayList<ResourceCompressorManager.ResourceEntry>(size);
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
				boolean filenames = EnginePropertiesManager.getPropertyAsBoolean(PropertyName.MINIFICATION_FILENAMES);
				boolean stats = EnginePropertiesManager.getPropertyAsBoolean(PropertyName.MINIFICATION_STATS);
				
				int fullsize = 0;
				
				if (filenames) {
					sources.append("/*/!\\ C8O resource compressor: ");
				
					for (ResourceEntry resourceEntry : resources) {
						sources.append(resourceEntry.getFile().getName() + " ");
					}
					
					sources.append("*/\n");
				}
				
				for (ResourceEntry resourceEntry : resources) {
					File file = resourceEntry.getFile();
					CompressionOptions compression = resourceEntry.getCompression();
					
					if (filenames) {
						sources.append("/*/!\\ <" + file.getName() + "> */\n");
					}
					if (compression == CompressionOptions.lines) {
						BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), resourceEntry.getEncoding()));
						int lineCpt = 1;
						
						for (String line = br.readLine(); line != null; line = br.readLine()) {
							fullsize += (line.length() + 1);
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
						String uncompressed = FileUtils.readFileToString(file, resourceEntry.getEncoding());
						fullsize += uncompressed.length();
						
						if (compression == CompressionOptions.none) {
							sources.append(uncompressed);
						} else {
							StringWriter compressed = new StringWriter();
							try {
								if (resourceType == ResourceType.js) {
									if (compression == CompressionOptions.light) {
										new JSMinProcessor().process(new StringReader(uncompressed), compressed);
									} else {
										new UglifyJsProcessor().process(new StringReader(uncompressed), compressed);
										compressed.append(';');
									}
								} else {
									if (compression == CompressionOptions.light) {
										new CssMinProcessor().process(new StringReader(uncompressed), compressed);
									} else {
										new CssCompressorProcessor().process(new StringReader(uncompressed), compressed);
									}
								}
								sources.append(compressed.toString());
							} catch (Throwable e) {
								e.printStackTrace();
								sources.append(uncompressed);
							}
						}
					}
					if (filenames) {
						sources.append("\n/*/!\\ </" + file.getName() + "> */\n");
					}
				}
				
				int saved = (100 - (sources.getBuffer().length() * 100 / fullsize));
				if (stats) {
					sources.append("/*/!\\ " + saved + "%  saved */");
				}
				
				Engine.logEngine.debug("(ResourceCompressor) Write new compressed [saved " + saved + "%] '" + resourceType + "' response to: " + cacheFile);
				
				result = sources.toString();
				
				FileUtils.write(cacheFile, result, "utf-8");
				
				for (ResourceEntry resourceEntry : resources) {
					resourceEntry.update();
				}
			} else {
				Engine.logEngine.trace("(ResourceCompressor) Serve existing compressed result from: " + cacheFile);
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

		public File getCommonFolder() {
			return ResourceCompressorManager.getCommonFolder(resourceType);
		}
	}
	
	private Map<String, ResourceBundle> cache = new HashMap<String, ResourceCompressorManager.ResourceBundle>();
	private String settingKey;
	
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
				response.setContentType(resourceBundle.getResourceType() == ResourceType.js ? "application/javascript" : "text/css");
				response.setCharacterEncoding("utf-8");
				response.getWriter().write(resourceBundle.getResult());
			} catch (IOException e) {
				Engine.logEngine.error("(ResourceCompressor) Failed to write compressed result", e);
			}
			return true;
		} else {			
			return false;
		}
	}
	
	public ResourceBundle process(String requestedFile) {
		Engine.logEngine.trace("(ResourceCompressor) Check request compatibility of '" + requestedFile + "'");
		Matcher requestMatcher = requestPattern.matcher(requestedFile);
		return requestMatcher == null ? null : process(requestMatcher);
	}
	
	private ResourceBundle process(Matcher requestMatcher) {
		try {
			if (requestMatcher.matches() && RequestPart.query.value(requestMatcher) != null) {
				String key = RequestPart.fullFromProject.value(requestMatcher) + settingKey;
				CompressionOptions compression = CompressionOptions.common;
				CompressionOptions commonCompression = EnginePropertiesManager.getPropertyAsEnum(PropertyName.MINIFICATION_LEVEL);
				
				ResourceBundle resourceBundle;
				synchronized (cache) {
					resourceBundle = cache.get(key);
					if (resourceBundle == null) {
						String virtualFilePath = Engine.PROJECTS_PATH + "/" + RequestPart.pathFromProject.value(requestMatcher);
						resourceBundle = new ResourceBundle(ResourceType.get(requestMatcher), new File(virtualFilePath), key);
						cache.put(key, resourceBundle);
						Engine.logEngine.debug("(ResourceCompressor) Create resourceBundle '" + key + "'");
					} else {
						Engine.logEngine.trace("(ResourceCompressor) Re-use resourceBundle '" + key + "'");
					}
				}
				synchronized (resourceBundle) {
					if (!resourceBundle.isInit()) {
						JSONArray resources = null;
						JSONObject options = null;
						String query = RequestPart.query.value(requestMatcher);
						Engine.logEngine.trace("(ResourceCompressor) Parse JSON query '" + query + "'");
						try {
							options = new JSONObject(query);
							if (options.has(GlobalOptions.resources.name())) {
								resources = options.getJSONArray(GlobalOptions.resources.name());
								if (options.has(GlobalOptions.compression.name())) {
									try {
										compression = CompressionOptions.valueOf(options.getString(GlobalOptions.compression.name()).toLowerCase());
									} catch (IllegalArgumentException e) {
										Engine.logEngine.info("(ResourceCompressor) Bad compression name requested: " + options.getString(FileOptions.compression.name()) + " (use common setting)");
									}
								}
							}
						} catch (JSONException e1) {
							
						}
						try {
							if (resources == null) {
								resources = new JSONArray(query);
							}
						} catch (JSONException e2) {
							resources = new JSONArray();
							if (options != null) {
								resources.put(options);
							} else if (query.length() == 0) {
								resources.put(RequestPart.fileName.value(requestMatcher));
							} else {
								resources.put(query);
							}
						}
						
						File relativeFile = new File(Engine.PROJECTS_PATH + "/" + RequestPart.pathFromProject.value(requestMatcher)).getParentFile();
						Engine.logEngine.trace("(ResourceCompressor) Solve relative resource from '" + relativeFile.toString() + "'");
						
						int optionsLength = resources.length();
						resourceBundle.init(optionsLength);
						for (int i = 0; i < optionsLength; i++) {
							Object optionObject = resources.get(i);
							String filepath = null;
							String encoding = "utf-8";
							CompressionOptions resourceCompression = null;
							if (optionObject instanceof JSONObject) {
								JSONObject option = (JSONObject) optionObject;
								if (option.has(FileOptions.file.name())) {
									filepath = option.getString(FileOptions.file.name());
								}
								if (option.has(FileOptions.encoding.name())) {
									encoding = option.getString(FileOptions.encoding.name());
								}
								if (option.has(FileOptions.compression.name())) {
									try {
										resourceCompression = CompressionOptions.valueOf(option.getString(FileOptions.compression.name()));
									} catch (IllegalArgumentException e) {
										Engine.logEngine.info("(ResourceCompressor) Bad compression name requested: " + option.getString(FileOptions.compression.name()) + " (use common setting)");
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
								if (resourceCompression == null) {
									resourceCompression = filepath.endsWith(".min" + fileExtension) ? CompressionOptions.none : compression;
								}
								
								File file = new File(filepath.startsWith("/") ? resourceBundle.getCommonFolder() : relativeFile, filepath);
								
								if (file.exists()) {
									Engine.logEngine.trace("(ResourceCompressor) Add file '" + file + "' (" + encoding +")");
									if (resourceCompression == CompressionOptions.common) {
										resourceCompression = commonCompression;
									}
									resourceBundle.add(new ResourceEntry(file, encoding, resourceCompression));
								} else if (!filepath.startsWith("!")) {
									Engine.logEngine.debug("(ResourceCompressor) Failed to add file '" + file + "' [" + filepath +"]");
								}
							}
						}
					}
					
					// clear old entries
					synchronized (compressorCacheDirectory) {
						long now = System.currentTimeMillis();
						if (now - compressorCacheDirectory.lastModified() > 3600000) { // check 1 time / hour
							Engine.logEngine.trace("(ResourceCompressor) Check cache files to suppress");
							for (File file : compressorCacheDirectory.listFiles()) {
								if (now - file.lastModified() > 172800000) { // delete older than 48H
									Engine.logEngine.debug("(ResourceCompressor) Suppress old cache file: " + file);
									FileUtils.deleteQuietly(file);
								}
							}
							compressorCacheDirectory.setLastModified(now);
						}
					}
					
					
					return resourceBundle;
				}
			}
		} catch (Exception e) {
			Engine.logEngine.warn("(ResourceCompressor) Failed to process compressed resources", e);
		}
		
		return null;
	}
	
	public void init() throws EngineException {
		refreshSettingKey();
		compressorCacheDirectory.mkdirs();
		Engine.theApp.eventManager.addListener(this, PropertyChangeEventListener.class);
	}
	
	public void destroy() throws EngineException {
		Engine.theApp.eventManager.removeListener(this, PropertyChangeEventListener.class);
		cache.clear();
	}

	synchronized private void refreshSettingKey() {
		settingKey = "";
		for (PropertyName propertyName : Arrays.asList(
				PropertyName.MINIFICATION_FILENAMES,
				PropertyName.MINIFICATION_LEVEL,
				PropertyName.MINIFICATION_STATS)) {
			settingKey += ";" + propertyName.name() + "=" + EnginePropertiesManager.getProperty(propertyName);
		}		
	}
	
	public void onEvent(PropertyChangeEvent event) {
		if (event.getKey().category == PropertyCategory.Minification) {
			refreshSettingKey();
		}
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
