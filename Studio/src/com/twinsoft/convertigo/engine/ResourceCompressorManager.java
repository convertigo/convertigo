package com.twinsoft.convertigo.engine;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
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

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import ro.isdc.wro.extensions.processor.js.UglifyJsProcessor;
import ro.isdc.wro.model.resource.processor.impl.css.CssCompressorProcessor;
import ro.isdc.wro.model.resource.processor.impl.css.CssMinProcessor;
import ro.isdc.wro.model.resource.processor.impl.js.JSMinProcessor;

import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class ResourceCompressorManager implements AbstractManager {
	static final File compressorCacheDirectory = new File(Engine.USER_WORKSPACE_PATH + "/compressor");
	static final Pattern requestPattern = Pattern.compile("(.*?/projects/|^)(((.*?)/.*?([^/]*?\\.(?:(js)|(css))))(?:\\?(.*)|$))");
	
	// Sample of use:
	// <script src="js/tdd.js?{compression:'standard', resources:['/jquery.min.js','/jquery.mobilelib.js','/ctf.core.js','custom.js','/jquery.mobile.min.js']}"></script>
	
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
		compression, resources
	}
	
	private enum FileOptions {
		file, encoding
	}
	
	private enum CompressionOptions {
		none, light, standard
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
		
		private ResourceEntry(File file, String encoding) {
			this.file = file;
			this.encoding = encoding;
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
	}
	
	public class ResourceBundle {
		private List<ResourceEntry> resources;
		private File cacheFile;
		private ResourceType resourceType;
		private File virtualFile;
		private CompressionOptions compression = CompressionOptions.standard;
		
		private ResourceBundle(ResourceType resourceType, File virtualFile, String key) {
			this.resourceType= resourceType; 
			this.virtualFile = virtualFile;
			key = key.replaceFirst("(.*?)/_private/(?:flashupdate|mobile)/", "$1/DisplayObjects/mobile/");
			key = StringUtils.normalize(key);
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
				StringWriter compressed = new StringWriter();				
				
				sources.append("/*!\n * C8O resource compressor for ");
				
				for (ResourceEntry resourceEntry : resources) {
					sources.append(resourceEntry.getFile().getName() + " ");
				}
				
				sources.append("\n */\n");
				
				for (ResourceEntry resourceEntry : resources) {
					File file = resourceEntry.getFile();
					sources.append("/*! START OF " + file.getName() + " */\n");
					sources.append(FileUtils.readFileToString(file, resourceEntry.getEncoding()));
					sources.append("\n/*! END OF " + file.getName() + "*/\n");
				}
				
				if (compression == CompressionOptions.none) {
					result = sources.toString();
				} else {
					if (resourceType == ResourceType.js) {
						if (compression == CompressionOptions.light) {
							new JSMinProcessor().process(new StringReader(sources.toString()), compressed);
						} else {
							new UglifyJsProcessor().process(new StringReader(sources.toString()), compressed);
						}
					} else {
						if (compression == CompressionOptions.light) {
							new CssMinProcessor().process(new StringReader(sources.toString()), compressed);
						} else {
							new CssCompressorProcessor().process(new StringReader(sources.toString()), compressed);
						}
					}
					result = compressed.toString();
				}
				FileUtils.write(cacheFile, result, "utf-8");
				
				for (ResourceEntry resourceEntry : resources) {
					resourceEntry.update();
				}
				
				if (Engine.logEngine.isDebugEnabled()) {
					int saved = (100 - (result.length() * 100 / sources.toString().length()));
					Engine.logEngine.debug("(ResourceCompressor) Write new compressed [" + compression + ": save " + saved + "%] '" + resourceType + "' response to: " + cacheFile);
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
		
		private void setCompression(CompressionOptions compression) {
			this.compression = compression;
		}
		
		public ResourceType getResourceType() {
			return resourceType;
		}
	}
	
	private Map<String, ResourceBundle> cache = new HashMap<String, ResourceCompressorManager.ResourceBundle>();
	
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
				String key = RequestPart.fullFromProject.value(requestMatcher);
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
									resourceBundle.setCompression(CompressionOptions.valueOf(options.getString(GlobalOptions.compression.name()).toLowerCase()));
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
							if (optionObject instanceof JSONObject) {
								JSONObject option = (JSONObject) optionObject;
								if (option.has(FileOptions.file.name())) {
									filepath = option.getString(FileOptions.file.name());
								}
								if (option.has(FileOptions.encoding.name())) {
									encoding = option.getString(FileOptions.encoding.name());
								}
							} else {
								filepath = optionObject.toString();
							}
							if (filepath != null) {
								File file = null;
								if (filepath.startsWith("/")) {
									file = new File(Engine.WEBAPP_PATH + (resourceBundle.resourceType == ResourceType.js ? "/scripts" : "/css") + filepath);
								} else {
									file = new File(relativeFile, filepath);
								}
								if (file != null && file.exists()) {
									Engine.logEngine.trace("(ResourceCompressor) Add file '" + file + "' (" + encoding +")");
									resourceBundle.add(new ResourceEntry(file, encoding));
								} else {
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
			Engine.logEngine.trace("(ResourceCompressor) Failed to process compressed resources", e);
		}
		
		return null;
	}
	
	public void init() throws EngineException {
		compressorCacheDirectory.mkdirs();
	}
	
	public void destroy() throws EngineException {
		cache.clear();
	}
}
