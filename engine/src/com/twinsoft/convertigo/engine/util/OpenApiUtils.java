/*
 * Copyright (c) 2001-2020 Convertigo SA.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.IMappingRefModel;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.UrlAuthentication;
import com.twinsoft.convertigo.beans.core.UrlAuthentication.AuthenticationType;
import com.twinsoft.convertigo.beans.core.UrlMapper;
import com.twinsoft.convertigo.beans.core.UrlMapping;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.core.UrlMappingResponse;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter.DataContent;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter.DataType;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter.Type;
import com.twinsoft.convertigo.beans.rest.AbstractRestOperation;
import com.twinsoft.convertigo.beans.transactions.AbstractHttpTransaction;
import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.beans.transactions.JsonHttpTransaction;
import com.twinsoft.convertigo.beans.transactions.XmlHttpTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableHttpMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.SchemaManager.Option;
import com.twinsoft.convertigo.engine.enums.AuthenticationMode;
import com.twinsoft.convertigo.engine.enums.DoFileUploadMode;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.util.UrlParser.UrlFields;

import io.swagger.v3.core.jackson.SchemaSerializer;
import io.swagger.v3.core.jackson.mixin.ComponentsMixin;
import io.swagger.v3.core.jackson.mixin.ExtensionsMixin;
import io.swagger.v3.core.jackson.mixin.OpenAPIMixin;
import io.swagger.v3.core.jackson.mixin.OperationMixin;
import io.swagger.v3.core.util.DeserializationModule;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.links.LinkParameter;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Encoding;
import io.swagger.v3.oas.models.media.EncodingProperty;
import io.swagger.v3.oas.models.media.FileSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.XML;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
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
	
	private static void walkRefs(Object ob, List<String> refList) {
		try {
			if (ob instanceof JSONObject) {
				JSONObject jsonOb = (JSONObject)ob;
				if (jsonOb.has("$ref")) {
					String ref = jsonOb.getString("$ref");
					if (!refList.contains(ref)) {
						refList.add(ref);
					}
					jsonOb.put("$ref", ref.substring(ref.indexOf('#')));
				}
				
				@SuppressWarnings("unchecked")
				Iterator<String> it = jsonOb.keys();
				while (it.hasNext()) {
					String pkey = it.next();
					walkRefs(jsonOb.get(pkey), refList);
				}
			} else if (ob instanceof JSONArray) {
				JSONArray jsonArray = (JSONArray)ob;
				for (int i = 0; i < jsonArray.length(); i++) {
					walkRefs(jsonArray.get(i), refList);
				}
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private static JSONObject makeCopy(JSONObject jsonOb) throws JSONException {
		if (jsonOb != null) {
			return new JSONObject(jsonOb.toString());
		}
		return null;
	}
	
	private static void toOas3Content(File jsonschemaFile, String oasDirUrl, Map<String, JSONObject> modelMap) {
		try {
			File targetDir = jsonschemaFile.getParentFile();
			String name = jsonschemaFile.getName().substring(0, jsonschemaFile.getName().indexOf('.'));
			
			String content = FileUtils.readFileToString(jsonschemaFile, "UTF-8");
			content = content.replaceAll("\"#\\\\/definitions", "\"#/components/schemas");
			content = content.replaceAll("\\.jsonschema#\\\\/definitions", ".json#/components/schemas");
			
			JSONObject jsonModels = new JSONObject();
			JSONObject jsonDefinition = new JSONObject(content).getJSONObject("definitions");
			@SuppressWarnings("rawtypes")
			Iterator it = jsonDefinition.keys();
			while (it.hasNext()) {
				String key = (String) it.next();
				JSONObject ob = jsonDefinition.getJSONObject(key);
				if (ob != null) {
					jsonModels.put(key, ob);
					
					// fill model map
					String pKey = oasDirUrl + name + ".json#/components/schemas/" + key;
					if (!modelMap.containsKey(pKey)) {
						List<String> refList = new ArrayList<String>();
						JSONArray refs = new JSONArray();
						JSONObject copy = makeCopy(ob);
						walkRefs(copy, refList);
						for (String ref: refList) {
							refs.put(ref);
						}
						modelMap.put(pKey, new JSONObject().put("model", copy).put("refs", refs));
					}
				}
			}
			
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
	
	private static String getModels(String oasDirUrl, UrlMapper urlMapper, Map<String, JSONObject> modelMap) {
		Project project = urlMapper.getProject();
		String projectName = project.getName();
		
		// User defined models
		String models = "{}";
		String mapperModels = urlMapper.getModels();
		if (!mapperModels.isEmpty()) {
			models = mapperModels;
		}

		// Generated models from XmlSchema
		File targetDir = new File(Engine.PROJECTS_PATH + "/" + projectName + "/" + jsonSchemaDirectory);
		File yamlFile = new File(targetDir, projectName+".yaml" );
		boolean doIt = Engine.isStudioMode() || !yamlFile.exists();
		
		if (doIt) {
			try {
				XmlSchemaCollection xmlSchemaCollection = Engine.theApp.schemaManager.getSchemasForProject(projectName, Option.noCache);
				NamespaceMap nsMap = (NamespaceMap) xmlSchemaCollection.getNamespaceContext();
				for (XmlSchema xmlSchema : xmlSchemaCollection.getXmlSchemas()) {
					String tns = xmlSchema.getTargetNamespace();
					if (tns.equals(Constants.URI_2001_SCHEMA_XSD)) continue;
					if (tns.equals(SchemaUtils.URI_SOAP_ENC)) continue;

					// generate models
					JSONObject oasObject = JsonSchemaUtils.getOasSchema(xmlSchemaCollection, xmlSchema, oasDirUrl, false);
					String content = oasObject.toString(4);
					
					// generate .jsonschema (working file for .json generation)
					String prefix = nsMap.getPrefix(tns);
					File jsonSchemaFile = new File(targetDir, prefix+".jsonschema" );
					FileUtils.write(jsonSchemaFile, content, "UTF-8");
					
					// generate .json (oas3 compliant)
					toOas3Content(jsonSchemaFile, oasDirUrl, modelMap);
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
		info.setTitle("Convertigo OAS3 REST API");
		info.setDescription("Find here all deployed projects");
		if (project != null) {
			info.setTitle(project.getName() + " OAS3 REST API");
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
			serverUrl = getConvertigoServeurUrl();
		}
		
		Server server1 = new Server();		
		server1.setDescription("Main server");
		server1.setUrl(serverUrl);
		servers.add(server1);
		openAPI.setServers(servers);
		
		String oas2Url = requestUrl.substring(0,requestUrl.indexOf("/" + servletMappingPath)) + "/swagger/dist/index.html?" + 
				URLUtils.encodePart("url",requestUrl.replace(servletMappingPath, SwaggerUtils.servletMappingPath) 
					+ "?YAML"+ (project != null ? "&__project=" + project.getName():""))
					+ (Engine.isStudioMode() ? "&showErrors" : "");
		
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
		Object value = ump.getValueOrNull();
		boolean isArray = ump.isMultiValued() || ump.isArray();
		
		if (isArray) {
			schema = new ArraySchema();
			((ArraySchema)schema).setItems(getSchema(ump.getDataType()));
//			((ArraySchema)schema).setEnum(Arrays.asList("val1","val2","val3"));
//			if (value != null && value instanceof String) {
//				((ArraySchema)schema).setExample(Arrays.asList(String.valueOf(value).split(";")));
//			}
		} else {
			schema = getSchema(ump.getDataType());
			if (value != null) {
				schema.setDefault(value);
			}
		}
		
		schema.setNullable(ump.isRequired() ? false:true);
		
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
				
				// add parameter
				if (ump.isExposed()) {
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
		
	}
	
	private static void addBodyParameter(Operation operation, UrlMappingParameter ump, String oasDirUrl, List<String> refList, boolean useExternalRef) {
		RequestBody requestBody = operation.getRequestBody();
		if (requestBody == null) {
			operation.setRequestBody(new RequestBody());
			requestBody = operation.getRequestBody();
			requestBody.content(new Content());
			MediaType mediaType = new MediaType();
			String modelReference = ((IMappingRefModel)ump).getModelReference();
			if (!modelReference.isEmpty()) {
				if (modelReference.indexOf(".jsonschema") != -1) {
					modelReference = modelReference.replace(".jsonschema#/definitions/", ".json#/components/schemas/");
					modelReference = oasDirUrl + modelReference;
				}
				if (!refList.contains(modelReference)) {
					refList.add(modelReference);
				}
				if (!useExternalRef  && modelReference.indexOf('#') != -1) {
					modelReference = modelReference.substring(modelReference.indexOf('#'));
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
	
	private static String getOperationId(List<String> idList, UrlMappingOperation umo, boolean useQName) {
		if (useQName) {
			return umo.getQName();
		} else {
			String prefix = umo.getProject().getName().toLowerCase();
			String operationId = prefix + "__" + umo.getName();
			
			int index = 1;
			while (idList.contains(operationId)) {
				operationId = operationId + "_n" + index;
				index++;
			}
			idList.add(operationId);
			return operationId;
		}
	}
	
	public static OpenAPI parse(String requestUrl, UrlMapper urlMapper) {
		return parse(requestUrl, urlMapper, false);
	}
	
	public static OpenAPI parse(String requestUrl, UrlMapper urlMapper, boolean useExternalRef) {
		Project project = urlMapper.getProject();
		String projectName = project.getName();
		
		String oasDirUrl = requestUrl.substring(0,requestUrl.indexOf("/"+servletMappingPath)) + 
										"/projects/"+ projectName + "/"+ jsonSchemaDirectory +"/";
		

		OpenAPI openAPI = null;
		
		File targetDir = new File(Engine.PROJECTS_PATH + "/" + projectName + "/" + jsonSchemaDirectory);
		File yamlFile = new File(targetDir, projectName+".yaml" );
		boolean doIt = Engine.isStudioMode() || !yamlFile.exists();
		
		if (doIt) {
			openAPI = parseCommon(requestUrl, project);
			
			List<Tag> tags = new ArrayList<>();
			Tag tag = new Tag();
			tag.setName(project.getName());
			tag.setDescription(project.getComment());
			tags.add(tag);
			openAPI.setTags(tags);
			
			if (openAPI.getComponents() == null) {
				openAPI.components(new Components());
			}
			
			// Security
			Map<String, SecurityScheme> securitySchemes = openAPI.getComponents().getSecuritySchemes();
			for (UrlAuthentication authentication: urlMapper.getAuthenticationList()) {
				if (AuthenticationType.Basic.equals(authentication.getType())) {
					if (securitySchemes == null || !securitySchemes.containsKey("basicAuth")) {
						SecurityScheme securitySchemesItem = new SecurityScheme();
						securitySchemesItem.setType(SecurityScheme.Type.HTTP);
						securitySchemesItem.setScheme("basic");
						openAPI.getComponents().addSecuritySchemes("basicAuth", securitySchemesItem);
						
						SecurityRequirement securityRequirement = new SecurityRequirement();
						securityRequirement.addList("basicAuth", new ArrayList<String>());
						openAPI.addSecurityItem(securityRequirement);
					}
				}
			}
			
			List<String> refList = new ArrayList<String>();
			List<String> opIdList = new ArrayList<String>();
			
			// Paths
			Paths paths = new Paths();
			try {
				for (UrlMapping urlMapping: urlMapper.getMappingList()) {
					PathItem item = new PathItem();
					for (UrlMappingOperation umo : urlMapping.getOperationList()) {
						Operation operation = new Operation();
						operation.setOperationId(getOperationId(opIdList, umo, false)/*umo.getQName()*/);
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
								Schema<?> schema = getSchema(ump);
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
								addBodyParameter(operation, ump, oasDirUrl, refList, useExternalRef);
							} else if (ump.getType() == Type.Header) {
								parameter = new HeaderParameter();
							} else if (ump.getType() == Type.Path) {
								// ignore : should have been treated before
							}
							
							if (parameter != null) { // Query | Header
								parameter.setName(ump.getName());
								parameter.setDescription(ump.getComment());
								parameter.setRequired(ump.isRequired());
								//parameter.setAllowEmptyValue(allowEmptyValue);
								Schema<?> schema = getSchema(ump);
								if (schema != null) {
									parameter.setSchema(schema);
								}
								
								// add parameter
								if (ump.isExposed()) {
									operation.addParametersItem(parameter);
								}
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
											modelReference = modelReference.replace(".jsonschema#/definitions/", ".json#/components/schemas/");
											modelReference = oasDirUrl + modelReference;
										}
										Content content = new Content();
										response.setContent(content);
										for (String mt: produces) {
											MediaType mediaType = new MediaType();
											content.addMediaType(mt, mediaType);
											ObjectSchema schema = new ObjectSchema();
											if (!refList.contains(modelReference)) {
												refList.add(modelReference);
											}
											if (!useExternalRef  && modelReference.indexOf('#') != -1) {
												modelReference = modelReference.substring(modelReference.indexOf('#'));
											}
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
	
			// Models and Schemas
			try {
				Map<String, JSONObject> modelMap = new HashMap<String, JSONObject>(1000);
				String models = getModels(oasDirUrl, urlMapper, modelMap);
				
				/*System.out.println("refList");
				for (String keyRef: refList) {
					System.out.println(keyRef);
				}
				System.out.println("modelMap");
				for (String keyRef: modelMap.keySet()) {
					System.out.println(keyRef);
				}*/
				
				JSONObject jsonModels = new JSONObject(models);
				for (String keyRef: refList) {
					addModelsFromMap(modelMap, keyRef, jsonModels);
				}
				
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
				openAPI.getComponents().schemas(map);
				
				modelMap.clear();
				
			} catch (Throwable t) {
				t.printStackTrace();
			}
			
			// write yaml
			try {
				FileUtils.write(yamlFile, prettyPrintYaml(openAPI), "UTF-8");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			
			// read yaml
			try {
				String content = FileUtils.readFileToString(yamlFile, "UTF-8");
				JsonNode rootNode = Yaml.mapper().readTree(content);
				OpenAPIDeserializer ds = new OpenAPIDeserializer();
				SwaggerParseResult result = ds.deserialize(rootNode);
				
				openAPI = result.getOpenAPI();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return openAPI;
	}
	
	public static void addModelsFromMap(Map<String, JSONObject> modelMap, String keyRef, JSONObject jsonModels) {
		try {
			if (modelMap.containsKey(keyRef)) {
				JSONObject ob = modelMap.get(keyRef);
				String pkey = keyRef.substring(keyRef.lastIndexOf('/')+1);
				if (ob.has("model")) {
					jsonModels.put(pkey, ob.getJSONObject("model"));
				}
				
				if (ob.has("refs")) {
					JSONArray refs = ob.getJSONArray("refs");
					for (int i = 0; i < refs.length(); i++) {
						addModelsFromMap(modelMap, refs.getString(i), jsonModels);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	
	private static String getConvertigoServeurUrl() {
		String webAppPath = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
		int index = webAppPath.indexOf("://") + 3;
		String scheme = webAppPath.substring(0, webAppPath.indexOf("://"));
		String host = webAppPath.substring(index, webAppPath.indexOf('/', index));
		String basePath = webAppPath.substring(index + host.length()) + "/" + servletMappingPath;
		String serverUrl = scheme+ "://"+ host + basePath;
		return serverUrl;
	}
	
	public static HttpConnector createRestConnector(OpenAPI openApi) throws Exception {
		try {
			HttpConnector httpConnector = new HttpConnector();
			httpConnector.bNew = true;
			
			Info info = openApi.getInfo();
			String title = info != null ? info.getTitle():"";
			title = title == null || title.isEmpty() ? "RestConnector":title;
			String description = info != null ? info.getDescription():"";
			description = description == null || description.isEmpty() ? "":description;
			httpConnector.setName(StringUtils.normalize(title));
			httpConnector.setComment(description);
			
			String httpUrl = "";
			List<Server> servers = openApi.getServers();
			if (servers.size() > 0) {
				httpUrl = servers.get(0).getUrl();
			}
			httpUrl = httpUrl.isEmpty() ? getConvertigoServeurUrl(): httpUrl;
			
			UrlFields urlFields = UrlParser.parse(httpUrl);
			if (urlFields != null) {
				String scheme = urlFields.getScheme();
				String host = urlFields.getHost();
				String port = urlFields.getPort();
				String basePath = urlFields.getPath();
				
				boolean isHttps = "https".equals(scheme);
				httpConnector.setHttps(isHttps);
				httpConnector.setServer(host);
				httpConnector.setPort(port == null ? (isHttps ? 443:80) : Integer.valueOf(port));
				httpConnector.setBaseDir(basePath);
			}
			httpConnector.setBaseUrl(httpUrl);
			
			List<SecurityRequirement> securityRequirements = openApi.getSecurity();
			if (securityRequirements != null && securityRequirements.size() > 0) {
				Map<String, SecurityScheme> securitySchemes = openApi.getComponents().getSecuritySchemes();
				for (SecurityRequirement sr: securityRequirements) {
					for (String s_name : sr.keySet()) {
						SecurityScheme securityScheme = securitySchemes.get(s_name);
						if (securityScheme != null) {
							boolean isBasicScheme = securityScheme.getScheme().toLowerCase().equals("basic");
							boolean isHttpType = securityScheme.getType().equals(SecurityScheme.Type.HTTP);
							if (isHttpType && isBasicScheme) {
								httpConnector.setAuthenticationType(AuthenticationMode.Basic);
								break;
							}
						}
					}
				}
			}
			
			Paths paths = openApi.getPaths();
			if (paths != null) {
				for (Entry<String, PathItem> entry : paths.entrySet()) {
					HttpMethodType httpMethodType = null;
					Operation operation = null;
					
					String subDir = entry.getKey();
					PathItem pathItem = entry.getValue();
					
					Map<HttpMethod, Operation> operationMap = pathItem.readOperationsMap();
					for (HttpMethod httpMethod : operationMap.keySet()) {
						
						operation = operationMap.get(httpMethod);
						
						if (httpMethod.equals(HttpMethod.GET)) {
							httpMethodType = HttpMethodType.GET;
						} else if (httpMethod.equals(HttpMethod.POST)) {
							httpMethodType = HttpMethodType.POST;
						} else if (httpMethod.equals(HttpMethod.PUT)) {
							httpMethodType = HttpMethodType.PUT;
						} else if (httpMethod.equals(HttpMethod.DELETE)) {
							httpMethodType = HttpMethodType.DELETE;
						} else if (httpMethod.equals(HttpMethod.HEAD)) {
							httpMethodType = HttpMethodType.HEAD;
						} else if (httpMethod.equals(HttpMethod.TRACE)) {
							httpMethodType = HttpMethodType.TRACE;
						} else if (httpMethod.equals(HttpMethod.OPTIONS)) {
							httpMethodType = HttpMethodType.OPTIONS;
						} else {
							httpMethodType = null;
						}
						
						if (operation != null && httpMethodType != null) {
							String operationId = operation.getOperationId();
							String operationDesc = operation.getDescription();
							String summary = operation.getSummary();
							
							String name = StringUtils.normalize(subDir + ":" + httpMethodType.toString());
							if (name.isEmpty()) {
								name = StringUtils.normalize(operationId);
								if (name.isEmpty()) {
									name = StringUtils.normalize(summary);
									if (name.isEmpty()) {
										name = "operation";
									}
								}
							}
							
							String comment = summary;
							if (comment.isEmpty()) {
								comment = operationDesc;
							}
							
							XMLVector<XMLVector<String>> httpParameters = new XMLVector<XMLVector<String>>();
							AbstractHttpTransaction transaction = new HttpTransaction();
							
							String h_ContentType = MimeType.WwwForm.value();
							/*if (consumeList != null) {
								if (consumeList.contains(MimeType.Json.value())) {
									h_ContentType = MimeType.Json.value();
								}
								else if (consumeList.contains(MimeType.Xml.value())) {
									h_ContentType = MimeType.Xml.value();
								}
								else {
									h_ContentType = consumeList.size() > 0 ? 
											consumeList.get(0) : MimeType.WwwForm.value();
								}
							}*/
							
							String h_Accept = MimeType.Json.value();
							/*if (produceList != null) {
								if (produceList.contains(h_ContentType)) {
									h_Accept = h_ContentType;
								}
								else {
									if (produceList.contains(MimeType.Json.value())) {
										h_Accept = MimeType.Json.value();
									}
									else if (produceList.contains(MimeType.Xml.value())) {
										h_Accept = MimeType.Xml.value();
									}
								}
								
								if (consumeList == null && h_Accept != null) {
									h_ContentType = h_Accept;
								}
							}*/
							
							if (h_Accept != null) {
								XMLVector<String> xmlv = new XMLVector<String>();
								xmlv.add("Accept");
								xmlv.add(h_Accept);
					   			httpParameters.add(xmlv);
					   			
								if (h_Accept.equals(MimeType.Xml.value())) {
									transaction = new XmlHttpTransaction();
									((XmlHttpTransaction)transaction).setXmlEncoding("UTF-8");
								}
								else if (h_Accept.equals(MimeType.Json.value())) {
									transaction = new JsonHttpTransaction();
									((JsonHttpTransaction)transaction).setIncludeDataType(false);
								}
							}
							
							// Add variables
							boolean hasBodyVariable = false;
							RequestBody body = operation.getRequestBody();
							if (body != null) {
								Map<String, MediaType> medias = body.getContent();
								for (String contentType : medias.keySet()) {
									MediaType mediaType = medias.get(contentType);
									Schema<?> mediaSchema =  mediaType.getSchema();
									List<String> requiredList = mediaSchema.getRequired();
									if (contentType.equals("application/x-www-form-urlencoded")) {
										@SuppressWarnings("rawtypes")
										Map<String, Schema> properties = mediaSchema.getProperties();
										if (properties != null) {
											for (String p_name : properties.keySet()) {
												Schema<?> schema = properties.get(p_name);
												String p_description = schema.getDescription();
												boolean p_required = requiredList == null ? false:requiredList.contains(p_name);
												
												boolean isMultiValued = false;
												if (schema instanceof ArraySchema) {
													isMultiValued = true;
												}
												
												RequestableHttpVariable httpVariable = isMultiValued ? 
														new RequestableHttpMultiValuedVariable():
														new RequestableHttpVariable();
												httpVariable.bNew = true;
												httpVariable.setHttpMethod(HttpMethodType.POST.name());
												httpVariable.setName(p_name);
												httpVariable.setDescription(p_name);
												httpVariable.setHttpName(p_name);
												httpVariable.setRequired(p_required);
												httpVariable.setComment(p_description == null ? "":p_description);
												
												if (schema instanceof FileSchema) {
													httpVariable.setDoFileUploadMode(DoFileUploadMode.multipartFormData);
												}
												
												Object defaultValue = schema.getDefault();
												if (defaultValue == null && p_required) {
													defaultValue = "";
												}
												httpVariable.setValueOrNull(defaultValue);
												
												transaction.addVariable(httpVariable);
											}
										}
									} else if (!hasBodyVariable) {
										RequestableHttpVariable httpVariable = new RequestableHttpVariable();
										httpVariable.bNew = true;
										httpVariable.setHttpMethod(HttpMethodType.POST.name());
										httpVariable.setRequired(true);
										
										// overrides variable's name for internal use
										httpVariable.setName(com.twinsoft.convertigo.engine.enums.Parameter.HttpBody.getName());
										
										Object defaultValue = null;
										httpVariable.setValueOrNull(defaultValue);
										
										transaction.addVariable(httpVariable);
										
										h_ContentType = contentType;
										
										hasBodyVariable = true;
									}
								}
							}
							
							List<Parameter> parameters = operation.getParameters();
							if (parameters != null) {
								for (Parameter parameter: parameters) {
									String p_name = parameter.getName();
									String p_description = parameter.getDescription();
									boolean p_required = parameter.getRequired();
									
									boolean isMultiValued = false;
									Schema<?> schema = parameter.getSchema();
									if (schema instanceof ArraySchema) {
										isMultiValued = true;
									}
									
									RequestableHttpVariable httpVariable = isMultiValued ? 
											new RequestableHttpMultiValuedVariable():
											new RequestableHttpVariable();
									httpVariable.bNew = true;
									
									httpVariable.setName(p_name);
									httpVariable.setDescription(p_name);
									httpVariable.setHttpName(p_name);
									httpVariable.setRequired(p_required);
									httpVariable.setComment(p_description == null ? "":p_description);
									
									if (parameter instanceof QueryParameter || parameter instanceof PathParameter || parameter instanceof HeaderParameter) {
										httpVariable.setHttpMethod(HttpMethodType.GET.name());
										if (parameter instanceof HeaderParameter) {
											// overrides variable's name : will be treated as dynamic header
											httpVariable.setName(com.twinsoft.convertigo.engine.enums.Parameter.HttpHeader.getName() + p_name);
											httpVariable.setHttpName(""); // do not post on target server
										}
										if (parameter instanceof PathParameter) {
											httpVariable.setHttpName(""); // do not post on target server
										}
									} else {
										httpVariable.setHttpMethod("");
									}
									
									Object defaultValue = schema.getDefault();
									if (defaultValue == null && p_required) {
										defaultValue = "";
									}
									httpVariable.setValueOrNull(defaultValue);
									
									transaction.addVariable(httpVariable);
								}
							}
							
							// Set Content-Type
							if (h_ContentType != null) {
								XMLVector<String> xmlv = new XMLVector<String>();
								xmlv.add(HeaderName.ContentType.value());
								xmlv.add(hasBodyVariable ? h_ContentType:MimeType.WwwForm.value());
					   			httpParameters.add(xmlv);
							}
							
							transaction.bNew =  true;
							transaction.setName(name);
							transaction.setComment(comment);
							transaction.setSubDir(subDir);
							transaction.setHttpVerb(httpMethodType);
							transaction.setHttpParameters(httpParameters);
							transaction.setHttpInfo(true);
							
							httpConnector.add(transaction);
						}
					}
				}
			}
			return httpConnector;
		}
		catch (Throwable t) {
			Engine.logEngine.error("Unable to create connector", t);
			throw new Exception("Unable to create connector", t);
		}
	}
	
	public static String prettyPrintJson(OpenAPI openAPI) {
		return Json.pretty(openAPI);
	}
	
    static ObjectMapper mapper;

    @SuppressWarnings({ "serial", "deprecation" })
	private static ObjectMapper objectMapper() {
        if (mapper == null) {
            YAMLFactory factory = new YAMLFactory();
            factory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
            factory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
            factory.disable(YAMLGenerator.Feature.SPLIT_LINES);
            factory.enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS);
            
            mapper = new ObjectMapper(factory);

            // handle ref schema serialization skipping all other props
            mapper.registerModule(new SimpleModule() {
                @Override
                public void setupModule(SetupContext context) {
                    super.setupModule(context);
                    context.addBeanSerializerModifier(new BeanSerializerModifier() {
                        @SuppressWarnings("unchecked")
						@Override
                        public JsonSerializer<?> modifySerializer(
                                SerializationConfig config, BeanDescription desc, JsonSerializer<?> serializer) {
                            if (Schema.class.isAssignableFrom(desc.getBeanClass())) {
                                return new SchemaSerializer((JsonSerializer<Object>) serializer);
                            }
                            return serializer;
                        }
                    });
                }
            });

            Module deserializerModule = new DeserializationModule();
            mapper.registerModule(deserializerModule);

            Map<Class<?>, Class<?>> sourceMixins = new LinkedHashMap<>();

            sourceMixins.put(ApiResponses.class, ExtensionsMixin.class);
            sourceMixins.put(ApiResponse.class, ExtensionsMixin.class);
            sourceMixins.put(Callback.class, ExtensionsMixin.class);
            sourceMixins.put(Components.class, ComponentsMixin.class);
            sourceMixins.put(Contact.class, ExtensionsMixin.class);
            sourceMixins.put(Encoding.class, ExtensionsMixin.class);
            sourceMixins.put(EncodingProperty.class, ExtensionsMixin.class);
            sourceMixins.put(Example.class, ExtensionsMixin.class);
            sourceMixins.put(ExternalDocumentation.class, ExtensionsMixin.class);
            sourceMixins.put(Header.class, ExtensionsMixin.class);
            sourceMixins.put(Info.class, ExtensionsMixin.class);
            sourceMixins.put(License.class, ExtensionsMixin.class);
            sourceMixins.put(Link.class, ExtensionsMixin.class);
            sourceMixins.put(LinkParameter.class, ExtensionsMixin.class);
            sourceMixins.put(MediaType.class, ExtensionsMixin.class);
            sourceMixins.put(OAuthFlow.class, ExtensionsMixin.class);
            sourceMixins.put(OAuthFlows.class, ExtensionsMixin.class);
            sourceMixins.put(OpenAPI.class, OpenAPIMixin.class);
            sourceMixins.put(Operation.class, OperationMixin.class);
            sourceMixins.put(Parameter.class, ExtensionsMixin.class);
            sourceMixins.put(PathItem.class, ExtensionsMixin.class);
            sourceMixins.put(Paths.class, ExtensionsMixin.class);
            sourceMixins.put(RequestBody.class, ExtensionsMixin.class);
            sourceMixins.put(Scopes.class, ExtensionsMixin.class);
            sourceMixins.put(SecurityScheme.class, ExtensionsMixin.class);
            sourceMixins.put(Server.class, ExtensionsMixin.class);
            sourceMixins.put(ServerVariable.class, ExtensionsMixin.class);
            sourceMixins.put(ServerVariables.class, ExtensionsMixin.class);
            sourceMixins.put(Tag.class, ExtensionsMixin.class);
            sourceMixins.put(XML.class, ExtensionsMixin.class);
            sourceMixins.put(Schema.class, ExtensionsMixin.class);

            mapper.setMixIns(sourceMixins);
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            return mapper;
        }
        return mapper;
    }

    public static String yamlPretty(Object o) {
        try {
        	ObjectWriter ow = objectMapper().writer(new DefaultPrettyPrinter());
            return ow.writeValueAsString(o);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
	
	public static String prettyPrintYaml(OpenAPI openAPI) {
		//return Yaml.pretty(openAPI);
		return yamlPretty(openAPI);
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
		OpenAPI openAPI = read("https://petstore3.swagger.io/api/v3/openapi.json");
		if (openAPI != null) {
			Json.prettyPrint(openAPI);
			Yaml.prettyPrint(openAPI);
		}
	}
	
	public static void testReadYaml() {
		OpenAPI openAPI = read("https://petstore3.swagger.io/api/v3/openapi.yaml");
		if (openAPI != null) {
			Json.prettyPrint(openAPI);
			Yaml.prettyPrint(openAPI);
		}
	}
	
}
