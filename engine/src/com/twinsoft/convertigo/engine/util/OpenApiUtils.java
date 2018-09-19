/*
 * Copyright (c) 2001-2018 Convertigo SA.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.codehaus.jettison.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.twinsoft.convertigo.beans.core.IMappingRefModel;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.UrlMapper;
import com.twinsoft.convertigo.beans.core.UrlMapping;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.core.UrlMappingResponse;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter.DataContent;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter.DataType;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter.Type;
import com.twinsoft.convertigo.beans.rest.AbstractRestOperation;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.SchemaManager.Option;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.enums.MimeType;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.FileSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.util.OpenAPIDeserializer;

public class OpenApiUtils {

	public static String servletMappingPath = "openapi";
	private static String jsonSchemaDirectory = "oas3";
	private static Pattern parseRequestUrl = Pattern.compile("http(s)?://(.*?)(/.*?"+servletMappingPath+")");
	
	public static OpenAPI read(String url) {
		return new OpenAPIV3Parser().read(url);
	}
	
	@SuppressWarnings("unused")
	private static void toOas3Content(File jsonschemaFile) {
		try {
			File targetDir = jsonschemaFile.getParentFile();
			String name = jsonschemaFile.getName().substring(0, jsonschemaFile.getName().indexOf('.'));
			
			String content = FileUtils.readFileToString(jsonschemaFile, "UTF-8");
			content = content.replaceAll(name+"\\.jsonschema#", "#");
			content = content.replaceAll("\"#\\\\/definitions", "\"#/components/schemas");
			content = content.replaceAll("\\.jsonschema#\\\\/definitions", ".json#/components/schemas");
			
			JSONObject jsonModels = new JSONObject();
			JSONObject jsonSchemas = new JSONObject(content).getJSONObject("definitions");
			@SuppressWarnings("rawtypes")
			Iterator it = jsonSchemas.keys();
			while (it.hasNext()) {
				String key = (String) it.next();
				Object ob = jsonSchemas.get(key);
				if (ob != null) {
					jsonModels.put(key, ob);
				}
			}
			//models = jsonModels.toString();
			
			OpenAPI oa = new OpenAPI();
			String s = Json.pretty(oa.info(new Info()));
			JSONObject json = new JSONObject(s);
			json.put("components", new JSONObject());
			json.getJSONObject("components").put("schemas", jsonModels);
			
			JsonNode rootNode = Json.mapper().readTree(json.toString());
			OpenAPIDeserializer ds = new OpenAPIDeserializer();
			SwaggerParseResult result = ds.deserialize(rootNode);
			
			String openApiContent = Json.pretty(result.getOpenAPI());
			//System.out.println(openApiContent);
			
			File jsonFile = new File(targetDir, name+".json");
			FileUtils.write(jsonFile, openApiContent, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String getModels(String requestUrl, UrlMapper urlMapper) {
		Project project = urlMapper.getProject();
		String projectName = project.getName();
		
		// User defined models
		String models = "{}";
		String mapperModels = urlMapper.getModels();
		if (!mapperModels.isEmpty()) {
			models = mapperModels;
		}
		
		// Generated models from XSD
		File targetDir = new File(Engine.PROJECTS_PATH + "/" + projectName + "/" + jsonSchemaDirectory);
		boolean doIt = Engine.isStudioMode() || !targetDir.exists();
		if (doIt) {
			try {
				
				File xsdFile = new File(targetDir, "xsd.jsonschema");
				if (!xsdFile.exists()) {
					FileUtils.copyFile(new File(Engine.WEBAPP_PATH + "/oas/xsd3.jsonschema"), xsdFile, true);
					//toOas3Content(xsdFile);
				}
				
				XmlSchemaCollection xmlSchemaCollection = Engine.theApp.schemaManager.getSchemasForProject(projectName, Option.noCache);
				NamespaceMap nsMap = (NamespaceMap) xmlSchemaCollection.getNamespaceContext();
				for (XmlSchema xmlSchema : xmlSchemaCollection.getXmlSchemas()) {
					String tns = xmlSchema.getTargetNamespace();
					if (tns.equals(Constants.URI_2001_SCHEMA_XSD)) continue;
					if (tns.equals(SchemaUtils.URI_SOAP_ENC)) continue;
	
					String prefix = nsMap.getPrefix(tns);
					File jsonschemaFile = new File(targetDir, prefix+".jsonschema" );
					JSONObject jsonObject = JsonSchemaUtils.getJsonSchema(xmlSchemaCollection, xmlSchema, false);
					String content = jsonObject.toString(4);
					FileUtils.write(jsonschemaFile, content, "UTF-8");
					//toOas3Content(jsonschemaFile);
					//System.out.println(content);
				}
			} catch (Exception e) {
				e.printStackTrace();
				try {
					Engine.logEngine.warn("Unexpected exception while generating Oas3 models from XSD", e);
					FileUtils.deleteDirectory(targetDir);
				} catch (Exception ex) {}
			}
		}
		
		return models;
	}
	
	private static OpenAPI parseCommon(String requestUrl, Project project) {
		OpenAPI openAPI = new OpenAPI();
		
		Info info = new Info();
		info.setContact(new Contact());
		info.setTitle("Convertigo REST API");
		info.setDescription("Find here all deployed projects");
		if (project != null) {
			info.setDescription(project.getComment());
			info.setVersion(project.getVersion());			
		}
		openAPI.setInfo(info);
		
		List<Server> servers = new ArrayList<>();
		String serverUrl, scheme, host, basePath;
		Matcher matcher = parseRequestUrl.matcher(requestUrl);
		if (matcher.find()) {
			scheme = matcher.group(1) == null ? "http":"https";
			host = matcher.group(2);
			basePath = matcher.group(3);
			serverUrl = scheme+ "://"+ host + basePath;
		} else {
			String webAppPath = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
			int index = webAppPath.indexOf("://") + 3;
			scheme = webAppPath.substring(0, webAppPath.indexOf("://"));
			host = webAppPath.substring(index, webAppPath.indexOf('/', index));
			basePath = webAppPath.substring(index + host.length()) + "/" + servletMappingPath;
			serverUrl = scheme+ "://"+ host + basePath;
		}
		
		Server server1 = new Server();		
		server1.setDescription("Main server");
		server1.setUrl(serverUrl);
		servers.add(server1);
		openAPI.setServers(servers);
		
		String oas2Url = requestUrl.substring(0,requestUrl.indexOf("/" + servletMappingPath)) + "/swagger/dist/index.html?" + 
				URLUtils.encodePart("url",requestUrl.replace(servletMappingPath, SwaggerUtils.servletMappingPath) 
					+ "?YAML"+ (project != null ? "&__project=" + project.getName():""));
		ExternalDocumentation externalDocumentation = new ExternalDocumentation();
		externalDocumentation.setDescription("Switch to Swagger definition (oas2)");
		externalDocumentation.setUrl(oas2Url);
		openAPI.setExternalDocs(externalDocumentation);
		
		return openAPI;
	}
	
	private static Schema<?> getSchema(DataType dataType) {
		if (dataType.equals(DataType.Boolean)) {
			 return new BooleanSchema();
		} else if (dataType.equals(DataType.Integer)) {
			return new IntegerSchema();
		} else if (dataType.equals(DataType.Number)) {
			return new NumberSchema();
		} else if (dataType.equals(DataType.String)) {
			return new StringSchema();
		} else if (dataType.equals(DataType.File)) {
			return  new FileSchema();
		} else if (dataType.equals(DataType.Model)) {
			return new ObjectSchema();
		}
		return null;
	}
	
	private static Schema<?> getSchema(UrlMappingParameter ump) {
		Schema<?> schema = null;
		boolean isArray = ump.isMultiValued() || ump.isArray();
		if (isArray) {
			schema = new ArraySchema();
			((ArraySchema)schema).setItems(getSchema(ump.getDataType()));
		} else {
			schema = getSchema(ump.getDataType());
		}
		return schema;
	}
	
	private static void addFormParameter(Operation operation, UrlMappingParameter ump) {
		RequestBody requestBody = operation.getRequestBody();
		if (requestBody == null) {
			operation.setRequestBody(new RequestBody());
			requestBody = operation.getRequestBody();
			requestBody.content(new Content());
			MediaType mediaType = new MediaType();
			mediaType.setSchema(new ObjectSchema());
			requestBody.getContent().addMediaType("application/x-www-form-urlencoded", mediaType);
		}
		
		if (ump.getType() == Type.Form) {
			MediaType mediaType = requestBody.getContent().get("application/x-www-form-urlencoded");
			Schema<?> mediaSchema =  mediaType.getSchema();
			Schema<?> propertiesItem = getSchema(ump);
			if (propertiesItem != null) {
				propertiesItem.setDescription(ump.getComment());
				mediaSchema.addProperties(ump.getName(), propertiesItem);

				if (ump.isRequired()) {
					List<String> requiredList = mediaSchema.getRequired();
					if (requiredList == null || !requiredList.contains(ump.getName())) {
						mediaSchema.addRequiredItem(ump.getName());
					}
				}
			}
		}
		
	}
	
	private static void addBodyParameter(Operation operation, UrlMappingParameter ump, String oasDirUrl) {
		RequestBody requestBody = operation.getRequestBody();
		if (requestBody == null) {
			operation.setRequestBody(new RequestBody());
			requestBody = operation.getRequestBody();
			requestBody.content(new Content());
			MediaType mediaType = new MediaType();
			String modelReference = ((IMappingRefModel)ump).getModelReference();
			if (!modelReference.isEmpty()) {
				if (modelReference.indexOf(".jsonschema") != -1) {
					modelReference = oasDirUrl + modelReference;
				}
				ObjectSchema oschema = new ObjectSchema();
				oschema.set$ref(modelReference);
				mediaType.setSchema(oschema);
			}
			
			DataContent dataInput = ump.getInputContent();
			if (dataInput.equals(DataContent.toJson)) {
				requestBody.getContent().addMediaType(MimeType.Json.value(), mediaType);
			}
			else if (dataInput.equals(DataContent.toXml)) {
				requestBody.getContent().addMediaType(MimeType.Xml.value(), mediaType);
			}
		}		
	}
	
	public static OpenAPI parse(String requestUrl, UrlMapper urlMapper) {
		Project project = urlMapper.getProject();
		String projectName = project.getName();
		
		String oasDirUrl = requestUrl.substring(0,requestUrl.indexOf("/"+servletMappingPath)) + 
										"/projects/"+ projectName + "/"+ jsonSchemaDirectory +"/";
		
		OpenAPI openAPI = parseCommon(requestUrl, project);
		
		List<Tag> tags = new ArrayList<>();
		Tag tag = new Tag();
		tag.setName(project.getName());
		tag.setDescription(project.getComment());
		tags.add(tag);
		openAPI.setTags(tags);
		
		
		try {
			String models = getModels(requestUrl, urlMapper);
			JSONObject jsonModels = new JSONObject(models);
			
			OpenAPI oa = new OpenAPI();
			String s = Json.pretty(oa.info(new Info()));
			JSONObject json = new JSONObject(s);
			json.put("components", new JSONObject());
			json.getJSONObject("components").put("schemas", jsonModels);
			
			JsonNode rootNode = Json.mapper().readTree(json.toString());
			OpenAPIDeserializer ds = new OpenAPIDeserializer();
			SwaggerParseResult result = ds.deserialize(rootNode);
			
			@SuppressWarnings("rawtypes")
			Map<String, Schema> map = result.getOpenAPI().getComponents().getSchemas();
			if (openAPI.getComponents() == null) {
				openAPI.components(new Components());
			}
			openAPI.getComponents().schemas(map);
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
		Paths paths = new Paths();
		try {
			for (UrlMapping urlMapping: urlMapper.getMappingList()) {
				PathItem item = new PathItem();
				for (UrlMappingOperation umo : urlMapping.getOperationList()) {
					Operation operation = new Operation();
					operation.setOperationId(umo.getQName());
					operation.setDescription(umo.getComment());
					operation.setSummary(umo.getComment());
					
					// Tags
					List<String> list = Arrays.asList(""+ project.getName());
					operation.setTags(list);
					
					// Parameters
					//  1 - add path parameters
					for (String pathVarName: urlMapping.getPathVariableNames()) {
						PathParameter parameter = new PathParameter();
						parameter.setName(pathVarName);
						
						// retrieve parameter description from bean
						UrlMappingParameter ump = null;
						try {
							ump = umo.getParameterByName(pathVarName);
						} catch (Exception e) {}
						if (ump != null && ump.getType() == Type.Path) {
							parameter.setDescription(ump.getComment());
							Schema<?> schema = getSchema(ump.getDataType());
							if (schema != null) {
								parameter.setSchema(schema);
							}
						}
						operation.addParametersItem(parameter);
					}
						
					//  2 - add other parameters
					for (UrlMappingParameter ump: umo.getParameterList()) {
						Parameter parameter = null;
						if (ump.getType() == Type.Query) {
							parameter = new QueryParameter();
						} else if (ump.getType() == Type.Form) {
							addFormParameter(operation, ump);
						} else if (ump.getType() == Type.Body) {
							addBodyParameter(operation, ump, oasDirUrl);
						} else if (ump.getType() == Type.Header) {
							parameter = new HeaderParameter();
						} else if (ump.getType() == Type.Path) {
							// ignore : should have been treated before
						}
						
						if (parameter != null) {
							parameter.setName(ump.getName());
							parameter.setDescription(ump.getComment());
							parameter.setRequired(ump.isRequired());
							Schema<?> schema = getSchema(ump);
							if (schema != null) {
								parameter.setSchema(schema);
							}
							
							operation.addParametersItem(parameter);
						}
					}
					
					// Responses
					List<String> produces = new ArrayList<String>();
					if (umo instanceof AbstractRestOperation) {
						DataContent dataOutput = ((AbstractRestOperation)umo).getOutputContent();
						if (dataOutput.equals(DataContent.toJson)) {
							produces = Arrays.asList(MimeType.Json.value());
						}
						else if (dataOutput.equals(DataContent.toXml)) {
							produces = Arrays.asList(MimeType.Xml.value());
						}
						else {
							produces = Arrays.asList(MimeType.Json.value(), MimeType.Xml.value());
						}
					}
					
					
					ApiResponses responses = new ApiResponses();
					operation.setResponses(responses);
					for (UrlMappingResponse umr: umo.getResponseList()) {
						String statusCode = umr.getStatusCode();
						if (!statusCode.isEmpty()) {
							if (!responses.containsKey(statusCode)) {
								ApiResponse response = new ApiResponse();
								response.setDescription(umr.getStatusText());
								responses.addApiResponse(statusCode, response);
								
								String modelReference = ((IMappingRefModel)umr).getModelReference();
								if (!modelReference.isEmpty() && !produces.isEmpty()) {
									if (modelReference.indexOf(".jsonschema") != -1) {
										//modelReference = modelReference.replace(".jsonschema#/definitions/", ".json#/components/schemas/");
										modelReference = oasDirUrl + modelReference;
									}
									Content content = new Content();
									response.setContent(content);
									for (String mt: produces) {
										MediaType mediaType = new MediaType();
										content.addMediaType(mt, mediaType);
										ObjectSchema schema = new ObjectSchema();
										schema.set$ref(modelReference);
										mediaType.setSchema(schema);
									}
								}
							}
						}
					}
					
					if (umo.getMethod().equals(HttpMethodType.DELETE.name())) {
						item.setDelete(operation);
					} else if (umo.getMethod().equals(HttpMethodType.GET.name())) {
						item.setGet(operation);
					} else if (umo.getMethod().equals(HttpMethodType.HEAD.name())) {
						item.setHead(operation);
					} else if (umo.getMethod().equals(HttpMethodType.OPTIONS.name())) {
						item.setOptions(operation);
					} else if (umo.getMethod().equals(HttpMethodType.POST.name())) {
						item.setPost(operation);
					} else if (umo.getMethod().equals(HttpMethodType.PUT.name())) {
						item.setPut(operation);
					} else if (umo.getMethod().equals(HttpMethodType.TRACE.name())) {
						item.setTrace(operation);
					}
				}
				
				paths.addPathItem(urlMapping.getPathWithPrefix(), item);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Engine.logEngine.error("Unexpected exception while parsing UrlMapper to generate definition", e);
		}
		openAPI.setPaths(paths);
		
		return openAPI;
	}
	
	@SuppressWarnings("rawtypes")
	public static OpenAPI parse(String requestUrl, Collection<UrlMapper> collection) {
		OpenAPI openAPI = parseCommon(requestUrl, null);
		
		List<Tag> tags = new ArrayList<>();
		Components components = new Components();
		components.callbacks(new HashMap<String, Callback>());
		components.examples(new HashMap<String, Example>());
		components.extensions(new HashMap<String, Object>());
		components.headers(new HashMap<String, Header>());
		components.links(new HashMap<String, Link>());
		components.parameters(new HashMap<String, Parameter>());
		components.requestBodies(new HashMap<String, RequestBody>());
		components.responses(new HashMap<String, ApiResponse>());
		components.schemas(new HashMap<String, Schema>());
		components.securitySchemes(new HashMap<String, SecurityScheme>());
		
		Paths paths = new Paths();
		for (UrlMapper urlMapper : collection) {
			if (urlMapper != null) {
				OpenAPI _openAPI = parse(requestUrl, urlMapper);
				if (_openAPI != null) {
					try {tags.addAll(_openAPI.getTags());} catch (Exception e) {}
					try {paths.putAll(_openAPI.getPaths());} catch (Exception e) {}
					
					Components _components = _openAPI.getComponents();
					if (_components != null) {
						try {components.getCallbacks().putAll(_components.getCallbacks());} catch (Exception e) {}
						try {components.getExamples().putAll(_components.getExamples());} catch (Exception e) {}
						try {components.getExtensions().putAll(_components.getExtensions());} catch (Exception e) {}
						try {components.getHeaders().putAll(_components.getHeaders());} catch (Exception e) {}
						try {components.getLinks().putAll(_components.getLinks());} catch (Exception e) {}
						try {components.getParameters().putAll(_components.getParameters());} catch (Exception e) {}
						try {components.getRequestBodies().putAll(_components.getRequestBodies());} catch (Exception e) {}
						try {components.getResponses().putAll(_components.getResponses());} catch (Exception e) {}
						try {components.getSchemas().putAll(_components.getSchemas());} catch (Exception e) {}
						try {components.getSecuritySchemes().putAll(_components.getSecuritySchemes());} catch (Exception e) {}
					}
				}
			}
		}
		
		openAPI.setTags(tags);
		openAPI.setPaths(paths);
		openAPI.setComponents(components);
		return openAPI;
	}
	
	public static OpenAPI parse(String requestUrl, String projectName) {
		OpenAPI openAPI;
		
		Project project;
		try {
			project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
			openAPI = parseCommon(requestUrl, project);
			
			List<Tag> tags = new ArrayList<>();
			Tag tag = new Tag();
			tag.setName(projectName);
			tag.setDescription(project.getComment());
			tags.add(tag);
			openAPI.setTags(tags);
			
		} catch (Exception e) {
			e.printStackTrace();
			openAPI = new OpenAPI();
		}
		
		return openAPI;
	}
	
	public static String prettyPrintJson(OpenAPI openAPI) {
		return Json.pretty(openAPI);
	}
	
	public static String prettyPrintYaml(OpenAPI openAPI) {
		return Yaml.pretty(openAPI);
	}
	
	public static String getYamlDefinition(String requestUrl, Object object) throws JsonProcessingException {
		if (object instanceof String) {	// project name
			return prettyPrintYaml(parse(requestUrl, (String) object));
		}
		if (object instanceof UrlMapper) {	// urlmapper of project
			return prettyPrintYaml(parse(requestUrl, (UrlMapper) object));
		}
		if (object instanceof Collection<?>) { // all projects urlmapper
			Collection<UrlMapper> collection = GenericUtils.cast(object);
			return prettyPrintYaml(parse(requestUrl, collection));
		}
		return null;
	}

	public static String getJsonDefinition(String requestUrl, Object object) {
		if (object instanceof String) {	// project name
			return prettyPrintJson(parse(requestUrl, (String)object));
		}
		if (object instanceof UrlMapper) {
			return prettyPrintJson(parse(requestUrl, (UrlMapper)object));
		}
		if (object instanceof Collection<?>) {
			Collection<UrlMapper> collection = GenericUtils.cast(object);
			return prettyPrintJson(parse(requestUrl, collection));
		}
		return null;
	}
	
	public static void testReadJsonUrl(String urlString) {
		OpenAPI openAPI = read(urlString);
		if (openAPI != null) {
			Json.prettyPrint(openAPI);
			Yaml.prettyPrint(openAPI);
		}
	}
	
	public static void testReadJson() {
		OpenAPI openAPI = read("http://petstore.swagger.io/v2/swagger.json");
		if (openAPI != null) {
			Json.prettyPrint(openAPI);
			Yaml.prettyPrint(openAPI);
		}
	}
	
	public static void testReadYaml() {
		OpenAPI openAPI = read("http://petstore.swagger.io/v2/swagger.yaml");
		if (openAPI != null) {
			Json.prettyPrint(openAPI);
			Yaml.prettyPrint(openAPI);
		}
	}
	
}
